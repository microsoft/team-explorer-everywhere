// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.util.Hierarchical;
import com.microsoft.tfs.core.util.Labelable;

/**
 * <p>
 * A node in the Classification Service Tree (i.e. Areas and Iterations).
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class CSSNode implements Hierarchical, Labelable {
    public static final char PATH_SEPERATOR = '\\';
    private CSSNode parent;

    private final List children = new ArrayList();
    private int level = 0;

    private String uri;

    private String name;
    private String parentUri;
    private String path;
    private String projectUri;

    private final CSSStructureType structureType;

    public CSSNode(final CSSStructureType structureType, final String uri) {
        this.structureType = structureType;

        synchronized (this) {
            this.uri = uri;
        }
    }

    public CSSNode(
        final CSSStructureType structureType,
        final String uri,
        final String name,
        final String parentUri,
        final String path,
        final String projectUri) {
        this(structureType, uri);

        synchronized (this) {
            this.name = name;
            this.parentUri = parentUri;
            this.path = path;
            this.projectUri = projectUri;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.util.Hierarchical#getChildren()
     */
    @Override
    public synchronized Object[] getChildren() {
        return children.toArray(new CSSNode[children.size()]);
    }

    public synchronized boolean removeChildNode(final CSSNode node) {
        return children.remove(node);
    }

    public synchronized int indexOfChild(final CSSNode node) {
        return children.indexOf(node);
    }

    public synchronized CSSNode removeChildAt(final int index) {
        return (CSSNode) children.remove(index);
    }

    public synchronized void addChildAt(final int index, final CSSNode child) {
        children.add(index, child);
    }

    public synchronized CSSNode getChildAt(final int index) {
        return (CSSNode) children.get(index);
    }

    public synchronized int getChildrenSize() {
        return children.size();
    }

    @Override
    public synchronized Object getParent() {
        return parent;
    }

    public synchronized CSSNode getParentNode() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean hasChildren() {
        return !children.isEmpty();
    }

    public synchronized boolean addChild(final CSSNode childNode) {
        childNode.setParent(this);
        childNode.setParentURI(getURI());
        // String oldPath = childNode.getPath();

        if (getPath() != null) {
            childNode.setPath(getPath() + PATH_SEPERATOR + childNode.getName());
        }

        // if (!childNode.getPath().equals(oldPath))
        // {
        // System.out.println("path changed from " + oldPath + " to " +
        // childNode.getPath());
        // }
        return children.add(childNode);
    }

    private synchronized void setParent(final CSSNode parent) {
        this.parent = parent;
    }

    @Override
    public synchronized String getLabel() {
        return getName();
    }

    public synchronized int getLevel() {
        return level;
    }

    public synchronized void setLevel(final int level) {
        this.level = level;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized void setName(final String name) {
        this.name = name;
    }

    public synchronized String getParentURI() {
        return parentUri;
    }

    public synchronized void setParentURI(final String parentUri) {
        this.parentUri = parentUri;
    }

    public synchronized String getPath() {
        return path;
    }

    public synchronized void setPath(final String path) {
        this.path = path;
    }

    public synchronized String getProjectURI() {
        return projectUri;
    }

    public synchronized void setProjectURI(final String projectUri) {
        this.projectUri = projectUri;
    }

    public synchronized String getURI() {
        return uri;
    }

    public synchronized void setURI(final String uri) {
        this.uri = uri;
    }

    public synchronized CSSStructureType getStructureType() {
        return structureType;
    }

    @Override
    public synchronized String toString() {
        return getName();
    }

    public static CSSNode resolveNode(final CSSNode parentNode, final String initialPath) {
        if (parentNode.getPath().equals(initialPath)) {
            return parentNode;
        }

        CSSNode node = parentNode;
        if (initialPath.startsWith(parentNode.getPath()) && parentNode.hasChildren()) {
            final CSSNode[] children = (CSSNode[]) parentNode.getChildren();
            for (int i = 0; i < children.length; i++) {
                if (initialPath.equals(children[i].getPath())) {
                    return children[i];
                }
                if (initialPath.startsWith(children[i].getPath() + PATH_SEPERATOR)) {
                    node = resolveNode(children[i], initialPath);
                    break;
                }
            }
        }
        return node;
    }

}
