// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.util.Check;

/**
 * This is the legacy-style file modification validator (which only implements
 * the pre-3.3 {@link IFileModificationValidator}). Post-3.3 versions of Eclipse
 * should not use it.
 *
 * @threadsafety unknown
 */
public class TFSFileModificationValidatorLegacy implements IFileModificationValidator {
    private final TFSFileModificationValidator validator;

    public TFSFileModificationValidatorLegacy(final TFSFileModificationValidator validator) {
        Check.notNull(validator, "validator"); //$NON-NLS-1$

        this.validator = validator;
    }

    @Override
    public IStatus validateEdit(final IFile[] files, final Object context) {
        return validator.validateEdit(files, (context != null), context);
    }

    @Override
    public IStatus validateSave(final IFile file) {
        return validator.validateSave(file);
    }
}
