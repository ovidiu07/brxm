package org.hippoecm.hst.provider;

import java.util.Calendar;
import java.util.Map;

public interface ValueProvider {
    
    public String getPath();
    public String getName();
    
    /**
     * Method returning true when the jcr node has the property 
     * @param propertyName
     * @return true if the node has the propertyName
     */
    public boolean hasProperty(String propertyName);

    /**
     * Returns the string value of a node property
     * @param propertyName
     * @return String value of the node property, or null if the property does not exist, or is multivalued, or is not of type string
     */
    public String getString(String propertyName);
    
    /**
     * Returns String array of string values of a node property
     * @param propertyName
     * @return String[] values of the node property, or null if the property does not exist, or is single-values, or is not of type string
     */
    public String[] getStrings(String propertyName);

    /**
     * Returns the boolean value of a node property
     * @param propertyName
     * @return Boolean value of the node property. If the property does not exist, or is multivalued, or is not of type boolean, false is returned
     */
    public Boolean getBoolean(String propertyName);
    
    /**
     * Returns boolean array of the boolean values of a node property
     * @param propertyName
     * @return Boolean[] values of the node property. If the property does not exist, or is single-valued, or is not of type boolean, a boolean array with all 'false' values is returned
     */
    public Boolean[] getBooleans(String propertyName);
    
    /**
     * Returns the long value of a node property.
     * @param propertyName
     * @return Long value of the node property. If the property does not exist, or is multivalued, or is not of type long, 0 is returned
     */
    public Long getLong(String propertyName);
    
    /**
     * Returns long array of the long values of a node property.  
     * @param propertyName
     * @return Long array presentation of the node property. If the property does not exist, or is single-valued, or is not of type long, an array of 0's is returned
     */
    public Long[] getLongs(String propertyName);

    /**
     * Returns the double value of a node property.
     * @param propertyName
     * @return Double value of the node property. If the property does not exist, or is multivalued, or is not of type double, 0 is returned
     */
    public Double getDouble(String propertyName);
    /**
     * Returns double array of the long values of a node property.  
     * @param propertyName
     * @return Double array presentation of the node property. If the property does not exist, or is single-valued, or is not of type double, an array of 0's is returned
     */
    public Double[] getDoubles(String propertyName);

    /**
     * Returns the Calendar value of a node property.
     * @param propertyName
     * @return Calendar value of the node property. If the property does not exist, or is multivalued, or is not of type jcr DATE, null is returned
     */
    public Calendar getDate(String propertyName);
    
    /**
     * Returns Calendar array of the Calendar values of a node property.
     * @param propertyName
     * @return Calendar[] of the Calendar values of the node property. If the property does not exist, or is single-valued, or is not of type jcr DATE, an array of nulls is returned
     */
    public Calendar[] getDates(String propertyName);
    
    /**
     * Returns all combined properties that are of type String, boolean, int, long, double or Calendar in a Map. 
     */
    public Map<String, Object> getProperties();
}
