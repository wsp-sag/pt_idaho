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

import com.pb.common.matrix.Matrix;
import com.pb.common.util.SeededRandom;
import com.pb.models.pt.Taz;
import com.pb.models.pt.TazManager;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

/** 
 * Extends the Amz class to include size terms for long-distance
 * destination choice.  
 * 
 * @author Erhardt
 * @version 1.0 03/14/2006
 * @todo update size term with correct parameters.  
 */
public class LDAmz implements Serializable {
	
    private static final long serialVersionUID = 1L;
    protected static Logger logger = Logger.getLogger(LDAmz.class);
    private boolean trace = false;
	    	
    // override the zones data item with LDTazs
    protected Hashtable <Integer, LDTaz> zones; 
    
    // for only the available TAZs
    private HashSet <Integer> availableZoneNumbers; 
    protected int ID;
    private int originZone; 
    
	/** 
	 * Default constructor.  
	 *  
	 */	
	public LDAmz(){  
		
        availableZoneNumbers = null; 
        originZone = 0;
	}

    /** 
     * Sets the included zones and parameters.  
     *  
     */ 
    public LDAmz(int id, HashSet<Integer> zoneIDs, TazManager zoneData, float[][] params){  
        this.ID = id; 
        this.availableZoneNumbers = null; 
        this.originZone = 0; 
        this.trace = false;
        
        this.zones = new Hashtable<Integer, LDTaz>();
        Hashtable<Integer, Taz> allTazs = zoneData.getTazDataHashtable();
        
        Iterator<Integer> zoneIDit = zoneIDs.iterator(); 
        while (zoneIDit.hasNext()) {
            Integer zoneID = zoneIDit.next();            
            Taz zone = allTazs.get(zoneID);
            if (zone==null) {
                logger.error("TazManager does not have zone with ID " + zoneID); 
            }          
            
            LDTaz ldZone = new LDTaz(zone); 
            ldZone.setParameters(params);
            ldZone.setTrace(trace); 
            zones.put(zoneID, ldZone);
        }
    }
    
    /** 
     * As an efficiency feature, certain values are stored, such that it will 
     * not be re-calculated for a specific household and tour.  
     * 
     * This method re-initializes those values, and should be applied between
     * each tour.  
     *
     */
    public void reset() {
        
        if (trace && logger.isDebugEnabled()) {
            logger.debug("Resetting stored LD AMZ values");
        }
        
        availableZoneNumbers = null; 
        originZone = 0; 
        
        Enumeration<Integer> zoneEnum = zones.keys(); 
        while (zoneEnum.hasMoreElements()) {
            Integer zoneID = zoneEnum.nextElement(); 
            LDTaz zone = zones.get(zoneID);
            zone.reset(); 
        }  
    }
    
    /** 
     * Creates a HashSet of available zones beyond the threshold distance
     * from the origin zone.  
     * 
     *   @param distance The zone-zone distance matrix.
     *   @param threshold The threshold -beyond- which to include zones.
     *   @param origin The origin zone.  
     *   
     *   @return The number of zones available.  
     */ 
    public int setAvailableZones(Matrix distance, double threshold, int origin) {

        // if the origin hasn't changed, don't process more
        if (origin==originZone) return availableZoneNumbers.size();
        
        // otherwise, do the calculations
        availableZoneNumbers = new HashSet<Integer>(); 
                
        Enumeration<Integer> zoneEnum = zones.keys(); 
        while (zoneEnum.hasMoreElements()) {
            Integer destination = zoneEnum.nextElement(); 
            float dist = distance.getValueAt(origin, destination);
            if (dist > threshold) {
                availableZoneNumbers.add(destination); 
            }            
        }     
        
        if (trace && logger.isDebugEnabled()) {
            logger.debug("Considering AMZ " + this.ID);
            logger.debug("  Determining zones beyond " + threshold + " miles");
            logger.debug("  from zone" + origin);
            logger.debug("  Number available: " + availableZoneNumbers.size());
        }
        
        originZone = origin;
        return availableZoneNumbers.size(); 
    }
    
	
    /** 
     * Add up the size terms for those zones that are available.  
     * 
     * @param hha Household attributes for the traveler.
     * @param tour The long-distance tour of interest.  
     *
     * @return The size term for the available zones.  
     *  
     */ 
    public double calculateLDSize(LDInternalDestinationChoicePersonAttributes hha, LDTour tour) {
        
        if (tour.homeTAZ !=originZone) {
            logger.error("Must set availabilities for origin "+tour.homeTAZ +"!");
            throw new RuntimeException("LDAmz availabilities not set!");
        }
        if (availableZoneNumbers.size()==0) {
        	logger.info("reached here 3");
        	return 0; 
        }
        
        double total = 0;         
        Iterator<Integer> zoneIter = availableZoneNumbers.iterator();
        while (zoneIter.hasNext()) {
            int zoneID = zoneIter.next();
            LDTaz zone = zones.get(zoneID);
            double size = zone.calculateLDSize(hha, tour.purpose);
            total += size; 
        }
        
        if (trace && logger.isDebugEnabled()) {
            logger.debug("Considering AMZ " + this.ID);
            logger.debug("  Total size: " + total);
        }
        
        return total; 
    }
    
    /** 
     * Chooses a TAZ from among the available zones, with the probabilities
     * proportional to the size terms.  
     * 
     * @param hha Household attributes for the traveler.
     * @param tour The long-distance tour of interest.
     * 
     * @return The ID of the chosen TAZ.   
     */ 
    public int chooseDestination(LDInternalDestinationChoicePersonAttributes hha, LDTour tour, double random) {
        
        if (tour.homeTAZ !=originZone) {
            logger.error("Must set availabilities for origin "+tour.homeTAZ +"!");
            throw new RuntimeException("LDAmz availabilities not set!");
        }
        
        double totalSize = calculateLDSize(hha, tour);
        double culmFreq=0;
        int chosenTaz = 0; 
        
        Iterator<Integer> zoneIter = availableZoneNumbers.iterator();
        while (zoneIter.hasNext()) {
            int zoneID = zoneIter.next();
            LDTaz zone = zones.get(zoneID);
            double size = zone.calculateLDSize(hha, tour.purpose);
            double probability = size / totalSize; 
            culmFreq += probability;
            if(random<culmFreq){
                chosenTaz = zoneID;
                break;
            }
        }
                
        // Make sure a pattern was chosen
        if(chosenTaz==0){
            logger.fatal("Error:No TAZ chosen");
            throw new RuntimeException("Error:No TAZ chosen");
        }
        
        if (trace && logger.isDebugEnabled()) {
            logger.debug("Choosing TAZ from AMZ " + this.ID);
            logger.debug("  Chosen TAA: " + chosenTaz);
        }
        
        return chosenTaz; 
    }
	
    /**
     * Set the trace option.
     * 
     * The trace option is set to false by default. The verbosity of trace
     * output is constrolled through the info and debug logger levels.
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }
}
