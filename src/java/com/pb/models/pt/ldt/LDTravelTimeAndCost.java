/*
 * Copyright 2006 PB Consult Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package com.pb.models.pt.ldt;

import org.apache.log4j.Logger;


/**
 * A class that represents LOS for a zone pair for long-distance tours.
 * 
 * Note that costs are stored in dollars for long-distance trips.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 6, 2006
 * 
 */
public class LDTravelTimeAndCost {

    protected static Logger logger = Logger.getLogger(LDTravelTimeAndCost.class);
    
    // one for each mode
    public float[] inVehicleTime ;      // total in-vehicle time
    public float[] icBusInVehicleTime;  // in-vehicle time on inter-city bus
    public float[] icRailInVehicleTime; // in-vehicle time on inter-city rail 
    public float[] walkTime      ;
    public float[] driveTime     ;
    public float[] waitTime      ;
    public float[] terminalTime  ; 
    public float[] cost          ;      // all costs in cents
    public float[] totalTime     ;
    public float[] frequency     ; 
    
    public LDTravelTimeAndCost() {
        
        inVehicleTime       = new float[LDTourModeType.values().length];
        icBusInVehicleTime  = new float[LDTourModeType.values().length];
        icRailInVehicleTime = new float[LDTourModeType.values().length];        
        walkTime            = new float[LDTourModeType.values().length];
        driveTime           = new float[LDTourModeType.values().length];
        waitTime            = new float[LDTourModeType.values().length];
        terminalTime        = new float[LDTourModeType.values().length]; 
        cost                = new float[LDTourModeType.values().length];
        totalTime           = new float[LDTourModeType.values().length];
        frequency           = new float[LDTourModeType.values().length]; 
    };
    
    /**
     * Adds up the total time and cost for both input objects.  If either in-vehicle time
     * is zero, then the total is set to zero for everything.  
     * 
     * @param tc1 First object to add. 
     * @param tc2 Second object to add. 
     * @return  The total time and cost.  
     */
    public static LDTravelTimeAndCost addTimeAndCost(LDTravelTimeAndCost tc1, LDTravelTimeAndCost tc2) {
        
        LDTravelTimeAndCost total = new LDTravelTimeAndCost(); 
        
        for (int i=0; i<total.inVehicleTime.length; i++) {
            
            if (tc1.inVehicleTime[i]>0 && tc2.inVehicleTime[i]>0) {            
                total.inVehicleTime      [i] = tc1.inVehicleTime[i]       + tc2.inVehicleTime[i]; 
                total.icBusInVehicleTime [i] = tc1.icBusInVehicleTime[i]  + tc2.icBusInVehicleTime[i]; 
                total.icRailInVehicleTime[i] = tc1.icRailInVehicleTime[i] + tc2.icRailInVehicleTime[i]; 
                total.walkTime           [i] = tc1.walkTime[i]            + tc2.walkTime[i]; 
                total.driveTime          [i] = tc1.driveTime[i]           + tc2.driveTime[i]; 
                total.waitTime           [i] = tc1.waitTime[i]            + tc2.waitTime[i]; 
                total.terminalTime       [i] = tc1.terminalTime[i]        + tc2.terminalTime[i]; 
                total.cost               [i] = tc1.cost[i]                + tc2.cost[i]; 
                total.totalTime          [i] = tc1.totalTime[i]           + tc2.totalTime[i];
                // only count frequency in one direction
                total.frequency          [i] = tc1.frequency[i];
            }
        }
        
        return total;
    }
    
    public void resetValues() {
        
        for (int i=0; i<inVehicleTime.length; i++) {
            
            inVehicleTime       [i] = 0;
            icBusInVehicleTime  [i] = 0;
            icRailInVehicleTime [i] = 0;
            walkTime            [i] = 0;
            driveTime           [i] = 0;
            waitTime            [i] = 0;
            terminalTime        [i] = 0; 
            cost                [i] = 0;  
            totalTime           [i] = 0;  
            frequency           [i] = 0; 
        }
        
    }

    public void print() {
        logger.info("\tTravel Time and Cost Values: ");
        
        LDTourModeType[] modes = LDTourModeType.values(); 
        for (int i=0; i<modes.length; i++) {
            logger.info("\tValues for mode " + modes[i]);
            logger.info("\t    inVehicleTime  = "       + inVehicleTime [i]);
            logger.info("\t    icBusInVehicleTime  = "  + icBusInVehicleTime [i]);
            logger.info("\t    icRailInVehicleTime  = " + icRailInVehicleTime [i]);
            logger.info("\t    walkTime       = "       + walkTime      [i]);
            logger.info("\t    driveTime      = "       + driveTime     [i]);
            logger.info("\t    waitTime       = "       + waitTime      [i]);
            logger.info("\t    terminalTime   = "       + terminalTime  [i]);
            logger.info("\t    cost           = "       + cost          [i]);
            logger.info("\t    totalTime      = "       + totalTime     [i]);  
            logger.info("\t    frequency      = "       + frequency     [i]);
        }
    }
}
