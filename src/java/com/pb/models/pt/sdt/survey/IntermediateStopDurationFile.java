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
 * Created on Dec 30, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.sdt.survey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.pb.common.matrix.BinaryMatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.model.ModelException;
import com.pb.models.pt.surveydata.Activity;
import com.pb.models.pt.surveydata.Household;
import com.pb.models.pt.surveydata.HouseholdMember;
import com.pb.models.pt.surveydata.Location;
import com.pb.models.pt.surveydata.Tour;
import com.pb.models.pt.Scheduler;

/**
 * Write the intermediate stop duration estimation file.
 * 
 * The duration model, like the tour scheduling model, uses the MORPC windowing
 * framework. In application, the tour schedule and the stop locations are
 * known. Therefore, the trip times are taken into account when constructing the
 * availability windows.
 * 
 * @author Stryker
 */
public class IntermediateStopDurationFile extends ScheduleFile {
    Logger logger = Logger.getLogger(IntermediateStopDurationFile.class);

    private Matrix ivtt;

    HashMap<Long, Integer> correspondence;

    /**
     * Constructor.
     */
    IntermediateStopDurationFile(String hhFileName, String perFileName,
            String actFileName) {
        super(hhFileName, perFileName, actFileName);
    }

    /**
     * Read the zonal file and store the correspondence relation.
     * 
     * @param fileName
     */
    public void readZonalFile(String fileName) {
        logger.info("Reading zonal file " + fileName);

        BufferedReader in;
        correspondence = new HashMap<Long, Integer>();

        try {
            in = new BufferedReader(new FileReader(new File(fileName)));

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

                int taz = new Integer(flds[positions.get("TAZ")]);
                long swtaz = new Long(flds[positions.get("TAZName")]);

                correspondence.put(swtaz, taz);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        logger.info("Finished reading zonal file.");
    }

    /**
     * Read and store the IVTT.
     * 
     * @param fileName
     */
    public void readTravelTimes(String fileName) {
        MatrixReader mr = new BinaryMatrixReader(new File(fileName));

        logger.info("Reading travel times from " + fileName);
        ivtt = mr.readMatrix();
        logger.info("Finished reading travel times.");
    }

    /**
     * Get the period offset for before the outbound intermediate stop.
     * 
     * @param tour
     * @return
     */
    private int afterInboundPeriodOffset(Tour tour) {
        int itaz = convertTaz(tour.getAnchor().getTaz());
        int ktaz = convertTaz(tour.getActivity(tour.getActivityCount() - 2)
                .getTaz());

        int minutes = (int) ivtt.getValueAt(itaz, ktaz);

        return minutes / 60;
    }

    /**
     * Get the period offset for before the outbound intermediate stop.
     * 
     * @param tour
     * @return
     */
    private int beforeOutboundPeriodOffset(Tour tour) {
        int itaz = convertTaz(tour.getAnchor().getTaz());
        int ktaz = convertTaz(tour.getActivity(1).getTaz());

        int minutes = (int) ivtt.getValueAt(itaz, ktaz);

        return minutes / 60;
    }

    /**
     * Get the period offset for before the outbound intermediate stop.
     * 
     * @param tour
     * @return
     */
    private int beforeInboundPeriodOffset(Tour tour) {
        int itaz = convertTaz(tour.getAnchor().getTaz());
        int k2taz = convertTaz(tour.getActivity(tour.getActivityCount() - 2)
                .getTaz());
        int jtaz = convertTaz(tour.getActivity(1).getTaz());

        int minutes = (int) (ivtt.getValueAt(itaz, k2taz) + ivtt.getValueAt(
                k2taz, jtaz));

        if (tour.getInboundStopCount() > 1) {
            Activity k1 = tour.getActivity(1);
            int k1taz = (int) k1.getTaz();

            minutes += (int) (ivtt.getValueAt(jtaz, k1taz) + ivtt.getValueAt(
                    k1taz, itaz))
                    + k1.duration();
        } else {
            minutes += (int) ivtt.getValueAt(jtaz, itaz);
        }

        return minutes / 60;
    }

    /**
     * Get the period offset for before the outbound intermediate stop.
     * 
     * @param tour
     * @return
     */
    private int afterOutboundPeriodOffset(Tour tour) {
        int itaz = convertTaz(tour.getAnchor().getTaz());
        int k1taz = convertTaz(tour.getActivity(1).getTaz());
        int jtaz = convertTaz(tour.getActivity(1).getTaz());

        int minutes = (int) (ivtt.getValueAt(itaz, k1taz) + ivtt.getValueAt(
                k1taz, jtaz));

        if (tour.getInboundStopCount() > 1) {
            int k2taz = convertTaz(tour.getActivity(3).getTaz());

            minutes += (int) (ivtt.getValueAt(jtaz, k2taz) + ivtt.getValueAt(
                    k2taz, itaz));
        } else {
            minutes += (int) ivtt.getValueAt(jtaz, itaz);
        }

        return minutes / 60;
    }

    /**
     * Convert a time to period.
     * 
     * Look at the note above.
     * 
     * @param activity
     */
    protected int departurePeriod(Activity activity) {
        int departure = activity.getDepartureMinute() / 60;
        // int arrival = activity.getArrivalMinute() / 60;

        /*
         * case 1: generalized last period; case 2: late night activity before
         * start of tour day
         */
        if (departure > LAST_HOUR
                || departure < ShortDistanceProcessor.SURVEY_START_HOUR) {
            return LAST_HOUR - FIRST_HOUR;
        }

        /* case 3: generalized early period */
        if (departure < FIRST_HOUR) {
            return 0;
        }

        return departure - FIRST_HOUR;
    }

    /**
     * Convert a time to period.
     * 
     * Look at the note above.
     * 
     * @param activity
     */
    protected int arrivalPeriod(Activity activity) {
        int arrival = activity.getArrivalMinute() / 60;
        int departure = activity.getDepartureMinute() / 60;

        /*
         * case 1: activity departs after survey day start and activity begins
         * before survey start
         */
        if (departure > ShortDistanceProcessor.SURVEY_START_HOUR) {
            if (arrival < FIRST_HOUR || arrival > departure) {
                return 0;
            }
        } else {
            if (arrival > LAST_HOUR || arrival < FIRST_HOUR) {
                return LAST_HOUR - FIRST_HOUR;
            }
        }

        return arrival - FIRST_HOUR;
    }

    private int convertTaz(long taz) {
        return correspondence.get(taz);
    }

    private int window(int a, int d) {
        int win = 0;

        for (int i = 0; i < LAST_HOUR - FIRST_HOUR + 1; ++i) {
            for (int j = i; j < LAST_HOUR - FIRST_HOUR + 1; ++j) {
                win += 1;
                if (i == a && j == d) {
                    return win;
                }
            }
        }

        // should not be reachable
        return 0;
    }

    /**
     * Write the intermediate stop duration estimation file
     * 
     * @param fileName
     *            Name of the output file.
     */
    public void write(String fileName) {
        PrintWriter outStream = null;

        Scheduler scheduler = new Scheduler(LAST_HOUR - FIRST_HOUR + 1);

        logger.info("Writing intermediate stop duration file to " + fileName);

        try {
            outStream = new PrintWriter(new BufferedWriter(new FileWriter(
                    fileName)));
        } catch (IOException e) {
            logger.fatal("Could not write to " + fileName);
            e.printStackTrace();
            System.exit(1);
        }

        // header
        outStream.print("hh_id,per_id,tour_id,stop,stop_type,stop_purp,aperiod"
                + ",dperiod,dev_dist,choosen");

        // availability section
        for (int i = FIRST_HOUR; i <= LAST_HOUR; ++i) {
            for (int j = i; j <= LAST_HOUR; ++j) {
                outStream.print(",aval" + (i * 100 + j));
            }
        }
        outStream.print("\n");

        for (Household household : sds.getHouseholds().values()) {
            long hh = household.getHousehold();
            Location home = household.getHome();

            sdp.codeHouseholdComposition(household);

            for (HouseholdMember member : household.getMembers().values()) {
                int hm = member.getMember();

                boolean bad = false;

                if (logger.getLevel() == Level.DEBUG) {
                    logger.debug("Writing stop duration schedule for " + hh
                            + "," + hm);
                }

                sdp.linkModes(member);
                sdp.clean(member);
                sdp.linkNonTrips(member);
                sdp.formHomeBasedTours(member, home);
                sdp.formWorkSubTours(member);
                sdp.generalizeMemberTours(member, home);

                for (Tour tour : member.getTours()) {
                    // skip tours without stops and tours that are not home
                    // based
                    if (bad
                            || tour.getActivityCount() < 4
                            || tour.getType() != ShortDistanceProcessor.HOME_BASED) {
                        continue;
                    }

                    int stops = 0;

                    Activity destination = tour.getDestination();

                    scheduler.clear();

                    boolean outbound = tour.getOutboundStopCount() > 0;
                    boolean inbound = tour.getInboundStopCount() > 0;

                    // set-up scheduler -- create 'fake' events before and after
                    // the tour
                    int departure = departurePeriod(tour);
                    int arrival = arrivalPeriod(tour);
                    scheduler.scheduleEvent(0, departure);
                    scheduler
                            .scheduleEvent(arrival, scheduler.getPeriods() - 1);

                    if (outbound) {
                        int before;
                        int after;
                        Activity stop = tour.getActivity(1);
                        try {
                            before = beforeOutboundPeriodOffset(tour);
                            after = afterOutboundPeriodOffset(tour);
                        } catch (NullPointerException e) {
                            logger.info("Skipping stop due to lack of taz.");
                            bad = true;
                            continue;
                        }

                        int aperiod = arrivalPeriod(stop);
                        int dperiod = departurePeriod(stop);

                        // schedule the before and after time
                        try {
                            if (departure + before > LAST_HOUR - FIRST_HOUR
                                    || arrival - after < 0) {
                                throw new ModelException();
                            }

                            scheduler.scheduleEvent(departure, departure
                                    + before);
                            scheduler.scheduleEvent(arrival - after, arrival);

                            // we are scheduling the event and then checking to
                            // see
                            // what windows are available for that event that
                            // also
                            // preserve order
                            scheduler.scheduleEvent(aperiod, dperiod);
                        } catch (ModelException e) {
                            logger.warn("Unable to schedule: " + hh + "," + hm);
                            bad = true;
                            continue;
                        }
                        // outStream.print(household.getAttributeAsInt("survey")
                        // + ",");
                        outStream.print(hh + "," + hm + "," + tour.getTour()
                                + "," + stops + ",1");
                        outStream.print("," + stop.getPurpose());
                        outStream.print("," + aperiod);
                        outStream.print("," + dperiod);
                        outStream.print(","
                                + stop.deviationDistance(home, destination));
                        outStream.print("," + window(aperiod, dperiod));

                        stops += 1;

                        // availability section
                        for (int i = 0; i <= LAST_HOUR - FIRST_HOUR; ++i) {
                            for (int j = i; j <= LAST_HOUR - FIRST_HOUR; ++j) {

                                if (i == aperiod
                                        && scheduler
                                                .isWindowAvailable(scheduler
                                                        .getEventCount() - 1, i, j)) {
                                    outStream.print(",1");
                                } else {
                                    outStream.print(",0");
                                }
                            }
                        }

                        outStream.print("\n");

                        // reset
                        if (inbound) {
                            scheduler.clear();
                            scheduler.scheduleEvent(0, departure);
                            scheduler.scheduleEvent(arrival, scheduler
                                    .getPeriods() - 1);
                        }

                    }

                    if (inbound) {
                        int before;
                        int after;
                        Activity stop = tour.getActivity(tour
                                .getActivityCount() - 2);
                        try {
                            before = beforeInboundPeriodOffset(tour);
                            after = afterInboundPeriodOffset(tour);
                        } catch (NullPointerException e) {
                            logger.info("Skipping stop do to lack of taz.");
                            bad = true;
                            continue;
                        }

                        int aperiod = arrivalPeriod(stop);
                        int dperiod = departurePeriod(stop);

                        // schedule the before and after time
                        try {
                            if (departure + before > LAST_HOUR - FIRST_HOUR
                                    || arrival - after < 0) {
                                throw new ModelException();
                            }
                            scheduler.scheduleEvent(departure, departure
                                    + before);
                            scheduler.scheduleEvent(arrival - after, arrival);
                            scheduler.scheduleEvent(aperiod, dperiod);
                        } catch (ModelException e) {
                            logger.warn("Unable to schedule: " + hh + "," + hm);
                            bad = true;
                            continue;
                        }

                        outStream.print(hh + "," + hm + "," + tour.getTour()
                                + "," + stops + ",2");
                        outStream.print("," + stop.getPurpose());
                        outStream.print("," + aperiod);
                        outStream.print("," + dperiod);
                        outStream.print(","
                                + stop.deviationDistance(home, destination));
                        outStream.print("," + window(aperiod, dperiod));

                        // availability section
                        for (int i = 0; i <= LAST_HOUR - FIRST_HOUR; ++i) {
                            for (int j = i; j <= LAST_HOUR - FIRST_HOUR; ++j) {

                                if (j == dperiod
                                        && scheduler
                                                .isWindowAvailable(scheduler
                                                        .getEventCount() - 1, i, j)) {
                                    outStream.print(",1");
                                } else {
                                    outStream.print(",0");
                                }
                            }
                        }
                        outStream.print("\n");
                        outStream.flush();
                    }

                }
            }
        }
        logger.info("Finished writing " + fileName);
    }

    /**
     * Write intermediate stop availability file.
     * 
     * Usage: java com.pb.models.pt.sdt.survey.ScheduleFile <household file>
     * <person file> <activity file> <matrix> <taz file> <output file>
     * 
     * @param args
     */
    public static void main(String[] args) {
        IntermediateStopDurationFile isdf = new IntermediateStopDurationFile(
                args[0], args[1], args[2]);
        ScheduleFile.logger.info(isdf);
        isdf.readTravelTimes(args[3]);
        isdf.readZonalFile(args[4]);
        isdf.write(args[5]);
    }

}
