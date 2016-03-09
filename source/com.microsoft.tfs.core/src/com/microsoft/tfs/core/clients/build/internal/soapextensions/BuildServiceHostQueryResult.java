// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IBuildServiceHost;
import com.microsoft.tfs.core.clients.build.IBuildServiceHostQueryResult;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.clients.build.internal.utils.QueryResultHelper;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildServiceHost;
import ms.tfs.build.buildservice._04._BuildServiceHostQueryResult;

public class BuildServiceHostQueryResult extends WebServiceObjectWrapper implements IBuildServiceHostQueryResult {
    private final BuildServiceHost[] serviceHosts;
    private final BuildController[] controllers;
    private final BuildAgent[] agents;

    public BuildServiceHostQueryResult(
        final IBuildServer buildServer,
        final _BuildServiceHostQueryResult webServiceObject) {
        super(webServiceObject);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$

        controllers = BuildTypeConvertor.toBuildControllersArray(buildServer, getWebServiceObject().getControllers());
        agents = BuildTypeConvertor.toBuildAgentArray(getWebServiceObject().getAgents());
        serviceHosts = BuildTypeConvertor.toBuildServiceHostArray(buildServer, getWebServiceObject().getServiceHosts());

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

    public BuildServiceHostQueryResult(
        final IBuildServer buildServer,
        final BuildAgent[] agents,
        final BuildController[] controllers,
        final BuildServiceHost[] serviceHosts) {
        super(new _BuildServiceHostQueryResult());

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$
        Check.notNull(agents, "agents"); //$NON-NLS-1$
        Check.notNull(controllers, "controllers"); //$NON-NLS-1$
        Check.notNull(serviceHosts, "serviceHosts"); //$NON-NLS-1$

        this.agents = agents;
        this.controllers = controllers;
        this.serviceHosts = serviceHosts;

        afterDeserialize(buildServer);
    }

    public _BuildServiceHostQueryResult getWebServiceObject() {
        return (_BuildServiceHostQueryResult) this.webServiceObject;
    }

    @Override
    public IBuildServiceHost[] getServiceHosts() {
        return serviceHosts;
    }

    @Override
    public IFailure[] getFailures() {
        return new IFailure[0];
    }

    private void afterDeserialize(final IBuildServer buildServer) {
        QueryResultHelper.match(serviceHosts, controllers, agents);
    }
}
