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
 * Created on Aug 9, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt.tests;


import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.pb.models.pt.TimedModel;

import junit.framework.TestCase;

public class TimedModelTest extends TestCase {
    Logger logger = Logger.getLogger(TimedModelTest.class);
    
    ConcreteTimedModel model = new ConcreteTimedModel();
    
    private class ConcreteTimedModel extends TimedModel {
        
        public void method1(int n) {
            startTiming();
            if (n < 0) {
                n = -n;
            }
            try {
                for (int i = 0; i < n; ++i) {
                    TimeUnit.MILLISECONDS.sleep(100);
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted.");
            }
            
            method2();
            
            endTiming();
        }
        
        public void method2() {
            try {
                TimeUnit.MILLISECONDS.sleep(75);
            } catch (InterruptedException e) {
                logger.warn("Interrupted.");
            }
        }
    }
    public static void main(String[] args) {
        junit.textui.TestRunner.run(TimedModelTest.class);
    }

    public void testNesting() {
        int n = 5;
        long expect = n * 100 + 75;
        long tol = 25;

        model.method1(n);
        long time = model.getElapsedTime();
        
        logger.info("Expcting " + expect + " and it took " + time);
        
        assertEquals(true, expect + tol > time && expect - tol < time);
    }
}
