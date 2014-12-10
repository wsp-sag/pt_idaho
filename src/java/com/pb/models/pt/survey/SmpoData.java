// SmpoData.java
//
// This library is used to read and code small MPO and rural area data
// Modified from MpoData.java jef 8/00 (tlumip)
// rpm 5/05 

package com.pb.models.pt.survey;

import com.pb.common.util.*;
import java.io.*;
import java.util.*;

import com.pb.models.pt.survey.Household;
import com.pb.models.pt.survey.Person;
import com.pb.models.pt.survey.Activity;

//import com.pb.models.pt.survey.Location;
//import com.pb.models.pt.survey.Trip;

// temporarily commented out
//import com.pb.models.pt.survey.TripCoder;
//import com.pb.models.pt.survey.TourCoder;
//import com.pb.models.pt.survey.TourSummaryStatistics;
//import com.pb.models.pt.survey.PersonFile;
//import com.pb.models.pt.survey.TourFile;
//import com.pb.models.pt.survey.NestedPatternGenerationFile;

//import com.pb.models.pt.survey.PatternEstimation;
//import com.pb.models.pt.survey.ActivityDurationFile;

public class SmpoData {

	public static void main(String[] args) throws IOException {

		System.out.println("Executing SmpoData");

		MpoHouseholds hh = new MpoHouseholds(args[0]);

		ArrayList households = hh.getArray();

		MpoPersons p = new MpoPersons(args[1], households);
		MpoActivities a = new MpoActivities(args[2], households);

		//Find the main activity when multiple activities reported at one location
		new PrioritizeActivities(households);

		//Coding trips for groups of activities at one location
		//		new TripCoder(households);

		for (int i = 0; i < hh.getArray().size(); ++i) {
			if (((Household) households.get(i)).sampleNumber == 1141386) {
				((Household) households.get(i)).printAll();
				break;
			}
		}

		//		new TourCoder(households);
		//		new TourSummaryStatistics(households,"mpo.rpt");
		//		new PersonFile(households,"personTours.dat");
		//		new TourFile(households,"tours.dat");
		//		NestedPatternGenerationFile pg =	new NestedPatternGenerationFile(households,"mpoworkerpatterns.dat",
		//		    "mpostudentpatterns.dat","mpootherpatterns.dat","mpowkendpatterns.dat");

		// //		new PatternEstimation(households,pg.weekdayPatterns,pg.weekendPatterns);
		// //		new ActivityDurationFile(households);
	}

}
//class MpoHouseholds
//This holds data from the Mpo File in an ArrayList

class MpoHouseholds {

	ArrayList households = new ArrayList();

	MpoHouseholds(String fileName) throws IOException {

		//read household file
		System.out.println("Reading " + fileName);
		InTextFile householdFile = new InTextFile();
		householdFile.open(fileName);

		String inHousehold = new String();

		while ((inHousehold = householdFile.readLine()) != null) {
			if (inHousehold.length() == 0)
				break;
			try {
				households.add(parse(inHousehold));
			} catch (Exception e) {
				System.out.println("Error parsing household:\n" + inHousehold);
				System.exit(1);
			}
		}
		householdFile.close();
	}

	//parse method takes a string and returns a household object
	Household parse(String inString) {

		MpoHousehold h = new MpoHousehold();
		StringTokenizer inToken = new StringTokenizer(inString, ",");

		h.recType = new Integer(inToken.nextToken()).intValue();
		h.sampleNumber = new Long(inToken.nextToken()).longValue();
		inToken.nextToken();
		h.assign = new Integer(inToken.nextToken()).intValue();
		h.travelDay = new Integer(inToken.nextToken()).intValue();
		h.stayHome = new Integer(inToken.nextToken()).intValue();
		h.overnightVisitors = new Integer(inToken.nextToken()).intValue();
		h.transientVisitors = new Integer(inToken.nextToken()).intValue();
		h.workers = new Integer(inToken.nextToken()).intValue();
		h.students = new Integer(inToken.nextToken()).intValue();
		h.drivers = new Integer(inToken.nextToken()).intValue();
		h.householdSize = new Integer(inToken.nextToken()).intValue();
		h.typeHome = new Integer(inToken.nextToken()).intValue();
		h.ownHome = new Integer(inToken.nextToken()).intValue();
		h.numberVehicles = new Integer(inToken.nextToken()).intValue();
		h.incomeLevel = new Integer(inToken.nextToken()).intValue();
		h.countyFips = new Integer(inToken.nextToken()).intValue();
		h.mpo = new Integer(inToken.nextToken()).intValue();
		h.expWeight = new Float(inToken.nextToken()).floatValue();
		h.survey = new Integer(inToken.nextToken()).intValue();
		inToken.nextToken();
		h.taz = new Integer(inToken.nextToken()).intValue();
		inToken.nextToken();
		h.state = inToken.nextToken();
		return h;
	}

	//to get the arraylist
	ArrayList getArray() {
		return households;
	}
}

//class MPO household 

class MpoHousehold extends Household {

	int recType;

	int survey;

	int travelDay;

	int stayHome;

	int overnightVisitors;

	int transientVisitors;

	int workers;

	int students;

	int drivers;

	int countyFips;

	int mpo;

	String state;

	int taz;

	float expWeight;
}

//class MpoPersons
//This class parses a line from the Mperson File into the Person class.

class MpoPersons {

	MpoPersons(String fileName, ArrayList households) throws IOException {

		//read person file
		System.out.println("Reading " + fileName);
		InTextFile personFile = new InTextFile();
		personFile.open(fileName);

		String inPerson = new String();
		int read = 0, allocated = 0;

		while ((inPerson = personFile.readLine()) != null) {
			Person p = new Person();
			if (inPerson.length() == 0)
				break;
			try {
				p = parse(inPerson);
			} catch (Exception e) {
				System.out.println("Error parsing person:\n" + inPerson);
				System.exit(1);
			}
			++read;
			//get the index of the household for p, add the person to it;
			for (int i = 0; i < households.size(); ++i) {
				if (((Household) households.get(i)).sampleNumber == p.sampleNumber) {
					((Household) households.get(i)).persons.add(p);
					++allocated;
					break;
				}
			}
		}
		System.out.println("Person records read: " + read);
		System.out.println("Person records allocated: " + allocated);
		personFile.close();
	}

	//parse method takes a string and returns a person object
	Person parse(String inString) {

		StringTokenizer inToken = new StringTokenizer(inString, ",");
		MpoPerson p = new MpoPerson();

		p.recType = new Integer(inToken.nextToken()).intValue();
		p.sampleNumber = new Long(inToken.nextToken()).longValue();
		p.personNumber = new Integer(inToken.nextToken()).intValue();
		p.relationship = new Integer(inToken.nextToken()).intValue();
		if (new Integer(inToken.nextToken()).intValue() == 2)
			p.female = true;
		p.age = new Float(inToken.nextToken()).intValue();
		p.license = new Integer(inToken.nextToken()).intValue();
		p.employmentStatus = new Integer(inToken.nextToken()).intValue();
		//skip address		
		inToken.nextToken();
		p.occupation = new Integer(inToken.nextToken()).intValue();
		p.industry = new Integer(inToken.nextToken()).intValue();
		if (new Integer(inToken.nextToken()).intValue() == 1)
			p.workAtHome = true;
		//skip hours worked at home
		inToken.nextToken();
		p.multipleJobs = new Integer(inToken.nextToken()).intValue();
		//skip number of additional jobs and second job address,occupation,industry
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		p.studentStatus = new Integer(inToken.nextToken()).intValue();
		//skip school attendance status (full vs part time)
		inToken.nextToken();
		p.studentLevel = new Integer(inToken.nextToken()).intValue();
		//skip school address
		inToken.nextToken();
		inToken.nextToken();
		//skip number of trips
		inToken.nextToken();
		p.expWeight = new Float(inToken.nextToken()).floatValue();
		p.survey = new Integer(inToken.nextToken()).intValue();
		p.workOutOfHome = new Integer(inToken.nextToken()).intValue();
		p.work = new Integer(inToken.nextToken()).intValue();
		p.volunteer = new Integer(inToken.nextToken()).intValue();
		//skip number of days of school attendance
		inToken.nextToken();
		return p;
	}

}

class MpoPerson extends Person {

	public int recType;

	public int survey;

	public float expWeight;

	public boolean workAtHome;

	public int multipleJobs;

	public int workOutOfHome;

	public int volunteer;

	public int headHouseholdStudentLevel;

}

// MpoActivities
//Parses a line from MpoActivityFile to Activity Class

class MpoActivities {
	MpoActivities(String fileName, ArrayList households) throws IOException {

		//read activity file
		System.out.println("Reading " + fileName);
		InTextFile activityFile = new InTextFile();
		activityFile.open(fileName);
		long read = 0, totalAllocated = 0;
		String inActivity = new String();
		while ((inActivity = activityFile.readLine()) != null) {
			MpoActivity a = new MpoActivity();
			if (inActivity.length() == 0)
				break;
			try {
				a = parse(inActivity);
			} catch (Exception e) {
				System.out.println("Error parsing activity:\n" + inActivity);
				System.exit(1);
			}
			++read;
			boolean allocated = false;
			//find the activity in the household\person vector
			for (int i = 0; i < households.size(); ++i) {
				if (((Household) households.get(i)).sampleNumber == a.sampleNumber) { //found household
					Household h = (Household) households.get(i);
					for (int j = 0; j < h.persons.size(); ++j) { // now find person
						long persno = ((Person) h.persons.get(j)).personNumber;
						if (persno == a.personNumber) { // found person, now allocate
							((Person) h.persons.get(j)).dayActivities.add(a);
							++totalAllocated;
							allocated = true;
							break;
						}
					}
				}
				if (allocated)
					break;
			}
			if (read == 1 || (read % 10000) == 0)
				System.out.println("Observation number " + read);
		}
		System.out.println("Activity records read: " + read);
		System.out.println("Activity records allocated: " + totalAllocated);
		activityFile.close();
	}

	//	"actdur","arr_hr","arr_min","dep_hr","dep_min","fareunit","hh_mem",
	//  "mode","parkdist","party","per_id","pl_no","pl_type","prk_unit","ptype",
	//  "rectype","trpdur","trp_act1","trp_act2","trp_act3","trp_act4","HH_ID",
	//  "BUSFARE","TAXIFARE","PER_TRP","PRK_COST","EXPWGT","survey","trp_act5",
	//  "access","egress","SWTAZ","MPOCODE","STATEFIPS","nonhh"

	// Note OSMP start/end hour variables are in 1-24 hr format	

	MpoActivity parse(String inString) {
		StringTokenizer inToken = new StringTokenizer(inString, ",");
		MpoActivity a = new MpoActivity();
		a.activityPurposes = new int[5];

		a.duration = new Integer(inToken.nextToken()).intValue();
		a.startHour = new Integer(inToken.nextToken()).intValue();
		a.startMinute = new Integer(inToken.nextToken()).intValue();
		a.endHour = new Integer(inToken.nextToken()).intValue();
		a.endMinute = new Integer(inToken.nextToken()).intValue();
		//skip fareunit,hh_mem
		inToken.nextToken();
		inToken.nextToken();
		a.activityTrip.mode = new Integer(inToken.nextToken()).intValue();
		//skip parkdist
		inToken.nextToken();
		a.activityTrip.partyNumber = 1 + new Integer(inToken.nextToken())
				.intValue();
		if (a.activityTrip.mode >= 11 & a.activityTrip.mode <= 16) {
			a.activityTrip.vehicleOccupants = a.activityTrip.partyNumber;
		}
		a.personNumber = new Integer(inToken.nextToken()).intValue();
		a.activityNumber = new Integer(inToken.nextToken()).intValue();
		a.place = new Integer(inToken.nextToken()).intValue();
		//skip prk_unit
		inToken.nextToken();
		a.placeType = new Integer(inToken.nextToken()).intValue();
		a.recType = new Integer(inToken.nextToken()).intValue();
		a.activityTrip.tripDuration = new Integer(inToken.nextToken())
				.intValue();
		a.activityPurposes[0] = new Integer(inToken.nextToken()).intValue();
		a.activityPurposes[1] = new Integer(inToken.nextToken()).intValue();
		a.activityPurposes[2] = new Integer(inToken.nextToken()).intValue();
		a.activityPurposes[3] = new Integer(inToken.nextToken()).intValue();
		a.sampleNumber = new Long(inToken.nextToken()).longValue();
		//skip busfare,taxifare,per_trp,prk_cost,expwgt
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		inToken.nextToken();
		a.survey = new Integer(inToken.nextToken()).intValue();
		a.activityPurposes[4] = new Integer(inToken.nextToken()).intValue();
		a.activityTrip.accessMode = new Integer(inToken.nextToken()).intValue();
		a.activityTrip.egressMode = new Integer(inToken.nextToken()).intValue();
		a.location.taz = new Long(inToken.nextToken()).longValue();
		//skip mpocode,statefips,nonhh
		//inToken.nextToken();inToken.nextToken();inToken.nextToken();
		return a;
	}
}

class MpoActivity extends Activity {

	public int recType;

	public int place;

	public int placeType;

	public int activityPurposes[];

	public int activity1;

	public int activity2;

	public int activity3;

	public int activity4;

	public int activity5;

	public int survey;

}
