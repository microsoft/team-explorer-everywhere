// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.wit.dialogs.SelectQueryItemDialog;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolderUtil;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;

public class QuerySaveControl extends BaseControl {
    public static final String QUERY_NAME_TEXT_ID = "QuerySaveControl.queryNameTextId"; //$NON-NLS-1$
    public static final String SERVER_QUERY_NAME_TEXT_ID = "QuerySaveControl.serverQueryNameTextId"; //$NON-NLS-1$
    public static final String SERVER_QUERY_BROWSE_BUTTON_ID = "QuerySaveControl.serverQueryBrowseButtonID"; //$NON-NLS-1$
    public static final String FILE_QUERY_TEXT_ID = "QuerySaveControl.fileQueryTextId"; //$NON-NLS-1$
    public static final String FILE_QUERY_BROWSE_BUTTON_ID = "QuerySaveControl.fileQueryBrowseButtonId"; //$NON-NLS-1$

    public static final class SaveMode {
        public static final SaveMode SERVER = new SaveMode("SERVER"); //$NON-NLS-1$
        public static final SaveMode FILE = new SaveMode("FILE"); //$NON-NLS-1$

        private final String name;

        private SaveMode(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ProjectCollection projectCollection;

    private SaveMode saveMode;
    private String queryName;
    private boolean overwriteExisting;
    private File saveDirectory;

    private QueryFolder parentFolder;

    private boolean ignoreQueryNameChanges = false;
    private boolean ignoreFileLocationChanges = false;

    private final Text queryNameText;
    private final Label serverQueryLabel;
    private final Text serverQueryNameText;
    private final Button serverQueryBrowseButton;
    private final Label fileQueryLabel;
    private final Text fileQueryText;
    private final Button fileQueryBrowseButton;

    public QuerySaveControl(
        final Composite parent,
        final int style,
        final ProjectCollection projectCollection,
        final QueryFolder parentFolder,
        final String serverName,
        final SaveMode initialSaveMode,
        final String initialQueryName,
        final File initialSaveDirectory) {
        super(parent, style);

        Check.notNull(projectCollection, "projectCollection"); //$NON-NLS-1$
        Check.notNull(parentFolder, "parentFolder"); //$NON-NLS-1$
        Check.notNull(serverName, "serverName"); //$NON-NLS-1$
        Check.notNull(initialSaveMode, "initialSaveMode"); //$NON-NLS-1$
        Check.notNull(initialQueryName, "initialQueryName"); //$NON-NLS-1$
        Check.notNull(initialSaveDirectory, "initialSaveDirectory"); //$NON-NLS-1$

        this.projectCollection = projectCollection;
        this.parentFolder = parentFolder;
        saveMode = initialSaveMode;
        queryName = initialQueryName;
        saveDirectory = initialSaveDirectory;

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        Label label = new Label(this, SWT.WRAP);
        label.setText(Messages.getString("QuerySaveControl.ServerLabelText")); //$NON-NLS-1$
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        final Text text = new Text(this, SWT.BORDER);
        text.setEditable(false);
        text.setText(serverName);

        final GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        textData.widthHint = getMinimumMessageAreaWidth();
        text.setLayoutData(textData);

        label = new Label(this, SWT.WRAP);
        label.setText(Messages.getString("QuerySaveControl.NameLabelText")); //$NON-NLS-1$
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));

        queryNameText = new Text(this, SWT.BORDER);
        queryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        queryNameText.setText(queryName);
        queryNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (!ignoreQueryNameChanges) {
                    queryNameChanged(((Text) e.widget).getText());
                }
            }
        });
        queryNameText.setFocus();
        queryNameText.selectAll();
        AutomationIDHelper.setWidgetID(queryNameText, QUERY_NAME_TEXT_ID);

        /* Group these into a composite to tweak layout */
        final Composite saveAreaComposite = new Composite(this, SWT.NONE);
        saveAreaComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        final GridLayout saveAreaLayout = new GridLayout(3, false);
        saveAreaLayout.marginWidth = 0;
        saveAreaLayout.marginHeight = 0;
        saveAreaLayout.horizontalSpacing = getHorizontalSpacing();
        saveAreaLayout.verticalSpacing = 0;
        saveAreaComposite.setLayout(saveAreaLayout);

        label = new Label(saveAreaComposite, SWT.WRAP);
        label.setText(Messages.getString("QuerySaveControl.SelectLocationLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().align(SWT.LEFT, SWT.CENTER).hSpan(3).vIndent(10).applyTo(label);

        Button button = new Button(saveAreaComposite, SWT.RADIO);
        button.setText(Messages.getString("QuerySaveControl.ServerButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().align(SWT.LEFT, SWT.CENTER).hSpan(3).vIndent(getVerticalSpacing()).applyTo(
            button);
        if (saveMode == SaveMode.SERVER) {
            button.setSelection(true);
        }
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                saveModeSelected(SaveMode.SERVER);
            }
        });

        serverQueryLabel = new Label(saveAreaComposite, SWT.NONE);
        serverQueryLabel.setText(Messages.getString("QuerySaveControl.ServerQueryLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(20).applyTo(serverQueryLabel);

        serverQueryNameText = new Text(saveAreaComposite, SWT.READ_ONLY | SWT.BORDER);
        serverQueryNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        serverQueryNameText.setText(QueryFolderUtil.getHierarchicalPath(parentFolder));

        serverQueryBrowseButton = new Button(saveAreaComposite, SWT.NONE);
        serverQueryBrowseButton.setText(Messages.getString("QuerySaveControl.ServerBrowseButtonText")); //$NON-NLS-1$
        serverQueryBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseServer();
            }
        });

        button = new Button(saveAreaComposite, SWT.RADIO);
        button.setText(Messages.getString("QuerySaveControl.FileButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().align(SWT.LEFT, SWT.CENTER).hSpan(3).vIndent(getVerticalSpacing()).applyTo(
            button);
        if (saveMode == SaveMode.FILE) {
            button.setSelection(true);
        }
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                saveModeSelected(SaveMode.FILE);
            }
        });

        fileQueryLabel = new Label(saveAreaComposite, SWT.NONE);
        fileQueryLabel.setText(Messages.getString("QuerySaveControl.LocationLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(20).applyTo(fileQueryLabel);

        fileQueryText = new Text(saveAreaComposite, SWT.BORDER);
        fileQueryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fileQueryText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                if (!ignoreFileLocationChanges) {
                    fileLocationModified(((Text) e.widget).getText());
                }
            }
        });

        fileQueryBrowseButton = new Button(saveAreaComposite, SWT.NONE);
        fileQueryBrowseButton.setText(Messages.getString("QuerySaveControl.FileQueryBrowseButtonText")); //$NON-NLS-1$
        fileQueryBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseLocal();
            }
        });

        ButtonHelper.setButtonsToButtonBarSize(new Button[] {
            serverQueryBrowseButton,
            fileQueryBrowseButton
        });

        saveModeSelected(saveMode);
    }

    public boolean validate() {
        final Shell shell = getShell();

        if (queryName == null || queryName.trim().length() == 0) {
            MessageBoxHelpers.errorMessageBox(
                shell,
                Messages.getString("QuerySaveControl.InvalidQueryDialogTitle"), //$NON-NLS-1$
                Messages.getString("QuerySaveControl.QueryNameBlankErrorText")); //$NON-NLS-1$
            return false;
        }

        if (queryName.length() > 255) {
            MessageBoxHelpers.errorMessageBox(
                shell,
                Messages.getString("QuerySaveControl.InvalidQueryDialogTitle"), //$NON-NLS-1$
                Messages.getString("QuerySaveControl.QueryNameTooLongErrorText")); //$NON-NLS-1$
            return false;
        }

        if (!FileHelpers.isValidNTFSFileName(queryName)) {
            MessageBoxHelpers.errorMessageBox(
                shell,
                Messages.getString("QuerySaveControl.InvalidQueryDialogTitle"), //$NON-NLS-1$
                Messages.getString("QuerySaveControl.QueryNameHasInvalidChars")); //$NON-NLS-1$
            return false;
        }

        if (saveMode == SaveMode.SERVER && parentFolder.getParent() == null) {
            MessageBoxHelpers.errorMessageBox(
                shell,
                Messages.getString("QuerySaveControl.InvalidFolderDialogTitle"), //$NON-NLS-1$
                Messages.getString("QuerySaveControl.InvalidFolderDialogText")); //$NON-NLS-1$
            return false;
        }

        if (saveMode == SaveMode.FILE) {
            saveDirectory = new File(fileQueryText.getText()).getParentFile();
            if (getSaveLocation().exists()) {
                if (!MessageBoxHelpers.dialogYesNoPrompt(
                    shell,
                    Messages.getString("QuerySaveControl.OverwriteQueryDialogTitle"), //$NON-NLS-1$
                    Messages.getString("QuerySaveControl.OverwriteQueryDialogText"))) //$NON-NLS-1$
                {
                    return false;
                }

                overwriteExisting = true;
            }
        } else {
            if (parentFolder.containsName(queryName)) {
                if (!MessageBoxHelpers.dialogYesNoPrompt(
                    shell,
                    Messages.getString("QuerySaveControl.OverwriteQueryDialogTitle"), //$NON-NLS-1$
                    Messages.getString("QuerySaveControl.OverwriteQueryDialogText"))) //$NON-NLS-1$
                {
                    return false;
                }

                overwriteExisting = true;
            }
        }

        return true;
    }

    public File getSaveLocation() {
        return new File(saveDirectory, queryName + ".wiq"); //$NON-NLS-1$
    }

    public GUID getParentGUID() {
        return parentFolder.getID();
    }

    public Project getProject() {
        return parentFolder.getProject();
    }

    public String getQueryName() {
        return queryName;
    }

    public SaveMode getSaveMode() {
        return saveMode;
    }

    public boolean getOverwriteExisting() {
        return overwriteExisting;
    }

    private void browseServer() {
        final SelectQueryItemDialog queryFolderDialog = new SelectQueryItemDialog(
            getShell(),
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer(),
            projectCollection.getProjects(),
            parentFolder,
            QueryItemType.QUERY_FOLDER);

        if (queryFolderDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        parentFolder = (QueryFolder) queryFolderDialog.getSelectedQueryItem();
        serverQueryNameText.setText(QueryFolderUtil.getHierarchicalPath(parentFolder));
    }

    private void browseLocal() {
        final FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);

        dialog.setFilterNames(new String[] {
            Messages.getString("QuerySaveControl.FileDialogWiqFilter") //$NON-NLS-1$
        });
        dialog.setFilterExtensions(new String[] {
            "*.wiq" //$NON-NLS-1$
        });
        dialog.setFilterPath(saveDirectory.getAbsolutePath());
        dialog.setText(Messages.getString("QuerySaveControl.SaveQueryDialogTitle")); //$NON-NLS-1$
        dialog.setFileName(queryName);

        final String path = dialog.open();

        if (path != null) {
            final File file = new File(path);
            saveDirectory = file.getParentFile();

            String name = file.getName();
            if (name.indexOf(".") != -1) //$NON-NLS-1$
            {
                name = name.substring(0, name.lastIndexOf(".")); //$NON-NLS-1$
            }
            queryName = name;
            ignoreQueryNameChanges = true;
            queryNameText.setText(queryName);
            ignoreQueryNameChanges = false;

            setFileLocationText();
        }
    }

    private void queryNameChanged(final String newQueryName) {
        queryName = newQueryName;

        if (saveMode == SaveMode.FILE) {
            setFileLocationText();
        }
    }

    private void fileLocationModified(final String newFileLocation) {
        ignoreQueryNameChanges = true;

        if (newFileLocation.trim().length() == 0) {
            queryNameText.setText(""); //$NON-NLS-1$
        } else {
            final File file = new File(newFileLocation);
            String name = file.getName();
            if (name.indexOf(".") != -1) //$NON-NLS-1$
            {
                name = name.substring(0, name.lastIndexOf(".")); //$NON-NLS-1$
            }
            queryNameText.setText(name);
            queryName = name;
        }

        ignoreQueryNameChanges = false;
    }

    private void saveModeSelected(final SaveMode saveMode) {
        this.saveMode = saveMode;

        final boolean enableServer = (saveMode == SaveMode.SERVER);
        final boolean enableFile = (saveMode == SaveMode.FILE);

        serverQueryLabel.setEnabled(enableServer);
        serverQueryNameText.setEnabled(enableServer);
        serverQueryBrowseButton.setEnabled(enableServer);
        fileQueryLabel.setEnabled(enableFile);
        fileQueryText.setEnabled(enableFile);
        fileQueryBrowseButton.setEnabled(enableFile);

        if (saveMode == SaveMode.FILE) {
            setFileLocationText();
        } else {
            clearFileLocationText();
        }
    }

    private void clearFileLocationText() {
        ignoreFileLocationChanges = true;
        fileQueryText.setText(""); //$NON-NLS-1$
        ignoreFileLocationChanges = false;
    }

    private void setFileLocationText() {
        ignoreFileLocationChanges = true;
        fileQueryText.setText(new File(saveDirectory, queryName + ".wiq").getAbsolutePath()); //$NON-NLS-1$
        ignoreFileLocationChanges = false;
    }
}
