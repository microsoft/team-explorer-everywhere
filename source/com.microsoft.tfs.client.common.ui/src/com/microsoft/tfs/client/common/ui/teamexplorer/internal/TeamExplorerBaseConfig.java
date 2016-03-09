// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import org.eclipse.core.runtime.IConfigurationElement;

public class TeamExplorerBaseConfig {
    // Extension attribute names.
    protected final static String ID_ATTR_NAME = "id"; //$NON-NLS-1$
    protected final static String TITLE_ATTR_NAME = "title"; //$NON-NLS-1$
    protected final static String PRIORITY_ATTR_NAME = "displayPriority"; //$NON-NLS-1$
    protected final static String TARGETPAGEID_ATTR_NAME = "targetPageID"; //$NON-NLS-1$
    protected final static String ICON_ATTR_NAME = "icon"; //$NON-NLS-1$
    protected final static String CLASS_ATTR_NAME = "class"; //$NON-NLS-1$
    protected final static String PARENTID_ATTR_NAME = "parentID"; //$NON-NLS-1$
    protected final static String TARGETVIEWID_ATTR_NAME = "viewID"; //$NON-NLS-1$

    private final String id;
    private final String title;
    private final IConfigurationElement element;

    public TeamExplorerBaseConfig(final IConfigurationElement element) {
        this.element = element;
        this.id = element.getAttribute(ID_ATTR_NAME);
        this.title = element.getAttribute(TITLE_ATTR_NAME);
    }

    public String getID() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public IConfigurationElement getElement() {
        return element;
    }
}
