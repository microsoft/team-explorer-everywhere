/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See License.txt in the repository root.
 */

/*
 * JNI functions that do filesystem work.
 */

#include <jni.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>

/* Required for Mac xattrs (ResourceForks, etc) */
#ifdef MACOS_X
# include <stdlib.h>
# include <sys/xattr.h>
# include <string.h>
#endif

#include "native_filesystem.h"
#include "util.h"
#include "objects.h"

#define CREATE_TEMP_FILE_RETRIES	10
#define TEMP_FILENAME_MAX			1024

static jobject newUnixFileSystemTime(
	JNIEnv *env,
	jclass timeClass,
	jmethodID timeCtorMethod,
	struct stat *st)
{
    /*
     * Mac OS X has tv_sec and tv_nsec in st_mtimespec, AIX has nanos
     * in st_mtime_n, Linux has them in st_mtim, most other Unixes do
     * not have nanosecond precision.
     */
#ifdef HAS_STAT_MTIMESPEC
    return newFileSystemTime(
        env,
        timeClass,
        timeCtorMethod,
        (jlong)st->st_mtimespec.tv_sec,
        (jlong)st->st_mtimespec.tv_nsec);
#elif HAS_STAT_MTIME_N
    return newFileSystemTime(
        env,
        timeClass,
        timeCtorMethod,
        (jlong)st->st_mtime,
        (jlong)st->st_mtime_n);
#elif HAS_STAT_MTIM
    return newFileSystemTime(
        env,
        timeClass,
        timeCtorMethod,
        (jlong)st->st_mtim.tv_sec,
        (jlong)st->st_mtim.tv_nsec);
#else
    return newFileSystemTime(
        env,
        timeClass,
        timeCtorMethod,
        (jlong)st->st_mtime,
        (jlong)0);
#endif
}

JNIEXPORT jobject JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeGetAttributes(
    JNIEnv *env, jclass cls, jstring jFilepath)
{
    const char *filepath;
    jclass exceptionClass, timeClass, attributesClass;
    jmethodID timeCtorMethod, attributesCtorMethod;
    jobject timeObj = NULL, attributesObj = NULL;
    jboolean symlink, readonly;
    int statResult, lstatResult;
    struct stat fileAttrs, linkAttrs;
    char *errorMessage;

    /*
     * Ensure that we can load the necessary classes for our file attributes return value.
     * Note that these methods will return NULL on failure and raise an exception that will
     * be handled when we return to Java.
     */
    timeClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/FileSystemTime");
    attributesClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/FileSystemAttributes");

    if (timeClass == NULL || attributesClass == NULL)
    {
        return NULL;
    }

    timeCtorMethod = (*env)->GetMethodID(env, timeClass, "<init>", "(JJ)V");
    attributesCtorMethod = (*env)->GetMethodID(env, attributesClass, "<init>",
        "(ZLcom/microsoft/tfs/jni/FileSystemTime;JZZZZZZZZZZ)V");

    if (timeCtorMethod == NULL || attributesCtorMethod == NULL)
    {
        return NULL;
    }

    /*
     * Get the file path.
     */
    if (jFilepath == NULL)
    {
        return NULL;
    }

    filepath = javaStringToPlatformChars(env, jFilepath);

    /* Get the file attributes (in the symbolic link case, gets the results of the item and the item referencing it.) */
    lstatResult = lstat(filepath, &linkAttrs);
    statResult = stat(filepath, &fileAttrs);

    releasePlatformChars(env, jFilepath, filepath);

    if (lstatResult != 0 || statResult != 0)
    {
        /* I/O errors are the only fatal errors. */
        if (errno == EIO)
        {
            exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");

            if (exceptionClass != NULL)
            {
                if ((errorMessage = strerror(errno)) == NULL)
                {
                    errorMessage = "Could not load error message";
                }

                (*env)->ThrowNew(env, exceptionClass, errorMessage);
            }

            return NULL;
        }

        /*
         * Otherwise, if the lstat succeeded (but the stat failed) then we likely have a symlink pointing
         * to a nonexistant file.
         */
        symlink = (lstatResult == 0 && ((linkAttrs.st_mode & S_IFLNK) == S_IFLNK)) ? JNI_TRUE : JNI_FALSE;

        if(symlink == JNI_TRUE)
        {
        	timeObj = newUnixFileSystemTime(
        		env,
        		timeClass,
        		timeCtorMethod,
			&linkAttrs);
        }

        return newFileSystemAttributes(env,
            attributesClass,
            attributesCtorMethod,
            symlink, /* file exist attribute; symlink true means this is a symlink pointing to a non-existing file, file exists is true too */
            timeObj,
            (jlong) 0,
            JNI_FALSE,
            JNI_FALSE,
            JNI_FALSE,
            JNI_FALSE,
            JNI_FALSE,
            JNI_FALSE,
            JNI_FALSE,
            JNI_FALSE,
            JNI_FALSE,
            symlink);
    }

	/* get symlink attribute first so that we can get modification time differently  */
	symlink = ((linkAttrs.st_mode & S_IFLNK) == S_IFLNK) ? JNI_TRUE : JNI_FALSE; 
    /* Look at the unix mode and the immutable bit on Mac OS X */
#ifdef MACOS_X
    readonly = ((fileAttrs.st_mode & S_IWUSR) != S_IWUSR) || ((fileAttrs.st_flags & UF_IMMUTABLE) == UF_IMMUTABLE) ? JNI_TRUE : JNI_FALSE;
#else
    readonly = ((fileAttrs.st_mode & S_IWUSR) != S_IWUSR) ? JNI_TRUE : JNI_FALSE;
#endif

    if(symlink == JNI_TRUE)
    {
    	timeObj = newUnixFileSystemTime(
    		env,
    		timeClass,
    		timeCtorMethod,
		&linkAttrs);
    }
    else {
	timeObj = newUnixFileSystemTime(
	    env, timeClass, timeCtorMethod, &fileAttrs);
    }
        
    if (timeObj == NULL)
    {
        return NULL;
    }

    attributesObj = newFileSystemAttributes(
        env,
        attributesClass,
        attributesCtorMethod,
        JNI_TRUE,
        timeObj,
        (jlong) fileAttrs.st_size,
        readonly,
        ((fileAttrs.st_mode & (S_IRGRP | S_IWGRP | S_IXGRP | S_IROTH | S_IWOTH | S_IXOTH)) == 0) ? JNI_TRUE : JNI_FALSE, /* owner only */
        ((fileAttrs.st_mode & (S_IWGRP | S_IWOTH)) == (S_IWGRP | S_IWOTH)) ? JNI_TRUE : JNI_FALSE, /* publicly writable */
        JNI_FALSE, /* hidden */
        JNI_FALSE, /* system */
        ((fileAttrs.st_mode & S_IFDIR) == S_IFDIR) ? JNI_TRUE : JNI_FALSE, /* directory */
        JNI_FALSE, /* archive */
        JNI_FALSE, /* not content indexed */
        ((fileAttrs.st_mode & S_IXUSR) != 0) ? JNI_TRUE : JNI_FALSE, /* executable */
        symlink
        );

    return attributesObj;
}

/*
 * WARNING
 *
 * Calls to this method must synchronized so that only one thread will execute it
 * at a time.  This is required to work around the design of Unix's umask() call.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeSetAttributes(
    JNIEnv *env, jclass cls, jstring filepath, jobject jAttributes)
{
    jclass attributesClass;
    jmethodID isReadOnlyMethod, isOwnerOnlyMethod, isExecutableMethod, isPublicWritableMethod;
    jboolean readonly, owneronly, executable, publicwritable;
    struct stat fileAttrs;
    int result;

    /*
     * Ensure that we can load the necessary classes for our file attributes return value.
     * Note that these methods will return NULL on failure and raise an exception that will
     * be handled when we return to Java.
     */
    attributesClass = (*env)->FindClass(env, "com/microsoft/tfs/jni/FileSystemAttributes");

    if (attributesClass == NULL)
    {
        return JNI_FALSE;
    }

    isReadOnlyMethod = (*env)->GetMethodID(env, attributesClass, "isReadOnly", "()Z");
    isOwnerOnlyMethod = (*env)->GetMethodID(env, attributesClass, "isOwnerOnly", "()Z");
    isExecutableMethod = (*env)->GetMethodID(env, attributesClass, "isExecutable", "()Z");
    isPublicWritableMethod = (*env)->GetMethodID(env, attributesClass, "isPublicWritable", "()Z");

    if (isReadOnlyMethod == NULL || isOwnerOnlyMethod == NULL || isExecutableMethod == NULL || isPublicWritableMethod == NULL)
    {
        return JNI_FALSE;
    }

    readonly = (*env)->CallBooleanMethod(env, jAttributes, isReadOnlyMethod);
    owneronly = (*env)->CallBooleanMethod(env, jAttributes, isOwnerOnlyMethod);
    executable = (*env)->CallBooleanMethod(env, jAttributes, isExecutableMethod);
    publicwritable = (*env)->CallBooleanMethod(env, jAttributes, isPublicWritableMethod);

    const char * str = javaStringToPlatformChars(env, filepath);
    result = stat(str, &fileAttrs);

#ifdef MACOS_X
    /* Remove immutable bit for MacOS X */
    if(result == 0 && (fileAttrs.st_flags & UF_IMMUTABLE) == UF_IMMUTABLE)
    {
        u_long newFlags = fileAttrs.st_flags;

        /* Remove immutable bit */
        newFlags &= ~(UF_IMMUTABLE);

        result = chflags(str, newFlags);
    }
#endif /* MACOS_X */

    if (result == 0)
    {
        /* Allocate a new mode to be modified.  Initialize from existing mode. */
        mode_t newMode = fileAttrs.st_mode;

        /* Get user's current umask. */
        mode_t userUmask = umask(0);
        umask(userUmask);

        if (publicwritable)
        {
        	/* If we want a publicly writable file, override the umask with 0. */
        	userUmask = 0;
        }
        else
        {
        	/* Remove group and other write bits. */
        	newMode &= ~(S_IWGRP | S_IWOTH);
        }

        if (readonly)
        {
            /* Remove all write bits. */
            newMode &= ~(S_IWUSR | S_IWGRP | S_IWOTH);
        }
        else
        {
            /* Flip on some read and write bits, allowing only what passes umask. */
            newMode |= ((S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH) & ~userUmask);
        }

        if (executable)
        {
            /* Flip on some read and executable bits, allowing only what passes umask. */
            newMode |= ((S_IRUSR | S_IXUSR | S_IRGRP | S_IXGRP | S_IROTH | S_IXOTH) & ~userUmask);
        }
        else
        {
            /* Remove all execute bits. */
            newMode &= ~(S_IXUSR | S_IXGRP | S_IXOTH);
        }
        
        if (owneronly)
        {
        	/* Remove all bits for group and others, regardless of umask or public writable. */
        	newMode &= ~(S_IRGRP | S_IWGRP | S_IXGRP | S_IROTH | S_IWOTH | S_IXOTH);
        }

        result = chmod(str, newMode);
    }

#ifdef MACOS_X
    char *immutable = getenv("TP_SET_IMMUTABLE");

    /* Reset immutability bit for readonly files on MacOS X if requested */
    if (result == 0 && readonly == JNI_TRUE && immutable != NULL && strcasecmp(immutable, "on") == 0)
    {
        result = chflags(str, (fileAttrs.st_flags | UF_IMMUTABLE));
    }
#endif /* MACOS_X */

    releasePlatformChars(env, filepath, str);

    return result == 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeCreateSymbolicLink(
    JNIEnv *env, jclass cls, jstring oldpath, jstring newpath)
{
    int result;

    const char * oldstr = javaStringToPlatformChars(env, oldpath);
    const char * newstr = javaStringToPlatformChars(env, newpath);
    result = symlink(oldstr, newstr);
    releasePlatformChars(env, oldpath, oldstr);
    releasePlatformChars(env, newpath, newstr);

    return result;
}

JNIEXPORT jobject JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeGetSymbolicLink(
    JNIEnv *env, jclass cls, jstring filePath)
{
	jstring link = NULL;
	char *tempLink;
	int lstatResult, linkLength;
	struct stat linkAttrs;
	
    const char * pathStr = javaStringToPlatformChars(env, filePath);
	lstatResult = lstat(pathStr, &linkAttrs);
	if(lstatResult == -1)
	{
		return NULL;
	}
	
	tempLink = malloc(linkAttrs.st_size + 1);
    linkLength = readlink(pathStr, tempLink, linkAttrs.st_size);
    
    if(linkLength < 0)
    {
    	return NULL;
    }	
	
	tempLink[linkLength] = '\0';
	link = platformCharsToJavaString(env, tempLink);
    releasePlatformChars(env, filePath, pathStr);

    return link;
}

JNIEXPORT jobject JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeCreateTempFileSecure(
	    JNIEnv *env, jclass cls, jstring jPrefix, jstring jSuffix, jstring jParentPath)
{
	jclass exceptionClass;
	jstring jFilename = NULL;
	const char *prefix, *suffix, *parentpath;
	char tempname[TEMP_FILENAME_MAX], filename[TEMP_FILENAME_MAX];
	char *errorMessage;
	int fd, outsize, i;

    prefix = javaStringToPlatformChars(env, jPrefix);
    suffix = javaStringToPlatformChars(env, jSuffix);
    parentpath = javaStringToPlatformChars(env, jParentPath);

    /* loop until we find a unique filename */
    for(i = 0; i < CREATE_TEMP_FILE_RETRIES; i++)
    {
    	outsize = snprintf(tempname, TEMP_FILENAME_MAX, "%s/%sXXXXXX", parentpath, prefix);
    	
    	if(outsize < 0 || outsize >= TEMP_FILENAME_MAX)
    	{
    		throwRuntimeExceptionString(env, "Path too long");
    		break;
    	}
    
		/* mktemp should never return NULL, but may set filename to the empty string on error */
		if(mktemp(tempname) == NULL || tempname[0] == '\0')
		{
			throwRuntimeExceptionCode(env, errno, "Could not mktemp");
			break;
		}
		
		outsize = snprintf(filename, TEMP_FILENAME_MAX, "%s%s", tempname, suffix);
		
		if(outsize < 0 || outsize >= TEMP_FILENAME_MAX)
		{
    		throwRuntimeExceptionString(env, "Path too long");
			break;
		}

		if((fd = open(filename, O_WRONLY | O_CREAT | O_EXCL, 0600)) < 0)
		{
			/* file exists, try a new name */
			if(errno == EEXIST)
			{
				continue;
			}

            exceptionClass = (*env)->FindClass(env, "java/io/IOException");

            if (exceptionClass != NULL)
            {
                if ((errorMessage = strerror(errno)) == NULL)
                {
                    errorMessage = "Could not load error message";
                }

                (*env)->ThrowNew(env, exceptionClass, errorMessage);
            }

			break;
		}
		
		jFilename = platformCharsToJavaString(env, filename);

		close(fd);
		
		break;
    }

	releasePlatformChars(env, jPrefix, prefix);
	releasePlatformChars(env, jSuffix, suffix);
	releasePlatformChars(env, jParentPath, parentpath);

    return jFilename;
}

#ifdef MACOS_X

/*
 * WARNING: Mac OS X only
 *
 * Lists all extended attributes for the file "filename" and returns a
 * string array of each UTF-8 xattr name.  Returns null if the file's
 * xattr list could not be found.
 *
 * Note: it is not recommended to use this with the com.apple.ResourceFork
 * extended attribute -- that (and only that) attribute can be arbitrarily
 * large, and should be accessed in a Stream-friendly way.
 */
JNIEXPORT jobjectArray JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeListMacExtendedAttributes(
    JNIEnv *env,
    jclass cls,
    jstring jFilename)
{
    const char *filename;
    ssize_t xattrbufsize;
    char *xattrlist = NULL;
    int i, xattrcount = 0;
    jobjectArray jValue;
    char *xattrname;

    filename = javaStringToPlatformChars(env, jFilename);

    /* Determine the size of the list of extended attributes by querying with NULL */
    if(
        (xattrbufsize = listxattr(filename, NULL, 0, 0)) > 0 &&
        (xattrlist = (void *)malloc(xattrbufsize)) != NULL
    )
    {
        /* Get the xattr, ensuring that length hasn't changed */
        if(listxattr(filename, xattrlist, xattrbufsize, 0) != xattrbufsize)
        {
            free(xattrlist);
            xattrlist = NULL;
        }
    }

    releasePlatformChars(env, jFilename, filename);

    if(xattrlist == NULL)
    {
        /* There we no xattrs, return an empty array (signifying non-error) */
        if(xattrbufsize == 0)
        {
            return (*env)->NewObjectArray(env, 0, (*env)->FindClass(env, "java/lang/String"), platformCharsToJavaString(env, ""));
        }

        /* File doesn't exist (xattrbufsize < 0) or malloc failed, return NULL */
        return NULL;
    }

    /* First count the number of strings (by counting nulls) */
    for(i = 0; i < xattrbufsize; i++)
    {
        if(*(xattrlist + i) == 0)
        {
            xattrcount++;
        }
    }

    /* There we no nulls, fail */
    if(xattrcount == 0)
    {
        free(xattrlist);
        return NULL;
    }

    jValue = (*env)->NewObjectArray(env, xattrcount, (*env)->FindClass(env, "java/lang/String"), platformCharsToJavaString(env, ""));

    /* Now add each string into the array */
    for(i = 0, xattrname = xattrlist; i < xattrcount; i++)
    {
        /* Add to the string array */
        (*env)->SetObjectArrayElement(env, jValue, i, platformCharsToJavaString(env, xattrname));

        /* Increment the name pointer */
        xattrname += strlen(xattrname) + 1;
    }

    free(xattrlist);

    return jValue;
}

/*
 * WARNING: Mac OS X only
 *
 * Acts as a reader for the value of an extended attribute.  Allows you to
 * read chunks at a time for the value of an extended attribute, useful
 * for streaming.
 *
 * This should only be used for com.apple.ResourceFork, as other extended
 * attributes are guaranteed to be small enough to fling around in byte
 * arrays (using set/get methods, below) and the setxattr(2) with the position
 * flag is not guaranteed to be implemented for any other xattrs.
 */
JNIEXPORT jint JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeReadMacExtendedAttribute(
    JNIEnv *env,
    jclass cls,
    jstring jFilename,
    jstring jAttrname,
    jbyteArray readbuf,
    jint size,
    jlong position)
{
    const char *filename;
    const char *xattrname;
    void *xattr = NULL;
    ssize_t xattrsize;

    if(size == 0)
    {
        return 0;
    }
    else if(size < 0)
    {
        return -2;
    }

    if((xattr = (void *)malloc(size)) == NULL)
    {
        return -2;
    }

    filename = javaStringToPlatformChars(env, jFilename);
    xattrname = javaStringToPlatformChars(env, jAttrname);

    xattrsize = getxattr(filename, xattrname, xattr, size, position, 0);

    /* Return -1 for EOF */
    if(xattrsize == 0)
    {
        xattrsize = -1;
    }

    releasePlatformChars(env, jFilename, filename);
    releasePlatformChars(env, jAttrname, xattrname);

    if(xattrsize > 0)
    {
        (*env)->SetByteArrayRegion(env, readbuf, 0, (jsize)xattrsize, xattr);
    }

    free(xattr);

    return xattrsize;
}

/*
 * WARNING: Mac OS X only
 *
 * Acts as a reader for the value of an extended attribute.  Allows you to
 * write chunks at a time for the value of an extended attribute, useful
 * for streaming.
 *
 * This should only be used for com.apple.ResourceFork, as other extended
 * attributes are guaranteed to be small enough to fling around in byte
 * arrays (using set/get methods, below) and the setxattr(2) with the position
 * flag is not guaranteed to be implemented for any other xattrs.
 */

JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeWriteMacExtendedAttribute(
    JNIEnv *env,
    jclass cls,
    jstring jFilename,
    jstring jAttrname,
    jbyteArray writebuf,
    jint size,
    jlong position)
{
    const char *filename;
    const char *xattrname;
    void *xattr = NULL;
    int ret;

    if(size == 0)
    {
        return JNI_TRUE;
    }
    else if(size < 0)
    {
        return JNI_FALSE;
    }

    if((xattr = (void *)malloc(size)) == NULL)
    {
        return JNI_FALSE;
    }

    filename = javaStringToPlatformChars(env, jFilename);
    xattrname = javaStringToPlatformChars(env, jAttrname);

    (*env)->GetByteArrayRegion(env, writebuf, 0, size, xattr);

    ret = setxattr(filename, xattrname, xattr, size, position, 0);

    free(xattr);

    return (ret == 0 ? JNI_TRUE : JNI_FALSE);
}

/*
 * WARNING: Mac OS X only
 *
 * Gets the attribute specified by "xattrname" for the file "filename",
 * and returns it.  Returns null if the attribute could not be found.
 *
 * Note: it is not recommended to use this with the com.apple.ResourceFork
 * extended attribute -- that (and only that) attribute can be arbitrarily
 * large, and should be accessed in a Stream-friendly way.
 */
JNIEXPORT jbyteArray JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeGetMacExtendedAttribute(
    JNIEnv *env,
    jclass cls,
    jstring jFilename,
    jstring jAttrname)
{
    const char *filename;
    const char *xattrname;
    ssize_t xattrsize;
    void *xattr = NULL;
    jbyteArray jValue;

    filename = javaStringToPlatformChars(env, jFilename);
    xattrname = javaStringToPlatformChars(env, jAttrname);

    /* Determine the size of the extended attribute by querying with NULL value */
    if(
        (xattrsize = getxattr(filename, xattrname, NULL, 0, 0, 0)) > 0 &&
        (xattr = (void *)malloc(xattrsize)) != NULL
    )
    {
        /* Get the xattr, ensuring length hasn't changed */
        if(getxattr(filename, xattrname, xattr, xattrsize, 0, 0) != xattrsize)
        {
            free(xattr);
            xattr = NULL;
        }
    }

    releasePlatformChars(env, jFilename, filename);
    releasePlatformChars(env, jAttrname, xattrname);

    if(xattr == NULL)
    {
        return NULL;
    }

    jValue = (*env)->NewByteArray(env, xattrsize);
    (*env)->SetByteArrayRegion(env, jValue, 0, (jsize)xattrsize, xattr);

    free(xattr);

    return jValue;
}

/*
 * WARNING: Mac OS X only
 *
 * Sets the attribute specified by "xattrname" for the file "filename",
 * and places it in the pointer given by "xattr".  Returns JNI_TRUE on
 * success or JNI_FALSE on failure.
 *
 * Note: it is not recommended to use this with the com.apple.ResourceFork
 * extended attribute -- that (and only that) attribute can be arbitrarily
 * large, and should be accessed in a Stream-friendly way.
 */
JNIEXPORT jboolean JNICALL Java_com_microsoft_tfs_jni_internal_filesystem_NativeFileSystem_nativeSetMacExtendedAttribute(
    JNIEnv *env,
    jclass cls,
    jstring jFilename,
    jstring jAttrname,
    jbyteArray jValue)
{
    const char *filename;
    const char *xattrname;
    void *xattr = NULL;
    long xattrsize = 0;
    int success;

    if(jValue != NULL)
    {
        xattrsize = (*env)->GetArrayLength(env, jValue);
    }

    if(xattrsize > 0 && (xattr = (void *)malloc(xattrsize)) == NULL)
    {
        return JNI_FALSE;
    }

    filename = javaStringToPlatformChars(env, jFilename);
    xattrname = javaStringToPlatformChars(env, jAttrname);

    (*env)->GetByteArrayRegion(env, jValue, 0, xattrsize, xattr);

    success = setxattr(filename, xattrname, xattr, (size_t)xattrsize, 0, 0);

    if(xattr != NULL)
    {
        free(xattr);
    }

    releasePlatformChars(env, jFilename, filename);
    releasePlatformChars(env, jAttrname, xattrname);

    return (success == 0) ? JNI_TRUE : JNI_FALSE;
}

#endif /* MACOS_X */
