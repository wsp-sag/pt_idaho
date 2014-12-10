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

import com.pb.common.daf.*;
import com.pb.common.util.BooleanLock;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import com.pb.models.utils.StatusLogger;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 *
 * PTDafMaster sends messages to work queues.
 *
 *
 * @author Christi Willison
 * @version 1.0, 5/5/2004
 *
 */
public class PTMasterTask extends Task {
    Logger ptDafMasterLogger = Logger.getLogger(PTMasterTask.class);
    MessageFactory mFactory;
    protected static BooleanLock signal = new BooleanLock(false);

    protected static boolean CALCULATE_DCLOGSUMS;
    protected static boolean CALCULATE_MCLOGSUMS;
    protected static boolean CALCULATE_WORKPLACE_LOCATIONS;

    Port[] mcWorkPorts;    //mode choice
    Port[] msServerPorts;  //micro-simulation (auto-own, workplace location, tour choices)
    Port[] dcWorkPorts;    //destination choice logsums
    Port defaultPort;
    Port resultWriterPort;
    private ArrayList<String> mcWorkQueues = new ArrayList<String>();
    private ArrayList<String> dcWorkQueues = new ArrayList<String>();
    private ArrayList<String> msWorkQueues = new ArrayList<String>();

    ResourceBundle ptdafRb; // this will be read in after the scenarioName has
                            // been read in from RunParams.txt

    ResourceBundle ptRb; // this will be read in after pathToPtRb has been
                            // read in from RunParams.txt

    ResourceBundle globalRb; // this will be read in after   pathToGlobalRb has
                                // been read in from RunParams.txt

    private PTDataReader reader;
    private VisitorDataReader vmReader;
    private TazManager tazManager;
    //float incomeConversionFactor;
    private PriceConverter priceConverter;

    //These variables help us keep track of what work has been done and is left to do.
    static int TOTAL_MCLOGSUMS =  (ActivityPurpose.values().length - 1) * PTHousehold.SEGMENTS;
    static int TOTAL_COLLAPSED_MCLOGSUMS; // is set in the properties file.  Will be read in during onStart method
    static int TOTAL_DCLOGSUMS = (ActivityPurpose.values().length - 2) * PTHousehold.SEGMENTS; // no logsums for work and home purposes

    static int NUM_HOUSEHOLDS;
    //static int NUM_VISITOR_HH;
    int[][] hhInfo;
    int [] visitorsInfo;
    int numHhsProcessed;
    int numVisitorsProcessed;
    static int NUM_PERSONS;
    static int NUM_VISITOR_PERSONS;
    int[] personHhIds;
    byte[] memberIds;
    short[] workplaces;
    int index = 0;

    int sampleRate;

    int nodesWithSkimsCount = 0;
    int mcLogsumCount = 0;
    int mcCollapsedLogsumCount = 0;
    int hhsWithAutoChoiceMade = 0;
    int personsWithWorkplaceCount = 0;
    int dcLogsumCount = 0;
    int householdsProcessedCount = 0;
    int visitorsProcessedCount = 0;
    int timingStatementsCount = 0;
    int numLDTToursExpected = 0; 
    int numLDTToursProcessed = 0; 
    
    //used by status logger
    private int timePeriod;

    private long activityPattern = 0;
    private long tourScheduling = 0;
    private long tourDestination = 0;
    private long tourMode = 0;
    private long tourStops = 0;
    private long stopDestination = 0;
    private long stopDuration = 0;
    private long tripMode = 0;

    boolean runningSEAM;

    public void doWork(){

        initializeVM();

        determineNumOfWorkQueues();  //this will figure out nMCWorkQueues, nMSWorkQueues, nDCWorkQueues, etc.

        setUpPortsAndQueues();

        receiveAndProcessMessages(); //wait for all nodes to read in the skims.

        if(ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.mc.logsums", true)){
            ptDafMasterLogger.info(getName() + ", Sending out mode choice logsum calculation work");
            sendMCLogsumWork();
            receiveAndProcessMessages();  //wait for the MCLogsums to finish
        }

        try{
        ptDafMasterLogger.info(getName() + ", Reading SynPopH and SynPopP files and collecting info");
        hhInfo = readHouseholdFile();     // returns first hhId (0 or 1), num of HHs, nWorkers per HH, income per HH, homeTaz per HH.
                                                          // (hhInfo[0][0] = firstIdNum,
                                                          // hhInfo[1][0] = nHhs,
                                                          // hhInfo[2][0-maxId] = nWorkers per HH
                                                          // hhInfo[3][0-maxId] = income per HH    / autos per HH (after AutoOwn)
                                                          // hhInfo[4][0-maxId] = home taz per HH
                                                          //hhInfo[5][0-maxId] =  hhSegment (after AutoOwn)         
        
        NUM_HOUSEHOLDS = hhInfo[1][0];
        NUM_PERSONS = readPersonFile(hhInfo[2]);     //this method returns num of Persons AND fills the hhInfo[2] array with number of workers indexed by hhId number
        } catch (Exception e) {
            ptDafMasterLogger.error("something",e);
            throw new RuntimeException(e);
        }
        
        ptDafMasterLogger.info(getName() + ", nHhs: " + NUM_HOUSEHOLDS + " nPers: " + NUM_PERSONS);


        ptDafMasterLogger.info(getName() + ", Sending out auto-ownership work");
        sendAutoOwnershipWork(hhInfo[2]);     //nWorkers per HH array will be sent along in the messages.
        receiveAndProcessMessages();         //AutoOwnership Work has to be done before Workplace Locations
                                             //can be calculated.   The number of autos
                                               //will replace the income per HH in the hhInfo[3] array.

        if (ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.workplaces", true)) {
            initializeWorkplaceHolder();
            sendWorkplaceLocationWork(hhInfo[5], hhInfo[4]);  //send segment per HH, and homeTazByHH out to workers with message
            receiveAndProcessMessages();    //implies that Workplace Locations have been determined and that we have written the employment by taz file
            ptDafMasterLogger.info(getName() + ", Signaling that the Workplace Location is finished.");
        }


        if(ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.dc.logsums", true)){
            ptDafMasterLogger.info(getName() + ", Sending destination choice logsums work");
            sendDCLogsumWork();
            receiveAndProcessMessages();    //implies that destination choice logsum work is complete.  It has to be done before HHs can be microsimulated

        }

        if (ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.sdt", true)
                || ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.ldt", true)) {

            sendSDTandLDTWork();  //sending the hh auto-ownership results and the person workTAZ results
            calculateNumHhsProcessed();
            receiveAndProcessMessages();
        }

        // If Visitor model is specified in the properties file
        // NUM_VISITOR_HH is defined in visitorInfo[1]
        if (ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.vm", false)) { 
            visitorsInfo=vmReader.getInfoFromVisitorHouseholdFile();
            int[] nullWorkerArray = new int[visitorsInfo[1] + 1];
            NUM_VISITOR_PERSONS = vmReader.getInfoFromPersonFile(nullWorkerArray);
            if (NUM_VISITOR_PERSONS > 0 && visitorsInfo[1] > 0) {
                sendVisitorsWork();
                calculateNumVisitorsProcessed();
                receiveAndProcessMessages();
            }else{
                logger.info("No visitor population for SDT models");
            }
        }

        if (ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.workplaces", true) ||
                   ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.sdt", true) ||
                   ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.ldt", true) ||
                   ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.vm", false)){
            sendCloseFileMessageToWriter();
            receiveAndProcessMessages();
        }

        writeDoneFile();

    }

    private void initializeVM(){
        ptDafMasterLogger.info("***" + getName() + ", started");

        ptDafMasterLogger.info(getName() + ", Reading RunParams.properties file");
        ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
        String scenarioName = ResourceUtil.getProperty(runParamsRb, "scenarioName");
        ptDafMasterLogger.info("\tScenario Name: " + scenarioName);
        int baseYear = Integer.parseInt(ResourceUtil.getProperty(
                            runParamsRb, "baseYear"));
                ptDafMasterLogger.info(getName() + ", Base Year: " + baseYear);
        int timeInterval = Integer.parseInt(ResourceUtil.getProperty(runParamsRb,
                "timeInterval"));
        ptDafMasterLogger.info("\tTime Interval: " + timeInterval);
        String pathToPtRb = ResourceUtil.getProperty(runParamsRb, "pathToAppRb");
        timePeriod = timeInterval;
        ptDafMasterLogger.info("\tResourceBundle Path: " + pathToPtRb);
        String pathToGlobalRb = ResourceUtil.getProperty(runParamsRb, "pathToGlobalRb");
        ptDafMasterLogger.info("\tResourceBundle Path: " + pathToGlobalRb);

        // Get the properties files.
        try {
            ptdafRb = ResourceUtil.getResourceBundle("ptdaf");
        } catch (MissingResourceException e) {
            ptdafRb = ResourceUtil.getResourceBundle("ptdaf_" + scenarioName);  //TLUMIP/Ohio thing.
        }

        ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));

        // are we running PECAS or SEAM?
        runningSEAM = ResourceUtil.getBooleanProperty(globalRb, "running.seam");

        //initialize price converter
        priceConverter = PriceConverter.getInstance(ptRb,globalRb);

        // set whether you want to calculate the dc and mode choice
        // logsums in production mode these should always be true.
        CALCULATE_DCLOGSUMS =  ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.dc.logsums", true);
        CALCULATE_MCLOGSUMS = ResourceUtil.getBooleanProperty(ptRb,"sdt.calculate.mc.logsums", true);
        CALCULATE_WORKPLACE_LOCATIONS = ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.workplaces", true);
        // if(!runningSEAM) TOTAL_COLLAPSED_MCLOGSUMS = ResourceUtil.getList(ptRb,"sdt.matrices.for.pecas").size();				[AK]
        TOTAL_COLLAPSED_MCLOGSUMS = 0;
        
        reader = new PTDataReader(ptRb, globalRb,null, baseYear);
        vmReader = new VisitorDataReader(ptRb, globalRb,null, baseYear);

        ptDafMasterLogger.info("Initializing TazManager.");
        String tazManagerClass = ResourceUtil.getProperty(ptRb,"sdt.taz.manager.class");
        Class tazClass = null;
        tazManager = null;
        try {
            tazClass = Class.forName(tazManagerClass);
            tazManager = (TazManager) tazClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.fatal(tazManagerClass + " not found");
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            logger.fatal("Can't Instantiate of TazManager of type "+ tazClass.getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Illegal Access of TazManager of type "+ tazClass.getName());
            throw new RuntimeException(e);
        }

        String tazClassName = ptRb.getString("sdt.taz.class");
        String alphaName = globalRb.getString("alpha.name");
        tazManager.setTazClassName(tazClassName);
        tazManager.setAlphaZoneName(alphaName);
        tazManager.readData(globalRb, ptRb);
        tazManager.setParkingCost(globalRb,ptRb,"alpha2beta.file");
        ptDafMasterLogger.info("Finished initiliazing TazManager.");

        //String cf = ResourceUtil.getProperty(globalRb, "convertTo2000Dollars", "1.0");
//        incomeConversionFactor = Float.parseFloat(cf);
        sampleRate = ResourceUtil.getIntegerProperty(globalRb, "pt.sample.rate", 1);


//        int lowMax = ResourceUtil.getIntegerProperty(ptRb,"pt.low.max.income",20000);
//        int highMax = ResourceUtil.getIntegerProperty(ptRb,"pt.med.high.max.income",60000);
        //PRICE, not INCOME, because this is coming from a property file, not from synthetic population income levels
        int lowMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(ptRb,"pt.low.max.income",20000),PriceConverter.ConversionType.PRICE);
        int highMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(ptRb,"pt.med.high.max.income",60000),PriceConverter.ConversionType.PRICE);
        IncomeSegmenter.setIncomeCategoryRanges(lowMax, highMax);

       ptDafMasterLogger.info(getName() + "Finished initialization");
    }

    private void determineNumOfWorkQueues(){
        ArrayList queues = ResourceUtil.getList(ptdafRb, "queueList");
        if (ptDafMasterLogger.isDebugEnabled())
            ptDafMasterLogger.debug("Total queues " + queues.size());
        for (Object queue : queues) {
            String queueName = (String) queue;
            if (ptDafMasterLogger.isDebugEnabled())
                ptDafMasterLogger.debug("Queue Name : " + queueName);
            if (queueName.indexOf("MC") >= 0) {
                mcWorkQueues.add(queueName);
                if (ptDafMasterLogger.isDebugEnabled())
                    ptDafMasterLogger.debug(queueName
                            + " added to mcWorkQueues");
            }
            if (queueName.indexOf("DC") >= 0) {
                dcWorkQueues.add(queueName);
                if (ptDafMasterLogger.isDebugEnabled())
                    ptDafMasterLogger.debug(queueName
                            + " added to dcWorkQueues");
            }
            if (queueName.indexOf("MS") >= 0) {
                msWorkQueues.add(queueName);
                if (ptDafMasterLogger.isDebugEnabled())
                    ptDafMasterLogger.debug(queueName
                            + " added to msWorkQueues");
            }

        }
        if (ptDafMasterLogger.isDebugEnabled()) {
            ptDafMasterLogger.debug("number of mcWorkQueues "
                    + mcWorkQueues.size());
            ptDafMasterLogger.debug("number of dcWorkQueues "
                    + dcWorkQueues.size());
            ptDafMasterLogger.debug("number of msWorkQueues "
                    + msWorkQueues.size());

        }

    }

    private void setUpPortsAndQueues() {

        PortManager pManager = PortManager.getInstance();
        mFactory = MessageFactory.getInstance();

        mcWorkPorts = new Port[mcWorkQueues.size()];
        for(int i=0;i<mcWorkQueues.size();i++){
            mcWorkPorts[i] = pManager.createPort(mcWorkQueues.get(i));
        }

        msServerPorts = new Port[msWorkQueues.size()];
        for(int i=0;i<msWorkQueues.size();i++){             //work queues will always start on node 1 (and be numbered 1...n)
            msServerPorts[i] = pManager.createPort(msWorkQueues.get(i)); //
        }

        dcWorkPorts = new Port[dcWorkQueues.size()];
        for(int i=0;i<dcWorkQueues.size();i++){
            dcWorkPorts[i] = pManager.createPort(dcWorkQueues.get(i));
        }

        defaultPort = pManager.createPort("TaskMasterQueue");

        resultWriterPort = pManager.createPort("ResultsWriterQueue");


    }

    private void sendMCLogsumWork(){
        ArrayList<Message> messages = createMCLogsumWorkMessages();
        sendAggregateWork(mcWorkPorts, messages, "MC Logsum work");

    }

    private int[][] readHouseholdFile(){
        int[][] info = new int[6][];

        //int[][] hhFileInfo = reader.getInfoFromHouseholdFile(incomeConversionFactor);
        int[][] hhFileInfo = reader.getInfoFromHouseholdFile(priceConverter);

        info[0] = new int[]{hhFileInfo[0][0]};      //lowest Id number - most likely 0 or 1
        info[1] = new int[]{hhFileInfo[1][0]};      //total number of hhs

        info[2] = new int[hhFileInfo[1][0] + hhFileInfo[0][0]];    //new array to hold the workers by HhId
                                                                   // //if hhId starts at 1, we will need nHhs + 1, if hhId starts at 5
                                                                                              //we will need nHhs + 5.
        info[3] = hhFileInfo[2];     //incomes by HhId

        info[4] = hhFileInfo[3];     //home taz by HhId

        info[5] = new int[hhFileInfo[1][0] + hhFileInfo[0][0]]; //new array to hold the Hh segment

        return info;
    }

    
    
    /**
     * This method returns the number of persons.
     * It also has a SIDE EFFECT of filling the hhInfo array
     * with num of workers per hh.
     * @param nWorkersPerHh workers indexed by HhId
     * @return nPersons  (also produces a side effect)
     */
    private int readPersonFile(int[] nWorkersPerHh){

        return reader.getInfoFromPersonFile(nWorkersPerHh);
    }

    private void sendAutoOwnershipWork(int[] nWorkersByHhId){
        ArrayList attachment = new ArrayList();
        attachment.add(0, nWorkersByHhId);
        sendMicroSimulatedWork(NUM_HOUSEHOLDS, MessageID.CALCULATE_AUTO_OWNERSHIP, attachment);
    }

    private void assignSegmentToHhs(HashMap<Integer, Byte> autosByHhId){
        int incomeInHh;
        int nWorkersInHh;
        int nAutosInHh;
        int segment;

        for (Integer hhId : autosByHhId.keySet()) {
            incomeInHh = hhInfo[3][hhId];
            nWorkersInHh = hhInfo[2][hhId];
            nAutosInHh = autosByHhId.get(hhId);
            hhInfo[3][hhId] = nAutosInHh;
            segment = IncomeSegmenter.calcLogsumSegment(incomeInHh, nAutosInHh, nWorkersInHh);
            hhInfo[5][hhId] = segment;
        }
    }

    private void sendWorkplaceLocationWork(int[] hhSegments, int[] homeTazs){

        ArrayList attachment = new ArrayList();
        attachment.add(0, hhSegments);
        attachment.add(1, homeTazs);
        sendMicroSimulatedWork(NUM_PERSONS, MessageID.CALCULATE_WORKPLACE_LOCATIONS, attachment);
    }

    private void sendDCLogsumWork(){
        ArrayList<Message> messages = createDCLogsumWorkMessages();
        sendAggregateWork(dcWorkPorts, messages, "DC Logsum work");
    }


    private void sendSDTandLDTWork(){
        ArrayList attachment = new ArrayList();
        attachment.add(0, hhInfo[3]);   //this is the auto-ownership array (autos by HhId)
        attachment.add(1, hhInfo[2]);  //this is the workers per hh
        attachment.add(2, personHhIds);
        attachment.add(3, memberIds);
        attachment.add(4, workplaces);

        sendMicroSimulatedWork(hhInfo[1][0], MessageID.PROCESS_HOUSEHOLDS, attachment);
    }

    private void sendVisitorsWork(){
        ArrayList attachment = new ArrayList();
        ptDafMasterLogger.info(getName() + ", vm_nHhs: " + visitorsInfo[1] + " vm_nPers: " + NUM_VISITOR_PERSONS);
        sendMicroSimulatedWork(visitorsInfo[1], MessageID.PROCESS_VISITOR, attachment);
    }

    
    
    private void writeDoneFile(){

        // check to see if any debug files were created. This indicates that
            // there were tours that could
            // not find destinations or stops that couldn't find locations, etc.
            File doneFile = null;
            try {
                doneFile = new File(ResourceUtil.getProperty(ptRb, "sdt.done.file"));

                File debugDir = new File(ResourceUtil.getProperty(ptRb, "sdt.debug.files"));
                if (!debugDir.exists())
                    debugDir.mkdir();
                File[] debugFiles = debugDir.listFiles();
                if (debugFiles.length != 0) {
                    ptDafMasterLogger
                            .error("This run of the PTModel had unresolved problems.\n"
                                    + "Please look at the files in  "
                                    + debugDir.getAbsolutePath()
                                    + " for further info.");
                }

                // Signal to the File Monitor that the model is finished.
                ptDafMasterLogger
                        .info("Signaling to the File Monitor that the model is finished");
            } catch (Exception e) {
                String err = "Not able to check debug files.";
                logger.warn(err);
            }
            try {
                PrintWriter writer = new PrintWriter(new FileWriter(doneFile));
                writer.println("pt daf is done." + new Date());
                writer.close();
                ptDafMasterLogger.info("pt daf is done.");
            } catch (IOException e) {
                String errMsg = "Could not write done file.";
                logger.fatal(errMsg);
                throw new RuntimeException(errMsg);
            }
    }

    private ArrayList<Message> createMCLogsumWorkMessages(){

        ArrayList<Message> msgs = new ArrayList<Message>();
        int msgCounter = 0;

        // enter loop on segments
        for (int segment = 0; segment < PTHousehold.SEGMENTS; ++segment) {
            // enter loop on purposes (skip home purpose)
            for (ActivityPurpose purpose : ActivityPurpose.values()) {
                if (purpose == ActivityPurpose.HOME) {
                    continue;
                }
                Message mcLogsumMessage = mFactory.createMessage();
                mcLogsumMessage.setId(MessageID.CREATE_MC_LOGSUMS);
                mcLogsumMessage.setValue("msgNum", (msgCounter+1));
                mcLogsumMessage.setValue("purpose", purpose);
                mcLogsumMessage.setValue("segment", segment);

                msgs.add(msgCounter, mcLogsumMessage);
                msgCounter++;
            }
        }

        return msgs;
    }

    private ArrayList<Message> createDCLogsumWorkMessages(){

        ArrayList<Message> msgs =  new ArrayList<Message>();
        int msgCounter = 0;

        // enter loop on segments
        for (int segment = 0; segment < PTHousehold.SEGMENTS; ++segment) {

            Message dcLogsumMessage = mFactory.createMessage();
            dcLogsumMessage.setId(MessageID.CREATE_DC_LOGSUMS);
            dcLogsumMessage.setValue("msgNum", (msgCounter+1));
            dcLogsumMessage.setValue("segment", segment);

            msgs.add(msgCounter, dcLogsumMessage);
            msgCounter++;
         }

        return msgs;
    }

    private void sendAggregateWork (Port[] ports, ArrayList<Message> msgs, String workType){

        String queueName;
        for (int i = 0; i < msgs.size(); i++) {
            queueName = ports[i % ports.length].getName();
            ptDafMasterLogger.info(getName() + ", Sending  " + workType + " to " + queueName);
            ports[i % ports.length].send(msgs.get(i));

        }
    }

    /**
     * The MasterTask will send out the start and end row that of the hh file that the
     * worker should read in.  The row number is not the same as the hhId number.  We are assigning
     * rows in the hh file (or person file), not particular hhIds.
     * @param nAgents micro-simulation objects
     * @param msgId  what type of work to do
     * @param attachments necessary info
     */
    private void sendMicroSimulatedWork(int nAgents, String msgId, ArrayList attachments){

        int nAgentsPerNode = nAgents / msServerPorts.length;

        int startRow = 2;       //row 1 is the header row, workers should start reading file from 2nd row.
        int endRow = nAgentsPerNode + startRow - 1;
        for(int i=0; i< msServerPorts.length - 1; i++){  // the last node will get any remainder
            Message message = mFactory.createMessage();
            message.setId(msgId);
            message.setValue("startRow", startRow);
            message.setValue("endRow", endRow);
            message.setValue("attachments", attachments);
            String queueName = msServerPorts[i % msServerPorts.length].getName();
            ptDafMasterLogger.info(getName() + ", Sending rows " + startRow + " thru " + endRow
                        + " to " + queueName);
            msServerPorts[i%msServerPorts.length].send(message);
            startRow = endRow + 1;
            endRow += nAgentsPerNode;
        }

        //Treat the last message separately - make sure to get any remainder rows.
        endRow += nAgents % msServerPorts.length;

        Message message = mFactory.createMessage();
        message.setId(msgId);
        message.setValue("startRow", startRow);
        message.setValue("endRow", endRow);
        message.setValue("attachments", attachments);      //attachments could be null but that is OK
        String queueName = msServerPorts[msServerPorts.length -1].getName();
        ptDafMasterLogger.info(getName() + ", Sending rows " + startRow + " thru " + endRow
                    + " to " + queueName);
        msServerPorts[msServerPorts.length -1].send(message);
    }

    public void sendCloseFileMessageToWriter(){
        Message allDone = mFactory.createMessage();
        allDone.setId(MessageID.ALL_HOUSEHOLDS_PROCESSED);
        resultWriterPort.send(allDone);
    }

    private void receiveAndProcessMessages(){
        while(signal.isFalse()){
            Message msg = defaultPort.receive();

            //Message is null if timeout occurred
            if (msg == null) {
                continue;
            }
            onMessage(msg);
        }
        signal.setValue(false);
    }

    public static void signalResultsProcessed() {
        signal.setValue(true);
    }

    /**
     * Wait for message. If message is MC_LOGSUMS_CREATED,
     * startWorkplaceLocation If message is WORKPLACE_LOCATIONS_CALCULATED, add
     * the workers to the persons array, and check if done with all segments. If
     * done, Set TazDataArrays, which will update the zone data in each node
     * with the number of households and teachers in each TAZ. Add persons with
     * workplace locations to households. Sort the household array by worklogsum
     * segment and non-worklogsum segment. If message is TAZDATA_UPDATED, check
     * if all nodes have completed updating their data If done, startDCLogsums()
     * If message is DC_LOGSUMS_CREATED, check if all segments have been
     * completed. If done, startProcessHouseholds(): Send out initial blocks of
     * households to workers. If message is HOUSEHOLDS_PROCESSED Send households
     * to householdResults method, which will increment up
     * householdsProcessedCount and send households for writing to results file.
     * If households processed less than total households, sendMoreHouseholds()
     *
     * @param msg Message
     */
    public void onMessage(Message msg) {
        ptDafMasterLogger.info(getName() + " received " + msg.getId()
                + " message from=" + msg.getSender());

        if(msg.getId().equals(MessageID.SKIMS_READ)){
            nodesWithSkimsCount++;
            ptDafMasterLogger.info(getName() + ", Received Skims Read message ");
            StatusLogger.logText("pt.skims.read",msg.getSender() + " read skims - (t" + timePeriod + ")");
            if(nodesWithSkimsCount == msWorkQueues.size()){
                signalResultsProcessed();
            }

        } else if (msg.getId().equals(MessageID.MC_LOGSUMS_CREATED)
                || msg.getId().equals(MessageID.MC_LOGSUMS_COLLAPSED)) {
            if (msg.getId().equals(MessageID.MC_LOGSUMS_CREATED)) {
                mcLogsumCount++;
                ptDafMasterLogger.info(getName() + ", Received mc logsum message "
                        + msg.getIntValue("msgNum") + ".  (" + mcLogsumCount
                        + " of " + TOTAL_MCLOGSUMS + ")");
                StatusLogger.logHistogram("pt.mc.logsums","PT Status: MC Logsums (t" + timePeriod + ")",TOTAL_MCLOGSUMS,mcLogsumCount,"MC Logsums Processed","Logsums");
            } else {
                mcCollapsedLogsumCount++;
                ptDafMasterLogger.info(getName() + ", Received collapsed matrix " + mcCollapsedLogsumCount
                        + " of " + TOTAL_COLLAPSED_MCLOGSUMS);
                StatusLogger.logHistogram("pt.collapsed.mc.logsums","PT Status: Collapsed MC Logsums (t" + timePeriod + ")",TOTAL_COLLAPSED_MCLOGSUMS,mcCollapsedLogsumCount,"Collapsed MC Logsums Processed","Logsums");
            }

            if(runningSEAM){
                if(mcLogsumCount == TOTAL_MCLOGSUMS){
                    ptDafMasterLogger.info(getName() + ", Signaling that the ModeChoice Logsums are finished.");
                signalResultsProcessed();
                }
            } else if (mcLogsumCount == TOTAL_MCLOGSUMS
                    && mcCollapsedLogsumCount == TOTAL_COLLAPSED_MCLOGSUMS) {
                ptDafMasterLogger.info(getName() + ", Signaling that the ModeChoice Logsums are finished.");
                signalResultsProcessed();
            }


        } else if (msg.getId().equals(MessageID.AUTO_OWNERSHIP_CALCULATED)) {

            HashMap<Integer, Byte> autosByHhId = (HashMap<Integer, Byte>) msg.getValue("autosByHhId");
            hhsWithAutoChoiceMade += autosByHhId.keySet().size();
            assignSegmentToHhs(autosByHhId);
            StatusLogger.logHistogram("pt.auto.ownership","PT Status: Auto Ownership (t" + timePeriod + ")",NUM_HOUSEHOLDS,hhsWithAutoChoiceMade,"Auto Ownership Households Processed","Households");

            if(hhsWithAutoChoiceMade == NUM_HOUSEHOLDS){
                ptDafMasterLogger.info(getName() + ", Signaling that the AutoOwnership is finished.");
                signalResultsProcessed();
            }



        } else if (msg.getId().equals(MessageID.WORKPLACE_LOCATIONS_CALCULATED)) {

            tazManager.updateWorkers((HashMap<String, int[]>) msg.getValue("empInTazs"));
            storeWorkplaces(msg);
            personsWithWorkplaceCount += (Integer) msg.getValue("nPersonsProcessed");
            StatusLogger.logHistogram("pt.workplace.location","PT Status: Workplace Location (t" + timePeriod + ")",NUM_PERSONS,personsWithWorkplaceCount,"Workplace Location Persons Processed","Persons");

            if (personsWithWorkplaceCount == NUM_PERSONS) {
                String employmentFileName = ResourceUtil.getProperty(ptRb, "sdt.current.employment");
                tazManager.writeEmploymentFile(employmentFileName);


                signalResultsProcessed();
            }



        } else if (msg.getId().equals(MessageID.DC_LOGSUMS_CREATED)) {

            dcLogsumCount++;
            ptDafMasterLogger.info(getName() + "Received dc logsum message "
                        + msg.getIntValue("msgNum") + ".  (" + dcLogsumCount
                        + " of " + TOTAL_DCLOGSUMS + ")");
            StatusLogger.logHistogram("pt.dc.logsums","PT Status: DC Logsums (t" + timePeriod + ")",TOTAL_DCLOGSUMS,dcLogsumCount,"DC Logsums Processed","Logsums");

            if (dcLogsumCount == TOTAL_DCLOGSUMS) {
                ptDafMasterLogger.info(getName() + ", Signaling that the Destination Choice Logsums are finished.");
                signalResultsProcessed();
            }


        } else if (msg.getId().equals(MessageID.HOUSEHOLDS_PROCESSED)) {

            householdsProcessedCount += (Integer) msg.getValue("nHhs");
            ptDafMasterLogger.info("Households processed so far: "
                    + householdsProcessedCount + " of " + (numHhsProcessed)
                    + " sent.");
            StatusLogger.logHistogram("pt.households.processed","PT Status: Households (t" + timePeriod + ")",numHhsProcessed,householdsProcessedCount,"DC Logsums Processed","Logsums");

            // keep track of how many LDT tours we're expecting
            numLDTToursExpected += (Integer) msg.getValue("ldtToursExpected");
            
            if (householdsProcessedCount == (numHhsProcessed) 
            		&& numLDTToursExpected==numLDTToursProcessed
            		&& numLDTToursExpected>0) {
                ptDafMasterLogger.info(getName() + ", Signaling that the all Hhs have been processed.");
                signalResultsProcessed();
            }
            
        } else if (msg.getId().equals(MessageID.LDTTOURS_PROCESSED)) {

            numLDTToursProcessed += (Integer) msg.getValue("ldtToursProcessed");
            ptDafMasterLogger.info("LDT Tours processed so far: " + numLDTToursProcessed + " of " + numLDTToursExpected + " sent.");
            StatusLogger.logHistogram("pt.ldttours.processed","PT Status: LDT Tours",numLDTToursProcessed,numLDTToursExpected,"LDT Tours Expected","LDT Tours Processed");
             
            if (householdsProcessedCount == (numHhsProcessed) && numLDTToursExpected==numLDTToursProcessed) {
                ptDafMasterLogger.info(getName() + ", Signaling that the all Hhs have been processed.");
                signalResultsProcessed();
            }    
        }  else if (msg.getId().equals(MessageID.VISITOR_HHS_PROCESSED)) {

            visitorsProcessedCount += (Integer) msg.getValue("nHhs");
            ptDafMasterLogger.info("Visitor Households processed so far: "
                    + visitorsProcessedCount + " of " + (numVisitorsProcessed)
                    + " sent.");
            StatusLogger.logHistogram("pt.visitor.households.processed","PT Status: Visitor Households (t" + timePeriod + ")",numVisitorsProcessed,visitorsProcessedCount,"DC Logsums Processed","Logsums");

            if (visitorsProcessedCount == (numVisitorsProcessed)) {
                ptDafMasterLogger.info(getName() + ", Signaling that the all Hhs have been processed.");
                signalResultsProcessed();
            }
        }  else if (msg.getId().equals(MessageID.MODEL_TIMINGS)) {
            timingStatementsCount++;
            activityPattern += (Long) msg.getValue("activityPattern");
            tourScheduling += (Long) msg.getValue("tourScheduling");
            tourDestination += (Long) msg.getValue("tourDestination");
            tourMode += (Long) msg.getValue("tourMode");
            tourStops += (Long) msg.getValue("tourStops");
            stopDestination += (Long) msg.getValue("stopDestination");
            stopDuration += (Long) msg.getValue("stopDuration");
            tripMode += (Long) msg.getValue("tripMode");

            if(timingStatementsCount == msServerPorts.length){
                ptDafMasterLogger.info(getName() + ", Signaling that the all Timing statements have been collected.");
                signalResultsProcessed();
            }


        } else if (msg.getId().equals(MessageID.ALL_FILES_WRITTEN)) {

//            ptDafMasterLogger.info("PT model timings.");
//            ptDafMasterLogger.info("activityPattern: " + activityPattern);
//            ptDafMasterLogger.info("tourScheduling: " + tourScheduling);
//            ptDafMasterLogger.info("tourDestination: " + tourDestination);
//            ptDafMasterLogger.info("tourMode: " + tourMode);
//            ptDafMasterLogger.info("tourStops: " + tourStops);
//            ptDafMasterLogger.info("stopDestination: " + stopDestination);
//            ptDafMasterLogger.info("stopDuration: " + stopDuration);
//            ptDafMasterLogger.info("tripMode: " + tripMode);

            ptDafMasterLogger.info(getName() + ", Signaling that all files have been closed");
            signalResultsProcessed();


        }
    }

    public void initializeWorkplaceHolder(){
        int sum = 0;
        for(int nWorkers : hhInfo[2]){
            sum += nWorkers;
        }

        personHhIds = new int[sum];
        memberIds = new byte[sum];
        workplaces = new short[sum];

    }

    public void storeWorkplaces(Message msg){
        HashMap<String,Short> workplaceByPersonId = (HashMap<String, Short>) msg.getValue("workplaceByPersonId");


        for(String key : workplaceByPersonId.keySet()){
            personHhIds[index] = Integer.parseInt(key.substring(0, key.indexOf("_")));
            memberIds[index] = Byte.parseByte(key.substring(key.indexOf("_")+1, key.length()));
            workplaces[index] = workplaceByPersonId.get(key);
            index++;
        }
    }

    private void calculateNumHhsProcessed(){
        numHhsProcessed = NUM_HOUSEHOLDS /sampleRate;

    }
    
    // Currently no sample rate for visitor
    private void calculateNumVisitorsProcessed() {
        numVisitorsProcessed = visitorsInfo[1]/1;
    }




}
