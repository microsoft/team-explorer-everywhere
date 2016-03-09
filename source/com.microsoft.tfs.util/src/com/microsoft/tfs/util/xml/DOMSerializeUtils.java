// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.xml;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;

/**
 * <p>
 * {@link DOMSerializeUtils} contains static utility methods to serialize DOM
 * {@link Document}s. {@link Document}s can be serialized to a variety of
 * targets, or client code can specify a JAXP {@link Result} to serialize to.
 * The serialization can be controlled to some degree by passing in flags.
 * </p>
 *
 * <p>
 * There are two modes of using the static methods on {@link DOMSerializeUtils}.
 * If you call a method that does not take a {@link Transformer} (or pass
 * <code>null</code> to a method that does take one), a {@link Transformer} from
 * an internal pool will be used for that method. This pool will grow as needed
 * and uses a default {@link TransformerFactory} and {@link Transformer}
 * obtained by calling appropriate methods on {@link JAXPUtils}. Alternatively,
 * you can call variants of each method that allow you to specify a
 * {@link Transformer}. This allows you to configure your own factory and
 * determine your own caching or pooling policy.
 * </p>
 *
 * <p>
 * {@link DOMSerializeUtils} is thread safe and concurrent. Multiple threads can
 * concurrently call either variant of methods without problems.
 * </p>
 */
public class DOMSerializeUtils {
    /**
     * The {@link TransformerCache} that is owned by the
     * {@link DOMSerializeUtils} class (never <code>null</code>).
     */
    private static final TransformerCache cache = new TransformerCache();

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
     * A flag value that can be passed to serialization methods that take flags
     * to indicate that an XML declaration should be serialized.
     */
    public static final int XML_DECLARATION = 1;

    /**
     * A flag value that can be passed to serialization methods that take flags
     * to indicate that the output should be indented.
     */
    public static final int INDENT = 2;

    /**
     * A flag value that can be passed to serialization methods that take flags
     * to indicate that a byte order mark should be produced. Using this flag is
     * not recommended.
     */
    public static final int BYTE_ORDER_MARK = 4;

    /**
     * A flag value that can be passed to serialization methods that take flags
     * to indicate that a doctype should be produced. Using this flag is not
     * recommended.
     */
    public static final int DOCTYPE = 8;

    /**
     * A flag value that can be passed to methods that take streams (or
     * writers). Normally, such methods automatically close the given stream (or
     * writer) before the method completes. If this flag is passed, the stream
     * (or writer) is left open and it is the caller's responsibility to close
     * it.
     */
    public static final int NO_CLOSE = 16;

    /**
     * <p>
     * Serializes the specified {@link Node} to a {@link String}. To serialize
     * the {@link Node}, a {@link Transformer} from the internal pool is used.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @return the {@link String} result (never <code>null</code>)
     */
    public static String toString(final Node node) {
        return toString(null, node, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to a {@link String}. To serialize
     * the {@link Node}, a {@link Transformer} from the internal pool is used.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     * @return the {@link String} result (never <code>null</code>)
     */
    public static String toString(final Node node, final int flags) {
        return toString(null, node, flags);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to a {@link String}.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @return the {@link String} result (never <code>null</code>)
     */
    public static String toString(final Transformer transformer, final Node node) {
        return toString(transformer, node, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to a {@link String}.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     * @return the {@link String} result (never <code>null</code>)
     */
    public static String toString(final Transformer transformer, final Node node, final int flags) {
        final StringWriter stringWriter = new StringWriter();

        serializeToWriter(transformer, node, stringWriter, flags | NO_CLOSE);

        return stringWriter.toString();
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Writer}.
     * This method closes the specified {@link Writer} before it returns (even
     * if an exception is thrown). To serialize the {@link Node}, a
     * {@link Transformer} from the internal pool is used.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param writer
     *        the {@link Writer} to serialize to (must not be <code>null</code>)
     */
    public static void serializeToWriter(final Node node, final Writer writer) {
        serializeToWriter(null, node, writer, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Writer}.
     * This method normally closes the specified {@link Writer} before it
     * returns (even if an exception is thrown). To leave the writer open, pass
     * the {@link #NO_CLOSE} flag. To serialize the {@link Node}, a
     * {@link Transformer} from the internal pool is used.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param writer
     *        the {@link Writer} to serialize to (must not be <code>null</code>)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serializeToWriter(final Node node, final Writer writer, final int flags) {
        serializeToWriter(null, node, writer, flags);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Writer}.
     * This method closes the specified {@link Writer} before it returns (even
     * if an exception is thrown).
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param writer
     *        the {@link Writer} to serialize to (must not be <code>null</code>)
     */
    public static void serializeToWriter(final Transformer transformer, final Node node, final Writer writer) {
        serializeToWriter(transformer, node, writer, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Writer}.
     * This method normally closes the specified {@link Writer} before it
     * returns (even if an exception is thrown). To leave the writer open, pass
     * the {@link #NO_CLOSE} flag.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param writer
     *        the {@link Writer} to serialize to (must not be <code>null</code>)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serializeToWriter(
        final Transformer transformer,
        final Node node,
        final Writer writer,
        final int flags) {
        Check.notNull(writer, "writer"); //$NON-NLS-1$

        try {
            final Result result = new StreamResult(writer);

            serialize(transformer, node, result, null, flags);
        } finally {
            if ((NO_CLOSE & flags) == 0) {
                IOUtils.closeSafely(writer);
            }
        }
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link File}. To
     * serialize the {@link Node}, a {@link Transformer} from the internal pool
     * is used.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param file
     *        the {@link File} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link File}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     */
    public static void serializeToFile(final Node node, final File file, final String encoding) {
        serializeToFile(null, node, file, encoding, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link File}. To
     * serialize the {@link Node}, a {@link Transformer} from the internal pool
     * is used.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * <p>
     * Pass the {@link #BYTE_ORDER_MARK} flag to write a byte order mark to the
     * file for supported encodings (normally, a byte order mark is not
     * explicitly written; passing this flag is not recommended).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param file
     *        the {@link File} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link File}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serializeToFile(final Node node, final File file, final String encoding, final int flags) {
        serializeToFile(null, node, file, encoding, flags);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link File}.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param file
     *        the {@link File} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link File}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     */
    public static void serializeToFile(
        final Transformer transformer,
        final Node node,
        final File file,
        final String encoding) {
        serializeToFile(transformer, node, file, encoding, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link File}.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * <p>
     * Pass the {@link #BYTE_ORDER_MARK} flag to write a byte order mark to the
     * file for supported encodings (normally, a byte order mark is not
     * explicitly written; passing this flag is not recommended).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param file
     *        the {@link File} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link File}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serializeToFile(
        final Transformer transformer,
        final Node node,
        final File file,
        final String encoding,
        final int flags) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        OutputStream outputStream;

        try {
            outputStream = new FileOutputStream(file);
        } catch (final FileNotFoundException e) {
            throw new XMLException(e);
        }

        outputStream = new BufferedOutputStream(outputStream);

        serializeToStream(transformer, node, outputStream, encoding, flags);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified
     * {@link OutputStream}. This method closes the specified
     * {@link OutputStream} before it returns (even if an exception is thrown).
     * To serialize the {@link Node}, a {@link Transformer} from the internal
     * pool is used.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param outputStream
     *        the {@link OutputStream} to serialize to (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link OutputStream}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     */
    public static void serializeToStream(final Node node, final OutputStream outputStream, final String encoding) {
        serializeToStream(null, node, outputStream, encoding, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified
     * {@link OutputStream}. This method normally closes the specified
     * {@link OutputStream} before it returns (even if an exception is thrown).
     * To leave the stream open, pass the {@link #NO_CLOSE} flag. To serialize
     * the {@link Node}, a {@link Transformer} from the internal pool is used.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * <p>
     * Pass the {@link #BYTE_ORDER_MARK} flag to write a byte order mark to the
     * stream for supported encodings (normally, a byte order mark is not
     * explicitly written; passing this flag is not recommended).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param outputStream
     *        the {@link OutputStream} to serialize to (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link OutputStream}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serializeToStream(
        final Node node,
        final OutputStream outputStream,
        final String encoding,
        final int flags) {
        serializeToStream(null, node, outputStream, encoding, flags);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified
     * {@link OutputStream}. This method closes the specified
     * {@link OutputStream} before it returns (even if an exception is thrown).
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param outputStream
     *        the {@link OutputStream} to serialize to (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link OutputStream}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     */
    public static void serializeToStream(
        final Transformer transformer,
        final Node node,
        final OutputStream outputStream,
        final String encoding) {
        serializeToStream(transformer, node, outputStream, encoding, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified
     * {@link OutputStream}. This method normally closes the specified
     * {@link OutputStream} before it returns (even if an exception is thrown).
     * To leave the stream open, pass the {@link #NO_CLOSE} flag.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * <p>
     * Pass the {@link #BYTE_ORDER_MARK} flag to write a byte order mark to the
     * stream for supported encodings (normally, a byte order mark is not
     * explicitly written; passing this flag is not recommended).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param outputStream
     *        the {@link OutputStream} to serialize to (must not be
     *        <code>null</code>)
     * @param encoding
     *        the encoding to use when writing to the {@link OutputStream}, or
     *        <code>null</code> to not specify an encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serializeToStream(
        final Transformer transformer,
        final Node node,
        final OutputStream outputStream,
        final String encoding,
        final int flags) {
        Check.notNull(outputStream, "outputStream"); //$NON-NLS-1$

        try {
            if ((BYTE_ORDER_MARK & flags) != 0) {
                writeByteOrderMark(outputStream, encoding);
            }

            final Result result = new StreamResult(outputStream);
            serialize(transformer, node, result, encoding, flags);
        } catch (final IOException e) {
            throw new XMLException(e);
        } finally {
            if ((NO_CLOSE & flags) == 0) {
                IOUtils.closeSafely(outputStream);
            }
        }
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Result}. To
     * serialize the {@link Node}, a {@link Transformer} from the internal pool
     * is used.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param result
     *        the {@link Result} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the result encoding to specify to the {@link Transformer}, or
     *        <code>null</code> to not specify a result encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     */
    public static void serialize(final Node node, final Result result, final String encoding) {
        serialize(null, node, result, encoding, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Result}. To
     * serialize the {@link Node}, a {@link Transformer} from the internal pool
     * is used.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param result
     *        the {@link Result} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the result encoding to specify to the {@link Transformer}, or
     *        <code>null</code> to not specify a result encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serialize(final Node node, final Result result, final String encoding, final int flags) {
        serialize(null, node, result, encoding, flags);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Result}.
     * </p>
     *
     * <p>
     * Neither an XML declaration nor a doctype is produced in the output, and
     * the output is not pretty-printed.
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param result
     *        the {@link Result} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the result encoding to specify to the {@link Transformer}, or
     *        <code>null</code> to not specify a result encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     */
    public static void serialize(
        final Transformer transformer,
        final Node node,
        final Result result,
        final String encoding) {
        serialize(transformer, node, result, encoding, NONE);
    }

    /**
     * <p>
     * Serializes the specified {@link Node} to the specified {@link Result}.
     * </p>
     *
     * <p>
     * Pass the {@link #XML_DECLARATION} flag to produce an XML declaration in
     * the output (normally, an XML declaration is not produced). Pass the
     * {@link #INDENT} flag to pretty-print the output (normally,
     * pretty-printing is not done). To produce a doctype in the output,
     * <code>node</code> must be a {@link Document} and the {@link #DOCTYPE}
     * flag must be passed (normally, a doctype is not produced).
     * </p>
     *
     * @throws XMLException
     *         if the serialization fails
     *
     * @param transformer
     *        the {@link Transformer} to serialize with, or <code>null</code> to
     *        use a {@link Transformer} from the internal pool
     * @param node
     *        the {@link Node} to serialize (must not be <code>null</code>)
     * @param result
     *        the {@link Result} to serialize to (must not be <code>null</code>)
     * @param encoding
     *        the result encoding to specify to the {@link Transformer}, or
     *        <code>null</code> to not specify a result encoding (passing
     *        <code>null</code> is not recommended; passing
     *        {@link #ENCODING_UTF8} is recommended)
     * @param flags
     *        a flag value as described above, or {@link #NONE} for default
     *        processing
     */
    public static void serialize(
        Transformer transformer,
        final Node node,
        final Result result,
        final String encoding,
        final int flags) {
        final boolean useCache = (transformer == null);

        if (useCache) {
            transformer = cache.takeTransformer();
        }

        try {
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, (flags & XML_DECLARATION) == 0 ? "yes" //$NON-NLS-1$
                : "no"); //$NON-NLS-1$

            if ((flags & INDENT) != 0) {
                transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
                transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "2"); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                transformer.setOutputProperty(OutputKeys.INDENT, "no"); //$NON-NLS-1$
            }

            if (encoding != null) {
                transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            }

            if ((flags & DOCTYPE) != 0) {
                if (node.getNodeType() == Node.DOCUMENT_NODE) {
                    final Document document = (Document) node;
                    final DocumentType doctype = document.getDoctype();
                    if (doctype != null) {
                        if (doctype.getPublicId() != null) {
                            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
                        }
                        if (doctype.getSystemId() != null) {
                            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
                        }
                    }
                }
            }

            final Source input = new DOMSource(node);

            transformer.transform(input, result);
        } catch (final TransformerException e) {
            throw new XMLException(e);
        } finally {
            if (useCache) {
                cache.releaseTransformer(transformer);
            }
        }
    }

    /**
     * If possible, writes a byte order mark to the given stream. No
     * <code>null</code> checking is done of the stream argument.
     *
     * @param stream
     *        the stream to write the BOM to (must not be <code>null</code>)
     * @param encoding
     *        the encoding to use or <code>null</code>
     * @throws IOException
     *         if the given {@link OutputStream} throws an {@link IOException}
     *         when writing the BOM
     */
    private static void writeByteOrderMark(final OutputStream stream, final String encoding) throws IOException {
        if (encoding == null) {
            /*
             * If encoding is not explicitly specified, we do not support
             * writing a BOM. JAXP does not seem to specify the default encoding
             * that will be used in this case.
             */
            return;
        }

        if ("UTF-8".equalsIgnoreCase(encoding)) //$NON-NLS-1$
        {
            /*
             * Writing a UTF-8 BOM for XML documents is generally considered bad
             * practice and should be avoided. This code is to allow for better
             * interoperability with Microsoft tools, which ignore this advice
             * and write UTF-8 BOMs.
             */

            stream.write(new byte[] {
                (byte) 0xEF,
                (byte) 0xBB,
                (byte) 0xBF
            });
        }

        if ("UTF-16BE".equalsIgnoreCase(encoding)) //$NON-NLS-1$
        {
            stream.write(new byte[] {
                (byte) 0xFE,
                (byte) 0xFF
            });
        }

        if ("UTF-16LE".equalsIgnoreCase(encoding)) //$NON-NLS-1$
        {
            stream.write(new byte[] {
                (byte) 0xFF,
                (byte) 0xFE
            });
        }

        /*
         * We do not need to handle the "UTF-16" encoding here. Unlike the
         * UTF-16BE and UTF-16LE encodings, the Java implementation of the
         * UTF-16 encoding always writes a byte order mark.
         */
    }
}
