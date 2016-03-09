// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.account.model;

import java.net.URI;
import java.util.Date;
import java.util.UUID;

/**
 * Placeholder class
 *
 * Should convert to generated class in the future
 */
public class Account {

    private UUID accountId;
    private UUID namespaceId;
    private URI accountUri;
    private String accountName;
    private String organizationName;
    private AccountType accountType;
    private UUID accountOwner;
    private UUID createdBy;
    private Date createdDate;
    private AccountStatus accountStatus;
    private String statusReason;
    private UUID lastUpdatedBy;
    private Date lastUpdatedDate;

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(final UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(final UUID namespaceId) {
        this.namespaceId = namespaceId;
    }

    public URI getAccountUri() {
        return accountUri;
    }

    public void setAccountUri(final URI accountUri) {
        this.accountUri = accountUri;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(final String accountName) {
        this.accountName = accountName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(final String organizationName) {
        this.organizationName = organizationName;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(final AccountType accountType) {
        this.accountType = accountType;
    }

    public UUID getAccountOwner() {
        return accountOwner;
    }

    public void setAccountOwner(final UUID accountOwner) {
        this.accountOwner = accountOwner;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(final UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(final Date createdDate) {
        this.createdDate = createdDate;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(final AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(final String statusReason) {
        this.statusReason = statusReason;
    }

    public UUID getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(final UUID lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public Date getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(final Date lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
