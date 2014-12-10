/*
 * Copyright  2006 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.pb.models.pt.ldt;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * Choose mode for long-distance tours (auto, air, walk to transit or drive to transit).  
 * Applied only for external destinations.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 23, 2006
 *
 */
public abstract class LDExternalModeChoiceModel {

    protected static Logger logger = Logger.getLogger(LDExternalModeChoiceModel.class);  
    protected ResourceBundle rb;
    protected ResourceBundle globalRb;
    protected boolean trace = false;
    
    // The model parameters, for each purpose
    protected float[][] parameters;
    protected Matrix distance;

    public long ldExternalModeFixedSeed = Long.MIN_VALUE;


    /**
     * Default Constructor. 
     *
     */
    public LDExternalModeChoiceModel() {
        
    }

    
    /**
     * Creates an instance of an LDExternalModeChoiceModel, based on the specific 
     * class specified in the pt resource bundle.  
     * 
     */
    public static LDExternalModeChoiceModel newInstance(ResourceBundle globalRb, ResourceBundle ptRb) {
        
        String className = ResourceUtil.getProperty(ptRb,"ldt.external.mode.choice.model.class");
        Class ldExternalModeChoiceModelClass;
        LDExternalModeChoiceModel ldExternalModeChoiceModel = null; 
        try {
            ldExternalModeChoiceModelClass = Class.forName(className); 
            ldExternalModeChoiceModel = (LDExternalModeChoiceModel) ldExternalModeChoiceModelClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating LDExternalModeChoiceModel class", e);
        }
        ldExternalModeChoiceModel.initialize(ptRb, globalRb);
        return ldExternalModeChoiceModel;         
    }
    
    /**
     * Initializes class variables.  
     * 
     * @param ptRb
     * @param globalRb
     */
    public void initialize(ResourceBundle ptRb, ResourceBundle globalRb) {
        this.rb = ptRb;
        this.globalRb = globalRb;
    }
    
    
    /**
     * Generically read a matrix file.
     *
     * @param fileName
     *            Base file name.
     * @param matrixName
     *            Name of the matrix in that file.
     * @return The matrix read in.
     */
    protected Matrix readTravelCost(String fileName, String matrixName) {
        long startTime = System.currentTimeMillis();
        
        logger.info("Reading travel costs in " + fileName);
        
        Matrix matrix = MatrixReader.readMatrix(new File(fileName), matrixName);
        matrix.setName(matrixName);

        logger.debug("\tRead " + fileName + " in: "
                + (System.currentTimeMillis() - startTime) / 1000 + " seconds");

        return matrix;
    }

    
    /**
     * Read parameters from file specified in properties.
     * 
     */
    protected void readParameters() {
        
        logger.info("Reading LDExternalModeChoiceModelParameters");
        parameters = ParameterReader.readParameters(rb,
                "ldt.external.mode.choice.parameters");        
    }
    
    /**
     * Override in class that's used.  
     *
     */
    abstract public LDTourModeType chooseMode(LDTour tour, boolean sensitivityTesting); 

    
    /**
     * Override in subclass.
     * 
     * @param tour The tour of interest.  
     * @return The distance from the home zone to the destination zone.  
     */
    public float getDistance(LDTour tour) {
        return 0; 
    }
    
    /**
     * Set the trace option.
     * 
     * The trace option is set to false by default. The verbosity of trace
     * output is constrolled through the info and debug logger levels.
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }

}
