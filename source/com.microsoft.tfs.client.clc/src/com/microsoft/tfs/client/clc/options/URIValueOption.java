// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;

/**
 * Parses a URI from the option value. The {@link URL} class is actually used
 * for the parse, because it's stricter and gives the user better messages for
 * common errors compared with {@link URI}.
 */
public class URIValueOption extends SingleValueOption {
    private URI uri;

    public URIValueOption() {
        super();
    }

    @Override
    public String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        /*
         * Let the superclass parse the string into the value.
         */
        super.parseValues(optionValueString);

        final String value = getValue().replaceAll("\\\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$

        final String incompleteURLMessageFormat = Messages.getString("URIValueOption.OptionRequiresFullURLFormat"); //$NON-NLS-1$

        try {
            /*
             * Use URL to parse, because it's more restrictive than URI, and
             * this helps users get the format correct during parse (here)
             * instead of seeing a network-level error later.
             */
            uri = new URI(null, new URL(value).toString(), null);
        } catch (final MalformedURLException e) {
            final String message =
                MessageFormat.format(incompleteURLMessageFormat, getMatchedAlias(), e.getLocalizedMessage());
            throw new InvalidOptionValueException(message);
        } catch (final URISyntaxException e) {
            /*
             * This exception should be pretty rare since most URLs are valid
             * URIs.
             */
            final String message =
                MessageFormat.format(incompleteURLMessageFormat, getMatchedAlias(), e.getLocalizedMessage());
            throw new InvalidOptionValueException(message);
        }
    }

    public URI getURI() {
        return uri;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.Option#getSyntaxString()
     */
    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":<url>"; //$NON-NLS-1$
    }
}
