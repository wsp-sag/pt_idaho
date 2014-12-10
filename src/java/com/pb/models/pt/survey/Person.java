// survey.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.models.pt.survey;
import java.util.*;
import java.io.*;

import com.pb.common.util.OutTextFile;

public class Person{

	public long sampleNumber;

	public int personNumber;
	public int relationship;
	public boolean female;
	public float age;
	public int license;
	public int employmentStatus;
	public int work;
	public int occupation;
	public int industry;
	public int lengthAtJob;
	public int telecommute;
	public boolean shift;
	public int studentStatus;
	public int studentLevel;
	public int educationLevel;
	public int ethnicity;
	public boolean disabled;
	public int typeDisability1;
	public int typeDisability2;

	public ArrayList dayActivities = new ArrayList();
	public ArrayList dayTrips = new ArrayList();
	public ArrayList dayTours = new ArrayList();
	

    /** getWordWithStops returns a word describing the activities
    on Home-Based tours in person-day 1. Two at-home 
    activities in  a row are reported as one letter 'h'.
    The codes are h=home,w=work-no workbased,b=work-workbased, c=school,s=shop,
    r=soc/rec,o=other.  Each intermediate stop activity is 
    enumerated, with the stop purpose. */
    public String getWordWithStops(int dayno){
        char[] activityLetters = new char[] { 'b',
            'w','c','s','s','r','o','o','o','w'};
        StringBuffer dayLetters = new StringBuffer();
        ListIterator t=dayTours.listIterator();
		boolean workBasedTour=false;
        while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity thisActivity=(Activity)thisTour.getLeaveOrigin();
			
			//if tour is work-based, continue
			if(thisTour.type=='b'){
				workBasedTour=true;
				thisActivity.letter='b';
				thisActivity=(Activity)thisTour.getArriveOrigin();
				thisActivity.letter='b';
				thisActivity=(Activity)thisTour.getDestination();
				thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				if(thisTour.hasIntermediateStop1){
					thisActivity=(Activity)thisTour.getIntermediateStop1();
					thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				}
				if(thisTour.hasIntermediateStop2){
					thisActivity=(Activity)thisTour.getIntermediateStop2();
					thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				}
				continue;
			}
			
			thisActivity.letter='h';
			//append h to dayLetters
            dayLetters.append("h");
			//Intermediate Stop 1
            if(thisTour.hasIntermediateStop1){
				thisActivity = (Activity)thisTour.getIntermediateStop1();
				thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
			}
			//primary destination
			thisActivity = (Activity)thisTour.getDestination();

			if(activityLetters[thisActivity.getGeneralActivity()]=='w'){
				//it is a work tour, check to see if last tour was a work-based
				if(workBasedTour){
					dayLetters.append('b');
					workBasedTour=false;
					thisActivity.letter='b';
				}else{
					dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
           			thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
           		}
			}else{
				dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
	            thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
            }
			//intermediate stop 2
			if(thisTour.hasIntermediateStop2){
				thisActivity = (Activity)thisTour.getIntermediateStop2();
				thisActivity.letter=activityLetters[thisActivity.getGeneralActivity()];
				dayLetters.append(activityLetters[thisActivity.getGeneralActivity()]);
			}
			thisActivity = (Activity)thisTour.getArriveOrigin();
			if(thisActivity.getGeneralActivity()==1||thisActivity.getGeneralActivity()==9){
				if(thisActivity.getGeneralActivity()==1)
					thisActivity.letter='w';
				else
					thisActivity.letter='b';
			}else{
				thisActivity.letter='h';
			}	
		} //next tour
        dayLetters.append("h");
      	return dayLetters.toString();
	}
			
          
	/** numberOfDay1HomeBasedTourIStops searches the number of the tour
   passed as an argument and returns the number of stops on the tour if 
   it is a home-based tour. 
	@param numberOfTour
    @return number of intermediate stops on the tour, 0 if no stops or no tour, -1 if
    not a home-based tour */
 	public int numberOfDayHomeBasedTourIStops(int numberOfTour){

		int numberOfIStops=0;

		if(numberOfTour<=dayTours.size()){
			Tour thisTour = (Tour)dayTours.get(numberOfTour-1);
			Activity originActivity = (Activity)thisTour.getLeaveOrigin();
			if(originActivity.getGeneralActivity()==1||originActivity.getGeneralActivity()==9){
				numberOfIStops=-1;
			}else{
				if(thisTour.hasIntermediateStop1)
					++numberOfIStops;
				if(thisTour.hasIntermediateStop2)
					++numberOfIStops;
			}
		}
		return numberOfIStops;
	}

	/** numberOfDay1Tours searches the generalActivity number
	of each tour primary destination and returns the number
	of tours where the primary destination general Activity equals
	the searchActivity	*/
	public int numberOfDayActivityTours(int searchActivity){
		int  numberTours=0;
		ListIterator t = dayTours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity destination = (Activity)thisTour.destination;
			if(destination.generalActivity==searchActivity)
				++numberTours;
		}
		return numberTours;
	}

	/** This method returns the total number of intermediate stops
	on all tours on day1 */
	public int numberOfDayStops(){
		int numberStops=0;
		ListIterator t = dayTours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			if(thisTour.hasIntermediateStop1)
				++numberStops;
			if(thisTour.hasIntermediateStop2)
				++numberStops;
		}
		return numberStops;
	}
	
	/** This method returns the number of day1tours that begin between the beginning
	and ending times passed as arguments.  The beginning time is the time leaving
	home for Home-Based Tours and the time leaving work for Work-Based Tours */
	public int numberOfDayToursBeginning(int fromTime, int toTime){
			
		int numberTours=0;
		ListIterator t= dayTours.listIterator();
		while(t.hasNext()){
			Tour thisTour=(Tour)t.next();
			Activity originActivity	= (Activity)thisTour.leaveOrigin;
			//convert origin activity ending time to military
			int leaveTime = originActivity.endHour*100 + originActivity.endMinute;
			if(originActivity.endAMPM==2 && originActivity.endHour!=12)
				leaveTime=leaveTime+1200;

			if(leaveTime>=fromTime && leaveTime<toTime)
				++numberTours;
		}
		return numberTours;
	}	
	
    public boolean isWorker(){
        if(employmentStatus>=1 && employmentStatus<=4)
            return true;
        else 
            return false;
    }
    public boolean isStudent(){
        if(studentStatus==1||studentStatus==2)
            return true;
        else
            return false;
    }		
	public long getSampleNumber(){
		return sampleNumber;
	}
	public int getPersonNumber(){
		return personNumber;
	}
	public int getStudentStatus(){
		return studentStatus;
	}
	public int getEmploymentStatus(){
		return employmentStatus;
	}
	public void setDayActivities(ArrayList a){
		dayActivities=a;
	}
	public ArrayList getDayActivities(){
		return dayActivities;
	}
	public void setDayTrips(ArrayList trips){
		dayTrips=trips;
	}
	public ArrayList getDayTrips(){
		return dayTrips;
	}
	public void setDayTours(ArrayList tours){
		dayTours=tours;
	}
	public ArrayList getDayTours(){
		return dayTours;
	}
	
	//to print to screen
	public void print(){
		System.out.println(personNumber+","+relationship+","+female+","+
			age+","+license+","+employmentStatus+","+occupation+","+
			industry+","+studentStatus+","+studentLevel+",");
	}
	/* to print to file, takes a PrintWriter Object - no new line,space-delimited
	* personNumber
	* relationship
	* female
	* age
	* license
	* employmentStatus
	* occupation
	* industry
	* studentStatus
	* studentLevel
	*/
	public void print(PrintWriter f){
		f.print(
			personNumber+" "+
			relationship+" ");
		if(female)
			f.print("1 ");
		else
			f.print("0 ");
		f.print(
			age+" "+
			license+" "+
			employmentStatus+" "+
			occupation+" "+
			industry+" ");
		f.print(
			studentStatus+" "+
			studentLevel+" ");
	}
	//to print to file, takes a OutTextFile object - no new line,space-delimited
	public void print(OutTextFile f) throws IOException{
		f.print(personNumber+","+relationship+","+female+","+
				age+","+license+","+employmentStatus+","+occupation+","+
				industry+","+studentStatus+","+studentLevel+",");
	}
	//print all to screen
	public void printAll(){
		//print the person data
		System.out.println(personNumber+","+relationship+","+female+","+
				age+","+license+","+employmentStatus+","+occupation+","+
				industry+","+studentStatus+","+studentLevel+",");
		//print the day1Activities data
		for(int i=0;i<dayActivities.size();++i)
			((Activity)dayActivities.get(i)).print();
		//print the day1Trips data
		for(int i=0;i<dayTrips.size();++i)
			((Activity)dayTrips.get(i)).print();

}


	
	public static void main(String[] args) {}
}