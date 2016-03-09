// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.UIPlugin;
import org.eclipse.ui.part.FileEditorInput;

import com.microsoft.tfs.checkinpolicies.build.BuildPolicy;
import com.microsoft.tfs.checkinpolicies.build.BuildPolicyFailure;
import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.checkinpolicies.build.TFSBuildCheckinPolicyPlugin;
import com.microsoft.tfs.checkinpolicies.build.settings.Area;
import com.microsoft.tfs.checkinpolicies.build.settings.BuildPolicyConfiguration;
import com.microsoft.tfs.checkinpolicies.build.settings.MarkerMatch;
import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckinPendingChanges;
import com.microsoft.tfs.util.Check;

/**
 * A TFS Check-in Policy that ensures the Eclipse workspace builds. The user can
 * configure which kinds of build markers are checked for (JDT, CDT, whatever)
 * and which severities and priorities count as build-breakers (error, warning,
 * info, etc).
 */
public class BuildPolicyUI extends BuildPolicy {
    /**
     * We hook up a listener to catch post-build events and re-evaluate.
     */
    private IResourceChangeListener resourceChangeListener;

    /**
     * Calculated on initialize so we don't run in Explorer (and elsewhere).
     */
    private boolean runningInEclipse;

    /**
     * All policy implementations must include a zero-argument constructor, so
     * they can be dynamically created by the policy framework.
     */
    public BuildPolicyUI() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#initialize(com.
     * microsoft .tfs.core.pendingcheckin.PendingCheckin,
     * com.microsoft.tfs.core.checkinpolicies.PolicyContext)
     */
    @Override
    public void initialize(final PendingCheckin pendingCheckin, final PolicyContext context) {
        super.initialize(pendingCheckin, context);

        if (context.getProperty(PolicyContextKeys.RUNNING_PRODUCT_ECLIPSE_PLUGIN) == null) {
            return;
        } else {
            runningInEclipse = true;
        }

        if (resourceChangeListener == null) {
            resourceChangeListener = new BuildPolicyResourceChangedListener(this);
            ResourcesPlugin.getWorkspace().addResourceChangeListener(
                resourceChangeListener,
                IResourceChangeEvent.POST_BUILD);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyBase#close()
     */
    @Override
    public void close() {
        if (runningInEclipse && resourceChangeListener != null) {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
            resourceChangeListener = null;
        }

        super.close();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#edit(com.microsoft.
     * tfs.core .checkinpolicies.PolicyEditArgs)
     */
    @Override
    public synchronized boolean edit(final PolicyEditArgs policyEditArgs) {
        final Shell shell = (Shell) policyEditArgs.getContext().getProperty(PolicyContextKeys.SWT_SHELL);

        if (shell == null) {
            return false;
        }

        if (policyEditArgs.getContext().getProperty(PolicyContextKeys.RUNNING_PRODUCT_ECLIPSE_PLUGIN) == null) {
            final MessageBox mb = new MessageBox(shell, SWT.OK | SWT.ICON_WARNING);
            mb.setText(Messages.getString("BuildPolicyUI.MessageTitle")); //$NON-NLS-1$
            mb.setMessage(Messages.getString("BuildPolicyUI.MessageText")); //$NON-NLS-1$
            mb.open();
        }

        /*
         * Create a new configuration object.
         */
        final BuildPolicyConfiguration config = getConfiguration();

        final BuildPolicyDialog dialog = new BuildPolicyDialog(shell, config);

        if (dialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        setConfiguration(dialog.getConfiguration());

        return true;
    }

    /**
     * Called by the {@link BuildPolicyResourceChangedListener} when an Eclipse
     * build finishes. Evalutes this policy and fires up an event to the
     * framework so it can repaint.
     */
    public void onBuild() {
        /*
         * We must create our own policy context object for this call, because
         * we should not re-use the one that the framework gave us.
         */
        final PolicyFailure[] failures = evaluate(new PolicyContext());
        firePolicyStateChangedEvent(failures);
    }

    /**
     * Gets the resources that correspond to the given changes at the configured
     * area (file, project, etc.) and puts them in the given list. If the change
     * does not have an Eclipse resource, it is ignored.
     * <p>
     * If the area to check is wider than file (for instance, project), multiple
     * changes in a single project result in one project resource being added to
     * the resource list.
     *
     * @param changes
     *        the changes to read (not null).
     * @return a set of unique resources scoped to the area we check.
     */
    private Set collectUniqueResourcesForChanges(final PendingChange[] changes) {
        Check.notNull(changes, "changes"); //$NON-NLS-1$

        final Set uniqueResources = new HashSet();

        for (int i = 0; i < changes.length; i++) {
            final PendingChange change = changes[i];

            final String localPath = change.getLocalItem();

            if (localPath == null || localPath.length() == 0) {
                continue;
            }

            final IResource resource = getAreaResourceForLocalPath(localPath);

            if (resource != null) {
                uniqueResources.add(resource);
            }
        }

        return uniqueResources;
    }

    /**
     * Gets the resource (file only) at the configured area (file, project,
     * etc.) for the given path. If the input is not a valid file resource, null
     * is returned.
     *
     * @return the resource, or null if there was no resource for the given
     *         path.
     */
    private IResource getAreaResourceForLocalPath(final String localPath) {
        Check.notNullOrEmpty(localPath, "localPath"); //$NON-NLS-1$

        final IResource resource = Resources.getResourceForLocation(localPath, ResourceType.FILE);

        if (resource == null) {
            return null;
        }

        final BuildPolicyConfiguration config = getConfiguration();

        /*
         * We could do the workspace case without resolving the resource
         * (because there's just one workspace), but we want to return null if
         * there was no resource for the path, so we do it down here.
         */
        if (config.getArea() == Area.WORKSPACE) {
            return resource.getWorkspace().getRoot();
        } else if (config.getArea() == Area.FILE) {
            return resource;
        } else if (config.getArea() == Area.PROJECT) {
            return resource.getProject();
        }

        final String messageFormat = Messages.getString("BuildPolicyUI.UnknownAreaFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, config.getArea().toString());
        throw new RuntimeException(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.checkinpolicies.PolicyBase#activate(com.microsoft
     * .tfs. core.checkinpolicies.PolicyFailure)
     */
    public void activate(final PolicyFailure failure) {
        if (runningInEclipse == false) {
            return;
        }

        /*
         * View the original resource with Eclipse's default viewer.
         */
        if (failure instanceof BuildPolicyFailure) {
            final IResource resource = ((BuildPolicyFailure) failure).getResource();

            if (resource == null) {
                return;
            }

            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

            if (resource instanceof IFile == false) {
                return;
            }

            final IEditorInput editorInput = new FileEditorInput((IFile) resource);

            final IEditorDescriptor editor =
                PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(resource.getName());

            if (editor == null) {
                return;
            }

            try {
                page.openEditor(editorInput, editor.getId());
            } catch (final PartInitException e) {
                UIPlugin.getDefault().getLog().log(
                    new Status(
                        Status.WARNING,
                        TFSBuildCheckinPolicyPlugin.PLUGIN_ID,
                        0,
                        Messages.getString("BuildPolicyUI.UnableToOpenEditor"), //$NON-NLS-1$
                        e));
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.checkinpolicies.PolicyInstance#evaluate(com.
     * microsoft .tfs.core.checkinpolicies.PolicyContext)
     */
    @Override
    public synchronized PolicyFailure[] evaluate(final PolicyContext context)
        throws PolicyEvaluationCancelledException {
        if (runningInEclipse == false) {
            return new PolicyFailure[0];
        }

        final PendingCheckin pendingCheckin = getPendingCheckin();
        final PendingCheckinPendingChanges changes = pendingCheckin.getPendingChanges();

        final PendingChange[] checkedChanges = changes.getCheckedPendingChanges();

        final List failures = new ArrayList();

        /*
         * Find the resource (if any) for each change at the correct scope
         * (file, project, working sets, workspace, etc.).
         */
        final Set areaResources = collectUniqueResourcesForChanges(checkedChanges);
        final BuildPolicyConfiguration config = getConfiguration();

        /*
         * Walk over the area resources and find all the markers that match.
         */
        for (final Iterator iterator = areaResources.iterator(); iterator.hasNext();) {
            final IResource areaResource = (IResource) iterator.next();

            final MarkerMatch[] markerMatches = config.getMarkers();

            /*
             * For each configured marker match...
             */
            for (int i = 0; i < markerMatches.length; i++) {
                final MarkerMatch markerMatch = markerMatches[i];
                try {
                    /*
                     * Find all the Eclipse resource markers for the item. We
                     * always turn on infinite depth. If the resource is a file,
                     * this doesn't change things. If the resource was a project
                     * or workspace, we need recursion to catch markers on other
                     * resources.
                     */
                    final IMarker[] foundMarkers = areaResource.findMarkers(
                        markerMatch.getMarkerType(),
                        markerMatch.isIncludeSubtypes(),
                        IResource.DEPTH_INFINITE);

                    if (foundMarkers == null || foundMarkers.length == 0) {
                        continue;
                    }

                    for (int j = 0; j < foundMarkers.length; j++) {
                        final IMarker foundMarker = foundMarkers[j];

                        if (markerMatch.matchesSeverityAndPriority(foundMarker)) {
                            final IResource originalPendingChangeResource = foundMarker.getResource();
                            final String subMessage = (String) foundMarker.getAttribute(IMarker.MESSAGE);

                            String message;
                            if (areaResource instanceof IFile) {
                                final String messageFormat =
                                    Messages.getString("BuildPolicyUI.FileHasBuildProblemFormat"); //$NON-NLS-1$
                                message = MessageFormat.format(messageFormat, areaResource.getName(), subMessage);
                            } else if (areaResource instanceof IProject) {
                                final String messageFormat =
                                    Messages.getString("BuildPolicyUI.ProjectHasBuildProblemFormat"); //$NON-NLS-1$
                                message = MessageFormat.format(
                                    messageFormat,
                                    areaResource.getName(),
                                    originalPendingChangeResource.getName(),
                                    subMessage);
                            } else if (areaResource instanceof IWorkspaceRoot) {
                                final String messageFormat =
                                    Messages.getString("BuildPolicyUI.WorkspaceHasBuildProblemFormat"); //$NON-NLS-1$
                                message = MessageFormat.format(
                                    messageFormat,
                                    originalPendingChangeResource.getName(),
                                    subMessage);
                            } else {
                                final String messageFormat =
                                    Messages.getString("BuildPolicyUI.UnknownBuildProblemFormat"); //$NON-NLS-1$
                                message = MessageFormat.format(messageFormat, subMessage);
                            }

                            failures.add(new BuildPolicyFailure(message, this, originalPendingChangeResource));
                        }
                    }
                } catch (final CoreException e) {
                    failures.add(
                        new PolicyFailure(
                            MessageFormat.format(
                                Messages.getString("BuildPolicyUI.ErrorReadingResourceInfoFormat"), //$NON-NLS-1$
                                e.getLocalizedMessage()),
                            this));

                    UIPlugin.getDefault().getLog().log(
                        new Status(
                            Status.WARNING,
                            TFSBuildCheckinPolicyPlugin.PLUGIN_ID,
                            0,
                            Messages.getString("BuildPolicyUI.ErrorReadingResourceInfo"), //$NON-NLS-1$
                            e));

                    e.printStackTrace();
                }
            }
        }

        return (PolicyFailure[]) failures.toArray(new PolicyFailure[failures.size()]);
    }
}
