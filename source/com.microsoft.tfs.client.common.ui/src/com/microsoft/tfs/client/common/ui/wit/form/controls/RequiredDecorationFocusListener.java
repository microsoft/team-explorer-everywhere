// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

public class RequiredDecorationFocusListener implements FocusListener {
    private static final String ADDED_DECORATION_WIDGET_DATA_KEY = "wit-focus-listener"; //$NON-NLS-1$
    private static final String DECORATION_TEXT = Messages.getString("RequiredDecorationFocusListener.DecorationText"); //$NON-NLS-1$

    private final Field workItemField;
    private final String modifyListenerWidgetDataKey;

    public RequiredDecorationFocusListener(final Field workItemField, final String modifyListenerWidgetDataKey) {
        this.workItemField = workItemField;
        this.modifyListenerWidgetDataKey = modifyListenerWidgetDataKey;
    }

    @Override
    public void focusGained(final FocusEvent e) {
        removeDecoration(e.widget, modifyListenerWidgetDataKey);
    }

    @Override
    public void focusLost(final FocusEvent e) {
        addDecoration(workItemField, e.widget, modifyListenerWidgetDataKey);

        /*
         * TODO: this block should probably live somewhere else, as it's only
         * needed for FieldControl's Combo
         */
        if (e.widget instanceof Combo) {
            final Combo combo = (Combo) e.widget;

            final String comboText = combo.getText().trim();
            final String[] comboItems = combo.getItems();

            for (int i = 0; i < comboItems.length; i++) {
                /*
                 * I18N: need to use a java.text.Collator with a specified
                 * Locale. Set the Collator's strenth to SECONDARY (or possibly
                 * PRIMARY) since the desired semantic is a case-insensitive
                 * comparison.
                 */
                if (comboItems[i].equalsIgnoreCase(comboText)) {
                    ModifyListener modifyListener = null;
                    if (modifyListenerWidgetDataKey != null) {
                        modifyListener = (ModifyListener) combo.getData(modifyListenerWidgetDataKey);
                    }

                    if (modifyListener != null) {
                        combo.removeModifyListener(modifyListener);
                    }

                    combo.setText(comboItems[i]);

                    if (modifyListener != null) {
                        combo.addModifyListener(modifyListener);
                    }

                    break;
                }
            }
        }
    }

    public static void removeDecoration(final Widget widget, final String modifyListenerWidgetDataKey) {
        if (hasDecoration(widget)) {
            ModifyListener modifyListener = null;
            if (modifyListenerWidgetDataKey != null) {
                modifyListener = (ModifyListener) widget.getData(modifyListenerWidgetDataKey);
            }

            if (widget instanceof Text) {
                final Text text = (Text) widget;

                if (modifyListener != null) {
                    text.removeModifyListener(modifyListener);
                }

                text.setText(""); //$NON-NLS-1$

                if (modifyListener != null) {
                    text.addModifyListener(modifyListener);
                }
            } else {
                final Combo combo = (Combo) widget;

                if (modifyListener != null) {
                    combo.removeModifyListener(modifyListener);
                }

                combo.setText(""); //$NON-NLS-1$

                if (modifyListener != null) {
                    combo.addModifyListener(modifyListener);
                }
            }

            setHasDecoration(widget, false);
        }
    }

    public static void addDecoration(
        final Field workItemField,
        final Widget widget,
        final String modifyListenerWidgetDataKey) {
        if (needToAddDecoration(workItemField, widget)) {
            ModifyListener modifyListener = null;
            if (modifyListenerWidgetDataKey != null) {
                modifyListener = (ModifyListener) widget.getData(modifyListenerWidgetDataKey);
            }

            if (widget instanceof Text) {
                final Text text = (Text) widget;

                if (modifyListener != null) {
                    text.removeModifyListener(modifyListener);
                }

                text.setText(DECORATION_TEXT);
                text.setSelection(new Point(0, DECORATION_TEXT.length()));

                if (modifyListener != null) {
                    text.addModifyListener(modifyListener);
                }
            } else {
                final Combo combo = (Combo) widget;

                if (modifyListener != null) {
                    combo.removeModifyListener(modifyListener);
                }

                combo.setText(DECORATION_TEXT);
                combo.setSelection(new Point(0, DECORATION_TEXT.length()));

                if (modifyListener != null) {
                    combo.addModifyListener(modifyListener);
                }
            }

            setHasDecoration(widget, true);
        }
    }

    private static boolean needToAddDecoration(final Field workItemField, final Widget widget) {
        /*
         * have we already added the decoration to this field?
         */
        if (hasDecoration(widget)) {
            /*
             * no need to add it again
             */
            return false;
        }

        /*
         * is the field valid?
         */
        if (workItemField.getStatus() == FieldStatus.VALID) {
            /*
             * the decoration is not added to valid fields
             */
            return false;
        }

        /*
         * is the field invalid with INVALID_EMPTY? (the field control case)
         */
        if (workItemField.getStatus() == FieldStatus.INVALID_EMPTY) {
            return true;
        }

        /*
         * is the field invalid with INVALID_PATH? (the classification control
         * case)
         */
        if (workItemField.getStatus() == FieldStatus.INVALID_PATH) {
            /*
             * in this case, the decoration is added if the classification
             * control's text widget is empty
             */
            final Combo combo = (Combo) widget;
            return combo.getText().trim().length() == 0;
        }

        /*
         * no decoration is needed - the field is invalid with an invalid type
         * that is not decorated
         */
        return false;
    }

    public static void setHasDecoration(final Widget widget, final boolean hasDecoration) {
        if (hasDecoration) {
            widget.setData(ADDED_DECORATION_WIDGET_DATA_KEY, Boolean.valueOf(true));
        } else {
            widget.setData(ADDED_DECORATION_WIDGET_DATA_KEY, null);
        }
    }

    public static boolean hasDecoration(final Widget widget) {
        final Boolean addedDecoration = (Boolean) widget.getData(ADDED_DECORATION_WIDGET_DATA_KEY);
        return (addedDecoration != null && addedDecoration.booleanValue());
    }
}
