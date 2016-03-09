// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._CheckinWorkItemAction;

/**
 * Check-in action on the work item.
 *
 * @since TEE-SDK-10.1
 */
public class CheckinWorkItemAction extends EnumerationWrapper {
    public static final CheckinWorkItemAction NONE = new CheckinWorkItemAction(_CheckinWorkItemAction.None);
    public static final CheckinWorkItemAction RESOLVE = new CheckinWorkItemAction(_CheckinWorkItemAction.Resolve);
    public static final CheckinWorkItemAction ASSOCIATE = new CheckinWorkItemAction(_CheckinWorkItemAction.Associate);

    private CheckinWorkItemAction(final _CheckinWorkItemAction action) {
        super(action);
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
    public static CheckinWorkItemAction fromWebServiceObject(final _CheckinWorkItemAction webServiceObject) {
        return (CheckinWorkItemAction) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinWorkItemAction getWebServiceObject() {
        return (_CheckinWorkItemAction) webServiceObject;
    }

    /**
     * Gets the correct enumeration value for the given string.
     *
     * @param value
     *        the string to map to an enumeration value
     * @return the correct enumeration value for the given string
     * @throws RuntimeException
     *         if no wrapper type is known for the given string
     */
    public static CheckinWorkItemAction fromString(final String value) {
        return fromWebServiceObject(_CheckinWorkItemAction.fromString(value));
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public String toUIString() {
        if (this == NONE) {
            return Messages.getString("CheckinWorkItemAction.None"); //$NON-NLS-1$
        } else if (this == ASSOCIATE) {
            return Messages.getString("CheckinWorkItemAction.Associate"); //$NON-NLS-1$
        } else if (this == RESOLVE) {
            return Messages.getString("CheckinWorkItemAction.Resolve"); //$NON-NLS-1$
        }

        throw new IllegalStateException(MessageFormat.format(
            "Can''t get UI string for work item action type {0}", //$NON-NLS-1$
            getWebServiceObject().toString()));
    }
}
