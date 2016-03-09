// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildAgent;
import com.microsoft.tfs.core.clients.build.IBuildAgentQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.clients.build.internal.utils.QueryResultHelper;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildAgentQueryResult;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildServiceHost;

public class BuildAgentQueryResult extends WebServiceObjectWrapper implements IBuildAgentQueryResult {
    private final BuildAgent[] agents;
    private final BuildServiceHost[] serviceHosts;
    private final BuildController[] controllers;

    public BuildAgentQueryResult(final IBuildServer buildServer, final _BuildAgentQueryResult webServiceObject) {
        super(webServiceObject);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$

        serviceHosts = BuildTypeConvertor.toBuildServiceHostArray(buildServer, getWebServiceObject().getServiceHosts());
        controllers = BuildTypeConvertor.toBuildControllersArray(buildServer, getWebServiceObject().getControllers());
        agents = BuildTypeConvertor.toBuildAgentArray(getWebServiceObject().getAgents());

        afterDeserialize(buildServer);
    }

    public BuildAgentQueryResult(
        final IBuildServer buildServer,
        final BuildAgent[] agents,
        final BuildController[] controllers,
        final BuildServiceHost[] serviceHosts) {
        super(new _BuildAgentQueryResult());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(agents, "agents"); //$NON-NLS-1$
        Check.notNull(controllers, "controllers"); //$NON-NLS-1$
        Check.notNull(serviceHosts, "serviceHosts"); //$NON-NLS-1$

        this.agents = agents;
        this.controllers = controllers;
        this.serviceHosts = serviceHosts;

        final _BuildAgent[] _agents = (_BuildAgent[]) WrapperUtils.unwrap(_BuildAgent.class, agents);
        final _BuildController[] _controllers =
            (_BuildController[]) WrapperUtils.unwrap(_BuildController.class, controllers);
        final _BuildServiceHost[] _serviceHosts =
            (_BuildServiceHost[]) WrapperUtils.unwrap(_BuildServiceHost.class, serviceHosts);

        getWebServiceObject().setAgents(_agents);
        getWebServiceObject().setControllers(_controllers);
        getWebServiceObject().setServiceHosts(_serviceHosts);

        afterDeserialize(buildServer);
    }

    public _BuildAgentQueryResult getWebServiceObject() {
        return (_BuildAgentQueryResult) this.webServiceObject;
    }

    @Override
    public IBuildAgent[] getAgents() {
        return agents;
    }

    @Override
    public IFailure[] getFailures() {
        return new IFailure[0];
    }

    private void afterDeserialize(final IBuildServer buildServer) {
        QueryResultHelper.match(serviceHosts, controllers, agents);
    }
}
