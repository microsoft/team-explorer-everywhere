// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.BitSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.httpclient.URIException;
import com.microsoft.tfs.core.httpclient.util.URIUtil;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;

/**
 * Static utility methods for working with {@link URI}s.
 *
 * @see URI
 *
 */
public class URIUtils {
    private static final Log log = LogFactory.getLog(URIUtils.class);

    /**
     * A bit set passed to {@link URIUtil#encode(String, BitSet)} to encode only
     * the parts of query strings which have not already been encoded.
     *
     * @see #encodeQueryIgnoringPercentCharacters(String)
     */
    final static BitSet ALLOWED_QUERY_CHARS_WITH_PERCENT;

    static {
        ALLOWED_QUERY_CHARS_WITH_PERCENT = (BitSet) com.microsoft.tfs.core.httpclient.URI.allowed_query.clone();
        ALLOWED_QUERY_CHARS_WITH_PERCENT.set('%');
    }

    /**
     * A bit set passed to {@link URIUtil#encode(String, BitSet)} to encode only
     * the parts of paths which have not already been encoded.
     *
     * @see #combinePartiallyEncodedPaths(String, String)
     */
    final static BitSet ALLOWED_PATH_CHARS_WITH_PERCENT;

    static {
        ALLOWED_PATH_CHARS_WITH_PERCENT = (BitSet) com.microsoft.tfs.core.httpclient.URI.allowed_abs_path.clone();
        ALLOWED_PATH_CHARS_WITH_PERCENT.set('%');
    }

    public static final String VSTS_ROOT_URL_STRING = "https://app.vssps.visualstudio.com"; //$NON-NLS-1$
    public static final String VSTS_ROOT_SIGNIN_URL_STRING = "https://app.vssps.visualstudio.com/_signin"; //$NON-NLS-1$
    public static final String VSTS_ROOT_SIGNOUT_URL_STRING = "https://app.vssps.visualstudio.com/_signout"; //$NON-NLS-1$
    public static final String VSTS_SUFFIX = ".visualstudio.com"; //$NON-NLS-1$
    public static final String TFS_REALM_URL_STRING = "https://tfs.app.visualstudio.com"; //$NON-NLS-1$

    public static final URI VSTS_ROOT_URL = newURI(VSTS_ROOT_URL_STRING);
    public static final URI VSTS_ROOT_SIGNIN_URL = newURI(VSTS_ROOT_SIGNIN_URL_STRING);
    public static final URI VSTS_ROOT_SIGNOUT_URL = newURI(VSTS_ROOT_SIGNOUT_URL_STRING);

    /**
     * <p>
     * Ensures that the specified {@link URI}'s path component ends with a slash
     * character (<code>/</code>).
     * </p>
     *
     * <p>
     * {@link URI}s that will be resolved against should always have a trailing
     * slash in their path component. For more information, see Sun Java bug
     * 4666701 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4666701).
     * </p>
     *
     * <p>
     * If the specified {@link URI} is opaque, it is returned. If the specified
     * {@link URI} is hierarchical and its path component already ends in a
     * slash, it is returned. Otherwise, a new {@link URI} is returned that is
     * identical to the specified {@link URI} except in its path component,
     * which will have a slash appended on.
     * </p>
     *
     * @param uri
     *        a {@link URI} to check (must not be <code>null</code>)
     * @return a {@link URI} as described above (never <code>null</code>)
     */
    public static URI ensurePathHasTrailingSlash(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        if (uri.isOpaque()) {
            return uri;
        }

        String path = uri.getPath();
        if (path != null && path.endsWith("/")) //$NON-NLS-1$
        {
            return uri;
        }

        if (path == null) {
            path = "/"; //$NON-NLS-1$
        } else {
            path = path + "/"; //$NON-NLS-1$
        }

        return newURI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
    }

    /**
     * <p>
     * Ensures that all the components of the {@link URI} are in lower-case.
     * </p>
     *
     * <p>
     * If the specified {@link URI} is opaque, it is returned. Otherwise, a new
     * {@link URI} is returned that is identical to the specified {@link URI}
     * except that the components (scheme, hostname, path) are converted to
     * their lower case equivalents (in a generic locale.)
     * </p>
     *
     * @param uri
     *        a {@link URI} to check (must not be <code>null</code>)
     * @return a {@link URI} as described above (never <code>null</code>)
     */
    public static URI toLowerCase(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        if (uri.isOpaque()) {
            return uri;
        }

        final String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(LocaleUtil.ROOT) : null;
        final String authority = uri.getAuthority() != null ? uri.getAuthority().toLowerCase(LocaleUtil.ROOT) : null;
        final String path = uri.getPath() != null ? uri.getPath().toLowerCase(LocaleUtil.ROOT) : null;
        final String query = uri.getQuery() != null ? uri.getQuery().toLowerCase(LocaleUtil.ROOT) : null;
        final String fragment = uri.getFragment() != null ? uri.getFragment().toLowerCase(LocaleUtil.ROOT) : null;

        return newURI(scheme, authority, path, query, fragment);
    }

    /**
     * Ensures that the specified {@link URI} has any trailing slashes REMOVED.
     * VisualStudio uses server URIs that lack trailing slashes, this is for
     * compatibility. However, a path of only / will be maintained.
     *
     * @param uri
     *        a {@link URI} to check (must not be <code>null</code>)
     * @return a {@link URI} as described above (never <code>null</code>)
     */
    public static URI removeTrailingSlash(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        if (uri.isOpaque()) {
            return uri;
        }

        String path = uri.getPath();

        if (path == null) {
            path = "/"; //$NON-NLS-1$
        } else if (!path.equals("/")) //$NON-NLS-1$
        {
            while (path.endsWith("/")) //$NON-NLS-1$
            {
                path = path.substring(0, path.length() - 1);
            }
        }

        return newURI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
    }

    /**
     * Returns a new {@link URI} containing only the scheme, user info, host,
     * port, and path of the given {@link URI}.
     *
     * @param uri
     *        the {@link URI} to remove the query parts from (must not be
     *        <code>null</code>)
     * @return a new {@link URI} containing only the scheme, user info, host,
     *         port, and path of the given {@link URI}
     */
    public static URI removeQueryParts(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, null);
        } catch (final URISyntaxException e) {
            final IllegalArgumentException e2 = new IllegalArgumentException(
                MessageFormat.format(Messages.getString("URIUtils.IllegalURIFormat"), uri)); //$NON-NLS-1$
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * Returns a new {@link URI} containing only the scheme, user info, host,
     * and port of the given {@link URI}.
     *
     * @param uri
     *        the {@link URI} to remove the path and query parts from (must not
     *        be <code>null</code>)
     * @return a new {@link URI} containing only the scheme, user info, host,
     *         and port of the given {@link URI}
     */
    public static URI removePathAndQueryParts(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        try {
            return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
        } catch (final URISyntaxException e) {
            final IllegalArgumentException e2 = new IllegalArgumentException(
                MessageFormat.format(Messages.getString("URIUtils.IllegalURIFormat"), uri)); //$NON-NLS-1$
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * <p>
     * Constructs a new {@link URI} from a string representation of the URI. Any
     * characters in the string that are illegal for URIs will be quoted. This
     * method is an alternative to the single-argument {@link URI} constructor,
     * which requires that all illegal characters must already be quoted.
     * </p>
     *
     * <p>
     * For example, the following code fails with a {@link URISyntaxException}
     * because the path component contains a space character, which is illegal:
     *
     * <pre>
     * URI uri = new URI(&quot;http://example.com/path/a file.txt&quot;);
     * </pre>
     *
     * Instead, do this:
     *
     * <pre>
     * URI uri = URIUtils.newURI(&quot;http://example.com/path/a file.txt&quot;);
     * </pre>
     *
     * </p>
     *
     * @param uri
     *        the {@link String} representation of the {@link URI} (must not be
     *        <code>null</code>)
     * @return a new {@link URI} (never <code>null</code>)
     */
    public static URI newURI(final String uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        /*
         * The URI string could be already encoded. Let's try first just to
         * create an URI object from it.
         */
        try {
            return new URI(uri);
        } catch (final URISyntaxException e) {
            /*
             * Ignore an error, it could be because the URI string either is not
             * encoded or malformed. Try to encode the string, and create a URI
             * object again.
             */
        }

        /*
         * While encoding the URI string, we should take in account that it
         * might contain many parts with their specific encoding rules.
         *
         * Let's use our own elaborated URI class from the HTTP client for that.
         */
        final com.microsoft.tfs.core.httpclient.URI encodedUri;
        try {
            encodedUri = new com.microsoft.tfs.core.httpclient.URI(uri, false);
        } catch (final URIException e) {
            final IllegalArgumentException e2 = new IllegalArgumentException(
                MessageFormat.format(Messages.getString("URIUtils.IllegalURIFormat"), uri)); //$NON-NLS-1$
            e2.initCause(e);
            throw e2;
        }

        /*
         * Now we have correctly encoded URI, but still its syntax could be
         * wrong.
         */
        try {
            return new URI(encodedUri.getEscapedURIReference());
        } catch (final URISyntaxException e) {
            final IllegalArgumentException e2 = new IllegalArgumentException(
                MessageFormat.format(Messages.getString("URIUtils.IllegalURIFormat"), uri)); //$NON-NLS-1$
            e2.initCause(e);
            throw e2;
        }
    }

    /**
     * <p>
     * Constructs a new {@link URI} from the string components of the URI. Any
     * characters in the components that are illegal for URIs will be quoted.
     * This method is an alternative to the corresponding {@link URI}
     * constructor which does encode some characters, but not all necessary
     * characters like the {@link URIUtil} class does (some RTL Unicode
     * characters remain unescaped in the {@link URI} constructor which cause
     * problems in HTTP requests).
     * </p>
     *
     * @param scheme
     *        Unencoded URI scheme
     *
     * @param authority
     *        Unencoded URI authority
     *
     * @param path
     *        Unencoded URI path
     *
     * @param query
     *        Unencoded URI query
     *
     * @param fragment
     *        Unencoded URI fragment
     * @return the new {@link URI}
     */
    public static URI newURI(
        final String scheme,
        final String authority,
        final String path,
        final String query,
        final String fragment) {
        final StringBuffer sb = new StringBuffer();

        try {
            if (scheme != null) {
                sb.append(scheme);
                sb.append(':');
            }

            if (authority != null) {
                sb.append("//"); //$NON-NLS-1$
                sb.append(URIUtil.encode(authority, com.microsoft.tfs.core.httpclient.URI.allowed_authority));
            }

            if (path != null) {
                sb.append(URIUtil.encodePath(path));
            }

            if (query != null) {
                sb.append('?');
                sb.append(URIUtil.encodeQuery(query));
            }

            if (fragment != null) {
                sb.append('#');
                sb.append(URIUtil.encode(fragment, com.microsoft.tfs.core.httpclient.URI.allowed_fragment));
            }
        } catch (final URIException e) {
            final IllegalArgumentException e2 =
                new IllegalArgumentException(MessageFormat.format(
                    Messages.getString("URIUtils.IllegalURIPartsFormat"), //$NON-NLS-1$
                    scheme,
                    authority,
                    path,
                    query,
                    fragment));
            e2.initCause(e);
            throw e2;
        }

        return URI.create(sb.toString());
    }

    public static URI newURI(final String scheme, final String authority, final String path, final String query) {
        return newURI(scheme, authority, path, query, null);
    }

    public static URI newURI(final String scheme, final String authority, final String path) {
        return newURI(scheme, authority, path, null, null);
    }

    public static URI newURI(final String scheme, final String authority) {
        return newURI(scheme, authority, null, null, null);
    }

    /**
     * <p>
     * Appends the specified parent {@link URI} with provided query parameters.
     * </p>
     *
     * <p>
     * Old query parameters and/or fragments in the specified parent {@link URI}
     * (if any) are discarded.
     * </p>
     *
     * @param parent
     *        a base {@link URI} (must not be <code>null</code>)
     * @return a new {@link URI} as described above (never <code>null</code>)
     */
    public static URI addQueryParameters(final URI parent, final Map<String, String> queryParameters) {
        if (queryParameters == null || queryParameters.size() == 0) {
            return parent;
        }

        // Make sure we use a well escaped URI string for a base.
        final StringBuilder sb = new StringBuilder(URIUtils.removeQueryParts(parent).toASCIIString());

        boolean isFirstParameter = true;

        for (final Entry<String, String> queryParameter : queryParameters.entrySet()) {
            if (isFirstParameter) {
                sb.append("?"); //$NON-NLS-1$
                isFirstParameter = false;
            } else {
                sb.append("&"); //$NON-NLS-1$
            }

            try {
                sb.append(URIUtil.encodeWithinQuery(queryParameter.getKey()));
                sb.append("="); //$NON-NLS-1$
                sb.append(URIUtil.encodeWithinQuery(queryParameter.getValue()));
            } catch (final URIException e) {
                final IllegalArgumentException e2 = new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("URIUtils.IllegalQueryParameterFormat"), //$NON-NLS-1$
                        queryParameter.getKey(),
                        queryParameter.getValue()));
                e2.initCause(e);
                throw e2;
            }
        }

        return newURI(sb.toString());
    }

    /**
     * <p>
     * Resolves the specified child {@link URI} against the specified
     * {@link URI}, returning a {@link URI} as the result.
     * </p>
     *
     * <p>
     * This method behaves differently than calling {@link URI#resolve(URI)}.
     * This method first uses the {@link #ensurePathHasTrailingSlash(URI)}
     * method to ensure that the specified parent {@link URI}'s path has a
     * trailing slash. See the documentation of
     * {@link #ensurePathHasTrailingSlash(URI)} method for a reason why this is
     * necessary.
     * </p>
     *
     * @param parent
     *        a base {@link URI} to resolve against (must not be
     *        <code>null</code>)
     * @param child
     *        a relative path to resolve (must not be <code>null</code>)
     * @return a new {@link URI} as described above (never <code>null</code>)
     */
    public static URI resolve(URI parent, final URI child) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$
        Check.notNull(child, "child"); //$NON-NLS-1$

        if (!child.isAbsolute() && !parent.isOpaque()) {
            parent = ensurePathHasTrailingSlash(parent);
        }

        return parent.resolve(child);
    }

    /**
     * <p>
     * Resolves the specified child string against the specified parent URI,
     * returning a new {@link URI} as the result.
     * </p>
     *
     * <p>
     * This method behaves differently than creating a new {@link URI} and
     * calling {@link URI#resolve(String)}. It first creates intermediate parent
     * and child {@link URI}s using the {@link #newURI(String)} method. Doing
     * this ensures that any characters in the specified parent and child
     * strings that are not legal {@link URI} characters are properly quoted.
     * This method then uses the {@link #ensurePathHasTrailingSlash(URI)} method
     * to ensure that the specified parent {@link URI}'s path has a trailing
     * slash. See the documentation of {@link #ensurePathHasTrailingSlash(URI)}
     * method for a reason why this is necessary.
     * </p>
     *
     * @throws IllegalArgumentException
     *         if the URI string computed from the given child string violates
     *         RFC 2396
     *
     * @param parent
     *        a base {@link URI} to resolve against (must not be
     *        <code>null</code>)
     * @param child
     *        a relative path to resolve (must not be <code>null</code>)
     * @return a new {@link URI} as described above (never <code>null</code>)
     */
    public static URI resolve(final String parent, final String child) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$
        Check.notNull(child, "child"); //$NON-NLS-1$

        final URI parentUri = newURI(parent);
        final URI childUri = newURI(child);

        return resolve(parentUri, childUri);
    }

    /**
     * <p>
     * Resolves the specified child string against the specified {@link URI},
     * returning a new {@link URI} as the result.
     * </p>
     *
     * <p>
     * This method behaves differently than calling {@link URI#resolve(String)}.
     * It first creates an intermediate child {@link URI} using the
     * {@link #newURI(String)} method. Doing this ensures that any characters in
     * the specified child URI that are not legal {@link URI} characters are
     * properly quoted. This method then uses the
     * {@link #ensurePathHasTrailingSlash(URI)} method to ensure that the
     * specified parent {@link URI}'s path has a trailing slash. See the
     * documentation of {@link #ensurePathHasTrailingSlash(URI)} method for a
     * reason why this is necessary.
     * </p>
     *
     * @throws IllegalArgumentException
     *         if the URI string computed from the given child string violates
     *         RFC 2396
     *
     * @param parent
     *        a base {@link URI} to resolve against (must not be
     *        <code>null</code>)
     * @param child
     *        a relative path to resolve (must not be <code>null</code>)
     * @return a new {@link URI} as described above (never <code>null</code>)
     */
    public static URI resolve(final URI parent, final String child) {
        Check.notNull(parent, "parent"); //$NON-NLS-1$
        Check.notNull(child, "child"); //$NON-NLS-1$

        final URI childUri = newURI(child);

        return resolve(parent, childUri);
    }

    /**
     * <p>
     * If a {@link URI} is successfully constructed but contains invalid
     * characters in the hostname (notably, underscores), the
     * {@link URI#getHost()} method returns {@link <code>null</code>}. This
     * method first gets the host using that method. If <code>null</code> is
     * returned, a best effort to get the original host (if any) is made by
     * converting the {@link URI} to a {@link URL} and calling
     * {@link URL#getHost()}.
     * </p>
     *
     * <p>
     * This method should only be used in the specific case when you want to try
     * to detect and handle the above condition. For most cases, underscores in
     * hostnames should correctly be treated as errors, and this method should
     * not be used.
     * </p>
     *
     * @param uri
     *        the {@link URI} to get the host name for (must not be
     *        <code>null</code>)
     * @return the host name of the {@link URI}, or <code>null</code> if the
     *         host name was not defined
     */
    public static String safeGetHost(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        String host = uri.getHost();

        if (host == null) {
            try {
                host = uri.toURL().getHost();
            } catch (final MalformedURLException e) {
                // ignore
            }
        }

        return host;
    }

    /**
     * Combine two partial URI paths to create a single path.
     *
     * @param path1
     *        The path prefix
     *
     * @param path2
     *        A relative path.
     *
     * @return The concatenated string with and leading slashes or tildes
     *         removed from the second string.
     */
    public static String combinePaths(final String path1, final String path2) {
        return combinePaths(path1, path2, false);
    }

    /**
     * Combine two partial URI paths to create a single path.
     *
     * @param path1
     *        The path prefix
     *
     * @param path2
     *        A relative path.
     *
     * @param encodeRelativePath
     *        If true, URI-encode the relative path (path2) before appending
     *
     * @return The concatenated string with and leading slashes or tildes
     *         removed from the second string.
     */
    public static String combinePaths(final String path1, String path2, final boolean encodeRelativePath) {
        if (encodeRelativePath && path2 != null) {
            try {
                path2 = URIUtil.encodePath(path2);
            } catch (final URIException e) {
                final IllegalArgumentException e2 = new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("URIUtils.IllegalURIPathFormat"), //$NON-NLS-1$
                        path2));
                e2.initCause(e);
                throw e2;
            }
        }

        if (path1 == null || path1.length() == 0) {
            return path2;
        }

        if (path2 == null || path2.length() == 0) {
            return path1;
        }

        final char separator = (path1.indexOf("/") >= 0) ? '/' : '\\'; //$NON-NLS-1$

        final char[] trimChars = new char[] {
            '\\',
            '/'
        };

        return StringUtil.trimEnd(path1, trimChars) + separator + StringUtil.trimBegin(path2, trimChars);
    }

    /**
     * Combine two partial URI paths to create a single path. The second part of
     * the path might already be encoded, or partially encoded. The method
     * encodes all characters that would normally need encoded in a path
     * <b>except</b> for the percent sign (which is left alone).
     *
     * @param path1
     *        The path prefix
     *
     * @param path2
     *        A relative path.
     *
     * @return The concatenated string with leading slashes or tildes removed
     *         from the second string.
     */
    public static String combinePartiallyEncodedPaths(final String path1, String path2) {
        if (path2 != null) {
            try {
                path2 = URIUtil.encode(path2, ALLOWED_PATH_CHARS_WITH_PERCENT);
            } catch (final URIException e) {
                final IllegalArgumentException e2 = new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("URIUtils.IllegalURIPathFormat"), //$NON-NLS-1$
                        path2));
                e2.initCause(e);
                throw e2;
            }
        }

        return combinePaths(path1, path2);
    }

    /**
     * Encodes a URI query string which might already be encoded, or partially
     * encoded, by encoding all characters that would normally need encoded in a
     * query string <b>except</b> for the percent sign (which is left aloen).
     * This means that if your goal is to encode strings that might have percent
     * signs which <em>do</em> need encoded, this method is not for you. This
     * method is for a very limited set of encoding cases (specifically, TFS
     * download URLs which are partially encoded but will never have percent
     * signs in the parts which do need encoded).
     *
     * @param partiallyEncoded
     *        the URL query string which may already have some parts encoded
     *        with percent signs, but needs other parts encoded (not encoding
     *        any percent signs that might be in that part)
     * @return the encoded string
     */
    public static String encodeQueryIgnoringPercentCharacters(final String partiallyEncoded) {
        Check.notNull(partiallyEncoded, "partiallyEncoded"); //$NON-NLS-1$

        try {
            return URIUtil.encode(partiallyEncoded, ALLOWED_QUERY_CHARS_WITH_PERCENT);
        } catch (final URIException e) {
            throw new IllegalArgumentException(
                MessageFormat.format(Messages.getString("URIUtils.IllegalURIQueryStringFormat"), partiallyEncoded)); //$NON-NLS-1$
        }
    }

    /**
     * Returns a {@link URI} string for display to the user, decoding escaped
     * ASCII characters into Unicode. If the decoding fails (invalid escaped
     * characters?) the URI's toString() value is returned instead.
     *
     * @param uri
     *        the URI to get the display string for (must not be
     *        <code>null</code>)
     * @return the decoded URI string
     */
    public static String decodeForDisplay(final URI uri) {
        Check.notNull(uri, "uri"); //$NON-NLS-1$

        try {
            return URIUtil.decode(uri.toASCIIString());
        } catch (final URIException e) {
            log.warn(MessageFormat.format(
                "Couldn''t decode URI [{0}] display string, returning raw instead", //$NON-NLS-1$
                uri.toASCIIString()), e);

            return uri.toString();
        }
    }

    /**
     * Returns a {@link URI} string string for display to the user, decoding
     * escaped ASCII characters into Unicode. If the decoding fails (invalid
     * escaped characters?) the given string is returned instead.
     *
     * @param asciiURIString
     *        the URI String to get the display string for (must not be
     *        <code>null</code>)
     * @return the decoded URI string
     */
    public static String decodeForDisplay(final String asciiURIString) {
        Check.notNull(asciiURIString, "asciiURIString"); //$NON-NLS-1$

        try {
            return URIUtil.decode(asciiURIString);
        } catch (final URIException e) {
            log.warn(MessageFormat.format(
                "Couldn''t decode URI [{0}] display string, returning raw instead", //$NON-NLS-1$
                asciiURIString), e);

            return asciiURIString;
        }
    }
}
