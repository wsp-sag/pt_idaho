package com.pb.models.pt;


public interface PTOccupationReferencer {

    /**
     * Convert occupation as a string into an enum.
     * 
     * @see 'pt.properties
     * 
     * @param occupation
     *            Occupation as a String
     * 
     * @return PTOccupation
     */
    public Enum getOccupation(String occupation);
    
    public Enum getOccupation(int index);

    public Enum getRetailOccupation();
    /**
     * Converts this enum into a string
     *
     * @see 'pt.properties'
     *
     * @return occupation
     *            Occupation as a String
     */
    public String toString();
}
