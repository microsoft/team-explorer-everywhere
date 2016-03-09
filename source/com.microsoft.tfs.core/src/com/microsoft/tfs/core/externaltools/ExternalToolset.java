// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * An ordered collection of {@link ExternalToolAssociation}s, with some handy
 * methods to find the association that's most appropriate for a given file or
 * directory name. Searches are linear from the start of the list; if an
 * {@link ExternalToolset} contains multiple {@link ExternalToolAssociation}s
 * that have overlapping extensions, the first in the set will match when the
 * set is searched for an extension.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public final class ExternalToolset {
    /**
     * The extension string to use to make a tool association for directories.
     */
    public static final String DIRECTORY_EXTENSION = "/"; //$NON-NLS-1$

    /**
     * The extension string to use to make a fall-back tool association, which
     * matches all file extensions as long as a more specific extension does not
     * exist in the set.
     */
    public static final String WILDCARD_EXTENSION = "*"; //$NON-NLS-1$

    /**
     * The name for each child memento that represents an association.
     */
    private static final String ASSOCIATION_MEMENTO_NAME = "association"; //$NON-NLS-1$

    /**
     * Our tools.
     */
    private final List<ExternalToolAssociation> associations = new ArrayList<ExternalToolAssociation>();

    /**
     * Creates an empty {@link ExternalToolset}.
     */
    public ExternalToolset() {
    }

    /**
     * Finds the appropriate {@link ExternalToolAssociation} in this
     * {@link ExternalToolset} to handle the given {@link File}, which can be a
     * directory or a file.
     *
     * @param file
     *        the File (file or directory) to get the tool for (must not be
     *        <code>null</code>)
     * @return the appropriate {@link ExternalToolAssociation} or null if none
     *         was appropriate
     */
    public synchronized ExternalTool findTool(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        if (file.isDirectory()) {
            return findToolForDirectory();
        }

        return findToolForExtension(LocalPath.getFileExtension(file.getName()));
    }

    /**
     * Finds the appropriate {@link ExternalTool} in this
     * {@link ExternalToolset} to handle the given path, which can be a file or
     * a directory. It can be relative or absolute.
     *
     * @param path
     *        the name of the file or directory (relative or absolute) to get
     *        the tool for (must not be <code>null</code>)
     * @return the appropriate {@link ExternalTool} or null if none was
     *         appropriate
     */
    public synchronized ExternalTool findTool(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        return findTool(new File(path));
    }

    /**
     * Finds the right {@link ExternalTool} for any directory.
     *
     * @return the appropriate {@link ExternalTool}, or null if no directory
     *         tool is configured
     */
    public synchronized ExternalTool findToolForDirectory() {
        for (final Iterator<ExternalToolAssociation> iterator = associations.iterator(); iterator.hasNext();) {
            final ExternalToolAssociation association = iterator.next();

            if (association.containsExtension(DIRECTORY_EXTENSION)) {
                return association.getTool();
            }
        }

        return null;
    }

    /**
     * Finds the right {@link ExternalTool} using only the file extension. Do
     * not call this with the special
     * {@link ExternalToolset#DIRECTORY_EXTENSION}.
     *
     * @param extension
     *        the file extension (not the full path, not
     *        {@link #DIRECTORY_EXTENSION}) (must not be <code>null</code>)
     * @return the appropriate {@link ExternalTool} or null if no appropriate
     *         tool is found (no extension match and no wildcard configured)
     */
    public synchronized ExternalTool findToolForExtension(String extension) {
        Check.notNull(extension, "extension"); //$NON-NLS-1$
        Check.isTrue(
            extension.equals(DIRECTORY_EXTENSION) == false,
            "the extension must not be the special directory extension " + DIRECTORY_EXTENSION); //$NON-NLS-1$

        /*
         * Strip off a leading dot if extension includes it.
         */
        if (extension.startsWith(".")) //$NON-NLS-1$
        {
            extension = extension.substring(1);
        }

        /*
         * Find by extension.
         */
        for (final Iterator<ExternalToolAssociation> iterator = associations.iterator(); iterator.hasNext();) {
            final ExternalToolAssociation association = iterator.next();

            if (association.containsExtension(extension)) {
                return association.getTool();
            }
        }

        /*
         * No extension match, see if there is a wildcard match.
         */
        for (final Iterator<ExternalToolAssociation> iterator = associations.iterator(); iterator.hasNext();) {
            final ExternalToolAssociation association = iterator.next();

            if (association.containsExtension(WILDCARD_EXTENSION)) {
                return association.getTool();
            }
        }

        return null;
    }

    public synchronized void addAssociation(final ExternalToolAssociation association) {
        Check.notNull(association, "association"); //$NON-NLS-1$

        associations.add(association);
    }

    public synchronized final void clear() {
        associations.clear();
    }

    public synchronized ExternalToolAssociation get(final int index) {
        return associations.get(index);
    }

    public final synchronized boolean isEmpty() {
        return associations.isEmpty();
    }

    public synchronized ExternalToolAssociation remove(final int index) {
        return associations.remove(index);
    }

    public synchronized boolean remove(final ExternalToolAssociation association) {
        Check.notNull(association, "association"); //$NON-NLS-1$

        return associations.remove(association);
    }

    public final synchronized int size() {
        return associations.size();
    }

    /**
     * @return the {@link ExternalToolAssociation}s for files
     */
    public synchronized ExternalToolAssociation[] getFileAssociations() {
        final List<ExternalToolAssociation> ret = new ArrayList<ExternalToolAssociation>();

        for (final Iterator<ExternalToolAssociation> iterator = associations.iterator(); iterator.hasNext();) {
            final ExternalToolAssociation association = iterator.next();

            if (association.containsExtension(ExternalToolset.DIRECTORY_EXTENSION) == false) {
                ret.add(association);
            }
        }

        return ret.toArray(new ExternalToolAssociation[ret.size()]);
    }

    /**
     * @return the {@link ExternalToolAssociation} for directories, if there is
     *         one, otherwise null
     */
    public synchronized ExternalToolAssociation getDirectoryAssociation() {
        for (final Iterator<ExternalToolAssociation> iterator = associations.iterator(); iterator.hasNext();) {
            final ExternalToolAssociation association = iterator.next();

            if (association.containsExtension(ExternalToolset.DIRECTORY_EXTENSION)) {
                return association;
            }
        }

        return null;
    }

    /**
     * Saves this toolset's state to the given {@link Memento}, which should
     * have a name (of the caller's choice) but no other data.
     *
     * @param memento
     *        the {@link Memento} to save this toolset's state to (must not be
     *        <code>null</code>)
     */
    public synchronized void saveToMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        /*
         * We can add many children with the same name.
         */
        for (final Iterator<ExternalToolAssociation> iterator = associations.iterator(); iterator.hasNext();) {
            final ExternalToolAssociation association = iterator.next();

            final Memento associationMemento = memento.createChild(ASSOCIATION_MEMENTO_NAME);
            association.saveToMemento(associationMemento);
        }
    }

    /**
     * Loads toolset state from the given {@link Memento}, which can have any
     * name.
     *
     * @param memento
     *        the {@link Memento} to load state from. If null, an empty
     *        {@link ExternalToolset} is returned.
     */
    public static ExternalToolset loadFromMemento(final Memento memento) {
        if (memento == null) {
            return new ExternalToolset();
        }

        final Memento[] associationsChildren = memento.getChildren(ASSOCIATION_MEMENTO_NAME);

        final ExternalToolset ret = new ExternalToolset();

        for (int i = 0; i < associationsChildren.length; i++) {
            ret.addAssociation(ExternalToolAssociation.loadFromMemento(associationsChildren[i]));
        }

        return ret;
    }
}
