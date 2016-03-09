// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.client.common.ui.framework.action.SelectionProviderAction;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;

public abstract class CSSNodeAction extends SelectionProviderAction {
    private final CommonStructureControl cssControl;

    public CSSNodeAction(
        final CommonStructureControl cssControl,
        final String text,
        final String toolTipText,
        final String iconPath) {
        super(cssControl);
        this.cssControl = cssControl;
        setText(text);
        if (toolTipText != null && toolTipText.length() > 0) {
            setToolTipText(toolTipText);
        }
        if (iconPath != null && iconPath.length() > 0) {
            setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(TFSCommonUIClientPlugin.PLUGIN_ID, iconPath));
        }
    }

    protected abstract boolean computeEnablement(CSSNode cssNode);

    @Override
    protected boolean computeEnablement(final IStructuredSelection selection) {
        return computeEnablement((CSSNode) selection.getFirstElement());
    }

    protected CommonStructureControl getCSSControl() {
        return cssControl;
    }

    protected CSSNode getSelectedNode() {
        return (CSSNode) getSelectionFirstElement();
    }

}
