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

import java.net.URI;
import java.util.UUID;

/** 
 */
public class TaskOrchestrationPlanReference {

    private URI artifactLocation;
    private URI artifactUri;
    private UUID planId;
    private String planType;
    private UUID scopeIdentifier;
    private int version;

    public URI getArtifactLocation() {
        return artifactLocation;
    }

    public void setArtifactLocation(final URI artifactLocation) {
        this.artifactLocation = artifactLocation;
    }

    public URI getArtifactUri() {
        return artifactUri;
    }

    public void setArtifactUri(final URI artifactUri) {
        this.artifactUri = artifactUri;
    }

    public UUID getPlanId() {
        return planId;
    }

    public void setPlanId(final UUID planId) {
        this.planId = planId;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(final String planType) {
        this.planType = planType;
    }

    public UUID getScopeIdentifier() {
        return scopeIdentifier;
    }

    public void setScopeIdentifier(final UUID scopeIdentifier) {
        this.scopeIdentifier = scopeIdentifier;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }
}
