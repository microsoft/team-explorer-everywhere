// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.revision;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.revision.Revision;
import com.microsoft.tfs.core.clients.workitem.revision.RevisionCollection;

public class RevisionCollectionImpl implements RevisionCollection {
    private final List<Revision> revisions = new ArrayList<Revision>();

    /*
     * ************************************************************************
     * START of implementation of RevisionCollection interface
     * ***********************************************************************
     */

    @Override
    public Revision get(final int ix) {
        return revisions.get(ix);
    }

    @Override
    public Iterator<Revision> iterator() {
        return revisions.iterator();
    }

    @Override
    public int size() {
        return revisions.size();
    }

    /*
     * ************************************************************************
     * END of implementation of RevisionCollection interface
     * ***********************************************************************
     */

    /**
     * Removes all Revisions in this RevisionCollection.
     */
    public void internalClear() {
        revisions.clear();
    }

    public RevisionImpl getRevisionInternal(final int ix) {
        return (RevisionImpl) revisions.get(ix);
    }

    public void addRevisionToStart(final RevisionImpl revision) {
        revisions.add(0, revision);
    }

    public void addRevisionToEnd(final RevisionImpl revision) {
        revisions.add(revision);
    }
}
