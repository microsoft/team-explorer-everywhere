// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildDeletionResult {
    /**
     * Gets a value indicating the complete or partial success of the deletion.
     * If false, other properties should be examined for the source of the
     * failure.
     *
     *
     * @return
     */
    public boolean isSuccessful();

    /**
     * Gets the failure associated with label deletion (if one occurred).
     *
     *
     * @return
     */
    public IFailure getLabelFailure();

    /**
     * Gets the failure associated with symbols deletion (if one occurred).
     *
     *
     * @return
     */
    public IFailure getSymbolsFailure();

    /**
     * Gets the failure associated with test result deletion (if one occurred).
     *
     *
     * @return
     */
    public IFailure getTestResultFailure();

    /**
     * Gets the failure associated with drop location deletion (if one
     * occurred).
     *
     *
     * @return
     */
    public IFailure getDropLocationFailure();
}
