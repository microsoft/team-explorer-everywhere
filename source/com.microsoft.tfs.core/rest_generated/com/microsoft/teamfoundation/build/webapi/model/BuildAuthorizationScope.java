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
public enum BuildAuthorizationScope {

    /**
    * The identity used should have build service account permissions scoped to the project collection. This is useful when resources for a single build are spread across multiple projects.
    */
    PROJECT_COLLECTION(1),
    /**
    * The identity used should have build service account permissions scoped to the project in which the build definition resides. This is useful for isolation of build jobs to a particular team project to avoid any unintentional escalation of privilege attacks during a build.
    */
    PROJECT(2),
    ;

    private int value;

    private BuildAuthorizationScope(final int value) {
        this.value = value;
    }

    public int getValue()
    {
        return value;
    }

    @Override
    public String toString() {
        final String name = super.toString();

        if (name.equals("PROJECT_COLLECTION")) { //$NON-NLS-1$
            return "projectCollection"; //$NON-NLS-1$
        }

        if (name.equals("PROJECT")) { //$NON-NLS-1$
            return "project"; //$NON-NLS-1$
        }

        return null;
    }
}
