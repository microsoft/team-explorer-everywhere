// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.Calendar;

import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.util.Closable;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.datetime.DotNETDate;

public class InitiallyDeletedLocalVersionUpdate implements IPopulatableLocalVersionUpdate, Closable {
    public InitiallyDeletedLocalVersionUpdate(
        final String sourceServerItem,
        final int itemID,
        final int versionLocal,
        final Calendar versionLocalDate,
        final int encoding,
        final String pendingChangeTargetServerItem) {
        this.sourceServerItem = sourceServerItem;
        this.itemID = itemID;
        this.versionLocal = versionLocal;
        this.versionLocalDate =
            DotNETDate.MIN_CALENDAR.equals(versionLocalDate) ? -1 : DotNETDate.toWindowsFileTimeUTC(versionLocalDate);
        this.encoding = encoding;
        this.pendingChangeTargetServerItem = pendingChangeTargetServerItem;
    }

    @Override
    public boolean isSendToServer() {
        return false;
    }

    @Override
    public boolean isCommitted() {
        return 0 != versionLocal;
    }

    @Override
    public String getSourceServerItem() {
        return sourceServerItem;
    }

    @Override
    public int getItemID() {
        return itemID;
    }

    @Override
    public String getTargetLocalItem() {
        return null;
    }

    @Override
    public int getVersionLocal() {
        return versionLocal;
    }

    public int getEncoding() {
        return encoding;
    }

    public long getVersionLocalDate() {
        return versionLocalDate;
    }

    @Override
    public byte[] getBaselineHashValue() {
        return baselineHashValue;
    }

    public long getBaselineFileLength() {
        return baselineFileLength;
    }

    @Override
    public byte[] getBaselineFileGUID() {
        return baselineFileGUID;
    }

    @Override
    public String getDownloadURL() {
        return downloadURL;
    }

    @Override
    public void setDownloadURL(final String value) {
        downloadURL = value;
    }

    @Override
    public String getPendingChangeTargetServerItem() {
        return pendingChangeTargetServerItem;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isFullyPopulated(final boolean requireVersionLocalDate) {
        return null != baselineHashValue
            && -1 != baselineFileLength
            && (!requireVersionLocalDate || -1 != versionLocalDate);
    }

    @Override
    public void updateFrom(final Item item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFrom(final WorkspaceLocalItem lvExisting) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateFrom(final PendingChange pendingChange) {
        baselineFileLength = pendingChange.getLength();
        baselineHashValue = pendingChange.getHashValue();
    }

    public void generateNewBaselineFileGuid() {
        baselineFileGUID = GUID.newGUID().getGUIDBytes();
    }

    private final String sourceServerItem;
    private final int itemID;
    private final int versionLocal;
    private final int encoding;

    private long versionLocalDate = -1;
    private byte[] baselineHashValue = null;
    private long baselineFileLength = -1;
    private byte[] baselineFileGUID = null;
    private String pendingChangeTargetServerItem = null;
    private String downloadURL = null;
}
