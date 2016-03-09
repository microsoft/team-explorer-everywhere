// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;

/**
 * Defines the interface for link control providers. A link control provider is
 * a non-stateless, non-threadsafe pluggable interface for handling work item
 * links.
 */
public interface LinkControlProvider {
    /**
     * Computes a human-friendly display name for a link type. Can be called at
     * any time.
     *
     * @param linkType
     *        link type to compute display name for
     * @return a display name for the given link type
     */
    public String getDisplayName(RegisteredLinkType linkType);

    /**
     * Creates a UI for creating or editing a link.
     *
     * @param composite
     *        the parent composite to create the UI in
     */
    public void initialize(Composite composite);

    /**
     * Validates the data in the UI.
     *
     * @param forEdit
     *        true if the UI is being used for editing an existing link
     * @param linkingWorkItem
     *        the work item the link would be added to
     * @return true if the data in the UI is in a valid state
     */
    public boolean isValid(boolean forEdit, WorkItem linkingWorkItem);

    /**
     * Obtain any error message due to invalid data in the UI. Will only be
     * called after a call to isValid() returns false.
     *
     * @return an error message
     */
    public String getErrorMessage();

    /**
     * Creates new link(s) based off data in the UI. Will only be called after a
     * call to isValid() returns true.
     *
     * @return a new link
     */
    public Link[] getLinks();
}
