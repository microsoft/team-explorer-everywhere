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

import java.net.URI;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.visualstudio.services.webapi.model.ReferenceLinks;

/** 
 */
public class BuildController
    extends ShallowReference {

    private ReferenceLinks _links;
    /**
    * The date the controller was created.
    */
    private Date createdDate;
    /**
    * The description of the controller.
    */
    private String description;
    /**
    * Indicates whether the controller is enabled.
    */
    private boolean enabled;
    /**
    * The status of the controller.
    */
    private ControllerStatus status;
    /**
    * The date the controller was last updated.
    */
    private Date updatedDate;
    /**
    * The controller's URI.
    */
    private URI uri;

    @JsonProperty("_links")
    public ReferenceLinks getLinks() {
        return _links;
    }

    @JsonProperty("_links")
    public void setLinks(final ReferenceLinks _links) {
        this._links = _links;
    }

    /**
    * The date the controller was created.
    */
    public Date getCreatedDate() {
        return createdDate;
    }

    /**
    * The date the controller was created.
    */
    public void setCreatedDate(final Date createdDate) {
        this.createdDate = createdDate;
    }

    /**
    * The description of the controller.
    */
    public String getDescription() {
        return description;
    }

    /**
    * The description of the controller.
    */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
    * Indicates whether the controller is enabled.
    */
    public boolean getEnabled() {
        return enabled;
    }

    /**
    * Indicates whether the controller is enabled.
    */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
    * The status of the controller.
    */
    public ControllerStatus getStatus() {
        return status;
    }

    /**
    * The status of the controller.
    */
    public void setStatus(final ControllerStatus status) {
        this.status = status;
    }

    /**
    * The date the controller was last updated.
    */
    public Date getUpdatedDate() {
        return updatedDate;
    }

    /**
    * The date the controller was last updated.
    */
    public void setUpdatedDate(final Date updatedDate) {
        this.updatedDate = updatedDate;
    }

    /**
    * The controller's URI.
    */
    public URI getUri() {
        return uri;
    }

    /**
    * The controller's URI.
    */
    public void setUri(final URI uri) {
        this.uri = uri;
    }
}
