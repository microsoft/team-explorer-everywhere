/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * logger_log4j: A log4j capable logging mechanism.
 *
 * This code is not thread safe - you must create a new logger_t for each
 * thread.
 */

#include <stdlib.h>
#include <string.h>
#include <jni.h>

#include "logger.h"
#include "logger_log4j.h"
#include "util.h"

/* All logging is done with single-byte UTF-8 strings */

_Ret_maybenull_ logger_t *logger_initialize(_In_ const void *jvm, _Printf_format_string_ const char *name)
{
    logger_t *logger;

    if ((logger = (logger_t *) malloc(sizeof(logger_t))) == NULL)
        return NULL;

    logger->name = name;
    logger->jvm = (JavaVM *) jvm;

    return logger;
}

void logger_write(_In_opt_ logger_t *logger, unsigned short level, _Printf_format_string_ const char *fmt, ...)
{
    va_list ap;
    JNIEnv *env;
    jclass logfactoryclass, logclass;
    jmethodID getlogmethod, logmethod;
    jstring jloggername, jmessage;
    jobject log4j;
    const char *loggerName, *methodName;
    char *message = NULL;

    if(fmt != NULL)
    {
        va_start(ap, fmt);
        message = tee_vsprintf(fmt, ap);
        va_end(ap);
    }

    if(message == NULL)
    {
        fprintf(stderr, "error: could not format message\n");
        return;
    }

    /* Debugging aid - allow null env / logger and dump to stdout / stderr */
    if(logger == NULL || logger->jvm == NULL)
    {
        FILE *output = (level == LOGLEVEL_WARN || level == LOGLEVEL_ERROR) ? stderr : stdout;

        fprintf(output, "%s\n", message);

        free(message);
        return;
    }

    if ((*logger->jvm)->GetEnv(logger->jvm, (void **)&env, JNI_VERSION_1_2))
    {
        fprintf(stderr, "error: could not locate jni environment\n");
        fprintf(stderr, "  %s\n", message);

        free(message);
        return;
    }

    loggerName = (logger->name != NULL) ? logger->name : "native";

    // Always single-byte strings
    if((jloggername = (*env)->NewStringUTF(env, loggerName)) == NULL)
    {
        (*env)->ExceptionClear(env);

        fprintf(stderr, "error: could not build java jloggername\n");
        fprintf(stderr, "  %s\n", message);

        free(message);
        return;
    }

    // Always single-byte strings
    if((jmessage = (*env)->NewStringUTF(env, message)) == NULL)
    {
        (*env)->ExceptionClear(env);

        fprintf(stderr, "error: could not build java log message\n");
        fprintf(stderr, "  %s\n", message);

        free(message);
        return;
    }

    if(level == LOGLEVEL_TRACE)
    methodName = "trace";
    else if(level == LOGLEVEL_DEBUG)
    methodName = "debug";
    else if(level == LOGLEVEL_INFO)
    methodName = "info";
    else if(level == LOGLEVEL_WARN)
    methodName = "warn";
    else if(level == LOGLEVEL_ERROR)
    methodName = "error";
    else
    methodName = "info";

    logfactoryclass = (*env)->FindClass(env, "org/apache/commons/logging/LogFactory");

    if(logfactoryclass == NULL)
    {
        (*env)->ExceptionClear(env);

        fprintf(stderr, "error: could not locate log factory class\n");
        fprintf(stderr, "  %s\n", message);

        free(message);
        return;
    }

    getlogmethod = (*env)->GetStaticMethodID(env, logfactoryclass, "getLog","(Ljava/lang/String;)Lorg/apache/commons/logging/Log;");

	if(getlogmethod == NULL)
	{
		(*env)->ExceptionClear(env);

		fprintf(stderr, "error: could not locate get log method\n");
		fprintf(stderr, "  %s\n", message);

		free(message);
		return;
	}

	log4j = (*env)->CallStaticObjectMethod(env, logfactoryclass, getlogmethod, jloggername);

	logclass = (*env)->GetObjectClass(env, log4j);

	if(logclass == NULL)
	{
		(*env)->ExceptionClear(env);

		fprintf(stderr, "error: could not locate logger class\n");
		fprintf(stderr, "  %s\n", message);

		free(message);
		return;
	}

	logmethod = (*env)->GetMethodID(env, logclass, methodName, "(Ljava/lang/Object;)V");

	if(logmethod == NULL)
	{
		(*env)->ExceptionClear(env);

		fprintf(stderr, "error: could not locate logger method %s\n", methodName);
		fprintf(stderr, "  %s\n", message);

		free(message);
		return;
	}

    // Done keeping this around for fprintf(stderr, ...) cases
	free(message);
	
	(*env)->CallVoidMethod(env, log4j, logmethod, jmessage);
	
	if((*env)->ExceptionCheck(env))
		(*env)->ExceptionClear(env);
}

void logger_dispose(_Inout_opt_ logger_t *logger)
{
    /* Debugging aid: allow null logger (noop) */
    if (logger != NULL)
    {
        free(logger);
    }
}
