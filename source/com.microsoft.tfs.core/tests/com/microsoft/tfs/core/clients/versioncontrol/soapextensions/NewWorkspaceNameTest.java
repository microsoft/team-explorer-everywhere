// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import ms.tfs.versioncontrol.clientservices._03._Workspace;

public class NewWorkspaceNameTest extends TestCase {
    public void testNewWorkspaceName_01() {
        final Workspace[] workspaces = null;
        final String newName = Workspace.computeNewWorkspaceName("machine", workspaces); //$NON-NLS-1$

        assertTrue("machine".equalsIgnoreCase(newName)); //$NON-NLS-1$
    }

    public void testNewWorkspaceName_02() {
        final Workspace[] workspaces = new Workspace[0];
        final String newName = Workspace.computeNewWorkspaceName("machine", workspaces); //$NON-NLS-1$

        assertTrue("machine".equalsIgnoreCase(newName)); //$NON-NLS-1$
    }

    public void testNewWorkspaceName_03() {
        final Workspace[] workspaces = createWorkspaces("machine"); //$NON-NLS-1$
        final String newName = Workspace.computeNewWorkspaceName("machine", workspaces); //$NON-NLS-1$

        assertTrue("machine_1".equalsIgnoreCase(newName)); //$NON-NLS-1$
    }

    public void testNewWorkspaceName_04() {
        final Workspace[] workspaces = createWorkspaces(
            "a", //$NON-NLS-1$
            "machine", //$NON-NLS-1$
            "b", //$NON-NLS-1$
            "machine_1", //$NON-NLS-1$
            "machine_2", //$NON-NLS-1$
            "machine_3", //$NON-NLS-1$
            "c"); //$NON-NLS-1$
        final String newName = Workspace.computeNewWorkspaceName("machine", workspaces); //$NON-NLS-1$

        assertTrue("machine_4".equalsIgnoreCase(newName)); //$NON-NLS-1$
    }

    public void testNewWorkspaceName_05() {
        final Workspace[] workspaces = createWorkspaces(
            "a", //$NON-NLS-1$
            "machine", //$NON-NLS-1$
            "b", //$NON-NLS-1$
            "machine_1", //$NON-NLS-1$
            "machine_3", //$NON-NLS-1$
            "c"); //$NON-NLS-1$
        final String newName = Workspace.computeNewWorkspaceName("machine", workspaces); //$NON-NLS-1$

        assertTrue("machine_2".equalsIgnoreCase(newName)); //$NON-NLS-1$
    }

    private Workspace createWorkspace(final String name) {
        final _Workspace _w = new _Workspace();
        _w.setName(name);

        final Workspace w = new Workspace(_w, null);
        return w;
    }

    private Workspace[] createWorkspaces(final String... names) {
        final List<Workspace> workspaces = new ArrayList<Workspace>(names.length);
        for (final String name : names) {
            workspaces.add(createWorkspace(name));
        }
        return workspaces.toArray(new Workspace[names.length]);
    }
}
