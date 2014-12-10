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
 * Created on Jul 12, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.sdt.survey;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.pt.surveydata.AbstractSurvey;
import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.Household;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.Location;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * @author Andrew Stryker &lt;stryker@pbworld.com&gt;
 * 
 */
public class ShortDistanceSurvey extends AbstractSurvey {
    protected static Logger logger = Logger
            .getLogger(ShortDistanceSurvey.class);

    private CSVFileReader reader = new CSVFileReader();

    private File hhFile = null;

    private File perFile = null;

    private File actFile = null;

    // codes for survey
    public final static int OHSW = 1;

    public final static int MORPC = 2;

    public final static int OKI = 3;

    public final static int NOACA = 4;

    // relation to head of household
    public final static int HEAD = 1;

    public final static int SPOUSE = 2;

    public final static int CHILD = 3;

    public final static int PARENT = 4;

    public final static int RELATIVE = 5;

    public final static int NON_RELATIVE = 6;

    public final static int HELP = 7;

    // activty types
    public final static int HOME_DOMESTIC = 9;

    public final static int HOME_HYGIENE = 10;

    public final static int HOME_EAT = 11;

    public final static int HOME_WORK = 12;

    public final static int HOME_SHOP = 13;

    public final static int HOME_SOCREC = 14;

    public final static int HOME_SLEEP = 15;

    public final static int HOME_OTHER = 16;

    public final static int AWAY_WORK = 17;

    public final static int AWAY_SCHOOL = 18;

    public final static int AWAY_VOLUNTEER = 19;

    public final static int AWAY_PU_DO = 20;

    public final static int AWAY_SOCREC = 21;

    public final static int AWAY_BOARD = 22;

    public final static int AWAY_ALIGHT = 23;

    public final static int AWAY_SHOP = 24;

    public final static int AWAY_PERSONAL = 25;

    public final static int AWAY_EAT = 26;

    public final static int AWAY_DRIVE = 27;

    public final static int AWAY_OTHER = 28;

    public final static int AWAY_WORK_RELATED = 29;

    public final static int AWAY_SCHOOL_RELATED = 30;

    // survey modes
    private final static int AUTO_DRIVER = 11;

    private final static int AUTO_PASSENGER = 12;

    private final static int CARPOOL_DRIVER = 13;

    private final static int CARPOOL_PASSENGER = 14;

    private final static int VANPOOL_DRIVER = 15;

    private final static int VANPOOL_PASSENGER = 16;

    private final static int BUS = 17;

    private final static int SCHOOL_BUS = 18;

    private final static int TAXI = 19;

    private final static int WALK = 20;

    private final static int BIKE = 21;

    private final static int MOTORCYCLE = 22;

    private final static int COMVEH_DRIVER = 23;

    private final static int COMVEH_PASSENGER = 24;

    private final static int TRAIN = 25;

    private final static int AIRPLANE = 26;

    private final static int NOT_AVAILABLE = 98;

    // survey access
    public final static int WALK_ACCESS = 1;

    public final static int PNR_ACCESS = 2;

    public final static int KNR_ACCESS = 3;

    // general mode
    public final static int GMODE_AUTO_DRIVER = 0;

    public final static int GMODE_AUTO_PASSENGER = 1;

    public final static int GMODE_WALK = 2;

    public final static int GMODE_BIKE = 3;

    public final static int GMODE_TRANSIT = 4;

    public final static int GMODE_SBUS = 5;

    public final static int GMODE_COM = 6;

    public final static int GMODE_AIR = 7;

    public final static int GMODE_NA = 8;

    public final static int GMODE_OTHER = 9;

    // activities farther than MAX_DISTANCE from home should not be part of the
    // short distance model
    public final static int MAX_DISTANCE = 50;

    /**
     * 
     * @param hhFileName
     *            The household file.
     * @param perFileName
     *            The household member file.
     * @param actFileName
     *            The activity file.
     */
    public ShortDistanceSurvey(String hhFileName, String perFileName,
            String actFileName) {
        logger.info("Opening " + hhFileName + " as the household file.");
        hhFile = new File(hhFileName);
        logger.info("Populating the household data structure.");
        householdData(hhFile);

        logger.info("Opening " + perFileName + " as the person file.");
        perFile = new File(perFileName);
        logger.info("Populating the person data structure.");
        memberData(perFile);

        logger.info("Opening " + actFileName + " as the activity file.");
        actFile = new File(actFileName);
        logger.info("Populating the activity data structure.");
        activityData(actFile);

        logger.info("Finished populating data structures.");
    }

    /**
     * Populate the household data structure.
     * 
     */
    private void householdData(File hhFile) {
        TableDataSet hhTable = null;
        Household household = null;

        try {
            hhTable = reader.readFile(hhFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (int r = 1; r <= hhTable.getRowCount(); ++r) {
            // read columns from the TableDataSet
            long hh = new Long(hhTable.getStringValueAt(r, "hh_id"))
                    .longValue();
            int survey = (int) hhTable.getValueAt(r, "survey");
            hh = survey * 10000000 + hh;
            household = new Household(hh);
            household.setAttribute("survey", survey);
            household.setIncome((int) hhTable.getValueAt(r, "income"));
            household.setVehicles((int) hhTable.getValueAt(r, "totveh"));
            household.setWeight(hhTable.getValueAt(r, "expwgt"));

            Location home = new Location();
            double xcord = new Double(hhTable.getStringValueAt(r, "hh_xcord"))
                    .doubleValue();
            double ycord = new Double(hhTable.getStringValueAt(r, "hh_ycord"))
                    .doubleValue();
            if ((survey == MORPC && (xcord > 0 && ycord > 0))
                    || (xcord < 0 && xcord > -100 && ycord > 0 && ycord < 90)) {
                home.setCoordinates(xcord, ycord);
            } else {
                logger.fatal("Home location without coordinates: " + hh);
            }

            // set the coordinate system
            if (survey == MORPC) {
                home.setCoordinateSystem(Location.STATE_PLANE);
            } else {
                home.setCoordinateSystem(Location.DECIMAL_DEGREES);
            }

            long taz = (long) hhTable.getValueAt(r, "swtaz");
            home.setTaz(taz);
            household.setHome(home);

            households.put(new Long(hh), household);
        }
    }

    /**
     * Populate the member data structures.
     * 
     */
    private void memberData(File perFile) {
        TableDataSet perTable = null;
        Household household = null;
        HouseholdMember member = null;

        try {
            perTable = reader.readFile(perFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (int r = 1; r <= perTable.getRowCount(); ++r) {
            long hh = new Long(perTable.getStringValueAt(r, "hh_id"))
                    .longValue();

            int hm = (int) perTable.getValueAt(r, "per_id");
            int survey = (int) perTable.getValueAt(r, "survey");
            hh = survey * 10000000 + hh;
            household = getHousehold(hh);
            member = new HouseholdMember(hh, hm);

            int worker = (int) perTable.getValueAt(r, "worker");
            member.setAttribute("worker", worker);
            member.setWorker(worker == 1);

            int student = (int) perTable.getValueAt(r, "student");
            member.setAttribute("student", student);
            member.setStudent(student == 1);
            member.setAttribute("age", (int) perTable.getValueAt(r, "age"));
            member.setAttribute("gender", (int) perTable
                    .getValueAt(r, "gender"));

            member.setAttribute("occup", (int) perTable.getValueAt(r, "occup"));
            member.setAttribute("industry", (int) perTable.getValueAt(r,
                    "industry"));

            household.appendMember(member);
        }
    }

    /**
     * Populate activities.
     */
    private void activityData(File actFile) {
        TableDataSet actTable = null;
        Activity activity = null;
        Household household = null;
        Location home = null;
        HouseholdMember member = null;

        try {
            actTable = reader.readFile(actFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (int r = 1; r <= actTable.getRowCount(); ++r) {
            long hh = new Long(actTable.getStringValueAt(r, "hh_id"))
                    .longValue();
            int hm = (int) actTable.getValueAt(r, "per_id");
            int act = (int) actTable.getValueAt(r, "pl_no");
            int survey = (int) actTable.getValueAt(r, "survey");
            hh = survey * 10000000 + hh;

            // some households are removed due to inconsistent data
            if (!containsHousehold(hh)) {
                logger.warn("Skipping activities for " + hh + "," + hm + ","
                        + act);
                continue;
            }

            household = getHousehold(hh);
            home = household.getHome();

            if (!household.containsHouseholdMember(hm)) {
                logger.error("Activity for an unreported household member: "
                        + hh + "," + hm + "," + act
                        + "\nRemoving this Household from the dataset");
                removeHousehold(hh);
                continue;
            }

            member = household.getHouseholdMember(hm);

            activity = new Activity(hh, hm, act);
            double xcord = new Double(actTable.getStringValueAt(r, "xcord"))
                    .doubleValue();
            double ycord = new Double(actTable.getStringValueAt(r, "ycord"))
                    .doubleValue();
            if ((survey == MORPC && (xcord > 0 && ycord > 0))) {
                activity.setCoordinates(xcord, ycord);
                activity.setCoordinateSystem(Location.STATE_PLANE);
            } else if (xcord < 0 && xcord > -100 && ycord > 0 && ycord < 90) {
                activity.setCoordinates(xcord, ycord);
                activity.setCoordinateSystem(Location.DECIMAL_DEGREES);
            } else {
                logger.warn("Activity location without coordinates: " + hh
                        + "," + hm + "," + act);
            }

            int hour = (int) actTable.getValueAt(r, "dep_hr");
            int minute = (int) actTable.getValueAt(r, "dep_min");
            activity.setDeparture(hour, minute);

            hour = (int) actTable.getValueAt(r, "arr_hr");
            minute = (int) actTable.getValueAt(r, "arr_min");
            activity.setArrival(hour, minute);

            activity.setType((int) actTable.getValueAt(r, "trp_act1"));
            String purpose = codePurpose((int) actTable.getValueAt(r,
                    "trp_act1"));

            // place type attributes
            activity.setAttribute("pl_type", (int) actTable.getValueAt(r,
                    "pl_type"));

            // don't believe home unless the coordinates are the same
            if (purpose.equals("H") && home.hasCoordinates()
                    && activity.hasCoordinates()
                    && !home.compareCoordinates(activity)) {
                logger.warn("Home purpose not in home location: " + hh + ","
                        + hm + "," + act);
                purpose = "O";
            }

            // set the coordinate system
            if (survey == MORPC) {
                activity.setCoordinateSystem(Location.STATE_PLANE);
            } else {
                activity.setCoordinateSystem(Location.DECIMAL_DEGREES);
            }

            double delta;
            if (!activity.hasCoordinates() || !home.hasCoordinates()) {
                delta = -9;
            } else {
                delta = home.distance(activity);
            }
            if (delta > MAX_DISTANCE && !purpose.equals("W")) {
                if (purpose.equals("H")) {
                    logger.fatal("inconsistent home locations: " + hh + ","
                            + hm + "," + act + "\n(" + home.getXcord() + ","
                            + home.getYcord() + ") (" + activity.getXcord()
                            + "," + activity.getYcord() + ")");
                }
                purpose = purpose.toLowerCase();
            }
            activity.setPurpose(purpose);

            activity.setAttribute("gmode", codeModes((int) actTable.getValueAt(
                    r, "mode")));
            activity.setAttribute("access", (int) actTable.getValueAt(r,
                    "access"));
            activity.setAttribute("egress", (int) actTable.getValueAt(r,
                    "egress"));
            int party = (int) actTable.getValueAt(r, "party");

            // party size does not include respondent
            party = party >= 97 ? 0 : party;

            activity.setAttribute("party", party);
            activity.setTaz((long) actTable.getValueAt(r, "swtaz"));
            member.appendActivity(activity);
        }
    }

    /**
     * Code purposes for activities.
     */
    private String codePurpose(int code) {
        switch (code) {
        case HOME_DOMESTIC:
        case HOME_HYGIENE:
        case HOME_SHOP:
        case HOME_SOCREC:
        case HOME_SLEEP:
        case HOME_OTHER:
        case HOME_WORK:
        case HOME_EAT:
            return "H";
        case AWAY_WORK:
            return "W";
        case AWAY_SCHOOL:
            return "C";
        case AWAY_BOARD:
        case AWAY_ALIGHT:
            return "M";
        case AWAY_SHOP:
            return "S";
        case AWAY_SOCREC:
            return "R";
        case AWAY_PERSONAL:
        case AWAY_EAT:
        case AWAY_PU_DO:
        case AWAY_VOLUNTEER:
        case AWAY_DRIVE:
        case AWAY_OTHER:
        case AWAY_SCHOOL_RELATED:
            return "O";
        case AWAY_WORK_RELATED:
            return "X";
        default:
            return "U";
        }
    }

    /**
     * Convert specific activity access modes to general modes.
     */
    private int codeModes(int code) {
        switch (code) {
        case AUTO_DRIVER:
        case CARPOOL_DRIVER:
        case VANPOOL_DRIVER:
        case MOTORCYCLE:
            return GMODE_AUTO_DRIVER;
        case AUTO_PASSENGER:
        case CARPOOL_PASSENGER:
        case VANPOOL_PASSENGER:
        case TAXI:
            return GMODE_AUTO_PASSENGER;
        case BUS:
        case TRAIN:
            return GMODE_TRANSIT;
        case SCHOOL_BUS:
            return GMODE_SBUS;
        case WALK:
            return GMODE_WALK;
        case BIKE:
            return GMODE_BIKE;
        case COMVEH_DRIVER:
        case COMVEH_PASSENGER:
            return GMODE_COM;
        case AIRPLANE:
            return GMODE_AIR;
        case NOT_AVAILABLE:
            return GMODE_NA;
        default:
            return GMODE_OTHER;
        }
    }

    /**
     * Quick summary as a String.
     */
    public String toString() {
        return "Ohio Home Interview Surveys with these files: " + hhFile + ", "
                + perFile + ", " + actFile;
    }

    /**
     * Testing.
     * 
     * @param args
     *            the household, person, and activity file names.
     */
    public static void main(String[] args) {
        ShortDistanceSurvey sds = new ShortDistanceSurvey(args[0], args[1],
                args[2]);
        logger.info(sds);
        Household household = sds.getHousehold(4011361);
        logger.info("The 10th household looks like:\n" + household);
    }
}
