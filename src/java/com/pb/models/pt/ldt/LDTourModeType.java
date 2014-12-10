/*
 * Copyright  2006 PB Consult Inc.
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
package com.pb.models.pt.ldt;

/**
 * Modes for use with long-distance tours.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 5, 2006
 *
 */
public enum LDTourModeType {
    AUTO, 
    AIR, 
    TRANSIT_WALK;
//    TRANSIT_DRIVE,
//    HSR_WALK,
//    HSR_DRIVE;
    
    /**
     * 
     * @return The name of the group, used in matrix naming conventions.
     */
    public String getGroupName() {
        if (this.equals(AUTO))  return "Car";
        else if (this.equals(AIR))   return "Air";
        else if (this.equals(TRANSIT_WALK))   return "Icwt";
        //else if (this.equals(TRANSIT_DRIVE)) return "Icdt";
        //else if (this.equals(HSR_WALK)) return "IcRwt";
        //else if (this.equals(HSR_DRIVE)) return "IcRdt";
        else return "none";
    }


    /**
     * 
     * @return The list of tables for skims of this type.  
     */
    public String[] getTableLabels() {
        if (this.equals(AUTO)) {
            return new String[]{"Time", "Dist", "Toll"};
        }
        else if (this.equals(AIR)) {
            return new String[]{"Ivt", "Far", "Fwt", "Drv"};
        }
        else if (this.equals(TRANSIT_WALK)) {
            return new String[]{"Ivt", "Far", "Fwt", "Twt", "Awk", "Xwk", "Ewk"};
        }
//        else if (this.equals(HSR_WALK)) {
//            return new String[]{"Ivt", "Riv", "Far", "Fwt", "Twt", "Awk", "Xwk", "Ewk", "Frq"};
//        }
//        else if (this.equals(TRANSIT_DRIVE)) {
//            return new String[]{"Ivt", "Biv", "Far", "Fwt", "Twt", "Xwk", "Drv"};
//        }
//        else if (this.equals(HSR_DRIVE)) {
//            return new String[]{"Ivt", "Riv", "Far", "Fwt", "Twt", "Xwk", "Drv", "Frq"};
//        }
        else {
            return new String[]{"none"};
        }
    }
    
}
