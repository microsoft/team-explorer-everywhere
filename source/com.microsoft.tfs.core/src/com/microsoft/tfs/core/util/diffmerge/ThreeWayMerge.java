// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.diffmerge;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import com.microsoft.tfs.core.clients.versioncontrol.MergeSummary;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions.EncodingStrategy;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.core.util.CodePageMapping;
import com.microsoft.tfs.core.util.CodePageMapping.UnknownCodePageException;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.diffmerge.internal.libsgdcore_lite;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessFinishedHandler;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * Encapsulates a three-way-merge operation, supporting internal and external
 * merge methods.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public final class ThreeWayMerge {
    /*
     * Our four main file I/O objects.
     */

    /**
     * The merge file is the output file of the merge process. It should not
     * exist on disk before an external merge tool is invoked, and will exist
     * afterward if the merge was successful.
     */
    private String mergedFileLabel = null;
    private String mergedFileName = null;
    private FileEncoding mergedEncoding = null;

    /**
     * The base file is the most common ancestor of your change and the other
     * guy's changes.
     */
    private String baseFileLabel = null;
    private String baseFileName = null;
    private FileEncoding baseEncoding = null;

    /**
     * Your file is the file on disk that you have edited that requires its
     * changes to be merged with the other guy's changes.
     */
    private String yourFileLabel = null;
    private String yourFileName = null;
    private FileEncoding yourEncoding = null;

    /**
     * Their file is the latest file in the server--the one the other guy
     * checked in that forced you to do a merge because you were also making
     * changes.
     */
    private String theirFileLabel = null;
    private String theirFileName = null;
    private FileEncoding theirEncoding = null;

    /*
     * Additional merge resources.
     */
    private FileEncoding intermediateEncoding = null;

    /**
     * When set (must not be <code>null</code>), this line ending character
     * sequence is always used when writing the merged file. When not set, an
     * end-of-line sequence detected from one of the input files is used
     * instead.
     */
    private String overrideMergedFileEndOfLine = null;

    /**
     * Creates a {@link ThreeWayMerge} that uses default configuration.
     */
    public ThreeWayMerge() {
    }

    /**
     * Creates a {@link ThreeWayMerge} that always uses the given end-of-line
     * character sequence in the output file, or the default configuration if
     * the parameter is null.
     *
     * @param overrideMergedFileEndOfLine
     *        the end-of-line character sequence to use when writing the merged
     *        file, or null to use the default (detected) end-of-line character
     *        sequence.
     */
    public ThreeWayMerge(final String overrideMergedFileEndOfLine) {
        this.overrideMergedFileEndOfLine = overrideMergedFileEndOfLine;
    }

    /*
     * File names.
     */

    public void setBaseFileName(final String file) {
        baseFileName = file;
    }

    public String getBaseFileName() {
        return baseFileName;
    }

    public void setTheirFileName(final String file) {
        theirFileName = file;
    }

    public String getTheirFileName() {
        return theirFileName;
    }

    public void setYourFileName(final String file) {
        yourFileName = file;
    }

    public String getYourFileName() {
        return yourFileName;
    }

    public void setMergedFileName(final String file) {
        mergedFileName = file;
    }

    public String getMergedFileName() {
        return mergedFileName;
    }

    public String getBaseFileLabel() {
        return baseFileLabel;
    }

    public String getMergedFileLabel() {
        return mergedFileLabel;
    }

    public String getTheirFileLabel() {
        return theirFileLabel;
    }

    public String getYourFileLabel() {
        return yourFileLabel;
    }

    public void setBaseFileLabel(final String baseFileLabel) {
        this.baseFileLabel = baseFileLabel;
    }

    public void setMergedFileLabel(final String mergedFileLabel) {
        this.mergedFileLabel = mergedFileLabel;
    }

    public void setTheirFileLabel(final String theirFileLabel) {
        this.theirFileLabel = theirFileLabel;
    }

    public void setYourFileLabel(final String yourFileLabel) {
        this.yourFileLabel = yourFileLabel;
    }

    /*
     * Encodings.
     */

    public void setBaseFileEncoding(final FileEncoding encoding) {
        baseEncoding = encoding;
    }

    public FileEncoding getBaseFileEncoding() {
        return baseEncoding;
    }

    public void setOriginalFileEncoding(final FileEncoding encoding) {
        theirEncoding = encoding;
    }

    public FileEncoding getOriginalFileEncoding() {
        return theirEncoding;
    }

    public void setModifiedFileEncoding(final FileEncoding encoding) {
        yourEncoding = encoding;
    }

    public FileEncoding getModifiedFileEncoding() {
        return yourEncoding;
    }

    public void setMergedFileEncoding(final FileEncoding encoding) {
        mergedEncoding = encoding;
    }

    public FileEncoding getMergedFileEncoding() {
        return mergedEncoding;
    }

    /*
     * Other resources.
     */

    /**
     * Sets the file encoding that will be used for the intermediate merge step.
     *
     * @param encoding
     */
    public void setIntermediateMergeEncoding(final FileEncoding encoding) {
        intermediateEncoding = encoding;
    }

    /**
     * Returns the file encoding that should be used in the intermediate merge
     * step.
     *
     * @return The FileEncoding to use.
     */
    public FileEncoding getIntermediateMergeEncoding() {
        return intermediateEncoding;
    }

    /*
     * Actions.
     */

    /**
     * Merges the content that would resolve the given conflict via an external
     * tool the user has configured. If the merge cannot happen (conflicts in
     * the files), false is returned and no merged file is written. The conflict
     * object is modified to include the conflicts found and result output.
     * <p>
     * For the state of the external merge process (whether it has completed
     * normally, with error, still running), pass a Process
     *
     * @param conflict
     *        the conflict to merge content for.
     * @param tool
     *        the merge tool to use (must not be <code>null</code>)
     * @param finishedHandler
     *        an event handler whose methods are invoked when the process runner
     *        reaches one of its terminal states. The caller would normally
     *        implement the handler to call
     *        {@link ExternalRunner#endMerge(ProcessRunner, String)} when the
     *        runner reaches any terminal state. May be null if no state
     *        information is desired via the handler.
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
     * @return the process runner that represents the running external merge
     *         tool.
     * @throws IOException
     *         if an error occurred creating the merge output file before the
     *         merge tool was invoked.
     * @throws ExternalToolException
     *         if the configured merge command or arguments string caused a
     *         problem creating the merge tool.
     */
    public ProcessRunner beginExternalMerge(
        final Conflict conflict,
        final ExternalTool tool,
        final ProcessFinishedHandler finishedHandler,
        final OutputStream capturedStandardOutput,
        final OutputStream capturedStandardError) throws IOException, ExternalToolException {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$
        Check.notNull(tool, "tool"); //$NON-NLS-1$

        /*
         * If we have not yet been configured to have an output file, use a
         * temporary file in the same directory as "your" file.
         */
        if (getMergedFileName() == null) {
            final File tempMergedFile = createMergeOutputFile();
            setMergedFileName(tempMergedFile.getAbsolutePath());
        }

        final File mergedFile = new File(getMergedFileName());

        /*
         * Delete the output file so the merge tool is not confused by its
         * existence.
         */
        mergedFile.delete();

        // TODO Set label properties that this class does not currently have

        return ExternalRunner.beginMerge(this, tool, finishedHandler, capturedStandardOutput, capturedStandardError);
    }

    /**
     * Completes the external merge that was started by beginExternalMerge (and
     * represented by the given mergeProcess). This method will block until the
     * external merge tool has completed and returned an exit code.
     *
     * @param mergeProcess
     *        the merge process to complete (if null, false is returned).
     * @return true if the merge result file was created, false if there was an
     *         error or no file was created.
     */
    public boolean endExternalMerge(final ProcessRunner mergeProcess) {
        if (mergeProcess == null) {
            return false;
        }

        // TODO convert encodings back

        return ExternalRunner.endMerge(mergeProcess, getMergedFileName());
    }

    /**
     * Merges the content that would resolve the given conflict. If the merge
     * cannot happen (conflicts in the files), false is returned and no merged
     * file is written. The conflict object is modified to include the conflicts
     * found and result output.
     *
     * @param conflict
     *        the conflict to merge content for.
     * @param onlyCountConflicts
     *        if true, no merge output will result, only conflicts are counted
     *        (and written to the conflict parameter's MergeSummary).
     * @throws UnknownCodePageException
     *         if one of the input code pages or the output code pages cannot be
     *         converted to a {@link Charset}.
     * @return true if no conflicts were found and the merge result file was
     *         created.
     */
    public boolean doInternalMerge(final Conflict conflict, final boolean onlyCountConflicts) {
        final Charset[] detectedCharset = new Charset[1];
        final String[] detectedEol = new String[1];
        final boolean[] detectedTrailingEol = {
            true
        };

        final Charset yourCharset = CodePageMapping.getCharset(conflict.getYourEncoding().getCodePage());
        final Charset theirCharset = CodePageMapping.getCharset(conflict.getTheirEncoding().getCodePage());

        final int baseCodePage = conflict.getBaseEncoding().getCodePage();
        final Charset baseCharset = baseCodePage > 0 ? CodePageMapping.getCharset(baseCodePage) : yourCharset;

        /*
         * First do a diff for conflicts.
         */
        final libsgdcore_lite.svn_diff_t diff = libsgdcore_lite.svn_diff3_file(
            getBaseFileName(),
            baseCharset,
            getYourFileName(),
            yourCharset,
            getTheirFileName(),
            theirCharset,
            detectedCharset,
            detectedEol,
            detectedTrailingEol);

        /*
         * Count the conflicts.
         */
        int common = 0;
        int diffModified = 0;
        int diffLatest = 0;
        int diffCommon = 0;
        int conflicts = 0;

        libsgdcore_lite.svn_diff_t iteratorDiff = diff;
        while (iteratorDiff != null) {
            if (iteratorDiff.type == libsgdcore_lite.type_e.type_common) {
                common++;
            } else if (iteratorDiff.type == libsgdcore_lite.type_e.type_diff_modified) {
                diffModified++;
            } else if (iteratorDiff.type == libsgdcore_lite.type_e.type_diff_latest) {
                diffLatest++;
            } else if (iteratorDiff.type == libsgdcore_lite.type_e.type_diff_common) {
                diffCommon++;
            } else if (iteratorDiff.type == libsgdcore_lite.type_e.type_conflict) {
                conflicts++;
            }

            iteratorDiff = iteratorDiff.next;
        }

        conflict.setContentMergeSummary(new MergeSummary(common, diffModified, diffLatest, diffCommon, conflicts));

        if (conflicts > 0 || onlyCountConflicts) {
            return false;
        }

        /*
         * Proceed to merge into the output file.
         */

        try {
            final File mergedFile = createMergeOutputFile();

            final FileOutputStream fos = new FileOutputStream(mergedFile);

            /*
             * If the resolution specified an output encoding, use that.
             * Otherwise, use the detected charset from the input files (will be
             * identical to what was passed in, otherwise this would have led to
             * a type conflict.)
             */
            Charset outputCharset = detectedCharset[0];

            if (conflict.getResolutionOptions().getEncodingStrategy() == EncodingStrategy.CONVERT_EXPLICIT) {
                outputCharset =
                    CodePageMapping.getCharset(conflict.getResolutionOptions().getExplicitEncoding().getCodePage());
            }

            final OutputStreamWriter encWriter =
                (outputCharset != null) ? new OutputStreamWriter(fos, outputCharset) : new OutputStreamWriter(fos);

            final BufferedWriter bw = new BufferedWriter(encWriter);

            /*
             * If an end-of-line override was specified, use it instead of the
             * one we detected.
             */
            libsgdcore_lite.svn_diff3_file_output(
                bw,
                diff,
                (overrideMergedFileEndOfLine != null) ? overrideMergedFileEndOfLine : detectedEol[0],
                detectedTrailingEol[0],
                getBaseFileName(),
                baseCharset,
                getYourFileName(),
                yourCharset,
                getTheirFileName(),
                theirCharset,
                null,
                null,
                null,
                null,
                true,
                false);
            bw.close();

            setMergedFileName(mergedFile.getAbsolutePath());
        } catch (final Throwable e) {
            return false;
        }

        return true;
    }

    /**
     * Creates an output file for an internal or external merge by detecting the
     * extension of the original file and creating a temp file with the same
     * extension in the same directory as the original. The same directory is
     * used to facilitate easy replacement via {@link File#renameTo(File)}
     * (which is most likely to work in the case where the two files are on the
     * same filesystem).
     * <p>
     * The caller gets to clean up the returned file. If the file is not renamed
     * to something else before the application exits,
     * {@link TempStorageService} will clean it up (delete it).
     *
     * @return the temp file to be used for a merge destination.
     */
    private File createMergeOutputFile() throws IOException {
        final File modifiedFile = new File(getYourFileName());

        final File mergedFile = TempStorageService.getInstance().createTempFile(
            modifiedFile.getParentFile(),
            LocalPath.getFileExtension(modifiedFile.getAbsolutePath()));

        return mergedFile;
    }
}
