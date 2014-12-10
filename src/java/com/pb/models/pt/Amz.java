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
package com.pb.models.pt;

import java.util.Hashtable;

import org.apache.log4j.Logger;



/** 
 * A data class containing information about the AMZ.  
 * 
 * While an AMZ is defined by the HashMap in the TazByAmz class, 
 * it is sometimes useful to store aggregate information at this
 * level.  For example, the long-distance tour destination choice
 * model uses AMZs as alternatives.  
 * 
 * @author Erhardt
 * @version 1.0 03/13/2006
 */
public class Amz {
	protected static Logger logger = Logger.getLogger(Amz.class);
	
    public int ID;
    protected String name;
    protected Hashtable <Integer, Taz> zones;
    
    /**
     * Default constructor.  
     *
     */
    public Amz() {
    	
    }
    
    /**
     * Sets AMZ name.
     *   
     * @param n The name.  
     */
    public void setName(String n) {
    	name = n; 
    }
    
    /**
     * Sets TAZs contained in the AMZ.  These
     * are stored as a HasSet.  
     *   
     * @param tazs A HashSet with the TAZs contained in this AMZ.    
     */
    public void setTazs(Hashtable<Integer, Taz> tazs) {
    	zones = tazs; 
    }
    
    /**
     *   
     * @return The number of TAZs in this AMZ.      
     */
    public int getNumZones() {
    	return zones.size(); 
    }
    
    

}
