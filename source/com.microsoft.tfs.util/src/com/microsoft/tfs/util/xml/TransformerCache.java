// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.xml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import com.microsoft.tfs.util.Check;

/**
 * {@link TransformerCache} implements a pool of JAXP {@link Transformer}s. Each
 * instance of a {@link TransformerCache} owns a JAXP {@link TransformerFactory}
 * . Both the factory and individual transformers in the pool are accessed by
 * {@link TransformerCache} in a thread-safe way. {@link TransformerCache} is
 * thread safe and can safely and performantly be accessed by multiple threads.
 */
public class TransformerCache {
    private TransformerFactory factory;
    private final Object factoryLock = new Object();

    private final List cache = new ArrayList();

    private final Method transformerResetMethod;

    /**
     * Creates a new {@link TransformerCache}. A {@link TransformerFactory} will
     * be created on demand by calling {@link JAXPUtils#newTransformerFactory()}
     * the first time one is needed.
     */
    public TransformerCache() {
        this(null);
    }

    /**
     * Creates a new {@link TransformerCache}, specifying the
     * {@link TransformerFactory} that the pool will own.
     *
     * @param factory
     *        the {@link TransformerFactory} that this cache will own, or
     *        <code>null</code> to create one on demand by calling
     *        {@link JAXPUtils#newTransformerFactory()}
     */
    public TransformerCache(final TransformerFactory factory) {
        synchronized (factoryLock) {
            this.factory = factory;
        }

        Method method = null;
        try {
            method = Transformer.class.getMethod("reset", (Class[]) null); //$NON-NLS-1$
        } catch (final SecurityException e) {
        } catch (final NoSuchMethodException e) {
        }

        transformerResetMethod = method;
    }

    /**
     * Obtains a {@link Transformer} from the pool. If there are no free
     * {@link Transformer}s in the pool, a new {@link Transformer} is returned.
     * The caller is responsible for calling
     * {@link #releaseTransformer(Transformer)} with the return value when they
     * are finished using it.
     *
     * @return a {@link Transformer} from this pool (never <code>null</code>)
     */
    public Transformer takeTransformer() {
        synchronized (cache) {
            if (cache.size() > 0) {
                return (Transformer) cache.remove(cache.size() - 1);
            }
        }

        return createNewTransformer();
    }

    /**
     * Called to return a previously taken {@link Transformer} back to this
     * pool. The caller must not call this method with a {@link Transformer}
     * that was not taken from this pool. The caller must not continue using the
     * {@link Transformer} after returning it to the pool.
     *
     * @param transformer
     *        the {@link Transformer} to release back into the pool (must not be
     *        <code>null</code>)
     */
    public void releaseTransformer(final Transformer transformer) {
        Check.notNull(transformer, "transformer"); //$NON-NLS-1$

        /*
         * Unlike the DocumentBuilderCache, we can't reuse Transformer instances
         * unless we can reset them. Not all Transformer instances created by
         * this cache will be used in the same way. The reset method was added
         * in JAXP 1.3 (Java 5), so we have to call it reflectively to be
         * compatible with Java 1.4. If reset fails, we can't add the
         * transformer back to the cache.
         */

        if (transformerResetMethod == null) {
            return;
        }

        try {
            transformerResetMethod.invoke(transformer, (Object[]) null);
        } catch (final IllegalArgumentException e) {
            return;
        } catch (final IllegalAccessException e) {
            return;
        } catch (final InvocationTargetException e) {
            return;
        }

        synchronized (cache) {
            cache.add(transformer);
        }
    }

    private Transformer createNewTransformer() {
        Transformer transformer;

        synchronized (factoryLock) {
            if (factory == null) {
                factory = JAXPUtils.newTransformerFactory();
            }

            transformer = JAXPUtils.newTransformer(factory);
        }

        return transformer;
    }
}
