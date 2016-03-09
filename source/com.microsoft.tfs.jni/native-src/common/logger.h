/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * logger.h defines an interface for logging systems.
 *
 * All logging functions use single-byte wide UTF-8 strings.  This is for convenience
 * because all Unix systems and Windows error strings are single-byte strings and
 * many logged strings are constant C strings.  Use tee_vsprintf() to format strings
 * for logging.
 */

#ifndef LOGGER_H
#define LOGGER_H

#include "tee_sal.h"

#ifdef LOGGER_CONSOLE
# include "logger_console.h"
#else
# include "logger_log4j.h"
#endif

/* Log levels */
#define LOGLEVEL_TRACE	0
#define LOGLEVEL_DEBUG	1
#define LOGLEVEL_INFO	2
#define LOGLEVEL_WARN	3
#define LOGLEVEL_ERROR	4
#define LOGLEVEL_FATAL	5

/* All logging is done with single-byte UTF-8 strings */

_Ret_maybenull_ logger_t *logger_initialize(_In_ const void *jvm, _Printf_format_string_ const char *name);
void logger_write(_In_opt_ logger_t *logger, unsigned short level, _Printf_format_string_ const char *fmt, ...);
void logger_dispose(_Inout_opt_ logger_t *logger);

#endif /* LOGGER_H */
