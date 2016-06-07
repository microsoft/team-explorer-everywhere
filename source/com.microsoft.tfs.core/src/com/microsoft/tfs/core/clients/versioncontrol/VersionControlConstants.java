// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

/**
 * Contains constant strings and numbers used by the version control client.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public abstract class VersionControlConstants {
    public final static String PROXY_BASE_DIR = "/VersionControlProxy"; //$NON-NLS-1$
    public final static String PROXY_DOWNLOAD_FILE_2005 = PROXY_BASE_DIR + "/item.asmx"; //$NON-NLS-1$
    public final static String PROXY_DOWNLOAD_FILE_2008 = PROXY_BASE_DIR + "/V1.0/item.asmx"; //$NON-NLS-1$
    public final static String PROXY_REPOSITORY_ID_QUERY_STRING = "rid"; //$NON-NLS-1$

    /**
     * Maximum comment size in characters.
     */
    public final static int MAX_COMMENT_SIZE = 2147483647;

    /**
     * Maximum computer name in characters.
     */
    public final static int MAX_COMPUTER_NAME_SIZE = 31;

    /**
     * Maximum length of a workspace name in characters.
     */
    public final static int MAX_WORKSPACE_NAME_SIZE = 64;

    /**
     * Maximum length of a change set name in characters.
     */
    public final static int MAX_CHANGE_SET_NAME_SIZE = 64;

    /**
     * Maximum length of a repository path before TFS_2012_QU1 in characters.
     * Includes $/ at beginning.
     */
    public final static int MAX_SERVER_PATH_SIZE_OLD = 259;

    /**
     * Maximum length of a repository path in characters since TFS2012_QU_2.
     * Includes $/ at beginning.
     */
    public final static int MAX_SERVER_PATH_SIZE = 399;

    /**
     * The maximum length in characters of any single path component in a server
     * path.
     */
    public final static int MAX_SERVER_PATH_COMPONENT_SIZE = 256;

    /**
     * Maximum length of a local path including drive letter and separators
     */
    public final static int MAX_LOCAL_PATH_SIZE = 259;

    /**
     * The maximum length in characters of any single path component in a local
     * path.
     */
    public final static int MAX_LOCAL_PATH_COMPONENT_SIZE = 256;

    /**
     * Maximum length of a user's display name. From unlen in lmcons.h
     */
    public final static int MAX_IDENTITY_NAME_SIZE = 256;

    /**
     * The maximum length of a label name in characters.
     */
    public final static int MAX_LABEL_NAME_SIZE = 64;

    /**
     * The maximum length of a check-in note name in characters.
     */
    public final static int CHECKIN_NOTE_NAME_SIZE = 64;

    /**
     * Maximum rows to be returned by a history query.
     */
    public final static int MAX_HISTORY_RESULTS = 2147483647;

    /**
     * Maximum number of merges to be returned when querying for historic
     * merges.
     */
    public final static int MAX_MERGES_RESULTS = 2147483647;

    /**
     * Maximum number of get results to process at one time.
     */
    public final static int MAX_GET_RESULTS = 250000;

    public final static int DESTROYED_FILE_ID = 1023;

    /**
     * If present in a changeset comment, the build service will not trigger
     * continuous integration builds for that changeset
     */
    public final static String NO_CI_CHECKIN_COMMENT = "***NO_CI***"; //$NON-NLS-1$

    /**
     * When an item is in an indeterminate state, the changeset for the version
     * in the workspace will be represented using this number.
     */
    public final static int INDETERMINATE_CHANGESET = -1;

    // Protocol strings (from Visual Studio's implementation).

    // Strings representing query string elements for the download file
    // HTTP GET request
    public final static String CHANGESET_QUERY_STRING = "cs"; //$NON-NLS-1$
    public final static String ITEM_ID_QUERY_STRING = "itemid"; //$NON-NLS-1$
    public final static String PENDING_CHANGE_ID_QUERY_STRING = "pcid"; //$NON-NLS-1$

    // Uploading files is done using MIME form-encoding. These are the
    // MIME field names for each section.
    public final static String SERVER_ITEM_FIELD = "item"; //$NON-NLS-1$
    public final static String WORKSPACE_NAME_FIELD = "wsname"; //$NON-NLS-1$
    public final static String WORKSPACE_OWNER_FIELD = "wsowner"; //$NON-NLS-1$
    public final static String HASH_FIELD = "hash"; //$NON-NLS-1$
    public final static String CONTENT_FIELD = "content"; //$NON-NLS-1$
    public final static String LENGTH_FIELD = "filelength"; //$NON-NLS-1$
    public final static String RANGE_FIELD = "range"; //$NON-NLS-1$

    // Reserved annotation names.
    public final static String CHECKIN_POLICIES_ANNOTATION = "CheckinPolicies"; //$NON-NLS-1$
    public final static String EXCLUSIVE_CHECKOUT_ANNOTATION = "ExclusiveCheckout"; //$NON-NLS-1$
    public final static String GET_LATEST_ON_CHECKOUT_ANNOTATION = "GetLatestOnCheckout"; //$NON-NLS-1$

    /**
     * This string means the currently authenticated user when used as a
     * username/owner parameter.
     */
    public final static String AUTHENTICATED_USER = "."; //$NON-NLS-1$

    // List of all global permissions.
    private static final String[] GLOBAL_PERMISSIONS = new String[] {
        "UseSystem", //$NON-NLS-1$
        "CreateWorkspace", //$NON-NLS-1$
        "AdminWorkspaces", //$NON-NLS-1$
        "AdminShelvesets", //$NON-NLS-1$
        "AdminConnections", //$NON-NLS-1$
        "AdminConfiguration" //$NON-NLS-1$
    };

    /*
     * Encodings understood by TFS. These are negative integers because positive
     * integers are used to express the code page that covers the file's
     * encoding.
     */
    public final static int ENCODING_BINARY = -1;
    public final static int ENCODING_UNCHANGED = -2;
    /**
     * Only meaningful on the server and on the client for local workspace
     * servicing.
     */
    public final static int ENCODING_FOLDER = -3;

    // Failure codes from checking pending checkins.
    public final static String LOCAL_ITEM_OUT_OF_DATE_EXCEPTION = "LocalItemOutOfDateException"; //$NON-NLS-1$
    public final static String MERGE_CONFLICT_EXISTS_EXCEPTION = "MergeConflictExistsException"; //$NON-NLS-1$
    public final static String ITEM_EXISTS_EXCEPTION = "ItemExistsException"; //$NON-NLS-1$
    public final static String ITEM_DELETED_EXCEPTION = "ItemDeletedException"; //$NON-NLS-1$
    public final static String LATEST_VERSION_DELETED_EXCEPTION = "LatestVersionDeletedException"; //$NON-NLS-1$
    public final static String ITEM_NOT_CHECKED_OUT_EXCEPTION = "ItemNotCheckedOutException"; //$NON-NLS-1$

    /**
     * Get the array of global permissions keywords. This method exists so the
     * actual array can be kept private (and thus immutable). Normal public
     * static final arrays can have their contents modified.
     *
     * @return a new array containing the global permissions keywords.
     */
    public static final String[] globalPermissions() {
        return GLOBAL_PERMISSIONS.clone();
    }

    /**
     * The artifact type of changesets.
     */
    public static final String CHANGESET_ARTIFACT_TYPE = "Changeset"; //$NON-NLS-1$

    /**
     * The artifact type of versioned items
     */
    public static final String VERSIONED_ITEM_ARTIFACT_TYPE = "VersionedItem"; //$NON-NLS-1$

    /**
     * The artifact type of latest items
     */
    public static final String LATEST_ITEM_ARTIFACT_TYPE = "LatestItemVersion"; //$NON-NLS-1$

    /**
     * The artifact type of storyboard
     */
    public static final String STORYBOARD_ARTIFACT_TYPE = "Storyboard"; //$NON-NLS-1$
}
