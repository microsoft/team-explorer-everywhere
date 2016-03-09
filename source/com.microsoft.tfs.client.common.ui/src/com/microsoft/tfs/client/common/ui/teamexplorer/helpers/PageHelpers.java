// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.ui.framework.image.ImageUtils;

public class PageHelpers {
    public static ImageHyperlink createDropHyperlink(
        final FormToolkit toolkit,
        final Composite parent,
        final String text) {
        final ImageHyperlink link = toolkit.createImageHyperlink(parent, SWT.WRAP | SWT.CENTER | SWT.RIGHT);

        link.setBackground(parent.getBackground());
        link.setForeground(parent.getForeground());

        final Image arrow = ImageUtils.createDisclosureTriangle(link);

        link.setText(text);
        link.setImage(arrow);
        link.setUnderlined(false);

        link.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (arrow != null) {
                    arrow.dispose();
                }
            }
        });

        return link;
    }

    public static ImageHyperlink createDropHyperlink(
        final FormToolkit toolkit,
        final Composite parent,
        final String text,
        final Menu menu) {
        final ImageHyperlink link = createDropHyperlink(toolkit, parent, text);

        link.setMenu(menu);
        link.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                PageHelpers.showPopup(link);
            }
        });

        final Point point = link.getLocation();
        point.y += link.getSize().y;
        link.getMenu().setLocation(point);
        link.getMenu().setVisible(false);

        return link;
    }

    private static void showPopup(final Control control) {
        final Rectangle itemRectangle = control.getBounds();
        final Point point = control.getParent().toDisplay(new Point(itemRectangle.x, itemRectangle.y));
        control.getMenu().setLocation(point.x, point.y + itemRectangle.height);
        control.getMenu().setVisible(true);
    }
}
