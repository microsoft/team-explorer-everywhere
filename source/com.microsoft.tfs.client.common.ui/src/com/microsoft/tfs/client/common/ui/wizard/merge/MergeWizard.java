// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.merge;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ServerPathFormatException;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * The wizard to take you through the merge process.
 */
public class MergeWizard extends Wizard {
    private final TFSRepository repository;
    private String sourcePath;
    private String targetPath;
    private VersionSpec fromVersion = null;
    private VersionSpec toVersion = LatestVersionSpec.INSTANCE;
    private MergeFlags mergeFlags = MergeFlags.NONE;

    private boolean isMapped = false;

    private SelectMergeSourceTargetWizardPage sourceTargetWizardPage;
    private SelectMergeTargetMappingWizardPage targetMappingWizardPage;
    private SelectMergeVersionWizardPage versionWizardPage;
    private SelectChangesetsWizardPage changesetsWizardPage;
    private MergeEndPage endPage;

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public static final int TEXT_CHARACTER_WIDTH = 100;

    public boolean isComplete = false;

    /**
     *
     */
    public MergeWizard(final TFSRepository repository, final String sourcePath) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.sourcePath = sourcePath;
        setDefaultPageImageDescriptor(imageHelper.getImageDescriptor("images/wizard/pageheader.png")); //$NON-NLS-1$
        setForcePreviousAndNextButtons(true);
    }

    @Override
    public void addPages() {
        sourceTargetWizardPage = new SelectMergeSourceTargetWizardPage(repository, sourcePath, imageHelper);
        addPage(sourceTargetWizardPage);

        targetMappingWizardPage = new SelectMergeTargetMappingWizardPage();
        addPage(targetMappingWizardPage);

        versionWizardPage = new SelectMergeVersionWizardPage();
        addPage(versionWizardPage);

        changesetsWizardPage = new SelectChangesetsWizardPage();
        addPage(changesetsWizardPage);

        endPage = new MergeEndPage();
        addPage(endPage);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.
     * IWizardPage)
     */
    @Override
    public IWizardPage getNextPage(final IWizardPage thisPage) {
        if (thisPage instanceof SelectMergeSourceTargetWizardPage) {
            sourcePath = sourceTargetWizardPage.getSourcePath();
            targetPath = sourceTargetWizardPage.getTargetPath();
            mergeFlags = sourceTargetWizardPage.getMergeFlags();

            try {
                isMapped = (repository.getWorkspace().getMappedLocalPath(targetPath) != null);
            } catch (final ServerPathFormatException e) {
            }

            if (isMapped && sourceTargetWizardPage.isSelectChangesetsSelected()) {
                changesetsWizardPage.setMergeSourceTarget(sourcePath, targetPath, mergeFlags);
                return changesetsWizardPage;
            } else if (isMapped) {
                return versionWizardPage;
            }
        } else if (thisPage instanceof SelectMergeTargetMappingWizardPage) {
            if (sourceTargetWizardPage.isSelectChangesetsSelected()) {
                changesetsWizardPage.setMergeSourceTarget(sourcePath, targetPath, mergeFlags);
                return changesetsWizardPage;
            }
        } else if (thisPage instanceof SelectMergeVersionWizardPage) {
            toVersion = versionWizardPage.getVersionSpec();
            return endPage;
        } else if (thisPage instanceof SelectChangesetsWizardPage) {
            fromVersion = changesetsWizardPage.getFromVersion();
            toVersion = changesetsWizardPage.getToVersion();
        }

        return super.getNextPage(thisPage);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#createPageControls(org.eclipse.swt.
     * widgets .Composite)
     */
    @Override
    public void createPageControls(final Composite pageContainer) {
        super.createPageControls(pageContainer);
        // ContextSensitiveHelp.setHelp(pageContainer,
        // TFSUIHelpContextIDs.MERGE_WIZARD);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#getWindowTitle()
     */
    @Override
    public String getWindowTitle() {
        return Messages.getString("MergeWizard.WizardTitle"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#isHelpAvailable()
     */
    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#canFinish()
     */
    @Override
    public boolean canFinish() {
        return isComplete && isMapped && sourcePath != null && targetPath != null && toVersion != null;
    }

    /**
     * @param isComplete
     *        the isComplete to set
     */
    public void setComplete(final boolean isComplete) {
        this.isComplete = isComplete;
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public VersionSpec getFromVersion() {
        return fromVersion;
    }

    public VersionSpec getToVersion() {
        return toVersion;
    }

    public MergeFlags getMergeFlags() {
        return mergeFlags;
    }

    @Override
    public void dispose() {
        imageHelper.dispose();
        super.dispose();
    }
}
