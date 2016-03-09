// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.queryhierarchy;

import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Options which affect the kinds of links returned for a query.
 *
 * @since TEE-SDK-10.1
 */
public final class LinkQueryMode extends TypesafeEnum {
    public static final LinkQueryMode UNKNOWN = new LinkQueryMode(0);

    /**
     * Specifies that links to work items will be returned.
     */
    public static final LinkQueryMode WORK_ITEMS = new LinkQueryMode(1);

    /**
     * Specifies that one-hop queries must contain the given link.
     */
    public static final LinkQueryMode LINKS_MUST_CONTAIN = new LinkQueryMode(2);

    /**
     * Specifies that one-hop queries will include all links.
     */
    public static final LinkQueryMode LINKS_MAY_CONTAIN = new LinkQueryMode(3);

    /**
     * Specifies that one-hop queries must not contain the given link.
     */
    public static final LinkQueryMode LINKS_DOES_NOT_CONTAIN = new LinkQueryMode(4);

    /**
     * Specifies that all links will be returned (recursively.)
     */
    public static final LinkQueryMode LINKS_RECURSIVE = new LinkQueryMode(5);

    private LinkQueryMode(final int value) {
        super(value);
    }
}
