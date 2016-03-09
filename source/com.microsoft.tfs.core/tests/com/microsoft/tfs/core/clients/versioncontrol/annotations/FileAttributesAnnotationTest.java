// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.annotations;

import java.text.ParseException;

import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.BooleanFileAttribute;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttribute;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeImpl;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributeNames;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.FileAttributesEntry;
import com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes.StringPairFileAttribute;

import junit.framework.TestCase;

public class FileAttributesAnnotationTest extends TestCase {
    public void testFileAttributesAnnotation() {
        assertTrue(new FileAttributesEntry("abc").matchesFilename("abc")); //$NON-NLS-1$ //$NON-NLS-2$

        BooleanFileAttribute attr = new BooleanFileAttribute(FileAttributeNames.EXECUTABLE);
        FileAttributesEntry entry = new FileAttributesEntry("abc", new FileAttribute[] //$NON-NLS-1$
        {
            attr
        });

        assertTrue(entry.matchesFilename("abc")); //$NON-NLS-1$

        /*
         * One executable attribute.
         */
        assertTrue(entry.getAttributes().containsAttribute(FileAttributeNames.EXECUTABLE));
        assertEquals(1, entry.getAttributes().size());

        final FileAttributeImpl foundAttr =
            entry.getAttributes().getBooleanFileAttribute(FileAttributeNames.EXECUTABLE);
        assertEquals(foundAttr, attr);
        assertEquals(FileAttributeNames.EXECUTABLE, foundAttr.getName());

        /*
         * Add a string pair attribute.
         */
        final String stringPairString = "abcxyz=some value we can't parse as a known key"; //$NON-NLS-1$
        final FileAttributeImpl firstStringPair = StringPairFileAttribute.parse(stringPairString);

        attr = new BooleanFileAttribute(FileAttributeNames.EXECUTABLE);
        entry = new FileAttributesEntry("abc", new FileAttribute[] //$NON-NLS-1$
        {
            attr,
            firstStringPair
        });
        assertEquals(2, entry.getAttributes().size());
        assertEquals(firstStringPair, entry.getAttributes().getStringPairFileAttribute("abcxyz")); //$NON-NLS-1$

        /*
         * Add a second string pair attribute.
         */
        final String stringPairString2 = "LMNOPQRSTUV=some other value we can't parse as a known key"; //$NON-NLS-1$
        final FileAttributeImpl secondStringPair = StringPairFileAttribute.parse(stringPairString2);
        entry = new FileAttributesEntry("abc", new FileAttribute[] //$NON-NLS-1$
        {
            attr,
            firstStringPair,
            secondStringPair
        });
        assertEquals(3, entry.getAttributes().size());
        assertEquals(firstStringPair, entry.getAttributes().getStringPairFileAttribute("abcxyz")); //$NON-NLS-1$
        assertEquals(secondStringPair, entry.getAttributes().getStringPairFileAttribute("LMNOPQRSTUV")); //$NON-NLS-1$
    }

    public void testDeserialization() {
        final String serializedValue = "filename.java:=123|x|ABC123=this one has an unknown key||so does this=one|x=|y"; //$NON-NLS-1$

        FileAttributesEntry entry = null;
        try {
            entry = FileAttributesEntry.parse(serializedValue);
        } catch (final ParseException e) {
            assertTrue(false);
        }

        assertTrue(entry.matchesFilename("filename.java")); //$NON-NLS-1$
        assertFalse(entry.matchesFilename("filename.javaxxx")); //$NON-NLS-1$
        assertFalse(entry.matchesFilename("a")); //$NON-NLS-1$

        assertEquals(4, entry.getAttributes().size());
        assertNotNull(entry.getAttributes().getBooleanFileAttribute("y")); //$NON-NLS-1$
        assertNull(entry.getAttributes().getBooleanFileAttribute("x")); //$NON-NLS-1$
        assertEquals(
            "this one has an unknown key", //$NON-NLS-1$
            entry.getAttributes().getStringPairFileAttribute("ABC123").getValue()); //$NON-NLS-1$
        assertEquals("one", entry.getAttributes().getStringPairFileAttribute("so does this").getValue()); //$NON-NLS-1$ //$NON-NLS-2$
        assertEquals("", entry.getAttributes().getStringPairFileAttribute("x").getValue()); //$NON-NLS-1$ //$NON-NLS-2$

        boolean threw = false;
        try {
            entry.getAttributes().getStringPairFileAttribute(""); //$NON-NLS-1$
        } catch (final Exception e) {
            threw = true;
        }

        assertTrue(threw);
    }

    public void testSerialization() {
        /*
         * Construct a file attributes annotation, serialize it to string, parse
         * it back, then compare both annotations.
         */
        final String filename = "filename.java"; //$NON-NLS-1$
        final FileAttributesEntry entry1 = new FileAttributesEntry(filename, new FileAttribute[] {
            new BooleanFileAttribute(FileAttributeNames.EXECUTABLE),
            new StringPairFileAttribute("some silly key", "some silly value"), //$NON-NLS-1$ //$NON-NLS-2$
            new StringPairFileAttribute("some other key", "some other = value") //$NON-NLS-1$ //$NON-NLS-2$
        });

        final String serialized = entry1.toString();

        FileAttributesEntry entry2 = null;
        try {
            entry2 = FileAttributesEntry.parse(serialized);
        } catch (final ParseException e) {
            assertTrue(false);
        }

        assertTrue(entry2.matchesFilename(filename));

        assertEquals(3, entry2.getAttributes().size());

        for (final FileAttribute newAttribute : entry2.getAttributes()) {
            // Find this one in the old attribute.
            boolean found = false;
            for (final FileAttribute oldAttribute : entry1.getAttributes()) {
                if (oldAttribute.getClass().equals(newAttribute.getClass())
                    && oldAttribute.getName().equals(newAttribute.getName())) {
                    if (oldAttribute instanceof StringPairFileAttribute) {
                        if (((StringPairFileAttribute) oldAttribute).getValue().equals(
                            ((StringPairFileAttribute) newAttribute).getValue())) {
                            found = true;
                            break;
                        }
                    } else {
                        found = true;
                        break;
                    }
                }
            }

            assertTrue(found);
        }
    }
}
