// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.browser;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class BrowserEditorInput implements IEditorInput {
    private final String url;
    private final String title;
    private final String tooltip;
    private final String browserId;

    public BrowserEditorInput(final String url, final String title, final String tooltip, final String browserId) {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null"); //$NON-NLS-1$
        }
        if (title == null) {
            throw new IllegalArgumentException("title must not be null"); //$NON-NLS-1$
        }
        if (tooltip == null) {
            throw new IllegalArgumentException("tooltip must not be null"); //$NON-NLS-1$
        }
        if (browserId == null) {
            throw new IllegalArgumentException("browserId must not be null"); //$NON-NLS-1$
        }

        this.url = url;
        this.title = title;
        this.tooltip = tooltip;
        this.browserId = browserId;
    }

    public String getURL() {
        return url;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof BrowserEditorInput) {
            final BrowserEditorInput other = (BrowserEditorInput) obj;
            return browserId.equals(other.browserId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return browserId.hashCode();
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return title;
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return tooltip;
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }
}
