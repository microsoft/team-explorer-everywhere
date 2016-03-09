// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;

public abstract class WIFormElementImpl implements WIFormElement {
    private final List childElements = new ArrayList();
    private WIFormElementImpl parentElement;

    private final Map attributes = new HashMap();

    /**
     * Called when the start tag is parsed in the WIT type definition. The
     * attributes from the start tag are processed by this method. Each subclass
     * must implement this method.
     *
     * @param attributes
     *        The attributes from the start tag.
     */
    abstract void startLoading(Attributes attributes);

    /**
     * Called when the end tag is parsed in the WIT type definition. Override
     * this method in a subclass if the child elements need to be processed.
     */
    void endLoading() {
    }

    void addChildElement(final WIFormElementImpl child) {
        childElements.add(child);
        child.parentElement = this;
    }

    Object[] childrenToArray(final Object[] a) {
        return childElements.toArray(a);
    }

    void setAttributes(final Attributes parseAttributes) {
        final int len = parseAttributes.getLength();
        for (int i = 0; i < len; i++) {
            attributes.put(parseAttributes.getQName(i), parseAttributes.getValue(i));
        }
    }

    @Override
    public WIFormElement[] getChildElements() {
        return (WIFormElement[]) childElements.toArray(new WIFormElement[] {});
    }

    @Override
    public WIFormElement getParentElement() {
        return parentElement;
    }

    @Override
    public String[] getAttributeNames() {
        return (String[]) attributes.keySet().toArray(new String[attributes.size()]);
    }

    @Override
    public String getAttribute(final String name) {
        return (String) attributes.get(name);
    }
}
