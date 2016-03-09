// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.util.EventObject;

public class CheckinSubControlEvent extends EventObject {
    public CheckinSubControlEvent(final AbstractCheckinSubControl subControl) {
        super(subControl);
    }

    public AbstractCheckinSubControl getControl() {
        return (AbstractCheckinSubControl) getSource();
    }

    public CheckinSubControlType getControlType() {
        return getControl().getSubControlType();
    }
}
