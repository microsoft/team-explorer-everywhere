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


/** 
 */
public class BuildDefinitionVariable {

    private boolean allowOverride;
    private boolean isSecret;
    private String value;

    public boolean getAllowOverride() {
        return allowOverride;
    }

    public void setAllowOverride(final boolean allowOverride) {
        this.allowOverride = allowOverride;
    }

    public boolean getIsSecret() {
        return isSecret;
    }

    public void setIsSecret(final boolean isSecret) {
        this.isSecret = isSecret;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}
