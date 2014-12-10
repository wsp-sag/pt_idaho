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
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;

import static com.pb.models.pt.daf.MessageID.DC_LOGSUMS_CREATED;
import com.pb.models.pt.util.SkimsInMemory;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * DCLogsumCalculator is a class that ...
 *
 * @author Christi Willison
 * @version 1.0,  Jan 19, 2007
 */
public class DCLogsumCalculatorTask extends MessageProcessingTask {
    protected Logger dcLogger = Logger.getLogger(DCLogsumCalculatorTask.class);
    protected static final Object lock = new Object();
    protected static boolean initialized = false;
    protected static String matrixWriterQueue = "DestinationChoiceWriterQueue";

    public static SkimsInMemory skims;
    protected static ResourceBundle ptRb;
    protected static ResourceBundle globalRb;

    //private TazManager tazManager;
    int[] extNumbers;
    private TourModeChoiceLogsumManager mcLogsums;
    TourDestinationChoiceModel model;


    public void onStart(){
        synchronized (lock) {
            dcLogger.info(getName() + ", Started");
            if (!initialized) {
                dcLogger.info(getName() + ", Initializing DCLogsum Task");
                // We need to read in the Run Parameters (pathToResourceBundle)
                //  from the RunParams.properties file
                // that was written by the Application Orchestrator
                String pathToPtRb;
                String pathToGlobalRb;

                dcLogger.info(getName() + ", Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,
                        "pathToAppRb");
                dcLogger.info(getName() + ", ResourceBundle Path: "
                        + pathToPtRb);
                pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,
                        "pathToGlobalRb");
                dcLogger.info(getName() + ", ResourceBundle Path: "
                        + pathToGlobalRb);

                ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                globalRb = ResourceUtil.getPropertyBundle(new File(
                        pathToGlobalRb));

                //initialize price converter
                PriceConverter.getInstance(ptRb,globalRb);

                skims = SkimsInMemory.getSkimsInMemory();

                initialized = true;
                dcLogger.info(getName() + ", Finished initializing");
            }
       }


    }

    public void onMessage(Message msg) {
        dcLogger.info(getName() + ", Received messageId=" + msg.getId()
                + " message from=" + msg.getSender() + ". MsgNum: " + msg.getIntValue("msgNum"));

        //createGradeschoolDCLogsums(msg);
        createDCLogsums(msg);
    }


    /**
     * Create destination choice aggregate logsums
     *
     * @param msg Message
     */
    public void createDCLogsums(Message msg) {
        initialize();

        int segment = (Integer) msg.getValue("segment");
        dcLogger.info(getName()+ ", Creating Destination Choice Logsums Vectors for segment: " + segment);
        int msgNum = ((msg.getIntValue("msgNum") - 1) * (ActivityPurpose.values().length-2))+1;

        Matrix mc;
        float[] logsums;
        for (ActivityPurpose purpose : ActivityPurpose.values()) {
            if (purpose == ActivityPurpose.HOME || purpose == ActivityPurpose.WORK) {
                continue;
            }
            mc = mcLogsums.getLogsumMatrix(purpose, segment);
            logsums = TourDestinationChoiceLogsums.createLogsums(
                    purpose, extNumbers, mc, skims, model);

            Message message = createMessage();
            message.setId(DC_LOGSUMS_CREATED);
            message.setValue("msgNum", msgNum);
            message.setValue("logsums", logsums);
            message.setValue("extNumbers", extNumbers);
            message.setValue("segment", segment);
            message.setValue("purpose", purpose);
            sendTo(matrixWriterQueue, message);

            msgNum++;
        }

    }

    /**
     * Create destination choice aggregate logsums
     *
     * @param msg Message
     */
    public void createGradeschoolDCLogsums(Message msg) {
        initialize();

        int segment = (Integer) msg.getValue("segment");
        dcLogger.info(getName()+ ", Creating Destination Choice Logsums Vectors for segment: " + segment);
        int msgNum = ((msg.getIntValue("msgNum") - 1) * (ActivityPurpose.values().length-2))+1;

        Matrix mc;
        float[] logsums;
        for (ActivityPurpose purpose : ActivityPurpose.values()) {
            if (purpose != ActivityPurpose.GRADESCHOOL) {
                continue;
            }
            dcLogger.info(getName()+ ", Creating Destination Choice Logsums Vectors for purpose: " + purpose);
            dcLogger.info(getName()+ ", Reading Mode Choice Logsums Vectors for purpose: " + purpose);

            mc = mcLogsums.getLogsumMatrix(purpose, segment);
            logsums = TourDestinationChoiceLogsums.createLogsums(
                    purpose, extNumbers, mc, skims, model);

            Message message = createMessage();
            message.setId(DC_LOGSUMS_CREATED);
            message.setValue("msgNum", msgNum);
            message.setValue("logsums", logsums);
            message.setValue("extNumbers", extNumbers);
            message.setValue("segment", segment);
            message.setValue("purpose", purpose);
            sendTo(matrixWriterQueue, message);

            msgNum++;
        }

    }

    private void initialize(){
        if (model == null ) {
            String tazManagerClassName = ResourceUtil.getProperty(ptRb,"sdt.taz.manager.class");
            Class tazManagerClass = null;
            TazManager tazManager = null;
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
            // inits for non-static members
            // read workplace locations from file
            dcLogger.info(getName() + ", Reading employment from file");
            String filePath = ResourceUtil.getProperty(ptRb, "sdt.current.employment");
            if(!new File(filePath).exists()){
                filePath = ResourceUtil.getProperty(ptRb, "sdt.previous.employment");
            }
            tazManager.updateWorkersFromSummary(filePath);

            model = new TourDestinationChoiceModel(ptRb);
            model.buildModel(tazManager);

            mcLogsums = new TourModeChoiceLogsumManager(globalRb, ptRb);
            extNumbers = tazManager.getExternalNumberArrayZeroIndexed();

        }
    }

    private void cleanup(){
        dcLogger = null;
        matrixWriterQueue = null;
        skims = null;
        ptRb = null;
        globalRb = null;
        extNumbers = null;
        mcLogsums = null;
        model = null;    
    }

    public static void main(String[] args) {

        DCLogsumCalculatorTask dcCalc = new DCLogsumCalculatorTask();
        dcCalc.name = "DCCalculatorTask";
        dcCalc.onStart();

        SkimsInMemory skims = SkimsInMemory.getSkimsInMemory();
        if(!skims.isReady()){
            System.out.println(", Reading Skims into memory");
            skims.setGlobalProperties(globalRb);
            skims.readSkims(ptRb);
        }

        Message msg = dcCalc.mFactory.createMessage();
        msg.setId(MessageID.CREATE_DC_LOGSUMS);
        msg.setValue("msgNum", (1));
        msg.setValue("segment", 2);

        dcCalc.createGradeschoolDCLogsums(msg);
    }

}
