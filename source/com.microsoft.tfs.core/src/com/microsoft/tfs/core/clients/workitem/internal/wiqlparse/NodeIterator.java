// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NodeIterator implements Iterator<Node> {
    private int index;
    private final Node node;

    NodeIterator(final Node node) {
        index = -1;
        this.node = node;
    }

    @Override
    public boolean hasNext() {
        return index < (node.getCount() - 1);
    }

    @Override
    public Node next() {
        if ((index + 1) >= node.getCount()) {
            throw new NoSuchElementException();
        }

        return node.getItem(++index);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
