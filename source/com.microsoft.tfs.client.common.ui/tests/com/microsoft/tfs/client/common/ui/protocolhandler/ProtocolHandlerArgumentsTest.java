// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.protocolhandler;

import java.net.URI;

import org.junit.Test;

import com.microsoft.tfs.core.util.URIUtils;

import junit.framework.TestCase;

public class ProtocolHandlerArgumentsTest extends TestCase {

    @Test
    public void testProtocolHandler_FindHandlerArgument_correct1() {
        final String[] args = new String[] {
            "aaa", //$NON-NLS-1$
            "bbb=ccc", //$NON-NLS-1$
            "-clonefromtfs", //$NON-NLS-1$
            "vsoeclipse://some-url", //$NON-NLS-1$
            "xyz" //$NON-NLS-1$
        };

        final URI foundUrl = ProtocolHandler.findProtocolHandlerUriArgument(args);
        assertNotNull("found URI", foundUrl); //$NON-NLS-1$
        assertEquals(URIUtils.newURI("vsoeclipse://some-url"), foundUrl); //$NON-NLS-1$
    }

    @Test
    public void testProtocolHandler_FindHandlerArgument_correct2() {
        final String vsoeclipseUrl = ProtocolHandlerTestData.getHandlerUrl(
            "http://arukhlin-dv1:8080/tfs/", //$NON-NLS-1$
            "http://arukhlin-dv1:8080/tfs/DefaultCollection/_git/gitTest_01", //$NON-NLS-1$
            "08f62406-7097-45f1-900b-f6283ae54a1a", //$NON-NLS-1$
            "gitTest_01", //$NON-NLS-1$
            "gitTest_01", //$NON-NLS-1$
            "master"); //$NON-NLS-1$

        final String[] args = new String[] {
            "aaa", //$NON-NLS-1$
            "bbb=ccc", //$NON-NLS-1$
            "-clonefromtfs", //$NON-NLS-1$
            vsoeclipseUrl, //
            "xyz" //$NON-NLS-1$
        };

        final URI foundUrl = ProtocolHandler.findProtocolHandlerUriArgument(args);
        assertNotNull("found URI", foundUrl); //$NON-NLS-1$
        assertEquals(URIUtils.newURI(vsoeclipseUrl), foundUrl); // $NON-NLS-1$
    }

    @Test
    public void testProtocolHandler_FindHandlerArgument_incorrect1() {
        final String[] args = new String[] {
            "aaa", //$NON-NLS-1$
            "-clonefromtfs", //$NON-NLS-1$
            "bbb=ccc", //$NON-NLS-1$
            "vsoeclipse://some-url", //$NON-NLS-1$
            "xyz" //$NON-NLS-1$
        };

        final URI foundUrl = ProtocolHandler.findProtocolHandlerUriArgument(args);
        assertNull("found URI", foundUrl); //$NON-NLS-1$
    }

    @Test
    public void testProtocolHandler_FindHandlerArgument_incorrect2() {
        final String[] args = new String[] {
            "aaa", //$NON-NLS-1$
            "bbb=ccc", //$NON-NLS-1$
            "vsoeclipse://some-url", //$NON-NLS-1$
            "-clonefromtfs", //$NON-NLS-1$
            "xyz" //$NON-NLS-1$
        };

        final URI foundUrl = ProtocolHandler.findProtocolHandlerUriArgument(args);
        assertNull("found URI", foundUrl); //$NON-NLS-1$
    }

    @Test
    public void testProtocolHandler_FindHandlerArgument_incorrect3() {
        final String[] args = new String[] {
            "aaa", //$NON-NLS-1$
            "bbb=ccc", //$NON-NLS-1$
            "vsoeclipse://some-url", //$NON-NLS-1$
            "xyz" //$NON-NLS-1$
        };

        final URI foundUrl = ProtocolHandler.findProtocolHandlerUriArgument(args);
        assertNull("found URI", foundUrl); //$NON-NLS-1$
    }
}
