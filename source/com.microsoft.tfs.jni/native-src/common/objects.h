/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#ifndef OBJECTS_H
#define OBJECTS_H

#include <jni.h>

jobject newFileSystemTime(
	JNIEnv *env,
	jclass timeClass,
	jmethodID timeCtorMethod,
	jlong modificationTimeSecs,
	jlong modificationTimeNanos);

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
	jboolean symbolicLink);

#endif /* OBJECTS_H */