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

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import static com.pb.common.math.MathUtil.exp;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.model.ConcreteAlternative;
import com.pb.common.model.LogitModel;
import com.pb.common.util.ResourceUtil;
import static com.pb.models.pt.AutoOwnershipModelParameters.*;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import static java.lang.Math.log;
import java.util.Random;
import java.util.ResourceBundle;

/** This class applies an auto ownership model
 *  to households in PT
 * @author Joel Freedman
 *  modified by Greg Erhardt to implement correct OSMP model parameters
 * @date 3.7.2006
 */
public class AutoOwnershipModel {
	transient static Logger logger = Logger.getLogger(AutoOwnershipModel.class);

    protected ResourceBundle rb;

    private Tracer tracer = Tracer.getTracer();

    private boolean trace = false;

    private ColumnVector accessibility;

    private float[][] parameters;

    protected LogitModel root;

    ConcreteAlternative[] alts;

    private long fixedSeed = 1965;
    
    /**
     * Constructor
     * @param rb Resource Bundle
     * @param alphaName The name given to the TAZ unit (AZONE, AlphaZone, etc)
     */
    public AutoOwnershipModel(ResourceBundle rb, String alphaName) {
        this.rb = rb;

        tracer.readTraceSettings(rb);
        readParameters();
    }

	/**
     * Constructor
     * @param rb Resource Bundle
     * @param time Time Matrix
     * @param distance Distance matrix
     */
    public AutoOwnershipModel(ResourceBundle rb, Matrix time, Matrix distance, String alphaName) {
        this(rb, alphaName);
        createDCAccessibility(time, distance, alphaName);
        
        /*
        logger.info("*** accessibilities are ***");
        for (int i = 0; i < accessibility.getRowCount(); i++) {
        	logger.info("i " + i +  ", accessibility " + accessibility.getValueAt(i));
        }
        */
    }

    /**
     * Create a DC Accessibility measure.
     *
     * The method uses last year's employment and the peak period auto distance.
     * @param time Time matrix
     * @param distance Distance matrix
     * @param alphaName name of zone field in file
     *
     */
    private void createDCAccessibility(Matrix time, Matrix distance, String alphaName) {

        // read employment
        CSVFileReader reader = new CSVFileReader();
        String previousEmp = ResourceUtil.getProperty(rb, "sdt.employment");
        logger.info(previousEmp);
        File file = new File(previousEmp);
        logger.info("Reading employment in " + file);
        TableDataSet table;
        try {
            table = reader.readFile(file);
        } catch (IOException e) {
            String msg = "Unable to read the employment.";
            logger.fatal(msg);
            throw new RuntimeException(msg, e);
        }

        int[] extNumbers = distance.getExternalNumbers();
        double distParam = Double.parseDouble(ResourceUtil.getProperty(rb, "sdt.auto.ownership.distance.parameter"));
        double timeParam = Double.parseDouble(ResourceUtil.getProperty(rb, "sdt.auto.ownership.time.parameter"));
        table.buildIndex(table.getColumnPosition(alphaName));

        // set-up the DC logsum like accessibility measure
        accessibility = new ColumnVector(distance.getRowCount());
        accessibility.setExternalNumbers(distance.getExternalNumbers());

        for (int c = 1; c <= distance.getColumnCount(); ++c) {
            int itaz = extNumbers[c];
            double logsum = 0;

            for (int r = 1; r <= distance.getColumnCount(); ++r) {
                int jtaz = extNumbers[r];
                float totalEmp;
                try{
                    totalEmp = table.getIndexedValueAt(jtaz, "TotEmp");
                } catch (Exception e){
                    totalEmp = 0;
                }
                logsum += totalEmp
                        * exp(distParam * distance.getValueAt(itaz, jtaz)
                                + timeParam * time.getValueAt(itaz, jtaz));
            }

            logsum = log(logsum);

            accessibility.setValueAt(itaz, (float) logsum);
        }

    }

    /**
     * Manually set the parameter array.
     *
     * This method is intended to factilitate testing.
     * @param parameters auto-ownership parameters
     */
    public void setParameters(float[][] parameters) {
        this.parameters = parameters;
    }

    /**
     * Return the parameter array.
     *
     * This method is intended to factilitate testing.
     * @return float[][] Auto Ownership parameters
     */
    public float[][] getParameters() {
        return parameters;
    }

    /**
     * Read parameters from file specified in PT properties.
     */
    private void readParameters() {
        String fileName = ResourceUtil.getProperty(rb,
                "sdt.auto.ownership.parameters");

        logger.info("Reading auto ownership parameters in " + fileName);

        try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(fileName));
            parameters = table.getValues();
        } catch (IOException e) {
            logger.fatal("Can not find auto ownership parameters file "
                    + fileName);
            logger.fatal(e);
            throw new RuntimeException(e);
        }

        logger.info("Parameters is " + parameters.length + " X "
                + parameters[0].length);

        if (!tracer.isTraceOn()) {
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
    }

    /**
     * Build the model.
     *
     * Since the alternatives are simple and pretty much describe themselves,
     * they are instances of a ConcreteAlternative class.
     */
    public void buildModel() {
        root = new LogitModel("Auto ownership model");

        alts = new ConcreteAlternative[ALTERNATIVES];
        for(int i=0;i<alts.length;++i){
            alts[i] = new ConcreteAlternative(
            		"autos"+i,	i);
            root.addAlternative(alts[i]);
         }
    }

    /**
     * Calculate the utility for each alternative.
     *
     * @param hh               the PTHousehold under consideration.
     * @return the composite utility.
     */
    public double calculateUtility(PTHousehold hh) {
        // use dummy DC logsum	
    	//logger.info("*** home taz " + hh.homeTaz);
    	double dclogsum = accessibility.getValueAt(hh.homeTaz);

        // parking cost is not included in generic model
		double parkingCost = 0;

		return calculateUtility(hh, dclogsum, parkingCost);
    }

    /**
     * Calculate the utility for each alternative.
     *
     * @param hh        the PTHousehold under consideration.
     * @param dclogsum  the destination choice logsum for the home TAZ.
     * @return the composite utility.
     */
    public double calculateUtility(PTHousehold hh, double dclogsum, double dailyParkingCost) {

        trace = tracer.isTraceHousehold(hh.ID);
		if (trace) {
            logger.info("Tracing auto ownership utility calculation for "
                    + "household " + hh.ID);
        }
		root.setDebug(trace);

        // Loop through each alterantive, and set the utility for each variable
        double utility[] = new double[ALTERNATIVES];
        for (int i = 0; i < ALTERNATIVES; i++) {

            utility[i] = 0;

            // constant
            utility[i] += parameters[i][CONSTANT];
            if (tracer.isTraceHousehold(hh.ID)) {
                logger.info("  Constant for " + i + " auto: "
                        + parameters[i][CONSTANT]);
            }

            // household size 1
            if (hh.size == 1) {
                utility[i] += parameters[i][HHSIZE1];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("  HH Size 1 utility for " + i + " auto: "
                            + parameters[i][HHSIZE1]);
                }
            }

            // household size 2
            if (hh.size == 2) {
                utility[i] += parameters[i][HHSIZE2];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("  HH Size 2 utility for " + i + " auto: "
                            + parameters[i][HHSIZE2]);
                }
            }

            // household size 3+
            if (hh.size >= 3) {
                utility[i] += parameters[i][HHSIZE3];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("  HH Size 3+ utility for " + i + " auto: "
                            + parameters[i][HHSIZE3]);
                }
            }

            // low income
            if (IncomeSegmenter.getIncomeCategory(hh.getIncome()) == IncomeSegmenter.IncomeCategory.INCOME_LOW) {
                utility[i] += parameters[i][INCOME1];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("  Low income utility for " + i + " auto: "
                            + parameters[i][INCOME1]);
                }
            }

            // low-mid income
            if (IncomeSegmenter.getIncomeCategory(hh.getIncome())== IncomeSegmenter.IncomeCategory.INCOME_MEDIUM_LOW) {
                utility[i] += parameters[i][INCOME2];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("  Low-mid income utility for " + i
                            + " auto: " + parameters[i][INCOME2]);
                }
            }

            // mid-high income
            if (IncomeSegmenter.getIncomeCategory(hh.getIncome()) == IncomeSegmenter.IncomeCategory.INCOME_MEDIUM_HIGH) {
                utility[i] += parameters[i][INCOME3];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("  Mid-high income utility for " + i
                            + " auto: " + parameters[i][INCOME3]);
                }
            }

            // high income
            if (IncomeSegmenter.getIncomeCategory(hh.getIncome()) == IncomeSegmenter.IncomeCategory.INCOME_HIGH) {
                utility[i] += parameters[i][INCOME4];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("  High income utility for " + i + " auto: "
                            + parameters[i][INCOME4]);
                }
            }

            // 0 workers
            if (hh.workers==0) {
                utility[i] += parameters[i][WORKERS0];
                if (tracer.isTraceHousehold(hh.ID)) {
                	logger.info("  Workers 0 utility for "+i+" auto: "
                			+ parameters[i][WORKERS0]);
                }
            }

            // 1 workers
            if (hh.workers==1) {
                utility[i] += parameters[i][WORKERS1];
                if (tracer.isTraceHousehold(hh.ID)) {
                	logger.info("  Workers 1 utility for "+i+" auto: "
                			+ parameters[i][WORKERS1]);
                }
            }

            // 2 workers
            if (hh.workers==2) {
                utility[i] += parameters[i][WORKERS2];
                if (tracer.isTraceHousehold(hh.ID)) {
                	logger.info("  Workers 2 utility for "+i+" auto: "
                			+ parameters[i][WORKERS2]);
                }
            }

            // 3+ workers
            if (hh.workers>=3) {
                utility[i] += parameters[i][WORKERS3];
                if (tracer.isTraceHousehold(hh.ID)) {
                	logger.info("  Workers 3 utility for "+i+" auto: "
                			+ parameters[i][WORKERS3]);
                }
            }

    		//the destination choice logsum
    		utility[i] += dclogsum*parameters[i][DCLOGSUM];
    		if (tracer.isTraceHousehold(hh.ID)) {
    			logger.info("    The home TAZ is:                  " + hh.homeTaz);
    			logger.info("    The destination choice logsum is: " + dclogsum);
    			logger.info("  DC logsum utility for "+i+" auto: "
    					+ dclogsum*parameters[i][DCLOGSUM]);
    		}

            // daily parking cost, only if non-zero
            if (dailyParkingCost > 0) {
                utility[i] += dailyParkingCost*parameters[i][DAYPARK];
                if (tracer.isTraceHousehold(hh.ID)) {
                    logger.info("    The home TAZ is:                  " + hh.homeTaz);
                    logger.info("    The daily parking cost is       : " + dclogsum);
                    logger.info("  Parking cost utility for "+i+" auto: "
                            + dailyParkingCost*parameters[i][DAYPARK]);
                }
            }

    		// set the utility
    		alts[i].setUtility(utility[i]);
    		if (tracer.isTraceHousehold(hh.ID)) {
    			logger.info("  Total utility for "+i+" auto: "+utility[i]);
    		}
    	}

        if (trace) {
            root.writeUtilityHeader();
        }

        double compUtility = root.getUtility();

        if (tracer.isTraceHousehold(hh.ID)) {
            logger.info("  Composite utility: " + compUtility);
        }

        return compUtility;
    }

    /**
     * Using the utilities computed with the calculate utilities method, use
     * Monte Carlo simulation to choose a household auto ownership level.
     *
     * @return auto ownership level (0, 1, 2, or 3+)
     */
    public int chooseAutoOwnership() {
        int autos;

        if (trace) {
            root.writeProbabilityHeader();
        }
        root.calculateProbabilities();

        try {
        	autos = (Integer) ((ConcreteAlternative) root
                    .chooseAlternative()).getAlternative();
        } catch (Exception e) {
            logger.error("Caught exception while choosing auto ownership.");
            throw new RuntimeException(e);
        }

        if (trace) {
            logger.info("Choose auto ownership " + autos);
        }

        return autos;
    }

    /**
     * Using the utilities computed with the calculate utilities method, use
     * Monte Carlo simulation to choose a household auto ownership level.
     *
     * @param randomRandom If the user wants the sequence of random numbers to
     * vary from run to run, they will set this variable to true and the random
     * number seed will depend on the system time and therefore be variable.  If the
     * user sets this value to false, then the random number generator will be seeded with
     * a fixed model seed + a unique household level seed and so the decision should be the same
     * each time.
     * @param hhSeed This is a value that is unique to the decision maker (in this case the HH)
     *
     * @return auto ownership level (0, 1, 2, or 3+)
     */
    public int chooseAutoOwnershipWithRandomSeedControl(boolean randomRandom, long hhSeed) {
        int autos;

        Random random;
        if(randomRandom)
            random = new Random(fixedSeed + System.currentTimeMillis() + hhSeed);
        else
            random = new Random(fixedSeed + hhSeed);

        if (trace) {
            root.writeProbabilityHeader();
        }
        root.calculateProbabilities();

        try {
        	autos = (Integer) ((ConcreteAlternative) root
                    .chooseAlternative(random)).getAlternative();
        } catch (Exception e) {
            logger.error("Caught exception while choosing auto ownership.");
            throw new RuntimeException(e);
        }

        if (trace) {
            logger.info("Choose auto ownership " + autos);
        }

        return autos;
    }

    public void setFixedSeed(long fixedSeed) {
        this.fixedSeed = fixedSeed;
    }
}
