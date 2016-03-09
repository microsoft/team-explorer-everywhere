// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.RegularExpressionTable;
import com.microsoft.tfs.client.common.ui.controls.RegularExpressionTableData;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.validation.AbstractTextControlValidator;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.ErrorLabel;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementListener;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.pendingcheckin.filters.ScopeFilter;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validity;

public class PolicyScopeControl extends Composite {
    private final Color RESULT_NEUTRAL_COLOR = getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);
    private final Color RESULT_NOT_EVALUATE_COLOR = new Color(getDisplay(), 255, 220, 210);
    private final Color RESULT_WILL_EVALUATE_COLOR = new Color(getDisplay(), 220, 255, 220);

    private final List expressions = new ArrayList();

    private final ErrorLabel newExpressionErrorLabel;
    private final Text newExpressionText;
    private final Text testText;
    private final RegularExpressionTable expressionTable;
    private final Text resultText;
    private final Button addButton;

    public PolicyScopeControl(final Composite parent, final int style) {
        super(parent, style);

        SWTUtil.gridLayout(this, 3);

        final Label newExpressionLabel =
            SWTUtil.createLabel(this, Messages.getString("PolicyScopeControl.NewExpressionLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hSpan(3).applyTo(newExpressionLabel);

        newExpressionErrorLabel = new ErrorLabel(this, SWT.NONE);

        newExpressionText = new Text(this, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(newExpressionText);
        newExpressionText.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;

                    if (addButton.isEnabled()) {
                        addClicked();
                    }
                }
            }
        });

        addButton = SWTUtil.createButton(this, Messages.getString("PolicyScopeControl.AddButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(addButton);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addClicked();
            }
        });

        final Composite configuredComposite =
            SWTUtil.createGroup(this, Messages.getString("PolicyScopeControl.ExpressionGroupText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().grab().hSpan(3).applyTo(configuredComposite);

        SWTUtil.gridLayout(configuredComposite, 2);
        ((GridLayout) configuredComposite.getLayout()).marginHeight = 10;
        ((GridLayout) configuredComposite.getLayout()).marginWidth = 10;

        expressionTable = new RegularExpressionTable(
            configuredComposite,
            SWT.MULTI | SWT.FULL_SELECTION,
            ScopeFilter.EXPRESSION_FLAGS);
        GridDataBuilder.newInstance().grab().fill().applyTo(expressionTable);
        ControlSize.setCharHeightHint(expressionTable, 6);
        expressionTable.addElementListener(new ElementListener() {
            @Override
            public void elementsChanged(final ElementEvent event) {
                updateTestResult();
            }
        });

        final Button deleteButton =
            SWTUtil.createButton(configuredComposite, Messages.getString("PolicyScopeControl.DeleteButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(deleteButton);
        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                deleteClicked();
            }
        });

        final Label testLabel =
            SWTUtil.createLabel(configuredComposite, Messages.getString("PolicyScopeControl.testLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(testLabel);

        testText = new Text(configuredComposite, SWT.BORDER | SWT.SINGLE);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(testText);
        testText.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                    e.detail = SWT.TRAVERSE_NONE;
                }
            }
        });

        testText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updateTestResult();
            }
        });

        SWTUtil.createLabel(configuredComposite, Messages.getString("PolicyScopeControl.ResultLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(testLabel);

        resultText = new Text(configuredComposite, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.WRAP);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(resultText);
        ControlSize.setCharHeightHint(resultText, 3);

        /*
         * Use slightly different validators for the error label and the "add"
         * button.
         */
        final String errorMessage = Messages.getString("PolicyScopeControl.NotValidRegex"); //$NON-NLS-1$

        newExpressionErrorLabel.getValidatorBinding().bind(
            new ExprTextELblValidator(newExpressionText, expressionTable, errorMessage));

        new ButtonValidatorBinding(addButton).bind(
            new ExprTextFBtnVldtr(newExpressionText, expressionTable, errorMessage));

        new ButtonValidatorBinding(deleteButton).bind(new SelectionProviderValidator(expressionTable));

        // Prime our test validation.
        updateTestResult();
    }

    private void updateTestResult() {
        final String textValue = testText.getText();

        if (textValue.length() == 0) {
            resultText.setText(""); //$NON-NLS-1$
            resultText.setBackground(RESULT_NEUTRAL_COLOR);
            return;
        }

        if (ServerPath.isServerPath(textValue) == false) {
            resultText.setText(Messages.getString("PolicyScopeControl.NotAFullServerPath")); //$NON-NLS-1$
            resultText.setBackground(RESULT_NEUTRAL_COLOR);
            return;
        }

        if (ServerPath.isWildcard(textValue)) {
            resultText.setText(Messages.getString("PolicyScopeControl.WildcardCharsInPath")); //$NON-NLS-1$
            resultText.setBackground(RESULT_NEUTRAL_COLOR);
            return;
        }

        try {
            ServerPath.canonicalize(textValue);
        } catch (final ServerPathFormatException e) {
            resultText.setText(Messages.getString("PolicyScopeControl.InvalidCharsInPath")); //$NON-NLS-1$
            resultText.setBackground(RESULT_NEUTRAL_COLOR);
            return;
        }

        final RegularExpressionTableData[] expressions = getScopeExpressions();

        if (expressions.length == 0) {
            final String messageFormat = Messages.getString("PolicyScopeControl.NoExpressionsConfiguredFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, textValue);

            resultText.setText(message);
            resultText.setBackground(RESULT_WILL_EVALUATE_COLOR);
            return;
        }

        final String[] expressionStrings = new String[expressions.length];

        for (int i = 0; i < expressions.length; i++) {
            expressionStrings[i] = expressions[i].getExpression();
        }

        final ScopeFilter filter = new ScopeFilter(expressionStrings);

        final int matchedExpression = filter.passesWhich(textValue);

        /*
         * A result of Integer.MIN_VALUE means there were no configured
         * expressions, but we test for that above, so we ignore it.
         */
        if (matchedExpression == -1) {
            final String messageFormat = Messages.getString("PolicyScopeControl.PoliciesWillNotBeEvaluatedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, textValue);

            resultText.setText(message);
            resultText.setBackground(RESULT_NOT_EVALUATE_COLOR);
            return;
        }

        final String messageFormat = Messages.getString("PolicyScopeControl.PoliciesWillBeEvaludatedFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, textValue, expressionStrings[matchedExpression]);

        resultText.setText(message);
        resultText.setBackground(RESULT_WILL_EVALUATE_COLOR);
    }

    public void setScopeExpressions(final RegularExpressionTableData[] newExpressions) {
        Check.notNull(newExpressions, "newExpressions"); //$NON-NLS-1$

        expressions.clear();
        expressions.addAll(Arrays.asList(newExpressions));
        refreshTable();
    }

    public RegularExpressionTableData[] getScopeExpressions() {
        return (RegularExpressionTableData[]) expressions.toArray(new RegularExpressionTableData[expressions.size()]);
    }

    private void deleteClicked() {
        final RegularExpressionTableData[] selectedExpressions = expressionTable.getSelectedExpressions();
        expressions.removeAll(Arrays.asList(selectedExpressions));
        refreshTable();
    }

    private void addClicked() {
        expressions.add(new RegularExpressionTableData(newExpressionText.getText()));
        refreshTable();

        newExpressionText.setText(""); //$NON-NLS-1$
    }

    private void refreshTable() {
        expressionTable.setExpressions(getScopeExpressions());
    }

    /**
     * Should be called ExpressionTextFormatErrorLabelValidator but given brief
     * name to keep class names short during build (Windows path length
     * work-around).
     */
    private static class ExprTextELblValidator extends ExprTextFBtnVldtr {

        public ExprTextELblValidator(
            final Text newExpressionText,
            final RegularExpressionTable table,
            final String errorMessage) {
            super(newExpressionText, table, errorMessage);
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
