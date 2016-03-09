// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.htmlfilter;

/**
 * Writes HTML text, tags, and attributes. Designed so implementations can be
 * used to fill single output buffer or split tag and text into two output
 * buffers.
 *
 * Copied from Web Access.
 *
 * @threadsafety unknown
 */
interface IHTMLFilterWriter {
    void writeText(String s, int offs, int len);

    void writeTag(String s, int offs, int len, String tag, boolean endTag);

    void writeEndOfTag(String s, int offs, int len, String tag);

    void writeAttribute(String s, int offs, int len, String tag, String attr, int i1, int i2);
}