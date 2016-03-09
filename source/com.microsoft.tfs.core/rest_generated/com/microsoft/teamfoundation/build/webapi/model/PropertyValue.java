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
import java.util.UUID;

/** 
 */
public class PropertyValue {

    /**
    * Guid of identity that changed this property value
    */
    private UUID changedBy;
    /**
    * The date this property value was changed
    */
    private Date changedDate;
    /**
    * Name in the name value mapping
    */
    private String propertyName;
    /**
    * Value in the name value mapping
    */
    private Object value;

    /**
    * Guid of identity that changed this property value
    */
    public UUID getChangedBy() {
        return changedBy;
    }

    /**
    * Guid of identity that changed this property value
    */
    public void setChangedBy(final UUID changedBy) {
        this.changedBy = changedBy;
    }

    /**
    * The date this property value was changed
    */
    public Date getChangedDate() {
        return changedDate;
    }

    /**
    * The date this property value was changed
    */
    public void setChangedDate(final Date changedDate) {
        this.changedDate = changedDate;
    }

    /**
    * Name in the name value mapping
    */
    public String getPropertyName() {
        return propertyName;
    }

    /**
    * Name in the name value mapping
    */
    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }

    /**
    * Value in the name value mapping
    */
    public Object getValue() {
        return value;
    }

    /**
    * Value in the name value mapping
    */
    public void setValue(final Object value) {
        this.value = value;
    }
}
