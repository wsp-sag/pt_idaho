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
 * Person types.
 */
public enum PersonType {
    PRESCHOOL, // age <= 5
    PRIMARY_SCHOOL, // 5 < age <= 17
    WORKER, // age > 17 and employed and not student
    COLLEGIATE, // age > 17 and student
    NON_WORKER // age > 17 and not employed and not student
}
