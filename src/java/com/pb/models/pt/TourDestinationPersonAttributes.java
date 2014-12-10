/*
 * Copyright 2006 PB Consult Inc.
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
 * This class is used for ...
 * Author: Christi Willison
 * Date: Nov 22, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class TourDestinationPersonAttributes {

    float twoTours;
    float threePlusTours;
    float oneStop;
    float twoStops;
    float preSchoolAtHome;
    int originTaz;


    /**
     * Default constructor.
     *
     *
     */
    public TourDestinationPersonAttributes(){

    }

     /**
      * Initialize all variables to 0.
      *
      */
      public void initialize(){
          twoTours=0;
          threePlusTours=0;
          oneStop=0;
          twoStops=0;
          preSchoolAtHome=0;
          originTaz=0;
       }

      /**
       * calculate terms based on person and household attributes.
       * @param thisHousehold PTHousehold
       * @param thisPerson PTPerson
       * @param thisTour Tour
       *
       */
      public void setAttributes(PTHousehold thisHousehold, PTPerson thisPerson, Tour thisTour){

          initialize();

          originTaz = thisTour.begin.location.zoneNumber;

          Tour[] tours = thisPerson.weekdayTours;

          if(tours != null){
            if(tours.length ==2)
              twoTours=1;
            else if(tours.length >= 3)
              threePlusTours=1;
          }

          // tabulate stops
          int stops=0;
          if(thisTour.hasInboundStop())
            ++stops;
          if(thisTour.hasOutboundStop())
             ++stops;

          if(stops==1)
              oneStop=1;
          else if(stops==2)
              twoStops=1;


          //check for pre-schooler
          PTPerson[] persons = thisHousehold.persons;
          if(persons != null){
              for (PTPerson person : persons)
                  if (person.age <= 5) preSchoolAtHome = 1;

          }
      }
}
