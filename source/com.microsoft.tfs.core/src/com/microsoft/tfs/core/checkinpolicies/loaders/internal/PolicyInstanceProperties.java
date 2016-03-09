// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies.loaders.internal;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Holds information parsed from a checkin policy properties file (standard Java
 * properties format). Contains a static method to parse these files (
 * {@link #load(InputStream)}).
 * </p>
 *
 * @threadsafety immutable
 */
public class PolicyInstanceProperties {
    private final static String ID_PROPERTY = "id"; //$NON-NLS-1$
    private final static String CLASS_NAME_PROPERTY = "class-name"; //$NON-NLS-1$

    private final String id;
    private final String className;

    /**
     * Creates a {@link PolicyInstanceProperties} for the given policy ID and
     * implementation class name.
     *
     * @param id
     *        the policy id (must not be <code>null</code>)
     * @param className
     *        the class name (must not be <code>null</code> or empty)
     */
    public PolicyInstanceProperties(final String id, final String className) {
        Check.notNull(id, "id"); //$NON-NLS-1$
        Check.notNull(className, "className"); //$NON-NLS-1$

        this.id = id;
        this.className = className;
    }

    /**
     * @return the policy type ID.
     */
    public String getID() {
        return id;
    }

    /**
     * @return the class name that implements this policy.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Loads properties information from the given stream.
     *
     * @param inputStream
     *        a stream opened to a properties file (must not be
     *        <code>null</code>)
     * @return a new {@link PolicyInstanceProperties} initialized with the
     *         values from the given stream.
     * @throws PolicyInstancePropertiesParseException
     *         on any load error.
     */
    public static PolicyInstanceProperties load(final InputStream inputStream) {
        Check.notNull(inputStream, "inputStream"); //$NON-NLS-1$

        final Properties p = new Properties();
        try {
            p.load(inputStream);
        } catch (final IOException e) {
            throw new PolicyInstancePropertiesParseException("Could not read the input stream as a properties file", e); //$NON-NLS-1$
        }

        final String id = p.getProperty(ID_PROPERTY);
        if (id == null || id.length() == 0) {
            throw new PolicyInstancePropertiesParseException(
                MessageFormat.format(
                    Messages.getString("PolicyInstanceProperties.IDPropertyNotFoundInInstanceFileFormat"), //$NON-NLS-1$
                    ID_PROPERTY));
        }

        final String className = p.getProperty(CLASS_NAME_PROPERTY);
        if (className == null || className.length() == 0) {
            throw new PolicyInstancePropertiesParseException(
                MessageFormat.format(
                    Messages.getString("PolicyInstanceProperties.ClassNamePropertyNotFoundInInstanceFileFormat"), //$NON-NLS-1$
                    CLASS_NAME_PROPERTY));
        }

        return new PolicyInstanceProperties(id, className);
    }
}
