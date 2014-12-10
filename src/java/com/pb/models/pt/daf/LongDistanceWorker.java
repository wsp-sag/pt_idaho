/*
 * Copyright 2006 PB Consult Inc.
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
 * Created on Sep 26, 2006 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.IncomeSegmenter;
import com.pb.models.pt.PriceConverter;
import com.pb.models.pt.TazManager;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.pt.util.Synchronizable;
import com.pb.models.pt.ldt.*;
import com.pb.models.utils.Tracer;
import com.pb.models.utils.StatusLogger;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * @author Andrew Stryker
 * @version 0.1
 */
public class LongDistanceWorker extends MessageProcessingTask {
    protected static Logger logger = Logger.getLogger(LongDistanceWorker.class);
    
    protected static final Object lock = new Object();
    
    public static SkimsInMemory skims;
    
    private ResourceBundle globalRb;

    private ResourceBundle ptRb;

    private TazManager tazManager = null;

    private Tracer tracer = Tracer.getTracer();

    private boolean firstMessage = true;

    private boolean sensitivityTestingMode;

    //used by status logger
    private int timePeriod;

    // LDT
    private LDSchedulingModel ldSchedulingModel;

    private LDInternalExternalModel ldInternalExternalModel;

    private LDExternalDestinationModel ldExternalDestinationModel;

    private LDInternalDestinationChoiceModel ldInternalDestinationChoiceModel;

    private LDExternalModeChoiceModel ldExternalModeChoiceModel;

    private LDInternalModeChoiceModel ldInternalModeChoiceModel;

    private LDAutoDetailsModel ldAutoDetailsModel;


    public void onStart() {
        logger.info(getName() + ", Initializing PT Model on Node");
        // We need to read in the Run Parameters (timeInterval and
        // pathToResourceBundle) from the RunParams.properties file
        // that was written by the Application Orchestrator
        String scenarioName;
        int timeInterval;
        int baseYear;
        String pathToPtRb;
        String pathToGlobalRb;

        logger.info(getName() + ", Reading RunParams.properties file");
        ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
        scenarioName = ResourceUtil.getProperty(runParamsRb, "scenarioName");
        logger.info(getName() + ", Scenario Name: " + scenarioName);
        baseYear = Integer.parseInt(ResourceUtil.getProperty(runParamsRb, "baseYear"));
        logger.info(getName() + ", Base Year: " + baseYear);
        timeInterval = Integer.parseInt(ResourceUtil.getProperty(runParamsRb, "timeInterval"));
        timePeriod = timeInterval;
        logger.info(getName() + ", Time Interval: " + timeInterval);
        pathToPtRb = ResourceUtil.getProperty(runParamsRb, "pathToAppRb");
        logger.info(getName() + ", ResourceBundle Path: " + pathToPtRb);
        pathToGlobalRb = ResourceUtil.getProperty(runParamsRb, "pathToGlobalRb");
        logger.info(getName() + ", ResourceBundle Path: " + pathToGlobalRb);

        ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));

        sensitivityTestingMode = ResourceUtil.getBooleanProperty(ptRb, "pt.sensitivity.testing", false);
                logger.info(getName() + ", Sensitivity Testing: " + sensitivityTestingMode);

        boolean calculateLDT = Boolean
                .parseBoolean(ResourceUtil.getProperty(ptRb, "sdt.calculate.ldt"));
        if (calculateLDT) {
            initialize();
        }
    }

    public void onMessage(Message msg) {
        logger.info(getName() + ", Received messageId=" + msg.getId()
                + " message from=" + msg.getSender() + " at " + new Date());

        if (msg.getId().equals(MessageID.HOUSEHOLDS_PROCESSED)) {
            processLongDistanceHouseholds(msg);
        }
    }

    private void initialize(){
        // set-up the Trace object
        tracer.readTraceSettings(ptRb);

        // initialize the tazManager
        if (tazManager == null) {
            String tazManagerClassName = ResourceUtil.getProperty(ptRb,"sdt.taz.manager.class");
            String tazClassName = ptRb.getString("sdt.taz.class");
            Class tazManagerClass;
            tazManager = null;
            try {
                tazManagerClass = Class.forName(tazManagerClassName);
                tazManager = (TazManager) tazManagerClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating TazManager class", e);
            }
            tazManager.setTazClassName(tazClassName);
            tazManager.readData(globalRb, ptRb);
        }
//        int lowMax = ResourceUtil.getIntegerProperty(ptRb,"pt.low.max.income",20000);
//        int highMax = ResourceUtil.getIntegerProperty(ptRb,"pt.med.high.max.income",60000);         
        PriceConverter priceConverter = PriceConverter.getInstance(globalRb,ptRb);
        //PRICE, not INCOME, because this is coming from a property file, not from synthetic population income levels
        int lowMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(ptRb,"pt.low.max.income",20000),PriceConverter.ConversionType.PRICE);
        int highMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(ptRb,"pt.med.high.max.income",60000),PriceConverter.ConversionType.PRICE);

        IncomeSegmenter.setIncomeCategoryRanges(lowMax, highMax);

        //Now reading all the skims in SkimInMemory Class - [AK]
        /*
        // initialize the skims in memory
    	LDSkimsInMemory LDSkims = LDSkimsInMemory.getInstance();
    	LDSkims.readSkimsIntoMemory(globalRb, ptRb);
		*/
      
    }
    
    private void initializeModels(){
        // initialize the models
        skims = SkimsInMemory.getSkimsInMemory();
        
        ldSchedulingModel = new LDSchedulingModel(ptRb);

        String internalExternalModelClassName = ResourceUtil.getProperty(ptRb,"ldt.internal.external.destination.model.class",null);
        if (internalExternalModelClassName !=null) {
            Class intExtModelClass;
            try {
                intExtModelClass = Class.forName(internalExternalModelClassName);
                ldInternalExternalModel = (LDInternalExternalModel) intExtModelClass.newInstance();
                ldInternalExternalModel.initializeObject(ptRb, globalRb, tazManager);
            } catch (Exception e) {
                throw new RuntimeException("Error instantiating LDInternalExternal class", e);
            }
        } else {
            ldInternalExternalModel = new LDInternalExternalModel(ptRb, globalRb, tazManager);
        }


        ldInternalModeChoiceModel = new LDInternalModeChoiceModel(globalRb, ptRb, tazManager);

        ldInternalDestinationChoiceModel = new LDInternalDestinationChoiceModel(
                globalRb, ptRb, tazManager, ldInternalModeChoiceModel);

        ldExternalModeChoiceModel = LDExternalModeChoiceModel.newInstance(globalRb, ptRb);

        ldExternalDestinationModel = LDExternalDestinationModel.newInstance(globalRb, ptRb);

        ldAutoDetailsModel = new LDAutoDetailsModel(ptRb);
    }

    public void processLongDistanceHouseholds(Message message) {

        if(firstMessage){
        	//initialize models
        	synchronized (lock) {
        		initializeModels();
        	}
            // read workplace locations from file
            String fileName = ResourceUtil.getProperty(ptRb, "sdt.current.employment");
            tazManager.updateWorkersFromSummary(fileName);
            firstMessage = false;
        }
        
        LDTour[] tours = (LDTour[]) message.getValue("tours");
        Message returnMessage = createMessage();
        returnMessage.setId(MessageID.LDT_HOUSEHOLDS);
        
        for (LDTour tour : tours) {
            tour.schedule = ldSchedulingModel.chooseSchedule(tour, sensitivityTestingMode);
            tour.destinationType = ldInternalExternalModel.chooseInternalExternal(tour, sensitivityTestingMode);

            if (tour.destinationType == LDTourDestinationType.EXTERNAL) {
                tour.destinationTAZ = ldExternalDestinationModel.chooseTaz(tour, sensitivityTestingMode);
                tour.distance = ldExternalModeChoiceModel.getDistance(tour);    
                
                // apply the correct model depending on the destination
                tour.modeChoiceHaloFlag = ldExternalDestinationModel.isDestinationInModeChoiceHalo(tour);
                if (tour.modeChoiceHaloFlag) {
                    tour.mode = ldInternalModeChoiceModel.chooseMode(tour, sensitivityTestingMode);
                    tour.outboundTime = ldInternalModeChoiceModel.getOutboundTravelTime(tour);
                    tour.inboundTime  = ldInternalModeChoiceModel.getInboundTravelTime(tour);                    
                } else {
                    tour.mode = ldExternalModeChoiceModel.chooseMode(tour, sensitivityTestingMode);
                }
            } else {
                tour.destinationTAZ = ldInternalDestinationChoiceModel.chooseTaz(tour, sensitivityTestingMode);
                tour.distance = ldInternalDestinationChoiceModel.getDistance(tour);
                tour.mode = ldInternalModeChoiceModel.chooseMode(tour, sensitivityTestingMode);
                tour.outboundTime = ldInternalModeChoiceModel.getOutboundTravelTime(tour);
                tour.inboundTime = ldInternalModeChoiceModel.getInboundTravelTime(tour);
            }
            tour.tripMode = ldAutoDetailsModel.chooseTripMode(tour, sensitivityTestingMode);
            tour.nearestAirport = ldAutoDetailsModel.chooseAirportTaz(tour);
        }

        StatusLogger.logText("pt.ld.tours","LD Tours: " + tours.length + " - t" + timePeriod);

        returnMessage.setValue("tours", tours);
        if (message.getSender().equals(RunLDTModels.class.toString())) {
            RunLDTModels.writeLongDistanceResults(tours, ptRb, globalRb);
        } else {
            sendTo("ResultsWriterQueue", returnMessage);
        }
    }


}
