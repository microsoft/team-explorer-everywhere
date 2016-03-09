// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.util.Check;

/**
 * Contains the results of a translation via working folder mapping of a path
 * (server or local) to the other type (local or server). Includes the
 * translated paths as well as information about the mapping which was used for
 * the translation.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public class PathTranslation {
    private final String originalPath;
    private final String translatedPath;
    private final boolean isCloaked;
    private final RecursionType recursionType;

    /**
     * Constructs a {@link PathTranslation}.
     *
     * @param originalPath
     *        the path (server or local) which was translated to the other kind
     *        (local or server) (must not be <code>null</code> or empty)
     * @param translatedPath
     *        the translated path (server or local). Must not be
     *        <code>null</code> or empty if isCloaked is <code>false</code>,
     *        otherwise may be <code>null</code>.
     * @param isCloaked
     *        <code>true</code> if the mapping used for translation was a cloak
     *        mapping, <code>false</code> if it was a normal mapping
     * @param recursionType
     *        the recursion type used to perform the mapping (see
     *        {@link #getRecursionType()} for more information)
     */
    public PathTranslation(
        final String originalPath,
        final String translatedPath,
        final boolean isCloaked,
        final RecursionType recursionType) {
        Check.notNullOrEmpty(originalPath, "originalPath"); //$NON-NLS-1$
        Check.notNull(recursionType, "recursionType"); //$NON-NLS-1$

        // Non-cloak mappings must have a translated item
        if (isCloaked == false) {
            Check.notNullOrEmpty(translatedPath, "translatedPath"); //$NON-NLS-1$
        } else {
            // Can't be empty if it's a cloak mapping, only null is supported
            Check.notEmpty(translatedPath, "translatedPath"); //$NON-NLS-1$
        }

        this.originalPath = originalPath;
        this.translatedPath = translatedPath;
        this.isCloaked = isCloaked;
        this.recursionType = recursionType;
    }

    /**
     * @return the path before translation (server or local). Never
     *         <code>null</code>.
     */
    public String getOriginalPath() {
        return originalPath;
    }

    /**
     * @return the path after translation (server or local). May be
     *         <code>null</code> when {@link #isCloaked()} returns
     *         <code>true</code>, otherwise never <code>null</code> (required to
     *         support a translation of a server path to a local path in a
     *         cloaked mapping).
     */
    public String getTranslatedPath() {
        return translatedPath;
    }

    /**
     * @return <code>true</code> if the {@link WorkingFolder} mapping used to
     *         translate the path was a cloak mapping, <code>false</code> if it
     *         was a non-cloak mapping
     */
    public boolean isCloaked() {
        return isCloaked;
    }

    /**
     * @return the {@link RecursionType} of the working folder mapping used to
     *         translate the path. Always {@link RecursionType#NONE} for items
     *         mapped inside a {@link RecursionType#ONE_LEVEL} mapping,
     *         otherwise the {@link WorkingFolder}'s mapping.
     */
    public RecursionType getRecursionType() {
        return recursionType;
    }
}
