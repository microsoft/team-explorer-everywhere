// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

/**
 * An internal WIT class used to convert values between types. Obtain instances
 * of this class by using WITypeConverterFactory.
 *
 * @see WITypeConverterFactory
 */
public interface WITypeConverter {
    /**
     * <p>
     * Converts the given input object to an object of the proper type for this
     * WITypeConverter (possibly the same type as the input object). See the
     * comments on WIValueSource for assumptions that implementors are allowed
     * to make about the type of the input value. The input value is allowed to
     * be null, in which case most implementors will return null as the
     * translated value.
     * </p>
     * <p>
     * If any errors occur during the conversion, the implementor must throw a
     * WITypeConverterException.
     * </p>
     *
     * @param input
     *        an input value to translate, as described above
     * @param valueSource
     *        an enum indicating the source of the input value
     * @return the translated input value
     * @throws WITypeConverterException
     */
    public Object translate(Object input, WIValueSource valueSource) throws WITypeConverterException;

    /**
     * Currently used to translate a strongly typed field value into a String to
     * be sent to the TFS server. If this method is ever used for other purposes
     * (eg to format values as Strings to display to the user) it will need to
     * be made more flexible.
     *
     * @param data
     *        the value to be formatted
     * @return character data appropriate to send to the server as a field value
     */
    public String toString(Object data);
}
