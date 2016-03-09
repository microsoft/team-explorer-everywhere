// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.teamexplorer;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.ImageButton;
import com.microsoft.tfs.client.common.ui.framework.helper.ColorUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.image.ImageUtils;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationLinkConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.link.ITeamExplorerNavigationLink;

/**
 * The new Team Explorer tile control on TE home page
 */
public class TeamExplorerTileControl extends BaseControl {
    private final Composite composite;
    private final ImageButton colorBar;
    private final Composite iconComposite;
    private final ImageButton iconArea;

    private final Composite titleComposite;
    private final Label titleArea;
    private final ImageButton splitter;
    private final Composite dropdownComposite;
    private final ImageHyperlink dropdown;

    private MenuManager popupMenuManager;

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private final Display display = getDisplay();
    private final Color titleBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    private final Color iconBackground = display.getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT);
    private final Color titleForeground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
    private final Color hoverIconBackground = display.getSystemColor(SWT.COLOR_GRAY);
    private final Color hoverTitleBackground = display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT);
    private final Color hoverTitleForeground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);

    private final int colorBarWidth = 4;
    private final int colorBarHeight = 36;
    private final int iconSize = 24;

    public TeamExplorerTileControl(final FormToolkit toolkit, final Composite parent, final int style) {
        super(parent, style);
        SWTUtil.gridLayout(this, 1, true, 0, 0);

        composite = new Composite(this, SWT.NONE);
        GridLayout gridLayout = new GridLayout(5, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        composite.setLayout(gridLayout);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(composite);
        toolkit.paintBordersFor(composite);
        toolkit.adapt(composite);

        colorBar = new ImageButton(composite, SWT.NONE);
        GridDataBuilder.newInstance().applyTo(colorBar);

        iconComposite = new Composite(composite, SWT.NONE);
        iconComposite.setSize(colorBarHeight, colorBarHeight);
        GridDataBuilder.newInstance().vGrab().vFill().hFill().applyTo(iconComposite);
        SWTUtil.gridLayout(iconComposite, 1, true, 6, 0);

        iconArea = new ImageButton(iconComposite, SWT.NONE);
        GridDataBuilder.newInstance().vGrab().hFill().vAlignCenter().applyTo(iconArea);

        titleComposite = new Composite(composite, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 10;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        GridDataBuilder.newInstance().vGrab().hGrab().hFill().vFill().applyTo(titleComposite);
        titleComposite.setLayout(gridLayout);

        titleArea = new Label(titleComposite, SWT.FLAT);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vAlignCenter().applyTo(titleArea);

        splitter = new ImageButton(composite, SWT.NONE);
        GridDataBuilder.newInstance().vGrab().vAlignCenter().applyTo(splitter);

        dropdownComposite = new Composite(composite, SWT.NONE);
        gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        GridDataBuilder.newInstance().vGrab().vFill().applyTo(dropdownComposite);
        dropdownComposite.setLayout(gridLayout);

        dropdown = new ImageHyperlink(dropdownComposite, SWT.NONE);
        GridDataBuilder.newInstance().vGrab().vAlignCenter().hAlignCenter().applyTo(dropdown);

        setDefaultColor();
        hookMouseMoveListener();

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
            }
        });
    }

    private void setDefaultColor() {
        setControlColor(iconBackground, titleBackground, titleForeground);
    }

    private void setHoverColor() {
        setControlColor(hoverIconBackground, hoverTitleBackground, hoverTitleForeground);
    }

    private void hookMouseMoveListener() {
        final MouseTrackListener listener = new MouseTrackListener() {
            @Override
            public void mouseEnter(final MouseEvent e) {
                setHoverColor();
            }

            @Override
            public void mouseExit(final MouseEvent e) {
                setDefaultColor();
            }

            @Override
            public void mouseHover(final MouseEvent e) {
                setHoverColor();
            }
        };

        final Cursor cursorHand = display.getSystemCursor(SWT.CURSOR_HAND);
        iconComposite.addMouseTrackListener(listener);
        iconArea.addMouseTrackListener(listener);
        titleComposite.addMouseTrackListener(listener);
        titleArea.addMouseTrackListener(listener);
        splitter.addMouseTrackListener(listener);
        composite.addMouseTrackListener(listener);
        iconComposite.setCursor(cursorHand);
        iconArea.setCursor(cursorHand);
        titleComposite.setCursor(cursorHand);
        titleArea.setCursor(cursorHand);
        splitter.setCursor(cursorHand);
        composite.setCursor(cursorHand);
        dropdownComposite.setCursor(cursorHand);

        final MouseTrackListener dropdownListener = new MouseTrackListener() {
            @Override
            public void mouseEnter(final MouseEvent e) {
                dropdownComposite.setBackground(hoverTitleBackground);
                dropdown.setBackground(hoverTitleBackground);
            }

            @Override
            public void mouseExit(final MouseEvent e) {
                dropdownComposite.setBackground(titleBackground);
                dropdown.setBackground(titleBackground);
            }

            @Override
            public void mouseHover(final MouseEvent e) {
                dropdownComposite.setBackground(hoverTitleBackground);
                dropdown.setBackground(hoverTitleBackground);
            }
        };

        dropdownComposite.addMouseTrackListener(dropdownListener);
        dropdown.addMouseTrackListener(dropdownListener);
    }

    public void setTitle(final String text) {
        titleArea.setText(text);
    }

    public void setColorBar(final String navId) {
        String colorHex = ColorTheme.stripColorMap.get(navId);
        if (colorHex == null) {
            colorHex = ColorTheme.OtherStripColor;
        }

        final Image enabledColorBar = ImageUtils.createRectangular(
            composite,
            ColorUtils.hexadecimalToColor(display, colorHex),
            colorBarWidth,
            colorBarHeight);

        colorBar.setEnabledImage(enabledColorBar);
        layout();
    }

    public void setIcon(final Image image) {
        Image scaledImage = image;
        if (image.getBounds().width != iconSize || image.getBounds().height != iconSize) {
            scaledImage = ImageUtils.grayScaleImage(image, iconSize, iconSize);
        }
        iconArea.setEnabledImage(scaledImage);
        iconArea.setDisabledImage(scaledImage);
        layout();
    }

    @Override
    public void addMouseListener(final MouseListener listener) {
        composite.addMouseListener(listener);
        iconComposite.addMouseListener(listener);
        titleArea.addMouseListener(listener);
        titleComposite.addMouseListener(listener);
        colorBar.addMouseListener(listener);
        iconArea.addMouseListener(listener);
    }

    public void addMenuItems(
        final Action openAction,
        final Action openInWebAction,
        final TeamExplorerNavigationLinkConfig[] items,
        final TeamExplorerContext context,
        final TeamExplorerNavigator navigator,
        final TeamExplorerNavigationItemConfig parentNavigationItem) {
        popupMenuManager = new MenuManager("#popup"); //$NON-NLS-1$
        popupMenuManager.setRemoveAllWhenShown(true);

        bindMenu();

        splitter.setEnabledImage(imageHelper.getImage("/images/teamexplorer/tebutton_splitter.png")); //$NON-NLS-1$
        splitter.setDisabledImage(imageHelper.getImage("/images/teamexplorer/tebutton_splitter.png")); //$NON-NLS-1$
        dropdown.setImage(imageHelper.getImage("/images/teamexplorer/tee_drop_arrow.png")); //$NON-NLS-1$
        layout();

        popupMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                manager.add(openAction);
                if (openInWebAction != null) {
                    manager.add(openInWebAction);
                }
                manager.add(new Separator());

                if (items != null && items.length > 0) {
                    for (final TeamExplorerNavigationLinkConfig item : items) {
                        final ITeamExplorerNavigationLink link = item.createInstance();
                        if (link.isVisible(context)) {
                            manager.add(
                                new SubItemAction(item.getTitle(), link, context, navigator, parentNavigationItem));
                        }
                    }
                }
            }
        });
    }

    private void bindMenu() {
        final Menu menu = popupMenuManager.createContextMenu(composite.getShell());
        composite.setMenu(menu);
        dropdown.setMenu(menu);

        dropdown.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                menu.setLocation(dropdownComposite.toDisplay(0, dropdownComposite.getSize().y));
                menu.setVisible(true);
            }
        });

        final MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseUp(final MouseEvent e) {
                if (e.button == TeamExplorerHelpers.MOUSE_RIGHT_BUTTON) {
                    menu.setVisible(true);
                }
            }
        };

        composite.addMouseListener(listener);
        iconComposite.addMouseListener(listener);
        titleArea.addMouseListener(listener);
        titleComposite.addMouseListener(listener);
        colorBar.addMouseListener(listener);
        iconArea.addMouseListener(listener);
    }

    private void setControlColor(
        final Color iconBackgroundColor,
        final Color titleBackgroundColor,
        final Color titleForegroundColor) {
        composite.setBackground(titleBackgroundColor);
        iconComposite.setBackground(iconBackgroundColor);
        iconArea.setBackground(iconBackgroundColor);
        titleComposite.setBackground(titleBackgroundColor);
        titleArea.setBackground(titleBackgroundColor);
        titleArea.setForeground(titleForegroundColor);
        splitter.setBackground(titleBackgroundColor);
        dropdownComposite.setBackground(titleBackgroundColor);
        dropdown.setBackground(titleBackgroundColor);
    }

    private class SubItemAction extends Action {
        private final ITeamExplorerNavigationLink link;
        private final TeamExplorerContext context;
        private final TeamExplorerNavigator navigator;
        private final TeamExplorerNavigationItemConfig parentNavigationItem;

        public SubItemAction(
            final String title,
            final ITeamExplorerNavigationLink link,
            final TeamExplorerContext context,
            final TeamExplorerNavigator navigator,
            final TeamExplorerNavigationItemConfig parentNavigationItem) {
            this.link = link;
            this.context = context;
            this.navigator = navigator;
            this.parentNavigationItem = parentNavigationItem;

            setEnabled(link.isEnabled(context));
            setText(title);
        }

        @Override
        public void run() {
            link.clicked(composite.getShell(), context, navigator, parentNavigationItem);
        }
    }

    private static class ColorTheme {
        protected static final String OtherStripColor = "#008b8b"; //$NON-NLS-1$

        protected static final Map<String, String> stripColorMap = new HashMap<String, String>() {
            {
                put(TeamExplorerHelpers.PendingChangeNavItemID, "#6b207b"); //$NON-NLS-1$
                put(TeamExplorerHelpers.VersionControlNavItemID, "#68217a"); //$NON-NLS-1$
                put(TeamExplorerHelpers.WorkItemNavItemID, "#009ece"); //$NON-NLS-1$
                put(TeamExplorerHelpers.BuildNavItemID, "#73828c"); //$NON-NLS-1$
                put(TeamExplorerHelpers.DocumentsNavItemID, "#fecc00"); //$NON-NLS-1$
                put(TeamExplorerHelpers.ReportsNavItemID, "#bf00ba"); //$NON-NLS-1$
                put(TeamExplorerHelpers.SettingsNavItemID, "#0079ce"); //$NON-NLS-1$
            }
        };
    }
}
