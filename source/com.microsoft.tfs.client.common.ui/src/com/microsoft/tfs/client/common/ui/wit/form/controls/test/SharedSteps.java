// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.util.xml.DOMUtils;

public class SharedSteps extends TestStep {

    private static final String FIELD_PARAMETERS = "Microsoft.VSTS.TCM.Parameters"; //$NON-NLS-1$

    public TestStep[] subSteps = new TestStep[0];

    public Field paramField = null;

    public String title = ""; //$NON-NLS-1$

    public SharedSteps(final Node node, final WorkItem wi, final String testStepsField)
        throws ParserConfigurationException,
            SAXException,
            IOException {
        super(node, wi);
        initRefWorkItem(node, wi, testStepsField);
    }

    private void initRefWorkItem(final Node node, final WorkItem workItem, final String testStepsField)
        throws ParserConfigurationException,
            SAXException,
            IOException {
        final Node refNode = node.getAttributes().getNamedItem("ref"); //$NON-NLS-1$
        if (refNode != null) {
            final String ref = refNode.getNodeValue();
            final WorkItem wi = workItem.getClient().getWorkItemByID(Integer.parseInt(ref));

            if (wi != null) {
                final Field field = wi.getFields().getField(testStepsField);
                if (field.getValue() != null) {
                    final Document d = TestStepUtil.parse(field.getValue().toString());
                    subSteps = TestStepUtil.extractSteps(d.getDocumentElement(), wi, testStepsField);
                    final Object obj = wi.getFields().getField(CoreFieldReferenceNames.TITLE).getValue();
                    title = (obj == null) ? "" : obj.toString(); //$NON-NLS-1$
                }
                paramField = wi.getFields().getField(FIELD_PARAMETERS);
            }
        }
    }

    Node[] extractParameters(final WorkItem wi) throws ParserConfigurationException, SAXException, IOException {
        final Field f = wi.getFields().getField(FIELD_PARAMETERS);
        if (f.getValue() != null) {
            final Element[] elements =
                DOMUtils.getChildElements(TestStepUtil.parse(f.getValue().toString()).getDocumentElement(), "param"); //$NON-NLS-1$
            return elements;
        }
        return new Node[0];
    }

}