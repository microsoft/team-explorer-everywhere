// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

public class WIValueSource {
    /**
     * Indicates that the source of a value is the TFS server. The value can be
     * assumed to be a String since that is the only data type the server is
     * capable of sending to the client (true for work item field data, not SOAP
     * in general). The type converter may choose to be more strict about its
     * conversion or use conversion routines that only make sense for server
     * data but not user data.
     */
    public static final WIValueSource SERVER = new WIValueSource("SERVER"); //$NON-NLS-1$

    /**
     * Indicates that the source of a value is the local system. This may mean
     * many things. The value could be directly from the user of the WIT OM. The
     * value may come from some other field value or the WIT metadata cache (eg
     * through the rule engine). The value may come from internal work item
     * code. In any case, the type converter must not make any assumption about
     * the type of the data.
     */
    public static final WIValueSource LOCAL = new WIValueSource("LOCAL"); //$NON-NLS-1$

    private final String type;

    private WIValueSource(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
