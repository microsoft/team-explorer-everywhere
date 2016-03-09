// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.TestCase;

public class URIUtilsTest extends TestCase {
    public void testCreateAbsoluteWithIllegalCharacters() {
        final URI uri = URIUtils.newURI("http://example.com/path/a file.txt"); //$NON-NLS-1$
        assertEquals("http://example.com/path/a%20file.txt", uri.toString()); //$NON-NLS-1$
    }

    public void testCreateRelativeWithIllegalCharacters() {
        final URI uri = URIUtils.newURI("path/a file.txt"); //$NON-NLS-1$
        assertEquals("path/a%20file.txt", uri.toString()); //$NON-NLS-1$
    }

    public void testResolveStrings() {
        final URI uri = URIUtils.resolve("http://example.com/a path", "some file.txt"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("http://example.com/a%20path/some%20file.txt", uri.toString()); //$NON-NLS-1$
    }

    public void testResolveSlashCanonicalization() throws URISyntaxException {
        URI result = URIUtils.resolve(new URI("http://www.example.com"), "test"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("http://www.example.com/test", result.toString()); //$NON-NLS-1$

        result = URIUtils.resolve(new URI("http://www.example.com"), "/test"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("http://www.example.com/test", result.toString()); //$NON-NLS-1$

        result = URIUtils.resolve(new URI("http://www.example.com/"), "test"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("http://www.example.com/test", result.toString()); //$NON-NLS-1$

        result = URIUtils.resolve(new URI("http://www.example.com/"), "/test"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("http://www.example.com/test", result.toString()); //$NON-NLS-1$
    }

    public void testIllegalCharactersAreQuoted() throws URISyntaxException {
        final URI result = URIUtils.resolve(new URI("http://www.example.com"), "test path"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("http://www.example.com/test%20path", result.toString()); //$NON-NLS-1$
    }

    public void testResolveAbsoluteChild() throws URISyntaxException {
        final URI result = URIUtils.resolve(new URI("http://www.example.com"), "http://www.mydomain.com"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("http://www.mydomain.com", result.toString()); //$NON-NLS-1$
    }

    public void testResolveRelative() throws URISyntaxException {
        URI result = URIUtils.resolve(new URI("/path1"), "path2"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/path1/path2", result.toString()); //$NON-NLS-1$

        result = URIUtils.resolve(new URI("/path1/"), "path2"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/path1/path2", result.toString()); //$NON-NLS-1$

        result = URIUtils.resolve(new URI("/path1"), "/path2"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/path2", result.toString()); //$NON-NLS-1$

        result = URIUtils.resolve(new URI("path1"), "path2"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("path1/path2", result.toString()); //$NON-NLS-1$

        result = URIUtils.resolve(new URI("path1"), "/path2"); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("/path2", result.toString()); //$NON-NLS-1$
    }

    public void testEnsureTrailingSlash() {
        final String difficultString = "OK_2 (\ufa05!)"; //$NON-NLS-1$
        final String difficultStringEscaped = "OK_2%20(%EF%A8%85!)"; //$NON-NLS-1$

        String uriNoSlash = "http://HOST_Fun$()2:8080/" + difficultString + "?" + difficultString; //$NON-NLS-1$ //$NON-NLS-2$
        String expected = "http://HOST_Fun$()2:8080/" + difficultStringEscaped + "/?" + difficultStringEscaped; //$NON-NLS-1$ //$NON-NLS-2$
        URI result = URIUtils.ensurePathHasTrailingSlash(URIUtils.newURI(uriNoSlash));
        assertEquals(expected, result.toString());

        result = URIUtils.ensurePathHasTrailingSlash(result);
        assertEquals(expected, result.toString());

        uriNoSlash = "ftp://ok?a#fun"; //$NON-NLS-1$
        expected = "ftp://ok/?a#fun"; //$NON-NLS-1$
        result = URIUtils.ensurePathHasTrailingSlash(URI.create(uriNoSlash));
        assertEquals(expected, result.toString());

        uriNoSlash = "https://a123:123/1/2/3/4?a=b&c=d"; //$NON-NLS-1$
        expected = "https://a123:123/1/2/3/4/?a=b&c=d"; //$NON-NLS-1$
        result = URIUtils.ensurePathHasTrailingSlash(URI.create(uriNoSlash));
        assertEquals(expected, result.toString());
    }

    public void testRemoveTrailingSlash() {
        final String difficultString = "OK_2 (\ufa05!)"; //$NON-NLS-1$
        final String difficultStringEscaped = "OK_2%20(%EF%A8%85!)"; //$NON-NLS-1$

        String uriSlash = "http://HOST_Fun$()2:8080/" + difficultString + "/?" + difficultString; //$NON-NLS-1$ //$NON-NLS-2$
        String expected = "http://HOST_Fun$()2:8080/" + difficultStringEscaped + "?" + difficultStringEscaped; //$NON-NLS-1$ //$NON-NLS-2$
        URI result = URIUtils.removeTrailingSlash(URIUtils.newURI(uriSlash));
        assertEquals(expected, result.toString());

        result = URIUtils.removeTrailingSlash(result);
        assertEquals(expected, result.toString());

        uriSlash = "ftp://ok/?a#fun"; //$NON-NLS-1$
        expected = "ftp://ok/?a#fun"; //$NON-NLS-1$
        result = URIUtils.removeTrailingSlash(URI.create(uriSlash));
        assertEquals(expected, result.toString());

        uriSlash = "https://a123:123/1/2/3/4/?a=b&c=d"; //$NON-NLS-1$
        expected = "https://a123:123/1/2/3/4?a=b&c=d"; //$NON-NLS-1$
        result = URIUtils.removeTrailingSlash(URI.create(uriSlash));
        assertEquals(expected, result.toString());

        uriSlash = "http://fun:80/"; //$NON-NLS-1$
        expected = "http://fun:80/"; //$NON-NLS-1$
        result = URIUtils.removeTrailingSlash(URI.create(uriSlash));
        assertEquals(expected, result.toString());
    }

    public void testDecodeForDisplay() {
        /*
         * These tests are do not strictly test all URI encoding rules. Since
         * the method is for display, it just tests that encoded strings convert
         * nicely to something to display to the user.
         */

        final String encodedSpaceURIString = "http://host:1234/abc%20def"; //$NON-NLS-1$
        final String decodedSpaceURIString = "http://host:1234/abc def"; //$NON-NLS-1$

        assertEquals(decodedSpaceURIString, URIUtils.decodeForDisplay(encodedSpaceURIString));
        assertEquals(decodedSpaceURIString, URIUtils.decodeForDisplay(URIUtils.newURI(decodedSpaceURIString)));

        // "http://tfsxp-2010:8080/tfs/Российская Федерация/"
        final String encodedRussianFederationURIString =
            "http://tfsxp-2010:8080/tfs/%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D0%B9%D1%81%D0%BA%D0%B0%D1%8F%20%D0%A4%D0%B5%D0%B4%D0%B5%D1%80%D0%B0%D1%86%D0%B8%D1%8F/"; //$NON-NLS-1$
        final String decodedRussianFederationURIString =
            "http://tfsxp-2010:8080/tfs/\u0420\u043e\u0441\u0441\u0438\u0439\u0441\u043a\u0430\u044f \u0424\u0435\u0434\u0435\u0440\u0430\u0446\u0438\u044f/"; //$NON-NLS-1$

        assertEquals(decodedRussianFederationURIString, URIUtils.decodeForDisplay(encodedRussianFederationURIString));
        assertEquals(
            decodedRussianFederationURIString,
            URIUtils.decodeForDisplay(URIUtils.newURI(decodedRussianFederationURIString)));

        /*
         * "http://tfsxp-2010:8080/tfs/Ф" is not fully encoded. Decoding it
         * should result in "?" in place of the non-escaped character.
         */
        final String unicodeURIString = "http://tfsxp-2010:8080/tfs/\u0424"; //$NON-NLS-1$
        assertEquals("http://tfsxp-2010:8080/tfs/?", URIUtils.decodeForDisplay(unicodeURIString)); //$NON-NLS-1$

        /*
         * Constructing a URI from the Unicode string should work correctly.
         */
        assertEquals(unicodeURIString, URIUtils.decodeForDisplay(URIUtils.newURI(unicodeURIString)));
    }
}
