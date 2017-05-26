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

import java.io.Serializable;

import com.pb.models.pt.Taz;

import static com.pb.models.pt.ldt.LDInternalDestinationChoiceParameters.*;

import org.apache.log4j.Logger;

/** 
 * Extends the Taz class to include size terms for long-distance
 * destination choice.  
 * 
 * @author Erhardt
 * @version 1.0 03/13/2006
 */
public class LDTaz implements Serializable {
    	
    private static final long serialVersionUID = 1L;
    protected static Logger logger = Logger.getLogger(LDTaz.class);
    private boolean trace = false;
    
    private float[][] params;
    Taz taz; 
    
    LDInternalDestinationChoicePersonAttributes currentHha;
    LDTourPurpose currentPurpose;
    double currentSize; 
    
	/** 
	 * Default constructor.  
	 *  
	 */	
	public LDTaz(Taz sdTaz){
        taz = sdTaz; 
        reset();
    }

    /**
     * Sets the model parameters for all purposes.
     * 
     * @param parameters
     *            an array of model parameters for this purpose.
     */     
    public void setParameters(float[][] parameters) {
        
        this.params = parameters; 
    }
	
    
    /** 
     * As an efficiency feature, the current size is stored for the TAZ, 
     * such that it will not be re-calculated for a specific household and
     * purpose.  
     * 
     * This method re-initializes those values, and should be applied between
     * each household.  
     *
     */
    public void reset() {
        currentHha = null;
        currentPurpose = null; 
        currentSize = 0; 
    }
    
	/** 
	 * Calculate the size terms for the long-distance tour
	 * destination choice model.
	 *  
     *  @param hha the household and person attributes.
     *  @param purpose the purpose of the tour under consideration.  
     *  
     *  @return the size term for this TAZ.
	 */	
	public double calculateLDSize(LDInternalDestinationChoicePersonAttributes hha,
            LDTourPurpose purpose) {
	
        double size = 0; 
        
        // check to see if the size has already been calculated
        if (hha.equals(currentHha) && purpose.equals(currentPurpose)) {
            size = currentSize;
        } 
        else {            
            
            // initialize the employment totals        
            float agforfish = 0;
            float metal     = 0;
            float lightind  = 0;
            float hvyind    = 0;
            float transp    = 0;
            float wholesale = 0;
            float retail    = 0;
            float hotel     = 0;
            float construct = 0;
            float health    = 0;
            float trnhndl   = 0;
            float othsvc    = 0;
            float k_12      = 0;
            float highered  = 0;
            float govt      = 0;
            float other     = 0;
            
            // set the employment totals
            
            // agriculture 
            if(taz.employment.containsKey("ldtAgriMining"))
            	agforfish = taz.employment.get("ldtAgriMining");
            else if(taz.employment.containsKey("AgforF"))
            	agforfish = taz.employment.get("AgforF");
            
            // transportation
            if(taz.employment.containsKey("ldtTransportation"))
            	transp = taz.employment.get("ldtTransportation");
            else if(taz.employment.containsKey("TrawhseF"))
            	transp = taz.employment.get("TrawhseF");
            
            // construction
            if(taz.employment.containsKey("ldtConst"))
            	construct = taz.employment.get("ldtConst");
            else if(taz.employment.containsKey("ConstrF"))
            	construct = taz.employment.get("ConstrF");
            
            // hotel
            if(taz.employment.containsKey("ldtHotel"))
            	hotel = taz.employment.get("ldtHotel");
            else if(taz.employment.containsKey("FoodlodgF"))
            	hotel = taz.employment.get("FoodlodgF");
            
            // retail
            if(taz.employment.containsKey("ldtRetail"))
            	retail = taz.employment.get("ldtRetail");
            else if(taz.employment.containsKey("RetailF"))
            	retail = taz.employment.get("RetailF");
            
            // health
            if(taz.employment.containsKey("ldtHealth"))
            	health = taz.employment.get("ldtHealth");
            else if(taz.employment.containsKey("HealthF"))
            	health = taz.employment.get("HealthF");
            
            // other services
            if(taz.employment.containsKey("ldtOtherService"))
            	othsvc = taz.employment.get("ldtOtherService");
            else if(taz.employment.containsKey("OtherF"))
            	othsvc = taz.employment.get("OtherF");
            
            // govt
            if(taz.employment.containsKey("ldtPublicAdmin"))
            	govt = taz.employment.get("ldtPublicAdmin");
            else if(taz.employment.containsKey("PublicF"))
            	govt = taz.employment.get("PublicF");
            
            
            // education
            if(taz.employment.containsKey("ldtEducation"))
            	highered = taz.employment.get("ldtEducation");
            else if(taz.employment.containsKey("EduHigh") && taz.employment.containsKey("EduK12")){
            	highered = taz.employment.get("EduHigh");
            	k_12 = taz.employment.get("EduK12");
            }
            
            // wholesale
            if(taz.employment.containsKey("ldtWholesale"))
            	wholesale = taz.employment.get("ldtWholesale");
            else if(taz.employment.containsKey("WhlsaleF"))
            	wholesale = taz.employment.get("WhlsaleF");
            
            // other
            if(taz.employment.containsKey("ldtOther"))
            	other = taz.employment.get("ldtOther");
            
            // hvyind
            if(taz.employment.containsKey("ldtManu"))
            	hvyind = taz.employment.get("ldtManu");
            else if(taz.employment.containsKey("ManufF"))
            	hvyind = taz.employment.get("ManufF");
            
                        
            int p = purpose.ordinal(); 
            size += params[p][SIZEHOUSEHOLDS]          * taz.getHouseholds(); 
            size += params[p][SIZETOTEMP]              * taz.getTotalEmployment();
            size += params[p][SIZEGOVTEMP]             * govt;
            size += params[p][SIZESAMEINDUSTRYEMP]     * agforfish * hha.agforfish;
            size += params[p][SIZESAMEINDUSTRYEMP]     * metal     * hha.metal;
            size += params[p][SIZESAMEINDUSTRYEMP]     * lightind  * hha.lightind;
            size += params[p][SIZESAMEINDUSTRYEMP]     * hvyind    * hha.hvyind;
            size += params[p][SIZESAMEINDUSTRYEMP]     * transp    * hha.transp;
            size += params[p][SIZESAMEINDUSTRYEMP]     * wholesale * hha.wholesale;
            size += params[p][SIZESAMEINDUSTRYEMP]     * retail    * hha.retail;
            size += params[p][SIZESAMEINDUSTRYEMP]     * hotel     * hha.hotel;
            size += params[p][SIZESAMEINDUSTRYEMP]     * construct * hha.construct;
            size += params[p][SIZESAMEINDUSTRYEMP]     * health    * hha.health;
            size += params[p][SIZESAMEINDUSTRYEMP]     * trnhndl   * hha.trnhndl;
            size += params[p][SIZESAMEINDUSTRYEMP]     * othsvc    * hha.othsvc;
            size += params[p][SIZESAMEINDUSTRYEMP]     * k_12      * hha.k_12;
            size += params[p][SIZESAMEINDUSTRYEMP]     * highered  * hha.highered;
            size += params[p][SIZESAMEINDUSTRYEMP]     * other     * hha.other;
            size += params[p][SIZESAMEINDUSTRYEMP]     * govt      * hha.govt;
            size += params[p][SIZEHOTELEMPIFOVERNIGHT] * hotel     * hha.completeTour;
            size += params[p][SIZEHIGHEREDEMPIFCOL]    * highered  * hha.collegeStudent;
            
            logger.debug("params[SIZEHOUSEHOLDS] " + params[p][SIZEHOUSEHOLDS] + ", taz.getHouseholds " + taz.getHouseholds()); 
            logger.debug("params[SIZETOTEMP] " + params[p][SIZETOTEMP] + ", taz.getTotalEmployment " + taz.getTotalEmployment());
            logger.debug("params[SIZEGOVTEMP] " + params[p][SIZEGOVTEMP] + ", govt " + govt);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", agforfish " + agforfish + ", hha.agforfish " + hha.agforfish);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", metal " + metal     + ", hha.metal " + hha.metal);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", lightind " + lightind  + ", hha.lightind " + hha.lightind);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", hvyind " + hvyind    + ", hha.hvyind " + hha.hvyind);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", transp " + transp    + ", hha.transp " + hha.transp);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", wholesale " + wholesale + ", hha.wholesale " + hha.wholesale);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", retail " + retail    + ", hha.retail " + hha.retail);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", hotel " + hotel     + ", hha.hotel " + hha.hotel);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", construct " + construct + ", hha.construct " + hha.construct);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", health " + health    + ", hha.health " + hha.health);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", trnhndl " + trnhndl   + ", hha.trnhndl " + hha.trnhndl);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", othsvc " + othsvc    + ", hha.othsvc " + hha.othsvc);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", k_12 " + k_12      + ", hha.k_12 " + hha.k_12);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", highered " + highered  + ", hha.highered " + hha.highered);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", other " + other     + ", hha.other " + hha.other);
            logger.debug("params[SIZESAMEINDUSTRYEMP] " + params[p][SIZESAMEINDUSTRYEMP] + ", govt " + govt      + ", hha.govt " + hha.govt);
            logger.debug("params[SIZEHOTELEMPIFOVERNIGHT] " + params[p][SIZEHOTELEMPIFOVERNIGHT] + ", hotel " +  hotel + ", hha.completeTour " +  hha.completeTour);
            logger.debug("params[SIZEHIGHEREDEMPIFCOL] " + params[p][SIZEHIGHEREDEMPIFCOL]  + ", highered " + highered + ", hha.collegeStudent " + hha.collegeStudent);
            
            // set the current values so we don't have to re-calculate
            currentHha = hha;
            currentPurpose = purpose; 
            currentSize = size;
            
            if (trace && logger.isDebugEnabled()) {
                this.print(); 
            }
        }
        
        return size; 
	}
    
    /**
     *print():For printing zone attributes to the screen
     *
     *
     **/
     public void print(){
         
         taz.print(); 
         
         logger.info("  Long-distance destination size calculations:");
         logger.info("    Household " + currentHha.hhID);
         logger.info("    Purpose   " + currentPurpose.toString()); 
         logger.info("    LD Size   " + currentSize);
     }
    
    
    /**
     * Set the trace option.
     * 
     * The trace option is set to false by default. The verbosity of trace
     * output is constrolled through the info and debug logger levels.
     */
    public void setTrace(boolean trace) {
        this.trace = trace;
    }
}
