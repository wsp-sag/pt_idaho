/*
 * Copyright 2005 PB Consult Inc.
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
 * Created on Aug 23, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.surveydata;

import java.util.Iterator;


/**
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class HouseholdMemberIterator implements Iterator<HouseholdMember> {
    private Household household;

    private HouseholdMember member;

    private Iterator<Household> householdIter;

    private Iterator<HouseholdMember> memberIter = null;

    public HouseholdMemberIterator(AbstractSurvey survey) {
        householdIter = survey.getHouseholdIterator();
    }

    public HouseholdMember next() {
        if (memberIter == null || !memberIter.hasNext()) {
            household = householdIter.next();
            memberIter = household.getHouseholdMemberIterator();
        }
        return memberIter.next();
    }

    public boolean hasNext() {
        if (householdIter.hasNext() || memberIter.hasNext()) {
            return true;
        }
        return false;
    }

    /**
     * Not supported.
     */
    public void remove() {
    }

}
