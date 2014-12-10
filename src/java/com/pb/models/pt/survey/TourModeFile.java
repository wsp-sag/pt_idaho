// TourModeFile.java
//
//
// This class writes a file summarizing activities, tours,and tour mode for each person-day
// 
package com.pb.models.pt.survey;
import java.util.*;
import java.io.*;

import com.pb.common.util.OutTextFile;

public class TourModeFile{

	/** constructor takes a list of households and a filename to write to
	* tours should be coded on household list.
	* format of output file includes:
	* Household Data
	* Person Data
	* Person Day
	* acts           	d1acts         
	* tours          	d1tours        
	* work           	d1work         
	* school         	d1school       
	* majshop        	d1majshp       
	* othshop        	d1othshp       
	* soc            	d1soc          
	* oth            	d1oth          
	* pudo           	d1pudo         
	* wrk-based      	d1wrkbas       
	* wrkjob2        	d1wrkjb2       
	* Intstops       	d1Intstp       
	* Night          	d1Night        
	* AMPeak         	d1AMPeak       
	* AMVall         	d1AMVall       
	* Midday         	d1Midday       
	* PMVall         	d1PMVall       
	* PMPeak         	d1PMPeak       
	* Evening        	d1Evenin      
    * WordPattern       WrdWStps 
    * 
*/

	public void PersonFile(List households, String personFileName) throws IOException{
		//open report file
		OutTextFile personFile = new OutTextFile();
		PrintWriter pFile = personFile.open(personFileName);

		ListIterator h = households.listIterator();
		while(h.hasNext()){
			Household thisHousehold = (Household)h.next();
			ListIterator p = thisHousehold.persons.listIterator();
			while(p.hasNext()){
				Person thisPerson = (Person)p.next();
				thisHousehold.print(pFile);
				thisPerson.print(pFile);
				//day1stats
				pFile.print(1+" ");
				//Number of activities,tours 
				pFile.print(thisPerson.dayActivities.size()+" ");
				pFile.print(thisPerson.dayTours.size()+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(1)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(2)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(3)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(4)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(5)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(6)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(7)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(8)+" ");
				pFile.print(thisPerson.numberOfDayActivityTours(9)+" ");
				pFile.print(thisPerson.numberOfDayStops()+" ");
				//number of day1 tours by time period
				pFile.print(thisPerson.numberOfDayToursBeginning(   0, 700)+" "); //night
				pFile.print(thisPerson.numberOfDayToursBeginning( 700, 900)+" "); //am peak
				pFile.print(thisPerson.numberOfDayToursBeginning( 900,1200)+" "); //am valley
				pFile.print(thisPerson.numberOfDayToursBeginning(1200,1300)+" "); //midday   
				pFile.print(thisPerson.numberOfDayToursBeginning(1300,1600)+" "); //pm valley
				pFile.print(thisPerson.numberOfDayToursBeginning(1600,1800)+" "); //pm peak  
				pFile.print(thisPerson.numberOfDayToursBeginning(1800,2400)+" "); //evening
                pFile.print(thisPerson.getWordWithStops(1)+" ");
				Pattern day1Pattern = new Pattern(thisPerson.getWordWithStops(1));
				day1Pattern.print(pFile);



				pFile.println();				
				thisHousehold.print(pFile);
				thisPerson.print(pFile);
			}
		}
	
		pFile.close();
	}
}
