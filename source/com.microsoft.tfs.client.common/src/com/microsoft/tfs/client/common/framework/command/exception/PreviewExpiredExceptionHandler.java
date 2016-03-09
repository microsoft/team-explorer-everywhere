// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command.exception;

import java.text.DateFormat;
import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.status.TeamExplorerStatus;
import com.microsoft.tfs.core.exceptions.TEPreviewExpiredException;
import com.microsoft.tfs.core.product.ProductInformation;

/**
 * Exception handler for preview expiration exceptions.
 */
public class PreviewExpiredExceptionHandler implements ICommandExceptionHandler {
    @Override
    public IStatus onException(final Throwable t) {
        if (t instanceof TEPreviewExpiredException) {
            return new TeamExplorerStatus(
                IStatus.ERROR,
                TFSCommonClientPlugin.PLUGIN_ID,
                0,
                MessageFormat.format(
                    Messages.getString("PreviewExpiredExceptionHandler.ReviewReleaseExpiredFormat"), //$NON-NLS-1$
                    ProductInformation.getCurrent().getFamilyFullName(),
                    DateFormat.getDateInstance(DateFormat.LONG).format(
                        ((TEPreviewExpiredException) t).getExpirationDate()),
                    ProductInformation.getCurrent().toString()),
                t);
        }

        return null;
    }
}
