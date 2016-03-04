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

import java.net.URI;
import java.util.Date;
import com.microsoft.visualstudio.services.webapi.model.IdentityRef;

/** 
 * Represents a change associated with a build.
 * 
 */
public class Change {

    /**
    * The author of the change.
    */
    private IdentityRef author;
    /**
    * The location of a user-friendly representation of the resource.
    */
    private URI displayUri;
    /**
    * Something that identifies the change. For a commit, this would be the SHA1. For a TFVC changeset, this would be the changeset id.
    */
    private String id;
    /**
    * The location of the full representation of the resource.
    */
    private URI location;
    /**
    * A description of the change. This might be a commit message or changeset description.
    */
    private String message;
    /**
    * Indicates whether the message was truncated
    */
    private boolean messageTruncated;
    /**
    * A timestamp for the change.
    */
    private Date timestamp;
    /**
    * The type of change. "commit", "changeset", etc.
    */
    private String type;

    /**
    * The author of the change.
    */
    public IdentityRef getAuthor() {
        return author;
    }

    /**
    * The author of the change.
    */
    public void setAuthor(final IdentityRef author) {
        this.author = author;
    }

    /**
    * The location of a user-friendly representation of the resource.
    */
    public URI getDisplayUri() {
        return displayUri;
    }

    /**
    * The location of a user-friendly representation of the resource.
    */
    public void setDisplayUri(final URI displayUri) {
        this.displayUri = displayUri;
    }

    /**
    * Something that identifies the change. For a commit, this would be the SHA1. For a TFVC changeset, this would be the changeset id.
    */
    public String getId() {
        return id;
    }

    /**
    * Something that identifies the change. For a commit, this would be the SHA1. For a TFVC changeset, this would be the changeset id.
    */
    public void setId(final String id) {
        this.id = id;
    }

    /**
    * The location of the full representation of the resource.
    */
    public URI getLocation() {
        return location;
    }

    /**
    * The location of the full representation of the resource.
    */
    public void setLocation(final URI location) {
        this.location = location;
    }

    /**
    * A description of the change. This might be a commit message or changeset description.
    */
    public String getMessage() {
        return message;
    }

    /**
    * A description of the change. This might be a commit message or changeset description.
    */
    public void setMessage(final String message) {
        this.message = message;
    }

    /**
    * Indicates whether the message was truncated
    */
    public boolean getMessageTruncated() {
        return messageTruncated;
    }

    /**
    * Indicates whether the message was truncated
    */
    public void setMessageTruncated(final boolean messageTruncated) {
        this.messageTruncated = messageTruncated;
    }

    /**
    * A timestamp for the change.
    */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
    * A timestamp for the change.
    */
    public void setTimestamp(final Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
    * The type of change. &quot;commit&quot;, &quot;changeset&quot;, etc.
    */
    public String getType() {
        return type;
    }

    /**
    * The type of change. &quot;commit&quot;, &quot;changeset&quot;, etc.
    */
    public void setType(final String type) {
        this.type = type;
    }
}
