// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.DiagnosticLocale;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.data.PropertyValueTable;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.AvailableCallback;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.PopulateCallback;

public class VMDetailsProvider implements DataProvider, PopulateCallback, AvailableCallback {
    private PropertyValueTable table;
    private PropertyValueTable tableNOLOC;

    @Override
    public Object getData() {
        return table;
    }

    @Override
    public Object getDataNOLOC() {
        return tableNOLOC;
    }

    @Override
    public boolean isAvailable() {
        return table != null && tableNOLOC != null;
    }

    @Override
    public void populate() throws Exception {
        table = populate(DiagnosticLocale.USER_LOCALE);
        tableNOLOC = populate(DiagnosticLocale.SUPPORT_LOCALE);
    }

    private static PropertyValueTable populate(final Locale locale) throws Exception {
        PropertyValueTable table = new PropertyValueTable(locale);

        try {
            final Object runtimeMxBean = Class.forName("java.lang.management.ManagementFactory").getDeclaredMethod( //$NON-NLS-1$
                "getRuntimeMXBean", //$NON-NLS-1$
                (Class[]) null).invoke(null, (Object[]) null);

            final String name = (String) Class.forName("java.lang.management.RuntimeMXBean").getDeclaredMethod( //$NON-NLS-1$
                "getName", //$NON-NLS-1$
                (Class[]) null).invoke(runtimeMxBean, (Object[]) null);

            final Long uptime = (Long) Class.forName("java.lang.management.RuntimeMXBean").getDeclaredMethod( //$NON-NLS-1$
                "getUptime", //$NON-NLS-1$
                (Class[]) null).invoke(runtimeMxBean, (Object[]) null);

            final Long startTimeMs = (Long) Class.forName("java.lang.management.RuntimeMXBean").getDeclaredMethod( //$NON-NLS-1$
                "getStartTime", //$NON-NLS-1$
                (Class[]) null).invoke(runtimeMxBean, (Object[]) null);

            @SuppressWarnings("unchecked")
            final List<String> inputArguments =
                (List<String>) Class.forName("java.lang.management.RuntimeMXBean").getDeclaredMethod( //$NON-NLS-1$
                    "getInputArguments", //$NON-NLS-1$
                    (Class[]) null).invoke(runtimeMxBean, (Object[]) null);

            final Date startTime = new Date(startTimeMs.longValue());

            table.addProperty(Messages.getString("VMDetailsProvider.NameProperty", locale), name); //$NON-NLS-1$
            table.addProperty(Messages.getString("VMDetailsProvider.UptimeProperty", locale), String.valueOf(uptime)); //$NON-NLS-1$
            table.addProperty(Messages.getString("VMDetailsProvider.StartTimeProperty", locale), startTime); //$NON-NLS-1$

            if (inputArguments.size() > 0) {
                int ix = 0;
                for (final Iterator<String> it = inputArguments.iterator(); it.hasNext();) {
                    final String arg = it.next();

                    String argIx = String.valueOf(ix);
                    if (argIx.length() == 1) {
                        argIx = "0" + argIx; //$NON-NLS-1$
                    }

                    final String messageFormat =
                        //@formatter:off
                        Messages.getString("VMDetailsProvider.InputArgumentPropertyFormat", locale); //$NON-NLS-1$
                        //@formatter:on
                    final String message = MessageFormat.format(messageFormat, argIx);
                    table.addProperty(message, arg);
                    ++ix;
                }
            }
        } catch (final Throwable t) {
            table = null;
            /*
             * ignore!
             */
        }

        return table;
    }
}
