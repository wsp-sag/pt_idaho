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
 *   Created on Feb 17, 2006 by Joel Freedman <freedman@pbworld.com>
 */
package com.pb.models.pt;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import static com.pb.models.pt.PatternParameters.*;
import static com.pb.models.pt.PatternVariables.*;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.ResourceBundle;

public class PatternChoiceModel extends TimedModel {

    final static Logger logger = Logger
            .getLogger(PatternChoiceModel.class);

    // array dimensioned by personType and number of parameters
    protected float[][] parameters;

    protected float[][] variables;

    protected TableDataSet patterns;

    // There is one logit model for every PersonType
    LogitModel[] patternModel;

    PTHousehold currentHousehold;

    PTPerson currentPerson;

    PatternModelPersonAttributes a;

    ConcreteAlternative chosenPattern;

    private static Tracer tracer = Tracer.getTracer();

    private boolean trace = false;

    ResourceBundle rb;

    /**
     * Default constructor. Reads the model parameters file, pattern file.
     * @param rb ResourceBundle
     */
    public PatternChoiceModel(ResourceBundle rb) {
        this.rb = rb;
        startTiming();

        CSVFileReader reader = new CSVFileReader();

        // read the pattern choice parameters
        String fileName = ResourceUtil.getProperty(rb,
                "sdt.pattern.choice.parameters");
        logger.info("Reading pattern choice parameters in " + fileName);
        try {
            TableDataSet table = reader.readFile(new File(fileName));
            parameters = table.getValues();
            logger.info("  Pattern parameters table has " + parameters.length + " rows and " 
                    + parameters[0].length + " columns"); 
        } catch (IOException e) {
            logger.fatal("Can't find PatternModelParameters file " + fileName);
            throw new RuntimeException(e);
        }
        // read the patterns
        fileName = ResourceUtil.getProperty(rb, "sdt.activity.patterns");
        logger.info("Reading activity patterns in " + fileName);
        try {
            patterns = reader.readFile(new File(fileName));
            logger.info("  Pattern table has " + patterns.getRowCount() + " rows and " 
                    + patterns.getColumnCount() + " columns"); 
        } catch (IOException e) {
            logger.fatal("Can't find ActivityPatterns file " + fileName);
            throw new RuntimeException(e);
        }

        // set pattern variables to the values in the patterns file, but skip
        // pattern
        // string row
        variables = new float[patterns.getRowCount()][patterns.getColumnCount()];

        logger.info("Filling the variables array.");

        for (int row = 1; row <= patterns.getRowCount(); ++row)
            for (int column = 1; column <= patterns.getColumnCount(); ++column) {

                if (column == PatternVariables.DAYPATTERN + 1)
                    continue;

                variables[row - 1][column - 1] = patterns.getValueAt(row,
                        column);

//                if (tracer.isTraceOn()) {
//                    int i = row - 1;
//                    int j = column - 1;
//                    logger.info(" i = " + i + ", j = " + j + ", value = "
//                            + variables[i][j]);
//                }
            }
        a = new PatternModelPersonAttributes();
        endTiming();
    }

    /**
     * This method declares an array of LogitModels (one per person type).
     * ConcreteAlternatives are added to each LogitModel according to the
     * availability settings in the Pattern file. The name of the alternative
     * will be the pattern string, and the alternative number will be the
     * patternNumber.
     * 
     */
    public void buildModel() {
        startTiming();
        
        patternModel = new LogitModel[PersonType.values().length];

        // initialize the patternModel array!
        for (int i = 0; i < patternModel.length; ++i) {
            patternModel[i] = new LogitModel("Person type " + i);
        }

        for (int row = 1; row <= patterns.getRowCount(); ++row) {

            String pattern = patterns.getStringValueAt(row,
                    PatternVariables.DAYPATTERN + 1);
            int patternNumber = (int) patterns.getValueAt(row,
                    PatternVariables.PATTERNNUMBER + 1);

            if (patterns.getValueAt(row, PatternVariables.PTYPE1 + 1) == 1)
                patternModel[0].addAlternative(new ConcreteAlternative(pattern,
                        patternNumber));

            if (patterns.getValueAt(row, PatternVariables.PTYPE2 + 1) == 1)
                patternModel[1].addAlternative(new ConcreteAlternative(pattern,
                        patternNumber));

            if (patterns.getValueAt(row, PatternVariables.PTYPE3 + 1) == 1)
                patternModel[2].addAlternative(new ConcreteAlternative(pattern,
                        patternNumber));

            if (patterns.getValueAt(row, PatternVariables.PTYPE4 + 1) == 1)
                patternModel[3].addAlternative(new ConcreteAlternative(pattern,
                        patternNumber));

            if (patterns.getValueAt(row, PatternVariables.PTYPE5 + 1) == 1)
                patternModel[4].addAlternative(new ConcreteAlternative(pattern,
                        patternNumber));
        }
        endTiming();
    }

    /**
     * Solve the pattern choice logit model for a person/household and return
     * the logsum.
     * 
     * @param household
     *            The household.
     * @param person
     *            The person.
     * @param distance
     *            A distance matrix, used to compute work distance if worker.
     * @return The logsum.
     */
    public double getUtility(PTHousehold household, PTPerson person,
            Matrix distance) {
        startTiming();

        currentHousehold = household;
        currentPerson = person;
        a.calculateAttributes(household, person, distance);
        a.calculateHouseholdType(household);

        int model = a.type.ordinal();

        if (tracer.isTracePerson(person.hhID + "_" + person.memberID)) {
            patternModel[model].setDebug(true);
            patternModel[model].writeUtilityHeader();
            trace = true;
        } else {
            patternModel[model].setDebug(false);
            trace = false;
        }

        // Iterate through the arraylist of alternatives and set the utility for
        // each
        ArrayList alternatives = patternModel[model].getAlternatives();
        for (Object o : alternatives) {
            ConcreteAlternative alt = (ConcreteAlternative) o;
            int patternNumber = (Integer) alt.getAlternativeObject();
            alt.setUtility(calculateUtility(a, patternNumber));
        }
        
        endTiming();
        return patternModel[model].getUtility();
    }

    /**
     * Choose a pattern according to the probabilities in the model. The
     * getUtility() method should be called before this method.
     * @param random Random number
     * @return The chosen pattern.
     */
    public ConcreteAlternative choosePattern(Random random) {
        startTiming();
        int model = a.type.ordinal();

        if (trace) {
            patternModel[model].writeProbabilityHeader();
        }

        patternModel[model].calculateProbabilities();
        try {
            chosenPattern = (ConcreteAlternative) patternModel[model]
                    .chooseElementalAlternative(random);
        } catch (Exception e) {
            logger.error("Error in pattern choice: no patterns available ");
            // have a real problem so write out info
            // info
            logger
                    .error("Error in pattern choice: no patterns available for this household, person");
            logger.error("Household " + currentHousehold.ID + " Person "
                    + currentPerson.memberID);

            // if not already done, write the logit info into a info file. Path
            // is specified in pt.properties
            PrintWriter file2 = PTResults.createTazDebugFile(rb, "PatternInfo.csv");
            if (file2 != null) { // if it is null that means an earlier
                // problem caused this file to be written
                // already and there
                // is no reason to write it out twice
                logger
                        .error("Writing out pattern info because couldn't find a pattern");
                patternModel[model].setDebug(true);
                patternModel[model].getUtility();
                patternModel[model].setDebug(false);
            }
            // We need to do something here so that the program can continue but
            // somehow mark this tour as
            // pathological. Right now I am just letting the chosen Pattern be H
            chosenPattern = new ConcreteAlternative("H", 0);
        }
        endTiming();
        return chosenPattern;
    }

    /**
     * Calculate the utility for a particular pattern, person, household, and
     * return it. If the pattern includes a work activity, and the person is not
     * a worker, this method will return a -999.
     * 
     * @param a The person.
     * @param patternNumber PatternModelPersonAttributes object
     * @return The utility.
     */
    private double calculateUtility(PatternModelPersonAttributes a,
            int patternNumber) {

        int pn = patternNumber - 1;
        
        int t = a.type.ordinal();

        double utility;

        // check if non-worker and pattern includes work
        if (a.worker != 1
                && variables[pn][PatternVariables.PRESENCEWORKACTIVITIES] == 1) {
            return -999;
        }

        utility = (double) parameters[t][CTRS1]
                * variables[pn][ONETOURPATTERN]
                + parameters[t][CTRS2]
                * variables[pn][TWOTOURSPATTERN]
                + parameters[t][CTRS3]
                * variables[pn][THREETOURSPATTERN]
                + parameters[t][CTRS4]
                * variables[pn][FOURTOURSPATTERN]
                + parameters[t][CTRS5]
                * variables[pn][FIVEPLUSTOURSPATTERN]
                + parameters[t][CWRKNSTPS]
                * variables[pn][WORKONLYNOSTOPS]
                + parameters[t][CWRKNSTP2]
                * variables[pn][WORKNOSTOPS]
                + parameters[t][CWRKOSTPS]
                * variables[pn][WORKONLYOUTSTOPS]
                + parameters[t][CWRKISTPS]
                * variables[pn][WORKONLYINSTOPS]
                + parameters[t][CWRKOSTP2]
                * variables[pn][WORKOUTSTOPS]
                + parameters[t][CWRKISTP2]
                * variables[pn][WORKINSTOPS] + parameters[t][CSCH]
                * variables[pn][SCHOOLONLY] + parameters[t][CWRK]
                * variables[pn][WORKONLY] + parameters[t][CSCHWRK]
                * variables[pn][SCHOOLBEFOREWORK]
                + parameters[t][CWRKSCH]
                * variables[pn][WORKBEFORESCHOOL]
//                + parameters[t][CCOMBO]
//                * variables[patternNumber][SCHOOLWITHWORKSTOPS]
                + parameters[t][CWRKPNSTP2]
                * variables[pn][WORK2PNOSTOPS]
                + parameters[t][CWRKPWSTP2]
                * variables[pn][WORK2PWITHSTOPS]
                + parameters[t][CACTCG1D] * variables[pn][SCHOOL2P]
                + parameters[t][CBACT]
                * variables[pn][NUMWBASEDTOURS]
                + parameters[t][CBACT1]
                * variables[pn][PRESENCEWBASEDTOURS]
                + parameters[t][CHSCHH]
                * variables[pn][HOMESCHOOLHOMEPATTERN]
                + parameters[t][CT1OTH] * variables[pn][OTHERONLY]
                + parameters[t][CT2SHP] * variables[pn][SHOPONLY2P]
                + parameters[t][CT2OTH] * variables[pn][OTHERONLY2P]
                + parameters[t][CP0SEC3B]
                * variables[pn][NOPRIMARYTHREEPLUSTOURS]
                + parameters[t][CACTSD]
                * variables[pn][PRESENCESHOPTOURS]
                + parameters[t][CACTRD]
                * variables[pn][PRESENCERECTOURS]
                + parameters[t][CACTOD]
                * variables[pn][PRESENCEOTHERTOURS]
                + parameters[t][CSHPWRK]
                * variables[pn][SHOPBEFOREWORK]
                + parameters[t][CRECWRK]
                * variables[pn][RECBEFOREWORK]
                + parameters[t][COTHWRK]
                * variables[pn][OTHBEFOREWORK]
                + parameters[t][CSHPSCH]
                * variables[pn][SHOPBEFORESCHOOL]
                + parameters[t][CRECSCH]
                * variables[pn][RECBEFORESCHOOL]
                + parameters[t][COTHSCH]
                * variables[pn][OTHBEFORESCHOOL]
                + parameters[t][CRECSHP]
                * variables[pn][RECBEFORESHOP]
                + parameters[t][CSHPACT1]
                * variables[pn][ONESHOPACTIVITY]
                + parameters[t][CSHPACT2]
                * variables[pn][TWOSHOPACTIVITIES]
                + parameters[t][CSHPACT3]
                * variables[pn][THREESHOPACTIVITIES]
                + parameters[t][CSHPACT4]
                * variables[pn][FOURSHOPACTIVITIES]
                + parameters[t][CRECACT1]
                * variables[pn][ONERECACTIVITY]
                + parameters[t][CRECACT2]
                * variables[pn][TWORECACTIVITIES]
                + parameters[t][CRECACT3]
                * variables[pn][THREERECACTIVITIES]
                + parameters[t][CRECACT4]
                * variables[pn][FOURRECACTIVITIES]
                + parameters[t][COTHACT1]
                * variables[pn][ONEOTHERACTIVITY]
                + parameters[t][COTHACT2]
                * variables[pn][TWOOTHERACTIVITIES]
                + parameters[t][COTHACT3]
                * variables[pn][THREEOTHERACTIVITIES]
                + parameters[t][COTHACT4]
                * variables[pn][FOUROTHERACTIVITIES]
                + parameters[t][CWNWINT1]
                * variables[pn][STOPSWORKTIMESNONWORKTOURS]
                + parameters[t][CWNWINT2]
                * variables[pn][STOPSWORKTIMESSTOPSNONWORK]
                + parameters[t][CTSINT1]
                * variables[pn][STOPSTIMESTOURS]
                + parameters[t][CSCOUT]
                * variables[pn][SCHOOLWITHOUTSTOPS]
                + parameters[t][CSCIN]
                * variables[pn][SCHOOLWITHINSTOPS]
                + parameters[t][CSCINOUT]
                * variables[pn][SCHOOLWITHBOTHSTOPS]
                + parameters[t][CWRKOUT]
                * variables[pn][WORKWITHOUTSTOPS]
                + parameters[t][CWRKIN]
                * variables[pn][WORKWITHINSTOPS]
                + parameters[t][CWRKINOUT]
                * variables[pn][WORKWITHBOTHSTOPS]
                + parameters[t][CP1_O]
                * variables[pn][SCHOOLORWORKOUTSTOPS]
                + parameters[t][CP1_I]
                * variables[pn][SCHOOLORWORKINSTOPS]
                + parameters[t][CP1_IO]
                * variables[pn][SCHOOLORWORKBOTHSTOPS]
                + parameters[t][CP2_O1]
                * variables[pn][SCHOOLANDWORKOUTSTOPSFIRST]
                + parameters[t][CP2_I1]
                * variables[pn][SCHOOLANDWORKINSTOPSFIRST]
                + parameters[t][CP2_IO1]
                * variables[pn][SCHOOLANDWORKBOTHSTOPSFIRST]
                + parameters[t][CP2_O2]
                * variables[pn][SCHOOLANDWORKOUTSTOPSSEC]
                + parameters[t][CP2_I2]
                * variables[pn][SCHOOLANDWORKINSTOPSSEC]
                + parameters[t][CP2_IO2]
                * variables[pn][SCHOOLANDWORKBOTHSTOPSSEC]
                + parameters[t][CCOMBO_IO]
                * variables[pn][SCHOOLWITHWORKANDEXTRASTOPS]
                + parameters[t][COUTGTINNS]
                * variables[pn][MOREOUTTHANIN]
                + parameters[t][COUTLTINNS]
                * variables[pn][MOREINTHANOUT]
                + parameters[t][COUTEQINNS]
                * variables[pn][EQUALOUTANDIN]
                + parameters[t][CSTPBD]
                * variables[pn][PRESENCESTOPSONWBASED]
                + parameters[t][CSTOPSSD]
                * variables[pn][PRESENCESTOPSONSHOP]
                + parameters[t][CSTOPSRD]
                * variables[pn][PRESENCESTOPSONREC]
                + parameters[t][CSTOPSO1]
                * variables[pn][ONESTOPONOTHER]
                + parameters[t][CSTOPSO2]
                * variables[pn][TWOSTOPSONOTHER]
                + parameters[t][CSTOPSO3]
                * variables[pn][THREEPLUSSTOPSONOTHER]
                + parameters[t][C2TA0] * variables[pn][TWOPLUSTOURS]
                * a.autos0 + parameters[t][CTWOALTA]
                * variables[pn][TWOPLUSTOURS] * a.autosltadults
                + parameters[t][CSTPNWA0]
                * variables[pn][PRESENCESTOPS] * a.autos0
                + parameters[t][CSTOPALTA]
                * variables[pn][PRESENCESTOPS] * a.autosltadults
                + parameters[t][CSTPWA0]
                * variables[pn][PRESENCESTOPSONWORK] * a.autos0
                + parameters[t][CSTPBA0]
                * variables[pn][PRESENCESTOPSONWBASED] * a.autos0
//                + parameters[t][CSTPWBA0]
//                * variables[patternNumber][PRESENCESTOPSONWORKWBASED] * a.autos0
//                + parameters[t][CSTPWBAI]
//                * variables[patternNumber][PRESENCESTOPSONWORKWBASED]
//                * a.autosltworkers
                + parameters[t][CSTOPNWA0]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.autos0 + parameters[t][CSRPTOURZA]
                * variables[pn][PRESENCESHOPRECOTHTOURS] * a.autos0
                + parameters[t][CSROSTPC]
                * variables[pn][PRESENCESTOPSFORSHOPRECOTH]
                * a.autosltworkers + parameters[t][CSROSTNWA]
                * variables[pn][PRESENCESTOPSFORSHOPRECOTH]
                * a.adultsgtworkers + parameters[t][CSROSTPAD]
                * variables[pn][PRESENCESTOPSFORSHOPRECOTH]
                * a.adultsle1 + parameters[t][CSTPPRIMHH7]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType7
                + parameters[t][CSTPPRIMHH8]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType8
                + parameters[t][CSTPPRIMHH9]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType9
                + parameters[t][CSTPPRIMHH10]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType10
                + parameters[t][CSTPPRIMHH11]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType11
                + parameters[t][CSTPPRIMHH12]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType12
                + parameters[t][CSTPPRIMHH13]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType13
                + parameters[t][CSTPPRIMHH14]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType14
                + parameters[t][CSTPPRIMHH15]
                * variables[pn][PRESENCESTOPSONPRIMARY] * a.hType15
                + parameters[t][CSTPNWHH1]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType1 + parameters[t][CSTPNWHH2]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType2 + parameters[t][CSTPNWHH3]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType3 + parameters[t][CSTPNWHH4]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType4 + parameters[t][CSTPNWHH5]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType5 + parameters[t][CSTPNWHH6]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType6 + parameters[t][CSTPNWHH7]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType7 + parameters[t][CSTPNWHH8]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType8 + parameters[t][CSTPNWHH9]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType9 + parameters[t][CSTPNWHH10]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType10 + parameters[t][CSTPNWHH11]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType11 + parameters[t][CSTPNWHH12]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType12 + parameters[t][CSTPNWHH13]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType13 + parameters[t][CSTPNWHH14]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType14 + parameters[t][CSTPNWHH15]
                * variables[pn][PRESENCESTOPSONSHOPRECOTH]
                * a.hType15 + parameters[t][CSCH_W]
                * variables[pn][SCHOOLONLY] * a.worker
                + parameters[t][CSCHAGE1]
                * variables[pn][PRESENCESCHOOLTOURS] * a.age1
                + parameters[t][CSCHAGE2]
                * variables[pn][PRESENCESCHOOLTOURS] * a.age2
                + parameters[t][CSCHAGE3]
                * variables[pn][PRESENCESCHOOLTOURS] * a.age3
                + parameters[t][CSCHAGE4]
                * variables[pn][PRESENCESCHOOLTOURS] * a.age4
                + parameters[t][CSCHAGE5]
                * variables[pn][PRESENCESCHOOLTOURS] * a.age5
                + parameters[t][CWRKAGE3]
                * variables[pn][PRESENCEWORKACTIVITIES] * a.age15
                + parameters[t][CWRKAGE4]
                * variables[pn][PRESENCEWORKACTIVITIES] * a.age16
                + parameters[t][CWRKAGE5]
                * variables[pn][PRESENCEWORKACTIVITIES] * a.age17
                + parameters[t][CSCHHINC]
                * variables[pn][PRESENCESCHOOLTOURS] * a.incHi
                + parameters[t][CSCHNWA]
                * variables[pn][PRESENCESCHOOLTOURS]
                * a.adultsgtworkers + parameters[t][CSTPSWFM]
                * variables[pn][PRESENCESHOPSTOPSONWORK] * a.female
                + parameters[t][CSHPAFEM]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.female
                + parameters[t][CSHPTT4C]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.age16to18 * a.autosgtadults + parameters[t][CSHPAAGE1]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.agelt25
                + parameters[t][CSHOPAGE2]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.age25to35 + parameters[t][CSHOPAGE3]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.age35to45 + parameters[t][CSHOPAGE4]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.age45to55 + parameters[t][CSHOPAGE5]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.age55to65 + parameters[t][CSHOPAGE6]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.age65plus + parameters[t][CSHOPLINC]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.incLow
                + parameters[t][CSHPAHINC]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.incHi
                + parameters[t][CSHPNWA]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.adultsgtworkers + parameters[t][CSHP1AD]
                * variables[pn][PRESENCESHOPACTIVITIES]
                * a.adultsle1 + parameters[t][CSHPHH1]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType1
                + parameters[t][CSHPHH2]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType2
                + parameters[t][CSHPHH3]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType3
                + parameters[t][CSHPHH4]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType4
                + parameters[t][CSHPHH5]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType5
                + parameters[t][CSHPHH6]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType6
                + parameters[t][CSHPHH7]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType7
                + parameters[t][CSHPHH8]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType8
                + parameters[t][CSHPHH9]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType9
                + parameters[t][CSHPHH10]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType10
                + parameters[t][CSHPHH11]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType11
                + parameters[t][CSHPHH12]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType12
                + parameters[t][CSHPHH13]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType13
                + parameters[t][CSHPHH14]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType14
                + parameters[t][CSHPHH15]
                * variables[pn][PRESENCESHOPACTIVITIES] * a.hType15
                + parameters[t][CRECAGE4]
                * variables[pn][PRESENCERECACTIVITIES] * a.age4
                + parameters[t][CRECAGE5]
                * variables[pn][PRESENCERECACTIVITIES] * a.age5
                + parameters[t][CRECAAGE25]
                * variables[pn][PRESENCERECACTIVITIES] * a.agelt25
                + parameters[t][CRECTOTAG1]
                * variables[pn][PRESENCERECACTIVITIES] * a.age25to35
                + parameters[t][CRECTOTAG2]
                * variables[pn][PRESENCERECACTIVITIES] * a.age35to45
                + parameters[t][CRECTOTAG3]
                * variables[pn][PRESENCERECACTIVITIES] * a.age45to55
                + parameters[t][CRECTOTAG4]
                * variables[pn][PRESENCERECACTIVITIES] * a.age55to65
                + parameters[t][CRECAGE6]
                * variables[pn][PRESENCERECACTIVITIES] * a.age65plus
                + parameters[t][CRECALINC]
                * variables[pn][PRESENCERECACTIVITIES] * a.incLow
                + parameters[t][CRECAHINC]
                * variables[pn][PRESENCERECACTIVITIES] * a.incHi
                + parameters[t][CREC2AD]
                * variables[pn][PRESENCERECACTIVITIES] * a.adultsge2
                + parameters[t][CRECHH5]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType5
                + parameters[t][CRECHH6]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType6
                + parameters[t][CRECHH7]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType7
                + parameters[t][CRECHH8]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType8
                + parameters[t][CRECHH9]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType9
                + parameters[t][CRECHH10]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType10
                + parameters[t][CRECHH11]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType11
                + parameters[t][CRECHH12]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType12
                + parameters[t][CRECHH13]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType13
                + parameters[t][CRECHH14]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType14
                + parameters[t][CRECHH15]
                * variables[pn][PRESENCERECACTIVITIES] * a.hType15
                + parameters[t][COTHAFEM]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.female
                + parameters[t][COTHTTA2]
                * variables[pn][PRESENCEOTHERACTIVITIES]
                * a.age11to14 + parameters[t][COTHTTA3]
                * variables[pn][PRESENCEOTHERACTIVITIES]
                * a.age14to16 + parameters[t][COTHTT4C]
                * variables[pn][PRESENCEOTHERACTIVITIES]
                * a.age16to18 * a.autosgtadults + parameters[t][COTHAAGE25]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.agelt25
                + parameters[t][COTHAGE5]
                * variables[pn][PRESENCEOTHERACTIVITIES]
                * a.age55to65 + parameters[t][COTHAGE6]
                * variables[pn][PRESENCEOTHERACTIVITIES]
                * a.age65plus + parameters[t][COTHTTLI]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.incLow
                + parameters[t][COTHAHINC]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.incHi
                + parameters[t][COTHNWA]
                * variables[pn][PRESENCEOTHERACTIVITIES]
                * a.adultsgtworkers + parameters[t][COTH1AD]
                * variables[pn][PRESENCEOTHERACTIVITIES]
                * a.adultsle1 + parameters[t][COTHHH1]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType1
                + parameters[t][COTHHH2]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType2
                + parameters[t][COTHHH3]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType3
                + parameters[t][COTHHH4]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType4
                + parameters[t][COTHHH5]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType5
                + parameters[t][COTHHH6]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType6
                + parameters[t][COTHHH7]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType7
                + parameters[t][COTHHH8]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType8
                + parameters[t][COTHHH9]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType9
                + parameters[t][COTHHH10]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType10
                + parameters[t][COTHHH11]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType11
                + parameters[t][COTHHH12]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType12
                + parameters[t][COTHHH13]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType13
                + parameters[t][COTHHH14]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType14
                + parameters[t][COTHHH15]
                * variables[pn][PRESENCEOTHERACTIVITIES] * a.hType15
                + parameters[t][CHFEM] * variables[pn][STAYATHOME]
                * a.female + parameters[t][CHOMEAGE1]
                * variables[pn][STAYATHOME] * a.age1
                + parameters[t][CHAGE1] * variables[pn][STAYATHOME]
                * a.age25to35 + parameters[t][CHAGE2]
                * variables[pn][STAYATHOME] * a.age35to45
                + parameters[t][CHAGE3] * variables[pn][STAYATHOME]
                * a.age45to55 + parameters[t][CHAGE4]
                * variables[pn][STAYATHOME] * a.age55to65
                + parameters[t][CHAGE5] * variables[pn][STAYATHOME]
                * a.age65plus + parameters[t][CHOMELINC]
                * variables[pn][STAYATHOME] * a.incLow
                + parameters[t][CHOMEA0] * variables[pn][STAYATHOME]
                * a.autos0 + parameters[t][CHOMEALTA]
                * variables[pn][STAYATHOME] * a.autosltadults
                + parameters[t][CHOMENWA]
                * variables[pn][STAYATHOME] * a.adultsgtworkers
                + parameters[t][CHOMEHH1]
                * variables[pn][STAYATHOME] * a.hType1
                + parameters[t][CHOMEHH2]
                * variables[pn][STAYATHOME] * a.hType2
                + parameters[t][CHOMEHH3]
                * variables[pn][STAYATHOME] * a.hType3
                + parameters[t][CHOMEHH4]
                * variables[pn][STAYATHOME] * a.hType4
                + parameters[t][CHOMEHH5]
                * variables[pn][STAYATHOME] * a.hType5
                + parameters[t][CHOMEHH6]
                * variables[pn][STAYATHOME] * a.hType6
                + parameters[t][CHOMEHH7]
                * variables[pn][STAYATHOME] * a.hType7
                + parameters[t][CHOMEHH8]
                * variables[pn][STAYATHOME] * a.hType8
                + parameters[t][CHOMEHH9]
                * variables[pn][STAYATHOME] * a.hType9
                + parameters[t][CHOMEHH10]
                * variables[pn][STAYATHOME] * a.hType10
                + parameters[t][CHOMEHH11]
                * variables[pn][STAYATHOME] * a.hType11
                + parameters[t][CHOMEHH12]
                * variables[pn][STAYATHOME] * a.hType12
                + parameters[t][CHOMEHH13]
                * variables[pn][STAYATHOME] * a.hType13
                + parameters[t][CHOMEHH14]
                * variables[pn][STAYATHOME] * a.hType14
                + parameters[t][CHOMEHH15]
                * variables[pn][STAYATHOME] * a.hType15
                + parameters[t][CDISTTR2] * variables[pn][NTOURS]
                * a.workDist1to2p5 + parameters[t][CDISTTR3]
                * variables[pn][NTOURS] * a.workDist2p5to5
                + parameters[t][CDISTTR4] * variables[pn][NTOURS]
                * a.workDist5to10 + parameters[t][CDISTTR5]
                * variables[pn][NTOURS] * a.workDist10to25
                + parameters[t][CDISTTR6] * variables[pn][NTOURS]
                * a.workDist25to50 + parameters[t][CDISTTR7]
                * variables[pn][NTOURS] * a.workDist50plus
                + parameters[t][CDISTST2]
                * variables[pn][NUMBEROFSTOPSONWORK]
                * a.workDist1to2p5
                + parameters[t][CDISTST3]
                * variables[pn][NUMBEROFSTOPSONWORK]
                * a.workDist2p5to5
                + parameters[t][CDISTST4]
                * variables[pn][NUMBEROFSTOPSONWORK]
                * a.workDist5to10
                + parameters[t][CDISTST5]
                * variables[pn][NUMBEROFSTOPSONWORK]
                * a.workDist10to25
                + parameters[t][CDISTST6]
                * variables[pn][NUMBEROFSTOPSONWORK]
                * a.workDist25to50
                + parameters[t][CDISTST7]
                * variables[pn][NUMBEROFSTOPSONWORK]
                * a.workDist50plus
                + parameters[t][CDISTSTA2]
                * variables[pn][STOPS] * a.workDist1to2p5
                + parameters[t][CDISTSTA3] * variables[pn][STOPS]
                * a.workDist2p5to5 + parameters[t][CDISTSTA4]
                * variables[pn][STOPS] * a.workDist5to10
                + parameters[t][CDISTSTA5] * variables[pn][STOPS]
                * a.workDist10to25 + parameters[t][CDISTSTA6]
                * variables[pn][STOPS]
                * a.workDist25to50 + parameters[t][CDISTSTA7]
                * variables[pn][STOPS] * a.workDist50plus
                + parameters[t][CNWRKTRS] * variables[pn][WORK_TOURS]
                + parameters[t][CNSCHTRS] * variables[pn][SCHOOL_TOURS]
                + parameters[t][CNSHPTRS] * variables[pn][SHOP_TOURS]
                + parameters[t][CNRECTRS] * variables[pn][REC_TOURS]
                + parameters[t][CNOTHTRS] * variables[pn][OTHER_TOURS]
                + parameters[t][CLSUMTOURSHP]     * a.dcLogsumShop    * variables[pn][SHOP_TOURS]
                + parameters[t][CLSUMTOURRECOTH]  * a.dcLogsumRec     * variables[pn][REC_TOURS]
                + parameters[t][CLSUMTOURRECOTH]  * a.dcLogsumOther   * variables[pn][OTHER_TOURS]
                + parameters[t][CLSUMSTOPPERTOUR] * a.dcLogsumShop    * variables[pn][SHOPSTOPSPERTOUR]
                + parameters[t][CLSUMSTOPPERTOUR] * a.dcLogsumRec     * variables[pn][RECSTOPSPERTOUR]
                + parameters[t][CLSUMSTOPPERTOUR] * a.dcLogsumOther   * variables[pn][OTHERSTOPSPERTOUR]                                                  
                + parameters[t][CLSUMCTOUR]       * a.dcLogsumCollege * variables[pn][NTOURS]      
                + parameters[t][CLSUMCSPT]        * a.dcLogsumCollege * variables[pn][SHOPSTOPSPERTOUR]                                                                    
                + parameters[t][CLSUMCSPT]        * a.dcLogsumCollege * variables[pn][RECSTOPSPERTOUR]
                + parameters[t][CLSUMCSPT]        * a.dcLogsumCollege * variables[pn][OTHERSTOPSPERTOUR]; 
        
        if (parameters[t].length>CSCHSTOPSCHTOUR && variables[pn].length>PRESENCESCHOOLSTOPSONSCHOOLTOURS) {
            utility += (double) parameters[t][CSCHSTOPSCHTOUR] * variables[pn][PRESENCESCHOOLSTOPSONSCHOOLTOURS] 
                    +  parameters[t][CWRKSTOPSCHTOUR] * variables[pn][PRESENCEWORKSTOPSONLYONSCHOOLTOURS]
                    +  parameters[t][CHOMEWORKHOME]   * variables[pn][WORKONLYNOSTOPS];                                            
        }
         
        if (trace) {
            logger.info("***");
            logger.info("Pattern "+patterns.getStringValueAt(patternNumber,2));
            logger.info("pattern utility: " + utility + " = " + parameters[t][CTRS1]); 
            logger.info("\tvariables[pn][ONETOURPATTERN]     *" +  variables[pn][ONETOURPATTERN]);  
            logger.info("\tparameters[t][CTRS2]    +" +  parameters[t][CTRS2]);   
            logger.info("\tvariables[pn][TWOTOURSPATTERN]    *" +  variables[pn][TWOTOURSPATTERN]);   
            logger.info("\tparameters[t][CTRS3]    +" +  parameters[t][CTRS3]);   
            logger.info("\tvariables[pn][THREETOURSPATTERN]    *" +  variables[pn][THREETOURSPATTERN]);   
            logger.info("\tparameters[t][CTRS4]    +" +  parameters[t][CTRS4]);   
            logger.info("\tvariables[pn][FOURTOURSPATTERN]     *" +  variables[pn][FOURTOURSPATTERN]);  
            logger.info("\tparameters[t][CTRS5]    +" +  parameters[t][CTRS5]);   
            logger.info("\tvariables[pn][FIVEPLUSTOURSPATTERN]     *" +  variables[pn][FIVEPLUSTOURSPATTERN]);  
            logger.info("\tparameters[t][CWRKNSTPS]    +" +  parameters[t][CWRKNSTPS]);   
            logger.info("\tvariables[pn][WORKONLYNOSTOPS]    *" +  variables[pn][WORKONLYNOSTOPS]);   
            logger.info("\tparameters[t][CWRKNSTP2]    +" +  parameters[t][CWRKNSTP2]);   
            logger.info("\tvariables[pn][WORKNOSTOPS]    *" +  variables[pn][WORKNOSTOPS]);   
            logger.info("\tparameters[t][CWRKOSTPS]    +" +  parameters[t][CWRKOSTPS]);   
            logger.info("\tvariables[pn][WORKONLYOUTSTOPS]     *" +  variables[pn][WORKONLYOUTSTOPS]);  
            logger.info("\tparameters[t][CWRKISTPS]    +" +  parameters[t][CWRKISTPS]);   
            logger.info("\tvariables[pn][WORKONLYINSTOPS]    *" +  variables[pn][WORKONLYINSTOPS]);   
            logger.info("\tparameters[t][CWRKOSTP2]    +" +  parameters[t][CWRKOSTP2]);   
            logger.info("\tvariables[pn][WORKOUTSTOPS]     *" +  variables[pn][WORKOUTSTOPS]);  
            logger.info("\tparameters[t][CWRKISTP2]    +" +  parameters[t][CWRKISTP2]);   
            logger.info("\tvariables[pn][WORKINSTOPS]  + parameters[t][CSCH]    *" +  variables[pn][WORKINSTOPS]  + "+" +  parameters[t][CSCH]); 
            logger.info("\tvariables[pn][SCHOOLONLY]  +  parameters[t][CWRK]   *" +  variables[pn][SCHOOLONLY]  + "+" +  parameters[t][CWRK]);  
            logger.info("\tvariables[pn][WORKONLY]  +  parameters[t][CSCHWRK]    *" +  variables[pn][WORKONLY]  + "+" +  parameters[t][CSCHWRK]); 
            logger.info("\tvariables[pn][SCHOOLBEFOREWORK]     *" +  variables[pn][SCHOOLBEFOREWORK]);  
            logger.info("\tparameters[t][CWRKSCH]    +" +  parameters[t][CWRKSCH]);   
            logger.info("\tvariables[pn][WORKBEFORESCHOOL]     *" +  variables[pn][WORKBEFORESCHOOL]);  
            logger.info("\tparameters[t][CWRKPNSTP2]     +" +  parameters[t][CWRKPNSTP2]);  
            logger.info("\tvariables[pn][WORK2PNOSTOPS]    *" +  variables[pn][WORK2PNOSTOPS]);   
            logger.info("\tparameters[t][CWRKPWSTP2]     +" +  parameters[t][CWRKPWSTP2]);  
            logger.info("\tvariables[pn][WORK2PWITHSTOPS]    *" +  variables[pn][WORK2PWITHSTOPS]);   
            logger.info("\tparameters[t][CACTCG1D]  *  variables[pn][SCHOOL2P]   +" +  parameters[t][CACTCG1D]  + "*" +  variables[pn][SCHOOL2P]);  
            logger.info("\tparameters[t][CBACT]    +" +  parameters[t][CBACT]);   
            logger.info("\tvariables[pn][NUMWBASEDTOURS]     *" +  variables[pn][NUMWBASEDTOURS]);  
            logger.info("\tparameters[t][CBACT1]     +" +  parameters[t][CBACT1]);  
            logger.info("\tvariables[pn][PRESENCEWBASEDTOURS]    *" +  variables[pn][PRESENCEWBASEDTOURS]);   
            logger.info("\tparameters[t][CHSCHH]     +" +  parameters[t][CHSCHH]);  
            logger.info("\tvariables[pn][HOMESCHOOLHOMEPATTERN]    *" +  variables[pn][HOMESCHOOLHOMEPATTERN]);   
            logger.info("\tparameters[t][CT1OTH]  *  variables[pn][OTHERONLY]    +" +  parameters[t][CT1OTH]  + "*" +  variables[pn][OTHERONLY]); 
            logger.info("\tparameters[t][CT2SHP]  *  variables[pn][SHOPONLY2P]   +" +  parameters[t][CT2SHP]  + "*" +  variables[pn][SHOPONLY2P]);  
            logger.info("\tparameters[t][CT2OTH]  *  variables[pn][OTHERONLY2P]    +" +  parameters[t][CT2OTH]  + "*" +  variables[pn][OTHERONLY2P]); 
            logger.info("\tparameters[t][CP0SEC3B]     +" +  parameters[t][CP0SEC3B]);  
            logger.info("\tvariables[pn][NOPRIMARYTHREEPLUSTOURS]    *" +  variables[pn][NOPRIMARYTHREEPLUSTOURS]); 
            logger.info("\tparameters[t][CACTSD]     +" +  parameters[t][CACTSD]);  
            logger.info("\tvariables[pn][PRESENCESHOPTOURS]    *" +  variables[pn][PRESENCESHOPTOURS]);   
            logger.info("\tparameters[t][CACTRD]     +" +  parameters[t][CACTRD]);  
            logger.info("\tvariables[pn][PRESENCERECTOURS]     *" +  variables[pn][PRESENCERECTOURS]);  
            logger.info("\tparameters[t][CACTOD]     +" +  parameters[t][CACTOD]);  
            logger.info("\tvariables[pn][PRESENCEOTHERTOURS]     *" +  variables[pn][PRESENCEOTHERTOURS]);  
            logger.info("\tparameters[t][CSHPWRK]    +" +  parameters[t][CSHPWRK]);   
            logger.info("\tvariables[pn][SHOPBEFOREWORK]     *" +  variables[pn][SHOPBEFOREWORK]);  
            logger.info("\tparameters[t][CRECWRK]    +" +  parameters[t][CRECWRK]);   
            logger.info("\tvariables[pn][RECBEFOREWORK]    *" +  variables[pn][RECBEFOREWORK]);   
            logger.info("\tparameters[t][COTHWRK]    +" +  parameters[t][COTHWRK]);   
            logger.info("\tvariables[pn][OTHBEFOREWORK]    *" +  variables[pn][OTHBEFOREWORK]);   
            logger.info("\tparameters[t][CSHPSCH]    +" +  parameters[t][CSHPSCH]);   
            logger.info("\tvariables[pn][SHOPBEFORESCHOOL]     *" +  variables[pn][SHOPBEFORESCHOOL]);  
            logger.info("\tparameters[t][CRECSCH]    +" +  parameters[t][CRECSCH]);   
            logger.info("\tvariables[pn][RECBEFORESCHOOL]    *" +  variables[pn][RECBEFORESCHOOL]);   
            logger.info("\tparameters[t][COTHSCH]    +" +  parameters[t][COTHSCH]);   
            logger.info("\tvariables[pn][OTHBEFORESCHOOL]    *" +  variables[pn][OTHBEFORESCHOOL]);   
            logger.info("\tparameters[t][CRECSHP]    +" +  parameters[t][CRECSHP]);   
            logger.info("\tvariables[pn][RECBEFORESHOP]    *" +  variables[pn][RECBEFORESHOP]);   
            logger.info("\tparameters[t][CSHPACT1]     +" +  parameters[t][CSHPACT1]);  
            logger.info("\tvariables[pn][ONESHOPACTIVITY]    *" +  variables[pn][ONESHOPACTIVITY]);   
            logger.info("\tparameters[t][CSHPACT2]     +" +  parameters[t][CSHPACT2]);  
            logger.info("\tvariables[pn][TWOSHOPACTIVITIES]    *" +  variables[pn][TWOSHOPACTIVITIES]);   
            logger.info("\tparameters[t][CSHPACT3]     +" +  parameters[t][CSHPACT3]);  
            logger.info("\tvariables[pn][THREESHOPACTIVITIES]    *" +  variables[pn][THREESHOPACTIVITIES]);   
            logger.info("\tparameters[t][CSHPACT4]     +" +  parameters[t][CSHPACT4]);  
            logger.info("\tvariables[pn][FOURSHOPACTIVITIES]     *" +  variables[pn][FOURSHOPACTIVITIES]);  
            logger.info("\tparameters[t][CRECACT1]     +" +  parameters[t][CRECACT1]);  
            logger.info("\tvariables[pn][ONERECACTIVITY]     *" +  variables[pn][ONERECACTIVITY]);  
            logger.info("\tparameters[t][CRECACT2]     +" +  parameters[t][CRECACT2]);  
            logger.info("\tvariables[pn][TWORECACTIVITIES]     *" +  variables[pn][TWORECACTIVITIES]);  
            logger.info("\tparameters[t][CRECACT3]     +" +  parameters[t][CRECACT3]);  
            logger.info("\tvariables[pn][THREERECACTIVITIES]     *" +  variables[pn][THREERECACTIVITIES]);  
            logger.info("\tparameters[t][CRECACT4]     +" +  parameters[t][CRECACT4]);  
            logger.info("\tvariables[pn][FOURRECACTIVITIES]    *" +  variables[pn][FOURRECACTIVITIES]);   
            logger.info("\tparameters[t][COTHACT1]     +" +  parameters[t][COTHACT1]);  
            logger.info("\tvariables[pn][ONEOTHERACTIVITY]     *" +  variables[pn][ONEOTHERACTIVITY]);  
            logger.info("\tparameters[t][COTHACT2]     +" +  parameters[t][COTHACT2]);  
            logger.info("\tvariables[pn][TWOOTHERACTIVITIES]     *" +  variables[pn][TWOOTHERACTIVITIES]);  
            logger.info("\tparameters[t][COTHACT3]     +" +  parameters[t][COTHACT3]);  
            logger.info("\tvariables[pn][THREEOTHERACTIVITIES]     *" +  variables[pn][THREEOTHERACTIVITIES]);  
            logger.info("\tparameters[t][COTHACT4]     +" +  parameters[t][COTHACT4]);  
            logger.info("\tvariables[pn][FOUROTHERACTIVITIES]    *" +  variables[pn][FOUROTHERACTIVITIES]);   
            logger.info("\tparameters[t][CWNWINT1]     +" +  parameters[t][CWNWINT1]);  
            logger.info("\tvariables[pn][STOPSWORKTIMESNONWORKTOURS]     *" +  variables[pn][STOPSWORKTIMESNONWORKTOURS]);  
            logger.info("\tparameters[t][CWNWINT2]     +" +  parameters[t][CWNWINT2]);  
            logger.info("\tvariables[pn][STOPSWORKTIMESSTOPSNONWORK]     *" +  variables[pn][STOPSWORKTIMESSTOPSNONWORK]);  
            logger.info("\tparameters[t][CTSINT1]    +" +  parameters[t][CTSINT1]);   
            logger.info("\tvariables[pn][STOPSTIMESTOURS]    *" +  variables[pn][STOPSTIMESTOURS]);   
            logger.info("\tparameters[t][CSCOUT]     +" +  parameters[t][CSCOUT]);  
            logger.info("\tvariables[pn][SCHOOLWITHOUTSTOPS]     *" +  variables[pn][SCHOOLWITHOUTSTOPS]);  
            logger.info("\tparameters[t][CSCIN]    +" +  parameters[t][CSCIN]);   
            logger.info("\tvariables[pn][SCHOOLWITHINSTOPS]    *" +  variables[pn][SCHOOLWITHINSTOPS]);   
            logger.info("\tparameters[t][CSCINOUT]     +" +  parameters[t][CSCINOUT]);  
            logger.info("\tvariables[pn][SCHOOLWITHBOTHSTOPS]    *" +  variables[pn][SCHOOLWITHBOTHSTOPS]);   
            logger.info("\tparameters[t][CWRKOUT]    +" +  parameters[t][CWRKOUT]);   
            logger.info("\tvariables[pn][WORKWITHOUTSTOPS]     *" +  variables[pn][WORKWITHOUTSTOPS]);  
            logger.info("\tparameters[t][CWRKIN]     +" +  parameters[t][CWRKIN]);  
            logger.info("\tvariables[pn][WORKWITHINSTOPS]    *" +  variables[pn][WORKWITHINSTOPS]);   
            logger.info("\tparameters[t][CWRKINOUT]    +" +  parameters[t][CWRKINOUT]);   
            logger.info("\tvariables[pn][WORKWITHBOTHSTOPS]    *" +  variables[pn][WORKWITHBOTHSTOPS]);   
            logger.info("\tparameters[t][CP1_O]    +" +  parameters[t][CP1_O]);   
            logger.info("\tvariables[pn][SCHOOLORWORKOUTSTOPS]     *" +  variables[pn][SCHOOLORWORKOUTSTOPS]);  
            logger.info("\tparameters[t][CP1_I]    +" +  parameters[t][CP1_I]);   
            logger.info("\tvariables[pn][SCHOOLORWORKINSTOPS]    *" +  variables[pn][SCHOOLORWORKINSTOPS]);   
            logger.info("\tparameters[t][CP1_IO]     +" +  parameters[t][CP1_IO]);  
            logger.info("\tvariables[pn][SCHOOLORWORKBOTHSTOPS]    *" +  variables[pn][SCHOOLORWORKBOTHSTOPS]);   
            logger.info("\tparameters[t][CP2_O1]     +" +  parameters[t][CP2_O1]);  
            logger.info("\tvariables[pn][SCHOOLANDWORKOUTSTOPSFIRST]     *" +  variables[pn][SCHOOLANDWORKOUTSTOPSFIRST]);  
            logger.info("\tparameters[t][CP2_I1]     +" +  parameters[t][CP2_I1]);  
            logger.info("\tvariables[pn][SCHOOLANDWORKINSTOPSFIRST]    *" +  variables[pn][SCHOOLANDWORKINSTOPSFIRST]); 
            logger.info("\tparameters[t][CP2_IO1]    +" +  parameters[t][CP2_IO1]);   
            logger.info("\tvariables[pn][SCHOOLANDWORKBOTHSTOPSFIRST]    *" +  variables[pn][SCHOOLANDWORKBOTHSTOPSFIRST]); 
            logger.info("\tparameters[t][CP2_O2]     +" +  parameters[t][CP2_O2]);  
            logger.info("\tvariables[pn][SCHOOLANDWORKOUTSTOPSSEC]     *" +  variables[pn][SCHOOLANDWORKOUTSTOPSSEC]);  
            logger.info("\tparameters[t][CP2_I2]     +" +  parameters[t][CP2_I2]);  
            logger.info("\tvariables[pn][SCHOOLANDWORKINSTOPSSEC]    *" +  variables[pn][SCHOOLANDWORKINSTOPSSEC]); 
            logger.info("\tparameters[t][CP2_IO2]    +" +  parameters[t][CP2_IO2]);   
            logger.info("\tvariables[pn][SCHOOLANDWORKBOTHSTOPSSEC]    *" +  variables[pn][SCHOOLANDWORKBOTHSTOPSSEC]); 
            logger.info("\tparameters[t][CCOMBO_IO]    +" +  parameters[t][CCOMBO_IO]);   
            logger.info("\tvariables[pn][SCHOOLWITHWORKANDEXTRASTOPS]    *" +  variables[pn][SCHOOLWITHWORKANDEXTRASTOPS]); 
            logger.info("\tparameters[t][COUTGTINNS]     +" +  parameters[t][COUTGTINNS]);  
            logger.info("\tvariables[pn][MOREOUTTHANIN]    *" +  variables[pn][MOREOUTTHANIN]);   
            logger.info("\tparameters[t][COUTLTINNS]     +" +  parameters[t][COUTLTINNS]);  
            logger.info("\tvariables[pn][MOREINTHANOUT]    *" +  variables[pn][MOREINTHANOUT]);   
            logger.info("\tparameters[t][COUTEQINNS]     +" +  parameters[t][COUTEQINNS]);  
            logger.info("\tvariables[pn][EQUALOUTANDIN]    *" +  variables[pn][EQUALOUTANDIN]);   
            logger.info("\tparameters[t][CSTPBD]     +" +  parameters[t][CSTPBD]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONWBASED]    *" +  variables[pn][PRESENCESTOPSONWBASED]);   
            logger.info("\tparameters[t][CSTOPSSD]     +" +  parameters[t][CSTOPSSD]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOP]    *" +  variables[pn][PRESENCESTOPSONSHOP]);   
            logger.info("\tparameters[t][CSTOPSRD]     +" +  parameters[t][CSTOPSRD]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONREC]     *" +  variables[pn][PRESENCESTOPSONREC]);  
            logger.info("\tparameters[t][CSTOPSO1]     +" +  parameters[t][CSTOPSO1]);  
            logger.info("\tvariables[pn][ONESTOPONOTHER]     *" +  variables[pn][ONESTOPONOTHER]);  
            logger.info("\tparameters[t][CSTOPSO2]     +" +  parameters[t][CSTOPSO2]);  
            logger.info("\tvariables[pn][TWOSTOPSONOTHER]    *" +  variables[pn][TWOSTOPSONOTHER]);   
            logger.info("\tparameters[t][CSTOPSO3]     +" +  parameters[t][CSTOPSO3]);  
            logger.info("\tvariables[pn][THREEPLUSSTOPSONOTHER]    *" +  variables[pn][THREEPLUSSTOPSONOTHER]);   
            logger.info("\tparameters[t][C2TA0]  *  variables[pn][TWOPLUSTOURS]    +" +  parameters[t][C2TA0]  + "*" +  variables[pn][TWOPLUSTOURS]); 
            logger.info("\ta.autos0  +  parameters[t][CTWOALTA]    *" +  a.autos0  + "+" +  parameters[t][CTWOALTA]); 
            logger.info("\tvariables[pn][TWOPLUSTOURS]  *  a.autosltadults   *" +  variables[pn][TWOPLUSTOURS]  + "*" +  a.autosltadults);  
            logger.info("\tparameters[t][CSTPNWA0]     +" +  parameters[t][CSTPNWA0]);  
            logger.info("\tvariables[pn][PRESENCESTOPS]  *  a.autos0   *" +  variables[pn][PRESENCESTOPS]  + "*" +  a.autos0);  
            logger.info("\tparameters[t][CSTOPALTA]    +" +  parameters[t][CSTOPALTA]);   
            logger.info("\tvariables[pn][PRESENCESTOPS]  *  a.autosltadults    *" +  variables[pn][PRESENCESTOPS]  + "*" +  a.autosltadults); 
            logger.info("\tparameters[t][CSTPWA0]    +" +  parameters[t][CSTPWA0]);   
            logger.info("\tvariables[pn][PRESENCESTOPSONWORK]  *  a.autos0   *" +  variables[pn][PRESENCESTOPSONWORK]  + "*" +  a.autos0);  
            logger.info("\tparameters[t][CSTPBA0]    +" +  parameters[t][CSTPBA0]);   
            logger.info("\tvariables[pn][PRESENCESTOPSONWBASED]  *  a.autos0   *" +  variables[pn][PRESENCESTOPSONWBASED]  + "*" +  a.autos0);  
            logger.info("\tparameters[t][CSTOPNWA0]    +" +  parameters[t][CSTOPNWA0]);   
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.autos0  +  parameters[t][CSRPTOURZA]    *" +  a.autos0  + "+" +  parameters[t][CSRPTOURZA]); 
            logger.info("\tvariables[pn][PRESENCESHOPRECOTHTOURS]  *  a.autos0   *" +  variables[pn][PRESENCESHOPRECOTHTOURS]  + "*" +  a.autos0);  
            logger.info("\tparameters[t][CSROSTPC]     +" +  parameters[t][CSROSTPC]);  
            logger.info("\tvariables[pn][PRESENCESTOPSFORSHOPRECOTH]     *" +  variables[pn][PRESENCESTOPSFORSHOPRECOTH]);  
            logger.info("\ta.autosltworkers  +  parameters[t][CSROSTNWA]   *" +  a.autosltworkers  + "+" +  parameters[t][CSROSTNWA]);  
            logger.info("\tvariables[pn][PRESENCESTOPSFORSHOPRECOTH]     *" +  variables[pn][PRESENCESTOPSFORSHOPRECOTH]);  
            logger.info("\ta.adultsgtworkers  +  parameters[t][CSROSTPAD]    *" +  a.adultsgtworkers  + "+" +  parameters[t][CSROSTPAD]); 
            logger.info("\tvariables[pn][PRESENCESTOPSFORSHOPRECOTH]     *" +  variables[pn][PRESENCESTOPSFORSHOPRECOTH]);  
            logger.info("\ta.adultsle1  +  parameters[t][CSTPPRIMHH7]    *" +  a.adultsle1  + "+" +  parameters[t][CSTPPRIMHH7]); 
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType7    *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType7); 
            logger.info("\tparameters[t][CSTPPRIMHH8]    +" +  parameters[t][CSTPPRIMHH8]);   
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType8    *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType8); 
            logger.info("\tparameters[t][CSTPPRIMHH9]    +" +  parameters[t][CSTPPRIMHH9]);   
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType9    *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType9); 
            logger.info("\tparameters[t][CSTPPRIMHH10]     +" +  parameters[t][CSTPPRIMHH10]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType10   *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType10);  
            logger.info("\tparameters[t][CSTPPRIMHH11]     +" +  parameters[t][CSTPPRIMHH11]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType11   *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType11);  
            logger.info("\tparameters[t][CSTPPRIMHH12]     +" +  parameters[t][CSTPPRIMHH12]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType12   *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType12);  
            logger.info("\tparameters[t][CSTPPRIMHH13]     +" +  parameters[t][CSTPPRIMHH13]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType13   *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType13);  
            logger.info("\tparameters[t][CSTPPRIMHH14]     +" +  parameters[t][CSTPPRIMHH14]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType14   *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType14);  
            logger.info("\tparameters[t][CSTPPRIMHH15]     +" +  parameters[t][CSTPPRIMHH15]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONPRIMARY]  *  a.hType15   *" +  variables[pn][PRESENCESTOPSONPRIMARY]  + "*" +  a.hType15);  
            logger.info("\tparameters[t][CSTPNWHH1]    +" +  parameters[t][CSTPNWHH1]);   
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType1  +  parameters[t][CSTPNWHH2]     *" +  a.hType1  + "+" +  parameters[t][CSTPNWHH2]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType2  +  parameters[t][CSTPNWHH3]     *" +  a.hType2  + "+" +  parameters[t][CSTPNWHH3]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType3  +  parameters[t][CSTPNWHH4]     *" +  a.hType3  + "+" +  parameters[t][CSTPNWHH4]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType4  +  parameters[t][CSTPNWHH5]     *" +  a.hType4  + "+" +  parameters[t][CSTPNWHH5]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType5  +  parameters[t][CSTPNWHH6]     *" +  a.hType5  + "+" +  parameters[t][CSTPNWHH6]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType6  +  parameters[t][CSTPNWHH7]     *" +  a.hType6  + "+" +  parameters[t][CSTPNWHH7]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType7  +  parameters[t][CSTPNWHH8]     *" +  a.hType7  + "+" +  parameters[t][CSTPNWHH8]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType8  +  parameters[t][CSTPNWHH9]     *" +  a.hType8  + "+" +  parameters[t][CSTPNWHH9]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType9  +  parameters[t][CSTPNWHH10]    *" +  a.hType9  + "+" +  parameters[t][CSTPNWHH10]); 
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType10  +  parameters[t][CSTPNWHH11]     *" +  a.hType10  + "+" +  parameters[t][CSTPNWHH11]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType11  +  parameters[t][CSTPNWHH12]     *" +  a.hType11  + "+" +  parameters[t][CSTPNWHH12]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType12  +  parameters[t][CSTPNWHH13]     *" +  a.hType12  + "+" +  parameters[t][CSTPNWHH13]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType13  +  parameters[t][CSTPNWHH14]     *" +  a.hType13  + "+" +  parameters[t][CSTPNWHH14]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType14  +  parameters[t][CSTPNWHH15]     *" +  a.hType14  + "+" +  parameters[t][CSTPNWHH15]);  
            logger.info("\tvariables[pn][PRESENCESTOPSONSHOPRECOTH]    *" +  variables[pn][PRESENCESTOPSONSHOPRECOTH]); 
            logger.info("\ta.hType15  +  parameters[t][CSCH_W]     *" +  a.hType15  + "+" +  parameters[t][CSCH_W]);  
            logger.info("\tvariables[pn][SCHOOLONLY]  *  a.worker    *" +  variables[pn][SCHOOLONLY]  + "*" +  a.worker); 
            logger.info("\tparameters[t][CSCHAGE1]     +" +  parameters[t][CSCHAGE1]);  
            logger.info("\tvariables[pn][PRESENCESCHOOLTOURS]  *  a.age1   *" +  variables[pn][PRESENCESCHOOLTOURS]  + "*" +  a.age1);  
            logger.info("\tparameters[t][CSCHAGE2]     +" +  parameters[t][CSCHAGE2]);  
            logger.info("\tvariables[pn][PRESENCESCHOOLTOURS]  *  a.age2   *" +  variables[pn][PRESENCESCHOOLTOURS]  + "*" +  a.age2);  
            logger.info("\tparameters[t][CSCHAGE3]     +" +  parameters[t][CSCHAGE3]);  
            logger.info("\tvariables[pn][PRESENCESCHOOLTOURS]  *  a.age3   *" +  variables[pn][PRESENCESCHOOLTOURS]  + "*" +  a.age3);  
            logger.info("\tparameters[t][CSCHAGE4]     +" +  parameters[t][CSCHAGE4]);  
            logger.info("\tvariables[pn][PRESENCESCHOOLTOURS]  *  a.age4   *" +  variables[pn][PRESENCESCHOOLTOURS]  + "*" +  a.age4);  
            logger.info("\tparameters[t][CSCHAGE5]     +" +  parameters[t][CSCHAGE5]);  
            logger.info("\tvariables[pn][PRESENCESCHOOLTOURS]  *  a.age5   *" +  variables[pn][PRESENCESCHOOLTOURS]  + "*" +  a.age5);  
            logger.info("\tparameters[t][CWRKAGE3]     +" +  parameters[t][CWRKAGE3]);  
            logger.info("\tvariables[pn][PRESENCEWORKACTIVITIES]  *  a.age15   *" +  variables[pn][PRESENCEWORKACTIVITIES]  + "*" +  a.age15);  
            logger.info("\tparameters[t][CWRKAGE4]     +" +  parameters[t][CWRKAGE4]);  
            logger.info("\tvariables[pn][PRESENCEWORKACTIVITIES]  *  a.age16   *" +  variables[pn][PRESENCEWORKACTIVITIES]  + "*" +  a.age16);  
            logger.info("\tparameters[t][CWRKAGE5]     +" +  parameters[t][CWRKAGE5]);  
            logger.info("\tvariables[pn][PRESENCEWORKACTIVITIES]  *  a.age17   *" +  variables[pn][PRESENCEWORKACTIVITIES]  + "*" +  a.age17);  
            logger.info("\tparameters[t][CSCHHINC]     +" +  parameters[t][CSCHHINC]);  
            logger.info("\tvariables[pn][PRESENCESCHOOLTOURS]  *  a.incHi    *" +  variables[pn][PRESENCESCHOOLTOURS]  + "*" +  a.incHi); 
            logger.info("\tparameters[t][CSCHNWA]    +" +  parameters[t][CSCHNWA]);   
            logger.info("\tvariables[pn][PRESENCESCHOOLTOURS]    *" +  variables[pn][PRESENCESCHOOLTOURS]);   
            logger.info("\ta.adultsgtworkers  +  parameters[t][CSTPSWFM]   *" +  a.adultsgtworkers  + "+" +  parameters[t][CSTPSWFM]);  
            logger.info("\tvariables[pn][PRESENCESHOPSTOPSONWORK]  *  a.female   *" +  variables[pn][PRESENCESHOPSTOPSONWORK]  + "*" +  a.female);  
            logger.info("\tparameters[t][CSHPAFEM]     +" +  parameters[t][CSHPAFEM]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.female    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.female); 
            logger.info("\tparameters[t][CSHPTT4C]     +" +  parameters[t][CSHPTT4C]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.age16to18 * a.autosgtadults  +  parameters[t][CSHPAAGE1]    *" +  a.age16to18 * a.autosgtadults  + "+" +  parameters[t][CSHPAAGE1]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.agelt25   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.agelt25);  
            logger.info("\tparameters[t][CSHOPAGE2]    +" +  parameters[t][CSHOPAGE2]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.age25to35  +  parameters[t][CSHOPAGE3]    *" +  a.age25to35  + "+" +  parameters[t][CSHOPAGE3]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.age35to45  +  parameters[t][CSHOPAGE4]    *" +  a.age35to45  + "+" +  parameters[t][CSHOPAGE4]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.age45to55  +  parameters[t][CSHOPAGE5]    *" +  a.age45to55  + "+" +  parameters[t][CSHOPAGE5]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.age55to65  +  parameters[t][CSHOPAGE6]    *" +  a.age55to65  + "+" +  parameters[t][CSHOPAGE6]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.age65plus  +  parameters[t][CSHOPLINC]    *" +  a.age65plus  + "+" +  parameters[t][CSHOPLINC]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.incLow    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.incLow); 
            logger.info("\tparameters[t][CSHPAHINC]    +" +  parameters[t][CSHPAHINC]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.incHi   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.incHi);  
            logger.info("\tparameters[t][CSHPNWA]    +" +  parameters[t][CSHPNWA]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.adultsgtworkers  +  parameters[t][CSHP1AD]    *" +  a.adultsgtworkers  + "+" +  parameters[t][CSHP1AD]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]     *" +  variables[pn][PRESENCESHOPACTIVITIES]);  
            logger.info("\ta.adultsle1  +  parameters[t][CSHPHH1]    *" +  a.adultsle1  + "+" +  parameters[t][CSHPHH1]); 
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType1    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType1); 
            logger.info("\tparameters[t][CSHPHH2]    +" +  parameters[t][CSHPHH2]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType2    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType2); 
            logger.info("\tparameters[t][CSHPHH3]    +" +  parameters[t][CSHPHH3]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType3    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType3); 
            logger.info("\tparameters[t][CSHPHH4]    +" +  parameters[t][CSHPHH4]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType4    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType4); 
            logger.info("\tparameters[t][CSHPHH5]    +" +  parameters[t][CSHPHH5]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType5    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType5); 
            logger.info("\tparameters[t][CSHPHH6]    +" +  parameters[t][CSHPHH6]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType6    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType6); 
            logger.info("\tparameters[t][CSHPHH7]    +" +  parameters[t][CSHPHH7]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType7    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType7); 
            logger.info("\tparameters[t][CSHPHH8]    +" +  parameters[t][CSHPHH8]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType8    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType8); 
            logger.info("\tparameters[t][CSHPHH9]    +" +  parameters[t][CSHPHH9]);   
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType9    *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType9); 
            logger.info("\tparameters[t][CSHPHH10]     +" +  parameters[t][CSHPHH10]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType10   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType10);  
            logger.info("\tparameters[t][CSHPHH11]     +" +  parameters[t][CSHPHH11]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType11   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType11);  
            logger.info("\tparameters[t][CSHPHH12]     +" +  parameters[t][CSHPHH12]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType12   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType12);  
            logger.info("\tparameters[t][CSHPHH13]     +" +  parameters[t][CSHPHH13]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType13   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType13);  
            logger.info("\tparameters[t][CSHPHH14]     +" +  parameters[t][CSHPHH14]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType14   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType14);  
            logger.info("\tparameters[t][CSHPHH15]     +" +  parameters[t][CSHPHH15]);  
            logger.info("\tvariables[pn][PRESENCESHOPACTIVITIES]  *  a.hType15   *" +  variables[pn][PRESENCESHOPACTIVITIES]  + "*" +  a.hType15);  
            logger.info("\tparameters[t][CRECAGE4]     +" +  parameters[t][CRECAGE4]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.age4   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.age4);  
            logger.info("\tparameters[t][CRECAGE5]     +" +  parameters[t][CRECAGE5]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.age5   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.age5);  
            logger.info("\tparameters[t][CRECAAGE25]     +" +  parameters[t][CRECAAGE25]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.agelt25    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.agelt25); 
            logger.info("\tparameters[t][CRECTOTAG1]     +" +  parameters[t][CRECTOTAG1]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.age25to35    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.age25to35); 
            logger.info("\tparameters[t][CRECTOTAG2]     +" +  parameters[t][CRECTOTAG2]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.age35to45    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.age35to45); 
            logger.info("\tparameters[t][CRECTOTAG3]     +" +  parameters[t][CRECTOTAG3]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.age45to55    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.age45to55); 
            logger.info("\tparameters[t][CRECTOTAG4]     +" +  parameters[t][CRECTOTAG4]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.age55to65    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.age55to65); 
            logger.info("\tparameters[t][CRECAGE6]     +" +  parameters[t][CRECAGE6]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.age65plus    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.age65plus); 
            logger.info("\tparameters[t][CRECALINC]    +" +  parameters[t][CRECALINC]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.incLow   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.incLow);  
            logger.info("\tparameters[t][CRECAHINC]    +" +  parameters[t][CRECAHINC]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.incHi    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.incHi); 
            logger.info("\tparameters[t][CREC2AD]    +" +  parameters[t][CREC2AD]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.adultsge2    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.adultsge2); 
            logger.info("\tparameters[t][CRECHH5]    +" +  parameters[t][CRECHH5]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType5   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType5);  
            logger.info("\tparameters[t][CRECHH6]    +" +  parameters[t][CRECHH6]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType6   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType6);  
            logger.info("\tparameters[t][CRECHH7]    +" +  parameters[t][CRECHH7]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType7   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType7);  
            logger.info("\tparameters[t][CRECHH8]    +" +  parameters[t][CRECHH8]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType8   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType8);  
            logger.info("\tparameters[t][CRECHH9]    +" +  parameters[t][CRECHH9]);   
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType9   *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType9);  
            logger.info("\tparameters[t][CRECHH10]     +" +  parameters[t][CRECHH10]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType10    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType10); 
            logger.info("\tparameters[t][CRECHH11]     +" +  parameters[t][CRECHH11]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType11    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType11); 
            logger.info("\tparameters[t][CRECHH12]     +" +  parameters[t][CRECHH12]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType12    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType12); 
            logger.info("\tparameters[t][CRECHH13]     +" +  parameters[t][CRECHH13]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType13    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType13); 
            logger.info("\tparameters[t][CRECHH14]     +" +  parameters[t][CRECHH14]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType14    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType14); 
            logger.info("\tparameters[t][CRECHH15]     +" +  parameters[t][CRECHH15]);  
            logger.info("\tvariables[pn][PRESENCERECACTIVITIES]  *  a.hType15    *" +  variables[pn][PRESENCERECACTIVITIES]  + "*" +  a.hType15); 
            logger.info("\tparameters[t][COTHAFEM]     +" +  parameters[t][COTHAFEM]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.female   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.female);  
            logger.info("\tparameters[t][COTHTTA2]     +" +  parameters[t][COTHTTA2]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]    *" +  variables[pn][PRESENCEOTHERACTIVITIES]); 
            logger.info("\ta.age11to14  +  parameters[t][COTHTTA3]     *" +  a.age11to14  + "+" +  parameters[t][COTHTTA3]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]    *" +  variables[pn][PRESENCEOTHERACTIVITIES]); 
            logger.info("\ta.age14to16  +  parameters[t][COTHTT4C]     *" +  a.age14to16  + "+" +  parameters[t][COTHTT4C]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]    *" +  variables[pn][PRESENCEOTHERACTIVITIES]); 
            logger.info("\ta.age16to18 * a.autosgtadults  +  parameters[t][COTHAAGE25]   *" +  a.age16to18 * a.autosgtadults  + "+" +  parameters[t][COTHAAGE25]);
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.agelt25    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.agelt25); 
            logger.info("\tparameters[t][COTHAGE5]     +" +  parameters[t][COTHAGE5]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]    *" +  variables[pn][PRESENCEOTHERACTIVITIES]); 
            logger.info("\ta.age55to65  +  parameters[t][COTHAGE6]     *" +  a.age55to65  + "+" +  parameters[t][COTHAGE6]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]    *" +  variables[pn][PRESENCEOTHERACTIVITIES]); 
            logger.info("\ta.age65plus  +  parameters[t][COTHTTLI]     *" +  a.age65plus  + "+" +  parameters[t][COTHTTLI]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.incLow   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.incLow);  
            logger.info("\tparameters[t][COTHAHINC]    +" +  parameters[t][COTHAHINC]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.incHi    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.incHi); 
            logger.info("\tparameters[t][COTHNWA]    +" +  parameters[t][COTHNWA]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]    *" +  variables[pn][PRESENCEOTHERACTIVITIES]); 
            logger.info("\ta.adultsgtworkers  +  parameters[t][COTH1AD]    *" +  a.adultsgtworkers  + "+" +  parameters[t][COTH1AD]); 
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]    *" +  variables[pn][PRESENCEOTHERACTIVITIES]); 
            logger.info("\ta.adultsle1  +  parameters[t][COTHHH1]    *" +  a.adultsle1  + "+" +  parameters[t][COTHHH1]); 
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType1   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType1);  
            logger.info("\tparameters[t][COTHHH2]    +" +  parameters[t][COTHHH2]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType2   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType2);  
            logger.info("\tparameters[t][COTHHH3]    +" +  parameters[t][COTHHH3]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType3   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType3);  
            logger.info("\tparameters[t][COTHHH4]    +" +  parameters[t][COTHHH4]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType4   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType4);  
            logger.info("\tparameters[t][COTHHH5]    +" +  parameters[t][COTHHH5]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType5   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType5);  
            logger.info("\tparameters[t][COTHHH6]    +" +  parameters[t][COTHHH6]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType6   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType6);  
            logger.info("\tparameters[t][COTHHH7]    +" +  parameters[t][COTHHH7]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType7   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType7);  
            logger.info("\tparameters[t][COTHHH8]    +" +  parameters[t][COTHHH8]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType8   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType8);  
            logger.info("\tparameters[t][COTHHH9]    +" +  parameters[t][COTHHH9]);   
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType9   *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType9);  
            logger.info("\tparameters[t][COTHHH10]     +" +  parameters[t][COTHHH10]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType10    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType10); 
            logger.info("\tparameters[t][COTHHH11]     +" +  parameters[t][COTHHH11]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType11    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType11); 
            logger.info("\tparameters[t][COTHHH12]     +" +  parameters[t][COTHHH12]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType12    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType12); 
            logger.info("\tparameters[t][COTHHH13]     +" +  parameters[t][COTHHH13]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType13    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType13); 
            logger.info("\tparameters[t][COTHHH14]     +" +  parameters[t][COTHHH14]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType14    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType14); 
            logger.info("\tparameters[t][COTHHH15]     +" +  parameters[t][COTHHH15]);  
            logger.info("\tvariables[pn][PRESENCEOTHERACTIVITIES]  *  a.hType15    *" +  variables[pn][PRESENCEOTHERACTIVITIES]  + "*" +  a.hType15); 
            logger.info("\tparameters[t][CHFEM]  *  variables[pn][STAYATHOME]    +" +  parameters[t][CHFEM]  + "*" +  variables[pn][STAYATHOME]); 
            logger.info("\ta.female  +  parameters[t][CHOMEAGE1]     *" +  a.female  + "+" +  parameters[t][CHOMEAGE1]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.age1    *" +  variables[pn][STAYATHOME]  + "*" +  a.age1); 
            logger.info("\tparameters[t][CHAGE1]  *  variables[pn][STAYATHOME]   +" +  parameters[t][CHAGE1]  + "*" +  variables[pn][STAYATHOME]);  
            logger.info("\ta.age25to35  +  parameters[t][CHAGE2]     *" +  a.age25to35  + "+" +  parameters[t][CHAGE2]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.age35to45   *" +  variables[pn][STAYATHOME]  + "*" +  a.age35to45);  
            logger.info("\tparameters[t][CHAGE3]  *  variables[pn][STAYATHOME]   +" +  parameters[t][CHAGE3]  + "*" +  variables[pn][STAYATHOME]);  
            logger.info("\ta.age45to55  +  parameters[t][CHAGE4]     *" +  a.age45to55  + "+" +  parameters[t][CHAGE4]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.age55to65   *" +  variables[pn][STAYATHOME]  + "*" +  a.age55to65);  
            logger.info("\tparameters[t][CHAGE5]  *  variables[pn][STAYATHOME]   +" +  parameters[t][CHAGE5]  + "*" +  variables[pn][STAYATHOME]);  
            logger.info("\ta.age65plus  +  parameters[t][CHOMELINC]    *" +  a.age65plus  + "+" +  parameters[t][CHOMELINC]); 
            logger.info("\tvariables[pn][STAYATHOME]  *  a.incLow    *" +  variables[pn][STAYATHOME]  + "*" +  a.incLow); 
            logger.info("\tparameters[t][CHOMEA0]  *  variables[pn][STAYATHOME]    +" +  parameters[t][CHOMEA0]  + "*" +  variables[pn][STAYATHOME]); 
            logger.info("\ta.autos0  +  parameters[t][CHOMEALTA]     *" +  a.autos0  + "+" +  parameters[t][CHOMEALTA]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.autosltadults   *" +  variables[pn][STAYATHOME]  + "*" +  a.autosltadults);  
            logger.info("\tparameters[t][CHOMENWA]     +" +  parameters[t][CHOMENWA]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.adultsgtworkers   *" +  variables[pn][STAYATHOME]  + "*" +  a.adultsgtworkers);  
            logger.info("\tparameters[t][CHOMEHH1]     +" +  parameters[t][CHOMEHH1]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType1    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType1); 
            logger.info("\tparameters[t][CHOMEHH2]     +" +  parameters[t][CHOMEHH2]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType2    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType2); 
            logger.info("\tparameters[t][CHOMEHH3]     +" +  parameters[t][CHOMEHH3]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType3    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType3); 
            logger.info("\tparameters[t][CHOMEHH4]     +" +  parameters[t][CHOMEHH4]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType4    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType4); 
            logger.info("\tparameters[t][CHOMEHH5]     +" +  parameters[t][CHOMEHH5]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType5    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType5); 
            logger.info("\tparameters[t][CHOMEHH6]     +" +  parameters[t][CHOMEHH6]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType6    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType6); 
            logger.info("\tparameters[t][CHOMEHH7]     +" +  parameters[t][CHOMEHH7]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType7    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType7); 
            logger.info("\tparameters[t][CHOMEHH8]     +" +  parameters[t][CHOMEHH8]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType8    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType8); 
            logger.info("\tparameters[t][CHOMEHH9]     +" +  parameters[t][CHOMEHH9]);  
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType9    *" +  variables[pn][STAYATHOME]  + "*" +  a.hType9); 
            logger.info("\tparameters[t][CHOMEHH10]    +" +  parameters[t][CHOMEHH10]);   
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType10     *" +  variables[pn][STAYATHOME]  + "*" +  a.hType10);  
            logger.info("\tparameters[t][CHOMEHH11]    +" +  parameters[t][CHOMEHH11]);   
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType11     *" +  variables[pn][STAYATHOME]  + "*" +  a.hType11);  
            logger.info("\tparameters[t][CHOMEHH12]    +" +  parameters[t][CHOMEHH12]);   
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType12     *" +  variables[pn][STAYATHOME]  + "*" +  a.hType12);  
            logger.info("\tparameters[t][CHOMEHH13]    +" +  parameters[t][CHOMEHH13]);   
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType13     *" +  variables[pn][STAYATHOME]  + "*" +  a.hType13);  
            logger.info("\tparameters[t][CHOMEHH14]    +" +  parameters[t][CHOMEHH14]);   
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType14     *" +  variables[pn][STAYATHOME]  + "*" +  a.hType14);  
            logger.info("\tparameters[t][CHOMEHH15]    +" +  parameters[t][CHOMEHH15]);   
            logger.info("\tvariables[pn][STAYATHOME]  *  a.hType15     *" +  variables[pn][STAYATHOME]  + "*" +  a.hType15);  
            logger.info("\tparameters[t][CDISTTR2]  *  variables[pn][NTOURS]   +" +  parameters[t][CDISTTR2]  + "*" +  variables[pn][NTOURS]);  
            logger.info("\ta.workDist1to2p5  +  parameters[t][CDISTTR3]    *" +  a.workDist1to2p5  + "+" +  parameters[t][CDISTTR3]); 
            logger.info("\tvariables[pn][NTOURS]  *  a.workDist2p5to5    *" +  variables[pn][NTOURS]  + "*" +  a.workDist2p5to5); 
            logger.info("\tparameters[t][CDISTTR4]  *  variables[pn][NTOURS]   +" +  parameters[t][CDISTTR4]  + "*" +  variables[pn][NTOURS]);  
            logger.info("\ta.workDist5to10  +  parameters[t][CDISTTR5]   *" +  a.workDist5to10  + "+" +  parameters[t][CDISTTR5]);  
            logger.info("\tvariables[pn][NTOURS]  *  a.workDist10to25    *" +  variables[pn][NTOURS]  + "*" +  a.workDist10to25); 
            logger.info("\tparameters[t][CDISTTR6]  *  variables[pn][NTOURS]   +" +  parameters[t][CDISTTR6]  + "*" +  variables[pn][NTOURS]);  
            logger.info("\ta.workDist25to50  +  parameters[t][CDISTTR7]    *" +  a.workDist25to50  + "+" +  parameters[t][CDISTTR7]); 
            logger.info("\tvariables[pn][NTOURS]  *  a.workDist50plus    *" +  variables[pn][NTOURS]  + "*" +  a.workDist50plus); 
            logger.info("\tparameters[t][CDISTST2]     +" +  parameters[t][CDISTST2]);  
            logger.info("\tvariables[pn][NUMBEROFSTOPSONWORK]    *" +  variables[pn][NUMBEROFSTOPSONWORK]);   
            logger.info("\ta.workDist1to2p5      *" +  a.workDist1to2p5);   
            logger.info("\tparameters[t][CDISTST3]     +" +  parameters[t][CDISTST3]);  
            logger.info("\tvariables[pn][NUMBEROFSTOPSONWORK]    *" +  variables[pn][NUMBEROFSTOPSONWORK]);   
            logger.info("\ta.workDist2p5to5      *" +  a.workDist2p5to5);   
            logger.info("\tparameters[t][CDISTST4]     +" +  parameters[t][CDISTST4]);  
            logger.info("\tvariables[pn][NUMBEROFSTOPSONWORK]    *" +  variables[pn][NUMBEROFSTOPSONWORK]);   
            logger.info("\ta.workDist5to10     *" +  a.workDist5to10);  
            logger.info("\tparameters[t][CDISTST5]     +" +  parameters[t][CDISTST5]);  
            logger.info("\tvariables[pn][NUMBEROFSTOPSONWORK]    *" +  variables[pn][NUMBEROFSTOPSONWORK]);   
            logger.info("\ta.workDist10to25      *" +  a.workDist10to25);   
            logger.info("\tparameters[t][CDISTST6]     +" +  parameters[t][CDISTST6]);  
            logger.info("\tvariables[pn][NUMBEROFSTOPSONWORK]    *" +  variables[pn][NUMBEROFSTOPSONWORK]);   
            logger.info("\ta.workDist25to50      *" +  a.workDist25to50);   
            logger.info("\tparameters[t][CDISTST7]     +" +  parameters[t][CDISTST7]);  
            logger.info("\tvariables[pn][NUMBEROFSTOPSONWORK]    *" +  variables[pn][NUMBEROFSTOPSONWORK]);   
            logger.info("\ta.workDist50plus      *" +  a.workDist50plus);   
            logger.info("\tparameters[t][CDISTSTA2]    +" +  parameters[t][CDISTSTA2]);   
            logger.info("\tvariables[pn][STOPS]  *  a.workDist1to2p5   *" +  variables[pn][STOPS]  + "*" +  a.workDist1to2p5);  
            logger.info("\tparameters[t][CDISTSTA3]  *  variables[pn][STOPS]   +" +  parameters[t][CDISTSTA3]  + "*" +  variables[pn][STOPS]);  
            logger.info("\ta.workDist2p5to5  +  parameters[t][CDISTSTA4]   *" +  a.workDist2p5to5  + "+" +  parameters[t][CDISTSTA4]);  
            logger.info("\tvariables[pn][STOPS]  *  a.workDist5to10    *" +  variables[pn][STOPS]  + "*" +  a.workDist5to10); 
            logger.info("\tparameters[t][CDISTSTA5]  *  variables[pn][STOPS]   +" +  parameters[t][CDISTSTA5]  + "*" +  variables[pn][STOPS]);  
            logger.info("\ta.workDist10to25  +  parameters[t][CDISTSTA6]   *" +  a.workDist10to25  + "+" +  parameters[t][CDISTSTA6]);  
            logger.info("\tvariables[pn][STOPS]  *  a.workDist25to50   *" +  variables[pn][STOPS]  + "*" +  a.workDist25to50);  
            logger.info("\tparameters[t][CDISTSTA7]  *  variables[pn][STOPS]   +" +  parameters[t][CDISTSTA7]  + "*" +  variables[pn][STOPS]);  
            logger.info("\ta.workDist50plus      *" + a.workDist50plus);
            logger.info("   + parameters[t][CNWRKTRS] * " + "variables[pn][WORK_TOURS]" + +parameters[t][CNWRKTRS] * variables[pn][WORK_TOURS]);
            logger.info("   + parameters[t][CNSCHTRS] * " + "variables[pn][SCHOOL_TOURS]" + +parameters[t][CNSCHTRS] * variables[pn][SCHOOL_TOURS]);
            logger.info("   + parameters[t][CNSHPTRS] * " + "variables[pn][SHOP_TOURS]" + +parameters[t][CNSHPTRS] * variables[pn][SHOP_TOURS]);
            logger.info("   + parameters[t][CNRECTRS] * " + "variables[pn][REC_TOURS]" + +parameters[t][CNRECTRS] * variables[pn][REC_TOURS]);
            logger.info("   + parameters[t][CNOTHTRS] * " + "variables[pn][OTHER_TOURS]" + +parameters[t][CNOTHTRS] * variables[pn][OTHER_TOURS]);            
            logger.info("\tparameters[t][CLSUMTOURSHP] * a.dcLogsumShop * variables[pn][SHOP_TOURS]   " 
                    + parameters[t][CLSUMTOURSHP] + "*" + a.dcLogsumShop + "*" + variables[pn][SHOP_TOURS]);
            logger.info("\tparameters[t][CLSUMTOURRECOTH] * a.dcLogsumRec * variables[pn][REC_TOURS]   " 
                    + parameters[t][CLSUMTOURRECOTH] + "*" + a.dcLogsumRec + "*" + variables[pn][REC_TOURS]);
            logger.info("\tparameters[t][CLSUMTOURRECOTH] * a.dcLogsumOther * variables[pn][OTHER_TOURS]   " 
                    + parameters[t][CLSUMTOURRECOTH] + "*" + a.dcLogsumOther + "*" + variables[pn][OTHER_TOURS]);    
            logger.info("\tparameters[t][CLSUMSTOPPERTOUR] * a.dcLogsumShop * variables[pn][SHOPSTOPSPERTOUR]   " 
                    + parameters[t][CLSUMSTOPPERTOUR] + "*" + a.dcLogsumShop + "*" + variables[pn][SHOPSTOPSPERTOUR]);
            logger.info("\tparameters[t][CLSUMSTOPPERTOUR] * a.dcLogsumRec * variables[pn][RECSTOPSPERTOUR]   " 
                    + parameters[t][CLSUMSTOPPERTOUR] + "*" + a.dcLogsumRec + "*" + variables[pn][RECSTOPSPERTOUR]);
            logger.info("\tparameters[t][CLSUMSTOPPERTOUR] * a.dcLogsumOther * variables[pn][OTHERSTOPSPERTOUR]   " 
                    + parameters[t][CLSUMSTOPPERTOUR] + "*" + a.dcLogsumOther + "*" + variables[pn][OTHERSTOPSPERTOUR]);
            logger.info("\tparameters[t][CLSUMCTOUR] * a.dcLogsumCollege * variables[pn][NTOURS]           " 
                    + parameters[t][CLSUMCTOUR] + "*" + a.dcLogsumCollege + "*" + variables[pn][NTOURS]           ); 
            logger.info("\tparameters[t][CLSUMCSPT]  * a.dcLogsumCollege * variables[pn][SHOPSTOPSPERTOUR] " 
                    + parameters[t][CLSUMCSPT]  + "*" + a.dcLogsumCollege + "*" + variables[pn][SHOPSTOPSPERTOUR] );                                                                    
            logger.info("\tparameters[t][CLSUMCSPT]  * a.dcLogsumCollege * variables[pn][RECSTOPSPERTOUR]  " 
                    + parameters[t][CLSUMCSPT]  + "*" + a.dcLogsumCollege + "*" + variables[pn][RECSTOPSPERTOUR]  ); 
            logger.info("\tparameters[t][CLSUMCSPT]  * a.dcLogsumCollege * variables[pn][OTHERSTOPSPERTOUR]" 
                    + parameters[t][CLSUMCSPT]  + "*" + a.dcLogsumCollege + "*" + variables[pn][OTHERSTOPSPERTOUR]);   

            if (parameters[t].length>CSCHSTOPSCHTOUR && variables[pn].length>PRESENCESCHOOLSTOPSONSCHOOLTOURS) {
                logger.info("\tparameters[t][CSCHSTOPSCHTOUR] * variables[pn][PRESENCESCHOOLSTOPSONSCHOOLTOURS]    "
                        + parameters[t][CSCHSTOPSCHTOUR] + "*" + variables[pn][PRESENCESCHOOLSTOPSONSCHOOLTOURS]); 
                logger.info("\tparameters[t][CWRKSTOPSCHTOUR] * variables[pn][PRESENCEWORKSTOPSONLYONSCHOOLTOURS]  "
                        + parameters[t][CWRKSTOPSCHTOUR] + "*" + variables[pn][PRESENCEWORKSTOPSONLYONSCHOOLTOURS]); 
                logger.info("\tparameters[t][CHOMEWORKHOME]   * variables[pn][WORKONLYNOSTOPS]                     "
                        + parameters[t][CHOMEWORKHOME] + "*" + variables[pn][WORKONLYNOSTOPS]);                                 
            }
        }

        return utility;

    }

    /**
     *  Convert a pattern to an array of empty tours given a pattern.
     * @param household Household
     * @param person PTPerson
     * @param pattern Pattern
     * @return Tour[] array of tours
     */
    public static Tour[] convertToTours(PTHousehold household, PTPerson person,
            Pattern pattern) {
        ArrayList<Tour> tours = new ArrayList<Tour>();
        Activity previous = null;
        String patternWord = pattern.toString().toLowerCase();

        // no tours -- stay at home all day
        if (patternWord.equals("h")) {
            return null;
        }
        
        // blank pattern or null pattern - write error
	if (patternWord.equals("")) {
	    logger.fatal("HHID: " + household.ID + " MemberID " + person.memberID
		    + " chose empty day pattern");
	    return null;
	}

        String[] strings = patternWord.split("h");

        for (int i = 0; i < strings.length; ++i) {
            String t = strings[i];
            Tour tour;
            ArrayList<Tour> wTours = null;

            // the first and last Strings are ""
            if (t.length() == 0) {
                continue;
            }

            if (t.indexOf("b") >= 0) {
                Tour wTour = new Tour("wow", null);

                wTour.begin.location.zoneNumber = person.workTaz;

                wTour.end.location.zoneNumber = person.workTaz;

                wTour.parentTourNumber=i-1;
                wTours = new ArrayList<Tour>();
                wTours.add(wTour);
                wTour.tourNumber = wTours.size();
            }

            tour = new Tour("h" + t + "h", previous);
            tour.tourNumber = i-1;
            tour.begin.location.zoneNumber = household.homeTaz;

            tour.end.location.zoneNumber = household.homeTaz;
            
            if (tour.primaryDestination.activityPurpose == ActivityPurpose.COLLEGE
                    && (person.personType == PersonType.STUDENTK12 || person.personType == PersonType.PRESCHOOL)) {
                tour.primaryDestination.activityPurpose = ActivityPurpose.GRADESCHOOL;
            }

            if (tour.getPurpose() == ActivityPurpose.WORK)
                tour.primaryDestination.location.zoneNumber = person.workTaz;

            previous = tour.end;

            tours.add(tour);

            if (wTours != null) {
                person.weekdayWorkBasedTours = wTours.toArray(new Tour[wTours
                        .size()]);
            }
        }

        if (tracer.isTracePerson(person.hhID + "_" + person.memberID)) {
            logger.info("Pattern: " + pattern);
            logger.info("New tours: " + tours.size());
        }

        return tours.toArray(new Tour[tours.size()]);
    }

    /**
     * Run as a stand alone.
     * @param args Runtime Arguments
     */
    public static void main(String[] args) {
        ResourceBundle rb = ResourceBundle.getBundle("sdt");
        PatternChoiceModel patternChoiceModel = new PatternChoiceModel(rb);
        patternChoiceModel.buildModel();
    }
}
