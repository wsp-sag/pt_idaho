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
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.Mode;
import static com.pb.models.pt.TourModeParameters.*;
import com.pb.models.pt.TourModePersonAttributes;
import com.pb.models.pt.TourModeType;
import com.pb.models.pt.ZoneAttributes;
import com.pb.models.pt.util.TravelTimeAndCost;
import org.apache.log4j.Logger;

/**
 * Passenger Transit mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class PassengerTransit extends Mode {
    final static Logger logger = Logger.getLogger(PassengerTransit.class);

    static final long serialVersionUID = 666;

    // public boolean isAvailable=true;
    // public boolean hasUtility=false;

    // double utility=0;

    public PassengerTransit() {
        isAvailable = true;
        hasUtility = false;
        utility = 0.0D;
        alternativeName = new String("PassengerTransit");
        type = TourModeType.PASSENGERTRANSIT;
    }

    /**
     * Calculates utility of passenger-transit mode
     * 
     * @param inbound -
     *            In-bound TravelTimeAndCost
     * @param outbound -
     *            Outbound TravelTimeAndCost
     * @param z -
     *            ZoneAttributes (Currently only parking cost)
     * @param c -
     *            TourModeParameters
     * @param p -
     *            PersonTourModeAttributes
     */
public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
           ZoneAttributes z,float[] c, TourModePersonAttributes p){

    hasUtility = false;
    utility=-999;
    isAvailable = true;

    if(inbound.sharedRide2Time==0.0) isAvailable=false;
          if(outbound.walkTransitInVehicleTime==0.0) isAvailable=false;
          if(p.tourPurpose==ActivityPurpose.WORK_BASED) isAvailable=false;

          if(isAvailable){
              double op_cost = outbound.walkTransitFare + z.parkingCost
                    * (p.primaryDuration / 60) + inbound.sharedRide2Cost; 
              
              time = outbound.walkTransitInVehicleTime + inbound.sharedRide2Time
                  + outbound.walkTransitFirstWaitTime
                  + outbound.walkTransitTransferWaitTime
                  + outbound.walkTransitWalkTime
                  + outbound.transitOvt;
              
              utility=
                  c[IVT]*(outbound.walkTransitInVehicleTime+inbound.sharedRide2Time)
                  + c[FWT]*outbound.walkTransitShortFirstWaitTime
                  + c[XWT]*outbound.walkTransitTransferWaitTime
                  + c[WLK]*outbound.walkTransitWalkTime
                  + c[COST_LOW] * op_cost * p.inclow
                  + c[COST_MED]* op_cost * p.incmed
                  + c[COST_HI] * op_cost * p.inchi
                  + c[PASS_TRAN]
                  + c[PASS_TRAN_AW_0]*p.auwk0 
                  + c[PASS_TRAN_AW_I]*p.auwk1 
                  + c[PASS_TRAN_AW_S]*p.auwk2
                  + c[PASS_TRAN_STOPS]*p.totalStops
                  + c[PASS_H1]*p.size1
                  + c[PASS_H2]*p.size2
                  + c[PASS_H3]*p.size3p
                  + c[OVT]*outbound.transitOvt;

              if(utility == Double.NEGATIVE_INFINITY || utility == Double.POSITIVE_INFINITY){
                  utility = -999;
                  isAvailable = false;
              }

              if (trace) {
                  logger.info("Passenger transit utility: " + utility + " = ");
                  logger.info("\t" + c[IVT] + "* (" + outbound.walkTransitInVehicleTime+ "+" + inbound.sharedRide2Time + ")");
                  logger.info("\t" + c[FWT] + "*" + outbound.walkTransitShortFirstWaitTime);
                  logger.info("\t" + c[XWT] + "*" + outbound.walkTransitTransferWaitTime);
                  logger.info("\t" + c[WLK] + "*" + outbound.walkTransitWalkTime);
                  logger.info("\t" + c[COST_LOW] + "*" + op_cost + "*" + p.inclow);
                  logger.info("\t" + c[COST_MED] + "*" + op_cost + "*" + p.incmed);
                  logger.info("\t" + c[COST_HI] +  "*" + op_cost + "*" + p.inchi);
                  logger.info("\t" + c[PASS_TRAN]);
                  logger.info("\t" + c[PASS_TRAN_AW_0] + "*" + p.auwk0); 
                  logger.info("\t" + c[PASS_TRAN_AW_I] + "*" + p.auwk1); 
                  logger.info("\t" + c[PASS_TRAN_AW_S] + "*" + p.auwk2);
                  logger.info("\t" + c[PASS_TRAN_STOPS] + "*" + p.totalStops);                 
                  logger.info("\t" + c[PASS_H1]+ "*" + p.size1);
                  logger.info("\t" + c[PASS_H2] + "*" + p.size2);
                  logger.info("\t" + c[PASS_H3] + "*" + p.size3p);
                  logger.info("\t" + c[OVT] + "*" + outbound.transitOvt);
              }
              
              hasUtility=true;
          } else if (trace) {
              logger.info("Passenger transit is not available.");
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
            // TODO - log this error to the node exception file
            throw new ModelException(msg);
        }
        return utility;
    }
}