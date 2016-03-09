// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.formatters.ViewToolArgumentFormatter;
import com.microsoft.tfs.core.util.diffmerge.ExternalRunner;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessRunner;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * <p>
 * Launches an external view tool and waits for the tool to exit. Returns an
 * error status only if the tool failed to launch (the exit code is ignored if
 * the tool could be started at all).
 * </p>
 * <p>
 * This command is long-running, so manage it appropriately so it does not block
 * other tasks.
 * </p>
 */
public class LaunchExternalViewToolCommand extends AbstractLaunchExternalToolCommand {
    private static final Log log = LogFactory.getLog(LaunchExternalViewToolCommand.class);

    private final ExternalTool tool;
    private final String path;
    private final boolean cleanUpFiles;

    /**
     * Creates a command that launches an external view tool.
     *
     * @param tool
     *        the tool to launch. Not null.
     * @param path
     *        the local path to the file or folder to view (not null)
     * @param cleanUpFiles
     *        if true the input files are cleaned up with
     *        {@link TempStorageService#cleanUpItem(String)} (which only deletes
     *        files from disk if they originated from the
     *        {@link TempStorageService}) when the external process finishes. If
     *        false the input files are never deleted under any circumstances
     */
    public LaunchExternalViewToolCommand(final ExternalTool tool, final String path, final boolean cleanUpFiles) {
        Check.notNull(tool, "tool"); //$NON-NLS-1$
        Check.notNull(path, "filePath"); //$NON-NLS-1$

        this.tool = tool;
        this.path = path;
        this.cleanUpFiles = cleanUpFiles;
    }

    @Override
    public String getName() {
        return (Messages.getString("LaunchExternalViewToolCommand.CommandText")); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return (Messages.getString("LaunchExternalViewToolCommand.ErrorText")); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        /* Viewer logs better details than we can */
        return null;
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        /*
         * One tick for launching, one tick for cleaning up the inputs.
         */
        progressMonitor.beginTask(getName(), 2);

        String messageFormat = "Beginning external view for {0}"; //$NON-NLS-1$
        String message = MessageFormat.format(messageFormat, path);
        log.info(message);

        final String[] arguments = new ViewToolArgumentFormatter().formatArguments(tool, path);

        // Copy the command and arguments into one array.
        final String[] fullCommandAndArguments =
            ExternalRunner.combineCommandAndArguments(tool.getCommand(), arguments);

        /*
         * Run the process synchronously.
         */
        final ProcessRunner runner = new ProcessRunner(fullCommandAndArguments, null, null, null);

        messageFormat = "Running external view tool like: {0}"; //$NON-NLS-1$
        message = MessageFormat.format(messageFormat, runner.getCommandLineForDisplay());
        log.info(message);

        runner.run();
        progressMonitor.worked(1);

        if (cleanUpFiles) {
            TempStorageService.getInstance().cleanUpItem(new File(path));
        }
        progressMonitor.worked(1);

        return makeStatusForProcessRunner(runner);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean isOperationCancelable() {
        return false;
    }

}