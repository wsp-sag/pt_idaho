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


import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Event
 *
 * Handles time aspects of events.
 *
 * @author stryker
 *
 */
public class Event extends Location {

    // tracking time with Calendar objects is preferred
    protected Calendar arrivalCalendar = null;

    protected Calendar departureCalendar = null;

    // track time in minutes past midnight
    protected int arrivalMinute;

    protected int departureMinute;

    private int tolerance = 15; // minutes leeway for comparing

    private int priority;

    /**
     * Set the equality comparison time in minutes.
     *
     * The default is 15 minutes.
     *
     * @param tolerance
     *            in minutes
     */
    public void setMinuteTolerance(int tolerance) {
        this.tolerance = tolerance;
    }

    public Calendar getArrivalCalendar() {
        return arrivalCalendar;
    }

    public void setArrival(Calendar arrivalCal) {
        this.arrivalCalendar = arrivalCal;
    }

    public Calendar getDepartureCalendar() {
        return departureCalendar;
    }

    public void setDeparture(Calendar departureCal) {
        this.departureCalendar = departureCal;
    }

    /**
     * Set the arrival date and time.
     *
     * @param year
     * @param month
     * @param date
     * @param hourOfDay
     * @param minute
     */
    public void setArrival(int year, int month, int date, int hourOfDay,
                           int minute) {
        arrivalCalendar = new GregorianCalendar();

        arrivalCalendar.set(year, month, date, hourOfDay, minute);
    }

    /**
     * Set the arrival time.
     *
     * NOTE: This method does not create a Calendar object.
     *
     * @param hour
     *            The hour in military time (e.g. 13 for 1:00 PM).
     * @param minutes
     *            The minutes past the hour.
     */
    public void setArrival(int hour, int minute) {
        arrivalMinute = hour * 60 + minute;
    }

    /**
     * Set the departure date and time.
     *
     * @param year
     * @param month
     * @param date
     * @param hourOfDay
     * @param minute
     */
    public void setDeparture(int year, int month, int date, int hourOfDay,
                             int minute) {
        departureCalendar = new GregorianCalendar();

        departureCalendar.set(year, month, date, hourOfDay, minute);
    }

    /**
     * Set the departure time.
     *
     * NOTE: This method does not create a Calendar object.
     *
     * @param hour
     *            The hour in military time (e.g. 13 for 1:00 PM).
     * @param minutes
     *            The minutes past the hour.
     */
    public void setDeparture(int hour, int minute) {
        departureMinute = hour * 60 + minute;
    }

    public int getArrivalMinute() {
        if (arrivalCalendar == null) {
            return arrivalMinute;
        }
        return (int) (arrivalCalendar.getTimeInMillis() / (60 * 1000));
    }

    public void setArrival(int arrivalMinute) {
        this.arrivalMinute = arrivalMinute;
    }

    public int getDepartureMinute() {
        if (departureCalendar == null) {
            return departureMinute;
        }
        return (int) (departureCalendar.getTimeInMillis() / (60 * 1000));
    }

    public void setDeparture(int departureMinute) {
        this.departureMinute = departureMinute;
    }

    /**
     * Calculate the duration in minutes.
     *
     * @return minutes
     */
    public int duration() {
        int depart = getDepartureMinute();
        int arrive = getArrivalMinute();

        // check for rolling over midnight
        if (arrive > depart) {
            return depart + 24 * 60 - arrive;
        }

        return depart - arrive;
    }

    /**
     * Compare Events for the same time and (if applicable) same location.
     */
    public boolean equals(com.pb.models.pt.surveydata.Event other) {
        int upper = getArrivalMinute() + tolerance;
        int lower = getArrivalMinute() - tolerance;
        int minute = other.getArrivalMinute();

        if (minute > upper || minute < lower) {
            return false;
        }

        upper = getDepartureMinute() + tolerance;
        lower = getDepartureMinute() - tolerance;
        minute = other.getDepartureMinute();

        if (minute > upper || minute < lower) {
            return false;
        }

        return true && super.equals(other);
    }

    /**
     * Compare two Calendar days by date only.
     *
     * The &lt or &gt points to the 'smaller' date.
     *
     * @param cal1
     * @param cal2
     * @return cal1 before cal2 -1, cal1 after cal2 1, same date 0
     */
    public static int compareDates(Calendar cal1, Calendar cal2) {
        int year1 = cal1.get(Calendar.YEAR);
        int year2 = cal2.get(Calendar.YEAR);

        if (year1 < year2) {
            return -1;
        }
        if (year1 > year2) {
            return 1;
        }

        int day1 = cal1.get(Calendar.DAY_OF_YEAR);
        int day2 = cal2.get(Calendar.DAY_OF_YEAR);

        if (day1 < day2) {
            return -1;
        }
        if (day1 > day2) {
            return 1;
        }

        return 0;
    }

    /**
     * Set the event priority.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @return Returns the priority.
     */
    public int getPriority() {
        return priority;
    }

}
