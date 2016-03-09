// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.image;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link ImageHelper} is a helper class that can be used by clients that manage
 * <code>SWT</code> {@link Image} resources and JFace {@link ImageDescriptor}
 * objects. {@link ImageHelper} creates {@link Image}s and
 * {@link ImageDescriptor}s on demand, caching them for subsequent use and
 * eventual disposal of the {@link Image}s. Client code does not need to worry
 * about the storage of each individual {@link Image} resource or
 * {@link ImageDescriptor} object and does not need special handling to ensure
 * that each {@link Image} or {@link ImageDescriptor} is created only once, no
 * matter how often it is used.
 * </p>
 *
 * <p>
 * Clients that use {@link ImageHelper} typically instantiate an instance of
 * {@link ImageHelper} early in the life of the client class (the constructor is
 * a good place). During the lifetime of the client, it calls the various
 * methods on {@link ImageHelper} that return {@link Image}s or
 * {@link ImageDescriptor}s. At the end of the lifetime of the client, the
 * client <b>must</b> call the {@link #dispose()} method of the
 * {@link ImageHelper} to ensure that any created {@link Image}s are properly
 * disposed.
 * </p>
 *
 * <p>
 * It is safe to call {@link #dispose()} multiple times. It is also safe to
 * re-use an {@link ImageHelper} after {@link #dispose()} has been called, as
 * long as there is an eventual ending {@link #dispose()} call.
 * </p>
 *
 * <p>
 * To obtain an {@link ImageDescriptor}, specify a plugin ID and an image path
 * relative to that plugin to get an {@link ImageDescriptor} for. To obtain an
 * {@link Image}, the same thing can be specified. Alternatively, {@link Image}s
 * can be requested by specifying an {@link ImageDescriptor} directly.
 * Optionally, a default plugin ID can be supplied when constructing an
 * {@link ImageHelper}, and only the image path need be specified when
 * requesting {@link Image}s or {@link ImageDescriptor}s.
 * </p>
 *
 * <p>
 * <b>Important</b>: Just like
 * {@link AbstractUIPlugin#imageDescriptorFromPlugin(String, String)}, the image
 * paths passed to {@link ImageHelper} must not include a leading "." or path
 * separator. The path "<code>icons/myimage.gif</code>" is valid, but "
 * <code>./icons/myimage.gif</code>" and "<code>/icons/myimage.gif</code>" are
 * not valid.
 * </p>
 */
public class ImageHelper {
    private static final Log log = LogFactory.getLog(ImageHelper.class);

    /**
     * An (optional) default plugin ID that can be set at construction time.
     */
    private final String defaultPluginId;

    /**
     * Caches {@link ImageDescriptor}s that are created by this object. Maps
     * from {@link ImageDescriptorKey} to {@link ImageDescriptor}. Must be
     * synchronized on when accessing.
     */
    private final Map createdImageDescriptors = new HashMap();

    /**
     * Caches {@link Image}s that are created by this object. Maps from
     * {@link ImageDescriptor} to {@link Image}. Must be synchronized on when
     * accessing.
     */
    private final Map createdImages = new HashMap();

    /**
     * Creates a new {@link ImageHelper} with no default plugin ID. When
     * requesting {@link Image}s and {@link ImageDescriptor}s, the
     * {@link #getImage(String)} and {@link #getImageDescriptor(String)}
     * convenience methods may not be used since they require a default plugin
     * ID to be set.
     */
    public ImageHelper() {
        this(null);
    }

    /**
     * Creates a new {@link ImageHelper} with the specified default plugin ID.
     * If the argument is not <code>null</code>, then {@link Image}s and
     * {@link ImageDescriptor}s can be requested without having to specify a
     * plugin ID with each request.
     *
     * @param defaultPluginId
     *        the default plugin ID to use when calling the
     *        {@link #getImage(String)} and {@link #getImageDescriptor(String)}
     *        convenience methods
     */
    public ImageHelper(final String defaultPluginId) {
        this.defaultPluginId = defaultPluginId;
    }

    /**
     * <p>
     * Obtains an {@link ImageDescriptor} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link ImageDescriptor}
     * corresponding to the specified image file path, a new
     * {@link ImageDescriptor} will be created. Otherwise, a cached
     * {@link ImageDescriptor} will be returned. If no image is found at the
     * specified image file path, a warning will be logged and a standard
     * "missing image" {@link ImageDescriptor} will be returned.
     * </p>
     *
     * <p>
     * This is a convenience method that does not specify a plugin ID to resolve
     * the image file path relative to. It should only be called if this
     * {@link ImageHelper} was created with a non-<code>null</code> default
     * plugin ID.
     * </p>
     *
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link ImageDescriptor} corresponding to the specified image
     *         file path (never <code>null</code>)
     */
    public ImageDescriptor getImageDescriptor(final String imageFilePath) {
        if (defaultPluginId == null) {
            throw new IllegalStateException(
                "to use this method, you must construct an ImageHelper with a non-null default plugin ID"); //$NON-NLS-1$
        }

        return getImageDescriptor(defaultPluginId, imageFilePath);
    }

    /**
     * <p>
     * Obtains an {@link ImageDescriptor} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link ImageDescriptor}
     * corresponding to the specified plugin ID and image file path, a new
     * {@link ImageDescriptor} will be created. Otherwise, a cached
     * {@link ImageDescriptor} will be returned. If no image is found at the
     * specified image file path, a warning will be logged and a standard
     * "missing image" {@link ImageDescriptor} will be returned.
     * </p>
     *
     * @param pluginId
     *        specifies the plugin to resolve the relative image path with (must
     *        not be <code>null</code>)
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link ImageDescriptor} corresponding to the specified image
     *         file path (never <code>null</code>)
     */
    public ImageDescriptor getImageDescriptor(final String pluginId, final String imageFilePath) {
        Check.notNull(pluginId, "pluginId"); //$NON-NLS-1$
        Check.notNull(imageFilePath, "imageFilePath"); //$NON-NLS-1$

        final ImageDescriptorKey key = new ImageDescriptorKey(pluginId, imageFilePath);
        synchronized (createdImageDescriptors) {
            ImageDescriptor descriptor = (ImageDescriptor) createdImageDescriptors.get(key);
            if (descriptor != null) {
                return descriptor;
            }

            descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, imageFilePath);

            if (descriptor == null) {
                final String messageFormat = "image missing: pluginId=[{0}] path=[{1}]"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, pluginId, imageFilePath);
                log.warn(message);
                descriptor = ImageDescriptor.getMissingImageDescriptor();
            } else {
                if (log.isTraceEnabled()) {
                    final String messageFormat = "created an image descriptor (pluginId=[{0}] path=[{1}]):{2}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, pluginId, imageFilePath, descriptor);
                    log.trace(message);
                }
            }

            createdImageDescriptors.put(key, descriptor);
            return descriptor;
        }
    }

    /**
     * <p>
     * Obtains an {@link Image} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link Image}
     * corresponding to the specified image file path, a new {@link Image} will
     * be created. Otherwise, a cached {@link Image} will be returned. If no
     * {@link Image} can be found at the specified image file path, a warning
     * will be logged and a standard "missing image" {@link Image} will be
     * returned.
     * </p>
     *
     * <p>
     * The caller of this method should <b>not</b> manually call
     * <code>dispose()</code> on the {@link Image} returned by this method.
     * Instead, when {@link #dispose()} is called on this {@link ImageHelper},
     * all {@link Image}s that it has created will be disposed.
     * </p>
     *
     * <p>
     * This is a convenience method that does not specify a plugin ID to resolve
     * the image file path relative to. It should only be called if this
     * {@link ImageHelper} was created with a non-<code>null</code> default
     * plugin ID.
     * </p>
     *
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link Image} corresponding to the specified image file path
     *         (never <code>null</code>)
     */
    public Image getImage(final String imageFilePath) {
        if (defaultPluginId == null) {
            throw new IllegalStateException(
                "to use this method, you must construct an ImageHelper with a non-null default plugin ID"); //$NON-NLS-1$
        }

        return getImage(defaultPluginId, imageFilePath);
    }

    /**
     * <p>
     * Obtains an {@link Image} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link Image}
     * corresponding to the specified plugin ID and image file path, a new
     * {@link Image} will be created. Otherwise, a cached {@link Image} will be
     * returned. If no {@link Image} can be found at the specified image file
     * path, a warning will be logged and a standard "missing image"
     * {@link Image} will be returned.
     * </p>
     *
     * <p>
     * The caller of this method should <b>not</b> manually call
     * <code>dispose()</code> on the {@link Image} returned by this method.
     * Instead, when {@link #dispose()} is called on this {@link ImageHelper},
     * all {@link Image}s that it has created will be disposed.
     * </p>
     *
     * @param pluginId
     *        specifies the plugin to resolve the relative image path with (must
     *        not be <code>null</code>)
     * @param imageFilePath
     *        the image file path (must not be <code>null</code>, also see
     *        comments on this class about specifying image file paths)
     * @return an {@link Image} corresponding to the specified image file path
     *         (never <code>null</code>)
     */
    public Image getImage(final String pluginId, final String imageFilePath) {
        return getImage(getImageDescriptor(pluginId, imageFilePath));
    }

    /**
     * Obtains an {@link Image} from this {@link ImageHelper}. If this
     * {@link ImageHelper} has not already created an {@link Image}
     * corresponding to the specified {@link ImageDescriptor}, a new
     * {@link Image} will be created. Otherwise, a cached {@link Image} will be
     * returned.
     *
     * <p>
     * The caller of this method should <b>not</b> manually call
     * <code>dispose()</code> on the {@link Image} returned by this method.
     * Instead, when {@link #dispose()} is called on this {@link ImageHelper},
     * all {@link Image}s that it has created will be disposed.
     * </p>
     *
     * @param imageDescriptor
     *        an {@link ImageDescriptor} that specifies the {@link Image} to
     *        obtain (must not be <code>null</code>)
     * @return an {@link Image} corresponding to the specified
     *         {@link ImageDescriptor} (never <code>null</code>)
     */
    public Image getImage(final ImageDescriptor imageDescriptor) {
        Check.notNull(imageDescriptor, "imageDescriptor"); //$NON-NLS-1$

        synchronized (createdImages) {
            Image image = (Image) createdImages.get(imageDescriptor);

            if (image != null) {
                return image;
            }

            image = imageDescriptor.createImage();
            createdImages.put(imageDescriptor, image);
            return image;
        }
    }

    /**
     * Must be called to dispose of any {@link Image}s created by this
     * {@link ImageHelper}. This method is safe to call multiple times.
     */
    public void dispose() {
        synchronized (createdImages) {
            final Image[] images = (Image[]) createdImages.values().toArray(new Image[createdImages.size()]);
            for (int i = 0; i < images.length; i++) {
                images[i].dispose();
            }
            createdImages.clear();
        }
    }

    /**
     * A private class used as a key for cached {@link ImageDescriptor}s.
     */
    private static class ImageDescriptorKey {
        private final String pluginId;
        private final String imageFilePath;

        public ImageDescriptorKey(final String pluginId, final String imageFilePath) {
            this.pluginId = pluginId;
            this.imageFilePath = imageFilePath;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + pluginId.hashCode();
            result = prime * result + imageFilePath.hashCode();
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ImageDescriptorKey)) {
                return false;
            }

            final ImageDescriptorKey other = (ImageDescriptorKey) obj;
            return pluginId.equals(other.pluginId) && imageFilePath.equals(other.imageFilePath);
        }
    }
}
