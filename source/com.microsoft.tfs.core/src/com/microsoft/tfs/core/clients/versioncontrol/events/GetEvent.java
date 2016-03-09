// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.AsyncGetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when file is retrieved from the server.
 *
 * @since TEE-SDK-10.1
 */
public class GetEvent extends CoreClientEvent {
    private static final String GET_CONFLICT = Messages.getString("GetEvent.GetConflictFormat"); //$NON-NLS-1$
    private static final String GET_CONFLICT_MOVE = Messages.getString("GetEvent.GetConflictMoveFormat"); //$NON-NLS-1$
    private static final String GET_NAMESPACE_CONFLICT = Messages.getString("GetEvent.GetNamespaceConflictFormat"); //$NON-NLS-1$
    private static final String GET_NAMESPACE_CONFLICT_MOVE =
        Messages.getString("GetEvent.GetNamespaceConflictMoveFormat"); //$NON-NLS-1$
    private static final String GET_SOURCE_WRITABLE_NEW_TARGET =
        Messages.getString("GetEvent.GetSourceWritableNewTargetFormat"); //$NON-NLS-1$
    private static final String GET_WRITABLE = Messages.getString("GetEvent.GetWritableFormat"); //$NON-NLS-1$
    private static final String GET_LOCAL_PENDING_MOVE = Messages.getString("GetEvent.GetLocalPendingMoveFormat"); //$NON-NLS-1$
    private static final String GET_LOCAL_PENDING = Messages.getString("GetEvent.GetLocalPendingFormat"); //$NON-NLS-1$

    private static final String GET_GETTING_MOVE = Messages.getString("GetEvent.GetGettingMoveFormat"); //$NON-NLS-1$

    private static final String GET_GETTING = Messages.getString("GetEvent.GetGettingFormat"); //$NON-NLS-1$

    private static final String GET_REPLACING_MOVE = Messages.getString("GetEvent.GetReplacingMoveFormat"); //$NON-NLS-1$

    private static final String GET_REPLACING = Messages.getString("GetEvent.GetReplacingFormat"); //$NON-NLS-1$

    private static final String GET_DELETING = Messages.getString("GetEvent.GetDeletingFormat"); //$NON-NLS-1$

    private static final String CANT_DELETE_NON_EMPTY_DIR_PATH =
        Messages.getString("GetEvent.GetDeleteNonEmptyDirPathFormat"); //$NON-NLS-1$

    private static final String GET_TARGET_IS_DIR = Messages.getString("GetEvent.GetTargetIsDirFormat"); //$NON-NLS-1$
    private static final String GET_UNABLE_TO_REFRESH = Messages.getString("GetEvent.GetUnableToRefreshFormat"); //$NON-NLS-1$

    static final long serialVersionUID = 845047770683850528L;

    private final Workspace workspace;
    private final OperationStatus status;
    private final GetOperation action;
    private final String targetLocalItem;
    private final ChangeType targetPendingChangeType;
    private final PropertyValue[] targetPropertyValues;
    private final boolean diskUpdateAttempted;

    public GetEvent(
        final EventSource source,
        final AsyncGetOperation asyncOp,
        final OperationStatus status,
        final GetOperation action,
        final String targetLocalItem,
        final ChangeType targetChangeType,
        final PropertyValue[] targetPropertyValues) {
        super(source);

        Check.notNull(asyncOp, "asyncOp"); //$NON-NLS-1$

        this.workspace = asyncOp.getWorkspace();
        this.status = status;
        this.action = action;
        this.targetLocalItem = targetLocalItem;
        this.targetPendingChangeType = targetChangeType;
        this.diskUpdateAttempted = !asyncOp.isPreview();
        this.targetPropertyValues = action.getPropertyValues();
    }

    /**
     * Converts this event into a displayable message.
     *
     *
     * @param targetName
     *        the path to the target, perhaps relative to another directory
     * @param error
     *        the error message, if any, that is also a result of this event
     * @return the displayable message for a successful action, if any
     */
    public String getMessage(String targetName, final AtomicReference<String> error) {
        final GetOperation getOp = getOperation();

        String actionString;
        if (getOp.isDelete()) {
            actionString = Messages.getString("GetEvent.DeleteOperation"); //$NON-NLS-1$
        } else {
            actionString = Messages.getString("GetEvent.GetOperation"); //$NON-NLS-1$
        }

        if (targetName == null) {
            targetName = getServerItem();
        }

        String sourceName = getSourceLocalItem();
        if (sourceName == null) {
            sourceName = getServerItem();
        }

        error.set(null);
        String message = null;

        final OperationStatus opStatus = getStatus();
        if (OperationStatus.CONFLICT == opStatus) {
            if (getTargetLocalItem() == null) {
                error.set(MessageFormat.format(
                    Messages.getString("GetEvent.CannotDoOpDueToConflictFormat"), //$NON-NLS-1$
                    sourceName,
                    actionString,
                    getSourcePendingChangeTypeName()));
            } else {
                /*
                 * For namespace conflicts as well as version conflicts, the
                 * conflicting change in question is noted as the
                 * SourcePendingChangeType.
                 */
                if (!action.isNamespaceConflict()) {
                    if (getSourceLocalItem() == null || LocalPath.equals(getSourceLocalItem(), getTargetLocalItem())) {
                        error.set(MessageFormat.format(GET_CONFLICT, new Object[] {
                            targetName,
                            actionString,
                            getSourcePendingChangeTypeName()
                        }));
                    } else {
                        error.set(MessageFormat.format(GET_CONFLICT_MOVE, new Object[] {
                            targetName,
                            actionString,
                            getSourcePendingChangeTypeName(),
                            sourceName
                        }));
                    }
                } else {
                    if (getSourceLocalItem() == null) {
                        error.set(MessageFormat.format(GET_NAMESPACE_CONFLICT, new Object[] {
                            targetName,
                            actionString,
                            getSourcePendingChangeTypeName()
                        }));
                    } else {
                        error.set(MessageFormat.format(GET_NAMESPACE_CONFLICT_MOVE, new Object[] {
                            targetName,
                            actionString,
                            getSourcePendingChangeTypeName(),
                            sourceName
                        }));
                    }
                }
            }
        } else if (OperationStatus.SOURCE_WRITABLE == opStatus) {
            /*
             * For a delete or simply downloading a file, we don't want to show
             * source and target in the message. If it is a move, we do want to
             * show both.
             */
            if (isDelete()
                || getSourceLocalItem() == null
                || LocalPath.equals(getSourceLocalItem(), getTargetLocalItem())) {
                /*
                 * Deletes don't have a target. Otherwise, the source is null or
                 * the target and the source are the same path.
                 */
                String path = targetName;
                if (isDelete()) {
                    path = sourceName;
                }

                error.set(MessageFormat.format(GET_WRITABLE, new Object[] {
                    path,
                    actionString
                }));
            } else {
                error.set(MessageFormat.format(GET_SOURCE_WRITABLE_NEW_TARGET, new Object[] {
                    targetName,
                    actionString,
                    sourceName
                }));
            }
        } else if (OperationStatus.TARGET_LOCAL_PENDING == opStatus) {
            if (getSourceLocalItem() == null) {
                error.set(MessageFormat.format(GET_LOCAL_PENDING, new Object[] {
                    targetName,
                    actionString,
                    getTargetPendingChangeTypeName()
                }));
            } else {
                error.set(MessageFormat.format(GET_LOCAL_PENDING_MOVE, new Object[] {
                    targetName,
                    actionString,
                    getTargetPendingChangeTypeName(),
                    sourceName
                }));
            }
        } else if (OperationStatus.TARGET_WRITABLE == opStatus) {
            error.set(MessageFormat.format(GET_WRITABLE, new Object[] {
                targetName,
                actionString
            }));
        } else if (OperationStatus.GETTING == opStatus) {
            if (getSourceLocalItem() == null || LocalPath.equals(getSourceLocalItem(), getTargetLocalItem())) {
                message = MessageFormat.format(GET_GETTING, new Object[] {
                    targetName
                });
            } else {
                message = MessageFormat.format(GET_GETTING_MOVE, new Object[] {
                    targetName,
                    sourceName
                });
            }
        } else if (OperationStatus.REPLACING == opStatus) {
            if (getSourceLocalItem() == null || LocalPath.equals(getSourceLocalItem(), getTargetLocalItem())) {
                message = MessageFormat.format(GET_REPLACING, new Object[] {
                    targetName
                });
            } else {
                message = MessageFormat.format(GET_REPLACING_MOVE, new Object[] {
                    targetName,
                    sourceName
                });
            }
        } else if (OperationStatus.DELETING == opStatus) {
            message = MessageFormat.format(GET_DELETING, new Object[] {
                sourceName
            });
        } else if (OperationStatus.SOURCE_DIRECTORY_NOT_EMPTY == opStatus) {
            error.set(MessageFormat.format(CANT_DELETE_NON_EMPTY_DIR_PATH, new Object[] {
                sourceName
            }));
        } else if (OperationStatus.TARGET_IS_DIRECTORY == opStatus) {
            error.set(MessageFormat.format(GET_TARGET_IS_DIR, new Object[] {
                targetName
            }));
        } else if (OperationStatus.UNABLE_TO_REFRESH == opStatus) {
            error.set(MessageFormat.format(GET_UNABLE_TO_REFRESH, new Object[] {
                targetName
            }));
        }

        return message;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public boolean isLatest() {
        return action.isLatest();
    }

    public String getSourceLocalItem() {
        return action.getSourceLocalItem();
    }

    public String getTargetLocalItem() {
        return targetLocalItem;
    }

    public int getVersion() {
        return action.getVersionServer();
    }

    public int getDeletionID() {
        return action.getDeletionID();
    }

    public String getServerItem() {
        return action.getTargetServerItem();
    }

    public int getItemID() {
        return action.getItemID();
    }

    public ItemType getItemType() {
        return action.getItemType();
    }

    public boolean isDelete() {
        return action.isDelete();
    }

    public ChangeType getChangeType() {
        return action.getChangeType();
    }

    public boolean isDiskUpdateAttempted() {
        return diskUpdateAttempted;
    }

    public GetOperation getOperation() {
        return action;
    }

    public PropertyValue[] getTargetPropertyValues() {
        return targetPropertyValues;
    }

    private String getSourcePendingChangeTypeName() {
        if (!action.hasConflict()) {
            return action.getChangeType().toUIString(true, action.getPropertyValues());
        } else {
            return action.getConflictingChangeType().toUIString(true, action.getPropertyValues());
        }
    }

    private String getTargetPendingChangeTypeName() {
        Check.isTrue(status == OperationStatus.TARGET_LOCAL_PENDING, "status == OperationStatus.TARGET_LOCAL_PENDING"); //$NON-NLS-1$

        if (targetPendingChangeType == null) {
            return ChangeType.NONE.toUIString(true, targetPropertyValues);
        }

        return targetPendingChangeType.toUIString(true, targetPropertyValues);
    }
}
