// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs;

import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CSSNodeComboControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.commonstructure.CSSNode;
import com.microsoft.tfs.util.Check;

public class DeleteNodesDialog extends IconAndMessageDialog {

    private final CSSNode rootNode;
    private final CSSNode toDelete;
    private CSSNodeComboControl combo;
    private CSSNode reclassifyNode;

    public DeleteNodesDialog(final Shell parentShell, final CSSNode rootNode, final CSSNode nodeToDelete) {
        super(parentShell);
        Check.notNull(rootNode, "rootNode"); //$NON-NLS-1$
        Check.notNull(nodeToDelete, "nodeToDelete"); //$NON-NLS-1$

        this.rootNode = rootNode;
        toDelete = nodeToDelete;
        message = Messages.getString("DeleteNodesDialog.ConfirmDeleteDialogMessage"); //$NON-NLS-1$
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        createMessageArea(parent);
        // create a composite with standard margins and spacing
        final Composite composite = new Composite(parent, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(composite);
        SWTUtil.gridLayout(composite, 1, false);
        composite.setFont(parent.getFont());

        SWTUtil.createLabel(composite, Messages.getString("DeleteNodesDialog.SelectNewPathLabelText")); //$NON-NLS-1$

        combo = new CSSNodeComboControl(composite, SWT.BORDER, rootNode, (CSSNode) toDelete.getParent(), toDelete);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(combo);

        return composite;
    }

    @Override
    protected void okPressed() {
        reclassifyNode = combo.getSelectedNode();

        if (reclassifyNode == null) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("DeleteNodesDialog.ErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("DeleteNodesDialog.ErrorDialogText")); //$NON-NLS-1$
            combo.getTextControl().selectAll();
            return;
        }

        super.okPressed();
    }

    public CSSNode getReclassifyNode() {
        return reclassifyNode;
    }

    public CSSNode getRootNode() {
        return rootNode;
    }

    public CSSNode getToDelete() {
        return toDelete;
    }

    @Override
    protected Image getImage() {
        return getWarningImage();
    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.getString("DeleteNodesDialog.DeleteNodesDialogTitle")); //$NON-NLS-1$
    }

}
