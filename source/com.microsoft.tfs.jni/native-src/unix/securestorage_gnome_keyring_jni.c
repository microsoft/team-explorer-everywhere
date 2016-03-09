/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/**********************************************************************************
 * DO NOT USE THESE FUNCTIONS until you solve the problems in the WARNING
 * below through clever Eclipse/Java tricks, or otherwise.
 **********************************************************************************
 */

/*
 * JNI functions that do secure password storage for GNOME Keyring (Linux, others).
 *
 * WARNING!
 *
 * Keyring functions must always be called on the UI thread where GTK/GDK is running
 * (if the application has one).  Calling functions from other threads can cause
 * the JVM to crash, because DBus (which the library uses to communicate with the Keyring
 * daemon) isn't thread-safe.
 */
#include <gnome-keyring.h>
#include <jni.h>

#include "native_securestorage_gnome_keyring_methods.h"
#include "util.h"
#include "logger.h"

/* The value we always write as the "authtype" parameter for saving network passwords. */
#define TEE_AUTHTYPE "TeamExplorerEverywhere"

/*
 * gnome-keyring first defined GNOME_KEYRING_RESULT_NO_MATCH in version 2.20.1.
 * Previous versions returned a GNOME_KEYRING_RESULT_OK and a NULL list or password
 * when none matched.  We need to compile with old GNOME versions but handle the
 * new value when running with newer libraries (otherwise error handling is tricky).
 */
#define GNOME_KEYRING_RESULT_NO_MATCH 9

/* Application name set via g_set_application_name() and g_set_prgname() if one was not already set. */
#define APP_NAME "Team Explorer Everywhere"

/*
 * Tests whether the default keyring is locked for cases when UI is not desired
 * (for example, so we don't cause a pop-up dialog during scripts).
 */
gboolean isDefaultKeyringLocked(logger_t * logger)
{
    GnomeKeyringInfo * info = NULL;
    GnomeKeyringResult result = gnome_keyring_get_info_sync(NULL, &info);

    gboolean ret = FALSE;
    if (result == GNOME_KEYRING_RESULT_OK)
    {
        ret = gnome_keyring_info_get_is_locked(info);

        logger_write(logger, LOGLEVEL_DEBUG, "Default GNOME keyring locked: %s", (ret == TRUE) ? "true" : "false");
    }
    else
    {
        logger_write(logger, LOGLEVEL_DEBUG,
            "Could not test whether default GNOME keyring is locked (result=%u), assuming locked", result);
    }

    if (info != NULL)
    {
        gnome_keyring_info_free(info);
    }

    return ret;
}

/*
 * Queries whether the GNOME Keyring implementation is available and we can communicate with the daemon.
 * Returns JNI_TRUE on success, JNI_FALSE on failure.
 *
 * This function must be called from Java before other functions are used, so
 * the glib type system is initialized properly.
 */
JNIEXPORT jboolean Java_com_microsoft_tfs_jni_internal_securestorage_NativeSecureStorageGNOMEKeyring_nativeImplementationAvailable(
    JNIEnv *env, jclass cls)
{
    /* g_get_prgname() may have been called by Eclipse or some other GTK host */
    if (g_get_prgname() == NULL)
    {
        g_set_prgname(APP_NAME);
    }

    if (g_get_application_name() == NULL)
    {
        g_set_application_name(APP_NAME);
    }

    return gnome_keyring_is_available() ? JNI_TRUE : JNI_FALSE;
}

/*
 * Sets the password for the given internet protocol for the given user.
 * Returns JNI_TRUE on success, JNI_FALSE on failure.
 *
 * jProtocolName: the name of the protocol (eg, "http" or "https") or NULL
 * jServer: the server name, may not be null
 * jPort: the port number, or 0
 * jPath: the path to connect to, or NULL
 * jUsername: the NTLM username to connect as, may not be NULL
 * jDomain: the NTLM domain to connect as, may be NULL
 * jId: domain-specific data (not the user domain), or NULL
 * jPlaintext: a byte array containing the plaintext password, may not be null
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_securestorage_NativeSecureStorageGNOMEKeyring_nativeEncryptPassword(
    JNIEnv *env, jclass cls, jstring jProtocolName, jstring jServer, jint jPort, jstring jPath, jstring jUsername,
    jstring jDomain, jstring jId, jstring jPlaintext, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    const char *protocolName;
    const char *server;
    guint32 port = (jPort >= 0) ? jPort : 0;
    const char *path;
    const char *username;
    const char *domain;
    const char *plaintext;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        if ((logger = logger_initialize(jvm, "com.microsoft.tfs.jni.natives.NativeSecureStorageGNOMEKeyringMethods"))
            == NULL)
        {
            return JNI_FALSE;
        }
    }

    if (jServer == NULL || jUsername == NULL || jPlaintext == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: server and username and plaintext must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    /*
     * As of 2010/08/20, GNOME has no command-line utilities to unlock keyrings.
     * Test whether the default keyring is locked for cases when UI is not desired
     * (for example, so we don't cause a pop-up dialog during scripts).
     */
    if (jAllowUi == JNI_FALSE && isDefaultKeyringLocked(logger))
    {
        logger_write(logger, LOGLEVEL_WARN,
            "Default GNOME keyring is locked and no user interface available, can't save password");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    protocolName = (jProtocolName != NULL) ? javaStringToPlatformChars(env, jProtocolName) : NULL;
    server = javaStringToPlatformChars(env, jServer);
    path = (jPath != NULL) ? javaStringToPlatformChars(env, jPath) : NULL;
    username = javaStringToPlatformChars(env, jUsername);
    domain = (jDomain != NULL) ? javaStringToPlatformChars(env, jDomain) : NULL;
    plaintext = javaStringToPlatformChars(env, jPlaintext);

    logger_write(
        logger,
        LOGLEVEL_DEBUG,
        "Saving entry keyring [%s] protocol [%s] server [%s] object [%s] authtype [%s] port [%u] user [%s] domain [%s]",
        NULL, protocolName, server, path, TEE_AUTHTYPE, port, username, domain);

    guint32 item_id = 0;
    GnomeKeyringResult result = gnome_keyring_set_network_password_sync(NULL, username, domain, server, path,
        protocolName, TEE_AUTHTYPE, port, plaintext, &item_id);

    if (result == GNOME_KEYRING_RESULT_OK)
    {
        logger_write(logger, LOGLEVEL_DEBUG, "  Saved item_id [%u]", item_id);
    }
    else if (result != GNOME_KEYRING_RESULT_CANCELLED)
    {
        logger_write(logger, LOGLEVEL_WARN, "Could not save GNOME keyring entry for %s (result=%u)", server, result);
    }

    /* Free memory. */

    if (jProtocolName != NULL)
    {
        releasePlatformChars(env, jProtocolName, protocolName);
    }

    releasePlatformChars(env, jServer, server);

    if (jPath != NULL)
    {
        releasePlatformChars(env, jPath, path);
    }

    releasePlatformChars(env, jUsername, username);

    if (jDomain != NULL)
    {
        releasePlatformChars(env, jDomain, domain);
    }

    releasePlatformChars(env, jPlaintext, plaintext);

    logger_dispose(logger);

    return (result == GNOME_KEYRING_RESULT_OK) ? JNI_TRUE : JNI_FALSE;
}

/*
 * Retrieves the plaintext password for the given internet protocol for the
 * given user.  Returns a string containing the password on success, returns
 * NULL on failure.
 *
 * jProtocolName: the name of the protocol (eg, "http" or "https") or NULL
 * jServer: the server name, may not be null
 * jPort: the port number, or 0
 * jPath: the path to connect to, or NULL
 * jUsername: the NTLM username to connect as, may not be NULL
 * jDomain: the NTLM domain to connect as, may be NULL
 * jId: domain-specific data (not the user domain), or NULL
 */
JNIEXPORT jstring JNICALL Java_com_microsoft_tfs_jni_internal_securestorage_NativeSecureStorageGNOMEKeyring_nativeDecryptPassword(
    JNIEnv *env, jclass cls, jstring jProtocolName, jstring jServer, jint jPort, jstring jPath, jstring jUsername,
    jstring jDomain, jstring jId, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    const char *protocolName;
    const char *server;
    guint32 port = (jPort >= 0) ? jPort : 0;
    const char *path;
    const char *username;
    const char *domain;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        if ((logger = logger_initialize(jvm, "com.microsoft.tfs.jni.natives.NativeSecureStorageGNOMEKeyringMethods"))
            == NULL)
        {
            return JNI_FALSE;
        }
    }

    if (jServer == NULL || jUsername == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: server and username must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    /*
     * As of 2010/08/20, GNOME has no command-line utilities to unlock keyrings.
     * Test whether the default keyring is locked for cases when UI is not desired
     * (for example, so we don't cause a pop-up dialog during scripts).
     */
    if (jAllowUi == JNI_FALSE && isDefaultKeyringLocked(logger))
    {
        logger_write(logger, LOGLEVEL_WARN,
            "Default GNOME keyring is locked and no user interface available, can't read password");
        logger_dispose(logger);

        return NULL;
    }

    protocolName = (jProtocolName != NULL) ? javaStringToPlatformChars(env, jProtocolName) : NULL;
    server = javaStringToPlatformChars(env, jServer);
    path = (jPath != NULL) ? javaStringToPlatformChars(env, jPath) : NULL;
    username = javaStringToPlatformChars(env, jUsername);
    domain = (jDomain != NULL) ? javaStringToPlatformChars(env, jDomain) : NULL;

    logger_write(
        logger,
        LOGLEVEL_DEBUG,
        "Searching for items that match keyring [%s] protocol [%s] server [%s] object [%s] authtype [%s] port [%u] user [%s] domain [%s]",
        NULL, protocolName, server, path, TEE_AUTHTYPE, port, username, domain);

    /*
     * The search returns a list because there may be multiple matching items.  The list is ordered
     * so that the items which match the fewest fields come first.  Most specific matches
     * (more field matches) are later.
     */

    GList * results = NULL;
    GnomeKeyringResult result = gnome_keyring_find_network_password_sync(username, domain, server, path, protocolName,
        TEE_AUTHTYPE, port, &results);

    /* Our return value. */
    jstring plaintext = NULL;

    switch (result)
    {
    case GNOME_KEYRING_RESULT_OK:
        /* Skip NULL results which we might get from older GNOME keyring versions. */
        if (results != NULL)
        {
            logger_write(logger, LOGLEVEL_DEBUG, "Search returned %u matches", g_list_length(results));

            if (g_list_length(results) > 0)
            {
                /* Most specific item is at the end. */
                GList * current = results;
                while (current != NULL)
                {
                    GnomeKeyringNetworkPasswordData * data = (GnomeKeyringNetworkPasswordData *) current->data;

                    if (data != NULL)
                    {
                        /* Log each item */
                        logger_write(
                            logger,
                            LOGLEVEL_DEBUG,
                            "  Found item in keyring [%s] id [%u] protocol [%s] server [%s] object [%s] authtype [%s] port [%u] user [%s] domain [%s]",
                            data->keyring, data->item_id, data->protocol, data->server, data->object, data->authtype,
                            data->port, data->user, data->domain);

                        /* If this is the last item in the list, save it. */
                        if (g_list_next(current) == NULL)
                        {
                            if (data->password != NULL)
                            {
                                logger_write(logger, LOGLEVEL_DEBUG, "  Choosing item id [%u]", data->item_id);

                                plaintext = platformCharsToJavaString(env, data->password);
                            }
                            else
                            {
                                logger_write(logger, LOGLEVEL_WARN,
                                    "Got NULL password string in GnomeKeyringNetworkPasswordData item");
                            }

                            break;
                        }
                    }
                    else
                    {
                        logger_write(logger, LOGLEVEL_WARN, "Got NULL data item from result, skipping");
                    }

                    current = g_list_next(current);
                }
            }

            gnome_keyring_network_password_list_free(results);
        }
        break;
    case GNOME_KEYRING_RESULT_NO_MATCH:
        // Normal for no match.  Ignore.
        break;
    case GNOME_KEYRING_RESULT_CANCELLED:
        // User canceled UI.  Ignore.
        break;
    default:
        logger_write(logger, LOGLEVEL_WARN, "Could not save GNOME keyring entry for %s (result=%u)", server, result);
        break;
    }

    /* Free memory. */

    if (jProtocolName != NULL)
    {
        releasePlatformChars(env, jProtocolName, protocolName);
    }

    releasePlatformChars(env, jServer, server);

    if (jPath != NULL)
    {
        releasePlatformChars(env, jPath, path);
    }

    releasePlatformChars(env, jUsername, username);

    if (jDomain != NULL)
    {
        releasePlatformChars(env, jDomain, domain);
    }

    logger_dispose(logger);

    return plaintext;
}

/*
 * Removes the password for the given internet protocol for the given user.
 * Returns JNI_TRUE on success and JNI_FALSE on failure.
 *
 * jProtocolName: the name of the protocol (eg, "http" or "https") or NULL
 * jServer: the server name, may not be null
 * jPort: the port number, or 0
 * jPath: the path to connect to, or NULL
 * jUsername: the NTLM username to connect as, may not be NULL
 * jDomain: the NTLM domain to connect as, may be NULL
 * jId: domain-specific data (not the user domain), or NULL
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_securestorage_NativeSecureStorageGNOMEKeyring_nativeRemovePassword(
    JNIEnv *env, jclass cls, jstring jProtocolName, jstring jServer, jint jPort, jstring jPath, jstring jUsername,
    jstring jDomain, jstring jId, jboolean jAllowUi)
{
    JavaVM *jvm;
    logger_t *logger = NULL;

    const char *protocolName;
    const char *server;
    guint32 port = (jPort >= 0) ? jPort : 0;
    const char *path;
    const char *username;
    const char *domain;

    /* Setup logging */
    if ((*env)->GetJavaVM(env, &jvm) == 0)
    {
        if ((logger = logger_initialize(jvm, "com.microsoft.tfs.jni.natives.NativeSecureStorageGNOMEKeyringMethods"))
            == NULL)
        {
            return JNI_FALSE;
        }
    }

    if (jServer == NULL || jUsername == NULL)
    {
        logger_write(logger, LOGLEVEL_ERROR, "Invalid usage: server and username must not be null");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    /*
     * As of 2010/08/20, GNOME has no command-line utilities to unlock keyrings.
     * Test whether the default keyring is locked for cases when UI is not desired
     * (for example, so we don't cause a pop-up dialog during scripts).
     */
    if (jAllowUi == JNI_FALSE && isDefaultKeyringLocked(logger))
    {
        logger_write(logger, LOGLEVEL_WARN,
            "Default GNOME keyring is locked and no user interface available, can't remove password");
        logger_dispose(logger);

        return JNI_FALSE;
    }

    protocolName = (jProtocolName != NULL) ? javaStringToPlatformChars(env, jProtocolName) : NULL;
    server = javaStringToPlatformChars(env, jServer);
    path = (jPath != NULL) ? javaStringToPlatformChars(env, jPath) : NULL;
    username = javaStringToPlatformChars(env, jUsername);
    domain = (jDomain != NULL) ? javaStringToPlatformChars(env, jDomain) : NULL;

    logger_write(
        logger,
        LOGLEVEL_DEBUG,
        "Searching for items that match keyring [%s] protocol [%s] server [%s] object [%s] authtype [%s] port [%u] user [%s] domain [%s]",
        NULL, protocolName, server, path, TEE_AUTHTYPE, port, username, domain);

    /*
     * There may be multiple matching items.  Delete them all.
     */

    GList * results = NULL;
    GnomeKeyringResult result = gnome_keyring_find_network_password_sync(username, domain, server, path, protocolName,
        TEE_AUTHTYPE, port, &results);

    /* If any fail to delete or an error happens, this goes false. */
    jboolean ret = JNI_TRUE;

    switch (result)
    {
    case GNOME_KEYRING_RESULT_OK:
        /* Skip NULL results which we might get from older GNOME keyring versions. */
        if (results != NULL)
        {
            logger_write(logger, LOGLEVEL_DEBUG, "Delete search returned %u matches", g_list_length(results));

            GList * current = results;
            while (current != NULL)
            {
                GnomeKeyringNetworkPasswordData * data = (GnomeKeyringNetworkPasswordData *) current->data;

                if (data != NULL)
                {
                    /* Log each item */
                    logger_write(
                        logger,
                        LOGLEVEL_DEBUG,
                        "  Deleting item keyring [%s] id [%u] protocol [%s] server [%s] object [%s] authtype [%s] port [%u] user [%s] domain [%s]",
                        data->keyring, data->item_id, data->protocol, data->server, data->object, data->authtype,
                        data->port, data->user, data->domain);

                    GnomeKeyringResult result = gnome_keyring_item_delete_sync(data->keyring, data->item_id);

                    if (result != GNOME_KEYRING_RESULT_OK)
                    {
                        logger_write(logger, LOGLEVEL_WARN, "Could not delete GNOME keyring item (result=%u)", result);
                        ret = JNI_FALSE;
                    }
                }

                current = g_list_next(current);
            }

            gnome_keyring_network_password_list_free(results);
        }
        break;
    case GNOME_KEYRING_RESULT_NO_MATCH:
        // Normal for no match.  Ignore.
        break;
    case GNOME_KEYRING_RESULT_CANCELLED:
        // User canceled UI.  Ignore.
        break;
    default:
        logger_write(logger, LOGLEVEL_WARN, "Could not save GNOME keyring entry for %s (result=%u)", server, result);
        ret = JNI_FALSE;
        break;
    }

    /* Free memory. */

    if (jProtocolName != NULL)
    {
        releasePlatformChars(env, jProtocolName, protocolName);
    }

    releasePlatformChars(env, jServer, server);

    if (jPath != NULL)
    {
        releasePlatformChars(env, jPath, path);
    }

    releasePlatformChars(env, jUsername, username);

    if (jDomain != NULL)
    {
        releasePlatformChars(env, jDomain, domain);
    }

    logger_dispose(logger);

    return ret;
}
