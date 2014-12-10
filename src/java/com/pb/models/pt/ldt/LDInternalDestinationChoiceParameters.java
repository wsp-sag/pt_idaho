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
 * A class that contains Internal Destination Choice Parameters for
 * long distance tours.  
 * 
 * @author Greg Erhardt
 * @version 1.0 03/23/2006
 */
public class LDInternalDestinationChoiceParameters {
		    
    // the utility terms
    public static final int MCLOGSUM = 0;
    public static final int TIMEIFCOMPLETETOUR = 1; 

    // the size terms
    public static final int SIZEHOUSEHOLDS = 2;
    public static final int SIZETOTEMP = 3;
    public static final int SIZEGOVTEMP = 4;
    public static final int SIZESAMEINDUSTRYEMP = 5;
    public static final int SIZEHOTELEMPIFOVERNIGHT = 6;
    public static final int SIZEHIGHEREDEMPIFCOL = 7;
    
    // calibration terms
    public static final int DISTANCE0TO60 = 8;
    public static final int DISTANCE60TO70 = 9;
    public static final int DISTANCE70TO150 = 10;

}
