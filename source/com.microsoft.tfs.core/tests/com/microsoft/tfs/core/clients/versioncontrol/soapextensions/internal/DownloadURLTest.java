// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal;

import junit.framework.TestCase;

public class DownloadURLTest extends TestCase {
    // Even though it's called DownloadURL, the class only parses the query
    // string part of a TFS download URL. The VS tests test mostly compression,
    // which our implementation doesn't do. These tests test parsing.

    public void testNull() {
        assertEquals(null, new DownloadURL(null).getURL());
        assertEquals(false, new DownloadURL(null).isContentDestroyed());
    }

    public void testEmpty() {
        assertEquals("", new DownloadURL("").getURL()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals(false, new DownloadURL("").isContentDestroyed()); //$NON-NLS-1$
    }

    public void testGetURL() {
        // These realistic looking strings come from the VS test
        // SuiteSrc/VC/Tests/UnitTests/DownloadUrlTests.cs

        final String string1 =
            "type=rsa&sfid=1034,1030,33035,1029,1039,1027,1028,1031,1035,1036&ts=633956267905131317&s=TA778QyAuo9DbyqeA2WFoi2Cj4CdF77Jb3UDoGAzHGkSfo9UT1pXf8AWWxlgFWarZWLMk1BzlXCNWEhMyXWQ53bYTAl%2BOCUni245dA5ymgZ65ucd9KTq%2FEWmlHIPkU92te8VkwQZO6TWLaQcsr12lTBDt4Dz3I%2Biyp2Z6lo1K14%3D&fid=1034&iid=77ba4d8e-ffd9-487a-bb4b-814144308d26&cp=/tfs/Collection0/"; //$NON-NLS-1$
        final String string2 =
            "type=rsa&sfid=1034,1030,33035,1029,1039,1027,1028,1031,1035,1036&ts=633956267905131317&s=TA778QyAuo9DbyqeA2WFoi2Cj4CdF77Jb3UDoGAzHGkSfo9UT1pXf8AWWxlgFWarZWLMk1BzlXCNWEhMyXWQ53bYTAl%2BOCUni245dA5ymgZ65ucd9KTq%2FEWmlHIPkU92te8VkwQZO6TWLaQcsr12lTBDt4Dz3I%2Biyp2Z6lo1K14%3D&fid=1030&iid=77ba4d8e-ffd9-487a-bb4b-814144308d26&cp=/tfs/Collection0/"; //$NON-NLS-1$

        assertEquals(string1, new DownloadURL(string1).getURL());
        assertEquals(false, new DownloadURL(string1).isContentDestroyed());

        assertEquals(string2, new DownloadURL(string2).getURL());
        assertEquals(false, new DownloadURL(string2).isContentDestroyed());
    }

    public void testGetEmptyFileID() {
        assertEquals(0, new DownloadURL("fid=").getFileID()); //$NON-NLS-1$
        assertEquals(0, new DownloadURL("fid=&a=b").getFileID()); //$NON-NLS-1$
        assertEquals(0, new DownloadURL("a=b&fid=").getFileID()); //$NON-NLS-1$
        assertEquals(0, new DownloadURL("a=b&fid=&c=d").getFileID()); //$NON-NLS-1$
    }

    public void testGetIntegerFileID() {
        assertEquals(0, new DownloadURL("fid=0").getFileID()); //$NON-NLS-1$
        assertEquals(0, new DownloadURL("fid=0&a=b").getFileID()); //$NON-NLS-1$
        assertEquals(0, new DownloadURL("a=b&fid=0").getFileID()); //$NON-NLS-1$
        assertEquals(0, new DownloadURL("a=b&fid=0&c=d").getFileID()); //$NON-NLS-1$

        assertEquals(8888, new DownloadURL("fid=8888").getFileID()); //$NON-NLS-1$
        assertEquals(8888, new DownloadURL("fid=8888&a=b").getFileID()); //$NON-NLS-1$
        assertEquals(8888, new DownloadURL("a=b&fid=8888").getFileID()); //$NON-NLS-1$
        assertEquals(8888, new DownloadURL("a=b&fid=8888&c=d").getFileID()); //$NON-NLS-1$

        assertEquals(Integer.MIN_VALUE, new DownloadURL("fid=-2147483648").getFileID()); //$NON-NLS-1$
        assertEquals(Integer.MIN_VALUE, new DownloadURL("fid=-2147483648&a=b").getFileID()); //$NON-NLS-1$
        assertEquals(Integer.MIN_VALUE, new DownloadURL("a=b&fid=-2147483648").getFileID()); //$NON-NLS-1$
        assertEquals(Integer.MIN_VALUE, new DownloadURL("a=b&fid=-2147483648&c=d").getFileID()); //$NON-NLS-1$

        assertEquals(Integer.MAX_VALUE, new DownloadURL("fid=2147483647").getFileID()); //$NON-NLS-1$
        assertEquals(Integer.MAX_VALUE, new DownloadURL("fid=2147483647&a=b").getFileID()); //$NON-NLS-1$
        assertEquals(Integer.MAX_VALUE, new DownloadURL("a=b&fid=2147483647").getFileID()); //$NON-NLS-1$
        assertEquals(Integer.MAX_VALUE, new DownloadURL("a=b&fid=2147483647&c=d").getFileID()); //$NON-NLS-1$
    }

    public void testIsContentDestroyed() {
        assertEquals(false, new DownloadURL(null).isContentDestroyed());
        assertEquals(false, new DownloadURL("").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(false, new DownloadURL("a=b").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(false, new DownloadURL("fid=123").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(false, new DownloadURL("fid=123&a=b").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(false, new DownloadURL("a=b&fid=123").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(false, new DownloadURL("a=b&fid=123&c=d").isContentDestroyed()); //$NON-NLS-1$

        // 1023 is the magic number
        assertEquals(true, new DownloadURL("fid=1023").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(true, new DownloadURL("fid=1023&a=b").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(true, new DownloadURL("a=b&fid=1023").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(true, new DownloadURL("a=b&fid=1023&c=d").isContentDestroyed()); //$NON-NLS-1$
        assertEquals(
            true,
            new DownloadURL(
                "type=rsa&sfid=1034,1030,33035,1029,1039,1027,1028,1031,1035,1036&ts=633956267905131317&s=TA778QyAuo9DbyqeA2WFoi2Cj4CdF77Jb3UDoGAzHGkSfo9UT1pXf8AWWxlgFWarZWLMk1BzlXCNWEhMyXWQ53bYTAl%2BOCUni245dA5ymgZ65ucd9KTq%2FEWmlHIPkU92te8VkwQZO6TWLaQcsr12lTBDt4Dz3I%2Biyp2Z6lo1K14%3D&fid=1023&iid=77ba4d8e-ffd9-487a-bb4b-814144308d26&cp=/tfs/Collection0/").isContentDestroyed()); //$NON-NLS-1$

    }
}
