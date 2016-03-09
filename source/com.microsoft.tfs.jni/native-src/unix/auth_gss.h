/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#ifndef AUTH_GSS_H
#define AUTH_GSS_H

#include <gssapi.h>
#include <krb5.h>
#include <com_err.h>

#include "logger.h"

#ifdef __FreeBSD__
# define errcode_t long
#endif

#ifdef DYNAMIC_GSSAPI

# define KRB5_INIT_CONTEXT		configuration->krb5_init_context_func
# define KRB5_FREE_CONTEXT		configuration->krb5_free_context_func
# define KRB5_CC_DEFAULT		configuration->krb5_cc_default_func
# define KRB5_CC_CLOSE			configuration->krb5_cc_close_func
# define KRB5_CC_GET_PRINCIPAL	configuration->krb5_cc_get_principal_func
# define KRB5_FREE_PRINCIPAL	configuration->krb5_free_principal_func
# define KRB5_UNPARSE_NAME		configuration->krb5_unparse_name_func
# define KRB5_FREE_UNPARSED_NAME configuration->krb5_free_unparsed_name_func

# define GSS_INDICATE_MECHS		configuration->gss_indicate_mechs_func
# define GSS_RELEASE_OID_SET	configuration->gss_release_oid_set_func
# define GSS_IMPORT_NAME		configuration->gss_import_name_func
# define GSS_RELEASE_NAME		configuration->gss_release_name_func
# define GSS_INIT_SEC_CONTEXT	configuration->gss_init_sec_context_func
# define GSS_RELEASE_BUFFER		configuration->gss_release_buffer_func
# define GSS_DISPLAY_STATUS		configuration->gss_display_status_func

# ifdef HAVE_KRB5_SVC_GET_MSG
#  define KRB5_SVC_GET_MSG       configuration->krb5_svc_get_msg_func
#  define KRB5_FREE_STRING       configuration->krb5_free_string_func
# else
#  define ERROR_MESSAGE			configuration->error_message_func
# endif /* HAVE_KRB5_SVC_GET_MSG */

#else /* DYNAMIC_GSSAPI */

# define KRB5_INIT_CONTEXT		krb5_init_context
# define KRB5_FREE_CONTEXT		krb5_free_context
# define KRB5_CC_DEFAULT		krb5_cc_default
# define KRB5_CC_CLOSE			krb5_cc_close
# define KRB5_CC_GET_PRINCIPAL	krb5_cc_get_principal
# define KRB5_FREE_PRINCIPAL	krb5_free_principal
# define KRB5_UNPARSE_NAME		krb5_unparse_name
# define KRB5_FREE_UNPARSED_NAME krb5_free_unparsed_name

# define GSS_INDICATE_MECHS		gss_indicate_mechs
# define GSS_RELEASE_OID_SET	gss_release_oid_set
# define GSS_IMPORT_NAME		gss_import_name
# define GSS_RELEASE_NAME		gss_release_name
# define GSS_INIT_SEC_CONTEXT	gss_init_sec_context
# define GSS_RELEASE_BUFFER		gss_release_buffer
# define GSS_DISPLAY_STATUS		gss_display_status

# ifdef HAVE_KRB5_SVC_GET_MSG
#  define KRB5_SVC_GET_MSG       krb5_svc_get_msg
#  define KRB5_FREE_STRING       krb5_free_string
# else
#  define ERROR_MESSAGE			error_message
# endif /* HAVE_KRB5_SVC_GET_MSG */

#endif /* DYNAMIC_GSSAPI */

typedef struct
{
    /* Logger */
    logger_t *logger;

    /*
     * Pointers to the shared library functions.  Do not use the actual name
     * of the functions here, as they can be implemented as macros on some
     * platforms.  :/
     */
#ifdef DYNAMIC_GSSAPI
    /* Pointer to GSSAPI and Kerberos 5 shared libraries */
    void *gssapi_library;
    void *krb5_library;

    /* Pointers to Kerberos 5 functions */
    krb5_error_code (*krb5_init_context_func)(krb5_context *context);
    void (*krb5_free_context_func)(krb5_context context);
    krb5_error_code (*krb5_cc_default_func)(krb5_context context, krb5_ccache *cache);
    krb5_error_code (*krb5_cc_close_func)(krb5_context context, krb5_ccache cache);
    krb5_error_code (*krb5_cc_get_principal_func)(krb5_context context, krb5_ccache id,
        krb5_principal *principal);
    void (*krb5_free_principal_func)(krb5_context context, krb5_principal principal);
    krb5_error_code (*krb5_unparse_name_func)(krb5_context context,
        krb5_const_principal principal, char **name);
    void (*krb5_free_unparsed_name_func)(krb5_context context, char *val);

    /* Pointers to GSSAPI functions */
    OM_uint32 (*gss_indicate_mechs_func)(OM_uint32 *minor_status, gss_OID_set *mech_set);
    OM_uint32 (*gss_release_oid_set_func)(OM_uint32 *minor_status, gss_OID_set *mech_set);
    OM_uint32 (*gss_import_name_func)(OM_uint32 *minor_status, gss_buffer_t input_name_buffer,
        gss_OID input_name_type, gss_name_t *output_name);
    OM_uint32 (*gss_release_name_func)(OM_uint32 *minor_status, gss_name_t *name);
    OM_uint32 (*gss_init_sec_context_func)(OM_uint32 *minor_status, gss_cred_id_t claimant_cred_handle,
        gss_ctx_id_t *context_handle, gss_name_t target_name, gss_OID mech_type, OM_uint32 req_flags,
        OM_uint32 time_req, gss_channel_bindings_t input_chan_bindings, gss_buffer_t input_token,
        gss_OID *actual_mech_type, gss_buffer_t output_token, OM_uint32 *ret_flags, OM_uint32 *time_rec);
    OM_uint32 (*gss_release_buffer_func)(OM_uint32 *minor_status, gss_buffer_t buffer);
    OM_uint32 (*gss_display_status_func)(OM_uint32 *, OM_uint32, int, gss_OID, OM_uint32 *, gss_buffer_t);

    /* Error handling functions (support varies by platform) */
# ifdef HAVE_KRB5_SVC_GET_MSG
    krb5_error_code (*krb5_svc_get_msg_func)(const krb5_ui_4 msg_id, char ** msg_text);
    void (*krb5_free_string_func)(krb5_context, char *);
# else
    const char *(*error_message_func)(errcode_t code);
# endif /* HAVE_KRB5_SVC_GET_MSG */

#endif /* DYNAMIC_GSSAPI */
} auth_configuration_t;

typedef struct
{
    /*
     * The auth configuration
     */
    auth_configuration_t *configuration;

    /*
     * The configured mechanism (eg, "ntlm", "negotiate") and if it is
     * available on this system, as well as the chosen OID.
     */
    unsigned short mechanism;
    gss_OID mechanism_oid;

    /*
     * The name of the target system - ie, the hostname to authenticate to.
     */
    char *target;

    /*
     * The current GSS context.
     */
    gss_ctx_id_t context;

    /*
     * Whether we have completed authentication
     */
    unsigned short int complete;

    /*
     * Any error message from authentication.
     */
    char *error_message;
} auth_t;

#endif /* AUTH_GSS_H */
