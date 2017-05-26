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
package com.pb.models.pt;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixWriter;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.utils.Tracer;

import org.apache.log4j.Logger;

import java.util.Enumeration;
import java.util.Random;

/**
 * WorkplaceLocationModel determines the workplace location of a person using
 * labor flows between zones by occupation. The model uses a Monte Carlo
 * Selection.
 * 
 * @author Steve Hansen
 * @version 1.0 03/28/2004
 * 
 */

public class WorkplaceLocationModel {
    private static Logger logger = Logger
            .getLogger(WorkplaceLocationModel.class);

    private static Tracer tracer = Tracer.getTracer();

    /**
     *
     * @param laborFlowProbabilities matrix of labor flows
     * @param person person
     * @param random Random number generator
     */
    public static void chooseWorkplace(Matrix laborFlowProbabilities, PTPerson person,
            Random random, String debugDirPath, int[] azones) {
        int destination = 0;
        boolean trace = tracer.isTracePerson(person.hhID + "_" + person.memberID);

        if (!person.employed) {
            return;
        }

        double selector = random.nextDouble();
        // sum is a running calculation of the total proportion of labor flows
        double probabilityTotal = 0;
        int counter = 0;
        int origin = (int) person.homeTaz;
        double total = laborFlowProbabilities.getRowSum(origin);
        if (trace) {
            logger.info("total labor flows from origin " + origin + " is " + total);
        }
        
        // only run if SEAM has labor flows
        if (total>0) {
            for (int i = 1; i < azones.length; i++) {
                counter++;

                // in case the probabilities don't add up exactly to one we will
                // scale them here.
                double prob = laborFlowProbabilities.getValueAt(origin, azones[i]) / total;

                probabilityTotal += prob;

                if (trace) {
                    logger.info("dest: " + azones[i] + ", prob: " + prob + ", probTotal: "
                            + probabilityTotal);
                }

                if (probabilityTotal > selector) {
                    destination = azones[i];
                    logger.debug("Found a TAZ!:  " + destination);
                    break;
                }

                if ((i + 1) == azones.length && probabilityTotal + 0.001 > 1.0) {
                    destination = azones[i];
                    logger.debug("Using last TAZ:  " + destination);
                    logger.warn("Selector value: " + selector + " Probability total: "
                            + probabilityTotal + " Counter: " + counter);

                    break;
                }
            }
        }

        // the rowsum was zero, or there was some other problem
        if (destination == 0 || total==0) {
            logger.error("Error With workplace location model - "
                    + "destination TAZ or row total shouldn't ==0!");
            logger.error("Selector value: " + selector + " Probability total: "
                    + probabilityTotal + " Counter: " + counter);
            logger.error("PersonID " + (person.hhID + "_" + person.memberID) + " Home TAZ "
                    + person.homeTaz + " Occupation " + person.occupation.name()
                    + " WorkSegment " + person.segment);
            logger.error("Assigning work taz as home zone."); 
            
            // in the interest of not stopping the model run we will assign
            // the
            // destinationTaz to be the originTaz. The user will be notified
            // at the
            // end of the run
            destination = (int) person.homeTaz;
        }

        person.workTaz = (short) destination;
    }
    
}