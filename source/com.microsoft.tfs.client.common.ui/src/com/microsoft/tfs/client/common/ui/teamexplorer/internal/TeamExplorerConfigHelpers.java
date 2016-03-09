// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;

public class TeamExplorerConfigHelpers {
    private static final Log log = LogFactory.getLog(TeamExplorerConfigHelpers.class);

    public static int getInteger(final IConfigurationElement element, final String attributeName) {
        try {
            return Integer.parseInt(element.getAttribute(attributeName));
        } catch (final NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    public static Image getIcon(final IConfigurationElement element, final String attributeName) {
        final String relativePath = element.getAttribute(attributeName);
        if (relativePath == null || relativePath.length() == 0) {
            final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
            return imageHelper.getImage("/images/teamexplorer/SamplePage.png"); //$NON-NLS-1$
        } else {
            final ImageHelper imageHelper = new ImageHelper(element.getContributor().getName());
            final Image image = imageHelper.getImage(relativePath);
            return image;
        }
    }

    public static Object createInstance(final IConfigurationElement element, final String id) {
        try {
            return element.createExecutableExtension(TeamExplorerBaseConfig.CLASS_ATTR_NAME);
        } catch (final CoreException e) {
            log.error(MessageFormat.format("Error creating TE class {0}", id), e); //$NON-NLS-1$
            return null;
        }
    }
}
