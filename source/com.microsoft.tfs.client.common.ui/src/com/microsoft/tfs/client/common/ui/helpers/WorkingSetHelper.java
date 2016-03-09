// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkingSet;

import com.microsoft.tfs.util.Check;

public class WorkingSetHelper {
    private static final Log log = LogFactory.getLog(WorkingSetHelper.class);

    public static String getLabel(final IWorkingSet workingSet) {
        Check.notNull(workingSet, "workingSet"); //$NON-NLS-1$

        try {
            final Method getLabelMethod = workingSet.getClass().getMethod("getLabel", new Class[0]); //$NON-NLS-1$
            final Object labelResult = getLabelMethod.invoke(workingSet, new Object[0]);

            if (labelResult != null && labelResult instanceof String) {
                return (String) labelResult;
            }
        } catch (final Exception e) {
            /* Suppress, Eclipse < 3.1 */
        }

        return workingSet.getName();
    }

    public static boolean isAggregateWorkingSet(final IWorkingSet workingSet) {
        Check.notNull(workingSet, "workingSet"); //$NON-NLS-1$

        try {
            final Method isAggregateMethod = workingSet.getClass().getMethod("isAggregateWorkingSet", new Class[0]); //$NON-NLS-1$
            final Object aggregateResult = isAggregateMethod.invoke(workingSet, new Object[0]);

            if (aggregateResult != null && aggregateResult instanceof Boolean) {
                return ((Boolean) aggregateResult).booleanValue();
            }
        } catch (final Exception e) {
            /* Suppress, Eclipse < 3.1 */
        }

        return false;
    }

    public static void addToWorkingSet(final IProject project, final IWorkingSet workingSet) {
        try {
            boolean aggregateWorkingSetAvailable = false;

            try {
                aggregateWorkingSetAvailable = (Class.forName("org.eclipse.ui.IAggregateWorkingSet") != null); //$NON-NLS-1$
            } catch (final Exception e) {
                /* Suppress */
            }

            if (aggregateWorkingSetAvailable && isAggregateWorkingSet(workingSet)) {
                try {
                    final Class aggregateClass = Class.forName("org.eclipse.ui.IAggregateWorkingSet"); //$NON-NLS-1$
                    final Method getComponentsMethod = aggregateClass.getMethod("getComponents", (Class[]) null); //$NON-NLS-1$
                    final IWorkingSet[] components =
                        (IWorkingSet[]) getComponentsMethod.invoke(workingSet, (Object[]) null);

                    for (int i = 0; i < components.length; i++) {
                        addToWorkingSet(project, components[i]);
                    }

                    return;
                } catch (final Throwable t) {
                    final String messageFormat = "Could not add {0} to working set {1}"; //$NON-NLS-1$
                    final String message =
                        MessageFormat.format(messageFormat, project.getName(), WorkingSetHelper.getLabel(workingSet));
                    log.error(message, t);
                }
            }

            final IAdaptable[] setMembers = workingSet.getElements();
            final IAdaptable[] newMembers = new IAdaptable[setMembers.length + 1];

            System.arraycopy(setMembers, 0, newMembers, 0, setMembers.length);

            newMembers[setMembers.length] = project;

            workingSet.setElements(newMembers);
        } catch (final Exception e) {
            final String messageFormat = "Could not add {0} to working set {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, project.getName(), getLabel(workingSet));
            log.error(message, e);
        }
    }

}
