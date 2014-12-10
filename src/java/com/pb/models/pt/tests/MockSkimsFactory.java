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
 * Created on Aug 16, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt.tests;

import static java.lang.Math.abs;
import static java.lang.Math.exp;

import java.util.ResourceBundle;

import com.pb.common.matrix.Matrix;
import com.pb.models.pt.util.SkimsInMemory;

public class MockSkimsFactory {

    public static SkimsInMemory skimsInMemoryFactory(ResourceBundle rb) {
        SkimsInMemory skims = SkimsInMemory.getSkimsInMemory();
        skims.setGlobalProperties(rb);
        Matrix distance = new Matrix(4, 4);

        for (int i = 1; i <= 4; ++i) {
            for (int j = 1; j <= 4; ++j) {
                distance.setValueAt(i, j, (float) exp(abs(i - j)));
            }
        }

        skims.opDist = distance;
        skims.pkDist = distance;

        return skims;
    }

}
