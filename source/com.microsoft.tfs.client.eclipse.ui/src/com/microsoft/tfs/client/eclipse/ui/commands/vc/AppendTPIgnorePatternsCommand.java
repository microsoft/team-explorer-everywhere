// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.commands.vc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.eclipse.tpignore.Line;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreCache;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreDocument;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Constructs regular expressions which match the specified resources and
 * appends those expressions as new lines to the specified .tpignore. The file's
 * existing newline conventions are used. The default file encoding is always
 * used.
 *
 * file appropriate for the from a specified .tpignore file the specified
 * matching pattern lines. The given patterns and lines in the file are trimmed
 * (String#trim()) and compared case-sensitively to determine a match.
 *
 * The {@link IFile} is validated for edit (
 * {@link IWorkspace#validateEdit(IFile[], Object)}) and modified in place.
 */
public class AppendTPIgnorePatternsCommand extends Command {
    private final static Log log = LogFactory.getLog(AppendTPIgnorePatternsCommand.class);

    private final IFile ignoreFile;
    private final IResource[] resources;

    /**
     * Updates the .tpignore file with a pattern for each given
     * {@link IResource}. The file is not checked for existing matches (may
     * create duplicates) and is created if it does not exist. Resources must
     * all share the same {@link IProject}.
     *
     * @param resources
     *        the resources to create entries for (must not be <code>null</code>
     *        )
     */
    public AppendTPIgnorePatternsCommand(final IFile ignoreFile, final IResource[] resources) {
        Check.notNull(ignoreFile, "ignoreFile"); //$NON-NLS-1$
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        this.ignoreFile = ignoreFile;
        this.resources = resources;
    }

    @Override
    public String getName() {
        return Messages.getString("AppendTPIgnorePatternsCommand.CommandTextFormat"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("AppendTPIgnorePatternsCommand.ErrorTextFormat"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("AppendTPIgnorePatternsCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (resources.length == 0) {
            return Status.OK_STATUS;
        }

        if (ignoreFile.isReadOnly()) {
            final IStatus validateStatus = ignoreFile.getWorkspace().validateEdit(new IFile[] {
                ignoreFile
            }, null);

            if (validateStatus.isOK() == false) {
                return validateStatus;
            }
        }

        /*
         * Load the document and add lines.
         */
        final TPIgnoreDocument doc = TPIgnoreDocument.read(ignoreFile);
        for (final IResource resource : resources) {
            doc.addLine(new Line(TPIgnoreCache.createIgnorePatternForResource(resource)));
        }

        /*
         * Save the new document. This triggers the
         * TPIgnoreResourceChangeListener which refresh label decoration.
         */
        doc.write(ignoreFile, progressMonitor);

        return Status.OK_STATUS;
    }
}
