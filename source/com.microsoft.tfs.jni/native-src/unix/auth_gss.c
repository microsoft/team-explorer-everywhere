/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * GSSAPI specific native authentication mechanisms.  (SPNEGO, Krb5, possibly
 * NTLM.)
 */

#include <stdlib.h>
#include <string.h>
#include <stdarg.h>
#include <stdio.h>
#include <gssapi.h>
#include <krb5.h>
#include <dlfcn.h>

/* These are required for OS X's SystemConfiguration (to get version number). */
#ifdef MACOS_X
# include <CoreServices/CoreServices.h>
# include <SystemConfiguration/SystemConfiguration.h>
#endif /* MACOS_X */

/* Required to use KRB5_SVC_GET_MSG when available. */
#ifdef HAVE_KRB5_SVC_GET_MSG
# include <krb5_svc.h>
#endif /* HAVE_KRB5_SVC_GET_MSG */

#include "auth_gss.h"
#include "auth.h"
#include "util.h"
#include "logger.h"

/*
 * Private: Get the error string for a Kerberos error code.  Must free with _tee_krb5_free_message.
 */
static const char unknownError[] = "Unknown error";
char * _tee_krb5_get_message(auth_configuration_t * configuration, krb5_context context, krb5_error_code code)
{
    char * message = NULL;

#ifdef HAVE_KRB5_SVC_GET_MSG
    KRB5_SVC_GET_MSG(code, &message);
#else
    message = (char *) ERROR_MESSAGE(code);
#endif /* HAVE_KRB5_SVC_GET_MSG */

    if (message != NULL)
    {
        return message;
    }

    return (char *) unknownError;
}

/*
 * Private: Free a string returned by _tee_krb5_get_message.
 */
void _tee_krb5_free_message(auth_configuration_t * configuration, krb5_context context, char * message)
{
    /* Never free our static message. */
    if (message == unknownError)
    {
        return;
    }

#ifdef HAVE_KRB5_SVC_GET_MSG
    KRB5_FREE_STRING(context, message);
#else
    /* com_err's error_message returns static; don't free */
#endif /* HAVE_KRB5_SVC_GET_MSG */
}

/*
 * Private: Gets the error string for a GSS error code.  Must free with _tee_gss_free_message.
 */
char * _tee_gss_get_message(auth_configuration_t * configuration, OM_uint32 code)
{
    /*
     * HACK: the proper thing to do is call GSS_DISPLAY_STATUS in a loop to get all the
     * subcodes (see Solaris's documentation for gss_display_status), but it's very rare
     * that subsequent strings are needed and probably not worth the string management.
     * This implementation just returns the first message.
     */
    gss_buffer_desc buffer;
    OM_uint32 context = 0;
    OM_uint32 minor = 0;

    if (GSS_DISPLAY_STATUS(&minor, code, GSS_C_GSS_CODE, GSS_C_NO_OID, &context, &buffer) == GSS_S_COMPLETE)
    {
        char * message = tee_strndup((char *) buffer.value, buffer.length);
        GSS_RELEASE_BUFFER(&minor, &buffer);

        return message;
    }

    return (char *) unknownError;
}

/*
 * Private: Free a string returned by _tee_gss_get_message.
 */
void _tee_gss_free_message(auth_configuration_t * configuration, char * message)
{
    /* Never free NULL or our static message. */
    if (message == NULL || message == unknownError)
    {
        return;
    }

    free(message);
}
/*
 * Some platforms don't follow the library dependency chain in
 * dlsym(3).  Most platforms will resolve krb5_* functions from the
 * GSSAPI library.  Some (notably, Solaris) require you to resolve these
 * symbols against the Kerberos 5 libraries.
 */
#ifdef __solaris__
# define DYNAMIC_KRB5		1
#else
# define DYNAMIC_KRB5		0
#endif /* SOLARIS */

/* GSSAPI library names.
 *
 * libgssapi_krb5.so:       Most UNIXes)
 * libgssapi_krb5.dylib:    Mac OS X
 * libgss.so:               Solaris
 */
char *auth_gssapi_libraries[] = { "libgssapi_krb5.so", "libgssapi_krb5.dylib", "libgss.so", NULL };

/*
 * Kerberos 5 library names: some operating systems implement
 * dlsym(3) such that it doesn't search dependent libraries, and
 * we must search them for symbols explicitly.
 */
#ifdef DYNAMIC_KRB5
/* libkrb5.so: Solaris */
char *auth_krb5_libraries[] = { "libkrb5.so", NULL };
#endif /* DYNAMIC_KRB5 */

/* OID for Kerberos5 */
static gss_OID_desc auth_oid_kerberos5 = { 9, (void *) "\x2a\x86\x48\x86\xf7\x12\x01\x02\x02" };

/* OID for SPNEGO */
static gss_OID_desc auth_oid_negotiate = { 6, (void *) "\x2b\x06\x01\x05\x05\x02" };

/* OID for NTLM */
static gss_OID_desc auth_oid_ntlm = { 10, (void *) "\x2b\x06\x01\x04\x01\x82\x37\x02\x02\x0a" };

/* Hostbased service */
static gss_OID_desc auth_hostbased_service = { 6, (void *) "\x2b\x06\x01\x05\x06\x02" };

/*
 * Private: Resolve a mechanism to an OID.
 */
gss_OID _auth_get_oid(auth_configuration_t *configuration, mechanism_t mechanism)
{
    OM_uint32 statusMajor, statusMinor;
    gss_OID mechanism_oid = NULL, item;
    gss_OID_set mechanismList;
    size_t i;
    int found = 0;
    const char *oidname;

    gss_OID negotiateOids[] = { &auth_oid_negotiate, &auth_oid_kerberos5, NULL };
    gss_OID *queryOids, *oid;

    if (configuration == NULL)
    {
        return NULL;
    }

    if (mechanism == AUTH_MECHANISM_NEGOTIATE)
    {
        logger_write(configuration->logger, LOGLEVEL_DEBUG, "Querying available mechanisms for negotiate");
        queryOids = negotiateOids;
    }
    else
    {
        return NULL;
    }

    /* Query supported mechanisms, hope for SPNEGO */
    statusMajor = GSS_INDICATE_MECHS(&statusMinor, &mechanismList);

    if (GSS_ERROR(statusMajor))
    {
        char * details = _tee_gss_get_message(configuration, statusMinor);
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not query mechanisms for negotiate: %x.%x (%s)",
            statusMajor, statusMinor, details);
        _tee_gss_free_message(configuration, details);

        return NULL;
    }

    if (mechanismList != NULL)
    {
        /* Walk the list of OIDs we're looking for */
        for (oid = queryOids; oid != NULL; oid++)
        {
            for (i = 0; i < mechanismList->count; i++)
            {
                item = &mechanismList->elements[i];

                if (item->length == (*oid)->length && memcmp(item->elements, (*oid)->elements, item->length) == 0)
                {
                    if (*oid == &auth_oid_negotiate)
                    {
                        oidname = "negotiate";
                    }
                    else if (*oid == &auth_oid_kerberos5)
                    {
                        oidname = "kerberos5";
                    }
                    else
                    {
                        oidname = "(unknown)";
                    }

                    logger_write(configuration->logger, LOGLEVEL_DEBUG, "Found OID for mechanism %s", oidname);

                    mechanism_oid = *oid;

                    found = 1;
                    break;
                }
            }

            if (found)
            {
                break;
            }
        }

        GSS_RELEASE_OID_SET(&statusMinor, &mechanismList);
    }

    return mechanism_oid;
}

/*
 * Private: wraps dlsym() with a debug message
 */
void *_auth_configure_symbol(auth_configuration_t *configuration, void *library, const char *symbolname)
{
    void *symbol;

    if ((symbol = dlsym(library, symbolname)) == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not locate %s symbol: %s", symbolname, dlerror());
    }
    else
    {
        logger_write(configuration->logger, LOGLEVEL_DEBUG, "Loaded symbol %s", symbolname);
    }

    return symbol;
}

auth_configuration_t *auth_configure(logger_t *logger)
{
    auth_configuration_t *configuration;
    char **gssapi_library_name, **krb5_library_name;
    void *gssapi_library, *krb5_library;

    /* GSSAPI crashes on 64-bit MacOS in Leopard.  See bug 2260. */
#if defined(MACOS_X) && (defined(__LP64__) || defined(__ppc64__))
    SInt32 os_version;

    /* Try to determine the current os version */
    if (Gestalt(gestaltSystemVersion, &os_version) != 0)
    {
        os_version = 0;
    }

    if (os_version < 0x1060)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Kerberos authentication is only supported on 64-bit platforms beginning with Mac OS 10.6.");
        return NULL;
    }
#endif

    if ((configuration = (auth_configuration_t *) malloc(sizeof(auth_configuration_t))) == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Could not malloc");
        return NULL;
    }

    /* Zero all values */
    memset(configuration, 0, sizeof(auth_configuration_t));

    configuration->logger = logger;

#ifdef DYNAMIC_GSSAPI
    for (gssapi_library_name = auth_gssapi_libraries; *gssapi_library_name != NULL; gssapi_library_name++)
    {
        if ((gssapi_library = dlopen(*gssapi_library_name, RTLD_NOW)) != NULL)
        {
            logger_write(configuration->logger, LOGLEVEL_INFO, "Loaded GSSAPI library: %s", *gssapi_library_name);

            configuration->gssapi_library = gssapi_library;
            break;
        }
        else
        {
            logger_write(configuration->logger, LOGLEVEL_DEBUG, "Could not load GSSAPI library: %s (%s)", *gssapi_library_name, dlerror());
        }
    }

    if (configuration->gssapi_library == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not load GSSAPI library, Kerberos authentication disabled");

        free(configuration);
        return NULL;
    }

#if DYNAMIC_KRB5
    for (krb5_library_name = auth_krb5_libraries; *krb5_library_name != NULL; krb5_library_name++)
    {
        if ((krb5_library = dlopen(*krb5_library_name, RTLD_NOW)) != NULL)
        {
            logger_write(configuration->logger, LOGLEVEL_INFO, "Loaded Kerberos 5 library: %s", *krb5_library_name);

            configuration->krb5_library = krb5_library;
            break;
        }
        else
        {
            logger_write(configuration->logger, LOGLEVEL_DEBUG, "Could not load Kerberos 5 library: %s", *krb5_library_name);
        }
    }

    if (configuration->krb5_library == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not load Kerberos 5 library, Kerberos authentication disabled");

        free(configuration);
        return NULL;
    }
#else
    /* Let dlsym() find the Kerberos 5 through GSSAPI dependencies. */
    configuration->krb5_library = configuration->gssapi_library;
#endif /* DYNAMIC_KRB5 */

    /*
     * Note that even though the krb5_* functions are in libkrb5 (not libgssapi),
     * GSSAPI takes Kerberos 5 as a dependency, thus dysym(3) will resolve them.
     * This is expected to be portable.
     */
    if (
        (configuration->krb5_init_context_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_init_context")) == NULL ||
        (configuration->krb5_free_context_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_free_context")) == NULL ||
        (configuration->krb5_cc_default_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_cc_default")) == NULL ||
        (configuration->krb5_cc_close_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_cc_close")) == NULL ||
        (configuration->krb5_cc_get_principal_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_cc_get_principal")) == NULL ||
        (configuration->krb5_free_principal_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_free_principal")) == NULL ||
        (configuration->krb5_unparse_name_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_unparse_name")) == NULL ||
        (configuration->krb5_free_unparsed_name_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_free_unparsed_name")) == NULL ||

        (configuration->gss_indicate_mechs_func = _auth_configure_symbol(configuration, configuration->gssapi_library, "gss_indicate_mechs")) == NULL ||
        (configuration->gss_release_oid_set_func = _auth_configure_symbol(configuration, configuration->gssapi_library, "gss_release_oid_set")) == NULL ||
        (configuration->gss_import_name_func = _auth_configure_symbol(configuration, configuration->gssapi_library, "gss_import_name")) == NULL ||
        (configuration->gss_release_name_func = _auth_configure_symbol(configuration, configuration->gssapi_library, "gss_release_name")) == NULL ||
        (configuration->gss_init_sec_context_func = _auth_configure_symbol(configuration, configuration->gssapi_library, "gss_init_sec_context")) == NULL ||
        (configuration->gss_release_buffer_func = _auth_configure_symbol(configuration, configuration->gssapi_library, "gss_release_buffer")) == NULL ||
        (configuration->gss_display_status_func = _auth_configure_symbol(configuration, configuration->gssapi_library, "gss_display_status")) == NULL ||

#ifdef HAVE_KRB5_SVC_GET_MSG
        (configuration->krb5_svc_get_msg_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_svc_get_msg")) == NULL ||
        (configuration->krb5_free_string_func = _auth_configure_symbol(configuration, configuration->krb5_library, "krb5_free_string")) == NULL ||
#else
        (configuration->error_message_func = _auth_configure_symbol(configuration, configuration->krb5_library, "error_message")) == NULL
#endif /* HAVE_KRB5_SVC_GET_MSG */
    )
    {
        dlclose(configuration->gssapi_library);

#if DYNAMIC_KRB5
        dlclose(configuration->krb5_library);
#endif /* DYNAMIC_KRB5 */

        free(configuration);

        return NULL;
    }
#endif /* DYNAMIC_GSSAPI */

    return configuration;
}

int auth_available(auth_configuration_t *configuration, mechanism_t mechanism)
{
    if (configuration == NULL)
    {
        return 0;
    }

    gss_OID mechanism_oid = _auth_get_oid(configuration, mechanism);

    if (mechanism_oid == NULL)
    {
        return 0;
    }

    /* Ensure we have credentials for this mechanism */
    return auth_supports_credentials_default(configuration, mechanism);
}

int auth_supports_credentials_default(auth_configuration_t *configuration, mechanism_t mechanism)
{
    platform_char * creds = auth_get_credentials_default(configuration, mechanism);
    int ret = (creds != NULL);
    free(creds);
    return ret;
}

int auth_supports_credentials_specified(auth_configuration_t *configuration, mechanism_t mechanism)
{
    /* GSSAPI only supports logged in (kerberos 5 ticketed) credentials */
    return 0;
}

platform_char *auth_get_credentials_default(auth_configuration_t *configuration, mechanism_t mechanism)
{
    krb5_error_code code;
    krb5_context context;
    krb5_ccache ccache;
    krb5_principal principal;
    char *details = NULL;
    char *principal_name = NULL;
    char *principal_name_copy = NULL;

    if (configuration == NULL || mechanism != AUTH_MECHANISM_NEGOTIATE)
    {
        return NULL;
    }

    logger_write(configuration->logger, LOGLEVEL_DEBUG, "Querying default kerberos ticket credentials");

    if ((code = KRB5_INIT_CONTEXT(&context)) != 0)
    {
        details = _tee_krb5_get_message(configuration, context, code);
        logger_write(configuration->logger, LOGLEVEL_WARN, "No kerberos5 context available (%s)", details);
        _tee_krb5_free_message(configuration, context, details);

        return NULL;
    }

    if ((code = KRB5_CC_DEFAULT(context, &ccache)) != 0)
    {
        details = _tee_krb5_get_message(configuration, context, code);
        logger_write(configuration->logger, LOGLEVEL_INFO, "No kerberos 5 credentials available (%s)", details);
        _tee_krb5_free_message(configuration, context, details);

        KRB5_FREE_CONTEXT(context);
        return NULL;
    }

    if ((code = KRB5_CC_GET_PRINCIPAL(context, ccache, &principal)) != 0)
    {
        details = _tee_krb5_get_message(configuration, context, code);
        logger_write(configuration->logger, LOGLEVEL_WARN, "Could not get principal for kerberos 5 credentials (%s)",
            details);
        _tee_krb5_free_message(configuration, context, details);

        KRB5_CC_CLOSE(context, ccache);
        KRB5_FREE_CONTEXT(context);
        return NULL;
    }

    if ((code = KRB5_UNPARSE_NAME(context, principal, &principal_name)) != 0)
    {
        details = _tee_krb5_get_message(configuration, context, code);
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Could not unparse kerberos 5 credential name (%s)",
            details);
        _tee_krb5_free_message(configuration, context, details);

        KRB5_FREE_PRINCIPAL(context, principal);
        KRB5_CC_CLOSE(context, ccache);
        KRB5_FREE_CONTEXT(context);
        return NULL;
    }

    // Duplicate the string so the caller can use free()
    principal_name_copy = strdup(principal_name);

    KRB5_FREE_UNPARSED_NAME(context, principal_name);
    KRB5_FREE_PRINCIPAL(context, principal);
    KRB5_CC_CLOSE(context, ccache);
    KRB5_FREE_CONTEXT(context);

    if (principal_name_copy == NULL)
    {
        logger_write(configuration->logger, LOGLEVEL_ERROR, "Couldn't duplicate principal_name");
        return NULL;
    }

    logger_write(configuration->logger, LOGLEVEL_DEBUG, "Determined kerberos 5 default principal to be %s",
        principal_name_copy);

    return principal_name_copy;
}

auth_t *auth_initialize(auth_configuration_t *configuration, mechanism_t mechanism)
{
    auth_t *auth;

    if (configuration == NULL)
    {
        return NULL;
    }

    if (mechanism == AUTH_MECHANISM_NEGOTIATE)
    {
        if ((auth = (auth_t *) malloc(sizeof(auth_t))) == NULL)
        {
            return NULL;
        }

        /* Zero all values */
        memset(auth, 0, sizeof(auth_t));

        auth->configuration = configuration;
        auth->mechanism = AUTH_MECHANISM_NEGOTIATE;
        auth->mechanism_oid = _auth_get_oid(configuration, AUTH_MECHANISM_NEGOTIATE);

        if (auth->mechanism_oid == NULL)
        {
            free(auth);
            return NULL;
        }

        return auth;
    }

    return NULL;
}

auth_configuration_t *auth_get_configuration(auth_t *auth)
{
    if (auth == NULL)
    {
        return NULL;
    }

    return auth->configuration;
}

void auth_set_error(auth_t *auth, const char *fmt, ...)
{
    va_list ap;

    if (auth == NULL)
    {
        return;
    }

    if (fmt == NULL)
    {
        auth->error_message = strdup("(unknown)");
        return;
    }

    va_start(ap, fmt);
    auth->error_message = tee_vsprintf(fmt, ap);
    va_end(ap);
}

const char *auth_get_error(auth_t *auth)
{
    if (auth == NULL)
    {
        return NULL;
    }

    return auth->error_message;
}

void auth_set_target(auth_t *auth, const char *target)
{
    if (auth == NULL)
    {
        return;
    }

    auth->target = (target != NULL) ? strdup(target) : NULL;
}

void auth_set_localhost(auth_t *auth, const char *localhost)
{
    /* GSSAPI does not allow us to override local hostname */
    return;
}

void auth_set_credentials_default(auth_t *auth)
{
    /* GSSAPI only supports logged in (kerberos 5 ticketed) credentials */
    return;
}

void auth_set_credentials(auth_t *auth, const platform_char *username, const platform_char *domain,
    const platform_char *password)
{
    /* GSSAPI only supports logged in (kerberos 5 ticketed) credentials */
    return;
}

int auth_get_token(auth_t *auth, const void *input, unsigned int inputlen, void **output, unsigned int *outputlen)
{
    auth_configuration_t *configuration;

    OM_uint32 statusMajor, statusMinor, statusIgnored;
    gss_buffer_desc target_buffer = GSS_C_EMPTY_BUFFER;
    gss_buffer_desc input_token = GSS_C_EMPTY_BUFFER;
    gss_buffer_desc output_token = GSS_C_EMPTY_BUFFER;
    gss_buffer_t input_token_ptr = GSS_C_NO_BUFFER;
    gss_name_t server;

    if (auth == NULL || auth->configuration == NULL)
    {
        return 0;
    }

    configuration = auth->configuration;

    if (output == NULL || outputlen == NULL)
    {
        auth_set_error(auth, "output buffer undefined");
        return 0;
    }

    if (auth->target == NULL)
    {
        auth_set_error(auth, "no target specified");
        return 0;
    }

    *output = NULL;
    *outputlen = 0;

    logger_write(configuration->logger, LOGLEVEL_DEBUG, "Beginning authentication for %s", auth->target);

    target_buffer.value = (auth->target != NULL) ? (void *) auth->target : "";
    target_buffer.length = (auth->target != NULL) ? strlen(auth->target) + 1 : 0;

    statusMajor = GSS_IMPORT_NAME(&statusMinor, &target_buffer, &auth_hostbased_service, &server);

    if (GSS_ERROR(statusMajor))
    {
        char * details = _tee_gss_get_message(configuration, statusMinor);
        auth_set_error(auth, "could not locate principal: %x.%x (%s)", statusMajor, statusMinor, details);
        _tee_gss_free_message(configuration, details);

        return 0;
    }

    if (inputlen > 0 && input != NULL)
    {
        input_token.value = (void *) input;
        input_token.length = inputlen + 1;

        input_token_ptr = &input_token;
    }
    else
    {
        /* Sanity check */
        if (auth->context != GSS_C_NO_CONTEXT)
        {
            auth_set_error(auth, "could not restart authentication");
            GSS_RELEASE_NAME(&statusMinor, &server);

            return 0;
        }
    }

    statusMajor = GSS_INIT_SEC_CONTEXT(&statusMinor, GSS_C_NO_CREDENTIAL, &auth->context, server, auth->mechanism_oid,
        GSS_C_DELEG_FLAG | GSS_C_MUTUAL_FLAG, GSS_C_INDEFINITE, GSS_C_NO_CHANNEL_BINDINGS, input_token_ptr, NULL,
        &output_token, NULL, NULL);

    if (GSS_ERROR(statusMajor))
    {
        char * details = _tee_gss_get_message(configuration, statusMinor);
        auth_set_error(auth, "negotiate failure: %x.%x (%s)", statusMajor, statusMinor, details);
        _tee_gss_free_message(configuration, details);

        GSS_RELEASE_NAME(&statusMinor, &server);
        return 0;
    }

    if (statusMajor == GSS_S_COMPLETE)
    {
        logger_write(configuration->logger, LOGLEVEL_DEBUG, "Negotiation is complete");
        auth->complete = 1;
    }

    *outputlen = output_token.length;

    if (output_token.length > 0)
    {
        if ((*output = (void *) malloc(output_token.length)) == NULL
            || memcpy(*output, output_token.value, output_token.length) == NULL)
        {
            auth_set_error(auth, "could not malloc");
            return 0;
        }
    }
    else
    {
        *output = NULL;
    }

    GSS_RELEASE_NAME(&statusMinor, &server);
    GSS_RELEASE_BUFFER(&statusMinor, (gss_buffer_t) & output_token);

    return 1;
}

int auth_is_complete(auth_t *auth)
{
    if (auth == NULL)
    {
        return 1;
    }

    return auth->complete;
}

void auth_dispose(auth_t *auth)
{
    if (auth == NULL)
    {
        return;
    }

    if (auth->target != NULL)
    {
        free(auth->target);
    }

    if (auth->error_message != NULL)
    {
        free(auth->error_message);
    }

    free(auth);
}

void auth_dispose_configuration(auth_configuration_t *configuration)
{
    if (configuration == NULL)
    {
        return;
    }

#ifdef DYNAMIC_GSSAPI
    if (configuration->gssapi_library != NULL)
    {
        dlclose(configuration->gssapi_library);
        configuration->gssapi_library = NULL;
    }

# ifdef DYNAMIC_KRB5
    if (configuration->krb5_library != NULL)
    {
        dlclose(configuration->krb5_library);
        configuration->krb5_library = NULL;
    }
# endif /* DYNAMIC_KRB5 */
#endif /* DYNAMIC_GSSAPI */
}
