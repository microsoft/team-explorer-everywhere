// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.text.MessageFormat;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.AutocompleteCombo;
import com.microsoft.tfs.client.common.ui.controls.generic.html.HTMLEditor;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.util.htmlfilter.HTMLFilter;

public class FieldUpdateModifyListener implements ModifyListener {
    public static final String MODIFICATION_KEY = "modification-key"; //$NON-NLS-1$
    public static final String MODIFY_LISTENER_WIDGET_DATA_KEY = "wit-modify-listener"; //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_TEXT_MODIFIED =
        new CodeMarker("com.microsoft.tfs.client.common.ui.wit.form.controls.FieldUpdateModifyListener#textModified"); //$NON-NLS-1$

    private final Field workItemField;

    public FieldUpdateModifyListener(final Field workItemField) {
        this.workItemField = workItemField;
    }

    @Override
    public void modifyText(final ModifyEvent e) {
        final Widget widget = e.widget;
        String value;

        if (widget instanceof Text) {
            final Text text = (Text) widget;
            value = text.getText();
        } else if (widget instanceof HTMLEditor) {
            final HTMLEditor editor = (HTMLEditor) widget;
            value = HTMLFilter.strip(editor.getHTML());
        } else if (widget instanceof AutocompleteCombo) {
            final AutocompleteCombo combo = (AutocompleteCombo) widget;
            value = combo.getText();
        } else if (widget instanceof Combo) {
            final Combo combo = (Combo) widget;

            value = combo.getText().trim();

            final String[] comboListItems = combo.getItems();
            for (int i = 0; i < comboListItems.length; i++) {
                /*
                 * I18N: need to use a java.text.Collator with a specified
                 * Locale. Set the Collator's strenth to SECONDARY (or possibly
                 * PRIMARY) since the desired semantic is a case-insensitive
                 * comparison.
                 */
                if (comboListItems[i].equalsIgnoreCase(value)) {
                    value = comboListItems[i];
                    break;
                }
            }
        } else {
            final String messageFormat = Messages.getString("FieldUpdateModifyListener.ListenerUnexpectedTypeFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, widget.getClass().getName());
            throw new IllegalStateException(message);
        }

        try {
            widget.setData(MODIFICATION_KEY, Boolean.TRUE);

            /*
             * SWT GTK workaround: focus events are not properly fired on first
             * paint, thus the focused widget never knows it has focus, thus
             * modify events can occur and the RequiredDecorationFocusListener
             * still thinks the widget is unfocused and invalid, and it will try
             * to clear the widget's state later, the next time it receives
             * focus. Thus, if a modify event occurs, let the
             * RequiredDecorationFocusListener know that we actually did have
             * focus.
             */
            RequiredDecorationFocusListener.setHasDecoration(widget, false);

            workItemField.setValue(widget, value);

            CodeMarkerDispatch.dispatch(CODEMARKER_TEXT_MODIFIED);
        } finally {
            widget.setData(MODIFICATION_KEY, null);
        }
    }
}
