// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.favorites;

import java.util.Arrays;

import com.microsoft.tfs.util.GUID;

import junit.framework.TestCase;

public class FavoriteItemTest extends TestCase {
    public void testDefaults() {
        final FavoriteItem item = new FavoriteItem();
        assertEquals(GUID.EMPTY, item.getID());
        assertEquals(GUID.EMPTY, item.getParentID());
        assertNull(item.getName());
        assertNull(item.getType());
        assertNull(item.getData());
    }

    public void testAccessors() {
        final FavoriteItem item = new FavoriteItem();

        final GUID id = GUID.newGUID();
        final GUID parentID = GUID.newGUID();

        item.setID(id);
        item.setParentID(parentID);
        item.setName("name"); //$NON-NLS-1$
        item.setType("bar"); //$NON-NLS-1$
        item.setData("xyz"); //$NON-NLS-1$

        assertEquals(id, item.getID());
        assertEquals(parentID, item.getParentID());
        assertEquals("name", item.getName()); //$NON-NLS-1$
        assertEquals("bar", item.getType()); //$NON-NLS-1$
        assertEquals("xyz", item.getData()); //$NON-NLS-1$
    }

    public void testSerializeDefaults() {
        final FavoriteItem item = new FavoriteItem();
        assertEquals("{}", item.serialize()); //$NON-NLS-1$
    }

    public void testSerializeGUIDs() {
        // The "default" value for a GUID is GUID.EMPTY, and this is not
        // serialized.

        final FavoriteItem item = new FavoriteItem();
        item.setID(GUID.EMPTY);
        item.setParentID(GUID.EMPTY);

        String json = "{}"; //$NON-NLS-1$
        assertEquals(json, item.serialize());

        // Non-empty values are serialized

        item.setID(new GUID("daa17708-5b9b-2c4c-a3e5-511f5c658e24")); //$NON-NLS-1$
        item.setParentID(new GUID("ABCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFc")); //$NON-NLS-1$

        json = "{\"id\":\"daa17708-5b9b-2c4c-a3e5-511f5c658e24\"," //$NON-NLS-1$
            + "\"parentId\":\"abcdefab-0ffa-bcda-a055-abcdefffaffc\"}"; //$NON-NLS-1$

        assertEquals(json, sortSerialization(item.serialize()));
    }

    public void testSerializeAll() {
        final FavoriteItem item = new FavoriteItem();
        item.setID(new GUID("daa17708-5b9b-2c4c-a3e5-511f5c658e24")); //$NON-NLS-1$
        item.setParentID(new GUID("ABCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFc")); //$NON-NLS-1$
        item.setName("abc"); //$NON-NLS-1$
        item.setType("xyz"); //$NON-NLS-1$
        item.setData("lmnop"); //$NON-NLS-1$

        // GUIDs to go lowercase
        final String json =
            "{\"data\":\"lmnop\",\"id\":\"daa17708-5b9b-2c4c-a3e5-511f5c658e24\",\"name\":\"abc\",\"parentId\":\"abcdefab-0ffa-bcda-a055-abcdefffaffc\",\"type\":\"xyz\"}"; //$NON-NLS-1$

        assertEquals(json, sortSerialization(item.serialize()));
    }

    public String sortSerialization(final String serialStr) {
        if (serialStr.length() <= 2) {
            return serialStr;
        }
        final String serialization = serialStr.substring(1, serialStr.length() - 1);
        final String tags[] = serialization.split(","); //$NON-NLS-1$
        Arrays.sort(tags);
        final StringBuffer sb = new StringBuffer();
        sb.append('{');
        for (final String tag : tags) {
            sb.append(tag);
            sb.append(',');
        }
        sb.setCharAt(sb.length() - 1, '}');
        return sb.toString();
    }

    public void testDeserializeDefaults() {
        final FavoriteItem item = FavoriteItem.deserialize("{}"); //$NON-NLS-1$
        assertNotNull(item);
        assertEquals(GUID.EMPTY, item.getID());
        assertEquals(GUID.EMPTY, item.getParentID());
        assertNull(item.getName());
        assertNull(item.getType());
        assertNull(item.getData());
    }

    public void testDeserializeGUIDs() {
        String json = "{\"parentId\":\"00000000-0000-0000-0000-000000000000\"," //$NON-NLS-1$
            + "\"id\":\"00000000-0000-0000-0000-000000000000\"}"; //$NON-NLS-1$

        FavoriteItem item = FavoriteItem.deserialize(json);

        assertNotNull(item);
        assertEquals(GUID.EMPTY, item.getID());
        assertEquals(GUID.EMPTY, item.getParentID());
        assertNull(item.getName());
        assertNull(item.getType());
        assertNull(item.getData());

        // Non-empty GUID values are serialized

        json = "{\"parentId\":\"abcdefab-0ffa-bcda-a055-abcdefffaffc\"," //$NON-NLS-1$
            + "\"id\":\"daa17708-5b9b-2c4c-a3e5-511f5c658e24\"}"; //$NON-NLS-1$

        item = FavoriteItem.deserialize(json);
        assertNotNull(item);
        assertEquals(new GUID("daa17708-5b9b-2c4c-a3e5-511f5c658e24"), item.getID()); //$NON-NLS-1$
        assertEquals(new GUID("ABCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFc"), item.getParentID()); //$NON-NLS-1$
        assertNull(item.getName());
        assertNull(item.getType());
        assertNull(item.getData());
    }

    public void testDeserializeAll() {
        final String json = "{\"parentId\":\"ABCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFc\"," //$NON-NLS-1$
            + "\"type\":\"xyz\",\"data\":\"lmnop\"," //$NON-NLS-1$
            + "\"name\":\"abc\",\"id\":\"daa17708-5b9b-2c4c-a3e5-511f5c658e24\"}"; //$NON-NLS-1$

        final FavoriteItem item = FavoriteItem.deserialize(json);

        assertNotNull(item);
        assertEquals(new GUID("daa17708-5b9b-2c4c-a3e5-511f5c658e24"), item.getID()); //$NON-NLS-1$
        assertEquals(new GUID("ABCDEFab-0ffA-BcDa-A055-AbcDEfFFAFFc"), item.getParentID()); //$NON-NLS-1$
        assertEquals("abc", item.getName()); //$NON-NLS-1$
        assertEquals("xyz", item.getType()); //$NON-NLS-1$
        assertEquals("lmnop", item.getData()); //$NON-NLS-1$
    }
}
