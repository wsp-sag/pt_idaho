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
package com.pb.idaho.pt.ldt;

import com.pb.models.pt.ldt.LDInternalExternalModel;
import com.pb.models.pt.ldt.ParameterReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.idaho.model.WorldZoneExternalZoneUtil;

import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * This class is used for ...
 * Author: Christi Willison
 * Date: Oct 11, 2007
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public class ITDLDInternalExternalModel extends LDInternalExternalModel {

    public ITDLDInternalExternalModel(){
        super();
    }



//    public void readExternalStations(ResourceBundle rb){
//        // get the external zones
//    	externalStations = new ArrayList<Integer>();
//        int[] externalZones = ResourceUtil.getIntegerArray(rb, "external.zones");
//        
//        for(int zone : externalZones){
//            externalStations.add(zone);    
//        }
//    	
//    }
    
    public void readExternalStations(ResourceBundle rb){
    	// get the external zones
    	externalStations = new ArrayList<Integer>();
        TableDataSet externalStationData = ParameterReader.readParametersAsTable(rb, "ldt.external.station.data");
    	for (int i = 0; i < externalStationData.getRowCount(); i++) {
    		int extSta = (int) externalStationData.getValueAt(i + 1, "ExSta");
    		externalStations.add(extSta);
    	}
    }
}