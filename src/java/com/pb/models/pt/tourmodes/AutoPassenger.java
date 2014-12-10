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
 * Passenger mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class AutoPassenger extends Mode {
    final static Logger logger = Logger.getLogger(AutoPassenger.class);

    final static long serialVersionUID = 666;
    
    // public String alternativeName="AutoPassenger";

     //public boolean isAvailable=true;
    // public boolean hasUtility=false;
    // double utility=0;
     
    public AutoPassenger() {
        isAvailable = true;
        hasUtility = false;
        utility = 0.0D;
        alternativeName = "AutoPassenger";
        type = TourModeType.AUTOPASSENGER;
    }

     /** Calculates Utility of Auto Passenger mode
      * 
      * @param inbound - In-bound TravelTimeAndCost
      * @param outbound - Outbound TravelTimeAndCost
      * @param zOrigin - ZoneAttributes at Origin (Currently parking cost and terminal time)
      * @param zDestination - ZoneAttributes at Destination (Currently parking cost and terminal time)
      * @param c - TourModeParameters
      * @param p - PersonTourModeAttributes
      */
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
          ZoneAttributes zOrigin, ZoneAttributes zDestination, float[] c, TourModePersonAttributes p){
               
         hasUtility = false;
         utility=-999;
         isAvailable = true;

         if(inbound.sharedRide2Time==0) isAvailable=false;
          if(outbound.sharedRide2Time==0) isAvailable=false;
               
          if(isAvailable){
               time=inbound.sharedRide2Time + outbound.sharedRide2Time;
                //if duration is zero, round it to 1 for parking costs
                float duration = p.primaryDuration == 0 ? 60 : p.primaryDuration;
               utility=
                 c[IVT]*(inbound.sharedRide2Time+outbound.sharedRide2Time)
                 + c[COST_LOW]*(inbound.sharedRide2Cost+outbound.sharedRide2Cost)*p.inclow
                 + c[COST_MED]*(inbound.sharedRide2Cost+outbound.sharedRide2Cost)*p.incmed
                 + c[COST_HI]*(inbound.sharedRide2Cost+outbound.sharedRide2Cost)*p.inchi
                 + c[COST_LOW]*zDestination.parkingCost*(duration/60)*p.inclow
                 + c[COST_MED]*zDestination.parkingCost*(duration/60)*p.incmed
                 + c[COST_HI]*zDestination.parkingCost*(duration/60)*p.inchi
                 + c[WLK]*2*zOrigin.terminalTime
                 + c[WLK]*2*zDestination.terminalTime
                 + c[PASS]
                 + c[PASS_AW_0]*p.auwk0 
                 + c[PASS_AW_I]*p.auwk1 
                 + c[PASS_AW_S]*p.auwk2
                 + c[PASS_STOPS]*p.totalStops
                 + c[PASS_H1]*p.size1
                 + c[PASS_H2]*p.size2
                 + c[PASS_H3]*p.size3p;
               if (trace) {
				logger.info("Auto passenger utility : "+utility+" = ");
				logger.info(c[IVT] + " * " + "(" + inbound.sharedRide2Time
						+ " + " + outbound.sharedRide2Time + ")");
				logger.info(c[COST_LOW] + " * " + "(" + inbound.sharedRide2Cost
						+ " + " + outbound.sharedRide2Cost + ")" + " * "
						+ p.inclow);
				logger.info(c[COST_MED] + " * " + "(" + inbound.sharedRide2Cost
						+ " + " + outbound.sharedRide2Cost + ")" + " * "
						+ p.incmed + ")");
				logger.info(c[COST_HI] + " * " + "(" + inbound.sharedRide2Cost
						+ " + " + outbound.sharedRide2Cost + ")" + " * "
						+ p.inchi);
				logger.info(c[COST_LOW] + " * " + zDestination.parkingCost + " * "
						+ duration / 60 + " * " + p.inclow);
				logger.info(c[COST_MED] + " * " + zDestination.parkingCost + " * "
						+ duration / 60 + " * " + p.incmed);
				logger.info(c[COST_HI] + " * " + zDestination.parkingCost + " * "
						+ duration / 60 + " * " + p.inchi);
                logger.info(c[WLK] + " * " + 2 * zOrigin.terminalTime);
                logger.info(c[WLK] + " * " + 2 * zDestination.terminalTime);
                logger.info(c[PASS]);
                logger.info(c[PASS_AW_0] + " * " + p.auwk0);
				logger.info(c[PASS_AW_I] + " * " + p.auwk1);
				logger.info(c[PASS_AW_S] + " * " + p.auwk2);
				logger.info(c[PASS_STOPS] + " * " + p.totalStops);
				logger.info(c[PASS_H1] + " * " + p.size1);
				logger.info(c[PASS_H2] + " * " + p.size2);
				logger.info(c[PASS_H3] + " * " + p.size3p);
			}
               hasUtility=true;
          } else if (trace) {
              logger.info("Auto passenger is not available.");
          }
     }

     /**
      *  Get drive transit utility
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