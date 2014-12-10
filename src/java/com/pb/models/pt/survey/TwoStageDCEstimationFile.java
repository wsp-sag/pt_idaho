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

package com.pb.models.pt.survey;

import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Create a two-stage destination choice estimation file.
 * 
 * This estimation file creator is for DC models that predict movements from
 * zone to district and then allocate district attractions back to the zonal
 * level. Additionally, zones are part of the alternative set only if they meet
 * a threshold.
 * 
 * One complication of this approach is that only _portions_ of districts may be
 * available in the alternative set. Consequently, the district size variable
 * must reflect the size of the zones included in the alternative set.
 * 
 * @author stryker
 * 
 */
public class TwoStageDCEstimationFile {
    protected static Logger logger = Logger.getLogger("com.pb.osmp");

    private double threshold = 0;

    private int draws = 20; // default value

    private ColumnVector zonalSize;

    private Matrix zonalDistance;

    private Matrix slatDistance;

    private HashMap<Integer, Integer> districtToZone;

    private HashMap<Integer, Integer> zoneToDistrict; // only relevant zones

    private ArrayList<Matrix> costs = new ArrayList<Matrix>();

    private ArrayList<ColumnVector> zonalVectors = new ArrayList<ColumnVector>();

    private int[] extArray;

    /**
     * Constructor.
     */
    public TwoStageDCEstimationFile() {
    }

    /**
     * Constructor.
     * 
     * @param draws
     *            The number of samples to draw.
     */
    public TwoStageDCEstimationFile(int draws) {
        this.draws = draws;
    }

    /**
     * Set the district to zone mapping.
     * 
     * @param districtToZone
     */
    public void setDistrictToZone(HashMap<Integer, Integer> districtToZone) {
        this.districtToZone = districtToZone;
    }

    /**
     * Calculate the size vector.
     * 
     * This method calculates a size vector as a function of the origin zone.
     * 
     * @return
     */
    public ColumnVector districtVector(ColumnVector zonal, int itaz) {
        ColumnVector dv = new ColumnVector(zoneToDistrict.size());
        dv.setExternalNumbers(slatDistance.getExternalNumbers());
        dv.setName(zonal.getName());

        // do not assume the cost or size objects have external arrays that
        // conform
        Iterator<Integer> zoneIter = zoneToDistrict.keySet().iterator();
        while (zoneIter.hasNext()) {
            int j = zoneIter.next();
            int d = zoneToDistrict.get(j);
            int proxy = districtToZone.get(d);

            float dist = zonalDistance.getValueAt(itaz, j);
            if (dist > threshold) {
                float value = dv.getValueAt(proxy) + zonal.getValueAt(j);
                dv.setValueAt(proxy, value);
            }
        }

        return dv;
    }

    /**
     * Create the estimation file.
     */
    public void createEstimationFile(String inFile, String outFile) {
        DestinationChoiceSampling dcs = new DestinationChoiceSampling(draws);

        // set the external array
        dcs.setExtArray(extArray);

        // use the full matrix for calculating lambda (we do not want to aver
        // the slat matrix as it is full of 0s)
        dcs.setLambda(zonalDistance);

        ArrayList<ColumnVector> distVectors = null;
        HashSet<Integer> tazSet = new HashSet<Integer>(zoneToDistrict.keySet());

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

            // grab header -- we need the column positions of the itaz and the
            // jtaz
            String line = reader.readLine();
            String[] header = line.split(",");

            int itazCol = -1;
            int jtazCol = -1;

            for (int i = 0; i < header.length; ++i) {
                if (header[i].equals("itaz")) {
                    itazCol = i;
                } else if (header[i].equals("jtaz")) {
                    jtazCol = i;
                }
            }

            if (itazCol < 0 || jtazCol < 0) {
                String msg = "Could not find itaz and jtaz columns in "
                        + inFile;
                logger.fatal(msg);
                throw new RuntimeException(msg);
            }

            // create new header
            line += ",chosen,cf_c";

            // cost matrices -- chosen
            for (int i = 0; i < costs.size(); ++i) {
                line += "," + costs.get(i).getName() + "_c";
            }

            // zonal data -- chosen
            for (int i = 0; i < zonalVectors.size(); ++i) {
                line += "," + zonalVectors.get(i).getName() + "_c";
            }

            // loop over each draw
            for (int j = 0; j < draws; ++j) {
                line += ",sample_" + j + ",cf_" + j;

                // cost matrices
                for (int i = 0; i < costs.size(); ++i) {
                    line += "," + costs.get(i).getName() + "_" + j;
                }

                // zonal data
                for (int i = 0; i < zonalVectors.size(); ++i) {
                    line += "," + zonalVectors.get(i).getName() + "_" + j;
                }
            }

            writer.write(line + "\n");

            int p = -1;
            ColumnVector distSize = null;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                if (fields.length != header.length) {
                    String msg = "Inconsistent number of fields.  Found "
                            + fields.length + " and expected " + header.length;
                    logger.fatal(msg);
                    throw new RuntimeException(msg);
                }

                int itaz = new Integer(fields[itazCol]);

                if (!tazSet.contains(itaz)) {
                    logger.warn("Skipping itaz that is not the zone system: "
                            + itaz);
                    continue;
                }

                // prepare the sampler

                dcs.clearSamples();

                // everytime there is a new origin, we need to calculate new
                // probabilities
                if (itaz != p) {
                    logger.info("New itaz: " + itaz);

                    distSize = districtVector(zonalSize, itaz);
                    dcs.computeProbabilities(slatDistance, distSize);

                    // convert zonal costs to slat format
                    distVectors = new ArrayList<ColumnVector>();
                    Iterator<ColumnVector> zVectIter = zonalVectors.iterator();
                    while (zVectIter.hasNext()) {
                        distVectors.add(districtVector(zVectIter.next(), itaz));
                    }

                    p = itaz;
                }

                int jtaz = new Integer(fields[jtazCol]);

                int proxy;
                if (tazSet.contains(jtaz)) {
                    int jdist = zoneToDistrict.get(jtaz);
                    proxy = districtToZone.get(jdist);
                } else {
                    proxy = -1;
                }

                // only assert the chosen if it is the zone system
                if (tazSet.contains(proxy)) {
                    dcs.assertAlternative(proxy);
                }

                logger.info("Sampling for (" + itaz + "," + jtaz + " -> "
                        + proxy + ")");

                if (tazSet.contains(proxy) && distSize.getValueAt(proxy) == 0) {
                    logger.warn("District size is 0.");
                }

                dcs.sampleFromProbabilities(itaz);

                Map<Integer, Double> alternatives = dcs.getAlternatives();
                Set<Integer> sampleSet = new HashSet<Integer>(alternatives
                        .keySet());

                // remove the chosen from the sampled set
                if (sampleSet.contains(proxy)) {
                    sampleSet.remove(proxy);
                }

                ArrayList<Integer> samples = new ArrayList<Integer>(sampleSet);

                if (tazSet.contains(proxy)) {
                    line += "," + proxy + "," + alternatives.get(proxy);
                } else {
                    line += "," + proxy + ",0";
                }

                // cost matrices -- chosen
                if (tazSet.contains(proxy)) {
                    for (int i = 0; i < costs.size(); ++i) {
                        line += "," + costs.get(i).getValueAt(itaz, proxy);
                    }
                } else {
                    for (int i = 0; i < costs.size(); ++i) {
                        line += ",-1";
                    }
                }

                // zonal data -- chosen
                if (tazSet.contains(proxy)) {
                    for (int i = 0; i < distVectors.size(); ++i) {
                        line += "," + distVectors.get(i).getValueAt(proxy);
                    }
                } else {
                    for (int i = 0; i < distVectors.size(); ++i) {
                        line += ",-1";
                    }
                }

                // loop over each draw
                for (int j = 0; j < draws; ++j) {

                    if (j < samples.size()) {
                        proxy = samples.get(j);
                        line += "," + proxy + "," + alternatives.get(proxy);
                    } else {
                        proxy = -9;
                        line += "," + proxy + ",0";
                    }

                    if (j < samples.size()) {
                        // cost matrices
                        for (int i = 0; i < costs.size(); ++i) {
                            line += "," + costs.get(i).getValueAt(itaz, proxy);
                        }

                        // zonal data
                        for (int i = 0; i < distVectors.size(); ++i) {
                            line += "," + distVectors.get(i).getValueAt(proxy);
                        }

                    } else {
                        // cost matrices
                        for (int i = 0; i < costs.size(); ++i) {
                            line += ",-1";
                        }

                        // zonal data
                        for (int i = 0; i < distVectors.size(); ++i) {
                            line += ",-1";
                        }
                    }
                }

                writer.write(line + "\n");

            }

            reader.close();
            writer.close();

        } catch (IOException e) {
            logger.fatal("I/O problem: " + e);
            throw new RuntimeException(e);
        }
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public Matrix getSlatDistance() {
        return slatDistance;
    }

    public void setSlatDistance(Matrix slatDistance) {
        this.slatDistance = slatDistance;
    }

    public Matrix getZonalDistance() {
        return zonalDistance;
    }

    public void setZonalDistance(Matrix zoneDistance) {
        this.zonalDistance = zoneDistance;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * @param costs
     *            The costs (in slat format) to set.
     */
    public void setCosts(ArrayList<Matrix> costs) {
        this.costs = costs;
    }

    /**
     * @param cost
     *            Append a cost matrix (in slat format) to the list.
     */
    public void appendCost(Matrix cost) {
        costs.add(cost);
    }

    /**
     * @param zonalSize
     *            The zonalSize to set.
     */
    public void setZonalSize(ColumnVector zonalSize) {
        this.zonalSize = zonalSize;
    }

    /**
     * @param zoneToDistrict
     *            The zoneToDistrict to set.
     */
    public void setZoneToDistrict(HashMap<Integer, Integer> zoneToDistrict) {
        this.zoneToDistrict = zoneToDistrict;
    }

    /**
     * @param extArray
     *            The extArray to set.
     */
    public void setExtArray(int[] extArray) {
        this.extArray = extArray;
    }

    /**
     * @param zonalVectors
     *            The zonalVectors to set.
     */
    public void setZonalVectors(ArrayList<ColumnVector> zonalVectors) {
        this.zonalVectors = zonalVectors;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

    }

}
