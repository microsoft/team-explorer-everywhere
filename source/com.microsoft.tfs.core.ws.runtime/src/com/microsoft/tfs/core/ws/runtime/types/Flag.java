// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import java.text.MessageFormat;
import java.util.Map;

import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPSerializationException;

public abstract class Flag {
    protected final String name;

    protected Flag(final String name, final Map valuesToInstances) {
        this.name = name;

        valuesToInstances.put(name, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof Flag == false) {
            return false;
        }

        return ((Flag) obj).name.equals(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets the apprpriate flag instance for the given string value.
     *
     * @param value
     *        the string value (not null)
     * @param valuesToInstances
     *        a map to use to find the instance for the given value
     * @return the appropriate flag instance.
     * @throws SOAPSerializationException
     *         if there was no matching flag instance.
     */
    public static Flag fromString(final String value, final Map valuesToInstances) throws SOAPSerializationException {
        final Flag ret = (Flag) valuesToInstances.get(value);

        if (ret == null) {
            final String messageFormat = "No flag matches the attribute value {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, value);
            throw new SOAPSerializationException(message);
        }

        return ret;
    }
}
