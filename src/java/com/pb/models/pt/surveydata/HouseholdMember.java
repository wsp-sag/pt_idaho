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
 * Created on Jun 30, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.surveydata;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Representation of a household member.
 * 
 * TODO: The iterator methods should use clones of their iteration targets. This
 * would allow modifications to the target data.
 * 
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class HouseholdMember extends AbstractSurveyData {
    private int member;

    private long household;

    private boolean worker;

    private boolean student;

    private boolean driver;

    private int age;

    private boolean female;

    private String pattern;

    private ArrayList<Activity> activities = null;

    private ArrayList<Trip> trips = null;

    private ArrayList<Tour> tours = null;

    /**
     * Constructor.
     */
    public HouseholdMember(long household, int member) {
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
     * @return Returns the member.
     */
    public int getMember() {
        return member;
    }

    /**
     * @return Returns the student.
     */
    public boolean isStudent() {
        return student;
    }

    /**
     * @param student
     *            The student to set.
     */
    public void setStudent(boolean student) {
        this.student = student;
    }

    /**
     * @return Returns the worker.
     */
    public boolean isWorker() {
        return worker;
    }

    /**
     * @param worker
     *            The worker to set.
     */
    public void setWorker(boolean worker) {
        this.worker = worker;
    }

    /**
     * @return Returns the driver.
     */
    public boolean isDriver() {
        return driver;
    }

    /**
     * @param driver
     *            The driver to set.
     */
    public void setDriver(boolean driver) {
        this.driver = driver;
    }

    /**
     * @return Returns the activities.
     */
    public ArrayList<Activity> getActivities() {
        return activities;
    }

    /**
     * @return The age of member, or a negative number if unknown.
     */
    public int getAge() {
        return age;
    }

    /**
     * Set the member age.
     * 
     * Use a negative number to denote an unknown age.
     * 
     * @param age
     */
    public void setAge(int age) {
        this.age = age;
    }

    public boolean isFemale() {
        return female;
    }

    public void setFemale(boolean female) {
        this.female = female;
    }

    /**
     * Overwrite the activity list.
     * 
     * @param Set
     *            the activity list
     */
    public void setActivities(ArrayList<Activity> activities) {
        this.activities = activities;
    }

    /**
     * @param activity
     *            The activity to add.
     */
    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    /**
     * 
     * @return Returns the number of activies.
     */
    public int getActivityCount() {
        return activities.size();
    }

    /**
     * @return Returns the total number of tours
     */
    public int getTourCount() {
        return tours.size();
    }

    /**
     * @return Returns number of tours of a given type.
     */
    public int getTourCount(int type) {
        int sum = 0;

        Iterator<Tour> tourIter = getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (tour.getType() == type) {
                sum += 1;
            }
        }

        return sum;
    }

    /**
     * @return Returns number of tours of a given purpose.
     */
    public int getTourCount(String purpose) {
        int sum = 0;

        Iterator<Tour> tourIter = getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (purpose.equals(tour.getPurpose())) {
                sum += 1;
            }
        }

        return sum;
    }

    /**
     * @return Returns the number of trips.
     */
    public int getTripCount() {
        return trips.size();
    }

    /**
     * Get Activity pattern.
     * 
     * @return The activity pattern.
     */
    public String getActivityPattern() {
        String pattern = "";

        if (activities != null && activities.size() > 0) {
            for (Activity activity : activities) {
                pattern += activity.getPurpose();
            }
        }

        return pattern;
    }

    /**
     * 
     * @return Returns an Iterator over activities.
     */
    public Iterator<Activity> getActivityIterator() {
        return activities.iterator();
    }

    /**
     * 
     * @param activity
     *            Append an Activity to the list of activities.
     */
    public void appendActivity(Activity activity) {
        if (activities == null) {
            activities = new ArrayList<Activity>();
        }
        activities.add(activity);
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
     * Sum the travel distance of each tour.
     * 
     * @return
     */
    public double getTourDistance() {
        Iterator<Tour> tourIter = getTourIterator();
        double sum = 0;

        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();
            double dist = tour.getDistance();

            if (dist < 0) {
                return -1;
            }

            sum += dist;
        }

        return sum;
    }

    /**
     * @return Returns the sum of travel distance for tours of a specified type.
     */
    public double getTourDistance(int type) {
        Iterator<Tour> tourIter = getTourIterator();
        double sum = 0;

        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (type == tour.getType()) {
                sum += tour.getDistance();
            }
        }

        return sum;
    }

    /**
     * @return Returns the sum of travel distance for tours of a specified
     *         purpose.
     */
    public double getTourDistance(String purpose) {
        Iterator<Tour> tourIter = getTourIterator();
        double sum = 0;

        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (purpose.equals(tour.getPurpose())) {
                sum += tour.getDistance();
            }
        }

        return sum;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * 
     * @return Returns the sequence of trips.
     */
    public ArrayList getTrips() {
        return trips;
    }

    /**
     * Overwrite the trip list.
     * 
     * @param trips
     *            ArrayList<Trip> of trips.
     */
    public void setTrips(ArrayList<Trip> trips) {
        this.trips = trips;
    }

    /**
     * Add a trip to the trip list.
     * 
     * @param trip
     */
    public void appendTrip(Trip trip) {
        if (trips == null) {
            trips = new ArrayList<Trip>();
        }
        trips.add(trip);
    }

    public void appendTour(Tour tour) {
        if (tours == null) {
            tours = new ArrayList<Tour>();
        }
        tours.add(tour);
    }

    public ArrayList<Tour> getTours() {
        return tours;
    }

    /**
     * Count the activities of a given purpose.
     * 
     * @param purpose
     *            Count activities matching this purpose.
     */
    public int getActivityCount(String purpose) {
        int i = 0;

        Iterator<Activity> actIter = getActivityIterator();
        while (actIter.hasNext()) {
            Activity activity = actIter.next();

            if (activity.getPurpose().equals(purpose)) {
                i += 1;
            }
        }

        return i;
    }

    /**
     * Count the activities of a given type.
     * 
     * @param purpose
     *            Count activities matching this type.
     */
    public int getActivityCount(int type) {
        int i = 0;

        Iterator<Activity> actIter = getActivityIterator();
        while (actIter.hasNext()) {
            Activity activity = actIter.next();

            if (activity.getType() == type) {
                i += 1;
            }
        }

        return i;
    }

    /**
     * Overwrite the tour list.
     * 
     * @param tours
     */
    public void setTours(ArrayList<Tour> tours) {
        this.tours = tours;
    }

    public Tour getTour(int t) {
        return tours.get(t);
    }

    /**
     * Iteration over a <i> cloned </i> copy of the tour list. This allows
     * modifications for the original data during iteration.
     * 
     * @return
     */
    public Iterator<Tour> getTourIterator() {
        return tours.iterator();
    }

    public boolean equals(HouseholdMember other) {
        if (household == other.getHousehold() && member == other.getMember()) {
            return true;
        }
        return false;
    }

    public boolean hasActivities() {
        if (activities != null && activities.size() > 0) {
            return true;
        }
        return false;
    }

    public boolean hasTours() {
        if (tours != null) {
            return true;
        }
        return false;
    }

    public boolean hasTrips() {
        if (trips != null) {
            return true;
        }
        return false;
    }

    /**
     * Summarize member in as a String.
     */
    public String toString() {
        return getActivityPattern();
    }

    /**
     * Test case -- to be written.
     * 
     * @param args
     */
    public static void main(String[] args) {
    }
}
