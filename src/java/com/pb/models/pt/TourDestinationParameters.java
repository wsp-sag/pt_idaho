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
 * A class that contains Destination Choice Parameters
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class TourDestinationParameters {

    public final static int DISTANCE=1;
    public final static int DISTANCE2=2;
    public final static int DISTANCE3=3;
    public final static int LOGDISTANCE=4;
    public final static int MAXDIST=5;
    public final static int DISTANCE2TOURS=6;
    public final static int DISTANCE3PTOURS=7;
    public final static int DISTANCE1STOP=8;
    public final static int DISTANCE2STOPS=9;
    public final static int DISTPSHOME=10;
    public final static int INTRAZONAL=11;
    public final static int LOGSUM=12;
    public final static int RETAIL=13;
    public final static int OTHERSERVICES=14;
    public final static int HOUSEHOLDS=15;
    public final static int GOVERNMENT=16;
    public final static int HEALTH=17;
    public final static int TRANSPORTATION=18;
    public final static int OTHEREMPLOYMENT=19;
    public final static int HIGHEREDUCATION=20;
    public final static int K12EDUCATION=21;
    public final static int OTHEREDUCATION=22;
    public final static int INTRAZONALACRES=23;
    public final static int INTRAZONALRURAL=24;
    public final static int INTRAZONALSUBURBAN=25;
    public final static int INTRAZONALURBAN=26;
    public final static int INTRAZONALCBD=27;
    
    // used in Ohio only
    public final static int HOTELEMPLOYMENT=28;
    public final static int OTHEROFFICEEMPLOYMENT=29; 
    
    // used in Oregon only
    public final static int COLUMBIARIVERCROSSING=30; 
    
    // distance by area type (used in Ohio) 
    public final static int DISTANCERURAL=31;
    public final static int DISTANCESUBURBAN=32;
    public final static int DISTANCEURBAN=33;
    public final static int DISTANCECBD=34;
    
}