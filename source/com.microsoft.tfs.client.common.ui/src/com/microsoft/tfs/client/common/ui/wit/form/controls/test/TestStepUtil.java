// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.util.xml.DOMUtils;

public class TestStepUtil {

    private static final String FIELD_PARAMETERS = "Microsoft.VSTS.TCM.Parameters"; //$NON-NLS-1$

    public static Document parse(final String xml) throws ParserConfigurationException, SAXException, IOException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    public static boolean isTestStep(final Object element) {
        if (element.getClass() == TestStep.class) {
            return true;
        }
        if (element instanceof Node) {
            final String name = ((Node) element).getNodeName();
            return (name != null && name.equals("step")); //$NON-NLS-1$
        }
        return false;
    }

    public static boolean isSharedSteps(final Object element) {
        if (element.getClass() == SharedSteps.class) {
            return true;
        }
        if (element instanceof Node) {
            final String name = ((Node) element).getNodeName();
            return (name != null && name.equals("compref")); //$NON-NLS-1$
        }
        return false;
    }

    public static String getParameterizedString(final Node n) {
        final Node[] nodes = DOMUtils.getChildElements(n);
        String str = ""; //$NON-NLS-1$
        for (int i = 0; i < nodes.length; i++) {
            final String text = DOMUtils.getText(nodes[i]);
            if (nodes[i].getNodeName().equals("parameter")) //$NON-NLS-1$
            {
                str += " @" + text + " "; //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (nodes[i].getNodeName().equals("text")) //$NON-NLS-1$
            {
                str += text;
            }
        }
        return str;
    }

    public static TestStep[] extractSteps(final Node node, final WorkItem wi, final String fieldName)
        throws ParserConfigurationException,
            SAXException,
            IOException {
        final Node[] nodes = DOMUtils.getChildElements(node);
        final ArrayList steps = new ArrayList();
        for (int i = 0; i < nodes.length; i++) {
            if (isTestStep(nodes[i])) {
                steps.add(new TestStep(nodes[i], wi));
            }
            if (isSharedSteps(nodes[i])) {
                final SharedSteps sharedSteps = new SharedSteps(nodes[i], wi, fieldName);
                steps.add(sharedSteps);
                steps.addAll(Arrays.asList(extractSteps(nodes[i], wi, fieldName)));
            }
        }
        return (TestStep[]) steps.toArray(new TestStep[0]);
    }

    public static String getAttr(final Node n, final String attr) {
        final Node attrNode = n.getAttributes().getNamedItem(attr);
        if (attrNode != null) {
            return attrNode.getNodeValue();
        }
        return null;
    }

    public static final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public static Param[] extractParams(final WorkItem wi, final TestStep[] steps)
        throws ParserConfigurationException,
            SAXException,
            IOException {
        final ArrayList params = new ArrayList();
        // Extract parameters from current work item
        params.addAll(Arrays.asList(paramsFromField(wi.getFields().getField(FIELD_PARAMETERS))));
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] instanceof SharedSteps) {
                // Extract parameters from referred work item
                final SharedSteps sharedStep = (SharedSteps) steps[i];
                params.addAll(Arrays.asList(paramsFromField(sharedStep.paramField)));
            }
        }
        return (Param[]) params.toArray(new Param[0]);
    }

    public static Param[] paramsFromField(final Field f)
        throws ParserConfigurationException,
            SAXException,
            IOException {
        if (f != null && f.getValue() != null) {
            final String xml = f.getValue().toString();
            final Document doc = parse(xml);
            if (doc != null) {
                final Element[] paramNodes = DOMUtils.getChildElements(doc.getDocumentElement(), "param"); //$NON-NLS-1$
                final Param[] params = new Param[paramNodes.length];
                for (int i = 0; i < params.length; i++) {
                    params[i] = new Param(paramNodes[i]);
                }
                return params;
            }
        }
        return null;
    }

    public static ParamDataTable[] extractParamData(final String xml) {
        Node n;
        try {
            n = parse(xml).getDocumentElement();
        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
            return new ParamDataTable[0];
        } catch (final SAXException e) {
            e.printStackTrace();
            return new ParamDataTable[0];
        } catch (final IOException e) {
            e.printStackTrace();
            return new ParamDataTable[0];
        }

        final Node[] tableNodes = DOMUtils.getChildElements(n, "Table1"); //$NON-NLS-1$
        final ParamDataTable[] tables = new ParamDataTable[tableNodes.length];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = new ParamDataTable(tableNodes[i]);
        }
        return tables;
    }

}
