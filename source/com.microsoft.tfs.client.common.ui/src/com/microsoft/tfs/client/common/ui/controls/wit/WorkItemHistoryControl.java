// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.wit;

import java.net.URI;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.generic.CompatibleBrowser;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.FontHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.WorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.revision.Revision;
import com.microsoft.tfs.core.clients.workitem.revision.RevisionField;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.HTTPUtil;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

public class WorkItemHistoryControl extends BaseControl {
    private static final Log log = LogFactory.getLog(WorkItemHistoryControl.class);

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String UPDATE_WORK_ITEM_LISTENER_KEY = "update-work-item-listener-key"; //$NON-NLS-1$

    private final DateFormat historyDateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
    private final LocationListener locationListener;
    private final WorkItem workItem;
    private Font historyFont;
    private final Text commentText;

    private final CompatibleBrowser browser;

    public WorkItemHistoryControl(final Composite parent, final int style, final WorkItem inputWorkItem) {
        this(parent, style, inputWorkItem, true);
    }

    public WorkItemHistoryControl(
        final Composite parent,
        final int style,
        final WorkItem inputWorkItem,
        final boolean editable) {
        super(parent, style);

        workItem = inputWorkItem;

        /*
         * The WorkItemHistoryControl composite uses a GridLayout with a single
         * column. The first cell holds the Text control for history comment
         * input. The second cell holds the Browser control for displaying
         * previous revisions.
         */
        final GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 20;
        setLayout(layout);

        /*
         * Create the Text control for history comment input.
         */
        commentText = new Text(this, SWT.MULTI | SWT.BORDER);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        commentText.setLayoutData(gd);

        // Mac hack: we need to force a two line display
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            FontHelper.getHeight(commentText.getFont());
        }

        if (!editable) {
            commentText.setEditable(false);
            commentText.setText(Messages.getString("WorkItemHistoryControl.CommentLabelText")); //$NON-NLS-1$
        } else {
            /*
             * Add a focus listener to the text control to provide the "Type
             * your comment here" decoration when the control is empty.
             */
            final Object initialValue = workItem.getFields().getField(CoreFieldReferenceNames.HISTORY).getValue();
            if (initialValue == null || initialValue.toString().length() == 0) {
                HistoryTextControlDecorator.addInitialDecoration(commentText);
            } else {
                commentText.setText(initialValue.toString());
            }
            commentText.addFocusListener(new HistoryTextControlDecorator());

            /*
             * Attempt to set a font on the text control similar to the font
             * used in visual studio.
             */
            setTextControlFont(commentText);
        }

        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();
        final int browserStyle = preferences.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE);

        /*
         * Use a simple browser viewer control, which can fall back to
         * JEditorPane if SWT's Browser doesn't work on the current platform.
         */
        browser = new CompatibleBrowser(this, browserStyle);

        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.verticalAlignment = SWT.FILL;
        browser.setLayoutData(gd);

        locationListener = new LocationAdapter() {
            @Override
            public void changing(final LocationEvent event) {
                if (!event.location.startsWith("about")) //$NON-NLS-1$
                {
                    URI uri;
                    try {
                        uri = URIUtils.newURI(event.location);
                        BrowserFacade.launchURL(uri, event.location);
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

        browser.addLocationListener(locationListener);

        /*
         * Add a ModifyListener to the text control to re-layout this composite
         * when the control's contents have changed.
         */
        commentText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                WorkItemHistoryControl.this.layout();
            }
        });

        /*
         * create a ModifyListener that will set the work item's history field
         * when the history comment text control is modified
         */
        final ModifyListener updateWorkItemListener = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final Text text = (Text) e.widget;
                final String htmlValue = createHTMLFromTextInput(text.getText().trim());
                workItem.getFields().getField(CoreFieldReferenceNames.HISTORY).setValue(htmlValue);
            }
        };

        /*
         * add the work item update modify listener to the text control
         */
        commentText.addModifyListener(updateWorkItemListener);

        /*
         * store a reference to the update listener so we can remove and re-add
         * it as part of the decoration process
         */
        commentText.setData(UPDATE_WORK_ITEM_LISTENER_KEY, updateWorkItemListener);

        updateHTMLControl();

        final WorkItemStateListener workItemStateListener = new WorkItemStateAdapter() {
            @Override
            public void saved(final WorkItem workItem) {
                update(workItem);
            }

            @Override
            public void synchedToLatest(final WorkItem workItem) {
                update(workItem);
            }

            private void update(final WorkItem workItem) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        if (isDisposed()) {
                            return;
                        }

                        updateHTMLControl();
                        if (editable) {
                            HistoryTextControlDecorator.addInitialDecoration(commentText);
                        }
                    }
                });
            }
        };

        workItem.addWorkItemStateListener(workItemStateListener);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItem.removeWorkItemStateListener(workItemStateListener);
            }
        });
    }

    private void setTextControlFont(final Text text) {
        final FontData[] tahomaFontData = getShell().getDisplay().getFontList("Tahoma", true); //$NON-NLS-1$
        if (tahomaFontData != null && tahomaFontData.length > 0) {
            historyFont = new Font(getShell().getDisplay(), "Tahoma", 10, SWT.NORMAL); //$NON-NLS-1$
        }

        if (historyFont != null) {
            text.setFont(historyFont);
        }

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (historyFont != null) {
                    historyFont.dispose();
                }
            }
        });
    }

    private void updateHTMLControl() {
        int fontsize = FontHelper.getHeight(getFont());

        /*
         * These are CSS system colors
         * (http://www.w3.org/TR/CSS21/ui.html#system-colors).
         */
        String bgColor = "Window"; //$NON-NLS-1$
        String fgColor = "WindowText"; //$NON-NLS-1$
        String tableBorderColor = "ButtonShadow"; //$NON-NLS-1$
        String tableBackgroundColor = "ButtonFace"; //$NON-NLS-1$
        String tableForegroundColor = "ButtonText"; //$NON-NLS-1$
        String fieldNameBackgroundColor = "ButtonFace"; //$NON-NLS-1$
        String fieldNameForegroundColor = "ButtonText"; //$NON-NLS-1$

        /*
         * Override the colors for the AWT browser because it doesn't understand
         * CSS system colors (or really CSS at all).
         */
        if (!CompatibleBrowser.isNativeBrowserAvailable()) {
            bgColor = "#ffffff"; //$NON-NLS-1$
            fgColor = "#000000"; //$NON-NLS-1$
            tableBorderColor = "#a0a0a0"; //$NON-NLS-1$
            tableBackgroundColor = "#f0f0f0"; //$NON-NLS-1$
            tableForegroundColor = "#000000"; //$NON-NLS-1$
            fieldNameBackgroundColor = "#f0f0f0"; //$NON-NLS-1$
            fieldNameForegroundColor = "#000000"; //$NON-NLS-1$
        }

        final StringBuffer html = new StringBuffer();
        html.append(
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">").append( //$NON-NLS-1$
                NEWLINE);
        html.append("<html>").append(NEWLINE); //$NON-NLS-1$

        html.append("<head>").append(NEWLINE); //$NON-NLS-1$
        html.append("<style>").append(NEWLINE); //$NON-NLS-1$

        /*
         * JEditorPane renders fonts very tiny on Linux and Solaris so they need
         * scaled, but HP-UX and AIX seems to be fine with the normal point
         * size.
         */
        if (CompatibleBrowser.isNativeBrowserAvailable() == false
            && (Platform.isCurrentPlatform(Platform.LINUX) || Platform.isCurrentPlatform(Platform.SOLARIS))) {
            fontsize *= 1.4;
        }

        html.append(
            "BODY { margin: 1px 6px 1px 1px; padding: 0; font-family: \"Tahoma\", \"Verdana\", sans-serif; font-size: " //$NON-NLS-1$
                + fontsize
                + "pt; background-color: " //$NON-NLS-1$
                + bgColor
                + "; color: " //$NON-NLS-1$
                + fgColor
                + "; }").append(NEWLINE); //$NON-NLS-1$

        if (CompatibleBrowser.isNativeBrowserAvailable() && WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
            html.append("* { font-size: " + fontsize + "px; }").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
        }

        html.append("TABLE.revision { border-collapse: collapse; border-spacing: 0; border: solid 1px " //$NON-NLS-1$
            + tableBorderColor
            + "; }"); //$NON-NLS-1$

        // hack for embedded AWT browser control
        if (!CompatibleBrowser.isNativeBrowserAvailable()) {
            html.append("TABLE.revision { margin: 0 0 10px 0; }"); //$NON-NLS-1$
        } else {
            html.append("TABLE.revision { margin: 1.4em 0 0 0; }").append(NEWLINE); //$NON-NLS-1$
            html.append("TABLE.revision:first-child { margin: 0; }").append(NEWLINE); //$NON-NLS-1$
        }

        html.append("TABLE TR.revisionheader { background-color: " //$NON-NLS-1$
            + tableBackgroundColor
            + "; color: " //$NON-NLS-1$
            + tableForegroundColor
            + "; border: solid 1px " //$NON-NLS-1$
            + tableBorderColor
            + "; border-bottom: none; }").append(NEWLINE); //$NON-NLS-1$

        html.append("TABLE TR.revisionheader TH { padding: 0.5em; text-align: left; font-weight: bold; }").append( //$NON-NLS-1$
            NEWLINE);

        html.append("TABLE TR.description { background-color: " //$NON-NLS-1$
            + tableBackgroundColor
            + "; color: " //$NON-NLS-1$
            + tableForegroundColor
            + "; }").append(NEWLINE); //$NON-NLS-1$
        html.append("TABLE TR.description TD { padding: 0.75em 0.5em 0.5em 0.5em; }").append(NEWLINE); //$NON-NLS-1$

        html.append("TABLE TR TH, TABLE TR TD { padding: 0.1em 0.5em 0.1em 0.5em; vertical-align: top; }").append( //$NON-NLS-1$
            NEWLINE);

        html.append(
            "TABLE TR TH.field, TABLE TR TH.oldvalue, TABLE TR TH.newvalue { padding-top: 0.7em; padding-bottom: 0.3em; border: solid 1px " //$NON-NLS-1$
                + tableBorderColor
                + "; text-align: left; }").append(NEWLINE); //$NON-NLS-1$
        html.append("TABLE TR TD.field, TABLE TR TD.oldvalue, TABLE TR TD.newvalue { border: solid 1px " //$NON-NLS-1$
            + tableBorderColor
            + "; }").append(NEWLINE); //$NON-NLS-1$

        html.append("TABLE TR TH.field, TABLE TR TD.field { background-color: " //$NON-NLS-1$
            + fieldNameBackgroundColor
            + "; color: " //$NON-NLS-1$
            + fieldNameForegroundColor
            + ";}"); //$NON-NLS-1$

        html.append("</style>").append(NEWLINE); //$NON-NLS-1$
        html.append("</head>").append(NEWLINE); //$NON-NLS-1$

        html.append("<body>").append(NEWLINE); //$NON-NLS-1$

        for (int i = workItem.getRevisions().size() - 1; i >= 0; i--) {
            final Revision revision = workItem.getRevisions().get(i);
            final boolean initial = (i == 0);

            final String revisedDateString = historyDateFormat.format(revision.getRevisionDate());

            html.append("<table class=\"revision\" width=\"100%\" border=\"0\">").append(NEWLINE); //$NON-NLS-1$
            html.append("<tr class=\"revisionheader\">").append(NEWLINE); //$NON-NLS-1$
            html.append("<th class=\"dateline\">").append(revisedDateString).append("</th>").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
            html.append("<th class=\"byline\" colspan=\"2\">").append(revision.getTagLine()).append("</th>").append( //$NON-NLS-1$ //$NON-NLS-2$
                NEWLINE);
            html.append("</tr>").append(NEWLINE); //$NON-NLS-1$

            final String history = (String) revision.getField(CoreFieldReferenceNames.HISTORY).getValue();
            if (history != null) {
                html.append("<tr class=\"description\">").append(NEWLINE); //$NON-NLS-1$
                html.append("<td colspan=\"3\">").append(NEWLINE); //$NON-NLS-1$

                /*
                 * AWT browser doesn't seem to understand em units, so hard-code
                 * some pixel spacing.
                 */
                if (!CompatibleBrowser.isNativeBrowserAvailable()) {
                    html.append("<p style=\"margin-top: 6px; margin-bottom: 6px;\">").append(NEWLINE); //$NON-NLS-1$
                }

                html.append(history).append(NEWLINE);

                if (!CompatibleBrowser.isNativeBrowserAvailable()) {
                    html.append("</p>").append(NEWLINE); //$NON-NLS-1$
                }

                html.append("</td>").append(NEWLINE); //$NON-NLS-1$
                html.append("</tr>").append(NEWLINE); //$NON-NLS-1$
            }

            html.append("<tr class=\"header\">").append(NEWLINE); //$NON-NLS-1$
            html.append("<th class=\"field\" width=\"20%\">" //$NON-NLS-1$
                + Messages.getString("WorkItemHistoryControl.Field") //$NON-NLS-1$
                + "</th>").append(NEWLINE); //$NON-NLS-1$
            if (initial) {
                html.append("<th class=\"newvalue\" colspan=\"2\" width=\"80%\">" //$NON-NLS-1$
                    + Messages.getString("WorkItemHistoryControl.InitialValue") //$NON-NLS-1$
                    + "</th>").append(NEWLINE); //$NON-NLS-1$
            } else {
                html.append("<th class=\"oldvalue\" width=\"40%\">" //$NON-NLS-1$
                    + Messages.getString("WorkItemHistoryControl.OldValue") //$NON-NLS-1$
                    + "</th>").append(NEWLINE); //$NON-NLS-1$
                html.append("<th class=\"newvalue\" width=\"40%\">" //$NON-NLS-1$
                    + Messages.getString("WorkItemHistoryControl.NewValue") //$NON-NLS-1$
                    + "</th>").append(NEWLINE); //$NON-NLS-1$
            }
            html.append("</tr>").append(NEWLINE); //$NON-NLS-1$

            for (int j = 0; j < revision.getFields().length; j++) {
                final RevisionField field = revision.getFields()[j];

                if (field.shouldIgnoreForDeltaTable()) {
                    continue;
                }

                final Object oldValue = field.getOriginalValue();
                final Object newValue = field.getValue();

                if (objectsAreEqual(oldValue, newValue)) {
                    continue;
                }

                final String oldValueString = (oldValue == null ? "" : convertValueForDisplay(oldValue)); //$NON-NLS-1$
                final String newValueString = (newValue == null ? "" : convertValueForDisplay(newValue)); //$NON-NLS-1$

                html.append("<tr>").append(NEWLINE); //$NON-NLS-1$
                html.append("<td class=\"field\">" + field.getName() + "</td>").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$

                if (initial) {
                    html.append("<td class=\"newvalue\" colspan=\"2\">" + newValueString + "</td>").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    html.append("<td class=\"oldvalue\">" + oldValueString + "</td>").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
                    html.append("<td class=\"newvalue\">" + newValueString + "</td>").append(NEWLINE); //$NON-NLS-1$ //$NON-NLS-2$
                }
                html.append("</tr>").append(NEWLINE); //$NON-NLS-1$
            }

            html.append("</table>").append(NEWLINE); //$NON-NLS-1$
        }

        html.append("</body>").append(NEWLINE); //$NON-NLS-1$

        html.append("</html>").append(NEWLINE); //$NON-NLS-1$

        browser.removeLocationListener(locationListener);
        browser.setText(html.toString());
        browser.addLocationListener(locationListener);
    }

    private boolean objectsAreEqual(final Object o1, final Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    private String convertValueForDisplay(final Object value) {
        if (value instanceof Date) {
            return historyDateFormat.format((Date) value);
        } else {
            return HTTPUtil.escapeHTMLCharacters(WorkItemUtils.objectToString(value));
        }
    }

    private static String createHTMLFromTextInput(String input) {
        // Escape HTML Character input.
        input = escapeTextInput(input);

        final String[] parts = input.split(NEWLINE);

        if (parts.length == 1) {
            return input;
        }

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < parts.length; i++) {
            sb.append("<P>"); //$NON-NLS-1$
            if (parts[i] == null || parts[i].length() == 0) {
                sb.append("&nbsp;"); //$NON-NLS-1$
            } else {
                sb.append(parts[i]);
            }
            sb.append("</P>"); //$NON-NLS-1$
            sb.append(NEWLINE);
        }

        return sb.toString();
    }

    private static String escapeTextInput(String untrustedData) {
        untrustedData = StringUtil.replace(untrustedData, "&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
        untrustedData = StringUtil.replace(untrustedData, "<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
        untrustedData = StringUtil.replace(untrustedData, ">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$
        untrustedData = StringUtil.replace(untrustedData, "\"", "&quot;"); //$NON-NLS-1$ //$NON-NLS-2$
        untrustedData = StringUtil.replace(untrustedData, "'", "&#x27;"); //$NON-NLS-1$ //$NON-NLS-2$
        untrustedData = StringUtil.replace(untrustedData, "/", "&#x2F;"); //$NON-NLS-1$ //$NON-NLS-2$
        return untrustedData;
    }

    private static class HistoryTextControlDecorator implements FocusListener {
        private static final String HISTORY_TEXT_DECORATION_KEY = "history-text-decoration-key"; //$NON-NLS-1$
        private static final String DECORATION_TEXT = WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)
            ? Messages.getString("WorkItemHistoryControl.TypeCommentHere") //$NON-NLS-1$
            : Messages.getString("WorkItemHistoryControl.TypeCommentHereWithNewline"); //$NON-NLS-1$

        public static void addInitialDecoration(final Text text) {
            final Boolean addedDecoration = (Boolean) text.getData(HISTORY_TEXT_DECORATION_KEY);
            if (addedDecoration != null && addedDecoration.booleanValue()) {
                return;
            }

            final ModifyListener modifyListener = (ModifyListener) text.getData(UPDATE_WORK_ITEM_LISTENER_KEY);
            if (modifyListener != null) {
                text.removeModifyListener(modifyListener);
            }
            text.setText(DECORATION_TEXT);
            text.setData(HISTORY_TEXT_DECORATION_KEY, Boolean.valueOf(true));
            if (modifyListener != null) {
                text.addModifyListener(modifyListener);
            }
        }

        @Override
        public void focusGained(final FocusEvent e) {
            final Text text = (Text) e.widget;

            final Boolean addedDecoration = (Boolean) e.widget.getData(HISTORY_TEXT_DECORATION_KEY);
            if (addedDecoration == null || !addedDecoration.booleanValue()) {
                return;
            }

            final ModifyListener modifyListener = (ModifyListener) text.getData(UPDATE_WORK_ITEM_LISTENER_KEY);
            text.removeModifyListener(modifyListener);
            text.setText(""); //$NON-NLS-1$
            text.addModifyListener(modifyListener);

            // Mac hack: grow to at least 60px so that the scroll bar displays
            // properly
            if (WindowSystem.isCurrentWindowSystem(WindowSystem.AQUA)) {
                ((GridData) text.getLayoutData()).heightHint = 70;
                text.getParent().layout(true);
            }

            e.widget.setData(HISTORY_TEXT_DECORATION_KEY, null);
        }

        @Override
        public void focusLost(final FocusEvent e) {
            final Text text = (Text) e.widget;

            if (text.getText().trim().length() != 0) {
                return;
            }

            final Boolean addedDecoration = (Boolean) e.widget.getData(HISTORY_TEXT_DECORATION_KEY);
            if (addedDecoration != null && addedDecoration.booleanValue()) {
                return;
            }

            final ModifyListener modifyListener = (ModifyListener) text.getData(UPDATE_WORK_ITEM_LISTENER_KEY);
            text.removeModifyListener(modifyListener);
            text.setText(DECORATION_TEXT);
            text.addModifyListener(modifyListener);

            e.widget.setData(HISTORY_TEXT_DECORATION_KEY, Boolean.valueOf(true));
        }
    }
}