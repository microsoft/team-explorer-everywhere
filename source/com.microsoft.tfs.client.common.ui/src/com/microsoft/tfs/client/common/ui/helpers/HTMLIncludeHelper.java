// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.base64.Base64;

/**
 * Helper method to handle HTML presentation to the user, including "include"
 * files and message transformation (for localization.)
 */
public class HTMLIncludeHelper {
    private static final Log log = LogFactory.getLog(HTMLIncludeHelper.class);

    private static Pattern includePattern;
    private static Pattern optionPattern;
    private static Pattern messagePattern;

    static {
        try {
            includePattern = Pattern.compile("%%%INCLUDE\\((.*?)\\)%%%"); //$NON-NLS-1$
            optionPattern = Pattern.compile("(\\p{Alnum}+)=\"([^\"]+)\"(?:,\\p{Blank}+)?"); //$NON-NLS-1$
            messagePattern = Pattern.compile("%%%MESSAGE\\((.*?)\\)%%%"); //$NON-NLS-1$
        } catch (final PatternSyntaxException e) {
            log.warn("Could not compile html include pattern", e); //$NON-NLS-1$
        }
    }

    private final HTMLIncludeResourceProvider resourceProvider;

    /**
     * Creates an {@link HTMLIncludeHelper} with the given resource loader that
     * will be used to load open files (providing {@link InputStream}s and read
     * localized messages.
     *
     * @param loader
     *        A class capable of loading files for calls to
     *        {@link #readFile(String)} as well as files included by include
     *        statements (not <code>null</code>)
     * @param messagesClass
     *        A class capable of loading messages referenced in loading HTML
     *        files (may be <code>null</code>). Should contain a static
     *        getString method that takes a string (key) and returns a string
     *        (locale specific message.)
     */
    public HTMLIncludeHelper(final HTMLIncludeResourceProvider resourceProvider) {
        Check.notNull(resourceProvider, "resourceProvider"); //$NON-NLS-1$

        this.resourceProvider = resourceProvider;
    }

    /**
     * Read the given resource, performing file include and message
     * transformations, returning the transformed text as a {@link String}.
     *
     * @param resourceName
     *        The name of the resource to include
     * @return The transformed resource
     * @throws IOException
     *         If the given resource, or any included resources, could not be
     *         read
     */
    public String readResource(final String resourceName) throws IOException {
        return readInputStream(resourceProvider.getInputStream(resourceName));
    }

    /**
     * Read the given resource, returning it as base64 encoded representation of
     * the resource.
     *
     * @param resourceName
     *        The name of the resource to include
     * @return The base64 encoded data.
     * @throws IOException
     *         If the given resource, or any included resources, could not be
     *         read
     */
    private String readResourceToBase64(final String resourceName) throws IOException {
        return readInputStreamToBase64(resourceProvider.getInputStream(resourceName));
    }

    /**
     * Read the given {@link InputStream}, returning it as base64 encoded
     * representation of the resource.
     *
     * @param inputStream
     *        The input stream to encode as base64
     * @return The base64 encoded data.
     * @throws IOException
     *         If the given resource, or any included resources, could not be
     *         read
     */
    private String readInputStreamToBase64(final InputStream inputStream) throws IOException {
        Check.notNull(inputStream, "inputStream"); //$NON-NLS-1$

        final BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        int b;
        while ((b = bufferedStream.read()) >= 0) {
            byteStream.write(b);
        }

        return new String(Base64.encodeBase64(byteStream.toByteArray()), "US-ASCII"); //$NON-NLS-1$
    }

    /**
     * Read the given {@link InputStream}, performing file include and message
     * transformations, returning the transformed text as a {@link String}.
     *
     * @param inputStream
     *        The name of the inputStream to include
     * @return The transformed resource
     * @throws IOException
     *         If the given resource, or any included resources, could not be
     *         read
     */
    private String readInputStream(final InputStream inputStream) throws IOException {
        Check.notNull(inputStream, "inputStream"); //$NON-NLS-1$

        final StringBuffer data = new StringBuffer();
        BufferedReader bufferedReader = null;

        try {
            final InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8"); //$NON-NLS-1$
            bufferedReader = new BufferedReader(reader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = transform(line);

                data.append(line);
                data.append("\n"); //$NON-NLS-1$
            }
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (final IOException e) {
                }
            }
        }

        return data.toString();
    }

    /**
     * Transforms the given line, performing inclusion pattern replacement using
     * the {@link #transformIncludes(String)} and
     * {@link #transformMessages(String)} methods.
     *
     * @param input
     *        A line of input
     * @return A line of output, after transformation
     * @throws IOException
     *         If one of the transformers failed
     */
    private String transform(String input) throws IOException {
        Check.notNull(input, "input"); //$NON-NLS-1$

        input = transformIncludes(input);
        input = transformMessages(input);

        return input;
    }

    /**
     * <p>
     * Transforms the given line, including referenced files by delegating to
     * the {@link HTMLIncludeResourceProvider}.
     * </p>
     *
     * <p>
     * Lines with text matching: %%%INCLUDE(file="filename")%%% will be
     * transformed by replacing the line with the given filename. The "format"
     * tag may also be specified with a value of "text" or "base64". If the
     * format is specified as "text", the file will be reinterpreted while being
     * included (ie, includes in included files will also be included.) If the
     * format is specified as "base64", the file will be included as base64
     * data.
     * </p>
     *
     * <p>
     * Examples:
     * </p>
     *
     * <p>
     * %%%INCLUDE(file="filename")%%% will include the resource specified by
     * "filename".
     * </p>
     *
     * <p>
     * %%%INCLUDE(file="filename", format="text")%%% will include the resource
     * specified by "filename". (This is identical to the above example.)
     * </p>
     *
     * <p>
     * %%%INCLUDE(file="filename", format="base64")%%% will include the base64
     * representation of the resource specified by "filename".
     * </p>
     *
     * @param input
     *        An input line from a resource
     * @return The line with any include statements transformed.
     * @throws IOException
     *         If any included resources could not be read
     */
    private String transformIncludes(final String input) throws IOException {
        Check.notNull(input, "input"); //$NON-NLS-1$

        final Matcher includeMatcher = includePattern.matcher(input);
        final StringBuffer transformation = new StringBuffer();

        while (includeMatcher.find()) {
            if (includeMatcher.groupCount() != 1) {
                log.warn(MessageFormat.format("Invalid include statement: {0}", includeMatcher.toString())); //$NON-NLS-1$
                continue;
            }

            final Matcher optionMatcher = optionPattern.matcher(includeMatcher.group(1));
            String resourceName = null;
            String format = "text"; //$NON-NLS-1$

            while (optionMatcher.find()) {
                if (optionMatcher.groupCount() != 2) {
                    log.warn(MessageFormat.format("Invalid include statement: {0}", includeMatcher.group(1))); //$NON-NLS-1$
                    continue;
                }

                if ("file".equals(optionMatcher.group(1))) //$NON-NLS-1$
                {
                    resourceName = optionMatcher.group(2);
                } else if ("format".equals(optionMatcher.group(1))) //$NON-NLS-1$
                {
                    format = optionMatcher.group(2);
                }
            }

            if (resourceName == null) {
                log.warn(MessageFormat.format("Invalid include statement: {0}", includeMatcher.group(1))); //$NON-NLS-1$
            } else if ("base64".equals(format)) //$NON-NLS-1$
            {
                includeMatcher.appendReplacement(transformation, readResourceToBase64(resourceName));
            } else {
                includeMatcher.appendReplacement(transformation, readResource(resourceName));
            }
        }

        includeMatcher.appendTail(transformation);

        return transformation.toString();
    }

    /**
     * <p>
     * Transforms the given line, including referenced localized messages by
     * delegating to the {@link HTMLIncludeResourceProvider}.
     * </p>
     *
     * <p>
     * Lines with text matching: %%%MESSAGE(key)%%% will be transformed by
     * replacing the line with the results of the
     * {@link HTMLIncludeResourceProvider}'s response to the given key.
     *
     * <p>
     * Example:
     * </p>
     *
     * <p>
     * %%%MESSAGE(ClassName.MessageKey)%%% will include the given message for
     * the key ClassName.MessageKey.
     * </p>
     *
     * @param input
     *        An input line from a resource
     * @return The line with any message statements transformed.
     * @throws IOException
     *         If any included resources could not be read
     */
    private String transformMessages(final String input) throws IOException {
        Check.notNull(input, "input"); //$NON-NLS-1$

        final Matcher messageMatcher = messagePattern.matcher(input);
        final StringBuffer transformation = new StringBuffer();

        while (messageMatcher.find()) {
            String replacement = ""; //$NON-NLS-1$

            if (messageMatcher.groupCount() == 1) {
                replacement = resourceProvider.getMessage(messageMatcher.group(1));
            } else {
                log.warn(MessageFormat.format(
                    "Could not transform message constant {0}: no messages class defined", //$NON-NLS-1$
                    messageMatcher.group(0)));
            }

            messageMatcher.appendReplacement(transformation, replacement);
        }

        messageMatcher.appendTail(transformation);

        return transformation.toString();
    }

    /**
     * A resource provider for an {@link HTMLIncludeHelper} that has knowledge
     * of how to load referenced resources and messages.
     *
     * @threadsafety unknown
     */
    public static interface HTMLIncludeResourceProvider {
        /**
         * Opens the given resourceName, returning the resultant
         * {@link InputStream}. If the given resource is not found, the
         * implementation may return an empty string or throw, but it may not
         * return <code>null</code>.
         *
         * @param resourceName
         *        The name of the resource to open, as specified to
         *        {@link HTMLIncludeHelper#readResource(String)} or as a
         *        resource referenced in an include statement.
         * @return An {@link InputStream} representing the specified resource
         *         (never <code>null</code>)
         * @throws IOException
         *         if a failure reading the resource occurred.
         */
        InputStream getInputStream(String resourceName) throws IOException;

        /**
         * <p>
         * Returns the localized message for the given key.
         * </p>
         *
         * <p>
         * If the given key is not found, the implementation may return an empty
         * string, a placeholder string or throw, but it may not return
         * <code>null</code>.
         * </p>
         *
         * @param key
         *        A key representing the message constant.
         * @return The given message (never <code>null</code>)
         * @throws IOException
         *         if a failure locating the message occurred.
         */
        String getMessage(String key) throws IOException;
    }
}
