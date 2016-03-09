// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.prefs.ExternalCompareToolDialog;
import com.microsoft.tfs.client.common.ui.dialogs.prefs.ExternalToolDialog;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalToolAssociation;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.core.util.MementoRepository;

public class ComparePreferencePage extends ExternalToolPreferencePage {
    private final ExternalToolset toolset;

    public ComparePreferencePage() {
        super();

        toolset = ExternalToolset.loadFromMemento(
            new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                ExternalToolPreferenceKey.COMPARE_KEY));
    }

    private void save() {
        final XMLMemento memento = new XMLMemento(ExternalToolPreferenceKey.COMPARE_KEY);
        toolset.saveToMemento(memento);

        new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).save(
            ExternalToolPreferenceKey.COMPARE_KEY,
            memento);
    }

    @Override
    protected String getName() {
        return Messages.getString("ComparePreferencePage.PageName"); //$NON-NLS-1$
    }

    @Override
    protected boolean supportsDirectories() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ExternalToolset getToolset() {
        return toolset;
    }

    @Override
    protected boolean addPressed() {
        final ExternalCompareToolDialog configDialog = new ExternalCompareToolDialog(
            getShell(),
            new ExternalToolAssociation(null, null),
            true,
            toolset.getFileAssociations());

        if (configDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        toolset.addAssociation(configDialog.getNewAssociation());

        save();

        return true;
    }

    @Override
    protected boolean editPressed() {
        final ExternalToolAssociation selected = getSelection();

        if (selected == null) {
            return false;
        }

        final ExternalCompareToolDialog configDialog =
            new ExternalCompareToolDialog(getShell(), selected, false, toolset.getFileAssociations());
        if (configDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        toolset.remove(selected);
        toolset.addAssociation(configDialog.getNewAssociation());

        save();

        return true;
    }

    @Override
    protected boolean removePressed() {
        final ExternalToolAssociation selected = getSelection();

        if (selected == null) {
            return false;
        }

        final String extenstions = ExternalToolDialog.combineExtensions(selected.getExtensions());
        final String messageFormat = Messages.getString("ComparePreferencePage.ConfirmRemoveFilesDialogTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, extenstions);

        if (!MessageDialog.openConfirm(
            getShell(),
            Messages.getString("ComparePreferencePage.ConfirmRemoveDialogTitle"), //$NON-NLS-1$
            message)) {
            return false;
        }

        toolset.remove(selected);

        save();

        return true;
    }

    @Override
    protected boolean duplicatePressed() {
        final ExternalToolAssociation selected = getSelection();

        if (selected == null) {
            return false;
        }

        final ExternalCompareToolDialog configDialog =
            new ExternalCompareToolDialog(getShell(), selected, true, toolset.getFileAssociations());
        if (configDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        toolset.addAssociation(configDialog.getNewAssociation());

        save();

        return true;
    }

    @Override
    protected boolean dirAddPressed() {
        final ExternalCompareToolDialog configDialog =
            new ExternalCompareToolDialog(getShell(), new ExternalToolAssociation(new String[] {
                ExternalToolset.DIRECTORY_EXTENSION
        }, null), true, toolset.getFileAssociations());

        if (configDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        getToolset().addAssociation(configDialog.getNewAssociation());

        save();

        return true;
    }

    @Override
    protected boolean dirChangePressed() {
        final ExternalToolAssociation selected = getToolset().getDirectoryAssociation();

        final ExternalCompareToolDialog configDialog =
            new ExternalCompareToolDialog(getShell(), selected, false, toolset.getFileAssociations());
        if (configDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        getToolset().remove(selected);
        getToolset().addAssociation(configDialog.getNewAssociation());

        save();

        return true;
    }

    @Override
    protected boolean dirRemovePressed() {
        final ExternalToolAssociation selected = getToolset().getDirectoryAssociation();

        if (selected == null) {
            return false;
        }

        final String confirmation = Messages.getString("ComparePreferencePage.ConfirmRemoveDialogText"); //$NON-NLS-1$

        if (!MessageDialog.openConfirm(
            getShell(),
            Messages.getString("ComparePreferencePage.ConfirmRemoveDialogTitle"), //$NON-NLS-1$
            confirmation)) {
            return false;
        }

        getToolset().remove(selected);
        save();

        return true;
    }
}