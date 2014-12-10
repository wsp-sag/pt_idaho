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

import org.apache.log4j.Logger;

import com.pb.common.util.ResourceUtil;

import java.util.ResourceBundle;

/**
 * Determines the destination for long-distance tours with 
 * destinations outside the model area.
 * 
 * @author Erhardt
 * @version 1.0 Apr 5, 2006
 *
 */
public abstract class LDExternalDestinationModel {

    protected static Logger logger = Logger.getLogger(LDExternalDestinationModel.class);
    protected ResourceBundle ptRb;  
    protected ResourceBundle globalRb;
    protected boolean trace = false;
        
    public long ldExternalDestinationFixedSeed = Long.MAX_VALUE/27;
    
    /**
     * Default constructor.      
     */
    public LDExternalDestinationModel() {
    }

    /**
     * Initializes the class.      
     */
    public void initialize(ResourceBundle globalRb,
            ResourceBundle ptRb) {
        this.ptRb = ptRb; 
        this.globalRb = globalRb;
    }
    
    
    /**
     * Creates an instance of an LDExternalDestinationModel, based on the specific 
     * class specified in the pt resource bundle.  
     * 
     */
    public static LDExternalDestinationModel newInstance(ResourceBundle globalRb, ResourceBundle ptRb) {
        String className = ResourceUtil.getProperty(ptRb,"ldt.external.destination.model.class");
        Class ldExternalDestinationModelClass;
        LDExternalDestinationModel ldExternalDestinationModel = null; 
        try {
            ldExternalDestinationModelClass = Class.forName(className); 
            ldExternalDestinationModel = (LDExternalDestinationModel) ldExternalDestinationModelClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error instantiating LDExternalDestinationModel class", e);
        }
        
        ldExternalDestinationModel.initialize(globalRb, ptRb);
        return ldExternalDestinationModel;         
    }
    
    
    /**
     * Choose a TAZ by selecting from a table of frequencies for the origin of the trip.
     * 
     * @param tour The long-distance tour of interest.  
     * 
     * @return  The ID of the chosen TAZ.  
     */
    abstract public int chooseTaz(LDTour tour, boolean sensitivityTesting);
    
    /**
     * Override this method. 
     * 
     * Checks whether the destination is within the Halo in which the internal
     * mode choice model can be applied.  Only possible to be true for OSMP model.  
     * 
     * @param tour
     * @return
     */
    public boolean isDestinationInModeChoiceHalo(LDTour tour) {
        
        return false; 
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
