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

import com.pb.common.model.ModelException;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.util.Random;
import java.util.ResourceBundle;


/**
 * If an LD tour occurs in a two-week period, determines the pattern of travel
 * on the simulated day based on static frequencies. The model is applied
 * separately for each purpose. The available patterns are: COMPLETE_TOUR -
 * round-trip LD tour. BEGIN_TOUR - departing on LD tour. END_TOUR - returnin
 * from LD tour. AWAY - away on LD travel during simulation day. NO_TOUR - no LD
 * tour on simulation day.
 * 
 * @author Erhardt
 * @version 1.0 03/10/2006
 * 
 */
public class LDPatternModel {

    protected static Logger logger = Logger.getLogger(LDPatternModel.class);

    protected ResourceBundle rb;

    private boolean trace = false;

    private Tracer tracer = Tracer.getTracer();

    private float[][] frequencies;

    private long ldPatternFixedSeed = Long.MIN_VALUE/3;

    /**
     * Constructor reads parameters file and builds the model.
     */
    public LDPatternModel(ResourceBundle rb) {
        this.rb = rb;
        readParameters();
        buildModel();
    }
    
    /**
     * Read parameters from file specified in properties.
     * 
     */
    private void readParameters() {

        logger.info("Reading LD Tour Pattern Model Parameters");
        frequencies = ParameterReader.readParameters(rb,
                "ldt.pattern.model.frequencies");

        if (tracer.isTraceOn()) {
            logger.info("Read the following frequencies, down by purpose:");

            for (int i = 0; i < frequencies.length; ++i) {
                String line = "parameter " + i;
                for (int j = 0; j < frequencies[0].length; ++j) {
                    line += "," + frequencies[i][j];
                }
                logger.info(line);
            }
        }

    }

    /**
     * Build model currently has no functionality for the long-distance
     * pattern model.  
     */
    private void buildModel() { 

    }

    /**
     * Draws a random number and monte carlo simulation chooses pattern for day.
     * 
     * @param frequencies  The choice frequencies for the pattern types.
     * @return The chosen pattern for the simulated day.
     */
    private LDTourPatternType choosePatternFromFrequency(float[] frequencies, double random) {

	double culmFreq = 0;
	LDTourPatternType chosenPattern = null;
	LDTourPatternType[] patterns = LDTourPatternType.values();
	for (int i = 0; i < frequencies.length; ++i) {
	    culmFreq += frequencies[i];
	    if (random < culmFreq) {
		chosenPattern = patterns[i];
		break;
	    }
	}

	// Make sure a pattern was chosen
	if (chosenPattern == null) {
	    String message = "No pattern chosen with a cumulative "
		    + "probability of " + culmFreq + " and a selector of "
		    + random;
	    logger.error(message);
	    throw new ModelException(message);
	}
	return chosenPattern;
    }

    /**
     * Draws a random number and monte carlo simulation chooses 
     * pattern for day based on the specified purpose.  
     * 
     * @param purpose  The long distance purpose: HOUSEHOLD, WORKRELATED or OTHER.
     * @return The chosen pattern for the simulated day.
     */ 
    private LDTourPatternType choosePattern(LDTourPurpose purpose, long decisionMakerSeed) {
        Random random = new Random();
        random.setSeed(decisionMakerSeed + ldPatternFixedSeed);

        LDTourPatternType choice;
        choice = choosePatternFromFrequency(frequencies[purpose.ordinal()], random.nextDouble());
        return choice; 
    }	
    
    /**
     * Chooses an LD pattern type for travel made by the entire household. The
     * LD Binary choice model must have already been applied, and the results
     * stored in the ldTourIndicator field.
     *
     *
     *
     *
     * 
     * @param hh
     *            The household of interest.
     * @return The chosen pattern type.
     */
    public LDTourPatternType chooseHouseholdPattern(PTHousehold hh, long hhSeed) {

        // default is no tour
        LDTourPatternType result = LDTourPatternType.NO_TOUR;

        // if a tour is made during the two-week period, choose a pattern.
        if (hh.ldHouseholdTourIndicator) {

            try {
                result = choosePattern(LDTourPurpose.HOUSEHOLD, hhSeed);
            } catch (ModelException e) {
                logger.error("Could not choose a household long distance "
                        + "pattern for household: " + hh.ID + ".");
            }
        }

        if (trace) {
            logger.info("    Chosen HOUSEHOLD pattern for HH " + hh.ID
                    + " is: " + result);
        }

        return result;
    }


    
    /**
     * Chooses an LD pattern type for travel made by individuals for each tour
     * purpose. The LD Binary choice model must have already been applied, and
     * the results stored in the ldTourIndicator field.
     * 
     * @param hh
     *            The household of interest.
     * @param p
     *            The person of interest.
     * @return Array with the chosen pattern type for each purpose.
     */
    public LDTourPatternType[] choosePersonPatterns(PTHousehold hh, PTPerson p, long personSeed) {
        
        LDTourPatternType[] result = new LDTourPatternType[LDTourPurpose.values().length]; 
        
        for (int i=0; i<result.length; i++) {
            // if the purpose is HOUSEHOLD, copy the household values
            if (LDTourPurpose.values()[i].equals(LDTourPurpose.HOUSEHOLD)) {
                result[i] = hh.ldHouseholdTourPattern; 
            }
            
            // otherwise, check to see if a tour occurs in the two-week period
            else if (p.ldTourIndicator[i]) {
                try {
                    result[i] = choosePattern(LDTourPurpose.values()[i], personSeed);
                } catch (ModelException e) {
                    logger.warn(e);
                    logger.warn("Could not choose a person long distance" + " pattern type " + i
                            + " for person: " + (p.hhID + "_" + p.memberID) + ".");
                    logger.warn("Frequency array looks like: ");
                    float[] array = frequencies[LDTourPurpose.values()[i].ordinal()];
                    for (int j = 0; j < array.length; ++j) {
                        logger.error(j + ": " + array[j]);
                    }
                    logger.warn("Choosing default of NO_TOUR"); 
                    result[i] = LDTourPatternType.NO_TOUR;
                }
            }
            // otherwise, there is no tour
            else {
                result[i] = LDTourPatternType.NO_TOUR;
            }
        }
        
        if(trace){
            for (int i=0; i<result.length; i++) {
                logger.info("    Chosen " + LDTourPurpose.values()[i]
                        + " pattern for HH " + hh.ID + " person " + p.memberID
                        + " is: " + result[i]);     
            }    
        }       
        
        return result; 
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
	
    /**
     * Runs the long-distance pattern models, and sets the fields on the household
     * and person objects indicating the pattern of any travel.  This determines
     * whether any travel occurs on the model day.  
     * 
     * @param households An array of households that may be traveling.
     */
    public void runPatternModel(PTHousehold[] households, boolean sensitivityTestingMode) {
        long hhSeed;
        long personSeed;

        logger.debug("Running LDT pattern models.");
        for (int i = 0; i < households.length; i++) {
            if (households[i] == null) {
                continue;
            }
            if(sensitivityTestingMode){
                hhSeed = households[i].ID + System.currentTimeMillis();
            } else {
                hhSeed = households[i].ID;
            }
            households[i].ldHouseholdTourPattern = chooseHouseholdPattern(households[i], hhSeed);
            for (int j = 0; j < households[i].persons.length; j++) {
                if(sensitivityTestingMode){
                    personSeed = households[i].ID*100 + households[i].persons[j].memberID + System.currentTimeMillis();
                } else {
                    personSeed = households[i].ID*100 + households[i].persons[j].memberID;
                }
                households[i].persons[j].ldTourPattern = choosePersonPatterns(
                        households[i], households[i].persons[j], personSeed);
            }
        }
    }
    
}
