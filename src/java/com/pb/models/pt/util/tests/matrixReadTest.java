package com.pb.models.pt.util.tests;

import java.io.File;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.TpplusMatrixReader64;

public class matrixReadTest {

	public static void main(String[] args) {
		String fileName = "C:\\Projects\\IDAHO\\model_pt\\testSce\\outputs\\b1mcls.tpp";
		System.out.println("fileName: " + fileName);
		
		//MatrixReader reader = MatrixReader.createReader(MatrixType.TPPLUS, new File(fileName));
        //Matrix pkTime = reader.readMatrix(matName);
		
        //TpplusMatrixReader64 reader64 = new TpplusMatrixReader64(newFile);
        //Matrix pkTime = reader64.readMatrix(1);
        
		File newFile = new File(fileName);
        System.out.println("File exist " + newFile.exists());
        
        Matrix logsum =  MatrixReader.readMatrix(newFile, "1");

        int nColumns = logsum.getColumnCount();
        int nRows = logsum.getRowCount();
        
        System.out.println("Number of Columns: " + nColumns);
        System.out.println("Number of Rows: " + nRows);
		System.out.println("Matrix read successfully");
	}
}