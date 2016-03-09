// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.item;

import junit.framework.TestCase;

public class ServerItemPathTest extends TestCase {
    private ServerItemPath path;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        path = new ServerItemPath("$/folder1/folder2/folder3/file"); //$NON-NLS-1$
    }

    public void testPathSetup() {
        assertEquals("file", path.getName()); //$NON-NLS-1$
        assertEquals("$/folder1/folder2/folder3/file", path.getFullPath()); //$NON-NLS-1$
        assertFalse(path.isRoot());
    }

    public void testIsAncestor() {
        assertTrue(path.isStrictParentOf(new ServerItemPath(path.getFullPath() + "/child"))); //$NON-NLS-1$
        assertTrue(path.isStrictParentOf(new ServerItemPath(path.getFullPath() + "/child/child2/child3"))); //$NON-NLS-1$
        assertFalse(path.isStrictParentOf(new ServerItemPath("$/a/b/c"))); //$NON-NLS-1$
        assertFalse(path.isStrictParentOf(path));
        assertFalse(path.isStrictParentOf(new ServerItemPath(path.getFullPath())));
        assertTrue(path.isStrictParentOf(path.combine("child1"))); //$NON-NLS-1$
        assertTrue(path.isStrictParentOf(path.combine("child1").combine("child2"))); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(path.isStrictParentOf(new ServerItemPath("$/folder1/folder2/folder3/fileold"))); //$NON-NLS-1$
    }

    public void testCreateChildPath() {
        final ServerItemPath child1 = path.combine("child1"); //$NON-NLS-1$
        final ServerItemPath child2 = path.combine("child2"); //$NON-NLS-1$
        final ServerItemPath grandChild = child1.combine("grandchild"); //$NON-NLS-1$

        assertEquals(path, child1.getParent());
        assertEquals(path, child2.getParent());
        assertEquals(child1, grandChild.getParent());
        assertEquals(path, grandChild.getParent().getParent());
    }

    public void testPathToParent() {
        final ServerItemPath a1 = path.getParent();
        assertEquals("folder3", a1.getName()); //$NON-NLS-1$
        assertEquals("$/folder1/folder2/folder3", a1.getFullPath()); //$NON-NLS-1$
        assertFalse(a1.isRoot());

        final ServerItemPath a2 = a1.getParent();
        assertEquals("folder2", a2.getName()); //$NON-NLS-1$
        assertEquals("$/folder1/folder2", a2.getFullPath()); //$NON-NLS-1$
        assertFalse(a2.isRoot());

        final ServerItemPath a3 = a2.getParent();
        assertEquals("folder1", a3.getName()); //$NON-NLS-1$
        assertEquals("$/folder1", a3.getFullPath()); //$NON-NLS-1$
        assertFalse(a3.isRoot());

        final ServerItemPath a4 = a3.getParent();
        assertEquals("$", a4.getName()); //$NON-NLS-1$
        assertEquals("$/", a4.getFullPath()); //$NON-NLS-1$
        assertTrue(a4.isRoot());
        assertNull(a4.getParent());

    }

    public void testTrailingSlashes() {
        final ServerItemPath a1 = new ServerItemPath("$/a1/a2/a3"); //$NON-NLS-1$
        final ServerItemPath a2 = new ServerItemPath("$/a1/a2/a3/"); //$NON-NLS-1$

        assertEquals("a3", a1.getName()); //$NON-NLS-1$
        assertEquals("a3", a2.getName()); //$NON-NLS-1$

        assertEquals("$/a1/a2/a3", a1.getFullPath()); //$NON-NLS-1$
        assertEquals("$/a1/a2/a3", a2.getFullPath()); //$NON-NLS-1$
    }

    public void testRootTrailingSlashes() {
        final ServerItemPath r1 = new ServerItemPath("$"); //$NON-NLS-1$
        final ServerItemPath r2 = new ServerItemPath("$/"); //$NON-NLS-1$
        final ServerItemPath a1 = new ServerItemPath("$/a1"); //$NON-NLS-1$
        final ServerItemPath a2 = new ServerItemPath("$/a1/"); //$NON-NLS-1$
        final ServerItemPath r3 = a1.getParent();
        final ServerItemPath r4 = a2.getParent();

        assertNotNull(r3);
        assertNotNull(r4);

        assertTrue(r1.isRoot());
        assertTrue(r2.isRoot());
        assertTrue(r3.isRoot());
        assertTrue(r4.isRoot());

        assertEquals("$/", r1.getFullPath()); //$NON-NLS-1$
        assertEquals("$/", r2.getFullPath()); //$NON-NLS-1$
        assertEquals("$/", r3.getFullPath()); //$NON-NLS-1$
        assertEquals("$/", r4.getFullPath()); //$NON-NLS-1$
    }

    public void testEquals() {
        assertTrue(new ServerItemPath("$/a1/a2").equals(new ServerItemPath("$/a1/a2"))); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(new ServerItemPath("$/a1/a2").equals(new ServerItemPath("$/a1/a2/"))); //$NON-NLS-1$ //$NON-NLS-2$
        assertFalse(new ServerItemPath("$/a1/a2").equals(new ServerItemPath("$/a1"))); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(new ServerItemPath("$/").equals(new ServerItemPath("$/"))); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(new ServerItemPath("$/").equals(new ServerItemPath("$"))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testEqualsIgnoringCase() {
        assertTrue(new ServerItemPath("$/a1/a2").equals(new ServerItemPath("$/A1/A2"))); //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(new ServerItemPath("$/A1/a2").equals(new ServerItemPath("$/a1/A2/"))); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void testCreateRoot() {
        final ServerItemPath root = ServerItemPath.ROOT;
        assertTrue(root.isRoot());
        assertEquals(root, ServerItemPath.ROOT);
        assertEquals("$/", root.getFullPath()); //$NON-NLS-1$
    }

    public void testGetPathSectionCount() {
        assertEquals(0, ServerItemPath.ROOT.getFolderDepth());
        assertEquals(0, new ServerItemPath("$").getFolderDepth()); //$NON-NLS-1$
        assertEquals(0, new ServerItemPath("$/").getFolderDepth()); //$NON-NLS-1$

        assertEquals(1, new ServerItemPath("$/a").getFolderDepth()); //$NON-NLS-1$
        assertEquals(1, new ServerItemPath("$/a/").getFolderDepth()); //$NON-NLS-1$

        assertEquals(2, new ServerItemPath("$/a/b").getFolderDepth()); //$NON-NLS-1$
        assertEquals(2, new ServerItemPath("$/a/b/").getFolderDepth()); //$NON-NLS-1$

        assertEquals(4, new ServerItemPath("$/a/b/c/d").getFolderDepth()); //$NON-NLS-1$
    }

    public void testGetPathSection() {
        assertEquals("$", path.getPathSection(0)); //$NON-NLS-1$
        assertEquals("folder1", path.getPathSection(1)); //$NON-NLS-1$
        assertEquals("folder2", path.getPathSection(2)); //$NON-NLS-1$
        assertEquals("folder3", path.getPathSection(3)); //$NON-NLS-1$
        assertEquals("file", path.getPathSection(4)); //$NON-NLS-1$

        try {
            path.getPathSection(5);
            fail("expected an ArrayIndexOutOfBoundsException"); //$NON-NLS-1$
        } catch (final ArrayIndexOutOfBoundsException e) {

        }

        final ServerItemPath testPath = new ServerItemPath("$/path / with/ spaces /in/it"); //$NON-NLS-1$
        assertEquals("$", testPath.getPathSection(0)); //$NON-NLS-1$
        assertEquals("path", testPath.getPathSection(1)); //$NON-NLS-1$
        assertEquals(" with", testPath.getPathSection(2)); //$NON-NLS-1$
        assertEquals(" spaces", testPath.getPathSection(3)); //$NON-NLS-1$
        assertEquals("in", testPath.getPathSection(4)); //$NON-NLS-1$
        assertEquals("it", testPath.getPathSection(5)); //$NON-NLS-1$
    }

    public void testCreateAncestorPath() {
        final ServerItemPath p1 = new ServerItemPath("$/a/b/c/d/e"); //$NON-NLS-1$

        assertEquals(ServerItemPath.ROOT, p1.getHierarchy()[0]);
        assertEquals(new ServerItemPath("$"), p1.getHierarchy()[0]); //$NON-NLS-1$

        assertEquals(new ServerItemPath("$/a"), p1.getHierarchy()[1]); //$NON-NLS-1$
        assertEquals(new ServerItemPath("$/a/b"), p1.getHierarchy()[2]); //$NON-NLS-1$
        assertEquals(new ServerItemPath("$/a/b/c"), p1.getHierarchy()[3]); //$NON-NLS-1$
        assertEquals(new ServerItemPath("$/a/b/c/d"), p1.getHierarchy()[4]); //$NON-NLS-1$
        assertEquals(new ServerItemPath("$/a/b/c/d/e"), p1.getHierarchy()[5]); //$NON-NLS-1$
        assertEquals(p1, p1.getHierarchy()[5]);
        assertEquals(p1, p1.getHierarchy()[p1.getFolderDepth()]);
    }

    public void testProjectPath() {
        final ServerItemPath projectPath = new ServerItemPath("$/SomeProject"); //$NON-NLS-1$
        assertEquals(projectPath, (new ServerItemPath("$/SomeProject/a")).getHierarchy()[1]); //$NON-NLS-1$
        assertEquals(projectPath, (new ServerItemPath("$/SomeProject/a.txt")).getHierarchy()[1]); //$NON-NLS-1$
        assertEquals(projectPath, (new ServerItemPath("$/SomeProject/a/b/c/d/e/f/g/h/i.txt")).getHierarchy()[1]); //$NON-NLS-1$
    }
}
