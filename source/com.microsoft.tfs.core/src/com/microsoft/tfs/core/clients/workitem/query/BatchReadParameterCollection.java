// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.util.Check;

/**
 * Represents a collection of parameters from a batch read.
 *
 * @since TEE-SDK-10.1
 */
public class BatchReadParameterCollection {
    private final List<BatchReadParameter> parameters = new ArrayList<BatchReadParameter>();

    public void add(final BatchReadParameter parameter) {
        Check.notNull(parameter, "parameter"); //$NON-NLS-1$

        if (!parameters.contains(parameter)) {
            parameters.add(parameter);
        }
    }

    public int getSize() {
        return parameters.size();
    }

    public BatchReadParameter getParameter(final int ix) {
        return parameters.get(ix);
    }
}
