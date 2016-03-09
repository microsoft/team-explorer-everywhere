// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;

/**
 * {@link CompatibilityLinkControl} wraps the link control returned by the
 * {@link CompatibilityLinkFactory}. The wrapper provides access to services
 * that are common between legacy compatibility links and the native link widget
 * introduced in SWT 3.1.
 *
 * @see CompatibilityLinkFactory
 */
public interface CompatibilityLinkControl {
    /**
     * @return the underlying link control (never <code>null</code>)
     */
    Control getControl();

    /**
     * Adds a {@link SelectionListener} that will be notified when the link is
     * selected.
     *
     * @param listener
     *        a {@link SelectionListener} to add (must not be <code>null</code>)
     */
    void addSelectionListener(SelectionListener listener);

    /**
     * Removes a previously added {@link SelectionListener}.
     *
     * @param listener
     *        a {@link SelectionListener} to remove (must not be
     *        <code>null</code>)
     */
    void removeSelectionListener(SelectionListener listener);

    /**
     * Called to determine whether the link wrapped by this
     * {@link CompatibilityLinkControl} supports rich-style hyperlink text
     * (i.e., text containing anchor tags). If this method returns
     * <code>true</code>, the {@link #setText(String)} method can be called and
     * passed such rich text. If <code>false</code>, hyperlink text will be
     * treated the same as normal text by the underlying link.
     *
     * @return <code>true</code> if hyperlink text is supported
     */
    boolean isHyperlinkTextSupported();

    /**
     * Sets the underlying link's text. If the link supports hyperlink text (
     * {@link #isHyperlinkTextSupported()}) then the text can contain embedded
     * hyperlinks (and must do so in order to create a selectable portion of the
     * link). Otherwise, the text should not contain embedded hyperlinks, and
     * the entire text will be selectable.
     *
     * @param text
     *        the link text to set (must not be <code>null</code>)
     */
    void setText(String text);

    /**
     * Sets simple text for the underlying link - this method works the same
     * whether or not the underlying link supports hyperlink text (
     * {@link #isHyperlinkTextSupported()}). If hyperlink text is supported, the
     * supplied text will be enclosed within an anchor tag. Otherwise, the text
     * will be used as-is.
     *
     * @param text
     *        the simple link text to set (must not be <code>null</code>)
     */
    void setSimpleText(String text);
}
