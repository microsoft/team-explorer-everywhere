// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.compare.ITypedElement;

/**
 * {@link ILabeledCompareElement} is an interface that compare elements can
 * implement to enable extended functionality in the Microsoft compare
 * framework. {@link ILabeledCompareElement}s can provide a descriptive label.
 * This label is longer and more descriptive than the simple string name
 * provided by {@link ITypedElement#getName()}. The label is shown in the
 * content/merge viewer.
 */
public interface ILabeledCompareElement {
    /**
     * @return a descriptive label for this compare element, or
     *         <code>null</code> if no label is available
     */
    String getLabel();

    /**
     * @return a non-localized descriptive label
     */
    String getLabelNOLOC();
}
