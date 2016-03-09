// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class TFSCommonUIImages {
    private static final Log log = LogFactory.getLog(TFSCommonUIImages.class);

    public static final String IMG_COMPARE = "IMG_COMPARE"; //$NON-NLS-1$
    public static final String IMG_VIEW = "IMG_VIEW"; //$NON-NLS-1$
    public static final String IMG_REFRESH = "IMG_REFRESH"; //$NON-NLS-1$
    public static final String IMG_UNDO = "IMG_UNDO"; //$NON-NLS-1$
    public static final String IMG_COPY = "IMG_COPY"; //$NON-NLS-1$
    public static final String IMG_DETAILS = "IMG_DETAILS"; //$NON-NLS-1$
    public static final String IMG_OPTIONS = "IMG_OPTIONS"; //$NON-NLS-1$

    private static final String IMAGE_FILE_PATH_PREFIX = "images/"; //$NON-NLS-1$
    private static final Map<String, ImageDescriptor> descriptors = new HashMap<String, ImageDescriptor>();

    public static ImageDescriptor getImageDescriptor(final String symbolicName) {
        return descriptors.get(symbolicName);
    }

    public static ImageDescriptor newImageDescriptorFromFile(final String imageFileName) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSCommonUIClientPlugin.PLUGIN_ID,
            IMAGE_FILE_PATH_PREFIX + imageFileName);
    }

    private static void addDescriptor(final String symbolicName, final String imageFileName) {
        try {
            final ImageDescriptor descriptor = newImageDescriptorFromFile(imageFileName);
            descriptors.put(symbolicName, descriptor);
        } catch (final Exception e) {
            final String messageFormat = "unable to load image [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, imageFileName);
            log.warn(message, e);
        }
    }

    private static void addWorkbenchImageDescriptor(final String ourSymbolicName, final String workbenchSymbolicName) {
        final ImageDescriptor descriptor =
            PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(workbenchSymbolicName);
        descriptors.put(ourSymbolicName, descriptor);
    }

    static {
        addDescriptor(IMG_COMPARE, "vc/compare.gif"); //$NON-NLS-1$
        addDescriptor(IMG_REFRESH, "common/refresh.gif"); //$NON-NLS-1$
        addDescriptor(IMG_UNDO, "vc/undo.gif"); //$NON-NLS-1$
        addDescriptor(IMG_OPTIONS, "common/options.gif"); //$NON-NLS-1$

        addWorkbenchImageDescriptor(IMG_VIEW, ISharedImages.IMG_OBJ_FOLDER);
        addWorkbenchImageDescriptor(IMG_COPY, ISharedImages.IMG_TOOL_COPY);
        addWorkbenchImageDescriptor(IMG_DETAILS, ISharedImages.IMG_OBJ_ELEMENT);
    }
}
