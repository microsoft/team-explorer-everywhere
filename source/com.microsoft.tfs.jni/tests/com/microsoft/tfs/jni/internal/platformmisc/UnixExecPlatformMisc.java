// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.platformmisc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.ExecHelpers;
import com.microsoft.tfs.jni.PlatformMisc;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * An implementation of the {@link PlatformMisc} interface via external Unix
 * process execution.
 */
public class UnixExecPlatformMisc implements PlatformMisc {
    private final static Log log = LogFactory.getLog(UnixExecPlatformMisc.class);

    @Override
    public int getDefaultCodePage() {
        return -1;
    }

    @Override
    public String getHomeDirectory(final String username) {
        Check.notNull(username, "username"); //$NON-NLS-1$

        /*
         * On Solaris /bin/sh is quite minimal, and tilde expansion does not
         * work. Force ksh instead.
         */
        String shell;
        if (Platform.isCurrentPlatform(Platform.SOLARIS)) {
            shell = "/bin/ksh"; //$NON-NLS-1$
        } else {
            shell = "/bin/sh"; //$NON-NLS-1$
        }

        /*
         * We depend on the user's shell to resolve "~user" to the user's real
         * home directory. If this fails in some cases (user is running an
         * oddball shell), we could explicitly invoke /bin/sh to do this work,
         * but in that case /bin/sh might also be an oddball and not work.
         */
        final String[] args = new String[] {
            shell,
            "-c", //$NON-NLS-1$
            "echo ~" + username //$NON-NLS-1$
        };

        final StringBuffer output = new StringBuffer();

        final int ret = ExecHelpers.exec(args, output);

        if (ret != 0) {
            log.error(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
        }

        final String outputString = output.toString().trim();

        /*
         * If we got the string back unchanged, it did not expand to a home
         * directory, so return null.
         */

        if (outputString.equals("~" + username)) //$NON-NLS-1$
        {
            return null;
        }

        return outputString;
    }

    @Override
    public String getComputerName() {
        /*
         * Run the "hostname" command. Don't use "-s" flag (short name) because
         * it's not supported on HPUX, Solaris, and elsewhere.
         */

        String[] args;

        if (Platform.isCurrentPlatform(Platform.Z_OS)) {
            /*
             * Unix is just one subsystem of z/OS, so there can be multiple host
             * names. The -g argument to hostname asks for the same result as
             * the Unix function "gethostbyname()" would return, which means
             * this matches the result of the native library.
             *
             * The other host name (from the MVS side, which "hostname" with no
             * arguments or "hostname -c" returns) may be "more correct", but it
             * doesn't match our native libraries, so we should use -g.
             */
            args = new String[] {
                "hostname", //$NON-NLS-1$
                "-g" //$NON-NLS-1$
            };
        } else if (Platform.isCurrentPlatform(Platform.HPUX) || Platform.isCurrentPlatform(Platform.SOLARIS)) {
            /*
             * HP-UX and Solaris return the short host name (no domain info)
             * without the use of the "-s" option (which isn't supported on
             * these platforms).
             */
            args = new String[] {
                "hostname" //$NON-NLS-1$
            };
        } else {
            /*
             * Linux and AIX return the short name if "-s" is not specified, but
             * they do support the "-s" option anyway, and Mac OS X needs the
             * "-s" option for short names (else we get FQDN).
             */
            args = new String[] {
                "hostname", //$NON-NLS-1$
                "-s" //$NON-NLS-1$
            };
        }

        final StringBuffer output = new StringBuffer();
        final int ret = ExecHelpers.exec(args, output);

        if (ret != 0) {
            log.error(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
            return null;
        }

        final String name = output.toString().trim();

        if (name.length() == 0) {
            return null;
        }

        return name;
    }

    @Override
    public String getEnvironmentVariable(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        /*
         * Unix doesn't support environment variables that begin with numbers.
         */
        if (Character.isDigit(name.charAt(0))) {
            return null;
        }

        final String[] args = new String[] {
            "/bin/sh", //$NON-NLS-1$
            "-c", //$NON-NLS-1$
            "echo \"$" + name + "\"" //$NON-NLS-1$ //$NON-NLS-2$
        };

        final StringBuffer output = new StringBuffer();

        final int ret = ExecHelpers.exec(args, output);

        if (ret != 0) {
            log.error(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
        }

        final String value = output.toString().trim();

        /*
         * If we got an empty string, the variable was not set, or was set to an
         * empty string.
         */

        if (value.length() == 0) {
            return null;
        }

        return value;
    }

    @Override
    public boolean changeCurrentDirectory(final String directory) {
        /*
         * Just can't do this.
         */
        return false;
    }

    @Override
    public String expandEnvironmentString(final String value) {
        /*
         * We don't need this on UNIX.
         */
        return value;
    }

    @Override
    public String getCurrentIdentityUser() {
        return null;
    }

    @Override
    public String getWellKnownSID(final int wellKnownSIDType, final String domainSIDString) {
        return null;
    }
}
