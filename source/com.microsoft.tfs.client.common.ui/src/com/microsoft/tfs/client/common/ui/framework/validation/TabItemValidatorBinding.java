// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

import java.text.MessageFormat;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.AbstractValidatorBinding;
import com.microsoft.tfs.util.valid.IValidationMessage;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Severity;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorBinding;
import com.microsoft.tfs.util.valid.Validity;

/**
 * {@link TabItemValidatorBinding} is a {@link ValidatorBinding} implementation
 * that binds a {@link Validator} to a {@link TabItem}. On non-win32 platforms,
 * an {@link Image} is added and removed from the {@link TabItem} to represent
 * the {@link Validator}'s current {@link Validity}. The SWT/win32 port contains
 * a bug involving {@link TabItem}s and {@link Image}s, so on the win32
 * platform, the text of the {@link TabItem} is decorated instead.
 *
 * @see ValidatorBinding
 * @see Validator
 * @see TabItem
 */
public class TabItemValidatorBinding extends AbstractValidatorBinding {
    private static final String ORIGINAL_TEXT_KEY = "TabItemValidatorBinding-original-text"; //$NON-NLS-1$

    private final TabItem tabItem;

    /**
     * Creates a new {@link TabItemValidatorBinding} that binds the specified
     * {@link TabItem}'s decoration to a {@link Validator}'s validation state.
     *
     * @param tabItem
     */
    public TabItemValidatorBinding(final TabItem tabItem) {
        Check.notNull(tabItem, "tabItem"); //$NON-NLS-1$
        this.tabItem = tabItem;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.util.valid.AbstractValidatorBinding#update(com.
     * microsoft .tfs.util.valid .IValidity)
     */
    @Override
    protected void update(final IValidity validity) {
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.WIN32)) {
            updateWin32(validity);
        } else {
            updateOthers(validity);
        }
    }

    private void updateWin32(final IValidity validity) {
        if (validity == null || validity.isValid()) {
            final String originalText = (String) tabItem.getData(ORIGINAL_TEXT_KEY);
            if (originalText != null) {
                tabItem.setText(originalText);
                tabItem.setData(ORIGINAL_TEXT_KEY, null);
            }
        } else {
            String originalText = (String) tabItem.getData(ORIGINAL_TEXT_KEY);
            if (originalText == null) {
                originalText = tabItem.getText();
                tabItem.setData(ORIGINAL_TEXT_KEY, originalText);

                final String messageFormat = Messages.getString("TabItemValidatorBinding.TabTitleErrorFormat"); //$NON-NLS-1$
                tabItem.setText(MessageFormat.format(messageFormat, originalText));
            }
        }
    }

    private void updateOthers(final IValidity validity) {
        if (validity == null || validity.isValid()) {
            tabItem.setImage(null);
        } else {
            final IValidationMessage message = validity.getFirstMessage();
            Image image;
            if (message != null && Severity.WARNING == message.getSeverity()) {
                image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
            } else {
                image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
            }
            tabItem.setImage(image);
        }
    }
}
