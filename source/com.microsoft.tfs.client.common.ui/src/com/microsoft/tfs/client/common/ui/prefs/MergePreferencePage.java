// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.dialogs.prefs.ExternalMergeToolDialog;
import com.microsoft.tfs.client.common.ui.dialogs.prefs.ExternalToolDialog;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalToolAssociation;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.core.util.MementoRepository;

public class MergePreferencePage extends ExternalToolPreferencePage {
    private final ExternalToolset toolset;

    public MergePreferencePage() {
        super();

        toolset = ExternalToolset.loadFromMemento(
            new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                ExternalToolPreferenceKey.MERGE_KEY));
    }

    private void save() {
        final XMLMemento memento = new XMLMemento(ExternalToolPreferenceKey.MERGE_KEY);
        toolset.saveToMemento(memento);

        new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).save(
            ExternalToolPreferenceKey.MERGE_KEY,
            memento);
    }

    @Override
    protected String getName() {
        return Messages.getString("MergePreferencePage.PageName"); //$NON-NLS-1$
    }

    @Override
    protected ExternalToolset getToolset() {
        return toolset;
    }

    @Override
    protected boolean addPressed() {
        final ExternalMergeToolDialog configDialog = new ExternalMergeToolDialog(
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

        final ExternalMergeToolDialog configDialog =
            new ExternalMergeToolDialog(getShell(), selected, false, toolset.getFileAssociations());

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

        final String extensions = ExternalToolDialog.combineExtensions(selected.getExtensions());
        final String messageFormat = Messages.getString("MergePreferencePage.ConfirmRemoveDialogTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, extensions);

        if (!MessageDialog.openConfirm(
            getShell(),
            Messages.getString("MergePreferencePage.ConfirmRemoveDialogTitle"), //$NON-NLS-1$
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

        final ExternalMergeToolDialog configDialog =
            new ExternalMergeToolDialog(getShell(), selected, true, toolset.getFileAssociations());

        if (configDialog.open() != IDialogConstants.OK_ID) {
            return false;
        }

        toolset.addAssociation(configDialog.getNewAssociation());

        save();

        return true;
    }

}