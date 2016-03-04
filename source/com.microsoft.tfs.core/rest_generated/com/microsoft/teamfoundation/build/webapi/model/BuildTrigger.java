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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/** 
 */
@JsonDeserialize(using = BuildTriggerDeserializer.class)
@JsonSerialize(using = BuildTriggerSerializer.class)
public class BuildTrigger {

    private DefinitionTriggerType triggerType;

    public DefinitionTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(final DefinitionTriggerType triggerType) {
        this.triggerType = triggerType;
    }
}
