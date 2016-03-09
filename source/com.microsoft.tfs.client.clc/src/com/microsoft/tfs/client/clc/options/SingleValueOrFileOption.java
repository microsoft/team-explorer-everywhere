// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;

public abstract class SingleValueOrFileOption extends SingleValueOption {
    private String _value = null;

    public SingleValueOrFileOption() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.SingleValueOption#getValue()
     */
    @Override
    public String getValue() {
        return _value;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        /*
         * Call the parent so we have a single string to work with.
         */
        super.parseValues(optionValueString);

        /*
         * Valid syntax is something like:
         *
         * /option:"this is a string"
         *
         * /option:@file.txt
         */
        final String value = super.getValue();

        if (value != null && value.length() > 1 && value.charAt(0) == '@') {
            final String file = value.substring(1);

            final StringBuffer sb = new StringBuffer();

            FileReader fr = null;
            BufferedReader br = null;
            try {
                fr = new FileReader(file);
                br = new BufferedReader(fr);

                final char[] buf = new char[1024];
                int read = 0;

                while ((read = fr.read(buf)) != -1) {
                    sb.append(buf, 0, read);
                }

                br.close();
                fr.close();
            } catch (final FileNotFoundException e) {
                final String messageFormat =
                    Messages.getString("SingleValueOrFileOption.OptionValueFileNotFoundFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, file);
                throw new InvalidOptionValueException(message);
            } catch (final IOException e) {
                final String messageFormat =
                    Messages.getString("SingleValueOrFileOption.ErrorReadingOptionValueFileFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, file, e.getLocalizedMessage());
                throw new InvalidOptionValueException(message);
            }

            _value = sb.toString();
        } else {
            _value = value;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.Option#getSyntaxString()
     */
    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":<value>|@valuefile"; //$NON-NLS-1$
    }
}
