/*
 * Copyright 2006 PB Americas
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Created on Dec 29, 2006 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.util.tests;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCollection;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.util.SkimsInMemory;

/**
 * @author Andrew Stryker
 * @version 0.1
 */
public class SkimsInMemoryTest {
    private Logger logger = Logger.getLogger(SkimsInMemoryTest.class);

    private SkimsInMemory skimsInMemory;

    private final int INTERNAL_ZONES = 10;

    private final int EXTERNAL_ZONES = 2;

    private File matrixFile; // all property file references should point

    // here

    private File zoneFile;

    /**
     * Create a temporary zone system.
     */
    private void writeZoneSystem() throws Exception {
        BufferedWriter writer;
        int[] extNumbers = createExtNumbers(INTERNAL_ZONES);

        zoneFile = new File("zoneFile.csv");
        writer = new BufferedWriter(new FileWriter(zoneFile));

        writer.write("AMZ,TAZ\n");
        for (int i = 1; i < extNumbers.length; ++i) {
            writer.write("1," + extNumbers[i] + "\n");
        }

        writer.close();
    }

    /**
     * Write a simple matrices to the working directory.
     * 
     * @throws java.lang.Exception
     */
    private void writeMatrixFiles() throws Exception {
        Matrix matrix = createMatrix(INTERNAL_ZONES + EXTERNAL_ZONES);
        MatrixWriter writer;

        matrixFile = new File("testMatrix.zmx");
        matrixFile.deleteOnExit();
        writer = MatrixWriter.createWriter(MatrixType.ZIP, matrixFile);

        logger.info("Writing temporary matrix to disk.");
        writer.writeMatrix(matrix);
    }

    /**
     * Set-up the test.
     */
    @Before
    public void setUp() throws Exception {
        ResourceBundle globalBundle = ResourceUtil.getResourceBundle("global");
        ResourceBundle ptBundle = ResourceUtil.getResourceBundle("pt");

        skimsInMemory = SkimsInMemory.getSkimsInMemory();
        writeZoneSystem();
        writeMatrixFiles();
        skimsInMemory.setGlobalProperties(globalBundle);
        skimsInMemory.readSkims(ptBundle);
    }

    /**
     * Delete the temporary files.
     */
    @After
    public void tearDown() {
        logger.info("Deleting temporary files");

        if (matrixFile != null) {
            logger.info("Deleting the matrix file.");
            matrixFile.delete();
        }

        if (zoneFile != null) {
            logger.info("Deleting the zone system file.");
            zoneFile.delete();
        }
    }

    /**
     * Create a simple square matrix for testing.
     */
    private Matrix createMatrix(int dimensions) {
        Matrix matrix = new Matrix(dimensions, dimensions);
        int[] extNumbers = createExtNumbers(dimensions);
        matrix.setExternalNumbers(extNumbers, extNumbers);

        return fillMatrix(matrix);
    }

    /**
     * Fill the matrix with dummy values.
     */
    private Matrix fillMatrix(Matrix matrix) {
        int[] rowExtNumbers = matrix.getExternalRowNumbers();
        int[] colExtNumbers = matrix.getExternalColumnNumbers();

        for (int r = 1; r < rowExtNumbers.length; ++r) {
            int row = rowExtNumbers[r];

            for (int c = 1; c < colExtNumbers.length; ++c) {
                int col = colExtNumbers[c];
                float value = row + col;

                matrix.setValueAt(row, col, value);
            }
        }
        return matrix;
    }

    /**
     * Create an external number array with gaps.
     */
    private int[] createExtNumbers(int dimensions) {
        int[] extNumbers = new int[dimensions + 1];

        for (int i = 1; i < extNumbers.length; ++i) {
            extNumbers[i] = 2 * i - 1;
        }

        return extNumbers;
    }

    /**
     * Check matrix.
     */
    private void checkMatrix(Matrix matrix) {
        int[] colExtNumbers = matrix.getExternalColumnNumbers();
        int[] rowExtNumbers = matrix.getExternalRowNumbers();

        assertEquals("Matrix should be square.", colExtNumbers.length,
                rowExtNumbers.length);
        assertEquals("External " + INTERNAL_ZONES
                + " zones for an array of length " + (INTERNAL_ZONES + 1)
                + ": " + colExtNumbers.length, INTERNAL_ZONES + 1,
                colExtNumbers.length);

        for (int r = 1; r < rowExtNumbers.length; ++r) {
            int row = rowExtNumbers[r];

            for (int c = 1; c < colExtNumbers.length; ++c) {
                int col = colExtNumbers[c];

                assertEquals("Checking row " + row + " and column " + col,
                        (float) (row + col), matrix.getValueAt(row, col),
                        0.001f);
            }
        }
    }

    /**
     * Test several matrices.
     */
    @Test
    public void testMatrices() {
        checkMatrix(skimsInMemory.opDist);
        checkMatrix(skimsInMemory.opTime);
        checkMatrix(skimsInMemory.opToll);

        checkMatrix(skimsInMemory.pkDist);
        checkMatrix(skimsInMemory.pkTime);
        checkMatrix(skimsInMemory.pkToll);

//        checkMatrixCollection(skimsInMemory.opdrv);
        checkMatrixCollection(skimsInMemory.opwlk);

//        checkMatrixCollection(skimsInMemory.pkdrv);
        checkMatrixCollection(skimsInMemory.pkwlk);
    }

    /**
     * @param collection
     */
    private void checkMatrixCollection(MatrixCollection collection) {
        for (Object name : collection.getHashMap().keySet()) {
            checkMatrix(collection.getMatrix((String) name));
        }
    }
}
