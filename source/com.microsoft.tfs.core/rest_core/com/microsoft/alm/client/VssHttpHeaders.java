// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.alm.client;

public abstract class VssHttpHeaders {
    public final static String ACTIVITY_ID = "ActivityId"; //$NON-NLS-1$
    public final static String TFS_VERSION = "X-TFS-Version"; //$NON-NLS-1$
    public final static String TFS_REDIRECT = "X-TFS-Redirect"; //$NON-NLS-1$
    public final static String TFS_EXCEPTION = "X-TFS-Exception"; //$NON-NLS-1$
    public final static String TFS_SERVICE_ERROR = "X-TFS-ServiceError"; //$NON-NLS-1$
    public final static String TFS_SESSION_HEADER = "X-TFS-Session"; //$NON-NLS-1$
    public final static String TFS_FED_AUTH_REALM = "X-TFS-FedAuthRealm"; //$NON-NLS-1$
    public final static String TFS_FED_AUTH_ISSUER = "X-TFS-FedAuthIssuer"; //$NON-NLS-1$
    public final static String TFS_FED_AUTH_REDIRECT = "X-TFS-FedAuthRedirect"; //$NON-NLS-1$
    public final static String VSS_PAGE_HANDLERS = "X-VSS-PageHandlers"; //$NON-NLS-1$
    public final static String VSS_USER_DATA = "X-VSS-UserData"; //$NON-NLS-1$
    public final static String VSS_AGENT_HEADER = "X-VSS-Agent"; //$NON-NLS-1$
    public final static String VSS_AUTHENTICATE_ERROR = "X-VSS-AuthenticateError"; //$NON-NLS-1$

    public final static String ACCEPT = "Accept"; //$NON-NLS-1$
    public final static String CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$
    public final static String CONTENT_ENCODING = "Content-Encoding"; //$NON-NLS-1$
    public final static String CONTENT_LENGTH = "Content-Length"; //$NON-NLS-1$
    public final static String HTTP_METHOD_OVERRIDE = "X-HTTP-Method-Override"; //$NON-NLS-1$

    public final static String CONTENT_TYPE_HEADER = "Content-Type"; //$NON-NLS-1$
    public final static String CONTENT_LENGTH_HEADER = "Content-Length"; //$NON-NLS-1$
}
