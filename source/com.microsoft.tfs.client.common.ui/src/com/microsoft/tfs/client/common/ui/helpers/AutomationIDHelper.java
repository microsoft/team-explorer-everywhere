// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.helpers;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Widget;

import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.util.Check;

/**
 * Helper class for assigning a unique name/id to widgets so that test UI
 * automation can find them.
 */
public class AutomationIDHelper {
    public static final String AUTOMATION_ID_DATA_KEY = "com.microsoft.tfs.test.automation.id"; //$NON-NLS-1$

    /**
     * Set the accessible/automation ID for the given widget. This uniquely
     * names the widget so that it can be found via UI automation.
     *
     * @param widget
     *        Widget to name
     *
     * @param id
     *        Accessible ID/control-name for the widget
     */
    public static void setWidgetID(final Widget widget, final String id) {
        Check.notNull(widget, "widget"); //$NON-NLS-1$

        widget.setData(AUTOMATION_ID_DATA_KEY, id);
    }

    /**
     * Sets the accessible / automation ID for a TableControl. This actually
     * sets the automation ID on the wrapped table control, so that users need
     * not have knowledge of the underlying table, and can use getTableById()
     * methods in swtbot.
     *
     * @param tableControl
     *        Table control to name
     * @param id
     *        Accessible ID/control-name for the widget
     */
    public static void setWidgetID(final TableControl tableControl, final String id) {
        Check.notNull(tableControl, "tableControl"); //$NON-NLS-1$

        setWidgetID(tableControl.getTable(), id);
    }

    /**
     * Sets the accessible / automation ID for a CheckboxTableViewer. This
     * actually sets the automation ID on the wrapped table control, so that
     * users need not have knowledge of the underlying table, and can use
     * getTableById() methods in swtbot.
     *
     * @param tableViewerControl
     *        Table Viewer Control to name
     * @param id
     *        Accessible ID/control-name for the widget
     */
    public static void setWidgetID(final TableViewer tableViewerControl, final String id) {
        Check.notNull(tableViewerControl, "tableViewerControl"); //$NON-NLS-1$

        setWidgetID(tableViewerControl.getTable(), id);
    }
}
