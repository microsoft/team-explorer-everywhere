// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.TestStep;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.TestStepUtil;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.Field;

public class TestStepContentProvider extends ContentProviderAdapter {
    private static final Log log = LogFactory.getLog(TestStepContentProvider.class);
    private static final String FIELD_STEPS = "Microsoft.VSTS.TCM.Steps"; //$NON-NLS-1$

    @Override
    public Object[] getElements(final Object inputElement) {
        if (inputElement instanceof WorkItem) {
            final WorkItem workItem = (WorkItem) inputElement;
            final Field field = workItem.getFields().getField(FIELD_STEPS);
            final Object fieldValue = field.getValue();

            if (fieldValue != null) {
                try {
                    final Document doc = TestStepUtil.parse(fieldValue.toString());
                    final TestStep[] steps =
                        TestStepUtil.extractSteps(doc.getDocumentElement(), field.getWorkItem(), field.getName());

                    return steps;
                } catch (final ParserConfigurationException e) {
                    log.error("Error in TestStepControl", e); //$NON-NLS-1$
                } catch (final SAXException e) {
                    log.error("Error in TestStepControl", e); //$NON-NLS-1$
                } catch (final IOException e) {
                    log.error("Error in TestStepControl", e); //$NON-NLS-1$
                }
            }
        }

        return new TestStep[0];
    }
}