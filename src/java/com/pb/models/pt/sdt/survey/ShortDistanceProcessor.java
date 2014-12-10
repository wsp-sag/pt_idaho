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
 * Created on Sep 13, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.sdt.survey;

import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.Household;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.Location;
import com.pb.models.pt.surveydata.Tour;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class contians the processing logic to abstract and generalize the Ohio
 * short distance survey data.
 * 
 * @author Andrew Stryker
 * @version 1.0
 */
public class ShortDistanceProcessor {
    protected static Logger logger = Logger
            .getLogger(ShortDistanceProcessor.class);

    public final static int SURVEY_START_HOUR = 3;

    // member types

    // only home-based tours
    public final static int MEMBER_HOMEBASED = 1;

    public final static int MEMBER_WORKBASED = 2;

    // at home all day
    public final static int MEMBER_HOME = 3;

    public final static int MEMBER_FRAGMENTS = 4;

    // trip mode types
    public final static int TRIPM_DA = 0;

    public final static int TRIPM_SR2 = 1;

    public final static int TRIPM_SR3P = 2;

    public final static int TRIPM_WALK = 3;

    public final static int TRIPM_BIKE = 4;

    public final static int TRIPM_WK_TRN = 5;

    public final static int TRIPM_DR_TRN = 6;

    public final static int TRIPM_SBUS = 7;

    public final static int TRIPM_COMVEH = 8;

    public final static int TRIPM_AIRPLANE = 9;

    public final static int TRIPM_NOMODE = 10;

    public final static int TRIPM_OTHER = 99;

    // tour mode types
    public final static int TOURM_AUTO_DRV = 0;

    public final static int TOURM_AUTO_PASS = 1;

    public final static int TOURM_WALK = 2;

    public final static int TOURM_BIKE = 3;

    public final static int TOURM_WK_TRN = 4;

    public final static int TOURM_TRN_PASS = 5;

    public final static int TOURM_PASS_TRN = 6;

    public final static int TOURM_DRV_TRN = 7;
    
    public final static int TOURM_SBUS = 8;

    public final static int TOURM_OTHER = 9;

    // tour types
    public final static int HOME_BASED = 1;

    public final static int NON_HOME_START = 2;

    public final static int NON_HOME_END = 3;

    public final static int WORK_BASED = 4;

    public final static int NON_HOME_TOUR_FRAGMENT = 5;

    public final static int NON_TOUR = 6;

    /**
     * Constructor.
     * 
     *             The output file for tour patterns.
     */
    public ShortDistanceProcessor() {
    }

    /**
     * Form home-based tours from the complete list of tours in each
     * HouseholdMember's list of activities.
     * 
     * The primary destination is the destination with the same purpose as the
     * tour type. In cases where there is more than one activity sharing the
     * tour purpose, the actitivity with the longest duration becomes the
     * primary destination if the tour purpose is school or work. Otherwise the
     * activity farthest from home is the destination.
     */
    public void formHomeBasedTours(HouseholdMember member, Location home) {
        Activity activity;
        Activity anchor;
        Activity destination;
        Tour tour;
        ArrayList<Activity> activities;

        long hh = member.getHousehold();
        int hm = member.getMember();
        int t = 0;

        // specially handling for 1 activity members
        if (member.getActivityCount() == 1) {
            tour = new Tour(hh, hm, t);
            activities = member.getActivities();
            activity = activities.get(0);

            tour.appendActivity(activity);
            tour.setPurpose(activity.getPurpose());
            tour.setArrival(activity.getArrivalCalendar());
            tour.setDeparture(activity.getDepartureCalendar());
            tour.setAnchor(activity);
            tour.setDestination(activity);
            tour.setType(NON_TOUR);
            member.appendTour(tour);
            return;
        }

        activities = member.getActivities();
        Iterator<Activity> actIter = activities.iterator();
        tour = null;
        activity = actIter.next();
        anchor = activity;
        if (!activity.getPurpose().equals("H")) {
            logger.warn("Tour began without a home activity: " + hh + "," + hm);
        }

        // loop through all the activities
        while (actIter.hasNext()) {
            tour = new Tour(hh, hm, t++);
            if (activity.getPurpose().equals("H")) {
                tour.setType(HOME_BASED);
            } else {
                tour.setType(NON_HOME_START);
            }
            tour.appendActivity(activity);
            tour.setDeparture(activity.getDepartureMinute());
            String tourPurpose = "O";
            destination = null;

            // loop through activities until we run out of activities or
            // come back home
            while (actIter.hasNext()) {
                activity = actIter.next();
                tour.appendActivity(activity);
                String actPurpose = activity.getPurpose();

                // we came back home
                if (actPurpose.equals("H")) {
                    tour.setArrival(activity.getArrivalMinute());
                    break;
                }

                double deltaDestination = 0;
                if (destination != null) {
                    deltaDestination = home.distance(destination);
                }
                double deltaActivity = home.distance(activity);

                // lower case first character means a long distance
                // actitivity - we don't want these tours
                if (actPurpose.matches("^[a-z]")) {
                    if (destination == null || !tourPurpose.equals("L")
                            || deltaDestination < deltaActivity) {
                        destination = activity;
                    }
                    tourPurpose = "L";
                } else
                // school tours
                {
                    if (member.isStudent() && actPurpose.equals("C")
                            && !tourPurpose.equals("L")) {
                        if (!tourPurpose.equals("C")
                                || destination.duration() < activity.duration()) {
                            destination = activity;
                        }
                        tourPurpose = "C";
                    }
                    // work tours
                    else if (member.isWorker() && actPurpose.equals("W")
                            && !tourPurpose.equals("C")) {
                        if (!tourPurpose.equals("W")
                                || destination.duration() < activity.duration()) {
                            destination = activity;
                        }
                        tourPurpose = "W";
                    }
                    // shop tours
                    else if (actPurpose.equals("S") && !tourPurpose.equals("L")
                            && !tourPurpose.equals("C")
                            && !tourPurpose.equals("W")) {

                        if (!tourPurpose.equals("S") || destination == null) {
                            destination = activity;
                        } else if (tourPurpose.equals("S")) {
                            if ((deltaDestination < 0 && deltaActivity >= 0)
                                    || (deltaActivity >= 0 && deltaDestination < deltaActivity)
                                    || (deltaDestination < 0
                                            && deltaActivity < 0 && destination
                                            .duration() < activity.duration())) {
                                destination = activity;
                            }
                        }

                        tourPurpose = "S";
                    }
                    // recreation tours
                    else if (actPurpose.equals("R") && !tourPurpose.equals("L")
                            && !tourPurpose.equals("C")
                            && !tourPurpose.equals("W")
                            && !tourPurpose.equals("S")) {
                        if (!tourPurpose.equals("R") || destination == null) {
                            destination = activity;
                        } else if (tourPurpose.equals("R")) {
                            if ((deltaDestination < 0 && deltaActivity >= 0)
                                    || (deltaActivity >= 0 && deltaDestination < deltaActivity)
                                    || (deltaDestination < 0
                                            && deltaActivity < 0 && destination
                                            .duration() < activity.duration())) {
                                destination = activity;
                            }
                        }
                        tourPurpose = "R";
                    }
                    // other tours
                    else if (actPurpose.equals("O") && tourPurpose.equals("O")) {
                        if (destination == null
                                || (deltaDestination < 0 && deltaActivity >= 0)
                                || (deltaActivity >= 0 && deltaDestination < deltaActivity)
                                || (deltaDestination < 0 && deltaActivity < 0 && destination
                                        .duration() < activity.duration())) {
                            destination = activity;
                        }
                    }
                }
            }

            // handle HH tours -- there should not be any of these
            if (destination == null) {
                if (tour.getActivityCount() > 2) {
                    logger.fatal("No destination for this tour: " + tour);
                }
                logger.error("Home-to-home tour for member " + member);
                destination = activity;
            }

            tour.setDestination(destination);
            tour.setPurpose(tourPurpose);

            if (!activity.getPurpose().equals("H")) {
                logger.warn("Tour ended without a home activity: " + hh + ","
                        + hm);
                if (tour.getType() == HOME_BASED) {
                    tour.setType(NON_HOME_END);
                } else {
                    tour.setType(NON_HOME_TOUR_FRAGMENT);
                }
            } else if (!anchor.getPurpose().equals("H")) {
                anchor = activity;
            }

            tour.setAnchor(anchor);

            if (tour.getType() == HOME_BASED) {
                logger.info("Formed a home-based tour: " + tour);
            } else {
                logger.info("Formed a non-home based tour: " + tour);
            }

            member.appendTour(tour);
        }
    }

    /**
     * Link change-mode activites.
     * 
     * Remove change-mode activities from a HouseholdMember's sequence of
     * activities. Adjust access modes accordingly.
     */
    public void linkModes(HouseholdMember member) {
        ArrayList<Activity> linked;

        Activity activity;
        Activity previousActivity;

        // skip members who do not have activities
        if (!member.hasActivities()) {
            return;
        }

        long hh = member.getHousehold();
        int hm = member.getMember();
        Iterator<Activity> actIter = member.getActivityIterator();
        linked = new ArrayList<Activity>();
        previousActivity = null;

        while (actIter.hasNext()) {
            activity = actIter.next();

            // look for change mode activities
            if (activity.getType() == ShortDistanceSurvey.AWAY_BOARD) {
                logger
                        .info("Linking a boarding activity for " + hh + ", "
                                + hm);
                continue;
            }

            if (activity.getType() == ShortDistanceSurvey.AWAY_ALIGHT) {
                logger.info("Linking an alighting activity for " + hh + ", "
                        + hm);
                continue;
            }

            // bring previous mode forward from a linked activity
            int gMode = activity.getAttributeAsInt("gmode");
            if (previousActivity != null
                    && (previousActivity.getType() == ShortDistanceSurvey.AWAY_BOARD || previousActivity
                            .getType() == ShortDistanceSurvey.AWAY_ALIGHT)) {
                int pGMode = previousActivity.getMode();

                if (gMode == ShortDistanceSurvey.GMODE_TRANSIT) {
                    if (pGMode == ShortDistanceSurvey.GMODE_AUTO_DRIVER
                            || pGMode == ShortDistanceSurvey.GMODE_AUTO_PASSENGER
                            || activity.getAttributeAsInt("access") == ShortDistanceSurvey.PNR_ACCESS
                            || activity.getAttributeAsInt("access") == ShortDistanceSurvey.KNR_ACCESS) {
                        activity.setMode(TRIPM_WK_TRN);
                    } else if (pGMode == ShortDistanceSurvey.GMODE_WALK
                            || pGMode == ShortDistanceSurvey.GMODE_BIKE
                            || activity.getAttributeAsInt("access") == ShortDistanceSurvey.WALK_ACCESS) {
                        activity.setMode(TRIPM_WK_TRN);
                    }
                }
            } else {
                if (gMode == ShortDistanceSurvey.GMODE_WALK) {
                    activity.setMode(TRIPM_WALK);
                } else if (gMode == ShortDistanceSurvey.GMODE_BIKE) {
                    activity.setMode(TRIPM_BIKE);
                } else if (gMode == ShortDistanceSurvey.GMODE_AUTO_DRIVER) {
                    int party = 1 + activity.getAttributeAsInt("party");

                    if (party == 1) {
                        activity.setMode(TRIPM_DA);
                    } else if (party == 2) {
                        activity.setMode(TRIPM_SR2);
                    } else {
                        activity.setMode(TRIPM_SR3P);
                    }

                } else if (gMode == ShortDistanceSurvey.GMODE_TRANSIT) {
                    if (activity.getAttributeAsInt("access") == ShortDistanceSurvey.PNR_ACCESS) {
                        activity.setMode(TRIPM_DR_TRN);
                    } else {
                        activity.setMode(TRIPM_WK_TRN);
                    }
                } else {
                    activity.setMode(TRIPM_OTHER);
                }
            }

            linked.add(activity);
            previousActivity = activity;
        } // activities
        member.setActivities(linked);
    }

    /**
     * Link non-trips.
     * 
     * Look for sequential activities in the same place with the same purpose
     * and remove. Invoke this method before forming tours. All members must
     * have at least one activity.
     */
    public void linkNonTrips(HouseholdMember member) {

        if (!member.hasActivities()) {
            return;
        }

        Activity previous = null;
        ArrayList<Activity> linked = new ArrayList<Activity>();

        Iterator<Activity> activityIter = member.getActivityIterator();
        while (activityIter.hasNext()) {
            Activity activity = activityIter.next();

            // condtions for NOT linking:
            // * first time through OR
            // * different purpose OR
            // * (same purpose AND different location)
            if (previous == null
                    || !activity.getPurpose().equals(previous.getPurpose())
                    || (activity.getPurpose().equals(previous.getPurpose()) && !activity
                            .compareCoordinates(previous))) {
                linked.add(activity);
                previous = activity;
            } else {
                // linking - any mode information is skipped since the
                // location supposedly did not change
                logger.info("Linking activities for member: " + member);
                logger.info("First activity: " + previous);
                logger.info("Second activity: " + activity);
                previous.setDeparture(activity.getDepartureMinute());
            }
        }

        if (logger.getLevel() == Level.DEBUG) {
            logger.debug("unlinked: " + member);
        }

        member.setActivities(linked);

        if (logger.getLevel() == Level.DEBUG) {
            logger.debug("linked: " + member);
        }
    }

    /**
     * Code trip modes.
     */
    public void codeTripMode(Tour tour) {
        Iterator<Activity> actIter = tour.getActivityIterator();
        
        // advance - we don't care how someone got to the first activity
        if (actIter.hasNext()) {
            actIter.next();
        }

        while (actIter.hasNext()) {
            Activity activity = actIter.next();
            int gMode = activity.getAttributeAsInt("gmode");
            int access = activity.getAttributeAsInt("access");
//            int egress = activity.getAttributeAsInt("egress");
            int party = activity.getAttributeAsInt("party");

            switch (gMode) {
            case ShortDistanceSurvey.GMODE_SBUS:
                activity.setMode(TRIPM_SBUS);
                break;
            case ShortDistanceSurvey.GMODE_BIKE:
                activity.setMode(TRIPM_BIKE);
                break;
            case ShortDistanceSurvey.GMODE_WALK:
                activity.setMode(TRIPM_WALK);
                break;
            case ShortDistanceSurvey.GMODE_TRANSIT:
                if (access == ShortDistanceSurvey.PNR_ACCESS) {
                    activity.setMode(TRIPM_DR_TRN);
                } else {
                    activity.setMode(TRIPM_WK_TRN);
                }
                break;
            case ShortDistanceSurvey.GMODE_AUTO_DRIVER:
                if (party < 2) {
                    activity.setMode(TRIPM_DA);
                    break;
                }
            case ShortDistanceSurvey.GMODE_AUTO_PASSENGER:
                if (party == 2) {
                    activity.setMode(TRIPM_SR2);
                } else {
                    activity.setMode(TRIPM_SR3P);
                }
                break;
            default:
                activity.setMode(TRIPM_OTHER);
            }

        }
    }
    
    /**
     * Code tour modes.
     * 
     */
    public void codeTourMode(Tour tour) {
        Activity destination;
        int tourMode;
        boolean inbound;
        boolean sbus;

        Iterator<Activity> actIter = tour.getActivityIterator();
        destination = tour.getDestination();
        tourMode = TOURM_OTHER;
        inbound = false;
        sbus = false;
        
        // advance - we don't care how someone got to the first activity
        if (actIter.hasNext()) {
            actIter.next();
        }

        while (actIter.hasNext()) {
            Activity activity = actIter.next();
            int gMode = activity.getAttributeAsInt("gmode");
    
            switch (gMode) {
            case ShortDistanceSurvey.GMODE_SBUS:
                tourMode = TOURM_SBUS;
                sbus = true;
                break;
            case ShortDistanceSurvey.GMODE_AUTO_DRIVER:
                if (tourMode == TOURM_TRN_PASS || tourMode == TOURM_PASS_TRN
                        || tourMode == TOURM_WK_TRN || tourMode == TOURM_DRV_TRN) {
                    tourMode = TOURM_DRV_TRN;
                } else if (!sbus) {
                    tourMode = TOURM_AUTO_DRV;
                }
                break;
            case ShortDistanceSurvey.GMODE_AUTO_PASSENGER:
                if (tourMode == TOURM_AUTO_PASS || tourMode == TOURM_WALK
                        || tourMode == TOURM_BIKE || tourMode == TOURM_OTHER) {
                    tourMode = TOURM_AUTO_PASS;
                } else if (inbound && tourMode == TOURM_WK_TRN) {
                    tourMode = TOURM_TRN_PASS;
                }
                break;
            case ShortDistanceSurvey.GMODE_TRANSIT:
                if (tourMode == TOURM_WK_TRN || tourMode == TOURM_WALK
                        || tourMode == TOURM_BIKE || tourMode == TOURM_OTHER) {
                    tourMode = TOURM_WK_TRN;
                } else if (inbound) {
                    if (tourMode == TOURM_AUTO_PASS) {
                        tourMode = TOURM_PASS_TRN;
                    }
                } else if (tourMode == TOURM_AUTO_DRV) {
                    tourMode = TOURM_DRV_TRN;
                }
                break;
            case ShortDistanceSurvey.GMODE_BIKE:
                if (tourMode == TOURM_BIKE || tourMode == TOURM_WALK
                        || tourMode == TOURM_OTHER) {
                    tourMode = TOURM_BIKE;
                }
                break;
            case ShortDistanceSurvey.GMODE_WALK:
                if (tourMode == TOURM_WALK || tourMode == TOURM_OTHER) {
                    tourMode = TOURM_WALK;
                }
                break;
            }

            if (activity == destination) {
                inbound = true;
            }

        }
        tour.setMode(tourMode);
    }

    /**
     * Clean tour fragments and locations for school and work.
     * 
     * About 3% of the tours are tour fragements. That is, these sequences of
     * activities did not begn and end at home. Often this is because the person
     * was at work or some other activity. Invoke this method to consolidate
     * first activities that are away from home with a last activity that is
     * also away from home. This results in some persons not beginning the day
     * at 3:00am. That's okay.
     * 
     * While we are doing this, look for multiple work and school locations.
     * Since we only model one school and one work location, choose the school
     * and work location with longest duration and call that home.
     * 
     * Also, give persons without activities an all day at home activity.
     * 
     * And recode school and work activities for members that are not students
     * and workers, respectively, to have other as the purpose.
     */
    public void clean(HouseholdMember member) {
        ArrayList<Activity> preHome = new ArrayList<Activity>();
        ArrayList<Activity> activities = new ArrayList<Activity>();
        ArrayList<Activity> schools = new ArrayList<Activity>();
        ArrayList<Activity> works = new ArrayList<Activity>();
        Activity activity = null;

        if (!member.hasActivities()) {
            logger.info("Imputing an all day at home activity for " + member);
            long hh = member.getHousehold();
            int hm = member.getMember();
            activity = new Activity(hh, hm, 1);
            activity.setArrival(3, 00);
            activity.setDeparture(2, 59);
            activity.setPurpose("H");
            activity.setAttribute("gmode", ShortDistanceSurvey.GMODE_NA);

            activities.add(activity);
            member.setActivities(activities);
        }

        // some folks have just one activity and the code below does not
        // handle that, especially if the activity is not H
        if (member.getActivityCount() == 1) {
            return;
        }

        Iterator<Activity> activityIter = member.getActivityIterator();
        while (activityIter.hasNext()) {
            activity = activityIter.next();
            String purpose = activity.getPurpose();

            // start filling activities once the first home activity is
            // encountered
            if (activities.size() > 0 || purpose.equals("H")) {
                activities.add(activity);
            } else {
                preHome.add(activity);
            }

            // track school and work locations
            if (purpose.equals("C")) {
                schools.add(activity);
            } else if (purpose.equals("W")) {
                works.add(activity);
            }
        }

        // consolidate activities from a first and last non-work pattern
        // when there activities before first home
        if (preHome.size() > 0) {
            Activity first = preHome.get(0);

            // compare the first and last activities
            if (activity.getPurpose().equals(first.getPurpose())
                    && activity.hasCoordinates() && first.hasCoordinates()
                    && activity.compareCoordinates(first)) {
                logger.info("Consolidating first and last recorded "
                        + "activity: " + member);
                activity.setDeparture(first.getDepartureMinute());

                for (int i = 1; i < preHome.size(); ++i) {
                    activities.add(preHome.get(i));
                }
                member.setActivities(activities);
            }
        }

        // handle multiple schools
        if (schools.size() > 1) {
            Activity school = null;

            // find the activity with the longest school duration
            activityIter = schools.iterator();
            while (activityIter.hasNext()) {
                activity = activityIter.next();

                if (school == null || activity.duration() > school.duration()) {
                    school = activity;
                }
            }

            // label activities that do not have the same location as the
            // school activity as "O"
            activityIter = schools.iterator();
            while (activityIter.hasNext()) {
                activity = activityIter.next();

                if (!school.compareCoordinates(activity)) {
                    activity.setPurpose("O");
                }
            }
        }

        // handle multiple work locations
        if (works.size() > 1) {
            Activity work = null;

            // find the activity with the longest work duration
            activityIter = works.iterator();
            while (activityIter.hasNext()) {
                activity = activityIter.next();

                if (work == null || activity.duration() > work.duration()) {
                    work = activity;
                }
            }

            // label activities that do not have the same location as the
            // work activity as the work-related purpose
            activityIter = works.iterator();
            while (activityIter.hasNext()) {
                activity = activityIter.next();

                if (!work.compareCoordinates(activity)) {
                    activity.setPurpose("X");
                }
            }
        }

    }

    /**
     * Set the member types.
     */
    public void setMemberType(HouseholdMember member) {
        int hb = member.getTourCount(HOME_BASED);
        int wb = member.getTourCount(WORK_BASED);

        if (member.getTourCount() > hb + wb) {
            member.setType(MEMBER_FRAGMENTS);
        } else if (wb > 0) {
            member.setType(MEMBER_WORKBASED);
        } else {
            member.setType(MEMBER_HOMEBASED);
        }
    }

    /**
     * Generalize tours member and rebuild the activity pattern.
     * 
     * @param member
     */
    public void generalizeMemberTours(HouseholdMember member, Location home) {
        String pattern = "";
        Tour tour = null;

        Iterator<Tour> tourIter = member.getTourIterator();
        while (tourIter.hasNext()) {
            tour = tourIter.next();
            generalizeTour(member, tour, home);

            if (tour.getType() == HOME_BASED) {
                // do not pick up the last activity until we get to the last
                // tour(below)
                for (int i = 0; i < tour.getActivityCount() - 1; ++i) {
                    pattern += tour.getActivity(i).getPurpose();
                }
            }
        }

        // add the last activity to the pattern file
        pattern += tour.getActivity(tour.getActivityCount() - 1).getPurpose();

        if (logger.getLevel() == Level.DEBUG) {
            logger.debug("Generalized " + member.getHousehold() + ","
                    + member.getMember() + " from "
                    + member.getActivityPattern() + " to " + pattern);
        }

        member.setPattern(pattern);
    }

    /**
     * Generalize the tour.
     * 
     * Tours are generalized according to the number of tours a member has in
     * its pattern. Members with one tour are generalized to have only one
     * intermediate stop per journey. Members with more than one pattern are
     * also generalized so that there is at most one intermediate stop per
     * journey. The intermediate stops for members with more than 1 tour are
     * recoded to have purpose Other. The intermediate stops are generalized to
     * the stop with the greatest deviation from the straight-line distance
     * between the anchor (home) and the destination.
     * 
     * @param member
     * @param tour
     * @param home
     */
    public void generalizeTour(HouseholdMember member, Tour tour, Location home) {
        tour.setAttribute("inbnd_i", 0);
        tour.setAttribute("inbnd_j", 0);
        tour.setAttribute("inbnd_k", 0);
        tour.setAttribute("outbnd_i", 0);
        tour.setAttribute("outbnd_j", 0);
        tour.setAttribute("outbnd_k", 0);

        // no need to generalize a tour with 3 activities or tours that are not
        // home or work based
        if (tour.getActivityCount() <= 3
                || (tour.getType() != HOME_BASED && tour.getType() != WORK_BASED)) {
            return;
        }

        ArrayList<Activity> linked = new ArrayList<Activity>();
        Activity destination = tour.getDestination();

        // work-based tours are easy
        if (tour.getType() == WORK_BASED) {
            destination.setPurpose("O");
            linked.add(tour.getActivity(0));
            linked.add(destination);
            linked.add(tour.getActivity(tour.getActivityCount() - 1));
            tour.setActivities(linked);
            return;
        }

        Activity first = tour.getActivity(0);
        Activity stop = null;
        Activity last = tour.getActivity(tour.getActivityCount() - 1);

        // first activity of the tour
        linked.add(first);

        // pay attention, we are going to loop through activites and stop in the
        // middle
        if (tour.getOutboundStopCount() > 0) {
            for (int i = 1; tour.getActivity(i) != destination; ++i) {
                Activity activity = tour.getActivity(i);
                if (stop == null
                        || (!stop.hasCoordinates() && activity.hasCoordinates())
                        || (!stop.hasCoordinates()
                                && !activity.hasCoordinates() && activity
                                .duration() > stop.duration())
                        || activity.deviationDistance(home, destination) > stop
                                .deviationDistance(home, destination)) {
                    stop = activity;
                }
            }
        }

        if (stop != null) {

            if (member.getTourCount() > 1) {
                stop.setAttribute("orig_purp", stop.getPurpose());
                stop.setPurpose("O");
            }

            linked.add(stop);

            // look for generalizaions by location
            for (int j = 1; tour.getActivity(j) != destination; ++j) {
                Activity activity = tour.getActivity(j);

                if (activity == stop) {
                    continue;
                }

                if (activity.getTaz() == first.getTaz()) {
                    int cnt = tour.getAttributeAsInt("outbnd_i") + 1;
                    tour.setAttribute("outbnd_i", cnt);
                } else if (activity.getTaz() == stop.getTaz()) {
                    int cnt = tour.getAttributeAsInt("outbnd_j") + 1;
                    tour.setAttribute("outbnd_j", cnt);
                } else if (activity.getTaz() == destination.getTaz()) {
                    int cnt = tour.getAttributeAsInt("outbnd_k") + 1;
                    tour.setAttribute("outbnd_k", cnt);
                }
            }
        }

        linked.add(destination);
        stop = null;

        if (tour.getInboundStopCount() > 0) {
            for (int i = tour.getActivityCount() - 2; tour.getActivity(i) != destination; --i) {
                Activity activity = tour.getActivity(i);

                if (stop == null
                        || (!stop.hasCoordinates() && activity.hasCoordinates())
                        || (!stop.hasCoordinates()
                                && !activity.hasCoordinates() && activity
                                .duration() > stop.duration())
                        || activity.deviationDistance(home, destination) > stop
                                .deviationDistance(home, destination)) {
                    stop = activity;
                }
            }
        }
        
        if (stop != null) {

            // if (member.getTourCount() > 1) {
            // stop.setPurpose("O");
            //            }

            linked.add(stop);

            // look for generalizaions by location
            for (int j = tour.getActivityCount() - 2; tour.getActivity(j) != destination; --j) {
                Activity activity = tour.getActivity(j);

                if (activity == stop) {
                    continue;
                }

                if (activity.getTaz() == first.getTaz()) {
                    int cnt = tour.getAttributeAsInt("inbnd_i") + 1;
                    tour.setAttribute("inbnd_i", cnt);
                } else if (activity.getTaz() == stop.getTaz()) {
                    int cnt = tour.getAttributeAsInt("inbnd_j") + 1;
                    tour.setAttribute("inbnd_j", cnt);
                } else if (activity.getTaz() == destination.getTaz()) {
                    int cnt = tour.getAttributeAsInt("inbnd_k") + 1;
                    tour.setAttribute("inbnd_k", cnt);
                }
            }
        }

        linked.add(last);

        if (logger.getLevel() == Level.ALL) {
            String pattern = "";
            Iterator i = tour.getActivityIterator();

            Activity a;
            while (i.hasNext()) {
                a = (Activity) i.next();
                pattern += a.getPurpose();
            }

            logger.info("Generalized " + member.getHousehold() + ","
                    + member.getMember() + " from " + tour.getPattern()
                    + " to " + pattern);
        }

        tour.setActivities(linked);
    }

    /**
     * Create tour patterns.
     * 
     * Generalize daily activity patterns into a sequence of tour purposes.
     */
    public void simplifyTours(HouseholdMember member) {
        ArrayList<Tour> simplified = new ArrayList<Tour>();

        Iterator<Tour> tourIter = member.getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();
            long hh = tour.getHousehold();
            int hm = tour.getMember();
            int t = tour.getTour();

            Tour simple = new Tour(hh, hm, t);
            simple.appendActivity(tour.getActivity(0));
            simple.appendActivity(tour.getDestination());
            simple
                    .appendActivity(tour
                            .getActivity(tour.getActivityCount() - 1));

            simple.setAnchor(tour.getActivity(0));
            simple.setDestination(tour.getDestination());

            simplified.add(simple);
        }

        member.setTours(simplified);
    }

    /**
     * Work-based sub-tours.
     */
    public void formWorkSubTours(HouseholdMember member) {
        long hh = member.getHousehold();
        int hm = member.getMember();

        if (!member.hasTours()) {
            return;
        }

        ArrayList<Tour> tours = new ArrayList<Tour>();

        Iterator<Tour> tourIter = member.getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();
            int t = member.getTours().size();
            tours.add(tour);

            // skip non-work tours
            if (!tour.getPurpose().equalsIgnoreCase("W")) {
                continue;
            }

            // declarations for variables to dectect and describe a new work
            // sub-tour
            Iterator<Activity> actIter = tour.getActivityIterator();
            ArrayList<Activity> preWork = new ArrayList<Activity>();
            ArrayList<Activity> postWork = null;
            Tour workTour = null;
            Activity firstWork = null;
            Activity destination = null;

            while (actIter.hasNext()) {
                Activity activity = actIter.next();

                // look for the first work activity
                if (workTour == null) {
                    preWork.add(activity);

                    // first work sub-tour
                    if (activity.getPurpose().equals("W")) {
                        firstWork = activity;
                        workTour = new Tour(hh, hm, ++t);
                        workTour.setType(WORK_BASED);
                        // use a cloned activity object so that updates
                        // to firstWork object do not get reflected here
                        Activity firstWork2 = (Activity) activity.clone();
                        workTour.appendActivity(firstWork2);
                        workTour.setAnchor(firstWork2);
                        workTour.setPurpose("O");
                    }
                    continue;
                }

                // past the first work activity
                workTour.appendActivity(activity);

                // each non-work activity is possibly a destination and
                // possibly part of the post work list
                if (!activity.getPurpose().equals("W")) {
                    double deltaDestination = 0;

                    if (destination != null) {
                        firstWork.distance(destination);
                    }
                    double deltaActivity = firstWork.distance(activity);

                    if (destination == null
                            || (deltaDestination < 0 && deltaActivity >= 0)
                            || (deltaActivity >= 0 && deltaDestination < deltaActivity)
                            || (deltaDestination < 0 && deltaActivity < 0 && destination
                                    .duration() < activity.duration())) {
                        destination = activity;
                    }

                    // postWork is null until the second encounter with work
                    if (postWork != null) {
                        postWork.add(activity);
                    }

                    continue;
                }

                // must be the second (or later) work activity

                // work-based tours, so change the attributes of the
                // first work activity
                tour.setPurpose("B");
                firstWork.setPurpose("B");
                firstWork.setPurpose("B");
                firstWork.setDeparture(activity.getDepartureMinute());
                tour.setDestination(firstWork);

                // code the destination onto the work tour before
                // creating a new one and store
                if (destination == null) {
                    logger.fatal("Missing destination for tour " + workTour);
                    destination = activity;
                }
                workTour.setDestination(destination);
                tours.add(workTour);
                logger.info("Formed a work-based sub-tour: " + workTour);

                // start-up a new work sub-tour
                workTour = new Tour(hh, hm, ++t);
                workTour.setType(WORK_BASED);
                workTour.setPurpose("O");
                workTour.setAnchor(activity);
                workTour.appendActivity(activity);

                destination = null;
                postWork = new ArrayList<Activity>();
            }

            // if postWork is null, then there was not a work sub-tour
            if (postWork == null) {
                continue;
            }

            // adjust the activity list when there was a sub-tour
            ArrayList<Activity> linked = new ArrayList<Activity>();

            Iterator<Activity> iter = preWork.iterator();
            while (iter.hasNext()) {
                linked.add(iter.next());
            }

            iter = postWork.iterator();
            while (iter.hasNext()) {
                linked.add(iter.next());
            }

            tour.setActivities(linked);
        }
        member.setTours(tours);
    }

    /**
     * Household composition.
     * 
     * Look through household members and mark households accordingly.
     */
    public void codeHouseholdComposition(Household household) {
        int under5 = 0;
        int under15 = 0;

        Iterator<HouseholdMember> memberIter = household
                .getHouseholdMemberIterator();
        while (memberIter.hasNext()) {
            HouseholdMember member = memberIter.next();

            int age = member.getAttributeAsInt("age");

            if (age <= 15) {
                under15 += 1;
                if (age <= 5) {
                    under5 += 1;
                }
            }
        }

        household.setAttribute("under5", under5);
        household.setAttribute("under15", under15);
    }

    /**
     * Activity-pattern composition.
     * 
     * Mark tours every which way.
     */
    public void codeMemberPatternAttributes(HouseholdMember member) {
        int home = 0;
        int school = 0;
        int work = 0;
        int pbiz = 0;
        int shop = 0;
        int food = 0;
        int recreate = 0;
        int other = 0;
        int ldist = 0;

        // first loop through activities

        if (member.hasActivities()) {
            Iterator<Activity> actIter = member.getActivityIterator();
            while (actIter.hasNext()) {
                Activity activity = actIter.next();

                if (activity.getPurpose().equals("H")) {
                    home += 1;
                } else if (activity.getPurpose().equals("C")) {
                    school += 1;
                } else if (activity.getPurpose().equals("W")) {
                    work += 1;
                } else if (activity.getPurpose().equals("P")) {
                    pbiz += 1;
                } else if (activity.getPurpose().equals("S")) {
                    shop += 1;
                } else if (activity.getPurpose().equals("F")) {
                    food += 1;
                } else if (activity.getPurpose().equals("R")) {
                    recreate += 1;
                } else if (activity.getPurpose().equals("O")) {
                    other += 1;
                } else {
                    ldist += 1;
                }
            }
        }

        member.setAttribute("home_acts", home);
        member.setAttribute("school_acts", school);
        member.setAttribute("work_acts", work);
        member.setAttribute("pbiz_acts", pbiz);
        member.setAttribute("shop_acts", shop);
        member.setAttribute("food_acts", food);
        member.setAttribute("recreate_acts", recreate);
        member.setAttribute("other_acts", other);
        member.setAttribute("ldist_acts", ldist);

        // loop through tours

        home = 0;
        school = 0;
        work = 0;
        pbiz = 0;
        shop = 0;
        food = 0;
        recreate = 0;
        other = 0;
        ldist = 0;

        if (member.hasTours()) {
            Iterator<Tour> tourIter = member.getTourIterator();
            while (tourIter.hasNext()) {
                Tour tour = tourIter.next();

                if (tour.getPurpose().equals("C")) {
                    school += 1;
                } else if (tour.getPurpose().equals("W")) {
                    work += 1;
                } else if (tour.getPurpose().equals("P")) {
                    pbiz += 1;
                } else if (tour.getPurpose().equals("S")) {
                    shop += 1;
                } else if (tour.getPurpose().equals("F")) {
                    food += 1;
                } else if (tour.getPurpose().equals("R")) {
                    recreate += 1;
                } else if (tour.getPurpose().equals("O")) {
                    other += 1;
                } else {
                    ldist += 1;
                }
            }
        }

        member.setAttribute("school_tours", school);
        member.setAttribute("work_tours", work);
        member.setAttribute("pbiz_tours", pbiz);
        member.setAttribute("shop_tours", shop);
        member.setAttribute("food_tours", food);
        member.setAttribute("recreate_tours", recreate);
        member.setAttribute("other_tours", other);
        member.setAttribute("ldist_tours", ldist);
    }

    /**
     * Arrange tours in precedence order.
     * 
     * @param member
     *            The HouseholdMember whose tours need arranging.
     */
    public void prioritizeTours(HouseholdMember member) {
        ArrayList<Tour> prioritized = new ArrayList<Tour>();

        int priority = 0;
        // work and school first
        Iterator<Tour> tourIter = member.getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (member.getHousehold() == 21007800 && member.getMember() == 1) {
                logger.info("fire in the hole.");
            }

            if (tour.getType() != HOME_BASED) {
                logger.debug("Not prioritizing non-home based tour.");
                continue;
            }

            if (tour.getPurpose().equalsIgnoreCase("W")
                    || tour.getPurpose().equalsIgnoreCase("B")
                    || tour.getPurpose().equals("C")) {
                tour.setPriority(priority);
                priority += 1;
                prioritized.add(tour);
            }
        }

        // shop
        tourIter = member.getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (tour.getType() != HOME_BASED) {
                logger.debug("Skipping non-home based tour.");
                continue;
            }

            if (tour.getPurpose().equals("S")) {
                tour.setPriority(priority);
                priority += 1;
                prioritized.add(tour);
            }
        }

        // recreate
        tourIter = member.getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (tour.getType() != HOME_BASED) {
                logger.debug("Skipping non-home based tour.");
                continue;
            }

            if (tour.getPurpose().equals("R")) {
                tour.setPriority(priority);
                priority += 1;
                prioritized.add(tour);
            }
        }

        // other
        tourIter = member.getTourIterator();
        while (tourIter.hasNext()) {
            Tour tour = tourIter.next();

            if (tour.getType() != HOME_BASED) {
                logger.debug("Skipping non-home based tour.");
                continue;
            }

            if (tour.getPurpose().equals("O")) {
                tour.setPriority(priority);
                priority += 1;
                prioritized.add(tour);
            }
        }

        // all others
        // tourIter = member.getTourIterator();
        // while (tourIter.hasNext()) {
        // Tour tour = tourIter.next();
        //
        // if (!(tour.getPurpose().equalsIgnoreCase("W")
        // || tour.getPurpose().equalsIgnoreCase("B")
        // || tour.getPurpose().equalsIgnoreCase("C")
        // || tour.getPurpose().equalsIgnoreCase("S")
        // || tour.getPurpose().equalsIgnoreCase("R")
        // || tour.getPurpose().equalsIgnoreCase("O") || tour.getPurpose()
        // .equals("B"))) {
        // tour.setPriority(priority);
        // priority += 1;
        // prioritized.add(tour);
        // }
        // }

        member.setTours(prioritized);
    }

    /**
     * Find the work TAZ.
     * 
     * @param member
     *            HouseholdMember
     */
    public Location findWorkLocation(HouseholdMember member) {
        if (!member.isWorker()) {
            return null;
        }

        for (Activity activity : member.getActivities()) {
            if (activity.getPurpose().equalsIgnoreCase("W")) {
                return activity;
            }
        }

        return null;
    }
}