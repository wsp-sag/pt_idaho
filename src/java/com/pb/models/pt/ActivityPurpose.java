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
package com.pb.models.pt;

import com.pb.common.model.ModelException;

import java.io.Serializable;

/**
 * Defines activity purposes.
 *
 * @author    Steve Hansen
 * @version   1.0 12/01/2003
 * 
 */
public enum ActivityPurpose implements Serializable {
      
	HOME,
    WORK,
    WORK_BASED,
    GRADESCHOOL,
    COLLEGE,
    SHOP,
    RECREATE,
    OTHER;
    
    /** converts an activity purpose character to an ActivityPurpose
     * 
     * @param activity activity character 
     * @return activity purpose number
     */
    public static ActivityPurpose getActivityPurpose(char activity){

    	switch(activity){
    		case 'w':
    			return ActivityPurpose.WORK;
    		case 'b':
    		 	return ActivityPurpose.WORK_BASED;
    		case 'c':
    			return ActivityPurpose.COLLEGE;
            case 'g':
                return ActivityPurpose.GRADESCHOOL;
    		case 's':
    			return ActivityPurpose.SHOP;
    		case 'r':
    			return ActivityPurpose.RECREATE;
    		case 'o':
    			return ActivityPurpose.OTHER;
    		default:
    			return ActivityPurpose.HOME;
    	}
   }
    
    /**
     * Convert the Enum to its String code.
     * @param activity Activity
     * @return String Activity String
     */
    public static String getActivityString(ActivityPurpose activity) {
     switch(activity) {
     case COLLEGE:
         return "c";
     case GRADESCHOOL:
         return "g";
     case HOME:
         return "h";
     case OTHER:
         return "o";
     case RECREATE:
         return "r";
     case SHOP:
         return "s";
     case WORK:
         return "w";
     case WORK_BASED:
         return "b";
     }
     
     throw new ModelException("Unkown ActivityPurpose");
    }

}