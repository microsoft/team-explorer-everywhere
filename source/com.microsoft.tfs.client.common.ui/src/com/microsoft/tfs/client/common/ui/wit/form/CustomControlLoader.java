// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.wit.form.controls.ErrorBoxControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.IWorkItemControl;

class CustomControlLoader {
    private static final Log log = LogFactory.getLog(CustomControlLoader.class);

    public static final String EXTENSION_POINT_ID = "workItemControls"; //$NON-NLS-1$
    public static final String EXTENSION_CONFIGURATION_ELEMENT_NAME = "workItemControl"; //$NON-NLS-1$

    public static final String ATT_ID = "id"; //$NON-NLS-1$
    public static final String ATT_TYPE = "controlType"; //$NON-NLS-1$
    public static final String ATT_CLASS = "class"; //$NON-NLS-1$

    private final IConfigurationElement configElement;

    private final String id;
    private final String controlType;

    private IWorkItemControl failedToLoadControl;

    public CustomControlLoader(final IConfigurationElement configElement) {
        this.configElement = configElement;
        id = getStringAttribute(this.configElement, ATT_ID, null);
        controlType = getStringAttribute(this.configElement, ATT_TYPE, null);

        // Make sure class is defined - do not load yet.
        getStringAttribute(this.configElement, ATT_CLASS, null);
    }

    public IWorkItemControl getControl() {
        if (failedToLoadControl != null) {
            return failedToLoadControl;
        }

        IWorkItemControl control;
        try {
            control = (IWorkItemControl) configElement.createExecutableExtension(ATT_CLASS);
        } catch (final CoreException e) {
            log.error("Error instantiating custom work item control class", e); //$NON-NLS-1$
            control = new ErrorBoxControl(e.getLocalizedMessage());
            failedToLoadControl = control;
        }
        return control;
    }

    private static String getStringAttribute(
        final IConfigurationElement configElement,
        final String name,
        final String defaultValue) {
        final String value = configElement.getAttribute(name);
        if (value != null) {
            return value;
        }
        if (defaultValue != null) {
            return defaultValue;
        }

        final String messageFormat = Messages.getString("CustomControlLoader.MissingAttributeFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, name);
        throw new IllegalArgumentException(message);
    }

    public String getControlType() {
        return controlType;
    }

    public String getID() {
        return id;
    }

}
