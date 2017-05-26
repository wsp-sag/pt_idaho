
/*
 * Copyright  2005 PB Consult Inc.
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
package com.pb.models.pt.daf;


import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import com.pb.models.pt.ldt.LDBinaryChoiceModel;
import com.pb.models.pt.ldt.LDPatternModel;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.RunLDTModels;
import com.pb.models.pt.tourmodes.AutoDriver;
import com.pb.models.pt.util.MCLogsumsInMemory;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;
import java.io.File;
import java.util.HashMap;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * HouseholdWorker processes all messages sent by PTDafMaster
 *
 *
 * @author Christi Willison
 * @version 3.0, 3/8/2005
 *
 */
public class MicroSimulationWorkerTask extends MessageProcessingTask {
    public static Logger ptLogger = Logger.getLogger(MicroSimulationWorkerTask.class);

    protected static final Object lock = new Object();
    protected static boolean initialized = false;

    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static int baseYear;
    protected static SkimsInMemory skims;
    protected static boolean CALCULATE_LDT;
    protected static boolean CALCULATE_SDT;
    protected static boolean CALCULATE_VM;

    protected static boolean sensitivityTestingMode;

    private Tracer tracer = Tracer.getTracer();

    protected PTOccupationReferencer occReferencer;

    private LDBinaryChoiceModel ldBinaryChoiceModel;
    private LDPatternModel ldPatternModel;

    protected TazManager tazManager;

    private PatternChoiceModel patternModel;
    private TourDestinationChoiceModel dcModel;
    private TourModeChoiceModel tourMC;

    private StopPurposeModel iStopPurposeModel;
    private TourSchedulingModel tourSchedulingModel;
    private TourStopChoiceModel tourStopChoiceModel;

    private StopDestinationChoiceModel stopDestinationChoiceModel;
    private StopDurationModel stopDurationModel;
    private TripModeChoiceModel tripModeChoiceModel;
    private WorkBasedTourModel workBasedTourModel;
    
    private static Matrix[] workMCLogsums;
    double durationTime;
    double primaryTime;
    double secondaryTime;
    double destZoneTime;
    double stopZoneTime;
    double tripModeTime;

    PTHousehold[] households;
    boolean firstProcessHouseholdMessage = true;

    //I did some testing to check the variability of the
    //first number in the sequence and these produce
    //a nice set of "random" numbers.
    long patternModelFixedSeed = Long.MIN_VALUE;
    long tourSchedulingFixedSeed = Long.MIN_VALUE/3;
    long tourDestinationFixedSeed = Long.MIN_VALUE/9;
    long tourModeFixedSeed = Long.MIN_VALUE/27;
    long tourStopFixedSeed = Long.MIN_VALUE/81;
    long stopPurposeFixedSeed = Long.MAX_VALUE;
    long stopDestinationFixedSeed = Long.MAX_VALUE/3;
    long stopDuration1FixedSeed = Long.MAX_VALUE/9;
    long stopDuration2FixedSeed = Long.MAX_VALUE/27;
    long tripModeFixedSeed = Long.MAX_VALUE/81;
    long workBasedFixedSeed = Long.MAX_VALUE/243;


    /**
     * Onstart method sets up model
     * @param occRef - project specific occupation names
     */
    public void onStart(PTOccupationReferencer occRef) {

         ptLogger.info(getName() + ", Started");
        // establish a connection between the workers
        // and the work server.
        Message checkInMsg = mFactory.createMessage();
        checkInMsg.setId(MessageID.WORKER_CHECKING_IN);
        checkInMsg.setValue("workQueueName", getName().trim().substring(0,2) + "_" + getName().trim().substring(12) + "WorkQueue");
        String queueName = "MS_node" + getName().trim().substring(12,13) + "WorkQueue";
        sendTo(queueName, checkInMsg);


        synchronized ( lock) {
            if (! initialized) {

                ptLogger.info(getName() + ", Initializing PT Model on Node");
                // We need to read in the Run Parameters (timeInterval and
                // pathToResourceBundle) from the RunParams.properties file
                // that was written by the Application Orchestrator
                String scenarioName;
                int timeInterval;
                String pathToPtRb;
                String pathToGlobalRb;

                ptLogger.info(getName() + ", Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                scenarioName = ResourceUtil.getProperty(runParamsRb,
                            "scenarioName");
                ptLogger.info(getName() + ", Scenario Name: " + scenarioName);
                baseYear = Integer.parseInt(ResourceUtil.getProperty(
                            runParamsRb, "baseYear"));
                ptLogger.info(getName() + ", Base Year: " + baseYear);
                timeInterval = Integer.parseInt(ResourceUtil.getProperty(
                            runParamsRb, "timeInterval"));
                ptLogger.info(getName() + ", Time Interval: " + timeInterval);
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,
                            "pathToAppRb");
                ptLogger.info(getName() + ", ResourceBundle Path: "
                            + pathToPtRb);
                pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,
                            "pathToGlobalRb");
                ptLogger.info(getName() + ", ResourceBundle Path: "
                            + pathToGlobalRb);

                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(
                                    pathToGlobalRb));

                //initialize price converter
                PriceConverter.getInstance(ptRb,globalRb);

                //The SkimReaderTask will read in the skims
                //prior to any other task being asked to do work.
                skims = SkimsInMemory.getSkimsInMemory();

                CALCULATE_LDT = ResourceUtil.getBooleanProperty( ptRb, "sdt.calculate.ldt", true);
                CALCULATE_SDT = ResourceUtil.getBooleanProperty( ptRb, "sdt.calculate.sdt", true);
                CALCULATE_VM = ResourceUtil.getBooleanProperty( ptRb, "sdt.calculate.vm", false);

                if(CALCULATE_LDT) logger.info("User is running the Resident Long-distance Models");
                if(CALCULATE_SDT) logger.info("User is running the Resident Short-distance Models");
                if(CALCULATE_VM) logger.info("User is running the Visitor Short-distance Models");

                //sensitivity testing is for Tlumip - added to code on 4/22/08 by Christi
                //The idea is that when in sensitivityTestMode we would allow the sequence of random numbers
                //to vary from run to run instead of fixing the seed (and thereby fixing the sequence of random numbers)
                //in order to be able to reproduce the results.
                sensitivityTestingMode = ResourceUtil.getBooleanProperty(ptRb, "pt.sensitivity.testing", false);
                ptLogger.info(getName() + ", Sensitivity Testing: " + sensitivityTestingMode);

                initialized = true;
            }

            // set-up the Trace object
             tracer.readTraceSettings( ptRb);
             occReferencer = occRef;
         }

         ptLogger.info(getName() + ", Finished onStart()");

    }

    /**
     * A worker bee that will process a block of households.
     *
     */
    public void onMessage(Message msg) {

        ptLogger.info(getName() + ", Received messageId=" + msg.getId()
                + " message from=" + msg.getSender());

        if (msg.getId().equals(MessageID.CALCULATE_AUTO_OWNERSHIP)) {
            initializeTazManager();
            runAutoOwnershipModel(msg);

        } else if(msg.getId().equals(MessageID.PROCESS_HOUSEHOLDS)){
            if(firstProcessHouseholdMessage){
            	//if only calculating LDT, only need to initialize the ldt models
                if(CALCULATE_LDT && !CALCULATE_SDT && !CALCULATE_VM){
                    initializeLDTModels();
                } else if(CALCULATE_VM && !CALCULATE_LDT && !CALCULATE_SDT){
                    initializeTazManager();
                    initializeSDTModels();
                }else{
                	initializeLDTModels();
                    initializeTazManager();
                    initializeSDTModels();
                }

                firstProcessHouseholdMessage = false;
            }
            processHouseholds(msg);
        }
    }
    
    private void runAutoOwnershipModel(Message msg){
        int startRow = (Integer) msg.getValue("startRow");
        int endRow = (Integer) msg.getValue("endRow");
        int[] workersByHhId = (int[]) msg.getValue("workersByHhId");

        ptLogger.info(getName() + ", Running the AutoOwnershipModel on rows " +
            startRow + " - " + endRow);

        AutoOwnershipModel aom = createAutoOwnershipModel(); 
        aom.buildModel();

        ptLogger.info(getName() + ", Reading in the Households and creating HH objects");
        PTDataReader reader = new PTDataReader( ptRb,  globalRb,  null, baseYear);
        PTHousehold[] households = reader.readHouseholds(startRow, endRow);
        ptLogger.info(getName() + ", Household array length: " + households.length);

        ptLogger.info(getName() + ", Starting auto ownership calculations.");
        HashMap<Integer, Byte> autosByHhId =new HashMap<Integer, Byte>();

        for (PTHousehold household : households) {
            household.workers = (byte)workersByHhId[household.ID];
            //logger.info("*** Summary: " + household.summary());
            aom.calculateUtility(household);
            household.autos = (byte) aom.chooseAutoOwnershipWithRandomSeedControl(sensitivityTestingMode, household.ID);
            autosByHhId.put(household.ID, household.autos);
        }


        ptLogger.info(getName() + ", Sending AutoOwnership results back to TaskMasterQueue");
        Message returnMsg = createMessage();
        returnMsg.setId(MessageID.AUTO_OWNERSHIP_CALCULATED);
        returnMsg.setValue("autosByHhId", autosByHhId);
        sendTo("TaskMasterQueue", returnMsg );

        ptLogger.info(getName() + ", Finished auto ownership.");
    }
    
    /**
     * Creates an auto ownership model to use. 
     * 
     * @return An auto ownership model.
     */
    public AutoOwnershipModel createAutoOwnershipModel() {

        AutoOwnershipModel aom = new AutoOwnershipModel( ptRb,  skims.pkTime,  skims.pkDist,  globalRb.getString("alpha.name"));
        return aom; 
    }


    /**
    * Process PT Models for one block of households
    *
    * @param msg Message
    */
    public void processHouseholds (Message msg) {

        ptLogger.info(getName() + ", Processing hh block " +  msg.getValue("blockNumber"));
        households = (PTHousehold[]) msg.getValue("households");

        //You can assume that the person wants to process visitor hhs
        //or otherwise you wouldn't be getting a message with visitor
        //hhs so there is no reason to check "CALCULATE_VM" here
        if(households[0].isVisitor()){
            runShortDistanceModels(0);

        }else if(CALCULATE_LDT && !CALCULATE_SDT){  //households must be residents
            //this will send tours off to the long-distance worker
            //if there are any
        	int numLDTTours = runLDTInitialModels();

            //this message will eventually be passed along to the MasterTask
            //which is keeping track of when the process is finished.
            Message returnMsg = createMessage();
            returnMsg.setId(MessageID.HOUSEHOLDS_PROCESSED);
            returnMsg.setValue("households", households);
            returnMsg.setValue("ldtToursExpected", numLDTTours);
            ptLogger.info(getName() + ", Sending households to results queue.");
            sendTo("ResultsWriterQueue", returnMsg);

        }else{
            int numLDTTours = runLDTInitialModels();
            runShortDistanceModels(numLDTTours);
        }
    }

    private void initializeLDTModels () {
        String alphaName = ResourceUtil.getProperty(globalRb, "alpha.name");
        TourDestinationChoiceLogsums.readLogsums(ptRb, alphaName);
        if(ldBinaryChoiceModel == null){
             ptLogger.info(getName() + ", Initializing the LD Binary Choice Model");
            ldBinaryChoiceModel = new LDBinaryChoiceModel( ptRb);

             ptLogger.info(getName() + ", Initializing the LD Pattern Choice Model");
            ldPatternModel = new LDPatternModel( ptRb);
        }
    }

    private int runLDTInitialModels(){

        ptLogger.info(getName() + ", Choosing LDT binary and patterns.");
        ldBinaryChoiceModel.runBinaryChoiceModelWithRandomSeedControl(households, sensitivityTestingMode);
        ldPatternModel.runPatternModel(households, sensitivityTestingMode);

        if(CALCULATE_LDT) {
            LDTour[] ldTours = RunLDTModels.createLDTours(households);
            if (ldTours != null) {
                Message ldMessage = createMessage();
                ldMessage.setId(MessageID.HOUSEHOLDS_PROCESSED);
                ldMessage.setValue("tours", ldTours);

                ptLogger.info(getName() + ", Sending long distance tours "
                    + " to the long distance worker.");

                sendTo("LongDistanceWorkQueue", ldMessage);
            }
            return ldTours == null ? 0 : ldTours.length;
        } else {
            return 0;             
        }        
    }

    private void initializeSDTModels(){
        if(patternModel == null){
                 ptLogger.info(getName() + ", Initializing stop purpose model.");
                iStopPurposeModel = new StopPurposeModel( ptRb);

                 ptLogger.info(getName() + ", Initializing the MC model.");
                tourMC = new TourModeChoiceModel( ptRb);

                 ptLogger.info(getName() + ", Initializing the DC model.");
                dcModel = new TourDestinationChoiceModel( ptRb);
                dcModel.buildModel(tazManager);

                 ptLogger.info(getName() + ", Initializing pattern model.");
                patternModel = new PatternChoiceModel( ptRb);
                patternModel.buildModel();

                 ptLogger.info(getName() + ", Initializing tour scheduling model.");
                tourSchedulingModel = new TourSchedulingModel( ptRb,  occReferencer);
                tourSchedulingModel.buildModel();

                ptLogger.info(getName() + ", Intializing stop destination.");
                stopDestinationChoiceModel = createStopDestinationChoiceModel(); 
                stopDestinationChoiceModel.buildModel(tazManager);  

                 ptLogger.info(getName() + ", Intializing stop mode choice.");
                tourStopChoiceModel = new TourStopChoiceModel( ptRb);
                tourStopChoiceModel.buildModel();

                 ptLogger.info(getName() + ", Initializing stop duration model.");
                stopDurationModel = new StopDurationModel( ptRb,  globalRb);
                stopDurationModel.buildModel( skims);

                 ptLogger.info(getName() + ", Initializing trip mode choice model.");
                tripModeChoiceModel = new TripModeChoiceModel( ptRb);

                 ptLogger.info(getName() + ", Initializing work-based tour model.");
                workBasedTourModel = new WorkBasedTourModel(ptRb);
        }
    }
    
    /**
     * 
     * @return a StopDestinationChoiceModel of the appropriate type. 
     */
    protected StopDestinationChoiceModel createStopDestinationChoiceModel() {
       StopDestinationChoiceModel model = new StopDestinationChoiceModel(ptRb);
       return model;       
    }

    private void initializeTazManager(){
        if (tazManager == null) {
            String tazManagerClassName = ResourceUtil.getProperty( ptRb,"sdt.taz.manager.class");
            Class tazManagerClass = null;
            tazManager = null;
            try {
                tazManagerClass = Class.forName(tazManagerClassName);
                tazManager = (TazManager) tazManagerClass.newInstance();
            } catch (ClassNotFoundException e) {
                 ptLogger.fatal(tazManagerClass + " not found");
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                 ptLogger.fatal("Can't Instantiate of TazManager of type "+tazManagerClass.getName());
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                 ptLogger.fatal("Illegal Access of TazManager of type "+tazManagerClass.getName());
                throw new RuntimeException(e);
            }
            String tazClassName =  ptRb.getString("sdt.taz.class");
            tazManager.setTazClassName(tazClassName);
            tazManager.readData( globalRb,  ptRb);
            tazManager.setParkingCost(globalRb,ptRb,"alpha2beta.file");
        }

        // read workplace locations from file
        String file = ResourceUtil.getProperty( ptRb, "sdt.employment");
        tazManager.updateWorkersFromSummary(file);
    }

    private void runShortDistanceModels(int numLDTTours){

    	ptLogger.info(getName() + ", Running Short-distance travel models");
    	//We will set the seed each time we call a new model and the seed will either
    	//be the sum of the modelSeed and the personSeed OR if we are in sensitivity testing
    	//mode the seed will be the sum of the 2 fixed seeds plus the clock time in order
    	//to randomize the random number sequence.
    	Random random = new Random();  //we will set the seed each time

    	workMCLogsums = new Matrix[TourModeChoiceLogsumManager.TOTALSEGMENTS];
    	for (int segment = 0; segment < TourModeChoiceLogsumManager.TOTALSEGMENTS; segment++) {
    		workMCLogsums[segment] = MCLogsumsInMemory.mcLogsumsInMemory[ActivityPurpose.WORK_BASED.ordinal()][segment];
    	}
		
    	for (PTHousehold household : households) {

    		//check to see if the household is making a long-distance
    		//tour on the model day - if so, go to the next household
    		//(the LDT models have taken care of this hh's tours)
    		if(household.isHhMakingALdtOnModelDay()){
    			continue;
    		}

    		if (tracer.isTraceHousehold(household.ID)) {
    			ptLogger.info(getName() + ", " + household.summary());
    		}

    		int segment = IncomeSegmenter.calcLogsumSegment(household.income, household.autos, household.workers);

    		for (PTPerson person : household.persons) {

    			//no reason to process a person making a long-distance
    			//tour because you can't do both.
    			if(PTPerson.isPersonMakingALdtOnModelDay(person)){
    				continue;
    			}

    			try {
    				boolean tracePerson = tracer.isTracePerson(person.hhID + "_" + person.memberID);

    				if (tracePerson) {
    					ptLogger.info(getName() + ", Applying PT models to HH " + household.ID + ", Person "
    							+ person.memberID + ".");
    					ptLogger.info(getName() + ", " + person.summary());
    				}



    				ptLogger.debug(getName() + ", Running the daily pattern choice model.");
    				double patternModelLogsum = patternModel.getUtility(household, person,  skims.pkDist);

    				if(sensitivityTestingMode) random.setSeed(patternModelFixedSeed+person.randomSeed+System.currentTimeMillis());
    				else random.setSeed(patternModelFixedSeed + person.randomSeed);

    				String patternName = patternModel.choosePattern(random).getName();

    				if (tracePerson) {
    					ptLogger.info(getName() + ", " + (person.hhID + "_" + person.memberID)
    							+ " has pattern -> " + patternName);
    				}

    				person.setPattern(new Pattern(patternName));
    				person.setPatternLogsum(patternModelLogsum); 
    				ptLogger.debug(getName() + ", Pattern is set");

    				if(person.weekdayPattern.toString().equals("h")||
    						person.weekdayPattern.toString().equals("H"))
    					continue;

    				person.weekdayTours = PatternChoiceModel.convertToTours(
    						household, person, person.getPattern());

    				person.orderTours();
    				person.prioritizeTours();

    				ptLogger.debug(getName() + ", Running stop choice.");
    				
    				for (int t = 0; t < person.getTourCount(); ++t) {
    					Tour tour = person.weekdayTours[t];

    					if(person.getTourCount()>=3){
    						tourStopChoiceModel.calculateUtilities(household, person, tour);
    						if(sensitivityTestingMode)
    							random.setSeed(tourStopFixedSeed + person.randomSeed + System.currentTimeMillis());
    						else random.setSeed(tourStopFixedSeed + person.randomSeed);

    						tourStopChoiceModel.chooseStopType(random);
    					}
    				}

    				ptLogger.debug(getName() + ", Running the scheduling model.");
    				
    				if(sensitivityTestingMode)
    					random.setSeed(tourSchedulingFixedSeed + person.randomSeed + System.currentTimeMillis());
    				else random.setSeed(tourSchedulingFixedSeed + person.randomSeed);

    				tourSchedulingModel.chooseAllSchedules(household, person,
    						skims, random);
    				
    				ptLogger.debug(getName() + ", Schedule is set");

    				for (int t = 0; t < person.getTourCount(); ++t) {
    					Tour tour = person.weekdayTours[t];

    					ActivityPurpose purpose = tour.getPurpose();
    					Matrix time =  skims.getTimeMatrix(purpose);
    					Matrix dist =  skims.getDistanceMatrix(purpose);

    					Matrix logsum = MCLogsumsInMemory.mcLogsumsInMemory[purpose.ordinal()][segment];

    					Taz dest;

    					if (purpose != ActivityPurpose.WORK && purpose != ActivityPurpose.WORK_BASED) {
    						ptLogger.debug(getName() + ", Running primary destination choice for tour " + tour.tourNumber);

    								dcModel.calculateUtility(household, person, tour, logsum, dist, time);

    								if(sensitivityTestingMode)
    									random.setSeed(tourDestinationFixedSeed + person.randomSeed + System.currentTimeMillis());
    								else random.setSeed(tourDestinationFixedSeed + person.randomSeed);

    								try {
    									dest = dcModel.chooseZone(random);
    								} catch (ModelException e) {
    									ptLogger.debug(getName() + ", Ignoring non-fatal model exception, " + e);
    									ptLogger.warn(getName() + ", Setting destination to home taz.");
    									dest = tazManager.getTazDataHashtable().get((int) person.homeTaz);
    								}
    								tour.primaryDestination.location.zoneNumber = dest.zoneNumber;
    					} else {
    						ptLogger.debug(getName() + ", Using work TAZ " + person.workTaz);
    						dest = tazManager.getTazDataHashtable().get((int) person.workTaz);
    						tour.primaryDestination.location.zoneNumber = person.workTaz;
    					}
    					ptLogger.debug(getName() + ", Primary destination is set");

    					if (tracePerson) {
    						ptLogger.info(getName() + ", Chose destination: "
    								+ tour.primaryDestination.location.zoneNumber);
    					}
    					Taz orig = tazManager.getTaz(tour.begin.location.zoneNumber);

    					ptLogger.debug(getName() + ", Running tour mode choice from " + orig.getZoneNumber() + " to taz  "
    							+ dest.getZoneNumber() + " for tour number " + tour.tourNumber);
    					tourMC.setAttributes(household, person, tour,  skims, orig, dest);
    					tourMC.calculateUtility();                        

    					if(sensitivityTestingMode)
    						random.setSeed(tourModeFixedSeed + person.randomSeed + System.currentTimeMillis());
    					else random.setSeed(tourModeFixedSeed + person.randomSeed);

    					tour.primaryMode = tourMC.chooseMode(random);
    					ptLogger.debug(getName() + ", Tour mode is set");

    					if (tracePerson) {
    						ptLogger.info(getName() + ", Chose mode: " + tour.primaryMode);
    					}

    					ptLogger.debug(getName() + ", Running intermediate stop purpose model for tour " + tour.tourNumber);

    					if(sensitivityTestingMode)
    						random.setSeed(stopPurposeFixedSeed + person.randomSeed + System.currentTimeMillis());
    					else random.setSeed(stopPurposeFixedSeed + person.randomSeed);
    					iStopPurposeModel.selectStopPurpose(tour, person, random);


    					ptLogger.debug(getName() + ", Running stop location choice for tour " + tour.tourNumber);
    					
    					if(sensitivityTestingMode)
    						random.setSeed(stopDestinationFixedSeed + person.randomSeed + System.currentTimeMillis());
    					else random.setSeed(stopDestinationFixedSeed + person.randomSeed);
    					
    					stopDestinationChoiceModel.calculateStopZones(household, person, tour,  skims, random);

    					if (tour.intermediateStop1 != null) {
    						ptLogger.debug("Running stop duration choice for stop 1 for tour " + tour.tourNumber);
    						ptLogger.debug("From zone " + tour.begin.location.zoneNumber
    								+ " to "  + tour.intermediateStop1.location.zoneNumber);

    						ptLogger.debug("Calculating stop duration utilities");
    						
    						stopDurationModel.calculateUtilities(person, tour,tour.intermediateStop1);

    						if(sensitivityTestingMode)
    							random.setSeed(stopDuration1FixedSeed + person.randomSeed + System.currentTimeMillis());
    						else random.setSeed(stopDuration1FixedSeed + person.randomSeed);
    						short duration = (short) stopDurationModel.chooseDuration(random);
    						tour.intermediateStop1.duration = duration;

    						if (tracer.isTracePerson(person.hhID + "_" + person.memberID)) {
    							ptLogger.info(getName() + ", Stop 1 duration: " + duration);
    							ptLogger.info(getName() + ", Stop 1 start: "
    									+ tour.intermediateStop1.startTime);
    							ptLogger.info(getName() + ", Stop 1 end: "
    									+ tour.intermediateStop1.endTime);
    						}
    					} else {
    						// assign a start time from the tour begin activity
    						tour.primaryDestination.startTime = tour.begin.endTime;
    					}

    					if (tour.intermediateStop2 != null) {
    						ptLogger.debug("Running stop duration choice.");
    						stopDurationModel.calculateUtilities(person, tour, tour.intermediateStop2);
    						if(sensitivityTestingMode)
    							random.setSeed(stopDuration2FixedSeed + person.randomSeed + System.currentTimeMillis());
    						else random.setSeed(stopDuration2FixedSeed + person.randomSeed);
    						short duration = (short) stopDurationModel.chooseDuration(random);
    						tour.intermediateStop2.duration = duration;

    						if (tracer.isTracePerson(person.hhID + "_" + person.memberID)) {
    							ptLogger.info(getName() + ", Stop 2 duration: " + duration);
    							ptLogger.info(getName() + ", Stop 2 start: "
    									+ tour.intermediateStop2.startTime);
    							ptLogger.info(getName() + ", Stop 2 end: "
    									+ tour.intermediateStop2.endTime);
    						}
    					} else {
    						// assign an end time from the tour end activity
    						tour.primaryDestination.endTime = tour.end.startTime;
    					}

    					ptLogger.debug(getName() + ", Running trip mode choice.");
    					if(sensitivityTestingMode)
    						random.setSeed(tripModeFixedSeed + person.randomSeed + System.currentTimeMillis());
    					else random.setSeed(tripModeFixedSeed + person.randomSeed);
    					tripModeChoiceModel.calculateTripModes(household,
    							person, tour,  skims, tazManager, random);
    					ptLogger.debug(getName() + ", Trip mode is set");
    				}

    				//process work-based tours
    				if(person.weekdayWorkBasedTours!=null){
    					Tour[] wbTours = person.weekdayWorkBasedTours;


    					for (Tour wbTour : wbTours) {
    						int parentTourNumber = wbTour.parentTourNumber;
    						wbTour.setWorkBasedTourAttributes(person.weekdayTours[parentTourNumber]);
    						if(sensitivityTestingMode)
    							random.setSeed(workBasedFixedSeed + person.randomSeed + wbTour.tourNumber + System.currentTimeMillis());
    						else random.setSeed(workBasedFixedSeed + person.randomSeed + wbTour.tourNumber);

    						workBasedTourModel.calculateWorkBasedTour(household,
    								person, wbTour,  skims, workMCLogsums, tazManager,
    								dcModel, tourMC, tripModeChoiceModel, random);
    					}
    				}
    			} catch (ModelException e) {
    				ptLogger.error("Caught an exception processing person: "
    						+ (person.hhID + "_" + person.memberID));
    				ptLogger.error("Summarizing person: " + (person.hhID + "_" + person.memberID));

    				ptLogger.error("Home-Based Tours:");
    				for (int n = 0; n < person.getTourCount(); ++n) {
    					Tour tour = person.weekdayTours[n];
    					tour.print();
    				}
    				//print the work-based tours
    				if(person.weekdayWorkBasedTours!=null){
    					ptLogger.error("Work-Based Tours:");
    					Tour[] wbTours = person.weekdayWorkBasedTours;
    					for (Tour tour : wbTours) {
    						tour.print();
    						ptLogger.info(getName() + ", Parent tour number " + tour.parentTourNumber);
    					}
    				}
    				ptLogger.error(person.summary());
    				throw new RuntimeException(e);
    			}

    		}
    	}


    	Message returnMsg = createMessage();
    	returnMsg.setValue("households", households);
    	returnMsg.setValue("ldtToursExpected", numLDTTours);
    	if(households[0].isVisitor()){
    		returnMsg.setId(MessageID.VISITOR_HHS_PROCESSED);
    		ptLogger.info(getName() + ", Sending Visitor HHs to results queue.");
    	} else {
    		returnMsg.setId(MessageID.HOUSEHOLDS_PROCESSED);
    		ptLogger.info(getName() + ", Sending HHs to results queue.");
    	}
    	sendTo("ResultsWriterQueue", returnMsg);

    }


}
