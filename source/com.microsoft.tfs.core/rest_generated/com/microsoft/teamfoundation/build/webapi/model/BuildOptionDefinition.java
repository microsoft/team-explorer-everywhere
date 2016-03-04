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

/** 
 */
public class BuildOptionDefinition
    extends BuildOptionDefinitionReference {

    private String description;
    private List<BuildOptionGroupDefinition> groups;
    private List<BuildOptionInputDefinition> inputs;
    private String name;
    private int ordinal;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<BuildOptionGroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(final List<BuildOptionGroupDefinition> groups) {
        this.groups = groups;
    }

    public List<BuildOptionInputDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(final List<BuildOptionInputDefinition> inputs) {
        this.inputs = inputs;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(final int ordinal) {
        this.ordinal = ordinal;
    }
}
