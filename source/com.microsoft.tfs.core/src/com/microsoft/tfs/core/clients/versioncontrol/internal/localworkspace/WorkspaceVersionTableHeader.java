// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.WorkspaceVersionTableException;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.util.NotYetImplementedException;

public class WorkspaceVersionTableHeader extends LocalMetadataTable {
    private static final short MAGIC_1 = 0x1234;
    private static final short MAGIC_2 = (short) 0xa7cc;
    private static final int SCHEMA_VERSION_1 = 1;
    private static final int SCHEMA_VERSION_2 = 2;

    private boolean pendingReconcile;

    public WorkspaceVersionTableHeader(final String fileName, final LocalMetadataTable cachedLoadSource)
        throws Exception {
        super(fileName, cachedLoadSource);
        /* Don't do anything here, Initialize() runs first */
    }

    @Override
    protected void initialize() {
        pendingReconcile = false;
    }

    @Override
    protected boolean cachedLoad(final LocalMetadataTable source) {
        // We can cached-load from either a WorkspaceVersionTable or a
        // WorkspaceVersionTableHeader.
        WorkspaceVersionTable lvCached = null;
        if (source instanceof WorkspaceVersionTable) {
            lvCached = (WorkspaceVersionTable) source;
        }

        if (null != lvCached) {
            pendingReconcile = lvCached.getPendingReconcile();
            return true;
        }

        WorkspaceVersionTableHeader lvhCached = null;
        if (source instanceof WorkspaceVersionTableHeader) {
            lvhCached = (WorkspaceVersionTableHeader) source;
        }

        if (null != lvhCached) {
            pendingReconcile = lvhCached.pendingReconcile;

            return true;
        }

        return false;
    }

    public boolean getPendingReconcile() {
        return pendingReconcile;
    }

    @Override
    protected void load(final InputStream is) throws IOException {
        final BinaryReader br = new BinaryReader(is, "UTF-16LE"); //$NON-NLS-1$

        try {
            final short magic = br.readInt16();

            if (MAGIC_1 != magic && MAGIC_2 != magic) {
                throw new WorkspaceVersionTableException(
                    "The workspace version table contains an unknown schema version."); //$NON-NLS-1$
            }

            final int schemaVersion = br.readInt32();
            if (schemaVersion == SCHEMA_VERSION_1 || schemaVersion == SCHEMA_VERSION_2) {
                loadFromVersion1(br);
            } else {
                throw new WorkspaceVersionTableException(
                    "The workspace version table contains an unknown schema version."); //$NON-NLS-1$
            }
        } catch (final Exception e) {
            if (e instanceof WorkspaceVersionTableException) {
                throw (WorkspaceVersionTableException) e;
            } else {
                // Wrap the exception
                throw new WorkspaceVersionTableException(e);
            }
        } finally {
            br.close();
        }
    }

    private void loadFromVersion1(final BinaryReader br) throws IOException {
        pendingReconcile = br.readBoolean();

        // The rest of the file is not read.
    }

    @Override
    protected boolean save(final OutputStream os) {
        throw new NotYetImplementedException();
    }
}
