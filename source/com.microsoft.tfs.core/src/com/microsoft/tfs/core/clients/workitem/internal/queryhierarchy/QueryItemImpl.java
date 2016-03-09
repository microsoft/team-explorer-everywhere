// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy;

import java.text.MessageFormat;
import java.util.Date;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.internal.QueryHierarchyProvider;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.FileHelpers;
import com.microsoft.tfs.util.GUID;

public abstract class QueryItemImpl implements QueryItem {
    private static final char[] INVALID_NAME_CHARACTERS = {
        0x00ab,
        0x00bb,
        0x2044
    };

    private WITContext context;

    private int projectId;

    private GUID id;
    private String name;
    private String originalName;
    private QueryFolder parent;
    private QueryFolder originalParent;
    private IdentityDescriptor ownerDescriptor;
    private IdentityDescriptor originalOwnerDescriptor;
    private Object accessControlList;
    private Object originalAccessControlList;
    private boolean deleted;
    private boolean personal;
    private boolean isNew;

    protected QueryItemImpl(final String name, final QueryFolder parent) {
        isNew = true;

        initialize(name, parent, null, null, true);

        if (this.parent != null) {
            this.parent.add(this);
        }
    }

    protected QueryItemImpl(
        final String name,
        final QueryFolder parent,
        final GUID id,
        final IdentityDescriptor ownerDescriptor) {
        initialize(name, parent, id, ownerDescriptor, false);

        isNew = false;
        originalParent = parent;

        if (this.parent != null && this.parent instanceof QueryFolderImpl) {
            ((QueryFolderImpl) this.parent).addInternal(this, false);
        }
    }

    private void initialize(
        String name,
        final QueryFolder parent,
        final GUID id,
        final IdentityDescriptor ownerDescriptor,
        final boolean checkName) {
        if (checkName) {
            name = checkNameIsValid(name);
        }

        if (id == null) {
            this.id = GUID.newGUID();
        } else {
            this.id = id;
        }

        this.name = name;
        originalName = name;
        this.parent = parent;
        this.ownerDescriptor = ownerDescriptor;
        originalOwnerDescriptor = ownerDescriptor;

        if (this.parent != null) {
            setProject(this.parent.getProject());
            personal = this.parent.isPersonal();
        }
    }

    private static String checkNameIsValid(String name) {
        if (name == null) {
            throw new IllegalArgumentException(Messages.getString("QueryItem.QueryNameCannotBeNull")); //$NON-NLS-1$
        }

        name = name.trim();

        if (name.length() > 0xff) {
            throw new IllegalArgumentException(Messages.getString("QueryItem.QueryNameTooLong")); //$NON-NLS-1$
        }

        if (!FileHelpers.isValidNTFSFileName(name)) {
            throw new IllegalArgumentException(Messages.getString("QueryItem.QueryNameContainsInvalidCharaacters")); //$NON-NLS-1$
        }

        for (int i = 0; i < INVALID_NAME_CHARACTERS.length; i++) {
            if (name.indexOf(INVALID_NAME_CHARACTERS[i]) >= 0) {
                throw new IllegalArgumentException(Messages.getString("QueryItem.QueryNameContainsInvalidCharaacters")); //$NON-NLS-1$
            }
        }

        return name;
    }

    protected String checkName(final String name) {
        return checkNameIsValid(name);
    }

    @Override
    public GUID getID() {
        return id;
    }

    protected void setID(final GUID id) {
        this.id = id;
    }

    @Override
    public Project getProject() {
        if (context != null) {
            return context.getClient().getProjects().getByID(projectId);
        }

        return null;
    }

    protected void setProject(final Project project) {
        if (project != null) {
            context = project.getWITContext();
            projectId = project.getID();
        } else {
            context = null;
            projectId = 0;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalName() {
        return originalName;
    }

    @Override
    public void setName(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        if (isDeleted()) {
            throw new WorkItemException(Messages.getString("QueryItem.CannotModifyDeletedItems")); //$NON-NLS-1$
        }

        if (!name.equals(this.name)) {
            final String newName = checkName(name);

            if (!newName.equals(this.name)) {
                if (parent != null && parent instanceof QueryFolderImpl) {
                    ((QueryFolderImpl) parent).checkForDuplicateName(this, newName);
                    ((QueryFolderImpl) parent).updateName(this, newName);
                    ((QueryFolderImpl) parent).onContentsChanged(this, QueryFolderAction.CHANGED);
                } else {
                    this.name = name;
                }
            }
        }
    }

    protected void setNameInternal(final String name) {
        this.name = name;
    }

    protected QueryHierarchyProvider getQueryHierarchyProvider() {
        if (context != null) {
            return context.getQueryHierarchyProvider();
        }

        return null;
    }

    @Override
    public QueryFolder getParent() {
        return parent;
    }

    @Override
    public QueryFolder getOriginalParent() {
        return originalParent;
    }

    protected void setParent(final QueryFolder parent) {
        if (this.parent != parent) {
            this.parent = parent;

            final Project project = getProject();
            final boolean isPersonal = personal;

            if (this.parent != null) {
                setProject(this.parent.getProject());
                personal = this.parent.isPersonal();
            } else {
                setProject(null);
                personal = false;
            }

            if (getProject() != project || isPersonal != personal) {
                onMoveChangedHierarchy();
            }
        }
    }

    @Override
    public IdentityDescriptor getOwnerDescriptor() {
        return ownerDescriptor;
    }

    @Override
    public IdentityDescriptor getOriginalOwnerDescriptor() {
        return originalOwnerDescriptor;
    }

    @Override
    public void setOwnerDescriptor(final IdentityDescriptor ownerDescriptor) {
        final Project project = getProject();

        if (ownerDescriptor != null && project != null && !project.getQueryHierarchy().supportsPermissions()) {
            throw new WorkItemException(Messages.getString("QueryItem.ServerDoesNotSupportPermissions")); //$NON-NLS-1$
        }

        this.ownerDescriptor = ownerDescriptor;
    }

    @Override
    public void delete() {
        if (isDeleted()) {
            throw new WorkItemException(Messages.getString("QueryItem.ItemAlreadyDeleted")); //$NON-NLS-1$
        }

        if (getParent() == null) {
            throw new WorkItemException(Messages.getString("QueryItem.CannotDeleteOrphanedItem")); //$NON-NLS-1$
        }

        if (this instanceof QueryFolderImpl && ((QueryFolderImpl) this).isRootNode()) {
            throw new WorkItemException(Messages.getString("QueryItem.RootNodesMayNotBeModified")); //$NON-NLS-1$
        }

        final QueryFolder previousParent = getParent();
        setDeleted(true);

        if (previousParent instanceof QueryFolderImpl) {
            ((QueryFolderImpl) previousParent).deleteInternal(this, true);
            ((QueryFolderImpl) previousParent).onContentsChanged(this, QueryFolderAction.REMOVED);
        }
    }

    @Override
    public boolean isDeleted() {
        if (parent == null) {
            return deleted;
        }

        if (!deleted) {
            return parent.isDeleted();
        }

        return true;
    }

    protected void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean isPersonal() {
        return personal;
    }

    protected void setPersonal(final boolean personal) {
        this.personal = personal;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public boolean isDirty() {
        if (!isDirtyShallow()) {
            if (accessControlList == null) {
                return (originalAccessControlList != null);
            }

            return !accessControlList.equals(originalAccessControlList);
        }

        return true;
    }

    protected boolean isDirtyShallow() {
        if (!isNew
            && !deleted
            && ((name == null && originalName == null)
                || (name != null && originalName != null && name.equals(originalName)))
            && ((ownerDescriptor == null && originalOwnerDescriptor == null)
                || (ownerDescriptor != null
                    && originalOwnerDescriptor != null
                    && ownerDescriptor.equals(originalOwnerDescriptor)))) {
            return parent != originalParent;
        }

        return true;
    }

    protected void resetDirty() {
        originalName = name;
        originalParent = parent;
        originalOwnerDescriptor = ownerDescriptor;
    }

    protected void resetInternal() {
        if (isDirty()) {
            ownerDescriptor = originalOwnerDescriptor;
            deleted = false;

            if (parent != originalParent) {
                if (parent != null) {
                    if (parent instanceof QueryFolderImpl) {
                        ((QueryFolderImpl) parent).deleteInternal(this, true);
                    }

                    parent = null;
                }

                name = originalName;

                if (originalParent != null && originalParent instanceof QueryFolderImpl) {
                    ((QueryFolderImpl) originalParent).addInternal(this, true);
                }
            } else if (parent != null && !parent.containsID(getID())) {
                name = originalName;

                if (parent instanceof QueryFolderImpl) {
                    ((QueryFolderImpl) parent).addInternal(this, true);
                }
            } else if (!name.equals(originalName)) {
                if (parent != null && parent instanceof QueryFolderImpl) {
                    ((QueryFolderImpl) parent).updateName(this, originalName);
                }
                name = originalName;
            }
        }
    }

    protected abstract void validate(WITContext context);

    protected void onMoveChangedHierarchy() {
    }

    protected void onSaveCompleted() {
        originalName = name;
        originalOwnerDescriptor = ownerDescriptor;

        if (isNew && !deleted) {
            isNew = false;
            originalParent = parent;

            if (parent instanceof QueryFolderImpl) {
                ((QueryFolderImpl) parent).onAddSaved(this);
            }
        } else if (!isNew && deleted) {
            final QueryFolder originalParent = this.originalParent;
            setProject(null);
            parent = null;
            this.originalParent = null;
            onMoveChangedHierarchy();

            if (originalParent instanceof QueryFolderImpl) {
                ((QueryFolderImpl) originalParent).onDeleteSaved(this);
            }
        } else if (parent != originalParent) {
            final QueryFolder originalParent = this.originalParent;
            this.originalParent = parent;

            if (originalParent instanceof QueryFolderImpl) {
                ((QueryFolderImpl) originalParent).onDeleteSaved(this);
            }

            if (parent instanceof QueryFolderImpl) {
                ((QueryFolderImpl) parent).onAddSaved(this);
            }
        } else if (!isNew && parent instanceof QueryFolderImpl) {
            ((QueryFolderImpl) parent).onUpdateSaved(this);
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}:{1}", name, id); //$NON-NLS-1$
    }

    public void updateAfterUpdate(final Date updateTime) {
    }

    @Override
    public abstract QueryItemType getType();

    @Override
    public int hashCode() {
        return this.getID().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof QueryItemImpl) {
            final QueryItemImpl o1 = (QueryItemImpl) o;
            return this.getID().equals(o1.getID());
        }
        return false;
    }
}
