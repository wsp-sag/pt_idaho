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
package com.pb.idaho.pt.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MathUtil;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.model.Alternative;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.DiscreteChoiceModel;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import com.pb.models.pt.daf.MessageID;
import com.pb.models.pt.util.MCLogsumsInMemory;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.idaho.pt.PTOccupation;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * ITDWorkplaceLocationTask is a class that determines the work place location of persons who are employed. 
 *
 * @author Ashish Kulshrestha
 * @email kulshresthaa@pbworld.com
 * @version 1.0, Jan 22, 2015
 *         
 */
public class ITDWorkplaceLocationTask extends MessageProcessingTask {
	public static Logger wlLogger = Logger.getLogger(ITDWorkplaceLocationTask.class);

	protected static final Object lock = new Object();
	protected static boolean initialized = false;
	protected static boolean dataRead = false;
	protected static boolean sensitivityTestingMode;

	protected static ResourceBundle ptRb;
	protected static ResourceBundle globalRb;
	protected static boolean CALCULATE_SDT;
	protected static String sendQueue = "TaskMasterQueue"; //default
	protected static int MAX_ALPHAZONE_NUMBER;
	protected static int BASE_YEAR;
	protected static String[] industryLabels;
	protected static TableDataSet occEmpShares;
	protected static SkimsInMemory skims;
	protected static Matrix[] logsumsInMemory;
	protected static String debugDirPath;
	protected static int[] aZones;
	private ConcreteAlternative[] alts;
	private LogitModel model;
	protected static float logsumParam;
	protected static float distParam;
	protected static float dist2Param;
	protected static float dist3Param;
	protected static float distLogParam;
	protected static float maxDist;
	protected static float [][][] districtConstants;
	
	protected static int totalDCDistricts;
	
	private TazManager tazManager;
	boolean debugTaz = true;
	
	PTOccupationReferencer occReferencer;
	long workplaceLocationModelSeed = Long.MIN_VALUE/243;	

	public void onStart(){
		onStart(PTOccupation.NONE);
	}

	/**
	 * Onstart method sets up model
	 * @param occReferencer - project specific occupation names
	 */
	public void onStart(PTOccupationReferencer occReferencer) {

		wlLogger.info(getName() + ", Started");
		// establish a connection between the workers
		// and the work server.
		Message checkInMsg = mFactory.createMessage();
		checkInMsg.setId(MessageID.WORKER_CHECKING_IN);
		checkInMsg.setValue("workQueueName", getName().trim().substring(0,2) + "_" + getName().trim().substring(12) + "WorkQueue");
		String queueName = "MS_node" + getName().trim().substring(12,13) + "WorkQueue";
		sendTo(queueName, checkInMsg);


		synchronized (lock) {
			if (!initialized) {
				wlLogger.info(getName() + ", Initializing PT Model on Node");
				// We need to read in the Run Parameters (timeInterval and
				// pathToResourceBundle) from the RunParams.properties file
				// that was written by the Application Orchestrator
				String scenarioName;
				int timeInterval;
				String pathToPtRb;
				String pathToGlobalRb;

				wlLogger.info(getName() + ", Reading RunParams.properties file");
				ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
				scenarioName = ResourceUtil.getProperty(runParamsRb,
						"scenarioName");
				wlLogger.info(getName() + ", Scenario Name: " + scenarioName);
				BASE_YEAR = Integer.parseInt(ResourceUtil.getProperty(
						runParamsRb, "baseYear"));
				wlLogger.info(getName() + ", Base Year: " + BASE_YEAR);
				timeInterval = Integer.parseInt(ResourceUtil.getProperty(
						runParamsRb, "timeInterval"));
				wlLogger.info(getName() + ", Time Interval: " + timeInterval);
				pathToPtRb = ResourceUtil.getProperty(runParamsRb,
						"pathToAppRb");
				wlLogger.info(getName() + ", ResourceBundle Path: "
						+ pathToPtRb);
				pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,
						"pathToGlobalRb");
				wlLogger.info(getName() + ", ResourceBundle Path: "
						+ pathToGlobalRb);

				ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
				globalRb = ResourceUtil.getPropertyBundle(new File(
						pathToGlobalRb));

				//initialize price converter
				PriceConverter.getInstance(ptRb,globalRb);

				CALCULATE_SDT = ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.sdt", true);
				if(!CALCULATE_SDT) sendQueue="ResultsWriterQueue";

				//sensitivity testing is for Tlumip - added to code on 4/22/08 by Christi
				//The idea is that when in sensitivityTestMode we would allow the sequence of random numbers
				//to vary from run to run instead of fixing the seed (and thereby fixing the sequence of random numbers)
				//in order to be able to reproduce the results.
				sensitivityTestingMode = ResourceUtil.getBooleanProperty(ptRb, "pt.sensitivity.testing", false);
				wlLogger.info(getName() + ", Sensitivity Testing: " + sensitivityTestingMode);

				debugDirPath = ptRb.getString("sdt.debug.files");
				
				//get the total number of districts defined for the dc calibration constants
				totalDCDistricts = Integer.parseInt(ptRb.getString("total.destination.choice.districts"));
				
				initialized = true;
			}

			this.occReferencer = occReferencer;

		}


		wlLogger.info(getName() + ", Finished onStart()");

	}

	/**
	 * A worker bee that will calculate workplaces for a set of persons
	 *
	 */
	public void onMessage(Message msg) {
		wlLogger.info(getName() + ", Received messageId=" + msg.getId()
				+ " message from=" + msg.getSender());

		if(msg.getId().equals(MessageID.CALCULATE_WORKPLACE_LOCATIONS)){
			readData();
			runWorkplaceLocationModel(msg);
		}
	}

	public void readData(){
		synchronized (lock) {
			if (!dataRead) {
				wlLogger.info("Reading alpha to beta file.");

				TableDataSet alphaToBetaTable = loadTableDataSet(globalRb,
						"alpha2beta.file");
				
				String alphaName = globalRb.getString("alpha.name");
				String betaName = globalRb.getString("beta.name");
				AlphaToBeta a2b = new AlphaToBeta(alphaToBetaTable, alphaName, betaName);
				aZones = a2b.getAlphaExternals1Based(); 
				MAX_ALPHAZONE_NUMBER = a2b.getMaxAlphaZone();
				
				//The SkimReaderTask will read in the skims prior to any other task being asked to do work.
				skims = SkimsInMemory.getSkimsInMemory();

				wlLogger.info("Reading work destination choice parameters.");
				// read work destination choice parameters
				TableDataSet table = loadTableDataSet(globalRb,
						"work.destination.choice.parameters");
				logsumParam = table.getValueAt(1, "logsum");
				distParam = table.getValueAt(1, "distance");
				dist2Param = table.getValueAt(1, "distance2");
				dist3Param = table.getValueAt(1, "distance3");
				distLogParam = table.getValueAt(1, "distanceLog");
				maxDist = table.getValueAt(1, "maxDist");
				
				occEmpShares = loadTableDataSet(globalRb,"work.destination.choice.occEmpShares");
				industryLabels = new String[occEmpShares.getRowCount()];
				industryLabels = occEmpShares.getColumnAsString(1);			
				
				String calibConstantFile = globalRb.getString("calibration.constants.parameters");
				ActivityPurpose[] purposes = ActivityPurpose.values();
				districtConstants = new float [totalDCDistricts+1][totalDCDistricts+1][purposes.length];
				
				//initialize
				for(int oD = 0; oD <= totalDCDistricts; oD++){
					for(int dD = 0; dD <= totalDCDistricts; dD++){
						for(int p = 0; p < purposes.length; p++){
							districtConstants[oD][dD][p] = 0;
						}
					}
				}
				
				//load calibration constants into array
				try {
					BufferedReader reader = new BufferedReader( new FileReader(calibConstantFile) );
					boolean firstRow = true;
					String line;
					while ((line = reader.readLine()) != null) {
						if(!firstRow){
							String[] tokens = line.split(",");
							int oDistrict = Integer.parseInt(tokens[0]);
							int dDistrict = Integer.parseInt(tokens[1]);
							int purpose = Integer.parseInt(tokens[2]);
							float constant = Float.parseFloat(tokens[3]);
							districtConstants[oDistrict][dDistrict][purpose] = constant;
						}
						firstRow = false;
					}
					reader.close();
				} catch (IOException e) {
					wlLogger.fatal("Can't find input file " + calibConstantFile);
					throw new RuntimeException("Can't find input file " + calibConstantFile);
				}
				
				logger.debug("constant for 0,2 and " + ActivityPurpose.WORK.ordinal() + " :" + districtConstants[0][2][ActivityPurpose.WORK.ordinal()]);
				dataRead = true;
			}
		}
	}
	

	/**
	 * Create labor flow matrices for a particular occupation, hh segment, and
	 * person array and then determine the workplace locations.
	 *
	 * @param msg Message
	 */
	public void runWorkplaceLocationModel(Message msg) {
		PTDataReader reader = new PTDataReader(ptRb, globalRb, occReferencer, BASE_YEAR);

		int startRow = (Integer) msg.getValue("startRow");
		int endRow = (Integer) msg.getValue("endRow");
		int[] segmentByHhId = (int[]) msg.getValue("segmentByHhId");
		int[] homeTazByHhId = (int[]) msg.getValue("homeTazbyHhId");
		double[] shadowPriceByTaz = (double[]) msg.getValue("shadowPriceByTaz");
		
		wlLogger.info(getName() + ", Running the WorkplaceLocationModel on rows " +
				startRow + " - " + endRow);
		PTPerson[] persons = reader.readPersonsForWorkplaceLocation(startRow, endRow);

		for(PTPerson person : persons){
			person.segment = (byte) segmentByHhId[person.hhID];
			person.homeTaz = (short) homeTazByHhId[person.hhID];
		}
		
		segmentByHhId = null;
		homeTazByHhId = null;

		//sort by homeTaz, segment and work_occupation
		Arrays.sort(persons);     
		
		// We want to find all persons that match a particular segment/homeTaz/work_occ pair and process those and then do the next segment/homeTaz/work_occ pair
		int index = 0; // index will keep track of where we are in the person array
		int nPersonsUnemployed = 0;
		int nPersonsWithWorkplace = 0;
		HashMap<String, int[]> workersByIndByTazId = new HashMap<String, int[]>(1000);
		HashMap<String, Short> workplaceByPersonId = new HashMap<String, Short>(1000000);

		ArrayList<PTPerson> personList = new ArrayList<PTPerson>();
		
		while (index < persons.length) {
			int segment = persons[index].segment;
			int homeTaz = persons[index].homeTaz;
			int occ = persons[index].workOccupation;
			int nPersons = 0; // number of people in subgroup for the segment/homeTaz/work_occ pair.
			while (persons[index].segment == segment
					&& persons[index].homeTaz == homeTaz && persons[index].workOccupation == occ) {
				if (persons[index].employed) {
					if (persons[index].workOccupation == 0) {
						wlLogger.warn(getName() +  ", Employed person has NONE as their occupation code");
					}
					nPersons++;
					personList.add(persons[index]);
					index++;
				} else { 	// the person is unemployed
					nPersonsUnemployed++;
					index++; // go to next person
				}
				if (index == persons.length)
					break; // the last person has been processed.
			}

			if (nPersons > 0) { 			// there were persons that matched the segment/homeTaz/work_occ combination
				wlLogger.debug(getName() + ", Finding Workplaces for " + nPersons + " persons");
				calculateWorkplaceLocation(personList, segment, homeTaz, occ, shadowPriceByTaz);						

				wlLogger.debug(getName() + ", Storing results to send back to TaskMasterQueue");
				storeResultsInHashMaps(personList, workersByIndByTazId, workplaceByPersonId);

				nPersonsWithWorkplace += nPersons;
			}
			personList.clear();
		}	

		wlLogger.info(getName() + ", Unemployed Persons: " + nPersonsUnemployed);
		wlLogger.info(getName() + ", Persons With Workplace Locations: " + nPersonsWithWorkplace);

		Message workLocations = createMessage();
		workLocations.setId(MessageID.WORKPLACE_LOCATIONS_CALCULATED);
		workLocations.setValue("empInTazs", workersByIndByTazId);
		workLocations.setValue("workplaceByPersonId", workplaceByPersonId);
		workLocations.setValue("nPersonsProcessed", persons.length);
		sendTo(sendQueue, workLocations);
	}

	/**
	 * Calculate work place locations for the array of persons 
	 *
	 */
	
	public void calculateWorkplaceLocation(ArrayList<PTPerson> persons, int segment, int homeTaz, int work_occupation, double[] shadowPriceByTaz) {    
		buildModel();
		Matrix distanceMatrix = skims.getDistanceMatrix(ActivityPurpose.WORK);
		Matrix logsumMatrix = MCLogsumsInMemory.mcLogsumsInMemory[ActivityPurpose.WORK.ordinal()][segment];
		boolean trace = false;

		calculateUtility(homeTaz, work_occupation, distanceMatrix, logsumMatrix, shadowPriceByTaz, trace);
		model.calculateExpUtilities();
		model.calculateProbabilities();
		
		for (PTPerson person : persons) {			
			Random random = new Random();
			random.setSeed(workplaceLocationModelSeed + person.randomSeed);
			int workPlace;

			try {
				workPlace = chooseWorkplace(random);
			} catch (ModelException e) {
				wlLogger.debug(getName() + ", Ignoring non-fatal model exception, " + e);
				wlLogger.info(getName() + ", Setting workplace to home taz.");
				workPlace = (int) person.homeTaz;
			}

			person.workTaz = (short) workPlace;
		}
	}

	private void buildModel() {        
		if(tazManager == null){
			String tazManagerClassName = ResourceUtil.getProperty(ptRb,"sdt.taz.manager.class");
			Class tazManagerClass = null;
			try {
				tazManagerClass = Class.forName(tazManagerClassName);
				tazManager = (TazManager) tazManagerClass.newInstance();
			} catch (ClassNotFoundException e) {
				logger.fatal(tazManagerClass + " not found");
				throw new RuntimeException(e);
			} catch (InstantiationException e) {
				logger.fatal("Can't Instantiate of TazManager of type "+tazManagerClass.getName());
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				logger.fatal("Illegal Access of TazManager of type "+tazManagerClass.getName());
				throw new RuntimeException(e);
			}
			
			String tazClassName = ptRb.getString("sdt.taz.class");
			tazManager.setTazClassName(tazClassName);
			tazManager.readData(globalRb, ptRb);
			
			String filePath = ResourceUtil.getProperty(ptRb, "sdt.employment");
			tazManager.updateWorkersFromSummary(filePath);
		}
		
		alts = new ConcreteAlternative[aZones.length-1];
		
		model = new LogitModel("Workplace Location Choice");
		model.setAvailability(true);
		
		for (int i = 1; i < aZones.length; i++) {
			Integer taz = aZones[i];
			
			alts[i-1] = new ConcreteAlternative(taz.toString(), taz);
			model.addAlternative(alts[i-1]);
		}
	}

	/**
	 * Calculate utilities for all possible work place TAZs from home TAZ for a person
	 */
	private void calculateUtility(int homeTaz, int work_occupation, Matrix distanceMatrix, Matrix logsumMatrix, double[] shadowPriceByTaz, boolean trace) {
		
		if(trace){
			logger.info("Tracing Utility Calculations");
			logger.info("  home taz " + homeTaz);
			logger.info("  work occ category " + work_occupation);
			logger.info("  *Alternatives*  ");
		}
	    
		int originDistrict = (int) tazManager.getTaz(homeTaz).dcDistrict;
		
		//for each alternative
		for (int i = 0; i < alts.length; i++) {
			Integer taz = (Integer) alts[i].getAlternative(); 
			int destinationDistrict = (int) tazManager.getTaz(taz).dcDistrict;
			float utilityConstant = districtConstants[originDistrict][destinationDistrict][ActivityPurpose.WORK.ordinal()];
		
			double totEmp = tazManager.getTaz(taz).employment.get("TotEmp"); 
			double segEmp = 0;
			for (int l = 0; l < industryLabels.length; l++){
				if(work_occupation > 0 && tazManager.getTaz(taz).employment.containsKey(industryLabels[l])) 
					segEmp += tazManager.getTaz(taz).employment.get(industryLabels[l]) * occEmpShares.getValueAt(l+1, work_occupation+1) / 100;	
			}
			
			//TAZ is available as work place only if the segmented employment for the zone is positive. 	
			if (segEmp > 0) {
	            float mcLogsum = logsumMatrix.getValueAt(homeTaz,taz);
	            float distance = distanceMatrix.getValueAt(homeTaz,taz);
	            
	            float sizeTerm = (float) (segEmp * shadowPriceByTaz[taz]);			
	            
	            if(sizeTerm >0)
	            	sizeTerm = (float) MathUtil.log(sizeTerm);
	            else
	            	sizeTerm = 0;
	            
	            distance = Math.min(distance, maxDist);         
				double utility =  utilityConstant + sizeTerm +  (logsumParam * mcLogsum) + (distParam * distance) + (dist2Param * Math.pow(distance, 2)) + (dist3Param * Math.pow(distance, 3)) + (distLogParam * Math.log(distance + 1));
				
				alts[i].setUtility(utility);  
				alts[i].setAvailability(true); 

				if(trace)
					logger.info("taz " + taz + ", totalEmp " + totEmp +  ", segEmp " + segEmp + ", sizeTerm " + sizeTerm + ", mcLogsum " + mcLogsum + ", distance " + distance + ", utility " + utility);
			}
			else {
				alts[i].setAvailability(false);
			}
		}
		if(trace)
			logger.info("End Tracing");
	}

	public int chooseWorkplace(Random random) {
		Integer chosenTaz;        
		try {
			ConcreteAlternative chosen = (ConcreteAlternative) model.chooseAlternative(random.nextDouble());
			chosenTaz = (Integer) chosen.getAlternative(); 
		} catch (Exception e) {
			wlLogger.info("Error in workplace location choice: no alts available");
			throw new ModelException();
		}        
		return chosenTaz;
	}

	public void storeResultsInHashMaps(ArrayList<PTPerson> personsBeingProcessed, HashMap<String, int[]> workersByIndByTazId,
			HashMap<String, Short> workplaceByPersonId ){
		
		for(PTPerson person : personsBeingProcessed){
			String industryLabel = "TotEmp";
			int[] employmentByTaz = workersByIndByTazId.get(industryLabel);
			if(employmentByTaz == null){
				employmentByTaz = new int[MAX_ALPHAZONE_NUMBER + 1];
				employmentByTaz[person.workTaz] = 1;
				workersByIndByTazId.put(industryLabel, employmentByTaz);
			}else{
				employmentByTaz[person.workTaz]++;
				workersByIndByTazId.put(industryLabel, employmentByTaz);
			}
			workplaceByPersonId.put(person.hhID + "_" + person.memberID, person.workTaz);
		}
	}

	private TableDataSet loadTableDataSet(ResourceBundle rb, String pathName) {
		String path = ResourceUtil.getProperty(rb, pathName);
		try {
			CSVFileReader reader = new CSVFileReader();
			return reader.readFile(new File(path));

		} catch (IOException e) {
			wlLogger.fatal("Can't find input table " + path);
			throw new RuntimeException("Can't find input table " + path);
		}
	}

}


