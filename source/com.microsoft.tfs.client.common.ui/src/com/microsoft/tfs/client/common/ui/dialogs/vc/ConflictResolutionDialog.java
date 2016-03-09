// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.conflicts.ConflictComparisonFactory;
import com.microsoft.tfs.client.common.ui.conflicts.ConflictComparisonOption;
import com.microsoft.tfs.client.common.ui.conflicts.resolutions.contributors.EclipseMergeConflictResolutionContributor;
import com.microsoft.tfs.client.common.ui.controls.generic.menubutton.MenuButton;
import com.microsoft.tfs.client.common.ui.controls.generic.menubutton.MenuButtonFactory;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUIType;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.prefs.ExternalToolPreferenceKey;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.ConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.FilenameConflictDescription;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.ConflictResolution;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.CompositeConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.conflicts.resolutions.contributors.ExternalConflictResolutionContributor;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.util.MementoRepository;

/**
 * ConflictResolutionDialog offers conflict resolution for a single conflict.
 */
public class ConflictResolutionDialog extends BaseDialog {
    public static final CodeMarker CODEMARKER_CONFLICTANALYSIS_COMPLETE = new CodeMarker(
        "com.microsoft.tfs.client.common.ui.dialogs.vc.ConflictResolutionDialog#conflictAnalysisComplete"); //$NON-NLS-1$

    public static final String DESCRIPTION_TEXT_ID = "ConflictResolutionDialog.DescriptionText"; //$NON-NLS-1$

    private boolean userSelected = false;

    private final ConflictDescription conflictDescription;

    private ConflictResolution conflictResolution;

    private Label changeDescriptionLabel;
    private final Map<ConflictResolution, Button> optionButtonMap = new LinkedHashMap<ConflictResolution, Button>();

    private MenuButton compareButton;

    /**
     * The currently configured merge tools, cached on dialog creation to avoid
     * loading every time we need to query for resolutions.
     */
    private final CompositeConflictResolutionContributor resolutionContributor;

    public ConflictResolutionDialog(final Shell parentShell, final ConflictDescription conflictDescription) {
        super(parentShell);

        this.conflictDescription = conflictDescription;

        setOptionPersistGeometry(false);
        setOptionResizableDirections(SWT.HORIZONTAL);

        /*
         * Eclipse/SWT 3.0 doesn't handle multi-line text boxes correctly, so
         * let it fill.
         */
        if (SWT.getVersion() >= 3100) {
            setOptionConstrainSize(new Point(600, SWT.DEFAULT));
        }

        resolutionContributor = new CompositeConflictResolutionContributor();
        resolutionContributor.addContributor(new EclipseMergeConflictResolutionContributor());
        resolutionContributor.addContributor(
            new ExternalConflictResolutionContributor(
                ExternalToolset.loadFromMemento(
                    new MementoRepository(
                        DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                            ExternalToolPreferenceKey.MERGE_KEY))));
    }

    @Override
    protected String provideDialogTitle() {
        final String filename =
            (conflictDescription.getLocalPath() != null) ? LocalPath.getFileName(conflictDescription.getLocalPath())
                : Messages.getString("ConflictResolutionDialog.UnknownLocalPath"); //$NON-NLS-1$

        final String messageFormat = Messages.getString("ConflictResolutionDialog.DialogTitleFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, conflictDescription.getName(), filename);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout(1, true);
        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginHeight = getVerticalMargin();
        dialogLayout.horizontalSpacing = getHorizontalSpacing();
        dialogLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(dialogLayout);

        /*
         * Description: use a wrapping text box on SWT >= 3100, use a wrapping
         * label on SWT < 3100. (Text boxes don't wrap, don't size properly on
         * Eclipse/SWT 3.0.)
         */
        final Text descriptionText = new Text(dialogArea, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
        AutomationIDHelper.setWidgetID(descriptionText, DESCRIPTION_TEXT_ID);
        descriptionText.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        descriptionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        descriptionText.setText(getDescription());

        // spacer -- no verticalIdent in gridlayout < SWT 3100
        final Composite spacer = new Composite(dialogArea, SWT.NONE);
        spacer.setLayoutData(new GridData(1, 1));

        // conflictDetailsGroup
        final Group detailsGroup = new Group(dialogArea, SWT.NONE);
        final GridData detailsGroupData = new GridData(GridData.FILL_HORIZONTAL);
        detailsGroup.setLayoutData(detailsGroupData);
        detailsGroup.setText(Messages.getString("ConflictResolutionDialog.DetailsGroupText")); //$NON-NLS-1$
        detailsGroup.setLayout(new GridLayout(2, false));

        // Changes: label
        if (conflictDescription.showChangeDescription()) {
            // "Changes:" label
            final Label changesLabel = new Label(detailsGroup, SWT.NONE);
            changesLabel.setText(Messages.getString("ConflictResolutionDialog.ChangesLabelText")); //$NON-NLS-1$

            changeDescriptionLabel = new Label(detailsGroup, SWT.NONE);
            final GridData changeDescriptionLabelData = new GridData(GridData.FILL_HORIZONTAL);
            changeDescriptionLabelData.horizontalIndent = 10;
            changeDescriptionLabel.setLayoutData(changeDescriptionLabelData);
            changeDescriptionLabel.setText(Messages.getString("ConflictResolutionDialog.ChangeDescriptionLabelText")); //$NON-NLS-1$
        }

        // "Path:" label
        final Label pathLabel = new Label(detailsGroup, SWT.NONE);
        pathLabel.setText(Messages.getString("ConflictResolutionDialog.PathLabelText")); //$NON-NLS-1$

        // path text
        final String localPath = conflictDescription.getLocalPath() != null ? conflictDescription.getLocalPath()
            : Messages.getString("ConflictResolutionDialog.UnknownLocalPath"); //$NON-NLS-1$

        final Text pathText = new Text(detailsGroup, SWT.READ_ONLY | SWT.BORDER);
        final GridData pathTextData = new GridData(GridData.FILL_HORIZONTAL);
        pathTextData.horizontalIndent = 5;
        pathText.setLayoutData(pathTextData);
        pathText.setText(localPath);

        // resolutionGroup
        final Group resolutionGroup = new Group(dialogArea, SWT.NONE);
        resolutionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        resolutionGroup.setText(Messages.getString("ConflictResolutionDialog.ResolutionGroupText")); //$NON-NLS-1$
        resolutionGroup.setLayout(new GridLayout());

        // resolution options
        final ConflictResolution[] resolutions = conflictDescription.getResolutions(resolutionContributor);

        for (int i = 0; i < resolutions.length; i++) {
            final Button optionButton = new Button(resolutionGroup, SWT.RADIO);
            AutomationIDHelper.setWidgetID(optionButton, resolutions[i].getDescription());
            optionButton.setText(resolutions[i].getDescription());
            optionButton.setToolTipText(resolutions[i].getHelpText());

            optionButtonMap.put(resolutions[i], optionButton);

            // setup a boolean to indicate whether the user has selected an
            // option
            optionButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    userSelected = true;
                }
            });
        }
    }

    /*
     * override Dialog.createButtonBar so that we can add a label to the left of
     * the buttons which shows resolution progress, a la MSFT
     *
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets
     * .Composite)
     */
    @Override
    protected Control createButtonBar(final Composite parent) {
        final Composite buttonBar = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = false;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
        buttonBar.setLayout(layout);

        final GridData data = new GridData(SWT.FILL, SWT.BOTTOM, true, false);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = false;
        buttonBar.setLayoutData(data);

        buttonBar.setFont(parent.getFont());

        // this is the button that allows comparisons
        compareButton = MenuButtonFactory.getMenuButton(buttonBar, SWT.NONE);
        compareButton.setText(Messages.getString("ConflictResolutionDialog.CompareButtonText")); //$NON-NLS-1$

        final GridData compareButtonData = new GridData(SWT.LEFT, SWT.CENTER, true, true);
        compareButtonData.grabExcessHorizontalSpace = true;
        compareButtonData.horizontalIndent = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        compareButton.setLayoutData(compareButtonData);

        // add the dialog's button bar to the right
        final Control buttonControl = super.createButtonBar(buttonBar);
        buttonControl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

        return buttonBar;
    }

    @Override
    protected void hookAfterButtonsCreated() {
        enableResolutionOptions();

        enableComparisonOptions();

        analyzeConflict();
    }

    private String getDescription() {
        final ConflictType conflictType = conflictDescription.getConflict().getType();

        final String serverPath = (conflictDescription.getServerPath() != null) ? conflictDescription.getServerPath()
            : Messages.getString("ConflictResolutionDialog.UnknownServerPath"); //$NON-NLS-1$

        // TODO This doesn't look very loc friendly (is it always right to
        // lower-case the first char?). Maybe use a duplicate lower-case string
        // resource?

        final String description = conflictDescription.getDescription().substring(0, 1).toLowerCase()
            + conflictDescription.getDescription().substring(1);

        if (conflictType == ConflictType.CHECKIN) {
            final String messageFormat = Messages.getString("ConflictResolutionDialog.CouldNotCheckinFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, serverPath, description);
        } else if (conflictType == ConflictType.MERGE) {
            final String messageFormat = Messages.getString("ConflictResolutionDialog.CouldNotMergeFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, serverPath, description);
        } else {
            final String messageFormat = Messages.getString("ConflictResolutionDialog.CouldNotRetrieveFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, serverPath, description);
        }
    }

    private void enableResolutionOptions() {
        boolean hasSelected = false;

        // make a first pass through the options if the user has selected
        // something. we may be disabling the option they selected.
        if (userSelected) {
            for (final Iterator<Entry<ConflictResolution, Button>> i =
                optionButtonMap.entrySet().iterator(); i.hasNext();) {
                final Entry<ConflictResolution, Button> entry = i.next();

                final ConflictResolution resolution = entry.getKey();
                final Button resolutionButton = entry.getValue();

                // if this button is selected (by the user), and is about to be
                // disabled, flag that they haven't
                // made a selection (for the next pass)
                if (!resolutionButton.isDisposed()
                    && resolutionButton.getSelection()
                    && !conflictDescription.isResolutionEnabled(resolution)) {
                    userSelected = false;
                }
            }
        }

        // now we walk through the options and set enablement and default
        // selection
        for (final Iterator<Entry<ConflictResolution, Button>> i =
            optionButtonMap.entrySet().iterator(); i.hasNext();) {
            final Entry<ConflictResolution, Button> entry = i.next();

            final ConflictResolution resolution = entry.getKey();
            final Button resolutionButton = entry.getValue();

            if (resolutionButton.isDisposed()) {
                continue;
            }

            final boolean enabled = conflictDescription.isResolutionEnabled(resolution);
            resolutionButton.setEnabled(enabled);

            if (userSelected) {
                // the user has selected an option, don't bother changing their
                // selection
            } else if (enabled && !hasSelected) {
                resolutionButton.setSelection(true);
                resolutionButton.setFocus();
                hasSelected = true;
            } else {
                resolutionButton.setSelection(false);
            }
        }
    }

    private void enableComparisonOptions() {
        final ConflictComparisonOption[] comparisons =
            ConflictComparisonFactory.getConflictComparison(conflictDescription).getOptions();

        if (comparisons == null || comparisons.length == 0) {
            compareButton.setEnabled(false);
            return;
        }

        final IAction[] comparisonActions = new IAction[comparisons.length];

        /* TODO: compare to arbitrary? */

        int enabledComparisons = 0;
        for (int i = 0; i < comparisons.length; i++) {
            final Object modifiedNode = comparisons[i].getModifiedNode();
            final Object originalNode = comparisons[i].getOriginalNode();

            comparisonActions[i] = new Action() {
                @Override
                public void run() {
                    final Compare compare = new Compare();

                    compare.setModified(modifiedNode);
                    compare.setOriginal(originalNode);

                    compare.addComparator(TFSItemContentComparator.INSTANCE);

                    compare.setUIType(CompareUIType.DIALOG);

                    compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(getShell()));
                    compare.open();
                }
            };

            final String messageFormat = Messages.getString("ConflictResolutionDialog.CompareActionTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                comparisons[i].getModifiedTitle(),
                comparisons[i].getOriginalTitle());

            comparisonActions[i].setText(message);

            if (modifiedNode == null || originalNode == null) {
                comparisonActions[i].setEnabled(false);
            } else {
                enabledComparisons++;
            }
        }

        /* Setup the menu when the disclosure triangle is clicked */
        compareButton.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                for (int i = 0; i < comparisonActions.length; i++) {
                    manager.add(comparisonActions[i]);
                }
            }
        });

        /* If the button is just clicked, open the default compare */
        compareButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                comparisonActions[0].run();
            }
        });

        compareButton.setEnabled(enabledComparisons > 0);
    }

    private void analyzeConflict() {
        final Shell shell = getShell();

        final Thread analyzerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // if analyzeConflict() returns false, there's nothing to do
                if (!conflictDescription.analyzeConflict()) {
                    CodeMarkerDispatch.dispatch(CODEMARKER_CONFLICTANALYSIS_COMPLETE);
                    return;
                }

                if (shell == null || shell.isDisposed()) {
                    return;
                }

                // run on the ui thread to avoid contention
                UIHelpers.runOnUIThread(shell, false, new Runnable() {
                    @Override
                    public void run() {
                        if (shell.isDisposed()) {
                            return;
                        }

                        // change description text has probably changed
                        if (!changeDescriptionLabel.isDisposed()) {
                            changeDescriptionLabel.setText(conflictDescription.getChangeDescription());
                        }

                        // check resolution options again
                        enableResolutionOptions();
                        CodeMarkerDispatch.dispatch(CODEMARKER_CONFLICTANALYSIS_COMPLETE);
                    }
                });
            }
        });

        analyzerThread.start();
    }

    @Override
    protected void okPressed() {
        /* Determine the selected resolution */
        ConflictResolution selectedResolution = null;

        for (final Iterator<Entry<ConflictResolution, Button>> i =
            optionButtonMap.entrySet().iterator(); i.hasNext();) {
            final Entry<ConflictResolution, Button> entry = i.next();

            final ConflictResolution resolution = entry.getKey();
            final Button resolutionButton = entry.getValue();

            if (resolutionButton.getSelection()) {
                selectedResolution = resolution;
                break;
            }
        }

        if (selectedResolution == null) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ConflictResolutionDialog.ErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("ConflictResolutionDialog.ErrorDialogText")); //$NON-NLS-1$
            return;
        }

        /*
         * See if the user has selected a resolution type that requires a new
         * name.
         */

        /* Filename conflicts (eg, add of an existing filename) */
        if (selectedResolution.needsNewPath() && conflictDescription instanceof FilenameConflictDescription) {
            final ConflictResolutionRenameDialog renameDialog = new ConflictResolutionRenameDialog(getShell());
            renameDialog.setFilename(conflictDescription.getServerPath());

            if (renameDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            selectedResolution.setNewPath(renameDialog.getFilename());
        }

        /*
         * User must resolve both a name conflict and an encoding conflict.
         */
        else if (selectedResolution.needsNewPath() && selectedResolution.needsEncodingSelection()) {
            final ConflictResolutionNameAndEncodingDialog nameAndEncodingDialog =
                new ConflictResolutionNameAndEncodingDialog(getShell());
            nameAndEncodingDialog.setConflictDescription(conflictDescription);

            if (nameAndEncodingDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            selectedResolution.setNewPath(nameAndEncodingDialog.getFilename());
            selectedResolution.setEncoding(nameAndEncodingDialog.getFileEncoding());
        }

        /*
         * Some version / merge conflicts require name selection (eg, target
         * rename)
         */
        else if (selectedResolution.needsNewPath()) {
            final ConflictResolutionNameSelectionDialog nameDialog =
                new ConflictResolutionNameSelectionDialog(getShell());
            nameDialog.setConflictDescription(conflictDescription);

            if (nameDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            selectedResolution.setNewPath(nameDialog.getFilename());
        }

        else if (selectedResolution.needsEncodingSelection()) {
            final ConflictResolutionEncodingDialog encodingDialog = new ConflictResolutionEncodingDialog(getShell());

            encodingDialog.setConflictDescription(conflictDescription);

            if (encodingDialog.open() != IDialogConstants.OK_ID) {
                return;
            }

            selectedResolution.setEncoding(encodingDialog.getFileEncoding());
        }

        conflictResolution = selectedResolution;
        super.okPressed();
    }

    public ConflictResolution getResolution() {
        return conflictResolution;
    }
}
