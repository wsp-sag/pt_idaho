/*
 * Copyright 2005 PB Consult Inc.
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
 * Created on Sep 14, 2005 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.sdt;

/**
 * Market segmentation for tours.
 */
public enum MarketSegmentType {
    // comments below reflect non-work tours; replace hhsize with hhworkers for
    // work tours
    LOW_NOAUTO, // hhinc < 15000, autos = 0
    LOW_SCARCE, // hhinc < 15000, autos < hhsize
    LOW_SUFFICIENT, // hhinc < 15000, autos >= hhsize
    MED_NOAUTO, // 15000 <= hhinc < 30000, autos = 0
    MED_SCARCE, // 15000 <= hhinc < 30000, autos < hhsize
    MED_SUFFICIENT, // 15000 <= hhinc < 30000, autos >= hhsize
    HIGH_NOAUTO, // hhinc >= 30000, autos = 0
    HIGH_SCARCE, // hhinc >= 30000, autos < hhsize
    HIGH_SUFFICIENT // hhinc >= 30000, autos >= hhsize
}
