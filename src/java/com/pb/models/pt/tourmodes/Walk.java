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
 * Walk mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class Walk extends Mode {
    static Logger logger = Logger.getLogger(Walk.class);
    
    static final long serialVersionUID = 666;
     
 //    public boolean isAvailable=true;
 //    public boolean hasUtility=false;
     
 //    double utility=0;

     public Walk(){
         isAvailable = true;
                hasUtility = false;
                utility = 0.0D;
          alternativeName=new String("Walk");
          type=TourModeType.WALK;
     }

    /** Calculates utility of walk mode
     * 
     * @param inbound - In-bound TravelTimeAndCost
     * @param outbound - Outbound TravelTimeAndCost
     * @param z - ZoneAttributes (Currently only parking cost)
     * @param c - TourModeParameters
     * @param p - PersonTourModeAttributes
     */
    
     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
          @SuppressWarnings("unused")
        ZoneAttributes z,float[] c, TourModePersonAttributes p){

         hasUtility = false;
         utility=-999;
         isAvailable = true;

         
         // Add an aditional condition to allow intraZonal walk longer than 4 miles     
          if((inbound.walkDistance>4) && (inbound.itaz!=inbound.jtaz))
              isAvailable=false;
          //Add an aditional condition to alow intraZonal walk longer than 4 miles    
          if((outbound.walkDistance>4)  && (outbound.itaz!=outbound.jtaz))
              isAvailable=false;

          if(isAvailable){
               time=(inbound.walkTime+outbound.walkTime);
               utility=(
                       c[WALK_TIME]*(inbound.walkTime+outbound.walkTime)
                     + c[WALK]
                     + c[WALK_AW_0]*p.auwk0 
                     + c[WALK_AW_I]*p.auwk1 
                     + c[WALK_AW_S]*p.auwk2
                     + c[WALK_STOPS]*p.totalStops
                );
               
               if (trace) {
                logger.info("walk utility: " + utility + " = ");
                logger.info("\t" + c[WALK_TIME] + "*"
                        + (inbound.walkTime + outbound.walkTime));
                logger.info("\t" + c[WALK]);
                logger.info("\t" + c[WALK_AW_0] + "*" + p.auwk0);
                logger.info("\t" + c[WALK_AW_I] + "*" + p.auwk1);
                logger.info("\t" + c[WALK_AW_S] + "*" + p.auwk2);
                logger.info("\t" + c[WALK_STOPS] + "*" + p.totalStops);
            }
               
               hasUtility=true;
          } else if (trace) {
              logger.info("Walk is not available.");
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