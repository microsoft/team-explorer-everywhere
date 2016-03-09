// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

import com.microsoft.tfs.util.Check;

/**
 * {@link DocumentBuilderCache} implements a pool of JAXP
 * {@link DocumentBuilder}s. Each instance of a {@link DocumentBuilderCache}
 * owns a JAXP {@link DocumentBuilderFactory}. Both the factory and individual
 * builders in the pool are accessed by {@link DocumentBuilderCache} in a
 * thread-safe way. {@link DocumentBuilderCache} is thread safe and can safely
 * and performantly be accessed by multiple threads.
 */
public class DocumentBuilderCache {
    private DocumentBuilderFactory factory;
    private final Object factoryLock = new Object();

    private final ErrorHandler errorHandler;
    private final EntityResolver entityResolver;

    private final List cache = new ArrayList();

    /**
     * Creates a new {@link DocumentBuilderCache}. A
     * {@link DocumentBuilderFactory} will be created on demand by calling
     * {@link JAXPUtils#newDocumentBuilderFactory()} the first time one is
     * needed.
     */
    public DocumentBuilderCache() {
        this(null, null, null);
    }

    /**
     * Creates a new {@link DocumentBuilderCache}, specifying the
     * {@link DocumentBuilderFactory} that the pool will own.
     *
     * @param factory
     *        the {@link DocumentBuilderFactory} that this cache will own, or
     *        <code>null</code> to create one on demand by calling
     *        {@link JAXPUtils#newDocumentBuilderFactory()}
     * @param errorHandler
     *        the SAX {@link ErrorHandler} to configure each
     *        {@link DocumentBuilder} in the pool with, or <code>null</code> to
     *        not configure the {@link DocumentBuilder}s with
     *        {@link ErrorHandler}s
     * @param entityResolver
     *        the SAX {@link EntityResolver} to configure each
     *        {@link DocumentBuilder} in the pool with, or <code>null</code> to
     *        not configure the {@link DocumentBuilder}s with
     *        {@link EntityResolver}s
     */
    public DocumentBuilderCache(
        final DocumentBuilderFactory factory,
        final ErrorHandler errorHandler,
        final EntityResolver entityResolver) {
        synchronized (factoryLock) {
            this.factory = factory;
        }

        this.errorHandler = errorHandler;
        this.entityResolver = entityResolver;
    }

    /**
     * Obtains a {@link DocumentBuilder} from the pool. If there are no free
     * {@link DocumentBuilder}s in the pool, a new {@link DocumentBuilder} is
     * returned. The caller is responsible for calling
     * {@link #releaseDocumentBuilder(DocumentBuilder)} with the return value
     * when they are finished using it.
     *
     * @return a {@link DocumentBuilder} from this pool (never <code>null</code>
     *         )
     */
    public DocumentBuilder takeDocumentBuilder() {
        synchronized (cache) {
            if (cache.size() > 0) {
                return (DocumentBuilder) cache.remove(cache.size() - 1);
            }
        }

        return createNewBuilder();
    }

    /**
     * Called to return a previously taken {@link DocumentBuilder} back to this
     * pool. The caller must not call this method with a {@link DocumentBuilder}
     * that was not taken from this pool. The caller must not continue using the
     * {@link DocumentBuilder} after returning it to the pool.
     *
     * @param builder
     *        the {@link DocumentBuilder} to release back into the pool (must
     *        not be <code>null</code>)
     */
    public void releaseDocumentBuilder(final DocumentBuilder builder) {
        Check.notNull(builder, "builder"); //$NON-NLS-1$

        /*
         * One of the design principles of this cache is that all
         * DocumentBuilders in the cache are configured in the same way. For
         * DocumentBuilders, this means they came from the same
         * DocumentBuilderFactory and have the same ErrorHandler and
         * EntityResolver. There is no need for this cache to call the JAXP 1.3
         * reset() method on the DocumentBuilders before putting them back into
         * the cache. The reset() method would reset the ErrorHandler and
         * EntityResolver to defaults, but we don't need this because of the
         * design of this cache.
         */

        synchronized (cache) {
            cache.add(builder);
        }
    }

    private DocumentBuilder createNewBuilder() {
        DocumentBuilder builder;

        synchronized (factoryLock) {
            if (factory == null) {
                factory = JAXPUtils.newDocumentBuilderFactory();
            }

            builder = JAXPUtils.newDocumentBuilder(factory);
        }

        if (errorHandler != null) {
            builder.setErrorHandler(errorHandler);
        }

        if (entityResolver != null) {
            builder.setEntityResolver(entityResolver);
        }

        return builder;
    }
}
