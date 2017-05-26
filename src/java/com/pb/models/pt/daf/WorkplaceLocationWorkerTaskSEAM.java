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
package com.pb.models.pt.daf;

import com.pb.common.daf.Message;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import com.pb.models.pt.util.SkimsInMemory;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * HouseholdWorker processes all messages sent by PTDafMaster
 *
 *
 * @author Christi Willison
 * @version 3.0, 3/8/2005
 *
 */
public class WorkplaceLocationWorkerTaskSEAM extends MessageProcessingTask {
    public static Logger wlLogger = Logger.getLogger(WorkplaceLocationWorkerTaskSEAM.class);

    protected static final Object lock = new Object();
    protected static boolean initialized = false;
    protected static boolean dataRead = false;

    protected static MatrixCollection alphaLaborFlows = new MatrixCollection();

    protected static TableDataSet occupationFile;
    protected static TableDataSet industriesFile;
    protected static String[] occupationLabels;
    protected static String[] industryLabels;
    
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;
    protected static boolean CALCULATE_SDT;
    protected static String sendQueue = "TaskMasterQueue"; //default
    //protected static int MAX_ALPHAZONE_NUMBER;
    protected static int BASE_YEAR;
    //protected static IndustryOccupationSplitIndustryReference indOccSplitRef;
    protected static SkimsInMemory skims;
    protected static String debugDirPath;
    //protected static int[] aZones;

    PTOccupationReferencer occReferencer;

    protected static int MAX_ZONE_NUMBER;
    private static int[] laborFlowZoneExternals;  //combination of internal TAZ (data.TAZs) and cordon zones which come from the CordonSizeTerms.csv file.
                                                 //indexing starts at 1!!



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

                debugDirPath = ptRb.getString("sdt.debug.files");
                if (!(new File(debugDirPath).exists()))
                    new File(debugDirPath).mkdir();
                                
                initialized = true;
            }

            this.occReferencer = occReferencer;
            //ooh, hacky, but don't want to subclass this at this point
            try {
                occReferencer.getClass().getMethod("setProjectState",ResourceBundle.class).invoke(null,globalRb);
            } catch (NoSuchMethodException e) {
                //not in the tlumip model...
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

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
            if (!dataRead) readData(); 
            runWorkplaceLocationModel(msg);
        }
    }

    public void readData(){
        synchronized (lock) {
            if (!dataRead) {
                wlLogger.info(getName() + ", Reading Labor Flow Data on Node");

//                String refFile = globalRb
//                        .getString("industry.occupation.to.split.industry.correspondence");
//                indOccSplitRef = new IndustryOccupationSplitIndustryReference(refFile);
                //indOccSplitRef = new IndustryOccupationSplitIndustryReference(IndustryOccupationSplitIndustryReference.getSplitCorrespondenceFilepath(globalRb));
                wlLogger.info("Reading alpha to beta file.");
                TableDataSet alphaToBetaTable = loadTableDataSet(globalRb, "alpha2beta.file");
                String alphaName = globalRb.getString("alpha.name");
                int[] aZones = alphaToBetaTable.getColumnAsInt(alphaName);

                // The cordon zones are special external zones that meant to
                // represent
                // the
                // "gateways" to the external regions. They are a subset of the
                // full
                // set
                // of ETAZs.
                TableDataSet cordonZonesTable = loadTableDataSet(globalRb, "cordon.zone.size.terms");
                int[] cordonZones = cordonZonesTable.getColumnAsInt("ETAZ");

                createZonalArray(aZones, cordonZones);

                // Find max zone number.
                int max = 0;
                for (int zone : cordonZones) {
                    if (zone > max)
                        max = zone;
                }
                MAX_ZONE_NUMBER = max;

                String path = ResourceUtil.getProperty(ptRb, "seam.labor.flows");
                String suffix = ResourceUtil.getProperty(ptRb, "matrix.extension");

                CSVFileReader reader = new CSVFileReader();
                try {
                    occupationFile = reader.readFile(new File(globalRb.getString("occupation.list.file")));
                } catch (IOException e) {
                   e.printStackTrace();
                }
                occupationLabels = occupationFile.getColumnAsString("occupation");
                
                try {
                	industriesFile = reader.readFile(new File(globalRb.getString("industry.list.file")));
                } catch (IOException e) {
                   e.printStackTrace();
                }
                industryLabels = industriesFile.getColumnAsString("industry");
                
                logger.info("Reading labor flow matrices.");
                for (String occupation : occupationLabels){         //indOccSplitRef.getOccupationLabelsByIndex()) {

                    if (occupation.startsWith("No Occupation")
                            || occupation.startsWith("0_NoOccupation")) {
                        continue;
                    }

                    File file = new File(path + occupation + suffix);

                    Matrix matrix = MatrixReader.readMatrix(file, occupation);
                    matrix.setName(occReferencer.getOccupation(occupation).name());
                    logger.info("Reading labor flows in from " + file.getAbsolutePath()
                            + " and adding to alphaLaborFlows as " + matrix.getName());
                    alphaLaborFlows.addMatrix(matrix);
                }

                // The SkimReaderTask will read in the skims prior to any other task being asked to do work.
                skims = SkimsInMemory.getSkimsInMemory();
                
                dataRead = true;                
            }
        }
    }


     /**
         * Create labor flow matrices for a particular occupation, hh segment,
         * and person array and then determine the workplace locations.
         * 
         * @param msg
         *            Message
         */
    public void runWorkplaceLocationModel(Message msg) {
        PTDataReader reader = new PTDataReader(ptRb, globalRb, occReferencer, BASE_YEAR);

        int startRow = (Integer) msg.getValue("startRow");
        int endRow = (Integer) msg.getValue("endRow");

         int[] homeTazByHhId = (int[]) msg.getValue("homeTazbyHhId");

        wlLogger.info(getName() + ", Running the WorkplaceLocationModel on rows " +
                startRow + " - " + endRow);
        PTPerson[] persons = reader.readPersonsForWorkplaceLocation(startRow, endRow);

        for(PTPerson person : persons){
            person.homeTaz = (short) homeTazByHhId[person.hhID];
        }


        //sort by occupation and segment
        Arrays.sort(persons); // sorts persons by workSegment (0-8) and then by occupation code (0-8)

        // We want to find all persons that match a particular occupation
        // pair and process those and then do the next occupation pair
        int index = 0; 					// index will keep track of where we are in the person array
        int nPersonsUnemployed = 0;
        int nPersonsWithWorkplace = 0;
        HashMap<String, int[]> workersByIndByTazId = new HashMap<String, int[]>(1000);
        HashMap<String, Short> workplaceByPersonId = new HashMap<String, Short>(1000000);


        ArrayList<PTPerson> personList = new ArrayList<PTPerson>();
        while (index < persons.length) {
            Enum occupation = persons[index].occupation;
            int nPersons = 0; // number of people in subgroup for the occ pair.
            while (persons[index].occupation == occupation) {
                if (persons[index].employed) {
                    if (persons[index].occupation == occReferencer.getOccupation(0)) {
                        wlLogger.warn(getName() +  ", Employed person has NONE as their occupation code");
                    }
                    nPersons++;
                    personList.add(persons[index]);
                    index++;
                } else { 			// the person is unemployed - their occupation code may or may not be 0.
                    nPersonsUnemployed++;
                    index++; 		// go to next person
                }
                if (index == persons.length)
                    break; // the last person has been processed.
            }
            if (nPersons > 0) { // there were persons that matched the occ pair (occ != 0)

                wlLogger.debug(getName() + ", Finding Workplaces for " + nPersons + " persons");
                calculateWorkplaceLocation(personList, alphaLaborFlows.getMatrix(occupation.name()));

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
     * Calculate workplace locations for the array of persons given the logsum
     * accessibility matrix (This method does not change the tazs
     * attribute so it is OK to use it)
     *
     * @param persons persons
     * @param flows
     *            matrix
     *
     */
    public void calculateWorkplaceLocation(ArrayList<PTPerson> persons,
                                           Matrix flows) {

        Random random = new Random();
        for (PTPerson person : persons) {
            random.setSeed(person.randomSeed);
            WorkplaceLocationModel.chooseWorkplace(flows,
                    person, random, debugDirPath, laborFlowZoneExternals);
        }

    }

    public void storeResultsInHashMaps(ArrayList<PTPerson> personsBeingProcessed, HashMap<String, int[]> workersByIndByTazId,
                                       HashMap<String, Short> workplaceByPersonId ){
        for(PTPerson person : personsBeingProcessed){
            String industryLabel = industryLabels[person.industry];				//indOccSplitRef.getSplitIndustryLabelFromIndex(person.industry);
            int[] employmentByTaz = workersByIndByTazId.get(industryLabel);
            if(employmentByTaz == null){
                employmentByTaz = new int[MAX_ZONE_NUMBER + 1];
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

    private void createZonalArray(int[] internalZones, int[] cordonZones){

        laborFlowZoneExternals = new int[cordonZones.length + internalZones.length + 1];
        System.arraycopy(internalZones,0,laborFlowZoneExternals,1,internalZones.length);
        System.arraycopy(cordonZones,0,laborFlowZoneExternals,internalZones.length+1,cordonZones.length);


    }

}
