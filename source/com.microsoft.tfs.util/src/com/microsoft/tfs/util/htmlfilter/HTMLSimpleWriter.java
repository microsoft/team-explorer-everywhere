// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.htmlfilter;

/**
 * Simple {@link IHTMLFilterWriter} which collects output in a big
 * {@link StringBuilder}.
 *
 * Copied from Web Access.
 *
 * @threadsafety thread-compatible
 */
class HTMLSimpleWriter implements IHTMLFilterWriter {
    // String buffer to fill
    private final StringBuilder m_str = new StringBuilder();

    // constructor
    public HTMLSimpleWriter() {
    }

    // convert to string
    @Override
    public String toString() {
        return m_str.toString();
    }

    // implement writer interface
    @Override
    public void writeText(final String s, final int offs, final int len) {
        m_str.append(s, offs, offs + len);
    }

    @Override
    public void writeTag(final String s, final int offs, final int len, final String tag, final boolean endTag) {
        m_str.append(s, offs, offs + len);
    }

    @Override
    public void writeEndOfTag(final String s, final int offs, final int len, final String tag) {
        m_str.append(s, offs, offs + len);
    }

    @Override
    public void writeAttribute(
        final String s,
        final int offs,
        final int len,
        final String tag,
        final String attr,
        final int i1,
        final int i2) {
        m_str.append(s, offs, offs + len);
    }
}
