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

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.PTDataReader;
import com.pb.models.pt.PTHousehold;
import com.pb.models.pt.PTResults;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Nov 16, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class PTResultsTest {
    protected static Logger logger = Logger.getLogger(PTResultsTest.class);

    public static void main(String[] args){
        ResourceBundle rb = ResourceUtil.getResourceBundle("pt");
        ResourceBundle globalRb = ResourceUtil.getResourceBundle("global");
        PTResults results = new PTResults(rb, globalRb);
        PTHousehold[] households;

        // Read household and person data
        PTDataReader dataReader = new PTDataReader(rb, globalRb, PTOccupation.NO_OCCUPATION,2000);
        logger.info("Adding synthetic population from database.");
        households = dataReader.readHouseholds();
        results.writeResults(households);

    }
}
