/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#ifndef LOGGER_LOG4J_H
#define LOGGER_LOG4J_H

#include <jni.h>

/* All logging is done with single-byte UTF-8 strings */

typedef struct {
	JavaVM *jvm;
	const char *name;
} logger_t;

#endif /* LOGGER_LOG4J_H */
