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

import com.pb.common.math.MathUtil;
import com.pb.common.model.Alternative;
import static com.pb.models.pt.TourDestinationParameters.*;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class is used to hold properties related
 * to the TAZs in the model area.  Properties
 * include employment levels, zoneNumber, acres, etc.
 *
 * Author: Christi Willison
 * Date: Oct 11, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */

public abstract class Taz implements Alternative, Comparable, Serializable {
    
    public static Logger logger = Logger.getLogger(Taz.class);
    public boolean trace;
    
    private final int MAX_MILES_FROM_HOME=50;
    
    public int zoneNumber;
    public boolean isCordon = false;
    public float acres;
    protected double lnAcres;
    protected float pricePerAcre;
    protected float pricePerSqFtSFD;
    
    public AreaType areatype; 
    public float terminalTime; 
    
    // share of grade school employment associated with schools, rather than administration
    // ranges from 0 to 1 
    public float gradeSchoolTeachingShare; 
    
    // binary flag indicating that the zone is north of the Columbia River, used in ITD
    public int northOfColumbiaRiver; 
    
    public float households;
    public HashMap<String, Float> employment;
    public float workParkingCost;
    public float nonWorkParkingCost;
    public int dcDistrict;
    public boolean tourSizeTermsSet;
    public boolean stopSizeTermsSet;
    public double[] tourSizeTerm = new double[ActivityPurpose.values().length];
    public double[] stopSizeTerm = new double[ActivityPurpose.values().length];
    public double[] tourLnSizeTerm = new double[ActivityPurpose.values().length];
    public double[] stopLnSizeTerm = new double[ActivityPurpose.values().length];
    
    //needed for implementing Alternative class
    public double utility;
    public double constant;
    public double expConstant;
    public String name;
    public boolean isAvailable;
    
    public Taz() {
    
        // following variables specifically for tour destination choice
        tourSizeTermsSet = false;
        stopSizeTermsSet = false;

        utility = 0;
        isAvailable = true;

        employment = new HashMap<String, Float>();
    }

    //Will be defined by the project specific TAZ.
    public abstract void setTourSizeTerms(float[][] tdpd);

    public abstract void setStopSizeTerms(float[][] params);


    /**
     * @return Returns the acres.
     */
    public float getAcres() {
        return acres;
    }

    
    public void setLnAcres(){
        if(acres != 0.0f)
            lnAcres = MathUtil.log(acres);
        else throw new RuntimeException("Acres is 0 - either it hasn't been set or the zone file" +
                " has an error");
    }

    /**
     * @return Returns the households.
     */
    public float getHouseholds() {
        return households;
    }

    /**
     * Return the total employment.
     *
     * @return total employment
     */
    public float getTotalEmployment() {
        Collection<Float> values = employment.values();
        float sum = 0.0f;
        for(Float value: values){
            sum += value;
        }
        return sum;
    }

    public double getParkingCost(ActivityPurpose actPurpose){
        if(actPurpose==ActivityPurpose.WORK)
            return this.workParkingCost;
        else
            return this.nonWorkParkingCost;
    }

    public void setTourSizeTerms(float[][] tdpd, boolean trace) {
         this.trace = trace;
         setTourSizeTerms(tdpd);
    }



    public void calcTourDestinationUtility(ActivityPurpose purpose,
            float[] tdp, double logsum, double distance,TourDestinationPersonAttributes attributes,
            boolean trace, Taz origin, float calibConstant) {
        this.trace = trace;
        calcTourDestinationUtility(purpose, tdp, logsum, distance, attributes, origin, calibConstant);
    }

    /**
     * Calculate the tour destination utility.
     * @param purpose  The purpose.
     * @param tdp  The parameter array fro the purpose.
     * @param logsum  The mode choice logsum.
     * @param distance Distance from anchor to primary destination.
     * @param attributes TourDestinationPersonAttributes 
     */
    public void calcTourDestinationUtility(ActivityPurpose purpose,
            float[] tdp, double logsum, double distance, 
            TourDestinationPersonAttributes attributes, 
            Taz origin, float calibConstant) {

        utility = -999;
        int intrazonal = 0;
        if(zoneNumber == attributes.originTaz) intrazonal = 1;
        
        // for segmenting by area type
        int rural=0; 
        if (areatype.equals(AreaType.RURAL)) rural=1;
        
        int suburban=0;
        if (areatype.equals(AreaType.SUBURBAN)) suburban=1;
        
        int urban=0;
        if (areatype.equals(AreaType.URBAN)) urban=1;
        
        int cbd=0;         
        if (areatype.equals(AreaType.CBD)) cbd=1;
       
        
        // utility calculations
        if (tourSizeTerm[purpose.ordinal()] > 0 && acres > 0) {
            utility = calibConstant + tdp[LOGSUM] * logsum
                    + tdp[DISTANCE] * Math.min(distance,tdp[MAXDIST])
                    + tdp[DISTANCE2] * Math.pow(Math.min(distance,tdp[MAXDIST]), 2)
                    + tdp[DISTANCE3] * Math.pow(Math.min(distance,tdp[MAXDIST]), 3)
                    + tdp[LOGDISTANCE] * Math.log(Math.min(distance,tdp[MAXDIST]) + 1)
                    + tdp[DISTANCE2TOURS] * distance *attributes.twoTours
                    + tdp[DISTANCE3PTOURS] * distance *attributes.threePlusTours
                    + tdp[DISTANCE1STOP] * distance * attributes.oneStop
                    + tdp[DISTANCE2STOPS] * distance * attributes.twoStops
                    + tdp[DISTPSHOME] * distance * attributes.preSchoolAtHome
                    + tdp[INTRAZONAL] * intrazonal
                    + tourLnSizeTerm[purpose.ordinal()];
            
            // check length in case parameter file doesn't include this
            if (tdp.length>INTRAZONALACRES) {
                utility += tdp[INTRAZONALACRES] * intrazonal * acres; 
            }   
            
            if (tdp.length>INTRAZONALRURAL) {
                utility += tdp[INTRAZONALRURAL]    * intrazonal * rural; 
                utility += tdp[INTRAZONALSUBURBAN] * intrazonal * suburban; 
                utility += tdp[INTRAZONALURBAN]    * intrazonal * urban; 
                utility += tdp[INTRAZONALCBD]      * intrazonal * cbd; 
            }
            
            if (tdp.length>DISTANCERURAL) {
                utility += tdp[DISTANCERURAL]    * distance * rural; 
                utility += tdp[DISTANCESUBURBAN] * distance * suburban; 
                utility += tdp[DISTANCEURBAN]    * distance * urban; 
                utility += tdp[DISTANCECBD]      * distance * cbd; 
            }
            
            // constant associated with crossing columbia river
            if (origin.northOfColumbiaRiver != this.northOfColumbiaRiver) {
            	utility += tdp[COLUMBIARIVERCROSSING]; 
            }
            
            isAvailable = true;

            if (trace) {
                logger.info("taz " +zoneNumber +" = "+ tdp[LOGSUM] + "*" + logsum);
                logger.info("   + "+ tdp[DISTANCE] + " * " + distance);
                logger.info("   + "+ tdp[DISTANCE2] + " * " + Math.pow(distance, 2));
                logger.info("   + "+ tdp[DISTANCE3] + " * " + Math.pow(distance, 3));
                logger.info("   + "+ tdp[LOGDISTANCE] + " * " + Math.log(distance + 1));
                logger.info("   + "+ tdp[DISTANCE2TOURS]+" * "+distance+" * "+attributes.twoTours);
                logger.info("   + "+ tdp[DISTANCE3PTOURS]+" * "+distance +" * "+attributes.threePlusTours);
                logger.info("   + "+ tdp[DISTANCE1STOP]+" * "+distance +" * "+ attributes.oneStop);
                logger.info("   + "+ tdp[DISTANCE2STOPS]+" * "+ distance +" * "+ attributes.twoStops);
                logger.info("   + "+ tdp[DISTPSHOME]+" * "+distance +" * "+ attributes.preSchoolAtHome);
                logger.info("   + "+ tdp[INTRAZONAL]+" * "+intrazonal);
                logger.info("   + "+ tourLnSizeTerm[purpose.ordinal()]);
                // check length in case parameter file doesn't include this
                if (tdp.length>INTRAZONALACRES) {
                    logger.info("   + "+ tdp[INTRAZONALACRES]+" * "+intrazonal+" * "+acres); 
                }            

                if (tdp.length>INTRAZONALRURAL) {
                    logger.info("   + "+ tdp[INTRAZONALRURAL]   +" * "+intrazonal+" * "+rural); 
                    logger.info("   + "+ tdp[INTRAZONALSUBURBAN]+" * "+intrazonal+" * "+suburban); 
                    logger.info("   + "+ tdp[INTRAZONALURBAN]   +" * "+intrazonal+" * "+urban); 
                    logger.info("   + "+ tdp[INTRAZONALCBD]     +" * "+intrazonal+" * "+cbd); 
                }
                
                // constant associated with crossing columbia river
                if (origin.northOfColumbiaRiver != this.northOfColumbiaRiver) {
                	logger.info("   + "+ tdp[COLUMBIARIVERCROSSING]);
                }
            }
        } else {
            isAvailable = false;
        }
    }

   
    /**
     * Calculate the stop destination choice utility
     *
     *
     *
     */
    
    public void calcStopDestinationUtility(ActivityPurpose actPurpose,
            float[] params, Mode mode, int originTaz, int destinationTaz,
            float autoTime, float walkTime, float bikeTime, float transitGeneralizedCost,
            float[] autoDists, int stopNumber, float distanceFromHome) {
        utility = -999;
        isAvailable = false;
        
        if (mode.type == TourModeType.WALK && walkTime > 120
                && zoneNumber != originTaz)
            return;       
        
        // if you are biking and this zone is more than 2 hours away, it is unavailable 
        if (mode.type == TourModeType.BIKE && bikeTime > 120
                && zoneNumber != originTaz)
            return; 
        
        // Changed due to calibration constraints; if you are Work/Work Based and this zone takes you more than
        // 50 miles the distance you would have traveled if you didn't stop, it is unavailable.
        if (((actPurpose!=ActivityPurpose.WORK)|| (actPurpose!=ActivityPurpose.WORK_BASED))
                && autoDists[1] >  autoDists[0] +MAX_MILES_FROM_HOME &&
                zoneNumber != originTaz && zoneNumber != destinationTaz)
            return;
        
        boolean transitLeg = false;
        if (mode.type == TourModeType.WALKTRANSIT)
//                || mode.type == TourModeType.DRIVETRANSIT)
            transitLeg = true;
        // Changes due to PT Calibration constraints
        else if (mode.type == TourModeType.TRANSITPASSENGER && stopNumber == 1)
           transitLeg = true;
        else if (mode.type == TourModeType.PASSENGERTRANSIT && stopNumber == 2)
            transitLeg = true;

        if (transitLeg && transitGeneralizedCost == 0.0 && walkTime > 80
                && zoneNumber != originTaz)
            return;

        int purpose = actPurpose.ordinal();// ActivityPurpose.getActivityPurposeValue(actPurpose);

        float intraOrigin = 0;
        float intraDestination = 0;
        
        if(zoneNumber==originTaz)
            intraOrigin=1;
        if(zoneNumber==destinationTaz)
            intraDestination=1;
        
        //disallow stops in zones with no size term, unless it is an intrazonal at origin or destination end.
        if (stopSizeTerm[purpose] <= 0.0 && intraOrigin==0 && intraDestination==0)
            return;
        
       if (mode.type == TourModeType.WALK) {
            utility = params[StopDestinationParameters.TIMEWALK] * walkTime
            + params[StopDestinationParameters.DISTANCEWALK] * autoDists[1]
            + params[StopDestinationParameters.DISTANCEPOWERWALK]* Math.pow(autoDists[1], 2)
            + params[StopDestinationParameters.ORIGINNONMOTOR]*intraOrigin
            + params[StopDestinationParameters.DESTNONMOTOR]*intraDestination
            + params[StopDestinationParameters.INTRALOGACRES]* lnAcres 
            + stopLnSizeTerm[purpose];
            
            if (trace) {
                logger.info("Stop zone "+zoneNumber+" walk utility " + utility + " = ");
                logger.info("      "+params[StopDestinationParameters.TIMEWALK] * walkTime );
                logger.info("    + " + (params[StopDestinationParameters.DISTANCEWALK] * autoDists[1]));
                logger.info("    + " + params[StopDestinationParameters.DISTANCEPOWERWALK]* Math.pow(autoDists[1], 2));
                logger.info("    + " + params[StopDestinationParameters.ORIGINNONMOTOR]* intraOrigin);
                logger.info("    + " + params[StopDestinationParameters.DESTNONMOTOR]* intraDestination);
                logger.info("    + " + params[StopDestinationParameters.INTRALOGACRES] * lnAcres );
                logger.info("    + " + stopLnSizeTerm[purpose]);
            }
        } else if (mode.type == TourModeType.BIKE) {
            utility = params[StopDestinationParameters.TIMEBIKE] * bikeTime
            + params[StopDestinationParameters.DISTANCEBIKE] * autoDists[1]
            + params[StopDestinationParameters.DISTANCEPOWERBIKE]* Math.pow(autoDists[1], 2)
            + params[StopDestinationParameters.ORIGINNONMOTOR]*intraOrigin
            + params[StopDestinationParameters.DESTNONMOTOR]*intraDestination
            + params[StopDestinationParameters.INTRALOGACRES]* lnAcres 
            + stopLnSizeTerm[purpose];
            
            if (trace) {
                logger.info("Stop zone "+zoneNumber+" bike utility " + utility + " = ");
                logger.info("      "+params[StopDestinationParameters.TIMEBIKE] * bikeTime );
                logger.info("    + " + (params[StopDestinationParameters.DISTANCEBIKE] * autoDists[1]));
                logger.info("    + " + params[StopDestinationParameters.DISTANCEPOWERBIKE]* Math.pow(autoDists[1], 2));
                logger.info("    + " + params[StopDestinationParameters.ORIGINNONMOTOR]* intraOrigin);
                logger.info("    + " + params[StopDestinationParameters.DESTNONMOTOR]* intraDestination);
                logger.info("    + " + params[StopDestinationParameters.INTRALOGACRES] * lnAcres );
                logger.info("    + " + stopLnSizeTerm[purpose]);
            }
        } else if (transitLeg) {
            utility = params[StopDestinationParameters.TIMETRANSIT] * transitGeneralizedCost
                + params[StopDestinationParameters.DISTANCETRANSIT]* autoDists[1]
                + params[StopDestinationParameters.DISTANCEPOWERTRANSIT]* Math.pow(autoDists[1], 2)
                + params[StopDestinationParameters.ORIGINTRANSIT]*intraOrigin
                + params[StopDestinationParameters.DESTTRANSIT]*intraDestination
                + params[StopDestinationParameters.INTRALOGACRES] * lnAcres
                + stopLnSizeTerm[purpose];
            if (trace) {
                logger.info("Stop zone "+zoneNumber+" transit utility " + utility + " = ");
                logger.info("      "+params[StopDestinationParameters.TIMETRANSIT] * transitGeneralizedCost );
                logger.info("    + " + (params[StopDestinationParameters.DISTANCETRANSIT] * autoDists[1]));
                logger.info("    + " + params[StopDestinationParameters.DISTANCEPOWERTRANSIT]* Math.pow(autoDists[1], 2));
                logger.info("    + " + params[StopDestinationParameters.ORIGINTRANSIT]* intraOrigin);
                logger.info("    + " + params[StopDestinationParameters.DESTTRANSIT]* intraDestination);
                logger.info("    + " + params[StopDestinationParameters.INTRALOGACRES] * lnAcres );
                logger.info("    + " + stopLnSizeTerm[purpose]);
            }
        } else {
            utility = params[StopDestinationParameters.TIMEAUTO] * autoTime
            + params[StopDestinationParameters.DISTANCEAUTO] * autoDists[1]
            + params[StopDestinationParameters.DISTANCEPOWERAUTO] * Math.pow(autoDists[1], 2)
            + params[StopDestinationParameters.ORIGINAUTO]*intraOrigin
            + params[StopDestinationParameters.DESTAUTO]*intraDestination
            + params[StopDestinationParameters.INTRALOGACRES] * lnAcres
            + stopLnSizeTerm[purpose];
            if (trace) {
                logger.info("Stop zone "+zoneNumber+" transit utility " + utility + " = ");
                logger.info("      "+params[StopDestinationParameters.TIMEAUTO] * autoTime );
                logger.info("    + " + (params[StopDestinationParameters.DISTANCEAUTO] * autoDists[1]));
                logger.info("    + " + params[StopDestinationParameters.DISTANCEPOWERAUTO]* Math.pow(autoDists[1], 2));
                logger.info("    + " + params[StopDestinationParameters.ORIGINAUTO]* intraOrigin);
                logger.info("    + " + params[StopDestinationParameters.DESTAUTO]* intraDestination);
                logger.info("    + " + params[StopDestinationParameters.INTRALOGACRES] * lnAcres );
                logger.info("    + " + stopLnSizeTerm[purpose]);
            }
        }
       
        // include the distance from home if the coefficient is present
        if (params.length > StopDestinationParameters.DISTANCEFROMHOME) {
            utility += params[StopDestinationParameters.DISTANCEFROMHOME] * distanceFromHome; 
            if (trace) {
                logger.info("    + " + params[StopDestinationParameters.DISTANCEFROMHOME] * distanceFromHome);
            }
        }
       
        isAvailable = true;
      }

    public void setTrace(boolean trace) {
        this.trace = trace;
        
    }

    /**
    Set the availability of this alternative.
    @param available True if alternative is available
    */
    public void setAvailability(boolean available){
        isAvailable=available;
    }

    /**
    Get the availability of this alternative.
    @return True if alternative is available
    */
    public boolean isAvailable(){
        return isAvailable;
    }

    /**
    Get the name of this alternative.
    @return The name of the alternative
    */
    public String getName(){
        return name;
    }

    public int getZoneNumber(){
        return zoneNumber;
    }
    /**
    Set the name of this alternative.
    @param name The name of the alternative
    */
    public void setName(String name){
        this.name=name;
    }

    public void setExpConstant(double expConstant){
        this.expConstant =expConstant;
    }
    public double getExpConstant(){
         return expConstant;
    }

    public void setConstant(double constant){
        this.constant = constant;
    }

    public double getConstant(){
        return constant;
    }

    /**
    Set the utility of the alternative.
    @param util  Utility value.
    */
    public void setUtility(double util){
        utility=util;
    }

    public double getUtility(){
          return utility;
    }

    public int compareTo(Object other) {
        Taz otherTaz = (Taz) other;
        if (zoneNumber < otherTaz.getZoneNumber()) {
            return -1;
        } else if (zoneNumber > otherTaz.getZoneNumber()) {
            return 1;
        }

        return 0;
    }

    //This method will be called from the household workers to check and see if the
    //TazOLD has been updated and what the size terms and the acres have been set to.
    public void summarizeTAZInfo(){
            if(tourSizeTermsSet){
                if(stopSizeTermsSet){
                    logger.info("");
                    logger.info("TAZ Data has been set, here is a summary: ");
                    logger.info("TAZ #: " + zoneNumber);
                    logger.info("TAZ acres: " + acres);
                    logger.info("TAZ tour size terms");
                    ActivityPurpose[] purposes = ActivityPurpose.values();
                    for(int i=0;i<purposes.length;i++){
                        String output = purposes[i] + ": \t"+ tourSizeTerm[i] + "\t";
                        logger.info("\t" + output);
                    }
                }else logger.warn("Stop Size Terms have not been set - this problem must be corrected before a summary can be " +
                            "produced");
            }else logger.warn("Tour Size Terms have not been set - this problem must be corrected before a summary can be " +
                                "produced");
    }

    /**
     *print():For printing zone attributes to the screen
     *
     *
     **/
    public void print(){

        logger.info(
            "\nzoneNumber: "+ zoneNumber
            +"\nhouseholds: "+ households
            +"\nworkParkingCost: "+ workParkingCost
            +"\nnonWorkParkingCost: "+ nonWorkParkingCost
            +"\nacres: "+ acres
            +"\npricePerAcre: "+ pricePerAcre
            +"\npricePerSqFtSFD: "+ pricePerSqFtSFD
            +"\ntoursizeTerms: ");
            for(int i=0;i<tourSizeTerm.length;++i)
               logger.info("\n ["+i+"]: "+tourSizeTerm[i]+" ln: "+tourLnSizeTerm[i]);

            logger.info("\nstopsizeTerms: ");
            for(int i=0;i<stopSizeTerm.length;++i)
                logger.info("\n "+i+" : "+stopSizeTerm[i]+" ln : "+stopLnSizeTerm[i]);
     }
}
