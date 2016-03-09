// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.ws.runtime.types;

import org.w3c.dom.Element;

/**
 * An implementation of {@link MixedContentType} where the elements are modeled
 * as DOM {@link Element}s.
 */
public abstract class DOMMixedContentType extends DOMAnyContentType implements MixedContentType {
    public DOMMixedContentType() {
        super();
    }

    /**
     * Creates a {@link DOMMixedContentType} with the given elements.
     *
     * @param elements
     *        the initial elements (not null, but may be empty)
     */
    public DOMMixedContentType(final Element[] elements) {
        super(elements);
    }
}
