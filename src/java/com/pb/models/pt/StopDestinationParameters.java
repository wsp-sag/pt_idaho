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


/** 
 * A class that contains stop Destination Parameters 
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class StopDestinationParameters {

    public final static int DISTANCEAUTO=1;
    public final static int DISTANCEWALK=2;
    public final static int DISTANCEBIKE=3;
    public final static int DISTANCETRANSIT=4;
    public final static int DISTANCEPOWERAUTO=5;
    public final static int DISTANCEPOWERWALK=6;
    public final static int DISTANCEPOWERBIKE=7;
    public final static int DISTANCEPOWERTRANSIT=8;
    public final static int TIMEAUTO=9;                  //add'l time if mode = auto driver, passenger
    public final static int TIMEWALK=10;                 //add'l time if mode = walk
    public final static int TIMEBIKE=11;                 //add'l time if mode = bike
    public final static int TIMETRANSIT=12;              //add'l time if mode = WalkTransit,TransitPassenger,PassengerTransit,DriveTransit
    public final static int ORIGINAUTO=13;               // if mode = autoDriver,autoPassenger & stop = origin
    public final static int ORIGINNONMOTOR=14;           // if mode = walk,bike & stop = origin
    public final static int ORIGINTRANSIT=15;            //if mode = WalkTransit,TransitPassenger,PassengerTransit,DriveTransit& stop = origin
    public final static int DESTAUTO=16;                 // if mode = autoDriver,autoPassenger & stop = destination
    public final static int DESTNONMOTOR=17;             // if mode = walk,bike & stop = destination
    public final static int DESTTRANSIT=18;              //if mode = WalkTransit,TransitPassenger,PassengerTransit,DriveTransit & stop = destination
    public final static int INTRALOGACRES=19; 
    public final static int HHS=20;                      //households
    public final static int RETAIL=21;
    public final static int OTHERSERVICES=22;
    public final static int GOVERNMENT=23;
    public final static int HEALTH=24;
    public final static int TRANSPORTATION=25;
    public final static int OTHEREMPLOYMENT=26;
    public final static int HIGHEREDUCATION=27;
    public final static int K12EDUCATION=28;
    public final static int OTHEREDUCATION=29;
    public final static int DISTANCEFROMHOME=30;
}

                                                              
                                                                   
                                                                   
                                                                   
