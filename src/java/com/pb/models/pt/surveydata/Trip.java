/*
 * Copyright 2005 PB Consult Inc.
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
 * Created on Aug 3, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.surveydata;

import org.apache.log4j.Logger;

import java.util.Calendar;

/**
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class Trip extends Event {
    protected static Logger logger = Logger
            .getLogger("com.pb.models.surveydata");

    private String purpose;

    private int trip;

    private int mode;

    private long household;

    private int member;

    private int tour;

    private Activity origin = null;

    private Activity destination = null;

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
     * @return Returns the destination.
     */
    public Activity getDestination() {
        return destination;
    }

    /**
     * @param destination
     *            The destination to set.
     */
    public void setDestination(Activity destination) {
        this.destination = destination;
        // arrival = destination.getArrival();
        mode = destination.getMode();
        type = destination.getType();
        purpose = destination.getPurpose();
    }

    /**
     * @return Returns the origin.
     */
    public Activity getOrigin() {
        return origin;
    }

    /**
     * @param origin
     *            The origin to set.
     */
    public void setOrigin(Activity origin) {
        this.origin = origin;
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
     * Initialize the trip with identifying attributes.
     */
    public Trip(long household, int member, int trip) {
        this.household = household;
        this.member = member;
        this.trip = trip;
    }

    /**
     * @return Returns the trip.
     */
    public long getTrip() {
        return trip;
    }

    /**
     * @param mode
     *            The mode to set.
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Summarize Trip attributes in a String.
     */
    public String toString() {
        String res = "Trip " + trip;
        if (mode != 0) {
            res += " is " + mode;
        }

        return res;
    }

    /**
     * Test case.
     * 
     * @param args
     */
    public static void main(String[] args) {
        Trip trip = new Trip(7, 3, 2);
        logger.info("Created: " + trip);

        trip.setArrival(2005, Calendar.JUNE, 30, 16, 3);
        trip.setDeparture(2005, Calendar.JULY, 1, 17, 0);

        int duration = trip.duration();

        logger.info("Ellapsed time should be 1497: " + duration);

    }
}