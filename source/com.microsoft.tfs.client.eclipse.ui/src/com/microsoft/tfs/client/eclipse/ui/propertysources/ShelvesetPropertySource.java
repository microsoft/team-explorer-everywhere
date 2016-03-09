// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertysources;

import java.text.DateFormat;
import java.util.Calendar;

import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.util.Check;

public class ShelvesetPropertySource extends ReadonlyPropertySource {
    private final Shelveset shelveset;
    private final DateFormat dateFormat = DateHelper.getDefaultPropertyPageDateTimeFormat();

    public ShelvesetPropertySource(final Shelveset shelveset) {
        Check.notNull(shelveset, "shelveset"); //$NON-NLS-1$

        this.shelveset = shelveset;
    }

    @Override
    protected void populate(final PropertyHolder propertyHolder) {
        final Calendar c = shelveset.getCreationDate();
        final Object value = (c != null ? dateFormat.format(c.getTime()) : null);
        propertyHolder.addProperty(
            Messages.getString("ShelvesetPropertySource.CreationDatePropertyName"), //$NON-NLS-1$
            Messages.getString("ShelvesetPropertySource.CreationDatePropertyDescription"), //$NON-NLS-1$
            value);

        propertyHolder.addProperty(
            Messages.getString("ShelvesetPropertySource.NamePropertyName"), //$NON-NLS-1$
            Messages.getString("ShelvesetPropertySource.NamePropertyDescription"), //$NON-NLS-1$
            shelveset.getName());

        propertyHolder.addProperty(
            Messages.getString("ShelvesetPropertySource.OwnerNamePropertyName"), //$NON-NLS-1$
            Messages.getString("ShelvesetPropertySource.OwnerNamePropertyDescription"), //$NON-NLS-1$
            shelveset.getOwnerName());

        propertyHolder.addProperty(
            Messages.getString("ShelvesetPropertySource.CommentPropertyName"), //$NON-NLS-1$
            shelveset.getComment());

        propertyHolder.addProperty(
            Messages.getString("ShelvesetPropertySource.OverrideCommentPropertyName"), //$NON-NLS-1$
            shelveset.getPolicyOverrideComment());
    }
}
