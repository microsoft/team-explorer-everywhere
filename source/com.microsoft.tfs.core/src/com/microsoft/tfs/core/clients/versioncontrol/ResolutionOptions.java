// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * Contains options that affect conflict resolution.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public final class ResolutionOptions {
    public static class EncodingStrategy extends TypesafeEnum {
        private EncodingStrategy(final int value) {
            super(value);
        }

        /**
         * When this strategy is chosen, the file's existing encoding is used.
         */
        public final static EncodingStrategy DEFAULT = new EncodingStrategy(0);
        /**
         * When this strategy is chosen, the all files involved in the merge
         * will have their encodings treated like the given encoding. No file
         * conversion is done.
         */
        public final static EncodingStrategy OVERRIDE_EXPLICIT = new EncodingStrategy(1);
        /**
         * When this strategy is chosen, the all files involved in the merge
         * will be converted into the explicitly named encoding.
         */
        public final static EncodingStrategy CONVERT_EXPLICIT = new EncodingStrategy(2);
    }

    /**
     * How to go about resolving encoding conflicts.
     */
    private EncodingStrategy encodingStrategy = EncodingStrategy.DEFAULT;

    /**
     * Only used when _encodingStrategy is not default.
     */
    private FileEncoding explicitEncoding = FileEncoding.AUTOMATICALLY_DETECT;

    private boolean useInternalEngine = true;

    private FileEncoding acceptMergeEncoding = null;

    private String newPath = null;

    private boolean acceptMergeWithConflicts = false;

    private PropertyValue[] acceptMergeProperties;

    /**
     * Creates a {@link ResolutionOptions} with the default options set.
     */
    public ResolutionOptions() {
        super();
    }

    /**
     * Sets the strategy for resolving encoding conflicts. If the strategy is
     * EncodingStrategy.DEFAULT, explicitEncoding must be null. If the strategy
     * is some other value, explicitEncoding must be non-null, and represents
     * the overriding encoding, or conversion encoding, or whatever that
     * strategy's comment says it represents.
     *
     * @param strategy
     *        the strategy to take for resolving encoding conflicts.
     * @param explicitEncoding
     *        the encoding to use for resolving conflicts (null if the strategy
     *        is EncodingStrategy.DEFAULT).
     */
    public void setEncodingStrategy(final EncodingStrategy strategy, final FileEncoding explicitEncoding) {
        Check.isTrue(
            (strategy == EncodingStrategy.DEFAULT && explicitEncoding == null || explicitEncoding != null),
            "explicitEncoding must be null if strategy is EncodingStrategy.DEFAULT"); //$NON-NLS-1$

        encodingStrategy = strategy;
        this.explicitEncoding = explicitEncoding;
    }

    /**
     * Gets the encoding resolution strategy. If the returned strategy is not
     * EncodingStrategy.DEFAULT, call getExplicitEncoding() to get the encoding
     * to be used for the strategy.
     *
     * @return the encoding resolution strategy.
     */
    public EncodingStrategy getEncodingStrategy() {
        return encodingStrategy;
    }

    /**
     * Gets the explicit encoding set previously as part of setting an encoding
     * strategy.
     *
     * @return the encoding to use as part of the encoding resolution strategy,
     *         null if not set or if the strategy was EncodingStrategy.DEFAULT.
     */
    public FileEncoding getExplicitEncoding() {
        return explicitEncoding;
    }

    public void setUseInternalEngine(final boolean useInternalEngine) {
        this.useInternalEngine = useInternalEngine;
    }

    public boolean useInternalEngine() {
        return useInternalEngine;
    }

    /**
     * When a conflict is to be resolved with the AcceptMerge resolution and
     * there is a conflicting pending encoding change, the given encoding will
     * be used (no file conversion is done). If the given encoding is null, the
     * conflict will not be resolved.
     *
     * @param encoding
     *        the encoding to use when an automatic merge is desired and there
     *        is a conflicting pending change.
     */
    public void setAcceptMergeEncoding(final FileEncoding encoding) {
        acceptMergeEncoding = encoding;
    }

    /**
     * Gets the encoding to use when an AcceptMerge resolution is desired but
     * there is a conflicting pending encoding change. If null is returned, the
     * encoding should be unchanged.
     *
     * @return the encoding to use to resolve the case where AcceptMerge must
     *         operate on a file with a conflicting pending encoding change,
     *         null if the encoding should be unchanged.
     */
    public FileEncoding getAcceptMergeEncoding() {
        return acceptMergeEncoding;
    }

    /**
     * When a conflict is to be resolved with the AcceptMerge resolution and
     * there is a conflicting pending property change, this property contains
     * the desired properties. If this property is left as null, the conflict
     * will not be resolved.
     */
    public PropertyValue[] getAcceptMergeProperties() {
        return acceptMergeProperties;
    }

    public void setAcceptMergeProperties(final PropertyValue[] acceptMergeProperties) {
        this.acceptMergeProperties = acceptMergeProperties;
    }

    /**
     * Sets the new path for a conflicted item or the item in its way when it
     * needs needs to move to a new location. This may happen in cases like
     * these:
     *
     * Merge conflict with AcceptMerge chosen, and there's a conflicting pending
     * rename: set the desired name. If null, the conflict will not be resolved.
     *
     * Namespace conflict with AcceptTheirs: set the path that would describe
     * their item.
     *
     * Namespace conflict with AcceptYours: set to the path of the local item
     * that was in the way of the server item.
     *
     * @param newPath
     *        the path to use for this conflict resolution, null to defer the
     *        resolution in the cases documented above.
     */
    public void setNewPath(final String newPath) {
        this.newPath = newPath;
    }

    /**
     * Gets the new path to use for this resolution. See setNewPath() comments
     * for details.
     *
     * @return the new path, null if not set.
     */
    public String getNewPath() {
        return newPath;
    }

    /**
     * @return true if a merge should be resolved when conflicts remain in the
     *         file, false if the merge should not be resolved when conflicts
     *         remain
     */
    public boolean isAcceptMergeWithConflicts() {
        return acceptMergeWithConflicts;
    }

    /**
     * Sets the option to accept (resolve a conflict) when the merge resulted in
     * conflicts.
     *
     * @param mergeWithConflicts
     *        true if a merge should be resolved when conflicts remain in the
     *        file, false if the merge should not be resolved when conflicts
     *        remain
     */
    public void setAcceptMergeWithConflicts(final boolean mergeWithConflicts) {
        acceptMergeWithConflicts = mergeWithConflicts;
    }
}
