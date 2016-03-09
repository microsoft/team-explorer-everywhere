// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final String BUNDLE_NAME = "com.microsoft.tfs.client.common.ui.teambuild.messages"; //$NON-NLS-1$

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(final String key) {
        try {
            return RESOURCE_BUNDLE.getString(key);
        } catch (final MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    public static String getString(final String key, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        try {
            return ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        } catch (final MissingResourceException e) {
            return getString(key);
        }
    }
}
