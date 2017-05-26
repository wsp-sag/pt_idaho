package com.pb.models.pt.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * This class is used for holding the
 * list of industries, occupation for person data,
 * industry categories for employment data 
 * and mapping between them for Idaho Statewide Model
 * Author: Ashish Kulshrestha
 * Date: Feb 12, 2015
 * Email: kulshresthaa@pbworld.com
 */

public class OccupationIndustryMapping {

	TableDataSet correspondenceFile;
	
    private static Set<String> personIndustryLabels;    
    private static Set<String> personOccupationLabels;  
    private static Set<String> employmentIndustryLabels;
	
    private static Set<Integer> personIndustryCodes;    
    private static Set<Integer> personOccupationCodes;  
    private static Set<Integer> employmentIndustryCodes;
    
    private static int maxPersonIndustryIndex;
    private static int maxPersonOccupationIndex;
    private static int maxEmploymentIndustryIndex;
    
    private static String[] personIndustryLabelsByIndex;
    private static String[] personOccupationLabelsByIndex;
    private static String[] employmentIndustryLabelsByIndex;
    
    private static HashMap<String,Integer> personIndustryLabelsToIndex;      
    private static HashMap<String,Integer> personOccupationLabelsToIndex;    
    private static HashMap<String,Integer> employmentIndustryLabelsToIndex;
	
    enum Type {Industry, Occupation, EmpIndustry} // these correspond to header names in corresp. file
	
    public OccupationIndustryMapping(String corresFile){
        readCorrespondenceFile(corresFile);

        maxPersonIndustryIndex = findMaxIndex(Type.Industry);
        maxPersonOccupationIndex = findMaxIndex(Type.Occupation);
        //maxEmploymentIndustryIndex = findMaxIndex(Type.EmpIndustry);

        personIndustryLabels = defineLabels(Type.Industry, personIndustryLabels);
        personOccupationLabels = defineLabels(Type.Occupation, personOccupationLabels);
        //employmentIndustryLabels = defineLabels(Type.EmpIndustry, employmentIndustryLabels);
        
        personIndustryCodes = defineCodes(Type.Industry, personIndustryCodes);
        personOccupationCodes = defineCodes(Type.Occupation, personOccupationCodes);
        //employmentIndustryLabels = defineLabels(Type.EmpIndustry, employmentIndustryLabels);

        //personIndustryLabelsToIndex = defineLabelToIndexCorrespondence(Type.Industry, personIndustryLabelsToIndex );
        //personOccupationLabelsToIndex = defineLabelToIndexCorrespondence(Type.Occupation, personOccupationLabelsToIndex);
        //employmentIndustryLabelsToIndex = defineLabelToIndexCorrespondence(Type.EmpIndustry, employmentIndustryLabelsToIndex);

        //personIndustryLabelsByIndex = createLabelArray(Type.Industry, personIndustryLabelsByIndex);
        //personOccupationLabelsByIndex = createLabelArray(Type.Occupation, personOccupationLabelsByIndex);
        //employmentIndustryLabelsByIndex = createLabelArray(Type.EmpIndustry, employmentIndustryLabelsByIndex);
    }
	
    private void readCorrespondenceFile(String file){
        CSVFileReader reader = new CSVFileReader();
        try {
            correspondenceFile = reader.readFile(new File(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private int findMaxIndex (Type category){
        int[] indices = new int[0];
        int max = 0;
        switch(category){
            case Industry:
                indices = correspondenceFile.getColumnAsInt(Type.Industry.toString() + "_Code");
                break;
            case Occupation:
                indices = correspondenceFile.getColumnAsInt(Type.Occupation.toString() + "_Code");
                break;
            case EmpIndustry:
                indices = correspondenceFile.getColumnAsInt(Type.EmpIndustry.toString() + "_Code");
                break;
        }
        for(int i: indices){
            if(i > max)
                max = i;
        }
        return max;
    }
    
    private Set<String> defineLabels(Type labelCol, Set<String> uniqueValues){
        String[] colStringValues = correspondenceFile.getColumnAsString(labelCol.toString());

        uniqueValues = new HashSet<String>();
        for(String colStringValue: colStringValues){
            uniqueValues.add(colStringValue);
        }

        return uniqueValues;
    }
    
    private Set<Integer> defineCodes(Type labelCol, Set<Integer> uniqueValues){
        int[] colIntValues = correspondenceFile.getColumnAsInt(labelCol.toString() + "_Code");

        uniqueValues = new HashSet<Integer>();
        for(Integer colStringValue: colIntValues){
            uniqueValues.add(colStringValue);
        }

        return uniqueValues;
    }
	
    private HashMap<String, Integer> defineLabelToIndexCorrespondence(Type labelCol, HashMap<String, Integer> labelToIndex){
        String[] colStringValues = correspondenceFile.getColumnAsString(labelCol.toString());
        int[] colIntValues = correspondenceFile.getColumnAsInt(labelCol.toString() + "_Code");

        labelToIndex = new HashMap<String, Integer>();
        String label;
        Integer index;
        for(int i=0; i< colStringValues.length; i++){
            label = colStringValues[i];
            index = colIntValues[i];
            labelToIndex.put(label, index);
        }

        return labelToIndex;
    }
    
    private String[] createLabelArray(Type category, String[] labelArray){
        int length;
        HashMap<String, Integer> map = null;
        switch(category){
            case Industry:
                length = personIndustryLabels.size();
                if(length == maxPersonIndustryIndex + 1 ) //indicates that the index starts at 0
                    labelArray = new String[length];
                else
                    labelArray = new String[length+1]; //index starts at 1
                map = personIndustryLabelsToIndex;
                break;
            case Occupation:
                length = personOccupationLabels.size();
                if(length == maxPersonOccupationIndex + 1)
                    labelArray = new String[length];
                else
                    labelArray = new String[length + 1];
                map = personOccupationLabelsToIndex;
                break;
            case EmpIndustry:
                length = employmentIndustryLabels.size();
                if(length == maxEmploymentIndustryIndex + 1)
                    labelArray = new String[length];
                else
                    labelArray = new String[length + 1];
                map = employmentIndustryLabelsToIndex;
                break;
        }

        for (String s : map.keySet()) {
            int index = map.get(s);
            labelArray[index] = s;
        }
        return labelArray;
    }

    private Set<String> getLabels(Type category){
        switch(category){
            case Industry: return personIndustryLabels;
            case Occupation: return personOccupationLabels;
            case EmpIndustry: return employmentIndustryLabels;
            default: return null; //should never get here
        }
    }

    private int getIndexFromLabel(Type category, String label){
        try {
            switch(category){
                case Industry:
                    return personIndustryLabelsToIndex.get(label);
                case Occupation:
                    return personOccupationLabelsToIndex.get(label);
                case EmpIndustry:
                    return employmentIndustryLabelsToIndex.get(label);
                default:
                    return -1;  //should never get here
            }
         } catch (Exception e) {
            throw new RuntimeException(label + " is not a valid " + category);
        }
    }

    private String getLabelFromIndex(Type category, int index){
        try {
            switch(category){
                case Industry:
                    return personIndustryLabelsByIndex[index];
                case Occupation:
                    return personOccupationLabelsByIndex[index];
                case EmpIndustry:
                    return employmentIndustryLabelsByIndex[index];
                default:
                    return "";  //should never get here
            }
         } catch (Exception e) {
            throw new RuntimeException(index + " is not a valid index number in " + category);
        }
    }
    public int getIndustryIndexFromLabel(String label){
        return getIndexFromLabel(Type.Industry, label);
    }

    public int getOccupationIndexFromLabel(String label){
        return getIndexFromLabel(Type.Occupation, label);
    }

    public int getSplitIndustryIndexFromLabel(String label){
        return getIndexFromLabel(Type.EmpIndustry, label);
    }

    public String getIndustryLabelFromIndex(int index){
        return getLabelFromIndex(Type.Industry, index);
    }

    public String getOccupationLabelFromIndex(int index){
        return getLabelFromIndex(Type.Occupation, index);
    }

    public String getSplitIndustryLabelFromIndex(int index){
        return getLabelFromIndex(Type.EmpIndustry, index);
    }

    public int getNumOfIndustries(){
        return personIndustryLabels.size();
    }

    public int getNumOfOccupations(){
        return personOccupationLabels.size();
    }

    public int getNumOfSplitIndustries(){
        return employmentIndustryLabels.size();
    }

    public Set<String> getIndustryLabels(){
        return getLabels(Type.Industry);
    }

    public Set<String> getOccupationLabels(){
        return getLabels(Type.Occupation);
    }

    public Set<String> getSplitIndustryLabels(){
        return getLabels(Type.EmpIndustry);
    }

    public String[] getIndustryLabelsByIndex(){
        return personIndustryLabelsByIndex;
    }

    public String[] getOccupationLabelsByIndex(){
        return personOccupationLabelsByIndex;
    }

    public String[] getSplitIndustryLabelsByIndex(){
        return employmentIndustryLabelsByIndex;
    }

    public int getMaxIndustryIndex(){
        return maxPersonIndustryIndex;
    }

    public int getMaxOccupationIndex(){
        return maxPersonOccupationIndex;
    }

    public int getMaxSplitIndustryIndex(){
        return maxEmploymentIndustryIndex;
    }

    public HashMap<String, Integer> getIndustryLabelToIndexMapping(){
        return personIndustryLabelsToIndex;
    }

    public HashMap<String, Integer> getOccupationLabelToIndexMapping(){
        return personOccupationLabelsToIndex;
    }

	public static void main(String[] args) {
		String occIndMappingFile = "C:/Projects/IDAHO/pt_idaho/testSce/inputs/Parameters/occupationIndustryEmploymentMapping.csv";
        OccupationIndustryMapping occIndEmpRef = new OccupationIndustryMapping(occIndMappingFile);
        
//        System.out.println(maxPersonIndustryIndex);
//        System.out.println(maxPersonOccupationIndex);
//        System.out.println("*******");
//        
//        for (String label:personIndustryLabels){
//        	System.out.println(label);
//        }
//        System.out.println("*******");
//        for (String label:personOccupationLabels){
//        	System.out.println(label);
//        }
//        System.out.println("*******");     
        
        for (String label : personIndustryLabelsToIndex.keySet()){
        	System.out.println(personIndustryLabelsToIndex.get(label));
        }
        System.out.println("*******");
        for (String label : personOccupationLabelsToIndex.keySet()){
        	System.out.println(personOccupationLabelsToIndex.get(label));
        }
	}

}
