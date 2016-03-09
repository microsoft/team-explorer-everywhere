// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.resourcedata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;

/**
 * Contains information about a resource, persisted via
 * {@link ResourceDataManager}. {@link ResourceDataManager} persists byte
 * arrays, and {@link #toByteArray()} and {@link #fromByteArray(byte[])} handle
 * the serialization process.
 *
 */
public class ResourceData {
    private static final Log log = LogFactory.getLog(ResourceData.class);

    private final String serverItem;
    private final int changesetId;

    public ResourceData(final String serverItem, final int changesetId) {
        Check.notNull(serverItem, "serverItem"); //$NON-NLS-1$

        this.serverItem = serverItem;
        this.changesetId = changesetId;
    }

    public String getServerItem() {
        return serverItem;
    }

    public int getChangesetID() {
        return changesetId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MessageFormat.format("{0} {1}", serverItem, Integer.toString(changesetId)); //$NON-NLS-1$
    }

    /**
     * @return this object serialized to a byte array, or <code>null</code> if
     *         there was a problem serializing
     */
    public byte[] toByteArray() {
        try {
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            final DataOutputStream dos = new DataOutputStream(os);

            dos.writeUTF(serverItem);
            dos.writeInt(changesetId);

            dos.close();
            return os.toByteArray();
        } catch (final IOException e) {
            log.error("Error serializing", e); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * Deserializes a {@link ResourceData} object from a byte array.
     *
     * @param value
     *        the byte array (not null)
     * @return the deserialized {@link ResourceData} or <code>null</code> if
     *         there was a problem deserializing
     */
    public static ResourceData fromByteArray(final byte[] value) {
        Check.notNull(value, "value"); //$NON-NLS-1$

        try {
            final ByteArrayInputStream is = new ByteArrayInputStream(value);
            final DataInputStream dis = new DataInputStream(is);

            return new ResourceData(dis.readUTF(), dis.readInt());
        } catch (final IOException e) {
            log.error("Error deserializing", e); //$NON-NLS-1$
            return null;
        }
    }
}
