// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core;

import java.util.UUID;

public abstract class CoreConstants {
    // Resources
    public final static String ProjectsResource = "projects"; //$NON-NLS-1$
    public final static String ProjectCollectionsResource = "projectCollections"; //$NON-NLS-1$
    public final static String TeamsResource = "teams"; //$NON-NLS-1$
    public final static String TeamMembersResource = "members"; //$NON-NLS-1$
    public final static String ConnectedServicesResource = "connectedServices"; //$NON-NLS-1$
    public final static String ProxyResource = "proxies"; //$NON-NLS-1$

    // Route Names
    public final static String ProjectCollectionsRouteName = "ProjectCollections"; //$NON-NLS-1$
    public final static String ProjectsRouteName = "Projects"; //$NON-NLS-1$
    public final static String TeamsRouteName = "Teams"; //$NON-NLS-1$
    public final static String TeamMembersRouteName = "TeamMembers"; //$NON-NLS-1$
    public final static String ConnectedServicesRouteName = "ConnectedServices"; //$NON-NLS-1$
    public final static String ProxyRouteName = "Proxies"; //$NON-NLS-1$

    // Location UUIDs
    public final static UUID ProjectCollectionsLocationId = UUID.fromString("8031090F-EF1D-4AF6-85FC-698CD75D42BF"); //$NON-NLS-1$
    public final static UUID ProjectsLocationId = UUID.fromString("603FE2AC-9723-48B9-88AD-09305AA6C6E1"); //$NON-NLS-1$
    public final static UUID TeamsLocationId = UUID.fromString("D30A3DD1-F8BA-442A-B86A-BD0C0C383E59"); //$NON-NLS-1$
    public final static UUID TeamMembersLocationId = UUID.fromString("294C494C-2600-4D7E-B76C-3DD50C3C95BE"); //$NON-NLS-1$
    public final static UUID ConnectedServices = UUID.fromString("B4F70219-E18B-42C5-ABE3-98B07D35525E"); //$NON-NLS-1$
    public final static UUID ProxyId = UUID.fromString("EC1F4311-F2B4-4C15-B2B8-8990B80D2908"); //$NON-NLS-1$

}
