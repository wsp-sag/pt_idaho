/*
 * Copyright  2007 PB Consult Inc.
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
package com.pb.models.pt.ldt;

import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.TourDestinationChoiceLogsums;

/**
 * @author Erhardt
 * @version 1.0 Oct 12, 2007
 *
 */
public class NormalizedTourDestinationChoiceLogsums {
    final static Logger logger = Logger.getLogger(TourDestinationChoiceLogsums.class);

    private TableDataSet normalizedLogsumData; 

    public NormalizedTourDestinationChoiceLogsums() {
        TableDataSet logsumData = TourDestinationChoiceLogsums.getLogsumTableDataSet(); 
        normalizeLogsums(logsumData); 
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
    private String getColumnName(ActivityPurpose purpose, int segment) {
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
    private int getColumnPosition(ActivityPurpose purpose, int segment) {
        String columnName = getColumnName(purpose, segment);
        return normalizedLogsumData.getColumnPosition(columnName);
    }
    
    /**
     * Normalize the logsums by dividing by the average in each column.  
     *
     */
    private void normalizeLogsums(TableDataSet logsumData) {
        logger.info("Normalizing TourDestinationChoiceLogsums");
        
        // first set the table index
        normalizedLogsumData = new TableDataSet();
        String alphaName = logsumData.getColumnLabel(1); 
        int[] extNumbers = logsumData.getColumnAsInt(alphaName); 
        normalizedLogsumData.appendColumn(extNumbers, alphaName);
        normalizedLogsumData.buildIndex(1);
        
        // then copy the normalized data
        for (int col=2; col<=logsumData.getColumnCount(); col++) {
            String label  = logsumData.getColumnLabel(col); 
            float[] data  = logsumData.getColumnAsFloat(col); 
            float average = logsumData.getColumnTotal(col) / logsumData.getRowCount(); 
            float[] normalizedData = new float[data.length]; 
            
            for (int row=0; row<data.length; row++) {
                normalizedData[row] = data[row] / average; 
            }
            normalizedLogsumData.appendColumn(normalizedData, label); 
        }        
    }
    
    /**
     * Get the normalized logsum for the purpose, segment, and TAZ.
     *
     * @param purpose Activity purpose
     * @param segment market segment
     * @param taz taz number
     * @return The destination choice logsum for the purpose, segment, and taz, divided by
     *         the average for that purpose and segment.
     */
    public float getNormalizedLogsum(ActivityPurpose purpose, int segment, int taz) {

        if (normalizedLogsumData==null) {
            throw new RuntimeException("ERROR: Must first initialize normalized logsums"); 
        }
        
        int columnPosition = getColumnPosition(purpose, segment);
        int rowPosition = normalizedLogsumData.getIndexedRowNumber(taz);
        try {
            return normalizedLogsumData.getValueAt(rowPosition, columnPosition);
        } catch (ArrayIndexOutOfBoundsException e) {
            String msg = "Could not find purpose " + purpose + ", segment "
                    + segment + ", taz " + taz;
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
    }

}
