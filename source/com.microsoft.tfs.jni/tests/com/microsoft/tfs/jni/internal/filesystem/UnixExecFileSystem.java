// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.filesystem;

import java.io.File;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.ExecHelpers;
import com.microsoft.tfs.jni.FileSystem;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemTime;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.PlatformVersion;

/**
 * An implementation of the {@link FileSystem} interface via external Unix
 * process execution.
 */
public class UnixExecFileSystem implements FileSystem {
    private final static Log log = LogFactory.getLog(UnixExecFileSystem.class);

    @Override
    public FileSystemAttributes getAttributes(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        final File file = new File(filepath);

        /*
         * Using Java's File class is faster than spawning an external process.
         *
         * This method for querying writable status fails on Unix when running
         * as root. Many Java implementations return "true" for all files
         * regardless of permissions set on the file when root.
         *
         * Note that this does take the Mac immutable flag into account.
         */
        final long lastModifiedMillis = file.lastModified();

        return new FileSystemAttributes(
            file.exists(),
            new FileSystemTime(lastModifiedMillis / 1000, (lastModifiedMillis % 1000) * 1000000),
            file.length(),
            file.canWrite() == false,
            isOwnerOnly(filepath),
            isPublicWritable(filepath),
            false,
            false,
            file.isDirectory(),
            false,
            false,
            isExecutable(filepath),
            isSymbolicLink(filepath));
    }

    @Override
    public String getOwner(final String path) {
        return null;
    }

    @Override
    public void setOwner(final String path, final String owner) {
    }

    @Override
    public void grantInheritableFullControl(
        final String path,
        final String user,
        final String copyExplicitRulesFromPath) {
    }

    @Override
    public void copyExplicitDACLEntries(final String sourcePath, final String targetPath) {
    }

    @Override
    public void removeExplicitAllowEntries(final String path, final String user) {
    }

    private boolean isOwnerOnly(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        /*
         * Invoke "ls -la" and read the string back. Then we parse the mode
         * section (like "-rwxr-xr-x") in Java for the group and other flags.
         */
        final String[] args = new String[] {
            "ls", //$NON-NLS-1$
            "-la", //$NON-NLS-1$
            filepath
        };

        final StringBuffer output = new StringBuffer();

        final int ret = ExecHelpers.exec(args, output);

        /*
         * Use "debug" level for a non-zero exit code because ls will return
         * errors if files don't exist (which means we should return false for
         * this method).
         */
        if (ret != 0) {
            log.debug(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
        }

        String outputString = output.toString();

        /*
         * Strip off any leading whitespace (I know of no Unix that starts the
         * line with whitespace but it's good to be careful).
         */
        outputString = outputString.trim();

        /*
         * If a character at positions 4-9 are not '-' then the file (or
         * directory) has some group or other flag set and is thus not
         * "owner only".
         */
        if (outputString.length() < 10
            || outputString.charAt(4) != '-'
            || outputString.charAt(5) != '-'
            || outputString.charAt(6) != '-'
            || outputString.charAt(7) != '-'
            || outputString.charAt(8) != '-'
            || outputString.charAt(9) != '-') {
            return false;
        }

        return true;
    }

    private boolean isPublicWritable(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        /*
         * Invoke "ls -la" and read the string back. Then we parse the mode
         * section (like "-rwxr-xr-x") in Java for the group and other flags.
         */
        final String[] args = new String[] {
            "ls", //$NON-NLS-1$
            "-la", //$NON-NLS-1$
            filepath
        };

        final StringBuffer output = new StringBuffer();

        final int ret = ExecHelpers.exec(args, output);

        /*
         * Use "debug" level for a non-zero exit code because ls will return
         * errors if files don't exist (which means we should return false for
         * this method).
         */
        if (ret != 0) {
            log.debug(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
        }

        String outputString = output.toString();

        /*
         * Strip off any leading whitespace (I know of no Unix that starts the
         * line with whitespace but it's good to be careful).
         */
        outputString = outputString.trim();

        /*
         * If characters at position 5 or 8 are '-', then it is not group and
         * other writable and thus not "public writable".
         */
        if (outputString.length() >= 10 && (outputString.charAt(5) == '-' || outputString.charAt(8) == '-')) {
            return false;
        }

        return true;
    }

    private boolean isExecutable(final String filepath) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        /*
         * Invoke "ls -la" and read the string back. Then we parse the mode
         * section (like "-rwxr-xr-x") in Java for the correct execute flag.
         */
        final String[] args = new String[] {
            "ls", //$NON-NLS-1$
            "-la", //$NON-NLS-1$
            filepath
        };

        final StringBuffer output = new StringBuffer();

        final int ret = ExecHelpers.exec(args, output);

        /*
         * Use "debug" level for a non-zero exit code because ls will return
         * errors if files don't exist (which means we should return false for
         * this method).
         */
        if (ret != 0) {
            log.debug(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
        }

        String outputString = output.toString();

        /*
         * Strip off any leading whitespace (I know of no Unix that starts the
         * line with whitespace but it's good to be careful).
         */
        outputString = outputString.trim();

        /*
         * If the fourth character is "x" or "X", the file (or directory) is
         * executable.
         */
        if (outputString.length() > 3 && (outputString.charAt(3) == 'x' || outputString.charAt(3) == 'X')) {
            return true;
        }

        return false;
    }

    @Override
    public boolean setAttributes(final String filepath, final FileSystemAttributes attributes) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$
        Check.notNull(attributes, "attributes"); //$NON-NLS-1$

        /* Query the current attributes to discover what we need to modify. */
        final FileSystemAttributes oldAttributes = getAttributes(filepath);

        if (oldAttributes == null) {
            return false;
        }

        final boolean forceUpdate = (oldAttributes.isPublicWritable() != attributes.isPublicWritable());
        final boolean allUsers = (attributes.isPublicWritable() && !attributes.isOwnerOnly());

        if (oldAttributes.isReadOnly() != attributes.isReadOnly() || forceUpdate) {
            if (!setReadOnly(filepath, attributes.isReadOnly(), allUsers)) {
                return false;
            }
        }

        if (oldAttributes.isOwnerOnly() != attributes.isOwnerOnly() || forceUpdate) {
            if (setOwnerOnly(filepath, attributes.isOwnerOnly()) == false) {
                return false;
            }
        }

        if (oldAttributes.isExecutable() != attributes.isExecutable() || forceUpdate) {
            if (setExecutable(filepath, attributes.isExecutable(), allUsers) == false) {
                return false;
            }
        }

        return true;
    }

    private boolean setReadOnly(final String filepath, final boolean readOnly, final boolean allUsers) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        if (readOnly) {
            boolean success = new File(filepath).setReadOnly();

            /*
             * MacOS X can optionally set the immutable bit on readonly files.
             * (This is enabled by setting TP_SET_IMMUTABLE=on.)
             */
            if (success
                && Platform.isCurrentPlatform(Platform.MAC_OS_X)
                && "on".equalsIgnoreCase(PlatformMiscUtils.getInstance().getEnvironmentVariable("TP_SET_IMMUTABLE"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                final String[] args = new String[] {
                    "chflags", //$NON-NLS-1$
                    "uchg", //$NON-NLS-1$
                    filepath
                };

                final int ret = ExecHelpers.exec(args, null);

                if (ret != 0) {
                    log.error(MessageFormat.format(
                        "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                        Integer.toString(ret),
                        ExecHelpers.buildCommandForError(args)));

                    success = false;
                }
            }

            return success;
        } else {
            String[] args;

            /*
             * MacOS X (any 4.4BSD based system) has a immutable flag on
             * filesystem objects. Many programs (Dreamweaver, Eclipse) set the
             * immutable flag when setting files readonly. We need to clear that
             * before setting the file writable.
             */
            if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
                args = new String[] {
                    "chflags", //$NON-NLS-1$
                    "nouchg", //$NON-NLS-1$
                    filepath
                };

                final int ret = ExecHelpers.exec(args, null);

                if (ret != 0) {
                    log.error(MessageFormat.format(
                        "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                        Integer.toString(ret),
                        ExecHelpers.buildCommandForError(args)));
                    return false;
                }
            }

            /*
             * If allUsers is unset, flip this to not writable so that we can
             * return it to writable based on the umask only.
             */
            if (!allUsers) {
                args = new String[] {
                    "chmod", //$NON-NLS-1$
                    "a-w", //$NON-NLS-1$
                    filepath
                };

                final int ret = ExecHelpers.exec(args, null);

                if (ret != 0) {
                    log.error(MessageFormat.format(
                        "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                        Integer.toString(ret),
                        ExecHelpers.buildCommandForError(args)));
                    return false;
                }
            }

            /*
             * Using simply "+w" instead of an explicit user, group, or other
             * specification (e.g. "u+w") will cause chmod to obey the user's
             * umask and only turn on the write bits not masked by it. This is a
             * good balance of convenience and configurability.
             *
             * However, if allUsers is set, extend to a+w.
             */
            final String perm = allUsers ? "a+w" : "+w"; //$NON-NLS-1$ //$NON-NLS-2$

            args = new String[] {
                "chmod", //$NON-NLS-1$
                perm,
                filepath
            };

            final int ret = ExecHelpers.exec(args, null);

            if (ret != 0) {
                log.error(MessageFormat.format(
                    "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                    Integer.toString(ret),
                    ExecHelpers.buildCommandForError(args)));
                return false;
            }
        }

        return true;
    }

    private boolean setOwnerOnly(final String filepath, final boolean ownerOnly) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        String[] args;
        boolean setImmutable = false;

        /*
         * MacOS X (any 4.4BSD based system) has a immutable flag on filesystem
         * objects. Many programs (Dreamweaver, Eclipse) set the immutable flag
         * when setting files readonly. We need to clear that before setting the
         * file executable, then restore it if it was set.
         */
        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            /*
             * Invoke "ls -laO" and read the string back. Then we parse the
             * flags section for the "uchg" flag. On 10.5, this is uppercase
             * "O". On 10.4, this is a lowercase "o".
             */
            if (PlatformVersion.isGreaterThanOrEqualToVersion("10.5")) //$NON-NLS-1$
            {
                args = new String[] {
                    "ls", //$NON-NLS-1$
                    "-laO", //$NON-NLS-1$
                    filepath
                };
            } else {
                args = new String[] {
                    "ls", //$NON-NLS-1$
                    "-lao", //$NON-NLS-1$
                    filepath
                };
            }

            final StringBuffer output = new StringBuffer();

            int ret = ExecHelpers.exec(args, output);

            if (ret != 0) {
                log.error(MessageFormat.format(
                    "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                    Integer.toString(ret),
                    ExecHelpers.buildCommandForError(args)));
                return false;
            }

            final String outputString = output.toString();
            final String[] outputSections = outputString.trim().split("\\s+", 9); //$NON-NLS-1$

            if (outputSections.length < 4) {
                log.error("External command returned invalid usage"); //$NON-NLS-1$
                return false;
            }

            final String[] flags = outputSections[4].split(","); //$NON-NLS-1$

            for (int i = 0; i < flags.length; i++) {
                if (flags[i].equalsIgnoreCase("uchg")) //$NON-NLS-1$
                {
                    setImmutable = true;
                }
            }

            if (setImmutable) {
                args = new String[] {
                    "chflags", //$NON-NLS-1$
                    "nouchg", //$NON-NLS-1$
                    filepath
                };

                ret = ExecHelpers.exec(args, null);

                if (ret != 0) {
                    log.error(MessageFormat.format(
                        "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                        Integer.toString(ret),
                        ExecHelpers.buildCommandForError(args)));
                    return false;
                }
            }
        }

        if (ownerOnly) {
            /*
             * Remove all bits for group and other.
             */
            args = new String[] {
                "chmod", //$NON-NLS-1$
                "go-rwx", //$NON-NLS-1$
                filepath
            };
        } else {
            /*
             * Using simply "+r" instead of an explicit user, group, or other
             * specification (e.g. "u+r") will cause chmod to obey the user's
             * umask and only turn on the execute bits not masked by it. This is
             * a good balance of convenience and configurability.
             */args = new String[] {
                "chmod", //$NON-NLS-1$
                "+r", //$NON-NLS-1$
                filepath
            };
        }

        int ret = ExecHelpers.exec(args, null);

        if (ret != 0) {
            log.error(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
            return false;
        }

        if (setImmutable && Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            args = new String[] {
                "chflags", //$NON-NLS-1$
                "uchg", //$NON-NLS-1$
                filepath
            };

            ret = ExecHelpers.exec(args, null);

            if (ret != 0) {
                log.error(MessageFormat.format(
                    "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                    Integer.toString(ret),
                    ExecHelpers.buildCommandForError(args)));
                return false;
            }
        }

        return true;
    }

    private boolean setExecutable(final String filepath, final boolean executable, final boolean allUsers) {
        Check.notNull(filepath, "filepath"); //$NON-NLS-1$

        String[] args;
        boolean setImmutable = false;

        /*
         * MacOS X (any 4.4BSD based system) has a immutable flag on filesystem
         * objects. Many programs (Dreamweaver, Eclipse) set the immutable flag
         * when setting files readonly. We need to clear that before setting the
         * file executable, then restore it if it was set.
         */
        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            /*
             * Invoke "ls -laO" and read the string back. Then we parse the
             * flags section for the "uchg" flag. On 10.5, this is uppercase
             * "O". On 10.4, this is a lowercase "o".
             */
            if (PlatformVersion.isGreaterThanOrEqualToVersion("10.5")) //$NON-NLS-1$
            {
                args = new String[] {
                    "ls", //$NON-NLS-1$
                    "-laO", //$NON-NLS-1$
                    filepath
                };
            } else {
                args = new String[] {
                    "ls", //$NON-NLS-1$
                    "-lao", //$NON-NLS-1$
                    filepath
                };
            }

            final StringBuffer output = new StringBuffer();

            int ret = ExecHelpers.exec(args, output);

            if (ret != 0) {
                log.error(MessageFormat.format(
                    "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                    Integer.toString(ret),
                    ExecHelpers.buildCommandForError(args)));
                return false;
            }

            final String outputString = output.toString();
            final String[] outputSections = outputString.trim().split("\\s+", 9); //$NON-NLS-1$

            if (outputSections.length < 4) {
                log.error("External command returned invalid usage"); //$NON-NLS-1$
                return false;
            }

            final String[] flags = outputSections[4].split(","); //$NON-NLS-1$

            for (int i = 0; i < flags.length; i++) {
                if (flags[i].equalsIgnoreCase("uchg")) //$NON-NLS-1$
                {
                    setImmutable = true;
                }
            }

            if (setImmutable) {
                args = new String[] {
                    "chflags", //$NON-NLS-1$
                    "nouchg", //$NON-NLS-1$
                    filepath
                };

                ret = ExecHelpers.exec(args, null);

                if (ret != 0) {
                    log.error(MessageFormat.format(
                        "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                        Integer.toString(ret),
                        ExecHelpers.buildCommandForError(args)));
                    return false;
                }
            }
        }

        if (executable) {
            /*
             * Using simply "+x" instead of an explicit user, group, or other
             * specification (e.g. "u+w") will cause chmod to obey the user's
             * umask and only turn on the execute bits not masked by it. This is
             * a good balance of convenience and configurability.
             *
             * However, if allUsers is true, we extend this to all users.
             */
            final String perm = allUsers ? "a+x" : "+x"; //$NON-NLS-1$ //$NON-NLS-2$

            args = new String[] {
                "chmod", //$NON-NLS-1$
                perm,
                filepath
            };
        } else {
            /*
             * Using simply "-x" instead of an explicit user, group, or other
             * specification (e.g. "u+w") will cause chmod to remove all execute
             * bits.
             */
            args = new String[] {
                "chmod", //$NON-NLS-1$
                "-x", //$NON-NLS-1$
                filepath
            };
        }

        int ret = ExecHelpers.exec(args, null);

        if (ret != 0) {
            log.error(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
            return false;
        }

        if (setImmutable && Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            args = new String[] {
                "chflags", //$NON-NLS-1$
                "uchg", //$NON-NLS-1$
                filepath
            };

            ret = ExecHelpers.exec(args, null);

            if (ret != 0) {
                log.error(MessageFormat.format(
                    "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                    Integer.toString(ret),
                    ExecHelpers.buildCommandForError(args)));
                return false;
            }
        }

        return true;
    }

    private boolean isSymbolicLink(final String path) {
        Check.notNull(path, "path"); //$NON-NLS-1$

        /*
         * Invoke "ls -la" and read the string back. Then we parse the mode
         * section (like "lrwxrwxrwx") in Java for the correct execute flag.
         */
        final String[] args = new String[] {
            "ls", //$NON-NLS-1$
            "-la", //$NON-NLS-1$
            path
        };

        final StringBuffer output = new StringBuffer();

        final int ret = ExecHelpers.exec(args, output);

        /*
         * Use "debug" level for a non-zero exit code because ls will return
         * errors if files don't exist (which means we should return false for
         * this method).
         */
        if (ret != 0) {
            log.debug(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
        }

        String outputString = output.toString();

        /*
         * Strip off any leading whitespace (I know of no Unix that starts the
         * line with whitespace but it's good to be careful).
         */
        outputString = outputString.trim();

        /*
         * If the first character is "l" or "L", the file (or directory) is
         * executable.
         */
        if (outputString.length() > 0 && (outputString.charAt(0) == 'l' || outputString.charAt(0) == 'L')) {
            return true;
        }

        return false;
    }

    @Override
    public boolean createSymbolicLink(final String oldpath, final String newpath) {
        Check.notNull(oldpath, "oldpath"); //$NON-NLS-1$
        Check.notNull(newpath, "newpath"); //$NON-NLS-1$

        String[] args;

        /*
         * Invoke "ln -s oldpath newpath".
         */
        args = new String[] {
            "ln", //$NON-NLS-1$
            "-s", //$NON-NLS-1$
            oldpath,
            newpath
        };

        final int ret = ExecHelpers.exec(args, null);

        if (ret != 0) {
            log.error(MessageFormat.format(
                "External command returned non-zero exit status {0}: {1}", //$NON-NLS-1$
                Integer.toString(ret),
                ExecHelpers.buildCommandForError(args)));
            return false;
        }

        return true;
    }

    @Override
    public String[] listMacExtendedAttributes(final String filepath) {
        return null;
    }

    @Override
    public int readMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return -1;
    }

    @Override
    public boolean writeMacExtendedAttribute(
        final String filepath,
        final String attribute,
        final byte[] buffer,
        final int size,
        final long position) {
        return false;
    }

    @Override
    public byte[] getMacExtendedAttribute(final String filepath, final String attribute) {
        return null;
    }

    @Override
    public boolean setMacExtendedAttribute(final String filepath, final String attribute, final byte[] value) {
        return false;
    }
}