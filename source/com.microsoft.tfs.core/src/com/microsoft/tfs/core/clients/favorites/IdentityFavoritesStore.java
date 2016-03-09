// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.favorites;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.favorites.exceptions.DuplicateFavoritesException;
import com.microsoft.tfs.core.clients.favorites.exceptions.FavoritesException;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceNames;
import com.microsoft.tfs.core.clients.webservices.IIdentityManagementService2;
import com.microsoft.tfs.core.clients.webservices.IdentityPropertyScope;
import com.microsoft.tfs.core.clients.webservices.MembershipQuery;
import com.microsoft.tfs.core.clients.webservices.ReadIdentityOptions;
import com.microsoft.tfs.core.clients.webservices.TeamFoundationIdentity;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.json.JSONParseException;

/**
 * Identity Store based implementation of IFavoritesStore
 *
 * @threadsafety thread-compatible
 */
public class IdentityFavoritesStore implements IFavoritesStore {
    private final static Log log = LogFactory.getLog(IdentityFavoritesStore.class);
    private final Object lock = new Object();

    private TFSTeamProjectCollection tpc;
    private String filterNamespace;
    private GUID identityGUID = GUID.EMPTY;

    /**
     * {@inheritDoc}
     * <p>
     * Initializes store, if the supplied Team Project Collection supports
     * Identity Management Service 2, which is currently required for using
     * Identity based Favorites.
     */
    @Override
    public void connect(final TFSTeamProjectCollection tpc, final String filterNamespace, final GUID identity) {
        Check.notNull(tpc, "tpc"); //$NON-NLS-1$
        Check.notNullOrEmpty(filterNamespace, "filterNamespace"); //$NON-NLS-1$
        Check.notNull(identity, "identity"); //$NON-NLS-1$

        final String location = tpc.getServerDataProvider().locationForCurrentConnection(
            ServiceInterfaceNames.IDENTITY_MANAGEMENT_2,
            ServiceInterfaceIdentifiers.COLLECTION_IDENTITY_MANAGEMENT_2);

        if (location != null && location.length() > 0) {
            synchronized (lock) {
                this.tpc = tpc;
                this.filterNamespace = filterNamespace;
                this.identityGUID = identity;
            }
        }
    }

    @Override
    public boolean isConnected() {
        synchronized (lock) {
            return tpc != null
                && !GUID.EMPTY.equals(identityGUID)
                && filterNamespace != null
                && filterNamespace.length() > 0;
        }
    }

    @Override
    public FavoriteItem[] getFavorites() {
        synchronized (lock) {
            Check.isTrue(isConnected(), "isConnected()"); //$NON-NLS-1$

            return getFavorites(getCurrentIdentity());
        }
    }

    @Override
    public void updateFavorites(final FavoriteItem[] items) {
        synchronized (lock) {
            Check.isTrue(isConnected(), "isConnected()"); //$NON-NLS-1$

            final TeamFoundationIdentity identity = getCurrentIdentity();
            updateFavorites(identity, filterNamespace, items);
            flushFavorites(identity, tpc);
        }
    }

    @Override
    public void remove(final GUID[] items) {
        synchronized (lock) {
            Check.isTrue(isConnected(), "isConnected()"); //$NON-NLS-1$

            final TeamFoundationIdentity identity = getCurrentIdentity();
            remove(identity, filterNamespace, items);
            flushFavorites(identity, tpc);
        }
    }

    @Override
    public GUID getIdentity() {
        return identityGUID;
    }

    private TeamFoundationIdentity getCurrentIdentity() {
        if (isConnected()) {
            return getCurrentIdentity(tpc, identityGUID, filterNamespace);
        } else {
            return null;
        }
    }

    /**
     * Get current Identity w/extended properties present for querying/updating
     * favorites information.
     */
    private static TeamFoundationIdentity getCurrentIdentity(
        final TFSTeamProjectCollection tpc,
        final GUID identityGuid,
        final String filterNamespace) {
        if (tpc != null) {
            final String[] propertyNameFilters = new String[] {
                filterNamespace + "*" //$NON-NLS-1$
            };

            final IIdentityManagementService2 identitySvc =
                (IIdentityManagementService2) tpc.getClient(IIdentityManagementService2.class);

            final TeamFoundationIdentity[] ids = identitySvc.readIdentities(
                new GUID[] {
                    identityGuid
            },
                MembershipQuery.DIRECT,
                ReadIdentityOptions.EXTENDED_PROPERTIES,
                propertyNameFilters,
                IdentityPropertyScope.LOCAL);

            Check.isTrue(ids.length == 1, "ids.length == 1"); //$NON-NLS-1$
            return ids[0];
        }
        return null;
    }

    /**
     * Returns list of favoriteItem objects. Application itself can create a
     * Tree repesentation out of this list using Id and ParentId properties of
     * entries
     */
    private static FavoriteItem[] getFavorites(final TeamFoundationIdentity identity) {
        Check.notNull(identity, "identity"); //$NON-NLS-1$

        final Iterable<Entry<String, Object>> result = identity.getProperties(IdentityPropertyScope.LOCAL);

        final List<FavoriteItem> favorites = new ArrayList<FavoriteItem>();
        for (final Entry<String, Object> entry : result) {
            final Object value = entry.getValue();
            if (value instanceof String) {
                try {
                    favorites.add(FavoriteItem.deserialize((String) value));
                } catch (final JSONParseException e) {
                    log.error(MessageFormat.format("Error deserializing favorite from JSON: {0}", value), e); //$NON-NLS-1$
                }
            }
        }

        return favorites.toArray(new FavoriteItem[favorites.size()]);
    }

    /**
     * Updates/Adds supplied favorite Items
     */
    private static void updateFavorites(
        final TeamFoundationIdentity identity,
        final String effectiveNamespace,
        final FavoriteItem[] items) {
        final Map<GUID, FavoriteItem> favorites = new HashMap<GUID, FavoriteItem>();
        for (final FavoriteItem item : getFavorites(identity)) {
            favorites.put(item.getID(), item);
        }

        for (final FavoriteItem favItem : items) {
            if (!GUID.EMPTY.equals(favItem.getParentID())) {
                if (!favorites.containsKey(favItem.getParentID())) {
                    throw new FavoritesException(Messages.getString("IdentityFavoritesStore.ParentFavoriteNotFound")); //$NON-NLS-1$
                }
            }

            for (final FavoriteItem f : favorites.values()) {
                if (f.getParentID().equals(favItem.getParentID())) {
                    if (!f.getID().equals(favItem.getID())
                        && String.CASE_INSENSITIVE_ORDER.compare(f.getType() + "", favItem.getType() + "") == 0 //$NON-NLS-1$ //$NON-NLS-2$
                        && String.CASE_INSENSITIVE_ORDER.compare(f.getName() + "", favItem.getName() + "") == 0 //$NON-NLS-1$ //$NON-NLS-2$
                        && String.CASE_INSENSITIVE_ORDER.compare(f.getData() + "", favItem.getData() + "") == 0) //$NON-NLS-1$ //$NON-NLS-2$
                    {
                        throw new DuplicateFavoritesException(
                            Messages.getString("IdentityFavoritesStore.FavoriteWithSameNameTypeDataExists"), //$NON-NLS-1$
                            favItem);
                    }
                }
            }

            setViewProperty(
                identity,
                IdentityPropertyScope.LOCAL,
                effectiveNamespace,
                favItem.getID().getGUIDString(),
                favItem.serialize());
        }
    }

    /**
     * Persist Team updates.
     */
    private static void flushFavorites(final TeamFoundationIdentity identity, final TFSTeamProjectCollection tpc) {
        final IIdentityManagementService2 identitySvc =
            (IIdentityManagementService2) tpc.getClient(IIdentityManagementService2.class);

        identitySvc.updateExtendedProperties(identity);
    }

    /**
     * Remove entries by given id list.
     * <p>
     * No-op on empty input
     */
    private static void remove(
        final TeamFoundationIdentity identity,
        final String effectiveNamespace,
        final GUID[] items) {
        if (items == null || items.length == 0) {
            return;
        }

        boolean allEmpty = true;
        for (final GUID item : items) {
            if (!GUID.EMPTY.equals(item)) {
                allEmpty = false;
                break;
            }
        }

        if (allEmpty) {
            return;
        }

        final Map<GUID, FavoriteItem> dict = new HashMap<GUID, FavoriteItem>();
        for (final FavoriteItem item : getFavorites(identity)) {
            dict.put(item.getID(), item);
        }

        final Set<GUID> itemsToDelete = new HashSet<GUID>(Arrays.asList(items));

        for (final FavoriteItem favItem : dict.values()) {
            fixTree(favItem, dict, itemsToDelete);
        }

        for (final GUID id : itemsToDelete) {
            removeViewProperty(identity, IdentityPropertyScope.LOCAL, effectiveNamespace, id.getGUIDString());
        }
    }

    /**
     * Sets a property value with the view namespace.
     */
    private static void setViewProperty(
        final TeamFoundationIdentity identity,
        final IdentityPropertyScope propertyScope,
        final String effectiveNamespace,
        final String propertyName,
        final Object propertyValue) {
        identity.setProperty(propertyScope, effectiveNamespace + propertyName, propertyValue);
    }

    /**
     * Remove view property, if it exists.
     */
    private static void removeViewProperty(
        final TeamFoundationIdentity identity,
        final IdentityPropertyScope propertyScope,
        final String effectiveNamespace,
        final String propertyName) {
        setViewProperty(identity, propertyScope, effectiveNamespace, propertyName, null);
    }

    /**
     * Helper to clean up child entries in case a parent gets deleted
     */
    private static boolean fixTree(
        final FavoriteItem item,
        final Map<GUID, FavoriteItem> items,
        final Set<GUID> itemsToDelete) {
        boolean result = false;

        if (itemsToDelete.contains(item.getID())) {
            return result;
        }

        if (GUID.EMPTY.equals(item.getParentID())) {
            result = true;
        } else if (items.containsKey(item.getParentID())) {
            result = fixTree(items.get(item.getParentID()), items, itemsToDelete);
        }

        if (!result) {
            itemsToDelete.add(item.getID());
        }

        return result;
    }
}
