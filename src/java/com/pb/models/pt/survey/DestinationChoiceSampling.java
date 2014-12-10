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
// DCEstimationFileConstruction.java
// Created on: Apr 2, 2004
// Last Change:  2004 June 1
package com.pb.models.pt.survey;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.ExternalNumberIterator;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.RowVector;

import java.lang.RuntimeException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Develop sample TAZs and correction factors for a destination choice model.
 * 
 * This class should be substantially revised before inclusion in a
 * micro-simulation model.
 * 
 * @author Andrew Stryker <stryker@pbworld.com>
 * @version 2.0 05 Oct 2005
 */
public class DestinationChoiceSampling {
    protected static Logger logger = Logger.getLogger("com.pb.common.matrix");

    protected int draws = 0; // number of samples to draw

    // map alternatives onto the number of times sampled
    protected HashMap<Integer, Integer> samples = new HashMap<Integer, Integer>();

    // map alternatives onto correction factors
    protected HashMap<Integer, Double> alternatives = null;

    protected HashSet<Integer> exclusions = null;

    protected double lambda = 0;

    protected double scale = 1;

    protected Matrix probabilities = null;

    protected int[] extArray = null;

    protected boolean surpressConformWarning = false;

    /**
     * Constructor.
     */
    public DestinationChoiceSampling() {
    }

    /**
     * Constructor that also sets the sample size.
     */
    public DestinationChoiceSampling(int draws) {
        setDraws(draws);
    }

    /**
     * Set the number of draws.
     */
    public void setDraws(int draws) {
        this.draws = draws;
    }

    /**
     * Set the probability matrix.
     * 
     * The probability matrix can either be set or computed automatically.
     * 
     * @param probabilities
     *            The probabilities to set.
     */
    public void setProbabilities(Matrix probabilities) {
        this.probabilities = probabilities;
    }

    /**
     * @return Returns the probabilities.
     */
    public Matrix getProbabilities() {
        return probabilities;
    }

    /**
     * Include a taz in the alternative set and consider the alternative when
     * computing correction factors.
     * 
     * Use this method to ensure that alternatives other than the chosen
     * alternative and other alternative are part of the choice set.
     * 
     * @param taz
     *            The asserted alternative taz.
     */
    public void assertAlternative(int taz) {
        Integer key = new Integer(taz);
        Integer a = 0;

        if (samples.containsKey(key)) {
            a = samples.get(key);
        }

        a += 1;
        samples.put(key, a);
    }

    /**
     * Clear the sample set.
     * 
     * This method resets the set of drawn and asserted alternatives.
     */
    public void clearSamples() {
        samples = new HashMap<Integer, Integer>();
        alternatives = null;
    }

    /**
     * Sample size.
     * 
     * Total number of samples (both drawn and asserted).
     */
    public int getSampleSize() {
        int size = 0;

        Iterator<Integer> iter = samples.values().iterator();
        while (iter.hasNext()) {
            size += iter.next();
        }

        return size;
    }

    /**
     * The alternatives excluded from the universe.
     * 
     * @return HashSet of excluded alternatives.
     */
    public HashSet<Integer> getExclusions() {
        return exclusions;
    }

    /**
     * Exclude alternatives from universe.
     * 
     * @param exclusions
     *            HashSet of alternatives to exclude.
     */
    public void setExclusions(HashSet<Integer> exclusions) {
        this.exclusions = exclusions;
    }

    /**
     * Clear the set of excluded alternatives.
     */
    public void clearExclusions() {
        if (exclusions != null) {
            exclusions.clear();
        } else {
            exclusions = new HashSet<Integer>();
        }
    }

    /**
     * Add alternative to the set of excluded alternatives.
     * 
     * @param alternative
     *            The alternative to exclude.
     */
    public void addExclusion(int alternative) {
        if (exclusions == null) {
            exclusions = new HashSet<Integer>();
        }

        addExclusion(new Integer(alternative));
    }

    /**
     * Add alternative to the set of excluded alternatives.
     * 
     * @param alternative
     *            The alternative to exclude.
     */
    public void addExclusion(Integer alternative) {
        if (exclusions == null) {
            exclusions = new HashSet<Integer>();
        }

        exclusions.add(alternative);
    }

    /**
     * Set lambda.
     * 
     * @param lambda
     *            The lambda value.
     */
    public void setLambda(double lambda) {
        this.lambda = lambda;
        logger.info("lambda set to " + lambda);
    }

    /**
     * Set lambda using:
     * 
     * lambda = 2 / avg(distance)
     * 
     * @param distance
     *            The distance (separation) matrix
     * 
     * Lambda must be called before computing probabilities.
     * 
     * @deprecated The average distance should be the average observed distance.
     */
    public void setLambda(Matrix distance) {
        int rows = distance.getRowCount();
        int cols = distance.getColumnCount();

        lambda = 2 / (distance.getSum() / (rows * cols));

        logger.info("lambda set to " + lambda);
    }

    public int[] getExtArray() {
        return extArray;
    }

    public void setExtArray(int[] extArray) {
        this.extArray = extArray;
    }

    // ----- general methods ------------------------------------------//

    /**
     * Compute the probability matrix.
     * 
     * This method calculates exponentiated utilities and probabilities using a
     * simple destination choice model.
     * 
     * The exponentiated utility (u) caluclation s:
     * 
     * u = aSize * exp(- lambda * d)
     * 
     * where aSize is the zonal size of the attraction zone, lambda is a
     * coefficient, and d is the distance.
     * 
     * lambda is computed automatically as:
     * 
     * lambda = 2 / avg(distance)
     * 
     * Probabilities are calculated for every TAZ including for the chosen TAZ.
     * The probabilities are computed using the formula:
     * 
     * p_j|i = exp(u_ij) / sum_j(exp(u_ij))
     * 
     * @param distance
     *            Matrix of disatance.
     * @param size
     *            ColumnVector of zonal size.
     */
    public void computeProbabilities(int row, Matrix distance, ColumnVector size) {
        int tazs;

        if (lambda == 0) {
            logger.warn("Lambda unset or set to 0.");
            throw new RuntimeException("Lambda unset or set to 0.");
        }

        // initialize probability matrix
        if (probabilities == null) {
            if (extArray == null) {
                logger
                        .warn("External array not set.  Iterating over size vector.");
                setExtArray(size.getExternalNumbers());
                tazs = size.getRowCount();
            } else {
                tazs = extArray.length - 1;
            }

            if (!surpressConformWarning && tazs != distance.getRowCount()) {
                logger.warn("The size and distance matrices do not conform.");
                logger.warn("Size vector has " + tazs
                        + " zones and the distance matrix has "
                        + distance.getRowCount());
                surpressConformWarning = true;
            }

            logger.debug("Found " + tazs + " TAZs.");

            probabilities = new Matrix(tazs, tazs);
            probabilities.setExternalNumbers(extArray);
        }

        RowVector expUtilities = new RowVector(probabilities.getRowCount());
        expUtilities.setExternalNumbers(extArray);

        // calculate the exponentiated utilities
        Iterator jIter = new ExternalNumberIterator(extArray);

        double sumExpU = 0;

        // compute expotentiated utilities for each row
        while (jIter.hasNext()) {
            int col = (Integer) jIter.next();
            double d;
            double u;

            double aSize = size.getValueAt(col);

            if (aSize > 0 && (exclusions == null || !exclusions.contains(col))) {
                d = distance.getValueAt(row, col);
                u = aSize * Math.exp(-lambda * d) * scale;
                logger.debug("Calculation for " + row + ", " + col + ": " + u
                        + " = " + aSize + " x exp(-" + lambda + " x " + d
                        + ") x " + scale);

            } else {
                u = 0;
                logger.debug("Skipping (" + row + ", " + col + ") because of "
                        + "zero attraction zone activity or exclusion.");
            }

            expUtilities.setValueAt(row, col, (float) u);
            sumExpU += u;
        }

        // compute and store probabilities
        jIter = size.getExternalNumberIterator();
        while (jIter.hasNext()) {
            int col = ((Integer) jIter.next()).intValue();
            float u = expUtilities.getValueAt(col);
            double p = u / sumExpU;

            logger.debug("Probability for (" + row + "," + col + ") : " + p
                    + " = " + u + " / " + sumExpU);

            probabilities.setValueAt(row, col, (float) p);
        }
    }

    /**
     * Compute probabilities.
     * 
     * Compute the probabilities for the entire matrix.
     */
    public void computeProbabilities(Matrix dist, ColumnVector size) {
        Iterator itazIter = size.getExternalNumberIterator();

        while (itazIter.hasNext()) {
            int row = (Integer) itazIter.next();
            computeProbabilities(row, dist, size);
        }
    }

    /**
     * Sample TAZs from the probibilities using Monte Carlo methods.
     * 
     * The samples are generated using a number draw from a uniform distribution
     * [0, 1.0) and compared to the cummulative probabiliy from a particular
     * itaz to each ataz. The cummulative probabiliy from itaz to attraction
     * TAZ[n] is the cummulative probabiliy for TAZ[n-1] plus probabiliy[itaz,
     * n]. The cummulative probabiliy of TAZ[1] is probabiliy[itaz, 1].
     * 
     * This method also computes the correction factors. The correction factors
     * for a sample is computed as the occurances of a TAZ is the sampling,
     * divided by the probability sampling that TAZ.
     * 
     * The sample TAZs in HashMap with coreection factors are available via
     * getSamples.
     * 
     * @param itaz
     *            The TAZ to sample.
     */
    public void sampleFromProbabilities(int itaz) {
        sampleDestinations(itaz);
        computeCorrections(itaz);
    }

    /**
     * Sample destinations to create the sample array.
     * 
     * @param itaz
     *            Production TAZ.
     */
    protected void sampleDestinations(int itaz) {
        Iterator jIter;
        int count;
        alternatives = new HashMap<Integer, Double>();

        for (int d = 0; d < draws; ++d) {
            double rn = Math.random();
            double cp = 0;

            // iterate over columns using only through the external number array
            jIter = new ExternalNumberIterator(extArray);
            while (jIter.hasNext()) {
                int t = (Integer) jIter.next();
                float p = probabilities.getValueAt(itaz, t);
                cp += p;

                if (logger.getLevel() == Level.DEBUG) {
                    logger.debug("Commulative probability for TAZ " + t + ": "
                            + cp);
                }

                // look for the first instance of a the random number being
                // less than the cummulative probability
                if (rn < cp) {
                    if (logger.getLevel() == Level.DEBUG) {
                        logger.debug("Sample ataz is " + t + " : " + rn + " < "
                                + cp);
                        logger.debug("Sample " + d + " for itaz " + itaz
                                + " and random draw of " + rn + " is " + t
                                + ".");
                    }

                    if (samples.containsKey(t)) {
                        count = samples.get(t) + 1;
                    } else {
                        count = 1;
                    }

                    samples.put(t, count);

                    if (count > 1) {
                        logger.debug("Occurrence " + count + " of sample TAZ "
                                + t + ".");
                    }

                    break; // next sample
                }
            }

            if (rn >= cp) {
                // should be impossible to reach this
                logger.error("Did not find a sample for " + itaz + "!");
                throw new RuntimeException("Did not find a sample for " + itaz
                        + "!");
            }

        }
    }

    /**
     * Compute correction factors.
     */
    protected void computeCorrections(int itaz) {
        Iterator<Integer> iter = samples.keySet().iterator();
        int sampleSize = getSampleSize();

        while (iter.hasNext()) {
            Integer key = iter.next();

            float p;
            // checking the zone system would be a better approach
            if (key > 0) {
                p = probabilities.getValueAt(itaz, key);
            } else {
                p = 0;
            }

            // p is not positive when the alternative has a 0 size term
            if (p > 0) {
                int freq = samples.get(key).intValue();
                double cf = -Math.log(p / (freq / ((double) sampleSize)));
                alternatives.put(key, cf);
                if (logger.getLevel() == Level.DEBUG) {
                    logger.debug("Correction factor for TAZ " + key + ", is "
                            + cf + " = -ln(" + p + " / (" + freq + " / "
                            + sampleSize + "))");
                }
            } else {
                logger.warn("Alternative " + key
                        + " has a selection probability of 0.");
                alternatives.put(key, 0.0);
            }
        }
    }

    // ----- getters --------------------------------------------------//

    /**
     * Get a HashMap of alternatives.
     * 
     * The HashMap maps sampled and asserted alternatives to correction factors.
     * 
     * @return Returns the correctionFactors.
     */
    public HashMap<Integer, Double> getAlternatives() {
        if (alternatives == null) {
            logger.fatal("Alternatives requested but not computed.");
            throw new RuntimeException(
                    "Alternatives requested but not computed.");
        }
        return alternatives;
    }

    /**
     * @return Returns the scale.
     */
    public double getUtilityScale() {
        return scale;
    }

    /**
     * @param scale
     *            The scale to set.
     */
    public void setUtilityScale(double scale) {
        this.scale = scale;
    }

    // ----- testing --------------------------------------------------//

    /**
     * Test the class.
     * 
     * Review the caluclations in the log files to check caculations.
     */
    public static void main(String[] Args) {
        int tazs = 100;
        int ataz = 45;
        int sampSize = 10;
        int[] itazs = { 11, 22, 33, 44, 100 };
        float val;
        Matrix dist = new Matrix(tazs, tazs);
        ColumnVector size = new ColumnVector(tazs);

        // external array -- to make sure that numbering gaps are handled
        // correctly
        int[] extArray = new int[tazs + 1];

        for (int i = 0; i < extArray.length; ++i) {
            if (i > 50) {
                extArray[i] = i + 5;
            } else {
                extArray[i] = i;
            }
        }

        dist.setExternalNumbers(extArray);
        size.setExternalNumbers(extArray);

        // compute a distance matrix
        Iterator i = dist.getExternalNumberIterator();
        Iterator j = dist.getExternalNumberIterator();

        while (i.hasNext()) {
            int r = ((Integer) i.next()).intValue();

            while (j.hasNext()) {
                int c = ((Integer) j.next()).intValue();

                double fudge = (Math.random() + 1) / 3;

                if (i == j) {
                    val = (float) (fudge * fudge);
                } else {
                    val = (float) (Math.sqrt(fudge * Math.abs(r - c)) * fudge);
                }

                dist.setValueAt(r, c, val);
            }
        }

        // zone size are generated using a uniform random distribution in the
        // range [0, 50000) except for every 10th taz; these are set to 0
        i = size.getExternalNumberIterator();

        while (i.hasNext()) {
            int r = ((Integer) i.next()).intValue();

            if ((r % 10) == 0) {
                val = 0;
            } else {
                val = (float) (Math.random() * 50000);
            }

            size.setValueAt(r, val);
        }

        // set-up the object
        DestinationChoiceSampling dc = new DestinationChoiceSampling();
        dc.setExtArray(extArray);
        dc.setDraws(sampSize);
        dc.setLambda(dist);
        dc.computeProbabilities(dist, size);

        for (int t = 0; t < itazs.length; ++t) {
            int itaz = itazs[t];
            dc.clearSamples();

            logger.info("Sampling for origin " + itaz + " and attraction "
                    + ataz + ".");

            dc.assertAlternative(ataz);
            dc.sampleFromProbabilities(itaz);

            HashMap<Integer, Double> alternatives = dc.getAlternatives();
            System.out.println("itaz: " + itaz);

            Iterator<Integer> iter = alternatives.keySet().iterator();
            while (iter.hasNext()) {
                Integer taz = iter.next();
                Double cf = alternatives.get(taz);
                logger.info("Sample " + taz + ": " + cf);
            }
        }
    }
}
