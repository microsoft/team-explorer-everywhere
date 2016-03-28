// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.feedback;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.FontHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.AbstractTextControlValidator;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.email.EmailAddressParser;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validity;

public class FeedbackDialog extends BaseDialog {

    private static final Log log = LogFactory.getLog(FeedbackDialog.class);

    // This is the same URI used by Visual Studio & IntellJ Send a Smile
    private static final URI URL_PRIVACY_POLICY = URIUtils.newURI("http://go.microsoft.com/fwlink/?LinkID=277167"); //$NON-NLS-1$

    // Right now the limit on properties is 1024 in AppInsights
    private static final int MAX_APP_INSIGHTS_PROPERTY_SIZE = 1024;

    private final boolean smile;

    private Text commentText;
    private Text emailText;
    private EmailAddressValidator validator;
    private Label emailErrorMessageLabel;
    private Font emailErrorMessageFont;

    private String commentString;
    private String emailString;

    public FeedbackDialog(final Shell parentShell, final boolean smile) {
        super(parentShell);
        this.smile = smile;
        setOptionResizable(true);

        // rename the label
        final String okLabel = smile ? Messages.getString("FeedbackDialog.OkButtonText.Smile") //$NON-NLS-1$
            : Messages.getString("FeedbackDialog.OkButtonText.Frown"); //$NON-NLS-1$

        setOptionIncludeDefaultButtons(false);
        addButtonDescription(IDialogConstants.OK_ID, okLabel, true);
        addButtonDescription(IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("FeedbackDialog.Title"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(Composite dialogArea) {
        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Label headerLabel = SWTUtil.createLabel(dialogArea, Messages.getString("FeedbackDialog.HeaderLabel"));//$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(headerLabel);

        final String messageString = smile ? Messages.getString("FeedbackDialog.CommentLabel.Smile") //$NON-NLS-1$
            : Messages.getString("FeedbackDialog.CommentLabel.Frown"); //$NON-NLS-1$

        final Label messageLabel = SWTUtil.createLabel(dialogArea, messageString);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(messageLabel);

        commentText = new Text(dialogArea, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        GridData commentTextGridData = GridDataBuilder.newInstance().hSpan(layout).wHint(
            getMinimumMessageAreaWidth()).hFill().hGrab().vFill().vGrab().getGridData();

        // show five lines of text
        final GC gc = new GC(commentText);
        final FontMetrics fm = gc.getFontMetrics();
        final int commentTextFontHeight = fm.getHeight();
        gc.dispose();
        commentTextGridData.heightHint = commentTextFontHeight * 5;

        commentText.setLayoutData(commentTextGridData);
        commentText.setTextLimit(MAX_APP_INSIGHTS_PROPERTY_SIZE);
        // prevent the text from grabbing tabs
        commentText.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            }
        });

        final Label emailLabel = SWTUtil.createLabel(dialogArea, Messages.getString("FeedbackDialog.EmailLabel"));//$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(emailLabel);

        emailText = new Text(dialogArea, SWT.SINGLE | SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(emailText);
        emailText.setTextLimit(MAX_APP_INSIGHTS_PROPERTY_SIZE);

        /*
         * Create a validator for the email textbox. Make sure to hook up the
         * text box modifier listeners (if any) after the validator so that the
         * validator gets run before the validation of all fields.
         */
        validator = new EmailAddressValidator(emailText);

        emailErrorMessageLabel = SWTUtil.createLabel(dialogArea, SWT.WRAP, StringUtil.EMPTY);
        emailErrorMessageFont =
            new Font(getShell().getDisplay(), FontHelper.italicize(emailErrorMessageLabel.getFont().getFontData()));
        emailErrorMessageLabel.setFont(emailErrorMessageFont);
        /*
         * Reserve three lines for the error messages. They could be pretty
         * wordy.
         */
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().hHint(commentTextFontHeight * 3).applyTo(
            emailErrorMessageLabel);

        emailErrorMessageLabel.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (emailErrorMessageFont != null) {
                    emailErrorMessageFont.dispose();
                }
            }
        });

        final Label thankYouLabel =
            SWTUtil.createLabel(dialogArea, SWT.WRAP, Messages.getString("FeedbackDialog.ThankYouLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).vIndent(getVerticalSpacing()).wHint(
            getMinimumMessageAreaWidth()).hFill().hGrab().applyTo(thankYouLabel);

        final CompatibilityLinkControl privacyLink = CompatibilityLinkFactory.createLink(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(privacyLink.getControl());
        privacyLink.setSimpleText(Messages.getString("FeedbackDialog.PrivacyLink")); //$NON-NLS-1$
        privacyLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                // Use external dialog since this is being launched from a
                // dialog, and therefore would be hidden by this dialog if
                // launched internally
                BrowserFacade.launchURL(URL_PRIVACY_POLICY, null, null, null, LaunchMode.EXTERNAL);
            }
        });
    }

    @Override
    protected void hookAfterButtonsCreated() {
        new ButtonValidatorBinding(getButton(IDialogConstants.OK_ID)).bind(validator);
    }

    @Override
    protected void okPressed() {
        // grab the values
        // If there are new lines or carriage returns the events do not go
        // through.
        // TODO -- move this lower in the stack?
        commentString = commentText.getText().replaceAll("\\n|\\r", " ");//$NON-NLS-1$ //$NON-NLS-2$
        emailString = emailText.getText() == null ? StringUtil.EMPTY : emailText.getText().trim();
        super.okPressed();
    }

    public String getComment() {
        return commentString;
    }

    public String getEmail() {
        return emailString;
    }

    public class EmailAddressValidator extends AbstractTextControlValidator {

        private final EmailAddressParser parser;

        public EmailAddressValidator(final Text subject) {
            super(subject);
            parser = new EmailAddressParser();
            validate();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IValidity computeValidity(String text) {
            if (emailErrorMessageLabel == null) {
                /*
                 * Not all controls are created yet.
                 */
                return Validity.VALID;
            }

            if (parser.parse(text)) {
                emailErrorMessageLabel.setText(StringUtil.EMPTY);
                return Validity.VALID;
            } else {
                final String erorMessage = parser.getErrorMessage();
                emailErrorMessageLabel.setText(erorMessage);
                return Validity.invalid(erorMessage);
            }
        }
    }
}
