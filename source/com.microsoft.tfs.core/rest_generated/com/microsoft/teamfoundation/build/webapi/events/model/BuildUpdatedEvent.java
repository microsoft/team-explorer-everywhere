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

package com.microsoft.teamfoundation.build.webapi.events.model;

import com.microsoft.teamfoundation.build.webapi.model.Build;

/** 
 */
public class BuildUpdatedEvent
    extends RealtimeBuildEvent {

    private Build build;

    public Build getBuild() {
        return build;
    }

    public void setBuild(final Build build) {
        this.build = build;
    }
}
