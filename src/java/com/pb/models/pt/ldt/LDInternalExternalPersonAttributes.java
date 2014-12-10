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
package com.pb.models.pt.ldt;

import org.apache.log4j.Logger;

import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;

/**
 * Calculates household attributes for use by the long-distance tour
 * internal-external models.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 4, 2006
 *
 */
public abstract class LDInternalExternalPersonAttributes {

    protected static Logger logger = Logger.getLogger(LDInternalExternalPersonAttributes.class);
        
    // household attributes
    public int hhID; 
    public int income60p;
    
    // person attributes
    public int persID; 
    public int worker;
    public int agelt25;
    public int age5564;
    public int age65p; 
    public int occConstruct;
    public int occFinInsReal;
    public int occPubAdmin; 
    public int occEducation; 
    public int occMedical;


    
    /**
     * Default constructor.
     *
     */
    public LDInternalExternalPersonAttributes(){

    }
    
    
    /**
     * Code the household attribute portion of the data.
     * @param h
     */
    public void codeHouseholdAttributes(PTHousehold h){
        
        hhID = h.ID;         
        income60p=0;    
        if (h.income > 60000) {
            income60p = 1; 
        }
    }
    
    
    /**
     * Code the occupation, student, and age variables based on the attributes
     * of a single household member.  
     * 
     * @param p
     */
    public abstract void codePersonAttributes(PTPerson p);
    
    /**
     * Print the household attributes used by the LD tour models.  
     * 
     */
    public void print(){
        logger.info("");
        logger.info("Long-Distance Internal-External Household Attributes: ");
        logger.info("   Household " + hhID);
        logger.info("     income60p     = " + income60p     );
        logger.info("   Person " + persID);
        logger.info("     persID        = " + persID);
        logger.info("     worker        = " + worker);
        logger.info("     agelt25       = " + agelt25);
        logger.info("     occConstruct  = " + occConstruct);
        logger.info("     occFinInsReal = " + occFinInsReal);
        logger.info("     occPubAdmin   = " + occPubAdmin);
        logger.info("     occEducation  = " + occEducation);
        logger.info("     occMedical    = " + occMedical);
    }
    
}
