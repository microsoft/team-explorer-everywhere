// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.dialogs.vc;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.CheckboxProviderValidator;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.eclipse.tpignore.TPIgnoreDocument;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.controls.TPIgnorePatternTable;
import com.microsoft.tfs.util.Check;

public class TPIgnorePatternsResolutionDialog extends BaseDialog {
    private final Map<Pattern, Set<IResource>> patternsToMatchedResources;

    private TPIgnorePatternTable table;
    private Label matchingResourcesLabel;
    private Pattern[] checkedPatterns;

    public TPIgnorePatternsResolutionDialog(
        final Shell shell,
        final Map<Pattern, Set<IResource>> patternsToMatchedResources) {
        super(shell);

        Check.notNull(patternsToMatchedResources, "patternsToMatchedResources"); //$NON-NLS-1$

        this.patternsToMatchedResources = patternsToMatchedResources;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("TPIgnorePatternsResolutionDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout(1, false);
        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginHeight = getVerticalMargin();
        dialogLayout.horizontalSpacing = getHorizontalSpacing();
        dialogLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(dialogLayout);

        final Label descriptionLabel = new Label(dialogArea, SWT.WRAP);
        descriptionLabel.setText(
            MessageFormat.format(
                Messages.getString("TPIgnorePatternsResolutionDialog.DescriptionLabelTextFormat"), //$NON-NLS-1$
                TPIgnoreDocument.DEFAULT_FILENAME));
        GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(descriptionLabel);

        table = new TPIgnorePatternTable(dialogArea, SWT.SINGLE | SWT.CHECK);
        GridDataBuilder.newInstance().grab().fill().hCHint(table, 12).applyTo(table);

        matchingResourcesLabel = new Label(dialogArea, SWT.WRAP);
        matchingResourcesLabel.setText(
            Messages.getString("TPIgnorePatternsResolutionDialog.MatchingResourcesLabelDefaultText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(
            matchingResourcesLabel);

        final Text text = new Text(dialogArea, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).hCHint(text, 6).applyTo(text);
        text.setEditable(false);

        table.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                if (table.getSelectedElement() == null) {
                    matchingResourcesLabel.setText(
                        Messages.getString("TPIgnorePatternsResolutionDialog.MatchingResourcesLabelDefaultText")); //$NON-NLS-1$
                    text.setText(""); //$NON-NLS-1$
                } else {
                    matchingResourcesLabel.setText(
                        MessageFormat.format(
                            Messages.getString("TPIgnorePatternsResolutionDialog.MatchingResourcesLabelFormat"), //$NON-NLS-1$
                            table.getSelectedPattern().pattern()));

                    /*
                     * A cheesy sort by path because Eclipse offers no public
                     * IResource comparator as of 3.6.
                     */
                    final Set<IResource> sortedResources = new TreeSet<IResource>(new Comparator<IResource>() {
                        @Override
                        public int compare(final IResource o1, final IResource o2) {
                            if (o1.getLocation() != null && o2.getLocation() == null) {
                                return 1;
                            }

                            if (o1.getLocation() == null && o2.getLocation() != null) {
                                return -1;
                            }

                            return o1.getLocation().toString().compareTo(o2.getLocation().toString());
                        }
                    });

                    sortedResources.addAll(patternsToMatchedResources.get(table.getSelectedElement()));

                    final StringBuilder sb = new StringBuilder();
                    for (final IResource resource : sortedResources) {
                        sb.append(resource.getFullPath());
                        sb.append("\n"); //$NON-NLS-1$
                    }
                    text.setText(sb.toString());
                }

                /*
                 * Required because labels may change size after formatting
                 * strings.
                 */
                dialogArea.layout();
            }
        });

        final Pattern[] elements =
            patternsToMatchedResources.keySet().toArray(new Pattern[patternsToMatchedResources.keySet().size()]);

        table.setPatterns(elements);
        table.setCheckedPatterns(elements);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);

        button.setText(Messages.getString("TPIgnorePatternsResolutionDialog.RemoveButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);

        new ButtonValidatorBinding(button).bind(new CheckboxProviderValidator(
            table,
            NumericConstraint.ONE_OR_MORE,
            Messages.getString("TPIgnorePatternsResolutionDialog.AtLeastOnePatternMustBeChecked"))); //$NON-NLS-1$
    }

    @Override
    protected void hookDialogAboutToClose() {
        checkedPatterns = table.getCheckedPattern();
    }

    public Pattern[] getCheckedPatterns() {
        return checkedPatterns;
    }
}
