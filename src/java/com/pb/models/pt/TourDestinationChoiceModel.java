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
import com.pb.common.matrix.Matrix;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * This model implements a logit model to choose a tour destination
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TourDestinationChoiceModel extends TimedModel {

    private static Logger logger = Logger
            .getLogger(TourDestinationChoiceModel.class);

    LogitModel destinationModel;

    TazManager tazManager;

    Taz chosenTaz;

    Mode chosenMode;

    protected float[][] parameters;

    protected float[] purposeParams;

    TourDestinationPersonAttributes personAttributes = new TourDestinationPersonAttributes();

    boolean writtenOutTheUtilitiesAlready = false;
    boolean constrainByTimeAvailable = false;
    boolean trace = false;
    float distanceThreshold = 999;

    boolean calculateUtilities = true;

    PTHousehold currentHousehold;

    PTPerson currentPerson;

    Tour currentTour;
    
    private Tracer tracer = Tracer.getTracer();
    
//    private String debugPath;

    /** 
     * Default constructor.  Reads the model parameters file.
     * @param rb Resource Bundle
     */
    public TourDestinationChoiceModel(ResourceBundle rb) {
        startTiming();
        // read the tour destination parameters
        logger.debug("Reading TourDestinationModelParameters");
        String fileName = ResourceUtil.getProperty(rb,
                "sdt.tour.destination.parameters");
        try {
            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(
                    fileName));
            parameters = table.getValues();
        } catch (IOException e) {
            logger.fatal("Can't find TourDestinationParameters file "
                    + fileName);
            throw new RuntimeException(e);
        }
        
        distanceThreshold = Float.parseFloat(ResourceUtil.getProperty(rb,
                "ldt.threshold.distance.in.miles"));
        logger.info("Long distance threshold set at " + distanceThreshold);

//        debugPath = ResourceUtil.getProperty(rb, "debugFiles.path");
        endTiming();
    }

    /**
     * Adds TAZs in the TazManager to the logit destination
     * choice model.  Also computes size terms in each TAZ.
     * 
     * @param tazs  A set of tazs to add to the model.
     */
    public void buildModel(TazManager tazs) { // passsing in PTModel.tazs and
        // adding to logit model as
        // alternatives
        startTiming();

        tazManager = tazs;
        destinationModel = new LogitModel("destinationModel", tazs.size());
        Enumeration destinationEnum = tazs.elements();
        while (destinationEnum.hasMoreElements()) {
            Taz destinationTaz = (Taz) destinationEnum.nextElement();
            boolean trace = tracer.isTraceZone(destinationTaz.zoneNumber);
            destinationTaz.setName("" + destinationTaz.zoneNumber);
            destinationTaz.setTourSizeTerms(parameters,trace);
            destinationModel.addAlternative(destinationTaz);
        }
        endTiming();
    }

    /**
     * Calculate utilites for all TAZs and return logsum for origin TAZ.
     * 
     * @param household  Decision-makers household.
     * @param person     Decision-maker.
     * @param tour       Decision-makers tour.
     * @param logsumMatrix   Mode choice logsums for this person.
     * @param distanceMatrix Highway distance matrix.
     * @param timeMatrix     Highway time matrix.
     * @return The destination choice logsum from tour origin TAZ to all TAZs.
     */
    public double calculateUtility(PTHousehold household,
            PTPerson person, Tour tour, Matrix logsumMatrix,
            Matrix distanceMatrix, Matrix timeMatrix) {
        startTiming();
        ActivityPurpose purpose = tour.getPurpose();


        int originTazNumber = tour.begin.location.zoneNumber;
        Taz origin = tazManager.getTaz(originTazNumber); 

        trace = tracer.isTracePerson(person.hhID + "_" + person.memberID);

        currentHousehold = household;
        currentPerson = person;
        currentTour = tour;

        personAttributes.setAttributes(household, person, tour);

        if (purpose == ActivityPurpose.WORK || tour.primaryDestination.activityPurpose
        		==ActivityPurpose.WORK_BASED) {
            endTiming();
            return (double) -999;
        }

        float[] params;
        if (tour.begin.activityPurpose == ActivityPurpose.WORK ) {
            params = parameters[ActivityPurpose.WORK_BASED.ordinal()];
        } else {
            params = parameters[purpose.ordinal()];
        }

        // calculate available time window (in minutes)
        int availableTime = 0;

        if (constrainByTimeAvailable)
            availableTime = currentTour.calculateDurationHourly();

        if (trace) {
            logger.info("Calculating Destination Zone for the "
                    + "following tour...");
            logger.info("Distance threshold set to " + distanceThreshold);
            logger.info("HHID " + household.ID + ", Person " + person.memberID
                    + ", Tour " + tour.tourNumber + ", ActivityPurpose " + purpose
                    + ", Origin "+ tour.begin.location.zoneNumber);
        }

        int tazsWithinDistanceThreshold = 0;
        int tazsWithinTimeAvailable = 0;
        
        // cycle through zones and compute total exponentiated utility
        Enumeration tazEnum = tazManager.elements();
        for (int i = 0; i < tazManager.size(); i++) { 
            Taz destinationTaz = (Taz) tazEnum.nextElement();
             
            float mcLogsum = logsumMatrix.getValueAt(originTazNumber,
                    destinationTaz.zoneNumber);

            float distance = distanceMatrix.getValueAt(originTazNumber,
                    destinationTaz.zoneNumber);

            float time = timeMatrix.getValueAt(originTazNumber,
                    destinationTaz.zoneNumber);

            boolean available = true;

            if (constrainByTimeAvailable)
                if (time * 2 > availableTime && destinationTaz.zoneNumber != originTazNumber) {
                    available = false;
                }else
                	++tazsWithinTimeAvailable;

            if (distance > distanceThreshold && destinationTaz.zoneNumber != originTazNumber) {
                available = false;
            } else {
                ++tazsWithinDistanceThreshold;
            }

            // check if the destination is available
            if (available) {
                destinationTaz.setAvailability(true);
            } else
                destinationTaz.setAvailability(false);



            if (destinationTaz.isAvailable()) {
                destinationTaz.calcTourDestinationUtility(purpose, params,
                        mcLogsum, distance, personAttributes, trace, origin);
            }

            if(trace){
                logger.info("Taz " + destinationTaz.getZoneNumber() + " is available? " + destinationTaz.isAvailable());
                logger.info("Distance: " + distance + " MC Logsum: " + mcLogsum);
                logger.info("Taz " + destinationTaz.getZoneNumber() + " utility " + destinationTaz.getUtility());
                logger.info("Number of tazs within distance threshold: " + tazsWithinDistanceThreshold);
            }
        
        } // end destinations

        if (tazsWithinDistanceThreshold == 0) {
            logger.info("Error:  No tazs within distance threshold "
                    + distanceThreshold + " miles ");
            logger.info("for origin taz " + originTazNumber);
            throw new RuntimeException();
        }
        if (constrainByTimeAvailable && tazsWithinTimeAvailable == 0) {
            logger.info("Error:  No tazs within time available "
                    + availableTime + " minutes ");
            logger.info("for origin taz " + originTazNumber);
            throw new RuntimeException();
        }

        destinationModel.setDebug(trace);
        
        if(trace)
            destinationModel.writeUtilityHeader();
        
        double utility = destinationModel.getUtility(); 

        endTiming();
        return utility;
    }

    /**
     * Choose a TAZ according to the probabilities in the
     * model.  The calculateUtility() method
     * should be called before this method.
     * @param random Random number generator
     * @return  The chosen TAZ.
     */
    public Taz chooseZone(Random random) {
        startTiming();
        destinationModel.setDebug(trace);
        if(trace)
            destinationModel.writeProbabilityHeader();

        try {
            destinationModel.calculateProbabilities();
        } catch (Exception e) {
            logger.error("No alternatives were available: setting destination zone to home zone");
            chosenTaz = tazManager.tazData.get((int)currentPerson.homeTaz); 
        }
        try {
            chosenTaz = (Taz) destinationModel.chooseElementalAlternative(random);
        } catch (Exception e) {
            if (currentPerson == null) {
                logger.error("person is null");
            }
            if (currentHousehold == null) {
                logger.error("household is null");
            }
            if (currentTour == null) {
                logger.error("tour is null");
            }

//            logger.fatal("Unable to choose destination for "
//                    + currentHousehold.ID + ", " + currentPerson.ID);
            logger.error(e);
            logger.fatal(currentHousehold);
            logger.fatal(currentPerson);
            logger.fatal("Tour purpose "
                    + currentTour.getPurpose()
                    + " from home taz of " + currentPerson.homeTaz);

            throw new ModelException(e);
        }

        // set the primaryDestination zone number.
        currentTour.primaryDestination.location.zoneNumber = chosenTaz.zoneNumber;
        if(trace) logger.info("Chose TAZ "+chosenTaz.zoneNumber+" for trace "
        	+ currentTour.primaryDestination.activityPurpose+" tour ");

        endTiming();
        return chosenTaz;
    }

    public boolean isCalculateUtilities() {
        return calculateUtilities;
    }

    public void setCalculateUtilities(boolean calculateUtilities) {
        this.calculateUtilities = calculateUtilities;
    }

    public float getDistanceThreshold() {
        return distanceThreshold;
    }

    public void setDistanceThreshold(float distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }

    /**
     * Ensure that the model can be built.
     * @param args Runtime args
     */
    public static void main(String[] args) {
        ResourceBundle appRb = ResourceUtil.getResourceBundle("sdt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        String tazManagerClass = ResourceUtil.getProperty(appRb,"sdt.taz.manager.class");
            Class tazClass = null;
        TazManager tazManager;
            try {
                tazClass = Class.forName(tazManagerClass);
                tazManager = (TazManager) tazClass.newInstance();
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
                throw new RuntimeException(e);
            }
            tazManager.readData(globalRb, appRb);

        TourDestinationChoiceModel model = new TourDestinationChoiceModel(appRb);
        model.buildModel(tazManager);
        
        logger.info("All done building model.");
    }
}
