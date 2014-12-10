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
package com.pb.models.pt.tests;

import com.pb.common.util.ObjectUtil;
import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTDataReader;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTPerson;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Nov 16, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class PTDataReaderTest {
    protected static Logger logger = Logger.getLogger(PTDataReaderTest.class);
    /**
     * Read the household and person files and report time and storage space
     * required.
     *
     * @param args Runtime Arguments
     */
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        logger.info("Running " + PTDataReader.class.getName());

        ResourceBundle pt = ResourceUtil.getResourceBundle("pt");
        ResourceBundle global = ResourceUtil.getResourceBundle("global");

        PTDataReader dataReader = new PTDataReader(pt, global, PTOccupation.NO_OCCUPATION,2000);

        PTHousehold[] households = dataReader.readHouseholds();
        PTPerson[] persons = dataReader.readPersons();

        PTDataReader.addHouseholdInfoToPersons(households, persons);

        logger.info("Finished reading and populating data structures");

        logger.info("Total time: "
                + ((System.currentTimeMillis() - startTime) / 1000)
                + " seconds.");

        logger.info("Size of households with persons = "
                + ObjectUtil.sizeOf(households));
    }
}
