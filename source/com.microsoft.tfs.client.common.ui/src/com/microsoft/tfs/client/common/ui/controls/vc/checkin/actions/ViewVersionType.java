// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin.actions;

import com.microsoft.tfs.client.common.ui.Messages;

public class ViewVersionType {
    public static final ViewVersionType DEFAULT =
        new ViewVersionType(
            Messages.getString("ViewVersionType.DefaultTypeText"), //$NON-NLS-1$
            Messages.getString("ViewVersionType.DefaultTypeTooltip")); //$NON-NLS-1$
    public static final ViewVersionType SHELVED =
        new ViewVersionType(
            Messages.getString("ViewVersionType.ShelvedTypeText"), //$NON-NLS-1$
            Messages.getString("ViewVersionType.ShelvedTypeTooltip")); //$NON-NLS-1$
    public static final ViewVersionType UNMODIFIED =
        new ViewVersionType(
            Messages.getString("ViewVersionType.UnmodifiedTypeText"), //$NON-NLS-1$
            Messages.getString("ViewVersionType.UnmodifiedTypeTooltip")); //$NON-NLS-1$
    public static final ViewVersionType LATEST =
        new ViewVersionType(
            Messages.getString("ViewVersionType.LatestTypeText"), //$NON-NLS-1$
            Messages.getString("ViewVersionType.LatestTypeTooltip")); //$NON-NLS-1$

    public static final ViewVersionType PREVIOUS =
        new ViewVersionType(
            Messages.getString("ViewVersionType.PreviousTypeText"), //$NON-NLS-1$
            Messages.getString("ViewVersionType.PreviousTypeTooltip")); //$NON-NLS-1$

    private final String text;
    private final String tooltip;

    private ViewVersionType(final String text, final String tooltip) {
        this.text = text;
        this.tooltip = tooltip;
    }

    public String getText() {
        return text;
    }

    public String getTooltipText() {
        return tooltip;
    }
}
