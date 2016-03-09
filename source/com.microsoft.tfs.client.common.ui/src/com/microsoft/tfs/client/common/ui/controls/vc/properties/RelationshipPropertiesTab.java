// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchProperties;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

public class RelationshipPropertiesTab implements PropertiesTab {
    private RelationshipPropertiesControl control;

    @Override
    public Control setupTabItemControl(final Composite parent) {
        control = new RelationshipPropertiesControl(parent, SWT.NONE);
        return control;
    }

    @Override
    public String getTabItemText() {
        return Messages.getString("RelationshipPropertiesTab.TabItemText"); //$NON-NLS-1$
    }

    @Override
    public void populate(final TFSRepository repository, final TFSItem item) {
        final ItemIdentifier rootItem = new ItemIdentifier(item.getFullPath(), LatestVersionSpec.INSTANCE, 0);
        populate(repository, rootItem);
    }

    @Override
    public void populate(final TFSRepository repository, final ItemIdentifier id) {
        final BranchObjectInput input = new BranchObjectInput(repository, id);
        final TreeViewer viewer = control.getViewer();
        viewer.setInput(input);
        viewer.expandToLevel(input.branch, AbstractTreeViewer.ALL_LEVELS);
    }

    @Override
    public boolean okPressed() {
        return true;
    }
}

class RelationshipPropertiesControl extends BaseControl {
    private final TreeViewer viewer;

    public RelationshipPropertiesControl(final Composite parent, final int style) {
        super(parent, style);

        final FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.spacing = getSpacing();
        setLayout(layout);

        viewer = new TreeViewer(this);
        viewer.setContentProvider(new BranchObjectContentProvider());
        viewer.setLabelProvider(new BranchObjectLabelProvider());
    }

    public TreeViewer getViewer() {
        return viewer;
    }
}

class BranchObjectContentProvider implements ITreeContentProvider {

    private BranchObjectInput input;

    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof BranchObject) {
            final BranchObject b = (BranchObject) parentElement;
            final ArrayList<BranchObject> children = new ArrayList<BranchObject>();
            for (int i = 0; i < b.getChildBranches().length; i++) {
                final BranchObject child = input.lookup(b.getChildBranches()[i]);

                if (child != null) {
                    children.add(child);
                }
            }
            return children.toArray(new BranchObject[0]);
        }
        return null;
    }

    @Override
    public Object getParent(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            final ItemIdentifier id = b.getProperties().getParentBranch();
            if (id != null) {
                return input.lookup(id);
            }
        }
        return null;
    }

    @Override
    public boolean hasChildren(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            return b.getChildBranches().length > 0;
        }
        return false;
    }

    @Override
    public Object[] getElements(final Object inputElement) {
        if (inputElement instanceof BranchObjectInput) {
            final BranchObjectInput b = (BranchObjectInput) inputElement;
            return new BranchObject[] {
                b.root
            };
        }
        return null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        if (newInput instanceof BranchObjectInput) {
            input = (BranchObjectInput) newInput;
        }
    }

}

class BranchObjectLabelProvider extends LabelProvider {

    @Override
    public Image getImage(final Object element) {
        if (element instanceof BranchObject) {
            final BranchProperties p = ((BranchObject) element).getProperties();
            if (p != null && p.getRootItem() != null && p.getRootItem().getDeletionID() > 0) {
                return new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID).getImage(
                    "images/vc/folder_branch_deleted.gif"); //$NON-NLS-1$
            }
            return new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID).getImage("images/vc/folder_branch.gif"); //$NON-NLS-1$
        }
        return super.getImage(element);
    }

    @Override
    public String getText(final Object element) {
        if (element instanceof BranchObject) {
            final BranchObject b = (BranchObject) element;
            return ServerPath.getFileName(b.getProperties().getRootItem().getItem());
        }
        return super.getText(element);
    }

}

class BranchObjectInput {
    BranchObject branch;

    BranchObject root;

    ArrayList<BranchObject> allBranches = new ArrayList<BranchObject>();

    TFSRepository repository;

    public BranchObjectInput(final TFSRepository repository, final ItemIdentifier id) {
        this.repository = repository;
        allBranches.addAll(Arrays.asList(query(id)));
        branch = lookup(id);
    }

    public BranchObject lookup(final ItemIdentifier id) {
        return find(id, allBranches.toArray(new BranchObject[0]));
    }

    private BranchObject find(final ItemIdentifier id, final BranchObject[] branches) {
        for (int i = 0; i < branches.length; i++) {
            if (id.getItem().equals(branches[i].getProperties().getRootItem().getItem())) {
                if (branches[i].getProperties().getParentBranch() != null) {
                    final BranchObject parent = queryFind(branches[i].getProperties().getParentBranch());
                    allBranches.add(parent);
                } else {
                    root = branches[i];
                }
                return branches[i];
            }
        }
        return null;
    }

    private BranchObject[] query(final ItemIdentifier id) {
        return repository.getVersionControlClient().queryBranchObjects(id, RecursionType.ONE_LEVEL);
    }

    private BranchObject queryFind(final ItemIdentifier id) {
        BranchObject b = lookup(id);
        if (b == null) {
            b = find(id, query(id));
        }
        return b;
    }
}
