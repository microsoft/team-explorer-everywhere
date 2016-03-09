// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.reporting.ReportingClient;
import com.microsoft.tfs.core.clients.sharepoint.internal.WSSConstants;
import com.microsoft.tfs.core.ws.runtime.client.SOAPService;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

import ms.wss._ListsSoap;
import ms.wss._ListsSoapService;

/**
 * A read-only client for the Windows Sharepoint Services web services. The
 * client supports listing document libraries for a project, listing documents
 * within a library, and retrieving detailed information about documents.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class WSSClient {
    private final _ListsSoap webService;
    private final TFSTeamProjectCollection connection;

    /**
     * Creates a {@link WSSClient} with the given
     * {@link TFSTeamProjectCollection} and web service proxy.
     *
     * @param connection
     *        a valid {@link TFSTeamProjectCollection} (must not be
     *        <code>null</code>)
     * @param webService
     *        the {@link _ListsSoapService} web service (must not be
     *        <code>null</code>)
     * @param projectName
     *        the Sharepoint project name (must not be <code>null</code> or
     *        empty)
     */
    public WSSClient(final TFSTeamProjectCollection connection, final _ListsSoap webService, final String projectName) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(webService, "webService"); //$NON-NLS-1$
        Check.notNullOrEmpty(projectName, "projectName"); //$NON-NLS-1$

        this.connection = connection;
        this.webService = webService;
    }

    /**
     * <p>
     * TEE will automatically correct the endpoints registered URL when creating
     * the web service, however we must provide a mechansim to correct fully
     * qualified URI's provided as additional URI from the same webservice.
     * </p>
     * <p>
     * We compare the passed uri with the registered web service endpoint, if
     * they share the same root (i.e. http://TFSERVER) then we correct the
     * passed uri to be the same as the corrected web service enpoint (i.e.
     * http://tfsserver.mycompany.com)
     * </p>
     *
     * @see ReportingClient#getFixedURI(String)
     */
    public String getFixedURI(final String uri) {
        try {
            final String url = connection.getRegistrationClient().getServiceInterfaceURL(
                ToolNames.SHAREPOINT,
                WSSConstants.BASE_SITE_URL_SERVICE_NAME);

            if (url == null || url.length() == 0) {
                // We don't have a server wide link - just pass back what we
                // were given
                return uri;
            }

            final URI registeredEndpointUri = new URI(url);

            final URI passedUri = new URI(uri);

            if (passedUri.getScheme().equals(registeredEndpointUri.getScheme())
                && passedUri.getHost().equals(registeredEndpointUri.getHost())
                && passedUri.getPort() == registeredEndpointUri.getPort()) {
                final URI endpointUri = ((SOAPService) webService).getEndpoint();
                final URI fixedUri = new URI(
                    endpointUri.getScheme(),
                    endpointUri.getHost(),
                    passedUri.getPath(),
                    passedUri.getQuery(),
                    passedUri.getFragment());
                return fixedUri.toASCIIString();
            }
        } catch (final URISyntaxException e) {
            // ignore;
        }
        return uri;
    }

    /**
     * Gets the document libraries available for a project.
     *
     * @return an array of populated {@link WSSDocumentLibrary} objects for a
     *         project. May be empty but never null.
     */
    public WSSDocumentLibrary[] getDocumentLibraries(final boolean refresh) {
        final ArrayList docLibs = new ArrayList();

        final Element[] messageElement =
            ((DOMAnyContentType) webService.getListCollection(new DOMAnyContentType())).getElements();

        final Element[] elements = DOMUtils.getChildElements(messageElement[0]);
        for (int i = 0; i < elements.length; i++) {
            final Element element = elements[i];
            if ("101".equals(element.getAttribute("ServerTemplate"))) //$NON-NLS-1$ //$NON-NLS-2$
            {
                final WSSDocumentLibrary docLib = new WSSDocumentLibrary(connection, element);

                if (!docLib.isHidden() && docLib.isValid()) {
                    // Get Children to DocLib
                    docLib.addChildren(getListItems(docLib, webService));

                    // Add the doclib to the return array.
                    docLibs.add(docLib);
                }
            }
        }

        Collections.sort(docLibs);

        return (WSSDocumentLibrary[]) docLibs.toArray(new WSSDocumentLibrary[docLibs.size()]);
    }

    private List getListItems(final WSSDocumentLibrary docLib, final _ListsSoap proxy) {
        final DOMAnyContentType query = getListItemQuery(docLib);
        final DOMAnyContentType fields = getFields();
        final DOMAnyContentType options = getQueryOptions();

        // Microsoft client API limits to 50000 entires.
        final DOMAnyContentType anyContent =
            (DOMAnyContentType) proxy.getListItems(
                docLib.getGUID(),
                null,
                query,
                fields,
                "50000", //$NON-NLS-1$
                options,
                null,
                new DOMAnyContentType());

        if (anyContent != null && anyContent.getElements() != null && anyContent.getElements().length > 0) {
            return buildHierarchy(docLib, anyContent.getElements()[0]);
        }

        return new LinkedList();

    }

    /**
     * This is a two pass process. First we build all the elements into a list,
     * then we put everything into it's subfolder.
     */
    private LinkedList buildHierarchy(final WSSDocumentLibrary parent, final Element listData) {
        final Element data = DOMUtils.getChildElements(listData)[0];
        final int items = Integer.parseInt(data.getAttribute("ItemCount")); //$NON-NLS-1$

        final LinkedList list = new LinkedList();
        final HashMap folders = new HashMap(items);
        String rootFolder = null;

        // Build up the list of WSS Nodes
        final Element[] elements = DOMUtils.getChildElements(data);
        for (int i = 0; i < elements.length; i++) {
            final WSSNode node = WSSNode.buildWSSNode(elements[i]);

            if (WSSObjectType.FOLDER.equals(node.getWSSObjectType())) {
                folders.put(node.getFullPath(), node);
            }

            /* The folder with the shortest path is the "root" folder. */
            if (rootFolder == null || node.getPath().length() < rootFolder.length()) {
                rootFolder = node.getPath();
            }

            node.setParent(parent);
            list.addFirst(node);
        }

        // Put everything into it's parent folder
        final ListIterator it = list.listIterator();
        while (it.hasNext()) {
            final WSSNode node = (WSSNode) it.next();
            if (folders.containsKey(node.getPath())) {
                // This item is in a sub folder.
                ((WSSNode) folders.get(node.getPath())).addChild(node);
                it.remove();
            } else if (node.getPath().length() != rootFolder.length()) {
                /*
                 * This item is not in the root folder, or a subfolder that was
                 * given to us. Thus it is in a hidden folder.
                 */
                it.remove();
            }
        }

        return list;
    }

    /**
     * <p>
     * Generate the list item query. The query will contain one message element
     * representing the following XML.
     * </p>
     * <p>
     *
     * <pre>
     * &lt;Query xmlns=&quot;&quot;&gt; &lt;Where&gt; &lt;Contains&gt; &lt;FieldRef Name=&quot;FileRef&quot; /&gt; &lt;Value
     * Type=&quot;Text&quot;&gt;/Development/&lt;/Value&gt; &lt;/Contains&gt; &lt;OrderBy&gt; &lt;FieldRef
     * Name=&quot;FileDirRef&quot; Ascending=&quot;true&quot; /&gt; &lt;/OrderBy&gt; &lt;/Where&gt; &lt;/Query&gt;
     * </pre>
     *
     * </p>
     *
     * @param docLib
     *        the document library to generate the query for (must not be
     *        <code>null</code>)
     * @return the query to list items for the specified document library
     */
    public DOMAnyContentType getListItemQuery(final WSSDocumentLibrary docLib) {
        Check.notNull(docLib, "docLib"); //$NON-NLS-1$

        final Document document = DOMCreateUtils.newDocument("Query"); //$NON-NLS-1$
        final Element query = document.getDocumentElement();

        final Element where = DOMUtils.appendChild(query, "Where"); //$NON-NLS-1$
        final Element contains = DOMUtils.appendChild(where, "Contains"); //$NON-NLS-1$

        DOMUtils.appendChild(contains, "FieldRef").setAttribute("Name", "FileRef"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        final Element value = DOMUtils.appendChildWithText(contains, "Value", "/" + docLib.getASCIIName() + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        value.setAttribute("Type", "Text"); //$NON-NLS-1$ //$NON-NLS-2$

        final Element orderBy = DOMUtils.appendChild(where, "OrderBy"); //$NON-NLS-1$
        final Element fieldRef2 = DOMUtils.appendChild(orderBy, "FieldRef"); //$NON-NLS-1$
        fieldRef2.setAttribute("Name", "FileDirRef"); //$NON-NLS-1$ //$NON-NLS-2$
        fieldRef2.setAttribute("Ascending", "true"); //$NON-NLS-1$ //$NON-NLS-2$

        return new DOMAnyContentType(new Element[] {
            query
        });
    }

    /**
     * <p>
     * Get the view fields. Will return object containing one message element
     * representing the following XML.
     * </p>
     * <p>
     *
     * <pre>
     * &lt;ViewFields xmlns=&quot;&quot;&gt; &lt;ViewFields&gt; &lt;FieldRef Name=&quot;ID&quot; /&gt; &lt;FieldRef
     * Name=&quot;FSObjType&quot; /&gt; &lt;FieldRef Name=&quot;FileRef&quot; /&gt; &lt;FieldRef
     * Name=&quot;FileLeafRef&quot; /&gt; &lt;/ViewFields&gt; &lt;/ViewFields&gt;
     * </pre>
     *
     * </p>
     *
     * @return the view fields
     */
    public DOMAnyContentType getFields() {
        final Document document = DOMCreateUtils.newDocument("ViewFields"); //$NON-NLS-1$
        final Element root = document.getDocumentElement();

        final Element viewFields = DOMUtils.appendChild(root, "ViewFields"); //$NON-NLS-1$

        DOMUtils.appendChild(viewFields, "FieldRef").setAttribute("Name", "ID"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        DOMUtils.appendChild(viewFields, "FieldRef").setAttribute("Name", "FSObjType"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        DOMUtils.appendChild(viewFields, "FieldRef").setAttribute("Name", "FileRef"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        DOMUtils.appendChild(viewFields, "FieldRef").setAttribute("Name", "FileLeafRef"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        return new DOMAnyContentType(new Element[] {
            root
        });
    }

    /**
     * <p>
     * Get the query options containing a single message element for the
     * following:-
     * </p>
     * <p>
     *
     * <pre>
     * &lt;QueryOptions xmlns=&quot;&quot;&gt;
     * &lt;IncludeMandatoryColumns&gt;false&lt;/IncludeMandatoryColumns&gt; &lt;ViewAttributes
     * Scope=&quot;RecursiveAll&quot; /&gt; &lt;/QueryOptions&gt;
     * </pre>
     *
     * </p>
     *
     * @return the query options
     */
    public DOMAnyContentType getQueryOptions() {
        final Document document = DOMCreateUtils.newDocument("QueryOptions"); //$NON-NLS-1$
        final Element root = document.getDocumentElement();

        DOMUtils.appendChildWithText(root, "IncludeMandatoryColumns", "false"); //$NON-NLS-1$ //$NON-NLS-2$

        DOMUtils.appendChild(root, "ViewAttributes").setAttribute("Scope", "RecursiveAll"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        return new DOMAnyContentType(new Element[] {
            root
        });
    }
}
