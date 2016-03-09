// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.utils.BuildPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._03._BuildGroupItemSpec;

public class BuildGroupItemSpec2010 extends WebServiceObjectWrapper {
    private String name;
    private String teamProject;

    public BuildGroupItemSpec2010(final _BuildGroupItemSpec value) {
        super(value);
    }

    public _BuildGroupItemSpec getWebServiceObject() {
        return (_BuildGroupItemSpec) webServiceObject;
    }

    public String getName() {
        return name;
    }

    public void setName(final String value) {
        name = value;
        getWebServiceObject().setFullPath(buildFullPath(teamProject, name));
    }

    public String getTeamProject() {
        return teamProject;
    }

    public void setTeamProject(final String value) {
        teamProject = value;
        getWebServiceObject().setFullPath(buildFullPath(teamProject, name));
    }

    public String getFullPath() {
        return getWebServiceObject().getFullPath();
    }

    public void setFullPath(final String value) {
        getWebServiceObject().setFullPath(value);

        // These should never throw here, since this setter should only get
        // called on deserialization.
        teamProject = BuildPath.getTeamProject(value);
        name = BuildPath.getItemName(value);
    }

    private static String buildFullPath(final String teamProject, final String name) {
        final StringBuilder sb = new StringBuilder();
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(teamProject);
        sb.append(BuildPath.PATH_SEPERATOR);
        sb.append(name);
        return sb.toString();
    }
}
