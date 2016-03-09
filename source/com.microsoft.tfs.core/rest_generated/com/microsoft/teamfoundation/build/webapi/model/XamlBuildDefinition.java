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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonSerialize(using = JsonSerializer.None.class)
public class XamlBuildDefinition
    extends DefinitionReference {

    private ReferenceLinks _links;
    /**
    * Batch size of the definition
    */
    private int batchSize;
    private String buildArgs;
    /**
    * The continuous integration quiet period
    */
    private int continuousIntegrationQuietPeriod;
    /**
    * The build controller
    */
    private BuildController controller;
    /**
    * The date this definition was created
    */
    private Date createdOn;
    /**
    * Default drop location for builds from this definition
    */
    private String defaultDropLocation;
    /**
    * Description of the definition
    */
    private String description;
    /**
    * The last build on this definition
    */
    private ShallowReference lastBuild;
    /**
    * The reasons supported by the template
    */
    private BuildReason supportedReasons;
    /**
    * How builds are triggered from this definition
    */
    private DefinitionTriggerType triggerType;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    /**
    * Batch size of the definition
    */
    public int getBatchSize() {
        return batchSize;
    }

    /**
    * Batch size of the definition
    */
    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public String getBuildArgs() {
        return buildArgs;
    }

    public void setBuildArgs(final String buildArgs) {
        this.buildArgs = buildArgs;
    }

    /**
    * The continuous integration quiet period
    */
    public int getContinuousIntegrationQuietPeriod() {
        return continuousIntegrationQuietPeriod;
    }

    /**
    * The continuous integration quiet period
    */
    public void setContinuousIntegrationQuietPeriod(final int continuousIntegrationQuietPeriod) {
        this.continuousIntegrationQuietPeriod = continuousIntegrationQuietPeriod;
    }

    /**
    * The build controller
    */
    public BuildController getController() {
        return controller;
    }

    /**
    * The build controller
    */
    public void setController(final BuildController controller) {
        this.controller = controller;
    }

    /**
    * The date this definition was created
    */
    public Date getCreatedOn() {
        return createdOn;
    }

    /**
    * The date this definition was created
    */
    public void setCreatedOn(final Date createdOn) {
        this.createdOn = createdOn;
    }

    /**
    * Default drop location for builds from this definition
    */
    public String getDefaultDropLocation() {
        return defaultDropLocation;
    }

    /**
    * Default drop location for builds from this definition
    */
    public void setDefaultDropLocation(final String defaultDropLocation) {
        this.defaultDropLocation = defaultDropLocation;
    }

    /**
    * Description of the definition
    */
    public String getDescription() {
        return description;
    }

    /**
    * Description of the definition
    */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
    * The last build on this definition
    */
    public ShallowReference getLastBuild() {
        return lastBuild;
    }

    /**
    * The last build on this definition
    */
    public void setLastBuild(final ShallowReference lastBuild) {
        this.lastBuild = lastBuild;
    }

    /**
    * The reasons supported by the template
    */
    public BuildReason getSupportedReasons() {
        return supportedReasons;
    }

    /**
    * The reasons supported by the template
    */
    public void setSupportedReasons(final BuildReason supportedReasons) {
        this.supportedReasons = supportedReasons;
    }

    /**
    * How builds are triggered from this definition
    */
    public DefinitionTriggerType getTriggerType() {
        return triggerType;
    }

    /**
    * How builds are triggered from this definition
    */
    public void setTriggerType(final DefinitionTriggerType triggerType) {
        this.triggerType = triggerType;
    }
}
