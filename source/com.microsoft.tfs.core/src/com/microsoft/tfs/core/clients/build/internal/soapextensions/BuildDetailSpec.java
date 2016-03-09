// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.flags.BuildQueryOrder;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueryDeletedOption;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._04._BuildDetailSpec;

public class BuildDetailSpec extends WebServiceObjectWrapper implements IBuildDetailSpec {
    private BuildDetailSpec() {
        super(new _BuildDetailSpec());
    }

    public BuildDetailSpec(final String teamProject) {
        this(new BuildDefinitionSpec(teamProject));
    }

    public BuildDetailSpec(final String teamProject, final String definitionName) {
        this(new BuildDefinitionSpec(teamProject, definitionName));
    }

    public BuildDetailSpec(final IBuildDefinition definition) {
        this(new BuildDefinitionSpec(definition));
    }

    public BuildDetailSpec(final IBuildDefinitionSpec definitionSpec) {
        this();

        // The default behavior should be to request everything.
        setDefaults();

        getWebServiceObject().setDefinitionSpec(((BuildDefinitionSpec) definitionSpec).getWebServiceObject());
    }

    public _BuildDetailSpec getWebServiceObject() {
        return (_BuildDetailSpec) this.webServiceObject;
    }

    /**
     * Gets or sets the build number filter. Wildcards are allowed.
     * {@inheritDoc}
     */
    @Override
    public String getBuildNumber() {
        return getWebServiceObject().getBuildNumber();
    }

    @Override
    public void setBuildNumber(final String value) {
        getWebServiceObject().setBuildNumber(value);
    }

    /**
     * Gets the information types to return. A single element with the value '*'
     * indicates all information. {@inheritDoc}
     */
    @Override
    public String[] getInformationTypes() {
        return getWebServiceObject().getInformationTypes().clone();
    }

    @Override
    public void setInformationTypes(final String[] value) {
        getWebServiceObject().setInformationTypes(value);
    }

    /**
     * Gets or sets the maximum number of builds to return per build definition.
     * {@inheritDoc}
     */
    @Override
    public int getMaxBuildsPerDefinition() {
        return getWebServiceObject().getMaxBuildsPerDefinition();
    }

    @Override
    public void setMaxBuildsPerDefinition(final int value) {
        getWebServiceObject().setMaxBuildsPerDefinition(value);
    }

    /**
     * Gets or sets the maximum finish time filter. {@inheritDoc}
     */
    @Override
    public Calendar getMaxFinishTime() {
        return getWebServiceObject().getMaxFinishTime();
    }

    @Override
    public void setMaxFinishTime(final Calendar value) {
        getWebServiceObject().setMaxFinishTime(value);
    }

    /**
     * Gets or sets the minimum changed time filter. {@inheritDoc}
     */
    @Override
    public Calendar getMinChangedTime() {
        return getWebServiceObject().getMinChangedTime();
    }

    @Override
    public void setMinChangedTime(final Calendar value) {
        getWebServiceObject().setMinChangedTime(value);
    }

    /**
     * Gets or sets the minimum finish time filter. {@inheritDoc}
     */
    @Override
    public Calendar getMinFinishTime() {
        return getWebServiceObject().getMinFinishTime();
    }

    @Override
    public void setMinFinishTime(final Calendar value) {
        getWebServiceObject().setMinFinishTime(value);
    }

    /**
     * Gets or sets the build quality filter. {@inheritDoc}
     */
    @Override
    public String getQuality() {
        return getWebServiceObject().getQuality();
    }

    @Override
    public void setQuality(final String value) {
        getWebServiceObject().setQuality(value);
    }

    /**
     * Gets or sets the behavior for returning deleted builds. {@inheritDoc}
     */
    @Override
    public QueryDeletedOption getQueryDeletedOption() {
        return QueryDeletedOption.fromWebServiceObject(getWebServiceObject().getQueryDeletedOption());
    }

    @Override
    public void setQueryDeletedOption(final QueryDeletedOption value) {
        getWebServiceObject().setQueryDeletedOption(value.getWebServiceObject());
    }

    /**
     * Gets or sets the options used to control the amount of data returned.
     * {@inheritDoc}
     */
    @Override
    public QueryOptions getQueryOptions() {
        return QueryOptions.fromWebServiceObject(getWebServiceObject().getQueryOptions());
    }

    @Override
    public void setQueryOptions(final QueryOptions value) {
        getWebServiceObject().setQueryOptions(value.getWebServiceObject());
    }

    /**
     * ets or sets the desired order of the builds returned. {@inheritDoc}
     */
    @Override
    public BuildQueryOrder getQueryOrder() {
        return BuildQueryOrder.fromWebServiceObject(getWebServiceObject().getQueryOrder());
    }

    @Override
    public void setQueryOrder(final BuildQueryOrder value) {
        getWebServiceObject().setQueryOrder(value.getWebServiceObject());
    }

    /**
     * Gets or sets the build reason filter. {@inheritDoc}
     */
    @Override
    public BuildReason getReason() {
        return BuildReason.fromWebServiceObject(getWebServiceObject().getReason());
    }

    @Override
    public void setReason(final BuildReason value) {
        getWebServiceObject().setReason(value.getWebServiceObject());
    }

    /**
     * Gets or sets the requested for filter. {@inheritDoc}
     */
    @Override
    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    @Override
    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

    /**
     * Gets or sets the build status filter. {@inheritDoc}
     */
    @Override
    public BuildStatus getStatus() {
        return BuildStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    @Override
    public void setStatus(final BuildStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    @Override
    public String[] getDefinitionURIs() {
        return getWebServiceObject().getDefinitionUris();
    }

    @Override
    public IBuildDefinitionSpec getDefinitionSpec() {
        return new BuildDefinitionSpec(getWebServiceObject().getDefinitionSpec());
    }

    private void setDefaults() {
        final _BuildDetailSpec detail = getWebServiceObject();

        detail.setBuildNumber(BuildConstants.STAR);
        detail.setMinFinishTime(DotNETDate.MIN_CALENDAR);
        detail.setMaxFinishTime(DotNETDate.MIN_CALENDAR);
        detail.setMinChangedTime(DotNETDate.MIN_CALENDAR);

        BuildStatus status = BuildStatus.SUCCEEDED;
        status = status.combine(BuildStatus.PARTIALLY_SUCCEEDED);
        status = status.combine(BuildStatus.STOPPED);
        status = status.combine(BuildStatus.FAILED);
        status = status.combine(BuildStatus.NOT_STARTED);
        status = status.combine(BuildStatus.IN_PROGRESS);

        detail.setStatus(status.getWebServiceObject());
        detail.setQueryOptions(QueryOptions.ALL.getWebServiceObject());
        detail.setInformationTypes(BuildConstants.ALL_INFORMATION_TYPES);
    }
}
