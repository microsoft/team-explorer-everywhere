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

package com.microsoft.teamfoundation.build.webapi.model;

import java.util.Date;

/** 
 */
public class BuildAgent {

    private String buildDirectory;
    private ShallowReference controller;
    private Date createdDate;
    private String description;
    private boolean enabled;
    private int id;
    private String messageQueueUrl;
    private String name;
    private String reservedForBuild;
    private ShallowReference server;
    private AgentStatus status;
    private String statusMessage;
    private Date updatedDate;
    private String uri;
    private String url;

    public String getBuildDirectory() {
        return buildDirectory;
    }

    public void setBuildDirectory(final String buildDirectory) {
        this.buildDirectory = buildDirectory;
    }

    public ShallowReference getController() {
        return controller;
    }

    public void setController(final ShallowReference controller) {
        this.controller = controller;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final Date createdDate) {
        this.createdDate = createdDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public String getMessageQueueUrl() {
        return messageQueueUrl;
    }

    public void setMessageQueueUrl(final String messageQueueUrl) {
        this.messageQueueUrl = messageQueueUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getReservedForBuild() {
        return reservedForBuild;
    }

    public void setReservedForBuild(final String reservedForBuild) {
        this.reservedForBuild = reservedForBuild;
    }

    public ShallowReference getServer() {
        return server;
    }

    public void setServer(final ShallowReference server) {
        this.server = server;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(final AgentStatus status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(final String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Date getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(final Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }
}
