/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * Common utility functions.  Not exports to JNI.
 */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <errno.h>

#ifdef _WIN32
# include <windows.h>
# include <strsafe.h>
#endif /* _WIN32 */

/* MVS is z/OS */
#ifdef _MVS_
# include <jni_convert.h>
#endif /* __MVS__ */

#include "util.h"

/*
 * Runtime exception throwing
 */
#define MESSAGE_BUFFER_SIZE 1024

void throwRuntimeExceptionString(_In_ JNIEnv *env, _Printf_format_string_ const char * messageFormat, ...)
{
	va_list ap;
	char * message;
	jclass exceptionClass;

    va_start(ap, messageFormat);
    message = tee_vsprintf(messageFormat, ap);
    va_end(ap);

	exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");

    if (exceptionClass != NULL)
    {
		(*env)->ThrowNew(env, exceptionClass, message);
	}

	free(message);
}

void throwRuntimeExceptionCode(_In_ JNIEnv * env, platform_error errorCode, _Printf_format_string_ const char * prefixFormat, ...)
{
	va_list ap;
	char * prefix = NULL;
#if defined(__solaris__) || defined(__hpux__)
    char * tmpErrorString = NULL;
#endif
	char errorString[MESSAGE_BUFFER_SIZE];
	char message[MESSAGE_BUFFER_SIZE];

	if (prefixFormat != NULL)
	{
		va_start(ap, prefixFormat);
		prefix = tee_vsprintf(prefixFormat, ap);
		va_end(ap);
	}

	// Format errorString
#ifdef _WIN32
	// String is single-byte wide.  Use FormatMessageA instead of Unicode method.
	if (FormatMessageA(FORMAT_MESSAGE_FROM_SYSTEM, NULL, errorCode, 0, errorString, 
		MESSAGE_BUFFER_SIZE, NULL) == 0)
	{
		sprintf_s(errorString, MESSAGE_BUFFER_SIZE, "Unknown error: %d", errorCode);
	}
#elif defined(__solaris__) || defined(__hpux__)
    /*
      strerror_r is not available on Solaris, but strerror is thread-safe.
      It's marked "obsolescent" on HP-UX, and strerror is thread-safe there, 
      too.
    */
    tmpErrorString = strerror(errorCode);
    if (tmpErrorString != NULL)
    {
        snprintf(errorString, MESSAGE_BUFFER_SIZE, "%s", tmpErrorString);
    }
    else
    {
        snprintf(errorString, MESSAGE_BUFFER_SIZE, "Unknown error: %d", errorCode);
    }
#else
	if (strerror_r(errorCode, errorString, MESSAGE_BUFFER_SIZE) != 0)
	{
		snprintf(errorString, MESSAGE_BUFFER_SIZE, "Unknown error: %d", errorCode);
	}
#endif

	// Format with a prefix if it was provided
	if (prefix != NULL)
	{
#ifdef _WIN32
		sprintf_s(message, MESSAGE_BUFFER_SIZE, "%s: %s", prefix, errorString);
#else
		snprintf(message, MESSAGE_BUFFER_SIZE, "%s: %s", prefix, errorString);
#endif
		free(prefix);

		throwRuntimeExceptionString(env, message);
	}
	else
	{
		throwRuntimeExceptionString(env, errorString);
	}
}

/*
 * JNI pointer marshalling
 */

jlong ptrToJlong(_In_ void * ptr)
{
    jlong pointer = 0;

    memset(&pointer, 0, sizeof(jlong));
    memcpy(&pointer, &ptr, sizeof(void *));

    return pointer;
}

_Ret_maybenull_ void * jlongToPtr(jlong pointer)
{
    void *ptr = 0;

    memcpy(&ptr, &pointer, sizeof(void *));

    return ptr;
}

/*
 * Replacements for missing or unsafe POSIX/CRT functions
 */

_Ret_maybenull_ char * tee_strndup(_In_z_ const char * str, size_t n)
{
    size_t originalSize;
    size_t limitSize;
    size_t duplicateSize;
    char * ret;

    if (str == NULL)
    {
        return NULL;
    }

    /* Calculate the two possible sizes and use the smaller for the duplicate. */
    originalSize = strlen(str) + 1;
    limitSize = n + 1;
    duplicateSize = (originalSize < limitSize) ? originalSize : limitSize;

    /* Copy and terminate. */
    ret = (char *) malloc(duplicateSize);
    if (ret == NULL)
    {
        return NULL;
    }

    memcpy(ret, str, duplicateSize);
    ret[duplicateSize - 1] = 0;

    return ret;
}

char * tee_vsprintf(const char * fmt, va_list ap)
{
    int outsize;

    char * buf = (char *) malloc(TEE_VSPRINTF_MAX_SIZE);
    if (buf == NULL)
    {
        return NULL;
    }

#ifdef _WIN32
    outsize = vsnprintf_s(buf, TEE_VSPRINTF_MAX_SIZE, _TRUNCATE, fmt, ap);
#else
    outsize = vsnprintf(buf, TEE_VSPRINTF_MAX_SIZE, fmt, ap);
#endif

    /*
     * outsize is the count of chars written not including the NUL.
     *
     * -1 is a detected failure.  Any size greater than the buffer size - 1
     * means the system is signalling it needs a bigger buffer.  A size
     * equal to the buffer size - 1 means it could have just fit or been
     * truncated, which is OK.
     */
    if (outsize == -1 || outsize >= TEE_VSPRINTF_MAX_SIZE)
    {
        free(buf);
        return NULL;
    }

    return buf;
}

/*
 * Java string (UTF-16) to platform string (varies) conversions
 */

#ifdef __MVS__
/* 
 * Converts Java UTF-8 string (jstring) to EBCDIC string (char *).
 * Returns NULL if an error occurred measuring or converting the string.
 *
 * This implementation inspired by:
 *  http://www.ibm.com/developerworks/eserver/articles/java_code.html
 */
const char * getEBCDICStringChars(JNIEnv * env, jstring javaString)
{
    jint rc, olen;
    char * platformString;

    /* A return of 0 is success, others are failures. */
    rc = GetStringPlatformLength(env, javaString, &olen, NULL);

    if (rc != 0)
    return NULL;

    platformString = (char *) malloc(olen);
    rc = GetStringPlatform(env, javaString, platformString, olen, NULL);

    if (rc != 0)
    return NULL;

    return platformString;
}
#endif /* __MVS__ */

_Ret_maybenull_ jstring platformCharsToJavaString(_In_ JNIEnv * env, _In_z_ const platform_char * platformString)
{
#ifdef __MVS__
    jstring javaString;

    if (NewStringPlatform(env, platformString, &javaString, NULL) != 0)
    {
        return NULL;
    }

    return javaString;
#elif defined(_WIN32)
    size_t len;

    if (FAILED(StringCchLength(platformString, STRSAFE_MAX_CCH, &len)))
    {
        return NULL;
    }

    /*
     * size_t is larger than jsize on 64-bit Windows (64-bit vs. 32-bit),
     * but STRSAFE_MAX_CCH is always INT_MAX or smaller so we're safe to cast.
     *
     * strsafe.h says: "The user may override STRSAFE_MAX_CCH, but it must
     * always be less than INT_MAX"
     */
    return (*env)->NewString(env, platformString, (jsize) len);
#else /* Unix */
    return (*env)->NewStringUTF(env, platformString);
#endif /* __MVS__ */
}

_Ret_maybenull_ const platform_char * javaStringToPlatformChars(_In_ JNIEnv * env, _In_ jstring javaString)
{
#ifdef __MVS__
    return getEBCDICStringChars(env, javaString);
#elif defined(_WIN32)
    return (*env)->GetStringChars(env, javaString, NULL);
#else /* Unix */
    return (*env)->GetStringUTFChars(env, javaString, NULL);
#endif
}

void releasePlatformChars(_In_ JNIEnv * env, _In_ jstring javaString, _In_z_ const platform_char * platformString)
{
#ifdef __MVS__
    free((void *) platformString);
#elif defined(_WIN32)
    (*env)->ReleaseStringChars(env, javaString, platformString);
#else /* Unix */
    (*env)->ReleaseStringUTFChars(env, javaString, platformString);
#endif
}

/*
 * Windows platform utilites
 */

#ifdef _WIN32
HMODULE safeLoadSystemDLL(_In_z_ LPCTSTR dllName)
{
    WCHAR fullPath[MAX_PATH];
    DWORD length = 0;

    if (dllName == NULL)
    {
        return NULL;
    }

    length = GetSystemDirectoryW(fullPath, MAX_PATH);

    if (length == 0 || length > MAX_PATH)
    {
        return NULL;
    }

    if (FAILED(StringCchCatW(fullPath, MAX_PATH, L"\\")))
    {
        return NULL;
    }

    if (FAILED(StringCchCatW(fullPath, MAX_PATH, dllName)))
    {
        return NULL;
    }

    return LoadLibrary(fullPath);
}
#endif /* _WIN32 */

