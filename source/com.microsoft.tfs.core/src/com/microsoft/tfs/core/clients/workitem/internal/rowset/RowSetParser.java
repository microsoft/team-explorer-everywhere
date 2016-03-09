// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rowset;

import java.text.MessageFormat;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.ws.runtime.stax.StaxUtils;
import com.microsoft.tfs.core.ws.runtime.stax.dom.DOMStreamReader;

public class RowSetParser {
    private String tableName;
    private int columnCount;

    public RowSetParseHandler parse(final Element element, final RowSetParseHandler handler) {
        /*
         * Wrap the element in an (in-memory) XMLStreamReader wrapper.
         */

        final XMLStreamReader reader = new DOMStreamReader(element);
        RowSetParseHandler ret;

        try {
            ret = parse(reader, handler);
            reader.close();
        } catch (final XMLStreamException e) {
            throw new RowSetParseException(MessageFormat.format(
                "Error handling Element with {0} wrapper: {1}", //$NON-NLS-1$
                DOMStreamReader.class.getName(),
                e.toString()));
        }

        return ret;
    }

    public RowSetParseHandler parse(final XMLStreamReader reader, final RowSetParseHandler handler) {
        handler.handleBeginParsing();

        /*
         * If the reader is at the start of the document, advance until the
         * first element.
         */
        if (reader.getEventType() == XMLStreamConstants.START_DOCUMENT) {
            try {
                while (reader.next() != XMLStreamConstants.START_ELEMENT) {
                    ;
                }
            } catch (final XMLStreamException e) {
                throw new RowSetParseException("could not find start element in stream for row set"); //$NON-NLS-1$
            }
        }

        /*
         * Check the name of the element - ensure that it's "table"
         */
        final String elementName = reader.getLocalName();
        if (!"table".equals(elementName)) //$NON-NLS-1$
        {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "the given MessageElement has a name of [{0}], not \"table\"", //$NON-NLS-1$
                    elementName));
        }

        /*
         * Get the table's name, validate it, and pass it to the handler
         */
        tableName = reader.getAttributeValue(null, "name"); //$NON-NLS-1$
        if (tableName == null) {
            throw new RowSetParseException("the metadata table did not have a name"); //$NON-NLS-1$
        }
        handler.handleTableName(tableName);

        /*
         * Parse the top-level columns and rows.
         */
        String localName = null;
        int event;

        try {
            do {
                event = reader.next();

                if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                    localName = reader.getLocalName();

                    if (localName.equals("columns")) //$NON-NLS-1$
                    {
                        /*
                         * Read all the columns and feed them to the handler.
                         * This method processes the END_ELEMENT event for the
                         * 'columns' element.
                         */
                        parseColumns(reader, handler);

                        /*
                         * Finished with columns, since there will only be one
                         * element that contains them.
                         */
                        handler.handleFinishedColumns();
                    } else if (localName.equals("rows")) //$NON-NLS-1$
                    {
                        /*
                         * Parse all the columns and feed them to the handler.
                         * This method processes the END_ELEMENT event for the
                         * 'rows' element.
                         */
                        parseRows(reader, handler);
                    } else {
                        throw new RowSetParseException(
                            MessageFormat.format(
                                "Unexpected child element named ''{0}'' inside ''table'' element", //$NON-NLS-1$
                                localName));
                    }
                }
            } while (event != XMLStreamConstants.END_ELEMENT);
        } catch (final XMLStreamException e) {
            throw new RowSetParseException(MessageFormat.format("Error parsing row set XML: {0}", e.toString())); //$NON-NLS-1$
        }

        handler.handleEndParsing();

        return handler;
    }

    private void parseColumns(final XMLStreamReader reader, final RowSetParseHandler handler)
        throws XMLStreamException,
            RowSetParseException {
        /*
         * Reset the column count
         */
        columnCount = 0;

        String localName = null;
        int event;

        do {
            event = reader.next();

            if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                localName = reader.getLocalName();

                /*
                 * Each column is a child "c" element of the "columns" element
                 */
                if (localName.equals("c")) //$NON-NLS-1$
                {
                    parseIndividualColumn(reader, handler);
                } else {
                    throw new RowSetParseException(
                        MessageFormat.format(
                            "Unexpected child element named ''{0}'' inside ''columns'' element", //$NON-NLS-1$
                            localName));
                }
            }
        } while (event != XMLStreamConstants.END_ELEMENT);
    }

    private void parseIndividualColumn(final XMLStreamReader reader, final RowSetParseHandler handler)
        throws XMLStreamException,
            RowSetParseException {
        String localName = null;
        int event;

        String columnName = null;
        String typeName = null;

        do {
            event = reader.next();

            if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                localName = reader.getLocalName();

                /*
                 * The column element should have a single "n" child - the name
                 * of the column
                 */
                if (localName.equals("n")) //$NON-NLS-1$
                {
                    columnName = StaxUtils.getElementTextOrNull(reader);
                } else if (localName.equals("t")) //$NON-NLS-1$
                {
                    /*
                     * The column element should have a single "t" child - the
                     * type of the column
                     */
                    typeName = StaxUtils.getElementTextOrNull(reader);
                } else {
                    throw new RowSetParseException(
                        MessageFormat.format(
                            "Unexpected child element named ''{0}'' inside ''columns'' element", //$NON-NLS-1$
                            localName));
                }
            }
        } while (event != XMLStreamConstants.END_ELEMENT);

        if (columnName == null) {
            throw new RowSetParseException("Did not get a column name element 'n'"); //$NON-NLS-1$
        }

        if (typeName == null) {
            throw new RowSetParseException("Did not get a column type element 't'"); //$NON-NLS-1$
        }

        handler.handleColumn(columnName, typeName);

        /*
         * Update the column count
         */
        ++columnCount;
    }

    private void parseRows(final XMLStreamReader reader, final RowSetParseHandler handler)
        throws XMLStreamException,
            RowSetParseException {
        String localName = null;
        int event;

        do {
            event = reader.next();

            if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                localName = reader.getLocalName();

                /*
                 * Each row is a child "r" element of the "rows" element
                 */
                if (localName.equals("r")) //$NON-NLS-1$
                {
                    parseIndividualRow(reader, handler);
                } else {
                    throw new RowSetParseException(
                        MessageFormat.format(
                            "Unexpected child element named ''{0}'' inside ''rows'' element", //$NON-NLS-1$
                            localName));
                }
            }
        } while (event != XMLStreamConstants.END_ELEMENT);
    }

    private void parseIndividualRow(final XMLStreamReader reader, final RowSetParseHandler handler)
        throws XMLStreamException {
        /*
         * Create an array to hold our row values that's the size of the number
         * of columns
         */
        final String[] rowValues = new String[columnCount];
        int colIx = 0;

        String localName = null;
        int event;

        do {
            event = reader.next();

            if (event == javax.xml.stream.XMLStreamConstants.START_ELEMENT) {
                localName = reader.getLocalName();

                /*
                 * Each row element has a child "f" element for each row value
                 * (field?)
                 */
                if (localName.equals("f")) //$NON-NLS-1$
                {
                    /*
                     * Check for the special "k" attribute - this indicates that
                     * some previous row values have been skipped
                     */
                    final String kAttribute = reader.getAttributeValue(null, "k"); //$NON-NLS-1$
                    if (kAttribute != null && kAttribute.length() > 0) {
                        final int specifiedColIx = Integer.parseInt(kAttribute);
                        while (colIx < specifiedColIx) {
                            rowValues[colIx++] = null;
                        }
                    }

                    /*
                     * The text of this value element is the next row value.
                     * getElementTextOrNull() reads the END_ELEMENT event.
                     */
                    rowValues[colIx++] = StaxUtils.getElementTextOrNull(reader);
                } else {
                    throw new RowSetParseException(
                        MessageFormat.format(
                            "Unexpected child element named ''{0}'' inside ''r'' element", //$NON-NLS-1$
                            localName));
                }
            }
        } while (event != XMLStreamConstants.END_ELEMENT);

        handler.handleRow(rowValues);
    }

    public String getTableName() {
        return tableName;
    }
}
