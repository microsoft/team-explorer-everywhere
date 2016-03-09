// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.sparsetree.SparseTree.EnumNodeCallback;

import junit.framework.Assert;
import junit.framework.TestCase;

public class SparseTreeTests extends TestCase {
    // Test 1. Add nodes in reverse order with ignore case.
    public void test1() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/a/Bear/cat/DOG", "node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BEAR/Cat", "node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/a/bear", "node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            "$/A", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumSubTreeOptions.NONE,
            Integer.MAX_VALUE,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/a/bear"); //$NON-NLS-1$
        expectedResults.add("$/A/BEAR/Cat"); //$NON-NLS-1$
        expectedResults.add("$/a/Bear/cat/DOG"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 2. Test ENUMERATE_SUB_TREE_ROOT flag, depth parameter.
    public void test2() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/a/Bear/cat/DOG", "node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BEAR/Cat", "node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/a/bear", "node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            "$/A", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            2,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/a/bear"); //$NON-NLS-1$
        expectedResults.add("$/a/bear/cat"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 3. Test ENUMERATE_SPARSE_NODES flag
    public void test3() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/A", "A node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/F", "F node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/W", "W node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/X/X", "X node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/Y/Y/Y", "Y node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr", "Zephyr node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            "$/A/BINgO", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SPARSE_NODES,
            Integer.MAX_VALUE,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/BINgO/C"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/F"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/W"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/X"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/X/X"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y/Y/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 4. Test ENUMERATE_SPARSE_NODES flag with ENUMERATE_SUB_TREE_ROOT and
    // a
    // depth
    public void test4() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/A", "A node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/F", "F node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/W", "W node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/X/X", "X node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/Y/Y/Y", "Y node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr", "Zephyr node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        final EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SPARSE_NODES.combine(EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT);
        st.EnumSubTree("$/A/BINgO", new AccumulatorCallback(), options, 2, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/BINgO"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 5. Test ENUMERATE_SPARSE_NODES flag with ENUMERATE_SUB_TREE_ROOT and
    // a
    // depth
    // This test has an additional $/A/B/C node
    public void test5() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/A", "A node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C", "C node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/F", "F node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/W", "W node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/X/X", "X node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/Y/Y/Y", "Y node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr", "Zephyr node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        final EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SPARSE_NODES.combine(EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT);
        st.EnumSubTree("$/A/BINgO", new AccumulatorCallback(), options, 2, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/BINgO"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 6. Test ENUMERATE_SPARSE_NODES flag with a depth
    // This test has an additional $/A/B node
    public void test6() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/A", "A node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B", "B node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C", "C node"); //$NON-NLS-1$//$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/F", "F node"); //$NON-NLS-1$//$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/W", "W node"); //$NON-NLS-1$//$NON-NLS-2$
        st.add("$/A/BINgO/C/D/X/X", "X node"); //$NON-NLS-1$//$NON-NLS-2$
        st.add("$/A/BINgO/C/Y/Y/Y", "Y node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr", "Zephyr node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            "$/A/BINgO", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SPARSE_NODES,
            2,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/BINgO/C"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 7. Test ENUMERATE_SPARSE_NODES flag and null token to enumerate
    // entire
    // tree
    public void test7() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/A", "A node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/F", "F node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/W", "W node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/X/X", "X node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/Y/Y/Y", "Y node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr", "Zephyr node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            null,
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SPARSE_NODES,
            Integer.MAX_VALUE,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$"); //$NON-NLS-1$
        expectedResults.add("$/A"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/F"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/W"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/X"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/X/X"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y/Y/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr/Zephyr"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 8. Test null token to enumerate entire tree
    public void test8() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/A", "A node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/F", "F node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/W", "W node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/X/X", "X node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/Y/Y/Y", "Y node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr", "Zephyr node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            null,
            new AccumulatorCallback(),
            EnumSubTreeOptions.NONE,
            Integer.MAX_VALUE,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/F"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/W"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/X/X"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y/Y/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 9. Test null token to enumerate entire tree, with EnumSubTreeRoot
    // flag (should have no effect)
    public void test9() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/A", "A node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/F", "F node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/E/W", "W node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/D/X/X", "X node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/C/Y/Y/Y", "Y node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr", "Zephyr node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            null,
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/F"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/E/W"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/D/X/X"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/C/Y/Y/Y"); //$NON-NLS-1$
        expectedResults.add("$/A/BINgO/Zephyr/Zephyr/Zephyr/Zephyr"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 10. Test fixed length token
    public void test10() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>(4, String.CASE_INSENSITIVE_ORDER);

        st.add("BASESUB1SUB2SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB2SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB5SUB7SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            "BASE", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumSubTreeOptions.NONE,
            Integer.MAX_VALUE,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("BASESUB1"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB3"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB4"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5SUB7SUB9"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 11. Test fixed length token with ENUMERATE_SPARSE_NODES and
    // EnumSubTreeRoot
    public void test11() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>(4, String.CASE_INSENSITIVE_ORDER);

        st.add("BASESUB1SUB2SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB2SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB5SUB7SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        final EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree("BASE", new AccumulatorCallback(), options, Integer.MAX_VALUE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("BASE"); //$NON-NLS-1$
        expectedResults.add("BASESUB1"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB3"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB4"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5SUB7"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5SUB7SUB9"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 12. Test fixed length token with ENUMERATE_SPARSE_NODES and
    // EnumSubTreeRoot and a depth
    public void test12() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>(4, String.CASE_INSENSITIVE_ORDER);

        st.add("BASESUB1SUB2SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB2SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB5SUB7SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        final EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree("BASE", new AccumulatorCallback(), options, 3, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("BASE"); //$NON-NLS-1$
        expectedResults.add("BASESUB1"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB3"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB4"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5SUB7"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 13. Test handling of empty-string token in variable-length mode
    public void test13() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("/$/BASE/SUB1/SUB2/SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB2/SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB5/SUB7/SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/FUN", "Other root node not to be enumerated"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/AAA", "Other root node not to be enumerated"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("", "Base node, empty"); //$NON-NLS-1$ //$NON-NLS-2$

        List<String> results = new ArrayList<String>();
        EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree("", new AccumulatorCallback(), options, 2, additionalData, results); //$NON-NLS-1$

        List<String> expectedResults = new ArrayList<String>();
        expectedResults.add(""); //$NON-NLS-1$
        expectedResults.add("/$"); //$NON-NLS-1$
        expectedResults.add("/$/BASE"); //$NON-NLS-1$
        expectedResults.add("/$/OTHR"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));

        results = new ArrayList<String>();
        options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree("/", new AccumulatorCallback(), options, 2, additionalData, results); //$NON-NLS-1$

        expectedResults = new ArrayList<String>();
        expectedResults.add(""); //$NON-NLS-1$
        expectedResults.add("/$"); //$NON-NLS-1$
        expectedResults.add("/$/BASE"); //$NON-NLS-1$
        expectedResults.add("/$/OTHR"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 14. EnumParents test with variable-length tokens and an implicit
    // empty-string root node
    public void test14() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("/$/BASE/SUB1/SUB2/SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB2/SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB5/SUB7/SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/FUN", "Other root node not to be enumerated"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/AAA", "Other root node not to be enumerated"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents(
            "/$/BASE/SUB1/SUB5/SUB7/SUB9", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumParentsOptions.ENUMERATE_SPARSE_NODES,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("/$/BASE/SUB1/SUB5/SUB7/SUB9"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB5/SUB7"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB5"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1"); //$NON-NLS-1$
        expectedResults.add("/$/BASE"); //$NON-NLS-1$
        expectedResults.add("/$"); //$NON-NLS-1$
        expectedResults.add(""); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 15. EnumParents test with variable-length tokens
    public void test15() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("/$/BASE/SUB1/SUB2/SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB2/SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB5/SUB7/SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/FUN", "Other root node not to be enumerated"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/AAA", "Other root node not to be enumerated"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents(
            "/$/BASE/SUB1/SUB5/SUB7/SUB9", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumParentsOptions.NONE,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("/$/BASE/SUB1/SUB5/SUB7/SUB9"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1"); //$NON-NLS-1$
        expectedResults.add("/$/BASE"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 16. EnumParents test with fixed-length tokens
    public void test16() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>(4, String.CASE_INSENSITIVE_ORDER);

        st.add("BASESUB1SUB2SUB3", "Sub 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("HELO", "fun"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("HELOWRLD", "bar"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("MONKEYSA", "bing"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents(
            "BASESUB1SUB2SUB3", //$NON-NLS-1$
            new AccumulatorCallback(),
            EnumParentsOptions.ENUMERATE_SPARSE_NODES,
            additionalData,
            results);

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("BASESUB1SUB2SUB3"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2"); //$NON-NLS-1$
        expectedResults.add("BASESUB1"); //$NON-NLS-1$
        expectedResults.add("BASE"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 17. Remove test with fixed-length tokens
    public void test17() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>(4, String.CASE_INSENSITIVE_ORDER);

        st.add("BASESUB1SUB2SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB2SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1SUB5SUB7SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("BASESUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$

        List<String> results = new ArrayList<String>();
        st.EnumSubTree(
            null,
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE,
            additionalData,
            results);

        List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("BASE"); //$NON-NLS-1$
        expectedResults.add("BASESUB1"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB3"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB4"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5SUB7SUB9"); //$NON-NLS-1$
        expectedResults.add("OTHR"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));

        // Remove the BASESUB1 node but not its children.
        Assert.assertTrue(st.remove("BASESUB1", false)); //$NON-NLS-1$

        results = new ArrayList<String>();
        st.EnumSubTree(
            null,
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE,
            additionalData,
            results);

        expectedResults = new ArrayList<String>();
        expectedResults.add("BASE"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB3"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB2SUB4"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5SUB7SUB9"); //$NON-NLS-1$
        expectedResults.add("OTHR"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));

        // Remove the BASESUB1SUB2 node (which is sparse) and its children.
        Assert.assertTrue(st.remove("BASESUB1SUB2", true)); //$NON-NLS-1$

        results = new ArrayList<String>();
        st.EnumSubTree(
            null,
            new AccumulatorCallback(),
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT,
            Integer.MAX_VALUE,
            additionalData,
            results);

        expectedResults = new ArrayList<String>();
        expectedResults.add("BASE"); //$NON-NLS-1$
        expectedResults.add("BASESUB1SUB5SUB7SUB9"); //$NON-NLS-1$
        expectedResults.add("OTHR"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 18. Test Remove with variable-length tokens.
    public void test18() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("/$/BASE/SUB1/SUB2/SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB2/SUB4", "Child node 4"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1/SUB5/SUB7/SUB9", "Child node 5"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE", "Base node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/BASE/SUB1", "Child node 1"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("/$/OTHR", "Other node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/FUN", "Other root node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/AAA", "Other root node"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("", "empty string root node"); //$NON-NLS-1$ //$NON-NLS-2$

        List<String> results = new ArrayList<String>();
        EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree(null, new AccumulatorCallback(), options, Integer.MAX_VALUE, additionalData, results);

        List<String> expectedResults = new ArrayList<String>();
        expectedResults.add(""); //$NON-NLS-1$
        expectedResults.add("/$"); //$NON-NLS-1$
        expectedResults.add("/$/BASE"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB2"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB2/SUB3"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB2/SUB4"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB5"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB5/SUB7"); //$NON-NLS-1$
        expectedResults.add("/$/BASE/SUB1/SUB5/SUB7/SUB9"); //$NON-NLS-1$
        expectedResults.add("/$/OTHR"); //$NON-NLS-1$
        expectedResults.add("$"); //$NON-NLS-1$
        expectedResults.add("$/AAA"); //$NON-NLS-1$
        expectedResults.add("$/FUN"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));

        // Remove the empty string token from the tree
        Assert.assertTrue(st.remove("/", false)); //$NON-NLS-1$

        results = new ArrayList<String>();
        options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree(null, new AccumulatorCallback(), options, Integer.MAX_VALUE, additionalData, results);

        // No change to expected results
        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));

        // Remove the empty string token's children from the tree
        Assert.assertTrue(st.remove("/", true)); //$NON-NLS-1$

        results = new ArrayList<String>();
        options = EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree(null, new AccumulatorCallback(), options, Integer.MAX_VALUE, additionalData, results);

        expectedResults = new ArrayList<String>();
        expectedResults.add("$"); //$NON-NLS-1$
        expectedResults.add("$/AAA"); //$NON-NLS-1$
        expectedResults.add("$/FUN"); //$NON-NLS-1$

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 19. Test common failure conditions and miscellaneous methods
    public void test19() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$/BASE/SUB1/SUB2/SUB3", "Child node 3"); //$NON-NLS-1$ //$NON-NLS-2$

        boolean caughtArgumentException = false;

        try {
            // Collision
            st.add("$/BASE/SUB1/SUB2/SUB3", "Replacement"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final IllegalArgumentException e) {
            caughtArgumentException = true;
        }

        Assert.assertTrue(caughtArgumentException);

        // Removing an object that isn't there
        Assert.assertFalse(st.remove("$/SILLY", false)); //$NON-NLS-1$
        Assert.assertFalse(st.remove("$/SILLY", true)); //$NON-NLS-1$

        // Does not exist in the tree
        Assert.assertNull(st.get("$/Wiggles")); //$NON-NLS-1$

        // Does not exist in the tree
        Assert.assertNull(st.get("/")); //$NON-NLS-1$

        // Clear
        st.clear();

        final List<String> results = new ArrayList<String>();
        final EnumSubTreeOptions options =
            EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);
        st.EnumSubTree(null, new AccumulatorCallback(), options, Integer.MAX_VALUE, additionalData, results);

        final List<String> expectedResults = new ArrayList<String>();

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 20. EnumParents test with SparseTreeAdditionalData (1)
    public void test20() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$", "Root"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "A"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/Z", "AZ"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents("$/A/B/C/D/E", new NoChildrenBelowCallback(), EnumParentsOptions.NONE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/B"); //$NON-NLS-1$
        expectedResults.add(null);

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 21. EnumParents test with SparseTreeAdditionalData (2)
    public void test21() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$", "Root"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "A"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/A", "AA"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents("$/A/B/C/D/E", new NoChildrenBelowCallback(), EnumParentsOptions.NONE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/B"); //$NON-NLS-1$
        expectedResults.add(null);

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 22. EnumParents test with SparseTreeAdditionalData (3)
    public void test22() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$", "Root"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "A"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/A", "AA"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C/A", "ABCA"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C/Y", "ABCY"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C/Z", "ABCZ"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/Z/Y", "AZY"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents("$/A/B/C/D/E", new NoChildrenBelowCallback(), EnumParentsOptions.NONE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/B/C/D"); //$NON-NLS-1$
        expectedResults.add(null);

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 23. EnumParents test with SparseTreeAdditionalData (4)
    public void test23() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$", "Root"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "A"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/A", "AA"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents("$/A/A", new NoChildrenBelowCallback(), EnumParentsOptions.NONE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/A"); //$NON-NLS-1$
        expectedResults.add(null);
        expectedResults.add(null);

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 24. EnumParents test with SparseTreeAdditionalData (5)
    public void test24() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$", "Root"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "A"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/A", "AA"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C/A", "ABCA"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C/D/X", "ABCDX"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C/Y", "ABCY"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/B/C/Z", "ABCZ"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/Z/Y", "AZY"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents("$/A/B/C/D/E", new NoChildrenBelowCallback(), EnumParentsOptions.NONE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/B/C/D/E"); //$NON-NLS-1$
        expectedResults.add(null);

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 25. EnumParents test with SparseTreeAdditionalData (6)
    public void test25() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$", "Root"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "A"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/A/B", "AAB"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents("$/A/A", new NoChildrenBelowCallback(), EnumParentsOptions.NONE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add(null);
        expectedResults.add(null);

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    // Test 25. EnumParents test with SparseTreeAdditionalData (7)
    public void test26() {
        final SparseTreeAdditionalData additionalData = new SparseTreeAdditionalData();
        final SparseTree<String> st = new SparseTree<String>('/', String.CASE_INSENSITIVE_ORDER);

        st.add("$", "Root"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A", "A"); //$NON-NLS-1$ //$NON-NLS-2$
        st.add("$/A/A/B", "AAB"); //$NON-NLS-1$ //$NON-NLS-2$

        final List<String> results = new ArrayList<String>();
        st.EnumParents("$/A/A/C", new NoChildrenBelowCallback(), EnumParentsOptions.NONE, additionalData, results); //$NON-NLS-1$

        final List<String> expectedResults = new ArrayList<String>();
        expectedResults.add("$/A/A/C"); //$NON-NLS-1$
        expectedResults.add(null);

        Assert.assertTrue(areStringListsEqual(expectedResults, results, String.CASE_INSENSITIVE_ORDER));
    }

    class NoChildrenBelowCallback implements EnumNodeCallback<String> {
        @Override
        public boolean invoke(
            final String token,
            final String referencedObject,
            final SparseTreeAdditionalData additionalData,
            final Object param) {
            @SuppressWarnings("unchecked")
            final List<String> accumulator = (List<String>) param;
            accumulator.add(additionalData.noChildrenBelow);

            // If hasChildren is false, then NoChildrenBelow should be equal to
            // token.
            Assert.assertTrue(token.equalsIgnoreCase(additionalData.noChildrenBelow) || additionalData.hasChildren);

            return false;
        }
    }

    class AccumulatorCallback implements EnumNodeCallback<String> {
        @Override
        public boolean invoke(
            final String token,
            final String referencedObject,
            final SparseTreeAdditionalData additionalData,
            final Object param) {
            @SuppressWarnings("unchecked")
            final List<String> accumulator = (List<String>) param;
            accumulator.add(token);
            return false;
        }
    }

    /**
     * Helper method to tell whether 2 lists are equal.
     */
    private boolean areStringListsEqual(
        final List<String> list1,
        final List<String> list2,
        final Comparator<String> comparison) {
        if (list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            if (list1.get(i) == null && list2.get(i) == null) {
                continue;
            }

            if (comparison.compare(list1.get(i), list2.get(i)) != 0) {
                return false;
            }
        }

        return true;
    }
}
