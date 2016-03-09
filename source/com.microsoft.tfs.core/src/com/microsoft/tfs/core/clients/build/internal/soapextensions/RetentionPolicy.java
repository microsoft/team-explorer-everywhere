// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IRetentionPolicy;
import com.microsoft.tfs.core.clients.build.flags.BuildReason;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.DeleteOptions;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._RetentionPolicy;

public class RetentionPolicy extends WebServiceObjectWrapper implements IRetentionPolicy {
    private IBuildDefinition buildDefinition;

    private RetentionPolicy() {
        this(new _RetentionPolicy());
    }

    public RetentionPolicy(final _RetentionPolicy webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Creates a new retention policy for the given BuildStatus, BuildReason
     * combination owned by the given BuildDefinition.
     *
     *
     * @param definition
     *        The build definition that owns this retention policy.
     * @param reason
     *        The reason of the retention policy.
     * @param status
     *        The status of the retention policy.
     * @param numberToKeep
     *        The number to keep of the retention policy.
     * @param deleteOptions
     *        The parts of the build to delete for the retention policy.
     */
    public RetentionPolicy(
        final IBuildDefinition definition,
        final BuildReason reason,
        final BuildStatus status,
        final int numberToKeep,
        final DeleteOptions deleteOptions) {
        this();

        this.buildDefinition = definition;
        getWebServiceObject().setBuildReason(reason.getWebServiceObject());
        getWebServiceObject().setBuildStatus(status.getWebServiceObject());
        getWebServiceObject().setNumberToKeep(numberToKeep);
        getWebServiceObject().setDeleteOptions(deleteOptions.getWebServiceObject());
    }

    public _RetentionPolicy getWebServiceObject() {
        return (_RetentionPolicy) this.webServiceObject;
    }

    @Override
    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public void setBuildDefinition(final IBuildDefinition value) {
        buildDefinition = value;
    }

    /**
     * Gets or sets the reasons to which this policy applies. {@inheritDoc}
     */
    @Override
    public BuildReason getBuildReason() {
        return BuildReason.fromWebServiceObject(getWebServiceObject().getBuildReason());
    }

    @Override
    public void setBuildReason(final BuildReason value) {
        getWebServiceObject().setBuildReason(value.getWebServiceObject());
    }

    /**
     * Gets or sets the status to which this policy applies. {@inheritDoc}
     */
    @Override
    public BuildStatus getBuildStatus() {
        return BuildStatus.fromWebServiceObject(getWebServiceObject().getBuildStatus());
    }

    @Override
    public void setBuildStatus(final BuildStatus value) {
        getWebServiceObject().setBuildStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the data that should be deleted. {@inheritDoc}
     */
    @Override
    public DeleteOptions getDeleteOptions() {
        return DeleteOptions.fromWebServiceObject(getWebServiceObject().getDeleteOptions());
    }

    @Override
    public void setDeleteOptions(final DeleteOptions value) {
        getWebServiceObject().setDeleteOptions(value.getWebServiceObject());
    }

    /**
     * Gets or sets the number of builds to retain. {@inheritDoc}
     */
    @Override
    public int getNumberToKeep() {
        return getWebServiceObject().getNumberToKeep();
    }

    @Override
    public void setNumberToKeep(final int value) {
        getWebServiceObject().setNumberToKeep(value);
    }
}
