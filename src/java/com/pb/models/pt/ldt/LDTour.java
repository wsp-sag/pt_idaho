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

import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;

import java.io.Serializable;

/**
 * A data class to store long distance tours.
 * 
 * @author Erhardt
 * @version 1.0 03/13/2006
 * 
 */
public class LDTour implements Serializable {
    private static final long serialVersionUID = 3;
    public int ID; 
    public PTHousehold hh; 
    public PTPerson person; 
    
    public LDTourPurpose purpose;
	public LDTourPatternType patternType;
    public int partySize;
    public int homeTAZ;
    public LDTourSchedule schedule; 
    public LDTourDestinationType destinationType; 
	public int destinationTAZ;
	public LDTourModeType mode;
    public LDTripModeType tripMode; 
    
    public float outboundTime;
    public float inboundTime; 
    public float distance; 

    public int nearestAirport;
    
    // indicates that is within the bounds of the internal mode choice model
    public boolean modeChoiceHaloFlag; 


    /**
     * Constructor.  
     * 
     * @param id        Tour ID
     * @param h      Household
     * @param p person
     * @param purp      Tour purpose
     * @param pattern   Tour pattern type
     */
    public LDTour(int id, PTHousehold h, PTPerson p, LDTourPurpose purp, LDTourPatternType pattern) {
        this.ID = id;
        this.hh = h; 
        this.person = p;
        this.purpose = purp;
        this.patternType = pattern;
        
        if (purpose.equals(LDTourPurpose.HOUSEHOLD)) {
            partySize = hh.size; 
        } else {
            partySize = 1; 
        }
        
        homeTAZ = hh.homeTaz;

        nearestAirport = 0;
        modeChoiceHaloFlag = false; 
    }

    /**
     * Constructor with default values for use in creating MC Logsums
     *
     */
    public LDTour(LDTourPurpose purp, LDTourPatternType pattern) {

        ID = 0;
        purpose = purp;
        patternType = pattern;
        partySize = 1;
        destinationType = LDTourDestinationType.INTERNAL;
        schedule = new LDTourSchedule(pattern, 8, 17);
        modeChoiceHaloFlag = false; 

    }
}
