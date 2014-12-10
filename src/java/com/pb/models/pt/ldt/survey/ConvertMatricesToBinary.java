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
package com.pb.models.pt.ldt.survey;

import java.io.File;

import org.apache.log4j.Logger;

import com.pb.common.matrix.BinaryMatrixWriter;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;

/**
 * Converts TransCAD matrices to binary.  
 * 
 * @author Erhardt
 * @version 1.0 May 10, 2006
 *
 */
public class ConvertMatricesToBinary {

    protected static Logger logger = Logger.getLogger(ConvertMatricesToBinary.class);
    
    /**
     * Generically read a matrix file.
     * 
     * @param fileName
     *            Full path and name of the file to read.
     * @param matrixName
     *            Name for the resulting matrix.
     */
    private static Matrix readMatrix(String fileName, String matrixName) {        

        Matrix m = null;
        try {
            logger.info("Reading travel costs in " + fileName);
            File file = new File(fileName);
            m = MatrixReader.readMatrix(file, matrixName);
        } catch (Exception e) {
            logger.fatal("Error reading matrix file " + fileName);
            e.printStackTrace();
        }

        return m;
    }
    
    /**
     * Generically writes a matrix file.
     * 
     * @param fileName
     *            Full path and name of the file to read.
     * @param m
     *            Matrix to write.
     */
    private static void writeMatrix(String fileName, Matrix m) {        

        try {
            logger.info("Writing matrix to file " + fileName);
            File file = new File(fileName);
            MatrixWriter writer = new BinaryMatrixWriter(file);
            writer.writeMatrix(m);
        } catch (Exception e) {
            logger.fatal("Error writing matrix file " + fileName);
            e.printStackTrace();
        }

    }    
    
    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        String[] inFileNames = {
//                "skims/carOp.mtx", 
//                "skims/carOp.mtx",  
//                "skims/carOp.mtx",
                
//                "skims/icwtPk.mtx", 
//                "skims/icwtPk.mtx", 
//                "skims/icwtPk.mtx", 
                "skims/icwtPk.mtx", 
                "skims/icwtPk.mtx", 
                "skims/icwtPk.mtx", 
                "skims/icwtPk.mtx", 
                "skims/icwtPk.mtx", 
                "skims/icwtPk.mtx", 
                "skims/icwtPk.mtx", 
                
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx", 
                "skims/icdtPk.mtx"
        };
        
        String[] matrixNames = {
//                "Time", 
//                "Dist", 
//                "Toll", 
                
//                "icwtPkFar", 
//                "icwtPkIvt", 
//                "icwtPkFwt", 
                "icwtPkTwt", 
                "icwtPkAwk", 
                "icwtPkXwk", 
                "icwtPkEwk", 
                "icwtPkXfr", 
                "icwtPkBiv", 
                "icwtPkRiv", 
                
                "icdtPkFar", 
                "icdtPkIvt", 
                "icdtPkFwt", 
                "icdtPkTwt", 
                "icdtPkAwk", 
                "icdtPkXwk", 
                "icdtPkEwk", 
                "icdtPkXfr", 
                "icdtPkDrv",
                "icdtPkEdr",
                "icdtPkBiv", 
                "icdtPkRiv"                
        };
        
        
        String[] outFileNames = {
//                "skims/carOpTime.binary", 
//                "skims/carOpDist.binary", 
//                "skims/carOpToll.binary",
                
//                "skims/icwtPkFar.binary", 
//                "skims/icwtPkIvt.binary", 
//                "skims/icwtPkFwt.binary", 
                "skims/icwtPkTwt.binary", 
                "skims/icwtPkAwk.binary", 
                "skims/icwtPkXwk.binary", 
                "skims/icwtPkEwk.binary", 
                "skims/icwtPkXfr.binary", 
                "skims/icwtPkBiv.binary", 
                "skims/icwtPkRiv.binary",  
                
                "skims/icdtPkFar.binary", 
                "skims/icdtPkIvt.binary", 
                "skims/icdtPkFwt.binary", 
                "skims/icdtPkTwt.binary", 
                "skims/icdtPkAwk.binary", 
                "skims/icdtPkXwk.binary", 
                "skims/icdtPkEwk.binary", 
                "skims/icdtPkXfr.binary", 
                "skims/icdtPkDrv.binary",
                "skims/icdtPkEdr.binary",
                "skims/icdtPkBiv.binary", 
                "skims/icdtPkRiv.binary"
        };        
        
        for (int i=0; i<inFileNames.length; i++) {
            Matrix m = readMatrix(inFileNames[i], matrixNames[i]);
            writeMatrix(outFileNames[i], m); 
        }
        
        logger.info("Finished!");
    }

}
