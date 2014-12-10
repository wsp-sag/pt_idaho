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

import org.apache.log4j.Logger;

import com.pb.common.matrix.Matrix;

import static com.pb.models.pt.PersonType.*;

public class PatternModelPersonAttributes {

    final static Logger logger = Logger
    .getLogger(PatternModelPersonAttributes.class);

    final static int hhTypes=15;
    PersonType type;
    
    float hType1=0;
    float hType2=0;
    float hType3=0;
    float hType4=0;
    float hType5=0;
    float hType6=0;
    float hType7=0;
    float hType8=0;
    float hType9=0;
    float hType10=0;
    float hType11=0;
    float hType12=0;
    float hType13=0;
    float hType14=0;
    float hType15=0;

    float age1=0;
    float age2=0;
    float age3=0;
    float age4=0;
    float age5=0;
    float age15=0;
    float age16=0;
    float age17=0;
    float age11to14=0;
    float age14to16=0;
    float age16to18=0;
    float agelt25=0;
    float age25to35=0;
    float age35to45=0;
    float age45to55=0;
    float age55to65=0;
    float age65plus=0;
    
    float worker=0;
    float female=0;
    
    float autos0=0;
    float autosltadults=0;
    float autosgtadults=0;
    float adultsle1=0;
    float adultsge2=0;
    float autosltworkers=0;
    float adultsgtworkers=0;
    
    float incLow=0;
    float incHi=0;
    
    float workDist1to2p5=0;
    float workDist2p5to5=0;
    float workDist5to10=0;
    float workDist10to25=0;
    float workDist25to50=0;
    float workDist50plus=0;
    
    int nWorkTours = 0;
    int nSchoolTours = 0;
    int nShopTours = 0;
    int nRecreationTours = 0;
    int nOtherTours = 0;
    
    float dcLogsumCollege = 0; 
    float dcLogsumShop = 0;
    float dcLogsumRec = 0; 
    float dcLogsumOther = 0; 

    /**
     * Default constructor, no values will be set until a call to
     * calculateAttributes()
     *
     */
    public PatternModelPersonAttributes(){
        
    }

    
    /**
     * Code attributes for a household, person.  This method will
     * initialize all attributes to 0 and recode them for a particular
     * person, household.
     * 
     * @param h Household.
     * @param p Person.
     * @param distance a matrix holding TAZ distances
     */
    public void calculateAttributes(PTHousehold h, PTPerson p, Matrix distance){

        worker=0;
        female=0;
        if(p.employed)
            worker=1;
        if(p.female)
            female=1;
        
        autos0=0;
        autosltadults=0;
        autosgtadults=0;
        autosltworkers=0;
        adultsle1=0;
        adultsge2=0;
        adultsgtworkers=0;
        
        int chld = 0;
        int adultworkers = 0;
        for (PTPerson person : h.persons) {
            if (person.age < 18) ++chld;
            if (person.age >= 18 && person.employed)
                ++adultworkers;
        }
        int adults = h.size - chld;
        
        if(h.autos==0)
            autos0=1;
        else if(h.autos<adults)
            autosltadults=1;
        else if(h.autos>adults)
            autosgtadults=1;
            
        
        if(h.autos>0 && h.autos<h.workers)
            autosltworkers=1;
        
        if(adults<=1)
            adultsle1=1;
        else if(adults>=2)
            adultsge2=1;
        
        if(adults>adultworkers)
            adultsgtworkers=1;
        
        incLow=0;
        incHi=0;
        
        if(h.income<15000)
            incLow=1;
        else if(h.income>60000)
            incHi=1;

        calculateHouseholdType(h);
        calculateAgeRange(p);
        calculateWorkDistance(p,distance);
        calculateNumberOfTours(p);
        calculateDcLogsums(h); 
        
        if (p.employed && p.age>=18 && !p.student) {
            type = WORKER; 
        } else if (p.age <= 5) {
            type = PRESCHOOL;
        } else if (p.student) {
            if (p.age > 17) {
                type = STUDENTCOLLEGE;
            } else {
                type = STUDENTK12;
            }
        } else {
            type = NONWORKER;
        }
        
    }
    /**
     * Calculate the household type (hType) for use in the pattern model
     * utility equation.  Use this method for adults only. Type of household
     * is as follows:
     * 
     *    1 one adult, non worker, no children
     *    2 one adult, non worker, 1+ children, no preschooler
     *    3 one adult, non worker, 1+ children, 1+ preschooler
     *    4 one adult, worker, no children
     *    5 one adult, worker, 1+ children, no preschooler
     *    6 one adult, worker, 1+ children, 1+ preschooler
     *    7 two+ adults, workers=adults, no children
     *    8 two+ adults, workers=adults, 1+ children, no preschooler
     *    9 two+ adults, workers=adults, 1+ children, 1+ preschooler
     *   10 two+ adults, workers<adults, no children
     *   11 two+ adults, workers<adults, 1+ children, no preschooler
     *   12 two+ adults, workers<adults, 1+ children, 1+ preschooler
     *   13 two+ adults, no workers, no children
     *   14 two+ adults, no workers, 1+ children, no preschooler
     *   15 two+ adults, no workers, 1+ children, 1+ preschooler
     * 
     * 
     * @param h The household.
     **/
    public void calculateHouseholdType(PTHousehold h){
        
        hType1=0;
        hType2=0;
        hType3=0;
        hType4=0;
        hType5=0;
        hType6=0;
        hType7=0;
        hType8=0;
        hType9=0;
        hType10=0;
        hType11=0;
        hType12=0;
        hType13=0;
        hType14=0;
        hType15=0;

        int size = h.size;
        int wrkr = h.workers;
        int chld = 0;
        int pres = 0;
        for (PTPerson person : h.persons) {
            if (person.age < 18) ++chld;
            if (person.age <= 5) ++pres;
        }
        int adult = size - chld;
        
        if(adult==1)
            if(wrkr==0)
                if(chld==0)
                    hType1=1; //one adult, non worker, no children
                else
                    if(pres==0)
                        hType2=1; //one adult, non worker, 1+ children, no preschooler
                    else
                        hType3=1; //one adult, non worker, 1+ children, 1+ preschooler
            else
                if(chld==0)
                    hType4=1; //one adult, worker, no children
                else
                    if(pres==0)
                        hType5=1; //one adult, worker, 1+ children, no preschooler
                    else
                        hType6=1; //one adult, worker, 1+ children, 1+ preschooler
        else
            if(wrkr==0)
                if(chld==0)
                    hType13=1; // two+ adults, no workers, no children
                else
                    if(pres==0)
                        hType14=1; //two+ adults, no workers, 1+ children, no preschooler
                    else
                        hType15=1; //two+ adults, no workers, 1+ children, 1+ preschooler
            else if(wrkr<adult)
                if(chld==0)
                    hType10=1; //two+ adults, workers<adults, no children
                else
                    if(pres==0)
                        hType11=1; //two+ adults, workers<adults, 1+ children, no preschooler
                    else
                        hType12=1; // two+ adults, workers<adults, 1+ children, 1+ preschooler
            else
                if(chld==0)
                    hType7=1; //two+ adults, workers=adults, no children
                else
                    if(pres==0)
                        hType8=1; //two+ adults, workers=adults, 1+ children, no preschooler
                    else
                        hType9=1; // two+ adults, workers=adults, 1+ children, 1+ preschooler
    }
 
    /**
     * Calculate the age range for the person.
     * 
     * @param p  The person.
     */
    public void calculateAgeRange(PTPerson p){

        age1=0;
        age2=0;
        age3=0;
        age4=0;
        age5=0;
        age15=0;
        age16=0;
        age17=0;
        age11to14=0;
        age14to16=0;
        age16to18=0;
        agelt25=0;
        age25to35=0;
        age35to45=0;
        age45to55=0;
        age55to65=0;
        age65plus=0;

        if(p.age<=1)
            age1=1;
        else if(p.age==2)
            age2=1;
        else if(p.age==3)
            age3=1;
        else if(p.age==4)
            age4=1;
        else if(p.age==5)
            age5=1;
        else if(p.age==15)
            age15=1;
        else if(p.age==16)
            age16=1;
        else if(p.age==17)
            age17=1;
        
        if(p.age>11 && p.age<14){
            age11to14=1;
            agelt25=1;
        }else if(p.age>=14 && p.age<16){
            age14to16=1;
            agelt25=1;
        }else if(p.age>=16 && p.age<18){
            age16to18=1;
            agelt25=1;
        }else if(p.age>=18 && p.age<25){
            agelt25=1;
        }else if(p.age>=25 && p.age<35)
            age25to35=1;
        else if(p.age>=35 && p.age<45)
            age35to45=1;
        else if(p.age>=45 && p.age<55)
            age45to55=1;
        else if(p.age>=55 && p.age<65)
            age55to65=1;
        else if(p.age>=65)
            age65plus=1;
    }
    
    /**
     * Calculate the work distance variables.  If the person is either unemployed,
     * or if the home taz or work taz data items are 0, the method will return
     * with work distance variables all set to 0.
     * 
     * @param p  The person (should be an employed person with a home TAZ and
     * work TAZ location.
     * @param distance A distance matrix for TAZs.
     */
    public void calculateWorkDistance(PTPerson p, Matrix distance){

        workDist1to2p5=0;
        workDist2p5to5=0;
        workDist5to10=0;
        workDist10to25=0;
        workDist25to50=0;
        workDist50plus=0;

        if(!p.employed)
            return;
        
        int workTaz = p.workTaz;
        int homeTaz = p.homeTaz;
        
        if(workTaz ==0 || homeTaz ==0){
            logger.debug("Error in calculateWorkDistance() for Household "+p.hhID+" person "+p.memberID);
            logger.debug("HomeTaz "+homeTaz+ " workTaz "+workTaz);
            return;
   
        }
        
        float dist = distance.getValueAt(homeTaz,workTaz);
    
        //code the distance variables
        if(dist>1.0 && dist<= 2.5)
            workDist1to2p5=1;
        else if(dist>2.5 && dist<=5.0)
            workDist2p5to5=1;
        else if(dist>5.0 && dist<=10.0)
            workDist5to10=1;
        else if(dist>10.0 && dist<25.0)
            workDist10to25=1;
        else if(dist>25.0 && dist<50.0)
            workDist25to50=1;
        else if(dist>=50.0)
            workDist50plus=1;
    
    }

    public void calculateNumberOfTours(PTPerson person) {
        nWorkTours = person.getTourCount(ActivityPurpose.WORK);
        nSchoolTours = person.getTourCount(ActivityPurpose.GRADESCHOOL)
                + person.getTourCount(ActivityPurpose.COLLEGE);
        nShopTours = person.getTourCount(ActivityPurpose.SHOP);
        nRecreationTours = person.getTourCount(ActivityPurpose.RECREATE);
    }
    
    /** 
     * Calculates the market segment to use for destination choice logsums.   
     * 
     * @param h Household
     */
    private void calculateDcLogsums(PTHousehold h) {
        int segment = IncomeSegmenter.calcLogsumSegment(h.getIncome(), h.getAutoCount(), h.getWorkerCount());

        dcLogsumCollege = TourDestinationChoiceLogsums.getLogsum(ActivityPurpose.COLLEGE, segment, h.homeTaz);     
        dcLogsumShop    = TourDestinationChoiceLogsums.getLogsum(ActivityPurpose.SHOP, segment, h.homeTaz); 
        dcLogsumRec     = TourDestinationChoiceLogsums.getLogsum(ActivityPurpose.RECREATE, segment, h.homeTaz); 
        dcLogsumOther   = TourDestinationChoiceLogsums.getLogsum(ActivityPurpose.OTHER, segment, h.homeTaz); 
    }
}
