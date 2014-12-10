/*
 * Copyright  2006 PB Consult Inc.
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
package com.pb.models.pt.ldt;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.IncomeSegmenter;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import static com.pb.models.pt.ldt.LDBinaryChoiceParameters.*;
import org.apache.log4j.Logger;

import java.util.Random;
import java.util.ResourceBundle;

/**
 * A binary logit choice model to determine if a long-distance tour is 
 * made by a household during a two-week period.  This model is applied
 * separately for each purpose:
 *   HOUSEHOLD   - travel in which the entire household participates.
 *   WORKRELATED - individual travel for workers.
 *   OTHER       - individual non-work travel. 
 *   
 * @author Freedman
 * @author Erhardt
 * @version 1.0 03/09/2006
 *
 */
public class LDBinaryChoiceModel {

    protected static Logger logger = Logger.getLogger(LDBinaryChoiceModel.class);
    protected ResourceBundle rb;
    private boolean trace = false;

    private LDTourPurpose[] purpose = LDTourPurpose.values();
    private float[][] parameters;
    private double[] utilities;
    private LDBinaryChoicePersonAttributes personAttributes;
    private NormalizedTourDestinationChoiceLogsums logsums;

    private long ldBinaryFixedSeed = Long.MAX_VALUE;

    /**
     * Constructor reads parameters file. Accepts DC logsums.
     */
    public LDBinaryChoiceModel(ResourceBundle rb) {
        this.rb = rb;
        readParameters();

        buildModel();

        String LDBinaryPersonAttribClassName =
                ResourceUtil.getProperty(rb,"ldt.binary.choice.person.attribute.class");
        Class binaryPerAttribClass = null;
        personAttributes = null;
        try {
            binaryPerAttribClass = Class.forName(LDBinaryPersonAttribClassName);
            personAttributes = (LDBinaryChoicePersonAttributes) binaryPerAttribClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.fatal("Can't find Class "+ LDBinaryPersonAttribClassName);
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of Person Attribute Class "+binaryPerAttribClass.getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of TazManager of type "+binaryPerAttribClass.getName());
            throw new RuntimeException(e);
        }
        
        logsums = new NormalizedTourDestinationChoiceLogsums(); 
    }


    /**
     * Read parameters from file specified in properties.
     * 
     */
    private void readParameters() {

        logger.info("Reading LD Tour Binary Choice Parameters");
        parameters = ParameterReader.readParameters(rb, "ldt.binary.choice.parameters");
    }


    /**
     * Initializes utilities to zero.  
     * 
     */
    private void buildModel() {
        utilities = new double[purpose.length];
    }

    /**
     * Calculates the utility of long distance HOUSEHOLD travel.  
     * The decision making agent is a household--the utility and
     * choice should be the same for all household members.  
     * 
     * Call buildModel() first.  
     * 
     * @param hh the PTHousehold that will consider traveling.  
     * @return utility of long distance household travel.  
     */
    private double calculateHouseholdUtility(PTHousehold hh) {

        // use the DC logsums for the other purpose
         double dclogsum = logsums.getNormalizedLogsum(
                ActivityPurpose.getActivityPurpose('o'),
                IncomeSegmenter.calcLogsumSegment(hh.income, hh.autos,hh.workers), hh.homeTaz);

        personAttributes.codeHouseholdAttributes(hh);
        personAttributes.codePersonAttributes(hh);
        utilities[LDTourPurpose.HOUSEHOLD.ordinal()] = calculateUtility(personAttributes,
                parameters[LDTourPurpose.HOUSEHOLD.ordinal()], dclogsum);
        return utilities[LDTourPurpose.HOUSEHOLD.ordinal()];
    }

    /**
     * Calculates the utility of long distance WORKRELATED travel.  
     * The decision making agent is a worker.  If a non-worker is
     * given to this method, it will return a utility of -999.    
     * 
     * Call buildModel() first.  
     * 
     * @param hh the PTHousehold of which the worker is a member.
     * @param p  the PTPerson who is a worker that considers travel.  
     * @return utility of long distance work related travel.  
     */
    private double calculateWorkRelatedUtility(PTHousehold hh, PTPerson p) {

        if(p.employed){
            // use the DC logsums for the other purpose
            double dclogsum = logsums.getNormalizedLogsum(
                    ActivityPurpose.getActivityPurpose('o'),
                    IncomeSegmenter.calcLogsumSegment(hh.income, hh.autos,hh.workers), hh.homeTaz);

            personAttributes.codeHouseholdAttributes(hh);
            personAttributes.codePersonAttributes(p);
            utilities[LDTourPurpose.WORKRELATED.ordinal()] = calculateUtility(personAttributes,
                    parameters[LDTourPurpose.WORKRELATED.ordinal()], dclogsum);
            return utilities[LDTourPurpose.WORKRELATED.ordinal()];
        }
        else {
            return -999;
        }
    }

    /**
     * Calculates the utility of long distance OTHER travel.  
     * The decision making agent is a person.   
     * 
     * Call buildModel() first.  
     * 
     * @param hh the PTHousehold of which the person is a member.
     * @param p  the PTPerson who is a person that considers travel.  
     * @return utility of long distance other travel.  
     */
    private double calculateOtherUtility(PTHousehold hh, PTPerson p) {

        // use the DC logsums for the other purpose
        double dclogsum = logsums.getNormalizedLogsum(
                ActivityPurpose.getActivityPurpose('o'),
                IncomeSegmenter.calcLogsumSegment(hh.income, hh.autos,hh.workers), hh.homeTaz);

        personAttributes.codeHouseholdAttributes(hh);
        personAttributes.codePersonAttributes(p);
        utilities[LDTourPurpose.OTHER.ordinal()] = calculateUtility(personAttributes,
                parameters[LDTourPurpose.OTHER.ordinal()], dclogsum);
        return utilities[LDTourPurpose.OTHER.ordinal()];
    }

    /**
     * Calculate the utility according to the LDTourBinaryChoice coefficients for this purpose
     * and the attributes of the decision-maker (either a household or a person).
     * 
     * @param a		The attributes of the decision-maker.
     * @param params  The appropriate parameters for this purpose.
     * @param dclogsum  Appropriate dc logsum for zones less than 50 miles for resident zone.
     * @return  The utility.
     */
    private double calculateUtility(LDBinaryChoicePersonAttributes a,
                                    float[] params, double dclogsum) {

        double utility=
            params[CONSTANT]
                   + params[WORKER_1        ] * a.worker_1
                   + params[WORKER_2        ] * a.worker_2
                   + params[WORKER_3        ] * a.worker_3
                   + params[AUTO_1          ] * a.auto_1
                   + params[AUTO_2          ] * a.auto_2
                   + params[AUTO_3          ] * a.auto_3
                   + params[SIZE_2          ] * a.size_2
                   + params[SIZE_3          ] * a.size_3
                   + params[SIZE_4          ] * a.size_4
                   + params[INCOME_2        ] * a.income_2
                   + params[INCOME_3        ] * a.income_3
                   + params[INCOME_4        ] * a.income_4
                   + params[SINGLEFAMILY    ] * a.singleFamily
                   + params[STUDENTS_3      ] * a.students_3
                   + params[OCC_AG_FARM_MINE] * a.occ_ag_farm_mine
                   + params[OCC_MANUFACTUR  ] * a.occ_manufactur
                   + params[OCC_TRANS_COMM  ] * a.occ_trans_comm
                   + params[OCC_WHOLESALE   ] * a.occ_wholesale
                   + params[OCC_FINANCE_RE  ] * a.occ_finance_re
                   + params[OCC_OTHER       ] * a.occ_other
                   + params[OCC_PROF_SCI    ] * a.occ_prof_sci
                   + params[SCHTYPE_COLLEGE ] * a.schtype_college
                   + params[MALE            ] * a.male
                   + params[AGE             ] * a.age
                   + params[AGE_SQ          ] * a.age_sq
                   + params[DC_LOGSUMLT50   ] * dclogsum
                   + params[HOUSEHOLD_LDTRIP] * a.household_LDTrip;

        if (trace && logger.isDebugEnabled()) {
            a.print();
            logger.debug("params[CONSTANT]                              = " + params[CONSTANT]                              ); 
            logger.debug("params[WORKER_1        ] * a.worker_1         = " + params[WORKER_1        ] * a.worker_1         ); 
            logger.debug("params[WORKER_2        ] * a.worker_2         = " + params[WORKER_2        ] * a.worker_2         ); 
            logger.debug("params[WORKER_3        ] * a.worker_3         = " + params[WORKER_3        ] * a.worker_3         ); 
            logger.debug("params[AUTO_1          ] * a.auto_1           = " + params[AUTO_1          ] * a.auto_1           ); 
            logger.debug("params[AUTO_2          ] * a.auto_2           = " + params[AUTO_2          ] * a.auto_2           ); 
            logger.debug("params[AUTO_3          ] * a.auto_3           = " + params[AUTO_3          ] * a.auto_3           ); 
            logger.debug("params[SIZE_2          ] * a.size_2           = " + params[SIZE_2          ] * a.size_2           ); 
            logger.debug("params[SIZE_3          ] * a.size_3           = " + params[SIZE_3          ] * a.size_3           ); 
            logger.debug("params[SIZE_4          ] * a.size_4           = " + params[SIZE_4          ] * a.size_4           ); 
            logger.debug("params[INCOME_2        ] * a.income_2         = " + params[INCOME_2        ] * a.income_2         ); 
            logger.debug("params[INCOME_3        ] * a.income_3         = " + params[INCOME_3        ] * a.income_3         ); 
            logger.debug("params[INCOME_4        ] * a.income_4         = " + params[INCOME_4        ] * a.income_4         ); 
            logger.debug("params[SINGLEFAMILY    ] * a.singleFamily     = " + params[SINGLEFAMILY    ] * a.singleFamily     ); 
            logger.debug("params[STUDENTS_3      ] * a.students_3       = " + params[STUDENTS_3      ] * a.students_3       ); 
            logger.debug("params[OCC_AG_FARM_MINE] * a.occ_ag_farm_mine = " + params[OCC_AG_FARM_MINE] * a.occ_ag_farm_mine ); 
            logger.debug("params[OCC_MANUFACTUR  ] * a.occ_manufactur   = " + params[OCC_MANUFACTUR  ] * a.occ_manufactur   ); 
            logger.debug("params[OCC_TRANS_COMM  ] * a.occ_trans_comm   = " + params[OCC_TRANS_COMM  ] * a.occ_trans_comm   ); 
            logger.debug("params[OCC_WHOLESALE   ] * a.occ_wholesale    = " + params[OCC_WHOLESALE   ] * a.occ_wholesale    ); 
            logger.debug("params[OCC_FINANCE_RE  ] * a.occ_finance_re   = " + params[OCC_FINANCE_RE  ] * a.occ_finance_re   ); 
            logger.debug("params[OCC_OTHER       ] * a.occ_other        = " + params[OCC_OTHER       ] * a.occ_other        ); 
            logger.debug("params[OCC_PROF_SCI    ] * a.occ_prof_sci     = " + params[OCC_PROF_SCI    ] * a.occ_prof_sci     ); 
            logger.debug("params[SCHTYPE_COLLEGE ] * a.schtype_college  = " + params[SCHTYPE_COLLEGE ] * a.schtype_college  ); 
            logger.debug("params[MALE            ] * a.male             = " + params[MALE            ] * a.male             ); 
            logger.debug("params[AGE             ] * a.age              = " + params[AGE             ] * a.age              ); 
            logger.debug("params[AGE_SQ          ] * a.age_sq           = " + params[AGE_SQ          ] * a.age_sq           ); 
            logger.debug("params[DC_LOGSUMLT50   ] * dclogsum           = " + params[DC_LOGSUMLT50   ] * dclogsum           ); 
            logger.debug("params[HOUSEHOLD_LDTRIP] * a.household_LDTrip = " + params[HOUSEHOLD_LDTRIP] * a.household_LDTrip ); 
            logger.debug("The destination choice logsum w/i 50 miles is: "+dclogsum);
            logger.debug("The total utility is: "+ utility);
        }

        return utility;
    }


    /**
     * Apply the binary choice logit model, draw a random number and monte-carlo the choice
     * to make a LD tour based on the probability and random number draw.
     * 
     * @param utility  The utility of making an LD tour.
     * @return the chosen alternative
     */
    private boolean simulateBinaryChoice(double utility, double random){
        boolean LDTour = false;

        double probability = 1/(1+Math.exp(-utility));


        if(random<probability)
            LDTour=true;

        return LDTour;
    }


    /**
     * Simulates the choice of long distance HOUSEHOLD travel.  
     * The decision making agent is a household--the 
     * choice should be the same for all household members.    
     *  
     * @param hh the PTHousehold that will consider traveling.  
     * @return boolean indicating if household long-distance travel
     *         occurs during a two-week window.    
     */
    public boolean chooseHouseholdBinary(PTHousehold hh, long hhSeed) {
        calculateHouseholdUtility(hh);

        Random random = new Random();
        random.setSeed(hhSeed + ldBinaryFixedSeed);
        boolean choice = simulateBinaryChoice(utilities[LDTourPurpose.HOUSEHOLD.ordinal()], random.nextDouble());

        if(trace){
            logger.info("    Chosen HOUSEHOLD LD travel for HH " + hh.ID + " is: " + choice);
        }

        return choice;
    }

    /**
     * Simulates the choice of long distance travel for persons.  
     *  
     * @param hh the PTHousehold of which the person is a member.
     * @param p  the PTPerson that considers travel.  
     * @return array boolean indicating if work related long-distance travel
     *         occurs during a two-week window for each purpose.     
     */
    public boolean[] choosePersonBinary(PTHousehold hh, PTPerson p, long hhPersonSeed) {

        boolean[] result = new boolean[LDTourPurpose.values().length];
        Random random = new Random();
        random.setSeed(hhPersonSeed + ldBinaryFixedSeed);

        // same as household travel
        result[LDTourPurpose.HOUSEHOLD.ordinal()] = hh.ldHouseholdTourIndicator;

        // no long distance travel if not a worker
        if (p.employed) {
            calculateWorkRelatedUtility(hh, p);
            result[LDTourPurpose.WORKRELATED.ordinal()] = simulateBinaryChoice(utilities[LDTourPurpose.WORKRELATED.ordinal()], random.nextDouble());
        }
        else {
            result[LDTourPurpose.WORKRELATED.ordinal()] = false;
        }

        // other
        calculateOtherUtility(hh, p);
        result[LDTourPurpose.OTHER.ordinal()] = simulateBinaryChoice(utilities[LDTourPurpose.OTHER.ordinal()], random.nextDouble());

        // trace results
        if(trace){
            for (int i=0; i<result.length; i++) {
                logger.info("    Chosen " + LDTourPurpose.values()[i]
                        + " long-distance travel for household " + hh.ID
                        + " is: " + result[i]);
            }
        }

        return result;
    }


    /**
     * Set the trace option.
     * 
     * The trace option is set to false by default. The verbosity of trace
     * output is constrolled through the info and debug logger levels.
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }


    /**
     * Runs the long-distance binary choice models, and sets the flags on the 
     * household and person objects to indicate whether any long-distance travel
     * occurs during a two-week period.
     *
     * This method will not randomize the seed (i.e. will not add on System.currentTime()) in
     * order to change the seed dynamically.  If you want to have control over this, use the
     * 'runBinaryChoiceModelWithRandomSeedControl' and pass in a boolean indicating whether
     * or not you are in sensitivity testing mode.
     * 
     * @param households An array of households that may be traveling.
     */
    public void runBinaryChoiceModel(PTHousehold[] households) {
        logger.debug("Running LDT binary choice models.");

        for (int i = 0; i < households.length; i++) {

            households[i].ldHouseholdTourIndicator = chooseHouseholdBinary(households[i], households[i].ID);

            long hhPersonSeed;
            for (int j = 0; j < households[i].persons.length; j++) {
                hhPersonSeed = households[i].ID*100 + households[i].persons[j].memberID;
                households[i].persons[j].ldTourIndicator = choosePersonBinary(
                        households[i], households[i].persons[j], hhPersonSeed);
            }
        }
    }

    /**
     * Runs the long-distance binary choice models, and sets the flags on the
     * household and person objects to indicate whether any long-distance travel
     * occurs during a two-week period.
     *
     * This method allows you to have dynamic control over the random number seed.
     *
     * @param households An array of households that may be traveling.
     */
    public void runBinaryChoiceModelWithRandomSeedControl(PTHousehold[] households, boolean sensitivityTestingMode) {
        logger.debug("Running LDT binary choice models.");
        long hhRandomSeed;
        long hhPersonSeed;
        for (int i = 0; i < households.length; i++) {
            if(sensitivityTestingMode)
                hhRandomSeed = households[i].ID + System.currentTimeMillis();
            else
                hhRandomSeed = households[i].ID;

            households[i].ldHouseholdTourIndicator = chooseHouseholdBinary(households[i], hhRandomSeed);

            for (int j = 0; j < households[i].persons.length; j++) {
                if(sensitivityTestingMode)
                    hhPersonSeed = households[i].ID*100 + households[i].persons[j].memberID + System.currentTimeMillis();
                else
                    hhPersonSeed = households[i].ID*100 + households[i].persons[j].memberID;

                households[i].persons[j].ldTourIndicator = choosePersonBinary(
                        households[i], households[i].persons[j], hhPersonSeed);
            }
        }
    }

}
