// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

/**
 * <p>
 * An interface that can be implemented by any object which has a hierarchical
 * relationship with other objects.
 * </p>
 * <p>
 * The point of implementing this interface is to make the hierarchcial
 * relationship explicit and to aid the UI layer in building hierarchical
 * interfaces.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public interface Hierarchical {
    /**
     * @return the parent of this object, or null if this object has no parent
     */
    public Object getParent();

    /**
     * @return the children of this object, or null if this object has no
     *         children
     */
    public Object[] getChildren();

    /**
     * This method is included because it's often less expensive to check for
     * the existence of children than to retrieve the children.
     *
     * @return true if this object has children, false if not
     */
    public boolean hasChildren();
}
