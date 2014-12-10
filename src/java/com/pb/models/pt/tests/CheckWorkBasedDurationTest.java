
/**
@author Ofir Cohen
@version 1.0, Mar 2, 2007 
*/


package com.pb.models.pt.tests;

import com.pb.common.util.ResourceUtil;

import com.pb.models.pt.WorkBasedTourModel;

import junit.framework.TestCase;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;
  
public class CheckWorkBasedDurationTest extends TestCase {
 
    
    final static String PRIM_COL="PRIM_ACCUM";
    final static String FIRST_COL="FIRST_ACCUM";

    private Logger logger = Logger.getLogger(CheckWorkBasedDurationTest.class);
     /**
     * Test method for
     */
    public void testWorkBasedDurationDistribution() {
 
        
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        WorkBasedTourModel wbm= new WorkBasedTourModel(rb);
       
        wbm.readPctWorkBasedDuration(rb);
        try{
            for(int i=0;i<20;i++) {
                logger.info("PRIM_COL "+i+"="+wbm.drawFromAccumulativeDistribution(PRIM_COL));
                logger.info("FIRST_COL "+i+"="+wbm.drawFromAccumulativeDistribution(FIRST_COL));
            }
        }        
        catch (Exception e) {
           e.printStackTrace();          
        }
   }
        
     

}
