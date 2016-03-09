// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild;

import org.eclipse.core.runtime.IAdapterFactory;

import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;

/**
 * Adapter factory to convert common team build types.
 */
public class TeamBuildAdapterFactory implements IAdapterFactory {
    private static Class[] SUPPORTED_TYPES = new Class[] {
        IBuildDefinition.class,
        IBuildDetail.class,
        TeamProject.class
    };

    /**
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object,
     *      java.lang.Class)
     */
    @Override
    public Object getAdapter(final Object adaptableObject, final Class adapterType) {
        if (IBuildDefinition.class.equals(adapterType)) {
            if (adaptableObject instanceof IBuildDetail) {
                return ((IBuildDetail) adaptableObject).getBuildDefinition();
            }
            if (adaptableObject instanceof IQueuedBuild) {
                return ((IQueuedBuild) adaptableObject).getBuildDefinition();
            }
        }
        if (IBuildDetail.class.equals(adapterType)) {
            if (adaptableObject instanceof IQueuedBuild) {
                return ((IQueuedBuild) adaptableObject).getBuild();
            }
        }
        if (TeamProject.class.equals(adapterType)) {
            if (adaptableObject instanceof IBuildDetail) {
                final IBuildDetail build = ((IBuildDetail) adaptableObject);
                if (build.getBuildDefinition() == null) {
                    return null;
                }
                return new TeamProject(build.getBuildServer(), build.getBuildDefinition().getTeamProject());
            }
            if (adaptableObject instanceof IQueuedBuild) {
                final IQueuedBuild build = ((IQueuedBuild) adaptableObject);
                if (build.getBuildDefinition() == null) {
                    return null;
                }
                return new TeamProject(build.getBuildServer(), build.getBuildDefinition().getTeamProject());
            }
        }
        return null;
    }

    /**
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
     */
    @Override
    public Class[] getAdapterList() {
        return SUPPORTED_TYPES;
    }
}
