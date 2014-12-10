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

import com.pb.models.pt.ldt.LDTourPatternType;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.Serializable;

/** 
 * A class for a Household in PT
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class PTHousehold implements Comparable, Serializable{

//     public static int numberOfCurrentHousehold;
     final transient Logger logger = Logger.getLogger(PTHousehold.class);

     public static final int SEGMENTS = 9;

     public int ID;
     public byte size;
     public byte autos;
     public byte workers;
     public int income;
     public boolean singleFamily;
     public boolean multiFamily;
     public short homeTaz;
     
     private boolean visitor=false;
     
     public static final long serialVersionUID = 1;
     
     // stores whether household made complete HOUSEHOLD LD tours 
     // Refers to a two-week period, not necessarily to the travel day
     // the pattern indicates whether or not travel occurs on the model day
     public boolean ldHouseholdTourIndicator;
     public LDTourPatternType ldHouseholdTourPattern; 
      
     public PTPerson[] persons;
     
     //not sure if we'll have the following
//     public int fullWorkers;
//     public int partWorkers;

     //for sorting
     public boolean idSort = false;
     public boolean segmentSort = true;
     
     //constructor
     public PTHousehold(){

     }

    /**
     * Count the number of adults in the household.
     * 
     * @return Number of adults.
     */
    public int getAdultCount() {
        return getCohortCount(PTPerson.ADULT_AGE, 999);
    }
    
    /**
     * Count the number of person in the housing within the age cohort. 
     * @param youngest age of youngest person
     * @param oldest age of oldest person
     * @return Number of persons in the cohort.
     */
    public int getCohortCount(int youngest, int oldest) {
        int n = 0;
        
        for (PTPerson person : persons) {
            int age = person.getAge(); 
            if (age >= youngest && age <= oldest) {
                n += 1;
            }
        }
        
        return n;
    }
    

    /**
     * Get the number of autos.
     * 
     * @return Number of autos.
     */
    public int getAutoCount() {
        return autos;
    }
    
    /**
     * Get the number of workers.
     * 
     * @return Number of workers.
     */
    public int getWorkerCount() {
        return workers;
    }
    
    /**
     * Get the Imcome of the HH.
     * 
     * @return Income.
     */
    public int getIncome() {
        return income;
    }
    
    public boolean isVisitor() {
        return visitor;
    }
    
    public void setVisitor (boolean visitor) {
        this.visitor =visitor;
    }
    /**
     * Count the number of non-working adults in the household.
     * 
     * @return Number of non-working adults.
     */
    public int getNonWorkingAdultCount() {
        int n = 0;
        
        for (PTPerson person : persons) {
            if (person.isAdult() && !person.isWorker()) {
                n += 1;
            }
        }
        
        return n;
    }


    /*
     * to sort households
     */
     public int compareTo(Object household){
     	  
          PTHousehold h = (PTHousehold)household;

              int segment = IncomeSegmenter.calcLogsumSegment(h.income, h.autos, h.workers);
              int thisSegment = IncomeSegmenter.calcLogsumSegment(this.income, this.autos, this.workers);
    
              if(thisSegment<segment) return -1;
              else if(thisSegment>segment) return 1;
              else return 0;
    }
        
     
     public String toString() {
         return "Houshold: " + ID;
     }

     public void print(){
          logger.info("");
          logger.info("HOUSEHOLD INFO: ");
          logger.info("ID=               "+     ID);
          logger.info("size=             "+        size);            
          logger.info("autos=            "+        autos);           
          logger.info("workers=          "+     workers);         
          logger.info("income=           "+     income);          
          logger.info("singleFamily=     "+     singleFamily);
          logger.info("multiFamily=      "+     multiFamily); 
          logger.info("homeTaz=          "+     homeTaz);         
     }

     /**
      * Summarize the household.
      * 
      * @return String household summary string
      */
    public String summary() {
         String string = "household: " + ID + " = {";
         
         string += " size: " + size + ",";
         string += " autos: " + autos + ",";
         string += " workers: " + workers + ",";
         string += " income: " + income + ",";
         string += " single family: " + singleFamily + ",";
         string += " home taz: " + homeTaz + "}";
         
         return string;
    }
     
     //to write to a text file, csv format
    public void printCSV(PrintWriter file){

          file.println(
               ID+","+
               +size+","
               +autos+","
               +workers+","
               +income+","
               +booleanToInt(singleFamily)+","
               +booleanToInt(multiFamily)+","
               +homeTaz
          );
    }
     
    public int booleanToInt(boolean boo){
         return (!boo) ? 0 : 1;
    }

    //In order to get a valid answer from this method, you must
    //ensure that your household has been through the LDTPatternModel
    //If it hasn't, then the method returns false but this could change
    //after the hh goes thru the LDTPatternModel.choosePattern method.
    public boolean isHhMakingALdtOnModelDay(){
        return ldHouseholdTourPattern != null && !ldHouseholdTourPattern.equals(LDTourPatternType.NO_TOUR);
    }
     
    public static void main(String[] args){
         
    }

}



