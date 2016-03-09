// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.dao;

/**
 * Models an entry from the link type WIT metadata table.
 */
public class WorkItemLinkTypeMetadata {
    private final String referenceName;
    private final String forwardName;
    private final String reverseName;
    private final int forwardId;
    private final int reverseId;
    private final int rules;

    /**
     * The constructor.
     *
     * @param referenceName
     *        The reference name for this link type.
     *
     * @param forwardName
     *        The forward name for this link type.
     *
     * @param forwardId
     *        The identifier for a forward link of this type.
     *
     * @param reverseName
     *        The reverse name for this link type.
     *
     * @param reverseId
     *        The identifier for a reverse link of this type.
     *
     * @param rules
     *        The rules mask for the link.
     */
    public WorkItemLinkTypeMetadata(
        final String referenceName,
        final String forwardName,
        final int forwardId,
        final String reverseName,
        final int reverseId,
        final int rules) {
        this.referenceName = referenceName;
        this.forwardName = forwardName;
        this.reverseName = reverseName;
        this.forwardId = forwardId;
        this.reverseId = reverseId;
        this.rules = rules;
    }

    /**
     * Returns the link type's reference name.
     */
    public String getReferenceName() {
        return referenceName;
    }

    /**
     * Returns the link type's forward link name.
     */
    public String getForwardName() {
        return forwardName;
    }

    /**
     * Returns the link type's reverse link name.
     */
    public String getReverseName() {
        return reverseName;
    }

    /**
     * Return the link type's forward link identifier.
     */
    public int getForwardID() {
        return forwardId;
    }

    /**
     * Returns the link type's reverse link identifier.
     */
    public int getReverseID() {
        return reverseId;
    }

    /**
     * Returns the rules mask for this link type.
     */
    public int getRules() {
        return rules;
    }

}
