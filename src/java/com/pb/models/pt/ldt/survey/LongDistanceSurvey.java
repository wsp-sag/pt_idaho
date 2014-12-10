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
 * Created on Jun 22, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.ldt.survey;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.pt.surveydata.AbstractSurvey;
import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.Household;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.HouseholdMemberIterator;
import com.pb.models.pt.surveydata.Location;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class LongDistanceSurvey extends AbstractSurvey {
    protected static Logger logger = Logger.getLogger("com.pb.ohsw");

    private OLD_CSVFileReader reader = new OLD_CSVFileReader();

    private File hhFile = null;

    private File perFile = null;

    private File tripFile = null;

    public final static int HOME = 0;

    public final static int WORK_RELATED = 1;

    public final static int OTHER = 2;

    private final static int HOME_PTYPE = 1;


    // complete modes from survey
    private final static int PUBLIC_TRANSIT = 3;

    private final static int GREYHOUND = 4;

    private final static int RAIL_MODE = 5;

    private final static int COMMERCIAL_PLANE = 6;

    private final static int PRIVATE_PLANE = 7;

    private final static int SCHOOL_BUS = 8;

    // simple modes for estimation
    public final static int AUTO = 0;

    public final static int BUS = 1;

    public final static int RAIL = 2;

    public final static int AIR = 3;

    // trip purposes (there are 15 codes in total)
    private final static int WORK_RELATED_PURPOSE = 3;

    // minimum trip length to be in the survey
    public final static double SURVEY_DISTANCE = 40;

    /**
     * 
     * @param hhFileName
     *            Household file name
     * @param perFileName
     *            Person file name
     * @param tripFileName
     *            Trip file name
     */
    public LongDistanceSurvey(String hhFileName, String perFileName,
            String tripFileName) {
        logger.info("Opening " + hhFileName + " as the household file.");
        hhFile = new File(hhFileName);
        logger.info("Populating the household data structure.");
        householdData();

        logger.info("Opening " + perFileName + " as the person file.");
        perFile = new File(perFileName);
        logger.info("Populating the person data structure.");
        memberData();

        logger.info("Opening " + tripFileName + " as the trip file.");
        tripFile = new File(tripFileName);
        logger.info("Populating the trip data structure.");
        tripData();
        logger.info("Finished populating data structures.");

        logger.info("Linking and imputing activities.");
        link();
    }

    /**
     * Populate the household data structure.
     * 
     */
    private void householdData() {
        TableDataSet hhTable = null;
        Household household = null;
        Location home = null;

        try {
            hhTable = reader.readFile(hhFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // read columns from the TableDataSet
        for (int r = 1; r <= hhTable.getRowCount(); ++r) {
            long hh = (long) hhTable.getValueAt(r, "hh_id");
            household = new Household(hh);
            household.setIncome((int) hhTable.getValueAt(r, "income"));
            household.setVehicles((int) hhTable.getValueAt(r, "totveh"));
            household.setWeight(new Double(hhTable.getValueAt(r, "expwgt")));

            home = new Location();
            double xcord = new Double(hhTable.getStringValueAt(r, "xcord")).doubleValue();
            double ycord = new Double(hhTable.getStringValueAt(r, "ycord")).doubleValue();
            home.setCoordinates(xcord, ycord);
            household.setHome(home);

            // store the household in a HashMap
            households.put(hh, household);
        }
    }

    /**
     * Populate the member data structures.
     * 
     */
    private void memberData() {
        TableDataSet perTable = null;
        HouseholdMember member = null;

        try {
            perTable = reader.readFile(perFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // read columns from the TableDataSet
        for (int r = 1; r <= perTable.getRowCount(); ++r) {
            long hh = (long) perTable.getValueAt(r, "hh_id");
            int hm = (int) perTable.getValueAt(r, "per_id");
            member = new HouseholdMember(hh, hm);

            member.setAttribute("relation", new Integer((int) perTable
                    .getValueAt(r, "relation")));
            member.setAttribute("occupation", new Integer((int) perTable
                    .getValueAt(r, "occup")));
            member.setAttribute("industry", new Integer((int) perTable
                    .getValueAt(r, "industry")));
            member.setDriver((int) perTable.getValueAt(r, "lic") == 1);
            member.setAttribute("primact", new Integer((int) perTable
                    .getValueAt(r, "primact")));

            // need to set number of jobs
            int jobs = 0;
            if ((int) perTable.getValueAt(r, "primact") <= 2) {
                jobs += 1;
            }
            int jb = (int) perTable.getValueAt(r, "morejobs");
            if (jb > 0 && jb < 9) {
                jobs += jb;
            }
            member.setAttribute("jobs", new Integer(jobs));
            if (jobs > 1) {
                member.setWorker(true);
            } else {
                member.setWorker(false);
            }

            Household household = households.get(hh);
            household.appendMember(member);
        }
    }

    /**
     * Populate trips.
     */
    private void tripData() {
        BufferedReader in;
        try {
            in = new BufferedReader(new FileReader(tripFile));

            // map fields to column positions
            String line = in.readLine();
            String[] flds = line.split(",");
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            for (int i = 0; i < flds.length; ++i) {
                String key = flds[i];
                positions.put(key, i);
            }

            // parse each data line
            while ((line = in.readLine()) != null) {
                flds = line.split(",");
                long hh = new Long(flds[positions.get("hh_id")]);
                int hm = new Integer(flds[positions.get("per_id")]);
                int plano = new Integer(flds[positions.get("plano")]);

                Household household = getHousehold(hh);
                HouseholdMember member = household.getHouseholdMember(hm);
                Location home = household.getHome();

                // survey start
                int bdatea2 = new Integer(flds[positions.get("bdatea2")]);
                household.setSurveyStart(computeCalendarDate(bdatea2, 0));
                member.setAttribute("aflga", flds[positions.get("aflga")]);

                // survey end
                int edatea2 = new Integer(flds[positions.get("edatea2")]);
                household.setSurveyEnd(computeCalendarDate(edatea2, 2359));
                member.setAttribute("aflgb", flds[positions.get("aflgb")]);

                // origin activity
                Activity activity = new Activity(hh, hm);
                activity.setAttribute("plano", plano);

                int bdate = new Integer(flds[positions.get("bdate")]);
                int deptm = new Integer(flds[positions.get("deptm")]);
                Calendar departure = computeCalendarDate(bdate, deptm);
                activity.setDeparture(departure);

                int optype = new Integer(flds[positions.get("optype")]);
                activity.setType(activityCode(optype));

                double oxcord = new Double(flds[positions.get("oxcord")]);
                double oycord = new Double(flds[positions.get("oycord")]);
                if (oxcord != -99999 && oycord != -99999) {
                    activity.setCoordinates(oxcord, oycord);
                    int otaz = new Integer(flds[positions.get("otaz")]);
                    activity.setTaz(otaz);
                } else {
                    logger.info("Missing origin coordinates for " + hh + ","
                            + hm + "," + plano);
                    activity.setTaz(-88888);
                }

                String ocity = flds[positions.get("ocity")];
                activity.setAttribute("city", ocity);
                String ostate = flds[positions.get("ostate")];
                activity.setAttribute("state", ostate);
                int ocnty = new Integer(flds[positions.get("ocnty")]);
                activity.setAttribute("cnty", ocnty);
                int ozip = new Integer(flds[positions.get("ozip")]);
                activity.setAttribute("zip", ozip);
                activity.setAttribute("city", flds[positions.get("ocity")]);

                int opltype = new Integer(flds[positions.get("opltype")]);
                activity.setAttribute("landType", opltype);

                // changing an activity purpose because it is close to home
                // results in incorrect tour patterns
                //
                // e.g. H - O - O - H could become H - O - K - H
                // if (home.equals(activity)) {
                if (activity.getType() == HOME) {
                    if (ostate.equalsIgnoreCase("OH")) {
                        activity.setPurpose("H");
                    } else {
                        activity.setPurpose("h");
                    }
                } else {
                    if (ostate.equalsIgnoreCase("OH")) {
                        activity.setPurpose("O");
                    } else {
                        activity.setPurpose("o");
                    }
                }

                activity.setAttribute("stops", 0);

                member.appendActivity(activity);

                // destination activity
                activity = new Activity(hh, hm);
                activity.setAttribute("plano", plano);

                int edate = new Integer(flds[positions.get("edate")]);
                int arrtm = new Integer(flds[positions.get("arrtm")]);
                Calendar arrival = computeCalendarDate(edate, arrtm);
                activity.setArrival(arrival);

                int dptype = new Integer(flds[positions.get("dptype")]);
                activity.setType(activityCode(dptype));

                double dxcord = new Double(flds[positions.get("dxcord")]);
                double dycord = new Double(flds[positions.get("dycord")]);
                if (dxcord != -99999 && dycord != -99999) {
                    activity.setCoordinates(dxcord, dycord);
                    int dtaz = new Integer(flds[positions.get("dtaz")]);
                    activity.setTaz(dtaz);
                } else {
                    logger.info("Missing destination coordinates for " + hh
                            + "," + hm + "," + plano);
                    activity.setTaz(-88888);
                }

                String dcity = flds[positions.get("dcity")];
                activity.setAttribute("city", dcity);
                String dstate = flds[positions.get("dstate")];
                activity.setAttribute("state", dstate);
                int dcnty = new Integer(flds[positions.get("dcnty")]);
                activity.setAttribute("cnty", dcnty);
                int dzip = new Integer(flds[positions.get("dzip")]);
                activity.setAttribute("zip", dzip);
                activity.setAttribute("city", flds[positions.get("dcity")]);

                int dpltype = new Integer(flds[positions.get("dpltype")]);
                activity.setAttribute("landType", dpltype);

                // code trip data into the destination
                int purpose = new Integer(flds[positions.get("tpur")]);

                if (activity.getType() == HOME) {
                    if (home.equals(activity)) {
                        activity.setPurpose("H");
                    } else {
                        // does not happen -- yeah!
                        if (activity.getAttributeAsString("state")
                                .equalsIgnoreCase("OH")) {
                            activity.setPurpose("V");
                        } else {
                            activity.setPurpose("v");
                        }
                    }
                } else if (purpose == WORK_RELATED_PURPOSE) {
                    if (dstate.equalsIgnoreCase("OH")) {
                        activity.setPurpose("W");
                    } else {
                        activity.setPurpose("w");
                    }
                } else {
                    if (dstate.equalsIgnoreCase("OH")) {
                        activity.setPurpose("O");
                    } else {
                        activity.setPurpose("o");
                    }
                }

                int mode = new Integer(flds[positions.get("mode")]);
                activity.setMode(mode);

                int stops = new Integer(flds[positions.get("stops")]);
                activity.setAttribute("stops", stops);

                member.addActivity(activity);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Link out origin activities and impute activities where needed.
     */
    public void link() {
        Household household;
        HouseholdMember member;
        Activity origin;
        Activity destination = null;
        // Activity activity;
        Location home;

        ArrayList<Activity> unlinked;
        ArrayList<Activity> linked;

        Iterator hmIter = new HouseholdMemberIterator(this);
        Iterator actIter = null;

        while (hmIter.hasNext()) {
            member = (HouseholdMember) hmIter.next();
            unlinked = member.getActivities();
            linked = new ArrayList<Activity>();
            int hm = member.getMember();
            long hh = member.getHousehold();

            household = this.getHousehold(hh);
            home = household.getHome();

            if (!member.hasActivities()) {
                continue; // no activities, go to next member
            }

            actIter = unlinked.iterator();

            // special processing for the first activity
            origin = (Activity) actIter.next();
            // handle activities before the start of the survey
            if (Activity.compareDates(origin.getDepartureCalendar(), household
                    .getSurveyStart()) < 0) {
                origin.setArrival(origin.getDepartureCalendar());
            } else {
                origin.setArrival(household.getSurveyStart());
            }
            // insert a home activity when the first activity near the home
            if (origin.getType() == OTHER
                    && origin.distanceMiles(home) < SURVEY_DISTANCE
                    && Activity.compareDates(household.getSurveyStart(), origin
                            .getDepartureCalendar()) < 0) {
                Activity activity = new Activity(hh, hm);
                activity.setTaz(home.getTaz());
                activity.setArrival(household.getSurveyStart());
                activity.setDeparture((Calendar) origin.getDepartureCalendar()
                        .clone());
                activity.setPurpose("J");
                activity.setType(HOME);
                activity.setAttribute("state", "OH");
                activity.setAttribute("stops", 0);
                activity.setCoordinates(home.getXcord(), home.getXcord());
                linked.add(activity);

                origin.setArrival(origin.getDepartureCalendar());
            }
            linked.add(origin);

            while (actIter.hasNext()) {
                destination = (Activity) actIter.next();

                if (!actIter.hasNext()) {
                    continue; // previous destination was the last
                }

                origin = (Activity) actIter.next();

                destination.setDeparture(origin.getDepartureCalendar());
                linked.add(destination);

                /*
                 * Examine origins and impute trips as necessary. Remember, the
                 * destination is the destination that _proceeds_ the origin.
                 */

                double delta = origin.distanceMiles(destination);
                int dType = destination.getType();
                int oType = origin.getType();
                int plano = origin.getAttributeAsInt("plano");
                String oState = origin.getAttributeAsString("state");
                String dState = destination.getAttributeAsString("state");

                // delta assumed to be 0 if places match and the coordinates
                // are not known
                if (!origin.hasCoordinates() || !destination.hasCoordinates()) {
                    // this happens too often to report
                    // logger.info("Compensated for bad geocodes: " + hh
                    // + ", " + hm + ", " + plano);

                    String oCity = origin.getAttributeAsString("city");
                    String dCity = destination.getAttributeAsString("city");

                    int oZip = origin.getAttributeAsInt("zip");
                    int dZip = destination.getAttributeAsInt("zip");

                    if (oCity.equalsIgnoreCase(dCity)
                            || (oZip != 99999 && oZip == dZip)
                            || (!oState.equalsIgnoreCase("OH") && oState
                                    .equalsIgnoreCase(dState))) {
                        delta = 0;
                    } else {
                        delta = 999;
                    }
                }

                /*
                 * matching location types: ensure that everything is good and
                 * discard the origin if it checks or add the origin to the
                 * linked trips if not
                 * 
                 * recall that origins are never work
                 */
                if (oType == dType || (oType == OTHER && dType == WORK_RELATED)) {
                    if (delta > SURVEY_DISTANCE) {
                        if (oType == HOME) {
                            // this never happens -- yeah!
                            logger.fatal("Two home locations:" + hh + ", " + hm
                                    + ", " + plano);
                        } else {
                            // impute the activity
                            if (oState.equalsIgnoreCase("OH")) {
                                origin.setPurpose("M");
                            } else {
                                origin.setPurpose("m");
                            }
                            origin.setArrival(origin.getDepartureCalendar());
                            origin.setMode(destination.getMode());
                            linked.add(origin);
                        }
                    }
                    continue;
                }

                double homeDestinationDelta = home.distanceMiles(destination);
                double homeOriginDelta = home.distanceMiles(origin);

                /*
                 * missing a home activity. add the origin as an activity
                 */
                if (dType == HOME) {
                    destination.setPurpose("J");
                    if (homeOriginDelta > SURVEY_DISTANCE) {
                        logger
                                .warn("Origin should be close to home but is not:"
                                        + hh + ", " + hm + ", " + plano);
                    }
                    origin.setArrival(origin.getDepartureCalendar());
                    origin.setMode(destination.getMode());
                    linked.add(origin);

                    continue;
                }

                /*
                 * destination to home missing -- impute home arrival attributes
                 */
                if (homeDestinationDelta > SURVEY_DISTANCE) {
                    logger.warn("Long other to home trip not recorded:" + hh
                            + ", " + hm + ", " + plano);
                } else {
                    logger.info("Short other to home trip not recorded:" + hh
                            + ", " + hm + ", " + plano);
                }

                origin.setArrival(origin.getDepartureCalendar());
                origin.setMode(destination.getMode());
                if (oState.equalsIgnoreCase("OH")) {
                    origin.setPurpose("K");
                } else {
                    origin.setPurpose("k");
                }
                linked.add(origin);

                // continue;
            }

            // add the last destination to the activity list
            // some respondents report activities past the end of the survey
            if (Activity.compareDates(destination.getArrivalCalendar(),
                    household.getSurveyEnd()) > 0) {
                destination.setDeparture(destination.getArrivalCalendar());
            } else {
                destination.setDeparture(household.getSurveyEnd());
            }
            linked.add(destination);

            // replace the unliked activities
            member.setActivities(linked);
        }
    }

    /**
     * Turn the survey date and times into a Calendar object.
     * 
     * NOTE: Using the built-in date formats would have been better.
     * 
     * @param date
     * @param time
     * @return date as a Calendar
     */
    private Calendar computeCalendarDate(int date, int time) {
        // date YYYYMMMMDD
        int year = date / 10000;
        int month = (date % 10000) / 100 - 1; // months are 0-indexed
        int day = date % 100;

        int hour = time / 100;
        int minute = time % 100;

        Calendar cal = new GregorianCalendar();
        cal.clear();
        cal.set(year, month, day, hour, minute);

        return cal;
    }

    /**
     * Convert to simple activity codes.
     * 
     * @param code
     * @return The activity code.
     */
    private int activityCode(int code) {
        if (code == HOME_PTYPE) {
            return HOME;
        }
        return OTHER;
    }

    /**
     * Convert modes to simple mode codes. Default mode is AUTO.
     * 
     * @param mode
     * @return Simple mode
     */
    private int surveyMode(int mode) {
        switch (mode) {
        case PUBLIC_TRANSIT:
        case GREYHOUND:
        case SCHOOL_BUS:
            return BUS;
        case RAIL_MODE:
            return RAIL;
        case COMMERCIAL_PLANE:
        case PRIVATE_PLANE:
            return AIR;
        default:
            return AUTO;
        }
    }

    /**
     * Quick summary as a String.
     */
    public String toString() {
        return "Ohio Long Distance Survey with these files: " + hhFile + ", "
                + perFile + ", " + tripFile;
    }

    /**
     * Testing.
     * 
     * @param args
     *            the household, person, and trip file names.
     */
    public static void main(String[] args) {
        LongDistanceSurvey lds = new LongDistanceSurvey(args[0], args[1],
                args[2]);
        logger.info(lds);
        logger.info("All done.");
    }
}
