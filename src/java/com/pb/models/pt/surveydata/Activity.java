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
package com.pb.models.pt.surveydata;

import org.apache.log4j.Logger;

import java.util.Calendar;

/**
 * @author Andrew Stryker <stryker@pbworld.com>
 *
 */
public class Activity extends Event {
    protected static Logger logger = Logger
            .getLogger("com.pb.models.surveydata");

    private int activity;

    private String purpose;

    private int day;

    private int mode;

    private long household;

    private int member;

    private int tour;

    /**
     * @return Returns the day.
     */
    public int getDay() {
        return day;
    }

    /**
     * @param day
     *            The day to set.
     */
    public void setDay(int day) {
        this.day = day;
    }

    /**
     * @return Returns the household number.
     */
    public long getHousehold() {
        return household;
    }

    /**
     * @param household
     *            The household to set.
     */
    public void setHousehold(int household) {
        this.household = household;
    }

    /**
     * @return Returns the member.
     */
    public int getMember() {
        return member;
    }

    /**
     * @param member
     *            The member to set.
     */
    public void setMember(int member) {
        this.member = member;
    }

    /**
     * @return Returns the tour number.
     */
    public int getTour() {
        return tour;
    }

    /**
     * @param tour
     *            The tour to set.
     */
    public void setTour(int tour) {
        this.tour = tour;
    }

    /**
     * @return Returns the mode.
     */
    public int getMode() {
        return mode;
    }

    /**
     * Initialize the activity with identifying attributes. (multi-day survey)
     */
    public Activity(long household, int member, int activity, int day) {
        this.household = household;
        this.member = member;
        this.activity = activity;
        this.day = day;
    }

    /**
     * Initialize the activity with identifying attributes.
     */
    public Activity(long household, int member, int activity) {
        this.household = household;
        this.member = member;
        this.activity = activity;
    }

    /**
     * Initialize the activity with identifying attributes.
     */
    public Activity(long household, int member) {
        this.household = household;
        this.member = member;
    }

    /**
     * @return Returns the activity.
     */
    public int getActivity() {
        return activity;
    }

    /**
     * @return Returns the purpose.
     */
    public String getPurpose() {
        return purpose;
    }

    /**
     * @param purpose
     *            The purpose to set.
     */
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    /**
     * @param mode
     *            The mode to set.
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Set the coordinates to that of another location.
     */
    public void setLocation(Location other) {
        setCoordinates(other.getXcord(), other.getYcord());
    }

    /**
     * Compare two Activities for the same times, purpose, and location.
     */
    public boolean equals(com.pb.models.pt.surveydata.Activity other) {
        if (other == null) {
            return false;
        }

        if (!getPurpose().equals(other.getPurpose())) {
            return false;
        }

        return true && super.equals(other);
    }

    /**
     * Summarize Activity attributes in a String.
     */
    public String toString() {
//        String res = "Activity " + activity;
//
//        DateFormat formatDate = new SimpleDateFormat("yyyy-MMM-dd'T'HH:mm");
//
//        if (purpose != null) {
//            res += " is " + purpose;
//        }
//        if (type != 0) {
//            res += " type " + type;
//        }
//        if (arrivalCalendar != null) {
//            res += " starting on " + formatDate.format(arrivalCalendar.getTime());
//        }
//        if (departureCalendar != null) {
//            res += " ending on " + formatDate.format(departureCalendar.getTime());
//        }
        return getPurpose();
    }

    /**
     * Test case.
     *
     * @param args
     */
    public static void main(String[] args) {
        com.pb.models.pt.surveydata.Activity activity = new com.pb.models.pt.surveydata.Activity(7, 3, 2, 1);
        com.pb.models.pt.surveydata.Activity.logger.info("Created: " + activity);

        activity.setArrival(2005, Calendar.JUNE, 30, 16, 3);
        activity.setDeparture(2005, Calendar.JULY, 1, 17, 0);
        activity.setCoordinates(-1 * (1. + 50. / 60. + 40. / 3600.), 53. + 9.
                / 60. + 2. / 3600.);
        activity.setPurpose("W");
        int duration = activity.duration();
        com.pb.models.pt.surveydata.Activity.logger.info("activity: " + activity);
        com.pb.models.pt.surveydata.Activity.logger.info("Ellapsed time should be 1497: " + duration);

        com.pb.models.pt.surveydata.Activity activity2 = new com.pb.models.pt.surveydata.Activity(7, 3, 3, 3);
        activity2.setArrival(2005, Calendar.JUNE, 30, 16, 6);
        activity2.setDeparture(2005, Calendar.JULY, 1, 17, 0);
        activity2.setCoordinates(-1 * (0. + 8. / 60. + 33. / 3600.), 52. + 12.
                / 60. + 19. / 3600.);
        activity2.setPurpose("W");
        com.pb.models.pt.surveydata.Activity.logger.info("Distance between the two activities should be 96.7"
                + " miles: " + activity.distanceMiles(activity2));
        com.pb.models.pt.surveydata.Activity.logger.info("The activities should be equal: "
                + activity.equals(activity2));
        activity2.getArrivalCalendar().add(Calendar.MINUTE, 60);
        com.pb.models.pt.surveydata.Activity.logger.info("The activities should not be equal: "
                + activity.equals(activity2));
    }
}
