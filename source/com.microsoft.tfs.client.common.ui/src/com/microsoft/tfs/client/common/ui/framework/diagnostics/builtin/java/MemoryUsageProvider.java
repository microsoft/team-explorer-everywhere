// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.util.Locale;
import java.util.Properties;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class MemoryUsageProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    public Object getData(final Locale locale) {
        final Properties properties = new Properties();

        final long maxMemory = Runtime.getRuntime().maxMemory();
        final long freeMemory = Runtime.getRuntime().freeMemory();
        final long totalMemory = Runtime.getRuntime().totalMemory();

        properties.setProperty(
            Messages.getString("MemoryUsageProvider.MaxMemoryProperty", locale), //$NON-NLS-1$
            String.valueOf(maxMemory));
        properties.setProperty(
            Messages.getString("MemoryUsageProvider.TotalMemoryProperty", locale), //$NON-NLS-1$
            String.valueOf(totalMemory));
        properties.setProperty(
            Messages.getString("MemoryUsageProvider.FreeMemoryProperty", locale), //$NON-NLS-1$
            String.valueOf(freeMemory));

        /*
         * try to use the Java5 JMX memory management interfaces, as the
         * information is much better than Runtime.*Memory() methods
         *
         * we ignore any error (which would indicate non-Java 5 JRE)
         */
        try {
            final Object memoryMxBean = Class.forName("java.lang.management.ManagementFactory").getDeclaredMethod( //$NON-NLS-1$
                "getMemoryMXBean", //$NON-NLS-1$
                (Class[]) null).invoke(null, (Object[]) null);

            final Object heapMemoryUsage = Class.forName("java.lang.management.MemoryMXBean").getDeclaredMethod( //$NON-NLS-1$
                "getHeapMemoryUsage", //$NON-NLS-1$
                (Class[]) null).invoke(memoryMxBean, (Object[]) null);

            final Object nonHeapMemoryUsage = Class.forName("java.lang.management.MemoryMXBean").getDeclaredMethod( //$NON-NLS-1$
                "getNonHeapMemoryUsage", //$NON-NLS-1$
                (Class[]) null).invoke(memoryMxBean, (Object[]) null);

            final Long heapInit = (Long) heapMemoryUsage.getClass().getDeclaredMethod("getInit", (Class[]) null).invoke( //$NON-NLS-1$
                heapMemoryUsage,
                (Object[]) null);

            final Long heapUsed = (Long) heapMemoryUsage.getClass().getDeclaredMethod("getUsed", (Class[]) null).invoke( //$NON-NLS-1$
                heapMemoryUsage,
                (Object[]) null);

            final Long heapCommitted =
                (Long) heapMemoryUsage.getClass().getDeclaredMethod("getCommitted", (Class[]) null).invoke( //$NON-NLS-1$
                    heapMemoryUsage,
                    (Object[]) null);

            final Long heapMax = (Long) heapMemoryUsage.getClass().getDeclaredMethod("getMax", (Class[]) null).invoke( //$NON-NLS-1$
                heapMemoryUsage,
                (Object[]) null);

            final Long nonHeapInit =
                (Long) nonHeapMemoryUsage.getClass().getDeclaredMethod("getInit", (Class[]) null).invoke( //$NON-NLS-1$
                    nonHeapMemoryUsage,
                    (Object[]) null);

            final Long nonHeapUsed =
                (Long) nonHeapMemoryUsage.getClass().getDeclaredMethod("getUsed", (Class[]) null).invoke( //$NON-NLS-1$
                    nonHeapMemoryUsage,
                    (Object[]) null);

            final Long nonHeapCommitted = (Long) nonHeapMemoryUsage.getClass().getDeclaredMethod(
                "getCommitted", //$NON-NLS-1$
                (Class[]) null).invoke(nonHeapMemoryUsage, (Object[]) null);

            final Long nonHeapMax =
                (Long) nonHeapMemoryUsage.getClass().getDeclaredMethod("getMax", (Class[]) null).invoke( //$NON-NLS-1$
                    nonHeapMemoryUsage,
                    (Object[]) null);

            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5HeapInitProperty", locale), //$NON-NLS-1$
                String.valueOf(heapInit));
            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5HeapUsedProperty", locale), //$NON-NLS-1$
                String.valueOf(heapUsed));
            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5HeapComittedProperty", locale), //$NON-NLS-1$
                String.valueOf(heapCommitted));
            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5HeapMaxProperty", locale), //$NON-NLS-1$
                String.valueOf(heapMax));

            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5NonHeapInitProperty", locale), //$NON-NLS-1$
                String.valueOf(nonHeapInit));
            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5NonHeapUsedProperty", locale), //$NON-NLS-1$
                String.valueOf(nonHeapUsed));
            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5NonHeapCommitedProperty", locale), //$NON-NLS-1$
                String.valueOf(nonHeapCommitted));
            properties.setProperty(
                Messages.getString("MemoryUsageProvider.Java5NonHeapMaxProperty", locale), //$NON-NLS-1$
                String.valueOf(nonHeapMax));
        } catch (final Throwable t) {
            /*
             * ignore!
             */
        }

        return properties;
    }
}
