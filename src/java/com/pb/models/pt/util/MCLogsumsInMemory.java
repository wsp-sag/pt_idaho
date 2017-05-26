package com.pb.models.pt.util;

import static com.pb.models.pt.ActivityPurpose.getActivityString;

import java.io.File;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.daf.Message;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixException;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.ActivityPurpose;
import com.pb.models.pt.TourModeChoiceLogsumManager;
import com.pb.models.pt.daf.DCLogsumCalculatorTask;
import com.pb.models.pt.daf.MessageID;

/**
 * A class that reads mode choice logsums in memory. 
 * 
 * @author Ashish Kulshrestha
 */

public class MCLogsumsInMemory {
    protected static final Object lock = new Object();
    protected static Logger logger = Logger.getLogger(SkimsInMemory.class);
    protected static ResourceBundle globalRb;
    public static Matrix[][] mcLogsumsInMemory;
    private static boolean logsumsRead = false;
    
  	public MCLogsumsInMemory() {
  		readLogsums();
  		setLogsumsRead(true);
  	}

    public Matrix[][] getLogsumsInMemory() {
        return mcLogsumsInMemory;
    }

	/**
	 * @return the logsumsRead
	 */
	public static boolean isLogsumsRead() {
		return logsumsRead;
	}

	/**
	 * @param logsumsRead the logsumsRead to set
	 */
	public static void setLogsumsRead(boolean logsumsRead) {
		MCLogsumsInMemory.logsumsRead = logsumsRead;
	}

	private void readLogsums() {
		ResourceBundle runParamsRb = ResourceUtil.getResourceBundle("RunParams");
		String pathToGlobalRb = ResourceUtil.getProperty(runParamsRb, "pathToGlobalRb");
	    globalRb = ResourceUtil.getPropertyBundle(new File(pathToGlobalRb));
	    
	    String path = ResourceUtil.getProperty(globalRb, "sdt.current.mode.choice.logsums");
        String ext = ResourceUtil.getProperty(globalRb, "matrix.extension",".zmx");
        mcLogsumsInMemory = new Matrix[ActivityPurpose.values().length][TourModeChoiceLogsumManager.TOTALSEGMENTS];
        
        logger.info("Reading mode choice logsums into memory ...");
        
        for (ActivityPurpose purpose : ActivityPurpose.values()){
        	if (purpose == ActivityPurpose.HOME)
        		continue;

        	for (int segment = 0; segment < TourModeChoiceLogsumManager.TOTALSEGMENTS; segment++) {
        		String fileName = path + getActivityString(purpose) + segment + "mcls" + ext;
        		Matrix m = null; 
        		String name = getActivityString(purpose) + segment + "mcls";
        		logger.debug("Reading matrix " + fileName);
        		MatrixReader matReader = null;

        		try {
        			matReader = MatrixReader.createReader(fileName);
        			m = matReader.readMatrix(name);
        		} catch (MatrixException e) {
        			e.printStackTrace();
        			while (m==null) {
        				try {
        					Thread.sleep(10000);
        				} catch (InterruptedException ie) {
        					ie.printStackTrace();
        				}
        				logger.error("Attempting again to read matrix " + name); 
        				m = matReader.readMatrix(name);
        			}
        		}
        		mcLogsumsInMemory[purpose.ordinal()][segment] = m;
        	}
        }
	}

	public static void main(String[] args) {
		MCLogsumsInMemory logsumsInMemory = new MCLogsumsInMemory();
        mcLogsumsInMemory = logsumsInMemory.getLogsumsInMemory();
        System.out.println("dimensions : " + mcLogsumsInMemory.length + ", " + mcLogsumsInMemory[0].length);
        System.out.println("mode choice logsums read");
    }
    
}







