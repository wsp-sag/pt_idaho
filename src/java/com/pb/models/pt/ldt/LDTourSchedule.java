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
package com.pb.models.pt.ldt;

/**
 * A data class to store the schedules of long-distance tours.
 * 
 * @author Erhardt
 * @version 1.0 03/13/2006
 * 
 */
public class LDTourSchedule implements Cloneable {

	public LDTourPatternType patternType;

	public int departureHour;
	public int arrivalHour;
	public int duration;

	/**
	 * Default constructor.
	 */
	public LDTourSchedule() {

	}

	/**
	 * Constructor for use with complete tours.
	 * 
	 * @param pattern
	 *            Type of pattern for this tour.
	 * @param departure
	 *            Depature hour, from 0 (midnight) to 23 (11 pm).
	 * @param arrival
	 *            Arrival hour, from 0 (midnight) to 23 (11 pm).
	 */
	public LDTourSchedule(LDTourPatternType pattern, int departure, int arrival) {
		patternType = pattern;
		if (patternType.equals(LDTourPatternType.COMPLETE_TOUR)) {
			departureHour = departure;
			arrivalHour = arrival;
			duration = arrivalHour - departureHour;
		} else if (patternType.equals(LDTourPatternType.BEGIN_TOUR)) {
			departureHour = departure;
			arrivalHour = -1;
			duration = -1;
		} else if (patternType.equals(LDTourPatternType.END_TOUR)) {
			departureHour = -1;
			arrivalHour = arrival;
			duration = -1;
		} else {
			departureHour = -1;
			arrivalHour = -1;
			duration = -1;
		}

		checkData();
	}

	/**
	 * Constructor for use with beginning or ending tours.
	 * 
	 * @param pattern
	 *            Type of pattern for this tour, should be begin or end.
	 * @param hour
	 *            Depature or arrival hour, from 0 (midnight) to 23 (11 pm).
	 */
	public LDTourSchedule(LDTourPatternType pattern, int hour) {
		if (pattern.equals(LDTourPatternType.BEGIN_TOUR)) {
			departureHour = hour;
			arrivalHour = -1;
			duration = -1;
		} else if (pattern.equals(LDTourPatternType.END_TOUR)) {
			departureHour = -1;
			arrivalHour = hour;
			duration = -1;
		} else {
			departureHour = -1;
			arrivalHour = -1;
			duration = -1;
		}

		checkData();
	}

	/**
	 * Creates a copy of the object.  
	 * 
	 * @return a clone of this object.  
	 */
	public LDTourSchedule clone() throws CloneNotSupportedException {
        super.clone();
        return new LDTourSchedule(this.patternType,
				this.departureHour, this.arrivalHour);
	}

	/**
	 * Checks that the times in the schedule are valid.
	 * 
	 */
	private void checkData() {
		if (departureHour < -1 || departureHour > 23) {
			throw new RuntimeException("Departure hour (" + departureHour
					+ ") is invalid!");
		}
		if (arrivalHour < -1 || arrivalHour > 23) {
			throw new RuntimeException("Arrival hour (" + departureHour
					+ ") is invalid!");
		}
		if (departureHour > arrivalHour && arrivalHour > -1) {
			throw new RuntimeException(
					"Departure cannot be later than arrival! (" + departureHour
							+ ">" + arrivalHour + ")");
		}
		if (duration != (arrivalHour - departureHour) && duration > -1
				& arrivalHour > -1 & departureHour > -1) {
			throw new RuntimeException("Duration calculated improperly!");
		}
	}

    /**
     * @return Returns the arrival in military.
     */
    public int getArrivalMilitaryTime() {
        if (arrivalHour == -1) return -1;   
        else return arrivalHour * 100;
    }

    /**
     * @return Returns the departure in military time.
     */
    public int getDepartureMilitaryTime() {
        if (departureHour == -1) return -1; 
        else return departureHour * 100;
    }

    /**
     * @return Returns the duration in military time.
     */
    public int getDurationMilitaryTime() {
        if (duration == -1) return -1; 
        else return duration * 100;
    }
    
    /** 
     * @return Tour duration, in minutes.
     */
    public int getDurationMinutes() {
        if (duration == -1) return -1;
        else return duration * 60;
    }
    
    /**
     * Converts a time, in minutes, to military time.  
     * 
     * @param minutes The time, in minutes, to convert to military.
     * @return The military time.
     */
    public static int convertToMilitaryTime(int minutes) {
        
        int leftoverMinutes = minutes % 60; 
        int timeHours = (minutes - leftoverMinutes) / 60;
        return timeHours * 100 + leftoverMinutes;
    }
}
