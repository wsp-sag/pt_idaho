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
 * binary choice model.  
 *   
 * @author Erhardt
 * @version 1.0 03/13/2006
 *
 */
public abstract class LDBinaryChoicePersonAttributes {
	
	protected static Logger logger = Logger.getLogger(LDBinaryChoicePersonAttributes.class);
	
    public int hhid; 
    public int persid; 
	public int worker_1;
	public int worker_2;
	public int worker_3;
	public int auto_1;
	public int auto_2;
	public int auto_3;
	public int size_2;
	public int size_3;
	public int size_4;
	public int income_2;
	public int income_3;
	public int income_4;
	public int singleFamily;
	public int students_3;
	public int occ_ag_farm_mine;
	public int occ_manufactur;
	public int occ_trans_comm;
	public int occ_wholesale;
	public int occ_finance_re;
	public int occ_prof_sci;
	public int occ_other;
	public int schtype_college;
	public int male;
	public int age;
	public int age_sq;
	public int household_LDTrip;
	public int taz;

	/**
	 * Default constructor.
	 *
	 */
	public LDBinaryChoicePersonAttributes(){
		
	}
	
	/**
	 * Code the household attribute portion of the data.
	 * @param h
	 */
	public void codeHouseholdAttributes(PTHousehold h){
		hhid = h.ID; 
        
		worker_1=0;
		worker_2=0;
		worker_3=0;
		auto_1=0;
		auto_2=0;
		auto_3=0;
		size_2=0;
		size_3=0;
		size_4=0;
		income_2=0;
		income_3=0;
		income_4=0;
		singleFamily=0;
		students_3=0;
		household_LDTrip=0;
		taz=0;
		
		if(h.workers==1)
			worker_1=1;
		else if(h.workers==2)
			worker_2=1;
		else if(h.workers>=3)
			worker_3=1;

		if(h.persons.length==2)
			size_2=1;
		else if(h.persons.length==3)
			size_3=1;
		else if(h.persons.length>=4)
			size_4=1;
		
		if(h.autos==1)
			auto_1=1;
		else if(h.autos==2)
			auto_2=1;
		else if(h.autos>=3)
			auto_3=1;

		if(h.income>=20000 && h.income<40000)
			income_2=1;
		else if(h.income>=40000 && h.income<60000)
			income_3=1;
		else if(h.income>60000)
			income_4=1;

		if(h.singleFamily)
			singleFamily=1;
		
		if(h.ldHouseholdTourIndicator)
			household_LDTrip=1;
		
		taz=h.homeTaz;
	}
	
	/**
	 * Code the person attributes based on the attributes
	 * of all household members.  Use this for the HOUSEHOLD LD 
	 * tour binary choice model.
	 * 
	 * @param h
	 */
	public abstract void codePersonAttributes(PTHousehold h);
	
	/**
	 * Code the occupation, student, and age variables based on the attributes
	 * of a single household member.  Use this for the WORKRELATED AND OTHER
	 * LD tour binary choice models.
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
        logger.info("Long-Distance Binary Choice Household Attributes: ");
    	logger.info("worker_1=        " + worker_1);        
    	logger.info("worker_2=        " + worker_2);        
    	logger.info("worker_3=        " + worker_3);        
    	logger.info("auto_1=          " + auto_1);          
    	logger.info("auto_2=          " + auto_2);          
    	logger.info("auto_3=          " + auto_3);          
    	logger.info("size_2=          " + size_2);          
    	logger.info("size_3=          " + size_3);          
    	logger.info("size_4=          " + size_4);          
    	logger.info("income_2=        " + income_2);        
    	logger.info("income_3=        " + income_3);        
    	logger.info("income_4=        " + income_4);        
    	logger.info("singleFamily=    " + singleFamily);    
    	logger.info("students_3=      " + students_3);      
    	logger.info("occ_ag_farm_mine=" + occ_ag_farm_mine);
    	logger.info("occ_manufactur=  " + occ_manufactur);  
    	logger.info("occ_trans_comm=  " + occ_trans_comm);  
    	logger.info("occ_wholesale);  " + occ_wholesale);   
    	logger.info("occ_finance_re=  " + occ_finance_re);  
    	logger.info("occ_prof_sci=    " + occ_prof_sci);    
    	logger.info("occ_other=       " + occ_other);       
    	logger.info("schtype_college= " + schtype_college); 
    	logger.info("male=            " + male);            
    	logger.info("age=             " + age);             
    	logger.info("age_sq=          " + age_sq);          
    	logger.info("household_LDTrip=" + household_LDTrip);
    	logger.info("taz);            " + taz);     
    }
	
}
