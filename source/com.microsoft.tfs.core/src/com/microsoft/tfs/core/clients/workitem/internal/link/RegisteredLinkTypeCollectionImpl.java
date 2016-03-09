// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.registration.OutboundLinkType;
import com.microsoft.tfs.core.clients.registration.RegistrationClient;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemConstants;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeCollection;

public class RegisteredLinkTypeCollectionImpl implements RegisteredLinkTypeCollection {
    private final List<RegisteredLinkType> types = new ArrayList<RegisteredLinkType>();
    private final Map<String, RegisteredLinkType> nameToType = new HashMap<String, RegisteredLinkType>();

    public RegisteredLinkTypeCollectionImpl(final WITContext context) {
        final RegistrationClient registrationClient = context.getConnection().getRegistrationClient();

        final OutboundLinkType[] outboundLinkTypes = registrationClient.getOutboundLinkTypes(
            ToolNames.WORK_ITEM_TRACKING,
            InternalWorkItemConstants.WORK_ITEM_ARTIFACT_TYPE);

        for (int i = 0; i < outboundLinkTypes.length; i++) {
            final RegisteredLinkTypeImpl linkType = new RegisteredLinkTypeImpl(outboundLinkTypes[i].getName());
            types.add(linkType);
            nameToType.put(linkType.getName(), linkType);
        }
    }

    @Override
    public String toString() {
        return types.toString();
    }

    /***************************************************************************
     * START of implementation of RegisteredLinkTypeCollection interface
     **************************************************************************/

    @Override
    public Iterator<RegisteredLinkType> iterator() {
        return types.iterator();
    }

    @Override
    public boolean contains(final RegisteredLinkType linkType) {
        return types.contains(linkType);
    }

    @Override
    public int size() {
        return types.size();
    }

    @Override
    public RegisteredLinkType get(final String name) {
        return nameToType.get(name);
    }

    /***************************************************************************
     * END of implementation of RegisteredLinkTypeCollection interface
     **************************************************************************/
}
