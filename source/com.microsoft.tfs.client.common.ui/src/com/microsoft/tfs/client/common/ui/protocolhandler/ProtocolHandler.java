// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.protocolhandler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.git.utils.GitHelpers;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

/**
 *
 */
public class ProtocolHandler {
    private static final Log log = LogFactory.getLog(ProtocolHandler.class);

    private static final String GIT_TOKEN = "_git"; //$NON-NLS-1$

    private final static String PROTOCOL_HANDLER_ENCODING_PARAM = "EncFormat="; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_TFS_LINK_PARAM = "tfslink="; //$NON-NLS-1$

    // tfsLink items
    private final static String PROTOCOL_HANDLER_PROJECT_ITEM = "project"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_REPOSITORY_ITEM = "repository"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_CLONE_URL_ITEM = "cloneUrl"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_SERVER_URL_ITEM = "serverUrl"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_COLLECTION_ID_ITEM = "collectionId"; //$NON-NLS-1$
    private final static String PROTOCOL_HANDLER_BRANCH_ITEM = "ref"; //$NON-NLS-1$

    public final static String PROTOCOL_HANDLER_ARG = "-clonefromtfs"; //$NON-NLS-1$
    public final static String PROTOCOL_HANDLER_SCHEME = "vsoeclipse"; //$NON-NLS-1$

    private static final ProtocolHandler instance = new ProtocolHandler();

    private URI protocolHandlerUri;

    private boolean isParsed = false;
    private boolean isAvailable = false;
    private String encoding;

    // tfsLink values
    private final AtomicReference<String> serverUrl = new AtomicReference<String>();
    private final AtomicReference<String> cloneUrl = new AtomicReference<String>();
    private final AtomicReference<String> collectionId = new AtomicReference<String>();
    private final AtomicReference<String> repository = new AtomicReference<String>();
    private final AtomicReference<String> project = new AtomicReference<String>();
    private final AtomicReference<String> branchName = new AtomicReference<String>();

    private ProtocolHandler() {
        this.protocolHandlerUri =
            findProtocolHandlerUriArgument(org.eclipse.core.runtime.Platform.getApplicationArgs());
    }

    // For testing purposes
    protected ProtocolHandler(final String protocolHandlerUri) {
        this.protocolHandlerUri = URIUtils.newURI(protocolHandlerUri);
    }

    private Map<String, AtomicReference<String>> prepareTfsLinkItemMap() {
        final Map<String, AtomicReference<String>> t =
            new TreeMap<String, AtomicReference<String>>(String.CASE_INSENSITIVE_ORDER);

        t.put(PROTOCOL_HANDLER_SERVER_URL_ITEM, serverUrl);
        t.put(PROTOCOL_HANDLER_CLONE_URL_ITEM, cloneUrl);
        t.put(PROTOCOL_HANDLER_COLLECTION_ID_ITEM, collectionId);
        t.put(PROTOCOL_HANDLER_PROJECT_ITEM, project);
        t.put(PROTOCOL_HANDLER_REPOSITORY_ITEM, repository);
        t.put(PROTOCOL_HANDLER_BRANCH_ITEM, branchName);

        // The branch parameter is not sent if the repository is empty.
        // We cannot infer the master branch however, because an empty
        // repository does not have any branches at all.
        branchName.set(StringUtil.EMPTY);

        return t;
    }

    public static ProtocolHandler getInstance() {
        return instance;
    }

    public boolean hasProtocolHandlerRequest() {
        return tryParseProtocolHandlerUri();
    }

    public String getProtocolHandlerServerUrl() {
        tryParseProtocolHandlerUri();
        return isAvailable ? serverUrl.get() : StringUtil.EMPTY;
    }

    public String getProtocolHandlerCollectionId() {
        tryParseProtocolHandlerUri();
        return isAvailable ? collectionId.get() : StringUtil.EMPTY;
    }

    public String getProtocolHandlerProject() {
        tryParseProtocolHandlerUri();
        return isAvailable ? project.get() : StringUtil.EMPTY;
    }

    public String getProtocolHandlerBranch() {
        tryParseProtocolHandlerUri();
        return isAvailable ? URIUtils.decodeForDisplay(branchName.get()) : StringUtil.EMPTY;
    }

    public String getProtocolHandlerBranchForHtml() {
        tryParseProtocolHandlerUri();
        return isAvailable ? StringUtil.escapeXml(URIUtils.decodeForDisplay(branchName.get())) : StringUtil.EMPTY;
    }

    public String getProtocolHandlerRepository() {
        tryParseProtocolHandlerUri();
        return isAvailable ? repository.get() : StringUtil.EMPTY;
    }

    public String getProtocolHandlerRepositoryForHtml() {
        tryParseProtocolHandlerUri();
        return isAvailable ? StringUtil.escapeXml(repository.get()) : StringUtil.EMPTY;
    }

    public String getProtocolHandlerCloneUrl() {
        tryParseProtocolHandlerUri();
        return isAvailable ? cloneUrl.toString() : StringUtil.EMPTY;
    }

    public String getProtocolHandlerCloneUrlForWebAccess() {
        tryParseProtocolHandlerUri();
        // TODO: Find a better way to build the URL. Use TSWAHyperlinkBuilder?
        return isAvailable ? cloneUrl.get() + "?version=GB" + branchName.get() : StringUtil.EMPTY; //$NON-NLS-1$
    }

    public String getProtocolHandlerEncoding() {
        tryParseProtocolHandlerUri();
        return isAvailable ? encoding : StringUtil.EMPTY;
    }

    private URI findProtocolHandlerUriArgument(final String[] applicationArgs) {
        if (applicationArgs == null) {
            return null;
        }
        boolean found = false;

        /*
         * @formatter:off
         * We're looking for a protocol handler argument among all command line
         * arguments passed by eclipse.launcher to the Eclipse application. The
         * protocol handler argument should have the following syntax:
         * 
         * -clonefromtfs <uri>
         * 
         * At this point we do not parse the value of the argument. We'll do it later.
         * @formatter:on
         */

        for (final String arg : applicationArgs) {
            if (found) {
                log.info(MessageFormat.format(
                    "Found the protocol handler argument: {0} {1}", //$NON-NLS-1$
                    PROTOCOL_HANDLER_ARG,
                    arg));

                try {
                    return URIUtils.newURI(arg);
                } catch (final Exception e) {
                    log.error("   Incorrect URL in the protocol handler argument", e); //$NON-NLS-1$
                }

                break;
            } else if (arg.equalsIgnoreCase(PROTOCOL_HANDLER_ARG)) {
                found = true;
            }
        }

        return null;
    }

    /*
     * @formatter:off
     * The protocol handler argument generated by TFS should have
     * the following syntax:
     * 
     * -clonefromtfs vsoeclipse://checkout/?EncFormat=UTF8&tfslink=<base64 encoded parameters>
     * 
     * @formatter:on
     */
    private synchronized boolean tryParseProtocolHandlerUri() {
        if (isParsed) {
            return isAvailable;
        }
        isParsed = true;

        if (protocolHandlerUri == null) {
            return false;
        }

        if (!PROTOCOL_HANDLER_SCHEME.equalsIgnoreCase(protocolHandlerUri.getScheme())) {
            log.error(MessageFormat.format(
                "   Incorrect scheme in the protocol handler URL: {0}", //$NON-NLS-1$
                protocolHandlerUri.getScheme() == null ? "NULL" : protocolHandlerUri.getScheme())); //$NON-NLS-1$
            return false;
        }

        boolean tfsLinkAvailable = false;

        final String queryString = protocolHandlerUri.getQuery();
        if (StringUtil.isNullOrEmpty(queryString)) {
            log.error("   Incorrect (empty) query string in the protocol handler URL"); //$NON-NLS-1$
            return false;
        }

        final String[] queryItems = queryString.split("&"); //$NON-NLS-1$

        for (final String queryItem : queryItems) {
            if (StringUtil.startsWithIgnoreCase(queryItem, PROTOCOL_HANDLER_TFS_LINK_PARAM)) {
                final String value = queryItem.substring(PROTOCOL_HANDLER_TFS_LINK_PARAM.length());

                log.info(MessageFormat.format(
                    "   Found query parameter: {0}{1}", //$NON-NLS-1$
                    PROTOCOL_HANDLER_TFS_LINK_PARAM,
                    value));

                tfsLinkAvailable = tryParseTfsLink(value);

            } else if (StringUtil.startsWithIgnoreCase(queryItem, PROTOCOL_HANDLER_ENCODING_PARAM)) {
                final String value = queryItem.substring(PROTOCOL_HANDLER_ENCODING_PARAM.length());

                log.info(MessageFormat.format(
                    "   Found query parameter: {0}{1}", //$NON-NLS-1$
                    PROTOCOL_HANDLER_ENCODING_PARAM,
                    value));

                encoding = value;
            }
        }

        if (tfsLinkAvailable) {
            isAvailable = true;
        } else {
            log.error(MessageFormat.format(
                "   Incorrect or missing {0} query parameter in the protocol handler URL", //$NON-NLS-1$
                PROTOCOL_HANDLER_TFS_LINK_PARAM));
        }

        return isAvailable;
    }

    /*
     * @formatter:off
     * The decoded tfslink value generated by TFS should have
     * the following syntax:
     * 
     *     serverUrl=<server-url>&
     *     cloneUrl=<clone-url>&
     *     collectionId=<GUID>&
     *     project=<project-name>&
     *     repository=<repository-name>&
     *     Ref=<branch-name>
     * 
     * At this moment we ignore ideType and ideExe.
     * 
     * @formatter:on
     */
    private boolean tryParseTfsLink(final String tfsLink) {
        final String decodedTfsLink;
        try {
            decodedTfsLink = new String(Base64.decodeBase64(tfsLink), "UTF-8"); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            log.error("Incorrectly encoded the tfslink query parameter in the protocol handler URI", e); //$NON-NLS-1$
            return false;
        }

        final String[] tfsLinkItems = decodedTfsLink.split("&"); //$NON-NLS-1$
        Map<String, AtomicReference<String>> tfsLinkItemMap = prepareTfsLinkItemMap();

        for (final String tfsLinkItem : tfsLinkItems) {
            final int idx = tfsLinkItem.indexOf("="); //$NON-NLS-1$

            final String itemName;
            final String itemValue;
            if (idx < 0) {
                itemName = tfsLinkItem;
                itemValue = StringUtil.EMPTY;
            } else {
                itemName = tfsLinkItem.substring(0, idx);
                itemValue = tfsLinkItem.substring(idx + 1);
            }

            if (tfsLinkItemMap.containsKey(itemName)) {
                log.info(MessageFormat.format(
                    "                          {0}={1}", //$NON-NLS-1$
                    itemName,
                    itemValue));

                tfsLinkItemMap.get(itemName).set(itemValue);
            }
        }

        for (final AtomicReference<String> value : tfsLinkItemMap.values()) {
            if (value.get() == null) {
                return false;
            }
        }
        return true;
    }

    public void removeProtocolHandlerArguments() {
        isAvailable = false;
    }

    public ICommand getRegistrationCommand() {
        if (!GitHelpers.isEGitInstalled(false)) {
            return null;
        }

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            return new ProtocolHandlerWindowsRegistrationCommand();
        }

        return null;
    }
}
