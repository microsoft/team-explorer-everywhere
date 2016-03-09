// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.FailureCodes;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalPendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.SeverityType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;

public class ParsedItemSpec {
    private final String targetItem;
    private final RecursionType recursion;
    private final String pattern;
    private final ParsedItemSpecOptions options;

    private ParsedItemSpec(
        final String targetItem,
        final String targetItemPattern,
        final RecursionType recursion,
        final ParsedItemSpecOptions options) {
        Check.notNullOrEmpty(targetItem, "targetItem"); //$NON-NLS-1$

        this.targetItem = targetItem;
        this.pattern = targetItemPattern;
        this.recursion = recursion;
        this.options = options;
    }

    public boolean isServerItem() {
        return ServerPath.isServerPath(targetItem);
    }

    /**
     * Gets you back the original ItemSpec.Item that was used to create this
     * ParsedItemSpec.
     */
    private String getUnparsedItem() {
        if (isServerItem()) {
            return getPattern() == null ? getTargetItem() : ServerPath.combine(getTargetItem(), getPattern());
        } else {
            return getPattern() == null ? getTargetItem() : LocalPath.combine(getTargetItem(), getPattern());
        }
    }

    public boolean match(final String itemToMatch) {
        Check.notNullOrEmpty(itemToMatch, "itemToMatch"); //$NON-NLS-1$

        if (isServerItem()) {
            if (RecursionType.FULL == getRecursionType()) {
                return ServerPath.isChild(getTargetItem(), itemToMatch)
                    && (null == getPattern()
                        || ItemPath.matchesWildcardFile(ServerPath.getFileName(itemToMatch), getPattern()));
            } else if (RecursionType.ONE_LEVEL == getRecursionType()) {
                return (ServerPath.equals(itemToMatch, getTargetItem())
                    || ServerPath.isDirectChild(getTargetItem(), itemToMatch))
                    && (null == getPattern()
                        || ItemPath.matchesWildcardFile(ServerPath.getFileName(itemToMatch), getPattern()));
            } else if (RecursionType.NONE == getRecursionType()) {
                if (null != getPattern()) {
                    return ServerPath.isDirectChild(getTargetItem(), itemToMatch)
                        && ItemPath.matchesWildcardFile(ServerPath.getFileName(itemToMatch), getPattern());
                }
                return ServerPath.equals(itemToMatch, getTargetItem());
            }
        } else {
            if (RecursionType.FULL == getRecursionType()) {
                return LocalPath.isChild(getTargetItem(), itemToMatch)
                    && (null == getPattern()
                        || ItemPath.matchesWildcardFile(LocalPath.getFileName(itemToMatch), getPattern()));
            } else if (RecursionType.ONE_LEVEL == getRecursionType()) {
                return (LocalPath.equals(itemToMatch, getTargetItem())
                    || LocalPath.isDirectChild(getTargetItem(), itemToMatch))
                    && (null == getPattern()
                        || ItemPath.matchesWildcardFile(LocalPath.getFileName(itemToMatch), getPattern()));
            } else if (RecursionType.NONE == getRecursionType()) {
                if (null != getPattern()) {
                    return LocalPath.isDirectChild(getTargetItem(), itemToMatch)
                        && ItemPath.matchesWildcardFile(LocalPath.getFileName(itemToMatch), getPattern());
                }
                return LocalPath.equals(itemToMatch, getTargetItem());
            }
        }

        /* unreachable */
        return false;
    }

    public Iterable<WorkspaceLocalItem> expandRootsFrom(
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final List<Failure> failuresAccumulator) {
        final Map<String, WorkspaceLocalItem> expandedRootItems = new HashMap<String, WorkspaceLocalItem>();
        for (final WorkspaceLocalItem lvEntry : expandFrom(lv, pc, failuresAccumulator)) {
            final String targetServerItem = pc.getTargetServerItemForLocalVersion(lvEntry);
            List<String> itemsToRemove = null;
            boolean parentExist = false;
            for (final String expandedRoot : expandedRootItems.keySet()) {
                // For recursive operations, make sure that this item
                // is not the child of an item already in the list.
                if (ServerPath.isChild(expandedRoot, targetServerItem)) {
                    parentExist = true;
                    break;
                } else if (ServerPath.isChild(targetServerItem, expandedRoot)) {
                    // lazily initialize the list when needed.
                    if (itemsToRemove == null) {
                        itemsToRemove = new ArrayList<String>();
                    }

                    // if the item is a parent of an item already on the list
                    // remove it.
                    itemsToRemove.add(expandedRoot);
                }
            }
            if (itemsToRemove != null) {
                for (final String serverItem : itemsToRemove) {
                    expandedRootItems.remove(serverItem);
                }
            }
            if (!parentExist) {
                expandedRootItems.put(targetServerItem, lvEntry);
            }
        }
        return expandedRootItems.values();
    }

    public Iterable<WorkspaceLocalItem> expandFrom(
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final List<Failure> failuresAccumulator) {
        final AtomicReference<Failure> failureOut = new AtomicReference<Failure>();
        final Iterable<WorkspaceLocalItem> toReturn = expandFrom(lv, pc, failureOut);
        final Failure failure = failureOut.get();

        if (null != failure) {
            failuresAccumulator.add(failure);
        }

        return toReturn;
    }

    public Iterable<WorkspaceLocalItem> expandFrom(
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final AtomicReference<Failure> failureOut) {
        failureOut.set(null);

        if (!isServerItem()) {
            return expandFromLocal(lv, failureOut);
        } else {
            return expandFromServer(lv, pc, failureOut);
        }
    }

    private Iterable<WorkspaceLocalItem> expandFromLocal(
        final WorkspaceVersionTable lv,
        final AtomicReference<Failure> failureOut) {
        failureOut.set(null);
        WorkspaceLocalItem firstItem = null;

        final boolean includeDeleted = (options.contains(ParsedItemSpecOptions.INCLUDE_DELETED));
        final Iterable<WorkspaceLocalItem> lvEntries =
            lv.queryByLocalItem(getTargetItem(), getRecursionType(), getPattern(), includeDeleted);

        for (final WorkspaceLocalItem lvEntry : lvEntries) {
            firstItem = lvEntry;
            break;
        }

        if (null == firstItem) {
            if (RecursionType.FULL != getRecursionType()) {
                final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundFormat"); //$NON-NLS-1$

                failureOut.set(
                    new Failure(
                        MessageFormat.format(format, getUnparsedItem()),
                        FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                        SeverityType.ERROR,
                        getUnparsedItem()));
            } else {
                final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundRecursiveFormat"); //$NON-NLS-1$

                failureOut.set(
                    new Failure(
                        MessageFormat.format(format, getUnparsedItem()),
                        FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                        SeverityType.ERROR,
                        getUnparsedItem()));
            }

            return new ArrayList<WorkspaceLocalItem>(0);
        }

        return lv.queryByLocalItem(getTargetItem(), getRecursionType(), getPattern(), includeDeleted);
    }

    private Iterable<WorkspaceLocalItem> expandFromServer(
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final AtomicReference<Failure> failureOut) {
        failureOut.set(null);
        WorkspaceLocalItem firstItem = null;

        final Iterable<WorkspaceLocalItem> lvEntries =
            queryLocalVersionsByTargetServerItem(lv, pc, getTargetItem(), getRecursionType(), getPattern(), options);

        for (final WorkspaceLocalItem lvEntry : lvEntries) {
            firstItem = lvEntry;
            break;
        }

        if (null == firstItem) {
            if (RecursionType.FULL != getRecursionType()) {
                final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundFormat"); //$NON-NLS-1$

                failureOut.set(
                    new Failure(
                        MessageFormat.format(format, getUnparsedItem()),
                        FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                        SeverityType.ERROR,
                        getUnparsedItem()));
            } else {
                final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundRecursiveFormat"); //$NON-NLS-1$

                failureOut.set(
                    new Failure(
                        MessageFormat.format(format, getUnparsedItem()),
                        FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                        SeverityType.ERROR,
                        getUnparsedItem()));
            }

            return new ArrayList<WorkspaceLocalItem>(0);
        }

        return queryLocalVersionsByTargetServerItem(lv, pc, getTargetItem(), getRecursionType(), getPattern(), options);
    }

    public static ParsedItemSpec fromItemSpec(
        final ItemSpec itemSpec,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ParsedItemSpecOptions options,
        final List<Failure> failuresAccumulator) {
        final AtomicReference<Failure> failureOut = new AtomicReference<Failure>();
        final ParsedItemSpec toReturn = fromItemSpec(itemSpec, wp, lv, pc, options, failureOut);
        final Failure failure = failureOut.get();

        if (null != failure) {
            failuresAccumulator.add(failure);
        }

        return toReturn;
    }

    public static ParsedItemSpec fromItemSpec(
        final ItemSpec itemSpec,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ParsedItemSpecOptions options,
        final AtomicReference<Failure> failureOut) {
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$

        if (ServerPath.isServerPath(itemSpec.getItem())) {
            return fromServerItemSpec(itemSpec, wp, lv, pc, options, failureOut, false);
        } else {
            return fromLocalItemSpec(itemSpec, wp, lv, pc, options, failureOut, false);
        }
    }

    public static ParsedItemSpec fromLocalItemSpec(
        final ItemSpec itemSpec,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ParsedItemSpecOptions options,
        final List<Failure> failuresAccumulator) {
        final AtomicReference<Failure> failure = new AtomicReference<Failure>();
        final ParsedItemSpec toReturn = fromItemSpec(itemSpec, wp, lv, pc, options, failure);

        if (null != failure.get()) {
            failuresAccumulator.add(failure.get());
        }

        return toReturn;
    }

    public static ParsedItemSpec fromLocalItemSpec(
        final ItemSpec itemSpec,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ParsedItemSpecOptions options,
        final AtomicReference<Failure> failureOut,
        final boolean checkLocalDiskIfNotFound) {
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$

        ParsedItemSpec toReturn = null;
        RecursionType recursion = itemSpec.getRecursionType();
        failureOut.set(null);

        /* Get the file name, see if this is a wildcard. */
        final String pathPart = LocalPath.getParent(itemSpec.getItem());
        String patternPart = LocalPath.getFileName(itemSpec.getItem());

        if (0 == patternPart.length()) {
            patternPart = null;
        }

        if (null != patternPart && LocalPath.isWildcard(patternPart)) {
            if (RecursionType.NONE == recursion) {
                // Promote to OneLevel because the query has a pattern.
                recursion = RecursionType.ONE_LEVEL;
            }

            ExistenceCheckResult checkResult = localItemExists(wp, lv, pc, pathPart, options);

            if (checkResult == ExistenceCheckResult.DOES_NOT_EXIST && checkLocalDiskIfNotFound) {
                checkResult = localItemExistsOnDisk(pathPart);
            }

            if (ExistenceCheckResult.DOES_NOT_EXIST == checkResult) {
                final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundRecursiveFormat"); //$NON-NLS-1$
                failureOut.set(
                    new Failure(
                        MessageFormat.format(format, itemSpec.getItem()),
                        FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                        SeverityType.ERROR,
                        itemSpec.getItem()));
            } else
            /* Parent item of wildcard expression exists: path and pattern */
            {
                toReturn = new ParsedItemSpec(pathPart, patternPart, recursion, options);
            }
        } else {
            ExistenceCheckResult checkResult = localItemExists(wp, lv, pc, itemSpec.getItem(), options);

            if (checkResult == ExistenceCheckResult.DOES_NOT_EXIST && checkLocalDiskIfNotFound) {
                checkResult = localItemExistsOnDisk(itemSpec.getItem());
            }

            if (ExistenceCheckResult.DOES_NOT_EXIST == checkResult) {
                if (RecursionType.NONE == recursion) {
                    final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundFormat"); //$NON-NLS-1$

                    failureOut.set(
                        new Failure(
                            MessageFormat.format(format, itemSpec.getItem()),
                            FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                            SeverityType.ERROR,
                            itemSpec.getItem()));
                } else {
                    checkResult = localItemExists(wp, lv, pc, pathPart, options);

                    if (checkResult == ExistenceCheckResult.DOES_NOT_EXIST && checkLocalDiskIfNotFound) {
                        checkResult = localItemExistsOnDisk(pathPart);
                    }

                    if (ExistenceCheckResult.DOES_NOT_EXIST == checkResult) {
                        final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundRecursiveFormat"); //$NON-NLS-1$
                        failureOut.set(
                            new Failure(
                                MessageFormat.format(format, itemSpec.getItem()),
                                FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                                SeverityType.ERROR,
                                itemSpec.getItem()));
                    } else
                    /*
                     * Parent item of non-wildcard expression exists: path and
                     * pattern
                     */
                    {
                        toReturn = new ParsedItemSpec(pathPart, patternPart, recursion, options);
                    }
                }
            } else {
                // The full item exists as specified.
                if (RecursionType.FULL == recursion && ExistenceCheckResult.IS_FILE == checkResult) {
                    toReturn = new ParsedItemSpec(pathPart, patternPart, recursion, options);
                } else {
                    toReturn = new ParsedItemSpec(itemSpec.getItem(), null, recursion, options);
                }
            }
        }

        return toReturn;
    }

    public static ParsedItemSpec FromServerItemSpec(
        final ItemSpec itemSpec,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ParsedItemSpecOptions options,
        final List<Failure> failuresAccumulator) {
        final AtomicReference<Failure> failureOut = new AtomicReference<Failure>();
        final ParsedItemSpec toReturn = fromServerItemSpec(itemSpec, wp, lv, pc, options, failureOut, false);
        final Failure failure = failureOut.get();

        if (null != failure) {
            failuresAccumulator.add(failure);
        }

        return toReturn;
    }

    public static ParsedItemSpec fromServerItemSpec(
        final ItemSpec itemSpec,
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final ParsedItemSpecOptions options,
        final AtomicReference<Failure> failureOut,
        final boolean checkLocalDiskIfNotFound) {
        Check.notNull(itemSpec, "itemSpec"); //$NON-NLS-1$
        Check.notNull(pc, "pc"); //$NON-NLS-1$

        ParsedItemSpec toReturn = null;
        RecursionType recursion = itemSpec.getRecursionType();
        failureOut.set(null);

        final String pathPart = ServerPath.getParent(itemSpec.getItem());
        String patternPart = ServerPath.getFileName(itemSpec.getItem());

        if (0 == patternPart.length()) {
            patternPart = null;
        }

        if (null != patternPart && ItemPath.isWildcard(patternPart)) {
            if (RecursionType.NONE == recursion) {
                // Promote to OneLevel because the query has a pattern.
                recursion = RecursionType.ONE_LEVEL;
            }

            ExistenceCheckResult checkResult = targetServerItemExists(lv, pc, pathPart, options);

            if (checkResult == ExistenceCheckResult.DOES_NOT_EXIST && checkLocalDiskIfNotFound) {
                checkResult =
                    localItemExistsOnDisk(WorkingFolder.getLocalItemForServerItem(pathPart, wp.getWorkingFolders()));
            }

            // Does the path part exist?
            if (ExistenceCheckResult.DOES_NOT_EXIST == checkResult) {
                final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundRecursiveFormat"); //$NON-NLS-1$

                failureOut.set(
                    new Failure(
                        MessageFormat.format(format, itemSpec.getItem()),
                        FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                        SeverityType.ERROR,
                        itemSpec.getItem()));
            } else {
                /*
                 * Parent item of wildcard expression exists: path and pattern
                 */
                toReturn = new ParsedItemSpec(pathPart, patternPart, recursion, options);
            }
        } else {
            ExistenceCheckResult checkResult = targetServerItemExists(lv, pc, itemSpec.getItem(), options);

            if (checkResult == ExistenceCheckResult.DOES_NOT_EXIST && checkLocalDiskIfNotFound) {
                checkResult = localItemExistsOnDisk(
                    WorkingFolder.getLocalItemForServerItem(itemSpec.getItem(), wp.getWorkingFolders()));
            }

            // Does the full item exist?
            if (ExistenceCheckResult.DOES_NOT_EXIST == checkResult) {
                if (RecursionType.NONE == recursion) {
                    final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundFormat"); //$NON-NLS-1$

                    failureOut.set(
                        new Failure(
                            MessageFormat.format(format, itemSpec.getItem()),
                            FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                            SeverityType.ERROR,
                            itemSpec.getItem()));
                } else {
                    checkResult = targetServerItemExists(lv, pc, pathPart, options);

                    if (checkResult == ExistenceCheckResult.DOES_NOT_EXIST && checkLocalDiskIfNotFound) {
                        checkResult = localItemExistsOnDisk(
                            WorkingFolder.getLocalItemForServerItem(pathPart, wp.getWorkingFolders()));
                    }

                    // Does the path part exist?
                    if (ExistenceCheckResult.DOES_NOT_EXIST == checkResult) {
                        final String format = Messages.getString("ParsedItemSpec.WorkspaceItemNotFoundRecursiveFormat"); //$NON-NLS-1$

                        failureOut.set(
                            new Failure(
                                MessageFormat.format(format, itemSpec.getItem()),
                                FailureCodes.ITEM_NOT_FOUND_EXCEPTION,
                                SeverityType.ERROR,
                                itemSpec.getItem()));
                    } else
                    /*
                     * Parent item of non-wildcard expression exists: path and
                     * pattern
                     */
                    {
                        toReturn = new ParsedItemSpec(pathPart, patternPart, recursion, options);
                    }
                }
            } else {
                // The full item exists as specified.
                if (RecursionType.FULL == recursion && ExistenceCheckResult.IS_FILE == checkResult) {
                    toReturn = new ParsedItemSpec(pathPart, patternPart, recursion, options);
                } else {
                    toReturn = new ParsedItemSpec(itemSpec.getItem(), null, recursion, options);
                }
            }
        }

        return toReturn;
    }

    public RecursionType getRecursionType() {
        return recursion;
    }

    public String getTargetItem() {
        return targetItem;
    }

    public String getPattern() {
        return pattern;
    }

    /**
     * Query the local version table starting from the provided target server
     * item, using the recursion type and pattern specified. This is an
     * expensive query because there is no index by target server item on the
     * local version table. Rename map-in and map-outs must be calculated to
     * return accurate results.
     *
     *
     * @param lv
     *        Local version table
     * @param pc
     *        Pending changes table
     * @param targetServerItem
     *        Target server item to query
     * @param recursion
     *        Recursion level for the query
     * @param pattern
     *        Pattern to match, if any (null to match all items)
     * @param options
     * @return The matching local version rows
     */
    public static Iterable<WorkspaceLocalItem> queryLocalVersionsByTargetServerItem(
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final String targetServerItem,
        final RecursionType recursion,
        final String pattern,
        final ParsedItemSpecOptions options) {
        Check.notNull(lv, "lv"); //$NON-NLS-1$
        Check.notNull(pc, "pc"); //$NON-NLS-1$
        Check.notNull(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        final boolean includeDeleted = options.contains(ParsedItemSpecOptions.INCLUDE_DELETED);

        // If there are no renames in the workspace, or the target server item
        // is $/, then this query is very simple.
        if (ServerPath.isRootFolder(targetServerItem) || !pc.hasRenames()) {
            // Query is not limited to a particular IsCommitted state -- returns
            // both
            return lv.queryByServerItem(targetServerItem, recursion, pattern, includeDeleted);
        }

        // Complicated case -- we want to start with the committed server item
        // index of the local version table, subtracting renames out and adding
        // in renames in.

        // We'll start by backing out any renames on the target server item
        // being queried.
        final String committedServerItem = pc.getCommittedServerItemForTargetServerItem(targetServerItem);

        final List<ApplicableRename> additiveRenames = new ArrayList<ApplicableRename>();

        if (!ServerPath.equals(targetServerItem, committedServerItem)) {
            if (ServerPath.equals(
                targetServerItem,
                pc.getTargetServerItemForCommittedServerItem(committedServerItem))) {
                additiveRenames.add(new ApplicableRename(committedServerItem, RenameType.ADDITIVE, recursion));
            }

            // Any adds or branches (uncommitted items) will have their local
            // version rows under the target server item. However, scanning just
            // uncommitted items from the target is insufficient. It is possible
            // for another workspace to commit an add of an item in the target
            // space of our root rename, and then for us to do a get on just
            // that item. If its path does not conflict with any of our pending
            // changes, then that's legal, and it must be a part of our result
            // set.
            additiveRenames.add(new ApplicableRename(targetServerItem, RenameType.ADDITIVE, recursion));
        } else if (ServerPath.equals(
            targetServerItem,
            pc.getTargetServerItemForCommittedServerItem(targetServerItem))) {
            additiveRenames.add(new ApplicableRename(committedServerItem, RenameType.ADDITIVE, recursion));
        }

        // Find all renames whose target is at or below our target server item.
        // We are looking for renames which bring committed items into our
        // target server item's scope.
        for (final LocalPendingChange pendingChange : pc.queryByTargetServerItem(targetServerItem, recursion, null)) {
            if (!pendingChange.isRename()) {
                continue;
            }

            // Here we are one level down, so if our main query's recursion is
            // one-level, reduce it to None.
            additiveRenames.add(
                new ApplicableRename(
                    pendingChange.getCommittedServerItem(),
                    RenameType.ADDITIVE,
                    (RecursionType.ONE_LEVEL == recursion) ? RecursionType.NONE : recursion));
        }

        // We now need to look for renames out of our committed server item
        // root, of any committed server item roots we queried as part of a
        // rename in, and if our root item is affected by a rename, we need to
        // look for rename outs at the target server item of the root item.
        // (Maybe we're renaming from A -> B, but the namespace slot B is only
        // clear because there is a rename from B -> C.)
        final List<ApplicableRename> applicableRenames = new ArrayList<ApplicableRename>();

        for (final ApplicableRename additiveRename : additiveRenames) {
            for (final LocalPendingChange pendingChange : pc.queryByCommittedServerItem(
                additiveRename.getCommittedServerItem(),
                recursion,
                null)) {
                if (!ServerPath.isChild(targetServerItem, pendingChange.getTargetServerItem())) {
                    applicableRenames.add(
                        new ApplicableRename(
                            pendingChange.getCommittedServerItem(),
                            RenameType.SUBTRACTIVE,
                            RecursionType.FULL));
                }
            }
        }

        // Add the additive renames to the subtractive renames. There may be
        // collisions on the committed server item; we will prefer the
        // subtractive rename in this case.
        for (final ApplicableRename additiveRename : additiveRenames) {
            applicableRenames.add(additiveRename);
        }

        // Next is a pass to remove extraneous applicable renames from the list.
        // Sort the applicable renames depth-first, in reverse. Additive sorts
        // after subtractive in the case of a collision.
        Collections.sort(applicableRenames, Collections.reverseOrder());

        RenameType previousRenameType = RenameType.SUBTRACTIVE;
        String previousServerItem = null;

        for (int i = applicableRenames.size() - 1; i >= 0; i--) {
            final ApplicableRename applicableRename = applicableRenames.get(i);

            if (null != previousServerItem
                && ServerPath.isChild(previousServerItem, applicableRename.getCommittedServerItem())) {
                // If a child applicable rename has the same type as its parent,
                // then it is redundant and the child must be removed. We will
                // also remove an applicable rename if the previous one had an
                // identical committed server item. Since we sort subtractive
                // renames after additive ones, this has the effect of
                // preferring subtractive renames over additive ones.
                if (previousRenameType == applicableRename.getRenameType()
                    || ServerPath.equals(applicableRename.getCommittedServerItem(), previousServerItem)) {
                    applicableRenames.remove(i);
                    continue;
                }
            }

            previousRenameType = applicableRename.getRenameType();
            previousServerItem = applicableRename.getCommittedServerItem();
        }

        // We're done removing the extraneous renames -- reverse the list again
        // to put it back in depth-first order.
        Collections.sort(applicableRenames, Collections.reverseOrder());

        // The next pass uses a stack to group additive renames with their child
        // subtractive renames. The output is the list of
        // CommittedServerItemQuery objects.
        final List<CommittedServerItemQuery> queries = new ArrayList<CommittedServerItemQuery>();

        final Stack<CommittedServerItemQuery> queryStack = new Stack<CommittedServerItemQuery>();
        CommittedServerItemQuery currentQuery = null;

        for (final ApplicableRename applicableRename : applicableRenames) {
            while (null != currentQuery
                && !ServerPath.isChild(
                    currentQuery.getCommittedServerItem(),
                    applicableRename.getCommittedServerItem())) {
                queries.add(currentQuery);

                if (queryStack.size() > 0) {
                    currentQuery = queryStack.pop();
                } else {
                    currentQuery = null;
                }
            }

            if (RenameType.ADDITIVE == applicableRename.getRenameType()) {
                if (null != currentQuery) {
                    queryStack.push(currentQuery);
                }

                currentQuery = new CommittedServerItemQuery(
                    applicableRename.getCommittedServerItem(),
                    applicableRename.getRecursionType());
            } else if (RenameType.SUBTRACTIVE == applicableRename.getRenameType() &&
                // If there is no current query, then this is an unparented
                // subtractive rename, which is of no value to the calculation
                // and
                // can be discarded.
            null != currentQuery) {
                currentQuery.getExcludedItems().add(applicableRename.getCommittedServerItem());
            }
        }

        while (queryStack.size() > 0) {
            queries.add(queryStack.pop());
        }

        if (null != currentQuery) {
            queries.add(currentQuery);
        }

        return new QueryLocalVersionsByTargetServerItemEnumerable(
            lv,
            targetServerItem,
            recursion,
            pattern,
            includeDeleted,
            queries);
    }

    private static ExistenceCheckResult localItemExistsOnDisk(final String localItem) {
        if (localItem == null || localItem.length() == 0) {
            return ExistenceCheckResult.DOES_NOT_EXIST;
        }

        final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(localItem);

        if (!attrs.exists()) {
            return ExistenceCheckResult.DOES_NOT_EXIST;
        }

        if (attrs.isDirectory()) {
            return ExistenceCheckResult.IS_FOLDER;
        } else {
            return ExistenceCheckResult.IS_FILE;
        }
    }

    private static ExistenceCheckResult localItemExists(
        final LocalWorkspaceProperties wp,
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final String localItem,
        final ParsedItemSpecOptions options) {
        Check.notNull(lv, "lv"); //$NON-NLS-1$
        Check.notNull(localItem, "localItem"); //$NON-NLS-1$

        final boolean includeDeleted = options.contains(ParsedItemSpecOptions.INCLUDE_DELETED);

        if (null != pc) {
            final String pendingServerItem = WorkingFolder.getServerItemForLocalItem(localItem, wp.getWorkingFolders());

            if (null != pendingServerItem) {
                final LocalPendingChange pcEntry = pc.getByTargetServerItem(pendingServerItem);

                if (null != pcEntry) {
                    if (ItemType.FILE == pcEntry.getItemType()) {
                        return ExistenceCheckResult.IS_FILE;
                    }

                    return ExistenceCheckResult.IS_FOLDER;
                } else if (pc.hasSubItemOfTargetServerItem(pendingServerItem)) {
                    return ExistenceCheckResult.IS_FOLDER;
                }
            }
        }

        for (final WorkspaceLocalItem lvEntry : lv.queryByLocalItem(
            localItem,
            RecursionType.FULL,
            null,
            includeDeleted)) {
            if (LocalPath.equals(lvEntry.getLocalItem(), localItem) && ItemType.FILE == lvEntry.getItemType()) {
                return ExistenceCheckResult.IS_FILE;
            }

            return ExistenceCheckResult.IS_FOLDER;
        }

        return ExistenceCheckResult.DOES_NOT_EXIST;
    }

    private static ExistenceCheckResult targetServerItemExists(
        final WorkspaceVersionTable lv,
        final LocalPendingChangesTable pc,
        final String targetServerItem,
        final ParsedItemSpecOptions options) {
        Check.notNull(lv, "lv"); //$NON-NLS-1$
        Check.notNull(pc, "pc"); //$NON-NLS-1$
        Check.notNull(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        if (ServerPath.isRootFolder(targetServerItem)) {
            return ExistenceCheckResult.IS_FOLDER;
        }

        final LocalPendingChange pcEntry = pc.getByTargetServerItem(targetServerItem);

        if (null != pcEntry) {
            if (ItemType.FILE == pcEntry.getItemType()) {
                return ExistenceCheckResult.IS_FILE;
            }

            return ExistenceCheckResult.IS_FOLDER;
        } else if (pc.hasSubItemOfTargetServerItem(targetServerItem)) {
            return ExistenceCheckResult.IS_FOLDER;
        }

        for (final WorkspaceLocalItem lvEntry : queryLocalVersionsByTargetServerItem(
            lv,
            pc,
            targetServerItem,
            RecursionType.FULL,
            null,
            options)) {
            if (ServerPath.equals(pc.getTargetServerItemForLocalVersion(lvEntry), targetServerItem)
                && ItemType.FILE == lvEntry.getItemType()) {
                return ExistenceCheckResult.IS_FILE;
            }

            return ExistenceCheckResult.IS_FOLDER;
        }

        return ExistenceCheckResult.DOES_NOT_EXIST;
    }

    /**
     * Validates if given local item matches itemType.
     */
    public static boolean matchItemType(final WorkspaceLocalItem localItem, final ItemType itemType) {
        if (itemType == ItemType.ANY) {
            return true;
        } else if (itemType == ItemType.FILE) {
            return !localItem.isDirectory();
        } else if (itemType == ItemType.FOLDER) {
            return localItem.isDirectory();
        } else {
            throw new IllegalArgumentException("Unknown itemType: " + itemType.toString()); //$NON-NLS-1$
        }
    }
}
