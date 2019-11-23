/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that do mutex and semaphore handling.
 */

#include <stdio.h>
#include <windows.h>
#include <jni.h>

#include "native_synchronization.h"
#include "util.h"
#include "logger.h"

/* Mutexes */

JNIEXPORT jlong JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCreateMutex(
    JNIEnv *env, jclass cls, jstring jMutexName)
{
    HANDLE mutex;

    LPCWSTR mutexName = (LPCWSTR) javaStringToPlatformChars(env, jMutexName);

    if (mutexName == NULL)
        return -1;

    mutex = CreateMutexW(NULL, FALSE, mutexName);

    releasePlatformChars(env, jMutexName, mutexName);

    if (mutex == NULL)
    {
        return -1;
    }

    return ptrToJlong(mutex);
}

JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeWaitForMutex(
    JNIEnv *env, jclass cls, jlong jMutexId, jint jTimeout)
{
    HANDLE mutex;
    DWORD timeout;
    DWORD result;

    if (jMutexId < 0)
        return JNI_FALSE;

    if ((mutex = jlongToPtr(jMutexId)) == NULL)
	{
		return -1;
	}

    timeout = (jTimeout < 0) ? INFINITE : jTimeout;

    result = WaitForSingleObject(mutex, timeout);

    if (result == WAIT_ABANDONED || result == WAIT_OBJECT_0)
        return 1;

    /* Would block: return 0 */
    if (result == WAIT_TIMEOUT)
        return 0;

    /* Error: return -1 */
    return -1;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeReleaseMutex(
    JNIEnv *env, jclass cls, jlong jMutexId)
{
    HANDLE mutex;

    if (jMutexId == 0)
        return JNI_FALSE;

    if ((mutex = jlongToPtr(jMutexId)) == NULL)
		return JNI_FALSE;

    if (ReleaseMutex(mutex) == FALSE)
        return JNI_FALSE;

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCloseMutex(
    JNIEnv *env, jclass cls, jlong jMutexId)
{
    HANDLE mutex;

    if (jMutexId == 0)
        return JNI_FALSE;

    if ((mutex = jlongToPtr(jMutexId)) == NULL)
		return JNI_FALSE;

    if (CloseHandle(mutex) != 0)
        return JNI_TRUE;

    return JNI_FALSE;
}
