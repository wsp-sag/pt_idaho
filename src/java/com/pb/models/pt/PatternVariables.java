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
 * Pattern.java stores the activity pattern for each person-day.
 * 
 * @author Joel Freedman
 * @version 3.0 2/2006
 * 
 */
public class PatternVariables {

    public static final int PATTERNNUMBER                              = 0  ;
    public static final int DAYPATTERN                                 = 1  ;
    public static final int NTOURS                                     = 2  ;
    public static final int STOPS                                      = 3  ;
    public static final int PTYPE1                                     = 4  ;
    public static final int PTYPE2                                     = 5  ;
    public static final int PTYPE3                                     = 6  ;
    public static final int PTYPE4                                     = 7  ;
    public static final int PTYPE5                                     = 8  ;
    public static final int PRESENCEWORKACTIVITIES                     = 9  ;
    public static final int PRESENCESCHOOLTOURS                        = 10 ;
    public static final int STAYATHOME                                 = 11 ;
    public static final int ONETOURPATTERN                             = 12 ;
    public static final int TWOTOURSPATTERN                            = 13 ;
    public static final int THREETOURSPATTERN                          = 14 ;
    public static final int FOURTOURSPATTERN                           = 15 ;
    public static final int FIVEPLUSTOURSPATTERN                       = 16 ;
    public static final int WORKONLYNOSTOPS                            = 17 ;
    public static final int WORKONLYINSTOPS                            = 18 ;
    public static final int WORKONLYOUTSTOPS                           = 19 ;
    public static final int WORKNOSTOPS                                = 20 ;
    public static final int WORKINSTOPS                                = 21 ;
    public static final int WORKOUTSTOPS                               = 22 ;
    public static final int SCHOOLONLY                                 = 23 ;
    public static final int WORKONLY                                   = 24 ;
    public static final int SCHOOLBEFOREWORK                           = 25 ;
    public static final int WORKBEFORESCHOOL                           = 26 ;
    public static final int WORK2PNOSTOPS                              = 27 ;
    public static final int WORK2PWITHSTOPS                            = 28 ;
    public static final int SCHOOL2P                                   = 29 ;
    public static final int NUMWBASEDTOURS                             = 30 ;
    public static final int PRESENCEWBASEDTOURS                        = 31 ;
    public static final int HOMESCHOOLHOMEPATTERN                      = 32 ;
    public static final int OTHERONLY                                  = 33 ;
    public static final int SHOPONLY2P                                 = 34 ;
    public static final int OTHERONLY2P                                = 35 ;
    public static final int NOPRIMARYTHREEPLUSTOURS                    = 36 ;
    public static final int PRESENCESHOPTOURS                          = 37 ;
    public static final int PRESENCERECTOURS                           = 38 ;
    public static final int PRESENCEOTHERTOURS                         = 39 ;
    public static final int SHOPBEFOREWORK                             = 40 ;
    public static final int RECBEFOREWORK                              = 41 ;
    public static final int OTHBEFOREWORK                              = 42 ;
    public static final int SHOPBEFORESCHOOL                           = 43 ;
    public static final int RECBEFORESCHOOL                            = 44 ;
    public static final int OTHBEFORESCHOOL                            = 45 ;
    public static final int RECBEFORESHOP                              = 46 ;
    public static final int ONESHOPACTIVITY                            = 47 ;
    public static final int TWOSHOPACTIVITIES                          = 48 ;
    public static final int THREESHOPACTIVITIES                        = 49 ;
    public static final int FOURSHOPACTIVITIES                         = 50 ;
    public static final int PRESENCESHOPACTIVITIES                     = 51 ;
    public static final int ONERECACTIVITY                             = 52 ;
    public static final int TWORECACTIVITIES                           = 53 ;
    public static final int THREERECACTIVITIES                         = 54 ;
    public static final int FOURRECACTIVITIES                          = 55 ;
    public static final int PRESENCERECACTIVITIES                      = 56 ;
    public static final int ONEOTHERACTIVITY                           = 57 ;
    public static final int TWOOTHERACTIVITIES                         = 58 ;
    public static final int THREEOTHERACTIVITIES                       = 59 ;
    public static final int FOUROTHERACTIVITIES                        = 60 ;
    public static final int PRESENCEOTHERACTIVITIES                    = 61 ;
    public static final int STOPSWORKTIMESNONWORKTOURS                 = 62 ;
    public static final int STOPSWORKTIMESSTOPSNONWORK                 = 63 ;
    public static final int STOPSTIMESTOURS                            = 64 ;
    public static final int SCHOOLWITHOUTSTOPS                         = 65 ;
    public static final int SCHOOLWITHINSTOPS                          = 66 ;
    public static final int SCHOOLWITHBOTHSTOPS                        = 67 ;
    public static final int WORKWITHOUTSTOPS                           = 68 ;
    public static final int WORKWITHINSTOPS                            = 69 ;
    public static final int WORKWITHBOTHSTOPS                          = 70 ;
    public static final int SCHOOLORWORKOUTSTOPS                       = 71 ;
    public static final int SCHOOLORWORKINSTOPS                        = 72 ;
    public static final int SCHOOLORWORKBOTHSTOPS                      = 73 ;
    public static final int SCHOOLANDWORKOUTSTOPSFIRST                 = 74 ;
    public static final int SCHOOLANDWORKINSTOPSFIRST                  = 75 ;
    public static final int SCHOOLANDWORKBOTHSTOPSFIRST                = 76 ;
    public static final int SCHOOLANDWORKOUTSTOPSSEC                   = 77 ;
    public static final int SCHOOLANDWORKINSTOPSSEC                    = 78 ;
    public static final int SCHOOLANDWORKBOTHSTOPSSEC                  = 79 ;
    public static final int SCHOOLWITHWORKANDEXTRASTOPS                = 80 ;
    public static final int MOREOUTTHANIN                              = 81 ;
    public static final int MOREINTHANOUT                              = 82 ;
    public static final int EQUALOUTANDIN                              = 83 ;
    public static final int PRESENCESTOPSONWORK                        = 84 ;
    public static final int PRESENCESTOPSONWBASED                      = 85 ;
    public static final int PRESENCESTOPSONSHOP                        = 86 ;
    public static final int PRESENCESTOPSONREC                         = 87 ;
    public static final int ONESTOPONOTHER                             = 88 ;
    public static final int TWOSTOPSONOTHER                            = 89 ;
    public static final int THREEPLUSSTOPSONOTHER                      = 90 ;
    public static final int TWOPLUSTOURS                               = 91 ;
    public static final int PRESENCESTOPS                              = 92 ;
    public static final int PRESENCESTOPSONPRIMARY                     = 93 ;
    public static final int PRESENCESTOPSONSHOPRECOTH                  = 94 ;
    public static final int PRESENCESHOPRECOTHTOURS                    = 95 ;
    public static final int STOPSFORSHOPRECOTH                         = 96 ;
    public static final int PRESENCESTOPSFORSHOPRECOTH                 = 97 ;
    public static final int PRESENCESHOPSTOPSONWORK                    = 98 ;
    public static final int SCHOOLWITHWORKSTOPS                        = 99 ;
    public static final int NUMBEROFSTOPSONWORK                        = 100;
    public static final int PRESENCESTOPSONWORKWBASED                  = 101;
    public static final int WORK_TOURS                                 = 102;
    public static final int SCHOOL_TOURS                               = 103;
    public static final int SHOP_TOURS                                 = 104;
    public static final int REC_TOURS                                  = 105;
    public static final int OTHER_TOURS                                = 106;
    public static final int SHOPSTOPSPERTOUR                           = 107; 
    public static final int RECSTOPSPERTOUR                            = 108; 
    public static final int OTHERSTOPSPERTOUR                          = 109;

    public static final int SHOPWITHOUTSTOPS                           = 110;
    public static final int SHOPWITHINSTOPS                            = 111;
    public static final int SHOPWITHBOTHSTOPS                          = 112;
    public static final int RECREATEWITHOUTSTOPS                       = 113;
    public static final int RECREATEWITHINSTOPS                        = 114;
    public static final int RECREATEWITHBOTHSTOPS                      = 115;
    public static final int OTHERWITHOUTSTOPS                          = 116;
    public static final int OTHERWITHINSTOPS                           = 117;
    public static final int OTHERWITHBOTHSTOPS                         = 118; 
    
    // new variables for ohio
    public static final int PRESENCESCHOOLSTOPSONSCHOOLTOURS           = 119; 
    public static final int PRESENCEWORKSTOPSONLYONSCHOOLTOURS         = 120; 
}
