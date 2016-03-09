// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.ui.IWorkingSet;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.util.Check;

/**
 * An instance of ImportOptions contains selected options and context for an
 * import operation. An ImportOptions contains all the information needed to do
 * an import, excluding the set of folders to be imported.
 *
 * This class acts as a "dumb data holder" style class (ie not object-oriented
 * at all). During the process of the ImportWizard, various ImportOptions
 * properties are set. By the time finish is clicked, the ImportOptions object
 * should be completely filled out.
 */
public class ImportOptions {
    private boolean useNewProjectWizard = false;
    private boolean forceGet = false;

    private IWorkingSet workingSet = null;

    private IWorkspace eclipseWorkspace;
    private Workspace tfsWorkspace;
    private TFSRepository tfsRepository;
    private SourceControlCapabilityFlags capabilityFlags;
    private UsernamePasswordCredentials credentials;

    private final ImportFolderValidator folderValidator;

    private final ImportNewProjectAction newProjectAction;

    private List<String> importFolders = new ArrayList<String>();

    public ImportOptions(final ImportNewProjectAction newProjectAction) {
        Check.notNull(newProjectAction, "newProjectAction"); //$NON-NLS-1$

        folderValidator = new ImportFolderValidator(this);
        this.newProjectAction = newProjectAction;
    }

    public IWorkspace getEclipseWorkspace() {
        return eclipseWorkspace;
    }

    public void setEclipseWorkspace(final IWorkspace eclipseWorkspace) {
        this.eclipseWorkspace = eclipseWorkspace;
    }

    public boolean isUseNewProjectWizard() {
        return useNewProjectWizard;
    }

    public void setUseNewProjectWizard(final boolean showOpenAs) {
        useNewProjectWizard = showOpenAs;
    }

    public boolean isForceGet() {
        return forceGet;
    }

    public void setForceGet(final boolean forceGet) {
        this.forceGet = forceGet;
    }

    public Workspace getTFSWorkspace() {
        return tfsWorkspace;
    }

    public void setTFSWorkspace(final Workspace workspace) {
        tfsWorkspace = workspace;
    }

    public TFSRepository getTFSRepository() {
        return tfsRepository;
    }

    public void setTFSRepository(final TFSRepository repository) {
        tfsRepository = repository;
    }

    public ImportFolderValidator getFolderValidator() {
        return folderValidator;
    }

    public ImportNewProjectAction getNewProjectAction() {
        return newProjectAction;
    }

    public IWorkingSet getWorkingSet() {
        return workingSet;
    }

    public void setWorkingSet(final IWorkingSet workingSet) {
        this.workingSet = workingSet;
    }

    public String[] getImportFolders() {
        return importFolders.toArray(new String[importFolders.size()]);
    }

    public void setImportFolders(final String[] importFolders) {
        this.importFolders = Arrays.asList(importFolders);
    }

    public SourceControlCapabilityFlags getCapabilityFlags() {
        return capabilityFlags;
    }

    public void setCapabilityFlags(final SourceControlCapabilityFlags capabilityFlags) {
        this.capabilityFlags = capabilityFlags;
    }

    public UsernamePasswordCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(final UsernamePasswordCredentials credentials) {
        this.credentials = credentials;
    }
}