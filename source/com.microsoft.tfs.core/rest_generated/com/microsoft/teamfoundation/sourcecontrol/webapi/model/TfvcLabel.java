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

package com.microsoft.teamfoundation.sourcecontrol.webapi.model;

import java.util.List;

/** 
 */
public class TfvcLabel
    extends TfvcLabelRef {

    private List<TfvcItem> items;

    public List<TfvcItem> getItems() {
        return items;
    }

    public void setItems(final List<TfvcItem> items) {
        this.items = items;
    }
}
