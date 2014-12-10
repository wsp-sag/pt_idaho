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
package com.pb.models.pt;

import com.pb.common.matrix.*;
import com.pb.common.util.ResourceUtil;
import static com.pb.models.pt.ActivityPurpose.getActivityString;
import com.pb.models.pt.util.SkimsInMemory;
import com.pb.models.pt.util.TravelTimeAndCost;
import com.pb.models.utils.Tracer;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ResourceBundle;


/**
 * This class manages tour mode choice logsums.  It 
 * can create logsums, read them, and write them.  It also
 * stores logsum matrices in memory for one market segment and
 * all tour purposes.
 * 
 * The logsums are currently created for 9 market segments.
 * 
 * @author Joel Freedman
 * @version 2.0 1/2006
 * 
 */
public class TourModeChoiceLogsumManager {

    private static transient Logger logger = Logger
            .getLogger(TourModeChoiceLogsumManager.class);

    private transient Tracer tracer = Tracer.getTracer();

    protected static final Object lock = new Object();
    
    AlphaToBeta tazToAmz;

    //  arrays with segments
    public static final int TOTALSEGMENTS=9;
    static final int[] auwk0segs={1,0,0,1,0,0,1,0,0};
    static final int[] auwk1segs={0,1,0,0,1,0,0,1,0};
    static final int[] auwk2segs={0,0,1,0,0,1,0,0,1};
    static final int[] inclowsegs={1,1,1,0,0,0,0,0,0};
    static final int[] incmedsegs={0,0,0,1,1,1,0,0,0};
    static final int[] inchisegs={0,0,0,0,0,0,1,1,1};
    
    private ZoneAttributes thisZone = new ZoneAttributes();
    TourModePersonAttributes ptma = new TourModePersonAttributes();
    
    TravelTimeAndCost departCost;
    TravelTimeAndCost returnCost;
    //TourModePersonAttributes thisPerson;
    
    //hold some number of logsum matrices in memory
    public Matrix[] logsums;

    int currentSegment = -99;

    float nonWorkParkingCostFactor;
 
    protected ResourceBundle rb;
    
    public TourModeChoiceLogsumManager(ResourceBundle globalRb,
            ResourceBundle ptRb) {
        
        this.rb = ptRb;
        logsums = new Matrix[ActivityPurpose.values().length];
        
        departCost = new TravelTimeAndCost();
        returnCost = new TravelTimeAndCost();
        
        /* [AK]
        String fileName = ResourceUtil.getProperty(globalRb, "alpha2beta.file");
        String alphaName = globalRb.getString("alpha.name");
        String betaName = globalRb.getString("beta.name");
        logger.info("Reading " + fileName);
        tazToAmz = new AlphaToBeta(new File(fileName), alphaName, betaName);
        */
        
        nonWorkParkingCostFactor = Float.parseFloat(rb.getString("sdt.non.work.parking.cost.factor"));

        tracer.readTraceSettings(rb);
        
    }

    /**
     * createLogsumMatrix
     * Calculates Mode choice logsums and returns a logsum matrix
     * @param taz taz
     * @param thisPurpose purpose
     * @param segment segment
     * @param skims SkimsInMemory object
     * @return Matrix Logsum matrix
     */
    public Matrix createLogsumMatrix(ActivityPurpose thisPurpose, int segment,
            TazManager taz, SkimsInMemory skims) {
        
        long time = System.currentTimeMillis();

        logger.info("Creating ModeChoiceLogsum Matrix for - Purpose: "
                + thisPurpose + "  Segment: " + segment);

        String mName = getName(thisPurpose, segment);

        //logger.info("**** RoWColNumbers : " + (skims.pkTime.getInternalNumbers().length - 1) + ", " + (skims.pkTime.getInternalNumbers().length - 1) + "  ****");
        
        Matrix m = new Matrix(mName, "mcLogsumMatrix", skims.pkTime.getInternalNumbers().length - 1, skims.pkTime.getInternalNumbers().length - 1);
        
        //Matrix m = new Matrix(mName, "mcLogsumMatrix", taz.size(), taz.size());				//[AK]
        //m.setExternalNumbers(taz.getExternalNumberArrayOneIndexed());							//[AK]
        
    	m.setName(mName);
        
        TourModeChoiceModel mcModel = new TourModeChoiceModel(rb);

        for (Taz originTaz : taz.values()) {
            int itaz = originTaz.zoneNumber;

            for (Taz destinationTaz : taz.values()) {
                int jtaz = destinationTaz.zoneNumber;
                boolean trace = tracer.isTraceZonePair(itaz, jtaz);
                
                mcModel.setTrace(trace);

                // set taz attributes (only parking cost at this point)
                if (thisPurpose == ActivityPurpose.WORK
                        || thisPurpose == ActivityPurpose.WORK_BASED) {
                    thisZone.parkingCost = destinationTaz.workParkingCost;
                } else {
                    thisZone.parkingCost = destinationTaz.nonWorkParkingCost * nonWorkParkingCostFactor;
                }
                thisZone.terminalTime = destinationTaz.terminalTime;

                TourModePersonAttributes attributes = setPersonTourModeAttributes(originTaz, destinationTaz,
                        thisPurpose, segment);
                departCost = setDepartCost(thisPurpose, skims, originTaz,
                        destinationTaz);

                returnCost = setReturnCost(thisPurpose, skims, originTaz,
                        destinationTaz);

                if (trace) {
                    departCost.printToScreen();
                    returnCost.printToScreen();
                }
                
                double logsum = mcModel.calculateUtility(departCost,
                        returnCost, attributes , thisZone);

                m.setDoubleValueAt(originTaz.zoneNumber, destinationTaz.zoneNumber, logsum,-999,Float.MAX_VALUE);

            } //end destination zone loop
        } // end origin zone loop

        logger.info("Created logsums in " + (System.currentTimeMillis() - time)
                / 1000 + " seconds");

        return m;
    }

 
    /**
     * setPersonTourModeAttributes
     * 
     * @param thisPurpose purpose
     * @param originTaz origin taz
     * @param destinationTaz destination taz
     * @param segment segment
     * @return TourModePersonAttributes tour mode attributes
     */ 
    public TourModePersonAttributes setPersonTourModeAttributes(Taz originTaz,
                                            Taz destinationTaz,
                                            ActivityPurpose thisPurpose,
                                            int segment){
        
        //PersonTourModeAttributes ptma = new PersonTourModeAttributes();                                                                                                     
        ptma.originZone=originTaz.zoneNumber;
        ptma.destinationZone=destinationTaz.zoneNumber;
                       
        //set tour purpose in person object
        //ptma.tourPurpose=purposes.charAt(purpose);
        ptma.tourPurpose=thisPurpose;                    
                                        
        //set TravelTimeAndCosts (default=offpeak)
        ptma.primaryDuration=120;
                         
        //set primary duration to 480 for work or school, 120 for other purposes
        if(thisPurpose==ActivityPurpose.WORK||thisPurpose==ActivityPurpose.COLLEGE||thisPurpose==ActivityPurpose.GRADESCHOOL)
            ptma.primaryDuration=480;
        else 
            ptma.primaryDuration=120;

        ptma.auwk0=auwk0segs[segment];
        ptma.auwk1=auwk1segs[segment];
        ptma.auwk2=auwk2segs[segment];

        if(ptma.auwk0==1)
            ptma.autos=0;
        else
            ptma.autos=1;
    
        ptma.inclow=inclowsegs[segment];
        ptma.incmed=incmedsegs[segment];
        ptma.inchi=inchisegs[segment];

        ptma.age = 20;
        return ptma;
    }// end setPersonTourModeAttributes
    
    /**
     * setDepartCost
     * Calculates the depart traves times and costs for the origin-destination pair
     * @param thisPurpose purpose
     * @param skims skims
     * @param originTaz origin taz
     * @param destinationTaz destination taz
     * @return TravelTimeAndCost travel time and cost
     */
    public TravelTimeAndCost setDepartCost(ActivityPurpose thisPurpose,
            SkimsInMemory skims, Taz originTaz, Taz destinationTaz) {
        if(thisPurpose==ActivityPurpose.WORK||thisPurpose==ActivityPurpose.GRADESCHOOL||thisPurpose==ActivityPurpose.COLLEGE)                        
            return skims.setTravelTimeAndCost(departCost, originTaz.zoneNumber, 
                                              destinationTaz.zoneNumber, 
                                              800);                         
        else 
            return skims.setTravelTimeAndCost(departCost, originTaz.zoneNumber, 
                                              destinationTaz.zoneNumber, 
                                              1200);
     }// end setDepartCost
    
    /** setReturnCost
     * Calculates the return travel times and costs for the origin-destination pair
     * @param thisPurpose purpose
     * @param skims skims
     * @param originTaz origin taz
     * @param destinationTaz destination taz
     * @return TravelTimeAndCost travel time and cost
     */   
    public TravelTimeAndCost setReturnCost(ActivityPurpose thisPurpose,
            SkimsInMemory skims, Taz originTaz, Taz destinationTaz) {
        if (thisPurpose == ActivityPurpose.WORK
                || thisPurpose == ActivityPurpose.GRADESCHOOL
                || thisPurpose == ActivityPurpose.COLLEGE)
            return skims.setTravelTimeAndCost(returnCost,
                    destinationTaz.zoneNumber, originTaz.zoneNumber, 1800);
        else
            return skims.setTravelTimeAndCost(returnCost,
                    destinationTaz.zoneNumber, originTaz.zoneNumber, 1300);
    }// end setReturnCost
    
    /**
     * Use this method to update logsums; only will update logsums if they are
     * different from the segment passed into the model.
     * 
     * @param segment segment
     */
    public void updateLogsums(int segment){
        
         // if the segment is different from the current segment, update
            // logsums
        if(segment!=currentSegment){

            if (logger.isDebugEnabled()) {
                logger.debug("Free memory before updating logsums: "+Runtime.getRuntime().freeMemory());
            }

            //loop through activity purposes, updating work activity logsums
          ActivityPurpose[] purpose = ActivityPurpose.values();
           for(int i=0;i<purpose.length;++i){
               if(purpose[i]==ActivityPurpose.HOME)
                   continue;
               logsums[i] = readLogsumMatrix(purpose[i],segment);
               currentSegment=segment;
           }
            if (logger.isDebugEnabled()) {
                logger.debug("Free memory after updating logsums: "+Runtime.getRuntime().freeMemory());
            }
        }
    }


   /** This method returns the file name of the matrix for a particular purpose and segment.
    * 
    * @param purpose purpose
    * @param segment segment
    * @return String activity string
    */
   public String getName(ActivityPurpose purpose, int segment){
       
       return getActivityString(purpose) + segment + "mcls";
   }
   
   /** 
    * Read a tour mc logsum matrix from disk for a particular tour purpose and market segment.
    * 
    * @param purpose purpose
    * @param segment segment
    * @return  The logsum matrix.
    */
   public Matrix readLogsumMatrix(ActivityPurpose purpose,int segment){
       //get path to skims
       String path = ResourceUtil.getProperty(rb, "sdt.current.mode.choice.logsums");
       String ext = ResourceUtil.getProperty(rb, "matrix.extension",".zmx");
       String name = path + getName(purpose, segment) + ext;
       
       Matrix m = null; 
       
       logger.info("Reading matrix " + name);
       
       try {
    	   m = MatrixReader.readMatrix(new File(name), name); 
       } catch (MatrixException e) {
    	   e.printStackTrace();
    	   while (m==null) {
    		   try {
    			   Thread.sleep(10000);
    		   } catch (InterruptedException ie) {
    			   ie.printStackTrace();
    		   }
    		   logger.error("Attempting again to read matrix " + name); 
    		   m = MatrixReader.readMatrix(new File(name), name);
    	   }
       }
       return m; 
  }
   
   /** 
    * Write a tour mc logsum matrix to disk for a particular tour purpose and market segment.
    * 
    * @param purpose The purpose of the matrix
    * @param segment The segment of the matrix
    * @param logsumMatrix  The matrix to write
    */
   public void writeLogsumMatrix(ActivityPurpose purpose,int segment, Matrix logsumMatrix){
       //get path to skims
       String path = ResourceUtil.getProperty(rb, "sdt.current.mode.choice.logsums");
       String ext = ResourceUtil.getProperty(rb, "matrix.extension");

       //construct name of file to write
       String name = path + getName(purpose, segment) + ext;
         
       MatrixWriter logsumWriter= MatrixWriter.createWriter(MatrixType.ZIP,new File(name)); 				
       logsumWriter.writeMatrix(logsumMatrix);
  }

   /**
    * Get a logsum matrix from the manager for a particular purpose and segment.
    * @param purpose Purpose
    * @param segment Segment
    * @return the tour mode choice logsum matrix for the purpose and segment.
    */
   public Matrix getLogsumMatrix(ActivityPurpose purpose, int segment){

       if (currentSegment != segment) {
            logger.debug("Updating logsum matrices to segment " + segment);
            updateLogsums(segment);
        }

       if(logsums[purpose.ordinal()]==null){
           logger.fatal("Error attempting to get a mode choice logsum matrix for ");
           logger.fatal("purpose "+purpose+" segment "+segment);
           throw new RuntimeException();
       }
       return logsums[purpose.ordinal()];
   }

    

   /**
    * Squeeze a matrix in the MatrixCollection.
    * @param purpose Activity purpose
    * @param segment segment
    * @param matrix Matrix to be squeezed
    * @return Matrix squeezed matrix.
    *
    */
   public Matrix squeeze(ActivityPurpose purpose, int segment, Matrix matrix) {
       MatrixCompression matrixCompression = new MatrixCompression(tazToAmz);

        return matrixCompression.getCompressedMatrix(matrix, "MEAN");
    }
   
   /**
    * Write a squeezed matrix.
    * @param purpose Activity purpose
    * @param segment segment
    * @param matrix Matrix to be written
    */
   public void writeSqueezedMatrix(ActivityPurpose purpose, int segment, Matrix matrix) {
       //get path to skims
       String path = ResourceUtil.getProperty(rb, "sdt.current.mode.choice.logsums");
       String ext = ResourceUtil.getProperty(rb, "matrix.extension");

       //construct name of file to write
       String name = path + getName(purpose, segment) + "AMZ" + ext;

       Matrix squeezed = squeeze(purpose, segment, matrix);

       MatrixWriter logsumWriter= MatrixWriter.createWriter(MatrixType.ZIP,new File(name)); 					
       logsumWriter.writeMatrix(squeezed);
   }

   /** Main method creates logsum matrices for all purposes and segments.
    * 
    * @param args Runtime arguments
    */
    public static void main(String[] args) {
        logger.info("creating tour mode choice logsums");
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");

        TourModeChoiceLogsumManager logsumManager = new TourModeChoiceLogsumManager(
                globalRb, rb);
        // read the skims into memory
        String tazManagerClass = ResourceUtil.getProperty(rb,"sdt.taz.manager.class");
        Class tazClass = null;
        TazManager tazManager;
        try {
            tazClass = Class.forName(tazManagerClass);
            tazManager = (TazManager) tazClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.fatal("Can't create new instance of TazManager of type "+tazClass.getName());
            throw new RuntimeException(e);
        }
       String tazClassName = rb.getString("sdt.taz.class");
        tazManager.setTazClassName(tazClassName);
        tazManager.readData(globalRb, rb);

        // read the tourModeParameters from jDataStore; if they don't exist,
        // write them to jDataStore first from csv
        SkimsInMemory skims = SkimsInMemory.getSkimsInMemory();
        skims.setGlobalProperties(globalRb);
        skims.readSkims(rb);

        // enter loop on purposes
        ActivityPurpose[] purposes = ActivityPurpose.values();
       for (ActivityPurpose purpose : purposes) {

           if (purpose == ActivityPurpose.HOME)
               continue;

           // enter loop on segments
           for (int segment = 0; segment < TOTALSEGMENTS; ++segment) {
               logger.info("Creating ModeChoiceLogsumMatrix for purpose:"
                       + purpose + " segment: " + segment);
               Matrix m = logsumManager.createLogsumMatrix(purpose,
                       segment, tazManager, skims);
               logger.info("Writing Matrix " + m.getName());
               logsumManager.writeLogsumMatrix(purpose, segment, m);
               logger.info("Created modeChoiceLogsum for purpose "
                       + purpose + " segment " + segment);
           }
       }
       logger.info("created tour mode choice logsums");
    }
}


