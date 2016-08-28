/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that access the Windows Registry.
 */

#include <stdio.h>
#include <windows.h>
#include <jni.h>

#include "tee_sal.h"
#include "native_registry.h"
#include "util.h"
#include "logger.h"

#define HKEY_ERROR ((HKEY)0)
#define KEY_NAME_MAXSIZE 256
#define VALUE_NAME_MAXSIZE 16383
#define VALUE_MAXSIZE 32768

/*
 * This function is only used internally.
 *
 * Maps a pre-defined constant to a predefined Windows root key.
 * The constant values defined in the Java layer must stay in sync
 * with the values defined in this function.
 *
 * rootKey: the constant identifier for the Windows root key.
 */
HKEY _getRootKey(jint rootKeyID)
{
    if (rootKeyID == 3)
    {
        return HKEY_CLASSES_ROOT;
    }
    else if (rootKeyID == 2)
    {
        return HKEY_LOCAL_MACHINE;
    }
    else
    {
        return HKEY_CURRENT_USER;
    }
}

/*
 * This function is only used internally.
 *
 * Open a Windows registry key given a root key id and a subkey path.
 * A sample path is "Software\Microsoft".
 *
 * rootKeyID: the constant identifier for the Windows root key.
 * path: the full path from the root to a key in the registry hierarchy.
 * samDesired: A mask that defines the access rights to the key (see WIN32 API).
 */
HKEY _openKey(_In_ JNIEnv *env, jint rootKeyID, _In_ jstring path, REGSAM samDesired)
{
    HKEY rootKey = _getRootKey(rootKeyID);
	const WCHAR *subKeyName;
    HKEY subKey;
	long result;

	if ((subKeyName = javaStringToPlatformChars(env, path)) == NULL)
	{
		// String allocation failed, exception already thrown
		return HKEY_ERROR;
	}

    result = RegOpenKeyExW(rootKey, subKeyName, 0, samDesired, &subKey);

    releasePlatformChars(env, path, subKeyName);

    if (result != ERROR_SUCCESS)
    {
        return HKEY_ERROR;
    }
    else
    {
        return subKey;
    }
}

/*
 * This function is only used internally.
 *
 * Closes the specified registry key.
 *
 * key: An open registry key.
 */
void _closeKey(HKEY key)
{
    RegCloseKey(key);
}

/*
 * This function is only used internally.
 *
 * Create a Java object of type RegistryValue and returns it as a jobject.
 */
_Ret_maybenull_ jobject _createRegistryValue(_In_ JNIEnv *env, _In_ jstring jValueName, _In_ unsigned char *data, int dataSize, DWORD actualType)
{
    // Find the RegistryValue class which is the return type.
    jclass valueClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/RegistryValue");

    if (valueClass == NULL)
    {
        return NULL;
    }

    if (actualType == REG_DWORD && dataSize >= sizeof(DWORD))
    {
        // Get the RegistryValue(String, int) constructor.
        jmethodID valueCtorMethod = (*env)->GetMethodID(env, valueClass, "<init>", "(Ljava/lang/String;I)V");
        jint value;

        if (valueCtorMethod == NULL)
        {
            return NULL;
        }

        // Create the java integer from the registry bytes.
        value = data[3] << 24;
        value += data[2] << 16;
        value += data[1] << 8;
        value += data[0];

        // Create the RegistryValue instance.
        return (*env)->NewObject(env, valueClass, valueCtorMethod, jValueName, value);
    }
    else if (actualType == REG_SZ)
    {
        // Get the Registry(String, String) constructor.
        jmethodID valueCtorMethod = (*env)->GetMethodID(env, valueClass, "<init>",
            "(Ljava/lang/String;Ljava/lang/String;)V");
        jstring value;

        if (valueCtorMethod == NULL)
        {
            return NULL;
        }

        // Create the java string from the registry bytes.
        value = (*env)->NewString(env, (jchar *) data, (dataSize / 2) - 1);

        // Create the RegistryValue instance.
        return (*env)->NewObject(env, valueClass, valueCtorMethod, jValueName, value);
    }
    else
    {
        return NULL;
    }
}

/*
 * This function is only used internally.
 *
 * Gets a WIN32 subkey for the specified subkey name.  The parent key
 * is already an open WIN32 subkey.
 *
 * key: An open registry key.
 * jSubKeyName: The name of the subkey to be opened.
 * samDesired: A mask that defines the access rights to the key (see WIN32 API).
 */
HKEY _getSubKey(_In_ JNIEnv *env, HKEY key, _In_ jstring jSubKeyName, REGSAM samDesired)
{
    const WCHAR *subKeyName;
    HKEY subKey;
	long result;

	if ((subKeyName = javaStringToPlatformChars(env, jSubKeyName)) == NULL)
	{
		// String allocation failed, exception already thrown
		return HKEY_ERROR;
	}

    result = RegOpenKeyExW(key, subKeyName, 0, samDesired, &subKey);

    releasePlatformChars(env, jSubKeyName, subKeyName);

    if (result != ERROR_SUCCESS)
    {
        return HKEY_ERROR;
    }

    return subKey;
}

/*
 * This function is only used internally.
 *
 * Gets the data for the specified subkey value.  The parent key
 * is already an open WIN32 subkey.
 *
 * key: An open registry key.
 * jSubKeyName: The name of the subkey to be opened.
 * jValueName: The name of the value to be retrieved.
 * types: A pointer to an array of DWORDs that specifies the desired value 
 *			types that can be returned.  These are the REG_* enumerations 
 *			(REG_DWORD, REG_SZ, etc.), not the RRF_RT_* flags.  Pass NULL 
 *			to match any value type.
 * typesCount: A count of the number of DWORDs in the types array if types is non-NULL.
 */
_Ret_maybenull_ jobject _getValue(_In_ JNIEnv *env, HKEY key, _In_ jstring jSubKeyName, _In_ jstring jValueName, 
	_In_opt_count_(typesCount) DWORD *types, size_t typesCount)
{
	HKEY subKey = NULL;
    DWORD actualType;
    DWORD size = VALUE_MAXSIZE;
    unsigned char *buffer;
    const WCHAR *valueName;
    long result;
    jobject ret;

    // Get the value from the registry.  RegGetValue would be a great function
	// to use here, but it's not available on Windows XP.  Use RegQueryValueEx
	// instead, which requires opening the subkey as a first step and can't
	// match by type.

	// Open the subkey that contains the value.
    subKey = _getSubKey(env, key, jSubKeyName, KEY_READ);
	
    if (subKey == HKEY_ERROR)
    {
        return NULL;
    }

    buffer = (unsigned char *) malloc(VALUE_MAXSIZE);
    if (buffer == NULL)
    {
        return NULL;
    }

    // Convert from jstring.
    if ((valueName = javaStringToPlatformChars(env, jValueName)) == NULL)
	{
		// String allocation failed, exception already thrown
		_closeKey(subKey);
		free(buffer);
		return NULL;
	}

	result = RegQueryValueEx(subKey, valueName, NULL, &actualType, buffer, &size);

	// Release the memory held by the names.
    releasePlatformChars(env, jValueName, valueName);

	// Done with the subkey.
	_closeKey(subKey);

    // Bail on failure.
    if (result != ERROR_SUCCESS)
    {
        free(buffer);
        return NULL;
    }

	// Ensure the actual type matches what was desired.
	if (types != NULL)
	{
		BOOL matchedType = FALSE;
		size_t typesIndex;

		for (typesIndex = 0; typesIndex < typesCount; typesIndex++)
		{
			if (types[typesIndex] == actualType)
			{
				matchedType = TRUE;
				break;
			}
		}

		if (matchedType == FALSE)
		{
			free(buffer);
			return NULL;
		}
	}

	// Documentation for RegQueryValueEx says REG_SZ strings may not come back 
	// null-terminated, but _createRegistryValue handles this fine.
    ret = _createRegistryValue(env, jValueName, buffer, size, actualType);

    free(buffer);
    return ret;
}

/*
 * This function is only used internally.
 *
 * Sets the value in a WIN32 subkey.  The parent key is already an open WIN32 subkey.
 * The value overwrites an existing value of the same type, if it already exists, or
 * create a new value of the specified type.
 *
 * key: An open registry key.
 * jValueName: The name of the value to set.
 * type: The type of the value to set (e.g. REG_DWORD, REG_SZ).
 * data: The bytes to set.
 * dataSize: The number of bytes to set.
 */
jboolean _setValue(_In_ JNIEnv *env, HKEY key, _In_ jstring jValueName, DWORD type, _In_ PVOID data, jint dataSize)
{
    const WCHAR *valueName;
	long result;

	if ((valueName = javaStringToPlatformChars(env, jValueName)) == NULL)
	{
		// String allocation failed, exception already thrown
		return FALSE;
	}

    result = RegSetValueEx(key, valueName, 0, type, data, dataSize);
    releasePlatformChars(env, jValueName, valueName);

    if (result == ERROR_SUCCESS)
    {
        return TRUE;
    }
    else
    {
        return FALSE;
    }
}

/*
 * This function is only used internally.
 *
 * Sets an exception in the JNI environment, which will be raised once JNI returns
 * to Java.
 *
 * errorCode: The Windows Registry error code.
 */
void _throwRegistryException(_In_ JNIEnv *env, DWORD errorCode)
{
    jclass exceptionClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/RegistryException");

    if (exceptionClass != NULL)
    {
        /*
         * Exception messages ARE NOT UTF-16 strings, they're ASCII strings.  Use FormatMessageA
         * instead of unicode methods.
         */

        DWORD dwFlags = FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM;
        char *errorMessage;

        if (FormatMessageA(dwFlags, NULL, errorCode, 0, (LPSTR) & errorMessage, 0, NULL) == 0)
        {
            char buffer[256];
            sprintf_s(buffer, 256, "Registry error code: %d", errorCode);
            (*env)->ThrowNew(env, exceptionClass, buffer);
        }
        else
        {
            (*env)->ThrowNew(env, exceptionClass, errorMessage);
            LocalFree(errorMessage);
        }
    }
}

/*
 * API: nativeCreate
 *
 * Ensures the specified path exists under the specified root.
 */
JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeCreate(
    JNIEnv *env,
    jclass cls,
    jint rootKeyID,
    jstring jSubkeyName)
{
    const WCHAR *subkeyName;
    long result;
    HKEY subkey;

    HKEY key = _getRootKey(rootKeyID);

    // Create the new subkey (is opened if it already exists).
    if ((subkeyName = javaStringToPlatformChars(env, jSubkeyName)) == NULL)
	{
		// String allocation failed, exception already thrown
		return;
	}

    result = RegCreateKeyExW(key, subkeyName, 0, NULL, 0, KEY_WRITE, NULL, &subkey, NULL);
    releasePlatformChars(env, jSubkeyName, subkeyName);

    if (result == ERROR_SUCCESS)
    {
        _closeKey(subkey);
    }
    else
    {
        _throwRegistryException(env, result);
    }
}

/*
 * API: nativeExists
 *
 * Returns true if the specified root and path exist, otherwise false.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeExists(JNIEnv *env, jclass cls, jint rootKeyID,
    jstring path)
{
    HKEY key = _openKey(env, rootKeyID, path, KEY_READ);

    if (key != HKEY_ERROR)
    {
        _closeKey(key);
        return JNI_TRUE;
    }
    else
    {
        return JNI_FALSE;
    }
}

/*
 * API: nativeCreateSubkey
 *
 * Creates a new subkey or opens a subkey if it already exists.  Returns
 * TRUE if the subkey is created or already exists.  Otherwise, returns FALSE.
 * The Java layer will allocate a new RegistryKey on success.
 *
 * rootKeyID: A root key identifier.
 * path: The path of the subkey in which the new subkey is created.
 * jSubkeyName: The name of the new subkey.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeCreateSubkey(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path, jstring jSubkeyName)
{
    const WCHAR *subkeyName;
    long result;
    jboolean ret;
    HKEY subkey;

    // Open the parent subkey.
    HKEY key = _openKey(env, rootKeyID, path, KEY_WRITE);

    if (key == HKEY_ERROR)
    {
        return JNI_FALSE;
    }

    // Create the new subkey (is opened if it already exists).
    if ((subkeyName = javaStringToPlatformChars(env, jSubkeyName)) == NULL)
	{
		// String allocation failed, exception already thrown
		_closeKey(key);
		return JNI_FALSE;
	}

    result = RegCreateKeyExW(key, subkeyName, 0, NULL, 0, KEY_WRITE, NULL, &subkey, NULL);
    releasePlatformChars(env, jSubkeyName, subkeyName);

    if (result == ERROR_SUCCESS)
    {
        _closeKey(subkey);
        ret = JNI_TRUE;
    }
    else
    {
        ret = JNI_FALSE;
    }

    _closeKey(key);
    return ret;
}

/*
 * API: nativeDeleteSubkey
 *
 * Deletes the subkey references by the given name.  Returns TRUE if the subkey
 * is deleted.  Otherwise, returns FALSE.
 *
 * rootKeyID: A root key identifier.
 * path: The path of the subkey in which the subkey will be deleted.
 * jSubkeyName: The name of the subkey to delete.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeDeleteSubkey(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path, jstring jSubkeyName)
{
    const WCHAR *subkeyName;
    long result;
    jboolean ret;

    // Open the parent subkey.
    HKEY parentKey = _openKey(env, rootKeyID, path, KEY_WRITE);

    if (parentKey == HKEY_ERROR)
    {
        return JNI_FALSE;
    }

    // Delete the subkey (is opened if it already exists).
    if ((subkeyName = javaStringToPlatformChars(env, jSubkeyName)) == NULL)
	{
		// String allocation failed, exception already thrown
		_closeKey(parentKey);
		return JNI_FALSE;
	}

    result = RegDeleteKeyW(parentKey, subkeyName);
    releasePlatformChars(env, jSubkeyName, subkeyName);

    if (result == ERROR_SUCCESS)
    {
        ret = JNI_TRUE;
    }
    else
    {
        ret = JNI_FALSE;
    }

    _closeKey(parentKey);
    return ret;
}

/*
 * API: nativeGetValue
 *
 * Returns a RegistryValue object containing the type of actual value
 * of the registry value being retrieved.
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey containing the value to be retrieved.
 * valueName: The name of the value to be retrieved.
 */
JNIEXPORT jobject JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeGetValue(JNIEnv *env, jclass cls, jint rootKeyID,
    jstring path, jstring valueName)
{
	HKEY key;
	DWORD types[2];
	types[0] = REG_DWORD;
	types[1] = REG_SZ;

    key = _getRootKey(rootKeyID);
    return _getValue(env, key, path, valueName, types, 2);
}

/*
 * API: nativeDeleteValue
 *
 * Deletes the given registry value.
 *
 * rootKeyID: A root key identifier.
 * path: The path of the subkey containing the value to be retrieved.
 * valueName: The name of the value to be retrieved.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeDeleteValue(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring jPath, jstring jValueName)
{
    const WCHAR *valueName;
    long result;
    jboolean ret;

    // Open the parent subkey.
    HKEY parentKey = _openKey(env, rootKeyID, jPath, KEY_WRITE);

    if (parentKey == HKEY_ERROR)
    {
        return JNI_FALSE;
    }

    // Delete the subkey (is opened if it already exists).
    if ((valueName = javaStringToPlatformChars(env, jValueName)) == NULL)
	{
		// String allocation failed, exception already thrown
		_closeKey(parentKey);
		return JNI_FALSE;
	}

    result = RegDeleteValueW(parentKey, valueName);
    releasePlatformChars(env, jValueName, valueName);

    if (result == ERROR_SUCCESS)
    {
        ret = JNI_TRUE;
    }
    else
    {
        ret = JNI_FALSE;
    }

    _closeKey(parentKey);
    return ret;
}

/*
 * API: nativeHasSubkey
 *
 * Returns a boolean indicating if the specified subkey exists in the
 * specified parent subkey.
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey containing the subkey to be tested.
 * subkeyName: The name of the subkey to be tested.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeHasSubkey(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path, jstring subkeyName)
{
    HKEY key = _openKey(env, rootKeyID, path, KEY_READ);
    HKEY subkey;

    if (key == HKEY_ERROR)
    {
        return JNI_FALSE;
    }

    subkey = _getSubKey(env, key, subkeyName, KEY_READ);

    if (subkey == HKEY_ERROR)
    {
        _closeKey(key);
        return JNI_FALSE;
    }

    _closeKey(subkey);
    _closeKey(key);
    return JNI_TRUE;
}

/*
 * API: nativeHasSubkey
 *
 * Returns a boolean indicating if any subkeys exist in the specified
 * parent subkey.
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey to be tested for child subkeys.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeHasSubkeys(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path)
{
    HKEY key = _openKey(env, rootKeyID, path, KEY_READ);
    DWORD subkeyCount;
    long result;

    if (key == HKEY_ERROR)
    {
        return JNI_FALSE;
    }

    result = RegQueryInfoKey(key, NULL, NULL, NULL, &subkeyCount, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    _closeKey(key);

    if (result != ERROR_SUCCESS)
    {
        return JNI_FALSE;
    }

    if (subkeyCount > 0)
    {
        return JNI_TRUE;
    }
    else
    {
        return JNI_FALSE;
    }
}

/*
 * API: nativeHasValue
 *
 * Returns a boolean indicating if the specified value exists in the
 * specified parent subkey.
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey containing the value to be tested.
 * valueName: The name of the value to be tested.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeHasValue(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path, jstring valueName)
{
    HKEY key = _getRootKey(rootKeyID);

    jobject value = _getValue(env, key, path, valueName, NULL, 0);

    if (value == NULL)
    {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * API: nativeHasValues
 *
 * Returns a boolean indicating if the specified subkey contains any values.
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey containing the value to be tested.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeHasValues(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path)
{
    HKEY key = _openKey(env, rootKeyID, path, KEY_READ);
    DWORD valuesCount;
    long result;

    if (key == HKEY_ERROR)
    {
        return JNI_FALSE;
    }

    result = RegQueryInfoKey(key, NULL, NULL, NULL, NULL, NULL, NULL, &valuesCount, NULL, NULL, NULL, NULL);
    _closeKey(key);

    if (valuesCount > 0)
    {
        return JNI_TRUE;
    }
    else
    {
        return JNI_FALSE;
    }
}

/*
 * API: nativeSetDwordValue
 *
 * Sets the specified DWORD value in the specified subkey.  If the value
 * already exists it is overwritten.  If the values does not already exist
 * it is created.
 *
 * TODO: What if the existing value is a different type?
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey to contain the new value.
 * valueName: The name of the value to set.
 * data: The value to set.
 */
JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeSetDwordValue(
    JNIEnv *env,
    jclass cls,
    jint rootKeyID,
    jstring path,
    jstring valueName,
    jint data)
{
    HKEY key = _openKey(env, rootKeyID, path, KEY_WRITE);
    unsigned char bytes[4];

    if (key == HKEY_ERROR)
    {
        return;
    }

    bytes[0] = data & 0xff;
    data = data >> 8;
    bytes[1] = data & 0xff;
    data = data >> 8;
    bytes[2] = data & 0xff;
    data = data >> 8;
    bytes[3] = data & 0xff;

    _setValue(env, key, valueName, REG_DWORD, bytes, 4);
    _closeKey(key);
}

/*
 * API: nativeSetStringValue
 *
 * Sets the specified string value in the specified subkey.  If the value
 * already exists it is overwritten.  If the values does not already exist
 * it is created.
 *
 * TODO: What if the existing value is a different type?
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey to contain the new value.
 * jValueName: The name of the value to set.
 * jData: The value to set.
 */
JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeSetStringValue(
    JNIEnv *env,
    jclass cls,
    jint rootKeyID,
    jstring path,
    jstring jValueName,
    jstring jData)
{
    HKEY key = _openKey(env, rootKeyID, path, KEY_WRITE);
    const WCHAR *data;
    int length;

    if (key == HKEY_ERROR)
    {
        return;
    }

    length = (*env)->GetStringLength(env, jData);
    
	if ((data = javaStringToPlatformChars(env, jData)) == NULL)
	{
		// String allocation failed, exception already thrown
		_closeKey(key);
		return;
	}

    _setValue(env, key, jValueName, REG_SZ, (PVOID) data, (length + 1) * 2);

    releasePlatformChars(env, jData, data);
    _closeKey(key);
}

/*
 * API: nativeGetSubkeys
 *
 * Returns a java array of strings representing the subkey names for the
 * specfified parent subkey.  If there are no subkeys an empty arrary is
 * returned.  If an error occurs, NULL is returned.
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey to contain the new value.
 */
JNIEXPORT jobjectArray JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeGetSubkeys(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path)
{
    long result;
    DWORD subkeyCount;

    // Open the parent subkey.  Bail if it does not exist.
    HKEY key = _openKey(env, rootKeyID, path, KEY_READ);

    if (key == HKEY_ERROR)
    {
        return NULL;
    }

    // Query to determine how many subkeys exist.
    result = RegQueryInfoKey(key, NULL, NULL, NULL, &subkeyCount, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

    if (result != ERROR_SUCCESS)
    {
        _closeKey(key);
        return NULL;
    }
    else
    {
        // The parent subkey is now open.
        int size;
        WCHAR buffer[KEY_NAME_MAXSIZE];
        DWORD subkeyIndex;
        jstring subkeyName;
        jstring *subkeys;
        jobjectArray ret;
        DWORD i;

        // Create a temporary array to hold the subkeys names.
        subkeys = (jstring *) malloc(subkeyCount * sizeof(jstring));
        if (subkeys == NULL)
        {
            _closeKey(key);
            return NULL;
        }

        // Enumerate the subkeys and store them in the temporary array.
        subkeyIndex = 0;
        result = ERROR_SUCCESS;
        while (subkeyIndex < subkeyCount && result == ERROR_SUCCESS)
        {
            size = KEY_NAME_MAXSIZE;
            result = RegEnumKeyExW(key, subkeyIndex, buffer, &size, NULL, NULL, NULL, NULL);

            if (result == ERROR_SUCCESS)
            {
                subkeyName = (*env)->NewString(env, buffer, size);
                subkeys[subkeyIndex++] = subkeyName;
            }
        }

        // Create a java string array of the correct size.
        ret = (*env)->NewObjectArray(env, subkeyIndex, (*env)->FindClass(env, "java/lang/String"),
            (*env)->NewStringUTF(env, (const char *) ""));

        // Copy the jstring subkey names to the java array.
        for (i = 0; i < subkeyIndex; i++)
        {
            (*env)->SetObjectArrayElement(env, ret, i, subkeys[i]);
        }

        free(subkeys);
        _closeKey(key);
        return ret;
    }
}

/*
 * API: nativeGetValues
 *
 * Returns a java array of RegistryValues representing the values and types for the
 * specfified parent subkey.  If there are no values an empty arrary is
 * returned.  If an error occurs, NULL is returned.
 * 
 * rootKeyID: A root key identifier.
 * path: The path of the subkey which contains the values.
 */
JNIEXPORT jobjectArray JNICALL Java_com_microsoft_tfs_jni_RegistryKey_nativeGetValues(JNIEnv *env, jclass cls,
    jint rootKeyID, jstring path)
{
    long result;
    DWORD valueCount;

    // Open the parent subkey.  Bail if it does not exist.
    HKEY key = _openKey(env, rootKeyID, path, KEY_READ);

    if (key == HKEY_ERROR)
    {
        return NULL;
    }

    // Query to determine how many subkeys exist.
    result = RegQueryInfoKey(key, NULL, NULL, NULL, NULL, NULL, NULL, &valueCount, NULL, NULL, NULL, NULL);

    if (result != ERROR_SUCCESS)
    {
        _closeKey(key);
        return NULL;
    }
    else
    {
        // The parent subkey is now open.
        DWORD nameSize;
        WCHAR *nameBuffer;
        DWORD dataSize;
        unsigned char *dataBuffer;
        DWORD valueIndex;
        DWORD valueType;
        jstring valueName;
        jobject value;
        jobject *values;
        jobjectArray ret;
        DWORD i;

        // Allocate the name buffer.
        nameBuffer = (WCHAR *) malloc(VALUE_NAME_MAXSIZE * sizeof(WCHAR));
        if (nameBuffer == NULL)
        {
            _closeKey(key);
            return NULL;
        }

        // Allocate the data buffer.
        dataBuffer = (unsigned char*) malloc(VALUE_MAXSIZE);
        if (dataBuffer == NULL)
        {
            _closeKey(key);
            free(nameBuffer);
            return NULL;
        }

        // Create a temporary array to hold the subkeys names.
        values = (jobject *) malloc(valueCount * sizeof(jobject));
        if (values == NULL)
        {
            _closeKey(key);
            free(nameBuffer);
            free(dataBuffer);
            return NULL;
        }

        // Enumerate the values and store them in the temporary array.
        valueIndex = 0;
        result = ERROR_SUCCESS;
        while (valueIndex < valueCount && result == ERROR_SUCCESS)
        {
            nameSize = VALUE_NAME_MAXSIZE;
            dataSize = VALUE_MAXSIZE;
            result = RegEnumValueW(key, valueIndex, nameBuffer, &nameSize, NULL, &valueType, dataBuffer, &dataSize);

            if (result == ERROR_SUCCESS)
            {
                valueName = (*env)->NewString(env, nameBuffer, nameSize);
                value = _createRegistryValue(env, valueName, dataBuffer, dataSize, valueType);
                values[valueIndex++] = value;
            }
        }

        // Create a java string array of the correct size.
        ret = (*env)->NewObjectArray(env, valueIndex, (*env)->FindClass(env, "com/microsoft/tfs/jni/RegistryValue"),
            NULL);

        // Copy the Registry values to the java array.
        for (i = 0; i < valueIndex; i++)
        {
            (*env)->SetObjectArrayElement(env, ret, i, values[i]);
        }

        free(nameBuffer);
        free(dataBuffer);
        free(values);
        _closeKey(key);
        return ret;
    }
}
