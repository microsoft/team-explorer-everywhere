// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.URISchemeHelper;
import com.microsoft.tfs.client.common.ui.controls.generic.CompatibleBrowser;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.FontHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Platform;

/**
 * For Eclipse < 3.5, edits a work item HTML field in two tabs: one tab is a
 * plain text editor (user edits HTML tags directly) and the other is a simple
 * web browser preview of the content (uses {@link CompatibleBrowser} for best
 * platform support).
 *
 * @threadsafety unknown
 */
public class LegacyHTMLFieldControl extends BaseHTMLFieldControl {
    private static final Log log = LogFactory.getLog(LegacyHTMLFieldControl.class);

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private CompatibleBrowser browser;
    private Font browserFont;
    private final LocationListener locationListener;

    public LegacyHTMLFieldControl() {
        super();

        locationListener = new LocationAdapter() {
            @Override
            public void changing(final LocationEvent event) {
                if (event.location.startsWith("about:") == false && event.location.startsWith("javascript:") == false) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    try {
                        final URI uri = URIUtils.newURI(event.location);

                        if (!URISchemeHelper.isOnTrustedUriWhiteList(uri)) {
                            URISchemeHelper.showUnsafeSchemeError(uri);
                        } else {
                            BrowserFacade.launchURL(uri, event.location);
                        }
                    } catch (final IllegalArgumentException e1) {
                        final String messageFormat = "unable to convert the string [{0}] into a valid URL"; //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, event.location);
                        log.warn(message);
                    } catch (final Exception e) {
                        final String messageFormat = "error launching url [{0}]"; //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, event.location);
                        log.warn(message, e);
                    }

                    event.doit = false;
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        final Field field = getField();

        initializeFallbackFont(parent.getDisplay());

        if (browserFont != null) {
            parent.addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(final DisposeEvent e) {
                    browserFont.dispose();
                }
            });
        }

        final CTabFolder tabFolder = new CTabFolder(parent, SWT.BOTTOM | SWT.BORDER);

        final CTabItem viewTabItem = new CTabItem(tabFolder, SWT.NONE);
        viewTabItem.setText(Messages.getString("LegacyHtmlFieldControl.ViewTabText")); //$NON-NLS-1$

        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();
        final int browserStyle = preferences.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE);

        browser = new CompatibleBrowser(tabFolder, browserStyle);
        viewTabItem.setControl(browser);

        browser.addLocationListener(locationListener);

        final CTabItem editTabItem = new CTabItem(tabFolder, SWT.NONE);
        editTabItem.setText(Messages.getString("LegacyHtmlFieldControl.EditTabText")); //$NON-NLS-1$

        final Text text = new Text(tabFolder, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.WRAP);
        editTabItem.setControl(text);

        if (browserFont != null) {
            text.setFont(browserFont);
        }

        if (isFormReadonly()) {
            text.setEditable(false);
        }

        final boolean wantsVerticalFill = wantsVerticalFill();
        tabFolder.setLayoutData(new GridData(
            SWT.FILL,
            wantsVerticalFill ? SWT.FILL : SWT.CENTER,
            true,
            wantsVerticalFill,
            columnsToTake,
            1));
        ControlSize.setCharHeightHint(tabFolder, 4);
        ControlSize.setCharWidthHint(tabFolder, 1);

        if (!isFormReadonly()) {
            final FieldUpdateModifyListener fieldUpdateModifyListener = new FieldUpdateModifyListener(field);
            text.addModifyListener(fieldUpdateModifyListener);
            text.setData(FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY, fieldUpdateModifyListener);
        }

        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (tabFolder.getSelection() == viewTabItem) {
                    setFallbackBrowserHTML(text.getText());
                }
            }
        });

        // Select the "Edit" tab if the form is not read-only and the field is
        // not empty. Otherwise, select the "View" tab as the initial view.
        final String fieldText = getFieldDataAsString(field.getName());
        if (!isFormReadonly() && (fieldText == null || fieldText.length() == 0)) {
            tabFolder.setSelection(editTabItem);
        } else {
            tabFolder.setSelection(viewTabItem);
        }

        final FieldChangeListener fieldChangeListener = new FieldChangeListener() {
            @Override
            public void fieldChanged(final FieldChangeEvent event) {
                /*
                 * Ignore the field changed event if the field was change was
                 * caused by this control.
                 */
                if (event.source == text) {
                    return;
                }

                final Display display = text.getDisplay();
                if (display.getThread() == Thread.currentThread()) {
                    fallbackBrowserFieldChangedSafe(event, text);
                    return;
                }

                UIHelpers.runOnUIThread(display, true, new Runnable() {
                    @Override
                    public void run() {
                        fallbackBrowserFieldChangedSafe(event, text);
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

        /*
         * fire a "fake" field change event to the field change listener this
         * sets up the text for this control
         */
        final FieldChangeEvent fieldChangeEvent = new FieldChangeEvent();
        fieldChangeEvent.field = field;
        fieldChangeListener.fieldChanged(fieldChangeEvent);

        getFieldTracker().addField(field);
        getFieldTracker().setFocusReceiver(field, new FieldTracker.FocusReceiver() {
            @Override
            public boolean setFocus() {
                tabFolder.setSelection(editTabItem);
                return text.setFocus();
            }
        });
    }

    private void fallbackBrowserFieldChangedSafe(final FieldChangeEvent event, final Text textControl) {
        final Field field = event.field;

        if (textControl.getData(FieldUpdateModifyListener.MODIFICATION_KEY) != null) {
            if (log.isTraceEnabled()) {
                final String messageFormat =
                    "HtmlFieldControl FieldChangeListener called for field change on [{0}]: skipping since field is being modified by UI"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
                log.trace(message);
            }
            return;
        }

        if (log.isTraceEnabled()) {
            final String messageFormat = "HtmlFieldControl FieldChangeListener called for field change on [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(field.getID()));
            log.trace(message);
        }

        final ModifyListener modifyListener =
            (ModifyListener) textControl.getData(FieldUpdateModifyListener.MODIFY_LISTENER_WIDGET_DATA_KEY);

        if (modifyListener != null) {
            textControl.removeModifyListener(modifyListener);
        }

        try {
            final String initialHtml = getHTMLTextFromField();

            textControl.setText(initialHtml);
            setFallbackBrowserHTML(initialHtml);
        } finally {
            if (modifyListener != null) {
                textControl.addModifyListener(modifyListener);
            }
        }
    }

    private void setFallbackBrowserHTML(final String html) {
        final StringBuffer sb = new StringBuffer();
        sb.append(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">").append( //$NON-NLS-1$
                NEWLINE);
        sb.append("<html>").append(NEWLINE); //$NON-NLS-1$

        sb.append("<head>").append(NEWLINE); //$NON-NLS-1$
        sb.append("<style>").append(NEWLINE); //$NON-NLS-1$

        final String bgColor = "#ffffff"; //$NON-NLS-1$
        final String fgColor = "#000000"; //$NON-NLS-1$

        int fontsize = FontHelper.getHeight(browser.getFont());

        /*
         * JEditorPane renders fonts very tiny on Linux and Solaris so they need
         * scaled, but HP-UX and AIX seems to be fine with the normal point
         * size.
         */
        if (CompatibleBrowser.isNativeBrowserAvailable() == false
            && (Platform.isCurrentPlatform(Platform.LINUX) || Platform.isCurrentPlatform(Platform.SOLARIS))) {
            fontsize *= 1.4;
        }

        sb.append("BODY { margin: 1px 6px 1px 1px; padding: 0; font-size: " //$NON-NLS-1$
            + fontsize
            + "pt; background-color: " //$NON-NLS-1$
            + bgColor
            + "; color: " //$NON-NLS-1$
            + fgColor
            + "; }").append(NEWLINE); //$NON-NLS-1$

        if (CompatibleBrowser.isNativeBrowserAvailable() && WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            sb.append("* { font-size: " + fontsize + "px; }").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
        }

        sb.append("</style>").append(NEWLINE); //$NON-NLS-1$
        sb.append("</head>").append(NEWLINE); //$NON-NLS-1$
        sb.append("<body>").append(NEWLINE); //$NON-NLS-1$
        sb.append(html);
        sb.append("</body></html>"); //$NON-NLS-1$

        browser.removeLocationListener(locationListener);
        browser.setText(sb.toString());
        browser.addLocationListener(locationListener);
    }

    private void initializeFallbackFont(final Display display) {
        final FontData[] tahomaFontData = display.getFontList("Tahoma", true); //$NON-NLS-1$
        if (tahomaFontData != null && tahomaFontData.length > 0) {
            browserFont = new Font(display, "Tahoma", 10, SWT.NORMAL); //$NON-NLS-1$
        }
    }
}
