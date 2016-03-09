// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.compare;

import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.vc.LaunchExternalCompareToolCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.JobOptions;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.common.ui.framework.compare.ExternalComparable;
import com.microsoft.tfs.client.common.ui.framework.compare.ExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.prefs.ExternalToolPreferenceKey;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.util.Check;

/**
 * Implements {@link ExternalCompareHandler} as a hook into the {@link Compare}
 * class framework to provide an external compare tool option when a file or
 * folder comparison is run.
 *
 * @threadsafety thread-compatible
 */
public class UserPreferenceExternalCompareHandler implements ExternalCompareHandler {
    private static final Log log = LogFactory.getLog(UserPreferenceExternalCompareHandler.class);

    private final Shell shell;

    /**
     * Creates a {@link UserPreferenceExternalCompareHandler}.
     *
     * @param shell
     *        the current shell (used when executing a command that provides UI
     *        status status
     */
    public UserPreferenceExternalCompareHandler(final Shell shell) {
        Check.notNull(shell, "shell"); //$NON-NLS-1$
        this.shell = shell;
    }

    /**
     * {@inheritDoc}
     *
     * Runs the external program (if one is configured) as another job, in order
     * to return control quickly.
     */
    @Override
    public boolean onCompare(
        final boolean threeWay,
        final IProgressMonitor monitor,
        final Object modified,
        final Object original,
        final Object ancestor) {
        if (!(modified instanceof ExternalComparable)
            || !(original instanceof ExternalComparable)
            || (threeWay && !(ancestor instanceof ExternalComparable))) {
            return false;
        }

        final ExternalComparable modifiedEC = (ExternalComparable) modified;
        final ExternalComparable originalEC = (ExternalComparable) original;
        final ExternalComparable ancestorEC = threeWay ? (ExternalComparable) ancestor : null;

        /*
         * Load the correct external compare toolset, using the preference key
         * contributed by the running product.
         */
        final ExternalToolset compareToolset = ExternalToolset.loadFromMemento(
            new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                ExternalToolPreferenceKey.COMPARE_KEY));

        /*
         * Determine the comparison tool type for directories, or for this
         * particular file.
         */
        final ExternalTool tool;

        if (ITypedElement.FOLDER_TYPE == modifiedEC.getType()) {
            tool = compareToolset.findToolForDirectory();
        } else {
            tool = compareToolset.findTool(modifiedEC.getName());
        }

        /*
         * No tool configured, so we can't handle it externally.
         */
        if (tool == null) {
            return false;
        }

        monitor.beginTask(
            Messages.getString("UserPreferenceExternalCompareHandler.ProgressPrepareCompare"), //$NON-NLS-1$
            threeWay ? 300 : 200);

        try {
            final String modifiedPath = modifiedEC.getExternalCompareFile(
                new SubProgressMonitor(
                    monitor,
                    100,
                    SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK)).getAbsolutePath();

            final String originalPath = originalEC.getExternalCompareFile(
                new SubProgressMonitor(
                    monitor,
                    100,
                    SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK)).getAbsolutePath();

            final String ancestorPath = threeWay ? ancestorEC.getExternalCompareFile(
                new SubProgressMonitor(
                    monitor,
                    100,
                    SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK)).getAbsolutePath()
                : null;

            final String modifiedLabel = CompareUtils.getLabel(modified);
            final String originalLabel = CompareUtils.getLabel(original);
            final String ancestorLabel = threeWay ? CompareUtils.getLabel(ancestor) : null;

            final String messageFormat = "Beginning external compare for {0} and {1}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, modifiedPath, originalPath);
            log.debug(message);

            final LaunchExternalCompareToolCommand compareCommand = new LaunchExternalCompareToolCommand(
                tool,
                originalPath,
                modifiedPath,
                ancestorPath,
                originalLabel,
                modifiedLabel,
                ancestorLabel);

            /*
             * These commands run at least as long as the external tool runs (if
             * it starts successfully), so use a job executor and job options to
             * hide it in the progress UI.
             */
            final JobOptions options = new JobOptions();
            options.setSystem(true);

            final ICommandExecutor executor = UICommandExecutorFactory.newUIJobCommandExecutor(shell, options);
            executor.execute(compareCommand);
        } catch (final InterruptedException e) {
            return true;
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            monitor.done();
        }

        return true;
    }
}
