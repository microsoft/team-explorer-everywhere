// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.controls.generic.html.EditorReadyListener;
import com.microsoft.tfs.client.common.ui.controls.generic.html.HTMLEditor;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;

/**
 * An HTML content editor control for work item fields, only available on
 * Eclipse 3.5 and later. Uses {@link HTMLEditor} for a full WYSIWYG editing
 * experience.
 *
 * @threadsafety unknown
 */
public class HTMLFieldControl extends BaseHTMLFieldControl {
    private HTMLEditor htmlEditor;

    /**
     * Tests whether this control is available for the currently running
     * Eclipse/SWT/browser version. Same requirements as {@link HTMLEditor}.
     *
     * @return <code>true</code> if this control can be used with this version
     *         of SWT and Browser, <code>false</code> if it cannot be loaded
     * @see HTMLEditor
     */
    public static boolean isAvailable() {
        return HTMLEditor.isAvailable();
    }

    public HTMLFieldControl() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        parent.setLayout(new GridLayout(1, true));

        final Field field = getField();

        final FieldChangeListener fieldChangeListener = new FieldChangeListener() {
            @Override
            public void fieldChanged(final FieldChangeEvent event) {
                /*
                 * Ignore the field changed event if it was triggered by this
                 * control
                 */
                if (event.source == htmlEditor) {
                    return;
                }

                final Display display = htmlEditor.getDisplay();
                final String html = getHTMLTextFromField();

                if (display.getThread() == Thread.currentThread()) {
                    htmlEditor.setHTML(html);
                    return;
                }

                UIHelpers.runOnUIThread(display, true, new Runnable() {
                    @Override
                    public void run() {
                        htmlEditor.setHTML(html);
                    }
                });
            }
        };
        field.addFieldChangeListener(fieldChangeListener);

        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                field.removeFieldChangeListener(fieldChangeListener);
            }
        });

        htmlEditor = new HTMLEditor(parent, new EditorReadyListener() {
            @Override
            public void editorReady() {
                htmlEditor.setHTML(getHTMLTextFromField());

                if (isFormReadonly()) {
                    htmlEditor.setReadOnly(true);
                }
            }
        }, SWT.NONE);

        htmlEditor.setLayoutData(new GridData(GridData.FILL_BOTH));

        final String workItemFieldRefName = getControlDescription().getFieldName() == null ? "" //$NON-NLS-1$
            : getWorkItem().getFields().getField(getControlDescription().getFieldName()).getReferenceName().toLowerCase(
                Locale.ENGLISH);

        AutomationIDHelper.setWidgetID(htmlEditor, workItemFieldRefName + "#htmlFieldControl"); //$NON-NLS-1$

        /*
         * Many methods on HTMLEditor cannot be called at this point (see
         * Javadoc); we must wait until the editor has finished loading via the
         * event.
         */

        if (!isFormReadonly()) {
            final FieldUpdateModifyListener fieldUpdateModifyListener = new FieldUpdateModifyListener(field);
            htmlEditor.addModifyListener(fieldUpdateModifyListener);
            htmlEditor.setData(FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY, fieldUpdateModifyListener);
        }
    }
}
