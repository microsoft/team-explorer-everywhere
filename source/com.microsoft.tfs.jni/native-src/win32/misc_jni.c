/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#include <stdio.h>
#include <windows.h>
#include <direct.h>
#include <jni.h>
#include <sddl.h>

#include "tee_sal.h"
#include "util.h"
#include "native_misc.h"

_Ret_maybenull_ jchar* _allocateBuffer(_In_ JNIEnv *env, DWORD size)
{
    jchar *buffer = malloc(sizeof(jchar) * size);

    /*
     * malloc() may fail.  We should throw.
     */
    if (buffer == NULL)
    {
        jclass oomError = (*env)->FindClass(env, "java/lang/OutOfMemoryError");

        if (oomError == NULL)
        {
            return NULL;
        }

        (*env)->ThrowNew(env, oomError, "malloc() returned NULL");
        return NULL;
    }

    return buffer;
}

JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeChangeCurrentDirectory(
    JNIEnv *env, jclass cls, jstring directoryPath)
{
    BOOL result;
    const WCHAR *str;
	
	if ((str = javaStringToPlatformChars(env, directoryPath)) == NULL)
	{
		// String allocation failed, exception already thrown
		return 1;
	}

    result = SetCurrentDirectoryW(str);
    releasePlatformChars(env, directoryPath, str);

    return (result == TRUE) ? 0 : 1;
}

/*
 * Gets the default code page in use by Windows.
 */
JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetDefaultCodePage(
    JNIEnv *env, jclass cls)
{
    return GetACP();
}

/*
 * Gets the computer's name.  This is the NetBIOS name, equivalent to .NET's 
 * Environment.MachineName property used to identify computers to TFS.
 */
JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetComputerName(
    JNIEnv *env, jclass cls)
{
    /*
     * Double duty as input buffer size indicator and count of characters
     * written.
     */
    DWORD size = MAX_COMPUTERNAME_LENGTH + 1;

    /*
     * The maximum computer name is generally very short (15),
     * so we can just use the stack.
     */
    WCHAR nameBuffer[MAX_COMPUTERNAME_LENGTH + 1];

	if(GetComputerNameW(nameBuffer, &size) == 0)
    {
        return NULL;
    }

    // GetComputerNameW always terminates the string
    return platformCharsToJavaString(env, nameBuffer);
}

/*
 * Gets the value for the environment variable name given, or NULL if not found.
 */
JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetEnvironmentVariable(
    JNIEnv *env, jclass cls, jstring name)
{
    DWORD bufferSize = 0;
    DWORD newSize = 0;
    jchar * buffer;
    jstring ret;

    const WCHAR * variableName = javaStringToPlatformChars(env, name);

    if (variableName == NULL)
    {
        return NULL;
    }

    /*
     * The first call to GetEnvironmentVariableW() returns the correct size to use
     * for the second call.
     */
    newSize = GetEnvironmentVariableW(variableName, NULL, bufferSize);

    /*
     * Variable not found.
     */
    if (newSize == 0)
    {
        releasePlatformChars(env, name, variableName);
        return NULL;
    }

    /*
     * Size includes null terminator.
     */
    bufferSize = newSize;
    buffer = _allocateBuffer(env, bufferSize);

    if (buffer == NULL)
    {
        releasePlatformChars(env, name, variableName);
        return NULL;
    }

    /*
     * Get the environment variable.
     */
    newSize = GetEnvironmentVariableW(variableName, buffer, bufferSize);
    releasePlatformChars(env, name, variableName);

    /*
     * Variable not found or not enough space (still! it must have grown since
     * the first call!).
     */
    if (newSize == 0 || newSize > bufferSize)
    {
        free(buffer);
        return NULL;
    }

    ret = platformCharsToJavaString(env, buffer);
    free(buffer);
    return ret;
}

/*
 * Expands environment variable references of the for %variableName% in the specified value.
 * Substitutions are found in the current users environment.
 */
JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeExpandEnvironmentString(
    JNIEnv *env, jclass cls, jstring jValue)
{
    DWORD bufferSize = 0;
    DWORD newSize = 0;
    jchar * buffer;
    jstring ret;
    const WCHAR *value;
	
	if ((value = javaStringToPlatformChars(env, jValue)) == NULL)
	{
		// String allocation failed, exception already thrown
		return NULL;
	}

    /*
     * The first call returns the correct size to use for the second call.
     */
    newSize = ExpandEnvironmentStringsW(value, NULL, bufferSize);

    /*
     * Some weird error (maybe syntax?).
     */
    if (newSize == 0)
    {
        releasePlatformChars(env, jValue, value);
        return NULL;
    }

    /*
     * Size includes null terminator.
     */
    bufferSize = newSize;
    buffer = _allocateBuffer(env, bufferSize);

    /*
     * Buffer couldn't be allocated.
     */
    if (buffer == NULL)
    {
        releasePlatformChars(env, jValue, value);
        return NULL;
    }

    /*
     * Expand the environment variables.
     */
    newSize = ExpandEnvironmentStringsW(value, buffer, bufferSize);
    releasePlatformChars(env, jValue, value);

    /*
     * Error or not enough space (still! it must have grown since
     * the first call!).
     */
    if (newSize == 0 || newSize > bufferSize)
    {
        free(buffer);
        return NULL;
    }

    ret = platformCharsToJavaString(env, buffer);
    free(buffer);
    return ret;
}

JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetCurrentIdentityUser(JNIEnv *env,
	jclass cls)
{
	DWORD result = 0;
	HANDLE processTokenHandle = NULL;
	PTOKEN_OWNER ownerToken = NULL;
	DWORD ownerTokenSize = 0;
	WCHAR * ownerSIDString = NULL;
	jstring jOwnerSIDString = NULL;

	// Get this process's token; impersonation changes the _thread's_ token, 
	// but not the process's, so this gets the real owner of the process.
	if (OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &processTokenHandle) == 0)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error getting the current process's token");
		goto cleanup;
	}

	// Call once for size
	if (GetTokenInformation(processTokenHandle, TokenOwner, NULL, ownerTokenSize, &ownerTokenSize) == 0)
	{
		result = GetLastError();
		if (result != ERROR_INSUFFICIENT_BUFFER)
		{
			throwRuntimeExceptionCode(env, GetLastError(), "Error getting token information size");
			goto cleanup;
		}
	}

	// Allocate the structure
	ownerToken = (PTOKEN_OWNER) malloc(ownerTokenSize);
	if (ownerToken == NULL)
	{
		goto cleanup;
	}

	// Call again for data
	if (GetTokenInformation(processTokenHandle, TokenOwner, ownerToken, ownerTokenSize, &ownerTokenSize) == 0)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error getting token information");
		goto cleanup;
	}

	// Convert to string
	if (ConvertSidToStringSidW(ownerToken->Owner, &ownerSIDString) == 0)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error converting sid to string sid");
		goto cleanup;
	}

	jOwnerSIDString = platformCharsToJavaString(env, ownerSIDString);

cleanup:

	if (processTokenHandle != NULL)
	{
		CloseHandle(processTokenHandle);
	}
	if (ownerSIDString != NULL)
	{
		LocalFree(ownerSIDString);
	}
	if (ownerToken != NULL)
	{
		free(ownerToken);
	}

	return jOwnerSIDString;
}

JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetWellKnownSID(JNIEnv *env,
	jclass cls, jint wellKnownSIDType, jstring jDomainSIDString)
{
	const WCHAR * domainSIDString = NULL;
	PSID domainSID = NULL;
	PSID wellKnownSID = NULL;
	DWORD wellKnownSIDSize = SECURITY_MAX_SID_SIZE;
	WCHAR * wellKnownSIDString = NULL;
	jstring jWellKnownSIDString = NULL;

	if (jDomainSIDString != NULL)
	{
		if ((domainSIDString = javaStringToPlatformChars(env, jDomainSIDString)) == NULL)
		{
			// String allocation failed, exception already thrown
			goto cleanup;
		}

		if (ConvertStringSidToSidW(domainSIDString, &domainSID) == FALSE)
		{
			throwRuntimeExceptionCode(env, GetLastError(), "Error converting string sid %S to sid", domainSIDString);
			goto cleanup;
		}
	}

	// Allocate a SID to receive the info
	wellKnownSID = malloc(wellKnownSIDSize);
	if (wellKnownSID == NULL)
	{
		throwRuntimeExceptionString(env, "could not allocate SID");
		goto cleanup;
	}

	// Get the SID structure that matches the type and domain
	if (CreateWellKnownSid((WELL_KNOWN_SID_TYPE) wellKnownSIDType, domainSID, wellKnownSID, 
		&wellKnownSIDSize) == 0)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error getting well known sid for type %d, domain %S", 
			wellKnownSIDType, domainSIDString != NULL ? domainSIDString : L"(null)");
		goto cleanup;
	}

	// Convert the structure to a string
	if (ConvertSidToStringSidW(wellKnownSID, &wellKnownSIDString) == 0)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error converting sid to string sid");
		goto cleanup;
	}

	jWellKnownSIDString = platformCharsToJavaString(env, wellKnownSIDString);

cleanup:

	if (domainSIDString != NULL)
	{
		releasePlatformChars(env, jDomainSIDString, domainSIDString);
	}
	if (domainSID != NULL)
	{
		LocalFree(domainSID);
	}
	if (wellKnownSID != NULL)
	{
		free(wellKnownSID);
	}
	if (wellKnownSIDString != NULL)
	{
		LocalFree(wellKnownSIDString);
	}

	return jWellKnownSIDString;
}
