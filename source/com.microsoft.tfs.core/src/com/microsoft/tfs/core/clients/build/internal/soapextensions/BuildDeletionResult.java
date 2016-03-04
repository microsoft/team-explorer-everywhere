// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildDeletionResult;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._BuildDeletionResult;

public class BuildDeletionResult extends WebServiceObjectWrapper implements IBuildDeletionResult {
    private BuildDeletionResult() {
        this(new _BuildDeletionResult());
    }

    public BuildDeletionResult(final _BuildDeletionResult webServiceObject) {
        super(webServiceObject);
    }

    public BuildDeletionResult(final BuildDeletionResult2010 result2010) {
        this();

        final _BuildDeletionResult result = getWebServiceObject();

        if (result2010.getDropLocationFailure() != null) {
            result.setDropLocationFailure(new Failure(result2010.getDropLocationFailure()).getWebServiceObject());
        }

        if (result2010.getLabelFailure() != null) {
            result.setLabelFailure(new Failure(result2010.getLabelFailure()).getWebServiceObject());
        }

        if (result2010.getSymbolsFailure() != null) {
            result.setSymbolsFailure(new Failure(result2010.getSymbolsFailure()).getWebServiceObject());
        }

        if (result2010.getTestResultFailure() != null) {
            result.setTestResultFailure(new Failure(result2010.getTestResultFailure()).getWebServiceObject());
        }
    }

    public _BuildDeletionResult getWebServiceObject() {
        return (_BuildDeletionResult) this.webServiceObject;
    }

    @Override
    public IFailure getLabelFailure() {
        return new Failure(getWebServiceObject().getLabelFailure());
    }

    @Override
    public IFailure getTestResultFailure() {
        return new Failure(getWebServiceObject().getTestResultFailure());
    }

    @Override
    public IFailure getDropLocationFailure() {
        return new Failure(getWebServiceObject().getDropLocationFailure());
    }

    @Override
    public IFailure getSymbolsFailure() {
        return new Failure(getWebServiceObject().getSymbolsFailure());
    }

    @Override
    public boolean isSuccessful() {
        final _BuildDeletionResult r = getWebServiceObject();
        return r.getDropLocationFailure() == null
            && r.getLabelFailure() == null
            && r.getTestResultFailure() == null
            && r.getSymbolsFailure() == null;
    }
}
