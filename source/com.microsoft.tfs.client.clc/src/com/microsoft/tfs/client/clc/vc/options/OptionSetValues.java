// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.SingleValueOrFileOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.util.IOUtils;

/**
 * Sets values for TFS 2012 named properties.
 */
public final class OptionSetValues extends SingleValueOrFileOption {
    private final List<PropertyValue> properties = new ArrayList<PropertyValue>();

    public OptionSetValues() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        properties.clear();

        /*
         * The super class handles the case where @valuefile is the only option,
         * reading it as a text file.
         */
        super.parseValues(optionValueString);

        String propertiesString = super.getValue().trim();

        if (propertiesString.length() == 0) {
            return;
        }

        // Parse the properties from lines consisting of either
        // name 1=value;name 2=value
        // or
        // name 1=value;
        // name 2=value line 1
        // value line 2;
        // Each property field/value pair is terminated by a semicolon.
        // Escape sequences are "==" for '=' in a field name and ";;" for ';' in
        // a value.
        final AtomicReference<String> value = new AtomicReference<String>();
        value.set(""); //$NON-NLS-1$
        do {
            final AtomicReference<String> name = new AtomicReference<String>();

            propertiesString = parseString(propertiesString, '=', name);
            if (name.get() == null || name.get().length() == 0) {
                throw new InvalidOptionValueException(Messages.getString("OptionSetValues.PropertyNameEmpty")); //$NON-NLS-1$
            }

            propertiesString = parseString(propertiesString, ';', value);
            if (value.get() == null || value.get().length() == 0) {
                throw new InvalidOptionValueException(
                    MessageFormat.format(
                        Messages.getString("OptionSetValues.PropertyValueEmptyFormat"), //$NON-NLS-1$
                        name.get()));
            }

            if (value.get().charAt(0) == '@') {
                final String path = value.get().substring(1);
                try {
                    properties.add(new PropertyValue(name.get(), IOUtils.toByteArray(new FileInputStream(path))));
                } catch (final FileNotFoundException e) {
                    throw new InvalidOptionValueException(
                        MessageFormat.format(
                            Messages.getString("SingleValueOrFileOption.OptionValueFileNotFoundFormat"), //$NON-NLS-1$
                            path));
                } catch (final IOException e) {
                    throw new InvalidOptionValueException(
                        MessageFormat.format(
                            Messages.getString("SingleValueOrFileOption.ErrorReadingOptionValueFileFormat"), //$NON-NLS-1$
                            path,
                            e.getLocalizedMessage()));
                }
            } else {
                properties.add(new PropertyValue(name.get(), value.get()));
            }
        } while (propertiesString.length() > 0);

    }

    /**
     * This helper method breaks a string at the specified separator. The
     * separator is escaped whenever it appears twice in a row.
     *
     * @param s
     *        the string to parse (must not be <code>null</code>)
     * @param separator
     *        the separator character
     * @param value
     *        the extracted string prior to the separator; the escape sequences
     *        are replaced with the single char; the value does not include the
     *        terminating separator
     * @return the rest of the string; the separator is not included at the
     *         front
     */
    private String parseString(final String s, final char separator, final AtomicReference<String> value) {
        final StringBuilder sb = new StringBuilder(s.length());
        int separatorIndex = -1;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == separator && i + 1 < s.length() && s.charAt(i + 1) == separator) {
                sb.append(separator);
                i++;
                continue;
            }

            if (s.charAt(i) == separator) {
                separatorIndex = i;
                break;
            }

            sb.append(s.charAt(i));
        }

        value.set(sb.toString());

        if (separatorIndex != -1 && separatorIndex != s.length() - 1) {
            return s.substring(separatorIndex + 1);
        } else {
            return ""; //$NON-NLS-1$
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.SingleValueOption#getValue()
     */
    @Override
    public final String getValue() {
        throw new IllegalStateException("OptionSetValues does not support getValue().  Use getValues() instead."); //$NON-NLS-1$
    }

    /**
     * Gets the parsed properties objects.
     *
     * @return the parsed properties objects.
     */
    public List<PropertyValue> getValues() {
        return properties;
    }

    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix()
            + getMatchedAlias()
            + ":@valuefile|name1=value1[;name2=value2;name3=@valuefile;...]"; //$NON-NLS-1$
    }
}
