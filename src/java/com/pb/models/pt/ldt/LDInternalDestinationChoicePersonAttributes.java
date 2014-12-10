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
import com.pb.models.pt.PersonType;
import com.pb.models.reference.IndustryOccupationSplitIndustryReference;

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
    public int agforfish;   
    public int metal; 
    public int lightind; 
    public int hvyind; 
    public int transp; 
    public int wholesale; 
    public int retail; 
    public int hotel; 
    public int construct; 
    public int health;  
    public int trnhndl;  
    public int util; 
    public int othsvc;  
    public int k_12;  
    public int highered;  
    public int govt;              
    
    // tour properties
    public int tourID; 
    public int completeTour;

    String[] splitIndustryLabels;
    
    /**
     * Default constructor.
     *
     */
    public LDInternalDestinationChoicePersonAttributes(IndustryOccupationSplitIndustryReference indOccRef){
        splitIndustryLabels = indOccRef.getSplitIndustryLabelsByIndex();
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
        metal     = 0; 
        lightind  = 0; 
        hvyind    = 0; 
        transp    = 0; 
        wholesale = 0; 
        retail    = 0; 
        hotel     = 0; 
        construct = 0; 
        health    = 0;  
        trnhndl   = 0;  
        util      = 0; 
        othsvc    = 0;  
        k_12      = 0;  
        highered  = 0;  
        govt      = 0;   
        

        if (p.employed == true) {

            // the OSMP labels 
            if (splitIndustryLabels[p.industry].equals("Agriculture Forestry and Fisheries Production")){
                agforfish = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Agriculture Forestry and Fisheries Office"))    {
                agforfish = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Primary Metal Products Production"))            {
                metal = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Primary Metal Products Office"))                {
                metal = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Light Industry Production"))                    {
                lightind = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Light Industry Office"))                        {
                lightind = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Heavy Industry Production"))                    {
                hvyind = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Heavy Industry Office"))                        {
                hvyind = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Transportation Equipment Production"))          {
                transp = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Transportation Equipment Office"))              {
                transp = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Wholesale Production"))                         {
                wholesale = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Wholesale Office"))                             {
                wholesale = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Retail Production"))                            {
                retail    = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Retail Office"))                                {
                retail    = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Hotel and Accommodation"))                      {
                hotel     = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Construction"))                                 {
                construct = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Health Care"))                                  {
                health    = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Transportation Handling"))                      {
                trnhndl   = 1;  
            }
            else if (splitIndustryLabels[p.industry].equals("Utilities Services"))                           {
                util      = 1; 
            }
            else if (splitIndustryLabels[p.industry].equals("Other Services"))                               {
                othsvc    = 1;    
            }
            else if (splitIndustryLabels[p.industry].equals("Gradeschool Education"))                        {
                k_12      = 1;    
            }
            else if (splitIndustryLabels[p.industry].equals("Post-Secondary Education"))                     {
                highered  = 1;   
            }
            else if (splitIndustryLabels[p.industry].equals("Government and Other"))                         {
                govt      = 1;     
            }
            else if (splitIndustryLabels[p.industry].equals("Federal Government NonDefense"))                {
                govt      = 1;     
            }
            else if (splitIndustryLabels[p.industry].equals("Federal Government Defense"))                   {
                govt      = 1;     
            }
            else if (splitIndustryLabels[p.industry].equals("Federal Government Investment"))                {
                govt      = 1;     
            }
            else if (splitIndustryLabels[p.industry].equals("State/Local Govt NonEducation"))                {
                govt      = 1;     
            }
            else if (splitIndustryLabels[p.industry].equals("State/Local Govt Education"))                   {
                govt      = 1;  
            }
            else if (splitIndustryLabels[p.industry].equals("State/Local Govt Investment"))                  {
                govt      = 1;  
            }      
            
            // the TLUMIP Labels 
            else if (splitIndustryLabels[p.industry].equals("ACCOMMODATIONS")) {
                hotel = 1;
            } else if (splitIndustryLabels[p.industry].equals("AGRICULTURE AND MINING-Agriculture")) {
                agforfish = 1;
            } else if (splitIndustryLabels[p.industry].equals("AGRICULTURE AND MINING-Office")) {
                agforfish = 1;
            } else if (splitIndustryLabels[p.industry]
                    .equals("COMMUNICATIONS AND UTILITIES-Light Industry")) {
                util = 1;
            } else if (splitIndustryLabels[p.industry]
                    .equals("COMMUNICATIONS AND UTILITIES-Office")) {
                util = 1;
            } else if (splitIndustryLabels[p.industry].equals("CONSTRUCTION")) {
                construct = 1;
            } else if (splitIndustryLabels[p.industry]
                    .equals("ELECTRONICS AND INSTRUMENTS-Light Industry")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("ELECTRONICS AND INSTRUMENTS-Office")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry]
                    .equals("FIRE BUSINESS AND PROFESSIONAL SERVICES")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("FOOD PRODUCTS-Heavy Industry")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("FOOD PRODUCTS-Light Industry")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("FOOD PRODUCTS-Office")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("FORESTRY AND LOGGING")) {
                agforfish = 1;
            } else if (splitIndustryLabels[p.industry].equals("GOVERNMENT ADMINISTRATION-Government Support")) {
                govt = 1;
            } else if (splitIndustryLabels[p.industry].equals("GOVERNMENT ADMINISTRATION-Office")) {
                govt = 1;
            } else if (splitIndustryLabels[p.industry].equals("HEALTH SERVICES-Hospital")) {
                health = 1;
            } else if (splitIndustryLabels[p.industry].equals("HEALTH SERVICES-Institutional")) {
                health = 1;
            } else if (splitIndustryLabels[p.industry].equals("HEALTH SERVICES-Office")) {
                health = 1;
            } else if (splitIndustryLabels[p.industry].equals("HIGHER EDUCATION")) {
                highered = 1;
            } else if (splitIndustryLabels[p.industry].equals("HOMEBASED SERVICES")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("LOWER EDUCATION-Grade School")) {
                k_12 = 1;
            } else if (splitIndustryLabels[p.industry].equals("LOWER EDUCATION-Office")) {
                k_12 = 1;
            } else if (splitIndustryLabels[p.industry].equals("LUMBER AND WOOD PRODUCTS-Heavy Industry")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("LUMBER AND WOOD PRODUCTS-Office")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("OTHER DURABLES-Heavy Industry")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("OTHER DURABLES-Light Industry")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("OTHER DURABLES-Office")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("OTHER NON-DURABLES-Heavy Industry")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("OTHER NON-DURABLES-Light Industry")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("OTHER NON-DURABLES-Office")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("PERSONAL AND OTHER SERVICES AND AMUSEMENTS")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("PULP AND PAPER-Heavy Industry")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("PULP AND PAPER-Office")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("RETAIL TRADE-Office")) {
                retail = 1;
            } else if (splitIndustryLabels[p.industry].equals("RETAIL TRADE-Retail")) {
                retail = 1;
            } else if (splitIndustryLabels[p.industry].equals("TRANSPORT-Depot")) {
                trnhndl = 1;
            } else if (splitIndustryLabels[p.industry].equals("TRANSPORT-Office")) {
                trnhndl = 1;
            } else if (splitIndustryLabels[p.industry].equals("WHOLESALE TRADE-Office")) {
                wholesale = 1;
            } else if (splitIndustryLabels[p.industry].equals("WHOLESALE TRADE-Warehouse")) {
                wholesale = 1;
            }

            //TLUMIP new AA split-industry labels
            else if (splitIndustryLabels[p.industry].equals("RES_agmin_ag")) {
                agforfish = 1;
            } else if (splitIndustryLabels[p.industry].equals("RES_forst_log")) {
                agforfish = 1;
            } else if (splitIndustryLabels[p.industry].equals("RES_offc_off")) {
                agforfish = 1;
            } else if (splitIndustryLabels[p.industry].equals("ENGY_elec_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("ENGY_ngas_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("ENGY_ptrl_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("ENGY_offc_off")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("CNST_main_xxx")) {
                construct = 1;
            } else if (splitIndustryLabels[p.industry].equals("CNST_nres_xxx")) {
                construct = 1;
            } else if (splitIndustryLabels[p.industry].equals("CNST_othr_xxx")) {
                construct = 1;
            } else if (splitIndustryLabels[p.industry].equals("CNST_res_xxx")) {
                construct = 1;
            } else if (splitIndustryLabels[p.industry].equals("CNST_offc_off")) {
                construct = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_food_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_food_li")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_htec_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_htec_li")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_hvtw_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_hvtw_li")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_lvtw_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_wdppr_hi")) {
                hvyind = 1;
            } else if (splitIndustryLabels[p.industry].equals("MFG_offc_off")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("WHSL_whsl_ware")) {
                wholesale = 1;
            } else if (splitIndustryLabels[p.industry].equals("WHSL_offc_off")) {
                wholesale = 1;
            } else if (splitIndustryLabels[p.industry].equals("RET_auto_ret")) {
                retail = 1;
            } else if (splitIndustryLabels[p.industry].equals("RET_stor_ret")) {
                retail = 1;
            } else if (splitIndustryLabels[p.industry].equals("RET_stor_off")) {
                retail = 1;
            } else if (splitIndustryLabels[p.industry].equals("RET_nstor_off")) {
                retail = 1;
            } else if (splitIndustryLabels[p.industry].equals("TRNS_trns_ware")) {
                trnhndl = 1;
            } else if (splitIndustryLabels[p.industry].equals("TRNS_trns_off")) {
                trnhndl = 1;
            } else if (splitIndustryLabels[p.industry].equals("INFO_info_off_li")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("INFO_info_off")) {
                lightind = 1;
            } else if (splitIndustryLabels[p.industry].equals("UTL_othr_off_li")) {
                util = 1;
            } else if (splitIndustryLabels[p.industry].equals("UTL_othr_off")) {
                util = 1;
            } else if (splitIndustryLabels[p.industry].equals("FIRE_fnin_off")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("FIRE_real_off")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("HLTH_hosp_hosp")) {
                health = 1;
            } else if (splitIndustryLabels[p.industry].equals("HLTH_care_inst")) {
                health = 1;
            } else if (splitIndustryLabels[p.industry].equals("HLTH_othr_off_li")) {
                health = 1;
            } else if (splitIndustryLabels[p.industry].equals("K12_k12_k12")) {
                k_12 = 1;
            } else if (splitIndustryLabels[p.industry].equals("K12_k12_off")) {
                k_12 = 1;
            } else if (splitIndustryLabels[p.industry].equals("HIED_hied_off_inst")) {
                highered = 1;
            } else if (splitIndustryLabels[p.industry].equals("ENT_ent_ret")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("HOSP_acc_acc")) {
                hotel = 1;
            } else if (splitIndustryLabels[p.industry].equals("HOSP_eat_ret_acc")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("SERV_tech_off")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("SERV_site_li")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("SERV_home_xxx")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("SERV_bus_off")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("SERV_nonp_off_inst")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("SERV_stor_ret")) {
                othsvc = 1;
            } else if (splitIndustryLabels[p.industry].equals("GOV_admn_gov")) {
                govt = 1;
            } else if (splitIndustryLabels[p.industry].equals("GOV_offc_off")) {
                govt = 1;
            }



            else {
                logger.error("Could not determine industry for person " + (p.hhID + "_" + p.memberID));

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
        logger.info("     agforfish      = " + agforfish     );   
        logger.info("     metal          = " + metal         ); 
        logger.info("     lightind       = " + lightind      ); 
        logger.info("     hvyind         = " + hvyind        ); 
        logger.info("     transp         = " + transp        ); 
        logger.info("     wholesale      = " + wholesale     ); 
        logger.info("     retail         = " + retail        ); 
        logger.info("     hotel          = " + hotel         ); 
        logger.info("     construct      = " + construct     ); 
        logger.info("     health         = " + health        );  
        logger.info("     trnhndl        = " + trnhndl       );  
        logger.info("     util           = " + util          ); 
        logger.info("     othsvc         = " + othsvc        );  
        logger.info("     k_12           = " + k_12          );  
        logger.info("     highered       = " + highered      );  
        logger.info("     govt           = " + govt          );   
        logger.info("   Tour " + tourID);
        logger.info("     completeTour   = " + completeTour  );
    }
    
}
