/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that do secure password storage for Keychain (Mac OS).
 */

#ifndef KEYCHAIN_JNI_H
#define KEYCHAIN_JNI_H

#include "logger.h"

typedef struct
{
    jstring jServerName;
    jstring jID;
    jstring jAccountName;
    jstring jPath;
    jint jPort;
    jint jProtocol;
    jint jAuthenticationType;
    jbyteArray jPassword;
    jstring jLabel;
    jstring jComment;

    const char *serverName;
    UInt32 serverNameLength;

    const char *id;
    UInt32 idLength;

    const char *accountName;
    UInt32 accountNameLength;

    const char *path;
    UInt32 pathLength;

    unsigned short port;

    SecProtocolType protocol;

    SecAuthenticationType authenticationType;

    const char *label;
    UInt32 labelLength;

    const char *comment;
    UInt32 commentLength;

    void *password;
    UInt32 passwordLength;
} keychain_internet_password_t;

Boolean _disableKeychainUI(logger_t *logger, Boolean *existingAllowUi);
Boolean _enableKeychainUI(logger_t *logger, Boolean existingAllowUi);

jobject _getJavaPasswordDataFromNative(JNIEnv *env, keychain_internet_password_t *passwordData);
keychain_internet_password_t *_getNativePasswordDataFromJava(JNIEnv *env, jobject jPasswordData);
void _freeNativePasswordData(JNIEnv *env, keychain_internet_password_t *passwordData);

#endif /* KEYCHAIN_JNI_H */
