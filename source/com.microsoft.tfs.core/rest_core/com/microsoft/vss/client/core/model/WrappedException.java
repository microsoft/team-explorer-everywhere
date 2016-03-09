// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.microsoft.vss.client.core.utils.StringUtil;

public class WrappedException {
    private WrappedException innerException;
    private String message;
    private String helpLink;
    private Class<?> type;
    private String typeName;
    private String typeKey;
    private int errorCode;
    private int eventId;
    private String stackTrace;

    public WrappedException() {
    }

    public WrappedException(final Throwable exception, final boolean includeErrorDetail) {
        this.type = exception.getClass();

        if (exception.getCause() != null) {
            this.innerException = new WrappedException(exception.getCause(), includeErrorDetail);
        }

        this.message = exception.getMessage();

        if (includeErrorDetail) {
            this.stackTrace = exception.getStackTrace().toString();
        }

        if (exception instanceof VssException) {
            this.eventId = ((VssException) exception).getEventId();
            this.errorCode = ((VssException) exception).getErrorCode();
        }
    }

    public WrappedException getInnerException() {
        return innerException;
    }

    public void setInnerException(final WrappedException innerException) {
        this.innerException = innerException;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getHelpLink() {
        return helpLink;
    }

    public void setHelpLink(final String helpLink) {
        this.helpLink = helpLink;
    }

    public Class<?> getType() {
        if (type == null) {
            // try to create the type from the TypeName
            if (!StringUtil.isNullOrEmpty(typeName)) {
                try {
                    setType(Class.forName(typeName));
                } catch (final Exception e) {
                    // swallow failures
                }
            }
        }
        return type;
    }

    public void setType(final Class<?> type) {
        this.type = type;

        if (type != null) {
            this.typeName = type.getName();
            this.typeKey = type.getSimpleName();
        }
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public String getTypeKey() {
        return typeKey;
    }

    public void setTypeKey(final String typeKey) {
        this.typeKey = typeKey;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final int errorCode) {
        this.errorCode = errorCode;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(final int eventId) {
        this.eventId = eventId;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(final String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public Exception Unwrap(final Map<String, Class<? extends Exception>> map) {
        Exception innerException = null;

        if (getInnerException() != null) {
            innerException = getInnerException().Unwrap(map);
        }

        Exception exception = null;

        final Class<?> type;
        if (!StringUtil.isNullOrEmpty(getTypeKey()) && map != null && map.containsKey(getTypeKey())) {
            // if they have bothered to map type, use that
            type = map.get(getTypeKey());
        } else {
            // no mapping, see if we can create the specified type
            type = getType();
        }

        if (type != null) {

            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    final Constructor<?> exceptionConstructor;

                    switch (attempt) {
                        case 0:
                            if ((exceptionConstructor = type.getConstructor(String.class, Exception.class)) != null) {
                                exception = (Exception) exceptionConstructor.newInstance(message, innerException);
                            }
                            break;
                        case 1:
                            if ((exceptionConstructor = type.getConstructor(String.class)) != null) {
                                exception = (Exception) exceptionConstructor.newInstance(message);
                            }
                            break;
                        case 2:
                            if ((exceptionConstructor = type.getConstructor()) != null) {
                                exception = (Exception) exceptionConstructor.newInstance();
                            }
                    }

                    if (exception != null) {
                        break;
                    }
                } catch (final Exception e) {
                    // do nothing
                }
            }
        }

        if (exception instanceof VssException) {
            ((VssException) exception).setEventId(this.eventId);
            ((VssException) exception).setErrorCode(this.errorCode);
        }

        if (exception == null && !StringUtil.isNullOrEmpty(message)) {
            assert false : "Unexpected exception type: " + getTypeName(); //$NON-NLS-1$
            exception = new VssServiceException(message, innerException);
        }

        assert exception != null : "Server Exception cannot be resolved."; //$NON-NLS-1$

        if (exception != null && !StringUtil.isNullOrEmpty(getHelpLink())) {
            try {
                final Method setHelpLinkMethod = exception.getClass().getMethod("setHelpLink", String.class); //$NON-NLS-1$
                if (setHelpLinkMethod != null) {
                    setHelpLinkMethod.invoke(exception, getHelpLink());
                }
            } catch (final Exception e) {
                // do nothing
            }
        }

        if (exception != null && getStackTrace() != null && getStackTrace().length() > 0) {

            final String[] lines = getStackTrace().split("\r\n"); //$NON-NLS-1$
            final List<StackTraceElement> list = new ArrayList<StackTraceElement>();

            for (final String line : lines) {
                list.add(parseTraceLine(line));
            }

            exception.setStackTrace(list.toArray(new StackTraceElement[list.size()]));
        }

        return exception;
    }

    private StackTraceElement parseTraceLine(final String line) {
        final String declaringClass;
        final String methodName;
        final String fileName;

        int lineNumber;

        String s = line.trim();
        int i;

        if (s.startsWith("at ")) { //$NON-NLS-1$
            s = s.substring(3);

            i = s.indexOf(":line "); //$NON-NLS-1$

            if (i < 0) {
                lineNumber = 0;
            } else {
                try {
                    lineNumber = Integer.parseInt(s.substring(i + 6));
                    s = s.substring(0, i);
                } catch (final Exception e) {
                    lineNumber = 0;
                }
            }

            i = s.indexOf(" in "); //$NON-NLS-1$

            if (i < 0) {
                fileName = StringUtil.EMPTY;
            } else {
                fileName = s.substring(i + 4);
                s = s.substring(0, i);
            }

            i = s.indexOf('(');

            if (i < 0) {
                declaringClass = s;
                methodName = StringUtil.EMPTY;
            } else {
                i = s.lastIndexOf('.', i);

                if (i < 0) {
                    declaringClass = s;
                    methodName = StringUtil.EMPTY;
                } else {
                    declaringClass = s.substring(0, i);
                    methodName = s.substring(i + 1);
                }
            }

            return new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
        } else {
            return new StackTraceElement(s, StringUtil.EMPTY, StringUtil.EMPTY, 0);
        }
    }

}
