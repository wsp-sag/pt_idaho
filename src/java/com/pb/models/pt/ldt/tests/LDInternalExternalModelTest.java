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
import com.pb.models.pt.TazManager;
import com.pb.models.pt.ldt.LDInternalExternalModel;
import com.pb.models.pt.ldt.LDInternalExternalPersonAttributes;
import com.pb.models.pt.ldt.LDTour;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * @author Erhardt
 * @version 1.0 May 15, 2006
 *
 */
public class LDInternalExternalModelTest extends TestCase {
    protected static Logger logger = Logger.getLogger(LDInternalExternalModelTest.class);
    
    private ResourceBundle ptRb;

    private LDInternalExternalModel model;

    String LDInExPersonAttribClassName;
    
    
    /**
     * Constructor for LDBinaryChoiceModelTest.
     * @param arg0
     */
    public LDInternalExternalModelTest(String arg0) {
        super(arg0);
    }

    /**
     * Gets the resource bundles and creates the model. 
     * 
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        logger.info("Testing LDInternalExternalModel.");
        super.setUp();
        
        ptRb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");

        TazManager tazManager = readTazData(ptRb, globalRb); 
        model = new LDInternalExternalModel(ptRb, globalRb, tazManager);
        model.setTrace(true);

        LDInExPersonAttribClassName = ResourceUtil.getProperty(ptRb,"ldt.inex.person.attribute.class");

    }
    
    
    private TazManager readTazData(ResourceBundle appRb, ResourceBundle globalRb) {
        // initialize the taz manager and destination choice logsums
        String tazManagerClassName = ResourceUtil.getProperty(appRb,"sdt.taz.manager.class");
        Class tazManagerClass = null;
        TazManager tazManager = null;
        try {
            tazManagerClass = Class.forName(tazManagerClassName);
            tazManager = (TazManager) tazManagerClass.newInstance();
        } catch (ClassNotFoundException e) {
            logger.fatal(tazManagerClass + " not found");
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            logger.fatal("Can't Instantiate of TazManager of type "+tazManagerClass.getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Illegal Access of TazManager of type "+tazManagerClass.getName());
            throw new RuntimeException(e);
        }

        String tazClassName = appRb.getString("sdt.taz.class");
        tazManager.setTazClassName(tazClassName);
        tazManager.readData(globalRb, appRb);
        
        // update the employment
        String empFileName = ResourceUtil.getProperty(appRb, "sdt.employment");
        tazManager.updateWorkersFromSummary(empFileName);
        
        return tazManager; 
    }
    
    /**
     * Tests the utility calculation.    
     *
     */
    public void testCalculateUtility() {
        logger.info("Test calculate utility.");

        int numTests = 1; 
        LDTour[] tour = new LDTour[numTests];
        LDInternalExternalPersonAttributes[] hha = new LDInternalExternalPersonAttributes[numTests];
        double[] expected = new double[numTests];
        double[] actual = new double[numTests];
        
        // tour 0
        tour[0] = LDMockDataBuilder.getTour(578);

        Class inExPerAttribClass = null;
        LDInternalExternalPersonAttributes personAttrib = null;
        try {
            inExPerAttribClass = Class.forName(LDInExPersonAttribClassName);
            personAttrib = (LDInternalExternalPersonAttributes) inExPerAttribClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of Person Attribute Class "+inExPerAttribClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of TazManager of type "+inExPerAttribClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        hha[0] = personAttrib;
        hha[0].codeHouseholdAttributes(tour[0].hh);
        hha[0].codePersonAttributes(tour[0].person);
        expected[0] = 1.309999942779541;
        actual[0] = model.calculateUtility(hha[0], tour[0]);
        
        for (int i = 0; i < tour.length; ++i) {
            try {
                assertEquals(expected[i], actual[i]);
            } catch (AssertionFailedError e) {
                logger.info("For tour " + tour[i].ID + 
                        ", External utility should be "
                                + expected[i] + " but is "
                                + actual[i] + ".");
                throw e;
            }
        }
    }
    
    
    /**
     * Main method.
     * 
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(LDInternalExternalModelTest.class);
    }
}
