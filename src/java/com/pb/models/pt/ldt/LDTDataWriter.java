/*
 * Copyright  2006 PB Consult Inc.
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
package com.pb.models.pt.ldt;

import com.pb.common.util.ResourceUtil;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ResourceBundle;

/**
 * Class to write results of long-distance travel models.
 * 
 * @author Erhardt
 * @version 1.0 May 8, 2006
 *
 */
public class LDTDataWriter {
    protected final static Logger logger = Logger.getLogger(LDTDataWriter.class);
    private ResourceBundle ldRb;
    private ResourceBundle globalRb;
    
    private PrintWriter tourWriter = null;
    private PrintWriter tripWriter = null;
    private PrintWriter assignmentTripWriter = null;

    
    /**
     * Default constructor.  
     *
     */
    public LDTDataWriter(ResourceBundle rb, ResourceBundle globalRb){
         this.ldRb = rb;
         this.globalRb = globalRb;
    }
    
    /**
     * Creates a print writer for writing output in this class.  
     * 
     * @param property Name of the property in the resource bundle.
     * @return         A print writer for writing to that file.  
     */
    private PrintWriter createWriter(ResourceBundle rb, String property) {
        String fileName  = ResourceUtil.getProperty(rb, property);
        File file = new File(fileName); 
        logger.info("Writing to file " + file);
        
        PrintWriter writer; 
        try {
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            writer = new PrintWriter(bw);
        } catch (IOException e) {
            logger.fatal("Could not create file" + file);
            throw new RuntimeException(e);
        }
        return writer; 
    }
    
    /**
     * Writes the tour objects to disk.
     * Writes them as person-tours for all modes, such that a
     * household tour stored as one object in memory will be written
     * once for each household member.
     * 
     * @param tours An array of long-distance tours.
     */
    public void writePersonTours(LDTour[] tours) {
        
        if (tours == null) {
            return;
        }

        if (tourWriter == null) {
            tourWriter = createWriter(ldRb, "ldt.tours");

            // write the header
            tourWriter.print("hhID,");
            tourWriter.print("memberID,");
            tourWriter.print("tourID,");
            tourWriter.print("income,");
            tourWriter.print("tourPurpose,");
            tourWriter.print("tourMode,");
            tourWriter.print("patternType,");
            tourWriter.print("destinationType,");
            tourWriter.print("home,");
            tourWriter.print("destination,");
            tourWriter.print("distance,");
            tourWriter.print("outboundTravelTime,");
            tourWriter.print("inboundTravelTime,");
            tourWriter.print("departureTime,");
            tourWriter.print("arrivalTime,");
            tourWriter.print("durationTime,");
            tourWriter.print("partySize,");
            tourWriter.print("tripMode");
            tourWriter.print("\n");
        }

        // write the data
        for (LDTour t : tours) {
            for (int p=0; p<t.partySize; p++) {
                writeTour(t, tourWriter);
            }
        }

        tourWriter.flush();
    }

    /**
     * Writes the trip objects to disk.
     * Writes them as person-trips for all modes, such that a
     * household tour stored as one object in memory will be written
     * once for each household member.
     * 
     * @param trips An array of long-distance trips.
     */
    public void writePersonTrips(LDTrip[] trips) {

        if (trips == null) {
            return;
        }

        if (tripWriter == null) {
            tripWriter = createWriter(ldRb, "ldt.person.trips");
            writeTripHeader(tripWriter);
        }

        // write the data
        for (LDTrip t : trips) {
            for (int p=0; p<t.partySize; p++) {
                writeTrip(t, tripWriter);
            }
        }

        tripWriter.flush();
    }

    /**
     * Writes trip lists to disk for use by assignment. Filters out
     * external transit trips b/c they will be unconnected in transit
     * assignment.  Only writes those highway trips that are vehicle trips.
     * 
     * Also writes the auto portion of air trips, assigning them to the
     * nearest airport.  Thus the air trips will be repeated both as an 
     * air trip from O->D and a DA trip from O->Airport or Airport->D.  
     *
     * @param trips An array of long-distance trips.
     */
    public void writeAssignmentTrips(LDTrip[] trips) {
        if (trips == null) {
            return;
        }
        
        if (assignmentTripWriter == null) {
            assignmentTripWriter = createWriter(globalRb, "ldt.vehicle.trips");
            writeTripHeader(assignmentTripWriter);
        }

        // write the data
        for (LDTrip t : trips) {
            for (int p=0; p<t.partySize; p++) {
                switch (t.mode) {
                case AUTO: {
                    writeTrip(t, assignmentTripWriter);
                    break;
                }
                case AIR: {
                    if (t.destinationType.equals(LDTourDestinationType.INTERNAL))
                        writeTrip(t, assignmentTripWriter);
                    writeDriveToAirportTrip(t, assignmentTripWriter);
                    break;
                }
                default: {
                    if (t.destinationType.equals(LDTourDestinationType.INTERNAL))
                        writeTrip(t, assignmentTripWriter);
                }
                }
            }
        }

        assignmentTripWriter.flush();
    }

    public void writeAssignmentTripsTest(LDTrip[] trips) {


        // write the data
        for (LDTrip t : trips) {
            for (int p=0; p<t.partySize; p++) {
                switch (t.mode) {
                case AUTO: {
                    System.out.println("AUTO CASE EVALUATED TRUE");
                }
                case AIR: {
                    System.out.println("AIR CASE EVALUATED TRUE");
                    
                }
                default: {
                    System.out.println("DEFAULT EVALUATED TRUE");
                }
                }
            }
        }


    }



    private void writeTripHeader(PrintWriter writer) {

        writer.print("hhID,");
        writer.print("memberID,");
        writer.print("tourID,");
        writer.print("income,");
        writer.print("tourPurpose,");
        writer.print("tourMode,");
        writer.print("origin,");
        writer.print("destination,");
        writer.print("distance,");
        writer.print("time,");
        writer.print("tripStartTime,");           // arrival time for returning tours
        writer.print("tripPurpose,");
        writer.print("tripMode,");
        writer.print("vehicleTrip");
        writer.print("\n");
    }

    private void writeTour(LDTour t, PrintWriter writer) {
        int i = 0;
        try {
            writer.print(t.hh.ID + ",");
            writer.print(t.person.memberID + ",");
            writer.print(t.ID + ",");
            writer.print(t.hh.income + ",");
            writer.print(t.purpose + ",");
            writer.print(t.mode + ",");
            writer.print(t.patternType + ",");
            writer.print(t.destinationType + ",");
            writer.print(t.homeTAZ + ",");
            writer.print(t.destinationTAZ + ",");
            writer.print(t.distance + ",");
            writer.print(t.outboundTime + ",");
            writer.print(t.inboundTime + ",");
            writer.print(t.schedule.getDepartureMilitaryTime() + ",");
            writer.print(t.schedule.getArrivalMilitaryTime() + ",");
            writer.print(t.schedule.getDurationMinutes() + ",");
            writer.print(t.partySize + ",");
            writer.print(t.tripMode);
            writer.print("\n");
        } catch (NullPointerException e) {
            for (; i < 16; ++i) {
                writer.print(",-9");
            }
            writer.print("\n");
        }
    }

    private void writeDriveToAirportTrip(LDTrip t, PrintWriter writer) {
        writer.print(t.hhID + ",");
        writer.print(t.personID + ",");
        writer.print(t.tourID + ",");      
        writer.print(t.tour.hh.income + ",");
        writer.print(t.purpose + ",");
        writer.print(t.mode + ",");

        // assign the drive portion of air trips, if necessary.
        if (t.outboundTrip) {
            writer.print(t.origin + ",");
            writer.print(t.nearestAirport + ",");     
        } else {
            writer.print(t.nearestAirport + ",");      
            writer.print(t.destination + ",");             
        }

        writer.print(t.distance + ",");
        writer.print(t.time + ",");
        writer.print(t.tripTimeOfDay + ",");
        writer.print(t.purpose + ",");
        
        // assume DA for drive to airport.
        writer.print(LDTripModeType.DA + ",");      
        
        writer.print(t.vehicleTrip);
        writer.print("\n");
    }
    
    
    private void writeTrip(LDTrip t, PrintWriter writer) {
        writer.print(t.hhID + ",");
        writer.print(t.personID + ",");
        writer.print(t.tourID + ",");      
        writer.print(t.tour.hh.income + ",");
        writer.print(t.purpose + ",");
        writer.print(t.mode + ",");
        writer.print(t.origin + ",");
        writer.print(t.destination + ",");
        writer.print(t.distance + ",");
        writer.print(t.time + ",");
        writer.print(t.tripTimeOfDay + ",");             // arrival time for returning tours
        writer.print(t.purpose + ",");
        writer.print(t.tripMode + ",");
        writer.print(t.vehicleTrip);
        writer.print("\n");
    }

    /**
     * Close static PrintWriters.
     *
     */
    public void close() {
        if (tourWriter!=null) {
            tourWriter.close();
        }
        if (tripWriter!=null) {
            tripWriter.close();            
        }
        if (assignmentTripWriter!=null) {
            assignmentTripWriter.close();            
        }
    }

    public static void main(String[] args) {
        LDTrip singleTrip = new LDTrip();
//        singleTrip.mode = LDTourModeType.AUTO;
        singleTrip.mode = LDTourModeType.AIR;
//        singleTrip.mode = LDTourModeType.TRANSIT_WALK;
        singleTrip.partySize = 1;

        LDTrip[] tripArray = {singleTrip};

        LDTDataWriter dataWriter = new LDTDataWriter(null, null);
        dataWriter.writeAssignmentTripsTest(tripArray);


    }

    
}
