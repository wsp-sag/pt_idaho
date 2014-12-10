/*
 * Copyright 2005 PB Consult Inc.
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
 * Created on Oct 17, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import com.pb.common.matrix.BinaryMatrixWriter;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * Convert a TAZ x TAZ matrix to a TAZ x AMZ matrix and back.
 * 
 * This class first uniquely maps an AMZ to a TAZ. Using this relationship, TAZ
 * x TAZ matrices can be averaged to TAZ x AMZ matrices. The TAZ x AMZ matrices
 * are really TAZ x TAZ matrices whre the AMZ information is stored in one TAZ,
 * filling the matrix in a "slat" like pattern.
 * 
 * @author stryker
 * 
 */
public class TazByAmz {
    public static Logger logger = Logger.getLogger(TazByAmz.class);

    protected HashMap<Integer, Integer> amzToTaz;

    protected HashMap<Integer, Integer> tazToAmz;

    protected ArrayList<Matrix> matrices;

    private ArrayList<Matrix> slats;

    protected ResourceBundle rb;

    /**
     * Generic Constructor.
     */
    public TazByAmz() {
    }

    /**
     * Constructor.
     * 
     * @param fileName  File name
     */
    public TazByAmz(String fileName) {
        rb = ResourceUtil.getResourceBundle("pt");
        readTazToAmz(fileName);
    }

    /**
     * Constructor.
     * 
     * @param fileName File
     * @param taz Taz name
     * @param amz Amz name
     */
    public TazByAmz(String fileName, String taz, String amz, ResourceBundle globalRb) {
        rb = globalRb; 
        readTazToAmz(fileName, taz, amz);
    }

    /**
     * Use the zone system file in the properties file.
     */
    public void readTazToAmz() {
        String fileName = rb.getString("alpha2beta.file");
        readTazToAmz(fileName);
    }

    /**
     * Read the correspondence file.
     * 
     * Use the properties file to file the TAZ and AMZ column names.
     * 
     * @param fileName
     *            CSV file with the correspondence
     */
    public void readTazToAmz(String fileName) {
        String tazCol = ResourceUtil.getProperty(rb, "alpha.name");
        String amzCol = ResourceUtil.getProperty(rb, "beta.name");

        readTazToAmz(fileName, tazCol, amzCol);
    }

    /**
     * Read the correspondence file.
     * 
     * Set the Alpha to Beta converter.
     * 
     * @param fileName
     *            CSV file with the correspondence
     * 
     * @param tazCol
     *            column heading for alpha zones
     * 
     * @param amzCol
     *            column heading for AMZs
     * 
     */
    public void readTazToAmz(String fileName, String tazCol, String amzCol) {
        tazToAmz = new HashMap<Integer, Integer>();

        CSVFileReader reader = new CSVFileReader();
        TableDataSet zoneSystem;

        try {
            zoneSystem = reader.readFile(new File(fileName));
        } catch (Exception e) {
            logger.fatal("Unable to read the correspondence file: " + fileName);
            throw new RuntimeException(e);
        }

        for (int r = 1; r <= zoneSystem.getRowCount(); ++r) {
            Integer taz = (int) zoneSystem.getValueAt(r, tazCol);
            Integer amz = (int) zoneSystem.getValueAt(r, amzCol);

            tazToAmz.put(taz, amz);

        }

        mapAmzToTaz();
    }

    /**
     * Read travel costs from a CSV file.
     * 
     * @param fileName  Travel cost file
     */
    public void readTravelCosts(String fileName) {
        HashSet<Integer> tazSet = new HashSet<Integer>(tazToAmz.keySet());

        logger.info("Reading travel cost file: " + fileName);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String[] header = reader.readLine().split(",");

            // create matrices
            matrices = new ArrayList<Matrix>();

            for (int c = 2; c < header.length; ++c) {
                logger.info("Creating " + header[c] + " matrix.");

                Matrix matrix = new Matrix(tazSet.size(), tazSet.size());
                matrix.setExternalNumbers(getExternalNumbers());
                matrix.setName(header[c]);

                matrices.add(matrix);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                if (fields.length != header.length) {
                    logger.fatal("Record length differs from the header: "
                            + line);
                    throw new RuntimeException("Record lengths differ.");
                }

                int itaz = new Integer(fields[0]);
                int jtaz = new Integer(fields[1]);

                // check for TAZs in the set
                if (tazSet.contains(itaz) && tazSet.contains(jtaz)) {

                    for (int i = 2; i < header.length; ++i) {
                        int m = i - 2;
                        float value = new Float(fields[i]);

                        matrices.get(m).setValueAt(itaz, jtaz, value);
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("Travel cost matrices ready.");
    }

    /**
     * Set the list of zonal matrices.
     * 
     * @param matrices  List of matrices
     */
    public void setMatrices(ArrayList<Matrix> matrices) {
        this.matrices = matrices;
    }

    /**
     * Set the zonal matrices to one matrix.
     * 
     * @param matrix Matrix to add to list
     */
    public void setMatrix(Matrix matrix) {
        matrices = new ArrayList<Matrix>();
        matrices.add(matrix);
    }

    /**
     * Append a matrix to the list of zonal matrices.
     * 
     * @param matrix Matrix to append
     */
    public void appendMatrix(Matrix matrix) {
        if (matrices == null) {
            matrices = new ArrayList<Matrix>();
        }
        matrices.add(matrix);
    }

    /**
     * Set the map AMZ to TAZ.
     * 
     */
    private void mapAmzToTaz() {
        logger.info("Mapping AMZs to TAZs.");

        amzToTaz = new HashMap<Integer, Integer>();
        for (Integer taz : tazToAmz.keySet()) {
            Integer amz = tazToAmz.get(taz);

            amzToTaz.put(amz, taz);
        }

    }

    /**
     * Get a matrices from the matrices array.
     * @param i matrix index
     * @return Matrix matrix
     */
    public Matrix getMatrix(int i) {
        return matrices.get(i);
    }

    /**
     * Get the matrices array.
     * @return ArrayList List of matrices
     */
    public ArrayList<Matrix> getMatrices() {
        return matrices;
    }

    /**
     * Get a slat matrix from the slats array.
     * @param i matrix index
     * @return matrix
     */
    public Matrix getSlat(int i) {
        return slats.get(i);
    }

    /**
     * Get the slat matices.
     * @return ArrayList array list of matrices
     */
    public ArrayList<Matrix> getSlats() {
        return slats;
    }

    /**
     * Write the AMZ to TAZ correspondence file.
     * 
     * The correspondence file is written in a CSV format.
     * 
     * @param fileName
     *            The name of the correspondence file to write.
     */
    public void writeAmzToTaz(String fileName) {
        File out = new File(fileName);
        Iterator tazIter = tazToAmz.keySet().iterator();

        logger.info("Writing correspondence to " + fileName);

        try {
            FileWriter writer = new FileWriter(out);

            // header
            writer.write("TAZ, proxy, AMZ\n");

            while (tazIter.hasNext()) {
                int taz = (Integer) tazIter.next();
                int amz = tazToAmz.get(taz);
                int proxy = amzToTaz.get(amz);

                writer.write(taz + "," + proxy + "," + amz + "\n");
            }

            writer.flush();
            writer.close();

        } catch (Exception e) {
            throw new RuntimeException("Could not write correspondence file.");
        }

        logger.info("Finished writing correspondence file.");

    }

    /**
     * Aggregate a square matrices into a "slats".
     * 
     */
    public void averageToSlats() {

        logger.info("Converting matrices to slat format.");

        // external arrays
        int tazs = tazToAmz.size();
        int[] tazExtArray = getExternalNumbers();

        // squeeze all the matrices
        slats = new ArrayList<Matrix>();
        for (Matrix matrix : matrices) {
            logger.info("Converting " + matrix.getName() + " to slats format.");

            Matrix slat = new Matrix(tazs, tazs);
            slat.setName(matrix.getName());
            slat.setExternalNumbers(tazExtArray);

            Matrix counts = new Matrix(tazs, tazs);
            counts.setExternalNumbers(tazExtArray);

            // fill the squeeze matrices with the summation, couting along the
            // way
            for (Integer r : tazToAmz.keySet()) {
                for (Integer c : tazToAmz.keySet()) {
                    int amz = tazToAmz.get(c);
                    int proxy = amzToTaz.get(amz);

                    float value = matrix.getValueAt(r, c)
                            + slat.getValueAt(r, proxy);

                    slat.setValueAt(r, proxy, value);
                    int cnt = (int) counts.getValueAt(r, proxy);
                    counts.setValueAt(r, proxy, ++cnt);
                }

            }

            // loop through the matrices to do averaging
            for (Integer row : tazToAmz.keySet()) {
                // recall that we are filling slats
                for (Integer amz : amzToTaz.keySet()) {
                    int proxy = amzToTaz.get(amz);

                    float sum = slat.getValueAt(row, proxy);
                    float cnt = counts.getValueAt(row, proxy);

                    if (cnt > 0) {
                        slat.setValueAt(row, proxy, sum / cnt);
                    } else {
                        logger.fatal("AMZ " + amz + " does not have any TAZs.");
                    }

                }
            }

            slats.add(slat);
        }
    }

    /**
     * Write the slats as binary matrices
     */
    public void writeMatrices() {
        BinaryMatrixWriter writer;

        logger.info("Writing matrices...");

        for (Matrix matrix : matrices) {
            String fileName = matrix.getName() + ".binary";
            logger.info("Writing matrix to file " + fileName);
            File file = new File(fileName);
            writer = new BinaryMatrixWriter(file);

            writer.writeMatrix(matrix);
        }
    }

    /**
     * Get an external array.
     * 
     * The 1-based external array contains all the TAZs mapped to an AMZ.
     * 
     * @return int[]
     */
    public int[] getExternalNumbers() {
        // TreeSet is an *ordered* Collection
        TreeSet<Integer> tazSet = new TreeSet<Integer>(tazToAmz.keySet());
        int[] extArray = new int[tazSet.size() + 1]; // 1-indexed

        int t = 0;
        for (Integer taz : tazSet) {
            extArray[++t] = taz;
        }

        return extArray;
    }
    
    /**
     * Get the set of all TAZs mapped to a specified AMZ.  
     * 
     * @param  amz The AMZ of interest.  
     * @return A HashSet containing the IDs of all TAZs in that AMZ.
     */
    public HashSet <Integer> getTazSet(int amz) {
        // TreeSet is an *ordered* Collection
        TreeSet<Integer> allTazs = new TreeSet<Integer>(tazToAmz.keySet());
        HashSet<Integer> tazsInAmz = new HashSet<Integer>(); 

        for (Integer currentTaz : allTazs) {
            int currentAmz = tazToAmz.get(currentTaz);
            if (currentAmz == amz) {
                tazsInAmz.add(currentTaz);
            }
        }

        return tazsInAmz;
    }

    public HashMap<Integer, Integer> getAmzToTaz() {
        return amzToTaz;
    }

    public HashMap<Integer, Integer> getTazToAmz() {
        return tazToAmz;
    }

    /**
     * Write the slats as binary matrices
     */
    public void writeSlats() {
        BinaryMatrixWriter writer;

        logger.info("Wrtiing slat matrices...");

        for (Matrix slat : slats) {
            String fileName = slat.getName() + "_slats.binary";
            logger.info("Writing slat matrix to file " + fileName);
            File file = new File(fileName);
            writer = new BinaryMatrixWriter(file);

            writer.writeMatrix(slat);
        }
    }

    /**
     * Run the class as a command line utility.
     * 
     * USAGE: TazByAmz <tabular matrix> <correspondence>
     * 
     * TazByAmz will write the cost matrices as a seires of binary files.
     * @param args Runtime arguments
     */
    public static void main(String[] args) {

        String costTable = args[0];
        String tazToAmz = args[1];

        logger.info("Travel cost file: " + costTable);
        logger.info("Zone correspondence file: " + tazToAmz);

        TazByAmz t2a = new TazByAmz();
        t2a.readTazToAmz(tazToAmz);
        t2a.readTravelCosts(costTable);
        t2a.writeMatrices();
        t2a.averageToSlats();
        t2a.writeSlats();
        t2a.writeAmzToTaz("amz2taz.csv");
    }

}