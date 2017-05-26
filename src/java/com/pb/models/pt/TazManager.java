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
import com.pb.common.model.ModelException;
import com.pb.common.util.ResourceUtil;
import com.pb.models.reference.IndustryOccupationSplitIndustryReference;

import org.apache.log4j.Logger;

import java.io.*;

import static java.lang.Integer.parseInt;

import java.util.*;

/**
 * Class to access TAZ information from CSV File
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public abstract class TazManager implements Serializable, Cloneable {

    public final static transient Logger logger = Logger.getLogger(TazManager.class);

    final static long serialVersionUID = 98;
    String tazClassName;
    String alphaName;

    // a hashtable of taz objects
    public Hashtable<Integer, Taz> tazData;

    protected Taz[] tazs;

    protected static TableDataSet empIndustriesFile;
    protected static String[] empIndustryLabels;
    
    // externals stores the external zone number index
    protected int[] externalsOne;

    protected int[] externalsZero;

    /**
     * Default constructor. Initializes the tazData Hashtable.
     * employmentCategories
     */
    public TazManager() {
        tazData = new Hashtable<Integer, Taz>();

    }

    public void setTazClassName(String tazClassName){
        this.tazClassName = tazClassName;
    }

    /**
     * Preferred method for accessing taz data.
     * 
     * @param zoneNumber number of taz that you want
     * @return Taz taz object
     */
    public Taz getTaz(int zoneNumber) {
        return tazData.get(zoneNumber);
    }

    /**
     * The not preferred method to get taz data.
     * @return Hashtable hashtable of taz objects
     */
    public Hashtable<Integer, Taz> getTazData(){
        return tazData;
    }

    public Enumeration<Taz> elements() {
        return tazData.elements();
    }

    public Set<Integer> keySet() {
        return tazData.keySet();
    }

    public Collection<Taz> values() {
        return tazData.values();
    }

    /**
     * Get a collection of the numbers in the taz system.
     * @return Set set of TAZ numbers
     */
    public Set<Integer> getTazKeySet(){
        return tazData.keySet();
    }

    /**
     * Get the size of the taz system.
     *
     * @return int number of tazs
     */
    public int size() {
        return tazData.size();
    }


    /**
     * Return a Hashtable of the tazData.
     * 
     * @return Hashtable Hashtable of Tazs
     */
    public Hashtable<Integer, Taz> getTazDataHashtable() {
        return tazData;
    }

    /**
     * Read in the taz data file and the cordon data file and store in the tazData
     * hashmap where key is zoneNumber and value is the taz
     * object.
     *
     * @param appRb pt resource bundle
     * @param globalRb global resource bundle
     */
    public void readData(ResourceBundle globalRb, ResourceBundle appRb) {
        if(tazClassName == null){
            throw new RuntimeException("Must call 'setTazClassName(className)' method prior to reading data");
        }

        CSVFileReader reader = new CSVFileReader();
        try {
        	empIndustriesFile = reader.readFile(new File(globalRb.getString("emp.industry.list.file")));
        } catch (IOException e) {
           e.printStackTrace();
        }
        empIndustryLabels = empIndustriesFile.getColumnAsString("empIndustry");

        String fileName = ResourceUtil.getProperty(globalRb, "alpha2beta.file");
        String alphaName = globalRb.getString("alpha.name");
        String areaName = globalRb.getString("area.name");
        String dcDistrictName = globalRb.getString(("destination.choice.district.name"));
        
        TableDataSet tazTable;
        logger.info("Reading TAZ data in " + fileName);
        try {
            tazTable = reader.readFile(new File(fileName));
        } catch (IOException e) {
            logger.fatal("Unable to create tazTable from " + fileName);
            throw new RuntimeException(e);
        }

        TableDataSet synPopTable;
        String synPopSummaryFile = appRb.getString("spg2.current.synpop.summary");
        logger.info("Reading SynPop data in " + synPopSummaryFile);
        try {
            synPopTable = reader.readFile(new File(synPopSummaryFile));
            synPopTable.buildIndex(1);
        } catch (IOException e) {
            logger.fatal("Unable to read from " + synPopSummaryFile);
            throw new RuntimeException(e);
        }

        for (int row = 1; row <= tazTable.getRowCount(); row++) {
            Class tazClass;
            Taz taz;
            try {
                tazClass = Class.forName(tazClassName);
                taz = (Taz)tazClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            taz.zoneNumber = (int) tazTable.getValueAt(row, alphaName);
            taz.acres = tazTable.getValueAt(row, areaName);
            //12/29/08 calling setParkingCost method instead of setting costs here so that we can be
            // implementation specific
//            taz.workParkingCost = tazTable.getValueAt(row, "DayPark") * 100;
//            taz.nonWorkParkingCost = tazTable.getValueAt(row, "HourPark") * 100;
            
            taz.dcDistrict = (int) tazTable.getValueAt(row, dcDistrictName);

            taz.households = synPopTable.getIndexedValueAt(taz.zoneNumber, "TotalHHs");

            if (tazTable.containsColumn("AreaType")) {
                int atcode   = (int) tazTable.getValueAt(row, "AreaType"); 
                taz.areatype = AreaType.getAreaType(atcode); 
            } else {
                taz.areatype = AreaType.NONE; 
            }
            
            if (tazTable.containsColumn("TerminalTime")) {
                taz.terminalTime = tazTable.getValueAt(row, "TerminalTime"); 
            } else {
                taz.terminalTime = 0; 
            }
        
            if (tazTable.containsColumn("gsTeachingShare")) {
                taz.gradeSchoolTeachingShare = tazTable.getValueAt(row, "gsTeachingShare"); 
                if (taz.gradeSchoolTeachingShare<0) taz.gradeSchoolTeachingShare=0; 
                if (taz.gradeSchoolTeachingShare>1) taz.gradeSchoolTeachingShare=1; 
            } else {
                taz.gradeSchoolTeachingShare = 1; 
            }
            

            if (tazTable.containsColumn("NorthColumbia")) {
                taz.northOfColumbiaRiver = (int) tazTable.getValueAt(row, "NorthColumbia"); 
            } else {
                taz.northOfColumbiaRiver = 0; 
            }
            
            tazData.put(taz.zoneNumber, taz);
        }

        logger.debug("Read " + tazData.size() + " zones");

        addExternalZones(globalRb);
    }

    /**
     * This method will be overwritten (and empty) in the ITDTazManager since
     * cordon zones are not used.
     * @param globalRb
     */
    public void addExternalZones(ResourceBundle globalRb){
        // now read in cordon taz data
        String fileName = ResourceUtil.getProperty(globalRb, "cordon.zone.size.terms");

        logger.info("Reading Cordon TAZ data in " + fileName);
        CSVFileReader reader = new CSVFileReader();
        TableDataSet tazTable;
        try {
            tazTable = reader.readFile(new File(fileName));
        } catch (IOException e) {
            logger.fatal("Unable to create tazTable from " + fileName);
            throw new RuntimeException(e);
        }

        for (int row = 1; row <= tazTable.getRowCount(); row++) {
            Class tazClass;
            Taz taz;
            try {
                tazClass = Class.forName(tazClassName);
                taz = (Taz)tazClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            taz.zoneNumber = (int) tazTable.getValueAt(row, "ETAZ");
            taz.households = tazTable.getValueAt(row, "TotalHHS");
            taz.acres=1000; //a simple placeholder, has no effect other than to make the zone eligible for selection in d.c.
            
            for (String label : empIndustryLabels) {
                float value = tazTable.getValueAt(row, label);
                taz.employment.put(label, value);
            }
            
            taz.isCordon = true;
            
            if (tazTable.containsColumn("AreaType")) {
                int atcode   = (int) tazTable.getValueAt(row, "AreaType"); 
                taz.areatype = AreaType.getAreaType(atcode); 
            } else {
                taz.areatype = AreaType.NONE; 
            }
            
            if (tazTable.containsColumn("TerminalTime")) {
                taz.terminalTime = tazTable.getValueAt(row, "TerminalTime"); 
            } else {
                taz.terminalTime = 0; 
            }
            
            taz.gradeSchoolTeachingShare=1; 
            
            tazData.put(taz.zoneNumber, taz);
        }
    }

    /**
     * The default method to populate a TazManager.
     *
     * Accepts a ptRb and globalRb as arguments, such that the class can
     * be called from the LongDistanceWorker.
     *
     * @param ptRb the pt ResourceBundle.
     * @param globalRb the global ResourceBundle.
     *
     *
     */
    private int[] readHouseholdFile(ResourceBundle ptRb, ResourceBundle globalRb) {
        String fileName = ResourceUtil.getProperty(ptRb, "spg2.synpopH");
        String alphaName = globalRb.getString("alpha.name");

        int zones = getMaxTazNumber();
        int[] households = new int[zones + 1];

       BufferedReader reader;

       try {
           reader = new BufferedReader(new FileReader(
                fileName));
           HashMap<String, Integer> positions = new HashMap<String, Integer>();
           String[] header = reader.readLine().split(",");
           for (int i = 0; i < header.length; ++i) {
               positions.put(header[i], i);
           }

           String line;
           while ((line = reader.readLine()) != null) {
               String[] fields = line.split(",");
                int taz = parseInt(fields[positions.get(alphaName)]);
                households[taz] += 1;
           }

       } catch (IOException e) {
           String error = "Could not read households";
           logger.fatal(error);
           throw new ModelException(error);
       }

       return households;

      }

    /**
     * Read in parking costs from the parking cost file and update the
     * TazManager data with parking costs.
     *
     * @param appRb resource bundle
     * @param globalRb resource bundle
     * @param fileName  parking cost file
     */
    public abstract void setParkingCost(ResourceBundle appRb, ResourceBundle globalRb,String fileName);

    /**
     * Update the households in every TAZ with the households contained in the
     * array. The household array should be indexed by external TAZ.
     *
     * @param households
     *            An array containing households per TAZ.
     */
    public void setHouseholds(int[] households) {
        Enumeration tazEnum = tazData.elements();
        while (tazEnum.hasMoreElements()) {
            Taz thisTaz = (Taz) tazEnum.nextElement();
            thisTaz.households = households[thisTaz.zoneNumber];
            if (logger.isDebugEnabled()) {
                logger.debug("households in zone " + thisTaz.zoneNumber + " = "
                        + households[thisTaz.zoneNumber]);
            }
        }
    }

     /**
     * Update the number of households by TazOLD.
     *
     * @param households
     *            array of household
     */
    public void updateHouseholds(PTHousehold[] households) {
        for (PTHousehold household : households) {
            Taz taz = tazData.get((int) household.homeTaz);
            if (taz != null) {
                taz.households += 1;
            } else {
                logger.error("We are not supposed to model the homeless.");
            }
        }
    }


    public void updateWorkersFromSummary(String fileName) {
        CSVFileReader reader = new CSVFileReader();
        TableDataSet empTable;
        try {
            empTable = reader.readFile(new File(fileName));
        } catch (IOException e) {
            throw new RuntimeException("Could not read file " + fileName);
        }
        String[] header = empTable.getColumnLabels();
        for(int row=1; row <= empTable.getRowCount(); row++){
            int zone = (int) empTable.getValueAt(row, header[0]);
            Taz taz = tazData.get(zone);

            if (taz == null) {
                logger.error("What's up with taz " + zone);
                continue;
            }

            for(int empCol = 1; empCol < header.length; empCol++){
                taz.employment.put(header[empCol], empTable.getValueAt(row, header[empCol]));
            }

        }
    }


    /**
     * Get the taz data as an array, sorted in ascending order from lowest zone
     * number to highest zone number.
     *
     * @return The sorted taz array.
     */
    public Taz[] getTazArray() {

        if (tazs == null) {
            tazs = new Taz[tazData.size()];
            int count = 0;
            for (Object t : tazData.values()) {
                Taz taz = (Taz) t;
                tazs[count] = taz;
                ++count;
            }
            Arrays.sort(tazs);
        }
        return tazs;
    }

    /**
     * Return an array of external TAZs. Use this method if using the array to
     * set up a Matrix object.
     *
     * @return An array of external TAZ numbers, sorted in order from lowest to
     *         highest. The array will be indexed starting from element 1.
     */
    public int[] getExternalNumberArrayOneIndexed() {
        if (externalsOne == null) {
            externalsOne = new int[tazData.size() + 1];
            int count = 1;
            for (Integer i : tazData.keySet()) {
                externalsOne[count] = i;
                ++count;
            }
            Arrays.sort(externalsOne);
        }
        return externalsOne;
    }

    /**
     * Return an array of external TAZs.
     *
     * @return An array of external TAZ numbers, sorted in order from lowest to
     *         highest. The array will be indexed starting from element 0.
     */
    public int[] getExternalNumberArrayZeroIndexed() {
        if (externalsZero == null) {
            externalsZero = new int[tazData.size()];
            int count = 0;
            for (Integer i : tazData.keySet()) {
                externalsZero[count] = i;
                ++count;
            }
            Arrays.sort(externalsZero);
        }

        return externalsZero;
    }

    /**
     * Check if the TazManager data contains this TAZ.
     *
     * @param zoneNumber
     *            The number of the taz to check.
     * @return True if contains zone, else false.
     */
    public boolean hasTaz(int zoneNumber) {
        return tazData.containsKey(new Integer(zoneNumber));
    }

    public static TableDataSet loadTableDataSet(ResourceBundle rb,
                                                String fileName) {
        try {
            String tazFile = ResourceUtil.getProperty(rb, fileName);

            CSVFileReader reader = new CSVFileReader();
            TableDataSet table = reader.readFile(new File(tazFile));


            return table;
        } catch (IOException e) {
            logger.fatal("Can't find TazData input table " + fileName);
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Write the employment file.
     *
     * @param fileName  employment file
     */
    public void writeEmploymentFile(String fileName) {
        Enumeration tazEnum = tazData.elements();

        logger.info("Writing employment by TAZ and industry to " + fileName);
        if(alphaName == null){
            logger.warn("No alpha name was set, using 'TAZ' as the default");
        }
        BufferedWriter fileWriter;
        
        try {
            fileWriter = new BufferedWriter(new FileWriter(new File(fileName)));

            fileWriter.write(alphaName);
            for(String empCat : empIndustryLabels){
                fileWriter.write("," + empCat);
            }
            fileWriter.write(",Total\n");

            while (tazEnum.hasMoreElements()) {
                Taz taz = (Taz) tazEnum.nextElement();
                double total = taz.getTotalEmployment();
                String lineToWrite = "" + taz.zoneNumber;
                for(String empCat : empIndustryLabels){
                    Float tazEmp = taz.employment.get(empCat);
                    if(tazEmp != null)
                        lineToWrite += "," + tazEmp;
                    else
                        lineToWrite += ",0";
                }
                lineToWrite += "," + total + "\n";
                fileWriter.write(lineToWrite);
            }
            fileWriter.close();
        } catch (IOException e) {
            String message = "Unable to create the employment file.";
            logger.fatal(message);
            throw new RuntimeException(message);
        }
    }

    /**
     * Return the largest Taz number.
     * @return maxZone maximum zone number
     */
    public int getMaxTazNumber() {
        int maxZone = 0;

        for (Taz taz : tazData.values()) {
            maxZone = maxZone > taz.zoneNumber ? maxZone : taz.zoneNumber;
        }

        return maxZone;
    }

    public void setAlphaZoneName(String name){
        this.alphaName = name;
    }

    public static void main(String[] args) throws Exception {
        ResourceBundle appRb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        String tazManagerClass = ResourceUtil.getProperty(appRb,"sdt.taz.manager.class");
        Class tazClass = null;
        TazManager tazManager = null;
        try {
            tazClass = Class.forName(tazManagerClass);
            tazManager = (TazManager) tazClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
            throw new RuntimeException(e);
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        if (tazManager != null) {
            tazManager.readData(globalRb, appRb);
        }
        logger.info("Finished reading TazData Table");

    }
}
