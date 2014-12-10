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
 * Created on Mar 8, 2006 by Greg Erhardt <erhardt@pbworld.com>
 */

package com.pb.models.pt.tests;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.AutoOwnershipModel;

/**
 * Test the auto ownership model.  
 * 
 * Give examples to the auto ownership model and test the results in the JUnit framework.
 * 
 * @author erhardt
 * @version 0.1
 * @see AutoOwnershipModel
 * @see AutoOwnershipModelParameters
 * 
 * @todo test that destination choice logsums are working.  
 * @todo test that sample households work with read parameters. 
 */
public class AutoOwnershipModelTest extends TestCase {
	protected Logger logger = Logger.getLogger(AutoOwnershipModelTest.class);
	
	private AutoOwnershipModel aoModel; 
	
	// use test parameters for now b/c not set up to read
	private double testParams[][] = {
			{0, 0    ,0,0    ,0    ,0,0    ,0    ,0    ,0,0    ,0    ,0    ,0}, 
			{1, 6.153,0,0.473,0    ,0,1.281,1.576,1.658,0,1.068,0.501,0    ,-0.423}, 
			{2, 8.465,0,2.845,2.523,0,2.002,2.995,3.824,0,1.226,1.723,0    ,-0.815},
			{3,12.584,0,2.611,2.868,0,2.225,3.519,4.549,0,1.530,2.204,2.731,-1.280}
			};
	        
	
    /**
     * Base constructor.  
     */	
    public AutoOwnershipModelTest(String name) {
        super(name);
    }
    
    /**
     * Create an auto ownership model for testing.
     */
    protected void setUp() throws Exception {
        super.setUp();

        logger.info("Creating auto ownership model with hard-coded test parameters.");
        
        // set parameters takes an array of floats, so need to typecast
        float testParamsFloat[][] = new float[testParams.length][testParams[0].length]; 
        for (int i=0; i<testParams.length; i++) {
        	for (int j=0; j<testParams[i].length; j++) {
        		testParamsFloat[i][j] = (float) testParams[i][j];
        	}
        }
        
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        
        aoModel = null /*new AutoOwnershipModel(rb)*/;
        aoModel.setParameters(testParamsFloat);
        aoModel.buildModel(); 
    }

    /**
     * Test read parameters method.
     * 
     * Include this test when file structure is set up to read parameters.  
     */
    /*
     public void testReadParameters() {
        logger.info("Testing AutoOwnershipModel.readParameters() method.");
        
        
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        aoModel = new AutoOwnershipModel(rb);
        aoModel.setTrace(true);
        
        float[][] actual = aoModel.getParameters(); 

        try {
        	assertEquals(actual.length, ALTERNATIVES); 
            assertEquals(actual[0].length, PARAMETERS);
        } catch (AssertionFailedError e) {
        	logger.info("  Parameters array should be "+ALTERNATIVES+" by "+PARAMETERS);
            throw e;
        }
                 
        // test that the value of each element is the same
        for (int i = 0; i < testParams.length; ++i) {
        	for (int j = 0; j < testParams[i].length; ++j) {
                try {
                	assertEquals(actual[i][j], testParams[i][j]);
                } catch (AssertionFailedError e) {
                	logger.info("  Parameter "+j+" for alternative "+i+" should be "+testParams[i][j]);
                    throw e;
                }
        	}            
        }
    }
    /*

    /**
     * Test calculate utilities method.
     */
    public void testCalculateUtilities() {
        logger.info("Testing AutoOwnershipModel.calculateUtilities() method.");
        
        // the expected composite utility, computed externally
        double tolerance = 0.01; 
        double expected[] = {
        		12.602, 2.304, 14.823, 3.767, 16.113, 4.485, 17.141, 5.169,
        		14.127, 3.377, 16.349, 4.953, 17.640, 5.756, 18.669, 6.509,
        		15.216, 3.840, 17.436, 5.757, 18.726, 6.797, 19.754, 7.685,
        		16.740, 5.099, 18.962, 7.073, 20.253, 8.152, 21.281, 9.064,
        		17.412, 5.524, 19.634, 7.582, 20.925, 8.713, 21.954, 9.651,
        		15.464, 3.666, 17.686, 5.647, 18.978, 6.750, 20.007, 7.678,
        		16.991, 4.974, 19.214, 7.012, 20.506, 8.152, 21.535, 9.101,
        		17.663, 5.466, 19.887, 7.570, 21.179, 8.749, 22.208, 9.717,
        		18.184, 5.491, 20.409, 7.681, 21.702, 8.952, 22.732, 9.971
        		};
        
        // test all combinations of variables
        int z=0;
        for (int s=1; s<=3; s++) {
        	for (int w=0; w<=s; w++) {
        		for (int i=1; i<=4; i++) {
        			for (int l=0; l<=1; l++) {
        		        PTHousehold hh = new PTHousehold();
        		        hh.ID = s*1000+w*100+i*10+l; 
        		        hh.homeTaz = 1;
        		        hh.size = (byte) s;
        		        hh.workers = (byte) w;
        		        hh.income = 16000*i;
        		        double dclogsum = 10*l;
                        double parkcost = 0; 
        		        double compUtility = aoModel.calculateUtility(hh, dclogsum, parkcost);
        		        
      		        
        		        double error = Math.abs(compUtility - expected[z]);        		                		        
                        try {
                        	assertTrue(error < tolerance);
                        } catch (AssertionFailedError e) {
            		        logger.info("Testing for HH #: "+hh.ID);
            		        logger.info("  HH size:        "+hh.size);
            		        logger.info("  HH workers:     "+hh.workers);
            		        logger.info("  HH income:      "+hh.income);
            		        logger.info("  DC logsum:      "+dclogsum);
            		        logger.info("  Expected composite utility: "+expected[z]);
            		        logger.info("  Actual composite utility:   "+compUtility);
                        	
                            throw e;
                        }
                        z++; 
        			}
        		}
        	}
        }        
    }    

    /**
     * Test calculate utilities method with destination choice logsums.
     * 
     * @todo implement this method
     */
    public void testCalculateUtilitiesWithDCLogsums() {
        logger.info("Testing AutoOwnershipModel.calculateUtilities() method");
        logger.info("  with full destination choice logsums."); 
        
    }    
    
    /**
     * Test choose auto ownership method.
     * 
     */
    public void testChooseAutoOwnership() {
        logger.info("Testing AutoOwnershipModel.chooseAutoOwnership() method.");
        
        int expectedMin = 0; 
        int expectedMax = 3;
        
        // test all combinations of variables
        for (int s=1; s<=3; s++) {
        	for (int w=0; w<=s; w++) {
        		for (int i=1; i<=4; i++) {
        			for (int l=0; l<=1; l++) {
        		        PTHousehold hh = new PTHousehold();
        		        hh.ID = s*1000+w*100+i*10+l; 
        		        hh.homeTaz = 1;
        		        hh.size = (byte) s;
        		        hh.workers = (byte) w;
        		        hh.income = 16000*i;
        		        double dclogsum = 10*l;
                        double parkcost = 0; 
        		        
        		        aoModel.calculateUtility(hh, dclogsum, parkcost);
        		        int autos = aoModel.chooseAutoOwnership(); 
        		        
                        try {
                        	assertTrue(autos >= expectedMin);
            		        assertTrue(autos <= expectedMax);
                        } catch (AssertionFailedError e) {
            		        logger.info("Testing for HH #: "+hh.ID);
            		        logger.info("  HH size:        "+hh.size);
            		        logger.info("  HH workers:     "+hh.workers);
            		        logger.info("  HH income:      "+hh.income);
            		        logger.info("  DC logsum:      "+dclogsum);
            		        logger.info("  Chosen auto ownerhsip: "+autos);
                        	logger.info("  Expected auto ownership 0<=autos<=3!");
            		        
                            throw e;
                        }        		        
        			}
        		}
        	}
        }
    }
    
    /**
     * Run tests from the command line.
     * 
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(AutoOwnershipModelTest.class);
    }
}
