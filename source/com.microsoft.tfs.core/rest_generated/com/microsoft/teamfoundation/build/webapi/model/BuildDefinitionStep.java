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
public class BuildDefinitionStep {

    private boolean alwaysRun;
    private boolean continueOnError;
    private String displayName;
    private boolean enabled;
    private HashMap<String,String> inputs;
    private TaskDefinitionReference task;

    public boolean getAlwaysRun() {
        return alwaysRun;
    }

    public void setAlwaysRun(final boolean alwaysRun) {
        this.alwaysRun = alwaysRun;
    }

    public boolean getContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(final boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
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

    public TaskDefinitionReference getTask() {
        return task;
    }

    public void setTask(final TaskDefinitionReference task) {
        this.task = task;
    }
}
