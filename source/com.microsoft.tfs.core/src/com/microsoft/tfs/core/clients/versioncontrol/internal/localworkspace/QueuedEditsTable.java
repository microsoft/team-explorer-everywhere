// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeSet;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.InvalidQueuedEditsTableException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;

public class QueuedEditsTable extends LocalMetadataTable {
    private final Set<String> queuedEdits = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

    private final short MAGIC = (short) 0xAB67;
    private final short SCHEMA_VERSION1 = 1;

    public QueuedEditsTable(final String fileName) throws IOException {
        super(fileName);
        /* Don't do anything here, Initialize() runs first */
    }

    @Override
    protected void load(final InputStream is) {
        final BinaryReader br = new BinaryReader(is, "UTF-16LE"); //$NON-NLS-1$
        try {
            final short magic = br.readInt16();

            if (MAGIC != magic) {
                throw new InvalidQueuedEditsTableException();
            }

            final byte schemaVersion = br.readByte();

            switch (schemaVersion) {
                case SCHEMA_VERSION1:
                    loadFromVersion1(br);
                    break;

                default:
                    throw new InvalidQueuedEditsTableException();
            }
        } catch (final Exception e) {
            if (e instanceof InvalidQueuedEditsTableException) {
                throw (InvalidQueuedEditsTableException) e;
            } else {
                // Wrap the exception
                throw new InvalidQueuedEditsTableException(e);
            }
        }
    }

    @Override
    protected boolean save(final OutputStream os) throws IOException {
        final BinaryWriter bw = new BinaryWriter(os, "UTF-16LE"); //$NON-NLS-1$
        try {
            bw.write(MAGIC);
            writeToVersion1(bw);
        } finally {
            bw.close();
        }

        // Return false if we have no queued edits. The base class will delete
        // the table entirely from disk if we return false.
        return queuedEdits.size() > 0;
    }

    private void loadFromVersion1(final BinaryReader br) throws IOException {
        final int queuedEditCount = br.readInt32();

        for (int i = 0; i < queuedEditCount; i++) {
            queuedEdits.add(br.readString());
        }
    }

    private void writeToVersion1(final BinaryWriter bw) throws IOException {
        bw.write(SCHEMA_VERSION1);

        // Number of queued edits stored in the table
        bw.write(queuedEdits.size());

        for (final String queuedEdit : queuedEdits) {
            bw.write(queuedEdit);
        }
    }

    public boolean addQueuedEdit(final String localItem) {
        final boolean added = queuedEdits.add(localItem);

        if (added) {
            setDirty(true);
        }

        return added;
    }

    public boolean removeQueuedEdit(final String localItem) {
        final boolean removed = queuedEdits.remove(localItem);

        if (removed) {
            setDirty(true);
        }

        return removed;
    }

    public void RemoveQueuedEdits(final Iterable<String> localItems) {
        boolean removed = false;

        for (final String localItem : localItems) {
            if (queuedEdits.remove(localItem) && !removed) {
                removed = true;
            }
        }

        if (removed) {
            setDirty(true);
        }
    }

    public String[] getQueuedEdits() {
        final String[] toReturn = new String[queuedEdits.size()];

        queuedEdits.toArray(toReturn);

        return toReturn;
    }
}
