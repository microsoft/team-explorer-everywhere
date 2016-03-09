// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.filter;

/**
 * <p>
 * {@link ResourceFilterResult} represents the result of an
 * {@link ResourceFilter} operation. An {@link ResourceFilter} returns a
 * {@link ResourceFilterResult} to indicate the status of a specified resource
 * according to that filter.
 * </p>
 *
 * <p>
 * A {@link ResourceFilterResult} represents either the acceptance or rejection
 * of a resource by a filter. In addition, a {@link ResourceFilterResult} may
 * represent the rejection or acceptance of the resource's descendants by the
 * filter. Alternatively, a {@link ResourceFilterResult} may say nothing about
 * the children of a resource.
 * </p>
 *
 * <p>
 * {@link ResourceFilterResult} instances can't be created. Either use one of
 * the static fields, or call the {@link #getInstance(int)} method with
 * appropriate flags. {@link ResourceFilterResult} instances are singleton-style
 * - using <code>==</code> to compare two {@link ResourceFilterResult} instances
 * for equality is allowed and preferred.
 * </p>
 */
public final class ResourceFilterResult {
    private static final ResourceFilterResult[] singletons = new ResourceFilterResult[6];

    /**
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>accepted</b> by a filter. This result does not imply either acceptance
     * or rejection of the resource's children by the filter.
     */
    public static final ResourceFilterResult ACCEPT = new ResourceFilterResult(ResourceFilter.RESULT_FLAG_ACCEPT);

    /**
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>rejected</b> by a filter. This result does not imply either acceptance
     * or rejection of the resource's children by the filter.
     */
    public static final ResourceFilterResult REJECT = new ResourceFilterResult(ResourceFilter.RESULT_FLAG_REJECT);

    /**
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>accepted</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>accepted</b> by the same filter.
     */
    public static final ResourceFilterResult ACCEPT_AND_ACCEPT_CHILDREN =
        new ResourceFilterResult(ResourceFilter.RESULT_FLAG_ACCEPT | ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN);

    /**
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>accepted</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>rejected</b> by the same filter.
     */
    public static final ResourceFilterResult ACCEPT_AND_REJECT_CHILDREN =
        new ResourceFilterResult(ResourceFilter.RESULT_FLAG_ACCEPT | ResourceFilter.RESULT_FLAG_REJECT_CHILDREN);

    /**
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>rejected</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>accepted</b> by the same filter.
     */
    public static final ResourceFilterResult REJECT_AND_ACCEPT_CHILDREN =
        new ResourceFilterResult(ResourceFilter.RESULT_FLAG_REJECT | ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN);

    /**
     * A {@link ResourceFilterResult} which indicates that a resource is
     * <b>rejected</b> by a filter. This result also indicates that all of the
     * resource's children will be <b>rejected</b> by the same filter.
     */
    public static final ResourceFilterResult REJECT_AND_REJECT_CHILDREN =
        new ResourceFilterResult(ResourceFilter.RESULT_FLAG_REJECT | ResourceFilter.RESULT_FLAG_REJECT_CHILDREN);

    /**
     * <p>
     * As an alternative to the static fields on {@link ResourceFilterResult},
     * this method can be used to obtain an instance of
     * {@link ResourceFilterResult} given some flags. The flags that are
     * supported for this method are defined on the {@link ResourceFilter}
     * interface using <code>FLAG_</code> prefixes. The flags defined there are
     * bitwise ORed together and passed to this method.
     * </p>
     *
     * <p>
     * The following rules must be observed when passing flags to this method:
     * <ul>
     * <li>Either {@link ResourceFilter#RESULT_FLAG_ACCEPT} or
     * {@link ResourceFilter#RESULT_FLAG_REJECT} must be specified</li>
     * <li>{@link ResourceFilter#RESULT_FLAG_ACCEPT} and
     * {@link ResourceFilter#RESULT_FLAG_REJECT} are mutually exclusive</li>
     * <li>All other flags are optional</li>
     * <li>{@link ResourceFilter#RESULT_FLAG_ACCEPT_CHILDREN} and
     * {@link ResourceFilter#RESULT_FLAG_REJECT_CHILDREN} are mutually exclusive
     * </li>
     * </ul>
     *
     * Any flag combination passed to this method that does not satisfy the
     * above rules will result in an {@link IllegalArgumentException} being
     * thrown.
     * </p>
     *
     * @param flags
     *        the flags used to get a {@link ResourceFilterResult} instance
     * @return a {@link ResourceFilterResult} instance (never <code>null</code>)
     */
    public static final ResourceFilterResult getInstance(final int flags) {
        return singletons[getSingletonIndex(flags)];
    }

    private static final int NONCHILD_MASK = 0x3;
    private static final int CHILD_MASK = 0xC;
    private static final int MASK = NONCHILD_MASK | CHILD_MASK;

    private static final int getSingletonIndex(final int flags) {
        switch (flags) {
            case ResourceFilter.RESULT_FLAG_ACCEPT:
                return 0;
            case ResourceFilter.RESULT_FLAG_REJECT:
                return 1;
            case ResourceFilter.RESULT_FLAG_ACCEPT | ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN:
                return 2;
            case ResourceFilter.RESULT_FLAG_ACCEPT | ResourceFilter.RESULT_FLAG_REJECT_CHILDREN:
                return 3;
            case ResourceFilter.RESULT_FLAG_REJECT | ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN:
                return 4;
            case ResourceFilter.RESULT_FLAG_REJECT | ResourceFilter.RESULT_FLAG_REJECT_CHILDREN:
                return 5;
        }

        throw new IllegalArgumentException("illegal flags: " + flags); //$NON-NLS-1$
    }

    private final int flags;

    private ResourceFilterResult(final int flags) {
        this.flags = flags;
        singletons[getSingletonIndex(flags)] = this;
    }

    /**
     * @return <code>true</code> if this {@link ResourceFilterResult} indicates
     *         that the resource was accepted by the filter
     */
    public boolean isAccept() {
        return (flags & ResourceFilter.RESULT_FLAG_ACCEPT) != 0;
    }

    /**
     * @return <code>true</code> if this {@link ResourceFilterResult} indicates
     *         that the resource was rejected by the filter
     */
    public boolean isReject() {
        return (flags & ResourceFilter.RESULT_FLAG_REJECT) != 0;
    }

    /**
     * @return <code>true</code> if this {@link ResourceFilterResult} indicates
     *         that all of the resource's descendants would be accepted by the
     *         filter
     */
    public boolean isAcceptChildren() {
        return (flags & ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN) != 0;
    }

    /**
     * @return <code>true</code> if this {@link ResourceFilterResult} indicates
     *         that all of the resource's descendants would be rejected by the
     *         filter
     */
    public boolean isRejectChildren() {
        return (flags & ResourceFilter.RESULT_FLAG_REJECT_CHILDREN) != 0;
    }

    /**
     * @return a {@link ResourceFilterResult} that is the inverse of this one
     */
    public ResourceFilterResult getInverse() {
        if ((flags & CHILD_MASK) == 0) {
            return getInstance((~flags) & NONCHILD_MASK);
        } else {
            return getInstance(~flags & MASK);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();

        if ((flags & ResourceFilter.RESULT_FLAG_ACCEPT) != 0) {
            sb.append("ACCEPT"); //$NON-NLS-1$
        } else {
            sb.append("REJECT"); //$NON-NLS-1$
        }

        if ((flags & ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN) != 0) {
            sb.append(",ACCEPT_CHILDREN"); //$NON-NLS-1$
        } else if ((flags & ResourceFilter.RESULT_FLAG_REJECT_CHILDREN) != 0) {
            sb.append(",REJECT_CHILDREN"); //$NON-NLS-1$
        }

        return sb.toString();
    }
}
