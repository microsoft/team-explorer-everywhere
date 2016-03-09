// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._Failure;
import ms.tfs.versioncontrol.clientservices._03._Warning;
import ms.tfs.versioncontrol.clientservices._03._WarningType;

/**
 * Represents a non-fatal version control failure as returned by the server.
 *
 * @since TEE-SDK-10.1
 */
public class Failure extends WebServiceObjectWrapper {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String INDENT = "   "; //$NON-NLS-1$

    public Failure(final _Failure failure) {
        super(failure);
    }

    /**
     * Constructs a {@link Failure} that represents a local workspace failure.
     *
     *
     * @param message
     *        the message string (must not be <code>null</code>)
     * @param code
     *        the error code string (must not be <code>null</code>)
     * @param severity
     *        the severity (must not be <code>null</code>)
     * @param item
     *        the server or local item path
     */
    public Failure(final String message, final String code, final SeverityType severity, final String item) {
        /*
         * code, severity, and message set here; item is set below.
         */
        this(
            new _Failure(
                null,
                code,
                severity.getWebServiceObject(),
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                null,
                message));

        if (item != null && ServerPath.isServerPath(item)) {
            setServerItem(item);
        } else {
            setLocalItem(item);
        }
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Failure getWebServiceObject() {
        return (_Failure) webServiceObject;
    }

    public SeverityType getSeverity() {
        return SeverityType.fromWebServiceObject(getWebServiceObject().getSev());
    }

    public String getCode() {
        return getWebServiceObject().getCode();
    }

    public String getServerItem() {
        return getWebServiceObject().getItem();
    }

    public void setServerItem(final String item) {
        getWebServiceObject().setItem(item);
    }

    public RequestType getRequestType() {
        return RequestType.fromWebServiceObject(getWebServiceObject().getReq());
    }

    public void setRequestType(final RequestType type) {
        getWebServiceObject().setReq(type != null ? type.getWebServiceObject() : null);
    }

    public String getLocalItem() {
        return LocalPath.tfsToNative(getWebServiceObject().getLocal());
    }

    public void setLocalItem(final String item) {
        getWebServiceObject().setLocal(LocalPath.nativeToTFS(item));
    }

    public String getMessage() {
        return getWebServiceObject().getMessage();
    }

    public Warning[] getWarnings() {
        return (Warning[]) WrapperUtils.wrap(Warning.class, getWebServiceObject().getWarnings());
    }

    public String getFormattedMessage() {
        final _Failure failure = getWebServiceObject();

        if (failure.getWarnings() == null || failure.getWarnings().length == 0) {
            // No warnings, display message as returned from server
            return failure.getMessage();
        }

        // We have warnings, iterate through them for item.
        final StringBuffer message = new StringBuffer(failure.getItem() + ":" + NEWLINE); //$NON-NLS-1$
        boolean firstWarning = true;
        final _Warning[] warnings = failure.getWarnings();
        for (int i = 0; i < warnings.length; i++) {
            if (firstWarning) {
                firstWarning = false;
            } else {
                message.append(NEWLINE);
            }

            if (_WarningType.NamespacePendingChangeWarning.equals(warnings[i].getWrn())) {
                message.append(INDENT);
                message.append(MessageFormat.format(
                    Messages.getString("Failure.ItemOpenedForChangeInWorkspaceFormat"), //$NON-NLS-1$
                    warnings[i].getCpp(),
                    new ChangeType(warnings[i].getChg(), warnings[i].getChgEx()).toUIString(true),
                    getFormattedWorkspaceName(warnings[i])));
            } else if (_WarningType.StaleVersionWarning.equals(warnings[i].getWrn())) {
                // Assume stale version warning
                message.append(INDENT);
                message.append(Messages.getString("Failure.NewerVersionExistsInSourceControl")); //$NON-NLS-1$
            } else {
                // Assume WarningType.ResourcePengingChangeWarning which is the
                // default
                // in the .NET client if Warning.WarningType has not been passed
                // by the
                // server.
                message.append(INDENT);
                message.append(MessageFormat.format(
                    Messages.getString("Failure.OpenedForChangeInWorkspaceFormat"), //$NON-NLS-1$
                    new ChangeType(warnings[i].getChg(), warnings[i].getChgEx()).toUIString(true),
                    getFormattedWorkspaceName(warnings[i])));
            }
        }

        return message.toString();
    }

    private String getFormattedWorkspaceName(final _Warning warning) {
        return WorkspaceSpec.parse(warning.getWs(), warning.getUserdisp()).toString();
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
