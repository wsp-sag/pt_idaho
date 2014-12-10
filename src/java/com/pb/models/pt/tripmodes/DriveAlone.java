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
package com.pb.models.pt.tripmodes;

import com.pb.models.pt.Mode;
import com.pb.models.pt.TripModeType;
import com.pb.models.pt.Activity;
import com.pb.models.pt.TourModeType;
import com.pb.models.pt.TripModePersonAttributes;
import com.pb.models.pt.TripModeParameters;
import com.pb.models.pt.ZoneAttributes;
import com.pb.models.pt.util.TravelTimeAndCost;

import org.apache.log4j.Logger;
/**
 * Driver alone mode
 *
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 *
 */
public class DriveAlone extends Mode {
    final static Logger logger = Logger.getLogger("com.pb.idaho.pt.default");

//     public boolean isAvailable=true;
//     public boolean hasUtility=false;

    //    double utility=0;

    public DriveAlone(){
        isAvailable = true;
        hasUtility = false;
        utility = 0.0D;
        alternativeName= "DriveAlone";
        type=TripModeType.DA;
    }

    /** Calculates utility of driving alone
     *
     * @param zOrigin - ZoneAttributes at Origin
     * @param zDestination - ZoneAttributes at Destination
     * @param p - PersonTourModeAttributes
     * @param tc - Travel time and Cost
     * @param a - person attributes
     * @param tourMode - tourMode
     * @param thisActivity - activity
     */
    public void calcUtility(TravelTimeAndCost tc, ZoneAttributes zOrigin, ZoneAttributes zDestination ,float[] p,
                            TripModePersonAttributes a,Mode tourMode, Activity thisActivity){
        //TODO: assumed that there are no parking costs at Origin. Is this correct?
        //TODO: is it correct to take the same value (walk) and multiply it by the terminal Time at both ends?
        hasUtility = false;
        utility=-999;
        isAvailable = true;

        if(a.age<16) isAvailable=false;
        if(a.autos==0) isAvailable=false;
        if(tourMode.type!=TourModeType.AUTODRIVER) isAvailable=false;

        if(isAvailable){
            time=(tc.driveAloneTime);
            //if duration is zero, round it to 1 for parking costs
            short duration = thisActivity.duration == 0 ? 60 : thisActivity.duration;
            utility=(
                    p[TripModeParameters.IVT]*(tc.driveAloneTime )
                            + p[TripModeParameters.OPCLOW]*(tc.driveAloneCost*a.inclow)
                            + p[TripModeParameters.OPCMED]*(tc.driveAloneCost*a.incmed)
                            + p[TripModeParameters.OPCHI]* (tc.driveAloneCost*a.inchi)
                            + p[TripModeParameters.PKGLOW]*((zDestination.parkingCost*(duration/60))*a.inclow)
                            + p[TripModeParameters.PKGMED]*((zDestination.parkingCost*(duration/60))*a.incmed)
                            + p[TripModeParameters.PKGHI]*((zDestination.parkingCost*(duration/60))*a.inchi)
                            + p[TripModeParameters.WLK]*(zOrigin.terminalTime)
                            + p[TripModeParameters.WLK]*(zDestination.terminalTime)
            );

            if (trace) {
                logger.info("drive-alone activity duration: " + duration + "/60"); 
                logger.info("drive-alone parking cost: " + zDestination.parkingCost);
                logger.info("drive-alone utility: " + utility);
                logger.info("\t" + p[TripModeParameters.IVT] + " * "+ tc.driveAloneTime);
                logger.info("\t" + p[TripModeParameters.OPCLOW] + " * " + (tc.driveAloneCost*a.inclow));
                logger.info("\t" + p[TripModeParameters.OPCMED] + " * " + (tc.driveAloneCost*a.incmed));
                logger.info("\t" + p[TripModeParameters.OPCHI] + " * " + (tc.driveAloneCost*a.inchi));
                logger.info("\t" + p[TripModeParameters.PKGLOW] + " * " + ((zDestination.parkingCost*(duration/60))*a.inclow));
                logger.info("\t" + p[TripModeParameters.PKGMED] + " * " + ((zDestination.parkingCost*(duration/60))*a.incmed));
                logger.info("\t" + p[TripModeParameters.PKGHI] + " * " + ((zDestination.parkingCost*(duration/60))*a.inchi) );
                logger.info("\t" + p[TripModeParameters.WLK] + " * " + (zOrigin.terminalTime));
                logger.info("\t" + p[TripModeParameters.WLK] + " * " + (zDestination.terminalTime));
            }
            hasUtility=true;
        }
    }
    /** Get drive alone utility */
    public double getUtility(){
        if(!hasUtility){
            logger.fatal("Error: Utility not calculated for "+alternativeName+"\n");
            //TODO - log this error to the node exception file
            System.exit(1);
        }
        return utility;
    }


}

