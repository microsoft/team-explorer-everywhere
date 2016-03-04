// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.account.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoreAttributes {

    @JsonProperty("DisplayName")
    private Attribute displayName;

    @JsonProperty("PublicAlias")
    private Attribute publicAlias;

    @JsonProperty("EmailAddress")
    private Attribute emailAddress;

    @JsonProperty("CountryName")
    private Attribute countryName;

    public Attribute getCountryName() {
        return countryName;
    }

    public void setCountryName(final Attribute countryName) {
        this.countryName = countryName;
    }

    public Attribute getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final Attribute emailAddress) {
        this.emailAddress = emailAddress;
    }

    public Attribute getPublicAlias() {
        return publicAlias;
    }

    public void setPublicAlias(final Attribute publicAlias) {
        this.publicAlias = publicAlias;
    }

    public Attribute getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final Attribute displayName) {
        this.displayName = displayName;
    }
}
