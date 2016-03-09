// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.wrappers;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._Item;

/**
 * <p>
 * Static utility methods for wrapping and unwrapping arrays of web service
 * objects with {@link WebServiceObjectWrapper}s. The work is done using
 * reflection, see the methods' Javadoc for details.
 * </p>
 * <p>
 * No methods are provided for wrapping/unwrapping single items, since this is
 * easily done via the constructor for the wrapper that takes the item, and the
 * wrapper's getWebServiceObject() method.
 * </p>
 *
 * @threadsafety thread-safe
 */
public abstract class WrapperUtils {
    private static final Log log = LogFactory.getLog(WrapperUtils.class);

    public static final String UNWRAP_METHOD_NAME = "getWebServiceObject"; //$NON-NLS-1$

    /**
     * <p>
     * Takes an array of web service objects (for example, an array of
     * {@link _Item}) and returns an array of web service wrapper objects of the
     * given type (for instance, {@link Item}).
     * </p>
     * <p>
     * A constructor for the given wrapper type that accepts one of the given
     * service objects must exist.
     * </p>
     * <p>
     * <code>null</code> values in the web service objects are copied into the
     * returned array.
     * </p>
     *
     * @param wrapperType
     *        the wrapper object class name (not array class) to use (must not
     *        be <code>null</code>)
     * @param webServiceObjects
     *        the objects to wrap (if null, null is returned)
     * @return a new array of wrapper objects, where each wraps one of the given
     *         web service objects
     */
    public static Object wrap(final Class wrapperType, final Object[] webServiceObjects) {
        Check.notNull(wrapperType, "wrapperType"); //$NON-NLS-1$

        if (webServiceObjects == null) {
            return null;
        }

        final Object ret = Array.newInstance(wrapperType, webServiceObjects.length);
        Class webServiceObjectType = null;
        Constructor constructor = null;

        if (webServiceObjects.length > 0) {
            try {

                for (int i = 0; i < webServiceObjects.length; i++) {
                    if (constructor == null && webServiceObjects[i] != null) {
                        webServiceObjectType = webServiceObjects[i].getClass();
                        constructor = wrapperType.getConstructor(new Class[] {
                            webServiceObjectType
                        });
                    }

                    /*
                     * Persist null values.
                     */
                    Array.set(ret, i, (webServiceObjects[i] != null) ? constructor.newInstance(new Object[] {
                        webServiceObjects[i]
                    }) : null);
                }

            } catch (final NoSuchMethodException e) {
                final String message = MessageFormat.format(
                    "Wrapper error: the desired wrapper class {0} does not have a visible constructor that accepts the web service type {1}", //$NON-NLS-1$
                    wrapperType,
                    webServiceObjectType);

                log.error(message, e);
                throw new RuntimeException(message);
            } catch (final Exception e) {
                final String message = MessageFormat.format(
                    "Error wrapping {0} in {1}", //$NON-NLS-1$
                    webServiceObjectType,
                    wrapperType);

                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }

        return ret;
    }

    /**
     * <p>
     * Takes an array of web service wrapper (for example, an array of
     * {@link Item}) and returns an array of the wrapped web service objects
     * that were inside the given type (for instance, {@link _Item}).
     * </p>
     * <p>
     * A public method named {@link #UNWRAP_METHOD_NAME} which takes no
     * arguments and returns the wrapped web service object must exist.
     * </p>
     * <p>
     * <code>null</code> values in the wrapper objects will be persisted in the
     * returned array.
     * </p>
     *
     * @param webServiceObjectType
     *        the type of the wrapped web service object (not array type) (not
     *        null)
     * @param wrapperObjects
     *        the wrapper objects to get the contents from(if null, null is
     *        returned)
     * @return a new array of web service objects, each extracted from the given
     *         wrapper objects
     */
    public static Object unwrap(final Class webServiceObjectType, final Object[] wrapperObjects) {
        Check.notNull(webServiceObjectType, "webServiceObjectType"); //$NON-NLS-1$

        if (wrapperObjects == null) {
            return null;
        }

        final Object ret = Array.newInstance(webServiceObjectType, wrapperObjects.length);
        if (wrapperObjects.length > 0) {
            try {
                Method method = null;
                for (int i = 0; i < wrapperObjects.length; i++) {
                    if (method == null && wrapperObjects[i] != null) {
                        method = wrapperObjects[i].getClass().getMethod(UNWRAP_METHOD_NAME, new Class[0]);
                    }

                    /*
                     * Persist null values.
                     */
                    Array.set(
                        ret,
                        i,
                        (wrapperObjects[i] != null) ? method.invoke(wrapperObjects[i], new Object[0]) : null);
                }

            } catch (final NoSuchMethodException e) {
                final String message = MessageFormat.format(
                    "Wrapper error: the given wrapper class {0} does not have a method named {1} that returns a {2}", //$NON-NLS-1$
                    wrapperObjects[0].getClass(),
                    UNWRAP_METHOD_NAME,
                    webServiceObjectType);

                log.error(message, e);
                throw new RuntimeException(message, e);
            } catch (final Exception e) {
                final String message = MessageFormat.format(
                    "Error unwrapping {0} from {1}", //$NON-NLS-1$
                    webServiceObjectType,
                    wrapperObjects);

                log.error(message, e);
                throw new RuntimeException(message, e);
            }
        }
        return ret;
    }
}