// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildQueueSpec2008;

public class BuildQueueSpec2008 extends WebServiceObjectWrapper {
    private BuildQueueSpec2008() {
        this(new _BuildQueueSpec2008());
    }

    public BuildQueueSpec2008(final _BuildQueueSpec2008 webServiceObject) {
        super(webServiceObject);
    }

    public BuildQueueSpec2008(final BuildQueueSpec spec) {
        this();

        final BuildControllerSpec controllerSpec = (BuildControllerSpec) spec.getControllerSpec();
        if (controllerSpec != null) {
            final BuildAgentSpec2008 spec2008 =
                TFS2008Helper.convert(controllerSpec, spec.getDefinitionSpec().getTeamProject());
            getWebServiceObject().setAgentSpec(spec2008.getWebServiceObject());
        }

        getWebServiceObject().setCompletedAge(spec.getCompletedAge());

        if (spec.getDefinitionSpec() != null) {
            getWebServiceObject().setDefinitionSpec(
                TFS2008Helper.convert(spec.getDefinitionSpec()).getWebServiceObject());
        }

        getWebServiceObject().setOptions(TFS2008Helper.convert(spec.getQueryOptions()).getWebServiceObject());
        getWebServiceObject().setStatusFlags(TFS2010Helper.convert(spec.getStatus()).getWebServiceObject());
    }

    public _BuildQueueSpec2008 getWebServiceObject() {
        return (_BuildQueueSpec2008) this.webServiceObject;
    }

    public QueryOptions2010 getQueryOptions() {
        return QueryOptions2010.fromWebServiceObject(getWebServiceObject().getOptions());
    }

    public void setQueryOptions(final QueryOptions2010 value) {
        getWebServiceObject().setOptions(value.getWebServiceObject());
    }

    public QueueStatus2010 getStatus() {
        return QueueStatus2010.fromWebServiceObject(getWebServiceObject().getStatusFlags());
    }

    public void setStatus(final QueueStatus2010 value) {
        getWebServiceObject().setStatusFlags(value.getWebServiceObject());
    }
}
