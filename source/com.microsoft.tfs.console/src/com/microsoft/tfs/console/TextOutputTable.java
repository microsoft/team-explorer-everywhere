// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.console;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.microsoft.tfs.console.display.ConsoleDisplay;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * A not-very-memory-efficient text output layout class that supports tabular
 * layout (columns, rows). Columns are defined, rows are added, then output is
 * made to a {@link PrintStream}.
 *
 * @threadsafety unknown
 */
public final class TextOutputTable {
    /**
     * The default overall indent (characters from left that we start the first
     * column).
     */
    private int overallIndent = 0;

    /**
     * The default width of gutters (empty characters between columns).
     */
    private final int gutterWidthCharacters = 1;

    private Column[] columns = new Column[0];
    private final List<String[]> rows = new ArrayList<String[]>();

    /**
     * The maximum number of characters we will write on a single line if
     * wrapping is enabled and we can wrap.
     */
    private final int maximumOutputWidthCharacters;

    /**
     * If true, show headings above columns. If false, do not.
     */
    private boolean showHeadings = true;

    /**
     * If <code>true</code>, wrap wide text to another row inside columns that
     * won't fit it. If <code>false</code>, perhaps make the column wider but
     * print all text on one row.
     */
    private boolean wrapColumnText = false;

    /**
     * If <code>true</code>, column text which would exceed the right margin of
     * the screen is truncated. If <code>false</code> long column text is
     * printed with no limit.
     */
    private boolean truncateLongLines = false;

    /**
     * We will wrap on these characters if wrapping is enabled. Order is
     * important: the first character is preferred, but if it's not found in the
     * string to be wrapped, the second character is tried, and so on.
     */
    private static final char[] CHARACTERS_FOR_WRAPPING = new char[] {
        ' ',
        '\t',
        '|'
    };

    /**
     * The smallest terminal width we allow for tables. Terminals smaller cause
     * output to simply use {@link ConsoleDisplay}'s defaults.
     * <p>
     * The maximum is read from {@link ConsoleDisplay} instead of redefined
     * here. {@link ConsoleDisplay} does not define a minimum because tiny
     * terminals may be useful to some users (but not for table output).
     */
    private final static int MINIMUM_TERMINAL_WIDTH = 20;

    /**
     * A column represents an allocation of horizontal space and its heading in
     * a {@link TextOutputTable}.
     *
     * @threadsafety thread-safe
     */
    public static class Column {
        private final String name;
        private final Sizing sizing;

        /**
         * Represents a column's width preference relative to its text contents
         * and the total screen space available.
         *
         * @threadsafety immutable
         */
        public static class Sizing extends TypesafeEnum {
            /**
             * The column will be wide enough to contain the widest element of
             * its contents.
             *
             * Contents are never line-wrapped.
             */
            public static final Sizing TIGHT = new Sizing(0);

            /**
             * The column's width will expand to take up available space. Only
             * one {@link #EXPAND} column is permitted per table, and it's
             * usually the last column.
             *
             * If wrapping is enabled and the content is too wide to fit on the
             * screen, the {@link #EXPAND} column's data will be wrapped onto
             * multiple lines.
             */
            public static final Sizing EXPAND = new Sizing(1);

            private Sizing(final int value) {
                super(value);
            }
        }

        /**
         * Constructs a column with the given name and size.
         *
         * @param name
         *        the name that will appear in the column heading (must not be
         *        <code>null</code>)
         * @param sizing
         *        the {@link Sizing} this column prefers (must not be
         *        <code>null</code>)
         */
        public Column(final String name, final Sizing sizing) {
            Check.notNull(name, "name"); //$NON-NLS-1$
            Check.notNull(sizing, "sizing"); //$NON-NLS-1$

            this.name = name;
            this.sizing = sizing;
        }

        /**
         * Get this column's name.
         *
         * @return this column's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Get this column's sizing preference.
         *
         * @return the {@link Sizing} this column prefers
         */
        public Sizing getSizing() {
            return sizing;
        }
    }

    /**
     * Constructs a table with the given width (which will be used if it is is
     * sane).
     * <p>
     * If the given width is really small (less than
     * {@link #MINIMUM_TERMINAL_WIDTH} columns), or if the width is really huge
     * (greater than {@link ConsoleDisplay#MAXIMUM_CONSOLE_WIDTH} columns),
     * {@link ConsoleDisplay#DEFAULT_CONSOLE_WIDTH} columns is used.
     * <p>
     * This is mostly to detect pathological terminal configurations (Eclipse's
     * debug terminal on at least Linux) causing gigantic terminals.
     *
     * @param maximumOutputWidthCharacters
     *        the maximum width (in characters) of table output when wrapping is
     *        possible (output may still exceed this value in some cases).
     *        Usually this is the terminal's width if the table will be
     *        displayed on a terminal.
     *        <p>
     */
    public TextOutputTable(int maximumOutputWidthCharacters) {
        if (maximumOutputWidthCharacters < MINIMUM_TERMINAL_WIDTH
            || maximumOutputWidthCharacters > ConsoleDisplay.MAXIMUM_CONSOLE_WIDTH) {
            maximumOutputWidthCharacters = ConsoleDisplay.DEFAULT_CONSOLE_WIDTH;
        }

        this.maximumOutputWidthCharacters = maximumOutputWidthCharacters;
    }

    /**
     * Clears all column definitions and row data in this table.
     */
    public synchronized void clear() {
        columns = new Column[0];
        rows.clear();
    }

    /**
     * Clears column definitions in this table.
     */
    public synchronized void clearColumns() {
        columns = new Column[0];
    }

    /**
     * Clears row data in this table.
     */
    public synchronized void clearRows() {
        rows.clear();
    }

    /**
     * Sets the columns for this table.
     *
     * @param columns
     *        the columns to use for this table (existing columns will be
     *        forgotten about). This array and items inside it must not be null.
     */
    public synchronized void setColumns(final Column[] columns) {
        Check.notNull(columns, "columns"); //$NON-NLS-1$

        this.columns = columns.clone();
    }

    /**
     * Sets whether column headings are visible. Default is true.
     *
     * @param showHeadings
     *        whether column headings will be visible in the output.
     */
    public void setHeadingsVisible(final boolean showHeadings) {
        this.showHeadings = showHeadings;
    }

    /**
     * Gets whether column headings are visible.
     *
     * @return true if the column headings will be visible in the output, false
     *         if they will be hidden.
     */
    public boolean getHeadingsVisible() {
        return showHeadings;
    }

    /**
     * Gets the width in characters of the display (well, the width the text
     * output table thinks it is).
     *
     * @return the width, in characters, of the display the text output table is
     *         writing to.
     */
    public int getDisplayWidth() {
        return maximumOutputWidthCharacters;
    }

    /**
     * Sets the indent, in characters, of all rows printed for the table
     * (including headers, separators, data, etc.).
     *
     * @param indentCharacters
     *        the number of characters to indent every row by.
     */
    public void setOverallIndent(final int indentCharacters) {
        Check.isTrue(indentCharacters >= 0, "indentCharacters must be at least 0."); //$NON-NLS-1$
        overallIndent = indentCharacters;
    }

    /**
     * Gets the overall indent for the table.
     *
     * @return the number of characters to indent every row by.
     */
    public int getOverallIndent() {
        return overallIndent;
    }

    public void setWrapColumnText(final boolean wrapColumnText) {
        this.wrapColumnText = wrapColumnText;
    }

    public boolean getWrapColumnText() {
        return wrapColumnText;
    }

    public void setTruncateLongLines(final boolean truncateLongLines) {
        this.truncateLongLines = truncateLongLines;
    }

    public boolean getTruncateLongLines() {
        return truncateLongLines;
    }

    /**
     * Gets the current row count of the table.
     *
     * @return the count of rows in the table.
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Add a row to the table. A row consists of an array of strings, each
     * string representing an item to appear in a column in that row. If there
     * are more items than columns, items at the end will not be shown. If there
     * are fewer items than columns, columns near the end will be empty. Use
     * <code>null</code>s or empty strings in the array to specify a column
     * should appear empty.
     *
     * @param items
     *        the items to include in the new row (not null).
     */
    public synchronized void addRow(final String[] items) {
        Check.notNull(items, "items"); //$NON-NLS-1$

        rows.add(items.clone());
    }

    /**
     * Sort the rows already added to this table with the given comparator. Does
     * not affect rows added later.
     *
     * @param comparator
     *        the comparator to use during the sort (not null).
     */
    public synchronized void sort(final Comparator<String[]> comparator) {
        Check.notNull(comparator, "comparator"); //$NON-NLS-1$

        Collections.sort(rows, comparator);
    }

    /**
     * Prints the table to a {@link PrintStream}.
     *
     * @param stream
     *        the stream to print the table to (must not be <code>null</code>)
     */
    public synchronized void print(final PrintStream stream) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$

        /*
         * Determine the correct widths of the columns. A column's width is
         * determined by its sizing preference, its text content, and available
         * space.
         *
         * TIGHT pressure column widths are computed first. Contents are never
         * truncated, but they may wrap to another line if wrapping is enabled.
         *
         * Any remaining space is given to an EXPAND column, if there is one
         * (there can be at most one). Contents may wrap if line wrapping is
         * enabled.
         *
         * All columns are at least one character wide.
         */

        /*
         * Since we'll be printing column gutters, we have to subtract the total
         * width of the gutters from the terminal width to know how much space
         * we have to write columns. Also factor in the overall indent, which is
         * printed at the beginning of every row (and separator row).
         */
        final int adjustedTerminalWidth =
            maximumOutputWidthCharacters - (gutterWidthCharacters * (columns.length - 1)) - overallIndent;

        /*
         * Some place to store our calculated column widths.
         */
        final int[] columnWidths = new int[columns.length];

        /*
         * First pass at column widths: calculate for tight first; sum
         * calculated tight widths; count expand columns (so we can error if >
         * 1).
         */
        int totalWidthUsed = 0;
        int expandColumnIndex = -1;
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].getSizing() == Column.Sizing.TIGHT) {
                /*
                 * Tight columns require their row data be examined. If any row
                 * item is found larger than the column's maximum width, the
                 * maximum width is used, otherwise the largest number found is
                 * used. Don't forget the column header text!
                 */

                int longestItemLength = (columns[i].getName() != null) ? columns[i].getName().length() : 1;

                for (int j = 0; j < rows.size(); j++) {
                    final String[] values = rows.get(j);
                    if (i > values.length - 1) {
                        continue;
                    }

                    final String item = values[i];

                    if (item == null) {
                        continue;
                    }

                    longestItemLength = Math.max(longestItemLength, item.length());
                }

                columnWidths[i] = longestItemLength;
                totalWidthUsed += longestItemLength;
            } else if (columns[i].getSizing() == Column.Sizing.EXPAND) {
                if (expandColumnIndex != -1) {
                    throw new RuntimeException("Can have only one Sizing.EXPAND column per table"); //$NON-NLS-1$
                }

                expandColumnIndex = i;
            }
        }

        /*
         * Account for the expand column (if present). Give it at least 1
         * character (maybe all the TIGHT columns used the available space and
         * we got pushed off the edge of the screen?).
         */
        if (expandColumnIndex != -1) {
            // Subtract 1 extra to leave the last character blank (VS does this)
            columnWidths[expandColumnIndex] = Math.max(1, adjustedTerminalWidth - totalWidthUsed - 1);
        }

        /*
         * Print the column headers (if enabled).
         */
        if (showHeadings) {
            /*
             * To keep our row printing logic unified, we put the column names
             * into a string array here.
             */
            final String[] columnNames = new String[columns.length];

            for (int i = 0; i < columns.length; i++) {
                columnNames[i] = columns[i].getName();
            }

            printRow(stream, columnNames, columnWidths);

            /*
             * Print the column header separator.
             */
            printSeparatorRow(stream, '-', columnWidths);
        }

        /*
         * Print the rows.
         */
        for (int i = 0; i < rows.size(); i++) {
            printRow(stream, rows.get(i), columnWidths);
        }
    }

    private void printSeparatorRow(
        final PrintStream stream,
        final char separatorChar,
        final int[] columnDisplayWidths) {
        // Buffer up our writes for better performance.
        final StringBuffer buf = new StringBuffer();

        printSpaces(buf, overallIndent);

        for (int i = 0; i < columns.length; i++) {
            /*
             * Print the gutter that separates columns.
             */
            if (i > 0) {
                printSpaces(buf, gutterWidthCharacters);
            }

            for (int j = 0; j < columnDisplayWidths[i]; j++) {
                buf.append(separatorChar);
            }
        }

        stream.println(buf.toString());
    }

    /**
     * Prints a row (possibly as multiple lines, if wrapping is enabled).
     */
    private void printRow(final PrintStream stream, final String[] items, final int[] columnDisplayWidths) {
        // Print the overall indent first.
        if (overallIndent > 0) {
            printSpaces(stream, overallIndent);
        }

        /*
         * If we have to wrap data in a column in this row, we build the new row
         * here with the remaining data.
         */
        boolean needsNewRow = false;
        final String[] newRow = new String[items.length];

        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                /*
                 * Print the gutter that separates columns.
                 */
                printSpaces(stream, gutterWidthCharacters);
            }

            /*
             * The item array may not have enough items for all columns. End the
             * line right here if that's the case.
             */
            if (i > items.length - 1) {
                break;
            }

            final int columnWidth = columnDisplayWidths[i];
            String text = items[i];

            /*
             * If the text is null, and there is a next column, just print
             * enough spaces for the column and move to the next column.
             *
             * If there is no next column, we don't print the spaces. This lets
             * terminals scroll faster, and won't trigger wrapping if the
             * terminal is resized small.
             */
            if (text == null) {
                if ((i + 1) < columns.length) {
                    printSpaces(stream, columnWidth);
                }
                continue;
            }

            /*
             * If word wrapping is enabled, and we'll have text that requires
             * it, do that special output case.
             *
             * Wrapping for EXPAND columns is commonly desired, but since TIGHT
             * columns are always big enough to contain their contents, they
             * will only wrap if there isn't enough screen space to contain
             * their contents.
             */
            if (wrapColumnText && text.length() > columnWidth) {
                /*
                 * Too long. Find the best wrap character that's after one word
                 * and wrap there, or if there are no wrap characters then
                 * simply truncate, putting the remainder of the string in the
                 * next row.
                 */

                final String sub = text.substring(0, columnWidth);

                // Try wrapping at the last instance of each wrap char
                int wrapIndex = -1;
                for (final char wrapChar : CHARACTERS_FOR_WRAPPING) {
                    wrapIndex = sub.lastIndexOf(wrapChar);

                    if (wrapIndex >= 0) {
                        /*
                         * Make sure there's some non-wrap char (non-whitespace)
                         * before the char we found.
                         */
                        int moonWalker = wrapIndex;
                        while (moonWalker >= 0 && isWrappingBreakCharacter(sub.charAt(moonWalker))) {
                            moonWalker--;
                        }

                        /*
                         * If the moonWalker stopped before -1, we found
                         * non-whitespace. Save the character after that
                         * non-whitespace as the break point.
                         */
                        if (moonWalker > -1) {
                            wrapIndex = moonWalker + 1;
                            break;
                        }
                    }
                }

                /*
                 * Not a single wrappable character found! Wrap at the end of
                 * the line.
                 */
                if (wrapIndex == -1) {
                    wrapIndex = sub.length() - 1;
                }

                /*
                 * Break this string in two at the correct wrap index we found,
                 * writing the first part back to the "text" variable so it can
                 * be written below in the normal (not wrapped) case. The second
                 * part goes into the new row.
                 */
                if (wrapIndex + 1 <= text.length()) {
                    newRow[i] = StringUtil.trimBegin(text.substring(wrapIndex + 1));
                }

                text = StringUtil.trimEnd(sub.substring(0, wrapIndex + 1));

                needsNewRow = true;
            }

            if (truncateLongLines) {
                stream.print(text.substring(0, Math.min(columnWidth, text.length())));
            } else {
                stream.print(text);
            }

            /*
             * If we need to pad, and there will be a next column, print the
             * spaces.
             *
             * If there is no next column, we don't print the spaces. This lets
             * terminals scroll faster, and won't trigger wrapping if the
             * terminal is resized small.
             */
            if ((text.length() < columnWidth) && ((i + 1) < columns.length)) {
                printSpaces(stream, columnWidth - text.length());
            }
        }

        stream.println();

        if (needsNewRow) {
            // Call this method recursively.
            printRow(stream, newRow, columnDisplayWidths);
        }
    }

    /**
     * Tests whether the table can break a line for wrapping at the given
     * character (usually some whitespace).
     *
     * @param c
     *        the character to test.
     * @return true if the text can be broken on the given character, false if
     *         it can not.
     */
    private boolean isWrappingBreakCharacter(final char c) {
        for (int i = 0; i < CHARACTERS_FOR_WRAPPING.length; i++) {
            if (c == CHARACTERS_FOR_WRAPPING[i]) {
                return true;
            }
        }

        return false;
    }

    /**
     * Prints spaces into an existing {@link StringBuffer}.
     *
     * @param buffer
     *        the buffer (not null).
     * @param count
     *        the number of spaces to print.
     */
    private void printSpaces(final StringBuffer buffer, final int count) {
        for (int i = 0; i < count; i++) {
            buffer.append(' ');
        }
    }

    /**
     * Prints spaces into an existing {@link PrintStream}.
     *
     * @param stream
     *        the stream (not null).
     * @param count
     *        the number of spaces to print.
     */
    private void printSpaces(final PrintStream stream, final int count) {
        Check.isTrue(count >= 0, "count >= 0"); //$NON-NLS-1$

        final StringBuffer buf = new StringBuffer();
        printSpaces(buf, count);
        stream.print(buf.toString());
    }
}
