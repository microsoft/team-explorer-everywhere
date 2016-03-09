// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.common;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.commands.QueryTeamProjectsCommand;
import com.microsoft.tfs.client.common.framework.command.ThreadedCancellableCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.connect.CrossCollectionProjectSelectControl;
import com.microsoft.tfs.client.common.ui.controls.connect.CrossCollectionProjectSelectControl.ProjectSelectionChangedEvent;
import com.microsoft.tfs.client.common.ui.controls.connect.CrossCollectionProjectSelectControl.ProjectSelectionChangedListener;
import com.microsoft.tfs.client.common.ui.controls.connect.CrossCollectionProjectTable.CrossCollectionProjectInfo;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

public class WizardCrossCollectionProjectSelectionPage extends WizardCrossCollectionSelectionPage {
    public static final String PAGE_NAME = "WizardCrossCollectionProjectSelectionPage"; //$NON-NLS-1$

    private CrossCollectionProjectSelectControl projectSelectControl;
    private final List<CrossCollectionProjectInfo> projects = new ArrayList<CrossCollectionProjectInfo>(100);

    public WizardCrossCollectionProjectSelectionPage() {
        super(
            PAGE_NAME,
            Messages.getString("WizardCrossCollectionSelectionPage.ProjectSelectionTitle"), //$NON-NLS-1$
            Messages.getString("WizardCrossCollectionSelectionPage.ProjectSelectionDescription")); //$NON-NLS-1$
    }

    @Override
    protected void createControls(
        final Composite container,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags) {
        projectSelectControl =
            new CrossCollectionProjectSelectControl(container, SWT.NONE, sourceControlCapabilityFlags);
        projectSelectControl.addListener(new ProjectSelectionChangedListener() {
            @Override
            public void onProjectSelectionChanged(final ProjectSelectionChangedEvent event) {
                final CrossCollectionProjectInfo info = projectSelectControl.getSelectedProject();
                if (info != null) {
                    setWizardData(info.getCollection(), info);
                } else {
                    removeWizardData(false);
                }

                setPageComplete(info != null);
            }
        });
        GridDataBuilder.newInstance().grab().fill().applyTo(projectSelectControl);
    }

    @Override
    protected boolean onPageFinished() {
        final CrossCollectionProjectInfo info = projectSelectControl.getSelectedProject();
        if (info != null) {
            setWizardData(info.getCollection(), info);
        }
        projectSelectControl.stopTimer();
        return info != null;
    }

    @Override
    protected void clearList() {
        projects.clear();
    }

    @Override
    protected void appendCollectionInformation(TFSTeamProjectCollection collection) {
        final QueryTeamProjectsCommand queryCommand2 = new QueryTeamProjectsCommand(collection);
        final IStatus status2 = getCommandExecutor().execute(new ThreadedCancellableCommand(queryCommand2));
        if (!status2.isOK()) {
            return;
        }
        final ProjectInfo[] projectInfos = queryCommand2.getProjects();
        for (final ProjectInfo info : projectInfos) {
            final CrossCollectionProjectInfo pi = new CrossCollectionProjectInfo(
                collection,
                info.getName(),
                info.getURI(),
                collection.getName(),
                collection.getBaseURI().getHost());
            projects.add(pi);
        }
    }

    @Override
    protected void refreshUI() {
        projectSelectControl.refresh(projects);
    }
}
