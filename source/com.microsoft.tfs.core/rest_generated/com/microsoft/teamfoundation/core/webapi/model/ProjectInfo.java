// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.core.webapi.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import com.microsoft.teamfoundation.common.model.ProjectState;

/** 
 * Contains information of the project
 * 
 */
public class ProjectInfo {

    private String abbreviation;
    private String description;
    private UUID id;
    private Date lastUpdateTime;
    private String name;
    private List<ProjectProperty> properties;
    /**
    * Current revision of the project
    */
    private long revision;
    private ProjectState state;
    private String uri;
    private long version;

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(final String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(final Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<ProjectProperty> getProperties() {
        return properties;
    }

    public void setProperties(final List<ProjectProperty> properties) {
        this.properties = properties;
    }

    /**
    * Current revision of the project
    */
    public long getRevision() {
        return revision;
    }

    /**
    * Current revision of the project
    */
    public void setRevision(final long revision) {
        this.revision = revision;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(final ProjectState state) {
        this.state = state;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(final long version) {
        this.version = version;
    }
}
