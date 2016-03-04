// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.delegatedauthorization.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Placeholder class
 *
 * Should convert to generated class in the future
 */
public class SessionToken {
    private UUID clientId;
    private UUID accessId;
    private UUID authorizationId;
    private UUID userId;
    private Date validFrom;
    private Date validTo;
    private String displayName;
    private String scope;
    private List<String> targetAccounts;
    private String token;
    private String alternateToken;
    private Boolean isValid;

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(final UUID clientId) {
        this.clientId = clientId;
    }

    public UUID getAccessId() {
        return accessId;
    }

    public void setAccessId(final UUID accessId) {
        this.accessId = accessId;
    }

    public UUID getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(final UUID authorizationId) {
        this.authorizationId = authorizationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getValidTo() {
        return validTo;
    }

    public void setValidTo(final Date validTo) {
        this.validTo = validTo;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public List<String> getTargetAccounts() {
        return targetAccounts;
    }

    public void setTargetAccounts(final List<String> targetAccounts) {
        this.targetAccounts = targetAccounts;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public String getAlternateToken() {
        return alternateToken;
    }

    public void setAlternateToken(final String alternateToken) {
        this.alternateToken = alternateToken;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(final Boolean isValid) {
        this.isValid = isValid;
    }
}
