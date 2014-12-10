package com.pb.models.pt.ldt;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ResourceBundle;

/**
 *
 * This class is used to run the long-distance travel models as a
 * stand-alone component.  Note that in most applications, the LDT models
 * are run as part of the DAF framework.  This class is provided primarily
 * for calibration.
 *
 * @author Erhardt
 * @version 1.0 May 9, 2006
 *
 */
public class RunLDTModels {
    protected static Logger logger = Logger.getLogger(RunLDTModels.class);
    PTOccupationReferencer occRef;

    protected static TazManager tazManager;
    protected ResourceBundle appRb;
    protected ResourceBundle globalRb;

    // Models used
//    protected LDSchedulingModel ldSchedulingModel;
//    protected LDInternalExternalModel ldInternalExternalModel;
//    protected LDInternalModeChoiceModel ldInternalModeChoiceModel;
//    protected LDInternalDestinationChoiceModel ldInternalDestinationChoiceModel;
//    protected LDExternalModeChoiceModel ldExternalModeChoiceModel;
//    protected LDExternalDestinationModel ldExternalDestinationModel;
//    protected LDAutoDetailsModel ldAutoDetailsModel;

    /**
     * Need to have a referencer that can parse the string occuption
     * to an index number.
     *
     */
    public RunLDTModels(PTOccupationReferencer occRef) {
        this.occRef = occRef;

    }


    /**
     * Checks whether or not a household engages in long-distance travel on the
     * travel day.
     *
     * @param household
     *            The household of interest.
     * @return A boolean flag indicating the presense of any long-distance
     *         travel in the household on the travel day.
     */
    public static boolean checkHouseholdForLongDistanceTravel(PTHousehold household) {
        boolean ldTravel = false;
        for (PTPerson person : household.persons) {
            for (int i = 0; i < person.ldTourPattern.length; i++) {
                if ((person.ldTourPattern[i].equals(LDTourPatternType.BEGIN_TOUR)
                    || person.ldTourPattern[i].equals(LDTourPatternType.END_TOUR)
                    || person.ldTourPattern[i].equals(LDTourPatternType.COMPLETE_TOUR))) {
                    ldTravel = true;
                    break;
                }
            }
        }
        return ldTravel;
    }



    /**
     * Creates a new LDTour object each time tour occurs.  Returns an array of
     * all LDTours that happen.
     *
     * @param households An array of households with binary and pattern fields set.
     * @return An array of long-distance tour objects created where relevant.
     */
    public static LDTour[] createLDTours(PTHousehold[] households) {

        logger.info("Creating new LDT tours");
        int id = 0;
        ArrayList<LDTour> tours = new ArrayList<LDTour>();

        for (PTHousehold hh : households) {
            if (hh == null) {
                continue;
            }
            if (hh.ldHouseholdTourPattern.equals(LDTourPatternType.BEGIN_TOUR)
                    || hh.ldHouseholdTourPattern.equals(LDTourPatternType.END_TOUR)
                    || hh.ldHouseholdTourPattern.equals(LDTourPatternType.COMPLETE_TOUR)) {
                LDTour t = new LDTour(id++, hh, hh.persons[0], LDTourPurpose.HOUSEHOLD, hh.ldHouseholdTourPattern);
                tours.add(t);
            }

            for (PTPerson p : hh.persons) {
                for (int k=0; k<LDTourPurpose.values().length; k++) {
                    if (k != LDTourPurpose.HOUSEHOLD.ordinal()) {
                        if (p.ldTourPattern[k].equals(LDTourPatternType.BEGIN_TOUR)
                                || p.ldTourPattern[k].equals(LDTourPatternType.END_TOUR)
                                || p.ldTourPattern[k].equals(LDTourPatternType.COMPLETE_TOUR)) {
                            LDTour t = new LDTour(id++, hh, p, LDTourPurpose.values()[k], p.ldTourPattern[k]);
                            tours.add(t);
                        }
                    }
                }
            }
        }

        logger.info("Created " + tours.size() + " LDT tours.");

        if(tours.size() > 0) {
            LDTour[] tourArray = new LDTour[tours.size()];
            tourArray = tours.toArray(tourArray);
            return tourArray;
        }else return null;
    }


    /**
     * Creates trip objects out of the tours.
     *
     * @param tours An array of tours.
     * @return      A corresponding array of trips.
     */
    public static LDTrip[] createTrips(LDTour[] tours) {

        logger.info("Creating trips from tours.  Starting with " + tours.length + " tours.");
        ArrayList<LDTrip> trips = new ArrayList<LDTrip>(tours.length);

        for (LDTour tour : tours) {
            if (tour.patternType.equals(LDTourPatternType.BEGIN_TOUR)) {
                LDTrip trip = new LDTrip(tour, LDTourPatternType.BEGIN_TOUR);
                trips.add(trip);
            }
            else if (tour.patternType.equals(LDTourPatternType.END_TOUR)) {
                LDTrip trip = new LDTrip(tour, LDTourPatternType.END_TOUR);
                trips.add(trip);
            }
            else if (tour.patternType.equals(LDTourPatternType.COMPLETE_TOUR)) {
                LDTrip outboundTrip = new LDTrip(tour, LDTourPatternType.BEGIN_TOUR);
                trips.add(outboundTrip);
                LDTrip inboundTrip = new LDTrip(tour, LDTourPatternType.END_TOUR);
                trips.add(inboundTrip);
            }
        }
        LDTrip[] tripsArray = new LDTrip[trips.size()];
        for (int i=0; i<trips.size(); i++) {
            tripsArray[i] = trips.get(i);
        }
        logger.info("  Created " + tripsArray.length + " trips.");

        return tripsArray;
    }

//    public void startModel(int baseYear,int timeInterval){
//        // set-up the Trace object
//        Tracer tracer = Tracer.getTracer();
//        tracer.readTraceSettings(appRb);
//        
//        // initialize the skims in memory
//        LDSkimsInMemory LDSkims = LDSkimsInMemory.getInstance();
//        LDSkims.readSkimsIntoMemory(globalRb, appRb);
//
//        // initialize the models
//        ldSchedulingModel = new LDSchedulingModel(appRb);
//
//        ldInternalExternalModel = new LDInternalExternalModel(appRb, tazManager);
//
//        ldInternalModeChoiceModel = new LDInternalModeChoiceModel(globalRb, appRb, tazManager);
//
//        ldInternalDestinationChoiceModel = new LDInternalDestinationChoiceModel(
//                globalRb, appRb, tazManager, ldInternalModeChoiceModel);
//
//        ldExternalModeChoiceModel = LDExternalModeChoiceModel.newInstance(globalRb, appRb);
//
//        ldExternalDestinationModel = LDExternalDestinationModel.newInstance(globalRb, appRb);
//
//        ldAutoDetailsModel = new LDAutoDetailsModel(appRb);
//
//    }
    
//    public void processHouseholds(PTHousehold[] households) {
//        // read households and create tours
//        LDTour[] tours = createLDTours(households);
//
//        int i=1; 
//        for (LDTour tour : tours) {
//            if (i < 6 || i % 1000 == 0) logger.info("LDT tour " + i);
//
//            tour.schedule = ldSchedulingModel.chooseSchedule(tour);
//            tour.destinationType = ldInternalExternalModel.chooseInternalExternal(tour);
//
//            if (tour.destinationType == LDTourDestinationType.EXTERNAL) {
//                tour.destinationTAZ = ldExternalDestinationModel.chooseTaz(tour);
//                tour.distance = ldExternalModeChoiceModel.getDistance(tour);    
//                
//                // apply the correct model depending on the destination
//                tour.modeChoiceHaloFlag = ldExternalDestinationModel.isDestinationInModeChoiceHalo(tour);
//                if (tour.modeChoiceHaloFlag==true) {
//                    tour.mode = ldInternalModeChoiceModel.chooseMode(tour);
//                    tour.outboundTime = ldInternalModeChoiceModel.getOutboundTravelTime(tour);
//                    tour.inboundTime  = ldInternalModeChoiceModel.getInboundTravelTime(tour);                    
//                } else {
//                    tour.mode = ldExternalModeChoiceModel.chooseMode(tour);                
//                }
//            } else {
//                tour.destinationTAZ = ldInternalDestinationChoiceModel.chooseTaz(tour);
//                tour.distance = ldInternalDestinationChoiceModel.getDistance(tour);
//                tour.mode = ldInternalModeChoiceModel.chooseMode(tour);
//                tour.outboundTime = ldInternalModeChoiceModel.getOutboundTravelTime(tour);
//                tour.inboundTime = ldInternalModeChoiceModel.getInboundTravelTime(tour);
//            }
//            tour.tripMode = ldAutoDetailsModel.chooseTripMode(tour); 
//            tour.nearestAirport = ldAutoDetailsModel.chooseAirportTaz(tour);
//            
//            i++;
//        }
//
//        logger.info("LDT finished processing " + households.length + " households.");
//        writeLongDistanceResults(tours, appRb, globalRb);
//    }
  

    public static void writeLongDistanceResults(LDTour[] tours, ResourceBundle appRb, ResourceBundle globalRb) {

        LDTrip[] trips = createTrips(tours);

        LDTDataWriter writer = new LDTDataWriter(appRb, globalRb);
        writer.writePersonTours(tours);
        writer.writePersonTrips(trips);
        writer.writeAssignmentTrips(trips);

        writer.close();

        // write a summary to the logger
        LDTReporter reporter = new LDTReporter(appRb);
        reporter.countTourLevelDecisions(tours, trips);
        reporter.logTourLevelDecisions();
    }
    
    
    /**************************************************************************
     * 
     * The private methods below are used for calbiration runs of LDT only.
     * 
     **************************************************************************/
    
    
    /**
     * Reads the households using the PTDataReader.
     *
     * @return An array of household objects.
     */
    private PTHousehold[] readHouseholdsAndPersons(int baseYear){

        int startRow = 2; 
        int endRow = Integer.MAX_VALUE -1;
        int ptSampleRate = ResourceUtil.getIntegerProperty(globalRb, "pt.sample.rate");

        PTDataReader reader = new PTDataReader(appRb, globalRb, occRef, baseYear); 

        reader.createRandomNumberGenerator();        
        reader.openPersonFile(); 
        reader.readPersonHeader(); 
        
        PTHousehold[] hhs = reader.readHouseholds(startRow, endRow, ptSampleRate);

        logger.info("Attaching persons to households"); 
        for(PTHousehold hh : hhs){

            //Find persons in the hh (will check for the existence of the workTaz
            hh.persons = reader.readPersonsForTravelModels(hh.size, hh.ID);
            for(PTPerson person : hh.persons){
                person.homeTaz = hh.homeTaz;
            }
        }
        reader.closePersonFile();

        Arrays.sort(hhs);
        logger.info("Finished reading in hhs and persons");
        return hhs;

    }

    
    public void readTazData() {
        // initialize the taz manager and destination choice logsums
        String tazManagerClassName = ResourceUtil.getProperty(appRb,"sdt.taz.manager.class");
        Class tazManagerClass = null;
        tazManager = null;
        try {
            tazManagerClass = Class.forName(tazManagerClassName);
            tazManager = (TazManager) tazManagerClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.fatal(tazManagerClass + " not found");
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            logger.fatal("Can't Instantiate of TazManager of type "+tazManagerClass.getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Illegal Access of TazManager of type "+tazManagerClass.getName());
            throw new RuntimeException(e);
        }

        String tazClassName = appRb.getString("sdt.taz.class");
        tazManager.setTazClassName(tazClassName);
        tazManager.readData(globalRb, appRb);
        
        // update the employment
        String empFileName = ResourceUtil.getProperty(appRb, "sdt.previous.employment");
        tazManager.updateWorkersFromSummary(empFileName);

    }
    

    /**
     * Constructs an array of just those households with LDT on the model day.
     *
     * @param households An array of households.
     * @return An array of the subset of households that makes long-distance travel.
     */
    private PTHousehold[] selectLongDistanceHouseholds(PTHousehold[] households) {

        logger.info("Selecting households with long distance travel.");
        logger.info("  HH Input:    " + households.length);

        ArrayList<PTHousehold> ldtHouseholds = new ArrayList<PTHousehold>();
        for (int i=0; i<households.length; i++) {
            boolean hasLDT = checkHouseholdForLongDistanceTravel(households[i]);
            if (hasLDT) ldtHouseholds.add(households[i]);
        }

        PTHousehold[] ldtHouseholdArray = new PTHousehold[ldtHouseholds.size()];
        logger.info("  HH Selected: " + ldtHouseholds.size());

        ldtHouseholdArray = ldtHouseholds.toArray(ldtHouseholdArray);
        logger.info("  HH Selected: " + ldtHouseholdArray.length);

        return ldtHouseholdArray;
    }

    
    /**
     * Reads in the household and person file, and creates an array of
     * LDTours based on those results.
     *
     * @return The list of LD tours made.
     */
    public LDTour[] createToursFromHouseholdFile(int baseYear) {
        PTHousehold[] households = readHouseholdsAndPersons(baseYear);
        return createLDTours(households);
    }
    
    private void setIncomeRanges() {
//        int lowMax = ResourceUtil.getIntegerProperty(appRb,"pt.low.max.income",20000);
//        int highMax = ResourceUtil.getIntegerProperty(appRb,"pt.med.high.max.income",60000);
        PriceConverter priceConverter = PriceConverter.getInstance(globalRb,appRb);
        //PRICE, not INCOME, because this is coming from a property file, not from synthetic population income levels
        int lowMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(appRb,"pt.low.max.income",20000),PriceConverter.ConversionType.PRICE);
        int highMax = priceConverter.convertPrice(ResourceUtil.getIntegerProperty(appRb,"pt.med.high.max.income",60000),PriceConverter.ConversionType.PRICE);
        IncomeSegmenter.setIncomeCategoryRanges(lowMax, highMax);
    }
    
    public void readDcLogsums() {
        logger.info("Reading DC Logsums");          
        String alphaName = ResourceUtil.getProperty(globalRb, "alpha.name");
        TourDestinationChoiceLogsums.readLogsums(appRb, alphaName);
    }
    
    /**
     * Generically read a matrix file.
     * 
     * @param fileName Base file name. File name extension.
     */
    private Matrix readTravelCost(String fileName, String name) {
        long startTime = System.currentTimeMillis();

        logger.info("Reading travel costs in " + fileName);
        Matrix matrix = MatrixReader.readMatrix(new File(fileName), name);
        matrix.setName(name);
        logger.debug("\tRead " + fileName + " in: "
                + (System.currentTimeMillis() - startTime) / 1000 + " seconds");

        return matrix;
    }

    /**
     * Reads the background data, and creates and auto ownership model.
     *
     * @return An auto ownership model
     */
    private AutoOwnershipModel createAutoOwnershipModel() {     
        String hwyPath = ResourceUtil.getProperty(appRb, "highway.assign.previous.skim.path");
        String fileName = ResourceUtil.getProperty(appRb, "pt.Car.Pk.skims.file");
        
        logger.info("Reading time in " + hwyPath + fileName);
        Matrix time = readTravelCost(hwyPath + fileName, "carPkTime");
        logger.info("Reading distance in " + hwyPath + fileName);
        Matrix dist = readTravelCost(hwyPath + fileName, "carPkDist");
        
        // create model
        String alphaName = appRb.getString("alpha.name");
        AutoOwnershipModel aom = new AutoOwnershipModel(appRb, time, dist, alphaName);
        aom.buildModel();

        return aom;
    }

    /**
     * Runs the household-level steps of the long-distance travel models, in the following steps:
     *
     *      1. Read households and persons from the synthetic population generator.
     *      2. Run auto ownership model, or read results.
     *      3. Binary choice of travel or no travel in two-week period.
     *      4. Pattern choice of COMPLETE_TOUR, BEGIN_TOUR, END_TOUR, AWAY, or NO_TOUR
     *      5. Write the households that make LD tours to disk.
     *
     *
     * TODO These models will actually be run as part of SDT, but are include here for testing.
     */
    public PTHousehold[] runHouseholdLevelModels(boolean runAutoOwnership, boolean writeOnlyLDTHH, int baseYear) {

        setIncomeRanges(); 

        // initialize the models
        AutoOwnershipModel aom = createAutoOwnershipModel();
        LDBinaryChoiceModel binaryModel = new LDBinaryChoiceModel(appRb);
        //binaryModel.setTrace(true);
        boolean sensitivityTestingMode = ResourceUtil.getBooleanProperty(appRb, "pt.sensitivity.testing", false);
        LDPatternModel patternModel = new LDPatternModel(appRb);


        LDTReporter reporter = new LDTReporter(appRb);

        PTHousehold[] households;

        // read households, and add auto ownership if necessary
        households = readHouseholdsAndPersons(baseYear);

        logger.info("Making household level decisions");
        if (runAutoOwnership) {
            for (PTHousehold household : households) {
                aom.calculateUtility(household);
                household.autos = (byte) aom.chooseAutoOwnership();
            }
        }

        // make long-distance travel decisions
        binaryModel.runBinaryChoiceModel(households);
        patternModel.runPatternModel(households, sensitivityTestingMode);

        // send to reporter
        reporter.countHouseholdLevelDecisions(households);

        // select only the HH with LDT if the option is set
        if (writeOnlyLDTHH) {
            households = selectLongDistanceHouseholds(households);
        }

        // write to disk
        PTResults results = new PTResults(appRb, globalRb);
        results.createFiles(); 
        results.writeHouseholdData(households);
        results.writePersonData(households);
        results.close(); 
        
        reporter.logHouseholdLevelDecisions();
        logger.info("Finished LDT Household Level Models.");
        
        return households; 
    }

    
    public void setResourceBundles(ResourceBundle appRb, ResourceBundle globalRb){
        this.appRb = appRb;
        this.globalRb = globalRb;
    }
    
}
