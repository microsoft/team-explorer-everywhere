// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.services.registration._03._FrameworkRegistrationEntry;
import ms.tfs.services.registration._03._RegistrationArtifactType;
import ms.tfs.services.registration._03._RegistrationDatabase;
import ms.tfs.services.registration._03._RegistrationEventType;
import ms.tfs.services.registration._03._RegistrationExtendedAttribute2;
import ms.tfs.services.registration._03._RegistrationServiceInterface;

/**
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class RegistrationEntry extends WebServiceObjectWrapper {
    public RegistrationEntry(
        final String type,
        final ServiceInterface[] serviceInterfaces,
        final Database[] databases,
        final EventType[] eventTypes,
        final ArtifactType[] artifactTypes,
        final RegistrationExtendedAttribute[] registrationExtendedAttributes) {
        /*
         * This is kind of ugly, but we have a lot of array fields which we
         * unwrap.
         */
        super(
            new _FrameworkRegistrationEntry(
                type,
                (_RegistrationServiceInterface[]) WrapperUtils.unwrap(
                    _RegistrationServiceInterface.class,
                    serviceInterfaces),
                (_RegistrationDatabase[]) WrapperUtils.unwrap(_RegistrationDatabase.class, databases),
                (_RegistrationEventType[]) WrapperUtils.unwrap(_RegistrationEventType.class, eventTypes),
                (_RegistrationArtifactType[]) WrapperUtils.unwrap(_RegistrationArtifactType.class, artifactTypes),
                (_RegistrationExtendedAttribute2[]) WrapperUtils.unwrap(
                    _RegistrationExtendedAttribute2.class,
                    registrationExtendedAttributes)));
    }

    public RegistrationEntry(final _FrameworkRegistrationEntry entry) {
        super(entry);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _FrameworkRegistrationEntry getWebServiceObject() {
        return (_FrameworkRegistrationEntry) webServiceObject;
    }

    public ArtifactType[] getArtifactTypes() {
        return (ArtifactType[]) WrapperUtils.wrap(ArtifactType.class, getWebServiceObject().getArtifactTypes());
    }

    public Database[] getDatabases() {
        return (Database[]) WrapperUtils.wrap(Database.class, getWebServiceObject().getDatabases());
    }

    public EventType[] getEventTypes() {
        return (EventType[]) WrapperUtils.wrap(EventType.class, getWebServiceObject().getEventTypes());
    }

    public RegistrationExtendedAttribute[] getRegistrationExtendedAttributes() {
        return (RegistrationExtendedAttribute[]) WrapperUtils.wrap(
            RegistrationExtendedAttribute.class,
            getWebServiceObject().getRegistrationExtendedAttributes());
    }

    public ServiceInterface[] getServiceInterfaces() {
        return (ServiceInterface[]) WrapperUtils.wrap(
            ServiceInterface.class,
            getWebServiceObject().getServiceInterfaces());
    }

    public String getType() {
        return getWebServiceObject().getType();
    }
}
