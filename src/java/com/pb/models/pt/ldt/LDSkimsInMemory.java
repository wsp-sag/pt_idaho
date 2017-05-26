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

import com.pb.common.matrix.CollapsedMatrixCollection;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PriceConverter;
import com.pb.models.pt.util.Synchronizable;

import org.apache.log4j.Logger;


import java.io.File;
import java.util.HashSet;
import java.util.ResourceBundle;

/**
 * A singleton class that stores skims for the long-distance travel models.
 * 
 * @author Erhardt
 * @version 1.0 Apr 6, 2006
 * 
 */
public class LDSkimsInMemory {
    private static LDSkimsInMemory instance = new LDSkimsInMemory();

    protected static final Object lock = new Object();
    
    private static boolean skimsRead = false;

    protected static Logger logger = Logger.getLogger(LDSkimsInMemory.class);
    
    // the array of modes
    private LDTourModeType[] mode; 
    
    // one for each mode
    private static MatrixCollection[] pkSkims;
    private static MatrixCollection[] opSkims;
    
    private HashSet<String>[] pkSkimTables;
    private HashSet<String>[] opSkimTables; 
    
    // other parameters
    private static float AOC;
    private static int AM_PEAK_START;
    private static int AM_PEAK_END;
    private static int PM_PEAK_START;
    private static int PM_PEAK_END;
    
    
    private LDSkimsInMemory(){}

    public static LDSkimsInMemory getInstance() {
        return instance;
    }
    /**
     * @param globalRb - global prop file
     * @param rb
     *            Property file
     */
    public void readSkimsIntoMemory (ResourceBundle globalRb, ResourceBundle rb) {
    	mode = LDTourModeType.values();
    	pkSkims = new MatrixCollection[mode.length];
    	opSkims = new MatrixCollection[mode.length];
    	pkSkimTables = new HashSet[mode.length];
    	opSkimTables = new HashSet[mode.length];

    	PriceConverter priceConverter = PriceConverter.getInstance(globalRb,rb);
    	AOC = priceConverter.convertPrice(Float.parseFloat(ResourceUtil.getProperty(globalRb, "auto.operating.cost")),PriceConverter.ConversionType.PRICE);
    	AM_PEAK_START = Integer.parseInt(ResourceUtil.getProperty(globalRb, "am.peak.start"));
    	AM_PEAK_END = Integer.parseInt(ResourceUtil.getProperty(globalRb, "am.peak.end"));
    	PM_PEAK_START = Integer.parseInt(ResourceUtil.getProperty(globalRb, "pm.peak.start"));
    	PM_PEAK_END = Integer.parseInt(ResourceUtil.getProperty(globalRb, "pm.peak.end"));

    	readSkims(rb);
    	skimsRead = true;
    }
    
    /**
     * Read the travel costs.
     * 
     * @param rb
     */
    private void readSkims(ResourceBundle rb) {

        logger.info("Reading Long Distance Skims into memory");
        long startTime = System.currentTimeMillis();
        
        String highwayPath = ResourceUtil.getProperty(rb, "highway.assign.previous.skim.path");
        String transitPath = ResourceUtil.getProperty(rb, "transit.assign.previous.skim.path");

        // read the off-peak skims
        for (int m=0; m<mode.length; m++) {
            String groupName  = mode[m].getGroupName();
            String[] tableLabels = mode[m].getTableLabels();
            opSkimTables[m] = createHashSet(tableLabels);
            opSkims[m] = readMatrixCollection(rb,mode[m] == LDTourModeType.AUTO ? highwayPath : transitPath, groupName, "Op", tableLabels);
        }

        // flag used in testing to save memory
        for (int m=0; m<mode.length; m++) {
                String groupName  = mode[m].getGroupName();
                String[] tableLabels = mode[m].getTableLabels();
                pkSkimTables[m] = createHashSet(tableLabels);
                pkSkims[m] = readMatrixCollection(rb,mode[m] == LDTourModeType.AUTO ? highwayPath : transitPath, groupName, "Pk", tableLabels);
        }


        logger.info("Finished reading long distance into memory in "
                + (System.currentTimeMillis() - startTime) / 1000
                + " seconds");
    }

    /**
     * Creates a hash set of the table names.  
     * 
     * @param values An array of Strings with the table names ("Ivt", "Fwt", etc).
     * @return       A hash set with the table names.  
     */
    private HashSet<String> createHashSet(String[] values) {
        HashSet<String> valueSet = new HashSet<String>(values.length);
        
        for (int i=0; i<values.length; i++) {
            valueSet.add(values[i]);
        }
        
        return valueSet; 
    }
    
    /**
     * Reads the skim matrices for a specific mode and time period.  
     * 
     * @param rb             Resource bundle.  
     * @param path           Path to the skims.
     * @param groupName      Name of the matrix group ("carOp", "carPk", "icdtPk", etc)
     * @param tableLabel      Names of the specific tables ("ivt", "fwt", "xwt", etc)
     * @return               A collection of skim matrices for that mode.  
     */
    private MatrixCollection readMatrixCollection(ResourceBundle rb,
            String path, String groupName, String timePeriod, String[] tableLabel) {
        long startTime = System.currentTimeMillis();
        MatrixCollection mc;

        // get the actual file names
        String fileName = ResourceUtil.getProperty(rb, "pt." + groupName + "." + timePeriod + ".skims.file");
        String coreName = groupName + timePeriod + tableLabel[0];
        
        Matrix index = readMatrix(path, fileName, coreName, tableLabel[0]);

        // use matrix compress for sparse matrices
        if (!groupName.startsWith("car")) {
            mc = new CollapsedMatrixCollection(index, false);
        } else {
            mc = new MatrixCollection(index);
        }

        // read the remaining matrices
        for (int i = 1; i < tableLabel.length; ++i) {
        	coreName = groupName + timePeriod + tableLabel[i]; 
        	Matrix m = readMatrix(path, fileName, coreName, tableLabel[i]);
            mc.addMatrix(m);
        }

        logger.info("\tRead matrix collection in: "
                + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
        
        return mc;
    }
        
    /**
     * Reads a specific skim matrix.
     * 
     * @param fileName  Name of the file to read. 
     * @param coreName Name of the matrix group ("carOpIvt", "carPkToll", etc)
     * @param tableLabel Name of the specific tables ("ivt", "fwt", "xwt", etc)
     * @return A matrix read from disk.  
     */
    private Matrix readMatrix(String path, String fileName, String coreName, String tableLabel) {
	    Matrix m = null;

	    try {
	        logger.info("Reading travel costs in " + path + fileName);
	        File file = new File(path + fileName);
	        m = MatrixReader.readMatrix(file, coreName);
	        m.setName(tableLabel);
	    } catch (Exception e) {
	        logger.fatal("Error reading matrix file " + fileName);
	        e.printStackTrace();
	    }

        return m;
    }
    
    /**
     * Sets the travel time and cost for a specific zone pair, at the specified
     * time of day.  Creates a new object to return.   
     * 
     * @param originTaz      - The origin zone.
     * @param destinationTaz - The destination zone.
     * @param time           - Time of day is in military time from 0 -> 2359
     * 
     * @return The calculated time and cost object.  
     */
    public LDTravelTimeAndCost setTravelTimeAndCost(int originTaz, int destinationTaz, int time) {

        LDTravelTimeAndCost tc = new LDTravelTimeAndCost();
        tc = setTravelTimeAndCost(tc, originTaz, destinationTaz, time);
        
        return tc; 
    }
    
    
    /**
     * Sets the travel time and cost for a specific zone pair, at the specified
     * time of day.  
     * 
     * @param tc             - A time and cost object, re-used for improved processing
     * @param originTaz      - The origin zone.
     * @param destinationTaz - The destination zone.
     * @param time           - Time of day is in military time from 0 -> 2359
     * 
     * @return The calculated time and cost object.  
     */
    private LDTravelTimeAndCost setTravelTimeAndCost(LDTravelTimeAndCost tc,
            int originTaz, int destinationTaz, int time) {
        
        // the AM peak
        if (time >= AM_PEAK_START && time <= AM_PEAK_END) {
            tc = setTravelTimeAndCost(tc, originTaz, destinationTaz, pkSkims, pkSkimTables);
        }
        
        // the PM peak-- reverse the origin and destination to get peak skims
        else if (time >= PM_PEAK_START && time <= PM_PEAK_END) { 
            tc = setTravelTimeAndCost(tc, destinationTaz, originTaz, pkSkims, pkSkimTables);
        }
        // the off-peak
        else {        
            tc = setTravelTimeAndCost(tc, originTaz, destinationTaz, opSkims, opSkimTables); 
        }
        
        return tc; 
    }

    /**
     * Sets the travel time and cost for a specific zone pair, with the skim matrix
     * collections appropriate to the time of day.  
     * 
     * @param tc         A time and ocst object, re-used for improved processing.  
     * @param fromTaz    The from zone.
     * @param toTaz      The to zone. 
     * @param skims      The skims for this time period, one matrix collection for each mode.  
     * @param skimTables A list of the tables in each matrix collection.
     * @return           The calculated time and cost object.
     */
    private LDTravelTimeAndCost setTravelTimeAndCost(LDTravelTimeAndCost tc,
            int fromTaz, int toTaz, 
            MatrixCollection[] skims, 
            HashSet[] skimTables) {

        tc.resetValues(); 
        
        for (int m=0; m<mode.length; m++) {
            if (mode[m].equals(LDTourModeType.AUTO)) {
                // time
                if (skimTables[m].contains("Time")) {
                    tc.inVehicleTime[m] = skims[m].getValue(fromTaz, toTaz, "Time");     
                }      
                
                // cost
                if (skimTables[m].contains("Dist")
                        && skimTables[m].contains("Toll")) {
                    tc.cost[m] = (skims[m].getValue(fromTaz, toTaz, "Dist") * AOC
                               + skims[m].getValue(fromTaz, toTaz, "Toll"));                    
                }
                
                // total time
                tc.totalTime[m] = tc.inVehicleTime[m];
            }
            else {
                tc.inVehicleTime[m] = skims[m].getValue(fromTaz, toTaz, "Ivt");
                if (tc.inVehicleTime[m] > 0) {
                	
                	// inter-city bus or rail in-vehicle time
                	if (skimTables[m].contains("Biv")) {
                		tc.icBusInVehicleTime[m] = skims[m].getValue(fromTaz, toTaz, "Biv"); 
                	}
                	if (skimTables[m].contains("Riv")) {
                		tc.icRailInVehicleTime[m] = skims[m].getValue(fromTaz, toTaz, "Riv"); 
                	}

                    // walk time
                    if (skimTables[m].contains("Awk")) {
                        tc.walkTime[m] += skims[m].getValue(fromTaz, toTaz, "Awk");
                    }
                    if (skimTables[m].contains("Xwk")) {
                        tc.walkTime[m] += skims[m].getValue(fromTaz, toTaz, "Xwk");
                    }
                    if (skimTables[m].contains("Ewk")) {
                        tc.walkTime[m] += skims[m].getValue(fromTaz, toTaz, "Ewk");
                    }

                    // drive time
                    if (skimTables[m].contains("Drv")) {
                        tc.driveTime     [m] = skims[m].getValue(fromTaz, toTaz, "Drv");
                    }                   
                    
                    // wait time 
                    if (skimTables[m].contains("Fwt")) {
                    	tc.waitTime[m] += skims[m].getValue(fromTaz, toTaz, "Fwt");
                    }
                    if (skimTables[m].contains("Twt")) {
                    	tc.waitTime[m] += skims[m].getValue(fromTaz, toTaz, "Twt");
                    }     
                    
                    // terminal time
                    switch(mode[m]) {
                        case AIR           : tc.terminalTime[m] = 90; break; 
                        case TRANSIT_WALK  : tc.terminalTime[m] = 30; break;
//                        case TRANSIT_DRIVE : tc.terminalTime[m] = 30; break;
//                        case HSR_WALK      : tc.terminalTime[m] = 30; break;
//                        case HSR_DRIVE     : tc.terminalTime[m] = 30; break;
                        default : tc.terminalTime[m] = 0; 
                    }
                    
                    // cost
                    if (skimTables[m].contains("Far")) {
                        tc.cost[m] = skims[m].getValue(fromTaz, toTaz, "Far");
                    }

                    // total time
                    tc.totalTime[m] = tc.inVehicleTime[m] + tc.walkTime[m] + tc.driveTime[m] 
                                    + tc.waitTime[m] + tc.terminalTime[m];
                    
                    // frequency
                    if (skimTables[m].contains("Frq")) {
                        tc.frequency[m] = skims[m].getValue(fromTaz, toTaz, "Frq");
                    }
                }
            }           
        }
        return tc; 
    }
    
    /**
     * @return matrix for the specified mode, with the specified name
     */
    public static Matrix getPeakMatrix(LDTourModeType mode, String name) {
        if (skimsRead) {
            MatrixCollection mc = pkSkims[mode.ordinal()];
            Matrix m = mc.getMatrix(name);
            return m;
        } else {
            throw new RuntimeException("Skims have not been read - must initialize first");
        }
    }

    /**
     * @return matrix for the specified mode, with the specified name
     */
    public static Matrix getOffPeakMatrix(LDTourModeType mode, String name) {
        if (skimsRead) {
            MatrixCollection mc = opSkims[mode.ordinal()];
            Matrix m = mc.getMatrix(name);
            return m;
        } else {
            throw new RuntimeException("Skims have not been read - must initialize first");
        }
    }
}
