// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.localworkspace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;

public abstract class LocalMetadataTable implements Closeable {
    private static final Log log = LogFactory.getLog(LocalMetadataTable.class);

    // Authoritative slot
    protected static final String FILE_EXTENSION_SLOT_ONE = ".tf1"; //$NON-NLS-1$

    // On-deck slot -- about to become authoritative when it is moved to slot 1
    protected static final String FILE_EXTENSION_SLOT_TWO = ".tf2"; //$NON-NLS-1$

    // Slot for data that is being written.
    protected static final String FILE_EXTENSION_SLOT_THREE = ".tf3"; //$NON-NLS-1$

    private boolean dirty;
    private boolean aborted;
    private boolean eligibleForCachedLoad;

    private final String filename;
    private FileSystemAttributes savedAttributes;

    private LocalMetadataTableLock tableLock;

    public LocalMetadataTable(final String fileName) throws IOException {
        this(fileName, null, 7);
    }

    public LocalMetadataTable(final String fileName, final LocalMetadataTable cachedLoadSource) throws IOException {
        this(fileName, cachedLoadSource, 7);
    }

    public LocalMetadataTable(final String filename, final LocalMetadataTable cachedLoadSource, final int retryCount)
        throws IOException {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        this.filename = filename;

        setDirty(false);
        setAborted(false);

        try {
            tableLock = new LocalMetadataTableLock(filename, retryCount, false);

            recover();

            initialize();

            log.debug(MessageFormat.format("Loading {0}", this.getClass().getCanonicalName())); //$NON-NLS-1$

            final long start = System.currentTimeMillis();

            if (!tryCachedLoad(cachedLoadSource)) {
                FileInputStream is = null;
                BufferedInputStream bis = null;

                try {
                    is = getInputStream();
                    if (is != null) {
                        bis = new BufferedInputStream(is);
                        load(bis);
                    }
                } finally {
                    if (bis != null) {
                        bis.close();

                    }
                    if (is != null) {
                        is.close();
                    }
                }
            }

            log.debug(MessageFormat.format(
                "Total time for load of {0} was {1} ms", //$NON-NLS-1$
                this.getClass().getName(),
                (System.currentTimeMillis() - start)));
        } catch (final Exception e) {
            close(false);
            throw new VersionControlException(e);
        }
    }

    @Override
    public void close() throws IOException {
        close(true);
    }

    protected void close(final boolean disposing) throws IOException {
        try {
            if (disposing) {
                if (tableLock != null) {
                    if (isDirty() && !isAborted()) {
                        log.debug(MessageFormat.format("Saving {0}", this.getClass().getName())); //$NON-NLS-1$

                        final long start = System.currentTimeMillis();
                        final boolean keepFile = true;

                        FileOutputStream os = null;
                        BufferedOutputStream bos = null;

                        try {
                            os = getOutputStream();
                            if (os != null) {
                                bos = new BufferedOutputStream(os);
                                save(bos);
                            }
                        } finally {
                            try {
                                if (bos != null) {
                                    bos.close();
                                }
                                if (os != null) {
                                    os.close();
                                }
                            } catch (final IOException e) {
                                log.error(
                                    MessageFormat.format("Could not close {0}", this.getClass().getCanonicalName()), //$NON-NLS-1$
                                    e);
                            }
                        }

                        // Move the written file from slot 3 to slot 2 to slot
                        // 1.
                        positionFile(keepFile);

                        log.debug(MessageFormat.format(
                            "Total time for save of {0} was {1} ms", //$NON-NLS-1$
                            this.getClass().getName(),
                            (System.currentTimeMillis() - start)));

                        setDirty(false);

                        saveComplete();

                        makeEligibleForCachedLoad();
                    } else if (!isDirty()) {
                        makeEligibleForCachedLoad();
                    }
                }
            }
        } finally {
            if (tableLock != null) {
                tableLock.close();
            }
        }
    }

    private void makeEligibleForCachedLoad() {
        // Capture the file size and timestamp for slot 1.
        savedAttributes = FileSystemUtils.getInstance().getAttributes(getSlotOnePath(filename));

        setEligibleForCachedLoad(true);
    }

    protected abstract void load(InputStream is) throws Exception;

    protected abstract boolean save(OutputStream os) throws IOException;

    protected void initialize() {
        // Called during the constructor, before the table is loaded.
    }

    protected void saveComplete() {
        // Called during Dispose(), after the table is successfully saved.
    }

    protected boolean cachedLoad(final LocalMetadataTable source) {
        return false;
    }

    private boolean tryCachedLoad(LocalMetadataTable source) {
        if (source != null) {
            /*
             * While we are constructing a new LocalMetadataTable instance here,
             * the implementation of CachedLoad in the subclass may decide to
             * shallow copy the cached instance. If it mutates those structures
             * and then the transaction is aborted, it may corrupt the cache.
             */
            source.setEligibleForCachedLoad(false);

            final FileSystemAttributes attrs = FileSystemUtils.getInstance().getAttributes(getSlotOnePath(filename));
            final FileSystemAttributes sourceSavedAttributes = source.getSavedAttributes();

            Check.notNull(attrs, "attrs"); //$NON-NLS-1$
            Check.notNull(sourceSavedAttributes, "sourceSavedAttributes"); //$NON-NLS-1$

            if (!attrs.exists()) {
                // Assume empty table if the file does not exist
                source = null;
                return cachedLoad(source);
            } else if (attrs.getSize() == sourceSavedAttributes.getSize()
                && attrs.getModificationTime().equals(sourceSavedAttributes.getModificationTime())) {
                // Load from cache if the file has not changed on disk
                return cachedLoad(source);
            }
        }

        return false;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(final boolean dirty) {
        Check.isTrue(isAborted() == false, "We should not be modifying dirty state if we are aborted"); //$NON-NLS-1$

        this.dirty = dirty;
    }

    /**
     * If true, all changes made to this file will be ignored.
     *
     */
    public boolean isAborted() {
        return aborted;
    }

    public void setAborted(final boolean aborted) {
        this.aborted = aborted;
    }

    /**
     * If true, the owning transaction may cache this table for a future cached
     * load.
     */
    public boolean isEligibleForCachedLoad() {
        return eligibleForCachedLoad;
    }

    public void setEligibleForCachedLoad(final boolean eligibleForCachedLoad) {
        this.eligibleForCachedLoad = eligibleForCachedLoad;
    }

    /**
     * Recover from an abandoned mutex.
     */
    private void recover() {
        final String slotOneFilename = getSlotOnePath(filename);
        final String slotTwoFilename = getSlotTwoPath(filename);

        final File slotTwoFile = new File(slotTwoFilename);

        if (slotTwoFile.exists()) {
            final File slotOneFile = new File(slotOneFilename);

            if (slotOneFile.exists()) {
                // A file in slot 1 is always preferred.
                // Delete whatever is in slot 2 because we now have the mutex
                // and want to clean up anything abandoned in slot two.
                slotTwoFile.delete();
            } else {
                // No file in slot 1? If a file is in slot 2, move it
                // to slot 1 and use it.
                renameFile(slotTwoFile, slotOneFile);
            }
        }
    }

    /**
     * Move the written file in slot 3 to slot 2, then to slot 1.
     */
    private void positionFile(final boolean keepFile) {
        final String slotOneFilename = getSlotOnePath(filename);
        final String slotTwoFilename = getSlotTwoPath(filename);
        final String slotThreeFilename = getSlotThreePath(filename);

        final File slotOneFile = new File(slotOneFilename);
        final File slotTwoFile = new File(slotTwoFilename);
        final File slotThreeFile = new File(slotThreeFilename);

        /* Move slot three file into slot two */
        slotTwoFile.delete();

        if (!slotThreeFile.renameTo(slotTwoFile)) {
            throw new RuntimeException(
                MessageFormat.format("Could not rename {0} to {1}", slotThreeFilename, slotTwoFilename)); //$NON-NLS-1$
        }

        /* Move slot two file into slot one */
        slotOneFile.delete();
        renameFile(slotTwoFile, slotOneFile);
    }

    private FileInputStream getInputStream() {
        /*
         * Note: the Visual Studio implementation will create the parent folder
         * if this file does not exist. We do not do this when trying to read
         * the file.
         */
        try {
            return new FileInputStream(new File(getSlotOnePath(filename)));
        } catch (final FileNotFoundException e) {
            return null;
        }
    }

    private FileOutputStream getOutputStream() {
        final File slotThreeFile = new File(getSlotThreePath(filename));

        try {
            if (!slotThreeFile.exists() && slotThreeFile.getParent() != null) {
                FileHelpers.createDirectoryIfNecessary(slotThreeFile.getParent());
            }

            return new FileOutputStream(slotThreeFile);
        } catch (final Exception e) {
            log.warn(MessageFormat.format("Could not open file {0} for writing", slotThreeFile.getAbsolutePath()), e); //$NON-NLS-1$
            return null;
        }
    }

    public static String getSlotOnePath(final String filename) {
        return filename + FILE_EXTENSION_SLOT_ONE;
    }

    public static String getSlotTwoPath(final String filename) {
        return filename + FILE_EXTENSION_SLOT_TWO;
    }

    public static String getSlotThreePath(final String filename) {
        return filename + FILE_EXTENSION_SLOT_THREE;
    }

    public FileSystemAttributes getSavedAttributes() {
        return savedAttributes;
    }

    protected String getFilename() {
        return filename;
    }

    /**
     * Helper method to avoid a known Java File.renameTo issue on Windows.
     * File.renameTo appears to intermittently fail on Windows. We retry a few
     * times after sleeping briefly. A day long experiment shows that in a
     * scenario where renameTo is known to have intermittent failures, that we
     * retry at most once, at which point it succeeds. The scenario where rename
     * has failed is when renaming the slot 2 file to slot 1 in the positionFile
     * method (note, we've never seen rename fail in the recover methods which
     * also does a rename of slot 2 to slot 1).
     *
     * Interesting notes to consider - we've never seen renameTo fail when
     * renaming slot 3 to slot 2. The slot 2 file is never opened for read or
     * write. The slot 1 file is only ever opened for READ. The slot 3 file is
     * only ever opened for WRITE. SEE TEE.TFSPREVIEW BUG 4184 for more info.
     *
     *
     * @param source
     * @param destination
     */
    private void renameFile(final File source, final File destination) {
        for (int i = 0; i < 5; i++) {
            if (source.renameTo(destination)) {
                return;
            }

            try {
                log.info("RETRY rename [" + i + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                Thread.sleep(100);
            } catch (final InterruptedException e) {
            }
        }

        throw new RuntimeException(
            MessageFormat.format("Could not rename {0} to {1} in recover", source.getName(), destination.getName())); //$NON-NLS-1$
    }
}
