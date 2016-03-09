// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildControllerSpec;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSpec;
import com.microsoft.tfs.core.clients.build.IQueuedBuildSpec;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._BuildQueueSpec;

public class BuildQueueSpec extends WebServiceObjectWrapper implements IQueuedBuildSpec {
    public BuildQueueSpec() {
        super(new _BuildQueueSpec());
    }

    public BuildQueueSpec(final String teamProject) {
        this(teamProject, BuildConstants.STAR);
    }

    public BuildQueueSpec(final String teamProject, final String definitionName) {
        this();

        final _BuildQueueSpec spec = getWebServiceObject();
        spec.setControllerSpec(new BuildControllerSpec(BuildConstants.STAR, BuildConstants.STAR).getWebServiceObject());
        spec.setCompletedAge(0);
        spec.setQueryOptions(QueryOptions.ALL.getWebServiceObject());
        spec.setStatus(QueueStatus.ALL.getWebServiceObject());
        spec.setDefinitionSpec(new BuildDefinitionSpec(teamProject, definitionName).getWebServiceObject());
    }

    public BuildQueueSpec(final String[] definitionUris) {
        this();

        final _BuildQueueSpec spec = getWebServiceObject();
        spec.setControllerSpec(new BuildControllerSpec(BuildConstants.STAR, BuildConstants.STAR).getWebServiceObject());
        spec.setCompletedAge(0);
        spec.setQueryOptions(QueryOptions.ALL.getWebServiceObject());
        spec.setStatus(QueueStatus.ALL.getWebServiceObject());
        spec.setDefinitionUris(definitionUris);
    }

    public _BuildQueueSpec getWebServiceObject() {
        return (_BuildQueueSpec) this.webServiceObject;
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
    public QueueStatus getStatus() {
        return QueueStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    @Override
    public void setStatus(final QueueStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    @Override
    public IBuildControllerSpec getControllerSpec() {
        return new BuildControllerSpec(getWebServiceObject().getControllerSpec());
    }

    @Override
    public IBuildDefinitionSpec getDefinitionSpec() {
        return new BuildDefinitionSpec(getWebServiceObject().getDefinitionSpec());
    }

    @Override
    public String[] getDefinitionURIs() {
        return getWebServiceObject().getDefinitionUris();
    }

    @Override
    public int getCompletedAge() {
        return getWebServiceObject().getCompletedAge();
    }

    @Override
    public void setCompletedAge(final int value) {
        getWebServiceObject().setCompletedAge(value);
    }
}
