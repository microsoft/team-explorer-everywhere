/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI for generic native authentication routines.  These handle data
 * conversion between Java and native types, then call the platform-specific
 * authentication routines linked into the library.  See auth_gss or auth_sspi
 * for more information.
 */

#include <stdlib.h>
#include <string.h>

#include "native_auth.h"
#include "util.h"
#include "auth.h"
#include "logger.h"

JNIEXPORT jlong JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthConfigure(JNIEnv *env,
    jclass class)
{
    JavaVM *jvm;
    logger_t *logger = NULL;
    auth_configuration_t *configuration;

    if ((*env)->GetJavaVM(env, &jvm) == 0)
        logger = logger_initialize(jvm, "com.microsoft.tfs.jni.natives.NativeAuthMethods");

    if ((configuration = auth_configure(logger)) == NULL)
        return 0;

    return ptrToJlong(configuration);
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthAvailable(JNIEnv *env,
    jclass class, jlong configurationId, jshort mechanism)
{
    auth_configuration_t *configuration;
    int available;

    if (configurationId == 0)
        return JNI_FALSE;

    if ((configuration = jlongToPtr(configurationId)) == NULL)
		return JNI_FALSE;
	
    available = auth_available(configuration, mechanism);

    return (available == 1) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthSupportsCredentialsDefault(
    JNIEnv *env, jclass class, jlong configurationId, jshort mechanism)
{
    auth_configuration_t *configuration;
    int supported;

    if (configurationId == 0)
        return JNI_FALSE;

    if ((configuration = jlongToPtr(configurationId)) == NULL)
		return JNI_FALSE;

    supported = auth_supports_credentials_default(configuration, mechanism);

    return (supported == 1) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthSupportsCredentialsSpecified(
    JNIEnv *env, jclass class, jlong configurationId, jshort mechanism)
{
    auth_configuration_t *configuration;
    int supported;

    if (configurationId == 0)
        return JNI_FALSE;

    if ((configuration = jlongToPtr(configurationId)) == NULL)
		return JNI_FALSE;

    supported = auth_supports_credentials_specified(configuration, mechanism);

    return (supported == 1) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthGetCredentialsDefault(
    JNIEnv *env, jclass class, jlong configurationId, jshort mechanism)
{
    platform_char *userdomain;

    auth_configuration_t *configuration;
    jstring juserdomain;

    if (configurationId == 0)
        return NULL;

    if ((configuration = jlongToPtr(configurationId)) == NULL)
		return NULL;

    userdomain = auth_get_credentials_default(configuration, mechanism);

    if (userdomain == NULL)
        return NULL;

    juserdomain = platformCharsToJavaString(env, userdomain);

    free(userdomain);

    return juserdomain;
}

JNIEXPORT jlong JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthInitialize(JNIEnv *env,
    jclass class, jlong configurationId, jshort mechanism)
{
    auth_configuration_t *configuration;
    auth_t *auth;

    if (configurationId == 0)
        return 0;

    if ((configuration = jlongToPtr(configurationId)) == NULL)
		return 0;

    if ((auth = auth_initialize(configuration, mechanism)) == NULL)
		return 0;

    return ptrToJlong(auth);
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthSetTarget
(JNIEnv *env, jclass class, jlong negotiateId, jstring jTarget)
{
    auth_t *auth;
    const platform_char *target;

    if (negotiateId == 0)
		return;

    if ((auth = jlongToPtr(negotiateId)) == NULL)
		return;

    target = (jTarget != NULL) ? javaStringToPlatformChars(env, jTarget) : NULL;

    auth_set_target(auth, target);

    if(target != NULL)
		releasePlatformChars(env, jTarget, target);
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthSetLocalhost
(JNIEnv *env, jclass class, jlong negotiateId, jstring jLocalhost)
{
    auth_t *auth;
    const platform_char *localhost;

    if (negotiateId == 0)
	    return;

	if ((auth = jlongToPtr(negotiateId)) == NULL)
		return;

    localhost = (jLocalhost != NULL) ? javaStringToPlatformChars(env, jLocalhost) : NULL;

    auth_set_localhost(auth, localhost);

    if(localhost != NULL)
    releasePlatformChars(env, jLocalhost, localhost);
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthSetCredentialsDefault
(JNIEnv *env, jclass class, jlong negotiateId)
{
    auth_t *auth;

    if (negotiateId == 0)
		return;

    if ((auth = jlongToPtr(negotiateId)) == NULL)
		return;

    auth_set_credentials_default(auth);
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthSetCredentialsSpecified
(JNIEnv *env, jclass class, jlong negotiateId, jstring jUsername, jstring jDomain, jstring jPassword)
{
    auth_t *auth;
    const platform_char *username, *domain, *password;

    if (negotiateId == 0)
		return;

    if ((auth = jlongToPtr(negotiateId)) == NULL)
		return;

    username = (jUsername != NULL) ? javaStringToPlatformChars(env, jUsername) : NULL;
    domain = (jDomain != NULL) ? javaStringToPlatformChars(env, jDomain) : NULL;
    password = (jPassword != NULL) ? javaStringToPlatformChars(env, jPassword) : NULL;

    auth_set_credentials(auth, username, domain, password);

    if(username != NULL)
		releasePlatformChars(env, jUsername, username);

    if(domain != NULL)
		releasePlatformChars(env, jDomain, domain);
	
    if(password != NULL)
		releasePlatformChars(env, jPassword, password);
}

JNIEXPORT jbyteArray JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthGetToken(JNIEnv *env,
    jclass class, jlong negotiateId, jbyteArray jInputToken)
{
    auth_t *auth;
    jbyteArray jOutputToken;

    void *inputToken = NULL;
    unsigned int inputTokenLen;

    void *outputToken = NULL;
    unsigned int outputTokenLen;

    if (negotiateId == 0)
        return NULL;

    if ((auth = jlongToPtr(negotiateId)) == NULL)
		return NULL;

    if (jInputToken == NULL)
        inputTokenLen = 0;
    else
        inputTokenLen = (*env)->GetArrayLength(env, jInputToken);

    if (inputTokenLen > 0)
    {
        if ((inputToken = (char *) malloc(inputTokenLen)) == NULL)
            return NULL;

        (*env)->GetByteArrayRegion(env, jInputToken, 0, inputTokenLen, inputToken);
    }

    if (auth_get_token(auth, inputToken, inputTokenLen, &outputToken, &outputTokenLen) == 0)
    {
        if (inputToken != NULL)
            free(inputToken);

        return NULL;
    }

    jOutputToken = (*env)->NewByteArray(env, outputTokenLen);

    (*env)->SetByteArrayRegion(env, jOutputToken, 0, outputTokenLen, outputToken);

    if (inputToken != NULL)
        free(inputToken);

    free(outputToken);

    return jOutputToken;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthIsComplete(JNIEnv *env,
    jclass class, jlong negotiateId)
{
    auth_t *auth;
    int complete;

    if (negotiateId == 0)
        return JNI_TRUE;

    if ((auth = jlongToPtr(negotiateId)) == NULL)
		return JNI_FALSE;

    complete = auth_is_complete(auth);

    return (complete == 1) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthGetErrorMessage(JNIEnv *env,
    jclass class, jlong negotiateId)
{
    auth_t *auth;
    const char *error_message;

    if (negotiateId == 0)
        return NULL;

    if ((auth = jlongToPtr(negotiateId)) == NULL)
		return NULL;

    error_message = auth_get_error(auth);

    if (error_message == NULL)
        return NULL;

    // Always a single-byte wide string
    return (*env)->NewStringUTF(env, error_message);
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_auth_NativeAuth_nativeAuthDispose
(JNIEnv *env, jclass class, jlong negotiateId)
{
    auth_t *auth;

    if (negotiateId == 0)
		return;

    if ((auth = jlongToPtr(negotiateId)) == NULL)
		return;

    auth_dispose(auth);
}
