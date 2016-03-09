// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.printers;

import java.text.DateFormat;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMerge;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangesetMergeDetails;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemMerge;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * Prints merge details.
 */
public final class MergePrinter {
    static class SourceDetailItem {
        private final String serverPath;

        private int startChangeset;
        private int endChangeset;

        public SourceDetailItem(final String path, final int changeset) {
            serverPath = path;
            startChangeset = changeset;
            endChangeset = changeset;
        }

        public void addChangeset(final int changeset) {
            if (changeset < startChangeset) {
                startChangeset = changeset;
            } else if (changeset > endChangeset) {
                endChangeset = changeset;
            }
        }

        @Override
        public String toString() {
            if (startChangeset == endChangeset) {
                return VersionedFileSpec.formatForPath(serverPath, new ChangesetVersionSpec(startChangeset)).toString();
            }

            return VersionedFileSpec.formatForPath(
                serverPath,
                new ChangesetVersionSpec(startChangeset),
                new ChangesetVersionSpec(endChangeset)).toString();
        }
    }

    static class TargetDetailItem implements Comparable {
        private final String serverPath;

        private final int changesetID;

        public TargetDetailItem(final String serverPath, final int changeset) {
            this.serverPath = serverPath;
            changesetID = changeset;
        }

        @Override
        public int hashCode() {
            return serverPath.hashCode() + changesetID;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) {
                return true;
            }

            if (o == null) {
                return false;
            }

            if (o instanceof TargetDetailItem) {
                return false;
            }

            return changesetID == ((TargetDetailItem) o).changesetID
                && ServerPath.equals(serverPath, ((TargetDetailItem) o).serverPath);
        }

        @Override
        public int compareTo(final Object o) {
            final int ret = String.CASE_INSENSITIVE_ORDER.compare(serverPath, ((TargetDetailItem) o).serverPath);

            if (ret == 0) {
                return changesetID - ((TargetDetailItem) o).changesetID;
            }

            return ret;
        }

        @Override
        public String toString() {
            return VersionedFileSpec.formatForPath(serverPath, new ChangesetVersionSpec(changesetID)).toString();
        }
    }

    public static int printBriefMerges(
        final ChangesetMerge[] merges,
        final DateFormat dateFormat,
        final Display display) {
        Check.notNull(merges, "merges"); //$NON-NLS-1$
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(display.getWidth());

        table.setColumns(new Column[] {
            new Column(Messages.getString("MergePrinter.Changeset"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("MergePrinter.MergedInChangeset"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("MergePrinter.Author"), Sizing.TIGHT), //$NON-NLS-1$
            new Column(Messages.getString("MergePrinter.Date"), Sizing.TIGHT) //$NON-NLS-1$
        });

        for (int i = 0; i < merges.length; i++) {
            final ChangesetMerge merge = merges[i];
            Check.notNull(merge, "merge"); //$NON-NLS-1$

            final String sourceVersionString = merge.getSourceVersion() + (merge.isPartial() ? "*" : " "); //$NON-NLS-1$ //$NON-NLS-2$

            table.addRow(new String[] {
                sourceVersionString,
                Integer.toString(merge.getTargetVersion()),
                merge.getTargetChangeset().getOwnerDisplayName(),
                dateFormat.format(merge.getTargetChangeset().getDate().getTime())
            });
        }

        table.print(display.getPrintStream());

        return merges.length;
    }

    public static int printDetailedMerges(
        final ChangesetMergeDetails merges,
        final DateFormat dateFormat,
        final Display display) {
        Check.notNull(merges, "merges"); //$NON-NLS-1$
        Check.notNull(dateFormat, "dateFormat"); //$NON-NLS-1$
        Check.notNull(display, "display"); //$NON-NLS-1$

        merges.getChangesets();
        final ItemMerge[] mergedItems = merges.getMergedItems();
        final ItemMerge[] unmergedItems = merges.getUnmergedItems();

        if (mergedItems.length > 0) {
            display.printLine(Messages.getString("MergePrinter.MergedItemsColon")); //$NON-NLS-1$
        }

        /*
         * We use a sorted map to prune and store our details items, with merge
         * version ranges correctly expanded for the destination items they
         * correspond to.
         */

        final SortedMap targetItemToSourceItemMap = new TreeMap(new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                /*
                 * We're comparing TargetDetailItems, which are Comparable.
                 */
                return ((Comparable) o1).compareTo(o2);
            }
        });

        for (int i = 0; i < mergedItems.length; i++) {
            final ItemMerge item = mergedItems[i];

            /*
             * The key is the target item.
             */
            final TargetDetailItem targetKey =
                new TargetDetailItem(item.getTargetServerItem(), item.getTargetVersionFrom());

            /*
             * If the merged item is not in the map, add it, otherwise update it
             * with the new source version.
             */

            final SourceDetailItem value = (SourceDetailItem) targetItemToSourceItemMap.get(targetKey);
            if (value == null) {
                targetItemToSourceItemMap.put(
                    targetKey,
                    new SourceDetailItem(item.getSourceServerItem(), item.getSourceVersionFrom()));
            } else {
                value.addChangeset(item.getSourceVersionFrom());
            }
        }

        for (final Iterator i = targetItemToSourceItemMap.entrySet().iterator(); i.hasNext();) {
            final Entry entry = (Entry) i.next();

            display.printLine((entry.getValue() + " -> " + (entry.getKey()))); //$NON-NLS-1$
        }

        /*
         * Now handle the unmerged items, which are flat and easy.
         */

        if (unmergedItems.length > 0) {
            display.printLine(Messages.getString("MergePrinter.UnMergedItemsColon")); //$NON-NLS-1$
        }

        for (int i = 0; i < unmergedItems.length; i++) {
            display.printLine(
                VersionedFileSpec.formatForPath(
                    unmergedItems[i].getSourceServerItem(),
                    new ChangesetVersionSpec(unmergedItems[i].getSourceVersionFrom())));
        }

        return mergedItems.length + unmergedItems.length;
    }

}
