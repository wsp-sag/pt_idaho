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
package com.pb.models.pt.tourmodes;

import com.pb.common.model.ModelException;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.Mode;
import static com.pb.models.pt.TourModeParameters.*;
import com.pb.models.pt.TourModePersonAttributes;
import com.pb.models.pt.TourModeType;
import com.pb.models.pt.util.TravelTimeAndCost;
import org.apache.log4j.Logger;

/**  
 * Drive Transit mode
 * 
 * @author Joel Freedman
 * @version 1.0 12/01/2003
 * 
 */
public class DriveTransit extends Mode {
//    final static Logger logger = Logger.getLogger(DriveTransit.class);
//
//    final static long serialVersionUID = 666;
//
////     public boolean isAvailable=true;
////     public boolean hasUtility=false;
////     double utility=0;
//
//
//     public DriveTransit(){
//          isAvailable = true;
//          hasUtility = false;
//          utility = 0.0D;
//          alternativeName=new String("DriveTransit");
//          type=TourModeType.DRIVETRANSIT;
//     }
//
//     /** Calculate Drive Transit utility
//      *
//      * @param inbound Inbound TravelTimeAndCost
//      * @param outbound Outbound TravelTimeAndCost
//      * @param c TourModeParameters
//      * @param p PersonTourModeAttributes
//      */
//     public void calcUtility(TravelTimeAndCost inbound, TravelTimeAndCost outbound,
//           float[] c, TourModePersonAttributes p){
//
//         hasUtility = false;
//         utility=-999;
//         isAvailable = true;
//
//         //check transit availability
//          if(inbound.driveTransitInVehicleTime==0) isAvailable=false;
//          if(outbound.driveTransitInVehicleTime==0) isAvailable=false;
//
//          //only available for work tours anc college tours
//          if(p.tourPurpose!=ActivityPurpose.WORK && p.tourPurpose!=ActivityPurpose.COLLEGE)
//              isAvailable=false;
//
//          if(isAvailable){
//               time=
//                    (inbound.driveTransitInVehicleTime+outbound.driveTransitInVehicleTime
//                 + inbound.driveTransitDriveTime+outbound.driveTransitDriveTime
//                 + inbound.driveTransitFirstWaitTime
//                 + outbound.driveTransitFirstWaitTime
//                  + inbound.driveTransitTransferWaitTime
//                 + outbound.driveTransitTransferWaitTime);
//
//               utility=(
//              c[IVT]*(inbound.driveTransitInVehicleTime+outbound.driveTransitInVehicleTime)
//            + c[DRV]*(inbound.driveTransitDriveTime+outbound.driveTransitDriveTime)
//            + c[FWT]*(inbound.driveTransitFirstWaitTime
//                 + outbound.driveTransitFirstWaitTime)
//            + c[XWT]*(inbound.driveTransitTransferWaitTime
//                 + outbound.driveTransitTransferWaitTime)
//            + c[WLK]*(inbound.driveTransitWalkTime+outbound.driveTransitWalkTime)
//            + c[COST_LOW]*((inbound.driveTransitFare+outbound.driveTransitFare)*p.inclow)
//            + c[COST_MED]*((inbound.driveTransitFare+outbound.driveTransitFare)*p.incmed)
//            + c[COST_HI]*((inbound.driveTransitFare+outbound.driveTransitFare)*p.inchi)
//            + c[COST_LOW]*((inbound.driveTransitDriveCost+outbound.driveTransitDriveCost)*p.inclow)
//            + c[COST_MED]*((inbound.driveTransitDriveCost+outbound.driveTransitDriveCost)*p.incmed)
//            + c[COST_HI]*((inbound.driveTransitDriveCost+outbound.driveTransitDriveCost)*p.inchi)
//            + c[DRV_TRAN]
//            + c[DRV_TRAN_AW_0]*p.auwk0
//            + c[DRV_TRAN_AW_I]*p.auwk1
//            + c[DRV_TRAN_AW_S]*p.auwk2
//            + c[DRV_TRAN_STOPS]*p.totalStops
//                  );
//
//              if(utility == Double.NEGATIVE_INFINITY || utility == Double.POSITIVE_INFINITY){
//                  utility = -999;
//                  isAvailable = false;
//              }
//
//               if (trace) {
//                    logger.info("Drive transit utility: " + utility + " = "
//                            + c[IVT] + "(" + inbound.driveTransitInVehicleTime + "+" + outbound.driveTransitInVehicleTime + ")");
//                    logger.info("\t+" + c[DRV] + "* ("
//                        + inbound.driveTransitDriveTime + "+"
//                        + outbound.driveTransitDriveTime + ")");
//                    logger.info("\t+" + c[FWT] + "* ("
//                            + inbound.driveTransitFirstWaitTime + "+"
//                            + outbound.driveTransitFirstWaitTime + ")");
//                    logger.info("\t+" + c[XWT] + "* ("
//                            + inbound.driveTransitTransferWaitTime + "+"
//                            + outbound.driveTransitTransferWaitTime + ")");
//                    logger.info("\t+" + c[WLK] + "* ("
//                            + inbound.driveTransitWalkTime + "+"
//                            + outbound.driveTransitWalkTime + ")");
//                    logger.info("\t+" + c[COST_LOW] + "* ("
//                            + inbound.driveTransitFare + "+"
//                            + outbound.driveTransitFare + ") * " + p.inclow);
//                    logger.info("\t+" + c[COST_MED] + "* ("
//                            + inbound.driveTransitFare + "+"
//                            + outbound.driveTransitFare + ") * " + p.incmed);
//                    logger.info("\t+" + c[COST_HI] + "* ("
//                            + inbound.driveTransitFare + "+"
//                            + outbound.driveTransitFare + ") * " + p.inchi);
//                    logger.info("\t+" + c[COST_LOW] + "* ("
//                            + inbound.driveTransitDriveCost + "+"
//                            + outbound.driveTransitDriveCost + ") * " + p.inclow);
//                    logger.info("\t+" + c[COST_MED] + "* ("
//                            + inbound.driveTransitDriveCost + "+"
//                            + outbound.driveTransitDriveCost + ") * " + p.incmed);
//                    logger.info("\t+" + c[COST_HI] + "* ("
//                            + inbound.driveTransitDriveCost + "+"
//                            + outbound.driveTransitDriveCost + ") * " + p.inchi);
//                    logger.info("\t+" + c[DRV_TRAN]);
//                    logger.info("\t+" + c[DRV_TRAN_AW_I] +" * " + p.auwk1);
//                    logger.info("\t+" + c[DRV_TRAN_AW_S] +" * " + p.auwk2);
//                    logger.info("\t+" + c[DRV_TRAN_STOPS] + " * "+p.totalStops);
//
//
//               }
//               hasUtility=true;
//          } else if (trace) {
//              logger.info("Drive transit is not available.");
//          }
//     }
//
//    /**
//     *  Get drive transit utility
//     */
//     @Override
//    public double getUtility() {
//        if (!hasUtility) {
//            String msg = "Error: Utility not calculated for " + alternativeName;
//            logger.fatal(msg);
//            //TODO - log this error to the node exception file
//            throw new ModelException(msg);
//        }
//        return utility;
//    }
}