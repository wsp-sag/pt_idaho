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

/**
 * A class that contains parameters for the Auto Ownership Model
 * 
 * @author Joel Freedman
 *  modified by Greg Erhardt to implement correct OSMP model parameters 
 * @date 3.7.2006
 */
public class AutoOwnershipModelParameters {
    // Be sure these are updated!
    public static final int ALTERNATIVES = 4;

    // PARAMETERS includes ASCs - used in testing
    public static final int PARAMETERS = 14;
    
    public static final int ALTNAME  = 0; 
	public static final int CONSTANT = 1; 
	public static final int HHSIZE1  = 2; 
	public static final int HHSIZE2  = 3; 
	public static final int HHSIZE3  = 4; 
	public static final int INCOME1  = 5; 
	public static final int INCOME2  = 6; 
	public static final int INCOME3  = 7; 
	public static final int INCOME4  = 8; 
	public static final int WORKERS0 = 9; 
	public static final int WORKERS1 = 10;
	public static final int WORKERS2 = 11; 
	public static final int WORKERS3 = 12; 
	public static final int DCLOGSUM = 13;
    public static final int DAYPARK  = 14; 

}
