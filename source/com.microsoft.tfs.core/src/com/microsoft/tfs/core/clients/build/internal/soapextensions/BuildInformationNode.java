// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.microsoft.tfs.core.clients.build.IBuildInformation;
import com.microsoft.tfs.core.clients.build.IBuildInformationNode;
import com.microsoft.tfs.core.clients.build.InformationNodeConverters;
import com.microsoft.tfs.core.clients.build.flags.InformationEditOptions;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._04._BuildInformationNode;
import ms.tfs.build.buildservice._04._InformationField;

public class BuildInformationNode extends WebServiceObjectWrapper
    implements IBuildInformationNode, Comparable<BuildInformationNode> {
    BuildDetail build;
    BuildInformation children;
    Map<String, String> fields;
    Map<String, String> lastSnapshot;
    BuildInformationNode parent;
    BuildInformation owner;
    Object lock = new Object();
    Object lockFields = new Object();

    private BuildInformationNode() {
        super(new _BuildInformationNode());

        getWebServiceObject().setFields(new _InformationField[0]);
        getWebServiceObject().setLastModifiedDate(DotNETDate.MIN_CALENDAR);
    }

    public BuildInformationNode(final _BuildInformationNode webServiceObject) {
        super(webServiceObject);

        afterDeserialize();
    }

    /**
     * Creates a new build information node with default values.
     *
     *
     * @param build
     *        The build that owns the new information node.
     * @param parent
     *        The parent that owns the new information node.
     */
    public BuildInformationNode(final BuildDetail build, final BuildInformationNode parent) {
        this();
        setID(getNextTempID());

        this.build = build;
        this.parent = parent;

        if (parent != null) {
            owner = (BuildInformation) parent.getChildren();
        }
    }

    /**
     * Creates a new build information node with default values.
     *
     *
     * @param build
     *        The build that owns the new information node.
     * @param collection
     *        The collection that owns the new information node.
     */
    public BuildInformationNode(final BuildDetail build, final BuildInformation collection) {
        this(build, (BuildInformationNode) null);
        this.owner = collection;
    }

    public BuildInformationNode(final BuildInformationNode2010 node2010) {
        this();

        getWebServiceObject().setFields(
            (_InformationField[]) WrapperUtils.unwrap(
                _InformationField.class,
                TFS2010Helper.convert(node2010.getInternalFields())));
        getWebServiceObject().setLastModifiedBy(node2010.getLastModifiedBy());
        getWebServiceObject().setLastModifiedDate(node2010.getLastModifiedDate());
        getWebServiceObject().setNodeId(node2010.getNodeID());
        getWebServiceObject().setParentId(node2010.getParentID());
        getWebServiceObject().setType(node2010.getType());

        afterDeserialize();
    }

    public void afterDeserialize() {
        fields = informationFieldsToDictionary(getInternalFields());
        synchronized (lockFields) {
            lastSnapshot = copyDictionary(getFields());
        }
    }

    public _BuildInformationNode getWebServiceObject() {
        return (_BuildInformationNode) this.webServiceObject;
    }

    /**
     * Gets the associated fields.
     *
     *
     * @return
     */
    public InformationField[] getInternalFields() {
        return (InformationField[]) WrapperUtils.wrap(InformationField.class, getWebServiceObject().getFields());
    }

    /**
     * Gets the domain user name of the user that made the last modification.
     * This field is read-only. {@inheritDoc}
     */
    @Override
    public String getLastModifiedBy() {
        return getWebServiceObject().getLastModifiedBy();
    }

    /**
     * Gets the last modification date. This field is read-only. {@inheritDoc}
     */
    @Override
    public Calendar getLastModifiedDate() {
        return getWebServiceObject().getLastModifiedDate();
    }

    /**
     * Gets or sets the ID.
     *
     *
     * @return
     */
    public int getNodeID() {
        return getWebServiceObject().getNodeId();
    }

    public void setNodeID(final int value) {
        getWebServiceObject().setNodeId(value);
    }

    /**
     * Gets or sets the parent node ID.
     *
     *
     * @return
     */
    public int getParentID() {
        return getWebServiceObject().getParentId();
    }

    public void setParentID(final int value) {
        getWebServiceObject().setParentId(value);
    }

    /**
     * Gets or sets the information node type. {@inheritDoc}
     */
    @Override
    public String getType() {
        return getWebServiceObject().getType();
    }

    @Override
    public void setType(final String value) {
        getWebServiceObject().setType(value);
    }

    /**
     * The children of this information node. {@inheritDoc}
     */
    @Override
    public IBuildInformation getChildren() {
        if (children == null) {
            synchronized (lock) {
                if (children == null) {
                    children = new BuildInformation(build, this);
                }
            }
        }
        return children;
    }

    /**
     * The custom name / value pairs associated with this information node.
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getFields() {
        synchronized (lockFields) {
            if (fields == null) {
                fields = new TreeMap<String, String>();
            }

            return fields;
        }
    }

    /**
     * The unique identifier of this information node. {@inheritDoc}
     */
    @Override
    public int getID() {
        return getWebServiceObject().getNodeId();
    }

    public void setID(final int value) {
        getWebServiceObject().setNodeId(value);
    }

    /**
     * The parent of this information node. {@inheritDoc}
     */
    @Override
    public IBuildInformationNode getParent() {
        return parent;
    }

    public void setParent(final IBuildInformationNode value) {
        parent = (BuildInformationNode) value;

        if (parent == null) {
            getWebServiceObject().setParentId(0);
        } else {
            getWebServiceObject().setParentId(value.getID());
        }
    }

    /**
     * Deletes this information node (and its children) from the server.
     * {@inheritDoc}
     */
    @Override
    public void delete() {
        // TODO_VNEXT: Batch up the deletes (i.e. don't call
        // UpdateBuildInformation here) as well as the Adds and Edits.
        if (getID() > 0) {
            final InformationDeleteRequest request = new InformationDeleteRequest();
            request.setBuildURI(build.getURI());
            request.setNodeID(getID());

            if (build.getBuildServer().getBuildServerVersion().isV2()) {
                build.getInternalBuildServer().getBuild2008Helper().updateBuildInformation(
                    new InformationChangeRequest[] {
                        request
                });
            } else if (build.getBuildServer().getBuildServerVersion().isV3()) {
                build.getInternalBuildServer().getBuild2010Helper().updateBuildInformation(
                    new InformationChangeRequest[] {
                        request
                });
            } else {
                build.getInternalBuildServer().getBuildService().updateBuildInformation(new InformationChangeRequest[] {
                    request
                });
            }
        }

        owner.deleteNode(this);
    }

    /**
     * Persists any changes to this information node (and its children) to the
     * server. {@inheritDoc}
     */
    @Override
    public void save() {
        synchronized (build.syncSave) {
            InformationNodeConverters.bulkUpdateInformationNodes(build, getRequests(true));
        }
    }

    @Override
    public int compareTo(final BuildInformationNode node) {
        return BuildInformationNodeComparer.getInstance().compare(this, node);
    }

    public BuildDetail getBuild() {
        return build;
    }

    public void setBuild(final BuildDetail value) {
        build = value;
    }

    /**
     * Returns true if something has changed since we were last saved, and false
     * otherwise.
     *
     *
     * @return true if something has changed since we were last saved, false
     *         otherwise.
     */
    public boolean isDirty() {
        // If we have new fields (or deleted fields), we are dirty.
        if (fields.size() != lastSnapshot.size()) {
            return true;
        }

        // If one of our fields no longer exists or has a new value, we are
        // dirty.
        for (final Entry<String, String> entry : getFields().entrySet()) {
            final String snapshotValue = lastSnapshot.get(entry.getKey());
            if (snapshotValue == null || !snapshotValue.equals(entry.getValue())) {
                return true;
            }
        }

        // Otherwise, we are not dirty.
        return false;
    }

    public InformationAddRequest createAddRequest() {
        // Update last snapshot to reflect this request.
        synchronized (lockFields) {
            lastSnapshot = copyDictionary(getFields());
        }

        final InformationAddRequest addRequest = new InformationAddRequest();
        addRequest.setBuildURI(build.getURI());
        addRequest.setFields(dictionaryToInformationFields(lastSnapshot));
        addRequest.setNodeID(getID());
        addRequest.setNodeType(getType());
        if (getParent() != null) {
            addRequest.setParentID(parent.getID());
        }
        addRequest.setNode(this);

        return addRequest;
    }

    /**
     * Gets all of the add and edit requests for the subtree rooted at this
     * node.
     *
     *
     * @param getUnsavedParentNodes
     * @return A list of add and edit requests.
     */
    public List<InformationChangeRequest> getRequests(final boolean getUnsavedParentNodes) {
        final List<InformationChangeRequest> result = new ArrayList<InformationChangeRequest>();

        // Id less than zero indicates a temporary Id - need to create an Add
        // request.
        if (getID() < 0) {
            // Walk up the tree to get all the ancestor nodes that have not been
            // saved yet until we reach a node that has been saved or we reach
            // the root node
            if (getUnsavedParentNodes) {
                IBuildInformationNode unsavedParent = getParent();
                while (unsavedParent != null && unsavedParent.getID() < 0) {
                    result.add(((BuildInformationNode) unsavedParent).createAddRequest());
                    unsavedParent = unsavedParent.getParent();
                }
            }

            result.add(createAddRequest());
        } else if (isDirty()) {
            synchronized (lockFields) {
                // Update last snapshot to reflect this request.
                lastSnapshot = copyDictionary(getFields());
            }

            // We have changed since the last time we were saved - need to
            // create an Edit request.
            final InformationEditRequest editRequest = new InformationEditRequest();
            editRequest.setBuildURI(build.getURI());
            editRequest.setFields(dictionaryToInformationFields(lastSnapshot));
            editRequest.setNodeID(getID());
            editRequest.setOptions(InformationEditOptions.REPLACE_FIELDS);
            result.add(editRequest);
        }

        for (final IBuildInformationNode node : getChildren().getNodes()) {

            result.addAll(((BuildInformationNode) node).getRequests(false));
        }

        return result;
    }

    /**
     * Convert an array of information fields to a dictionary.
     *
     *
     * @param fields
     *        The array of information fields to convert.
     * @return The new dictionary.
     */
    private Map<String, String> informationFieldsToDictionary(final InformationField[] fields) {
        final Map<String, String> result = new TreeMap<String, String>();

        for (final InformationField field : fields) {
            result.put(field.getName(), field.getValue());
        }

        return result;
    }

    /**
     * Convert a dictionary to an array of information fields.
     *
     *
     * @param map
     *        The dictionary to convert.
     * @return The new array of information fields.
     */
    private InformationField[] dictionaryToInformationFields(final Map<String, String> map) {
        final List<InformationField> fieldList = new ArrayList<InformationField>();

        if (map != null) {
            for (final Entry<String, String> entry : map.entrySet()) {
                fieldList.add(new InformationField(entry.getKey(), entry.getValue()));
            }
        }

        return fieldList.toArray(new InformationField[fieldList.size()]);
    }

    /**
     * Copy a dictionary. This, in itself, is not thread-safe.
     *
     *
     * @param map
     *        The dictionary to copy.
     * @return The new dictionary.
     */
    private Map<String, String> copyDictionary(final Map<String, String> map) {
        final Map<String, String> result = new TreeMap<String, String>();

        if (map != null) {
            for (final Entry<String, String> entry : map.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    public BuildInformation getOwner() {
        return owner;
    }

    public void setOwner(final BuildInformation value) {
        owner = value;
    }

    public static int getNextTempID() {
        synchronized (s_lockTempID) {
            return --s_nextTempID;
        }
    }

    private static int s_nextTempID = 0;
    private static Object s_lockTempID = new Object();
}
