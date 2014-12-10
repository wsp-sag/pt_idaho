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

import com.pb.common.model.ModelException;
import com.pb.models.pt.Mode;
import static com.pb.models.pt.TripModeParameters.*;
import com.pb.models.pt.TripModePersonAttributes;
import com.pb.models.pt.TripModeType;
import com.pb.models.pt.util.TravelTimeAndCost;
import org.apache.log4j.Logger;

/**
 * Driver alone mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class WalkTrip extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.idaho.pt.default");
     public WalkTrip(){
          alternativeName=new String("WalkTrip");
          type=TripModeType.WALK;
          isAvailable = true;
          hasUtility = false;
          utility = 0.0D;
     }
     /** Calculates utility of walk mode
      * 
      * @param travelTimeAndCost - TravelTimeAndCost
      * @param c - TripModeParameters
      * @param p - PersonTripModeAttributes
      */
     
      public void calcUtility(TravelTimeAndCost travelTimeAndCost,   float[] c, 
              TripModePersonAttributes p){

          hasUtility = false;
          utility=-999;
          isAvailable = true;

          // Add an aditional condition to alow intraZonal walk longer than 4 miles     
           if((travelTimeAndCost.walkDistance>4) && (p.originTaz != p.destinationTaz))
               isAvailable=false;
 
           if(isAvailable){
                time= travelTimeAndCost.walkTime;
                 
                utility=(
                        c[WLK]*(travelTimeAndCost.walkTime)
                      + c[PASSWALK]*p.passengerLeg
                      + c[WALKTRANWALK]*p.transitLeg );
                
                if (trace) {
                    logger.info("walk utility: " + utility + " = ");
                    logger.info("\t" + c[WLK] + " * " + travelTimeAndCost.walkTime );
                    logger.info("\t" + c[PASSWALK] + " * " + p.passengerLeg);
                    logger.info("\t" + c[WALKTRANWALK] + " * " + p.transitLeg);
                 }
                
                hasUtility=true;
           } else if (trace) {
               logger.info("Walk trip is not available.");
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

