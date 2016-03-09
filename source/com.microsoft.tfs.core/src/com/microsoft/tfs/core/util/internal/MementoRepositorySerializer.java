// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.microsoft.tfs.core.internal.persistence.RawDataSerializer;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.XMLMemento;

public class MementoRepositorySerializer extends RawDataSerializer {
    public MementoRepositorySerializer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object deserialize(final DataInputStream dataInputStream) throws IOException, InterruptedException {
        /*
         * XMLMemento is the standard choice, and it implements deserialization
         * for us.
         */
        return XMLMemento.read(dataInputStream, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void serialize(final Object object, final DataOutputStream dataOutputStream)
        throws IOException,
            InterruptedException {
        /*
         * Copy the memento into a new XMLMemento, so we can use its built-in
         * DOM serialization.
         */
        final Memento memento = (Memento) object;

        final XMLMemento xmlMemento = new XMLMemento(memento.getName());
        xmlMemento.putMemento(memento);

        xmlMemento.write(dataOutputStream, null);
    }
}
