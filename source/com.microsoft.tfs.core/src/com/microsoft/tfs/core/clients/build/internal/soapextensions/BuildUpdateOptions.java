// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.BuildUpdate;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._BuildUpdateOptions;

public class BuildUpdateOptions extends WebServiceObjectWrapper {
    public BuildUpdateOptions() {
        super(new _BuildUpdateOptions());

        getWebServiceObject().setFields(new BuildUpdate(BuildUpdate.NONE).getWebServiceObject());
        getWebServiceObject().setStatus(BuildStatus.NONE.getWebServiceObject());
        getWebServiceObject().setCompilationStatus(BuildPhaseStatus.UNKOWN.getWebServiceObject());
        getWebServiceObject().setTestStatus(BuildPhaseStatus.UNKOWN.getWebServiceObject());
    }

    public _BuildUpdateOptions getWebServiceObject() {
        return (_BuildUpdateOptions) this.webServiceObject;
    }

    /**
     * Gets or sets the build number. Corresponds to
     * <see cref="BuildUpdate.BuildNumber" />.
     *
     *
     * @return
     */
    public String getBuildNumber() {
        return getWebServiceObject().getBuildNumber();
    }

    public void setBuildNumber(final String value) {
        getWebServiceObject().setBuildNumber(value);
    }

    /**
     * Gets or sets the compilation status. Corresponds to
     * <see cref="BuildUpdate.CompilationStatus" />.
     *
     *
     * @return
     */
    public BuildPhaseStatus getCompilationStatus() {
        return BuildPhaseStatus.fromWebServiceObject(getWebServiceObject().getCompilationStatus());
    }

    public void setCompilationStatus(final BuildPhaseStatus value) {
        getWebServiceObject().setCompilationStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the drop location. Corresponds to
     * <see cref="BuildUpdate.DropLocation" />.
     *
     *
     * @return
     */
    public String getDropLocation() {
        return getWebServiceObject().getDropLocation();
    }

    public void setDropLocation(final String value) {
        getWebServiceObject().setDropLocation(value);
    }

    /**
     * Gets or sets the fields which should be updated. Only values included
     * here will be extracted from this object during an update.
     *
     *
     * @return
     */
    public BuildUpdate getFields() {
        return new BuildUpdate(getWebServiceObject().getFields());
    }

    public void setFields(final BuildUpdate value) {
        getWebServiceObject().setFields(value.getWebServiceObject());
    }

    /**
     * Gets or sets the keep forever flag. Corresponds to
     * <see cref="BuildUpdate.KeepForever" />.
     *
     *
     * @return
     */
    public boolean isKeepForever() {
        return getWebServiceObject().isKeepForever();
    }

    public void setKeepForever(final boolean value) {
        getWebServiceObject().setKeepForever(value);
    }

    /**
     * Gets or sets the label name. Corresponds to
     * <see cref="BuildUpdate.LabelName" />.
     *
     *
     * @return
     */
    public String getLabelName() {
        return getWebServiceObject().getLabelName();
    }

    public void setLabelName(final String value) {
        getWebServiceObject().setLabelName(value);
    }

    /**
     * Gets or sets the log location. Corresponds to
     * <see cref="BuildUpdate.LogLocation" />.
     *
     *
     * @return
     */
    public String getLogLocation() {
        return getWebServiceObject().getLogLocation();
    }

    public void setLogLocation(final String value) {
        getWebServiceObject().setLogLocation(value);
    }

    /**
     * Gets or sets the quality. Corresponds to
     * <see cref="BuildUpdate.Quality" />.
     *
     *
     * @return
     */
    public String getQuality() {
        return getWebServiceObject().getQuality();
    }

    public void setQuality(final String value) {
        getWebServiceObject().setQuality(value);
    }

    /**
     * Gets or sets the source get version. Corresponds to
     * <see cref="BuildUpdate.SourceGetVersion" />.
     *
     *
     * @return
     */
    public String getSourceGetVersion() {
        return getWebServiceObject().getSourceGetVersion();
    }

    public void setSourceGetVersion(final String value) {
        getWebServiceObject().setSourceGetVersion(value);
    }

    /**
     * Gets or sets the status. Corresponds to
     * <see cref="BuildUpdate.Status" />.
     *
     *
     * @return
     */
    public BuildStatus getStatus() {
        return BuildStatus.fromWebServiceObject(getWebServiceObject().getStatus());
    }

    public void setStatus(final BuildStatus value) {
        getWebServiceObject().setStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the test status. Corresponds to
     * <see cref="BuildUpdate.TestStatus" />.
     *
     *
     * @return
     */
    public BuildPhaseStatus getTestStatus() {
        return BuildPhaseStatus.fromWebServiceObject(getWebServiceObject().getTestStatus());
    }

    public void setTestStatus(final BuildPhaseStatus value) {
        getWebServiceObject().setTestStatus(value.getWebServiceObject());
    }

    /**
     * Gets or sets the URI of the build to update.
     *
     *
     * @return
     */
    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public void setURI(final String value) {
        getWebServiceObject().setUri(value);
    }
}
