// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.datetime.LenientDateTimeParser;

import ms.tfs.versioncontrol.clientservices._03._DateVersionSpec;

/**
 * Represents a date used to select a {@link Changeset} by.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class DateVersionSpec extends VersionSpec {
    /**
     * Returns the single character identifier for the type of VersionSpec
     * implemented by this class.
     */
    protected static final char IDENTIFIER = 'D';

    /**
     * TODO Cache this in a smarter way that will allow for locale-specific core
     * instances.
     */
    private final static LenientDateTimeParser parser = new LenientDateTimeParser();

    public DateVersionSpec(final _DateVersionSpec spec) {
        super(spec);
    }

    public DateVersionSpec(final Calendar calendar) {
        super(new _DateVersionSpec(calendar, null));
    }

    /**
     * Creates a {@link DateVersionSpec} from a date and time string. Both date
     * and time components will default to current time if not specified in ths
     * given string.
     *
     * @throws ParseException
     *         if the date/time string could not be parsed.
     */
    public DateVersionSpec(final String dateTimeString) throws ParseException {
        super(new _DateVersionSpec(parser.parse(dateTimeString, true, true), null));
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String dateString = "<null>"; //$NON-NLS-1$

        final _DateVersionSpec spec = (_DateVersionSpec) getWebServiceObject();
        if (spec.getDate() != null) {
            dateString = SimpleDateFormat.getDateTimeInstance().format(spec.getDate().getTime());
        }

        return IDENTIFIER + dateString;
    }

    /**
     * @return the date.
     */
    public Calendar getDate() {
        return ((_DateVersionSpec) getWebServiceObject()).getDate();
    }
}
