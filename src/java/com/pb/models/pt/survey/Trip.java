// Trip.java
//
// A class library for travel survey data
// jf 7/00

package com.pb.models.pt.survey;
import java.io.*;

public class Trip implements Cloneable{

	public int tripDuration;
	public int mode;
	public int vehicleOccupants;
	public int partyNumber;
	public int accessMode;
	public int egressMode;
	public int startTime;
	public int startAMPM;
	public int endTime;
	public int endAMPM;
	public boolean changeModes;
	public int modeChanged;


	//to print to screen
	public void print(){
		System.out.print(tripDuration+","+mode+","+","+partyNumber);
	}
	public void print(PrintWriter pw){
		pw.print(tripDuration+","+mode+","+","+partyNumber);
	}

	/*following method returns mode code as follows:
	  0 = Unknown
	  1 = Drive-Alone
	  2 = Driver - 2 Person
	  3 = Driver - 3+Person
	  4 = Passgr - 2 Person
	  5 = Passgr - 3+Person
	  6 = Walk
	  7 = Bike
	  8 = School Bus
	  9 = Walk - Bus - Walk
	  10= Walk - Bus - PNR
	  11= Walk - Bus - KNR
	  12= PNR  - Bus - Walk
	  13= KNR  - Bus - Walk
      21= Taxi/Limo
      22= Motorcycle
      23= Commercial vehicle driver
      24= Commercial vehicle passgr
      25= Train
      26= Airplane
      27= Other
      
	*	*/
	public int getDetailedMode(){
		int dmode=0;
		
		if(mode==20){			//walk
			dmode=6;
		}else if(mode==21){	    //bike
			dmode=7;
		}else if(mode==18){	    //school bus
		 	dmode=8;
		}else if(mode==19){     //taxi/limo
			dmode=21;
		}else if(mode==22){     //motorcycle
			dmode=22;
		}else if(mode==23){     //commercial veh driver
			dmode=23;
		}else if(mode==24){     //commercial veh passgr
			dmode=24;
		}else if(mode==25){     //train
			dmode=25;
		}else if(mode==26){     //airplane
			dmode=26;
		}else if(mode==17){	    //public bus
			if(accessMode==0){
				dmode=9;
			}else if(accessMode==1 && egressMode==1){
				dmode=9;
			}else if(accessMode==1 && egressMode==2){
				dmode=10;
			}else if(accessMode==1 && egressMode==3){
				dmode=11;
			}else if(accessMode==2 && egressMode==1){
				dmode=12;
			}else if(accessMode==3 && egressMode==1){
				dmode=13;
			}else{
				dmode=9;}
		}
		else if(mode>=11 && mode<=16){ //Personal or other vehicle
			if(mode==11||mode==13||mode==15){   //driver
				if(vehicleOccupants==1) {
					dmode=1;
			     }else if(vehicleOccupants==2){
			     	dmode=2;
			     }else if(vehicleOccupants>=3){
			     	dmode=3;
			     }else{
			      	dmode=1;}
			     }
			}else if(mode==12||mode==14||mode==16){  //passgr
				if(vehicleOccupants==1){
					dmode=1;
				}if(vehicleOccupants==2){
		     	    dmode=2;
		        }else if(vehicleOccupants>=3){
		     	    dmode=3;
		     	}else {
		     		dmode=1;}
			}
		return dmode;
	}
		public Object clone() {
		Object o = null;
		try {
			o= super.clone();
		} catch(CloneNotSupportedException e) {
			System.err.println("Activity object cannot clone");
		}
		return o;
	}	

	public static void main(String[] args) {}
}
	
