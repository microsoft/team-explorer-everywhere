// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.tasks.vc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.tasks.BaseTask;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resource.TPIgnoreResourcesFilter;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreCache;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnorePatternComparator;
import com.microsoft.tfs.client.eclipse.ui.commands.vc.RemoveTPIgnorePatternsCommand;
import com.microsoft.tfs.client.eclipse.ui.dialogs.vc.TPIgnorePatternsResolutionDialog;
import com.microsoft.tfs.util.Check;

public class TPUnignoreTask extends BaseTask {
    private final IResource[] resources;

    /**
     * Removes .tpignore file entries which match the given resources, prompting
     * for confirmation when there are multiple or inexact matches. Resources
     * must all share the same {@link IProject}.
     *
     * @param shell
     *        a shell to use for raising warnings and errors (must not be
     *        <code>null</code>)
     * @param resources
     *        the resources to remove matching entries for (must not be
     *        <code>null</code> or empty, must all share the same
     *        {@link IProject})
     */
    public TPUnignoreTask(final Shell shell, final IResource[] resources) {
        super(shell);

        Check.notNullOrEmpty(resources, "resources"); //$NON-NLS-1$

        this.resources = resources;

        /*
         * Using a job command executor is important for a specific reason: the
         * ignore command we run changes workspace resources, which can cause
         * resource change listeners (from our plug-in or others) to do
         * complicated things which might raise an error dialog. If we use the
         * default UI command executor instead of a job executor, this task
         * might raise a conflicting progress dialog while that resource change
         * listener is showing an error dialog, causing a live lock of the UI on
         * some platforms (user can't dismiss either dialog).
         */
        setCommandExecutor(
            UICommandExecutorFactory.newUIJobCommandExecutor(getShell(), new JobOptions().setUser(true)));
    }

    @Override
    public IStatus run() {
        final TPIgnoreCache cache =
            ((TPIgnoreResourcesFilter) PluginResourceFilters.TPIGNORE_FILTER).getIgnorableResourcesCache();

        /*
         * Because we handle selections of multiple resources, and each resource
         * can match multiple patterns, and some of those patterns may be common
         * to multiple resources, we have to keep track of the multiple match
         * cases to present the user a confirmation when the decisions aren't
         * simple. Sometimes the operation is simple (zero or one pattern
         * matches every resource) and we don't show confirmation UI in that
         * case.
         *
         * Use a custom comparator to compute equal patterns (we don't want
         * duplicates in the dialog).
         */
        final TPIgnorePatternComparator comparator = new TPIgnorePatternComparator();
        final Map<Pattern, Set<IResource>> patternsToMatchedResources =
            new TreeMap<Pattern, Set<IResource>>(comparator);
        final Set<Pattern> allMatchingPatterns = new TreeSet<Pattern>(comparator);

        boolean anyResourceMatchedMultiplePatterns = false;
        boolean anyPatternMatchedMultipleResources = false;
        boolean anyPatternMatchedIndirectly = false;

        for (final IResource resource : resources) {
            final Pattern[] matchingPatterns = cache.getMatchingPatterns(resource);

            /*
             * Omit entries for resources which did not match any patterns.
             */
            if (matchingPatterns.length > 0) {
                if (matchingPatterns.length > 1) {
                    anyResourceMatchedMultiplePatterns = true;
                }

                // Update lists of resources for each pattern
                for (final Pattern pattern : matchingPatterns) {
                    allMatchingPatterns.add(pattern);

                    // See if this was an indirect match
                    if (pattern.pattern().trim().equals(
                        TPIgnoreCache.createIgnorePatternForResource(resource).trim()) == false) {
                        anyPatternMatchedIndirectly = true;
                    }

                    // Update the resource set
                    Set<IResource> resourceSet = patternsToMatchedResources.get(pattern);

                    if (resourceSet == null) {
                        resourceSet = new HashSet<IResource>();
                        patternsToMatchedResources.put(pattern, resourceSet);
                    }

                    resourceSet.add(resource);

                    if (resourceSet.size() > 1) {
                        anyPatternMatchedMultipleResources = true;
                    }
                }
            }
        }

        /*
         * If there were resources with any matching patterns.
         */
        if (patternsToMatchedResources.size() > 0) {
            /*
             * Since this task's resources must all be from one project, simply
             * get the .tpignore file for the first resource.
             */
            final IFile ignoreFile = TPIgnoreCache.getIgnoreFile(resources[0]);

            final Set<Pattern> patternsToRemove = new HashSet<Pattern>();

            if (anyPatternMatchedIndirectly
                || anyResourceMatchedMultiplePatterns
                || anyPatternMatchedMultipleResources) {
                /*
                 * 1:*, *:1, or *:* with patterns and resources, or indirect
                 * matches. Ask the user to choose the patterns to remove.
                 */

                final TPIgnorePatternsResolutionDialog dialog =
                    new TPIgnorePatternsResolutionDialog(getShell(), patternsToMatchedResources);

                if (dialog.open() == IDialogConstants.OK_ID) {
                    patternsToRemove.addAll(Arrays.asList(dialog.getCheckedPatterns()));
                }
            } else {
                /*
                 * Simple case: only one pattern per resource. Remove each
                 * pattern from the ignore file.
                 */
                patternsToRemove.addAll(allMatchingPatterns);
            }

            if (!patternsToRemove.isEmpty()) {
                return getCommandExecutor().execute(
                    new RemoveTPIgnorePatternsCommand(ignoreFile, TPIgnoreCache.getPatternStrings(patternsToRemove)));
            }

        }

        return Status.OK_STATUS;
    }
}
