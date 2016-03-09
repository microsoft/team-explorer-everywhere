/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#include <stdio.h>
#include <windows.h>
#include <direct.h>
#include <jni.h>
#include <accctrl.h>
#include <aclapi.h>
#include <sddl.h>

#include "util.h"
#include "objects.h"
#include "native_filesystem.h"
#include "logger.h"

JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeGetOwner(JNIEnv *env,
    jclass cls, jstring jPath)
{
    const WCHAR * path = NULL;
    DWORD result = 0;
    PSECURITY_DESCRIPTOR securityDescriptor = NULL;
    PSID ownerSID = NULL;
    WCHAR * ownerSIDString = NULL;
    jstring jOwnerSIDString = NULL;

    if (jPath == NULL)
    {
		throwRuntimeExceptionString(env, "path must not be null");
        goto cleanup;
    }

    if ((path = javaStringToPlatformChars(env, jPath)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	// Get sid, which points into securityDescriptor
	result = GetNamedSecurityInfoW(path, SE_FILE_OBJECT, OWNER_SECURITY_INFORMATION, 
		&ownerSID, NULL, NULL, NULL, &securityDescriptor);
    if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error getting file security info for %S", path);
		goto cleanup;
	}

	// Convert to string SID
	if (ConvertSidToStringSidW(ownerSID, &ownerSIDString) == FALSE)
    {
		throwRuntimeExceptionCode(env, GetLastError(), "Error converting sid to string sid");
		goto cleanup;
	}

    jOwnerSIDString = platformCharsToJavaString(env, ownerSIDString);

cleanup:

	if (path != NULL)
	{
	    releasePlatformChars(env, jPath, path);
	}
	// ownerSID points inside securityDescriptor
	if (securityDescriptor != NULL)
	{
		LocalFree(securityDescriptor);
	}
	if (ownerSIDString != NULL)
	{
		LocalFree(ownerSIDString);
	}

    return jOwnerSIDString;
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeSetOwner(JNIEnv *env,
	jclass cls, jstring jPath, jstring jOwnerSIDString)
{
    const WCHAR * path= NULL;
    const WCHAR * ownerSIDString = NULL;
    PSID ownerSID = NULL;
    DWORD result = 0;

    if (jPath == NULL)
    {
       	throwRuntimeExceptionString(env, "path must not be null");
		goto cleanup;
    }

	if (jOwnerSIDString == NULL)
	{
		throwRuntimeExceptionString(env, "user must not be null");
		goto cleanup;
	}

    if ((ownerSIDString = javaStringToPlatformChars(env, jOwnerSIDString)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}
    
	if (ConvertStringSidToSidW(ownerSIDString, &ownerSID) == FALSE)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error converting string %S sid to sid", ownerSIDString);
		goto cleanup;
	}

    if ((path = javaStringToPlatformChars(env, jPath)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	result = SetNamedSecurityInfoW((WCHAR *) path, SE_FILE_OBJECT, OWNER_SECURITY_INFORMATION, 
		ownerSID, NULL, NULL, NULL);
    if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error getting file security info for %S", path);
		goto cleanup;
	}

cleanup:

	if (ownerSIDString != NULL)
	{
	   releasePlatformChars(env, jOwnerSIDString, ownerSIDString);
	}
	if (path != NULL)
	{
		releasePlatformChars(env, jPath, path);
	}
	if (ownerSID != NULL)
	{
		LocalFree(ownerSID);
	}
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeGrantInheritableFullControl(
    JNIEnv *env, jclass cls, jstring jPath, jstring jUserSIDString, jstring jCopyExplicitRulesFromPath)
{
	const WCHAR * path = NULL;
	const WCHAR * userSIDString = NULL;
	const WCHAR * copyExplicitRulesFromPath = NULL;
	DWORD result = 0;
	PACL existingDACL = NULL;
	PACL newDACL = NULL;
	PSECURITY_DESCRIPTOR securityDescriptor = NULL;
	PSID userSID = NULL;
	EXPLICIT_ACCESS fullControl;
	
	if (jPath == NULL)
    {
       	throwRuntimeExceptionString(env, "path must not be null");
		goto cleanup;
    }

	if (jUserSIDString == NULL)
	{
		throwRuntimeExceptionString(env, "user must not be null");
		goto cleanup;
	}

	// Get the existing DACL entries
	if (jCopyExplicitRulesFromPath != NULL)
	{
		if ((copyExplicitRulesFromPath = javaStringToPlatformChars(env, jCopyExplicitRulesFromPath)) == NULL)
		{
			// String allocation failed, exception already thrown
			goto cleanup;
		}

		result = GetNamedSecurityInfo(copyExplicitRulesFromPath, SE_FILE_OBJECT, DACL_SECURITY_INFORMATION, 
			NULL, NULL, &existingDACL, NULL, &securityDescriptor);

		if (result != ERROR_SUCCESS)
		{
			throwRuntimeExceptionCode(env, result, "Error getting file security info for %S", copyExplicitRulesFromPath);
			goto cleanup;
	    }
	}

	// Convert the string SID to a structure
	if ((userSIDString = javaStringToPlatformChars(env, jUserSIDString)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	if (ConvertStringSidToSidW(userSIDString, &userSID) == FALSE)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error converting string sid %S to sid", userSIDString);
		goto cleanup;
	}

	/*
	 * Create a new explicit access entry with rights equivalent to .NET's 
	 * FileSystemRights.FullControl (0x1F01FF; see FileSecurity.cs) and
	 * full inheritance.
	 */
	ZeroMemory(&fullControl, sizeof(EXPLICIT_ACCESS));
    fullControl.grfAccessPermissions = 0x1F01FF;
    fullControl.grfAccessMode = GRANT_ACCESS;
    fullControl.grfInheritance= CONTAINER_INHERIT_ACE | OBJECT_INHERIT_ACE;
    fullControl.Trustee.TrusteeForm = TRUSTEE_IS_SID;
	fullControl.Trustee.TrusteeType = TRUSTEE_IS_USER;
    fullControl.Trustee.ptstrName = userSID;
	
	// Merge new entry with old entries into a new list
	result = SetEntriesInAcl(1, &fullControl, existingDACL, &newDACL);
    if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error setting entries in ACL");
        goto cleanup;
    }

	// Set the list on the path
	if ((path = javaStringToPlatformChars(env, jPath)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	result = SetNamedSecurityInfo((WCHAR *) path, SE_FILE_OBJECT, DACL_SECURITY_INFORMATION,
		NULL, NULL, newDACL, NULL);
	if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error setting file security info for %S", path);
        goto cleanup;
	}

cleanup:

	if (path != NULL)
	{
		releasePlatformChars(env, jPath, path);
	}
	if (userSIDString != NULL)
	{
		releasePlatformChars(env, jUserSIDString, userSIDString);
	}
	if (copyExplicitRulesFromPath != NULL)
	{
		releasePlatformChars(env, jCopyExplicitRulesFromPath, copyExplicitRulesFromPath);
	}
	if (securityDescriptor != NULL)
	{
		LocalFree(securityDescriptor);
	}
	if (userSID != NULL)
	{
		LocalFree(userSID);
	}
	if (newDACL != NULL)
	{
		LocalFree(newDACL);
	}
	// existingDACL points inside securityDescriptor
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeRemoveExplicitAllowEntries(
    JNIEnv *env, jclass cls, jstring jPath, jstring jUserSIDString)
{
	const WCHAR * path = NULL;
	const WCHAR * userSIDString = NULL;
	PSID userSID = NULL;
	DWORD result = 0;
	PACL dacl = NULL;
	PSECURITY_DESCRIPTOR securityDescriptor = NULL;
	ACL_SIZE_INFORMATION aclSizeInfo;
	ULONG aceCount = 0;
	BOOL modifiedDACL = FALSE;

	if (jPath == NULL)
    {
       	throwRuntimeExceptionString(env, "path must not be null");
		goto cleanup;
    }

	if (jUserSIDString == NULL)
    {
       	throwRuntimeExceptionString(env, "user must not be null");
		goto cleanup;
    }
	
	// Convert the SID string to a struct
	if ((userSIDString = javaStringToPlatformChars(env, jUserSIDString)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	if (ConvertStringSidToSidW(userSIDString, &userSID) == FALSE)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error converting string sid %S to sid", userSIDString);
		goto cleanup;
	}

	if ((path = javaStringToPlatformChars(env, jPath)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	// Get file's DACL
	result = GetNamedSecurityInfo(path, SE_FILE_OBJECT, DACL_SECURITY_INFORMATION, 
		NULL, NULL, &dacl, NULL, &securityDescriptor);
	if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error getting file security info for %S", path);
		goto cleanup;
	}

	// Get the count of entries int the DACL
	if (GetAclInformation(dacl, &aclSizeInfo, sizeof(aclSizeInfo), AclSizeInformation) == 0)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error getting DACL");
		goto cleanup;
	}

	// Loop over the DACL backwards, removing matching entries
	for (aceCount = aclSizeInfo.AceCount; aceCount > 0; aceCount--)
	{
		ULONG aceIndex = aceCount - 1;
		ACCESS_ALLOWED_ACE * ace = NULL;
		PSID sid = NULL;

		if (GetAce(dacl, aceIndex, (LPVOID *) &ace) == 0)
		{
			throwRuntimeExceptionCode(env, GetLastError(), "Error getting ACE at index %d", aceIndex);
			goto cleanup;
		}

		// Skip inherited (non-explicit) entries
		if ((((ACE_HEADER *) ace)->AceFlags & INHERITED_ACE) == INHERITED_ACE)
		{
			continue;
		}

		// Extract the SID for "allow" types
		switch(((ACE_HEADER *) ace)->AceType)
		{
			case ACCESS_ALLOWED_ACE_TYPE:
				sid = (PSID) &((ACCESS_ALLOWED_ACE *) ace)->SidStart;
				break;
			case ACCESS_ALLOWED_CALLBACK_ACE_TYPE:
				sid = (PSID) &((ACCESS_ALLOWED_CALLBACK_ACE *) ace)->SidStart;
				break;
			case ACCESS_ALLOWED_CALLBACK_OBJECT_ACE_TYPE:
				sid = (PSID) &((ACCESS_ALLOWED_CALLBACK_OBJECT_ACE *) ace)->SidStart;
				break;
			case ACCESS_ALLOWED_OBJECT_ACE_TYPE:
				sid = (PSID) &((ACCESS_ALLOWED_OBJECT_ACE *) ace)->SidStart;
				break;
			default:
				// These are "deny" or other entries
				break;
		}

		if (sid != NULL && EqualSid(sid, userSID))
		{
			if (DeleteAce(dacl, aceIndex) == 0)
			{
				throwRuntimeExceptionCode(env, GetLastError(), "Error deleting ACE at index %d", aceIndex);
				goto cleanup;
			}

			modifiedDACL = TRUE;
		}

		// Nothing to free in the loop, all pointers are into dacl
	}

	if (modifiedDACL)
	{
		result = SetNamedSecurityInfo((WCHAR *) path, SE_FILE_OBJECT, DACL_SECURITY_INFORMATION,
			NULL, NULL, dacl, NULL);
		if (result != ERROR_SUCCESS)
		{
			throwRuntimeExceptionCode(env, result, "Error setting security info for %S", path);
			goto cleanup;
		}
	}

cleanup:

	if (path != NULL)
	{
		releasePlatformChars(env, jPath, path);
	}
	if (userSID != NULL)
	{
		LocalFree(userSID);
	}
	if (userSIDString != NULL)
	{
	   releasePlatformChars(env, jUserSIDString, userSIDString);
	}
	// dacl points inside securityDescriptor
	if (securityDescriptor != NULL)
	{
		LocalFree(securityDescriptor);
	}
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeCopyExplicitDACLEntries(
    JNIEnv *env, jclass cls, jstring jSourcePath, jstring jTargetPath)
{
	const WCHAR * sourcePath = NULL;
	const WCHAR * targetPath = NULL;
	DWORD result = 0;
	PACL sourceDACL = NULL;
	PACL targetDACL = NULL;
	PACL newDACL = NULL;
	PSECURITY_DESCRIPTOR sourceSecurityDescriptor = NULL;
	PSECURITY_DESCRIPTOR targetSecurityDescriptor = NULL;
	PEXPLICIT_ACCESS sourceExplicitEntries = NULL;
	ULONG sourceExplicitEntriesCount = 0;

	if (jSourcePath == NULL)
    {
       	throwRuntimeExceptionString(env, "source path must not be null");
		goto cleanup;
    }

	if (jTargetPath == NULL)
	{
		throwRuntimeExceptionString(env, "target path must not be null");
		goto cleanup;
	}

	if ((sourcePath = javaStringToPlatformChars(env, jSourcePath)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	if ((targetPath = javaStringToPlatformChars(env, jTargetPath)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	// Get source's DACL
	result = GetNamedSecurityInfo(sourcePath, SE_FILE_OBJECT, DACL_SECURITY_INFORMATION, 
		NULL, NULL, &sourceDACL, NULL, &sourceSecurityDescriptor);
	if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error getting security info for %S", sourcePath);
		goto cleanup;
	}

	// Get the explicit entries in the source DACL
	result = GetExplicitEntriesFromAcl(sourceDACL, &sourceExplicitEntriesCount, &sourceExplicitEntries);
	if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error getting ACL entries");
		goto cleanup;
	}

	if (sourceExplicitEntries == 0)
	{
		goto cleanup;
	}

	// Get target's DACL
	result = GetNamedSecurityInfo(targetPath, SE_FILE_OBJECT, DACL_SECURITY_INFORMATION, 
		NULL, NULL, &targetDACL, NULL, &targetSecurityDescriptor);
	if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error getting security info for %S", targetPath);
		goto cleanup;
	}

	// Merge the source entries into the target list
	result = SetEntriesInAcl(sourceExplicitEntriesCount, sourceExplicitEntries, targetDACL, &newDACL);
    if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error setting entries in ACL");
        goto cleanup;
    }

	// Set the list on the target path
	result = SetNamedSecurityInfo((WCHAR *) targetPath, SE_FILE_OBJECT, DACL_SECURITY_INFORMATION,
		NULL, NULL, newDACL, NULL);
	if (result != ERROR_SUCCESS)
	{
		throwRuntimeExceptionCode(env, result, "Error setting security info for %S", targetPath);
        goto cleanup;
	}

cleanup:

	if (sourcePath != NULL)
	{
		releasePlatformChars(env, jSourcePath, sourcePath);
	}
	if (targetPath != NULL)
	{
		releasePlatformChars(env, jTargetPath, targetPath);
	}
	// sourceDACL points into sourceSecurityDescriptor
	if (sourceSecurityDescriptor != NULL)
	{
		LocalFree(sourceSecurityDescriptor);
	}
	// targetDACL points into targetSecurityDescriptor
	if (targetSecurityDescriptor != NULL)
	{
		LocalFree(targetSecurityDescriptor);
	}
	if (sourceExplicitEntries != NULL)
	{
		LocalFree(sourceExplicitEntries);
	}
	if (newDACL != NULL)
	{
		LocalFree(newDACL);
	}
}

JNIEXPORT jobject JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeGetAttributes(
    JNIEnv *env, jclass cls, jstring jFilepath)
{
    const WCHAR *filepath;
    WIN32_FILE_ATTRIBUTE_DATA attributes;
    BOOL result;
    unsigned __int64 modificationTimeTicks;
    jlong modificationTimeSecs, modificationTimeNanos;
    jlong fileSize;
    jclass exceptionClass, timeClass, attributesClass;
    jmethodID timeCtorMethod, attributesCtorMethod;
    jobject timeObj = NULL, attributesObj = NULL;
    DWORD error;
    char *errorMessage;

    /*
     * Ensure that we can load the necessary classes for our file attributes return value.
     * Note that these methods will return NULL on failure and raise an exception that will
     * be handled when we return to Java.
     */
    timeClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/FileSystemTime");
    attributesClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/FileSystemAttributes");

    if (timeClass == NULL || attributesClass == NULL)
        return NULL;

    timeCtorMethod = (*env)->GetMethodID(env, timeClass, "<init>", "(JJ)V");
    attributesCtorMethod = (*env)->GetMethodID(env, attributesClass, "<init>",
        "(ZLcom/microsoft/tfs/jni/FileSystemTime;JZZZZZZZZZZ)V");

    if (timeCtorMethod == NULL || attributesCtorMethod == NULL)
        return NULL;

    /*
     * Get the file path.
     */
    if (jFilepath == NULL)
        return NULL;

    if ((filepath = javaStringToPlatformChars(env, jFilepath)) == NULL)
	{
		// String allocation failed, exception already thrown
		return NULL;
	}

    /* Get the file attributes */
    result = GetFileAttributesExW(filepath, GetFileExInfoStandard, &attributes);

    releasePlatformChars(env, jFilepath, filepath);

    if (result == 0)
    {
        /* Handle common error cases where the file is not found. */
        error = GetLastError();

        if (error == ERROR_TOO_MANY_OPEN_FILES || error == ERROR_READ_FAULT || error == ERROR_SHARING_VIOLATION
            || error == ERROR_LOCK_VIOLATION)
        {
            exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");

            if (exceptionClass != NULL)
            {
                /*
                 * Exception messages ARE NOT UTF-16 strings, they're ASCII strings.  Use FormatMessageA
                 * instead of unicode methods.
                 */
                if (FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM, NULL, error, 0,
                    (LPSTR) & errorMessage, 0, NULL) == 0)
                {
                    errorMessage = "Could not load error message";
                }

                (*env)->ThrowNew(env, exceptionClass, errorMessage);
            }

            return NULL;
        }
        
        return newFileSystemAttributes(env,
        	attributesClass,
        	attributesCtorMethod,
        	JNI_FALSE, /* File does not exist */
        	NULL,
        	(jlong) 0,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE,
        	JNI_FALSE);
    }

    /*
     * Convert the date into a standard format:  first combine the 32 bit values into a single 64 bit value,
     * then subtract the number of seconds between 1601 and 1970 to base on the unix epoch, then split into
     * whole second and nanosecond portions (from the 100-nanosecond "ticks").
     */
    modificationTimeTicks = ((((unsigned __int64)attributes.ftLastWriteTime.dwHighDateTime & 0xffffffff) << 32) |
        ((unsigned __int64)attributes.ftLastWriteTime.dwLowDateTime & 0xffffffff));

    modificationTimeSecs = (jlong)((modificationTimeTicks / 10000000)) - 11644473600L;
    modificationTimeNanos = (modificationTimeTicks % 10000000);

    /* Get the file size as a jlong */
    fileSize = (((unsigned __int64)attributes.nFileSizeHigh & 0xffffffff) << 32) | (attributes.nFileSizeLow & 0xffffffff);

	timeObj = newFileSystemTime(
		env,
		timeClass,
		timeCtorMethod,
		modificationTimeSecs,
		modificationTimeNanos);

    attributesObj = newFileSystemAttributes(
        env,
        attributesClass,
        attributesCtorMethod,
        JNI_TRUE,
        timeObj,
        fileSize,
        ((attributes.dwFileAttributes & FILE_ATTRIBUTE_READONLY) == FILE_ATTRIBUTE_READONLY) ? JNI_TRUE : JNI_FALSE,
        JNI_FALSE, /* owner only */
        JNI_FALSE, /* public writable */
        ((attributes.dwFileAttributes & FILE_ATTRIBUTE_HIDDEN) == FILE_ATTRIBUTE_HIDDEN) ? JNI_TRUE : JNI_FALSE,
        ((attributes.dwFileAttributes & FILE_ATTRIBUTE_SYSTEM) == FILE_ATTRIBUTE_SYSTEM) ? JNI_TRUE : JNI_FALSE,
        ((attributes.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) == FILE_ATTRIBUTE_DIRECTORY) ? JNI_TRUE : JNI_FALSE,
        ((attributes.dwFileAttributes & FILE_ATTRIBUTE_ARCHIVE) == FILE_ATTRIBUTE_ARCHIVE) ? JNI_TRUE : JNI_FALSE,
        ((attributes.dwFileAttributes & FILE_ATTRIBUTE_NOT_CONTENT_INDEXED) == FILE_ATTRIBUTE_NOT_CONTENT_INDEXED) ? JNI_TRUE : JNI_FALSE,
        JNI_TRUE, /* executable */
        JNI_FALSE /* symbolic link */);

    return attributesObj;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeSetAttributes(
    JNIEnv *env, jclass cls, jstring jFilepath, jobject jAttributes)
{
    jclass attributesClass;
    jmethodID isReadOnlyMethod, isHiddenMethod, isSystemMethod, isArchiveMethod, isNotContentIndexedMethod;
    jobject timeObj = NULL, attributesObj = NULL;
    const WCHAR *filepath;
    DWORD attributes;
    jboolean readonly, hidden, system, archive, notContentIndexed;
    BOOL result;

    /*
     * Ensure that we can load the necessary classes for our file attributes return value.
     * Note that these methods will return NULL on failure and raise an exception that will
     * be handled when we return to Java.
     */
    attributesClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/FileSystemAttributes");

    if (attributesClass == NULL)
        return JNI_FALSE;

    isReadOnlyMethod = (*env)->GetMethodID(env, attributesClass, "isReadOnly", "()Z");
    isHiddenMethod = (*env)->GetMethodID(env, attributesClass, "isHidden", "()Z");
    isSystemMethod = (*env)->GetMethodID(env, attributesClass, "isSystem", "()Z");
    isArchiveMethod = (*env)->GetMethodID(env, attributesClass, "isArchive", "()Z");
    isNotContentIndexedMethod = (*env)->GetMethodID(env, attributesClass, "isNotContentIndexed", "()Z");

    if (isReadOnlyMethod == NULL || isHiddenMethod == NULL || isSystemMethod == NULL || isArchiveMethod == NULL
        || isNotContentIndexedMethod == NULL)
        return JNI_FALSE;

    readonly = (*env)->CallBooleanMethod(env, jAttributes, isReadOnlyMethod);
    hidden = (*env)->CallBooleanMethod(env, jAttributes, isHiddenMethod);
    system = (*env)->CallBooleanMethod(env, jAttributes, isSystemMethod);
    archive = (*env)->CallBooleanMethod(env, jAttributes, isArchiveMethod);
    notContentIndexed = (*env)->CallBooleanMethod(env, jAttributes, isNotContentIndexedMethod);

    /*
     * Get the file path.
     */
    if (jFilepath == NULL)
        return JNI_FALSE;

    if ((filepath = javaStringToPlatformChars(env, jFilepath)) == NULL)
	{
		// String allocation failed, exception already thrown
		return JNI_FALSE;
	}

    /*
     * Get the current attributes
     */
    attributes = GetFileAttributesW(filepath);

    if (attributes == INVALID_FILE_ATTRIBUTES)
    {
        result = FALSE;
    }
    else
    {
        if (readonly == JNI_TRUE)
        {
            attributes |= FILE_ATTRIBUTE_READONLY;
        }
        else
        {
            attributes &= (~FILE_ATTRIBUTE_READONLY);
        }

        if (hidden == JNI_TRUE)
        {
            attributes |= FILE_ATTRIBUTE_HIDDEN;
        }
        else
        {
            attributes &= (~FILE_ATTRIBUTE_HIDDEN);
        }

        if (system == JNI_TRUE)
        {
            attributes |= FILE_ATTRIBUTE_SYSTEM;
        }
        else
        {
            attributes &= (~FILE_ATTRIBUTE_SYSTEM);
        }

        if (archive == JNI_TRUE)
        {
            attributes |= FILE_ATTRIBUTE_ARCHIVE;
        }
        else
        {
            attributes &= (~FILE_ATTRIBUTE_ARCHIVE);
        }

        if (notContentIndexed == JNI_TRUE)
        {
            attributes |= FILE_ATTRIBUTE_NOT_CONTENT_INDEXED;
        }
        else
        {
            attributes &= (~FILE_ATTRIBUTE_NOT_CONTENT_INDEXED);
        }

        result = SetFileAttributesW(filepath, attributes);
    }

    releasePlatformChars(env, jFilepath, filepath);

    return (result == TRUE) ? JNI_TRUE : JNI_FALSE;
}
