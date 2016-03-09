// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.launcher;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.dialogs.generic.MacMountBusyDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.util.process.ProcessFinishedHandler;
import com.microsoft.tfs.util.process.ProcessRunner;

public class MacUNCLauncher {
    public static boolean launch(final String uncPath) {
        String server, volume, path;
        final String serverAndVolumeAndPath = uncPath.substring(2);

        final int volumeIndex = serverAndVolumeAndPath.indexOf("\\"); //$NON-NLS-1$

        // we're passed a server and at least a volume
        // (ie \\server\volume or \\server\volume\file.txt)
        if (volumeIndex > 0) {
            server = serverAndVolumeAndPath.substring(0, volumeIndex);
            final String volumeAndPath = serverAndVolumeAndPath.substring(volumeIndex + 1);

            final int pathIndex = volumeAndPath.indexOf("\\"); //$NON-NLS-1$

            // we're passed a file (ie \\server\volume\file.txt)
            if (pathIndex > 0) {
                volume = volumeAndPath.substring(0, pathIndex);
                path = volumeAndPath.substring(pathIndex + 1);
            }
            // we're just passed a volume (ie \\server\volume)
            else {
                volume = volumeAndPath;
                path = ""; //$NON-NLS-1$
            }
        }
        // if we're just passed a server (ie \\server)
        else {
            server = serverAndVolumeAndPath;
            volume = ""; //$NON-NLS-1$
            path = ""; //$NON-NLS-1$
        }

        return launch(server, volume, path);
    }

    private static boolean launch(final String server, final String volume, final String path) {
        String localMountPoint;

        if ((localMountPoint = getMountPoint(server, volume)) == null) {
            // try to mount the smb path, then try to get the
            // local path (mountpoint)
            if (!mountSMB(server, volume) || (localMountPoint = getMountPoint(server, volume)) == null) {
                return false;
            }
        }

        return Program.launch(localMountPoint + "/" + path.replace('\\', '/')); //$NON-NLS-1$
    }

    private static boolean mountSMB(final String server, final String volume) {
        final String serverSmbUri = "smb://" + server + "/" + volume; //$NON-NLS-1$ //$NON-NLS-2$

        final SMBMounter smbMounter = new SMBMounter(serverSmbUri);
        return smbMounter.run();
    }

    private static String getMountPoint(final String server, final String volume) {
        final OutputStream runnerOutputStream = new ByteArrayOutputStream();

        final MountPointHandler mountPointHandler = new MountPointHandler(server, volume, runnerOutputStream);

        final ProcessRunner runner = new ProcessRunner(new String[] {
            "/sbin/mount" //$NON-NLS-1$
        }, null, null, mountPointHandler, runnerOutputStream, null);

        runner.run();

        if (mountPointHandler.getLocalMountPoint() != null) {
            return mountPointHandler.getLocalMountPoint();
        }

        return null;
    }

    private static class SMBMounter implements ProcessFinishedHandler {
        private final String path;
        private final MacMountBusyDialog busyDialog;

        private boolean running = true;
        private boolean success;

        public SMBMounter(final String path) {
            this.path = path;

            busyDialog = new MacMountBusyDialog(Display.getCurrent().getActiveShell(), path);
        }

        public boolean run() {
            final ProcessRunner mountRunner = new ProcessRunner(new String[] {
                "/usr/bin/osascript", //$NON-NLS-1$
                "-e", //$NON-NLS-1$
                "tell application \"Finder\"", //$NON-NLS-1$
                "-e", //$NON-NLS-1$
                "try", //$NON-NLS-1$
                "-e", //$NON-NLS-1$
                "mount volume \"" + path + "\"", //$NON-NLS-1$ //$NON-NLS-2$
                "-e", //$NON-NLS-1$
                "end try", //$NON-NLS-1$
                "-e", //$NON-NLS-1$
                "end tell" //$NON-NLS-1$
            }, null, null, this, null, null);

            ProcessRunner.runAsync(mountRunner);
            busyDialog.open();

            if (running) {
                running = false;
                mountRunner.interrupt();
            }

            return success;
        }

        @Override
        public void processCompleted(final ProcessRunner runner) {
            success = (runner.getExitCode() == 0);
            closeBusyDialog();
        }

        @Override
        public void processExecFailed(final ProcessRunner runner) {
            closeBusyDialog();
        }

        @Override
        public void processInterrupted(final ProcessRunner runner) {
            closeBusyDialog();
        }

        private void closeBusyDialog() {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    if (running) {
                        busyDialog.close();
                    }

                    running = false;
                }
            });
        }
    }

    static class MountPointHandler implements ProcessFinishedHandler {
        /*
         * Mac OS will display windows mountpoints as one of:
         *
         * //DOMAIN;USER@SERVER/VOLUME/
         *
         * //user@server/volume/
         *
         * //server/volume/
         */
        private final static Pattern smbPattern = Pattern.compile("^//(?:(?:([^/;]+);)?([^/@]+)@)?([^/]+)/(.*)"); //$NON-NLS-1$

        private final OutputStream outputStream;
        private final String server;
        private final String volume;

        private String localMountPoint;

        public MountPointHandler(final String server, final String volume, final OutputStream outputStream) {
            this.server = server;
            this.volume = volume;
            this.outputStream = outputStream;
        }

        @Override
        public void processExecFailed(final ProcessRunner runner) {
        }

        @Override
        public void processInterrupted(final ProcessRunner runner) {
        }

        @Override
        public void processCompleted(final ProcessRunner runner) {
            if (runner.getExitCode() == 0) {
                final String[] mountPoints = outputStream.toString().split("\n"); //$NON-NLS-1$

                for (int i = 0; i < mountPoints.length; i++) {
                    final String localMountPoint = getLocalMountPoint(server, volume, mountPoints[i]);

                    if (localMountPoint != null) {
                        this.localMountPoint = localMountPoint;
                        break;
                    }
                }
            }
        }

        static String getLocalMountPoint(final String server, final String volume, final String mountLine) {
            final String[] mountData = mountLine.split(" "); //$NON-NLS-1$

            if (mountData.length < 3) {
                return null;
            }

            final Matcher matcher = smbPattern.matcher(mountData[0]);

            if (matcher.find() && matcher.groupCount() == 4) {
                if (server.equalsIgnoreCase(matcher.group(3)) && volume.equalsIgnoreCase(matcher.group(4))) {
                    return mountData[2];
                }
            }

            return null;
        }

        public String getLocalMountPoint() {
            return localMountPoint;
        }
    }
}
