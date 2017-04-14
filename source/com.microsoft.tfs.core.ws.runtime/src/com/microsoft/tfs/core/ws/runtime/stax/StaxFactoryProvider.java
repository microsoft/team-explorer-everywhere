// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.stax;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;

public abstract class StaxFactoryProvider {
    /**
     * @param setCoalescing
     *        if true, the {@link XMLInputFactory#IS_COALESCING} property is set
     *        to {@link Boolean#TRUE} before the factory is returned. This makes
     *        all text returned via {@link XMLStreamReader#getText()} contain
     *        the entire text segment, instead of (possibly) coming back as
     *        multiple events.
     * @return the new factory
     */
    public static XMLInputFactory getXMLInputFactory(final boolean setCoalescing) {
        final XMLInputFactory factory = XMLInputFactory.newInstance();

        if (setCoalescing) {
            factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        }

        return factory;
    }

    /**
     * Like {@link #getXMLInputFactory(boolean)} but for
     * {@link XMLOutputFactory}s.
     *
     * @return the new factory
     */
    public static XMLOutputFactory getXMLOutputFactory() {
        final XMLOutputFactory factory = XMLOutputFactory.newInstance();
        factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
        return factory;
    }

}
