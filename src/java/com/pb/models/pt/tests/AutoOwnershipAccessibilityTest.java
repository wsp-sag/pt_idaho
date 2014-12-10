/*
 * Copyright  2008 PB Consult Inc.
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

import java.io.File;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.util.ResourceUtil;
import com.pb.models.pt.AutoOwnershipModel;
import com.pb.models.pt.util.SkimsInMemory;

/**
 * @author Erhardt
 * @version 1.0 Mar 24, 2008
 *
 */
public class AutoOwnershipAccessibilityTest {
    transient static Logger logger = Logger.getLogger(AutoOwnershipModel.class);
    
    /**
     * @param args
     */
    public static void main(String[] args) {

        ResourceBundle ptRb = ResourceUtil.getPropertyBundle(new File("z:\\models\\osmp_gui\\Base\\t0\\zzapp.properties"));
        ResourceBundle globalRb = ResourceUtil.getPropertyBundle(new File("z:\\models\\osmp_gui\\Base\\t0\\zzapp.properties"));

        //The SkimReaderTask will read in the skims
        //prior to any other task being asked to do work.
        SkimsInMemory skims = SkimsInMemory.getSkimsInMemory();
        if(!skims.isReady()){
            logger.info("Reading Skims into memory");
            skims.setGlobalProperties(globalRb);
            skims.readHighwaySkims(ptRb);
        }
        
        AutoOwnershipModel aom = new AutoOwnershipModel( ptRb,  skims.pkTime,  skims.pkDist,  globalRb.getString("alpha.name"));
        aom.buildModel(); 

        logger.info("Finished testing auto ownership accessibilities"); 
    }

}
