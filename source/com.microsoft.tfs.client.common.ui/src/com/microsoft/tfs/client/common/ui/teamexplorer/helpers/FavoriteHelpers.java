// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.text.MessageFormat;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.core.clients.favorites.exceptions.DuplicateFavoritesException;

public class FavoriteHelpers {
    public static void showDuplicateError(final Shell shell, final DuplicateFavoritesException e, final int count) {
        final String message;

        if (count == 1) {
            message = e.getLocalizedMessage();
        } else {
            final String format = Messages.getString("FavoriteHelpers.NoFavoritesAddedFormat"); //$NON-NLS-1$
            message = MessageFormat.format(format, e.getLocalizedMessage(), e.getDuplicateItem().getName());
        }

        final String title = Messages.getString("FavoriteHelpers.FavoriteNotAddedTitle"); //$NON-NLS-1$
        MessageBoxHelpers.errorMessageBox(shell, title, message);
    }
}
