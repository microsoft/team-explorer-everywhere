// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.LabelSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpecParseException;
import com.microsoft.tfs.util.Check;

/**
 *         A QualifiedItem is a local or server file path, a deletion ID (0 if
 *         not deleted) and an array of versions.
 *
 *         This class if thread-safe.
 */
public final class QualifiedItem {
    /**
     * Uses platform's native path conventions since this object is never passed
     * to the server.
     */
    private String _path;

    private int _deletionID;
    private VersionSpec[] _versions;

    public QualifiedItem(final String path, final int deletionID, final VersionSpec[] versions) {
        super();

        if (ServerPath.isServerPath(path)) {
            _path = path;
        } else {
            _path = LocalPath.canonicalize(path);
        }

        _deletionID = deletionID;
        _versions = versions;
    }

    /**
     * Parses a versioned file spec into this QualifiedItem.
     *
     * @param versionedFileSpec
     *        the versioned file spec to parse.
     * @param user
     *        the username to use when constructing specs that require one.
     * @param defaultVersion
     *        the version to use for this item if a version could not be parsed
     *        from the file spec. If null, getVersions() will return null.
     * @param allowVersionRange
     *        whether to allow ranges of versions for this item.
     */
    public QualifiedItem(
        final String versionedFileSpec,
        final String user,
        final VersionSpec defaultVersion,
        final boolean allowVersionRange) throws VersionSpecParseException, LabelSpecParseException {
        final VersionedFileSpec vfs = VersionedFileSpec.parse(versionedFileSpec, user, allowVersionRange);

        Check.notNull(vfs, "vfs"); //$NON-NLS-1$

        _path = vfs.getItem();
        _deletionID = (vfs.getDeletionVersionSpec() != null) ? vfs.getDeletionVersionSpec().getDeletionID() : 0;

        /*
         * If no versions were parsed, we want to use our fallback version.
         */
        if (vfs.getVersions().length == 0) {
            // Preserve the null.
            if (defaultVersion == null) {
                _versions = null;
            } else {
                _versions = new VersionSpec[] {
                    defaultVersion
                };
            }
        } else {
            _versions = vfs.getVersions();
        }
    }

    public String getPath() {
        return _path;
    }

    public int getDeletionID() {
        return _deletionID;
    }

    public VersionSpec[] getVersions() {
        return _versions;
    }

    public void setPath(final String path) {
        if (ServerPath.isServerPath(path)) {
            _path = path;
        } else {
            _path = LocalPath.canonicalize(path);
        }
    }

    public void setDeletionID(final int _deletionid) {
        _deletionID = _deletionid;
    }

    public void setVersions(final VersionSpec[] versions) {
        _versions = versions;
    }

    /**
     * Constructs an AGetRequest from this qualified item, using the supplied
     * recursion type.
     *
     * @param recursion
     *        the type of recursion to specify in the AGetRequest object.
     * @return a new AGetRequest object that describes this qualified item.
     * @throws ClassNotFoundException
     *         if this object's _versions structure contains an unsupported type
     *         of AVersionSpec class.
     */
    public GetRequest toGetRequest(final RecursionType recursion) throws ClassNotFoundException {
        Check.isTrue(_versions.length > 0 && _versions[0] != null, "_versions.length > 0 && _versions[0] != null"); //$NON-NLS-1$

        return new GetRequest(toItemSpec(recursion), _versions[0]);
    }

    /**
     * Constructs an {@link ItemSpec} from this qualified item, using the
     * supplied recursion type.
     *
     * @param recursion
     *        the type of recursion to include in the {@link ItemSpec} object
     * @return a new {@link ItemSpec} object that describes this qualified item
     */
    public ItemSpec toItemSpec(final RecursionType recursion) {
        return new ItemSpec(_path, recursion, _deletionID);
    }
}
