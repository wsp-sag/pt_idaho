/*
 * Copyright 2006 PB Consult Inc.
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
package com.pb.models.pt;

/**
 * This class is used for defining project specific income ranges
 * that are used to classify a household as low, med-low, med or
 * high income range as well as determine their segmentation used
 * for the dc logsum calculations.
 *
 * Author: Christi Willison
 * Date: Feb 13, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class IncomeSegmenter {

    
    static enum IncomeCategory {
    INCOME_LOW, INCOME_MEDIUM_LOW, INCOME_MEDIUM_HIGH, INCOME_HIGH}

    static private int LOW_MAX_INCOME;
    static private int MED_LOW_MAX_INCOME;
    static private int MED_HIGH_MAX_INCOME;

    static boolean incomeLevelsSet = false;
    /**
     * This method can be used to override the income categories.
     * @param lowMax - income level below which a household is considered low income
     * @param highMax - income level below which a household is considered med income
     *                  and equal to or above they are considered high income.
     */
    public static void setIncomeCategoryRanges(int lowMax, int highMax) {
        LOW_MAX_INCOME    = lowMax;
        MED_HIGH_MAX_INCOME= highMax;
        MED_LOW_MAX_INCOME = (LOW_MAX_INCOME + MED_HIGH_MAX_INCOME) / 2;

        incomeLevelsSet = true;
    }

    public static String getIncomeCategoryLabel(int income) {
        if(!incomeLevelsSet) throw new RuntimeException("Must call 'setIncomeCategoryRanges' prior to using this method");
        IncomeCategory category = getIncomeCategory(income);

        switch (category) {
        case INCOME_LOW:
            return "LOW";
        case INCOME_MEDIUM_LOW:
        case INCOME_MEDIUM_HIGH:
            return "MED";
        default:
            return "HGH";
        }
    }


    public static IncomeCategory getIncomeCategory(int income) {
        if(!incomeLevelsSet) throw new RuntimeException("Must call 'setIncomeCategoryRanges' prior to using this method");

        if (income < LOW_MAX_INCOME) {
            return IncomeCategory.INCOME_LOW;
        }

        if (income < MED_LOW_MAX_INCOME) {
            return IncomeCategory.INCOME_MEDIUM_LOW;
        }

        if (income < MED_HIGH_MAX_INCOME) {
            return IncomeCategory.INCOME_MEDIUM_HIGH;
        }

        return IncomeCategory.INCOME_HIGH;
    }

    /**
         * Segmentation for the Mode/DC Logsums market segment
         * @return int logsum segment
         */
        public static int calcLogsumSegment(int income, int autos, int workers) {
            if(!incomeLevelsSet) throw new RuntimeException("Must call 'setIncomeCategoryRanges' prior to using this method");

            int segment;
            // hh income - -20, 20 - 60, 60+
            int inc;
            int suff;

            if (income < LOW_MAX_INCOME) {
                inc = 0;
            } else if (income < MED_HIGH_MAX_INCOME) {
                inc = 1;
            } else {
                inc = 2;
            }

            if (autos == 0) {
                suff = 0;
            } else if (autos < workers) {
                suff = 1;
            } else {
                suff = 2;
            }

            segment = inc * 3 + suff;
            return segment;
        }




    public static String print() {
        if(!incomeLevelsSet) throw new RuntimeException("Must call 'setIncomeCategoryRanges' prior to using this method");

        return ("LOW_MAX_INCOME="+LOW_MAX_INCOME+"\\n"
              +"MED_LOW_MAX_INCOME"+MED_LOW_MAX_INCOME+"\\n"
              +"MED_HIGH_MAX_INCOME"+MED_HIGH_MAX_INCOME);
    }

}
