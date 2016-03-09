/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * SSPI specific native authentication mechanisms.  (SPNEGO, NTLM)
 */

#include <windows.h>
#include <security.h>
#include <stdio.h>

#include "auth.h"
#include "util.h"
#include "logger.h"

_Ret_maybenull_ auth_configuration_t *auth_configure(_In_opt_ logger_t *logger)
{
    auth_configuration_t *configuration;

    /* Create an iwa_t */
    if ((configuration = (auth_configuration_t *) malloc(sizeof(auth_configuration_t))) == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not malloc");
        return NULL;
    }

    /* Zero all values */
    memset(configuration, 0, sizeof(auth_configuration_t));

    configuration->logger = logger;

    /* Load the DLL with our safe loading utility */
    if ((configuration->security_dll = safeLoadSystemDLL(L"security.dll")) == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not load security.dll");

		free(configuration);
        return NULL;
    }

    /* Find the address of the initialization function */
    if ((configuration->init_security_interface = (INIT_SECURITY_INTERFACE) GetProcAddress(configuration->security_dll,
        "InitSecurityInterfaceW")) == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not obtain security interface: %d", GetLastError());

        free(configuration);
        return NULL;
    }

    /* Run the initialization function to populate our security function table */
    if ((configuration->function_table = (configuration->init_security_interface)()) == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not obtain security function table");

		free(configuration);
        return NULL;
    }

    return configuration;
}

/*
 * Queries available authentication mechanisms.  Returns 1 if the specified
 * mechanism ("negotiate", "ntlm", etc) is available, 0 otherwise.
 */

int auth_available(_In_ auth_configuration_t *configuration, mechanism_t mechanism)
{
    return (mechanism == AUTH_MECHANISM_NEGOTIATE || mechanism == AUTH_MECHANISM_NTLM);
}

int auth_supports_credentials_default(_In_ auth_configuration_t *configuration, mechanism_t mechanism)
{
    CredHandle *credentials;
    int supported = 0;

    if (mechanism != AUTH_MECHANISM_NEGOTIATE && mechanism != AUTH_MECHANISM_NTLM)
        return 0;

    credentials = _auth_get_credhandle(configuration, mechanism, NULL, NULL, NULL);

    supported = (credentials != NULL);

    if (credentials != NULL)
		_auth_dispose_credhandle(configuration, credentials);

    return supported;
}

int auth_supports_credentials_specified(_In_ auth_configuration_t *configuration, mechanism_t mechanism)
{
    /* SSPI always supports specified credentials...? */
    return (mechanism == AUTH_MECHANISM_NEGOTIATE || mechanism == AUTH_MECHANISM_NTLM);
}

/*
 * Gets the username and domain used when connecting with default credentials.
 */
_Ret_maybenull_ platform_char *auth_get_credentials_default(_In_ auth_configuration_t *configuration, mechanism_t mechanism)
{
    WCHAR *username = NULL;

    SecPkgContext_Names names;
    CredHandle *credentials;

    if (mechanism != AUTH_MECHANISM_NEGOTIATE && mechanism != AUTH_MECHANISM_NTLM)
	    return NULL;

    if (configuration == NULL)
        return NULL;

    credentials = _auth_get_credhandle(configuration, mechanism, NULL, NULL, NULL);

    /* Get the username out of the credentials */
    if (configuration->function_table->QueryCredentialsAttributes(credentials, SECPKG_ATTR_NAMES, &names) != SEC_E_OK)
    {
        logger_write(configuration->logger, LOGLEVEL_WARN, "Could not determine default credential name");
    }
    else
    {
        username = _wcsdup(names.sUserName);
    }

	if (credentials != NULL)
      _auth_dispose_credhandle(configuration, credentials);

    return username;
}

_Ret_maybenull_ CredHandle *_auth_get_credhandle(_In_ auth_configuration_t *configuration, mechanism_t mechanism,
    _In_opt_z_ const platform_char *username, _In_opt_z_ const platform_char *domain, _In_opt_z_ const platform_char *password)
{
    CredHandle *credentials;

    SECURITY_STATUS status;
    TimeStamp timestamp;

    SEC_WINNT_AUTH_IDENTITY *identity = NULL;

    SEC_WCHAR *package_name;

    if (mechanism == AUTH_MECHANISM_NTLM)
        package_name = L"NTLM";
    else if (mechanism == AUTH_MECHANISM_NEGOTIATE)
        package_name = L"Negotiate";
    else
        return NULL;

    /* Create Authentication Info if necessary */
    if (username != NULL || password != NULL || domain != NULL)
    {
        if ((identity = _auth_get_identity(configuration, username, domain, password)) == NULL)
        {
            logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not obtain identity handle for %S\\%S", domain,
                username);
            return NULL;
        }
    }

    /* Create a credential handle */
    if ((credentials = (CredHandle *) malloc(sizeof(CredHandle))) == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not malloc");

        _auth_dispose_identity(configuration, identity);
        return NULL;
    }

    /* Get the credentials */
    if ((status = configuration->function_table->AcquireCredentialsHandle(NULL, package_name, SECPKG_CRED_BOTH, NULL,
        identity, NULL, NULL, credentials, &timestamp)) != SEC_E_OK)
    {
        logger_write(configuration->logger, LOGLEVEL_WARN, "Could not acquire credentials handle for %S: %lx",
            package_name, status);

        free(credentials);
        credentials = NULL;
    }

    _auth_dispose_identity(configuration, identity);
    return credentials;
}

void _auth_dispose_credhandle(_In_ auth_configuration_t *configuration, _Inout_ CredHandle *credentials)
{
    configuration->function_table->FreeCredentialsHandle(credentials);
    free(credentials);
}

_Ret_maybenull_ SEC_WINNT_AUTH_IDENTITY *_auth_get_identity(_In_ auth_configuration_t *configuration, 
	_In_opt_z_ const platform_char *username, _In_opt_z_ const platform_char *domain, _In_opt_z_ const platform_char *password)
{
    const WCHAR *usernameW, *domainW, *passwordW;
    SEC_WINNT_AUTH_IDENTITY *identity = NULL;

    usernameW = (username == NULL) ? L"" : username;
    passwordW = (password == NULL) ? L"" : password;
    domainW = (domain == NULL) ? L"" : domain;

    if ((identity = (SEC_WINNT_AUTH_IDENTITY *) malloc(sizeof(SEC_WINNT_AUTH_IDENTITY))) == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not malloc");
        return NULL;
    }

    identity->User = _wcsdup(usernameW);
    identity->UserLength = (unsigned long) wcslen(usernameW);

    identity->Password = _wcsdup(passwordW);
    identity->PasswordLength = (unsigned long) wcslen(passwordW);

    identity->Domain = _wcsdup(domainW);
    identity->DomainLength = (unsigned long) wcslen(domainW);

    identity->Flags = SEC_WINNT_AUTH_IDENTITY_UNICODE;

    return identity;
}

void _auth_dispose_identity(_In_ auth_configuration_t *configuration, _Inout_opt_ SEC_WINNT_AUTH_IDENTITY *identity)
{
    if (identity != NULL)
    {
        SecureZeroMemory(identity->Password, identity->PasswordLength);

        free(identity->User);
        free(identity->Password);
        free(identity->Domain);

        free(identity);
    }
}

/*
 * Creates an authentication object that can be used to authenticate with the
 * provided mechanism.  Returns NULL if an error occured.
 */
_Ret_maybenull_ auth_t *auth_initialize(_In_ auth_configuration_t *configuration, mechanism_t mechanism)
{
    auth_t *auth;
    SEC_WCHAR *package_name;
    SECURITY_STATUS status;
    SecPkgInfo *package_info;

    if (configuration == NULL || configuration->security_dll == NULL)
        return NULL;

    if (mechanism == AUTH_MECHANISM_NTLM)
        package_name = L"NTLM";
    else if (mechanism == AUTH_MECHANISM_NEGOTIATE)
        package_name = L"Negotiate";
    else
    {
        logger_write(configuration->logger, LOGLEVEL_WARN, "Unknown mechanism type requested: %d", mechanism);
        return NULL;
    }

    if ((auth = (auth_t *) malloc(sizeof(auth_t))) == NULL)
        return NULL;

    /* Zero all values */
    memset(auth, 0, sizeof(auth_t));

    auth->configuration = configuration;
    auth->mechanism = mechanism;

    /* Query the security package info */
    if ((status = configuration->function_table->QuerySecurityPackageInfo(package_name, &package_info)) != SEC_E_OK)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not get %S package info: %d", package_name, status);
        free(auth);
        return NULL;
    }

    /* copy attributes into our data structure */
    auth->token_maxlen = package_info->cbMaxToken;
    auth->mechanism_capabilities = package_info->fCapabilities;

    /* release with windows internal struct */
    if (configuration->function_table->FreeContextBuffer(package_info) != SEC_E_OK)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not free context buffer: %d", status);
        free(auth);
        return NULL;
    }

    return auth;
}

_Ret_maybenull_ auth_configuration_t *auth_get_configuration(_In_ auth_t *auth)
{
    if (auth == NULL)
        return NULL;

    return auth->configuration;
}

void auth_set_error(_In_ auth_t *auth, _Printf_format_string_ const char *fmt, ...)
{
    va_list ap;

    if (auth == NULL)
    {
        return;
    }

    if (fmt == NULL)
    {
        auth->error_message = _strdup("(unknown)");
        return;
    }

    va_start(ap, fmt);
    auth->error_message = tee_vsprintf(fmt, ap);
    va_end(ap);
}

_Ret_maybenull_ const char *auth_get_error(_In_ auth_t *auth)
{
    if (auth == NULL)
    {
        return NULL;
    }

    return auth->error_message;
}

/*
 * Sets the target of the authentication - typically the remote host name.
 */
void auth_set_target(_In_ auth_t *auth, _In_opt_z_ const platform_char *target)
{
    SEC_WCHAR *separator;

    if (auth == NULL)
        return;

    if (target == NULL)
    {
        auth->target = NULL;
    }
    else
    {
        auth->target = _wcsdup((const WCHAR *) target);

        /* Convert separator to a slash */
        if ((separator = wcsrchr(auth->target, L'@')) != NULL)
            *separator = L'/';
    }
}

/*
 * Sets the local hostname for authentication.
 */
void auth_set_localhost(_In_ auth_t *auth, _In_opt_z_ const platform_char *localhost)
{
    return;
}

/*
 * Requests that the underlying authentication mechanism use default
 * credentials for authentication.
 */
void auth_set_credentials_default(_In_ auth_t *auth)
{
    if (auth == NULL)
        return;

    logger_write(auth->configuration->logger, LOGLEVEL_DEBUG, "Configuring with default credentials");
    auth->credentials = _auth_get_credhandle(auth->configuration, auth->mechanism, NULL, NULL, NULL);
}

/*
 * Requests that the underlying authentication mechanism use the provided
 * username, domain, password for authentication.
 */
void auth_set_credentials(_In_ auth_t *auth, _In_opt_z_ const platform_char *username, 
	_In_opt_z_ const platform_char *domain, _In_opt_z_ const platform_char *password)
{
    if (auth == NULL)
        return;

    logger_write(auth->configuration->logger, LOGLEVEL_DEBUG, "Configuring with credentials %S\\%S", domain, username);
    auth->credentials = _auth_get_credhandle(auth->configuration, auth->mechanism, username, domain, password);
}

/*
 * Gets a token (message) that can be passed to the remote server for
 * authentication.  Any input token (from the server) should be provided with
 * its length.  A pointer to the output token will be placed in outputToken and
 * its length in outputTokenLen.  On success, 1 will be returned - on error, 0.
 */
int auth_get_token(_In_ auth_t *auth, _In_opt_ const void *inputToken, unsigned int inputTokenLen, 
	_Deref_out_opt_ void **outputToken, _Out_opt_ unsigned int *outputTokenLen)
{
    CtxtHandle *newContext;
    SecBufferDesc *inputBuffer = NULL;
    SecBufferDesc *outputBuffer;
    SECURITY_STATUS status;
    unsigned long contextAttributes;

    if (outputToken != NULL)
    {
        *outputToken = NULL;
    }

	if (outputTokenLen != NULL)
	{
		*outputTokenLen = 0;
	}

    /* Sanity check */
    if (auth == NULL)
    {
        return 0;
    }

    if (auth->credentials == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "Invalid authentication object");
        return 0;
    }

    if ((newContext = (CtxtHandle *) malloc(sizeof(CtxtHandle))) == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "Could not malloc");
        return 0;
    }

    /* Create space for the output */
    if ((outputBuffer = _auth_createbuffer_size(auth, auth->token_maxlen)) == NULL)
    {
        _auth_dispose_context(auth, newContext);

        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "Could not initialize output buffer");
        return 0;
    }

    /* Create the input data if it exists */
    if (inputToken != NULL && (inputBuffer = _auth_createbuffer_string(auth, inputToken, inputTokenLen)) == NULL)
    {
        _auth_dispose_context(auth, newContext);
        _auth_dispose_buffer(outputBuffer);

        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "Could not initialize input buffer");
        return 0;
    }

    /* Get the next message and new security context */
    status = auth->configuration->function_table->InitializeSecurityContext(auth->credentials, auth->context,
        auth->target, AUTH_INITIALIZE_FLAGS, 0, SECURITY_NETWORK_DREP, inputBuffer, 0, newContext, outputBuffer,
        &contextAttributes, NULL);

    /* Swap the old context for the new */
    if (auth->context != NULL && _auth_compare_context(auth->context, newContext) == 0)
        _auth_dispose_context(auth, auth->context);

    /* Complete the security context, if necessary */
    if (status == SEC_I_COMPLETE_AND_CONTINUE || status == SEC_I_COMPLETE_NEEDED)
        status = auth->configuration->function_table->CompleteAuthToken(newContext, outputBuffer);

    /* Inspect the status for success */
    if (status == SEC_E_OK || status == SEC_I_COMPLETE_AND_CONTINUE || status == SEC_I_COMPLETE_NEEDED
        || status == SEC_I_CONTINUE_NEEDED)
    {
        /* Ensure that the output pointer is sane, then duplicate it */
        if (outputToken != NULL && outputBuffer->cBuffers == 1 && outputBuffer->pBuffers->cbBuffer > 0)
        {
            *outputToken = (char *) malloc(outputBuffer->pBuffers->cbBuffer);

			if (outputTokenLen != NULL)
				*outputTokenLen = outputBuffer->pBuffers->cbBuffer;

            memcpy(*outputToken, outputBuffer->pBuffers->pvBuffer, outputBuffer->pBuffers->cbBuffer);
        }

        auth->context = newContext;
    }
    else
    {
        auth->context = NULL;
    }

    if (status == SEC_E_OK || status == SEC_I_COMPLETE_NEEDED)
        auth->complete = 1;

    /* Clean up buffers */
    if (inputBuffer != NULL)
        _auth_dispose_buffer(inputBuffer);

    _auth_dispose_buffer(outputBuffer);

    return 1;
}

/*
 * Compares the contexts referenced by each context handle.
 *
 * Returns: 1 if they are identical, 0 if they are different
 */
int _auth_compare_context(_In_ CtxtHandle *one, _In_ CtxtHandle *two)
{
    if (one->dwUpper == two->dwUpper && one->dwLower == two->dwLower)
        return 1;

    return 0;
}

void _auth_dispose_context(_In_ auth_t *auth, _Inout_ CtxtHandle *context)
{
    if (auth == NULL)
        return;

    auth->configuration->function_table->DeleteSecurityContext(context);
    free(context);
}

/*
 * Queries the authentication mechanism to determine whether authentication is
 * complete.  Returns 1 if authentication has finished (successfully or not),
 * 0 if handshaking is still occurring.
 */
int auth_is_complete(_In_ auth_t *auth)
{
    if (auth == NULL)
        return 1;

    return auth->complete;
}

/*
 * Create an empty security buffer.
 *
 * Returns: a pointer to an empty SecBufferDesc, or NULL if it could not be
 *          created.  This memory must be freed by the caller.
 */
_Ret_maybenull_ SecBufferDesc *_auth_createbuffer(_In_ auth_t *auth)
{
    SecBufferDesc *buffer_desc;
    SecBuffer *buffer;

    /*
     * A buffer description can refer to a single string, or an array.
     * Create a buffer description, and a single internal buffer of size 0
     */

    if ((buffer_desc = (SecBufferDesc *) malloc(sizeof(SecBufferDesc))) == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "iwa_createBuffer: could not malloc buffer_desc");
        return NULL;
    }

    if ((buffer = (SecBuffer *) malloc(sizeof(SecBuffer))) == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "iwa_createBuffer: could not malloc buffer");
        free(buffer_desc);
        return NULL;
    }

    buffer_desc->cBuffers = 1;
    buffer_desc->ulVersion = SECBUFFER_VERSION;
    buffer_desc->pBuffers = buffer;

    buffer->BufferType = SECBUFFER_EMPTY;
    buffer->cbBuffer = 0;
    buffer->pvBuffer = NULL;

    return buffer_desc;
}

/*
 * Create a security buffer suitable for holding a string of the specified
 * size.
 *
 * length: size of string to create (in bytes)
 * 
 * Return: a pointer to a SecBufferDesc of the appropriate size, or NULL if
 *         the memory could not be allocated.
 */
_Ret_maybenull_ SecBufferDesc *_auth_createbuffer_size(_In_ auth_t *auth, unsigned long length)
{
    SecBufferDesc *buffer_desc;
    SecBuffer *buffer;
    char *string;

    if ((buffer_desc = _auth_createbuffer(auth)) == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR,
            "iwa_createBufferWithLength: could not create buffer");
        return NULL;
    }

    if ((string = (char *) malloc(length)) == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "iwa_createBufferWithLength: could not malloc");

        _auth_dispose_buffer(buffer_desc);
        return NULL;
    }

    buffer = buffer_desc->pBuffers;

    buffer->BufferType = SECBUFFER_TOKEN;
    buffer->cbBuffer = length;
    buffer->pvBuffer = string;

    return buffer_desc;
}

/*
 * Create a security buffer which contains a copy of the caller's string, specified
 * by the pointer with size length.
 *
 * orig:   string to hold in the security buffer
 * length: length of the string
 * 
 * Return: a pointer to a SecBufferDesc containing the given string, or NULL if
 *         the memory could not be allocated.
 */
_Ret_maybenull_ SecBufferDesc *_auth_createbuffer_string(_In_ auth_t *auth, _In_z_ const unsigned char *orig, unsigned long length)
{
    SecBufferDesc *buffer_desc;
    SecBuffer *buffer;
    unsigned char *string;

    if ((buffer_desc = _auth_createbuffer(auth)) == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "Could not create buffer");
        return NULL;
    }

    if ((string = (unsigned char *) malloc(length)) == NULL)
    {
        logger_write(auth->configuration->logger, LOGLEVEL_ERROR, "Could not malloc");

        _auth_dispose_buffer(buffer_desc);
        return NULL;
    }

    memcpy(string, orig, length);

    buffer = buffer_desc->pBuffers;

    buffer->BufferType = SECBUFFER_TOKEN;
    buffer->cbBuffer = (unsigned long) length;
    buffer->pvBuffer = string;

    return buffer_desc;
}

/*
 * Releases the memory associated with the security buffer.
 *
 * bufferDesc: the buffer description to free
 */
void _auth_dispose_buffer(_Inout_ SecBufferDesc *buffer_desc)
{
    SecBuffer *buffer;
    unsigned long i;

    /* We do not use, or expect, arrays, but we should free them if they come up */
    for (i = 0, buffer = buffer_desc->pBuffers; i < buffer_desc->cBuffers; i++, buffer++)
    {
        /* Empty buffers don't need freeing */
        if (buffer->BufferType == SECBUFFER_EMPTY)
            continue;

        free(buffer->pvBuffer);
    }

    free(buffer_desc);
}

/*
 * Cleans up any resources associated with the authentication object.
 */
void auth_dispose(_Inout_ auth_t *auth)
{
    if (auth == NULL)
        return;

    if (auth->credentials != NULL)
        _auth_dispose_credhandle(auth->configuration, auth->credentials);

    if (auth->context != NULL)
        _auth_dispose_context(auth, auth->context);

    if (auth->target != NULL)
        free(auth->target);

    free(auth);
}
