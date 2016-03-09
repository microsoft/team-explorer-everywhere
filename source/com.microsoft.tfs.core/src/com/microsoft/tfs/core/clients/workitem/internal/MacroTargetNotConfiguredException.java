// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import com.microsoft.tfs.core.exceptions.TECoreException;

/**
 * Exception raised when a valid macro definition in a WIT link's URLRoot does
 * not have a corresponding target configured (e.g. the server does not have a
 * portal configured and a portal macro is used in a link).
 *
 *
 * @threadsafety thread-safe
 */
public class MacroTargetNotConfiguredException extends TECoreException {
    private static final long serialVersionUID = 9021829377553436251L;

    private final String title;
    private final String body;

    public MacroTargetNotConfiguredException(final String messageTitle, final String messageBody) {
        super();

        title = messageTitle;
        body = messageBody;
    }

    public String getMessageTitle() {
        return title;
    }

    public String getMessageBody() {
        return body;
    }
}
