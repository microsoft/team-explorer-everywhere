// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.utils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.microsoft.tfs.core.clients.build.internal.BuildTypeInfo;

/**
 * A simple SAX handler to parse BuildTypeInfo objects from V1 TFSBuild.proj
 * file.
 */
public class BasicBuildTypeParseHandler extends DefaultHandler {
    private final BuildTypeInfo buildTypeInfo;
    private final StringBuffer contents = new StringBuffer();

    public BasicBuildTypeParseHandler(final String buildTypeName) {
        buildTypeInfo = new BuildTypeInfo(buildTypeName);
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(final char[] ch, final int start, final int length) throws SAXException {
        contents.append(new String(ch, start, length));
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
        throws SAXException {
        contents.setLength(0);
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(final String uri, final String localName, final String name) throws SAXException {
        if (name == null || name.length() == 0) {
            return;
        }

        if ("Description".equalsIgnoreCase(name)) //$NON-NLS-1$
        {
            buildTypeInfo.setDescription(contents.toString());
        } else if ("BuildMachine".equalsIgnoreCase(name)) //$NON-NLS-1$
        {
            buildTypeInfo.setBuildMachine(contents.toString());
        } else if ("DropLocation".equalsIgnoreCase(name)) //$NON-NLS-1$
        {
            buildTypeInfo.setDropLocation(contents.toString());
        } else if ("BuildDirectoryPath".equalsIgnoreCase(name)) //$NON-NLS-1$
        {
            buildTypeInfo.setBuildDir(contents.toString());
        } else if ("ProjectFileVersion".equalsIgnoreCase(name)) //$NON-NLS-1$
        {
            buildTypeInfo.setVersion(contents.toString());
        }
    }

    /**
     * @return the buildTypeInfo
     */
    public BuildTypeInfo getBuildTypeInfo() {
        return buildTypeInfo;
    }

}
