// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.locking;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.TestCase;

public class AdvisoryFileLockTest extends TestCase {
    private static class Scoreboard {
        private final ArrayList scores = new ArrayList();

        public Scoreboard() {
        }

        public synchronized void report(final int score) {
            this.scores.add(new Integer(score));
        }

        public synchronized void validate() throws IllegalStateException {
            int lastScore = Integer.MIN_VALUE;

            for (int i = 0; i < this.scores.size(); i++) {
                final int score = ((Integer) this.scores.get(i)).intValue();

                if (score <= lastScore) {
                    final StringBuffer sb = new StringBuffer("Scoreboard out of order:"); //$NON-NLS-1$
                    for (int j = 0; j < scores.size(); j++) {
                        sb.append(" " + scores.get(j)); //$NON-NLS-1$
                    }
                    throw new IllegalStateException(sb.toString());
                }

                lastScore = score;
            }
        }
    }

    public void testFileConstruction() throws IOException, InterruptedException {
        // File exists
        File file = File.createTempFile("settingsLockTest-", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        AdvisoryFileLock lock = AdvisoryFileLock.create(file, false);
        assertNotNull(lock);
        lock.release();

        // File does not exist
        file = File.createTempFile("settingsLockTest-", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        file.delete();
        lock = AdvisoryFileLock.create(file, false);
        assertNotNull(lock);
        lock.release();

        // Null file
        try {
            AdvisoryFileLock.create((File) null, false);
            assertTrue("should have thrown for null file", false); //$NON-NLS-1$
        } catch (final Exception e) {
        }
    }

    public void testDoubleRelease() throws IOException, InterruptedException {
        final File file = File.createTempFile("settingsLockTest-", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        final AdvisoryFileLock lock = AdvisoryFileLock.create(file, false);
        assertFalse("should not be released", lock.isReleased()); //$NON-NLS-1$

        lock.release();
        assertTrue("should be released", lock.isReleased()); //$NON-NLS-1$

        lock.release();
        assertTrue("should be released", lock.isReleased()); //$NON-NLS-1$
    }

    public void testNonBlockingAcquireFail() throws Exception {
        final File file = File.createTempFile("settingsLockTest-", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        final Scoreboard scores = new Scoreboard();

        final Runnable runA = new Runnable() {
            @Override
            public void run() {
                try {
                    scores.report(1);
                    final AdvisoryFileLock lockA = AdvisoryFileLock.create(file, false);
                    assertNotNull(lockA);
                    Thread.sleep(500);
                    scores.report(4);
                    lockA.release();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        };

        final Runnable runB = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    scores.report(2);
                    final AdvisoryFileLock lockB = AdvisoryFileLock.create(file, false);
                    assertNull(lockB);
                    scores.report(3);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        };

        final Thread threadA = new Thread(runA);
        final Thread threadB = new Thread(runB);

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        scores.validate();

        file.delete();
    }

    public void testNonBlockingAcquireSucceed() throws Exception {
        final File file = File.createTempFile("settingsLockTest-", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        final Scoreboard scores = new Scoreboard();

        final Runnable runA = new Runnable() {
            @Override
            public void run() {
                try {
                    scores.report(1);
                    final AdvisoryFileLock lockA = AdvisoryFileLock.create(file, false);
                    assertNotNull(lockA);
                    Thread.sleep(200);
                    scores.report(2);
                    lockA.release();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        };

        final Runnable runB = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(300);
                    scores.report(3);
                    final AdvisoryFileLock lockB = AdvisoryFileLock.create(file, false);
                    assertNotNull(lockB);
                    scores.report(4);
                    lockB.release();
                    scores.report(5);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        };

        final Thread threadA = new Thread(runA);
        final Thread threadB = new Thread(runB);

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        scores.validate();

        file.delete();
    }

    public void testBlockingAcquireOrder() throws Exception {
        final File file = File.createTempFile("settingsLockTest-", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        final Scoreboard scores = new Scoreboard();

        final Runnable runA = new Runnable() {
            @Override
            public void run() {
                try {
                    scores.report(1);
                    final AdvisoryFileLock lockA = AdvisoryFileLock.create(file, true);
                    scores.report(2);
                    Thread.sleep(500);
                    lockA.release();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        };

        final Runnable runB = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);
                    scores.report(3);
                    final AdvisoryFileLock lockB = AdvisoryFileLock.create(file, true);
                    scores.report(4);
                    lockB.release();
                    scores.report(5);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        };

        final Thread threadA = new Thread(runA);
        final Thread threadB = new Thread(runB);

        threadA.start();
        threadB.start();

        threadA.join();
        threadB.join();

        scores.validate();

        file.delete();
    }
}
