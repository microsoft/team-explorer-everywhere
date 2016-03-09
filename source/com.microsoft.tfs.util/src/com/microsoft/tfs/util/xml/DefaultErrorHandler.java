// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.xml;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * {@link DefaultErrorHandler} is a default SAX {@link ErrorHandler}
 * implementation. This implementation throws a {@link SAXException} for every
 * error, fatal error, and warning that is encountered.
 */
public class DefaultErrorHandler implements ErrorHandler {
    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error(final SAXParseException exception) throws SAXException {
        throw exception;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError(final SAXParseException exception) throws SAXException {
        throw exception;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning(final SAXParseException exception) throws SAXException {
        throw exception;
    }
}
