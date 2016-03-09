// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.InvalidPendingChangeTableException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.WebServiceLayerLocalWorkspaces;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.NotYetImplementedException;

public class LocalPendingChangesTableHeader extends LocalMetadataTable {
    private final static short MAGIC = 0x7425;

    private static final byte SCHEMA_VERSION_1 = 1;
    private static final byte SCHEMA_VERSION_2 = 2;

    private int pendingChangeCount;
    private GUID clientSignature;

    public LocalPendingChangesTableHeader(final String fileName, final LocalMetadataTable cachedLoadSource)
        throws Exception {
        super(fileName, cachedLoadSource);
        /* Don't do anything here, Initialize() runs first */
    }

    @Override
    protected void initialize() {
        clientSignature = WebServiceLayerLocalWorkspaces.INITIAL_PENDING_CHANGES_SIGNATURE;
        pendingChangeCount = 0;
    }

    @Override
    protected void load(final InputStream is) throws InvalidPendingChangeTableException, IOException {
        final BinaryReader br = new BinaryReader(is, "UTF-16LE"); //$NON-NLS-1$
        try {
            final short magic = br.readInt16();

            if (MAGIC != magic) {
                throw new InvalidPendingChangeTableException();
            }

            final byte schemaVersion = br.readByte();
            if (schemaVersion == SCHEMA_VERSION_1 || schemaVersion == SCHEMA_VERSION_2) {
                loadFromVersion1(br);
            } else {
                throw new InvalidPendingChangeTableException();
            }
        } catch (final Exception e) {
            if (e instanceof InvalidPendingChangeTableException) {
                throw (InvalidPendingChangeTableException) e;
            } else {
                // Wrap the exception
                throw new InvalidPendingChangeTableException(e);
            }
        } finally {
            br.close();
        }
    }

    private void loadFromVersion1(final BinaryReader br) throws IOException {
        final byte[] clientSignatureBytes = br.readBytes(16);
        clientSignature = new GUID(clientSignatureBytes);
        pendingChangeCount = br.readInt32();
    }

    @Override
    protected boolean cachedLoad(final LocalMetadataTable source) {
        // We can cached-load from either a LocalPendingChangesTable or a
        // LocalPendingChangesTableHeader.
        if (source instanceof LocalPendingChangesTable) {
            final LocalPendingChangesTable pcCached = (LocalPendingChangesTable) source;
            clientSignature = pcCached.getClientSignature();
            pendingChangeCount = pcCached.getCount();
            return true;
        }

        if (source instanceof LocalPendingChangesTableHeader) {
            final LocalPendingChangesTableHeader pchCached = (LocalPendingChangesTableHeader) source;
            clientSignature = pchCached.clientSignature;
            pendingChangeCount = pchCached.pendingChangeCount;
            return true;
        }

        return false;
    }

    @Override
    protected boolean save(final OutputStream os) {
        throw new NotYetImplementedException();
    }

    public GUID getClientSignature() {
        return clientSignature;
    }

    public int getCount() {
        return pendingChangeCount;
    }

}
