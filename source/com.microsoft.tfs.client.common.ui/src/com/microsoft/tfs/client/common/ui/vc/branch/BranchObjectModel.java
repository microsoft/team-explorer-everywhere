// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.vc.branch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

/*
 * Queries a branch's first level to get its parent and children and then
 * continues to repeat the process for all it's ancestors.
 */

public class BranchObjectModel {

    private final Map branchMap = new HashMap();

    private final ArrayList roots = new ArrayList();

    private final ItemIdentifier item;

    private final VersionControlClient vc;

    public BranchObjectModel(
        final VersionControlClient vc,
        final ItemIdentifier id,
        final RecursionType recursionType) {
        this.vc = vc;
        item = id;
        addBranches(id, recursionType);
    }

    public void addBranch(final BranchObject b) {
        branchMap.put(b.getProperties().getRootItem(), b);
        final ItemIdentifier parent = b.getProperties().getParentBranch();
        if (parent == null) {
            roots.add(b);
        } else {
            if (!branchMap.containsKey(parent)) {
                addBranches(parent, RecursionType.NONE);
            }
        }

    }

    public void addBranches(final ItemIdentifier id, final RecursionType recursionType) {
        final BranchObject[] b = vc.queryBranchObjects(id, RecursionType.FULL);
        for (int i = 0; i < b.length; i++) {
            b[i].getProperties().getRootItem();
            addBranch(b[i]);
        }
    }

    public List getChildren(final BranchObject b) {
        final ItemIdentifier[] childIds = b.getChildBranches();
        final ArrayList children = new ArrayList();
        for (int i = 0; i < childIds.length; i++) {
            if (branchMap.containsKey(childIds[i])) {
                children.add(toBranchObject(childIds[i]));
            }
        }
        return children;
    }

    public BranchObject getParent(final BranchObject b) {
        final ItemIdentifier parent = b.getProperties().getParentBranch();
        return toBranchObject(parent);
    }

    public BranchObject[] getBranchObjects() {
        return (BranchObject[]) branchMap.values().toArray(new BranchObject[0]);
    }

    public ItemIdentifier getItem() {
        return item;
    }

    public BranchObject[] getRoots() {
        return (BranchObject[]) roots.toArray(new BranchObject[0]);
    }

    public BranchObject toBranchObject(final ItemIdentifier id) {
        if (id == null) {
            return null;
        }
        return (BranchObject) branchMap.get(id);
    }
}
