// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.dialogs;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.AutocompleteCombo;
import com.microsoft.tfs.client.common.ui.controls.generic.DatepickerCombo;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.client.common.ui.wit.WITSearchModel;
import com.microsoft.tfs.client.common.ui.wit.controls.ClassificationCombo;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemQueryUtils;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.node.Node.TreeType;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

public class WITSearchDialog extends BaseDialog {
    public static final int EDIT_IN_QUERY_BUILDER = IDialogConstants.CLIENT_ID + 2;

    public static final String PROJECTS_DROPDOWN_ID = "WITSearchDialog.projectsCombo"; //$NON-NLS-1$
    public static final String WORKITEMTYPE_DROPDOWN_ID = "WITSearchDialog.typeCombo"; //$NON-NLS-1$
    public static final String STATE_DROPDOWN_ID = "WITSearchDialog.stateCombo"; //$NON-NLS-1$

    private static final String MODIFY_LISTENER_KEY = "modify-listener-key"; //$NON-NLS-1$
    private static final String DIALOG_SETTINGS_SECTION_KEY = "wit-search-dialog"; //$NON-NLS-1$

    private final WorkItemClient witClient;
    private final FieldDefinition[] queryableFields;
    private FieldDefinition stateFieldDefinition;
    private FieldDefinition assignedToFieldDefinition;
    private final Project[] projects;
    private final String[] uniqueTypeNames;
    private int selectedProjectIndex = -1;
    protected WITSearchModel model;

    public WITSearchDialog(final Shell parentShell, final WorkItemClient witClient, final String projectName) {
        this(parentShell, witClient, projectName, false);
    }

    public WITSearchDialog(
        final Shell parentShell,
        final WorkItemClient witClient,
        final String projectName,
        final boolean enableEditInQueryBuilderButton) {
        super(parentShell);
        this.witClient = witClient;

        if (enableEditInQueryBuilderButton) {
            addButtonDescription(EDIT_IN_QUERY_BUILDER, Messages.getString("WITSearchDialog.EditButtonText"), false); //$NON-NLS-1$
        }

        final Set<FieldDefinition> queryableFieldDefinitions = new HashSet<FieldDefinition>();
        for (final FieldDefinition fieldDefinition : witClient.getFieldDefinitions()) {
            if (fieldDefinition.isQueryable()) {
                queryableFieldDefinitions.add(fieldDefinition);
                if (fieldDefinition.getReferenceName().equals(CoreFieldReferenceNames.STATE)) {
                    stateFieldDefinition = fieldDefinition;
                } else if (fieldDefinition.getReferenceName().equals(CoreFieldReferenceNames.ASSIGNED_TO)) {
                    assignedToFieldDefinition = fieldDefinition;
                }
            }
        }

        queryableFields = queryableFieldDefinitions.toArray(new FieldDefinition[] {});
        Arrays.sort(queryableFields);
        projects = witClient.getProjects().getProjects();
        uniqueTypeNames = computeUniqueWorkItemTypeNames(witClient);

        model = new WITSearchModel(queryableFields, projects, uniqueTypeNames);

        final IDialogSettings uiSettings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        final IDialogSettings witSearchDialogSettings = uiSettings.getSection(DIALOG_SETTINGS_SECTION_KEY);
        if (witSearchDialogSettings != null) {
            model.restoreFromSettings(witSearchDialogSettings);
        }
        if (projectName != null) {
            model.setProjectName(projectName);
        }
        if (model.getProjectName() != null) {
            int ix = 0;
            while (selectedProjectIndex == -1 && ix < projects.length) {
                if (projects[ix].getName().equals(model.getProjectName())) {
                    selectedProjectIndex = ix;
                }
                ++ix;
            }
        }
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (EDIT_IN_QUERY_BUILDER == buttonId) {
            editInQueryBuilder();
        }
    }

    private void editInQueryBuilder() {
        setReturnCode(EDIT_IN_QUERY_BUILDER);
        close();
    }

    private String[] computeUniqueWorkItemTypeNames(final WorkItemClient client) {
        final Set<String> names = new HashSet<String>();

        for (final Project project : client.getProjects()) {
            for (final WorkItemType type : project.getWorkItemTypes()) {
                names.add(type.getName());
            }
        }

        final String[] sortedNames = names.toArray(new String[] {});
        Arrays.sort(sortedNames);
        return sortedNames;
    }

    public Query createQuery() {
        return witClient.createQuery(
            getWIQL(),
            WorkItemQueryUtils.makeContext(getSelectedProject(), WorkItemHelpers.getCurrentTeamName()));
    }

    public String getWIQL() {
        return model.buildWIQL();
    }

    public Project getSelectedProject() {
        if (selectedProjectIndex == -1) {
            return null;
        } else {
            return projects[selectedProjectIndex];
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        final IDialogSettings uiSettings = TFSCommonUIClientPlugin.getDefault().getDialogSettings();
        IDialogSettings witSearchDialogSettings = uiSettings.getSection(DIALOG_SETTINGS_SECTION_KEY);
        if (witSearchDialogSettings == null) {
            witSearchDialogSettings = uiSettings.addNewSection(DIALOG_SETTINGS_SECTION_KEY);
        }
        model.saveToSettings(witSearchDialogSettings);
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("WITSearchDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button okButton = getButton(IDialogConstants.OK_ID);
        final Button editInQueryBuilderButton = getButton(EDIT_IN_QUERY_BUILDER);
        okButton.setEnabled(model.isValid());
        if (editInQueryBuilderButton != null) {
            editInQueryBuilderButton.setEnabled(model.isValid());
        }

        model.addModelChangeListener(new WITSearchModel.WITSearchModelChangedListener() {
            @Override
            public void modelChanged() {
                okButton.setEnabled(model.isValid());
                if (editInQueryBuilderButton != null) {
                    editInQueryBuilderButton.setEnabled(model.isValid());
                }
            }
        });
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, true);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final Composite searchComposite = new Composite(dialogArea, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        searchComposite.setLayoutData(gd);

        final Composite constraintsComposite = new Composite(dialogArea, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        constraintsComposite.setLayoutData(gd);

        final Group wiqlPreviewComposite = new Group(dialogArea, SWT.NONE);
        wiqlPreviewComposite.setText(Messages.getString("WITSearchDialog.PreviewCompositeLabelText")); //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        wiqlPreviewComposite.setLayoutData(gd);

        populateSearchArea(searchComposite);
        populateConstraintsArea(constraintsComposite);
        populateWiqlPreviewArea(wiqlPreviewComposite);
    }

    private void populateWiqlPreviewArea(final Composite wiqlPreviewComposite) {
        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        wiqlPreviewComposite.setLayout(layout);

        final Text text =
            new Text(wiqlPreviewComposite, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        final GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        text.setLayoutData(gd);
        ControlSize.setCharHeightHint(text, 4);
        ControlSize.setCharWidthHint(text, 80);

        final WITSearchModel.WITSearchModelChangedListener listener =
            new WITSearchModel.WITSearchModelChangedListener() {
                @Override
                public void modelChanged() {
                    if (model.isValid()) {
                        text.setText(model.buildWIQLWhereClause());
                    } else {
                        text.setText(Messages.getString("WITSearchDialog.WIQLPreviewNotAvailable")); //$NON-NLS-1$
                    }
                }
            };

        listener.modelChanged();
        model.addModelChangeListener(listener);
    }

    private void populateConstraintsArea(final Composite composite) {
        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Group criteriaGroup = new Group(composite, SWT.NONE);
        criteriaGroup.setText(Messages.getString("WITSearchDialog.AdditionalCriteriaButtonText")); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        criteriaGroup.setLayoutData(gd);

        final Group createdDateGroup = new Group(composite, SWT.NONE);
        createdDateGroup.setText(Messages.getString("WITSearchDialog.CreatedDateGroupText")); //$NON-NLS-1$
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        createdDateGroup.setLayoutData(gd);

        final Group changedDateGroup = new Group(composite, SWT.NONE);
        changedDateGroup.setText(Messages.getString("WITSearchDialog.ChangedDateGroupText")); //$NON-NLS-1$
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        changedDateGroup.setLayoutData(gd);

        populateCriteriaArea(criteriaGroup);
        populateDateArea(createdDateGroup, CoreFieldReferenceNames.CREATED_DATE);
        populateDateArea(changedDateGroup, CoreFieldReferenceNames.CHANGED_DATE);
    }

    private void populateDateArea(final Composite composite, final String fieldName) {
        final GridLayout layout = new GridLayout(4, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Label afterLabel = new Label(composite, SWT.NONE);
        afterLabel.setText(Messages.getString("WITSearchDialog.AfterLabelText")); //$NON-NLS-1$

        final DatepickerCombo afterDatePicker =
            new DatepickerCombo(composite, SWT.BORDER, DateFormat.getDateInstance(DateFormat.SHORT));
        afterDatePicker.setDate(model.getDate(fieldName, true));
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        afterDatePicker.setLayoutData(gd);
        afterDatePicker.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                model.setDate(afterDatePicker.getDate(), fieldName, true);
            }
        });

        final Label beforeLabel = new Label(composite, SWT.NONE);
        beforeLabel.setText(Messages.getString("WITSearchDialog.BeforeLabelText")); //$NON-NLS-1$

        final DatepickerCombo beforeDatePicker =
            new DatepickerCombo(composite, SWT.BORDER, DateFormat.getDateInstance(DateFormat.SHORT));
        beforeDatePicker.setDate(model.getDate(fieldName, false));
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        beforeDatePicker.setLayoutData(gd);
        beforeDatePicker.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                model.setDate(beforeDatePicker.getDate(), fieldName, false);
            }
        });
    }

    private void populateSearchArea(final Composite composite) {
        final GridLayout layout = new GridLayout(1, true);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Group searchTermsGroup = new Group(composite, SWT.NONE);
        searchTermsGroup.setText(Messages.getString("WITSearchDialog.SearchTermsGroupText")); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        searchTermsGroup.setLayoutData(gd);

        final Group searchFieldsGroup = new Group(composite, SWT.NONE);
        searchFieldsGroup.setText(Messages.getString("WITSearchDialog.SearchFieldsGroupText")); //$NON-NLS-1$
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        searchFieldsGroup.setLayoutData(gd);

        populateSearchTermsArea(searchTermsGroup);
        populateSearchFieldsArea(searchFieldsGroup);
    }

    private void populateSearchTermsArea(final Composite composite) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Text searchTermsText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP);
        final GridData gd = new GridData();
        gd.verticalSpan = 4;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.verticalAlignment = SWT.FILL;
        searchTermsText.setLayoutData(gd);
        searchTermsText.setText(model.getSearchTerms());
        final ModifyListener modifyListener = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                model.setSearchTerms(searchTermsText.getText());
            }
        };
        searchTermsText.addModifyListener(modifyListener);
        searchTermsText.setData(MODIFY_LISTENER_KEY, modifyListener);
        searchTermsText.addFocusListener(
            new HintDecorator(Messages.getString("WITSearchDialog.EnterSearchTermsHere"), searchTermsText)); //$NON-NLS-1$

        final Label typeLabel = new Label(composite, SWT.NONE);
        typeLabel.setText(Messages.getString("WITSearchDialog.TypeLabelText")); //$NON-NLS-1$

        /*
         * ALL
         */
        final Button allTypeButton = new Button(composite, SWT.RADIO);
        allTypeButton.setText(Messages.getString("WITSearchDialog.AllTermsButtonText")); //$NON-NLS-1$
        if (model.getSearchType() == WITSearchModel.SEARCH_TYPE_ALL_TERMS) {
            allTypeButton.setSelection(true);
        }
        allTypeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setSearchType(WITSearchModel.SEARCH_TYPE_ALL_TERMS);
            }
        });

        /*
         * ANY
         */
        final Button anyTypeButton = new Button(composite, SWT.RADIO);
        anyTypeButton.setText(Messages.getString("WITSearchDialog.AnyTermButtonText")); //$NON-NLS-1$
        if (model.getSearchType() == WITSearchModel.SEARCH_TYPE_ANY_TERM) {
            anyTypeButton.setSelection(true);
        }
        anyTypeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setSearchType(WITSearchModel.SEARCH_TYPE_ANY_TERM);
            }
        });
    }

    private void populateSearchFieldsArea(final Composite composite) {
        final GridLayout layout = new GridLayout(5, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        /*
         * TITLE
         */
        final Button titleCheck = new Button(composite, SWT.CHECK);
        final Label titleLabel = new Label(composite, SWT.CHECK);
        titleLabel.setText(Messages.getString("WITSearchDialog.TitleLabelText")); //$NON-NLS-1$
        if (model.isIncludeTitle()) {
            titleCheck.setSelection(true);
        }
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                titleCheck.setSelection(!titleCheck.getSelection());
                model.setIncludeTitle(titleCheck.getSelection());
            }
        });
        titleCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setIncludeTitle(titleCheck.getSelection());
            }
        });

        final Label separatorLabel = new Label(composite, SWT.SEPARATOR);
        final GridData gd = new GridData();
        gd.verticalSpan = 3;
        separatorLabel.setLayoutData(gd);

        /*
         * CUSTOM FIELD 1
         */
        final Button customField1Check = new Button(composite, SWT.CHECK);
        customField1Check.setText(Messages.getString("WITSearchDialog.AdditionalFieldButtonText")); //$NON-NLS-1$
        final Combo customField1Cobmo = createQueryableFieldsCombo(composite, queryableFields);
        if (model.isIncludeCustomField1()) {
            customField1Check.setSelection(true);
        }

        ComboHelper.setVisibleItemCount(customField1Cobmo);
        customField1Cobmo.setText(model.getCustomField1());
        customField1Cobmo.setEnabled(customField1Check.getSelection());
        customField1Check.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                customField1Cobmo.setEnabled(customField1Check.getSelection());
                model.setIncludeCustomField1(customField1Check.getSelection());
            }
        });
        customField1Cobmo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setCustomField1(customField1Cobmo.getText());
            }
        });

        /*
         * DESCRIPTION
         */
        final Button descriptionCheck = new Button(composite, SWT.CHECK);
        final Label descriptionLabel = new Label(composite, SWT.CHECK);
        descriptionLabel.setText(Messages.getString("WITSearchDialog.DescriptionLabelText")); //$NON-NLS-1$
        if (model.isIncludeDescription()) {
            descriptionCheck.setSelection(true);
        }
        descriptionLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                descriptionCheck.setSelection(!descriptionCheck.getSelection());
                model.setIncludeDescription(descriptionCheck.getSelection());
            }
        });
        descriptionCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setIncludeDescription(descriptionCheck.getSelection());
            }
        });

        /*
         * CUSTOM FIELD 2
         */
        final Button customField2Check = new Button(composite, SWT.CHECK);
        customField2Check.setText(Messages.getString("WITSearchDialog.AdditionalField2LabelText")); //$NON-NLS-1$
        final Combo customField2Cobmo = createQueryableFieldsCombo(composite, queryableFields);
        if (model.isIncludeCustomField2()) {
            customField2Check.setSelection(true);
        }

        ComboHelper.setVisibleItemCount(customField2Cobmo);
        customField2Cobmo.setText(model.getCustomField2());
        customField2Cobmo.setEnabled(customField2Check.getSelection());
        customField2Check.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                customField2Cobmo.setEnabled(customField2Check.getSelection());
                model.setIncludeCustomField2(customField2Check.getSelection());
            }
        });
        customField2Cobmo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setCustomField2(customField2Cobmo.getText());
            }
        });

        /*
         * HISTORY
         */
        final Button historyCheck = new Button(composite, SWT.CHECK);
        final Label historyLabel = new Label(composite, SWT.CHECK);
        historyLabel.setText(Messages.getString("WITSearchDialog.HistoryLabelText")); //$NON-NLS-1$
        if (model.isIncludeHistory()) {
            historyCheck.setSelection(true);
        }
        historyLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                historyCheck.setSelection(!historyCheck.getSelection());
                model.setIncludeHistory(historyCheck.getSelection());
            }
        });
        historyCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setIncludeHistory(historyCheck.getSelection());
            }
        });

        /*
         * CUSTOM FIELD 3
         */
        final Button customField3Check = new Button(composite, SWT.CHECK);
        customField3Check.setText(Messages.getString("WITSearchDialog.AdditionalField3LabelText")); //$NON-NLS-1$
        final Combo customField3Cobmo = createQueryableFieldsCombo(composite, queryableFields);
        if (model.isIncludeCustomField3()) {
            customField3Check.setSelection(true);
        }

        ComboHelper.setVisibleItemCount(customField3Cobmo);
        customField3Cobmo.setText(model.getCustomField3());
        customField3Cobmo.setEnabled(customField3Check.getSelection());
        customField3Check.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                customField3Cobmo.setEnabled(customField3Check.getSelection());
                model.setIncludeCustomField3(customField3Check.getSelection());
            }
        });
        customField3Cobmo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setCustomField3(customField3Cobmo.getText());
            }
        });
    }

    private void populateCriteriaArea(final Composite composite) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Label projectLabel = new Label(composite, SWT.NONE);
        projectLabel.setText(Messages.getString("WITSearchDialog.ProjectLabelText")); //$NON-NLS-1$
        final Combo projectCombo = createProjectCombo(composite);
        AutomationIDHelper.setWidgetID(projectCombo, PROJECTS_DROPDOWN_ID);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        projectCombo.setLayoutData(gd);
        projectCombo.setText(model.getProjectName());

        final boolean projectSelected = (selectedProjectIndex != -1);
        final Project projectForClassificationControls = (projectSelected ? projects[selectedProjectIndex] : null);

        final Label areaLabel = new Label(composite, SWT.NONE);
        areaLabel.setText(Messages.getString("WITSearchDialog.AreaLabelText")); //$NON-NLS-1$
        final ClassificationCombo areaClassificationCombo = new ClassificationCombo(composite, SWT.BORDER);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        areaClassificationCombo.setLayoutData(gd);
        areaLabel.setEnabled(projectSelected);
        areaClassificationCombo.setProject(projectForClassificationControls);
        areaClassificationCombo.setTreeType(CoreFieldReferenceNames.AREA_PATH);
        areaClassificationCombo.setEnabled(projectSelected);
        areaClassificationCombo.setText(model.getAreaPath());

        final Label iterationLabel = new Label(composite, SWT.NONE);
        iterationLabel.setText(Messages.getString("WITSearchDialog.IterationLabelText")); //$NON-NLS-1$
        final ClassificationCombo iterationClassificationCombo = new ClassificationCombo(composite, SWT.BORDER);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        iterationClassificationCombo.setLayoutData(gd);
        iterationLabel.setEnabled(projectSelected);
        iterationClassificationCombo.setProject(projectForClassificationControls);
        iterationClassificationCombo.setTreeType(CoreFieldReferenceNames.ITERATION_PATH);
        iterationClassificationCombo.setEnabled(projectSelected);
        iterationClassificationCombo.setText(model.getIterationPath());

        final Label typeLabel = new Label(composite, SWT.NONE);
        typeLabel.setText(Messages.getString("WITSearchDialog.WorkItemTypeLabelText")); //$NON-NLS-1$
        final Combo typeCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        AutomationIDHelper.setWidgetID(typeCombo, WORKITEMTYPE_DROPDOWN_ID);
        populateTypeCombo(typeCombo);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        typeCombo.setLayoutData(gd);
        typeCombo.setText(model.getWorkItemTypeName());
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                model.setWorkItemTypeName(typeCombo.getText());
            }
        });

        final Label stateLabel = new Label(composite, SWT.NONE);
        stateLabel.setText(Messages.getString("WITSearchDialog.StateLabelText")); //$NON-NLS-1$

        final Combo stateCombo = new Combo(composite, SWT.DROP_DOWN);
        AutomationIDHelper.setWidgetID(stateCombo, STATE_DROPDOWN_ID);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        stateCombo.setLayoutData(gd);
        final String[] stateValues = stateFieldDefinition.getAllowedValues().getValues();
        for (int i = 0; i < stateValues.length; i++) {
            stateCombo.add(stateValues[i]);
        }
        stateCombo.setText(model.getState());
        stateCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                model.setState(stateCombo.getText());
            }
        });

        final Label assignedToLabel = new Label(composite, SWT.NONE);
        assignedToLabel.setText(Messages.getString("WITSearchDialog.AssignedToLabelText")); //$NON-NLS-1$

        final Combo assignedToCombo = new AutocompleteCombo(composite, SWT.DROP_DOWN);
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        assignedToCombo.setLayoutData(gd);
        final String[] assignedToValues = assignedToFieldDefinition.getAllowedValues().getValues();
        assignedToCombo.setItems(assignedToValues);
        assignedToCombo.setText(model.getAssignedTo());
        assignedToCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                model.setAssignedTo(assignedToCombo.getText());
            }
        });

        areaClassificationCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final String path = ((Combo) e.widget).getText();
                final String resolvedPath = resolvePath(path, Node.TreeType.AREA);
                model.setAreaPath(resolvedPath);
                if (resolvedPath == null && path.trim().length() > 0) {
                    areaClassificationCombo.setBackground(
                        getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                } else {
                    areaClassificationCombo.setBackground(null);
                }
            }
        });

        iterationClassificationCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final String path = ((Combo) e.widget).getText();
                final String resolvedPath = resolvePath(path, Node.TreeType.ITERATION);
                model.setIterationPath(resolvedPath);
                if (resolvedPath == null && path.trim().length() > 0) {
                    iterationClassificationCombo.setBackground(
                        getShell().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                } else {
                    iterationClassificationCombo.setBackground(null);
                }
            }
        });

        projectCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (projectCombo.getSelectionIndex() != 0) {
                    /*
                     * a project was selected
                     */

                    selectedProjectIndex = projectCombo.getSelectionIndex() - 1;

                    areaLabel.setEnabled(true);
                    areaClassificationCombo.setEnabled(true);
                    areaClassificationCombo.setProject(projects[selectedProjectIndex]);
                    areaClassificationCombo.setText(""); //$NON-NLS-1$
                    model.setAreaPath(null);

                    iterationLabel.setEnabled(true);
                    iterationClassificationCombo.setEnabled(true);
                    iterationClassificationCombo.setProject(projects[selectedProjectIndex]);
                    iterationClassificationCombo.setText(""); //$NON-NLS-1$
                    model.setIterationPath(null);

                    model.setProjectName(projects[selectedProjectIndex].getName());

                    populateTypeCombo(typeCombo);
                    model.setWorkItemTypeName(typeCombo.getText());
                } else {
                    /*
                     * no project is selected
                     */

                    selectedProjectIndex = -1;

                    areaClassificationCombo.setText(""); //$NON-NLS-1$
                    areaLabel.setEnabled(false);
                    areaClassificationCombo.setEnabled(false);
                    model.setAreaPath(null);

                    iterationClassificationCombo.setText(""); //$NON-NLS-1$
                    iterationLabel.setEnabled(false);
                    iterationClassificationCombo.setEnabled(false);
                    model.setIterationPath(null);

                    model.setProjectName(null);

                    populateTypeCombo(typeCombo);
                    model.setWorkItemTypeName(typeCombo.getText());
                }
            }
        });
    }

    private String resolvePath(final String path, final TreeType treeType) {
        if (path == null || path.trim().length() == 0) {
            return null;
        }

        final Project project = projects[selectedProjectIndex];

        final Object resolvedObject = project.resolvePath(path, treeType);

        if (resolvedObject == null) {
            return null;
        } else if (resolvedObject instanceof Project) {
            return ((Project) resolvedObject).getName();
        } else {
            return ((Node) resolvedObject).getPath();
        }
    }

    private void populateTypeCombo(final Combo combo) {
        final String oldType = combo.getText();

        combo.removeAll();

        String[] types;

        if (selectedProjectIndex >= 0) {
            final Project project = projects[selectedProjectIndex];
            final WorkItemType[] witTypes = project.getWorkItemTypes().getTypes();
            types = new String[witTypes.length];
            for (int i = 0; i < witTypes.length; i++) {
                types[i] = witTypes[i].getName();
            }
        } else {
            types = uniqueTypeNames;
        }

        Arrays.sort(types);

        combo.add(""); //$NON-NLS-1$
        boolean foundOldType = false;

        for (int i = 0; i < types.length; i++) {
            combo.add(types[i]);
            if (types[i].equals(oldType)) {
                foundOldType = true;
            }
        }

        if (foundOldType) {
            combo.setText(oldType);
        } else {
            combo.setText(""); //$NON-NLS-1$
        }
    }

    private Combo createProjectCombo(final Composite composite) {
        final Combo combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);

        combo.add(""); //$NON-NLS-1$

        for (int i = 0; i < projects.length; i++) {
            combo.add(projects[i].getName());
        }

        return combo;
    }

    private Combo createQueryableFieldsCombo(final Composite composite, final FieldDefinition[] queryableFields) {
        final Combo combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);

        combo.add(WITSearchModel.SEARCH_FIELD_SELECT);
        for (int i = 0; i < queryableFields.length; i++) {
            if (
            /*
             * these 3 fields are hard-coded as search fields - there's no
             * reason to include them as custom fields too
             */
            queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.TITLE)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.DESCRIPTION)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.HISTORY)
                ||

            /*
             * these fields can have values specified in the constraints area
             * (right side of the form) - so no need to have them as custom
             * fields too
             */
                queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.TEAM_PROJECT)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.AREA_PATH)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.ITERATION_PATH)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.WORK_ITEM_TYPE)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.STATE)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.ASSIGNED_TO)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.CHANGED_DATE)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.CREATED_DATE)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.AUTHORIZED_DATE) ||

            /*
             * these tree path id fields shouldn't be included as custom search
             * fields, as the string equivalent fields can be queried in the
             * constraints area
             */
                queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.ITERATION_ID)
                || queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.AREA_ID) ||

            /*
             * System.Id doesn't make a lot of sense to query on
             */
                queryableFields[i].getReferenceName().equals(CoreFieldReferenceNames.ID)

            )

            {
                continue;
            }
            combo.add(queryableFields[i].getName());
        }

        return combo;
    }

    private static class HintDecorator implements FocusListener {
        private static final String DECORATED_KEY = "hint-decorator-decorated-key"; //$NON-NLS-1$
        private final String hint;

        public HintDecorator(final String hint, final Text control) {
            this.hint = hint;
            addDecorationIfNeeded(control);
        }

        @Override
        public void focusGained(final FocusEvent e) {
            if (!(e.widget instanceof Text)) {
                return;
            }

            final Text text = (Text) e.widget;

            final Boolean b = (Boolean) text.getData(DECORATED_KEY);
            if (b != null) {
                if (b.booleanValue()) {
                    final ModifyListener modifyListener = (ModifyListener) text.getData(MODIFY_LISTENER_KEY);
                    if (modifyListener != null) {
                        text.removeModifyListener(modifyListener);
                    }
                    text.setForeground(null);
                    text.setText(""); //$NON-NLS-1$
                    if (modifyListener != null) {
                        text.addModifyListener(modifyListener);
                    }
                }

                text.setData(DECORATED_KEY, Boolean.FALSE);
            }
        }

        @Override
        public void focusLost(final FocusEvent e) {
            if (!(e.widget instanceof Text)) {
                return;
            }

            final Text text = (Text) e.widget;
            addDecorationIfNeeded(text);
        }

        private void addDecorationIfNeeded(final Text text) {
            if (text.getText().trim().length() > 0) {
                return;
            }

            final Boolean b = (Boolean) text.getData(DECORATED_KEY);
            if (b == null || !b.booleanValue()) {
                final ModifyListener modifyListener = (ModifyListener) text.getData(MODIFY_LISTENER_KEY);
                if (modifyListener != null) {
                    text.removeModifyListener(modifyListener);
                }
                text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
                text.setText(hint);
                if (modifyListener != null) {
                    text.addModifyListener(modifyListener);
                }

                text.setData(DECORATED_KEY, Boolean.TRUE);
            }
        }
    }
}
