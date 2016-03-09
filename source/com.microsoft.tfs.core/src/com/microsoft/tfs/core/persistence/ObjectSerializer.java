// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 * A {@link ObjectSerializer} is used to serialize and deserialize in-memory
 * objects out to byte streams. Several implementations exist in this package.
 * </p>
 *
 * @see PersistenceStore
 *
 * @since TEE-SDK-10.1
 */
public interface ObjectSerializer {
    /**
     * Serializes an in-memory object to a byte stream. This method should not
     * close the supplied {@link OutputStream} - it is the caller's
     * responsibility to close it.
     *
     * @param object
     *        the in-memory object to serialize (must not be <code>null</code>)
     * @param outputStream
     *        the output stream to serialize to (must not be <code>null</code>)
     * @throws IOException
     * @throws InterruptedException
     */
    public void serialize(Object object, OutputStream outputStream) throws IOException, InterruptedException;

    /**
     * Deserializes the contents of a byte stream and produce an in-memory
     * object. This method should not close the supplied {@link InputStream} -
     * it is the caller's responsibility to close it.
     *
     * @param inputStream
     *        the input stream to deserialize (must not be <code>null</code>)
     * @return the result of the deserialization
     * @throws IOException
     * @throws InterruptedException
     */
    public Object deserialize(InputStream inputStream) throws IOException, InterruptedException;
}
