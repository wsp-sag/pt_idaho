package com.pb.models.pt.tests;

import com.pb.models.pt.PTOccupationReferencer;

/**
 * This class is used for testing purposes.  Many of the classes in
 * PT need to have a PTOccupationReferencer and so you have to pass
 * one of the PTOccupation constants as that referencer and then you
 * can call the getOccupation(String) and getOccupation(index) using that
 * referencer object.
 *
 * I have changed the occupation names so they don't coincide with any of the
 *
 * Author: Christi Willison
 * Date: Oct 25, 2006
 * Email: willison@pbworld.com
 * Created by IntelliJ IDEA.
 */
public enum PTOccupation implements PTOccupationReferencer {
    NO_OCCUPATION, ENTERTAINER,
    SECRETARY, MEDICAL, RELIGIOUS,
    TECHNOLOGY, SALES, PAPER_PUSHER,
    HOTELIER;

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
    public PTOccupation getOccupation(String occupation) {
        if (occupation.startsWith("Medical")) {
            return MEDICAL;
        }
        if (occupation.startsWith("Secretary")) {
            return SECRETARY;
        }
        if (occupation.startsWith("Techology")) {
            return TECHNOLOGY;
        }
        if (occupation.startsWith("Religion")) {
            return RELIGIOUS;
        }
        if (occupation.startsWith("Hotel")) {
            return HOTELIER;
        }
        if (occupation.startsWith("Paper")) {
            return PAPER_PUSHER;
        }
        if (occupation.startsWith("Sales")) {
            return SALES;
        }
        if (occupation.startsWith("Entertainer")) {
            return ENTERTAINER;
        }
        if (occupation.startsWith("No Occupation")) {
            return NO_OCCUPATION;
        }
        return null;
    }

    public PTOccupation getOccupation(int index) {
        switch(index){
            case 0: return NO_OCCUPATION;
            case 1: return ENTERTAINER;
            case 2: return SECRETARY;
            case 3: return MEDICAL;
            case 4: return RELIGIOUS;
            case 5: return TECHNOLOGY;
            case 6: return SALES;
            case 7: return PAPER_PUSHER;
            case 8: return HOTELIER;
            default: return null;
        }

    }

    public PTOccupation getRetailOccupation(){
        return SALES;
    }


    /**
     * Converts this enum into a string
     *
     * @see 'pt.properties'
     *
     * @return occupation
     *            Occupation as a String
     */
    public String toString() {
        if (this.equals(NO_OCCUPATION)) {
            return "No Occupation";
        }
        if (this.equals(ENTERTAINER)) {
            return "Entertainer";
        }
        if (this.equals(SECRETARY)) {
            return "Secretary";
        }
        if (this.equals(MEDICAL)) {
            return "Medical";
        }
        if (this.equals(RELIGIOUS)) {
            return "Religious";
        }
        if (this.equals(TECHNOLOGY)) {
            return "Technology";
        }
        if (this.equals(SALES)) {
            return "Sales";
        }
        if (this.equals(PAPER_PUSHER)) {
            return "Paper Pusher";
        }
        if (this.equals(HOTELIER)) {
            return "Hotelier";
        }
        return null;

    }

    public static void main(String[] args){
        PTOccupationReferencer myRef = PTOccupation.NO_OCCUPATION;
        Enum occupation = myRef.getOccupation(2);

        System.out.println("Name: " + occupation.name());
        System.out.println("Index: " + occupation.ordinal());

        for(Enum e : occupation.getClass().getEnumConstants())
            System.out.println(e);
    }
}


