// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Local workspace baseline file missing exception.
 *
 * @since TEE-SDK-11
 */
public class MissingBaselineException extends VersionControlException {
    private static final long serialVersionUID = -7645862032591267364L;
    private final String targetLocalItem;

    public MissingBaselineException(final String targetLocalItem) {
        super(MessageFormat.format(
            Messages.getString("MissingBaselineException.MissingBaselineFormat"), //$NON-NLS-1$
            targetLocalItem));
        this.targetLocalItem = targetLocalItem;
    }

    public String getTargetLocalItem() {
        return targetLocalItem;
    }

}
