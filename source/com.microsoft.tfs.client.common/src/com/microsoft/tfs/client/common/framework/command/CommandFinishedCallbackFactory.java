// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

/**
 * Helper class for command finished callback management.
 *
 * @threadsafety unknown
 */
public class CommandFinishedCallbackFactory {
    protected CommandFinishedCallbackFactory() {
    }

    public static ICommandFinishedCallback getDefaultCallback() {
        return MultiCommandFinishedCallback.combine(getPlatformLogCallback(), getTeamExplorerLogCallback());
    }

    /**
     * Provides a command finished callback that will log to the Platform
     * ("Eclipse") log.
     *
     * @return An {@link ICommandFinishedCallback} that will log to the platform
     *         log.
     */
    public static final ICommandFinishedCallback getPlatformLogCallback() {
        return new PlatformLogCommandFinishedCallback();
    }

    /**
     * Provides a command finished callback that will log to the Team Explorer
     * ("product" or "private") log.
     *
     * @return An {@link ICommandFinishedCallback} that will log to the Team
     *         Explorer log.
     */
    public static final ICommandFinishedCallback getTeamExplorerLogCallback() {
        return new TeamExplorerLogCommandFinishedCallback();
    }
}
