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
package com.pb.models.pt.ldt.tourmodes;

import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.Mode;
import com.pb.models.pt.PriceConverter;
import com.pb.models.pt.ldt.LDModeChoiceHouseholdAttributes;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.LDTourModeType;
import com.pb.models.pt.ldt.LDTourPatternType;
import com.pb.models.pt.util.TravelTimeAndCost;

import org.apache.log4j.Logger;

import static com.pb.models.pt.ldt.LDInternalModeChoiceParameters.*;

import java.util.ResourceBundle;

/**
 * Long-distance train mode.
 *
 * @author Erhardt
 * @version 1.0 Apr 11, 2006
 *
 */
public class HsrDrive extends Mode {

    protected static Logger logger = Logger.getLogger(HsrDrive.class);

    private final static long serialVersionUID = 872;

    private static int m;

    private float taxiRatePerMinute;
    private float parkingCost;
    private float[] averageNumDays;

    public HsrDrive(ResourceBundle rb) {

        isAvailable = true;
        hasUtility = false;
        utility = 0.0D;
        alternativeName = new String("HighSpeedRailDrive");
//        type = LDTourModeType.HSR_DRIVE;
        HsrDrive.m = type.ordinal();

        // get some parameters used to calculate cost
//        taxiRatePerMinute = (float) ResourceUtil.getDoubleProperty(rb, "ldt.taxi.rate.per.minute.in.cents");
//        parkingCost       = (float) ResourceUtil.getDoubleProperty(rb, "ldt.airport.parking.cost.in.cents");
        PriceConverter priceConverter = PriceConverter.getInstance(); //will throw exception if not initialized yet
        taxiRatePerMinute = (float) priceConverter.convertPrice(ResourceUtil.getDoubleProperty(rb, "ldt.taxi.rate.per.minute.in.cents"),PriceConverter.ConversionType.PRICE);
        parkingCost       = (float) priceConverter.convertPrice(ResourceUtil.getDoubleProperty(rb, "ldt.airport.parking.cost.in.cents"),PriceConverter.ConversionType.PRICE);

        String[] averageDaysArray = ResourceUtil.getArray(rb, "ldt.average.duration.multi-day.trip.by.purpose");
        averageNumDays = new float[averageDaysArray.length];
        for (int i=0; i<averageDaysArray.length; i++) {
            averageNumDays[i] = new Float(averageDaysArray[i]);
        }
    }


    /**
     * Calculates utility of choosing this mode
     *
     * @param c -
     *            TourModeParameters
     * @param hh -
     *            LDModeChoiceHouseholdAttributes
     * @param tour -
     *            LD tour of interest
     * @param tc -
     *            TravelTimeAndCost
     * @return boolean flag indicating if the mode is available.
     */
    public boolean calculateUtility(float[] c, LDModeChoiceHouseholdAttributes hh,
            LDTour tour, TravelTimeAndCost tc) {

        // calculate tour attributes
        int completeTour = 0;
        if (tour.patternType.equals(LDTourPatternType.COMPLETE_TOUR)) {
            completeTour = 1;
        }

        // set availability
        isAvailable = true;
        if (tc.icRailInVehicleTime[HsrDrive.m]==0) isAvailable = false;
        if (completeTour==1) {
            if (tc.totalTime[HsrDrive.m] > (tour.schedule.duration)) {
                isAvailable = false;
            }
        }

        // scale cost such that all travelers must pay fare
        float cost = tc.cost[HsrDrive.m] * tour.partySize;

        // if no HH vehicle, must pay taxi fare to station
        if (hh.autos==0) {
            cost += taxiRatePerMinute * tc.driveTimeForLDMode[HsrDrive.m];
        }
        // otherwise, must pay for parking
        else {
            if (tour.patternType==LDTourPatternType.COMPLETE_TOUR)
                cost += parkingCost;
            else
                cost += parkingCost * averageNumDays[tour.purpose.ordinal()];
        }

        // calculate utility
        if (isAvailable) {
        	
            utility = 0;
            utility += c[INVEHICLETIME] * tc.inVehicleTime [HsrDrive.m];
            utility += c[HSRINVEHICLETIME] * tc.inVehicleTime [HsrDrive.m];
            utility += c[WALKTIME] * tc.walkTimeForLDMode      [HsrDrive.m];
//            utility += c[DRIVETIME] * tc.driveTime     [HsrDrive.m];
            utility += c[WAITTIME] * tc.waitTime [HsrDrive.m];
            utility += c[TERMINALTIME  ] * tc.terminalTime[HsrDrive.m]; 
            utility += c[COSTINC020] * cost * hh.inclow;
            utility += c[COSTINC2060] * cost * hh.incmed;
            utility += c[COSTINC60P] * cost * hh.inchi;
//            utility += c[CONSTHSR_DRIVE];
            
            // account for frequency in Oregon
            if (c.length>FREQUENCYCOEFFICIENT) {
            	double frequencyTerm = Math.pow(c[FREQUENCYNUMERATOR] / tc.frequency[HsrDrive.m], c[FREQUENCYPOWER]);
            	utility += c[FREQUENCYCOEFFICIENT] * frequencyTerm; 
            }

            hasUtility = true;
        }

        return isAvailable;
    }

    /**
     * Get drive transit utility
     */
    public double getUtility() {
        if (!hasUtility) {
            String msg = "Error: Utility not calculated for " + alternativeName;
            HsrDrive.logger.fatal(msg);
            throw new ModelException(msg);
        }
        return utility;
    }
}
