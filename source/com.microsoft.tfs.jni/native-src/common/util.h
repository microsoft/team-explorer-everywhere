/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#ifndef UTIL_H
#define UTIL_H

#include <jni.h>

#ifdef _WIN32
# include <windows.h>
#endif

#include "tee_sal.h"

/**********************************************************************
 * Runtime exception throwing
 **********************************************************************/

#ifdef _WIN32
typedef DWORD platform_error;
#else
typedef int platform_error;
#endif

/*
 * Throws a RuntimeException with the specified message (always a single-
 * byte wide string).  The caller should return control to Java quickly 
 * after calling this method.
 *
 * messageFormat must not be NULL.
 */
void throwRuntimeExceptionString(_In_ JNIEnv * env, _Printf_format_string_ const char * messageFormat, ...);

/*
 * Throws a RuntimeException with an error message appropriate for
 * the specified system error code prefixed by the given string.  If a
 * prefix is given, ": " and the error are appended.  If a NULL prefix
 * is given, only the error message is included in the exception (no
 * separating colon).
 *
 * On Windows the error code is formatted with FormatMessage, on Unix 
 * strerror_r is used.
 */
void throwRuntimeExceptionCode(_In_ JNIEnv * env, platform_error errorCode, _Printf_format_string_ const char * prefixFormat, ...);

/**********************************************************************
 * JNI pointer marshalling
 **********************************************************************/

/*
 * Converts a C pointer to a jlong, suitable for 32 or 64 bit archs.
 */
jlong ptrToJlong(_In_ void *ptr);

/*
 * Converts a jlong to a C pointer, suitable for 32 or 64 bit archs.
 */
_Ret_maybenull_ void *jlongToPtr(jlong pointer);

/**********************************************************************
 * Replacements for missing or unsafe POSIX/CRT functions
 **********************************************************************/

/*
 * Solaris doesn't have strndup, so here's a portable one.
 * Free the result with free(3).  Returns NULL on error.
 *
 * This function only works with single-byte char strings.
 */
_Ret_maybenull_ char * tee_strndup(_In_z_ const char * str, size_t n);

/*
 * Printf-style message building with varargs support.  Truncates output to 
 * TEE_VSPRINTF_MAX_SIZE chars (but always terminates the string).
 * Free the result with free(3).  Returns NULL on error.
 *
 * This function only works with single-byte char strings, but the "%S" type
 * may be used on Windows to format wide character arguments.
 */
#define TEE_VSPRINTF_MAX_SIZE 4096
_Ret_maybenull_ char * tee_vsprintf(_Printf_format_string_ const char * fmt, va_list ap);

/**********************************************************************
 * Java string (UTF-16) to platform string (varies) conversions
 *
 * Use these functions every time you need to get the platform string
 * from a jstring to use in a Unix/Win32 function, and every time you
 * need to put a platform string back into a jstring to return to
 * Java.
 **********************************************************************/

#ifdef _WIN32
// Windows WCHAR is 16-bit (for UTF-16)
typedef WCHAR platform_char;
#else
// Unix char is 8-bit (for UTF-8 or vendor code page)
typedef char platform_char;
#endif

/*
 * Use this function every time you need to wrap a platform string in a
 * jstring to send back to Java.  platformString will be a single-byte,
 * multi-byte, or wide character string (depending on platform).
 * Returns NULL on error.
 */
_Ret_maybenull_ jstring platformCharsToJavaString(_In_ JNIEnv * env, _In_z_ const platform_char * platformString);

/*
 * Use this function every time you need to get a platform string from
 * a jstring to use with platform functions.  The returned string  will be a
 * single-byte, multi-byte, or wide character string (depending on platform).
 * Returns NULL on error.
 *
 * Free the result with releasePlatformChars().
 */
_Ret_maybenull_ const platform_char * javaStringToPlatformChars(_In_ JNIEnv * env, _In_ jstring javaString);

/*
 * Use this to free strings returned by javaStringToPlatformChars().
 */
void releasePlatformChars(_In_ JNIEnv * env, _In_ jstring javaString, _In_z_ const platform_char * platformString);

/**********************************************************************
 * Windows platform utilites
 **********************************************************************/

#ifdef _WIN32
/*
 * Like Win32 LoadLibrary but takes only short names ("security.dll") and loads them
 * from the system directory (something like "C:\Windows\System32") by full path.  
 * This helps mitigate DLL load path attacks.
 *
 * Loading libraries from non-system locations requires a different and more
 * delicate approach to ensure safety from DLL load path attacks.
 */
HMODULE safeLoadSystemDLL(_In_z_ LPCTSTR lpFileName);
#endif /* _WIN32 */

#endif /* UTIL_H */
