// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers;

import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.TestStepUtil;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.Field;

public class ParamDataContentProvider extends ContentProviderAdapter {
    private static final String FIELD_DATA_SOURCE = "Microsoft.VSTS.TCM.LocalDataSource"; //$NON-NLS-1$

    @Override
    public Object[] getElements(final Object inputElement) {
        if (inputElement instanceof WorkItem) {
            final WorkItem workItem = (WorkItem) inputElement;
            final Field field = workItem.getFields().getField(FIELD_DATA_SOURCE);
            final Object fieldValue = field.getValue();
            final String s = (fieldValue == null) ? "" : fieldValue.toString(); //$NON-NLS-1$
            return TestStepUtil.extractParamData(s);
        }

        return new Object[0];
    }
}
