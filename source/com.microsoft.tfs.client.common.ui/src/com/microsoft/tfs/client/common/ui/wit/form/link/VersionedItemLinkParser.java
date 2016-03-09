// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.link;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.workitem.link.VersionedItemLinkTypeNames;
import com.microsoft.tfs.util.Check;

public final class VersionedItemLinkParser {
    private static final Log log = LogFactory.getLog(VersionedItemLinkParser.class);

    /**
     * IMPLEMENTATION DETAIL (keep private) The encoding to use when URL
     * encoding/decoding. This must match {@link ArtifactID}.
     */
    private static final String URL_ENCODING = "UTF-8"; //$NON-NLS-1$

    private VersionedItemLinkParser() {
    }

    /**
     * Parses the tool-specific ID out of a versioned item {@link ArtifactID}
     * and returns the item path, changeset and deletion id referred to by this
     * artifact.
     *
     * @throws RuntimeException
     *         if the artifact could not be parsed
     * @param versionedArtifactId
     *        An {@link ArtifactID} of type VersionedItemLink or LatestVersion
     *        (not <code>null</code>)
     * @return A {@link VersionedItemLinkData} with the appropriate data, never
     *         <code>null</code>
     */
    public static VersionedItemLinkData parse(final ArtifactID versionedArtifactId) {
        Check.notNull(versionedArtifactId, "versionedArtifactId"); //$NON-NLS-1$

        if (!VersionedItemLinkTypeNames.VERSIONED_ITEM.equals(versionedArtifactId.getArtifactType())) {
            throw new RuntimeException("Artifact is not a versioned item artifact"); //$NON-NLS-1$
        }
        if (versionedArtifactId.getToolSpecificID() == null || versionedArtifactId.getToolSpecificID().length() == 0) {
            throw new RuntimeException("Invalid versioned item ID for artifact"); //$NON-NLS-1$
        }

        try {
            final String decodedToolId = URLDecoder.decode(versionedArtifactId.getToolSpecificID(), URL_ENCODING);

            /*
             * The tool ID should contain at least three segments - the first
             * segment must be the item path, and then attributes
             * (changesetVersion= and deletionId= are currently the only
             * attributes known, and changesetVersion is required. Others are
             * ignored for forward compatibility).
             *
             * The item path itself is *also* URI encoded to allow us to split
             * the tool id properly (so that we don't split parts of the item
             * that contain an ampersand.)
             */
            final String[] toolComponents = decodedToolId.split("&"); //$NON-NLS-1$

            final String itemPath = "$/" + URLDecoder.decode(toolComponents[0], URL_ENCODING); //$NON-NLS-1$
            int changesetVersion = -1;
            int deletionId = 0;

            for (int i = 1; i < toolComponents.length; i++) {
                if (toolComponents[i].startsWith("changesetVersion=")) //$NON-NLS-1$
                {
                    final String c = toolComponents[i].substring(17);

                    try {
                        changesetVersion = Integer.parseInt(c);
                    } catch (final NumberFormatException e) {
                        throw new RuntimeException(MessageFormat.format("The changeset version {0} is not valid", c)); //$NON-NLS-1$
                    }
                } else if (toolComponents[i].startsWith("deletionId=")) //$NON-NLS-1$
                {
                    final String d = toolComponents[i].substring(11);

                    try {
                        deletionId = Integer.parseInt(d);
                    } catch (final NumberFormatException e) {
                        throw new RuntimeException(MessageFormat.format("The deletion ID {0} is not valid", d)); //$NON-NLS-1$
                    }
                } else {
                    log.info(MessageFormat.format("Unknown versioned item artifact component: {0}", toolComponents[i])); //$NON-NLS-1$
                }
            }

            /* Ensure changeset version was seen */
            if (changesetVersion < 0) {
                throw new RuntimeException(
                    "Invalid version ID for artifact - changesetVersion is a required parameter"); //$NON-NLS-1$
            }

            return new VersionedItemLinkData(itemPath, changesetVersion, deletionId);
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class VersionedItemLinkData {
        private final String itemPath;
        private final int changesetVersion;
        private final int deletionId;

        private VersionedItemLinkData(final String itemPath, final int changesetVersion, final int deletionId) {
            Check.notNull(itemPath, "itemPath"); //$NON-NLS-1$

            this.itemPath = itemPath;
            this.changesetVersion = changesetVersion;
            this.deletionId = deletionId;
        }

        public String getItemPath() {
            return itemPath;
        }

        public int getChangesetVersion() {
            return changesetVersion;
        }

        public int getDeletionID() {
            return deletionId;
        }
    }
}
