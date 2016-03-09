// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertysources;

import java.text.DateFormat;
import java.util.Calendar;

import com.microsoft.tfs.client.common.util.DateHelper;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.util.ArrayUtils;
import com.microsoft.tfs.util.Check;

public class ChangePropertySource extends ReadonlyPropertySource {
    private final Change change;
    private final DateFormat dateFormat = DateHelper.getDefaultPropertyPageDateTimeFormat();

    public ChangePropertySource(final Change change) {
        Check.notNull(change, "change"); //$NON-NLS-1$

        this.change = change;
    }

    @Override
    protected void populate(final PropertyHolder propertyHolder) {
        final Item item = change.getItem();

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.ChangeTypesPropertyName"), //$NON-NLS-1$
            Messages.getString("ChangePropertySource.ChangeTypesPropertyDescription"), //$NON-NLS-1$
            change.getChangeType().toUIString(true, item));

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.ChangesetIdPropertyName"), //$NON-NLS-1$
            String.valueOf(item.getChangeSetID()));

        final Calendar c = item.getCheckinDate();
        Object value = (c != null ? dateFormat.format(c.getTime()) : null);
        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.CheckinDatePropertyName"), //$NON-NLS-1$
            Messages.getString("ChangePropertySource.CheckinDatePropertyDescription"), //$NON-NLS-1$
            value);

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.DeletionIdPropertyName"), //$NON-NLS-1$
            String.valueOf(item.getDeletionID()));

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.EncodingPropertyName"), //$NON-NLS-1$
            String.valueOf(item.getEncoding()));

        final ItemType itemType = item.getItemType();
        value = (itemType != null ? itemType.toString() : null);
        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.ItemTypePropertyName"), //$NON-NLS-1$
            Messages.getString("ChangePropertySource.ItemTypePropertyDescription"), //$NON-NLS-1$
            value);

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.ItemIdPropertyName"), //$NON-NLS-1$
            String.valueOf(item.getItemID()));

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.ServerItemPropertyName"), //$NON-NLS-1$
            Messages.getString("ChangePropertySource.ServerItemPropertyDescription"), //$NON-NLS-1$
            item.getServerItem());

        propertyHolder.addProperty(Messages.getString("ChangePropertySource.TimeZonePropertyName"), item.getTimeZone()); //$NON-NLS-1$

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.TimeZoneOffsetPropertyName"), //$NON-NLS-1$
            item.getTimeZoneO());

        final byte[] hash = item.getContentHashValue();
        value = (hash != null ? ArrayUtils.byteArrayToHexString(hash) : null);
        propertyHolder.addProperty(Messages.getString("ChangePropertySource.HashValuePropertyName"), value); //$NON-NLS-1$

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.ContentLengthPropertyName"), //$NON-NLS-1$
            String.valueOf(item.getContentLength()));

        propertyHolder.addProperty(
            Messages.getString("ChangePropertySource.DownloadUrlPropertyName"), //$NON-NLS-1$
            item.getDownloadURL());
    }
}
