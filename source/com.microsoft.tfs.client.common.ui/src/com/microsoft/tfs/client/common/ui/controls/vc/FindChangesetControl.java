// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.controls.vc.history.HistoryCombinedControl;
import com.microsoft.tfs.client.common.ui.controls.vc.history.HistoryInput;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.util.Check;

public class FindChangesetControl extends BaseControl {
    private final TFSRepository repository;
    private FindChangesetOptionsControl optionsControl;
    private final HistoryCombinedControl historyControl;

    public FindChangesetControl(final Composite parent, final int style, final TFSRepository repository) {
        super(parent, style);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        this.repository = repository;

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        final Control optionsControl = createOptionsControl(this);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(optionsControl);

        final Label label = new Label(this, SWT.NONE);
        label.setText(Messages.getString("FindChangesetControl.ResultsLabelText")); //$NON-NLS-1$

        historyControl = new HistoryCombinedControl(this, SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().grab().fill().applyTo(historyControl);
    }

    public FindChangesetOptionsControl getOptionsControl() {
        return optionsControl;
    }

    public HistoryCombinedControl getHistoryControl() {
        return historyControl;
    }

    @Override
    public boolean setFocus() {
        return optionsControl.setFocus();
    }

    private Control createOptionsControl(final Composite parent) {
        final Group composite = new Group(parent, SWT.NONE);
        composite.setText(Messages.getString("FindChangesetControl.FindOptionsGroupText")); //$NON-NLS-1$

        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        optionsControl = new FindChangesetOptionsControl(composite, SWT.NONE, repository);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(optionsControl);

        final Button button = new Button(composite, SWT.NONE);
        button.setText(Messages.getString("FindChangesetControl.FindButtonText")); //$NON-NLS-1$
        ButtonHelper.setButtonToButtonBarSize(button);

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                find();
            }
        });

        return composite;
    }

    public void refresh() {
        find();
    }

    private void find() {
        HistoryInput input;

        try {
            input = optionsControl.getHistoryInput();
        } catch (final FindChangesetOptionsControl.ValidationException e) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                Messages.getString("FindChangesetControl.NoChangesetFoundTitle"), //$NON-NLS-1$
                e.getLocalizedMessage());
            return;
        }

        historyControl.setInput(input);
    }
}
