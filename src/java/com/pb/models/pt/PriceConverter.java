package com.pb.models.pt;

import com.pb.common.matrix.Matrix;
import com.pb.common.util.ResourceUtil;

import java.util.*;

/**
 * The {@code PriceConverter} class provides a centralized location for converting price values from one year base to another.
 * It uses the internally held {@code ConversionType} enum to define the conversions available.
 *
 * @author crf
 *         started Oct 14, 2010 7:23:19 AM
 */
public class PriceConverter {

    /**
     * The {@code ConversionType} enum defines the conversions available to the {@code PriceConverter} class.
     * <p>
     * Note that the current structure of the pt code means that this class should be initialized (with app and global
     * resource bundles) as soon as possible to avoid unintialized errors being thrown.
     */
    public static enum ConversionType {
        /**
         * Indicates a straight price conversion (probably read from a parameter or property file).
         */
        PRICE("pt.price.to.2000$.conversion.factor"),
        /**
         * Indicates an income conversion (probably from a synthesized population).
         */
        INCOME("pt.income.to.2000$.conversion.factor"),
        /**
         * Indicates a skim matrix conversion (probably from an assignment module).
         */
        SKIM("pt.skim.to.2000$.conversion.factor")
        ;

        private final String propertyFileKey;
        private ConversionType(String propertyFileKey) {
            this.propertyFileKey = propertyFileKey;
        }

        /**
         * Get the property key associated with this conversion type.
         *
         * @return this conversion type's property key.
         */
        public String getPropertyKey() {
            return propertyFileKey;
        }
    }

    private static volatile PriceConverter instance = null; //singleton, will be lazily loaded

    /**
     * Get a {@code PriceConverter} instance. If one has already been created, then that one will be returned, otherwise
     * a new instance will be constructed and returned.  If a conversion type's key is not found in any of the passed-in
     * bundles, then that conversion type's factor will be set to <code>1.0</code>.
     * <p>
     * Note that the current structure of the pt code means that this class should be initialized (with app and global
     * resource bundles) as soon as possible to avoid unintialized errors being thrown.
     *
     * @param bundles
     *        The resource bundles in which the property keys for the {@code ConversionType}s may be found.
     *
     * @return the price converter instance.
     */
    synchronized public static PriceConverter getInstance(ResourceBundle ... bundles) {
        //don't know if it needs to be synchronized, but this won't be a performance bottleneck
        //so set to one thread at a time access to make sure lazy loading is correctly performed
        if (instance == null)
            instance = new PriceConverter(bundles);
        return instance;
    }

    private final Map<ConversionType,Double> conversionMap;
    private final Set<ConversionType> conversionUnnecessarySet;

    private PriceConverter(ResourceBundle ... bundles) {
        if (bundles.length == 0)
            throw new IllegalArgumentException("PriceConverter must be initialized with resource bundle(s).");
        conversionMap = new EnumMap<ConversionType,Double>(ConversionType.class);
        conversionUnnecessarySet = EnumSet.noneOf(ConversionType.class);

        for (ConversionType type : ConversionType.values()) {
            for (ResourceBundle bundle : bundles) {
                String value = ResourceUtil.getProperty(bundle,type.propertyFileKey);
                if (value != null) {
                    conversionMap.put(type,Double.parseDouble(value));
                    break;
                }
            }
            if (!conversionMap.containsKey(type))
                conversionMap.put(type,1.0);
            if (conversionMap.get(type) == 1.0) //if 1.0, then no conversion needed, so add to unnecessary set
                conversionUnnecessarySet.add(type);
        }
    }

    /**
     * Get the conversion factor associated with the specified type.
     *
     * @param type
     *        The conversion type in question.
     *
     * @return the conversion factor for {@code type}.
     */
    public double getConversionFactor(ConversionType type) {
        return conversionMap.get(type);
    }

    /**
     * Convert a {@code double} price.
     *
     * @param price
     *        The price to convert.
     *
     * @param type
     *        The type of conversion to perform.
     *
     * @return the converted value of {@code price} for {@code type}.
     */
    public double convertPrice(double price, ConversionType type) {
        if (conversionUnnecessarySet.contains(type))
            return price;
        return price*conversionMap.get(type);
    }

    /**
     * Convert a {@code float} price.
     *
     * @param price
     *        The price to convert.
     *
     * @param type
     *        The type of conversion to perform.
     *
     * @return the converted value of {@code price} for {@code type}.
     */
    public float convertPrice(float price, ConversionType type) {
        if (conversionUnnecessarySet.contains(type))
            return price;
        return price*conversionMap.get(type).floatValue();
    }

    /**
     * Convert an {@code int} price.
     *
     * @param price
     *        The price to convert.
     *
     * @param type
     *        The type of conversion to perform.
     *
     * @return the converted value of {@code price} for {@code type}.
     */
    public int convertPrice(int price, ConversionType type) {
        if (conversionUnnecessarySet.contains(type))
            return price;
        return (int) Math.round(price*conversionMap.get(type));
    }

    /**
     * Convert a {@code Matrix} of prices.  This converts the matrix in place (if necessary), and returns that matrix.
     *
     * @param matrix
     *        The matrix to convert.
     *
     * @param type
     *        The type of conversion to perform.
     *
     * @return {@code matrix}, which will have been converted for {@code type}.
     */
    public Matrix convertMatrix(Matrix matrix, ConversionType type) {
        if (!conversionUnnecessarySet.contains(type))
            matrix.scale(conversionMap.get(type).floatValue());
        return matrix;
    }
}
