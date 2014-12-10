package com.pb.models.pt.tests;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.StopDurationModel;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

//import junit.textui.TestRunner;

/**
 * Test the stop duration model.
 * 
 * @author Stryker
 * 
 */
public class StopDurationModelTest extends TestCase {
    protected Logger logger = Logger.getLogger(StopDurationModelTest.class);

    private StopDurationModel sdm;

    PTPerson person;

    /**
     * Generic test set-up.
     */
    public StopDurationModelTest() {
        super();
        ResourceBundle appRb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        
        sdm = new StopDurationModel(appRb, globalRb);
        
        // setUpParameters();
    }
/*
    // helper classes
    private void setUpParameters() {
        float[][]  parameters = new float[1][StopDurationParameters.getParameterCount()];

        // the data that follows is meaningless

        // ASCs
        parameters[0][StopDurationParameters.DURATION_0.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_1.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_2.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_3.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_4.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_5.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_6.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_7.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_8.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_9.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_10.ordinal()] = 0;
        parameters[0][StopDurationParameters.DURATION_11.ordinal()] = 0;

        parameters[0][StopDurationParameters.OUTBOUND.ordinal()] = 0;
        parameters[0][StopDurationParameters.MORNING.ordinal()] = 0;
        
        parameters[0][StopDurationParameters.ADULT_WORKER.ordinal()] = 0;
        
        parameters[0][StopDurationParameters.DAILY_TOURS_2.ordinal()] = 0;
        parameters[0][StopDurationParameters.DAILY_TOURS_3.ordinal()] = 0;
        parameters[0][StopDurationParameters.DAILY_TOURS_3P.ordinal()] = 0;
        parameters[0][StopDurationParameters.DAILY_TOURS_4.ordinal()] = 0;
        
        parameters[0][StopDurationParameters.DAILY_STOPS_2P.ordinal()] = 0;
        
        parameters[0][StopDurationParameters.DAILY_ACTS_6_7.ordinal()] = 0;
        parameters[0][StopDurationParameters.DAILY_ACTS_6P.ordinal()] = 0;
        parameters[0][StopDurationParameters.DAILY_ACTS_8P.ordinal()] = 0;
        
        parameters[0][StopDurationParameters.SHOP_STOP.ordinal()] = 0;
        parameters[0][StopDurationParameters.SHOP_STOP_SHOP_TOURS.ordinal()] = 0;
        
        parameters[0][StopDurationParameters.REC_STOP.ordinal()] = 0;
        parameters[0][StopDurationParameters.DEVIATION_DISTANCE.ordinal()] = 0;
        
        sdm.setParameters(parameters);
    }

    private void setUpPerson() {
        person.age = 18;
        person.employed = false;
    }
*/
    /**
     * Set-up a test case.
     */
    public void testCalculateUtility() {

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
    }

}
