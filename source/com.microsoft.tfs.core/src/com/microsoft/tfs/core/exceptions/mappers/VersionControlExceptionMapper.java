// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions.mappers;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ActionDeniedBySubscriberException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.PathTooLongException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ReconcileBlockedByProjectRenameException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ResourceAccessException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.TeamFoundationServerExceptionProperties;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.WorkspaceNotFoundException;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;

/**
 * Maps exceptions for the version control client.
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class VersionControlExceptionMapper extends TECoreExceptionMapper {
    /**
     * @see TECoreExceptionMapper#map(RuntimeException)
     */
    public static RuntimeException map(final RuntimeException e) {
        if (e instanceof SOAPFault) {
            final RuntimeException betterException = mapSoapFault((SOAPFault) e);

            /*
             * Only return the better exception if it's different than what was
             * supplied. Otherwise, we want to use the TECoreExceptionMapper's
             * logic.
             */
            if (betterException != e) {
                return betterException;
            }
        }

        // Defer to the basic core mapper.
        return TECoreExceptionMapper.map(e);
    }

    private static RuntimeException mapSoapFault(final SOAPFault soapFault) {
        final Node detailNode = soapFault.getDetail();

        if (detailNode != null) {
            VersionControlException newException = null;

            final String subCode = soapFault.getSubCode().getSubCode().getLocalPart();

            if (subCode.equals("ActionDeniedBySubscriberException")) //$NON-NLS-1$
            {
                newException = new ActionDeniedBySubscriberException(soapFault.getMessage());
            } else if (subCode.equals("WorkspaceNotFoundException")) //$NON-NLS-1$
            {
                newException = new WorkspaceNotFoundException(soapFault.getMessage(), soapFault);
            } else if (subCode.equals("ResourceAccessException")) //$NON-NLS-1$
            {
                newException = new ResourceAccessException(soapFault.getMessage(), soapFault);
            } else if (subCode.equals("ReconcileBlockedByProjectRenameException")) //$NON-NLS-1$
            {
                newException = new ReconcileBlockedByProjectRenameException(soapFault.getMessage(), soapFault);
            } else if (subCode.equals("PathTooLongException")) //$NON-NLS-1$
            {
                newException = new VersionControlException(
                    new PathTooLongException(detailNode.getAttributes().getNamedItem("ExceptionMessage").toString())); //$NON-NLS-1$
            }

            if (newException != null) {
                final TeamFoundationServerExceptionProperties properties = readProperties(detailNode);
                newException.setProperties(properties);
                return newException;
            }
        }

        return soapFault;
    }

    private static TeamFoundationServerExceptionProperties readProperties(final Node detailNode) {
        if (detailNode.getNodeType() == Node.ELEMENT_NODE) {
            final Element detailElement = (Element) detailNode;
            final NodeList nodes = detailElement.getElementsByTagName("ExceptionProperties"); //$NON-NLS-1$

            if (nodes != null && nodes.getLength() > 0) {
                return new TeamFoundationServerExceptionProperties((Element) nodes.item(0));
            }
        }

        return null;
    }
}
