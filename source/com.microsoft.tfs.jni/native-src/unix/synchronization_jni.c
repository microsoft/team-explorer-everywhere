/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that do mutex and semaphore handling.
 */

#include <semaphore.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <jni.h>

#include "native_synchronization.h"
#include "util.h"
#include "logger.h"

/*
 * HP-UX doesn't define SEM_FAILED, but the man page says it returns -1 on error.
 */
#ifndef SEM_FAILED
# ifdef __hpux__
#  define SEM_FAILED ((sem_t *) -1)
# endif
#endif

/* Mutexes: implemented as a wrapper around a single value semaphore */

JNIEXPORT jlong JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCreateMutex(
    JNIEnv *env, jclass cls, jstring jMutexName)
{
    return Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCreateSemaphore(env, cls,
        jMutexName, 1);
}

JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeWaitForMutex(
    JNIEnv *env, jclass cls, jlong jMutexId, jint jTimeout)
{
    return Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeWaitForSemaphore(env, cls,
        jMutexId, jTimeout);
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeReleaseMutex(
    JNIEnv *env, jclass cls, jlong jMutexId)
{
    return Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeReleaseSemaphore(env, cls,
        jMutexId);
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCloseMutex(
    JNIEnv *env, jclass cls, jlong jMutexId)
{
    return Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCloseSemaphore(env, cls,
        jMutexId);
}

/* Semaphores */

JNIEXPORT jlong JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCreateSemaphore(
    JNIEnv *env, jclass cls, jstring jSemaphoreName, jint jInitialValue)
{
    sem_t *semaphore;
    unsigned int initialValue = jInitialValue;

    const char * semaphoreName = javaStringToPlatformChars(env, jSemaphoreName);

    if (semaphoreName == NULL)
        return -1;

    semaphore = sem_open(semaphoreName, O_CREAT, 0644, initialValue);

    if (semaphore == SEM_FAILED)
        return -1;

    return ptrToJlong(semaphore);
}

JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeWaitForSemaphore(
    JNIEnv *env, jclass cls, jlong jSemaphoreId, jint jTimeout)
{
    sem_t *semaphore;
    unsigned int timeout, sleepTime;

    if (jSemaphoreId < 0)
        return JNI_FALSE;

    semaphore = jlongToPtr(jSemaphoreId);

    /* Block, simply defer to sem_wait as it's probably more efficient than polling. */
    if (jTimeout < 0)
    {
        if (sem_wait(semaphore) == 0)
            return 1;

        /* Error: return -1 */
        return -1;
    }
    /* Non blocking */
    else if (jTimeout == 0)
    {
        if (sem_trywait(semaphore) == 0)
            return 1;

        /* Would block, return 0 */
        if (errno == EAGAIN)
            return 0;

        return -1;
    }

    /* Block for specific milliseconds */
    timeout = jTimeout;

    while (timeout > 0)
    {
        /* Success */
        if (sem_trywait(semaphore) == 0)
            return 1;

        /* Error */
        if (errno != EAGAIN)
            return -1;

        /* Sleep for 200 milliseconds, or the remaining time, whichever is smaller */
        sleepTime = (timeout > 200) ? 200 : timeout;

        /* Would block, sleep and continue */
        usleep(sleepTime * 1000);
        timeout -= sleepTime;
    }

    return 0;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeReleaseSemaphore(
    JNIEnv *env, jclass cls, jlong jSemaphoreId)
{
    sem_t *semaphore;

    if (jSemaphoreId < 0)
        return JNI_FALSE;

    semaphore = jlongToPtr(jSemaphoreId);

    if (sem_post(semaphore) == 0)
        return JNI_TRUE;

    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_synchronization_NativeSynchronization_nativeCloseSemaphore(
    JNIEnv *env, jclass cls, jlong jSemaphoreId)
{
    sem_t *semaphore;

    if (jSemaphoreId < 0)
        return JNI_FALSE;

    semaphore = jlongToPtr(jSemaphoreId);

    if (sem_close(semaphore) == 0)
        return JNI_TRUE;

    return JNI_FALSE;
}
