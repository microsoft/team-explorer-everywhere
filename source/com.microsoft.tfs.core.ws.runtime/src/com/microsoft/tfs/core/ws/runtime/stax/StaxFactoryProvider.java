// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.stax;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;

public abstract class StaxFactoryProvider {
    /**
     * Creates a new {@link XMLInputFactory} that can be used by projects other
     * than the web services runtime, and is compatible with the factories used
     * in the web service runtime. In most situations, the classes would be
     * compatible if the other project simply called
     * {@link XMLInputFactory#newInstance()} itself, but with Java 5 EE and Java
     * 6, and when the other package is a separate Eclipse plug-in, then the
     * factories may not be compatible (Java 5 EE and Java 6 introduced a StAX
     * API that may cause different classloaders to load incompatible
     * implementations).
     *
     * So call this method if you need an {@link XMLInputFactory} that's
     * compatible with the web service layer.
     *
     * @param setCoalescing
     *        if true, the {@link XMLInputFactory#IS_COALESCING} property is set
     *        to {@link Boolean#TRUE} before the factory is returned. This makes
     *        all text returned via {@link XMLStreamReader#getText()} contain
     *        the entire text segment, instead of (possibly) coming back as
     *        multiple events.
     * @return the new factory
     */
    public static XMLInputFactory getXMLInputFactory(final boolean setCoalescing) {
        final ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(StaxFactoryProvider.class.getClassLoader());

            final XMLInputFactory factory = XMLInputFactory.newInstance();

            if (setCoalescing) {
                factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
            }

            return factory;
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextCL);
        }
    }

    /**
     * Like {@link #getXMLInputFactory(boolean)} but for
     * {@link XMLOutputFactory}s.
     *
     * @return the new factory
     */
    public static XMLOutputFactory getXMLOutputFactory() {
        final ClassLoader currentContextCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(StaxFactoryProvider.class.getClassLoader());
            return XMLOutputFactory.newInstance();
        } finally {
            Thread.currentThread().setContextClassLoader(currentContextCL);
        }
    }

}
