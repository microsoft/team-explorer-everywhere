// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

public interface IBuildGroupItem {
    /**
     * The URI of the build group item.
     *
     *
     * @return
     */
    public String getURI();

    /**
     * The name of the build group item.
     *
     *
     * @return
     */
    public String getName();

    public void setName(String value);

    /**
     * The team project that owns the build group item.
     *
     *
     * @return
     */
    public String getTeamProject();

    /**
     * The full path of this build group item.
     *
     *
     * @return
     */
    public String getFullPath();

    /**
     * Refreshes the build group item by getting current property values from
     * the build server.
     *
     *
     */
    public void refresh();
}
