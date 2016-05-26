// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.UsernamePasswordValueOption;
import com.microsoft.tfs.util.StringUtil;

public final class OptionLogin extends UsernamePasswordValueOption {

    public enum LoginType {
        JWT, USERNAME_PASSWORD
    }

    LoginType type;
    String token;

    public OptionLogin() {
        super();
    }

    public LoginType getType() {
        return type;
    }

    public String getToken() {
        return token;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void parseValues(String optionValueString) throws InvalidOptionValueException {
        if (!StringUtil.isNullOrEmpty(getMatchedAlias())
            // The matching alias can be null in unit tests
            && LoginType.JWT.toString().equalsIgnoreCase(getMatchedAlias())) {
            type = LoginType.JWT;
            token = optionValueString;
        } else {
            type = LoginType.USERNAME_PASSWORD;
            super.parseValues(optionValueString);
        }
    }

}
