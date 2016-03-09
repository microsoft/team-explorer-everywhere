// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient.methods;

/**
 * Implements the HTTP PATCH method.
 * <p>
 * The HTTP PATCH method is defined in section 19.6.1.1 of
 * <a href="http://www.ietf.org/rfc/rfc2068.txt">RFC2068</a>: <blockquote> The
 * PATCH method is similar to PUT except that the entity contains a list of
 * differences between the original version of the resource identified by the
 * Request-URI and the desired content of the resource after the PATCH action
 * has been applied. </blockquote>
 * </p>
 */
public class PatchMethod extends PutMethod {

    // ----------------------------------------------------------- Constructors

    /**
     * No-arg constructor.
     */
    public PatchMethod() {
        super();
    }

    /**
     * Constructor specifying a URI.
     *
     * @param uri
     *        either an absolute or relative URI
     */
    public PatchMethod(final String uri) {
        super(uri);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Return <tt>"PATCH"</tt>.
     *
     * @return <tt>"PATCH"</tt>
     */
    @Override
    public String getName() {
        return "PATCH";
    }
}
