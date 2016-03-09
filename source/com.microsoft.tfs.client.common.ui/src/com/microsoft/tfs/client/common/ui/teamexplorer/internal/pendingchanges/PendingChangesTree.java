// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal.pendingchanges;

import java.io.File;
import java.text.MessageFormat;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;

public abstract class PendingChangesTree {
    private static final Log log = LogFactory.getLog(PendingChangesTree.class);

    private static final String LOCAL_PATH_SEPARATOR = File.separator;
    private static final String SERVER_PATH_SEPARATOR = Character.toString(ServerPath.PREFERRED_SEPARATOR_CHARACTER);

    private static final String QUOTED_LOCAL_PATH_SEPARATOR = Pattern.quote(LOCAL_PATH_SEPARATOR);
    private static final String QUOTED_SERVER_PATH_SEPARATOR = Pattern.quote(SERVER_PATH_SEPARATOR);

    private final PendingChangesTreeNode root = new PendingChangesTreeNode(""); //$NON-NLS-1$

    public abstract PendingChangesTreeNode createTreeNode(final String subpath);

    public PendingChangesTreeNode[] getRoots() {
        return root.getChildren();
    }

    public boolean isEmpty() {
        return root.getChildren().length == 0;
    }

    public void addPendingChange(final PendingChange pendingChange) {
        final String localPath;
        final String serverPath;

        final String[] segments;
        if ((localPath = pendingChange.getLocalItem()) != null) {
            segments = localPath.split(QUOTED_LOCAL_PATH_SEPARATOR);
        } else if ((serverPath = pendingChange.getServerItem()) != null) {
            segments = serverPath.split(QUOTED_SERVER_PATH_SEPARATOR);
        } else {
            log.warn(MessageFormat.format(
                "Pending change ''{0}'' has neither server nor local path, skipping", //$NON-NLS-1$
                pendingChange));
            return;
        }

        PendingChangesTreeNode currentNode = root;

        for (final String segment : segments) {
            final PendingChangesTreeNode existingNode = currentNode.findChild(segment);
            if (existingNode == null) {
                final PendingChangesTreeNode newNode = createTreeNode(segment);
                currentNode.addChild(newNode);
                currentNode = newNode;
            } else {
                currentNode = existingNode;
            }
        }

        currentNode.setPendingChange(pendingChange);
    }

    public void collapseRedundantLevels() {
        for (final PendingChangesTreeNode rootNode : root.getChildren()) {
            final String separator;
            if (rootNode.getSubpath().equals(ServerPath.ROOT_NAME_ONLY)) {
                separator = SERVER_PATH_SEPARATOR;
            } else {
                separator = LOCAL_PATH_SEPARATOR;
            }

            collapseRedundantLevels(rootNode, separator);
        }
    }

    private void collapseRedundantLevels(final PendingChangesTreeNode node, final String separator) {
        while (node.childCount() == 1 && !node.getChild(0).isLeaf() && node.getChild(0).getPendingChange() == null) {
            node.collapseRedundantChild(separator);
        }

        for (final PendingChangesTreeNode childNode : node.getChildren()) {
            collapseRedundantLevels(childNode, separator);
        }
    }
}
