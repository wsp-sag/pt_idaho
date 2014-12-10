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
 * Created on Aug 23, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt.tests;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.StopPurposeModel;
import com.pb.models.pt.Tour;
import static com.pb.models.pt.tests.MockDataBuilder.householdFactory;
import static com.pb.models.pt.tests.MockDataBuilder.personFactory;
import com.pb.models.utils.Tracer;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.Random;
import java.util.ResourceBundle;

public class IntermediateStopPurposeTest extends TestCase {

    private Logger logger = Logger.getLogger(IntermediateStopPurposeTest.class);

    private StopPurposeModel model;

    private Random random;

    private Tracer tracer = Tracer.getTracer();

    public IntermediateStopPurposeTest() {
        super();

        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");

        tracer.setTrace(true);

        random = new Random();

        model = new StopPurposeModel(rb);
    }

    public void testProblem0() {
        logger.info("General test of the intermediate stop purpose model.");

        PTHousehold household = householdFactory(2, 16000, 1);
        PTPerson person = personFactory(household, 17, false, false,
                "hoshsoh");

        for (Tour tour : person.weekdayTours) {
            tour.print();
            
            model.selectStopPurpose(tour, person, random);
            
            tour.print();
        }
        
        logger.info("General test completed successfully.");
    }
    
    public void testProblem1() {
        logger.info("General test of the intermediate stop purpose model.");

        PTHousehold household = householdFactory(2, 16000, 1);
        PTPerson person = personFactory(household, 17, true, true,
                "howhsohrh");

        for (Tour tour : person.weekdayTours) {
            tour.print();

            model.selectStopPurpose(tour, person, random);

            tour.print();
        }

        logger.info("General test completed successfully.");
    }
    
    public void testProblem2() {
        logger.info("General test of the intermediate stop purpose model.");

        PTHousehold household = householdFactory(2, 16000, 1);
        PTPerson person = personFactory(household, 17, true, true,
                "horhrh");

        for (Tour tour : person.weekdayTours) {
            tour.print();

            model.selectStopPurpose(tour, person, random);

            tour.print();
        }

        logger.info("General test completed successfully.");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(IntermediateStopPurposeTest.class);
    }

}
