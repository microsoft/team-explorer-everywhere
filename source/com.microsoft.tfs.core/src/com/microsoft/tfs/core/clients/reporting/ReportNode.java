// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.reporting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.util.Hierarchical;
import com.microsoft.tfs.core.util.Labelable;

import ms.sql.reporting.reportingservices._CatalogItem;

/**
 * A node (report, folder, etc.) in the report hierarchy.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public abstract class ReportNode extends WebServiceObjectWrapper implements Labelable, Hierarchical {
    private final String projectName;

    private Object parent;
    private final List<ReportNode> children = new ArrayList<ReportNode>();

    /**
     * Creates a {@link ReportNode} with only a path, no other fields
     * initialized (they will be their default values). This constructor is
     * provided for use by {@link ReportFolder}.
     *
     * @param path
     *        the path (must not be <code>null</code>)
     */
    protected ReportNode(final String projectName, final String path) {
        this(projectName, newCatalogItemWithOnlyPath(path));
    }

    protected ReportNode(final String projectName, final _CatalogItem item) {
        super(item);
        this.projectName = projectName;
    }

    /**
     * Assists the {@link #ReportNode(String)} constructor.
     */
    private static _CatalogItem newCatalogItemWithOnlyPath(final String path) {
        final _CatalogItem item = new _CatalogItem();
        item.setPath(path);
        return item;
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CatalogItem getWebServiceObject() {
        return (_CatalogItem) webServiceObject;
    }

    /**
     * @return Returns the createdBy.
     */
    public String getCreatedBy() {
        return getWebServiceObject().getCreatedBy();
    }

    /**
     * @return Returns the creationDate.
     */
    public Calendar getCreationDate() {
        return getWebServiceObject().getCreationDate();
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    /**
     * @return Returns the ID.
     */
    public String getID() {
        return getWebServiceObject().getID();
    }

    /**
     * @return Returns the hidden.
     */
    public boolean isHidden() {
        return getWebServiceObject().isHidden();
    }

    /**
     * @return Returns the label.
     */
    @Override
    public String getLabel() {
        return getWebServiceObject().getName();
    }

    /**
     * @return Returns the modifiedBy.
     */
    public String getModifiedBy() {
        return getWebServiceObject().getModifiedBy();
    }

    /**
     * @return Returns the modifiedDate.
     */
    public Calendar getModifiedDate() {
        return getWebServiceObject().getModifiedDate();
    }

    /**
     * @return Returns the path.
     */
    public String getPath() {
        return getWebServiceObject().getPath();
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return getWebServiceObject().getType().toString();
    }

    /**
     * To string method overloaded to return the path of the report.
     */
    @Override
    public String toString() {
        return getPath();
    }

    /**
     * @return Returns the parent.
     */
    @Override
    public Object getParent() {
        return parent;
    }

    /**
     * @param parent
     *        The parent to set.
     */
    public void setParent(final Object parent) {
        this.parent = parent;
    }

    public void addChild(final ReportNode child) {
        children.add(child);
    }

    public ReportNode[] getChildReportNodes() {
        return children.toArray(new ReportNode[children.size()]);
    }

    @Override
    public Object[] getChildren() {
        return children.toArray();
    }

    @Override
    public boolean hasChildren() {
        return children.size() > 0;
    }

    public String getProjectName() {
        return projectName;
    }
}
