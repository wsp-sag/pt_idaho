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
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;

/**
 * Writes out the mode choice logsums
 * @author hansens
 *
 */

public class MCWriterTask extends MessageProcessingTask {
    static Logger matrixWriterLogger = Logger.getLogger(MCWriterTask.class);

    protected static final Object lock = new Object();
    protected static boolean initialized = false;

    static ResourceBundle rb;

    static String extension = null;
    static String modeChoiceLogsumsWritePath = null;

    private static int mcCollaspedCount = 0;

    Matrix m;

    public void onStart() {
    	synchronized (lock) {
            matrixWriterLogger.info( "***" + getName() + " started");
            if (!initialized) {
                //We need to read in the Run Parameters (pathToResourceBundle) from the RunParams.txt file
                //that was written by the Application Orchestrator
                String pathToPtRb;

                matrixWriterLogger.info(getName() + ", Reading RunParams.properties file");
                ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
                pathToPtRb = ResourceUtil.getProperty(runParamsRb,"pathToAppRb");
                matrixWriterLogger.info(getName() + ",\tResourceBundle Path: " + pathToPtRb);

                rb = ResourceUtil.getPropertyBundle(new File(pathToPtRb));
                extension = ResourceUtil.getProperty(rb, "matrix.extension", ".zmx");
                modeChoiceLogsumsWritePath = ResourceUtil.getProperty(rb, "sdt.current.mode.choice.logsums");

                initialized = true;
            }


        }

    }

    public void onMessage(Message msg) {
        matrixWriterLogger.info(getName() + " received messageId=" + msg.getId()
                + " message from=" + msg.getSender() );
        if (msg.getId().equals(MessageID.MC_LOGSUMS_CREATED)) {
            matrixWriterLogger.info("\t" + getName() + ", Msg Num: " + msg.getIntValue("msgNum"));
            writeMatrix(msg, modeChoiceLogsumsWritePath);
        } else if (msg.getId().equals(MessageID.MC_LOGSUMS_COLLAPSED)) {
            mcCollaspedCount += 1;
            matrixWriterLogger.info("\t" + getName() + ", Msg Num: " + mcCollaspedCount);
            writeMatrix(msg, modeChoiceLogsumsWritePath);
        } else {
            matrixWriterLogger.info(getName() + ", MatrixWriter initialized.");
        }
    }

    private void writeMatrix(Message msg, String path) {

        m = (Matrix) (msg.getValue("matrix"));
        long startTime = System.currentTimeMillis();
        
        /*		[AK]
        MatrixWriter mw = MatrixWriter.createWriter(MatrixType.ZIP,				
                new File(path + m.getName() + extension)); // Open for writing
        mw.writeMatrix(m);
		*/

        MatrixWriter.writeMatrix(new File(path + m.getName() + extension), m);
        
        matrixWriterLogger.info(getName() + ", Wrote matrix " + (path + m.getName()) + extension
                + " in " + (System.currentTimeMillis() - startTime)
                / 1000.0 + " seconds");

        msg.setValue("matrix", null);
        sendTo("TaskMasterQueue", msg);
    }


}
