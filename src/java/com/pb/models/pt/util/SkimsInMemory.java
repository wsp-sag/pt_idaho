/*
 * Copyright 2005 PB Consult Inc.
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
package com.pb.models.pt.util;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.*;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.ldt.LDTourModeType;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.PriceConverter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ResourceBundle;

/**
 * A class that creates mode choice logsums by 9 market segments for all trip
 * purposes
 * 
 * @author Joel Freedman
 */
public class SkimsInMemory implements Serializable {
    private static SkimsInMemory skims = new SkimsInMemory();

    private static final long serialVersionUID = 1L;
    
    protected static final Object lock = new Object();

    protected static Logger logger = Logger.getLogger(SkimsInMemory.class);

    //public MatrixCollection pkwlk, pkdrv, opwlk, opdrv;
    public MatrixCollection pkwlk, opwlk;

    public Matrix pkAirFwt, pkAirIvt, pkAirDrv, pkAirFar;
    public Matrix opAirFwt, opAirIvt, opAirDrv, opAirFar;
    
    public Matrix pkTime, pkDist, pkToll, opTime, opDist, opToll;

    // the array of modes
    private LDTourModeType[] mode; 
    
    public static float AOC;

    public static float WALK_MPH;

    public static float BIKE_MPH;

    public static float DRIVE_TRANSIT_MPH;

    public static int FIRST_WAIT_SEGMENT;

    public static int AM_PEAK_START;

    public static int AM_PEAK_END;

    public static int PM_PEAK_START;

    public static int PM_PEAK_END;

    public static String MATRIX_EXTENSION;

    private static boolean propertiesRead = false;

    private static boolean skimsRead = false;

    private static boolean getInternalCordonZonePartition = false; //True in Ohio, False in TLUMIP - set by existence of cordon zone file.

    //private double dollarConversionFactor;
    private PriceConverter priceConverter;

    // public static String[] mNameGlobal;

    private AlphaToBeta alphaToBeta;

    private TableDataSet externalCordonZones;

    private SkimsInMemory() {
    }

    /**
     * Get SkimsInMemory reference.
     */
    public static SkimsInMemory getSkimsInMemory() {
        return skims;
    }

    /**
     * Set properties from the global resource bundle.
     */
    public void setGlobalProperties(ResourceBundle globalRb) {
        MATRIX_EXTENSION = globalRb.getString("matrix.extension");
//        AOC = Float.parseFloat(ResourceUtil.getProperty(globalRb, "auto.operating.cost"));
        priceConverter = PriceConverter.getInstance(); //will throw an error if not initialized
        AOC = priceConverter.convertPrice(Float.parseFloat(ResourceUtil.getProperty(globalRb, "auto.operating.cost")),PriceConverter.ConversionType.PRICE);
        WALK_MPH = Float.parseFloat(ResourceUtil.getProperty(globalRb, "sdt.walk.mph"));
        logger.info("walk speed (mph): " + WALK_MPH);

        BIKE_MPH = Float.parseFloat(ResourceUtil.getProperty(globalRb, "sdt.bike.mph"));
        logger.info("bike speed (mph): " + BIKE_MPH);

        DRIVE_TRANSIT_MPH = Float.parseFloat(ResourceUtil.getProperty(globalRb, "sdt.drive.transit.mph"));
        logger.info("drive transit speed (mph): " + DRIVE_TRANSIT_MPH);

        FIRST_WAIT_SEGMENT = Integer.parseInt(ResourceUtil.getProperty(globalRb, "sdt.first.wait.segment"));
        AM_PEAK_START = Integer.parseInt(ResourceUtil.getProperty(globalRb, "am.peak.start"));
        AM_PEAK_END = Integer.parseInt(ResourceUtil.getProperty(globalRb, "am.peak.end"));
        PM_PEAK_START = Integer.parseInt(ResourceUtil.getProperty(globalRb, "pm.peak.start"));
        PM_PEAK_END = Integer.parseInt(ResourceUtil.getProperty(globalRb, "pm.peak.end"));

        alphaToBeta = new AlphaToBeta(new File(globalRb.getString("alpha2beta.file")),
                globalRb.getString("alpha.name"), globalRb.getString("beta.name"));
        
        String cordonZonesFilePath = ResourceUtil.getProperty(globalRb, "cordon.zone.size.terms", null);

        if (cordonZonesFilePath != null) {
            getInternalCordonZonePartition = true;
            CSVFileReader reader = new CSVFileReader();
            try {
                externalCordonZones = reader.readFile(new File(globalRb.getString("cordon.zone.size.terms")));
            } catch (IOException e) {
                throw new RuntimeException("Can't find cordon zone file: "
                        + globalRb.getString("cordon.zone.size.terms"), e);
            }
        }

        //String cf = ResourceUtil.getProperty(globalRb, "convertTo2000Dollars", "1.0");
        //dollarConversionFactor = Float.parseFloat(cf);

        //logger.debug("1990 to 2000 dollar conversion factor is " + dollarConversionFactor);
        logger.debug("1990 to 2000 skim dollar conversion factor is " + priceConverter.getConversionFactor(PriceConverter.ConversionType.SKIM));

        propertiesRead = true;
    }

    /**
     * Create a MatrixCollection from matrix files.
     * 
     * @param matrixNames Matrix names (Ivt must be first!)
     * 
     * @param fileNames Matrix file names (Ivt, Fwt, Twt, Brd, Far, Awk, Aux)
     * @param path Path to matrix files
     * @param type Skim type (wtPk, etc.)
     */
    private MatrixCollection storeTransitSkims(String[] matrixNames, String fileName, String path, String type) {
        long start = System.currentTimeMillis();
        
        Matrix matrix = MatrixReader.readMatrix(new File(path + fileName), type + matrixNames[0]);
        
        if (getInternalCordonZonePartition) { //This happens in Ohio but not in TLUMIP.
            matrix = getInternalCordonPartition(matrix);
        } else {
            matrix = getInternalPartition(matrix);
        }

        matrix.setName(type + matrixNames[0]);
        MatrixCollection mc = new CollapsedMatrixCollection(matrix, false);

        for (int i = 0; i < matrixNames.length; ++i) {
            logger.info("Reading " + type + matrixNames[i] + " in " + path + fileName);

            matrix = MatrixReader.readMatrix(new File(path + fileName), type + matrixNames[i]);
            if (getInternalCordonZonePartition) { //This happens in Ohio but not in TLUMIP.
                matrix = getInternalCordonPartition(matrix);
            } else {
                matrix = getInternalPartition(matrix);
            }

            matrix.setName(type + matrixNames[i]);

            if (matrixNames[i].startsWith("Far")) {
                matrix = convert1990To2000Dollars(matrix);
            }

            mc.addMatrix(matrix);
            logger.debug("Finished adding matrix " + matrix.getName());
        }

        logger.debug("\tRead and stored matrices in " + ((System.currentTimeMillis() - start) / 1000) + " seconds.");

        return mc;
    }

    private Matrix convert1990To2000Dollars(Matrix matrix) {
//        if (dollarConversionFactor != 1) {
//            logger.info("Dollar conversion for matrix " + matrix.getName());
//            matrix.scale((float) dollarConversionFactor);
//        }
//
//        return matrix;
        return priceConverter.convertMatrix(matrix, PriceConverter.ConversionType.SKIM);
    }

    /**
     * Partition a matrix to contain only the TAZs internal to the model area
     * and the cordon zones but not all of the external zones.
     * Used in Ohio
     */
    private Matrix getInternalCordonPartition(Matrix full) {
        int[] extNumbers = alphaToBeta.getAlphaExternals1Based();

        int[] cordonNumbers = externalCordonZones.getColumnAsInt("ETAZ");

        int[] newComboExternalNumbers = new int[extNumbers.length + cordonNumbers.length];

        System.arraycopy(extNumbers, 0, newComboExternalNumbers, 0, extNumbers.length);
        System.arraycopy(cordonNumbers, 0, newComboExternalNumbers, extNumbers.length, cordonNumbers.length);

        return full.getSubMatrix(newComboExternalNumbers);
    }

    /**
     * Partition a matrix to contain only the TAZs internal to the model area
     * Used in TLUMIP
     */
    private Matrix getInternalPartition(Matrix full) {
        int[] extNumbers = alphaToBeta.getAlphaExternals1Based();

        return full.getSubMatrix(extNumbers);
    }

    /**
     * Read the travel costs.
     * 
     * @param rb
     */
    public void readSkims(ResourceBundle rb) {
        
		readHighwaySkims(rb);
		
		readAirSkims(rb);

		try {
			String fileName;
			String[] matrixNames;

			logger.info("Reading transit skims into memory.");

			String transitPath = ResourceUtil.getProperty(rb, "transit.assign.previous.skim.path");
			logger.info("Transit Skim Path = " + transitPath);
        
			matrixNames = ResourceUtil.getArray(rb, "sdt.wt.peak.core.names");
			fileName = ResourceUtil.getProperty(rb, "sdt.wt.peak.skims.file");
			pkwlk = storeTransitSkims(matrixNames, fileName, transitPath, "WtPk");

			matrixNames = ResourceUtil.getArray(rb, "sdt.wt.offpeak.core.names");
			fileName = ResourceUtil.getProperty(rb, "sdt.wt.offpeak.skims.file");
			opwlk = storeTransitSkims(matrixNames, fileName, transitPath, "WtOp");

			//matrixNames = ResourceUtil.getArray(rb, "sdt.dt.peak.core.names");
			//fileName = ResourceUtil.getProperty(rb, "sdt.dt.peak.skims.file");
			//pkdrv = storeTransitSkims(matrixNames, fileName, transitPath, "DtPk");

			//matrixNames = ResourceUtil.getArray(rb, "sdt.dt.offpeak.core.names");
			//fileName = ResourceUtil.getProperty(rb, "sdt.dt.offpeak.skims.file");
			//opdrv = storeTransitSkims(matrixNames, fileName, transitPath, "DtOp");
			
        
		} catch (Exception e) {
			logger.fatal("Error reading travel costs.");
			throw new RuntimeException(e);
		}

		skimsRead = true;
		logger.info("Finished reading skims into memory");

    }

    public void readHighwaySkims(ResourceBundle rb) {

        readPeakHighwaySkims(rb);
        readOffPeakHighwaySkims(rb);

        skimsRead = true;
        logger.info("Finished reading highway skims into memory");
    }

    public void readPeakHighwaySkims(ResourceBundle rb) {
        String hwyPath = ResourceUtil.getProperty(rb, "highway.assign.previous.skim.path");
        logger.info("Hwy Skim Path = " + hwyPath);
        logger.info("Reading Peak Highway Matrices into memory");

        try {
            String fileName;

            // 0 - time, 1 - dist, 2 - toll
            fileName = ResourceUtil.getProperty(rb, "pt.Car.Pk.skims.file");
            logger.info("Reading time in " + hwyPath + fileName);
            pkTime = readTravelCost(hwyPath + fileName, "CarPkTime");
            logger.info("Reading distance in " + hwyPath + fileName);
            pkDist = readTravelCost(hwyPath + fileName, "CarPkDist");
            logger.info("Reading toll in " + hwyPath + fileName);
            pkToll = readTravelCost(hwyPath + fileName, "CarPkToll");
            pkToll = convert1990To2000Dollars(pkToll);

        } catch (Exception e) {
            logger.fatal("Error reading travel costs.");
            throw new RuntimeException(e);
        }

        logger.info("Finished reading peak highway skims into memory");
    }

    public void readOffPeakHighwaySkims(ResourceBundle rb) {
        String hwyPath = ResourceUtil.getProperty(rb, "highway.assign.previous.skim.path");
        logger.info("Hwy Skim Path = " + hwyPath);
        logger.info("Reading Off-peak Highway Matrices into memory");

        try {
            String fileName;

            // 0 - time, 1 - dist, 2 - toll
            fileName = ResourceUtil.getProperty(rb, "pt.Car.Op.skims.file");
            logger.debug("Reading time in " + hwyPath + fileName);
            opTime = readTravelCost(hwyPath + fileName, "CarOpTime");
            logger.debug("Reading distance in " + hwyPath + fileName);
            opDist = readTravelCost(hwyPath + fileName, "CarOpDist");
            logger.debug("Reading toll in " + hwyPath + fileName);
            opToll = readTravelCost(hwyPath + fileName, "CarOpToll");
            opToll = convert1990To2000Dollars(opToll);

        } catch (Exception e) {
            logger.fatal("Error reading travel costs.");
            throw new RuntimeException(e);
        }

        logger.info("Finished reading off-peak highway skims into memory");
    }

    public void readAirSkims(ResourceBundle rb) {

        readPeakAirSkims(rb);
        readOffPeakAirSkims(rb);
        
        skimsRead = true;
        logger.info("Finished reading air skims into memory");
    }

    public void readPeakAirSkims(ResourceBundle rb) {
        String airPath = ResourceUtil.getProperty(rb, "transit.assign.previous.skim.path");
        logger.info("Air Skim Path = " + airPath);
        logger.info("Reading Peak Air Matrices into memory");

        try {
            String fileName;
            fileName = ResourceUtil.getProperty(rb, "pt.Air.Pk.skims.file");
            pkAirIvt = readTravelCost(airPath + fileName, "AirPkIvt");
            pkAirFwt = readTravelCost(airPath + fileName, "AirPkFwt");
            pkAirDrv = readTravelCost(airPath + fileName, "AirPkDrv");
            pkAirFar = readTravelCost(airPath + fileName, "AirPkFar");
            pkAirFar = convert1990To2000Dollars(pkAirFar);

        } catch (Exception e) {
            logger.fatal("Error reading travel costs.");
            throw new RuntimeException(e);
        }

        logger.info("Finished reading peak air skims into memory");
    }

    public void readOffPeakAirSkims(ResourceBundle rb) {
        String airPath = ResourceUtil.getProperty(rb, "transit.assign.previous.skim.path");
        logger.info("Air Skim Path = " + airPath);
        logger.info("Reading Off-peak Air Matrices into memory");

        try {
            String fileName;
            fileName = ResourceUtil.getProperty(rb, "pt.Air.Op.skims.file");
            opAirIvt = readTravelCost(airPath + fileName, "AirOpIvt");
            opAirFwt = readTravelCost(airPath + fileName, "AirOpFwt");
            opAirDrv = readTravelCost(airPath + fileName, "AirOpDrv");
            opAirFar = readTravelCost(airPath + fileName, "AirOpFar");
            opAirFar = convert1990To2000Dollars(opAirFar);


        } catch (Exception e) {
            logger.fatal("Error reading travel costs.");
            throw new RuntimeException(e);
        }

        logger.info("Finished reading off-peak air skims into memory");
    }
    
    public boolean isReady() {
        return skimsRead && propertiesRead;
    }

    /**
     * 
     * @param endTime
     * @param originTaz
     * @param destinationTaz
     * @return time based distance
     */
    public float getDistance(int endTime, int originTaz, int destinationTaz) {
        if ((endTime >= AM_PEAK_START && endTime <= AM_PEAK_END)
                || (endTime >= PM_PEAK_START && endTime <= PM_PEAK_END)) { // peak
            // if PM Peak, then reverse origin and destination to get peak skims
            if (endTime >= PM_PEAK_START && endTime <= PM_PEAK_END)
                return pkDist.getValueAt(destinationTaz, originTaz);
            else
                return pkDist.getValueAt(originTaz, destinationTaz);
        } else
            return opDist.getValueAt(originTaz, destinationTaz);
    }

    // used by the CreateDestinationChoiceLogsums method in calculating the
    // utilities of each OD pair.
    public double getDistance(char purpose, int originTaz, int destinationTaz) {
        if (purpose == 'w') {
            return (double) pkDist.getValueAt(originTaz, destinationTaz);
        } else {
            return (double) opDist.getValueAt(originTaz, destinationTaz);
        }
    }

    /**
     * Get the distance between zones for a zone pair.
     * 
     * @param purpose
     * @param originTaz
     * @param destinationTaz
     * @return purposed based distance
     */
    public double getDistance(ActivityPurpose purpose, int originTaz, int destinationTaz) {
        if (purpose == ActivityPurpose.WORK || purpose == ActivityPurpose.WORK_BASED) {
            return (double) pkDist.getValueAt(originTaz, destinationTaz);
        } else {
            return (double) opDist.getValueAt(originTaz, destinationTaz);
        }
    }

    /**
     * Get the distance matrix.
     * 
     * @param purpose
     * @return purposed-based distance matrix
     */
    public Matrix getDistanceMatrix(ActivityPurpose purpose) {
        if (purpose == ActivityPurpose.WORK || purpose == ActivityPurpose.WORK_BASED) {
            return pkDist;
        }

        return opDist;
    }

    /**
     * Get the time matrix.
     * 
     * @param purpose
     * @return time-based distance matrix
     */
    public Matrix getTimeMatrix(ActivityPurpose purpose) {
        if (purpose == ActivityPurpose.WORK || purpose == ActivityPurpose.WORK_BASED) {
            return pkTime;
        }

        return opTime;
    }

    /**
     * Get the time between zones for a zone pair.
     * 
     * @param purpose
     * @param originTaz
     * @param destinationTaz
     * @return purpose-based time
     */
    public double getTime(ActivityPurpose purpose, int originTaz, int destinationTaz) {
        if (purpose == ActivityPurpose.WORK || purpose == ActivityPurpose.WORK_BASED) {
            return (double) pkTime.getValueAt(originTaz, destinationTaz);
        } else {
            return (double) opTime.getValueAt(originTaz, destinationTaz);
        }
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
    public TravelTimeAndCost setTravelTimeAndCost(int originTaz, int destinationTaz, int time) {

    	TravelTimeAndCost tc = new TravelTimeAndCost();
        tc = setTravelTimeAndCost(tc, originTaz, destinationTaz, time);
        
        return tc; 
    }

    // to set the travel time and cost, based on the origin taz, the destination
    // taz, and the time of day.
    // time of day is in military time from 0 -> 2359
    public TravelTimeAndCost setTravelTimeAndCost(TravelTimeAndCost tc, int originTaz, int destinationTaz, int time) {

        tc.itaz = originTaz;
        tc.jtaz = destinationTaz;

        tc.resetValues(); 
        if ((time >= AM_PEAK_START && time <= AM_PEAK_END) || (time >= PM_PEAK_START && time <= PM_PEAK_END)) { // peak
            // if PM Peak, then reverse origin and destination to get peak skims
            if (time >= PM_PEAK_START && time <= PM_PEAK_END) {

                tc.driveAloneTime     = pkTime.getValueAt(destinationTaz, originTaz);
                tc.driveAloneDistance = pkDist.getValueAt(destinationTaz, originTaz);
                tc.driveAloneCost     = pkDist.getValueAt(destinationTaz, originTaz) * AOC
                                      + pkToll.getValueAt(destinationTaz, originTaz);
                
                tc.sharedRide2Time     = pkTime.getValueAt(destinationTaz, originTaz);
                tc.sharedRide2Distance = pkDist.getValueAt(destinationTaz, originTaz);
                tc.sharedRide2Cost     = pkDist.getValueAt(destinationTaz, originTaz) * AOC
                                       + pkToll.getValueAt(destinationTaz, originTaz) / 2f;
                
                tc.sharedRide3Time     = pkTime.getValueAt(destinationTaz, originTaz);
                tc.sharedRide3Distance = pkDist.getValueAt(destinationTaz, originTaz);
                tc.sharedRide3Cost     = pkDist.getValueAt(destinationTaz, originTaz) * AOC
                                       + pkToll.getValueAt(destinationTaz, originTaz) / 3.5f;
                                
                tc.walkTime = ((pkDist.getValueAt(destinationTaz, originTaz) * 60 / WALK_MPH));
                tc.bikeTime = ((pkDist.getValueAt(destinationTaz, originTaz) * 60 / BIKE_MPH));
                // else it is AM peak
            } else {

                tc.driveAloneTime     = pkTime.getValueAt(originTaz, destinationTaz);
                tc.driveAloneDistance = pkDist.getValueAt(originTaz, destinationTaz);
                tc.driveAloneCost     = pkDist.getValueAt(originTaz, destinationTaz) * AOC
                                      + pkToll.getValueAt(originTaz, destinationTaz);
                
                tc.sharedRide2Time     = pkTime.getValueAt(originTaz, destinationTaz);
                tc.sharedRide2Distance = pkDist.getValueAt(originTaz, destinationTaz);
                tc.sharedRide2Cost     = pkDist.getValueAt(originTaz, destinationTaz) * AOC
                                       + pkToll.getValueAt(originTaz, destinationTaz) / 2f;
                
                tc.sharedRide3Time     = pkTime.getValueAt(originTaz, destinationTaz);
                tc.sharedRide3Distance = pkDist.getValueAt(originTaz, destinationTaz);
                tc.sharedRide3Cost     = pkDist.getValueAt(originTaz, destinationTaz) * AOC
                                       + pkToll.getValueAt(originTaz, destinationTaz) / 3.5f;
                
                tc.walkTime = ((pkDist.getValueAt(originTaz, destinationTaz) * 60 / WALK_MPH));
                tc.bikeTime = ((pkDist.getValueAt(originTaz, destinationTaz) * 60 / BIKE_MPH));
            }
            tc.walkDistance = pkDist.getValueAt(originTaz, destinationTaz);
            tc.bikeDistance = pkDist.getValueAt(originTaz, destinationTaz);

            // if PM Peak, then reverse origin and destination to get peak skims
            if (time >= PM_PEAK_START && time <= PM_PEAK_END) {
                tc.walkTransitInVehicleTime = pkwlk.getValue(destinationTaz, originTaz, "WtPkIvt");
                if (tc.walkTransitInVehicleTime > 0) {
                    tc.walkTransitFirstWaitTime = pkwlk.getValue(destinationTaz, originTaz, "WtPkFwt");
                    tc.walkTransitShortFirstWaitTime = min(tc.walkTransitFirstWaitTime, FIRST_WAIT_SEGMENT);
                    tc.walkTransitLongFirstWaitTime = max((tc.walkTransitFirstWaitTime - FIRST_WAIT_SEGMENT), 0);
                    tc.walkTransitTotalWaitTime = pkwlk.getValue(destinationTaz, originTaz, "WtPkTwt");
                    tc.walkTransitTransferWaitTime = Math.max(
                            (tc.walkTransitTotalWaitTime - tc.walkTransitFirstWaitTime), 0);
                    tc.walkTransitNumberBoardings = pkwlk.getValue(destinationTaz, originTaz, "WtPkBrd");
                    tc.walkTransitWalkTime = pkwlk.getValue(destinationTaz, originTaz, "WtPkAwk")
                            + pkwlk.getValue(destinationTaz, originTaz, "WtPkXwk")
                            + pkwlk.getValue(destinationTaz, originTaz, "WtPkEwk");
                    tc.walkTransitFare = pkwlk.getValue(destinationTaz, originTaz, "WtPkFar");

                    //tc.transitOvt = pkwlk.getValue(destinationTaz,originTaz, "WtPkOvt");				[AK]
                } else {
                    tc.walkTransitFirstWaitTime = 0;
                    tc.walkTransitShortFirstWaitTime = 0;
                    tc.walkTransitLongFirstWaitTime = 0;
                    tc.walkTransitTotalWaitTime = 0;
                    tc.walkTransitTransferWaitTime = 0;
                    tc.walkTransitNumberBoardings = 0;
                    tc.walkTransitWalkTime = 0;
                    tc.walkTransitFare = 0;

                    //tc.transitOvt = 0;															   [AK]
                }

//                tc.driveTransitInVehicleTime = pkdrv.getValue(destinationTaz, originTaz, "dtPkIvt");
//                if (tc.driveTransitInVehicleTime > 0) {
//                    tc.driveTransitFirstWaitTime = pkdrv.getValue(destinationTaz, originTaz, "dtPkFwt");
//                    tc.driveTransitShortFirstWaitTime = min(tc.driveTransitFirstWaitTime, FIRST_WAIT_SEGMENT);
//                    tc.driveTransitLongFirstWaitTime = Math.max((tc.driveTransitFirstWaitTime - FIRST_WAIT_SEGMENT), 0);
//                    tc.driveTransitTotalWaitTime = pkdrv.getValue(destinationTaz, originTaz, "dtPkTwt");
//                    tc.driveTransitTransferWaitTime = Math.max(
//                            (tc.driveTransitTotalWaitTime - tc.driveTransitFirstWaitTime), 0);
//                    tc.driveTransitNumberBoardings = pkdrv.getValue(destinationTaz, originTaz, "dtPkBrd");
//                    tc.driveTransitWalkTime = pkdrv.getValue(destinationTaz, originTaz, "dtPkXwk")
//                            + pkdrv.getValue(destinationTaz, originTaz, "dtPkEwk");
//                    tc.driveTransitDriveTime = pkdrv.getValue(destinationTaz, originTaz, "dtPkDrv");
//                    tc.driveTransitDriveCost = (tc.driveTransitDriveTime / 60) * DRIVE_TRANSIT_MPH;
//                    tc.driveTransitFare = pkdrv.getValue(destinationTaz, originTaz, "dtPkFar");
//                }
                // else it is AM peak
            } else {
                tc.walkTransitInVehicleTime = pkwlk.getValue(originTaz, destinationTaz, "WtPkIvt");
                if (tc.walkTransitInVehicleTime > 0) {

                    tc.walkTransitFirstWaitTime = pkwlk.getValue(originTaz, destinationTaz, "WtPkFwt");
                    tc.walkTransitShortFirstWaitTime = min(tc.walkTransitFirstWaitTime, FIRST_WAIT_SEGMENT);
                    tc.walkTransitLongFirstWaitTime = max((tc.walkTransitFirstWaitTime - FIRST_WAIT_SEGMENT), 0);
                    tc.walkTransitTotalWaitTime = pkwlk.getValue(originTaz, destinationTaz, "WtPkTwt");
                    tc.walkTransitTransferWaitTime = max((tc.walkTransitTotalWaitTime - tc.walkTransitFirstWaitTime), 0);
                    tc.walkTransitNumberBoardings = pkwlk.getValue(originTaz, destinationTaz, "WtPkBrd");
                    tc.walkTransitWalkTime = pkwlk.getValue(originTaz, destinationTaz, "WtPkAwk")
                            + pkwlk.getValue(originTaz, destinationTaz, "WtPkXwk")
                            + pkwlk.getValue(originTaz, destinationTaz, "WtPkEwk");
                    tc.walkTransitFare = pkwlk.getValue(originTaz, destinationTaz, "WtPkFar");
                    
                    //tc.transitOvt = pkwlk.getValue(originTaz, destinationTaz, "WtPkOvt");						[AK]
                } else {
                    tc.walkTransitFirstWaitTime = 0;
                    tc.walkTransitShortFirstWaitTime = 0;
                    tc.walkTransitLongFirstWaitTime = 0;
                    tc.walkTransitTotalWaitTime = 0;
                    tc.walkTransitTransferWaitTime = 0;
                    tc.walkTransitNumberBoardings = 0;
                    tc.walkTransitWalkTime = 0;
                    tc.walkTransitFare = 0;
                    
                    //tc.transitOvt = 0;																		[AK]
                }

//                tc.driveTransitInVehicleTime = pkdrv.getValue(originTaz, destinationTaz, "dtPkIvt");
//                if (tc.driveTransitInVehicleTime > 0) {
//                    tc.driveTransitFirstWaitTime = pkdrv.getValue(originTaz, destinationTaz, "dtPkFwt");
//                    tc.driveTransitShortFirstWaitTime = min(tc.driveTransitFirstWaitTime, FIRST_WAIT_SEGMENT);
//                    tc.driveTransitLongFirstWaitTime = max((tc.driveTransitFirstWaitTime - FIRST_WAIT_SEGMENT), 0);
//                    tc.driveTransitTotalWaitTime = pkdrv.getValue(originTaz, destinationTaz, "dtPkTwt");
//                    tc.driveTransitTransferWaitTime = max(
//                            (tc.driveTransitTotalWaitTime - tc.driveTransitFirstWaitTime), 0);
//                    tc.driveTransitNumberBoardings = pkdrv.getValue(originTaz, destinationTaz, "dtPkBrd");
//                    tc.driveTransitWalkTime = pkdrv.getValue(originTaz, destinationTaz, "dtPkXwk")
//                            + pkdrv.getValue(originTaz, destinationTaz, "dtPkEwk");
//                    tc.driveTransitDriveTime = pkdrv.getValue(originTaz, destinationTaz, "dtPkDrv");
//                    tc.driveTransitDriveCost = (tc.driveTransitDriveTime / 60) * DRIVE_TRANSIT_MPH;
//                    tc.driveTransitFare = pkdrv.getValue(originTaz, destinationTaz, "dtPkFar");
//                }
            }
            
            tc.airInVehicleTime = pkAirIvt.getValueAt(originTaz, destinationTaz);
            if (tc.airInVehicleTime > 0) {            
                tc.airFirstWaitTime = pkAirFwt.getValueAt(originTaz, destinationTaz);
                tc.airTotalWaitTime = tc.airFirstWaitTime;
                tc.airWalkTime = 0;
                tc.airDriveTime = pkAirDrv.getValueAt(originTaz, destinationTaz);
                tc.airFare = pkAirFar.getValueAt(originTaz, destinationTaz);                
            } else {
                tc.airFirstWaitTime = pkAirFwt.getValueAt(originTaz, destinationTaz);
                tc.airTotalWaitTime = tc.airFirstWaitTime;
                tc.airWalkTime = 0;
                tc.airDriveTime = pkAirDrv.getValueAt(originTaz, destinationTaz);
                tc.airFare = pkAirFar.getValueAt(originTaz, destinationTaz);  
            }
            
            // else it is offpeak
        } else {
            tc.driveAloneTime     = opTime.getValueAt(originTaz, destinationTaz);
            tc.driveAloneDistance = opDist.getValueAt(originTaz, destinationTaz);
            tc.driveAloneCost     = opDist.getValueAt(originTaz, destinationTaz) * AOC
                                  + opToll.getValueAt(originTaz, destinationTaz);

            tc.sharedRide2Time     = opTime.getValueAt(originTaz, destinationTaz);
            tc.sharedRide2Distance = opDist.getValueAt(originTaz, destinationTaz);
            tc.sharedRide2Cost     = opDist.getValueAt(originTaz, destinationTaz) * AOC
                                   + opToll.getValueAt(originTaz, destinationTaz) / 2f;

            tc.sharedRide3Time     = opTime.getValueAt(originTaz, destinationTaz);
            tc.sharedRide3Distance = opDist.getValueAt(originTaz, destinationTaz);
            tc.sharedRide3Cost     = opDist.getValueAt(originTaz, destinationTaz) * AOC
                                   + opToll.getValueAt(originTaz, destinationTaz) / 3.5f;

            
            tc.walkTime = ((opDist.getValueAt(originTaz, destinationTaz) * 60 / WALK_MPH));
            tc.walkDistance = opDist.getValueAt(originTaz, destinationTaz);

            tc.bikeTime = ((opDist.getValueAt(originTaz, destinationTaz) * 60 / BIKE_MPH));
            tc.bikeDistance = opDist.getValueAt(originTaz, destinationTaz);

            tc.walkTransitInVehicleTime = opwlk.getValue(originTaz, destinationTaz, "WtOpIvt");

            if (tc.walkTransitInVehicleTime > 0) {
                tc.walkTransitFirstWaitTime = opwlk.getValue(originTaz, destinationTaz, "WtOpFwt");
                tc.walkTransitShortFirstWaitTime = min(tc.walkTransitFirstWaitTime, FIRST_WAIT_SEGMENT);
                tc.walkTransitLongFirstWaitTime = max((tc.walkTransitFirstWaitTime - FIRST_WAIT_SEGMENT), 0);
                tc.walkTransitTotalWaitTime = opwlk.getValue(originTaz, destinationTaz, "WtOpTwt");
                tc.walkTransitTransferWaitTime = max((tc.walkTransitTotalWaitTime - tc.walkTransitFirstWaitTime), 0);
                tc.walkTransitNumberBoardings = opwlk.getValue(originTaz, destinationTaz, "WtOpBrd");
                tc.walkTransitWalkTime = opwlk.getValue(originTaz, destinationTaz, "WtOpAwk")
                        + opwlk.getValue(originTaz, destinationTaz, "WtOpXwk")
                        + opwlk.getValue(originTaz, destinationTaz, "WtOpEwk");
                tc.walkTransitFare = opwlk.getValue(originTaz, destinationTaz, "WtOpFar");
                
                //tc.transitOvt = opwlk.getValue(originTaz, destinationTaz,"WtOpOvt");							[AK]
            } else {
                tc.walkTransitFirstWaitTime = 0;
                tc.walkTransitShortFirstWaitTime = 0;
                tc.walkTransitLongFirstWaitTime = 0;
                tc.walkTransitTotalWaitTime = 0;
                tc.walkTransitTransferWaitTime = 0;
                tc.walkTransitNumberBoardings = 0;
                tc.walkTransitWalkTime = 0;
                tc.walkTransitFare = 0;
                
                //tc.transitOvt = 0;																			[AK]
            }

//            tc.driveTransitInVehicleTime = opdrv.getValue(originTaz, destinationTaz, "dtOpIvt");
//            if (tc.driveTransitInVehicleTime > 0) {
//                tc.driveTransitFirstWaitTime = opdrv.getValue(originTaz, destinationTaz, "dtOpFwt");
//                tc.driveTransitShortFirstWaitTime = min(tc.driveTransitFirstWaitTime, FIRST_WAIT_SEGMENT);
//                tc.driveTransitLongFirstWaitTime = max((tc.driveTransitFirstWaitTime - FIRST_WAIT_SEGMENT), 0);
//                tc.driveTransitTotalWaitTime = opdrv.getValue(originTaz, destinationTaz, "dtOpTwt");
//                tc.driveTransitTransferWaitTime = max((tc.driveTransitTotalWaitTime - tc.driveTransitFirstWaitTime), 0);
//                tc.driveTransitNumberBoardings = opdrv.getValue(originTaz, destinationTaz, "dtOpBrd");
//                tc.driveTransitWalkTime = opdrv.getValue(originTaz, destinationTaz, "dtOpXwk")
//                        + opdrv.getValue(originTaz, destinationTaz, "dtOpEwk");
//                tc.driveTransitDriveTime = opdrv.getValue(originTaz, destinationTaz, "dtOpDrv");
//                tc.driveTransitDriveCost = (tc.driveTransitDriveTime / 60) * DRIVE_TRANSIT_MPH;
//                tc.driveTransitFare = opdrv.getValue(originTaz, destinationTaz, "dtOpFar");
//            }
        }
        
        tc.airInVehicleTime = opAirIvt.getValueAt(originTaz, destinationTaz);
        if (tc.airInVehicleTime > 0) {            
            tc.airFirstWaitTime = opAirFwt.getValueAt(originTaz, destinationTaz);
            tc.airTotalWaitTime = tc.airFirstWaitTime;
            tc.airWalkTime = 0;
            tc.airDriveTime = opAirDrv.getValueAt(originTaz, destinationTaz);
            tc.airFare = opAirFar.getValueAt(originTaz, destinationTaz);                
        } else {
            tc.airFirstWaitTime = opAirFwt.getValueAt(originTaz, destinationTaz);
            tc.airTotalWaitTime = tc.airFirstWaitTime;
            tc.airWalkTime = 0;
            tc.airDriveTime = opAirDrv.getValueAt(originTaz, destinationTaz);
            tc.airFare = opAirFar.getValueAt(originTaz, destinationTaz);  
        }
        
        
        mode = LDTourModeType.values();
        
        for (int m=0; m<mode.length; m++) {
            if (mode[m].equals(LDTourModeType.AUTO)) {
            	tc.inVehicleTime[m]  = tc.driveAloneTime;
                tc.cost[m]           = tc.driveAloneCost;                  
                tc.totalTime[m]      = tc.inVehicleTime[m];
            }
            
            if (mode[m].equals(LDTourModeType.AIR)) {
            	tc.inVehicleTime[m]      = tc.airInVehicleTime;
                tc.cost[m]               = tc.airFare;                  
                tc.walkTimeForLDMode[m]  = tc.airWalkTime;
                tc.waitTime[m]           = tc.airTotalWaitTime;
                tc.terminalTime[m]       = 90;
                tc.totalTime[m]          = tc.inVehicleTime[m] + tc.walkTimeForLDMode[m] + tc.waitTime[m] + tc.terminalTime[m];
            }
            
            if (mode[m].equals(LDTourModeType.TRANSIT_WALK)) {
            	tc.inVehicleTime[m]      = tc.walkTransitInVehicleTime;
                tc.cost[m]               = tc.walkTransitFare;                  
                tc.walkTimeForLDMode[m]  = tc.walkTransitWalkTime;
                tc.waitTime[m]           = tc.walkTransitTotalWaitTime;
                tc.terminalTime[m]       = 30;
                tc.totalTime[m]          = tc.inVehicleTime[m] + tc.walkTimeForLDMode[m] + tc.waitTime[m] + tc.terminalTime[m];
            }
            
        	/*
        	// inter-city bus or rail in-vehicle time
        	if (skimTables[m].contains("Biv")) {
        		tc.icBusInVehicleTime[m] = skims[m].getValue(fromTaz, toTaz, "Biv"); 
        	}
        	if (skimTables[m].contains("Riv")) {
        		tc.icRailInVehicleTime[m] = skims[m].getValue(fromTaz, toTaz, "Riv"); 
        	}
			*/
            
        }        

        return tc;
    }
    
    /*
     * for intermediate stop destination choice
     * 
     */

    public float getAdditionalAutoTime(int fromTaz, int toTaz, int stopTaz, int time) {

        float directTime = 0;
        float totalTime = 0;

        if ((time >= AM_PEAK_START && time <= AM_PEAK_END) || (time >= PM_PEAK_START && time <= PM_PEAK_END)) { // peak
            directTime = pkTime.getValueAt(fromTaz, toTaz);
            totalTime = pkTime.getValueAt(fromTaz, stopTaz) + pkTime.getValueAt(stopTaz, toTaz);
        } else {
            directTime = opTime.getValueAt(fromTaz, toTaz);
            totalTime = opTime.getValueAt(fromTaz, stopTaz) + opTime.getValueAt(stopTaz, toTaz);
        }

        return max((totalTime - directTime), 0);

    }

    /*
     * for intermediate stop destination choice
     * 
     */

    public float getAdditionalWalkTime(int fromTaz, int toTaz, int stopTaz, int time) {

        float directTime = 0;
        float totalTime = 0;

        if ((time >= AM_PEAK_START && time <= AM_PEAK_END) || (time >= PM_PEAK_START && time <= PM_PEAK_END)) { // peak
            directTime = (pkDist.getValueAt(fromTaz, toTaz) * 60 / WALK_MPH);
            totalTime = (pkDist.getValueAt(fromTaz, stopTaz) * 60 / WALK_MPH)
                    + (pkDist.getValueAt(stopTaz, toTaz) * 60 / WALK_MPH);
        } else {
            directTime = (opDist.getValueAt(fromTaz, toTaz) * 60 / WALK_MPH);
            totalTime = (opDist.getValueAt(fromTaz, stopTaz) * 60 / WALK_MPH)
                    + (opDist.getValueAt(stopTaz, toTaz) * 60 / WALK_MPH);
        }

        return max((totalTime - directTime), 0);

    }

    /*
     * for intermediate stop destination choice
     * 
     */

    public float getAdditionalBikeTime(int fromTaz, int toTaz, int stopTaz, int time) {

        float directTime = 0;
        float totalTime = 0;

        if ((time >= AM_PEAK_START && time <= AM_PEAK_END) || (time >= PM_PEAK_START && time <= PM_PEAK_END)) { // peak
            directTime = (pkDist.getValueAt(fromTaz, toTaz) * 60 / BIKE_MPH);
            totalTime = (pkDist.getValueAt(fromTaz, stopTaz) * 60 / BIKE_MPH)
                    + (pkDist.getValueAt(stopTaz, toTaz) * 60 / BIKE_MPH);
        } else {
            directTime = (opDist.getValueAt(fromTaz, toTaz) * 60 / BIKE_MPH);
            totalTime = (opDist.getValueAt(fromTaz, stopTaz) * 60 / BIKE_MPH)
                    + (opDist.getValueAt(stopTaz, toTaz) * 60 / BIKE_MPH);
        }

        return max((totalTime - directTime), 0);

    }

    /*
     * for intermediate stop destination choice
     * 
     */

    public float getAdditionalGeneralizedTransitCost(int fromTaz, int toTaz, int stopTaz, int time) {

        float directCost = 0;
        float totalCost = 0;
        float inVehicleTime = 0;
        float firstWaitTime = 0;
        float totalWaitTime = 0;
        float transferWaitTime = 0;
        float walkTime = 0;
        float firstWaitFactor = (float) 1.5;
        float transferWaitFactor = (float) 2.5;
        float walkFactor = (float) 3.0;

        // the following formula was used to compute generalized cost for model
        // estimation:
        // costToStop= ivtToStop + 1.5*fwtToStop + 2.5*(transfer wait) +
        // 3.0*auxToStop

        if ((time >= AM_PEAK_START && time <= AM_PEAK_END) || (time >= PM_PEAK_START && time <= PM_PEAK_END)) { // peak

            // fromTaz->toTaz
            inVehicleTime = pkwlk.getValue(fromTaz, toTaz, "WtPkIvt");
            if (inVehicleTime <= 0)
                return 0;

            firstWaitTime = pkwlk.getValue(fromTaz, toTaz, "WtPkFwt");
            totalWaitTime = pkwlk.getValue(fromTaz, toTaz, "WtPkTwt");
            transferWaitTime = max((totalWaitTime - firstWaitTime), 0);
            walkTime = pkwlk.getValue(fromTaz, toTaz, "WtPkAwk") + pkwlk.getValue(fromTaz, toTaz, "WtPkXwk")
                    + pkwlk.getValue(fromTaz, toTaz, "WtPkEwk");

            directCost = inVehicleTime + firstWaitFactor * firstWaitTime + transferWaitFactor * transferWaitTime
                    + walkFactor * walkTime;

            // fromTaz->stopTaz
            inVehicleTime = pkwlk.getValue(fromTaz, stopTaz, "WtPkIvt");
            if (inVehicleTime <= 0)
                return 0;

            firstWaitTime = pkwlk.getValue(fromTaz, stopTaz, "WtPkFwt");
            totalWaitTime = pkwlk.getValue(fromTaz, stopTaz, "WtPkTwt");
            transferWaitTime = max((totalWaitTime - firstWaitTime), 0);
            walkTime = pkwlk.getValue(fromTaz, stopTaz, "WtPkAwk") + pkwlk.getValue(fromTaz, stopTaz, "WtPkXwk")
                    + pkwlk.getValue(fromTaz, stopTaz, "WtPkEwk");

            totalCost = inVehicleTime + firstWaitFactor * firstWaitTime + transferWaitFactor * transferWaitTime
                    + walkFactor * walkTime;

            // stopTaz->toTaz
            inVehicleTime = pkwlk.getValue(stopTaz, toTaz, "WtPkIvt");
            if (inVehicleTime <= 0)
                return 0;

            firstWaitTime = pkwlk.getValue(stopTaz, toTaz, "WtPkFwt");
            totalWaitTime = pkwlk.getValue(stopTaz, toTaz, "WtPkTwt");
            transferWaitTime = max((totalWaitTime - firstWaitTime), 0);
            walkTime = pkwlk.getValue(stopTaz, toTaz, "WtPkAwk") + pkwlk.getValue(stopTaz, toTaz, "WtPkXwk")
                    + pkwlk.getValue(stopTaz, toTaz, "WtPkEwk");

            totalCost += inVehicleTime + firstWaitFactor * firstWaitTime + transferWaitFactor * transferWaitTime
                    + walkFactor * walkTime;

        } else {
            // fromTaz->toTaz
            inVehicleTime = opwlk.getValue(fromTaz, toTaz, "WtOpIvt");
            if (inVehicleTime <= 0)
                return 0;

            firstWaitTime = opwlk.getValue(fromTaz, toTaz, "WtOpFwt");
            totalWaitTime = opwlk.getValue(fromTaz, toTaz, "WtOpTwt");
            transferWaitTime = max((totalWaitTime - firstWaitTime), 0);
            walkTime = opwlk.getValue(fromTaz, toTaz, "WtOpAwk") + opwlk.getValue(fromTaz, toTaz, "WtOpXwk")
                    + opwlk.getValue(fromTaz, toTaz, "WtOpEwk");

            directCost = inVehicleTime + firstWaitFactor * firstWaitTime + transferWaitFactor * transferWaitTime
                    + walkFactor * walkTime;

            // fromTaz->stopTaz
            inVehicleTime = opwlk.getValue(fromTaz, stopTaz, "WtOpIvt");
            if (inVehicleTime <= 0)
                return 0;

            firstWaitTime = opwlk.getValue(fromTaz, stopTaz, "WtOpFwt");
            totalWaitTime = opwlk.getValue(fromTaz, stopTaz, "WtOpTwt");
            transferWaitTime = max((totalWaitTime - firstWaitTime), 0);
            walkTime = opwlk.getValue(fromTaz, stopTaz, "WtOpAwk") + opwlk.getValue(fromTaz, stopTaz, "WtOpXwk")
                    + opwlk.getValue(fromTaz, stopTaz, "WtOpEwk");

            totalCost = inVehicleTime + firstWaitFactor * firstWaitTime + transferWaitFactor * transferWaitTime
                    + walkFactor * walkTime;

            // stopTaz->toTaz
            inVehicleTime = opwlk.getValue(stopTaz, toTaz, "WtOpIvt");
            if (inVehicleTime <= 0)
                return 0;

            firstWaitTime = opwlk.getValue(stopTaz, toTaz, "WtOpFwt");
            totalWaitTime = opwlk.getValue(stopTaz, toTaz, "WtOpTwt");
            transferWaitTime = max((totalWaitTime - firstWaitTime), 0);
            walkTime = opwlk.getValue(stopTaz, toTaz, "WtOpAwk") + opwlk.getValue(stopTaz, toTaz, "WtOpXwk")
                    + opwlk.getValue(stopTaz, toTaz, "WtOpEwk");

            totalCost += inVehicleTime + firstWaitFactor * firstWaitTime + transferWaitFactor * transferWaitTime
                    + walkFactor * walkTime;

        }

        return max((totalCost - directCost), 0);

    }

    /**
     * Returns an array of auto distances where first element is distance from
     * origin taz (fromTaz) to primary destination taz and second element is
     * distance from origin taz (fromTaz) to stop taz. Third and fourth elements 
     * are from home->stop and from stop->home
     * 
     * @param fromTaz The anchor location.
     * @param toTaz The primary destination.
     * @param stopTaz An intermediate stop location.
     * @param time Time period.
     * @return 2-d array, element 0=Distft , element 1=Distfs
     */
    public float[] getAdditionalAutoDistance(int fromTaz, int toTaz, int stopTaz, int homeTaz, int time) {

        float[] autoDists = new float[2];

        if ((time >= AM_PEAK_START && time <= AM_PEAK_END) || (time >= PM_PEAK_START && time <= PM_PEAK_END)) { // peak
            autoDists[0] = pkDist.getValueAt(fromTaz, toTaz);
            autoDists[1] = pkDist.getValueAt(fromTaz, stopTaz) + pkDist.getValueAt(stopTaz, toTaz);

        } else {
            autoDists[0] = opDist.getValueAt(fromTaz, toTaz);
            autoDists[1] = opDist.getValueAt(fromTaz, stopTaz) + opDist.getValueAt(stopTaz, toTaz);
        }

        return autoDists;

    }

    public void freeTimeMatrices() {
        pkwlk = null;
//        pkdrv = null;
        opwlk = null;
//        opdrv = null;
        pkTime = null;
        opTime = null;
    }

    public void checkSkims(SkimsInMemory skims) {

        int ptaz = 1;
        int ataz = 1;

        // check 1
        logger.info("PTAZ " + ptaz + " ATAZ " + ataz);
        float pkwtivt = skims.pkwlk.getValue(ptaz, ataz, "pwtivt");
//        float pkdtfwt = skims.pkdrv.getValue(ptaz, ataz, "pdtfwt");
        float pktimet = skims.pkTime.getValueAt(ptaz, ataz);
        logger.info("pk walk time " + pkwtivt);
//        logger.info("pk drive fwait " + pkdtfwt);
        logger.info("pkTime " + pktimet);
        ptaz = 1;
        ataz = 2;

        // check 2
        logger.info("PTAZ " + ptaz + " ATAZ " + ataz);
        pkwtivt = skims.pkwlk.getValue(ptaz, ataz, "pwtivt");
//        pkdtfwt = skims.pkdrv.getValue(ptaz, ataz, "pdtfwt");
        pktimet = skims.pkTime.getValueAt(ptaz, ataz);
        logger.info("pk walk time " + pkwtivt);
//        logger.info("pk drive fwait " + pkdtfwt);
        logger.info("pkTime " + pktimet);

        // check 3
        ptaz = 1;
        ataz = 1240;
        logger.info("PTAZ " + ptaz + " ATAZ " + ataz);
        pkwtivt = skims.pkwlk.getValue(ptaz, ataz, "pwtivt");
//        pkdtfwt = skims.pkdrv.getValue(ptaz, ataz, "pdtfwt");
        pktimet = skims.pkTime.getValueAt(ptaz, ataz);
        logger.info("pk walk time " + pkwtivt);
//        logger.info("pk drive fwait " + pkdtfwt);
        logger.info("pkTime " + pktimet);

        // check 4
        ptaz = 2;
        ataz = 3;
        logger.info("PTAZ " + ptaz + " ATAZ " + ataz);
        pkwtivt = skims.pkwlk.getValue(ptaz, ataz, "pwtivt");
//        pkdtfwt = skims.pkdrv.getValue(ptaz, ataz, "pdtfwt");
        pktimet = skims.pkTime.getValueAt(ptaz, ataz);
        logger.info("pk walk time " + pkwtivt);
//        logger.info("pk drive fwait " + pkdtfwt);
        logger.info("pkTime " + pktimet);
        logger.info(" ");
    }

    /**
     * Generically read a matrix file.
     * 
     * @param fileName Base file name. File name extension.
     */
    private Matrix readTravelCost(String fileName, String name) {
        long startTime = System.currentTimeMillis();

        //logger.info("Reading travel costs in " + fileName);
        Matrix matrix = MatrixReader.readMatrix(new File(fileName), name);
        
        if (getInternalCordonZonePartition) {
            matrix = getInternalCordonPartition(matrix);
        } else {
            matrix = getInternalPartition(matrix);
        }
        
        matrix.setName(name);
        logger.debug("\tRead " + fileName + " in: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");

        return matrix;
    }

    
    public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        SkimsInMemory skims = new SkimsInMemory();
        skims.readSkims(rb);
        skims.checkSkims(skims);
    }

}
