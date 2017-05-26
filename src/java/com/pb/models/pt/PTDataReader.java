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

import com.pb.common.datafile.DiskObjectArray;
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.common.util.SeededRandom;
import com.pb.models.pt.ldt.LDTourPatternType;
import com.pb.models.pt.ldt.LDTourPurpose;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * PTDataReader reads Household and Person data.
 *
 * Adds household information to person array that are necessary for workplace
 * location model adds persons to household array (done after workplace location
 * model has run)
 *
 * @author Steve Hansen
 * @author Andrew Stryker <stryker@pbworld.com> (substaintial revision)
 * @version 3.0 6 March 2006
 *
 */
public class PTDataReader {
    protected static Logger logger = Logger.getLogger(PTDataReader.class);

    protected ResourceBundle ptRb;
    protected ResourceBundle globalRb;

    protected BufferedReader householdReader;

    protected BufferedReader personReader;

    PTOccupationReferencer myOccReferencer;

    protected int baseYear;

    private final PriceConverter priceConverter;

    //float incomeConversionFactor = 1.0f;   //default value, will check in prop file for new value.


    //Field names from the SynPop file that need to be read.
    //These change depending on whether 1990 or 2000 PUMS are
    //used to build SynPop.  In TlUMIP 1990 or 2000 is used,
    //In OSMP, only 2000 is used. The ones defined here are
    //the same in both years, otherwise they will be initialized
    //in the constructor (read from the global.properties file)
    static String LD_TOUR_FIELD = "LD_HOUSEHOLD_TOUR";
    static String LD_PATTERN_FIELD = "LD_HOUSEHOLD_PATTERN";
    static String LD_INDICATOR_PREFIX = "LD_INDICATOR_";
    protected static String HH_ID_FIELD = "HH_ID";
    static String PERSONS_FIELD = "PERSONS";
    static String AUTO_FIELD ;
    static String UNIT_FIELD;
    static String INC_FIELD;
    static String TAZ_FIELD;
    //Person file headers
    static String PERSON_ID_FIELD = "PERS_ID";
    static String WORK_TAZ_FIELD = "WORK_TAZ";
    static String GENDER_FIELD = "SEX";
    static String AGE_FIELD = "AGE";
    static String SPLIT_IND_FIELD = "INDUSTRY";
    static String OCC_FIELD = "SW_OCCUP";
    static String SCHOOL_FIELD;
    static String EMP_FIELD;
    static String WORK_OCC_FIELD;

    HashMap<String, Integer> personFieldPositions;
    HashMap<String, Integer> householdFieldPositions;

    int hhCount = 0;
    int rowPointerPosition = 2;
    Random randomNumGenerator;

    /**
     * Constructor.
     *
     * Opens the household and person files.
     *
     * @param ptRb
     *            resource bundle
     * @param globalRb
     *            resource bundle
     * @param occRef
     *            occupation reference object (project specific)
     */
    public PTDataReader(ResourceBundle ptRb, ResourceBundle globalRb, PTOccupationReferencer occRef, int baseYear) {
        this.ptRb = ptRb;
        this.globalRb = globalRb;
        myOccReferencer = occRef;
        this.baseYear = baseYear;

        priceConverter = PriceConverter.getInstance(ptRb,globalRb);

        //String cf = ResourceUtil.getProperty(globalRb, "convertTo2000Dollars", "1.0");
        //incomeConversionFactor = Float.parseFloat(cf);
        //logger.debug("Incomes will be multiplied by " + incomeConversionFactor);
        logger.debug("Incomes will be multiplied by " + priceConverter.getConversionFactor(PriceConverter.ConversionType.INCOME));

        AUTO_FIELD = globalRb.getString("pums.autoField.name");
        UNIT_FIELD = globalRb.getString("pums.houseTypeField.name");
        INC_FIELD = globalRb.getString("pums.hhIncomeField.name");
        TAZ_FIELD = globalRb.getString("alpha.name");
        SCHOOL_FIELD = globalRb.getString("pums.studentStatField.name");
        EMP_FIELD = globalRb.getString("pums.empStatField.name");
        WORK_OCC_FIELD = globalRb.getString("pums.workOccupation.name");
    }


//    public void openHouseholdFile(){
//        String hhFileName = ResourceUtil.getProperty(ptRb, "spg2.synpopH");
//        logger.info("Opening household file " + hhFileName);
//        try {
//            householdReader = new BufferedReader(new FileReader(hhFileName));
//        } catch (IOException e) {
//            logger.fatal("Could not open household file" + hhFileName);
//            throw new RuntimeException(e);
//        }
//    }


    public void openHouseholdFile(){
        String hhFileName = ResourceUtil.getProperty(ptRb, "spg2.synpopH");
        logger.info("Opening household file " + hhFileName);
        //attempt to retry if network is flakey
        int maxRetryCount = 10;
        int retryCount = 0;
        Exception ex = null;
        while (retryCount < maxRetryCount) {
            try {
                householdReader = new BufferedReader(new FileReader(hhFileName));
                ex = null;
                break;
            } catch (IOException e) {
                logger.fatal("Could not open household file" + hhFileName);
                if (e.getMessage().toLowerCase().indexOf("specified network name is no longer available") == -1)
                    throw new RuntimeException(e); //not the network error we're having problems with
                ex = e;
                retryCount++;
            }
        }
        if (ex != null)
            throw new RuntimeException(ex);
    }

    public void closeHouseholdFile(){
        String hhFileName = ResourceUtil.getProperty(ptRb, "spg2.synpopH");
        logger.info("Closing household file " + hhFileName);
        try {
            householdReader.close();
        } catch (IOException e) {
            logger.fatal("Could not close household file" + hhFileName);
            throw new RuntimeException(e);
        }
    }

    public void openPersonFile(){
        String perFileName = ResourceUtil.getProperty(ptRb, "spg2.synpopP");
        // person file
        //attempt to retry if network is flakey
        int maxRetryCount = 10;
        int retryCount = 0;
        Exception ex = null;
        while (retryCount < maxRetryCount) {
            try {
                personReader = new BufferedReader(new FileReader(perFileName));
                ex = null;
                break;
            } catch (IOException e) {
                logger.fatal("Could not open person file" + perFileName);
                if (e.getMessage().toLowerCase().indexOf("specified network name is no longer available") == -1)
                    throw new RuntimeException(e); //not the network error we're having problems with
                ex = e;
                retryCount++;
            }
        }
        if (ex != null)
            throw new RuntimeException(ex);
    }



    public void closePersonFile(){
        String perFileName = ResourceUtil.getProperty(ptRb, "spg2.synpopP");
        // person file
        try {
            personReader.close();
        } catch (IOException e) {
            logger.fatal("Could not close person file" + perFileName);
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method when you just want to read in the entire
     * household file and create HH objects
     *
     ** @return  PTHoushold array
     */
    public PTHousehold[] readHouseholds(){
        return readHouseholds(2, Integer.MAX_VALUE -1, 1);
    }


    /**
     * Convenience method when sample rate = 1, meaning you want
     * to read every household in the file.
     *
     * @param startRow
     * @param endRow
     * @return  PTHoushold array
     */
    public PTHousehold[] readHouseholds(int startRow, int endRow){
        return readHouseholds(startRow, endRow, 1);
    }

    public PTHousehold[] readHouseholds(int startRow, int endRow, int sampleRate, boolean visitor) {
        logger.info("Sample Rate: " + sampleRate);
        ArrayList<PTHousehold> households = new ArrayList<PTHousehold>();
        int rowPointerPosition = 1;
        int hhCount = 0;

        openHouseholdFile();
        try {
            // create a lookup table for field positions from header row
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            String[] header = householdReader.readLine().split(",");
            rowPointerPosition++;
            for (int i = 0; i < header.length; ++i) {
                positions.put(header[i], i);
            }

            //skip the rows that are not the ones you want
            while(rowPointerPosition < startRow){
                householdReader.readLine();
                rowPointerPosition++;
                hhCount++;
            }


            String line = householdReader.readLine();
            rowPointerPosition++;
            hhCount++;
            while (line != null && rowPointerPosition <= endRow + 1) {
                if (hhCount % sampleRate==0) {
                    PTHousehold household = new PTHousehold();
                    parseHhLine(household, line, positions);  //this will set the household members
                    household.setVisitor(visitor);
                    households.add(household);
                    hhCount=0;   //reset counter cause it only needs to count up to sample rate
                }
                line = householdReader.readLine();
                rowPointerPosition++;
                hhCount++;
            }

            closeHouseholdFile();

        } catch (IOException e) {
            logger.fatal("Error reading household file.");
            throw new RuntimeException(e);
        }

        logger.info("Read in " + households.size() + " households");

        return households.toArray(new PTHousehold[households.size()]);
    }

    /**
     *
     * Convenience method: Used to read resident hhs.
     * 0. hh_id
     * 1. state
     * 2. serial number
     * 3. persons
     * 4. unittype or units1
     * 5. hhinc or rhhinc
     * 6. taz or Azone
     * @param startRow - position to start in household file
     * @param endRow - last row to read in hh file
     * @param sampleRate - only read the nth hh.
     * @return An array of Households
     */
    public PTHousehold[] readHouseholds(int startRow, int endRow, int sampleRate) {

        return readHouseholds(startRow, endRow, sampleRate, false);
    }


    public void readHouseholdHeader(){
            try {
                // create a lookup table for field positions
                householdFieldPositions = new HashMap<String, Integer>();
                String[] header = householdReader.readLine().split(",");
                for (int i = 0; i < header.length; ++i) {
                    householdFieldPositions.put(header[i], i);
                }

            } catch (IOException e) {
                logger.fatal("Error reading household file.");
                throw new RuntimeException(e);
            }
        }


    /**
     * Not currently used but saving for a possible refactor.  Header has been read
     * prior to this method being called.
     */
    public PTHousehold[] readHouseholdBlock(int startRow, int endRow, int sampleRate) {
        logger.info("Sample Rate: " + sampleRate);
        ArrayList<PTHousehold> households = new ArrayList<PTHousehold>();

        try {
            //skip the rows that are not the ones you want
            while(rowPointerPosition < startRow){
                householdReader.readLine();
                hhCount++;
                rowPointerPosition++;
            }

            String line = householdReader.readLine();
            rowPointerPosition++;

            while (line != null && rowPointerPosition <= endRow + 1) {
                if (hhCount % sampleRate==0) {
                    PTHousehold household = new PTHousehold();
                    parseHhLine(household, line, householdFieldPositions);  //this will set the household members
                    households.add(household);
                    hhCount = 0;    //reset counter cause it only needs to count up to sample rate
                }
                line = householdReader.readLine();
                rowPointerPosition++;
                hhCount++;
            }

        } catch (IOException e) {
            logger.fatal("Error reading household file.");
            throw new RuntimeException(e);
        }

        logger.info("Read in " + households.size() + " households");

        return households.toArray(new PTHousehold[households.size()]);
    }

    private void parseHhLine(PTHousehold household, String line, HashMap<String, Integer> positions){
        boolean readAutos = positions.containsKey(AUTO_FIELD);

        boolean readLDT = positions.containsKey(LD_TOUR_FIELD);

        String[] fields = line.split(",");

        household.ID = Integer.parseInt(fields[positions.get(HH_ID_FIELD)]);
        household.size = Byte.parseByte(fields[positions.get(PERSONS_FIELD)]);

        // housing type
        // 1. mobile home,
        // 2. one-family detached
        // 3. one-family attached
        // 4. 2 apartments
        // 5. 3-4 apartments
        // 6. etc.
        //
        int type = Integer.parseInt(fields[positions.get(UNIT_FIELD)]);
        household.singleFamily = type == 1 || type == 2 || type == 3;
        household.multiFamily = !household.singleFamily;

        
        // code income into segments
        //household.income = (int)(Integer.parseInt(fields[positions.get(INC_FIELD)])*incomeConversionFactor);
        household.income = priceConverter.convertPrice((int) Double.parseDouble(fields[positions.get(INC_FIELD)]),PriceConverter.ConversionType.INCOME);

        household.homeTaz = Short.parseShort(fields[positions.get(TAZ_FIELD)]);

        if (readAutos) {
            household.autos = Byte.parseByte(fields[positions.get(AUTO_FIELD)]);
        }

        if (readLDT) {
            household.ldHouseholdTourIndicator = Boolean.parseBoolean(fields[positions
                    .get(LD_TOUR_FIELD)]);
            household.ldHouseholdTourPattern = LDTourPatternType
                    .getType(Integer.parseInt(fields[positions
                            .get(LD_PATTERN_FIELD)]));
        }
    }

    //public int[][] getInfoFromHouseholdFile(float incomeConversionFactor){
    public int[][] getInfoFromHouseholdFile(PriceConverter priceConverter){
        openHouseholdFile();
        try {
            int lowestIdNum = Integer.MAX_VALUE;
            int nHhsInFile = 0;
            // create a lookup table for field positions
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            String[] header = householdReader.readLine().split(",");
            for (int i = 0; i < header.length; ++i) {
                positions.put(header[i], i);
            }

            int hhIdIndex = positions.get(HH_ID_FIELD);
            int incomeIndex = positions.get(INC_FIELD);
            int homeTazIndex = positions.get(TAZ_FIELD);
            int currentId = -1;
            int currentIncome;
            short homeTaz;
            HashMap<Integer, Integer> incomesByHhId = new HashMap<Integer, Integer>();
            HashMap<Integer, Short> homeTazByHhId = new HashMap<Integer, Short>();
            String line = householdReader.readLine();

            while (line != null) {
                String[] fields = line.split(",");
                currentId = Integer.parseInt(fields[hhIdIndex]);
                if(currentId < lowestIdNum) lowestIdNum = currentId;
                nHhsInFile++;
//                currentIncome = (int) (Integer.parseInt(fields[incomeIndex]) * incomeConversionFactor);
                currentIncome = priceConverter.convertPrice((int) Double.parseDouble(fields[incomeIndex]), PriceConverter.ConversionType.INCOME);
                incomesByHhId.put(currentId, currentIncome);

                homeTaz = Short.parseShort(fields[homeTazIndex]);
                homeTazByHhId.put(currentId, homeTaz);

                line = householdReader.readLine();
            }

            closeHouseholdFile();

            int[] incomesByHh = new int[nHhsInFile + lowestIdNum];
            int[] homeTazsByHh = new int[nHhsInFile + lowestIdNum];

            for(int hhId : incomesByHhId.keySet()){
                incomesByHh[hhId] = incomesByHhId.get(hhId);
                homeTazsByHh[hhId] = homeTazByHhId.get(hhId);
            }

            return new int[][] {{lowestIdNum}, {nHhsInFile}, incomesByHh, homeTazsByHh};

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


}

    /**
     * Read all the persons in the person file.
     *
     * Field labels, case respected:
     *
     * hh_id - household identification
     *
     * pers_id - person identification
     *
     * serialno - ignored
     *
     * sex - female = 1, male = 2
     *
     * age - person's age
     *
     * enroll - 2 or 3 means that the person is in school
     *
     * esr - employment
     *
     * indnaics - industry code (from PUMS)
     *
     * occsoc5 - occupation code (from PUMS)
     *
     * sw_unsplit_ind - general industry
     *
     * sw_occup - statewide occupation
     *
     * sw_split_ind - statewide industry split
     *
     * @return An array of all persons.
     */
    public PTPerson[] readPersons() {
        ArrayList<PTPerson> persons = new ArrayList<PTPerson>();

        openPersonFile();
        try {
            // create a lookup table for field positions
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            String[] header = personReader.readLine().split(",");
            for (int i = 0; i < header.length; ++i) {
                positions.put(header[i], i);
            }

            String line = personReader.readLine();

            while (line != null) {
                PTPerson person = new PTPerson();
                parsePersonLine(person, line, positions);

                persons.add(person);
                line = personReader.readLine();
            }

            closePersonFile();

        } catch (IOException e) {
            logger.fatal("Error reading person file.");
            throw new RuntimeException(e);
        }
        logger.info("Read " + persons.size() + " persons.");

        return persons.toArray(new PTPerson[persons.size()]);
    }

    public void readPersonHeader(){
        try{
            // create a lookup table for field positions
            personFieldPositions = new HashMap<String, Integer>();
            String[] header = personReader.readLine().split(",");

            for (int i = 0; i < header.length; ++i) {
                personFieldPositions.put(header[i], i);
            }

            if(personFieldPositions.containsKey(WORK_TAZ_FIELD)){
                logger.info("Reading workplace taz from SynPopP");
            }
        }catch (Exception e){
            throw new RuntimeException("Cannot read header of person file");

        }
    }

    /**
     * Read all the persons in the person file.
     *
     * Field labels, case respected:
     *
     * hh_id - household identification
     *
     * pers_id - person identification
     *
     * serialno - ignored
     *
     * sex - female = 1, male = 2
     *
     * age - person's age
     *
     * enroll - 2 or 3 means that the person is in school
     *
     * esr - employment
     *
     * indnaics - industry code (from PUMS)
     *
     * occsoc5 - occupation code (from PUMS)
     *
     * sw_unsplit_ind - general industry
     *
     * sw_occup - statewide occupation
     *
     * sw_split_ind - statewide industry split
     *
     * @return An array of all persons.
     */
    public PTPerson[] readPersonsForTravelModels(int hhSize, int hhId) {
        PTPerson[] persons = new PTPerson[hhSize];
        try {
            int hhIdPos = personFieldPositions.get(HH_ID_FIELD);

            int idRead = -1;
            String line = null;
            while (idRead != hhId){
                line = personReader.readLine();
                if(line != null){
                    idRead = Integer.parseInt(line.split(",")[hhIdPos]);
                }else throw new RuntimeException("At end of file and no persons with hhId: " + hhId);
            }

            PTPerson person = new PTPerson();
            parsePersonLine(person, line, personFieldPositions);
            //assign a unique random number seed that is a combination
            //of the persons HHID and their member ID
            person.randomSeed = person.hhID*100 + person.memberID;
            persons[0] = person;

            for(int personCount = 1; personCount < hhSize; personCount++) {
                line = personReader.readLine();
                person = new PTPerson();
                parsePersonLine(person, line, personFieldPositions);
                //assign a unique random number seed that is a combination
                //of the persons HHID and their member ID
                person.randomSeed = person.hhID*100 + person.memberID;
                
                if(person.hhID != hhId) throw new RuntimeException("SynPop files are not sorted correctly");

                persons[personCount] = person;
            }



        } catch (IOException e) {
            logger.fatal("Error reading person file.");
            throw new RuntimeException(e);
        }

        return persons;
    }

    public void parsePersonLine(PTPerson person, String line, HashMap<String, Integer> positions){
        // look for long distance variables
        boolean ldtRead = positions.containsKey(LD_INDICATOR_PREFIX
                + LDTourPurpose.HOUSEHOLD);

        boolean workTazRead = positions.containsKey(WORK_TAZ_FIELD);
        int hhIdPos = positions.get(HH_ID_FIELD);
        int memberPos =positions.get(PERSON_ID_FIELD);
        int femalePos = positions.get(GENDER_FIELD);
        int agePos = positions.get(AGE_FIELD);
        int schoolPos = positions.get(SCHOOL_FIELD);
        int empPos =positions.get(EMP_FIELD);
        int splitIndPos = positions.get(SPLIT_IND_FIELD);
        int occPos = positions.get(OCC_FIELD);
        int workTazPos = -1;
        if(positions.containsKey(WORK_TAZ_FIELD)){
            workTazPos = positions.get(WORK_TAZ_FIELD);
        }

        String[] fields = line.split(",");

        person.hhID = Integer.parseInt(fields[hhIdPos]);
        person.memberID = Integer.parseInt(fields[memberPos]);
        if(baseYear == 1990){
            person.female = Integer.parseInt(fields[femalePos]) == 1;
        }else {
            person.female = Integer.parseInt(fields[femalePos]) == 2;    
        }

        person.age = Byte.parseByte(fields[agePos]);

        int school = Byte.parseByte(fields[schoolPos]);
        person.student = school == 2 || school == 3;

        int employ = Integer.parseInt(fields[empPos]);
        person.employed = employ == 1 || employ == 2 || employ == 4
                || employ == 5;
       
        person.occupation = myOccReferencer.getOccupation(Integer.parseInt(fields[occPos]));
        
        // code the person type
        if (person.age <= 5) person.personType = PersonType.PRESCHOOL;
        else if (person.age <= 17) person.personType = PersonType.STUDENTK12;
        else if (person.student) person.personType = PersonType.STUDENTCOLLEGE;
        else if (person.employed) person.personType = PersonType.WORKER;
        else person.personType = PersonType.NONWORKER;

        // when LDT is reading the file, whether a long distance trip
        // happens is already known
        if (ldtRead) {

            for (LDTourPurpose purpose : LDTourPurpose.values()) {
                String label = LD_INDICATOR_PREFIX + purpose;
                person.ldTourIndicator[purpose.ordinal()] = Boolean.parseBoolean(fields[positions
                        .get(label)]);
            }

            for (LDTourPurpose purpose : LDTourPurpose.values()) {
                String label = LD_PATTERN_FIELD + purpose;
                person.ldTourPattern[purpose .ordinal()] = LDTourPatternType
                        .getType(fields[positions.get(label)]);
            }
        }

        if (workTazRead) {
            person.workTaz = Short.parseShort(fields[workTazPos]);
        }
    }

    /**
     * Read all the persons in the person file.
     *
     * Only certain fields necessary for workplace location model will be read:
     *
     * hh_id - household identification
     *
     * pers_id - person identification
     *
     * esr - employment
     *
     * sw_occup - statewide occupation
     *
     * sw_split_ind - statewide industry split
     *
     * and person is assigned a random seed.
     *
     * 4/22/08 Christi made a change to how the person's randomSeed was set.  It is now the
     * HHID*10 + the memberID.  This seed will be unique to this person and will be set the
     * same no matter which VM the person is created on.
     *
     * 3/10/15 Ashish made changes to use work occupation code for persons. This was used to get the split
     * employment to be used as segmented employment in the work place location choice model. 
     * @return An array of all persons.
     */
    public PTPerson[] readPersonsForWorkplaceLocation(int startRow, int endRow) {

        ArrayList<PTPerson> persons = new ArrayList<PTPerson>();
        int rowPointerPosition = 1;
        openPersonFile();
        try {
            // create a lookup table for field positions
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            String[] header = personReader.readLine().split(",");
            rowPointerPosition++;
            for (int i = 0; i < header.length; ++i) {
                positions.put(header[i], i);
            }

            int hhIdPos = positions.get(HH_ID_FIELD);
            int memberPos =positions.get(PERSON_ID_FIELD);
            int empPos =positions.get(EMP_FIELD);
            int workOccPos = positions.get(WORK_OCC_FIELD);
            
            //skip the rows that are not the ones you want
            while(rowPointerPosition < startRow){
                personReader.readLine();
                rowPointerPosition++;
            }

            String line = personReader.readLine();
            rowPointerPosition++;
            
            while (line != null && rowPointerPosition <= endRow + 1) {                
            	PTPerson person = new PTPerson();
                String[] fields = line.split(",");
                
                person.hhID = Integer.parseInt(fields[hhIdPos]);
                person.memberID = Integer.parseInt(fields[memberPos]);

                int employ = Integer.parseInt(fields[empPos]);
                
                person.employed = employ == 1 || employ == 2 || employ == 4
                || employ == 5;
                
                person.workOccupation = Integer.parseInt(fields[workOccPos]);		
                
                person.randomSeed = person.hhID*100 + person.memberID;

                persons.add(person);

                line = personReader.readLine();
                rowPointerPosition++;
            }
            
            closePersonFile();

        } catch (IOException e) {
            logger.fatal("Error reading person file.");
            throw new RuntimeException(e);
        }
        logger.info("Read " + persons.size() + " persons.");

        return persons.toArray(new PTPerson[persons.size()]);
    }


    /**
     * This method returns the number of persons in the person file.
     *  It also has the SIDE EFFECT of populating
     * the nWorkersPerHh, an array passed in by the user that is initialized
     * but not populated with actual data.
     *
     * @param nWorkersPerHh
     * @return total number of persons (nWorkersPerHh array is also populated)
     */
    public int getInfoFromPersonFile (int[] nWorkersPerHh){
        openPersonFile();
        try {
            // create a lookup table for field positions
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            String[] header = personReader.readLine().split(",");
            for (int i = 0; i < header.length; ++i) {
                positions.put(header[i], i);
            }

            int hhIdPos = positions.get(HH_ID_FIELD);
            int empPos =positions.get(EMP_FIELD);

            int hhId = -1;
            int empCode = -1;

            int nPersons = 0;
            String line = personReader.readLine();

            String[] fields = null;
            while (line != null) {
                fields = line.split(",");

                hhId = Integer.parseInt(fields[hhIdPos]);
                empCode = Integer.parseInt(fields[empPos]);

                if(empCode ==1  || empCode == 2 || empCode == 4 || empCode == 5){
                    nWorkersPerHh[hhId]++;
                }

                nPersons++;
                line = personReader.readLine();
            }

            closePersonFile();
            return nPersons;

        } catch (IOException e) {
            logger.fatal("Error reading person file.");
            throw new RuntimeException(e);
        }

    }

    /**
     * Populate the household with person characteristics and vice versa.
     *
     * This method sorts the Household array by HHID and the Persons array by
     * PersonID and then adds worker information to the households by calling
     * the 'addWorkerInformationToHouseholds' method and then adds homeTAZ and
     * hhWorkSegment to the persons array by calling
     * 'addHomeTazAndHHWorkSegmentToPersons' method. Finally, place persons into
     * households.
     *
     * This method must be called before the AutoOwnership model is run.
     * @param households Household array
     * @param persons Person array
     */
    public static void addPersonInfoToHouseholds(
            PTHousehold[] households, PTPerson[] persons) {

        logger.info("Sorting persons and households by hhID.");
        sortPersonsAndHouseholds(households, persons);

        logger.info("Putting persons into households.");
        addPersonsToHouseholds(households, persons);

        logger.info("Adding Worker info to Households.");
        addWorkerInformationToHouseholds(households, persons);
    }

    /**
     * Populate the household with person characteristics and vice versa.
     *
     * This method sorts the Household array by HHID and the Persons array by
     * PersonID and then adds worker information to the households by calling
     * the 'addWorkerInformationToHouseholds' method and then adds homeTAZ and
     * hhWorkSegment to the persons array by calling
     * 'addHomeTazAndHHWorkSegmentToPersons' method. Finally, place persons into
     * households.
     *
     * This method must be called before the AutoOwnership model is run.
     * @param households Household array
     * @param persons Person array
     */
    public static void sortThenAddPersonsToHouseholds(
            PTHousehold[] households, PTPerson[] persons) {

        logger.info("Sorting persons and households by hhID.");
        sortPersonsAndHouseholds(households, persons);

        logger.info("Putting persons into households.");
        addPersonsToHouseholds(households, persons);


    }



    /**
     * This method makes sure that the persons are in their proper household and
     * sets the households's workers attribute to the number of workers
     * depending on the employment status of the persons in the person array. It
     * returns the array of households.
     *
     * @param households Household array
     * @param persons Person array
     */
    private static void addWorkerInformationToHouseholds(
            PTHousehold[] households, PTPerson[] persons) {

        try {
            // persons counter
            int p = 0;
            // loop through all households
            for (PTHousehold household : households) {
                // loop through the persons in this household
                for (int i = 0; i < household.size; i++) {
                    if (persons[p].hhID != household.ID) {
                        throw new Exception();
                    }
                    if (persons[p].employed)
                        household.workers++;
                    p++;
                    if (p == persons.length)
                        break;
                }
            }

        } catch (Exception e) {
            String msg = "Household file and person do not match.  Unable to "
                    + "add worker data to households.";
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * This method sets the the person's householdWorkSegment
     * to the household work logsum segment.
     * It returns the array of persons.
     *
     * @param households Household Array
     * @param persons Person Array
     * @return the person array (not really necessary)
     */
    public static PTPerson[] addHouseholdInfoToPersons(
            PTHousehold[] households, PTPerson[] persons) {

        // persons counter
        int p = 0;
        // loop through all households
        for (PTHousehold household : households) {
            // loop through the persons in this household
            for (int i = 0; i < household.size; i++) {
                persons[p].segment = (byte) IncomeSegmenter.calcLogsumSegment(household.income, household.autos, household.workers);
                p++;
                if (p == persons.length)
                    break;
            }
        }
        return persons;
    }



    /**
     * Generate a PTPerson array for
     * each household. Fill it with persons from the person array that have the
     * same ID as the household. Set the person array for the household to the
     * created array.
     *
     * **IMPORTANT:  sortPersonsAndHouseholds method must be called
     * prior to this method.
     *
     * @param households Household array
     * @param persons persons array
     * @return An array of households with persons
     */
    private static PTHousehold[] addPersonsToHouseholds(
            PTHousehold[] households, PTPerson[] persons) {

        try {
            // persons counter
            int p = 0;
            // loop through all households
            for (PTHousehold household : households) {
                household.persons = new PTPerson[household.size];
                for (int personNumber = 0; personNumber < household.persons.length; personNumber++) {
                    if (persons[p].hhID != household.ID) {
                        throw new ModelException("Person " + p
                                + " is in household " + persons[p].hhID
                                + " not in " + household.ID + ".");
                    }
                    persons[p].homeTaz = household.homeTaz;
                    household.persons[personNumber] = persons[p];
                    p++;

                    if (p % 10000 == 0) {
                        logger.info("Added person " + p);
                    }

                    if (p == persons.length)
                        break;
                }
            }

        } catch (Exception e) {
            logger.fatal("Household file and person file don't match up.  "
                    + "Unable to add persons to households.");
            // TODO - log this exception to the node exception logger
            throw new RuntimeException(e);
        }
        return households;
    }

    private static void sortPersonsAndHouseholds(PTHousehold[] households,
            PTPerson[] persons) {
        // Sort households by household ID
        Arrays.sort(households, new Comparator<PTHousehold>() {
            public int compare(PTHousehold hha, PTHousehold hhb) {
                if (hha.ID < hhb.ID)
                    return -1;
                else if (hha.ID > hhb.ID)
                    return 1;
                else
                    return 0;
            }
        });

        // Sort persons by household ID
        Arrays.sort(persons, new Comparator<PTPerson>() {
            public int compare(PTPerson pa, PTPerson pb) {
                if (pa.hhID < pb.hhID)
                    return -1;
                else if (pa.hhID > pb.hhID)
                    return 1;
                else
                    return 0;
            }
        });

    }

    /**
     * Reads a disk object array of the households that make long-distance tours.
     * This method serves as the interface between SDT and LDT.
     *
     * @param fileName The name of the file with the disk object array.
     * @return An array of household objects read from disk.
     */
    public static PTHousehold[] readLDTHouseholdDiskObject(String fileName) {

        PTHousehold[] households = null;

        try {
            DiskObjectArray diskObjectArray = new DiskObjectArray(fileName);
            int numHH = diskObjectArray.getArraySize();
            households = new PTHousehold[numHH];
            for (int i = 0; i < numHH; i++) {
                households[i] = (PTHousehold) diskObjectArray.get(i);
            }
        }
        catch (Exception e) {
            logger.fatal("Error reading LD Household Disk Object Array: " + fileName);
            e.printStackTrace();
        }

        return households;
    }

    //This method is called by the MSServerTask in order to
    //create the random number generator that is used to assign
    //a random seed to each person as they are read in for the travel
    //models
    public void createRandomNumberGenerator(){
        randomNumGenerator = new Random(2002);  //Same seed as SeededRandom class
    }

    public static void main(String[] args) throws IOException {
    	
//    	String line = "1,1,1,45,1,1,,620.0,,MANAGER,";
//    	String[] fields = line.split(",");
//    	int index = 0;
//        for(String field : fields) {
//        	System.out.println("index  " + index + "     value  " + field);
//        	index ++;
//        }
        String hhFileName = "C:/Users/kulshresthaa/Desktop/PopSyn0_HH/PopSyn0_HH.CSV";
        logger.info("Opening household file " + hhFileName);
        BufferedReader householdReader = null;
        try {
        	householdReader = new BufferedReader(new FileReader(hhFileName));

        } catch (IOException e) {
        	logger.fatal("Could not open household file" + hhFileName);
        }
        
            int lowestIdNum = Integer.MAX_VALUE;
            int nHhsInFile = 0;
            // create a lookup table for field positions
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            String[] header = householdReader.readLine().split(",");
            
            for (int i = 0; i < header.length; ++i) {
                positions.put(header[i], i);
                logger.info(header[i]);
            }
            
            int hhIdIndex = positions.get(HH_ID_FIELD);
            int incomeIndex = positions.get(INC_FIELD);
            int homeTazIndex = positions.get(TAZ_FIELD);
            int currentId = -1;
            int currentIncome;
            short homeTaz;
            HashMap<Integer, Integer> incomesByHhId = new HashMap<Integer, Integer>();
            HashMap<Integer, Short> homeTazByHhId = new HashMap<Integer, Short>();
            String line = householdReader.readLine();

            while (line != null) {
                String[] fields = line.split(",");
                currentId = Integer.parseInt(fields[hhIdIndex]);
                if(currentId < lowestIdNum) lowestIdNum = currentId;
                nHhsInFile++;
                currentIncome = (int) Double.parseDouble(fields[incomeIndex]);
                incomesByHhId.put(currentId, currentIncome);

                homeTaz = Short.parseShort(fields[homeTazIndex]);
                homeTazByHhId.put(currentId, homeTaz);

                line = householdReader.readLine();
            }

        try {
        	householdReader.close();
        } catch (IOException e) {
        	logger.fatal("Could not close household file" + hhFileName);
        }    	
    }
}
