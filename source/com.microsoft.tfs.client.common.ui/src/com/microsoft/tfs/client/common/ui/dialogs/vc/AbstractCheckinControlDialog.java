// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.AbstractCheckinSubControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinControl;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlEvent;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlListener;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.CheckinSubControlTitleChangedListener;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;

public abstract class AbstractCheckinControlDialog extends BaseDialog {
    private CheckinControl checkinControl;
    private final CheckinSubControlTitleChangedListener titleListener;

    protected abstract String getBaseTitle();

    public AbstractCheckinControlDialog(final Shell parentShell) {
        super(parentShell);

        titleListener = new CheckinSubControlTitleChangedListener() {
            @Override
            public void onTitleChanged(final CheckinSubControlEvent event) {
                AbstractCheckinControlDialog.this.onTitleChanged();
            }
        };
    }

    protected final void setCheckinControl(final CheckinControl checkinControl) {
        setCheckinControl(checkinControl, true);
    }

    protected final CheckinControl getCheckinControl() {
        return checkinControl;
    }

    protected final void setCheckinControl(final CheckinControl checkinControl, final boolean listenForTitleChanges) {
        this.checkinControl = checkinControl;

        if (!listenForTitleChanges) {
            return;
        }

        checkinControl.addSubControlListener(new CheckinSubControlListener() {
            @Override
            public void onSubControlHidden(final CheckinSubControlEvent event) {
                event.getControl().removeTitleChangedListener(titleListener);
                onTitleChanged();
            }

            @Override
            public void onSubControlVisible(final CheckinSubControlEvent event) {
                event.getControl().addTitleChangedListener(titleListener);
                onTitleChanged();
            }
        });

        final AbstractCheckinSubControl subControl = checkinControl.getVisibleSubControl();
        if (subControl != null) {
            subControl.addTitleChangedListener(titleListener);
        }

        onTitleChanged();
    }

    @Override
    protected final String provideDialogTitle() {
        return computeTitle();
    }

    protected void onTitleChanged() {
        if (getShell() == null) {
            return;
        }

        final String title = computeTitle();
        getShell().setText(title);
    }

    protected String computeTitle() {
        if (checkinControl == null || checkinControl.getVisibleSubControl() == null) {
            return getBaseTitle();
        }

        final String messageFormat = Messages.getString("AbstractCheckinControlDialog.SubControlTitleFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, getBaseTitle(), checkinControl.getVisibleSubControl().getTitle());
    }
}
