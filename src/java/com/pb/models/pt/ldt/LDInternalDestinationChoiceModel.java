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

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import com.pb.models.pt.util.SkimsInMemory;

import static com.pb.models.pt.ldt.LDInternalDestinationChoiceParameters.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * This model implements a logit model to choose a long-distance tour destination
 * 
 * @author Greg Erhardt
 * @version 1.0 03/23/2006
 * 
 */

public class LDInternalDestinationChoiceModel {

    private static Logger logger = Logger.getLogger(LDInternalDestinationChoiceModel.class);

    private ResourceBundle ptRb;
    
    private ResourceBundle globalRb;

    private boolean trace = false;

    private float[][] parameters;

    private float distanceThreshold;

    private HashMap<Integer, Integer> districtToZone;

    public static SkimsInMemory skims;
    
    protected static TableDataSet industriesFile;
    protected static String[] industryLabels;
    
    private Matrix zonalDistance;

    // logsums for each purpose
    private LDInternalModeChoiceModel modeChoiceModel;
    private MatrixCollection slatMcLogsum;

    private Matrix slatDist;

    private Matrix slatTime;

    private ConcreteAlternative[] amzAlts;

    private LogitModel amzModel;

    private LDInternalDestinationChoicePersonAttributes hha;

    private long ldInternalDestinationFixedSeed = Long.MAX_VALUE/81;

    /**
     * Constructor reads parameters file.
     */
    public LDInternalDestinationChoiceModel(ResourceBundle globalRb,
            ResourceBundle rb, TazManager tazManager,
            LDInternalModeChoiceModel mcModel) {

        this.ptRb = rb;
        this.globalRb = globalRb;
        this.modeChoiceModel = mcModel;
        readParameters();
        distanceThreshold = (float) ResourceUtil.getDoubleProperty(rb, "ldt.threshold.distance.in.miles");
        
        String personIndustryFile = globalRb.getString("industry.list.file");
        
        hha = new LDInternalDestinationChoicePersonAttributes(personIndustryFile);

        buildModel(tazManager);
    }
	
    /**
     * Read parameters from file specified in properties.
     * 
     */
    private void readParameters() {
        
        logger.info("Reading LDInternalDestinationChoiceParameters");
        parameters = ParameterReader.readParameters(ptRb,
                "ldt.internal.destination.choice.parameters");
    }
	
    /**
     * Computes size terms in each TAZ.
     * 
     * @param tazData  The TAZ information to add to the model.
     *                 cost matrix (dollars)--estimated with $0.10/mile.
     */
    public void buildModel(TazManager tazData) { 

        logger.info("Building Internal Destination Choice Model...");
        skims = SkimsInMemory.getSkimsInMemory();
        Matrix distance = skims.opDist;
        Matrix time = skims.opTime;
        
        // set zone-district equivalencies
        String tbaFileName = globalRb.getString("alpha2beta.file");
        String alphaName = globalRb.getString("alpha.name");
        String betaName = globalRb.getString("beta.name");
        TazByAmz tba = new TazByAmz(tbaFileName, alphaName, betaName, ptRb);
        districtToZone = tba.getAmzToTaz(); 
        
    	// aggregate the impedance matrices to slats    	
    	zonalDistance = distance; 
        slatDist = aggregateToSlats(tba, distance);
        slatTime = aggregateToSlats(tba, time);

        // build the logsum matrices
        slatMcLogsum = new MatrixCollection();
        for (LDTourPurpose purpose : LDTourPurpose.values()) {
            Matrix m = modeChoiceModel.createLogsumMatrix(purpose);
            Matrix slat = aggregateToSlats(tba, m);
            slat.setName(purpose.toString());
            slatMcLogsum.addMatrix(slat);
        }
    	// add the AMZs as alternatives to the models
        amzModel = new LogitModel("LD Destination Choice");
        defineAlternatives(tba, tazData);
    }
    
    
    /**
     * Aggregates a single matrix to a slat (zone to district) format.  
     * 
     * @param tba A TazByAmz object containing the equivalencies.  
     * @param zonalMatrix  The matrix to aggregate.  
     * 
     * @return An aggregated slat matrix.  
     */  
    private Matrix aggregateToSlats(TazByAmz tba, 
            Matrix zonalMatrix) {
        
        logger.debug("Aggregating " + zonalMatrix.getName()
                + " matrix to slats (zone-to-district) format");
        
        tba.setMatrix(zonalMatrix);
        tba.averageToSlats();
        
        return tba.getSlat(0); 
    }
    
    /**
     * Creates a set of Amz objects as alternatives.
     * 
     */  
    private void defineAlternatives(TazByAmz tba, TazManager tazData) {
        
        logger.info("  Adding AMZ alternatives to Internal Destination Choice Model...");
        
        Set<Integer> amzSet = districtToZone.keySet();
        amzAlts = new ConcreteAlternative[amzSet.size()];        
        
        Iterator<Integer> iter = amzSet.iterator();
        int index = 0; 
        while (iter.hasNext()) {
        
            int id = iter.next();
            HashSet<Integer> tazSet = tba.getTazSet(id);
            LDAmz amz = new LDAmz(id, tazSet, tazData, parameters); 
            amz.setTrace(trace); 
            amzAlts[index] = new ConcreteAlternative(""+id, amz);
            amzModel.addAlternative(amzAlts[index]);
            index++; 
        } 
        
        logger.info("  Total of " + index + " alternatives added.");
    }    
    
    /** 
     * As an efficiency feature, certain values are stored, such that it will 
     * not be re-calculated for a specific household and tour.  
     * 
     * This method re-initializes those values, and should be applied between
     * each tour.  
     *
     */
    private void resetAmzs() {
        
        for (int i=0; i<amzAlts.length; i++) {
            LDAmz amz = (LDAmz) amzAlts[i].getAlternative();
            amz.reset(); 
        }

        if (trace && logger.isDebugEnabled()) {
            logger.debug("Reset stored LD AMZ values");
        }
    }
    
    
    /**
     * Calculates the impedance going from a TAZ to an AMZ.
     * 
     * @param hha  The household and tour attributes of the traveler.
     * @param tour The tour being considered.
     * @param amz  The district being considered as a destination.
     * 
     * @return The impedance to that district.  
     */  
    private double calculateImpedance(LDInternalDestinationChoicePersonAttributes hha, 
            LDTour tour, 
            LDAmz amz) {
    	
        int p = tour.purpose.ordinal(); 
        int row = tour.homeTAZ;
        int col = districtToZone.get(amz.ID);
        
        Matrix slatLogsumMatrix = slatMcLogsum.getMatrix(tour.purpose.toString());
        float logsum = slatLogsumMatrix.getValueAt(row, col);
        float dist = slatDist.getValueAt(row, col);
        float time = slatTime.getValueAt(row, col);

        int distance0to60flag   = 0;
        int distance60to70flag  = 0;
        int distance70to150flag = 0;
        if (dist < 60) {
            distance0to60flag = 1;
        } else if (dist < 70) {
            distance60to70flag = 1;
        } else if (dist < 150) {
            distance70to150flag = 1;
        }

        double imp = 0;
        imp += parameters[p][MCLOGSUM]           * logsum;
        imp += parameters[p][TIMEIFCOMPLETETOUR] * time * hha.completeTour; 
        imp += parameters[p][DISTANCE0TO60]      * distance0to60flag;
        imp += parameters[p][DISTANCE60TO70]     * distance60to70flag;
        imp += parameters[p][DISTANCE70TO150]    * distance70to150flag;
        
        if (trace && logger.isDebugEnabled()) {
            logger.debug("Impedance to AMZ" + amz.ID + " is " + imp);
        }
        
    	return imp; 
    }
    
       
    /**
     * Calculate utilites for all possible destinations
     * and return logsum for origin TAZ.
     * 
     * @param hha  The household and tour attributes of the traveler.
     * @param tour Decision-makers long-distance tour.
     * 
     * @return The destination choice logsum from tour origin TAZ to all TAZs. 
     */
    private double calculateUtility(LDInternalDestinationChoicePersonAttributes hha, LDTour tour) {
    	        
        // for each alternative
        for (int i=0; i<amzAlts.length; i++) {
            // get the AMZ and calculate the availability
            LDAmz amz = (LDAmz) amzAlts[i].getAlternative();
            int numAvail = amz.setAvailableZones(zonalDistance,
                    distanceThreshold, tour.homeTAZ);
            // if its not available, don't calculate more
            if (numAvail==0) {
                amzAlts[i].setAvailability(false);
                logger.debug("homeTAZ " + tour.homeTAZ + ", amz not available " + amz.ID);
            }
            else {
                double impedance = calculateImpedance(hha, tour, amz);
                double size = amz.calculateLDSize(hha, tour);
                if (size > 0) {
                    double utility = impedance + MathUtil.log(size);
                    amzAlts[i].setUtility(utility);  
                    amzAlts[i].setAvailability(true); 
                }
                else {
                    amzAlts[i].setAvailability(false);
                }
            }
            
            if (trace && logger.isDebugEnabled()) {
                logger.debug("Calculating utility of AMZ" + amz.ID);
                logger.debug("  Availability is " + amzAlts[i].isAvailable());
                logger.debug("  Total utility is " + amzAlts[i].getUtility());  
            }
            
        }
        
        return amzModel.getUtility();
    }
    
    
    
    /**
     * Choose an AMZ (district) according to the probabilities in the
     * model.  The calculateUtility() method should be called before 
     * this method.
     * 
     * @return  The chosen AMZ.
     */
    private LDAmz chooseAmz(double random) {
        
        amzModel.calculateProbabilities(); 

        ConcreteAlternative chosen = (ConcreteAlternative) amzModel.chooseAlternative(random); 
        LDAmz chosenAmz = (LDAmz) chosen.getAlternative(); 
        
    	return chosenAmz; 
    }
    
    /**
     * Choose an TAZ by first choosing an AMZ (district) based on the model, 
     * then picking a TAZ within that district proportionally to the size terms.
     * The calculateUtility() method should be called before this method.
     * 
     * @param tour The long-distance tour of interest.  
     * 
     * @return  The ID of the chosen TAZ.  
     */
    public int chooseTaz(LDTour tour, boolean sensitivityTesting) {
        PTHousehold hh = tour.hh; 
        PTPerson p = tour.person; 
        
        // calculate the household and person characteristics
        hha.codeHouseholdAttributes(hh);
        hha.codePersonAttributes(p);
        hha.codeTourAttributes(tour);

        long seed = tour.hh.ID*100 + tour.person.memberID + tour.ID + ldInternalDestinationFixedSeed;
        if(sensitivityTesting) seed += System.currentTimeMillis();

        Random random = new Random();
        random.setSeed(seed);
        
        // choose the AMZ, then the TAZ within that district 
        resetAmzs(); 
        calculateUtility(hha, tour);
        
//        logger.info("*********");
//        for (int i=0; i<amzAlts.length; i++) {
//        	logger.info("i value " + i);
//        	LDAmz amz = (LDAmz) amzAlts[i].getAlternative();
//        	logger.info("amz " + amz.ID + ", available ? " + amzAlts[i].isAvailable());
//        }
//        logger.info("*********");
        
        LDAmz amz = null; 
        int t = 0; 
        try {
            amz = chooseAmz(random.nextDouble());
            t = amz.chooseDestination(hha, tour, random.nextDouble());
        } catch (ModelException e) {
            //logger.error("Error in " + LDInternalDestinationChoiceModel.class); 
            //hha.print(); 
        	logger.warn("LDInternalDestinationChoiceModel, Setting destination to home taz.");
            t = hh.homeTaz; 
        }
    	
        
        if (trace) {
            logger.info("    The External Destination for HH " + tour.hh.ID
                    + " person " + tour.person.memberID + " tour " + tour.ID
                    + " is AMZ: " + amz.ID + " TAZ: " + t);
        }
        
    	return t; 
    }   
    
    /**
     * Gets the distance from the home zone to the destination zone.  Tour 
     * must have the destination set.  Used as a utility to develop trip-length
     * distributions. 
     * 
     * @param tour The tour of interest.  
     * @return The distance from the home zone to the destination zone.  
     */
    public float getDistance(LDTour tour) {
        float dist = zonalDistance.getValueAt(tour.homeTAZ, tour.destinationTAZ);
        return dist; 
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
   
