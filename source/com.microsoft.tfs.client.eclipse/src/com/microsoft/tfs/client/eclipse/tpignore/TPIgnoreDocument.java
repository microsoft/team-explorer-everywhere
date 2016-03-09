// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.tpignore;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.NewlineUtils;

/**
 * An editable document which can read and write .tpignore files. A document
 * consists of {@link Line}s, which are immutable, but the collection of lines
 * may be modified.
 *
 * @threadsafety thread-compatible
 */
public class TPIgnoreDocument {
    private final static Log log = LogFactory.getLog(TPIgnoreDocument.class);

    /**
     * The default name for .tpignore files.
     */
    public final static String DEFAULT_FILENAME = ".tpignore"; //$NON-NLS-1$

    private final List<Line> lines = new ArrayList<Line>();
    private final String newlineSequence;
    private final String charsetName;

    /**
     * Constructs a {@link TPIgnoreDocument}.
     *
     *
     * @param newlineSequence
     *        the newline sequence to use when saving the file (must not be
     *        <code>null</code>)
     * @param charsetName
     *        the charset name to use when writing the file to byte stream (
     *        {@link #getInputStream()}) (may be <code>null</code> to use the
     *        default charset)
     */
    public TPIgnoreDocument(final String newlineSequence, final String charsetName) {
        Check.notNull(newlineSequence, "newlineSequence"); //$NON-NLS-1$

        this.newlineSequence = newlineSequence;
        this.charsetName = charsetName;
    }

    /**
     * @return a new {@link List} of {@link Line}s in this document
     */
    public synchronized List<Line> getLines() {
        return new ArrayList<Line>(lines);
    }

    public synchronized void addLine(final Line line) {
        Check.notNull(line, "line"); //$NON-NLS-1$
        lines.add(line);
    }

    public synchronized void setLine(final int index, final Line line) {
        Check.notNull(line, "line"); //$NON-NLS-1$
        lines.set(index, line);
    }

    public synchronized void setLines(final List<Line> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
    }

    public synchronized Line getLine(final int index) {
        return lines.get(index);
    }

    @Override
    public synchronized String toString() {
        final StringBuilder sb = new StringBuilder();

        for (final Line line : lines) {
            sb.append(line.getContents());
            sb.append(newlineSequence);
        }

        return sb.toString();
    }

    /**
     * @return an {@link InputStream} which reads a copy of this
     *         {@link TPIgnoreDocument}'s contents. The stream does not affect
     *         the internal state of the {@link TPIgnoreDocument} after it is
     *         returned (the client can use it however it wants)
     */
    public InputStream getInputStream() {
        final String contents = toString();

        try {
            return new ByteArrayInputStream(charsetName != null ? contents.getBytes(charsetName)
                : contents.getBytes(ResourcesPlugin.getEncoding()));
        } catch (final UnsupportedEncodingException e) {
            log.error(
                MessageFormat.format(
                    "Could not use original encoding {0} when preparing stream; using default instead", //$NON-NLS-1$
                    charsetName),
                e);

            return new ByteArrayInputStream(contents.getBytes());
        }
    }

    /**
     * Writes the document to the specified file. Clients must validate the
     * resource edit.
     *
     * @param file
     *        the file to write the document to (must not be <code>null</code>)
     * @param progressMonitor
     *        the {@link IProgressMonitor} to use (may be <code>null</code>, see
     *        {@link IFile#setContents(InputStream, boolean, boolean, IProgressMonitor)}
     *        )
     * @throws CoreException
     *         see
     *         {@link IFile#setContents(InputStream, boolean, boolean, IProgressMonitor)}
     */
    public void write(final IFile file, final IProgressMonitor progressMonitor) throws CoreException {
        Check.notNull(file, "file"); //$NON-NLS-1$

        if (file.exists() == false) {
            file.create(getInputStream(), false, progressMonitor);
        } else {
            file.setContents(getInputStream(), false, true, progressMonitor);
        }
    }

    /**
     * Reads a .tpignore file from the given {@link IFile} using the
     * {@link IFile} encoding. The file's newline conventions are detected so
     * the document can be saved preserving them.
     *
     * @param file
     *        the {@link IFile} to read from (must not be <code>null</code>)
     * @return a new {@link TPIgnoreDocument}
     * @throws IOException
     *         if an error occurred reading from the file
     * @throws CoreException
     *         if an error occurred reading from the file
     */
    public static TPIgnoreDocument read(final IFile file) throws IOException, CoreException {
        Check.notNull(file, "file"); //$NON-NLS-1$

        if (file.exists() == false) {
            return new TPIgnoreDocument(NewlineUtils.PLATFORM_NEWLINE, null);
        }

        /*
         * Open a reader to detect the newline convention with.
         */
        String newline = null;
        Reader reader = null;
        try {
            reader = new InputStreamReader(file.getContents(), file.getCharset());
            newline = NewlineUtils.detectNewlineConvention(reader);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    // ignore
                }
                reader = null;
            }
        }

        if (newline == null) {
            newline = NewlineUtils.PLATFORM_NEWLINE;
        }

        final TPIgnoreDocument ret = new TPIgnoreDocument(newline, file.getCharset());

        BufferedReader br = null;
        try {
            reader = new InputStreamReader(file.getContents(), file.getCharset());
            br = new BufferedReader(reader);

            String line;
            while ((line = br.readLine()) != null) {
                ret.addLine(new Line(line));
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }

        return ret;
    }
}
