// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.controls.FilesControl;
import com.microsoft.tfs.core.clients.build.GitProperties;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDefinitionSourceProvider;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;

public abstract class LocalBuildFileSelectionPage extends WizardPage {
    private FilesControl filesControl;
    private final IBuildDefinition buildDefinition;
    private final IBuildDefinitionSourceProvider sourceProvider;
    private final String repoPath;

    public LocalBuildFileSelectionPage(
        final String pageName,
        final IBuildDefinition buildDefinition,
        final String title,
        final String description) {
        super(pageName, title, null);
        setDescription(description);
        this.buildDefinition = buildDefinition;
        this.sourceProvider = buildDefinition.getDefaultSourceProvider();
        this.repoPath = sourceProvider.getValueByName(GitProperties.LocalRepoPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 2, false);
        GridDataBuilder.newInstance().fill().grab().applyTo(composite);
        setControl(composite);

        filesControl = new FilesControl(composite, SWT.SINGLE, new BuildFileFilter());
        filesControl.setInitialPath(repoPath, true);
        GridDataBuilder.newInstance().hSpan(2).fill().grab().applyTo(filesControl);
    }

    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public String getBuildFileRelativePath() {
        // TODO: add checking make sure selected build file is inside repo
        final File[] selectedFiles = filesControl.getSelectedFiles();
        if (selectedFiles == null || selectedFiles.length == 0) {
            return null;
        }

        final String filePath = selectedFiles[0].getAbsolutePath();
        return LocalPath.makeRelative(filePath, repoPath);
    }

    public String getBuildFileSuffix() {
        return ".xml"; //$NON-NLS-1$
    }

    public abstract String getBuildFileLabel();

    final class BuildFileFilter implements FilenameFilter {
        @Override
        public boolean accept(final File dir, final String name) {
            return new File(dir, name).isDirectory() || name.endsWith(getBuildFileSuffix());
        }
    }
}
