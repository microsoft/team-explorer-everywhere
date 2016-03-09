// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.links;

import java.text.MessageFormat;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.update.ElementHandler;

/**
 * The base class for a work item component: either a file attachment or one of
 * the link types.
 */
public abstract class WITComponent {
    /*
     * True if this component has been newly created by the end user. The
     * component does not exist on the server, and when the parent work item is
     * saved the component will be added to the server. The parent work item may
     * or may not be a newly created work item.
     */
    private boolean newlyCreated;

    /*
     * True if this component is pending deletion. The component exists on the
     * server and the parent work item is not a newly created work item. When
     * the parent work item is saved, this component will be deleted from the
     * server.
     */
    private boolean pendingDeletion;

    /*
     * True if this component is pending some modification. The component exists
     * on the server, and the parent work item is not newly saved. When the
     * parent work item is saved, the server version of this component will be
     * updated.
     */
    private boolean pendingModification;

    /*
     * The external id of this component (if any)
     */
    private int extId = -1;

    /*
     * the associated component collection (if any)
     */
    private WITComponentCollection associatedCollection;

    /**
     * Creates a new instance of a WITComponent. The initial state (old from the
     * server or newly created) must be specified.
     *
     * @param newlyCreated
     *        true if created by the user
     */
    protected WITComponent(final boolean newlyCreated) {
        this.newlyCreated = newlyCreated;
    }

    /**
     * @return true if this component has been newly created by the user
     */
    public final boolean isNewlyCreated() {
        return newlyCreated;
    }

    /**
     * Sets the external ID of this component.
     *
     * @param extId
     *        the external ID
     */
    public final void setExtID(final int extId) {
        this.extId = extId;
    }

    /**
     * @return the external ID of this component
     */
    public final int getExtID() {
        return extId;
    }

    /**
     * @return true if this component should be counted as part of an "old"
     *         count
     */
    public final boolean shouldIncludeInOldCount() {
        return !newlyCreated;
    }

    /**
     * @return true if this component should be counted as part of a "new" count
     */
    public final boolean shouldIncludeInNewCount() {
        return !pendingDeletion;
    }

    /**
     * Associates this component with a component collection.
     *
     * @param collection
     *        the WITComponentCollection to associate with
     */
    public final void associate(final WITComponentCollection collection) {
        if (associatedCollection != null) {
            if (associatedCollection == collection) {
                return;
            }
            throw new IllegalStateException("this component is already associated with another collection"); //$NON-NLS-1$
        }

        associatedCollection = collection;
        associatedCollection.getComponentSet().add(this);
    }

    /**
     * Deletes this component.
     */
    public final void delete() {
        if (associatedCollection == null) {
            return;
        }

        if (newlyCreated) {
            disassociate();
        } else {
            pendingDeletion = true;
        }
    }

    /**
     * Called after a server update.
     */
    public final void update() {
        onUpdate();
        if (newlyCreated) {
            newlyCreated = false;
        } else if (pendingDeletion) {
            disassociate();
            pendingDeletion = false;
        } else if (pendingModification) {
            pendingModification = false;
        }
    }

    public void reset() {
        if (newlyCreated) {
            disassociate();
        }

        /*
         * Right now it looks like a component could be both pending
         * modification and pending deletion. Technically this shouldn't be
         * allowed, but also shouldn't cause problems. Should probably change
         * things so that the three states (newlyCreated, pendingModification,
         * pendingDeletion) are always mutually exclusive.
         */

        if (pendingDeletion) {
            pendingDeletion = false;
        }

        if (pendingModification) {
            pendingModification = false;
        }
    }

    /**
     * @return true if this component is dirty (needs saving to the server)
     */
    public final boolean isDirty() {
        return (newlyCreated && associatedCollection != null) || pendingModification || pendingDeletion;
    }

    /**
     * @return true if this component is pending deletion (deleted from UI but
     *         not the server)
     */
    public final boolean isPendingDelete() {
        return pendingDeletion;
    }

    /**
     * Creates XML for the update package.
     *
     * @param parentElement
     *        the parent element to use
     * @return an ElementHandler to process the response, or null
     */
    public final ElementHandler createXMLForUpdatePackage(final Element parentElement) {
        if (newlyCreated) {
            createXMLForAdd(parentElement);
            if (getInsertTagName() != null) {
                return new ComponentExtIDHandler(this, getInsertTagName());
            }
        } else if (pendingDeletion) {
            createXMLForRemove(parentElement);
        }
        return null;
    }

    /**
     * @return true if this component should be included as part of the public
     *         set of components in the associated collection
     */
    public final boolean shouldIncludeAsPartOfPublicCollection() {
        return associatedCollection != null && !pendingDeletion;
    }

    /**
     * @param other
     *        another WITComponent to compare to
     * @return true if this component is considered equivalent to the argument
     */
    protected abstract boolean isEquivalentTo(WITComponent other);

    protected abstract void createXMLForAdd(Element parentElement);

    protected abstract String getInsertTagName();

    protected abstract void createXMLForRemove(Element parentElement);

    /**
     * Component subclasses can override to do custom processing on update.
     */
    protected void onUpdate() {

    }

    /**
     * Throws an IllegalArgumentException if the input is longer than a maximum
     * length.
     *
     * @param input
     *        input string to test
     * @param argumentName
     *        the argument name to use in the exception message
     * @param maxLength
     *        the maximum length to validate with
     */
    protected final void validateTextMaxLength(final String input, final String argumentName, final int maxLength) {
        if (input != null && input.length() > maxLength) {

            throw new IllegalArgumentException(
                MessageFormat.format(
                    "invalid length ({0}) of argument \"{1}\" - max length is {2}", //$NON-NLS-1$
                    input.length(),
                    argumentName,
                    maxLength));
        }
    }

    protected final WITComponentCollection getAssociatedCollection() {
        return associatedCollection;
    }

    protected final boolean isPendingModification() {
        return pendingModification;
    }

    protected final void setPendingModification(final boolean pendingModification) {
        this.pendingModification = pendingModification;
    }

    private void disassociate() {
        associatedCollection.getComponentSet().remove(this);
        associatedCollection = null;
    }
}
