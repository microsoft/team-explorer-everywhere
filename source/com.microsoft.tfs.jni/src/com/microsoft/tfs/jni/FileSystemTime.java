// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.text.MessageFormat;

import com.microsoft.tfs.util.Check;

/**
 * A representation of filesystem time, suitable for last modification dates. Is
 * at most nanosecond precise, however actual precision depends on the
 * underlying operating system and filesystem. Is expected to be treated as an
 * opaque time object for comparison only.
 *
 * @threadsafety immutable
 */
public class FileSystemTime implements Comparable<FileSystemTime> {
    private final long seconds;
    private final long nanoseconds;

    /**
     * Creates a new {@link FileSystemTime}.
     *
     * @param seconds
     *        The number of seconds since Jan 1, 1970 00:00:00 UTC.
     */
    public FileSystemTime(final long seconds) {
        this.seconds = seconds;
        this.nanoseconds = 0;
    }

    /**
     * Creates a new {@link FileSystemTime}.
     *
     * @param seconds
     *        The number of whole seconds since Jan 1, 1970 00:00:00 UTC.
     * @param nanoseconds
     *        The number of whole nanoseconds.
     */
    public FileSystemTime(final long seconds, final long nanoseconds) {
        this.seconds = seconds;
        this.nanoseconds = nanoseconds;
    }

    /**
     * Returns the time in "java time", that is (the number of milliseconds
     * since Jan 1 1970.)
     *
     * @return the time in Java time format
     */
    public long getJavaTime() {
        return (seconds * 1000 + (nanoseconds / 1000));
    }

    /**
     * Returns the time in "Windows file system time format." (Number of
     * 100-nanosecond "ticks" since Jan 1 1601.) Note that this should be
     * treated as a 64 bit object, not an actual long. That is to say that it
     * should be treated as an unsigned.
     *
     * @return the time in Windows file system time format
     */
    public long getWindowsFilesystemTime() {
        return (nanoseconds + ((seconds + 11644473600L) * 10000000));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (object == null || !(object instanceof FileSystemTime)) {
            return false;
        }

        if (object == this) {
            return true;
        }

        final FileSystemTime other = (FileSystemTime) object;

        return (this.seconds == other.seconds && this.nanoseconds == other.nanoseconds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final FileSystemTime other) {
        Check.notNull(other, "other"); //$NON-NLS-1$

        if (this.seconds < other.seconds) {
            return -1;
        } else if (this.seconds > other.seconds) {
            return 1;
        }

        if (this.nanoseconds < other.nanoseconds) {
            return -1;
        } else if (this.nanoseconds > other.nanoseconds) {
            return 1;
        }

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = 17;

        /* Note that the xor the two halves gives Long's hashcode */
        hashCode = 31 * hashCode + (int) (seconds ^ (seconds >>> 32));
        hashCode = 31 * hashCode + (int) (nanoseconds ^ (nanoseconds >>> 32));

        return hashCode;
    }

    @Override
    public String toString() {
        return MessageFormat.format("FileSystemTime [seconds={0}, nanoseconds={1}]", seconds, nanoseconds); //$NON-NLS-1$
    }
}
