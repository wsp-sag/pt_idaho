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
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * MSServerTask is a class that will distribute the MICRO-SIMULATION work
 * that is sent from the MasterTask to any workers that are on the node.
 * There should only be one of these tasks per node.
 *
 * @author Christi Willison
 * @version 1.0,  Jan 23, 2007
 */
public class MSServerTask extends MessageProcessingTask {
    Logger serverLogger = Logger.getLogger(MSServerTask.class);

    static int MAX_BLOCK_SIZE;

    ResourceBundle ptRb;
    ResourceBundle globalRb;
    int baseYear;

    int nHHWorkersOnThisNode = 0;
    int nPWorkersOnThisNode=0;
    ArrayList<String> localHHWorkQueues = new ArrayList<String>();
    ArrayList<String> localPWorkQueues = new ArrayList<String>();
    String nodeNumber;
    int ptSampleRate;
    int vmSampleRate;
    

    PTOccupationReferencer occRef;
    PTDataReader reader;
    VisitorDataReader vmReader;
    
    
    HashMap<String, Short> workTazByPersId;
    int[] autosByHhId;
    int[] workersByHhId;


    public void onStart(PTOccupationReferencer occRef){
        serverLogger.info("***" + getName() + ", started");
        nodeNumber = getName().trim().substring(12,13);

        ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
        String pathToPtRb = ResourceUtil.getProperty(runParamsRb,
                            "pathToAppRb");
        serverLogger.info(getName() + ", ResourceBundle Path: "
                    + pathToPtRb);
        String pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,
                    "pathToGlobalRb");
        serverLogger.info(getName() + ", ResourceBundle Path: "
                    + pathToGlobalRb);
        int baseYear = Integer.parseInt(ResourceUtil.getProperty(runParamsRb,
                    "baseYear"));
        serverLogger.info(getName() + ", Base Year: "
                    + baseYear);

        ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        globalRb = ResourceUtil.getPropertyBundle(new File(
                            pathToGlobalRb));

        MAX_BLOCK_SIZE = Integer.parseInt(ResourceUtil.getProperty(ptRb,
                "sdt.max.block.size"));

        this.occRef = occRef;
        ptSampleRate = ResourceUtil.getIntegerProperty(globalRb, "pt.sample.rate", 1);
        vmSampleRate=1;

//        int lowMax = ResourceUtil.getIntegerProperty(ptRb,"pt.low.max.income",20000);
//        int highMax = ResourceUtil.getIntegerProperty(ptRb,"pt.med.high.max.income",60000);
        PriceConverter priceConverter = PriceConverter.getInstance(globalRb,ptRb);
        //PRICE, not INCOME, because this is coming from a property file, not from synthetic population income levels
        int lowMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(ptRb,"pt.low.max.income",20000),PriceConverter.ConversionType.PRICE);
        int highMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(ptRb,"pt.med.high.max.income",60000),PriceConverter.ConversionType.PRICE);
        IncomeSegmenter.setIncomeCategoryRanges(lowMax, highMax);
        
   }

    public void onMessage(Message msg){
        serverLogger.info(getName() + " received messageId=" + msg.getId()
                + " message from=" + msg.getSender());

        if (msg.getId().equals(MessageID.WORKER_CHECKING_IN)){
            if(((String) msg.getValue("workQueueName")).contains("HH")){
                nHHWorkersOnThisNode ++;
                localHHWorkQueues.add((String) msg.getValue("workQueueName"));
            }else if(((String) msg.getValue("workQueueName")).contains("PN")){
                nPWorkersOnThisNode++;
                localPWorkQueues.add((String) msg.getValue("workQueueName"));
            }

        } else if(msg.getId().equals(MessageID.CALCULATE_AUTO_OWNERSHIP)){
            int startRow = (Integer) msg.getValue("startRow");
            int endRow = (Integer) msg.getValue("endRow");

            serverLogger.info(getName() + ", Node " + nodeNumber +
                    " working on SynPopH rows " + startRow + " thru " + endRow);
            sendWorkToWorkersOnThisNode(localHHWorkQueues, msg, new String[]{"workersByHhId"});

        } else if (msg.getId().equals(MessageID.CALCULATE_WORKPLACE_LOCATIONS)){
            int startRow = (Integer) msg.getValue("startRow");
            int endRow = (Integer) msg.getValue("endRow");

            serverLogger.info(getName() + ", Node " + nodeNumber +
                    " working on SynPopP rows " + startRow + " thru " + endRow);
            sendWorkToWorkersOnThisNode(localPWorkQueues, msg, new String[]{"segmentByHhId", "homeTazbyHhId"});

        } else if (msg.getId().equals(MessageID.PROCESS_HOUSEHOLDS)){
            int startRow = (Integer) msg.getValue("startRow");
            int endRow = (Integer) msg.getValue("endRow");

            serverLogger.info(getName() + ", Node " + nodeNumber +
                    " working on SynPopH rows " + startRow + " thru " + endRow);
            getHouseholdMessageInfo(msg);
            PTHousehold[] households = readHouseholds(msg); 
            startSendingHouseholds(households);  
            
    } else if (msg.getId().equals(MessageID.PROCESS_VISITOR)){
        int startRow = (Integer) msg.getValue("startRow");
        int endRow = (Integer) msg.getValue("endRow");

        serverLogger.info(getName() + ", Node " + nodeNumber +
                " working on VM SynPopH rows " + startRow + " thru " + endRow);
        openVisitorFiles();
        PTHousehold[] households = readVisitors(msg); 
        startSendingHouseholds(households);  
    }


    }

    private void sendWorkToWorkersOnThisNode(ArrayList<String> workQueues, Message msg, String[] attachNames){
        int startRow = (Integer) msg.getValue("startRow");
        int endRow = (Integer) msg.getValue("endRow");

        int nHhs = endRow - startRow + 1;

        int nHhsPerWorker = nHhs / workQueues.size();

        endRow = nHhsPerWorker + startRow - 1;
        for(int i=0; i< workQueues.size() - 1; i++){  // the last worker will get any remainder
            Message message = mFactory.createMessage();
            message.setId(msg.getId());
            message.setValue("startRow", startRow);
            message.setValue("endRow", endRow);
            for(int n=0; n < attachNames.length; n++){
                if(((ArrayList) msg.getValue("attachments")).get(n)==null) serverLogger.info(getName() + ", passing null");
                message.setValue(attachNames[n], ((ArrayList) msg.getValue("attachments")).get(n));
                serverLogger.info(getName() + ", Attached " + attachNames[n]);
            }

            String queueName = workQueues.get(i % workQueues.size());
            serverLogger.info(getName() + ", Sending rows " + startRow + " thru " + endRow
                        + " to " + queueName);
            sendTo(queueName, message);
            startRow = endRow + 1;
            endRow += nHhsPerWorker;
        }

        //Treat the last message separately - make sure to get any remainder rows.
        endRow += nHhs % workQueues.size();

        Message message = mFactory.createMessage();
        message.setId(msg.getId());
        message.setValue("startRow", startRow);
        message.setValue("endRow", endRow);
        for(int n=0; n < attachNames.length; n++){
            if(((ArrayList) msg.getValue("attachments")).get(n)==null) serverLogger.info(getName() + ", passing null");
            message.setValue(attachNames[n], ((ArrayList) msg.getValue("attachments")).get(n));
            serverLogger.info(getName() + ", Attached " + attachNames[n]);
        }
        String queueName = workQueues.get(workQueues.size() -1);
        serverLogger.info(getName() + ", Sending rows " + startRow + " thru " + endRow
                    + " to " + queueName);
        sendTo(queueName, message);
    }



    private void getHouseholdMessageInfo(Message msg){
        serverLogger.info(getName() + ", Processing incoming message");
        ArrayList attachment = (ArrayList) msg.getValue("attachments");
        autosByHhId = (int[])attachment.get(0);
        workersByHhId = (int[]) attachment.get(1);
        //add the persons workTaz IF the workplace location model was run

        if(attachment.get(2) != null){
            serverLogger.info(getName() + ", Creating workTazHashMap");
            int[] hhIds = (int[]) attachment.get(2);
            byte[] memIds = (byte[]) attachment.get(3);
            short[] workplaces = (short[]) attachment.get(4);
            workTazByPersId = new HashMap<String, Short>(hhIds.length);
            String key;
            for(int i=0 ; i<hhIds.length; i++){
                key = hhIds[i] + "_" + memIds[i];
                workTazByPersId.put(key, workplaces[i]);
            }
            serverLogger.info(getName() + ", Finished creating workTazHashMap");
        }

        //read the first line of the person file
        reader = new PTDataReader(ptRb, globalRb, occRef, baseYear);
        reader.createRandomNumberGenerator();
        reader.openPersonFile();
        reader.readPersonHeader();

     }

    
    private void openVisitorFiles(){
        serverLogger.info(getName() + ", Opening Person file");
        //add the persons workTaz IF the workplace location model was run

        //read the first line of the person file
        vmReader = new VisitorDataReader(ptRb, globalRb, occRef, baseYear);
        vmReader.createRandomNumberGenerator();
        vmReader.openPersonFile();
        vmReader.readPersonHeader();

     }



    /**
     * Starts the household processing by sending a specified number of
     * household blocks to each work queue.
     * @param households the array of households to send
     *
     */
    private void startSendingHouseholds(PTHousehold[] households) {
        serverLogger.info(getName() + ", Sending households.");

            //households are sorted by segment
        int blockCounter = 0;
        int householdCounter = 0;
        int blockSize;

        int[] numHHBlocksPerQueue = calculateHhBlocksPerQueue(households.length);
        int nBlocksToSend = 0;
        for(int nHHBlocks : numHHBlocksPerQueue){
            nBlocksToSend+=nHHBlocks;
        }
        // iterate through number of workers, num of hh blocks depends on the queue.
        while (nBlocksToSend > 0) {
            for (int q = 0; q < localHHWorkQueues.size(); q++) {
                if ( numHHBlocksPerQueue[q] > 0) {
                    blockCounter++;
                    // create an array of households
                    blockSize =MAX_BLOCK_SIZE < households.length - householdCounter? MAX_BLOCK_SIZE: households.length - householdCounter;
    //                remove
    //                serverLogger.info("Max block size: " + MAX_BLOCK_SIZE);
    //                serverLogger.info("households.length - householdCounter: " + (households.length - householdCounter) );
    //                serverLogger.info("block size: " + blockSize);
                    PTHousehold[] householdBlock = new PTHousehold[blockSize];

                    // fill it with households from the main household array
                    int addedHouseholds=0;
                    while(householdCounter<households.length && addedHouseholds < blockSize) {
                            householdBlock[addedHouseholds] = households[householdCounter];
                            addedHouseholds++;
                            householdCounter++;
                    }

                    Message processHouseholds = createMessage();
                    processHouseholds.setId(MessageID.PROCESS_HOUSEHOLDS);
                    processHouseholds.setValue("blockNumber", blockCounter);
                    processHouseholds.setValue("households", householdBlock);

                    String queueName = localHHWorkQueues.get(q);
                    serverLogger.info(getName() + ", Sending HH Block " + blockCounter + ", ( "
                            + householdBlock.length +" hhs) to " + queueName);
                    sendTo(queueName, processHouseholds);
                    serverLogger.info(getName() + "householdCounter = "+ householdCounter);

                    numHHBlocksPerQueue[q]--;
                    nBlocksToSend--;
                }

            }
        }

        serverLogger.info(getName() + ", Finished sending households.");
    }
    
    
 
    /**
     * A Similar method to startSendingHouseholds but this one uses the vmReader 
     * and sends visitor messages
     * @param msg
     */
    
    /*
    private void startSendingVisitors(Message msg) {
        serverLogger.info(getName() + ", Sending households.");

        PTHousehold[] visitors = readVisitors(msg);     //households are sorted by segment


        int blockCounter = 0;
        int visitorCounter = 0;
        int blockSize;

        int[] numHHBlocksPerQueue = calculateHhBlocksPerQueue(visitors.length);
        // iterate through number of workers, num of hh blocks depends on the queue.
        for (int q = 0; q < localHHWorkQueues.size(); q++) {
            for (int j = 0; j < numHHBlocksPerQueue[q]; j++) {

                blockCounter++;
                // create an array of households
                blockSize =MAX_BLOCK_SIZE < visitors.length - visitorCounter? MAX_BLOCK_SIZE: visitors.length - visitorCounter;
                PTHousehold[] householdBlock = new PTHousehold[blockSize];

                // fill it with households from the main household array
                int addedHouseholds=0;
                while(visitorCounter<visitors.length && addedHouseholds < blockSize) {
                        householdBlock[addedHouseholds] = visitors[visitorCounter];
                        addedHouseholds++;
                        visitorCounter++;
                }

                Message processVisitors = createMessage();
                processVisitors.setId(MessageID.PROCESS_VISITOR);
                processVisitors.setValue("blockNumber", blockCounter);
                processVisitors.setValue("households", householdBlock);

                String queueName = localHHWorkQueues.get(q);
                serverLogger.info(getName() + ", Sending Visitor Block " + blockCounter + ", ( "
                        + householdBlock.length +" hhs) to " + queueName);
                sendTo(queueName, processVisitors);
                serverLogger.info(getName() + "VisitorCounter = "+ visitorCounter);

            }
        }

        serverLogger.info(getName() + ", Finished sending households.");
    }
    */
    

    private PTHousehold[] readHouseholds(Message msg){
        int startRow = (Integer) msg.getValue("startRow");
        int endRow = (Integer) msg.getValue("endRow");
        serverLogger.info(getName() + ", Reading in " + (endRow-startRow+1)/ptSampleRate + " hhs");

        PTHousehold[] hhs = reader.readHouseholds(startRow, endRow, ptSampleRate);

        for(PTHousehold hh : hhs){
            hh.autos = (byte) autosByHhId[hh.ID];
            hh.workers = (byte)workersByHhId[hh.ID];

            //Find persons in the hh (will check for the existence of the workTaz
            hh.persons = reader.readPersonsForTravelModels(hh.size, hh.ID);
            for(PTPerson person : hh.persons){
                person.homeTaz = hh.homeTaz;
            }


            //add the persons workTaz IF the workplace location model was run
            if(workTazByPersId != null){
                for(PTPerson person : hh.persons){
                    try {
                        if(person.employed){
                            person.workTaz = workTazByPersId.get(person.hhID + "_" + person.memberID);
                        }
                    } catch (Exception e) {
                        serverLogger.info(getName() + "Couldn't find workplace for person: " + (person.hhID + "_" + person.memberID));
                        serverLogger.info(getName() + "Key is in hashmap: " + workTazByPersId.containsKey(person.hhID + "_" + person.memberID));
                        if(workTazByPersId.containsKey(person.hhID + "_" + person.memberID)){
                            serverLogger.info(getName() + "Value is " + workTazByPersId.get(person.hhID + "_" + person.memberID));
                        }
                        throw new RuntimeException("Can't find person's work taz");
                    }
                }
           }

        }
        reader.closePersonFile();

        Arrays.sort(hhs);
        serverLogger.info(getName() + ", Finished reading in " + hhs.length + " hhs");
        return hhs;

    }
    
    
    private PTHousehold[] readVisitors(Message msg){
        int startRow = (Integer) msg.getValue("startRow");
        int endRow = (Integer) msg.getValue("endRow");
        serverLogger.info(getName() + ", Reading in " + (endRow-startRow+1)/vmSampleRate + " hhs");

        PTHousehold[] vmHHs = vmReader.readHouseholds(startRow, endRow, vmSampleRate, true);

        for(PTHousehold hh : vmHHs){
            hh.autos = 1;
            hh.workers = 0;

            //Find persons in the hh (will check for the existence of the workTaz
            hh.persons = vmReader.readPersonsForTravelModels(hh.size, hh.ID);
            for(PTPerson person : hh.persons){
                person.homeTaz = hh.homeTaz;
            }
        }
        vmReader.closePersonFile();

        Arrays.sort(vmHHs);
        serverLogger.info(getName() + ", Finished reading in " + vmHHs.length + " hhs");
        return vmHHs;

    }
    
    
    //number of hhs being sent = nHHs / sampleRate + 1 (hhCounter starts at 0)
        //if(numbers of hhs being sent % max_block_size == 0)
        //    number of blocks being sent = number of hhs being sent / max_block_size
        //else
        //   number of blocks being sent = (number of hhs being sent / max_block_size) + 1
        //
        //if(number of blocks being sent % nHHWorkQueues == 0)
        //    number of blocks per queue = nBlocksBeingSent / nHHWorkQueues
        //else
        //   number of blocks per queue = nBlocksBeingSent / nHHWorkQueues for 1..n-1 queues
        //                              = nBlocksBeingSent /nHHWorkQueues + 1 for last queue.
    private int[] calculateHhBlocksPerQueue(int nHHs){

        MAX_BLOCK_SIZE = (nHHs / localHHWorkQueues.size()) > MAX_BLOCK_SIZE ? MAX_BLOCK_SIZE: nHHs / localHHWorkQueues.size();

        int nBlocksBeingSent;
        if(nHHs % MAX_BLOCK_SIZE == 0){
            nBlocksBeingSent = nHHs / MAX_BLOCK_SIZE;
        }else{
            nBlocksBeingSent = (nHHs / MAX_BLOCK_SIZE) + 1;
        }

        int[] nBlocksPerQueue = new int[localHHWorkQueues.size()];
        int index = 0;
        while (nBlocksBeingSent > 0){
            nBlocksPerQueue[index % localHHWorkQueues.size()]++;
            index++;
            nBlocksBeingSent--;
        }

        return nBlocksPerQueue;
    }
}
