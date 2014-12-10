/*
 * Copyright  2006 PB Consult Inc.
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
package com.pb.models.pt.ldt.tests;

import org.apache.log4j.Logger;

import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.PTOccupationReferencer;
import com.pb.models.pt.tests.PTOccupation;
import com.pb.models.pt.ldt.LDTour;
import com.pb.models.pt.ldt.LDTourPatternType;
import com.pb.models.pt.ldt.LDTourPurpose;

/**
 * @author Erhardt
 * @version 1.0 May 15, 2006
 *
 */
public class LDMockDataBuilder {
    protected static Logger logger = Logger.getLogger(LDMockDataBuilder.class);

    protected static PTOccupationReferencer myReferencer = PTOccupation.NO_OCCUPATION;

    /**
     * Creates a mock household for testing.  
     * 
     * @param id
     * @return
     */
    public static PTHousehold getHousehold(int id) {
        PTHousehold hh = new PTHousehold();
        
        if (id == 578) {
            hh.ID = 584;
            hh.size = 4;
            hh.autos = 2;
            hh.workers = 2;
            hh.persons = getPersonsForHousehold(id, 4);
            hh.multiFamily = false;
            hh.income = 106000;
            hh.homeTaz = 672;
        }   
        else {
            logger.error("Can't create household " + id);
        }
        
        return hh;
    }
    
    /**
     * Creates mock persons for testing.
     * 
     * @param id
     * @param numPersons
     * @return
     */
    public static PTPerson[] getPersonsForHousehold(int id, int numPersons) {
        PTPerson[] persons = new PTPerson[numPersons];
        
        for (int i = 0; i < persons.length; i++) {
            persons[i] = new PTPerson();
            persons[i].hhID = id;
            persons[i].memberID = i + 1;
        }               
        
        if (id == 578) {
            // person 1
            persons[0].employed = true;
            persons[0].student = false;
            persons[0].age = 54;
            persons[0].female = false;
            persons[0].occupation = myReferencer.getOccupation("Professional");
            persons[0].industry = 5;
            
            // person 2
            persons[1].employed = true;
            persons[1].student = false;
            persons[1].age = 36;
            persons[1].female = true;
            persons[1].occupation = myReferencer.getOccupation("Service");
            persons[1].industry = 8;
            
            // person 3
            persons[2].employed = false;
            persons[2].student = true;
            persons[2].age = 4;
            persons[2].female = true;
            
            // person 4
            persons[3].employed = false;
            persons[3].student = false;
            persons[3].age = 0;
            persons[3].female = true;            
        }
        
        return persons;
    }
    
    /** 
     * Creates a mock tour for testing.  
     * 
     * @param hhId
     * @return
     */
    public static LDTour getTour(int hhId) {
        
        LDTour tour = null; 
        
        if (hhId == 578) {
            PTHousehold hh = getHousehold(578);
            PTPerson p = hh.persons[0];            
            tour = new LDTour(0, hh, p, LDTourPurpose.WORKRELATED, LDTourPatternType.BEGIN_TOUR);
        }        
        
        return tour;
    }
}
