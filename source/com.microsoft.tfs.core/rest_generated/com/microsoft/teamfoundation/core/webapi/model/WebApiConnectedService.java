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

import com.microsoft.visualstudio.services.webapi.model.IdentityRef;

/** 
 */
public class WebApiConnectedService
    extends WebApiConnectedServiceRef {

    /**
    * The user who did the OAuth authentication to created this service
    */
    private IdentityRef authenticatedBy;
    /**
    * Extra description on the service.
    */
    private String description;
    /**
    * Friendly Name of service connection
    */
    private String friendlyName;
    /**
    * Id/Name of the connection service. For Ex: Subscription Id for Azure Connection
    */
    private String id;
    /**
    * The kind of service.
    */
    private String kind;
    /**
    * The project associated with this service
    */
    private TeamProjectReference project;
    /**
    * Optional uri to connect directly to the service such as https://windows.azure.com
    */
    private String serviceUri;

    /**
    * The user who did the OAuth authentication to created this service
    */
    public IdentityRef getAuthenticatedBy() {
        return authenticatedBy;
    }

    /**
    * The user who did the OAuth authentication to created this service
    */
    public void setAuthenticatedBy(final IdentityRef authenticatedBy) {
        this.authenticatedBy = authenticatedBy;
    }

    /**
    * Extra description on the service.
    */
    public String getDescription() {
        return description;
    }

    /**
    * Extra description on the service.
    */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
    * Friendly Name of service connection
    */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
    * Friendly Name of service connection
    */
    public void setFriendlyName(final String friendlyName) {
        this.friendlyName = friendlyName;
    }

    /**
    * Id/Name of the connection service. For Ex: Subscription Id for Azure Connection
    */
    public String getId() {
        return id;
    }

    /**
    * Id/Name of the connection service. For Ex: Subscription Id for Azure Connection
    */
    public void setId(final String id) {
        this.id = id;
    }

    /**
    * The kind of service.
    */
    public String getKind() {
        return kind;
    }

    /**
    * The kind of service.
    */
    public void setKind(final String kind) {
        this.kind = kind;
    }

    /**
    * The project associated with this service
    */
    public TeamProjectReference getProject() {
        return project;
    }

    /**
    * The project associated with this service
    */
    public void setProject(final TeamProjectReference project) {
        this.project = project;
    }

    /**
    * Optional uri to connect directly to the service such as https://windows.azure.com
    */
    public String getServiceUri() {
        return serviceUri;
    }

    /**
    * Optional uri to connect directly to the service such as https://windows.azure.com
    */
    public void setServiceUri(final String serviceUri) {
        this.serviceUri = serviceUri;
    }
}
