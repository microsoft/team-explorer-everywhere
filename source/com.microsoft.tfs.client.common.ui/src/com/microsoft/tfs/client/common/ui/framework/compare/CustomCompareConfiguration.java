// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * <p>
 * {@link CustomCompareConfiguration} is a {@link CompareConfiguration} subclass
 * that contains logic to compute default labels and images for compare
 * elements. Unlike the base {@link CompareConfiguration}, this class computes a
 * label and image for each element, not just the root elements in a recursive
 * compare.
 * </p>
 *
 * <p>
 * Optionally, you can supply a {@link CompareLabelProvider} to a
 * {@link CustomCompareConfiguration} that will be delegated to. If a
 * {@link CompareLabelProvider} is not provided, the
 * {@link CompareUtils#getLabel(Object)} and
 * {@link CompareUtils#getImage(Object)} are used to compute a default label and
 * image for each element.
 * </p>
 *
 * <p>
 * Note that by calling methods such as {@link #setLeftLabel(String)}, you are
 * overriding the per-element labels and providing one label that will be used
 * for all elements. This is exactly how the base {@link CompareConfiguration}
 * works. If you set one label or image for all elements, the
 * {@link CompareLabelProvider} will not be used.
 * </p>
 */
public class CustomCompareConfiguration extends CompareConfiguration {
    /**
     * A property that can be set on this {@link CompareConfiguration} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property provides a {@link String} suffix that will be appended to each
     * left label computed by this {@link CompareConfiguration}. This suffix is
     * only used if there is no default left label (see
     * {@link #setLeftLabel(String)}) and no {@link CompareLabelProvider} is
     * provided.
     */
    public static final String LEFT_LABEL_SUFFIX_PROPERTY =
        "com.microsoft.tfs.client.common.ui.compare.LeftLabelSuffix"; //$NON-NLS-1$

    /**
     * A property that can be set on this {@link CompareConfiguration} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property provides a {@link String} prefix that will be prepended to each
     * left label computed by this {@link CompareConfiguration}. This prefix is
     * only used if there is no default left label (see
     * {@link #setLeftLabel(String)}) and no {@link CompareLabelProvider} is
     * provided.
     */
    public static final String LEFT_LABEL_PREFIX_PROPERTY =
        "com.microsoft.tfs.client.common.ui.compare.LeftLabelPrefix"; //$NON-NLS-1$

    /**
     * A property that can be set on this {@link CompareConfiguration} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property provides a {@link String} suffix that will be appended to each
     * right label computed by this {@link CompareConfiguration}. This suffix is
     * only used if there is no default right label (see
     * {@link #setRightLabel(String)}) and no {@link CompareLabelProvider} is
     * provided.
     */
    public static final String RIGHT_LABEL_SUFFIX_PROPERTY =
        "com.microsoft.tfs.client.common.ui.compare.RightLabelSuffix"; //$NON-NLS-1$

    /**
     * A property that can be set on this {@link CompareConfiguration} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property provides a {@link String} prefix that will be prepended to each
     * right label computed by this {@link CompareConfiguration}. This prefix is
     * only used if there is no default right label (see
     * {@link #setRightLabel(String)}) and no {@link CompareLabelProvider} is
     * provided.
     */
    public static final String RIGHT_LABEL_PREFIX_PROPERTY =
        "com.microsoft.tfs.client.common.ui.compare.RightLabelPrefix"; //$NON-NLS-1$

    /**
     * A property that can be set on this {@link CompareConfiguration} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property provides a {@link String} suffix that will be appended to each
     * ancestor label computed by this {@link CompareConfiguration}. This suffix
     * is only used if there is no default ancestor label (see
     * {@link #setAncestorLabel(String)}) and no {@link CompareLabelProvider} is
     * provided.
     */
    public static final String ANCESTOR_LABEL_SUFFIX_PROPERTY =
        "com.microsoft.tfs.client.common.ui.compare.AncestorLabelSuffix"; //$NON-NLS-1$

    /**
     * A property that can be set on this {@link CompareConfiguration} (see
     * {@link CompareConfiguration#setProperty(String, Object)}). If set, this
     * property provides a {@link String} prefix that will be prepended to each
     * ancestor label computed by this {@link CompareConfiguration}. This prefix
     * is only used if there is no default ancestor label (see
     * {@link #setAncestorLabel(String)}) and no {@link CompareLabelProvider} is
     * provided.
     */
    public static final String ANCESTOR_LABEL_PREFIX_PROPERTY =
        "com.microsoft.tfs.client.common.ui.compare.AncestorLabelPrefix"; //$NON-NLS-1$

    private CompareLabelProvider labelProvider;
    private Image emptyImage;

    /**
     * Creates a new {@link CustomCompareConfiguration} with no
     * {@link CompareLabelProvider}. If labels and images are not set globally
     * by methods like {@link #setLeftLabel(String)} they will be computed by
     * delegating to {@link CompareUtils#getLabel(Object)} and
     * {@link CompareUtils#getImage(Object)}.
     */
    public CustomCompareConfiguration() {
        this(null);
    }

    /**
     * Creates a new {@link CustomCompareConfiguration} with the specified
     * {@link CompareLabelProvider}.
     *
     * @param labelProvider
     *        the {@link CompareLabelProvider} to use, or <code>null</code> to
     *        use default labels and images
     */
    public CustomCompareConfiguration(final CompareLabelProvider labelProvider) {
        this.labelProvider = labelProvider;
    }

    /**
     * Sets the label provider of this {@link CustomCompareConfiguration}.
     *
     * @param labelProvider
     *        the {@link CompareLabelProvider} to use or <code>null</code> to
     *        not use a {@link CompareLabelProvider}
     */
    public void setLabelProvider(final CompareLabelProvider labelProvider) {
        this.labelProvider = labelProvider;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.compare.CompareConfiguration#getLeftImage(java.lang.Object)
     */
    @Override
    public Image getLeftImage(final Object element) {
        return getImage(element, super.getLeftImage(element), CompareElementType.LEFT);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.compare.CompareConfiguration#getLeftLabel(java.lang.Object)
     */
    @Override
    public String getLeftLabel(final Object element) {
        return getLabel(element, super.getLeftLabel(element), CompareElementType.LEFT);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.compare.CompareConfiguration#getRightImage(java.lang.Object)
     */
    @Override
    public Image getRightImage(final Object element) {
        return getImage(element, super.getRightImage(element), CompareElementType.RIGHT);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.compare.CompareConfiguration#getRightLabel(java.lang.Object)
     */
    @Override
    public String getRightLabel(final Object element) {
        return getLabel(element, super.getRightLabel(element), CompareElementType.RIGHT);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.CompareConfiguration#getAncestorImage(java.lang.
     * Object )
     */
    @Override
    public Image getAncestorImage(final Object element) {
        return getImage(element, super.getAncestorImage(element), CompareElementType.ANCESTOR);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.CompareConfiguration#getAncestorLabel(java.lang.
     * Object )
     */
    @Override
    public String getAncestorLabel(final Object element) {
        return getLabel(element, super.getAncestorLabel(element), CompareElementType.ANCESTOR);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.compare.CompareConfiguration#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if (emptyImage != null) {
            emptyImage.dispose();
            emptyImage = null;
        }
    }

    private Image getImage(final Object element, final Image defaultImage, final CompareElementType type) {
        if (defaultImage != null) {
            return defaultImage;
        }

        if (element == null) {
            return getEmptyImage();
        }

        final ICompareInput compareInput = (ICompareInput) element;
        final ITypedElement compareElement = type.getCompareElementFromCompareInput(compareInput);

        if (compareElement == null) {
            return getEmptyImage();
        }

        if (labelProvider != null) {
            final Image image = labelProvider.getImage(compareInput, compareElement, type);
            if (image != null) {
                return image;
            }
        }

        final Image image = CompareUtils.getImage(compareElement);

        if (image != null) {
            return image;
        }

        return getEmptyImage();
    }

    private Image getEmptyImage() {
        if (emptyImage != null) {
            return emptyImage;
        }

        final ImageData imageData = new ImageData(1, 1, 1, new PaletteData(new RGB[] {
            new RGB(0, 0, 0)
        }));
        imageData.transparentPixel = imageData.palette.getPixel(new RGB(0, 0, 0));
        emptyImage = new Image(Display.getDefault(), imageData);
        return emptyImage;
    }

    private String getLabel(final Object element, final String defaultLabel, final CompareElementType type) {
        if (defaultLabel != null) {
            return defaultLabel;
        }

        if (element == null) {
            return ""; //$NON-NLS-1$
        }

        final ICompareInput compareInput = (ICompareInput) element;
        final ITypedElement compareElement = type.getCompareElementFromCompareInput(compareInput);

        if (compareElement == null) {
            return ""; //$NON-NLS-1$
        }

        if (labelProvider != null) {
            final String label = labelProvider.getLabel(compareInput, compareElement, type);
            if (label != null) {
                return label;
            }
        }

        String label = CompareUtils.getLabel(compareElement);
        if (label != null) {
            final String prefix = type.getLabelPrefix(this);
            final String suffix = type.getLabelSuffix(this);
            if (prefix != null) {
                label = prefix + label;
            }
            if (suffix != null) {
                label = label + suffix;
            }
            return label;
        }

        return ""; //$NON-NLS-1$
    }
}
