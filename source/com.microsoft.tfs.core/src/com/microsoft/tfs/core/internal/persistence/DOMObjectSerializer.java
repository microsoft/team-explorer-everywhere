// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.w3c.dom.Document;

import com.microsoft.tfs.core.persistence.ObjectSerializer;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * {@link DOMObjectSerializer} is a {@link ObjectSerializer} implementation that
 * serves as a base class for serializers that are based on DOM {@link Document}
 * s. Subclasses need only implement methods for building a {@link Document} and
 * for parsing a {@link Document}.
 *
 * @see ObjectSerializer
 */
public abstract class DOMObjectSerializer implements ObjectSerializer {
    /**
     * {@inheritDoc}
     */
    @Override
    public Object deserialize(final InputStream inputStream) throws IOException, InterruptedException {
        final Document document =
            DOMCreateUtils.parseStream(inputStream, DOMCreateUtils.ENCODING_UTF8, DOMCreateUtils.NO_CLOSE);

        return createComponentFromDocument(document);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final Object object, final OutputStream outputStream)
        throws IOException,
            InterruptedException {
        final Document document = createDocumentFromComponent(object);

        DOMSerializeUtils.serializeToStream(
            document,
            outputStream,
            DOMSerializeUtils.ENCODING_UTF8,
            DOMSerializeUtils.INDENT | DOMSerializeUtils.NO_CLOSE);
    }

    /**
     * Called by the base class during the
     * {@link #serialize(Object, OutputStream)} method. The subclass must
     * produce a DOM {@link Document} for the given in-memory object. The
     * subclass should usually use the {@link DOMUtils} utility class to create
     * a {@link Document}.
     *
     * @param object
     *        the in-memory object to produce a DOM for (must not be
     *        <code>null</code>)
     * @return a {@link Document} that represents the serialized state of the
     *         specified component
     */
    protected abstract Document createDocumentFromComponent(Object object);

    /**
     * Called by the base class during the {@link #deserialize(InputStream)}
     * method. The subclass must walk the supplied {@link Document} and return
     * an {@link Object} corresponding to the data. The subclass can use methods
     * on the {@link DOMUtils} utility class to assist in walking the
     * {@link Document} tree.
     *
     * @param document
     *        the DOM {@link Document} to walk (must not be <code>null</code>)
     * @return an {@link Object} that is the result of walking the DOM and
     *         deserializing the data
     */
    protected abstract Object createComponentFromDocument(Document document);
}
