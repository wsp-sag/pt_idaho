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

import com.pb.common.util.SeededRandom;
import com.pb.common.util.ObjectUtil;
import com.pb.models.pt.ldt.LDTourPatternType;
import com.pb.models.pt.ldt.LDTourPurpose;
import com.pb.models.pt.tests.PTOccupation;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.Serializable;

/**
 * A class containing all information about a person
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class PTPerson implements Serializable, Comparable {
    private transient Logger logger = Logger.getLogger(PTPerson.class);

    public final static int ADULT_AGE = 18;

    public boolean employed; // will be true if 'RLABOR' code = 1,2,4 or 5.
    public boolean student;
    public byte age;
    public boolean female;
    public Enum occupation;
    public byte industry; // these are less general than occupation and
                                        // correspond to the Ed Industry categories

    public int memberID;
    public int hhID;
    public PersonType personType;
    public int workOccupation;
    public short workTaz;

    public Pattern weekdayPattern;
    public double weekdayPatternLogsum; 
    public Tour[] weekdayTours;
    public Tour[] weekdayWorkBasedTours;

// stores whether person made LD tours of the types in the LDTourPurpose
    // enumeration.  Refers to a two-week period, not necessarily to the travel day
    // The pattern indicates whether or not travel occurs on the model day.
    public boolean[] ldTourIndicator = new boolean[LDTourPurpose.values().length];
    public LDTourPatternType[] ldTourPattern = new LDTourPatternType[LDTourPurpose.values().length];

    // These need to be added
    public short homeTaz;

    public byte segment;

    private Scheduler scheduler;

    private int[] priority;

    public long randomSeed;

    // double workDCLogsum;
    //double schoolDCLogsum;

    //double shopDCLogsum;

    //double recreateDCLogsum;

    //double otherDCLogsum;

    //double workBasedDCLogsum;

    //double weekdayPatternLogsum;

    //double weekendPatternLogsum;




    // not sure if these are available in application
    // int worksFullTime;
    // int worksPartTime;

    // int studentFullTime;
    // int studentPartTime;





    // constructor
    public PTPerson() {

        ldTourIndicator = new boolean[LDTourPurpose.values().length];

    }


    /**
     * Summarize person into a string.
     */
    public String toString() {
        return "Person: " + (hhID + "_" + memberID);
    }

    public void print() {
        logger.info("");
        logger.info("PERSON INFO: ");
        logger.info("employed=                  " + employed);
        logger.info("student=                   " + student);
        logger.info("age=                       " + age);
        logger.info("female=                    " + female);
        logger.info("occupation=                " + occupation);
        // logger.info("workDCLogsum= "+workDCLogsum);
     //   logger.info("schoolDCLogsum=            " + schoolDCLogsum);
     //   logger.info("shopDCLogsum=              " + shopDCLogsum);
     //   logger.info("recreateDCLogsum=          " + recreateDCLogsum);
     //   logger.info("otherDCLogsum=             " + otherDCLogsum);
     //   logger.info("workBasedDCLogsum=         " + workBasedDCLogsum);
        logger.info("workTaz=                   " + workTaz);
        logger.info("industry=                  " + industry);
        logger.info("segment=                   " + segment);
    }

    /**
     * Summarize person
     * @return String summary string
     */
    public String summary() {
        String string = "Person " + memberID + " = { ";

        string += " householdId: " + hhID + ",";
        string += " homeTaz: " + homeTaz + ",";
        string += " employed: " + employed + ",";
        string += " student: " + student + ",";
        string += " age: " + age + ",";
        string += " female: " + female + ",";
        string += " occupation: " + occupation + ",";
        string += " industry: " + industry + ",";
        string += " work taz: " + workTaz + ",";
        string += " segment: " + segment;
        if (weekdayPattern != null) {
            string += ", pattern: " + weekdayPattern;
        }
        string += " }";

        return string;
    }

    // to write to a text file, csv format
    public void printCSV(PrintWriter file) {
        try {
            file.println(hhID + "," + memberID + "," + booleanToInt(employed) + ","
                    + booleanToInt(student) 
                    + "," + age + "," + booleanToInt(female) + "," + occupation
                    + ","
                    // +workDCLogsum+","
                    + 0 + "," + 0 + ","
                    + 0 + "," + 0 + ","
                    + 0
                  //  + schoolDCLogsum + "," + shopDCLogsum + ","
                  //  + recreateDCLogsum + "," + otherDCLogsum + ","
                  //  + workBasedDCLogsum
                    + "," + workTaz

                    // +weekdayPattern+","
                    // +weekendPattern+","
                    + weekdayTours.length + ","
                    // +weekendTours.length+","
//                    + weekdayWorkBasedTours.length
            // +weekendWorkBasedTours.length+","
                    // TODO: Write pattern
                    );
        } catch (Exception e) {
            logger.error("Unable to write to person file.");
        }
    }

    public int booleanToInt(boolean boo) {
        return (!boo) ? 0 : 1;
    }

    public void orderTours() {
        for (int i = 0; i < weekdayTours.length; ++i) {
            weekdayTours[i].setOrder(i);
        }
    }

    /**
     * Prioritize the tours.
     * 
     * Tours are prioritized in order of tour precedence. Within a precedence
     * level, tours are prioritized in chronological order.
     */
    public void prioritizeTours() {
        int p = 0;
        priority = new int[weekdayTours.length];

        for (int i = 0; i < weekdayTours.length; ++i) {
            ActivityPurpose purpose = weekdayTours[i].primaryDestination.activityPurpose;
            if (purpose == ActivityPurpose.GRADESCHOOL
                    || purpose == ActivityPurpose.COLLEGE
                    || purpose == ActivityPurpose.WORK
                    || purpose == ActivityPurpose.WORK_BASED) {
                priority[p] = i;
                weekdayTours[i].setPriority(p);
                p += 1;
            }
        }

        for (int i = 0; i < weekdayTours.length; ++i) {
            ActivityPurpose purpose = weekdayTours[i].primaryDestination.activityPurpose;
            if (purpose == ActivityPurpose.SHOP) {
                priority[p] = i;
                weekdayTours[i].setPriority(p);
                p += 1;
            }
        }

        for (int i = 0; i < weekdayTours.length; ++i) {
            ActivityPurpose purpose = weekdayTours[i].primaryDestination.activityPurpose;
            if (purpose == ActivityPurpose.RECREATE) {
                priority[p] = i;
                weekdayTours[i].setPriority(p);
                p += 1;
            }
        }

        for (int i = 0; i < weekdayTours.length; ++i) {
            ActivityPurpose purpose = weekdayTours[i].primaryDestination.activityPurpose;
            if (purpose == ActivityPurpose.OTHER) {
                priority[p] = i;
                weekdayTours[i].setPriority(p);
                p += 1;
            }
        }
    }

    /**
     * Get a tour by its priority.
     * @param priority priority of tour
     * @return Tour tour that has this priority
     */
    public Tour getTourByPriority(int priority) {
        return weekdayTours[this.priority[priority]];
    }

    /**
     * Initilize the scheduler.
     * @param periods periods that will be scheduled
     */
    public void initScheduler(int periods) {
//        logger.info("Setting up scheduler for " + periods + " periods.");
        scheduler = new Scheduler(periods);
    }

    /**
     * Get the tour scheduler.
     * @return Scheduler Handle to the scheduler object
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Get tour the number of tours.
     * @return int number of weekday tours for all purposes
     */
    public int getTourCount() {
        return weekdayTours == null ? 0 : weekdayTours.length;
    }

    /**
     * Get tour the number of tours by the purpose type.
     * @param purpose ActivityPurpose
     * @return int number of tours for a particular purpose
     */
    public int getTourCount(ActivityPurpose purpose) {
        int count = 0;

        if (weekdayTours == null) {
            return count;
        }

        for (Tour tour : weekdayTours) {
            if (tour.primaryDestination.activityPurpose == purpose) {
                count += 1;
            }
        }

        return count;
    }
    /**
     * Get the occupation code for the person.
     * 
     * @return occupation code
     */
    public Enum getOccupation() {
        return occupation;
    }

    /**
     * Get the industry code for the person.
     * 
     * @return industry code
     */
    public int getIndustry() {
        return industry;
    }

    /**
     * Is the person an adult?
     * 
     * @return True if the person is an adult.
     */
    public boolean isAdult() {
        return age >= ADULT_AGE;
    }

    /**
     * Is the person an adult?
     * 
     * @return True if the person is an adult.
     */
    public boolean isWorker() {
        return employed;
    }

    /**
     * Get the age of the person.
     * 
     * @return The person's age.
     */
    public int getAge() {
        return age;
    }

    /**
     * Sort by hometaz, segment and work_occupation.
     */
    public int compareTo(Object person) {
        PTPerson p = (PTPerson) person;
        
        int cs = (p.homeTaz * 100) + (p.segment * 10) + p.workOccupation;
        int compositeSegment = (this.homeTaz * 100) + (this.segment * 10) + this.workOccupation;
        
        if (compositeSegment < cs)
            return -1;
        else if (compositeSegment > cs)
            return 1;
        else
            return 0;

    }

    public Pattern getPattern() {
        try {
            return (Pattern) weekdayPattern.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setPattern(Pattern pattern) {
        weekdayPattern = pattern;
    }

    public void setPatternLogsum(double logsum) {
        weekdayPatternLogsum = logsum; 
    }
    
    //In order to get a valid answer from this method, you must
    //ensure that your household has been through the LDTPatternModel
    //If it hasn't, then the method returns false but this could change
    //after the hh goes thru the LDTPatternModel.choosePattern method.
    public static boolean isPersonMakingALdtOnModelDay(PTPerson p){
        boolean travel = false;
        for(LDTourPatternType patternType : p.ldTourPattern){
            if(patternType != null){
                travel = !patternType.equals(LDTourPatternType.NO_TOUR);
            }
            if(travel) break;
        }
        return travel;
    }

    private static PTPerson createDummy(){
        PTPerson person = new PTPerson();
        person.hhID = 6000000;
        person.memberID = 1;
        person.female = true;
        person.age = 100;
        person.student = true;
        person.employed = true;

        person.industry = 45;
        person.occupation = PTOccupation.PAPER_PUSHER;
        person.personType = PersonType.WORKER;
        person.ldTourIndicator[0] = true;
        person.ldTourIndicator[1] = false;
        person.ldTourIndicator[2] = false;
        person.ldTourPattern[0] = LDTourPatternType.BEGIN_TOUR;
        person.ldTourPattern[1] = LDTourPatternType.NO_TOUR;
        person.ldTourPattern[2] = LDTourPatternType.NO_TOUR;
        person.workTaz = 5002;
        person.randomSeed = (long) (SeededRandom.getRandom() * Long.MAX_VALUE);
        return person;
    }

    public static void main(String[] args) {
        PTPerson person = PTPerson.createDummy();
        person.logger.info("Size of person: " + ObjectUtil.sizeOf(person));
    }


}
