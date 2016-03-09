// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.sync;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.microsoft.tfs.client.eclipse.sync.syncinfo.SynchronizeInfo;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class SynchronizeLabelDecorator implements ILabelDecorator {
    private static final Log log = LogFactory.getLog(SynchronizeLabelDecorator.class);

    private final Subscriber subscriber;
    private volatile boolean decorate;

    /*
     * We use a specific preference store so that we can get the TeamUIPlugin
     * preferences (which control sync label decoration.) You would think that
     * the sync label decoration preference would influence whether we were
     * called to decorate or not, but no, we need to actually check the pref.
     */
    private final IPreferenceStore preferenceStore;

    private static final String TEAM_UI_PLUGIN_ID = "org.eclipse.team.ui"; //$NON-NLS-1$
    private static final String DECORATION_PREFERENCE_CONSTANT = TEAM_UI_PLUGIN_ID + ".view_syncinfo_in_label"; //$NON-NLS-1$

    private final SingleListenerFacade listeners = new SingleListenerFacade(ILabelProviderListener.class);

    public SynchronizeLabelDecorator(final Subscriber subscriber) {
        this.subscriber = subscriber;

        preferenceStore = new ScopedPreferenceStore(new InstanceScope(), TEAM_UI_PLUGIN_ID);

        decorate = Boolean.TRUE.equals(preferenceStore.getBoolean(DECORATION_PREFERENCE_CONSTANT));

        preferenceStore.addPropertyChangeListener(new IPropertyChangeListener() {
            @Override
            public void propertyChange(final PropertyChangeEvent event) {
                if (event.getProperty().equals(DECORATION_PREFERENCE_CONSTANT)) {
                    /*
                     * Note that we compare against the string value of the
                     * preference here. Preferences are not strongly typed
                     * (they're strings under the hood), so in the property
                     * change event, we're given the string value.
                     */
                    decorate = "true".equals(event.getNewValue()); //$NON-NLS-1$

                    ((ILabelProviderListener) listeners.getListener()).labelProviderChanged(
                        new LabelProviderChangedEvent(SynchronizeLabelDecorator.this));
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image decorateImage(final Image image, final Object element) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String decorateText(final String text, final Object element) {
        if (!decorate) {
            return null;
        }

        if (!(element instanceof ISynchronizeModelElement)) {
            return null;
        }

        final IResource resource = ((ISynchronizeModelElement) element).getResource();

        if (resource == null) {
            return null;
        }

        SynchronizeInfo syncInfo;

        try {
            final SyncInfo syncObject = subscriber.getSyncInfo(resource);

            /* Sanity check, should never happen */
            if (syncObject == null) {
                return null;
            }

            /* Make sure this is our SynchronizeInfo object */
            if (!(syncObject instanceof SynchronizeInfo)) {
                log.error(MessageFormat.format(
                    "Asked to decorate non-TFS synchronization info ({0}) for resource {1}", //$NON-NLS-1$
                    syncObject.getClass().getCanonicalName(),
                    resource));
                return null;
            }

            syncInfo = (SynchronizeInfo) syncObject;
        } catch (final Throwable t) {
            log.warn(MessageFormat.format("Could not get synchronization info for resource {0}", resource), t); //$NON-NLS-1$
            return null;
        }

        final String localDecoration = decorateLocalChange(resource, syncInfo);
        final String remoteDecoration = decorateRemoteChange(resource, syncInfo);

        if (localDecoration != null && remoteDecoration != null) {
            return MessageFormat.format(
                "{0}  [{1} / {2}]", //$NON-NLS-1$
                syncInfo.getLocal().getName(),
                localDecoration,
                remoteDecoration);
        } else if (localDecoration != null) {
            return MessageFormat.format("{0}  [{1}]", syncInfo.getLocal().getName(), localDecoration); //$NON-NLS-1$
        } else if (remoteDecoration != null) {
            return MessageFormat.format("{0}  [{1}]", syncInfo.getLocal().getName(), remoteDecoration); //$NON-NLS-1$
        }

        /* Probably a parent of a modified resource */
        return syncInfo.getLocal().getName();
    }

    private String decorateLocalChange(final IResource resource, final SynchronizeInfo syncInfo) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(syncInfo, "syncInfo"); //$NON-NLS-1$

        if ((syncInfo.getKind() & SyncInfo.OUTGOING) == 0) {
            return null;
        }

        if (syncInfo.getLocalChanges() == null) {
            /*
             * We should only ever get here when we're decorating a writable
             * conflict
             */
            if ((resource instanceof IFile) && !((IFile) resource).isReadOnly()) {
                return Messages.getString("SynchronizeLabelDecorator.WriteableConflict"); //$NON-NLS-1$
            }

            return null;
        }

        /* Decorate outgoing */
        final PendingChange pendingChange = syncInfo.getLocalChanges();

        String renameChangeDecoration = null;
        if (pendingChange.getChangeType().contains(ChangeType.RENAME)) {
            final String sourceLocal = pendingChange.getSourceServerItem() == null ? null
                : syncInfo.getRepository().getWorkspace().getMappedLocalPath(pendingChange.getSourceServerItem());
            final String targetLocal = pendingChange.getLocalItem();

            /* Handle case changing renames */
            if (ServerPath.equals(pendingChange.getSourceServerItem(), pendingChange.getServerItem())) {
                renameChangeDecoration = Messages.getString("SynchronizeLabelDecorator.CaseChangingRename"); //$NON-NLS-1$
            }

            /*
             * This resource is the source of a rename (must compare server
             * paths)
             */
            else if (LocalPath.equals(sourceLocal, resource.getLocation().toOSString())) {
                renameChangeDecoration =
                    MessageFormat.format(
                        Messages.getString("SynchronizeLabelDecorator.RenameToFormat"), //$NON-NLS-1$
                        getRelativePath(targetLocal, sourceLocal));
            }

            /* This resource is the target of a rename */
            else if (LocalPath.equals(targetLocal, resource.getLocation().toOSString())) {
                renameChangeDecoration =
                    MessageFormat.format(
                        Messages.getString("SynchronizeLabelDecorator.RenameFromFormat"), //$NON-NLS-1$
                        getRelativePath(sourceLocal, targetLocal));
            } else {
                renameChangeDecoration = ""; //$NON-NLS-1$
            }
        }

        /* Exclude rename changetypes, they were handled above */
        final String otherChangeDecoration =
            pendingChange.getChangeType().remove(ChangeType.RENAME).toUIString(true, pendingChange);

        if (renameChangeDecoration != null
            && renameChangeDecoration.length() > 0
            && otherChangeDecoration != null
            && otherChangeDecoration.length() > 0) {
            /*
             * Note: do not externalize without also externalizing
             * ChangeType#toUIString's separator
             */
            return MessageFormat.format("{0}, {1}", renameChangeDecoration, otherChangeDecoration); //$NON-NLS-1$
        } else if (renameChangeDecoration != null && renameChangeDecoration.length() > 0) {
            return renameChangeDecoration;
        } else if (otherChangeDecoration != null && otherChangeDecoration.length() > 0) {
            return otherChangeDecoration;
        }

        return null;
    }

    private String decorateRemoteChange(final IResource resource, final SynchronizeInfo syncInfo) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$
        Check.notNull(syncInfo, "syncInfo"); //$NON-NLS-1$

        if ((syncInfo.getKind() & SyncInfo.INCOMING) == 0) {
            return null;
        }

        final GetOperation getOperation = syncInfo.getRemoteOperation();

        if (getOperation == null) {
            /*
             * We may have a conflicting change that does not have a get
             * operation (in 2010, when a delete is pended, we do not get get
             * operations for conflicting server edits.)
             */
            final PendingChange pendingChange = syncInfo.getLocalChanges();
            final Item item = syncInfo.getRemoteItem();

            if (pendingChange != null
                && pendingChange.getChangeType().contains(ChangeType.DELETE)
                && item != null
                && item.getChangeSetID() > pendingChange.getVersion()) {
                if (item.getDeletionID() > 0) {
                    return Messages.getString("SynchronizeLabelDecorator.Delete"); //$NON-NLS-1$
                }

                /* Return latest version number. */
                return Integer.toString(item.getChangeSetID());
            }

            return null;
        }

        /*
         * This is a delete, or the source of a rename on a post-schema change
         * (2010+) server.
         */
        if (getOperation.isDelete()) {
            /*
             * Don't display the changeset, users probably don't care what
             * changeset this was deleted in
             */
            return Messages.getString("SynchronizeLabelDecorator.Delete"); //$NON-NLS-1$
        }

        /*
         * Case changing renames are special, it's useless to say
         * "dir1 rename from dir1" when the folder case changes.
         */
        if (getOperation.isCaseChangingRename()) {
            return Messages.getString("SynchronizeLabelDecorator.CaseChangingRename"); //$NON-NLS-1$
        }

        /*
         * This is a rename (local paths differ)
         */
        if (getOperation.getCurrentLocalItem() != null
            && getOperation.getTargetLocalItem() != null
            && !LocalPath.equals(getOperation.getCurrentLocalItem(), getOperation.getTargetLocalItem())) {
            /* This is the source of the rename */
            if (LocalPath.equals(getOperation.getCurrentLocalItem(), resource.getLocation().toOSString())) {
                return MessageFormat.format(
                    Messages.getString("SynchronizeLabelDecorator.RenameToFormat"), //$NON-NLS-1$
                    getRelativePath(getOperation.getTargetLocalItem(), getOperation.getCurrentLocalItem()));
            }

            /* This is the target of the rename */
            else if (LocalPath.equals(getOperation.getTargetLocalItem(), resource.getLocation().toOSString())) {
                return MessageFormat.format(
                    Messages.getString("SynchronizeLabelDecorator.RenameFromFormat"), //$NON-NLS-1$
                    getRelativePath(getOperation.getCurrentLocalItem(), getOperation.getTargetLocalItem()));
            }

            else {
                return Messages.getString("SynchronizeLabelDecorator.Rename"); //$NON-NLS-1$
            }
        }

        return Integer.toString(getOperation.getVersionServer());
    }

    private String getRelativePath(final String path, final String relativeTo) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(relativeTo, "relativeTo"); //$NON-NLS-1$

        return LocalPath.makeRelative(path, LocalPath.getDirectory(relativeTo));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addListener(final ILabelProviderListener listener) {
        listeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeListener(final ILabelProviderListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        /* Nothing to dispose */
    }
}
