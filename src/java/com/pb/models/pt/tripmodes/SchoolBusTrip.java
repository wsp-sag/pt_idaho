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
/** 
 * School Bus mode
 * 
 * @author Ofir Cohen
 * @version 1.0 4/24/2007
 *  
 */

package com.pb.models.pt.tripmodes;

import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.Mode;
import com.pb.models.pt.TourModeType;
import com.pb.models.pt.TripModeParameters;
import com.pb.models.pt.TripModeType;


import org.apache.log4j.Logger;
public class SchoolBusTrip extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.idaho.pt.default");
    public SchoolBusTrip(){
        isAvailable = true;
        hasUtility = false;
        utility = 0.0D;
          alternativeName=new String("SchoolBus");
          type=TripModeType.SCHOOL_BUS;
     }
    
    public void calcUtility(float[] p,  Mode tourMode, ActivityPurpose tourPurpose){

    hasUtility = false;
    utility=-999;
    isAvailable = true;
    
    if(tourMode.type!=TourModeType.AUTOPASSENGER)
        isAvailable=false;
    if(tourPurpose!= ActivityPurpose.GRADESCHOOL)
        isAvailable=false;
    if(isAvailable){
        utility=
       p[TripModeParameters.SCHOOLBUS];       
        hasUtility=true;
   }
    
    }
    
    public double getUtility(){
        if(!hasUtility){
             logger.fatal("Error: Utility not calculated for "+alternativeName+"\n");
            //TODO - log this error to the node exception file
             System.exit(1);
        };
        return utility;
   };
}

