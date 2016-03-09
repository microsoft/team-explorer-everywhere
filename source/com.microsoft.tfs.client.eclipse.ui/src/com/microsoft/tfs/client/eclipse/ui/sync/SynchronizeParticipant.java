// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.sync;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantDescriptor;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PartInitException;

import com.microsoft.tfs.client.common.framework.command.JobCommandAdapter;
import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryManagerListener;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeSubscriber;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.TFSEclipseClientUIPlugin;
import com.microsoft.tfs.client.eclipse.ui.actions.sync.SynchronizeActionGroup;
import com.microsoft.tfs.client.eclipse.ui.commands.sync.RefreshSubscriberCommand;
import com.microsoft.tfs.util.Check;

public class SynchronizeParticipant extends SubscriberParticipant {
    public static final String PARTICIPANT_ID = "com.microsoft.tfs.client.eclipse.ui.sync.SynchronizeParticipant"; //$NON-NLS-1$

    public static final String CONTEXT_MENU_GROUP_ID = "context_menu_1"; //$NON-NLS-1$

    // strings used as keys into mementos
    private static final String PARTICIPANT_SETTINGS =
        TFSEclipseClientUIPlugin.PLUGIN_ID + ".SYNCHRONIZEPARTICIPANT_SETTINGS"; //$NON-NLS-1$
    private static final String PARTICIPANT_SETTINGS_RESOURCES = PARTICIPANT_SETTINGS + ".RESOURCES"; //$NON-NLS-1$

    private IResource[] resources;

    /**
     * No-arg constructor is used for creation from persisted synchronization
     * state. (Ie, restarting eclipse with an open Team Sync perspective.)
     */
    public SynchronizeParticipant() {
        super();

        finishConstruction();
    }

    public SynchronizeParticipant(final ISynchronizeScope scope) {
        super(scope);

        finishConstruction();
    }

    private void finishConstruction() {
        setSubscriber(SynchronizeSubscriber.getInstance());
        setSecondaryId(Long.toString(System.currentTimeMillis()));

        TFSEclipseClientPlugin.getDefault().getProjectManager().addListener(
            new SynchronizeParticipantProjectListener());
    }

    /**
     * This allows the synchronize wizard to set the resources to sync. Storing
     * them here allows us to persist them between invocations of the sync view.
     *
     * @param resources
     *        The IResources to synchronize on
     */
    public void setResources(final IResource[] resources) {
        this.resources = resources;

        getSubscriberSyncInfoCollector().setRoots(resources);
    }

    /**
     * Restores the resources that were previously saved from a saveState().
     * This will usually be called when opening a previously pinned sync view.
     *
     * @param settings
     *        The IMemento to restore from
     */
    private void setResources(final IMemento settings) {
        Check.notNull(settings, "settings"); //$NON-NLS-1$

        final List<IResource> resourceList = new ArrayList<IResource>();
        final String resourcePathList = settings.getString(PARTICIPANT_SETTINGS_RESOURCES);

        if (resourcePathList == null) {
            return;
        }

        final String[] resourcePaths = resourcePathList.split("\\|"); //$NON-NLS-1$

        for (int i = 0; i < resourcePaths.length; i++) {
            ResourceType resourceType = ResourceType.FILE;

            if (resourcePaths[i].endsWith("/")) //$NON-NLS-1$
            {
                resourceType = ResourceType.CONTAINER;
            }

            final IResource resource = Resources.getResourceForLocation(resourcePaths[i], resourceType);

            if (resource != null) {
                resourceList.add(resource);
            }
        }

        setResources(resourceList.toArray(new IResource[resourceList.size()]));
    }

    /*
     * We override getResources() because we store resources chosen from the
     * synchronize wizard for persistance.
     *
     * (non-Javadoc)
     *
     * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#getResources()
     */
    @Override
    public IResource[] getResources() {
        if (resources != null) {
            return resources;
        }

        return super.getResources();
    }

    /*
     * Initializes the participant immediately before the View is raised, called
     * to restore persisted state.
     *
     * @see
     * org.eclipse.team.ui.synchronize.SubscriberParticipant#init(java.lang.
     * String, org.eclipse.ui.IMemento)
     */
    @Override
    public void init(final String secondaryId, final IMemento memento) throws PartInitException {
        super.init(secondaryId, memento);

        // reset secondaryId -- otherwise it will persist the original
        // secondaryId set by the constructor and you'll have multiple
        // persisted syncs. yoinks!
        setSecondaryId(secondaryId);

        // load the list of resources from the last synchronization for this
        // secondaryId. (usually because a synchronization state is being
        // restored.)
        if (memento != null) {
            final IMemento settings = memento.getChild(PARTICIPANT_SETTINGS);

            if (settings != null) {
                setResources(settings);
            }
        }

        // this is called when Eclipse starts with the synchronize view open,
        // this is our best chance to refresh. (otherwise the synchronize view
        // will always start with no changes to display.)
        final RefreshSubscriberCommand refreshCommand =
            new RefreshSubscriberCommand(SynchronizeSubscriber.getInstance(), getResources(), IResource.DEPTH_INFINITE);

        final Job refreshJob = new JobCommandAdapter(refreshCommand);
        refreshJob.setName(Messages.getString("SynchronizeParticipant.RefreshJobName")); //$NON-NLS-1$
        refreshJob.setPriority(Job.LONG);
        refreshJob.schedule();

        try {
            final ISynchronizeParticipantDescriptor descriptor =
                TeamUI.getSynchronizeManager().getParticipantDescriptor(PARTICIPANT_ID);
            setInitializationData(descriptor);
        } catch (final CoreException e) {
            throw new PartInitException(e.getMessage());
        }
    }

    /*
     * Called to save persistent synchronization state.
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.ui.synchronize.ISynchronizeParticipant#saveState(org
     * .eclipse.ui.IMemento)
     */
    @Override
    public void saveState(final IMemento memento) {
        super.saveState(memento);

        final IMemento settings = memento.createChild(PARTICIPANT_SETTINGS);

        // persist the paths of all resources that we're currently
        // synchronizing,
        // we'll use these paths to restore the synchronization view
        final IResource[] resources = getResources();

        final StringBuffer resourceList = new StringBuffer();
        for (int i = 0; i < resources.length; i++) {
            if (i > 0) {
                resourceList.append("|"); //$NON-NLS-1$
            }

            resourceList.append(resources[i].getLocation().toOSString());

            if (resources[i].getType() != IResource.FILE) {
                resourceList.append("/"); //$NON-NLS-1$
            }
        }

        settings.putString(PARTICIPANT_SETTINGS_RESOURCES, resourceList.toString());
    }

    /*
     * The ID for this synchronizer: must match the ID in plugin.xml.
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#getId()
     */
    @Override
    public String getId() {
        return PARTICIPANT_ID;
    }

    /*
     * The name of the synchronizer to display.
     *
     * (non-Javadoc)
     *
     * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#getName()
     */
    @Override
    public String getName() {
        return Messages.getString("SynchronizeParticipant.TeamFoundationServer"); //$NON-NLS-1$
    }

    /*
     * Initialize the page configuration: show filter buttons, default to show
     * incomding, outgoing and conflicting changes, setup a label decorator,
     * etc.
     *
     * (non-Javadoc)
     *
     * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#
     * initializeConfiguration
     * (org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
     */
    @Override
    protected void initializeConfiguration(final ISynchronizePageConfiguration configuration) {
        super.initializeConfiguration(configuration);

        // setup a label decorator
        configuration.addLabelDecorator(new SynchronizeLabelDecorator(getSubscriber()));

        // set incoming/outgoing/conflict mode to be the default
        configuration.setSupportedModes(ISynchronizePageConfiguration.ALL_MODES);
        configuration.setMode(ISynchronizePageConfiguration.BOTH_MODE);

        // add the synchronize context menu group
        configuration.addMenuGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, CONTEXT_MENU_GROUP_ID);
    }

    /*
     * Override to add action contributions.
     *
     * (non-Javadoc)
     *
     * @see org.eclipse.team.ui.synchronize.SubscriberParticipant#
     * validateConfiguration
     * (org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration)
     */
    @Override
    protected void validateConfiguration(final ISynchronizePageConfiguration configuration) {
        configuration.addActionContribution(new SynchronizeActionGroup());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.ui.synchronize.SubscriberParticipant#getLongTaskName()
     */
    @Override
    protected String getLongTaskName() {
        final String messageFormat = Messages.getString("SynchronizeParticipant.TaskNameNoResourceFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, getName());
        return message;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.ui.synchronize.SubscriberParticipant#getShortTaskName()
     */
    @Override
    protected String getShortTaskName() {
        return getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.team.ui.synchronize.SubscriberParticipant#getLongTaskName
     * (org.eclipse.core.resources.IResource[])
     */
    @Override
    protected String getLongTaskName(final IResource[] resources) {
        if (resources.length == 1) {
            final String messageFormat = Messages.getString("SynchronizeParticipant.TaskNameSingleResourceFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, getName(), resources[0].getFullPath().toString());
            return message;
        } else {
            final String messageFormat = Messages.getString("SynchronizeParticipant.TaskNameMultiResourceFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getName(), resources.length);
            return message;
        }
    }

    /*
     * Allow viewer contributions.
     *
     * (non-Javadoc)
     *
     * @seeorg.eclipse.team.ui.synchronize.AbstractSynchronizeParticipant#
     * isViewerContributionsSupported()
     */
    @Override
    protected boolean isViewerContributionsSupported() {
        return true;
    }

    private final class SynchronizeParticipantProjectListener implements ProjectRepositoryManagerListener {
        private final Map<Thread, List<IProject>> threadToOfflineProjectMap = new HashMap<Thread, List<IProject>>();

        @Override
        public void onOperationStarted() {
        }

        @Override
        public void onProjectConnected(final IProject project, final TFSRepository repository) {
            /*
             * Do nothing currently
             *
             * TODO: reconnect projects that we removed going offline.
             */
        }

        @Override
        public void onProjectDisconnected(final IProject project) {
            unsynchronizeProject(project);
        }

        @Override
        public void onProjectRemoved(final IProject project) {
            unsynchronizeProject(project);
        }

        private void unsynchronizeProject(final IProject project) {
            /*
             * If we're currently synchronizing any of these projects, remove
             * them.
             */
            synchronized (threadToOfflineProjectMap) {
                List<IProject> projectList = threadToOfflineProjectMap.get(Thread.currentThread());

                if (projectList == null) {
                    projectList = new ArrayList<IProject>();
                    threadToOfflineProjectMap.put(Thread.currentThread(), projectList);
                }

                projectList.add(project);
            }
        }

        @Override
        public void onOperationFinished() {
            synchronized (threadToOfflineProjectMap) {
                final List<IProject> offlineProjectList = threadToOfflineProjectMap.get(Thread.currentThread());

                if (offlineProjectList != null) {
                    final IResource[] roots = getResources();
                    final List<IResource> onlineResources = new ArrayList<IResource>();

                    for (int i = 0; i < roots.length; i++) {
                        if (!offlineProjectList.contains(roots[i].getProject())) {
                            onlineResources.add(roots[i]);
                        }
                    }

                    threadToOfflineProjectMap.remove(Thread.currentThread());

                    setResources(onlineResources.toArray(new IResource[onlineResources.size()]));
                }
            }
        }
    }
}
