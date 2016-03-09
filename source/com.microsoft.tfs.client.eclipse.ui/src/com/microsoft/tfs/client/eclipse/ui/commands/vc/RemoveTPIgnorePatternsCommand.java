// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.commands.vc;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.eclipse.tpignore.Line;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreDocument;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Removes from a specified .tpignore file the specified matching pattern lines.
 * The given patterns and lines in the file are trimmed (String#trim()) and
 * compared case-sensitively to determine a match.
 *
 * The {@link IFile} is validated for edit (
 * {@link IWorkspace#validateEdit(IFile[], Object)}) and modified in place.
 */
public class RemoveTPIgnorePatternsCommand extends Command {
    private final static Log log = LogFactory.getLog(RemoveTPIgnorePatternsCommand.class);

    private final IFile ignoreFile;
    private final String[] patterns;

    /**
     * Constructs a {@link RemoveTPIgnorePatternsCommand} to remove the
     * specified patterns from the specified file.
     *
     * @param ignoreFile
     *        the file to remove patterns from (must not be <code>null</code>)
     * @param patterns
     *        the patterns to remove (usually just the result of
     *        {@link Pattern#toString()}) (must not be <code>null</code>)
     */
    public RemoveTPIgnorePatternsCommand(final IFile ignoreFile, final String[] patterns) {
        Check.notNull(ignoreFile, "ignoreFile"); //$NON-NLS-1$
        Check.notNull(patterns, "patterns"); //$NON-NLS-1$

        this.ignoreFile = ignoreFile;
        this.patterns = patterns;
    }

    @Override
    public String getName() {
        return Messages.getString("RemoveTPIgnorePatternsCommand.CommandTextFormat"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("RemoveTPIgnorePatternsCommand.ErrorTextFormat"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("RemoveTPIgnorePatternsCommand.CommandTextFormat", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (ignoreFile.isReadOnly()) {
            final IStatus validateStatus = ignoreFile.getWorkspace().validateEdit(new IFile[] {
                ignoreFile
            }, null);

            if (validateStatus.isOK() == false) {
                return validateStatus;
            }
        }

        /*
         * Load and remove matching lines.
         */
        final TPIgnoreDocument doc = TPIgnoreDocument.read(ignoreFile);
        final List<Line> lines = doc.getLines();

        for (final String pattern : patterns) {
            final Iterator<Line> iterator = lines.iterator();
            while (iterator.hasNext()) {
                final Line line = iterator.next();
                if (line.getContents().trim().equals(pattern.trim())) {
                    iterator.remove();
                }
            }
        }

        doc.setLines(lines);

        /*
         * Save the new document. This triggers the
         * TPIgnoreResourceChangeListener which refresh label decoration.
         */
        ignoreFile.setContents(doc.getInputStream(), false, true, progressMonitor);

        return Status.OK_STATUS;
    }
}
