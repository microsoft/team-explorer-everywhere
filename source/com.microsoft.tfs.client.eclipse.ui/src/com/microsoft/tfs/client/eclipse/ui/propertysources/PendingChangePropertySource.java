// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertysources;

import java.text.DateFormat;
import java.util.Calendar;

import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.util.ArrayUtils;
import com.microsoft.tfs.util.Check;

public class PendingChangePropertySource extends ReadonlyPropertySource {
    private final PendingChange pendingChange;
    private final DateFormat dateFormat = DateHelper.getDefaultPropertyPageDateTimeFormat();
    private final PropertyValue[] properties;

    public PendingChangePropertySource(final PendingChange pendingChange, final PropertyValue[] properties) {
        Check.notNull(pendingChange, "pendingChange"); //$NON-NLS-1$

        this.pendingChange = pendingChange;
        this.properties = properties;
    }

    @Override
    protected void populate(final PropertyHolder propertyHolder) {
        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.ChangeTypePropertyName"), //$NON-NLS-1$
            Messages.getString("PendingChangePropertySource.ChangeTypePropertyDescription"), //$NON-NLS-1$
            pendingChange.getChangeType().toUIString(true, properties));

        final Calendar c = pendingChange.getCreationDate();
        Object value = (c != null ? dateFormat.format(c.getTime()) : null);
        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.CreationDatePropertyName"), //$NON-NLS-1$
            Messages.getString("PendingChangePropertySource.CreationDatePropertyDescription"), //$NON-NLS-1$
            value);

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.DeletionIDPropertyName"), //$NON-NLS-1$
            String.valueOf(pendingChange.getDeletionID()));

        final ItemType itemType = pendingChange.getItemType();
        value = (itemType != null ? itemType.toString() : null);
        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.ItemTypePropertyName"), //$NON-NLS-1$
            Messages.getString("PendingChangePropertySource.ItemTypePropertyDescription"), //$NON-NLS-1$
            value);

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.EncodingPropertyName"), //$NON-NLS-1$
            String.valueOf(pendingChange.getEncoding()));

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.EncodingPropertyDescription"), //$NON-NLS-1$
            String.valueOf(pendingChange.getItemID()));

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.LocalItemPropertyName"), //$NON-NLS-1$
            Messages.getString("PendingChangePropertySource.LocalItemPropertyDescription"), //$NON-NLS-1$
            pendingChange.getLocalItem());

        final LockLevel lockLevel = pendingChange.getLockLevel();
        value = (lockLevel != null ? lockLevel.toString() : null);
        propertyHolder.addProperty(Messages.getString("PendingChangePropertySource.LockLevelPropertyName"), value); //$NON-NLS-1$

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.ServerItemPropertyName"), //$NON-NLS-1$
            Messages.getString("PendingChangePropertySource.ServerItemPropertyDescription"), //$NON-NLS-1$
            pendingChange.getServerItem());

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.SourceLocalItemPropertyName"), //$NON-NLS-1$
            pendingChange.getSourceLocalItem());

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.SourceServerItemPropertyName"), //$NON-NLS-1$
            Messages.getString("PendingChangePropertySource.SourceServerItemPropertyDescription"), //$NON-NLS-1$
            pendingChange.getSourceServerItem());

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.VersionPropertyName"), //$NON-NLS-1$
            Messages.getString("PendingChangePropertySource.VersionPropertyDescription"), //$NON-NLS-1$
            String.valueOf(pendingChange.getVersion()));

        byte[] hash = pendingChange.getHashValue();
        value = (hash != null ? ArrayUtils.byteArrayToHexString(hash) : null);
        propertyHolder.addProperty(Messages.getString("PendingChangePropertySource.HashValuePropertyName"), value); //$NON-NLS-1$

        hash = pendingChange.getUploadContentHashValue();
        value = (hash != null ? ArrayUtils.byteArrayToHexString(hash) : null);
        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.UploadHashValuePropertyName"), //$NON-NLS-1$
            value);

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.PropertyChangeIDPropertyName"), //$NON-NLS-1$
            String.valueOf(pendingChange.getPendingChangeID()));

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.DownloadURLPropertyName"), //$NON-NLS-1$
            pendingChange.getDownloadURL());

        propertyHolder.addProperty(
            Messages.getString("PendingChangePropertySource.ShelvedDownloadUrlPropertyName"), //$NON-NLS-1$
            pendingChange.getShelvedDownloadURL());
    }
}
