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
 *   Created on Feb 22, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import static com.pb.models.pt.PersonType.*;
import static com.pb.models.pt.TourSchedulingParameters.*;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * Tour Scheduling Model
 * 
 * The tour scheduling model
 * 
 * @author Stryker
 */
public class TourSchedulingModel extends TimedModel {
    static Logger logger = Logger.getLogger(TourSchedulingModel.class);

    protected LogitModel schedulingModel;

    private float[][] parameters;

    private int periods;

    private Scheduler scheduler;

    private Tour tour;

    protected ResourceBundle rb;

    private Tracer tracer = Tracer.getTracer();

    private boolean trace = false;

    private int FIRST_ALLOWED_HOUR;

    private PatternModelPersonAttributes attributes = new PatternModelPersonAttributes();

    private PTOccupationReferencer occRef;

    /**
     * Constructor.
     * @param rb Resource Bundle
     * @param occRef occupation referencer
     */
    public TourSchedulingModel(ResourceBundle rb, PTOccupationReferencer occRef) {
        startTiming();
        this.rb = rb;
        FIRST_ALLOWED_HOUR = ResourceUtil.getIntegerProperty(rb, "sdt.start.hour");
        int end_hour = ResourceUtil.getIntegerProperty(rb, "sdt.end.hour");
        periods = end_hour - FIRST_ALLOWED_HOUR + 1;
        readParameters();
        endTiming();
        this.occRef = occRef;
    }

    /**
     * Read parameters from the PT properities file.
     */
    public void readParameters() {
        startTiming();
        String fileName = ResourceUtil.getProperty(rb,
                "sdt.tour.duration.parameters");

        logger.info("Reading tour scheduling parameters file " + fileName);

        try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fileName));
            parameters = table.getValues();
        } catch (IOException e) {
            logger.fatal("Can not find tour duration parameters file "
                    + fileName);
            logger.fatal(e);
            throw new RuntimeException(e);
        }

        logger.info("Parameters is " + parameters.length + " X "
                + parameters[0].length);

        if (!tracer.isTraceOn()) {
            endTiming();
            return;
        }

        logger.info("Read parameter values (across by purpose)");
        for (int j = 0; j < parameters[0].length; ++j) {
            String s = "parameter " + j;
            for (float[] parameter : parameters) {
                s += "," + parameter[j];
            }
            logger.info(s);
        }

        endTiming();
    }

    /**
     * Manually set the parameter array.
     * 
     * This method is intended to factilitate testing.
     * @param parameters array of parameters
     * @param periods periods
     */
    public void setParameters(float[][] parameters, int periods) {
        this.parameters = parameters;
    }

    /**
     * Build the model.
     * 
     * Since the alternatives are simple and pretty much describe themselves,
     * they are instances of a ConcreteAlternative class.
     */
    public void buildModel() {
        startTiming();
        schedulingModel = new LogitModel("Tour scheduling model");

        int alt = -1;
        for (int i = 0; i < periods; ++i) {
            for (int j = i; j < periods; ++j) {
                alt += 1;
                schedulingModel.addAlternative(new ConcreteAlternative(alt
                        + ": " + i + " -> " + j, alt));
                if (tracer.isTraceOn()) {
                    logger.debug("Added alternative: " + i + "-> " + j);
                }
            }
        }
        endTiming();
    }

    /**
     * Calculate the utilities for each alternative.
     * @param household PTHousehold
     * @param person PTPerson
     * @param tour Tour object
     * @param skims SkimsInMemory object
     * @return double utility
     */
    public double calculateUtilities(PTHousehold household, PTPerson person,
            Tour tour, SkimsInMemory skims) {
        startTiming();

//        int hhId = household.ID;
        trace = tracer.isTracePerson(person.hhID + "_" + person.memberID);
        schedulingModel.setDebug(trace);
        scheduler = person.getScheduler();
        this.tour = tour;
        ActivityPurpose purpose = tour.primaryDestination.activityPurpose;
        
        if (trace) {
            logger.info("Scheduling tour " + tour.getOrder() + " with priority "
                    + tour.getPriority());
            logger.info("Tour purpose: " + tour.getPurpose());
            for (int i = 0; i < scheduler.getEventCount(); ++i) {
                logger.info("Event " + i + ": " + scheduler.getEventStart(i)
                        + " -> " + scheduler.getEventEnd(i));
            }
        }

        float[] param = parameters[purpose.ordinal()];
        Pattern pattern = person.weekdayPattern;

        attributes.calculateHouseholdType(household);
        float[] hh_type = new float[PatternModelPersonAttributes.hhTypes];

        hh_type[0] =  attributes.hType1;
        hh_type[1] =  attributes.hType2;
        hh_type[2] =  attributes.hType3;
        hh_type[3] =  attributes.hType4;
        hh_type[4] =  attributes.hType5;
        hh_type[5] =  attributes.hType6;
        hh_type[6] =  attributes.hType7;
        hh_type[7] =  attributes.hType8;
        hh_type[8] =  attributes.hType9;
        hh_type[9] =  attributes.hType10;
        hh_type[10] = attributes.hType11;
        hh_type[11] = attributes.hType12;
        hh_type[12] = attributes.hType13;
        hh_type[13] = attributes.hType14;
        hh_type[14] = attributes.hType15;
        
        for(int i=0;i<hh_type.length;++i)
            if(trace && hh_type[i]!=0)
                logger.info("Person has household type "+i);

        // pririoty of the tour we are scheduling
        int priority = tour.getPriority();
        int order = tour.getOrder();

        // Here's how this needs to work. If there was a tour with a lower
        // order number and a lower priority number, schedule this tour
        // right after that tour. Else, schedule at 0, 0.
        if (trace) {
            logger.info("Scheduling tour " + order + " with priority "
                    + tour.getPriority());
        }
        
        //check if trying to schedule a tour before a college or school tour
        boolean returnBy7PM=false;
        if((order+1)<person.weekdayTours.length){
            for(int i=(order+1);i<person.weekdayTours.length;++i){
                ActivityPurpose thisPurpose = person.weekdayTours[i].primaryDestination.activityPurpose;
                if(thisPurpose == ActivityPurpose.COLLEGE||
                        thisPurpose==ActivityPurpose.GRADESCHOOL)
                    returnBy7PM=true;
            }
        }
        
        boolean match = false;
        for (int i = order - 1; i >= 0; --i) {
            int previous = person.weekdayTours[i].getPriority();
            if (previous < priority) {
                if (trace)
                    logger.info("Previous tour " + i + " priority "
                                    + previous
                                    + " is less than current tour priority "
                                    + priority);
                int end = scheduler.getEventEnd(previous);
                if(trace) logger.info("Previous tour ended at "+end);
                scheduler.scheduleEvent(end, end);
                match = true;
                break;
            }
        }

        if (!match) {
            if(trace)
                logger.info("Did not find previous tour with lower priority");
            scheduler.scheduleEvent(0, 0);

        }

            scheduler.setEventWindow(priority);
        if (trace) {
            logger.info("Tracing tour scheduling utility calculation.");
            logger.info("Tour constraint window: "
                    + scheduler.getFirstWindowPeriod() + " -> "
                    + scheduler.getLastWindowPeriod());
        }

        // set availability and calculate utilities
        int alt = -1;
        for (int i = 0; i < periods; ++i) {
            for (int j = i; j < periods; ++j) {
                alt += 1;

                if (trace) {
                    logger.info(" ** ");
                    logger.info("Period "+i+"->"+j+", alt:"+alt);
                    logger.info(" ** ");
                }
                if (scheduler.isWindowAvailable(priority, i, j)) {
                    schedulingModel.getAlternative(alt).setAvailability(true);
                } else {
                    schedulingModel.getAlternative(alt).setAvailability(false);
                    schedulingModel.getAlternative(alt).setUtility(-999);
                    if(trace) logger.info("Period Unavailable");
                    continue;
                }

                float start = i + FIRST_ALLOWED_HOUR;
                // int end = j + FIRST_ALLOWED_HOUR;
                float duration = j - i;

                // ASC & duration
                float p = param[CDEPART5 + i];
                if(trace) logger.info("departure constant: " + p);
                
                double utility = p;

 
                p = param[CDURAT0 + (int)duration];
                utility += p;

                if (trace) {
                    logger.info(" duration constant: " + p);
                }

                
                //code to check if another school or college tour
                if(returnBy7PM && (j+ FIRST_ALLOWED_HOUR)>19){
                    schedulingModel.getAlternative(alt).setAvailability(false);
                    schedulingModel.getAlternative(alt).setUtility(-999);
                    if(trace) logger.info("Period Unavailable (School/College constraint)");
                    continue;
                }
                 
                
                // All the parameters apply to departure and duration. The
                // parameter index for duration is always one greater than the
                // parameter index for the departure. Hence we have a loop here
                // instead of writing all the utility expressions twice.
                for (int k = 0; k < 2; ++k) {

                    // additional school tours
                    int m= k*DEPARTURE_PARAMETERS;
                    p = param[CSCH2PDEP + m];
                    if (p != 0 && pattern.nSchoolTours > 1) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("additional school tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("additional school tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // additional work tours
                    p = param[CWRK2PDEP + m];
                    if (p != 0 && pattern.nShopTours > 1) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("additional work tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("additional work tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // additional shop tours
                    p = param[CSHP2PDEP + m];
                    if (p != 0 && pattern.nShopTours > 1) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("additional shop tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("additional shop tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // additional recreation tours
                    p = param[CREC2PDEP + m];
                    if (p != 0 && pattern.nRecreateTours > 1) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("additional recreation tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("additional shop tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // presence of school tours
                    p = param[CSCHTRSDEP + m];
                    if (p != 0 && pattern.nSchoolTours > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("additional school tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                        }
                        utility += term;
                    }

                    // presence of work tours
                    p = param[CWRKTRDEP + m];
                    if (p != 0 && pattern.nWorkTours > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("presence of work tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("presence of work tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // presence of work based tours
                    p = param[CWRKBDEP + m];
                    if (p != 0 && pattern.nWorkBasedTours > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("presence of work based tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("presence of work base tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // presence of shop tours
                    p = param[CSHPTRDEP + m];
                    if (p != 0 && pattern.nShopTours > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("presence of shop tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("presence of shop tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // presence of recreation tours
                    p = param[CRECTRDEP + m];
                    if (p != 0 && pattern.nRecreateTours > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("presence of recreation tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("presence of recreation tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // presence of other tours
                    p = param[COTHTRDDEP + m];
                    if (p != 0 && pattern.nRecreateTours > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("presence of oter tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("presence of other tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // inbound stop only on tour
                    p = param[CSTPSIDEP + m];
                    if (p != 0 && tour.hasInboundStop()
                            && !tour.hasOutboundStop()) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("only inbound stop"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("only inbound stop"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // outbound stop only on tour
                    p = param[CSTPSODEP + m];
                    if (p != 0 && !tour.hasInboundStop()
                            && tour.hasOutboundStop()) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("only outbond stop"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("only outbound stop"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // outbound and inbound stops on tour
                    p = param[CSTPSBDEP + m];
                    if (p != 0 && tour.hasInboundStop()
                            && tour.hasOutboundStop()) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("outbound and inbound stops"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("outbound and inbound stops"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // outbound stop only on tour
                    p = param[CSTPSODEP + m];
                    if (p != 0 && !tour.hasInboundStop()
                            && tour.hasOutboundStop()) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("only outbond stop"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("only outbound stop"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // first of two tours
                    p = param[CFOF2DEP + m];
                    if (p != 0 && tour.getOrder() == 0
                            && pattern.getHomeBasedTourCount() == 2) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("first of two tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("first of two tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // second of two tours
                    p = param[CSOF2DEP + m];
                    if (p != 0 && tour.getOrder() == 1
                            && pattern.getHomeBasedTourCount() == 2) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("second of two tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("second of two tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // first of three tours
                    p = param[CFOF3DEP + m];
                    if (p != 0 && tour.getOrder() == 0
                            && pattern.getHomeBasedTourCount() == 3) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("first of three tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("first of three tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // second of three tours
                    p = param[CSOF3DEP + m];
                    if (p != 0 && tour.getOrder() == 1
                            && pattern.getHomeBasedTourCount() == 3) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("second of three tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("second of three tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // third of three tours
                    p = param[CTOF3DEP + m];
                    if (p != 0 && tour.getOrder() == 2
                            && pattern.getHomeBasedTourCount() == 3) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("third of three tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("third of three tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // first of four tours
                    p = param[CFOF4DEP + m];
                    if (p != 0 && tour.getOrder() == 0
                            && pattern.getHomeBasedTourCount() == 4) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("first of four tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("first of four tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // second of four tours
                    p = param[CSOF4DEP + m];
                    if (p != 0 && tour.getOrder() == 1
                            && pattern.getHomeBasedTourCount() == 4) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("second of four tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("second of four tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // third of four tours
                    p = param[CTOF4DEP + m];
                    if (p != 0 && tour.getOrder() == 2
                            && pattern.getHomeBasedTourCount() == 4) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("third of four tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("third of four tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // fourth of four tours
                    p = param[CVOF4DEP + m];
                    if (p != 0 && tour.getOrder() == 3
                            && pattern.getHomeBasedTourCount() == 4) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("fourth of four tours"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("fourth of four tours"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // pre-schooler
                    p = param[CPSPTDEP + m];
                    if (p != 0 && person.personType == PRESCHOOL) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("pre-schooler"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("pre-schooler"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // grade / high school student
                    p = param[CGHSSTDDEP + m];
                    if (p != 0 && person.personType == STUDENTK12) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("grade / high school student"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("grade / high school studen"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // worker
                    p = param[CWRKADDEP + m];
                    if (p != 0 && person.personType == WORKER) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("worker "
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("worker"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // college student
                    p = param[CCOLSTDDEP + m];
                    if (p != 0 && person.personType == STUDENTCOLLEGE) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("college student"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("college student"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // 18 <= age < 25
                    p = param[CAGE25DEP + m];
                    if (p != 0 && person.getAge() >= 18 && person.getAge() < 25) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("18 <= age < 25"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("18 <= age < 25"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // 25 <= age < 35
                    p = param[CAGE21DEP + m];
                    if (p != 0 && person.getAge() >= 25 && person.getAge() < 35) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("25 <= age < 35"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("25 <= age < 35"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;                    }

                    // 35 <= age < 55
                    p = param[CAGE22DEP + m];
                    if (p != 0 && person.getAge() >= 35 && person.getAge() < 55) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("35 >= age < 55"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("35 >= age < 55"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // age >= 55
                    p = param[CAGE55DEP + m];
                    if (p != 0 && person.getAge() >= 55) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("age >= 55"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("age >= 55"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // non-worker, 18 <= age < 25
                    p = param[CNW25DEP + m];
                    if (p != 0 && person.personType == NONWORKER
                            && person.getAge() >= 18 && person.getAge() < 25) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("non-worker, 18 <= age < 25"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("non-worker, 18 <= age < 25"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // non-worker, age >= 55
                    p = param[CNW55DEP + m];
                    if (p != 0 && person.personType == NONWORKER
                            && person.getAge() >= 55) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("non-worker, age >= 55"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("non-worker,age >= 55"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // worker, 25 <= age < 55
                    p = param[CWRK25DEP + m];
                    if (p != 0 && person.personType == WORKER
                            && person.getAge() >= 25 && person.getAge() < 55) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("worker, 25 <= age < 35"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("worker, 25 <= age < 35"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // worker, age >= 55
                    p = param[CWRK55DEP + m];
                    if (p != 0 && person.personType == WORKER
                            && person.getAge() >= 55) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("worker, age >= 55"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("worker, age >= 55"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // retail occupation or industry
                    p = param[CRETLDEP + m];
                    if (p != 0 && (person.employed
                            && (person.getOccupation() == occRef.getRetailOccupation() || person
                                    .getIndustry() == 14 || person.getIndustry() == 13))) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("retail occupation or industry"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("retail occupation or industry"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // Public administration or industry (not retail)
                    p = param[CGOVDEP + m];
                    if (p != 0 && (person.employed &&
                               (person.getOccupation() != occRef.getRetailOccupation() && person
                                    .getIndustry() == 23))) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("public administration or industry"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("public administration or industry"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // household
                    p = param[CHIINCDEP + m];
                    int segment = IncomeSegmenter.calcLogsumSegment(household.income, household.autos, household.workers); 
                    if (p != 0 && segment > 5) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("high income"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("high income"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // zero car household
                    p = param[CAUTO0DEP + m];
                    if (p != 0 && household.autos == 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("zero car household"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("zero car household"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // household classes
                    // TODO: define CHHCLASS0DEP
                    for (int h = 0; h < hh_type.length; ++h) {
                        p = param[CHHCLASS2DEP + (2 * h) + m];
                        double term;
                        if (k == 0) {
                            term = p * hh_type[h] * start;
                            if (trace) {
                                logger.info("household type " + h
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * hh_type[h] * duration;
                            if (trace) {
                                logger.info("household type " + h
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // two or more adults
                    p = param[CADLT2DEP + m];
                    if (p != 0 && household.getAdultCount() > 1) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("two or more adults"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("two or more adults"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // non-working adult in household
                    p = param[CNWADLTDEP + m];
                    if (p != 0 && household.getNonWorkingAdultCount() > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("non-working adult in household"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("non-working adult in household"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // children 5 or younger
                    p = param[CCH5DEP + m];
                    if (p != 0 && household.getCohortCount(0, 5) > 0) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("children 5 or younger in household"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("children 5 or younger in household"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // children aged 6 - 15
                    p = param[CCH615DEP + m];
                    if (p != 0 && household.getCohortCount(6, 15) > 0) {
                        if (k == 0) {
                            utility += p * start;
                        } else {
                            utility += p * duration;
                        }
                    }

                    // high income and preschooler
                    p = param[CHIPSDEP + m];
                    if (p != 0 && person.personType == PRESCHOOL
                            && IncomeSegmenter.getIncomeCategory(household.getIncome()) == IncomeSegmenter.IncomeCategory.INCOME_HIGH) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("high income and pre-schooler"
                                        + " departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("high income and pre-schooler"
                                        + " duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // high income and grade/high school person
                    p = param[CHIGHSDEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && IncomeSegmenter.getIncomeCategory(household.getIncome()) == IncomeSegmenter.IncomeCategory.INCOME_HIGH) {
                        double term;
                        if (k == 0) {
                            term = p * start;
                            if (trace) {
                                logger.info("high income and grade/high "
                                        + "school person departure: " + term);
                            }
                        } else {
                            term = p * duration;
                            if (trace) {
                                logger.info("high income and grade/high " +
                                        "school person duration: " + term);
                            }
                        }
                        utility += term;
                    }

                    // high income and college person
                    p = param[CHICOLDEP + m];
                    if (p != 0 && person.personType == STUDENTCOLLEGE
                            && IncomeSegmenter.getIncomeCategory(household.getIncome()) == IncomeSegmenter.IncomeCategory.INCOME_HIGH) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("high income college person" + type
                                    + term);
                        }
                    }

                    // Zero car household and college person
                    p = param[CA0COLDEP + m];
                    if (p != 0 && person.personType == STUDENTCOLLEGE
                            && IncomeSegmenter.getIncomeCategory(household.getIncome()) == IncomeSegmenter.IncomeCategory.INCOME_HIGH) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("zero car household and collge person"
                                    + type + term);
                        }
                    }

                    // insuffient auto/worker and preschooler
                    p = param[CAIPSDEP + m];
                    if (p != 0
                            && (household.getAutoCount() > household
                                    .getWorkerCount())
                            && person.personType == PRESCHOOL) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("insuffient auto/worker and "
                                    + "pre-schooler" + type + term);
                        }
                    }

                    // insuffient auto/worker and grade/high school person
                    p = param[CAIGHSDEP + m];
                    if (p != 0
                            && (household.getAutoCount() > household
                                    .getWorkerCount())
                            && person.personType == STUDENTK12) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("insuffient auto/worker and grade/high"
                                    + " school person" + type + term);
                        }
                    }

                    // preschooler in household class 6
                    p = param[CPSHH6DEP + m];
                    if (p != 0 && person.personType == PRESCHOOL
                            && ((int) attributes.hType6) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("pre-schooler in household class 6" + type
                                    + term);
                        }
                    }

                    // preschooler in household class 9
                    p = param[CPSHH9DEP + m];
                    if (p != 0 && person.personType == PRESCHOOL
                            && ((int) attributes.hType9) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("pre-schooler in household class 9"
                                    + type + term);
                        }
                    }

                    // grade/high schooler in household class 7
                    p = param[CGHSHH7DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType7) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 7" + type + term);
                        }
                    }

                    // grade/high schooler in household class 8
                    p = param[CGHSHH8DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType8) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 8" + type + term);
                        }
                    }

                    // grade/high schooler in household class 9
                    p = param[CGHSHH9DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType9) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 9" + type + term);
                        }
                    }

                    // grade/high schooler in household class 10
                    p = param[CGHSHH10DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType10) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 10" + type + term);
                        }
                    }

                    // grade/high schooler in household class 11
                    p = param[CGHSHH11DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType11) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 11" + type + term);
                        }
                    }

                    // grade/high schooler in household class 12
                    p = param[CGHSHH12DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType12) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 12" + type + term);
                        }
                    }

                    // grade/high schooler in household class 13
                    p = param[CGHSHH13DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType13) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 13" + type + term);
                        }
                    }

                    // grade/high schooler in household class 14
                    p = param[CGHSHH14DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType14) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 14" + type + term);
                        }
                    }

                    // grade/high schooler in household class 15
                    p = param[CGHSHH15DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType15) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("grade/high schooler in household "
                                    + "class 15" + type + term);
                        }
                    }

                    // college schooler in household class 4
                    p = param[CCOLHH4DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType4) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 4" + type + term);
                        }
                    }

                    // college schooler in household class 5
                    p = param[CCOLHH5DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType5) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 5" + type + term);
                        }
                    }

                    // college schooler in household class 6
                    p = param[CCOLHH6DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType6) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 6" + type + term);
                        }
                    }

                    // college schooler in household class 7
                    p = param[CCOLHH7DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType7) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 7" + type + term);
                        }
                    }

                    // college schooler in household class 8
                    p = param[CCOLHH8DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType8) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 8" + type + term);
                        }
                    }

                    // college schooler in household class 9
                    p = param[CCOLHH9DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType9) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 9" + type + term);
                        }
                    }

                    // college schooler in household class 10
                    p = param[CCOLHH10DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType10) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 10" + type + term);
                        }
                    }

                    // college schooler in household class 11
                    p = param[CCOLHH11DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType11) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 11" + type + term);
                        }
                    }

                    // college schooler in household class 12
                    p = param[CCOLHH12DEP + m];
                    if (p != 0 && person.personType == STUDENTK12
                            && ((int) attributes.hType12) != 0) {
                        double term = getShiftUtility(p, k, start, duration);
                        utility += term;

                        if (trace) {
                            String type = k == 0 ? " departure:" : " duration:";
                            logger.info("college student in household "
                                    + "class 12" + type + term);
                        }
                    }

                    // distance between home and primary destination
                    p = param[CXYDSTDEP + m];
                    if (p != 0) {
                        double dist = skims.getDistance(
                                tour.primaryDestination.endTime,
                                household.homeTaz,
                                tour.primaryDestination.location.zoneNumber);
                        double term = p * dist;
                        String type = k == 0 ? " departure:" : " duration:";
                        
                        if (trace) {
                            logger.info("distance between home and primary " +
                                    "destination" + type + term);
                        }
                    }
                }

                if (trace) {
                    logger.info("Total utility: " + utility);
                }
                schedulingModel.getAlternative(alt).setUtility(utility);

            }
        }

        if (trace) {
            schedulingModel.writeUtilityHeader();
        }

        double compUtility = schedulingModel.getUtility();

        if (tracer.isTracePerson(person.hhID + "_" + person.memberID)) {
            logger.info("Composite utility: " + compUtility);
        }

        endTiming();
        return compUtility;
    }

    /**
     * Choose schedules for all tours.
     * 
     * @param person
     *            making the tours
     * @param household PTHousehold
     * @param skims SkimsInMemory object
     * @param random random number generator
     */
    public void chooseAllSchedules(PTHousehold household, PTPerson person,
            SkimsInMemory skims, Random random) {
        startTiming();
        person.initScheduler(periods);

       
        for (int i = 0; i < person.getTourCount(); ++i) {
            calculateUtilities(household, person, person.getTourByPriority(i),
                    skims);
            SeededRandom.setSeed(person.hhID + person.memberID);
            chooseSchedule(random);
        }
        
        // assign end time to the last activity on the last tour of the day
        person.weekdayTours[person.getTourCount() - 1].end.endTime = 2300;
        endTiming();
    }

    /**
     * Choose a schedule.
     * 
     * Using the utilities computed with the calculate utilities method, choose
     * a schedule via Monte Carlo simulation. Previous tours must have have been
     * previously scheduled for this method to work correctly.
     * @param random random number generator
     * @return the alternative number of the choosen alternative
     */
    public int chooseSchedule(Random random) {
        startTiming();

        if (trace) {
            schedulingModel.writeProbabilityHeader();
        }

        schedulingModel.calculateProbabilities();
        ConcreteAlternative alternative = (ConcreteAlternative) schedulingModel
                .chooseAlternative(random);
        int chosen = (Integer) alternative.getAlternative();

        int start = scheduler.getAlternativeStart(chosen);
        int end = scheduler.getAlternativeEnd(chosen);

        tour.begin.endTime = (short) ((start+ FIRST_ALLOWED_HOUR) * 100);
        tour.end.startTime = (short) ((end+ FIRST_ALLOWED_HOUR) * 100);
        tour.calculateDurationHourly();
        
        if (trace) {
            logger.info("Tour " + tour.getOrder() + " chose time period: " + chosen + " ("
                    + start + " -> " + end + ")");
        }

        scheduler.rescheduleEvent(start, end);

        endTiming();
        return chosen;
    }

    /**
     * Get utility for the "shift".
     * 
     * Add the utility for a departure or duration.
     * 
     * @param p
     *            parameter value
     * @param k
     *            depurture (0) or duration (1)
     * @param departure
     *            Alternative departure
     * @param duration
     *            Alternative duration
     * @return Utility component.
     */
    private double getShiftUtility(double p, int k, float departure, float duration) {

        if (k == 0) {
            return p * ((double)departure);
        }

        return p * ((double)duration);
    }


    
}
