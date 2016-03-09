// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.wit.form.controls.AssociatedAutomationControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.AttachmentsControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.ClassificationControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.DateTimeControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.ErrorBoxControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.FieldControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.GroupControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.HTMLFieldControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.IWorkItemControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.LegacyHTMLFieldControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.LinkLabelControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.LinksControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.LogControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.PlainTextControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.SplitterControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.TabGroupControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.WebPageControl;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.TestStepsControl;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.form.WIFormControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormControlTypeConstants;
import com.microsoft.tfs.core.clients.workitem.form.WIFormDescription;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormGroup;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLayout;
import com.microsoft.tfs.core.clients.workitem.form.WIFormSplitter;
import com.microsoft.tfs.core.clients.workitem.form.WIFormTabGroup;

public class Helpers {
    /* Teamprise layout name retained for compatibility */
    private static final String WINFORMS_LAYOUT_TARGET = "WinForms"; //$NON-NLS-1$
    private static final String TEAMPRISE_LAYOUT_TARGET = "Teamprise"; //$NON-NLS-1$
    private static final String SWT_LAYOUT_TARGET = "JavaSWT"; //$NON-NLS-1$

    private static HashMap customControls = null;
    private static Object customControlLock = new Object();

    private static final Log log = LogFactory.getLog(Helpers.class);

    public static int getMinimalNumberOfColumnsRequired(final IWorkItemControl[] controls) {
        int numColumns = 0;

        for (int i = 0; i < controls.length; i++) {
            if (controls[i].getMinimumRequiredColumnCount() > numColumns) {
                numColumns = controls[i].getMinimumRequiredColumnCount();
            }
        }

        return numColumns;
    }

    public static IWorkItemControl[] getControls(final WIFormElement[] formElements, final FormContext formContext) {
        final IWorkItemControl[] controls = new IWorkItemControl[formElements.length];
        for (int i = 0; i < controls.length; i++) {
            controls[i] = getControl(formElements[i], formContext);
            controls[i].init(formElements[i], formContext);
        }
        return controls;
    }

    /**
     * Get a work item control for the given form element
     *
     * @param formElement
     *        The element in the work item type definition to create a control
     *        from
     *
     * @param formContext
     *        Work item form context
     *
     * @return A new work item control
     */
    public static IWorkItemControl getControl(final WIFormElement formElement, final FormContext formContext) {
        IWorkItemControl control = null;

        /*
         * First try the "PreferredType" for this control, if specified.
         * (PreferredType is only valid for Form Control elements)
         */
        if (formElement instanceof WIFormControl) {
            final WIFormControl formControl = (WIFormControl) formElement;
            if (formControl.getPreferredType() != null) {
                control = getControl(formElement, formContext, true);
            }
        }

        /*
         * If PreferredType was not specified, or if a control of that type
         * could not be loaded, then load a control of "Type" instead (the
         * default).
         */
        if (control == null || control instanceof ErrorBoxControl) {
            control = getControl(formElement, formContext, false);
        }

        return control;
    }

    /**
     * Get a work item control for the given form element
     *
     * @param formElement
     *        The element in the work item type definition to create a control
     *        from
     *
     * @param formContext
     *        Work item form context
     *
     * @param usePreferredType
     *        If true, load the "PreferredType". If false, load the "Type".
     *
     * @return A new work item control
     */
    private static IWorkItemControl getControl(
        final WIFormElement formElement,
        final FormContext formContext,
        final boolean usePreferredType) {
        // Attempt to allocate an internal control for this form element.
        final IWorkItemControl internalControl =
            getControlInternal(formElement, formContext.getWorkItem(), usePreferredType);
        boolean isErrorControl = internalControl instanceof ErrorBoxControl;

        if (internalControl != null && !isErrorControl) {
            return internalControl;
        }

        // There is no internal control so try to create a custom control.
        final IWorkItemControl customControl =
            getControlCustom(formElement, formContext.getWorkItem(), usePreferredType);
        isErrorControl = customControl instanceof ErrorBoxControl;

        if (customControl != null && !isErrorControl) {
            return customControl;
        }

        // Return null or the error box control.
        return customControl == null ? internalControl : customControl;
    }

    public static IWorkItemControl getControlCustom(
        final WIFormElement formElement,
        final WorkItem workItem,
        final boolean usePreferredType) {
        if (!(formElement instanceof WIFormControl)) {
            // Custom controls are only valid for form controls (not tabs,
            // splitters etc)
            return null;
        }

        IWorkItemControl control = null;
        try {
            synchronized (customControlLock) {
                if (customControls == null) {
                    loadCustomControls();
                }

                final WIFormControl controlDescription = (WIFormControl) formElement;
                final String controlType =
                    usePreferredType ? controlDescription.getPreferredType() : controlDescription.getType();

                final CustomControlLoader loader = (CustomControlLoader) customControls.get(controlType);

                if (loader != null) {
                    control = loader.getControl();
                }
            }
        } catch (final Exception e) {
            // We encountered an error loading the control, return an error
            // control with the error message.
            log.error("Error loading custom control", e); //$NON-NLS-1$
            control = new ErrorBoxControl(e.getLocalizedMessage());
        }

        return control;
    }

    private static void loadCustomControls() {
        customControls = new HashMap();

        final IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(
            TFSCommonUIClientPlugin.PLUGIN_ID,
            CustomControlLoader.EXTENSION_POINT_ID);

        if (point == null) {
            return;
        }

        final IExtension[] extensions = point.getExtensions();
        for (int i = 0; i < extensions.length; i++) {
            final IConfigurationElement[] ces = extensions[i].getConfigurationElements();
            for (int j = 0; j < ces.length; j++) {
                if (ces[j].getName().equals(CustomControlLoader.EXTENSION_CONFIGURATION_ELEMENT_NAME)) {
                    // Note that we just wrap up the found extension
                    // implementation in the control loader at this point
                    // we do not actually load the class (and so take the memory
                    // hit of loading the plugin) until that
                    // control is references by an opened work item.
                    final CustomControlLoader loader = new CustomControlLoader(ces[j]);
                    customControls.put(loader.getControlType(), loader);
                }
            }
        }
    }

    public static IWorkItemControl getControlInternal(
        final WIFormElement formElement,
        final WorkItem workItem,
        final boolean usePreferredType) {
        if (formElement instanceof WIFormGroup) {
            return new GroupControl();
        } else if (formElement instanceof WIFormTabGroup) {
            return new TabGroupControl();
        } else if (formElement instanceof WIFormSplitter) {
            return new SplitterControl();
        } else if (formElement instanceof WIFormControl) {
            final WIFormControl controlDescription = (WIFormControl) formElement;
            final String controlType =
                usePreferredType ? controlDescription.getPreferredType() : controlDescription.getType();

            if (WIFormControlTypeConstants.ATTACHMENTS_CONTROL.equals(controlType)) {
                return new AttachmentsControl();
            } else if (WIFormControlTypeConstants.LINKS_CONTROL.equals(controlType)) {
                return new LinksControl();
            } else if (WIFormControlTypeConstants.WORK_ITEM_LOG_CONTROL.equals(controlType)) {
                return new LogControl();
            } else if (WIFormControlTypeConstants.WORK_ITEM_CLASSIFICATION_CONTROL.equals(controlType)) {
                return new ClassificationControl();
            } else if (WIFormControlTypeConstants.FIELD_CONTROL.equals(controlType)) {
                return new FieldControl();
            } else if (WIFormControlTypeConstants.HTML_FIELD_CONTROL.equals(controlType)) {
                final String fieldName = controlDescription.getFieldName();

                if (fieldName != null) {
                    if (workItem.getFields().getField(
                        fieldName).getFieldDefinition().getFieldType() == FieldType.HTML) {
                        if (HTMLFieldControl.isAvailable()) {
                            return new HTMLFieldControl();
                        } else {
                            return new LegacyHTMLFieldControl();
                        }
                    }
                }

                // WITFieldType.PLAINTEXT
                return new PlainTextControl();
            } else if (WIFormControlTypeConstants.DATE_TIME_CONTROL.equals(controlType)) {
                return new DateTimeControl();
            } else if (WIFormControlTypeConstants.ASSOCIATED_AUTOMATION_CONTROL.equals(controlType)) {
                return new AssociatedAutomationControl();
            } else if (WIFormControlTypeConstants.REPRO_STEPS_CONTROL.equals(controlType)) {
                if (HTMLFieldControl.isAvailable()) {
                    return new HTMLFieldControl();
                } else {
                    return new LegacyHTMLFieldControl();
                }
            } else if (WIFormControlTypeConstants.TEST_STEPS_CONTROL.equals(controlType)) {
                return new TestStepsControl();
            } else if (WIFormControlTypeConstants.LABEL_CONTROL.equals(controlType)) {
                return new LinkLabelControl();
            } else if (WIFormControlTypeConstants.WEBPAGE_CONTROL.equals(controlType)) {
                return new WebPageControl();
            }

            final String messageFormat = Messages.getString("Helpers.UnsupportedCustomControlFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, controlType);
            return new ErrorBoxControl(message);
        }

        final String messageFormat = Messages.getString("Helpers.IllegalStateFormElementFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, (formElement == null ? "null" //$NON-NLS-1$
            : formElement.getClass().getName()));
        throw new IllegalStateException(message);
    }

    public static WIFormLayout getLayoutForForm(final WIFormDescription formDescription) {
        final WIFormLayout[] layouts = formDescription.getLayoutChildren();

        /*
         * common case: one layout with target unspecified or target =
         * "WinForms"
         */
        if (layouts.length == 1) {
            if (layouts[0].getTarget() == null || layouts[0].getTarget().equals(WINFORMS_LAYOUT_TARGET)) {
                return layouts[0];
            } else {
                final String messageFormat = Messages.getString("Helpers.FormLayoutUnrecognizedFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, layouts[0].getTarget());
                throw new FormBuildException(message);
            }
        }

        int teampriseLayoutIx = -1;
        int swtLayoutIx = -1;
        int winformsLayoutIx = -1;
        int unspecifiedTargetLayoutIx = -1;

        for (int i = 0; i < layouts.length; i++) {
            if (layouts[i].getTarget() == null) {
                unspecifiedTargetLayoutIx = i;
            } else if (layouts[i].getTarget().equals(SWT_LAYOUT_TARGET)) {
                swtLayoutIx = i;
            } else if (layouts[i].getTarget().equals(TEAMPRISE_LAYOUT_TARGET)) {
                // Kept for back-compat with Teamprise client.
                teampriseLayoutIx = i;
            } else if (layouts[i].getTarget().equals(WINFORMS_LAYOUT_TARGET)) {
                winformsLayoutIx = i;
            }
        }

        if (swtLayoutIx != -1) {
            return layouts[swtLayoutIx];
        }

        if (teampriseLayoutIx != -1) {
            return layouts[teampriseLayoutIx];
        }

        if (winformsLayoutIx != -1) {
            return layouts[winformsLayoutIx];
        }

        if (unspecifiedTargetLayoutIx != -1) {
            return layouts[unspecifiedTargetLayoutIx];
        }

        final String messageFormat = Messages.getString("Helpers.UnableToFindLayoutFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, layouts.length);
        throw new FormBuildException(message);
    }

}
