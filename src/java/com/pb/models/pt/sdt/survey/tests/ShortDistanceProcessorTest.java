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
 * Created on Dec 28, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.sdt.survey.tests;

import com.pb.models.pt.sdt.survey.ShortDistanceProcessor;
import com.pb.models.pt.sdt.survey.ShortDistanceSurvey;
import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.Location;
import com.pb.models.pt.surveydata.Tour;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.ArrayList;

public class ShortDistanceProcessorTest extends TestCase {
    protected Logger logger = Logger
            .getLogger(ShortDistanceProcessorTest.class);

    private ShortDistanceProcessor sdp;

    private Location home;

    private HouseholdMember member;

    public ShortDistanceProcessorTest(String name) {
        super(name);

        sdp = new ShortDistanceProcessor();
    }

    protected void setUp() throws Exception {
        super.setUp();

        home = new Location();
        home.setCoordinates(10000, 10000);
        home.setCoordinateSystem(Location.STATE_PLANE);

        member = new HouseholdMember(1, 1);
    }

    /**
     * Test the tour generalize method.
     */
    public void testFormHomeBasedTour() {
        logger.info("Testing tour formation.");

        ArrayList<Activity> activities = new ArrayList<Activity>();
        Activity activity;

        int a = 0;

        member.setWorker(true);

        activity = createActivityHelper(a++, "H", 3, 7, 1);
        activity.setCoordinates(home.getXcord(), home.getYcord());
        activities.add(activity);

        activity = createActivityHelper(a++, "O", 7.3, 7.4, 1);
        activity.setCoordinates(20000, 20000);
        activity.setMode(ShortDistanceSurvey.GMODE_AUTO_DRIVER);
        activities.add(activity);

        activity = createActivityHelper(a++, "O", 7.5, 7.7, 1);
        activity.setCoordinates(20000, 20000);
        activity.setMode(ShortDistanceSurvey.GMODE_AUTO_DRIVER);
        activities.add(activity);

        activity = createActivityHelper(a++, "O", 7.8, 8, 3);
        activity.setCoordinates(40000, 20000);
        activity.setMode(ShortDistanceSurvey.GMODE_AUTO_DRIVER);
        activities.add(activity);

        activity = createActivityHelper(a++, "W", 8.5, 19, 10);
        activity.setCoordinates(30000, 30000);
        activity.setMode(ShortDistanceSurvey.GMODE_AUTO_DRIVER);
        activities.add(activity);

        activity = createActivityHelper(a++, "H", 19.5, 2 + 59 / 60, 1);
        activity.setCoordinates(home.getXcord(), home.getYcord());
        activity.setMode(ShortDistanceSurvey.GMODE_AUTO_DRIVER);
        activities.add(activity);

        member.setActivities(activities);

        logger.info("Linking non-trips.");
        sdp.linkNonTrips(member);
        logger.info("Activity pattern: " + member.getActivityPattern());

        assertEquals("HOOWH", member.getActivityPattern());

        logger.info("Forming tours.");
        sdp.formHomeBasedTours(member, home);
        logger.info("Activity pattern: " + member.getActivityPattern());

        Tour tour = member.getTour(0);

        assertEquals("HOOWH", tour.getPattern());

        logger.info("Tour pattern to generalize: " + tour.getPattern());

        sdp.generalizeTour(member, tour, home);

        logger.info("Generalized tour pattern: " + tour.getPattern());
        assertEquals(4, tour.getActivityCount());
        assertEquals("HOWH", tour.getPattern());
        assertEquals(1, tour.getOutboundStopCount());
        assertEquals(0, tour.getInboundStopCount());
        assertEquals(10, sdp.findWorkLocation(member).getTaz());
    }

    private Activity createActivityHelper(int number, String purpose,
            double start, double end, int taz) {
        Activity activity = new Activity(1, 1, number);

        activity.setPurpose(purpose);
        activity.setArrival((int) start * 60);
        activity.setDeparture((int) end * 60);
        activity.setTaz(taz);
        activity.setCoordinateSystem(home.getCoordinateSystem());
        activity.setAttribute("party", 1);

        return activity;
    }
}
