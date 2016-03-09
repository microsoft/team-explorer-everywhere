// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;

/**
 * A file attributes entry is a regular expression that can be matched against a
 * TFS file name (not a full path) and a set of {@link FileAttribute}s which can
 * be serialized to and from a line in a TEE attributes file (.tpattributes) as
 * a string.
 * <p>
 * This class is immutable (and therefore thread-safe).
 */
public final class FileAttributesEntry {
    private final static Log log = LogFactory.getLog(FileAttributesEntry.class);

    private final Pattern filenamePattern;
    private final FileAttributesCollection attributes;

    /**
     * Comes after the file name and before all the attributes. Colon is not a
     * special regular expression character, so it has the same meaning as an
     * expression delimiter as for normal file names (TFS does not allow colons
     * in file names).
     */
    private static final char FILE_AND_ATTRIBUTES_SEPARATOR = ':';

    /**
     * The character used to separate multiple attributes inside a serialized
     * entry.
     */
    private static final char ATTRIBUTE_SEPARATOR = '|';

    /**
     * Creates a file attributes entry for the given file with no attributes
     * set.
     *
     * @param filenameExpression
     *        a regular expression describing the names of the files (not full
     *        path) these attributes will apply to (must not be
     *        <code>null</code> or empty).
     */
    public FileAttributesEntry(final String filenameExpression) {
        Check.notNullOrEmpty(filenameExpression, "filename"); //$NON-NLS-1$
        filenamePattern = compilePattern(filenameExpression);
        attributes = new FileAttributesCollection(new FileAttribute[] {});
    }

    /**
     * Creates a file attributes entry for the given file with the given
     * attributes initially set.
     *
     * @param filenameExpression
     *        a regular expression describing the names of the files (not full
     *        path) these attributes will apply to (must not be
     *        <code>null</code> or empty).
     * @param initialAttributes
     *        the inital attributes to add to this entry. The array and its
     *        items must not be null.
     */
    public FileAttributesEntry(final String filenameExpression, final FileAttribute[] initialAttributes) {
        Check.notNullOrEmpty(filenameExpression, "filename"); //$NON-NLS-1$
        Check.notNull(initialAttributes, "initialAttributes"); //$NON-NLS-1$

        filenamePattern = compilePattern(filenameExpression);
        attributes = new FileAttributesCollection(initialAttributes);
    }

    /**
     * Compiles a filename expression into a {@link Pattern} using the regex
     * flags appropriate for a file attributes entry.
     *
     * @param filenameExpression
     *        the filename expression to compile.
     * @return the compiled pattern.
     * @throws PatternSyntaxException
     *         if the regular expression could not be compiled.
     */
    private Pattern compilePattern(final String filenameExpression) {
        return Pattern.compile(filenameExpression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * Parses the given serialized entry (a string like
     * "Class.java:x|b=false|i=34") into a file name and set of attributes.
     * <p>
     * Parsing is liberal. Empty attributes are ignored and not added to the
     * returned entry object's attribute set. If an equals sign is found in the
     * attribute, the attribute is parsed as a StringPairAttribute, otherwise it
     * is parsed as a BooleanFileAttribute.
     * <p>
     * Leading and trailing whitespace is stripped from boolean attributes. An
     * attribute like " x " is parsed equal to attribute "x" or " x".
     * <p>
     * Leading and trailing whitespace is stripped from the keys and values of
     * string pair attributes. " name=value ", "name=value", " name = value",
     * and "name = value" are all equal.
     * <p>
     * Multiple attributes of some types may appear in the returned entry. For
     * instance, if the boolean attribute "b" is read twice, it exists only once
     * in the set. But if both string pair file attributes "zap=abc" and
     * "zap=xyz" are parsed, they will both exist in the returned set.
     *
     * @param serializedEntry
     *        the serialized entry string (like "Name.java:x|b=false|i=34").
     * @returns the parsed file attributes entry.
     * @throws ParseException
     *         if the serialized entry is empty or another error occurred
     *         parsing it.
     * @throws PatternSyntaxException
     *         if the filename expression could not be parsed.
     */
    public static FileAttributesEntry parse(final String serializedEntry)
        throws ParseException,
            PatternSyntaxException {
        Check.notNull(serializedEntry, "serializedEntry"); //$NON-NLS-1$

        if (serializedEntry.length() == 0) {
            throw new ParseException(Messages.getString("FileAttributesEntry.FileAttributesEntryIsEmpty"), 0); //$NON-NLS-1$
        }

        /*
         * Find the first separator.
         */
        final int sepIndex = serializedEntry.indexOf(FILE_AND_ATTRIBUTES_SEPARATOR);
        if (sepIndex == -1) {
            throw new ParseException(
                MessageFormat.format(
                    Messages.getString("FileAttributesEntry.FileAndAttributesSeparatorNotFoundFormat"), //$NON-NLS-1$
                    FILE_AND_ATTRIBUTES_SEPARATOR),
                0);
        }

        final String filenameExpression = serializedEntry.substring(0, sepIndex);

        final String rest = serializedEntry.substring(sepIndex + 1);

        /*
         * Split on all separators.
         */
        final String[] attributeStrings = rest.split("\\" + ATTRIBUTE_SEPARATOR); //$NON-NLS-1$

        final List<FileAttribute> attributes = new ArrayList<FileAttribute>();
        for (int i = 0; i < attributeStrings.length; i++) {
            final String attributeString = attributeStrings[i];

            // Ignore empty attributes.
            if (attributeString.length() == 0) {
                continue;
            }

            FileAttribute a = null;

            if (StringPairFileAttribute.looksLikeStringPairFileAttribute(attributeString)) {
                a = StringPairFileAttribute.parse(attributeString);
            } else {
                a = BooleanFileAttribute.parse(attributeString);
            }

            if (a != null) {
                attributes.add(a);
            } else {
                log.warn(MessageFormat.format("Ignoring malformed file attribute ''{0}''", attributeString)); //$NON-NLS-1$
            }
        }

        return new FileAttributesEntry(filenameExpression, attributes.toArray(new FileAttribute[attributes.size()]));
    }

    /**
     * Returns a serialized-to-string entry. If the attributes set is empty,
     * null is returned.
     */
    @Override
    public String toString() {
        if (attributes.size() == 0) {
            return null;
        }

        final StringBuffer sb = new StringBuffer();

        sb.append(filenamePattern.pattern());
        sb.append(FILE_AND_ATTRIBUTES_SEPARATOR);

        int i = 0;
        for (final FileAttribute attribute : attributes) {
            if (attribute != null) {
                if (i++ > 0) {
                    sb.append(ATTRIBUTE_SEPARATOR);
                }

                sb.append(attribute.toString());
            }
        }

        return sb.toString();
    }

    /**
     * Tests whether the filename expression this entry was built with matches
     * the given file name (no full path).
     *
     * @param filename
     *        the file name to test (not a full path). (must not be
     *        <code>null</code> or empty)
     * @return true if the filename matches (case is ignored), false if it does
     *         not match.
     */
    public boolean matchesFilename(final String filename) {
        Check.notNullOrEmpty(filename, "filename"); //$NON-NLS-1$
        return filenamePattern.matcher(filename).matches();
    }

    /**
     * Get a copy of all attributes contained in this entry.
     *
     * @return all attributes contained in this entry.
     */
    public FileAttributesCollection getAttributes() {
        return attributes;
    }
}
