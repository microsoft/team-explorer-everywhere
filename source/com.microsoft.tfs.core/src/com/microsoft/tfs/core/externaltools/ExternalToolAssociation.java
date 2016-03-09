// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

/**
 * Associates one or more file extension strings with an {@link ExternalTool}.
 * Extensions always have whitespace trimmed and are compared case-insensitive.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class ExternalToolAssociation {
    private final Set<String> extensions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private ExternalTool tool;

    /**
     * The name for each child memento that represents an extension.
     */
    private static final String EXTENSION_MEMENTO_NAME = "extension"; //$NON-NLS-1$

    /**
     * The name for each child memento that represents an tool.
     */
    private static final String TOOL_MEMENTO_NAME = "tool"; //$NON-NLS-1$

    /**
     * Creates a {@link ExternalToolAssociation} that associates the given
     * extensions with the given tool.
     *
     * @param extensions
     *        the extension (may be <code>null</code> or empty; elements not
     *        <code>null</code>, or empty string, or all whitespace)
     * @param tool
     *        the tool (may be null)
     */
    public ExternalToolAssociation(final String[] extensions, final ExternalTool tool) {
        synchronized (this) {
            if (extensions != null) {
                putExtensions(extensions);
            }

            this.tool = tool;
        }
    }

    /**
     * Adds an extension to the set of file extensions, if it did not already
     * exist in the set. Extensions are stored case-insensitive.
     *
     * @param extension
     *        the extension to add (not <code>null</code>, not empty, not all
     *        whitespace)
     * @return true if the extension set did not already contain the extension,
     *         false if it did already contain the extension
     */
    public synchronized boolean putExtension(String extension) {
        Check.notNull(extension, "extension"); //$NON-NLS-1$

        extension = extension.trim();
        Check.notEmpty(extension, "extension"); //$NON-NLS-1$

        return extensions.add(extension);
    }

    /**
     * Adds all the given file extensions, if they did not already exist in the
     * set.
     *
     * @see #putExtension(String)
     *
     * @param extensions
     *        the extensions to add (may be <code>null</code> or empty; elements
     *        not <code>null</code>, or empty string, or all whitespace)
     * @return true if any of the items were new to the set, false if they were
     *         all already in the set
     */
    public synchronized boolean putExtensions(final String[] extensions) {
        if (extensions == null) {
            return false;
        }

        boolean changed = false;

        for (int i = 0; i < extensions.length; i++) {
            if (putExtension(extensions[i])) {
                changed = true;
            }
        }

        return changed;
    }

    /**
     * @return the extensions in this association, never null but may be empty
     */
    public synchronized String[] getExtensions() {
        return extensions.toArray(new String[extensions.size()]);
    }

    /**
     * Clears the extensions in this association.
     */
    public synchronized void clearExtensions() {
        extensions.clear();
    }

    /**
     * Sets the {@link ExternalTool}.
     *
     * @param tool
     *        the {@link ExternalTool}, may be null
     */
    public synchronized void setTool(final ExternalTool tool) {
        this.tool = tool;
    }

    /**
     * @return this association's {@link ExternalTool}, may be null
     */
    public synchronized ExternalTool getTool() {
        return tool;
    }

    /**
     * Tests whether this {@link ExternalToolAssociation} contains the given
     * extension.
     *
     * @param fileExtension
     *        the file extension (must not be <code>null</code>)
     * @return true if this extension exists in this association, false if it
     *         does not
     */
    public synchronized boolean containsExtension(final String fileExtension) {
        Check.notNull(fileExtension, "fileExtension"); //$NON-NLS-1$

        /*
         * This will use the case-insensitive search we configured the set with.
         */
        return extensions.contains(fileExtension);
    }

    /**
     * Saves this association's state to the given {@link Memento}, which should
     * have a name (of the caller's choice) but no other data.
     *
     * @param memento
     *        the {@link Memento} to save this association's state to (must not
     *        be <code>null</code>)
     */
    public synchronized void saveToMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        /*
         * We can add many children with the same name.
         */
        for (final Iterator<String> iterator = extensions.iterator(); iterator.hasNext();) {
            final String extension = iterator.next();

            final Memento extensionMemento = memento.createChild(EXTENSION_MEMENTO_NAME);
            extensionMemento.putTextData(extension);
        }

        if (tool != null) {
            final Memento toolMemento = memento.createChild(TOOL_MEMENTO_NAME);
            tool.saveToMemento(toolMemento);
        }
    }

    /**
     * Loads association state from the given {@link Memento}, which can have
     * any name.
     *
     * @param memento
     *        the {@link Memento} to load state from (must not be
     *        <code>null</code>)
     */
    public static ExternalToolAssociation loadFromMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        final List<String> extensions = new ArrayList<String>();
        final Memento[] extensionsChildren = memento.getChildren(EXTENSION_MEMENTO_NAME);

        for (int i = 0; i < extensionsChildren.length; i++) {
            extensions.add(extensionsChildren[i].getTextData());
        }

        ExternalTool tool = null;
        if (memento.getChild("tool") != null) //$NON-NLS-1$
        {
            tool = ExternalTool.loadFromMemento(memento.getChild(TOOL_MEMENTO_NAME));
        }

        return new ExternalToolAssociation(extensions.toArray(new String[extensions.size()]), tool);
    }
}
