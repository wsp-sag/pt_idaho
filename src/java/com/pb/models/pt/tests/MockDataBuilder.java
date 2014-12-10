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
 * Created on Mar 8, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt.tests;

import com.pb.models.pt.IncomeSegmenter;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.Pattern;
import com.pb.models.pt.PatternChoiceModel;
import com.pb.models.pt.PersonType;
import com.pb.models.pt.TazManager;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Random;

/**
 * Randomly select elements of an Enumeration.
 * 
 */
class Enums {
    private static Random rand = new Random();

    public static <T extends Enum<T>> T random(Class<T> ec) {
        return random(ec.getEnumConstants());
    }

    public static <T> T random(T[] values) {
        return values[rand.nextInt(values.length)];
    }
}

public class MockDataBuilder {
    static Logger logger = Logger.getLogger(MockDataBuilder.class);

    private static int personCount = 0;

    private static int householdCount = 0;

    private ArrayList<PTHousehold> households = new ArrayList<PTHousehold>();

    private ArrayList<PTPerson> persons = new ArrayList<PTPerson>();


    public MockDataBuilder() {
        IncomeSegmenter.setIncomeCategoryRanges(20000,60000);
    }

    /**
     * Create a mock synthetic householdssuitable for testing.
     * 
     * Households have the following dimensions:
     * 
     * persons: 1 -> 6
     * 
     * workers: 0 -> 3
     * 
     * income: 1 -> 4
     * 
     * residence type: single family versus multi-family
     */
    private void createHouseholds() {
        households.clear();

        int n = 0;
        try {
            for (byte p = 1; p <= 2; ++p) {
                for (byte w = 0; w <= 3 && w <= p; ++w) {
                    for (byte i = 1; i <= 4; ++i) {
                        for (byte r = 0; r <= 1; ++r) {
                            PTHousehold hh = new PTHousehold();
                            hh.ID = ++n;
                            hh.size = p;
                            hh.workers = w;
                            hh.income = i;
                            hh.singleFamily = r == 0;

                            households.add(hh);
                        }
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            logger.fatal("Initialized " + n + " households before running out"
                    + " of memory.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the mock household array.
     */
    public PTHousehold[] getHouseholds() {
        return households.toArray(new PTHousehold[households.size()]);
    }

    /**
     * Create mock persons.
     * 
     * Each mock person is createdly randomly.
     */
    private void createPersons(TazManager tazdata) {
        persons.clear();

        Random r = new Random();

        for (PTHousehold household : households) {
            for (int i = 0; i < household.persons.length; ++i) {
                PTPerson person = new PTPerson();

                person.hhID = household.ID;
                person.employed = r.nextBoolean();
                person.student = r.nextBoolean();
                person.female = r.nextBoolean();

                if (person.employed) {
                    person.occupation = Enums.random(PTOccupation.class);
                }


                // age = 1->80
                person.age = (byte) (r.nextInt(80) + 1);

                // work tazs
                if (person.employed) {
                    boolean foundTaz = false;
                    while (!foundTaz) {
                        short randomTaz = (short) (r.nextInt(4200) + 1);
                        if (tazdata.hasTaz(randomTaz)) {
                            person.workTaz = randomTaz;
                            foundTaz = true;
                        }
                    }
                }


                household.persons[i] = person;
            }
        }
    }

    /**
     * Build a mock person.
     *
     * @param household
     * @param age
     * @param female
     * @param employed
     * @param word
     * @return PTPerson
     */
    public static PTPerson personFactory(PTHousehold household, int age,
            boolean female, boolean employed, String word) {
        personCount = household.size;

        PTPerson person = new PTPerson();
        person.setPattern(new Pattern(word));

        household.homeTaz = (short) 1;

        person.memberID = --personCount;
        person.workTaz = (short) 10;
        person.age = (byte) age;
        person.employed = employed;
        person.segment = (byte) IncomeSegmenter.calcLogsumSegment(household.income,household.autos,household.workers);
        person.weekdayTours = PatternChoiceModel.convertToTours(household,
                person, person.getPattern());

        if (employed) {
            person.personType = PersonType.WORKER;
        } else if (age < 18) {
            person.personType = PersonType.STUDENTK12;
        }
        
        int first = 5;

        int last = 19;

        person.initScheduler(last - first + 1);

        person.orderTours();
        person.prioritizeTours();
        
        return person;
    }


    public static PTHousehold householdFactory(int autos, int income, int members) {
        PTHousehold household = new PTHousehold();

        household.ID = ++householdCount;
        household.autos = (byte) autos;
        household.income = income;
        household.homeTaz = 1;
        household.persons = new PTPerson[members];

        return household;
    }

    /**
     * Get the person array.
     */
    public PTPerson[] getPersons() {
        return persons.toArray(new PTPerson[persons.size()]);
    }

    /**
     * Create mock households and persons.
     * 
     * @param tazData
     *            Taz data.
     */
    public void createMockData(TazManager tazData) {
        createHouseholds();
        createPersons(tazData);
    }

}
