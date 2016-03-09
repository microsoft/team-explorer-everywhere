// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.category;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.workitem.category.Category;
import com.microsoft.tfs.core.clients.workitem.category.CategoryCollection;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeCategoryMetadata;

public class CategoryCollectionImpl implements CategoryCollection {
    private final List<Category> categories;

    /**
     * Constructor. Retrieves category data from the metadata and produces
     * models for the categories are are associated with the specified project.
     *
     *
     * @param context
     *        The WIT context.
     *
     * @param projectId
     *        The identifier of the project this collection will be associated
     *        with.
     */
    public CategoryCollectionImpl(final WITContext context, final int projectId) {
        WorkItemTypeCategoryMetadata[] categoriesMetadata;
        categoriesMetadata = context.getMetadata().getWorkItemTypeCategoriesTable().getCategories();

        categories = new ArrayList<Category>();
        for (int i = 0; i < categoriesMetadata.length; i++) {
            final WorkItemTypeCategoryMetadata categoryMetadata = categoriesMetadata[i];
            if (categoryMetadata.getProjectID() == projectId) {
                categories.add(
                    new CategoryImpl(
                        categoryMetadata.getCategoryID(),
                        categoryMetadata.getName(),
                        categoryMetadata.getReferenceName(),
                        categoryMetadata.getDefaultWorkItemTypeID()));
            }
        }
    }

    @Override
    public Iterator<Category> iterator() {
        return categories.iterator();
    }

    @Override
    public int size() {
        return categories.size();
    }

    @Override
    public Category get(final int index) {
        return categories.get(index);
    }

    @Override
    public boolean contains(final String categoryReferenceName) {
        return get(categoryReferenceName) != null;
    }

    @Override
    public Category get(final String categoryReferenceName) {
        for (final Category category : categories) {
            if (category.getReferenceName().equals(categoryReferenceName)) {
                return category;
            }
        }
        return null;
    }
}
