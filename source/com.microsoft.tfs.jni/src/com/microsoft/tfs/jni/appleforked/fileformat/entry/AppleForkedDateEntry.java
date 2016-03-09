// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.fileformat.entry;

import java.text.MessageFormat;
import java.util.Date;

import com.microsoft.tfs.util.ByteArrayUtils;
import com.microsoft.tfs.util.Check;

/**
 * The Date entry in an AppleSingle / AppleDouble (version 2.0) document.
 */
public class AppleForkedDateEntry {
    public static final int DATE_ENTRY_SIZE = 16;

    private Date creationDate;
    private Date modificationDate;
    private Date backupDate;
    private Date accessDate;

    public AppleForkedDateEntry() {
    }

    public AppleForkedDateEntry(
        final Date creationDate,
        final Date modificationDate,
        final Date backupDate,
        final Date accessDate) {
        setCreationDate(creationDate);
        setModificationDate(modificationDate);
        setBackupDate(backupDate);
        setAccessDate(accessDate);
    }

    public AppleForkedDateEntry(final byte[] dateEntry) {
        decode(dateEntry);
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(final Date modificationDate) {
        this.modificationDate = modificationDate;
    }

    public Date getBackupDate() {
        return backupDate;
    }

    public void setBackupDate(final Date backupDate) {
        this.backupDate = backupDate;
    }

    public Date getAccessDate() {
        return accessDate;
    }

    public void setAccessDate(final Date accessDate) {
        this.accessDate = accessDate;
    }

    public void decode(final byte[] dateEntry) {
        Check.notNull(dateEntry, "dateEntry"); //$NON-NLS-1$
        Check.isTrue(dateEntry.length == DATE_ENTRY_SIZE, "dateEntry.length == DATE_ENTRY_SIZE"); //$NON-NLS-1$

        final long javaCreationTime = getJavaTime(ByteArrayUtils.getUnsignedInt32(dateEntry, 0));
        final long javaModificationTime = getJavaTime(ByteArrayUtils.getUnsignedInt32(dateEntry, 4));
        final long javaBackupTime = getJavaTime(ByteArrayUtils.getUnsignedInt32(dateEntry, 8));
        final long javaAccessTime = getJavaTime(ByteArrayUtils.getUnsignedInt32(dateEntry, 12));

        creationDate = new Date(javaCreationTime);
        modificationDate = new Date(javaModificationTime);
        backupDate = new Date(javaBackupTime);
        accessDate = new Date(javaAccessTime);
    }

    public byte[] encode() {
        final byte[] dateEntry = new byte[DATE_ENTRY_SIZE];

        final int appleCreationTime = creationDate != null ? getAppleTime(creationDate.getTime()) : 0;
        final int appleModificationTime = modificationDate != null ? getAppleTime(modificationDate.getTime()) : 0;
        final int appleBackupTime = backupDate != null ? getAppleTime(backupDate.getTime()) : 0;
        final int appleAccessTime = accessDate != null ? getAppleTime(accessDate.getTime()) : 0;

        ByteArrayUtils.putInt32(dateEntry, 0, appleCreationTime);
        ByteArrayUtils.putInt32(dateEntry, 4, appleModificationTime);
        ByteArrayUtils.putInt32(dateEntry, 8, appleBackupTime);
        ByteArrayUtils.putInt32(dateEntry, 12, appleAccessTime);

        return dateEntry;
    }

    private final int getAppleTime(final long javaTime) {
        /* Get number of seconds since Unix epoch */
        final long unixTime = javaTime / 1000;

        /* Return number of seconds since Mac epoch (1/1/2000) */
        return (int) (unixTime - 946684800);
    }

    private final long getJavaTime(final long appleTime) {
        /* Get number of seconds since Unix epoch */
        final long unixTime = appleTime + 946684800;

        /* Return millis for Java time */
        return (unixTime * 1000);
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "creation={0}, modification={1}, backup={2}, access={3}", //$NON-NLS-1$
            creationDate,
            modificationDate,
            backupDate,
            accessDate);
    }
}
