// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemConstants;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.DateTime.UncheckedParseException;

public class WIDateTypeConverter implements WITypeConverter {
    @Override
    public Object translate(final Object input, final WIValueSource valueSource) throws WITypeConverterException {
        if (input == null) {
            return null;
        }

        if (input instanceof Date) {
            return input;
        }

        if (input instanceof String) {
            final String valueToParse = ((String) input).trim();

            if (valueSource == WIValueSource.SERVER) {
                if (InternalWorkItemConstants.NULL_DATE_STRING.equals(valueToParse)) {
                    return null;
                }

                try {
                    /*
                     * Since the WIValueSource is SERVER, we only need to worry
                     * about a single date format. All dates sent by the server
                     * come over in this format.
                     */
                    final SimpleDateFormat format = InternalWorkItemUtils.newMetadataDateFormat();
                    return format.parse(valueToParse);
                } catch (final ParseException ex) {
                    throw new WITypeConverterException(
                        MessageFormat.format(
                            "unable to convert value [{0}] to a Date (SERVER value)", //$NON-NLS-1$
                            valueToParse),
                        ex);
                }
            } else {
                if (valueToParse.length() == 0) {
                    return null;
                }

                try {
                    /*
                     * I18N: need to use a specified Locale (and possibly
                     * TimeZone), not the default
                     */
                    final Date date = DateTime.parse(valueToParse, Locale.getDefault(), TimeZone.getDefault());

                    /*
                     * Need to range-check the date. This is what Visual
                     * Studio's client does - it seems that if the year is less
                     * than 1753 or greater than 9999, the field is set invalid
                     * with INVALID_DATE.
                     */
                    final Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    final int year = cal.get(Calendar.YEAR);
                    if (year < 1753 || year > 9999) {
                        throw new WITypeConverterException(MessageFormat.format(
                            "The date is out of range (year={0})", //$NON-NLS-1$
                            year), FieldStatus.INVALID_DATE, date);
                    }

                    return date;
                } catch (final UncheckedParseException ex) {
                    throw new WITypeConverterException(
                        MessageFormat.format(
                            "unable to convert value [{0}] to a Date (LOCAL value)", //$NON-NLS-1$
                            valueToParse),
                        ex);
                }
            }
        }

        throw new WITypeConverterException(MessageFormat.format(
            "unable to convert [{0}] to a Date", //$NON-NLS-1$
            input.getClass().getName()));
    }

    @Override
    public String toString(final Object data) {
        if (data == null) {
            return null;
        }

        final SimpleDateFormat format = InternalWorkItemUtils.newMetadataDateFormat();
        return format.format((Date) data);
    }
}
