/*
 * Copyright 2006 PB Consult Inc.
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
 */
package com.pb.models.pt.ldt;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * Used for LDT Calibration to report LDT statistics.
 *
 * @author Erhardt
 * @version 1.0 Oct 27, 2006
 *
 */
public class LDTReporter {

    protected static Logger logger = Logger.getLogger(LDTReporter.class);

    // by purpose, for persons
    private int[] binaryChoiceTrue;
    private int[] binaryChoiceFalse;
    private int binaryChoiceTrueTotal;
    private int binaryChoiceFalseTotal;

    // by purpose and pattern type
    private int[][] personTours;

    // tours and trips by purpose
    private int[] tourCount;
    private int[] tripCount;

    // one for each hour
    private int[]   beginTourDeparture;
    private int[]   endTourArrival;
    private int[][] completeTourSchedule;

    // by purpose and pattern type
    private int[][] internalDest;
    private int[][] externalDest;

    // by purpose, and number of bins
    private int[][] internalTLFD;
    private int[][] externalTLFD;
    private int[] internalTLFDbins;
    private int[] externalTLFDbins;

    // by purpose and pattern type
    private float[][] internalAggregateDistance;
    private float[][] externalAggregateDistance;
    private float[][] internalAggregateTrips;
    private float[][] externalAggregateTrips;

    // by purpose and mode
    private int[][] internalModeChoice;
    private int[][] externalModeChoice;

    // by purpose and destination type
    private int[][] autoPersonTrips;
    private int[][] autoVehicleTrips;
    
    // trips over 100 miles for comparison to American Travel Survey
    // segmented by mode
    private int[] internalAtsTrips; 
    private int[] externalAtsTrips; 
    
    // person trips by trip mode--trip mode by internal/external
    private int[][] personTripsByTripMode; 
    
    // person trips by party size--segmented by purpose
    private int[][] personTripsByPartySize; 
    
    // VMT by internal/external
    private float[] vmt; 


    /**
     * Constructor initializes counts to zero
     *
     */
    public LDTReporter(ResourceBundle rb){

        // initialize the binary choice counts to zero
        binaryChoiceTrue = new int[LDTourPurpose.values().length];
        binaryChoiceFalse = new int[LDTourPurpose.values().length];
        binaryChoiceTrueTotal = 0;
        binaryChoiceFalseTotal = 0;

        // initialize the person tour counts to zero
        personTours = new int[LDTourPurpose.values().length][LDTourPatternType.values().length];

        // initiazlize tour and trip counts to zero
        tourCount = new int[LDTourPurpose.values().length];
        tripCount = new int[LDTourPurpose.values().length];

        // initialize the schedule counts to zero
        beginTourDeparture   = new int[24];
        endTourArrival       = new int[24];
        completeTourSchedule = new int[24][24];

        // initialize internal external counts
        internalDest = new int[LDTourPurpose.values().length][LDTourPatternType.values().length];
        externalDest = new int[LDTourPurpose.values().length][LDTourPatternType.values().length];

        // initialize trip length frequency distributions
        int internalTLFDbinSize = ResourceUtil.getIntegerProperty(rb, "ldt.report.internalTLFDbinSize");
        int externalTLFDbinSize = ResourceUtil.getIntegerProperty(rb, "ldt.report.externalTLFDbinSize");
        int internalTLFDnumBins = ResourceUtil.getIntegerProperty(rb, "ldt.report.internalTLFDnumBins");
        int externalTLFDnumBins = ResourceUtil.getIntegerProperty(rb, "ldt.report.externalTLFDnumBins");

        internalTLFD = new int[LDTourPurpose.values().length][internalTLFDnumBins];
        externalTLFD = new int[LDTourPurpose.values().length][externalTLFDnumBins];

        internalTLFDbins = new int[internalTLFDnumBins];
        for (int i=0; i<internalTLFDnumBins; i++) {
            internalTLFDbins[i] = (i+1) * internalTLFDbinSize;
        }

        externalTLFDbins = new int[externalTLFDnumBins];
        for (int i=0; i<externalTLFDnumBins; i++) {
            externalTLFDbins[i] = (i+1) * externalTLFDbinSize;
        }

        // initialize average trip lengths
        internalAggregateDistance = new float[LDTourPurpose.values().length][LDTourPatternType.values().length];
        externalAggregateDistance = new float[LDTourPurpose.values().length][LDTourPatternType.values().length];
        internalAggregateTrips    = new float[LDTourPurpose.values().length][LDTourPatternType.values().length];
        externalAggregateTrips    = new float[LDTourPurpose.values().length][LDTourPatternType.values().length];

        // initialize the mode choice trip counts
        internalModeChoice = new int[LDTourPurpose.values().length][LDTourModeType.values().length];
        externalModeChoice = new int[LDTourPurpose.values().length][LDTourModeType.values().length];

        // initialize average auto occupancy counts by purpose and destination type
        autoPersonTrips = new int[LDTourPurpose.values().length][LDTourDestinationType.values().length];
        autoVehicleTrips = new int[LDTourPurpose.values().length][LDTourDestinationType.values().length];
        
        // initialize trips over 100 miles for comparison to American Travel Survey
        // segmented by mode
        internalAtsTrips = new int[LDTourModeType.values().length]; 
        externalAtsTrips = new int[LDTourModeType.values().length]; 
        

        // person trips by trip mode--trip mode by internal/external
        personTripsByTripMode = new int[LDTripModeType.values().length][LDTourDestinationType.values().length]; 
        // person trips by party size--segmented by purpose
        personTripsByPartySize = new int[LDTourPurpose.values().length][3]; 
        
        // VMT by internal/external
        vmt = new float[LDTourDestinationType.values().length]; 
    }


    /**
     * Adds up the binary choice of travel and tours by pattern type.
     *
     * @param households Array of households for which to tabulate statistics.
     */
    public void countHouseholdLevelDecisions(PTHousehold[] households) {

        for (PTHousehold hh : households) {
            for (PTPerson person : hh.persons) {

                boolean anyTrue = false;

                for (int i=0; i<LDTourPurpose.values().length; i++) {
                    if (person.ldTourIndicator[i]==true) {
                        binaryChoiceTrue[i]++;
                        personTours[i][person.ldTourPattern[i].ordinal()]++;
                        anyTrue = true;
                    } else {
                        binaryChoiceFalse[i]++;
                    }
                }
                if (anyTrue) binaryChoiceTrueTotal++;
                else binaryChoiceFalseTotal++;
            }
        }
    }

    /**
     * Adds up the LDT results at a tour level.
     *
     * @param tours Array of LD tours.
     */
    public void countTourLevelDecisions(LDTour[] tours, LDTrip[] trips) {
        countTours(tours);
        countSchedules(tours);
        countInternalExternal(tours);

        countTrips(trips);
        countTripLengthDistribution(trips);
        countAggregateDistance(trips);

        countModeChoice(tours);
        countAutoOccupancy(trips);
        
        countPersonTrips(trips); 
        countVmt(trips); 
    }

    /**
     * Count the number of tours.
     *
     * @param tours The array of tours.
     */
    private void countTours(LDTour[] tours) {
        for (LDTour tour : tours) {
            tourCount[tour.purpose.ordinal()] += tour.partySize;
        }
    }

    /**
     * Count the number of trips.
     *
     * @param trips The array of trips.
     */
    private void countTrips(LDTrip[] trips) {
        for (LDTrip trip : trips) {
            tripCount[trip.purpose.ordinal()] += trip.partySize;
            
            // trips > 100 miles for comparison to ATS           
            if (trip.distance > 100 || trip.destinationType.equals(LDTourDestinationType.EXTERNAL)) {            
                if (trip.destinationType.equals(LDTourDestinationType.INTERNAL)) {
                    internalAtsTrips[trip.mode.ordinal()] += trip.partySize; 
                } else {
                    externalAtsTrips[trip.mode.ordinal()] += trip.partySize;                 
                }
            }
        }
    }


    /**
     * Tabulate the trips departing and arriving by hour.
     *
     * @param tours The long-distance tours.
     */
    private void countSchedules(LDTour[] tours) {
        for (LDTour tour : tours) {
            int depart = (int) Math.floor(tour.schedule.getDepartureMilitaryTime()/100.0);
            int arrive = (int) Math.floor(tour.schedule.getArrivalMilitaryTime()/100.0);

            if (tour.patternType.equals(LDTourPatternType.BEGIN_TOUR)) {
                beginTourDeparture[depart] += tour.partySize;
            } else if (tour.patternType.equals(LDTourPatternType.END_TOUR)) {
                endTourArrival[arrive] += tour.partySize;
            } else if (tour.patternType.equals(LDTourPatternType.COMPLETE_TOUR)) {
                completeTourSchedule[depart][arrive] += tour.partySize;
            }
        }
    }

    /**
     * Counts the internal-external trips by purpose and pattern type.
     *
     * @param tours The array of tours.
     */
    private void countInternalExternal(LDTour[] tours) {

        for (LDTour tour : tours) {
            if (tour.destinationType.equals(LDTourDestinationType.INTERNAL)) {
                internalDest[tour.purpose.ordinal()][tour.patternType.ordinal()] += tour.partySize;
            } else {
                externalDest[tour.purpose.ordinal()][tour.patternType.ordinal()] += tour.partySize;
            }
        }
    }

    /**
     * Counts the mode choice decisions.
     *
     * @param tours The array of tours.
     */
    private void countModeChoice(LDTour[] tours) {

        for (LDTour tour : tours) {
            if (tour.destinationType.equals(LDTourDestinationType.INTERNAL)) {
                internalModeChoice[tour.purpose.ordinal()][tour.mode.ordinal()] += tour.partySize;
            } else {
                externalModeChoice[tour.purpose.ordinal()][tour.mode.ordinal()] += tour.partySize;
            }
        }
    }

    /**
     * Tabulates the trip length frequency distribution by purpose and pattern type.
     *
     * @param trips The array of trips to count.
     */
    private void countTripLengthDistribution(LDTrip[] trips) {

        for (LDTrip trip : trips) {
            if (trip.destinationType.equals(LDTourDestinationType.INTERNAL)) {
                int bin = 0;
                while ((trip.distance>internalTLFDbins[bin]) && (bin<(internalTLFDbins.length-1))) {
                    bin++;
                }
                internalTLFD[trip.purpose.ordinal()][bin] += trip.partySize;
            } else {
                int bin = 0;
                while ((trip.distance>externalTLFDbins[bin]) && (bin<(externalTLFDbins.length-1))) {
                    bin++;
                }
                externalTLFD[trip.purpose.ordinal()][bin] += trip.partySize;
            }
        }
    }

    /**
     * Counts aggregate trip distance for calculating the average.
     *
     * @param trips The array of trips.
     */
    private void countAggregateDistance(LDTrip[] trips) {

        for (LDTrip trip : trips) {
            if (trip.destinationType.equals(LDTourDestinationType.INTERNAL)) {
                internalAggregateDistance[trip.purpose.ordinal()][trip.patternType.ordinal()] += (trip.partySize * trip.distance);
                internalAggregateTrips[trip.purpose.ordinal()][trip.patternType.ordinal()] += trip.partySize;
            } else {
                externalAggregateDistance[trip.purpose.ordinal()][trip.patternType.ordinal()] += (trip.partySize * trip.distance);
                externalAggregateTrips[trip.purpose.ordinal()][trip.patternType.ordinal()] += trip.partySize;
            }
        }
    }

    /**
     * Counts the auto vehicle trips and person trips to report the
     * average auto occupancy.
     *
     * @param trips An array of trips.
     */
    private void countAutoOccupancy(LDTrip[] trips) {

        for (LDTrip trip : trips) {
            if (trip.mode.equals(LDTourModeType.AUTO)) {
                autoPersonTrips[trip.purpose.ordinal()][trip.destinationType.ordinal()] += trip.partySize;
                
                if (trip.tripMode.equals(LDTripModeType.DA)) {
                    autoVehicleTrips[trip.purpose.ordinal()][trip.destinationType.ordinal()] += 1 * trip.partySize;
                } else if (trip.tripMode.equals(LDTripModeType.SR2)) {
                    autoVehicleTrips[trip.purpose.ordinal()][trip.destinationType.ordinal()] += 0.5 * trip.partySize;
                } else if (trip.tripMode.equals(LDTripModeType.SR2)) {
                    autoVehicleTrips[trip.purpose.ordinal()][trip.destinationType.ordinal()] += 0.285 * trip.partySize;
                }
             }
        }
    }

    /**
     * Counts the total number of person trips
     *
     * @param trips An array of trips.
     */
    private void countPersonTrips(LDTrip[] trips) {

        for (LDTrip trip : trips) {
            personTripsByTripMode[trip.tripMode.ordinal()][trip.destinationType.ordinal()] += trip.partySize;
            
            int size = trip.partySize-1; 
            if (size>=3) size = 2; 
            personTripsByPartySize[trip.purpose.ordinal()][size] += trip.partySize; 
        }
    }
    
    /**
     * Counts the total vehicle miles traveled
     *
     * @param trips An array of trips.
     */
    private void countVmt(LDTrip[] trips) {

        for (LDTrip trip : trips) {
            if (trip.tripMode.equals(LDTripModeType.DA)) {
                vmt[trip.destinationType.ordinal()] += 1.0 * trip.partySize * trip.distance;
            } else if (trip.tripMode.equals(LDTripModeType.SR2)) {
                vmt[trip.destinationType.ordinal()] += 0.5 * trip.partySize * trip.distance;
            } else if (trip.tripMode.equals(LDTripModeType.SR2)) {
                vmt[trip.destinationType.ordinal()] += 0.285 * trip.partySize * trip.distance;
            }
        }
    }

    /**
     * Writes the household level decisions to the logger.
     *
     */
    public void logHouseholdLevelDecisions() {

        logger.info("");
        logger.info("Long Distance Binary Choice of Travel for Persons, for Two-Week Period");
        logger.info("Purpose                  No         Yes");
        for (int i=0; i<LDTourPurpose.values().length; i++) {
            String line = String.format("%-15s", LDTourPurpose.values()[i]);
            line = line + String.format("%,12d", binaryChoiceFalse[i]);
            line = line + String.format("%,12d", binaryChoiceTrue[i]);
            logger.info(line);
        }
        String total = "Total          ";
        total = total + String.format("%,12d", binaryChoiceFalseTotal);
        total = total + String.format("%,12d", binaryChoiceTrueTotal);
        logger.info(total);


        logger.info("");
        logger.info("Long Distance Total Person Tours on Model Day");
        String header = "PatternType   ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-12s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int pat=0; pat<LDTourPatternType.values().length; pat++) {
            String line = String.format("%-15s", LDTourPatternType.values()[pat]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,12d", personTours[purp][pat]);
            }
            logger.info(line);
        }
    }

    /**
     * Write the tour-level LDT results to the logger.
     *
     */
    public void logTourLevelDecisions() {
        logTripsAndTours();
        logSchedules();
        logInternalExternal();
        logTripLengthDistributions();
        logAverageTripLengths();
        logModeChoice();
        logAutoOccupancy();
        logPersonTrips(); 
        logVmt(); 
    }

    /**
     * Writes the number of trips and tours to the logger.
     *
     */
    private void logTripsAndTours() {

        float[] tripsPerTour = new float[LDTourPurpose.values().length];

        logger.info("");
        logger.info("Long Distance Trip and Tour Totals");
        logger.info("Purpose               Tours       Trips  Trips/Tour");
        for (int i=0; i<LDTourPurpose.values().length; i++) {
            if (tourCount[i]>0) {
                tripsPerTour[i] = ((float)tripCount[i])/((float)tourCount[i]);
            }
            String line = String.format("%-15s", LDTourPurpose.values()[i]);
            line = line + String.format("%,12d", tourCount[i]);
            line = line + String.format("%,12d", tripCount[i]);
            line = line + String.format("%,12.2f", tripsPerTour[i]);
            logger.info(line);
        }

    }

    /**
     * Writes the schedule decisions to the logger.
     *
     */
    private void logSchedules() {

        logger.info("");
        logger.info("Long Distance Begin Tour Schedules");
        logger.info("DepatureHour       Tours");
        for (int i=0; i<beginTourDeparture.length; i++) {
            String line = String.format("%,12d", i);
            line = line + String.format("%,12d", beginTourDeparture[i]);
            logger.info(line);
        }

        logger.info("");
        logger.info("Long Distance End Tour Schedules");
        logger.info("ArrivalHour        Tours");
        for (int i=0; i<endTourArrival.length; i++) {
            String line = String.format("%,12d", i);
            line = line + String.format("%,12d", endTourArrival[i]);
            logger.info(line);
        }

        logger.info("");
        logger.info("Long Distance Complete Tour Schedules");
        logger.info("Tours                         ArrivalHour");
        logger.info("DepartureHour                 ");
        for (int i=0; i<completeTourSchedule.length; i++) {
            String line = String.format("%,12d", i);
            for (int j=0; j<completeTourSchedule[i].length; j++) {
                line = line + String.format("%,12d", completeTourSchedule[i][j]);
            }
            logger.info(line);
        }
    }

    /**
     * Writes the internal-external decisions to the logger.
     *
     */
    private void logInternalExternal() {

        logger.info("");
        logger.info("Long Distance Tours with Internal Destinations");
        String header = "PatternType      ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-12s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int pat=0; pat<LDTourPatternType.values().length; pat++) {
            String line = String.format("%-15s", LDTourPatternType.values()[pat]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,12d", internalDest[purp][pat]);
            }
            logger.info(line);
        }

        logger.info("");
        logger.info("Long Distance Tours with External Destinations");
        header = "PatternType      ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-12s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int pat=0; pat<LDTourPatternType.values().length; pat++) {
            String line = String.format("%-15s", LDTourPatternType.values()[pat]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,12d", externalDest[purp][pat]);
            }
            logger.info(line);
        }
    }

    /**
     * Writes the mode choice results to the logger.
     *
     */
    private void logModeChoice() {

        logger.info("");
        logger.info("Long Distance Tours with Internal Destinations--Mode Choice");
        String header = "Mode             ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-12s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int mode=0; mode<LDTourModeType.values().length; mode++) {
            String line = String.format("%-15s", LDTourModeType.values()[mode]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,12d", internalModeChoice[purp][mode]);
            }
            logger.info(line);
        }

        logger.info("");
        logger.info("Long Distance Tours with External Destinations--Mode Choice");
        header = "Mode             ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-12s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int mode=0; mode<LDTourModeType.values().length; mode++) {
            String line = String.format("%-15s", LDTourModeType.values()[mode]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,12d", externalModeChoice[purp][mode]);
            }
            logger.info(line);
        }
        
        logger.info("");
        logger.info("Long Distance Trips > 100 Miles (including all external)--for Comparison to American Travel Survey");
        header = "Mode           Internal    External    Total";
        logger.info(header);
        for (int mode=0; mode<LDTourModeType.values().length; mode++) {
            String line = String.format("%-15s", LDTourModeType.values()[mode]);
            line = line + String.format("%,12d", internalAtsTrips[mode]);
            line = line + String.format("%,12d", externalAtsTrips[mode]); 
            line = line + String.format("%,12d", (internalAtsTrips[mode] + externalAtsTrips[mode]));
            logger.info(line);
        }
    }

    /**
     * Writes the trip length frequency distributions to the logger.
     *
     */
    private void logTripLengthDistributions() {

        // internal TLFDs
        logger.info("");
        logger.info("Long Distance Tours with Internal Destinations");

        String header = "DistanceMax      ";
        for (int purp = 0; purp < LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-12s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);

        for (int bin = 0; bin < internalTLFDbins.length; bin++) {
            String line = String.format("%,15d", internalTLFDbins[bin]);
            for (int purp = 0; purp < LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,12d", internalTLFD[purp][bin]);
            }
            logger.info(line);
        }

        // external TLFDs
        logger.info("");
        logger.info("Long Distance Tours with External Destinations");

        header = "DistanceMax      ";
        for (int purp = 0; purp < LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-12s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);

        for (int bin = 0; bin < externalTLFDbins.length; bin++) {
            String line = String.format("%,15d", externalTLFDbins[bin]);
            for (int purp = 0; purp < LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,12d", externalTLFD[purp][bin]);
            }
            logger.info(line);
        }
    }

    /**
     * Writes the average trip lengths to the logger.
     *
     */
    private void logAverageTripLengths() {

        logger.info("");
        logger.info("Long Distance Tours with Internal Destinations--Aggregate Trip Length (mi)");
        String header = "PatternType      ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-16s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int pat=0; pat<LDTourPatternType.values().length; pat++) {
            String line = String.format("%-15s", LDTourPatternType.values()[pat]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,16.2f", internalAggregateDistance[purp][pat]);
            }
            logger.info(line);
        }

        logger.info("");
        logger.info("Long Distance Tours with Internal Destinations--Aggregate Number of Trips");
        header = "PatternType      ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-16s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int pat=0; pat<LDTourPatternType.values().length; pat++) {
            String line = String.format("%-15s", LDTourPatternType.values()[pat]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,16.2f", internalAggregateTrips[purp][pat]);
            }
            logger.info(line);
        }

        logger.info("");
        logger.info("Long Distance Tours with External Destinations--Aggregate Trip Length (mi)");
        header = "PatternType      ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-16s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int pat=0; pat<LDTourPatternType.values().length; pat++) {
            String line = String.format("%-15s", LDTourPatternType.values()[pat]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,16.2f", externalAggregateDistance[purp][pat]);
            }
            logger.info(line);
        }

        logger.info("");
        logger.info("Long Distance Tours with External Destinations--Aggregate Number of Trips");
        header = "PatternType      ";
        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            header = header + String.format("%-16s", LDTourPurpose.values()[purp]);
        }
        logger.info(header);
        for (int pat=0; pat<LDTourPatternType.values().length; pat++) {
            String line = String.format("%-15s", LDTourPatternType.values()[pat]);
            for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
                line = line + String.format("%,16.2f", externalAggregateTrips[purp][pat]);
            }
            logger.info(line);
        }

    }

    /**
     * Prints the average auto occupancy to the logger.
     *
     */
    private void logAutoOccupancy() {

        logger.info("");
        logger.info("Long Distance Trip Average Auto Occupancy");
        String header = "Purpose          ";
        for (int dest=0; dest<LDTourDestinationType.values().length; dest++) {
            header = header + String.format("%-16s", LDTourDestinationType.values()[dest]);
        }
        logger.info(header);

        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            String line = String.format("%-16s", LDTourPurpose.values()[purp]);
            for (int dest=0; dest<LDTourDestinationType.values().length; dest++) {
                float autoOcc = ((float) autoPersonTrips[purp][dest]) / ((float) autoVehicleTrips[purp][dest]);
                line = line + String.format("%,16.2f", autoOcc);
            }
            logger.info(line);
        }

    }


    /**
     * Prints the person trips by trip mode to the logger.
     *
     */
    private void logPersonTrips() {

        logger.info("");
        logger.info("Long Distance Person Trips--by Trip Mode");
        String header = "TripMode          ";
        for (int dest=0; dest<LDTourDestinationType.values().length; dest++) {
            header = header + String.format("%-16s", LDTourDestinationType.values()[dest]);
        }
        logger.info(header);

        for (int mode=0; mode<LDTripModeType.values().length; mode++) {
            String line = String.format("%-16s", LDTripModeType.values()[mode]);
            for (int dest=0; dest<LDTourDestinationType.values().length; dest++) {
                line = line + String.format("%,16d", personTripsByTripMode[mode][dest]);
            }
            logger.info(line);
        }
        
        logger.info("");
        logger.info("Long Distance Person Trips--by Party Size");
        header = "Purpose       1        2          3          ";
        logger.info(header);

        for (int purp=0; purp<LDTourPurpose.values().length; purp++) {
            String line = String.format("%-16s", LDTourPurpose.values()[purp]);
            for (int size=0; size<3; size++) {
                line = line + String.format("%,16d", personTripsByPartySize[purp][size]);
            }
            logger.info(line);
        }
    }
    

    /**
     * Prints the vehicle miles traveled to the logger.
     *
     */
    private void logVmt() {

        logger.info("");
        logger.info("Long Distance Vehicle Miles Traveled");
        String header = "DestinationType     VMT     ";
        logger.info(header);
        for (int dest=0; dest<LDTourDestinationType.values().length; dest++) {
            String line = String.format("%-16s", LDTourDestinationType.values()[dest]);
            line = line + String.format("%,16.0f", vmt[dest]);
            logger.info(line);
        }

    }
    
}
