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
import com.pb.models.pt.util.TravelTimeAndCost;

import org.apache.log4j.Logger;

import static com.pb.models.pt.TourModeParameters.*;

/**  
 * Bike Mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class Bike extends Mode {
    final static Logger logger = Logger.getLogger(Bike.class);
     
    final static long serialVersionUID = 666;

//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
//     double utility=0;

     public Bike(){
          isAvailable = true;
          hasUtility = false;
          utility = 0.0D;
          alternativeName=new String("Bike");
          type=TourModeType.BIKE;
     }
     
    /** Calculates utility biking
     * 
     * @param inbound - In-bound TravelTimeAndCost
     * @param outbound - Outbound TravelTimeAndCost
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
     public void calcUtility(TravelTimeAndCost inbound,
            TravelTimeAndCost outbound, float[] c, TourModePersonAttributes p) {

         hasUtility = false;
         utility=-999;
         isAvailable = true;

         //Add an aditional condition to alow intraZonal Bike longer than 8 miles    
        if ((inbound.bikeDistance > 8.0) &&( inbound.itaz!=inbound.jtaz))
            isAvailable = false;
        if ((outbound.bikeDistance > 8.0)&&( outbound.itaz!=outbound.jtaz))
            isAvailable = false;

        if (isAvailable) {
            time = (inbound.bikeTime + outbound.bikeTime);
            utility = (c[BIKE_TIME] * (inbound.bikeTime + outbound.bikeTime)
                    + c[BIKE]
                    + c[BIKE_AW_0] * p.auwk0
                    + c[BIKE_AW_I] * p.auwk1
                    + c[BIKE_AW_S] * p.auwk2
                    + c[BIKE_STOPS] * p.totalStops);

            if (trace) {
                logger.info("Utility for bike mode: " + utility + " = ");
                logger.info( c[BIKE_TIME] + "*(" + inbound.bikeTime + "+" + outbound.bikeTime + ")");
                logger.info(" + "+ c[BIKE]);
                logger.info(" + "+ c[BIKE_AW_0] + "*" + p.auwk0);
                logger.info(" + "+ c[BIKE_AW_I] + "*" + p.auwk1);
                logger.info(" + "+ c[BIKE_AW_S] + "*" + p.auwk2);
                logger.info(" + "+ c[BIKE_STOPS] + "*" + p.totalStops);
            }

            hasUtility = true;
        } else if (trace) {
            logger.info("Bike is not available.");
        }
    }
     
     /**
         * Get drive transit utility
         */
     @Override
     public double getUtility() {
         if (!hasUtility) {
             String msg = "Error: Utility not calculated for " + alternativeName;
             logger.fatal(msg);
             //TODO - log this error to the node exception file
             throw new ModelException(msg);
         }
         return utility;
     }
 }