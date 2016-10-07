// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.StringUtil;

/**
 * Environment variables only used by the CLC.
 */
public abstract class EnvironmentVariables {
    /**
     * When set to any value, credentials provided by command-line options or at
     * the interactive prompt will be saved to the workspace cache
     * <em>when the workspace cache is updated</em>. The cache is not updated
     * during all commands, only for ones that query, create, or delete
     * workspaces.
     * <p>
     * The default (when this variable is not set) is that no credentials are
     * saved to the workspace cache.
     */
    public static final String AUTO_SAVE_CREDENTIALS = "TF_AUTO_SAVE_CREDENTIALS"; //$NON-NLS-1$

    /**
     * When set to any value, this program (with included arguments) is used to
     * compare files for the CLC's "difference" ("diff") command.
     * <p>
     * The string is tokenized, and the first token is the command to run, the
     * others are placeholders for arguments substituted before the command is
     * run.
     * <p>
     * The default (when this variable is not set) is to print an error with
     * instructions on setting the variable.
     */
    public static final String EXTERNAL_DIFF_COMMAND = "TF_DIFF_COMMAND"; //$NON-NLS-1$

    /**
     * When set to any value, this program (with included arguments) is used to
     * perform content merges for the CLC's "resolve" command.
     * <p>
     * The string is tokenized, and the first token is the command to run, the
     * others are placeholders for arguments substituted before the command is
     * run.
     * <p>
     * The default (when this variable is not set) is to print an error with
     * instructions on setting the variable.
     */
    public static final String EXTERNAL_MERGE_COMMAND = "TF_MERGE_COMMAND"; //$NON-NLS-1$

    /**
     * Specifies the value to use as the HTTP <em>and</em> HTTPS proxy for all
     * connections. Upper-case and lower-case variables are supported.
     * <p>
     * The default (when this variable is not set) is to use the operating
     * system's proxy, if it can be detected, otherwise no proxy is used.
     */
    public static final String HTTP_PROXY_URL = "HTTP_PROXY"; //$NON-NLS-1$
    public static final String HTTP_PROXY_URL_ALTERNATE = "http_proxy"; //$NON-NLS-1$

    /**
     * Specifies the value to use as the HTTPS proxy for all connections.
     * Upper-case and lower-case variables are supported.
     * <p>
     * The default (when this variable is not set) is to use the operating
     * system's proxy, if it can be detected, otherwise no proxy is used.
     */
    public static final String HTTPS_PROXY_URL = "HTTPS_PROXY"; //$NON-NLS-1$
    public static final String HTTPS_PROXY_URL_ALTERNATE = "https_proxy"; //$NON-NLS-1$

    /**
     * Specifies the list of hosts / domain names that will <em>not</em> be
     * subject to the {@link HTTP_PROXY_URL} environment variable (above.)
     */
    public static final String NO_PROXY_HOSTS = "NO_PROXY"; //$NON-NLS-1$
    public static final String NO_PROXY_HOSTS_ALTERNATE = "no_proxy"; //$NON-NLS-1$

    /**
     * When set to any value, summaries of get and merge operations are not
     * displayed.
     * <p>
     * Visual Studio uses this environment variable for the same purpose.
     */
    public static final String NO_SUMMARY = "TFSVC_NOSUMMARY"; //$NON-NLS-1$

    /**
     * When set to any value, disables the sending of telemetry values.
     * <p>
     * IntelliJ uses this environment variable when calling the CLC.
     */
    public static final String NO_TELEMETRY = "TF_NOTELEMETRY"; //$NON-NLS-1$

    /**
     * This environment variable is only used on Mac OS. When set to False, No
     * or N PersistanceCredentialsManager will be used to store credentials
     * instead of KeyChain
     * <p>
     * The default (when this variable is not set) is that KeyChain is used to
     * save credentials
     */
    public static final String USE_KEYCHAIN = "TF_USE_KEYCHAIN"; //$NON-NLS-1$

    /**
     * This environment variable is used to disable the oauth2 interactive login
     * flow when we issue request against VSTS instances
     * <p>
     * When set to False, we will bypass showing the browser to user.
     */
    public static final String BYPASS_INTERACTIVE_BROWSER_LOGIN = "TF_BYPASS_BROWSER_LOGIN"; //$NON-NLS-1$

    public static boolean getBoolean(final String variableName, final boolean defaultValue) {
        final String value = PlatformMiscUtils.getInstance().getEnvironmentVariable(variableName);

        if (StringUtil.isNullOrEmpty(value)) {
            return defaultValue;
        } else {
            return !value.equalsIgnoreCase("FALSE") //$NON-NLS-1$
                && !value.equalsIgnoreCase("NO") //$NON-NLS-1$
                && !value.equalsIgnoreCase("N"); //$NON-NLS-1$
        }
    }

}
