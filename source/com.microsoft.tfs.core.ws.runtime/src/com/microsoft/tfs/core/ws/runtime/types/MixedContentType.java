// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

/**
 * A container for XSD's "any" type, which can hold arbitrary XML data (elements
 * and inner text) that does not have to follow any schema.
 * <p>
 * In the future, support for element text may be added.
 */
public interface MixedContentType extends AnyContentType {
}