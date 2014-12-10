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
import com.pb.models.pt.util.TravelTimeAndCost;
import org.apache.log4j.Logger;

/**  
 * Transit Passenger Mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */


public class TransitPassenger extends Mode {
    static Logger logger = Logger.getLogger(TransitPassenger.class);
    
    static final long serialVersionUID = 666;

//     public boolean isAvailable=true;
//     public boolean hasUtility=false;
//     double utility=0;

     public TransitPassenger(){
         isAvailable = true;
         hasUtility = false;
         utility = 0.0D;
          alternativeName=new String("TransitPassenger");
          type=TourModeType.TRANSITPASSENGER;
     }
     
    /** Calculates utility of transit-passenger mode
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
          if(outbound.sharedRide2Time==0.0) isAvailable=false;
          if(p.tourPurpose==ActivityPurpose.WORK_BASED) isAvailable=false;
     
          if(isAvailable){
               time=(inbound.walkTransitInVehicleTime+outbound.sharedRide2Time
                    +inbound.walkTransitFirstWaitTime
                    +inbound.walkTransitTransferWaitTime
                    +inbound.walkTransitWalkTime
                    +inbound.transitOvt
               );
               utility=(
            c[IVT]*(inbound.walkTransitInVehicleTime+outbound.sharedRide2Time)
            + c[FWT]*inbound.walkTransitFirstWaitTime
            + c[XWT]*inbound.walkTransitTransferWaitTime
            + c[WLK]*inbound.walkTransitWalkTime
            + c[COST_LOW]*((inbound.walkTransitFare+outbound.sharedRide2Cost)*p.inclow)
            + c[COST_MED]*((inbound.walkTransitFare+outbound.sharedRide2Cost)*p.incmed)
            + c[COST_HI]*((inbound.walkTransitFare+outbound.sharedRide2Cost)*p.inchi)
            + c[TRAN_PASS]
            + c[TRAN_PASS_AW_0]*p.auwk0 
            + c[TRAN_PASS_AW_I]*p.auwk1 
            + c[TRAN_PASS_AW_S]*p.auwk2
            + c[TRAN_PASS_STOPS]*p.totalStops
            + c[PASS_H1]*p.size1
            + c[PASS_H2]*p.size2
            + c[PASS_H3]*p.size3p
            + c[OVT]*inbound.transitOvt
               );

              if(utility == Double.NEGATIVE_INFINITY || utility == Double.POSITIVE_INFINITY){
                  utility = -999;
                  isAvailable = false;
              }

              
               if (trace) {
                   logger.info("Transit passenger utility: " + utility + " = ");
                   logger.info(c[IVT]+ "*" +(inbound.walkTransitInVehicleTime+outbound.sharedRide2Time));
                   logger.info("\t" +  c[FWT]+ "*" +inbound.walkTransitFirstWaitTime);
                   logger.info("\t" +  c[XWT]+ "*" +inbound.walkTransitTransferWaitTime);
                   logger.info("\t" +  c[WLK]+ "*" +inbound.walkTransitWalkTime);
                   logger.info("\t" +  c[COST_LOW]+ "*" +((inbound.walkTransitFare+outbound.sharedRide2Cost)*p.inclow));
                   logger.info("\t" +  c[COST_MED]+ "*" +((inbound.walkTransitFare+outbound.sharedRide2Cost)*p.incmed));
                   logger.info("\t" +  c[COST_HI]+ "*" +((inbound.walkTransitFare+outbound.sharedRide2Cost)*p.inchi));
                   logger.info("\t" +  c[TRAN_PASS] );
                   logger.info("\t" +  c[TRAN_PASS_AW_0]+ "*" +p.auwk0 );
                   logger.info("\t" +  c[TRAN_PASS_AW_I]+ "*" +p.auwk1 );
                   logger.info("\t" +  c[TRAN_PASS_AW_S]+ "*" +p.auwk2);
                   logger.info("\t" +  c[TRAN_PASS_STOPS]+ "*" +p.totalStops);
                   logger.info("\t" +  c[PASS_H1]+ "*" +p.size1);
                   logger.info("\t" +  c[PASS_H2]+ "*" +p.size2);
                   logger.info("\t" +  c[PASS_H3]+ "*" +p.size3p);
                   logger.info("\t" +  c[OVT]+ "*" +inbound.transitOvt);

               }
               hasUtility=true;
          } else if (trace) {
              logger.info("Transit passenger is not available.");
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