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
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.TourDestinationChoiceLogsums;
import static com.pb.models.pt.daf.MessageID.DC_LOGSUMS_CREATED;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * This class is used for collecting the dc logsums from the
 * various workers.  Once all logsums are in, the class will
 * write them out to disk.  There should be at most one of these
 * classes for the PTDAF application and it should be on the
 * FileServer node.
 *
 * Author: Christi Willison
 * Date: Jan 31, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class DCWriterTask extends MessageProcessingTask {
    Logger logger = Logger.getLogger(DCWriterTask.class);

    ResourceBundle appRb;
    String alphaName;

    private static int dcCount = 0;

    private int TOTAL_DCLOGSUMS = (ActivityPurpose.values().length - 2)
            * PTHousehold.SEGMENTS; // no logsums for work and home purposes


    public void onStart() {
        logger.info(getName() + ", Reading RunParams.properties file");
        ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
        String pathToPtRb = ResourceUtil.getProperty(runParamsRb,"pathToAppRb");
        logger.info(getName() + "\tResourceBundle Path: " + pathToPtRb);
        String pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,"pathToGlobalRb");
        logger.info(getName() + "\tResourceBundle Path: " + pathToGlobalRb);

        appRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));
        alphaName = globalRb.getString("alpha.name");			
    }

    public void onMessage(Message msg) {
        logger.info(getName() + " received messageId=" + msg.getId()
                + " message from=" + msg.getSender() +" Msg number: " + msg.getValue("msgNum") );

        if (msg.getId().equals(DC_LOGSUMS_CREATED)) {
            //On first message initialize the TourDestinationChoiceLogsumManager
            if (dcCount==0) {
                int[] extNumbers = (int[]) msg.getValue("extNumbers");
                TourDestinationChoiceLogsums.createTourDestinationChoiceLogsumsTable(extNumbers, alphaName);
            }
            float[] logsum = (float[]) msg.getValue("logsums");
            ActivityPurpose purpose = (ActivityPurpose) msg.getValue("purpose");
            int segment = (Integer) msg.getValue("segment");

            TourDestinationChoiceLogsums.setLogsumsInTable(purpose, segment, logsum);
            dcCount ++;

            if (dcCount == TOTAL_DCLOGSUMS) {
                logger.info("Writing DC logsums to disk.");
                TourDestinationChoiceLogsums.writeLogsums(appRb);
            }

            sendTo("TaskMasterQueue", msg);

        } else {
            logger.info("DestinationChoiceWriter initialized.");
        }
    }
}
