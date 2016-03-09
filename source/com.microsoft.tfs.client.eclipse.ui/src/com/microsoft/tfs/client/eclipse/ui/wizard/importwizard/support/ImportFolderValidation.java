// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import com.microsoft.tfs.util.BitField;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

public class ImportFolderValidation {
    private ImportFolderValidationStatus status = ImportFolderValidationStatus.OK;
    private String message = null;
    private ImportFolderValidationFlag flags = ImportFolderValidationFlag.NONE;

    public ImportFolderValidation(final ImportFolderValidationStatus status, final String message) {
        Check.notNull(status, "status"); //$NON-NLS-1$

        this.status = status;
        this.message = message;
    }

    public ImportFolderValidation(
        final ImportFolderValidationStatus status,
        final String message,
        final ImportFolderValidationFlag flags) {
        Check.notNull(status, "status"); //$NON-NLS-1$
        Check.notNull(flags, "flags"); //$NON-NLS-1$

        this.status = status;
        this.message = message;
        this.flags = flags;
    }

    public ImportFolderValidationStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasFlag(final ImportFolderValidationFlag flag) {
        return flags.containsFlag(flag);
    }

    public static class ImportFolderValidationStatus extends TypesafeEnum {
        public static final ImportFolderValidationStatus OK = new ImportFolderValidationStatus(0);
        public static final ImportFolderValidationStatus CLOAKED = new ImportFolderValidationStatus(1);
        public static final ImportFolderValidationStatus ALREADY_EXISTS = new ImportFolderValidationStatus(2);
        public static final ImportFolderValidationStatus ERROR = new ImportFolderValidationStatus(3);

        private ImportFolderValidationStatus(final int value) {
            super(value);
        }
    }

    public static class ImportFolderValidationFlag extends BitField {
        public static final ImportFolderValidationFlag NONE = new ImportFolderValidationFlag(0);
        public static final ImportFolderValidationFlag EXISTING_MAPPING = new ImportFolderValidationFlag(1);
        public static final ImportFolderValidationFlag RECURSIVE = new ImportFolderValidationFlag(2);
        public static final ImportFolderValidationFlag NO_VISUAL_ERROR = new ImportFolderValidationFlag(4);

        protected ImportFolderValidationFlag(final int flags) {
            super(flags);
        }

        public boolean containsFlag(final ImportFolderValidationFlag flag) {
            return containsAllInternal(flag);
        }
    }
}