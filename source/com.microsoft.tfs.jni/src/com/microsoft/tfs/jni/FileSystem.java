// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * An interface to filesystem platform utilities. See {@link FileSystemUtils}
 * for static access to these methods.
 */
public interface FileSystem {
    /**
     * Queries a file's attributes, including creation time, file size, and
     * permissions.
     *
     * @param filepath
     *        The file to read attributes for (not <code>null</code>)
     * @return The attributes of the file. Should never return <code>null</code>
     *         . The return type contains a method to test whether the file
     *         exists.
     * @throws RuntimeException
     *         Throws a runtime exception if the an error occurred (e.g.
     *         permissions error).
     */
    public FileSystemAttributes getAttributes(String filepath);

    /**
     * Sets the attributes on a file, limited to permissions. This sets the
     * dwFileAttributes property on files (in Windows) and sets the appropriate
     * chmod(2) bits (on Unix).
     * <p>
     * The attributes set depend on the platform - for example, the archive,
     * hidden and system bits are supported only on Windows, while the
     * executable bit is supported only on Unix.
     * <p>
     * Modification time is always ignored, as is file size.
     *
     * @param filepath
     *        the file to change attributes for
     * @param attributes
     *        the attributes to save
     * @return true if the attribute change succeeded, false if it did not.
     */
    public boolean setAttributes(String filepath, FileSystemAttributes attributes);

    /**
     * Gets the file's owner as a string. On Windows this string is a security
     * identifier (SID) string. Not implemented on Unix.
     *
     * @param path
     *        the path to get the owner of (must not be <code>null</code>)
     * @return the owner as a string
     * @throws RuntimeException
     *         if there was an error getting the owner
     */
    public String getOwner(String path);

    /**
     * Sets the file's owner from a string. On Windows this string is a security
     * identifier (SID) string. Not implemented on Unix.
     *
     * @param path
     *        the path to set the owner on (must not be <code>null</code>)
     * @param user
     *        the string to set the owner to (must not be <code>null</code>)
     * @throws RuntimeException
     *         if there was an error setting the owner
     */
    public void setOwner(String path, String user);

    /**
     * Adds an inheritable discretionary access control list (DACL) rule on the
     * specified file granting full control to the named user. If
     * copyExistingRulesFromPath is specified all explicit DACL rules from that
     * path are also added to path. Not implemented on Unix.
     *
     * @param path
     *        the path to add the access control rules to (must not be
     *        <code>null</code>)
     * @param user
     *        the user security identifier (SID) to grant access to (must not be
     *        <code>null</code>)
     * @param copyExplicitRulesFromPath
     *        if not <code>null</code>, all explicit access control rules from
     *        this path are also copied to path
     * @throws RuntimeException
     *         if there was an error granting access
     */
    public void grantInheritableFullControl(String path, String user, String copyExplicitRulesFromPath);

    /**
     * Copies all the explicit entries in the source path's discretionary access
     * control list (DACL) to the target path. Existing target DACL entries are
     * preserved.
     *
     * @param sourcePath
     *        the path to copy DACL rules from (must not be <code>null</code>)
     * @param targetPath
     *        the path to copy DACL rules to (must not be <code>null</code>)
     */
    public void copyExplicitDACLEntries(String sourcePath, String targetPath);

    /**
     * Removes all explicit "allow" entries from the given path's discretionary
     * access control list (DACL) for the specified user.
     *
     * @param path
     *        the path to remove explicit "allow" entries from (must not be
     *        <code>null</code>)
     * @param user
     *        the user security identifier (SID) to remove "allow" entries for
     *        (must not be <code>null</code>)
     */
    public void removeExplicitAllowEntries(String path, String user);

    /**
     * Creates a symbolic link at the given newpath that points to the given
     * oldpath.
     * <p>
     * On non-Unix platforms, this method does no work and always returns false.
     *
     * @param oldpath
     *        the old path (existing file) where the link will point (not null).
     * @param newpath
     *        the new path (name of the link to create) (not null).
     * @return true if the link was created, false if not.
     */
    public boolean createSymbolicLink(String oldpath, String newpath);

    /**
     * Lists all the extended attributes for the given filepath.
     * <p>
     * On non-Mac OS X platforms, this method returns null.
     *
     * @param filepath
     *        the path to the file to query
     * @return a String array containing the names of all extended attributes
     *         for the given file
     */
    public String[] listMacExtendedAttributes(String filepath);

    /**
     * Reads bytes out of the extended attribute value for the given filepath.
     * This should only be used for the Resource Fork, Apple's documentation
     * suggests that reading bytes from other xattrs will not work (use
     * getMacExtendedAttribute.)
     * <p>
     * On non-Mac OS X platforms, this returns -1.
     *
     * @param filepath
     *        the path to the file to query
     * @param attribute
     *        the attribute to return
     * @param buffer
     *        a byte array to write a chunk of the xattr into
     * @param size
     *        the length to read
     * @param position
     *        the position to read at
     * @return the number of bytes read or -1 for EOF, -2 for error
     */
    public int readMacExtendedAttribute(String filepath, String attribute, byte[] buffer, int size, long position);

    /**
     * Writes bytes to the extended attribute value for the given filepath. This
     * should only be used for the Resource Fork, Apple's documentation suggests
     * that reading bytes from other xattrs will not work (use
     * getMacExtendedAttribute.)
     * <p>
     * On non-Mac OS X platforms, this returns -1.
     *
     * @param filepath
     *        the path to the file to query
     * @param attribute
     *        the attribute to return
     * @param buffer
     *        a byte array to write a chunk of the xattr into
     * @param size
     *        the length to read
     * @param position
     *        the position to read at
     * @return the number of bytes read or -1 for EOF
     */
    public boolean writeMacExtendedAttribute(String filepath, String attribute, byte[] buffer, int size, long position);

    /**
     * Gets the given extended attribute for the given filepath.
     * <p>
     * On non-Mac OS X platforms, this method returns null.
     *
     * @param filepath
     *        the path to the file to query
     * @param attribute
     *        the attribute to return
     * @return a byte array containing the extended attribute data or null if it
     *         does not exist
     */
    public byte[] getMacExtendedAttribute(String filepath, String attribute);

    /**
     * Sets a Macintosh extended attribute for the given filepath.
     * <p>
     * On non-Mac OS X platforms, this method does no work and returns false.
     *
     * @param filepath
     *        the path to the file to set finder information for
     * @param value
     *        a byte array containing the new extended attribute value (or null
     *        to remove the attribute)
     * @return true if the extended was updated successfully, false otherwise
     */
    public boolean setMacExtendedAttribute(String filepath, String attribute, byte[] value);
}
