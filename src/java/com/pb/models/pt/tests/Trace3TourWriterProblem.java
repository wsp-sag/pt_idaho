/*
 * Copyright 2007 PB Consult Inc.
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
package com.pb.models.pt.tests;

import java.io.PrintWriter;

import org.apache.log4j.Logger;

import com.pb.models.pt.Activity;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.ActivityType;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTDataWriter;
import com.pb.models.pt.PTPerson;
import com.pb.models.pt.Tour;

/**
@author Ofir Cohen
@version 1.0, Feb 13, 2007 
*/
public class Trace3TourWriterProblem {

    final static Logger logger = Logger.getLogger(Trace3TourWriterProblem.class);
    
    private static Tour tour;
    
    private static PrintWriter pWriter;
    

    static PTHousehold [] myHHArray= new PTHousehold[1];
    
    
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        myHHArray[0]= new PTHousehold();
        PTPerson []p= new PTPerson[3];
        
        char[] purposes1 = { 's', 'c', 'b', 'o', 'r', 'w', 'o', 's'};
        p[0]=createPerson(purposes1);
              
        char[] purposes2 = { 's', 'c', 'b', 'o'};
        p[1]=createPerson(purposes2);
        
        char[] purposes3 = { 's', 'c', 'b'};
        p[2]=createPerson(purposes3);
        
        // Adding intermidiate stops for person 1
        for ( int i=0;i<8;i++) {
            tour = p[0].weekdayTours[i];
            
            tour.begin = new Activity();  
            tour.begin.activityType = ActivityType.BEGIN;
            tour.begin.activityNumber = 1;
            tour.begin.activityPurpose =ActivityPurpose.HOME;
            
            tour.intermediateStop1 = new Activity();            
            tour.intermediateStop1.activityType = ActivityType.INTERMEDIATE_STOP;
            tour.intermediateStop1.activityNumber = 2;
            tour.intermediateStop1.activityPurpose =ActivityPurpose.OTHER;
            
            
            tour.primaryDestination = new Activity();  
            tour.primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
            tour.primaryDestination.activityNumber = 1;
            tour.primaryDestination.activityPurpose =ActivityPurpose.WORK;
            
                        
            tour.intermediateStop2 = new Activity();            
            tour.intermediateStop2.activityType = ActivityType.INTERMEDIATE_STOP;
            tour.intermediateStop2.activityNumber = 3;
            tour.intermediateStop2.activityPurpose =ActivityPurpose.SHOP;
            
            tour.end = new Activity();            
            tour.end.activityType = ActivityType.END;
            tour.end.activityNumber = 3;
            tour.end.activityPurpose =ActivityPurpose.HOME;

            
        }
        
            // Adding intermidiate stops for person 2
            
        for ( int i=0;i<3;i++) {
            
            tour = p[1].weekdayTours[i];
            
            tour.begin = new Activity();  
            tour.begin.activityType = ActivityType.BEGIN;
            tour.begin.activityNumber = 1;
            tour.begin.activityPurpose =ActivityPurpose.HOME;
            
            tour.primaryDestination = new Activity();  
            tour.primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
            tour.primaryDestination.activityNumber = 1;
            tour.primaryDestination.activityPurpose =ActivityPurpose.WORK;
            
            tour.end = new Activity();            
            tour.end.activityType = ActivityType.END;
            tour.end.activityNumber = 3;
            tour.end.activityPurpose =ActivityPurpose.HOME;
        }
        
        tour = p[1].weekdayTours[3];
        tour.begin = new Activity();  
        tour.begin.activityType = ActivityType.BEGIN;
        tour.begin.activityNumber = 1;
        tour.begin.activityPurpose =ActivityPurpose.HOME;
        
        tour.intermediateStop1 = new Activity();            
        tour.intermediateStop1.activityType = ActivityType.INTERMEDIATE_STOP;
        tour.intermediateStop1.activityNumber = 2;
        tour.intermediateStop1.activityPurpose =ActivityPurpose.OTHER;
        
        
        tour.primaryDestination = new Activity();  
        tour.primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
        tour.primaryDestination.activityNumber = 1;
        tour.primaryDestination.activityPurpose =ActivityPurpose.WORK;
        
                    
        tour.intermediateStop2 = new Activity();            
        tour.intermediateStop2.activityType = ActivityType.INTERMEDIATE_STOP;
        tour.intermediateStop2.activityNumber = 3;
        tour.intermediateStop2.activityPurpose =ActivityPurpose.SHOP;
        
        tour.end = new Activity();            
        tour.end.activityType = ActivityType.END;
        tour.end.activityNumber = 3;
        tour.end.activityPurpose =ActivityPurpose.HOME;

            
      // Adding intermidiate stops for person 3
      //Tour 1 Person 3
        tour = p[2].weekdayTours[0];
            
        tour.begin = new Activity();  
        tour.begin.activityType = ActivityType.BEGIN;
        tour.begin.activityNumber = 1;
        tour.begin.activityPurpose =ActivityPurpose.HOME;
         
        tour.primaryDestination = new Activity();  
        tour.primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
        tour.primaryDestination.activityNumber = 1;
        tour.primaryDestination.activityPurpose =ActivityPurpose.WORK;
                   
        tour.end = new Activity();            
        tour.end.activityType = ActivityType.END;
        tour.end.activityNumber = 3;
        tour.end.activityPurpose =ActivityPurpose.HOME;
          
        // Tour 2 Person 3
        tour = p[2].weekdayTours[1];
        tour.begin = new Activity();  
        tour.begin.activityType = ActivityType.BEGIN;
        tour.begin.activityNumber = 1;
        tour.begin.activityPurpose =ActivityPurpose.HOME;
         
        tour.intermediateStop1 = new Activity();            
        tour.intermediateStop1.activityType = ActivityType.INTERMEDIATE_STOP;
        tour.intermediateStop1.activityNumber = 2;
        tour.intermediateStop1.activityPurpose =ActivityPurpose.OTHER;
         
        tour.primaryDestination = new Activity();  
        tour.primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
        tour.primaryDestination.activityNumber = 1;
        tour.primaryDestination.activityPurpose =ActivityPurpose.WORK;
            
        tour.end = new Activity();            
        tour.end.activityType = ActivityType.END;
        tour.end.activityNumber = 3;
        tour.end.activityPurpose =ActivityPurpose.HOME;
            
        // Tour 3 Person 3  
        tour = p[2].weekdayTours[2];
        tour.begin = new Activity();  
        tour.begin.activityType = ActivityType.BEGIN;
        tour.begin.activityNumber = 1;
        tour.begin.activityPurpose =ActivityPurpose.HOME;
            
        tour.primaryDestination = new Activity();  
        tour.primaryDestination.activityType = ActivityType.PRIMARY_DESTINATION;
        tour.primaryDestination.activityNumber = 1;
        tour.primaryDestination.activityPurpose =ActivityPurpose.WORK;
                        
        tour.end = new Activity();            
        tour.end.activityType = ActivityType.END;
        tour.end.activityNumber = 3;
        tour.end.activityPurpose =ActivityPurpose.HOME;
             
      
        
        
        myHHArray[0].persons=p;
        try{
            pWriter= new PrintWriter("c:/models/tlumip/bugIn3Tour.csv");
        }catch (Exception e) {
            logger.info("writer couldn't instantiated");
        }
        PTDataWriter.writeToursToTextFile(myHHArray,pWriter,true );
              
    }

    private static PTPerson createPerson (char[] purposes) {
        logger.info("Creating a person with a complex series of tours.");
        PTPerson person = new PTPerson();
        logger.info("Created a person.");
        
        Tour[] tours = new Tour[purposes.length];

        // set-up the week day tour array
        for (int i = 0; i < tours.length; ++i) {
            logger.info("Creating tour " + i);
            tours[i] = new Tour();
            tours[i].primaryDestination.activityPurpose = ActivityPurpose
                    .getActivityPurpose(purposes[i]);
        }
        person.weekdayTours = tours;

        logger.info("Exiting the person creation.");
        return person;    
    }
}

