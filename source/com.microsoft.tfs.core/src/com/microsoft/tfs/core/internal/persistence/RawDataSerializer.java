// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.persistence;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.microsoft.tfs.core.persistence.ObjectSerializer;

/**
 * {@link RawDataSerializer} is a {@link ObjectSerializer} implementation that
 * serves as a base class for serializers that simply read and write raw data.
 * Subclasses implement methods for writing to a {@link DataOutputStream} and
 * reading from a {@link DataInputStream}.
 *
 * @see ObjectSerializer
 */
public abstract class RawDataSerializer implements ObjectSerializer {
    /**
     * {@inheritDoc}
     */
    @Override
    public Object deserialize(final InputStream inputStream) throws IOException, InterruptedException {
        final DataInputStream dataInputStream = new DataInputStream(inputStream);
        return deserialize(dataInputStream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final Object object, final OutputStream outputStream)
        throws IOException,
            InterruptedException {
        final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        serialize(object, dataOutputStream);
    }

    /**
     * Serialize the specified component to the {@link DataOutputStream}.
     *
     * @param object
     *        the component to serialize (must not be <code>null</code>)
     * @param dataOutputStream
     *        the {@link DataOutputStream} (must not be <code>null</code>)
     */
    protected abstract void serialize(Object object, DataOutputStream dataOutputStream)
        throws IOException,
            InterruptedException;

    /**
     * Reads events off of the {@link DataInputStream} to deserialize a
     * component.
     *
     * @param dataInputStream
     *        the {@link DataInputStream} (must not be <code>null</code>)
     * @return the deserialized component, or <code>null</code> if the component
     *         could not be deserialized
     */
    protected abstract Object deserialize(DataInputStream dataInputStream) throws IOException, InterruptedException;
}
