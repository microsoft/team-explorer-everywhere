// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public class ImageCache {
    private final Map imageMap = new HashMap();

    public synchronized Image getImage(final ImageDescriptor imageDescriptor) {
        if (imageDescriptor == null) {
            return null;
        }
        Image image = (Image) imageMap.get(imageDescriptor);
        if (image == null) {
            image = imageDescriptor.createImage();
            imageMap.put(imageDescriptor, image);
        }
        return image;
    }

    public void dispose() {
        for (final Iterator it = imageMap.values().iterator(); it.hasNext();) {
            final Image image = (Image) it.next();
            image.dispose();
        }
        imageMap.clear();
    }
}
