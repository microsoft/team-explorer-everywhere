// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.ICompareInput;

/**
 * {@link CompareElementType} represents a side of a compare: left (
 * {@link #LEFT}), right ({@link #RIGHT}), or ancestor ({@link #ANCESTOR}).
 */
public abstract class CompareElementType {
    /**
     * Represents the left side of a compare.
     */
    public static final CompareElementType LEFT = new CompareElementType("LEFT") { //$NON-NLS-1$
        @Override
        ITypedElement getCompareElementFromCompareInput(final ICompareInput input) {
            return input.getLeft();
        }

        @Override
        String getLabelPrefix(final CompareConfiguration compareConfiguration) {
            return (String) compareConfiguration.getProperty(CustomCompareConfiguration.LEFT_LABEL_PREFIX_PROPERTY);
        }

        @Override
        String getLabelSuffix(final CompareConfiguration compareConfiguration) {
            return (String) compareConfiguration.getProperty(CustomCompareConfiguration.LEFT_LABEL_SUFFIX_PROPERTY);
        }

        @Override
        void setElement(final Compare compare, final Object element) {
            compare.setModified(element);
        }
    };

    /**
     * Represents the right side of a compare.
     */
    public static final CompareElementType RIGHT = new CompareElementType("RIGHT") { //$NON-NLS-1$
        @Override
        ITypedElement getCompareElementFromCompareInput(final ICompareInput input) {
            return input.getRight();
        }

        @Override
        String getLabelPrefix(final CompareConfiguration compareConfiguration) {
            return (String) compareConfiguration.getProperty(CustomCompareConfiguration.RIGHT_LABEL_PREFIX_PROPERTY);
        }

        @Override
        String getLabelSuffix(final CompareConfiguration compareConfiguration) {
            return (String) compareConfiguration.getProperty(CustomCompareConfiguration.RIGHT_LABEL_SUFFIX_PROPERTY);
        }

        @Override
        void setElement(final Compare compare, final Object element) {
            compare.setOriginal(element);
        }
    };

    /**
     * Represents the ancestor side of a compare.
     */
    public static final CompareElementType ANCESTOR = new CompareElementType("ANCESTOR") { //$NON-NLS-1$
        @Override
        ITypedElement getCompareElementFromCompareInput(final ICompareInput input) {
            return input.getAncestor();
        }

        @Override
        String getLabelPrefix(final CompareConfiguration compareConfiguration) {
            return (String) compareConfiguration.getProperty(CustomCompareConfiguration.ANCESTOR_LABEL_PREFIX_PROPERTY);
        }

        @Override
        String getLabelSuffix(final CompareConfiguration compareConfiguration) {
            return (String) compareConfiguration.getProperty(CustomCompareConfiguration.ANCESTOR_LABEL_SUFFIX_PROPERTY);
        }

        @Override
        void setElement(final Compare compare, final Object element) {
            compare.setAncestor(element);
        }
    };

    abstract ITypedElement getCompareElementFromCompareInput(ICompareInput input);

    abstract String getLabelPrefix(CompareConfiguration compareConfiguration);

    abstract String getLabelSuffix(CompareConfiguration compareConfiguration);

    abstract void setElement(Compare compare, Object element);

    private final String s;

    private CompareElementType(final String s) {
        this.s = s;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return s;
    }
}
