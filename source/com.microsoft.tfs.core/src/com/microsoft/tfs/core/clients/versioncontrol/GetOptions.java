// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.util.BitField;

/**
 * Options which control how items are processed during get operations.
 *
 * @since TEE-SDK-10.1
 */
public final class GetOptions extends BitField {
    /**
     * No get options.
     */
    public final static GetOptions NONE = new GetOptions(0, "None"); //$NON-NLS-1$

    /**
     * Overwrite local files that happen to be writeable (except files checked
     * out to the user) with the versions from the server. Normally writeable
     * files that are not checked out are flagged as writeable conflicts.
     */
    public final static GetOptions OVERWRITE = new GetOptions(1, "Overwrite"); //$NON-NLS-1$

    /**
     * Ask the server to send all files, not just ones it thinks are out of
     * date.
     */
    public final static GetOptions GET_ALL = new GetOptions(2, "ForceGetAll"); //$NON-NLS-1$

    /**
     * Perform the get as normal, but do not actually download the files from
     * the server and do not update any data on disk. Local version updates are
     * <b>not</b> sent to the server. {@link GetStatus} objects are returned to
     * the caller as if all operations were actually processed, and events are
     * fired as if the files were actually downloaded.
     * <p>
     * The general rule of Preview is that no workspace information on the
     * server and no workspace information on disk is changed during its
     * operation. It is safe to perform gets with Preview enabled without
     * worrying about what state may change.
     * <p>
     * Some errors that would appear during a non-preview get will not occur
     * when Preview is enabled. For instance, file permissions problems on disk
     * won't be encountered when Preview is enabled and will not be reported.
     */
    public final static GetOptions PREVIEW = new GetOptions(4, "Preview"); //$NON-NLS-1$

    /**
     * <P>
     * Updates the database references of the local remapped branch for all
     * items where the content on the local disk is the same as the content you
     * are downloading from the version control branch.
     * </p>
     * <P>
     * This means that if re-mapping a local folder to a new server path then we
     * only download files that are different between the two paths. If talking
     * to an older TFS instance with this flag then it is ignored and all files
     * will be downloaded.
     * </p>
     *
     * @since TFS 2008 SP1
     */

    public final static GetOptions REMAP = new GetOptions(8, "Remap"); //$NON-NLS-1$
    /**
     * <P>
     * Used only on the client. Instructs the client not to try to auto resolve
     * conflicts
     * </p>
     *
     * @since TFS 2012
     */
    public final static GetOptions NO_AUTO_RESOLVE = new GetOptions(16, "NoAutoResolve"); //$NON-NLS-1$

    /**
     * Perform the get as normal, but do not actually download the files from
     * the server and do not update any data on disk. Local version updates
     * <b>are</b> sent to the server. {@link GetStatus} objects are returned to
     * the caller as if all operations were actually processed, and events are
     * fired as if the files were actually downloaded.
     * <p>
     * Unlike {@link #PREVIEW}, using this option makes changes to TFS workspace
     * information (pending changes and local versions).
     * <p>
     * This option is useful for clients who must participate in a larger
     * "managed resources" framework, like Eclipse, where it is better to let
     * the framework actually update the disk after the change is pended.
     * <p>
     * Some errors such as local disk permissions problems won't appear if this
     * option is enabled, and it is the responsibility of the code pending the
     * change to recover (undo the change, synthesize the disk change, etc.).
     * <p>
     *
     * @warning this is a TEE client specific feature - therefore it should be
     *          removed from the flags before options are sent to the server.
     */
    public final static GetOptions NO_DISK_UPDATE = new GetOptions(256, "NoDiskUpdate"); //$NON-NLS-1$

    public static GetOptions combine(final GetOptions[] changeTypes) {
        return new GetOptions(BitField.combine(changeTypes));
    }

    private GetOptions(final int flags, final String name) {
        super(flags);
        registerStringValue(getClass(), flags, name);
    }

    private GetOptions(final int flags) {
        super(flags);
    }

    public boolean containsAll(final GetOptions other) {
        return containsAllInternal(other);
    }

    public boolean contains(final GetOptions other) {
        return containsInternal(other);
    }

    public boolean containsAny(final GetOptions other) {
        return containsAnyInternal(other);
    }

    public GetOptions remove(final GetOptions other) {
        return new GetOptions(removeInternal(other));
    }

    public GetOptions retain(final GetOptions other) {
        return new GetOptions(retainInternal(other));
    }

    public GetOptions combine(final GetOptions other) {
        return new GetOptions(combineInternal(other));
    }
}
