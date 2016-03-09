// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.favorites.exceptions;

import com.microsoft.tfs.core.clients.favorites.FavoriteItem;
import com.microsoft.tfs.util.Check;

public class DuplicateFavoritesException extends FavoritesException {
    private final FavoriteItem duplicateItem;

    public DuplicateFavoritesException(final FavoriteItem duplicateItem) {
        super();

        Check.notNull(duplicateItem, "duplicateItem"); //$NON-NLS-1$
        this.duplicateItem = duplicateItem;
    }

    public DuplicateFavoritesException(final String message, final FavoriteItem duplicateItem) {
        super(message);

        Check.notNull(duplicateItem, "duplicateItem"); //$NON-NLS-1$
        this.duplicateItem = duplicateItem;
    }

    public DuplicateFavoritesException(final FavoriteItem duplicateItem, final Throwable cause) {
        super(cause);

        Check.notNull(duplicateItem, "duplicateItem"); //$NON-NLS-1$
        this.duplicateItem = duplicateItem;
    }

    public FavoriteItem getDuplicateItem() {
        return duplicateItem;
    }
}
