// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.QueryOptions2010;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus2010;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildQueueSpec;

public class BuildQueueSpec2010 extends WebServiceObjectWrapper {
    private BuildQueueSpec2010() {
        this(new _BuildQueueSpec());
    }

    public BuildQueueSpec2010(final _BuildQueueSpec value) {
        super(value);
    }

    public BuildQueueSpec2010(final BuildQueueSpec spec) {
        this();

        final _BuildQueueSpec o = getWebServiceObject();
        o.setCompletedAge(spec.getCompletedAge());

        if (spec.getControllerSpec() != null) {
            final BuildControllerSpec2010 newSpec =
                new BuildControllerSpec2010((BuildControllerSpec) spec.getControllerSpec());
            o.setControllerSpec(newSpec.getWebServiceObject());
        }

        if (spec.getDefinitionSpec() != null) {
            final BuildDefinitionSpec2010 newSpec = new BuildDefinitionSpec2010(spec.getDefinitionSpec());
            o.setDefinitionSpec(newSpec.getWebServiceObject());
        } else if (spec.getDefinitionURIs() != null) {
            o.setDefinitionUris(spec.getDefinitionURIs());
        }

        setQueryOptions(TFS2010Helper.convert(spec.getQueryOptions()));
        setRequestedFor(spec.getRequestedFor());
        setStatus(TFS2010Helper.convert(spec.getStatus()));
    }

    public _BuildQueueSpec getWebServiceObject() {
        return (_BuildQueueSpec) webServiceObject;
    }

    public QueryOptions2010 getQueryOptions() {
        return QueryOptions2010.fromWebServiceObject(getWebServiceObject().getOptions());
    }

    public String getRequestedFor() {
        return getWebServiceObject().getRequestedFor();
    }

    public QueueStatus2010 getStatus() {
        return QueueStatus2010.fromWebServiceObject(getWebServiceObject().getStatusFlags());
    }

    public void setQueryOptions(final QueryOptions2010 value) {
        getWebServiceObject().setOptions(value.getWebServiceObject());
    }

    public void setRequestedFor(final String value) {
        getWebServiceObject().setRequestedFor(value);
    }

    public void setStatus(final QueueStatus2010 value) {
        getWebServiceObject().setStatusFlags(value.getWebServiceObject());
    }
}
