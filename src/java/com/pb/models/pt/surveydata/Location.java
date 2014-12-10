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
 * Created on Jul 12, 2005 by Andrew Stryker <stryker@pbworld.com>
 *
 */
package com.pb.models.pt.surveydata;

import org.apache.log4j.Logger;

/**
 * Spatial information.
 * 
 * TODO: Recognize common coordinate system and automatically coordinates.
 * Perhaps there should be a coordinate object which handles this? Or code that
 * functionality here. This would a more backward compatiable solution.
 * 
 * @author Andrew Stryker <stryker@pbworld.com>
 * 
 */
public class Location extends AbstractSurveyData {
    protected static Logger logger = Logger
            .getLogger("com.pb.models.surveydata");

    private long location;

    private double ycord;

    private double xcord;

    private boolean coordinates = false;

    private long taz = -1;

    public static final int DECIMAL_DEGREES = 1;

    public static final int STATE_PLANE = 2;

    private int coordinateSystem = 0;

    /**
     * Constructor.
     * 
     */
    public Location() {
    }

    public Location(double xcord, double ycord) {
        this.xcord = xcord;
        this.ycord = ycord;
        coordinates = true;
    }

    /**
     * Constructor.
     * 
     * @param location
     */
    public Location(int location) {
        this.location = location;
    }

    /**
     * Set the coordinate coordinateSystem.
     */
    public void setCoordinateSystem(int system) {
        this.coordinateSystem = system;
    }

    /**
     * Get the coordinate coordinateSystem
     */
    public int getCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * @return Returns the taz.
     */
    public long getTaz() {
        return taz;
    }

    /**
     * @param taz
     *            The taz to set.
     */
    public void setTaz(long taz) {
        this.taz = taz;
    }

    /**
     * @return Returns the ycord.
     */
    public double getYcord() {
        return ycord;
    }

    /**
     * @param xcord
     *            The xcord to set.
     * @param ycord
     *            The ycord to set.
     */
    public void setCoordinates(double xcord, double ycord) {
        this.xcord = xcord;
        this.ycord = ycord;
        coordinates = true;
    }

    /**
     * @return Returns the location.
     */
    public long getLocation() {
        return location;
    }

    /**
     * @return Returns the xcord.
     */
    public double getXcord() {
        return xcord;
    }

    /**
     * Computes the distance between two lcocations.
     * 
     * This method uses coordinate systems information to determine the distance
     * between two Locations.
     * 
     */
    public double distance(Location other) {
        if (hasCoordinates() && other.hasCoordinates()) {
            if (coordinateSystem == DECIMAL_DEGREES
                    && other.getCoordinateSystem() == DECIMAL_DEGREES) {
                return distanceMiles(other);
            } else if (coordinateSystem == STATE_PLANE
                    && other.getCoordinateSystem() == STATE_PLANE) {
                return pythagoras(other) / 5280.0;
            }
        }

        return -1;
    }

    /**
     * Compute the distance between two locations in miles using the Haversine
     * method.
     * 
     * @param other
     *            The location in
     * @return The distance between the two locations in miles.
     */
    public double distanceKM(Location other) {
        final double earthRadius = 6373; // km

        double lat1 = Math.toRadians(getYcord());
        double long1 = Math.toRadians(getXcord());

        double lat2 = Math.toRadians(other.getYcord());
        double long2 = Math.toRadians(other.getXcord());

        double dLat = lat2 - lat1;
        double dLon = long2 - long1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(lat1)
                * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return c * earthRadius;
    }

    /**
     * Compute the distance between two locations in miles using the Haversine
     * method.
     * 
     * @param other
     *            The comparative location.
     * @return The distance between the two locations in miles.
     */
    public double distanceMiles(Location other) {
        final double kmMileConversion = 0.621371192237;
        return distanceKM(other) * kmMileConversion;
    }

    /**
     * Compute the distance between two locations using the Pythagoras.
     * 
     * @param other
     *            The comparison location.
     * 
     * @return The distance between the two locations in miles.
     */
    public double pythagoras(Location other) {
        return Math.sqrt(Math.pow((ycord - other.getYcord()), 2)
                + Math.pow((xcord - other.getXcord()), 2));
    }

    /**
     * Compute the deviation distance.
     */
    public double deviationDistance(Location a, Location b) {
        double d_ij = a.distance(b);
        double d_ik = distance(a);
        double d_kj = distance(b);

        return d_ik + d_kj - d_ij;
    }

    /**
     * Compares two locations for equality. Locations with the same coordinates
     * are equal.
     * 
     * @param other
     *            The Location for comparison.
     * @return true if the two locations have the same coordinates.
     */
    public boolean equals(Location other) {
        return compareCoordinates(other);
    }

    /**
     * Compare coordinates of two locations.
     * 
     * @param other
     *            The Location for comparison.
     * @return true if the two locations have the same coordinates.
     */
    public boolean compareCoordinates(Location other) {
        return this.ycord == other.getYcord() && this.xcord == other.getXcord();
    }

    /**
     * Compares two locations for equality. Locations with the same coordinates
     * are equal.
     * 
     * @param other
     *            The Location for comparison.
     * @param tolerance
     *            The tolerance (in miles) for comparing equality.
     * @return true if the two locations have the same coordinates.
     */
    public boolean equals(Location other, double tolerance) {
        if (hasCoordinates() && other.hasCoordinates()
                && distanceMiles(other) <= tolerance) {
            return true;
        }
        return false;
    }

    /**
     * Check for bad geocodes. Assume that clients only set good coordinates.
     * 
     * @return
     */
    public boolean hasCoordinates() {
        return coordinates;
    }

    /**
     * Summarize the location in as a String.
     */
    public String toString() {
        String res = new Long(location).toString();

        if (ycord != -99999 && xcord != -99999) {
            res += " (" + ycord + ", " + xcord + ")";
        }

        if (taz != -99999) {
            res += " taz(" + taz + ")";
        }

        return res;
    }

    /**
     * Basic tests.
     */
    public static void main(String[] args) {
        Location loc1 = new Location(23); // Sandberg's number
        logger.info("Location 1: " + loc1);
        logger.info("Setting coordinates.");
        loc1.setCoordinates(-1 * (1. + 50. / 60. + 40. / 3600.), 53. + 9. / 60.
                + 2. / 3600.);
        logger.info("Location is now: " + loc1);
        logger.info("Setting TAZ");
        loc1.setTaz(432);
        logger.info("Location is now: " + loc1);
        logger.info("Setting TAZ (again)");
        loc1.setTaz(23);
        logger.info("Location is now: " + loc1);

        Location loc2 = new Location(10); // Ron Santo's number
        loc2.setTaz(83);
        loc2.setCoordinates(-1 * (0. + 8. / 60. + 33. / 3600.), 52. + 12. / 60.
                + 19. / 3600.);
        logger.info("Location 2:" + loc2);

        logger.info("The separation distance should be 96.7 miles: "
                + loc1.distanceMiles(loc2));
        logger.info("The separation distance should be 155.6 (km): "
                + loc1.distanceKM(loc2));
    }
}
