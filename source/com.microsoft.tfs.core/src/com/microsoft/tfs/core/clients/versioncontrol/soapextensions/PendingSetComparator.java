// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Comparator;

import com.microsoft.tfs.core.util.TFSUser;
import com.microsoft.tfs.core.util.TFSUsernameParseException;

/**
 * Compare engine for {@link PendingSet} items.
 *
 * @since TEE-SDK-10.1
 */
public final class PendingSetComparator implements Comparator<PendingSet> {
    public PendingSetComparator() {
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare(final PendingSet first, final PendingSet second) {
        /*
         * This method looks complicated, but it's really straight-forward (just
         * a little verbose to cover all the cases).
         */

        int res = 0;

        // Compare the usernames.
        if (first.getOwnerName() != null && second.getOwnerName() != null) {
            /*
             * Somewhat arbitrarily, if we encounter an error parsing the first
             * username, we consider the first item to be less than the second;
             */

            TFSUser firstOwner;
            try {
                firstOwner = new TFSUser(first.getOwnerName());
            } catch (final TFSUsernameParseException e) {
                return -1;
            }

            TFSUser secondOwner;
            try {
                secondOwner = new TFSUser(second.getOwnerName());
            } catch (final TFSUsernameParseException e) {
                return 1;
            }

            res = firstOwner.compareTo(secondOwner);
            if (res != 0) {
                return res;
            }
        } else if (first.getOwnerName() == null && second.getOwnerName() != null) {
            /*
             * If the first owner is null but the second is not, the first item
             * is less than the second.
             */
            return -1;
        } else if (first.getOwnerName() != null && second.getOwnerName() == null) {
            /*
             * If the first owner is not null but the second is, the first item
             * is greater than the second.
             */
            return 1;
        }

        // Compare the computers.
        if (first.getComputer() != null && second.getComputer() != null) {
            res = first.getComputer().compareToIgnoreCase(second.getComputer());
            if (res != 0) {
                return res;
            }
        } else if (first.getComputer() == null && second.getComputer() != null) {
            /*
             * If the first computer is null but the second is not, the first
             * item is less than the second.
             */
            return -1;
        } else if (first.getComputer() != null && second.getComputer() == null) {
            /*
             * If the first computer is not null but the second is, the first
             * item is greater than the second.
             */
            return 1;
        }

        // Compare workspaces.
        if (first.getName() != null && second.getName() != null) {
            res = first.getComputer().compareToIgnoreCase(second.getComputer());
            if (res != 0) {
                return res;
            }
        } else if (first.getComputer() == null && second.getComputer() != null) {
            /*
             * If the first computer is null but the second is not, the first
             * item is less than the second.
             */
            return -1;
        } else if (first.getComputer() != null && second.getComputer() == null) {
            /*
             * If the first computer is not null but the second is, the first
             * item is greater than the second.
             */
            return 1;
        }

        return 0;
    }
}
