// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import com.microsoft.tfs.core.clients.workitem.internal.links.WITComponent;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkTextMaxLengths;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;

public abstract class LinkImpl extends WITComponent implements Link {
    /*
     * the RegisteredLinkType of this link
     */
    private final RegisteredLinkType linkType;

    /*
     * all link types have a comment
     */
    private String originalComment;
    private String newComment;

    /*
     * some links cannot be modified or deleted
     */
    private final boolean readOnly;

    /*
     * link types have a description that's used to provide a human-readable
     * (friendly) display name for the link
     */
    private String description;
    private boolean descriptionComputed = false;

    protected LinkImpl(
        final RegisteredLinkType linkType,
        final String comment,
        final int extId,
        final boolean newComponent,
        final boolean readOnly) {
        super(newComponent);

        this.linkType = linkType;
        setExtID(extId);

        validateTextMaxLength(comment, "comment", LinkTextMaxLengths.COMMENT_MAX_LENGTH); //$NON-NLS-1$
        originalComment = comment;
        this.readOnly = readOnly;
    }

    /*
     * ************************************************************************
     * START of implementation of Link interface
     * ***********************************************************************
     */

    @Override
    public int getLinkID() {
        return getExtID();
    }

    @Override
    public RegisteredLinkType getLinkType() {
        return linkType;
    }

    @Override
    public String getComment() {
        if (isPendingModification()) {
            return newComment;
        } else {
            return originalComment;
        }
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setComment(final String commentInput) {
        validateTextMaxLength(commentInput, "comment", LinkTextMaxLengths.COMMENT_MAX_LENGTH); //$NON-NLS-1$

        if (isNewlyCreated()) {
            originalComment = commentInput;
            return;
        }

        final boolean equalToOriginal =
            (commentInput == null ? originalComment == null : commentInput.equals(originalComment));

        if (isPendingModification()) {
            if (equalToOriginal) {
                setPendingModification(false);
                getAssociatedCollection().possiblyChangedDirtyState();
            }
        } else {
            if (!equalToOriginal) {
                newComment = commentInput;
                setPendingModification(true);
                getAssociatedCollection().possiblyChangedDirtyState();
            }
        }
    }

    @Override
    public String getDescription() {
        if (!descriptionComputed) {
            return getFallbackDescription();
        }
        return description;
    }

    /*
     * ************************************************************************
     * END of implementation of Link interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of implementation of internal (LinkImpl) methods
     * ***********************************************************************
     */

    public void setDescription(final String description) {
        this.description = description;
        descriptionComputed = true;
    }

    public boolean isDescriptionComputed() {
        return descriptionComputed;
    }

    @Override
    protected void onUpdate() {
        if (isPendingModification()) {
            originalComment = newComment;
        }
    }

    @Override
    protected boolean isEquivalentTo(final WITComponent other) {
        return isEquivalent((Link) other);
    }

    public abstract boolean isEquivalent(Link link);

    public abstract LinkImpl cloneLink();

    protected String getFallbackDescription() {
        return ""; //$NON-NLS-1$
    }

    /*
     * ************************************************************************
     * END of implementation of internal (LinkImpl) methods
     * ***********************************************************************
     */
}
