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
 *   Created on Mar 8, 2006 by Rosella Picado <picado@pbworld.com>
 */
package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * StopPurpose2TourTableReader
 * 
 * This class reads the frequency table for tours in 2 tour patterns
 * of stop purpose ocurrance for the Intermediate Stop Purpose Models.
 * 
 * @version 0.2
 * @author Picado
 * 
 */
public class StopPurpose2TourTableReader {

    final static Logger logger = Logger
            .getLogger(StopPurpose2TourTableReader.class);
    //TODO - A comment here indicating what the 3,2,and 3 stand for would be good
    //TODO - Better would be to get them from code and not have a hard-coded value
    private float [][][][][] likelihood =
            new float[PersonType.values().length][3][2][ActivityPurpose.values().length][3];

    private Tracer tracer = Tracer.getTracer();

    /**
     * This method reads the intermediate stop purpose model parameters.
     * @param rb Resource Bundle
     */

    public StopPurpose2TourTableReader(ResourceBundle rb) {
        float parameters[][];

        // read the stop purpose model parameters

        String fileName = ResourceUtil.getProperty(rb,
                "sdt.stop.purpose.parameters2T");
        
        logger
                .info("Reading Intermediate Tour Stop Purpose Parameters from file "
                        + fileName);
        try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(
                    fileName));

            parameters = table.getValues();

        } catch (IOException e) {
            logger.fatal("Can't read Tour Stop Purpose Model Parameters file "
                    + fileName);
            throw new RuntimeException(e);
        }

        // fill the likelihood array
        for (float[] parameter : parameters) {

            likelihood[(int) parameter[0] - 1]   //personType
                    [(int) parameter[1]]     //tourPosition
                    [(int) parameter[2] - 1]   //stop
                    [(int) parameter[3]]     //tourPurpose
                    [(int) parameter[4] - 1]   //stopPurpose
                    = parameter[5];

        }

        if (tracer.isTraceOn()) {

            logger.info("Printing tour stop purpose model parameters ...");

            for (int i = 0; i < parameters.length; ++i) {

                logger.info("row " + i + ": " + " persontype "
                        + parameters[i][0] + " tour position " + parameters[i][1]
                        + " stop number " + parameters[i][2] + " tour purpose "
                        + parameters[i][3] + " stop purpose "
                        + parameters[i][4] + " likelihood " + parameters[i][5]);
            }
        }

    }

    /**
     * This method returns the probability distribution of stop purposes, given
     * the person type, tour purpose and position, and stop number.
     * @param ptype person type
     * @param tourPosition tour position
     * @param stop stop number
     * @param tpurp Tour purpose
     * @return float[] likelihood array
     */
    public float[] getLikelihood(int ptype, int tourPosition, int stop,
            ActivityPurpose tpurp) {

        // return an array of probabilities for each stop purpose;

        return likelihood[ptype][tourPosition][stop-1][tpurp.ordinal()];

    }

}
