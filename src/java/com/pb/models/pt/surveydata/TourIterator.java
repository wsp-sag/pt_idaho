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
 * Created on Aug 18, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.surveydata;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class TourIterator implements Iterator<Tour> {
    private Household household = null;

    private HashMap households = null;

    private HouseholdMember member = null;

    private Iterator<Household> householdIter = null;

    private Iterator<HouseholdMember> memberIter = null;

    private Iterator<Tour> tourIter = null;

    public TourIterator(AbstractSurvey survey) {
        households = survey.getHouseholds();
        householdIter = survey.getHouseholdIterator();
    }

    public Tour next() {
        // need a new member?
        while ((memberIter == null || tourIter == null || !tourIter.hasNext() || !member
                .hasTours())) {
            // need a new household?
            while (memberIter == null || !memberIter.hasNext()) {
                household = householdIter.next();
                memberIter = household.getHouseholdMemberIterator();
            }
            member = memberIter.next();
            if (member.hasTours()) {
                tourIter = member.getTourIterator();
            }
        }

        return tourIter.next();
    }

    public boolean hasNext() {
        if (householdIter.hasNext() || memberIter.hasNext()
                || tourIter.hasNext()) {
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
