// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.util.Check;

/**
 * This is the new-style file modification validator (extending the new as of
 * Eclipse 3.3 {@link FileModificationValidator} class). This class merely
 * proxies to {@link TFSFileModificationValidator}.
 *
 * @threadsafety unknown
 */
public class TFSFileModificationValidator2 extends FileModificationValidator {
    private final TFSFileModificationValidator validator;

    public TFSFileModificationValidator2(final TFSFileModificationValidator validator) {
        Check.notNull(validator, "validator"); //$NON-NLS-1$

        this.validator = validator;
    }

    @Override
    public IStatus validateEdit(final IFile[] files, final FileModificationValidationContext context) {
        boolean attemptUi;
        Object shell;

        if (context == null) {
            attemptUi = false;
            shell = null;
        } else {
            attemptUi = true;
            shell = context.getShell();
        }

        return validator.validateEdit(files, attemptUi, shell);
    }

    @Override
    public IStatus validateSave(final IFile file) {
        return validator.validateSave(file);
    }
}
