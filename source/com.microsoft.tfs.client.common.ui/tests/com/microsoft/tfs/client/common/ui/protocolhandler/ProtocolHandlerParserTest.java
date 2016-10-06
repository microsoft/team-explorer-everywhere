// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.protocolhandler;

import org.junit.Test;

import junit.framework.TestCase;

public class ProtocolHandlerParserTest extends TestCase {

    @Test
    public void testProtocolHandler_ParseSimple_correct1() {
        final String vsoeclipseUrl = ProtocolHandlerTestData.getHandlerUrl(
            "http://arukhlin-dv1:8080/tfs/", //$NON-NLS-1$
            "http://arukhlin-dv1:8080/tfs/DefaultCollection/_git/gitTest_01", //$NON-NLS-1$
            "08f62406-7097-45f1-900b-f6283ae54a1a", //$NON-NLS-1$
            "gitTest_01", //$NON-NLS-1$
            "gitTest_01", //$NON-NLS-1$
            "master"); //$NON-NLS-1$
        final ProtocolHandler handler = new ProtocolHandler(vsoeclipseUrl);
        assertTrue(handler.hasProtocolHandlerRequest());
        assertEquals("master", handler.getProtocolHandlerBranch()); //$NON-NLS-1$
        assertEquals("master", handler.getProtocolHandlerBranchForHtml()); //$NON-NLS-1$
        assertEquals(
            "http://arukhlin-dv1:8080/tfs/DefaultCollection/_git/gitTest_01", //$NON-NLS-1$
            handler.getProtocolHandlerCloneUrl());
        assertEquals("08f62406-7097-45f1-900b-f6283ae54a1a", handler.getProtocolHandlerCollectionId()); //$NON-NLS-1$
        assertEquals("UTF8", handler.getProtocolHandlerEncoding()); //$NON-NLS-1$
        assertEquals("gitTest_01", handler.getProtocolHandlerProject()); //$NON-NLS-1$
        assertEquals("gitTest_01", handler.getProtocolHandlerRepository()); //$NON-NLS-1$
        assertEquals("gitTest_01", handler.getProtocolHandlerRepositoryForHtml()); //$NON-NLS-1$
        assertEquals("http://arukhlin-dv1:8080/tfs/", handler.getProtocolHandlerServerUrl()); //$NON-NLS-1$
    }

    @Test
    public void testProtocolHandler_ParseSimple_correct2() {
        final String vsoeclipseUrl = ProtocolHandlerTestData.getHandlerUrl(
            "http://arukhlin-dv1:8080/tfs/", //$NON-NLS-1$
            "http://arukhlin-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/gitTest_011", //$NON-NLS-1$
            "08f62406-7097-45f1-900b-f6283ae54a1a", //$NON-NLS-1$
            "gitTest_01", //$NON-NLS-1$
            "gitTest_011", //$NON-NLS-1$
            "master"); //$NON-NLS-1$
        final ProtocolHandler handler = new ProtocolHandler(vsoeclipseUrl);
        assertTrue(handler.hasProtocolHandlerRequest());
        assertEquals("master", handler.getProtocolHandlerBranch()); //$NON-NLS-1$
        assertEquals("master", handler.getProtocolHandlerBranchForHtml()); //$NON-NLS-1$
        assertEquals(
            "http://arukhlin-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/gitTest_011", //$NON-NLS-1$
            handler.getProtocolHandlerCloneUrl());
        assertEquals("08f62406-7097-45f1-900b-f6283ae54a1a", handler.getProtocolHandlerCollectionId()); //$NON-NLS-1$
        assertEquals("UTF8", handler.getProtocolHandlerEncoding()); //$NON-NLS-1$
        assertEquals("gitTest_01", handler.getProtocolHandlerProject()); //$NON-NLS-1$
        assertEquals("gitTest_011", handler.getProtocolHandlerRepository()); //$NON-NLS-1$
        assertEquals("gitTest_011", handler.getProtocolHandlerRepositoryForHtml()); //$NON-NLS-1$
        assertEquals("http://arukhlin-dv1:8080/tfs/", handler.getProtocolHandlerServerUrl()); //$NON-NLS-1$
    }

    @Test
    public void testProtocolHandler_ParseSimple_correct3() {
        final String vsoeclipseUrl = ProtocolHandlerTestData.getHandlerUrl(
            "http://arukhlin-dv1:8080/tfs/", //$NON-NLS-1$
            "http://arukhlin-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/%D1%80%D0%B5%D0%BF%D0%B0", //$NON-NLS-1$
            "b19afa93-8765-4506-9ddc-fbf6d62da591", //$NON-NLS-1$
            "gitTest_01", //$NON-NLS-1$
            "репа", //$NON-NLS-1$
            "<&M"); //$NON-NLS-1$
        final ProtocolHandler handler = new ProtocolHandler(vsoeclipseUrl);
        assertTrue(handler.hasProtocolHandlerRequest());
        assertEquals("<&M", handler.getProtocolHandlerBranch()); //$NON-NLS-1$
        assertEquals("&lt;&amp;M", handler.getProtocolHandlerBranchForHtml()); //$NON-NLS-1$
        assertEquals(
            "http://arukhlin-dv1:8080/tfs/DefaultCollection/gitTest_01/_git/%D1%80%D0%B5%D0%BF%D0%B0", //$NON-NLS-1$
            handler.getProtocolHandlerCloneUrl());
        assertEquals("b19afa93-8765-4506-9ddc-fbf6d62da591", handler.getProtocolHandlerCollectionId()); //$NON-NLS-1$
        assertEquals("UTF8", handler.getProtocolHandlerEncoding()); //$NON-NLS-1$
        assertEquals("gitTest_01", handler.getProtocolHandlerProject()); //$NON-NLS-1$
        assertEquals("репа", handler.getProtocolHandlerRepository()); //$NON-NLS-1$
        assertEquals("репа", handler.getProtocolHandlerRepositoryForHtml()); //$NON-NLS-1$
        assertEquals("http://arukhlin-dv1:8080/tfs/", handler.getProtocolHandlerServerUrl()); //$NON-NLS-1$
    }
}
