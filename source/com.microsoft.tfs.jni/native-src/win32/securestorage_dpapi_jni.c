/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*******************************************************************************
 *******************************************************************************
 *
 * This file is no longer used (compiled or linked).  It's here for future
 * reference if we need to hook up DPAPI (but probably through a simpler
 * interface).
 *
 ********************************************************************************
 ********************************************************************************/

/*
 * JNI functions that do secure password encryption for DPAPI (Win32).
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include <security.h>
#include <jni.h>

#include "native_securestorage_dpapi.h"
#include "util.h"
#include "logger.h"

/*
 * Queries whether the DPAPI implementation is available.
 * Returns JNI_TRUE on success, JNI_FALSE on failure.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_securestorage_NativeSecureStorageDpapi_nativeImplementationAvailable(
    JNIEnv *env, jclass cls)
{
    return JNI_TRUE;
}

/*
 * Encrypts a byte array with DPAPI (with the current user's credentials)
 * and returns the resultant ciphertext as a byte array.  (Or null on
 * failure.)
 *
 * jDescription: a description of the plaintext being encrypted (may be displayed to the user)
 * jPlaintext: the plaintext to encrypt
 * jEntropy: optional additional entropy (may be NULL)
 */
JNIEXPORT jbyteArray JNICALL Java_com_microsoft_tfs_jni_internal_securestorage_NativeSecureStorageDpapi_nativeEncryptPassword(
    JNIEnv *env, jclass cls, jstring jDescription, jbyteArray jPlaintext, jbyteArray jEntropy, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    BOOL status;

    LPCWSTR description;

    DWORD plaintextLength = 0;
    DWORD entropyLength = 0;

    BYTE *plaintext;
    BYTE *entropy = NULL;

    DATA_BLOB plaintextBlob;
    DATA_BLOB entropyBlob;
    DATA_BLOB *entropyBlobPtr;
    DATA_BLOB ciphertextBlob;

    jbyteArray jCiphertext;

    DWORD flags = 0;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        logger = logger_initialize(jvm, "com.microsoft.tfs.jni.natives.NativeSecureStorageDPAPIMethods");
    }

    if (jPlaintext == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: plaintext must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    plaintextLength = (*env)->GetArrayLength(env, jPlaintext);

    if ((plaintext = (BYTE *) malloc(plaintextLength)) == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not malloc");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    (*env)->GetByteArrayRegion(env, jPlaintext, 0, plaintextLength, plaintext);

    plaintextBlob.cbData = plaintextLength;
    plaintextBlob.pbData = plaintext;

    if (jEntropy != NULL)
    {
        entropyLength = (*env)->GetArrayLength(env, jEntropy);

        if ((entropy = (BYTE *) malloc(entropyLength)) == NULL)
        {
            logger_write(logger, LOGLEVEL_ERROR, "Could not malloc");
            logger_dispose(logger);

            free(plaintext);

            return JNI_FALSE;
        }

        (*env)->GetByteArrayRegion(env, jEntropy, 0, entropyLength, entropy);

        entropyBlob.cbData = entropyLength;
        entropyBlob.pbData = entropy;

        entropyBlobPtr = &entropyBlob;
    }
    else
    {
        entropyBlobPtr = NULL;
    }

    description = (jDescription != NULL) ? (LPCWSTR) javaStringToPlatformCharsW(env, jDescription) : NULL;

    if (description != NULL)
    {
        logger_write(logger, LOGLEVEL_INFO, "Encrypting password for %s", description);
    }
    else
    {
        logger_write(logger, LOGLEVEL_INFO, "Encrypting password");
    }

    if (jAllowUi == JNI_FALSE)
    {
        flags |= CRYPTPROTECT_UI_FORBIDDEN;
    }

    status = CryptProtectData(&plaintextBlob, description, entropyBlobPtr, NULL, NULL, flags, &ciphertextBlob);

    releasePlatformChars(env, jDescription, (char *) description);

    free(plaintext);

    if (jEntropy != NULL)
    {
        free(entropy);
    }

    if (status == 0)
    {
        logger_dispose(logger);
        return NULL;
    }

    jCiphertext = (*env)->NewByteArray(env, ciphertextBlob.cbData);

    (*env)->SetByteArrayRegion(env, jCiphertext, 0, ciphertextBlob.cbData, ciphertextBlob.pbData);

    LocalFree(ciphertextBlob.pbData);

    logger_dispose(logger);

    return jCiphertext;
}

/*
 * Decrypts a byte array with DPAPI (with the current user's credentials)
 * and returns the resultant plaintext as a byte array.  (Or null on
 * failure.)
 *
 * jCiphertext: the ciphertext to decrypt
 * jEntropy: optional additional entropy (may be NULL)
 */
JNIEXPORT jbyteArray JNICALL Java_com_microsoft_tfs_jni_internal_securestorage_NativeSecureStorageDpapi_nativeDecryptPassword(
    JNIEnv *env, jclass cls, jstring jDescription, jbyteArray jCiphertext, jbyteArray jEntropy, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    BOOL status;

    LPCWSTR description;

    DWORD ciphertextLength = 0;
    DWORD entropyLength = 0;

    BYTE *ciphertext;
    BYTE *entropy = NULL;

    DATA_BLOB ciphertextBlob;
    DATA_BLOB entropyBlob;
    DATA_BLOB *entropyBlobPtr;
    DATA_BLOB plaintextBlob;

    jbyteArray jPlaintext;

    DWORD flags = 0;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        logger = logger_initialize(jvm, "com.microsoft.tfs.jni.natives.NativeSecureStorageDPAPIMethods");
    }

    if (jCiphertext == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: ciphertext must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    ciphertextLength = (*env)->GetArrayLength(env, jCiphertext);

    if ((ciphertext = (BYTE *) malloc(ciphertextLength)) == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not malloc");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    (*env)->GetByteArrayRegion(env, jCiphertext, 0, ciphertextLength, ciphertext);

    ciphertextBlob.cbData = ciphertextLength;
    ciphertextBlob.pbData = ciphertext;

    if (jEntropy != NULL)
    {
        entropyLength = (*env)->GetArrayLength(env, jEntropy);

        if ((entropy = (BYTE *) malloc(entropyLength)) == NULL)
        {
            logger_write(logger, LOGLEVEL_ERROR, "Could not malloc");
            logger_dispose(logger);

            free(ciphertext);

            return JNI_FALSE;
        }

        (*env)->GetByteArrayRegion(env, jEntropy, 0, entropyLength, entropy);

        entropyBlob.cbData = entropyLength;
        entropyBlob.pbData = entropy;

        entropyBlobPtr = &entropyBlob;
    }
    else
    {
        entropyBlobPtr = NULL;
    }

    description = (jDescription != NULL) ? (LPCWSTR) javaStringToPlatformCharsW(env, jDescription) : NULL;

    if (description != NULL)
    {
        logger_write(logger, LOGLEVEL_INFO, "Decrypting password for %s", description);
    }
    else
    {
        logger_write(logger, LOGLEVEL_INFO, "Decrypting password");
    }

    if (jAllowUi == JNI_FALSE)
    {
        flags |= CRYPTPROTECT_UI_FORBIDDEN;
    }

    status = CryptUnprotectData(&ciphertextBlob, NULL, entropyBlobPtr, NULL, NULL, flags, &plaintextBlob);

    free(ciphertext);

    if (jEntropy != NULL)
    {
        free(entropy);
    }

    if (status == 0)
    {
        logger_dispose(logger);
        return NULL;
    }

    jPlaintext = (*env)->NewByteArray(env, plaintextBlob.cbData);

    (*env)->SetByteArrayRegion(env, jPlaintext, 0, plaintextBlob.cbData, plaintextBlob.pbData);

    LocalFree(plaintextBlob.pbData);

    logger_dispose(logger);

    return jPlaintext;
}
