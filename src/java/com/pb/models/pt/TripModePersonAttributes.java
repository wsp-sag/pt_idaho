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

import java.io.PrintWriter;

import org.apache.log4j.Logger;


/** 
 * Person Attributes for Trip Mode Choice
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */

public class TripModePersonAttributes {

    final static Logger logger = Logger.getLogger("com.pb.idaho.pt.default");
    
    public int age;
    public int autos;
    public int size2;
    public int size3p;
    public int inclow;
    public int incmed;
    public int inchi;
    public int tourOriginTaz;
    public int tourDestinationTaz;
    public int originTaz;
    public int destinationTaz;
    public int transitLeg;
    public int passengerLeg;
    public int tripNumber;
    public int totalTripsOnTour;
    public short tourOriginDepartTime;
    public short tourPrimaryDestinationDepartTime;
    public short tripDepartTime;

   /**
    * Default constructor.
    *
    */
   public TripModePersonAttributes(){
        
             age=0;
             autos=0;
             size2=0;
             size3p=0;
             inclow=0;
             incmed=0;
             inchi=0;
            originTaz=0;
            destinationTaz=0;
   }
   /**
    * Set the person trip mode attributes.
    * @param thisHousehold
    * @param thisPerson
    * @param thisTour
    * @param tripNumber The number of the trip on the tour.
    */
   public void setAttributes(PTHousehold thisHousehold, PTPerson thisPerson, Tour thisTour, int tripNumber){
        
       age=0;
       autos=0;

       size2=0;
       size3p=0;
       inclow=0;
       incmed=0;
       inchi=0;
            
       age=thisPerson.age;
       autos=thisHousehold.autos;
       
        if(thisHousehold.size==2)
            size2=1;
        else if(thisHousehold.size>=3)
            size3p=1;
        
        if(thisHousehold.income<20000)
            inclow=1;
        else if(thisHousehold.income>=20000 && thisHousehold.income<=60000)
            incmed=1;
        else
            inchi=1;
                  
        passengerLeg=0;
        transitLeg=0;
        
        if((TourModeType) thisTour.primaryMode.type==TourModeType.AUTOPASSENGER)
            passengerLeg=1;
        else if((TourModeType)thisTour.primaryMode.type ==TourModeType.WALKTRANSIT)
            transitLeg=1;
//        else if((TourModeType)thisTour.primaryMode.type ==TourModeType.DRIVETRANSIT)
//            transitLeg=1;
        
        tourOriginTaz = thisTour.begin.location.zoneNumber;
        tourDestinationTaz = thisTour.primaryDestination.location.zoneNumber;
        
        originTaz = thisTour.getOriginActivity(tripNumber).location.zoneNumber;
        destinationTaz =  thisTour.getDestinationActivity(tripNumber).location.zoneNumber;
        
        this.tripNumber = tripNumber;
        totalTripsOnTour = 2;
        if(thisTour.hasOutboundStop()) ++totalTripsOnTour;
        if(thisTour.hasInboundStop()) ++totalTripsOnTour;
        
        tourOriginDepartTime = thisTour.begin.endTime;
        tourPrimaryDestinationDepartTime = thisTour.primaryDestination.endTime;
        tripDepartTime = thisTour.getOriginActivity(tripNumber).endTime;
        
        //first trip
        if(tripNumber==1){
            if((TourModeType) thisTour.primaryMode.type==TourModeType.PASSENGERTRANSIT)
                passengerLeg=1;
            else if((TourModeType) thisTour.primaryMode.type==TourModeType.TRANSITPASSENGER)
                transitLeg=1;
        }else if(tripNumber==2){
            if(thisTour.hasOutboundStop()){
                if((TourModeType) thisTour.primaryMode.type==TourModeType.PASSENGERTRANSIT)
                    passengerLeg=1;
                else if((TourModeType) thisTour.primaryMode.type==TourModeType.TRANSITPASSENGER)
                    transitLeg=1;
            }else{
                if((TourModeType) thisTour.primaryMode.type==TourModeType.PASSENGERTRANSIT)
                    transitLeg=1;
                else if((TourModeType) thisTour.primaryMode.type==TourModeType.TRANSITPASSENGER)
                    passengerLeg=1;
            }
        }else if(tripNumber==3){
            if((TourModeType) thisTour.primaryMode.type==TourModeType.PASSENGERTRANSIT)
                transitLeg=1;
            else if((TourModeType) thisTour.primaryMode.type==TourModeType.TRANSITPASSENGER)
                passengerLeg=1;
        }else if(tripNumber==4){
            if((TourModeType) thisTour.primaryMode.type==TourModeType.PASSENGERTRANSIT)
                transitLeg=1;
            else if((TourModeType) thisTour.primaryMode.type==TourModeType.TRANSITPASSENGER)
                passengerLeg=1;
        }else{
            logger.fatal("Error: Current version of code does not support tours with "+tripNumber+" trips");
            logger.fatal("Person ID: "+thisPerson.hhID);
            throw new RuntimeException();
        }
        
   
   }        
   /** 
    * Print the properties to a file.
    * @param file
    */
    public void print(PrintWriter file){
        file.println("PersonTripModeAttributes:");
        file.println("\tage = " + age);
        file.println("\tautos = " + autos);
        file.println("\tsize2 = " + size2);
        file.println("\tsize3p = " + size3p);
        file.println("\tinclow = " + inclow);
        file.println("\tincmed = " + incmed);
        file.println("\tinchi = " + inchi);
        file.println();
        file.println();

        file.flush();
    }

}
     
     
