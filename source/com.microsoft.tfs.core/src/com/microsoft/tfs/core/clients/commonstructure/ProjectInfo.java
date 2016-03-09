// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.classification._03._ProjectInfo;

/**
 * <p>
 * Contains information about a project.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class ProjectInfo extends WebServiceObjectWrapper {
    private SourceControlCapabilityFlags sourceControlCapabilityFlags = SourceControlCapabilityFlags.NONE;

    public ProjectInfo(final String name, final String uri) {
        super(new _ProjectInfo());
        getWebServiceObject().setName(name);
        getWebServiceObject().setUri(uri);
    }

    public ProjectInfo(final _ProjectInfo projectInfo) {
        super(projectInfo);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ProjectInfo getWebServiceObject() {
        return (_ProjectInfo) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }

    public String getGUID() {
        final String uri = getWebServiceObject().getUri();

        // uri looks like:
        // vstfs:///Classification/TeamProject/3406fa58-0815-4d9e-8
        // e4f-1d33b1a8a239

        return uri.substring(uri.lastIndexOf('/') + 1);
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    public SourceControlCapabilityFlags getSourceControlCapabilityFlags() {
        return sourceControlCapabilityFlags;
    }

    public void setSourceControlCapabilityFlags(final SourceControlCapabilityFlags sourceControlCapabilityFlags) {
        this.sourceControlCapabilityFlags = sourceControlCapabilityFlags;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return MessageFormat.format("project \"{0}\" ({1})", getName(), getGUID()); //$NON-NLS-1$
    }
}
