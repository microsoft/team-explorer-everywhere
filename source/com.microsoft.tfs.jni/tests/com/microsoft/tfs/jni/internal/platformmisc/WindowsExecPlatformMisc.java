// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.platformmisc;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.ExecHelpers;
import com.microsoft.tfs.jni.PlatformMisc;
import com.microsoft.tfs.util.Check;

/**
 * An implementation of the {@link PlatformMisc} interface via external Unix
 * process execution.
 */
public class WindowsExecPlatformMisc implements PlatformMisc {
    private final static Log log = LogFactory.getLog(WindowsExecPlatformMisc.class);

    @Override
    public int getDefaultCodePage() {
        return -1;
    }

    @Override
    public String getHomeDirectory(final String username) {
        /*
         * I don't think we can do this. Just return the username as the path.
         */
        return null;
    }

    @Override
    public String getComputerName() {
        return getEnvironmentVariable("COMPUTERNAME"); //$NON-NLS-1$
    }

    @Override
    public String getEnvironmentVariable(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$

        /*
         * Windows conventions surround variables with percent signs for shell
         * expansion.
         */
        final String variableName = "%" + name + "%"; //$NON-NLS-1$ //$NON-NLS-2$

        final String[] args = new String[] {
            "cmd", //$NON-NLS-1$
            "/c", //$NON-NLS-1$
            "echo", //$NON-NLS-1$
            variableName
        };

        final StringBuffer output = new StringBuffer();
        final int ret = ExecHelpers.exec(args, output);

        if (ret != 0) {
            log.error(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
            return null;
        }

        final String value = output.toString().trim();

        /*
         * If the name we got is empty, or exactly matched the variable we tried
         * to expand, it was not set.
         */
        if (value.length() == 0 || value.equalsIgnoreCase(variableName)) {
            return null;
        }

        return value;
    }

    @Override
    public boolean changeCurrentDirectory(final String directory) {
        /*
         * Just can't do it.
         */
        return false;
    }

    @Override
    public String expandEnvironmentString(final String value) {
        /*
         * We don't need to call to the shell to test this.
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
