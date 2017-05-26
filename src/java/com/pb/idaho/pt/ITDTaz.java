/*
 * Copyright 2006 PB Consult Inc.
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
package com.pb.idaho.pt;

import com.pb.common.math.MathUtil;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.StopDestinationParameters;
import com.pb.models.pt.Taz;
import static com.pb.models.pt.TourDestinationParameters.*;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Oct 24, 2006
 * Email: willison@pbworld.com
 * Modified: Ashish Kulshrestha
 * Date: Jan 10, 2015
 * Email: kulshresthaa@pbworld.com
 */
public class ITDTaz extends Taz {

	public void setTourSizeTerms(float[][] tdpd){
        float sizeTerm;
        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){
            float destParams[] = tdpd[i];
            
            sizeTerm = (destParams[HOUSEHOLDS] * households);           			
            sizeTerm += (destParams[RETAIL] * employment.get("sdtRetail"));			
            sizeTerm += (destParams[OTHERSERVICES] * employment.get("sdtOtherServ"));		
            sizeTerm += (destParams[HEALTH] * employment.get("sdtHealth"));				
            sizeTerm += (destParams[TRANSPORTATION] * employment.get("sdtTransportation"));				            
            sizeTerm += (destParams[K12EDUCATION] * employment.get("sdtK12Ed"));
            sizeTerm += (destParams[HIGHEREDUCATION] * employment.get("sdtHigherEd"));
            sizeTerm += (destParams[OTHEREDUCATION] * employment.get("sdtOtherEd"));
            sizeTerm += (destParams[GOVERNMENT] * employment.get("sdtPublicAdmin"));				
            sizeTerm += (destParams[OTHEREMPLOYMENT] * employment.get("sdtOther"));				
            
            tourSizeTerm[i] = sizeTerm;
            
            if(sizeTerm>0)
            	tourLnSizeTerm[i] = MathUtil.log(sizeTerm);
        }
        tourSizeTermsSet = true;
	}
    
	public void setStopSizeTerms(float[][] params){
        float sizeTerm;
        ActivityPurpose[] purposes = ActivityPurpose.values();
        for(int i=0;i<purposes.length;i++){
        	float[] p = params[i];
        	
            sizeTerm = (p[StopDestinationParameters.RETAIL] * employment.get("sdtRetail"))
            		 + (p[StopDestinationParameters.OTHERSERVICES] * employment.get("sdtOtherServ"))
            		 + (p[StopDestinationParameters.HEALTH] * employment.get("sdtHealth"))
            		 + (p[StopDestinationParameters.TRANSPORTATION] * employment.get("sdtTransportation"))
            		 + (p[StopDestinationParameters.K12EDUCATION] * employment.get("sdtK12Ed"))
            		 + (p[StopDestinationParameters.HIGHEREDUCATION] * employment.get("sdtHigherEd"))
            		 + (p[StopDestinationParameters.HIGHEREDUCATION] * employment.get("sdtOtherEd"))
            		 + (p[StopDestinationParameters.GOVERNMENT] * employment.get("sdtPublicAdmin"))
            		 + (p[StopDestinationParameters.OTHEREMPLOYMENT] * employment.get("sdtOther"))
                     + (p[StopDestinationParameters.HHS] * households);
            
            stopSizeTerm[i] = sizeTerm;
            if (sizeTerm > 0)
                stopLnSizeTerm[i] = MathUtil.log(sizeTerm);
        }
        stopSizeTermsSet = true;
	}
}
