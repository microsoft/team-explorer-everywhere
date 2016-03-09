// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.SingleValueOrFileOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.util.Check;

public final class OptionNotes extends SingleValueOrFileOption {
    private CheckinNote notes = new CheckinNote();

    public OptionNotes() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        super.parseValues(optionValueString);

        /*
         * Turn the single string we got from the parent into multiple lines so
         * we can parse them easier.
         */

        Check.notNull(super.getValue(), "super.getValue()"); //$NON-NLS-1$

        final StringReader sr = new StringReader(super.getValue());
        final BufferedReader br = new BufferedReader(sr);

        final ArrayList lines = new ArrayList();

        try {
            String l = null;
            while ((l = br.readLine()) != null) {
                lines.add(l);
            }

            sr.close();
            br.close();
        } catch (final IOException e) {
            final String messageFormat = Messages.getString("OptionNotes.OptionValueCouldNotBeParsedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getLocalizedMessage());

            throw new InvalidOptionValueException(message);
        }

        /*
         * Each line will be like one of:
         *
         * key=value
         *
         * "key"="value"
         *
         * "key"="value";"key2"="value2"
         */

        final List noteFieldValues = new ArrayList();

        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.get(i);
            Check.notNull(line, "line"); //$NON-NLS-1$

            /*
             * Trim beginning and trailing whitespace.
             */
            line = line.trim();

            /*
             * Ignore blank lines (mainly for option files).
             */
            if (line.length() == 0) {
                continue;
            }

            /*
             * Split at the semicolon first.
             */
            final String[] subNotes = line.split(";", 0); //$NON-NLS-1$

            for (int j = 0; j < subNotes.length; j++) {
                final String note = subNotes[j];
                Check.notNull(note, "note"); //$NON-NLS-1$

                if (note.length() == 0) {
                    throw new InvalidOptionValueException(Messages.getString("OptionNotes.NotMustNotBeEmpty")); //$NON-NLS-1$
                }

                /*
                 * Split at the equals sign.
                 */
                final String[] subStrings = note.split("=", 2); //$NON-NLS-1$

                if (subStrings.length != 2) {
                    throw new InvalidOptionValueException(
                        Messages.getString("OptionNotes.NotesOptionMustBeFilenameOrPair")); //$NON-NLS-1$
                }

                /*
                 * Remove any quotes at the beginning and end of the strings.
                 */
                for (int k = 0; k < subStrings.length; k++) {
                    subStrings[k].trim();
                    subStrings[k] = subStrings[k].replaceAll("^\"", "").replaceAll("\"$", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

                    if (subStrings[k].length() == 0) {
                        throw new InvalidOptionValueException(
                            Messages.getString("OptionNotes.NotesFieldAndValueMustNotBeEmpty")); //$NON-NLS-1$
                    }
                }

                // Add it to the big list.
                noteFieldValues.add(new CheckinNoteFieldValue(subStrings[0], subStrings[1]));
            }
        }

        notes = new CheckinNote((CheckinNoteFieldValue[]) noteFieldValues.toArray(new CheckinNoteFieldValue[0]));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.SingleValueOption#getValue()
     */
    @Override
    public final String getValue() {
        throw new IllegalStateException("OptionNotes does not support getValue().  Use getNotes() instead."); //$NON-NLS-1$
    }

    /**
     * Gets the parsed notes object.
     *
     * @return the parsed notes object.
     */
    public CheckinNote getNotes() {
        return notes;
    }

    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix()
            + getMatchedAlias()
            + ":\"note\"=\"value\"[;\"note2\"=\"value2\"[;...]]|@notefile"; //$NON-NLS-1$
    }
}
