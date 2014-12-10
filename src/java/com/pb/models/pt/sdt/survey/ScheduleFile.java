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
 * Created on Dec 1, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.sdt.survey;

import com.pb.models.pt.Scheduler;
import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.Household;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.Location;
import com.pb.models.pt.surveydata.Tour;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * Time-of-day estimation file writer.
 * 
 * Note about overnight tours.
 * 
 * For scheduling tours, I am the following this assumption: If a tour arrives
 * home between the beginning of the survey day (3:00 am) and the first hour
 * period (5:00 am), then the departure period is 5. If not, the tour departure
 * period is either period 23 or earlier and the arrival period is 23.
 * 
 * So a tour that starts at 22:00 and ends at 6:00 will look like it started in
 * period 5 and ended in period 6. Tours that begin at 1:00 am and end at 2:00
 * am look like period 23 to period 23.
 * 
 * @author Stryker
 * 
 */
public class ScheduleFile {
    protected static Logger logger = Logger.getLogger(ScheduleFile.class);

    public final static int FIRST_HOUR = 5;

    public final static int LAST_HOUR = 23;

    protected ShortDistanceProcessor sdp;

    protected ShortDistanceSurvey sds;

    /**
     * Constructor.
     */
    public ScheduleFile(String hhFileName, String perFileName,
            String actFileName) {
        sdp = new ShortDistanceProcessor();
        sds = new ShortDistanceSurvey(hhFileName, perFileName, actFileName);
    }

    /**
     * Convert a time to period.
     * 
     * Look at the note above.
     * 
     * @param tour
     */
    protected int departurePeriod(Tour tour) {
        int period;
        int departure = tour.getDepartureMinute() / 60;
        int arrival = tour.getArrivalMinute() / 60;

        if (arrival < departure) {
            if (departure < FIRST_HOUR) {
                period = 0;
            } else if (departure > LAST_HOUR) {
                period = LAST_HOUR - FIRST_HOUR;
            } else {
                period = departure - FIRST_HOUR;
            }
        } else if (arrival >= ShortDistanceProcessor.SURVEY_START_HOUR
                && departure < FIRST_HOUR) {
            period = 0;
        } else if (departure < ShortDistanceProcessor.SURVEY_START_HOUR
                || departure > LAST_HOUR) {
            period = LAST_HOUR - FIRST_HOUR;
        } else {
            period = departure - FIRST_HOUR;
        }

        return period;
    }

    /**
     * Convert a time to period.
     * 
     * Look at the note above.
     * 
     * @param tour
     */
    protected int arrivalPeriod(Tour tour) {
        int period;
        int arrival = tour.getArrivalMinute() / 60;
        int departure = tour.getDepartureMinute() / 60;

        if (departure > arrival
                || arrival < ShortDistanceProcessor.SURVEY_START_HOUR
                || arrival > LAST_HOUR) {
            period = LAST_HOUR - FIRST_HOUR;
        } else if (arrival >= ShortDistanceProcessor.SURVEY_START_HOUR
                && arrival <= FIRST_HOUR) {
            period = 0;
        } else {
            period = arrival - FIRST_HOUR;
        }

        return period;
    }

    /**
     * Write each tour and its availability.
     * 
     * Loop through each member and that members's day pattern.
     */
    public void write(String fileName) {
        PrintWriter outStream = null;

        Scheduler scheduler = new Scheduler(LAST_HOUR - FIRST_HOUR + 1);

        logger.info("Writing schedule to " + fileName);

        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(
                    fileName)));
        } catch (IOException e) {
            logger.fatal("Could not write to " + fileName);
            e.printStackTrace();
            System.exit(1);
        }

        // header
        outStream.print("survey, hh_id, per_id, tour_id, priority, income, "
                + "hhsize, hhsize5, hhsize15, vehicles, worker, student, "
                + "type, mode, purpose, pattern, destination, anchor, "
                + "activites, outbound, inbound, departure, dperiod, arrival, "
                + "aperiod, window, tour_dist, xy_dist, hexpfact");

        // availability section
        for (int i = FIRST_HOUR; i <= LAST_HOUR; ++i) {
            for (int j = i; j <= LAST_HOUR; ++j) {
                outStream.print(",aval" + (i * 100 + j));
            }
        }

        outStream.print("\n");

        Iterator<Household> householdIter = sds.getHouseholdIterator();
        while (householdIter.hasNext()) {
            Household household = householdIter.next();
            long hh = household.getHousehold();
            Location home = household.getHome();

            sdp.codeHouseholdComposition(household);

            for (HouseholdMember member : household.getMembers().values()) {
                int hm = member.getMember();

                boolean bad = false;

                if (logger.getLevel() == Level.DEBUG) {
                    logger.debug("Writing tour schedule for " + hh + "," + hm);
                }

                scheduler.clear();

                sdp.linkModes(member);
                sdp.clean(member);
                sdp.linkNonTrips(member);
                sdp.formHomeBasedTours(member, home);
                sdp.formWorkSubTours(member);
                sdp.generalizeMemberTours(member, home);
                sdp.prioritizeTours(member);

                for (Tour tour : member.getTours()) {
                    if (bad) {
                        logger.info("Skipping because because of time issues: "
                                + hh + ", " + hm + "," + tour.getTour());
                        continue;
                    }

                    if (tour.getType() != ShortDistanceProcessor.HOME_BASED) {
                        logger.info("Skipping non-home based tour.");
                        continue;
                    }

                    int start = departurePeriod(tour);
                    int end = arrivalPeriod(tour);

                    if (!scheduler.isWindowAvailable(start, end)) {
                        logger.warn("Unable to schedule: " + hh + "," + hm);
                        bad = true;
                        continue;
                    }

                    // update the

                    sdp.codeTourMode(tour);

                    outStream.print(household.getAttributeAsInt("survey"));
                    outStream.print("," + hh + "," + hm);
                    outStream.print("," + tour.getTour());
                    outStream.print("," + tour.getPriority());
                    outStream.print("," + household.getIncome());
                    outStream.print("," + household.getMemberCount());
                    outStream
                            .print("," + household.getAttributeAsInt("under5"));
                    outStream.print(","
                            + household.getAttributeAsInt("under15"));
                    outStream.print("," + household.getVehicles());
                    outStream.print("," + member.isWorker());
                    outStream.print("," + member.isStudent());
                    outStream.print("," + tour.getType());
                    outStream.print("," + tour.getMode());
                    outStream.print("," + tour.getPurpose());
                    outStream.print("," + tour.getPattern());
                    Activity destination = tour.getDestination();
                    outStream.print("," + destination.getTaz());
                    Activity anchor = tour.getAnchor();
                    outStream.print("," + anchor.getTaz());
                    outStream.print("," + tour.getActivityCount());
                    outStream.print("," + tour.getOutboundStopCount());
                    outStream.print("," + tour.getInboundStopCount());

                    outStream.print("," + tour.getDepartureMinute());
                    outStream.print("," + (start + FIRST_HOUR));
                    outStream.print("," + tour.getArrivalMinute());
                    outStream.print("," + (end + FIRST_HOUR));
                    outStream
                            .print(","
                                    + ((start + FIRST_HOUR) * 100 + (end + FIRST_HOUR)));

                    outStream.print("," + tour.getDistance());
                    outStream.print("," + anchor.distance(destination));
                    outStream.print("," + household.getWeight());

                    // availability section
                    int priority = tour.getPriority();
                    scheduler.scheduleEvent(start, end);
                    
                    logger.debug("Tour priority is " + priority);
                    for (int i = 0; i <= LAST_HOUR - FIRST_HOUR; ++i) {
                        for (int j = i; j <= LAST_HOUR - FIRST_HOUR; ++j) {

                            if (scheduler.isWindowAvailable(scheduler
                                    .getEventCount() - 1, i, j)) {
                                outStream.print(",1");
                            } else {
                                outStream.print(",0");
                            }
                        }
                    }
                    outStream.print("\n");

                }
            }
        }

        logger.info("Finished writing " + fileName + ".");
        outStream.close();
    }

    /**
     * Write tour availability file.
     * 
     * Usage: java com.pb.models.pt.sdt.survey.ScheduleFile <household file>
     * <person file> <activity file> <output file>
     */
    public static void main(String[] args) {
        ScheduleFile sf = new ScheduleFile(args[0], args[1], args[2]);
        ScheduleFile.logger.info(sf);
        sf.write(args[3]);
    }

}
