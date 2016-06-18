// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;
import com.microsoft.tfs.util.xml.DOMUtils;
import com.microsoft.tfs.util.xml.DefaultErrorHandler;
import com.microsoft.tfs.util.xml.DocumentBuilderCache;
import com.microsoft.tfs.util.xml.JAXPUtils;
import com.microsoft.tfs.util.xml.XMLException;

/**
 * <p>
 * A {@link WIQDocument} represents a specific type of document used to store
 * work item query information. A {@link WIQDocument} is closely tied to its
 * default serialized form, which is an XML document. XML documents in this
 * format are usually stored as files with a .wiq extension, which is where the
 * name of this class comes from.
 * </p>
 *
 * <p>
 * A {@link WIQDocument} at minimum contains a non-<code>null</code> WIQL string
 * and an integer version. There are also several other pieces of optional
 * information that a {@link WIQDocument} may contain.
 * </p>
 *
 * <p>
 * A {@link WIQDocument} is immutable and threadsafe, and is an in-memory
 * representation of the query data.
 * </p>
 *
 * <p>
 * You can construct a {@link WIQDocument} by passing individual data values to
 * a constructor, or you can call static methods to load a {@link WIQDocument}
 * from a {@link File} ({@link #load(File)}) or XML {@link Document} (
 * {@link #load(Document)}).
 * </p>
 *
 * <p>
 * You can produce an XML {@link Document} from a {@link WIQDocument} by calling
 * {@link #toXMLDocument()}, and you can serialize a {@link WIQDocument} to a
 * {@link File} by calling {@link #save(File)}.
 * </p>
 *
 * @since TEE-SDK-10.1
 */
public class WIQDocument {
    private static final int DEFAULT_VERSION = 1;

    private static final String XML_SCHEMA_RESOURCE_NAME = "wiq.xsd"; //$NON-NLS-1$

    private static final String VERSION_ATTRIBUTE_NAME = "Version"; //$NON-NLS-1$
    private static final String WORK_ITEM_QUERY_ELEMENT_NAME = "WorkItemQuery"; //$NON-NLS-1$
    private static final String WIQL_ELEMENT_NAME = "Wiql"; //$NON-NLS-1$
    private static final String TEAM_FOUNDATION_SERVER_ELEMENT_NAME = "TeamFoundationServer"; //$NON-NLS-1$
    private static final String TEAM_PROJECT_ELEMENT_NAME = "TeamProject"; //$NON-NLS-1$
    private static final String TEAM_NAME_ELEMENT_NAME = "TeamName"; //$NON-NLS-1$

    private static final Object docBuilderCacheLock = new Object();
    private static DocumentBuilderCache docBuilderCache;

    private static DocumentBuilderCache getDocBuilderCache() {
        synchronized (docBuilderCacheLock) {
            if (docBuilderCache != null) {
                return docBuilderCache;
            }

            final DocumentBuilderFactory factory = createDocumentBuilderFactory();
            docBuilderCache = new DocumentBuilderCache(factory, new DefaultErrorHandler(), null);

            return docBuilderCache;
        }
    }

    private static DocumentBuilderFactory createDocumentBuilderFactory() {
        /*
         * Hardcode a dependency on Xerces - the Java 1.4 built-in Crimson
         * parser does not support JAXP 1.2, which we need for XMLSchema
         * validation.
         */
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        final InputStream schemaStream = WIQDocument.class.getResourceAsStream(XML_SCHEMA_RESOURCE_NAME);

        if (schemaStream == null) {
            throw new WIQDocumentParseException(Messages.getString("WiqDocument.UnableToLoadWiqSchema")); //$NON-NLS-1$
        }

        try {
            final byte[] schema = IOUtils.toByteArray(schemaStream);
            return JAXPUtils.newDocumentBuilderFactoryForXSValidation(factory, new ByteArrayInputStream(schema));
        } catch (final IOException e) {
            throw new WIQDocumentParseException(e);
        }
    }

    private final String wiql;
    private final String teamFoundationServer;
    private final String teamProject;
    private final String teamName;
    private final int version;

    public static WIQDocument load(final File file) throws WIQDocumentParseException {
        Check.notNull(file, "file"); //$NON-NLS-1$

        Document document;

        final DocumentBuilderCache cache = getDocBuilderCache();

        final DocumentBuilder builder = cache.takeDocumentBuilder();

        try {
            document = DOMCreateUtils.parseFile(builder, file, DOMCreateUtils.ENCODING_UTF8);
        } catch (final XMLException e) {
            throw new WIQDocumentParseException(e);
        } finally {
            cache.releaseDocumentBuilder(builder);
        }

        return load(document);
    }

    public static WIQDocument load(final Document document) throws WIQDocumentParseException {
        Check.notNull(document, "document"); //$NON-NLS-1$

        final Element rootElement = document.getDocumentElement();

        int version = DEFAULT_VERSION;
        if (rootElement.hasAttribute(VERSION_ATTRIBUTE_NAME)) {
            try {
                version = Integer.parseInt(rootElement.getAttribute(VERSION_ATTRIBUTE_NAME));
            } catch (final NumberFormatException e) {
                // ignore, version will be DEFAULT_VERSION
            }
        }

        String wiql = null;
        String teamFoundationServer = null;
        String teamProject = null;
        String teamName = null;

        final Element[] rootElementChildren = DOMUtils.getChildElements(rootElement);
        for (int i = 0; i < rootElementChildren.length; i++) {
            final Element currentChild = rootElementChildren[i];
            if (WIQL_ELEMENT_NAME.equals(currentChild.getLocalName())) {
                wiql = DOMUtils.getText(currentChild).trim();
                if (wiql.trim().length() == 0) {
                    wiql = null;
                }
            } else if (TEAM_FOUNDATION_SERVER_ELEMENT_NAME.equals(currentChild.getLocalName())) {
                teamFoundationServer = DOMUtils.getText(currentChild).trim();
                if (teamFoundationServer.trim().length() == 0) {
                    teamFoundationServer = null;
                }
            } else if (TEAM_PROJECT_ELEMENT_NAME.equals(currentChild.getLocalName())) {
                teamProject = DOMUtils.getText(currentChild).trim();
                if (teamProject.trim().length() == 0) {
                    teamProject = null;
                }
            } else if (TEAM_NAME_ELEMENT_NAME.equals(currentChild.getLocalName())) {
                teamName = DOMUtils.getText(currentChild).trim();
                if (teamName.trim().length() == 0) {
                    teamName = null;
                }
            }
        }

        if (wiql == null) {
            final String messageFormat = Messages.getString("WiqDocument.DocuementDidNotContainElementFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, WIQL_ELEMENT_NAME);
            throw new WIQDocumentParseException(message);
        }

        return new WIQDocument(wiql, teamFoundationServer, teamProject, teamName, version);
    }

    public WIQDocument(
        final String wiql,
        final String teamFoundationServer,
        final String teamProject,
        final String teamName) {
        this(wiql, teamFoundationServer, teamProject, teamName, DEFAULT_VERSION);
    }

    public WIQDocument(
        final String wiql,
        final String teamFoundationServer,
        final String teamProject,
        final String teamName,
        final int version) {
        Check.notNull(wiql, "wiql"); //$NON-NLS-1$

        this.wiql = wiql;
        this.teamFoundationServer = teamFoundationServer;
        this.teamProject = teamProject;
        this.teamName = teamName;
        this.version = version;
    }

    public String getWIQL() {
        return wiql;
    }

    public String getTeamFoundationServer() {
        return teamFoundationServer;
    }

    public String getTeamProject() {
        return teamProject;
    }

    public String getTeamName() {
        return teamName;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        final String messageFormat = "wiql: [{0}] tfs: [{1}] project: [{2}] version: [{3}]"; //$NON-NLS-1$
        return MessageFormat.format(messageFormat, wiql, teamFoundationServer, teamProject, Integer.toString(version));
    }

    public Document toXMLDocument() {
        final Document document = DOMCreateUtils.newDocument(WORK_ITEM_QUERY_ELEMENT_NAME);
        final Element root = document.getDocumentElement();
        root.setAttribute(VERSION_ATTRIBUTE_NAME, String.valueOf(version));

        if (teamFoundationServer != null) {
            String serverUrlToSave = teamFoundationServer;

            /*
             * make sure we end in a trailing slash to be identical to Visual
             * Studio's implementation
             */
            if (!serverUrlToSave.endsWith("/")) //$NON-NLS-1$
            {
                serverUrlToSave = serverUrlToSave + "/"; //$NON-NLS-1$
            }

            DOMUtils.appendChildWithText(root, TEAM_FOUNDATION_SERVER_ELEMENT_NAME, serverUrlToSave);
        }

        if (teamProject != null) {
            DOMUtils.appendChildWithText(root, TEAM_PROJECT_ELEMENT_NAME, teamProject);
        }

        if (teamName != null) {
            DOMUtils.appendChildWithText(root, TEAM_NAME_ELEMENT_NAME, teamName);
        }

        DOMUtils.appendChildWithText(root, WIQL_ELEMENT_NAME, wiql);

        return document;
    }

    public void save(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        DOMSerializeUtils.serializeToFile(
            toXMLDocument(),
            file,
            DOMSerializeUtils.ENCODING_UTF8,
            DOMSerializeUtils.XML_DECLARATION | DOMSerializeUtils.BYTE_ORDER_MARK);
    }
}
