// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.flags.BuildQueryOrder2010;
import com.microsoft.tfs.core.clients.build.flags.BuildReason2010;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus2010;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption2010;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildDetailSpec;

public class BuildDetailSpec2010 extends WebServiceObjectWrapper {
    private BuildDetailSpec2010() {
        this(new _BuildDetailSpec());
    }

    public BuildDetailSpec2010(final _BuildDetailSpec value) {
        super(value);
    }

    public BuildDetailSpec2010(final IBuildServer buildServer, final IBuildDetailSpec spec) {
        this();

        setBuildNumber(spec.getBuildNumber());

        if (spec.getDefinitionSpec() != null) {
            setDefinitionSpec(TFS2010Helper.convert(spec.getDefinitionSpec()));
            setDefinitionPath(spec.getDefinitionSpec().getFullPath());
        } else if (spec.getDefinitionURIs() != null) {
            setDefinitionURIs(spec.getDefinitionURIs());
        }

        setInformationTypes(spec.getInformationTypes());
        setMaxBuildsPerDefinition(spec.getMaxBuildsPerDefinition());
        setMaxFinishTime(spec.getMaxFinishTime());
        setMinChangedTime(spec.getMinChangedTime());
        setMinFinishTime(spec.getMinFinishTime());
        setQuality(spec.getQuality());
        setQueryDeletedOption(TFS2010Helper.convert(spec.getQueryDeletedOption()));
        setQueryOptions(TFS2010Helper.convert(spec.getQueryOptions()));
        setQueryOrder(TFS2010Helper.convert(spec.getQueryOrder()));
        setReason(TFS2010Helper.convert(spec.getReason()));
        setRequestedFor(spec.getRequestedFor());
        setStatus(TFS2010Helper.convert(spec.getStatus()));
    }

    public _BuildDetailSpec getWebServiceObject() {
        return (_BuildDetailSpec) webServiceObject;
    }

    public String getBuildNumber() {
        return getWebServiceObject().getBuildNumber();
    }

    public String[] getInformationTypes() {
        return getWebServiceObject().getInformationTypes();
    }

    public int getMaxBuildsPerDefinition() {
        return getWebServiceObject().getMaxBuildsPerDefinition();
    }

    public Calendar getMaxFinishTime() {
        return getWebServiceObject().getMaxFinishTime();
    }

    public Calendar getMinChangedTime() {
        return getWebServiceObject().getMinChangedTime();
    }

    public Calendar getMinFinishTime() {
        return getWebServiceObject().getMinFinishTime();
    }

    public String getQuality() {
        return getWebServiceObject().getQuality();
    }

    public QueryDeletedOption2010 getQueryDeletedOption() {
        return QueryDeletedOption2010.fromWebServiceObject(getWebServiceObject().getQueryDeletedOption());
    }

    public QueryOptions2010 getQueryOptions() {
        return QueryOptions2010.fromWebServiceObject(getWebServiceObject().getQueryOptions());
    }

    public BuildQueryOrder2010 getQueryOrder() {
        return BuildQueryOrder2010.fromWebServiceObject(getWebServiceObject().getQueryOrder());
    }

    public BuildReason2010 getReason() {
        return BuildReason2010.fromWebServiceObject(getWebServiceObject().getReason());
    }

    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    public BuildStatus2010 getStatus() {
        return BuildStatus2010.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public void setBuildNumber(final String value) {
        getWebServiceObject().setBuildNumber(value);
    }

    public void setInformationTypes(final String[] value) {
        getWebServiceObject().setInformationTypes(value);
    }

    public void setMaxBuildsPerDefinition(final int value) {
        getWebServiceObject().setMaxBuildsPerDefinition(value);
    }

    public void setMaxFinishTime(final Calendar value) {
        getWebServiceObject().setMaxFinishTime(value);
    }

    public void setMinChangedTime(final Calendar value) {
        getWebServiceObject().setMinChangedTime(value);
    }

    public void setMinFinishTime(final Calendar value) {
        getWebServiceObject().setMinFinishTime(value);
    }

    public void setDefinitionURIs(final String[] value) {
        getWebServiceObject().setDefinitionUris(value);
        getWebServiceObject().setDefinitionSpec(null);
    }

    public void setDefinitionSpec(final BuildDefinitionSpec2010 value) {
        getWebServiceObject().setDefinitionSpec(value.getWebServiceObject());
        getWebServiceObject().setDefinitionUris(null);
    }

    public void setDefinitionPath(final String value) {
        getWebServiceObject().setDefinitionPath(value);
    }

    public void setQuality(final String value) {
        getWebServiceObject().setQuality(value);
    }

    public void setQueryDeletedOption(final QueryDeletedOption2010 value) {
        getWebServiceObject().setQueryDeletedOption(value.getWebServiceObject());
    }

    public void setQueryOptions(final QueryOptions2010 value) {
        getWebServiceObject().setQueryOptions(value.getWebServiceObject());
    }

    public void setQueryOrder(final BuildQueryOrder2010 value) {
        getWebServiceObject().setQueryOrder(value.getWebServiceObject());
    }

    public void setReason(final BuildReason2010 value) {
        getWebServiceObject().setReason(value.getWebServiceObject());
    }

    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

    public void setStatus(final BuildStatus2010 value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }
}
