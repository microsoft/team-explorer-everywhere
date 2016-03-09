// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.datetime;

import java.text.DateFormat;

/**
 *         Contains a SimpleDateFormat instance for an already-expanded pattern
 *         string with custom equals and hashCode implementations for uset in a
 *         Set. Also contains some other attributes for later decision-making.
 *         <p>
 *         Instances of this class are what the parser matches a given string
 *         against.
 *         <p>
 *         This class is immutable (and therefore thread-safe).
 */
class LenientDateTimeFormat {
    private final DateFormat df;
    private final int creationIndex;
    private final boolean specifiesDate;
    private final boolean specifiesTime;

    /**
     * An existing SimpleDateFormat with original order.
     *
     * @param format
     *        the SimpleDateFormat instance.
     * @param creationIndex
     *        an integer describing the original order of the pattern this
     *        object wraps in the array that defined the patterns. Used so these
     *        formats can later be sorted back to their original order.
     * @param specifiesDate
     *        true if this format specifies a date, false if it does not.
     * @param specifiesTime
     *        true if this format specifies a time, false if it does not.
     */
    public LenientDateTimeFormat(
        final DateFormat format,
        final int creationIndex,
        final boolean specifiesDate,
        final boolean specifiesTime) {
        df = format;
        this.creationIndex = creationIndex;
        this.specifiesDate = specifiesDate;
        this.specifiesTime = specifiesTime;
    }

    /**
     * @return the simple date format.
     */
    public DateFormat getDateFormat() {
        return df;
    }

    public int getCreationIndex() {
        return creationIndex;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ((obj instanceof LenientDateTimeFormat) == false) {
            return false;
        }

        final LenientDateTimeFormat other = (LenientDateTimeFormat) obj;

        if (df != null) {
            if (other.df == null) {
                return false;
            } else {
                return df.equals(((LenientDateTimeFormat) obj).df);
            }
        } else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if (df != null) {
            return df.hashCode();
        } else {
            return 0;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return df.toString();
    }

    public boolean specifiesDate() {
        return specifiesDate;
    }

    public boolean specifiesTime() {
        return specifiesTime;
    }
}
