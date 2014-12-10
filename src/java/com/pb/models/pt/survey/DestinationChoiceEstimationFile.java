/*
 * Copyright  2005 PB Consult Inc.
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
// DestnationChoiceFile.java
// Created on: Apr 7, 2004
package com.pb.models.pt.survey;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.BinaryMatrixReader;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * Create a destination choice estimation file.
 * 
 * This utility reads a specified estimation file, distance matrix, and zonal
 * size file. The class use DestinationChoiceSampling to generate samples and
 * correction factors.
 * 
 * TODO: Throw exceptions when given bad data.
 * 
 * @author Andrew Stryker <stryker@pbworld.com>
 * @version 2.0 5 Oct 2005
 */
public class DestinationChoiceEstimationFile {
    protected Logger logger = Logger.getLogger("com.pb.models");

    protected int[] extArray;

    protected int tazs;

    protected TableDataSet zonalData;

    protected int draws;

    protected HashSet<Integer> tazSet = new HashSet<Integer>();

    protected ArrayList<Matrix> costs = new ArrayList<Matrix>();

    protected ArrayList<String> skimLabels = new ArrayList<String>();

    protected PrintWriter out;

    protected ColumnVector zSize;

    protected ResourceBundle rb = ResourceUtil.getResourceBundle("pt");

    /**
     * Constructor.
     */
    public DestinationChoiceEstimationFile() {
    }

    /**
     * Constructor.
     * 
     * Construct and set number of samples.
     * 
     * @param samples
     *            The number of samples.
     */
    public DestinationChoiceEstimationFile(int draws) {
        logger.info("Initialized destination choice estimation file builder.");
        setDraws(draws);
    }

    /**
     * Read zonal data from file.
     * 
     * @param zonalDataFileName
     *            Zonal size file name (CSV format).
     */
    public void readZonalData(String zonalDataFileName) {
        logger.info("Reading the zonal data in file " + zonalDataFileName);

        try {
            CSVFileReader cr = new CSVFileReader();
            zonalData = cr.readFile(new File(zonalDataFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int tazCol = zonalData.getColumnPosition("taz");
        zonalData.buildIndex(tazCol);
        
        // create the zone system
        for (int r = 1; r <= zonalData.getRowCount(); ++r) {
            int taz = (int) zonalData.getValueAt(r, tazCol);
            tazSet.add(taz);
        }
    }

    /**
     * Add a Matrix to the list of costs.
     * 
     * @param fileName
     *            Name of the binary file with skim data.
     */
    public void addSkim(String fileName) {
        logger.info("Adding skim from " + fileName);

        MatrixReader mr = new BinaryMatrixReader(new File(fileName));
        Matrix skim = mr.readMatrix();
        costs.add(skim);
        skimLabels.add(fileName.replaceFirst(".binary", "_"));
    }

    /**
     * Print zonal data.
     * 
     * @param taz
     *            The TAZ for the zonal information to print.
     */
    protected void printZonalData(int taz) {
        int cols = zonalData.getColumnCount();
        int tazCol = zonalData.getColumnPosition("taz");
        float val;

        for (int c = 1; c <= cols; ++c) {
            if (c == tazCol) {
                continue;
            }

            // TAZ will be 0 when there were duplicates sampled
            if (taz == 0) {
                val = 0;
            } else {
                val = zonalData.getIndexedValueAt(taz, c);
            }

            out.print("," + val);
        }
    }

    /**
     * Print skim data.
     * 
     * Prints the skim data for a given zone pair.
     * 
     * @param ptaz
     *            Production zone.
     * @param ataz
     *            Attraction zone.
     */
    protected void printSkims(int ptaz, int ataz) {
        Iterator sk = costs.listIterator();
        Matrix skim;
        float val;

        while (sk.hasNext()) {
            skim = (Matrix) sk.next();

            if (ataz == 0) {
                val = 0;
            } else {
                val = skim.getValueAt(ptaz, ataz);
            }

            out.print("," + val);
        }
    }

    /**
     * Map field names to column positions.
     * 
     * @param header
     *            String of the comma delimited header record.
     * 
     * @return HashMap
     */
    protected HashMap<String, Integer> mapFieldPositions(String header) {
        // accepts only comma delimited input
        HashMap<String, Integer> hm = new HashMap<String, Integer>();
        String[] flds = header.split(",");

        for (int i = 0; i < flds.length; ++i) {
            String token = flds[i];
            Integer pos = new Integer(i);

            if (hm.containsKey(token)) {
                logger.warn("Duplicate field name:  " + token);
            }

            hm.put(token, pos);
        }

        return hm;
    }

    /**
     * Print the header.
     * 
     * Appends headers to the estimation file header.
     * 
     * @param header
     *            The header line from the estimation file.
     */
    protected void printHeader(String header) {
        out.print(header);

        String label;
        String suffix;

        // append new column labels
        for (int s = -1; s <= draws; ++s) {
            if (s == -1) {
                suffix = "chosen";
            } else if (s == 0) {
                suffix = "intra";
            } else {
                suffix = new Integer(s).toString();
            }

            // no samples tazes for chosen and intra, and no correction factors
            // for intras
            if (s < 0) {
                out.print(",cf_chosen");
            }

            if (s > 0) {
                out.print(",sample_" + suffix + ",cf_" + suffix);
            }

            // labels for zonal data
            int cols = zonalData.getColumnCount();

            for (int c = 1; c <= cols; ++c) {
                label = zonalData.getColumnLabel(c);

                if (!label.equals("taz")) {
                    out.print("," + label + "_" + suffix);
                }
            }

            // labels for costs
            Iterator<String> sl = skimLabels.listIterator();

            while (sl.hasNext()) {
                label = sl.next();
                out.print("," + label + suffix);
            }
        }

        out.println();
        out.flush();
    }

    /**
     * Do the sampling.
     * 
     * Create the utilities and write the file.
     * 
     * @param inFile
     *            The input file.
     * @param outFile
     *            The output file.
     */
    public void createEstimationFile(String inFile, String outFile) {
        logger.info("Creating a Destination Estimation File.");

        logger.info("Preparing for sampling.");

        DestinationChoiceSampling dc = new DestinationChoiceSampling(draws);

        // the separation distance *must* be the first matrix
        Matrix dist = costs.get(0);
        extArray = dist.getExternalNumbers();
        tazs = dist.getColumnCount();

        // it's best to explicately set the external number array
        dc.setExtArray(extArray);

        // lambda is not automatically calculated
        dc.setLambda(dist);

        // set-up for the probability calculations
        int tazColumn = zonalData.getColumnPosition("taz");
        int sizeColumn = zonalData.getColumnPosition("size");
        zSize = createColumnVector(zonalData, tazColumn, sizeColumn);
        zSize.setExternalNumbers(extArray);
        dc.computeProbabilities(dist, zSize);

        BufferedReader in;
        String[] flds;

        try {
            in = new BufferedReader(new FileReader(inFile));
            out = new PrintWriter(new BufferedWriter(new FileWriter(outFile)),
                    true);

            logger.info("Writing to estimation file " + outFile);

            // process header
            String line = in.readLine();
            HashMap<String, Integer> positions = mapFieldPositions(line);
            printHeader(line);

            int r = 1;
            Map<Integer, Double> samples;

            // loop through the estimation file
            while ((line = in.readLine()) != null) {
                r += 1;
                flds = line.split(",");
                int ptaz = new Integer(flds[positions.get("ptaz").intValue()])
                        .intValue();
                int ataz = new Integer(flds[positions.get("ataz").intValue()])
                        .intValue();

                // skip TAZs that are not part of the zone system defined in
                // the zonal data file
                if (!tazSet.contains(new Integer(ptaz))
                        || !tazSet.contains(new Integer(ataz))) {
                    logger.info("TAZ pair not in zone system (" + ptaz + ", "
                            + ataz + ")");

                    continue;
                }

                // skip where the size of the attraction variable is 0 or less
                if (zSize.getValueAt(ataz) <= 0) {
                    logger.info("Skipping zone pair (" + ptaz + ", " + ataz
                            + ") due to a non-positive size.");

                    continue;
                }

                logger.info("Sampling for record " + r + ", TAZ pair (" + ptaz
                        + ", " + ataz + ")");

                // clear the sampled set and then assert alternatives
                dc.clearSamples();
                dc.assertAlternative(ataz);

                logger.info("Drawing " + draws + " alternatives.");
                dc.sampleFromProbabilities(ptaz);
                samples = dc.getAlternatives();

                // create a new entry in the estimation file
                out.print(line);

                // correction factor for the chosen (asserted) alternative
                out.print("," + samples.get(ataz));
                samples.remove(ataz);

                // append zonal data and travel cost data
                printZonalData(ataz);
                printSkims(ptaz, ataz);

                // intra-zonal data
                printZonalData(ptaz);
                printSkims(ptaz, ptaz);

                // work through the samples, excluding the chosen
                // append to output line
                Iterator<Integer> altIter = samples.keySet().iterator();
                int a = 0;
                while (altIter.hasNext()) {
                    int staz = altIter.next();                
                    double cf = samples.get(staz);
                    a += 1;
                    
                    out.print("," + staz + "," + cf);

                    // append data
                    printZonalData(staz);
                    printSkims(ptaz, staz);
                }

                // sometimes the sample set has duplicates
                while (a++ < draws) {
                    out.print(",0,0");

                    // append data
                    printZonalData(0);
                    printSkims(ptaz, 0);
                }
                out.println();
            }

            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set the zonal size attribute.
     * 
     * @param size
     */
    public void setZSize(ColumnVector size) {
        zSize = size;
    }

    /**
     * Convert columns from a TableDataSet to a ColumnVector.
     * 
     * User supplies columns to use.
     * 
     * @param table
     *            TableDataSet table.
     * @param taz
     *            Column with TAZs.
     * @param values
     *            Column with values.
     */
    protected ColumnVector createColumnVector(TableDataSet table, int taz,
            int values) {
        int[] tazArray = table.getColumnAsInt(taz);
        float[] valueArray = table.getColumnAsFloat(values);
        ColumnVector cv = new ColumnVector(tazs);
        cv.setExternalNumbers(extArray);

        for (int i = 0; i < tazArray.length; ++i) {
            int t = tazArray[i];
            float v = valueArray[i];
            cv.setValueAt(t, v);
        }

        return cv;
    }

    /**
     * Print usage information and exit.
     * 
     * @param error
     *            Exit error code.
     */
    protected static void usage(int error) {
        System.out
                .println("Usage: java DestinationChoiceFile <infile> "
                        + "<outfile> <samples> <zonal data> <distance matrix> [costs...]");

        System.out.println("\nWhere:");
        System.out.println("\tinfile\tinput estimation is CSV formart");
        System.out.println("\toutfile\toutput file name");
        System.out.println("\tsamples\tnumber of samples to draw");
        System.out.println("\tzonal data\tCSV file of TAZ and zone data");
        System.out.println("\tdistance matrix\tdistance matrix file name");
        System.out.println("\tskims additional costs to append");
        System.out.println("\nNote:\tThe CSV file needs to have column "
                + "headings.");
        System.out.println("\tThe input file must have fields names \"ptaz\""
                + " and \"ataz\" in the heading.");
        System.out.println("\tThe zonal data must have a field named \"taz\""
                + " and a field named \"size\".");
        System.out.println("\tThe matrices must be in binary fomrat.");

        System.exit(error);
    }

    /**
     * Set the number of desired samples.
     * 
     * @param samples
     *            The samples to set.
     */
    public void setDraws(int draws) {
        logger.info("Set to draw " + draws + " destinations.");
        this.draws = draws;
    }

    /**
     * Build destination choice estimation files for WFRC.
     */
    public static void main(String[] args) {
        // check arguments and print usage
        if (args.length < 5) {
            usage(1);
        }

        // label parameters
        String estFileName = args[0];
        String outFileName = args[1];
        int draws = new Integer(args[2]).intValue();
        String zoneDataFileName = args[3];

        // create DestnationChoiceFile object
        DestinationChoiceEstimationFile dcf = new DestinationChoiceEstimationFile(
                draws);

        for (int i = 4; i < args.length; ++i) {
            dcf.addSkim(args[i]);
        }

        dcf.readZonalData(zoneDataFileName);
        dcf.createEstimationFile(estFileName, outFileName);
    }
}
