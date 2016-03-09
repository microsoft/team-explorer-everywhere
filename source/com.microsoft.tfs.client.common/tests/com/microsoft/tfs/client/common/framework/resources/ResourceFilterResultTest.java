// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources;

import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;

import junit.framework.TestCase;

public class ResourceFilterResultTest extends TestCase {
    public void testSingletonInstances() {
        assertTrue(ResourceFilterResult.ACCEPT == ResourceFilterResult.getInstance(ResourceFilter.RESULT_FLAG_ACCEPT));

        assertTrue(ResourceFilterResult.REJECT == ResourceFilterResult.getInstance(ResourceFilter.RESULT_FLAG_REJECT));

        assertTrue(
            ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN == ResourceFilterResult.getInstance(
                ResourceFilter.RESULT_FLAG_ACCEPT | ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN));

        assertTrue(
            ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN == ResourceFilterResult.getInstance(
                ResourceFilter.RESULT_FLAG_ACCEPT | ResourceFilter.RESULT_FLAG_REJECT_CHILDREN));

        assertTrue(
            ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN == ResourceFilterResult.getInstance(
                ResourceFilter.RESULT_FLAG_REJECT | ResourceFilter.RESULT_FLAG_ACCEPT_CHILDREN));

        assertTrue(
            ResourceFilterResult.REJECT_AND_REJECT_CHILDREN == ResourceFilterResult.getInstance(
                ResourceFilter.RESULT_FLAG_REJECT | ResourceFilter.RESULT_FLAG_REJECT_CHILDREN));
    }

    public void testGetInverse() {
        assertTrue(ResourceFilterResult.REJECT == ResourceFilterResult.ACCEPT.getInverse());

        assertTrue(ResourceFilterResult.ACCEPT == ResourceFilterResult.REJECT.getInverse());

        assertTrue(
            ResourceFilterResult.REJECT_AND_REJECT_CHILDREN == ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN.getInverse());

        assertTrue(
            ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN == ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN.getInverse());

        assertTrue(
            ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN == ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN.getInverse());

        assertTrue(
            ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN == ResourceFilterResult.REJECT_AND_REJECT_CHILDREN.getInverse());
    }

    public void testBadFlags() {
        try {
            ResourceFilterResult.getInstance(ResourceFilter.RESULT_FLAG_ACCEPT | ResourceFilter.RESULT_FLAG_REJECT);
            fail();
        } catch (final IllegalArgumentException ex) {

        }

        try {
            ResourceFilterResult.getInstance(ResourceFilter.RESULT_FLAG_NONE);
            fail();
        } catch (final IllegalArgumentException ex) {

        }
    }

    public void testIsAccept() {
        assertTrue(ResourceFilterResult.ACCEPT.isAccept());
        assertFalse(ResourceFilterResult.REJECT.isAccept());
        assertTrue(ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN.isAccept());
        assertTrue(ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN.isAccept());
        assertFalse(ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN.isAccept());
        assertFalse(ResourceFilterResult.REJECT_AND_REJECT_CHILDREN.isAccept());
    }

    public void testIsReject() {
        assertFalse(ResourceFilterResult.ACCEPT.isReject());
        assertTrue(ResourceFilterResult.REJECT.isReject());
        assertFalse(ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN.isReject());
        assertFalse(ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN.isReject());
        assertTrue(ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN.isReject());
        assertTrue(ResourceFilterResult.REJECT_AND_REJECT_CHILDREN.isReject());
    }

    public void testIsAcceptChildren() {
        assertFalse(ResourceFilterResult.ACCEPT.isAcceptChildren());
        assertFalse(ResourceFilterResult.REJECT.isAcceptChildren());
        assertTrue(ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN.isAcceptChildren());
        assertFalse(ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN.isAcceptChildren());
        assertTrue(ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN.isAcceptChildren());
        assertFalse(ResourceFilterResult.REJECT_AND_REJECT_CHILDREN.isAcceptChildren());
    }

    public void testIsRejectChildren() {
        assertFalse(ResourceFilterResult.ACCEPT.isRejectChildren());
        assertFalse(ResourceFilterResult.REJECT.isRejectChildren());
        assertFalse(ResourceFilterResult.ACCEPT_AND_ACCEPT_CHILDREN.isRejectChildren());
        assertTrue(ResourceFilterResult.ACCEPT_AND_REJECT_CHILDREN.isRejectChildren());
        assertFalse(ResourceFilterResult.REJECT_AND_ACCEPT_CHILDREN.isRejectChildren());
        assertTrue(ResourceFilterResult.REJECT_AND_REJECT_CHILDREN.isRejectChildren());
    }
}
