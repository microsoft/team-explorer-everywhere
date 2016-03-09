// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.util.Check;

public class SWTUtil {
    public static Label createLabel(final Composite parent) {
        return createLabel(parent, SWT.NONE);
    }

    public static Label createLabel(final Composite parent, final int style) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        return new Label(parent, style);
    }

    public static Label createLabel(final Composite parent, final Image image) {
        return createLabel(parent, SWT.NONE, image);
    }

    public static Label createLabel(final Composite parent, final int style, final Image image) {
        final Label label = createLabel(parent, style);

        if (image != null) {
            label.setImage(image);
        }

        return label;
    }

    public static Label createLabel(final Composite parent, final String text) {
        return createLabel(parent, SWT.NONE, text);
    }

    public static Label createLabel(final Composite parent, final int style, final String text) {
        final Label label = createLabel(parent, style);

        if (text != null) {
            label.setText(text);
        }

        return label;
    }

    public static Label createVerticalSeparator(final Composite parent) {
        return createVerticalSeparator(parent, SWT.NONE);
    }

    public static Label createVerticalSeparator(final Composite parent, final int style) {
        return createSeparator(parent, style | SWT.VERTICAL);
    }

    public static Label createHorizontalSeparator(final Composite parent) {
        return createHorizontalSeparator(parent, SWT.NONE);
    }

    public static Label createHorizontalSeparator(final Composite parent, final int style) {
        return createSeparator(parent, style | SWT.HORIZONTAL);
    }

    public static Label createSeparator(final Composite parent, final int style) {
        return createLabel(parent, style | SWT.SEPARATOR);
    }

    public static Button createButton(final Composite parent, final String text) {
        return createButton(parent, SWT.NONE, text);
    }

    public static Button createButton(final Composite parent, final int style, final String text) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        final Button button = new Button(parent, style);

        if (text != null) {
            button.setText(text);
        }

        return button;
    }

    public static GridLayout gridLayout(final Composite composite) {
        return gridLayout(composite, 1);
    }

    public static GridLayout gridLayout(final Composite composite, final int numColumns) {
        return gridLayout(composite, numColumns, false);
    }

    public static GridLayout gridLayout(
        final Composite composite,
        final int numColumns,
        final boolean makeColumnsEqualWidth) {
        return gridLayout(composite, numColumns, makeColumnsEqualWidth, 5, 5);
    }

    public static GridLayout gridLayout(
        final Composite composite,
        final int numColumns,
        final boolean makeColumnsEqualWidth,
        final int marginWidth,
        final int marginHeight) {
        Check.notNull(composite, "composite"); //$NON-NLS-1$

        final GridLayout layout = new GridLayout(numColumns, makeColumnsEqualWidth);
        layout.marginWidth = marginWidth;
        layout.marginHeight = marginHeight;

        composite.setLayout(layout);

        return layout;
    }

    public static FillLayout fillLayout(final Composite composite) {
        return fillLayout(composite, SWT.HORIZONTAL);
    }

    public static FillLayout fillLayout(final Composite composite, final int type) {
        return fillLayout(composite, type, 0, 0, 0);
    }

    public static FillLayout fillLayout(
        final Composite composite,
        final int type,
        final int marginWidth,
        final int marginHeight,
        final int spacing) {
        Check.notNull(composite, "composite"); //$NON-NLS-1$

        final FillLayout layout = new FillLayout(type);
        layout.marginWidth = marginWidth;
        layout.marginHeight = marginHeight;
        layout.spacing = spacing;

        composite.setLayout(layout);

        return layout;
    }

    public static FormLayout formLayout(final Composite composite) {
        return formLayout(composite, 0, 0, 0);
    }

    public static FormLayout formLayout(final Composite composite, final int marginWidth, final int marginHeight) {
        return formLayout(composite, marginWidth, marginHeight, 0);
    }

    public static FormLayout formLayout(
        final Composite composite,
        final int marginWidth,
        final int marginHeight,
        final int spacing) {
        Check.notNull(composite, "composite"); //$NON-NLS-1$

        final FormLayout layout = new FormLayout();
        layout.marginWidth = marginHeight;
        layout.marginHeight = marginHeight;
        layout.spacing = spacing;

        composite.setLayout(layout);

        return layout;
    }

    public static Composite createComposite(final Composite parent) {
        return createComposite(parent, SWT.NONE);
    }

    public static Composite createComposite(final Composite parent, final int style) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        return new Composite(parent, style);
    }

    public static Group createGroup(final Composite parent, final String text) {
        return createGroup(parent, SWT.NONE, text);
    }

    public static Group createGroup(final Composite parent, final int style, final String text) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        final Group group = new Group(parent, style);
        if (text != null) {
            group.setText(text);
        }
        return group;
    }

    public static Control createVerticalGridLayoutSpacer(final Composite parent, final int verticalSpan) {
        return createGridLayoutSpacer(parent, SWT.DEFAULT, SWT.DEFAULT, 1, verticalSpan);
    }

    public static Control createVerticalGridLayoutSpacer(
        final Composite parent,
        final int heightHint,
        final int verticalSpan) {
        return createGridLayoutSpacer(parent, SWT.DEFAULT, heightHint, 1, verticalSpan);
    }

    public static Control createHorizontalGridLayoutSpacer(final Composite parent, final int horizontalSpan) {
        return createGridLayoutSpacer(parent, SWT.DEFAULT, SWT.DEFAULT, horizontalSpan, 1);
    }

    public static Control createHorizontalGridLayoutSpacer(
        final Composite parent,
        final int widthHint,
        final int horizontalSpan) {
        return createGridLayoutSpacer(parent, widthHint, SWT.DEFAULT, horizontalSpan, 1);
    }

    public static Control createGridLayoutSpacer(final Composite parent) {
        return createGridLayoutSpacer(parent, SWT.DEFAULT, SWT.DEFAULT, 1, 1);
    }

    public static Control createGridLayoutSpacer(
        final Composite parent,
        final int horizontalSpan,
        final int verticalSpan) {
        return createGridLayoutSpacer(parent, SWT.DEFAULT, SWT.DEFAULT, horizontalSpan, verticalSpan);
    }

    public static Control createGridLayoutSpacer(
        final Composite parent,
        final int widthHint,
        final int heightHint,
        final int horizontalSpan,
        final int verticalSpan) {
        final Label label = createLabel(parent);
        label.setVisible(false);
        final GridData gd = new GridData();
        gd.widthHint = widthHint;
        gd.heightHint = heightHint;
        gd.horizontalSpan = horizontalSpan;
        gd.verticalSpan = verticalSpan;
        label.setLayoutData(gd);
        return label;
    }

    public static int addGridLayoutVerticalIndent(final Control controls, final int verticalIndent) {
        return addGridLayoutVerticalIndent(new Control[] {
            controls
        }, verticalIndent);
    }

    public static int addGridLayoutVerticalIndent(final Control[] controls, final int verticalIndent) {
        Check.notNull(controls, "controls"); //$NON-NLS-1$

        if (SWT.getVersion() >= 3100) {
            for (int i = 0; i < controls.length; i++) {
                GridData gridData = (GridData) controls[i].getLayoutData();
                if (gridData == null) {
                    gridData = new GridData();
                    controls[i].setLayoutData(gridData);
                }

                try {
                    final Class gridDataClass = gridData.getClass();
                    final Field viField = gridDataClass.getField("verticalIndent"); //$NON-NLS-1$

                    viField.setInt(gridData, verticalIndent);
                } catch (final Exception e) {
                    break;
                }
            }
            return 0;
        }

        final GridLayout layout = (GridLayout) controls[0].getParent().getLayout();
        final int spacing = Math.max(0, verticalIndent - layout.verticalSpacing);
        final Control spacer =
            createGridLayoutSpacer(controls[0].getParent(), SWT.DEFAULT, spacing, layout.numColumns, 1);
        spacer.moveAbove(controls[0]);
        return 1;
    }

    public static TabItem createTabItem(final TabFolder parent, final String text) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        final TabItem tabItem = new TabItem(parent, SWT.NONE);

        if (text != null) {
            tabItem.setText(text);
        }

        return tabItem;
    }

    /**
     * Enables or disables a composite and all its children, recursively. Some
     * platforms (Mac OS) do not disable composites appropriately: for example,
     * when setting a group disabled, the children are unnavigable but still
     * drawn as enabled.
     *
     * @param composite
     *        The composite to enable or disable (not <code>null</code>)
     * @param enabled
     *        True to enable, false to disable
     */
    public static void setCompositeEnabled(final Composite composite, final boolean enabled) {
        Check.notNull(composite, "composite"); //$NON-NLS-1$

        final Control[] children = composite.getChildren();

        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof Composite) {
                setCompositeEnabled((Composite) children[i], enabled);
            } else {
                children[i].setEnabled(enabled);
            }
        }

        composite.setEnabled(enabled);
    }

    public static void setSelectAllOnFocusGained(final Text text) {
        Check.notNull(text, "text"); //$NON-NLS-1$
        text.addFocusListener(TEXT_SELECT_ALL_FOCUS_LISTENER);
    }

    private static final Log log = LogFactory.getLog(SWTUtil.class);

    private static FocusListener TEXT_SELECT_ALL_FOCUS_LISTENER = new FocusAdapter() {
        @Override
        public void focusGained(final FocusEvent e) {
            ((Text) e.widget).selectAll();
        }
    };
}
