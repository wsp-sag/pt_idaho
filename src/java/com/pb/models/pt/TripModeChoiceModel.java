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
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.tripmodes.BikeTrip;
import com.pb.models.pt.tripmodes.DriveAlone;
import com.pb.models.pt.tripmodes.DriveTransitTrip;
import com.pb.models.pt.tripmodes.SchoolBusTrip;
import com.pb.models.pt.tripmodes.SharedRide2;
import com.pb.models.pt.tripmodes.SharedRide3Plus;
import com.pb.models.pt.tripmodes.WalkTransitTrip;
import com.pb.models.pt.tripmodes.WalkTrip;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.pt.util.TravelTimeAndCost;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;
import static com.pb.models.pt.TripModeParameters.NEST;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.ResourceBundle;

/** 
 * This class implements a logit model to choose a mode for a trip
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

 
public class TripModeChoiceModel extends TimedModel {
    final static Logger logger = Logger.getLogger(TripModeChoiceModel.class);

    final static int debugID = -1;

    private Tracer tracer = Tracer.getTracer();

    private boolean trace = false;

    boolean wroteOutNullTripMode = false;
    
    boolean writeDebugFiles; 

    LogitModel root;
    LogitModel autoNest;
    LogitModel nonMotorNest;
    LogitModel transitNest;
    
    DriveAlone driveAlone;
    SharedRide2 sharedRide2;
    SharedRide3Plus sharedRide3Plus;
    WalkTrip walkTrip;
    BikeTrip bikeTrip;
    SchoolBusTrip schoolBus;
    DriveTransitTrip driveTransitTrip;
    WalkTransitTrip walkTransitTrip;
    
    protected float[][] parameters;

    protected float[] purposeParams;

    ResourceBundle rb;
    TravelTimeAndCost cost = new TravelTimeAndCost();
    TripModePersonAttributes personAttributes = new TripModePersonAttributes();
    ZoneAttributes zoneAttributesDestination = new ZoneAttributes();
    ZoneAttributes zoneAttributesOrigin = new ZoneAttributes();

    
    /**
     * Constructor. Sets up model structure and reads trip mode parameters.
     * @param rb Resource Bundle
     */
     public TripModeChoiceModel(ResourceBundle rb) {
        this.rb = rb;
        startTiming();
        
        //trip definitions
        driveAlone = new DriveAlone();
        sharedRide2 = new SharedRide2();
        sharedRide3Plus = new SharedRide3Plus();
        walkTrip = new WalkTrip();
        bikeTrip = new BikeTrip();
        schoolBus = new SchoolBusTrip();
        driveTransitTrip = new DriveTransitTrip();
        walkTransitTrip = new WalkTransitTrip();
        
        autoNest = new LogitModel("Auto Nest");
        autoNest.addAlternative(driveAlone);
        autoNest.addAlternative(sharedRide2);
        autoNest.addAlternative(sharedRide3Plus);
        autoNest.addAlternative(schoolBus);
        
        nonMotorNest = new LogitModel("Non Motor Nest");
        nonMotorNest.addAlternative(walkTrip);
        
        transitNest = new LogitModel("Transit Nest");
        transitNest.addAlternative(walkTransitTrip);

        //moved this to the calculate method to paramaterize
//        autoNest.setDispersionParameter(1.0/0.7);
//        nonMotorNest.setDispersionParameter(1.0/0.7);
//        transitNest.setDispersionParameter(1.0/0.7);
        
        root = new LogitModel("root");
        root.addAlternative(autoNest);
        root.addAlternative(nonMotorNest);
        root.addAlternative(transitNest);

        writeDebugFiles = ResourceUtil.getBooleanProperty(rb, "sdt.write.debug.files", false);
        
        String fileName = ResourceUtil.getProperty(rb,
                "sdt.trip.mode.parameters");

        TableDataSet paramTable;
        try {
            CSVFileReader reader = new CSVFileReader();
            File paramFile = new File(fileName);
            paramTable = reader.readFile(paramFile);
        } catch (IOException e) {
            logger.fatal("Can't read TripModeParameters input table "
                    + fileName);
            throw new RuntimeException(e);
        }

        parameters = paramTable.getValues();


        logger.info("Parameters is " + parameters.length + " X "
                + parameters[0].length);

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
      * Calculate 
      * @param thisHousehold household
      * @param thisPerson person
      * @param thisTour tour
      * @param skims SkimsInMemory
      * @param tazs tazs
      * @param random Random number generator
      */
     public void calculateTripModes(PTHousehold thisHousehold, 
                                    PTPerson thisPerson, 
                                    Tour thisTour, 
                                    SkimsInMemory skims,
                                    TazManager tazs,
                                    Random random
                                    ){
               
         startTiming();

         trace = tracer.isTracePerson(thisPerson.hhID + "_" + thisPerson.memberID);
 
         int trips = thisTour.getNumberOfTrips();
         purposeParams = parameters[thisTour.primaryDestination.activityPurpose.ordinal()];
         if (thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK_BASED)
            purposeParams = parameters[ActivityPurpose.WORK.ordinal()];
         if (thisTour.begin.activityPurpose == ActivityPurpose.WORK)
            purposeParams = parameters[ActivityPurpose.WORK_BASED.ordinal()];

         float dispersionParameter = 1.0f/purposeParams[NEST];
         autoNest.setDispersionParameter(dispersionParameter);
         nonMotorNest.setDispersionParameter(dispersionParameter);
         transitNest.setDispersionParameter(dispersionParameter);
         
         //iterate through trips on this tour
         for(int i=1;i<=trips;++i){

             if(trace){
                 logger.info("Tracing trip mode choice calculations for "+thisPerson.hhID + "_" + thisPerson.memberID);
                logger.info("Trip "+i);
             }
             
             Activity originActivity = thisTour.getOriginActivity(i);
             Activity destinationActivity = thisTour.getDestinationActivity(i);
             //calculate activity duration, as it is used in utility calculations
             originActivity.calculateDuration();
             destinationActivity.calculateDuration();
             int originTaz = originActivity.location.zoneNumber;
             int destinationTaz = destinationActivity.location.zoneNumber;
             short departTime = originActivity.endTime;
             cost = skims.setTravelTimeAndCost(cost, originTaz, destinationTaz, departTime);
             
             //All trips on WALK tours should be assigned WALKTRIP
             if(thisTour.primaryMode.type == TourModeType.WALK){
                destinationActivity.tripMode = TripModeType.WALK;
                destinationActivity.distanceToActivity = cost.walkDistance;
                destinationActivity.timeToActivity = (short) cost.walkTime;
             }
         
             //All trips on BIKE tours should be assigned BIKETRIP
             if(thisTour.primaryMode.type == TourModeType.BIKE){
                 destinationActivity.tripMode = TripModeType.BIKE;
                 destinationActivity.distanceToActivity = cost.bikeDistance;
                 destinationActivity.timeToActivity = (short) cost.bikeTime;
             } 
             
             root.setDebug(trace);
             driveAlone.setTrace(trace);
             sharedRide2.setTrace(trace);
             sharedRide3Plus.setTrace(trace);
             schoolBus.setTrace(trace);
             walkTrip.setTrace(trace);
             walkTransitTrip.setTrace(trace);
             
             
             //Trips on WALKTRANSIT tours should have a choice between WALK and WALKTRANSIT
             if(thisTour.primaryMode.type == TourModeType.WALKTRANSIT){
                 personAttributes.setAttributes(thisHousehold,thisPerson, thisTour, i);
                 driveAlone.setUtility(-999);
                 sharedRide2.setUtility(-999);
                 sharedRide3Plus.setUtility(-999);
                 schoolBus.setUtility(-999);
                 walkTrip.calcUtility(cost, purposeParams, personAttributes);
                 walkTransitTrip.calcUtility(cost, purposeParams, personAttributes);
                 
                 Mode tripMode = chooseMode(cost,
                         personAttributes,
                         thisTour.primaryMode,
                         thisTour.primaryDestination.activityPurpose,
                         zoneAttributesDestination,
                         thisHousehold.ID,
                         thisPerson.memberID,
                         random
                         );        
                 destinationActivity.tripMode = (TripModeType) tripMode.type;
                 destinationActivity.distanceToActivity = cost.driveAloneDistance;
                 destinationActivity.timeToActivity = (short) tripMode.time;             
             }
         
             // DRIVETRANSIT Tours should be either DRIVETRANSIT trips or choose between WALK or WALKTRANSIT
//             if(thisTour.primaryMode.type == TourModeType.DRIVETRANSIT){
//
//                if(i==1 || i==trips){
//                    //first and last trips on DRIVETRANSIT tours should be DRIVETRANSIT
//                    destinationActivity.tripMode = TripModeType.DR_TRAN;
//                    destinationActivity.distanceToActivity = cost.driveAloneDistance;
//                    destinationActivity.timeToActivity = (short) (cost.driveTransitWalkTime
//                        + cost.driveTransitDriveTime + cost.driveTransitTotalWaitTime + cost.driveTransitInVehicleTime);
//                 }else{
//                    //and middle trips should choose between WALK and WALKTRANSIT
//                     personAttributes.setAttributes(thisHousehold,thisPerson, thisTour, i);
//                     driveAlone.setUtility(-999);
//                     sharedRide2.setUtility(-999);
//                     sharedRide3Plus.setUtility(-999);
//                     schoolBus.setUtility(-999);
//                     walkTrip.calcUtility(cost, purposeParams, personAttributes);
//                     walkTransitTrip.calcUtility(cost, purposeParams, personAttributes);
//
//                     Mode tripMode = chooseMode(cost,
//                             personAttributes,
//                             thisTour.primaryMode,
//                             thisTour.primaryDestination.activityPurpose,
//                             zoneAttributesDestination,
//                             thisHousehold.ID,
//                             thisPerson.memberID,
//                             random
//                             );
//                     destinationActivity.tripMode = (TripModeType) tripMode.type;
//                     destinationActivity.distanceToActivity = cost.driveAloneDistance;
//                     destinationActivity.timeToActivity = (short) tripMode.time;
//
//                 }
//             }
 
             
             //AUTODRIVER Tours should choose between DRIVEALONE, SHARED2, SHARED3PLUS
             if(thisTour.primaryMode.type == TourModeType.AUTODRIVER){
                personAttributes.setAttributes(thisHousehold,thisPerson, thisTour, i);
                 if ((thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK
                         || thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK_BASED) &&
                         destinationActivity.activityType == ActivityType.PRIMARY_DESTINATION)
                    //if activity duration is 0, then the activity took place during the one hour, so just set the duration to 1 hour
                    zoneAttributesDestination.parkingCost = ((tazs.getTaz(destinationTaz)).workParkingCost /
                            (destinationActivity.duration == 0 ? 60 : destinationActivity.duration)) * 60;
                 else
                    zoneAttributesDestination.parkingCost=(tazs.getTaz(destinationTaz)).nonWorkParkingCost;
                 zoneAttributesDestination.terminalTime=(tazs.getTaz(destinationTaz)).terminalTime;
                 zoneAttributesOrigin.terminalTime=(tazs.getTaz(thisTour.begin.location.zoneNumber)).terminalTime;
                 
                driveAlone.calcUtility( cost, zoneAttributesOrigin, zoneAttributesDestination, purposeParams, personAttributes, thisTour.primaryMode, destinationActivity);
                sharedRide2.calcUtility( cost, zoneAttributesOrigin, zoneAttributesDestination, purposeParams, personAttributes, thisTour.primaryMode, destinationActivity);
                sharedRide3Plus.calcUtility( cost, zoneAttributesOrigin, zoneAttributesDestination, purposeParams, personAttributes, thisTour.primaryMode, destinationActivity);
                schoolBus.setUtility(-999);
                walkTrip.setUtility(-999);
                walkTransitTrip.setUtility(-999);
                 
                Mode tripMode = chooseMode(cost,
                        personAttributes,
                        thisTour.primaryMode,
                        thisTour.primaryDestination.activityPurpose,
                        zoneAttributesDestination,
                        thisHousehold.ID,
                        thisPerson.memberID,
                        random
                        );        
                destinationActivity.tripMode = (TripModeType) tripMode.type;
                destinationActivity.distanceToActivity = cost.driveAloneDistance;
                destinationActivity.timeToActivity = (short) tripMode.time;             
             
             }
             
             //AUTOPASSENGER Tours should choose between SHARED2, SHARED3PLUS, and WALK trips
             //TRANSITPASSENGER and PASSENGERTRANSIT Tours should choose between  SHARED2, SHARED3PLUS, and WALK trips or WALK AND TRANSIT TRIPS
             if(thisTour.primaryMode.type == TourModeType.AUTOPASSENGER || 
                     thisTour.primaryMode.type == TourModeType.PASSENGERTRANSIT || thisTour.primaryMode.type == TourModeType.TRANSITPASSENGER){

                personAttributes.setAttributes(thisHousehold,thisPerson, thisTour, i);
                 if ((thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK
                         || thisTour.primaryDestination.activityPurpose == ActivityPurpose.WORK_BASED) &&
                         destinationActivity.activityType == ActivityType.PRIMARY_DESTINATION)
                    //if activity duration is 0, then the activity took place during the one hour, so just set the duration to 1 hour
                    zoneAttributesDestination.parkingCost = ((tazs.getTaz(destinationTaz)).workParkingCost /
                            (destinationActivity.duration == 0 ? 60 : destinationActivity.duration)) * 60;
                 else
                    zoneAttributesDestination.parkingCost=(tazs.getTaz(destinationTaz)).nonWorkParkingCost;
                 zoneAttributesDestination.terminalTime=(tazs.getTaz(destinationTaz)).terminalTime;
                 zoneAttributesOrigin.terminalTime=(tazs.getTaz(thisTour.begin.location.zoneNumber)).terminalTime;
                 
                if(personAttributes.passengerLeg==1){
                    driveAlone.setUtility(-999);
                    sharedRide2.calcUtility( cost, zoneAttributesOrigin, zoneAttributesDestination, purposeParams, personAttributes, thisTour.primaryMode, destinationActivity);
                    sharedRide3Plus.calcUtility( cost, zoneAttributesOrigin, zoneAttributesDestination, purposeParams, personAttributes, thisTour.primaryMode, destinationActivity);
                    schoolBus.calcUtility( purposeParams,  thisTour.primaryMode, thisTour.primaryDestination.activityPurpose);
                    walkTrip.calcUtility(cost, purposeParams, personAttributes);            
                    walkTransitTrip.setUtility(-999);
                }else if(personAttributes.transitLeg==1){
                    driveAlone.setUtility(-999);
                    sharedRide2.setUtility(-999);
                    sharedRide3Plus.setUtility(-999);
                    schoolBus.setUtility(-999);
                    walkTrip.calcUtility(cost, purposeParams, personAttributes);            
                    walkTransitTrip.calcUtility(cost, purposeParams, personAttributes);
                }
                Mode tripMode = chooseMode(cost,
                        personAttributes,
                        thisTour.primaryMode,
                        thisTour.primaryDestination.activityPurpose,
                        zoneAttributesDestination,
                        thisHousehold.ID,
                        thisPerson.memberID,
                        random
                        );        
                destinationActivity.tripMode = (TripModeType) tripMode.type;
                destinationActivity.distanceToActivity = cost.driveAloneDistance;
                destinationActivity.timeToActivity = (short) tripMode.time;             
             
             }
          }  //done looping on trips
         endTiming();
     }


         /**
          * Choose a mode for the trip; utilities should already have been set for all trip modes.
          * 
          * @param tc  travelTimeAndCosts for this zone-pair, for debugging
          * @param thisPerson  Attributes for this trip, for debugging
          * @param tourMode The tour mode for this tour, for debugging
          * @param tourPurpose  The purpose for the primary destination, for debugging
          * @param thisZone The zonal attributes, for debugging
          * @param hhID  A household ID, for debugging
          * @param mID   A member ID, for debugging
          * @param random  A random number
          * @return The chosen TripMode.
          */
         public Mode chooseMode(
                           TravelTimeAndCost tc,
                           TripModePersonAttributes thisPerson,
                           Mode tourMode,
                           ActivityPurpose tourPurpose,
                           ZoneAttributes thisZone,
                           int hhID,
                           int mID,
                           Random random){
             startTiming();
                 
                

            if (trace) {
                root.writeAvailabilities();
            }
            root.computeAvailabilities();

            if (trace) {
                root.writeUtilityHeader();
            }
            double logsum = root.getUtility();

            if (trace) {
                logger.info("Logsum " + logsum);
                root.writeProbabilityHeader();
            }
            root.calculateProbabilities();

            Mode chosenMode = null;
            TripModeType chosenModeType = null;
            try{
                chosenMode = (Mode) root.chooseElementalAlternative(random);
                chosenModeType = (TripModeType) chosenMode.type;
            }catch(Exception e){                
                if(writeDebugFiles) {
                    //A trip mode could not be found.  Create a debug file in the debug directory with
                    //pertinant information and then assign 'SharedRide2' as the trip mode so that the
                    //program can continue running.  The PTDafMaster will check for the existence of debug files at the end
                    //of the PT run and will write out a warning message and move the files into the t# directory.
                    logger.warn("A trip mode could not be found, see HH" + (hhID+"_P"+mID)  +  "TripModeDebugFile.txt.  Location of file is specfied in pt.properties");
                    PrintWriter file = PTResults.createTripModeDebugFile(rb, "HH" + (hhID+"_P"+mID) + "TripModeDebugFile.txt");  // will determine location of debug file and
                                                                                                        // add a header to the file.
                    file.println("Summary:");
                    file.println();
                    file.println("HHID = " + hhID);
                    file.println("MemberID = " + mID);
                    
                    file.println("Tour Origin Zone = " + thisPerson.tourOriginTaz);
                    file.println("Tour Destination Zone = " + thisPerson.tourDestinationTaz);
                    
                    file.println("Origin Zone = " + thisPerson.originTaz);
                    file.println("Destination Zone = " + thisPerson.destinationTaz);
                    
                    file.println("Passenger leg = " + thisPerson.passengerLeg);
                    file.println("Transit leg = " + thisPerson.transitLeg);
                    
                    file.println("TourMode = " + tourMode);   //toString() method returns alternative name.
                    file.println("TourPurpose = "+tourPurpose);
                    file.println("Trip number = "+thisPerson.tripNumber+ " of "+thisPerson.totalTripsOnTour+" trips on tour");
                    
                    file.println("Tour origin depart time "+thisPerson.tourOriginDepartTime);
                    file.println("Tour primary destination depart time "+thisPerson.tourPrimaryDestinationDepartTime);
                    file.println("Trip depart time "+thisPerson.tripDepartTime);
                    
                    file.println();
                    file.flush();
    
                    file.println("Details: ");
                    thisPerson.print(file);      //prints out the person trip mode attributes to the debug file
                    thisZone.print(file);        //prints out the parking cost in the destination zone
                    tc.print(file);                 //prints out the travel time and cost from origin zone to destination zone.
    
                    file.close();
                }
                //Now assign the trip mode to "SharedRide2" and return.
                chosenMode = new SharedRide2();
                chosenMode.time = tc.sharedRide2Time;
                chosenModeType = (TripModeType) chosenMode.type;
              
            }

            if(trace) logger.info("Chose trip mode "+chosenModeType+
            	" for trace tour");

            endTiming();
            return chosenMode;
     }


}
