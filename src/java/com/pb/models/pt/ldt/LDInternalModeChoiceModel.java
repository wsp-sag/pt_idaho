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
import com.pb.common.model.LogitModel;
import com.pb.common.model.ModelException;
import com.pb.models.pt.Mode;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.Taz;
import com.pb.models.pt.TazManager;
import static com.pb.models.pt.ldt.LDInternalModeChoiceParameters.*;
import com.pb.models.pt.ldt.tourmodes.Air;
import com.pb.models.pt.ldt.tourmodes.Auto;
//import com.pb.models.pt.ldt.tourmodes.HsrDrive;
//import com.pb.models.pt.ldt.tourmodes.HsrWalk;
//import com.pb.models.pt.ldt.tourmodes.TransitDrive;
import com.pb.models.pt.ldt.tourmodes.TransitWalk;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.pt.util.TravelTimeAndCost;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.Random;

/**
 * Choose mode for long-distance tours (auto, air, walk to transit, or drive to transit).  
 * Applied only for internal destinations.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 5, 2006
 *
 */
public class LDInternalModeChoiceModel {
    
    protected static Logger logger = Logger.getLogger(LDInternalModeChoiceModel.class);  
    protected ResourceBundle rb;    
    private boolean trace = false;
    
    // a resource for creating logsum matrices
    TazManager tazManager;

    // Logit Model with a bus-train nest
    protected LogitModel root;
    protected LogitModel groundNest;
    protected LogitModel transitNest;
//    protected LogitModel hsrNest;

    //Elemental Alternatives                
    protected Auto auto;
    protected Air air; 
    protected TransitWalk transitWalk;
//    protected TransitDrive transitDrive;
//    protected HsrWalk hsrWalk;
//    protected HsrDrive hsrDrive;
    
    // The model parameters, for each purpose
    protected float[][] parameters;
    public static SkimsInMemory skims;

    private long ldInternalModeFixedSeed = Long.MIN_VALUE/9;
    
    /**
     * Constructor reads parameters file.  
     */
    public LDInternalModeChoiceModel(ResourceBundle globalRb, ResourceBundle rb,
            TazManager tazManager){
        this.rb = rb;
        this.tazManager = tazManager;
        readParameters();
        skims = SkimsInMemory.getSkimsInMemory();
        buildModel(); 
    }
    
    /**
     * Read parameters from file specified in properties.
     * 
     */
    private void readParameters() {
        
        logger.info("Reading LDInternalModeChoiceModeleParameters");
        parameters = ParameterReader.readParameters(rb,
                "ldt.internal.mode.choice.parameters");
    }
    
    
    /**
     * Adds the alternatives and sets up the model structure.  
     * 
     */
    private void buildModel() { 
        
        logger.info("Building Long Distance Mode Choice Model...");
        
        root        = new LogitModel("root");
        groundNest  = new LogitModel("Ground Transit-HighSpeedRail Nest");
        transitNest = new LogitModel("Transit Walk-Drive Nest");
        
        auto         = new Auto(rb);
        air          = new Air(rb);
        transitWalk  = new TransitWalk(rb);
        
        transitNest.addAlternative(transitWalk);

        groundNest.addAlternative(transitNest);

        root.addAlternative(auto);
        root.addAlternative(air);
        root.addAlternative(groundNest);

        // set dispersion parameters
        groundNest.setDispersionParameter(root.getDispersionParameter() / parameters[0][NESTGROUND]);
        transitNest.setDispersionParameter(groundNest.getDispersionParameter() / parameters[0][NESTTRANSIT]);
    }
    
    /**
     * Calculates the necessary skim values.  
     * 
     * @param tour The LD tour of interest.  
     * @return     The travel time and cost for all modes for that tour.  
     */
    private TravelTimeAndCost setTravelTimeAndCost(LDTour tour) {
        TravelTimeAndCost impedance = null;
        switch (tour.patternType) {
        case BEGIN_TOUR : 
            impedance = skims.setTravelTimeAndCost(
                    tour.homeTAZ, tour.destinationTAZ,
                    tour.schedule.getDepartureMilitaryTime());
            break; 
        case END_TOUR : 
            impedance = skims.setTravelTimeAndCost(
                tour.destinationTAZ, tour.homeTAZ,
                tour.schedule.getArrivalMilitaryTime());
            break;
        case COMPLETE_TOUR :
        	TravelTimeAndCost outboundImpedance = skims.setTravelTimeAndCost(
                    tour.homeTAZ, tour.destinationTAZ,
                    tour.schedule.getDepartureMilitaryTime());

        	TravelTimeAndCost inboundImpedance = skims.setTravelTimeAndCost(
                    tour.destinationTAZ, tour.homeTAZ,
                    tour.schedule.getArrivalMilitaryTime());

            impedance = TravelTimeAndCost.addTimeAndCost(outboundImpedance, inboundImpedance); 
            break;
        default: 
            logger.error("Cannot determine mode.  There is no tour for tourID " + tour.ID);
        }
        
        return impedance;     
    }
    
    /**
     * Solve the logit model for the given set of parameters, costs, traveler and taz
     * attributes, and return the logsum at the root level. 
     * 
     * @param impedance              Costs for journey back to anchor location.
     * @param householdAttributes    Attributes of household making tour.
     * @param tour                  Tour object.
     */
    private double calculateUtility(TravelTimeAndCost impedance,
            LDModeChoiceHouseholdAttributes householdAttributes,
            LDTour tour) {

        float[] purposeParams = parameters[tour.purpose.ordinal()];

        // calculate utilities
        boolean isAirAvailable = air.calculateUtility(purposeParams,
                householdAttributes, tour, impedance);
        boolean isTransitWalkAvailable = transitWalk.calculateUtility(
                purposeParams, householdAttributes, tour, impedance);

        // if no other modes are available, auto will be.
        boolean isNonAutoModeAvailable;
        if (isAirAvailable | isTransitWalkAvailable) { 
            isNonAutoModeAvailable = true;
        } else {
            isNonAutoModeAvailable = false;
        }       
        auto.calculateUtility(purposeParams, householdAttributes, tour, impedance, isNonAutoModeAvailable);
          
        // calculate the totals
        root.computeAvailabilities();
        double logsum = root.getUtility();     

        // report the results
        if (trace) {
            logger.info("    Long-distance mode choice utilities, for availalbe alternatives:");
            if (auto.isAvailable) {
                logger.info("      The auto utility is                         " + auto.getUtility());
            }
            if (air.isAvailable) {
                logger.info("      The air utility is                          " + air.getUtility());
            }
            if (transitWalk.isAvailable) {
                logger.info("      The transit walk access utility is          " + transitWalk.getUtility());
            }
//            if (transitDrive.isAvailable) {
//                logger.info("      The transit drive utility is                " + transitDrive.getUtility());
//            }
//            if (hsrWalk.isAvailable) {
//                logger.info("      The high speed rail walk access utility is  " + hsrWalk.getUtility());
//            }
//            if (hsrDrive.isAvailable) {
//                logger.info("      The high speed rail drive access utility is " + hsrDrive.getUtility());
//            }
        }
        return logsum;
    }
    
    
    /**
     * Creates a matrix of mode choice logsums for the specified tour purpose.
     * Because it would be unreasonable to restrict the choice of modes based
     * on the available travel time, we do this for complete tours.  Also, we
     * assume the travelers are middle income and own an auto.
     *
     * @param purpose The tour purpose of interest.
     * @return An LDT Mode Choice Logsum matrix.
     */
    public Matrix createLogsumMatrix(LDTourPurpose purpose) {
        logger.info("Creating LD Mode Choice Logsum Matrix for Purpose: " + purpose );

        // initialize the output matrix
        Matrix m = new Matrix(purpose.toString() + " logsum",
                "LdtMcLogsumMatrix", tazManager.size(), tazManager.size());
        m.setExternalNumbers(tazManager.getExternalNumberArrayOneIndexed());

        // set up a dummy tour for use here
        LDTour dummyTour = new LDTour(purpose, LDTourPatternType.BEGIN_TOUR);
        LDModeChoiceHouseholdAttributes hha = new LDModeChoiceHouseholdAttributes();

        // set the logsum for all zone pairs
        for (Taz originTaz : tazManager.values()) {
            dummyTour.homeTAZ = originTaz.zoneNumber;

            for (Taz destinationTaz : tazManager.values()) {
                dummyTour.destinationTAZ = destinationTaz.zoneNumber;

                TravelTimeAndCost impedance = setTravelTimeAndCost(dummyTour);
                float logsum = (float) calculateUtility(impedance, hha, dummyTour);
                m.setValueAt(originTaz.zoneNumber, destinationTaz.zoneNumber, logsum);
            }
        }

        return m;
    }

    /**
     * Choose a mode from the model.
     *
     */
    public LDTourModeType chooseMode(LDTour tour, boolean sensitivityTesting) {
        PTHousehold hh = tour.hh;

        long seed = tour.hh.ID*100 + tour.person.memberID + tour.ID + ldInternalModeFixedSeed;
        if(sensitivityTesting) seed += System.currentTimeMillis();

        Random random = new Random();
        random.setSeed(seed);

        Mode chosenMode; 
        // calculate necessary variables
        LDModeChoiceHouseholdAttributes householdAttributes = new LDModeChoiceHouseholdAttributes();
        householdAttributes.setAttributes(hh);
        TravelTimeAndCost impedance = setTravelTimeAndCost(tour);
                            
        // calculate the utility
        calculateUtility(impedance, householdAttributes, tour);
            
        // choose the mode        
        try {
            root.calculateProbabilities();
            chosenMode = (Mode) root.chooseElementalAlternative(random);
        } catch (Exception e) {
            String msg = "Error in mode choice: no modes available (probably)";
            //go ahead and log the utility calculations - lets see what is going on
            trace = true;
            logger.info("Starting a psuedo-debug trace for ldt error");
            logger.info("Origin zone: " + tour.homeTAZ);
            logger.info("Destination zone: " + tour.destinationTAZ);
            logger.info("Destination type: " + tour.destinationType);
            //impedance.print();				//[AK]
            calculateUtility(impedance,householdAttributes,tour);
            logger.info("Ending psuedo-debug trace for ldt error");
            trace = false;
            logger.fatal(msg);
            //throw new ModelException(msg);
            throw new ModelException(e);
        }

        if (trace) {
            logger.info("    The Internal Mode Choice for HH + " + tour.hh.ID + " person "
                    + tour.person.memberID + " tour " + tour.ID + " is : "
                    + chosenMode.alternativeName);
        }
        
        // convert to a mode type
        LDTourModeType chosenModeType = (LDTourModeType) chosenMode.type;
        
        return chosenModeType;
    }
    
    /**
     * A utility to get the travel time to travel by that mode from the trips origin
     * to destination.  Used to fill in times for trip length distributions.  
     * 
     * @param tour The tour of interest.
     * @param mode The mode of interest.
     * @return The travel time to go from the trips home to destination by that mode.   
     */
    public float getOutboundTravelTime(LDTour tour, LDTourModeType mode) {    	
    	TravelTimeAndCost impedance = skims.setTravelTimeAndCost(tour.homeTAZ, tour.destinationTAZ, tour.schedule.getDepartureMilitaryTime());
    	return impedance.totalTime[mode.ordinal()];
    }
    
    public float getOutboundTravelTime(LDTour tour) {
        return getOutboundTravelTime(tour, tour.mode);
    }
    
    /**
     * A utility to get the travel time to travel by that mode from the trips destination
     * to origin.  Used to fill in times for trip length distributions.  
     * 
     * @param tour The tour of interest.
     * @param mode The mode of interest.
     * @return The travel time to go from the trips destination to home by that mode.   
     */
    public float getInboundTravelTime(LDTour tour, LDTourModeType mode) {
    	
    	/*	Changed [AK]
        LDTravelTimeAndCost impedance = skims.setTravelTimeAndCost(
                tour.destinationTAZ, tour.homeTAZ,
                tour.schedule.getArrivalMilitaryTime());
        return impedance.totalTime[mode.ordinal()];
        */
    	
    	TravelTimeAndCost impedance = skims.setTravelTimeAndCost(tour.destinationTAZ, tour.homeTAZ, tour.schedule.getDepartureMilitaryTime());
    	return impedance.totalTime[mode.ordinal()];
    }       
    
    public float getInboundTravelTime(LDTour tour) {
        return getInboundTravelTime(tour, tour.mode);
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
