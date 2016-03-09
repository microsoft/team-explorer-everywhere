// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildController;
import com.microsoft.tfs.core.clients.build.IBuildControllerQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IFailure;
import com.microsoft.tfs.core.clients.build.internal.utils.BuildTypeConvertor;
import com.microsoft.tfs.core.clients.build.internal.utils.QueryResultHelper;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._BuildAgent;
import ms.tfs.build.buildservice._04._BuildController;
import ms.tfs.build.buildservice._04._BuildControllerQueryResult;
import ms.tfs.build.buildservice._04._BuildServiceHost;

public class BuildControllerQueryResult extends WebServiceObjectWrapper implements IBuildControllerQueryResult {
    private final BuildController[] controllers;
    private final BuildServiceHost[] serviceHosts;
    private final BuildAgent[] agents;

    public BuildControllerQueryResult(
        final IBuildServer buildServer,
        final _BuildControllerQueryResult webServiceObject) {
        super(webServiceObject);

        Check.notNull(buildServer, "buildServer"); //$NON-NLS-1$

        serviceHosts = BuildTypeConvertor.toBuildServiceHostArray(buildServer, getWebServiceObject().getServiceHosts());
        agents = BuildTypeConvertor.toBuildAgentArray(getWebServiceObject().getAgents());
        controllers = BuildTypeConvertor.toBuildControllersArray(buildServer, getWebServiceObject().getControllers());

        afterDeserialize(buildServer);
    }

    public BuildControllerQueryResult(
        final IBuildServer buildServer,
        final BuildAgent[] agents,
        final BuildController[] controllers,
        final BuildServiceHost[] serviceHosts) {
        super(new _BuildControllerQueryResult());

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

    public _BuildControllerQueryResult getWebServiceObject() {
        return (_BuildControllerQueryResult) this.webServiceObject;
    }

    @Override
    public IBuildController[] getControllers() {
        return controllers;
    }

    @Override
    public IFailure[] getFailures() {
        return new IFailure[0];
    }

    private void afterDeserialize(final IBuildServer buildServer) {
        QueryResultHelper.match(serviceHosts, controllers, agents);
    }
}
