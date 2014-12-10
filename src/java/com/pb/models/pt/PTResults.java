/*
 * Copyright  2005 PB Consult Inc.
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
package com.pb.models.pt;

import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.ldt.LDTourPurpose;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * PTResults
 *
 * @author Freedman
 * @version Mar 3, 2004
 *
 */
public class PTResults {

    final static Logger logger = Logger.getLogger(PTResults.class);
    private static PrintWriter debug;
    private static PrintWriter workPlaceLocations = null;

    PrintWriter weekdayTour;
    PrintWriter weekdayTrip;
    PrintWriter weekdayPattern;
    PrintWriter householdData;
    PrintWriter personData;
    PrintWriter weekendTour;
    PrintWriter weekendPattern;
    PrintWriter weekendTrip;

    PrintWriter visitorTrip;
    PrintWriter visitorHouseholdData;
    PrintWriter visitorPersonData;

    ResourceBundle rb;
    ResourceBundle globalRb;
    boolean runLDT;
    
    //trips by time-of-day
	HashMap<Short,Integer> startTimes = new HashMap<Short,Integer>(); 
    
    public PTResults(ResourceBundle rb, ResourceBundle globalRb){
        this.rb = rb;
        this.globalRb = globalRb;
        
    }

    /**
     * Convenience method for the resident short-distance model outputs
     */
    public void createFiles(){
        weekdayTour = open(ResourceUtil.getProperty(rb, "sdt.person.tours"));
        weekdayTour.println("hhID,memberID,personAge,weekdayTour(yes/no)," +
                "initialTourString,completedTourString,tour#,departDist," +
                "activityPurpose,startTime,endTime,timeToActivity," +
                "distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity," +
                "distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity," +
                "distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity," +
                "distanceToActivity,tripMode,location," +
                "activityPurpose,startTime,endTime,timeToActivity," +
                "distanceToActivity,tripMode,location,primaryMode");

        weekdayTrip = createTripFile("sdt.person.trips");

        weekdayPattern = open(ResourceUtil.getProperty(rb,
                "sdt.person.patterns"));
        weekdayPattern.println("hhID,memberID,personAge,dayPatternLogsum,dayPattern,"
                + "nWeekdayTours,nWorkTours,nSchoolTours,"
                + "nShopTours,nRecreateTours,nOtherTours");

        householdData = createHouseholdDataFile("sdt.household.data");

        personData = createPersonDataFile("sdt.person.data");
        
    }

    /**
     * Convenience method for the visitor short-distance model outputs
     */
    public void createVisitorFiles(){
        visitorTrip = createTripFile("sdt.visitor.person.trips");

        visitorHouseholdData = createHouseholdDataFile("sdt.visitor.party.data");

        visitorPersonData = createPersonDataFile("sdt.visitor.person.data");

    }

    /**
     *  This method will set up the weekdayTrip file.  It is a standalone
     * method so that it can be used for the resident and the visitor model
     * trip files.
     * @param fileProperty Name of the property that specifies the trip file (could be visitor or resident)
     */
    public PrintWriter createTripFile(String fileProperty){
        PrintWriter tripWriter = open(ResourceUtil.getProperty(globalRb, fileProperty));
        tripWriter.println("hhID,memberID,weekdayTour(yes/no),tour#,"
		    + "subTour(yes/no),tourPurpose,tourSegment,tourMode,"
		    + "origin,destination,time,distance,tripStartTime,tripEndTime,"
		    + "tripPurpose,tripMode,income,age,enroll,esr");
        return tripWriter;
    }

    /**
     * This method is standalone since LDT needs to write out this file but not
     * all of the other files.
     * @param fileProperty Name of the property that specifies the data file (could be long-distance or short-distance)
     */
    public PrintWriter createHouseholdDataFile(String fileProperty){
        PrintWriter writer = open(ResourceUtil.getProperty(rb, fileProperty));
        writer.println("HH_ID,TAZ,PERSONS,SINGLE_FAMILY,AUTOS,HINC,"
                + "LD_HOUSEHOLD_TOUR,LD_HOUSEHOLD_PATTERN");
        return writer;
    }

    /**
     * This method is standalone since LDT needs to write out this file but not
     * all of the other files.
     * @param fileProperty Name of the property that specifies the data file (could be long-distance or short-distance)
     */
    public PrintWriter createPersonDataFile(String fileProperty){
        PrintWriter writer = open(ResourceUtil.getProperty(rb, fileProperty));
        writer.print("HH_ID,memberID,home_taz,SEX,AGE,ENROLL,ESR,"
                + "SW_SPLIT_IND,SW_OCCUP,WORK_TAZ");

        for (LDTourPurpose purpose : LDTourPurpose.values()) {
            writer.print(",LD_INDICATOR_" + purpose);
        }

        for (LDTourPurpose purpose : LDTourPurpose.values()) {
            writer.print(",LD_TOUR_PATTERN_" + purpose);
        }
        writer.println(",generalPattern,"
                + "completePattern,nWeekdayTours,nWorkTours,"
                + "nSchoolTours,nShopTours,nRecreateTours,nOtherTours");
        return writer;
    }

    /**
     * Convenience method for resident short-distance models.
     * @param households array of processed households.
     */
    public void writeResults(PTHousehold[] households) {
        logger.info("Writing patterns and tours to csv file");
        PTDataWriter.writeToursToTextFile(households, weekdayTour, true);
        writeTrips(households);
        PTDataWriter.writeWeekdayPatternsToFile(households, weekdayPattern);
        writeHouseholdData(households);
        writePersonData(households);
    }

    /**
     * Convenience method for resident short-distance models.
     * @param households array of processed households.
     */
    public void writeVisitorResults(PTHousehold[] households) {
        logger.info("Writing patterns and tours to csv file");
        writeVisitorTrips(households);
        writeVisitorPartyData(households);
        writeVisitorPersonData(households);
    }

    public void writeVisitorTrips(PTHousehold[] households){
        PTDataWriter.writeTrips(households, visitorTrip);
    }

    public void writeVisitorPartyData(PTHousehold[] households){
        PTDataWriter.writeHouseholdData(households, visitorHouseholdData);
    }

    public void writeVisitorPersonData(PTHousehold[] households){
        PTDataWriter.writePersonData(households, visitorPersonData);
    }

    public void writeTrips(PTHousehold[] households){
        PTDataWriter.writeTrips(households, weekdayTrip);
    }

    public void writeHouseholdData(PTHousehold[] households){
        PTDataWriter.writeHouseholdData(households, householdData);
    }

    public void writePersonData(PTHousehold[] households){
        PTDataWriter.writePersonData(households, personData);
    }

    /**
     * Convenience method for resident short-distance models
     */
    public void close(){
        logger.info("Closing tour, pattern and trip output files.");
        weekdayTour.flush();
        weekdayTour.close();

        closeTripFile();

        weekdayPattern.flush();
        weekdayPattern.close();

        closeHouseholdData();

        closePersonData();

        if (workPlaceLocations != null) {
            workPlaceLocations.flush();
            workPlaceLocations.close();
        }
    }

    public void closeTripFile(){
        weekdayTrip.flush();
        weekdayTrip.close();
    }

    public void closeHouseholdData(){
        householdData.flush();
        householdData.close();
    }

    public void closePersonData(){
        personData.flush();
        personData.close();
    }
    
    /**
     * Count summaries
     *
     * @param households Household array
     */
    public void calcSummaries(PTHousehold[] households) {
    	
    	String todTripsFileName = ResourceUtil.getProperty(globalRb, "sdt.tod.trips.file");
        
        if(todTripsFileName != null) {
        	calcTODTrips(households);
        }
    	
    }
    
    public void calcTODTrips(PTHousehold[] households) {
        
    //loop through households and sum trips by trip start time
        for (PTHousehold household : households) {
            if (household == null) {
                continue;
            }
            for (PTPerson person : household.persons) {
                if (person.getTourCount() == 0) {
                    continue;
                }
                
                for (Tour tour : person.weekdayTours) {
                    
                	//tour start trip start time
                	if(startTimes.containsKey(Short.valueOf(tour.begin.endTime))) {
                		startTimes.put(tour.begin.endTime, startTimes.get(tour.begin.endTime).intValue() + 1);
                	} else {
                		startTimes.put(tour.begin.endTime, 1);
                	}
                	
                	//tour end trip start time
                	if(startTimes.containsKey(Short.valueOf(tour.primaryDestination.endTime))) {
                		startTimes.put(tour.primaryDestination.endTime, startTimes.get(tour.primaryDestination.endTime).intValue() + 1);
                	} else {
                		startTimes.put(tour.primaryDestination.endTime, 1);
                	}
                                    	
                    if (tour.intermediateStop1 != null) {
                    	
                    	//tour intermediate stop trip start time
                    	if(startTimes.containsKey(Short.valueOf(tour.intermediateStop1.endTime))) {
                    		startTimes.put(tour.intermediateStop1.endTime, startTimes.get(tour.intermediateStop1.endTime).intValue() + 1);
                    	} else {
                    		startTimes.put(tour.intermediateStop1.endTime, 1);
                    	}
                    	
                    } 

                    if (tour.intermediateStop2 != null) {
                    	
                    	//tour intermediate stop 2 trip start time
                    	if(startTimes.containsKey(Short.valueOf(tour.intermediateStop2.endTime))) {
                    		startTimes.put(tour.intermediateStop2.endTime, startTimes.get(tour.intermediateStop2.endTime).intValue() + 1);
                    	} else {
                    		startTimes.put(tour.intermediateStop2.endTime, 1);
                    	}
                    	
                    } 
                }
                // Now print the weekday work based tours - if there are any.
                if (person.weekdayWorkBasedTours != null) {

                    for (Tour tour : person.weekdayWorkBasedTours) {
                    	
                    	//work base tour start trip start time
                    	if(startTimes.containsKey(Short.valueOf(tour.begin.endTime))) {
                    		startTimes.put(tour.begin.endTime, startTimes.get(tour.begin.endTime).intValue() + 1);
                    	} else {
                    		startTimes.put(tour.begin.endTime, 1);
                    	}
                    	                    	
                    	//work base tour end trip start time
                    	if(startTimes.containsKey(Short.valueOf(tour.primaryDestination.endTime))) {
                    		startTimes.put(tour.primaryDestination.endTime, startTimes.get(tour.primaryDestination.endTime).intValue() + 1);
                    	} else {
                    		startTimes.put(tour.primaryDestination.endTime, 1);
                    	}
                    	
                    }
                }

            }
        }
    }
    
    public void writeSummaryFiles() {
    	
    	String todTripsFileName = ResourceUtil.getProperty(globalRb, "sdt.tod.trips.file");
        
        if(todTripsFileName != null) {
        	PrintWriter writer = open(todTripsFileName);
            PTDataWriter.writeTODData(startTimes, writer);
            writer.close();
        }    
    }

    /**
     * Convenience method for the visitor short-distance model outputs
     */
    public void closeVisitorFiles(){
        visitorTrip.flush();
        visitorTrip.close();

        visitorHouseholdData.flush();
        visitorHouseholdData.close();

        visitorPersonData.flush();
        visitorPersonData.close();
    }

    public static PrintWriter createTourDebugFile(ResourceBundle rb, String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"sdt.debug.files");
        if (!(new File(pathToDebugDir).exists()))
            new File(pathToDebugDir).mkdir();
        debug = open(pathToDebugDir + fileName);
        logger.info("Writing to " + pathToDebugDir + fileName);
        debug.println(",,,Begin,,,,,,,IMStop1,,,,,,,PrimaryDestination,,,,,,,IMStop2,,,,,,,End");
        debug.println("tourString,tour#,departDist," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                    "primaryMode");
        debug.flush();
        return debug;
    }

    public static PrintWriter createTazDebugFile(ResourceBundle rb, String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"sdt.debug.files");
        if (!(new File(pathToDebugDir).exists()))
            new File(pathToDebugDir).mkdir();
        //check to see if the file has already been written.  If so, we don't need to write it again.
        if(new File(pathToDebugDir + fileName).exists()) debug=null;
        //if file doesn't exists than create it and return the writer
        else {
            logger.info("Writing to " + pathToDebugDir + fileName);
            debug = open(pathToDebugDir + fileName);
            debug.println(",,,,,,,,,TourSizeTerms,,,,,,,,,,,,TourLnSizeTerms,,,,,,,,,,,,StopSizeTerms,,,,,,,StopLnSizeTerms");
            debug.println("zoneNumber,households,workParkingCost," +
                    "nonWorkParkingCost,acres,pricePerAcre,pricePerSqFtSFD,singleFamilyHH,multiFamilyHH," +
                    "h,w1,w2,w3,w4,b,c1,c2,c3,s,r,o"+
                    "h,w1,w2,w3,w4,b,c1,c2,c3,s,r,o"+
                    "h,w,b,c,s,r,o"+
                    "h,w,b,c,s,r,o");
            debug.flush();
        }

        return debug;
    }

    public static PrintWriter createTripModeDebugFile(ResourceBundle rb, String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"sdt.debug.files");
        if (!(new File(pathToDebugDir).exists()))
            new File(pathToDebugDir).mkdir();
        debug = open(pathToDebugDir + fileName);
        logger.info("Writing to " + pathToDebugDir + fileName);
        debug.println("Trip Mode Choice Model Debug File");
        debug.println("Written: " + new Date());
        debug.println("No trip mode could be chosen.  Here is the summary followed by the details");
        debug.flush();
        return debug;
    }
    

    public static PrintWriter createTourModeDebugFile(ResourceBundle rb, String fileName){
        String pathToDebugDir = ResourceUtil.getProperty(rb,"sdt.debug.files");
        if (!(new File(pathToDebugDir).exists()))
            new File(pathToDebugDir).mkdir();
        debug = open(pathToDebugDir + fileName);
        logger.info("Writing to " + pathToDebugDir + fileName);
        debug.println("Tour Mode Choice Model Debug File");
        debug.println("Written: " + new Date());
        debug.println("No tour mode could be chosen.  Here is the summary followed by the details");
        debug.flush();
        return debug;
    }

    public static MatrixWriter createMatrixWriter(String fileName, String debugDirPath){
        if(new File(debugDirPath).exists()){
            File flowsToWrite = new File(debugDirPath + fileName + ".zmx");						   
            if(!flowsToWrite.exists()){
                return MatrixWriter.createWriter(MatrixType.ZIP, flowsToWrite);					
            } else return null;
        }else throw new RuntimeException("Debug directory path does not exist");
    }

    /**
     * Write work place locations for each worker to a file.
     *
     * This is only called when work place calculations are turned on and SDT
     * calculations are turned off.  If SDT calculations are turned on, then
     * the work place locations are written as part of the person file.
     * @param persons PTPerson[]
     */
    public static void writeWorkPlaceLocations(ResourceBundle rb, PTPerson[] persons) {

        // lazy init
        if (workPlaceLocations == null) {
            String fileName = ResourceUtil.getProperty(rb,
                    "sdt.workplace.locations");
            try {
                workPlaceLocations = new PrintWriter(fileName);
            } catch (FileNotFoundException e) {
                String eMsg = "Could not write work place locations to "
                        + fileName;
                logger.error(eMsg);
                throw new ModelException(e, eMsg);
            }

            // header
            workPlaceLocations.println("HH_ID,memberId,WORK_TAZ");
        }

        for (PTPerson person : persons) {
            workPlaceLocations.println(person.hhID + "," + person.memberID + ","
                    + person.workTaz);
        }
    }



    /**
     * Write work place locations for each worker to a file.
     *
     * This is only called when work place calculations are turned on and SDT
     * calculations are turned off.  If SDT calculations are turned on, then
     * the work place locations are written as part of the person file.
     * @param workplaceByPersonId hashmap
     */
    public static void writeWorkPlaceLocations(ResourceBundle rb, HashMap<String, Integer> workplaceByPersonId) {

        // lazy init
        if (workPlaceLocations == null) {
            String fileName = ResourceUtil.getProperty(rb,
                    "sdt.workplace.locations");
            try {
                workPlaceLocations = new PrintWriter(fileName);
            } catch (FileNotFoundException e) {
                String eMsg = "Could not write work place locations to "
                        + fileName;
                logger.error(eMsg);
                throw new ModelException(e, eMsg);
            }

            // header
            workPlaceLocations.println("HH_ID,memberId,WORK_TAZ");
        }

        for (String key : workplaceByPersonId.keySet()) {
            String hhId =  key.substring(0, key.indexOf("_"));
            String memberId =key.substring(key.indexOf("_") + 1, key.length());
            workPlaceLocations.println( hhId + "," + memberId + ","
                    + workplaceByPersonId.get(key));
        }
    }

    public static PrintWriter open(String textFileName) {
        logger.info("Opening " + textFileName + " for writing.");

        try {
            PrintWriter pwFile;
            pwFile = new PrintWriter(new BufferedWriter(new FileWriter(
                    textFileName)));
            return pwFile;
        } catch (IOException e) {
            logger.error("Could not open file " + textFileName
                    + " for writing\n");
        }
        logger.error("Could not open file " + textFileName
                + " for writing\n");
        return null;
    }



}
