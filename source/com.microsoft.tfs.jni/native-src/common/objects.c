/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * Utilities for creating Java objects from JNI.  Not exports to JNI.
 *
 * These exist because some JNI methods, eg NewObject, are variadic.  These
 * functions ensure that the appropriate number of arguments are always
 * passed.
 */

#include <jni.h>

#include "objects.h"

jobject newFileSystemTime(
	JNIEnv *env,
	jclass timeClass,
	jmethodID timeCtorMethod,
	jlong modificationTimeSecs,
	jlong modificationTimeNanos)
{
	return (*env)->NewObject(
		env,
		timeClass,
		timeCtorMethod,
		modificationTimeSecs,
		modificationTimeNanos);
}

jobject newFileSystemAttributes(
	JNIEnv *env,
	jclass attributesClass,
	jmethodID attributesCtorMethod,
	jboolean fileExists,
	jobject modificationTime,
	jlong fileSize,
	jboolean readOnly,
	jboolean ownerOnly,
	jboolean publicWritable,
	jboolean hidden,
	jboolean system,
	jboolean directory,
	jboolean archive,
	jboolean notContentIndexed,
	jboolean executable,
	jboolean symbolicLink)
{
	return (*env)->NewObject(
		env,
		attributesClass,
		attributesCtorMethod,
		fileExists,
		modificationTime,
		fileSize,
		readOnly,
		ownerOnly,
		publicWritable,
		hidden,
		system,
		directory,
		archive,
		notContentIndexed,
		executable,
		symbolicLink);
}