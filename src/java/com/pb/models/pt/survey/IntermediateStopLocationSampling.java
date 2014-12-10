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
 * Created on Sep 22, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.survey;

import java.lang.RuntimeException;

import java.util.HashMap;
import java.util.Iterator;

import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.ExternalNumberIterator;
import com.pb.common.matrix.Matrix;

/**
 * This class calculates probabilities and draws samples to estimate an
 * intermediate stop location.
 * 
 * @version 1.0 Andrew Stryker <stryker@pbworld.com>
 */
public class IntermediateStopLocationSampling extends DestinationChoiceSampling {

    private Matrix deviation = null;

    /**
     * Constructor
     */
    public IntermediateStopLocationSampling() {
        super();
    }

    /**
     * Constructor that sets the number of draws.
     */
    public IntermediateStopLocationSampling(int draws) {
        super(draws);
    }

    /**
     * Set lambda using:
     * 
     * lambda = 2 / avg(d_k|ij)
     * 
     * This method must be called before computing probabilities.
     */
    public void setLambda(Matrix distance) {
        double sum = 0;

        logger.info("Calculating the optimal lambda.");

        if (extArray == null) {
            logger.warn("External array not set; iterating over distance");
            setExtArray(distance.getExternalNumbers());
        }
        
        Iterator iIter = new ExternalNumberIterator(extArray);
        while (iIter.hasNext()) {
            int itaz = ((Integer) iIter.next()).intValue();
            logger.info("Looping through origin " + itaz);
            int j = 0;

            Iterator jIter = new ExternalNumberIterator(extArray);
            while (jIter.hasNext()) {
                int jtaz = ((Integer) jIter.next()).intValue();
                double d_ij = distance.getValueAt(itaz, jtaz);

                if (++j % 500 == 0) {
                    logger.info("Looping through destination " + jtaz);
                }

                Iterator kIter = new ExternalNumberIterator(extArray);
                while (kIter.hasNext()) {
                    int ktaz = ((Integer) kIter.next()).intValue();
                    double d_ik = distance.getValueAt(itaz, ktaz);
                    double d_kj = distance.getValueAt(ktaz, jtaz);

                    sum += d_ik + d_kj - d_ij;
                }
            }
        }

        int tazs = extArray.length - 1;
        lambda = 2 / (sum / (tazs * tazs * tazs));
        logger.info("lambda set to: " + lambda);
    }

    // ----- general methods ------------------------------------------//

    /**
     * Compute the probability matrix.
     * 
     * This method calculates exponentiated utilities and probabilities using a
     * simple intermediate stop location model.
     * 
     * The exponentiated utility (u) caluclation s:
     * 
     * u_k|ij = size_k * exp(- lambda * d_k|ij)
     * 
     * where size is the zonal size of the stop location zone, lambda is a
     * coefficient, and d is the out-of-direction distance needed to get to the
     * the stop location. Defining i as the anchor, j as the destination, and k
     * as the stop,
     * 
     * d_k|ij = (d_ik + d_jk) - d_ij
     * 
     * lambda is computed automatically as:
     * 
     * lambda = 2 / avg_k(d_k|ij)
     * 
     * Probabilities are calculated for every TAZ including for the chosen TAZ.
     * The probabilities are computed using the formula:
     * 
     * p_k|ij = exp(u_k|ij) / sum_k(exp(u_k|ij))
     * 
     * The implementation of this class works by first constructing matrix
     * d_jk|i, that is the deviation distance to destination j, given origin i,
     * for anchor k. The problem simplifies to the DestinationChoiceSampling
     * problem. Because of this set-up, programs using this class should sort
     * data their observations by origins and destinations.
     * 
     * @param distance
     *            Matrix of i to j distances.
     * @param size
     *            ColumnVector of zonal size.
     */
    public void computeProbabilities(Matrix distance, ColumnVector size, int itaz) {
        logger.info("Computing probablilities for origin zone " + itaz);

        int tazs = extArray.length - 1;

        // re-use a distance matrix if possible
        if (deviation == null) {
            deviation = new Matrix(tazs, tazs);
            deviation.setExternalNumbers(extArray);
        }

        logger.info("Calculating deviation distance matrix for " + itaz);

        Iterator jIter = new ExternalNumberIterator(extArray);
        while (jIter.hasNext()) {
            int jtaz = ((Integer) jIter.next()).intValue();
            float d_ij = distance.getValueAt(itaz, jtaz);

            Iterator kIter = new ExternalNumberIterator(extArray);
            while (kIter.hasNext()) {
                int ktaz = ((Integer) kIter.next()).intValue();
                float d_ik = distance.getValueAt(itaz, ktaz);
                float d_kj = distance.getValueAt(ktaz, jtaz);
                float d_k = d_ik + d_kj - d_ij;

                distance.setValueAt(jtaz, ktaz, d_k);
            }
        }

        super.computeProbabilities(deviation, size);
    }
    
    /**
     * Override the DestinationChoiceSampling method.
     */
    public void computeProbabilities(Matrix distance, ColumnVector size) {
        throw new RuntimeException("This method is not supported.");
    }

    /**
     * Compute deviation distance.
     */
    public static float deviationDistance(Matrix distance, int itaz, int jtaz,
            int ktaz) {
        float d_ij = distance.getValueAt(itaz, jtaz);
        float d_ik = distance.getValueAt(itaz, ktaz);
        float d_kj = distance.getValueAt(ktaz, jtaz);

        return d_ik + d_kj - d_ij;
    }

    /**
     * Test case adapted from DestinationChoiceSampling.
     */
    public static void main(String[] args) {
        int tazs = 100;
        int ataz = 45;
        int sampSize = 20;
        //int[] itazs = { 11, 22, 33, 44, 100 };
        int[] itazs = { 11, 22 };
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
        IntermediateStopLocationSampling isls = new IntermediateStopLocationSampling();
        isls.setExtArray(extArray);
        isls.setDraws(sampSize);
        isls.setLambda(dist);

        for (int t = 0; t < itazs.length; ++t) {
            int itaz = itazs[t];
            isls.clearSamples();
            isls.computeProbabilities(dist, size, itaz);
            isls.assertAlternative(ataz);
            isls.sampleFromProbabilities(itaz);

            HashMap<Integer, Double> alternatives = isls.getAlternatives();
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
