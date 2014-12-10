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
 *   Created on Feb 22, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt.ldt;

import com.pb.common.model.ModelException;


/**
 * A data class enumerating the possible types of long-distance tours.  
 *   
 * @author Erhardt
 * @version 1.0 03/13/2006
 *
 */
public enum LDTourPatternType {
	   COMPLETE_TOUR,
	   BEGIN_TOUR,
	   END_TOUR,
	   AWAY,
	   NO_TOUR;
       
        /** converts a pattern type string into a pattern type
         * 
         * @param  type type string
         * @return pattern type
         */
        public static LDTourPatternType getType(String type){

            if (type.equals("COMPLETE_TOUR")) return LDTourPatternType.COMPLETE_TOUR;
            if (type.equals("BEGIN_TOUR")) return LDTourPatternType.BEGIN_TOUR;
            if (type.equals("END_TOUR")) return LDTourPatternType.END_TOUR;
            if (type.equals("AWAY")) return LDTourPatternType.AWAY;
            if (type.equals("NO_TOUR")) return LDTourPatternType.NO_TOUR;        

            // default
            return LDTourPatternType.NO_TOUR; 
       }

        public static LDTourPatternType getType(int index) {

            if (index==LDTourPatternType.COMPLETE_TOUR.ordinal()) return LDTourPatternType.COMPLETE_TOUR;
            else if (index==LDTourPatternType.BEGIN_TOUR.ordinal()) return LDTourPatternType.BEGIN_TOUR;
            else if (index==LDTourPatternType.END_TOUR.ordinal()) return LDTourPatternType.END_TOUR;
            else if (index==LDTourPatternType.AWAY.ordinal()) return LDTourPatternType.AWAY;
            else if (index==LDTourPatternType.NO_TOUR.ordinal()) return LDTourPatternType.NO_TOUR;
            else throw new ModelException("Invalid tour pattern index code: " + index);
        }
}
