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
 * Created on Sep 20, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.survey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.pb.common.matrix.Matrix;

public class IntermediateStopEstimationFile extends
        DestinationChoiceEstimationFile {

    public IntermediateStopEstimationFile() {
        super();
    }

    public IntermediateStopEstimationFile(int draws) {
        super(draws);
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
        logger.info("Creating an Intermediate Stop Location Estimation File.");

        logger.info("Preparing for sampling.");
        IntermediateStopLocationSampling isls = new IntermediateStopLocationSampling(
                draws);

        // the separation distance *must* be the first matrix
        Matrix dist = costs.get(0);
        extArray = dist.getExternalNumbers();
        tazs = dist.getColumnCount();

        // it's best to explicately set the external number array
        isls.setExtArray(extArray);

        // lambda is not automatically calculated
        isls.setLambda(dist);

        // set-up for the probability calculations
        int tazColumn = zonalData.getColumnPosition("taz");
        int sizeColumn = zonalData.getColumnPosition("size");
        zSize = createColumnVector(zonalData, tazColumn, sizeColumn);
        isls.computeProbabilities(dist, zSize);

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
                int itaz = new Integer(flds[positions.get("itaz").intValue()]).intValue();
                int jtaz = new Integer(flds[positions.get("jtaz").intValue()]).intValue();
                int ktaz = new Integer(flds[positions.get("ktaz").intValue()]).intValue();
                
                // skip TAZs that are not part of the zone system defined in
                // the zonal data file
                if (!tazSet.contains(new Integer(itaz))
                        || !tazSet.contains(new Integer(jtaz))
                        || !tazSet.contains(new Integer(ktaz))) {
                    logger.info("Skipping zone triplet (" + itaz + ", " + jtaz
                            + ", " + ktaz + ")");

                    continue;
                }

                // skip where the size of the attraction variable is 0 or less
                if (zSize.getValueAt(ktaz) <= 0) {
                    logger.info("Skipping zone triplet (" + itaz + ", " + jtaz + ", " + ktaz
                            + ") due to a non-positive size.");

                    continue;
                }

                logger.info("Sampling for record " + r + ", TAZ pair (" + itaz
                        + ", " + jtaz + ", " + ktaz + ")");

                // clear the sampled set and then assert alternatives
                isls.clearSamples();
                // dc.assertAlternative(ataz);

                logger.info("Drawing " + draws + " alternatives.");
                isls.sampleFromProbabilities(itaz);
                samples = isls.getAlternatives();

                // create a new entry in the estimation file
                out.print(line);

                // correction factor for the chosen (asserted) alternative
                out.print("," + samples.get(new Integer(jtaz)));

                // append zonal data and travel cost data
                printZonalData(jtaz);
                printSkims(itaz, jtaz);

                // intra-zonal data
                printZonalData(ktaz);
                printSkims(jtaz, ktaz);

                // work through the samples, excluding the chosen
                // append to output line
                int alternatives = samples.size();
                Integer sampleArray[] = (Integer[]) samples.keySet().toArray();
                int s = 0;
                for (; s < sampleArray.length; ++s) {
                    int staz = sampleArray[s].intValue();
                    if (staz == ktaz) {
                        continue;
                    }
                    out.print("," + staz + "," + samples.get(new Integer(s)));

                    // append data
                    printZonalData(staz);
                    printSkims(jtaz, staz);
                }

                // sometimes the sample set has duplicates
                for (; s < alternatives; ++s) {
                    int staz = 0;
                    out.print("," + staz + "," + samples.get(new Integer(s)));

                    // append data
                    printZonalData(staz);
                    printSkims(jtaz, staz);
                }
                out.println();
            }

            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
