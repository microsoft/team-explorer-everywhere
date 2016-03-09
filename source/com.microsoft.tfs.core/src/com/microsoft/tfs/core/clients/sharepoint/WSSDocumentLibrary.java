// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.util.Hierarchical;
import com.microsoft.tfs.core.util.Labelable;
import com.microsoft.tfs.util.Check;

/**
 * Object representing a document library in Sharepoint.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class WSSDocumentLibrary implements Hierarchical, Labelable, Comparable {
    private static final Log log = LogFactory.getLog(WSSDocumentLibrary.class);

    private final String guid;
    private String label;
    private boolean hidden;
    private Object parent;
    private final boolean isValid;
    private String asciiName;

    private final String EMPTY = ""; //$NON-NLS-1$

    private final ArrayList children = new ArrayList();;
    private boolean childrenSorted = false;

    private String defaultViewURL;

    public WSSDocumentLibrary() {
        // Default constructor only here to make testing easier.
        guid = EMPTY;
        label = EMPTY;
        isValid = false;
    }

    public WSSDocumentLibrary(final TFSTeamProjectCollection connection, final Element node) {
        /*
         * An example of a message element is as follows:-
         *
         * <ns1:List DocTemplateUrl="/sites/TEE/Development/Forms/template.doc"
         * DefaultViewUrl ="/sites/TEE/Development/Forms/AllItems.aspx"
         * ID="{C0260C8E-B4F2-420C-A6FA-C9787EEB1237}" Title="Development"
         * Description="" ImageUrl="/_layouts/images/itdl.gif"
         * Name="{C0260C8E-B4F2-420C-A6FA-C9787EEB1237}" BaseType="1"
         * ServerTemplate="101" Created="20060220 20:39:26" Modified=
         * "20060220 20:39:26" LastDeleted="20060220 20:39:26" Version="0"
         * Direction="none" ThumbnailSize="" WebImageWidth="" WebImageHeight=""
         * Flags="25169920" ItemCount="0" AnonymousPermMask="" RootFolder=""
         * ReadSecurity="1" WriteSecurity="1" Author="1" EventSinkAssembly=""
         * EventSinkClass="" EventSinkData="" EmailInsertsFolder=""
         * AllowDeletion="True" AllowMultiResponses="False"
         * EnableAttachments="True" EnableModeration="False"
         * EnableVersioning="False" Hidden="False" MultipleDataList="False"
         * Ordered="False" ShowUser="True"
         * xmlns:ns1="http://schemas.microsoft.com/sharepoint/soap/"/>
         */

        guid = node.getAttribute("ID"); //$NON-NLS-1$
        label = node.getAttribute("Title"); //$NON-NLS-1$
        defaultViewURL = node.getAttribute("DefaultViewUrl"); //$NON-NLS-1$
        final String isHidden = node.getAttribute("Hidden"); //$NON-NLS-1$

        if (isHidden != null) {
            hidden = Boolean.valueOf(isHidden).booleanValue();
        }

        asciiName = calculateASCIIName(connection, node);

        isValid =
            (asciiName != null) && (guid != null) && (guid.length() > 0) && (label != null) && (label.length() > 0);

    }

    private String calculateASCIIName(final TFSTeamProjectCollection connection, final Element node) {
        // Attempt to work out the ascii name of the document library. This
        // encapsulates the logic found in
        // Microsoft.TeamFoundation.Client.SharePoint.DocumentLibraryInfo.FromXmlElement
        // in the .NET OM

        String name = null;

        String defaultViewUrl = EMPTY;
        if (node.hasAttribute("DefaultViewUrl")) //$NON-NLS-1$
        {
            defaultViewUrl = node.getAttribute("DefaultViewUrl"); //$NON-NLS-1$
        }

        String webFullUrl = EMPTY;
        if (node.hasAttribute("WebFullUrl")) //$NON-NLS-1$
        {
            webFullUrl = node.getAttribute("WebFullUrl"); //$NON-NLS-1$
        }

        name = calculateASCIINameFromWebFullURL(defaultViewUrl, webFullUrl);
        if (name != null) {
            return name;
        }

        // We're now into back-compat territory - just return back label as that
        // is all the old client used to do
        return label;

        // TODO for full .NET OM compatibility do, however not sure that this is
        // needed due to back-compat behaviour in our catalog service layer
        // name = calculateAsciiNameFromRegistrationData(connection,
        // defaultViewUrl);
        // if (name != null)
        // {
        // return name;
        // }
        //
        // name = calculateAsciiNameFromTemplateUrl(defaultViewUrl);
        // if (name != null)
        // {
        // return name;
        // }
    }

    protected String calculateASCIINameFromWebFullURL(final String defaultViewUrl, final String webFullUrl) {
        if (defaultViewUrl == null || defaultViewUrl.length() == 0 || webFullUrl == null || webFullUrl.length() == 0) {
            return null;
        }
        if (defaultViewUrl.startsWith(webFullUrl) && webFullUrl.length() + 1 < defaultViewUrl.length()) {
            // for example
            // DefaultViewUrl="/sites/TEE/TEE/Reports/Forms/AllItems.aspx"
            // and WebFullUrl="/sites/TEE/TEE"
            // then asciiName == "Reports"
            int startPos = webFullUrl.length();
            if (!webFullUrl.endsWith("/")) //$NON-NLS-1$
            {
                startPos++;
            }
            final String value = defaultViewUrl.substring(startPos).trim();
            final int endPos = value.indexOf('/');
            if (endPos < 0) {
                return value;
            }
            return value.substring(0, endPos);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.util.Hierarchical#getParent()
     */
    @Override
    public Object getParent() {
        return parent;
    }

    /**
     * @param parent
     *        The parent to set.
     */
    public void setParent(final Object parent) {
        this.parent = parent;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.util.Hierarchical#getChildren()
     */
    @Override
    public Object[] getChildren() {
        if (!childrenSorted) {
            Collections.sort(children);
            childrenSorted = true;
        }

        return children.toArray();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.util.Hierarchical#hasChildren()
     */
    @Override
    public boolean hasChildren() {
        return children.size() > 0;
    }

    public void addChild(final Object child) {
        children.add(child);
        childrenSorted = false;
    }

    public void addChildren(final Collection children) {
        this.children.addAll(children);
        childrenSorted = false;
    }

    /**
     * @return Returns the defaultViewUrl.
     */
    public String getDefaultViewURL() {
        return defaultViewURL;
    }

    /**
     * @param defaultViewUrl
     *        The defaultViewUrl to set.
     */
    public void setDefaultViewURL(final String defaultViewUrl) {
        this.defaultViewURL = defaultViewUrl;
    }

    /**
     * @return Returns the guid.
     */
    public String getGUID() {
        return guid;
    }

    /**
     * @return Returns the hidden.
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * @param hidden
     *        The hidden to set.
     */
    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * @return Returns the label.
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * @param label
     *        The label to set.
     */
    public void setLabel(final String label) {
        Check.notNull(label, "label"); //$NON-NLS-1$

        this.label = label;
    }

    public String getASCIIName() {
        return asciiName;
    }

    public void setASCIIName(final String asciiName) {
        this.asciiName = asciiName;
    }

    public boolean isValid() {
        return isValid;
    }

    @Override
    public int compareTo(final Object compare) {
        if (!(compare instanceof WSSDocumentLibrary)) {
            throw new ClassCastException(
                MessageFormat.format(
                    "A WssDocumentLibrary to compare against was expected, class passed was {0}", //$NON-NLS-1$
                    compare.getClass().getName()));
        }

        final WSSDocumentLibrary anotherLib = (WSSDocumentLibrary) compare;

        return getLabel().compareTo(anotherLib.getLabel());
    }

}
