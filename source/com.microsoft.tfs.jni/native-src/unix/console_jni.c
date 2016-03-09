/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that do console work.
 */

#include <unistd.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <stdio.h>
#include <termios.h>
#include <jni.h>

#include "native_console.h"
#include "util.h"

/*
 * Does the real console/terminal size work.  Not exported via JNI.
 */
void _getConsoleSize(int * rows, int * columns)
{
    int ttyfd;
    struct winsize ws;
    static const char * tty = "/dev/tty";

    if ((ttyfd = open(tty, O_RDONLY)) >= 0)
    {
        if (ioctl(ttyfd, TIOCGWINSZ, &ws) >= 0)
        {
            *rows = ws.ws_row;
            *columns = ws.ws_col;
        }
        close(ttyfd);
    }
}

/*
 * Gets the height of the console this process is attached to in rows.
 *
 * If an error occured, 0 is returned.
 */
JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_console_NativeConsole_nativeGetRows(JNIEnv *env, jclass cls)
{
    int rows = 0, columns = 0;
    _getConsoleSize(&rows, &columns);
    return rows;
}

/*
 * Gets the width of the terminal this process is attached to in columns.
 *
 * If an error occured, 0 is returned.
 */
JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_console_NativeConsole_nativeGetColumns(JNIEnv *env,
    jclass cls)
{
    int rows = 0, columns = 0;
    _getConsoleSize(&rows, &columns);
    return columns;
}

/*
 * Disables character echo on the console.
 *
 * Returns true if the operation succeeded, false if it failed.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_console_NativeConsole_nativeDisableEcho(JNIEnv *env,
    jclass cls)
{
    struct termios settings;

    /* Get settings. */
    if (tcgetattr(STDIN_FILENO, &settings) != 0)
    {
        return JNI_FALSE;
    }

    settings.c_lflag &= (~ECHO);

    /* Set them back. */
    if (tcsetattr(STDIN_FILENO, TCSANOW, &settings) != 0)
    {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Enables character echo on the console.
 *
 * Returns true if the operation succeeded, false if it failed.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_console_NativeConsole_nativeEnableEcho(JNIEnv *env,
    jclass cls)
{
    struct termios settings;

    /* Get settings. */
    if (tcgetattr(STDIN_FILENO, &settings) != 0)
    {
        return JNI_FALSE;
    }

    settings.c_lflag |= (ECHO);

    /* Set them back. */
    if (tcsetattr(STDIN_FILENO, TCSANOW, &settings) != 0)
    {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
