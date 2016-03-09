// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.tpignore;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import com.microsoft.tfs.util.Check;

/**
 * Handles the in-memory persistence of the exclusions from multiple
 * <code>.tpignore</code> files (one per project) using
 * <code>IgnorableResourcesFileParser</code> as reader. This cache object keeps
 * the resource patterns fresh by periodically (and transparently) reading from
 * the files on disk.
 */
public final class TPIgnoreCache {
    private static final Log log = LogFactory.getLog(TPIgnoreCache.class);

    private static class RefreshAction {
        public final static int NOTHING = 1;
        public final static int LOAD_OR_REFRESH = 2;
        public final static int REMOVE_EXISTING = 3;
        public final static int RETURN_EXISTING = 4;
    }

    /**
     * Caches our patterns we have read from disk. The key is the .tpignore
     * {@link IFile} and the result is a cache entry (which contains compiled
     * patterns and the time they were read from disk).
     */
    private final HashMap<IFile, TPIgnoreCacheEntry> ignoreFileToEntriesMap = new HashMap<IFile, TPIgnoreCacheEntry>();

    private static Pattern[] NO_MATCH_PATTERN_ARRAY = new Pattern[0];

    /**
     * Creates a cache to consult for the latest news in resources that should
     * be ignored during automatic adds.
     */
    public TPIgnoreCache() {
    }

    /**
     * Gets the {@link IFile} the .tpignore file for the given resource might
     * exist.
     *
     * @param resource
     *        the resource to get the .tpignore file for (must not be
     *        <code>null</code>)
     * @return the .tpignore {@link IFile} for the specified resource (the
     *         .tpignore file may not exist), or <code>null</code> if the
     *         resource cannot have a .tpignore file (workspace root)
     */
    public static IFile getIgnoreFile(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        if (resource.getProject() == null) {
            return null;
        }

        return resource.getProject().getFile(TPIgnoreDocument.DEFAULT_FILENAME);
    }

    /**
     * Creates a regular expression to match the given resource. If the resource
     * is a project or folder, the pattern matches every path which starts with
     * that folder path. For files, the expression matches that file exactly.
     *
     * @param resource
     *        the resource to create a pattern for (must not be
     *        <code>null</code>)
     * @return the pattern string
     */
    public static String createIgnorePatternForResource(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        final String path = TPIgnoreCache.createResourceMatchString(resource);

        String patternString = Pattern.quote(path);

        /*
         * Make folders match recursively, other types match only exactly.
         */
        if (resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT) {
            patternString = patternString + ".*"; //$NON-NLS-1$
        }

        return patternString;
    }

    /**
     * Gets the normalized matching string for a resource. The string always
     * starts with a slash, then the Eclipse project-relative path, then a slash
     * only if the resource is a folder or project.
     *
     * @param resource
     *        the resource to get the match string for (must not be
     *        <code>null</code>)
     * @return the match string
     */
    public static String createResourceMatchString(final IResource resource) {
        String matchPath = resource.getProjectRelativePath().toString();

        if (matchPath.startsWith("/") == false) //$NON-NLS-1$
        {
            matchPath = "/" + matchPath; //$NON-NLS-1$
        }

        if ((resource.getType() == IResource.FOLDER || resource.getType() == IResource.PROJECT)
            && matchPath.endsWith("/") == false) //$NON-NLS-1$
        {
            matchPath = matchPath + "/"; //$NON-NLS-1$
        }

        return matchPath;
    }

    /**
     * Gets pattern strings from {@link Pattern} objects.
     *
     * @param patterns
     *        the patterns to get strings for (must not be <code>null</code>)
     * @return an array of strings equal in size to the given collection
     *         containing the pattern strings
     */
    public static String[] getPatternStrings(final Collection<Pattern> patterns) {
        Check.notNull(patterns, "patterns"); //$NON-NLS-1$

        final String[] ret = new String[patterns.size()];

        int i = 0;
        for (final Pattern pattern : patterns) {
            ret[i++] = pattern.toString();
        }

        return ret;
    }

    /**
     * Gets the unique (as computed by {@link TPIgnorePatternComparator})
     * patterns in this cache which match the given resource.
     *
     * @param resource
     *        the resource to find matching patterns for (must not be
     *        <code>null</code>)
     * @return the {@link Pattern}s which match, {@link #NO_MATCH_PATTERN_ARRAY}
     *         if none match
     */
    public Pattern[] getMatchingPatterns(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        if (canMatch(resource)) {
            final Pattern[] patterns = getExclusionPatterns(resource.getProject());

            if (patterns == null || patterns.length == 0) {
                return NO_MATCH_PATTERN_ARRAY;
            }

            final String matchPath = createResourceMatchString(resource);

            final Set<Pattern> matches = new TreeSet<Pattern>(new TPIgnorePatternComparator());

            for (final Pattern pattern : patterns) {
                if (pattern.matcher(matchPath).matches()) {
                    log.debug(MessageFormat.format(
                        "item ''{0}'' matched exclusion pattern ''{1}''", //$NON-NLS-1$
                        matchPath,
                        pattern.toString()));

                    matches.add(pattern);
                }
            }

            if (matches.size() > 0) {
                return matches.toArray(new Pattern[matches.size()]);
            }
        }

        return NO_MATCH_PATTERN_ARRAY;
    }

    /**
     * Tests whether the given resource matches any exclusion pattern in this
     * cache.
     *
     * @param resource
     *        the resource to test whether it matches (not null).
     * @return true if the the given resource matches the exclusions set for the
     *         resource's project, false if it does not.
     */
    public boolean matchesAnyPattern(final IResource resource) {
        Check.notNull(resource, "resource"); //$NON-NLS-1$

        if (canMatch(resource)) {
            final Pattern[] patterns = getExclusionPatterns(resource.getProject());

            if (patterns == null || patterns.length == 0) {
                return false;
            }

            final String matchPath = createResourceMatchString(resource);

            for (final Pattern pattern : patterns) {
                if (pattern.matcher(matchPath).matches()) {
                    log.debug(MessageFormat.format(
                        "item ''{0}'' matched exclusion pattern ''{1}''", //$NON-NLS-1$
                        matchPath,
                        pattern.toString()));

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * @return <code>true</code> if the specified resource can match cache
     *         entries, <code>false</code> if it is not valid to match it
     */
    private boolean canMatch(final IResource resource) {
        return resource.getType() == IResource.FILE
            || resource.getType() == IResource.FOLDER
            || resource.getType() == IResource.PROJECT;
    }

    /**
     * Gets the exclusion patterns for the given project.
     *
     * @param project
     *        the Eclipse project to get exclusions for (not null).
     * @return an array of the regular expression patterns loaded from the
     *         .tpignore file for the given project, or null if none were loaded
     *         because the file did not exist or an error occurred reading it.
     */
    private Pattern[] getExclusionPatterns(final IProject project) {
        Check.notNull(project, "project"); //$NON-NLS-1$

        final IFile ignoreFile = getIgnoreFile(project);
        if (ignoreFile == null) {
            return null;
        }

        final boolean ignoreFileExists = ignoreFile.exists();
        final long ignoreFileLastModified = ignoreFile.getModificationStamp();

        Pattern[] ret = null;

        synchronized (ignoreFileToEntriesMap) {
            final boolean alreadyHaveEntry = ignoreFileToEntriesMap.containsKey(ignoreFile);

            TPIgnoreCacheEntry existingEntry = null;

            int refreshAction = RefreshAction.NOTHING;

            if (ignoreFileExists && alreadyHaveEntry) {
                existingEntry = ignoreFileToEntriesMap.get(ignoreFile);

                // We have the file on disk and an entry, so update it if the
                // dates are new, otherwise return what we have.
                if (existingEntry.getLoadedFromDiskTime() != ignoreFileLastModified) {
                    refreshAction = RefreshAction.LOAD_OR_REFRESH;
                } else {
                    refreshAction = RefreshAction.RETURN_EXISTING;
                }
            } else if (ignoreFileExists && alreadyHaveEntry == false) {
                refreshAction = RefreshAction.LOAD_OR_REFRESH;
            } else if (ignoreFileExists == false && alreadyHaveEntry) {
                refreshAction = RefreshAction.REMOVE_EXISTING;
            } else {
                /*
                 * File does not exist and we have no entry, so nothing to do.
                 */
            }

            switch (refreshAction) {
                case RefreshAction.LOAD_OR_REFRESH:
                    log.trace("LOAD_OR_REFRESH"); //$NON-NLS-1$

                    ret = TPIgnoreFileParser.load(ignoreFile);

                    /*
                     * Null patterns means error reading file, so remove the
                     * entry so it can be parsed again later.
                     */
                    if (ret == null) {
                        ignoreFileToEntriesMap.remove(ignoreFile);
                    } else {
                        /*
                         * Query the file's mod time again so we're most
                         * accurate in heading off future re-reads.
                         */
                        ignoreFileToEntriesMap.put(
                            ignoreFile,
                            new TPIgnoreCacheEntry(ignoreFile.getModificationStamp(), ret));
                    }

                    break;
                case RefreshAction.REMOVE_EXISTING:
                    log.trace("REMOVE_EXISTING"); //$NON-NLS-1$
                    ignoreFileToEntriesMap.remove(ignoreFile);
                    break;
                case RefreshAction.RETURN_EXISTING:
                    log.trace("RETURN_EXISTING"); //$NON-NLS-1$
                    Check.notNull(existingEntry, "existingEntry"); //$NON-NLS-1$
                    ret = existingEntry.getPatterns();
                    break;
                case RefreshAction.NOTHING:
                    log.trace("NOTHING"); //$NON-NLS-1$
                    // Nothing to load or return.
                    break;
            }
        }

        return ret;
    }
}
