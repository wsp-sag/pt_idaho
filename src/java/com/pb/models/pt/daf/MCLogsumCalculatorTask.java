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
import com.pb.common.daf.MessageFactory;
import com.pb.common.daf.MessageProcessingTask;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import com.pb.models.pt.util.SkimsInMemory;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * MCLogsumCalculatorTask is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Jan 19, 2007
 */
public class MCLogsumCalculatorTask  extends MessageProcessingTask {
    protected Logger mcLogger = Logger.getLogger(MCLogsumCalculatorTask.class);
    protected static final Object lock = new Object();
    protected static boolean initialized = false;
    protected static String matrixWriterQueue = "ModeChoiceWriterQueue";

    public static SkimsInMemory skims;
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;


    private TazManager tazManager;


    private TourModeChoiceLogsumManager mcLogsums;



    public void onStart(){
        synchronized (lock) {
            mcLogger.info(getName() + ", Started");
            if (!initialized) {
                mcLogger.info(getName() + ", Initializing MCLogsum Task");
                // We need to read in the Run Parameters (pathToResourceBundle)
                //  from the RunParams.properties file
                // that was written by the Application Orchestrator
                String pathToPtRb;
                String pathToGlobalRb;

                mcLogger.info(getName() + ", Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,
                        "pathToAppRb");
                mcLogger.info(getName() + ", ResourceBundle Path: "
                        + pathToPtRb);
                pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,
                        "pathToGlobalRb");
                mcLogger.info(getName() + ", ResourceBundle Path: "
                        + pathToGlobalRb);

                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(
                        pathToGlobalRb));

                //initialize price converter
                PriceConverter.getInstance(ptRb,globalRb);

                skims = SkimsInMemory.getSkimsInMemory();
                initialized = true;
                mcLogger.info(getName() + ", Finished initializing parent object");
            }
       }
    }

    public void onMessage(Message msg) {
        mcLogger.info(getName() + ", Received messageId=" + msg.getId()
                + " message from=" + msg.getSender() + ". MsgNum: " + msg.getIntValue("msgNum"));
        ActivityPurpose purpose = (ActivityPurpose)(msg.getValue("purpose"));
        Integer segment = (Integer) msg.getValue("segment");

        Matrix logsum = createMCLogsums(purpose, segment);

        mcLogger.info(getName() + ", Sending msg " + msg.getIntValue("msgNum")
        + ".  Purpose: " +  purpose + ", segment: " + segment + " to " + matrixWriterQueue);
        sendMCLogsumToWriter(msg, logsum);

    }

    public Matrix createMCLogsums(ActivityPurpose purpose, Integer segment) {
        if(tazManager == null){
            String tazManagerClassName = ResourceUtil.getProperty(ptRb,"sdt.taz.manager.class");
            Class tazManagerClass = null;
            tazManager = null;
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

            mcLogsums = new TourModeChoiceLogsumManager(globalRb, ptRb);
        }

        // Creating the ModeChoiceLogsum Matrix
        mcLogger.info(getName()+ ", Creating Mode Choice Logsum Matrix for purpose: "
                    + purpose + " segment: " + segment);
        return mcLogsums.createLogsumMatrix(purpose, segment, tazManager, skims);
   }

    public void sendMCLogsumToWriter(Message msg, Matrix m){
        msg.setId(MessageID.MC_LOGSUMS_CREATED);
        msg.setValue("matrix", m);
        sendTo(matrixWriterQueue, msg);
    }

    private void cleanup(){
        mcLogger = null;
        skims = null;
        ptRb = null;
        globalRb = null;
        tazManager = null;
        mcLogsums = null;
    }

    public static void main(String[] args) {
        int TOTAL_SEGMENTS = PTHousehold.SEGMENTS;
        MessageFactory mFactory = MessageFactory.getInstance();
        MCLogsumCalculatorTask mcCalc = new MCLogsumCalculatorTask();
        mcCalc.onStart();

        System.out.println("Creating aggregate mode choice logsums.");
        int msgCounter = 0;

        // enter loop on segments
        for (int segment = 0; segment < TOTAL_SEGMENTS; ++segment) {
            // enter loop on purposes (skip home purpose)
            for (ActivityPurpose purpose : ActivityPurpose.values()) {
                if (purpose == ActivityPurpose.HOME) {
                    continue;
                }
                Message mcLogsumMessage = mFactory.createMessage();
                msgCounter++;
                mcLogsumMessage.setId(MessageID.CREATE_MC_LOGSUMS);
                mcLogsumMessage.setValue("purpose", purpose);
                mcLogsumMessage.setValue("segment", segment);
                mcLogsumMessage.setValue("id", msgCounter);

                mcCalc.onMessage(mcLogsumMessage);
                System.out.println("Segment " + segment + ", Purpose " + purpose + " sent to MCLogsum" +
                "Calculator Task.");
            }
        }
    }
}
