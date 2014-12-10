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

import org.apache.log4j.Logger;


/**
 * A data class to store long distance trips.  
 * 
 * @author Erhardt
 * @version 1.0 May 12, 2006
 *
 */
public class LDTrip {
    protected final static Logger logger = Logger.getLogger(LDTrip.class);
    
    int hhID;
    int personID;
    int tourID;  
    public LDTour tour; 
    public LDTourPurpose purpose;
    public LDTourModeType mode;
    public LDTourDestinationType destinationType;
    public LDTourPatternType patternType;
    int origin;
    int destination;
    float distance;
    float time;
    int tripTimeOfDay;
    public int partySize;
    public int nearestAirport;
    public LDTripModeType tripMode; 

    // flag indicating if the tour involves an auto driver
    public boolean vehicleTrip;

    // flag indicating if the trip is in the outbound direction
    public boolean outboundTrip;   
    
    // indicates that is within the bounds of the internal mode choice model
    public boolean modeChoiceHaloFlag; 

    /**
     * Default constructor.  
     */
    public LDTrip() {
        
    }
    
    /**
     * Creates a trip from a tour.  Can only create either an outbound
     * or an inbound trip at one time, so if the tour is a complete tour,
     * this method must be called twice.  
     * 
     * @param tour The tour from which to create the trip.  
     * @param type Either BEGIN_TOUR or END_TOUR.  
     */
    public LDTrip(LDTour tour, LDTourPatternType type) {
        hhID            = tour.hh.ID;
        personID        = tour.person.memberID;
        tourID          = tour.ID;
        this.tour       = tour; 
        purpose         = tour.purpose;
        mode            = tour.mode;
        destinationType = tour.destinationType;
        patternType     = tour.patternType;
        partySize       = tour.partySize;
        distance        = tour.distance;
        nearestAirport  = tour.nearestAirport;
        modeChoiceHaloFlag = tour.modeChoiceHaloFlag; 
        tripMode        = tour.tripMode; 
        
        if (tour.mode.equals(LDTourModeType.AUTO) || tour.mode.equals(LDTourModeType.AIR)) {
            vehicleTrip = true; 
        } else {
            vehicleTrip = false; 
        }
                               
        if (type.equals(LDTourPatternType.BEGIN_TOUR)) {
            origin      = tour.homeTAZ;
            destination = tour.destinationTAZ;
            time          = tour.outboundTime;
            tripTimeOfDay = tour.schedule.getDepartureMilitaryTime(); 
            outboundTrip  = true;
        }
        else if (type.equals(LDTourPatternType.END_TOUR)) {
            origin      = tour.destinationTAZ;
            destination = tour.homeTAZ;
            time        = tour.inboundTime;
            tripTimeOfDay = tour.schedule.getArrivalMilitaryTime();   // arrival time for returning tours
            outboundTrip  = false;
        }
        else {
            logger.error("Invalid tour pattern type when creating trip from tour " + tour.ID);
            logger.error("  Can only create a trip for inbound or outbound, not both.");
        }
      
    }

}
