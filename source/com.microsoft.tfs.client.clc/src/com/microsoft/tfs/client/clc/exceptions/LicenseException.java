// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.exceptions;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

public class LicenseException extends Exception {
    static final long serialVersionUID = 6885764512958611924L;

    private final LicenseExceptionType type;

    public LicenseException(final LicenseExceptionType type, final String message) {
        super(message);

        Check.notNull(type, "type"); //$NON-NLS-1$
        this.type = type;
    }

    public LicenseExceptionType getType() {
        return type;
    }

    public static class LicenseExceptionType extends TypesafeEnum {
        public static final LicenseExceptionType EULA = new LicenseExceptionType(0);
        public static final LicenseExceptionType PRODUCT_ID = new LicenseExceptionType(1);

        private LicenseExceptionType(final int value) {
            super(value);
        }
    }
}
