// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions.EncodingStrategy;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.GetEngine;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.MergeToolNotConfiguredException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PreMergeFailedException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeNames;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeValues;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesCollection;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.StringPairFileAttribute;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.diffmerge.ThreeWayMerge;
import com.microsoft.tfs.jni.helpers.FileCopyHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessFinishedHandler;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * <p>
 * Handles high-level merge tasks for external tool and custom merge processes.
 * A {@link MergeEngine} is designed to be used by {@link Workspace}, and is not
 * intended to be used by other clients.
 * </p>
 *
 * @threadsafety thread-safe
 */
public final class MergeEngine {
    private static final Log log = LogFactory.getLog(MergeEngine.class);

    private final Workspace workspace;
    private final VersionControlClient client;
    private final ExternalToolset mergeToolset;

    /**
     * No merge toolset will be available in this {@link MergeEngine}, so
     * external merge operations will fail.
     *
     * @equivalence MergeEngine(workspace, client, null)
     */
    public MergeEngine(final Workspace workspace, final VersionControlClient client) {
        this(workspace, client, null);
    }

    /**
     * Creates a {@link MergeEngine} with the given workspace, client, and merge
     * toolset (which will be used to select a tool when external merges require
     * them).
     *
     * @param workspace
     *        the workspace where items are being merged (must not be
     *        <code>null</code>)
     * @param client
     *        the version control client (must not be <code>null</code>)
     * @param mergeToolset
     *        a set of tools to use for external merges (may be null but
     *        external merges will fail)
     */
    public MergeEngine(
        final Workspace workspace,
        final VersionControlClient client,
        final ExternalToolset mergeToolset) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        this.workspace = workspace;
        this.client = client;
        this.mergeToolset = mergeToolset;
    }

    /**
     * Begins an external content merge for the given conflict. The appropriate
     * external merge tool will be invoked and a {@link ProcessRunner} that
     * wraps it returned. This runner can be polled for the process state, or
     * you can pass in a {@link ProcessFinishedHandler} and be notified as soon
     * as the state changes.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is a <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param conflict
     *        the conflict whose content should be merged (must not be
     *        <code>null</code>)
     * @param threeWayMerge
     *        a newly constructed {@link ThreeWayMerge} to be used by this merge
     *        operation. (must not be <code>null</code>)
     * @param finishedHandler
     *        an event handler whose methods are invoked when the process runner
     *        reaches one of its terminal states. The caller would normally
     *        implement the handler to call
     *        {@link #endExternalMerge(ProcessRunner, Conflict, ThreeWayMerge)}
     *        when the runner reaches any terminal state. May be null if no
     *        state information is desired via the handler.
     * @param capturedStandardOutput
     *        a stream to capture the text written by the child process to its
     *        standard output stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @param capturedStandardError
     *        a stream to capture the text written by the child process to its
     *        standard error stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @return the process runner that can be queried for information about the
     *         external merge tool process.
     * @throws MergeToolNotConfiguredException
     *         if no configured merge tool exists for the given conflict. No
     *         external process was launched.
     * @throws IOException
     *         if an error occurred creating the merge output file before the
     *         merge tool was invoked. No external process was launched.
     * @throws PreMergeFailedException
     *         if the pre merge step failed for this conflict. No external
     *         process was launched.
     * @throws ExternalToolException
     *         if no merge tools are available, or if the configured merge
     *         command or arguments string caused a problem creating the merge
     *         tool.
     */
    public ProcessRunner beginExternalMerge(
        final Conflict conflict,
        final ThreeWayMerge threeWayMerge,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError)
            throws MergeToolNotConfiguredException,
                PreMergeFailedException,
                IOException,
                ExternalToolException {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(threeWayMerge, "threeWayMerge"); //$NON-NLS-1$

        final String fileName = conflict.getResolutionOptions().getNewPath() != null
            ? ServerPath.getFileName(conflict.getResolutionOptions().getNewPath()) : conflict.getTargetLocalItem();

        final ExternalTool tool = mergeToolset != null ? mergeToolset.findTool(fileName) : null;

        if (tool == null) {
            throw new MergeToolNotConfiguredException(
                MessageFormat.format(
                    Messages.getString("MergeEngine.CouldNotFindExternalMergeToolForFileFormat"), //$NON-NLS-1$
                    threeWayMerge.getYourFileName()));
        }

        if (preMerge(conflict, threeWayMerge, false) == false) {
            throw new PreMergeFailedException();
        }

        return threeWayMerge.beginExternalMerge(
            conflict,
            tool,
            finishedHandler,
            capturedStandardOutput,
            capturedStandardError);
    }

    /**
     * Ends an external content merge for the given conflict that was begun with
     * {@link #beginExternalMerge(Conflict, ThreeWayMerge, ProcessFinishedHandler, OutputStream, OutputStream)}
     * .
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is a <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param runner
     *        the process runner created by
     *        {@link #beginExternalMerge(Conflict, ThreeWayMerge, ProcessFinishedHandler, OutputStream, OutputStream)}
     *        . If this runner is not already finished, this method will wait
     *        until it finishes. (must not be <code>null</code>)
     * @param conflict
     *        the {@link Conflict} that was passed to
     *        {@link #beginExternalMerge(Conflict, ThreeWayMerge, ProcessFinishedHandler, OutputStream, OutputStream)}
     *        to start this merge. (must not be <code>null</code>)
     * @param threeWayMerge
     *        the {@link ThreeWayMerge} that was passed to
     *        {@link #beginExternalMerge(Conflict, ThreeWayMerge, ProcessFinishedHandler, OutputStream, OutputStream)}
     *        to start this merge. (must not be <code>null</code>)
     * @return true if the external merge succeeded, false otherwise
     */
    public boolean endExternalMerge(
        final ProcessRunner runner,
        final Conflict conflict,
        final ThreeWayMerge threeWayMerge) {
        Check.notNull(runner, "runner"); //$NON-NLS-1$
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(threeWayMerge, "threeWayMerge"); //$NON-NLS-1$

        try {
            final boolean ret = threeWayMerge.endExternalMerge(runner);

            conflict.setMergedFileName(threeWayMerge.getMergedFileName());

            return ret;
        } finally {
            /*
             * Clean up after any files left around.
             */
            if (threeWayMerge.getBaseFileName() != null) {
                TempStorageService.getInstance().cleanUpItem(new File(threeWayMerge.getBaseFileName()));
            }
            if (threeWayMerge.getTheirFileName() != null) {
                TempStorageService.getInstance().cleanUpItem(new File(threeWayMerge.getTheirFileName()));
            }
        }
    }

    /**
     * Sets up a merge for the given conflict so that callers may perform a
     * custom merge process, returning the merge output file that should be
     * written to to complete the merge. Users should call
     * {@link MergeEngine#endCustomMerge(String, Conflict, ThreeWayMerge, boolean)}
     * to complete the merge.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is a <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param conflict
     *        the conflict whose content should be merged (must not be
     *        <code>null</code>)
     * @param threeWayMerge
     *        a newly constructed {@link ThreeWayMerge} to be used by this merge
     *        operation. (must not be <code>null</code>)
     * @throws IOException
     *         if an error occurred creating the merge input or output files.
     * @throws PreMergeFailedException
     *         if the pre merge step failed for this conflict.
     * @return the filename that merged output should be written to
     */
    public String beginCustomMerge(final Conflict conflict, final ThreeWayMerge threeWayMerge)
        throws PreMergeFailedException,
            IOException {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(threeWayMerge, "threeWayMerge"); //$NON-NLS-1$

        if (preMerge(conflict, threeWayMerge, false) == false) {
            throw new PreMergeFailedException();
        }

        Check.notNull(threeWayMerge.getYourFileName(), "threeWayMerge.getYourFileName()"); //$NON-NLS-1$

        final String tempFilename = TempStorageService.getInstance().createTempFile(
            new File(LocalPath.getDirectory(threeWayMerge.getYourFileName())),
            LocalPath.getFileExtension(threeWayMerge.getYourFileName())).getAbsolutePath();

        final FileEncoding yourEncoding = threeWayMerge.getModifiedFileEncoding();
        final FileEncoding mergedEncoding = threeWayMerge.getIntermediateMergeEncoding() != null
            ? threeWayMerge.getIntermediateMergeEncoding() : threeWayMerge.getMergedFileEncoding();

        final Charset yourCharset = CodePageMapping.getCharset(yourEncoding.getCodePage(), false);
        final Charset mergedCharset = CodePageMapping.getCharset(mergedEncoding.getCodePage(), false);

        try {
            FileCopyHelper.copyText(threeWayMerge.getYourFileName(), yourCharset, tempFilename, mergedCharset);
        } catch (final MalformedInputException e) {
            log.info("The file " //$NON-NLS-1$
                + threeWayMerge.getYourFileName()
                + " could not be read with the encoding " //$NON-NLS-1$
                + yourCharset.displayName(), e);
            throw new VersionControlException(
                MessageFormat.format(
                    FileEncoding.ENCODING_ERROR_MESSAGE_FORMAT,
                    Messages.getString("MergeEngine.ModifiedFile"), //$NON-NLS-1$
                    LocalPath.getFileName(threeWayMerge.getYourFileName()),
                    yourCharset.displayName()),
                e);
        } catch (final UnmappableCharacterException e) {
            log.info("The file " //$NON-NLS-1$
                + threeWayMerge.getYourFileName()
                + " could not be read with the encoding " //$NON-NLS-1$
                + yourCharset.displayName(), e);
            throw new VersionControlException(
                MessageFormat.format(
                    FileEncoding.ENCODING_ERROR_MESSAGE_FORMAT,
                    Messages.getString("MergeEngine.ModifiedFile"), //$NON-NLS-1$
                    LocalPath.getFileName(threeWayMerge.getYourFileName()),
                    yourCharset.displayName()),
                e);
        }

        return tempFilename;
    }

    /**
     * Ends an custom content merge for the given conflict that was begun with
     * {@link #beginCustomMerge(Conflict, ThreeWayMerge)}.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is a <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param mergeFilename
     *        the merge output filename created by
     *        {@link #beginCustomMerge(Conflict, ThreeWayMerge)} (must not be
     *        <code>null</code>).
     * @param conflict
     *        the {@link Conflict} that was passed to
     *        {@link #beginCustomMerge(Conflict, ThreeWayMerge)} to start this
     *        merge. (must not be <code>null</code>)
     * @param threeWayMerge
     *        the {@link ThreeWayMerge} that was passed to
     *        {@link #beginCustomMerge(Conflict, ThreeWayMerge)} to start this
     *        merge. (must not be <code>null</code>)
     * @param success
     *        true if the custom merge was accepted, false if the merge failed
     */
    public void endCustomMerge(
        final String mergeFilename,
        final Conflict conflict,
        final ThreeWayMerge threeWayMerge,
        final boolean success) {
        Check.notNull(mergeFilename, "mergeFilename"); //$NON-NLS-1$
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(threeWayMerge, "threeWayMerge"); //$NON-NLS-1$

        if (success) {
            conflict.setMergedFileName(mergeFilename);
        } else {
            TempStorageService.getInstance().cleanUpItem(new File(mergeFilename));
        }

        /*
         * Clean up after any files left around.
         */
        if (threeWayMerge.getBaseFileName() != null) {
            TempStorageService.getInstance().cleanUpItem(new File(threeWayMerge.getBaseFileName()));
        }
        if (threeWayMerge.getTheirFileName() != null) {
            TempStorageService.getInstance().cleanUpItem(new File(threeWayMerge.getTheirFileName()));
        }
    }

    /**
     * Counts the potential content merge conflicts for the given conflict and
     * stores them in the conflict's merge summary.
     *
     * @param conflict
     *        the conflict to count content conflicts for (must not be
     *        <code>null</code>)
     */
    public void countContentConflicts(final Conflict conflict) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        /*
         * Classify the conflict in high-level terms.
         */
        final boolean isMerge = (conflict.getType() == ConflictType.MERGE);
        final boolean isGetOrCheckin =
            (conflict.getType() == ConflictType.GET || conflict.getType() == ConflictType.CHECKIN);
        final boolean isNamespace = (isGetOrCheckin && conflict.isNamespaceConflict());

        /*
         * See if we can do an automatic file merge.
         *
         * If I want the automatic behavior, and my item is a file, and there's
         * no namespace conflict, continue...
         */
        if (conflict.getYourItemType() != ItemType.FOLDER && isNamespace == false) {
            /*
             * If my change was an edit, or there's a forced merge over a
             * baseline edit, or there's a merge where their version is less
             * than the baseline version, proceed...
             */
            if (conflict.getYourChangeType().contains(ChangeType.EDIT)
                || (isMerge && conflict.isForced() && conflict.getBaseChangeType().contains(ChangeType.EDIT))
                || (isMerge && conflict.getTheirLastMergedVersion() < conflict.getBaseVersion())) {
                /*
                 * If their file is empty, or there's no merge problem at all
                 * (versions match), or the whole conflict is over a delete,
                 * there's nothing the file merge can do for us.
                 */
                if (conflict.getTheirVersion() == 0
                    || (isMerge == false && conflict.getYourVersion() == conflict.getTheirVersion())
                    || conflict.getBaseChangeType().contains(ChangeType.DELETE)) {
                    log.debug(MessageFormat.format(
                        "Skipped content conflict analysis for {0}", //$NON-NLS-1$
                        conflict.getSourceLocalItem()));
                } else {
                    /*
                     * If there is not a result file already set for this
                     * conflict, go ahead with the automatic merge.
                     */
                    if (conflict.getMergedFileName() == null || conflict.getMergedFileName() == "") //$NON-NLS-1$
                    {
                        try {
                            /*
                             * If onlyCountConflicts is true in this method
                             * call, the return will always be false, so we can
                             * exit early.
                             *
                             * The merge problem detected here is not related to
                             * conflicts (those are reported through the content
                             * merge summary). Problems detected here might be
                             * failure to launch an external process, or
                             * conflicting encoding options.
                             */
                            final boolean mergeProblem = (mergeContent(conflict, true, null, null, null) == false);

                            /*
                             * If the merge call returned false or left a merge
                             * summary warning in the conflict, the merge did
                             * not succeed.
                             */
                            if (mergeProblem
                                || (conflict.getContentMergeSummary() != null
                                    && conflict.getResolutionOptions().isAcceptMergeWithConflicts() == false
                                    && conflict.getContentMergeSummary().getTotalConflictingLines() != 0)) {
                                /*
                                 * Visual Studio's client deletes the merged
                                 * file here, because no exceptions were thrown,
                                 * and we know the file is useless (because it
                                 * still has conflicts).
                                 */
                                if (conflict.getMergedFileName() != null) {
                                    final File f = new File(conflict.getMergedFileName());
                                    if (f.exists()) {
                                        f.delete();
                                    }
                                }
                            }
                        } catch (final Exception e) {
                            /*
                             * The content merge failed with an exception, which
                             * we should treat as a non-fatal error.
                             */
                            client.getEventEngine().fireNonFatalError(
                                new NonFatalErrorEvent(EventSource.newFromHere(), workspace, e));
                        }
                    }
                }
            }
        }
    }

    /**
     * Merges content for the given conflict. Detects whether to use internal or
     * external method. Both strategies may block a significant amount of time
     * while the merge happens.
     * <p>
     * <!-- Event Origination Info -->
     * <p>
     * This method is a <b>core event origination point</b>. The
     * {@link EventSource} object that accompanies each event fired by this
     * method describes the execution context (current thread, etc.) when and
     * where this method was invoked.
     *
     * @param conflict
     *        the conflict whose content should be merged (must not be
     *        <code>null</code>)
     * @param onlyCountConflicts
     *        if true, the pre merge completes but only the conflicts are
     *        counted, no merged file result will be present (check the conflict
     *        object's MergeSummary for the conflict count). If false, the merge
     *        is completed, and the output file is created. This parameter is
     *        ignored when the external merge tool is used.
     * @param finishedHandler
     *        an event handler whose methods are invoked when the process runner
     *        used to run external merge tools reaches one of its terminal
     *        states. May be null if no state information is desired via the
     *        handler.
     * @param capturedStandardOutput
     *        a stream to capture the text written by the child process to its
     *        standard output stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @param capturedStandardError
     *        a stream to capture the text written by the child process to its
     *        standard error stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @return true if the merge was successful (the merge process was run),
     *         false if it was not (the merge process could not be started or
     *         there was a non-content-conflict error).
     */
    public boolean mergeContent(
        final Conflict conflict,
        final boolean onlyCountConflicts,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        if (conflict.getResolutionOptions().useInternalEngine() == false) {
            return externalMergeContent(conflict, finishedHandler, capturedStandardOutput, capturedStandardError);
        } else {
            return internalMergeContent(conflict, onlyCountConflicts);
        }
    }

    /**
     * Merges the content of the files described by the given conflict using the
     * built-in automatic content merge tool. The conflict object is modified to
     * describe the merge result.
     *
     * @param conflict
     *        the conflict whose content should be merged (must not be
     *        <code>null</code>)
     * @param onlyCountConflicts
     *        if true, the pre merge completes but only the conflicts are
     *        counted, no merged file result will be present (check the conflict
     *        object's MergeSummary for the conflict count). If false, the merge
     *        is completed, and the output file is created.
     * @returns true if the merge was successful (the merge process was run),
     *          false if it was not (the merge process could not be started or
     *          there was a non-content-conflict error).
     */
    private boolean internalMergeContent(final Conflict conflict, final boolean onlyCountConflicts) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        /*
         * .tpattributes can set the desired client end-of-line style for a
         * working folder file. If this attribute is enabled, we must pass that
         * EOL style to the three way merge engine so the resulting file also
         * has that style.
         */
        String desiredNewlineSequence = null;
        if (conflict.getTargetLocalItem() != null && conflict.getTargetLocalItem().length() > 0) {
            final GetEngine getEngine = new GetEngine(client);

            /*
             * Note: files cannot be binary or they would have failed the
             * preMerge check
             */
            final FileAttributesCollection attributes =
                getEngine.getAttributesForFile(conflict.getTargetLocalItem(), conflict.getServerPath(), true);

            if (attributes != null) {
                final StringPairFileAttribute attribute =
                    attributes.getStringPairFileAttribute(FileAttributeNames.CLIENT_EOL);

                if (attribute != null && attribute.getValue() != null) {
                    desiredNewlineSequence = FileAttributeValues.getEndOfLineStringForAttributeValue(attribute);

                    if (desiredNewlineSequence == null) {
                        log.error(
                            MessageFormat.format(
                                "Unsupported client end-of-line style ''{0}'' for ''{1}'' during automatic merge.  The automatically detected end-of-line style will be used instead.", //$NON-NLS-1$
                                attribute.getValue(),
                                conflict.getTargetLocalItem()));
                    } else if (desiredNewlineSequence.equals("")) //$NON-NLS-1$
                    {
                        desiredNewlineSequence = null;
                    }
                }
            }
        }

        final ThreeWayMerge twm = new ThreeWayMerge(desiredNewlineSequence);
        try {
            boolean ret = false;

            if (preMerge(conflict, twm, onlyCountConflicts)) {
                ret = twm.doInternalMerge(conflict, onlyCountConflicts);

                if (onlyCountConflicts == false) {
                    conflict.setMergedFileName(twm.getMergedFileName());
                }
            }

            return ret;
        } catch (final Exception e) {
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), workspace, e));
            return false;
        } finally {
            /*
             * Clean up after any files left around.
             */
            if (twm.getBaseFileName() != null) {
                TempStorageService.getInstance().cleanUpItem(new File(twm.getBaseFileName()));
            }
            if (twm.getTheirFileName() != null) {
                TempStorageService.getInstance().cleanUpItem(new File(twm.getTheirFileName()));
            }
        }
    }

    /**
     * Merges the content of the files described by the given conflict using the
     * user-defined external content merge tool. The conflict object is modified
     * to describe the merge result.
     * <p>
     * This method works by blocking until the external tool returns.
     *
     * @param conflict
     *        the conflict whose content should be merged (must not be
     *        <code>null</code>)
     * @param finishedHandler
     *        an event handler whose methods are invoked when the process runner
     *        used to run external merge tools reaches one of its terminal
     *        states. May be null if no state information is desired via the
     *        handler.
     * @param capturedStandardOutput
     *        a stream to capture the text written by the child process to its
     *        standard output stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @param capturedStandardError
     *        a stream to capture the text written by the child process to its
     *        standard error stream. Pass null if you don't want this output.
     *        <p>
     *        <b>See the warning in {@link ProcessRunner}'s Javadoc about
     *        deadlock.</b>
     * @return true if the merge was successful (the merge process was run),
     *         false if it was not (the merge process could not be started or
     *         there was a non-content-conflict error).
     */
    private boolean externalMergeContent(
        final Conflict conflict,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        final ThreeWayMerge twm = new ThreeWayMerge();
        try {
            final String fileName = conflict.getResolutionOptions().getNewPath() != null
                ? ServerPath.getFileName(conflict.getResolutionOptions().getNewPath()) : conflict.getTargetLocalItem();

            final ExternalTool tool = mergeToolset != null ? mergeToolset.findTool(fileName) : null;

            if (tool == null) {
                log.warn(MessageFormat.format(
                    "Could not find an external merge tool for the file {0}", //$NON-NLS-1$
                    conflict.getTargetLocalItem()));
                return false;
            }

            if (preMerge(conflict, twm, false)) {
                // Null finished handler because we block.
                final ProcessRunner runner = twm.beginExternalMerge(
                    conflict,
                    tool,
                    finishedHandler,
                    capturedStandardOutput,
                    capturedStandardError);
                final boolean ret = twm.endExternalMerge(runner);

                conflict.setMergedFileName(twm.getMergedFileName());

                return ret;
            }
        } catch (final Exception e) {
            client.getEventEngine().fireNonFatalError(new NonFatalErrorEvent(EventSource.newFromHere(), workspace, e));
            return false;
        } finally {
            /*
             * Clean up after any files left around.
             */
            if (twm.getBaseFileName() != null) {
                TempStorageService.getInstance().cleanUpItem(new File(twm.getBaseFileName()));
            }
            if (twm.getTheirFileName() != null) {
                TempStorageService.getInstance().cleanUpItem(new File(twm.getTheirFileName()));
            }
        }

        return false;
    }

    private boolean preMerge(final Conflict conflict, final ThreeWayMerge twm, final boolean onlyCountConflicts) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(twm, "twm"); //$NON-NLS-1$

        final FileEncoding baseFileEncoding = conflict.getBaseEncoding();
        final FileEncoding theirFileEncoding = conflict.getTheirEncoding();
        final FileEncoding yourFileEncoding = conflict.getYourEncoding();

        Check.notNull(baseFileEncoding, "baseFileEncoding"); //$NON-NLS-1$
        Check.notNull(theirFileEncoding, "theirFileEncoding"); //$NON-NLS-1$
        Check.notNull(yourFileEncoding, "yourFileEncoding"); //$NON-NLS-1$

        // Determine if we have any differences in encodings.
        final boolean encodingsDiffer = baseFileEncoding.getCodePage() > 0
            && ((!baseFileEncoding.equals(theirFileEncoding)) || (!baseFileEncoding.equals(yourFileEncoding)));

        // Are any of the encodings binary?
        final boolean anyBinary = baseFileEncoding.equals(FileEncoding.BINARY)
            || theirFileEncoding.equals(FileEncoding.BINARY)
            || yourFileEncoding.equals(FileEncoding.BINARY);

        final boolean isBaselessMerge = conflict.isBaseless();

        // Will we use an empty baseline?
        final boolean useEmptyBase = !conflict.isShelvesetConflict()
            && isBaselessMerge
            && ((conflict.getYourVersion() == conflict.getBaseVersion()
                && conflict.getYourItemID() == conflict.getBaseItemID())
                || (conflict.getTheirVersion() == conflict.getBaseVersion()
                    && conflict.getTheirItemID() == conflict.getBaseItemID()));

        /*
         * Determine the correct encodings for each file involved in the merge
         * process (3 inputs, 1 output).
         */
        if (anyBinary && conflict.getResolutionOptions().getEncodingStrategy() != EncodingStrategy.OVERRIDE_EXPLICIT) {
            client.getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    workspace,
                    new Exception(Messages.getString("MergeEngine.AtLeastOneInputtoMergeEngineIsBinary")))); //$NON-NLS-1$
            return false;
        } else if (conflict.getResolutionOptions().getEncodingStrategy() == EncodingStrategy.OVERRIDE_EXPLICIT) {
            /*
             * When an explicit encoding override has been chosen, all files in
             * the operation will be treated as these files.
             */

            final FileEncoding override = conflict.getResolutionOptions().getExplicitEncoding();
            Check.notNull(override, "override"); //$NON-NLS-1$

            twm.setMergedFileEncoding(override);
            twm.setBaseFileEncoding(override);
            twm.setModifiedFileEncoding(override);
            twm.setOriginalFileEncoding(override);
        } else {
            /*
             * Determine what the output (merged) encoding will be.
             */
            if (conflict.getResolutionOptions().getEncodingStrategy() == EncodingStrategy.CONVERT_EXPLICIT) {
                /*
                 * If we're supposed to convert, use the explicit encoding for
                 * the intermediate file, but use the existing modified file's
                 * encoding for the output (so we don't pend an encoding
                 * change).
                 */
                final FileEncoding convert = conflict.getResolutionOptions().getExplicitEncoding();
                Check.notNull(convert, "convert"); //$NON-NLS-1$

                twm.setIntermediateMergeEncoding(convert);
                twm.setMergedFileEncoding(yourFileEncoding);
            } else {
                /*
                 * If we're just counting the conflicts, do not enforce the
                 * requirement that encodings are the same, just pick an
                 * encoding and count the conflicts. The UI should require the
                 * user select an encoding to proceed.
                 */
                if (encodingsDiffer && !onlyCountConflicts) {
                    client.getEventEngine().fireNonFatalError(
                        new NonFatalErrorEvent(EventSource.newFromHere(), workspace, new Exception(
                            //@formatter:off
                            Messages.getString("MergeEngine.FilesHaveDifferentEncodingsAndConversionNotSpecified")))); //$NON-NLS-1$
                            //@formatter:on
                    return false;
                } else {
                    /*
                     * Use the your file encoding; as good as any.
                     */
                    twm.setMergedFileEncoding(yourFileEncoding);
                }
            }

            /*
             * Set the encodings for the three input files.
             */
            twm.setBaseFileEncoding(isBaselessMerge ? yourFileEncoding : baseFileEncoding);
            twm.setModifiedFileEncoding(yourFileEncoding);
            twm.setOriginalFileEncoding(theirFileEncoding);
        }

        // Make sure we have the source item, or else the merge can't be done.
        if (conflict.getSourceLocalItem() == null || new File(conflict.getSourceLocalItem()).exists() == false) {
            client.getEventEngine().fireNonFatalError(
                new NonFatalErrorEvent(
                    EventSource.newFromHere(),
                    workspace,
                    new Exception(
                        MessageFormat.format(
                            Messages.getString("MergeEngine.MergeCannotCompleteBecauseExistingFileNotAvailableFormat"), //$NON-NLS-1$
                            ((conflict.getSourceLocalItem() == null) ? "" : conflict.getSourceLocalItem()))))); //$NON-NLS-1$
            return false;
        }

        twm.setYourFileName(conflict.getSourceLocalItem());

        /*
         * Ensure we have copies of each of the files to use as inputs. Our temp
         * file downloader will put each of these files in their own
         * directories, so we can use the same file name for each part of the
         * merge.
         */
        try {
            // Base.
            twm.setBaseFileName(
                getTempFilePath(
                    ServerPath.getFileName(
                        isBaselessMerge ? conflict.getYourServerItem() : conflict.getBaseServerItem())));
            if (useEmptyBase) {
                // Create an empty file.
                new File(twm.getBaseFileName()).createNewFile();
            } else if (isBaselessMerge) {
                // Download the your file.
                conflict.downloadYourFile(client, twm.getBaseFileName());
            } else {
                // Download the base file.
                conflict.downloadBaseFile(client, twm.getBaseFileName());
            }

            // Latest.
            twm.setTheirFileName(getTempFilePath(ServerPath.getFileName(conflict.getTheirServerItem())));
            conflict.downloadTheirFile(client, twm.getTheirFileName());
        } catch (final IOException e) {
            throw new VersionControlException(e);
        }

        // Update the labels.
        createLabels(conflict, twm, isBaselessMerge);

        return true;
    }

    /**
     * Creates and sets label strings on the given three way merge object for
     * the given conflict's information.
     *
     * @param conflict
     *        the conflict to create labels for (must not be <code>null</code>)
     * @param twm
     *        the three way merge to set the labels on (must not be
     *        <code>null</code>)
     */
    private void createLabels(final Conflict conflict, final ThreeWayMerge twm, final boolean isBaselessMerge) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(twm, "twm"); //$NON-NLS-1$

        if (conflict.isShelvesetConflict()) {
            twm.setTheirFileLabel(MessageFormat.format(
                Messages.getString("MergeEngine.LabelTheirShelvesetFormat"), //$NON-NLS-1$
                conflict.getTheirServerItem(),
                conflict.getTheirShelvesetDisplayName(workspace)));

            twm.setBaseFileLabel(MessageFormat.format(
                Messages.getString("MergeEngine.LabelOriginalFormat"), //$NON-NLS-1$
                VersionedFileSpec.formatForPath(
                    conflict.getBaseServerItem(),
                    new ChangesetVersionSpec(conflict.getYourVersion()))));

            twm.setYourFileLabel(
                MessageFormat.format(Messages.getString("MergeEngine.LabelYoursFormat"), conflict.getYourServerItem())); //$NON-NLS-1$
        } else if (conflict.getType() == ConflictType.MERGE) {
            twm.setTheirFileLabel(MessageFormat.format(
                Messages.getString("MergeEngine.LabelTheirsMergeFormat"), //$NON-NLS-1$
                VersionedFileSpec.formatForPath(
                    conflict.getTheirServerItem(),
                    new ChangesetVersionSpec(conflict.getTheirVersion()))));

            twm.setBaseFileLabel(MessageFormat.format(
                Messages.getString("MergeEngine.LabelOriginalFormat"), //$NON-NLS-1$
                VersionedFileSpec.formatForPath(
                    isBaselessMerge ? conflict.getYourServerItem() : conflict.getBaseServerItem(),
                    new ChangesetVersionSpec(
                        isBaselessMerge ? conflict.getYourVersion() : conflict.getBaseVersion()))));

            twm.setYourFileLabel(MessageFormat.format(
                Messages.getString("MergeEngine.LabelYoursMergeFormat"), //$NON-NLS-1$
                VersionedFileSpec.formatForPath(
                    conflict.getYourServerItemSource(),
                    new ChangesetVersionSpec(conflict.getYourVersion()))));
        } else {
            twm.setTheirFileLabel(MessageFormat.format(
                Messages.getString("MergeEngine.LabelTheirsFormat"), //$NON-NLS-1$
                VersionedFileSpec.formatForPath(
                    conflict.getTheirServerItem(),
                    new ChangesetVersionSpec(conflict.getTheirVersion()))));

            twm.setBaseFileLabel(MessageFormat.format(
                Messages.getString("MergeEngine.LabelOriginalFormat"), //$NON-NLS-1$
                VersionedFileSpec.formatForPath(
                    conflict.getBaseServerItem(),
                    new ChangesetVersionSpec(conflict.getYourVersion()))));

            twm.setYourFileLabel(
                MessageFormat.format(Messages.getString("MergeEngine.LabelYoursFormat"), conflict.getYourServerItem())); //$NON-NLS-1$
        }

        twm.setMergedFileLabel(Messages.getString("MergeEngine.LabelMergeTarget")); //$NON-NLS-1$
    }

    /**
     * Creates a new temp directory and returns the full path to the given file
     * inside it (does not create the file).
     *
     * @param fileName
     *        the file name (not absolute path) to append to the newly created
     *        temp directory (must not be <code>null</code> or empty)
     * @return the full path to the temp file (which is not created, but parent
     *         directories are)
     */
    public String getTempFilePath(final String fileName) {
        Check.notNullOrEmpty(fileName, "fileName"); //$NON-NLS-1$

        try {
            return new File(TempStorageService.getInstance().createTempDirectory(), fileName).getAbsolutePath();
        } catch (final IOException e) {
            log.error(MessageFormat.format("error creating merge temp file {0}", fileName), e); //$NON-NLS-1$
            throw new VersionControlException(e);
        }
    }
}
