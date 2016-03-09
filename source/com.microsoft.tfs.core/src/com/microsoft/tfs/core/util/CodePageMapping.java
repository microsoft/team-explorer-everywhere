// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;

/**
 * <h1>Overview</h1>
 *
 * <p>
 * <b>Important:</b> Read the Default Endian Note section before using this
 * class.
 * </p>
 *
 * <p>
 * {@link CodePageMapping} implements a mapping of code pages to Java
 * {@link Charset}s. This mapping is needed because TFS stores file encoding
 * information as code page numbers. To make use of the encoding information
 * from Java, we need to translate a code page into an appropriate Java
 * {@link Charset} to use.
 * </p>
 *
 * <p>
 * Each code page maps to 0 or more canonical charset names. If a code page maps
 * to more than one charset name, the names are tried in sequence until one is
 * found that is a valid charset in the current Java virtual machine.
 * </p>
 *
 * <p>
 * Each canonical charset name maps to 0 or 1 code page integers. If a charset
 * name maps to a code page integer, that code page is considered the best
 * approximation for that charset.
 * </p>
 *
 * <p>
 * The mappings are based on hardcoded data. The mappings can be added to or
 * overridden at runtime by setting system properties:
 * <ul>
 * <li>To map code pages to charsets, set the property
 * <code>codePageMapping.X</code>, where X is the desired code page number to
 * map. The value of the property should be a comma-separated list of charset
 * names to try, in sequence, when mapping the code page.</li>
 * <li>To map charsets to code pages, set the property
 * <code>charsetMapping.X</code>, where X is the charset name. The value of the
 * property should be the code page integer to use when mapping the charset.
 * </li>
 * </ul>
 * For example:
 *
 * <pre>
 * -DcodePageMapping.949=x-windows-949,x-IBM949,x-IBM949C
 * -DcharsetMapping.x-windows-949=949
 * -DcharsetMapping.x-IBM949=949
 * -DcharsetMapping.x-IBM949C=949
 * </pre>
 *
 * </p>
 *
 * <h2>Default Endian Note ("UTF-16", "UTF-32")</h2>
 *
 * <p>
 * Java and Windows assume opposite byte orders when the endian-unspecified
 * encoding names "UTF-16" and "UTF-32" are used for encoding and decoding text.
 * </p>
 *
 * <p>
 * As a Java Charset name, "UTF-16" and "UTF-32" mean "read big-endian if no
 * BOM, always write big-endian". The Unicode Standard specifies this behavior
 * in Section 3.10 (Unicode Encoding Schemes), item D98 (D101 specifies the same
 * behavior for UTF-32):
 * </p>
 *
 * <blockquote>D98: "The UTF-16 encoding scheme may or may not begin with a BOM.
 * However, when there is no BOM, and in the absence of a higher-level protocol,
 * the byte order of the UTF-16 encoding scheme is big-endian." </blockquote>
 *
 * <p>
 * However, Windows doesn't follow the standard when these names are used. It
 * assumes "read little-endian if no BOM, always write little-endian".
 * </p>
 *
 * <p>
 * In this class, Windows code page 1201 (aka "Unicode (Big-Endian)",
 * "unicodeFFFE") is mapped to the Java Charset name "UTF-16" which triggers
 * big-endian behavior with readers/writers. Correspondingly, if Java tells us a
 * reader/writer is in "UTF-16" encoding, we want to tell TFS that we're using
 * Windows code page 1201. "UTF-32" works similarly.
 * </p>
 *
 * <p>
 * Additionally, Windows code page 1200 (aka "Unicode", "utf-16"; little-endian
 * assumed) must map from/to the explicit-endian Java Charset name "UTF-16LE".
 * Make sure to specify the endian-explicit "UTF-16LE" Java Charset (or
 * "UTF32-LE") if you mean little-endian.
 * </p>
 *
 * @see Charset
 */
public class CodePageMapping {
    private static final Log log = LogFactory.getLog(CodePageData.class);

    private static final String charsetMappingKey = "charsetMapping"; //$NON-NLS-1$
    private static final String codePageMappingKey = "codePageMapping"; //$NON-NLS-1$

    /**
     * Gets a list of charsets that are mappable to code pages.
     *
     * @return Known Charsets
     */
    public static Charset[] getCharsets() {
        final List<Charset> charsetList = new ArrayList<Charset>();
        final List<String> charsetNameList = new ArrayList<String>();

        charsetNameList.addAll(Arrays.asList(CodePageData.getCharsetNames()));
        charsetNameList.addAll(Arrays.asList(getSystemPropertyKeys(charsetMappingKey)));

        for (final Iterator<String> i = charsetNameList.iterator(); i.hasNext();) {
            final String charsetName = i.next();

            if (tryCharsetIsSupported(charsetName)) {
                final Charset charset = tryCharsetForName(charsetName);

                if (charset != null && !charsetList.contains(charset)) {
                    charsetList.add(charset);
                }
            }
        }

        return charsetList.toArray(new Charset[charsetList.size()]);
    }

    /**
     * Gets a list of codepages that are mappable to code pages.
     *
     * @return Known Code Pages
     */
    public static int[] getCodePages() {
        final List<Integer> codePageList = new ArrayList<Integer>();

        codePageList.addAll(Arrays.asList(CodePageData.getCodePages()));
        final String[] codePageStrings = getSystemPropertyKeys(codePageMappingKey);

        for (int i = 0; i < codePageStrings.length; i++) {
            try {
                final Integer codePage = new Integer(codePageStrings[i]);

                if (!codePageList.contains(codePage)) {
                    codePageList.add(codePage);
                }
            } catch (final NumberFormatException e) {
                /* Ignore */
            }
        }

        final int[] codePages = new int[codePageList.size()];

        for (int i = 0; i < codePageList.size(); i++) {
            codePages[i] = codePageList.get(i).intValue();
        }

        return codePages;
    }

    private static String[] getSystemPropertyKeys(final String keyPrefix) {
        final List<String> propertyKeyList = new ArrayList<String>();
        final Properties systemProperties = System.getProperties();

        for (final Iterator<Object> i = systemProperties.keySet().iterator(); i.hasNext();) {
            final String key = (String) i.next();

            if (key.startsWith(keyPrefix + ".")) //$NON-NLS-1$
            {
                final String keyConfigurationPart = key.substring(keyPrefix.length() + 2);
                propertyKeyList.add(keyConfigurationPart);
            }
        }

        return propertyKeyList.toArray(new String[propertyKeyList.size()]);
    }

    /**
     * Translates the specified code page into an encoding. If the code page can
     * not be translated, an {@link UnknownCodePageException} is thrown.
     * Otherwise, this method returns a charset name that is supported by this
     * Java virtual machine.
     *
     * @param codePage
     *        a code page to translate
     * @return a valid encoding for the code page (never <code>null</code>)
     * @throws UnknownCodePageException
     */
    public static String getEncoding(final int codePage) {
        return getEncoding(codePage, true, true);
    }

    /**
     * <p>
     * Attempts to translate the specified code page into an encoding.
     * </p>
     *
     * <p>
     * If the code page does not map to an encoding, the <code>mustExist</code>
     * parameter specifies the policy. If <code>mustExist</code> is
     * <code>true</code>, an {@link UnknownCodePageException} is thrown.
     * Otherwise, <code>null</code> is returned.
     * </p>
     *
     * <p>
     * If the code page maps to an encoding that is not supported by this Java
     * virtual machine, the <code>mustBeSupportedCharset</code> specifies the
     * policy. If <code>mustBeSupportedCharset</code> is <code>true</code>, an
     * {@link UnknownCodePageException} is thrown. Otherwise, the non-supported
     * encoding is returned.
     * </p>
     *
     * @param codePage
     *        a code page to translate
     * @param mustExist
     *        if <code>true</code>, the code page must map to a known encoding
     * @param mustBeSupportedCharset
     *        if <code>true</code>, the code page must map to a supported
     *        charset in this Java virtual machine
     * @return an encoding for the code page, which may be unsupported if
     *         <code>mustBeSupportedCharset</code> is <code>false</code> and may
     *         be <code>null</code> if <code>mustExist</code> is
     *         <code>false</code>
     * @throws UnknownCodePageException
     */
    public static String getEncoding(
        final int codePage,
        final boolean mustExist,
        final boolean mustBeSupportedCharset) {
        final String[] charsetNames = getCharsetNamesForCodePage(codePage, mustExist);

        if (charsetNames == null) {
            return null;
        }

        for (int i = 0; i < charsetNames.length; i++) {
            if (tryCharsetIsSupported(charsetNames[i])) {
                return charsetNames[i];
            }
        }

        if (!mustBeSupportedCharset) {
            return charsetNames[0];
        }

        throw new UnknownCodePageException(codePage);
    }

    /**
     * Translates the specified code page into a {@link Charset}. If the code
     * page can not be translated, an {@link UnknownCodePageException} is
     * thrown.
     *
     * @param codePage
     *        a code page to translate
     * @return a {@link Charset} for the code page (never <code>null</code>)
     * @throws UnknownCodePageException
     */
    public static Charset getCharset(final int codePage) {
        return getCharset(codePage, true);
    }

    /**
     * <p>
     * Translates the specified code page into a {@link Charset}.
     * </p>
     *
     * <p>
     * If the code page does not map to an {@link Charset}, the
     * <code>mustExist</code> parameter specifies the policy. If
     * <code>mustExist</code> is <code>true</code>, an
     * {@link UnknownCodePageException} is thrown. Otherwise, <code>null</code>
     * is returned.
     * </p>
     *
     * @param codePage
     *        a code page to translate
     * @param mustExist
     *        if <code>true</code>, the code page must map to a {@link Charset}
     * @return a {@link Charset} for the code page, which may be
     *         <code>null</code> if <code>mustExist</code> is <code>false</code>
     * @throws UnknownCodePageException
     */
    public static Charset getCharset(final int codePage, final boolean mustExist) {
        final String[] charsetNames = getCharsetNamesForCodePage(codePage, mustExist);

        if (charsetNames == null) {
            return null;
        }

        for (int i = 0; i < charsetNames.length; i++) {
            if (tryCharsetIsSupported(charsetNames[i])) {
                final Charset charset = tryCharsetForName(charsetNames[i]);
                if (charset != null) {
                    return charset;
                }
            }
        }

        if (!mustExist) {
            return null;
        }

        throw new UnknownCodePageException(codePage);
    }

    /**
     * Translates the specified encoding into a code page. If the encoding can
     * not be translated, an {@link UnknownEncodingException} is thrown.
     *
     * @param encoding
     *        an encoding to translate (must not be <code>null</code>)
     * @return a code page appropriate for passing to TFS
     * @throws UnknownEncodingException
     */
    public static int getCodePage(final String encoding) {
        return getCodePage(encoding, true);
    }

    /**
     * <p>
     * Translates the specified encoding into a code page.
     * </p>
     *
     * <p>
     * If the encoding does not map to a code page, the <code>mustExist</code>
     * parameter specifies the policy. If <code>mustExist</code> is
     * <code>true</code>, an {@link UnknownEncodingException} is thrown.
     * Otherwise, <code>0</code> is returned. The value 0 is not a valid code
     * page value for TFS.
     * </p>
     *
     * @param encoding
     *        an encoding to translate (must not be <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, the encoding must map to a code page
     * @return a code page for the encoding, which may be <code>0</code> if
     *         <code>mustExist</code> is <code>false</code>
     * @throws UnknownEncodingException
     */
    public static int getCodePage(final String encoding, final boolean mustExist) {
        Check.notNull(encoding, "encoding"); //$NON-NLS-1$

        Integer codePage = tryLookupSystemPropertyCodePage(encoding);
        if (codePage != null) {
            return codePage.intValue();
        }

        final Charset charset = tryCharsetForName(encoding);

        if (charset != null) {
            final String name = charset.name();

            if (!encoding.equalsIgnoreCase(name)) {
                codePage = tryLookupSystemPropertyCodePage(name);
                if (codePage != null) {
                    return codePage.intValue();
                }
            }

            final Set<String> aliases = charset.aliases();
            for (final Iterator<String> it = aliases.iterator(); it.hasNext();) {
                final String alias = it.next();
                if (encoding.equalsIgnoreCase(alias)) {
                    continue;
                }
                codePage = tryLookupSystemPropertyCodePage(alias);
                if (codePage != null) {
                    return codePage.intValue();
                }
            }
        }

        codePage = CodePageData.getCodePage(encoding);
        if (codePage != null) {
            return codePage.intValue();
        }

        if (charset != null) {
            final String name = charset.name();
            if (!encoding.equalsIgnoreCase(name)) {
                codePage = CodePageData.getCodePage(charset.name());
                if (codePage != null) {
                    return codePage.intValue();
                }
            }
        }

        if (mustExist) {
            if (charset != null) {
                throw new UnknownEncodingException(charset);
            }

            throw new UnknownEncodingException(encoding);
        }

        return 0;
    }

    /**
     * Translates the specified {@link Charset} into a code page. If the
     * {@link Charset} can not be translated, an
     * {@link UnknownEncodingException} is thrown.
     *
     * @param charset
     *        a {@link Charset} to translate (must not be <code>null</code>)
     * @return a code page appropriate for passing to TFS
     * @throws UnknownEncodingException
     */
    public static int getCodePage(final Charset charset) {
        return getCodePage(charset, true);
    }

    /**
     * <p>
     * Translates the specified {@link Charset} into a code page.
     * </p>
     *
     * <p>
     * If the {@link Charset} does not map to a code page, the
     * <code>mustExist</code> parameter specifies the policy. If
     * <code>mustExist</code> is <code>true</code>, an
     * {@link UnknownEncodingException} is thrown. Otherwise, <code>0</code> is
     * returned. The value 0 is not a valid code page value for TFS.
     * </p>
     *
     * @param charset
     *        a {@link Charset} to translate (must not be <code>null</code>)
     * @param mustExist
     *        if <code>true</code>, the {@link Charset} must map to a code page
     * @return a code page for the {@link Charset}, which may be <code>0</code>
     *         if <code>mustExist</code> is <code>false</code>
     * @throws UnknownEncodingException
     */
    public static int getCodePage(final Charset charset, final boolean mustExist) {
        Check.notNull(charset, "charset"); //$NON-NLS-1$

        Integer codePage = tryLookupSystemPropertyCodePage(charset.name());

        if (codePage != null) {
            return codePage.intValue();
        }

        final Set<String> aliases = charset.aliases();
        for (final Iterator<String> it = aliases.iterator(); it.hasNext();) {
            final String alias = it.next();
            codePage = tryLookupSystemPropertyCodePage(alias);
            if (codePage != null) {
                return codePage.intValue();
            }
        }

        codePage = CodePageData.getCodePage(charset.name());
        if (codePage != null) {
            return codePage.intValue();
        }

        if (mustExist) {
            throw new UnknownEncodingException(charset);
        }

        return 0;
    }

    /**
     * An exception thrown to indicate that a code page specified as an argument
     * to a {@link CodePageMapping} method was unknown to that class. This
     * indicates that there was no configuration data available for the
     * specified code page, after looking at both hard-coded data and
     * configuration data specified at runtime. This exception is also used if
     * configuration data was available for the code page, but the charset that
     * the code page mapped to was not supported by this Java virtual machine.
     */
    public static class UnknownCodePageException extends TECoreException {
        private final int codePage;

        /**
         * Creates a new {@link UnknownCodePageException} for the specified code
         * page.
         *
         * @param codePage
         *        the code page that triggered this exception
         */
        public UnknownCodePageException(final int codePage) {
            super(MessageFormat.format("The code page {0} is unknown", Integer.toString(codePage))); //$NON-NLS-1$
            this.codePage = codePage;
        }

        /**
         * @return the code page that was unknown
         */
        public int getCodePage() {
            return codePage;
        }
    }

    /**
     * An exception thrown to indicate that either a {@link Charset} or the name
     * of an encoding specified as an argument to a {@link CodePageMapping}
     * method was unknown to that class. This indicates that there was no
     * configuration data available for the specified encoding, after looking at
     * both hard-coded data and configuration data specified at runtime.
     */
    public static class UnknownEncodingException extends TECoreException {
        private final Charset charset;
        private final String encoding;

        /**
         * Creates a new {@link UnknownEncodingException} for the specified
         * {@link Charset}.
         *
         * @param charset
         *        a {@link Charset} that was unknown to a
         *        {@link CodePageMapping} method (must not be <code>null</code>)
         */
        public UnknownEncodingException(final Charset charset) {
            super(MessageFormat.format("The charset \"{0}\" is unknown", charset.name())); //$NON-NLS-1$

            Check.notNull(charset, "charset"); //$NON-NLS-1$
            this.charset = charset;
            encoding = null;
        }

        /**
         * Creates a new {@link UnknownEncodingException} for the specified
         * encoding.
         *
         * @param encoding
         *        an encoding that was unknown to a {@link CodePageMapping}
         *        method (must not be <code>null</code>)
         */
        public UnknownEncodingException(final String encoding) {
            super(MessageFormat.format("The encoding \"{0}\" is unknown", encoding)); //$NON-NLS-1$

            Check.notNull(encoding, "encoding"); //$NON-NLS-1$
            charset = null;
            this.encoding = encoding;
        }

        /**
         * @return the encoding that was unknown
         */
        public String getEncoding() {
            if (charset != null) {
                return charset.name();
            }
            return encoding;
        }

        /**
         * @return the {@link Charset} that was unknown, or <code>null</code>
         */
        public Charset getCharset() {
            return charset;
        }
    }

    /**
     * Attempts to produce an array of charset names for the specified code
     * page. If successful, the array of charset names should be tried in
     * sequence when attempting to obtain a {@link Charset} object.
     *
     * @param codePage
     *        the code page to get charset names for
     * @param mustExist
     *        if <code>true</code> and no charset names were found for the
     *        specified code page, throw a {@link UnknownCodePageException}
     * @return an array of charset names, or <code>null</code> if the code page
     *         was not known and <code>mustExist</code> was <code>false</code>
     */
    private static String[] getCharsetNamesForCodePage(final int codePage, final boolean mustExist) {
        final List<String> charsetNames = new ArrayList<String>();

        final String systemPropertyName = "codePageMapping." + codePage; //$NON-NLS-1$

        final String systemPropertyValue = System.getProperty(systemPropertyName);
        if (systemPropertyValue != null) {
            final String[] systemPropertyCharsetNames = systemPropertyValue.split(","); //$NON-NLS-1$
            for (int i = 0; i < systemPropertyCharsetNames.length; i++) {
                final String charsetName = systemPropertyCharsetNames[i].trim();
                if (charsetName.length() > 0 && !charsetNames.contains(charsetName)) {
                    charsetNames.add(charsetName);
                }
            }
        }

        final String[] configuredCharsetNames = CodePageData.getCharsetNames(codePage);

        if (configuredCharsetNames != null) {
            for (int i = 0; i < configuredCharsetNames.length; i++) {
                if (!charsetNames.contains(configuredCharsetNames[i])) {
                    charsetNames.add(configuredCharsetNames[i]);
                }
            }
        }

        if (charsetNames.size() == 0) {
            if (mustExist) {
                throw new UnknownCodePageException(codePage);
            }

            return null;
        }

        return charsetNames.toArray(new String[charsetNames.size()]);
    }

    /**
     * Attempts to produce a {@link Charset} object for the specified charset
     * name. If any errors occur, returns <code>null</code>.
     *
     * @param charsetName
     *        a charset name (must not be <code>null</code>)
     * @return a {@link Charset} object or <code>null</code>
     */
    private static Charset tryCharsetForName(final String charsetName) {
        try {
            return Charset.forName(charsetName);
        } catch (final IllegalCharsetNameException e) {
            return null;
        } catch (final UnsupportedCharsetException e) {
            return null;
        }
    }

    /**
     * Determines if the specified charset name is supported by calling the
     * {@link Charset#isSupported(String)} method. Returns the result of that
     * method, or <code>false</code> if that method throws an
     * {@link IllegalCharsetNameException}.
     *
     * @param charsetName
     *        a charset name (must not be <code>null</code>)
     * @return <code>true</code> if the charset name is supported
     */
    private static boolean tryCharsetIsSupported(final String charsetName) {
        try {
            return Charset.isSupported(charsetName);
        } catch (final IllegalCharsetNameException e) {
            return false;
        }
    }

    /**
     * Attempts to produce a code page for the specified charset name by
     * checking to see if a system property (charsetMapping.<i>charsetName</i>)
     * has been set to explicitly assign a code page to the charset name. The
     * name is not made into a canonical charset name, and no aliases of the
     * name are tried.
     *
     * @param charsetName
     *        a charset name to look up
     * @return a code page for the specified charset name, or <code>null</code>
     *         if the name did not map to a code page
     */
    private static Integer tryLookupSystemPropertyCodePage(final String charsetName) {
        String value = System.getProperty("charsetMapping." + charsetName); //$NON-NLS-1$
        if (value == null) {
            value = System.getProperty("charsetMapping." + charsetName.toLowerCase()); //$NON-NLS-1$
        }

        if (value == null) {
            return null;
        }

        try {
            return Integer.valueOf(value);
        } catch (final NumberFormatException ex) {
            return null;
        }
    }
}
