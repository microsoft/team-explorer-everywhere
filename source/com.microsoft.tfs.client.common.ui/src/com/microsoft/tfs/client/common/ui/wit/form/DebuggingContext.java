// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.core.clients.workitem.form.WIFormControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormGroup;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTab;

public class DebuggingContext {
    private static final Log log = LogFactory.getLog(DebuggingContext.class);
    private static final String KEY = DebuggingContext.class.getName() + "-bgcolor"; //$NON-NLS-1$
    private static final boolean DEBUG_ENABLED = Boolean.getBoolean("com.microsoft.tfs.ui.workitem.form.debug"); //$NON-NLS-1$
    private static final boolean DEBUG_MARGINS_ENABLED =
        Boolean.getBoolean("com.microsoft.tfs.ui.workitem.form.debug-margins"); //$NON-NLS-1$

    private final int[] BG_COLORS = new int[] {
        SWT.COLOR_BLUE,
        SWT.COLOR_CYAN,
        SWT.COLOR_GREEN,
        SWT.COLOR_MAGENTA,
        SWT.COLOR_RED,
        SWT.COLOR_YELLOW
    };

    private final String[] BG_COLOR_NAMES = new String[] {
        "BLUE", //$NON-NLS-1$
        "CYAN", //$NON-NLS-1$
        "GREEN", //$NON-NLS-1$
        "MAGENTA", //$NON-NLS-1$
        "RED", //$NON-NLS-1$
        "YELLOW" //$NON-NLS-1$
    };

    private final Display display;

    public DebuggingContext(final Display display) {
        this.display = display;
    }

    public void setupFormGroupLayout(final FormGroupLayout formGroupLayout) {
        if (DEBUG_MARGINS_ENABLED) {
            if (formGroupLayout.marginWidth == 0) {
                formGroupLayout.marginWidth = 5;
            }
            if (formGroupLayout.marginHeight == 0) {
                formGroupLayout.marginHeight = 5;
            }
        }
    }

    public void setupGridLayout(final GridLayout gridLayout) {
        if (DEBUG_MARGINS_ENABLED) {
            if (gridLayout.marginWidth == 0) {
                gridLayout.marginWidth = 5;
            }
            if (gridLayout.marginHeight == 0) {
                gridLayout.marginHeight = 5;
            }
        }
    }

    public void setupFillLayout(final FillLayout fillLayout) {
        if (DEBUG_MARGINS_ENABLED) {
            if (fillLayout.marginWidth == 0) {
                fillLayout.marginWidth = 5;
            }
            if (fillLayout.marginHeight == 0) {
                fillLayout.marginHeight = 5;
            }
        }
    }

    public void setupStackLayout(final StackLayout stackLayout) {
        if (DEBUG_MARGINS_ENABLED) {
            if (stackLayout.marginWidth == 0) {
                stackLayout.marginWidth = 5;
            }
            if (stackLayout.marginHeight == 0) {
                stackLayout.marginHeight = 5;
            }
        }
    }

    public void debug(final Composite composite, final WIFormElement formElement) {
        if (!DEBUG_ENABLED) {
            return;
        }

        final Integer parentIx = (Integer) composite.getParent().getData(KEY);
        int ix = 0;
        if (parentIx != null) {
            ix = parentIx.intValue() + 1;
        }

        if (ix >= BG_COLORS.length) {
            ix = 0;
        }

        composite.setBackground(display.getSystemColor(BG_COLORS[ix]));
        composite.setData(KEY, new Integer(ix));

        final StringBuffer sb = new StringBuffer();
        sb.append(getShortName(formElement));
        sb.append(" - "); //$NON-NLS-1$
        sb.append(BG_COLOR_NAMES[ix]);
        if (formElement instanceof WIFormGroup) {
            final WIFormGroup groupDescription = (WIFormGroup) formElement;
            if (groupDescription.getLabel() != null) {
                sb.append(" (" + groupDescription.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } else if (formElement instanceof WIFormControl) {
            final WIFormControl controlDescription = (WIFormControl) formElement;
            sb.append(" (" + controlDescription.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (formElement instanceof WIFormTab) {
            final WIFormTab tabDescription = (WIFormTab) formElement;
            sb.append(" (" + tabDescription.getLabel() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        log.trace(sb.toString());
    }

    private String getShortName(final WIFormElement formElement) {
        String s = formElement.getClass().getName();
        s = s.substring(s.lastIndexOf(".") + 1); //$NON-NLS-1$
        if (s.startsWith("WIForm")) //$NON-NLS-1$
        {
            s = s.substring(6);
        }
        if (s.endsWith("Impl")) //$NON-NLS-1$
        {
            s = s.substring(0, s.length() - 4);
        }
        return s;
    }
}
