//PrioritizeActivities.java
//
// A class library for travel survey data
// rp 5/05

package com.pb.models.pt.survey;
import java.util.*;

public class PrioritizeActivities {

	//constructor takes a list of households, iterates through, calls method for Lists of activities
	public PrioritizeActivities(List households){

		System.out.println("Prioritizing activities at each location");

		for(int i=0;i<households.size();++i){
			Household h = (Household) households.get(i);
			for(int j=0;j<h.persons.size();++j){							// get person
				Person p = (Person)h.getPersons().get(j);
//				System.out.println("Household "+h.sampleNumber+" Person "+p.personNumber);
				for(int k=0;k<p.dayActivities.size();++k){                  // get activity record
					MpoActivity a = (MpoActivity)p.getDayActivities().get(k);
			        a.activity = findMainPurpose(a);
				}
			}
		}
	}
	
	//This method finds the highest priority activity purpose for each location
	
    int findMainPurpose(MpoActivity a){
    	
    	boolean[] tests = new boolean[7];
    	int mainPurpose;
    	
    	for(int i=0;i<5;++i){

    		if(a.activityPurposes[i]>=9 && a.activityPurposes[i]<=16)          //home
    			tests[6]=true;
    		else if(a.activityPurposes[i]==17)                                 //work
    			tests[0]=true;
    		else if(a.activityPurposes[i]==18)                                 //school
    			tests[1]=true;
    		else if(a.activityPurposes[i]==24)                                 //shop
    			tests[2]=true;
    		else if(a.activityPurposes[i]==24||a.activityPurposes[i]==26)      //social-recreational-eat meal
    			tests[3]=true;
    		else if(a.activityPurposes[i]==20)                                 //pickup-dropoff
    			tests[4]=true;
    		else                                                               //other
    			tests[5]=true;
    		}
    	
		//iterate through array of tests, set the value of activity to 1 + the test index
    	mainPurpose=99;
		for(int i=0;i<tests.length;++i){
			if(tests[i]==true){
				mainPurpose=i+1;
				break;
			}
		}
		if(mainPurpose==99){
			System.out.println("Main activity purpose incorrectly set");
		}
		return mainPurpose;
    }
}
