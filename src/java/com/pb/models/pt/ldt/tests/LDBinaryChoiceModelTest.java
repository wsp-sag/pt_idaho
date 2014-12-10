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

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.TourDestinationChoiceLogsumManager;
import com.pb.models.pt.TourDestinationChoiceLogsums;
import com.pb.models.pt.ldt.LDBinaryChoiceModel;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * Class to test the LDT Binary Choice Model.
 * 
 * @author Erhardt
 * @version 1.0 May 15, 2006
 *
 */
public class LDBinaryChoiceModelTest extends TestCase {
    protected static Logger logger = Logger.getLogger(LDBinaryChoiceModelTest.class);
    
    private ResourceBundle rb;

    private LDBinaryChoiceModel model; 
    
    
    /**
     * Constructor for LDBinaryChoiceModelTest.
     * @param arg0
     */
    public LDBinaryChoiceModelTest(String arg0) {
        super(arg0);
    }
//
//    /**
//     * Gets the resource bundles and creates the model. 
//     * 
//     * @see TestCase#setUp()
//     */
//    protected void setUp() throws Exception {
//        logger.info("Testing LDBinaryChoiceModel.");
//        super.setUp();
//        
//        rb = ResourceUtil.getResourceBundle("ldt");
//        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
//        String alphaName = globalRb.getString("alpha.name");
//
//        TourDestinationChoiceLogsumManager dcLogsums = new TourDestinationChoiceLogsumManager();
//        TourDestinationChoiceLogsums.readLogsums(rb, alphaName);
//        
//        model = new LDBinaryChoiceModel(rb);
//        model.setTrace(true);
//    }
//    
//    /**
//     * Tests the utility calculation for HOUSEHOLD travel.  
//     *
//     */
//    public void testCalculateHouseholdUtility() {
//        logger.info("Test calculate household utility.");
//
//        int numTests = 1; 
//        PTHousehold[] hh = new PTHousehold[numTests];
//        double[] expected = new double[numTests];
//        double[] actual = new double[numTests];
//        
//        // household 0
//        hh[0] = LDMockDataBuilder.getHousehold(578);
//        expected[0] = -0.5857000052928925;
//        actual[0] = model.calculateHouseholdUtility(hh[0]);
//        
//        for (int i = 0; i < hh.length; ++i) {
//            try {
//                assertEquals(expected[i], actual[i]);
//            } catch (AssertionFailedError e) {
//                hh[0].print();
//                logger.info("For household " + hh[i].ID + ", HOUSEHOLD binary utility should be "
//                                + expected[i] + " but is "
//                                + actual[i] + ".");
//                throw e;
//            }
//        }
//    }
    
//    /**
//     * Tests the utility calculation for WORKRELATED travel.  
//     *
//     */
//    public void testCalculateWorkRelatedUtility() {
//        logger.info("Test calculate work related utility.");
//
//        int numTests = 1; 
//        PTHousehold[] hh = new PTHousehold[numTests];
//        double[] expected = new double[numTests];
//        double[] actual = new double[numTests];
//        
//        // household 0
//        hh[0] = LDMockDataBuilder.getHousehold(578);
//        expected[0] = 0.3066001534461975;
//        actual[0] = model.calculateWorkRelatedUtility(hh[0], hh[0].persons[0]);
//        
//        for (int i = 0; i < hh.length; ++i) {
//            try {
//                assertEquals(expected[i], actual[i]);
//            } catch (AssertionFailedError e) {
//                hh[0].print();
//                hh[0].persons[0].print();
//                logger.info("For household " + hh[i].ID + " person "
//                        + hh[i].persons[0].ID
//                        + ", WORKRELATED binary utility should be "
//                        + expected[i] + " but is " + actual[i] + ".");
//                throw e;
//            }
//        }
//    }
    
//    /**
//     * Tests the utility calculation for OTHER travel.  
//     *
//     */
//    public void testCalculateOtherUtility() {
//        logger.info("Test calculate other utility.");
//
//        int numTests = 1; 
//        PTHousehold[] hh = new PTHousehold[numTests];
//        double[] expected = new double[numTests];
//        double[] actual = new double[numTests];
//        
//        // household 0
//        hh[0] = LDMockDataBuilder.getHousehold(578);
//        expected[0] = 1.721199743449688;
//        actual[0] = model.calculateOtherUtility(hh[0], hh[0].persons[0]);
//        
//        for (int i = 0; i < hh.length; ++i) {
//            try {
//                assertEquals(expected[i], actual[i]);
//            } catch (AssertionFailedError e) {
//                hh[0].print();
//                hh[0].persons[0].print();
//                logger.info("For household " + hh[i].ID + " person "
//                        + hh[i].persons[0].ID
//                        + ", OTHER binary utility should be "
//                        + expected[i] + " but is " + actual[i] + ".");
//                throw e;
//            }
//        }
//    }
    
    /**
     * Main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(LDBinaryChoiceModelTest.class);
    }
}
