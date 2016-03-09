// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;

public class FormGroupData {
    private final int width;
    private final boolean widthPercentage;

    public FormGroupData(final int width, final boolean widthPercentage) {
        this.width = width;
        this.widthPercentage = widthPercentage;
    }

    @Override
    public String toString() {
        final String messageFormat = "{0} (percentage={1})"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, Integer.toString(width), widthPercentage);
        return message;
    }

    public int getWidth() {
        return width;
    }

    public boolean isWidthPercentage() {
        return widthPercentage;
    }
}
