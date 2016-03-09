// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.builtin.java;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;
import java.util.Properties;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.DataProvider;
import com.microsoft.tfs.client.common.ui.framework.diagnostics.extend.LocalizedDataProvider;

public class DefaultCharacterEncodingProvider extends LocalizedDataProvider implements DataProvider {
    @Override
    protected Object getData(final Locale locale) {
        final Properties properties = new Properties();

        final String defaultCharsetName = System.getProperty("file.encoding"); //$NON-NLS-1$
        properties.put(
            Messages.getString("DefaultCharacterEncodingProvider.DefaultCharSet", locale), //$NON-NLS-1$
            defaultCharsetName);

        Charset defaultCharset;
        try {
            defaultCharset = Charset.forName(defaultCharsetName);
        } catch (final IllegalCharsetNameException ex) {
            properties.put(
                Messages.getString("DefaultCharacterEncodingProvider.IllegalCharsetName", locale), //$NON-NLS-1$
                ex.getMessage());
            return properties;
        } catch (final UnsupportedCharsetException ex) {
            properties.put(
                Messages.getString("DefaultCharacterEncodingProvider.UnsupportedCharsetException", locale), //$NON-NLS-1$
                ex.getMessage());
            return properties;
        }

        final String charsetDisplayName = defaultCharset.displayName();
        properties.put(
            Messages.getString("DefaultCharacterEncodingProvider.DefaultCharsetDisplayName", locale), //$NON-NLS-1$
            charsetDisplayName);

        return properties;
    }
}
