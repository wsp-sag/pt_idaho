/*
 * Copyright 2007 PB Consult Inc.
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

/**
@author Ofir Cohen
@version 1.0, Mar 15, 2007 
*/

import com.pb.common.util.ResourceUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;

public class VisitorDataReader extends PTDataReader{
    
    public VisitorDataReader(ResourceBundle ptRb,
            ResourceBundle globalRb, PTOccupationReferencer occRef, int baseYear) {
        super(ptRb,globalRb,occRef, baseYear);

    }

    public void openHouseholdFile(){
        String hhFileName = ResourceUtil.getProperty(ptRb, "vm.households");
        logger.info("Opening Visitor household file " + hhFileName);
        try {
            householdReader = new BufferedReader(new FileReader(hhFileName));
        } catch (IOException e) {
            logger.fatal("Could not open Visitor household file" + hhFileName);
            throw new RuntimeException(e);
        }
    }

    public void closeHouseholdFile(){
        String hhFileName = ResourceUtil.getProperty(ptRb, "vm.households");
        logger.info("Closing Visitor household file " + hhFileName);
        try {
            householdReader.close();
        } catch (IOException e) {
            logger.fatal("Could not close Visitor household file" + hhFileName);
            throw new RuntimeException(e);
        }
    }

    public void openPersonFile(){
        String perFileName = ResourceUtil.getProperty(ptRb, "vm.persons");
        // person file
        try {
            personReader = new BufferedReader(new FileReader(perFileName));
        } catch (IOException e) {
            logger.fatal("Could not Visitor open person file" + perFileName);
            throw new RuntimeException(e);
        }
    }

    public void closePersonFile(){
        String perFileName = ResourceUtil.getProperty(ptRb, "vm.persons");
        // person file
        try {
            personReader.close();
        } catch (IOException e) {
            logger.fatal("Could not close Visitor person file" + perFileName);
            throw new RuntimeException(e);
        }
    }
    
    public int[] getInfoFromVisitorHouseholdFile() {
        openHouseholdFile();
        try {
            int lowestIdNum = Integer.MAX_VALUE;
            int nHhsInFile = 0;
            // create a lookup table for field positions
            HashMap<String, Integer> positions = new HashMap<String, Integer>();
            String[] header = householdReader.readLine().split(",");
            for (int i = 0; i < header.length; ++i) {
                positions.put(header[i], i);
            }

            int hhIdIndex = positions.get(HH_ID_FIELD);
            int currentId = -1;
            String line = householdReader.readLine();

            while (line != null) {
                String[] fields = line.split(",");
                currentId = Integer.parseInt(fields[hhIdIndex]);
                if(currentId < lowestIdNum) lowestIdNum = currentId;
                nHhsInFile++;
                line = householdReader.readLine();
            }

            closeHouseholdFile();

            return new int[] {lowestIdNum, nHhsInFile};

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

