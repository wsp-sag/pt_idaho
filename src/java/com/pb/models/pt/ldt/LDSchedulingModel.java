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

import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.SeededRandom;
import static com.pb.models.pt.ldt.LDSchedulingParameters.*;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * Determines the schedule of a long-distance tour.  The model is different
 * by purpose:  
 * 	 COMPLETE_TOUR - Applies a constants-only logit model based on departure hour
 *                   and arrival hour.  The resulting probabilies are static and do 
 *                   not vary for different tours.    
 *   BEGIN_TOUR    - Draws from a static frequency distribution for departure hour.
 *   END_TOUR      - Draws from a static frequency distribution for arrival hour.
 *   AWAY          - Not relevant.
 *   NO_TOUR       - Not relevant.
 *   
 * @author Erhardt
 * @version 1.0 03/10/2006
 *
 */
public class LDSchedulingModel {
	
    protected static Logger logger = Logger.getLogger(LDSchedulingModel.class);
    protected ResourceBundle rb;  
    private boolean trace = false;
		
    private float[][] completeScheduleParameters; 
    private LogitModel completeSchedulingModel; 
    private ConcreteAlternative[] alts; 

    private float[][] departureArrivalFrequencies; 
    private double[] completeScheduleFrequencies;

    private long ldSchedulingFixedSeed = Long.MAX_VALUE/3;
    
    /**
	 * Constructor reads parameters file and builds the model.   
	 */
	public LDSchedulingModel(ResourceBundle rb){
		this.rb = rb; 
		readParameters(); 
        buildModel(); 
	}
    
    /**
     * Read frequencies and parameters.  
     * 
     */
    private void readParameters() {
        
    	// first read in the arrival and departure frequencies
        logger.info("Reading LD Scheduling Model Frequencies");
        departureArrivalFrequencies = ParameterReader.readParameters(rb,
                "ldt.tour.schedule.frequencies");
        
        // then read the parameters for complete tours
        logger.info("Reading LD Scheduling Model Parameters");
        completeScheduleParameters = ParameterReader.readParameters(rb,
                "ldt.tour.schedule.parameters");   
    }
        

    /**
     * Build the model.
     * 
     * Add alternatives for each valid combination of departure time and 
     * arrival time.  
     */
    private void buildModel() {
        completeSchedulingModel = new LogitModel("LD Tour scheduling model");

        // create a list of all the possible schedules
        int i = 0;
        ArrayList listOfSchedules = new ArrayList(); 
        for (int dep = EARLIESTDEPARTURE; dep <= LATESTDEPARTURE; ++dep) {
            for (int dur = SHORTESTDURATION; dur <= LONGESTDURATION; ++dur) {
            	int arr = dep + dur; 
            	if (arr <= LATESTARRIVAL) {
            		LDTourSchedule schedule = new LDTourSchedule(
							LDTourPatternType.COMPLETE_TOUR, dep, arr);
            		listOfSchedules.add(schedule); 
                	i++;
            	}
            }
        }
        
        // add those schedules as alternatives
        alts = new ConcreteAlternative[listOfSchedules.size()];
        for (i=0; i<listOfSchedules.size(); i++) {
        	LDTourSchedule schedule = (LDTourSchedule) listOfSchedules.get(i);
        	alts[i] = new ConcreteAlternative(i + 
        			": depart " + schedule.departureHour + 
        			" arrive " + schedule.arrivalHour, 
        			schedule);
    		completeSchedulingModel.addAlternative(alts[i]);	
        }        
        
        // These utilities and frequencies are the same for everyone.
        calculateUtility(LDTourPatternType.COMPLETE_TOUR); 
        calculateCompleteScheduleFrequencies(); 
    }
    
    /**
     * Calculate the utility for each complete schedule alternative.  
     * 
     * @return the composite utility.  
     */
    private double calculateUtility(LDTourPatternType type) {
    	
		if(trace){
			logger.info("Calculating utilities for long-distance complete schedules"); 
		}

		for (int i=0; i<alts.length; i++) {
			double util = 0; 
			LDTourSchedule schedule = (LDTourSchedule) alts[i].getAlternative(); 
	
            if (schedule.duration == 2)
                util += completeScheduleParameters[type.ordinal()][DURATION2];
            if (schedule.duration == 3)
                util += completeScheduleParameters[type.ordinal()][DURATION3];
            if (schedule.duration == 4)
                util += completeScheduleParameters[type.ordinal()][DURATION4];
            if (schedule.duration == 5)
                util += completeScheduleParameters[type.ordinal()][DURATION5];
            if (schedule.duration == 6)
                util += completeScheduleParameters[type.ordinal()][DURATION6];
            if (schedule.duration == 7)
                util += completeScheduleParameters[type.ordinal()][DURATION7];
            if (schedule.duration == 8)
                util += completeScheduleParameters[type.ordinal()][DURATION8];
            if (schedule.duration == 9)
                util += completeScheduleParameters[type.ordinal()][DURATION9];
            if (schedule.duration == 10)
                util += completeScheduleParameters[type.ordinal()][DURATION10];
            if (schedule.duration == 11)
                util += completeScheduleParameters[type.ordinal()][DURATION11];
            if (schedule.duration == 12)
                util += completeScheduleParameters[type.ordinal()][DURATION12];
            if (schedule.duration == 13)
                util += completeScheduleParameters[type.ordinal()][DURATION13];
            if (schedule.duration == 14)
                util += completeScheduleParameters[type.ordinal()][DURATION14];
            if (schedule.duration == 15)
                util += completeScheduleParameters[type.ordinal()][DURATION15];
            if (schedule.duration == 16)
                util += completeScheduleParameters[type.ordinal()][DURATION16];
            if (schedule.duration == 17)
                util += completeScheduleParameters[type.ordinal()][DURATION17];
            
            if (schedule.departureHour == 5)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR5];
            if (schedule.departureHour == 6)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR6];
            if (schedule.departureHour == 7)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR7];
            if (schedule.departureHour == 8)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR8];
            if (schedule.departureHour == 9)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR9];
            if (schedule.departureHour == 10)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR10];
            if (schedule.departureHour == 11)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR11];
            if (schedule.departureHour == 12)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR12];
            if (schedule.departureHour == 13)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR13];
            if (schedule.departureHour == 14)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR14];
            if (schedule.departureHour == 15)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR15];
            if (schedule.departureHour == 16)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR16];
            if (schedule.departureHour == 17)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR17];
            if (schedule.departureHour == 18)
                util += completeScheduleParameters[type.ordinal()][DEPARTHOUR18];
            
            // arrival time
            if (schedule.arrivalHour == 5)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR5];
            if (schedule.arrivalHour == 6)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR6];
            if (schedule.arrivalHour == 7)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR7];
            if (schedule.arrivalHour == 8)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR8];
            if (schedule.arrivalHour == 9)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR9];
            if (schedule.arrivalHour == 10)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR10];
            if (schedule.arrivalHour == 11)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR11];
            if (schedule.arrivalHour == 12)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR12];
            if (schedule.arrivalHour == 13)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR13];
            if (schedule.arrivalHour == 14)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR14];
            if (schedule.arrivalHour == 15)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR15];
            if (schedule.arrivalHour == 16)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR16];
            if (schedule.arrivalHour == 17)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR17];
            if (schedule.arrivalHour == 18)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR18];
            if (schedule.arrivalHour == 19)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR19];
            if (schedule.arrivalHour == 20)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR20];
            if (schedule.arrivalHour == 21)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR21];
            if (schedule.arrivalHour == 22)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR22];
            if (schedule.arrivalHour == 23)
                util += completeScheduleParameters[type.ordinal()][ARRIVEHOUR23];

            if( trace && logger.isDebugEnabled()) {
				logger.debug("Calculating utility for complete schedule alternative "+i);
				logger.debug("  The departure hour is " + schedule.departureHour); 
				logger.debug("  The duration is " + schedule.duration);
				logger.debug("  The utility is " + util);
			}
			
	    	completeSchedulingModel.getAlternative(i).setUtility(util);
		}
         
		// return the total utility (logsum)
        double compUtility = completeSchedulingModel.getUtility();
        if (trace) {
            logger.info("  Composite utility: " + compUtility);
        }
        return compUtility;
    }
    
    
    /**
	 * Calculates the frequency of each alternative, and stores it in 
	 * completeScheduleFrequencies.  
	 * 
	 * Calculate utilities before calling this method.  
	 * 
	 */
    private void calculateCompleteScheduleFrequencies() {
    	completeSchedulingModel.calculateProbabilities(); 
        completeScheduleFrequencies = new double[alts.length];
    	completeScheduleFrequencies = completeSchedulingModel.getProbabilities(); 
    }


	/**
	 * Simulates the choice of a schedule for the given long- distance tour
	 * 
	 * @param tour The long-distance tour under consideration.
	 * @return the chosen LD tour schedule.  
	 */
    public LDTourSchedule chooseSchedule(LDTour tour, boolean sensitivityTestingMode) {
    	LDTourSchedule chosenSchedule; 
    	long seed = tour.hh.ID*100 + tour.person.memberID + tour.ID + ldSchedulingFixedSeed;
        if(sensitivityTestingMode) seed += System.currentTimeMillis();

        Random random = new Random();
        random.setSeed(seed);

        // both departure and arrival
    	if (tour.patternType.equals(LDTourPatternType.COMPLETE_TOUR)) {
    		int choice = chooseFromFrequencies(completeScheduleFrequencies, random.nextDouble());
    		LDTourSchedule chosenAlt = (LDTourSchedule) alts[choice].getAlternative();
            chosenSchedule = null;
            try {
                chosenSchedule = chosenAlt.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    	
    	// departure only
    	else if (tour.patternType.equals(LDTourPatternType.BEGIN_TOUR)) {
    		int departHour = chooseFromFrequencies(
                    departureArrivalFrequencies[LDTourPatternType.BEGIN_TOUR.ordinal()],random.nextDouble());
    		chosenSchedule = new LDTourSchedule(tour.patternType, departHour);    		
    	}
    	
    	// arrival only 
    	else if (tour.patternType.equals(LDTourPatternType.END_TOUR)) {
    		int arriveHour = chooseFromFrequencies(
                    departureArrivalFrequencies[LDTourPatternType.END_TOUR.ordinal()],random.nextDouble());
    		chosenSchedule = new LDTourSchedule(tour.patternType, arriveHour);
    	}
    	
    	// out of area or no LD travel 
    	else {
    		chosenSchedule = new LDTourSchedule(tour.patternType, -1, -1); 
    		
    		if(trace) logger.info("No long-distance tours scheduled");
    	}
        
        // trace the results
        if (trace) {
            logger.info("    The chosen schedule for HH " + tour.hh.ID
                    + " person " + tour.person.memberID + " tour " + tour.ID
                    + " is departure: " + chosenSchedule.departureHour
                    + " arrival: " + chosenSchedule.arrivalHour + " duration: "
                    + chosenSchedule.duration);
        }
    	
    	return chosenSchedule; 
    }
    
	/**
	 * Draws a random number and monte carlo simulation chooses pattern for day.
	 * 
	 * @param frequencies  The choice frequencies.
	 * @return The index of the simulated choice.
	 */
	private int chooseFromFrequencies(float[] frequencies, double random){
		
        double[] doubleFrequencies = new double[frequencies.length];
        for (int i=0; i<frequencies.length; i++) {
            doubleFrequencies[i] = (double) frequencies[i];
        }
              
        return chooseFromFrequencies(doubleFrequencies, random);
	}

    /**
     * Draws a random number and monte carlo simulation chooses pattern for day.
     * 
     * @param frequencies  The choice frequencies.
     * @return The index of the simulated choice.
     */
    private int chooseFromFrequencies(double[] frequencies, double random) {
        
        double culmFreq=0;
        int chosen = -1; 
        for(int i=0;i<frequencies.length;++i){
            culmFreq += frequencies[i];
            if(random<culmFreq){
                chosen = i;
                break;
            }
        }
        
        if (chosen == -1) {
            logger.fatal("Error: No pattern chosen");
            throw new RuntimeException();   
        }
        
        return chosen;      
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
