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
public class BuildDefinitionChangedEvent {

    private AuditAction changeType;
    private BuildDefinition definition;

    public AuditAction getChangeType() {
        return changeType;
    }

    public void setChangeType(final AuditAction changeType) {
        this.changeType = changeType;
    }

    public BuildDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(final BuildDefinition definition) {
        this.definition = definition;
    }
}
