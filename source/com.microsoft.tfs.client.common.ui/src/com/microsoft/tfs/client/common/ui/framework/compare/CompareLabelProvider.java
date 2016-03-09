// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.swt.graphics.Image;

/**
 * A {@link CompareLabelProvider} can be used by a
 * {@link CustomCompareConfiguration}. By implementing this interface, clients
 * can have full control over the labels and images that are shown for elements
 * in compare.
 */
public interface CompareLabelProvider {
    /**
     * Get an {@link Image} for a compare element.
     *
     * @param compareInput
     *        the {@link ICompareInput} that represents the current node being
     *        displayed in the content/merge viewer - for example, a
     *        {@link DiffNode} (must not be <code>null</code>)
     * @param compareElement
     *        the compare element representing one side of the compare to
     *        provide an image for (must not be <code>null</code>)
     * @param type
     *        the {@link CompareElementType} that represents which side of the
     *        compare the <code>compareElement</code> is (must not be
     *        <code>null</code>)
     * @return an {@link Image} for the element, or <code>null</code>
     */
    public Image getImage(ICompareInput compareInput, ITypedElement compareElement, CompareElementType type);

    /**
     * Get a label for a compare element.
     *
     * @param compareInput
     *        the {@link ICompareInput} that represents the current node being
     *        displayed in the content/merge viewer - for example, a
     *        {@link DiffNode} (must not be <code>null</code>)
     * @param compareElement
     *        the compare element representing one side of the compare to
     *        provide an image for (must not be <code>null</code>)
     * @param type
     *        the {@link CompareElementType} that represents which side of the
     *        compare the <code>compareElement</code> is (must not be
     *        <code>null</code>)
     * @return an label for the element, or <code>null</code>
     */
    public String getLabel(ICompareInput compareInput, ITypedElement compareElement, CompareElementType type);
}
