// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.ImportWizard;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepository;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportGitRepositoryCollection;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportItemCollectionBase;
import com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support.ImportOptions;
import com.microsoft.tfs.core.ServerCapabilities;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringHelpers;
import com.microsoft.tfs.util.StringUtil;

public class GitImportWizardCloneParametersPage extends ExtendedWizardPage {
    private static final Log log = LogFactory.getLog(GitImportWizardCloneParametersPage.class);

    public static final String PAGE_NAME = "GitImportWizardCloneParametersPage"; //$NON-NLS-1$

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    private static final String EGIT_PREF_STORE_ID = "org.eclipse.egit.ui"; //$NON-NLS-1$

    private static final String DEFAULT_REPOSITORY_DIR_KEY = "default_repository_dir"; //$NON-NLS-1$

    private Button defaultGitRepositoryDirectoryButton;
    private Button customWorkspaceDirectoryButton;
    private Text workingDirectoryText;
    private Button workingDirectorySelectButton;
    private Text remoteNameText;
    private Button cloneSubmodulesButton;

    private Button useGeneratedCredentialsButton;
    private Button useAlternativeCredentialsButton;
    private boolean credentialsSelectionChanged;

    private Text userNameText;
    private Text passwordText;
    private Button savePasswordButton;

    private String userName;
    private String password;
    private boolean savePassword;

    Font italicFont = null;
    Font boldFont = null;

    private ImportGitRepository[] repositories;

    public GitImportWizardCloneParametersPage() {
        super(
            PAGE_NAME,
            Messages.getString("GitImportWizardCloneParametersPage.PageTitle"), //$NON-NLS-1$
            Messages.getString("GitImportWizardCloneParametersPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final ImportGitRepositoryCollection itemCollection =
            (ImportGitRepositoryCollection) getExtendedWizard().getPageData(ImportItemCollectionBase.class);
        repositories = itemCollection.getRepositories();

        final Composite container = new Composite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        createLocationSettingsControls(container);
        createRepositorySettingsControls(container);
        createAuthenticationSettingsControl(container);

        refresh();
    }

    private void createLocationSettingsControls(final Composite container) {
        final Composite locationSettingsContainer = new Composite(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(locationSettingsContainer);

        final GridLayout commonSettingsLayout = new GridLayout(2, false);
        commonSettingsLayout.horizontalSpacing = getHorizontalSpacing();
        commonSettingsLayout.verticalSpacing = 0; // getVerticalSpacing();
        commonSettingsLayout.marginWidth = 0; // getHorizontalMargin();
        commonSettingsLayout.marginHeight = 0; // getVerticalMargin();

        locationSettingsContainer.setLayout(commonSettingsLayout);

        final Group commonSettingsGroup = new Group(locationSettingsContainer, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(commonSettingsGroup);

        final GridLayout commonSettingsGroupLayout = new GridLayout(3, false);
        commonSettingsGroupLayout.horizontalSpacing = getHorizontalSpacing();
        commonSettingsGroupLayout.verticalSpacing = getVerticalSpacing();
        commonSettingsGroupLayout.marginWidth = getHorizontalMargin();
        commonSettingsGroupLayout.marginHeight = getVerticalMargin();

        commonSettingsGroup.setLayout(commonSettingsGroupLayout);
        commonSettingsGroup.setText(Messages.getString("GitImportWizardCloneParametersPage.LocationSettingsText")); //$NON-NLS-1$

        defaultGitRepositoryDirectoryButton = new Button(commonSettingsGroup, SWT.RADIO);
        defaultGitRepositoryDirectoryButton.setText(
            Messages.getString("GitImportWizardCloneParametersPage.EclipseWorkspaceDirectoryText")); //$NON-NLS-1$
        defaultGitRepositoryDirectoryButton.setToolTipText(
            Messages.getString("GitImportWizardCloneParametersPage.EclipseWorkspaceDirectoryTooltip")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).hFill().hGrab().applyTo(defaultGitRepositoryDirectoryButton);

        defaultGitRepositoryDirectoryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setWorkingDirectoryControls(false);
                checkCommonCloneParameters(getDefaultGitRootFolder(), remoteNameText.getText());
            }
        });

        customWorkspaceDirectoryButton = new Button(commonSettingsGroup, SWT.RADIO);
        customWorkspaceDirectoryButton.setText(Messages.getString("GitImportWizardCloneParametersPage.LocationText")); //$NON-NLS-1$
        customWorkspaceDirectoryButton.setToolTipText(
            Messages.getString("GitImportWizardCloneParametersPage.LocationTooltip")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().applyTo(customWorkspaceDirectoryButton);

        customWorkspaceDirectoryButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                setWorkingDirectoryControls(true);
                checkCommonCloneParameters(workingDirectoryText.getText(), remoteNameText.getText());
            }
        });

        workingDirectoryText = new Text(commonSettingsGroup, SWT.BORDER);
        workingDirectoryText.setText(getDefaultGitRootFolder());
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(workingDirectoryText);

        workingDirectoryText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                checkCommonCloneParameters(workingDirectoryText.getText(), remoteNameText.getText());
            }
        });

        workingDirectorySelectButton = new Button(commonSettingsGroup, SWT.PUSH);
        workingDirectorySelectButton.setText(Messages.getString("GitImportWizardCloneParametersPage.BrowseText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().wButtonHint(workingDirectorySelectButton).applyTo(workingDirectorySelectButton);

        workingDirectorySelectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final DirectoryDialog dirDialog = new DirectoryDialog(getShell());
                dirDialog.setMessage(
                    Messages.getString("GitImportWizardCloneParametersPage.SelectRootDirectoryMessageText")); //$NON-NLS-1$
                dirDialog.setFilterPath(workingDirectoryText.getText());
                final String selectedDir = dirDialog.open();

                if (!StringHelpers.isNullOrEmpty(selectedDir)) {
                    final String path = new Path(selectedDir).toOSString();
                    workingDirectoryText.setText(path);
                }
            }
        });

        defaultGitRepositoryDirectoryButton.setSelection(true);
        setWorkingDirectoryControls(false);
    }

    private void createRepositorySettingsControls(final Composite container) {
        final Composite repositorySettingsContainer = new Composite(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(repositorySettingsContainer);

        final GridLayout commonSettingsLayout = new GridLayout(2, false);
        commonSettingsLayout.horizontalSpacing = getHorizontalSpacing();
        commonSettingsLayout.verticalSpacing = 0; // getVerticalSpacing();
        commonSettingsLayout.marginWidth = 0; // getHorizontalMargin();
        commonSettingsLayout.marginHeight = 0; // getVerticalMargin();

        repositorySettingsContainer.setLayout(commonSettingsLayout);

        final Group commonSettingsGroup = new Group(repositorySettingsContainer, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(commonSettingsGroup);

        final GridLayout commonSettingsGroupLayout = new GridLayout(3, false);
        commonSettingsGroupLayout.horizontalSpacing = getHorizontalSpacing();
        commonSettingsGroupLayout.verticalSpacing = getVerticalSpacing();
        commonSettingsGroupLayout.marginWidth = getHorizontalMargin();
        commonSettingsGroupLayout.marginHeight = getVerticalMargin();

        commonSettingsGroup.setLayout(commonSettingsGroupLayout);
        commonSettingsGroup.setText(Messages.getString("GitImportWizardCloneParametersPage.RepositorySettingsText")); //$NON-NLS-1$

        final Label remoteNameLabel = new Label(commonSettingsGroup, SWT.NONE);
        remoteNameLabel.setText(Messages.getString("GitImportWizardCloneParametersPage.RemoteName")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().applyTo(remoteNameLabel);

        remoteNameText = new Text(commonSettingsGroup, SWT.BORDER);
        remoteNameText.setText("origin"); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(remoteNameText);

        remoteNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                checkCommonCloneParameters(workingDirectoryText.getText(), remoteNameText.getText());
            }
        });

        final Label placeholder = new Label(commonSettingsGroup, SWT.NONE);
        GridDataBuilder.newInstance().applyTo(placeholder);

        final Label cloneSubmodulesLabel = new Label(commonSettingsGroup, SWT.NONE);
        cloneSubmodulesLabel.setText(Messages.getString("GitImportWizardCloneParametersPage.CloneSubmodulesText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().applyTo(cloneSubmodulesLabel);

        cloneSubmodulesButton = new Button(commonSettingsGroup, SWT.CHECK);
        GridDataBuilder.newInstance().applyTo(cloneSubmodulesButton);
    }

    private void setWorkingDirectoryControls(final boolean enabled) {
        workingDirectoryText.setEnabled(enabled);
        workingDirectorySelectButton.setEnabled(enabled);
    }

    private String getDefaultGitRootFolder() {
        final InstanceScope scope = new InstanceScope();
        final IEclipsePreferences prefs = scope.getNode(EGIT_PREF_STORE_ID);

        Check.notNull(prefs, "Egit preferences store"); //$NON-NLS-1$

        String workingDirectory = prefs.get(DEFAULT_REPOSITORY_DIR_KEY, null);

        // If the preference is not set then use the home environment variable
        if (StringHelpers.isNullOrEmpty(workingDirectory)) {
            workingDirectory = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.HOME);

            // If the home envornment variable is not set then use the user
            // profile (the same logic as egit)
            if (StringHelpers.isNullOrEmpty(workingDirectory)) {
                workingDirectory =
                    PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.USER_PROFILE);
            }
        }

        return workingDirectory;
    }

    private void initCloneParameters() {
        setDefaultCloneParameters(false);
    }

    private void setDefaultCloneParameters(final boolean overrideOldValues) {
        final String workingDirectoryRoot = defaultGitRepositoryDirectoryButton.getSelection()
            ? getDefaultGitRootFolder() : workingDirectoryText.getText();
        final String remoteName = remoteNameText.getText();
        final boolean cloneSubmodules = cloneSubmodulesButton.getSelection();

        checkCommonCloneParameters(workingDirectoryRoot, remoteName);

        for (final ImportGitRepository repository : repositories) {
            if (overrideOldValues || StringHelpers.isNullOrEmpty(repository.getWorkingDirectory())) {
                final String workingDirectory =
                    new Path(workingDirectoryRoot).append(repository.getName()).toOSString();

                repository.setWorkingDirectory(workingDirectory);
                repository.setRemoteName(remoteName);
                repository.setCloneSubmodules(cloneSubmodules);
            }
        }

        setPageComplete();
    }

    private void checkCommonCloneParameters(final String workingDirectoryRoot, final String remoteName) {
        final StringBuilder sb = new StringBuilder();

        if (!isWorkingDirectoryRootValid(workingDirectoryRoot)) {
            sb.append(getErrorMessage());
        }

        if (!isRemoteNameValid(remoteName)) {
            if (sb.length() != 0) {
                sb.append(NEWLINE);
            }

            sb.append(getErrorMessage());
        }

        if (sb.length() != 0) {
            setErrorMessage(sb.toString());
        } else {
            setErrorMessage(null);
        }

        setPageComplete();
    }

    private void createAuthenticationSettingsControl(final Composite container) {
        final Group credentialsGroup = new Group(container, SWT.NONE);
        credentialsGroup.setText(Messages.getString("GitImportWizardCloneParametersPage.AuthenticationSettingsText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hSpan(2).hFill().applyTo(credentialsGroup);

        final GridLayout credentialsLayout = new GridLayout(2, false);
        credentialsLayout.marginWidth = getHorizontalMargin();
        credentialsLayout.marginHeight = getVerticalMargin();
        credentialsLayout.horizontalSpacing = getHorizontalSpacing();
        credentialsLayout.verticalSpacing = getVerticalSpacing();
        credentialsGroup.setLayout(credentialsLayout);

        createCredentialsOptions(credentialsGroup);
        createCredentialsFields(credentialsGroup);
        createCredentialsSaveOption(credentialsGroup);

        final Link alternateCredentialsRequiredLabel = new Link(credentialsGroup, 0);
        alternateCredentialsRequiredLabel.setText(
            Messages.getString("GitImportWizardCloneParametersPage.AlternativeCredentialsRequiredMessage")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().applyTo(alternateCredentialsRequiredLabel);

        alternateCredentialsRequiredLabel.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                super.widgetSelected(e);
                final URI uri = URIUtils.newURI("http://go.microsoft.com/fwlink/?LinkID=327527"); //$NON-NLS-1$
                BrowserFacade.launchURL(uri, null, null, null, LaunchMode.EXTERNAL);
            }

        });

        credentialsSelectionChanged = false;
        setDefaultCredentialsSelection();
    }

    private void createCredentialsOptions(final Composite container) {
        useGeneratedCredentialsButton = new Button(container, SWT.RADIO);
        useGeneratedCredentialsButton.setText(
            Messages.getString("GitImportWizardCloneParametersPage.UseLoginCredentialsText")); //$NON-NLS-1$
        useGeneratedCredentialsButton.setToolTipText(
            Messages.getString("GitImportWizardCloneParametersPage.UseLoginCredentialsTooltip")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().applyTo(useGeneratedCredentialsButton);

        useGeneratedCredentialsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                super.widgetSelected(e);
                setCredentialControls(false);
                credentialsSelectionChanged = true;
            }
        });

        useAlternativeCredentialsButton = new Button(container, SWT.RADIO);
        useAlternativeCredentialsButton.setText(
            Messages.getString("GitImportWizardCloneParametersPage.UseAlternativeCredentialsText")); //$NON-NLS-1$
        useAlternativeCredentialsButton.setToolTipText(
            Messages.getString("GitImportWizardCloneParametersPage.UseAlternativeCredentialsTooltip")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().applyTo(useAlternativeCredentialsButton);

        useAlternativeCredentialsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                super.widgetSelected(e);
                setCredentialControls(true);
                credentialsSelectionChanged = true;
                loadStoredCredentials();
            }
        });
    }

    private void createCredentialsFields(final Composite container) {
        final Label userNameLabel =
            SWTUtil.createLabel(container, SWT.NONE, Messages.getString("GitImportWizardCloneParametersPage.UserText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(15).hFill().applyTo(userNameLabel);

        userNameText = new Text(container, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(userNameText);

        userNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                userName = userNameText.getText();
                setPageComplete();
            }
        });

        final Label passwordLabel = SWTUtil.createLabel(
            container,
            SWT.NONE,
            Messages.getString("GitImportWizardCloneParametersPage.PasswordText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(15).hFill().applyTo(passwordLabel);

        passwordText = new Text(container, SWT.PASSWORD | SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(passwordText, 16).applyTo(passwordText);

        passwordText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                password = passwordText.getText();
                setPageComplete();
            }
        });
    }

    private void createCredentialsSaveOption(final Composite container) {
        final Label savePasswordLabel = SWTUtil.createLabel(
            container,
            SWT.NONE,
            Messages.getString("GitImportWizardCloneParametersPage.SavePasswordText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hIndent(15).hFill().applyTo(savePasswordLabel);

        savePasswordButton = new Button(container, SWT.CHECK);
        GridDataBuilder.newInstance().applyTo(savePasswordButton);

        savePasswordButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                savePassword = savePasswordButton.getSelection();
            }
        });
    }

    private void setCredentialControls(final boolean enabled) {
        userNameText.setEnabled(enabled);
        passwordText.setEnabled(enabled);
        savePasswordButton.setEnabled(enabled);

        setPageComplete();
    }

    private void setDefaultCredentialsSelection() {
        if (!credentialsSelectionChanged) {
            final ImportWizard wizard = getImportWizard();
            final TFSTeamProjectCollection connection =
                (TFSTeamProjectCollection) wizard.getPageData(TFSTeamProjectCollection.class);
            final boolean isHosted = connection.getServerCapabilities().contains(ServerCapabilities.HOSTED);

            useGeneratedCredentialsButton.setSelection(!isHosted);
            useAlternativeCredentialsButton.setSelection(isHosted);
            setCredentialControls(isHosted);

            if (isHosted) {
                loadStoredCredentials();
            }
        }
    }

    private void loadStoredCredentials() {
        final ImportWizard wizard = getImportWizard();
        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) wizard.getPageData(TFSTeamProjectCollection.class);

        final CachedCredentials cachedCredentials =
            EclipseCredentialsManagerFactory.getGitCredentialsManager().getCredentials(connection.getBaseURI());
        if (cachedCredentials != null) {
            userNameText.setText(cachedCredentials.getUsername());
            if (cachedCredentials.getPassword() != null) {
                passwordText.setText(cachedCredentials.getPassword());
            } else {
                passwordText.setText(StringUtil.EMPTY);
            }
        }
    }

    public boolean isValidCredentialsInfo() {
        if (getErrorMessage() != null) {
            return false;
        }

        if (useAlternativeCredentialsButton.getSelection()
            && (StringHelpers.isNullOrEmpty(password) || StringHelpers.isNullOrEmpty(userName))) {
            return false;
        }

        return true;
    }

    private void setPageComplete() {
        setPageComplete(isValid());
    }

    public boolean isValid() {
        return getErrorMessage() == null && isValidCredentialsInfo();
    }

    @Override
    public void refresh() {
        setErrorMessage(null);

        final ImportGitRepositoryCollection itemCollection =
            (ImportGitRepositoryCollection) getExtendedWizard().getPageData(ImportItemCollectionBase.class);

        repositories = itemCollection.getRepositories();
        initCloneParameters();

        setPageComplete();
    }

    private boolean isWorkingDirectoryRootValid(final String path) {
        if (repositories == null) {
            return true;
        }

        if (isWorkingDirectoryValid(path)) {
            for (final ImportGitRepository repository : repositories) {
                final File workingDirectory = new File(path, repository.getName());
                if (workingDirectory.exists() && workingDirectory.list().length != 0) {
                    final String messageFormat =
                        Messages.getString("GitImportWizardCloneParametersPage.FolderIsNotEmptyFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, workingDirectory.getAbsolutePath());

                    setErrorMessage(message);
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isWorkingDirectoryValid(final String path) {
        return isWorkingDirectoryValid(path, true);
    }

    private boolean isWorkingDirectoryValid(final String path, final boolean mustExist) {
        try {
            final File workingFolder = new File(path);
            if (mustExist && !workingFolder.exists()) {
                final String messageFormat =
                    Messages.getString("GitImportWizardCloneParametersPage.FolderDoesNotExistsFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, path);

                setErrorMessage(message);
                return false;
            }

            if (workingFolder.exists() && (!workingFolder.isDirectory() || workingFolder.isHidden())) {
                final String messageFormat =
                    Messages.getString("GitImportWizardCloneParametersPage.WrongFolderPathFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, path);

                setErrorMessage(message);
                return false;
            }

            if (!mustExist && !isWorkingDirectoryValid(LocalPath.getParent(path))) {
                final String messageFormat =
                    Messages.getString("GitImportWizardCloneParametersPage.WrongFolderPathFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, LocalPath.getParent(path));

                setErrorMessage(message);
                return false;
            }

            return true;
        } catch (final Exception e) {
            final String messageFormat =
                Messages.getString("GitImportWizardCloneParametersPage.ErrorCheckingFolderPathFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, path);

            log.error(message, e);
            setErrorMessage(message);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean onPageFinished() {
        setDefaultCloneParameters(true);

        final ImportOptions options = (ImportOptions) getImportWizard().getPageData(ImportOptions.class);
        options.setCredentials(getCredentials());

        return isValid();
    }

    private boolean isRemoteNameValid(final String remoteName) {
        if (!Repository.isValidRefName(Constants.R_REMOTES + remoteName)) {
            final String messageFormat = Messages.getString("GitImportWizardCloneParametersPage.WrongRemoteNameFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, remoteName);

            setErrorMessage(message);
            return false;
        } else {
            return true;
        }
    }

    private UsernamePasswordCredentials getCredentials() {
        final ImportWizard wizard = getImportWizard();
        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) wizard.getPageData(TFSTeamProjectCollection.class);

        if (useAlternativeCredentialsButton.getSelection()) {
            final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);

            if (savePassword) {
                final CachedCredentials cachedCredentials =
                    new CachedCredentials(connection.getBaseURI(), userName, password);
                if (!EclipseCredentialsManagerFactory.getGitCredentialsManager().setCredentials(cachedCredentials)) {
                    MessageDialog.openError(
                        getShell(),
                        Messages.getString("GitImportWizardCloneParametersPage.StroreCredentialsFailedTitle"), //$NON-NLS-1$
                        Messages.getString("GitImportWizardCloneParametersPage.StoreCredentialsFailedMessageText")); //$NON-NLS-1$
                }
            }

            return credentials;
        } else {
            final CachedCredentials cachedCredentials =
                EclipseCredentialsManagerFactory.getGitCredentialsManager().getCredentials(connection.getBaseURI());

            if (cachedCredentials != null) {
                return new UsernamePasswordCredentials(
                    cachedCredentials.getUsername(),
                    cachedCredentials.getPassword());
            } else {
                return null;
            }
        }
    }

    private ImportWizard getImportWizard() {
        return (ImportWizard) getExtendedWizard();
    }

    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);

        if (visible) {
            setDefaultCredentialsSelection();
            setPageComplete();
        }
    }
}
