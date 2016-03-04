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

import java.util.HashMap;

/** 
 */
public class BuildOption {

    private BuildOptionDefinitionReference definition;
    private boolean enabled;
    private HashMap<String,String> inputs;

    public BuildOptionDefinitionReference getDefinition() {
        return definition;
    }

    public void setDefinition(final BuildOptionDefinitionReference definition) {
        this.definition = definition;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public HashMap<String,String> getInputs() {
        return inputs;
    }

    public void setInputs(final HashMap<String,String> inputs) {
        this.inputs = inputs;
    }
}
