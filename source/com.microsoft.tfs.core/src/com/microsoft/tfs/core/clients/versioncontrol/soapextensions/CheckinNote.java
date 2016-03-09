// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.CheckinNoteNameValidationException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.xml.DOMUtils;

import ms.tfs.versioncontrol.clientservices._03._CheckinNote;
import ms.tfs.versioncontrol.clientservices._03._CheckinNoteFieldValue;

/**
 * Contains supplemental information (possibly required, depending on team
 * project configuration) provided by the user during checkin which becomes part
 * of a {@link Changeset}.
 *
 * @since TEE-SDK-10.1
 */
public class CheckinNote extends WebServiceObjectWrapper {
    // XML Constants compatible with VS
    //
    // <CheckinNotes>
    // <Note name="Internationalization Impact">
    // Checkin note text goes here.
    // </Note>
    // <Note name="Performance Impact">
    // Other checkin note text goes here.
    // </Note>
    // </CheckinNotes>

    public static final String XML_CHECKIN_NOTES = "CheckinNotes"; //$NON-NLS-1$
    public static final String XML_NOTE = "Note"; //$NON-NLS-1$
    public static final String XML_NOTE_NAME = "name"; //$NON-NLS-1$

    /**
     * The maximum size allowed for checkin note names.
     */
    public static final int CHECKIN_NOTE_NAME_MAX_SIZE_CHARS = 64;

    public CheckinNote() {
        super(new _CheckinNote());
    }

    public CheckinNote(final _CheckinNote checkinNote) {
        super(checkinNote);
    }

    public CheckinNote(final CheckinNoteFieldValue[] values) {
        super(new _CheckinNote(toWebServiceObjectArray(values)));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _CheckinNote getWebServiceObject() {
        return (_CheckinNote) webServiceObject;
    }

    private static _CheckinNoteFieldValue[] toWebServiceObjectArray(final CheckinNoteFieldValue[] values) {
        Check.notNull(values, "values"); //$NON-NLS-1$

        final _CheckinNoteFieldValue[] ret = new _CheckinNoteFieldValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = values[i].getWebServiceObject();
        }

        return ret;
    }

    /**
     * Check if the specified string is a valid checkin note name and strip any
     * leading and trailing whitespace characters. Throw an exception if the
     * name is invalid.
     *
     * @param name
     *        String containing checkin note name.
     * @return Canonicalized name String.
     */
    public static String canonicalizeName(String name) {
        // Remove all leading and trailing whitespace.
        if (name != null) {
            name = name.trim();
        }

        // Check for an empty name.
        if (name == null || name.length() == 0) {
            throw new CheckinNoteNameValidationException(
                Messages.getString("CheckinNote.SuppliedCheckinNoteNameWasEmpty")); //$NON-NLS-1$
        }

        // Check for invalid characters.
        // TODO: Using Character.IsISOControl to determine if control character.
        // Possibly replace this with a regex
        // containing all .NET control characters?
        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) {
                throw new CheckinNoteNameValidationException(
                    Messages.getString("CheckinNote.SuppliedCheckinNoteNameContainedAnIllegalCharacter"), //$NON-NLS-1$
                    name);
            }
        }

        // Check the length of the checkin note name.
        if (name.length() > CHECKIN_NOTE_NAME_MAX_SIZE_CHARS) {
            throw new CheckinNoteNameValidationException(
                MessageFormat.format(
                    Messages.getString("CheckinNote.SuppliedCheckinNoteNameTooLongMaximumSizeFormat"), //$NON-NLS-1$
                    CHECKIN_NOTE_NAME_MAX_SIZE_CHARS),
                name);
        }

        return name;

    }

    public CheckinNoteFieldValue[] getValues() {
        final _CheckinNote note = getWebServiceObject();

        if (note == null || note.getValues() == null || note.getValues().length == 0) {
            return new CheckinNoteFieldValue[0];
        }

        final _CheckinNoteFieldValue[] values = note.getValues();
        final CheckinNoteFieldValue[] ret = new CheckinNoteFieldValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = new CheckinNoteFieldValue(values[i]);
        }

        return ret;
    }

    /**
     * Get an iterator over the array of CheckInNoteFieldValues.
     */
    public synchronized Iterator iterator() {
        return Arrays.asList(getWebServiceObject().getValues()).iterator();
    }

    /**
     * Creates an instance from the XML representation used in the cache file.
     *
     * @param checkinNoteNode
     *        the check notes node (must not be <code>null</code>)
     */
    public static CheckinNote loadFromXML(final Element checkinNoteNode) {
        final List<CheckinNoteFieldValue> fieldValues = new ArrayList<CheckinNoteFieldValue>();

        for (final Element child : DOMUtils.getChildElements(checkinNoteNode)) {
            if (child.getNodeName().equals(XML_NOTE)) {
                final String fieldName = child.getAttributes().getNamedItem(XML_NOTE_NAME).getNodeValue();
                final String fieldValue = DOMUtils.getText(child);
                fieldValues.add(new CheckinNoteFieldValue(fieldName, fieldValue));
            }
        }

        return new CheckinNote(fieldValues.toArray(new CheckinNoteFieldValue[fieldValues.size()]));
    }

    /**
     * Saves this instance to the XML format used in the cache file.
     */
    public synchronized void saveAsXML(final Element parent) {
        final Element checkinNoteNode = DOMUtils.appendChild(parent, XML_CHECKIN_NOTES);

        for (final CheckinNoteFieldValue fieldValue : getValues()) {
            final Element noteNode = DOMUtils.appendChildWithText(checkinNoteNode, XML_NOTE, fieldValue.getValue());
            noteNode.setAttribute(XML_NOTE_NAME, fieldValue.getName());
        }
    }
}
