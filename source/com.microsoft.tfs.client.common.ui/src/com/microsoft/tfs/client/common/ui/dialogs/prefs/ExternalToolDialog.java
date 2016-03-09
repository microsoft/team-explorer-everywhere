// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.prefs;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.FormHelper;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.ExternalToolAssociation;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.externaltools.WindowsStyleArgumentTokenizer;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolValidator;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

public abstract class ExternalToolDialog extends BaseDialog {
    public static final String EXTENSION_TEXT_ID = "ExternalToolDialog.extensionText"; //$NON-NLS-1$
    public static final String COMMAND_TEXT_ID = "ExternalToolDialog.commandText"; //$NON-NLS-1$

    private final ExternalToolAssociation existingAssociation;
    private final boolean isNew;

    /*
     * Optional field for other associations, these will be checked against the
     * user-entered association to prevent duplicate file extensions from being
     * added.
     */
    private final ExternalToolAssociation[] otherAssociations;

    private ExternalToolAssociation newAssociation;

    private Text extensionText;
    private Text commandText;
    private Button commandBrowseButton;
    private Font helpFont;

    /**
     * Creates a {@link ExternalToolDialog}.
     *
     * @param parentShell
     *        the parent shell (not null)
     * @param association
     *        the association to edit (not null)
     * @param isNew
     *        true if this is for a new item (dialog will use a different
     *        title), false if it is for an existing item
     */
    public ExternalToolDialog(
        final Shell parentShell,
        final ExternalToolAssociation association,
        final boolean isNew,
        final ExternalToolAssociation[] otherAssociations) {
        super(parentShell);

        setOptionResizable(false);

        Check.notNull(association, "association"); //$NON-NLS-1$
        existingAssociation = association;
        this.isNew = isNew;

        this.otherAssociations = otherAssociations != null ? otherAssociations : new ExternalToolAssociation[0];
    }

    protected abstract String getExternalToolType();

    /**
     * @return true if {@link #extensionText} is the
     *         {@link ExternalToolset#DIRECTORY_EXTENSION}, false if it is not
     *         (including null)
     */
    private boolean hasDirectoryExtension() {
        return existingAssociation.containsExtension(ExternalToolset.DIRECTORY_EXTENSION);
    }

    /**
     * Combines the given extensions strings into a single string, extensions
     * separated by a comma.
     *
     * @param extensions
     *        the extensions to make into a nice string (not null)
     * @return the combined extensions string, separated by commas
     */
    public static String combineExtensions(final String[] extensions) {
        Check.notNull(extensions, "extensions"); //$NON-NLS-1$

        final StringBuffer ret = new StringBuffer();
        for (int i = 0; i < extensions.length; i++) {
            if (i > 0) {
                ret.append(", "); //$NON-NLS-1$
            }

            ret.append(extensions[i]);
        }

        return ret.toString();
    }

    /**
     * Splits the given extension string on whitespace and commas into
     * individual (trimmed) extensions.
     *
     * @param extensions
     *        the extension string (not null)
     * @return the individual extensions, trimmed
     */
    public static String[] splitExtensions(final String extensions) {
        Check.notNull(extensions, "extensions"); //$NON-NLS-1$

        final String[] strings = extensions.replaceAll(",", " ").split("\\s+"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        final List<String> ret = new ArrayList<String>();

        for (int i = 0; i < strings.length; i++) {
            strings[i] = strings[i].trim();

            if (strings[i].length() > 0) {
                ret.add(strings[i]);
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    @Override
    protected String provideDialogTitle() {
        String fileType = null;

        if (isNew) {
            return MessageFormat.format(
                Messages.getString("ExternalToolDialog.AddExternalFormat"), //$NON-NLS-1$
                getExternalToolType());
        }

        if (hasDirectoryExtension()) {
            fileType = Messages.getString("ExternalToolDialog.Directories"); //$NON-NLS-1$
        } else {
            fileType = MessageFormat.format(
                Messages.getString("ExternalToolDialog.ExtentionFilesFormat"), //$NON-NLS-1$
                combineExtensions(existingAssociation.getExtensions()));
        }

        return MessageFormat.format(
            Messages.getString("ExternalToolDialog.EditExternalFormat"), //$NON-NLS-1$
            getExternalToolType(),
            fileType);
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        final Composite container = (Composite) super.createDialogArea(parent);

        final FormLayout containerLayout = new FormLayout();
        containerLayout.spacing = getSpacing();
        containerLayout.marginWidth = getHorizontalMargin();
        containerLayout.marginHeight = getVerticalMargin();
        container.setLayout(containerLayout);

        extensionText = new Text(container, SWT.BORDER);
        AutomationIDHelper.setWidgetID(extensionText, EXTENSION_TEXT_ID);

        final Label extensionPromptLabel = new Label(container, SWT.NONE);
        final Label commandPromptLabel = new Label(container, SWT.NONE);

        commandText = new Text(container, SWT.BORDER);
        AutomationIDHelper.setWidgetID(commandText, COMMAND_TEXT_ID);

        commandBrowseButton = new Button(container, SWT.NONE);

        if (hasDirectoryExtension()) {
            extensionText.setText(Messages.getString("ExternalToolDialog.AllDirectories")); //$NON-NLS-1$
            extensionText.setEditable(false);
            extensionText.setEnabled(false);
        } else {
            /*
             * If this is a duplicate of an existing association
             * (existingAssociation has data, but isNew is true) then do not
             * display the extensions of the tool being duplicated.
             */
            if (isNew) {
                extensionText.setText(""); //$NON-NLS-1$
            } else {
                extensionText.setText(combineExtensions(existingAssociation.getExtensions()));
            }
        }

        final FormData extensionPromptData = new FormData();
        extensionPromptData.top =
            new FormAttachment(extensionText, FormHelper.VerticalOffset(extensionPromptLabel, extensionText), SWT.TOP);
        extensionPromptData.left = new FormAttachment(0, 0);
        extensionPromptLabel.setLayoutData(extensionPromptData);
        extensionPromptLabel.setText(Messages.getString("ExternalToolDialog.ExtenstionLabelText")); //$NON-NLS-1$

        final FormData extensionWidgetData = new FormData();
        extensionWidgetData.top = new FormAttachment(0, 0);
        extensionWidgetData.left = new FormAttachment(extensionPromptLabel, 0, SWT.RIGHT);
        extensionText.setLayoutData(extensionWidgetData);

        final Label extensionHelpLabel = new Label(container, SWT.NONE);

        final FontData fd = extensionHelpLabel.getFont().getFontData()[0];

        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            fd.setHeight(fd.getHeight() - 2);
        }
        fd.setStyle(SWT.ITALIC);

        helpFont = new Font(getShell().getDisplay(), fd);
        extensionHelpLabel.setFont(helpFont);

        final FormData extensionHelpData = new FormData();
        extensionHelpData.top =
            new FormAttachment(extensionText, FormHelper.VerticalOffset(extensionHelpLabel, extensionText), SWT.TOP);
        extensionHelpData.left = new FormAttachment(extensionText, 20, SWT.RIGHT);
        extensionHelpLabel.setLayoutData(extensionHelpData);
        extensionHelpLabel.setText(Messages.getString("ExternalToolDialog.ExtentionHelpLabelText")); //$NON-NLS-1$

        if (hasDirectoryExtension()) {
            extensionHelpLabel.setVisible(false);
        }

        ControlSize.setCharWidthHint(extensionText, 20);

        final FormData commandPromptData = new FormData();
        commandPromptData.top =
            new FormAttachment(commandText, FormHelper.VerticalOffset(commandPromptLabel, commandText), SWT.TOP);
        commandPromptData.left = new FormAttachment(0, 0);
        commandPromptLabel.setLayoutData(commandPromptData);
        commandPromptLabel.setText(Messages.getString("ExternalToolDialog.CommandPromptLabelText")); //$NON-NLS-1$

        final FormData commandTextData = new FormData();
        commandTextData.top = new FormAttachment(
            commandBrowseButton,
            FormHelper.VerticalOffset(commandText, commandBrowseButton),
            SWT.TOP);
        commandTextData.left = new FormAttachment(commandPromptLabel, 0, SWT.RIGHT);
        commandTextData.right = new FormAttachment(commandBrowseButton, 0, SWT.LEFT);
        commandText.setLayoutData(commandTextData);
        ControlSize.setCharWidthHint(commandText, 35);

        if (existingAssociation.getTool() != null) {
            commandText.setText(existingAssociation.getTool().getOriginalCommandAndArguments());
        }

        final FormData commandBrowseData = new FormData();
        commandBrowseData.top = new FormAttachment(extensionText, 0, SWT.BOTTOM);
        commandBrowseData.right = new FormAttachment(100, 0);
        commandBrowseButton.setLayoutData(commandBrowseData);
        commandBrowseButton.setText(Messages.getString("ExternalToolDialog.CommandBrowseButtonText")); //$NON-NLS-1$
        commandBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browse();
            }
        });

        String explanationText = Messages.getString("ExternalToolDialog.ExplanationText"); //$NON-NLS-1$
        if (getExplanationText().length() > 0) {
            final String messageFormat = Messages.getString("ExternalToolDialog.ExplanationTextFormat"); //$NON-NLS-1$
            explanationText = MessageFormat.format(messageFormat, getExplanationText());
        }

        final Label explanationLabel = new Label(container, SWT.NONE);
        final FormData explanationLabelData = new FormData();
        explanationLabelData.top = new FormAttachment(commandText, 10, SWT.BOTTOM);
        explanationLabelData.left = new FormAttachment(0, 0);
        explanationLabel.setLayoutData(explanationLabelData);
        explanationLabel.setText(explanationText);

        final Label requiredLabel = new Label(container, SWT.NONE);
        final FormData requiredLabelData = new FormData();
        requiredLabelData.top = new FormAttachment(explanationLabel, ((0 - containerLayout.spacing) / 2), SWT.BOTTOM);
        requiredLabelData.left = new FormAttachment(0, 0);
        requiredLabel.setLayoutData(requiredLabelData);
        requiredLabel.setText(getRequiredText());

        final Label optionalLabel = new Label(container, SWT.NONE);
        final FormData optionalLabelData = new FormData();
        optionalLabelData.top = new FormAttachment(explanationLabel, ((0 - containerLayout.spacing) / 2), SWT.BOTTOM);
        optionalLabelData.left = new FormAttachment(requiredLabel, 10, SWT.RIGHT);
        optionalLabelData.right = new FormAttachment(100, 0);
        optionalLabel.setLayoutData(optionalLabelData);
        optionalLabel.setText(getOptionalText());

        return container;
    }

    protected String getExplanationText() {
        return ""; //$NON-NLS-1$
    }

    protected String getRequiredText() {
        return ""; //$NON-NLS-1$
    }

    protected String getOptionalText() {
        return ""; //$NON-NLS-1$
    }

    /**
     * Gets the {@link ExternalToolValidator} appropriate for this kind of
     * external tool configuration dialog.
     *
     * @return an {@link ExternalToolValidator}
     */
    protected abstract ExternalToolValidator getToolValidator();

    @Override
    protected void okPressed() {
        String errorMessage = null;

        String[] newExtensions = null;

        if (hasDirectoryExtension() == false) {
            newExtensions = splitExtensions(extensionText.getText());
        } else {
            newExtensions = new String[] {
                ExternalToolset.DIRECTORY_EXTENSION
            };
        }

        final String newCommandAndArguments = commandText.getText();

        if (hasDirectoryExtension() == false && newExtensions.length == 0) {
            errorMessage = Messages.getString("ExternalToolDialog.FileExtensionRequired"); //$NON-NLS-1$
        } else if (newCommandAndArguments == null || newCommandAndArguments.length() == 0) {
            errorMessage = Messages.getString("ExternalToolDialog.CommandAndArgsRequired"); //$NON-NLS-1$
        }

        if (errorMessage != null) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ExternalToolDialog.InvalidConfigDialogTitle"), //$NON-NLS-1$
                errorMessage);
            return;
        }

        // sanity check input on the extensions
        if (newExtensions != null && hasDirectoryExtension() == false) {
            for (int i = 0; i < newExtensions.length && errorMessage == null; i++) {
                if (newExtensions[i] == null || newExtensions[i].length() == 0) {
                    errorMessage = Messages.getString("ExternalToolDialog.EmptyFileExtensionError"); //$NON-NLS-1$
                } else if (newExtensions[i].startsWith(".")) //$NON-NLS-1$
                {
                    errorMessage = Messages.getString("ExternalToolDialog.InvalidFileExtension"); //$NON-NLS-1$
                } else if (newExtensions[i].equals(ExternalToolset.DIRECTORY_EXTENSION)) {
                    errorMessage = Messages.getString("ExternalToolDialog.InvalidFileExtensionHasSlash"); //$NON-NLS-1$
                }

                /* Test against existing tool associations */
                for (int j = 0; j < otherAssociations.length && errorMessage == null; j++) {
                    /*
                     * If we're editing an existing association, allow to
                     * proceed. Note that we're given an existingAssociation in
                     * duplicate mode, but isNew is true. This requires users to
                     * edit the file extensions.
                     */
                    if (existingAssociation != null && (existingAssociation.equals(otherAssociations[j]) && !isNew)) {
                        continue;
                    }

                    final String[] otherExtensions = otherAssociations[j].getExtensions();

                    for (int k = 0; k < otherExtensions.length && errorMessage == null; k++) {
                        if (otherExtensions[k].equalsIgnoreCase(newExtensions[i])) {
                            errorMessage = MessageFormat.format(
                                Messages.getString("ExternalToolDialog.DuplicateFileExtensionFormat"), //$NON-NLS-1$
                                newExtensions[i]);
                        }
                    }
                }
            }
        }

        if (errorMessage == null) {
            try {
                final ExternalTool tool = new ExternalTool(newCommandAndArguments);

                /*
                 * Delegate to the derived class to validate.
                 */
                getToolValidator().validate(tool);

                newAssociation = new ExternalToolAssociation(newExtensions, tool);
            } catch (final Exception e) {
                errorMessage = e.getMessage();
            }
        }

        if (errorMessage != null) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ExternalToolDialog.InvalidConfigDialogTitle"), //$NON-NLS-1$
                errorMessage);
            return;
        }

        super.okPressed();
    }

    /**
     * @return the new {@link ExternalToolAssociation} made after OK was clicked
     *         in the dialog. Null if OK was not successful.
     */
    public ExternalToolAssociation getNewAssociation() {
        return newAssociation;
    }

    private void browse() {
        final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
        String filename = fileDialog.open();

        if (filename != null) {
            // If the filename has a space in it, put the filename in quotes.
            if (filename.indexOf(' ') > 0) {
                filename = "\"" + filename + "\""; //$NON-NLS-1$ //$NON-NLS-2$
            }

            /*
             * If there's already a command string, just replace the first token
             * with the browse result.
             */
            final String existingCommandAndArguments = commandText.getText();

            if (existingCommandAndArguments.length() == 0) {
                commandText.setText(filename);
            } else {
                final String commandPart = WindowsStyleArgumentTokenizer.getRawFirstToken(existingCommandAndArguments);
                commandText.setText(filename + existingCommandAndArguments.substring(commandPart.length()));
            }
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        if (helpFont != null && !helpFont.isDisposed()) {
            helpFont.dispose();
            helpFont = null;
        }
    }
}
