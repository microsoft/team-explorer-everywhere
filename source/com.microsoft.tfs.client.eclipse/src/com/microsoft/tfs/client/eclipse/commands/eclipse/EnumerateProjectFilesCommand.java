// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.commands.eclipse;

import java.text.MessageFormat;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.CompositeResourceFilterType;
import com.microsoft.tfs.client.common.framework.resources.filter.FilteredResourceCollector;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public class EnumerateProjectFilesCommand extends TFSCommand {
    private final IProject project;
    private String[] paths;

    private boolean includeProject = true;
    private boolean includeFolders = true;

    public EnumerateProjectFilesCommand(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        this.project = project;
    }

    @Override
    public String getName() {
        return (MessageFormat.format(
            Messages.getString("EnumerateProjectFilesCommand.CommandTextFormat"), //$NON-NLS-1$
            project.getName()));
    }

    @Override
    public String getErrorDescription() {
        return (MessageFormat.format(
            Messages.getString("EnumerateProjectFilesCommand.ErrorTextFormat"), //$NON-NLS-1$
            project.getName()));
    }

    @Override
    public String getLoggingDescription() {
        return (MessageFormat.format(
            Messages.getString("EnumerateProjectFilesCommand.CommandTextFormat", LocaleUtil.ROOT), //$NON-NLS-1$
            project.getName()));
    }

    public void setIncludeProject(final boolean includeProject) {
        this.includeProject = includeProject;
    }

    public void setIncludeFolders(final boolean includeFolders) {
        this.includeFolders = includeFolders;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        progressMonitor.beginTask(
            MessageFormat.format(
                Messages.getString("EnumerateProjectFilesCommand.EnumeratingFilesInProjectFormat"), //$NON-NLS-1$
                project.getName()),
            IProgressMonitor.UNKNOWN);

        final FilteredResourceCollector collector = new FilteredResourceCollector(
            getProjectFilesEnumerationFilter(),
            ResourceFilter.FILTER_FLAG_TREE_OPTIMIZATION,
            progressMonitor);

        project.accept(collector);

        paths = Resources.getLocations(collector.getResources(), LocationUnavailablePolicy.IGNORE_RESOURCE);

        return Status.OK_STATUS;
    }

    private ResourceFilter getProjectFilesEnumerationFilter() {
        final CompositeResourceFilter.Builder builder =
            new CompositeResourceFilter.Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT);

        builder.addFilter(PluginResourceFilters.STANDARD_FILTER);
        builder.addFilter(new EnumerateProjectFilesFilter());

        return builder.build();
    }

    public String[] getPaths() {
        return paths;
    }

    private class EnumerateProjectFilesFilter extends ResourceFilter {
        @Override
        public ResourceFilterResult filter(final IResource resource, final int flags) {
            int resultFlags;

            /* Always accept shared files */
            if (resource.getType() == IResource.FILE) {
                resultFlags = RESULT_FLAG_ACCEPT;
            } else if (resource.getType() == IResource.FOLDER) {
                resultFlags = includeFolders ? RESULT_FLAG_ACCEPT : RESULT_FLAG_REJECT;
            } else if (resource.getType() == IResource.PROJECT) {
                resultFlags = includeProject ? RESULT_FLAG_ACCEPT : RESULT_FLAG_REJECT;
            } else {
                resultFlags = RESULT_FLAG_REJECT;
            }

            return ResourceFilterResult.getInstance(resultFlags);
        }
    }
}