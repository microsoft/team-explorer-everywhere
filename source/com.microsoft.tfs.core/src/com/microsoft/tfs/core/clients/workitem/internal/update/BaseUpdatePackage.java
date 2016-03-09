// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.internal.InternalWorkItemUtils;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.exceptions.mappers.WorkItemExceptionMapper;
import com.microsoft.tfs.core.ws.runtime.exceptions.SOAPFault;
import com.microsoft.tfs.core.ws.runtime.types.AnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.StaxAnyContentType;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;

import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap_UpdateResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap_UpdateResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap_UpdateResponse;

public abstract class BaseUpdatePackage {
    private final WITContext context;
    private final Element root;

    public BaseUpdatePackage(final WITContext context) {
        this.context = context;

        final Document document = DOMCreateUtils.newDocument(UpdateXMLConstants.ELEMENT_NAME_PACKAGE);
        root = document.getDocumentElement();
        root.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_PRODUCT, context.getProductValue());
    }

    protected WITContext getContext() {
        return context;
    }

    protected Element getRoot() {
        return root;
    }

    public void update() {
        /*
         * call the web service
         */
        final AnyContentType metadata;
        final String dbStamp;
        final AnyContentType result;
        try {
            if (context.isVersion2()) {
                final _ClientService2Soap_UpdateResponse response = context.getProxy().update(
                    getUpdatePackage(),
                    context.getMetadataUpdateHandler().getHaveEntries(),
                    new DOMAnyContentType(),
                    new StaxAnyContentType());
                metadata = response.getMetadata();
                dbStamp = response.getDbStamp();
                result = response.getResult();
            } else if (context.isVersion3()) {
                final _ClientService3Soap_UpdateResponse response = context.getProxy3().update(
                    getUpdatePackage(),
                    context.getMetadataUpdateHandler().getHaveEntries(),
                    new DOMAnyContentType(),
                    new StaxAnyContentType());
                metadata = response.getMetadata();
                dbStamp = response.getDbStamp();
                result = response.getResult();
            } else {
                final _ClientService5Soap_UpdateResponse response = context.getProxy5().update(
                    getUpdatePackage(),
                    context.getMetadataUpdateHandler().getHaveEntries(),
                    new DOMAnyContentType(),
                    new StaxAnyContentType());
                metadata = response.getMetadata();
                dbStamp = response.getDbStamp();
                result = response.getResult();
            }

        } catch (final SOAPFault soapFault) {
            throw WorkItemExceptionMapper.map(soapFault);
        }

        /*
         * update metadata
         */
        context.getMetadataUpdateHandler().updateMetadata(metadata, dbStamp);
        metadata.dispose();

        /*
         * handle the update response
         */
        handleUpdateResponse((DOMAnyContentType) result);
    }

    protected abstract void handleUpdateResponse(DOMAnyContentType response);

    protected DOMAnyContentType getUpdatePackage() {
        return new DOMAnyContentType(new Element[] {
            root
        });
    }

    public String getUpdateXML() {
        return DOMSerializeUtils.toString(root, DOMSerializeUtils.INDENT);
    }

    protected Date parseDate(final String input) {
        final DateFormat dateFormat = InternalWorkItemUtils.newMetadataDateFormat();
        try {
            return dateFormat.parse(input);
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
