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
 * Created on Dec 2, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt;

import java.io.Serializable;

import com.pb.common.model.ModelException;

/**
 * Schedule tours.
 * 
 * This class computes availability windows for an arbitrary length of time
 * devided into a finite number of periods as described by Mark Bradely and
 * Peter Vovsha.
 * 
 * The scheduler can take a large number of events. The events are 0-indexed and
 * are set in order that they are scheduled. The scheduler also enumerates start /
 * end combinations into a number of alternatives.
 * 
 * @author Stryker
 * @version 1.5
 */
public class Scheduler implements Serializable {
    private long[] schedule; // array of bit-fields

    static final long serialVersionUID = -1;

    private boolean[] window;

    private int[] alternativeStarts;

    private int[] alternativeEnds;

    private int events = 0;

    /**
     * Constructor.
     * @param periods time periods
     */
    public Scheduler(int periods) {
        int alts = sumSeries(periods);
        alternativeStarts = new int[alts];
        alternativeEnds = new int[alts];

        alts = 0;
        for (int i = 0; i < periods; ++i) {
            for (int j = i; j < periods; ++j) {
                alternativeStarts[alts] = i;
                alternativeEnds[alts] = j;
                alts += 1;
            }
        }

        schedule = new long[periods];
        window = new boolean[periods];

        clear();
    }

    /**
     * Schedule an event.
     * 
     * @param start
     *            Period the event starts.
     * @param end
     *            Period the event ends.
     */
    public void scheduleEvent(int start, int end) {

        if (start > end) {
            throw new ModelException("Start period is after end period.");
        }
        // check if the window is open first
        if (!isWindowAvailable(start, end)) {
            throw new ModelException("Window " + start + " to " + end
                    + " is not available.");
        }

        long e = 1 << events;

        for (int i = start; i <= end; ++i) {
            schedule[i] |= e;
        }

        events += 1;
    }

    /**
     * Reschedule the last event.
     * 
     * Remove and then reschedule the last event.
     * @param start start hour
     * @param end end hour
     */
    public void rescheduleEvent(int start, int end) {
        events -= 1;
        long e = 1 << events;
        for (int i = 0; i < getPeriods(); ++i) {
            schedule[i] |= e;
            schedule[i] ^= e;
        }

        scheduleEvent(start, end);
    }

    /**
     * Is the period in the current availibility window?
     * @param period time period
     * @return true if the period is in the most recently computed window
     */
    public boolean isInWindow(int period) {
        return window[period];
    }

    /**
     * Get number of periods.
     * 
     * @return number of periods
     */
    public int getPeriods() {
        return schedule.length;
    }

    /**
     * Is an event in the period?
     * @param event event number
     * @param period period number
     * @return boolean is event in period
     */
    public boolean isEventInPeriod(int event, int period) {
        long e = 1 << event;
        return (schedule[period] & e) == e;
    }

    /**
     * Set availibility window for an event.
     * @param event Event number
     *
     */
    public void setEventWindow(int event) {
        if (event == 0) {
            for (int i = 0; i < getPeriods(); ++i) {
                window[i] = true;
            }
            return;
        }

        int start = getEventStart(event);
        int end = getEventEnd(event);

        // look through the previous events
        int first = 0;
        int last = getPeriods();
        for (int i = 0; i < event; i++) {
            if (getEventEnd(i) > first && getEventEnd(i) <= start) {
                first = getEventEnd(i);
            }

            if (getEventStart(i) < last && getEventStart(i) >= end) {
                last = getEventStart(i);
            }

        }

        for (int i = 0; i < getPeriods(); ++i) {
            window[i] = i >= first && i <= last;
        }
    }

    /**
     * Get the first period of an event.
     * @param event Event number
     * @return int Start period of event.
     */
    public int getEventStart(int event) {
        int i = 0;

        while (!isEventInPeriod(event, i)) {
            i += 1;
        }

        return i;
    }

    /**
     * Get the last period of an event.
     * @param event Event number
     * @return int end period of event
     */
    public int getEventEnd(int event) {
        int i = getPeriods() - 1;

        while (!isEventInPeriod(event, i)) {
            i -= 1;
        }

        return i;
    }

    /**
     * Get the first period in the availibility window.
     * @return int First window period
     */
    public int getFirstWindowPeriod() {
        int i = 0;

        while (i < getPeriods()) {
            if (window[i]) {
                return i;
            }
            i += 1;
        }

        throw new ModelException("Could not find an availibility window.");
    }

    /**
     * Get the last period in the availibility window.
     * @return int Last window period
     */
    public int getLastWindowPeriod() {
        int i = getPeriods() - 1;

        while (i >= 0) {
            if (window[i]) {
                return i;
            }
            i -= 1;
        }

        throw new ModelException("Could not find an availability window.");
    }

    /**
     * Is period available for additional events?
     * @param period Time period
     * @return boolean Is the period available
     */
    public boolean isPeriodAvailable(int period) {
        if (period == 0 || period == getPeriods() - 1) {
            return true;
        }

        long previous = schedule[period - 1];
        long current = schedule[period];
        long next = schedule[period + 1];
  
        /*
         * A period is not available if the same event is scheduled in the previous
         * period, this period, and the next period.
         */
        return current == 0 || previous == 0 || next == 0
                || ((previous != current) && (current != next))
                || ((previous & current) != (current & next));


    }

    /**
     * Is window available for new event?
     * 
     * Windows are available if the first and last periods are available and
     * there are no event in the periods (if any) between.
     * @param start start hour
     * @param end end hour
     * @return boolean Window availability
     *
     */
    public boolean isWindowAvailable(int start, int end) {
        if (!isPeriodAvailable(start) || !isPeriodAvailable(end)) {
            return false;
        }

        for (int i = start + 1; i < end; ++i) {
            if (schedule[i] > 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Is window available for an event?
     * 
     * Only consider events with higher priorities when looking for windows.
     * @param event Event
     * @param start start hour
     * @param end end hour
     * @return boolean Window availability
     */
    public boolean isWindowAvailable(int event, int start, int end) {
        setEventWindow(event);

        return window[start] && window[end];
    }

    /**
     * Get the number of events.
     * 
     * Recall that event numbering starts at 0.
     * @return int number of events
     */
    public int getEventCount() {
        return events;
    }

    /**
     * Clear the scheduler.
     */
    public void clear() {
        for (int i = 0; i < getPeriods(); ++i) {
            schedule[i] = 0;
            window[i] = true;
        }
        events = 0;
    }

    /**
     * Get the number of alternatives.
     * @return int number of alternatives
     */
    public int getAlternativeCount() {
        return alternativeStarts.length;
    }

    /**
     * Get the alternative start period.
     * @param alternative time of day alternative
     * @return int start period of alternative
     */
    public int getAlternativeStart(int alternative) {
        return alternativeStarts[alternative];
    }

    /**
     * Get the alternative end period.
     * @param alternative time of day alternative
     * @return int end period of alternative
     */
    public int getAlternativeEnd(int alternative) {
        return alternativeEnds[alternative];
    }

    /**
     * Get the alternative number for a start / end pair.
     * 
     * @param start start hour
     * @param end end hour
     * @return alternative
     */
    public int getAlternative(int start, int end) {
        int alternative = 0;

        for (int i = 0; i < start; ++i) {
            alternative += getPeriods() - i;
        }

        return alternative + end - start;
    }

    /**
     * Recursively sum a series of length n where n + 1 := n + 1.
     * 
     * @param n beginning of series to sum
     * @return int sum of n, n-1, n-2, etc.
     */
    private int sumSeries(int n) {
        if (n == 0) {
            return 0;
        }
        return n + sumSeries(n - 1);
    }
}
