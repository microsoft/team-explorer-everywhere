// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.xml;

import java.io.File;
import java.io.InputStream;
import java.text.MessageFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

/**
 * {@link JAXPUtils} contains static utility methods for creating JAXP objects.
 * No caching of any of these objects is done by this class.
 */
public class JAXPUtils {
    private static final Log log = LogFactory.getLog(JAXPUtils.class);

    private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage"; //$NON-NLS-1$
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema"; //$NON-NLS-1$
    private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource"; //$NON-NLS-1$

    /**
     * A convenience method to create a new {@link DocumentBuilderFactory}. The
     * factory is configured to be namespace aware (
     * {@link DocumentBuilderFactory#setNamespaceAware(boolean)} is called with
     * <code>true</code>).
     *
     * @throws XMLException
     *         if a new factory can't be created
     *
     * @return a new {@link DocumentBuilderFactory} (never <code>null</code>)
     */
    public static DocumentBuilderFactory newDocumentBuilderFactory() {
        return newDocumentBuilderFactory(null);
    }

    /**
     * A convenience method to create a new {@link DocumentBuilderFactory}. If
     * the given {@link ClassLoader} is not <code>null</code>, it is temporarily
     * set as the calling thread's context classloader while calling into JAXP
     * to get a {@link DocumentBuilderFactory} instance. The factory is
     * configured to be namespace aware (
     * {@link DocumentBuilderFactory#setNamespaceAware(boolean)} is called with
     * <code>true</code>).
     *
     * @throws XMLException
     *         if a new factory can't be created
     *
     * @param contextClassloader
     *        the context classloader or <code>null</code> if none
     * @return a new {@link DocumentBuilderFactory} (never <code>null</code>)
     */
    public static DocumentBuilderFactory newDocumentBuilderFactory(final ClassLoader contextClassloader) {
        final boolean setTCL = contextClassloader != null;
        ClassLoader currentTCL = null;

        if (setTCL) {
            currentTCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(contextClassloader);
        }

        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            if (log.isTraceEnabled()) {
                final String messageFormat = "Created a new DocumentBuilderFactory: {0}(loaded from: {1})"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    factory.getClass().getName(),
                    factory.getClass().getClassLoader());
                log.trace(message);
            }

            factory.setNamespaceAware(true);

            return factory;
        } catch (final FactoryConfigurationError e) {
            throw new XMLException(e);
        } finally {
            if (setTCL) {
                Thread.currentThread().setContextClassLoader(currentTCL);
            }
        }
    }

    /**
     * A convenience method to create a new {@link DocumentBuilder}. The new
     * {@link DocumentBuilder} is not configured in any way by this method. To
     * create the {@link DocumentBuilder}, a new {@link DocumentBuilderFactory}
     * is created by calling {@link #newDocumentBuilderFactory()}.
     *
     * @throws XMLException
     *         if a new {@link DocumentBuilder} can't be created
     *
     * @return a new {@link DocumentBuilder} (never <code>null</code>)
     */
    public static DocumentBuilder newDocumentBuilder() {
        return newDocumentBuilder(null);
    }

    /**
     * A convenience method to create a new {@link DocumentBuilder} using the
     * given {@link DocumentBuilderFactory}. The new {@link DocumentBuilder} is
     * not configured in any way by this method.
     *
     * @throws XMLException
     *         if a new {@link DocumentBuilder} can't be created
     *
     * @param factory
     *        the {@link DocumentBuilderFactory} to use or <code>null</code> to
     *        create a new factory using the
     *        {@link #newDocumentBuilderFactory()} method
     * @return a new {@link DocumentBuilder} created from the given
     *         {@link DocumentBuilderFactory} (never <code>null</code>)
     */
    public static DocumentBuilder newDocumentBuilder(DocumentBuilderFactory factory) {
        if (factory == null) {
            factory = newDocumentBuilderFactory();
        }

        try {
            final DocumentBuilder builder = factory.newDocumentBuilder();

            if (log.isTraceEnabled()) {
                final String messageFormat = "Created a new DocumentBuilder: {0} (loaded from: {1}) (factory: {2})"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    builder.getClass().getName(),
                    builder.getClass().getClassLoader(),
                    factory.getClass().getName());
                log.trace(message);
            }

            return builder;
        } catch (final ParserConfigurationException e) {
            throw new XMLException(e);
        }
    }

    /**
     * A convenience method to create a new {@link TransformerFactory}. The
     * factory is not configured in any way by this method.
     *
     * @throws XMLException
     *         if a new {@link TransformerFactory} can't be created
     *
     * @return a new {@link TransformerFactory} (never <code>null</code>)
     */
    public static TransformerFactory newTransformerFactory() {
        return newTransformerFactory(null);
    }

    /**
     * A convenience method to create a new {@link TransformerFactory}. If the
     * given {@link ClassLoader} is not <code>null</code>, it is temporarily set
     * as the calling thread's context classloader while calling into JAXP to
     * get a {@link TransformerFactory} instance. The factory is not configured
     * in any way by this method.
     *
     * @throws XMLException
     *         if a new {@link TransformerFactory} can't be created
     *
     * @param contextClassloader
     *        the context classloader or <code>null</code> if none
     * @return a new {@link TransformerFactory} (never <code>null</code>)
     */
    public static TransformerFactory newTransformerFactory(final ClassLoader contextClassloader) {
        final boolean setTCL = contextClassloader != null;
        ClassLoader currentTCL = null;

        if (setTCL) {
            currentTCL = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(contextClassloader);
        }

        try {
            final TransformerFactory factory = TransformerFactory.newInstance();

            if (log.isTraceEnabled()) {
                final String messageFormat = "Created a new TransformerFactory: {0} (loaded from: {1})"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    factory.getClass().getName(),
                    factory.getClass().getClassLoader());
                log.trace(message);
            }

            return factory;
        } catch (final TransformerFactoryConfigurationError e) {
            throw new XMLException(e);
        } finally {
            if (setTCL) {
                Thread.currentThread().setContextClassLoader(currentTCL);
            }
        }
    }

    /**
     * A convenience method to create a new {@link Transformer}. The new
     * {@link Transformer} is not configured in any way by this method. To
     * create the {@link Transformer}, a new {@link TransformerFactory} is
     * created by calling {@link #newTransformerFactory()}.
     *
     * @throws XMLException
     *         if a new {@link Transformer} can't be created
     *
     * @return a new {@link Transformer} (never <code>null</code>)
     */
    public static Transformer newTransformer() {
        return newTransformer(null);
    }

    /**
     * A convenience method to create a new {@link Transformer} using the given
     * {@link TransformerFactory}. The new {@link Transformer} is not configured
     * in any way by this method.
     *
     * @throws XMLException
     *         if a new {@link Transformer} can't be created
     *
     * @param factory
     *        the {@link TransformerFactory} to use or <code>null</code> to
     *        create a new factory using the {@link #newTransformerFactory()}
     *        method
     * @return a new {@link Transformer} created from the given
     *         {@link TransformerFactory} (never <code>null</code>)
     */
    public static Transformer newTransformer(TransformerFactory factory) {
        if (factory == null) {
            factory = newTransformerFactory();
        }

        try {
            final Transformer transformer = factory.newTransformer();

            if (log.isTraceEnabled()) {
                final String messageFormat = "Created a new Transformer: {0} (loaded from: {1}) (factory: {2})"; //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    transformer.getClass().getName(),
                    transformer.getClass().getClassLoader(),
                    factory.getClass().getName());
                log.trace(message);
            }

            return transformer;
        } catch (final TransformerConfigurationException e) {
            throw new XMLException(e);
        }
    }

    /**
     * <p>
     * Creates a new (or configures an existing) {@link DocumentBuilderFactory}
     * that will perform XML Schema validation when parsing. This method is
     * called before parsing to obtain a configured
     * {@link DocumentBuilderFactory} that produces {@link DocumentBuilder}s
     * that will be used for XML Schema for validation.
     * </p>
     *
     * <p>
     * The supplied <code>schemaSource</code> object must be one of the
     * following:
     * <ul>
     * <li>A {@link String} that points to the URI of the schema</li>
     *
     * <li>An {@link InputStream} with the schema contents (will not be closed
     * by this method)</li>
     *
     * <li>A SAX {@link InputSource} that indicates the schema</li>
     *
     * <li>A {@link File} that indicates the schema</li>
     *
     * <li>An array of objects, each one of which is one of the above</li>
     * </ul>
     * </p>
     *
     * @throws XMLException
     *         if the {@link DocumentBuilderFactory} can't be created or
     *         properly configured
     *
     * @param schemaSource
     *        the schema source as described above
     * @return a configured {@link DocumentBuilderFactory} (never
     *         <code>null</code>)
     */
    public static DocumentBuilderFactory newDocumentBuilderFactoryForXSValidation(final Object schemaSource) {
        return newDocumentBuilderFactoryForXSValidation(null, schemaSource);
    }

    /**
     * <p>
     * Creates a new (or configures an existing) {@link DocumentBuilderFactory}
     * that will perform XML Schema validation when parsing. This method is
     * called before parsing to obtain a configured
     * {@link DocumentBuilderFactory} that produces {@link DocumentBuilder}s
     * that will be used for XML Schema for validation.
     * </p>
     *
     * <p>
     * The supplied <code>schemaSource</code> object must be one of the
     * following:
     * <ul>
     * <li>A {@link String} that points to the URI of the schema</li>
     *
     * <li>An {@link InputStream} with the schema contents (will not be closed
     * by this method)</li>
     *
     * <li>A SAX {@link InputSource} that indicates the schema</li>
     *
     * <li>A {@link File} that indicates the schema</li>
     *
     * <li>An array of objects, each one of which is one of the above</li>
     * </ul>
     * </p>
     *
     * @throws XMLException
     *         if the {@link DocumentBuilderFactory} can't be created or
     *         properly configured
     *
     * @param factory
     *        the {@link DocumentBuilderFactory} to configure, or
     *        <code>null</code> to create a {@link DocumentBuilderFactory} using
     *        the {@link #newDocumentBuilderFactory()} method
     * @param schemaSource
     *        the schema source as described above
     * @return a configured {@link DocumentBuilderFactory} (never
     *         <code>null</code>)
     */
    public static DocumentBuilderFactory newDocumentBuilderFactoryForXSValidation(
        DocumentBuilderFactory factory,
        final Object schemaSource) {
        if (factory == null) {
            factory = newDocumentBuilderFactory();
        }

        factory.setNamespaceAware(true);
        factory.setValidating(true);

        try {
            factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
        } catch (final IllegalArgumentException e) {
            final String messageFormat =
                "The DocumentBuilderFactory [{0}] loaded from ClassLoader [{1}] does not support JAXP 1.2"; //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, factory.getClass().getName(), factory.getClass().getClassLoader());
            throw new XMLException(message, e);
        }

        if (schemaSource != null) {
            factory.setAttribute(JAXP_SCHEMA_SOURCE, schemaSource);
        }

        return factory;
    }
}
