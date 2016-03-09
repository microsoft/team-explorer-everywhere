// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * A collection of constants and static utility methods for dealing with newline
 * characters in a Unicode-friendly way.
 * </p>
 *
 * <p>
 * For more information about newlines in Unicode, consult the following sources
 * which were refereneced when writing this class:
 * <ul>
 * <li>The Unicode standard 4.0, Chapter 5, Section 5.8
 * (http://www.unicode.org/versions/Unicode4.0.0/ch05.pdf)</li>
 * <li>Unicode Explained by Jukka K. Korpela, pages 424-427</li>
 * <li>Unicode Demystified by Richard Gillam, pages 457-460</li>
 * </ul>
 * </p>
 */
public class NewlineUtils {
    private static final Log log = LogFactory.getLog(NewlineUtils.class);

    /**
     * The "line feed" character (<code>control-J</code>). This is the standard
     * newline character on Unix-like systems, among others. This character has
     * the code point <code>U+000A</code> and can be represented by the escape
     * sequence <code>'\n'</code>. On Windows systems, the standard newline
     * sequence is a two-character sequence consisting of
     * {@link #CARRIAGE_RETURN} followed by this character.
     */
    public static final char LINE_FEED = '\n';

    /**
     * The "carriage return" character (<code>control-M</code>). This is the
     * standard newline character on pre-OSX Mac systems, among others. This
     * character has the code point <code>U+000D</code> and can be represented
     * by the escape sequence <code>'\r'</code>. On Windows systems, the
     * standard newline sequence is a two-character sequence consisting of this
     * character followed by {@link #LINE_FEED}.
     */
    public static final char CARRIAGE_RETURN = '\r';

    /**
     * The "next line" character. This character has the code point
     * <code>U+0085</code>. This character is the Unicode version of EBCDIC's
     * newline (<code>NL</code>) character, and is used mainly when dealing with
     * IBM mainframe data.
     */
    public static final char NEXT_LINE = '\u0085';

    /**
     * The "line tabulation" character, also known as a "vertical tab" (
     * <code>control-K</code>). This character has the code point
     * <code>U+000B</code>. It is little used, but the Unicode standard suggests
     * treating it as a newline in many situations. It is mainly used by
     * Microsoft Word, which uses this character as a line break within a
     * paragraph (as opposed to the more common paragraph break).
     */
    public static final char LINE_TABULATION = '\u000B';

    /**
     * The "form feed" character (<code>control-L</code>). This character has
     * the code point <code>U+000C</code>. A form feed (or page break) character
     * implies a line break as well, and the Unicode standard suggests treating
     * it like a newline character in many situations.
     */
    public static final char FORM_FEED = '\u000C';

    /**
     * The Unicode "unambiguous line separator" character. This character has
     * the code point <code>U+2028</code>. It is little used.
     */
    public static final char LINE_SEPARATOR = '\u2028';

    /**
     * The Unicode "unambiguous paragraph separator" character. This character
     * has the code point <code>U+2029</code>. It is little used.
     */
    public static final char PARAGRAPH_SEPARATOR = '\u2029';

    /**
     * <p>
     * The platform-dependent newline sequence. For example, on Unix-like
     * platforms this will be <code>'\n'</code> while on Windows systems it will
     * be <code>'\r\n'</code>.
     * </p>
     *
     * <p>
     * This field is a convenience field, fully equivalent to:
     *
     * <pre>
     * System.getProperty(&quot;line.separator&quot;)
     * </pre>
     *
     * </p>
     */
    public static final String PLATFORM_NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    /**
     * The empty string, intended to be used as a value for the
     * <code>replacement</code> argument to the
     * {@link #replaceNewlines(String, String, boolean)} method.
     */
    public static final String REMOVE = ""; //$NON-NLS-1$

    /**
     * A string consisting of a single space character, intended to be used as a
     * value for the <code>replacement</code> argument to the
     * {@link #replaceNewlines(String, String, boolean)} method.
     */
    public static final String SPACE = " "; //$NON-NLS-1$

    /**
     * The number of characters of read-ahead space we require in our buffered
     * readers. Must be large enough to fit all end-of-line sequences we can
     * read. For Windows-style line endings (two characters), we need one
     * character of read-behind because the second character is accessible
     * through a local variable.
     */
    private static final int BUFFERED_READER_MARK_READ_AHEAD_CHARACTERS = 1;

    /**
     * A convenience method, fully equivalent to:
     *
     * <pre>
     * replaceNewlines(reader, writer, NewlineUtils.SPACE, true);
     * </pre>
     *
     * @throws IOException
     *         if an error occurred reading from the {@link Reader}, writing to
     *         the {@link Writer}. These errors may include the discovery of
     *         invalid character sequences in the input.
     *
     * @see #replaceNewlines(String, String, boolean)
     */
    public static void stripNewlines(final Reader reader, final Writer writer) throws IOException {
        replaceNewlines(reader, writer, SPACE, true);
    }

    /**
     * A convenience method, fully equivalent to:
     *
     * <pre>
     * replaceNewlines(input, NewlineUtils.SPACE, true);
     * </pre>
     *
     * @see #replaceNewlines(String, String, boolean)
     */
    public static String stripNewlines(final String input) {
        return replaceNewlines(input, SPACE, true);
    }

    /**
     * A convenience method, fully equivalent to:
     *
     * <pre>
     * replaceNewlines(reader, writer, replacement, true);
     * </pre>
     *
     * @throws IOException
     *         if an error occurred reading from the {@link Reader}, writing to
     *         the {@link Writer}. These errors may include the discovery of
     *         invalid character sequences in the input.
     *
     * @see #replaceNewlines(String, String, boolean)
     */
    public static void replaceNewlines(final Reader reader, final Writer writer, final String replacement)
        throws IOException {
        replaceNewlines(reader, writer, replacement, true);
    }

    /**
     * A convenience method, fully equivalent to:
     *
     * <pre>
     * replaceNewlines(input, replacement, true);
     * </pre>
     *
     * @see #replaceNewlines(String, String, boolean)
     */
    public static String replaceNewlines(final String input, final String replacement) {
        return replaceNewlines(input, replacement, true);
    }

    /**
     * Replaces all newlines in the given {@link Reader} with the
     * <code>replacement</code> argument and writes the results to the given
     * {@link Writer}. This is a convenience method, fully equivalent to
     * calling:
     *
     * <pre>
     * replaceNewlines(reader, writer, PLATFORM_NEWLINE, false)
     * </pre>
     *
     * @throws IOException
     *         if an error occurred reading from the {@link Reader}, writing to
     *         the {@link Writer}. These errors may include the discovery of
     *         invalid character sequences in the input.
     */
    public static void normalizeToPlatform(final Reader reader, final Writer writer) throws IOException {
        replaceNewlines(reader, writer, PLATFORM_NEWLINE, false);
    }

    /**
     * Replaces all newlines in the input <code>String</code> with the
     * platform-specific newline sequence. This is a convenience method, fully
     * equivalent to calling:
     *
     * <pre>
     * replaceNewlines(input, PLATFORM_NEWLINE, false)
     * </pre>
     */
    public static String normalizeToPlatform(final String input) {
        return replaceNewlines(input, PLATFORM_NEWLINE, false);
    }

    /**
     * Detects which newline convention used given {@link Reader}. The first
     * newline sequence detected is returned (<code>null</code> if none is
     * detected).
     *
     * The return value is a string containing one of the fields in this class,
     * or for Windows-style newlines, a string with a {@link #CARRIAGE_RETURN}
     * followed by {@link #LINE_FEED}.
     *
     * @param reader
     *        the {@link Reader} to read from
     * @return the newline sequence detected, <code>null</code> if the reader
     *         was <code>null</code> or no newline sequence was detected
     */
    public static String detectNewlineConvention(final Reader reader) {
        if (reader != null) {
            final BufferedReader bufferedReader = new BufferedReader(reader);

            int currentCharacter;
            try {
                while ((currentCharacter = bufferedReader.read()) != -1) {
                    /*
                     * Move the index past the second character of the Windows
                     * newline sequence (CRLF), if applicable.
                     */
                    if (currentCharacter == CARRIAGE_RETURN) {
                        if (bufferedReader.read() == LINE_FEED) {
                            /*
                             * Found a Windows-style CRLF.
                             */
                            return "" + (CARRIAGE_RETURN) + (LINE_FEED); //$NON-NLS-1$
                        }
                    }

                    /*
                     * All other newline sequences are single characters.
                     */
                    if (isNewlineCharacter((char) currentCharacter)) {
                        return "" + ((char) currentCharacter); //$NON-NLS-1$
                    }
                }
            } catch (final IOException e) {
                log.error("Error detecting newline convention", e); //$NON-NLS-1$
            }
        }

        log.debug("Could not detect any newlines in stream"); //$NON-NLS-1$

        return null;
    }

    /**
     * <p>
     * Replaces newlines read from the given {@link Reader} with the
     * <code>replacement</code> argument and writes the results to the given
     * {@link Writer}.
     * </p>
     * <p>
     * This method internally wraps the given reader in a {@link BufferedReader}
     * (so it can mark and rewind the stream to read multi-character EOL
     * sequences), so the caller may omit its own buffering layer. The
     * {@link Writer} is not automatically buffered.
     * </p>
     *
     * @see #replaceNewlines(String, String, boolean)
     *
     * @param reader
     *        The input {@link Reader} to read characters from. If the input is
     *        <code>null</code>, the method returns immediately (no characters
     *        are written to the {@link Writer}).
     * @param writer
     *        The output {@link Writer} where characters and converted newlines
     *        are written (must not be <code>null</code>)
     * @param replacement
     *        The replacement <code>String</code> for newlines (must not be
     *        <code>null</code>)
     * @param groupNewlines
     *        controls the behavior of this method when there are consecutive
     *        newlines in the input (see the Javadoc for the method that takes
     *        String input for details)
     * @throws IOException
     *         if an error occurred reading from the {@link Reader}, writing to
     *         the {@link Writer}. These errors may include the discovery of
     *         invalid character sequences in the input.
     */
    public static void replaceNewlines(
        final Reader reader,
        final Writer writer,
        final String replacement,
        final boolean groupNewlines) throws IOException {
        if (reader == null) {
            return;
        }

        Check.notNull(writer, "output"); //$NON-NLS-1$
        Check.notNull(replacement, "replacement"); //$NON-NLS-1$

        final BufferedReader bufferedReader = new BufferedReader(reader);

        boolean inNewlineGroup = false;

        int currentCharacter;
        while ((currentCharacter = bufferedReader.read()) != -1) {
            final boolean isNewlineCharacter = isNewlineCharacter((char) currentCharacter);

            /*
             * Move the index past the second character of the Windows newline
             * sequence (CRLF), if applicable.
             */
            if (currentCharacter == CARRIAGE_RETURN) {
                /*
                 * Set a mark so we can rewind after peeking.
                 */
                bufferedReader.mark(BUFFERED_READER_MARK_READ_AHEAD_CHARACTERS);

                final int nextCharacter = bufferedReader.read();

                if (nextCharacter == LINE_FEED) {
                    /*
                     * Is a line feed, set this as the current character for
                     * evaluation this iteration.
                     */
                    currentCharacter = nextCharacter;
                } else {
                    /*
                     * Not a line feed or end of stream.
                     */
                    bufferedReader.reset();
                }
            }

            if (isNewlineCharacter) {
                if (groupNewlines) {
                    /*
                     * Just record that we've entered a newline group - we'll
                     * apply the replacement string to the result after we've
                     * exited the group.
                     */
                    inNewlineGroup = true;
                } else {
                    /*
                     * Not in grouping mode - each distinct newline character
                     * gets its own replacement.
                     */
                    writer.write(replacement);
                }
            } else {
                if (groupNewlines && inNewlineGroup) {
                    /*
                     * Exiting a newline group - apply the replacement to the
                     * result.
                     */
                    writer.write(replacement);
                    inNewlineGroup = false;
                }
                writer.write(currentCharacter);
            }
        }

        if (inNewlineGroup) {
            /*
             * The input string terminated while we were in a newline group, so
             * be sure to add in the replacement character for this group
             * (without this check, newline groups at the end of strings would
             * be ignored).
             */
            writer.write(replacement);
            inNewlineGroup = false;
        }
    }

    /**
     * <p>
     * Replaces newlines in the <code>input</code> argument with the
     * <code>replacement</code> argument.
     * </p>
     *
     * <p>
     * To remove all newlines from the <code>input</code> argument (replacing
     * them with empty strings) pass {@link #REMOVE} for the
     * <code>replacement</code> argument. To replace newlines with spaces, pass
     * {@link #SPACE} for the <code>replacement</code> argument.
     * </p>
     *
     * <p>
     * If the <code>groupNewlines</code> argument is <code>true</code>, then
     * multiple consecutive newlines are treated as a single newline: a sequence
     * of any number of consecutive newlines is replaced by a single instance of
     * the replacement string. If <code>groupNewlines</code> is false, then each
     * newline is replaced by the replacement string.
     * </p>
     *
     * <p>
     * Regardless of the value of the <code>groupNewlines</code> argument, the
     * Windows platform newline sequence (the two-character sequence
     * {@link #CARRIAGE_RETURN} {@link #LINE_FEED}) is always treated as a
     * single newline.
     * </p>
     *
     * @param input
     *        The input <code>String</code> to perform newline replacement on.
     *        If the input is <code>null</code>, then <code>null</code> will be
     *        returned.
     *
     * @param replacement
     *        The replacement <code>String</code> for newlines (must not be
     *        <code>null</code>)
     *
     * @param groupNewlines
     *        controls the behavior of this method when there are consecutive
     *        newlines in the input (see above)
     *
     * @return a new <code>String</code> created by replacing all newlines in
     *         the input <code>String</code> with the specified replacement
     */
    public static String replaceNewlines(final String input, final String replacement, final boolean groupNewlines) {
        if (input == null) {
            return null;
        }

        Check.notNull(replacement, "replacement"); //$NON-NLS-1$

        final StringReader inputReader = new StringReader(input);
        final StringWriter result = new StringWriter();

        try {
            replaceNewlines(inputReader, result, replacement, groupNewlines);
        } catch (final IOException e) {
            throw new RuntimeException("Error converting newline characters", e); //$NON-NLS-1$
        }

        return result.toString();
    }

    /**
     * <p>
     * Tests whether a character is considered to be a newline character. This
     * method answers <code>true</code> if the given input character is equal to
     * one of the predefined newline characters defined as public constants on
     * this class.
     * </p>
     *
     * <p>
     * Be careful with this method. On the Windows platform, the preferred way
     * of expressing a newline is a sequence of two characters (
     * {@link #CARRIAGE_RETURN} followed by {@link #LINE_FEED}). If this method
     * is used in a naive way, a program could detect single Windows-style
     * newlines as multiple newlines, since each component of the Windows
     * newline sequence is considered a newline character in its own right by
     * this method.
     * </p>
     *
     * @see #LINE_FEED
     * @see #CARRIAGE_RETURN
     * @see #NEXT_LINE
     * @see #LINE_TABULATION
     * @see #FORM_FEED
     * @see #LINE_SEPARATOR
     * @see #PARAGRAPH_SEPARATOR
     *
     * @param c
     *        an input character to test
     * @return <code>true</code> if the character is a newline character as
     *         described above
     */
    public static boolean isNewlineCharacter(final char c) {
        return (c == LINE_FEED)
            || (c == CARRIAGE_RETURN)
            || (c == NEXT_LINE)
            || (c == LINE_TABULATION)
            || (c == FORM_FEED)
            || (c == LINE_SEPARATOR)
            || (c == PARAGRAPH_SEPARATOR);
    }

    /**
     * Converts all of the line-endings in a file to the desired newline
     * sequence. The conversion is done using a temporary file which replaces
     * the original file before the method returns.
     * <p>
     * The character set to use for the input and output files may be specified,
     * or left null to use the default.
     * <p>
     * This method uses Java's {@link InputStreamReader} class to read the input
     * file, automatically detects the byte-encoding of the input file
     * (UTF-16LE, UTF-16BE, UTF-8, etc.). The ability of this method to read and
     * convert files is limited to those which can be read by the Java runtime
     * where it is run.
     *
     * @param file
     *        the file to convert (which will be replaced by the converted file
     *        when this method returns). Not null.
     * @param charset
     *        the {@link Charset} to use to read the input file and write the
     *        converted file, or null to use the default encoding.
     * @param desiredNewlineSequence
     *        the desired newline sequence to write to the converted file. Not
     *        null, but may be empty to remove newlines. See public members of
     *        this class for pre-defined newline characters.
     * @throws UnsupportedEncodingException
     *         if the encoding given is not supported by this Java environment.
     * @throws IOException
     *         if an error occurred reading the file or writing the converted
     *         file. This exception may also indicate an error reading the file
     *         with the given character set.
     */
    public static void convertFile(final File file, final Charset charset, final String desiredNewlineSequence)
        throws UnsupportedEncodingException,
            IOException {
        Check.notNull(file, "file"); //$NON-NLS-1$
        Check.notNull(desiredNewlineSequence, "desiredNewlineSequence"); //$NON-NLS-1$

        final File directory = file.getParentFile();
        File temp = File.createTempFile("tfsEOL", ".tmp", directory); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            /*
             * Don't need a buffered reader because the conversion method uses
             * its own buffer.
             */
            Reader reader = null;
            Writer writer = null;
            try {
                /*
                 * The concept of the "default" character set is complicated,
                 * since it means something different to the different stream
                 * layers involved. It's simplest just to use the different
                 * constructors.
                 */
                if (charset == null) {
                    reader = new InputStreamReader(new FileInputStream(file));
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp)));
                } else {
                    final CharsetDecoder decoder = charset.newDecoder();
                    decoder.onMalformedInput(CodingErrorAction.REPORT);
                    decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

                    reader = new InputStreamReader(new FileInputStream(file), decoder);
                    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp), charset));
                }

                replaceNewlines(reader, writer, desiredNewlineSequence, false);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        log.error("Error closing reader", e); //$NON-NLS-1$
                    }
                }

                if (writer != null) {
                    try {
                        writer.close();
                    } catch (final IOException e) {
                        log.error("Error closing writer", e); //$NON-NLS-1$
                    }
                }
            }

            /*
             * Replace the original with the temp file. Delete the original file
             * because renaming to an existing file which is read-only fails on
             * some platforms. Changing the original file to writeable requires
             * JNI methods not available in com.microsoft.tfs.util for
             * dependency reasons.
             */

            if (file.delete() == false) {
                final String messageFormat = "Error deleting file '{0}' for replacement with EOL-converted file."; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, file.getAbsolutePath());
                log.error(message);
                throw new IOException(message);
            }

            if (temp.renameTo(file) == false) {
                final String messageFormat = "Error renaming temporary file '{0}' to '{1}' after EOL conversion."; //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, temp.getAbsolutePath(), file.getAbsolutePath());
                log.error(message);
                throw new IOException(message);
            }

            /*
             * File no longer exists, we can skip the deletion for slight
             * performance increase.
             */
            temp = null;
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }
}
