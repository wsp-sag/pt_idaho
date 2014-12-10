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
import static com.pb.models.pt.TourModeParameters.*;
import com.pb.models.pt.TourModePersonAttributes;
import com.pb.models.pt.TourModeType;
import com.pb.models.pt.util.TravelTimeAndCost;
import org.apache.log4j.Logger;

/**  
 * Walk-Transit mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class WalkTransit extends Mode {
    static Logger logger = Logger.getLogger(WalkTransit.class);

    static final long serialVersionUID = 666;

//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
     
//     double utility=0;

     public WalkTransit(){
         isAvailable = true;
                 hasUtility = false;
                 utility = 0.0D;
          alternativeName=new String("WalkTransit");
          type=TourModeType.WALKTRANSIT;
     }
     
    /** Calculates utility of walk-transit mode
     * 
     * @param inbound - In-bound TravelTimeAndCost
     * @param outbound - Outbound TravelTimeAndCost
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
           float[] c, TourModePersonAttributes p){

         hasUtility = false;
         utility=-999;
         isAvailable = true;

         if(inbound.walkTransitInVehicleTime==0.0) isAvailable=false;
          if(outbound.walkTransitInVehicleTime==0.0) isAvailable=false;
          
          if(isAvailable){
               time= (inbound.walkTransitInVehicleTime+outbound.walkTransitInVehicleTime
                 + inbound.walkTransitFirstWaitTime
                 + outbound.walkTransitFirstWaitTime
                 + inbound.walkTransitTransferWaitTime
                 + outbound.walkTransitTransferWaitTime
                 + inbound.walkTransitWalkTime+outbound.walkTransitWalkTime)
                 + inbound.transitOvt + outbound.transitOvt;

               utility=(
              c[IVT]*(inbound.walkTransitInVehicleTime+outbound.walkTransitInVehicleTime)
            + c[FWT]*(inbound.walkTransitFirstWaitTime
                 +outbound.walkTransitFirstWaitTime)
            + c[XWT]*(inbound.walkTransitTransferWaitTime
                 +outbound.walkTransitTransferWaitTime)
            + c[WLK]*(inbound.walkTransitWalkTime+outbound.walkTransitWalkTime)
            + c[COST_LOW]*((inbound.walkTransitFare+outbound.walkTransitFare)*p.inclow)
            + c[COST_MED]*((inbound.walkTransitFare+outbound.walkTransitFare)*p.incmed)
            + c[COST_HI]*((inbound.walkTransitFare+outbound.walkTransitFare)*p.inchi)
            + c[WALK_TRAN]
            + c[WALK_TRAN_AW_0]*p.auwk0 
            + c[WALK_TRAN_AW_I]*p.auwk1 
            + c[WALK_TRAN_AW_S]*p.auwk2
            + c[WALK_TRAN_STOPS]*p.totalStops
            + c[OVT]*(inbound.transitOvt+outbound.transitOvt)
               );

              if(utility == Double.NEGATIVE_INFINITY || utility == Double.POSITIVE_INFINITY){
                  utility = -999;
                  isAvailable = false;
              }
               
               if (trace) {
                   logger.info("walk transit utility: " + utility);
                logger
                        .info(c[IVT]
                                + "*"
                                + (inbound.walkTransitInVehicleTime + outbound.walkTransitInVehicleTime));
                logger.info("\t" + c[FWT] + "* ("
                        + inbound.walkTransitFirstWaitTime + "+ "
                        + outbound.walkTransitFirstWaitTime + ")");
                logger.info("\t" + c[XWT] + "* ("
                        + inbound.walkTransitTransferWaitTime + "+"
                        + outbound.walkTransitTransferWaitTime + ")");
                logger.info("\t" + c[WLK] + "*" + "("
                        + inbound.walkTransitWalkTime
                        + outbound.walkTransitWalkTime + ")");
                logger.info("\t" + c[COST_LOW] + "* (("
                        + inbound.walkTransitFare + outbound.walkTransitFare
                        + ")* " + p.inclow + ")");
                logger.info("\t" + c[COST_MED] + "* (("
                        + inbound.walkTransitFare + outbound.walkTransitFare
                        + ")*" + p.incmed + ")");
                logger.info("\t" + c[COST_HI] + "* (("
                        + inbound.walkTransitFare + outbound.walkTransitFare
                        + ") *" + p.inchi + ")");
                logger.info("\t" + c[WALK_TRAN]);
                logger.info("\t" + c[WALK_TRAN_AW_0] + "*" + p.auwk0);
                logger.info("\t" + c[WALK_TRAN_AW_I] + "*" + p.auwk1);
                logger.info("\t" + c[WALK_TRAN_AW_S] + "*" + p.auwk2);
                logger.info("\t" + c[WALK_TRAN_STOPS] + "*" + p.totalStops);
                logger.info("\t" + c[OVT] + "* ("
                        + inbound.transitOvt + "+"
                        + outbound.transitOvt + ")");
            }

               hasUtility=true;
          } else if (trace) {
              logger.info("Walk transit is not available.");
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