// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceKey;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class RepositoryManager {
    private static final Log log = LogFactory.getLog(RepositoryManager.class);

    /*
     * This lock arbitrates all the repository fields in this class.
     */
    private final Object lock = new Object();

    /**
     * A list of all {@link TFSRepository}s held by this manager. A map of
     * {@link WorkspaceKey} to {@link TFSRepository} items.
     */
    private final List<TFSRepository> repositoryList = new ArrayList<TFSRepository>();
    private final Map<WorkspaceKey, TFSRepository> repositoryMap = new HashMap<WorkspaceKey, TFSRepository>();
    private TFSRepository defaultRepository = null;

    private final SingleListenerFacade listeners = new SingleListenerFacade(RepositoryManagerListener.class);

    public RepositoryManager() {
        log.debug("RepositoryManager started"); //$NON-NLS-1$
    }

    public final void addListener(final RepositoryManagerListener listener) {
        listeners.addListener(listener);
    }

    public final void removeListener(final RepositoryManagerListener listener) {
        listeners.removeListener(listener);
    }

    public final TFSRepository[] getRepositories() {
        synchronized (lock) {
            return repositoryList.toArray(new TFSRepository[repositoryList.size()]);
        }
    }

    /**
     * Convenience method to get the current repositories and add a listener for
     * new ones. Exists so that you can add a repository in the repository
     * manager lock, meaning that you will be guaranteed not to have a race
     * between getting the list and adding a listener.
     *
     */
    public final TFSRepository[] getRepositoriesAndAddListener(final RepositoryManagerListener listener) {
        synchronized (lock) {
            listeners.addListener(listener);

            return getRepositories();
        }
    }

    public final TFSRepository setDefaultRepository(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        synchronized (lock) {
            TFSRepository repository = getRepository(workspace);

            if (repository == null) {
                repository = new TFSRepository(workspace);
            }

            return setDefaultRepository(repository);
        }
    }

    public final TFSRepository setDefaultRepository(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        final WorkspaceKey key = new WorkspaceKey(repository.getWorkspace());

        synchronized (lock) {
            /* See if this repository is already being managed */
            final TFSRepository existingRepository = repositoryMap.get(key);

            /*
             * An equivalent but not identical repository is being set, we need
             * to replace
             */
            if (existingRepository == null || existingRepository != repository) {
                /*
                 * TODO: this is to work around the existing UI code that
                 * expects one repository. We keep only a single repository in
                 * repository manager. So if we're adding a new repository, make
                 * sure to eliminate the existing one.
                 */
                if (defaultRepository != null) {
                    defaultRepository.close();
                    removeRepository(defaultRepository);
                }

                final RepositoryReplaceResults results = addRepositoryInternal(repository);

                /* Already the default, don't do any more */
                if (results.isDefaultRepository()) {
                    return repository;
                }
            }

            defaultRepository = repository;
            getListener().onDefaultRepositoryChanged(new RepositoryManagerEvent(this, repository));

            return repository;
        }
    }

    public final TFSRepository getDefaultRepository() {
        synchronized (lock) {
            return defaultRepository;
        }
    }

    /**
     * Convenience method to get the current repositories and add a listener for
     * new ones. Exists so that you can add a repository in the repository
     * manager lock, meaning that you will be guaranteed not to have a race
     * between getting the list and adding a listener.
     *
     */
    public TFSRepository getDefaultRepositoryAndAddListener(final RepositoryManagerListener listener) {
        synchronized (lock) {
            listeners.addListener(listener);

        }
        return defaultRepository;
    }

    public final TFSRepository addRepository(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        final TFSRepository repository = new TFSRepository(workspace);
        try {
            addRepository(repository);
        } catch (final RepositoryConflictException e) {
            // Ensure the repository we created gets closed
            repository.close();
            throw e;
        }

        return repository;
    }

    /**
     * @throws RepositoryExistsException
     *         If the given repository already is managed. This is so that
     *         callers do not fire added listeners erroneously. Make sure to
     *         call {@link TFSRepository#close()} if the repository object will
     *         no longer be used.
     */
    public final void addRepository(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        addRepositoryInternal(repository);
    }

    /**
     * @throws RepositoryConflictException
     *         If the given repository already is managed. This is so that
     *         callers do not fire added listeners erroneously. Make sure to
     *         call {@link TFSRepository#close()} if the repository object will
     *         no longer be used.
     */
    private final RepositoryReplaceResults addRepositoryInternal(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        RepositoryReplaceResults results;

        results = replaceRepositoryInternal(repository);

        if (results.isIdenticalRepository()) {
            return results;
        }

        final TFSRepository existingRepository = results.getExistingRepository();

        if (existingRepository != null) {
            getListener().onRepositoryRemoved(new RepositoryManagerEvent(this, existingRepository));
        }

        getListener().onRepositoryAdded(new RepositoryManagerEvent(this, repository));

        if (results.isDefaultRepository() && existingRepository != null) {
            getListener().onDefaultRepositoryChanged(new RepositoryManagerEvent(this, repository));
        }

        return results;
    }

    /**
     * This will begin managing the existing TFS Repository. If there is already
     * a TFS Repository being managed for the given workspace, it will be
     * removed and the new one will be added, unless the given TFS Repository is
     * equal to the existing TFS Repository, in which case an exception is
     * thrown.
     *
     * @param repository
     *        The TFS Repository to begin managing
     * @return The existing repository for this workspace
     * @throws RepositoryExistsException
     *         If the given repository already is managed. This is so that
     *         callers do not fire added listeners erroneously. Make sure to
     *         call {@link TFSRepository#close()} if the repository object will
     *         no longer be used.
     */
    private final RepositoryReplaceResults replaceRepositoryInternal(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        final WorkspaceKey key = new WorkspaceKey(repository.getWorkspace());

        boolean isDefaultRepository = false;
        TFSRepository existingRepository;

        synchronized (lock) {
            /*
             * TODO: there's bunches of legacy UI code that assumes that we have
             * a single connection with a single (TFS) workspace. This exists
             * here to ensure that we don't actually have multiple TFS
             * Repositories before the UI code is actually capable of handling
             * it.
             */
            if (repositoryList.size() > 0) {
                throw new RepositoryConflictException();
            }

            final String messageFormat = "replaceRepositoryInternal: creating new repository, key=[{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, key);
            log.trace(message);

            existingRepository = repositoryMap.get(key);

            /*
             * If we were asked to add an existing repository, throw an
             * exception for the callers to deal with. This prevents us from
             * firing removed/added events for the same repository.
             */
            if (existingRepository != null && existingRepository == repository) {
                return new ExistingRepositoryReplaceResults(repository);
            } else if (existingRepository != null) {
                repositoryList.remove(existingRepository);
            }

            repositoryList.add(repository);
            repositoryMap.put(key, repository);

            if (repositoryList.size() == 1) {
                isDefaultRepository = true;
                defaultRepository = repository;
            }
        }

        getListener().onDefaultRepositoryChanged(new RepositoryManagerEvent(this, repository));

        return new RepositoryReplaceResults(isDefaultRepository, existingRepository);
    }

    public final TFSRepository getRepository(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        final WorkspaceKey key = new WorkspaceKey(workspace);

        synchronized (lock) {
            final TFSRepository repository = repositoryMap.get(key);
            return repository;
        }
    }

    public final TFSRepository getRepository(final WorkspaceInfo cachedWorkspace) {
        Check.notNull(cachedWorkspace, "cachedWorkspace"); //$NON-NLS-1$

        final WorkspaceKey key = new WorkspaceKey(cachedWorkspace);

        synchronized (lock) {
            final TFSRepository repository = repositoryMap.get(key);

            final String messageFormat = "tryGetRepository, key=[{0}], result=[{1}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, key, repository);
            log.trace(message);
            return repository;
        }
    }

    public final TFSRepository getOrCreateRepository(final Workspace workspace) {
        return getOrCreateRepository(workspace, null);
    }

    public final TFSRepository getOrCreateRepository(
        final Workspace workspace,
        final RepositoryStatusContainer statusContainer) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        final WorkspaceKey key = new WorkspaceKey(workspace);
        TFSRepository repository;

        synchronized (lock) {
            if ((repository = repositoryMap.get(key)) != null) {
                if (statusContainer != null) {
                    statusContainer.setRepositoryStatus(RepositoryStatus.EXISTING);
                }

                return repository;
            }

            repository = new TFSRepository(workspace);

            final RepositoryReplaceResults results;
            try {
                results = replaceRepositoryInternal(repository);
            } catch (final RepositoryConflictException e) {
                // Ensure the repository we created gets closed
                repository.close();
                throw e;
            }

            if (results.isIdenticalRepository()) {
                /*
                 * Can't happen unless code erroneously adds a repository
                 * without locking
                 */
                throw new ConcurrentModificationException("The RepositoryManager was modified outside of a lock"); //$NON-NLS-1$
            }
        }

        getListener().onRepositoryAdded(new RepositoryManagerEvent(this, repository));

        if (statusContainer != null) {
            statusContainer.setRepositoryStatus(RepositoryStatus.CREATED);
        }

        return repository;
    }

    public final void removeAllRepositories() {
        final List<TFSRepository> removedRepositories;

        synchronized (lock) {
            removedRepositories = new ArrayList<TFSRepository>(repositoryList);

            for (final Entry<WorkspaceKey, TFSRepository> repositoryEntry : repositoryMap.entrySet()) {
                repositoryMap.remove(repositoryEntry.getKey());
                repositoryList.remove(repositoryEntry.getValue());
            }

            defaultRepository = null;
        }

        for (final TFSRepository repository : removedRepositories) {
            getListener().onRepositoryRemoved(new RepositoryManagerEvent(this, repository));
        }

        getListener().onDefaultRepositoryChanged(new RepositoryManagerEvent(this, null));
    }

    public final void removeRepository(final TFSRepository repository) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        final WorkspaceKey key = new WorkspaceKey(repository.getWorkspace());

        boolean wasDefaultRepository = false;
        TFSRepository newDefaultRepository = null;

        synchronized (lock) {
            final TFSRepository testRepository = repositoryMap.get(key);

            /*
             * Sanity check: two repositories can have the same WorkspaceKey but
             * in fact be different
             */
            if (testRepository == null || testRepository != repository) {
                throw new IllegalArgumentException("The specified repository is not in the RepositoryManager"); //$NON-NLS-1$
            }

            repositoryMap.remove(key);
            repositoryList.remove(testRepository);

            if (repository.equals(defaultRepository)) {
                defaultRepository = repositoryList.size() == 0 ? null : repositoryList.get(0);

                wasDefaultRepository = true;
                newDefaultRepository = defaultRepository;
            }
        }

        getListener().onRepositoryRemoved(new RepositoryManagerEvent(this, repository));

        if (wasDefaultRepository) {
            getListener().onDefaultRepositoryChanged(new RepositoryManagerEvent(this, newDefaultRepository));
        }
    }

    private RepositoryManagerListener getListener() {
        return (RepositoryManagerListener) listeners.getListener();
    }

    public static class RepositoryStatusContainer {
        private RepositoryStatus status;

        private void setRepositoryStatus(final RepositoryStatus status) {
            this.status = status;
        }

        public RepositoryStatus getRepositoryStatus() {
            return status;
        }
    }

    public static class RepositoryStatus extends TypesafeEnum {
        public static final RepositoryStatus CREATED = new RepositoryStatus(0);
        public static final RepositoryStatus EXISTING = new RepositoryStatus(1);

        private RepositoryStatus(final int value) {
            super(value);
        }
    }

    private class RepositoryReplaceResults {
        private final boolean isDefaultRepository;
        private final TFSRepository existingRepository;

        public RepositoryReplaceResults(final boolean isDefaultRepository, final TFSRepository existingRepository) {
            this.isDefaultRepository = isDefaultRepository;
            this.existingRepository = existingRepository;
        }

        public boolean isDefaultRepository() {
            return isDefaultRepository;
        }

        public TFSRepository getExistingRepository() {
            return existingRepository;
        }

        public boolean isIdenticalRepository() {
            return false;
        }
    }

    private class ExistingRepositoryReplaceResults extends RepositoryReplaceResults {
        public ExistingRepositoryReplaceResults(final TFSRepository repository) {
            super(false, repository);
        }

        @Override
        public boolean isIdenticalRepository() {
            return true;
        }
    }
}
