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
import com.pb.models.pt.PriceConverter;
import com.pb.models.pt.util.SkimsInMemory;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Feb 3, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class SkimsReaderTask extends MessageProcessingTask {
    Logger skimsLogger = Logger.getLogger(SkimsReaderTask.class);

    public void onStart(){

        skimsLogger.info(getName() + ", Started");

        // We need to read in the Run Parameters (pathToResourceBundle)
        //  from the RunParams.properties file
        // that was written by the Application Orchestrator
        String pathToPtRb;
        String pathToGlobalRb;

        skimsLogger.info(getName() + ", Reading RunParams.properties file");
        ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
        pathToPtRb = ResourceUtil.getProperty(runParamsRb,
                "pathToAppRb");
        skimsLogger.info(getName() + ", ResourceBundle Path: "
                + pathToPtRb);
        pathToGlobalRb = ResourceUtil.getProperty(runParamsRb,
                "pathToGlobalRb");
        skimsLogger.info(getName() + ", ResourceBundle Path: "
                + pathToGlobalRb);

        ResourceBundle ptRb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File(
                pathToGlobalRb));

        //initialize price converter
        PriceConverter.getInstance(ptRb,globalRb);

        SkimsInMemory skims = SkimsInMemory.getSkimsInMemory();
        if(!skims.isReady()){
            skimsLogger.info(getName() +  ", Reading Skims into memory");
            skims.setGlobalProperties(globalRb);
            skims.readSkims(ptRb);
        }

        Message skimsReadMsg = mFactory.createMessage();
        skimsReadMsg.setId(MessageID.SKIMS_READ);
        sendTo("TaskMasterQueue", skimsReadMsg);
    }

 }
