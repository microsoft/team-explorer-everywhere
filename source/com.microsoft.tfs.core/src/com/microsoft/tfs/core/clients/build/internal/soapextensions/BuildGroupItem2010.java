// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildGroupItem;
import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildGroupItem;

public class BuildGroupItem2010 extends WebServiceObjectWrapper implements IBuildGroupItem {
    private String name;
    private String teamProject;

    public BuildGroupItem2010(final _BuildGroupItem buildGroupItem) {
        super(buildGroupItem);
    }

    public _BuildGroupItem getWebServiceObject() {
        return (_BuildGroupItem) webServiceObject;
    }

    @Override
    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String value) {
        name = value;
    }

    @Override
    public String getTeamProject() {
        return teamProject;
    }

    @Override
    public String getFullPath() {
        if (teamProject == null || name == null) {
            final String fullPath = getWebServiceObject().getFullPath();
            teamProject = BuildPath.getTeamProject(fullPath);
            name = BuildPath.getItemName(fullPath);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(teamProject);
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(name);
        return sb.toString();
    }

    public void setFullPath(final String value) {
        getWebServiceObject().setFullPath(value);

        teamProject = BuildPath.getTeamProject(value);
        name = BuildPath.getItemName(value);
    }

    @Override
    public void refresh() {
    }
}
