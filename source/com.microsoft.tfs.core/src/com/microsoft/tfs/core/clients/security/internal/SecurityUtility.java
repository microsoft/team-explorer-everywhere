// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.security.internal;

public class SecurityUtility {
    public static class MergePermissionsResult {
        public final int updatedAllow;
        public final int updatedDeny;

        public MergePermissionsResult(final int updatedAllow, final int updatedDeny) {
            this.updatedAllow = updatedAllow;
            this.updatedDeny = updatedDeny;
        }
    }

    /**
     * <p>
     * Merges existing permissions with new permissions. Precedence is taken in
     * the following order: Denies, Allows, Removes. If the same bit is set to 1
     * in both the allow and deny permissions, the deny will outweigh the allow.
     * </p>
     *
     * @param existingAllow
     *        The existing allowed permissions.
     * @param existingDeny
     *        The existing denied permissions.
     * @param allow
     *        The new allowed permissions.
     * @param deny
     *        The new denied permissions.
     * @param remove
     *        The permissions to remove.
     */
    public static MergePermissionsResult mergePermissions(
        final int existingAllow,
        final int existingDeny,
        final int newAllow,
        final int newDeny,
        final int remove) {
        final int updatedAllow = ((existingAllow & ~remove) | newAllow) & ~newDeny;
        final int updatedDeny = ((existingDeny & ~remove) | newDeny) & ~updatedAllow;

        return new MergePermissionsResult(updatedAllow, updatedDeny);
    }
}