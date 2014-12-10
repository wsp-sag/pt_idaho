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
 * Created on Jul 1, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.surveydata;

import java.lang.Integer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author Andrew Stryker &lt;stryker@pbworld.com&gt;
 * 
 */
public class Household extends AbstractSurveyData {
    protected static Logger logger = Logger.getLogger("com.pb.ohsw");

    private long household;

    private Calendar surveyStart = null;

    private Calendar surveyEnd = null;

    private Location home;

    private int survey;

    private int income;

    private int vehicles;

    private double weight;

    private HashMap<Integer, HouseholdMember> members = new HashMap<Integer, HouseholdMember>();

    /**
     * Constructor.
     */
    public Household(long household) {
        this.household = household;
    }

    /**
     * @param surveyStart
     *            The survey start date to set.
     */
    public void setSurveyStart(Calendar start) {
        this.surveyStart = start;
    }

    /**
     * @return Returns the weight.
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @param weight
     *            The weight to set.
     */
    public void setWeight(double tripExpansionFactor) {
        this.weight = tripExpansionFactor;
    }

    /**
     * @return Returns the household.
     */
    public long getHousehold() {
        return household;
    }

    /**
     * @return Returns the members.
     */
    public HashMap<Integer, HouseholdMember> getMembers() {
        return members;
    }

    /**
     * @param member
     * @return Returns a houshold member;
     */
    public HouseholdMember getMember(int member) {
        return getMember(new Integer(member));
    }

    /**
     * @param member
     * @return Returns a houshold member;
     */
    public HouseholdMember getMember(Integer member) {
        return members.get(member);
    }

    public Iterator<HouseholdMember> getHouseholdMemberIterator() {
        return members.values().iterator();
    }

    /**
     * @return the number of household members.
     */
    public int getMemberCount() {
        return members.size();
    }

    /**
     * @param Household
     *            member identifier
     * 
     * @return Returns a household member data structure.
     */
    public HouseholdMember getHouseholdMember(int member) {
        return members.get(new Integer(member));
    }

    /**
     * @param member
     *            The member to add.
     */
    public void appendMember(HouseholdMember member) {
        members.put(new Integer(member.getMember()), member);
    }

    /**
     * @return Returns the income.
     */
    public int getIncome() {
        return income;
    }

    /**
     * @param income
     *            The income to set.
     */
    public void setIncome(int income) {
        this.income = income;
    }

    /**
     * @return Returns the vehicles.
     */
    public int getVehicles() {
        return vehicles;
    }

    /**
     * @param vehicles
     *            The vehicles to set.
     */
    public void setVehicles(int vehicles) {
        this.vehicles = vehicles;
    }

    /**
     * @return Returns the home.
     */
    public Location getHome() {
        return home;
    }

    /**
     * @param home
     *            The home to set.
     */
    public void setHome(Location home) {
        this.home = home;
    }

    /**
     * @return Returns the survey end date.
     */
    public Calendar getSurveyEnd() {
        return surveyEnd;
    }

    /**
     * @param surveyEnd
     *            The survey end date to set.
     */
    public void setSurveyEnd(Calendar end) {
        this.surveyEnd = end;
    }

    /**
     * @return Returns the survey start date.
     */
    public Calendar getSurveyStart() {
        return surveyStart;
    }

    /**
     * @return Returns the workers.
     */
    public int calculateWorkers() {
        // loop through set of members and count workers
        Set s = members.keySet();
        Iterator i = s.iterator();
        int workers = 0;

        while (i.hasNext()) {
            HouseholdMember hm = (HouseholdMember) i.next();
            if (hm.isWorker())
                workers += 1;
        }

        return workers;
    }

    /**
     * Check for the existence of a HouseholdMember
     */
    public boolean containsHouseholdMember(int hm) {
        return members.containsKey(hm);
    }

    /**
     * Count students.
     * 
     * @return Number of household members who are students.
     */
    public int getStudentCount() {
        int cnt = 0;

        for (HouseholdMember member : members.values()) {
            if (member.isStudent()) {
                cnt += 1;
            }
        }

        return cnt;
    }

    /**
     * Count workers.
     * 
     * @return Number of household members who are workers.
     */
    public int getWorkerCount() {
        int cnt = 0;

        for (HouseholdMember member : members.values()) {
            if (member.isWorker()) {
                cnt += 1;
            }
        }

        return cnt;
    }

    /**
     * Summarize household into a string.
     */
    public String toString() {
        String res = "Household " + household;
        DateFormat formatDate = new SimpleDateFormat("yyyy-MMM-dd'T'HH:mm");

        if (surveyStart != null) {
            res += "\nSurvey surveyStart on "
                    + formatDate.format(surveyEnd.getTime());
        }

        if (surveyEnd != null) {
            res += "\nSurvey ends on " + formatDate.format(surveyEnd.getTime());
        }

        res += "\nwith " + getMemberCount() + " member(s):\n";

        if (members == null) {
            return res;
        }

        // loop through set of members
        Set s = members.keySet();
        Iterator i = s.iterator();

        while (i.hasNext()) {
            Integer key = (Integer) i.next();
            HouseholdMember hm = members.get(key);
            res += "\t" + hm + "\n";
        }

        return res;
    }

    public static void main(String[] args) {
    }
}
