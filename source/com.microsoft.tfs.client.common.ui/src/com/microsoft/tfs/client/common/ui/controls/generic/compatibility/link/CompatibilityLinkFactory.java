// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.util.Check;

/**
 * <p>
 * {@link CompatibilityLinkFactory} is used to create link widgets across
 * different versions of SWT. SWT introduced a native link widget in 3.1, and
 * this factory creates that widget when it is available. If the native link
 * widget is not available, a legacy fake-link control is created instead.
 * </p>
 *
 * <p>
 * Regardless of the type of link widget created, the returned object has the
 * type {@link CompatibilityLinkControl}. This interface allows client code
 * access to the same services no matter which underlying link control is
 * chosen.
 * </p>
 */
public class CompatibilityLinkFactory {
    /**
     * Creates a new link control with the specified parent and style.
     *
     * @param parent
     *        the link's parent {@link Composite} (must not be <code>null</code>
     *        )
     * @param style
     *        the link's style, or {@link SWT#NONE}
     * @return a {@link CompatibilityLinkControl} that wraps the new link widget
     *         (never <code>null</code>)
     */
    public static CompatibilityLinkControl createLink(final Composite parent, final int style) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        try {
            return createNewLink(parent, style);
        } catch (final Exception e) {
            return createLegacyLink(parent, style);
        }
    }

    private static CompatibilityLinkControl createLegacyLink(final Composite parent, final int style) {
        final LegacyLink link = new LegacyLink(parent, style);
        return new LegacyLinkCompatibilityWrapper(link);
    }

    private static CompatibilityLinkControl createNewLink(final Composite parent, final int style) throws Exception {
        final Control link = (Control) Class.forName("org.eclipse.swt.widgets.Link").getConstructor(new Class[] //$NON-NLS-1$
        {
            Composite.class,
            Integer.TYPE
        }).newInstance(new Object[] {
            parent,
            new Integer(SWT.NONE)
        });

        return new NewLinkCompatibilityWrapper(link);
    }

    private static class NewLinkCompatibilityWrapper implements CompatibilityLinkControl {
        private final Control link;

        public NewLinkCompatibilityWrapper(final Control link) {
            this.link = link;
        }

        @Override
        public void addSelectionListener(final SelectionListener listener) {
            try {
                link.getClass().getMethod("addSelectionListener", new Class[] //$NON-NLS-1$
                {
                    SelectionListener.class
                }).invoke(link, new Object[] {
                    listener
                });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Control getControl() {
            return link;
        }

        @Override
        public boolean isHyperlinkTextSupported() {
            return true;
        }

        @Override
        public void removeSelectionListener(final SelectionListener listener) {
            try {
                link.getClass().getMethod("removeSelectionListener", new Class[] //$NON-NLS-1$
                {
                    SelectionListener.class
                }).invoke(link, new Object[] {
                    listener
                });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void setSimpleText(final String text) {
            setText("<a>" + text + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
        }

        @Override
        public void setText(final String text) {
            try {
                link.getClass().getMethod("setText", new Class[] //$NON-NLS-1$
                {
                    String.class
                }).invoke(link, new Object[] {
                    text
                });
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class LegacyLinkCompatibilityWrapper implements CompatibilityLinkControl {
        private final LegacyLink link;

        public LegacyLinkCompatibilityWrapper(final LegacyLink link) {
            this.link = link;
        }

        @Override
        public void addSelectionListener(final SelectionListener listener) {
            link.addSelectionListener(listener);
        }

        @Override
        public void removeSelectionListener(final SelectionListener listener) {
            link.removeSelectionListener(listener);
        }

        @Override
        public Control getControl() {
            return link;
        }

        @Override
        public boolean isHyperlinkTextSupported() {
            return false;
        }

        @Override
        public void setSimpleText(final String text) {
            link.setText(text);
        }

        @Override
        public void setText(final String text) {
            link.setText(text);
        }
    }
}
