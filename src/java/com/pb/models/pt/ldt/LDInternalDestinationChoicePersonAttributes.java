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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.PersonType;

/**
 * Calculates household attributes for use by the long-distance tour
 * destination choice models.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 3, 2006
 *
 */
public class LDInternalDestinationChoicePersonAttributes {

    protected static Logger logger = Logger.getLogger(LDInternalDestinationChoicePersonAttributes.class);
    
    // household attributes
    public int hhID; 
    public int income020;
    public int income2060;
    public int income60p;
    
    // person attributes
    public int persID; 
    public int collegeStudent; 
    public int retail;
    public int othsvc;
    public int health;
    public int transp;
    public int k_12;
    public int highered;
    public int govt;
    public int other;            
    public int no_ind;
    public int agforfish;
    public int metal;
    public int lightind;
    public int hvyind;
    public int wholesale;
    public int hotel;
    public int construct;
    public int trnhndl;
    
    // tour properties
    public int tourID; 
    public int completeTour;

    HashMap <Integer, String> industries = new HashMap <Integer, String> ();
    
    /**
     * Default constructor.
     *
     */
//    public LDInternalDestinationChoicePersonAttributes(IndustryOccupationSplitIndustryReference indOccRef){
//        splitIndustryLabels = indOccRef.getSplitIndustryLabelsByIndex();
//    }
    
	public LDInternalDestinationChoicePersonAttributes(String personIndustryFile){
		TableDataSet table = null;
		CSVFileReader reader = new CSVFileReader();
        try {
        	table = reader.readFile(new File(personIndustryFile));
        } catch (IOException e) {
           e.printStackTrace();
        }
        logger.debug("row count " + table.getRowCount());
        logger.debug(table.getValueAt(2, 1));
        for(int r = 1; r <= table.getRowCount(); r++){
        	logger.debug("r value " + r);
        	int ind_code = (int) table.getValueAt(r, 1);
        	logger.debug("ind_code value" + ind_code);
        	String ind_desc = table.getStringValueAt(r, 2);
        	logger.debug("ind_desc value " + ind_desc);
        	industries.put(ind_code, ind_desc);
        }
    }
	
	/**
     * Code the household attribute portion of the data.
     * @param h
     */
    public void codeHouseholdAttributes(PTHousehold h){
        
        hhID = h.ID; 
        
        // set income flags
        income020=0; 
        income2060=0; 
        income60p=0;         
        if (h.income < 20000) {
            income020 = 1; 
        }
        else if (h.income < 60000) {
            income2060 = 1; 
        }
        else {
            income60p = 1; 
        }
    }
    
    
    /**
     * Code the occupation, student, and age variables based on the attributes
     * of a single household member.  
     * 
     * @param p
     */
    public void codePersonAttributes(PTPerson p){
        
        persID = p.memberID;
        
        // set college student flag
        collegeStudent = 0; 
        if (p.personType.equals(PersonType.STUDENTCOLLEGE)) {
            collegeStudent = 1; 
        }

        // set industry flags
        agforfish = 0;
        metal = 0;
        lightind = 0;
        hvyind = 0;
        wholesale = 0;
        hotel = 0;
        construct = 0;
        trnhndl = 0;
        retail = 0;
        othsvc = 0;
        health = 0;
        transp = 0;
        k_12 = 0;
        highered = 0;
        govt = 0;
        other = 0;
        no_ind = 0;

        if (p.employed == true) {
            if (industries.get((int) p.industry).equals("Agriculture, Forestry, Fishing and Hunting")) {
        		agforfish = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Mining, Quarrying, and Oil and Gas Extraction")) {
        		metal = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Manufacturing")) {
        		hvyind = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Wholesale Trade")) {
        		wholesale = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Accommodation and Food Services")) {
        		hotel = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Construction")) {
        		construct = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Retail Trade")) {
        		retail = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Other Services (except Public Administration)")) {
        		othsvc = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Health Care and Social Assistance")) {
        		health = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Transportation and Warehousing")) {
        		transp = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Educational Services")) {
        		highered = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Public Administration")) {
        		govt = 1;
        	}
        	else if (industries.get((int) p.industry).equals("Management of Companies and Enterprises")) {
        		other = 1;
        	}
        	else if (industries.get((int) p.industry).equals("No Industry")) {
        		other = 1;
        	}
            else {
                other = 1;
            }
        }
    }
    
    /**
     * Code the tour attribute portion of the data.
     * @param t
     */
    public void codeTourAttributes(LDTour t){
        
        tourID = t.ID; 
        
        // is it a complete tour in one day
        completeTour = 0; 
        if (t.patternType.equals(LDTourPatternType.COMPLETE_TOUR)) {
            completeTour = 1;
        }
    }
    
    /**
     * Print the household attributes used by the LD tour models.  
     * 
     */
    public void print(){
        logger.info("");
        logger.info("Long-Distance Internal Destination Choice Household Attributes: ");
        logger.info("   Household " + hhID);
        logger.info("     income020      = " + income020     );
        logger.info("     income2060     = " + income2060    );
        logger.info("     income60p      = " + income60p     );
        logger.info("   Person " + persID); 
        logger.info("     retail         = " + retail        ); 
        logger.info("     othsvc         = " + othsvc        );  
        logger.info("     health         = " + health        );  
        logger.info("     transp         = " + transp        ); 
        logger.info("     k_12           = " + k_12          );  
        logger.info("     highered       = " + highered      );
        logger.info("     govt           = " + govt          );  
        logger.info("     other       = " + other      );  
        logger.info("   Tour " + tourID);
        logger.info("     completeTour   = " + completeTour  );
    }
    
}
