// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.vc;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.DateVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.util.LocaleUtil;

public class VersionSpecHelper {
    public static String getVersionSpecDescription(final VersionSpec version) {
        return getVersionSpecDescription(version, Locale.getDefault());
    }

    public static String getVersionSpecDescription(final GetRequest getRequest) {
        return getVersionSpecDescription(getRequest, Locale.getDefault());
    }

    public static String getVersionSpecDescriptionNOLOC(final VersionSpec version) {
        return getVersionSpecDescription(version, Locale.getDefault());
    }

    public static String getVersionSpecDescriptionNOLOC(final GetRequest getRequest) {
        return getVersionSpecDescription(getRequest, LocaleUtil.ROOT);
    }

    public static String getVersionSpecDescription(final VersionSpec version, final Locale locale) {
        if (version instanceof LatestVersionSpec) {
            return Messages.getString("VersionSpecHelper.Latest", locale); //$NON-NLS-1$
        } else if (version instanceof WorkspaceVersionSpec) {
            final String ver = ((WorkspaceVersionSpec) version).getName();
            final String owner = ((WorkspaceVersionSpec) version).getOwner();
            final String messageFormat = Messages.getString("VersionSpecHelper.WorkspaceVersionFormat", locale); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, ver, owner);
        } else if (version instanceof ChangesetVersionSpec) {
            final int changeset = ((ChangesetVersionSpec) version).getChangeset();
            final String messageFormat = Messages.getString("VersionSpecHelper.ChangesetFormat", locale); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, Integer.toString(changeset));
        } else if (version instanceof DateVersionSpec) {
            final String s =
                SimpleDateFormat.getDateTimeInstance().format(((DateVersionSpec) version).getDate().getTime());
            final String messageFormat = Messages.getString("VersionSpecHelper.DateFormat", locale); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, s);
        } else if (version instanceof LabelVersionSpec) {
            final String label = ((LabelVersionSpec) version).getLabel();
            final String messageFormat = Messages.getString("VersionSpecHelper.LabelFormat", locale); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, label);
        }

        return version.toString();
    }

    public static String getVersionSpecDescription(final GetRequest getRequest, final Locale locale) {
        return getVersionSpecDescription(getRequest.getVersionSpec(), locale);
    }
}
