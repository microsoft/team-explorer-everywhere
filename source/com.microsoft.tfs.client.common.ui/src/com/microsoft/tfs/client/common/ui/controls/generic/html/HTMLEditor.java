// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.html;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.controls.generic.FullFeaturedBrowser;
import com.microsoft.tfs.client.common.ui.controls.generic.html.DropdownToolItemSelectionListener.MenuItemSelectedHandler;
import com.microsoft.tfs.client.common.ui.dialogs.generic.StringInputDialog;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipLabelManager;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * A rich text editing control for HTML content. Only supported on Eclipse 3.5
 * and later (though this class compiles on Eclipse 3.2 and newer) where the
 * Browser hosts Internet Explorer, Mozilla, or Webkit. Use
 * {@link #isAvailable()} to test whether the control will load in the current
 * platform ({@link Browser} will load and supports the required Javascript/HTML
 * features). If the {@link #DISABLE_PROPERTY_NAME} system property is set, the
 * control always reports itself unavailable.
 * </p>
 * <h1>Asynchronous Usage Notice</h1>
 * <p>
 * Some methods on this control may only be executed <b>after</b> the browser
 * completes the load of the Javascript editor content, which can only be
 * reliably detected via the {@link EditorReadyListener} supplied at
 * construction. This behavior is dictated by the underlying {@link Browser}
 * control, which runs Javascript asynchronously. Methods which <b>cannot</b> be
 * used until the editor is ready:
 * <ul>
 * <li>{@link #getReadOnly()}</li>
 * <li>{@link #setReadOnly(boolean)}</li>
 * <li>{@link #getHTML()}</li>
 * <li>{@link #setHTML(String)}</li>
 * </ul>
 * If you call these methods before the editor is ready, they will throw
 * {@link IllegalStateException}.
 * </p>
 *
 * @threadsafety thread-compatible
 */
public class HTMLEditor extends Composite {
    /**
     * {@link HTMLEditor} is always unavailable ({@link #isAvailable()} returns
     * <code>false</code>) when this system property is set to any value.
     */
    public static final String DISABLE_PROPERTY_NAME =
        "com.microsoft.tfs.client.common.ui.controls.generic.html.htmleditor.disable"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(HTMLEditor.class);

    /**
     * Minimum version of SWT required to use the HTML editor. 3.5 and later are
     * supported.
     */
    private static final int MINIMUM_SWT_VERSION = 3500;

    private static final int TOOL_TIP_SHOW_DELAY_MILLISECONDS = 1000;

    /**
     * The name of the Javascript HTML editor resource.
     */
    private static final String HTML_EDITOR_RESOURCE = "HTMLEditor.html"; //$NON-NLS-1$

    /**
     * The encoding used to read the {@link #HTML_EDITOR_RESOURCE} resource.
     */
    private static final String HTML_EDITOR_RESOURCE_ENCODING = "UTF-8"; //$NON-NLS-1$

    /**
     * The default font size (HTML size) for the font drop-down.
     */
    private static final String DEFAULT_FONT_SIZE = "2"; //$NON-NLS-1$

    /**
     * The modifier key which must be down when clicking or pressing Enter on a
     * link to open it (instead of edit it).
     */
    private static final int OPEN_LINK_MODIFIER_KEY =
        WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA) ? SWT.COMMAND : SWT.CONTROL;

    /**
     * Matches strings like "rgb(0,0,0)", "rgb (1, 2 ,3 )"
     */
    private static final Pattern RGB_CSS_COLOR_PATTERN = Pattern.compile(
        "rgb[\\s]*\\(([^,]*?),([^,]*?),([^\\)]*?)\\)", //$NON-NLS-1$
        Pattern.CASE_INSENSITIVE);

    /**
     * Initialized by {@link #isAvailable()} to <code>true</code> if this
     * control can be used on the running platform, <code>false</code> if it
     * cannot, null if not yet tested.
     */
    private static Boolean browserAvailable;
    private static int browserAvailableForStyle;

    /**
     * The {@link FullFeaturedBrowser} used to host the JavaScript-based editor.
     */
    private final FullFeaturedBrowser browser;

    /**
     * Set to <code>true</code> when the {@link FullFeaturedBrowser} completes
     * the load of the Javascript HTML editor program. Tracked for safety, so
     * some methods can throw {@link IllegalStateException} if the editor has
     * not been loaded (instead of risking putting the native browser into a bad
     * state).
     */
    private boolean editorReady = false;

    /**
     * Editor ready listener; one is given in the constructor
     */
    private final EditorReadyListener editorReadyListener;

    /**
     * Content modification listeners.
     */
    private final SingleListenerFacade modifyListeners = new SingleListenerFacade(ModifyListener.class);

    /**
     * Provides toolbar images.
     */
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    /*
     * Tool bar items.
     */
    private final CoolBar coolBar;
    private Combo fontSizeCombo;
    private Combo fontNameCombo;
    private MenuItem leftMenuItem;
    private MenuItem centerMenuItem;
    private MenuItem rightMenuItem;
    private MenuItem justifyMenuItem;
    private ToolItem underlineButtonItem;
    private ToolItem italicButtonItem;
    private ToolItem boldButtonItem;
    private ToolItem indentButtonItem;
    private ToolItem outdentButtonItem;
    private ToolItem orderedListButtonItem;
    private ToolItem unorderedListButtonItem;
    private ToolItem linkButtonItem;

    private Shell toolTipShell;
    private Timer toolTipShellTimer;

    /*
     * Styles computed per-platform so the control matches the native HTML
     * editing experience.
     */
    private final int coolbarStyle;
    private final int toolbarStyle;
    private final int fullFeaturedBrowserStyle;

    /**
     * Gets whether {@link HTMLEditor} is availble for the running platform
     * (browser requirements are met). If this method returns <code>false</code>
     * , construction via
     * {@link #HTMLEditor(Composite, EditorReadyListener, int)} will throw.
     *
     * @return <code>true</code> if {@link HTMLEditor} is supported on the
     *         running platform, <code>false</code> if it is disabled (the
     *         {@link #DISABLE_PROPERTY_NAME} system property is set) or if
     *         minimum browser requirements are not met (see the log for
     *         details)
     * @see HTMLEditor
     * @see HTMLEditor#DISABLE_PROPERTY_NAME
     */
    public static boolean isAvailable() {
        if (System.getProperty(DISABLE_PROPERTY_NAME) != null) {
            return false;
        }

        /*
         * No synchronization here because this method is always called on the
         * UI thread.
         */
        final int browserStyle = getBrowserStyle();
        if (HTMLEditor.browserAvailable == null || browserStyle != HTMLEditor.browserAvailableForStyle) {
            HTMLEditor.browserAvailable = Boolean.valueOf(isAvailableInternal());
            HTMLEditor.browserAvailableForStyle = browserStyle;
        }

        return HTMLEditor.browserAvailable;
    }

    private static boolean isAvailableInternal() {
        /*
         * OS X's SWT native browser is bad on Carbon, avoid it.
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230035
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            HTMLEditor.log.warn("HTMLEditor does not support SWT Browser on Mac OS Carbon"); //$NON-NLS-1$
            return false;
        }

        if (SWT.getVersion() < HTMLEditor.MINIMUM_SWT_VERSION) {
            HTMLEditor.log.warn(
                MessageFormat.format(
                    "SWT version {0} not new enough ({1} or newer required) to use HTMLEditor", //$NON-NLS-1$
                    Integer.toString(SWT.getVersion()),
                    Integer.toString(HTMLEditor.MINIMUM_SWT_VERSION)));
            return false;
        }

        Shell shell = null;
        FullFeaturedBrowser browser = null;

        try {
            shell = new Shell();
            browser = new FullFeaturedBrowser(shell, SWT.NONE, getBrowserStyle());

            browser.setJavascriptEnabled(true);
            if (browser.getJavascriptEnabled() == false) {
                HTMLEditor.log.warn("Could not enable Javascript in SWT Browser for HTMLEditor"); //$NON-NLS-1$
                return false;
            }

            /*
             * On Windows, only IE 7 and newer will run our Javascript
             * correctly. We don't use XMLHttpRequest, but it's a good test for
             * IE 6, which doesn't have it (IE 7+ does).
             */
            if (Platform.isCurrentPlatform(Platform.WINDOWS) && browser.getBrowserType().equalsIgnoreCase("ie")) //$NON-NLS-1$
            {
                // Our Javascript needs a document to run with.
                browser.setText("<html></html>"); //$NON-NLS-1$

                final Object hasXMLHttpRequest = browser.evaluate("return ('XMLHttpRequest' in window);"); //$NON-NLS-1$

                if (hasXMLHttpRequest != null
                    && hasXMLHttpRequest instanceof Boolean
                    && ((Boolean) hasXMLHttpRequest).booleanValue() == false) {
                    HTMLEditor.log.warn("IE major version 6 detected; this version not supported by HTMLEditor"); //$NON-NLS-1$
                    return false;
                }
            }

            // Success!
            HTMLEditor.log.info("SWT Browser successfully loaded for HTMLEditor"); //$NON-NLS-1$
            return true;
        } catch (final Throwable t) {
            HTMLEditor.log.warn("SWT Browser failed to load for HTMLEditor", t); //$NON-NLS-1$
            return false;
        } finally {
            if (browser != null) {
                browser.dispose();
            }

            if (shell != null) {
                shell.dispose();
            }
        }
    }

    private static int getBrowserStyle() {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();
        return preferences.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE);
    }

    /**
     * Constructs an {@link HTMLEditor}. Unlike many SWT widgets,
     * {@link HTMLEditor}'s initialization is asynchronous because the native
     * web browser can run Javascript in the background. The given
     * {@link EditorReadyListener} is invoked when the control is ready. See the
     * class Javadoc for details.
     *
     * @param parent
     *        the widget's parent (must not be <code>null</code>)
     * @param editorReadyListener
     *        the listener to be notified when the HTML editor has completed
     *        loading and is ready for use (must not be <code>null</code>)
     * @param style
     *        the widget's style (the control will automatically enable
     *        {@link SWT#BORDER} on some platforms to match the appearance of
     *        native HTML editor controls, so it's best for users not to set
     *        that flag)
     * @throws IllegalStateException
     *         if the control is not available for this platform (
     *         {@link #isAvailable()} would return <code>false</code>)
     */
    public HTMLEditor(final Composite parent, final EditorReadyListener editorReadyListener, final int style) {
        super(parent, style);

        Check.notNull(parent, "parent"); //$NON-NLS-1$
        Check.notNull(editorReadyListener, "editorReadyListener"); //$NON-NLS-1$

        if (HTMLEditor.isAvailable() == false) {
            throw new IllegalStateException("SWT Browser not available or functional on this platform"); //$NON-NLS-1$
        }

        this.editorReadyListener = editorReadyListener;

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();

                hideToolTip();
            }
        });

        /*
         * Use different border styles depending on platform.
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.WINDOWS)
            || WindowSystem.isCurrentWindowSystem(WindowSystem.GTK)) {
            // Matches standard Eclipse look.
            coolbarStyle = SWT.FLAT;

            // On Windows only flat toolbars can be keyboard-traversed. GTK
            // looks better this way.
            toolbarStyle = SWT.FLAT;

            // Single thin line border on browser.
            fullFeaturedBrowserStyle = SWT.BORDER;
        } else {
            // TODO customize for other platforms.

            coolbarStyle = SWT.FLAT;
            toolbarStyle = SWT.FLAT;
            fullFeaturedBrowserStyle = SWT.BORDER;
        }

        /*
         * Use a grid layout, one column wide. Coolbar goes at the top, then
         * browser. Set spacing and margins to 0 for a tight fit.
         */
        final GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        setLayout(layout);

        /*
         * Use a CoolBar at the top level because ToolBar on GTK cannot wrap
         * buttons, and we need wrapping or else the editor may be unusable in
         * small places (work item forms). A CoolBar will wrap its children
         * (ToolBars) automatically if they specify a minimum size.
         */

        coolBar = new CoolBar(this, coolbarStyle);
        coolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        coolBar.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                /*
                 * Coolbars can resize themselves (users drag strips to new
                 * vertical places), so we must force a relayout of this control
                 * so the browser area shrinks/grows in response to the new
                 * size.
                 */
                layout();
            }
        });

        createCoolItem(coolBar, createFontToolBar(coolBar));
        createCoolItem(coolBar, createFormatToolBar(coolBar));
        createCoolItem(coolBar, createColorToolBar(coolBar));
        createCoolItem(coolBar, createAlignmentToolBar(coolBar));
        createCoolItem(coolBar, createListToolBar(coolBar));
        createCoolItem(coolBar, createIndentToolBar(coolBar));
        createCoolItem(coolBar, createLinkToolBar(coolBar));

        // Use default Browser style but style the composite for borders, etc.
        browser = new FullFeaturedBrowser(this, fullFeaturedBrowserStyle, getBrowserStyle());
        browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        browser.setJavascriptEnabled(true);

        /*
         * Handle Control/Command-Click to open a link under the mouse.
         */
        browser.getBrowser().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(final MouseEvent e) {
                if (e.button == 1 && (e.stateMask & OPEN_LINK_MODIFIER_KEY) != 0) {
                    openLinkUnderMouseCursor();
                }
            }
        });

        /*
         * These key combinations are handled specially here, the rest are left
         * to the browser control to handle:
         *
         * - Control/Command-Enter to open a link under the selection
         */
        browser.getBrowser().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.keyCode == SWT.CR && (e.stateMask & OPEN_LINK_MODIFIER_KEY) != 0) {
                    openLinkUnderSelection();
                }
            }
        });

        // Loads the Javascript application.
        initializeBrowser();

        // Create the tooltip and timer to show link click help
        createLinkHelpToolTip();
    }

    /**
     * Override to compute the size with each CoolBar item on its own row. We
     * don't want the control to be sized based on the width of the entire
     * CoolBar since we expect it to collapse if needed. There are layout
     * scenarios that that will cause the HTMLEditor to layout for the full
     * width of a CoolBar and other scenarios where it won't, so we explicitly
     * collapse the CoolBar to ensure the CoolBar is not the determining factor
     * in the overall HTMLEditor control width (see pioneer bug 4349)
     */
    @Override
    public Point computeSize(final int wHint, final int hHint, final boolean flag) {
        if (wHint == -1) {
            final int[] currentIndicies = coolBar.getWrapIndices();
            final int[] allIndicies = new int[coolBar.getItemCount()];
            for (int i = 0; i < allIndicies.length; i++) {
                allIndicies[i] = i;
            }

            coolBar.setWrapIndices(allIndicies);
            final Point p = super.computeSize(wHint, hHint, flag);
            coolBar.setWrapIndices(currentIndicies);
            return p;
        } else {
            return super.computeSize(wHint, hHint, flag);
        }
    }

    private void createLinkHelpToolTip() {
        toolTipShell = new Shell(getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
        toolTipShell.setBackground(getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        final FillLayout layout = new FillLayout();
        layout.marginWidth = 4;
        layout.marginHeight = 4;
        toolTipShell.setLayout(layout);

        final Label label = new Label(toolTipShell, SWT.NONE);
        label.setForeground(getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        label.setBackground(getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            label.setText(Messages.getString("HTMLEditor.HoldDownCommandAndClickLinkToOpen")); //$NON-NLS-1$
        } else {
            label.setText(Messages.getString("HTMLEditor.HoldDownControlAndClickLinkToOpen")); //$NON-NLS-1$
        }

        label.pack();
        toolTipShell.pack();
    }

    private ToolBar createLinkToolBar(final CoolBar parent) {
        final ToolBar toolBar = new ToolBar(parent, toolbarStyle);

        linkButtonItem = new ToolItem(toolBar, SWT.PUSH);
        linkButtonItem.setToolTipText(Messages.getString("HTMLEditor.ConvertToHyperlinkToolTip")); //$NON-NLS-1$
        linkButtonItem.setImage(imageHelper.getImage("/images/htmleditor/link.gif")); //$NON-NLS-1$
        linkButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                /*
                 * IE and Mozilla do not give us the existing link on the
                 * selecte item (if there is one) through any
                 * queryCommandValue() invocation. To retrieve the current link,
                 * call a special method on the HTMLEditor.
                 */
                final Object existingLinkObject = evaluate("return editor.getLinkUnderSelection() || '';"); //$NON-NLS-1$
                final String initialValue =
                    ((existingLinkObject instanceof String) && existingLinkObject.toString().length() > 0)
                        ? (String) existingLinkObject : "http://"; //$NON-NLS-1$

                final StringInputDialog dialog =
                    new StringInputDialog(
                        getShell(),
                        Messages.getString("HTMLEditor.HyperlinkInputDialogPrompt"), //$NON-NLS-1$
                        initialValue,
                        Messages.getString("HTMLEditor.HyperlinkInputDialogTitle"), //$NON-NLS-1$
                        "HTMLEditor.toolBar.linkButtonItem"); //$NON-NLS-1$

                /*
                 * Allow an empty string, to unset the link.
                 */
                dialog.setRequired(false);

                if (dialog.open() == IDialogConstants.OK_ID) {
                    if (dialog.getInput() != null && dialog.getInput().length() > 0) {
                        /*
                         * Disallow Javascript links. These would be removed
                         * during the round-trip through save anyway (see
                         * HtmlFilter).
                         */

                        if (dialog.getInput().startsWith("javascript:") == false) //$NON-NLS-1$
                        {
                            doEditorCommand("CreateLink", false, dialog.getInput()); //$NON-NLS-1$
                        }
                    } else {
                        doEditorCommand("Unlink", false, null); //$NON-NLS-1$
                    }
                }
            }
        });

        return toolBar;

    }

    private ToolBar createIndentToolBar(final CoolBar parent) {
        final ToolBar toolBar = new ToolBar(parent, toolbarStyle);

        indentButtonItem = new ToolItem(toolBar, SWT.PUSH);
        indentButtonItem.setToolTipText(Messages.getString("HTMLEditor.IncreaseIndentToolTip")); //$NON-NLS-1$
        indentButtonItem.setImage(imageHelper.getImage("/images/htmleditor/indent.gif")); //$NON-NLS-1$
        indentButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doEditorCommand("Indent", false, null); //$NON-NLS-1$
            }
        });

        outdentButtonItem = new ToolItem(toolBar, SWT.PUSH);
        outdentButtonItem.setToolTipText(Messages.getString("HTMLEditor.DecreaseIndentToolTip")); //$NON-NLS-1$
        outdentButtonItem.setImage(imageHelper.getImage("/images/htmleditor/outdent.gif")); //$NON-NLS-1$
        outdentButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doEditorCommand("Outdent", false, null); //$NON-NLS-1$
            }
        });

        return toolBar;
    }

    private ToolBar createListToolBar(final CoolBar parent) {
        final ToolBar toolBar = new ToolBar(parent, toolbarStyle);

        unorderedListButtonItem = new ToolItem(toolBar, SWT.PUSH);
        unorderedListButtonItem.setToolTipText(Messages.getString("HTMLEditor.BulletsToolTip")); //$NON-NLS-1$
        unorderedListButtonItem.setImage(imageHelper.getImage("/images/htmleditor/unordered_list.gif")); //$NON-NLS-1$
        unorderedListButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doEditorCommand("InsertUnorderedList", false, null); //$NON-NLS-1$
            }
        });

        orderedListButtonItem = new ToolItem(toolBar, SWT.PUSH);
        orderedListButtonItem.setToolTipText(Messages.getString("HTMLEditor.NumberingToolTip")); //$NON-NLS-1$
        orderedListButtonItem.setImage(imageHelper.getImage("/images/htmleditor/ordered_list.gif")); //$NON-NLS-1$
        orderedListButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doEditorCommand("InsertOrderedList", false, null); //$NON-NLS-1$
            }
        });

        return toolBar;
    }

    private ToolBar createAlignmentToolBar(final CoolBar parent) {
        final ToolBar toolBar = new ToolBar(parent, toolbarStyle);

        /*
         * Alignment buttons go in a drop-down menu.
         */

        /*
         * DropdownToolItemSelectionListener handles much of the drop-down menu
         * work for us. Just add menu items to it.
         */
        final ToolItem alignmentButtonItem = new ToolItem(toolBar, SWT.DROP_DOWN);
        final DropdownToolItemSelectionListener alignmentToolItemSelectionListener =
            new DropdownToolItemSelectionListener(alignmentButtonItem);

        leftMenuItem = alignmentToolItemSelectionListener.addMenuItem(
            Messages.getString("HTMLEditor.AlignLeftToolTip"), //$NON-NLS-1$
            imageHelper.getImage("/images/htmleditor/align_left.gif"), //$NON-NLS-1$
            Messages.getString("HTMLEditor.AlignLeftToolTip"), //$NON-NLS-1$
            SWT.NONE,
            new MenuItemSelectedHandler() {
                @Override
                public void onMenuItemSelected(final MenuItem menuItem) {
                    doEditorCommand("JustifyLeft", false, null); //$NON-NLS-1$
                    updateToolBar();
                }
            });

        centerMenuItem = alignmentToolItemSelectionListener.addMenuItem(
            Messages.getString("HTMLEditor.CenterToolTip"), //$NON-NLS-1$
            imageHelper.getImage("/images/htmleditor/align_center.gif"), //$NON-NLS-1$
            Messages.getString("HTMLEditor.CenterToolTip"), //$NON-NLS-1$
            SWT.NONE,
            new MenuItemSelectedHandler() {
                @Override
                public void onMenuItemSelected(final MenuItem menuItem) {
                    doEditorCommand("JustifyCenter", false, null); //$NON-NLS-1$
                    updateToolBar();
                }
            });

        rightMenuItem =
            alignmentToolItemSelectionListener.addMenuItem(
                Messages.getString("HTMLEditor.AlignRightToolTip"), //$NON-NLS-1$
                imageHelper.getImage("/images/htmleditor/align_right.gif"), //$NON-NLS-1$
                Messages.getString("HTMLEditor.AlignRightToolTip"), //$NON-NLS-1$
                SWT.NONE,
                new MenuItemSelectedHandler() {
                    @Override
                    public void onMenuItemSelected(final MenuItem menuItem) {
                        doEditorCommand("JustifyRight", false, null); //$NON-NLS-1$
                        updateToolBar();
                    }
                });

        justifyMenuItem =
            alignmentToolItemSelectionListener.addMenuItem(
                Messages.getString("HTMLEditor.JustifyToolTip"), //$NON-NLS-1$
                imageHelper.getImage("/images/htmleditor/align_full.gif"), //$NON-NLS-1$
                Messages.getString("HTMLEditor.JustifyToolTip"), //$NON-NLS-1$
                SWT.NONE,
                new MenuItemSelectedHandler() {
                    @Override
                    public void onMenuItemSelected(final MenuItem menuItem) {
                        doEditorCommand("JustifyFull", false, null); //$NON-NLS-1$
                        updateToolBar();
                    }
                });

        alignmentToolItemSelectionListener.setDefaultToolItem(leftMenuItem);
        alignmentButtonItem.addSelectionListener(alignmentToolItemSelectionListener);

        return toolBar;
    }

    private ToolBar createFontToolBar(final CoolBar parent) {
        final ToolBar toolBar = new ToolBar(parent, toolbarStyle);

        fontNameCombo = new Combo(toolBar, SWT.NONE);
        fontNameCombo.setItems(getSystemFontNames(getShell()));
        fontNameCombo.setToolTipText(Messages.getString("HTMLEditor.FontNameToolTip")); //$NON-NLS-1$
        fontNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                applyFontName(fontNameCombo.getText());

                /*
                 * Throw the focus back into the browser so the user can type
                 * with their newly selected font.
                 */
                browser.setFocus();
            }
        });

        final ToolItem fontNameComboItem = new ToolItem(toolBar, SWT.SEPARATOR);
        fontNameComboItem.setWidth(140);
        fontNameComboItem.setControl(fontNameCombo);

        fontSizeCombo = new Combo(toolBar, SWT.NONE);
        fontSizeCombo.setToolTipText(Messages.getString("HTMLEditor.FontSizeToolTip")); //$NON-NLS-1$
        fontSizeCombo.setItems(new String[] {
            "1", //$NON-NLS-1$
            "2", //$NON-NLS-1$
            "3", //$NON-NLS-1$
            "4", //$NON-NLS-1$
            "5", //$NON-NLS-1$
            "6", //$NON-NLS-1$
            "7" //$NON-NLS-1$
        });
        fontSizeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                applyFontSize(fontSizeCombo.getText());

                /*
                 * Throw the focus back into the browser so the user can type
                 * with their newly selected size.
                 */
                browser.setFocus();
            }
        });

        final ToolItem fontSizeComboItem = new ToolItem(toolBar, SWT.SEPARATOR);
        fontSizeComboItem.setWidth(80);
        fontSizeComboItem.setControl(fontSizeCombo);

        return toolBar;
    }

    private ToolBar createFormatToolBar(final CoolBar parent) {
        final ToolBar toolBar = new ToolBar(parent, toolbarStyle);

        boldButtonItem = new ToolItem(toolBar, SWT.CHECK);
        boldButtonItem.setToolTipText(Messages.getString("HTMLEditor.BoldToolTip")); //$NON-NLS-1$
        boldButtonItem.setImage(imageHelper.getImage("/images/htmleditor/bold.gif")); //$NON-NLS-1$
        boldButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doEditorCommand("Bold", false, null); //$NON-NLS-1$
            }
        });

        italicButtonItem = new ToolItem(toolBar, SWT.CHECK);
        italicButtonItem.setToolTipText(Messages.getString("HTMLEditor.ItalicToolTip")); //$NON-NLS-1$
        italicButtonItem.setImage(imageHelper.getImage("/images/htmleditor/italic.gif")); //$NON-NLS-1$
        italicButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doEditorCommand("Italic", false, null); //$NON-NLS-1$
            }
        });

        underlineButtonItem = new ToolItem(toolBar, SWT.CHECK);
        underlineButtonItem.setToolTipText(Messages.getString("HTMLEditor.UnderlineToolTip")); //$NON-NLS-1$
        underlineButtonItem.setImage(imageHelper.getImage("/images/htmleditor/underline.gif")); //$NON-NLS-1$
        underlineButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                doEditorCommand("Underline", false, null); //$NON-NLS-1$
            }
        });

        return toolBar;
    }

    private ToolBar createColorToolBar(final CoolBar parent) {
        final ToolBar toolBar = new ToolBar(parent, toolbarStyle);

        final ToolItem foregroundColorButtonItem = new ToolItem(toolBar, SWT.PUSH);
        foregroundColorButtonItem.setToolTipText(Messages.getString("HTMLEditor.ForegroundColorToolTip")); //$NON-NLS-1$
        foregroundColorButtonItem.setImage(imageHelper.getImage("/images/htmleditor/foreground_color.gif")); //$NON-NLS-1$
        foregroundColorButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final String newColor =
                    pickColor(browser.getShell(), convertHTMLColorObject(queryCommandValue("ForeColor"))); //$NON-NLS-1$

                if (newColor != null) {
                    doEditorCommand("ForeColor", false, newColor); //$NON-NLS-1$
                }
            }
        });

        final ToolItem backgroundColorButtonItem = new ToolItem(toolBar, SWT.PUSH);
        backgroundColorButtonItem.setToolTipText(Messages.getString("HTMLEditor.BackgroundColorToolTip")); //$NON-NLS-1$
        backgroundColorButtonItem.setImage(imageHelper.getImage("/images/htmleditor/background_color.gif")); //$NON-NLS-1$
        backgroundColorButtonItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final String newColor =
                    pickColor(browser.getShell(), convertHTMLColorObject(queryCommandValue("BackColor"))); //$NON-NLS-1$

                if (newColor != null) {
                    doEditorCommand("BackColor", false, newColor); //$NON-NLS-1$
                }
            }
        });

        return toolBar;
    }

    private void createCoolItem(final CoolBar coolBar, final ToolBar toolBar) {
        Check.notNull(coolBar, "coolBar"); //$NON-NLS-1$
        Check.notNull(toolBar, "toolBar"); //$NON-NLS-1$

        // Compute the size of the toolbar
        toolBar.pack();
        final Point toolBarSize = toolBar.getSize();

        // Create a CoolItem to hold the toolbar
        final CoolItem coolItem = new CoolItem(coolBar, SWT.NONE);
        coolItem.setControl(toolBar);

        // Set the preferred size to what was computed from the toolbar
        final Point coolItemSize = coolItem.computeSize(toolBarSize.x, toolBarSize.y);

        /*
         * SWT Quirk (Bug?)
         *
         * The cool item should have its PREFERRED size set to the result of its
         * OWN computeSize() calculation, but its MINIMUM size should be set to
         * its "child" TOOL BAR's computed size. I think it should rightly use
         * the same size (its OWN computed size) for minimum size, but this
         * leaves way too much empty space in the right side of the toolbar.
         */
        coolItem.setPreferredSize(coolItemSize);
        coolItem.setMinimumSize(toolBarSize);
    }

    /**
     * <p>
     * Initializes the Javascript editor program in the {@link Browser}. Call
     * this method just once during construction.
     * </p>
     * <p>
     * Main initialization steps:
     * <ol>
     * <li>Loading the HTML editor document resource (this is the Javascript
     * editor program)</li>
     * <li>Setting the text in the browser control (the HTML editor document
     * just loaded)</li>
     * <li>Attaching a {@link LocationListener} to reject navigation to other
     * URLs</li>
     * <li>Attaching browser function callbacks so Javascript can call back into
     * Java</li>
     * </ol>
     * </p>
     */
    protected void initializeBrowser() {
        final InputStream stream = this.getClass().getResourceAsStream(HTMLEditor.HTML_EDITOR_RESOURCE);

        if (stream == null) {
            throw new MissingResourceException(
                MessageFormat.format("Could not load HTML editor resource {0}", HTMLEditor.HTML_EDITOR_RESOURCE), //$NON-NLS-1$
                HTMLEditor.HTML_EDITOR_RESOURCE,
                ""); //$NON-NLS-1$
        }

        try {
            try {
                final Reader reader = new InputStreamReader(stream, HTMLEditor.HTML_EDITOR_RESOURCE_ENCODING);
                try {
                    final StringBuilder text = new StringBuilder();

                    final char[] buffer = new char[1024];
                    int read;
                    while ((read = reader.read(buffer, 0, buffer.length)) != -1) {
                        text.append(buffer, 0, read);
                    }

                    browser.setText(text.toString());

                    /*
                     * Attach Javascript functions. These classes will only load
                     * with Eclipse/SWT 3.5 or later, so don't store field
                     * references to them or insert "import" statements for them
                     * to ensure the HTMLEditor class can be loaded with older
                     * Eclipse/SWT versions.
                     */
                    new com.microsoft.tfs.client.common.ui.controls.generic.html.EditorReadyFunction(
                        browser.getBrowser(),
                        "HTMLEditorLoadComplete", //$NON-NLS-1$
                        this);

                    new com.microsoft.tfs.client.common.ui.controls.generic.html.ModifiedFunction(
                        browser.getBrowser(),
                        "HTMLEditorDocumentBodyInnerHTMLModified", //$NON-NLS-1$
                        this);

                    new com.microsoft.tfs.client.common.ui.controls.generic.html.SelectionChangedFunction(
                        browser.getBrowser(),
                        "HTMLEditorSelectionChanged", //$NON-NLS-1$
                        this);

                    new com.microsoft.tfs.client.common.ui.controls.generic.html.MouseLinkEnterFunction(
                        browser.getBrowser(),
                        "HTMLEditorMouseLinkEnter", //$NON-NLS-1$
                        this);

                    new com.microsoft.tfs.client.common.ui.controls.generic.html.MouseLinkExitFunction(
                        browser.getBrowser(),
                        "HTMLEditorMouseLinkExit", //$NON-NLS-1$
                        this);
                } catch (final IOException e) {
                    HTMLEditor.log.error("Error reading from stream", e); //$NON-NLS-1$
                    browser.setText(MessageFormat.format(
                        "<html><body>Internal error: {0}</body></html>", //$NON-NLS-1$
                        e.getLocalizedMessage()));
                } finally {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        HTMLEditor.log.error("Error closing reader", e); //$NON-NLS-1$
                    }
                }
            } catch (final UnsupportedEncodingException e) {
                HTMLEditor.log.error(MessageFormat.format(
                    "Couldn''t create InputStreamReader with encoding {0}", //$NON-NLS-1$
                    HTMLEditor.HTML_EDITOR_RESOURCE_ENCODING), e);
                browser.setText(MessageFormat.format(
                    "<html><body>Internal error: {0}</body></html>", //$NON-NLS-1$
                    e.getLocalizedMessage()));
            }
        } finally {
            try {
                stream.close();
            } catch (final IOException e) {
                HTMLEditor.log.error("Error closing resource stream", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Throws {@link IllegalStateException} if the editor is not ready yet (has
     * not completed Javascript initialization).
     */
    private void ensureEditorLoadCompleted() {
        if (editorReady == false) {
            throw new IllegalStateException(MessageFormat.format("{0} not ready", HTMLEditor.class.getName())); //$NON-NLS-1$
        }
    }

    /**
     * Executes the given script in {@link #browser}, logging if the execution
     * failed.
     *
     * @param script
     *        the script to execute (must not be <code>null</code>)
     * @return <code>true</code> if the script succeeded, <code>false</code> if
     *         it failed
     * @see Browser#execute(String)
     */
    private boolean execute(final String script) {
        Check.notNull(script, "script"); //$NON-NLS-1$

        log.debug(script);
        if (browser.execute(script) == false) {
            HTMLEditor.log.error(MessageFormat.format("Error executing script ''{0}''", script)); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    /**
     * Evaluates the given script in {@link #browser}, logging and rethrowing
     * any errors that occur.
     *
     * @param script
     *        the script to evaluate (must not be <code>null</code>)
     * @return the script's return value
     * @see Browser#evaluate(String)
     */
    private Object evaluate(final String script) {
        Check.notNull(script, "script"); //$NON-NLS-1$

        try {
            log.debug(script);
            return browser.evaluate(script);
        } catch (final SWTException e) {
            HTMLEditor.log.error(MessageFormat.format("Error evaluating script ''{0}'': {1}", script, e.getMessage())); //$NON-NLS-1$
            throw e;
        }
    }

    /**
     * Invoked by the Javascript editor when it has completed its load.
     */
    public void onEditorReady() {
        log.debug("onEditorReady"); //$NON-NLS-1$

        editorReady = true;

        applyFontSize(DEFAULT_FONT_SIZE);

        /*
         * Call once because we don't detect cursor movement on first click in
         * Mozilla.
         */
        updateToolBar();

        /*
         * Prevent the browser from opening its own links. Safari/WebKit wants
         * to do this aggressively when the user clicks on links, and doesn't
         * give us the chance to obey the user's browser preferences (internal
         * vs. external app).
         */
        browser.getBrowser().addLocationListener(new LocationAdapter() {
            @Override
            public void changing(final LocationEvent event) {
                log.debug("Blocking location change event"); //$NON-NLS-1$

                event.doit = false;
            }
        });

        /*
         * Prevent new browser windows from opening. WindowEvent's design is
         * very limiting. In order to handle the event one must give it a
         * Browser to open in, which doesn't work with our BrowserFacade class.
         * We can cancel the event by not assigning anything to the browser
         * field and setting required to true (see WidowEvent).
         */
        browser.getBrowser().addOpenWindowListener(new OpenWindowListener() {
            @Override
            public void open(final WindowEvent event) {
                log.debug("Blocking open window event"); //$NON-NLS-1$

                event.required = true;
                event.browser = null;
            }
        });

        editorReadyListener.editorReady();
    }

    /**
     * Invoked by the Javascript editor when the editable document content
     * changes.
     */
    protected void onModified() {
        log.debug("onModified"); //$NON-NLS-1$

        final Event e = new Event();
        e.widget = this;

        final ModifyEvent me = new ModifyEvent(e);

        ((ModifyListener) modifyListeners.getListener()).modifyText(me);
    }

    /**
     * Invoked by the Javascript editor when the selection changes (including
     * cursor position)
     */
    public void onSelectionChanged() {
        log.debug("onSelectionChanged"); //$NON-NLS-1$

        updateToolBar();
    }

    /**
     * Invoked by the Javascript editor when the mouse enters the area (hovers)
     * over a link.
     */
    public void onMouseLinkEnter() {
        log.debug("onMouseLinkEnter"); //$NON-NLS-1$

        scheduleToolTip();
    }

    /**
     * Invoked by the Javascript editor when the mouse exits the area over a
     * link.
     */
    public void onMouseLinkExit() {
        log.debug("onMouseLinkExit"); //$NON-NLS-1$

        hideToolTip();
    }

    private void scheduleToolTip() {
        // Hide any existing tool tip and cancel its timer
        hideToolTip();

        log.trace(MessageFormat.format("Scheduling tool tip to show in {0} ms", TOOL_TIP_SHOW_DELAY_MILLISECONDS)); //$NON-NLS-1$

        toolTipShellTimer = new Timer();
        toolTipShellTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        showToolTip();
                    }
                });
            }
        }, TOOL_TIP_SHOW_DELAY_MILLISECONDS);

    }

    private void showToolTip() {
        // Calculate size and position
        final Point size = toolTipShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final Point position = new Point(getDisplay().getCursorLocation().x, getDisplay().getCursorLocation().y + 20);

        // Ensure it will be on the screen
        if (position.x + size.x > getDisplay().getBounds().width) {
            position.x = getDisplay().getBounds().width - size.x;
        }
        if (position.y + size.y > getDisplay().getBounds().height) {
            position.y = getDisplay().getBounds().height - size.y;
        }

        toolTipShell.setBounds(position.x, position.y, size.x, size.y);

        log.trace("Showing tool tip"); //$NON-NLS-1$
        toolTipShell.setVisible(true);

        // Schedule a timer to hide the tooltip
        toolTipShellTimer = new Timer();
        toolTipShellTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        log.trace("Tool tip timer fired"); //$NON-NLS-1$
                        hideToolTip();
                    }
                });
            }
        }, TableTooltipLabelManager.getPlatformDefaultTooltipTimeout());

    }

    private void hideToolTip() {
        if (toolTipShellTimer != null) {
            log.trace("Clearing tool tip timer"); //$NON-NLS-1$
            toolTipShellTimer.cancel();
            toolTipShellTimer = null;
        }

        if (toolTipShell.isDisposed() == false) {
            log.trace("Hiding tool tip window"); //$NON-NLS-1$
            toolTipShell.setVisible(false);
        }
    }

    /**
     * Updates the tool bar enablement and push states to match the current
     * state of the editor (cursor position in text, etc.).
     */
    private void updateToolBar() {
        final String currentFontName = getCurrentFontName();
        if (currentFontName != null) {
            fontNameCombo.setText(currentFontName);
        }
        fontNameCombo.setEnabled(queryCommandEnabled("FontName")); //$NON-NLS-1$

        final String currentFontSize = getCurrentFontSize();
        if (currentFontSize != null) {
            fontSizeCombo.setText(currentFontSize);
        }
        fontSizeCombo.setEnabled(queryCommandEnabled("FontSize")); //$NON-NLS-1$

        boldButtonItem.setEnabled(queryCommandEnabled("Bold")); //$NON-NLS-1$
        boldButtonItem.setSelection(queryCommandState("Bold")); //$NON-NLS-1$

        italicButtonItem.setEnabled(queryCommandEnabled("Italic")); //$NON-NLS-1$
        italicButtonItem.setSelection(queryCommandState("Italic")); //$NON-NLS-1$

        underlineButtonItem.setEnabled(queryCommandEnabled("Underline")); //$NON-NLS-1$
        underlineButtonItem.setSelection(queryCommandState("Underline")); //$NON-NLS-1$

        leftMenuItem.setEnabled(queryCommandEnabled("JustifyLeft")); //$NON-NLS-1$
        leftMenuItem.setSelection(queryCommandState("JustifyLeft")); //$NON-NLS-1$

        centerMenuItem.setEnabled(queryCommandEnabled("JustifyCenter")); //$NON-NLS-1$
        centerMenuItem.setSelection(queryCommandState("JustifyCenter")); //$NON-NLS-1$

        rightMenuItem.setEnabled(queryCommandEnabled("JustifyRight")); //$NON-NLS-1$
        rightMenuItem.setSelection(queryCommandState("JustifyRight")); //$NON-NLS-1$

        justifyMenuItem.setEnabled(queryCommandEnabled("JustifyFull")); //$NON-NLS-1$
        justifyMenuItem.setSelection(queryCommandState("JustifyFull")); //$NON-NLS-1$

        /*
         * Do not update the alignment menu with the current selection; the menu
         * should keep the user's previously selected item available for format
         * application.
         */

        unorderedListButtonItem.setEnabled(queryCommandEnabled("InsertUnorderedList")); //$NON-NLS-1$

        orderedListButtonItem.setEnabled(queryCommandEnabled("InsertOrderedList")); //$NON-NLS-1$

        linkButtonItem.setEnabled(queryCommandEnabled("CreateLink")); //$NON-NLS-1$
    }

    private String getCurrentFontName() {
        final Object fontName = queryCommandValue("FontName"); //$NON-NLS-1$

        if (fontName instanceof String) {
            final String fontNameString = ((String) fontName);

            /*
             * Mozilla gives us an empty string when it's the default font name
             * for the document. Use the Mozilla-specific getComputedStyle()
             * method instead.
             *
             * IE gives us an empty string when it's the default font, but it
             * doesn't support getComputedStyle(), so skip it.
             */
            if (fontNameString.length() == 0) {
                if (browser.getBrowserType().equals("ie")) //$NON-NLS-1$
                {
                    return null;
                }

                final Object computedPropertyFontFamily = evaluate(
                    "return editor.richEditor.getComputedStyle(editor.richEditor.document.body, null).getPropertyValue('font-family');"); //$NON-NLS-1$
                if (computedPropertyFontFamily instanceof String) {
                    return (String) computedPropertyFontFamily;
                }
            }

            return fontNameString;
        }

        return null;
    }

    private String getCurrentFontSize() {
        final Object fontSize = queryCommandValue("FontSize"); //$NON-NLS-1$
        if (fontSize != null) {
            String fontSizeString = ""; //$NON-NLS-1$

            if (fontSize instanceof Double) {
                // IE gives us a double, but integers look nicer to the user.
                fontSizeString = Long.toString(Math.round((Double) fontSize));
            } else if (fontSize instanceof String) {
                fontSizeString = ((String) fontSize);

                /*
                 * Mozilla gives us an empty string when it's the default size
                 * for the document. Use the Mozilla-specific getComputedStyle()
                 * method instead.
                 *
                 * IE gives us an empty string when it's the default size, but
                 * it doesn't support getComputedStyle(), so skip it.
                 */
                if (fontSizeString.length() == 0) {
                    if (browser.getBrowserType().equals("ie") == false) //$NON-NLS-1$
                    {
                        final Object computedPropertyFontSize = evaluate(
                            "return editor.richEditor.getComputedStyle(editor.richEditor.document.body, null).getPropertyValue('font-size');"); //$NON-NLS-1$
                        if (computedPropertyFontSize instanceof String) {
                            fontSizeString = (String) computedPropertyFontSize;
                        }
                    }
                }
            }

            return fontSizeString;
        }

        return null;
    }

    /**
     * Gets a list of available system fonts.
     *
     * @param shell
     *        the shell from which to list fonts (must not be <code>null</code>)
     * @return the list of font names (never <code>null</code>)
     */
    public String[] getSystemFontNames(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        final Set<String> names = new HashSet<String>();

        /*
         * Ignore font names which start with an @ sign. These show up on
         * Windows and I don't know what the symbol means, but they're all
         * duplicates (?) of other fonts so it seems safe to ignore them.
         */

        // Once for all the scalable fonts
        FontData[] fontDataArray = shell.getDisplay().getFontList(null, true);
        for (int i = 0; i < fontDataArray.length; i++) {
            if (fontDataArray[i].getName().startsWith("@") == false) //$NON-NLS-1$
            {
                names.add(fontDataArray[i].getName());
            }
        }

        // Twice for the non-scalable fonts
        fontDataArray = shell.getDisplay().getFontList(null, false);
        for (int i = 0; i < fontDataArray.length; i++) {
            if (fontDataArray[i].getName().startsWith("@") == false) //$NON-NLS-1$
            {
                names.add(fontDataArray[i].getName());
            }
        }

        final String[] namesArray = names.toArray(new String[names.size()]);
        Arrays.sort(namesArray);

        return namesArray;
    }

    /**
     * Opens a dialog to let the user pick a color.
     *
     * @param shell
     *        the shell on which to open the dialog (must not be
     *        <code>null</code>)
     * @param initialColor
     *        the initial color to set in the dialog, or <code>null</code> to
     *        use the dialog default
     * @return the color the user picked, or <code>null</code> if the dialog was
     *         cancelled
     */
    public String pickColor(final Shell shell, final RGB initialColor) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$

        /*
         * Mac OS X is Extremely Weird Here!
         *
         * On Mac OS X, we open the ColorDialog and it's automatically hooked
         * into the selection in Safari! Changing colors in the dialog while
         * it's open immediately changes the color in the document, before the
         * dialog even closes (we're blocking in this method). We always get
         * null back from dialog.open() when the dialog is closed, so we don't
         * set anything in the document, but that's OK because Safari is
         * magically updating its document for us. Very weird!
         */

        final ColorDialog dialog = new ColorDialog(shell);

        if (initialColor != null) {
            dialog.setRGB(initialColor);
        }

        final RGB rgb = dialog.open();
        if (rgb != null) {
            return String.format("#%1$02x%2$02x%3$02x", rgb.red, rgb.green, rgb.blue); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * Converts the {@link Object} we get back from the browser when we want the
     * foreground or background color into an {@link RGB}. IE gives us a
     * {@link Double} object, Firefox gives us a {@link String} in the format
     * "#000000" or "rgb(0,0,0)".
     *
     * @param o
     *        the object to convert (may be <code>null</code>
     * @return the converted {@link RGB} or <code>null</code> if the object
     *         could not be converted
     */
    private RGB convertHTMLColorObject(final Object o) {
        if (o == null) {
            return null;
        }

        if (o instanceof String) {
            /*
             * Handle both #000000 and rgb(0,0,0) styles. Mozilla alternates
             * between them.
             */
            final String s = (String) o;
            log.debug("convertHTMLColorObject called with string " + s); //$NON-NLS-1$

            if (s.startsWith("#")) //$NON-NLS-1$
            {
                /*
                 * This implementation assumes 24 bits of color info. Will there
                 * ever be more in this string?
                 */
                final int value = Integer.parseInt(s.substring(1), 16);
                return new RGB((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF);
            } else if (s.toLowerCase(Locale.ENGLISH).startsWith("rgb")) //$NON-NLS-1$
            {
                final Matcher matcher = RGB_CSS_COLOR_PATTERN.matcher(s);

                if (matcher.find() && matcher.groupCount() == 3) {
                    return new RGB(
                        Integer.parseInt(matcher.group(1).trim()),
                        Integer.parseInt(matcher.group(2).trim()),
                        Integer.parseInt(matcher.group(3).trim()));
                } else {
                    log.warn(MessageFormat.format("Couldn''t parse color object from string ''{0}''", s)); //$NON-NLS-1$
                    return null;
                }
            }
        } else if (o instanceof Double) {
            final long value = Math.round((Double) o);
            log.debug(MessageFormat.format(
                "convertHTMLColorObject called with Double {0} (rounded to long {1})", //$NON-NLS-1$
                Double.toString(((Double) o)),
                Long.toString(value)));

            /*
             * Always seems to be little-endian (on little-endian Windows
             * platforms). Not sure of a reliable way to detect the format IE
             * will supply us, so just use little endian for now.
             */

            return new RGB((int) value & 0xFF, (int) (value >> 8) & 0xFF, (int) (value >> 16) & 0xFF);
        }

        return null;
    }

    private void applyFontName(final String fontName) {
        doEditorCommand("FontName", false, fontName); //$NON-NLS-1$
    }

    private void applyFontSize(final String fontSize) {
        doEditorCommand("FontSize", false, fontSize); //$NON-NLS-1$
    }

    /**
     * Tests whether the specified editor command can be successfully executed
     * using execCommand given the current state of the document.
     */
    private boolean queryCommandEnabled(final String commandIdentifier) {
        Check.notNull(commandIdentifier, "command"); //$NON-NLS-1$

        final StringBuffer cmd = new StringBuffer();
        cmd.append("return editor.queryCommandEnabled(\""); //$NON-NLS-1$
        cmd.append(commandIdentifier);
        cmd.append("\") || ''"); //$NON-NLS-1$

        final Object ret = browser.evaluate(cmd.toString());

        if (ret instanceof Boolean == false) {
            return false;
        }

        return ((Boolean) ret).booleanValue();
    }

    /**
     * Gets the current state of the specified command.
     */
    private boolean queryCommandState(final String commandIdentifier) {
        Check.notNull(commandIdentifier, "command"); //$NON-NLS-1$

        final StringBuffer cmd = new StringBuffer();
        cmd.append("return editor.queryCommandState(\""); //$NON-NLS-1$
        cmd.append(commandIdentifier);
        cmd.append("\") || ''"); //$NON-NLS-1$

        final Object ret = browser.evaluate(cmd.toString());

        if (ret instanceof Boolean == false) {
            return false;
        }

        return ((Boolean) ret).booleanValue();
    }

    /**
     * Gets the value of the document, range, or selection for the given
     * command.
     */
    private Object queryCommandValue(final String commandIdentifier) {
        Check.notNull(commandIdentifier, "command"); //$NON-NLS-1$

        final StringBuffer cmd = new StringBuffer();
        cmd.append("return editor.queryCommandValue(\""); //$NON-NLS-1$
        cmd.append(commandIdentifier);
        cmd.append("\") || ''"); //$NON-NLS-1$

        return browser.evaluate(cmd.toString());
    }

    private void doEditorCommand(final String command, final boolean showDefaultUserInterface, final String value) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        String commandString = ""; //$NON-NLS-1$
        if (value == null) {
            commandString = MessageFormat.format(
                "editor.execCommand(\"{0}\", {1}, null)", //$NON-NLS-1$
                command,
                Boolean.toString(showDefaultUserInterface));
        } else {
            commandString = MessageFormat.format(
                "editor.execCommand(\"{0}\", {1}, \"{2}\")", //$NON-NLS-1$
                command,
                Boolean.toString(showDefaultUserInterface),
                escapeStringForJavascriptExpression(value));
        }

        // Execute does logging.
        execute(commandString);
    }

    /**
     * Adds the given listener to the list of listeners which will be notified
     * when the editor content changes.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addModifyListener(final ModifyListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        modifyListeners.addListener(listener);
    }

    /**
     * Removes the given listener from the list of listeners who will be
     * notified when the editor content changes.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeModifyListener(final ModifyListener listener) {
        Check.notNull(listener, "listener"); //$NON-NLS-1$

        modifyListeners.removeListener(listener);
    }

    /**
     * Adds a location changed listener to the supporting {@link Browser}.
     *
     * @param listener
     *        the location listener to add (must not be <code>null</code>)
     * @see Browser#addLocationListener(LocationListener)
     */
    public void addLocationListener(final LocationListener listener) {
        browser.addLocationListener(listener);
    }

    /**
     * Removes a location changed listener from the supporting {@link Browser}.
     *
     * @param listener
     *        the location listener to remove (must not be <code>null</code>)
     * @see Browser#removeLocationListener(LocationListener)
     */
    public void removeLocationListener(final LocationListener listener) {
        browser.removeLocationListener(listener);
    }

    /**
     * <p>
     * Gets the innerHTML content from the body element of the HTML editor's
     * inner iframe. Because this content comes from inside a body element, it
     * will not contain &lt;html&gt;, &lt;head&gt;, or &lt;body&gt; tags.
     * </p>
     * <p>
     * <h3>Warning</h3>
     * <p>
     * Only call this method after the editor has completed loading (see
     * {@link HTMLEditor})!
     * </p>
     *
     * @return the HTML content in the body element
     */
    public String getHTML() {
        ensureEditorLoadCompleted();

        return (String) evaluate("return editor.getDocumentBodyInnerHTML() || '';"); //$NON-NLS-1$
    }

    /**
     * <p>
     * Sets the innerHTML content inside the body element of the HTML editor
     * inner iframe. Because this content goes inside a body element, it should
     * not contain &lt;html&gt;, &lt;head&gt;, or &lt;body&gt; tags.
     * </p>
     * <p>
     * <h3>Warning</h3>
     * <p>
     * Only call this method after the editor has completed loading (see
     * {@link HTMLEditor})!
     * </p>
     *
     * @param html
     *        the HTML to set in the body element (must not be <code>null</code>
     *        )
     */
    public void setHTML(final String html) {
        Check.notNull(html, "htmlContent"); //$NON-NLS-1$
        ensureEditorLoadCompleted();

        /*
         * Skip updating the contents if the new contents are the same, because
         * setting the contents on some Browser implementations (Firefox on
         * Linux) resets the caret position to 0. This is a simple work-around
         * and saves update work at the expense of one extra read. Other
         * solutions may involve saving/restoring the caret.
         */
        if (html.equals(getHTML())) {
            return;
        }

        execute(MessageFormat.format(
            "editor.setDocumentBodyInnerHTML(\"{0}\");", //$NON-NLS-1$
            escapeStringForJavascriptExpression(html)));
    }

    /**
     * Escapes the given string so it can be used as a string literal (use
     * double quotes to surround) in a Javascript expression passed to
     * {@link #evaluate(String)}.
     *
     * @param string
     *        the string to escape (must not be <code>null</code>)
     * @return the escaped string (never <code>null</code>)
     */
    private String escapeStringForJavascriptExpression(String string) {
        if (string.length() == 0) {
            return string;
        }

        /*
         * Escape 1 backslash with 2 backslashes (wow, that's a lot of chars to
         * do this: double them once for the Java compiler, double them again to
         * make it through the regular expression engine). This must be done
         * before the newline replacement, so we don't double up the (required
         * single) backslashes for the newline sequences.
         */
        string = string.replaceAll("\\\\", "\\\\\\\\"); //$NON-NLS-1$//$NON-NLS-2$

        /*
         * Escape newlines.
         */
        string = string.replaceAll("\\r\\n", "\\\\r\\\\n"); //$NON-NLS-1$ //$NON-NLS-2$
        string = string.replaceAll("\\n", "\\\\n"); //$NON-NLS-1$ //$NON-NLS-2$
        string = string.replaceAll("\\r", "\\\\r"); //$NON-NLS-1$ //$NON-NLS-2$

        /*
         * Escape all double quotes with backslash and double-quote, so the
         * outer double-quoting of the contents in an expression aren't
         * interrupted.
         */
        string = string.replaceAll("\"", "\\\\\""); //$NON-NLS-1$ //$NON-NLS-2$

        return string;
    }

    /**
     * <p>
     * Configures the editor to allow or prevent changes to the document.
     * </p>
     * <p>
     * <h3>Warning</h3>
     * <p>
     * Only call this method after the editor has completed loading (see
     * {@link HTMLEditor})!
     * </p>
     *
     * @param value
     *        if <code>true</code> the document is editable by the user, if
     *        <code>false</code> the document is not editable
     */
    public void setReadOnly(final boolean value) {
        ensureEditorLoadCompleted();

        if (browser.execute(MessageFormat.format("editor.setReadOnly({0});", Boolean.toString(value))) == false) //$NON-NLS-1$
        {
            HTMLEditor.log.warn(MessageFormat.format("Error setting editor to read-only = {0}", value)); //$NON-NLS-1$
        }
    }

    /**
     * <p>
     * Gets whether the control is read-only.
     * </p>
     * <p>
     * <h3>Warning</h3>
     * <p>
     * Only call this method after the editor has completed loading (see
     * {@link HTMLEditor})!
     * </p>
     *
     * @return <code>true</code> if the editor is read-only, <code>false</code>
     *         if the editor content may be edited
     */
    public boolean getReadOnly() {
        ensureEditorLoadCompleted();

        return ((Boolean) evaluate("return editor.getReadOnly();")).booleanValue(); //$NON-NLS-1$
    }

    /**
     * Handles the user typing control-enter on an existing link in the editor.
     */
    private void openLinkUnderSelection() {
        log.debug("opening link under selection"); //$NON-NLS-1$

        BusyIndicator.showWhile(getDisplay(), new Runnable() {
            @Override
            public void run() {
                // More immediate feedback.
                hideToolTip();

                final Object linkObject = evaluate("return editor.getLinkUnderSelection() || '';"); //$NON-NLS-1$
                if ((linkObject instanceof String) && linkObject.toString().length() > 0) {
                    openLink((String) linkObject);
                }
            }
        });
    }

    /**
     * Handles the user clicking (or typing control-enter on) an existing link
     * in the editor.
     */
    private void openLinkUnderMouseCursor() {
        log.debug("opening link under mouse cursor"); //$NON-NLS-1$

        BusyIndicator.showWhile(getDisplay(), new Runnable() {
            @Override
            public void run() {
                // More immediate feedback.
                hideToolTip();

                final Object linkObject = evaluate("return editor.getLinkUnderMouseCursor() || '';"); //$NON-NLS-1$
                if ((linkObject instanceof String) && linkObject.toString().length() > 0) {
                    openLink((String) linkObject);
                }
            }
        });
    }

    private void openLink(final String urlString) {
        Check.notNull(urlString, "urlString"); //$NON-NLS-1$

        // TODO handle proprietary TFS link types here

        try {
            final URI uri = URIUtils.newURI(urlString);
            BrowserFacade.launchURL(uri, null);
        } catch (final IllegalArgumentException e) {
            log.error("Could not parse " + urlString, e); //$NON-NLS-1$

            MessageBoxHelpers.errorMessageBox(
                getShell(),
                Messages.getString("HTMLEditor.ErrorOpeningLink"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("HTMLEditor.LinkCouldNotBeParsedAsURLFormat"), //$NON-NLS-1$
                    urlString,
                    e.getLocalizedMessage()));
            return;
        }
    }
}
