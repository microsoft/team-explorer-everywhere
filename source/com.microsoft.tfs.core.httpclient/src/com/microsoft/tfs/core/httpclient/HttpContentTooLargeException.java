// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.httpclient;

/**
 * Signals that the response content was larger than anticipated.
 *
 * @author Ortwin Gl?ck
 */
public class HttpContentTooLargeException extends HttpException {
    private final int maxlen;

    public HttpContentTooLargeException(final String message, final int maxlen) {
        super(message);
        this.maxlen = maxlen;
    }

    /**
     * @return the maximum anticipated content length in bytes.
     */
    public int getMaxLength() {
        return maxlen;
    }
}
