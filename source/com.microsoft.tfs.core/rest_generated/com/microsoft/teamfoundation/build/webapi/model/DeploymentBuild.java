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
 * Deployment iformation for type "Build"
 * 
 */
public class DeploymentBuild
    extends Deployment {

    private int buildId;

    public int getBuildId() {
        return buildId;
    }

    public void setBuildId(final int buildId) {
        this.buildId = buildId;
    }
}
