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
package com.pb.models.pt.surveydata;

import java.util.HashMap;
import java.util.Iterator;

public abstract class AbstractSurvey extends AbstractSurveyData {

    protected HashMap<Long, Household> households = new HashMap<Long, Household>();

    /**
     * @return Return a HashMap of survey households.
     */
    public HashMap<Long, Household> getHouseholds() {
        return households;
    }

    /**
     * Get a Household object.
     *
     * @param hh
     *            Household identifier.
     */
    public Household getHousehold(long hh) {
        return households.get(hh);
    }

    /**
     * Remove a Household object.
     *
     * @param hh
     *            Household identifier.
     */
    public void removeHousehold(long hh) {
        households.remove(hh);
    }

    /**
     * Check for the existence of a Household.
     *
     * @param hh
     *            Household identifier.
     */
    public boolean containsHousehold(long hh) {
        return households.containsKey(hh);
    }

    /**
     * Iteration over all households in the survey.
     */
    public Iterator<Household> getHouseholdIterator() {
        return households.values().iterator();
    }
}
