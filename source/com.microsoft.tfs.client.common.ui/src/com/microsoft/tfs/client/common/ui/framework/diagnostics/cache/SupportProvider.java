// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;

public class SupportProvider {
    private final String id;
    private final String dialogTitle;
    private final String dialogText;
    private final String exportFilenamePrefix;
    private ImageDescriptor dialogImage;
    private ImageDescriptor dialogImageLeft;
    private ImageDescriptor dialogImageRight;

    private final List supportContacts = new ArrayList();

    public SupportProvider(
        final String id,
        final String dialogTitle,
        final String dialogText,
        final String exportFilenamePrefix,
        final ImageDescriptor dialogImage) {
        this.id = id;
        this.dialogTitle = dialogTitle;
        this.dialogText = dialogText;
        this.exportFilenamePrefix = exportFilenamePrefix;
        this.dialogImage = dialogImage;
    }

    public SupportProvider(
        final String id,
        final String dialogTitle,
        final String dialogText,
        final String exportFilenamePrefix,
        final ImageDescriptor dialogImageLeft,
        final ImageDescriptor dialogImageRight) {
        this.id = id;
        this.dialogTitle = dialogTitle;
        this.dialogText = dialogText;
        this.exportFilenamePrefix = exportFilenamePrefix;
        this.dialogImageLeft = dialogImageLeft;
        this.dialogImageRight = dialogImageRight;
    }

    void addSupportContact(final SupportContact supportContact) {
        supportContacts.add(supportContact);
    }

    public ImageDescriptor getDialogImage() {
        return dialogImage;
    }

    public ImageDescriptor getDialogImageLeft() {
        return dialogImageLeft;
    }

    public ImageDescriptor getDialogImageRight() {
        return dialogImageRight;
    }

    public String getDialogText() {
        return dialogText;
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    public String getID() {
        return id;
    }

    public SupportContact[] getSupportContacts() {
        return (SupportContact[]) supportContacts.toArray(new SupportContact[supportContacts.size()]);
    }

    public String getExportFilenamePrefix() {
        return exportFilenamePrefix;
    }

    public SupportContactCategory[] getSortedContactCategories() {
        final List categories = new ArrayList();

        for (final Iterator it = supportContacts.iterator(); it.hasNext();) {
            final SupportContact contact = (SupportContact) it.next();
            if (!categories.contains(contact.getCategory())) {
                categories.add(contact.getCategory());
            }
        }

        Collections.sort(categories);

        return (SupportContactCategory[]) categories.toArray(new SupportContactCategory[categories.size()]);
    }

    public SupportContact[] getSortedContactsForCategory(final SupportContactCategory category) {
        final List contacts = new ArrayList();

        for (final Iterator it = supportContacts.iterator(); it.hasNext();) {
            final SupportContact contact = (SupportContact) it.next();
            if (category == contact.getCategory()) {
                contacts.add(contact);
            }
        }

        Collections.sort(contacts);

        return (SupportContact[]) contacts.toArray(new SupportContact[contacts.size()]);
    }
}
