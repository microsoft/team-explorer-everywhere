// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.vc.TypedItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.events.BranchCommittedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.BranchCommittedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.BranchObjectUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.BranchObjectUpdatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.DestroyEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.DestroyListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.NewPendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.OperationCompletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ShelveEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ShelveListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.UndonePendingChangeListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.BitField;
import com.microsoft.tfs.util.Check;

/**
 *
 * Subclasses may implement one of the following:
 *
 * filesChanged(Set): handles a Set of Strings affected by core events. If you
 * don't override this, it will simply call fileChanged(String) which you should
 * then override.
 *
 * fileChanged(String): handle a single file affected by core resources. If you
 * override filesChanged(Set), this is never called.
 *
 * @threadsafety unknown
 */
public abstract class CoreAffectedFileCollector {
    private final Log log = LogFactory.getLog(this.getClass());

    /*
     * A map of TFS Repository to the affected event listener for that
     * repository (Workspace).
     */
    private final Map<TFSRepository, CoreAffectedEventListener> repositorySet =
        new HashMap<TFSRepository, CoreAffectedEventListener>();

    /*
     * A map of thread (sending events) to the set of files that have been been
     * affected by the events, and a lock to control access. (Store a file set
     * for each thread so that we don't have to hold a heavy lock to the list of
     * all files being updated by each thread.)
     */
    private final Map<Thread, Set<TypedItemSpec>> threadToItemSpecSetMap = new HashMap<Thread, Set<TypedItemSpec>>();

    /*
     * The type of events to pay attention to. Users may, for example, not wish
     * to receive pending change events through here (as they are hooked to
     * pending change cache events also.)
     */
    private final EventType eventTypes;

    public CoreAffectedFileCollector() {
        this(EventType.ALL);
    }

    public CoreAffectedFileCollector(final EventType eventTypes) {
        Check.notNull(eventTypes, "eventTypes"); //$NON-NLS-1$

        this.eventTypes = eventTypes;
    }

    public final boolean containsRepository(final TFSRepository repository) {
        synchronized (repositorySet) {
            return repositorySet.containsKey(repository);
        }
    }

    public final void addRepository(final TFSRepository repository) {
        final CoreAffectedEventListener coreListener = new CoreAffectedEventListener(repository);

        synchronized (repositorySet) {
            if (repositorySet.containsKey(repository)) {
                throw new RuntimeException(Messages.getString("CoreAffectedFileCollector.RepositoryAlreadyExists")); //$NON-NLS-1$
            }

            repositorySet.put(repository, coreListener);
        }

        final VersionControlEventEngine engine = repository.getWorkspace().getClient().getEventEngine();

        engine.addBranchCommittedListener(coreListener);
        engine.addBranchObjectUpdatedListener(coreListener);
        engine.addCheckinListener(coreListener);
        engine.addShelveListener(coreListener);
        engine.addConflictResolvedListener(coreListener);
        engine.addDestroyListener(coreListener);
        engine.addGetListener(coreListener);
        engine.addMergingListener(coreListener);
        engine.addNewPendingChangeListener(coreListener);
        engine.addOperationCompletedListener(coreListener);
        engine.addUndonePendingChangeListener(coreListener);
    }

    public final void removeRepository(final TFSRepository repository) {
        CoreAffectedEventListener coreListener;

        synchronized (repositorySet) {
            coreListener = repositorySet.remove(repository);

            if (coreListener == null) {
                throw new RuntimeException(Messages.getString("CoreAffectedFileCollector.CannotRemoveWorkspace")); //$NON-NLS-1$
            }
        }

        final VersionControlEventEngine engine = repository.getWorkspace().getClient().getEventEngine();

        engine.removeBranchCommittedListener(coreListener);
        engine.removeBranchObjectUpdatedListener(coreListener);
        engine.removeCheckinListener(coreListener);
        engine.removeShelveListener(coreListener);
        engine.removeConflictResolvedListener(coreListener);
        engine.removeDestroyListener(coreListener);
        engine.removeGetListener(coreListener);
        engine.removeMergingListener(coreListener);
        engine.removeNewPendingChangeListener(coreListener);
        engine.removeOperationCompletedListener(coreListener);
        engine.removeUndonePendingChangeListener(coreListener);
    }

    /**
     * Clients may override to perform file handling in batch.
     *
     * If clients override, they should not override
     * {@link #fileChanged(String)} as it will not be called.
     *
     * @param fileSet
     */
    protected void filesChanged(final Set<TypedItemSpec> itemSpecSet) {
        for (final Iterator<TypedItemSpec> i = itemSpecSet.iterator(); i.hasNext();) {
            fileChanged(i.next());
        }
    }

    /**
     * Clients may override to perform per-file handling.
     *
     * If clients override, they should not override {@link #filesChanged(Set)}
     * as this method will not be called.
     *
     * @param localItem
     */
    protected void fileChanged(final ItemSpec itemSpec) {
    }

    private final Set<TypedItemSpec> getItemSpecSet() {
        synchronized (threadToItemSpecSetMap) {
            Set<TypedItemSpec> itemSpecSet;

            if ((itemSpecSet = threadToItemSpecSetMap.get(Thread.currentThread())) == null) {
                itemSpecSet = new HashSet<TypedItemSpec>();
                threadToItemSpecSetMap.put(Thread.currentThread(), itemSpecSet);
            }

            return itemSpecSet;
        }
    }

    private final Set<TypedItemSpec> removeItemSpecSet() {
        synchronized (threadToItemSpecSetMap) {
            Set<TypedItemSpec> itemSpecSet = threadToItemSpecSetMap.remove(Thread.currentThread());

            if (itemSpecSet == null) {
                itemSpecSet = new HashSet<TypedItemSpec>();
            }

            return itemSpecSet;
        }
    }

    protected final Log getLog() {
        return log;
    }

    public final static class EventType extends BitField {
        private static final long serialVersionUID = -6235284307155069578L;

        public static final EventType GET = new EventType(1);
        public static final EventType MERGING = new EventType(2);
        public static final EventType NEW_PENDING_CHANGE = new EventType(4);
        public static final EventType UNDONE_PENDING_CHANGE = new EventType(8);
        public static final EventType CONFLICT_RESOLVED = new EventType(16);
        public static final EventType CHECKIN = new EventType(32);
        public static final EventType DESTROY = new EventType(64);
        public static final EventType BRANCH_COMMITTED = new EventType(128);
        public static final EventType BRANCH_OBJECT_UPDATED = new EventType(256);
        public static final EventType SHELVE = new EventType(512);

        public static final EventType ALL = combine(new EventType[] {
            GET,
            MERGING,
            NEW_PENDING_CHANGE,
            UNDONE_PENDING_CHANGE,
            CONFLICT_RESOLVED,
            CHECKIN,
            DESTROY,
            BRANCH_COMMITTED,
            BRANCH_OBJECT_UPDATED,
            SHELVE
        });

        private EventType(final int flags) {
            super(flags);
        }

        public static EventType combine(final EventType[] eventTypes) {
            return new EventType(BitField.combine(eventTypes));
        }

        public EventType remove(final EventType[] eventTypes) {
            int flags = toIntFlags();

            for (int i = 0; i < eventTypes.length; i++) {
                flags &= ~(eventTypes[i].toIntFlags());
            }

            return new EventType(flags);
        }

        public boolean contains(final EventType eventType) {
            return containsInternal(eventType);
        }
    }

    private final class CoreAffectedEventListener
        implements BranchCommittedListener, BranchObjectUpdatedListener, CheckinListener, ConflictResolvedListener,
        DestroyListener, GetListener, MergingListener, NewPendingChangeListener, OperationCompletedListener,
        UndonePendingChangeListener, ShelveListener {
        final TFSRepository repository;

        public CoreAffectedEventListener(final TFSRepository repository) {
            Check.notNull(repository, "repository"); //$NON-NLS-1$

            this.repository = repository;
        }

        @Override
        public void onBranchCommitted(final BranchCommittedEvent e) {
            if (!eventTypes.contains(EventType.BRANCH_COMMITTED)) {
                return;
            }

            // Not in batch, create and dispatch from here
            final Set<TypedItemSpec> itemSpecSet = new HashSet<TypedItemSpec>();

            if (e.getSourcePath() != null) {
                final String localPath = repository.getWorkspace().getMappedLocalPath(e.getSourcePath());

                if (localPath != null) {
                    itemSpecSet.add(new TypedItemSpec(localPath, RecursionType.FULL, ItemType.FOLDER));
                }
            }

            if (e.getTargetPath() != null) {
                final String localPath = repository.getWorkspace().getMappedLocalPath(e.getTargetPath());

                if (localPath != null) {
                    itemSpecSet.add(new TypedItemSpec(localPath, RecursionType.FULL, ItemType.FOLDER));
                }
            }

            filesChanged(itemSpecSet);
        }

        @Override
        public void onBranchObjectUpdated(final BranchObjectUpdatedEvent e) {
            if (!eventTypes.contains(EventType.BRANCH_OBJECT_UPDATED)) {
                return;
            }

            // Not in batch, create and dispatch from here
            final Set<TypedItemSpec> itemSpecSet = new HashSet<TypedItemSpec>();

            if (e.getBranchProperties().getParentBranch() != null) {
                final String localPath =
                    repository.getWorkspace().getMappedLocalPath(e.getBranchProperties().getParentBranch().getItem());

                if (localPath != null) {
                    itemSpecSet.add(new TypedItemSpec(localPath, RecursionType.FULL, ItemType.FOLDER));
                }
            }

            if (e.getBranchProperties().getRootItem() != null) {
                final String localPath =
                    repository.getWorkspace().getMappedLocalPath(e.getBranchProperties().getRootItem().getItem());

                if (localPath != null) {
                    itemSpecSet.add(new TypedItemSpec(localPath, RecursionType.FULL, ItemType.FOLDER));
                }
            }

            filesChanged(itemSpecSet);
        }

        @Override
        public void onCheckin(final CheckinEvent e) {
            if (!eventTypes.contains(EventType.CHECKIN)) {
                return;
            }

            // Not in batch, create and dispatch from here
            final Set<TypedItemSpec> itemSpecSet = new HashSet<TypedItemSpec>();

            // Treat undone and committed changes the same.
            final List<PendingChange> allChanges = new ArrayList<PendingChange>();
            allChanges.addAll(Arrays.asList(e.getUndoneChanges()));
            allChanges.addAll(Arrays.asList(e.getCommittedChanges()));

            addItemsForPendingChanges(itemSpecSet, allChanges.toArray(new PendingChange[allChanges.size()]));

            filesChanged(itemSpecSet);
        }

        @Override
        public void onShelve(final ShelveEvent e) {
            if (!eventTypes.contains(EventType.SHELVE)) {
                return;
            }

            /*
             * The shelve event is fired after all the get operations have
             * completed (we have processed those events already) but also after
             * files have been locally deleted if local files were not to be
             * preserved (changes were "moved").
             */
            if (e.isMove()) {
                // Not in batch, create and dispatch from here
                final Set<TypedItemSpec> itemSpecSet = new HashSet<TypedItemSpec>();

                addItemsForPendingChanges(itemSpecSet, e.getShelvedChanges());

                filesChanged(itemSpecSet);
            }
        }

        @Override
        public void onConflictResolved(final ConflictResolvedEvent e) {
            if (!eventTypes.contains(EventType.CONFLICT_RESOLVED)) {
                return;
            }

            // Happens in operation batch, use the thread-local set
            final Set<TypedItemSpec> itemSpecSet = getItemSpecSet();

            final String yourLocalItem =
                repository.getWorkspace().getMappedLocalPath(e.getConflict().getYourServerItem());
            final String theirLocalItem =
                repository.getWorkspace().getMappedLocalPath(e.getConflict().getTheirServerItem());

            if (yourLocalItem != null) {
                itemSpecSet.add(
                    new TypedItemSpec(
                        yourLocalItem,
                        getRecursionType(e.getConflict().getYourItemType()),
                        e.getConflict().getYourItemType()));
            }

            if (theirLocalItem != null) {
                itemSpecSet.add(
                    new TypedItemSpec(
                        theirLocalItem,
                        getRecursionType(e.getConflict().getTheirItemType()),
                        e.getConflict().getTheirItemType()));
            }
        }

        @Override
        public void onDestroy(final DestroyEvent e) {
            if (!eventTypes.contains(EventType.DESTROY)) {
                return;
            }

            final String serverItem = e.getDestroyedItem().getServerItem();
            final String localItem = repository.getWorkspace().getMappedLocalPath(serverItem);

            if (localItem != null) {
                // Not in batch, create and dispatch from here
                final Set<TypedItemSpec> set = new HashSet<TypedItemSpec>();

                set.add(
                    new TypedItemSpec(
                        localItem,
                        getRecursionType(e.getDestroyedItem().getItemType()),
                        e.getDestroyedItem().getItemType()));

                filesChanged(set);
            }
        }

        @Override
        public void onGet(final GetEvent e) {
            if (!eventTypes.contains(EventType.GET)) {
                return;
            }

            // Happens in operation batch, use the thread-local set
            final Set<TypedItemSpec> itemSpecSet = getItemSpecSet();

            if (e.getTargetLocalItem() != null) {
                itemSpecSet.add(
                    new TypedItemSpec(
                        e.getTargetLocalItem(),
                        getRecursionType(e.getOperation().getItemType()),
                        e.getOperation().getItemType()));
            } else if (e.getServerItem() != null) {
                final String localPath = repository.getWorkspace().getMappedLocalPath(e.getServerItem());

                if (localPath != null) {
                    itemSpecSet.add(
                        new TypedItemSpec(
                            localPath,
                            getRecursionType(e.getOperation().getItemType()),
                            e.getOperation().getItemType()));
                }
            }
        }

        @Override
        public void onMerging(final MergingEvent e) {
            if (!eventTypes.contains(EventType.MERGING)) {
                return;
            }

            /*
             * We may not have a pending change for this merged item yet (in the
             * case of conflicts, these will be fired later with the correct
             * pending change.) This could only occur with files, so we can
             * assume that if there's no pending change, we're dealing with a
             * file.
             */
            final ItemType itemType =
                (e.getPendingChange() != null) ? e.getPendingChange().getItemType() : ItemType.FILE;
            final RecursionType recursionType = getRecursionType(itemType);

            // Happens in operation batch, use the thread-local set
            final Set<TypedItemSpec> itemSpecSet = getItemSpecSet();

            /*
             * Logic here is the same as for processing pending change events,
             * but look at the conflict to find paths because it will have
             * information for the items the server merged for us.
             */
            if (e.getSourceLocalItem() != null) {
                itemSpecSet.add(new TypedItemSpec(e.getSourceLocalItem(), recursionType, itemType));
            } else if (e.getSourceServerItem() != null) {
                final String sourceLocalItem = repository.getWorkspace().getMappedLocalPath(e.getSourceServerItem());

                if (sourceLocalItem != null) {
                    itemSpecSet.add(new TypedItemSpec(sourceLocalItem, recursionType, itemType));
                }
            }

            if (e.getTargetLocalItem() != null) {
                itemSpecSet.add(new TypedItemSpec(e.getTargetLocalItem(), recursionType, itemType));
            } else if (e.getTargetServerItem() != null) {
                final String targetLocalItem = repository.getWorkspace().getMappedLocalPath(e.getTargetServerItem());

                if (targetLocalItem != null) {
                    itemSpecSet.add(new TypedItemSpec(targetLocalItem, recursionType, itemType));
                }
            }
        }

        @Override
        public void onNewPendingChange(final PendingChangeEvent e) {
            if (!eventTypes.contains(EventType.NEW_PENDING_CHANGE)) {
                return;
            }

            // Happens in operation batch, use the thread-local set
            final Set<TypedItemSpec> itemSpecSet = getItemSpecSet();

            addItemsForPendingChanges(itemSpecSet, new PendingChange[] {
                e.getPendingChange()
            });
        }

        @Override
        public void onUndonePendingChange(final PendingChangeEvent e) {
            if (!eventTypes.contains(EventType.UNDONE_PENDING_CHANGE)) {
                return;
            }

            // Happens in operation batch, use the thread-local set
            final Set<TypedItemSpec> itemSpecSet = getItemSpecSet();

            addItemsForPendingChanges(itemSpecSet, new PendingChange[] {
                e.getPendingChange()
            });
        }

        @Override
        public void onOperationCompleted(final OperationCompletedEvent e) {
            final Set<TypedItemSpec> itemSpecSet = removeItemSpecSet();

            if (itemSpecSet.size() > 0) {
                filesChanged(itemSpecSet);
            }
        }

        /**
         * Examines {@link PendingChange}s for affected local paths, creates
         * {@link TypedItemSpec}s for them, and puts them in the specified
         * {@link Set}.
         *
         * @param itemSpecSet
         *        the set to add new {@link TypedItemSpec}s to (must not be
         *        <code>null</code>)
         * @param pendingChanges
         *        the pending changes to examine for local paths (must not be
         *        <code>null</code>)
         */
        private void addItemsForPendingChanges(
            final Set<TypedItemSpec> itemSpecSet,
            final PendingChange[] pendingChanges) {
            Check.notNull(itemSpecSet, "itemSpecSet"); //$NON-NLS-1$
            Check.notNull(pendingChanges, "pendingChanges"); //$NON-NLS-1$

            for (final PendingChange change : pendingChanges) {
                if (change.getLocalItem() != null) {
                    itemSpecSet.add(
                        new TypedItemSpec(
                            change.getLocalItem(),
                            getRecursionType(change.getItemType()),
                            change.getItemType()));
                } else if (change.getServerItem() != null) {
                    final String localItem = repository.getWorkspace().getMappedLocalPath(change.getServerItem());

                    if (localItem != null) {
                        itemSpecSet.add(
                            new TypedItemSpec(localItem, getRecursionType(change.getItemType()), change.getItemType()));
                    }
                }

                if (change.getSourceLocalItem() != null) {
                    itemSpecSet.add(
                        new TypedItemSpec(
                            change.getSourceLocalItem(),
                            getRecursionType(change.getItemType()),
                            change.getItemType()));
                } else if (change.getSourceServerItem() != null) {
                    final String sourceLocalItem =
                        repository.getWorkspace().getMappedLocalPath(change.getSourceServerItem());

                    if (sourceLocalItem != null) {
                        itemSpecSet.add(
                            new TypedItemSpec(
                                sourceLocalItem,
                                getRecursionType(change.getItemType()),
                                change.getItemType()));
                    }
                }
            }
        }

        private RecursionType getRecursionType(final ItemType itemType) {
            if (itemType == ItemType.FILE) {
                return RecursionType.NONE;
            } else {
                return RecursionType.FULL;
            }
        }
    }
}
