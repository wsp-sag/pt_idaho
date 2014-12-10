/*
 * Copyright  2005 PB Consult Inc.
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
package com.pb.models.pt.tourmodes;

import com.pb.common.model.ModelException;
import com.pb.models.pt.Mode;
import com.pb.models.pt.TourModeType;
import com.pb.models.pt.TourModePersonAttributes;
import com.pb.models.pt.ZoneAttributes;
import com.pb.models.pt.util.TravelTimeAndCost;

import org.apache.log4j.Logger;

import static com.pb.models.pt.TourModeParameters.*;

/**
 * Driver mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 */
public class AutoDriver extends Mode {
    final static Logger logger = Logger.getLogger(AutoDriver.class);

    final static long serialVersionUID = 666;
    
    // public boolean isAvailable=true;
    // public boolean hasUtility=false;

    // double utility=0;

    public AutoDriver() {
        isAvailable = true;
        hasUtility = false;
        utility = 0.0D;
        alternativeName = "AutoDriver";
        type = TourModeType.AUTODRIVER;
    }

    /**
     * Calculates utility of being an auto driver
     * 
     * @param inbound -
     *            In-bound TravelTimeAndCost
     * @param outbound -
     *            Outbound TravelTimeAndCost
     * @param zOrigin -
     *            ZoneAttributes at origin (Currently parking cost and terminal time)
     * @param zDestination -
     *            ZoneAttributes at destination (Currently parking cost and terminal time)
     * @param c -
     *            TourModeParameters
     * @param p -
     *            PersonTourModeAttributes
     */
    public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound, ZoneAttributes zOrigin,
                            ZoneAttributes zDestination, float[] c, TourModePersonAttributes p) {

        hasUtility = false;
        utility=-999;
        isAvailable = true;

        if (p.age < 16)
            isAvailable = false;
        if (p.autos == 0)
            isAvailable = false;

        if (isAvailable) {
            time = inbound.driveAloneTime + outbound.driveAloneTime + 2 * zOrigin.terminalTime + 2 * zDestination.terminalTime;
            //if duration is zero, round it to 1 for parking costs
            float duration = p.primaryDuration == 0 ? 60 : p.primaryDuration;
            utility = (c[IVT]
                    * (inbound.driveAloneTime + outbound.driveAloneTime)
                    + c[WLK] * 2 * zOrigin.terminalTime
                    + c[WLK] * 2 * zDestination.terminalTime
                    + c[COST_LOW]
                    * (((inbound.driveAloneCost + outbound.driveAloneCost) + 
                            (zDestination.parkingCost * (duration / 60))) * p.inclow)
                    + c[COST_MED]
                    * (((inbound.driveAloneCost + outbound.driveAloneCost) + 
                            (zDestination.parkingCost * (duration / 60))) * p.incmed)
                    + c[COST_HI]
                    * (((inbound.driveAloneCost + outbound.driveAloneCost) + 
                            (zDestination.parkingCost * (duration / 60))) * p.inchi));
            
            if (trace) {
                logger.info("Auto driver utility: " +utility+ " = ");
                logger.info( c[IVT] + "* (" + inbound.driveAloneTime + " + "
                        + outbound.driveAloneTime + ")");
                logger.info( c[WLK] + "* (" + 2 * zOrigin.terminalTime + ")");
                logger.info( c[WLK] + "* (" + 2 * zDestination.terminalTime + ")");
                logger.info(" + " + c[COST_LOW]+ " * (((" + inbound.driveAloneCost + " + "
                        + outbound.driveAloneCost + ")" + "+ (" + zDestination.parkingCost
                        + "* (" + duration / 60 + "))) * " + p.inclow
                        + ")" );
                logger.info(" + " + c[COST_MED] + "* ((("
                        + inbound.driveAloneCost + " + "
                        + outbound.driveAloneCost + ") +" + "(" + zDestination.parkingCost
                        + "*(" + duration / 60 + "))) *" + p.incmed
                        + ")");
                logger.info(" + "+ c[COST_HI] + "* (((" + inbound.driveAloneCost
                        + " + " + outbound.driveAloneCost + ")+" + "("
                        + zDestination.parkingCost + "* (" + duration / 60
                        + "))) *" + p.inchi + ")");
            }
            hasUtility = true;
        } else if (trace) {
            logger.info("Auto driver is not available.");
        }
    }
    
    /**
     * Get drive transit utility
     */
    public double getUtility() {
        if (!hasUtility) {
            String msg = "Error: Utility not calculated for " + alternativeName;
            logger.fatal(msg);
            // TODO - log this error to the node exception file
            throw new ModelException(msg);
        }
        return utility;
    }
}