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
package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.Alternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;

import static com.pb.models.pt.TourModeParameters.NEST;

import com.pb.models.pt.tourmodes.AutoDriver;
import com.pb.models.pt.tourmodes.AutoPassenger;
import com.pb.models.pt.tourmodes.Bike;
//import com.pb.models.pt.tourmodes.DriveTransit;
import com.pb.models.pt.tourmodes.PassengerTransit;
import com.pb.models.pt.tourmodes.TransitPassenger;
import com.pb.models.pt.tourmodes.Walk;
import com.pb.models.pt.tourmodes.WalkTransit;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.pt.util.TravelTimeAndCost;
import com.pb.models.utils.Tracer;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.ResourceBundle;

/** 
 * This model implements a logit model to choose a tour mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TourModeChoiceModel extends TimedModel {
    
     static Logger logger = Logger.getLogger(TourModeChoiceModel.class);
    
     protected Mode chosenMode;
     protected double logsum;
     //Root LogitModel
     protected LogitModel root = new LogitModel("root");
     
     //Mode Nests
     protected LogitModel autoNest = new LogitModel("autoNest");
     protected LogitModel nonMotorizedNest = new LogitModel("nonMotorizedNest");
     protected LogitModel transitNest = new LogitModel("transitNest");
     protected LogitModel passengerNest = new LogitModel("passengerNest");
     
     //Elemental Alternatives                
     protected AutoDriver thisDriver = new AutoDriver();
     protected AutoPassenger thisPassenger = new AutoPassenger();
     protected Walk thisWalk = new Walk();
     protected Bike thisBike = new Bike();
     protected WalkTransit thisWalkTransit = new WalkTransit();
     protected TransitPassenger thisTransitPassenger = new TransitPassenger();
     protected PassengerTransit thisPassengerTransit = new PassengerTransit();
     //protected DriveTransit thisDriveTransit = new DriveTransit();
     
     // stores data for solving model for particular tour
     protected TourModePersonAttributes personAttributes = new TourModePersonAttributes();
     protected TravelTimeAndCost departCost = new TravelTimeAndCost();
     protected TravelTimeAndCost returnCost = new TravelTimeAndCost();
     ZoneAttributes zoneAttributesDestination = new ZoneAttributes();
     ZoneAttributes zoneAttributesOrigin = new ZoneAttributes();
     protected float[][] parameters;
     protected float[] purposeParams;
     
     // store person info for reporting
     private PTPerson currentPerson; 
     private Tour currentTour; 
     private ResourceBundle rb; 

     final static int debugID = 1;
     
     private Tracer tracer = Tracer.getTracer();
     
     private boolean trace;
     
     boolean writeDebugFiles; 
     
     /**
      * Constructor sets up model structure.
      * @param rb ResourceBundle
      */
     public TourModeChoiceModel(ResourceBundle rb) {
         startTiming();
         autoNest.addAlternative(thisDriver);

         nonMotorizedNest.addAlternative(thisWalk);
         nonMotorizedNest.addAlternative(thisBike);
         
         transitNest.addAlternative(thisWalkTransit);
         //transitNest.addAlternative(thisDriveTransit);
         
         passengerNest.addAlternative(thisPassenger);
         passengerNest.addAlternative(thisPassengerTransit);
         passengerNest.addAlternative(thisTransitPassenger);
         
         root.addAlternative(autoNest);
         root.addAlternative(nonMotorizedNest);
         root.addAlternative(transitNest);
         root.addAlternative(passengerNest);
         
         this.rb = rb; 
         
         logger.debug("Adding table TourModeParameters");
         
         String fileName = ResourceUtil.getProperty(rb,
         "sdt.tour.mode.parameters");
         CSVFileReader reader = new CSVFileReader();
         TableDataSet paramTable;
         
         writeDebugFiles = ResourceUtil.getBooleanProperty(rb, "sdt.write.debug.files", false);
         
         try {
             File paramFile = new File(fileName);
             paramTable = reader.readFile(paramFile);
         } catch (IOException e) {
             logger.fatal("Can't find TourModeParameters input table "
                     + fileName);
             throw new RuntimeException(e);
         }
         
         parameters = paramTable.getValues();
         if (!tracer.isTraceOn()) {
             endTiming();
             return;
         }
         
         logger.info("Read parameter values (across by purpose)");
         for (int j = 0; j < parameters[0].length; ++j) {
             String s = "parameter " + j;
             for (float[] parameter : parameters) {
                 s += "," + parameter[j];
             }
             logger.info(s);
         }
         endTiming();
    }
     
     /**
      * Set the attributes for the traveler, tour and skims for a 
      * particular tour mode choice model solve.
      * 
      * @param thisHousehold household
      * @param thisPerson person
      * @param thisTour tour object
      * @param skims SkimsInMemory object
      * @param originTaz destination taz
      * @param destinationTaz destination taz
      */
     public void setAttributes(PTHousehold thisHousehold, PTPerson thisPerson,
            Tour thisTour, SkimsInMemory skims, Taz originTaz, Taz destinationTaz) {
        startTiming();
        
        currentPerson = thisPerson;
         currentTour = thisTour;
 
        trace = tracer.isTracePerson(thisPerson.hhID + "_" + thisPerson.memberID);

        // set travel time and cost
        departCost = skims.setTravelTimeAndCost(departCost,
                thisTour.begin.location.zoneNumber,
                thisTour.primaryDestination.location.zoneNumber,
                thisTour.begin.endTime);

        returnCost = skims.setTravelTimeAndCost(returnCost,
                thisTour.primaryDestination.location.zoneNumber,
                thisTour.end.location.zoneNumber,
                thisTour.primaryDestination.endTime);

        // set taz attributes (only parking cost at this point)
        if (thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK
                || thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK_BASED)
            //zoneAttributesDestination.parkingCost = (destinationTaz.workParkingCost / 60 * thisTour.tourDuration);
        //assume a fixed daily cost for work parking costs
            zoneAttributesDestination.parkingCost = (destinationTaz.workParkingCost / thisTour.tourDuration) * 60;
        else
            zoneAttributesDestination.parkingCost = destinationTaz.nonWorkParkingCost;

        /////////todo//////////
//        if (destinationTaz.getZoneNumber() == 1) {
//            logger.info("TAZ PARKING COST IN ATTRIBUTES: " + zoneAttributesDestination.parkingCost);
//        }
        /////////todo////////
         zoneAttributesDestination.terminalTime = destinationTaz.terminalTime;
         zoneAttributesOrigin.terminalTime = originTaz.terminalTime;
        // set person tour mode attributes
        personAttributes.setAttributes(thisHousehold, thisPerson, thisTour);
        // primary duration caclulation probably incorrect
        personAttributes.primaryDuration = thisTour.tourDuration;
        purposeParams = parameters[personAttributes.tourPurpose.ordinal()];
         //work tour with a subtour (but not the subtour)
        if (personAttributes.tourPurpose == ActivityPurpose.WORK_BASED)
           purposeParams = parameters[ActivityPurpose.WORK.ordinal()];
         //on a work-based subtour
        if (thisTour.begin.activityPurpose == ActivityPurpose.WORK)
            purposeParams = parameters[ActivityPurpose.WORK_BASED.ordinal()];

        this.thisDriver.setAvailability(true);
        this.thisPassenger.setAvailability(true);
        this.thisWalk.setAvailability(true);
        this.thisBike.setAvailability(true);
        this.thisWalkTransit.setAvailability(true);
        this.thisTransitPassenger.setAvailability(true);
        this.thisPassengerTransit.setAvailability(true);
        //this.thisDriveTransit.setAvailability(true);
        endTiming();

        // calculateUtility(params,departCost,returnCost,personAttributes,thisTour,zone);
    }
     
     /**
         * Solve the logit model after the attributes have already been set by
         * the setAttributes method, and return the logsum at the root level.
         * @return utility
         */
    public double calculateUtility() {
        startTiming();

        root.setDebug(trace);
        thisBike.setTrace(trace);
        thisDriver.setTrace(trace);
        //thisDriveTransit.setTrace(trace);
        thisPassenger.setTrace(trace);
        thisPassengerTransit.setTrace(trace);
        thisTransitPassenger.setTrace(trace);
        thisWalk.setTrace(trace);
        thisWalkTransit.setTrace(trace);


        // calculate utilities
        this.thisDriver.calcUtility(departCost, returnCost, zoneAttributesOrigin, zoneAttributesDestination,
                purposeParams, personAttributes);

        this.thisPassenger.calcUtility(departCost, returnCost, zoneAttributesOrigin, zoneAttributesDestination,
                purposeParams, personAttributes);

        this.thisWalk.calcUtility(departCost, returnCost, zoneAttributesDestination,
                purposeParams, personAttributes);

        this.thisBike.calcUtility(departCost, returnCost, purposeParams,
                personAttributes);

        this.thisWalkTransit.calcUtility(departCost, returnCost, purposeParams,
                personAttributes);

        this.thisTransitPassenger.calcUtility(departCost, returnCost,
                purposeParams, personAttributes);

        this.thisPassengerTransit.calcUtility(departCost, returnCost,
                zoneAttributesDestination, purposeParams, personAttributes);

//        this.thisDriveTransit.calcUtility(departCost, returnCost,
//                purposeParams, personAttributes);

        float dispersionParameter = purposeParams[NEST];
        this.autoNest.setDispersionParameter(this.root.getDispersionParameter()/dispersionParameter);
        this.nonMotorizedNest.setDispersionParameter(this.root.getDispersionParameter()/dispersionParameter);
        this.transitNest.setDispersionParameter(this.root.getDispersionParameter()/dispersionParameter);
        this.passengerNest.setDispersionParameter(this.root.getDispersionParameter()/dispersionParameter);

        if (trace) {
            root.writeAvailabilities();
        }

        this.root.computeAvailabilities();

        if (trace) {
            root.writeUtilityHeader();
        }

        logsum = this.root.getUtility();

        endTiming();
        return logsum;
    }

    /**
     * Solve the logit model for the given set of parameters, costs, traveler
     * and taz attributes, and return the logsum at the root level. Need to use
     * setAttributes first.
     * 
     * @param departCost
     *            Costs for journey to primary destination.
     * @param returnCost
     *            Costs for journey back to anchor location.
     * @param personAttributes
     *            Attributes of person making tour.
     * @param zone
     *            Attributes of primary destination zone.
     * @return double utility
     */
    public double calculateUtility(TravelTimeAndCost departCost,
            TravelTimeAndCost returnCost,
            TourModePersonAttributes personAttributes, ZoneAttributes zone) {
        
        root.setDebug(trace);
        thisBike.setTrace(trace);
        thisDriver.setTrace(trace);
//        thisDriveTransit.setTrace(trace);
        thisPassenger.setTrace(trace);
        thisPassengerTransit.setTrace(trace);
        thisTransitPassenger.setTrace(trace);
        thisWalk.setTrace(trace);
        thisWalkTransit.setTrace(trace);
        
        this.thisDriver.setAvailability(true);
        this.thisPassenger.setAvailability(true);
        this.thisWalk.setAvailability(true);
        this.thisBike.setAvailability(true);
        this.thisWalkTransit.setAvailability(true);
        this.thisTransitPassenger.setAvailability(true);
        this.thisPassengerTransit.setAvailability(true);
//        this.thisDriveTransit.setAvailability(true);

        
        purposeParams = parameters[personAttributes.tourPurpose.ordinal()];
        
        //calculate utilities
        
        if (trace) {
            logger.info("Tracing calculations for purpose "
                    + personAttributes.tourPurpose);
        }
        
        this.thisDriver.calcUtility( departCost, returnCost, zoneAttributesOrigin,
                zoneAttributesDestination, purposeParams, personAttributes);
        
        this.thisPassenger.calcUtility( departCost, returnCost,zoneAttributesOrigin,
                zoneAttributesDestination, purposeParams, personAttributes);
        
        this.thisWalk.calcUtility( departCost, returnCost,
                zoneAttributesDestination, purposeParams, personAttributes);
        
        this.thisBike.calcUtility( departCost, returnCost,
                purposeParams, personAttributes);
        
        this.thisWalkTransit.calcUtility( departCost, returnCost,
                purposeParams, personAttributes);
        
        this.thisTransitPassenger.calcUtility( departCost, returnCost,
                purposeParams, personAttributes);
        
        this.thisPassengerTransit.calcUtility( departCost, returnCost,
                zoneAttributesDestination, purposeParams, personAttributes);
        
//        this.thisDriveTransit.calcUtility( departCost, returnCost,
//                purposeParams, personAttributes);
        
        float dispersionParameter = purposeParams[NEST];
        this.autoNest.setDispersionParameter(this.root.getDispersionParameter() / dispersionParameter);
        this.nonMotorizedNest.setDispersionParameter(this.root.getDispersionParameter() / dispersionParameter);
        this.transitNest.setDispersionParameter(this.root.getDispersionParameter() / dispersionParameter);
        this.passengerNest.setDispersionParameter(this.root.getDispersionParameter() / dispersionParameter);
        
        if (trace) {
            root.writeAvailabilities();
        }
        
        this.root.computeAvailabilities();
        
        if (trace) {
            root.writeUtilityHeader();
        }

        //logsum = this.root.getUtility();
        //try-catch block added by crf to trace invalid utility errors - uncomment above line and delete
        // block to rever
        try {
            logsum = this.root.getUtility();
        } catch (Exception e) {
            this.thisDriver.setTrace(true);
            this.thisDriver.calcUtility( departCost, returnCost, zoneAttributesOrigin,
                    zoneAttributesDestination, purposeParams, personAttributes);
            this.thisPassenger.setTrace(true);
            this.thisPassenger.calcUtility( departCost, returnCost,zoneAttributesOrigin,
                    zoneAttributesDestination, purposeParams, personAttributes);
            this.thisWalk.setTrace(true);
            this.thisWalk.calcUtility( departCost, returnCost,
                    zoneAttributesDestination, purposeParams, personAttributes);
            this.thisBike.setTrace(true);
            this.thisBike.calcUtility( departCost, returnCost,
                    purposeParams, personAttributes);
            this.thisWalkTransit.setTrace(true);
            this.thisWalkTransit.calcUtility( departCost, returnCost,
                    purposeParams, personAttributes);
            this.thisTransitPassenger.setTrace(true);
            this.thisTransitPassenger.calcUtility( departCost, returnCost,
                    purposeParams, personAttributes);
            this.thisPassengerTransit.setTrace(true);
            this.thisPassengerTransit.calcUtility( departCost, returnCost,
                    zoneAttributesDestination, purposeParams, personAttributes);
//            this.thisDriveTransit.setTrace(true);
//            this.thisDriveTransit.calcUtility( departCost, returnCost,
//                    purposeParams, personAttributes);
            throw new RuntimeException(e);
        }
        
        endTiming();
        return logsum;
    }
    
    /**
     * Choose a mode from the model.
     * @param random Random number generator
     * @return mode chosen mode
     */
    public Mode chooseMode(Random random) {
        startTiming();

        if (trace) {
            root.writeProbabilityHeader();
        }

        this.root.calculateProbabilities();

        try {
            chosenMode = (Mode) this.root.chooseElementalAlternative(random);
        } catch (Exception e) {
        	logger.debug("A tour mode could not be found. Assigned SR2 as the tour mode.");
        	logger.debug("Summary:");
        	logger.debug("HHID = " + currentPerson.hhID);
        	logger.debug("MemberID = " + currentPerson.memberID);
        	logger.debug("Begin Zone = " + currentTour.begin.location.zoneNumber);
        	logger.debug("Primary Destination Zone = " + currentTour.primaryDestination.location.zoneNumber);
        	logger.debug("End Zone = " + currentTour.end.location.zoneNumber);
        	logger.debug("TourPurpose = "+currentTour.primaryDestination.activityPurpose);
        	logger.debug("**********************");
        	
        	if (writeDebugFiles) {
                //A tour mode could not be found.  Create a debug file in the debug directory with
                //pertinant information and then assign 'SharedRide2' as the tour mode so that the
                //program can continue running.  The PTDafMaster will check for the existence of debug files at the end
                //of the PT run and will write out a warning message and move the files into the t# directory.
                int hhID = currentPerson.hhID;
                int mID  = currentPerson.memberID;
                logger.warn("A tour mode could not be found, see HH" + (hhID+"_P"+mID)  +  "TourModeDebugFile.txt.  Location of file is specfied in pt.properties");
                PrintWriter file = PTResults.createTourModeDebugFile(rb, "HH" + (hhID+"_P"+mID) + "TourModeDebugFile.txt");  // will determine location of debug file and
                                                                                                    // add a header to the file.
                file.println("Summary:");
                file.println();
                file.println("HHID = " + hhID);
                file.println("MemberID = " + mID);
                file.println("Begin Zone = " + currentTour.begin.location.zoneNumber);
                file.println("Primary Destination Zone = " + currentTour.primaryDestination.location.zoneNumber);
                file.println("End Zone = " + currentTour.end.location.zoneNumber);
                file.println("TourPurpose = "+currentTour.primaryDestination.activityPurpose);
                file.println();
                file.flush();
            }

            //Now assign the trip mode to "SharedRide2" and return.
            chosenMode = new AutoPassenger(); 
        }

        if(trace && personAttributes!=null) logger.info("Chose tour mode "+chosenMode+" for trace tour "
        		+personAttributes.tourPurpose);
        endTiming();
        return chosenMode;
    }

    /**
     * Set the availability of drive alone mode (use for
     * work-based tour model.
     * 
     * @param bool True or false.
     */
    public void setDriveAloneAvailability(boolean bool){
        thisDriver.setAvailability(bool);
    }
    
    public void setTrace(boolean trace) {
        this.trace = trace;
    }
}