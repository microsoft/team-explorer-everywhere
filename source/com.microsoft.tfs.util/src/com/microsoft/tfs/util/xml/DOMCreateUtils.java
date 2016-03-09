// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.xml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;

/**
 * <p>
 * {@link DOMCreateUtils} contains static utility methods to create DOM
 * {@link Document}s. A {@link Document} can be created from scratch (
 * <code>newDocument*()</code>) or created by parsing XML from a data source (
 * <code>parse*()</code>).
 * </p>
 *
 * <p>
 * There are two modes of using the static methods on {@link DOMCreateUtils}. If
 * you call a method that does not take a {@link DocumentBuilder} (or pass
 * <code>null</code> to a method that does take one), a {@link DocumentBuilder}
 * from an internal pool will be used for that method. This pool will grow as
 * needed and uses a default {@link DocumentBuilderFactory} and
 * {@link DocumentBuilder} obtained by calling appropriate methods on
 * {@link JAXPUtils}. Alternatively, you can call variants of each method that
 * allow you to specify a {@link DocumentBuilder}. This allows you to configure
 * your own factory and determine your own caching or pooling policy.
 * </p>
 *
 * <p>
 * {@link DOMCreateUtils} is thread safe and concurrent. Multiple threads can
 * concurrently call either variant of methods without problems.
 * </p>
 */
public class DOMCreateUtils {
    /**
     * The {@link DocumentBuilderCache} that is owned by the
     * {@link DOMCreateUtils} class (never <code>null</code>).
     */
    private static final DocumentBuilderCache cache = new DocumentBuilderCache();

    /**
     * An encoding value that can be passed to methods that take an encoding.
     * This value represents UTF-8 encoding, should always be available, and
     * should be the preferred encoding for most uses.
     */
    public static final String ENCODING_UTF8 = "UTF-8"; //$NON-NLS-1$

    /**
     * A flag value that can be passed to methods that take flags to indicate no
     * special processing.
     */
    public static final int NONE = 0;

    /**
     * A flag value that can be passed to methods that take streams (or
     * readers). Normally, such methods automatically close the given stream (or
     * reader) before the method completes. If this flag is passed, the stream
     * (or reader) is left open and it is the caller's responsibility to close
     * it.
     */
    public static final int NO_CLOSE = 1;

    /**
     * Creates a new {@link Document}. To create the {@link Document}, a
     * {@link DocumentBuilder} from the internal pool is used.
     *
     * @throws XMLException
     *         if the {@link Document} can't be created
     *
     * @param name
     *        the name of the document root element
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document newDocument(final String name) {
        return newDocumentNS(null, null, name);
    }

    /**
     * Creates a new {@link Document}.
     *
     * @throws XMLException
     *         if the {@link Document} can't be created
     *
     * @param builder
     *        the {@link DocumentBuilder} to use when creating the
     *        {@link Document}, or <code>null</code> to use a
     *        {@link DocumentBuilder} from the internal pool
     * @param name
     *        the name of the document root element
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document newDocument(final DocumentBuilder builder, final String name) {
        return newDocumentNS(builder, null, name);
    }

    /**
     * Creates a new {@link Document} that has a root element that is in a
     * namespace. To create the {@link Document}, a {@link DocumentBuilder} from
     * the internal pool is used.
     *
     * @throws XMLException
     *         if the {@link Document} can't be created
     *
     * @param namespaceURI
     *        the namespace URI of the document root element, or
     *        <code>null</code>
     * @param qualifiedName
     *        the qualified name of the document root element
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document newDocumentNS(final String namespaceURI, final String qualifiedName) {
        return newDocumentNS(null, namespaceURI, qualifiedName);
    }

    /**
     * Creates a new {@link Document} that has a root element that is in a
     * namespace.
     *
     * @throws XMLException
     *         if the {@link Document} can't be created
     *
     * @param builder
     *        the {@link DocumentBuilder} to use when creating the
     *        {@link Document}, or <code>null</code> to use a
     *        {@link DocumentBuilder} from the internal pool
     * @param namespaceURI
     *        the namespace URI of the document root element, or
     *        <code>null</code>
     * @param qualifiedName
     *        the qualified name of the document root element
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document newDocumentNS(
        DocumentBuilder builder,
        final String namespaceURI,
        final String qualifiedName) {
        final boolean useCache = (builder == null);

        if (useCache) {
            builder = cache.takeDocumentBuilder();
        }

        try {
            final DOMImplementation implementation = builder.getDOMImplementation();

            return implementation.createDocument(namespaceURI, qualifiedName, null);
        } finally {
            if (useCache) {
                cache.releaseDocumentBuilder(builder);
            }
        }
    }

    /**
     * Creates a new {@link Document} that has a doctype (a {@link DocumentType}
     * ). To create the {@link Document}, a {@link DocumentBuilder} from the
     * internal pool is used.
     *
     * @throws XMLException
     *         if the {@link Document} can't be created
     *
     * @param docTypeQualifiedName
     *        the qualified name of the doctype
     * @param docTypePublicId
     *        the public ID of the doctype
     * @param docTypeSystemId
     *        the system ID of the doctype
     * @param namespaceURI
     *        the namespace URI of the document root element, or
     *        <code>null</code>
     * @param qualifiedName
     *        the qualified name of the document root element
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document newDocumentWithDocType(
        final String docTypeQualifiedName,
        final String docTypePublicId,
        final String docTypeSystemId,
        final String namespaceURI,
        final String qualifiedName) {
        return newDocumentWithDocType(
            null,
            docTypeQualifiedName,
            docTypePublicId,
            docTypeSystemId,
            namespaceURI,
            qualifiedName);
    }

    /**
     * Creates a new {@link Document} that has a doctype (a {@link DocumentType}
     * ).
     *
     * @throws XMLException
     *         if the {@link Document} can't be created
     *
     * @param builder
     *        the {@link DocumentBuilder} to use when creating the
     *        {@link Document}, or <code>null</code> to use a
     *        {@link DocumentBuilder} from the internal pool
     * @param docTypeQualifiedName
     *        the qualified name of the doctype
     * @param docTypePublicId
     *        the public ID of the doctype
     * @param docTypeSystemId
     *        the system ID of the doctype
     * @param namespaceURI
     *        the namespace URI of the document root element, or
     *        <code>null</code>
     * @param qualifiedName
     *        the qualified name of the document root element
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document newDocumentWithDocType(
        DocumentBuilder builder,
        final String docTypeQualifiedName,
        final String docTypePublicId,
        final String docTypeSystemId,
        final String namespaceURI,
        final String qualifiedName) {
        final boolean useCache = (builder == null);

        if (useCache) {
            builder = cache.takeDocumentBuilder();
        }

        try {
            final DOMImplementation implementation = builder.getDOMImplementation();

            final DocumentType doctype =
                implementation.createDocumentType(docTypeQualifiedName, docTypePublicId, docTypeSystemId);

            return implementation.createDocument(namespaceURI, qualifiedName, doctype);
        } finally {
            if (useCache) {
                cache.releaseDocumentBuilder(builder);
            }
        }
    }

    /**
     * Parses the specified {@link File} into a {@link Document}. To parse the
     * {@link File}, a {@link DocumentBuilder} from the internal pool is used.
     *
     * @throws XMLException
     *         if parsing the {@link File} fails
     *
     * @param file
     *        a {@link File} containing XML to parse (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding of the {@link File}, or <code>null</code> if the
     *        encoding is not known (passing <code>null</code> is not
     *        recommended)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseFile(final File file, final String encoding) {
        return parseFile(null, file, encoding);
    }

    /**
     * Parses the specified {@link File} into a {@link Document}.
     *
     * @throws XMLException
     *         if parsing the {@link File} fails
     *
     * @param builder
     *        the {@link DocumentBuilder} to parse with, or <code>null</code> to
     *        use a {@link DocumentBuilder} from the internal pool
     * @param file
     *        a {@link File} containing XML to parse (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding of the {@link File}, or <code>null</code> if the
     *        encoding is not known (passing <code>null</code> is not
     *        recommended)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseFile(final DocumentBuilder builder, final File file, final String encoding) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
            throw new XMLException(e);
        }

        inputStream = new BufferedInputStream(inputStream);

        return parseStream(builder, inputStream, encoding, NONE);
    }

    /**
     * Parses the specified {@link InputStream} into a {@link Document}. This
     * method closes the specified {@link InputStream} before it returns (even
     * if an exception is thrown). To parse the {@link InputStream}, a
     * {@link DocumentBuilder} from the internal pool is used.
     *
     * @throws XMLException
     *         if parsing the {@link InputStream} fails
     *
     * @param inputStream
     *        a {@link InputStream} containing XML to parse (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding of the {@link InputStream}, or <code>null</code> if
     *        the encoding is not known (passing <code>null</code> is not
     *        recommended)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseStream(final InputStream inputStream, final String encoding) {
        return parseStream(null, inputStream, encoding, NONE);
    }

    /**
     * Parses the specified {@link InputStream} into a {@link Document}. This
     * method normally closes the specified {@link InputStream} before it
     * returns (even if an exception is thrown). To leave the stream open, pass
     * the {@link #NO_CLOSE} flag. To parse the {@link InputStream}, a
     * {@link DocumentBuilder} from the internal pool is used.
     *
     * @throws XMLException
     *         if parsing the {@link InputStream} fails
     *
     * @param inputStream
     *        a {@link InputStream} containing XML to parse (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding of the {@link InputStream}, or <code>null</code> if
     *        the encoding is not known (passing <code>null</code> is not
     *        recommended)
     * @param flags
     *        a flags value as described above, or {@link #NONE} for default
     *        processing
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseStream(final InputStream inputStream, final String encoding, final int flags) {
        return parseStream(null, inputStream, encoding, flags);
    }

    /**
     * Parses the specified {@link InputStream} into a {@link Document}. This
     * method closes the specified {@link InputStream} before it returns (even
     * if an exception is thrown).
     *
     * @throws XMLException
     *         if parsing the {@link InputStream} fails
     *
     * @param builder
     *        the {@link DocumentBuilder} to parse with, or <code>null</code> to
     *        use a {@link DocumentBuilder} from the internal pool
     * @param inputStream
     *        a {@link InputStream} containing XML to parse (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding of the {@link InputStream}, or <code>null</code> if
     *        the encoding is not known (passing <code>null</code> is not
     *        recommended)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseStream(
        final DocumentBuilder builder,
        final InputStream inputStream,
        final String encoding) {
        return parseStream(builder, inputStream, encoding, NONE);
    }

    /**
     * Parses the specified {@link InputStream} into a {@link Document}. This
     * method normally closes the specified {@link InputStream} before it
     * returns (even if an exception is thrown). To leave the stream open, pass
     * the {@link #NO_CLOSE} flag.
     *
     * @throws XMLException
     *         if parsing the {@link InputStream} fails
     *
     * @param builder
     *        the {@link DocumentBuilder} to parse with, or <code>null</code> to
     *        use a {@link DocumentBuilder} from the internal pool
     * @param inputStream
     *        a {@link InputStream} containing XML to parse (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding of the {@link InputStream}, or <code>null</code> if
     *        the encoding is not known (passing <code>null</code> is not
     *        recommended)
     * @param flags
     *        a flags value as described above, or {@link #NONE} for default
     *        processing
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseStream(
        final DocumentBuilder builder,
        final InputStream inputStream,
        final String encoding,
        final int flags) {
        Check.notNull(inputStream, "inputStream"); //$NON-NLS-1$

        try {
            final InputSource input = new InputSource(inputStream);
            if (encoding != null) {
                input.setEncoding(encoding);
            }

            return parse(builder, input);
        } finally {
            if ((NO_CLOSE & flags) == 0) {
                IOUtils.closeSafely(inputStream);
            }
        }
    }

    /**
     * Parses the specified {@link Reader} into a {@link Document}. This method
     * closes the specified {@link Reader} before it returns (even if an
     * exception is thrown). To parse the {@link Reader}, a
     * {@link DocumentBuilder} from the internal pool is used.
     *
     * @throws XMLException
     *         if parsing the {@link InputStream} fails
     *
     * @param reader
     *        a {@link Reader} containing XML to parse (must not be
     *        <code>null</code>)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseReader(final Reader reader) {
        return parseReader(null, reader, NONE);
    }

    /**
     * Parses the specified {@link Reader} into a {@link Document}. This method
     * normally closes the specified {@link Reader} before it returns (even if
     * an exception is thrown). To leave the stream open, pass the
     * {@link #NO_CLOSE} flag. To parse the {@link Reader}, a
     * {@link DocumentBuilder} from the internal pool is used.
     *
     * @throws XMLException
     *         if parsing the {@link InputStream} fails
     *
     * @param reader
     *        a {@link Reader} containing XML to parse (must not be
     *        <code>null</code>)
     * @param flags
     *        a flags value as described above, or {@link #NONE} for default
     *        processing
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseReader(final Reader reader, final int flags) {
        return parseReader(null, reader, flags);
    }

    /**
     * Parses the specified {@link Reader} into a {@link Document}. This method
     * closes the specified {@link Reader} before it returns (even if an
     * exception is thrown).
     *
     * @throws XMLException
     *         if parsing the {@link Reader} fails
     *
     * @param builder
     *        the {@link DocumentBuilder} to parse with, or <code>null</code> to
     *        use a {@link DocumentBuilder} from the internal pool
     * @param reader
     *        a {@link Reader} containing XML to parse (must not be
     *        <code>null</code>)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseReader(final DocumentBuilder builder, final Reader reader) {
        return parseReader(builder, reader, NONE);
    }

    /**
     * Parses the specified {@link Reader} into a {@link Document}. This method
     * normally closes the specified {@link Reader} before it returns (even if
     * an exception is thrown). To leave the stream open, pass the
     * {@link #NO_CLOSE} flag.
     *
     * @throws XMLException
     *         if parsing the {@link Reader} fails
     *
     * @param builder
     *        the {@link DocumentBuilder} to parse with, or <code>null</code> to
     *        use a {@link DocumentBuilder} from the internal pool
     * @param reader
     *        a {@link Reader} containing XML to parse (must not be
     *        <code>null</code>)
     * @param flags
     *        a flags value as described above, or {@link #NONE} for default
     *        processing
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseReader(final DocumentBuilder builder, final Reader reader, final int flags) {
        Check.notNull(reader, "reader"); //$NON-NLS-1$

        try {
            final InputSource input = new InputSource(reader);
            return parse(builder, input);
        } finally {
            if ((NO_CLOSE & flags) == 0) {
                IOUtils.closeSafely(reader);
            }
        }
    }

    /**
     * Parses the specified {@link String} into a {@link Document}. To parse the
     * {@link String}, a {@link DocumentBuilder} from the internal pool is used.
     *
     * @throws XMLException
     *         if parsing the {@link String} fails
     *
     * @param xml
     *        a {@link String} containing XML to parse (must not be
     *        <code>null</code>)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseString(final String xml) {
        return parseString(null, xml);
    }

    /**
     * Parses the specified {@link String} into a {@link Document}.
     *
     * @throws XMLException
     *         if parsing the {@link String} fails
     *
     * @param builder
     *        the {@link DocumentBuilder} to parse with, or <code>null</code> to
     *        use a {@link DocumentBuilder} from the internal pool
     * @param xml
     *        a {@link String} containing XML to parse (must not be
     *        <code>null</code>)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parseString(final DocumentBuilder builder, final String xml) {
        Check.notNull(xml, "xml"); //$NON-NLS-1$

        return parseReader(builder, new StringReader(xml), NO_CLOSE);
    }

    /**
     * Parses the specified SAX {@link InputSource} into a {@link Document}. To
     * parse the {@link InputSource}, a {@link DocumentBuilder} from the
     * internal pool is used.
     *
     * @throws XMLException
     *         if parsing the input fails
     *
     * @param input
     *        the {@link InputSource} to parse (must not be <code>null</code>)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parse(final InputSource input) {
        return parse(null, input);
    }

    /**
     * Parses the specified SAX {@link InputSource} into a {@link Document}.
     *
     * @throws XMLException
     *         if parsing the input fails
     *
     * @param builder
     *        the {@link DocumentBuilder} to parse with, or <code>null</code> to
     *        use a {@link DocumentBuilder} from the internal pool
     * @param input
     *        the {@link InputSource} to parse (must not be <code>null</code>)
     * @return a new {@link Document} (never <code>null</code>)
     */
    public static Document parse(DocumentBuilder builder, final InputSource input) {
        Check.notNull(input, "input"); //$NON-NLS-1$

        final boolean useCache = (builder == null);

        if (useCache) {
            builder = cache.takeDocumentBuilder();
        }

        try {
            return builder.parse(input);
        } catch (final SAXException e) {
            throw new XMLException(e);
        } catch (final IOException e) {
            throw new XMLException(e);
        } finally {
            if (useCache) {
                cache.releaseDocumentBuilder(builder);
            }
        }
    }
}
