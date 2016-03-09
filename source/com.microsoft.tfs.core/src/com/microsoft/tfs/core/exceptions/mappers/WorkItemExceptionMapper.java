// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.exceptions.mappers;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.exceptions.ItemAlreadyUpdatedOnServerException;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnauthorizedAccessException;
import com.microsoft.tfs.core.clients.workitem.exceptions.ValidationException;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemLinkValidationException;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;

/**
 * <p>
 * The {@link WorkItemExceptionMapper} class is used to map exceptions during a
 * call to the Work Item Tracking web service into an appropriate exception that
 * the WIT object model should throw. The exceptions that are translated are
 * generally {@link SOAPFault}s.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class WorkItemExceptionMapper extends TECoreExceptionMapper {
    /*
     * This class is similar in purpose to a class named "OME" in the .NET OM.
     */

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
            final Element detailElement = (Element) detailNode;
            final NodeList children = detailElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                final Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    final Element childElement = (Element) child;
                    if ("details".equals(childElement.getNodeName())) //$NON-NLS-1$
                    {
                        final String sId = childElement.getAttribute("id"); //$NON-NLS-1$
                        if (sId != null && sId.length() > 0) {
                            try {
                                final int id = Integer.parseInt(sId);
                                return mapSoapFaultDetailsID(id, soapFault);
                            } catch (final NumberFormatException ex) {

                            }
                        }
                    }
                }
            }
        }

        return soapFault;
    }

    private static RuntimeException mapSoapFaultDetailsID(final int id, final SOAPFault soapFault) {
        switch (id) {
            /*
             * see:Microsoft.TeamFoundation.WorkItemTracking.Client.OME.
             * GetMappingUnauthorizedAccessException
             */
            case 600031:
                return new UnauthorizedAccessException(
                    Messages.getString("WorkItemExceptionMapper.ItemDoesNotExistOrNoPermission"), //$NON-NLS-1$
                    soapFault,
                    id);

            case 600035:
                return new UnauthorizedAccessException(
                    Messages.getString("WorkItemExceptionMapper.NotRecognizedAdministratorInTeamProject"), //$NON-NLS-1$
                    soapFault,
                    id);

            /*
             * see:Microsoft.TeamFoundation.WorkItemTracking.Client.OME.
             * GetMappingValidationException
             */
            case 600036:
                return new ValidationException(
                    Messages.getString("WorkItemExceptionMapper.QueryNameAlreadyExists"), //$NON-NLS-1$
                    soapFault,
                    id,
                    ValidationException.Type.NOT_UNIQUE_STORED_QUERY);

            /*
             * This is the error returned when a work item can't be updated
             * because of a permissions issue.
             *
             * This is not the same exception that Visual Studio's OM would
             * throw. Their code would catch the lack of permissions on the
             * client side before the update to the server, and throw a
             * ValidationException.
             */
            case 600072:
                return new WorkItemException(
                    Messages.getString("WorkItemExceptionMapper.NoPermissionToUpdateWorkItem")); //$NON-NLS-1$

            /*
             * see:Microsoft.TeamFoundation.WorkItemTracking.Client.OME.
             * MapBackendException
             */
            case 600122:
                return new ItemAlreadyUpdatedOnServerException(soapFault);

            case 600269:
                return new WorkItemLinkValidationException(
                    Messages.getString("WorkItemExceptionMapper.CannotAddLinkBecauseTypeDisabled"), //$NON-NLS-1$
                    soapFault,
                    id,
                    WorkItemLinkValidationException.Type.ADD_LINK_DISABLED_TYPE);

            case 600270:
                return new WorkItemLinkValidationException(
                    Messages.getString("WorkItemExceptionMapper.LinkWouldResultInCircularRelationship"), //$NON-NLS-1$
                    soapFault,
                    id,
                    WorkItemLinkValidationException.Type.ADD_LINK_CIRCULARITY);

            case 600271:
                return new WorkItemLinkValidationException(
                    Messages.getString("WorkItemExceptionMapper.CanOnlyHaveOneLinkOfThisType"), //$NON-NLS-1$
                    soapFault,
                    id,
                    WorkItemLinkValidationException.Type.ADD_LINK_EXTRA_PARENT);

            case 600272:
                return new WorkItemLinkValidationException(
                    Messages.getString("WorkItemExceptionMapper.LinkWouldResultInCircularRelationship"), //$NON-NLS-1$
                    soapFault,
                    id,
                    WorkItemLinkValidationException.Type.ADD_LINK_CHILD_IS_ANCESTOR);

            case 600273:
                return new WorkItemLinkValidationException(
                    Messages.getString("WorkItemExceptionMapper.LinkToWorkItemAlreadyExists"), //$NON-NLS-1$
                    soapFault,
                    id,
                    WorkItemLinkValidationException.Type.ADD_LINK_ALREADY_EXISTS);

            case 600278:
                return new WorkItemLinkValidationException(
                    Messages.getString("WorkItemExceptionMapper.LinkBetweenWorkItemsWouldExceedMaximum"), //$NON-NLS-1$
                    soapFault,
                    id,
                    WorkItemLinkValidationException.Type.ADD_LINK_MAX_DEPTH_EXCEEDED);
        }

        return soapFault;
    }
}
