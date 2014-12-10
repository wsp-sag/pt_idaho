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
            
            // set the employement totals, depending on whether the file contains OSMP or TLUMIP definitions
            //  now might also be new TLUMIP AA split industries instead
            
            // agriculture
            if (taz.employment.containsKey("Agriculture Forestry and Fisheries Office")
                    && taz.employment.containsKey("Agriculture Forestry and Fisheries Production")) {
                agforfish = taz.employment.get("Agriculture Forestry and Fisheries Office")
                        + taz.employment.get("Agriculture Forestry and Fisheries Production");
            } else if (taz.employment.containsKey("AGRICULTURE AND MINING-Agriculture")
                    && taz.employment.containsKey("AGRICULTURE AND MINING-Office")
                    && taz.employment.containsKey("FORESTRY AND LOGGING")) {
                agforfish = taz.employment.get("AGRICULTURE AND MINING-Agriculture")
                    + taz.employment.get("AGRICULTURE AND MINING-Office")
                    + taz.employment.get("FORESTRY AND LOGGING");
            } else if (taz.employment.containsKey("RES_agmin_ag")
                    && taz.employment.containsKey("RES_forst_log")
                    && taz.employment.containsKey("RES_offc_off")) {
                agforfish = taz.employment.get("RES_agmin_ag")
                    + taz.employment.get("RES_forst_log")
                    + taz.employment.get("RES_offc_off");
            }


            // metals
            if (taz.employment.containsKey("Primary Metal Products Production")
                    && taz.employment.containsKey("Primary Metal Products Office")) {
                metal = taz.employment.get("Primary Metal Products Production")
                        + taz.employment.get("Primary Metal Products Office");
            }
            
            // light industrial
            if (taz.employment.containsKey("Light Industry Production")
                    && taz.employment.containsKey("Light Industry Office")) {
                lightind = taz.employment.get("Light Industry Production")
                        + taz.employment.get("Light Industry Office");
            } else if(taz.employment.containsKey("COMMUNICATIONS AND UTILITIES-Light Industry")   
                    && taz.employment.containsKey("COMMUNICATIONS AND UTILITIES-Office")
                    && taz.employment.containsKey("ELECTRONICS AND INSTRUMENTS-Light Industry")
                    && taz.employment.containsKey("ELECTRONICS AND INSTRUMENTS-Office")
                    && taz.employment.containsKey("FOOD PRODUCTS-Light Industry")
                    && taz.employment.containsKey("FOOD PRODUCTS-Office")
                    && taz.employment.containsKey("LUMBER AND WOOD PRODUCTS-Office")
                    && taz.employment.containsKey("OTHER DURABLES-Heavy Industry")
                    && taz.employment.containsKey("OTHER DURABLES-Light Industry")
                    && taz.employment.containsKey("OTHER DURABLES-Office")
                    && taz.employment.containsKey("OTHER NON-DURABLES-Light Industry")
                    && taz.employment.containsKey("OTHER NON-DURABLES-Office")
                    && taz.employment.containsKey("PULP AND PAPER-Office")) {            
                lightind = taz.employment.get("COMMUNICATIONS AND UTILITIES-Light Industry")   
                    + taz.employment.get("COMMUNICATIONS AND UTILITIES-Office")
                    + taz.employment.get("ELECTRONICS AND INSTRUMENTS-Light Industry")
                    + taz.employment.get("ELECTRONICS AND INSTRUMENTS-Office")
                    + taz.employment.get("FOOD PRODUCTS-Light Industry")
                    + taz.employment.get("FOOD PRODUCTS-Office")
                    + taz.employment.get("LUMBER AND WOOD PRODUCTS-Office")
                    + taz.employment.get("OTHER DURABLES-Heavy Industry")
                    + taz.employment.get("OTHER DURABLES-Light Industry")
                    + taz.employment.get("OTHER DURABLES-Office")
                    + taz.employment.get("OTHER NON-DURABLES-Light Industry")
                    + taz.employment.get("OTHER NON-DURABLES-Office")
                    + taz.employment.get("PULP AND PAPER-Office");            
            } else if(taz.employment.containsKey("ENGY_offc_off")
                    && taz.employment.containsKey("MFG_food_li")
                    && taz.employment.containsKey("MFG_htec_li")
                    && taz.employment.containsKey("MFG_hvtw_li")
                    && taz.employment.containsKey("MFG_offc_off")
                    && taz.employment.containsKey("INFO_info_off_li")
                    && taz.employment.containsKey("INFO_info_off")
                    && taz.employment.containsKey("UTL_othr_off_li")
                    && taz.employment.containsKey("UTL_othr_off")) {
                lightind = taz.employment.get("ENGY_offc_off")
                    + taz.employment.get("MFG_food_li")
                    + taz.employment.get("MFG_htec_li")
                    + taz.employment.get("MFG_hvtw_li")
                    + taz.employment.get("MFG_offc_off")
                    + taz.employment.get("INFO_info_off_li")
                    + taz.employment.get("INFO_info_off")
                    + taz.employment.get("UTL_othr_off_li")
                    + taz.employment.get("UTL_othr_off");
            }  

            
            // heavy industry
            if (taz.employment.containsKey("Heavy Industry Production")
                    && taz.employment.containsKey("Heavy Industry Office")) {
                hvyind = taz.employment.get("Heavy Industry Production")
                        + taz.employment.get("Heavy Industry Office");
            } else if (taz.employment.containsKey("FOOD PRODUCTS-Heavy Industry")
                    && taz.employment.containsKey("LUMBER AND WOOD PRODUCTS-Heavy Industry")
                    && taz.employment.containsKey("OTHER NON-DURABLES-Heavy Industry")
                    && taz.employment.containsKey("PULP AND PAPER-Heavy Industry")) {
                hvyind = taz.employment.get("FOOD PRODUCTS-Heavy Industry")
                    + taz.employment.get("LUMBER AND WOOD PRODUCTS-Heavy Industry")
                    + taz.employment.get("OTHER NON-DURABLES-Heavy Industry")
                    + taz.employment.get("PULP AND PAPER-Heavy Industry");
            } else if (taz.employment.containsKey("ENGY_elec_hi")
                    && taz.employment.containsKey("ENGY_ngas_hi")
                    && taz.employment.containsKey("ENGY_ptrl_hi")
                    && taz.employment.containsKey("MFG_food_hi")
                    && taz.employment.containsKey("MFG_htec_hi")
                    && taz.employment.containsKey("MFG_hvtw_hi")
                    && taz.employment.containsKey("MFG_lvtw_hi")
                    && taz.employment.containsKey("MFG_wdppr_hi")) {
                hvyind = taz.employment.get("ENGY_elec_hi")
                    + taz.employment.get("ENGY_ngas_hi")
                    + taz.employment.get("ENGY_ptrl_hi")
                    + taz.employment.get("MFG_food_hi")
                    + taz.employment.get("MFG_htec_hi")
                    + taz.employment.get("MFG_hvtw_hi")
                    + taz.employment.get("MFG_lvtw_hi")
                    + taz.employment.get("MFG_wdppr_hi");
            }   
            
            // transportation equipment
            if (taz.employment.containsKey("Transportation Equipment Production")
                    && taz.employment.containsKey("Transportation Equipment Office")) {
                transp = taz.employment.get("Transportation Equipment Production")
                        + taz.employment.get("Transportation Equipment Office");
            }
            
            // wholesale 
            if (taz.employment.containsKey("Wholesale Production")
                    && taz.employment.containsKey("Wholesale Office")) {
                wholesale = taz.employment.get("Wholesale Production")
                        + taz.employment.get("Wholesale Office");
            } else if (taz.employment.containsKey("WHOLESALE TRADE-Office")
                    && taz.employment.containsKey("WHOLESALE TRADE-Warehouse")) {
                wholesale = taz.employment.get("WHOLESALE TRADE-Office")
                        + taz.employment.get("WHOLESALE TRADE-Warehouse");
            } else if (taz.employment.containsKey("WHSL_whsl_ware")
                    && taz.employment.containsKey("WHSL_offc_off")) {
                wholesale = taz.employment.get("WHSL_whsl_ware")
                        + taz.employment.get("WHSL_offc_off");
            }

            
            // retail
            if (taz.employment.containsKey("Retail Production")
                    && taz.employment.containsKey("Retail Office")) {
                retail = taz.employment.get("Retail Production")
                        + taz.employment.get("Retail Office");
            } else if (taz.employment.containsKey("RETAIL TRADE-Retail")
                    && taz.employment.containsKey("RETAIL TRADE-Office")) {
                retail = taz.employment.get("RETAIL TRADE-Retail")
                        + taz.employment.get("RETAIL TRADE-Office");
            } else if (taz.employment.containsKey("RET_auto_ret")
                    && taz.employment.containsKey("RET_stor_ret")
                    && taz.employment.containsKey("RET_stor_off")
                    && taz.employment.containsKey("RET_nstor_off")) {
                retail = taz.employment.get("RET_auto_ret")
                        + taz.employment.get("RET_stor_ret")
                        + taz.employment.get("RET_stor_off")
                        + taz.employment.get("RET_nstor_off");
            }
            
            // hotel 
            if (taz.employment.containsKey("Hotel and Accommodation")) {
                hotel = taz.employment.get("Hotel and Accommodation");
            } else if (taz.employment.containsKey("ACCOMMODATIONS")) {
                hotel = taz.employment.get("ACCOMMODATIONS");
            } else if (taz.employment.containsKey("HOSP_acc_acc")) {
                hotel = taz.employment.get("HOSP_acc_acc");
            }     
                     
            // construction
            if (taz.employment.containsKey("Construction")) {
                construct = taz.employment.get("Construction");
            } else if (taz.employment.containsKey("CONSTRUCTION")) {
                construct = taz.employment.get("CONSTRUCTION");
            } else if (taz.employment.containsKey("CNST_main_xxx")
                    && taz.employment.containsKey("CNST_nres_xxx")
                    && taz.employment.containsKey("CNST_othr_xxx")
                    && taz.employment.containsKey("CNST_res_xxx")
                    && taz.employment.containsKey("CNST_offc_off")) {
                construct = taz.employment.get("CNST_main_xxx")
                        + taz.employment.get("CNST_nres_xxx")
                        + taz.employment.get("CNST_othr_xxx")
                        + taz.employment.get("CNST_res_xxx")
                        + taz.employment.get("CNST_offc_off");
            }

                        
            // health 
            if (taz.employment.containsKey("Health Care")) {
                health = taz.employment.get("Health Care");
            } else if (taz.employment.containsKey("HEALTH SERVICES-Office") 
                    && taz.employment.containsKey("HEALTH SERVICES-Institutional") 
                    && taz.employment.containsKey("HEALTH SERVICES-Hospital")) {
                health = taz.employment.get("HEALTH SERVICES-Office") 
                    + taz.employment.get("HEALTH SERVICES-Institutional") 
                    + taz.employment.get("HEALTH SERVICES-Hospital");
            } else if (taz.employment.containsKey("HLTH_hosp_hosp")
                    && taz.employment.containsKey("HLTH_care_inst")
                    && taz.employment.containsKey("HLTH_othr_off_li")) {
                health = taz.employment.get("HLTH_hosp_hosp")
                    + taz.employment.get("HLTH_care_inst")
                    + taz.employment.get("HLTH_othr_off_li");
            }

            
            // transport
            if (taz.employment.containsKey("Transportation Handling")) {
                trnhndl = taz.employment.get("Transportation Handling");
            } else if (taz.employment.containsKey("TRANSPORT-Office") 
                    && taz.employment.containsKey("TRANSPORT-Depot")) {
                trnhndl = taz.employment.get("TRANSPORT-Office") 
                    + taz.employment.get("TRANSPORT-Depot");
            } else if (taz.employment.containsKey("TRNS_trns_ware")
                    && taz.employment.containsKey("TRNS_trns_off")) {
                trnhndl = taz.employment.get("TRNS_trns_ware")
                    + taz.employment.get("TRNS_trns_off");
            }

            
            // other serivices
            if (taz.employment.containsKey("Other Services")) {
                othsvc = taz.employment.get("Other Services");
            } else if (taz.employment.containsKey("FIRE BUSINESS AND PROFESSIONAL SERVICES")  
                    && taz.employment.containsKey("PERSONAL AND OTHER SERVICES AND AMUSEMENTS")
                    && taz.employment.containsKey("HOMEBASED SERVICES")) {
                othsvc = taz.employment.get("FIRE BUSINESS AND PROFESSIONAL SERVICES")  
                    + taz.employment.get("PERSONAL AND OTHER SERVICES AND AMUSEMENTS")
                    + taz.employment.get("HOMEBASED SERVICES");
            } else if (taz.employment.containsKey("FIRE_fnin_off")
                    && taz.employment.containsKey("FIRE_real_off")
                    && taz.employment.containsKey("ENT_ent_ret")
                    && taz.employment.containsKey("HOSP_eat_ret_acc")
                    && taz.employment.containsKey("SERV_tech_off")
                    && taz.employment.containsKey("SERV_site_li")
                    && taz.employment.containsKey("SERV_home_xxx")
                    && taz.employment.containsKey("SERV_bus_off")
                    && taz.employment.containsKey("SERV_nonp_off_inst")
                    && taz.employment.containsKey("SERV_stor_ret")) {
                othsvc = taz.employment.get("FIRE_fnin_off")
                    + taz.employment.get("FIRE_real_off")
                    + taz.employment.get("ENT_ent_ret")
                    + taz.employment.get("HOSP_eat_ret_acc")
                    + taz.employment.get("SERV_tech_off")
                    + taz.employment.get("SERV_site_li")
                    + taz.employment.get("SERV_home_xxx")
                    + taz.employment.get("SERV_bus_off")
                    + taz.employment.get("SERV_nonp_off_inst")
                    + taz.employment.get("SERV_stor_ret");
            }

            

            // grade school 
            if (taz.employment.containsKey("Gradeschool Education")) {
                k_12 = taz.employment.get("Gradeschool Education");
            } else if (taz.employment.containsKey("LOWER EDUCATION-Grade School")
                    && taz.employment.containsKey("LOWER EDUCATION-Office")) {
                k_12 = taz.employment.get("LOWER EDUCATION-Grade School") 
                    + taz.employment.get("LOWER EDUCATION-Office");
            } else if (taz.employment.containsKey("K12_k12_k12")
                    && taz.employment.containsKey("K12_k12_off")) {
                k_12 = taz.employment.get("K12_k12_k12")
                    + taz.employment.get("K12_k12_off");
            }

            
            // higher ed
            if (taz.employment.containsKey("Post-Secondary Education")) {
                highered = taz.employment.get("Post-Secondary Education");
            } else if (taz.employment.containsKey("HIGHER EDUCATION")) {
                highered = taz.employment.get("HIGHER EDUCATION");
            } else if (taz.employment.containsKey("HIED_hied_off_inst")) {
                highered = taz.employment.get("HIED_hied_off_inst");
            }
                        
            // government
            if (taz.employment.containsKey("Government and Other")) {
                govt = taz.employment.get("Government and Other");
            } else if(taz.employment.containsKey("GOVERNMENT ADMINISTRATION-Office") 
                    && taz.employment.containsKey("GOVERNMENT ADMINISTRATION-Government Support")) {
                govt = taz.employment.get("GOVERNMENT ADMINISTRATION-Office") 
                    + taz.employment.get("GOVERNMENT ADMINISTRATION-Government Support");
            } else if(taz.employment.containsKey("GOV_admn_gov")
                    && taz.employment.containsKey("GOV_offc_off")) {
                govt = taz.employment.get("GOV_admn_gov")
                    + taz.employment.get("GOV_offc_off");
            }

                        
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
            size += params[p][SIZESAMEINDUSTRYEMP]     * govt      * hha.govt;
            size += params[p][SIZEHOTELEMPIFOVERNIGHT] * hotel     * hha.completeTour;
            size += params[p][SIZEHIGHEREDEMPIFCOL]    * highered  * hha.collegeStudent;
            
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
