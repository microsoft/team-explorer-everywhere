/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#include <stdio.h>
#include <windows.h>
#include <jni.h>
#include <wincred.h>
#include <string.h>
#include "util.h"
#include "logger.h"

JNIEXPORT jobject JNICALL Java_com_microsoft_tfs_jni_internal_wincredential_NativeWinCredential_nativeFindCredential(JNIEnv *env, jclass cls, jstring jlocation)
{
	jclass winCredClass;
	jmethodID ctorMethod, setServerNameMethod, setAccountNameMethod, setPasswordMethod;
	jstring jAccountName, jPassword;
	PCREDENTIAL pcred;
	WCHAR *accountName;
	WCHAR *password;
	const WCHAR *location;
	jobject jCredData = NULL;

	if((location = javaStringToPlatformChars(env, jlocation)) == NULL)
	{
		return NULL;
	}

	if (CredRead(location, CRED_TYPE_GENERIC, 0, &pcred))
	{
		/*
		 * Ensure that we can load the necessary classes from Java.
		 * Note that these methods will return NULL on failure and raise an exception that will
		 * be handled when we return to Java.
		 */
		winCredClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/WinCredential");
		if (winCredClass == NULL)
		{
			return NULL;
		}

		/* Locate the necessary constructors and setter methods */
		ctorMethod = (*env)->GetMethodID(env, winCredClass, "<init>", "()V");
		setServerNameMethod = (*env)->GetMethodID(env, winCredClass, "setServerUri", "(Ljava/lang/String;)V");
		setAccountNameMethod = (*env)->GetMethodID(env, winCredClass, "setAccountName", "(Ljava/lang/String;)V");
		setPasswordMethod = (*env)->GetMethodID(env, winCredClass, "setPassword", "(Ljava/lang/String;)V");

		if (ctorMethod == NULL || setServerNameMethod == NULL || setAccountNameMethod == NULL || setPasswordMethod == NULL)
		{
			return NULL;
		}

		jCredData = (*env)->NewObject(env, winCredClass, ctorMethod);

		accountName = (WCHAR *)malloc(sizeof(WCHAR) * (wcslen(pcred->UserName) + 1));
		password = (WCHAR *)malloc(pcred->CredentialBlobSize + sizeof(WCHAR));
		wcsncpy_s(accountName, wcslen(pcred->UserName) + 1, pcred->UserName, wcslen(pcred->UserName));
		wcsncpy_s(password, pcred->CredentialBlobSize / sizeof(WCHAR) + 1, (WCHAR*)pcred->CredentialBlob, pcred->CredentialBlobSize / sizeof(WCHAR));

		/* Marshal the data into java types. */
		jAccountName = (accountName != NULL) ? platformCharsToJavaString(env, accountName) : NULL;
		jPassword = (password != NULL) ? platformCharsToJavaString(env, password) : NULL;

		/* Configure the java object */
		(*env)->CallVoidMethod(env, jCredData, setServerNameMethod, jlocation);
		(*env)->CallVoidMethod(env, jCredData, setAccountNameMethod, jAccountName);
		(*env)->CallVoidMethod(env, jCredData, setPasswordMethod, jPassword);

		free(accountName);
		free(password);

		releasePlatformChars(env, jlocation, location);
		CredFree(pcred);
		return jCredData;
	}
	else
	{
		releasePlatformChars(env, jlocation, location);
		return NULL;
	}
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_wincredential_NativeWinCredential_nativeStoreCredential(JNIEnv *env, jclass cls, jstring jlocation, jstring jusername, jstring jpwd)
{
	const WCHAR *c_location;
	const WCHAR *c_username;
	const WCHAR *password;
	WCHAR *location;
	WCHAR *username;
	jboolean result;
	CREDENTIAL cred;

	c_location = javaStringToPlatformChars(env, jlocation);
	location = (WCHAR *) malloc(sizeof(WCHAR) * (wcslen(c_location) + 1));
	wcsncpy_s(location, wcslen(c_location)+1, c_location, wcslen(c_location));

	c_username = javaStringToPlatformChars(env, jusername);
	username = (WCHAR *) malloc(sizeof(WCHAR) * (wcslen(c_username) + 1));
	wcsncpy_s(username, wcslen(c_username) + 1, c_username, wcslen(c_username));

	password = javaStringToPlatformChars(env, jpwd);

	cred.Flags = 0;
	cred.Type = CRED_TYPE_GENERIC;
	cred.TargetName = location;
	cred.Comment = NULL;
	cred.LastWritten.dwLowDateTime = 0;
	cred.LastWritten.dwHighDateTime = 0;
	cred.CredentialBlobSize = (DWORD)(sizeof(WCHAR) * (wcslen(password)));
	cred.CredentialBlob = (LPBYTE)password;
	cred.Persist = CRED_PERSIST_LOCAL_MACHINE;
	cred.AttributeCount = 0;
	cred.Attributes = NULL;
	cred.TargetAlias = NULL;
	cred.UserName = username;

	if (CredWrite(&cred, 0))
	{
		result = JNI_TRUE;
	}
	else
	{
		result = JNI_FALSE;
	}

	free(location);
	free(username);
	releasePlatformChars(env, jlocation, c_location);
	releasePlatformChars(env, jusername, c_username);
	releasePlatformChars(env, jpwd, password);
	return result;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_wincredential_NativeWinCredential_nativeEraseCredential(JNIEnv *env, jclass cls, jstring jlocation)
{
	jboolean result;
	const WCHAR *c_location;

	c_location = javaStringToPlatformChars(env, jlocation);

	if(CredDelete(c_location, CRED_TYPE_GENERIC, 0))
	{
		result = JNI_TRUE;
	}
	else
	{
		result = JNI_FALSE;
	}

	releasePlatformChars(env, jlocation, c_location);
	return result;
}
