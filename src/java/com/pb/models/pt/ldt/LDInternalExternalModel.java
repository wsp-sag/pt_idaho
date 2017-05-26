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

import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.TazManager;
import com.pb.models.pt.util.SkimsInMemory;

import static com.pb.models.pt.ldt.LDInternalExternalParameters.*;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * This binary choice model predicts whether a long-distance tour
 * will have a destination within Ohio + Halo (internal) or outside
 * of that area (external).  
 * 
 * Note that the base alternative is INTERNAL, with a utility of 0.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 4, 2006
 *
 */
public class LDInternalExternalModel {

    protected static Logger logger = Logger.getLogger(LDInternalExternalModel.class);
    protected ResourceBundle appRb;
    protected ResourceBundle globalRb;
    public static SkimsInMemory skims;
    private boolean trace = false;
    
    private float[][] parameters; 
    private LDInternalExternalPersonAttributes iepa;
    
    public ArrayList<Integer> externalStations; 
    private Hashtable<Integer, Float> timeToExternalStation;

    private long ldInternalExternalFixedSeed = Long.MAX_VALUE/9;
    /**
     * Default constructor used at initialization
     */
    public LDInternalExternalModel(){

    }
    
    /**
     * Constructor reads parameters file and builds the model.   
     */
    public LDInternalExternalModel(ResourceBundle rb, ResourceBundle globalRb, TazManager tazManager){
        initializeObject(rb, globalRb,  tazManager);
    }

    public void initializeObject(ResourceBundle rb, ResourceBundle globalRb, TazManager tazManager){
        this.appRb = rb;
        this.globalRb = globalRb;
        readParameters();
        readExternalStations(rb);
        buildModel();
        calculateTimeToExternalStations(tazManager);				
        String internalExternalClassName = ResourceUtil.getProperty(rb,"ldt.inex.person.attribute.class");
        Class inExClass = null;
        iepa = null;
        try {
            inExClass = Class.forName(internalExternalClassName);
            iepa = (LDInternalExternalPersonAttributes) inExClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of LDInternalExternalPersonAttributes of type "+inExClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of LDInternalExternalPersonAttributes of type "+inExClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Read parameters from file specified in properties.
     * 
     */
    private void readParameters() {
        
        logger.info("Reading LD Tour Internal-External Model Parameters");
        parameters = ParameterReader.readParameters(appRb,
                "ldt.internal.external.parameters");
               

    }

        public void readExternalStations(ResourceBundle rb){					
            // read the external station IDs
            TableDataSet externalStationData = ParameterReader.readParametersAsTable(globalRb,
                "external.zonal.data");
            externalStations = new ArrayList<Integer>();

            for (int row=1; row<=externalStationData.getRowCount(); row++) {
                Integer taz = new Integer((int) externalStationData.getValueAt(row, "ETAZ"));
                externalStations.add(taz);
            }
        }
    
    /**
     * Currently has no functionality, but included for consistency of interface.  
     */
    private void buildModel() { 

    }
    
    /** 
     * Calculates the time from each TAZ to the nearest external station.  
     *
     */
    public void calculateTimeToExternalStations(TazManager tazManager) {
    	skims = SkimsInMemory.getSkimsInMemory();
    	Matrix time = skims.opTime;
        
        int[] tazId = tazManager.getExternalNumberArrayZeroIndexed(); 
         
        timeToExternalStation = new Hashtable<Integer, Float>(); 
        
        for (int i=0; i<tazId.length; i++) {
            float minTime = 999999; 
            for (Integer extSta : externalStations) {
                float curTime = time.getValueAt(tazId[i], extSta);
                if (curTime < minTime) {
                    minTime = curTime; 
                }
            }
            timeToExternalStation.put(tazId[i], minTime); 
        }      
    }
    
    /**
     * Calculate the utility of an EXTERNAL destination.  
     * 
     * @param hha  The household and tour attributes of the traveler.
     * @param tour Decision-makers long-distance tour.
     * 
     * @return The utility of an external destination 
     */
    public double calculateUtility(LDInternalExternalPersonAttributes hha, LDTour tour) {
    
        int p = tour.purpose.ordinal(); 
        
        // code one last attribute
        int completeTour = 0;
        if (tour.patternType == LDTourPatternType.COMPLETE_TOUR) completeTour = 1; 
    
        // get the time to nearest external station
        Float timeToExt = timeToExternalStation.get(tour.homeTAZ);
        
        // then the utility
        double utility = 0; 
        utility += parameters[p][CONSTANT];
        utility += parameters[p][COMPLETETOUR] * completeTour; 
        utility += parameters[p][INCOME60P]    * hha.income60p; 
        utility += parameters[p][WORKER]       * hha.worker;  
        utility += parameters[p][AGELT25]      * hha.agelt25;
        utility += parameters[p][AGE5564]      * hha.age5564;
        utility += parameters[p][AGE65P]       * hha.age65p;
        utility += parameters[p][OCCCONSTRUCT] * hha.occConstruct;
        utility += parameters[p][OCCFININSREAL]* hha.occFinInsReal;
        utility += parameters[p][OCCPUBADMIN]  * hha.occPubAdmin;
        utility += parameters[p][OCCEDUCATION] * hha.occEducation;
        utility += parameters[p][OCCMEDICAL]   * hha.occMedical;
        utility += parameters[p][TIMETOEXTSTA] * timeToExt; 
                
        if (trace && logger.isDebugEnabled()) {
            logger.debug("The utility of an external destination is " + utility);
        }
        
        return utility;
    }
    
    
    /**
     * Draw a random number and monte-carlo the choice.
     * 
     * @param utility  The utility of having an external destination.  
     * @return the chosen alternative
     */
    private LDTourDestinationType simlulateBinaryChoice(double utility, double random){
        LDTourDestinationType choice = LDTourDestinationType.INTERNAL;
        
        double probability = 1/(1+Math.exp(-utility));
        

        if(random<probability) choice = LDTourDestinationType.EXTERNAL; 
                    
        return choice;
    }
    
    /**
     * Chooses whether the destination is internal or external.  
     * 
     * @param tour The long-distance tour of interest.
     * @return     Choice of an internal or external destination.  
     */
    public LDTourDestinationType chooseInternalExternal(LDTour tour, boolean sensitivityTesting) {
        PTHousehold hh = tour.hh; 
        PTPerson p = tour.person;

        //calculate randomSeed based on the decision maker seed and the model seed
        long seed = tour.hh.ID*100 + tour.person.memberID + tour.ID + ldInternalExternalFixedSeed;
        if(sensitivityTesting) seed += System.currentTimeMillis();

        //Create the random number generator
        Random random = new Random();
        random.setSeed(seed);
        
        // calculate the household and person characteristics
        iepa.codeHouseholdAttributes(hh);
        iepa.codePersonAttributes(p);
        
        // choose the alternative
        double utility = calculateUtility(iepa, tour);
        LDTourDestinationType choice = simlulateBinaryChoice(utility, random.nextDouble());
        
        // print the choice
        if (trace) {
            logger.info("    The IE for HH + " + tour.hh.ID
                    + " person " + tour.person.memberID + " tour " + tour.ID
                    + " is : " + choice);
        }
        
        return choice; 
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

    /**
     * Writes the time from each TAZ to the nearest external station to 
     * a CSV file.  Used in developing the model for exporting to an 
     * estimation file.  
     *
     */
    private void writeTimeToExternalStations(String fileName) {
        
        // convert to a table data set
        float[] tazArray  = new float[timeToExternalStation.size()];
        float[] timeArray = new float[timeToExternalStation.size()]; 
        int i=0; 
        
        for (Integer taz : timeToExternalStation.keySet()) {
            Float timeToExt = timeToExternalStation.get(taz); 
            
            tazArray[i]  = taz.floatValue(); 
            timeArray[i] = timeToExt.floatValue(); 
            i++; 
        }
                
        TableDataSet outTable = new TableDataSet(); 
        outTable.appendColumn(tazArray, "TAZ");
        outTable.appendColumn(timeArray, "TimeToExtSta");
        
        // write
        try { 
            File file = new File(fileName);
            CSVFileWriter writer = new CSVFileWriter();
            writer.writeFile(outTable, file);
            writer.close();
        } catch (Exception e) {
            logger.error("Error writing to file " + fileName);
            System.exit(1);            
        }
    }
    
    /**
     * Only used for model development to write TAZ distance to external stations
     * to file.  
     * 
     * @param appRb
     * @param globalRb
     * @return
     */
    private static TazManager readTazData(ResourceBundle appRb, ResourceBundle globalRb) {
        // initialize the taz manager and destination choice logsums
        String tazManagerClassName = ResourceUtil.getProperty(appRb,"sdt.taz.manager.class");
        Class tazManagerClass = null;
        TazManager tazManager = null;
        try {
            tazManagerClass = Class.forName(tazManagerClassName);
            tazManager = (TazManager) tazManagerClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.fatal(tazManagerClass + " not found");
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            logger.fatal("Can't Instantiate of TazManager of type "+tazManagerClass.getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Illegal Access of TazManager of type "+tazManagerClass.getName());
            throw new RuntimeException(e);
        }

        String tazClassName = appRb.getString("sdt.taz.class");
        tazManager.setTazClassName(tazClassName);
        tazManager.readData(globalRb, appRb);
        
        // update the employment
        String empFileName = ResourceUtil.getProperty(appRb, "sdt.employment");
        tazManager.updateWorkersFromSummary(empFileName);
        
        return tazManager; 
    }
    
    /**
     * Launch path used in model development to write a table with the
     * time from each TAZ to the nearest external station.  
     * 
     * @param args
     */
    public static void main(String[] args) {

        ResourceBundle appRb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        
        TazManager tazManager = readTazData(appRb, globalRb); 

        // initialize the skims in memory
        //LDSkimsInMemory LDSkims = LDSkimsInMemory.getInstance();
        //LDSkims.readSkimsIntoMemory(globalRb, appRb);
        
        LDInternalExternalModel model = new LDInternalExternalModel(appRb, globalRb, tazManager);
        model.writeTimeToExternalStations("timeToExternalStations.csv");
        
        logger.info("Done writing time to external stations.");
    }
}
