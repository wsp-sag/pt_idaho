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
 */
package com.pb.models.pt.tests;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.Pattern;
import com.pb.models.pt.PatternChoiceModel;
import com.pb.models.pt.PersonType;
import com.pb.models.pt.StopPurposeModel;
import com.pb.models.utils.Tracer;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * @author Andrew Stryker
 * @version 0.1
 */
public class StopPurposeModelTest extends TestCase {
    private Logger logger = Logger.getLogger(StopPurposeModelTest.class);

    private Tracer tracer = Tracer.getTracer();

    private StopPurposeModel model;

    private NotRandom notRandom = new NotRandom();

    public StopPurposeModelTest() {
        super();
        tracer.setTrace(true);

        logger.info("Running " + StopPurposeModelTest.class);

        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        model = new StopPurposeModel(rb);
    }

    /**
     * Test method for
     * com.pb.osmp.pt.StopPurposeModel#selectStopPurpose(com.pb.osmp.pt.Tour, com.pb.osmp.pt.PTPerson, java.util.Random)}.
     */
    public void testSelectStopPurpose() {
        PTHousehold household = new PTHousehold();
        PTPerson person = new PTPerson();
        person.hhID = 1;
        person.memberID = 1;
        tracer.tracePerson(person.hhID + "_" + person.memberID);
        person.personType = PersonType.PRESCHOOL;
        Pattern pattern = new Pattern("hochrohrh");

        household.persons = new PTPerson[1];
        household.persons[0] = person;
        person.weekdayPattern = pattern;
        person.weekdayTours = PatternChoiceModel.convertToTours(household,
                person, pattern);

        person.orderTours();
        person.prioritizeTours();

        model.selectStopPurpose(person.weekdayTours[0], person, notRandom);
    }

}
