// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.dialogs;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLink;
import com.microsoft.tfs.core.clients.workitem.internal.MacroTargetNotConfiguredException;
import com.microsoft.tfs.util.Check;

public class LabelableLinkSelectionDialog extends BaseDialog {
    private LabelableLinkTable linkTable;

    private final WorkItem workItem;
    private final LabelableLinkSelectionItem[] linkItems;
    private LabelableLinkSelectionItem selection;

    public LabelableLinkSelectionDialog(
        final Shell parentShell,
        final WorkItem workItem,
        final LabelableLinkSelectionItem[] linkItems) {
        super(parentShell);

        Check.notNull(workItem, "workItem"); //$NON-NLS-1$
        Check.notNull(linkItems, "linkItems"); //$NON-NLS-1$

        this.workItem = workItem;
        this.linkItems = linkItems;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("LabelableLinkSelectionDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label explanationLabel = new Label(dialogArea, SWT.WRAP);
        explanationLabel.setText(Messages.getString("LabelableLinkSelectionDialog.LinkSelectionNotAvail")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(explanationLabel);
        ControlSize.setSizeHints(
            explanationLabel,
            convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH),
            SWT.DEFAULT);

        linkTable = new LabelableLinkTable(dialogArea, SWT.BORDER | SWT.FULL_SELECTION);
        linkTable.setLinkItems(linkItems);
        GridDataBuilder.newInstance().grab().fill().applyTo(linkTable);
        ControlSize.setCharHeightHint(linkTable, 8);

        linkTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                selection = linkTable.getSelectedLinkItem();
                final Exception error = selection == null ? null : selection.getError();

                if (error != null) {
                    if (selection.getErrorShown() == false) {
                        if (error instanceof MacroTargetNotConfiguredException) {
                            final MacroTargetNotConfiguredException macroEx = (MacroTargetNotConfiguredException) error;
                            MessageDialog.openInformation(
                                getShell(),
                                macroEx.getMessageTitle(),
                                macroEx.getMessageBody());
                        } else {
                            final String messageFormat =
                                Messages.getString("LabelableLinkSelectionDialog.ErrorDialogTextFormat"); //$NON-NLS-1$
                            final String message =
                                MessageFormat.format(messageFormat, selection.getError().getLocalizedMessage());

                            ErrorDialog.openError(
                                getShell(),
                                Messages.getString("LabelableLinkSelectionDialog.ErrorDialogTitle"), //$NON-NLS-1$
                                message,
                                null);
                        }

                        selection.setErrorShown(true);
                    }

                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    return;
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(selection != null);
                }
            }
        });
    }

    @Override
    protected void hookAfterButtonsCreated() {
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    public LabelableLinkSelectionItem getSelectedLink() {
        return selection;
    }

    public static class LabelableLinkSelectionItem {
        private final String label;
        private final WIFormLink link;
        private Exception error = null;
        private boolean errorShown = false;

        public LabelableLinkSelectionItem(final String label, final WIFormLink link) {
            Check.notNull(label, "label"); //$NON-NLS-1$
            Check.notNull(link, "link"); //$NON-NLS-1$

            this.label = label;
            this.link = link;
        }

        public String getLabel() {
            return label;
        }

        public WIFormLink getLink() {
            return link;
        }

        private void setError(final Exception error) {
            this.error = error;
        }

        private Exception getError() {
            return error;
        }

        private void setErrorShown(final boolean errorShown) {
            this.errorShown = errorShown;
        }

        private boolean getErrorShown() {
            return errorShown;
        }
    }

    private class LabelableLinkTable extends TableControl {
        protected LabelableLinkTable(final Composite parent, final int style) {
            super(parent, style, LabelableLinkSelectionItem.class, null);

            setUseDefaultContentProvider();
            setUseDefaultLabelProvider();
            setupTable(true, true, new TableColumnData[] {
                new TableColumnData(
                    Messages.getString("LabelableLinkSelectionDialog.ColumnNameDescription"), //$NON-NLS-1$
                    0.30F,
                    "description"), //$NON-NLS-1$
                new TableColumnData(Messages.getString("LabelableLinkSelectionDialog.ColumnNameUrl"), 0.70F, "url") //$NON-NLS-1$ //$NON-NLS-2$
            });
        }

        public void setLinkItems(final LabelableLinkSelectionItem[] linkItems) {
            setElements(linkItems);
        }

        public LabelableLinkSelectionItem getSelectedLinkItem() {
            return (LabelableLinkSelectionItem) getSelectedElement();
        }

        @Override
        public String getColumnText(final Object element, final String propertyName) {
            final LabelableLinkSelectionItem item = (LabelableLinkSelectionItem) element;

            if (propertyName.equals("description")) //$NON-NLS-1$
            {
                return item.getLabel();
            } else if (propertyName.equals("url")) //$NON-NLS-1$
            {
                if (item.getError() != null) {
                    final String messageFormat = Messages.getString("LabelableLinkSelectionDialog.CellErrorFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, getErrorMessage(item.getError()));
                    return message;
                }

                try {
                    return item.getLink().getURL(workItem);
                } catch (final Exception e) {
                    item.setError(e);

                    final String messageFormat = Messages.getString("LabelableLinkSelectionDialog.CellErrorFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, getErrorMessage(e));
                    return message;
                }
            }

            return Messages.getString("LabelableLinkSelectionDialog.UnknownCellContent"); //$NON-NLS-1$
        }

        private String getErrorMessage(final Exception error) {
            if (error instanceof MacroTargetNotConfiguredException) {
                return ((MacroTargetNotConfiguredException) error).getMessageTitle();
            } else {
                return error.getLocalizedMessage();
            }
        }
    }
}
