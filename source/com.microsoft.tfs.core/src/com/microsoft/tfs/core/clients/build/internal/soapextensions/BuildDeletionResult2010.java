// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildDeletionResult;
import ms.tfs.build.buildservice._03._Failure;

public class BuildDeletionResult2010 extends WebServiceObjectWrapper {
    public BuildDeletionResult2010(final _BuildDeletionResult value) {
        super(value);
    }

    public _BuildDeletionResult getWebServiceObject() {
        return (_BuildDeletionResult) webServiceObject;
    }

    public Failure2010 getDropLocationFailure() {
        return wrapFailure(getWebServiceObject().getDropLocationFailure());
    }

    public Failure2010 getLabelFailure() {
        return wrapFailure(getWebServiceObject().getLabelFailure());
    }

    public Failure2010 getSymbolsFailure() {
        return wrapFailure(getWebServiceObject().getSymbolsFailure());
    }

    public Failure2010 getTestResultFailure() {
        return wrapFailure(getWebServiceObject().getTestResultFailure());
    }

    public void setDropLocationFailure(final Failure2010 value) {
        getWebServiceObject().setDropLocationFailure(value.getWebServiceObject());
    }

    public void setLabelFailure(final Failure2010 value) {
        getWebServiceObject().setLabelFailure(value.getWebServiceObject());
    }

    public void setSymbolsFailure(final Failure2010 value) {
        getWebServiceObject().setSymbolsFailure(value.getWebServiceObject());
    }

    public void setTestResultFailure(final Failure2010 value) {
        getWebServiceObject().setTestResultFailure(value.getWebServiceObject());
    }

    private Failure2010 wrapFailure(final _Failure _failure) {
        if (_failure == null) {
            return null;
        }
        return new Failure2010(_failure);
    }
}
