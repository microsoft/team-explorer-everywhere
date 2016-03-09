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
public class BuildSettings {

    private RetentionPolicy defaultRetentionPolicy;
    private RetentionPolicy maximumRetentionPolicy;

    public RetentionPolicy getDefaultRetentionPolicy() {
        return defaultRetentionPolicy;
    }

    public void setDefaultRetentionPolicy(final RetentionPolicy defaultRetentionPolicy) {
        this.defaultRetentionPolicy = defaultRetentionPolicy;
    }

    public RetentionPolicy getMaximumRetentionPolicy() {
        return maximumRetentionPolicy;
    }

    public void setMaximumRetentionPolicy(final RetentionPolicy maximumRetentionPolicy) {
        this.maximumRetentionPolicy = maximumRetentionPolicy;
    }
}
