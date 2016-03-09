// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.launcher;

import java.io.File;

import org.eclipse.swt.program.Program;

import com.microsoft.tfs.util.Platform;

/**
 * A utility class containing a static method for launching external file,
 * folders, and URLs.
 *
 * Use this class instead of org.eclipse.swt.program.Program. This class
 * contains workarounds for some bugs in that class.
 */
public class Launcher {
    /**
     * Launch a file, folder, or URL.
     *
     * @param toLaunch
     *        the item to be launched - either an absolute path to a local file
     *        or folder, or a URL - do not pass null
     * @return true if the item is launched
     */
    public static boolean launch(String toLaunch) {
        if (toLaunch == null) {
            throw new IllegalArgumentException("launch string must not be null"); //$NON-NLS-1$
        }

        /*
         * Need to do a check here to work around a bug in Program.launch(). If
         * we're on a unix platform, and the item begins with a '/', append a
         * '/.', which will not change the path, but will ensure that
         * Program.launch() works correctly. Program.launch() doesn't operate
         * right if the path does not contain a '.'.
         *
         * Of course, Program.launch() on OS X can't handle /./path because
         * /usr/bin/open on OS X is janky.
         */
        if (File.separatorChar == '/'
            && toLaunch.charAt(0) == '/'
            && System.getProperty("os.name").startsWith("Mac OS X") == false) //$NON-NLS-1$ //$NON-NLS-2$
        {
            toLaunch = "/." + toLaunch; //$NON-NLS-1$
        }

        // handle SMB-style paths to launching
        if (toLaunch.startsWith("\\\\")) //$NON-NLS-1$
        {
            return launchUNC(toLaunch);
        } else {
            return Program.launch(toLaunch);
        }
    }

    private static boolean launchUNC(final String toLaunch) {
        // on windows, simply add double quotes around the path(?)
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return Program.launch("\"" + toLaunch + "\""); //$NON-NLS-1$ //$NON-NLS-2$
        } else if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            return MacUNCLauncher.launch(toLaunch);
        } else {
            return Program.launch("smb:" + toLaunch.replaceAll("\\\\", "/")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
}
