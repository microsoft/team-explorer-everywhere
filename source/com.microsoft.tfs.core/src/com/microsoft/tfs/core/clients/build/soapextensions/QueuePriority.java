// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;
import com.microsoft.tfs.util.Check;

import ms.tfs.build.buildservice._04._QueuePriority;

/**
 * Describes priority in queue.
 *
 * @since TEE-SDK-10.1
 */
public class QueuePriority extends EnumerationWrapper implements Comparable<QueuePriority> {
    public static final QueuePriority LOW = new QueuePriority(_QueuePriority.Low, "Low"); //$NON-NLS-1$
    public static final QueuePriority BELOW_NORMAL = new QueuePriority(_QueuePriority.BelowNormal, "Below Normal"); //$NON-NLS-1$
    public static final QueuePriority NORMAL = new QueuePriority(_QueuePriority.Normal, "Normal"); //$NON-NLS-1$
    public static final QueuePriority ABOVE_NORMAL = new QueuePriority(_QueuePriority.AboveNormal, "Above Normal"); //$NON-NLS-1$
    public static final QueuePriority HIGH = new QueuePriority(_QueuePriority.High, "High"); //$NON-NLS-1$

    private final String displayText;

    private QueuePriority(final _QueuePriority queuePriority, final String displayText) {
        super(queuePriority);
        this.displayText = displayText;
    }

    /**
     * Gets the correct wrapper type for the given web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     * @return the correct wrapper type for the given web service object
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service object
     */
    public static QueuePriority fromWebServiceObject(final _QueuePriority webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (QueuePriority) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _QueuePriority getWebServiceObject() {
        return (_QueuePriority) webServiceObject;
    }

    /**
     * Gets the correct wrapper type for the given display text.
     *
     * @param displayText
     *        the display text to match (must not be <code>null</code>)
     * @return the correct wrapper type
     */
    public static QueuePriority fromDisplayText(final String displayText) {
        Check.notNull(displayText, "displayText"); //$NON-NLS-1$

        if (LOW.getDisplayText().equals(displayText)) {
            return LOW;
        }
        if (BELOW_NORMAL.getDisplayText().equals(displayText)) {
            return BELOW_NORMAL;
        }
        if (ABOVE_NORMAL.getDisplayText().equals(displayText)) {
            return ABOVE_NORMAL;
        }
        if (HIGH.getDisplayText().equals(displayText)) {
            return HIGH;
        }

        return NORMAL;
    }

    /**
     * @return The human readable text corresponding to this queue priority.
     */
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public int compareTo(final QueuePriority other) {
        return getSortPosition(this) - getSortPosition(other);
    }

    private int getSortPosition(final QueuePriority priority) {
        if (priority == HIGH) {
            return 1;
        }
        if (priority == ABOVE_NORMAL) {
            return 2;
        }
        if (priority == NORMAL) {
            return 3;
        }
        if (priority == BELOW_NORMAL) {
            return 4;
        }
        if (priority == LOW) {
            return 5;
        }
        return 0;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
