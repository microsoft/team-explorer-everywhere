// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.form.WIFormControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormCustomControlOptions;
import com.microsoft.tfs.core.clients.workitem.form.WIFormDockEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLabelPositionEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLabelText;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLink;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLinksControlOptions;
import com.microsoft.tfs.core.clients.workitem.form.WIFormPaddingAttribute;
import com.microsoft.tfs.core.clients.workitem.form.WIFormReadOnlyEnum;
import com.microsoft.tfs.core.clients.workitem.form.WIFormWebPageControlOptions;

public class WIFormControlImpl extends WIFormElementImpl implements WIFormControl {
    private String fieldName;
    private String type;
    private String preferredType;
    private String label;
    private WIFormLabelPositionEnum labelPosition;
    private WIFormDockEnum dock;
    private WIFormPaddingAttributeImpl padding;
    private WIFormPaddingAttributeImpl margin;
    private WIFormReadOnlyEnum readOnly;
    private WIFormWebPageControlOptions webPageControlOptions;
    private WIFormLinksControlOptions linksControlOptions;
    private WIFormCustomControlOptions customControlOptions;
    private WIFormLink link;
    private WIFormLabelText labelText;
    private Integer height;

    /**
     * Process the attributes of the "Control" element.
     *
     * Attributes: - FieldName: optional - Type: required - Label: optional -
     * LabelPosition: optional - Dock: optional - Padding: optional - Margin:
     * optional - ReadOnly: optional - Height: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        fieldName = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_FIELD_NAME);
        type = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_TYPE);
        preferredType =
            WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_PREFERRED_TYPE);
        label = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_LABEL);
        labelPosition = WIFormLabelPositionEnumFactory.fromType(
            attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_LABEL_POSITION));
        dock = WIFormDockEnumFactory.fromType(attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_DOCK));
        padding = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_PADDING);
        margin = WIFormParseHandler.readPaddingAttribute(attributes, WIFormParseConstants.ATTRIBUTE_NAME_MARGIN);
        readOnly =
            WIFormReadOnlyEnumFactory.fromType(attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_READ_ONLY));
        height = WIFormParseHandler.readIntegerValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_HEIGHT);
        setAttributes(attributes);
    }

    /**
     * Process the child elements of the "Control" element.
     *
     * Child elements: (choice) - Link (minimum=0, maximum=1) - LabelText
     * (minimum=0, maximum=1) Child elements: (choice) - CustomControlOptions
     * (minimum=0, maximum=1) - LinksControlOptions (minimum=0, maximum=1) -
     * WebpageControlOptions (minimum=0, maximum=1)
     */
    @Override
    void endLoading() {
        final WIFormElement[] children = getChildElements();
        for (int i = 0; i < children.length; i++) {
            final WIFormElement child = children[i];
            if (child instanceof WIFormWebPageControlOptions) {
                webPageControlOptions = (WIFormWebPageControlOptions) child;
            } else if (child instanceof WIFormLinksControlOptions) {
                linksControlOptions = (WIFormLinksControlOptions) child;
            } else if (child instanceof WIFormCustomControlOptions) {
                customControlOptions = (WIFormCustomControlOptions) child;
            } else if (child instanceof WIFormLink) {
                link = (WIFormLink) child;
            } else if (child instanceof WIFormLabelText) {
                labelText = (WIFormLabelText) child;
            }
        }
    }

    /**
     * Corresponds to the "FieldName" attribute in XML use: optional
     */
    @Override
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Corresponds to the "Type" attribute in XML use: required
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Corresponds to the "PreferredType" attribute in XML use: optional (if
     * specified, a control of the PreferredType should be attempted to load. If
     * the control type could not be found or loaded, then "Type" is used as a
     * fallback)
     */
    @Override
    public String getPreferredType() {
        return preferredType;
    }

    /**
     * Corresponds to the "Label" attribute in XML use: optional
     */
    @Override
    public String getLabel() {
        return label;
    }

    /**
     * Corresponds to the "LabelPosition" attribute in XML use:
     * ${simpleTypeReference.use}
     */
    @Override
    public WIFormLabelPositionEnum getLabelPosition() {
        return labelPosition;
    }

    /**
     * Corresponds to the "Dock" attribute in XML use: optional
     */
    @Override
    public WIFormDockEnum getDock() {
        return dock;
    }

    /**
     * Corresponds to the "Padding" attribute in XML use: optional
     */
    @Override
    public WIFormPaddingAttribute getPadding() {
        return padding;
    }

    /**
     * Corresponds to the "Margin" attribute in XML use: optional
     */
    @Override
    public WIFormPaddingAttribute getMargin() {
        return margin;
    }

    /**
     * Corresponds to the "ReadOnly" attribute in XML use: optional
     */
    @Override
    public WIFormReadOnlyEnum getReadOnly() {
        return readOnly;
    }

    /**
     * Corresponds to the "Height" attribute in XML. A value of null indicates
     * the attribute did not appear in the XML and should default. use: optional
     */
    @Override
    public Integer getHeight() {
        return height;
    }

    /**
     * Corresponds to the "WebPageControlOption" child element in XML use:
     * optional
     */
    @Override
    public WIFormWebPageControlOptions getWebPageControlOptions() {
        return webPageControlOptions;
    }

    /**
     * Corresponds to the "LinksControlOption" child element in XML use:
     * optional
     */
    @Override
    public WIFormLinksControlOptions getLinksControlOptions() {
        return linksControlOptions;
    }

    /**
     * Corresponds to the "CustomControlOption" child element in XML use:
     * optional
     */
    @Override
    public WIFormCustomControlOptions getCustomControlOptions() {
        return customControlOptions;
    }

    /**
     * Corresponds to the "LabelText" child element in XML use: optional
     */
    @Override
    public WIFormLabelText getLabelText() {
        return labelText;
    }

    /**
     * Corresponds to the "Link" child element in XML use: optional
     *
     * @return
     */
    @Override
    public WIFormLink getLink() {
        return link;
    }
}
