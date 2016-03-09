// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ChangePendedFlags;
import com.microsoft.tfs.core.clients.versioncontrol.ClientLocalVersionUpdate;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.ProcessType;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueue;
import com.microsoft.tfs.core.clients.versioncontrol.UpdateLocalVersionQueueOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.GetDownloadWorker;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.GetEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.MergingEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.PendingChangeEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.DestroyedContentUnavailableException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService.ExecutionExceptionHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.concurrent.AccountingCompletionService.ResultProcessor;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttribute;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeNames;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeValues;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesEntry;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesFile;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.StringPairFileAttribute;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalVersionTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceProperties;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.LocalWorkspaceTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLock;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspacePropertiesTransaction;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceVersionTable;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RequestType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.internal.AppleSingleUtil;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.jni.helpers.FileCopyHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.HashUtils;
import com.microsoft.tfs.util.NewlineUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.datetime.DotNETDate;
import com.microsoft.tfs.util.shutdown.ShutdownEventListener;
import com.microsoft.tfs.util.shutdown.ShutdownManager;
import com.microsoft.tfs.util.shutdown.ShutdownManager.Priority;
import com.microsoft.tfs.util.tasks.CanceledException;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * <p>
 * Processes {@link GetOperation}s to complete workspace operations (get, pend
 * changes, undo, etc.).
 * </p>
 *
 * @since TEE-SDK-10.1
 */
public final class GetEngine {
    /**
     * The number of GetOperations to process between checking whether or not
     * the thread should yield the WorkspaceLock (local workspaces only).
     */
    private static final int PROCESS_GET_OPERATIONS_UNIT_OF_WORK = 24;

    private static final Priority SHUTDOWN_HANDLER_PRIORITY = Priority.EARLY;

    private static final Log log = LogFactory.getLog(GetEngine.class);

    private final VersionControlClient client;

    private static FileAttributesCollection defaultFileAttributes;
    private List<FileAttributesEntry> globalAttributeEntries = null;

    private static final String EOL_CLIENT_ENVIRONMENT_VARIABLE = "TF_EOL_CLIENT"; //$NON-NLS-1$
    private static final String EOL_SERVER_ENVIRONMENT_VARIABLE = "TF_EOL_SERVER"; //$NON-NLS-1$

    /**
     * A mapping between local directory paths (String) and a {@link List} of
     * {@link FileAttributesEntry}. Populated by
     * {@link #processSingleGetOperation(GetEngineState, GetOperation, GetOptions)}
     * so subsequent invocations of that method can skip loading the entries
     * from disk for paths already visited. If a key exists in this mapping and
     * its value is null, this indicates the file attributes file was not
     * present at that directory and no load should be attempted for the
     * lifetime of this {@link GetEngine} (for a speed gain).
     * <p>
     * To guarantee thread-safety, only access this cache via
     * {@link #getAttributesEntryForFile(String)}.
     */
    private final Map<String, List<FileAttributesEntry>> localDirectoryToEntryListMap =
        new HashMap<String, List<FileAttributesEntry>>();

    static {
        try {
            final List<FileAttribute> attributes = new ArrayList<FileAttribute>();

            final String defaultClientEol =
                PlatformMiscUtils.getInstance().getEnvironmentVariable(EOL_CLIENT_ENVIRONMENT_VARIABLE);
            if (defaultClientEol != null) {
                attributes.add(new StringPairFileAttribute(FileAttributeNames.CLIENT_EOL, defaultClientEol));
            }

            final String defaultServerEol =
                PlatformMiscUtils.getInstance().getEnvironmentVariable(EOL_SERVER_ENVIRONMENT_VARIABLE);
            if (defaultServerEol != null) {
                attributes.add(new StringPairFileAttribute(FileAttributeNames.SERVER_EOL, defaultServerEol));
            }

            if (attributes.size() > 0) {
                defaultFileAttributes =
                    new FileAttributesCollection(attributes.toArray(new FileAttribute[attributes.size()]));
            }
        } catch (final Exception e) {
            log.warn("Could not query default file attributes", e); //$NON-NLS-1$
        }
    }

    /**
     * Construct a {@link GetEngine}.
     *
     * @param client
     *        the client to use during the download (must not be
     *        <code>null</code>)
     */
    public GetEngine(final VersionControlClient client) {
        super();

        Check.notNull(client, "client"); //$NON-NLS-1$

        this.client = client;
    }

    /**
     * Gets the symbolic link destination for the given target local item. The
     * .tpattributes file is consulted for an entry that declares the
     * destination for the given file. Most target local items are not supposed
     * to be links, so null is returned for them. If the given local item is
     * supposed to be a symbolic link but the destination is not mapped, an
     * empty string is returned and the file should not be created at all.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param targetLocalItem
     *        the target local item to get a symbolic link destination for. If
     *        null, null is returned.
     * @param workspace
     *        the workspace to resolve symbolc link paths in (must not be
     *        <code>null</code>)
     * @return the local destination for the given file if it is supposed to be
     *         a symbolic link, null if it is not supposed to be a symbolic link
     *         at all, and empty string if the link destination is not mapped.
     */
    public String getSymbolicLinkDestination(
        final File tempFile,
        final GetOperation operation,
        final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        final String targetLocalItem = operation.getTargetLocalItem();
        final String targetServerItem = operation.getTargetServerItem();

        if (targetLocalItem == null) {
            return null;
        }

        /*
         * Get symbolic link via TFS property first
         */
        if (PlatformMiscUtils.getInstance().getEnvironmentVariable(
            EnvironmentVariables.DISABLE_SYMBOLIC_LINK_PROP) == null) {
            final boolean isSymlink = PropertyConstants.IS_SYMLINK.equals(
                PropertyUtils.selectMatching(operation.getPropertyValues(), PropertyConstants.SYMBOLIC_KEY));
            if (isSymlink) {
                /*
                 * If symlink, get the file content as the link destination
                 */
                String localDestinationPath = null;
                try {
                    final BufferedReader br = new BufferedReader(new FileReader(tempFile));
                    localDestinationPath = br.readLine();
                    br.close();
                } catch (final IOException e) {
                    onNonFatalError(e);
                    return ""; //$NON-NLS-1$
                }

                if (localDestinationPath == null || localDestinationPath.length() == 0) {
                    onNonFatalError(new IOException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("GetEngine.CannotCreateTargetItemAsSymbolicLinkBecauseNoLocalDestinationPathSuppliedFormat"), //$NON-NLS-1$
                        //@formatter:on
                        targetLocalItem)));
                    return ""; //$NON-NLS-1$
                }
                return localDestinationPath;
            }
        }

        /*
         * Get symbolic link via .tpattributes
         */
        final FileAttributesCollection attributes =
            getAttributesForFile(targetLocalItem, targetServerItem, isTextEncoding(operation.getEncoding()));

        if (attributes == null) {
            return null;
        }

        /*
         * Local link overrides server link.
         */
        final StringPairFileAttribute localLinkAttribute =
            attributes.getStringPairFileAttribute(FileAttributeNames.LOCAL_LINK);
        if (localLinkAttribute != null) {
            final String localDestinationPath = localLinkAttribute.getValue();
            if (localDestinationPath == null || localDestinationPath.length() == 0) {
                onNonFatalError(new IOException(MessageFormat.format(
                    //@formatter:off
                    Messages.getString("GetEngine.CannotCreateTargetItemAsSymbolicLinkBecauseNoLocalDestinationPathSuppliedFormat"), //$NON-NLS-1$
                    //@formatter:on
                    targetLocalItem)));
                return ""; //$NON-NLS-1$
            }

            return localDestinationPath;
        }

        /*
         * Look for server link.
         */
        final StringPairFileAttribute serverLinkAttribute =
            attributes.getStringPairFileAttribute(FileAttributeNames.LINK);
        if (serverLinkAttribute != null) {
            String destinationServerPath = serverLinkAttribute.getValue();
            if (destinationServerPath == null || destinationServerPath.length() == 0) {
                onNonFatalError(new IOException(MessageFormat.format(
                    //@formatter:off
                    Messages.getString("GetEngine.CannotCreateTargetItemAsSymbolicLinkBecauseNoDestinationPathSuppliedFormat"), //$NON-NLS-1$
                    //@formatter:on
                    targetLocalItem)));
                return ""; //$NON-NLS-1$
            }

            /*
             * Fix up the destination server path to support a relative path. If
             * the path is not absolute, prepend the server path that the local
             * item's parent directory is mapped to.
             */
            if (destinationServerPath.startsWith(ServerPath.ROOT) == false) {
                final String serverPathForLocalItem = workspace.getMappedServerPath(targetLocalItem);
                Check.notNull(serverPathForLocalItem, "serverPathForLocalItem"); //$NON-NLS-1$

                try {
                    destinationServerPath = ServerPath.canonicalize(ServerPath.getParent(serverPathForLocalItem)
                        + "/" //$NON-NLS-1$
                        + destinationServerPath);
                } catch (final ServerPathFormatException e) {
                    onNonFatalError(new ServerPathFormatException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("GetEngine.CannotCreateTargetItemAsSymbolicLinkBecauseRelativeDestinationPathCouldNotBeParsedFormat"), //$NON-NLS-1$
                        //@formatter:on
                        targetLocalItem,
                        destinationServerPath,
                        e.getLocalizedMessage())));
                    return ""; //$NON-NLS-1$
                }
            }

            String localDestination = null;
            try {
                localDestination = workspace.getMappedLocalPath(destinationServerPath);

                if (localDestination == null) {
                    onNonFatalError(new IOException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("GetEngine.CannotCreateTargetItemAsSymbolicLinkBecauseDestinationServerPathCannotBeMappedFormat"), //$NON-NLS-1$
                        //@formatter:on
                        targetLocalItem,
                        destinationServerPath)));
                    return ""; //$NON-NLS-1$
                }

                if (workspace.isLocalPathMapped(localDestination) == false) {
                    onNonFatalError(new IOException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("GetEngine.CannotCreateTargetItemAsSymbolicLinkBecauseDestinationPathIsNotInsideMappedFolderFormat"), //$NON-NLS-1$
                        //@formatter:on
                        targetLocalItem,
                        localDestination)));
                    return ""; //$NON-NLS-1$
                }
            } catch (final ServerPathFormatException e) {
                onNonFatalError(new ServerPathFormatException(MessageFormat.format(
                    //@formatter:off
                    Messages.getString("GetEngine.CannotCreateTargetItemAsSymbolicLinkBecauseAbsoluteDestinationPathCouldNotBeParsedFormat"), //$NON-NLS-1$
                    //@formatter:on
                    targetLocalItem,
                    destinationServerPath,
                    e.getLocalizedMessage())));
                return ""; //$NON-NLS-1$
            }

            return localDestination;
        }

        return null;
    }

    public void onNonFatalError(final Throwable t) {
        onNonFatalError(t, null);
    }

    public void onNonFatalError(final Throwable t, final Workspace workspace) {
        if (workspace != null) {
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), workspace, t));
        } else {
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), client, t));
        }
    }

    /**
     * Fire the get event.
     */
    private void fireGettingEvent(
        final AsyncGetOperation asyncOp,
        final OperationStatus status,
        final GetOperation getOp,
        ChangeType targetChangeType,
        final PropertyValue[] targetPropertyValues) {
        if (status == OperationStatus.TARGET_LOCAL_PENDING) {
            Check.isTrue(
                targetChangeType != ChangeType.NONE,
                "There should have been a target pending change: " + getOp); //$NON-NLS-1$
        } else {
            targetChangeType = ChangeType.NONE;
        }

        client.getEventEngine().fireGet(
            new GetEvent(
                EventSource.newFromHere(),
                asyncOp,
                status,
                getOp,
                getOp.getTargetLocalItem(),
                targetChangeType,
                targetPropertyValues));
    }

    /**
     * Fire the appropriate event.
     */
    private void recordEvent(final AsyncGetOperation asyncOp, final OperationStatus status, final GetOperation getOp) {
        recordEvent(asyncOp, status, getOp, null);
    }

    /**
     * Fire the appropriate event.
     */
    private void recordEvent(
        final AsyncGetOperation asyncOp,
        final OperationStatus status,
        final GetOperation getOp,
        final GetOperation targetOp) {
        Check.isTrue(
            status != OperationStatus.DELETING || null == getOp.getTargetLocalItem(),
            "targetLocalItem must be null for deletion events"); //$NON-NLS-1$

        final ChangeType targetChangeType = targetOp != null ? targetOp.getEffectiveChangeType() : ChangeType.NONE;
        final PropertyValue[] targetPropertyValues = targetOp != null ? targetOp.getPropertyValues() : null;

        if (log.isTraceEnabled()) {
            log.trace(MessageFormat.format(
                "Recording OperationStatus.{0} for {1}", //$NON-NLS-1$
                status,
                getOp.getTargetServerItem()));
        }

        try {
            // If pending an Edit where the local and server version differ (Get
            // Latest on Checkout,
            // then produce Getting events
            if (asyncOp.getType() == ProcessType.PEND
                && getOp.getChangeType().contains(ChangeType.EDIT)
                && getOp.getVersionLocal() != getOp.getVersionServer()) {
                fireGettingEvent(asyncOp, status, getOp, targetChangeType, targetPropertyValues);
            }

            // Any get ops during a merge/rollback that don't have details are
            // just gets.
            if (asyncOp.getType() == ProcessType.GET
                || ((asyncOp.getType() == ProcessType.MERGE || asyncOp.getType() == ProcessType.ROLLBACK)
                    && getOp.getMergeDetails() == null)) {
                fireGettingEvent(asyncOp, status, getOp, targetChangeType, targetPropertyValues);
            } else if (asyncOp.getType() == ProcessType.PEND
                || asyncOp.getType() == ProcessType.UNDO
                || asyncOp.getType() == ProcessType.UNSHELVE) {
                if (getOp.getChangeType().contains(ChangeType.RENAME)
                    && status == OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY) {
                    // If the deletion of the source fails for a rename, fire a
                    // non-fatal error because
                    // the pending/undone change event has already been fired.
                    onNonFatalError(
                        new VersionControlException(
                            MessageFormat.format(
                                Messages.getString("GetEngine.CantDeleteNonEmptyDirPathFormat"), //$NON-NLS-1$
                                getOp.getSourceLocalItem())),
                        asyncOp.getWorkspace());
                } else if (getOp.getChangeType().equals(ChangeType.NONE) && asyncOp.getType() == ProcessType.UNSHELVE) {
                    fireGettingEvent(asyncOp, status, getOp, targetChangeType, targetPropertyValues);
                } else {
                    final PendingChange pc = new PendingChange(asyncOp.getWorkspace(), getOp, asyncOp.getType());

                    final PendingChangeEvent event = new PendingChangeEvent(
                        EventSource.newFromHere(),
                        asyncOp.getWorkspace(),
                        pc,
                        status,
                        asyncOp.getFlags());

                    if (asyncOp.getType() == ProcessType.UNDO) {
                        client.getEventEngine().fireUndonePendingChange(event);
                    } else {
                        client.getEventEngine().fireNewPendingChange(event);
                    }
                }
            } else if (asyncOp.getType() == ProcessType.MERGE || asyncOp.getType() == ProcessType.ROLLBACK) {
                final Conflict mergeDetails = getOp.getMergeDetails();

                if (mergeDetails.getBaseChangeType().contains(ChangeType.RENAME)
                    && status == OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY) {
                    // If the deletion of the source fails for a rename, fire a
                    // non-fatal error
                    // because the pending/undone change event has already been
                    // fired.
                    onNonFatalError(
                        new VersionControlException(
                            MessageFormat.format(
                                Messages.getString("GetEngine.CantDeleteNonEmptyDirPathFormat"), //$NON-NLS-1$
                                mergeDetails.getTargetLocalItem())),
                        asyncOp.getWorkspace());
                } else {
                    // If the merge operation resulted in a pending change,
                    // include that in the
                    // merge event.
                    PendingChange pc = null;
                    if (getOp.hasPendingChange()) {
                        pc = new PendingChange(asyncOp.getWorkspace(), getOp, asyncOp.getType());
                    }

                    client.getEventEngine().fireMerging(
                        new MergingEvent(
                            EventSource.newFromHere(),
                            mergeDetails,
                            asyncOp.getWorkspace(),
                            getOp.isLatest(),
                            pc,
                            status,
                            targetChangeType,
                            !asyncOp.isPreview(),
                            getOp.getPropertyValues()));
                }
            }
        } catch (final Exception e) {
            // There shouldn't be any exceptions being thrown, but if there are,
            // we don't want them to bubble out and get added to the retry list.
            log.warn("Exception recording get event", e); //$NON-NLS-1$
        }
    }

    /**
     * Convenience method that calls
     * {@link #processGetOperations(Workspace, ProcessType, RequestType, GetOperation[][], GetOptions, boolean, boolean, ChangePendedFlags)}
     * with just one array of operations and {@link RequestType#NONE}.
     */
    public GetStatus processGetOperations(
        final Workspace workspace,
        final ProcessType type,
        final GetOperation[] operations,
        final GetOptions options,
        final ChangePendedFlags flags) {
        final GetOperation[][] operationSet = new GetOperation[1][];
        operationSet[0] = operations;

        return processGetOperations(workspace, type, RequestType.NONE, operationSet, options, false, true, flags);
    }

    /**
     * Process all the GetOperations the server returned to us as a result of
     * its get() method. This is generally not called directly from users of
     * this library. Instead, call the get methods in the {@link Workspace}
     * class.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is an <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param the
     *        workspace to process operations in (must not be <code>null</code>)
     * @param type
     *        the type of process to perform on the get operations (must not be
     *        <code>null</code>)
     * @param requestType
     *        the type of request for the operations (must not be
     *        <code>null</code>)
     * @param results
     *        the array of arrays of operations the server returned. Null items
     *        in the arrays will be skipped. Null arrays are not allowed.
     * @param options
     *        options for the get operation (must not be <code>null</code>)
     * @param deleteUndoneAdds
     *        if <code>true</code> adds which are undone can be deleted
     * @param onlineOperation
     *        true if this is for a server workspace
     * @param flags
     *        flags returned by the server when pending changes (must not be
     *        <code>null</code>; pass ChangePendedFlags.UNKNOWN if no server
     *        value available in your operation)
     * @return the status of the operation. Failure information for each item is
     *         available inside this object.
     * @throws CanceledException
     *         if the user canceled the processing through the default
     *         {@link TaskMonitor}
     */
    public GetStatus processGetOperations(
        final Workspace workspace,
        final ProcessType type,
        final RequestType requestType,
        final GetOperation[][] results,
        final GetOptions options,
        final boolean deleteUndoneAdds,
        final boolean onlineOperation,
        final ChangePendedFlags flags) throws CanceledException {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.notNull(requestType, "requestType"); //$NON-NLS-1$
        Check.notNull(results, "results"); //$NON-NLS-1$
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNull(flags, "flags"); //$NON-NLS-1$

        log.debug("Set all get operations type to: " + type.toString()); //$NON-NLS-1$
        int getOpsCount = 0;
        for (final GetOperation[] getOperations : results) {
            for (final GetOperation getOp : getOperations) {
                getOp.setProcessType(type);
                getOpsCount++;
            }
        }
        log.debug("Get operations count: " + String.valueOf(getOpsCount)); //$NON-NLS-1$

        final UpdateLocalVersionQueueOptions localUpdateOptions =
            calculateUpdateLocalVersionOptions(workspace, type, requestType, onlineOperation);

        log.debug("localUpdateOptions: " + localUpdateOptions.toString()); //$NON-NLS-1$
        log.debug("options: " + options.toString()); //$NON-NLS-1$

        final List<ClientLocalVersionUpdate> remapUpdates = new ArrayList<ClientLocalVersionUpdate>();

        if (WorkspaceLocation.LOCAL == workspace.getLocation() && options.contains(GetOptions.REMAP)) {
            final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
            try {
                log.debug("Trying to remap versions in the local workspace"); //$NON-NLS-1$
                transaction.execute(new LocalVersionTransaction() {
                    @Override
                    public void invoke(final WorkspaceVersionTable lv) {
                        // In a server workspace, when Remap is specified, the
                        // local version table is updated for you when an item
                        // is remapped. In a local workspace, the local version
                        // table is not updated, and a GetOperation is returned
                        // with VersionLocal set equal to VersionServer and
                        // SourceLocalItem equal to TargetLocalItem.
                        // VersionLocal is not actually set to that value yet.
                        // We must update the local version table (both local
                        // and server) in response to these GetOperations.

                        final boolean setLastFileTimeToCheckin =
                            workspace.getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN);

                        for (final GetOperation[] getOperations : results) {
                            for (final GetOperation getOp : getOperations) {
                                if (null != getOp.getTargetLocalItem()
                                    && LocalPath.equals(getOp.getSourceLocalItem(), getOp.getTargetLocalItem())
                                    &&
                                    // Here the server is lying and telling us
                                    // VersionLocal is equal to VersionServer
                                    // even though it does not (yet). It is a
                                    // signal that this is a remap operation.
                                getOp.getVersionLocal() == getOp.getVersionServer()) {
                                    getOp.setIgnore(true);

                                    final WorkspaceLocalItem lvExisting = lv.getByLocalItem(getOp.getTargetLocalItem());

                                    if (null != lvExisting) {
                                        // If necessary, update the
                                        // last-modified time of the item on
                                        // disk to match the new check-in date
                                        // of the item that now occupies that
                                        // local path.
                                        if (setLastFileTimeToCheckin
                                            && !DotNETDate.MIN_CALENDAR.equals(getOp.getVersionServerDate())
                                            && VersionControlConstants.ENCODING_FOLDER != getOp.getEncoding()
                                            && new File(getOp.getTargetLocalItem()).exists()) {
                                            try {
                                                final File targetLocalFile = new File(getOp.getTargetLocalItem());
                                                final FileSystemAttributes attrs =
                                                    FileSystemUtils.getInstance().getAttributes(targetLocalFile);
                                                boolean restoreReadOnly = false;

                                                if (attrs.isReadOnly()) {
                                                    attrs.setReadOnly(false);
                                                    FileSystemUtils.getInstance().setAttributes(targetLocalFile, attrs);
                                                    restoreReadOnly = true;
                                                }

                                                targetLocalFile.setLastModified(
                                                    getOp.getVersionServerDate().getTimeInMillis());

                                                if (restoreReadOnly) {
                                                    attrs.setReadOnly(true);
                                                    FileSystemUtils.getInstance().setAttributes(targetLocalFile, attrs);
                                                }
                                            } catch (final Exception ex) {
                                                log.warn("Error setting file time for get with remap", ex); //$NON-NLS-1$
                                            }
                                        }

                                        remapUpdates.add(
                                            new ClientLocalVersionUpdate(
                                                getOp.getSourceServerItem(),
                                                getOp.getItemID(),
                                                getOp.getTargetLocalItem(),
                                                getOp.getVersionServer(),
                                                getOp.getVersionServerDate(),
                                                getOp.getEncoding(),
                                                lvExisting.getHashValue(),
                                                lvExisting.getLength(),
                                                lvExisting.getBaselineFileGUID(),
                                                null /* pendingChangeTargetServerItem */,
                                                getOp.getPropertyValues()));
                                    }
                                }
                            }
                        }

                    }
                });
            } finally {
                try {
                    transaction.close();
                } catch (final IOException e) {
                    throw new VersionControlException(e);
                }
            }

            log.debug("The remapUpdates list has been prepared"); //$NON-NLS-1$
        }

        final WorkspaceLock wLock = workspace.lock();

        try {
            /*
             * Create a BaselineFolderCollection object to speed up read
             * operations on the baseline folders (so that each simple operation
             * like creating a new baseline file, deleting a baseline file, etc.
             * does not require us to open/close the WP table). Then attach that
             * BaselineFolderCollection to the WorkspaceLock which protects this
             * ProcessGetOperations().
             */
            final AtomicReference<BaselineFolderCollection> baselineFolders =
                new AtomicReference<BaselineFolderCollection>();

            if (workspace.getLocation() == WorkspaceLocation.LOCAL) {
                final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
                try {
                    transaction.execute(new WorkspacePropertiesTransaction() {
                        @Override
                        public void invoke(final LocalWorkspaceProperties wp) {
                            baselineFolders.set(new BaselineFolderCollection(workspace, wp.getBaselineFolders()));
                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }

                wLock.setBaselineFolders(baselineFolders.get());
            }

            log.debug("remapUpdates.size(): " + remapUpdates.size()); //$NON-NLS-1$

            if (remapUpdates.size() > 0) {
                // If we have any remap update requests for the local version
                // table, execute them using a larger batch size than we
                // normally use. No file downloads are happening so these should
                // go through very quickly.
                Check.isTrue(
                    localUpdateOptions.contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER),
                    "localUpdateOptions.contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER)"); //$NON-NLS-1$

                final UpdateLocalVersionQueue ulvq = new UpdateLocalVersionQueue(
                    workspace,
                    localUpdateOptions,
                    wLock,
                    5000 /* flushTriggerLevel */,
                    10000 /* maximumLevel */,
                    Integer.MAX_VALUE /* timeTriggerInMilliseconds */);

                try {
                    for (final ClientLocalVersionUpdate remapUpdate : remapUpdates) {
                        ulvq.queueUpdate(remapUpdate);
                    }
                } finally {
                    ulvq.close();
                }

                log.debug("Local version updates have been successfully queued."); //$NON-NLS-1$
            }

            // Now, create an object to track the state for this get operation.
            final AsyncGetOperation asyncOp = new AsyncGetOperation(
                workspace,
                type,
                requestType,
                options,
                deleteUndoneAdds,
                wLock,
                localUpdateOptions,
                flags,
                new AccountingCompletionService<WorkerStatus>(client.getUploadDownloadWorkerExecutor()));

            log.debug("Preparing Get Operation actions"); //$NON-NLS-1$

            final GetOperation[] actions = prepareGetOperations(asyncOp, results);

            log.debug("Number of Get Operation actions prepared: " + actions.length); //$NON-NLS-1$

            processOperations(asyncOp, actions);

            log.debug("All Get Operation actions have been processed."); //$NON-NLS-1$

            return asyncOp.getStatus();
        } catch (final CoreCancelException e) {
            throw new CanceledException();
        } finally {
            if (wLock != null) {
                wLock.close();
            }
        }
    }

    private UpdateLocalVersionQueueOptions calculateUpdateLocalVersionOptions(
        final Workspace workspace,
        final ProcessType type,
        final RequestType requestType,
        final boolean onlineOperation) {
        UpdateLocalVersionQueueOptions localUpdateOptions = UpdateLocalVersionQueueOptions.UPDATE_BOTH;

        if (WorkspaceLocation.LOCAL == workspace.getLocation()) {
            if (ProcessType.PEND == type) {
                // Offline operations
                if (requestType == RequestType.ADD
                    || requestType == RequestType.EDIT
                    || requestType == RequestType.DELETE
                    || requestType == RequestType.RENAME) {
                    localUpdateOptions = UpdateLocalVersionQueueOptions.UPDATE_LOCAL;
                }

                localUpdateOptions = onlineOperation
                    ? localUpdateOptions.combine(UpdateLocalVersionQueueOptions.UPDATE_SERVER) : localUpdateOptions;
            }

            if (ProcessType.UNDO == type) {
                localUpdateOptions = UpdateLocalVersionQueueOptions.UPDATE_LOCAL;

                if (onlineOperation) {
                    localUpdateOptions = localUpdateOptions.combine(UpdateLocalVersionQueueOptions.UPDATE_SERVER);
                }
            }
        }

        return localUpdateOptions;
    }

    private void processOperations(final AsyncGetOperation asyncOp, GetOperation[] actions) throws CoreCancelException {
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$
        Check.notNull(actions, "actions"); //$NON-NLS-1$

        // Bail out quickly if there is nothing to do (avoids unnecessarily
        // creating TFProxyServer, etc.).
        if (actions.length == 0) {
            return;
        }

        /*
         * Getting a task monitor from the service means we don't create a sub
         * monitor (that requires knowing how many of the parent's ticks to
         * use), so just begin work.
         *
         * Look below for WORK = n comments.
         */
        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();
        taskMonitor.begin(null, 100);

        // finally ensures taskMonitor.close()
        try {
            /*
             * In a local workspace, tag each GetOperation with the local
             * version row of its target local item, if it exists. This will be
             * used to determine whether or not to file writable file conflicts.
             *
             * WORK = 1
             */
            if (WorkspaceLocation.LOCAL == asyncOp.getWorkspace().getLocation()
                && asyncOp.getLocalUpdateOptions().contains(UpdateLocalVersionQueueOptions.UPDATE_SERVER)) {
                log.debug("Preparing local version updates: begins"); //$NON-NLS-1$
                taskMonitor.setCurrentWorkDescription(Messages.getString("GetEngine.PreparingLocalVersionUpdates")); //$NON-NLS-1$

                final GetOperation[] transactionActions = actions;
                final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(asyncOp.getWorkspace());
                try {
                    transaction.execute(new LocalVersionTransaction() {
                        @Override
                        public void invoke(final WorkspaceVersionTable lv) {
                            for (final GetOperation operation : transactionActions) {
                                if (null != operation.getTargetLocalItem()) {
                                    operation.setLocalVersionEntry(lv.getByLocalItem(operation.getTargetLocalItem()));
                                }
                            }
                        }
                    });
                } finally {
                    try {
                        transaction.close();
                    } catch (final IOException e) {
                        throw new VersionControlException(e);
                    }
                }
                log.debug("Preparing local version updates: ended"); //$NON-NLS-1$
            }
            taskMonitor.worked(1);

            /*
             * A shutdown listener that's registered for the duration of the
             * get, then unregistered. This is mainly for the case the user
             * cancels the CLC (with control-C) and we want to send version
             * updates to the server before the JVM shuts down. We use a monitor
             * class to synchronize between the threads.
             */
            final ShutdownMonitor shutdownMonitor = new ShutdownMonitor();
            final ShutdownEventListener getInterruptedShutdownListener =
                new GetShutdownEventListener(taskMonitor, shutdownMonitor);

            // finally ensures flush version updates, shutdown listener removed
            try {
                ShutdownManager.getInstance().addShutdownEventListener(
                    getInterruptedShutdownListener,
                    SHUTDOWN_HANDLER_PRIORITY);

                // Record the number of operations to be processed (ignored
                // operations don't count).
                asyncOp.getStatus().setNumOperations(actions.length);
                for (final GetOperation action : actions) {
                    if (action.isIgnore()) {
                        asyncOp.getStatus().decrementNumOperations();
                    }
                }

                /*
                 * A sub monitor for the operations.
                 *
                 * WORK = 98
                 */
                log.debug("Getting files: started"); //$NON-NLS-1$

                final TaskMonitor gettingTaskMonitor = taskMonitor.newSubTaskMonitor(98);
                gettingTaskMonitor.begin(Messages.getString("GetEngine.Getting"), actions.length); //$NON-NLS-1$

                try {
                    /*
                     * Process the operations and continue to retry for items
                     * that fail until there is no forward progress.
                     */
                    int retryCount;
                    while (true) {
                        // gettingTaskMonitor "works" 1 for each action
                        processOperationsInternal(asyncOp, actions, gettingTaskMonitor);

                        retryCount = actions.length;
                        Check.isTrue(
                            asyncOp.getRetryList().size() <= retryCount,
                            "The retry list size exceeded the previous one!"); //$NON-NLS-1$
                        if (asyncOp.isPreview()
                            || asyncOp.getRetryList().size() == 0
                            || asyncOp.getRetryList().size() >= retryCount) {
                            break;
                        }

                        actions = new GetOperation[asyncOp.getRetryList().size()];
                        for (int i = 0; i < actions.length; i++) {
                            actions[i] = asyncOp.getRetryList().get(i).getRetryOp();
                        }
                        asyncOp.resetForRetry();
                    }
                } finally {
                    gettingTaskMonitor.done();
                }
                log.debug("Getting files: ended"); //$NON-NLS-1$

                // Record whatever warnings remain.
                for (final RetryEntry entry : asyncOp.getRetryList()) {
                    /*
                     * If this was part of a directory move, fire a warning
                     * about it, but do not add it to the list of get warnings
                     * since it can't be resolved (the server knows that the
                     * folder is now in the new location). The get event for the
                     * rename has already been fired.
                     */
                    if (entry.getStatus() == OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY
                        && entry.getRetryOp().getTargetLocalItem() != null
                        && asyncOp.getDontDeleteFolderHash().containsKey(entry.getRetryOp().getTargetLocalItem())) {
                        onNonFatalError(
                            new Exception(
                                MessageFormat.format(
                                    Messages.getString("GetEngine.CantDeleteNonEmptyDirPathFormat"), //$NON-NLS-1$
                                    entry.getRetryOp().getCurrentLocalItem())),
                            asyncOp.getWorkspace());
                        continue;
                    }

                    /*
                     * If we end up with a non-empty directory that is to be
                     * deleted, tell the server that we deleted it even though
                     * we couldn't. Otherwise, the user is pestered with the
                     * warning for each get.
                     */
                    if (!asyncOp.isPreview()
                        && entry.getStatus() == OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY
                        && entry.getRetryOp().getTargetLocalItem() == null) {
                        // Queue a request to tell the server that we "deleted"
                        // it.
                        asyncOp.queueLocalVersionUpdate(entry.getRetryOp(), null, entry.getRetryOp().getVersionLocal());
                    }

                    // Add the warnings to the GetStatus object.
                    asyncOp.getStatus().incrementNumWarnings();

                    /*
                     * Record local conflicts only for the cases where there is
                     * a writable file in the way or the target has a pending
                     * change. Other cases cannot be handled as conflicts (e.g.,
                     * non-empty directory, directory in the way of a file,
                     * etc.).
                     */
                    if (!asyncOp.isPreview()
                        && (entry.getStatus() == OperationStatus.SOURCE_WRITABLE
                            || entry.getStatus() == OperationStatus.TARGET_WRITABLE
                            || entry.getStatus() == OperationStatus.TARGET_LOCAL_PENDING)) {
                        asyncOp.getLocalUpdateQueue().flush();

                        asyncOp.getWorkspace().addConflict(
                            ConflictType.LOCAL,
                            entry.getRetryOp().getItemID(),
                            entry.getRetryOp().getVersionServer(),
                            entry.getRetryOp().getPendingChangeID(),
                            entry.getRetryOp().getCurrentLocalItem(),
                            entry.getRetryOp().getTargetLocalItem(),
                            entry.getStatus());

                        asyncOp.getStatus().setHaveResolvableWarnings(true);
                    }

                    recordEvent(asyncOp, entry.getStatus(), entry.getRetryOp(), entry.getTargetAction());
                }
            } finally {
                /*
                 * Flush outstanding updates in a try block because the shutdown
                 * hook removal code must run.
                 *
                 * WORK = 1
                 */
                taskMonitor.setCurrentWorkDescription(Messages.getString("GetEngine.WaitingForVersionUpdatesToFinish")); //$NON-NLS-1$
                try {
                    asyncOp.getLocalUpdateQueue().close();
                } finally {
                    taskMonitor.worked(1);

                    ShutdownManager.getInstance().removeShutdownEventListener(
                        getInterruptedShutdownListener,
                        SHUTDOWN_HANDLER_PRIORITY);

                    /*
                     * There may be a shutdown event listener (or many of them)
                     * waiting for this thread to finish after cancellation, so
                     * we should notify all of them.
                     */
                    log.trace("acquiring shutdown monitor lock from get loop"); //$NON-NLS-1$
                    synchronized (shutdownMonitor) {
                        log.trace("setting isShutdown"); //$NON-NLS-1$
                        shutdownMonitor.setShutdown();

                        log.trace("notifying all listeners"); //$NON-NLS-1$
                        shutdownMonitor.notifyAll();
                    }
                }
            }
        } finally {
            taskMonitor.done();
        }
    }

    /**
     * @param asyncOp
     *        the async operation (must not be <code>null</code>)
     * @param actions
     *        the actions to process (must not be <code>null</code>)
     * @param gettingTaskMonitor
     *        the task monitor used for detecting cancelation, setting current
     *        work description, and tracking progress; .worked(1) is called each
     *        time an operation is processed (must not be <code>null</code>)
     */
    private void processOperationsInternal(
        final AsyncGetOperation asyncOp,
        final GetOperation[] actions,
        final TaskMonitor gettingTaskMonitor) throws CoreCancelException {
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$
        Check.notNull(actions, "actions"); //$NON-NLS-1$
        Check.notNull(gettingTaskMonitor, "taskMonitor"); //$NON-NLS-1$

        log.debug("Try processOperationsInternal: started"); //$NON-NLS-1$

        try {
            int actionCount = 0;

            for (final GetOperation action : actions) {
                /*
                 * Every N processed operations, if this is a local workspace,
                 * check to see if we should yield the workspace lock and let
                 * some other thread in the system use the workspace for a
                 * moment.
                 */
                if (0 == (actionCount++ % PROCESS_GET_OPERATIONS_UNIT_OF_WORK)
                    && WorkspaceLocation.LOCAL == asyncOp.getWorkspace().getLocation()
                    && asyncOp.getWorkspaceLock().isYieldRequested()) {
                    /*
                     * Wait for all outstanding downloads to finish. (No new
                     * ones will be queued, since it's this thread that does
                     * that.)
                     */
                    waitForCompletions(asyncOp.getCompletionService());

                    /*
                     * Flush the ULVQ so that the other process cannot observe
                     * the state of the items on disk as being out of sync with
                     * the local version table
                     */
                    asyncOp.getLocalUpdateQueue().flush();

                    // Actually yield to the other thread/process
                    asyncOp.getWorkspaceLock().yield();
                }

                // Skip actions that the user discarded.
                if (action.isIgnore()) {
                    continue;
                }

                throwIfCanceled(gettingTaskMonitor);

                // if we received a fatal exception on our async op's
                // throw
                throwIfFatalError(asyncOp);

                gettingTaskMonitor.setCurrentWorkDescription(action.getTargetServerItem());

                // Perform one get action.
                processOperation(action, asyncOp);

                gettingTaskMonitor.worked(1);
            }
        } finally {
            log.debug("Try processOperationsInternal: waiting for downloads completion"); //$NON-NLS-1$
            // Wait for all downloads submitted so far
            waitForCompletions(asyncOp.getCompletionService());
        }

        log.debug("Try processOperationsInternal: finished successfully"); //$NON-NLS-1$

        throwIfCanceled(gettingTaskMonitor);

        throwIfFatalError(asyncOp);

        /*
         * Now that the full get is done, process the directory deletions (list
         * is sorted bottom up).
         */
        finishDirectoryDeletionsAndMoves(asyncOp, gettingTaskMonitor);
    }

    /**
     * Deletes the source item when processing a get operation.
     *
     * @param operation
     *        the operation whose source needs deleted (must not be
     *        <code>null</code>)
     */
    public void deleteSource(final GetOperation operation, final FileSystemAttributes existingLocalAttrs) {
        Check.notNull(operation, "op"); //$NON-NLS-1$

        if (existingLocalAttrs.exists() == false) {
            return;
        }

        if (operation.getItemType() == ItemType.FOLDER) {
            if (existingLocalAttrs.isDirectory()) {
                new File(operation.getCurrentLocalItem()).delete();
                log.debug(MessageFormat.format("Deleted directory: {0}", operation.getCurrentLocalItem())); //$NON-NLS-1$
                return;
            }
        } else if (operation.getItemType() == ItemType.FILE) {
            // Don't stomp a directory if we thought it was a file.
            if (existingLocalAttrs.isDirectory() && !existingLocalAttrs.isSymbolicLink()) {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("GetEngine.FileIsADirectoryFormat"), //$NON-NLS-1$
                        operation.getCurrentLocalItem()));
            }

            // Delete the file.
            new File(operation.getCurrentLocalItem()).delete();
            log.debug(MessageFormat.format("Deleted file: {0}", operation.getCurrentLocalItem())); //$NON-NLS-1$
        } else {
            /*
             * this is when action.ItemType = ItemType.Any - this should only
             * happen when the item has been destroyed on the server - in this
             * case we will just destroy what is on disk
             */
            Check.isTrue(
                operation.getTargetServerItem() == null && operation.getVersionServer() == 0,
                "Cannot pass ItemType of any when there is server information"); //$NON-NLS-1$

            if (existingLocalAttrs.isDirectory()) {
                new File(operation.getCurrentLocalItem()).delete();
                log.debug(MessageFormat.format("Deleted directory: {0}", operation.getCurrentLocalItem())); //$NON-NLS-1$
            } else {
                // Delete the file.
                new File(operation.getCurrentLocalItem()).delete();
                log.debug(MessageFormat.format("Deleted file: {0}", operation.getCurrentLocalItem())); //$NON-NLS-1$
            }

            return;
        }
    }

    /**
     * Prepare the results returned from the server for processing in the main
     * get loop.
     *
     */
    private GetOperation[] prepareGetOperations(final AsyncGetOperation asyncOp, final GetOperation[][] results) {
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$
        Check.notNull(results, "results"); //$NON-NLS-1$

        /*
         * The common case is a single result, and we do not want to slow that
         * down. In the case where there are multiple requests (and thus
         * multiple results), we need to filter to make sure that we don't have
         * redundant getOps.
         */
        Map<String, GetOperation> newLocalItemHash = null;
        if (results.length > 1) {
            newLocalItemHash = new TreeMap<String, GetOperation>(LocalPath.TOP_DOWN_COMPARATOR);
        }

        System.currentTimeMillis();
        for (int i = 0; i < results.length; i++) {
            final GetOperation[] tempGetOps = results[i];

            for (final GetOperation getOp : tempGetOps) {
                /*
                 * We need to build a hashtable of getOps that have a source
                 * that is an existing item. In the multiple result case, we
                 * also need to filter out redundant getOps. Each local item
                 * currently on disk can only have one operation. Also, each
                 * target local item can have only one operation. They must be
                 * considered separately.
                 */
                final String sourceLocalItem = getOp.getSourceLocalItem();
                if (sourceLocalItem != null) {
                    if (results.length == 1) {
                        if (!asyncOp.getExistingLocalHash().containsKey(sourceLocalItem)) {
                            asyncOp.getExistingLocalHash().put(sourceLocalItem, getOp);
                        } else {
                            // This is a server problem.
                            onNonFatalError(new Exception(MessageFormat.format(
                                //@formatter:off
                                Messages.getString("GetEngine.ServerGateUsTwoGetOperationsForSameLocalPathMayRequireMultipleGetsFormat"), //$NON-NLS-1$
                                //@formatter:on
                                sourceLocalItem)));
                        }
                    } else {
                        // I think this test is redundant because of the test
                        // above
                        if (sourceLocalItem != null) {
                            if (!asyncOp.getExistingLocalHash().containsKey(sourceLocalItem)) {
                                asyncOp.getExistingLocalHash().put(sourceLocalItem, getOp);
                            } else {
                                final GetOperation existingOp = asyncOp.getExistingLocalHash().get(sourceLocalItem);

                                /*
                                 * favor the get operation which has a target
                                 * local item this happens in the case when the
                                 * caller does 2 scoped gets See bug 416603 for
                                 * details
                                 */
                                if (existingOp.getTargetLocalItem() == null && getOp.getTargetLocalItem() != null) {
                                    asyncOp.getExistingLocalHash().put(sourceLocalItem, getOp);
                                }
                            }
                        }
                    }
                } else if (results.length != 1) {
                    final String newLocalItem = getOp.getTargetLocalItem();
                    if (newLocalItem != null && !newLocalItemHash.containsKey(newLocalItem)) {
                        newLocalItemHash.put(newLocalItem, getOp);
                    }
                }
            }
        }

        /*
         * Since JDK 1.7 {@link Arrays.sort} algorithm has changed:
         *
         * The implementation was adapted from Tim Peters's list sort for Python
         * (<a href=
         * "http://svn.python.org/projects/python/trunk/Objects/listsort.txt">
         * TimSort</a>). It uses techiques from Peter McIlroy's "Optimistic
         * Sorting and Information Theoretic Complexity", in Proceedings of the
         * Fourth Annual ACM-SIAM Symposium on Discrete Algorithms, pp 467-474,
         * January 1993.
         *
         * For some unknown reason the new implementation is not compatible with
         * the {@link GetOperation}'s compareTo method. We have to use another
         * means to get an ordered list of operation, e.g. to utilize {@link
         * TreeSet}.
         */

        // Sort the get operations for execution. Note that HatGui's cache
        // model relies on this.
        final TreeSet<GetOperation> getOps = new TreeSet<GetOperation>(GetOperation.GET_OPERATION_COMPARATOR);

        // Again, we've optimized the case of a single result.
        if (results.length == 1) {
            getOps.addAll(Arrays.asList(results[0]));
        } else {
            // copy our get ops to the output sorted set
            getOps.addAll(asyncOp.getExistingLocalHash().values());
            getOps.addAll(newLocalItemHash.values());
        }

        // Record the total number of operations for use in the events.
        asyncOp.totalNumOperations = getOps.size();

        return getOps.toArray(new GetOperation[getOps.size()]);
    }

    /**
     *
     * Finish directory deletions and moves. Directory deletions are queued and
     * attempted after other actions have completed since the directory may not
     * be empty until actions involving descendants have completed. Directory
     * moves are accomplished by copy-and-delete, so they too are finished here.
     */
    private void finishDirectoryDeletionsAndMoves(final AsyncGetOperation asyncOp, final TaskMonitor taskMonitor)
        throws CoreCancelException {
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$
        Check.notNull(taskMonitor, "taskMonitor"); //$NON-NLS-1$

        for (final GetOperation delete : asyncOp.getDeletes().values()) {
            throwIfCanceled(taskMonitor);

            /*
             * Processing a subsequent get operation may have removed this
             * delete through a call to ClearLocalItem(). Also, if the folder
             * should not be deleted, skip the rest of this.
             */
            if (delete.getCurrentLocalItem() == null
                || asyncOp.getDontDeleteFolderHash().containsKey(delete.getCurrentLocalItem())) {
                continue;
            }

            try {
                final FileSystemAttributes attrs =
                    FileSystemUtils.getInstance().getAttributes(delete.getCurrentLocalItem());
                final boolean dirExists = attrs.exists();

                /*
                 * Before we try to delete the folder from disk, get rid of any
                 * $tf baseline folders that are still in it. Their contents
                 * will be moved to another baseline folder or to the backup
                 * location (Workspace.LocalMetadataLocation)
                 *
                 * Do this even if NoDiskUpdate is set, so a higher layer can
                 * finish a delete/rename and not lose the metadata. This case
                 * should be very rare, as these higher layers (like Eclipse)
                 * don't normally manage mappings in a way this could happen.
                 */
                if (null != asyncOp.getBaselineFolders()
                    && asyncOp.getBaselineFolders().isImmediateParentOfBaselineFolder(delete.getSourceLocalItem())) {
                    clearBaselineFoldersBeneathPath(asyncOp.getWorkspace(), delete.getSourceLocalItem());
                }

                /*
                 * We go into this block for Preview and NoDiskUpdate so all the
                 * right events and warnings get generated. More importantly we
                 * complete the get operation so higher layers can proceed (for
                 * example, when Eclipse actually performs the rename after pend
                 * with NoDiskUpdate).
                 */
                if (asyncOp.isPreview()
                    || asyncOp.isNoDiskUpdate()
                    || !dirExists
                    || LocalPath.isDirectoryEmpty(delete.getSourceLocalItem())) {
                    final boolean deleteAsUndoAdd = shouldDeleteAsUndoAdd(asyncOp, delete);

                    /*
                     * If this action was part of a move, we don't want a
                     * separate event for the deletion of the source, and we
                     * don't want to ack the server (otherwise, it will think we
                     * don't have that item ID). Otherwise, we need to update
                     * the server.
                     */
                    final String newLocalItem = delete.getTargetLocalItem();
                    if (!deleteAsUndoAdd
                        && (newLocalItem == null || asyncOp.getDontDeleteFolderHash().get(newLocalItem) != delete)) {
                        /*
                         * Deletes postpone the deletion notification as well.
                         * If we are only acknowledging it, don't tell the user
                         * because we are obviously not deleting it.
                         */
                        recordEvent(asyncOp, OperationStatus.DELETING, delete);

                        if (!asyncOp.isPreview()) {
                            // Queue a request to tell the server that I moved
                            // or deleted it.
                            asyncOp.queueLocalVersionUpdate(delete, null, delete.getVersionLocal());
                        }
                    }

                    /*
                     * Now that we are finished with it, mark it complete and
                     * effectively remove it from this list by clearing the
                     * local item. Remove the location from the hash.
                     */
                    delete.setDownloadCompleted(true);
                    asyncOp.getExistingLocalHash().remove(delete.getCurrentLocalItem());

                    /*
                     * Delete the directory after firing the event, because the
                     * change has already occurred on the server. We know the
                     * directory is empty at this point, but it may fail due to
                     * being in use or lack of permissions.
                     */
                    try {
                        if (!asyncOp.isPreview() && !asyncOp.isNoDiskUpdate()) {
                            deleteSource(delete, attrs);
                        }
                    } finally {
                        /*
                         * Always clear delete.SourceLocalItem, because at this
                         * point we're done with it, regardless of whether the
                         * actual delete succeeded.
                         */
                        delete.clearLocalItem();
                    }
                } else {
                    asyncOp.addWarning(OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY, delete, null);
                }
            } catch (final ProxyException e) {
                // We want these to keep going up.
                throw e;
            } catch (final Exception e) {
                onNonFatalError(e, asyncOp.getWorkspace());
            }
        }
    }

    private void clearBaselineFoldersBeneathPath(final Workspace workspace, final String sourceLocalItem) {
        final LocalWorkspaceTransaction transaction = new LocalWorkspaceTransaction(workspace);
        try {
            transaction.execute(new WorkspacePropertiesTransaction() {
                @Override
                public void invoke(final LocalWorkspaceProperties wp) {
                    while (true) {
                        BaselineFolder toRemove = null;

                        for (final BaselineFolder baselineFolder : wp.getBaselineFolders()) {
                            if (null != baselineFolder.path
                                && LocalPath.isDirectChild(sourceLocalItem, baselineFolder.path)) {
                                toRemove = baselineFolder;
                                break;
                            }
                        }

                        if (null == toRemove) {
                            break;
                        } else {
                            wp.removeBaselineFolder(toRemove);
                        }
                    }
                }
            });
        } finally {
            try {
                transaction.close();
            } catch (final IOException e) {
                throw new VersionControlException(e);
            }
        }
    }

    /**
     * Returns true if the target file of the action should be deleted, because
     * it's undo of pending add and deletePendingAdds was set to true;
     */
    private static boolean shouldDeleteAsUndoAdd(final AsyncGetOperation asyncOp, final GetOperation getOp) {
        return asyncOp.getDeleteUndoneAdds()
            && getOp.getChangeType().contains(ChangeType.ADD)
            && ProcessType.UNDO == getOp.getType();
    }

    private boolean isWritableFileConflict(
        final AsyncGetOperation asyncOp,
        final GetOperation action,
        final FileSystemAttributes newLocalAttrs) {
        if (action.isOkayToOverwriteExistingLocal()
            && LocalPath.equals(action.getSourceLocalItem(), action.getTargetLocalItem())) {
            return false;
        }

        if (WorkspaceLocation.SERVER == asyncOp.getWorkspace().getLocation()) {
            if (newLocalAttrs.isReadOnly() || newLocalAttrs.isDirectory()) {
                return false;
            }
        } else
        /* Local workspace */
        {
            if (newLocalAttrs.isDirectory()) {
                return false;
            }

            final WorkspaceLocalItem lvEntry = action.getLocalVersionEntry();

            boolean fileIsWritable = false;

            fileIsWritable |= null == lvEntry;

            if (null != lvEntry) {
                fileIsWritable |= lvEntry.getLength() != newLocalAttrs.getSize();

                final long onDiskModifiedTime = newLocalAttrs.getModificationTime().getWindowsFilesystemTime();
                fileIsWritable |= lvEntry.getLastModifiedTime() != onDiskModifiedTime;
            }

            if (!fileIsWritable) {
                return false;
            }
        }

        // At this point we know we have a writable file. Let's check to see if
        // its contents match
        // the incoming file's contents.
        return !localContentIsRedundant(action.getTargetLocalItem(), action.getHashValue());
    }

    /**
     * Returns true if the local file exists and it has the same hash content as
     * the server file.
     *
     *
     * @param localItemPath
     * @param serverHashValue
     * @return
     */
    private static boolean localContentIsRedundant(final String localItemPath, final byte[] serverHashValue) {
        byte[] localHashValue;
        try {
            localHashValue = HashUtils.hashFile(new File(localItemPath), "MD5"); //$NON-NLS-1$
        } catch (final Exception e) {
            return false;
        }

        boolean redundant = false;
        if (serverHashValue != null && localHashValue != null && serverHashValue.length != 0) {
            redundant = Arrays.equals(serverHashValue, localHashValue);
        }

        return redundant;
    }

    /**
     * Process one operation in the context of an on-going
     * {@link AsyncGetOperation}. This handles files and directories; gets,
     * deletes, moves, etc. If the operation completes successfully, it queues
     * an acknowledgement to the server.
     */
    private void processOperation(final GetOperation action, final AsyncGetOperation asyncOp) {
        Check.notNull(action, "action"); //$NON-NLS-1$
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$

        // Get the new local item once since it has to be computed for
        // GetOperations.
        final String newLocalItem = action.getTargetLocalItem();

        /*
         * Check the path length here for compatibility with .NET, which
         * discovers the condition when ItemSpec.GetFullPath() is used.
         *
         * Only do for non-preview, as the .NET implementation would only
         * encounter the limit when writing files to disk.
         */
        if (newLocalItem != null && !asyncOp.isPreview()) {
            try {
                LocalPath.checkLocalItem(newLocalItem, null, false, false, false, true);
            } catch (final PathTooLongException e) {
                log.warn("Path too long, not getting", e); //$NON-NLS-1$
                onNonFatalError(
                    new VersionControlException(
                        MessageFormat.format(
                            Messages.getString("GetEngine.LocalPathTooLongFormat"), //$NON-NLS-1$
                            newLocalItem)),
                    asyncOp.getWorkspace());
                return;
            }
        }

        /*
         * If this get operation has been overridden by the target conflict
         * management code below (code that checks the existingLocalHash), then
         * just ignore this. To better understand this see the code below where
         * targetAction.ClearLocalItem() is called.
         */
        if (action.isDownloadCompleted()) {
            return;
        }

        // For conflicts, fire the conflict event.
        if (action.hasConflict()) {
            asyncOp.addConflict(action);
            recordEvent(asyncOp, OperationStatus.CONFLICT, action);
            return;
        }

        // Determine whether this is a pending rename that is changing the
        // path's case.
        final boolean isCaseChangingRename = action.isCaseChangingRename();

        /*
         * Tracks whether the operation's download is completed in this method,
         * versus being queued for asynchronous processing, so we can set
         * .tpattributes if it is complete.
         */
        boolean downloadCompletedHere = false;

        try {
            // **************************************
            // Error checks against the source item.
            // **************************************

            // Determine if the local item at the location we currently have it
            // exists.
            FileSystemAttributes existingLocalAttrs = new FileSystemAttributes();
            boolean existingLocalExists = false;

            if (action.getCurrentLocalItem() != null) {
                existingLocalAttrs = FileSystemUtils.getInstance().getAttributes(action.getCurrentLocalItem());
                existingLocalExists = existingLocalAttrs.exists();

                log.debug(MessageFormat.format(
                    "existingLocalAttrs = {0}, existingLocalExists = {1}", //$NON-NLS-1$
                    existingLocalAttrs,
                    existingLocalExists));

                /*
                 * If we are undoing an edit that is not also an add, set the
                 * file back to read-only so that we will not see it as a
                 * writable file later.
                 */
                if (asyncOp.getType() == ProcessType.UNDO) {
                    if (action.getChangeType().contains(ChangeType.EDIT)
                        && action.getChangeType().contains(ChangeType.ADD) == false) {
                        action.setOkayToOverwriteExistingLocal(true);

                        if (WorkspaceLocation.SERVER == asyncOp.getWorkspace().getLocation()
                            && existingLocalAttrs.isReadOnly() == false) {
                            existingLocalAttrs.setReadOnly(true);
                            existingLocalAttrs.setArchive(false);
                            FileSystemUtils.getInstance().setAttributes(
                                action.getCurrentLocalItem(),
                                existingLocalAttrs);
                            log.debug(
                                MessageFormat.format(
                                    "Setting file to read only (archive=true) as part of undoing an edit: {0}", //$NON-NLS-1$
                                    action.getCurrentLocalItem()));
                        }
                    }
                }

                // Check for problems deleting the source local file/directory.
                if (existingLocalExists
                    && (newLocalItem == null
                        || LocalPath.equals(action.getCurrentLocalItem(), newLocalItem) == false)) {
                    // Check if we are getting a file but the source is actually
                    // a directory.
                    if (action.getItemType() == ItemType.FILE
                        && !existingLocalAttrs.isSymbolicLink()
                        && existingLocalAttrs.isDirectory()) {
                        // I have decided to not make this an error, but rather
                        // to skip the deletion of the source.
                        existingLocalExists = false;
                    }
                }
            }

            // **************************************
            // Error checks against the target item.
            // **************************************

            /*
             * Check if there is a get operation against the target item. This
             * code is critical for breaking rename cycles (the case where all
             * items involved in the rename cycle have pending changes just
             * results in a conflict for each -- otherwise, get should make
             * progress).
             */
            FileSystemAttributes newLocalAttrs = new FileSystemAttributes();
            boolean newLocalExists = false;
            GetOperation targetAction = null;
            if (newLocalItem != null) {
                newLocalAttrs = FileSystemUtils.getInstance().getAttributes(newLocalItem);
                newLocalExists = newLocalAttrs.exists();

                log.debug(MessageFormat.format(
                    "newLocalAttrs = {0}, newLocalExists = {1}", //$NON-NLS-1$
                    newLocalAttrs.toString(),
                    newLocalExists));
                log.debug(MessageFormat.format("NewContentNeeded = {0}", action.isNewContentNeeded())); //$NON-NLS-1$

                // Check if we are getting a file but the target is actually a
                // directory.
                if (newLocalExists
                    && action.getItemType() != ItemType.FOLDER
                    && !newLocalAttrs.isSymbolicLink()
                    && newLocalAttrs.isDirectory()) {
                    asyncOp.addWarning(OperationStatus.TARGET_IS_DIRECTORY, action);
                    log.debug(MessageFormat.format("TargetIsDirectory, newLocalItem = {0}", newLocalItem)); //$NON-NLS-1$
                    return;
                }

                targetAction = asyncOp.getExistingLocalHash().get(newLocalItem);
                if (targetAction != null && targetAction != action && !isCaseChangingRename) {
                    /*
                     * Check if there is a pending change against the target
                     * item. If there is a target pending change and we're not
                     * processing the results of a merge or unshelve or undo
                     * (e.g., unshelve cyclic rename), we'll stop processing the
                     * current action. None of these errors are possible if this
                     * is a case changing rename.
                     */
                    if (newLocalExists
                        && action.getType() != ProcessType.UNSHELVE
                        && action.getType() != ProcessType.MERGE
                        && action.getType() != ProcessType.ROLLBACK
                        && action.getType() != ProcessType.UNDO
                        && targetAction.getEffectiveChangeType().isEmpty() == false) {
                        asyncOp.addWarning(OperationStatus.TARGET_LOCAL_PENDING, action, targetAction);
                        log.debug(
                            MessageFormat.format(
                                "TargetLocalPending, newLocalItem = {0}, targetAction.ChangeType = {1}", //$NON-NLS-1$
                                newLocalItem,
                                targetAction.getEffectiveChangeType()));
                        return;
                    } else if (newLocalExists
                        && !asyncOp.isOverwrite()
                        && isWritableFileConflict(asyncOp, action, newLocalAttrs)
                        && newLocalAttrs.isSymbolicLink() == false) {
                        // We have to stop if the target item is a writable
                        // file.
                        asyncOp.addWarning(OperationStatus.TARGET_WRITABLE, action);
                        log.debug(MessageFormat.format("TargetWritable, newLocalItem = {0}", newLocalItem)); //$NON-NLS-1$
                        return;
                    } else {
                        /*
                         * We have a get operation with this target as the
                         * source but it doesn't have a pending change. If the
                         * target is just a file, processing this action will
                         * handle it and there is no reason to complete the
                         * source portion of the target action. If the target is
                         * a folder, it will get added to the hash of items to
                         * not delete when the directory is created (see further
                         * down).
                         */
                        if (targetAction.getItemType() == ItemType.FILE) {
                            /*
                             * In order to make this work in the face of
                             * crashes, I need to actually tell the server that
                             * I no longer have it. We must lock the target
                             * action across both clearing the local item and
                             * posting the update to prevent a race condition
                             * where the ULV call for a download could happen in
                             * between.
                             */
                            synchronized (targetAction) {
                                log.debug(MessageFormat.format(
                                    "ProcessOperation: clearing source local item {0}", //$NON-NLS-1$
                                    action.getCurrentLocalItem()));

                                if (!asyncOp.isPreview() && !targetAction.isDownloadCompleted()) {
                                    /*
                                     * For a delete, we can complete the
                                     * operation (for merge, we need to ack it
                                     * as resolved as well). Otherwise, just
                                     * tell the server we don't have the item.
                                     */
                                    if (targetAction.isDelete()) {
                                        asyncOp.queueLocalVersionUpdate(
                                            targetAction,
                                            null,
                                            targetAction.getVersionLocal());
                                    }
                                }

                                /*
                                 * Only set the DownloadCompleted flag if the
                                 * target action is a delete; othwerwise, the
                                 * action hasn't been completed.
                                 */
                                if (targetAction.isDelete() && !targetAction.isDownloadCompleted()) {
                                    targetAction.setDownloadCompleted(true);
                                    downloadCompletedHere = true;
                                    recordEvent(asyncOp, OperationStatus.DELETING, targetAction);
                                }

                                // We no longer have this item at this location
                                // --
                                // don't call until after using
                                // the local item path in the ULV call.
                                targetAction.clearLocalItem();

                                // Now remove the location from the hash.
                                asyncOp.getExistingLocalHash().remove(newLocalItem);
                            }
                        }
                    }
                }
            }

            // if true, this is pending add which is being undone and we are
            // asked to delete it afterwards
            final boolean deleteAsUndoAdd = shouldDeleteAsUndoAdd(asyncOp, action);

            // **************************************
            // Time to perform the get.
            // **************************************

            // Check if we have something to get (rather than just deleting
            // something).
            if (!action.isDelete()) {
                // Handle getting folders very differently from getting files.
                if (action.getItemType() == ItemType.FOLDER) {
                    // Check if the target item is a writable file.
                    if (!asyncOp.isOverwrite()
                        && newLocalExists
                        && isWritableFileConflict(asyncOp, action, newLocalAttrs)) {
                        asyncOp.addWarning(OperationStatus.TARGET_WRITABLE, action);
                        log.debug(MessageFormat.format("TargetWritable, newLocalItem = {0}", newLocalItem)); //$NON-NLS-1$
                        return;
                    }

                    // Check if an item exists at the target location.
                    if (!asyncOp.isPreview() && !asyncOp.isNoDiskUpdate() && newLocalExists) {
                        // If it is just a file (we've already confirmed it's
                        // read-only) just delete it.
                        if (!newLocalAttrs.isDirectory()) {
                            if (!new File(newLocalItem).delete()) {
                                throw new IOException(
                                    MessageFormat.format(
                                        Messages.getString("GetEngine.CouldNotDeleteFileFormat"), //$NON-NLS-1$
                                        newLocalItem));
                            } else {
                                log.debug(
                                    MessageFormat.format(
                                        "Deleting read-only file that''s in the way of a directory: {0}", //$NON-NLS-1$
                                        newLocalItem));
                            }
                            newLocalExists = false;
                        }
                    }

                    String sourceLocalItem = null;

                    // if we are case changing rename and the item exists
                    // locally
                    if (isCaseChangingRename && existingLocalExists) {
                        sourceLocalItem = action.getCurrentLocalItem();
                    } else if (newLocalExists) {
                        // if the target already exists and we have a delete on
                        // the same path
                        // we convert it to a rename -- this takes care of the
                        // get /remap case
                        // where the case changes.
                        if (targetAction != null
                            && targetAction.getItemType() == ItemType.FOLDER
                            && targetAction.isDelete()
                            && LocalPath.lastPartEqualsCaseSensitive(
                                targetAction.getCurrentLocalItem(),
                                newLocalItem) == false) {
                            sourceLocalItem = targetAction.getCurrentLocalItem();
                        }
                    }

                    // Create the directory. The call to create a directory does
                    // not throw an exception
                    // if the dir already exists (e.g., due to a race
                    // condition).
                    if (!asyncOp.isPreview() && (!newLocalExists || sourceLocalItem != null)) {
                        // If this is a case changing rename then we can safely
                        // do a Directory.Move, so we do,
                        // but only if source directory exists (Bug: 448888)
                        if (sourceLocalItem != null) {
                            final File sourceLocalItemFile = new File(sourceLocalItem);

                            if (sourceLocalItemFile.renameTo(new File(newLocalItem)) == false) {
                                onNonFatalError(
                                    new IOException(MessageFormat.format(
                                        Messages.getString("GetEngine.FailedToRenameDirectoryFormat"), //$NON-NLS-1$
                                        sourceLocalItemFile,
                                        newLocalItem)),
                                    asyncOp.getWorkspace());
                            } else {
                                log.debug(MessageFormat.format(
                                    "Renamed directory: {0} -> {1}", //$NON-NLS-1$
                                    action.getCurrentLocalItem(),
                                    newLocalItem));
                            }
                        } else {
                            if (!asyncOp.isNoDiskUpdate()) {
                                final File newLocalFile = new File(newLocalItem);

                                if (newLocalFile.mkdirs() == false) {
                                    /*
                                     * Double-check that the directory does not
                                     * exist to avoid race conditions in mkdirs.
                                     */
                                    if (!newLocalFile.isDirectory()) {
                                        onNonFatalError(
                                            new IOException(MessageFormat.format(
                                                Messages.getString("GetEngine.FailedToCreateDirectoryFormat"), //$NON-NLS-1$
                                                newLocalItem)),
                                            asyncOp.getWorkspace());
                                    }
                                } else {
                                    log.debug(MessageFormat.format("Created directory: {0}", newLocalItem)); //$NON-NLS-1$
                                }
                            }
                        }
                    }

                    if (deleteAsUndoAdd) {
                        if (!asyncOp.getDeletes().containsKey(newLocalItem)) {
                            asyncOp.getDeletes().put(newLocalItem, action);
                        }
                    }
                    // Ensure that the newLocalItem folder will never be deleted
                    // by another operation.
                    else if (!asyncOp.getDontDeleteFolderHash().containsKey(newLocalItem)) {
                        asyncOp.getDontDeleteFolderHash().put(newLocalItem, action);
                    }

                    // Schedule the source file/directory for deletion if it is
                    // different from the target.
                    if (existingLocalExists && !LocalPath.equals(action.getCurrentLocalItem(), newLocalItem)) {
                        if (!asyncOp.getDeletes().containsKey(action.getCurrentLocalItem())) {
                            asyncOp.getDeletes().put(action.getCurrentLocalItem(), action);
                        }

                        // Go ahead and record the "move" and notify the server.
                        recordEvent(
                            asyncOp,
                            action.getCurrentLocalItem() == null ? OperationStatus.GETTING : OperationStatus.REPLACING,
                            action);

                        if (!asyncOp.isPreview()) {
                            asyncOp.queueLocalVersionUpdate(
                                action,
                                action.getTargetLocalItem(),
                                action.getVersionServer());
                            action.setDownloadCompleted(true);
                            downloadCompletedHere = true;
                        }
                    } else {
                        recordEvent(
                            asyncOp,
                            action.getCurrentLocalItem() == null ? OperationStatus.GETTING : OperationStatus.REPLACING,
                            action);

                        if (!asyncOp.isPreview()) {
                            if (asyncOp.getType() != ProcessType.PEND && asyncOp.getType() != ProcessType.UNDO
                                || !action.getEffectiveChangeType().contains(ChangeType.ADD)) {
                                /*
                                 * Queue a request to tell the server that I got
                                 * it. In a local workspace, when getting a
                                 * folder that we already have, use the force
                                 * option.
                                 */
                                asyncOp.queueLocalVersionUpdate(
                                    action,
                                    action.getTargetLocalItem(),
                                    action.getVersionServer(),
                                    asyncOp.getWorkspace().getLocation() == WorkspaceLocation.LOCAL);

                                action.setDownloadCompleted(true);
                                downloadCompletedHere = true;
                            }
                        }
                    }
                } else
                // Getting a file.
                {
                    /*
                     * If we are editing an existing file or the file exists at
                     * a different location, GetAll is false, and version local
                     * is the same as on the server, move the file.
                     */
                    if (existingLocalExists
                        && (action.getEffectiveChangeType().contains(ChangeType.EDIT)
                            && action.getVersionLocal() == action.getVersionServer()
                            || !asyncOp.isGetAll() && !action.isNewContentNeeded())) {
                        try {
                            // Force ignore case here so we can detect
                            // case-changing renames on all platforms.
                            if (LocalPath.equals(action.getCurrentLocalItem(), newLocalItem, true)) {
                                // When edit is true and we get to this point
                                // with the path not having
                                // changed, it is either a GetAll or there is
                                // nothing to download (the
                                // content didn't change on the server even
                                // though the version number did).
                                if (action.getEffectiveChangeType().contains(ChangeType.EDIT) && asyncOp.isGetAll()) {
                                    // When GetAll is specified and the file is
                                    // being edited, we obviously cannot
                                    // download the file. Do NOT add it to the
                                    // retry list!
                                    recordEvent(asyncOp, OperationStatus.UNABLE_TO_REFRESH, action);
                                    asyncOp.getStatus().incrementNumWarnings();
                                } else {
                                    // If this isn't a preview and this is a
                                    // case changing rename. (i.e. rename $/project
                                    // -> $/PROJECT)
                                    if (!asyncOp.isPreview() && isCaseChangingRename && !asyncOp.isNoDiskUpdate()) {
                                        if (new File(action.getCurrentLocalItem()).renameTo(
                                            new File(newLocalItem)) == false) {
                                            onNonFatalError(
                                                new IOException(MessageFormat.format(
                                                    Messages.getString("GetEngine.FailedToRenameFileFormat"), //$NON-NLS-1$
                                                    action.getCurrentLocalItem(),
                                                    newLocalItem)),
                                                asyncOp.getWorkspace());
                                        } else {
                                            log.debug(MessageFormat.format(
                                                "Renamed file from {0} to {1}", //$NON-NLS-1$
                                                action.getCurrentLocalItem(),
                                                newLocalItem));
                                        }
                                    }

                                    // For get there's nothing to do -- just go
                                    // ahead and fire the event. For pend/undo
                                    // the action happened on the server and we
                                    // need to fire the event, though there
                                    // is no disk update.
                                    if (asyncOp.getType() != ProcessType.GET || isCaseChangingRename) {
                                        recordEvent(asyncOp, OperationStatus.GETTING, action);
                                    }
                                }

                                // There's nothing that needs to be downloaded.
                                if (!asyncOp.isPreview()) {
                                    Check.isTrue(
                                        !action.isNewContentNeeded()
                                            || (action.getEffectiveChangeType().contains(ChangeType.EDIT)
                                                && asyncOp.isGetAll()),
                                        MessageFormat.format(
                                            "Local and server versions expected to be equal except for edit: {0}", //$NON-NLS-1$
                                            action));

                                    if (deleteAsUndoAdd) {
                                        if (new File(newLocalItem).delete() == false) {
                                            throw new IOException(
                                                MessageFormat.format(
                                                    Messages.getString("GetEngine.CouldNotDeleteFileFormat"), //$NON-NLS-1$
                                                    newLocalItem));
                                        }
                                    }
                                    // No need to queue a local version update
                                    // for the undo of a pending add. The call
                                    // to UndoPendingChanges removes the local
                                    // version row as part of that
                                    // call.
                                    else if (!(ProcessType.UNDO == asyncOp.getType()
                                        && action.getChangeType().contains(ChangeType.ADD))) {
                                        asyncOp.queueLocalVersionUpdate(
                                            action,
                                            action.getTargetLocalItem(),
                                            action.getVersionServer());

                                        action.setDownloadCompleted(true);
                                        downloadCompletedHere = true;
                                    }
                                }

                                return;
                            }
                        } finally {
                            /*
                             * If the file is checked out, make sure it is
                             * writable. This is also necessary for the code
                             * further down that maintains the read-only setting
                             * of the source when performing the copy/delete
                             * move.
                             */
                            if (!asyncOp.isPreview()
                                && action.getEffectiveChangeType().contains(ChangeType.EDIT)
                                && existingLocalAttrs.isReadOnly()
                                && !existingLocalAttrs.isSymbolicLink()) {
                                existingLocalAttrs.setReadOnly(false);
                                existingLocalAttrs.setArchive(true);
                                FileSystemUtils.getInstance().setAttributes(
                                    action.getCurrentLocalItem(),
                                    existingLocalAttrs);
                                log.debug(MessageFormat.format(
                                    "Set edited file to read/write (archive=true): {0}", //$NON-NLS-1$
                                    action.getCurrentLocalItem()));
                            }
                        }

                        // Check for a writable target before attempting the
                        // move (we know it's not a directory).
                        if (!asyncOp.isOverwrite()
                            && newLocalExists
                            && isWritableFileConflict(asyncOp, action, newLocalAttrs)
                            && !asyncOp.isNoDiskUpdate()) {
                            asyncOp.addWarning(OperationStatus.TARGET_WRITABLE, action);
                            return;
                        }

                        if (!asyncOp.isPreview() && !deleteAsUndoAdd && !asyncOp.isNoDiskUpdate()) {
                            /**
                             * We may get a rename for a file to a directory
                             * that does not exist. (Particularly when handling
                             * name conflicts and the user chooses the
                             * destination filename.)
                             */
                            FileHelpers.createDirectoryIfNecessary(LocalPath.getParent(newLocalItem));

                            // Copy the source over the target file (we know at
                            // this point that the target
                            // must be read-only). If we are undoing pending add
                            // which is under pending rename, we just don't copy
                            // the file.

                            if (newLocalAttrs.exists()) {
                                new File(newLocalItem).delete();
                            }

                            if (!existingLocalAttrs.isSymbolicLink()) {
                                FileCopyHelper.copy(action.getCurrentLocalItem(), newLocalItem);
                                log.debug(MessageFormat.format(
                                    "Copied file from {0} to {1}", //$NON-NLS-1$
                                    action.getCurrentLocalItem(),
                                    newLocalItem));

                                // Apply the appropriate last-write time
                                // forward.
                                if (asyncOp.getWorkspace().getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                                    && !DotNETDate.MIN_CALENDAR.equals(action.getVersionServerDate())) {
                                    final File newLocalFile = new File(newLocalItem);
                                    if (existingLocalAttrs.isReadOnly()) {
                                        existingLocalAttrs.setReadOnly(false);
                                        FileSystemUtils.getInstance().setAttributes(newLocalFile, existingLocalAttrs);
                                        existingLocalAttrs.setReadOnly(true);
                                    }

                                    if (action.getEffectiveChangeType().contains(ChangeType.EDIT)) {
                                        newLocalFile.setLastModified(action.getVersionServerDate().getTimeInMillis());
                                    } else {
                                        // Pending edit; carry the existing
                                        // timestamp forward
                                        final FileSystemAttributes attrs =
                                            FileSystemUtils.getInstance().getAttributes(action.getCurrentLocalItem());
                                        newLocalFile.setLastModified(attrs.getModificationTime().getJavaTime());
                                    }
                                }
                            } else {
                                final FileSystemUtils util = FileSystemUtils.getInstance();
                                final String destinationPath = util.getSymbolicLink(action.getCurrentLocalItem());
                                util.createSymbolicLink(destinationPath, newLocalItem);
                            }

                            // We must preserve the read/write setting since the
                            // user may have attrib'ed the file outside of this program.
                            FileSystemUtils.getInstance().setAttributes(newLocalItem, existingLocalAttrs);
                        }

                        // Report that we are actually getting the file.
                        recordEvent(
                            asyncOp,
                            action.getCurrentLocalItem() == null ? OperationStatus.GETTING : OperationStatus.REPLACING,
                            action);

                        if (asyncOp.isPreview()) {
                            return;
                        }

                        // Tell the server that the file is in the new location.
                        asyncOp.queueLocalVersionUpdate(action, action.getTargetLocalItem(), action.getVersionServer());

                        synchronized (action) {
                            action.setDownloadCompleted(true);
                            downloadCompletedHere = true;

                            // Delete the source.
                            if (!asyncOp.isNoDiskUpdate()) {
                                if (new File(action.getCurrentLocalItem()).delete() == false) {
                                    throw new IOException(
                                        MessageFormat.format(
                                            Messages.getString("GetEngine.CouldNotDeleteFileFormat"), //$NON-NLS-1$
                                            action.getCurrentLocalItem()));
                                } else {
                                    log.debug(MessageFormat.format(
                                        "Deleted file source of move: {0}", //$NON-NLS-1$
                                        action.getCurrentLocalItem()));
                                }
                            }
                        }
                    } else {
                        // Download the file unless the source or target is
                        // writable and Overwrite is not specified.
                        if (action.getEffectiveChangeType().contains(ChangeType.ADD) && !action.isNewContentNeeded()) {
                            // If the action is for a file with a pending add
                            // and we don't have or it is not on disk, there is
                            // nothing more we can do. For pend or undo, the
                            // change still happened on the server, so fire the
                            // regular event.
                            if (asyncOp.getType() == ProcessType.PEND || asyncOp.getType() == ProcessType.UNDO) {
                                recordEvent(asyncOp, OperationStatus.GETTING, action);
                            }

                            // Let the user know that there is an error unless
                            // we are processing an Undo request or a Pend that
                            // has Preview turned on (happens in the VSIP code).
                            if (!asyncOp.isNoDiskUpdate()
                                && asyncOp.getType() != ProcessType.UNDO
                                && (!asyncOp.isPreview() || asyncOp.getType() != ProcessType.PEND)) {
                                if (newLocalItem != null) {
                                    onNonFatalError(
                                        new VersionControlException(MessageFormat.format(
                                            Messages.getString("GetEngine.AddedItemMissingLocallyFormat"), //$NON-NLS-1$
                                            newLocalItem)),
                                        asyncOp.getWorkspace());
                                } else {
                                    onNonFatalError(
                                        new VersionControlException(MessageFormat.format(
                                            Messages.getString("GetEngine.AddedItemMissingLocallyFormat"), //$NON-NLS-1$
                                            action.getCurrentLocalItem())),
                                        asyncOp.getWorkspace());
                                }
                            }

                            return;
                        } else if (!asyncOp.isOverwrite()
                            && WorkspaceLocation.SERVER == asyncOp.getWorkspace().getLocation()
                            && existingLocalExists
                            && !existingLocalAttrs.isReadOnly()
                            && !action.isOkayToOverwriteExistingLocal()
                            && !isCaseChangingRename
                            && !localContentIsRedundant(action.getCurrentLocalItem(), action.getHashValue())
                            && !existingLocalAttrs.isSymbolicLink()) {
                            asyncOp.addWarning(OperationStatus.SOURCE_WRITABLE, action, null);
                        } else if (!asyncOp.isOverwrite()
                            && newLocalExists
                            && isWritableFileConflict(asyncOp, action, newLocalAttrs)
                            && !isCaseChangingRename
                            && !newLocalAttrs.isSymbolicLink()) {
                            asyncOp.addWarning(OperationStatus.TARGET_WRITABLE, action);
                            return;
                        } else {
                            // Report that we are actually getting the file.
                            recordEvent(
                                asyncOp,
                                action.getCurrentLocalItem() == null ? OperationStatus.GETTING
                                    : OperationStatus.REPLACING,
                                action);

                            if (action.isContentDestroyed()) {
                                onNonFatalError(
                                    new DestroyedContentUnavailableException(MessageFormat.format(
                                        Messages.getString("GetEngine.DestroyedFileContentUnavailableExceptionFormat"), //$NON-NLS-1$
                                        action.getVersionServer(),
                                        action.getTargetLocalItem())),
                                    asyncOp.getWorkspace());
                                return;
                            }

                            // Don't go any further if we aren't actually
                            // getting it.
                            if (asyncOp.isPreview()) {
                                return;
                            }

                            Check.isTrue(
                                !deleteAsUndoAdd,
                                "We are downloading file which is not needed (undo of pending add)"); //$NON-NLS-1$

                            if (action.isUndo()
                                && null != action.getBaselineFileGUID()
                                && null == action.getDownloadURL()) {
                                // Local workspace offline undo (baseline folder
                                // restore)
                                if (!asyncOp.isNoDiskUpdate()) {
                                    // check symbolic link first
                                    final boolean isSymlink = PropertyConstants.IS_SYMLINK.equals(
                                        PropertyUtils.selectMatching(
                                            action.getPropertyValues(),
                                            PropertyConstants.SYMBOLIC_KEY));

                                    asyncOp.getBaselineFolders().copyBaselineToTarget(
                                        action.getBaselineFileGUID(),
                                        action.getTargetLocalItem(),
                                        -1,
                                        action.getHashValue(),
                                        isSymlink);

                                }

                                if (asyncOp.getWorkspace().getOptions().contains(WorkspaceOptions.SET_FILE_TO_CHECKIN)
                                    && !DotNETDate.MIN_CALENDAR.equals(action.getVersionServerDate())) {
                                    new File(action.getTargetLocalItem()).setLastModified(
                                        action.getVersionServerDate().getTimeInMillis());
                                }

                                asyncOp.queueLocalVersionUpdate(
                                    new ClientLocalVersionUpdate(
                                        action.getSourceServerItem(),
                                        action.getItemID(),
                                        action.getTargetLocalItem(),
                                        action.getVersionServer(),
                                        action.getEncoding(),
                                        false,
                                        action.getPropertyValues()));

                                if (existingLocalExists
                                    && action.getCurrentLocalItem() != null
                                    && !LocalPath.equals(action.getCurrentLocalItem(), action.getTargetLocalItem())
                                    && !asyncOp.isNoDiskUpdate()) {
                                    Check.isTrue(
                                        action.getItemType() != ItemType.FOLDER,
                                        MessageFormat.format(
                                            "Should not try to delete a folder here: {0}", //$NON-NLS-1$
                                            action.toString()));

                                    deleteSource(action, existingLocalAttrs);
                                }

                                downloadCompletedHere = true;
                            } else if (null != action.getDownloadURL()) {
                                // Download URL get (common case)
                                asyncGetFile(
                                    action,
                                    existingLocalExists,
                                    existingLocalAttrs,
                                    newLocalExists,
                                    newLocalAttrs,
                                    asyncOp);
                            }
                        }
                    }
                }
            } else
            // Operation is a delete.
            {
                // if the server sent back any and we want to do this if the
                // existing item is a directory
                if (action.getItemType() == ItemType.FOLDER
                    || (action.getItemType() == ItemType.ANY
                        && !existingLocalAttrs.isSymbolicLink()
                        && existingLocalAttrs.isDirectory())) {
                    // Normally, we just have to queue folder deletes. This is
                    // because we don't want to do it until folders are empty
                    // and the async nature of get makes it really hard to
                    // determine when that is. When the current local path is
                    // null, we just fire an event.
                    if (action.getCurrentLocalItem() == null) {
                        recordEvent(asyncOp, OperationStatus.DELETING, action);
                    } else if (!asyncOp.getDeletes().containsKey(action.getCurrentLocalItem())) {
                        asyncOp.getDeletes().put(action.getCurrentLocalItem(), action);
                    }
                } else {
                    // If the file is writable, stop. Otherwise, delete the
                    // file.
                    if (!asyncOp.isOverwrite()
                        && existingLocalExists
                        && WorkspaceLocation.SERVER == asyncOp.getWorkspace().getLocation()
                        && !existingLocalAttrs.isReadOnly()
                        && !existingLocalAttrs.isSymbolicLink()
                        && !action.isOkayToOverwriteExistingLocal()) {
                        Check.isTrue(
                            !action.getEffectiveChangeType().contains(ChangeType.EDIT),
                            MessageFormat.format(
                                "The edit bit is set, yet we are trying to delete this file: {0}", //$NON-NLS-1$
                                action));
                        asyncOp.addWarning(OperationStatus.SOURCE_WRITABLE, action, null);
                    } else {
                        recordEvent(asyncOp, OperationStatus.DELETING, action);

                        // Don't go any further if we aren't actually deleting
                        // it.
                        if (asyncOp.isPreview()) {
                            return;
                        }

                        // Delete the file and acknowledge it.
                        deleteSource(action, existingLocalAttrs);

                        asyncOp.queueLocalVersionUpdate(
                            action,
                            null,
                            action.getVersionServer() != 0 ? action.getVersionServer() : action.getVersionLocal());
                        action.setDownloadCompleted(true);
                        downloadCompletedHere = true;
                    }
                }
            }
        } catch (final PathTooLongException e) {
            /*
             * We already checked the target item at the top of this method, but
             * LocalPath.canonicalize or LocalPath.checkLocalItem may have
             * detected another path that exceeds the limit.
             */
            log.warn("Path too long, not getting", e); //$NON-NLS-1$
            onNonFatalError(
                new VersionControlException(MessageFormat.format(
                    Messages.getString("GetEngine.LocalPathTooLongFormat"), //$NON-NLS-1$
                    newLocalItem)),
                asyncOp.getWorkspace());
        } catch (final CanceledException e) {
            // Don't convert these to non-fatals
            throw e;
        } catch (final Exception e) {
            // Note that we'll catch exceptions due to problems such as unable
            // to open a file for writing because another process has it locked.

            log.warn("Caught and converted an exception: ", e); //$NON-NLS-1$
            onNonFatalError(e, asyncOp.getWorkspace());
        } finally {
            /*
             * Apply .tpattributes if the download completed here. If it was
             * queued for asynch processing, it will not be marked completed
             * here and attributes get applied elsewhere.
             */
            if (downloadCompletedHere && !asyncOp.isPreview() && !asyncOp.isNoDiskUpdate()) {
                applyFileAttributesAfterGet(asyncOp, action);
            }
        }
    }

    /**
     * Begin an async get file and setup the necessary state so that completion
     * happens correctly.
     */
    private void asyncGetFile(
        final GetOperation action,
        final boolean existingLocalExists,
        final FileSystemAttributes existingLocalAttrs,
        final boolean newLocalExists,
        final FileSystemAttributes newLocalAttrs,
        final AsyncGetOperation asyncOp) {
        if (!asyncOp.isNoDiskUpdate()) {
            // if the content we are downloading is a diff against the version
            // rather than the version itself, don't store the baseline (because
            // it's not a baseline at all)
            if (asyncOp.getBaselineFolders() != null && !action.getEffectiveChangeType().contains(ChangeType.EDIT)) {
                action.setBaselineFileGUID(GUID.newGUID().getGUIDBytes());
            }

            final GetDownloadWorker worker = new GetDownloadWorker(
                EventSource.newFromHere(),
                TaskMonitorService.getTaskMonitor(),
                client,
                this,
                action,
                asyncOp,
                existingLocalAttrs,
                asyncOp.getBaselineFolders(),
                action.getBaselineFileGUID());

            /*
             * Get the .tpattributes file synchronously. This is kind of a hack
             * but it's the easiest way to preserve asynchronous behavior for
             * the majority of downloads, and still apply extended attributes
             * correctly.
             *
             * The problem is the .tpattributes file contains information about
             * how to complete downloads (set attributes) on other files in this
             * directory, but we may not have it on disk in time for other items
             * that are being downloaded concurrently.
             *
             * GetEngine always sorts items to get to put .tpattributes files
             * first, but since we use a completion service backed by a thread
             * pool, we can't guarantee the order the downloads start or
             * complete.
             *
             * The solution is to synchronously download the .tpattributes file.
             * A .tpattributes file always sorts after all the files that can't
             * be affected by it, and before all the ones that can.
             */
            if (action.getTargetServerItem() != null
                && ServerPath.getFileName(action.getTargetServerItem()).equals(FileAttributesFile.DEFAULT_FILENAME)) {
                try {
                    worker.call();
                } catch (final Exception e) {
                    log.warn("Exception downloading synchronously", e); //$NON-NLS-1$
                }
            } else {
                /*
                 * A normal download. Let our thread pool execute this task.
                 * submit() will block if all the workers are busy (because the
                 * completion service wraps a BoundedExecutor), which is what we
                 * want. This keeps our downloads from getting too far ahead of
                 * our disk completion I/O and keeps the network from being
                 * saturated by too many concurrent requests.
                 */
                asyncOp.getCompletionService().submit(worker);
            }
        } else {
            /*
             * GetOptions.NoDiskUpdate was true, so just tell the server we did
             * the get though we didn't touch any files.
             */
            asyncOp.queueLocalVersionUpdate(action, action.getTargetLocalItem(), action.getVersionServer());
        }
    }

    /**
     * Reads the .tpattributes file from the directory that contains the given
     * localItemPath and stores a mapping between lower-cased file names and
     * attributes in this class instance's localDirectoryToAttributesMapCache.
     *
     * @param localItem
     *        a local file path, whose parent directory contains the
     *        .tpattributes file to load (must not be <code>null</code> or
     *        empty).
     */
    private synchronized void ensureAttributesReadIntoCache(final String localItem) {
        Check.notNullOrEmpty(localItem, "localItem"); //$NON-NLS-1$

        List<FileAttributesEntry> entries = null;

        /*
         * Check the cache for an existing list of attributes so we can skip
         * loading them from disk to save time. If an entry exists in the map
         * with a null value, this means there is no file attributes file to
         * load at that directory. If no entry exists at all, we should try to
         * load the file and update the map.
         */
        final String localDirectory = LocalPath.getDirectory(localItem);
        if (localDirectoryToEntryListMap.containsKey(localDirectory) == true) {
            final List<FileAttributesEntry> value = localDirectoryToEntryListMap.get(localDirectory);

            /*
             * Null value means we have tried and failed to load the attributes
             * file for this directory, so there are no attributes to apply.
             */

            if (value == null) {
                return;
            }

            entries = value;
        } else {
            /*
             * Try to load and update the cache.
             */
            entries = FileAttributesFile.loadAttributesFile(
                localDirectory + File.separator + FileAttributesFile.DEFAULT_FILENAME);

            if (entries == null) {
                /*
                 * Do not put null into the cache if the file we're getting is
                 * the actual attributes file, because the next time we go
                 * through here the file can be loaded.
                 */
                if (Collator.getInstance().equals(
                    LocalPath.getFileName(localItem),
                    FileAttributesFile.DEFAULT_FILENAME) == false) {
                    localDirectoryToEntryListMap.put(localDirectory, null);
                }

                return;
            }

            /*
             * Update the cache with the new entries.
             */
            localDirectoryToEntryListMap.put(localDirectory, entries);
        }
    }

    /**
     * Reads the global tpattributes file, if it exists, and stores a mapping
     * Reads the .tpattributes file from the directory that contains the given
     * localItemPath and stores a mapping between lower-cased file names and
     * attributes in this class instance's localDirectoryToAttributesMapCache.
     *
     * @param localItem
     *        a local file path, whose parent directory contains the
     *        .tpattributes file to load (must not be <code>null</code> or
     *        empty).
     */
    private synchronized void ensureGlobalAttributesReadIntoCache() {
        if (globalAttributeEntries == null) {
            List<FileAttributesEntry> entries = null;
            final String globalPath =
                PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.GLOBAL_TPATTRIBUTES);

            if (globalPath != null && globalPath.length() > 0) {
                entries = FileAttributesFile.loadGlobalFile(globalPath);

                if (entries == null) {
                    log.warn(MessageFormat.format("Could not read global attributes file {0}", globalPath)); //$NON-NLS-1$
                }
            }

            globalAttributeEntries = (entries != null) ? entries : new ArrayList<FileAttributesEntry>();
        }
    }

    private boolean isTextEncoding(final int encoding) {
        return (encoding != FileEncoding.BINARY.getCodePage());
    }

    /**
     * Gets the file attributes entries that apply to the given path name,
     * loading them from the .tpattributes file if present.
     *
     * @param localItem
     *        the local item to get attributes for (must not be
     *        <code>null</code> or empty).
     * @param includeDefaults
     *        include default file attributes from environment variables and/or
     *        system properties. recommended only when the given file is a text
     *        file.
     * @return the attribute entry loaded for the given local item, or null if
     *         the .tpattributes file could not be read or the given item did
     *         not have an entry.
     */
    public synchronized FileAttributesCollection getAttributesForFile(
        final String localItem,
        final String serverItem,
        final boolean includeDefaults) {
        Check.notNullOrEmpty(localItem, "localItem"); //$NON-NLS-1$

        // Make sure the attributes have been loaded.
        ensureAttributesReadIntoCache(localItem);

        // We don't apply attributes to .tpattributes files.
        if (LocalPath.getFileName(localItem).equalsIgnoreCase(FileAttributesFile.DEFAULT_FILENAME)) {
            return defaultFileAttributes;
        }

        /*
         * Get the list of attributes entries for this directory and match.
         */
        final String fileName = LocalPath.getFileName(localItem);
        final List<FileAttributesEntry> entries = localDirectoryToEntryListMap.get(LocalPath.getDirectory(localItem));
        if (entries != null) {
            /*
             * Find an entry that matches the given filename. This is a linear
             * walk of the entries, which scales somewhat poorly with large
             * attributes files, but those are rare and solutions to efficiently
             * sort and search on regular expressions are complicated.
             */
            for (final FileAttributesEntry entry : entries) {
                if (entry != null && entry.matchesFilename(fileName)) {
                    // Merge with defaults
                    return entry.getAttributes().mergeWith(includeDefaults ? defaultFileAttributes : null);
                }
            }
        }

        /* No match in the .tpattributes; look for global attributes. */
        if (serverItem != null) {
            ensureGlobalAttributesReadIntoCache();

            for (final FileAttributesEntry entry : globalAttributeEntries) {
                if (entry != null && entry.matchesFilename(serverItem)) {
                    // Merge with defaults
                    return entry.getAttributes().mergeWith(includeDefaults ? defaultFileAttributes : null);
                }
            }
        }

        return includeDefaults ? defaultFileAttributes : null;
    }

    /**
     * Checks the directory that contains the given local item path for a
     * .tpattributes file, and if it exists, reads it and applies any attributes
     * listed there that should be applied to the temp file before is is renamed
     * to the final working folder name.
     *
     * @param targetServerItem
     *        the server target of the operation. This file is not modified. If
     *        null, this method does nothing.
     * @param targetLocalItem
     *        the local target of the operation. This file is not modified. If
     *        null, this method does nothing.
     * @param tempFile
     *        the temporary file whose attributes/contents will be updated. Must
     *        not be null.
     */
    public void applyFileAttributesToTempFile(
        final String targetServerItem,
        final String targetLocalItem,
        final int encoding,
        final File tempFile) {
        applyFileAttributesToTempFile(targetServerItem, targetLocalItem, encoding, tempFile, null);
    }

    public void applyFileAttributesToTempFile(
        final String targetServerItem,
        final String targetLocalItem,
        final int encoding,
        final File tempFile,
        final GetOperation operation) {
        if (targetServerItem == null || targetLocalItem == null) {
            return;
        }

        Check.notNull(tempFile, "tempFile"); //$NON-NLS-1$

        final FileAttributesCollection attributes =
            getAttributesForFile(targetLocalItem, targetServerItem, isTextEncoding(encoding));

        if (attributes != null) {
            /*
             * Handle AppleSingle decoding: this must be done first, as other
             * operations need to deal with the data fork that will be decoded
             * from this file.
             */
            final StringPairFileAttribute transformAttribute =
                attributes.getStringPairFileAttribute(FileAttributeNames.TRANSFORM);

            if (transformAttribute != null
                && "apple".equals(transformAttribute.getValue()) //$NON-NLS-1$
                && AppleSingleUtil.isSupportedPlatform()) {
                log.debug(MessageFormat.format("Decoding AppleSingle file for {0}", targetLocalItem)); //$NON-NLS-1$

                try {
                    AppleSingleUtil.decodeFile(tempFile);
                } catch (final IOException e) {
                    final String message =
                        MessageFormat.format(
                            Messages.getString("GetEngine.CouldNotDecodeAppleSingleFileFormat"), //$NON-NLS-1$
                            targetLocalItem,
                            e.getLocalizedMessage());

                    log.warn(message, e);
                    onNonFatalError(new TECoreException(message));
                }
            }

            /* Handle EOL conversion */
            final StringPairFileAttribute eolAttribute =
                attributes.getStringPairFileAttribute(FileAttributeNames.CLIENT_EOL);

            if (eolAttribute != null && eolAttribute.getValue() != null) {
                if (encoding == VersionControlConstants.ENCODING_UNCHANGED
                    && client.getServiceLevel().getValue() < WebServiceLevel.TFS_2012.getValue()
                    && operation != null) {
                    final Item item = client.getItem(operation.getItemID(), operation.getVersionServer());
                    operation.setEncoding(item.getEncoding().getCodePage());
                }

                if (operation.getEncoding() != FileEncoding.BINARY.getCodePage()) {

                    final String desiredNewlineSequence =
                        FileAttributeValues.getEndOfLineStringForAttributeValue(eolAttribute);

                    if (desiredNewlineSequence == null) {
                        onNonFatalError(
                            new TECoreException(
                                MessageFormat.format(
                                    Messages.getString("GetEngine.UnsupportedClientEOLStyleFormat"), //$NON-NLS-1$
                                    eolAttribute.getValue(),
                                    targetLocalItem,
                                    FileAttributesFile.DEFAULT_FILENAME)));
                    } else if (desiredNewlineSequence.equals("")) //$NON-NLS-1$
                    {
                        log.debug(MessageFormat.format("Not converting line endings for {0}", targetLocalItem)); //$NON-NLS-1$
                    } else {
                        log.debug(MessageFormat.format(
                            "Converting line endings for {0} to {1}", //$NON-NLS-1$
                            targetLocalItem,
                            eolAttribute.getValue()));

                        try {
                            Charset charset = CodePageMapping.getCharset(operation.getEncoding(), false);

                            if (charset == null) {
                                charset = Charset.defaultCharset();
                            }

                            NewlineUtils.convertFile(tempFile, charset, desiredNewlineSequence);

                            log.info(MessageFormat.format(
                                "Converted line endings in {0} to {1} (using charset {2})", //$NON-NLS-1$
                                tempFile,
                                eolAttribute.getValue(),
                                charset.name()));
                        } catch (final UnsupportedEncodingException e) {
                            final String message = MessageFormat.format(
                                Messages.getString("GetEngine.CouldNotChangeEOLBecauseUnknownFileEncodingFormat"), //$NON-NLS-1$
                                targetLocalItem,
                                e.getLocalizedMessage());

                            log.error(message, e);
                            onNonFatalError(new TECoreException(message));
                        } catch (final IOException e) {
                            final String message = MessageFormat.format(
                                Messages.getString("GetEngine.CouldNotChangeEOLBecauseIOExceptionFormat"), //$NON-NLS-1$
                                targetLocalItem,
                                e.getLocalizedMessage());

                            log.error(message, e);
                            onNonFatalError(new TECoreException(message));
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies filesystem attributes specified by certain TFS 2012 property
     * values or a .tpattributes file in the directory where the target local
     * item resides.
     * <p>
     * If no special property values are defined or no entries are found in the
     * .tpattributes table, the file's attributes are set to their default
     * values.
     *
     * @param asyncOp
     *        the {@link AsyncGetOperation} (must not be <code>null</code>)
     * @param operation
     *        the {@link GetOperation} (must not be <code>null</code>)
     */
    public void applyFileAttributesAfterGet(final AsyncGetOperation asyncOp, final GetOperation operation) {
        /*
         * Return early if "no disk update".
         *
         * Execute bit (the only attribute currently supported) is only
         * supported on Unix.
         *
         * There must be a target item to update.
         *
         * Skip for pended adds because the execute bit might have been set on
         * the file by the user before the pend and we want to preserve that
         * setting.
         */
        if (asyncOp.isNoDiskUpdate()
            || !Platform.isCurrentPlatform(Platform.GENERIC_UNIX)
            || operation.getTargetLocalItem() == null
            || (asyncOp.getType() == ProcessType.PEND && operation.getChangeType().contains(ChangeType.ADD))) {
            return;
        }

        final String localItem = operation.getTargetLocalItem();
        final String serverItem = operation.getTargetServerItem();

        final FileSystemAttributes attr = FileSystemUtils.getInstance().getAttributes(localItem);

        /*
         * Item must exist and be a file (executable bit not controlled for
         * directories).
         */
        if (!attr.exists() || attr.isDirectory()) {
            return;
        }

        boolean desiredExecutable = false;

        /*
         * Executable via TFS 2012 property?
         */
        if (PlatformMiscUtils.getInstance().getEnvironmentVariable(
            EnvironmentVariables.DISABLE_APPLY_EXECUTABLE_PROP) == null) {
            desiredExecutable = PropertyConstants.EXECUTABLE_ENABLED_VALUE.equals(
                PropertyUtils.selectMatching(operation.getPropertyValues(), PropertyConstants.EXECUTABLE_KEY));
        }

        /*
         * Executable via .tpattributes file?
         */
        if (!desiredExecutable) {
            final FileAttributesCollection attributes =
                getAttributesForFile(localItem, serverItem, isTextEncoding(operation.getEncoding()));

            if (attributes != null) {
                desiredExecutable = attributes.containsBooleanAttribute(FileAttributeNames.EXECUTABLE);
            }
        }

        /*
         * Always save the attributes if the desired value differs from the
         * existing value (that is, don't just save for "+x", also save for
         * "-x").
         *
         * Skip symbolic links because that would change the item at the other
         * end of the link.
         */
        if (attr.isSymbolicLink() == false) {
            if (attr.isExecutable() != desiredExecutable) {
                attr.setExecutable(desiredExecutable);
                FileSystemUtils.getInstance().setAttributes(localItem, attr);
            }
        }
    }

    /**
     * Waits for all the tasks that have been submitted to the given
     * {@link AccountingCompletionService} to finish. This method may be called
     * multiple times on a single completion service instance.
     *
     * @param completionService
     *        the {@link AccountingCompletionService} to wait on (must not be
     *        <code>null</code>)
     */
    public static void waitForCompletions(final AccountingCompletionService<WorkerStatus> completionService) {
        Check.notNull(completionService, "completionService"); //$NON-NLS-1$

        completionService.waitForCompletions(new ResultProcessor<WorkerStatus>() {
            @Override
            public void processResult(final WorkerStatus result) {
                final WorkerStatus status = result;

                if (status.getFinalState() == FinalState.ERROR) {
                    log.debug("Get worker thread finished with EXCEPTION"); //$NON-NLS-1$
                } else if (status.getFinalState() == FinalState.CANCELED) {
                    log.debug("Get worker thread finished with CANCELED"); //$NON-NLS-1$
                }
            }
        }, new ExecutionExceptionHandler() {
            @Override
            public void handleException(final ExecutionException e) {
                log.warn("Get worker exception", e); //$NON-NLS-1$
            }
        });
    }

    private void throwIfCanceled(final TaskMonitor gettingTaskMonitor) throws CoreCancelException {
        if (gettingTaskMonitor.isCanceled()) {
            final CoreCancelException e = new CoreCancelException();
            log.error(e);
            throw e;
        }
    }

    private void throwIfFatalError(final AsyncGetOperation asyncOp) {
        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$

        final Throwable fatalError = asyncOp.getFatalError();
        if (fatalError != null) {
            log.error("Fatal download error detected: ", fatalError); //$NON-NLS-1$
            if (fatalError instanceof VersionControlException) {
                throw (VersionControlException) fatalError;
            } else {
                throw new VersionControlException(fatalError);
            }
        }
    }

    /**
     * Used to synchronize the get engine loop in
     * {@link GetEngine#processGetOperations(ProcessType, GetOperation[][], GetOptions)}
     * with the {@link ShutdownEventListener} method, which is called on a new
     * thread when the JVM wants to shut down. This is a thread-safe monitor
     * object, which can be synchronized on and holds state.
     */
    private static class ShutdownMonitor {
        private volatile boolean isShutdown;

        public ShutdownMonitor() {
        }

        protected boolean isShutdown() {
            return isShutdown;
        }

        protected void setShutdown() {
            isShutdown = true;
        }
    }

    private static class GetShutdownEventListener implements ShutdownEventListener {
        private final TaskMonitor taskMonitor;
        private final ShutdownMonitor shutdownMonitor;

        GetShutdownEventListener(final TaskMonitor taskMonitor, final ShutdownMonitor shutdownMonitor) {
            Check.notNull(taskMonitor, "taskMonitor"); //$NON-NLS-1$
            Check.notNull(shutdownMonitor, "shutdownMonitor"); //$NON-NLS-1$

            this.taskMonitor = taskMonitor;
            this.shutdownMonitor = shutdownMonitor;
        }

        @Override
        public void onShutdown() {
            log.trace("listener shutting down in-progress get operation"); //$NON-NLS-1$

            /*
             * Unhook ourselves first, to reduce the likelyhood of multiple
             * invocations.
             */
            ShutdownManager.getInstance().removeShutdownEventListener(this, SHUTDOWN_HANDLER_PRIORITY);

            log.trace("listener canceling TaskMonitor"); //$NON-NLS-1$

            /*
             * Setting cancellation stops the download quickly because layers
             * below this one poll the monitor.
             */
            taskMonitor.setCanceled();

            log.trace("listener acquiring shutdown monitor"); //$NON-NLS-1$

            /*
             * We have to wait until the running get thread meets up with us
             * before allowing the shutdown manager to continue. It should meet
             * quickly since we canceled the get. If the shutdown manager
             * continued before cancellation, it might do things like shut down
             * network connections, and the get might crash.
             */
            synchronized (shutdownMonitor) {
                while (shutdownMonitor.isShutdown() == false) {
                    log.trace("listener waiting on monitor because no shutdown yet"); //$NON-NLS-1$
                    try {
                        shutdownMonitor.wait();
                        log.trace("listener monitor woke"); //$NON-NLS-1$
                    } catch (final InterruptedException e) {
                        log.error("listener monitor interrupted, ignoring", e); //$NON-NLS-1$
                    }
                }

                log.trace("listener confirmed shutdown"); //$NON-NLS-1$
            }
        }
    }
}
