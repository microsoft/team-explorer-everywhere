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

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.util.List;
import java.util.UUID;

/** 
 */
public class TaskDefinition {

    private TaskExecution agentExecution;
    private String author;
    private String category;
    private boolean contentsUploaded;
    private List<Demand> demands;
    private String description;
    private String friendlyName;
    private List<TaskGroupDefinition> groups;
    private String helpMarkDown;
    private String hostType;
    private String iconUrl;
    private UUID id;
    private List<TaskInputDefinition> inputs;
    private String instanceNameFormat;
    private String minimumAgentVersion;
    private String name;
    private String packageLocation;
    private String packageType;
    private boolean serverOwned;
    private List<TaskSourceDefinition> sourceDefinitions;
    private String sourceLocation;
    private TaskVersion version;
    private List<String> visibility;

    public TaskExecution getAgentExecution() {
        return agentExecution;
    }

    public void setAgentExecution(final TaskExecution agentExecution) {
        this.agentExecution = agentExecution;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public boolean getContentsUploaded() {
        return contentsUploaded;
    }

    public void setContentsUploaded(final boolean contentsUploaded) {
        this.contentsUploaded = contentsUploaded;
    }

    public List<Demand> getDemands() {
        return demands;
    }

    public void setDemands(final List<Demand> demands) {
        this.demands = demands;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(final String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public List<TaskGroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(final List<TaskGroupDefinition> groups) {
        this.groups = groups;
    }

    public String getHelpMarkDown() {
        return helpMarkDown;
    }

    public void setHelpMarkDown(final String helpMarkDown) {
        this.helpMarkDown = helpMarkDown;
    }

    public String getHostType() {
        return hostType;
    }

    public void setHostType(final String hostType) {
        this.hostType = hostType;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(final String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public List<TaskInputDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(final List<TaskInputDefinition> inputs) {
        this.inputs = inputs;
    }

    public String getInstanceNameFormat() {
        return instanceNameFormat;
    }

    public void setInstanceNameFormat(final String instanceNameFormat) {
        this.instanceNameFormat = instanceNameFormat;
    }

    public String getMinimumAgentVersion() {
        return minimumAgentVersion;
    }

    public void setMinimumAgentVersion(final String minimumAgentVersion) {
        this.minimumAgentVersion = minimumAgentVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPackageLocation() {
        return packageLocation;
    }

    public void setPackageLocation(final String packageLocation) {
        this.packageLocation = packageLocation;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(final String packageType) {
        this.packageType = packageType;
    }

    public boolean getServerOwned() {
        return serverOwned;
    }

    public void setServerOwned(final boolean serverOwned) {
        this.serverOwned = serverOwned;
    }

    public List<TaskSourceDefinition> getSourceDefinitions() {
        return sourceDefinitions;
    }

    public void setSourceDefinitions(final List<TaskSourceDefinition> sourceDefinitions) {
        this.sourceDefinitions = sourceDefinitions;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(final String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public TaskVersion getVersion() {
        return version;
    }

    public void setVersion(final TaskVersion version) {
        this.version = version;
    }

    public List<String> getVisibility() {
        return visibility;
    }

    public void setVisibility(final List<String> visibility) {
        this.visibility = visibility;
    }
}
