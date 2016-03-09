/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that do misc work.
 */

#include <pwd.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <jni.h>

/* These are required for OS X's SystemConfiguration (to get hostname). */
#ifdef MACOS_X
# include <CoreServices/CoreServices.h>
# include <SystemConfiguration/SystemConfiguration.h>
#endif /* MACOS_X */

/*
 * Import limits to try to get host name max len.  Set it if it's in
 * a bizarre place (MacOS X, Linux).
 */
#include <limits.h>

#ifndef HOST_NAME_MAX
# ifdef _POSIX_HOST_NAME_MAX
#  define HOST_NAME_MAX _POSIX_HOST_NAME_MAX
# else
#  define HOST_NAME_MAX 255     /* POSIX defines it as such */
# endif
#endif /* HOST_NAME_MAX */

#include "native_misc.h"
#include "util.h"

/* This is a private function in Mac OS X */
#ifdef MACOS_X
CFStringRef SCPreferencesGetHostName(SCPreferencesRef preferences);
#endif /* MACOS_X */

JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeChangeCurrentDirectory(
    JNIEnv *env, jclass cls, jstring directoryPath)
{
    int result;

    const char * str = javaStringToPlatformChars(env, directoryPath);
    result = chdir(str);
    releasePlatformChars(env, directoryPath, str);

    return result;
}

/*
 * Gets the home directory for the given user, returning NULL if
 * an error occurred or the user was not found.
 */
JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetHomeDirectory(
    JNIEnv *env, jclass cls, jstring username)
{
    struct passwd pd;
    struct passwd * pwdptr = &pd;
    struct passwd * tempPwdPtr;
    char pwdbuffer[1024];
    int ret = 0;

    /*
     * Get the password field entry into our structure.
     */
    const char * str = javaStringToPlatformChars(env, username);

#ifdef __solaris__
    tempPwdPtr = getpwnam_r(str, pwdptr, pwdbuffer, sizeof(pwdbuffer));
#else
    ret = getpwnam_r(str, pwdptr, pwdbuffer, sizeof(pwdbuffer), &tempPwdPtr);
#endif

    releasePlatformChars(env, username, str);

    if (ret != 0 || tempPwdPtr == NULL)
        return NULL;

    return platformCharsToJavaString(env, tempPwdPtr->pw_dir);
}

#ifdef MACOS_X

/*
 * Convert a CFString to a ptr to a char array.  Assumes UTF8.  This
 * function is not exported to JNI.
 */
char *_getCharArrayFromString(CFStringRef string)
{
    CFIndex len;
    CFStringEncoding encoding = kCFStringEncodingUTF8;
    char *result;

    /* Determine the size required to hold this string */
    len =
    CFStringGetMaximumSizeForEncoding(CFStringGetLength(string), encoding)
    + 1;

    if((result = (char *)malloc(len)) == NULL)
    return NULL;

    /* Convert from a CFString to a char array */
    CFStringGetCString(string, result, len, encoding);

    return result;
}

#endif /* MACOS_X */

/*
 * Gets the hostname of the current computer, returning NULL if an error
 * occured.  On MacOS, this will attempt to return the HostName system
 * preference (10.4+).  If that fails, this will attempt to return the
 * LocalHostName system preference (aka, the Bonjour name as configured in
 * Sharing preferences.  10.3+).  This is to work around MacOS's dynamic
 * hostname.  If both of those fail, or on all other platforms, this uses
 * the unix hostname.
 */
JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetComputerName(
    JNIEnv *env, jclass cls)
{
    char *hostname = NULL;
    jstring hostnameJava;

#ifdef MACOS_X
    SInt32 os_version;

    CFStringRef hostnameRef;
    SCPreferencesRef prefs;

    /* Try to determine the current os version */
    if(Gestalt(gestaltSystemVersion, &os_version) != 0)
    os_version = 0;

    /* Try to get the HostName system preference which appeared in 10.4 */
    if(os_version >= 0x1040 &&
        (prefs = SCPreferencesCreate(NULL, CFSTR("Microsoft"), NULL)) != NULL)
    {
        /* Will return NULL if the HostName is not configured */
        if((hostnameRef = (CFStringRef)SCPreferencesGetHostName(prefs)) != NULL)
        {
            /* Convert to a C string */
            hostname = _getCharArrayFromString(hostnameRef);
        }

        /*
         * Note that SCPreferencesGetHostNameRef returns an immutable ref
         * into the preferences, NOT a copy.  We should not release
         * hostnameRef, releasing the preference ref will do that.
         */
        CFRelease(prefs);
    }

    /* If that failed, attempt to get the LocalHostName system preference */
    if(hostname == NULL && os_version >= 0x1030 &&
        (hostnameRef = SCDynamicStoreCopyLocalHostName(NULL)) != NULL)
    {
        /* Convert to a C string */
        hostname = _getCharArrayFromString(hostnameRef);
        CFRelease(hostnameRef);
    }

    if(hostname == NULL)
    {
#endif /* MACOS_X */

    if ((hostname = (char *) malloc(HOST_NAME_MAX + 1)) == NULL)
        return NULL;

    if (gethostname(hostname, HOST_NAME_MAX) < 0)
    {
        free(hostname);
        return NULL;
    }

    /* gethostname() not guaranteed to write a null */
    hostname[HOST_NAME_MAX] = '\0';

#ifdef MACOS_X
}
#endif /* MACOS_X */

    hostnameJava = platformCharsToJavaString(env, hostname);
    free(hostname);

    return hostnameJava;
}

/*
 * Gets the value associated with the environment variable name, or returns NULL
 * if no value was set.
 */
JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_platformmisc_NativePlatformMisc_nativeGetEnvironmentVariable(
    JNIEnv *env, jclass cls, jstring name)
{
    const char * variableName = javaStringToPlatformChars(env, name);

    char * variableValue = getenv(variableName);

    releasePlatformChars(env, name, variableName);

    if (variableValue == NULL)
        return NULL;

    return platformCharsToJavaString(env, variableValue);
}
