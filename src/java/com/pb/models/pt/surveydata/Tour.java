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
 * Created on Jul 11, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.surveydata;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Represenation of tours.
 * 
 * Tours are a sequence of activites. The Tour class includes functionality that
 * handles the activity sequence and store information about the tour.
 * 
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class Tour extends Event {
    protected static Logger logger = Logger.getLogger("com.pb.models");

    private long household;

    private int member;

    private int tour;

    private String purpose = null;

    private int mode;

    private ArrayList<Activity> activities = null;

    private ArrayList<Trip> trips = null;

    private Activity destination;

    private Activity anchor;

    /**
     * Constructor.
     */
    private Tour() {
    }

    public Tour(long household, int member, int tour) {
        this.household = household;
        this.member = member;
        this.tour = tour;
    }

    public Tour(long household, int member) {
        this.household = household;
        this.member = member;
    }

    /**
     * @return Returns the household.
     */
    public long getHousehold() {
        return household;
    }

    /**
     * @param household
     *            The household to set.
     */
    public void setHousehold(long household) {
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
    public void setDestination(Activity anchor) {
        this.destination = anchor;
    }

    /**
     * @return Returns the primary tour mode.
     */
    public int getMode() {
        return mode;
    }

    /**
     * @param mode
     *            The primary tour mode.
     */
    public void setMode(int mode) {
        this.mode = mode;
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
     * @return Returns the anchor.
     */
    public Activity getAnchor() {
        return anchor;
    }

    /**
     * @param anchor
     *            The anchor to set.
     */
    public void setAnchor(Activity anchor) {
        this.anchor = anchor;
    }

    /**
     * @param activity
     *            Append the activity to the Activity list.
     */
    public void appendActivity(Activity activity) {
        if (activities == null) {
            activities = new ArrayList<Activity>();
        }
        activities.add(activity);
    }

    /**
     * Get an activity from the activity list. 0-based.
     * 
     * @param n
     * @return
     */
    public Activity getActivity(int n) {
        return activities.get(n);
    }

    public int getActivityCount() {
        return activities == null ? 0 : activities.size();
    }

    public ArrayList<Activity> getActivities() {
        return activities;
    }

    /**
     * @return The number of Activities.
     * @deprecated Use getActivityCount()
     */
    public int getNumberOfActivities() {
        return getActivityCount();
    }

    /**
     * Calculates the number of outbound stops. The destination activitiy must
     * be set and be one of the objects in the activities ArrayList.
     * 
     * Be careful when using this method. For tours like h - w - o - W - h where
     * the upper case W is the destination, there are two outbound stops.
     * 
     * @deprecated use getOutboundStopCount
     * @return The number of outbound stops of -1 if the destination is the last
     *         activity.
     */
    public int outboundStops() {
        if (activities == null || destination == null) {
            return 0;
        }

        Iterator<Activity> activityIter = getActivityIterator();
        Activity activity;
        int stops = -1; // compensate for the first activity
        while (activityIter.hasNext()) {
            activity = activityIter.next();

            if (activity == destination) {
                break;
            }

            stops += 1;
        }

        return stops;
    }

    /**
     * Calculates the number of inbound stops. The destination activitiy must be
     * set and be one of the objects in the activities ArrayList.
     * 
     * Be careful when using this method. For tours like h - W - o - w - h where
     * the upper case W is the destination, there are two inbound stops.
     * 
     * @deprecated use getInboundStopCount
     * @return The number of inbound stops or -1 if the destination is the last
     *         activity.
     */
    public int inboundStops() {
        if (activities == null || destination == null) {
            return 0;
        }

        Iterator<Activity> activityIter = getActivityIterator();
        Activity activity;
        int stops = -1;
        while (activityIter.hasNext()) {
            activity = activityIter.next();

            if (activity == destination) {
                stops = -1; // compensate for last activity
                continue;
            }

            stops += 1;
        }

        return stops;
    }

    /**
     * @return An iterator over the activities in the Tour.
     */
    public Iterator<Activity> getActivityIterator() {
        return activities.iterator();
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
     * @param activities
     *            The activities to set.
     */
    public void setActivities(ArrayList<Activity> activities) {
        this.activities = activities;
    }

    /**
     * Form trips from the list of activities.
     * 
     * A null value for the activity list results in a null value for the trip
     * list.
     */
    public void formTrips() {
        // household members without activities
        if (activities == null) {
            return;
        }

        trips = new ArrayList<Trip>();

        Iterator<Activity> activityIter = getActivityIterator();
        Activity activity = activityIter.next();
        int t = 0;
        Trip trip = new Trip(household, member, ++t);
        trip.setOrigin(activity);

        while (activityIter.hasNext()) {
            activity = activityIter.next();
            trip.setDestination(activity);
            trips.add(trip);

            if (activityIter.hasNext()) {
                trip = new Trip(household, member, ++t);
                trip.setOrigin(activity);
            }
        }
    }

    /**
     * @return Outbound intermediate stops.
     */
    public int getOutboundStopCount() {
        Iterator i = getActivityIterator();
        Activity a = null;
        int cntr = -1; // gets set to 0 for the first activity

        while (i.hasNext()) {
            a = (Activity) i.next();
            // break when finding the destination
            if (a.equals(destination)) {
                break;
            }
            cntr += 1;
        }

        // for tour fragments, ensure that the number of stop is at least 0
        if (cntr < 0) {
            cntr = 0;
        }

        return cntr;
    }

    /**
     * @return Inbound intermediate stops.
     */
    public int getInboundStopCount() {
        Iterator i = getActivityIterator();
        Activity a = null;
        int cntr = -1;

        while (i.hasNext()) {
            a = (Activity) i.next();
            cntr += 1;
            if (a.equals(destination)) {
                cntr = -1; // to compensate for the last activity
            }
        }

        // for tour fragments, ensure that the number of stop is at least 0
        if (cntr < 0) {
            cntr = 0;
        }

        return cntr;
    }

    /**
     * Compare tours for the same sequence of activities.
     * 
     * @param The
     *            comparison Tour.
     * @return true if the Tours are the same.
     */
    public boolean equals(Tour other) {
        if (other == null) {
            return false;
        }

        // save a little time and ensure the loop below will work
        if (getNumberOfActivities() != other.getNumberOfActivities()) {
            return false;
        }

        Iterator actIter1 = getActivityIterator();
        Iterator actIter2 = other.getActivityIterator();

        Activity activity1;
        Activity activity2;

        while (actIter1.hasNext()) {
            activity1 = (Activity) actIter1.next();
            activity2 = (Activity) actIter2.next();

            if (!activity1.equals(activity2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 
     * @return The activity pattern as a String.
     */
    public String getPattern() {
        String res = "";

        if (activities != null && activities.size() > 0) {
            for (Activity activity : activities) {
                res += activity.getPurpose();
            }
        }

        return res;
    }

    /**
     * Calculate the straight-line stop-to-stop tour distance.
     * 
     * Accumulate the straight-line distance between each successive activity in
     * the activity sequence.
     */
    public double getDistance() {
        double sum = 0;

        Iterator<Activity> activityIter = getActivityIterator();
        Activity previous = null;
        while (activityIter.hasNext()) {
            Activity activity = activityIter.next();

            if (previous != null) {
                double dist = activity.distance(previous);

                if (dist < 0) {
                    return -1;
                }

                sum += dist;
            }

            previous = activity;
        }

        return sum;
    }

    /**
     * Tour departure time.
     * 
     * The tour departure time is the departure time in minutes past midnight
     * for the first activity.
     */
    public int getDepartureMinute() {
        Activity first = activities.get(0);

        return first.getDepartureMinute();
    }

    /**
     * Tour arrival time.
     * 
     * The tour arrival time is the arrival time in minutes past midnight for
     * the last activity.
     */
    public int getArrivalMinute() {
        Activity last = activities.get(activities.size() - 1);

        return last.getArrivalMinute();
    }

    /**
     * Summarize tour as a sequence of activities.
     * 
     * @return
     */
    public String toString() {
        return getPurpose() + ": " + getPattern();
    }

    public static void main(String[] args) {
        Activity a = new Activity(7, 3, 1, 1);
        a.setPurpose("H");
        a.setDeparture(1975, 4, 24, 6, 30);
        Activity b = new Activity(7, 3, 2, 1);
        b.setPurpose("Sh");
        Activity c = new Activity(7, 3, 3, 1);
        c.setPurpose("W");
        Activity d = new Activity(7, 3, 4, 1);
        d.setPurpose("M");
        Activity e = new Activity(7, 3, 5, 1);
        e.setPurpose("E");
        Activity f = new Activity(7, 3, 6, 1);
        f.setPurpose("H");
        f.setArrival(1975, 4, 24, 8, 45);

        Tour t = new Tour(0, 0, 1);
        logger.info("The Tour has " + t.getNumberOfActivities()
                + " Activities.");

        t.appendActivity(a);
        t.appendActivity(b);
        t.appendActivity(c);
        t.appendActivity(d);
        t.appendActivity(e);
        t.appendActivity(f);
        logger.info("There are now " + t.getNumberOfActivities()
                + " Activities.");

        t.setDestination(c);
        logger.info("Outbound stops: " + t.getOutboundStopCount());
        logger.info("Inbound stops: " + t.getInboundStopCount());

        logger.info("Here is the tour: " + t);
        logger.info("It took " + t.duration() + " minutes.");
    }
}
