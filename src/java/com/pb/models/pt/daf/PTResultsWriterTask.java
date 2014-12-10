/*
 * Copyright 2006 PB Consult Inc.
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
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTResults;
import static com.pb.models.pt.daf.MessageID.WORKPLACE_LOCATIONS_CALCULATED;
import com.pb.models.pt.ldt.LDTDataWriter;
import com.pb.models.pt.ldt.LDTReporter;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.LDTrip;
import com.pb.models.pt.ldt.RunLDTModels;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * New file writer that works with the newly distributed PT code
 * @author willison
 *
 */
public class PTResultsWriterTask extends MessageProcessingTask {
    protected Logger resultsWriterLogger = Logger
            .getLogger(PTResultsWriterTask.class);

    private static ResourceBundle ptRb;
    private static ResourceBundle globalRb;

    boolean firstMessage = true;

    private LDTDataWriter ldtDataWriter;
    private LDTReporter ldtReporter;

    boolean calcSDT;
    boolean calcLDT;
    boolean calcVM;


    PTResults results;

    public void onStart() {
        resultsWriterLogger.info("***" + getName() + " started");

        ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
        String pathToPtRb = ResourceUtil.getProperty(runParamsRb, "pathToAppRb");
        ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        String pathToGlobalRb = ResourceUtil.getProperty(runParamsRb, "pathToGlobalRb");
        globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));
        calcSDT = ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.sdt", true);
        calcLDT = ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.ldt", true);
        calcVM = ResourceUtil.getBooleanProperty(ptRb, "sdt.calculate.vm", false);

    }

    public void onMessage(Message msg) {
        String msgId = msg.getId();
        resultsWriterLogger.info(getName() + " received "
                + msgId + " message from=" + msg.getSender());


        if (firstMessage) {
            if(calcSDT || calcVM)
                results = new PTResults(ptRb,globalRb);

            if(calcSDT){
                resultsWriterLogger.info("Creating resident output files");
                results.createFiles();
            }

            if ( calcLDT ) {
                ldtDataWriter = new LDTDataWriter(ptRb, globalRb);
                ldtReporter = new LDTReporter(ptRb);
            }

            if(calcVM){
                resultsWriterLogger.info("Creating visitor output files");
                results.createVisitorFiles();
            }

            firstMessage = false;
        }

        if (msg.getId().equals(WORKPLACE_LOCATIONS_CALCULATED)) {
            HashMap<String,Integer> personInfo = (HashMap<String, Integer>) msg.getValue("workplaceByPersonId");
            PTResults.writeWorkPlaceLocations(ptRb, personInfo);
            sendTo("TaskMasterQueue", msg);

        } else if (msg.getId().equals(MessageID.LDT_HOUSEHOLDS)) {
            LDTour[] ldTours = (LDTour[]) msg.getValue("tours");
            LDTrip[] ldTrips = RunLDTModels.createTrips(ldTours);
            ldtDataWriter.writePersonTours(ldTours);
            ldtDataWriter.writePersonTrips(ldTrips);
            ldtDataWriter.writeAssignmentTrips(ldTrips);
            ldtReporter.countTourLevelDecisions(ldTours, ldTrips);


            Message masterMsg = createMessage();
            masterMsg.setId(MessageID.LDTTOURS_PROCESSED);
            masterMsg.setValue("ldtToursProcessed", ldTours.length);
            logger.info("Forwarding the LDT processed message to Task Master.  " + ldTours.length + " LDT tours have been processed");
            sendTo("TaskMasterQueue", masterMsg);

            resultsWriterLogger.info(getName() + " " + ldTours.length + " LDT tours have been written out");
            
        } else if (msgId.equals(MessageID.VISITOR_HHS_PROCESSED)) {
            PTHousehold[] hhs = (PTHousehold[]) msg.getValue("households");
            writeVisitorResults(hhs);

            Message masterMsg = createMessage();
            masterMsg.setId(MessageID.VISITOR_HHS_PROCESSED);
            masterMsg.setValue("nHhs", hhs.length);
            logger.info("Forwarding the VM HH processed message to Task Master.  " + hhs.length + " VM households have been processed");
            sendTo("TaskMasterQueue", masterMsg);

            resultsWriterLogger.info(getName() + " " + hhs.length + " visitor households have been written out");

        } else if (msgId.equals(MessageID.HOUSEHOLDS_PROCESSED)) {
                PTHousehold[] hhs = (PTHousehold[]) msg.getValue("households");
                int numLDTTours = (Integer) msg.getValue("ldtToursExpected");
                if (calcSDT) {
                	writeResults(hhs);
                	results.calcSummaries(hhs);
                }

                Message masterMsg = createMessage();
                masterMsg.setId(MessageID.HOUSEHOLDS_PROCESSED);
                masterMsg.setValue("nHhs", hhs.length);
                masterMsg.setValue("ldtToursExpected", numLDTTours);
                logger.info("Forwarding the HH processed message to Task Master.  " + hhs.length + " households have been processed");
                sendTo("TaskMasterQueue", masterMsg);

                resultsWriterLogger.info(getName() + " " + hhs.length + " households have been written out");

        } else if (msg.getId().equals(MessageID.ALL_HOUSEHOLDS_PROCESSED)) {
            if(calcSDT){
            	results.writeSummaryFiles();
                results.close();
            }

            if(calcVM){
                results.closeVisitorFiles();
            }
            
            if (calcLDT ) {
                ldtDataWriter.close();
                ldtReporter.logTourLevelDecisions();
            }
            Message filesWritten = createMessage();
            filesWritten.setId(MessageID.ALL_FILES_WRITTEN);
            sendTo("TaskMasterQueue", filesWritten);
        }
    }



    /**
     * Get the households from the message and send the
     * households to the writeResults method of the results object.
     * @param hhs PTHousehold array
     */
    private void writeResults(PTHousehold[] hhs) {
        results.writeResults(hhs);
    }

    /**
     * Get the households from the message and send the
     * households to the writeResults method of the results object.
     * @param hhs PTHousehold array
     */
    private void writeVisitorResults(PTHousehold[] hhs) {
        results.writeVisitorResults(hhs);
    }

}
