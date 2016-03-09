// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.conflicts.resolutions;

import java.nio.charset.Charset;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.MessageDialog;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareResult;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions.EncodingStrategy;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolutionStatus;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ExternalConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.engines.MergeEngine;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PreMergeFailedException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.diffmerge.ThreeWayMerge;

public class EclipseMergeConflictResolution extends ConflictResolution {
    private static final Log log = LogFactory.getLog(EclipseMergeConflictResolution.class);

    private final Object lock = new Object();

    // may need to rename the target of the merge
    private String newPath;

    // may need a target encoding
    private FileEncoding newEncoding;

    // whether the merge was accepted
    private final Object compareResultLock = new Object();
    private CompareResult compareResult;

    public EclipseMergeConflictResolution(
        final ConflictDescription conflictDescription,
        final String description,
        final String helpText,
        final ConflictResolutionOptions options) {
        super(conflictDescription, description, helpText, options);
    }

    @Override
    public ConflictResolution newForConflictDescription(final ConflictDescription conflictDescription) {
        /*
         * Only called by dummy methods for fancy UI, this doesn't need to have
         * merge tools and thus be accurate.
         */
        return new ExternalConflictResolution(
            conflictDescription,
            getDescription(),
            getHelpText(),
            ConflictResolutionOptions.NONE,
            null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNewPath(final String newPath) {
        if (!needsNewPath()) {
            throw new TECoreException(MessageFormat.format(
                Messages.getString("EclipseMergeConflictResolution.ResolutionTypeDoesNotAcceptNewPathsFormat"), //$NON-NLS-1$
                getDescription()));
        }

        synchronized (lock) {
            this.newPath = newPath;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setEncoding(final FileEncoding newEncoding) {
        if (!needsEncodingSelection()) {
            throw new TECoreException(MessageFormat.format(
                Messages.getString("EclipseMergeConflictResolution.ResolutionTypeDoesNotAcceptNewEncodingsFormat"), //$NON-NLS-1$
                getDescription()));
        }

        synchronized (lock) {
            this.newEncoding = newEncoding;
        }
    }

    @Override
    protected ConflictResolutionStatus work() throws Exception {
        final ConflictDescription conflictDescription = getConflictDescription();

        final Workspace workspace = conflictDescription.getWorkspace();
        final MergeEngine mergeEngine = new MergeEngine(workspace, workspace.getClient());

        /*
         * we need to create a copy of the conflict as external resolution can
         * leave the object wedged when it fails
         */
        final Conflict conflict = getConflictDescription().getConflict();

        // set the new path if necessary
        if (newPath != null) {
            conflict.getResolutionOptions().setNewPath(newPath);
        }

        // set new encoding if necessary
        if (newEncoding != null) {
            /*
             * The encoding strategy is strictly for the merge engine - it
             * determines the output file's encoding. The acceptMergeEncoding
             * option is strictly for the resolveConflict web services call - it
             * determines the encoding type metadata used by tfs. Both must be
             * specified.
             */
            conflict.getResolutionOptions().setEncodingStrategy(EncodingStrategy.CONVERT_EXPLICIT, newEncoding);
            conflict.getResolutionOptions().setAcceptMergeEncoding(newEncoding);
        }

        /*
         * Create a new three way merge object to help us with labels and file
         * handling
         */
        final ThreeWayMerge threeWayMerge = new ThreeWayMerge();

        final String modifiedFilename = mergeEngine.beginCustomMerge(conflict, threeWayMerge);

        if (modifiedFilename == null) {
            throw new PreMergeFailedException();
        }

        boolean mergeSuccess = false;

        try {
            /* The modified (local) object */
            final String modifiedLabel = threeWayMerge.getYourFileLabel();

            final FileEncoding outputEncoding = threeWayMerge.getIntermediateMergeEncoding() != null
                ? threeWayMerge.getIntermediateMergeEncoding() : threeWayMerge.getMergedFileEncoding();
            final Charset outputCharset = CodePageMapping.getCharset(outputEncoding.getCodePage(), false);

            /* The original (latest) object */
            final String originalFilename = threeWayMerge.getTheirFileName();
            final String originalLabel = threeWayMerge.getTheirFileLabel();
            final Charset originalCharset =
                CodePageMapping.getCharset(threeWayMerge.getOriginalFileEncoding().getCodePage(), false);

            /* The common ancestor (base) object */
            final String ancestorFilename = threeWayMerge.getBaseFileName();
            final String ancestorLabel = threeWayMerge.getBaseFileLabel();
            final Charset ancestorCharset =
                CodePageMapping.getCharset(threeWayMerge.getBaseFileEncoding().getCodePage(), false);

            UIHelpers.runOnUIThread(false, new Runnable() {
                @Override
                public void run() {
                    try {
                        final Compare compare = new Compare();
                        compare.setModifiedLocalPath(modifiedFilename, outputCharset, ResourceType.FILE, null);
                        compare.getCompareConfiguration().setLeftLabel(modifiedLabel);
                        compare.getCompareConfiguration().setLeftEditable(true);

                        compare.setOriginalLocalPath(originalFilename, originalCharset, ResourceType.FILE, null);
                        compare.getCompareConfiguration().setRightLabel(originalLabel);
                        compare.getCompareConfiguration().setRightEditable(false);

                        compare.setAncestorLocalPath(ancestorFilename, ancestorCharset, ResourceType.FILE, null);
                        compare.getCompareConfiguration().setAncestorLabel(ancestorLabel);

                        /*
                         * Set the comparison to always dirty - this will enable
                         * the save button and make it obvious that the user
                         * must either save (accepting the merge) or cancel
                         * (aborting the resolution.)
                         *
                         * This is particularly important in the case where the
                         * latest and the user's file both have common changes
                         * from an ancestor - when the buffers are not initially
                         * dirty, Eclipse's compare engine will not display the
                         * "Inputs are equivalent" message, instead it will
                         * display the left and right as identical, but will
                         * only allow Cancel to proceed.
                         */
                        compare.setAlwaysDirty(true);

                        /*
                         * CompareUIType must be DIALOG or another synchronous
                         * comparison type (in order to get a compare result)
                         */
                        compare.setUIType(CompareUIType.DIALOG);
                        final CompareResult result = compare.open();

                        synchronized (compareResultLock) {
                            compareResult = result;
                        }
                    } catch (final Exception e) {
                        log.warn("Could not open eclipse merge dialog for conflict resolution", e); //$NON-NLS-1$

                        MessageDialog.openError(
                            ShellUtils.getBestParent(ShellUtils.getWorkbenchShell()),
                            Messages.getString("EclipseMergeConflictResolution.ErrorDialogTitle"), //$NON-NLS-1$
                            MessageFormat.format(
                                Messages.getString("EclipseMergeConflictResolution.ErrorDialogMessageFormat"), //$NON-NLS-1$
                                e.getMessage()));
                    }
                }
            });

            synchronized (compareResultLock) {
                /*
                 * If the contents were identical, there was no merge necessary
                 * and this is implicitly accepted. Otherwise, the merge is
                 * accepted if the user clicked the save button or the results
                 * are saved otherwise.
                 */
                mergeSuccess = (compareResult != null
                    && (compareResult.isContentIdentical()
                        || compareResult.wasOKPressed()
                        || compareResult.isContentSaved()));
            }
        } finally {
            mergeEngine.endCustomMerge(modifiedFilename, conflict, threeWayMerge, mergeSuccess);
        }

        if (!mergeSuccess) {
            return ConflictResolutionStatus.CANCELLED;
        }

        conflict.setResolution(Resolution.ACCEPT_MERGE);
        workspace.resolveConflict(conflict);

        if (conflict.isResolved()) {
            return ConflictResolutionStatus.SUCCESS;
        } else {
            /* Get error messages from merge failures */
            if (conflict.getContentMergeSummary() != null
                && conflict.getContentMergeSummary().getTotalConflictingLines() > 0) {
                setErrorMessage(Messages.getString("EclipseMergeConflictResolution.ConflictingContentChanges")); //$NON-NLS-1$
            }

            conflict.setResolution(Resolution.NONE);
            return ConflictResolutionStatus.FAILED;
        }
    }
}
