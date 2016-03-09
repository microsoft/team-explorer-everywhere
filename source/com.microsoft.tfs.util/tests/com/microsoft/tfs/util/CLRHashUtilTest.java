// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

public class CLRHashUtilTest extends TestCase {
    private class HashDataPair {
        public int hash;
        public String string;
    }

    private HashDataPair[] loadData(final String resource) throws IOException {
        final Properties p = new Properties();
        p.load(CLRHashUtil.class.getClassLoader().getResourceAsStream(resource));

        final List<HashDataPair> list = new ArrayList<HashDataPair>();
        final Enumeration<?> e = p.propertyNames();
        while (e.hasMoreElements()) {
            final String key = (String) e.nextElement();

            final HashDataPair hdp = new HashDataPair();
            hdp.hash = Integer.parseInt(key);
            hdp.string = p.getProperty(key);
            list.add(hdp);
        }

        return list.toArray(new HashDataPair[list.size()]);
    }

    public void testGetStringHashOrcas32() throws IOException {
        // The HashGenerator.cs program made these files
        for (final HashDataPair pair : loadData("com/microsoft/tfs/util/CLRHashUtilTestData-String32.properties")) //$NON-NLS-1$
        {
            assertEquals(pair.hash, CLRHashUtil.getStringHashOrcas32(pair.string));
        }
    }

    public void testGetStringHashOrcas64() throws IOException {
        // The HashGenerator.cs program made these files
        for (final HashDataPair pair : loadData("com/microsoft/tfs/util/CLRHashUtilTestData-String64.properties")) //$NON-NLS-1$
        {
            assertEquals(pair.hash, CLRHashUtil.getStringHashOrcas64(pair.string));
        }
    }

    public void testGetGUIDHash() throws IOException {
        assertEquals(0, CLRHashUtil.getGUIDHash(GUID.EMPTY));
        assertEquals(285212689, CLRHashUtil.getGUIDHash(new GUID("11111111-1111-1111-1111-111111111111"))); //$NON-NLS-1$

        // The HashGenerator.cs program made these files
        for (final HashDataPair pair : loadData("com/microsoft/tfs/util/CLRHashUtilTestData-Guid.properties")) //$NON-NLS-1$
        {
            assertEquals(pair.hash, CLRHashUtil.getGUIDHash(new GUID(pair.string)));
        }
    }
}
