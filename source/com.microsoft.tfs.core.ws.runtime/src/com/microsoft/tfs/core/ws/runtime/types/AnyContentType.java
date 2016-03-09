// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import java.util.Iterator;

import javax.xml.transform.Source;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.ws.runtime.serialization.ElementDeserializable;
import com.microsoft.tfs.core.ws.runtime.serialization.ElementSerializable;

/**
 * A container for XSD's "any" type, which can hold arbitrary XML data that does
 * not have to follow any schema. Implementations of this interface may choose
 * to store the data as {@link Element}s, provide hooks for custom serialization
 * and deserialization, read/write data in files, or do something else.
 * <p>
 * Each implementation must provide {@link Source}s on demand. It may provide
 * other methods to access the content.
 */
public interface AnyContentType extends ElementSerializable, ElementDeserializable {
    /**
     * Gets an {@link Iterator} for the top-level elements in this
     * {@link AnyContentType}. See the derived classes' documentation for the
     * type returned by {@link Iterator#next()}.
     *
     * @return an {@link Iterator} for top-level element data
     */
    public Iterator getElementIterator();

    /**
     * Cleans up any resources used to provide the content. Callers must call
     * this when done using the instance. All resources accessed via
     * {@link #getSourceIterator()} must be closed before calling
     * {@link #dispose()}.
     */
    public void dispose();
}