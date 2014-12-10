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
package com.pb.models.pt.tests;

import com.pb.common.util.ObjectUtil;
import com.pb.common.util.SeededRandom;
import com.pb.models.pt.Pattern;
import com.pb.models.pt.PersonType;
import com.pb.models.pt.Scheduler;
import com.pb.models.pt.Tour;
import com.pb.models.pt.ldt.LDTourPatternType;
import com.pb.models.pt.ldt.LDTourPurpose;
import org.apache.log4j.Logger;

import java.io.Serializable;

/**
 * A class containing all information about a person
 *
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 *
 */
public class PTPerson_Test implements Serializable {
    private transient Logger logger = Logger.getLogger(PTPerson_Test.class);

        public final static int ADULT_AGE = 18;

        public boolean employed; // will be true if 'RLABOR' code = 1,2,4 or 5.
        public boolean student;
        public byte age;
        public boolean female;
        public Enum occupation;
        public byte industry; // these are less general than occupation and
                                            // correspond to the Ed Industry categories


        public int ID;
        public int memberID;
        public int hhID;
        public PersonType personType;

        public short workTaz;

        public Pattern weekdayPattern;
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




    public PTPerson_Test() {
        //ldTourIndicator = new boolean[LDTourPurpose.values().length];
     }

   private static PTPerson_Test createDummy(){
        PTPerson_Test person = new PTPerson_Test();
        person.hhID = 6000000;
        person.ID = 14000000;
        person.memberID = 1;

        person.age = 100;

        person.industry = 45;

        person.workTaz = 5002;

        person.personType = PersonType.WORKER;


        person.female = true;

        person.student = true;

        person.employed = true;

        person.ldTourIndicator[0] = true;

        person.ldTourIndicator[1] = false;

        person.ldTourIndicator[2] = false;

        person.occupation = PTOccupation.PAPER_PUSHER;
        person.ldTourPattern[0] = LDTourPatternType.BEGIN_TOUR;
        person.ldTourPattern[1] = LDTourPatternType.NO_TOUR;
        person.ldTourPattern[2] = LDTourPatternType.NO_TOUR;


        person.randomSeed = (long) (SeededRandom.getRandom() * Long.MAX_VALUE);
        return person;
    }

    public static void main(String[] args) {
        PTPerson_Test person = PTPerson_Test.createDummy();
        System.out.println("Size of person: " + ObjectUtil.sizeOf(person));
    }


}
