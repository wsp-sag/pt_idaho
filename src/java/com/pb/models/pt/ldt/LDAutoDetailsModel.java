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

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.models.pt.util.SkimsInMemory;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.Random;

/**
 * Draws from frequencies to determine the auto occupancy for long-distance
 * auto trips.  Also, determines which airport to send people to so the highway
 * portion can be assigned.
 *
 * @author Erhardt
 * @version 1.0 Nov 2, 2006
 *
 */
public class LDAutoDetailsModel {

    protected static Logger logger = Logger.getLogger(LDSchedulingModel.class);

    // list of frequencies by purpose and auto occupancy
    private float[] avgAutoOcc;
    
    // probability of choosing each 
    private float[] daProb; 
    private float[] sr2Prob; 
    private float[] sr3Prob;

    public static SkimsInMemory skims;
    // distance matrix for determining which airport is nearest
    private Matrix dist;

    // list of zones with airports
    private int[] airportZones;

    private long ldAutoDetailsFixedSeed = Long.MIN_VALUE/27;

    /**
     * Default constructor
     *
     */
    public LDAutoDetailsModel(ResourceBundle ptRb) {
        readParameters(ptRb);
        
        daProb = new float[avgAutoOcc.length];
        sr2Prob = new float[avgAutoOcc.length];
        sr3Prob = new float[avgAutoOcc.length];
        for (int i=0; i<avgAutoOcc.length; i++) {
            sr2Prob[i] = (avgAutoOcc[i] - 1) * 0.5f; 
            sr3Prob[i] = (avgAutoOcc[i] - 1) * 0.2f; 
            daProb[i] = 1 - sr2Prob[i] - sr3Prob[i]; 
        }
        skims = SkimsInMemory.getSkimsInMemory();
        dist = skims.opDist;
    }

    /**
     * Read parameters.
     *
     */
    private void readParameters(ResourceBundle rb) {

        LDAutoDetailsModel.logger.info("Reading LD Auto Details Parameters");

        // first the auto occupancy
        String[] autoOccString = ResourceUtil.getArray(rb, "ldt.average.auto.occupancy.by.purpose");
        avgAutoOcc = new float[autoOccString.length];
        for (int i=0; i<autoOccString.length; i++) {
            avgAutoOcc[i] = (new Float(autoOccString[i])).floatValue();
            if (avgAutoOcc[i] < 1) avgAutoOcc[i] = 1;
            if (avgAutoOcc[i] > 2.4) avgAutoOcc[i] = 2.4f; 
        }

        // then the airport zones
//        String[] airportZoneString = ResourceUtil.getArray(rb, "ldt.airport.zones");
//        airportZones = new int[airportZoneString.length];
//        for (int i=0; i<airportZoneString.length; i++) {
//            airportZones[i] = (new Integer(airportZoneString[i])).intValue();
//        }
        
        TableDataSet airportData = ParameterReader.readParametersAsTable(rb,"ldt.airport.zones");
        airportZones = new int[airportData.getRowCount()];
        for (int i = 0; i < airportData.getRowCount(); i++) {
        	airportZones[i] = (int) airportData.getValueAt(i+1, "TAZ");
        }
    }

    /**
     * Choose the trip mode, with the correct auto occupancy. 
     * 
     * @param tour
     * @return the trip mode, with correct auto occupancy.
     */
    public LDTripModeType chooseTripMode(LDTour tour, boolean sensitivityTesting) {


        switch (tour.mode) {
        case AIR:           return LDTripModeType.AIR; 
        case TRANSIT_WALK:  return LDTripModeType.TRANSIT_WALK; 
//        case TRANSIT_DRIVE: return LDTripModeType.TRANSIT_DRIVE;
//        case HSR_WALK:      return LDTripModeType.HSR_WALK;
//        case HSR_DRIVE:     return LDTripModeType.HSR_DRIVE;
        default:                                   
            if (tour.purpose.equals(LDTourPurpose.HOUSEHOLD)) {
                if (tour.partySize>=3) return LDTripModeType.SR3P; 
                else if (tour.partySize==2) return LDTripModeType.SR2; 
                else return LDTripModeType.DA; 
            } else {
                long seed = tour.hh.ID*100 + tour.person.memberID + tour.ID + ldAutoDetailsFixedSeed;
                if(sensitivityTesting) seed += System.currentTimeMillis();

                Random randomSequence = new Random();
                randomSequence.setSeed(seed);
                double random = randomSequence.nextDouble();
                if (random < daProb[tour.purpose.ordinal()]) 
                    return LDTripModeType.DA; 
                else if (random < (daProb[tour.purpose.ordinal()]+sr2Prob[tour.purpose.ordinal()])) 
                    return LDTripModeType.SR2;
                else 
                    return LDTripModeType.SR3P;
            }
        }
    }
    

    /**
     * The portion of air trips that drives to the airport will be assigned
     * as highway trips to the nearest airport.  Returns the TAZ of the
     * nearest airport.
     *
     * @param tour The tour of interest.
     * @return The TAZ of the airport used.
     */
    public int chooseAirportTaz(LDTour tour) {

        int nearestAirport = 0;
        float nearestDist  = Float.MAX_VALUE;

        for (int i=0; i<airportZones.length; i++) {
            float currentDist = dist.getValueAt(tour.homeTAZ, airportZones[i]);
            if (currentDist < nearestDist) {
                nearestDist = currentDist;
                nearestAirport = airportZones[i];
            }
        }

        return nearestAirport;
    }
}
