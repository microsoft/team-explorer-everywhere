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

package com.microsoft.teamfoundation.build.webapi.events.model;

import com.microsoft.teamfoundation.build.webapi.model.AuditAction;
import com.microsoft.teamfoundation.build.webapi.model.BuildDefinition;

/** 
 */
public class BuildDefinitionChangingEvent {

    private AuditAction changeType;
    private BuildDefinition newDefinition;
    private BuildDefinition originalDefinition;

    public AuditAction getChangeType() {
        return changeType;
    }

    public void setChangeType(final AuditAction changeType) {
        this.changeType = changeType;
    }

    public BuildDefinition getNewDefinition() {
        return newDefinition;
    }

    public void setNewDefinition(final BuildDefinition newDefinition) {
        this.newDefinition = newDefinition;
    }

    public BuildDefinition getOriginalDefinition() {
        return originalDefinition;
    }

    public void setOriginalDefinition(final BuildDefinition originalDefinition) {
        this.originalDefinition = originalDefinition;
    }
}
