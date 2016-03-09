/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

#include <stdio.h>
#include <windows.h>
#include <direct.h>
#include <jni.h>

#include "util.h"
#include "native_messagewindow.h"

/*
 * A Win32 window intended for interprocess communication.  It is not visible on
 * screen, but it's technically not a "message-only" window because those aren't
 * enumerable (and this one is).
 *
 * All messages >= WM_USER are sent to the Java class.
 *
 * Sizes of HWND, WPARAM, and LPARAM types depend on processor
 * architecture.  jlongs are used at the JNI boundaries.  Callers should
 * ensure values are appropriate for the arch in use (or they might get
 * truncated).
 */

// The Java class must provide a static method of this type to receive 
// messages.
#define MESSAGE_RECEIVED_METHOD			"messageReceived"
#define MESSAGE_RECEIVED_METHOD_PROTO	"(JIJJ)V"

// Docs for WNDCLASS/WNDCLASSEX structs say 256 is the max class name length.
#define WINDOW_CLASS_NAME_MAX		256

// References to static Java type information.  These references are never 
// released (they're for type info, so that's not too bad).
static jobject javaClass;
static jmethodID messageReceivedMethodID;

// Index into thread-local storage where the JNIEnv is stored.
static DWORD tlsIndex = 0;

// Input data to PostMessageFunc
typedef struct
{
	// Message sent only to windows that match this class
	const WCHAR * windowClass;

	// Message sent only to windows with a GWLP_USERDATA value that
	// matches one of the values in userData (count may be 0 to disable
	// the user data check)
	SIZE_T userDataCount;
	LONG_PTR * userData;

	// Data
	UINT msg;
	WPARAM wParam;
	LPARAM lParam;
} MessageData;

/*
 * Sends WM_USER messages to the Java class, forwards others to the default
 * procedure.
 */
LRESULT CALLBACK WindowProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    JNIEnv * env = NULL;

	env = TlsGetValue(tlsIndex);
    if (env != NULL)
	{
        /* 
		 * If an exception has already occurred,
         * allow the stack to unwind so that the
         * exception will be thrown in Java.
		 */
        if ((*env)->ExceptionOccurred(env))
		{
			return (LRESULT) 0;
		}
    
		if (msg >= WM_USER && msg <= 0x7FFF)
        {
			(*env)->CallStaticVoidMethod(
				env, 
				javaClass, 
				messageReceivedMethodID, 
				(jlong) hwnd,
				(jint) msg, 
				(jlong) wParam, 
				(jlong) lParam);
			return (LRESULT) 0;
        }
    }

	return DefWindowProc(hwnd, msg, wParam, lParam); 
}

/*
 * Sends the MessageData in the lParam asynchronously to all the windows 
 * with the specified class and matching user data.
 */
BOOL CALLBACK PostMessageFunc(HWND hwnd, LPARAM lParam)
{
	WCHAR className[WINDOW_CLASS_NAME_MAX];
	MessageData * data = (MessageData *) lParam;

	// Ensure the window's user data matches
	if (data->userDataCount > 0)
	{
		BOOL match = FALSE;
		SIZE_T i;
		LONG_PTR userData;
		
		userData = GetWindowLongPtr(hwnd, GWLP_USERDATA);

		for (i = 0; i < data->userDataCount; i++)
		{
			if (data->userData[i] == userData)
			{
				match = TRUE;
				break;
			}
		}

		if (!match)
		{
			// Not an error, keep processing other windows
			return TRUE;
		}
	}

	// Test the class name for a match
	if (GetClassNameW(hwnd, className, WINDOW_CLASS_NAME_MAX) == 0)
	{
		// Don't throw an exception, return true to keep the send process going
		return TRUE;
	}

	if (wcscmp(className, data->windowClass) == 0)
	{
		if (PostMessage(hwnd, data->msg, data->wParam, data->lParam) == 0)
		{
			int error = GetLastError();

			// ERROR_ACCESS_DENIED is expected if User Interface Privilege Isolation (UIPI) blocked it
			// ERROR_NOT_ENOUGH_QUOTA is expected if we hit a configured message limit
			if (error != ERROR_SUCCESS
				&& error != ERROR_ACCESS_DENIED 
				&& error != ERROR_NOT_ENOUGH_QUOTA)
			{
				// Set the error code our enumerator can report via exception
				SetLastError(error);
				return FALSE;
			}
		}
	}

	return TRUE;
}

JNIEXPORT jlong JNICALL Java_com_microsoft_tfs_jni_MessageWindow_nativeCreateWindow(
    JNIEnv *env, jclass cls, jlong hwndParent, jstring jClassName, jstring jWindowTitle, jlong userData)
{
	HWND hwnd = NULL;
	const WCHAR * className = NULL;
	const WCHAR * windowTitle = NULL;
	WNDCLASS wc;

	// Save references to static class information once
    if (tlsIndex == 0)
	{
        tlsIndex = TlsAlloc();
        if (tlsIndex == TLS_OUT_OF_INDEXES)
		{
			throwRuntimeExceptionCode(env, GetLastError(), "Error allocating thread local storage for message window");
			goto cleanup;
		}

        javaClass = (*env)->NewGlobalRef(env, (jobject) cls);

        messageReceivedMethodID = (*env)->GetStaticMethodID(
			env, 
			(jobject) cls, 
			MESSAGE_RECEIVED_METHOD, 
			MESSAGE_RECEIVED_METHOD_PROTO);

		if (messageReceivedMethodID == NULL)
		{
			throwRuntimeExceptionString(env, "Class missing static method %s [%s]", MESSAGE_RECEIVED_METHOD, MESSAGE_RECEIVED_METHOD_PROTO);
			tlsIndex = 0;
			goto cleanup;
		}
    }

	// Save reference to JVM environment
	if (TlsSetValue(tlsIndex, (LPVOID) env) == 0)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error saving environment thread local storage for message window");
		goto cleanup;
	}

	if ((className = javaStringToPlatformChars(env, jClassName)) == NULL)
	{
		// String allocation failed, exception already thrown
		goto cleanup;
	}

	if (jWindowTitle != NULL)
	{
		if ((windowTitle = javaStringToPlatformChars(env, jWindowTitle)) == NULL)
		{
			// String allocation failed, exception already thrown
			goto cleanup;
		}
	}

	// Register the custom class if needed
	if (GetClassInfoW((HINSTANCE) GetModuleHandle(NULL), className, &wc) == 0)
	{
		wc.style = 0;
		wc.lpfnWndProc = WindowProc;
		wc.cbClsExtra = 0;
		wc.cbWndExtra = 0;
		wc.hInstance = (HINSTANCE) GetModuleHandle(NULL);
		wc.hIcon = NULL;
		wc.hCursor = NULL;
		wc.hbrBackground = NULL;
		wc.lpszMenuName = NULL;
		wc.lpszClassName = className;

		if (RegisterClassW(&wc) == 0)
		{
			throwRuntimeExceptionCode(env, GetLastError(), "Error registering window class");
			goto cleanup;
		}
	}

	// Create the window with the specified class name and title
	hwnd = CreateWindowEx(
		0,									//  dwExStyle 
        className,							//  lpszClassName
        windowTitle,						//  lpszWindowName
        WS_OVERLAPPED,						//  style
        0, 0, 0, 0,							//  x, y, width, height
        (HWND) hwndParent,					//  hWndParent
        NULL,								//  hMenu
        (HINSTANCE) GetModuleHandle(NULL),	//  hInst
        NULL);								//  pvParam
	if (hwnd == NULL)
	{
		throwRuntimeExceptionCode(env, GetLastError(), "Error creating native message window");
		goto cleanup;
	}

	// Set the user data 
	SetWindowLongPtr(hwnd, GWLP_USERDATA, (LONG_PTR) userData);

cleanup:

	if (className != NULL)
	{
		releasePlatformChars(env, jClassName, className); 
	}
	if (windowTitle != NULL)
	{
		releasePlatformChars(env, jWindowTitle, windowTitle); 
	}

    return (jlong) hwnd;
}

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_MessageWindow_nativeDestroyWindow(
    JNIEnv *env, jclass cls, jlong hwnd)
{
	return DestroyWindow((HWND) hwnd) == 0 ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_microsoft_tfs_jni_MessageWindow_nativeSendMessage(
    JNIEnv *env, jclass cls, jlong hwnd, jstring jClassName, jlongArray jUserData, jint msg, jlong wParam, jlong lParam)
{
	MessageData data;
	memset(&data, 0, sizeof(data));

	data.windowClass = javaStringToPlatformChars(env, jClassName);
	data.userDataCount = 0;
	data.userData = NULL;
	data.msg = (UINT) msg;
	data.wParam = (WPARAM) wParam;
	data.lParam = (LPARAM) lParam;
	
	// Caller may want to filter by user data
	if (jUserData != NULL)
	{
		data.userDataCount = (*env)->GetArrayLength(env, jUserData);
		
		if (data.userDataCount > 0)
		{
			SIZE_T i;
			jlong * elements;
		
			data.userData = (LONG_PTR *) malloc(sizeof(LONG_PTR) * data.userDataCount);
		
			if (data.userData == NULL)
			{
				throwRuntimeExceptionString(env, "Failed to allocate user data");
				goto cleanup;
			}

			// Copy/convert the user data values from jlong to LONG_PTRs
			elements = (*env)->GetLongArrayElements(env, jUserData, 0);
			for (i = 0; i < data.userDataCount; i++)
			{
				data.userData[i] = (LONG_PTR) elements[i];
			}
			(*env)->ReleaseLongArrayElements(env, jUserData, elements, 0);
		}
	}
	
	if (EnumWindows(PostMessageFunc, (LPARAM) &data) == 0)
	{
		// Our callback sets error on failure
		throwRuntimeExceptionCode(env, GetLastError(), "PostMessage failed");
		goto cleanup;
	}

cleanup:

	if (data.windowClass != NULL)
	{
		releasePlatformChars(env, jClassName, data.windowClass);
	}

	if (data.userData != NULL)
	{
		free(data.userData);
	}
}
