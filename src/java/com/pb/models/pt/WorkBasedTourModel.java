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
import com.pb.common.matrix.Matrix;
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.utils.Tracer;



 import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.ResourceBundle;


 /**
 * This class implements a logit model to choose a work based tour mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

 
public class WorkBasedTourModel{
    final static Logger logger = Logger.getLogger(WorkBasedTourModel.class);
    boolean writtenOutTheUtilitiesAlready = false;
     final static int debugID = -1;
     TourModePersonAttributes personAttributes;
     Tracer tracer;
     boolean trace=false;
     
     TableDataSet stopDurationTable;
     
     final static String PRIM_COL="PRIM_ACCUM";
     final static String FIRST_COL="FIRST_ACCUM";
     /**
      * Default constructor.
      *
      */
     public WorkBasedTourModel(ResourceBundle ptRb){
     
         personAttributes = new TourModePersonAttributes();
         tracer = Tracer.getTracer();
         readPctWorkBasedDuration(ptRb);
     }
     /**
      * Schedule the activities, choose a primary destination, a tour mode, 
      * and trip modes.
      * 
      * @param household household
      * @param person person
      * @param tour tour
      * @param skims SkimsInMemory
      * @param mcLogsums mode choice logsums
      * @param tazs tazManager
      * @param tdcm dest choice model
      * @param tourMCM tour mode choice model
      * @param tripMCM tripmode choice model
      * @param random Random number generator
      */
     public void calculateWorkBasedTour(PTHousehold household, 
     		                            PTPerson person, 
										Tour tour, SkimsInMemory skims,
                                        Matrix[] workMCLogsums,
                                        TazManager tazs,
                                        TourDestinationChoiceModel tdcm,
                                        TourModeChoiceModel tourMCM,
                                        TripModeChoiceModel tripMCM,
                                        Random random){
         
         double percentPrimaryDestinationTime=0;
         double firstDurationPercent=0;
          
    	 trace = tracer.isTracePerson(person.hhID + "_" + person.memberID);
          if(trace){
               logger.info("Work-Based Tour found for household "+household.ID);
           }


          //set mode choice person attributes
          
          personAttributes.setAttributes(household,person,tour);  //I don't think this is used/necessary
          /*
          double alpha =     0.98503;                  
          double beta =     5.45779;                    
          double r = random.nextDouble();
          
          r = Math.min(r,(alpha-0.00001));
          
          double percentPrimaryDestinationTime = - Math.log(-(r/alpha-1))/beta;
          */
 
                             
          // Draw the primary Stop ratio from Distribution(PctWorkBasedDuration.csv) - coheno, Feb 26,07
          try {
              percentPrimaryDestinationTime = drawFromAccumulativeDistribution(PRIM_COL);
          } catch (Exception e) {
              e.printStackTrace();
              System.exit(1);
          }
          Tour workTour = person.weekdayTours[tour.parentTourNumber];
          tour.begin.startTime = workTour.primaryDestination.startTime;
          tour.end.endTime = workTour.primaryDestination.endTime;
          
          // Calculate the duration for the tour based on the start time of the first
          // activity and the end time of the last activity.

          int startHour = tour.begin.startTime / 100;
          int startMinute = tour.begin.startTime - startHour * 100;

          int endHour = tour.end.endTime / 100;
          int endMinute = tour.end.endTime - endHour * 100;

          int totalMinutesAvailable = (short) ((endHour - startHour + 1) * 60);
          totalMinutesAvailable += (short) (endMinute - startMinute);
 
          tour.primaryDestination.duration=new Long(Math.round(percentPrimaryDestinationTime*totalMinutesAvailable)).shortValue();
          //logger.info("percentPrimaryDestinationTime="+percentPrimaryDestinationTime+", totalMinutesAvailable="+totalMinutesAvailable);
          int minutesAvailable = totalMinutesAvailable-tour.primaryDestination.duration;
          
          //calculate first work activity duration using logistic curve
          //logistic curve random=alpha/(1+beta*EXP(-1*gamma*percent))
          //solve for percent = - ln( (alpha/random-1)/beta )/gamma
          //note: random must be < alpha or undefined
          //Coefficient Data:
          /*
          alpha =    0.98877;
          beta =     103.49955;
          double gamma =   9.26164;
 
          r = random.nextDouble();
          r = Math.min(r,(alpha-0.00001));
          
          //double firstDurationPercent = - Math.log( (alpha/r-1)/beta )/gamma;
          */
          
          //Draw the First Stop Ratio from Distribution (PctWorkBasedDuration.csv) - coheno, Feb 26,07
          try{
              firstDurationPercent = drawFromAccumulativeDistribution(FIRST_COL);
             } catch (Exception e) {
                 e.printStackTrace();
                 System.exit(1);
             }

          tour.begin.duration=new Double(minutesAvailable*firstDurationPercent).shortValue();
          //logger.info("firstDurationPercent="+firstDurationPercent+", minutesAvailable="+minutesAvailable);
          tour.begin.calculateEndTime();
          tour.primaryDestination.calculateStartTime(tour.begin);
          tour.primaryDestination.calculateEndTime();
          tour.end.calculateStartTime(tour.primaryDestination);
          tour.end.duration= (short)(totalMinutesAvailable-(tour.primaryDestination.duration+tour.begin.duration));
           
          if(trace){
               logger.info("Percent primary destination time "+percentPrimaryDestinationTime);
               logger.info("Total tour time (based on first activity "+tour.begin.duration);
               logger.info("Primary destination duration "+tour.primaryDestination.duration);
               logger.info("Minutes available for tour begin and end activities"+minutesAvailable);
               logger.info("Tour begin activity duration "+tour.begin.duration);
               logger.info("Tour end activity duration "+tour.end.duration);
               logger.info("Minutes available for travel "+minutesAvailable);
          }


          
         if(trace){
                  logger.info("Here are the utilities for the tazs passed into the 'WorkBasedTour.calculateWorkBaseTour' method");
                  logger.info("for HHID " + household.ID + ", Person " + person.memberID + ", Tour " + tour.tourNumber
                          + ", ActivityPurpose b"
                          + ", Origin " + tour.begin.location.zoneNumber);
                  logger.info("ZoneNumber,Utility,Logsum");
          }

         //get skims for tour; note that time and distance needs to be
         //passed the OTHER purpose to return off-peak times, while the 
         //mode choice logsum matrix is WORK_BASED to represent mode logsum for tour
         int segment = IncomeSegmenter.calcLogsumSegment(household.income, household.autos, household.workers);
         Matrix time = skims.getTimeMatrix(ActivityPurpose.OTHER);
         Matrix dist = skims.getDistanceMatrix(ActivityPurpose.OTHER);
         Matrix logsum = workMCLogsums[segment];
         
         tdcm.constrainByTimeAvailable = true;
         tdcm.setDistanceThreshold(50);
         tdcm.calculateUtility(household, person, tour,
                 logsum, time, dist);
         
         Taz dest;
         try {
        	 dest = tdcm.chooseZone(random);
         } catch (ModelException e){
        	 dest = tazs.getTaz(tour.begin.location.zoneNumber);
         }
         
         Taz orig = tazs.getTaz(tour.begin.location.zoneNumber);
         tour.primaryDestination.location.zoneNumber = dest.zoneNumber;
         tdcm.constrainByTimeAvailable = false;

         tourMCM.setAttributes(household, person, tour, skims, orig, dest);
         if(!tour.driveToWork)
             tourMCM.setDriveAloneAvailability(false);
         else
             tourMCM.setDriveAloneAvailability(true);

         tourMCM.calculateUtility();
         tour.primaryMode = tourMCM.chooseMode(random);
          
          tour.hasPrimaryMode=true;
          if(logger.isDebugEnabled() && household.ID==debugID) {
              logger.debug("Logsum for household "+household.ID+"="+logsum);
          }
          
          tripMCM.calculateTripModes(household,
                  person, tour, skims, tazs, random);

     }
     
     public double drawFromAccumulativeDistribution(String colName) throws Exception {
         double rand= Math.random();
         int row=1;
         while (rand> stopDurationTable.getValueAt(row,colName)) 
             row++;
         if(row>stopDurationTable.getRowCount())
             throw new Exception("error in accumulative file");             
         return stopDurationTable.getValueAt(row,"RATIO");
          
     }
     
     public void readPctWorkBasedDuration(ResourceBundle ptRb) {
         String stopDurationFile= ResourceUtil.getProperty(ptRb,"sdt.pct.work.based.duration");
         logger.info("WorkBased stopDuration File="+stopDurationFile);
         try {
             CSVFileReader reader = new CSVFileReader();
              stopDurationTable = reader.readFile(new File(stopDurationFile));
         } catch (IOException e) {
             logger.fatal("Can't find workBased Duration file "
                     + stopDurationFile);
             throw new RuntimeException(e);
         }
         
         logger.info("Work based stopDurationFile was read");

     }


}

