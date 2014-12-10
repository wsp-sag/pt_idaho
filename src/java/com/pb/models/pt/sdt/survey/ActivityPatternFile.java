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
 * Created on Dec 14, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.sdt.survey;

import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.Household;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.HouseholdMemberIterator;
import com.pb.models.pt.surveydata.Location;
import com.pb.models.pt.surveydata.Tour;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

public class ActivityPatternFile {
    protected static Logger logger = Logger
            .getLogger(ActivityPatternFile.class);

    private ShortDistanceProcessor sdp;

    private ShortDistanceSurvey sds;

    /**
     * Constructor.
     * 
     * Reads the survey data.
     * 
     * @param hhFileName
     * @param perFileName
     * @param actFileName
     */
    public ActivityPatternFile(String hhFileName, String perFileName,
            String actFileName) {
        sdp = new ShortDistanceProcessor();
        sds = new ShortDistanceSurvey(hhFileName, perFileName, actFileName);
    }

    /**
     * Write tours with attributes to file.
     */
    public void writeActivityPatterns(String fileName) {
        PrintWriter outStream = null;

        logger.info("Writing activity patterns to " + fileName);

        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(
                    fileName)));
        } catch (IOException e) {
            logger.fatal("Could not write to " + fileName);
            e.printStackTrace();
            System.exit(1);
        }

        outStream
                .println("survey, hh_id, per_id, income, hhsize, hhsize5, "
                        + "hhsize15, vehicles, worker, student, tours, pattern, "
                        + "activities, cplx_work, distance, gpattern, gdistance, "
                        + "hexpfact, w_acts, c_acts, "
                        + "s_acts, r_acts, o_acts, w_tours, c_tours, s_tours, r_tours, "
                        + "o_tours, b_tours, istops1, istops2, istops3, "
                        + "istops4p, w_istops, nw_istops, w_stops, c_stops, "
                        + "s_stops, r_stops, o_stops, purp_t1, purp_t2, "
                        + "purp_t3, purp_t4, occ, ind, age, hh_studs, hh_works,"
                        + "home_taz, work_taz, home_work_dist");

        Iterator<HouseholdMember> memberIter = new HouseholdMemberIterator(sds);

        while (memberIter.hasNext()) {
            HouseholdMember member = memberIter.next();
            long hh = member.getHousehold();
            Household household = sds.getHousehold(hh);
            Location home = household.getHome();

            sdp.codeHouseholdComposition(household);
            int hm = member.getMember();

            sdp.linkModes(member);
            sdp.clean(member);
            sdp.linkNonTrips(member);
            sdp.formHomeBasedTours(member, home);
            sdp.formWorkSubTours(member);

            Iterator<Tour> tourIter = member.getTourIterator();
            while (tourIter.hasNext()) {
                Tour tour = tourIter.next();
                sdp.codeTourMode(tour);
            }

            outStream.print(household.getAttributeAsInt("survey"));
            outStream.print("," + hh + "," + hm);
            outStream.print("," + household.getIncome());
            outStream.print("," + household.getMemberCount());
            outStream.print("," + household.getAttributeAsInt("under5"));
            outStream.print("," + household.getAttributeAsInt("under15"));
            outStream.print("," + household.getVehicles());
            outStream.print("," + member.isWorker());
            outStream.print("," + member.isStudent());
            outStream.print("," + member.getTourCount());
            outStream.print("," + member.getActivityPattern());
            outStream.print("," + member.getActivityCount());
            outStream.print(","
                    + (member.getActivityCount("B") + member
                            .getActivityCount("b")));
            // outStream
            // .print(","
            // + member
            // .getTourDistance(ShortDistanceProcessor.HOME_BASED));

            outStream.print("," + member.getTourDistance());

            // generalize the tours
            sdp.generalizeMemberTours(member, home);

            outStream.print("," + member.getPattern());
            outStream.print("," + member.getTourDistance());

            outStream.print("," + household.getWeight());

            // activity counts
            int w_acts = 0;
            int c_acts = 0;
            int s_acts = 0;
            int r_acts = 0;
            int o_acts = 0;
            int b_acts = 0;
            tourIter = member.getTourIterator();
            while (tourIter.hasNext()) {
                Tour tour = tourIter.next();

                for (int i = 0; i < tour.getActivityCount() - 1; ++i) {
                    Activity activity = tour.getActivity(i);

                    // destinations are not stops
                    if (activity == tour.getDestination()) {
                        continue;
                    }

                    String purpose = activity.getAttributeAsString("orig_purp");

                    if (purpose.equalsIgnoreCase("W")) {
                        w_acts += 1;
                    } else if (purpose.equals("C")) {
                        c_acts += 1;
                    } else if (purpose.equals("S")) {
                        s_acts += 1;
                    } else if (purpose.equals("R")) {
                        r_acts += 1;
                    } else if (purpose.equals("O")) {
                        o_acts += 1;
                    } else if (purpose.equals("B")) {
                        b_acts += 1;
                    }
                }
            }
            outStream.print("," + w_acts);
            outStream.print("," + c_acts);
            outStream.print("," + s_acts);
            outStream.print("," + r_acts);
            outStream.print("," + o_acts);
            // outStream.print("," + b_acts);

            outStream.print(","
                    + (member.getTourCount("W") + member.getTourCount("w")));
            outStream.print("," + member.getTourCount("C"));
            outStream.print("," + member.getTourCount("S"));
            outStream.print("," + member.getTourCount("R"));
            outStream.print("," + member.getTourCount("O"));
            outStream.print("," + member.getTourCount("B"));
            outStream.print(","
                    + (member.getTour(0).getActivityCount() < 3 ? 0 : member
                            .getTour(0).getActivityCount() - 3));
            if (member.getTourCount() > 1) {
                outStream.print(","
                        + (member.getTour(1).getActivityCount() - 3));
            } else {
                outStream.print(",0");
            }
            if (member.getTourCount() > 2) {
                outStream.print(","
                        + (member.getTour(2).getActivityCount() - 3));
            } else {
                outStream.print(",0");
            }

            int istops = 0;
            for (int i = 3; i < member.getTourCount(); ++i) {
                istops += member.getTour(i).getActivityCount();
            }
            outStream.print("," + istops);

            int w_tours = 0;
            int nw_tours = 0;
            tourIter = member.getTourIterator();
            while (tourIter.hasNext()) {
                Tour tour = tourIter.next();

                if (tour.getPurpose().equalsIgnoreCase("W")) {
                    w_tours += tour.getActivityCount() > 3 ? tour
                            .getActivityCount() - 3 : 0;
                } else {
                    nw_tours += tour.getActivityCount() > 3 ? tour
                            .getActivityCount() - 3 : 0;
                }
            }

            outStream.print("," + w_tours);
            outStream.print("," + nw_tours);

            int w_stops = 0;
            int c_stops = 0;
            int s_stops = 0;
            int r_stops = 0;
            int o_stops = 0;
            // int b_stops = 0;
            tourIter = member.getTourIterator();
            while (tourIter.hasNext()) {
                Tour tour = tourIter.next();

                for (int i = 1; i < tour.getActivityCount() - 1; ++i) {
                    Activity activity = tour.getActivity(i);

                    // destinations are not stops
                    if (activity == tour.getDestination()) {
                        continue;
                    }

                    String purpose = activity.getPurpose();

                    if (purpose.equalsIgnoreCase("W")) {
                        w_stops += 1;
                    } else if (purpose.equals("C")) {
                        c_stops += 1;
                    } else if (purpose.equals("S")) {
                        s_stops += 1;
                    } else if (purpose.equals("R")) {
                        r_stops += 1;
                    } else if (purpose.equals("O")) {
                        o_stops += 1;
                        // } else if (purpose.equals("B")) {
                        // b_stops += 1;
                    }
                }
            }

            outStream.print("," + w_stops);
            outStream.print("," + c_stops);
            outStream.print("," + s_stops);
            outStream.print("," + r_stops);
            outStream.print("," + o_stops);
            // outStream.print("," + b_stops);

            int i = 0;
            for (; i < member.getTourCount() && i < 4; ++i) {
                outStream.print("," + member.getTour(i).getPurpose());
            }
            for (; i < 4; ++i) {
                outStream.print(",n");
            }

            outStream.print("," + member.getAttributeAsInt("occup"));
            outStream.print("," + member.getAttributeAsInt("industry"));

            outStream.print("," + member.getAttributeAsInt("age"));
            outStream.print("," + household.getStudentCount());
            outStream.print("," + household.getWorkerCount());

            outStream.print("," + household.getHome().getTaz());
            Location work = sdp.findWorkLocation(member);
            if (work == null) {
                outStream.print(",-1,-1");
            } else {
                outStream.print("," + work.getTaz());
                outStream.print("," + home.distance(work));
            }

            outStream.println();
        }

        logger.info("Finished writing " + fileName + ".");
    }

    /**
     * Usage: java com.pb.models.pt.sdt.survey.TourPatternFile <household file>
     * <person file> <activity file> <outfile>
     */
    public static void main(String[] args) {
        ActivityPatternFile apf = new ActivityPatternFile(args[0], args[1],
                args[2]);
        apf.writeActivityPatterns(args[3]);
    }

}
