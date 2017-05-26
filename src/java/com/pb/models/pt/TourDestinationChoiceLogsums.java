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
package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.utils.Tracer;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * A class that either creates a new table to hold logsum data and is then
 * called to calculate the logsums or reads in the logsums from a file.
 *
 * This class also knows how to write the logsums to a file.
 *
 * This class appears to be a replacement for the older TourDestinationChoiceLogsum class.
 *
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 */

public class TourDestinationChoiceLogsums {
    final static Logger logger = Logger.getLogger(TourDestinationChoiceLogsums.class);

    private static TableDataSet logsumData;

    /**
     * Set the zone numbering. This is only necessary when values are not
     * getting read from a file.
     *
     * @param alphaName header for first column
     * @param extNumbers
     *            The external number array (0-based).
     */
    public static void createTourDestinationChoiceLogsumsTable(int[] extNumbers, String alphaName) {
        logsumData = new TableDataSet();
        logsumData.appendColumn(extNumbers, alphaName);
        logsumData.buildIndex(1);
    }


    /**
     * Set the logsums for the given purpose and segment to the logsum array. If
     * it already exists the manager will be updated with the new data.
     *
     * @param purpose
     *            Activity purpose.
     * @param segment
     *            Household segment.
     * @param logsums
     *            Logsum array.
     */
    public static void setLogsumsInTable(ActivityPurpose purpose, int segment, float[] logsums) {
        String columnName = getColumnName(purpose, segment);
        int columnPosition;
        columnPosition = logsumData.getColumnPosition(columnName);
        if (columnPosition != -1) {
            logsumData.setColumnAsFloat(columnPosition, logsums);
        } else {
            logsumData.appendColumn(logsums, columnName);
        }
        
    }

    /**
     * Read the logsums from the file dcLogsum.path set in the resource bundle.
     * @param alphaName name associated with the traffic analysis zones
     * @param rb Resource bundle
     */
    public static void readLogsums(ResourceBundle rb, String alphaName) {
        String dcLogsumFile = ResourceUtil.getProperty(rb, "sdt.dc.logsums");
        logger.info("Reading TourDestinationChoiceLogsumFile: " + dcLogsumFile);

        CSVFileReader reader = new CSVFileReader();
        try {
            logsumData = reader.readFile(new File(dcLogsumFile));
        } catch (IOException e) {
            logger.fatal("Can't find TourDestinationChoiceLogsum file " + dcLogsumFile);
            throw new RuntimeException(e);
        }
        int tazPosition = logsumData.getColumnPosition(alphaName);
        logsumData.buildIndex(tazPosition);
        
    }


    /**
     * Create and return an array holding the destination choice logsums for
     * this purpose and segment. The array will be dimensioned according to the
     * taz data array passed to the method.
     *
     * @param purpose Purpose
     * @param tazNums external numbers (0-based)
     * @param modeChoiceLogsums Matrix of logsums
     * @param skims SkimsInMemory object
     * @param model TourDestinationChoiceModel
     * @return float[]
     */
    public static float[] createLogsums(ActivityPurpose purpose,
            int[] tazNums, Matrix modeChoiceLogsums, SkimsInMemory skims,
            TourDestinationChoiceModel model) {

        Matrix distance = skims.getDistanceMatrix(purpose);
        Matrix time = skims.getTimeMatrix(purpose);
        return createLogsums(purpose, tazNums, modeChoiceLogsums,
                distance, time, model);
    }

    /**
     * Create and return an array holding the destination choice logsums for
     * this purpose and segment. The array will be dimensioned according to the
     * taz data array passed to the method.
     *
     * @param purpose Activity purpose
     * @param extNumbers tazNumbers
     * @param modeChoiceLogsums Matrix of Mode choice logsums
     * @param distance Distance matrix
     * @param time Time matrix
     * @param model TourDestinationChoiceModel
     * @return float[] logsums
     */
    private static float[] createLogsums(ActivityPurpose purpose,
            int[] extNumbers, Matrix modeChoiceLogsums, Matrix distance,
            Matrix time, TourDestinationChoiceModel model) {

        float[] logsums = new float[extNumbers.length];

        int count = 0;
        PTHousehold household = new PTHousehold();
        PTPerson person = new PTPerson();
        Tour tour = new Tour();
        Tracer tracer = Tracer.getTracer();
        tracer.tracePerson("1_1");

        // enter a TAZ loop and compute the logsum from each TAZ to all TAZs for
        // this segment,purpose
        for (int pTazNumber : extNumbers) {

            if(tracer.isTraceZone(pTazNumber)){
                person.hhID = 1;
                person.memberID = 1;
            }
            
            tour.begin.location.zoneNumber = pTazNumber;
            tour.end.location.zoneNumber = pTazNumber;
            tour.primaryDestination.activityPurpose = purpose;

            float logsum = (float) model.calculateUtility(household, person,
            		tour, modeChoiceLogsums, distance, time);

            if (Float.isInfinite(logsum) || Float.isNaN(logsum)) {
            	logsum = -999;
            }
            logsums[count] = logsum;
            ++count;
        }
        return logsums;
    }

    /**
     * Get the logsum for the purpose, segment, and TAZ.
     *
     * @param purpose Activity purpose
     * @param segment market segment
     * @param taz taz number
     * @return The destination choice logsum for the purpose, segment, and taz.
     */
    public static float getLogsum(ActivityPurpose purpose, int segment, int taz) {

        int columnPosition = getColumnPosition(purpose, segment);
        int rowPosition = logsumData.getIndexedRowNumber(taz);
        try {
            return logsumData.getValueAt(rowPosition, columnPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            String msg = "Could not find purpose " + purpose + ", segment "
                    + segment + ", taz " + taz;
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
    }
    
    public static TableDataSet getLogsumTableDataSet() {
        return logsumData;
    }

    /**
     * Write the DC logsums to disk.
     * @param rb Resource Bundle
     */
    public static void writeLogsums(ResourceBundle rb) {
        String fileName = ResourceUtil.getProperty(rb, "sdt.dc.logsums");
        File file = new File(fileName);
        CSVFileWriter writer = new CSVFileWriter();

        try {
            logger.info("Writing DC logsums to " + fileName);
            writer.writeFile(logsumData, file);
        } catch (IOException e) {
            String msg = "Could not write to " + fileName;
            logger.fatal(msg);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the name of the column based on the purpose and segment.
     *
     * @param purpose
     *            The purpose.
     * @param segment
     *            The household segment.
     * @return The name of the column
     */
    private static String getColumnName(ActivityPurpose purpose, int segment) {
        return purpose.name() + segment;
    }

    /**
     * Return the position of the column holding data for the purpose and
     * segment.
     *
     * @param purpose
     *            The purpose.
     * @param segment
     *            The household segment.
     * @return The position of the column.
     */
    private static int getColumnPosition(ActivityPurpose purpose, int segment) {
        String columnName = getColumnName(purpose, segment);
        return logsumData.getColumnPosition(columnName);
    }
    
}
