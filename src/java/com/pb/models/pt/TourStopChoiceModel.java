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
 *   Created on March 1, 2006 by Rosella Picado <picado@pbworld.com>
 */
package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import static com.pb.models.pt.ActivityPurpose.*;
import static com.pb.models.pt.TourStopParameters.*;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * Tour stop model alternatives.
 */
enum TourStopAlternative {
    NONE, OUTBOUND, INBOUND, BOTH
}

/**
 * TourStopChoiceModel
 * 
 * This class chooses the intermediate stops for tours that are part of a 3+
 * tour pattern. The choices are none, inbound, outbound, or both.
 * 
 * @version 0.1
 * @author Picado
 * 
 */
public class TourStopChoiceModel extends TimedModel {

    final static Logger logger = Logger.getLogger(TourStopChoiceModel.class);

    // array dimensioned by tour purpose and number of parameters
    private float[][] parameters;

    private Tracer tracer = Tracer.getTracer();

    private LogitModel[] tourStopModel;
    private LogitModel lm;
    
    private Tour tour;

    boolean trace;
    private PatternModelPersonAttributes attributes = new PatternModelPersonAttributes();

    /**
     * Default constructor. Reads the model parameters file.
     * @param rb Resource Bundle 
     */
    public TourStopChoiceModel(ResourceBundle rb) {
        startTiming();
        // read the tour stop choice parameters
        String fileName = ResourceUtil.getProperty(rb,
                "sdt.tour.stop.choice.parameters");
        logger.info("Reading Tour Stop Model Parameters from file "
                        + fileName);
        try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fileName));
            parameters = table.getValues();
        } catch (IOException e) {
            logger.fatal("Can't find Tour Stop Model Parameters file "
                    + fileName);
            throw new RuntimeException(e);
        }

        if (tracer.isTraceOn() && logger.isDebugEnabled()) {

            logger.info("Printing tour stop choice model parameters ...");

            for (int i = 0; i < parameters.length; ++i) {
                for (int j = 0; j < parameters[i].length; ++j) {
                    logger.info("row " + i + ", col " + j + ", value = "
                            + parameters[i][j]);
                }
            }
        }
        endTiming();
    }

    /**
     * This method declares an array of LogitModels (one per tour purpose). Each
     * logit model has four alternatives: no stops, one inbound stop, one
     * outbound stop, or both inbound and outbound stops.
     */
    public void buildModel() {
        startTiming();
        int models = parameters.length;
        logger.debug("Creating " + models + " logit models.");
        tourStopModel = new LogitModel[models];

        for (int i = 0; i < models; ++i) {
            tourStopModel[i] = new LogitModel("Stop choice " + i);
            for (TourStopAlternative alt : TourStopAlternative.values()) {
                ConcreteAlternative concreteAlternative = new ConcreteAlternative(
                        alt.name(), alt.ordinal());
                tourStopModel[i].addAlternative(concreteAlternative);
            }
        }
        endTiming();
    }

    /**
     * Calculate the utilities for each alternative.
     * @param household PTHousehold
     * @param person PTPerson
     * @param tour Tour object
     * @return double utilties
     */
    public double calculateUtilities(PTHousehold household, PTPerson person,
            Tour tour) {
        startTiming();
        this.tour = tour;
        trace = tracer.isTracePerson(person.hhID + "_" + person.memberID);

        attributes.calculateHouseholdType(household);
        int hh_types = (CNONE_HTYPE15 - CNONE_HTYPE1) / 4 + 1;
        double[] hh_type = new double[hh_types];

        hh_type[0] = attributes.hType1;
        hh_type[1] = attributes.hType2;
        hh_type[2] = attributes.hType3;
        hh_type[3] = attributes.hType4;
        hh_type[4] = attributes.hType5;
        hh_type[5] = attributes.hType6;
        hh_type[6] = attributes.hType7;
        hh_type[7] = attributes.hType8;
        hh_type[8] = attributes.hType9;
        hh_type[9] = attributes.hType10;
        hh_type[10] = attributes.hType11;
        hh_type[11] = attributes.hType12;
        hh_type[12] = attributes.hType13;
        hh_type[13] = attributes.hType14;
        hh_type[14] = attributes.hType15;

        ActivityPurpose purpose = tour.getPurpose();

        int p = purpose.ordinal();

        lm = tourStopModel[p];
        lm.setDebug(trace);
        if (trace) {
            logger.info("Tracing tour stop choice.");
        }

        Pattern pattern = person.weekdayPattern;

        for (TourStopAlternative alt : TourStopAlternative.values()) {
            int i = alt.ordinal();

            double utility = parameters[p][CNONE + i];

            if (trace) {
                logger.info("**");
                logger.info("Alternative "+alt.name());
                logger.info("constant term " + utility);
            }

            if (purpose == WORK_BASED) {
                double term = parameters[p][CNONE_WBASED + i];
                utility += term;
                if (trace) {
                    logger.info("term WBASED = " + term);
                }
            }

            if (tour.isFirstWork(person)) {
                double term = parameters[p][CNONE_1STWORKTOUR + i];
                utility += term;
                if (trace) {
                    logger.info("term 1STWORKTOUR = " + term);
                }
            }

            if (person.getTourCount() > 3) {
                double term = parameters[p][CNONE_FOURPLUSTOURS + i];
                utility += term;
                if (trace) {
                    logger.info("term FOURPLUSTOURS = " + term);
                }
            }

            // first school tour
            if (purpose == GRADESCHOOL || purpose == COLLEGE) {
                for (Tour tourX : person.weekdayTours) {
                    ActivityPurpose purposeX = tour.primaryDestination.activityPurpose;

                    if (tour == tourX) {
                        if (purpose == purposeX) {
                            double term = parameters[p][CNONE_1STSCHOOLTOUR + i];
                            utility += term;
                            if (trace) {
                                logger.info("term 1STSCHOOLTOUR = " + term);
                            }
                        }
                        break;
                    }

                    if (purposeX == purpose) {
                        break;
                    }
                }
            }

            if (pattern.shopTours > 1) {
                double term = parameters[p][CNONE_PRESENCESHOPTOURS + i];
                utility += term;
                if (trace) {
                    logger.info("term PRESENCESHOPTOURS = " + term);
                }
            }

            if (purpose == SHOP && pattern.shopTours > 2) {
                double term = parameters[p][CNONE_PRESENCEADDTLSHOPTRS + i];
                utility += term;
                if (trace) {
                    logger.info("term PRESENCEADDTLSHOPTRS = " + term);
                }
            }

            if (pattern.otherTours > 1) {
                double term = parameters[p][CNONE_PRESENCEOTHERTOURS + i];
                utility += term;
                if (trace) {
                    logger.info("term PRESENCEOTHERTOURS = " + term);
                }
            }

            if (purpose == OTHER && pattern.otherTours > 2) {
                double term = parameters[p][CNONE_PRESENCEADDTLOTHERTRS + i];
                utility += term;
                if (trace) {
                    logger.info("term PRESENCEADDTLOTHERTRS = " + term);
                }
            }

            if (pattern.workActivities > 1 || pattern.schoolActivities > 1) {
                double term = parameters[p][CNONE_PRESENCEWRKORSCH + i];
                utility += term;
                if (trace) {
                    logger.info("term PRESENCEWRKORSCH = " + term);
                }
            }

            if (household.income < 20000) {
                double term = parameters[p][CNONE_LOWINCOME + i];
                utility += term;
                if (trace) {
                    logger.info("term LOWINCOME = " + term);
                }
            }

            if (household.autos == 0) {
                double term = parameters[p][CNONE_ZEROCARHHLD + i];
                utility += term;
                if (trace) {
                    logger.info("term ZEROCARHHLD = " + term);
                }
            }

            if (person.student && person.age < 18 && person.age > 5) {
                double term = parameters[p][CNONE_GHSPTYPE + i];
                utility += term;
                if (trace) {
                    logger.info("term GHSPTYPE = " + term);
                }
            }

            if (person.student && person.age < 6) {
                double term = parameters[p][CNONE_PSPTYPE + i];
                utility += term;
                if (trace) {
                    logger.info("term PSPTYPE = " + term);
                }
            }

            if (person.student && person.age >= 18) {
                double term = parameters[p][CNONE_COLPTYPE + i];
                utility += term;
                if (trace) {
                    logger.info("term COLPTYPE = " + term);
                }
            }

            if (person.employed && !(person.student) && person.age >=18) {
                double term = parameters[p][CNONE_WRKPTYPE + i];
                utility += term;
                if (trace) {
                    logger.info("term CNONE_WRKPTYPE = " + term);
                }
            }

            for (int j = 0; j < hh_types; ++j) {
                double term = parameters[p][CNONE_HTYPE1 + (4 * j) + i]
                        * hh_type[j];
                utility += term;
                if (trace && term>0) {
                    logger.info("term household type " + j + " = " + term);
                }
            }

            lm.getAlternative(i).setUtility(utility);
        }


        if(trace)
            lm.writeUtilityHeader();

        double compUtility = lm.getUtility();

        if (tracer.isTracePerson(person.hhID + "_" + person.memberID)) {

            logger.info("Composite utility: " + compUtility);
        }

        endTiming();
        return compUtility;
    }

    /**
     * Choose the stop types.
     * @param random Random number generator
     * @return int stop type
     */
    public int chooseStopType(Random random) {
        startTiming();

        if(trace)
            lm.writeProbabilityHeader();

        lm.calculateProbabilities();
        int type;

        try {
            type = (Integer) ((ConcreteAlternative) lm
                    .chooseAlternative(random)).getAlternative();
        } catch (Exception e) {
            logger.error("Caught exception while choosing a stop duration.");
            throw new RuntimeException(e);
        }

        // modify the tour based on the choice
        if (type == TourStopAlternative.OUTBOUND.ordinal()
                || type == TourStopAlternative.BOTH.ordinal()) {
            tour.intermediateStop1 = new Activity();
        }

        if (type == TourStopAlternative.INBOUND.ordinal()
                || type == TourStopAlternative.BOTH.ordinal()) {
            tour.intermediateStop2 = new Activity();
        }
        
        if(trace){
            TourStopAlternative[] alts = TourStopAlternative.values();
            logger.info("Chose stop type "+alts[type]);
        }
        endTiming();
        return type;
    }

    /**
     * @param args Runtime args
     */
    public static void main(String[] args) {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        TourStopChoiceModel tourStopChoiceModel = new TourStopChoiceModel(rb);
        tourStopChoiceModel.buildModel();
    }

}
