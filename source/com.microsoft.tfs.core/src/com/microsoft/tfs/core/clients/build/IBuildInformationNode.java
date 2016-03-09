// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Calendar;
import java.util.Map;

public interface IBuildInformationNode {
    /**
     * The children of the information node.
     *
     *
     * @return
     */
    public IBuildInformation getChildren();

    /**
     * The custom name / value pairs associated with the information node.
     *
     *
     * @return
     */
    public Map<String, String> getFields();

    /**
     * The unique identifier of the information node.
     *
     *
     * @return
     */
    public int getID();

    /**
     * The user who last modified the information node.
     *
     *
     * @return
     */
    public String getLastModifiedBy();

    /**
     * The date at which the information node was last modified.
     *
     *
     * @return
     */
    public Calendar getLastModifiedDate();

    /**
     * The parent of the information node.
     *
     *
     * @return
     */
    public IBuildInformationNode getParent();

    /**
     * The type of the information node.
     *
     *
     * @return
     */
    public String getType();

    public void setType(String value);

    /**
     * Deletes the information node from the server.
     *
     *
     */
    public void delete();

    /**
     * Persists any changes to the information node (and its children) to the
     * server.
     *
     *
     */
    public void save();
}
