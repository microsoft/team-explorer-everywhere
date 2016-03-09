// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.history;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.Check;

/**
 * Similar to:
 * Microsoft.TeamFoundation.VersionControl.Controls.HistoryEnumeratorCache
 */
public class HistoryIteratorCache {
    private final Iterator historyIterator;
    private final List cache = new ArrayList();
    private boolean endOfList = false;

    public HistoryIteratorCache(final Iterator iterator) {
        Check.notNull(iterator, "iterator"); //$NON-NLS-1$

        historyIterator = iterator;
    }

    public void cacheItems(final int end) {
        while (!endOfList && end > cache.size()) {
            if (historyIterator.hasNext()) {
                cache.add(historyIterator.next());
            } else {
                endOfList = true;
            }
        }
    }

    public int size() {
        return cache.size();
    }

    public Changeset get(final int index) {
        return (Changeset) cache.get(index);
    }

    public boolean isEndOfList() {
        return endOfList;
    }
}
