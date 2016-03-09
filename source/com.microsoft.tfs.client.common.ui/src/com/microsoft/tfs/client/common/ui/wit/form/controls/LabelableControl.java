// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.wit.dialogs.LabelableLinkSelectionDialog;
import com.microsoft.tfs.client.common.ui.wit.dialogs.LabelableLinkSelectionDialog.LabelableLinkSelectionItem;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.form.WIFormControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLabelPositionEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLink;
import com.microsoft.tfs.core.clients.workitem.form.WIFormReadOnlyEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormText;
import com.microsoft.tfs.core.clients.workitem.internal.MacroTargetNotConfiguredException;

public abstract class LabelableControl extends BaseWorkItemControl {
    // A map of link-text to WIFormLink elements. This control allows multiple
    // link segments and
    // regular text segments to be intermixed. This map associates a single
    // link-text with the
    // corresponding WIFormLink element.
    //
    // Uses a list so that we can order the links for back compat.
    private final List textAndLinkList = new ArrayList();

    // columnsToTake will be >= getControlColumns()
    protected abstract void createControl(Composite parent, int columnsToTake);

    // how many columns will the createControl() method take up?
    protected abstract int getControlColumns();

    protected WIFormControl getControlDescription() {
        return (WIFormControl) getFormElement();
    }

    protected boolean isLabelOnly() {
        return false;
    }

    protected boolean isFormReadonly() {
        return getControlDescription().getReadOnly() == WIFormReadOnlyEnum.TRUE;
    }

    @Override
    public int getMinimumRequiredColumnCount() {
        if (getControlDescription().getLabelPosition() == WIFormLabelPositionEnum.TOP
            || getControlDescription().getLabelPosition() == WIFormLabelPositionEnum.BOTTOM) {
            return getControlColumns();
        }
        return getControlColumns() + 1;
    }

    @Override
    public void addToComposite(final Composite parent) {
        final int numColumns = ((GridLayout) parent.getLayout()).numColumns;
        final LinkTextData textData = getLabelTextAndLinks(getControlDescription(), textAndLinkList);

        if (textData != null) {
            CompatibilityLinkControl label = null;

            if (isLabelOnly()) {
                label = CompatibilityLinkFactory.createLink(parent, SWT.NONE);
                label.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, numColumns, 1));
            } else if (getControlDescription().getLabelPosition() == WIFormLabelPositionEnum.LEFT
                || getControlDescription().getLabelPosition() == null) {
                label = CompatibilityLinkFactory.createLink(parent, SWT.NONE);
                createControl(parent, numColumns - 1);
            } else if (getControlDescription().getLabelPosition() == WIFormLabelPositionEnum.RIGHT) {
                createControl(parent, numColumns - 1);
                label = CompatibilityLinkFactory.createLink(parent, SWT.NONE);
            } else if (getControlDescription().getLabelPosition() == WIFormLabelPositionEnum.TOP) {
                label = CompatibilityLinkFactory.createLink(parent, SWT.NONE);
                label.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, numColumns, 1));
                createControl(parent, numColumns);
            } else
            /* WIFormLabelPositionEnum.BOTTOM */
            {
                createControl(parent, numColumns);
                label = CompatibilityLinkFactory.createLink(parent, SWT.NONE);
                label.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, numColumns, 1));
            }

            // Set the label and/or link-text on the control.
            if (label.isHyperlinkTextSupported() && textData.getHyperlinkText() != null) {
                label.setText(textData.getHyperlinkText());
            } else if (textData.getText() != null) {
                label.setText(textData.getText());
            }

            // Setup the selection click handler.
            label.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    /*
                     * Try to get the link (in the text field in Eclipse >= 3.1)
                     */
                    try {
                        final java.lang.reflect.Field textField = e.getClass().getField("text"); //$NON-NLS-1$
                        final String text = (String) textField.get(e);

                        if (text != null) {
                            invokeLink(((Control) e.widget).getShell(), text);
                            return;
                        }
                    } catch (final Exception failure) {
                        /* Suppress, uses backcompat */
                    }

                    selectLink(((Control) e.widget).getShell());
                }
            });

            getWorkItemEditorContextMenu().setMenuOnControl(label.getControl());

            setupToolTip(label.getControl());
        } else {
            createControl(parent, numColumns);
        }
    }

    // it's OK for subclasses to return null if there is no field for tool tip
    // support
    protected Field getFieldForToolTipSupport() {
        return null;
    }

    private void setupToolTip(final Control label) {
        /*
         * the field that provides the help text for the tool tip
         */
        final Field field = getFieldForToolTipSupport();

        /*
         * subclasses can indicate that no such tool tip functionality should
         * exist
         */
        if (field == null) {
            return;
        }

        /*
         * create a work item field change listener to set the tool tip
         */
        final ToolTipFieldChangeListener toolTipFieldChangeListener = new ToolTipFieldChangeListener(label);

        /*
         * add the listener to the field
         */
        field.addFieldChangeListener(toolTipFieldChangeListener);

        /*
         * add a dispose listener so that the field change listener is removed
         * from the work item when this UI is disposed. the work item has a
         * lifecycle independent of the UI.
         */
        label.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                field.removeFieldChangeListener(toolTipFieldChangeListener);
            }
        });

        /*
         * fire a "fake" field change event to the field change listener this
         * sets up the initial tool tip
         */
        final FieldChangeEvent fieldChangeEvent = new FieldChangeEvent();
        fieldChangeEvent.field = field;
        toolTipFieldChangeListener.fieldChanged(fieldChangeEvent);
    }

    private LinkTextData getLabelTextAndLinks(final WIFormControl control, final List textAndLinkList) {
        // The label attribute is used if there no link or label child.
        if (control.getLink() == null && control.getLabelText() == null) {
            return new LinkTextData(control.getLabel(), control.getLabel());
        }

        // Get the link and label from the child elements.
        final StringBuffer hyperlinkBuffer = new StringBuffer();
        final StringBuffer textBuffer = new StringBuffer();

        if (control.getLink() != null) {
            // The text for this link comes from the control attribute.
            // The URL comes from the link child.
            final String label = control.getLabel();
            if (label != null) {
                hyperlinkBuffer.append("<a>"); //$NON-NLS-1$
                hyperlinkBuffer.append(label);
                hyperlinkBuffer.append("</a>"); //$NON-NLS-1$

                textBuffer.append(label);

                textAndLinkList.add(new LabelableLinkSelectionItem(label, control.getLink()));
            }
        } else if (control.getLabelText() != null) {
            final WIFormText[] textElements = control.getLabelText().getTextElements();
            for (int i = 0; i < textElements.length; i++) {
                final WIFormText textChild = textElements[i];
                final String text = textChild.getInnerText();
                final WIFormLink link = textChild.getLink();

                if (link != null) {
                    hyperlinkBuffer.append("<a>"); //$NON-NLS-1$
                    hyperlinkBuffer.append(text);
                    hyperlinkBuffer.append("</a>"); //$NON-NLS-1$

                    textBuffer.append(text);

                    textAndLinkList.add(new LabelableLinkSelectionItem(text, link));
                } else {
                    hyperlinkBuffer.append(text);
                    textBuffer.append(text);
                }
            }
        }

        String hyperlink = hyperlinkBuffer.toString();
        String text = textBuffer.toString();

        if (text != null) {
            text = text.trim();

            if (text.length() == 0) {
                text = null;
            }
        }

        if (hyperlink != null) {
            hyperlink = hyperlink.trim();

            if (hyperlink.length() == 0) {
                hyperlink = null;
            }
        }

        if (text == null || hyperlink == null) {
            return null;
        }

        return new LinkTextData(hyperlink, text);
    }

    private void invokeLink(final Shell shell, final String clickedText) {
        LabelableLinkSelectionItem linkItem = null;

        for (final Iterator i = textAndLinkList.iterator(); i.hasNext();) {
            final LabelableLinkSelectionItem item = (LabelableLinkSelectionItem) i.next();

            if (item.getLabel().equals(clickedText)) {
                linkItem = item;
            }
        }

        if (linkItem != null && linkItem.getLink() != null) {
            invokeLink(shell, linkItem.getLink());
        }
    }

    private void selectLink(final Shell shell) {
        LabelableLinkSelectionItem linkItem = null;

        if (textAndLinkList.size() == 1) {
            linkItem = (LabelableLinkSelectionItem) textAndLinkList.get(0);
        } else if (textAndLinkList.size() > 1) {
            final LabelableLinkSelectionDialog linkDialog = new LabelableLinkSelectionDialog(
                shell,
                getFormContext().getWorkItem(),
                (LabelableLinkSelectionItem[]) textAndLinkList.toArray(
                    new LabelableLinkSelectionItem[textAndLinkList.size()]));

            if (linkDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            linkItem = linkDialog.getSelectedLink();
        }

        if (linkItem != null) {
            invokeLink(shell, linkItem.getLink());
        }
    }

    private void invokeLink(final Shell shell, final WIFormLink link) {
        String url;
        try {
            url = link.getURL(getFormContext().getWorkItem());
            Launcher.launch(url);
        } catch (final MacroTargetNotConfiguredException macroEx) {
            MessageDialog.openInformation(shell, macroEx.getMessageTitle(), macroEx.getMessageBody());
        } catch (final Exception ex) {
            final Log log = LogFactory.getLog(LabelableControl.class);
            log.error("Could not launch the browser for link (url)", ex); //$NON-NLS-1$

            final String messageFormat = Messages.getString("LabelableControl.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, ex.getMessage());
            MessageDialog.openError(shell, Messages.getString("LabelableControl.ErrorDialogTitle"), message); //$NON-NLS-1$
        }
    }

    private static class LinkTextData {
        private final String hyperlinkText;
        private final String text;

        public LinkTextData(final String hyperlinkText, final String text) {
            this.hyperlinkText = hyperlinkText;
            this.text = text;
        }

        public String getHyperlinkText() {
            return hyperlinkText;
        }

        public String getText() {
            return text;
        }
    }
}
