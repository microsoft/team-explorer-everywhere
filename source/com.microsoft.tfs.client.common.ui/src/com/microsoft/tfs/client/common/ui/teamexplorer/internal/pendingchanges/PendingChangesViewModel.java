// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.checkinpolicies.ExtensionPointPolicyLoader;
import com.microsoft.tfs.client.common.commands.QueryLocalWorkspacesCommand;
import com.microsoft.tfs.client.common.commands.wit.QueryWorkItemsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheAdapter;
import com.microsoft.tfs.client.common.repository.cache.pendingchange.PendingChangeCacheEvent;
import com.microsoft.tfs.client.common.server.ServerManagerEvent;
import com.microsoft.tfs.client.common.server.ServerManagerListener;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyFailureData;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.core.checkinpolicies.PolicyContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluationCancelledException;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluator;
import com.microsoft.tfs.core.checkinpolicies.PolicyEvaluatorState;
import com.microsoft.tfs.core.checkinpolicies.PolicyFailure;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.events.LocalWorkspaceScanListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeCandidatesChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangesChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangesCountChangedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.VersionControlEventEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceCreatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceDeletedListener;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceEvent.WorkspaceEventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.WorkspaceUpdatedListener;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.SavedCheckin;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkItemCheckedInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.events.WorkItemEventEngine;
import com.microsoft.tfs.core.clients.workitem.events.WorkItemSaveEvent;
import com.microsoft.tfs.core.clients.workitem.events.WorkItemSaveListener;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameter;
import com.microsoft.tfs.core.clients.workitem.query.BatchReadParameterCollection;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.pendingcheckin.AffectedTeamProjects;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class PendingChangesViewModel {
    private final static Log log = LogFactory.getLog(PendingChangesViewModel.class);

    private final Shell workbenchShell;

    // These fields updated when the server changes, null when offline
    private TFSServer server;
    private TFSRepository repository;
    private Workspace workspace;
    private Workspace[] allWorkspaces;

    private PendingChange[] allPendingChanges = new PendingChange[0];
    private PendingChange[] allCandidates = new PendingChange[0];

    private List<PendingChange> filteredIncludedChanges = new ArrayList<PendingChange>();
    private List<PendingChange> filteredExcludedChanges = new ArrayList<PendingChange>();

    private int numIncludedUnfilteredChanges;
    private int numExcludedUnfilteredChanges;

    private final Map<Integer, WorkItemCheckinInfo> associatedWorkItems = new HashMap<Integer, WorkItemCheckinInfo>();

    private final AffectedTeamProjects affectedTeamProjects = new AffectedTeamProjects();

    private boolean includedFilterEnabled = false;
    private boolean excludedFilterEnabled = false;

    private String includedFilterText;
    private String excludedFilterText;

    private String comment = ""; //$NON-NLS-1$
    private String uncommittedComment = ""; //$NON-NLS-1$

    private CheckinNoteFieldDefinition[] checkinNoteFieldDefinitions = new CheckinNoteFieldDefinition[0];
    private final Map<String, CheckinNoteFieldValue> checkinNoteValues = new HashMap<String, CheckinNoteFieldValue>();

    // Evaluator and context are null when offline
    private PolicyEvaluator policyEvaluator;
    private PolicyContext policyContext;
    private PolicyFailureData[] policyWarnings = new PolicyFailureData[0];

    private final Set<PendingChange> excludedPendingChanges = new HashSet<PendingChange>();

    private final ServerManagerListener serverListener = new ServerListener();
    private final WorkspaceListener workspaceListener = new WorkspaceListener();

    private final CacheListener pendingChangeCacheListener = new CacheListener();
    private final CorePendingChangeListener corePendingChangeListener = new CorePendingChangeListener();
    private final CoreCandidateListener coreCandidateListener = new CoreCandidateListener();
    private final CoreLocalWorkspaceScanListener coreLocalWorkspaceScanListener = new CoreLocalWorkspaceScanListener();
    private final UIWorkItemSaveListener workItemSaveListener = new UIWorkItemSaveListener();

    private final SingleListenerFacade includedPendingChangesListeners =
        new SingleListenerFacade(PendingChangesChangedListener.class);
    private final SingleListenerFacade excludedPendingChangesListeners =
        new SingleListenerFacade(PendingChangesChangedListener.class);
    private final SingleListenerFacade includedChangesCountListeners =
        new SingleListenerFacade(PendingChangesCountChangedListener.class);
    private final SingleListenerFacade excludedChangesCountListeners =
        new SingleListenerFacade(PendingChangesCountChangedListener.class);
    private final SingleListenerFacade candidateListeners =
        new SingleListenerFacade(PendingChangeCandidatesChangedListener.class);
    private final SingleListenerFacade checkinNoteFieldDefinitionsChangedListeners =
        new SingleListenerFacade(CheckinNoteFieldDefinitionsChangedListener.class);
    private final SingleListenerFacade checkinNoteFieldValuesChangedListeners =
        new SingleListenerFacade(CheckinNoteFieldValuesChangedListener.class);
    private final SingleListenerFacade associatedWorkItemsListeners =
        new SingleListenerFacade(AssociatedWorkItemsChangedListener.class);
    private final SingleListenerFacade checkinCommentChangedListeners =
        new SingleListenerFacade(CheckinCommentChangedListener.class);

    private final SingleListenerFacade policyWarningsChangedListeners =
        new SingleListenerFacade(PolicyWarningsChangedListener.class);

    public PendingChangesViewModel(final Shell workbenchShell) {
        this.workbenchShell = workbenchShell;
        final TFSProductPlugin plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin();

        plugin.getServerManager().addListener(serverListener);

        initialize();
    }

    /**
     * Sets the {@link PendingChangesViewModel}'s connection-oriented data from
     * the UI client plugin's current default server and repository.
     * <p>
     * Call whenever the default connection information changes, including when
     * offline.
     */
    public void initialize() {
        final TFSProductPlugin plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin();

        final TFSServer newServer = plugin.getServerManager().getDefaultServer();
        final TFSRepository newRepository = plugin.getRepositoryManager().getDefaultRepository();
        final Workspace newWorkspace = newRepository == null ? null : newRepository.getWorkspace();
        allWorkspaces = null;

        if (connectionObjectEqual(server, newServer)
            && connectionObjectEqual(repository, newRepository)
            && connectionObjectEqual(workspace, newWorkspace)) {
            return;
        }

        // Remove listeners from old connection
        removeListeners();

        // The default server, repository, and workspace may be null when
        // offline in a server workspace
        server = newServer;
        repository = newRepository;
        workspace = newWorkspace;

        // Clear the filters.
        includedFilterEnabled = false;
        excludedFilterEnabled = false;
        includedFilterText = ""; //$NON-NLS-1$
        excludedFilterText = ""; //$NON-NLS-1$

        policyEvaluator = null;
        policyContext = null;

        if (repository != null) {
            policyEvaluator =
                new PolicyEvaluator(repository.getVersionControlClient(), new ExtensionPointPolicyLoader());

            policyContext = new PolicyContext();
            policyContext.addProperty(PolicyContextKeys.SWT_SHELL, workbenchShell);
            policyContext.addProperty(PolicyContextKeys.TFS_TEAM_PROJECT_COLLECTION, server.getConnection());
            policyContext.addProperty(PolicyContextKeys.RUNNING_PRODUCT_ECLIPSE_PLUGIN, new Object());
        }

        comment = ""; //$NON-NLS-1$
        associatedWorkItems.clear();
        excludedPendingChanges.clear();

        affectedTeamProjects.set(new PendingChange[0]);
        checkinNoteFieldDefinitions = new CheckinNoteFieldDefinition[0];
        checkinNoteValues.clear();

        // Retrieve all pending changes and candidates
        loadPendingChangesAndCandidates();

        // Get the last save check-in state.
        loadSavedCheckin();

        // Listen to the new connection
        addListeners();
    }

    /**
     * Removes listeners this class has hooked into lower-level plugins and
     * core. Safe to call when not connected.
     */
    private void removeListeners() {
        if (server != null) {
            final VersionControlEventEngine vcEventEngine =
                server.getConnection().getVersionControlClient().getEventEngine();
            vcEventEngine.removeWorkspaceCreatedListener(workspaceListener);
            vcEventEngine.removeWorkspaceDeletedListener(workspaceListener);
            vcEventEngine.removeWorkspaceUpdatedListener(workspaceListener);

            final WorkItemEventEngine wiEventEngine = server.getConnection().getWorkItemClient().getEventEngine();
            wiEventEngine.removeWorkItemSaveListener(workItemSaveListener);
        }

        if (repository != null) {
            repository.getPendingChangeCache().removeListener(pendingChangeCacheListener);
        }

        if (workspace != null) {
            final VersionControlEventEngine vcEventEngine = workspace.getClient().getEventEngine();
            vcEventEngine.removePendingChangesChangedListener(corePendingChangeListener);
            vcEventEngine.removePendingChangeCandidatesChangedListener(coreCandidateListener);
            vcEventEngine.removeLocalWorkspaceScanListener(coreLocalWorkspaceScanListener);
        }
    }

    /**
     * Adds listeners to events fired by lower-level plugins and core. Safe to
     * call when not connected.
     */
    private void addListeners() {
        if (server != null) {
            final VersionControlEventEngine vcEventEngine =
                server.getConnection().getVersionControlClient().getEventEngine();
            vcEventEngine.addWorkspaceCreatedListener(workspaceListener);
            vcEventEngine.addWorkspaceDeletedListener(workspaceListener);
            vcEventEngine.addWorkspaceUpdatedListener(workspaceListener);

            final WorkItemEventEngine wiEventEngine = server.getConnection().getWorkItemClient().getEventEngine();
            wiEventEngine.addWorkItemSaveListener(workItemSaveListener);
        }

        // Pending change cache provides much more efficient notification of the
        // fine-grained events than core
        if (repository != null) {
            repository.getPendingChangeCache().addListener(pendingChangeCacheListener);
        }

        // Listen to coarse-grained pending change events from core.
        if (workspace != null) {
            final VersionControlEventEngine vcEventEngine = workspace.getClient().getEventEngine();
            vcEventEngine.addPendingChangesChangedListener(corePendingChangeListener);
            vcEventEngine.addPendingChangeCandidatesChangedListener(coreCandidateListener);
            vcEventEngine.addLocalWorkspaceScanListener(coreLocalWorkspaceScanListener);
        }
    }

    /**
     * @return the repository or <code>null</code> if offline
     */
    public TFSRepository getRepository() {
        return repository;
    }

    /**
     * @return the workspace or <code>null</code> if offline
     */
    public Workspace getWorkspace() {
        return workspace;
    }

    public Workspace[] getWorkspaces() {
        if (allWorkspaces == null) {
            final QueryLocalWorkspacesCommand queryCommand = new QueryLocalWorkspacesCommand(server.getConnection());
            final IStatus queryStatus = new CommandExecutor().execute(queryCommand);

            if (queryStatus.isOK()) {
                allWorkspaces = queryCommand.getWorkspaces();

                Arrays.sort(allWorkspaces, new Comparator<Workspace>() {
                    @Override
                    public int compare(final Workspace workspace0, final Workspace workspace1) {
                        return String.CASE_INSENSITIVE_ORDER.compare(workspace0.getName(), workspace1.getName());
                    }
                });
            }
        }

        return allWorkspaces;
    }

    public PolicyEvaluatorState evaluateCheckinPolicies(final PendingCheckin pendingCheckin) {
        return evaluateCheckinPolicies(pendingCheckin, null);
    }

    public PolicyEvaluatorState evaluateCheckinPolicies(
        final PendingCheckin pendingCheckin,
        final AtomicReference<PolicyFailure[]> outFailures) {
        if (policyEvaluator == null) {
            return PolicyEvaluatorState.UNEVALUATED;
        }

        Check.notNull(policyContext, "policyContext"); //$NON-NLS-1$

        PolicyFailure[] failures;

        try {
            policyEvaluator.setPendingCheckin(pendingCheckin);
            failures = policyEvaluator.evaluate(policyContext);
        } catch (final PolicyEvaluationCancelledException e) {
            return PolicyEvaluatorState.CANCELLED;
        }
        policyWarnings = PolicyFailureData.fromPolicyFailures(failures, policyContext);
        firePolicyWarningsChangedEvent();

        if (outFailures != null) {
            outFailures.set(failures);
        }

        return policyEvaluator.getPolicyEvaluatorState();
    }

    public CheckinNote getCheckinNote() {
        final List<CheckinNoteFieldValue> values = new ArrayList<CheckinNoteFieldValue>();

        for (final CheckinNoteFieldDefinition definition : checkinNoteFieldDefinitions) {
            if (checkinNoteValues.containsKey(definition.getName())) {
                values.add(checkinNoteValues.get(definition.getName()));
            }
        }

        return new CheckinNote(values.toArray(new CheckinNoteFieldValue[values.size()]));
    }

    public void setCheckinNoteFieldValues(final CheckinNoteFieldValue[] values) {
        checkinNoteValues.clear();

        if (values != null) {
            for (final CheckinNoteFieldValue value : values) {
                checkinNoteValues.put(value.getName(), value);
            }
        }

        updateLastSavedCheckin();
        fireCheckinNoteFieldValuesChangedEvent();
    }

    public void clearCheckinNotes() {
        checkinNoteValues.clear();
        updateLastSavedCheckin();
        fireCheckinNoteFieldValuesChangedEvent();
    }

    public void clearPolicyWarnings() {
        if (policyWarnings != null && policyWarnings.length > 0) {
            policyWarnings = new PolicyFailureData[0];
            firePolicyWarningsChangedEvent();
        }
    }

    public int getCheckinNoteFieldDefinitionCount() {
        return checkinNoteFieldDefinitions.length;
    }

    public CheckinNoteFieldDefinition[] getCheckinNoteFieldDefinitions() {
        return checkinNoteFieldDefinitions;
    }

    public String getCheckinNoteFieldValue(final String fieldName) {
        final CheckinNoteFieldValue fieldValue = checkinNoteValues.get(fieldName);
        return fieldValue == null ? null : fieldValue.getValue();
    }

    public int getPolicyWarningsCount() {
        return (policyWarnings == null) ? 0 : policyWarnings.length;
    }

    public PolicyFailureData[] getPolicyWarnings() {
        return policyWarnings;
    }

    public void clearComment() {
        if (comment.length() > 0) {
            setComment(""); //$NON-NLS-1$
        }
    }

    /*
     * Set the comment but don't commit it to the local cache and don't raise
     * comment changed events. The value of this comment will be automatically
     * committed on an attempt to read the cached comment or cleared if a value
     * is explicitly committed.
     */
    public void setUncommittedComment(final String value) {
        uncommittedComment = value;
    }

    public void setComment(String value) {
        if (value == null) {
            value = ""; //$NON-NLS-1$
        }

        // If the comment has changed, write it to the local cache and fire the
        // comment changed event.
        if (!comment.equals(value)) {
            comment = value;
            updateLastSavedCheckin();
            fireCheckinCommentChangedEvent(value);
        }

        // Clear the uncommitted comment.
        uncommittedComment = ""; //$NON-NLS-1$
    }

    public String getComment() {
        // If there is a pending uncommitted comment we commit it now. An
        // uncommitted comment may have been set as a light weight way of
        // storing the current comment without incurring the expensive
        // processing of writing it to the local disk cache and firing the
        // comment changed events.
        if (uncommittedComment.length() > 0) {
            setComment(uncommittedComment);
            uncommittedComment = ""; //$NON-NLS-1$
        }

        // Return the committed comment.
        return comment == null ? "" : comment; //$NON-NLS-1$
    }

    public void setCheckinNoteFieldValue(final String name, final String text) {
        if (checkinNoteValues.containsKey(name)) {
            final CheckinNoteFieldValue currentFieldValue = checkinNoteValues.get(name);
            if (!currentFieldValue.getValue().equals(text)) {
                checkinNoteValues.get(name).setValue(text);
                updateLastSavedCheckin();
                fireCheckinNoteFieldValuesChangedEvent();
            }
        } else {
            final CheckinNoteFieldValue value = new CheckinNoteFieldValue(name, text);
            checkinNoteValues.put(name, value);
            updateLastSavedCheckin();
            fireCheckinNoteFieldValuesChangedEvent();
        }

    }

    public WorkItemCheckinInfo[] getAssociatedWorkItems() {
        return associatedWorkItems.values().toArray(new WorkItemCheckinInfo[associatedWorkItems.size()]);
    }

    public int getAssociatedWorkItemCount() {
        return associatedWorkItems.size();
    }

    /**
     * Associates the specified work item. The default action is used. If the
     * work item was already associated, its check-in action is not changed.
     *
     * @param workItem
     *        the work item to associate (must not be <code>null</code>)
     */
    public void associateWorkItem(final WorkItem workItem) {
        associateWorkItems(new WorkItem[] {
            workItem
        });
    }

    /**
     * Associates the specified work items. The default action is used. If some
     * work items were already associated, their check-in actions are not
     * changed.
     *
     * @param workItems
     *        the work items to associate (must not be <code>null</code>)
     */
    public void associateWorkItems(final WorkItem[] workItems) {
        boolean added = false;

        for (final WorkItem workItem : workItems) {
            if (!associatedWorkItems.containsKey(workItem.getID())) {
                final WorkItemCheckinInfo checkinInfo = new WorkItemCheckinInfo(workItem);
                checkinInfo.setActionToDefault();
                associatedWorkItems.put(workItem.getID(), checkinInfo);
                added = true;
            }
        }

        if (added) {
            fireAssociatedWorkItemsChangedEvent();
            updateLastSavedCheckin();
        }
    }

    /**
     * Associates the specified work items. If some work items were already
     * associated, their check-in actions are <b>replaced</b> by the actions in
     * the specified work items (this is normal unshelve behavior).
     *
     * @param workItemInfos
     *        the work items to associate (must not be <code>null</code>)
     */
    public void associateWorkItems(final WorkItemCheckinInfo[] workItemInfos) {
        boolean added = false;

        for (final WorkItemCheckinInfo info : workItemInfos) {
            associatedWorkItems.put(info.getWorkItem().getID(), info);
            added = true;
        }

        if (added) {
            fireAssociatedWorkItemsChangedEvent();
            updateLastSavedCheckin();
        }
    }

    public void dissociateAllWorkItems() {
        if (associatedWorkItems.size() > 0) {
            final Collection<WorkItemCheckinInfo> values = associatedWorkItems.values();
            final WorkItemCheckinInfo[] workItemCheckinInfos = values.toArray(new WorkItemCheckinInfo[values.size()]);
            dissociateWorkItems(workItemCheckinInfos);
        }
    }

    public void dissociateWorkItems(final WorkItemCheckinInfo[] workItemCheckinInfos) {
        boolean removed = false;

        for (final WorkItemCheckinInfo workItemCheckinInfo : workItemCheckinInfos) {
            if (associatedWorkItems.containsKey(workItemCheckinInfo.getWorkItem().getID())) {
                final WorkItem workItem = workItemCheckinInfo.getWorkItem();
                associatedWorkItems.remove(workItem.getID());
                removed = true;
            }
        }

        if (removed) {
            fireAssociatedWorkItemsChangedEvent();
            updateLastSavedCheckin();
        }
    }

    public boolean isLocalWorkspace() {
        return workspace != null && workspace.getLocation() == WorkspaceLocation.LOCAL;
    }

    public void addAssociatedWorkItemsChangedListener(final AssociatedWorkItemsChangedListener listener) {
        associatedWorkItemsListeners.addListener(listener);
    }

    public void removeAssociatedWorkItemsChangedListener(final AssociatedWorkItemsChangedListener listener) {
        associatedWorkItemsListeners.removeListener(listener);
    }

    public void addCheckinCommentChangedListener(final CheckinCommentChangedListener listener) {
        checkinCommentChangedListeners.addListener(listener);
    }

    public void removeCheckinCommentChangedListener(final CheckinCommentChangedListener listener) {
        checkinCommentChangedListeners.removeListener(listener);
    }

    public void addPolicyWarningsChangedListener(final PolicyWarningsChangedListener listener) {
        policyWarningsChangedListeners.addListener(listener);
    }

    public void removePolicyWarningsChangedListener(final PolicyWarningsChangedListener listener) {
        policyWarningsChangedListeners.removeListener(listener);
    }

    public void addCheckinNoteFieldValuesChangedListener(final CheckinNoteFieldValuesChangedListener listener) {
        checkinNoteFieldValuesChangedListeners.addListener(listener);
    }

    public void removeCheckinNoteFieldValuesChangedListener(final CheckinNoteFieldValuesChangedListener listener) {
        checkinNoteFieldValuesChangedListeners.removeListener(listener);
    }

    public void addIncludedPendingChangesChangedListener(final PendingChangesChangedListener listener) {
        includedPendingChangesListeners.addListener(listener);
    }

    public void removeIncludedPendingChangesChangedListener(final PendingChangesChangedListener listener) {
        includedPendingChangesListeners.removeListener(listener);
    }

    public void addExcludedPendingChangesChangedListener(final PendingChangesChangedListener listener) {
        excludedPendingChangesListeners.addListener(listener);
    }

    public void removeExcludedPendingChangesChangedListener(final PendingChangesChangedListener listener) {
        excludedPendingChangesListeners.removeListener(listener);
    }

    public void addIncludedPendingChangesCountChangedListener(final PendingChangesCountChangedListener listener) {
        includedChangesCountListeners.addListener(listener);
    }

    public void removeIncludedPendingChangesCountChangedListener(final PendingChangesCountChangedListener listener) {
        includedChangesCountListeners.removeListener(listener);
    }

    public void addExcludedPendingChangesCountChangedListener(final PendingChangesCountChangedListener listener) {
        excludedChangesCountListeners.addListener(listener);
    }

    public void removeExcludedPendingChangesCountChangedListener(final PendingChangesCountChangedListener listener) {
        excludedChangesCountListeners.removeListener(listener);
    }

    public void addPendingChangeCandidatesChangedListener(final PendingChangeCandidatesChangedListener listener) {
        candidateListeners.addListener(listener);
    }

    public void removePendingChangeCandidatesChangedListener(final PendingChangeCandidatesChangedListener listener) {
        candidateListeners.removeListener(listener);
    }

    public void addCheckinNoteFieldDefinitionsChangedListener(
        final CheckinNoteFieldDefinitionsChangedListener listener) {
        checkinNoteFieldDefinitionsChangedListeners.addListener(listener);
    }

    public void removeCheckinNoteFieldDefinitionsChangedListener(
        final CheckinNoteFieldDefinitionsChangedListener listener) {
        checkinNoteFieldDefinitionsChangedListeners.removeListener(listener);
    }

    public boolean getIncludedFilterEnabled() {
        return includedFilterEnabled;
    }

    public boolean getExcludedFilterEnabled() {
        return excludedFilterEnabled;
    }

    public void setIncludedFilterEnabled(final boolean enabled) {
        if (includedFilterEnabled != enabled) {
            includedFilterEnabled = enabled;
            if (includedFilterText != null && includedFilterText.length() > 0) {
                updateFilteredIncludedChanges();
            }
        }
    }

    public void setExcludedFilterVisible(final boolean enabled) {
        if (excludedFilterEnabled != enabled) {
            excludedFilterEnabled = enabled;
            if (excludedFilterText != null && excludedFilterText.length() > 0) {
                updateFilteredExcludedChanges();
            }
        }
    }

    public String getIncludedFilterText() {
        return includedFilterText;
    }

    public String getExcludedFilterText() {
        return excludedFilterText;
    }

    public void setIncludedFilterText(final String filterText) {
        includedFilterText = filterText;
        updateFilteredIncludedChanges();
    }

    public void setExcludedFilterText(final String filterText) {
        excludedFilterText = filterText;
        updateFilteredExcludedChanges();
    }

    private void loadPendingChangesAndCandidates() {
        if (workspace == null) {
            allPendingChanges = new PendingChange[0];
            updateCandidates(new PendingChange[0]);
            // Also updates checkin note field definitions
            updateIncludedExcludedAndAffected();
            return;
        }

        final long start = System.currentTimeMillis();

        final ItemSpec[] itemSpecs = new ItemSpec[1];
        itemSpecs[0] = new ItemSpec(ServerPath.ROOT, RecursionType.FULL);
        final AtomicReference<PendingChange[]> outCandidateChanges = new AtomicReference<PendingChange[]>();

        allPendingChanges = workspace.getPendingChangesWithCandidates(itemSpecs, false, outCandidateChanges);
        updateCandidates(outCandidateChanges.get());

        updateIncludedExcludedAndAffected();

        log.debug(MessageFormat.format(
            "Load changes including candidates took {0} ms", //$NON-NLS-1$
            (System.currentTimeMillis() - start)));
    }

    private void loadSavedCheckin() {
        if (workspace == null) {
            updateIncludedExcludedAndAffected();
            dissociateAllWorkItems();
            clearCheckinNotes();
            return;
        }

        final SavedCheckin savedCheckin = workspace.getLastSavedCheckin();

        if (savedCheckin != null) {
            // Set the comment from the last saved check-in.
            comment = savedCheckin.getComment();
            if (comment == null) {
                comment = ""; //$NON-NLS-1$
            }
            fireCheckinCommentChangedEvent(comment);

            // Set the excluded pending changes.
            final String[] excludedPaths = savedCheckin.getExcludedServerPaths();

            if (excludedPaths != null && excludedPaths.length > 0) {
                final Set<String> set = new HashSet<String>(Arrays.asList(excludedPaths));

                for (final PendingChange pendingChange : allPendingChanges) {
                    if (set.contains(pendingChange.getServerItem())) {
                        excludedPendingChanges.add(pendingChange);
                    }
                }

                // Update the include, exclude, and effected lists.
                updateIncludedExcludedAndAffected();
            }

            // Set the associated work items
            initializeAssociatedWorkItems(savedCheckin.getWorkItemsCheckedInfo());

            // Set the check-in notes.
            initializeCheckinNote(savedCheckin.getCheckinNotes());
        }
    }

    private void initializeAssociatedWorkItems(final WorkItemCheckedInfo[] workItemInfos) {
        if (workItemInfos == null || workItemInfos.length == 0) {
            return;
        }

        final BatchReadParameterCollection batchReadParams = new BatchReadParameterCollection();
        final Map<Integer, CheckinWorkItemAction> map = new HashMap<Integer, CheckinWorkItemAction>();

        for (final WorkItemCheckedInfo workItemInfo : workItemInfos) {
            if (workItemInfo.isChecked()) {
                map.put(workItemInfo.getID(), workItemInfo.getCheckinAction());
                batchReadParams.add(new BatchReadParameter(workItemInfo.getID()));
            }
        }

        final WorkItemClient client = server.getConnection().getWorkItemClient();

        final String[] workItemFieldNames = new String[] {
            CoreFieldReferenceNames.ID,
            CoreFieldReferenceNames.TITLE,
            CoreFieldReferenceNames.STATE,
            CoreFieldReferenceNames.CREATED_BY,
            CoreFieldReferenceNames.CREATED_DATE,
            CoreFieldReferenceNames.ASSIGNED_TO,
            CoreFieldReferenceNames.CHANGED_DATE,
            CoreFieldReferenceNames.AREA_PATH,
            CoreFieldReferenceNames.ITERATION_PATH,
        };

        final StringBuilder sb = new StringBuilder();
        sb.append("select "); //$NON-NLS-1$

        boolean needComma = false;
        for (final String workItemFieldName : workItemFieldNames) {
            sb.append(needComma ? ",[" : "["); //$NON-NLS-1$ //$NON-NLS-2$
            sb.append(workItemFieldName);
            sb.append("]"); //$NON-NLS-1$
            needComma = true;
        }

        sb.append(" from workitems"); //$NON-NLS-1$
        final Query query = client.createQuery(sb.toString(), batchReadParams);

        final QueryWorkItemsCommand command = new QueryWorkItemsCommand(query);
        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(workbenchShell);
        final IStatus status = executor.execute(command);

        if (status.isOK() && command.getWorkItems() != null) {
            final WorkItem[] workItems = command.getWorkItems();

            if (workItems != null) {
                for (final WorkItem workItem : workItems) {
                    final WorkItemCheckinInfo info = new WorkItemCheckinInfo(workItem, map.get(workItem.getID()));
                    associatedWorkItems.put(workItem.getID(), info);
                }
            }
        }
    }

    private void initializeCheckinNote(final CheckinNote checkinNote) {
        setCheckinNoteFieldValues(checkinNote == null ? null : checkinNote.getValues());
    }

    private void updateIncludedExcludedAndAffected() {
        updateFilteredExcludedChanges();
        updateFilteredIncludedChanges();
        updateAffectedProjects();
    }

    private void updateCheckinNoteFieldDefinitions(final String[] teamProjectPaths) {
        final CheckinNoteFieldDefinition[] definitions;

        if ((workspace != null
            && workspace.getLocation() == WorkspaceLocation.LOCAL
            && workspace.getClient().getConnection().getConnectivityFailureOnLastWebServiceCall())
            || teamProjectPaths == null
            || teamProjectPaths.length == 0) {
            definitions = new CheckinNoteFieldDefinition[0];
        } else {
            final VersionControlClient client = repository.getVersionControlClient();
            final SortedSet<CheckinNoteFieldDefinition> set =
                client.queryCheckinNoteFieldDefinitionsForServerPaths(teamProjectPaths);

            definitions = set.toArray(new CheckinNoteFieldDefinition[set.size()]);
        }

        if (checkinNotesDiffer(checkinNoteFieldDefinitions, definitions)) {
            checkinNoteFieldDefinitions = definitions;
            fireCheckinNoteFieldDefinitionsChangedEvent();
        }
    }

    public void updateFilteredIncludedChanges() {
        final List<PendingChange> changes = getPendingChanges(false);
        final int numChanges = changes.size();

        if (includedFilterEnabled) {
            filterPendingChanges(changes, includedFilterText);
        }

        if (changesDiffer(changes, filteredIncludedChanges)) {
            filteredIncludedChanges = changes;
            fireIncludedChangesChangedEvent();
        }

        if (numChanges != numIncludedUnfilteredChanges) {
            numIncludedUnfilteredChanges = numChanges;
            fireIncludedPendingChangesCountChangedEvent();
        }
    }

    private boolean checkinNotesDiffer(
        final CheckinNoteFieldDefinition[] notes1,
        final CheckinNoteFieldDefinition[] notes2) {
        Check.notNull(notes1, "notes1"); //$NON-NLS-1$
        Check.notNull(notes2, "notes2"); //$NON-NLS-1$

        if (notes1.length != notes2.length) {
            return true;
        }

        final Set<CheckinNoteFieldDefinition> set = new HashSet<CheckinNoteFieldDefinition>();

        for (final CheckinNoteFieldDefinition note : notes1) {
            set.add(note);
        }

        for (final CheckinNoteFieldDefinition note : notes2) {
            if (!set.contains(note)) {
                return true;
            }
        }

        return false;
    }

    private boolean changesDiffer(final List<PendingChange> changes1, final List<PendingChange> changes2) {
        Check.notNull(changes1, "changes1"); //$NON-NLS-1$
        Check.notNull(changes2, "changes2"); //$NON-NLS-1$

        if (changes1.size() != changes2.size()) {
            return true;
        }

        return changesDiffer(
            changes1.toArray(new PendingChange[changes1.size()]),
            changes2.toArray(new PendingChange[changes2.size()]));
    }

    private boolean changesDiffer(final PendingChange[] changes1, final PendingChange[] changes2) {
        Check.notNull(changes1, "changes1"); //$NON-NLS-1$
        Check.notNull(changes2, "changes2"); //$NON-NLS-1$

        if (changes1.length != changes2.length) {
            return true;
        }

        for (final PendingChange change2 : changes2) {
            boolean found = false;
            for (final PendingChange change1 : changes1) {
                if (change2.equalsIgnoringLockLevelAndVersion(change1)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return true;
            }
        }

        return false;
    }

    private void updateFilteredExcludedChanges() {
        final List<PendingChange> changes = getPendingChanges(true);
        final int numChanges = changes.size();

        if (excludedFilterEnabled) {
            filterPendingChanges(changes, excludedFilterText);
        }

        if (changesDiffer(changes, filteredExcludedChanges)) {
            filteredExcludedChanges = changes;
            fireExcludedChangesChangedEvent();
        }

        if (numChanges != numExcludedUnfilteredChanges) {
            numExcludedUnfilteredChanges = numChanges;
            fireExcludedPendingChangesCountChangedEvent();
        }
    }

    private void updateAffectedProjects() {
        if (affectedTeamProjects.set(getIncludedPendingChanges())) {
            updateCheckinNoteFieldDefinitions(affectedTeamProjects.getTeamProjectPaths());
        }
    }

    private void updateCandidates(final PendingChange[] newCandidates) {
        if (changesDiffer(newCandidates, allCandidates)) {
            allCandidates = newCandidates;
            firePendingChangesCandidatesChangedEvent();
        }
    }

    private void fireAssociatedWorkItemsChangedEvent() {
        ((AssociatedWorkItemsChangedListener) associatedWorkItemsListeners.getListener()).onAssociatedWorkItemsChanged();
    }

    private void fireIncludedChangesChangedEvent() {
        ((PendingChangesChangedListener) includedPendingChangesListeners.getListener()).onPendingChangesChanged(null);
    }

    private void fireExcludedChangesChangedEvent() {
        ((PendingChangesChangedListener) excludedPendingChangesListeners.getListener()).onPendingChangesChanged(null);
    }

    private void fireIncludedPendingChangesCountChangedEvent() {
        ((PendingChangesCountChangedListener) includedChangesCountListeners.getListener()).onPendingChangesCountChanged(
            null);
    }

    private void fireExcludedPendingChangesCountChangedEvent() {
        ((PendingChangesCountChangedListener) excludedChangesCountListeners.getListener()).onPendingChangesCountChanged(
            null);
    }

    private void firePendingChangesCandidatesChangedEvent() {
        ((PendingChangeCandidatesChangedListener) candidateListeners.getListener()).onPendingChangeCandidatesChanged(
            null);
    }

    private void fireCheckinNoteFieldDefinitionsChangedEvent() {
        ((CheckinNoteFieldDefinitionsChangedListener) checkinNoteFieldDefinitionsChangedListeners.getListener()).onCheckinNoteFieldDefinitionsChanged();
    }

    private void fireCheckinCommentChangedEvent(final String comment) {
        ((CheckinCommentChangedListener) checkinCommentChangedListeners.getListener()).onCheckinCommentChanged(comment);
    }

    private void fireCheckinNoteFieldValuesChangedEvent() {
        ((CheckinNoteFieldValuesChangedListener) checkinNoteFieldValuesChangedListeners.getListener()).onCheckinNoteFieldValuesChanged();
    }

    private void firePolicyWarningsChangedEvent() {
        ((PolicyWarningsChangedListener) policyWarningsChangedListeners.getListener()).onPolicyWarningsChanged();
    }

    public int getCandidateChangeCount() {
        return allCandidates.length;
    }

    public PendingChange[] getAllPendingChanges() {
        return allPendingChanges;
    }

    public PendingChange[] getIncludedPendingChanges() {
        return listToArray(filteredIncludedChanges);
    }

    public PendingChange[] getExcludedPendingChanges() {
        return listToArray(filteredExcludedChanges);
    }

    public PendingChange[] getIncludedUnfilteredPendingChanges() {
        return listToArray(getPendingChanges(false));
    }

    public int getIncludedUnfilteredChangeCount() {
        return numIncludedUnfilteredChanges;
    }

    public int getExcludedUnfilteredChangeCount() {
        return numExcludedUnfilteredChanges;
    }

    public int getIncludedFilteredChangeCount() {
        return filteredIncludedChanges.size();
    }

    public int getExcludedFilteredChangeCount() {
        return filteredExcludedChanges.size();
    }

    public PendingChange[] getCandidatePendingChanges() {
        return allCandidates;
    }

    public void includeAllPendingChanges() {
        includePendingChanges(allPendingChanges);
    }

    public void excludeAllPendingChanges() {
        excludePendingChanges(allPendingChanges);
    }

    public void includePendingChanges(final PendingChange[] changesToInclude) {
        if (changesToInclude == null || changesToInclude.length == 0) {
            return;
        }

        for (final PendingChange change : changesToInclude) {
            excludedPendingChanges.remove(change);
        }

        updateIncludedExcludedAndAffected();
        updateLastSavedCheckin();
    }

    public void excludePendingChanges(final PendingChange[] changesToExclude) {
        if (changesToExclude == null) {
            return;
        }

        for (final PendingChange change : changesToExclude) {
            excludedPendingChanges.add(change);
        }

        updateIncludedExcludedAndAffected();
        updateLastSavedCheckin();
    }

    public void updateLastSavedCheckin() {
        if (workspace != null) {
            final SavedCheckin newSavedCheckin = new SavedCheckin(
                comment,
                excludedPendingChanges.toArray(new PendingChange[excludedPendingChanges.size()]),
                getCheckinNote(),
                getWorkItemCheckedInfos(),
                null);

            workspace.setLastSavedCheckin(newSavedCheckin);
        }
    }

    private List<PendingChange> getPendingChanges(final boolean excludedChanges) {
        final List<PendingChange> list = new ArrayList<PendingChange>();
        for (final PendingChange pendingChange : allPendingChanges) {
            final boolean isExcluded = excludedPendingChanges.contains(pendingChange);

            if (excludedChanges && isExcluded || !excludedChanges && !isExcluded) {
                list.add(pendingChange);
            }
        }
        return list;
    }

    private void filterPendingChanges(final List<PendingChange> list, String filterText) {
        if (filterText == null || filterText.length() == 0) {
            return;
        }

        filterText = filterText.trim();
        filterText = filterText.replace("\"", ""); //$NON-NLS-1$//$NON-NLS-2$
        final boolean isWildCard = Wildcard.isWildcard(filterText);

        Pattern caseInsensitivePattern = null;
        if (isWildCard) {
            /*
             * Our wildcard matcher, FileHelpers.filenameMatches, considers
             * backslashes in the pattern to be escape characters, so double
             * them up.
             */
            filterText = filterText.replace("\\", "\\\\"); //$NON-NLS-1$//$NON-NLS-2$
        } else {
            /*
             * For non-wildcards, use a regular expression to get
             * case-insensitive substring search.
             */
            caseInsensitivePattern = Pattern.compile(Pattern.quote(filterText), Pattern.CASE_INSENSITIVE);
        }

        for (int i = list.size() - 1; i >= 0; i--) {
            final PendingChange change = list.get(i);

            String item = change.getLocalItem();
            if (item == null) {
                item = change.getSourceLocalItem();
            }
            if (item == null) {
                item = change.getServerItem();
            }

            if (isWildCard) {
                /*
                 * Wildcards are anchored at the beginning and end of the string
                 * (it's not a substring search). If we don't find a match
                 * against the full path, try matching just the file name.
                 */

                // Match the whole path
                boolean isMatch = FileHelpers.filenameMatches(item, filterText, true);

                // Match the file name
                if (!isMatch) {
                    String fileName;
                    if (ServerPath.isServerPath(item)) {
                        fileName = ServerPath.getFileName(item);
                    } else {
                        fileName = LocalPath.getFileName(item);
                    }

                    isMatch = FileHelpers.filenameMatches(fileName, filterText, true);
                }

                if (!isMatch) {
                    list.remove(i);
                }
            } else {
                // Use the case-insensitive expression pattern.
                if (!caseInsensitivePattern.matcher(item).find()) {
                    list.remove(i);
                }
            }
        }
    }

    private WorkItemCheckedInfo[] getWorkItemCheckedInfos() {
        final List<WorkItemCheckedInfo> list = new ArrayList<WorkItemCheckedInfo>();

        for (final WorkItemCheckinInfo checkinInfo : associatedWorkItems.values()) {
            list.add(new WorkItemCheckedInfo(checkinInfo.getWorkItem().getID(), true, checkinInfo.getAction()));
        }

        return list.toArray(new WorkItemCheckedInfo[list.size()]);
    }

    private static PendingChange[] listToArray(final List<PendingChange> list) {
        return list == null ? new PendingChange[0] : list.toArray(new PendingChange[list.size()]);
    }

    private boolean connectionObjectEqual(final Object a, final Object b) {
        if (a == b) {
            return true;
        } else if (a == null || b == null) {
            return false;
        }

        return a.equals(b);
    }

    private final class WorkspaceListener
        implements WorkspaceCreatedListener, WorkspaceUpdatedListener, WorkspaceDeletedListener {
        @Override
        public void onWorkspaceDeleted(final WorkspaceEvent e) {
            allWorkspaces = null;
        }

        @Override
        public void onWorkspaceUpdated(final WorkspaceUpdatedEvent e) {
            allWorkspaces = null;
        }

        @Override
        public void onWorkspaceCreated(final WorkspaceEvent e) {
            allWorkspaces = null;
        }
    }

    private final class ServerListener implements ServerManagerListener {
        @Override
        public void onServerAdded(final ServerManagerEvent event) {
            final TFSServer server = event.getServer();
            final VersionControlEventEngine eventEngine =
                server.getConnection().getVersionControlClient().getEventEngine();
            eventEngine.addWorkspaceCreatedListener(workspaceListener);
            eventEngine.addWorkspaceDeletedListener(workspaceListener);
            eventEngine.addWorkspaceUpdatedListener(workspaceListener);
        }

        @Override
        public void onServerRemoved(final ServerManagerEvent event) {
            final TFSServer server = event.getServer();
            final VersionControlEventEngine eventEngine =
                server.getConnection().getVersionControlClient().getEventEngine();
            eventEngine.removeWorkspaceCreatedListener(workspaceListener);
            eventEngine.removeWorkspaceDeletedListener(workspaceListener);
            eventEngine.removeWorkspaceUpdatedListener(workspaceListener);
        }

        @Override
        public void onDefaultServerChanged(final ServerManagerEvent event) {
            allWorkspaces = null;
        }
    }

    private class CacheListener extends PendingChangeCacheAdapter {
        @Override
        public void onAfterUpdatePendingChanges(
            final PendingChangeCacheEvent event,
            final boolean modifiedDuringOperation) {
            if (modifiedDuringOperation) {
                loadPendingChangesAndCandidates();
            }
        }
    }

    private class CorePendingChangeListener implements PendingChangesChangedListener {
        @Override
        public void onPendingChangesChanged(final WorkspaceEvent e) {
            if (e.getWorkspaceSource() == WorkspaceEventSource.EXTERNAL) {
                loadPendingChangesAndCandidates();
            }
        }
    }

    private class CoreCandidateListener implements PendingChangeCandidatesChangedListener {
        @Override
        public void onPendingChangeCandidatesChanged(final WorkspaceEvent e) {
            loadPendingChangesAndCandidates();
            firePendingChangesCandidatesChangedEvent();
        }
    }

    private class CoreLocalWorkspaceScanListener implements LocalWorkspaceScanListener {
        @Override
        public void onLocalWorkspaceScan(final WorkspaceEvent e) {
            if (e.getWorkspaceSource() == WorkspaceEventSource.EXTERNAL) {
                loadPendingChangesAndCandidates();
                firePendingChangesCandidatesChangedEvent();
            }
        }
    }

    private class UIWorkItemSaveListener implements WorkItemSaveListener {
        @Override
        public void onWorkItemSave(final WorkItemSaveEvent e) {
            final int id = e.getWorkItem().getID();

            final WorkItemCheckinInfo oldCheckinInfo = associatedWorkItems.get(id);
            if (oldCheckinInfo != null) {
                // Replace the association with a new checkin info that contains
                // the updated work item

                final WorkItemCheckinInfo newCheckinInfo =
                    new WorkItemCheckinInfo(e.getWorkItem(), oldCheckinInfo.getAction());

                associatedWorkItems.put(id, newCheckinInfo);

                fireAssociatedWorkItemsChangedEvent();
            }
        }
    }
}
