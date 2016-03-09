// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Date;

import com.microsoft.tfs.core.Messages;

/**
 * Writes any metadata table to an HTML file on disk.
 */
public class HTMLWriterRowSetHandler implements RowSetParseHandler {
    private PrintWriter out;
    private final String filename;
    private int colCount;
    private int rowCount;

    public HTMLWriterRowSetHandler(final String filename) {
        this.filename = filename;
    }

    @Override
    public void handleBeginParsing() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(filename);
        } catch (final FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        out = new PrintWriter(fos);

        colCount = 0;
        rowCount = 0;
    }

    @Override
    public void handleFinishedColumns() {
        out.println("</tr>"); //$NON-NLS-1$
    }

    @Override
    public void handleEndParsing() {
        out.println("</table>"); //$NON-NLS-1$
        out.println(MessageFormat.format(
            "{0}: {1}", //$NON-NLS-1$
            Messages.getString("HTMLWriterRowSetHandler.ColumnsCountSummaryText"), //$NON-NLS-1$
            Integer.toString(colCount)));
        out.println("<br/>"); //$NON-NLS-1$
        out.println(MessageFormat.format(
            "{0}: {1}", //$NON-NLS-1$
            Messages.getString("HTMLWriterRowSetHandler.RowsCountSummaryText"), //$NON-NLS-1$
            Integer.toString(rowCount)));
        out.println("</body></html>"); //$NON-NLS-1$
        out.close();
    }

    @Override
    public void handleTableName(final String tableName) {
        out.println(MessageFormat.format(
            "<html><head><title>{0}: {1}</title>", //$NON-NLS-1$
            Messages.getString("HTMLWriterRowSetHandler.TableTitleText"), //$NON-NLS-1$
            tableName));
        out.println("<style type=\"text/css\">"); //$NON-NLS-1$
        out.println("body {font-family: Verdana, Arial, Helvetica, sans-serif}"); //$NON-NLS-1$
        out.println("table {font-size: 10px;}"); //$NON-NLS-1$
        out.println("</style>"); //$NON-NLS-1$
        out.println("<body>"); //$NON-NLS-1$
        out.println(MessageFormat.format(
            "{0}: {1}", //$NON-NLS-1$
            Messages.getString("HTMLWriterRowSetHandler.CurrentTimeText"), //$NON-NLS-1$
            new Date()));
        out.println("<br/><br/>"); //$NON-NLS-1$
        out.println("<table border=\"1\">"); //$NON-NLS-1$
        out.println("<tr>"); //$NON-NLS-1$
    }

    @Override
    public void handleColumn(final String name, final String type) {
        out.println("<th>"); //$NON-NLS-1$
        out.println(name);
        out.println("<br/>"); //$NON-NLS-1$
        out.println(type);
        out.println("</th>"); //$NON-NLS-1$

        ++colCount;
    }

    @Override
    public void handleRow(final String[] rowValues) {
        out.println("<tr>"); //$NON-NLS-1$
        for (int i = 0; i < rowValues.length; i++) {
            out.println("<td>"); //$NON-NLS-1$
            final String value = rowValues[i];
            out.println(value == null || value.trim().length() == 0 ? "&nbsp;" : value); //$NON-NLS-1$
            out.println("</td>"); //$NON-NLS-1$
        }
        out.println("</tr>"); //$NON-NLS-1$

        ++rowCount;
    }
}
