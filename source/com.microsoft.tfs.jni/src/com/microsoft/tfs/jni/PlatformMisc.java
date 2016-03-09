// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * An interface to miscellaneous platform utilities. See
 * {@link PlatformMiscUtils} for static access to these methods.
 */
public interface PlatformMisc {
    /**
     * Changes the working directory of the JVM. This should be used very rarely
     * (the CLC needs to do this to emulate Microsoft's).
     * <p>
     * <b>This is a dangerous method because the operation is inherently
     * dangerous in the Java virtual machine! Do not use this unless you're
     * prepared to guard all your java.io access against relative path
     * confusion!</b>
     *
     * @param directory
     *        the directory to change to (not null).
     * @return true if the directory change succeeded, false if it did not.
     */
    public boolean changeCurrentDirectory(String directory);

    /**
     * Gets the home directory for the given username or <code>null</code> if
     * the home directory cannot be resolved.
     *
     * @param username
     *        the user whose home directory location to return (not null).
     * @return the home directory of the given user, <code>null</code> if none
     *         was found
     */
    public String getHomeDirectory(String username);

    /**
     * Gets the default Windows code page in use.
     * <p>
     * On Unix platforms this method always returns -1.
     *
     * @return the code page number.
     */
    public int getDefaultCodePage();

    /**
     * Gets this computer's short name (not fully qualified with a domain).
     *
     * @return this computer's short name, or <code>null</code> if an error
     *         happened reading the computer name. Returns <code>null</code>
     *         instead of an empty string if the platform reported an empty
     *         name.
     */
    public String getComputerName();

    /**
     * Gets the value associated with the given environment variable name. Do
     * not prefix with a dollar sign or surround with percent signs.
     * <p>
     * Returns <code>null</code> if the value was not defined or was an empty
     * string. Windows doesn't seem to allow empty environment values (
     * "set FOO=" unsets FOO), but Unix does.
     *
     * @param name
     *        the name of the environment variable to get the value for,
     *        <b>without</b> a dollar sign prefix or percent sign prefix and
     *        suffix--just the raw variable name (not null or empty).
     * @return the value assigned to the given environment variable name, or
     *         <code>null</code> if no value was found in the environment, or
     *         <code>null</code> if the value was defined to be an empty string
     */
    public String getEnvironmentVariable(String name);

    /**
     * Expands environment-variable string and replaces them with the values
     * defined for the current user. This is for Windows only, all other
     * platforms return the specified value unchanged. The supplied parameter is
     * the value of an environment variable and not the name of an environment
     * variable. The value may contain environment-variable strings in the form
     * %variableName%. For each such reference, the %variableName% portion is
     * replaced with the current value of that environment variable.
     * <p>
     *
     * @param value
     *        the value retrieved from an environment varialble which may
     *        contain one or more environment-variable strings in the form
     *        %variable-name%.
     * @return the value with all environment variable substitutions applied or
     *         the original value if not the windows platform.
     */
    public String expandEnvironmentString(String value);

    /**
     * Gets the security identifier (SID) of the user the process (not thread)
     * is currently running as. Not implemented on Unix.
     *
     * @return the security identifier (SID) for the user running the current
     *         process
     * @throws RuntimeException
     *         if there was an error getting the identity
     */
    public String getCurrentIdentityUser();

    /**
     * Gets the well-known SID string for the specified type and (optional)
     * domain SID string. Not implemented on Unix.
     *
     * @param wellKnownSIDType
     *        one of the Windows WELL_KNOWN_SID_TYPE enumeration values
     * @param domainSIDString
     *        the SID string of the domain to resolve the well known type with,
     *        or <code>null</code> if the type does not use a domain SID
     * @return the SID string
     * @throws RuntimeException
     *         if an error occurred resolving the SID string
     */
    public String getWellKnownSID(int wellKnownSIDType, String domainSIDString);

}
