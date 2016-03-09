// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertysources;

import java.text.DateFormat;
import java.util.Calendar;

import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.util.Check;

public class ChangesetPropertySource extends ReadonlyPropertySource {
    private final Changeset changeset;
    private final DateFormat dateFormat = DateHelper.getDefaultPropertyPageDateTimeFormat();

    public ChangesetPropertySource(final Changeset changeset) {
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$

        this.changeset = changeset;
    }

    @Override
    protected void populate(final PropertyHolder propertyHolder) {
        propertyHolder.addProperty(
            Messages.getString("ChangesetPropertySource.CommitterPropertyName"), //$NON-NLS-1$
            changeset.getCommitter());

        final Calendar c = changeset.getDate();
        final Object value = (c != null ? dateFormat.format(c.getTime()) : null);
        propertyHolder.addProperty(
            Messages.getString("ChangesetPropertySource.DatePropertyName"), //$NON-NLS-1$
            Messages.getString("ChangesetPropertySource.DatePropertyDescription"), //$NON-NLS-1$
            value);

        propertyHolder.addProperty(
            Messages.getString("ChangesetPropertySource.ChangesetIdPropertyName"), //$NON-NLS-1$
            Messages.getString("ChangesetPropertySource.ChangesetIdPropertyDescription"), //$NON-NLS-1$
            String.valueOf(changeset.getChangesetID()));

        propertyHolder.addProperty(
            Messages.getString("ChangesetPropertySource.OwnerPropertyName"), //$NON-NLS-1$
            changeset.getOwner());

        propertyHolder.addProperty(
            Messages.getString("ChangesetPropertySource.CommentPropertyName"), //$NON-NLS-1$
            changeset.getComment());
    }
}
