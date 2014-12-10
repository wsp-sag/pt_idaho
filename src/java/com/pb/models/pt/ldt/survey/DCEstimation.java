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
 * Created on Oct 26, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.ldt.survey;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.BinaryMatrixReader;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.TazByAmz;
import com.pb.models.pt.survey.TwoStageDCEstimationFile;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Set;

public class DCEstimation {
    protected static Logger logger = Logger.getLogger("com.pb.osmp");

    protected ResourceBundle rb = ResourceUtil.getResourceBundle("pt");

    private TazByAmz tba;

    private TwoStageDCEstimationFile dcf;

    private int draws = 20;

    /**
     * Constructor.
     * 
     * Assembles the information from the properties file.
     */
    public DCEstimation() {
    }

    /**
     * Set the number of samples to draw.
     * 
     */
    public void setDraws(int draws) {
        this.draws = draws;
    }

    /**
     * Set-up the file constructor.
     */
    public void setup(String inFile, String sizeFile, String outFile) {
        dcf = new TwoStageDCEstimationFile();
        dcf.setDraws(draws);

        // read property file info
        String ptData = ResourceUtil.getProperty(rb, "dir.pt") + "/"
                + ResourceUtil.getProperty(rb, "dir.data") + "/";
        logger.info("pt data directory: " + ptData);

        // zonal system
        String zonal = ptData + ResourceUtil.getProperty(rb, "dir.zonal") + "/";
        String zoneSystem = zonal + ResourceUtil.getProperty(rb, "zSystem");
        logger.info("zone system: " + zoneSystem);
        tba = new TazByAmz(zoneSystem);
        dcf.setDistrictToZone(tba.getAmzToTaz());
        dcf.setZoneToDistrict(tba.getTazToAmz());

        // check: write correspondence
        // tba.writeAmzToTaz("amz2taz.csv");

        // travel costs
        BinaryMatrixReader bmr;

        String congestedDir = ptData
                + ResourceUtil.getProperty(rb, "dir.tCosts") + "/"
                + ResourceUtil.getProperty(rb, "dir.congested") + "/";

        String distance = congestedDir
                + ResourceUtil.getProperty(rb, "bCosts.dist");
        logger.info("distance matrix: " + distance);
        bmr = new BinaryMatrixReader(new File(distance));
        Matrix zonalDistance = bmr.readMatrix();
        logger.info("The district matrx is " + zonalDistance.getRowCount()
                + " x " + zonalDistance.getColumnCount());
        tba.appendMatrix(zonalDistance);
        dcf.setZonalDistance(zonalDistance);

        String gcosts = congestedDir
                + ResourceUtil.getProperty(rb, "bCosts.gcost");
        logger.info("generalized cost matrix: " + gcosts);
        bmr = new BinaryMatrixReader(new File(gcosts));
        tba.appendMatrix(bmr.readMatrix());

        String time = congestedDir
                + ResourceUtil.getProperty(rb, "bCosts.time");
        logger.info("time matrix: " + time);
        bmr = new BinaryMatrixReader(new File(time));
        tba.appendMatrix(bmr.readMatrix());

        String toll = congestedDir
                + ResourceUtil.getProperty(rb, "bCosts.toll");
        logger.info("toll matrix: " + toll);
        bmr = new BinaryMatrixReader(new File(toll));
        tba.appendMatrix(bmr.readMatrix());

        // slats
        logger.info("creating the slat matrices");
        tba.averageToSlats();
        dcf.setSlatDistance(tba.getSlat(0));
        dcf.setCosts(tba.getSlats());

        // long distance threshold
        int threshold = new Integer(ResourceUtil.getProperty(rb,
                "ldt.threshold")).intValue();
        logger.info("Setting the threshold to " + threshold);
        dcf.setThreshold(threshold);

        // the size term for sampling must be in the first column of the zonal
        // file
        logger.info("Getting zonal data from " + sizeFile);
        ArrayList<ColumnVector> zonalVectors = zonalData(sizeFile);
        dcf.setZonalSize(zonalVectors.get(0));
        dcf.setZonalVectors(zonalVectors);

        // set the external number array
        dcf.setExtArray(tba.getExternalNumbers());

        // let's do it
        logger.info("Creating the estimation file.");
        dcf.createEstimationFile(inFile, outFile);
        
        logger.info("All done.");
    }

    /**
     * Read the zonal data file.
     * 
     * This method converts data into an ArrayList of ColumnVectors. The taz
     * *must* be in the first column. All other columns are put into
     * ColumnVectors.
     * 
     * @param fileName
     * @return
     */
    private ArrayList<ColumnVector> zonalData(String fileName) {

        // bring in the tabular data -- the table must have a column named size
        CSVFileReader csv = new CSVFileReader();
        TableDataSet table = null;
        ArrayList<ColumnVector> dataList = new ArrayList<ColumnVector>();

        try {
            table = csv.readFile(new File(fileName));
        } catch (Exception e) {
            logger.fatal("Unable to read the zonal file.");
            throw new RuntimeException(e);
        }

        logger.info("Found " + table.getRowCount() + " rows and "
                + table.getColumnCount() + " columns.");

        // create the external arrays
        int[] extArray = tba.getExternalNumbers();

        HashMap<Integer, Integer> tazToAmz = tba.getTazToAmz();
        Set<Integer> tazSet = tazToAmz.keySet();

        // assume the TAZ is in the first column position and we want all the
        // data in the other columns
        String[] names = table.getColumnLabels();
        for (int c = 2; c <= table.getColumnCount(); ++c) {
            ColumnVector cData = new ColumnVector(extArray.length);
            cData.setExternalNumbers(extArray);
            cData.setName(names[c - 1]);

            for (int r = 1; r <= table.getRowCount(); ++r) {
                int taz = (int) table.getValueAt(r, 1);

                if (!tazSet.contains(taz)) {
                    logger.warn("TAZ not in zone system: " + taz);
                    continue;
                }

                float value = table.getValueAt(r, c);
                cData.setValueAt(taz, value);
            }

            dataList.add(cData);
        }

        return dataList;
    }

    /**
     * usage: DCEstimation
     * 
     * @param args
     */
    public static void main(String[] args) {
        DCEstimation dce = new DCEstimation();
        int draws = new Integer(args[3]);
        dce.setDraws(draws);
        dce.setup(args[0], args[1], args[2]);
    }

}
