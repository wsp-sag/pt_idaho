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
package com.pb.models.pt;

import com.pb.models.pt.ldt.LDTourPatternType;
import org.apache.log4j.Logger;
import java.util.*;
import java.io.PrintWriter;

/**
 * PTDataWriter.java writes PT data to output device
 * 
 * @author Joel Freedman
 * @version 1.0 October 2002
 * 
 */

public class PTDataWriter {

    final static Logger logger = Logger.getLogger(PTDataWriter.class);

    /**
         * @param households Household array
         * @param oFile PrintWriter with handle to output file
         * @param weekday whether or not it is a weekday
         */
    public static void writeToursToTextFile(PTHousehold[] households,
            PrintWriter oFile, boolean weekday) {

        // open file for writing

        for (int hhNumber = 0; hhNumber < households.length; ++hhNumber) {
            PTHousehold thisHousehold = households[hhNumber];

            if (households[hhNumber] == null) {
                continue;
            }

            if (households[hhNumber].persons == null) {
                logger.warn("Household number " + hhNumber + " is empty.");
                continue;
            }

            for (PTPerson thisPerson : households[hhNumber].persons) {
                /*
                 * "hhID,memberID,personAge,weekdayTour(yes/no),initialTourString,completedTourString,tour#,departDist," +
                 * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                 * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                 * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                 * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                 * "activityPurpose,startTime,endTime,timeToActivity,distanceToActivity,tripMode,location," +
                 * "primaryMode
                 */

                if (weekday) {

                    // print weekday home-based tours first
                    for (int tourNumber = 0; tourNumber < thisPerson.getTourCount(); ++tourNumber) {
                        Tour tour = thisPerson.weekdayTours[tourNumber];

                        oFile.print(thisHousehold.ID + ",");
                        oFile.print(thisPerson.memberID + ",");
                        oFile.print(thisPerson.age + ",");
                        oFile.print("1,"); // weekdayTour=1 (TRUE)
                        tour.printCSV(oFile);
                        oFile.println();
                    }

                    // print weekday work-based tours next
                    for (int tourNumber = 0; thisPerson.weekdayWorkBasedTours != null
                            && tourNumber < thisPerson.weekdayWorkBasedTours.length; ++tourNumber) {
                        Tour thisTour = thisPerson.weekdayWorkBasedTours[tourNumber];

                        oFile.print(thisHousehold.ID + ",");
                        oFile.print(thisPerson.memberID + ",");
                        oFile.print(thisPerson.age + ",");
                        oFile.print("1,"); // weekdayTour=1 (TRUE)
                        thisTour.printCSV(oFile);
                        oFile.println();

                    }
                }
            }// end persons
        }// end households
    }

    /**
     * Summarize trips.
     *
     * Write out trips in a sensible, maintainable manner.
     * @param households Household array
     */
    public static void writeTrips(PTHousehold[] households, PrintWriter oFile) {
        for (PTHousehold household : households) {
            if (household == null) {
                continue;
            }
            for (PTPerson person : household.persons) {
                if (person.getTourCount() == 0) {
                    continue;
                }
                int trip = 0;
                for (Tour tour : person.weekdayTours) {
                    if (tour.intermediateStop1 != null) {
                        writeTrip(oFile, household, person, tour, tour.begin,
                                tour.intermediateStop1, trip++);
                        writeTrip(oFile, household, person, tour,
                                tour.intermediateStop1,
                                tour.primaryDestination, trip++);
                    } else {
                        writeTrip(oFile, household, person, tour, tour.begin,
                                tour.primaryDestination, trip++);
                    }

                    if (tour.intermediateStop2 != null) {
                        writeTrip(oFile, household, person, tour,
                                tour.primaryDestination,
                                tour.intermediateStop2, trip++);
                        writeTrip(oFile, household, person, tour,
                                tour.intermediateStop2, tour.end, trip);
                    } else {
                        writeTrip(oFile, household, person, tour,
                                tour.primaryDestination, tour.end, trip);
                    }
                }
                // Now print the weekday work based tours - if there are any.
                if (person.weekdayWorkBasedTours != null) {

                    for (Tour tour : person.weekdayWorkBasedTours) {
                        writeTrip(oFile, household, person, tour, tour.begin,
                                tour.primaryDestination, trip++);
                        writeTrip(oFile, household, person, tour,
                                tour.primaryDestination, tour.end, trip);
                    }
                }


            }
        }
    }

    private static void writeTrip(PrintWriter oFile, PTHousehold household, PTPerson person,
            Tour tour, Activity begin, Activity end, int trip) {
        oFile.print(person.hhID + ",");
        oFile.print(person.memberID + ",");
        oFile.print("1,");
        oFile.print(tour.tourNumber + ",");
        if (tour.begin.activityPurpose == ActivityPurpose.WORK) {
            oFile.print("1,");
        } else {
            oFile.print("0,");
        }
        try {
            oFile.print(tour.primaryDestination.activityPurpose + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        oFile.print(trip + ",");
        try {
            oFile.print(tour.primaryMode + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(begin.location.zoneNumber + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(end.location.zoneNumber + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(end.timeToActivity + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(end.distanceToActivity + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(begin.endTime + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(end.startTime + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(end.activityPurpose + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(end.tripMode + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(household.income + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            oFile.print(person.age + ",");
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            if (person.student) {
                oFile.print("1,");
            } else {
                oFile.print("3,");
            }
        } catch (NullPointerException e) {
            oFile.print("-9,");
        }
        try {
            if (person.employed) {
                oFile.print("1");
            } else {
                oFile.print("0");
            }
        } catch (NullPointerException e) {
            oFile.print("-9");
        }

	    oFile.println();
    }



    public static void writeWeekdayPatternsToFile(PTHousehold[] households,
            PrintWriter oFile) {

        for (PTHousehold thisHousehold : households) {
            if (thisHousehold == null) {
                continue;
            }
            if (thisHousehold.persons == null) {
                logger.warn("Empty household.");
                continue;
            }
            for (PTPerson thisPerson : thisHousehold.persons) {
                Pattern pattern = thisPerson.weekdayPattern;

                // hhID,memberID,personAge,pattern,nTours,nWorkTours,nSchoolTours,
                // nShopTours,nRecreateTours,nOtherTours
                oFile.print(thisHousehold.ID + "," + thisPerson.memberID + ","
                        + thisPerson.age + "," + thisPerson.weekdayPatternLogsum);
                try {
                    oFile.print("," + pattern.dayPattern);
                } catch (NullPointerException e) {
                    oFile.print(",null");
                }
                oFile.print("," + thisPerson.getTourCount());
                try {
                    oFile.print("," + pattern.nWorkTours + "," + pattern.nSchoolTours +
                        "," + pattern.nShopTours + "," + pattern.nRecreateTours
                        + "," + pattern.nOtherTours);
                } catch (NullPointerException e) {
                    oFile.print(",0,0,0,0,0");
                }

                oFile.println();
            }
        }
    }

    /**
     * Write household decisions.
     * 
     * @param writer PrintWriter
     *            Name of file to write to.
     * @param households
     *            Array of households to write.
     */
    public static void writeHouseholdData(PTHousehold[] households,
            PrintWriter writer) {
        
        if (writer==null) 
            throw new RuntimeException("Invalid PrintWriter given to PTDataWriter.writeHouseholdData()");
        
        for (PTHousehold household : households) {
            if (household == null || household.persons == null) {
                continue;
            }

            writer.print(household.ID + "," + household.homeTaz + ","
                    + household.persons.length);

            if (household.singleFamily) {
                writer.print(",1");
            } else {
                writer.print(",0");
            }
            writer.print("," + household.getAutoCount());
            writer.print("," + household.income);
            if (household.ldHouseholdTourIndicator) {
                writer.print(",1");
            } else {
                writer.print(",0");
            }

            if (household.ldHouseholdTourPattern == null) {
                writer.println(",none");
            } else {
                writer.println("," + household.ldHouseholdTourPattern.ordinal());
            }
        }

    }

    /**
     * Writes a single person as a line in the file.
     * 
     * @param writer
     *            Print writer pointing to the output file
     * @param households
     *            Person to write
     */
    public static void writePersonData(PTHousehold[] households, PrintWriter writer) {
        for (PTHousehold household : households) {
            if (household == null || household.persons == null) {
                continue;
            }
            for (PTPerson person : household.persons) {

                writer.print(person.hhID + "," + person.memberID + ","
                        + household.homeTaz);

                if (person.female) {
                    writer.print(",1");
                } else {
                    writer.print(",2");
                }

                writer.print("," + person.age);

                if (person.student) {
                    writer.print(",1");
                } else {
                    writer.print(",3");
                }

                if (person.employed) {
                    writer.print(",1");
                } else {
                    writer.print(",0");
                }

                writer.print("," + person.industry + "," + person.occupation
                        + "," + person.workTaz);

                for (boolean aLdTourIndicator : person.ldTourIndicator) {

                    if (aLdTourIndicator) {
                        writer.print(",1");
                    } else {
                        writer.print(",0");
                    }

                }

                for (LDTourPatternType aLdTourPattern : person.ldTourPattern) {
                    try {
                        writer.print("," + aLdTourPattern.ordinal());
                    } catch (NullPointerException e) {
                        writer.print(",-9");
                    }
                }

                Pattern pattern = person.weekdayPattern;

                // hhID,memberID,personAge,weekdayTour(yes/no),patternLogsum,
                // pattern,nHomeActivities,nWorkActivities,nSchoolActivities,
                // nShopActivities,nRecreateActivities,nOtherActivities
                try {
                    writer.print("," + pattern.dayPattern);
                } catch (NullPointerException e) {
                    writer.print(",null");
                }
                try {
                    writer.print("," + Pattern.toursToPatternString(person.weekdayTours));
                } catch (NullPointerException e) {
                    writer.print(",null");
                }
                // detail pattern goes here
//                try {
//                    writer.print("," + pattern.dayPattern);
//                } catch (NullPointerException e) {
//                    writer.print(",null");
//                }
                writer.print("," + person.getTourCount());
                try {
                    writer.print("," + pattern.nWorkTours + ","
                            + pattern.nSchoolTours + "," + pattern.nShopTours
                            + "," + pattern.nRecreateTours + ","
                            + pattern.nOtherTours);
                } catch (NullPointerException e) {
                    writer.print(",0,0,0,0,0");
                } 

                writer.println();
            }
        }
    }

    /**
     * Write trips by time-of-day.
     *
     * @param households Household array
     */
    public static void writeTODData(HashMap<Short,Integer> startTimes, PrintWriter oFile) {
    	
    	logger.info("Writing trips by time-of-day to csv file");
    	
    	//calculate trips by hour of the day
    	int[] startTrips = new int[24];
    	
    	Iterator i = startTimes.entrySet().iterator();
        while(i.hasNext()) {
          Map.Entry<Short,Integer> me = (Map.Entry) i.next();
          startTrips[(int)Math.floor(me.getKey()/100.0)-1] = startTrips[(int)Math.floor(me.getKey()/100.0)-1] + me.getValue().intValue();          
        }
        
        //write header
    	oFile.println("TIME,TRIPSTARTS");

    	//write trips by time-of-day (1600 = 4pm)
    	for(int j=0; j<startTrips.length; j++) {
    		oFile.println(((j+1) * 100) + "," + startTrips[j]);
    	}       
        oFile.flush();
    }

}
