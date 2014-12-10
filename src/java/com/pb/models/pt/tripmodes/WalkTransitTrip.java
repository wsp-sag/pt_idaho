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
package com.pb.models.pt.tripmodes;

import static com.pb.models.pt.TripModeParameters.*;
import com.pb.common.model.ModelException;
import com.pb.models.pt.Mode;
import com.pb.models.pt.TripModePersonAttributes;
import com.pb.models.pt.TripModeType;
import com.pb.models.pt.util.TravelTimeAndCost;

import org.apache.log4j.Logger;
/** 
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.2 5/01/2006
 * 
 */
public class WalkTransitTrip extends Mode {
    
    final static Logger logger = Logger.getLogger("com.pb.idaho.pt.default");
     public WalkTransitTrip(){
         isAvailable = true;
          alternativeName=new String("WalkTransitTrip");
          hasUtility = false;
          utility = 0.0D;
         type=TripModeType.WK_TRAN;
     }
     
     /** Calculates utility of walk-transit mode
      * @param c - TripModeParameters
      * @param p - PersonTripModeAttributes
      */
      public void calcUtility(TravelTimeAndCost travelTimeAndCost,  float[] c, TripModePersonAttributes p){

          hasUtility = false;
          utility=-999;
          isAvailable = true;

          if(travelTimeAndCost.walkTransitInVehicleTime==0.0) isAvailable=false;
           
           if(isAvailable){
                time= (travelTimeAndCost.walkTransitInVehicleTime
                  + travelTimeAndCost.walkTransitFirstWaitTime
                  + travelTimeAndCost.walkTransitTransferWaitTime
                  + travelTimeAndCost.walkTransitWalkTime
                  + travelTimeAndCost.transitOvt);

                utility=(
               c[IVT]* travelTimeAndCost.walkTransitInVehicleTime
             + c[FWT]* travelTimeAndCost.walkTransitFirstWaitTime
             + c[XWT]* travelTimeAndCost.walkTransitTransferWaitTime
             + c[WLK]* travelTimeAndCost.walkTransitWalkTime
             + c[OPCPAS]* travelTimeAndCost.walkTransitFare
             + c[OVT]* travelTimeAndCost.transitOvt);

               if(utility == Double.POSITIVE_INFINITY || utility == Double.NEGATIVE_INFINITY){
                   utility = -999;
                   isAvailable = false;
               }
                
                if (trace) {
                    logger.info("Walk-transit trip utility: " + utility);
                 logger.info("\t" + c[IVT] + " * "+ travelTimeAndCost.walkTransitInVehicleTime);
                 logger.info("\t" + c[FWT] + " * " + travelTimeAndCost.walkTransitFirstWaitTime  );
                 logger.info("\t" + c[XWT] + " * " + travelTimeAndCost.walkTransitTransferWaitTime );
                 logger.info("\t" + c[WLK] + " * " + travelTimeAndCost.walkTransitWalkTime );
                 logger.info("\t" + c[OPCPAS] + " * " + travelTimeAndCost.walkTransitFare );
                 logger.info("\t" + c[OVT] + " * " + travelTimeAndCost.transitOvt );
              }

                hasUtility=true;
           } else if (trace) {
               logger.info("Walk transit trip mode is not available.");
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

