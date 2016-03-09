// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.favorites;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.GUID;

/**
 *
 * Provides a contract for persistence and retrieval of FavoriteItems.
 *
 * @threadsafety unknown
 */
public interface IFavoritesStore {
    /**
     * Provides uniform Connection Mechanism for store instances.
     */
    void connect(TFSTeamProjectCollection tpc, String filterNamespace, GUID identity);

    /**
     * Indicates if the Favorites Store is connected.
     */
    boolean isConnected();

    /**
     * Gets Favorite items associated with connection state.
     */
    FavoriteItem[] getFavorites();

    /**
     * Updates Favorites store with supplied items - Flushes immediately.
     */
    void updateFavorites(FavoriteItem[] items);

    /**
     * Removes specified favorites - Flushes immediately.
     */
    void remove(GUID[] items);

    /**
     * Provides the Identity associated with the stored user, which is unique
     * within scope of the TPC. Format is not guaranteed.
     */
    GUID getIdentity();
}