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
import com.pb.models.pt.surveydata.Location;
import com.pb.models.pt.surveydata.Tour;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * This generates tours using methods in the ShortDistanceProcessor.
 * 
 * @author Stryker
 * 
 */
public class TourPatternFile {
    protected Logger logger = Logger.getLogger(TourPatternFile.class);

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
    public TourPatternFile(String hhFileName, String perFileName,
            String actFileName) {
        sdp = new ShortDistanceProcessor();
        sds = new ShortDistanceSurvey(hhFileName, perFileName, actFileName);
    }

    /**
     * Write tours with attributes to file.
     */
    public void writeTours(String fileName) {
        PrintWriter outStream = null;
        int activitiesToPrint = 8;

        logger.info("Writing tours to " + fileName);

        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(
                    fileName)));
        } catch (IOException e) {
            logger.fatal("Could not write to " + fileName);
            e.printStackTrace();
            System.exit(1);
        }

        // header
        outStream.print("survey, hh_id, per_id, income, hhsize, hhsize5, "
                + "hhsize15, vehicles, worker, student, tour, "
                + "type, mode, purpose, pattern, destination, anchor, "
                + "activites, outbound, inbound, departure, arrival, "
                + "tour_dist, g_pattern, g_outbound, g_inbound, g_dist, "
                + "xy_dist, inbnd_i, inbnd_j, inbnd_k, outbnd_i, outbnd_j,"
                + "outbnd_k, hexpfact");

        for (int a = 0; a < activitiesToPrint; ++a) {
            outStream.print(", purpose" + a);
            outStream.print(", taz" + a);
            outStream.print(", type" + a);
            outStream.print(", mode" + a);
            outStream.print(", arrival" + a);
            outStream.print(", departure" + a);
        }

        outStream.print("\n");

        Iterator<Household> householdIter = sds.getHouseholdIterator();
        while (householdIter.hasNext()) {
            Household household = householdIter.next();
            long hh = household.getHousehold();
            Location home = household.getHome();

            sdp.codeHouseholdComposition(household);

            Iterator<HouseholdMember> memberIter = household
                    .getHouseholdMemberIterator();
            while (memberIter.hasNext()) {
                HouseholdMember member = memberIter.next();
                int hm = member.getMember();
                
                if (hh == 21048881) {
                    logger.info("Debug");
                }

                sdp.linkModes(member);
                sdp.clean(member);
                sdp.linkNonTrips(member);
                sdp.formHomeBasedTours(member, home);
                sdp.formWorkSubTours(member);
                sdp.generalizeMemberTours(member, home);

                Iterator<Tour> tourIter = member.getTourIterator();
                while (tourIter.hasNext()) {
                    Tour tour = tourIter.next();

                    sdp.codeTripMode(tour);
                    sdp.codeTourMode(tour);

                    outStream.print(household.getAttributeAsInt("survey"));
                    outStream.print("," + hh + "," + hm);
                    outStream.print("," + household.getIncome());
                    outStream.print("," + household.getMemberCount());
                    outStream
                            .print("," + household.getAttributeAsInt("under5"));
                    outStream.print(","
                            + household.getAttributeAsInt("under15"));
                    outStream.print("," + household.getVehicles());
                    outStream.print("," + member.isWorker());
                    outStream.print("," + member.isStudent());
                    outStream.print("," + tour.getTour());
                    outStream.print("," + tour.getType());
                    outStream.print("," + tour.getMode());
                    outStream.print("," + tour.getPurpose());
                    outStream.print("," + tour.getPattern());
                    outStream.print("," + tour.getDestination().getTaz());
                    outStream.print("," + tour.getAnchor().getTaz());
                    outStream.print("," + tour.getActivityCount());
                    outStream.print("," + tour.getOutboundStopCount());
                    outStream.print("," + tour.getInboundStopCount());
                    outStream.print("," + tour.getDepartureMinute());
                    outStream.print("," + tour.getArrivalMinute());
                    outStream.print("," + tour.getDistance());

                    // generalize the tour
                    sdp.generalizeTour(member, tour, home);

                    outStream.print("," + tour.getPattern());
                    outStream.print("," + tour.getOutboundStopCount());
                    outStream.print("," + tour.getInboundStopCount());
                    outStream.print("," + tour.getDistance());

                    outStream.print(","
                            + tour.getAnchor().distance(tour.getDestination()));

                    // report some generalization measures
                    outStream.print("," + tour.getAttributeAsInt("inbnd_i"));
                    outStream.print("," + tour.getAttributeAsInt("inbnd_j"));
                    outStream.print("," + tour.getAttributeAsInt("inbnd_k"));
                    outStream.print("," + tour.getAttributeAsInt("outbnd_i"));
                    outStream.print("," + tour.getAttributeAsInt("outbnd_j"));
                    outStream.print("," + tour.getAttributeAsInt("outbnd_k"));

                    outStream.print("," + household.getWeight());

                    int a;
                    for (a = 0; a < tour.getActivityCount()
                            && a < activitiesToPrint; ++a) {
                        Activity activity = tour.getActivity(a);
                        outStream.print("," + activity.getPurpose());
                        outStream.print("," + activity.getTaz());
                        outStream.print("," + activity.getType());
                        outStream.print("," + activity.getMode());
                        outStream.print("," + activity.getArrivalMinute());
                        outStream.print("," + activity.getDepartureMinute());
                    }

                    for (; a < activitiesToPrint; ++a) {
                        outStream.print(",-9");
                        outStream.print(",-99999");
                        outStream.print(",-99999");
                        outStream.print(",-99999");
                        outStream.print(",-99999");
                        outStream.print(",-99999");
                    }

                    outStream.print("\n");
                }
            }
        }
        logger.info("Finished writing " + fileName + ".");
    }

    /**
     * Usage: java com.pb.models.pt.sdt.survey.TourPatternFile <household file>
     * <person file> <activity file> <outfile>
     */
    public static void main(String[] args) {
        TourPatternFile tpf = new TourPatternFile(args[0], args[1], args[2]);
        tpf.writeTours(args[3]);
    }

}