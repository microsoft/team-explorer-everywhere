// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.reporting.ReportUtils;
import com.microsoft.tfs.core.clients.sharepoint.WSSUtils;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.pguidance.ProcessGuidanceURLInfo;
import com.microsoft.tfs.util.Check;

public class MacroHelpers {
    /**
     * The fixed macros tokens which may appear in a link parameter definition.
     */
    public static final String ParamMacroProcessGuidanceUrl = "@processguidance"; //$NON-NLS-1$
    public static final String ParamMacroPortal = "@portalpage"; //$NON-NLS-1$
    public static final String ParamMacroReportManagerUrl = "@reportmanagerurl"; //$NON-NLS-1$
    public static final String ParamMacroReportServiceSiteUrl = "@reportservicesiteurl"; //$NON-NLS-1$

    static final String[] macros = new String[] {
        ParamMacroProcessGuidanceUrl,
        ParamMacroPortal,
        ParamMacroReportManagerUrl,
        ParamMacroReportServiceSiteUrl,
    };

    /**
     * Returns true if the specified value starts with one of the fixed macro
     * tokens.
     */
    public static boolean isMacro(final String value) {
        Check.notNull(value, "value"); //$NON-NLS-1$
        if (value.startsWith("@")) //$NON-NLS-1$
        {
            final String valueLower = value.toLowerCase();
            for (int i = 0; i < macros.length; i++) {
                if (valueLower.startsWith(macros[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks the beginning of the provided string for a macro and replaces it
     * if necessary. If the string starts with a macro but the macro value is
     * undefined (not configured), an exception is thrown.
     *
     * @param workItem
     *        The work item which contains the link with a potential macro in
     *        the URL.
     *
     * @param uriWithMacros
     *        The link's URL which may or may not contain a macro.
     *
     * @exception MacroTargetNotConfiguredException
     *            Thrown if a valid macro cannot be expanded because the
     *            corresponding service is not configured.
     */
    public static String resolveURIMacros(final WorkItem workItem, final String uriWithMacros) {
        String trimmedUrl = uriWithMacros.trim();

        final String witProtocol = "x-mvwit:"; //$NON-NLS-1$
        if (trimmedUrl.startsWith(witProtocol)) {
            trimmedUrl = trimmedUrl.substring(witProtocol.length());
        }

        final Project project = workItem.getType().getProject();
        final TFSTeamProjectCollection connection = project.getWorkItemClient().getConnection();

        final String compareUrl = trimmedUrl.toLowerCase();
        String macro = null;
        String url = null;

        // Check to see if the trimmed URL's prefix matches any macro. Attempt
        // to retrieve
        // the corresponding URL when a match is found.
        if (compareUrl.startsWith(ParamMacroReportServiceSiteUrl)) {
            macro = ParamMacroReportManagerUrl;
            url = ReportUtils.getReportServiceURL(connection);
        } else if (compareUrl.startsWith(ParamMacroProcessGuidanceUrl)) {
            macro = ParamMacroProcessGuidanceUrl;

            final ProcessGuidanceURLInfo processGuidence = workItem.getType().getProcessGuidanceURL();

            if (processGuidence != null && processGuidence.isValid()) {
                url = processGuidence.getURL();
            }
        } else if (compareUrl.startsWith(ParamMacroPortal)) {
            macro = ParamMacroPortal;
            url = WSSUtils.getWSSURL(connection, new ProjectInfo(project.getName(), project.getURI()));
        } else if (compareUrl.startsWith(ParamMacroReportManagerUrl)) {
            macro = ParamMacroReportManagerUrl;
            url = ReportUtils.getReportManagerURL(connection);
        } else {
            return uriWithMacros;
        }

        // The trimmed URL's prefix matched a macro. If a valid macro URL was
        // found, replace the macro
        // with the retrieved value, otherwise throw an exception. The exception
        // will occur in the
        // case where a particular service targeted by the macro is not
        // configured on the server.
        if (url != null && url.length() > 0) {
            if (trimmedUrl.length() == macro.length()) {
                return url;
            } else {
                return url + trimmedUrl.substring(macro.length());
            }
        } else {
            String title;
            String body;

            if (macro.equals(ParamMacroPortal)) {
                title = Messages.getString("MacroHelpers.NoPortalConfgiredTitle"); //$NON-NLS-1$
                body = Messages.getString("MacroHelpers.NoPortalConfiguredBody"); //$NON-NLS-1$
            } else if (macro.equals(ParamMacroProcessGuidanceUrl)) {
                title = Messages.getString("MacroHelpers.NoProcessConfiguredTitle"); //$NON-NLS-1$
                body = Messages.getString("MacroHelpers.NoProcessConfiguredBody"); //$NON-NLS-1$
            } else {
                title = Messages.getString("MacroHelpers.ReportsNotEnabledTitle"); //$NON-NLS-1$
                body = Messages.getString("MacroHelpers.ReportesNotEnabledBody"); //$NON-NLS-1$
            }

            throw new MacroTargetNotConfiguredException(title, body);
        }
    }
}
