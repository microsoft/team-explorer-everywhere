// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._FileType;

/**
 * Represents a single server-registered file type. It has a name, an associated
 * list of file extensions which define names which match this type, and a
 * boolean indicating whether the file can be checked out non-exclusively.
 *
 * @since TEE-SDK-10.1
 */
public class FileType extends WebServiceObjectWrapper {
    /**
     * Construct a new, empty {@link FileType}, with a null name and no
     * associated extensions.
     */
    public FileType() {
        super(new _FileType());
    }

    /**
     * Construct an AFileType given a soap-returned FileType object. All
     * attributes of the new AFileType object are driven from the FileType
     * object.
     *
     * @param fileType
     */
    public FileType(final _FileType fileType) {
        super(fileType);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _FileType getWebServiceObject() {
        return (_FileType) webServiceObject;
    }

    /**
     * @return the descriptive name of this file type
     */
    public String getName() {
        return getWebServiceObject().getName();
    }

    /**
     * Gets the list of associated extensions for this file type in a List form
     *
     * @return list of extensions
     */
    public List<String> getExtensions() {
        return new ArrayList<String>(Arrays.asList(getWebServiceObject().getExtensions()));
    }

    /**
     * Gets the list of associated extensions for this file type, suitable for
     * displaying only. The extensions will be returned as a semicolon separated
     * list of strings.
     *
     * @return list of associated extensions for display purposes
     */
    public String getDisplayExtensions() {
        final String[] extensionArray = getWebServiceObject().getExtensions();
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < extensionArray.length; i++) {
            sb.append(extensionArray[i]);
            if (i < extensionArray.length - 1) {
                sb.append("; "); //$NON-NLS-1$
            }
        }
        return sb.toString();
    }

    /**
     * Gets the list of associated extensions for this file type, suitable for
     * editing. After modification, the returned String can be passed back to
     * this object by calling setEditingExtensions(). The extensions will be
     * returned as a semicolon separated list of strings, each with a *.
     * prepended to it.
     *
     * @return list of associated extensions for editing purposes
     */
    public String getEditingExtensions() {
        final String[] extensions = getWebServiceObject().getExtensions();
        if (extensions == null) {
            return null;
        }

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < extensions.length; i++) {
            sb.append("*." + extensions[i]); //$NON-NLS-1$
            if (i < extensions.length - 1) {
                sb.append("; "); //$NON-NLS-1$
            }
        }
        return sb.toString();
    }

    /**
     * Sets the associated extensions for this file type. The given string can
     * be comprised of a single extension or multiple extensions. If multiple,
     * the extensions must be separated by either commas or semicolons.
     * Whitespace surrounding extensions is ignored, and a leading *. if present
     * will be stripped from each extension. The following are all valid inputs
     * to this method: txt *.txt txt, bat, *exe *.txt; *.bat, *.exe
     *
     * @param s
     *        extensions to set
     */
    public void setEditingExtensions(final String s) {
        final String[] parts = s.split("[,;]"); //$NON-NLS-1$
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            if (parts[i].startsWith("*.")) //$NON-NLS-1$
            {
                parts[i] = parts[i].substring(2);
            }
        }

        getWebServiceObject().setExtensions(parts);
    }

    /**
     * @return <code>true</code> if the file type allows multiple check-outs.
     */
    public boolean isAllowMultipleCheckout() {
        return getWebServiceObject().isMulti();
    }

    /**
     * Sets whether this file type allows multiple check-outs.
     *
     * @param allowMultipleCheckout
     *        <code>true</code> if this file type should allow multiple
     *        check-outs.
     */
    public void setAllowMultipleCheckout(final boolean allowMultipleCheckout) {
        getWebServiceObject().setMulti(allowMultipleCheckout);
    }

    /**
     * Set the name of this file type.
     *
     * @param name
     *        name to use
     */
    public void setName(final String name) {
        getWebServiceObject().setName(name);
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "{0} name [{1}] extensions [{2}] multi [{3}]", //$NON-NLS-1$
            FileType.class.getName(),
            getWebServiceObject().getName(),
            (getWebServiceObject().getExtensions() == null ? "null" : Arrays.asList( //$NON-NLS-1$
                getWebServiceObject().getExtensions()).toString()),
            getWebServiceObject().isMulti());
    }
}
