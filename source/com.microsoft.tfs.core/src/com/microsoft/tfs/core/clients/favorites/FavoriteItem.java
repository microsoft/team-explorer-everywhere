// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.favorites;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.json.JSONEncoder;
import com.microsoft.tfs.util.json.JSONObject;

/***
 * Represents favorite entry in Identity Favorites Store
 *
 * @threadsafety thread-compatible
 */
public class FavoriteItem {
    // Names used in JSON form; case is important
    private final static String JSON_ID = "id"; //$NON-NLS-1$
    private final static String JSON_PARENT_ID = "parentId"; //$NON-NLS-1$
    private final static String JSON_NAME = "name"; //$NON-NLS-1$
    private final static String JSON_TYPE = "type"; //$NON-NLS-1$
    private final static String JSON_DATA = "data"; //$NON-NLS-1$

    private GUID id = GUID.EMPTY;
    private GUID parentID = GUID.EMPTY;
    private String name;
    private String type;
    private String data;

    public FavoriteItem() {
    }

    /**
     * Unique Id of the the entry
     */
    public GUID getID() {
        return id;
    }

    public void setID(final GUID id) {
        Check.notNull(id, "id"); //$NON-NLS-1$
        this.id = id;
    }

    /**
     * Id of the parent favorite folder
     */
    public GUID getParentID() {
        return parentID;
    }

    public void setParentID(final GUID parentID) {
        Check.notNull(parentID, "parentID"); //$NON-NLS-1$
        this.parentID = parentID;
    }

    /**
     * Display text for favorite entry
     */
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Application specific favorite entry specifier. Empty or Null represents
     * that Favorite item is a Folder
     */
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Application specific data for the entry
     */
    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    /**
     * Sugar to determine whether entry is a folder
     */
    public boolean isFolder() {
        return type == null || type.length() == 0;
    }

    /**
     * Serializes Entry in JSON format
     */
    public String serialize() {
        final JSONObject o = new JSONObject();

        if (!GUID.EMPTY.equals(id)) {
            o.put(JSON_ID, id.getGUIDString());
        }

        if (!GUID.EMPTY.equals(parentID)) {
            o.put(JSON_PARENT_ID, parentID.getGUIDString());
        }

        if (name != null) {
            o.put(JSON_NAME, name);
        }

        if (type != null) {
            o.put(JSON_TYPE, type);
        }

        if (data != null) {
            o.put(JSON_DATA, data);
        }

        return JSONEncoder.encodeObject(o);
    }

    /**
     * Deserialize JSON string to FavoriteItem class
     *
     * @param value
     *        JSON string (must not be <code>null</code> or empty)
     */
    public static FavoriteItem deserialize(final String value) {
        Check.notNullOrEmpty(value, "value"); //$NON-NLS-1$

        final JSONObject o = JSONEncoder.decodeObject(value);
        final FavoriteItem item = new FavoriteItem();

        final String id = o.get(JSON_ID);
        if (id != null && id.length() > 0) {
            item.id = new GUID(id);
        }

        final String parentID = o.get(JSON_PARENT_ID);
        if (parentID != null && parentID.length() > 0) {
            item.parentID = new GUID(parentID);
        }

        item.name = o.get(JSON_NAME);
        item.type = o.get(JSON_TYPE);
        item.data = o.get(JSON_DATA);

        return item;
    }
}