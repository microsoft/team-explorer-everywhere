// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;

public class ImportGitRepositoryCollection extends ImportItemCollectionBase {
    @Override
    protected ImportFolderValidation validate(final ImportItemBase selectedPath) {
        final ImportFolderValidation validation = super.validate(selectedPath);

        return validation;
    }

    final TFSTeamProjectCollection connection;

    public ImportGitRepositoryCollection(final TFSTeamProjectCollection connection) {
        super();
        this.connection = connection;
    }

    public ImportGitRepositoryCollection(final TFSTeamProjectCollection connection, final List<TypedServerItem> items) {
        this(connection);
        setItems(items);
    }

    @Override
    @Deprecated
    protected ImportItemBase getImportItem(final String itemPath) {
        Check.isTrue(
            false,
            "The method ImportItemBase is deprecated, getImportItem(TypedServerItem item) should be used instead"); //$NON-NLS-1$
        return null;
    }

    @Override
    protected ImportItemBase getImportItem(final TypedServerItem item) {
        Check.isTrue(
            item instanceof TypedServerGitRepository,
            "Item is not an instance of the TypedServerGitRepository class"); //$NON-NLS-1$
        final TypedServerGitRepository repositoryItem = (TypedServerGitRepository) item;

        final ImportGitRepository repo = new ImportGitRepository(repositoryItem.getServerPath());
        repo.setRepositroyJson(repositoryItem.getJson());

        return repo;
    }

    public ImportGitRepository[] getRepositories() {
        final ImportItemBase[] items = getItems();
        final List<ImportGitRepository> repositories = new ArrayList<ImportGitRepository>();

        for (final ImportItemBase item : items) {
            repositories.add((ImportGitRepository) item);
        }

        java.util.Collections.sort(repositories);
        return repositories.toArray(new ImportGitRepository[repositories.size()]);
    }
}
