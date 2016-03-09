// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.formatters.CompareToolArgumentFormatter;
import com.microsoft.tfs.core.util.diffmerge.ExternalRunner;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * <p>
 * Launches an external compare tool and waits for the tool to exit. Returns an
 * error status only if the tool failed to launch (the exit code is ignored if
 * the tool could be started at all).
 * </p>
 * <p>
 * This command is long-running, so manage it appropriately so it does not block
 * other tasks.
 * </p>
 */
public class LaunchExternalCompareToolCommand extends AbstractLaunchExternalToolCommand {
    private static final Log log = LogFactory.getLog(LaunchExternalCompareToolCommand.class);

    private final ExternalTool tool;

    private final String originalPath;
    private final String modifiedPath;
    private final String originalLabel;
    private final String modifiedLabel;

    /**
     * Creates a command that launches an external compare tool. Currently the
     * ancestor path and label are ignored (never passed to tools).
     *
     * @param tool
     *        the tool to launch. Not null.
     * @param originalPath
     *        the local path to the original file (server version or older
     *        version).
     * @param modifiedPath
     *        the local path to the modified file (local or newer version - not
     *        null).
     * @param ancestorPath
     *        the local path to the file to show as the ancestor (may be null).
     * @param originalLabel
     *        the label string to pass for the original item (may be null or
     *        empty).
     * @param modifiedLabel
     *        the label string to pass for the modified (local) item (may be
     *        null or empty).
     * @param ancestorLabel
     *        the label string to pass for the ancestor item (may be null or
     *        empty).
     * @param cleanUpFiles
     *        if true the input files are cleaned up with
     *        {@link TempStorageService#cleanUpItem(String)} (which only deletes
     *        files from disk if they originated from the
     *        {@link TempStorageService}) when the external process finishes. If
     *        false the input files are never deleted under any circumstances
     * @todo pass ancestorPath and ancestorLabel to configured external tool
     */
    public LaunchExternalCompareToolCommand(
        final ExternalTool tool,
        final String originalPath,
        final String modifiedPath,
        final String ancestorPath,
        final String originalLabel,
        final String modifiedLabel,
        final String ancestorLabel) {
        Check.notNull(tool, "tool"); //$NON-NLS-1$
        Check.notNull(originalPath, "originalPath"); //$NON-NLS-1$
        Check.notNull(modifiedPath, "modifiedPath"); //$NON-NLS-1$
        // Ancestor may be null.

        this.tool = tool;

        this.originalPath = originalPath;
        this.modifiedPath = modifiedPath;
        // Empty strings are what the MergeTool wants to use when substituting.
        this.originalLabel = (originalLabel != null) ? originalLabel : ""; //$NON-NLS-1$
        this.modifiedLabel = (modifiedLabel != null) ? modifiedLabel : ""; //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return (Messages.getString("LaunchExternalCompareToolCommand.CommandText")); //$NON-NLS-1$

    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("LaunchExternalCompareToolCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /* Launcher logs better details than we can */
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        /*
         * One tick for launching, one tick for cleaning up the inputs.
         */
        progressMonitor.beginTask(getName(), 1);

        log.info("Beginning external compare for " + modifiedPath + " with " + originalPath); //$NON-NLS-1$ //$NON-NLS-2$

        // TODO use the ancestor stuff
        final String[] arguments = new CompareToolArgumentFormatter().formatArguments(
            tool,
            originalPath,
            modifiedPath,
            originalLabel,
            modifiedLabel);

        // Copy the command and arguments into one array.
        final String[] fullCommandAndArguments =
            ExternalRunner.combineCommandAndArguments(tool.getCommand(), arguments);

        /*
         * Run the process synchronously.
         */
        final ProcessRunner runner = new ProcessRunner(fullCommandAndArguments, null, null, null);

        log.info("Running external compare tool like: " + runner.getCommandLineForDisplay()); //$NON-NLS-1$
        runner.run();
        progressMonitor.worked(1);

        return makeStatusForProcessRunner(runner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCancellable() {
        return false;
    }
}
