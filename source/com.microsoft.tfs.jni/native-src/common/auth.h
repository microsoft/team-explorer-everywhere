/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * A common authentication interface for native authentication mechanisms.
 * SSPI or GSSAPI (or etc) should be in the platform specific directories.
 *
 * Note that any method which returns or takes an auth_t is NOT thread safe.
 */

#ifndef AUTH_H
#define AUTH_H

#ifdef _WIN32
# include "auth_sspi.h"
#else
# include "auth_gss.h"
#endif

#include "tee_sal.h"
#include "util.h"
#include "logger.h"

/* Mechanism definition */
#define AUTH_MECHANISM_NTLM			1
#define AUTH_MECHANISM_NEGOTIATE 	2

typedef unsigned short mechanism_t;

/*
 * This is the common native authentication interface.  Platform specific code must
 * implement these functions.
 */

/*
 * Sets up the authentication system.  This typically (but need not) loads
 * shared libraries and the like.  Must return non-NULL on success.  Logger
 * is (optionally) a logger_t logger, or NULL to log
 * to stdout/stderr (for debugging.)
 */
_Ret_maybenull_ auth_configuration_t *auth_configure(_In_opt_ logger_t *logger);

/*
 * Destroys the configuration.  Required.
 */
void auth_dispose_configuration(_In_ auth_configuration_t *configuration);

/*
 * Queries available authentication mechanisms.  Returns 1 if the specified
 * mechanism ("negotiate", "ntlm", etc) is available, 0 otherwise.
 */
int auth_available(_In_ auth_configuration_t *configuration, mechanism_t mechanism);

/*
 * Queries the authentication system to determine whether default credentials
 * (ie the credentials of the currently logged in user) may be used to
 * authenticate for the given mechanism.  Returns 1 if default credentials
 * are available, 0 otherwise.
 */
int auth_supports_credentials_default(_In_ auth_configuration_t *configuration, mechanism_t mechanism);

/*
 * Queries the authentication system to determine whether specified credentials
 * (ie a username, domain, password) may be used to authenticate for the given
 * mechanism.  Returns 1 if default credentials are available, 0 otherwise.
 */
int auth_supports_credentials_specified(_In_ auth_configuration_t *configuration, mechanism_t mechanism);

/*
 * Gets the username and domain used when connecting with default credentials.
 * Returns NULL if default credentials could not be found.
 * Free the result with free(3).
 */
_Ret_maybenull_ platform_char *auth_get_credentials_default(_In_ auth_configuration_t *configuration, mechanism_t mechanism);

/*
 * Creates an authentication object that can be used to authenticate with the
 * provided mechanism.  Returns NULL if an error occured.
 */
_Ret_maybenull_ auth_t *auth_initialize(_In_ auth_configuration_t *configuration, mechanism_t mechanism);

/*
 * Sets an error message that can be retrieved by the caller.
 *
 * This function only works with single-byte wide strings.
 */
void auth_set_error(_In_ auth_t *auth, _Printf_format_string_ const char *fmt, ...);

/*
 * Retrieves a previously set error message.
 *
 * This function returns a single-byte wide string, NULL if no message was previously
 * set.
 */
_Ret_maybenull_ const char *auth_get_error(_In_ auth_t *auth);

/*
 * Sets the target of the authentication - typically the remote host name.
 */
void auth_set_target(_In_ auth_t *auth, _In_opt_z_ const platform_char *target);

/*
 * Sets the local hostname for authentication.
 */
void auth_set_localhost(_In_ auth_t *auth, _In_opt_z_ const platform_char *localhost);

/*
 * Requests that the underlying authentication mechanism use default
 * credentials for authentication.
 */
void auth_set_credentials_default(_In_ auth_t *auth);

/*
 * Requests that the underlying authentication mechanism use the provided
 * username, domain, password for authentication.
 */
void auth_set_credentials(_In_ auth_t *auth, _In_opt_z_ const platform_char *username, 
	_In_opt_z_ const platform_char *domain, _In_opt_z_ const platform_char *password);

/*
 * Gets a token (message) that can be passed to the remote server for
 * authentication.  Any input token (from the server) should be provided with
 * its length.  A pointer to the output token will be placed in outputToken and
 * its length in outputTokenLen.  On success, 1 will be returned - on error, 0.
 */
int auth_get_token(_In_ auth_t *auth, _In_opt_ const void *inputToken, unsigned int inputTokenLen, 
	_Deref_out_opt_ void **outputToken, _Out_opt_ unsigned int *outputTokenLen);

/*
 * Queries the authentication mechanism to determine whether authentication is
 * complete.  Returns 1 if authentication has finished (successfully or not),
 * 0 if handshaking is still occurring.
 */
int auth_is_complete(_In_ auth_t *auth);

/*
 * Cleans up any resources associated with the authentication object.
 */
void auth_dispose(_Inout_ auth_t *auth);

#endif /* AUTH_H */
