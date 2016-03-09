// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._Mapping;
import ms.tfs.versioncontrol.clientservices._03._RecursionType;
import ms.tfs.versioncontrol.clientservices._03._WorkingFolder;
import ms.tfs.versioncontrol.clientservices._03._WorkingFolderType;

/**
 * <p>
 * Represents a generic mapping of a server item (by path) to something, though
 * that something is not defined here (see {@link WorkingFolder}).
 * </p>
 * <p>
 * Derived classes must implement getWebServiceObject().
 * </p>
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public abstract class Mapping extends WebServiceObjectWrapper {
    /**
     * A working folder with {@link #DEPTH_ONE_LEVEL} affects only the files
     * directly inside of the mapped path. Subdirectories are not part of the
     * mapping.
     *
     * @since TFS 2008
     */
    private static final int DEPTH_ONE_LEVEL = 1;

    /**
     * A working folder with {@link #DEPTH_FULL} is a fully recursive mapping.
     * All files in the mapped folder and all files in subfolders are affected
     * by the mapping.
     */
    private static final int DEPTH_FULL = 120;

    public Mapping() {
        super(new _Mapping());
    }

    public Mapping(final _Mapping mapping) {
        super(mapping);
    }

    /**
     * Creates a {@link Mapping} tfor the given server item.
     *
     * @param serverItem
     *        the server item being mapped (must not be <code>null</code>)
     * @param type
     *        the type of mapping to create (must not be <code>null</code>)
     * @param recursion
     *        the type of recursion to use for the working folder (must not be
     *        <code>null</code>)
     */
    public Mapping(final String serverItem, final WorkingFolderType type, final RecursionType recursion) {
        super(new _Mapping(serverItem, type.getWebServiceObject(), getDepthFromRecursion(recursion)));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * This method is called "getBaseWebServiceObject" so derived classes can
     * use the proper method name for their type.
     *
     * @return the web service object this class wraps.
     */
    private _Mapping getWebServiceObject() {
        return (_Mapping) webServiceObject;
    }

    /**
     * Converts a {@link _RecursionType} into a numeric depth value for use
     * inside the {@link _WorkingFolder} class.
     *
     * @param recursion
     *        the recursion type to convert (must not be <code>null</code>)
     * @return one of {@link #DEPTH_FULL} or {@link #DEPTH_ONE_LEVEL} that
     *         corresponds to the given recursion type.
     */
    protected static int getDepthFromRecursion(final RecursionType recursion) {
        Check.notNull(recursion, "recursion"); //$NON-NLS-1$

        if (recursion == RecursionType.FULL) {
            return DEPTH_FULL;
        }

        if (recursion == RecursionType.ONE_LEVEL) {
            return DEPTH_ONE_LEVEL;
        }

        /*
         * Default (traditional mapping). Returning DEPTH_NONE would produce a
         * WorkingFolder that would be invalid when given to the server.
         */
        return DEPTH_FULL;
    }

    /**
     * Gets the path to the server item in this mapping.
     *
     * @return the server path.
     */
    public String getServerItem() {
        return getWebServiceObject().getItem();
    }

    /**
     * Sets the path to the server item in this working folder mapping. Unused
     * if this mapping type is Cloak.
     *
     * @param item
     *        the server path (must not be <code>null</code>)
     */
    public void setServerItem(final String item) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        getWebServiceObject().setItem(item);
    }

    /**
     * <p>
     * Gets the path to the server item in this mapping in a format that may
     * differ from the canonical server item (returned by
     * {@link #getServerItem()} in order to communicate more information to the
     * user. For example, one-level working folder mappings (see
     * {@link #DEPTH_ONE_LEVEL}) will have "/*" appended to the path.
     * </p>
     * <p>
     * Do not compare or sort {@link Mapping}s on this field because it might
     * change for locale-reasons.
     * </p>
     *
     * @return the server path, perhaps modified for display.
     */
    public String getDisplayServerItem() {
        if (getWebServiceObject().getDepth() == DEPTH_ONE_LEVEL) {
            return getWebServiceObject().getItem() + "/*"; //$NON-NLS-1$
        }

        return getWebServiceObject().getItem();
    }

    /**
     * Gets the type of working folder mapping.
     *
     * @return the type of working folder mapping.
     */
    public WorkingFolderType getType() {
        return WorkingFolderType.fromWebServiceObject(getWebServiceObject().getType());
    }

    /**
     * @return the recursion style of the mapping.
     */
    public RecursionType getDepth() {
        /*
         * Visual Studio's OM never returns "none" recursion types, so we do the
         * same.
         */

        if (getWebServiceObject().getDepth() == DEPTH_ONE_LEVEL) {
            return RecursionType.ONE_LEVEL;
        }

        return RecursionType.FULL;
    }

    /**
     * @return true if this mapping is a cloak mapping, false if it is not.
     */
    public boolean isCloaked() {
        return (getWebServiceObject().getType().equals(_WorkingFolderType.Cloak));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Mapping == false) {
            return false;
        }
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        final Mapping other = (Mapping) obj;

        /*
         * This logic matches the Visual Studio OM as of TFS 2010 Beta 1.
         */

        return (ServerPath.equals(getServerItem(), other.getServerItem())
            && getDepth().equals(other.getDepth())
            && getType().equals(other.getType()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        /*
         * Visual Studio hashes WorkingFolder objects only on the server path
         * component.
         */
        result =
            result * 37 + ((getWebServiceObject().getItem() == null) ? 0 : getWebServiceObject().getItem().hashCode());

        return result;
    }
}
