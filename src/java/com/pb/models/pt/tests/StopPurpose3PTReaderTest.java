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
 *   Created on Mar 10, 2006 by Rosella Picado <picado@pbworld.com>
 */
package com.pb.models.pt.tests;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.StopPurpose3PTourTableReader;

import static com.pb.models.pt.PersonType.*;
import static com.pb.models.pt.ActivityPurpose.*;

import junit.framework.TestCase;

public class StopPurpose3PTReaderTest extends TestCase {

    static Logger logger = Logger.getLogger(StopPurpose3PTReaderTest.class);
    
    private StopPurpose3PTourTableReader freqTable;

    public StopPurpose3PTReaderTest() {
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        freqTable = new StopPurpose3PTourTableReader(rb);
    }
    
    public void testProbabilitySums() {

//        public float[] getLikelihood(int ptype, int tourPosition, int stop,
//                ActivityPurpose tpurp) {
        float[] probabilities = freqTable.getLikelihood(PRESCHOOL.ordinal(), 0,
                1, GRADESCHOOL);

        float sum = 0;

        for (int i = 0; i < probabilities.length; ++i) {

            sum += probabilities[i];
        }
        
        assertTrue(sum >= 0.999);
        
    }
}
