/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#ifndef AUTH_SSPI_H
#define AUTH_SSPI_H

#include <windows.h>
#include <security.h>
#include <stdio.h>

#include "util.h"
#include "auth.h"
#include "logger.h"

/* Flags used for initializing a security context (client-side) */
#define AUTH_INITIALIZE_FLAGS	(ISC_REQ_CONFIDENTIALITY | ISC_REQ_MUTUAL_AUTH)

/* Flags used for accepting a security context (server-side) */
#define AUTH_ACCEPT_FLAGS		(ASC_REQ_CONFIDENTIALITY | ASC_REQ_MUTUAL_AUTH)

typedef struct
{
    /* Logger */
    logger_t *logger;

    /* The security.dll instance which we dynamically load */
    HINSTANCE security_dll;

    /* The address of the security interface initialization function from the dll */
    INIT_SECURITY_INTERFACE init_security_interface;

    /* Addresses of the security functions from the security interface */
    PSecurityFunctionTable function_table;
} auth_configuration_t;

typedef struct
{
    /*
     * The auth configuration
     */
    auth_configuration_t *configuration;

    /*
     * The configured mechanism (eg, "ntlm", "negotiate").
     */
    unsigned short mechanism;

    /* Maximum length of a token */
    unsigned long token_maxlen;

    /* Capabilities for this package */
    unsigned long mechanism_capabilities;

    /* The authentication target SPN */
    SEC_WCHAR *target;

    /* The credentials for this session */
    CredHandle *credentials;
    int credentialsOwned;

    /* The current SSPI context */
    CtxtHandle *context;

    /*
     * Whether we have completed authentication
     */
    unsigned short int complete;

    /*
     * Any error message from authentication.
     */
    char *error_message;
} auth_t;

_Ret_maybenull_ CredHandle *_auth_get_credhandle(_In_ auth_configuration_t *auth, unsigned short mechanism, 
	_In_opt_z_ const platform_char *username, _In_opt_z_ const platform_char *domain, _In_opt_z_ const platform_char *password);

void _auth_dispose_credhandle(_In_ auth_configuration_t *configuration, _Inout_ CredHandle *credentials);

_Ret_maybenull_ SEC_WINNT_AUTH_IDENTITY *_auth_get_identity(_In_ auth_configuration_t *configuration, 
	_In_opt_z_ const platform_char *username, _In_opt_z_ const platform_char *domain, _In_opt_z_ const platform_char *password);

void _auth_dispose_identity(_In_ auth_configuration_t *configuration, _Inout_opt_ SEC_WINNT_AUTH_IDENTITY *identity);

int _auth_compare_context(_In_ CtxtHandle *one, _In_ CtxtHandle *two);

void _auth_dispose_context(_In_ auth_t *auth, _Inout_ CtxtHandle *context);

_Ret_maybenull_ SecBufferDesc *_auth_createbuffer(_In_ auth_t *auth);

_Ret_maybenull_ SecBufferDesc *_auth_createbuffer_size(_In_ auth_t *auth, unsigned long length);

_Ret_maybenull_ SecBufferDesc *_auth_createbuffer_string(_In_ auth_t *auth, _In_z_ const unsigned char *orig, unsigned long length);

void _auth_dispose_buffer(_Inout_ SecBufferDesc *buffer_desc);

#endif /* AUTH_SSPI_H */
