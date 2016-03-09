// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.microsoft.tfs.core.Messages;

/**
 * {@link CodePageData} reads the code page mappings that are stored in
 * codepages.xml. This class loads codepages.xml as a resource, parses it, and
 * stores the data. It is intended for use only by the {@link CodePageMapping}
 * class.
 */
class CodePageData {
    private static final Log log = LogFactory.getLog(CodePageData.class);

    private static final Object lock = new Object();
    private static boolean initialized = false;
    private static Map codePageToCharsetNames = new HashMap();
    private static Map charsetNameToCodePage = new HashMap();

    /**
     * Gets the array of charset names that are configured and can be mapped to
     * a code page.
     *
     * @return the charset names that can be resolved to code pages
     */
    static String[] getCharsetNames() {
        synchronized (lock) {
            if (!initialized) {
                initialize();
            }

            final Set charsetNameSet = charsetNameToCodePage.keySet();
            return (String[]) charsetNameSet.toArray(new String[charsetNameSet.size()]);
        }
    }

    /**
     * Gets the array of code pages that are configured and can be mapped to a
     * charset
     *
     * @return the code pages that can be resolved to charsets
     */
    static Integer[] getCodePages() {
        synchronized (lock) {
            if (!initialized) {
                initialize();
            }

            final Set codePageSet = codePageToCharsetNames.keySet();
            return (Integer[]) codePageSet.toArray(new Integer[codePageSet.size()]);
        }
    }

    /**
     * Gets the array of canonical charset names for the specified code page.
     *
     * @param codePage
     *        the code page to look up
     * @return the canonical charset names for the code page, or
     *         <code>null</code> if the code page is unknown
     */
    static String[] getCharsetNames(final int codePage) {
        final Integer key = new Integer(codePage);

        synchronized (lock) {
            if (!initialized) {
                initialize();
            }

            return (String[]) codePageToCharsetNames.get(key);
        }
    }

    /**
     * Gets the code page for the specified canonical charset name.
     *
     * @param charsetName
     *        the charset name to look up (must not be <code>null</code>)
     * @return the code page, <code>null</code> if the charset is unknown
     */
    static Integer getCodePage(final String charsetName) {
        synchronized (lock) {
            if (!initialized) {
                initialize();
            }

            return (Integer) charsetNameToCodePage.get(charsetName.toLowerCase());
        }
    }

    private static void initialize() {
        initialized = true;

        final InputStream inputStream = CodePageData.class.getResourceAsStream("codepages.xml"); //$NON-NLS-1$

        if (inputStream == null) {
            log.error("Unable to load the codepages.xml resource"); //$NON-NLS-1$
            return;
        }

        try {
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            final InputSource inputSource = new InputSource(inputStream);
            parser.parse(inputSource, new CodePagesHandler());
        } catch (final Exception e) {
            log.error(Messages.getString("CodePageData.ErrorDuringParsing"), e); //$NON-NLS-1$
        } finally {
            try {
                inputStream.close();
            } catch (final IOException e) {
            }
        }
    }

    private static class CodePagesHandler extends DefaultHandler {
        private int currentCodePage = -1;
        private final List currentCharsetNames = new ArrayList();

        @Override
        public void startElement(
            final String uri,
            final String localName,
            final String qName,
            final Attributes attributes) throws SAXException {
            if ("codePage".equals(qName)) //$NON-NLS-1$
            {
                try {
                    currentCodePage = Integer.parseInt(attributes.getValue("value")); //$NON-NLS-1$
                    currentCharsetNames.clear();
                } catch (final NumberFormatException ex) {

                }
            } else if ("charset".equals(qName) && currentCodePage != -1) //$NON-NLS-1$
            {
                final String name = attributes.getValue("name"); //$NON-NLS-1$

                if (name != null) {
                    currentCharsetNames.add(name);

                    final String sReverseMapping = attributes.getValue("reverseMapping"); //$NON-NLS-1$
                    final boolean reverseMapping = !"false".equalsIgnoreCase(sReverseMapping); //$NON-NLS-1$

                    if (reverseMapping) {
                        if (charsetNameToCodePage.containsKey(name.toLowerCase())) {
                            log.warn(
                                MessageFormat.format(
                                    "charset name [{0}] is mapped to more than one code page: {1} {2}", //$NON-NLS-1$
                                    name,
                                    charsetNameToCodePage.get(name).toString(),
                                    Integer.toString(currentCodePage)));
                        } else {
                            charsetNameToCodePage.put(name.toLowerCase(), new Integer(currentCodePage));
                        }
                    }
                }
            }
        }

        @Override
        public void endElement(final String uri, final String localName, final String qName) throws SAXException {
            if ("codePage".equals(qName) && currentCodePage != -1) //$NON-NLS-1$
            {
                final Integer codePageKey = new Integer(currentCodePage);
                if (codePageToCharsetNames.containsKey(codePageKey)) {
                    log.warn(MessageFormat.format(
                        "code page [{0}] is duplicated in the mappings", //$NON-NLS-1$
                        Integer.toString(currentCodePage)));
                } else if (currentCharsetNames.size() == 0) {
                    log.warn(MessageFormat.format(
                        "code page [{0}] had no charset mappings", //$NON-NLS-1$
                        Integer.toString(currentCodePage)));
                } else {
                    final String[] charsetNames =
                        (String[]) currentCharsetNames.toArray(new String[currentCharsetNames.size()]);
                    codePageToCharsetNames.put(codePageKey, charsetNames);
                }

                currentCodePage = -1;
            }
        }
    }
}
