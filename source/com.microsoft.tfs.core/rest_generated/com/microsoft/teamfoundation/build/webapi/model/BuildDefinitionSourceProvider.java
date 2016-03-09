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
import java.util.HashMap;

/** 
 */
public class BuildDefinitionSourceProvider {

    /**
    * Uri of the associated definition
    */
    private String definitionUri;
    /**
    * fields associated with this build definition
    */
    private HashMap<String,String> fields;
    /**
    * Id of this source provider
    */
    private int id;
    /**
    * The lst time this source provider was modified
    */
    private Date lastModified;
    /**
    * Name of the source provider
    */
    private String name;
    /**
    * Which trigger types are supported by this definition source provider
    */
    private DefinitionTriggerType supportedTriggerTypes;

    /**
    * Uri of the associated definition
    */
    public String getDefinitionUri() {
        return definitionUri;
    }

    /**
    * Uri of the associated definition
    */
    public void setDefinitionUri(final String definitionUri) {
        this.definitionUri = definitionUri;
    }

    /**
    * fields associated with this build definition
    */
    public HashMap<String,String> getFields() {
        return fields;
    }

    /**
    * fields associated with this build definition
    */
    public void setFields(final HashMap<String,String> fields) {
        this.fields = fields;
    }

    /**
    * Id of this source provider
    */
    public int getId() {
        return id;
    }

    /**
    * Id of this source provider
    */
    public void setId(final int id) {
        this.id = id;
    }

    /**
    * The lst time this source provider was modified
    */
    public Date getLastModified() {
        return lastModified;
    }

    /**
    * The lst time this source provider was modified
    */
    public void setLastModified(final Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
    * Name of the source provider
    */
    public String getName() {
        return name;
    }

    /**
    * Name of the source provider
    */
    public void setName(final String name) {
        this.name = name;
    }

    /**
    * Which trigger types are supported by this definition source provider
    */
    public DefinitionTriggerType getSupportedTriggerTypes() {
        return supportedTriggerTypes;
    }

    /**
    * Which trigger types are supported by this definition source provider
    */
    public void setSupportedTriggerTypes(final DefinitionTriggerType supportedTriggerTypes) {
        this.supportedTriggerTypes = supportedTriggerTypes;
    }
}
