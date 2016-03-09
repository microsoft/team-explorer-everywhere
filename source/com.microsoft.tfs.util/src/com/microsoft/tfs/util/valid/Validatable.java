// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.valid;

/**
 * <p>
 * A {@link Validatable} is an object that can supply a {@link Validator} for
 * itself. In other words, this interface marks a self-validating object.
 * </p>
 *
 * <p>
 * In general, most objects will not be self-validating. {@link Validator}s for
 * most objects will need to be obtained externally to the object itself. One
 * reason for this is that different clients of an object may have different
 * validation rules for that object. However, in some situations it may make
 * sense to have an object supply a {@link Validator} for itself.
 * </p>
 *
 * <p>
 * The subject ({@link Validator#getSubject()}) of the returned
 * {@link Validator} is the {@link Validatable} that returned it.
 * </p>
 *
 * @see Validator
 */
public interface Validatable {
    /**
     * Called to obtain a {@link Validator} for this subject. Each call to this
     * method may return a different {@link Validator} or each call may return
     * the same {@link Validator} instance.
     *
     * @return a {@link Validator} (never <code>null</code>)
     */
    public Validator getValidator();
}
