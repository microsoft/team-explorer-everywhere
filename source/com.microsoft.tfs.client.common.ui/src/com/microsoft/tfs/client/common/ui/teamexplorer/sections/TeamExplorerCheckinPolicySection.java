// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import java.text.MessageFormat;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyFailureData;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.PendingChangesHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges.PolicyWarningsChangedListener;
import com.microsoft.tfs.util.Check;

public class TeamExplorerCheckinPolicySection extends TeamExplorerPendingChangesBaseSection {
    private TableViewer tableViewer;
    private final PolicyWarningsChangedListener listener = new WarningsChangedListener();
    private final Image warningImage =
        PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);

    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return context.isConnected() && getPolicyWarningsCount() > 0;
    }

    @Override
    public String getTitle() {
        final String format = Messages.getString("TeamExplorerCommon.TitleWithCountFormat"); //$NON-NLS-1$
        return MessageFormat.format(format, baseTitle, getPolicyWarningsCount());
    }

    public int getPolicyWarningsCount() {
        return getModel() == null ? 0 : getModel().getPolicyWarningsCount();
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // / Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 3, false, 0, 0);

        // Create the 'Reevaluate' link.
        final String evaluateLinkText = Messages.getString("TeamExplorerCheckinPolicySection.ReevaluateLinkText"); //$NON-NLS-1$
        final Hyperlink evaluateLink = toolkit.createHyperlink(composite, evaluateLinkText, SWT.NONE);
        evaluateLink.setUnderlined(false);
        evaluateLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                final Shell shell = composite.getShell();
                PendingChangesHelpers.evaluatePolicies(shell, getModel());
            }
        });
        GridDataBuilder.newInstance().applyTo(evaluateLink);

        final Label separator = toolkit.createLabel(composite, "|", SWT.VERTICAL); //$NON-NLS-1$
        GridDataBuilder.newInstance().vFill().applyTo(separator);

        // Create the 'Reevaluate' link.
        final String hideLinkText = Messages.getString("TeamExplorerCheckinPolicySection.HideLinkText"); //$NON-NLS-1$
        final Hyperlink hideLink = toolkit.createHyperlink(composite, hideLinkText, SWT.NONE);
        hideLink.setUnderlined(false);
        hideLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                TeamExplorerHelpers.showOrHideSection(composite, false);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(composite);
            }
        });
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(hideLink);

        // Create the table viewer, which is visible when the table is
        // non-empty.
        tableViewer = new TableViewer(composite, SWT.NO_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        tableViewer.setContentProvider(new PolicyWarningsContentProvider());
        tableViewer.setLabelProvider(new PolicyWarningsLabelProvider());
        tableViewer.setInput(getModel().getPolicyWarnings());
        GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).hSpan(3).applyTo(
            tableViewer.getControl());

        // Register the context menu with the table.
        registerContextMenu(context, tableViewer.getControl(), tableViewer);

        // Listen for changes to the Policy Warnings list.
        getModel().addPolicyWarningsChangedListener(listener);

        // Handle disposal of this control.
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                getModel().removePolicyWarningsChangedListener(listener);
            }
        });

        return composite;
    }

    private class PolicyWarningsContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return (Object[]) inputElement;
        }
    }

    private class PolicyWarningsLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            Check.isTrue(columnIndex == 0, "columnIndex == 0"); //$NON-NLS-1$
            Check.isTrue(element instanceof PolicyFailureData, "element instanceof PolicyFailureData"); //$NON-NLS-1$

            return warningImage;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            Check.isTrue(columnIndex == 0, "columnIndex == 0"); //$NON-NLS-1$
            Check.isTrue(element instanceof PolicyFailureData, "element instanceof PolicyFailureData"); //$NON-NLS-1$

            final PolicyFailureData data = (PolicyFailureData) element;
            return data.getMessage();
        }
    }

    private class WarningsChangedListener implements PolicyWarningsChangedListener {
        @Override
        public void onPolicyWarningsChanged() {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (tableViewer == null || tableViewer.getTable() == null || tableViewer.getTable().isDisposed()) {
                        return;
                    }

                    Check.notNull(tableViewer.getInput(), "tableViewer.getInput()"); //$NON-NLS-1$
                    Check.isTrue(
                        tableViewer.getInput() instanceof PolicyFailureData[],
                        "tableViewer.getInput() instanceof PolicyFailureData[]"); //$NON-NLS-1$

                    final PolicyFailureData[] warnings = getModel().getPolicyWarnings();
                    tableViewer.setInput(warnings);

                    final Composite composite = tableViewer.getTable().getParent();
                    TeamExplorerHelpers.showOrHideSection(composite, warnings.length > 0);
                    TeamExplorerHelpers.updateContainingSectionTitle(composite, getTitle());
                    TeamExplorerHelpers.relayoutContainingScrolledComposite(composite);
                }
            });
        }
    }
}
