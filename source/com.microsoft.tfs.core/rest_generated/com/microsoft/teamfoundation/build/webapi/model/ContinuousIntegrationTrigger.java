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

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;

/** 
 */
@JsonDeserialize(using = JsonDeserializer.None.class)
@JsonSerialize(using = JsonSerializer.None.class)
public class ContinuousIntegrationTrigger
    extends BuildTrigger {

    private boolean batchChanges;
    private List<String> branchFilters;

    public boolean getBatchChanges() {
        return batchChanges;
    }

    public void setBatchChanges(final boolean batchChanges) {
        this.batchChanges = batchChanges;
    }

    public List<String> getBranchFilters() {
        return branchFilters;
    }

    public void setBranchFilters(final List<String> branchFilters) {
        this.branchFilters = branchFilters;
    }
}
