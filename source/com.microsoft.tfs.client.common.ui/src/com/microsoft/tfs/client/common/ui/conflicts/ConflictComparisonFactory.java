// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.conflicts;

import java.nio.charset.Charset;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.compare.ServerItemByItemIDGenerator;
import com.microsoft.tfs.client.common.ui.compare.TFSConflictNode;
import com.microsoft.tfs.client.common.ui.compare.TFSConflictNode.TFSConflictNodeType;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.BothDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.DeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.FilenameConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.LocallyDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.MergeConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.MergeSourceDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.MergeTargetDeletedConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ShelvesetConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.VersionConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.WritableConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

public final class ConflictComparisonFactory {
    private static final Log log = LogFactory.getLog(ConflictComparisonFactory.class);

    /* Names of simple objects */
    private static final String BASE = Messages.getString("ConflictComparisonFactory.Base"); //$NON-NLS-1$
    private static final String LOCAL = Messages.getString("ConflictComparisonFactory.Local"); //$NON-NLS-1$
    private static final String SERVER = Messages.getString("ConflictComparisonFactory.Server"); //$NON-NLS-1$
    private static final String TARGET = Messages.getString("ConflictComparisonFactory.Target"); //$NON-NLS-1$
    private static final String SOURCE = Messages.getString("ConflictComparisonFactory.Source"); //$NON-NLS-1$
    private static final String ORIGINAL = Messages.getString("ConflictComparisonFactory.Original"); //$NON-NLS-1$
    private static final String DELETED = Messages.getString("ConflictComparisonFactory.Deleted"); //$NON-NLS-1$
    private static final String SHELVESET = Messages.getString("ConflictComparisonFactory.Shelveset"); //$NON-NLS-1$

    /* Prompts for descriptions in conflict resolution editor */
    private static final String PROMPT_LOCAL_FILE = Messages.getString("ConflictComparisonFactory.LocalFile"); //$NON-NLS-1$
    private static final String PROMPT_LOCAL_VERSION = Messages.getString("ConflictComparisonFactory.LocalVersion"); //$NON-NLS-1$
    private static final String PROMPT_SERVER_VERSION = Messages.getString("ConflictComparisonFactory.ServerVersion"); //$NON-NLS-1$
    private static final String PROMPT_SHELVESET_VERSION =
        Messages.getString("ConflictComparisonFactory.ShelvesetName"); //$NON-NLS-1$
    private static final String PROMPT_SOURCE_VERSION = Messages.getString("ConflictComparisonFactory.SourceVersion"); //$NON-NLS-1$
    private static final String PROMPT_TARGET_VERSION = Messages.getString("ConflictComparisonFactory.TargetVersion"); //$NON-NLS-1$

    public static String[] getAllPrompts() {
        return new String[] {
            PROMPT_LOCAL_FILE,
            PROMPT_LOCAL_VERSION,
            PROMPT_SERVER_VERSION,
            PROMPT_SHELVESET_VERSION,
            PROMPT_SOURCE_VERSION,
            PROMPT_TARGET_VERSION
        };
    }

    public static ConflictComparison getConflictComparison(final ConflictDescription conflictDescription) {
        Check.notNull(conflictDescription, "conflictDescription"); //$NON-NLS-1$

        /* Writable conflicts (source or target) */
        if (conflictDescription instanceof WritableConflictDescription) {
            return getWritableConflictComparison((WritableConflictDescription) conflictDescription);
        }
        /* TODO */
        else if (conflictDescription instanceof BothDeletedConflictDescription) {
            return null;
        }
        /* TODO */
        else if (conflictDescription instanceof MergeSourceDeletedConflictDescription) {
            return getMergeSourceDeletedConflictComparison((MergeSourceDeletedConflictDescription) conflictDescription);
        }
        /* TODO */
        else if (conflictDescription instanceof LocallyDeletedConflictDescription) {
            return getLocallyDeletedConflictComparison((LocallyDeletedConflictDescription) conflictDescription);
        }
        /* TODO */
        else if (conflictDescription instanceof MergeTargetDeletedConflictDescription) {
            return getMergeTargetDeletedConflictComparison((MergeTargetDeletedConflictDescription) conflictDescription);
        }
        /* TODO */
        else if (conflictDescription instanceof DeletedConflictDescription) {
            return getDeletedConflictComparison((DeletedConflictDescription) conflictDescription);
        }
        /* Baseless merge conflicts (source edited) */
        else if (conflictDescription.isBaseless()) {
            return getMergeBaselessConflictComparison((MergeConflictDescription) conflictDescription);
        }
        /* Standard merge conflicts (source and target edited) */
        else if (conflictDescription instanceof MergeConflictDescription) {
            return getMergeConflictComparison((MergeConflictDescription) conflictDescription);
        }
        /*
         * Standard shelveset conflicts (edited locally, edited in a shelveset)
         */
        else if (conflictDescription instanceof ShelvesetConflictDescription) {
            return getShelvesetConflictComparison((ShelvesetConflictDescription) conflictDescription);
        }
        /* Standard version conflicts */
        else if (conflictDescription instanceof VersionConflictDescription) {
            return getVersionConflictComparison((VersionConflictDescription) conflictDescription);
        }
        /* TODO */
        else if (conflictDescription instanceof FilenameConflictDescription) {
            return getFilenameConflictComparison((FilenameConflictDescription) conflictDescription);
        }

        log.error("Unknown conflict description type"); //$NON-NLS-1$
        return null;
    }

    private static ConflictComparison getWritableConflictComparison(
        final WritableConflictDescription conflictDescription) {
        /*
         * NOTE: writable conflicts will never have a base ItemID or version, at
         * least as of Orcas, even if the user has a version of the conflicting
         * file. at some point this may change, so that users can compare with
         * workspace version (which would be useful.)
         */

        final Object localNode = getLocalSourceNode(conflictDescription);
        final Object latestNode = getLatestNode(conflictDescription);

        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(LOCAL, localNode, SERVER, latestNode)
        };

        /* Local file description */
        final String localFileDescription = conflictDescription.getConflict().getTargetLocalItem();

        /* Server version comparison description */
        final String serverVersionDescription = getPathAndVersionDescription(
            conflictDescription.getServerPath(),
            conflictDescription.getConflict().getTheirVersion());

        final String serverVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareLocalToVersionFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getTheirVersion()));

        final ConflictComparisonDescription[] descriptions = new ConflictComparisonDescription[] {
            /*
             * Note: we do not provide comparison options for the local file.
             * This is because we do not provide a link in the local file
             * description.
             */
            new ConflictComparisonDescription(
                PROMPT_LOCAL_FILE,
                localFileDescription,
                null,
                new Object(),
                new Object()),

            new ConflictComparisonDescription(
                PROMPT_SERVER_VERSION,
                serverVersionDescription,
                serverVersionTooltip,
                localNode,
                latestNode)
        };

        return new ConflictComparison(options, descriptions);
    }

    private static ConflictComparison getLocallyDeletedConflictComparison(
        final LocallyDeletedConflictDescription conflictDescription) {
        final Object originalNode = getBaseNode(conflictDescription);
        final Object latestNode = getLatestNode(conflictDescription);

        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(ORIGINAL, originalNode, SERVER, latestNode)
        };

        /* Server version comparison description */
        final String serverVersionDescription = getPathAndVersionDescription(
            conflictDescription.getServerPath(),
            conflictDescription.getConflict().getTheirVersion());

        final String serverVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareOriginalToVersionFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getTheirVersion()));

        final ConflictComparisonDescription[] descriptions = new ConflictComparisonDescription[] {
            new ConflictComparisonDescription(
                PROMPT_SERVER_VERSION,
                serverVersionDescription,
                serverVersionTooltip,
                originalNode,
                latestNode)
        };

        return new ConflictComparison(options, descriptions);
    }

    private static ConflictComparison getMergeSourceDeletedConflictComparison(
        final MergeSourceDeletedConflictDescription conflictDescription) {
        final Object baseNode = getBaseNode(conflictDescription);
        final Object deletedNode = getTheirNode(conflictDescription);
        final Object targetNode = getLocalTargetNode(conflictDescription);

        return new ConflictComparison(new ConflictComparisonOption[] {
            new ConflictComparisonOption(BASE, baseNode, TARGET, targetNode),
            new ConflictComparisonOption(DELETED, deletedNode, TARGET, targetNode)
        });
    }

    private static ConflictComparison getMergeTargetDeletedConflictComparison(
        final MergeTargetDeletedConflictDescription conflictDescription) {
        final Object sourceNode = getTheirNode(conflictDescription);
        final Object deletedNode = getYourNode(conflictDescription);
        final Object baseNode = getBaseNode(conflictDescription);

        return new ConflictComparison(new ConflictComparisonOption[] {
            new ConflictComparisonOption(SOURCE, sourceNode, DELETED, deletedNode),
            new ConflictComparisonOption(BASE, baseNode, SOURCE, sourceNode),
            new ConflictComparisonOption(BASE, baseNode, DELETED, deletedNode)
        });
    }

    private static ConflictComparison getDeletedConflictComparison(
        final DeletedConflictDescription conflictDescription) {
        final Object localNode = getLocalSourceNode(conflictDescription);
        final Object deletedNode = getTheirNode(conflictDescription);
        final Object baseNode = getBaseNode(conflictDescription);

        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(LOCAL, localNode, DELETED, deletedNode),
            new ConflictComparisonOption(LOCAL, localNode, ORIGINAL, baseNode),
            new ConflictComparisonOption(ORIGINAL, baseNode, DELETED, deletedNode)
        };

        /* Local version comparison description */
        final String localVersionDescription = getPathAndVersionDescription(
            conflictDescription.getLocalPath(),
            conflictDescription.getConflict().getYourVersion());

        final String localVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareLocalToVersionFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getYourVersion()));

        /* Deleted version description */
        final String deletedVersionDescription = getPathDeletedDescription(
            conflictDescription.getConflict().getServerPath(),
            conflictDescription.getConflict().getTheirVersion());

        final String deletedVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareOriginalToDeletedFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getTheirVersion()));

        final ConflictComparisonDescription[] descriptions = new ConflictComparisonDescription[] {
            new ConflictComparisonDescription(
                PROMPT_LOCAL_VERSION,
                localVersionDescription,
                localVersionTooltip,
                localNode,
                baseNode),

            new ConflictComparisonDescription(
                PROMPT_SERVER_VERSION,
                deletedVersionDescription,
                deletedVersionTooltip,
                localNode,
                deletedNode)
        };

        return new ConflictComparison(options, descriptions);
    }

    private static ConflictComparison getMergeConflictComparison(final MergeConflictDescription conflictDescription) {
        final Object sourceNode = getTheirNode(conflictDescription);
        final Object targetNode = getYourNode(conflictDescription);
        final Object baseNode = getBaseNode(conflictDescription);

        /* Source version description */
        final String sourceVersionDescription = getPathAndMergeDescription(
            conflictDescription.getConflict().getTheirServerItem(),
            conflictDescription.getConflict().getTheirVersionFrom(),
            conflictDescription.getConflict().getTheirVersion());

        final String sourceVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareBaseToSource"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getYourVersion()));

        /* Target version description */
        final String targetVersionDescription = getPathAndVersionDescription(
            conflictDescription.getConflict().getYourServerItem(),
            conflictDescription.getConflict().getYourVersion());

        final String targetVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareBaseToTarget"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getTheirVersion()));

        ConflictComparisonDescription[] descriptions;
        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(SOURCE, sourceNode, TARGET, targetNode),
            new ConflictComparisonOption(BASE, baseNode, SOURCE, sourceNode),
            new ConflictComparisonOption(BASE, baseNode, TARGET, targetNode)
        };

        if (baseNode != null) {
            descriptions = new ConflictComparisonDescription[] {
                new ConflictComparisonDescription(
                    PROMPT_SOURCE_VERSION,
                    sourceVersionDescription,
                    sourceVersionTooltip,
                    baseNode != null ? baseNode : sourceNode,
                    sourceNode),

                new ConflictComparisonDescription(
                    PROMPT_TARGET_VERSION,
                    targetVersionDescription,
                    targetVersionTooltip,
                    baseNode != null ? baseNode : sourceNode,
                    targetNode)
            };
        } else {
            descriptions = new ConflictComparisonDescription[0];
        }
        return new ConflictComparison(options, descriptions);
    }

    private static ConflictComparison getMergeBaselessConflictComparison(
        final MergeConflictDescription conflictDescription) {
        final Object sourceNode = getTheirNode(conflictDescription);
        final Object targetNode = getYourNode(conflictDescription);

        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(SOURCE, sourceNode, TARGET, targetNode),
            new ConflictComparisonOption(BASE, null, SOURCE, sourceNode),
            new ConflictComparisonOption(BASE, null, TARGET, targetNode)
        };

        /* Source version description */
        final String sourceVersionDescription = getPathAndMergeDescription(
            conflictDescription.getConflict().getTheirServerItem(),
            conflictDescription.getConflict().getTheirVersionFrom(),
            conflictDescription.getConflict().getTheirVersion());

        final String sourceVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareBaseToSource"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getYourVersion()));

        /* Target version description */
        final String targetVersionDescription = getPathAndVersionDescription(
            conflictDescription.getConflict().getYourServerItem(),
            conflictDescription.getConflict().getYourVersion());

        final String targetVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareBaseToTarget"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getTheirVersion()));

        final ConflictComparisonDescription[] descriptions = new ConflictComparisonDescription[] {
            new ConflictComparisonDescription(
                PROMPT_SOURCE_VERSION,
                sourceVersionDescription,
                sourceVersionTooltip,
                sourceNode,
                sourceNode),

            new ConflictComparisonDescription(
                PROMPT_TARGET_VERSION,
                targetVersionDescription,
                targetVersionTooltip,
                sourceNode,
                targetNode)
        };

        return new ConflictComparison(options, descriptions);
        // return new ConflictComparison(options, new
        // ConflictComparisonDescription[0]);
    }

    private static ConflictComparison getShelvesetConflictComparison(
        final ShelvesetConflictDescription conflictDescription) {
        final Object localNode = getLocalSourceNode(conflictDescription);
        final Object shelvesetNode = getShelvesetNode(conflictDescription);
        final Object baseNode = getBaseNode(conflictDescription);

        /* Simple comparison options */
        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(LOCAL, localNode, SHELVESET, shelvesetNode),
            new ConflictComparisonOption(SHELVESET, shelvesetNode, BASE, baseNode),
            new ConflictComparisonOption(LOCAL, localNode, BASE, baseNode)
        };

        /* Local version comparison description */
        final String localVersionDescription = getPathAndVersionDescription(
            conflictDescription.getLocalPath(),
            conflictDescription.getConflict().getYourVersion());

        final String localVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareLocalToVersionFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getYourVersion()));

        /* Shelveset comparison description */
        final String shelvesetVersionDescription =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareShelvesetNameFormat"), //$NON-NLS-1$
                conflictDescription.getConflict().getTheirShelvesetDisplayName(conflictDescription.getWorkspace()));

        final String shelvesetVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareLocalToShelvesetFormat"), //$NON-NLS-1$
                conflictDescription.getConflict().getTheirShelvesetDisplayName(conflictDescription.getWorkspace()));

        final ConflictComparisonDescription[] descriptions = new ConflictComparisonDescription[] {
            new ConflictComparisonDescription(
                PROMPT_LOCAL_VERSION,
                localVersionDescription,
                localVersionTooltip,
                localNode,
                baseNode),

            new ConflictComparisonDescription(
                PROMPT_SHELVESET_VERSION,
                shelvesetVersionDescription,
                shelvesetVersionTooltip,
                localNode,
                shelvesetNode)
        };

        return new ConflictComparison(options, descriptions);
    }

    private static ConflictComparison getVersionConflictComparison(
        final VersionConflictDescription conflictDescription) {
        final Object localNode = getLocalSourceNode(conflictDescription);
        final Object latestNode = getLatestNode(conflictDescription);
        final Object originalNode = getBaseNode(conflictDescription);

        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(LOCAL, localNode, SERVER, latestNode),
            new ConflictComparisonOption(LOCAL, localNode, ORIGINAL, originalNode),
            new ConflictComparisonOption(ORIGINAL, originalNode, SERVER, latestNode)
        };

        /* Local version comparison description */
        final String localVersionDescription = getPathAndVersionDescription(
            conflictDescription.getConflict().getSourceLocalItem(),
            conflictDescription.getConflict().getYourVersion());

        final String localVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareLocalToVersionFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getYourVersion()));

        /* Server version comparison description */
        final String serverVersionDescription = getPathAndVersionDescription(
            conflictDescription.getConflict().getTheirServerItem(),
            conflictDescription.getConflict().getTheirVersion());

        final String serverVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareLocalToVersionFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getTheirVersion()));

        final ConflictComparisonDescription[] descriptions = new ConflictComparisonDescription[] {
            new ConflictComparisonDescription(
                PROMPT_LOCAL_VERSION,
                localVersionDescription,
                localVersionTooltip,
                localNode,
                originalNode),

            new ConflictComparisonDescription(
                PROMPT_SERVER_VERSION,
                serverVersionDescription,
                serverVersionTooltip,
                localNode,
                latestNode)
        };

        return new ConflictComparison(options, descriptions);
    }

    private static ConflictComparison getFilenameConflictComparison(
        final FilenameConflictDescription conflictDescription) {
        final Object localNode = getLocalSourceNode(conflictDescription);
        final Object latestNode = getLatestNode(conflictDescription);

        final ConflictComparisonOption[] options = new ConflictComparisonOption[] {
            new ConflictComparisonOption(LOCAL, localNode, SERVER, latestNode)
        };

        /* Server version comparison description */
        final String serverVersionDescription = getPathAndVersionDescription(
            conflictDescription.getConflict().getTheirServerItem(),
            conflictDescription.getConflict().getTheirVersion());

        final String serverVersionTooltip =
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.CompareLocalToVersionFormat"), //$NON-NLS-1$
                Integer.toString(conflictDescription.getConflict().getTheirVersion()));

        final ConflictComparisonDescription[] descriptions = new ConflictComparisonDescription[] {
            new ConflictComparisonDescription(
                PROMPT_SERVER_VERSION,
                serverVersionDescription,
                serverVersionTooltip,
                localNode,
                latestNode)
        };

        return new ConflictComparison(options, descriptions);
    }

    /*
     * Comparison node factory
     */

    private static Object getLocalSourceNode(final ConflictDescription conflictDescription) {
        /* Target writable conflicts will lack a source local item. */
        final String localPath = conflictDescription.getConflict().getSourceLocalItem() != null
            ? conflictDescription.getConflict().getSourceLocalItem()
            : conflictDescription.getConflict().getTargetLocalItem();

        final Charset charset = conflictDescription.getConflict().getYourEncoding() != null
            ? CodePageMapping.getCharset(conflictDescription.getConflict().getYourEncoding().getCodePage(), false)
            : null;

        if (localPath == null) {
            log.warn("Could not determine local path for conflict"); //$NON-NLS-1$
            return null;
        }

        return CompareUtils.createCompareElementForLocalPath(localPath, charset, ResourceType.FILE, null);
    }

    private static Object getLocalTargetNode(final ConflictDescription conflictDescription) {
        final String localPath = conflictDescription.getConflict().getTargetLocalItem();
        final Charset charset = conflictDescription.getConflict().getTheirEncoding() != null
            ? CodePageMapping.getCharset(conflictDescription.getConflict().getYourEncoding().getCodePage(), false)
            : null;

        if (localPath == null) {
            log.warn("Could not determine local target path for conflict"); //$NON-NLS-1$
            return null;
        }

        return CompareUtils.createCompareElementForLocalPath(localPath, charset, ResourceType.FILE, null);
    }

    private static Object getLatestNode(final ConflictDescription conflictDescription) {
        final int itemId = conflictDescription.getConflict().getTheirItemID();

        if (itemId == 0) {
            log.warn("Could not determine latest item id"); //$NON-NLS-1$
            return null;
        }

        return new ServerItemByItemIDGenerator(
            conflictDescription.getWorkspace().getClient(),
            itemId,
            Integer.MAX_VALUE);
    }

    private static Object getBaseNode(final ConflictDescription conflictDescription) {
        /* BaseItemID == 0 for baseless merges. */
        if (conflictDescription.getConflict().getBaseVersion() == 0 || conflictDescription.isBaseless()) {
            return null;
        }

        final int itemId = conflictDescription.getConflict().getBaseItemID();
        final int itemVersion = conflictDescription.getConflict().getBaseVersion();

        if (itemId == 0 || itemVersion == 0) {
            log.warn("Could not determine workspace item id / version"); //$NON-NLS-1$
            return null;
        }

        return new ServerItemByItemIDGenerator(conflictDescription.getWorkspace().getClient(), itemId, itemVersion);
    }

    private static Object getTheirNode(final ConflictDescription conflictDescription) {
        if (conflictDescription.getConflict().getTheirVersion() == 0) {
            return null;
        }

        final int itemId = conflictDescription.getConflict().getTheirItemID();
        final int itemVersion = conflictDescription.getConflict().getTheirVersion();

        if (itemId == 0 || itemVersion == 0) {
            log.warn("Could not determine your item id / version"); //$NON-NLS-1$
            return null;
        }

        return new ServerItemByItemIDGenerator(conflictDescription.getWorkspace().getClient(), itemId, itemVersion);
    }

    private static Object getYourNode(final ConflictDescription conflictDescription) {
        if (conflictDescription.getConflict().getYourVersion() == 0) {
            return null;
        }

        final int itemId = conflictDescription.getConflict().getYourItemID();
        final int itemVersion = conflictDescription.getConflict().getYourVersion();

        if (itemId == 0 || itemVersion == 0) {
            log.warn("Could not determine your item id / version"); //$NON-NLS-1$
            return null;
        }

        return new ServerItemByItemIDGenerator(conflictDescription.getWorkspace().getClient(), itemId, itemVersion);
    }

    private static Object getShelvesetNode(final ConflictDescription conflictDescription) {
        final Conflict conflict = conflictDescription.getConflict();

        if (conflict.getTheirDownloadURL() == null || conflict.getTheirDownloadURL().length() == 0) {
            log.warn("Conflict did not have download URL for shelveset conflict ID " + conflict.getConflictID()); //$NON-NLS-1$
            return null;
        }

        final TFSConflictNode shelvesetNode =
            new TFSConflictNode(conflictDescription.getWorkspace().getClient(), conflict, TFSConflictNodeType.THEIRS);

        shelvesetNode.setLabel(
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.ShelvesetNodeLabelFormat"), //$NON-NLS-1$
                conflict.getTheirServerItem() != null ? conflict.getTheirServerItem() : conflict.getFileName(),
                conflict.getTheirShelvesetName()));

        shelvesetNode.setLabel(
            MessageFormat.format(
                Messages.getString("ConflictComparisonFactory.ShelvesetNodeLabelFormat", LocaleUtil.ROOT), //$NON-NLS-1$
                conflict.getTheirServerItem() != null ? conflict.getTheirServerItem() : conflict.getFileName(),
                conflict.getTheirShelvesetName()));

        return shelvesetNode;
    }

    /*
     * Description string formatters
     */

    private static String getPathAndVersionDescription(final String path, final int changesetId) {
        return MessageFormat.format(
            Messages.getString("ConflictComparisonFactory.CompareDescriptionPathAndVersionDescriptionFormat"), //$NON-NLS-1$
            path,
            Integer.toString(changesetId));
    }

    private static String getPathAndMergeDescription(
        final String path,
        final int fromChangesetId,
        final int toChangesetId) {
        return MessageFormat.format(
            Messages.getString("ConflictComparisonFactory.CompareDescriptionPathAndMergeVersionDescriptionFormat"), //$NON-NLS-1$
            path,
            Integer.toString(fromChangesetId),
            Integer.toString(toChangesetId));
    }

    private static String getPathDeletedDescription(final String path, final int deletionChangesetId) {
        return MessageFormat.format(
            Messages.getString("ConflictComparisonFactory.CompareDescriptionPathDeletedDescriptionFormat"), //$NON-NLS-1$
            path,
            Integer.toString(deletionChangesetId));
    }
}
