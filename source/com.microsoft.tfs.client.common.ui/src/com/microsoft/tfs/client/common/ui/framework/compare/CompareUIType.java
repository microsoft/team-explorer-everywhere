// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;

import com.microsoft.tfs.client.common.ui.framework.compare.internal.CompatibleCompareUI;

/**
 * {@link CompareUIType} represents the two types of internal Eclipse compare
 * UI: editors (represented by {@link #EDITOR}) and dialogs (represented by
 * {@link #DIALOG}).
 */
public abstract class CompareUIType {
    /**
     * A {@link CompareUIType} that represents compare editors.
     */
    public static final CompareUIType EDITOR = new CompareUIType("EDITOR") { //$NON-NLS-1$
        @Override
        public void openCompareUI(final CompareEditorInput input) {
            CompareUI.openCompareEditor(input);
        }
    };

    /**
     * A {@link CompareUIType} that represents compare dialogs.
     */
    public static final CompareUIType DIALOG = new CompareUIType("DIALOG") { //$NON-NLS-1$

        @Override
        public void openCompareUI(final CompareEditorInput input) {
            boolean legacy = true;

            try {
                CompareEditorInput.class.getDeclaredMethod("okPressed", (Class[]) null); //$NON-NLS-1$
                legacy = false;
            } catch (final Exception e) {
            }

            if (legacy) {
                CompatibleCompareUI.openCompareDialog(input);
            } else {
                /*
                 * The Eclipse compare dialog can be used with 3.3 and higher
                 * since it was fixed.
                 */
                CompareUI.openCompareDialog(input);
            }
        }

    };

    /**
     * Opens an internal Eclipse compare UI using the specified
     * {@link CompareEditorInput}.
     *
     * @param input
     *        the {@link CompareEditorInput} to open (must not be
     *        <code>null</code>)
     */
    public abstract void openCompareUI(CompareEditorInput input);

    private final String s;

    private CompareUIType(final String s) {
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
