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
 * Created on Dec 9, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.tests;

import org.apache.log4j.Logger;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import com.pb.common.model.ModelException;
import com.pb.models.pt.Scheduler;

/**
 * Test the Scheduler.
 * 
 * Show examples the Scheduler and test the results in the JUnit framework.
 * 
 * @author stryker
 */
public class SchedulerTest extends TestCase {
    protected Logger logger = Logger.getLogger(SchedulerTest.class);

    private Scheduler scheduler;

    private int periods;

    public SchedulerTest(String name) {
        super(name);

        logger.info("Creating an eight period schedule.");

        periods = 8;
        
        scheduler = new Scheduler(periods);
    }

    /**
     * Test the alternative arrays.
     */
    public void testGetAlternativeCount() {
        logger.info("Testing start / end enumeration.");
        assertEquals(36, scheduler.getAlternativeCount());
    }

    public void testGetalternative() {
        logger.info("Testing getAlternative()");
        assertEquals(0, scheduler.getAlternative(0, 0));
        assertEquals(1, scheduler.getAlternative(0, 1));
        assertEquals(2, scheduler.getAlternative(0, 2));
        assertEquals(3, scheduler.getAlternative(0, 3));
        assertEquals(4, scheduler.getAlternative(0, 4));
        assertEquals(5, scheduler.getAlternative(0, 5));
        assertEquals(6, scheduler.getAlternative(0, 6));
        assertEquals(7, scheduler.getAlternative(0, 7));
        assertEquals(8, scheduler.getAlternative(1, 1));
        assertEquals(9, scheduler.getAlternative(1, 2));
        assertEquals(10, scheduler.getAlternative(1, 3));
        assertEquals(11, scheduler.getAlternative(1, 4));
        assertEquals(12, scheduler.getAlternative(1, 5));
        assertEquals(13, scheduler.getAlternative(1, 6));
        assertEquals(14, scheduler.getAlternative(1, 7));
        assertEquals(15, scheduler.getAlternative(2, 2));
        assertEquals(16, scheduler.getAlternative(2, 3));
        assertEquals(17, scheduler.getAlternative(2, 4));
        assertEquals(18, scheduler.getAlternative(2, 5));
        assertEquals(19, scheduler.getAlternative(2, 6));
        assertEquals(20, scheduler.getAlternative(2, 7));
        assertEquals(21, scheduler.getAlternative(3, 3));
        assertEquals(22, scheduler.getAlternative(3, 4));
        assertEquals(23, scheduler.getAlternative(3, 5));
        assertEquals(24, scheduler.getAlternative(3, 6));
        assertEquals(25, scheduler.getAlternative(3, 7));
        assertEquals(26, scheduler.getAlternative(4, 4));
        assertEquals(27, scheduler.getAlternative(4, 5));
        assertEquals(28, scheduler.getAlternative(4, 6));
        assertEquals(29, scheduler.getAlternative(4, 7));
        assertEquals(30, scheduler.getAlternative(5, 5));
        assertEquals(31, scheduler.getAlternative(5, 6));
        assertEquals(32, scheduler.getAlternative(5, 7));
        assertEquals(33, scheduler.getAlternative(6, 6));
        assertEquals(34, scheduler.getAlternative(6, 7));
        assertEquals(35, scheduler.getAlternative(7, 7));
    }

    public void testGetAlternativeStart() {
        logger.info("Testing getAlternativeStart()");

        assertEquals(0, scheduler.getAlternativeStart(0));
        assertEquals(0, scheduler.getAlternativeStart(1));
        assertEquals(0, scheduler.getAlternativeStart(2));
        assertEquals(0, scheduler.getAlternativeStart(3));
        assertEquals(0, scheduler.getAlternativeStart(4));
        assertEquals(0, scheduler.getAlternativeStart(5));
        assertEquals(0, scheduler.getAlternativeStart(6));
        assertEquals(0, scheduler.getAlternativeStart(7));
        assertEquals(1, scheduler.getAlternativeStart(8));
        assertEquals(1, scheduler.getAlternativeStart(9));
        assertEquals(1, scheduler.getAlternativeStart(10));
        assertEquals(1, scheduler.getAlternativeStart(11));
        assertEquals(1, scheduler.getAlternativeStart(12));
        assertEquals(1, scheduler.getAlternativeStart(13));
        assertEquals(1, scheduler.getAlternativeStart(14));
        assertEquals(2, scheduler.getAlternativeStart(15));
        assertEquals(2, scheduler.getAlternativeStart(16));
        assertEquals(2, scheduler.getAlternativeStart(17));
        assertEquals(2, scheduler.getAlternativeStart(18));
        assertEquals(2, scheduler.getAlternativeStart(19));
        assertEquals(2, scheduler.getAlternativeStart(20));
        assertEquals(3, scheduler.getAlternativeStart(21));
        assertEquals(3, scheduler.getAlternativeStart(22));
        assertEquals(3, scheduler.getAlternativeStart(23));
        assertEquals(3, scheduler.getAlternativeStart(24));
        assertEquals(3, scheduler.getAlternativeStart(25));
        assertEquals(4, scheduler.getAlternativeStart(26));
        assertEquals(4, scheduler.getAlternativeStart(27));
        assertEquals(4, scheduler.getAlternativeStart(28));
        assertEquals(4, scheduler.getAlternativeStart(29));
        assertEquals(5, scheduler.getAlternativeStart(30));
        assertEquals(5, scheduler.getAlternativeStart(31));
        assertEquals(5, scheduler.getAlternativeStart(32));
        assertEquals(6, scheduler.getAlternativeStart(33));
        assertEquals(6, scheduler.getAlternativeStart(34));
        assertEquals(7, scheduler.getAlternativeStart(35));
    }

    public void testGetAlternativeEnd() {
        logger.info("Testing getAlternativeEnd()");
        assertEquals(0, scheduler.getAlternativeEnd(0));
        assertEquals(1, scheduler.getAlternativeEnd(1));
        assertEquals(2, scheduler.getAlternativeEnd(2));
        assertEquals(3, scheduler.getAlternativeEnd(3));
        assertEquals(4, scheduler.getAlternativeEnd(4));
        assertEquals(5, scheduler.getAlternativeEnd(5));
        assertEquals(6, scheduler.getAlternativeEnd(6));
        assertEquals(7, scheduler.getAlternativeEnd(7));
        assertEquals(1, scheduler.getAlternativeEnd(8));
        assertEquals(2, scheduler.getAlternativeEnd(9));
        assertEquals(3, scheduler.getAlternativeEnd(10));
        assertEquals(4, scheduler.getAlternativeEnd(11));
        assertEquals(5, scheduler.getAlternativeEnd(12));
        assertEquals(6, scheduler.getAlternativeEnd(13));
        assertEquals(7, scheduler.getAlternativeEnd(14));
        assertEquals(2, scheduler.getAlternativeEnd(15));
        assertEquals(3, scheduler.getAlternativeEnd(16));
        assertEquals(4, scheduler.getAlternativeEnd(17));
        assertEquals(5, scheduler.getAlternativeEnd(18));
        assertEquals(6, scheduler.getAlternativeEnd(19));
        assertEquals(7, scheduler.getAlternativeEnd(20));
        assertEquals(3, scheduler.getAlternativeEnd(21));
        assertEquals(4, scheduler.getAlternativeEnd(22));
        assertEquals(5, scheduler.getAlternativeEnd(23));
        assertEquals(6, scheduler.getAlternativeEnd(24));
        assertEquals(7, scheduler.getAlternativeEnd(25));
        assertEquals(4, scheduler.getAlternativeEnd(26));
        assertEquals(5, scheduler.getAlternativeEnd(27));
        assertEquals(6, scheduler.getAlternativeEnd(28));
        assertEquals(7, scheduler.getAlternativeEnd(29));
        assertEquals(5, scheduler.getAlternativeEnd(30));
        assertEquals(6, scheduler.getAlternativeEnd(31));
        assertEquals(7, scheduler.getAlternativeEnd(32));
        assertEquals(6, scheduler.getAlternativeEnd(33));
        assertEquals(7, scheduler.getAlternativeEnd(34));
        assertEquals(7, scheduler.getAlternativeEnd(35));
    }

    /**
     * Test schedule creation.
     */
    public void testScheduleEvent() {
        logger.info("Testing for a correct schedule array.");

        logger.info("Event 0: 3 -> 5");
        scheduler.scheduleEvent(3, 5);

        logger.info("Event 1: 1 -> 3");
        scheduler.scheduleEvent(1, 3);

        logger.info("Event 2: 6 -> 7");
        scheduler.scheduleEvent(6, 7);

        logger.info("Event 3: 6 -> 6");
        scheduler.scheduleEvent(6, 6);

        long[] expected = { 0, 2, 2, 3, 1, 1, 12, 4 };

        checkSchedule(expected);
    }

    /**
     * Test reporting a constraint window.
     */
    public void testConstraintWindow() {
        logger.info("Test reporting a constraint window.");

        logger.info("Event 0: 3 -> 7");
        scheduler.scheduleEvent(3, 7);

        logger.info("Event 1: 3 -> 3");
        scheduler.scheduleEvent(3, 3);

        logger.info("The contraint window of event 1 should be 0 -> 3");
        scheduler.setEventWindow(1);
        assertEquals(0, scheduler.getFirstWindowPeriod());
        assertEquals(3, scheduler.getLastWindowPeriod());
    }

    /**
     * Test setting a window.
     */
    public void testSetEventWindow() {
        logger.info("Testing setWindow().");

        logger.info("Event 0: 3 -> 5");
        scheduler.scheduleEvent(3, 5);

        logger.info("Event 1: 1 -> 3");
        scheduler.scheduleEvent(1, 3);

        boolean[] expected = { true, true, true, true, false, false, false,
                false };

        scheduler.setEventWindow(1);


        for (int i = 0; i < periods; ++i) {
            try {
                assertEquals(scheduler.isInWindow(i), expected[i]);
            } catch (AssertionFailedError e) {
                logger.info("Failure in testSetEventWindow!");
                logger.info("For period " + i + ": expecting: " + expected[i]
                        + ", got: " + scheduler.isInWindow(i));
                throw e;
            }
        }
    }

    /**
     * Test getting the last period of the window.
     */
    public void testGetLastWindowPeriod() {
        assertEquals(7, scheduler.getLastWindowPeriod());
    }

    /**
     * Test period availability method.
     */
    public void testIsPeriodAvailable() {
        logger.info("Testing period availability method.");
        logger.info("Event 0: 1 -> 4");
        scheduler.scheduleEvent(1, 4);

        logger.info("Event 1: 5 -> 7");
        scheduler.scheduleEvent(5, 7);

        logger.info("Event 2: 5 -> 5");
        scheduler.scheduleEvent(5, 5);

        for (int i = 0; i < scheduler.getPeriods(); ++i) {
            boolean expected = true;
            if (i > 1 && i < 4) {
                expected = false;
            }

            if (i == 6) {
                expected = false;
            }

            logger.info("Testing period " + i + " availibility.");
            assertEquals(expected, scheduler.isPeriodAvailable(i));
        }
    }

    /**
     * Test window availability method.
     */
//    public void testIsWindowAvailable() {
//        logger.info("Testing window availability method.");
//
//        logger.info("Event 0: 3 -> 3");
//        scheduler.scheduleEvent(3, 3);
//
//        logger.info("Event 1: 6 -> 7");
//        scheduler.scheduleEvent(6, 7);
//
//        for (int i = 0; i < scheduler.getEventCount(); ++i) {
//            for (int j = i; j < scheduler.getEventCount(); ++j) {
//                if (j <= 3 || (i >= 3 && j <= 6) || (i >= 7)) {
//                    assertEquals(true, scheduler.isWindowAvailable(i, j));
//                } else {
//                    assertEquals(false, scheduler.isWindowAvailable(i, j));
//                }
//            }
//        }
//    }

    /**
     * Test window availability with event priority.
     */
    public void testIsWindowAvailableForEvent() {
        logger.info("Testing window availability method with event priority.");

        logger.info("Event 0: 1 -> 3");
        scheduler.scheduleEvent(1, 3);

        logger.info("Event 1: 5 -> 7");
        scheduler.scheduleEvent(5, 7);

        logger.info("Event 2: 4 -> 5");
        scheduler.scheduleEvent(4, 5);

        // event 0 -- every window show be available
        for (int i = 0; i < scheduler.getEventCount(); ++i) {
            for (int j = i; j < scheduler.getEventCount(); ++j) {
                assertEquals(true, scheduler.isWindowAvailable(0, i, j));
            }
        }

        // event 1 -- every window after event 0 should be available
        for (int i = 0; i < scheduler.getEventCount(); ++i) {
            for (int j = i; j < scheduler.getEventCount(); ++j) {
                if (i >= 3) {
                    try {
                        assertEquals(true, scheduler.isWindowAvailable(1, i, j));
                    } catch (AssertionFailedError e) {
                        logger.info("Window " + i + " to " + j
                                + " should be available.");
                        throw e;
                    }
                } else {
                    try {
                        assertEquals(false, scheduler
                                .isWindowAvailable(1, i, j));
                    } catch (AssertionFailedError e) {
                        logger.info("Window " + i + " to " + j
                                + " NOT should be available.");
                        throw e;
                    }
                }
            }
        }

        // event 2 -- every window after event 0 and before event 1 should be
        // available
        for (int i = 0; i < scheduler.getEventCount(); ++i) {
            for (int j = i; j < scheduler.getEventCount(); ++j) {
                if (i >= 3 && j <= 5) {
                    try {
                        assertEquals(true, scheduler.isWindowAvailable(2, i, j));
                    } catch (AssertionFailedError e) {
                        logger.info("Window " + i + " to " + j
                                + " should be available.");
                        throw e;
                    }
                } else {
                    try {
                        assertEquals(false, scheduler
                                .isWindowAvailable(2, i, j));
                    } catch (AssertionFailedError e) {
                        logger.info("Window " + i + " to " + j
                                + " should NOT be available.");
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Test windows edge case.
     */
    public void testFistLastWindowEdgeCase() {
        logger.info("Scheduling a 0 -> 0 event.");
        scheduler.scheduleEvent(0, 0);
        scheduler.setEventWindow(0);

        assertEquals(0, scheduler.getFirstWindowPeriod());
        // recall that periods are [0, periods())
        assertEquals(scheduler.getPeriods() - 1, scheduler
                .getLastWindowPeriod());
    }

    /**
     * Test scheduling a conflicting event.
     */
    public void testConflictingEvent() {

        logger.info("Event 0: 3 -> 5");
        scheduler.scheduleEvent(3, 5);

        boolean expectedFailure = false;
        try {
            scheduler.scheduleEvent(3, 4);
        } catch (ModelException e) {
            expectedFailure = true;
        }

        assertEquals(expectedFailure, true);
    }

    /**
     * Test scheduling a spanning over a conflicting event.
     * 
     * This is different than above since all period are available for an event,
     * but having an event spanning another event is not allowed.
     */
    public void testConflictingSpanningEvent() {

        logger.info("Event 0: 3 -> 3");
        scheduler.scheduleEvent(3, 3);

        boolean expectedFailure = false;
        try {
            scheduler.scheduleEvent(2, 4);
        } catch (ModelException e) {
            expectedFailure = true;
        }

        assertEquals(true, expectedFailure);
    }

    /**
     * Check finding the first period of an event.
     */
    public void testGetEventStart() {
        logger.info("Testing getEventStart().");

        scheduler.scheduleEvent(0, 3);
        scheduler.scheduleEvent(7, 7);
        scheduler.scheduleEvent(3, 3);
        scheduler.scheduleEvent(6, 7);

        assertEquals(0, scheduler.getEventStart(0));
        assertEquals(7, scheduler.getEventStart(1));
        assertEquals(3, scheduler.getEventStart(2));
        assertEquals(6, scheduler.getEventStart(3));
    }

    /**
     * Check getting number of events.
     * 
     * The real value of this test is to ensure that the event count is
     * incremented correctly.
     */
    public void testGetEvents() {
        logger.info("Testing getEvents().");

        scheduler.scheduleEvent(3, 5);
        scheduler.scheduleEvent(1, 2);
        scheduler.scheduleEvent(3, 3);

        assertEquals(3, scheduler.getEventCount());
    }

    /**
     * Check finding the last period of an event.
     */
    public void testGetEventEnd() {
        logger.info("Testing getEventEnd().");

        scheduler.scheduleEvent(3, 5);
        scheduler.scheduleEvent(5, 5);
        scheduler.scheduleEvent(5, 7);

        assertEquals(5, scheduler.getEventEnd(0));
        assertEquals(5, scheduler.getEventEnd(1));
        assertEquals(7, scheduler.getEventEnd(scheduler.getEventCount() - 1));
    }

    /**
     * Check rescheduling the last event.
     */
    public void testRescheduleEvent() {
        logger.info("Testing rescheduleEvent().");

        scheduler.scheduleEvent(3, 5);
        scheduler.scheduleEvent(5, 5);
        scheduler.scheduleEvent(3, 3);

        scheduler.rescheduleEvent(1, 3);

        assertEquals(3, scheduler.getEventCount());

        long[] expected = { 0, 4, 4, 5, 1, 3, 0, 0 };

        checkSchedule(expected);
    }

    /**
     * Check the set window from a period.
     */
//    public void testSetWindow() {
//        logger.info("Testing setWindow().");
//
//        scheduler.scheduleEvent(3, 5);
//        scheduler.scheduleEvent(5, 5);
//        scheduler.scheduleEvent(3, 3);
//        
//        scheduler.setWindow(1);
//        
//        assertEquals(true, scheduler.isPeriodAvailable(1));
//
//        boolean[] expected = { true, true, true, true, false, false, false,
//                false };
//        
//        for (int i = 0; i < scheduler.getPeriods(); ++i) {
//            boolean result = scheduler.isInWindow(i);
//            boolean expect = expected[i];
//            
//            logger.info("Period " + i + " in window: " + result);
//            
//            assertEquals(expect, result);
//        }
//        
//        for (int i = 0; i < scheduler.getPeriods(); ++i) {
//            for (int j = i; j < scheduler.getPeriods(); ++j) {
//                boolean result = scheduler.isWindowAvailable(i, j);
//                boolean expect = expected[i] && expected[j];
//
//                if (result != expect) {
//                    logger.error("Unexpected result on " + i + " to " + j
//                            + ": " + result);
//                    assertEquals(expect, result);
//                }
//            }
//        }
//    }

    /**
     * Check that a schedule matches expectations.
     */
    private void checkSchedule(long[] expectation) {
        for (int event = 0; event < 3; ++event) {
            long ev = 1 << event;
            for (int i = 0; i < periods; ++i) {
                boolean expect = (ev & expectation[i]) == ev;
                try {
                    assertEquals(expect, scheduler.isEventInPeriod(event, i));
                } catch (AssertionFailedError e) {
                    logger.info("For period " + i + ": expecting: " + expect
                            + ", got: " + scheduler.isEventInPeriod(event, i));
                    throw e;
                }
            }
        }
    }

    /**
     * Run tests from the command line.
     * 
     * @param args
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(SchedulerTest.class);
    }
}
