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

public enum PrimaryTourModeType {
    AUTO_DRIVER, // driver for any trip on tour, no school bus
    AUTO_PASSENGER, // passenger or non-motorized for all trips
    WALK, // walker for all trips
    BIKE, // bicyle or walk for all trips
    WALK_TRANSIT, // walk-transit trip on tour, no drive-transit; includes
    // trips passenger and transit on same half-tour
    TRANSIT_PASSENGER, // any trip outbound is walk-transit and any return trip
    // is passenger
    PASSENGER_TRANSIT, // any trip outbound is passenger and any return trip is
    // walk-transit
    DRIVE_TRANSIT
    // any trip outbound is auto-access transit and any return trip is
    // auto-access transit
}
