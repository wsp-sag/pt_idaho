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

import com.pb.common.model.ModelException;
import static com.pb.models.pt.ActivityPurpose.WORK;
import static com.pb.models.pt.ActivityPurpose.WORK_BASED;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.Serializable;

/**
 * A class containing tour attributes
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class Tour implements Serializable {

    private final static long serialVersionUID = 0;

    // Attributes
    final transient Logger logger = Logger.getLogger(Tour.class);

    public Activity begin;

    public Activity primaryDestination;

    public Activity intermediateStop1;

    public Activity intermediateStop2;

    public Activity end;

    public String tourString;
    
    private String completedTourString;

    public Mode primaryMode;

    public float departDist;

    public float returnDist;

    // public Mode primaryMode;
    boolean hasPrimaryMode;

    public int tourNumber; // 1->total tours

    // for work-based tours, based on tour mode to work
    public boolean driveToWork;

    public int tourDuration;

    private int priority = -1;

    private int order = -1;
    
    //for work-based tours, the number of the parent tour
    public int parentTourNumber;

    // constructor to create empty Tour, to be used with workBasedTour
    public Tour() {

        // initialize the origin and primary destination activities
        begin = new Activity();
        // begin.isBegin=1;
        end = new Activity();
        // end.isEnd=1;
        primaryDestination = new Activity();
        // primaryDestination.isPrimaryDestination=1;

    }

    /**
     * Calculate the duration for the tour based on the end time of the first
     * activity and the start time of the last activity. Store as part of the
     * tour in the tourDuration field, also return it. This method will compute
     * duration based on a continuous representation of time.
     * 
     * @return The tour duration in minutes.
     */
    public int calculateDurationContinuous() {
        int startHour = begin.endTime / 100;
        int startMinute = begin.endTime - startHour * 100;

        int endHour = end.startTime / 100;
        int endMinute = end.startTime - endHour * 100;

        tourDuration = (short) ((endHour - startHour) * 60);
        tourDuration += (short) (endMinute - startMinute);
        return tourDuration;

    }

    /**
     * Calculate the duration for the tour based on the end time of the first
     * activity and the start time of the last activity. Store as part of the
     * tour in the tourDuration field, also return it. This method will compute
     * duration based on a 1-hour representation of time. For example, the
     * duration for a tour beginning and ending at 900 will be 60 minutes (as
     * the maximum potential duration for the tour).
     * 
     * @return The tour duration in minutes.
     */
    public int calculateDurationHourly() {
        int startHour = begin.endTime / 100;
        int startMinute = begin.endTime - startHour * 100;

        int endHour = end.startTime / 100;
        int endMinute = end.startTime - endHour * 100;

        tourDuration = (short) ((endHour - startHour + 1) * 60);
        tourDuration += (short) (endMinute - startMinute);
        return tourDuration;

    }

    // contructor takes a pattern and an integer identifying the tourNumber and
    // fills in attributes accordingly
    public Tour(String thisTourString, Activity begin) {

        tourString = thisTourString;

        if (thisTourString.length() < 3) {
            String message = "Less than 3 activities on tour: "
                    + thisTourString;
            logger.fatal(message);
            throw new ModelException(message);
        }

        // initialize the origin and primary destination activities
        if (begin == null) {
            this.begin = new Activity();
            this.begin.activityType = ActivityType.BEGIN;
            this.begin.activityNumber = 1;
            this.begin.activityPurpose = ActivityPurpose
                    .getActivityPurpose(thisTourString.charAt(0));
        } else {
            this.begin = begin;
        }

        end = new Activity();
        end.activityType = ActivityType.END;
        end.activityNumber = thisTourString.length();
        end.activityPurpose = ActivityPurpose.getActivityPurpose(thisTourString
                .charAt(thisTourString.length() - 1));

        primaryDestination = new Activity();
        primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
        // The activity number and activity purpose will be determined below.

        // code activity numbers
        if (thisTourString.length() == 5) { // there are two intermediate stops

            intermediateStop1 = new Activity();
            intermediateStop1.activityType = ActivityType.INTERMEDIATE_STOP;
            intermediateStop1.activityNumber = 2;
            intermediateStop1.activityPurpose = ActivityPurpose
                    .getActivityPurpose(thisTourString.charAt(1));

            primaryDestination.activityNumber = 3;
            primaryDestination.activityPurpose = ActivityPurpose
                    .getActivityPurpose(thisTourString.charAt(2));

            intermediateStop2 = new Activity();
            intermediateStop2.activityType = ActivityType.INTERMEDIATE_STOP;
            intermediateStop2.activityNumber = 4;
            intermediateStop2.activityPurpose = ActivityPurpose
                    .getActivityPurpose(thisTourString.charAt(3));
        }
        if (thisTourString.length() == 4) { // there is one intermediate stop;
            // currently the default is that the stop will come after the
            // primary
            // destination if the 2 stops are the same purpose.

            if (hasIntermediateStop1(thisTourString) == 1) { // stop comes
                // before
                // primary
                // destination
                intermediateStop1 = new Activity();
                intermediateStop1.activityType = ActivityType.INTERMEDIATE_STOP;
                intermediateStop1.activityNumber = 2;
                intermediateStop1.activityPurpose = ActivityPurpose
                        .getActivityPurpose(thisTourString.charAt(1));

                primaryDestination.activityNumber = 3;
                primaryDestination.activityPurpose = ActivityPurpose
                        .getActivityPurpose(thisTourString.charAt(2));

            } else { // stop comes after the primary destination
                primaryDestination.activityNumber = 2;
                primaryDestination.activityPurpose = ActivityPurpose
                        .getActivityPurpose(thisTourString.charAt(1));

                intermediateStop2 = new Activity();
                intermediateStop2.activityType = ActivityType.INTERMEDIATE_STOP;
                intermediateStop2.activityNumber = 3;
                intermediateStop2.activityPurpose = ActivityPurpose
                        .getActivityPurpose(thisTourString.charAt(2));

            }
        } // end if one stop on tour
        if (thisTourString.length() == 3) {
            primaryDestination.activityNumber = 2;
            primaryDestination.activityPurpose = ActivityPurpose
                    .getActivityPurpose(thisTourString.charAt(1));
        }
    }// end Tour creation.

    // This method prints the Tour attributes to the logger.
    // Need to change this method to use times within activity rather than
    // obsolete Tour variables
    public void print() {
        logger.info("");
        logger.info("TOUR INFO: ");
        logger.info("priority:  " + getPriority());
        logger.info("order:  " + getOrder());
        logger.info("");
        logger.info("Begin Activity");
        logger.info("****************************");
        begin.print();
        logger.info("Time To Begin Activity: " + begin.startTime);
        if (intermediateStop1 != null) {
            logger.info("");
            logger.info("Intermediate Stop 1 Activity");
            logger.info("****************************");
            intermediateStop1.print();
            logger.info("Time To Intermediate Stop 1 Activity: "
                    + intermediateStop1.timeToActivity);
        }
        logger.info("");
        logger.info("Primary Destination Activity");
        logger.info("****************************");
        primaryDestination.print();
        logger.info("Time To Primary Destination Activity: "
                + primaryDestination.timeToActivity);
        logger.info("Trip mode: " + primaryDestination.tripMode);
        if (intermediateStop2 != null) {
            logger.info("");
            logger.info("Intermediate Stop 2 Activity");
            logger.info("****************************");
            intermediateStop2.print();
            logger.info("Time To Intermediate Stop 2 Activity: "
                    + intermediateStop2.timeToActivity);
        }
        logger.info("");
        logger.info("End Activity");
        logger.info("****************************");
        end.print();
        logger.info("Time To End Activity: " + end.timeToActivity);
        if (hasPrimaryMode)
            primaryMode.print();

        // Travel times (unweighted minutes)
        // public int timeToPrimaryDestination;
        // public int timeToBegin;
        // public int timeToEnd;
        // public int timeToIntermediateStop1;
        // public int timeToIntermediateStop2;

    }

    /**
     * Count number of tour activities.
     * 
     * @return activity count
     */
    public int getActivityCount() {
        return tourString.length();
    }

    /**
     * Has an outbound stop
     * 
     * @return True if there is an outbound intermediate stop.
     */
    public boolean hasOutboundStop() {
        return intermediateStop1 != null;
    }

    /**
     * Has an inbound stop
     * 
     * @return True if there is an inbound intermediate stop.
     */
    public boolean hasInboundStop() {
        return intermediateStop2 != null;
    }

    // This method returns 1 if an intermediateStop occurs before the primary
    // destination
    public int hasIntermediateStop1(String tourString) {
        int intermediateStop1 = 0;
        if ((tourString.length() == 5 || tourString.length() == 4)
                && (ActivityPurpose.getActivityPurpose(tourString.charAt(1))
                        .ordinal() > ActivityPurpose.getActivityPurpose(
                        tourString.charAt(2)).ordinal()))
            intermediateStop1 = 1;
        
        // special case for HCWH
        if (tourString.length()==4) {
            if (ActivityPurpose.getActivityPurpose(tourString.charAt(1))
                    .equals(ActivityPurpose.COLLEGE)
                 && ActivityPurpose.getActivityPurpose(tourString.charAt(2))
                 .equals(ActivityPurpose.WORK)) {
                intermediateStop1 = 0; 
            }
        }

        // special case for HWCH
        if (tourString.length()==4) {
            if (ActivityPurpose.getActivityPurpose(tourString.charAt(1))
                    .equals(ActivityPurpose.WORK)
                 && ActivityPurpose.getActivityPurpose(tourString.charAt(2))
                 .equals(ActivityPurpose.COLLEGE)) {
                intermediateStop1 = 1; 
            }
        }
        
        return intermediateStop1;
    }
    
    public int iStopsCheck(int stops, String tourString) {
        int iStopsCheckReturn = 0;
        if (stops == tourString.length() - 3)
            iStopsCheckReturn = 1;
        return iStopsCheckReturn;
    }

    // this method sets up the workBased tour based on the attributes of the
    // work tour
    public void setWorkBasedTourAttributes(Tour workTour) {

        begin.activityType = ActivityType.BEGIN;
        begin.activityPurpose = ActivityPurpose.WORK;
        begin.activityNumber = 1;
        begin.location = workTour.primaryDestination.location;
        begin.startTime = workTour.primaryDestination.startTime;
        //may not have activity duration, so use tour duration as a proxy
        begin.duration = (short) workTour.tourDuration;
//        begin.duration = workTour.primaryDestination.duration;

        primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
        primaryDestination.activityPurpose = ActivityPurpose.OTHER;
        primaryDestination.activityNumber = 2;

        end.activityType = ActivityType.END;
        end.activityPurpose = ActivityPurpose.WORK;
        end.activityNumber = 3;
        end.location = workTour.primaryDestination.location;
        end.endTime = workTour.primaryDestination.endTime;
        tourString = "wow";

        if (workTour.primaryMode.type == TourModeType.AUTODRIVER)
            driveToWork = true;
        else
            driveToWork = false;

        tourDuration = workTour.primaryDestination.duration;
    }

    void printCSV(PrintWriter file) {

        /*
         * "hhID,memberID,personAge,weekdayTour(yes/no),initialTourString,completedTourString,tour#,departDist," +
         * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
         * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
         * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
         * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
         * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
         * "primaryMode
         */
        
        file.print(tourString + ",");
        file.print(getCompletedTourString() + ",");
        file.print(tourNumber + ",");
        file.print(departDist + ",");
        begin.printCSV(file);

        if (intermediateStop1 != null)
            intermediateStop1.printCSV(file);
        else
            file.print("0,0,0,0,0,0,0,");
        primaryDestination.printCSV(file);
        if (intermediateStop2 != null)
            intermediateStop2.printCSV(file);
        else
            file.print("0,0,0,0,0,0,0,");
        end.printCSV(file);

        if (primaryMode != null) {
            file.print(primaryMode.type);
        } else {
            file.print("no mode");
        }

        file.flush();
        // the calling method will close the file.

    }

    /**
     * Set the tour priority.
     * 
     * The tour priority is the scheduling order of the tour. The number is
     * 0-based. A negative number means that the tour has not been prioritized.
     * @param priority Tour priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Get the tour priority.
     * 
     * The tour priority is the scheduling order of the tour. The number is
     * 0-based. A negative number means that the tour has not been prioritized.
     * @return int priority of tour
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Get the chronological order within the day-pattern of the tour.
     * 
     * @return order, 0-based, -1 means unset
     */
    public int getOrder() {
        return order;
    }

    /**
     * Set the chronological order of the tour.
     * 
     * @param order
     *            0-based
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Get the tour purpose.
     * 
     * @return ActivityPurpose of the primary destination.
     */
    public ActivityPurpose getPurpose() {

        if (tourString != null && tourString.charAt(0) == 'w') {
            return ActivityPurpose.WORK_BASED;
        }

        return primaryDestination.activityPurpose;
    }
    
    /**
     * 
     * @return String completed tour string
     */
    public String getCompletedTourString() {
        String string = ActivityPurpose
                .getActivityString(begin.activityPurpose);

        if (intermediateStop1 != null
		&& intermediateStop1.activityPurpose != null) {
            string += ActivityPurpose
                    .getActivityString(intermediateStop1.activityPurpose);
        }

        string += ActivityPurpose
                .getActivityString(primaryDestination.activityPurpose);

        if (intermediateStop2 != null
		&& intermediateStop2.activityPurpose != null) {
            string += ActivityPurpose
                    .getActivityString(intermediateStop2.activityPurpose);
        }

        string += ActivityPurpose.getActivityString(end.activityPurpose);

        return string;
    }

    /**
     * Find out if tour is the first work or work-based tour in the pattern.
     * @param person PTPerson
     * @return boolean Is the the person's first work-based tour
     */
    public boolean isFirstWork(PTPerson person) {

        Pattern tourPattern = person.weekdayPattern;
        ActivityPurpose purpose = getPurpose();
        int nWorkTours = tourPattern.nWorkTours;

        if (purpose != WORK && purpose != WORK_BASED) {
            return false;
        }

        if (nWorkTours == 1) {
            return true;

        }

        for (Tour t : person.weekdayTours) {
            if (t == this) {
                return true;
            }
            if (t.getPurpose() == WORK || t.getPurpose() == WORK_BASED) {
                return false;
            }
        }

        // it should be impossible to reach this point in the code
        throw new RuntimeException("Logic error when calling isFirstWork().");
    }
    
    /**
     * Get the origin activity for the trip number.  Must not be greater than the number
     * of trips on this tour or method will throw an error.
     * 
     * @param tripNumber
     * @return The origin activity
     */
    public Activity getOriginActivity(int tripNumber){
        int trips = getNumberOfTrips();
        if(tripNumber>trips || tripNumber==0){
            logger.fatal("Error: Attempting to get origin activity for trip number "+tripNumber+ " on tour with "+trips+" trips");
            throw new RuntimeException();
        }
        if(tripNumber==1)
            return begin;
        else if(tripNumber==2)
            if(hasOutboundStop())
                return intermediateStop1;
            else
                return primaryDestination;
        else if(tripNumber==3)
            if(hasOutboundStop())
                return primaryDestination;
            else
                return intermediateStop2;
        else 
            return intermediateStop2;
                    
    }
    
    /**
     * Get the destination activity for the trip number.  Must not be greater than the number
     * of trips on this tour or method will throw an error.
     * 
     * @param tripNumber
     * @return  The destination activity
     */
    public Activity getDestinationActivity(int tripNumber){
        int trips = getNumberOfTrips();
        if(tripNumber>trips || tripNumber==0){
            logger.fatal("Error: Attempting to get destination activity for trip number "+tripNumber+ " on tour with "+trips+" trips");
            throw new RuntimeException();
        }
        if(tripNumber==1)
            if(hasOutboundStop())
                return intermediateStop1;
            else
                return primaryDestination;
        else if(tripNumber==2)
            if(hasOutboundStop())
                return primaryDestination;
            else if(hasInboundStop())
                return intermediateStop2;
            else
                return end;
         else if(tripNumber==3)
             if(hasOutboundStop())
                 if(hasInboundStop())
                    return intermediateStop2;
                 else
                     return end;
            else
                return end;
        else 
            return end;
                    
    }
    /**
     * Return the number of trips on this tour.
     * 
     * @return The number of trips on the tour; default is 2.
     */
    public int getNumberOfTrips(){
        int trips=2;
        if( hasOutboundStop() )
            ++trips;
        if(hasInboundStop())
            ++trips;
        return trips;
    }
} /* end class Tour */
