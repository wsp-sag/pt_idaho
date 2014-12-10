/*
 * Copyright  2005 PB Consult Inc.
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
 */
package com.pb.models.pt.surveydata;

import java.util.HashMap;
import java.util.ArrayList;

/**
 * @author Andrew Stryker <stryker@pbworld.com>
 *
 */
public abstract class AbstractSurveyData implements Cloneable {
    private HashMap<String, Object> attributes = null;

    protected int type;

    /**
     * @return Returns the attribute as an Object.
     */
    public Object getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    /**
     * @return Returns the attribute as an int.
     */
    public int getAttributeAsInt(String attribute) {
        return ((Integer) attributes.get(attribute)).intValue();
    }

    /**
     * @return Returns the attribute as a double.
     */
    public double getAttributeAsDouble(String attribute) {
        return ((Double) attributes.get(attribute)).doubleValue();
    }

    /**
     * @return Returns the attribute as a String.
     */
    public String getAttributeAsString(String attribute) {
        return (String) attributes.get(attribute);
    }

    /**
     * @return Returns the attribute as an ArrayList.
     */
    public ArrayList getAttributeAsArrayList(String attribute) {
        return (ArrayList) attributes.get(attribute);
    }

    /**
     * @param attribute
     *            The attribute to set.
     */
    public void setAttribute(String attribute, Object value) {
        if (attributes == null) {
            attributes = new HashMap<String, Object>();
        }
        attributes.put(attribute, value);
    }

    /**
     * @param attribute
     *            The attribute to set.
     */
    public void setAttribute(String attribute, int value) {
        setAttribute(attribute, new Integer(value));
    }

    /**
     * @param attribute
     *            The attribute to set.
     */
    public void setAttribute(String attribute, double value) {
        setAttribute(attribute, new Double(value));
    }

    /**
     * @param attribute
     *            The attribute to set.
     */
    public void setAttribute(String attribute, ArrayList value) {
        setAttribute(attribute, (Object) value);
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /**
     * Support cloning.
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
