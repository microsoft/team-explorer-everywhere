/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#include <stdio.h>
#include <windows.h>
#include <direct.h>
#include <jni.h>

#include "tee_sal.h"
#include "native_console.h"

/*
 * Does the real console size work.  Pass NULL for any argument you do not wish to have a value written to.
 * On error no arguments are updated.
 */
void _getScreenBufferAndWindowSize(_Inout_opt_ int * screenBufferRows, _Inout_opt_ int * screenBufferColumns, 
	_Inout_opt_ int * windowRows, _Inout_opt_ int * windowColumns)
{
    CONSOLE_SCREEN_BUFFER_INFO info;

    if (GetConsoleScreenBufferInfo(GetStdHandle(STD_OUTPUT_HANDLE), &info) != TRUE)
    {
        return;
    }

    if (screenBufferRows != NULL)
    {
        *screenBufferRows = info.dwSize.Y;
    }

    if (screenBufferColumns != NULL)
    {
        *screenBufferColumns = info.dwSize.X;
    }

    if (windowRows != NULL)
    {
        // Add 1 because rows at both bounds are visible
        *windowRows = info.srWindow.Bottom - info.srWindow.Top + 1;
    }

    if (windowColumns != NULL)
    {
        // Add 1 because columns at both bounds are visible
        *windowColumns = info.srWindow.Right - info.srWindow.Left + 1;
    }
}

/*
 * Gets the height of the console WINDOW (not screen buffer) this process is attached to in rows.
 *
 * If an error occured, 0 is returned.
 */
JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_console_NativeConsole_nativeGetRows(JNIEnv *env, jclass cls)
{
    int rows = 0;
    _getScreenBufferAndWindowSize(NULL, NULL, &rows, NULL);
    return rows;
}

/*
 * Gets the width of the console SCREEN BUFFER (not window) this process is attached to in columns.
 *
 * If an error occured, 0 is returned.
 */
JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_console_NativeConsole_nativeGetColumns(JNIEnv *env,
    jclass cls)
{
    int columns = 0;
    _getScreenBufferAndWindowSize(NULL, &columns, NULL, NULL);
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
    HANDLE inputHandle = GetStdHandle(STD_INPUT_HANDLE);
    DWORD consoleFlags;

    if (inputHandle != INVALID_HANDLE_VALUE)
    {
        /* Get the current mode. */
        if (GetConsoleMode(inputHandle, &consoleFlags))
        {
            /* Turn off echo. */
            consoleFlags &= ~(ENABLE_ECHO_INPUT);

            if (SetConsoleMode(inputHandle, consoleFlags))
            {
                return JNI_TRUE;
            }
        }
    }

    return JNI_FALSE;
}

/*
 * Enables character echo on the console.
 *
 * Returns true if the operation succeeded, false if it failed.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_console_NativeConsole_nativeEnableEcho(JNIEnv *env,
    jclass cls)
{
    HANDLE inputHandle = GetStdHandle(STD_INPUT_HANDLE);
    DWORD consoleFlags;

    if (inputHandle != INVALID_HANDLE_VALUE)
    {
        /* Get the current mode. */
        if (GetConsoleMode(inputHandle, &consoleFlags))
        {
            /* Turn on echo. */
            consoleFlags |= (ENABLE_ECHO_INPUT);

            if (SetConsoleMode(inputHandle, consoleFlags))
            {
                return JNI_TRUE;
            }
        }
    }

    return JNI_FALSE;
}
