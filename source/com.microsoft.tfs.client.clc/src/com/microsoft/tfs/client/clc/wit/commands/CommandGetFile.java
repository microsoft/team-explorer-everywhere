// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.commands;

import java.io.File;
import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.wit.options.OptionFileID;
import com.microsoft.tfs.client.clc.wit.options.OptionOverwrite;
import com.microsoft.tfs.client.clc.wit.options.OptionWorkItemID;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.files.Attachment;
import com.microsoft.tfs.core.clients.workitem.files.DownloadException;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public class CommandGetFile extends BaseWITCommand {
    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandGetFile.HelpText1") //$NON-NLS-1$
        };
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            /*
             * wit getfile /workitem:<id> /fileid:<id> [/overwrite] [local
             * filespec]
             */
            new AcceptedOptionSet(new Class[] {
                OptionOverwrite.class
            }, "[local filespec]", new Class[] //$NON-NLS-1$
            {
                OptionWorkItemID.class,
                OptionFileID.class
            })
        };
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        Option o = null;

        if ((o = findOptionType(OptionWorkItemID.class)) == null) {
            throw new InvalidOptionException(Messages.getString("CommandGetFile.WorkitemOptionRequired")); //$NON-NLS-1$
        }
        final int workItemId = ((OptionWorkItemID) o).getNumber();

        if ((o = findOptionType(OptionFileID.class)) == null) {
            throw new InvalidOptionException(Messages.getString("CommandGetFile.FieldOptionRequired")); //$NON-NLS-1$
        }
        final int fileId = ((OptionFileID) o).getNumber();

        final WorkItem workItem = getWorkItemByID(workItemId);

        final Attachment attachment = workItem.getAttachments().getAttachmentByFileID(fileId);
        if (attachment == null) {
            final String messageFormat = Messages.getString("CommandGetFile.WorkItemDidNotHaveFileAttachmentFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                Integer.toString(workItem.getFields().getID()),
                Integer.toString(fileId));

            throw new CLCException(message);
        }

        final File localTarget = computeLocalDownloadTarget(attachment.getFileName());

        final String messageFormat = Messages.getString("CommandGetFile.DownloadingFormat"); //$NON-NLS-1$
        final String message =
            MessageFormat.format(messageFormat, attachment.getURL().toExternalForm(), localTarget.getAbsolutePath());

        getDisplay().getPrintStream().println(message);
        try {
            attachment.downloadTo(localTarget);
        } catch (final DownloadException ex) {
            final String exceptionMessageFormat = Messages.getString("CommandGetFile.ErrorDuringDownloadFormat"); //$NON-NLS-1$
            final String exceptionMessage = MessageFormat.format(exceptionMessageFormat, ex.getLocalizedMessage());

            log.warn(exceptionMessage, ex);
            throw new CLCException(exceptionMessage);
        }
        getDisplay().getPrintStream().println(Messages.getString("CommandGetFile.DownloadComplete")); //$NON-NLS-1$
    }

    private File computeLocalDownloadTarget(final String fileName) throws CLCException {
        File target = null;

        if (getFreeArguments().length > 0) {
            target = new File(getFreeArguments()[0]);
            if (target.isDirectory()) {
                target = new File(target, fileName);
            }
        } else {
            target = new File(fileName);
        }

        if (target.exists()) {
            if (findOptionType(OptionOverwrite.class) == null) {
                final String messageFormat =
                    Messages.getString("CommandGetFile.LocalDownloadTargetExistsAndOverwriteNotSpecifiedFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, target.getAbsolutePath());

                throw new CLCException(message);
            }
        }

        return target;
    }
}
