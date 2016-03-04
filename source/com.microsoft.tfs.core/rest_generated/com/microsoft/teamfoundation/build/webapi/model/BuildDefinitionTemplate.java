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

import java.util.UUID;

/** 
 */
public class BuildDefinitionTemplate {

    private boolean canDelete;
    private String category;
    private String description;
    private UUID iconTaskId;
    private String id;
    private String name;
    private BuildDefinition template;

    public boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(final boolean canDelete) {
        this.canDelete = canDelete;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public UUID getIconTaskId() {
        return iconTaskId;
    }

    public void setIconTaskId(final UUID iconTaskId) {
        this.iconTaskId = iconTaskId;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public BuildDefinition getTemplate() {
        return template;
    }

    public void setTemplate(final BuildDefinition template) {
        this.template = template;
    }
}
