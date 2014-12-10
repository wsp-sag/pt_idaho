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
package com.pb.models.pt;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import java.io.File;

import org.apache.log4j.Logger;
import java.io.Serializable;
import java.util.ResourceBundle;
import com.pb.common.util.ResourceUtil;

/** A class that holds expUtilities skim matrices for 
    specific market segments in PT
    uses new Matrix package
 * @author Joel Freedman
 */
public class DCExpUtilitiesManager implements Serializable{

     final static Logger logger = Logger.getLogger(DCExpUtilitiesManager.class);
     Matrix[] expUtilities; 
     ResourceBundle rb;
     int currentWorkSegment=-1;
     int currentNonWorkSegment=-1;
     
     static final long serialVersionUID = 666;
     
    public DCExpUtilitiesManager(ResourceBundle rb){
        this.rb = rb;
        
        //set the array to the total number of out-home activities
        expUtilities = new Matrix[ActivityPurpose.values().length];
        
    }
     /**
      * Use this method to update expUtilities; only will update if they are different from the
      * segments passed into the model.
      * Note: Does not store work expUtilities at this time.
      * @param workSegment Work segment
      * @param nonWorkSegment Nonwork segment
      * @return int[] exponentiated utilities
      */
     public int[] updateExpUtilities(int workSegment, int nonWorkSegment){
         int[] readNew = new int[2];
         if(logger.isDebugEnabled()) {
             logger.debug("Free memory before updating expUtilities: "+Runtime.getRuntime().freeMemory());
         }
         
         //if the workSegment is different from the current worksegment, update work expUtilities
         if(workSegment!=currentWorkSegment){
             readNew[0]=1;
            logger.info("Updating work-based expUtilities: segment "+workSegment);

            expUtilities[ActivityPurpose.WORK_BASED.ordinal()]=readMatrix(ActivityPurpose.WORK_BASED,workSegment);
            currentWorkSegment=workSegment;
         }
     
         //if the nonWorkSegment is different from the current worksegment, update non-work expUtilities
         if(nonWorkSegment!=currentNonWorkSegment){
             readNew[1] = 1;
             logger.info("Updating non-work expUtilities: segment "+nonWorkSegment);
            
             ActivityPurpose[] purposes = ActivityPurpose.values();
             for(int i=0;i<purposes.length;++i){
                 
                 if(purposes[i]==ActivityPurpose.WORK)
                     continue;
                 expUtilities[i]=readMatrix(purposes[i],nonWorkSegment);
              }
             
             currentNonWorkSegment=nonWorkSegment;
         }
         if(logger.isDebugEnabled()) {
             logger.debug("Free memory after updating expUtilities: "+Runtime.getRuntime().freeMemory());
         }
         return readNew;

     }
    /**
     * Read and return the exponentiated utilities for the given
     * purpose and market segment
     * @param purpose Activity purpose
     * @param market Market segment
     * @return the exponentiated utility Matrix for the purpose and
     * household market segment
     */
    protected Matrix readMatrix(ActivityPurpose purpose, int market){
   
        //get path to skims
        String path = ResourceUtil.getProperty(rb, "dcExpUtilitesWrite.path");
        String ext = ResourceUtil.getProperty(rb, "internal.matrix.extension");

        //construct name of file to read
        String name = path + purpose
                + Integer.toString(market) + "dceu." + ext;
        logger.info("\t\t\tReading expUtilities Matrix "+name);
            
        return MatrixReader.readMatrix(new File(name), name); 
    }

    /**
     * Get the matrix for the activity purpose 
      * @param activityPurpose Activity Purpose
     * @return the expUtilities matrix for the appropriate purpose.
     */
    public Matrix getMatrix(ActivityPurpose activityPurpose){
        
        Matrix expMatrix;
        
        if(activityPurpose==ActivityPurpose.WORK){
            logger.fatal("Error: attempting to access work dc utilities, not supported");
            throw new RuntimeException();
        }else 
            expMatrix=expUtilities[activityPurpose.ordinal()];
         
        return expMatrix;
        
    }

        public static void main(String[] args) {}   
}