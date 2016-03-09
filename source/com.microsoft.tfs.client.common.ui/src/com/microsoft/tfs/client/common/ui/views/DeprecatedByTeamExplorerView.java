// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.views;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

public class DeprecatedByTeamExplorerView extends ViewPart {
    private static final Log log = LogFactory.getLog(DeprecatedByTeamExplorerView.class);

    public DeprecatedByTeamExplorerView() {
    }

    @Override
    public final void createPartControl(final Composite parent) {
        /* Compute metrics in pixels */
        final GC gc = new GC(parent);
        final FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();

        final GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing =
            Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_SPACING) * 2;
        layout.verticalSpacing = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_SPACING);
        layout.marginWidth = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.HORIZONTAL_MARGIN);
        layout.marginHeight = Dialog.convertVerticalDLUsToPixels(fontMetrics, IDialogConstants.VERTICAL_MARGIN);
        parent.setLayout(layout);

        final Label imageLabel = new Label(parent, SWT.NONE);
        imageLabel.setImage(Display.getCurrent().getSystemImage(SWT.ICON_INFORMATION));

        final Link textLabel = new Link(parent, SWT.READ_ONLY | SWT.WRAP);
        textLabel.setText(Messages.getString("DeprecatedByTeamExplorerView.DeprecatedText")); //$NON-NLS-1$
        textLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    page.showView(TeamExplorerView.ID);
                } catch (final Exception f) {
                    log.warn("Could not open Team Explorer View", f); //$NON-NLS-1$

                    MessageDialog.openError(
                        getSite().getShell(),
                        Messages.getString("DeprecatedByTeamExplorerView.OpenViewFailedTitle"), //$NON-NLS-1$
                        Messages.getString("DeprecatedByTeamExplorerView.OpenViewFailedMessage")); //$NON-NLS-1$
                }
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(textLabel);
    }

    @Override
    public void setFocus() {
    }
}
