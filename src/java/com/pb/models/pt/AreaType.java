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
package com.pb.models.pt;

import com.pb.common.model.ModelException;

/**
 * @author Erhardt
 * @version 1.0 Apr 2, 2008
 *
 */
public enum AreaType {
    NONE, 
    RURAL,
    SUBURBAN,
    URBAN,
    CBD; 
    
    public static AreaType getAreaType(int index) {
        
        if (index==NONE.ordinal())          return NONE;
        else if (index==RURAL.ordinal())    return RURAL; 
        else if (index==SUBURBAN.ordinal()) return SUBURBAN; 
        else if (index==URBAN.ordinal())    return URBAN; 
        else if (index==CBD.ordinal())      return CBD; 
        else throw new ModelException("Invalid area type index code: " + index);
    }
}
