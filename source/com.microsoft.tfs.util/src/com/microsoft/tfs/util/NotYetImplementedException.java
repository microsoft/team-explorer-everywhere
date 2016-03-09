// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

/**
 * This exception exists so that we can code to it, but it searched on or
 * removed in the future, ensuring that we no longer have any areas of code that
 * we are expecting to implement.
 * <p>
 * It is expected that this will be removed before any public release.
 *
 * @threadsafety unknown
 */
public class NotYetImplementedException extends RuntimeException {

}
