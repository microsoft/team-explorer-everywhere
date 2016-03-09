// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.viewer;

import java.text.DecimalFormat;
import java.text.MessageFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.core.util.FileTypeDescription;

/**
 * <p>
 * {@link FolderFileLabelProvider} is an abstract {@link LabelProvider}
 * implementation used to build label providers that display folder and file
 * elements (possibly among others).
 * </p>
 *
 * <p>
 * Subclasses implement {@link #getText(Object)} and {@link #getImage(Object)}
 * as usual. When the subclass needs an image for a folder, it calls
 * {@link #getImageForFolder()}. When a subclass needs an image for a file, it
 * calls {@link #getImageForFile(String)}.
 * </p>
 *
 * <p>
 * {@link FolderFileLabelProvider} also makes an {@link ImageHelper} instance
 * available to subclasses ({@link #getImageHelper()}). This can be used to
 * managed {@link Image}s and relieve the subclass of the responsibility of
 * disposing the images. If the subclass overrides {@link #dispose()}, it must
 * be sure to call the super implementation.
 * </p>
 */
public abstract class FolderFileLabelProvider extends LabelProvider {
    /**
     * The {@link ImageHelper} used by this {@link FolderFileLabelProvider} -
     * never <code>null</code> and cleaned up in {@link #dispose()}.
     */
    private final ImageHelper imageHelper;

    private final DecimalFormat[] sizeFormat = new DecimalFormat[] {
        new DecimalFormat("#0.00"), //$NON-NLS-1$
        new DecimalFormat("#0.0"), //$NON-NLS-1$
        new DecimalFormat("#0"), //$NON-NLS-1$
    };

    /**
     * Creates a new {@link FolderFileLabelProvider}.
     */
    public FolderFileLabelProvider() {
        imageHelper = new ImageHelper();
    }

    /**
     * {@link FolderFileLabelProvider} overrides this {@link LabelProvider}
     * method to clean up the {@link ImageHelper} that it manages. Subclasses
     * must call super.dispose() if they also override this method.
     */
    @Override
    public void dispose() {
        imageHelper.dispose();
    }

    protected final String getSymbolicLinkDescription() {
        return Messages.getString("FolderFileLabelProvider.FileTypeSymbolicLink"); //$NON-NLS-1$
    }

    protected final String getFolderDescription() {
        return Messages.getString("FolderFileLabelProvider.FileTypeDirectory"); //$NON-NLS-1$
    }

    protected final String getFileTypeDescription(final String filename) {
        return FileTypeDescription.getDescription(filename);
    }

    protected final String getFileSize(final long size) {
        int idx = 0;
        float value = size;
        final DecimalFormat format;

        final String[] formats = new String[] {
            Messages.getString("FolderFileLabelProvider.FileSizeBytesFormat"), //$NON-NLS-1$
            Messages.getString("FolderFileLabelProvider.FileSizeKilobytesFormat"), //$NON-NLS-1$
            Messages.getString("FolderFileLabelProvider.FileSizeMegabytesFormat"), //$NON-NLS-1$
            Messages.getString("FolderFileLabelProvider.FileSizeGigabytesFormat"), //$NON-NLS-1$
            Messages.getString("FolderFileLabelProvider.FileSizeTerabytesFormat"), //$NON-NLS-1$
            Messages.getString("FolderFileLabelProvider.FileSizePetabytesFormat"), //$NON-NLS-1$
        };

        while (value > 1024 && idx < formats.length - 1) {
            value /= 1024;
            idx++;
        }

        if (idx > 0 && value < 10) {
            format = sizeFormat[0];
        } else if (idx > 0 && value < 100) {
            format = sizeFormat[1];
        } else {
            format = sizeFormat[2];
        }

        return MessageFormat.format(formats[idx], format.format(value));
    }

    /**
     * Called by a subclass to obtain an image for a folder element. Subclasses
     * should not dispose the returned image.
     *
     * @return an {@link Image} for a folder element
     */
    protected final Image getImageForFolder() {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
    }

    /**
     * Called by a subclass to obtain an image for a file element. The image is
     * determined by using the platform editor registry to find an associated
     * editor image for the specified filename. If the specified filename is
     * <code>null</code> or if the editor registry does not contain an editor
     * for that filename, a generic file image is returned. Subclasses should
     * not dispose the returned image.
     *
     * @param filename
     *        the filename to get an image for, or <code>null</code> to return a
     *        generic file image
     * @return an {@link Image} for a file element
     */
    protected final Image getImageForFile(final String filename) {
        return getImageForFile(filename, true);
    }

    /**
     * Called by a subclass to obtain an image for a file element. If
     * <code>useEditorRegistryImages</code> is <code>true</code>, the image is
     * determined by using the platform editor registry to find an associated
     * editor image for the specified filename. If the specified filename is
     * <code>null</code>, <code>useEditorRegistyImages</code> is
     * <code>false</code>, or if the editor registry does not contain an editor
     * for that filename, a generic file image is returned. Subclasses should
     * not dispose the returned image.
     *
     * @param filename
     *        the filename to get an image for, or <code>null</code> to return a
     *        generic file image
     * @param useEditorRegistryImages
     *        <code>true</code> to use the platform editor registry to attempt
     *        to find an image for the filename
     * @return an {@link Image} for a file element
     */
    protected final Image getImageForFile(final String filename, final boolean useEditorRegistryImages) {
        if (useEditorRegistryImages && filename != null) {
            final ImageDescriptor imageDescriptor =
                PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(filename);

            if (imageDescriptor != null) {
                return imageHelper.getImage(imageDescriptor);
            }
        }

        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
    }

    /**
     * @return the {@link ImageHelper} managed by this
     *         {@link FolderFileLabelProvider}
     */
    protected final ImageHelper getImageHelper() {
        return imageHelper;
    }
}
