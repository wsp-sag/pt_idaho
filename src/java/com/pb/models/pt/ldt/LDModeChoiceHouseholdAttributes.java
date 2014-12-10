/*
 * Copyright 2006 PB Consult Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package com.pb.models.pt.ldt;

import com.pb.models.pt.PTHousehold;
import org.apache.log4j.Logger;

/**
 * Person and household attributes used by long-distance mode choice models.
 * 
 * @author Erhardt
 * @version 1.0 Apr 5, 2006
 * 
 * @todo update with actual values.
 */
public class LDModeChoiceHouseholdAttributes {

    protected static Logger logger = Logger.getLogger(LDModeChoiceHouseholdAttributes.class);
    
    public int autos;
    public int inclow;
    public int incmed;
    public int inchi;

    /**
     * Returns an attribute object with the default attributes.
     * These are used in creating mode choice logsums.
     *
     */
    public LDModeChoiceHouseholdAttributes() {

        autos  = 1;
        inclow = 0;
        incmed = 1;
        inchi  = 0;

    }

    public void setAttributes(PTHousehold thisHousehold) {

        autos = thisHousehold.autos;
        
        inclow = 0;
        incmed = 0;
        inchi = 0;
        if (thisHousehold.income < 20000) inclow = 1; 
        else if (thisHousehold.income < 60000) incmed = 1; 
        else inchi = 1; 
    }

}
