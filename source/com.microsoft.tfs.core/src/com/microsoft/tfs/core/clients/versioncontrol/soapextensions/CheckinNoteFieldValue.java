// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._CheckinNoteFieldValue;

/**
 * Represents one checkin note field value. Consists of a name, value pair.
 * <p>
 * Validation is performed to check that name is valid. No validation is done on
 * value.
 *
 * @since TEE-SDK-10.1
 */
public class CheckinNoteFieldValue extends WebServiceObjectWrapper {
    public CheckinNoteFieldValue() {
        super(new _CheckinNoteFieldValue());
    }

    public CheckinNoteFieldValue(final String name, final String value) {
        /*
         * Ensure validation happens.
         */
        this();

        setName(name);
        setValue(value);
    }

    public CheckinNoteFieldValue(final _CheckinNoteFieldValue value) {
        /*
         * Ensure validation happens.
         */
        this(value.getName(), value.getVal());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinNoteFieldValue getWebServiceObject() {
        return (_CheckinNoteFieldValue) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public void setName(final String name) {
        getWebServiceObject().setName(CheckinNote.canonicalizeName(name));
    }

    public String getValue() {
        return getWebServiceObject().getVal();
    }

    /**
     * Encode value using default XML ecoding and store.
     */
    public void setValue(final String value) {
        // We must mormalize the string to pass through carriage returns. Not
        // sure
        // Why Axis isn't doing this automagically - guess it is a bug with the
        // XML Encoder...

        // So far, my efforts have failed here. Short of writing my own XML
        // Encoder trying to figure
        // out what to do.

        final String normalizedValue = value;
        getWebServiceObject().setVal(normalizedValue);
    }
}
