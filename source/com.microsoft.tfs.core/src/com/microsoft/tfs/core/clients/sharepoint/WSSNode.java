// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.util.Hierarchical;
import com.microsoft.tfs.core.util.Labelable;

/**
 * An object (document, folder, etc.) in a Sharepoint installation.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class WSSNode implements Hierarchical, Labelable, Comparable {
    private static final Log log = LogFactory.getLog(WSSNode.class);

    private Object parent;
    private final List children = new ArrayList();

    private String wssObjectType;
    private String fullPath;
    private String label;
    private String editor;
    private String path;

    private boolean childrenSorted = false;

    public WSSNode() {
        super();
    }

    public WSSNode(final Element element) {
        wssObjectType = WSSUtils.decodeWSSString(element.getAttribute("ows_FSObjType")); //$NON-NLS-1$
        fullPath = WSSUtils.decodeWSSString(element.getAttribute("ows_FileRef")); //$NON-NLS-1$
        label = WSSUtils.decodeWSSString(element.getAttribute("ows_FileLeafRef")); //$NON-NLS-1$
        editor = WSSUtils.decodeWSSString(element.getAttribute("ows_Editor")); //$NON-NLS-1$
        path = fullPath.substring(0, (fullPath.length() - label.length()) - 1);
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

    public void addChild(final Object child) {
        if (child instanceof WSSNode) {
            ((WSSNode) child).setParent(this);
        }
        children.add(child);
        childrenSorted = false;

    }

    @Override
    public Object[] getChildren() {
        if (!childrenSorted) {
            Collections.sort(children);
            childrenSorted = true;
        }

        return children.toArray();
    }

    @Override
    public boolean hasChildren() {
        return children.size() > 0;
    }

    /**
     * @return Returns the editor.
     */
    public String getEditor() {
        return editor;
    }

    /**
     * @param editor
     *        The editor to set.
     */
    public void setEditor(final String editor) {
        this.editor = editor;
    }

    /**
     * @return Returns the fullPath.
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * @param fullPath
     *        The fullPath to set.
     */
    public void setFullPath(final String fullPath) {
        this.fullPath = fullPath;
    }

    /**
     * @return Returns the wssObjectType.
     */
    public String getWSSObjectType() {
        return wssObjectType;
    }

    /**
     * @param wssObjectType
     *        The wssObjectType to set.
     */
    public void setWSSObjectType(final String wssObjectType) {
        this.wssObjectType = wssObjectType;
    }

    /**
     * @return Returns the label.
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * @param label
     *        The label to set.
     */
    public void setLabel(final String label) {
        this.label = label;
    }

    /**
     * Create a strongly typed WssNode from data passed back from the WSS Web
     * Service.
     *
     * @return WssDocument or WssFolder representing point on node.
     */
    public static WSSNode buildWSSNode(final Element element) {
        final String wssObjType = WSSUtils.decodeWSSString(element.getAttribute("ows_FSObjType")); //$NON-NLS-1$
        if (WSSObjectType.FILE.equals(wssObjType)) {
            return new WSSDocument(element);
        }
        if (WSSObjectType.FOLDER.equals(wssObjType)) {
            return new WSSFolder(element);
        }
        return new WSSNode(element);
    }

    /**
     * @return Returns the path.
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     *        The path to set.
     */
    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public int compareTo(final Object compare) {
        if (!(compare instanceof WSSNode)) {
            throw new ClassCastException(
                MessageFormat.format(
                    "A WssNode to compare against was expected, class passed was {0}", //$NON-NLS-1$
                    compare.getClass().getName()));
        }
        final WSSNode anotherNode = (WSSNode) compare;
        if (fullPath.equals(anotherNode.getFullPath())) {
            return 0;
        }
        if (WSSObjectType.FOLDER.equals(getWSSObjectType())
            && WSSObjectType.FILE.equals(anotherNode.getWSSObjectType())) {
            return -1;
        }
        if (WSSObjectType.FILE.equals(getWSSObjectType())
            && WSSObjectType.FOLDER.equals(anotherNode.getWSSObjectType())) {
            return 1;
        }
        return getFullPath().compareTo(anotherNode.getFullPath());
    }

}
