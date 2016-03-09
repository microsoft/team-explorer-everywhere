// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.teamsettings;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;

import ms.tfs.services.teamconfiguration._01._TeamConfiguration;

public class TeamConfiguration extends WebServiceObjectWrapper {
    public TeamConfiguration(final _TeamConfiguration webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _TeamConfiguration getWebServiceObject() {
        return (_TeamConfiguration) webServiceObject;
    }

    public String getProjectURI() {
        return getWebServiceObject().getProjectUri();
    }

    public void setProjectURI(final String value) {
        getWebServiceObject().setProjectUri(value);
    }

    public GUID getTeamID() {
        return new GUID(getWebServiceObject().getTeamId());
    }

    public void setTeamID(final GUID value) {
        getWebServiceObject().setTeamId(value.getGUIDString());
    }

    public String getTeamName() {
        return getWebServiceObject().getTeamName();
    }

    public void setTeamName(final String value) {
        getWebServiceObject().setTeamName(value);
    }

    public TeamSettings getTeamSettings() {
        return new TeamSettings(getWebServiceObject().getTeamSettings());
    }

    public void setTeamSettings(final TeamSettings value) {
        getWebServiceObject().setTeamSettings(value != null ? value.getWebServiceObject() : null);
    }

    public boolean isDefaultTeam() {
        return getWebServiceObject().isIsDefaultTeam();
    }

    public void setDefaultTeam(final boolean value) {
        getWebServiceObject().setIsDefaultTeam(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TeamConfiguration == false) {
            return false;
        }

        return ((TeamConfiguration) o).getTeamID().equals(getTeamID());
    }

    @Override
    public int hashCode() {
        return getTeamID().hashCode();
    }
}
