/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that do secure password storage for Keychain (Mac OS).
 */

#include <Security/Security.h>
#include <string.h>
#include <jni.h>

#include "keychain_jni.h"
#include "native_keychain.h"
#include "util.h"
#include "logger.h"

/*
 * Sets the password for the given internet protocol for the given user.
 * Returns JNI_TRUE on success, JNI_FALSE on failure.
 *
 * jPasswordData: the KeychainInternetPassword object containing the password data to add to keychain
 * jAllowUi: JNI_TRUE to allow UI to be raised, JNI_FALSE otherwise
 */
JNIEXPORT jboolean Java_com_microsoft_tfs_jni_internal_keychain_NativeKeychain_nativeAddInternetPassword(JNIEnv *env,
    jclass cls, jobject jPasswordData, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    keychain_internet_password_t *passwordData;

    OSStatus status;

    Boolean existingAllowUi;

    /*
     * The attributes array must be sized appropriately to handle all
     * attributes we may pass, see below.
     */
    SecKeychainAttributeList attrList;
    SecKeychainAttribute attributes[2];

    SecKeychainItemRef itemRef = NULL;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        if ((logger = logger_initialize(jvm, "com.microsoft.tfs.jni.internal.keychain.NativeKeychain"))
            == NULL)
        {
            return JNI_FALSE;
        }
    }

    if (jPasswordData == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: password data must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    if ((passwordData = _getNativePasswordDataFromJava(env, jPasswordData)) == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not convert java keychain data to native");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    if (passwordData->serverName == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "serverName is required for keychain");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    /* Handle -noprompt mode */
    if (jAllowUi == JNI_FALSE)
    {
        if (_disableKeychainUI(logger, &existingAllowUi) == FALSE)
        {
            logger_dispose(logger);
            return JNI_FALSE;
        }
    }

    /*
     * The password does not exist in the keychain.  Add it.
     */
    logger_write(logger, LOGLEVEL_INFO, "Saving entry to keychain for %s", passwordData->serverName);

    status = SecKeychainAddInternetPassword(NULL, /* Save to default keychain */
    passwordData->serverNameLength, passwordData->serverName, passwordData->idLength, passwordData->id,
        passwordData->accountNameLength, passwordData->accountName, passwordData->pathLength, passwordData->path,
        passwordData->port, passwordData->protocol, passwordData->authenticationType, passwordData->passwordLength,
        passwordData->password, &itemRef);

    /*
     * Update the label and comment, this can't be done when adding a password.
     *
     * Note: if you add additional attributes, update the size declaration
     * (above)
     */
    if ((passwordData->labelLength > 0 || passwordData->commentLength > 0) && status == 0 && itemRef != NULL)
    {
        attrList.count = 0;
        attrList.attr = attributes;

        if (passwordData->labelLength > 0)
        {
            attributes[attrList.count].tag = kSecLabelItemAttr;
            attributes[attrList.count].length = passwordData->labelLength;
            attributes[attrList.count].data = (void *) passwordData->label;

            attrList.count++;
        }

        if (passwordData->commentLength > 0)
        {
            attributes[attrList.count].tag = kSecCommentItemAttr;
            attributes[attrList.count].length = passwordData->commentLength;
            attributes[attrList.count].data = (void *) passwordData->comment;

            attrList.count++;
        }

        status = SecKeychainItemModifyAttributesAndData(itemRef, &attrList, 0, NULL);

        /* If we couldn't update the label, only warn. */
        if (status != 0)
        {
            logger_write(logger, LOGLEVEL_WARN, "Could not update name of keychain entry for %s: status = %d",
                passwordData->serverName, status);
            status = 0;
        }
    }

    if(status == 0 && itemRef != NULL)
    {
        CFRelease(itemRef);
    }

    _freeNativePasswordData(env, passwordData);

    if (status != 0)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not save entry to keychain: status = %d", status);
    }
    else
    {
        logger_write(logger, LOGLEVEL_DEBUG, "Successfully saved entry to keychain");
    }

    /* Reset the UI in noprompt mode */
    if (jAllowUi == JNI_FALSE)
    {
        _enableKeychainUI(logger, existingAllowUi);
    }

    logger_dispose(logger);

    return (status == 0) ? JNI_TRUE : JNI_FALSE;
}

/*
 * Modifies the attributes and/or plaintext password for the given internet
 * protocol for the given user.  Returns JNI_TRUE on success, JNI_FALSE on
 * failure.
 *
 * jOldPasswordData: the KeychainInternetPassword object containing the password data to find in the keychain
 * jNewPasswordData: the KeychainInternetPassword object containing the password data to update keychain with
 * jAllowUi: JNI_TRUE to allow UI to be raised, JNI_FALSE otherwise
 */
JNIEXPORT jboolean Java_com_microsoft_tfs_jni_internal_keychain_NativeKeychain_nativeModifyInternetPassword(
    JNIEnv *env, jclass cls, jobject jOldPasswordData, jobject jNewPasswordData, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    OSStatus status;

    keychain_internet_password_t *oldPasswordData;
    keychain_internet_password_t *newPasswordData;

    SecKeychainItemRef itemRef = NULL;

    /*
     * The attributes array must be sized appropriately to handle all
     * attributes we may pass, see below.
     */
    SecKeychainAttributeList attrList;
    SecKeychainAttribute attributes[8];

    Boolean existingAllowUi;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        if ((logger = logger_initialize(jvm, "com.microsoft.tfs.jni.internal.keychain.NativeKeychain"))
            == NULL)
        {
            return JNI_FALSE;
        }
    }

    if (jOldPasswordData == NULL || jNewPasswordData == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: password data must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    if ((oldPasswordData = _getNativePasswordDataFromJava(env, jOldPasswordData)) == NULL || (newPasswordData
        = _getNativePasswordDataFromJava(env, jNewPasswordData)) == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not convert java keychain data to native");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    if (oldPasswordData->serverName == NULL || newPasswordData->serverName == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "serverName is required for keychain");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    /* Handle -noprompt mode */
    if (jAllowUi == JNI_FALSE)
    {
        if (_disableKeychainUI(logger, &existingAllowUi) == FALSE)
        {
            logger_dispose(logger);
            return JNI_FALSE;
        }
    }

    /* Find the existing keychain entry */
    logger_write(logger, LOGLEVEL_DEBUG, "Searching keychain for entry for %s", oldPasswordData->serverName);

    status = SecKeychainFindInternetPassword(NULL, /* Search all keychains */
    oldPasswordData->serverNameLength, oldPasswordData->serverName, oldPasswordData->idLength, oldPasswordData->id,
        oldPasswordData->accountNameLength, oldPasswordData->accountName, oldPasswordData->pathLength,
        oldPasswordData->path, oldPasswordData->port, oldPasswordData->protocol, oldPasswordData->authenticationType,
        NULL, NULL, &itemRef);

    if (status == 0 && itemRef != NULL)
    {
        /*
         * Note: if you add additional attributes, update the size declaration
         * (above)
         */
        attrList.count = 8;
        attrList.attr = attributes;

        attributes[0].tag = kSecServerItemAttr;
        attributes[0].length = newPasswordData->serverNameLength;
        attributes[0].data = (void *) newPasswordData->serverName;

        attributes[1].tag = kSecSecurityDomainItemAttr;
        attributes[1].length = newPasswordData->idLength;
        attributes[1].data = (void *) newPasswordData->id;

        attributes[2].tag = kSecAccountItemAttr;
        attributes[2].length = newPasswordData->accountNameLength;
        attributes[2].data = (void *) newPasswordData->accountName;

        attributes[3].tag = kSecPathItemAttr;
        attributes[3].length = newPasswordData->pathLength;
        attributes[3].data = (void *) newPasswordData->path;

        attributes[4].tag = kSecPortItemAttr;
        attributes[4].length = sizeof(newPasswordData->port);
        attributes[4].data = (void *) &(newPasswordData->port);

        attributes[5].tag = kSecProtocolItemAttr;
        attributes[5].length = sizeof(newPasswordData->protocol);
        attributes[5].data = (void *) &(newPasswordData->protocol);

        attributes[6].tag = kSecLabelItemAttr;
        attributes[6].length = newPasswordData->labelLength;
        attributes[6].data = (void *) newPasswordData->label;

        attributes[7].tag = kSecCommentItemAttr;
        attributes[7].length = newPasswordData->commentLength;
        attributes[7].data = (void *) newPasswordData->comment;

        status = SecKeychainItemModifyAttributesAndData(itemRef, &attrList, newPasswordData->passwordLength,
            newPasswordData->password);

        if (status == 0)
        {
            logger_write(logger, LOGLEVEL_INFO, "Updated keychain entry for %s", newPasswordData->serverName);
        }
        else
        {
            logger_write(logger, LOGLEVEL_WARN, "Could not update keychain entry for %s: status = %d",
                newPasswordData->serverName, status);
        }

        CFRelease(itemRef);
    }
    else
    {
        logger_write(logger, LOGLEVEL_WARN, "Could not update keychain entry for %s (not found): status = %d",
            oldPasswordData->serverName, status);
    }

    _freeNativePasswordData(env, oldPasswordData);
    _freeNativePasswordData(env, newPasswordData);

    /* Reset the UI in noprompt mode */
    if (jAllowUi == JNI_FALSE)
    {
        _enableKeychainUI(logger, existingAllowUi);
    }

    logger_dispose(logger);

    return (status == 0) ? JNI_TRUE : JNI_FALSE;
}

/*
 * Retrieves the (complete) keychain information for the given keychain data.
 * Returns a KeychainInternetPassword on success, or NULL on failure.
 *
 * jPasswordData: the KeychainInternetPassword object containing the password data to find in the keychain
 * jAllowUi: JNI_TRUE to allow UI to be raised, JNI_FALSE otherwise
 */
JNIEXPORT jbyteArray Java_com_microsoft_tfs_jni_internal_keychain_NativeKeychain_nativeFindInternetPassword(
    JNIEnv *env, jclass cls, jobject jQueryPasswordData, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    OSStatus status;

    SecKeychainItemRef itemRef = NULL;

    SecKeychainAttributeInfo attributeQuery;
    SecKeychainAttributeList *attrList;
    SecKeychainAttribute *attributes;

    UInt32 attributeTag[9];
    UInt32 attributeFormat[9];

    void *password;
    UInt32 passwordLength;

    keychain_internet_password_t *queryPasswordData;
    keychain_internet_password_t completePasswordData;

    Boolean existingAllowUi;

    jobject jCompletePasswordData = NULL;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        if ((logger = logger_initialize(jvm, "com.microsoft.tfs.jni.internal.keychain.NativeKeychain"))
            == NULL)
        {
            return JNI_FALSE;
        }
    }

    if (jQueryPasswordData == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: password data must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    if ((queryPasswordData = _getNativePasswordDataFromJava(env, jQueryPasswordData)) == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not convert java keychain data to native");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    if (queryPasswordData->serverName == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "serverName is required for keychain");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    /* Handle -noprompt mode */
    if (jAllowUi == JNI_FALSE)
    {
        if (_disableKeychainUI(logger, &existingAllowUi) == FALSE)
        {
            logger_dispose(logger);
            return JNI_FALSE;
        }
    }

    logger_write(logger, LOGLEVEL_INFO, "Searching keychain for entry for %s", queryPasswordData->serverName);

    /* Locate the keychain record matching the given query. */
    status = SecKeychainFindInternetPassword(NULL, /* Search all keychains */
    queryPasswordData->serverNameLength, queryPasswordData->serverName, queryPasswordData->idLength,
        queryPasswordData->id, queryPasswordData->accountNameLength, queryPasswordData->accountName,
        queryPasswordData->pathLength, queryPasswordData->path, queryPasswordData->port, queryPasswordData->protocol,
        queryPasswordData->authenticationType, NULL, NULL, &itemRef);

    if (status != 0 || itemRef == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not read keychain entry for %s: status = %d",
            queryPasswordData->serverName, status);
    }
    else
    {
        /* Set up the list of attributes to read. Note: if you add additional attributes, update the size declaration
         * (above)
         */
        attributeQuery.count = 9;
        attributeQuery.tag = attributeTag;
        attributeQuery.format = attributeFormat;

        attributeQuery.tag[0] = kSecServerItemAttr;
        attributeQuery.format[0] = 0;

        attributeQuery.tag[1] = kSecSecurityDomainItemAttr;
        attributeQuery.format[1] = 0;

        attributeQuery.tag[2] = kSecAccountItemAttr;
        attributeQuery.format[2] = 0;

        attributeQuery.tag[3] = kSecPathItemAttr;
        attributeQuery.format[3] = 0;

        attributeQuery.tag[4] = kSecPortItemAttr;
        attributeQuery.format[4] = 0;

        attributeQuery.tag[5] = kSecProtocolItemAttr;
        attributeQuery.format[5] = 0;

        attributeQuery.tag[6] = kSecAuthenticationTypeItemAttr;
        attributeQuery.format[6] = 0;

        attributeQuery.tag[7] = kSecLabelItemAttr;
        attributeQuery.format[7] = 0;

        attributeQuery.tag[8] = kSecCommentItemAttr;
        attributeQuery.format[8] = 0;

        /* Read all the attributes from the keychain entry, this will fill in any data that was unknown from the query. */
        status = SecKeychainItemCopyAttributesAndData(itemRef, &attributeQuery, NULL, &attrList, &passwordLength,
            &password);

        if (status != 0)
        {
            logger_write(logger, LOGLEVEL_ERROR, "Could not read keychain attributes for %s: status = %d",
                queryPasswordData->serverName, status);
        }
        else
        {
            /* Set up the native password data structure - this is done in the same order as the arguments we queried (above). */
            completePasswordData.serverNameLength = attrList->attr[0].length;
            completePasswordData.serverName = attrList->attr[0].data;

            completePasswordData.idLength = attrList->attr[1].length;
            completePasswordData.id = attrList->attr[1].data;

            completePasswordData.accountNameLength = attrList->attr[2].length;
            completePasswordData.accountName = attrList->attr[2].data;

            completePasswordData.pathLength = attrList->attr[3].length;
            completePasswordData.path = attrList->attr[3].data;

            completePasswordData.port = *((UInt16 *) attrList->attr[4].data);

            completePasswordData.protocol = *((SecProtocolType *) attrList->attr[5].data);

            completePasswordData.authenticationType = *((SecAuthenticationType *) attrList->attr[6].data);

            completePasswordData.labelLength = attrList->attr[7].length;
            completePasswordData.label = attrList->attr[7].data;

            completePasswordData.commentLength = attrList->attr[8].length;
            completePasswordData.comment = attrList->attr[8].data;

            completePasswordData.passwordLength = passwordLength;
            completePasswordData.password = password;

            jCompletePasswordData = _getJavaPasswordDataFromNative(env, &completePasswordData);

            SecKeychainItemFreeAttributesAndData(attrList, password);

            logger_write(logger, LOGLEVEL_DEBUG, "Successfully loaded entry from keychain");
        }

        CFRelease(itemRef);
    }

    _freeNativePasswordData(env, queryPasswordData);

    /* Reset the UI in noprompt mode */
    if (jAllowUi == JNI_FALSE)
    {
        _enableKeychainUI(logger, existingAllowUi);
    }

    logger_dispose(logger);

    return jCompletePasswordData;
}


/*
 * Retrieves the (complete) keychain information for the given keychain data.
 * Returns a KeychainInternetPassword on success, or NULL on failure.
 *
 * jPasswordData: the KeychainInternetPassword object containing the password data to find in the keychain
 * jAllowUi: JNI_TRUE to allow UI to be raised, JNI_FALSE otherwise
 */
JNIEXPORT jboolean Java_com_microsoft_tfs_jni_internal_keychain_NativeKeychain_nativeRemoveInternetPassword(
    JNIEnv *env, jclass cls, jobject jPasswordData, jboolean jAllowUi)
{
    JavaVM *jvm;
     logger_t *logger = NULL;

     OSStatus status;

     SecKeychainItemRef itemRef = NULL;

     keychain_internet_password_t *passwordData;

     Boolean existingAllowUi;

     /* Setup logging */
     if ((*env)->GetJavaVM(env, &jvm) == 0)
     {
         if ((logger = logger_initialize(jvm, "com.microsoft.tfs.jni.internal.keychain.NativeKeychain"))
             == NULL)
         {
             return JNI_FALSE;
         }
     }

     if (jPasswordData == NULL)
     {
         logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: password data must not be null");
         logger_dispose(logger);

         return JNI_FALSE;
     }

     if ((passwordData = _getNativePasswordDataFromJava(env, jPasswordData)) == NULL)
     {
         logger_write(logger, LOGLEVEL_ERROR, "Could not convert java keychain data to native");
         logger_dispose(logger);

         return JNI_FALSE;
     }

     if (passwordData->serverName == NULL)
     {
         logger_write(logger, LOGLEVEL_ERROR, "serverName is required for keychain");
         logger_dispose(logger);

         return JNI_FALSE;
     }

     /* Handle -noprompt mode */
     if (jAllowUi == JNI_FALSE)
     {
         if (_disableKeychainUI(logger, &existingAllowUi) == FALSE)
         {
             logger_dispose(logger);
             return JNI_FALSE;
         }
     }

     logger_write(logger, LOGLEVEL_INFO, "Searching keychain for entry for %s", passwordData->serverName);

     /* Locate the keychain record matching the given query. */
     status = SecKeychainFindInternetPassword(NULL, /* Search all keychains */
         passwordData->serverNameLength, passwordData->serverName, passwordData->idLength,
         passwordData->id, passwordData->accountNameLength, passwordData->accountName,
         passwordData->pathLength, passwordData->path, passwordData->port, passwordData->protocol,
         passwordData->authenticationType, NULL, NULL, &itemRef);

     if (status != 0 || itemRef == NULL)
     {
         logger_write(logger, LOGLEVEL_ERROR, "Could not read keychain entry for %s: status = %d",
             passwordData->serverName, status);
     }
     else
     {
         status = SecKeychainItemDelete(itemRef);

         if(status != 0)
         {
             logger_write(logger, LOGLEVEL_WARN, "Could not remove password for %s: status = %d",
                 passwordData->serverName, status);
         }
         else
         {
             logger_write(logger, LOGLEVEL_DEBUG, "Removed password for %s", passwordData->serverName);
         }

         CFRelease(itemRef);
     }

     /* Reset the UI in noprompt mode */
     if(jAllowUi == JNI_FALSE)
     {
         _enableKeychainUI(logger, existingAllowUi);
     }

     _freeNativePasswordData(env, passwordData);

     logger_dispose(logger);

     return (status == 0) ? JNI_TRUE : JNI_FALSE;
}

jobject _getJavaPasswordDataFromNative(JNIEnv *env, keychain_internet_password_t *passwordData)
{
    jclass passwordDataClass, protocolClass, authenticationTypeClass;
    jmethodID passwordDataCtorMethod, protocolCtorMethod, authenticationTypeCtorMethod;
    jmethodID setServerNameMethod, setIDMethod, setAccountNameMethod, setPathMethod, setPortMethod, setProtocolMethod;
    jmethodID setAuthenticationTypeMethod, setPasswordMethod, setLabelMethod, setCommentMethod;
    jobject jPasswordData, jProtocolEnum, jAuthenticationTypeEnum;
    jstring jServerName, jID, jAccountName, jPath, jLabel, jComment;
    jint jPort;
    jbyteArray jPassword;
    char *serverName, *id, *accountName, *path, *label, *comment;

    /*
     * Ensure that we can load the necessary classes for our keychain data return value.
     * Note that these methods will return NULL on failure and raise an exception that will
     * be handled when we return to Java.
     */
    passwordDataClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/KeychainInternetPassword");
    protocolClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/KeychainProtocol");
    authenticationTypeClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/KeychainAuthenticationType");

    if (passwordDataClass == NULL || protocolClass == NULL || authenticationTypeClass == NULL)
    {
        free(passwordData);
        return JNI_FALSE;
    }

    /* Locate the necessary constructors and setter methods */
    passwordDataCtorMethod = (*env)->GetMethodID(env, passwordDataClass, "<init>", "()V");
    protocolCtorMethod = (*env)->GetMethodID(env, protocolClass, "<init>", "(I)V");
    authenticationTypeCtorMethod = (*env)->GetMethodID(env, authenticationTypeClass, "<init>", "(I)V");

    setServerNameMethod = (*env)->GetMethodID(env, passwordDataClass, "setServerName", "(Ljava/lang/String;)V");
    setIDMethod = (*env)->GetMethodID(env, passwordDataClass, "setID", "(Ljava/lang/String;)V");
    setAccountNameMethod = (*env)->GetMethodID(env, passwordDataClass, "setAccountName", "(Ljava/lang/String;)V");
    setPathMethod = (*env)->GetMethodID(env, passwordDataClass, "setPath", "(Ljava/lang/String;)V");
    setPortMethod = (*env)->GetMethodID(env, passwordDataClass, "setPort", "(I)V");
    setProtocolMethod = (*env)->GetMethodID(env, passwordDataClass, "setProtocol",
        "(Lcom/microsoft/tfs/jni/KeychainProtocol;)V");
    setAuthenticationTypeMethod = (*env)->GetMethodID(env, passwordDataClass, "setAuthenticationType",
        "(Lcom/microsoft/tfs/jni/KeychainAuthenticationType;)V");
    setPasswordMethod = (*env)->GetMethodID(env, passwordDataClass, "setPassword", "([B)V");
    setLabelMethod = (*env)->GetMethodID(env, passwordDataClass, "setLabel", "(Ljava/lang/String;)V");
    setCommentMethod = (*env)->GetMethodID(env, passwordDataClass, "setComment", "(Ljava/lang/String;)V");

    if (passwordDataCtorMethod == NULL || protocolCtorMethod == NULL || authenticationTypeCtorMethod == NULL
        || setServerNameMethod == NULL || setIDMethod == NULL || setAccountNameMethod == NULL || setPathMethod == NULL
        || setPortMethod == NULL || setProtocolMethod == NULL || setAuthenticationTypeMethod == NULL
        || setPasswordMethod == NULL || setLabelMethod == NULL || setCommentMethod == NULL)
    {
        free(passwordData);
        return JNI_FALSE;
    }

    jPasswordData = (*env)->NewObject(env, passwordDataClass, passwordDataCtorMethod);
    jProtocolEnum = (*env)->NewObject(env, protocolClass, protocolCtorMethod, passwordData->protocol);
    jAuthenticationTypeEnum = (*env)->NewObject(env, authenticationTypeClass, authenticationTypeCtorMethod,
        passwordData->authenticationType);

    /* We need to duplicate these strings so that they're appropriately null terminated. */
    serverName = (passwordData->serverName != NULL) ? tee_strndup(passwordData->serverName,
        passwordData->serverNameLength) : NULL;
    id = (passwordData->id != NULL) ? tee_strndup(passwordData->id, passwordData->idLength) : NULL;
    accountName = (passwordData->accountName != NULL) ? tee_strndup(passwordData->accountName,
        passwordData->accountNameLength) : NULL;
    path = (passwordData->path != NULL) ? tee_strndup(passwordData->path, passwordData->pathLength) : NULL;
    label = (passwordData->label != NULL) ? tee_strndup(passwordData->label, passwordData->labelLength) : NULL;
    comment = (passwordData->comment != NULL) ? tee_strndup(passwordData->comment, passwordData->commentLength) : NULL;

    /* Marshal the data into java types. */
    jServerName = (passwordData->serverName != NULL) ? platformCharsToJavaString(env, serverName) : NULL;
    jID = (id != NULL) ? platformCharsToJavaString(env, id) : NULL;
    jAccountName = (accountName != NULL) ? platformCharsToJavaString(env, accountName) : NULL;
    jPath = (path != NULL) ? platformCharsToJavaString(env, path) : NULL;
    jPort = passwordData->port;
    jLabel = (label != NULL) ? platformCharsToJavaString(env, label) : NULL;
    jComment = (comment != NULL) ? platformCharsToJavaString(env, comment) : NULL;

    if (passwordData->password != NULL)
    {
        jPassword = (*env)->NewByteArray(env, passwordData->passwordLength);
        (*env)->SetByteArrayRegion(env, jPassword, 0, passwordData->passwordLength, passwordData->password);
    }
    else
    {
        jPassword = NULL;
    }

    /* Configure the java object */
    (*env)->CallVoidMethod(env, jPasswordData, setServerNameMethod, jServerName);
    (*env)->CallVoidMethod(env, jPasswordData, setIDMethod, jID);
    (*env)->CallVoidMethod(env, jPasswordData, setAccountNameMethod, jAccountName);
    (*env)->CallVoidMethod(env, jPasswordData, setPathMethod, jPath);
    (*env)->CallVoidMethod(env, jPasswordData, setPortMethod, jPort);
    (*env)->CallVoidMethod(env, jPasswordData, setProtocolMethod, jProtocolEnum);
    (*env)->CallVoidMethod(env, jPasswordData, setAuthenticationTypeMethod, jAuthenticationTypeEnum);
    (*env)->CallVoidMethod(env, jPasswordData, setPasswordMethod, jPassword);
    (*env)->CallVoidMethod(env, jPasswordData, setLabelMethod, jLabel);
    (*env)->CallVoidMethod(env, jPasswordData, setCommentMethod, jComment);

    if (serverName != NULL)
    {
        free(serverName);
    }

    if (id != NULL)
    {
        free(id);
    }

    if (accountName != NULL)
    {
        free(accountName);
    }

    if (path != NULL)
    {
        free(path);
    }

    if (label != NULL)
    {
        free(label);
    }

    if (comment != NULL)
    {
        free(comment);
    }

    return jPasswordData;
}

keychain_internet_password_t *_getNativePasswordDataFromJava(JNIEnv *env, jobject jPasswordData)
{
    jclass passwordDataClass, protocolClass, authenticationTypeClass;
    jmethodID getServerNameMethod, getIDMethod, getAccountNameMethod, getPathMethod, getPortMethod, getProtocolMethod;
    jmethodID getAuthenticationTypeMethod, getPasswordMethod, getLabelMethod, getCommentMethod;
    jmethodID getProtocolValueMethod, getAuthenticationTypeValueMethod;
    jobject jProtocolEnum, jAuthenticationTypeEnum;

    keychain_internet_password_t *passwordData;

    if ((passwordData = (keychain_internet_password_t *) malloc(sizeof(keychain_internet_password_t))) == NULL)
    {
        return NULL;
    }

    /*
     * Ensure that we can load the necessary classes for our keychain data return value.
     * Note that these methods will return NULL on failure and raise an exception that will
     * be handled when we return to Java.
     */
    passwordDataClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/KeychainInternetPassword");
    protocolClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/KeychainProtocol");
    authenticationTypeClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/KeychainAuthenticationType");

    if (passwordDataClass == NULL || protocolClass == NULL || authenticationTypeClass == NULL)
    {
        free(passwordData);
        return JNI_FALSE;
    }

    /* Locate the necessary getter methods */
    getServerNameMethod = (*env)->GetMethodID(env, passwordDataClass, "getServerName", "()Ljava/lang/String;");
    getIDMethod = (*env)->GetMethodID(env, passwordDataClass, "getID", "()Ljava/lang/String;");
    getAccountNameMethod = (*env)->GetMethodID(env, passwordDataClass, "getAccountName", "()Ljava/lang/String;");
    getPathMethod = (*env)->GetMethodID(env, passwordDataClass, "getPath", "()Ljava/lang/String;");
    getPortMethod = (*env)->GetMethodID(env, passwordDataClass, "getPort", "()I");
    getProtocolMethod = (*env)->GetMethodID(env, passwordDataClass, "getProtocol",
        "()Lcom/microsoft/tfs/jni/KeychainProtocol;");
    getAuthenticationTypeMethod = (*env)->GetMethodID(env, passwordDataClass, "getAuthenticationType",
        "()Lcom/microsoft/tfs/jni/KeychainAuthenticationType;");
    getPasswordMethod = (*env)->GetMethodID(env, passwordDataClass, "getPassword", "()[B");
    getLabelMethod = (*env)->GetMethodID(env, passwordDataClass, "getLabel", "()Ljava/lang/String;");
    getCommentMethod = (*env)->GetMethodID(env, passwordDataClass, "getComment", "()Ljava/lang/String;");

    getProtocolValueMethod = (*env)->GetMethodID(env, protocolClass, "getValue", "()I");
    getAuthenticationTypeValueMethod = (*env)->GetMethodID(env, authenticationTypeClass, "getValue", "()I");

    if (getServerNameMethod == NULL || getIDMethod == NULL || getAccountNameMethod == NULL || getPathMethod == NULL
        || getPortMethod == NULL || getProtocolMethod == NULL || getAuthenticationTypeMethod == NULL
        || getPasswordMethod == NULL || getLabelMethod == NULL || getCommentMethod == NULL || getProtocolValueMethod
        == NULL || getAuthenticationTypeValueMethod == NULL)
    {
        free(passwordData);
        return JNI_FALSE;
    }

    /* Get the data out of the keychain internet password data */
    passwordData->jServerName = (jstring)(*env)->CallObjectMethod(env, jPasswordData, getServerNameMethod);
    passwordData->jID = (jstring)(*env)->CallObjectMethod(env, jPasswordData, getIDMethod);
    passwordData->jAccountName = (jstring)(*env)->CallObjectMethod(env, jPasswordData, getAccountNameMethod);
    passwordData->jPath = (jstring)(*env)->CallObjectMethod(env, jPasswordData, getPathMethod);
    passwordData->jPort = (*env)->CallIntMethod(env, jPasswordData, getPortMethod);

    jProtocolEnum = (*env)->CallObjectMethod(env, jPasswordData, getProtocolMethod);
    passwordData->jProtocol = (*env)->CallIntMethod(env, jProtocolEnum, getProtocolValueMethod);

    jAuthenticationTypeEnum = (jstring)(*env)->CallObjectMethod(env, jPasswordData, getAuthenticationTypeMethod);
    passwordData->jAuthenticationType = (*env)->CallIntMethod(env, jAuthenticationTypeEnum,
        getAuthenticationTypeValueMethod);

    passwordData->jPassword = (*env)->CallObjectMethod(env, jPasswordData, getPasswordMethod);
    passwordData->jLabel = (jstring)(*env)->CallObjectMethod(env, jPasswordData, getLabelMethod);
    passwordData->jComment = (jstring)(*env)->CallObjectMethod(env, jPasswordData, getCommentMethod);

    /* Marshal the data to C types */
    passwordData->serverName = (passwordData->jServerName != NULL) ? javaStringToPlatformChars(env,
        passwordData->jServerName) : NULL;
    passwordData->serverNameLength = (passwordData->jServerName != NULL) ? strlen(passwordData->serverName) : 0;

    passwordData->id = (passwordData->jID != NULL) ? javaStringToPlatformChars(env, passwordData->jID) : NULL;
    passwordData->idLength = (passwordData->jID != NULL) ? strlen(passwordData->id) : 0;

    passwordData->accountName = (passwordData->jAccountName != NULL) ? javaStringToPlatformChars(env,
        passwordData->jAccountName) : NULL;
    passwordData->accountNameLength = (passwordData->jAccountName != NULL) ? strlen(passwordData->accountName) : 0;

    passwordData->path = (passwordData->jPath != NULL) ? javaStringToPlatformChars(env, passwordData->jPath) : NULL;
    passwordData->pathLength = (passwordData->jPath != NULL) ? strlen(passwordData->path) : 0;

    passwordData->port = (passwordData->jPort >= 0) ? passwordData->jPort : 0;

    passwordData->protocol = passwordData->jProtocol;

    passwordData->authenticationType = passwordData->jAuthenticationType;

    if (passwordData->jPassword != NULL)
    {
        passwordData->passwordLength = (*env)->GetArrayLength(env, passwordData->jPassword);

        if (passwordData->passwordLength > 0)
        {
            if ((passwordData->password = (void *) malloc(passwordData->passwordLength)) == NULL)
            {
                free(passwordData);
                return JNI_FALSE;
            }

            (*env)->GetByteArrayRegion(env, passwordData->jPassword, 0, passwordData->passwordLength,
                passwordData->password);
        }
        else
        {
            passwordData->password = NULL;
        }
    }
    else
    {
        passwordData->passwordLength = 0;
        passwordData->password = NULL;
    }

    passwordData->label = (passwordData->jLabel != NULL) ? javaStringToPlatformChars(env, passwordData->jLabel) : NULL;
    passwordData->labelLength = (passwordData->jLabel != NULL) ? strlen(passwordData->label) : 0;

    passwordData->comment = (passwordData->jComment != NULL) ? javaStringToPlatformChars(env, passwordData->jComment)
        : NULL;
    passwordData->commentLength = (passwordData->jComment != NULL) ? strlen(passwordData->comment) : 0;

    return passwordData;
}

void _freeNativePasswordData(JNIEnv *env, keychain_internet_password_t *passwordData)
{
    if (passwordData->jServerName != NULL)
    {
        releasePlatformChars(env, passwordData->jServerName, passwordData->serverName);
    }

    if (passwordData->jID != NULL)
    {
        releasePlatformChars(env, passwordData->jID, passwordData->id);
    }

    if (passwordData->jAccountName != NULL)
    {
        releasePlatformChars(env, passwordData->jAccountName, passwordData->accountName);
    }

    if (passwordData->jPath != NULL)
    {
        releasePlatformChars(env, passwordData->jPath, passwordData->path);
    }

    if (passwordData->password != NULL)
    {
        free(passwordData->password);
    }

    if (passwordData->jLabel != NULL)
    {
        releasePlatformChars(env, passwordData->jLabel, passwordData->label);
    }

    if (passwordData->jComment != NULL)
    {
        releasePlatformChars(env, passwordData->jComment, passwordData->comment);
    }

    free(passwordData);
}

/*
 * Disables the keychain user interface.  Will log error messages to the
 * provided logger (must not be null.)  Sets existingAllowUi based on the
 * existing value of the global keychain UI state, for subsequent calls
 * to _enableKeychainUI.  Returns TRUE on success, FALSE on failure.
 */
Boolean _disableKeychainUI(logger_t *logger, Boolean *existingAllowUi)
{
    OSStatus status;

    if (logger == NULL || existingAllowUi == NULL)
    {
        return FALSE;
    }

    /*
     * If we are not to display ui (ie, "-noprompt" mode), then query
     * the current keychain UI status.  Note that this is NOT
     * per-process, so we need to respect this global state.
     * (Also note that another process could change this value in between
     * our calls.  Meh.)
     */
    status = SecKeychainGetUserInteractionAllowed(existingAllowUi);

    if (status != 0)
    {
        logger_write(logger, LOGLEVEL_ERROR,
            "Could not query Keychain user interaction, will not use keychain for password management");

        return FALSE;
    }
    else if (*existingAllowUi == TRUE)
    {
        logger_write(logger, LOGLEVEL_INFO, "Disabling keychain user interface");

        status = SecKeychainSetUserInteractionAllowed(FALSE);

        if (status != 0)
        {
            logger_write(logger, LOGLEVEL_ERROR,
                "Could not disable Keychain user interaction, will not use keychain for password management");

            return FALSE;
        }
    }
    else
    {
        logger_write(logger, LOGLEVEL_DEBUG, "Keychain user interface already disabled");
    }

    return TRUE;
}

/*
 * Enables the keychain user interface (if it was previously enabled, as
 * determined by the value of existingAllowUi.)  Returns TRUE on success,
 * FALSE on failure.
 */
Boolean _enableKeychainUI(logger_t *logger, Boolean existingAllowUi)
{
    OSStatus status;

    if (logger == NULL)
    {
        return FALSE;
    }

    if (existingAllowUi == FALSE)
    {
        return TRUE;
    }

    status = SecKeychainSetUserInteractionAllowed(TRUE);

    if (status != 0)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not enable Keychain user interaction");

        return FALSE;
    }

    logger_write(logger, LOGLEVEL_INFO, "Keychain user interaction enabled");

    return TRUE;
}
