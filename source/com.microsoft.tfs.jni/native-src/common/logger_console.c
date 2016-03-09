/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * logger_console: A simple console (stdout/stderr) logging mechanism.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>

#include "logger.h"
#include "util.h"

/* All logging is done with single-byte UTF-8 strings */

_Ret_maybenull_ logger_t *logger_initialize(_In_ const void *jvm, _In_z_ const char *name)
{
    logger_t *logger = (logger_t *) malloc(sizeof(logger_t));

	if (logger != NULL)
      logger->dummy = 0;

    return logger;
}

void logger_write(_In_opt_ logger_t *logger, unsigned short loglevel, _Printf_format_strin_ const char *fmt, ...)
{
    va_list ap;
    char *message;

    FILE *fh = (loglevel == LOGLEVEL_WARN || loglevel == LOGLEVEL_ERROR) ? stderr : stdout;

    // Always single-byte strings in logging functions

    if(fmt != NULL)
    {
        va_start(ap, fmt);
        message = tee_vsprintf(fmt, ap);
        va_end(ap);
    }

    if(message == NULL)
    {
		return;
    }

    fprintf(fh, "%s\n", message);
    fflush(fh);

    free(message);
}

void logger_dispose(_Inout_opt_ logger_t *logger)
{
    /* Debugging hack: allow null logger */
    if (logger != NULL)
    {
        free(logger);
    }
}
