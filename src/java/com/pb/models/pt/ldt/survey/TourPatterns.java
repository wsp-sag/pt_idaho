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
 * Created on Aug 15, 2005 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt.ldt.survey;

import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.Household;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.HouseholdMemberIterator;
import com.pb.models.pt.surveydata.Location;
import com.pb.models.pt.surveydata.Tour;
import com.pb.models.pt.surveydata.TourIterator;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class TourPatterns {
    protected static Logger logger = Logger.getLogger("com.pb.ohsw");

    public final static int COMPLETE_TOUR = 0;

    public final static int BEGIN_TOUR = 1;

    public final static int END_TOUR = 2;

    public final static int ON_TOUR = 3;

    public final static int NON_TOUR = 4;

    public final static int AT_HOME = 5;

    public final static int HOUSEHOLD_TOUR = 0;

    public final static int WORK_TOUR = 1;

    public final static int OTHER_TOUR = 2;

    public final static int HOME = LongDistanceSurvey.HOME;

    public final static int WORK_RELATED = LongDistanceSurvey.WORK_RELATED;

    public final static int OTHER = LongDistanceSurvey.OTHER;

    private HashMap households;

    private LongDistanceSurvey lds;

    private DateFormat formatDate = new SimpleDateFormat("yyyy-MMM-dd");

    private DateFormat formatTime = new SimpleDateFormat("yyyy-MMM-dd'T'HH:mm");

    public TourPatterns(LongDistanceSurvey lds) {
        this.lds = lds;
        households = lds.getHouseholds();
        logger.info("Forming tours.");
        createTours();
        logger.info("Categorizing tours.");
        categorizeTours();
        logger.info("Looking for primary destinations");
        findPrimaryDestination();

        writeTours("tour_patterns.csv");

        // logger.info("Forming tour days.");
        // createTourDays();
        // logger.info("Categorizing tour days.");
        // categorizeTourDays();
        //
        // writePersonTourDays("tourDays.csv");
        logger.info("All done.");
    }

    private void createTours() {
        Iterator<HouseholdMember> memberIter = new HouseholdMemberIterator(lds);
        Iterator<Activity> actIter;
        HouseholdMember member;
        Activity activity = null;
        // Activity previousActivity = null;
        // ArrayList activityList;
        Tour tour;
        int t = 0;

        while (memberIter.hasNext()) {
            member = memberIter.next();
            int hm = member.getMember();
            long hh = member.getHousehold();

            Household household = lds.getHousehold(hh);
            Location home = household.getHome();
            // activityList = member.getActivities();

            // fake a home tour for members without activities
            if (!member.hasActivities()) {
                logger.info("Faking a tour for a member with no activities.");
                tour = new Tour(hh, hm, 0);
                activity = new Activity(hh, hm, 0);
                activity.setCoordinates(home.getXcord(), home.getYcord());
                activity.setAttribute("state", "OH");
                activity.setAttribute("stops", 0);
                activity.setPurpose("H");
                activity.setArrival(household.getSurveyStart());
                activity.setDeparture(household.getSurveyEnd());
                activity.setTaz(home.getTaz());
                tour.setPurpose("N");
                tour.setType(AT_HOME);
                tour.appendActivity(activity);
                tour.setDeparture(household.getSurveyStart());
                tour.setArrival(household.getSurveyEnd());
                tour.setAnchor(activity);
                tour.setDestination(activity);
                ArrayList<Activity> actList = new ArrayList<Activity>();
                actList.add(activity);
                member.setActivities(actList);
                member.appendTour(tour);
                continue;
            }

            actIter = member.getActivityIterator();

            // logger.info("Creating tours for " + household.getHousehold()
            // + ", " + member.getMember());

            tour = null;
            if (actIter.hasNext()) {
                t = 0;
                activity = actIter.next();
            }
            while (actIter.hasNext()) {
                t += 1;
                tour = new Tour(hh, hm, t);
                tour.appendActivity(activity);
                tour.setDeparture(activity.getDepartureCalendar());
                tour.setAnchor(activity);

                while (actIter.hasNext()) {
                    activity = actIter.next();
                    tour.appendActivity(activity);
                    if (activity.getType() == HOME) {
                        tour.setArrival(activity.getArrivalCalendar());
                        tour.setDestination(activity);
                        break;
                    }
                }

                // if the last activity is not home, then the tour
                // departure date is the same as the departure of the last
                // activity
                if (!activity.getPurpose().equals("H")) {
                    tour.setArrival(activity.getDepartureCalendar());
                }
                logger.info("Tour: " + hh + "," + hm + "," + t + ","
                        + tour.getPattern());
                member.appendTour(tour);
            } // activities

        }
    }

    private int compareDates(Calendar cal1, Calendar cal2) {
        return Activity.compareDates(cal1, cal2);
    }

    private void createTourDays() {
        Iterator<HouseholdMember> memberIter = new HouseholdMemberIterator(lds);
        Calendar cursor;
        Calendar end;
        int t;
        ArrayList<Activity> activities;

        while (memberIter.hasNext()) {
            HouseholdMember member = memberIter.next();
            long hh = member.getHousehold();

            Household household = lds.getHousehold(hh);
            Location home = household.getHome();
            int hm = member.getMember();
            Iterator<Tour> tourIter = member.getTourIterator();

            ArrayList<Tour> tourDays;

            Activity previousActivity = null;
            tourDays = new ArrayList<Tour>();

            // for members without any activities
            if (!tourIter.hasNext()) {
                logger.fatal("Member should have at least one tour.");
                // cursor will keep changing values, clone to keep the orginal
                // unchanged
                cursor = (Calendar) household.getSurveyStart().clone();
                end = household.getSurveyEnd();

                // fake home activity
                Activity activity = new Activity(hh, hm);
                activity.setTaz(home.getTaz());
                activity.setArrival((Calendar) cursor.clone());
                activity.setDeparture(end);
                activity.setPurpose("H");
                activity.setType(HOME);
                activity.setAttribute("state", "OH");
                activity.setAttribute("stops", 0);

                while (compareDates(cursor, end) <= 0) {
                    Tour tourDay = new Tour(hh, hm);
                    tourDay.setAttribute("tour", 0);
                    tourDay.setArrival((Calendar) cursor.clone());
                    tourDay.setDeparture((Calendar) cursor.clone());
                    tourDay.appendActivity(activity);
                    tourDay.setPurpose("N");
                    tourDay.setType(NON_TOUR);
                    tourDays.add(tourDay);

                    cursor.add(Calendar.DATE, 1);
                }
                member.setAttribute("tourDays", tourDays);
                continue;
            }

            /*
             * The first activity of the first tour begins on the first survey
             * day. The strategy is to initialize a cursor to this activity.
             * Increment the cursor while it is less then the departure time of
             * the activity. Every time the cursor is incremented, a new tourDay
             * is formed.
             */
            while (tourIter.hasNext()) {
                Tour tour = tourIter.next();
                t = tour.getTour();
                Iterator<Activity> activityIter = tour.getActivityIterator();

                activities = tour.getActivities();
                Activity activity = activities.get(0);
                // cursor will keep changing values, clone to keep the orginal
                // unchanged
                cursor = (Calendar) activity.getArrivalCalendar().clone();
                // logger.info("cursor: " + printDate(cursor));

                activity = activities.get(activities.size() - 1);
                end = activity.getArrivalCalendar();
                // logger.info("end: " + printDate(cursor));

                // go through the list of activities, creating new tourDays each
                // day
                Tour tourDay = null;
                previousActivity = null;

                while (activityIter.hasNext()) {
                    activity = activityIter.next();

                    // the cursor should never be after the arrival date of the
                    // activity
                    if (compareDates(cursor, activity.getArrivalCalendar()) > 0) {
                        logger.fatal("discarding out of order activity: " + hh
                                + "," + hm + "," + activity);
                        continue;
                    }

                    // start the first activity with a new tourDay
                    if (previousActivity == null) {
                        tourDay = new Tour(hh, hm);
                        tourDay.setAttribute("tour", t);
                        tourDay.setArrival((Calendar) cursor.clone());
                        tourDay.setMode(tour.getMode());
                        tourDay.setDeparture((Calendar) cursor.clone());
                        tourDay.setType(activity.getType());
                        tourDay.setPurpose(tour.getPurpose());
                        tourDay.appendActivity(activity);
                        tourDay.setAnchor(tour.getAnchor());
                        tourDay.setDestination(tour.getDestination());

                        previousActivity = activity;
                        continue;
                    }

                    // The cursor is before the departure of the previous
                    // activity: increment the cursor and form a new tourDay
                    // until the cursor matches the departure date.
                    while (compareDates(cursor, previousActivity
                            .getDepartureCalendar()) < 0) {
                        tourDays.add(tourDay);
                        cursor.add(Calendar.DATE, 1);
                        tourDay = new Tour(hh, hm);
                        tourDay.setAttribute("tour", t);
                        tourDay.setArrival((Calendar) cursor.clone());
                        tourDay.setMode(tour.getMode());
                        tourDay.setDeparture((Calendar) cursor.clone());
                        tourDay.setType(previousActivity.getType());
                        tourDay.setPurpose(tour.getPurpose());
                        tourDay.appendActivity(previousActivity);
                        tourDay.setAnchor(tour.getAnchor());
                        tourDay.setDestination(tour.getDestination());
                    }

                    // the activity starts on the cursor day
                    if (compareDates(cursor, activity.getArrivalCalendar()) == 0) {
                        tourDay.appendActivity(activity);
                        previousActivity = activity;
                        continue;
                    }

                    // The arrival of the current activity is after the cursor
                    // day: increment the cursor and start a new tourDay
                    while (compareDates(cursor, activity.getArrivalCalendar()) <= 0) {
                        tourDays.add(tourDay);
                        if (tourDay.getPattern().equals("HH")) {
                            logger.fatal("Home to home tourDay?");
                        }
                        cursor.add(Calendar.DATE, 1);
                        tourDay = new Tour(hh, hm);
                        tourDay.setAttribute("tour", t);
                        tourDay.setArrival((Calendar) cursor.clone());
                        tourDay.setMode(tour.getMode());
                        tourDay.setDeparture((Calendar) cursor.clone());
                        tourDay.setType(activity.getType()); // really there
                        tourDay.appendActivity(activity); // is no activity
                        tourDay.setPurpose(tour.getPurpose());
                        tourDay.setAnchor(tour.getAnchor());
                        tourDay.setDestination(tour.getDestination());
                    }

                    previousActivity = activity;
                } // activities

                // add tourDays until the end if the survey
                if (!tourIter.hasNext()) {
                    while (compareDates(cursor, activity.getDepartureCalendar()) < 0) {
                        tourDays.add(tourDay);

                        cursor.add(Calendar.DATE, 1);
                        // logger.info("cursor: " + printDate(cursor));

                        tourDay = new Tour(hh, hm);
                        tourDay.setAttribute("tour", t);
                        tourDay.setArrival((Calendar) cursor.clone());
                        tourDay.setMode(tour.getMode());
                        tourDay.setDeparture((Calendar) cursor.clone());
                        tourDay.setType(activity.getType());
                        tourDay.setPurpose(tour.getPurpose());
                        tourDay.appendActivity(activity);
                        tourDay.setAnchor(tour.getAnchor());
                        tourDay.setDestination(tour.getDestination());
                    }
                    tourDays.add(tourDay);
                    member.setAttribute("tourDays", tourDays);
                }
            }

        }
    }

    private void categorizeTours() {
        Iterator hhIter = lds.getHouseholdIterator();
        Iterator memberIter;
        Iterator tourIter;
        Iterator actIter;
        Household household;
        HouseholdMember member;
        ArrayList activities;
        Activity activity;
        Activity first;
        Activity last;
        Tour tour;

        String purpose;

        while (hhIter.hasNext()) {
            household = (Household) hhIter.next();
            long hh = household.getHousehold();
            memberIter = household.getMembers().keySet().iterator();

            while (memberIter.hasNext()) {
                member = household.getMember((Integer) memberIter.next());
                // int hm = member.getMember();

                tourIter = member.getTourIterator();

                while (tourIter.hasNext()) {
                    tour = (Tour) tourIter.next();

                    // skip tours that spent the whole time at home
                    try {
                        if (tour.getPurpose().equalsIgnoreCase("N")) {
                            logger.info("Stay-at-home non-tour.");
                            continue;
                        }
                    } catch (NullPointerException e) {
                        ;
                    }

                    activities = tour.getActivities();

                    first = (Activity) activities.get(0);
                    last = (Activity) activities.get(activities.size() - 1);

                    if (first.getType() == HOME && last.getType() == HOME) {
                        tour.setType(COMPLETE_TOUR);
                    } else if (first.getType() == HOME) {
                        tour.setType(BEGIN_TOUR);
                    } else if (last.getType() == HOME) {
                        tour.setType(END_TOUR);
                    } else {
                        tour.setType(NON_TOUR);
                    }

                    actIter = activities.iterator();

                    // assume other and look for work; check for household tours
                    // below
                    purpose = "O";

                    while (actIter.hasNext()) {
                        activity = (Activity) actIter.next();
                        if (activity.getPurpose().equalsIgnoreCase("W")) {
                            purpose = "W";
                            break;
                        }
                    }
                    tour.setPurpose(purpose);

                    // loop through activities for modes
                    actIter = activities.iterator();
                    int mode = LongDistanceSurvey.AUTO;
                    while (actIter.hasNext()) {
                        activity = (Activity) actIter.next();
                        mode = mode > activity.getMode() ? mode : activity
                                .getMode();
                    }
                    tour.setMode(mode);
                }
            }

            // check for household traveling together
            HouseholdMember member1 = null;
            HouseholdMember memberX = null;
            Tour tourX = null;
            ArrayList<Tour> tours;
            member1 = household.getMember(1);
            Iterator tourIter1 = member1.getTourIterator();
            Iterator tourIterX;

            while (tourIter1.hasNext()) {
                Tour tour1 = (Tour) tourIter1.next();
                tours = new ArrayList<Tour>();
                tours.add(tour1);

                memberIter = household.getMembers().keySet().iterator();
                while (memberIter.hasNext()) {
                    memberX = household.getMember((Integer) memberIter.next());
                    if (!member1.equals(memberX)) {

                        tourIterX = memberX.getTourIterator();
                        while (tourIterX.hasNext()) {
                            tourX = (Tour) tourIterX.next();

                            if (tour1.equals(tourX)) {
                                tours.add(tourX);
                                break;
                            }
                        }
                    }
                }

                // when the number of tours is equal to the number of
                // members then one tour was found for each household members
                if (tours.size() == household.getMemberCount()
                        && household.getMemberCount() > 1) {
                    tourIterX = tours.iterator();

                    while (tourIterX.hasNext()) {
                        tourX = (Tour) tourIterX.next();
                        tourX.setPurpose("H");
                    }
                }
            }
        }
    }

    /**
     * Find destinations for each tours. The anchor location is the location
     * farthest from home.
     * 
     * 
     */
    private void findPrimaryDestination() {
        Iterator tourIter = new TourIterator(lds);
        Iterator actIter;
        ArrayList<Activity> activities;
        Activity activity;
        Activity anchor;
        Activity first;
        Activity last;
        Activity destination;
        Tour tour;
        double maxDistance;
        double distance;

        while (tourIter.hasNext()) {
            tour = (Tour) tourIter.next();

            activities = tour.getActivities();

            // stay-at-home tours do not need processing
            if (activities.size() == 1
                    && activities.get(0).getPurpose().equals("H")) {
                continue;
            }

            // find the tour base
            first = activities.get(0);
            last = activities.get(activities.size() - 1);
            anchor = null;

            int type = tour.getType();
            if (type == COMPLETE_TOUR || type == BEGIN_TOUR) {
                anchor = first;
            } else if (type == END_TOUR) {
                anchor = last;
            } else {
                anchor = null;
            }
            tour.setAnchor(anchor);

            if (anchor == null) {
                continue;
            }

            actIter = activities.iterator();
            maxDistance = 0;
            distance = 0;
            destination = null;

            while (actIter.hasNext()) {
                activity = (Activity) actIter.next();
                distance = anchor.distanceMiles(activity);

                if (distance > maxDistance
                        && (tour.getPurpose().equals("H") || tour.getPurpose()
                                .equalsIgnoreCase(activity.getPurpose()))) {
                    destination = activity;
                    maxDistance = distance;
                }
            }

            if (destination == null) {
                logger.warn("Could not find a destination for tour: " + tour);
                tour.setType(NON_TOUR);
            } else {
                tour.setDestination(destination);
            }
        }
    }

    private void categorizeTourDays() {
        Iterator<Household> householdIter = lds.getHouseholdIterator();
        Household household;

        while (householdIter.hasNext()) {
            household = householdIter.next();
            // long hh = household.getHousehold();
            Iterator<HouseholdMember> memberIter = household
                    .getHouseholdMemberIterator();

            while (memberIter.hasNext()) {
                HouseholdMember member = memberIter.next();
                // int hm = member.getMember();

                ArrayList<Tour> tourDays = (ArrayList) member
                        .getAttribute("tourDays");
                Iterator<Tour> tourDaysIter = tourDays.iterator();

                while (tourDaysIter.hasNext()) {
                    Tour tourDay = tourDaysIter.next();
                    Iterator<Activity> activityIter = tourDay
                            .getActivityIterator();
                    int outStops = 0;
                    int inStops = 0;
                    int outActivities = 0;
                    int inActivities = 0;
                    boolean inbound = false;
                    boolean inOhio = false;

                    // need to establish tourDay type and count in/out stops
                    // and activities

                    boolean homeStart = false;
                    Activity activity = activityIter.next();
                    // Activity previousActivity = null;
                    if (activity.getType() == LongDistanceSurvey.HOME) {
                        homeStart = true;
                    }

                    if (activity.getAttributeAsString("state")
                            .equalsIgnoreCase("OH")) {
                        inOhio = true;
                    }

                    while (activityIter.hasNext()) {
                        activity = activityIter.next();

                        Activity destination = tourDay.getDestination();
                        if (destination != null && destination.equals(activity)) {
                            inbound = true;
                        }
                        int s;
                        try {
                            s = activity.getAttributeAsInt("stops");
                        } catch (NullPointerException e) {
                            s = 0;
                        }

                        if (activity.getAttributeAsString("state")
                                .equalsIgnoreCase("OH")) {

                            inOhio = true;
                        } else {
                            // activity from Ohio to out of region
                            if (!inbound && inOhio) {
                                outStops += s;
                                outActivities += 1;

                                // treat refused as 0
                                if (s == 99) {
                                    s = 0;
                                }
                            }
                            inOhio = false;
                        }

                        if (inOhio) {

                            if (inbound) {
                                inStops += s;
                                inActivities += 1;
                            } else {
                                outStops += s;
                                outActivities += 1;
                            }
                        }

                        if (inActivities > 10 || outActivities > 10) {
                            logger.debug("lots of activities");
                        }

                    }

                    // finished activity iteration
                    tourDay.setAttribute("outStops", outStops);
                    tourDay.setAttribute("outActivities", outActivities);
                    tourDay.setAttribute("inStops", inStops);
                    tourDay.setAttribute("inActivities", inActivities);

                    if (tourDay.getNumberOfActivities() == 1) {
                        if (homeStart) {
                            tourDay.setType(NON_TOUR);
                        } else {
                            tourDay.setType(ON_TOUR);
                        }
                    } else {

                        if (activity.getType() == LongDistanceSurvey.HOME) {
                            if (homeStart) {
                                tourDay.setType(COMPLETE_TOUR);
                            } else {
                                tourDay.setType(END_TOUR);
                            }
                        } else {
                            if (homeStart) {
                                tourDay.setType(BEGIN_TOUR);
                            } else {
                                tourDay.setType(ON_TOUR);
                            }
                        }
                    }
                }
            }

        }
    }

    public void writeTours(String fileName) {
        logger.info("Writing tour patterns to " + fileName);

        PrintWriter outStream = null;

        logger.info("Writing to tours to " + fileName);
        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(
                    fileName)));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        outStream.println("hh_id, per_id, tour, oneday, purpose, pattern, "
                + "type, anchor_taz, dest_taz, anchor_type, dest_type, "
                + "anchor_state, dest_state, tour_dept, tour_dept_day, "
                + "tour_arr, tour_arr_day, dest_arr, dest_arr_day, "
                + "dest_dept, dest_dept_day, survey_start, survey_end, mode");

        Iterator<Tour> tourIter = new TourIterator(lds);
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            long hh = tour.getHousehold();
            Household household = lds.getHousehold(hh);

            outStream.print(tour.getHousehold() + ",");
            outStream.print(tour.getMember() + ",");
            outStream.print(tour.getTour() + ",");
            try {
                if (compareDates(tour.getArrivalCalendar(), tour
                        .getDepartureCalendar()) == 0) {
                    outStream.print("1,");
                } else {
                    outStream.print("0,");
                }
            } catch (NullPointerException e) {
                outStream.print("0,");
            }
            outStream.print(tour.getPurpose() + ",");
            outStream.print(tour.getPattern() + ",");
            int type = tour.getType();
            outStream.print(type + ",");

            if (type == NON_TOUR) {
                outStream.print(-77777 + ",");
                outStream.print(-77777 + ",");
                outStream.print(-9 + ",");
                outStream.print(-9 + ",");
                outStream.print(-9 + ",");
                outStream.print(-9 + "\n");
            } else {
                if (tour.getAnchor().hasCoordinates()) {
                    outStream.print(tour.getAnchor().getTaz() + ",");
                } else {
                    outStream.print(-88888 + ",");
                }
                if (tour.getDestination().hasCoordinates()) {
                    outStream.print(tour.getDestination().getTaz() + ",");
                } else {
                    outStream.print(-88888 + ",");
                }
                try {
                    outStream.print(tour.getAnchor().getAttributeAsInt(
                            "landType")
                            + ",");
                } catch (NullPointerException e) {
                    outStream.print(-9999 + ",");
                }
                try {
                    outStream.print(tour.getDestination().getAttributeAsInt(
                            "landType")
                            + ",");
                } catch (NullPointerException e) {
                    outStream.print(-9999 + ",");
                }
                try {
                    outStream.print(tour.getAnchor().getAttributeAsString(
                            "state")
                            + ",");
                } catch (NullPointerException e) {
                    outStream.print("NA,");
                }
                try {
                    outStream.print(tour.getDestination().getAttributeAsString(
                            "state"));
                } catch (NullPointerException e) {
                    outStream.print("NA");
                }
                outStream.print(","
                        + formatTime.format(tour.getDepartureCalendar()
                                .getTime()));
                outStream
                        .print(","
                                + tour.getDepartureCalendar().get(
                                        Calendar.DAY_OF_WEEK));
                outStream.print(","
                        + formatTime
                                .format(tour.getArrivalCalendar().getTime()));
                outStream.print(","
                        + tour.getArrivalCalendar().get(Calendar.DAY_OF_WEEK));
                outStream.print(","
                        + formatTime.format(tour.getDestination()
                                .getArrivalCalendar().getTime()));
                outStream.print(","
                        + tour.getDestination().getArrivalCalendar().get(
                                Calendar.DAY_OF_WEEK));
                outStream.print(","
                        + formatTime.format(tour.getDestination()
                                .getDepartureCalendar().getTime()));
                outStream.print(","
                        + tour.getDestination().getDepartureCalendar().get(
                                Calendar.DAY_OF_WEEK));
                outStream.print(","
                        + formatTime.format(household.getSurveyStart()
                                .getTime()));
                outStream
                        .print(","
                                + formatTime.format(household.getSurveyEnd()
                                        .getTime()));
                outStream.print("," + tour.getMode());
                outStream.print("\n");
            }
        }

        logger.info("Finished writing tour patterns.");
    }

    public void writePersonTourDays(String fileName) {
        PrintWriter outStream = null;
        Tour tourDay;
        // Iterator tourDayIter;

        logger.info("Writing to person days to " + fileName);
        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(
                    fileName)));
            outStream.print("hh_id, per_id, worker, date, day, tour, type, "
                    + "purpose, pattern, mode, "
                    + "first_taz, destination_taz, last_taz, distance, "
                    + "tour_pattern, dept, arr");

            for (int i = 0; i < 8; ++i) {
                outStream.print(",act" + i + "_purp");
                outStream.print(",act" + i + "_taz");
                outStream.print(",act" + i + "_arr");
                outStream.print(",act" + i + "_dept");
                outStream.print(",act" + i + "_mode");
                outStream.print(",act" + i + "_type");
                outStream.print(",act" + i + "_stops");
            }
            outStream.println();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        Iterator<Household> householdIter = lds.getHouseholdIterator();
        Household household;

        while (householdIter.hasNext()) {
            household = householdIter.next();
            long hh = household.getHousehold();
            Iterator memberIter = household.getMembers().keySet().iterator();
            HouseholdMember member;

            while (memberIter.hasNext()) {
                member = household.getMember((Integer) memberIter.next());

                int hm = member.getMember();

                int worker = 0;

                if (member.isWorker()) {
                    worker = 1;
                }

                logger.info("Writing " + hh + "," + hm);

                ArrayList tourDays = (ArrayList) member
                        .getAttribute("tourDays");
                Iterator tourDaysIter = tourDays.iterator();

                while (tourDaysIter.hasNext()) {
                    tourDay = (Tour) tourDaysIter.next();

                    // discard records before and after the survey
                    if (compareDates(tourDay.getArrivalCalendar(), household
                            .getSurveyStart()) < 0) {
                        logger.info("Discarding a tourDay before the survey "
                                + "start date: " + hh + "," + hm + ","
                                + tourDay);
                        continue;
                    }
                    if (compareDates(tourDay.getDepartureCalendar(), household
                            .getSurveyEnd()) > 0) {
                        logger.info("Discarding a tourDay after the survey "
                                + "start date: " + hh + "," + hm + ","
                                + tourDay);
                        continue;
                    }

                    int t = tourDay.getAttributeAsInt("tour") - 1;

                    String tourPattern;
                    try {
                        tourPattern = member.getTour(t).getPattern();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        tourPattern = "-";
                    }

                    Calendar cal = tourDay.getDepartureCalendar();
                    String date = printDate(cal);
                    int day = cal.get(Calendar.DAY_OF_WEEK);
                    int mode = tourDay.getMode();
                    int type = tourDay.getType();
                    String purpose = tourDay.getPurpose();
                    String pattern = tourDay.getPattern();
                    long first = 0;
                    long last = 0;
                    // String dept = formatTime.format(tourDay
                    // .getDepartureCalendar().getTime());
                    String dept = printDate(tourDay.getDepartureCalendar());
                    // String arr = formatTime
                    // .format(tourDay.getArrivalCalendar());
                    String arr = printDate(tourDay.getArrivalCalendar());
                    int size = tourDay.getActivityCount();

                    if (size > 0) {
                        first = tourDay.getActivity(0).getTaz();
                        last = tourDay.getActivity(size - 1).getTaz();
                    }

                    Activity destination = tourDay.getDestination();
                    Activity anchor = tourDay.getAnchor();

                    double distance = 0;
                    long dest = 0;
                    if (destination != null) {
                        dest = destination.getTaz();
                        if (anchor != null) {
                            distance = destination.distanceMiles(anchor);
                        }
                    }

                    // logger.info("writing date " + date + " with stops "
                    // + outActivities + ", " + inActivities);
                    String record = hh + "," + hm + "," + worker + "," + date
                            + "," + day + "," + t + "," + type + "," + purpose
                            + "," + pattern + "," + mode + "," + first + ","
                            + dest + "," + last + "," + distance + ","
                            + tourPattern + "," + dept + "," + arr;

                    ArrayList activities = member.getActivities();
                    int i;
                    String arrival;
                    String departure;
                    for (i = 0; i < activities.size() && i < 8; ++i) {
                        Activity activity = (Activity) activities.get(i);
                        long taz = activity.getTaz();
                        mode = activity.getMode();
                        purpose = activity.getPurpose();
                        int stops = activity.getAttributeAsInt("stops");
                        type = activity.getType();

                        arrival = formatTime.format(activity
                                .getArrivalCalendar().getTime());
                        departure = formatTime.format(activity
                                .getDepartureCalendar().getTime());

                        record += "," + purpose + "," + taz + "," + arrival
                                + "," + departure + "," + mode + "," + type
                                + "," + stops;
                    }
                    for (; i < 8; ++i) {
                        int taz = -99999;
                        mode = -99999;
                        purpose = "-99999";
                        int stops = -99999;
                        type = -99999;
                        arrival = "-99999";
                        departure = "-99999";

                        record += "," + purpose + "," + taz + "," + arrival
                                + "," + departure + "," + mode + "," + type
                                + "," + stops;
                    }
                    outStream.println(record);
                    outStream.flush();
                }
            }
        }

    }

    private String printDate(Calendar cal) {
        return formatDate.format(cal.getTime());
    }

    public static void main(String[] args) {
        LongDistanceSurvey lds = new LongDistanceSurvey(args[0], args[1],
                args[2]);
        TourPatterns patterns = new TourPatterns(lds);
        patterns.createTourDays();
        patterns.writePersonTourDays(args[3]);
    }
}
