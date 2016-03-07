// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.eclipse.ui.egit.importwizard.CrossCollectionRepositoryTable.CrossCollectionRepositoryInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringHelpers;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class CrossCollectionRepositorySelectControl extends BaseControl {
    private static final Log log = LogFactory.getLog(CrossCollectionRepositorySelectControl.class);

    public static final String REPO_TABLE_ID = "CrossCollectionRepositorySelectControl.repoTable"; //$NON-NLS-1$

    private static final String EGIT_PREF_STORE_ID = "org.eclipse.egit.ui"; //$NON-NLS-1$

    private static final String DEFAULT_REPOSITORY_DIR_KEY = "default_repository_dir"; //$NON-NLS-1$

    private final Text filterBox;
    private final Timer filterTimer;
    private FilterTask filterTask;

    private final CrossCollectionRepositoryTable table;
    private final Text parentDirectoryBox;
    private final Button browseButton;
    private final Text folderNameBox;
    private boolean ignoreChanges = false;

    private final SingleListenerFacade listeners = new SingleListenerFacade(RepositorySelectionChangedListener.class);

    public CrossCollectionRepositorySelectControl(
        final Composite parent,
        final int style,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags) {
        super(parent, style);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = 0;
        setLayout(layout);

        // Create timer for filtering the list
        filterTimer = new Timer(false);

        // Create the filter text box
        filterBox = new Text(this, SWT.BORDER);
        filterBox.setMessage(Messages.getString("CrossCollectionRepositorySelectControl.FilterHintText")); //$NON-NLS-1$
        filterBox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                startTimer();
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(filterBox);

        table = new CrossCollectionRepositoryTable(this, SWT.NONE);
        table.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                onSelectionChanged(true);
            }
        });
        GridDataBuilder.newInstance().grab().hHint(getVerticalSpacing() * 30).vIndent(
            getVerticalSpacing()).fill().applyTo(table);

        Label parentDirectoryLabel = SWTUtil.createLabel(
            this,
            Messages.getString("CrossCollectionRepositorySelectControl.ParentDirectoryLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().vIndent(getVerticalSpacing()).hFill().applyTo(parentDirectoryLabel);

        final Composite container = new Composite(this, SWT.NONE);
        final GridLayout layout2 = SWTUtil.gridLayout(container, 2, false, 0, 0);
        layout2.marginWidth = 0;
        layout2.marginHeight = 0;
        layout2.horizontalSpacing = getHorizontalSpacing();
        layout2.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout2);

        parentDirectoryBox = new Text(container, SWT.BORDER);
        parentDirectoryBox.setText(getDefaultGitRootFolder());
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(parentDirectoryBox);
        parentDirectoryBox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                onSelectionChanged(false);
            }
        });

        browseButton = SWTUtil.createButton(
            container,
            Messages.getString("CrossCollectionRepositorySelectControl.BrowseButtonText")); //$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                parentDirectoryBox.setText(browse(parentDirectoryBox.getText()));
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(container);

        Label folderNameLabel = SWTUtil.createLabel(
            this,
            Messages.getString("CrossCollectionRepositorySelectControl.RepositoryFolderLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().vIndent(getVerticalSpacing()).hFill().applyTo(folderNameLabel);

        folderNameBox = new Text(this, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(folderNameBox);
        folderNameBox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                onSelectionChanged(false);
            }
        });
    }

    private void onSelectionChanged(boolean updateFolderName) {
        if (ignoreChanges) {
            return;
        }

        ignoreChanges = true;
        try {
            final CrossCollectionRepositoryInfo repo = table.getSelectedRepository();
            if (updateFolderName) {
                String newFolderName = ""; //$NON-NLS-1$
                if (repo != null) {
                    newFolderName = repo.getRepositoryName();
                }
                folderNameBox.setText(newFolderName);
            }

            // Fire event to let listeners know that something changed
            ((RepositorySelectionChangedListener) listeners.getListener()).onRepositorySelectionChanged(
                new RepositorySelectionChangedEvent(repo));
        } finally {
            ignoreChanges = false;
        }
    }

    private void startTimer() {
        // Cancel any existing task
        if (filterTask != null) {
            filterTask.cancel();
        }
        // Create a new task
        filterTask = new FilterTask();
        // Schedule the task
        filterTimer.schedule(filterTask, 400);
    }

    // Call this when the owning control is going away
    public void stopTimer() {
        // Cancel any existing task
        if (filterTask != null) {
            filterTask.cancel();
        }
    }

    private class FilterTask extends TimerTask {
        @Override
        public void run() {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    table.applyFilter(filterBox.getText());
                }
            });
        }
    }

    private String getDefaultGitRootFolder() {
        final IScopeContext scope = new InstanceScope();
        final IEclipsePreferences prefs = scope.getNode(EGIT_PREF_STORE_ID);

        Check.notNull(prefs, "Egit preferences store"); //$NON-NLS-1$

        String workingDirectory = prefs.get(DEFAULT_REPOSITORY_DIR_KEY, null);

        // If the preference is not set then use the home environment variable
        if (StringHelpers.isNullOrEmpty(workingDirectory)) {
            workingDirectory = PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.HOME);

            // If the home environment variable is not set then use the user
            // profile (the same logic as eGit)
            if (StringHelpers.isNullOrEmpty(workingDirectory)) {
                workingDirectory =
                    PlatformMiscUtils.getInstance().getEnvironmentVariable(EnvironmentVariables.USER_PROFILE);
            }
        }

        return workingDirectory;
    }

    private String browse(final String currentDirectory) {
        final DirectoryDialog dlg = new DirectoryDialog(this.getShell());
        dlg.setFilterPath(currentDirectory);
        dlg.setText(Messages.getString("CrossCollectionRepositorySelectControl.BrowseDialogTitle")); //$NON-NLS-1$
        dlg.setMessage(Messages.getString("CrossCollectionRepositorySelectControl.BrowseDialogMessage")); //$NON-NLS-1$
        final String newDirectory = dlg.open();
        return newDirectory != null ? newDirectory : currentDirectory;
    }

    public CrossCollectionRepositoryInfo getSelectedRepository() {
        return table.getSelectedRepository();
    }

    public void setSelectedRepository(CrossCollectionRepositoryInfo repository) {
        table.setSelectedElement(repository);
    }
    
    public String getWorkingDirectory() {
        final String parentDirectory = parentDirectoryBox.getText();
        final String folderName = folderNameBox.getText();
        if (!StringHelpers.isNullOrEmpty(parentDirectory) && !StringHelpers.isNullOrEmpty(folderName)) {
            final File parent = new File(parentDirectory.trim());
            final File workingDirectory = new File(parent, folderName.trim());
            return workingDirectory.getPath();
        }
        return null;
    }

    public void addListener(final RepositorySelectionChangedListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final RepositorySelectionChangedListener listener) {
        listeners.removeListener(listener);
    }

    public void refresh(final List<CrossCollectionRepositoryInfo> repos) {
        if (repos == null) {
            table.setRepositories(null);
            return;
        }
        table.setRepositories(repos.toArray(new CrossCollectionRepositoryInfo[repos.size()]));
    }

    public interface RepositorySelectionChangedListener {
        public void onRepositorySelectionChanged(RepositorySelectionChangedEvent event);
    }

    public final class RepositorySelectionChangedEvent {
        private final CrossCollectionRepositoryInfo selectedRepository;

        private RepositorySelectionChangedEvent(final CrossCollectionRepositoryInfo selectedRepository) {
            this.selectedRepository = selectedRepository;
        }

        public CrossCollectionRepositoryInfo getSelectedRepository() {
            return selectedRepository;
        }
    }
}
