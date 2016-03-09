// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.annotate;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.rangedifferencer.IRangeComparator;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Compares two files line-by-line and notes end-of-lines.
 *
 * @threadsafety unknown
 */
public class LineComparator implements IRangeComparator {
    /* Whether to tag lines as differing if they differ only in EOL type */
    private final boolean strictEolEquality = false;

    /*
     * Whether to tag last line as differing if one has EOL and other has none
     */
    private final boolean trailingEolDifference = true;

    private Line[] lines = new Line[0];

    public LineComparator(final InputStream inputStream, final String charset) throws IOException {
        final List lineList = new ArrayList();
        final char[] buf = new char[1024];
        int readlen = 0;
        final List currentLineCharList = new ArrayList();
        boolean hasCr = false;

        final InputStreamReader reader = new InputStreamReader(inputStream, charset);

        while ((readlen = reader.read(buf)) > 0) {
            for (int i = 0; i < readlen; i++) {
                final char c = buf[i];

                if (c == '\r') {
                    hasCr = true;
                    continue;
                } else if (c == '\n') {
                    addLine(lineList, currentLineCharList, hasCr ? LineEnding.CRLF : LineEnding.LF);
                    hasCr = false;
                } else {
                    /* Last char was CR, we waited for a NL but didn't see it */
                    if (hasCr) {
                        addLine(lineList, currentLineCharList, LineEnding.CR);
                    }

                    currentLineCharList.add(new Character(c));
                    hasCr = false;
                }
            }
        }

        if (currentLineCharList.size() > 0) {
            addLine(lineList, currentLineCharList, hasCr ? LineEnding.CR : LineEnding.NONE);
        }

        lines = (Line[]) lineList.toArray(new Line[lineList.size()]);
    }

    private void addLine(final List lineList, final List charList, final LineEnding lineEnding) {
        final char[] chars = new char[charList.size()];

        for (int i = 0; i < charList.size(); i++) {
            chars[i] = ((Character) charList.get(i)).charValue();
        }

        lineList.add(new Line(new String(chars), lineEnding));
        charList.clear();
    }

    @Override
    public int getRangeCount() {
        return lines.length;
    }

    @Override
    public boolean rangesEqual(final int thisIndex, final IRangeComparator other, final int otherIndex) {
        Check.notNull(lines, "lines"); //$NON-NLS-1$
        Check.isTrue(thisIndex < lines.length, "thisIndex < lines.length"); //$NON-NLS-1$
        Check.notNull(other, "other"); //$NON-NLS-1$
        Check.isTrue(other instanceof LineComparator, "other instanceof LineComparatorNew"); //$NON-NLS-1$

        final LineComparator otherComparator = (LineComparator) other;

        Check.notNull(otherComparator.lines, "otherComparator.lines"); //$NON-NLS-1$
        Check.isTrue(otherIndex < otherComparator.lines.length, "otherIndex < otherComparator.lines.length"); //$NON-NLS-1$

        final Line thisLine = lines[thisIndex];
        final Line otherLine = otherComparator.lines[otherIndex];

        if (!thisLine.getLine().equals(otherLine.getLine())) {
            return false;
        }

        /* Compare line endings (optionally) */
        if (strictEolEquality && !thisLine.getLineEnding().equals(otherLine.getLineEnding())) {
            return false;
        }

        /* One line has a newline, the other does not */
        if (trailingEolDifference
            && (thisLine.getLineEnding() == LineEnding.NONE || otherLine.getLineEnding() == LineEnding.NONE)
            && thisLine.getLineEnding() != otherLine.getLineEnding()) {
            return false;
        }

        return true;
    }

    @Override
    public boolean skipRangeComparison(final int length, final int maxLength, final IRangeComparator other) {
        return false;
    }

    private static class LineEnding extends TypesafeEnum {
        public static final LineEnding NONE = new LineEnding(0);
        public static final LineEnding CR = new LineEnding(1);
        public static final LineEnding LF = new LineEnding(2);
        public static final LineEnding CRLF = new LineEnding(3);

        private LineEnding(final int value) {
            super(value);
        }
    }

    private static class Line {
        private final String line;
        private final LineEnding lineEnding;

        public Line(final String line, final LineEnding lineEnding) {
            Check.notNull(line, "line"); //$NON-NLS-1$
            Check.notNull(lineEnding, "lineEnding"); //$NON-NLS-1$

            this.line = line;
            this.lineEnding = lineEnding;
        }

        public String getLine() {
            return line;
        }

        public LineEnding getLineEnding() {
            return lineEnding;
        }
    }
}
