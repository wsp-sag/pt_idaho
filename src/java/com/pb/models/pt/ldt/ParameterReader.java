/*
 * Copyright  2006 PB Consult Inc.
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

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * @author Erhardt
 * @version 1.0 Apr 28, 2006
 *
 */
public class ParameterReader {
    
    protected static Logger logger = Logger.getLogger(ParameterReader.class);
    
    public static float[][] readParameters(ResourceBundle rb, String property) {
        TableDataSet table = readParametersAsTable(rb, property);
        float[][] params = getParameterValues(table);
        return params; 
    }
    
    /**
     * Read parameters from file specified in properties.
     * 
     */
    public static TableDataSet readParametersAsTable(ResourceBundle rb, String property) {
        
        logger.info("Reading property: " + property);
        String fileName = ResourceUtil.getProperty(rb, property);
        logger.info("Reading file: "+ fileName); 
        
        TableDataSet table; 
        try {
            CSVFileReader reader = new CSVFileReader();
            table = reader.readFile(new File(fileName));
        } catch (IOException e) {
            logger.fatal("Can't find file " + fileName);
            throw new RuntimeException(e);
        }
        return table; 
    }
    
    /**
     * Returns the values in a table data set.  Assumes the first
     * column is for labels, so does not include those.  
     * 
     * @param t TableDataSet to read from.
     * @return a two-dimensional array of floats with the parameter
     *         values.  
     */   
    private static float[][] getParameterValues(TableDataSet t) {        
        // one fewer columns because we're dropping the first
        float[][] values = new float[t.getRowCount()][t.getColumnCount() - 1];
                
        for (int i=0; i<values.length; i++) {
            for (int j=0; j<values[i].length; j++) {      
                // table data set is 1-based, and ignore the first column
                values[i][j] = t.getValueAt(i+1, j+2);
            }
        }
        return values; 
    }
    
    /**
     * Generically read a matrix file given a resource bundle and property.
     * 
     */
    public static Matrix readMatrix(ResourceBundle rb, String property, String name) {        

        logger.info("Reading property: " + property);
        String fileName = ResourceUtil.getProperty(rb, property);
        logger.info("Reading file: "+ fileName); 
        
        Matrix m = null;
        try {
            m = MatrixReader.readMatrix(new File(fileName), name);
            m.setName(name); 
        } catch (Exception e) {
            logger.fatal("Error reading matrix file " + fileName);
            throw new RuntimeException(e);
        }

        return m;
    }
    
}
