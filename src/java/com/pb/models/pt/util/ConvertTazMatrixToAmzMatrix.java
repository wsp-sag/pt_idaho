/*
 * Copyright 2007 PB Americas
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Created on Jan 8, 2007 by Andrew Stryker <stryker@pbworld.com>
 */

package com.pb.models.pt.util;

import java.io.File;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.pb.common.matrix.AlphaToBeta;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixCompression;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;

/**
 * @author Andrew Stryker
 * @version 0.1
 */
public class ConvertTazMatrixToAmzMatrix {
    private static Logger logger = Logger
            .getLogger(ConvertTazMatrixToAmzMatrix.class);

    /**
     * @param args [0] TAZ matrix
     * @param args [1] District matrix
     */
    public static void main(String[] args) {
        ResourceBundle bundle = ResourceUtil.getResourceBundle("global");
        Matrix tazMatrix = MatrixReader.readMatrix(new File(args[0]), args[0]);
        AlphaToBeta alphaToBeta = new AlphaToBeta(new File(bundle
                .getString("alpha2beta.file")), bundle.getString("alpha.name"),
                bundle.getString("beta.name"));

        logger.info("Compressing matrix in file " + args[0]);
        MatrixCompression compression = new MatrixCompression(alphaToBeta);
        Matrix districtMatrix = compression.getCompressedMatrix(tazMatrix,
                "SUM");

        logger.info("Writing results to file " + args[1]);

        MatrixWriter writer = MatrixWriter.createWriter(args[1]);
        writer.writeMatrix(districtMatrix);

        logger.info("All done.");
    }
}
