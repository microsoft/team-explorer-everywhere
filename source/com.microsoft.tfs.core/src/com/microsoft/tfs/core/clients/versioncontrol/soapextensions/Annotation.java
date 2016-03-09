// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._Annotation;

/**
 * @since TEE-SDK-10.1
 */
public class Annotation extends WebServiceObjectWrapper {
    public Annotation() {
        super(new _Annotation());
    }

    public Annotation(final _Annotation annotation) {
        super(annotation);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Annotation getWebServiceObject() {
        return (_Annotation) webServiceObject;
    }

    public String getComment() {
        return getWebServiceObject().getComment();
    }

    public Calendar getDate() {
        return getWebServiceObject().getDate();
    }

    public String getItem() {
        return getWebServiceObject().getItem();
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public int getVersion() {
        return getWebServiceObject().getV();
    }

    public String getValue() {
        return getWebServiceObject().getValue();
    }

    public void setComment(final String value) {
        getWebServiceObject().setComment(value);
    }

    public void setDate(final Calendar value) {
        getWebServiceObject().setDate(value);
    }

    public void setItem(final String value) {
        getWebServiceObject().setItem(value);
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setVersion(final int value) {
        getWebServiceObject().setV(value);
    }

    public void setValue(final String value) {
        getWebServiceObject().setValue(value);
    }
}
