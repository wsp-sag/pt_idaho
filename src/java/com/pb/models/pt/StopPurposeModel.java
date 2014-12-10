/*
 * Copyright 2006 PB Consult Inc.
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
 *   Created on Mar 3, 2006 by Rosella Picado <picado@pbworld.com>
 */
package com.pb.models.pt;

import com.pb.common.model.ModelException;
import static com.pb.models.pt.ActivityPurpose.*;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.util.Random;
import java.util.ResourceBundle;

/**
 * IntermediateStopPurposeModel
 * 
 * This class implements a model to select the purpose of intermediate stops. It
 * applies to tours in 2-tour day patterns only.
 * 
 * @version 0.2
 * @author Picado
 * 
 */

public class StopPurposeModel extends TimedModel {

    final static Logger logger = Logger
            .getLogger(StopPurposeModel.class);

    private Tracer tracer = Tracer.getTracer();

    private boolean trace = false;

    private StopPurpose2TourTableReader freqTable2T;

    private StopPurpose3PTourTableReader freqTable3PT;

    /**
     * Default constructor. It reads the model parameters for the 2 tour and 3+
     * tour patterns.
     * @param rb Resource Bundle
     */
    public StopPurposeModel(ResourceBundle rb) {
        startTiming();

        freqTable2T = new StopPurpose2TourTableReader(rb);

        freqTable3PT = new StopPurpose3PTourTableReader(rb);

        endTiming();
    }

    /**
     * This method applies the Intermediate Stop Purpose Model.
     * 
     * @param tour  Tour object
     * @param person Person
     * @param random Random number
     */
    public void selectStopPurpose(Tour tour, PTPerson person, Random random) {
        startTiming();
//        ActivityPurpose purpose = tour.getPurpose();

        trace = tracer.isTracePerson(person.hhID + "_" + person.memberID);

        int ntours = person.getTourCount();

        // if one tour, no need to select stop purposes;
//	if (ntours <= 1 || purpose == WORK || purpose == WORK_BASED) {
        if (ntours <= 1) {
	    endTiming();
            return;
        }

        int ptype = person.personType.ordinal();
        int tourNumber = tour.getOrder();
        boolean stop1 = tour.hasOutboundStop();
        boolean stop2 = tour.hasInboundStop();
        ActivityPurpose tourPurpose = tour.getPurpose();
        ActivityPurpose stopPurpose;

        // default stop purpose is Other;
        float[] stopProbabilities = { 0, 0, 1 };

        // Other tours can only have Other stops;
        // If not is not Other, then apply model;

        if (tourPurpose == OTHER) {

            if (stop1) {
                setPurposeStop1(OTHER, tour);
            }

            if (stop2) {
                setPurposeStop2(OTHER, tour);
            }

            endTiming();
            return;
        }

        // determine the position of the tour in the pattern (first,
        // last,
        // mid);

        int tourPosition;

        if (tourNumber == 0) {
            tourPosition = 0;
        } else if (tourNumber == ntours - 1) {
            tourPosition = 2;
        } else {
            tourPosition = 1;
        }

        // choose purpose for the outbound stop;

        if (stop1) {

            if (ntours == 2) {

                stopProbabilities = freqTable2T.getLikelihood(ptype,
                        tourPosition, 1, tourPurpose);
            }

            if (ntours > 2) {

                stopProbabilities = freqTable3PT.getLikelihood(ptype,
                        tourPosition, 1, tourPurpose);
            }

            if (trace) {
                logger.info("Find a stop purpose for person "
                        + (person.hhID + "_" + person.memberID) + ", person type " +  person.personType);
                logger.info("stop 1, tour position " + tourPosition
                        + ", tour purpose " + tourPurpose);
                logger.info("total tours " + person.getTourCount());
            }
            

            try {
        	stopPurpose = choosePurpose(stopProbabilities, random);
            } catch (ModelException e) {
        	logger.error(e);
                logger.error("Could not find a stop purpose for person "
                        + (person.hhID + "_" + person.memberID) + ", person type " +  person.personType);
        	logger.error("stop 1, tour position " + tourPosition
        		+ ", tour purpose " + tourPurpose);
                logger.error("total tours " + person.getTourCount());
        	logger.error("Setting the stop purpose to other.");
        	stopPurpose = ActivityPurpose.OTHER;
            }

            setPurposeStop1(stopPurpose, tour);
        }

        // choose purpose for the inbound stop;

        if (stop2) {

            if (ntours == 2) {

                stopProbabilities = freqTable2T.getLikelihood(ptype,
                        tourPosition, 2, tourPurpose);
            }

            if (ntours > 2) {

                stopProbabilities = freqTable3PT.getLikelihood(ptype,
                        tourPosition, 2, tourPurpose);
            }
            
            if (trace) {
                for (int i = 0; i < stopProbabilities.length; ++i) {
                    logger.info("Alternative " + i + ": " + stopProbabilities[i]);
                }
            }
            
            try {
        	stopPurpose = choosePurpose(stopProbabilities, random);
            } catch (ModelException e) {
        	logger.error(e);
        	logger.error("Could not find a stop purpose for person "
                        + (person.hhID + "_" + person.memberID) + ", person type " +  person.personType);
        	logger.error("stop 2, tour position " + tourPosition
        		+ ", tour purpose " + tourPurpose);
        	logger.error("Setting the stop purpose to other.");
        	stopPurpose = ActivityPurpose.OTHER;
            }

            setPurposeStop2(stopPurpose, tour);
        }
        endTiming();
    }

    /**
     * This method chooses the stop purpose.
     * 
     * @param probabilities probability array
     * @param random Random number
     * @return ActivityPurpose Purpose of activity
     * @throws ModelException No purpose found
     */
    private ActivityPurpose choosePurpose(float[] probabilities, Random random)
	    throws ModelException {
        double rn = random.nextDouble();
        double cp = 0;
        int purpose = -1;

        ActivityPurpose stopPurpose = OTHER;

        if (trace) {
            logger.info("selector: " + rn);
        }
        
        for (int i = 0; i < probabilities.length; ++i) {

            float p = probabilities[i];
            cp += p;

            if (trace) {
                logger.info("purpose " + i + ", probability " + p
                        + ", cummulative propbability " + cp);
            }

            if (rn < cp) {
                purpose = i;
                break;
            }
        }

        if (purpose == -1) {
            String message = "Could not find a purpose!";
            logger.error(message);
            throw new ModelException(message);
        }

        switch (purpose) {
        case 0:
            stopPurpose = SHOP;
            break;
        case 1:
            stopPurpose = RECREATE;
            break;
        case 2:
            stopPurpose = OTHER;
            break;
        default:
            logger.error("No purpose found for this stop");
        }

//        if (rn >= cp) {
//            // should be impossible to reach this
//            logger.error("Did not find a stop purpose ");
//            throw new RuntimeException("Did not find a stop purpose");
//        }

        if (trace) {
            logger.info("Selected stop purpose " + stopPurpose);
            logger.info("Random seletor :" + rn);
        }

        return stopPurpose;
    }

    /**
     * This method sets the purpose of the outbound intermediate stop.
     * 
     * @param purpose Activity purpose
     * @param tour  Tour object
     */
    private void setPurposeStop1(ActivityPurpose purpose, Tour tour) {
        startTiming();
        tour.intermediateStop1.activityPurpose = purpose;
        endTiming();
    }

    /**
     * This method sets the purpose of the inbound intermediate stop.
     * 
     * @param purpose Activity purpose
     * @param tour  Tour object
     */
    private void setPurposeStop2(ActivityPurpose purpose, Tour tour) {
        startTiming();
        tour.intermediateStop2.activityPurpose = purpose;
        endTiming();
    }

}
