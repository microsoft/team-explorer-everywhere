// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class DefaultTimeZoneProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale displayLocale) {
        final Properties properties = new Properties();

        final TimeZone timeZone = TimeZone.getDefault();
        final Date now = new Date();

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.TimeZoneClassProperty", displayLocale), //$NON-NLS-1$
            timeZone.getClass().getName());

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.DSTSavingProperty", displayLocale), //$NON-NLS-1$
            String.valueOf(timeZone.getDSTSavings()));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.IDProperty", displayLocale), //$NON-NLS-1$
            timeZone.getID());

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.RawOffsetProperty", displayLocale), //$NON-NLS-1$
            String.valueOf(timeZone.getRawOffset()));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.CurrentOffsetProperty", displayLocale), //$NON-NLS-1$
            String.valueOf(timeZone.getOffset(now.getTime())));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.UsesDSTProperty", displayLocale), //$NON-NLS-1$
            String.valueOf(timeZone.useDaylightTime()));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.CurrentlyInDSTProperty", displayLocale), //$NON-NLS-1$
            String.valueOf(timeZone.inDaylightTime(now)));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.DispNameLongDSTPropery", displayLocale), //$NON-NLS-1$
            timeZone.getDisplayName(true, TimeZone.LONG, displayLocale));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.DispNameLongNoDSTProperty", displayLocale), //$NON-NLS-1$
            timeZone.getDisplayName(false, TimeZone.LONG, displayLocale));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.DispNameShortDSTProperty", displayLocale), //$NON-NLS-1$
            timeZone.getDisplayName(true, TimeZone.SHORT, displayLocale));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.DispNameShortNoDSPProperty", displayLocale), //$NON-NLS-1$
            timeZone.getDisplayName(false, TimeZone.SHORT, displayLocale));

        properties.setProperty(
            Messages.getString("DefaultTimeZoneProvider.TimeZone", displayLocale), //$NON-NLS-1$
            timeZone.toString());

        return properties;
    }
}
