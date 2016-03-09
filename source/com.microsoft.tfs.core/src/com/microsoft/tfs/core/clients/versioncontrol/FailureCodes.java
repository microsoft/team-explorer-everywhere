// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;

/**
 * Contains string codes for use in {@link Failure} objects built by the client
 * that match the server code values.
 *
 * @threadsafety immutable
 * @since TEE-SDK-11.0
 */
public class FailureCodes {
    /*
     * In VS the codes are the names of the exception classes, computed with
     * "typeof(SomeException).Name". They are reproduced here.
     */

    public static final String ITEM_CLOAKED_EXCEPTION = "ItemCloakedException"; //$NON-NLS-1$

    public static final String CHANGE_ALREADY_PENDING_EXCEPTION = "ChangeAlreadyPendingException"; //$NON-NLS-1$

    public static final String ITEM_EXISTS_EXCEPTION = "ItemExistsException"; //$NON-NLS-1$

    public static final String PENDING_PARENT_DELETE_EXCEPTION = "PendingParentDeleteException"; //$NON-NLS-1$

    public static final String CANNOT_CHANGE_ROOT_FOLDER_EXCEPTION = "CannotChangeRootFolderException"; //$NON-NLS-1$

    public static final String CANNOT_CREATE_FILES_IN_ROOT_EXCEPTION = "CannotCreateFilesInRootException"; //$NON-NLS-1$

    public static final String INVALID_PROJECT_PENDING_CHANGE_EXCEPTION = "InvalidProjectPendingChangeException"; //$NON-NLS-1$

    public static final String NOT_ALLOWED_ON_FOLDER_EXCEPTION = "NotAllowedOnFolderException"; //$NON-NLS-1$

    public static final String INCOMPATIBLE_CHANGE_EXCEPTION = "IncompatibleChangeException"; //$NON-NLS-1$

    public static final String PENDING_CHILD_EXCEPTION = "PendingChildException"; //$NON-NLS-1$

    public static final String PENDING_DELETE_CONFLICT_CHANGE_EXCEPTION = "PendingDeleteConflictChangeException"; //$NON-NLS-1$

    public static final String ITEM_NOT_CHECKED_OUT_EXCEPTION = "ItemNotCheckedOutException"; //$NON-NLS-1$

    public static final String ITEM_NOT_FOUND_EXCEPTION = "ItemNotFoundException"; //$NON-NLS-1$

    public static final String ITEM_NOT_MAPPED_EXCEPTION = "ItemNotMappedException"; //$NON-NLS-1$

    public static final String BASELINE_UNAVAILABLE_EXCEPTION = "BaselineUnavailableException"; //$NON-NLS-1$

    public static final String WILDCARD_NOT_ALLOWED_EXCEPTION = "WildcardNotAllowedException"; //$NON-NLS-1$

    public static final String RENAME_WORKING_FOLDER_EXCEPTION = "RenameWorkingFolderException"; //$NON-NLS-1$

    public static final String REPOSITORY_PATH_TOO_LONG_EXCEPTION = "RepositoryPathTooLongException"; //$NON-NLS-1$

    public static final String TARGET_CLOAKED_EXCEPTION = "TargetCloakedException"; //$NON-NLS-1$
}
