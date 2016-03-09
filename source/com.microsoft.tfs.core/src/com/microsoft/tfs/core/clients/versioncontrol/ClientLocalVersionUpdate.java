// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.WorkspaceLocalItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.LocalVersionUpdate;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.datetime.DotNETDate;

/**
 * <p>
 * Contains version information about local working folder files sent to the
 * server during and after a "get" operation. Supersedes
 * {@link LocalVersionUpdate} for use within the client SDK, though
 * {@link LocalVersionUpdate}s are still sent to the server.
 * </p>
 * <p>
 * Clients must call {@link #close()} to release resources associated with this
 * class. {@link UpdateLocalVersionQueue} handles closing objects passed to it,
 * so users of that class do not need to do it.
 * </p>
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-11.0
 */
public final class ClientLocalVersionUpdate implements IPopulatableLocalVersionUpdate {
    private static final Log log = LogFactory.getLog(ClientLocalVersionUpdate.class);

    private static final byte[] ZERO_LENGTH_BYTE_ARRAY = new byte[0];

    private final String sourceServerItem;
    private final int itemID;
    private final String targetLocalItem;
    private final int versionLocal;

    private long versionLocalDate = -1;
    private int encoding = 0;
    private byte[] baselineHashValue = null;
    private long baselineFileLength = -1;
    private byte[] baselineFileGuid = null;
    private boolean keepLocalVersionEntryOnDelete = false;

    // Used if the caller wants to force a particular last modified date into
    // the table rather than having UpdateLocalVersion lift the latest value
    // from disk.
    private long lastModifiedDate = -1;

    private String pendingChangeTargetServerItem = null;
    private String downloadURL = null;

    private PropertyValue[] properties;

    public ClientLocalVersionUpdate(
        final String sourceServerItem,
        final int itemID,
        final String targetLocalItem,
        final int versionLocal,
        final PropertyValue[] properties) {
        // -1 is a valid value for versionLocal -- it maps to 'committed'
        Check.isTrue(versionLocal >= -1, "versionLocal"); //$NON-NLS-1$

        this.sourceServerItem = sourceServerItem;
        this.itemID = itemID;
        this.targetLocalItem = targetLocalItem;
        this.versionLocal = versionLocal;
        this.properties = properties;
    }

    public ClientLocalVersionUpdate(
        final String sourceServerItem,
        final int itemID,
        final String targetLocalItem,
        final int versionLocal,
        final int encoding,
        final boolean keepLocalVersionRowOnDelete,
        final PropertyValue[] properties) {
        this(sourceServerItem, itemID, targetLocalItem, versionLocal, properties);

        this.encoding = encoding;
        this.keepLocalVersionEntryOnDelete = keepLocalVersionRowOnDelete;
    }

    public ClientLocalVersionUpdate(
        final String sourceServerItem,
        final int itemID,
        final String targetLocalItem,
        final int versionLocal,
        final Calendar versionLocalDate,
        final int encoding,
        final byte[] baselineHashValue,
        final long baselineFileLength,
        final byte[] baselineFileGuid,
        final String pendingChangeTargetServerItem,
        final PropertyValue[] properties) {
        this(sourceServerItem, itemID, targetLocalItem, versionLocal, properties);

        // We must provide an encoding here if we'll be potentially updating
        // from a PendingChange later. The encoding returned on a PendingChange
        // object is the *pending* encoding and not useful for us.
        Check.isTrue(
            0 != encoding || null == pendingChangeTargetServerItem,
            "0 != encoding || null == pendingChangeTargetServerItem"); //$NON-NLS-1$

        this.versionLocalDate =
            DotNETDate.MIN_CALENDAR.equals(versionLocalDate) ? -1 : DotNETDate.toWindowsFileTimeUTC(versionLocalDate);
        this.encoding = encoding;
        this.baselineHashValue = baselineHashValue;
        this.baselineFileLength = baselineFileLength;
        this.baselineFileGuid = baselineFileGuid;
        this.pendingChangeTargetServerItem = pendingChangeTargetServerItem;
    }

    @Override
    public boolean isSendToServer() {
        return true;
    }

    @Override
    public boolean isCommitted() {
        return versionLocal != 0;
    }

    /**
     * Indicates whether this ClientLocalVersionUpdate has all the fields
     * populated that are necessary to call UpdateLocalVersion for a local
     * workspace. {@inheritDoc}
     */
    @Override
    public boolean isFullyPopulated(final boolean requireLocalVersionDate) {
        if (null == targetLocalItem) {
            return true;
        }

        if (0 == encoding || VersionControlConstants.ENCODING_UNCHANGED == encoding) {
            // We don't know the encoding for some reason; this update will need
            // to be updated before we can process it.
            return false;
        }

        if (VersionControlConstants.ENCODING_FOLDER == encoding
            || (0 == versionLocal && null == pendingChangeTargetServerItem)) {
            // We're working with a folder or a pending add; there's no need to
            // know any additional data like VersionLocalDate or any baseline
            // properties.
            return true;
        }

        if (0 != versionLocal && requireLocalVersionDate && -1 == versionLocalDate) {
            // If the caller passed requireLocalVersionDate = true, check to
            // make sure we have a value for VersionLocalDate. This is only
            // necessary for committed items.
            return false;
        }

        return null != baselineHashValue && 16 == baselineHashValue.length && -1 != baselineFileLength;
    }

    @Override
    public void updateFrom(final Item item) {
        Check.isTrue(0 != versionLocal, "Attempted to update a ClientLocalVersionUpdate for an uncommitted item"); //$NON-NLS-1$

        encoding = item.getEncoding().getCodePage();

        if (VersionControlConstants.ENCODING_FOLDER != encoding) {
            baselineFileLength = item.getContentLength();
            baselineHashValue = item.getContentHashValue();
            versionLocalDate = DotNETDate.MIN_CALENDAR.equals(item.getCheckinDate()) ? -1
                : DotNETDate.toWindowsFileTimeUTC(item.getCheckinDate());
        }
    }

    @Override
    public void updateFrom(final WorkspaceLocalItem lvExisting) {
        Check.isTrue(
            ServerPath.equals(sourceServerItem, lvExisting.getServerItem()) && versionLocal == lvExisting.getVersion(),
            "ServerPath.equals(sourceServerItem, lvExisting.getServerItem()) && versionLocal == lvExisting.getVersion()"); //$NON-NLS-1$

        if (0 == encoding || VersionControlConstants.ENCODING_UNCHANGED == encoding) {
            encoding = lvExisting.getEncoding();
        } else if (0 != versionLocal) {
            // Check that the encoding of this committed item has not changed.
            Check.isTrue(encoding == lvExisting.getEncoding(), "encoding == lvExisting.getEncoding()"); //$NON-NLS-1$
        }

        if (-1 == versionLocalDate) {
            versionLocalDate = lvExisting.getCheckinDate();
        }

        final boolean isSymlink = PropertyConstants.IS_SYMLINK.equals(
            PropertyUtils.selectMatching(lvExisting.getPropertyValues(), PropertyConstants.SYMBOLIC_KEY))
            || PropertyConstants.IS_SYMLINK.equals(
                PropertyUtils.selectMatching(properties, PropertyConstants.SYMBOLIC_KEY));

        if (isSymlink) {
            baselineFileLength = 0;
        }

        if (-1 == baselineFileLength) {
            baselineFileLength = lvExisting.getLength();
        } else if (-1 != lvExisting.getLength() && !isSymlink && 0 != versionLocal) {
            // Check that the baseline length of this committed item has not
            // changed.
            if (baselineFileLength != lvExisting.getLength()) {
                // log an error in addition to throwing exception, below. Do
                // this in English only
                final String logMessage =
                    MessageFormat.format(
                        "The server file: {0} is out of sync with the local version: {1}", //$NON-NLS-1$
                        getSourceServerItem(),
                        getTargetLocalItem());
                log.error(logMessage);

                // building the message only after the check fails to avoid
                // unnecessary string building
                final String errorMessage =
                    MessageFormat.format(
                        Messages.getString("ClientLocalVersionUpdate.ErrorLengthOutOfSyncFormat"), //$NON-NLS-1$
                        getSourceServerItem(),
                        getTargetLocalItem());
                Check.isTrue(false, errorMessage);
            }
        }

        if (null == baselineHashValue || 16 != baselineHashValue.length) {
            baselineHashValue = lvExisting.getHashValue();
        } else if (lvExisting.hasHashValue() && 0 != versionLocal) {
            // Check that the baseline hash value of this committed item has not
            // changed.
            Check.isTrue(
                Arrays.equals(baselineHashValue, lvExisting.getHashValue()),
                "Arrays.equals(baselineHashValue, lvExisting.getHashValue())"); //$NON-NLS-1$
        }

        if (null == baselineFileGuid || 16 != baselineFileGuid.length) {
            baselineFileGuid = lvExisting.getBaselineFileGUID();
        }
    }

    @Override
    public void updateFrom(final PendingChange pendingChange) {
        Check.isTrue(versionLocal == pendingChange.getVersion(), "versionLocal == pendingChange.getVersion()"); //$NON-NLS-1$

        // We can't update our encoding from a PendingChange object since it's
        // the pending encoding.
        Check.isTrue(0 != encoding, "0 != encoding"); //$NON-NLS-1$

        // We don't get the checkin date here from the PendingChange object,
        // but as of right now, the only place queueing updates with a value
        // for PendingChangeTargetServerItem always provides a value for
        // LocalVersionDate. The caller of this method should ensure that
        // they re-check IsFullyPopulated after calling UpdateFrom.

        baselineFileLength = pendingChange.getLength();
        baselineHashValue = pendingChange.getHashValue();
    }

    public void generateNewBaselineFileGUID() {
        Check.isTrue(
            null == baselineFileGuid
                && VersionControlConstants.ENCODING_FOLDER != encoding
                && (0 != versionLocal || null != downloadURL),
            "null == baselineFileGuid && VersionControlConstants.ENCODING_FOLDER != encoding && (0 != versionLocal || null != downloadURL)"); //$NON-NLS-1$

        baselineFileGuid = GUID.newGUID().getGUIDBytes();
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
        return targetLocalItem;
    }

    @Override
    public int getVersionLocal() {
        return versionLocal;
    }

    public long getVersionLocalDate() {
        return versionLocalDate;
    }

    public int getEncoding() {
        return encoding;
    }

    @Override
    public byte[] getBaselineHashValue() {
        if (null == baselineHashValue || 16 != baselineHashValue.length) {
            return ZERO_LENGTH_BYTE_ARRAY;
        }
        return baselineHashValue;
    }

    public long getBaselineFileLength() {
        return baselineFileLength;
    }

    @Override
    public byte[] getBaselineFileGUID() {
        return baselineFileGuid;
    }

    public boolean getKeepLocalVersionEntryOnDelete() {
        return keepLocalVersionEntryOnDelete;
    }

    @Override
    public String getPendingChangeTargetServerItem() {
        return pendingChangeTargetServerItem;
    }

    @Override
    public String getDownloadURL() {
        return downloadURL;
    }

    @Override
    public void setDownloadURL(final String value) {
        downloadURL = value;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(final long value) {
        lastModifiedDate = value;
    }

    public PropertyValue[] getPropertyValues() {
        return properties;
    }

    public void setPropertyValues(final PropertyValue[] properties) {
        this.properties = properties;
    }
}