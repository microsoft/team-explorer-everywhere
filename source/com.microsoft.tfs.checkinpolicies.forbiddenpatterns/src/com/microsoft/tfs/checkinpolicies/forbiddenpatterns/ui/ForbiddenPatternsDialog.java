// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.forbiddenpatterns.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.checkinpolicies.forbiddenpatterns.ForbiddenPatternsPolicy;
import com.microsoft.tfs.checkinpolicies.forbiddenpatterns.Messages;
import com.microsoft.tfs.client.common.ui.controls.RegularExpressionTable;
import com.microsoft.tfs.client.common.ui.controls.RegularExpressionTableData;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.AbstractTextControlValidator;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.ErrorLabel;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validity;

/**
 * Edits the forbidden patterns.
 */
public class ForbiddenPatternsDialog extends BaseDialog {
    private final String teamProject;

    private final List regularExpressionTableData = new ArrayList();

    private ErrorLabel newExpressionErrorLabel;
    private Text newExpressionText;
    private Button addButton;
    private Button deleteButton;
    private RegularExpressionTable expressionTable;

    public ForbiddenPatternsDialog(final Shell parentShell, final String teamProject, final String[] expressions) {
        super(parentShell);

        Check.notNull(teamProject, "teamProject"); //$NON-NLS-1$
        Check.notNull(expressions, "expressions"); //$NON-NLS-1$

        this.teamProject = teamProject;
        setExpressions(expressions);
    }

    public void setExpressions(final String[] expressions) {
        Check.notNull(expressions, "expressions"); //$NON-NLS-1$

        this.regularExpressionTableData.clear();

        for (int i = 0; i < expressions.length; i++) {
            this.regularExpressionTableData.add(new RegularExpressionTableData(expressions[i]));
        }

        refreshTable();
    }

    public String[] getExpressions() {
        final RegularExpressionTableData[] expressions = expressionTable.getExpressions();
        final String[] ret = new String[expressions.length];

        for (int i = 0; i < expressions.length; i++) {
            ret[i] = expressions[i].getExpression();
        }

        return ret;
    }

    private void refreshTable() {
        if (this.expressionTable != null) {
            expressionTable.setExpressions(
                (RegularExpressionTableData[]) this.regularExpressionTableData.toArray(
                    new RegularExpressionTableData[this.regularExpressionTableData.size()]));
        }
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        SWTUtil.gridLayout(dialogArea, 3);

        /*
         * Create the label, text, and button at top.
         */
        final Label newExpressionLabel =
            SWTUtil.createLabel(dialogArea, Messages.getString("ForbiddenPatternsDialog.NewExpressionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hSpan(3).applyTo(newExpressionLabel);

        this.newExpressionErrorLabel = new ErrorLabel(dialogArea, SWT.NONE);

        this.newExpressionText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(this.newExpressionText);
        this.newExpressionText.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;

                    if (ForbiddenPatternsDialog.this.addButton.isEnabled()) {
                        addClicked();
                    }
                }
            }
        });

        this.addButton = SWTUtil.createButton(dialogArea, Messages.getString("ForbiddenPatternsDialog.AddButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(addButton);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addClicked();
            }
        });

        /*
         * Create the table.
         */
        this.expressionTable = new RegularExpressionTable(
            dialogArea,
            SWT.MULTI | SWT.FULL_SELECTION,
            ForbiddenPatternsPolicy.EXPRESSION_FLAGS,
            "forbidden-patterns-dialog"); //$NON-NLS-1$
        GridDataBuilder.newInstance().grab().fill().hSpan(2).applyTo(this.expressionTable);

        this.deleteButton =
            SWTUtil.createButton(dialogArea, Messages.getString("ForbiddenPatternsDialog.DeleteButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(deleteButton);
        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                deleteClicked();
            }
        });

        /*
         * Use slightly different validators for the error label and the "add"
         * button.
         */
        final String errorMessage = Messages.getString("ForbiddenPatternsDialog.TextIsNotValidRegEx"); //$NON-NLS-1$

        newExpressionErrorLabel.getValidatorBinding().bind(
            new ExprTextELblValidator(newExpressionText, this.expressionTable, errorMessage));

        new ButtonValidatorBinding(addButton).bind(
            new ExprTextFBtnVldtr(newExpressionText, this.expressionTable, errorMessage));

        new ButtonValidatorBinding(deleteButton).bind(new SelectionProviderValidator(this.expressionTable));

        refreshTable();
    }

    private void deleteClicked() {
        final RegularExpressionTableData[] selectedExpressions = this.expressionTable.getSelectedExpressions();
        regularExpressionTableData.removeAll(Arrays.asList(selectedExpressions));
        refreshTable();
    }

    private void addClicked() {
        this.regularExpressionTableData.add(new RegularExpressionTableData(this.newExpressionText.getText()));
        refreshTable();

        this.newExpressionText.setText(""); //$NON-NLS-1$
    }

    @Override
    protected String provideDialogTitle() {
        final String messageFormat = Messages.getString("ForbiddenPatternsDialog.DialogTitleFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, this.teamProject);
        return message;
    }

    /**
     * Should be called ExpressionTextFormatErrorLabelValidator but given brief
     * name to keep class names short during build (Windows path length
     * work-around).
     */
    private static class ExprTextELblValidator extends ExprTextFBtnVldtr {

        public ExprTextELblValidator(
            final Text subject,
            final RegularExpressionTable table,
            final String errorMessage) {
            super(subject, table, errorMessage);
        }

        @Override
        protected IValidity computeValidity(final String text) {
            /*
             * The error label doesn't show if there's empty text.
             */
            if (text.length() == 0) {
                return Validity.VALID;
            }

            return super.computeValidity(text);
        }
    }

    /**
     * Should be called ExpressionTextFormatButtonValidator but given brief name
     * to keep class names short during build (Windows path length work-around).
     */
    private static class ExprTextFBtnVldtr extends AbstractTextControlValidator {
        private final RegularExpressionTable table;
        private final String errorMessage;

        public ExprTextFBtnVldtr(final Text subject, final RegularExpressionTable table, final String errorMessage) {
            super(subject);

            Check.notNull(table, "table"); //$NON-NLS-1$
            this.table = table;
            this.errorMessage = errorMessage;

            validate();
        }

        @Override
        protected IValidity computeValidity(final String text) {
            return table.isValidRegularExpression(text) ? Validity.VALID : Validity.invalid(errorMessage);
        }
    }
}
