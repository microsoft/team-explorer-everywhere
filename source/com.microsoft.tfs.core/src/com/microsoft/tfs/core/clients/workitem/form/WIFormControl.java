// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.form;

/**
 * WIFormControl - represents the "ControlType" complex type.
 *
 * @since TEE-SDK-10.1
 */
public interface WIFormControl extends WIFormElement {

    /**
     * Corresponds to the "FieldName" attribute in XML use: optional
     */
    public String getFieldName();

    /**
     * Corresponds to the "Type" attribute in XML use: required
     */
    public String getType();

    /**
     * Corresponds to the "PreferredType" attribute in XML use: optional (if
     * specified, a control of the PreferredType should be attempted to load. If
     * the control type could not be found or loaded, then "Type" is used as a
     * fallback)
     */
    public String getPreferredType();

    /**
     * Corresponds to the "Label" attribute in XML use: optional (mutually
     * exclusive with label child content)
     */
    public String getLabel();

    /**
     * Corresponds to the "LabelPosition" attribute in XML use:
     * ${simpleTypeReference.use}
     */
    public WIFormLabelPositionEnum getLabelPosition();

    /**
     * Corresponds to the "Dock" attribute in XML use: optional
     */
    public WIFormDockEnum getDock();

    /**
     * Corresponds to the "Padding" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getPadding();

    /**
     * Corresponds to the "Margin" attribute in XML use: optional
     */
    public WIFormPaddingAttribute getMargin();

    /**
     * Corresponds to the "ReadOnly" attribute in XML use: optional
     */
    public WIFormReadOnlyEnum getReadOnly();

    /**
     * Corresponds to the "Height" attribute in XML. A value of null indicates
     * the attribute was not specified. use: optional
     */
    public Integer getHeight();

    /**
     * Corresponds to the "WebPageOptionsControl" child element in XML use:
     * optional
     */
    public WIFormWebPageControlOptions getWebPageControlOptions();

    /**
     * Corresponds to the "LinksControlOptions" child element in XML use:
     * optional
     */
    public WIFormLinksControlOptions getLinksControlOptions();

    /**
     * Corresponds to the "CustomControlOptions" child element in XML use:
     * optional
     */
    public WIFormCustomControlOptions getCustomControlOptions();

    /**
     * Corresponds to the "Link" child element in XML use: optional
     */
    public WIFormLink getLink();

    /**
     * Corresponds to the "LabelText" child element in XML use: optional
     */
    public WIFormLabelText getLabelText();
}
