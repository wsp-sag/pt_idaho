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
 * Created on Nov 21, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.survey;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.matrix.BinaryMatrixWriter;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.ZipMatrixReader;
import com.pb.common.matrix.ExternalNumberIterator;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixWriter;
import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.util.ResourceUtil;

/**
 * Create interim MC Logsums.
 * 
 * @author Stryker
 * 
 */
public class MCLogsums {
    protected static Logger logger = Logger.getLogger(MCLogsums.class);

    protected ResourceBundle rb = ResourceUtil.getResourceBundle("pt");

    private boolean zipMatrices;

    private Matrix distance;

    private Matrix time;

    private Matrix toll;

    // TAZtoAMZ file will be read in if AMZ MClogsums are requested otherwise,
    // null
    private AlphaToBeta a2b;

    private int[] externalNumbers;

    private String period;

    private double ivtt;

    private double cost;

    private double driveCons;

    private double passConsASC;

    private double walkCons;

    private double bikeCons;

    private double passConsSize;

    private double dispersion;

    private double walkMax; // miles

    private double walkSpeed; // mph

    private double bikeMax;

    private double bikeSpeed;

    // private ArrayList<String> householdSizes;

    private ArrayList<String> incomeCategories;

    private ArrayList<String> autoSufficiency;

    private HashMap<String, String> purposes;

    /**
     * Constructor.
     * 
     * Initialize global data.
     */
    public MCLogsums(boolean zipMatrices) {
        this.zipMatrices = zipMatrices;

        // work based is handled differently
        purposes = new HashMap<String, String>();
        purposes.put("w", "work");
        purposes.put("c", "school");
        purposes.put("s", "shop");
        purposes.put("r", "recreate");
        purposes.put("o", "other");

        // market segmentation
        // householdSizes = new ArrayList<String>();
        // householdSizes.add("1");
        // householdSizes.add("2");
        // householdSizes.add("3p");

        incomeCategories = new ArrayList<String>();
        incomeCategories.add("low");
        incomeCategories.add("med");
        incomeCategories.add("high");

        autoSufficiency = new ArrayList<String>();
        autoSufficiency.add("0");
        autoSufficiency.add("i");
        autoSufficiency.add("s");
    }

    /**
     * Read the Zip matrix travel costs.
     * 
     * 
     * @param period
     *            Time period (congested or freeflow)
     */
    public void readCosts(String period) {
        this.period = period;
        String assign = rb.getString("assignDir");
        ZipMatrixReader reader;
        File fileName;

        logger.info("Reading travel costs in " + assign);

        logger.info("Reading distance matrix.  Units = miles");
        fileName = new File(assign + "car" + period + "Dist.zipMatrix");
        reader = new ZipMatrixReader(fileName);
        distance = reader.readMatrix();

        logger.info("Reading time matrix.  Units = minutes");
        fileName = new File(assign + "car" + period + "Time.zipMatrix");
        reader = new ZipMatrixReader(fileName);
        time = reader.readMatrix();

        logger.info("Reading toll matrix.  Units = cents");
        fileName = new File(assign + "car" + period + "Toll.zipMatrix");
        reader = new ZipMatrixReader(fileName);
        toll = reader.readMatrix();

        // set the external number array
        externalNumbers = distance.externalRowNumbers;

        logger.info("Finished reading travel costs.");
    }

    /**
     * Generically create logsums.
     */
    public Matrix calculateLogsums() {
        logger.info("Calculating logsums.");

        boolean trace = new Boolean(rb.getString("trace"));
        int itaz = 0;
        int jtaz = 0;

        if (trace) {
            itaz = new Integer(rb.getString("trace.itaz"));
            jtaz = new Integer(rb.getString("trace.jtaz"));
            logger
                    .info("Tracing calculations for (" + itaz + "," + jtaz
                            + ").");
        }

        int zones = externalNumbers.length - 1;
        Matrix logsum = new Matrix(zones, zones);
        logsum.setExternalNumbers(externalNumbers);

        ExternalNumberIterator rowIter = new ExternalNumberIterator(
                externalNumbers);

        while (rowIter.hasNext()) {
            int row = (Integer) rowIter.next();

            ExternalNumberIterator colIter = new ExternalNumberIterator(
                    externalNumbers);

            while (colIter.hasNext()) {
                int col = (Integer) colIter.next();

                double ds = distance.getValueAt(row, col);
                double tm = time.getValueAt(row, col);
                double tl = toll.getValueAt(row, col);

                if (trace && itaz == row && jtaz == col) {
                    logger.info("Distance: " + ds);
                    logger.info("Time: " + tm);
                    logger.info("Toll: " + tl);
                }

                // non-motorized
                double walkExpU = 0;
                if (ds < walkMax) {
                    double walkTime = ds / walkSpeed * 60;
                    walkExpU = Math.exp(ivtt * walkTime + walkCons);

                    if (trace && itaz == row && jtaz == col) {
                        logger.info("Walk time: " + walkTime);
                        logger.info("Walk expontetial utility: " + walkExpU);
                    }
                }

                double bikeExpU = 0;
                if (ds < bikeMax) {
                    double bikeTime = ds / bikeSpeed * 60;
                    bikeExpU = Math.exp(ivtt * bikeTime + bikeCons);

                    if (trace && itaz == row && jtaz == col) {
                        logger.info("Bike time: " + bikeTime);
                        logger.info("Bike expontetial utility: " + bikeExpU);
                    }
                }

                double nonMotorizedExpU = Math.exp(dispersion
                        * Math.log(walkExpU + bikeExpU));

                if (trace && itaz == row && jtaz == col) {
                    logger.info("Non-motorized expotential utility: "
                            + nonMotorizedExpU);
                }

                // auto-drive is an upper level choice
                double autoDriveExpU = Math.exp(ivtt * tm + cost * tl
                        + driveCons);

                if (trace && itaz == row && jtaz == col) {
                    logger.info("Auto driver expotential utility: "
                            + autoDriveExpU);
                }

                // auto-passenger is a lower level choice; the other passenger
                // choice are transit which is not handled yet
                double passExpU = Math.exp(dispersion
                        * (ivtt * tm + cost * tl + passConsASC + passConsSize));

                if (trace && itaz == row && jtaz == col) {
                    logger.info("Passenger expotential utility: " + passExpU);
                }

                // double passExpU= nesting * passAutoExpU;

                // we are not able to do transit yet

                // set the value;
                double lsum = Math
                        .log((nonMotorizedExpU + autoDriveExpU + passExpU));
                logsum.setValueAt(row, col, (float) lsum);

                if (trace && itaz == row && jtaz == col) {
                    logger.info("Final logsum: " + lsum);
                }
            }

        }

        return logsum;
    }

    /**
     * Convenience method that will only produce TAZ logsums. You must produce
     * TAZ logsums before you can produce AMZ logsums Convenience method that
     * will only produce TAZ logsums. You must produce TAZ logsums before you
     * can produce AMZ logsums
     */
    public void generateLogsums() {
        generateLogsums(false);
    }

    /**
     * Generate both TAZ and AMZ logsums.
     */
    public void generateLogsums(boolean AMZ) {

        if (AMZ) {
            logger.info("Generating TAZ and AMZ MC logsums.");
        } else {
            logger.info("Generating TAZ MC Logsums Only");
        }

        String outputDir = rb.getString("modeChoiceLogsumsWrite.path");

        // the parameters generic across all purposes
        double nesting = new Double(rb.getString("sdt.mc.nesting"));
        logger.info("Nesting parameter set to " + nesting);
        dispersion = 1 / nesting;

        walkMax = new Double(rb.getString("sdt.walk.max"));
        logger.info("Maximum walk distance " + walkMax + " miles.");
        walkSpeed = new Double(rb.getString("sdt.walk.speed"));
        logger.info("Walk speeds set to " + walkSpeed + " mph.");

        bikeMax = new Double(rb.getString("sdt.bike.max"));
        logger.info("Maximum bike distance " + bikeMax + " miles.");
        bikeSpeed = new Double(rb.getString("sdt.bike.speed"));
        logger.info("Bike speeds set to " + bikeSpeed + " mph.");

        Iterator<String> purposeIter = purposes.keySet().iterator();
        while (purposeIter.hasNext()) {
            String code = purposeIter.next();
            String purpose = purposes.get(code);

            logger.info("Setting parameters for " + purpose);

            // purpose specific parameters
            ivtt = new Double(rb.getString("sdt." + purpose + ".ivtt"));
            logger.info("ivtt parameter set to " + ivtt);

            logger.info("Iterating over market segments.");

            // track the market segment number
            int seg = 0;

            // the passenger parameter is specific to household sizes
            // Iterator<String> householdSizeIter = householdSizes.iterator();
            // while (householdSizeIter.hasNext()) {
            // String size = householdSizeIter.next();
            //
            // passConsSize = new Double(rb.getString("sdt." + purpose
            // + ".pass." + size));
            // logger.info("Household size " + size
            // + " passenger constant set to " + passConsSize);

            // the cost parameters are income specfic
            Iterator<String> incomeIter = incomeCategories.iterator();
            while (incomeIter.hasNext()) {
                String income = incomeIter.next();

                cost = new Double(rb.getString("sdt." + purpose + ".cost."
                        + income));
                logger.info("Income level " + income
                        + " cost parameter set to " + cost);

                // ASCs are specific to auto sufficiency
                Iterator<String> autoSuffIter = autoSufficiency.iterator();
                while (autoSuffIter.hasNext()) {
                    String sufficiency = autoSuffIter.next();

                    driveCons = new Double(rb.getString("sdt." + purpose
                            + ".drive." + sufficiency));
                    logger.info("Drive ASC for auto sufficiency level '"
                            + sufficiency + "' set to " + driveCons);

                    passConsASC = new Double(rb.getString("sdt." + purpose
                            + ".pass." + sufficiency));
                    logger.info("Passenger ASC for auto sufficiency level '"
                            + sufficiency + "' set to " + passConsASC);

                    walkCons = new Double(rb.getString("sdt." + purpose
                            + ".walk." + sufficiency));
                    logger.info("Walk ASC for auto sufficiency level '"
                            + sufficiency + "' set to " + walkCons);

                    bikeCons = new Double(rb.getString("sdt." + purpose
                            + ".bike." + sufficiency));
                    logger.info("Bike ASC for auto sufficiency level '"
                            + sufficiency + "' set to " + bikeCons);

                    Matrix lgsms = calculateLogsums();
                    lgsms.setName("logsum");

                    if (zipMatrices) {
                    	logger.info("reached here");
                    	String fileName = outputDir + code + seg + "mcls.zmx";

                        logger.info("Writing matrix to " + fileName);

                        File file = new File(fileName);
                        ZipMatrixWriter zmw = new ZipMatrixWriter(file);
                        zmw.writeMatrix(lgsms);

                        if (AMZ) {
                            String amzFileName = outputDir + code + seg + "mclsAMZ.zmx";
                            squeezeAndWriteMatrix(lgsms, amzFileName);
                        }

                    } else {
                        String fileName = outputDir + code + seg + "mcls.bin";

                        logger.info("Writing matrix to " + fileName);

                        File file = new File(fileName);
                        BinaryMatrixWriter bmw = new BinaryMatrixWriter(file);
                        bmw.writeMatrix(lgsms);
                    }

                    seg += 1;
                }
                // not using household size
                // }
            }

        }

        // work-based is special
        ivtt = new Double(rb.getString("sdt.workbased.ivtt"));
        driveCons = new Double(rb.getString("sdt.workbased.drive"));
        logger.info("Drive ASC set to " + driveCons);

        passConsASC = new Double(rb.getString("sdt.workbased.pass"));
        logger.info("Passenger ASC set to " + passConsASC);

        walkCons = new Double(rb.getString("sdt.workbased.walk"));
        logger.info("Walk ASC set to " + walkCons);

        bikeCons = new Double(rb.getString("sdt.workbased.bike"));
        logger.info("Bike ASC set to " + bikeCons);

        int seg = 0;

        Iterator<String> incomeIter = incomeCategories.iterator();
        while (incomeIter.hasNext()) {
            String income = incomeIter.next();

            cost = new Double(rb.getString("sdt.workbased.cost." + income));

            Matrix lgsms = calculateLogsums();

            if (zipMatrices) {
            	logger.info("reached here");
                String fileName = outputDir + "b" + seg + "mcls.zmx";
                File file = new File(fileName);

                logger.info("Writing matrix to " + fileName);
                ZipMatrixWriter zmw = new ZipMatrixWriter(file);
                zmw.writeMatrix(lgsms);

                if (AMZ) {
                    String amzFileName = outputDir + "b" + seg + "mclsAMZ.zmx";
                    squeezeAndWriteMatrix(lgsms, amzFileName);
                }

            } else {
                String fileName = outputDir + "/workbased/" + period + "/"
                        + income + "binary";

                logger.info("Writing matrix to " + fileName);

                File file = new File(fileName);
                BinaryMatrixWriter bmw = new BinaryMatrixWriter(file);
                bmw.writeMatrix(lgsms);
            }

            seg += 1;
        }

        logger.info("Finished generating logsums.");
    }

    private void squeezeAndWriteMatrix(Matrix m, String fileName) {
        if (a2b == null) {
            a2b = new AlphaToBeta(new File(rb.getString("tazToAmz.file")),
                    "TAZ", "AMZ");
        }
        MatrixCompression compressHelper = new MatrixCompression(a2b);
        Matrix betaLogsum = compressHelper.getCompressedMatrix(m, "MEAN");

        logger.info("Writing squeezed matrix to " + fileName);
        ZipMatrixWriter zmw = new ZipMatrixWriter(new File(fileName));
        zmw.writeMatrix(betaLogsum);
    }

    /**
     * Generate interim MC logsums.
     * 
     * Usage: java com.pb.models.pt.MCLogsums [-binary] <Pk | Op>...
     */
    public static void main(String[] args) {
        boolean zipMatrices = true;
        String period = null;

        if(args.length == 2){
            if (args[0].equals("-binary")) {
                zipMatrices = false;
                period = args[1];
            }else {
                System.out.println("If you are passing in two arguments, your" +
                        " first argument must be the '-binary' flag " +
                        "\nand your second argument must be the period ('Pk' or 'Op')");
            }
        }else if(args.length == 1) {
            period = args[0];
        }else{
            System.out.println("ERROR: TIME PERIOD NOT SPECIFIED." +
                    "\nYou can pass one or two arguments to this main method." +
                    "\nIf you want binary instead of zip matrices your first argument should" +
                    "\nbe a '-binary' flag.  Your next argument must be the skim time period" +
                    "\nthat you will be using.  If you  pass in a single argument" +
                    "\n('Pk' or 'Op') by default you will get zip matrices back");
        }


        MCLogsums mcl = new MCLogsums(zipMatrices);
        mcl.readCosts(period);
        if (zipMatrices) {
            // the 'true' argument means "Write the AMZ logsums as well as
            // the TAZ logsums.
            mcl.generateLogsums(true);
        } else {
            mcl.generateLogsums();  //will write only the TAZ logsums
        }


        MCLogsums.logger.info("All done.");
    }
}
