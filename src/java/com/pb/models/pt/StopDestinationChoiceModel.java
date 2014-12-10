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
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Random;
import java.util.ResourceBundle;

/** 
 * Model for choosing the intermediate stop destination on tours
 * 
 * @author Freedman
 * @version 1.0 12/01/2003
 *  
 */

 
public class StopDestinationChoiceModel extends TimedModel {
     final static Logger logger = Logger
            .getLogger(StopDestinationChoiceModel.class);
     double utility;
     final static int debugID = 68313;
     LogitModel iStop1Model;
     LogitModel iStop2Model;
     protected float[][] iStop1Params;
     protected float[][] iStop2Params; 

     protected float[] iStop1PurposeParams;
     protected float[] iStop2PurposeParams; 
     
     float distanceThreshold = 999;

     // for weighting out-of-vehicle time (auto terminal time) 
     float walkFactor = (float) 3.0;
     
     private Tracer tracer = Tracer.getTracer();
    private ResourceBundle rb;
      
     /**
      * Default constructor.  Reads parameters from pt resource bundle key.
      * @param rb Resource Bundle
      */
     public StopDestinationChoiceModel(ResourceBundle rb) {
         this.rb = rb;
         startTiming();
         // read the tour destination parameters
         logger.info("Reading StopDestinationModelParameters");
         String fileName1 = ResourceUtil.getProperty(rb, "sdt.stop.destination.parameters");
         iStop1Params = readParameters(fileName1); 
         
         // if segmented, read in the second stop parameters, otherwise, assume they're the same
         String fileName2 = ResourceUtil.getProperty(rb, "sdt.stop.destination.parameters2", "not found");
         if (fileName2.equals("not found")) {
             logger.info("StopDestinationModelParameters are same for second stop as first"); 
             iStop2Params = iStop1Params; 
         } else {
             logger.info("Reading StopDestinationModelParamters for second stop"); 
             iStop2Params = readParameters(fileName2); 
         }

         endTiming();
     }
 
     private float[][] readParameters(String fileName) {

         logger.info("Reading StopDestinationModelParameters in " + fileName);

         float[][] parameters; 
         try {
             CSVFileReader reader = new CSVFileReader();
             TableDataSet table = reader.readFile(new File(fileName));
             parameters = table.getValues();
         } catch (IOException e) {
             throw new RuntimeException("Can't find StopDestinationParameters file " + fileName, e);
         }

         logger.info("Parameters is " + parameters.length + " X " + parameters[0].length);

         if (tracer.isTraceOn()) {
             logger.info("Read parameter values (across by purpose)");
             for (int j = 0; j < parameters[0].length; ++j) {
                 String s = "parameter " + j;
                 for (float[] parameter : parameters) {
                     s += "," + parameter[j];
                 }
                 logger.info(s);
             }             
         }
         
         return parameters; 
     }
 
     public void buildModel(TazManager tazs){
         startTiming();

         //load up distance threshold from resource bundle if it exists
         String distanceThreshold = rb.getString("pt.stop.distance.choice.threshold");
         if (distanceThreshold != null)
            this.distanceThreshold = Float.parseFloat(distanceThreshold);

         iStop1Model = new LogitModel("iStop1Model",tazs.size());
         iStop2Model = new LogitModel("iStop2Model",tazs.size());

         //create stop1 model with stop1 tazData
         Enumeration stop1DestinationEnum=tazs.elements();
         while(stop1DestinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) stop1DestinationEnum.nextElement();
             destinationTaz.setStopSizeTerms(iStop1Params);
             destinationTaz.setLnAcres();
             iStop1Model.addAlternative(destinationTaz);
         }

         //create stop2 model with stop2 tazData
         Enumeration stop2DestinationEnum=tazs.elements();
         while(stop2DestinationEnum.hasMoreElements()){
             Taz destinationTaz = (Taz) stop2DestinationEnum.nextElement();
             destinationTaz.setStopSizeTerms(iStop2Params); // won't this override above?
             destinationTaz.setLnAcres();
             iStop2Model.addAlternative(destinationTaz);
         }
         endTiming();
     }

     public void calculateStopZones(PTHousehold thisHousehold,
            PTPerson thisPerson, Tour thisTour, SkimsInMemory skims,
            Random random) {
        startTiming();

        boolean trace = tracer.isTracePerson(thisPerson.hhID + "_" + thisPerson.memberID);
        iStop1Model.setDebug(trace);
        iStop2Model.setDebug(trace);
        
        if (trace) {
            logger.info("Tracing stop destination choice for a tour between "
                    + thisTour.begin.location.zoneNumber + " and "
                    + thisTour.end.location.zoneNumber + " with " +
                      thisTour.primaryDestination.location.zoneNumber);
            logger.info("Distance threshold set to " + distanceThreshold);
            logger.info("HHID " + thisHousehold.ID + ", Person " + thisPerson.memberID
                    + ", Tour " + thisTour.tourNumber + ", ActivityPurpose " + thisTour.getPurpose()
                    + ", Origin "+ thisTour.begin.location.zoneNumber
                    + ", Primary destination "+ thisTour.end.location.zoneNumber
                    + ", Intermediate stop 1:  " + (thisTour.intermediateStop1 == null ? "no" : "yes")
                    + ", Intermediate stop 2:  " + (thisTour.intermediateStop2 == null ? "no" : "yes"));


        }

        // no stops on tour
        if (thisTour.intermediateStop1 == null
                && thisTour.intermediateStop2 == null) {
            if (trace) {
                logger.info("No stops on this tour");
            }
            endTiming();
            return;
        }

        // no begin taz, end Taz, or primaryDestination taz
        if (thisTour.begin.location.zoneNumber == 0
                || thisTour.end.location.zoneNumber == 0
                || thisTour.primaryDestination.location.zoneNumber == 0) {
            logger.error("Not running StopZones model for the following "
                    + "tour due to problems in the begin,end and primary"
                    + " dest TAZ numbers");
            logger.error("Error: begin taz : "
                    + thisTour.begin.location.zoneNumber + " end taz "
                    + thisTour.end.location.zoneNumber + " pd taz "
                    + thisTour.primaryDestination.location.zoneNumber);

            // write the tour information into a debug file. Path is
            // specified in the pt.properties file
            logger.error("Writing Tour Debug info to the debug directory");
            PrintWriter file = PTResults.createTourDebugFile(rb, "HH"
                    + thisHousehold.ID + "Tour" + thisTour.tourNumber + ".csv");
            thisTour.printCSV(file);
            file.close();
            endTiming();
            return;
        }

        //if drive-transit, set the stop zone to the primary destination zone
//        if(thisTour.primaryMode.type == TourModeType.DRIVETRANSIT ){
//            if(thisTour.intermediateStop1 != null){
//                thisTour.intermediateStop1.location.zoneNumber=thisTour.primaryDestination.location.zoneNumber;
//                thisTour.intermediateStop1.distanceToActivity = skims.getDistance(thisTour.begin.endTime,
//                    thisTour.begin.location.zoneNumber,
//                    thisTour.intermediateStop1.location.zoneNumber);
//
//                thisTour.primaryDestination.distanceToActivity = skims.getDistance(thisTour.intermediateStop1.endTime,
//                  thisTour.intermediateStop1.location.zoneNumber,
//                  thisTour.primaryDestination.location.zoneNumber);
//
//            }
//
//            if(thisTour.intermediateStop2 != null){
//                thisTour.intermediateStop2.location.zoneNumber=thisTour.primaryDestination.location.zoneNumber;
//                thisTour.intermediateStop2.distanceToActivity = skims.getDistance(
//                        thisTour.primaryDestination.endTime,
//                        thisTour.primaryDestination.location.zoneNumber,
//                        thisTour.intermediateStop2.location.zoneNumber);
//
//                thisTour.end.distanceToActivity = skims.getDistance(
//                        thisTour.intermediateStop2.endTime,
//                        thisTour.intermediateStop2.location.zoneNumber,
//                        thisTour.end.location.zoneNumber);
//
//            }
//
//        }
        // set up destination choice parameters
        iStop1PurposeParams = iStop1Params[thisTour.primaryDestination.activityPurpose.ordinal()];
        iStop2PurposeParams = iStop2Params[thisTour.primaryDestination.activityPurpose.ordinal()];

        // First run the stop1location model
        if (thisTour.intermediateStop1 != null) { // && thisTour.primaryMode.type != TourModeType.DRIVETRANSIT) {
 
            
             // calculate utilities for each taz. Use the ptModel.tazs that were
            // passed into the method.
            for (Object o : (iStop1Model.getAlternatives())) {
                Taz stop1Taz = (Taz) o;

                float autoTime = 0;
                float walkTime = 0;
                float bikeTime = 0;
                float transitGeneralizedCost = 0;

                // autoDists[0] = distance
                // from begin to primary destination
                // autoDists[1] = distance from begin to stop + stop to primary
                // destination
                // autoDists[2] = distance from stopTaz to HomeTaz

                 
                float[] autoDists = skims.getAdditionalAutoDistance(
                        thisTour.begin.location.zoneNumber,
                        thisTour.primaryDestination.location.zoneNumber,
                        stop1Taz.zoneNumber,thisPerson.homeTaz, thisTour.begin.endTime);

                // check to make sure within distance threshold of anchor location.
                //need to allow stops within same zone, even if it goes over distance threshold
                if ((autoDists[1] - autoDists[0]) > distanceThreshold &&
                        stop1Taz.zoneNumber != thisTour.begin.location.zoneNumber &&
                        stop1Taz.zoneNumber != thisTour.primaryDestination.location.zoneNumber) {
                    stop1Taz.setAvailability(false);
                    if (trace) {  
                         logger.info("Stop 2 Taz not available: " + stop1Taz.zoneNumber +
                         " because prim. dest. to stop to end distance (" + autoDists[1] + ") > distance threshold (" + distanceThreshold + ")");
                    }
                    continue;
                }
                
                // get the distance from home
                float distFromHome = skims.getDistance(thisTour.begin.endTime, thisPerson.homeTaz, stop1Taz.zoneNumber);

                
                //calculate walk time for walk modes and transit modes (in case stop zone is not connected by transit)
                if (thisTour.primaryMode.type == TourModeType.WALK || thisTour.primaryMode.type == TourModeType.WALKTRANSIT
                        || thisTour.primaryMode.type == TourModeType.TRANSITPASSENGER)
                    walkTime = skims.getAdditionalWalkTime(
                            thisTour.begin.location.zoneNumber,
                            thisTour.primaryDestination.location.zoneNumber,
                            stop1Taz.zoneNumber, thisTour.begin.endTime);
                else if (thisTour.primaryMode.type == TourModeType.BIKE)
                    bikeTime = skims.getAdditionalBikeTime(
                            thisTour.begin.location.zoneNumber,
                            thisTour.primaryDestination.location.zoneNumber,
                            stop1Taz.zoneNumber, thisTour.begin.endTime);

                else if (thisTour.primaryMode.type == TourModeType.WALKTRANSIT
                        || thisTour.primaryMode.type == TourModeType.TRANSITPASSENGER)

                    transitGeneralizedCost = skims
                            .getAdditionalGeneralizedTransitCost(
                                    thisTour.begin.location.zoneNumber,
                                    thisTour.primaryDestination.location.zoneNumber,
                                    stop1Taz.zoneNumber, thisTour.begin.endTime);

                else {
                    autoTime = skims.getAdditionalAutoTime(
                            thisTour.begin.location.zoneNumber,
                            thisTour.primaryDestination.location.zoneNumber,
                            stop1Taz.zoneNumber, thisTour.begin.endTime);
                    
                    // include terminal time, (to + from), weighted at 3 * IVT
                    autoTime += 2 * walkFactor * stop1Taz.terminalTime; 
                }

                ActivityPurpose purposeForSizeTerm = getPurposeForFirstStopSizeTerm(thisTour);
                stop1Taz.setTrace(trace);
                stop1Taz.calcStopDestinationUtility(
                        purposeForSizeTerm,
                        iStop1PurposeParams, thisTour.primaryMode,
                        thisTour.begin.location.zoneNumber, thisTour.primaryDestination.location.zoneNumber, 
                        autoTime, walkTime,
                        bikeTime, transitGeneralizedCost, autoDists, 1, distFromHome);

                if (trace) {
                    logger.info("Stop 1 " + stop1Taz.zoneNumber + " utility: "
                            + stop1Taz.getUtility());
                }
            }

            if (trace) {
                iStop1Model.writeAvailabilities();
            }
            iStop1Model.computeAvailabilities();

            if (trace) {
                iStop1Model.writeUtilityHeader();
            }
            iStop1Model.getUtility();

            if (trace) {
                iStop1Model.writeProbabilityHeader();
            }
            iStop1Model.calculateProbabilities();

             try {

                 Taz chosenTaz = (Taz) iStop1Model.chooseAlternative(random.nextDouble());
                 thisTour.intermediateStop1.location.zoneNumber=chosenTaz.zoneNumber;
                 
                 if (trace) {
                     logger.info("Intermediate Stop 1 chosen taz: " + chosenTaz.zoneNumber);
                 }

             } catch(Exception e) {
//                 logger.error(e);
//                 if (1 == 1) {
//                     throw new RuntimeException(e);
//                 }
//                 // this error message is not always true!
                 logger.error("Error in stop destination choice: no zones available for " +
                         "Household "+thisHousehold.ID+" Person "+thisPerson.memberID+" Tour "+thisTour.tourNumber);
//                 logger.error("A Stop1 Tour file and the TAZ info will be written out to the debug directory.");
//
//                //write the tour information into a debug file.  Path is specified in the pt.properties file
//                logger.error("Writing Tour Debug info to the debug directory");
//                PrintWriter file = PTResults.createTourDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop1.csv");
//                thisTour.printCSV(file);
//                file.close();
//
//                // if not already done, write the taz info into a debug file.  Path is specified in pt.properties
//                PrintWriter file2 = PTResults.createTazDebugFile("HH" + thisHousehold.ID + "Tour" + thisTour.tourNumber+"Stop1AvailableAlternatives.csv");
//                if(file2 != null){  //if it is null that means an earlier problem caused this file to be written already and there
//                                       //is no reason to write it out twice
//                    logger.error("Writing out Taz Info because the first intermediate stop on the tour couldn't find a destination");
//                    Iterator alts = iStop1Model.getAlternatives().iterator();
//                    while(alts.hasNext()){
//                        Taz stop1Taz = (Taz)alts.next();
//                        if (stop1Taz.isAvailable()) {
//                            stop1Taz.printCSV(file2);
//                        }
//                    }
//                    file2.close();
//
//                }
                //in the interest of not stopping the run, we will just assign the stop location to be the
                //tour begin location.  A log report at the end of PT will notify the user of the erroneous data.
                thisTour.intermediateStop1.location.zoneNumber=thisTour.begin.location.zoneNumber;
            }
            thisTour.intermediateStop1.distanceToActivity = skims.getDistance(thisTour.begin.endTime,
                                                                                 thisTour.begin.location.zoneNumber,
                                                                                 thisTour.intermediateStop1.location.zoneNumber);

            thisTour.primaryDestination.distanceToActivity = skims.getDistance(thisTour.intermediateStop1.endTime,
               		                                                              thisTour.intermediateStop1.location.zoneNumber,
																				  thisTour.primaryDestination.location.zoneNumber);
         }//end of stop1location model

         //Now do the stop2location destination choice model
         if(thisTour.intermediateStop2!=null) { // && thisTour.primaryMode.type != TourModeType.DRIVETRANSIT){

            for (Object o : iStop2Model.getAlternatives()) {

                 Taz stop2Taz = (Taz) o;

                 float autoTime = 0;
                 float walkTime = 0;
                 float bikeTime = 0;
                 float transitGeneralizedCost = 0;

                 float[] autoDists = skims.getAdditionalAutoDistance(thisTour.primaryDestination.location.zoneNumber,
                         thisTour.end.location.zoneNumber,
                         stop2Taz.zoneNumber,thisPerson.homeTaz,
                         thisTour.primaryDestination.endTime);

                 // check to make sure within distance threshold of anchor location.
                 if ((autoDists[1] - autoDists[0]) > distanceThreshold &&
                        stop2Taz.zoneNumber != thisTour.end.location.zoneNumber &&
                        stop2Taz.zoneNumber != thisTour.primaryDestination.location.zoneNumber) {
                     stop2Taz.setAvailability(false);

                     if (trace) {
                         logger.info("Stop 2 Taz not available: " + stop2Taz.zoneNumber +
                         " because prim. dest. to stop to end distance (" + autoDists[1] + ") > distance threshold (" + distanceThreshold + ")");
                     }

                     continue;
                 }
                 
                 // get the distance from home
                 float distFromHome = skims.getDistance(thisTour.begin.endTime, thisPerson.homeTaz, stop2Taz.zoneNumber);

                 
                 //calculate walk time for walk mode or for transit mode (you might be able to walk to the stop)
                 if (thisTour.primaryMode.type == TourModeType.WALK || thisTour.primaryMode.type == TourModeType.WALKTRANSIT ||
                         thisTour.primaryMode.type == TourModeType.PASSENGERTRANSIT)
                     walkTime = skims.getAdditionalWalkTime(thisTour.primaryDestination.location.zoneNumber,
                             thisTour.end.location.zoneNumber,
                             stop2Taz.zoneNumber,
                             thisTour.primaryDestination.endTime
                     );
                 else if (thisTour.primaryMode.type == TourModeType.BIKE)
                     bikeTime = skims.getAdditionalBikeTime(thisTour.primaryDestination.location.zoneNumber,
                             thisTour.end.location.zoneNumber,
                             stop2Taz.zoneNumber,
                             thisTour.primaryDestination.endTime
                     );
                 else if (thisTour.primaryMode.type == TourModeType.WALKTRANSIT ||
                         thisTour.primaryMode.type == TourModeType.PASSENGERTRANSIT)

                     transitGeneralizedCost =
                             skims.getAdditionalGeneralizedTransitCost(thisTour.primaryDestination.location.zoneNumber,
                                     thisTour.end.location.zoneNumber,
                                     stop2Taz.zoneNumber,
                                     thisTour.primaryDestination.endTime
                             );
                 else {
                     autoTime = skims.getAdditionalAutoTime(thisTour.primaryDestination.location.zoneNumber,
                             thisTour.end.location.zoneNumber,
                             stop2Taz.zoneNumber,
                             thisTour.primaryDestination.endTime
                     );
                 
                     // include terminal time, (to + from, so 2), weighted at 3 * IVT
                     autoTime += 2 * walkFactor * stop2Taz.terminalTime;
                 }
                 //destination choice model for this taz
                 ActivityPurpose purposeForSizeTerm = getPurposeForSecondStopSizeTerm(thisTour);
                 stop2Taz.setTrace(trace);
                 stop2Taz.calcStopDestinationUtility(
                         purposeForSizeTerm, 
                         iStop2PurposeParams,
                         thisTour.primaryMode,
                         thisTour.begin.location.zoneNumber,
                         thisTour.primaryDestination.location.zoneNumber,
                         autoTime,
                         walkTime,
                         bikeTime,
                         transitGeneralizedCost,
                         autoDists,
                         2, 
                         distFromHome
                 );


                 if (trace) {
                     logger.info("Stop 2 " + stop2Taz.zoneNumber + " utility"
                             + stop2Taz.getUtility());
                 }

             } //utilties have been calculated for each taz

             if (trace) {
                iStop2Model.writeAvailabilities();
            }
            iStop2Model.computeAvailabilities();

            if (trace) {
                iStop2Model.writeUtilityHeader();
            }
            iStop2Model.getUtility();

            if (trace) {
                iStop2Model.writeProbabilityHeader();
            }
            iStop2Model.calculateProbabilities();

            try {

                Taz chosenTaz = (Taz) iStop2Model.chooseAlternative(random.nextDouble());
                thisTour.intermediateStop2.location.zoneNumber = chosenTaz.zoneNumber;
                
                if (trace) {
                    logger.info("Intermediate Stop 2 chosen taz:" + chosenTaz.zoneNumber);
                }

            } catch (Exception e) {
                logger.error("Error in stop destination choice: no "
                        + "zones available for Household " + thisHousehold.ID + " Person "
                        + thisPerson.memberID + " Tour " + thisTour.tourNumber);

//                // write the tour information into a debug file. Path is
//                // specified in the pt.properties file
//                logger.error("Writing Tour Debug info to the debug directory");
//                PrintWriter file = PTResults.createTourDebugFile("HH"
//                        + thisHousehold.ID + "Tour" + thisTour.tourNumber
//                        + "Stop2.csv");
//                thisTour.printCSV(file);
//                file.close();
//
//                // if not already done, write the taz info into a debug file.
//                // Path is specified in pt.properties
//                PrintWriter file2 = PTResults.createTazDebugFile("HH"
//                        + thisHousehold.ID + "Tour" + thisTour.tourNumber
//                        + "Stop2AvailableAlternatives.csv");
//                if (file2 != null) { // if it is null that means an earlier
//                                        // problem caused this file to be
//                                        // written already and there
//                    // is no reason to write it out twice
//                    logger
//                            .error("Writing out Taz Info because the second stop on this tour couldn't find a destination");
//                    Iterator alts = iStop2Model.getAlternatives().iterator();
//                    while (alts.hasNext()) {
//                        Taz stop2Taz = (Taz) alts.next();
//                        if (stop2Taz.isAvailable()) {
//                            stop2Taz.printCSV(file2);
//                        }
//                    }
//                    file2.close();
//                }
//                // in the interest of not stopping the run, we will just assign
//                // the stop location to be the
//                // tour begin location. A log report at the end of PT will
//                // notify the user of the erroneous data.
//
//                logger.error("Could not find a stop 1 location for this tour: "
//                        + thisTour);
//                logger.error("Purpose: " + thisTour.getPurpose());
                // logger.error("Begin, end: " +
                // thisTour.begin.location.zoneNumber + ", " +
                // thisTour.end.location.zoneNumber);

                thisTour.intermediateStop2.location.zoneNumber = thisTour.begin.location.zoneNumber;

            }
            thisTour.intermediateStop2.distanceToActivity = skims.getDistance(
                    thisTour.primaryDestination.endTime,
                    thisTour.primaryDestination.location.zoneNumber,
                    thisTour.intermediateStop2.location.zoneNumber);

            thisTour.end.distanceToActivity = skims.getDistance(
                    thisTour.intermediateStop2.endTime,
                    thisTour.intermediateStop2.location.zoneNumber,
                    thisTour.end.location.zoneNumber);
        } // end of stop2location model

         endTiming();
    } // end of calculateStopZones method.

    /** 
     * 
     * @param thisTour Tour of interest
     * @return the purpose to use when calculating size terms
     *         (by default, the main tour purpose)
     */
    protected ActivityPurpose getPurposeForFirstStopSizeTerm(Tour thisTour) {
        return thisTour.primaryDestination.activityPurpose; 
    }

    /** 
     * 
     * @param thisTour Tour of interest
     * @return the purpose to use when calculating size terms
     *         (by default, the main tour purpose)
     */
    protected ActivityPurpose getPurposeForSecondStopSizeTerm(Tour thisTour) {
        return thisTour.primaryDestination.activityPurpose; 
    }
     
    /**
     * Ensure that the model can get built.
     * @param args Argument array
     */
    public static void main(String[] args) {
        ResourceBundle appRb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        logger.info("Initializing TazManager.");
        String tazManagerClass = ResourceUtil.getProperty(appRb,"sdt.taz.manager.class");
        Class tazClass = null;
        TazManager tazManager;
        try {
            tazClass = Class.forName(tazManagerClass);
            tazManager = (TazManager) tazClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
            throw new RuntimeException(e);
        }
        tazManager.readData(globalRb, appRb);

        StopDestinationChoiceModel model = new StopDestinationChoiceModel(appRb);
        model.buildModel(tazManager);

        logger.info("All done!");
    }


    public float getDistanceThreshold() {
        return distanceThreshold;
    }


    public void setDistanceThreshold(float distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }
}