// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.teamsettings;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.services.teamconfiguration._01._TeamFieldValue;
import ms.tfs.services.teamconfiguration._01._TeamSettings;

public class TeamSettings extends WebServiceObjectWrapper {
    public TeamSettings(final _TeamSettings webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _TeamSettings getWebServiceObject() {
        return (_TeamSettings) webServiceObject;
    }

    public String getBacklogIterationPath() {
        return getWebServiceObject().getBacklogIterationPath();
    }

    public void setBacklogIterationPath(final String value) {
        getWebServiceObject().setBacklogIterationPath(value);
    }

    public String[] getIterationPaths() {
        return getWebServiceObject().getIterationPaths();
    }

    public void setIterationPaths(final String[] value) {
        getWebServiceObject().setIterationPaths(value);
    }

    public TeamFieldValue[] getTeamFieldValues() {
        return (TeamFieldValue[]) WrapperUtils.wrap(TeamFieldValue.class, getWebServiceObject().getTeamFieldValues());
    }

    public void setTeamFieldValues(final TeamFieldValue[] teamFieldValues) {
        getWebServiceObject().setTeamFieldValues(
            (_TeamFieldValue[]) WrapperUtils.unwrap(_TeamFieldValue.class, teamFieldValues));
    }

    public String getTeamField() {
        return getWebServiceObject().getTeamField();
    }

    public void setTeamField(final String teamField) {
        getWebServiceObject().setTeamField(teamField);
    }

    public String getCurrentIterationPath() {
        return getWebServiceObject().getCurrentIterationPath();
    }

    public void setCurrentIterationPath(final String currentIterationPath) {
        getWebServiceObject().setCurrentIterationPath(currentIterationPath);
    }

}
