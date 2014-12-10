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

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.Serializable;
/** 
 * Pattern.java stores the out-of-home activity pattern for each person-day,
 * as well as summary information about the pattern that can
 * be used in model estimation.
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class Pattern implements Serializable, Cloneable {
    final static Logger logger = Logger.getLogger(Pattern.class);
    
    //To hold the pattern
     public StringBuffer dayPattern = new StringBuffer();

     //Simple number of activities by type in pattern variables
     public int homeActivities;
    public int workActivities;
     public int schoolActivities;
     public int shopActivities;
     public int recreateActivities;
     public int otherActivities;
    public int nWorkTours;
     public int nSchoolTours;
     public int nShopTours;
     public int nRecreateTours;
     public int nOtherTours;
     public int nWorkBasedTours;
     public int numberOfToursGT2;

    //boolean values 1=true, 0=false
     public int t1Dummy;
     public int t2Dummy;
     public int t3Dummy;
     public int t4Dummy;
     public int t5pDummy;  
     public int wrkDummy;     
     public int schDummy;    
     public int shpDummy;     
     public int recDummy;     
     public int othDummy;     
     public int wkbDummy;
     public int toursEquals1;
     public int toursEquals2;
     public int toursEquals2Plus;
     public int toursEquals3Plus;
     public int toursEquals3;
     public int toursEquals4;
     public int toursEquals4Plus;
     public int toursEquals5Plus;
     public int workOnly;
     public int schoolOnly;
     public int shopOnly;
     public int recreateOnly;
     public int otherOnly;
     public int isWeekend;
     public int shopTours;
     public int socRecTours;
     public int otherTours;

     //Number of intermediate stop variables
     public int tour1IStops;
     public int tour2IStops;
     public int tour3IStops;
     public int tour4PIStops;
      public int workTourIStops;
     public int nonWorkTourIStops;
     public int totalIStops;
     public int IStopsEquals1;
     public int IStopsEquals2Plus;
     public int IStopsEquals2;
     public int IStopsEquals3;
     public int IStopsEquals3Plus;
     public int IStopsEquals4Plus;
     
     //Combination of activities on tour variables
     public int workPSchool;
     public int workPShop;
      public int workPRecreate;
     public int workPOther;
     public int schoolPShop;
     public int schoolPRecreate;
     public int schoolPOther;
     public int shopPRecreate;
     public int shopPOther;
     public int recreatePOther;

     //stops variables
     public int stopsOnWorkTours;
     public int stopsOnSchoolTours;
     public int stopsOnShopTours;
     public int stopsOnRecreateTours;
     public int stopsOnOtherTours;

     //Sequence variables
     public int tour1Purpose;
     public int tour2Purpose;
     public int tour3Purpose;
     public int tour4Purpose;
     public int tour1IsWork;
     public int tour1IsSchool;
     public int workTourNotFirst;
     public int schoolTourNotFirst;
     
     //pattern file variables
     public int observed=1;

     //added from Pattern Alternative//
       public boolean isAvailable=true;
       public boolean hasUtility=false;     
       double utility=0;
       double constant;
       double expConstant;
       String name;
    //the base pattern is homeAllDay
    //private static final Pattern homeAllDay=new Pattern("h");

     
/** Pattern class constructor 
@param word A day-pattern encoded as a String of any length 
with the following character values:  h=home,w=work(no work-based tour),
b=work(work-based tour),c=school,s=shop,r=social/recreation,o=other.
*/
     public Pattern(String word){
         
          dayPattern.append(word);

          countActivitiesbyPurpose();

          countIntermediateStops();

          countActivityCombinations();

          countTours();

     }

     /** counts the number of activities, tours by purpose, stores results in class variables. */
     void countActivitiesbyPurpose(){
          //search through word, count number of activities by purpose
          for(int i=0;i<dayPattern.length();++i){

               char thisChar=dayPattern.charAt(i);

               if(thisChar=='h'||thisChar=='H')
                    ++homeActivities;
               if(thisChar=='w'||thisChar=='W'){
                    ++workActivities;
                    wrkDummy=1;
               }
               if(thisChar=='c'||thisChar=='C'){
                    ++schoolActivities;
                    schDummy=1;
               }
               if(thisChar=='s'||thisChar=='S'){
                    ++shopActivities;
                    shpDummy=1;
               }
               if(thisChar=='r'||thisChar=='R'){
                    ++recreateActivities;
                    recDummy=1;
               }
               if(thisChar=='o'||thisChar=='O'){
                    ++otherActivities;
                    othDummy=1;
               }
               if(thisChar=='b'||thisChar=='B'){
                    ++workActivities;
                    ++nWorkBasedTours;
                    wrkDummy=1;
                    wkbDummy=1;
               }
          }

          if(homeActivities>=2)
               t1Dummy=1;
          if(homeActivities>=3)
               t2Dummy=1;
          if(homeActivities>=4)
               t3Dummy=1;
          if(homeActivities>=5)
               t4Dummy=1;
          if(homeActivities>=6)
               t5pDummy=1;
               
          if(homeActivities==2)
               toursEquals1=1;
          if(homeActivities==3)
               toursEquals2=1;
          if(homeActivities==4)
               toursEquals3=1;
         if(homeActivities==5)
               toursEquals4=1;
         if(homeActivities >= 3)
                toursEquals3Plus=1;
         if(homeActivities>=4)
               toursEquals3Plus=1;
          if(homeActivities>=5)
                toursEquals4Plus=1;
          if(homeActivities>=6)
               toursEquals5Plus=1;
               
          
          if(wrkDummy==1 && schDummy==0 && shpDummy==0 && recDummy==0 && othDummy==0)
               workOnly=1;
          else if(wrkDummy==0 && schDummy==1 && shpDummy==0 && recDummy==0 && othDummy==0)
               schoolOnly=1;
          else if(wrkDummy==0 && schDummy==0 && shpDummy==1 && recDummy==0 && othDummy==0)
               shopOnly=1;
          else if(wrkDummy==0 && schDummy==0 && shpDummy==0 && recDummy==1 && othDummy==0)
               recreateOnly=1;
          else if(wrkDummy==0 && schDummy==0 && shpDummy==0 && recDummy==0 && othDummy==1)
               otherOnly=1;
               

     } //end countActivitiesbyPurpose()

     /** 
      * This method takes a tour number and returns the activities in the pattern
      * that relate to the tour number.  For example, if the pattern is HWHORH, and the 
      * tour number is 2, the return is HORH.
      * 
      * @param tourNumber Tour number
      * @return A string representing the tour for the pattern.
      */
     public String getTourString(int tourNumber){
          
          if(homeActivities<2){
               logger.fatal("Error: less than 2 home activities on pattern "+dayPattern);
               logger.fatal("Cannot return tour number "+tourNumber+ ". Returning null");
               return null;
          }

          String dayString = dayPattern.toString().toLowerCase();
          StringBuffer tourString = new StringBuffer();
          //the following indices are used to locate the at-home activities on either end of a tour
          int lastHomeActivityIndex=0;
          int firstHomeActivityIndex;
          int n=0;
          //get desired tour
          while(dayString.length()>lastHomeActivityIndex+1){
               ++n;
               firstHomeActivityIndex=lastHomeActivityIndex;
               lastHomeActivityIndex=dayString.indexOf("h",firstHomeActivityIndex+1);
               if(n==tourNumber){
                    tourString.append(dayString.substring(firstHomeActivityIndex,lastHomeActivityIndex+1));
                    break;          
               }
          }
//          System.out.println("Got string "+tourString);
          return tourString.toString();
     }


     /** counts the number of intermediate stops in this word, stores results in class variables. */
     void countIntermediateStops(){
          if(homeActivities>=2)
               tour1IStops=getTourString(1).length()-3;
          if(homeActivities>=3)
               tour2IStops=getTourString(2).length()-3;
          if(homeActivities>=4)
               tour3IStops=getTourString(3).length()-3;
               
          if(homeActivities>=5)
               for(int i=5;i<=homeActivities;++i)
                    tour4PIStops += getTourString(i-1).length()-3;
               
          totalIStops=tour1IStops+tour2IStops+tour3IStops+tour4PIStops;
          if(totalIStops==1)
               IStopsEquals1=1;
          if(totalIStops>=2)
               IStopsEquals2Plus=1;
          if(totalIStops==2)
               IStopsEquals2=1;
          if(totalIStops==3)
               IStopsEquals3=1;
          if(totalIStops>=3)
               IStopsEquals3Plus=1;
          if(totalIStops>=4)
               IStopsEquals4Plus=1;



     }

     /** counts the number of activity combinations in this word, stores results in class variables */
     void countActivityCombinations(){ 
          int tourNumber=1;
          if(homeActivities>1){
               while(homeActivities>=(tourNumber+1)){
//                    System.out.println("pattern "+dayPattern.toString()+" has "+homeActivities+" homeActivities");
                    String thisTour=getTourString(tourNumber);
                    boolean workActivity=false;
                    boolean schoolActivity=false;
                    boolean shopActivity=false;
                    boolean recreateActivity=false;
                    boolean otherActivity=false;
                    //cycle through letters on this tour between two home locations
                    for(int i=1;i<(thisTour.length()-1);++i){
                         if(thisTour.charAt(i)=='w'||thisTour.charAt(i)=='b'||thisTour.charAt(i)=='W'||thisTour.charAt(i)=='B')
                              workActivity=true;
                         if(thisTour.charAt(i)=='c'||thisTour.charAt(i)=='C')
                              schoolActivity=true;
                         if(thisTour.charAt(i)=='s'||thisTour.charAt(i)=='S')
                              shopActivity=true;
                         if(thisTour.charAt(i)=='r'||thisTour.charAt(i)=='R')
                              recreateActivity=true;
                         if(thisTour.charAt(i)=='o'||thisTour.charAt(i)=='O')
                              otherActivity=true;
                    } //end cycling through letters of this tour
                    //number of stops
                    if(workActivity && thisTour.length()>3)
                         stopsOnWorkTours += thisTour.length()-3;
                    else if(schoolActivity && thisTour.length()>3)
                         stopsOnSchoolTours += thisTour.length()-3;
                    else if(shopActivity && thisTour.length()>3)
                         stopsOnShopTours += thisTour.length()-3;
                    else if(recreateActivity && thisTour.length()>3)
                         stopsOnRecreateTours += thisTour.length()-3;
                    else if(otherActivity && thisTour.length()>3)
                         stopsOnOtherTours += thisTour.length()-3;



                    //combinations
                    if(workActivity && schoolActivity)
                         ++workPSchool;
                    if(workActivity && shopActivity)
                         ++workPShop;
                    if(workActivity && recreateActivity)
                         ++workPRecreate;
                    if(workActivity && otherActivity)
                         ++workPOther;
                    if(schoolActivity && shopActivity)
                         ++schoolPShop;
                    if(schoolActivity && recreateActivity)
                         ++schoolPRecreate;
                    if(schoolActivity && otherActivity)
                         ++schoolPOther;
                    if(shopActivity && recreateActivity)
                         ++shopPRecreate;
                    if(shopActivity && otherActivity)          
                         ++shopPOther;
                    if(recreateActivity && otherActivity)
                         ++recreatePOther;


                    //sequence
                    if(tourNumber==1){  //these purposes do NOT correspond to the values listed in "ActivityPurpose" but
                         if(workActivity)   //they are not used outside this class so it is irrelevant.
                              tour1Purpose=1;
                         else if(schoolActivity)
                              tour1Purpose=2;
                         else if(shopActivity)
                              tour1Purpose=3;
                         else if(recreateActivity)
                              tour1Purpose=4;
                         else if(otherActivity)
                              tour1Purpose=5;
                    }else if(tourNumber==2){
                         if(workActivity)
                              tour2Purpose=1;
                         else if(schoolActivity)
                              tour2Purpose=2;
                         else if(shopActivity)
                              tour2Purpose=3;
                         else if(recreateActivity)
                              tour2Purpose=4;
                         else if(otherActivity)
                              tour2Purpose=5;
                    }else if(tourNumber==3){
                         if(workActivity)
                              tour3Purpose=1;
                         else if(schoolActivity)
                              tour3Purpose=2;
                         else if(shopActivity)
                              tour3Purpose=3;
                         else if(recreateActivity)
                              tour3Purpose=4;
                         else if(otherActivity)
                              tour3Purpose=5;
                    }else if(tourNumber==4){
                         if(workActivity)
                              tour4Purpose=1;
                         else if(schoolActivity)
                              tour4Purpose=2;
                         else if(shopActivity)
                              tour4Purpose=3;
                         else if(recreateActivity)
                              tour4Purpose=4;
                         else if(otherActivity)
                              tour4Purpose=5;
                    }
                    ++tourNumber;
               } //end this tour     
          }
          if(tour1Purpose==1)
               tour1IsWork=1;
          else if(tour1Purpose==2)
               tour1IsSchool=1;
     }
     
     /**
      * Count the number of home-based tours.
      * 
      * @return number of home-based tours.
      */
     public int getHomeBasedTourCount() {
         return nWorkTours + nSchoolTours + nShopTours + nRecreateTours
                + nOtherTours;
     }

    /**
     * Creates a pattern that is just the current tour, then creates a Tour
     * object using that pattern. Counts number of tours for each tour purpose
     * using the primaryDestination activity of the Tour
     */
     public void countTours(){
          
          if(homeActivities>1){
               for(int i=0;i<(homeActivities-1);++i){
                    String tourString;
                    tourString =getTourString(i+1);
                    
                    Tour thisTour = new Tour(tourString, null);
                    
                    //increment tour count variables
                    if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK || 
                            thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)
                         ++nWorkTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.GRADESCHOOL)
                         ++nSchoolTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.COLLEGE)
                        ++nSchoolTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.SHOP)
                         ++nShopTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.RECREATE)
                         ++nRecreateTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.OTHER)
                         ++nOtherTours;
                    else if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED)
                         ++nWorkBasedTours;
                         
                    if((thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK || 
                   thisTour.primaryDestination.activityPurpose==ActivityPurpose.WORK_BASED) && i>1)
                         workTourNotFirst=1;
                    
                    if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.GRADESCHOOL && i>1)
                         schoolTourNotFirst=1;
                    if(thisTour.primaryDestination.activityPurpose==ActivityPurpose.COLLEGE && i>1)
                        schoolTourNotFirst=1;
                         
                    if(i>2)
                         ++numberOfToursGT2;

                   if(nShopTours > 0) shopTours=1;
                   if(nRecreateTours > 0) socRecTours = 1;
                   if(nOtherTours > 0) otherTours = 1;
               }
          }
               
          
     }
     public void print(){
               //To hold the pattern
          logger.info("***************************");
          logger.info("dayPattern = "+ dayPattern);

          //Simple number of activities by type in pattern variables
          logger.info("homeActivities =       "+ homeActivities);
          logger.info("workActivities =       "+ workActivities);
          logger.info("schoolActivities =     "+ schoolActivities);
          logger.info("shopActivities =       "+ shopActivities);
          logger.info("recreateActivities =   "+ recreateActivities);
          logger.info("otherActivities =      "+ otherActivities);
          logger.info("t1Dummy =              "+ t1Dummy);
          logger.info("t2Dummy =              "+ t2Dummy);
          logger.info("t3Dummy =              "+ t3Dummy);
          logger.info("t4Dummy =              "+ t4Dummy);
          logger.info("t5pDummy =             "+ t5pDummy);
          logger.info("wrkDummy =             "+ wrkDummy);
          logger.info("schDummy =             "+ schDummy);
          logger.info("shpDummy =             "+ shpDummy);
          logger.info("recDummy =             "+ recDummy);
          logger.info("othDummy =             "+ othDummy);
          logger.info("wkbDummy =             "+ wkbDummy);
          logger.info("toursEquals1 =         "+ toursEquals1);
          logger.info("toursEquals2 =         "+ toursEquals2);
          logger.info("toursEquals3Plus =     "+ toursEquals3Plus);
          logger.info("toursEquals3 =         "+ toursEquals3);
          logger.info("toursEquals4 =         "+ toursEquals4);
          logger.info("toursEquals5Plus =     "+ toursEquals5Plus);
          logger.info("workOnly =             "+ workOnly);
          logger.info("schoolOnly =           "+ schoolOnly);
          logger.info("shopOnly =             "+ shopOnly);
          logger.info("recreateOnly =         "+ recreateOnly);
          logger.info("otherOnly =            "+ otherOnly);
          logger.info("isWeekend =            "+ isWeekend);
          logger.info("nWorkTours =            "+ nWorkTours);
          logger.info("nSchoolTours =          "+ nSchoolTours);
          logger.info("nShopTours =            "+ nShopTours);
          logger.info("nRecreateTours =        "+ nRecreateTours);
          logger.info("nOtherTours =           "+ nOtherTours);
          logger.info("nWorkBasedTours =       "+ nWorkBasedTours);
          logger.info("numberOfToursGT2 =     "+ numberOfToursGT2);
                                                                                                               
                                                                                                               
          //Number of intermediate stop variables                        
          logger.info("tour1IStops =          "+ tour1IStops);
          logger.info("tour2IStops =          "+ tour2IStops);
          logger.info("tour3IStops =          "+ tour3IStops);
          logger.info("tour4PIStops =         "+ tour4PIStops);
          logger.info("workTourIStops =       "+ workTourIStops);
          logger.info("nonWorkTourIStops =    "+ nonWorkTourIStops);
          logger.info("totalIStops =          "+ totalIStops);
          logger.info("IStopsEquals1 =        "+ IStopsEquals1);
          logger.info("IStopsEquals2Plus =    "+ IStopsEquals2Plus);
          logger.info("IStopsEquals2 =        "+ IStopsEquals2);
          logger.info("IStopsEquals3 =        "+ IStopsEquals3);
          logger.info("IStopsEquals3Plus =    "+ IStopsEquals3Plus);
          logger.info("IStopsEquals4Plus =    "+ IStopsEquals4Plus);
                                                                                                               
          //Combination of activities on tour variables                   
          logger.info("workPSchool =          "+ workPSchool);
          logger.info("workPShop =            "+ workPShop);
          logger.info("workPRecreate =        "+ workPRecreate);
          logger.info("workPOther =           "+ workPOther);
          logger.info("schoolPShop =          "+ schoolPShop);
          logger.info("schoolPRecreate =      "+ schoolPRecreate);
          logger.info("schoolPOther =         "+ schoolPOther);
          logger.info("shopPRecreate =        "+ shopPRecreate);
          logger.info("shopPOther =           "+ shopPOther);
          logger.info("recreatePOther =       "+ recreatePOther);
                                                                                         
          //stops variables                                                               
          logger.info("stopsOnWorkTours =     "+ stopsOnWorkTours);
          logger.info("stopsOnSchoolTours =   "+ stopsOnSchoolTours);
          logger.info("stopsOnShopTours =     "+ stopsOnShopTours);
          logger.info("stopsOnRecreateTours = "+ stopsOnRecreateTours);
          logger.info("stopsOnOtherTours =    "+ stopsOnOtherTours);
                                                                                                              
          //Sequence variables                                                              
          logger.info("tour1Purpose =         "+ tour1Purpose);
          logger.info("tour2Purpose =         "+ tour2Purpose);
          logger.info("tour3Purpose =         "+ tour3Purpose);
          logger.info("tour4Purpose =         "+ tour4Purpose);
          logger.info("tour1IsWork =          "+ tour1IsWork);
          logger.info("tour1IsSchool =        "+ tour1IsSchool);
          logger.info("workTourNotFirst =     "+ workTourNotFirst);
          logger.info("schoolTourNotFirst =   "+ schoolTourNotFirst);
                                                                                                               
     }
     
     
     public void print(PrintWriter f){
          f.print(     
               homeActivities+" "+
              workActivities+" "+
               schoolActivities+" "+
               shopActivities+" "+
               recreateActivities+" "+
               otherActivities+" "+
               nWorkBasedTours+" "+
               tour1IStops+" "+
               tour2IStops+" "+
               tour3IStops+" "+
               tour4PIStops+" "+
                workTourIStops+" "+
               nonWorkTourIStops+" "+
               workPSchool+" "+
               workPShop+" "+
                workPRecreate+" "+
               workPOther+" "+
               schoolPShop+" "+
               schoolPRecreate+" "+
               schoolPOther+" "+
               shopPRecreate+" "+
               shopPOther+" "+
               recreatePOther+" "+
               stopsOnWorkTours+" "+
               stopsOnSchoolTours+" "+
               stopsOnShopTours+" "+
               stopsOnRecreateTours+" "+
               stopsOnOtherTours+" "+
               tour1Purpose+" "+
               tour2Purpose+" "+
               tour3Purpose+" "+
               tour4Purpose+" "+
               t1Dummy+" "+
               t2Dummy+" "+
               t3Dummy+" "+
               t4Dummy+" "+
               t5pDummy+" "+
               wrkDummy+" "+     
               schDummy+" "+     
               shpDummy+" "+     
               recDummy+" "+     
               othDummy+" "+     
               wkbDummy+" "     
          );
     }
     
//to write to a text file, csv format
     public void printCSV(PrintWriter file){

          file.print(
               dayPattern+","
               +homeActivities+","
               +workActivities+","
               +schoolActivities+","
               +shopActivities+","
               +recreateActivities+","
               +otherActivities

                                                                                                                                                         
          );
     }
     
     public boolean equals(Object obj){
          
          Pattern comparePattern = (Pattern)obj;
          boolean tf=false;
        String compareString=comparePattern.dayPattern.toString();
          String thisString=this.dayPattern.toString();
          if(compareString.compareTo(thisString)==0)
               tf=true;
          return tf;
     }

     /**
     *
     * To print the pattern to the screen
     *
     */
//     public void print(){
//          thisPattern.print();
//          logger.info("Utility = "+utility);
//     }

 
    public Object clone() throws CloneNotSupportedException {
        Pattern newPattern;
        try {
            newPattern = (Pattern) super.clone();
            newPattern.dayPattern = dayPattern;

            //Simple number of activities by type in pattern variables
            newPattern.homeActivities = homeActivities;
            newPattern.workActivities = workActivities;
            newPattern.schoolActivities = schoolActivities;
            newPattern.shopActivities=shopActivities;
            newPattern.recreateActivities=recreateActivities;
            newPattern.otherActivities=otherActivities;
            newPattern.t1Dummy=t1Dummy;
            newPattern.t2Dummy=t2Dummy;
            newPattern.t3Dummy=t3Dummy;
            newPattern.t4Dummy=t4Dummy;
             newPattern.t5pDummy=t5pDummy;
             newPattern.wrkDummy=wrkDummy;
             newPattern.schDummy=schDummy;
             newPattern.shpDummy=shpDummy;
             newPattern.recDummy=recDummy;
             newPattern.othDummy=othDummy;
             newPattern.wkbDummy=wkbDummy;
             newPattern.toursEquals1=toursEquals1;
             newPattern.toursEquals2=toursEquals2;
             newPattern.toursEquals3Plus=toursEquals3Plus;
             newPattern.toursEquals3=toursEquals3;
             newPattern.toursEquals4=toursEquals4;
             newPattern.toursEquals5Plus=toursEquals5Plus;
             newPattern.workOnly=workOnly;
             newPattern.schoolOnly=schoolOnly;
             newPattern.shopOnly=shopOnly;
             newPattern.recreateOnly=recreateOnly;
             newPattern.otherOnly=otherOnly;
             newPattern.isWeekend=isWeekend;
             newPattern.nWorkTours=nWorkTours;
             newPattern.nSchoolTours=nSchoolTours;
             newPattern.nShopTours=nShopTours;
             newPattern.nRecreateTours=nRecreateTours;
             newPattern.nOtherTours=nOtherTours;
             newPattern.nWorkBasedTours=nWorkBasedTours;
             newPattern.numberOfToursGT2=numberOfToursGT2;

             //Number of intermediate stop variables
             newPattern.tour1IStops=tour1IStops;
             newPattern.tour2IStops=tour2IStops;
             newPattern.tour3IStops=tour3IStops;
             newPattern.tour4PIStops=tour4PIStops;
             newPattern.workTourIStops=workTourIStops;
             newPattern.nonWorkTourIStops=nonWorkTourIStops;
             newPattern.totalIStops=totalIStops;
             newPattern.IStopsEquals1=IStopsEquals1;
             newPattern.IStopsEquals2Plus=IStopsEquals2Plus;
             newPattern.IStopsEquals2=IStopsEquals2;
             newPattern.IStopsEquals3=IStopsEquals3;
             newPattern.IStopsEquals3Plus=IStopsEquals3Plus;
             newPattern.IStopsEquals4Plus=IStopsEquals4Plus;

             //Combination of activities on tour variables
             newPattern.workPSchool=workPSchool;
             newPattern.workPShop=workPShop;
              newPattern.workPRecreate=workPRecreate;
             newPattern.workPOther=workPOther;
             newPattern.schoolPShop=schoolPShop;
             newPattern.schoolPRecreate=schoolPRecreate;
             newPattern.schoolPOther=schoolPOther;
             newPattern.shopPRecreate=shopPRecreate;
             newPattern.shopPOther=shopPOther;
             newPattern.recreatePOther=recreatePOther;

             //stops variables
             newPattern.stopsOnWorkTours=stopsOnWorkTours;
             newPattern.stopsOnSchoolTours=stopsOnSchoolTours;
             newPattern.stopsOnShopTours=stopsOnShopTours;
             newPattern.stopsOnRecreateTours=stopsOnRecreateTours;
             newPattern.stopsOnOtherTours=stopsOnOtherTours;

             //Sequence variables
             newPattern.tour1Purpose=tour1Purpose;
             newPattern.tour2Purpose=tour2Purpose;
             newPattern.tour3Purpose=tour3Purpose;
             newPattern.tour4Purpose=tour4Purpose;
             newPattern.tour1IsWork=tour1IsWork;
             newPattern.tour1IsSchool=tour1IsSchool;
             newPattern.workTourNotFirst=workTourNotFirst;
             newPattern.schoolTourNotFirst=schoolTourNotFirst;

             //pattern file variables
             newPattern.observed=observed;

             //added from Pattern Alternative//
             newPattern.isAvailable=isAvailable;
             newPattern.hasUtility=hasUtility;
             newPattern.utility=utility;
             newPattern.constant=constant;
             newPattern.expConstant=expConstant;
             newPattern.name=name;
             }
             catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.toString());
             }
               return newPattern;
            }
    
    public String toString(){
        return dayPattern.toString();
    }

    public static void main(String[] args) {
        Pattern testPattern = new Pattern("hsssh");
        testPattern.print();
    }
    
    /**
     * Convert an array of tours to a pattern string.
     * @param tours Tour array
     * @return String Pattern string
     */
    public static String toursToPatternString(Tour[] tours) {
	String pattern = "H";

	if (tours != null) {
	    for (Tour tour : tours) {
		if (tour.intermediateStop1 != null) {
		    try {
			pattern += ActivityPurpose
				.getActivityString(tour.intermediateStop1.activityPurpose);
		    } catch (NullPointerException e) {
                logger.error("Tour with out activityPurpose "
                        + "intermediate stop 1: ");
		    }
		}
		pattern += ActivityPurpose
			.getActivityString(tour.primaryDestination.activityPurpose);
		if (tour.intermediateStop2 != null) {
		    if (tour.intermediateStop2.activityPurpose == null) {
			logger.error("Tour with out activityPurpose "
				+ "intermediate stop 2: ");
			tour.print();
		    } else {
			pattern += ActivityPurpose
				.getActivityString(tour.intermediateStop2.activityPurpose);
		    }
		}
		pattern += "H";
	    }
	}

	return pattern;
    }
}
