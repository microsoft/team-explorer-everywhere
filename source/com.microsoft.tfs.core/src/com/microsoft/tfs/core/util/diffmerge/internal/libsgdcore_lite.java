// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.diffmerge.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.text.MessageFormat;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.TypesafeEnum;

// ////////////////////////////////////////////////////////////////
// LibSgdCore_Diff23.cs
// all of the diff3 stuff formerly in VaultClientOperationsLib/diff3.cs
// plus stuff to do 2-way diffs to replace SGDiff/sgdiff.cs.
//
// ALL Comments were left exactly as they were in the original C# file
// ////////////////////////////////////////////////////////////////

/*
 *
 * Portions of this code were derived from code which originally came from
 * Subversion. The copyright appears below.
 *
 * Copyright (c) 2000-2004 Collab.Net. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 * include the following acknowledgment: "This product includes software
 * developed by Collab.Net (http://www.Collab.Net/)." Alternately, this
 * acknowledgment may appear in the software itself, if and wherever such
 * third-party acknowledgments normally appear.
 *
 * 4. The hosted project names must not be used to endorse or promote products
 * derived from this software without prior written permission. For written
 * permission, please contact info@collab.net.
 *
 * 5. Products derived from this software may not use the "Tigris" name nor may
 * "Tigris" appear in their names without prior written permission of
 * Collab.Net.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COLLAB.NET
 * OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of Collab.Net.
 */

public class libsgdcore_lite {
    private static final Log log = LogFactory.getLog(libsgdcore_lite.class);

    final static class NDX {
        public final static int NDX_UNSPECIFIED = -1;
        public final static int NDX_ORIGINAL = 0;
        public final static int NDX_MODIFIED = 1;
        public final static int NDX_LATEST = 2;
    }

    final static class file_output_type_e extends TypesafeEnum {
        public final static file_output_type_e file_output_skip = new file_output_type_e(1);
        public final static file_output_type_e file_output_normal = new file_output_type_e(2);

        private file_output_type_e(final int type) {
            super(type);
        }
    }

    final static class file_output_baton_t {
        public Writer output_file;

        public String[] path = new String[3];

        public int[] current_line = new int[3];

        private String _eol = System.getProperty("line.separator"); //$NON-NLS-1$
        private boolean trailingEol = true;

        public String[][] buffer = new String[3][];
        public int[] endp = new int[3];
        public int[] curp = new int[3];

        public String conflict_modified;
        public String conflict_original;
        public String conflict_separator;
        public String conflict_latest;
        public boolean display_original_in_conflict;
        public boolean display_resolved_conflicts;

        public String getEOL() {
            return _eol;
        }

        public void setEOL(final String eol) {
            _eol = eol;
        }

        public boolean getTrailingEol() {
            return trailingEol;
        }

        public void setTrailingEol(final boolean trailingEol) {
            this.trailingEol = trailingEol;
        }
    }

    final static class node_t implements Comparable {
        public node_t parent;
        public node_t left;
        public node_t right;

        public file_token_t token;

        @Override
        public int compareTo(final Object obj) {
            return token.compareTo(((node_t) obj).token);
        }
    }

    final static class tree_t {
        public node_t root;
    }

    final static class position_t {
        public position_t next;
        public position_t prev; // SourceGear Added
        public node_t node;
        public int offset;
        public int maxoffset;
    }

    final static class lcs_t {
        public lcs_t next;
        public position_t[] position = new position_t[2];
        public int length;
    }

    final static class snake_t {
        public int y;
        public lcs_t lcs;
        public position_t[] position = new position_t[2];
    }

    // ////////////////////////////////////////////////////////////////
    // a "file_token" represented the data in a "line" of a file that
    // we load and run the diff/diff3 algorithm on.

    final static class file_token_t implements Comparable {
        public int length;
        public String line;

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof file_token_t)) {
                return false;
            }

            return (line.equals(((file_token_t) obj).line));
        }

        @Override
        public int compareTo(final Object obj) {
            return (line.compareTo(((file_token_t) obj).line));
        }
    }

    final static class SGD_MergeResult extends TypesafeEnum {
        public static final SGD_MergeResult SGD_MergeResult_Identical = new SGD_MergeResult(0);
        public static final SGD_MergeResult SGD_MergeResult_Differences = new SGD_MergeResult(1);
        public static final SGD_MergeResult SGD_MergeResult_Conflicts = new SGD_MergeResult(2);
        public static final SGD_MergeResult SGD_MergeResult_Error = new SGD_MergeResult(3);

        private SGD_MergeResult(final int result) {
            super(result);
        }
    }

    public final static class type_e extends TypesafeEnum {
        public static final type_e type_common = new type_e(0);
        public static final type_e type_diff_modified = new type_e(1);
        public static final type_e type_diff_latest = new type_e(2);
        public static final type_e type_diff_common = new type_e(3);
        public static final type_e type_conflict = new type_e(4);
        public static final type_e __nr_types__ = new type_e(5);

        private type_e(final int type) {
            super(type);
        }
    }

    final static class svn_diff_datasource_e extends TypesafeEnum {
        /* The oldest form of the data. */
        public static final svn_diff_datasource_e svn_diff_datasource_original = new svn_diff_datasource_e(0);

        /* The same data , but potentially changed by the user. */
        public static final svn_diff_datasource_e svn_diff_datasource_modified = new svn_diff_datasource_e(1);

        /*
         * The latest version of the data, possibly different than the user
         * modified version.
         */
        public static final svn_diff_datasource_e svn_diff_datasource_latest = new svn_diff_datasource_e(2);

        /* The common ancestor of original and modified. */
        public static final svn_diff_datasource_e svn_diff_datasource_ancestor = new svn_diff_datasource_e(3);

        private svn_diff_datasource_e(final int source) {
            super(source);
        }
    }

    public final static class svn_diff_t {
        public svn_diff_t next;
        public type_e type;
        public int[] m_start = new int[3]; // see NDX_ above
        public int[] m_length = new int[3]; // see NDX_ above
        public svn_diff_t resolved_diff;
    }

    // TODO create an interface for output_fns_t. then rename this instance
    // TODO to be the diff3_output_fns. in the original svn code, there were
    // TODO other instances for 2-way diffing - such as normal and unified
    // TODO output modes. i haven't bothered with this because we don
    // currently
    // TODO need to dump output from 2-way diffs.

    final static class output_fns_t {
        private final file_output_baton_t _baton;

        public output_fns_t(final file_output_baton_t baton) {
            _baton = baton;
        }

        public void output_common(
            final int original_start,
            final int original_length,
            final int modified_start,
            final int modified_length,
            final int latest_start,
            final int latest_length,
            final boolean lastDiff) {
            file_output_hunk(0, original_start, original_length, lastDiff);
        }

        public void output_diff_modified(
            final int original_start,
            final int original_length,
            final int modified_start,
            final int modified_length,
            final int latest_start,
            final int latest_length,
            final boolean lastDiff) {
            file_output_hunk(1, modified_start, modified_length, lastDiff);
        }

        public void output_diff_latest(
            final int original_start,
            final int original_length,
            final int modified_start,
            final int modified_length,
            final int latest_start,
            final int latest_length,
            final boolean lastDiff) {
            file_output_hunk(2, latest_start, latest_length, lastDiff);
        }

        public void output_diff_common(
            final int original_start,
            final int original_length,
            final int modified_start,
            final int modified_length,
            final int latest_start,
            final int latest_length,
            final boolean lastDiff) {
            file_output_hunk(1, modified_start, modified_length, lastDiff);
        }

        public void output_conflict(
            final int original_start,
            final int original_length,
            final int modified_start,
            final int modified_length,
            final int latest_start,
            final int latest_length,
            final svn_diff_t resolved_diff,
            final boolean lastDiff) {
            if (resolved_diff != null && _baton.display_resolved_conflicts) {
                libsgdcore_lite.svn_diff_output(resolved_diff, this);
            }

            try {
                _baton.output_file.write(_baton.conflict_modified);
                _baton.output_file.write(_baton.getEOL());
            } catch (final Exception ex) {
                throw new RuntimeException("error writing file", ex); //$NON-NLS-1$
            }

            file_output_hunk(1, modified_start, modified_length, false);

            if (_baton.display_original_in_conflict) {
                try {
                    _baton.output_file.write(_baton.conflict_original);
                    _baton.output_file.write(_baton.getEOL());
                } catch (final Exception ex) {
                    throw new RuntimeException("error writing file", ex); //$NON-NLS-1$
                }

                file_output_hunk(0, original_start, original_length, false);
            }

            try {
                _baton.output_file.write(_baton.conflict_separator);
                _baton.output_file.write(_baton.getEOL());
            } catch (final Exception ex) {
                throw new RuntimeException("error writing file", ex); //$NON-NLS-1$
            }

            file_output_hunk(2, latest_start, latest_length, false);

            try {
                _baton.output_file.write(_baton.conflict_latest);
                _baton.output_file.write(_baton.getEOL());
            } catch (final Exception ex) {
                throw new RuntimeException("error writing file", ex); //$NON-NLS-1$
            }
        }

        void file_output_line(final file_output_type_e type, final int idx, final boolean lastLine) {
            final int curp = _baton.curp[idx];
            final int endp = _baton.endp[idx];
            final int current_line = _baton.current_line[idx];

            try {
                /* Lazily update the current line even if we're at EOF. */
                _baton.current_line[idx]++;

                if (curp == endp) {
                    return;
                }

                if (type != file_output_type_e.file_output_skip) {
                    final String s = _baton.buffer[idx][current_line]; // .TrimEnd();

                    // _baton.output_file.WriteLine(s);
                    _baton.output_file.write(s);

                    if (lastLine == false || _baton.getTrailingEol() == true) {
                        _baton.output_file.write(_baton.getEOL());
                    }
                }

                _baton.curp[idx]++;
            } catch (final Exception ex) {
                throw new RuntimeException("error writing file", ex); //$NON-NLS-1$
            }
        }

        private void file_output_hunk(final int idx, int target_line, final int target_length, final boolean lastDiff) {
            /* Skip lines until we are at the start of the changed range */
            while (_baton.current_line[idx] < target_line) {
                file_output_line(file_output_type_e.file_output_skip, idx, false);
            }

            target_line += target_length;

            while (_baton.current_line[idx] < target_line) {
                final boolean lastLine = (lastDiff && _baton.current_line[idx] + 1 == target_line);

                file_output_line(file_output_type_e.file_output_normal, idx, lastLine);
            }
        }
    }

    final static class file_baton_t {
        /* TODO: why are these [4] rather than [3] ? */
        public String[] path = new String[4];
        public String[][] buffer = new String[4][];
        public int[] curp = new int[4];
        public int[] endp = new int[4];
        public file_token_t token;
        public boolean reuse_token;
        public int options;
        public String[] eol = new String[4];
        public Charset[] enc = new Charset[4];
        public boolean[] hasTrailingNewline = {
            true,
            true,
            true,
            true
        };

        public static String[] GetLines(
            final String filename,
            final int fileTag,
            final Charset[] enc,
            final String[] eol,
            final boolean[] hasTrailingEol) {
            int windows = 0, unix = 0, mac = 0;

            try {
                /*
                 * Create an instance of StreamReader to read from a file. The
                 * using statement also closes the StreamReader.
                 *
                 * WARNING: setting the third argument to 'true' causes
                 * StreamReader() to sniff for a Unicode BOM, remove it from the
                 * buffer for the first line, and override the character
                 * encoding actually used.
                 */
                final FileInputStream fis = new FileInputStream(filename);
                InputStreamReader isr;

                if (enc[0] != null) {
                    final CharsetDecoder decoder = enc[0].newDecoder();
                    decoder.onMalformedInput(CodingErrorAction.REPORT);
                    decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

                    isr = new InputStreamReader(fis, decoder);
                } else {
                    isr = new InputStreamReader(fis);
                }

                final BufferedReader sr = new BufferedReader(isr);

                /*
                 * Assume that the file is terminated with a newline unless we
                 * discover otherwise on read.
                 */
                hasTrailingEol[0] = true;

                try {
                    final ArrayList lines = new ArrayList();
                    String line;
                    final String[] newline = new String[1];

                    while ((line = readline(sr, newline)) != null) {
                        lines.add(line);

                        if (newline[0] == null) {
                            /*
                             * By definition, the only way we could *not* have a
                             * newline character on a line is if it is the last
                             * line in the file. Thus, if there's no newline,
                             * the file does not end with a trailing newline
                             */
                            hasTrailingEol[0] = false;
                        } else if (newline[0].equals("\r\n")) //$NON-NLS-1$
                        {
                            windows++;
                        } else if (newline[0].equals("\n")) //$NON-NLS-1$
                        {
                            unix++;
                        } else if (newline[0].equals("\r")) //$NON-NLS-1$
                        {
                            mac++;
                        }
                    }

                    try {
                        enc[0] = Charset.forName(isr.getEncoding());
                    } catch (final Throwable e) {
                        enc[0] = getDefaultCharset();
                    }

                    if (windows > unix && windows > mac) {
                        eol[0] = "\r\n"; //$NON-NLS-1$
                    } else if (unix > windows && unix > mac) {
                        eol[0] = "\n"; //$NON-NLS-1$
                    } else if (mac > windows && mac > unix) {
                        eol[0] = "\r"; //$NON-NLS-1$
                    } else {
                        eol[0] = System.getProperty("line.separator"); //$NON-NLS-1$
                    }

                    return (String[]) lines.toArray(new String[lines.size()]);
                } finally {
                    sr.close();
                }
            } catch (final MalformedInputException e) {
                log.info("The file " + filename + " could not be read with the encoding " + getEncodingName(enc[0]), e); //$NON-NLS-1$ //$NON-NLS-2$
                throw new VersionControlException(MessageFormat.format(
                    FileEncoding.ENCODING_ERROR_MESSAGE_FORMAT,
                    getFileTagName(fileTag),
                    LocalPath.getFileName(filename),
                    getEncodingName(enc[0])), e);
            } catch (final UnmappableCharacterException e) {
                log.info("The file " + filename + " could not be read with the encoding " + getEncodingName(enc[0]), e); //$NON-NLS-1$ //$NON-NLS-2$
                throw new VersionControlException(MessageFormat.format(
                    FileEncoding.ENCODING_ERROR_MESSAGE_FORMAT,
                    getFileTagName(fileTag),
                    LocalPath.getFileName(filename),
                    getEncodingName(enc[0])), e);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to read file " + filename, e); //$NON-NLS-1$
            }
        }

        private static String getEncodingName(final Charset charset) {
            return charset != null ? charset.displayName() : getDefaultCharset().displayName();
        }

        private static String getFileTagName(final int fileTag) {
            switch (fileTag) {
                case 0:
                    return Messages.getString("libsgdcore.lite.OriginalFile"); //$NON-NLS-1$
                case 1:
                    return Messages.getString("libsgdcore.lite.ModifiedFile"); //$NON-NLS-1$
                case 2:
                    return Messages.getString("libsgdcore.lite.LatestFile"); //$NON-NLS-1$
                default:
                    return StringUtil.EMPTY;
            }
        }

        private static String readline(final BufferedReader reader, final String[] newline) throws IOException {
            final char[] cbuf = new char[1];
            final StringBuffer line = new StringBuffer();
            int readlen = 0;

            newline[0] = null;
            boolean lastCR = false;

            while (reader.read(cbuf, 0, 1) > 0) {
                readlen++;

                // line ending was a \r\n
                if (cbuf[0] == '\n' && lastCR == true) {
                    newline[0] = "\r\n"; //$NON-NLS-1$
                    break;
                }
                // line ending was a \r
                else if (lastCR == true) {
                    newline[0] = "\r"; //$NON-NLS-1$
                    reader.reset();
                    break;
                }
                // line ending was a \n
                else if (cbuf[0] == '\n') {
                    newline[0] = "\n"; //$NON-NLS-1$
                    break;
                } else if (cbuf[0] == '\r') {
                    // mark the stream here so that we can read the next char
                    // (in case it's not a \n)
                    reader.mark(1);
                    lastCR = true;
                } else {
                    lastCR = false;
                    line.append(cbuf[0]);
                }
            }

            // handle eof
            if (readlen == 0) {
                return null;
            }

            return line.toString();
        }

        // / <summary>
        // / Uses the actual character encoding of the three input files to
        // compute
        // / the best character encoding choice for the output. If all are
        // .Default,
        // / that is the result; if any are unicode/utf (because the input files
        // had
        // / a BOM), we pick one of them.
        // / </summary>
        // / <returns></returns>
        public Charset ComputeBestEncoding() {
            // if the user has changed the encoding, use ours
            if (enc[NDX.NDX_ORIGINAL] != null
                && enc[NDX.NDX_MODIFIED] != null
                && !enc[NDX.NDX_ORIGINAL].equals(enc[NDX.NDX_MODIFIED])) {
                return enc[NDX.NDX_MODIFIED];
            }

            // if the latest version has changed the encoding, use theirs
            if (enc[NDX.NDX_ORIGINAL] != null
                && enc[NDX.NDX_LATEST] != null
                && !enc[NDX.NDX_ORIGINAL].equals(enc[NDX.NDX_LATEST])) {
                return enc[NDX.NDX_LATEST];
            }

            if (enc[NDX.NDX_ORIGINAL] != null) {
                return enc[NDX.NDX_ORIGINAL];
            }

            return getDefaultCharset();
        }

        private static Charset getDefaultCharset() {
            try {
                final String defaultEncoding = System.getProperty("file.encoding"); //$NON-NLS-1$

                if (defaultEncoding != null && Charset.forName(defaultEncoding) != null) {
                    return Charset.forName(defaultEncoding);
                }
            } catch (final Exception e) {
            }

            try {
                return Charset.forName("UTF-8"); //$NON-NLS-1$
            } catch (final Exception e) {
                return null;
            }
        }

        public String ComputeBestLineEnding() {
            // if the user has changed the encoding, use ours
            if (eol[NDX.NDX_ORIGINAL] != null
                && eol[NDX.NDX_MODIFIED] != null
                && !eol[NDX.NDX_ORIGINAL].equals(eol[NDX.NDX_MODIFIED])) {
                return eol[NDX.NDX_MODIFIED];
            }

            // if the latest version has changed the encoding, use theirs
            if (eol[NDX.NDX_ORIGINAL] != null
                && eol[NDX.NDX_LATEST] != null
                && !eol[NDX.NDX_ORIGINAL].equals(eol[NDX.NDX_LATEST])) {
                return eol[NDX.NDX_LATEST];
            }

            if (eol[NDX.NDX_ORIGINAL] != null) {
                return eol[NDX.NDX_ORIGINAL];
            }

            return System.getProperty("line.separator"); //$NON-NLS-1$
        }

        public boolean ComputeBestTrailingEol() {
            // if the user has changed trailing eol strategy, use ours
            if (hasTrailingNewline[NDX.NDX_ORIGINAL] != hasTrailingNewline[NDX.NDX_MODIFIED]) {
                return hasTrailingNewline[NDX.NDX_MODIFIED];
            }

            // if the latest version has changed the eol strategy, use theirs
            if (hasTrailingNewline[NDX.NDX_ORIGINAL] != hasTrailingNewline[NDX.NDX_LATEST]) {
                return hasTrailingNewline[NDX.NDX_LATEST];
            }

            return hasTrailingNewline[NDX.NDX_ORIGINAL];
        }
    }

    final static class svn_diff_fns_t {
        private file_baton_t _baton;

        public svn_diff_fns_t(final file_baton_t baton) {
            _baton = baton;
        }

        public void datasource_open(final svn_diff_datasource_e datasource) {
            // this just checks if we ignore case, always case sensitive so
            // remove
            // file_load_fncmp(baton);

            final int idx = datasource.getValue();

            final Charset[] refenc = new Charset[1];
            final String[] refeol = new String[1];
            final boolean[] refHasTrailingNewline = new boolean[1];
            refenc[0] = _baton.enc[idx];

            final String[] fileLines =
                file_baton_t.GetLines(_baton.path[idx], idx, refenc, refeol, refHasTrailingNewline);

            if (_baton.enc[idx] == null) {
                _baton.enc[idx] = refenc[0];
            }

            if (_baton.eol[idx] == null) {
                _baton.eol[idx] = refeol[0];
            }

            _baton.hasTrailingNewline[idx] = refHasTrailingNewline[0];

            if (fileLines.length == 0) {
                _baton.buffer[idx] = null;
                _baton.curp[idx] = 0;
                _baton.endp[idx] = 0;
            } else {
                _baton.buffer[idx] = fileLines;
                _baton.curp[idx] = 0;
                _baton.endp[idx] = _baton.buffer[idx].length;
            }
        }

        public void file_token_discard(final Object token) {
            _baton.reuse_token = _baton.token.equals(token);
        }

        public void file_token_discard_all() {
            _baton = null;
        }

        // ref filetoken_t token
        public void datasource_get_next_token(final file_token_t[] token, final svn_diff_datasource_e datasource) {
            // file_baton_t file_baton = _baton;
            file_token_t file_token;
            int idx;
            int endp;
            int curp;
            // String eol;
            // int eollen;
            // int bAnyEOL;

            token[0] = null;

            idx = datasource.getValue();

            curp = _baton.curp[idx];
            endp = _baton.endp[idx];

            if (curp == endp) {
                return;
            }

            if (!_baton.reuse_token) {
                file_token = new file_token_t(); // (file_token_t
                // *)apr_palloc(file_baton
                // ->pool,
                // sizeof(*file_token));
                _baton.token = file_token;
            } else {
                file_token = _baton.token;
                _baton.reuse_token = false;
            }

            file_token.length = 0;

            final String line = _baton.buffer[idx][curp].replaceAll("(\r|\n)+$", ""); //$NON-NLS-1$ //$NON-NLS-2$

            file_token.line = line;
            file_token.length =
                line.length(); /*
                                * don't include the EOL in the token
                                */

            _baton.curp[idx]++; // file_baton.curp[idx] = (T_DATA_CHAR *)eol +
            // eollen;

            token[0] = file_token;
            file_token.line = line;
        }

        public int token_compare(final Object token1, final Object token2) {
            final file_token_t file_token1 = (file_token_t) token1;
            final file_token_t file_token2 = (file_token_t) token2;

            if (file_token1.length < file_token2.length) {
                return -1;
            } else if (file_token1.length > file_token2.length) {
                return 1;
            }

            return file_token1.compareTo(file_token2);
        }

        public Charset ComputeBestEncoding() {
            return _baton.ComputeBestEncoding();
        }

        public String ComputeBestLineEnding() {
            return _baton.ComputeBestLineEnding();
        }

        public boolean ComputeBestTrailingEol() {
            return _baton.ComputeBestTrailingEol();
        }
    }

    /*
     * BEGIN libsgdcore_lite
     */

    private final tree_t tree = new tree_t();

    public static SGD_MergeResult _do_merge(
        final String original,
        final Charset originalEnc,
        final String modified,
        final Charset modifiedEnc,
        final String latest,
        final Charset latestEnc,
        final String result,
        String eol) {
        // if the files are all identical or we have differences,
        // we generate a result file. if there are conflicts,
        // just give up.

        svn_diff_t pDiff = null;
        final Charset[] outEnc = new Charset[1];
        final String[] outEol = new String[1];
        final boolean[] trailingNewline = {
            true
        };

        try {
            pDiff = svn_diff3_file(
                original,
                originalEnc,
                modified,
                modifiedEnc,
                latest,
                latestEnc,
                outEnc,
                outEol,
                trailingNewline);

            if (eol == null) {
                eol = outEol[0];
            }
        } catch (final Exception e) {
            return SGD_MergeResult.SGD_MergeResult_Error;
        }

        if (contains_conflicts(pDiff) == true) {
            return SGD_MergeResult.SGD_MergeResult_Conflicts;
        }

        try {
            final File fi = new File(result);

            final FileOutputStream fos = new FileOutputStream(fi);
            final OutputStreamWriter encWriter = new OutputStreamWriter(fos, outEnc[0]);
            final BufferedWriter pFileResult = new BufferedWriter(encWriter);

            try {
                svn_diff3_file_output(
                    pFileResult,
                    pDiff,
                    eol,
                    trailingNewline[0],
                    original,
                    originalEnc,
                    modified,
                    modifiedEnc,
                    latest,
                    latestEnc,
                    null,
                    null,
                    null,
                    null,
                    true,
                    false);
            } finally {
                pFileResult.close();
            }
        } catch (final Exception e) {
            return SGD_MergeResult.SGD_MergeResult_Error;
        }

        final SGD_MergeResult mr = ((contains_diffs(pDiff) == true) ? SGD_MergeResult.SGD_MergeResult_Differences
            : SGD_MergeResult.SGD_MergeResult_Identical);

        return mr;
    }

    public static SGD_MergeResult _test_merge(
        final String original,
        final Charset originalEnc,
        final String modified,
        final Charset modifiedEnc,
        final String latest,
        final Charset latestEnc,
        final int[] numConflicts) {
        numConflicts[0] = 0;
        svn_diff_t pDiff = null;

        try {
            pDiff = svn_diff3_file(
                original,
                originalEnc,
                modified,
                modifiedEnc,
                latest,
                latestEnc,
                new Charset[1],
                new String[1],
                new boolean[1]);
        } catch (final Exception e) {
            return SGD_MergeResult.SGD_MergeResult_Error;
        }

        numConflicts[0] = count_conflicts(pDiff);
        if (numConflicts[0] > 0) {
            return SGD_MergeResult.SGD_MergeResult_Conflicts;
        }

        final SGD_MergeResult mr = ((contains_diffs(pDiff) == true) ? SGD_MergeResult.SGD_MergeResult_Differences
            : SGD_MergeResult.SGD_MergeResult_Identical);

        return mr;
    }

    public static void svn_diff3_file_output(
        final Writer output_file,
        final svn_diff_t diff,
        final String eol,
        final boolean trailingEol,
        final String original_path,
        final Charset original_charset,
        final String modified_path,
        final Charset modified_charset,
        final String latest_path,
        final Charset latest_charset,
        final String conflict_original,
        final String conflict_modified,
        final String conflict_latest,
        final String conflict_separator,
        final boolean display_original_in_conflict,
        final boolean display_resolved_conflicts) {
        final file_output_baton_t baton = new file_output_baton_t();
        baton.output_file = output_file;
        baton.path[0] = original_path;
        baton.path[1] = modified_path;
        baton.path[2] = latest_path;

        final Charset[] charsets = new Charset[3];
        charsets[0] = original_charset;
        charsets[1] = modified_charset;
        charsets[2] = latest_charset;

        baton.conflict_modified = conflict_modified != null ? conflict_modified : ("<<<<<<< " + modified_path); //$NON-NLS-1$
        baton.conflict_original = conflict_original != null ? conflict_original : ("||||||| " + original_path); //$NON-NLS-1$
        baton.conflict_separator = conflict_separator != null ? conflict_separator : "======="; //$NON-NLS-1$
        baton.conflict_latest = conflict_latest != null ? conflict_latest : (">>>>>>> " + latest_path); //$NON-NLS-1$

        baton.display_original_in_conflict = display_original_in_conflict;
        baton.display_resolved_conflicts = display_resolved_conflicts && !display_original_in_conflict;

        baton.setEOL(eol);
        baton.setTrailingEol(trailingEol);

        for (int idx = 0; idx < 3; idx++) {
            baton.buffer[idx] = file_baton_t.GetLines(baton.path[idx], idx, new Charset[] {
                charsets[idx]
            }, new String[1], new boolean[1]);
            baton.curp[idx] = 0;
            baton.endp[idx] = baton.buffer[idx].length;
        }

        final output_fns_t vtable = new output_fns_t(baton);

        svn_diff_output(diff, vtable);
    }

    public static void svn_diff_output(svn_diff_t diff, final output_fns_t vtable) {
        while (diff != null) {
            final boolean lastDiff = (diff.next == null);

            if (diff.type.equals(type_e.type_common)) {
                vtable.output_common(
                    diff.m_start[NDX.NDX_ORIGINAL],
                    diff.m_length[NDX.NDX_ORIGINAL],
                    diff.m_start[NDX.NDX_MODIFIED],
                    diff.m_length[NDX.NDX_MODIFIED],
                    diff.m_start[NDX.NDX_LATEST],
                    diff.m_length[NDX.NDX_LATEST],
                    lastDiff);
            } else if (diff.type.equals(type_e.type_diff_common)) {
                vtable.output_diff_common(
                    diff.m_start[NDX.NDX_ORIGINAL],
                    diff.m_length[NDX.NDX_ORIGINAL],
                    diff.m_start[NDX.NDX_MODIFIED],
                    diff.m_length[NDX.NDX_MODIFIED],
                    diff.m_start[NDX.NDX_LATEST],
                    diff.m_length[NDX.NDX_LATEST],
                    lastDiff);
            } else if (diff.type.equals(type_e.type_diff_modified)) {
                vtable.output_diff_modified(
                    diff.m_start[NDX.NDX_ORIGINAL],
                    diff.m_length[NDX.NDX_ORIGINAL],
                    diff.m_start[NDX.NDX_MODIFIED],
                    diff.m_length[NDX.NDX_MODIFIED],
                    diff.m_start[NDX.NDX_LATEST],
                    diff.m_length[NDX.NDX_LATEST],
                    lastDiff);
            } else if (diff.type.equals(type_e.type_diff_latest)) {
                vtable.output_diff_latest(
                    diff.m_start[NDX.NDX_ORIGINAL],
                    diff.m_length[NDX.NDX_ORIGINAL],
                    diff.m_start[NDX.NDX_MODIFIED],
                    diff.m_length[NDX.NDX_MODIFIED],
                    diff.m_start[NDX.NDX_LATEST],
                    diff.m_length[NDX.NDX_LATEST],
                    lastDiff);
            } else if (diff.type.equals(type_e.type_conflict)) {
                vtable.output_conflict(
                    diff.m_start[NDX.NDX_ORIGINAL],
                    diff.m_length[NDX.NDX_ORIGINAL],
                    diff.m_start[NDX.NDX_MODIFIED],
                    diff.m_length[NDX.NDX_MODIFIED],
                    diff.m_start[NDX.NDX_LATEST],
                    diff.m_length[NDX.NDX_LATEST],
                    diff.resolved_diff,
                    lastDiff);
            }

            diff = diff.next;
        }
    }

    public static boolean contains_diffs(svn_diff_t diff) {
        while (diff != null) {
            if (diff.type != type_e.type_common) {
                return true;
            }

            diff = diff.next;
        }

        return false;
    }

    public static boolean contains_conflicts(svn_diff_t diff) {
        while (diff != null) {
            if (diff.type == type_e.type_conflict) {
                return true;
            }

            diff = diff.next;
        }

        return false;
    }

    public static int count_conflicts(svn_diff_t diff) {
        int count = 0;
        while (diff != null) {
            if (diff.type == type_e.type_conflict) {
                ++count;
            }
            diff = diff.next;
        }

        return count;
    }

    public static int count_type(svn_diff_t diff, final type_e t) {
        int count = 0;
        while (diff != null) {
            if (diff.type == t) {
                ++count;
            }
            diff = diff.next;
        }

        return count;
    }

    // / <summary>
    // /
    // / </summary>
    // / <param name="diff"></param>
    // / <param name="original"></param>
    // / <param name="modified"></param>
    // / <param name="latest"></param>
    public static svn_diff_t svn_diff3_file(
        final String original,
        final Charset originalEnc,
        final String modified,
        final Charset modifiedEnc,
        final String latest,
        final Charset latestEnc,
        final Charset[] outputEnc,
        final String[] outputEol,
        final boolean[] hasTrailingNewline) {
        final file_baton_t baton = new file_baton_t();

        baton.path[0] = original;
        baton.enc[0] = originalEnc;

        baton.path[1] = modified;
        baton.enc[1] = modifiedEnc;

        baton.path[2] = latest;
        baton.enc[2] = latestEnc;

        final svn_diff_fns_t vtable = new svn_diff_fns_t(baton);

        return svn_diff3(vtable, outputEnc, outputEol, hasTrailingNewline);
    }

    // ////////////////////////////////////////////////////////////////
    // Jeff's NOTE: SourceGear.
    // NOTE: from what I can tell, the datasources (files) are broken
    // NOTE: into "tokens" (lines).
    // NOTE:
    // NOTE: the tokens are put into a circular "position list" and
    // NOTE: then stuffed into a "tree" (ordered by the token-compare-function)
    // NOTE: essentially computing a sorted ordering of the tokens.
    // NOTE: all datasources are stuffed into the same tree.
    // NOTE:
    // NOTE: two tokens (lines) are then equal iff they have the same
    // NOTE: node in the tree. (BTW, this lets them play some games
    // NOTE: with deleting the contents of the nodes once the tree is
    // NOTE: complete.) (BTW, BTW, some of these games are stupid,
    // NOTE: because they have to re-read/-parse the files in order
    // NOTE: to output diffs....)
    // ////////////////////////////////////////////////////////////////

    // ref position_t position_list
    // ref tree_t tree
    private static void get_tokens(
        final position_t[] position_list,
        final int position_idx,
        final tree_t[] tree,
        final svn_diff_fns_t vtable,
        final svn_diff_datasource_e datasource) {

        position_t start_position = null;
        position_t position = null;
        position_t position_ref;
        position_t prev = null;
        node_t node;
        file_token_t token;
        int offset;

        position_list[position_idx] = null;

        vtable.datasource_open(datasource);

        position_ref = start_position;
        offset = 0;
        token = null;

        final node_t ref_node = null;

        if (tree[0].root == null) {
            tree[0].root = ref_node;
        }
        while (true) {
            final file_token_t[] reftoken = new file_token_t[] {
                token
            };
            vtable.datasource_get_next_token(reftoken, datasource);
            token = reftoken[0];

            if (token == null) {
                break;
            }

            offset++;

            node = tree_insert_token(tree, ref_node, vtable, token);

            // Create a new position
            position = new position_t();
            position.next = null;
            position.node = node;
            position.offset = offset;
            position.prev = prev;
            prev = position;

            if (position_ref != null) {
                position_ref.next = position;
            }
            if (start_position == null) {
                start_position = position;
            }
            position_ref = position;

        }

        position_ref = start_position;

        if (start_position != null) {
            start_position.prev = position;
            position.next = start_position;
        }

        // SVN_ERR(vtable->datasource_close(diff_baton, datasource));

        position_list[position_idx] = position;

        if (position != null) {
            // keep track of the maximum offset so the snake function knows
            // when to quit
            final int maxoff = position.offset;
            position_t tmp = position_list[position_idx];
            while (tmp != null) {
                tmp.maxoffset = maxoff;
                tmp = tmp.next;
                if (tmp.offset >= maxoff) {
                    break;
                }
            }
        }

        // NOTE: for some strange reason we return the postion_list
        // NOTE: pointing to the last position in the datasource
        // NOTE: (so pl->next gives the beginning of the datasource).

    }

    private static node_t tree_insert_token(
        final tree_t[] tree,
        final node_t ref_node,
        final svn_diff_fns_t vtable,
        final file_token_t token) {
        int rv;

        final node_t node = new node_t();
        node_t temp = tree[0].root;

        while (temp != ref_node) {
            node.parent = temp;

            rv = vtable.token_compare(node.parent.token, token);

            if (rv == 0) {
                /* Discard the token */
                vtable.file_token_discard(token);

                return temp;
            } else if (rv > 0) {
                temp = temp.left;
            } else {
                temp = temp.right;
            }
        }

        node.left = null;
        node.right = null;
        node.token = token;
        if (node.parent != null) {
            rv = vtable.token_compare(node.parent.token, node.token);

            if (rv > 0) {
                node.parent.left = node;
            } else {
                node.parent.right = node;
            }
        } else {
            tree[0].root = node;
        }

        return node;
    }

    private static void snake(final int k, final snake_t[] fp, final int idx) {
        position_t start_position_0 = null;
        position_t start_position_1 = null;
        position_t position_0 = null;
        position_t position_1 = null;
        lcs_t lcs;
        lcs_t previous_lcs;

        if (fp[k - 1].y + 1 > fp[k + 1].y) {
            start_position_0 = fp[k - 1].position[0];
            start_position_1 = fp[k - 1].position[1].next;

            previous_lcs = fp[k - 1].lcs;
        } else {
            start_position_0 = fp[k + 1].position[0].next;
            start_position_1 = fp[k + 1].position[1];

            previous_lcs = fp[k + 1].lcs;
        }

        /*
         * ### Optimization, skip all positions that don't have matchpoints ###
         * anyway. Beware of the sentinel, don't skip it!
         */
        position_0 = start_position_0;
        position_1 = start_position_1;

        if (position_0.node != null && position_1.node != null) {
            while (position_0.node == position_1.node) {
                position_0 = position_0.next;
                position_1 = position_1.next;

                if (position_0.offset > position_0.maxoffset || position_1.offset > position_1.maxoffset) {
                    break;
                }
            }
        }

        if (position_1 != start_position_1) {
            lcs = new lcs_t();

            lcs.position[idx] = start_position_0;
            lcs.position[Math.abs(1 - idx)] = start_position_1;
            lcs.length = position_1.offset - start_position_1.offset;

            lcs.next = previous_lcs;
            fp[k].lcs = lcs;
        } else {
            fp[k].lcs = previous_lcs;
        }

        fp[k].position[0] = position_0;
        fp[k].position[1] = position_1;

        fp[k].y = position_1.offset;
    }

    private static lcs_t lcs_reverse(lcs_t lcs) {
        lcs_t next;
        lcs_t prev;

        next = null;
        while (lcs != null) {
            prev = lcs.next;
            lcs.next = next;
            next = lcs;
            lcs = prev;
        }

        return next;
    }

    // ////////////////////////////////////////////////////////////////
    // svn_diff__lcs_juggle() -- juggle the lcs list to eliminate
    // minor items. the lcs algorithm is greedy in that it will
    // accept a one line match as a "common" sequence and then create
    // another item for the next portion of a large change (a line
    // of code containing only a '{' or '}', for example). if the
    // gap between two "common" portions ends with the same text as
    // is in the first "common" portion, it is sort of arbitrary
    // whether we use the first "common" portion or the one in the
    // gap. but by selecting the one at the end of the gap, we can
    // coalesce the item with the following one (and append the gap
    // onto the prior node).
    private static int lcs_juggle(final lcs_t[] lcsHead) {
        // return 1 if we did some juggling
        // return 0 if we did nothing

        int bDidJuggling = 0;

        lcs_t lcs;
        lcs_t lcsNext;
        position_t pGapTail;
        position_t pBegin;
        position_t pEnd;
        int lenGap0, lenGap1, ndxGap;
        int k;

        lcs = lcsHead[0];
        while (lcs != null && lcs.next != null) {
            lcsNext = lcs.next;

            // if the next lcs is length zero, we are on the last one and can
            // do anything.
            if (lcsNext.length == 0) {
                lcs = lcs.next;
                continue;
            }

            // compute the gap between this sequence and the next one.
            lenGap0 = (lcsNext.position[0].offset - lcs.position[0].offset - lcs.length);
            lenGap1 = (lcsNext.position[1].offset - lcs.position[1].offset - lcs.length);

            // if both gaps are zero we're probably at the end.
            // whether we are or not, we don't have anything to do.
            if ((lenGap0 == 0) && (lenGap1 == 0)) {
                lcs = lcs.next;
                continue;
            }

            // if both are > 0, this is not a minor item and
            // we shouldn't mess with it.
            if ((lenGap0 > 0) && (lenGap1 > 0)) {
                lcs = lcs.next;
                continue;
            }

            // compute the index (0 or 1) of the one which has
            // the gap past the end of the common portion. the
            // other is contiguous with the next lcs.
            ndxGap = (lenGap1 > 0 ? 1 : 0);

            // get the end of the gap and back up the length of common part.
            for (pGapTail = lcsNext.position[ndxGap], k = 0; (k < lcs.length); pGapTail = pGapTail.prev, k++) {
                ;
            }

            // see if the common portion (currently prior to
            // the gap) is exactly repeated at the end of the
            // gap.
            boolean noshift = false;

            for (k = 0, pBegin = lcs.position[ndxGap], pEnd = pGapTail; (k < lcs.length); k++, pBegin =
                pBegin.next, pEnd = pEnd.next) {
                if (pBegin.node != pEnd.node) {
                    noshift = true;
                    break;
                }
            }

            if (noshift) {
                lcs = lcs.next;
                continue;
            }

            // we can shift the common portion to the end of the gap
            // and push the first common portion and the remainder
            // of the gap onto the tail of the previous lcs.
            lcs.position[ndxGap] = pGapTail;

            // now, we have 2 contiguous lcs's (lcs and lcsNext) that
            // are contiguous on both parts. so lcsNext is redundant.
            // we just add the length of lcsNext to lcs and remove
            // lcsNext from the list.
            //
            // but wait, the last thing in the lcs list is a "sentinal",
            // a zero length lcs item that we can't delete.

            if (lcsNext.length > 0) {
                lcs.length += lcsNext.length;
                lcs.next = lcsNext.next;

                bDidJuggling = 1;
            }

            // let's go back and try this again on this lcs and see
            // if it has overlap with the next one.
            continue;
        }

        return bDidJuggling;
    }

    private static lcs_t lcs(final position_t position_list1, final position_t position_list2) {
        int idx;
        final int[] length = new int[2];
        snake_t[] fp;
        int d;
        int k;
        int p = 0;
        lcs_t lcs;

        final position_t[] sentinel_position = new position_t[2];
        final node_t[] sentinel_node = new node_t[2];

        lcs = new lcs_t();
        lcs.position[0] = new position_t();
        lcs.position[0].offset = (position_list1 != null ? position_list1.offset + 1 : 1);
        lcs.position[1] = new position_t();
        lcs.position[1].offset = (position_list2 != null ? position_list2.offset + 1 : 1);
        lcs.length = 0;
        lcs.next = null;

        if (position_list1 == null || position_list2 == null) {
            return lcs;
        }

        /* Calculate length of both sequences to be compared */
        length[0] = (position_list1.offset - position_list1.next.offset + 1);
        length[1] = (position_list2.offset - position_list2.next.offset + 1);
        idx = length[0] > length[1] ? 1 : 0;

        fp = new snake_t[length[0] + length[1] + 3];
        for (int i = 0; i < fp.length; i++) {
            fp[i] = new snake_t();
        }

        sentinel_position[idx] = new position_t();
        sentinel_position[idx].next = position_list1.next;
        position_list1.next = sentinel_position[idx];
        sentinel_position[idx].offset = position_list1.offset + 1;

        sentinel_position[Math.abs(1 - idx)] = new position_t();
        sentinel_position[Math.abs(1 - idx)].next = position_list2.next;
        position_list2.next = sentinel_position[Math.abs(1 - idx)];
        sentinel_position[Math.abs(1 - idx)].offset = position_list2.offset + 1;

        sentinel_position[0].node = sentinel_node[0];
        sentinel_position[1].node = sentinel_node[1];

        d = length[Math.abs(1 - idx)] - length[idx];

        /*
         * start at the end of the first position list then compare from there
         * in both directions the length of the first positionlist - 1 will be
         * the first to be used to get previous position information from, make
         * sure it holds sane data
         */

        final int x = length[idx] + 1;

        fp[x - 1] = new snake_t();

        fp[x - 1].position[0] = sentinel_position[0].next;
        fp[x - 1].position[1] = sentinel_position[1];

        p = 0;
        do {
            /* Forward */
            for (k = x - p; k < x + d; k++) {
                snake(k, fp, idx);
            }

            for (k = d + x + p; k >= x + d; k--) {
                snake(k, fp, idx);
            }

            p++;
        } while (fp[d + x].position[1] != sentinel_position[1]);

        lcs.next = fp[d + x].lcs;
        lcs = lcs_reverse(lcs);

        position_list1.next = sentinel_position[idx].next;
        position_list2.next = sentinel_position[Math.abs(1 - idx)].next;

        // run our juggle algorithm on the LCS list to try to
        // eliminate minor annoyances -- this has the effect of
        // joining large chunks that were split by a one line
        // change -- where it is arbitrary whether the one line
        // is handled before or after the second large chunk.
        //
        // we let this run until it reaches (a kind of transitive)
        // closure. this is required because the concatenation
        // tends to cascade backwards -- that is, the tail of a
        // large change get joined to the prior change and then that
        // gets joined to the prior change (see filediffs/d176_*).
        //
        // TODO figure out how to do the juggle in one pass.
        // it's possible that we could do this in one trip if we
        // had back-pointers in the lcs list or if we tried it
        // before the list is (un)reversed. but i'll save that
        // for a later experiment. it generally only runs once,
        // but i've seen it go 4 times for a large block (with
        // several blank or '{' lines) (on d176). it is linear
        // in the number of lcs'es and since we now have back
        // pointers on the position list, it's probably not worth
        // the effort (or the amount of text in this comment :-).

        final lcs_t[] reflcs = new lcs_t[] {
            lcs
        };

        while (lcs_juggle(reflcs) > 0) {
            // dbg__dump_lcs(lcs,"LCS is:");
        }

        lcs = reflcs[0];

        return lcs;
    }

    private static svn_diff_t svn_diff3(
        final svn_diff_fns_t vtable,
        final Charset[] enc,
        final String[] eol,
        final boolean[] hasTrailingNewline) {
        tree_t tree = new tree_t();

        final position_t[] position_list = new position_t[3];
        lcs_t lcs_om;
        lcs_t lcs_ol;

        final tree_t[] reftree = new tree_t[] {
            tree
        };

        get_tokens(position_list, 0, reftree, vtable, svn_diff_datasource_e.svn_diff_datasource_original);

        get_tokens(position_list, 1, reftree, vtable, svn_diff_datasource_e.svn_diff_datasource_modified);

        get_tokens(position_list, 2, reftree, vtable, svn_diff_datasource_e.svn_diff_datasource_latest);

        enc[0] = vtable.ComputeBestEncoding();
        eol[0] = vtable.ComputeBestLineEnding();
        hasTrailingNewline[0] = vtable.ComputeBestTrailingEol();

        tree = reftree[0];

        /* Get rid of the tokens, we don't need them to calc the diff */
        vtable.file_token_discard_all();

        // Get the lcs for original-modified and original-latest *
        lcs_om = lcs(position_list[0], position_list[1]);
        lcs_ol = lcs(position_list[0], position_list[2]);

        /* Produce a merged diff */
        return svn_diff3__diff3(lcs_om, lcs_ol, 1, 1, 1, position_list);
    }

    public static svn_diff_t svn_diff3__diff3(
        lcs_t lcs_om,
        lcs_t lcs_ol,
        int original_start,
        int modified_start,
        int latest_start,
        final position_t[] position_list) {
        svn_diff_t diff = null;
        svn_diff_t diff_ref = null;

        int original_sync;
        int modified_sync;
        int latest_sync;
        int common_length;
        int modified_length;
        int latest_length;
        boolean is_modified;
        boolean is_latest;
        final position_t[] sentinel_position = new position_t[] {
            new position_t(),
            new position_t()
        };

        /*
         * Point the position lists to the start of the list so that
         * common_diff/conflict detection actually is able to work.
         */
        if (position_list[1] != null) {
            sentinel_position[0].next = position_list[1].next;
            sentinel_position[0].offset = position_list[1].offset + 1;
            position_list[1].next = sentinel_position[0];
            position_list[1] = sentinel_position[0].next;
        } else {
            sentinel_position[0].offset = 1;
            sentinel_position[0].next = null;
            position_list[1] = sentinel_position[0];
            position_list[1].maxoffset = 1;
        }

        if (position_list[2] != null) {
            sentinel_position[1].next = position_list[2].next;
            sentinel_position[1].offset = position_list[2].offset + 1;
            position_list[2].next = sentinel_position[1];
            position_list[2] = sentinel_position[1].next;
        } else {
            sentinel_position[1].offset = 1;
            sentinel_position[1].next = null;
            position_list[2] = sentinel_position[1];
            position_list[2].maxoffset = 1;
        }

        while (true) {
            /* Find the sync points */
            while (true) {
                if (lcs_om.position[0].offset > lcs_ol.position[0].offset) {
                    original_sync = lcs_om.position[0].offset;

                    while (lcs_ol.position[0].offset + lcs_ol.length < original_sync) {
                        lcs_ol = lcs_ol.next;
                    }

                    /*
                     * If the sync point is the EOF, and our current lcs segment
                     * doesn't reach as far as EOF, we need to skip this
                     * segment.
                     */
                    if (lcs_om.length == 0
                        && lcs_ol.length > 0
                        && lcs_ol.position[0].offset + lcs_ol.length == original_sync
                        && lcs_ol.position[1].offset + lcs_ol.length != lcs_ol.next.position[1].offset) {
                        lcs_ol = lcs_ol.next;
                    }

                    if (lcs_ol.position[0].offset <= original_sync) {
                        break;
                    }
                } else {
                    original_sync = lcs_ol.position[0].offset;

                    while (lcs_om.position[0].offset + lcs_om.length < original_sync) {
                        lcs_om = lcs_om.next;
                    }

                    /*
                     * If the sync point is the EOF, and our current lcs segment
                     * doesn't reach as far as EOF, we need to skip this
                     * segment.
                     */
                    if (lcs_ol.length == 0
                        && lcs_om.length > 0
                        && lcs_om.position[0].offset + lcs_om.length == original_sync
                        && lcs_om.position[1].offset + lcs_om.length != lcs_om.next.position[1].offset) {
                        lcs_om = lcs_om.next;
                    }

                    if (lcs_om.position[0].offset <= original_sync) {
                        break;
                    }
                }
            }

            modified_sync = lcs_om.position[1].offset + (original_sync - lcs_om.position[0].offset);
            latest_sync = lcs_ol.position[1].offset + (original_sync - lcs_ol.position[0].offset);

            /* Determine what is modified, if anything */
            is_modified =
                lcs_om.position[0].offset - original_start > 0 || lcs_om.position[1].offset - modified_start > 0;

            is_latest = lcs_ol.position[0].offset - original_start > 0 || lcs_ol.position[1].offset - latest_start > 0;

            if (is_modified || is_latest) {
                modified_length = modified_sync - modified_start;
                latest_length = latest_sync - latest_start;

                svn_diff_t dr = new svn_diff_t();

                dr.m_start[NDX.NDX_ORIGINAL] = original_start - 1;
                dr.m_length[NDX.NDX_ORIGINAL] = original_sync - original_start;
                dr.m_start[NDX.NDX_MODIFIED] = modified_start - 1;
                dr.m_length[NDX.NDX_MODIFIED] = modified_length;
                dr.m_start[NDX.NDX_LATEST] = latest_start - 1;
                dr.m_length[NDX.NDX_LATEST] = latest_length;
                dr.resolved_diff = null;

                if (is_modified && is_latest) {
                    final svn_diff_t[] refdr = new svn_diff_t[] {
                        dr
                    };

                    resolve_conflict(refdr, position_list);

                    dr = refdr[0];
                } else if (is_modified) {
                    dr.type = type_e.type_diff_modified; // _diff_modified;
                } else {
                    dr.type = type_e.type_diff_latest;
                }

                if (diff == null) {
                    diff = dr;
                    diff_ref = dr;
                } else {
                    diff_ref.next = dr;
                    diff_ref = dr;
                }

            }

            /* Detect EOF */
            if (lcs_om.length == 0 || lcs_ol.length == 0) {
                break;
            }

            modified_length = lcs_om.length - (original_sync - lcs_om.position[0].offset);
            latest_length = lcs_ol.length - (original_sync - lcs_ol.position[0].offset);
            common_length = modified_length < latest_length ? modified_length : latest_length;

            final svn_diff_t dn = new svn_diff_t();

            dn.type = type_e.type_common;
            dn.m_start[NDX.NDX_ORIGINAL] = original_sync - 1;
            dn.m_length[NDX.NDX_ORIGINAL] = common_length;
            dn.m_start[NDX.NDX_MODIFIED] = modified_sync - 1;
            dn.m_length[NDX.NDX_MODIFIED] = common_length;
            dn.m_start[NDX.NDX_LATEST] = latest_sync - 1;
            dn.m_length[NDX.NDX_LATEST] = common_length;
            dn.resolved_diff = null;

            if (diff == null) {
                diff = dn;
                diff_ref = dn;
            } else {
                diff_ref.next = dn;
                diff_ref = dn;
            }

            /* Set the new offsets */
            original_start = original_sync + common_length;
            modified_start = modified_sync + common_length;
            latest_start = latest_sync + common_length;

            /*
             * Make it easier for diff_common/conflict detection by recording
             * last lcs start positions
             */
            if (position_list[1].offset < lcs_om.position[1].offset) {
                position_list[1] = lcs_om.position[1];
            }

            if (position_list[2].offset < lcs_ol.position[1].offset) {
                position_list[2] = lcs_ol.position[1];
            }

            /*
             * Make sure we are pointing to lcs entries beyond the range we just
             * processed
             */
            while (original_start >= lcs_om.position[0].offset + lcs_om.length && lcs_om.length > 0) {
                lcs_om = lcs_om.next;
            }

            while (original_start >= lcs_ol.position[0].offset + lcs_ol.length && lcs_ol.length > 0) {
                lcs_ol = lcs_ol.next;
            }
        }

        diff_ref = null;

        return diff;
    }

    public static void resolve_conflict(final svn_diff_t[] hunk, final position_t[] position_list) {
        int modified_start = hunk[0].m_start[NDX.NDX_MODIFIED] + 1;
        int latest_start = hunk[0].m_start[NDX.NDX_LATEST] + 1;
        int common_length;
        int modified_length = hunk[0].m_length[NDX.NDX_MODIFIED];
        int latest_length = hunk[0].m_length[NDX.NDX_LATEST];
        final position_t[] start_position = new position_t[2];
        final position_t[] position = new position_t[2];
        lcs_t lcs_x = null;
        lcs_t lcs_ref = lcs_x;
        svn_diff_t diff_ref = null;

        // First find the starting positions for the
        // comparison
        start_position[0] = position_list[1];
        start_position[1] = position_list[2];

        while (start_position[0].offset < modified_start) {
            start_position[0] = start_position[0].next;
        }

        while (start_position[1].offset < latest_start) {
            start_position[1] = start_position[1].next;
        }

        position[0] = start_position[0];
        position[1] = start_position[1];

        common_length = modified_length < latest_length ? modified_length : latest_length;

        while (common_length > 0 && position[0].node == position[1].node) {
            position[0] = position[0].next;
            position[1] = position[1].next;

            common_length--;
        }

        if (common_length == 0 && modified_length == latest_length) {
            hunk[0].type = type_e.type_diff_common;
            hunk[0].resolved_diff = null;

            position_list[1] = position[0];
            position_list[2] = position[1];

            return;
        }

        hunk[0].type = type_e.type_conflict;

        /*
         * ### If we have a conflict we can try to find the ### common parts in
         * it by getting an lcs between ### modified (start to start + length)
         * and ### latest (start to start + length). ### We use this lcs to
         * create a simple diff. Only ### where there is a diff between the two,
         * we have ### a conflict. ### This raises a problem; several common
         * diffs and ### conflicts can occur within the same original ### block.
         * This needs some thought. ### ### NB: We can use the node _pointers_
         * to identify ### different tokens
         */

        /*
         * Calculate how much of the two sequences was actually the same.
         */

        common_length = (modified_length < latest_length ? modified_length : latest_length) - common_length;

        /*
         * If there were matching symbols at the start of both sequences, record
         * that fact.
         */

        if (common_length > 0) {
            final lcs_t lr = new lcs_t();
            lr.next = null;
            lr.position[0] = start_position[0];
            lr.position[1] = start_position[1];
            lr.length = common_length;

            // lcs_ref = lcs_x.next;

            if (lcs_x == null) {
                lcs_x = lr;
                lcs_ref = lr;
            } else {
                lcs_ref.next = lr;
                lcs_ref = lr;
            }
        }

        modified_length -= common_length;
        latest_length -= common_length;

        modified_start = start_position[0].offset;
        latest_start = start_position[1].offset;

        start_position[0] = position[0];
        start_position[1] = position[1];

        /*
         * Create a new ring for svn_diff__lcs to grok. We can safely do this
         * given we don't need the positions we processed anymore.
         */

        if (modified_length == 0) {
            position_list[1] = position[0];
            position[0] = null;
        } else {
            while (--modified_length > 0) {
                position[0] = position[0].next;
            }

            position_list[1] = position[0].next;
            position[0].next = start_position[0];
        }

        if (latest_length == 0) {
            position_list[2] = position[1];
            position[1] = null;
        } else {
            while (--latest_length > 0) {
                position[1] = position[1].next;
            }

            position_list[2] = position[1].next;
            position[1].next = start_position[1];
        }

        lcs_ref = lcs(position[0], position[1]);

        /*
         * Fix up the EOF lcs element in case one of the two sequences was NULL.
         */

        if (lcs_ref.position[0].offset == 1) {
            lcs_ref.position[0] = position_list[1];
        }

        if (lcs_ref.position[1].offset == 1) {
            lcs_ref.position[1] = position_list[2];
        }

        /* Restore modified_length and latest_length */
        modified_length = hunk[0].m_length[NDX.NDX_MODIFIED];
        latest_length = hunk[0].m_length[NDX.NDX_LATEST];

        lcs_x = lcs_ref;

        /* Produce the resolved diff */
        while (true) {
            if (modified_start < lcs_x.position[0].offset || latest_start < lcs_x.position[1].offset) {
                final svn_diff_t dr = new svn_diff_t();

                dr.type = type_e.type_conflict;
                dr.m_start[NDX.NDX_ORIGINAL] = hunk[0].m_start[NDX.NDX_ORIGINAL];
                dr.m_length[NDX.NDX_ORIGINAL] = hunk[0].m_length[NDX.NDX_ORIGINAL];
                dr.m_start[NDX.NDX_MODIFIED] = modified_start - 1;
                dr.m_length[NDX.NDX_MODIFIED] = lcs_x.position[0].offset - modified_start;
                dr.m_start[NDX.NDX_LATEST] = latest_start - 1;
                dr.m_length[NDX.NDX_LATEST] = lcs_x.position[1].offset - latest_start;

                if (hunk[0].resolved_diff == null) {
                    hunk[0].resolved_diff = dr;
                    diff_ref = dr;
                } else {
                    diff_ref.next = dr;
                    diff_ref = dr;
                }
            }

            /* Detect the EOF */
            if (lcs_x.length == 0) {
                break;
            }

            modified_start = lcs_x.position[0].offset;
            latest_start = lcs_x.position[1].offset;

            final svn_diff_t dn = new svn_diff_t();

            dn.type = type_e.type_common;
            dn.m_start[NDX.NDX_ORIGINAL] = hunk[0].m_start[NDX.NDX_ORIGINAL];
            dn.m_length[NDX.NDX_ORIGINAL] = hunk[0].m_length[NDX.NDX_ORIGINAL];
            dn.m_start[NDX.NDX_MODIFIED] = modified_start - 1;
            dn.m_length[NDX.NDX_MODIFIED] = lcs_x.length;
            dn.m_start[NDX.NDX_LATEST] = latest_start - 1;
            dn.m_length[NDX.NDX_LATEST] = lcs_x.length;

            if (hunk[0].resolved_diff == null) {
                hunk[0].resolved_diff = dn;
                diff_ref = dn;
            } else {
                diff_ref.next = dn;
                diff_ref = dn;
            }

            modified_start += lcs_x.length;
            latest_start += lcs_x.length;

            lcs_x = lcs_x.next;
        }
    }
}
