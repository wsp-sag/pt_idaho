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

/**
 * Iteratoration over all activities in a survey.
 *
 * @author Andrew Stryker <stryker@pbworld.com>
 *
 */
public class ActivityIterator implements Iterator {

    private HashMap households = null;

    private Iterator hhIter = null;

    private Household household = null;

    private Iterator memIter = null;

    private HouseholdMember member = null;

    private Iterator actIter = null;

    private com.pb.models.pt.surveydata.Activity activity = null;

    private ActivityIterator() {
    }

    /**
     * Constructor.
     *
     * @param households
     */
    public ActivityIterator(HashMap households) {
        this.hhIter = households.keySet().iterator();
    }

    public boolean hasNext() {
        if (hhIter.hasNext() || memIter.hasNext() || actIter.hasNext()) {
            return true;
        }
        return false;
    }

    public Object next() {
        if (!actIter.hasNext()) {
            if (!memIter.hasNext()) {
                household = (Household) hhIter.next();
                memIter = household.getMembers().keySet().iterator();
            }
            member = (HouseholdMember) memIter.next();
            actIter = member.getActivityIterator();
        }
        return actIter.next();
    }

    public void remove() {
        throw new UnsupportedOperationException(
                "This iterator does not support the remove method");
    }
}
