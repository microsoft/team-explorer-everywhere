// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.sparsetree;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.NotYetImplementedException;
import com.microsoft.tfs.util.StringUtil;

public class SparseTree<T> {
    /**
     * The root node in the store.
     */
    private SparseTreeNode<T> rootNode;

    /**
     * Comparison type for the tokens
     */
    private Comparator<String> tokenComparison;

    /**
     * The optional separator character. If this is the null character, there is
     * no separator and the token assumed to be a fixed length meaning the
     * fixedElementLength member will be used for splitting
     */
    private char tokenSeparator;

    /**
     * Character array for the optional separator character. Used for splitting
     * and trimming.
     */
    private char[] tokenSeparatorArray;

    /**
     * The optional element length. If this is -1, a variable length token is
     * assumed and the token separator will be used.
     */
    private int fixedElementLength;

    /**
     * The number of key/value pairs in the SparseTree.
     */
    private int count;

    private WeakReference<SparseTreeNode<T>> ts_findClosestNodeCache;

    /**
     * An empty string array.
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[] {
        "" //$NON-NLS-1$
    };

    /**
     * The EnumNodeCallback is used when enumerating the nodes in the store. It
     * is used with both EnumParents and EnumSubTree.
     */
    public interface EnumNodeCallback<T> {
        boolean invoke(String token, T referencedObject, SparseTreeAdditionalData additionalData, Object param);
    }

    /**
     * The ModifyInPlaceCallback is used when a caller wants to look up a node
     * in the tree with a certain token and then change the referenced object
     * for that token, with only a single lookup in the tree.
     */
    public interface ModifyInPlaceCallback<T> {
        T invoke(String token, T referencedObject, Object param);
    }

    /**
     * Tokens have variable length segments that are separated by the set of
     * characters given to the constructor. Tokens are compared with an
     * OrdinalIgnoreCase comparison.
     *
     *
     * @param tokenSeparator
     *        The set of valid token separator characters.
     */
    public SparseTree(final char tokenSeparator) {
        this(tokenSeparator, -1, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Tokens have fixed length segments that are separated by the element
     * length provided. Tokens are compared with an OrdinalIgnoreCase comparison
     *
     *
     * @param fixedElementLength
     *        The fixed element length to separate the tokens by.
     */
    public SparseTree(final int fixedElementLength) {
        this('\0', fixedElementLength, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Tokens are assumed to contain fixed length segments (ie. OrdinalPaths).
     * Tokens are compared with the StringComparison method supplied.
     *
     *
     * @param tokenComparison
     *        The type of comparison to use when comparing tokens.
     */
    public SparseTree(final Comparator<String> tokenComparison) {
        this('\0', tokenComparison);
    }

    /**
     * Tokens have variable length segments that are separated by the set of
     * characters given to the constructor. Tokens are compared with the
     * StringComparison method supplied
     *
     *
     * @param tokenSeparator
     *        The token separator character.
     * @param tokenComparison
     *        The type of comparison to use when comparing tokens.
     */
    public SparseTree(final char tokenSeparator, final Comparator<String> tokenComparison) {
        this(tokenSeparator, -1, tokenComparison);
    }

    /**
     * Tokens have fixed length segments that are separated by the length
     * provided. Tokens are compared with the StringComparison method supplied
     *
     *
     * @param fixedElementLength
     *        The fixed element length to separate the tokens by.
     * @param tokenComparison
     *        The type of comparison to use when comparing tokens.
     */
    public SparseTree(final int fixedElementLength, final Comparator<String> tokenComparison) {
        this('\0', fixedElementLength, tokenComparison);
    }

    public SparseTree(
        final char tokenSeparator,
        final int fixedElementLength,
        final Comparator<String> tokenComparison) {
        // Validate the split parameters
        Check.isTrue(
            tokenSeparator != '\0' || fixedElementLength > 0,
            "The token separator cannot be null or element length has to be greater than 0"); //$NON-NLS-1$

        clear();
        this.tokenComparison = tokenComparison;
        this.tokenSeparator = tokenSeparator;

        if (this.tokenSeparator != '\0') {
            this.tokenSeparatorArray = new char[] {
                this.tokenSeparator
            };
        }

        this.fixedElementLength = fixedElementLength;
    }

    /**
     * This is used to add an object to the store at a specific path
     *
     *
     * @param token
     *        The token we are adding.
     * @param referencedObject
     *        The referencedObject we are adding.
     */
    public void add(final String token, final T referencedObject) {
        add(token, referencedObject, false);
    }

    /**
     * This is used to add an object to the store at a specific path
     *
     *
     * @param token
     *        The token we are adding.
     * @param referencedObject
     *        The referencedObject we are adding.
     * @param overwrite
     *        If true and an object already exists for this token, that object
     *        will be removed and this one will take its place.
     */
    public void add(String token, final T referencedObject, final boolean overwrite) {
        Check.notNull(token, "token"); //$NON-NLS-1$

        // Canonicalize the input.
        token = canonicalizeToken(token);

        final String[] tokenElements = splitToken(token);

        // Find the closest parent and child node
        final AtomicReference<SparseTreeNode<T>> node = new AtomicReference<SparseTreeNode<T>>();
        final boolean exactMatch = findClosestNode(token, tokenElements, rootNode, node);
        SparseTreeNode<T> closestNode = node.get();

        // Add the node to the tree if is not already in the tree
        if (!exactMatch) {
            final SparseTreeNode<T> newNode = new SparseTreeNode<T>(token, tokenElements, referencedObject);

            // If there was no parent found the closest parent is the root
            if (closestNode == null) {
                closestNode = rootNode;
            }

            addNode(closestNode, newNode);
        } else if (overwrite) {
            // Update the referenced object for this node
            closestNode.referencedObject = referencedObject;
        } else {
            // Item already exists
            throw new IllegalArgumentException("A node with the same token already exists in the SparseTree."); //$NON-NLS-1$
        }
    }

    /**
     * Given a token, invokes the callback provided. The callback will be
     * provided with the referenced object for that token, if one exists. The
     * callback then should return the new referenced object for the token. The
     * tree will be updated accordingly.
     *
     *
     * @param token
     *        Token to look up
     * @param callback
     *        Callback to invoke
     * @param param
     *        Object passed through for your own use
     */
    public void modifyInPlace(String token, final ModifyInPlaceCallback<T> callback, final Object param) {
        Check.notNull(token, "token"); //$NON-NLS-1$

        // Canonicalize the input.
        token = canonicalizeToken(token);

        T referencedObject = null;
        final String[] tokenElements = splitToken(token);

        // Find the closest parent and child node
        final AtomicReference<SparseTreeNode<T>> node = new AtomicReference<SparseTreeNode<T>>();
        final boolean exactMatch = findClosestNode(token, tokenElements, rootNode, node);
        SparseTreeNode<T> closestNode = node.get();

        if (exactMatch) {
            referencedObject = closestNode.referencedObject;
        }

        // Invoke the callback
        referencedObject = callback.invoke(token, referencedObject, param);

        // Add the node to the tree if is not already in the tree
        if (!exactMatch) {
            final SparseTreeNode<T> newNode = new SparseTreeNode<T>(token, tokenElements, referencedObject);

            // If there was no parent found the closest parent is the root
            if (closestNode == null) {
                closestNode = rootNode;
            }

            addNode(closestNode, newNode);
        } else {
            closestNode.referencedObject = referencedObject;
        }
    }

    /**
     * Removes a token from the store. If recurse is true, all of the children
     * of the token will be removed as well.
     *
     *
     * @param token
     *        The token to remove.
     * @param removeChildren
     *        True if all of the children should be removed as well.
     * @return True if something was removed.
     */
    public boolean remove(String token, final boolean removeChildren) {
        Check.notNull(token, "token"); //$NON-NLS-1$

        // Canonicalize the input.
        token = canonicalizeToken(token);

        final String[] tokenElements = splitToken(token);

        final AtomicReference<SparseTreeNode<T>> node = new AtomicReference<SparseTreeNode<T>>();
        final boolean isSpecifiedToken = findClosestNode(token, tokenElements, rootNode, node);
        final SparseTreeNode<T> nodeToInvestigate = node.get();

        // If we found the exact node we were looking for, remove it.
        if (isSpecifiedToken) {
            return removeNode(nodeToInvestigate, removeChildren);
        }

        // We found a node which may have children of the specified token. If we
        // are removing
        // children of token, then we should remove the range of matching
        // children.
        if (removeChildren) {
            final SparseTreeRange range = findRange(tokenElements, tokenElements.length, 0, nodeToInvestigate);

            if (range.getStart() >= 0) {
                int removedCount = 0;

                for (int i = range.getStart(); i <= range.getEnd(); i++) {
                    // Null m_parent to invalidate it if cached (see IsRooted
                    // and FindClosestNode for details)
                    nodeToInvestigate.children.get(i).parent = null;

                    // Count the number of nodes being removed
                    removedCount += countNode(nodeToInvestigate.children.get(i));
                }

                // Remove range from list.
                final int start = range.getStart();
                int count = range.getEnd() - range.getStart() + 1;
                for (int i = 0; i < count; i++) {
                    nodeToInvestigate.children.remove(start);
                }

                // We have removed removedCount nodes from the SparseTree.
                count -= removedCount;

                return true;
            }
        }

        return false;
    }

    public T get(final String token) {
        return get(token, true);
    }

    public T get(String token, final boolean exactMatch) {
        // We require a non-null or empty token
        if (token == null) {
            throw new IllegalArgumentException("token"); //$NON-NLS-1$
        }

        // Canonicalize the input.
        token = canonicalizeToken(token);

        final String[] tokenElements = splitToken(token);

        final AtomicReference<SparseTreeNode<T>> closestNode = new AtomicReference<SparseTreeNode<T>>();
        final boolean found = findClosestNode(token, tokenElements, rootNode, closestNode);
        final SparseTreeNode<T> node = closestNode.get();

        if (!found) {
            if (exactMatch || node == rootNode) {
                return null;
            } else {
                return node.referencedObject;
            }
        }

        return node.referencedObject;
    }

    public void set(final String token, final T referencedObject) {
        add(token, referencedObject, true);
    }

    /**
     * Clears all of the tokens in the store.
     */
    public void clear() {
        rootNode = new SparseTreeNode<T>("", new String[0], null); //$NON-NLS-1$
        count = 0;
    }

    /**
     * Given a token that is in the store, returns the children of that node in
     * the SparseTree. Because this data structure is a SparseTree, the children
     * may not be immediate children, but there are no items in the SparseTree
     * between token and the returned children.
     *
     * This method can be used to find the root(s) of the SparseTree by passing
     * null as the token to query.
     *
     * If you are not sure, you probably want to be calling EnumSubTree with a
     * depth value of 1 instead.
     *
     * @param token
     *        Token to query (null for the root)
     * @return Array of referenced objects
     * @throws KeyNotFoundException
     */
    @SuppressWarnings("unchecked")
    public KeyValuePair<String, T>[] EnumChildren(String token) throws KeyNotFoundException {
        SparseTreeNode<T> node = rootNode;

        // Canonicalize the input.
        token = canonicalizeToken(token);

        if (null != token) {
            final String[] tokenElements = splitToken(token);

            final AtomicReference<SparseTreeNode<T>> closestNode = new AtomicReference<SparseTreeNode<T>>();
            final boolean found = findClosestNode(token, tokenElements, rootNode, closestNode);
            node = closestNode.get();

            if (!found) {
                throw new KeyNotFoundException();
            }
        }

        final ArrayList<KeyValuePair<String, T>> toReturn =
            new ArrayList<KeyValuePair<String, T>>(node.getChildCount());

        if (node.getChildCount() > 0) {
            for (int i = 0; i < node.children.size(); i++) {
                toReturn.add(
                    new KeyValuePair<String, T>(node.children.get(i).token, node.children.get(i).referencedObject));
            }
        }

        return (KeyValuePair<String, T>[]) toReturn.toArray();
    }

    /**
     * Enumerates all parents of the specified token. Only nodes that have been
     * specifically added to the SparseTree are enumerated. There is no
     * requirement that the specified token exist in the tree. If it does exist,
     * it is the first node enumerated. The provided callback can return true at
     * any time to halt the enumeration.
     *
     * @param token
     *        The token whose parents should be enumerated
     * @param callback
     *        The callback invoked for each enumerated node
     * @return True if the enumeration was stopped by callback
     */
    public boolean EnumParents(final String token, final EnumNodeCallback<T> callback) {
        return EnumParents(token, callback, EnumParentsOptions.NONE, null, null);
    }

    /**
     * Enumerates all parents of the specified token. Only nodes that have been
     * specifically added to the SparseTree are enumerated, unless the
     * EnumerateSparseNodes flag is passed as part of the options parameter.
     * There is no requirement that the specified token exist in the tree. If it
     * does exist, it is the first node enumerated. The provided callback can
     * return true at any time to halt the enumeration.
     *
     *
     * @param token
     *        The token whose parents we are enumerating.
     * @param callback
     *        The callback to make each time we hit a node.
     * @param options
     *        EnumParentsOptions flags to control behavior
     * @param additionalData
     *        If provided, this instance of SparseTreeAdditionalData will be
     *        populated with additional data and passed to you each time the
     *        callback is invoked. Can be null.
     * @param param
     *        Object passed through for your own use
     * @return True if we should halt our enumeration
     */
    public boolean EnumParents(
        final String token,
        final EnumNodeCallback<T> callback,
        EnumParentsOptions options,
        final SparseTreeAdditionalData additionalData,
        final Object param) {
        // This is a shim for (legacy) callback-based enumeration of the
        // SparseTree.
        // The legacy callback-based functions have the following contract
        // around the additionalData parameter.
        // 1. If null is passed, then the caller does not need any of the
        // additional data on the
        // SparseTreeAdditionalData structure.
        // 2. If the caller does need the additional data, the caller is
        // responsible for providing its own
        // instance of SparseTreeAdditionalData, which EnumParents/EnumSubTree
        // will mutate and provide
        // back to the caller as a parameter to each callback.
        // The idea of using a single instance like this is not compatible with
        // the IEnumerable-based APIs.
        // Instead, the SparseTreeAdditionalData structure is retired and that
        // data is returned as part of
        // the EnumeratedSparseTreeNode object. For precise backward
        // compatibility we retain the semantics
        // of the SparseTreeAdditionalData here, including using the single
        // caller-provided instance.
        options = options.remove(EnumParentsOptions.INCLUDE_ADDITIONAL_DATA);

        if (null != additionalData) {
            options = options.combine(EnumParentsOptions.INCLUDE_ADDITIONAL_DATA);
        }

        for (final EnumeratedSparseTreeNode<T> node : EnumParents(token, options)) {
            if (null != additionalData) {
                additionalData.hasChildren = node.hasChildren;
                additionalData.noChildrenBelow = node.noChildrenBelow;
            }

            if (callback.invoke(node.token, node.referencedObject, additionalData, param)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Queries the SparseTree for the parents of the provided token. Only nodes
     * that have been specifically added to the SparseTree are enumerated,
     * unless the EnumerateSparseNodes flag is passed as part of the options
     * parameter. There is no requirement that the specified token exist in the
     * tree. Nodes are enumerated from token to the root of the tree in a
     * streaming fashion. Do not modify the SparseTree while enumerating.
     *
     *
     * @param token
     *        The token whose parents to retrieve
     * @param options
     *        EnumParentsOptions flags to control behavior
     * @return The parents of the provided token
     */
    public Iterable<EnumeratedSparseTreeNode<T>> EnumParents(final String token, final EnumParentsOptions options) {
        return new EnumParentsEnumerable(this, token, options);
    }

    /**
     * Queries the SparseTree for the parents of the provided token. Only nodes
     * that have been specifically added to the SparseTree are enumerated,
     * unless the EnumerateSparseNodes flag is passed as part of the options
     * parameter. There is no requirement that the specified token exist in the
     * tree. Nodes are enumerated from token to the root of the tree in a
     * streaming fashion.
     *
     * Do not modify the SparseTree while enumerating.
     *
     *
     * @param token
     *        The token whose parents to retrieve
     * @param options
     *        EnumParentsOptions flags to control behavior
     */
    public Iterable<T> EnumParentsReferencedObjects(final String token, final EnumParentsOptions options) {
        return new ReferencedObjectEnumerable<T>(EnumParents(token, options));
    }

    /**
     * This will enumerate the entire subtree of the path supplied. The root of
     * the subtree will not be enumerated. Only elements that have been
     * specifically added to the SparseTree are returned. The traversal of the
     * tree is depth-first.
     *
     * @param token
     *        The token whose subtree to enumerate
     * @param callback
     *        The callback we are invoking on each node
     * @return True if we should stop our enumeration.
     */
    public boolean EnumSubTree(final String token, final EnumNodeCallback<T> callback) {
        return EnumSubTree(token, callback, EnumSubTreeOptions.NONE, Integer.MAX_VALUE, null, null);
    }

    /**
     * This will enumerate the entire subtree of the path supplied. The root of
     * the subtree will not be enumerated unless the EnumerateSubTreeRoot flag
     * is specified. Only elements that have been specifically added to the
     * SparseTree are returned, unless the EnumerateSparseNodes flag is
     * specified. The traversal of the tree is depth-first. A parameter may be
     * passed through for your own use.
     *
     *
     * @param token
     *        The token whose subtree to enumerate
     * @param callback
     *        The callback we are invoking on each node
     * @param options
     *        EnumSubTreeOptions flags to control behavior
     * @param depth
     *        The depth of the enumeration. Default is Int32.MaxValue
     * @param additionalData
     *        If provided, this instance of SparseTreeAdditionalData will be
     *        populated with additional data and passed to you each time the
     *        callback is invoked.
     * @param param
     *        Object passed through for your own use
     * @return True if we should stop our enumeration.
     */
    public boolean EnumSubTree(
        final String token,
        final EnumNodeCallback<T> callback,
        EnumSubTreeOptions options,
        final int depth,
        final SparseTreeAdditionalData additionalData,
        final Object param) {
        options = options.remove(EnumSubTreeOptions.INCLUDE_ADDITIONAL_DATA);

        if (null != additionalData) {
            options = options.combine(EnumSubTreeOptions.INCLUDE_ADDITIONAL_DATA);
        }

        for (final EnumeratedSparseTreeNode<T> node : EnumSubTree(token, options, depth)) {
            if (null != additionalData) {
                additionalData.hasChildren = node.hasChildren;
                additionalData.noChildrenBelow = node.noChildrenBelow;
            }

            if (callback.invoke(node.token, node.referencedObject, additionalData, param)) {
                return true;
            }
        }

        return false;
    }

    /**
     * This will enumerate the entire subtree of the path supplied. The root of
     * the subtree will not be enumerated unless the EnumerateSubTreeRoot flag
     * is specified. Only elements that have been specifically added to the
     * SparseTree are returned, unless the EnumerateSparseNodes flag is
     * specified. The traversal of the tree is depth-first.
     *
     *
     * @param token
     *        The token whose parents to retrieve
     * @param options
     *        EnumSubTreeOptions flags to control behavior
     * @param depth
     * @return The children of the provided token
     */
    public Iterable<EnumeratedSparseTreeNode<T>> EnumSubTree(
        final String token,
        final EnumSubTreeOptions options,
        final int depth) {
        return new EnumSubTreeEnumerable(this, token, options, depth);
    }

    /**
     * This will enumerate the entire subtree of the path supplied. The root of
     * the subtree will not be enumerated unless the EnumerateSubTreeRoot flag
     * is specified. Only elements that have been specifically added to the
     * SparseTree are returned, unless the EnumerateSparseNodes flag is
     * specified. The traversal of the tree is depth-first.
     *
     *
     * @param token
     *        The token whose parents to retrieve
     * @param options
     *        EnumSubTreeOptions flags to control behavior
     * @param depth
     * @return The children of the provided token
     */
    public Iterable<T> EnumSubTreeReferencedObjects(
        final String token,
        final EnumSubTreeOptions options,
        final int depth) {
        return new ReferencedObjectEnumerable<T>(EnumSubTree(token, options, depth));
    }

    /**
     * This enumerates all the immediate children of the root node of the tree
     * -- that is, all entries in the SparseTree which have no parent.
     *
     *
     * @return all immediate children of the root node of the tree.
     */
    public Iterable<EnumeratedSparseTreeNode<T>> EnumRoots() {
        final int size = rootNode.getChildCount();
        final List<EnumeratedSparseTreeNode<T>> toReturn = new ArrayList<EnumeratedSparseTreeNode<T>>(size);

        for (int i = 0; i < size; i++) {
            toReturn.add(
                new EnumeratedSparseTreeNode<T>(
                    rootNode.children.get(i).token,
                    rootNode.children.get(i).referencedObject,
                    rootNode.children.get(i).getChildCount() != 0,
                    null));
        }

        return toReturn;
    }

    /**
     * This enumerates all the immediate children of the root node of the tree
     * -- that is, all entries in the SparseTree which have no parent.
     *
     *
     * @return all immediate children of the root node of the tree.
     */
    public Iterable<T> EnumRootsReferencedObjects() {
        final int size = rootNode.getChildCount();
        final List<T> toReturn = new ArrayList<T>(size);

        for (int i = 0; i < size; i++) {
            toReturn.add(rootNode.children.get(i).referencedObject);
        }

        return toReturn;
    }

    /**
     * Checks to see if a SparseTreeNode's parents can be followed all the way
     * up to the root node. This method is used in validating whether or not a
     * cached FindClosestNode result is still valid, since the tree could have
     * changed in the intervening time.
     *
     *
     * @param node
     * @return
     */
    private boolean isRooted(SparseTreeNode<T> node) {
        while (node.parent != null) {
            node = node.parent;
        }

        // When a node is unlinked from the tree, we make sure to clear its
        // parent so that future calls to IsRooted on that node fail and we do
        // not use it as valid cached data.
        return node == rootNode;
    }

    /**
     * Finds the closest node to the token.
     *
     *
     * @param token
     *        The canonicalized token we're looking for
     * @param tokenElements
     *        The elements of the token we're looking for
     * @param nodeList
     *        The current nodeList to look for the token in.
     * @param closestNode
     *        The current closest node.
     * @return True if the node found is the exact node we were looking for.
     */
    private boolean findClosestNode(
        final String token,
        final String[] tokenElements,
        SparseTreeNode<T> nodeList,
        final AtomicReference<SparseTreeNode<T>> closestNode) {
        int elementCount = 0;
        final SparseTreeNode<T> cachedNode = getFindClosestNodeCache();

        if (null != cachedNode
            && null != cachedNode.parent
            && isSubItem(token, cachedNode.token)
            && isRooted(cachedNode)) {
            // OK, we have a valid cached entry which can help us out here.
            // This call to FindClosestNode will start doing looking for the
            // target
            // node starting from ts_findClosestNodeCache, instead of
            // m_rootNode.

            nodeList = cachedNode;
            elementCount = cachedNode.tokenElements.length;

            if (elementCount == tokenElements.length) {
                // Exact match
                closestNode.set(cachedNode);
                return true;
            }
        }

        final boolean result = findClosestNodeHelper(tokenElements, elementCount, nodeList, closestNode);

        // Cache this result for future calls.
        setFindClosestNodeCache(closestNode.get());

        return result;
    }

    /**
     * Finds the closest node to the token.
     *
     *
     * @param tokenElements
     *        The token we are looking for.
     * @param elementIndex
     *        The index of the element that we are looking at.
     * @param nodeList
     *        The current nodeList to look for the token in.
     * @param closestNode
     *        The current closest node.
     * @return True if the node found is the exact node we were looking for.
     */
    private boolean findClosestNodeHelper(
        final String[] tokenElements,
        final int elementIndex,
        final SparseTreeNode<T> nodeList,
        final AtomicReference<SparseTreeNode<T>> closestNode) {
        closestNode.set(nodeList);

        int startChildIndex = 0;
        int endChildIndex = nodeList.getChildCount() - 1;
        int midChildIndex = 0;

        while (startChildIndex <= endChildIndex) {
            // Compare against the item in the mid point of this range
            midChildIndex = startChildIndex + ((endChildIndex - startChildIndex) >> 1);
            final SparseTreeNode<T> childNode = nodeList.children.get(midChildIndex);

            int currentElementIndex = elementIndex;
            int result;

            // If this element is Equal to our element then this node is
            // either a parent of our path, equal to our path, or is a
            // child or our path.
            while ((result = tokenComparison.compare(
                tokenElements[currentElementIndex],
                childNode.tokenElements[currentElementIndex])) == 0) {
                // If we are on our last element and the child has the same
                // amount of elements that we do then
                // this node is an exact match
                if (currentElementIndex == tokenElements.length - 1
                    && childNode.tokenElements.length == tokenElements.length) {
                    closestNode.set(childNode);
                    return true;
                }

                // If this element is equal to the last path element of the node
                // we are examining then this
                // node is at least the parent. Set it to the closest node and
                // then search through its sub tree.
                if (childNode.tokenElements.length < tokenElements.length
                    && currentElementIndex == (childNode.tokenElements.length - 1)) {
                    closestNode.set(childNode);

                    // Search the sub-tree for the nearest
                    return findClosestNodeHelper(tokenElements, currentElementIndex + 1, childNode, closestNode);
                }

                // Move to the next element in the list
                currentElementIndex++;

                // If we are past the end of our paths element list we are done
                if (currentElementIndex == tokenElements.length) {
                    break;
                }
            }

            // If result = 0 here that means that the item we are looking for
            // the closest node for is actually the parent of the item we were
            // just looking at. We can return right now saying that the item
            // that was passed in as the node list is the closest node
            if (result == 0) {
                return false;
            }

            // If the new value is less than the mid-point try insert
            // into the bottom half otherwise use the top half.
            if (result < 0) {
                endChildIndex = midChildIndex - 1;
            } else {
                startChildIndex = midChildIndex + 1;
            }
        }

        // If we get here then we can assume that nothing in nodeList matched
        // and that we should just be returning the parent.
        return false;
    }

    /**
     * Add is used to add a childNode to this node. If there are children of the
     * node being added currently attached directly to this node they will be
     * moved under this child node.
     *
     * @param parentNode
     * @param childNode
     *        The node we are adding to the parent.
     */
    private void addNode(final SparseTreeNode<T> parentNode, final SparseTreeNode<T> childNode) {
        final int childCount = parentNode.getChildCount();

        // Reparent childNode under parentNode.
        reparentNode(parentNode, childNode);

        // If the node list is empty then just add us to the list
        if (childCount == 0) {
            // If the list is not there yet, we need to create it.
            parentNode.children = new ArrayList<SparseTreeNode<T>>();
            parentNode.children.add(childNode);
        } else {
            // We need to figure out where to insert this child in the list to
            // maintain sorted order.
            int insertIndex;

            // Common case: See if this item belongs off of the end of the list.
            if (compareByElements(
                childNode.tokenElements,
                parentNode.children.get(childCount - 1).tokenElements,
                parentNode.tokenElements.length,
                parentNode.children.get(childCount - 1).tokenElements.length - 1) > 0) {
                // Common case: The item belongs at the end of the list.
                insertIndex = childCount;
            } else {
                // OK, do a binary search to find out where to insert.
                insertIndex = ~findLocation(childNode.tokenElements, parentNode);
            }

            if (insertIndex < childCount
                && 0 == compareByElements(
                    childNode.tokenElements,
                    parentNode.children.get(insertIndex).tokenElements,
                    parentNode.tokenElements.length,
                    childNode.tokenElements.length - 1)) {
                // Find out if parentNode has children that need to be
                // reparented under childNode.
                final SparseTreeRange childrenRange =
                    findRange(childNode.tokenElements, childNode.tokenElements.length, insertIndex, parentNode);

                // Create the children list on childNode, reserving enough space
                childNode.children =
                    new ArrayList<SparseTreeNode<T>>(childrenRange.getEnd() - childrenRange.getStart() + 1);

                // Add the nodes to the child, preserving the existing sorted
                // order on the range
                for (int i = childrenRange.getStart(); i <= childrenRange.getEnd(); i++) {
                    reparentNode(childNode, parentNode.children.get(i));
                    childNode.children.add(parentNode.children.get(i));
                }

                // Remove the nodes from the parent.
                final int start = childrenRange.getStart();
                final int count = childrenRange.getEnd() - childrenRange.getStart() + 1;
                for (int i = 0; i < count; i++) {
                    parentNode.children.remove(start);
                }
            }

            parentNode.children.add(insertIndex, childNode);
        }

        // There is now one more key/value pair in the SparseTree.
        count++;
    }

    /**
     * This is used to remove a single node, or the entire subtree from a
     * section of the hierarchy.
     *
     *
     * @param nodeToRemove
     * @param removeChildren
     *        If true, the children will be removed as well.
     * @return True if something was removed.
     */
    private boolean removeNode(final SparseTreeNode<T> nodeToRemove, final boolean removeChildren) {
        final SparseTreeNode<T> parent = nodeToRemove.parent;

        // Find our location in our parent
        final int removeIndex = findLocation(nodeToRemove.tokenElements, parent);

        if (removeIndex >= 0) {
            // Remove myself from my parent
            parent.children.remove(removeIndex);

            // Null this field to invalidate it if cached (see IsRooted and
            // FindClosestNode for details)
            nodeToRemove.parent = null;

            if (!removeChildren) {
                // If we are not removing the children add them to my parent
                if (nodeToRemove.getChildCount() > 0) {
                    // Find the index to insert the list of child nodes in the
                    // parent in order to maintain sorted order.
                    final int insertIndex = ~findLocation(nodeToRemove.children.get(0).tokenElements, parent);

                    for (int i = 0; i < nodeToRemove.children.size(); i++) {
                        reparentNode(parent, nodeToRemove.children.get(i));
                    }

                    // Insert the entire children set from the dead node into
                    // the parent.
                    parent.children.addAll(insertIndex, nodeToRemove.children);
                }

                // One item has been unlinked from the SparseTree.
                count--;
            } else {
                // Count the number of items that have been unlinked from the
                // SparseTree.
                count -= countNode(nodeToRemove);
            }
        }

        return removeIndex >= 0;
    }

    /**
     * Counts the number of nodes in a subtree of the SparseTree, including the
     * node itself.
     *
     *
     * @param nodeToCount
     *        Root of the subtree to count
     * @return Number of nodes in the subtree (returns at least 1)
     */
    private int countNode(final SparseTreeNode<T> nodeToCount) {
        // If the node has no children, the count is 1 (the node itself).
        if (0 == nodeToCount.getChildCount()) {
            return 1;
        }

        int count = 1;
        SparseTreeNode<T> currentNode;
        final Stack<SparseTreeNode<T>> nodeStack = new Stack<SparseTreeNode<T>>();

        nodeStack.push(nodeToCount);

        while (nodeStack.size() > 0) {
            currentNode = nodeStack.pop();

            if (currentNode.getChildCount() > 0) {
                for (final SparseTreeNode<T> childNode : currentNode.children) {
                    nodeStack.push(childNode);
                }
            }

            count++;
        }

        return count;
    }

    /**
     * Reparents childNode under parentNode, but does not add it to its children
     * list.
     *
     *
     * @param parentNode
     *        New parent node for childNode
     * @param childNode
     *        Node to point to parentNode
     */
    private void reparentNode(final SparseTreeNode<T> parentNode, final SparseTreeNode<T> childNode) {
        // Associate this child node with the parent
        childNode.parent = parentNode;

        // To save memory, throw away duplicate strings in the child node's
        // tokenElements array, and use the strings from the parent.
        for (int i = 0; i < parentNode.tokenElements.length; i++) {
            childNode.tokenElements[i] = parentNode.tokenElements[i];
        }
    }

    /**
     * Binary search the sorted children of a SparseTreeNode to find the index
     * of a child node.
     *
     *
     * @param tokenElements
     *        The token to search for
     * @param nodeToSearch
     *        The node whose children should be searched
     * @return The index of the specified child node, if found. If not found, a
     *         negative number which is the bitwise complement of the index
     *         where the element should be inserted to maintain the sorted order
     *         is returned.
     */
    private int findLocation(final String[] tokenElements, final SparseTreeNode<T> nodeToSearch) {
        final int childCount = nodeToSearch.getChildCount();

        if (childCount == 0) {
            // Should be inserted at index 0
            return ~0;
        }

        int startIndex = 0;
        int midIndex = 0;
        int endIndex = childCount - 1;
        int result = 0;

        while (startIndex <= endIndex) {
            // Compare against the item in the midpoint of the range.
            midIndex = startIndex + ((endIndex - startIndex) >> 1);

            result = compareByElements(
                tokenElements,
                nodeToSearch.children.get(midIndex).tokenElements,
                nodeToSearch.tokenElements.length,
                nodeToSearch.children.get(midIndex).tokenElements.length - 1);

            // If the item already exists just return
            if (result == 0) {
                return midIndex;
            }

            if (result > 0) {
                startIndex = midIndex + 1;
            } else {
                endIndex = midIndex - 1;
            }
        }

        return ~startIndex;
    }

    /**
     * Finds the range of elements in the sorted children list of nodeToSearch
     * which start with the first tokenElementsLength elements of tokenElements.
     * The sorted children list is binary searched twice starting at startIndex.
     *
     * If no child nodes are found from startIndex to
     * nodeToSearch.m_children.Count, the range (-1, -1) is returned. If at
     * least one child node which starts with the first tokenElementsLength
     * elements of tokenElements is found, then the returned range will have a
     * non-negative Start which contains the index of the first child node
     * meeting the criteria, and End will contain the index of the last child
     * node meeting the criteria.
     *
     * @param tokenElements
     *        Token elements of the range to match
     * @param tokenElementsLength
     *        Length of tokenElements array (can be shorter than
     *        tokenElements.Length)
     * @param startIndex
     *        The index in the sorted children list of nodeToSearch use as a
     *        floor for the binary search (normally 0)
     * @param nodeToSearch
     *        The node whose children list is to be searched.
     * @return A SparseTreeRange indicating the
     */
    private SparseTreeRange findRange(
        final String[] tokenElements,
        final int tokenElementsLength,
        int startIndex,
        final SparseTreeNode<T> nodeToSearch) {
        if (nodeToSearch.getChildCount() == 0) {
            return new SparseTreeRange(-1, -1);
        }

        final SparseTreeRange toReturn = new SparseTreeRange();

        // Binary search to find the start of the range.
        startIndex = startIndex - 1;
        int midIndex;
        int endIndex = nodeToSearch.children.size();

        while (endIndex - startIndex > 1) {
            // Compare against the item in the midpoint of the range.
            midIndex = startIndex + ((endIndex - startIndex) >> 1);
            final SparseTreeNode<T> childNode = nodeToSearch.children.get(midIndex);

            final int result = compareByElements(
                tokenElements,
                childNode.tokenElements,
                nodeToSearch.tokenElements.length,
                tokenElementsLength - 1);

            if (result > 0) {
                startIndex = midIndex;
            } else {
                endIndex = midIndex;
            }
        }

        if (endIndex == nodeToSearch.children.size()
            || compareByElements(
                tokenElements,
                nodeToSearch.children.get(endIndex).tokenElements,
                nodeToSearch.tokenElements.length,
                tokenElementsLength - 1) != 0) {
            toReturn.setStart(-1);
        } else {
            toReturn.setStart(endIndex);
        }

        // Binary search to find the end of the range.
        if (toReturn.getStart() >= 0) {
            endIndex = nodeToSearch.children.size();
            startIndex = toReturn.getStart();

            while (endIndex - startIndex > 1) {
                // Compare against the item in the midpoint of the range.
                midIndex = startIndex + ((endIndex - startIndex) >> 1);
                final SparseTreeNode<T> childNode = nodeToSearch.children.get(midIndex);

                final int result = compareByElements(
                    tokenElements,
                    childNode.tokenElements,
                    nodeToSearch.tokenElements.length,
                    tokenElementsLength - 1);

                if (result < 0) {
                    endIndex = midIndex;
                } else {
                    startIndex = midIndex;
                }
            }

            if (startIndex >= 0
                && compareByElements(
                    tokenElements,
                    nodeToSearch.children.get(startIndex).tokenElements,
                    nodeToSearch.tokenElements.length,
                    tokenElementsLength - 1) != 0) {
                toReturn.setEnd(-1);
            } else {
                toReturn.setEnd(startIndex);
            }
        }

        return toReturn;
    }

    /**
     * The same functionality as String.Compare except that it treats separators
     * as the heaviest weighted character.
     *
     *
     * @param tokenElements1
     *        The elements that make up the first token.
     * @param tokenElements2
     *        The elements that make up the second token.
     * @param tokenStartIndex
     *        The index to start comparing tokenElements1 and tokenElements2.
     * @param tokenEndIndex
     *        The index to stop the comparison of tokenElements1 and
     *        tokenElements2.
     * @return 0 if all the elements matched.
     */
    private int compareByElements(
        final String[] tokenElements1,
        final String[] tokenElements2,
        final int tokenStartIndex,
        int tokenEndIndex) {
        tokenEndIndex = tokenEndIndex + 1;

        int currentIndex = tokenStartIndex;

        // Calculate where to stop up front so we don't have to do multiple
        // checks on each iteration
        // int lastIndex = Math.Min(tokenElements1.Length,
        // tokenElements2.Length, tokenEndIndex);
        int lastIndex = tokenElements1.length > tokenElements2.length ? tokenElements2.length : tokenElements1.length;
        lastIndex = tokenEndIndex > lastIndex ? lastIndex : tokenEndIndex;

        // Compare each token element by element.
        while (currentIndex < lastIndex) {
            int ret;
            if ((ret = tokenComparison.compare(tokenElements1[currentIndex], tokenElements2[currentIndex])) != 0) {
                return ret;
            }
            currentIndex++;
        }

        // Check to see if it was one of the element array lengths that
        // terminated us and not the tokenEndIndex.
        // It is important we recognize the empty string as only equal to
        // another empty string.
        if (currentIndex == tokenEndIndex) {
            return 0;
        }

        // We get past the above check in the case that Max of the two lengths
        // were passed in.
        // if the elements evaluated equally, return the one that is shorter as
        // being valued less. Note
        // that this will return 0 in the case they have the same length.
        return tokenElements1.length - tokenElements2.length;
    }

    /**
     * Canonicalizes a token based on the type of splitting that has been
     * supplied.
     *
     *
     * @param token
     *        The token to canonicalize.
     * @return The canonicalized token.
     */
    private String canonicalizeToken(final String token) {
        if (null == token) {
            return null;
        }

        if (tokenSeparator != '\0') {
            return StringUtil.trimEnd(token, tokenSeparator);
        } else if (fixedElementLength > 0) {
            if (token.length() == 0) {
                throw new IllegalArgumentException(
                    "Empty string tokens are not supported with a fixed element length."); //$NON-NLS-1$
            }

            return token;
        }

        return null;
    }

    /**
     * Splits a canonicalized token into its elements based on the type of
     * splitting that has been supplied.
     *
     *
     * @param token
     *        The canonicalized token to split.
     * @return The array of elements that make up the token.
     */
    private String[] splitToken(final String token) {
        if (tokenSeparator != '\0') {
            if (token.length() == 0) {
                return EMPTY_STRING_ARRAY;
            }

            return token.split(Pattern.quote(new String(tokenSeparatorArray)));
        } else if (fixedElementLength > 0) {
            final int numElements = (token.length() / fixedElementLength);
            final String[] elements = new String[numElements];

            for (int i = 0; i < numElements; i++) {
                final int startOffset = i * fixedElementLength;
                final int endOffset = startOffset + fixedElementLength;
                elements[i] = token.substring(startOffset, endOffset);
            }

            return elements;
        }

        return null;
    }

    /**
     * Given an item and a possible parent, returns true if item is a subitem of
     * parent given the token separator or fixed element length. This check does
     * NOT depend on the state of the tree and is based solely on the contents
     * of the passed strings.
     *
     *
     * @param item
     *        Item to check
     * @param parent
     *        Potential parent of item
     * @return True if parent is a parent of item
     */
    public boolean isSubItem(final String item, final String parent) {
        if (parent.length() > item.length()) {
            return false;
        }

        final String itemPrefix = item.substring(0, parent.length());

        if (fixedElementLength > 0) {
            return tokenComparison.compare(parent, itemPrefix) == 0;
        } else if (tokenSeparator != '\0') {
            // This is the same check as FileSpec.IsSubItem.
            return tokenComparison.compare(parent, itemPrefix) == 0
                && (item.length() == parent.length()
                    || (parent.length() > 0 && parent.charAt(parent.length() - 1) == tokenSeparator)
                    || item.charAt(parent.length()) == tokenSeparator);
        }

        return false;
    }

    /**
     * Adds a token part to the specified StringBuilder.
     *
     *
     * @param builder
     *        StringBuilder to which the token part should be added
     * @param tokenCountInBuilder
     *        Number of tokens currently in the StringBuilder
     * @param tokenPart
     *        Token part to add
     */
    private void addTokenPart(final StringBuilder builder, final int tokenCountInBuilder, final String tokenPart) {
        if (tokenSeparator != '\0') {
            // We need to know the number of tokens currently in the
            // StringBuilder in order to accurately know whether or not we need
            // to add a separator. A buffer with the empty-string token added
            // looks the same as an empty buffer.
            if (tokenCountInBuilder > 0) {
                builder.append(tokenSeparator);
            }

            builder.append(tokenPart);
        } else if (fixedElementLength > 0) {
            builder.append(tokenPart);
        }
    }

    /**
     * Returns the number of tokens the two strings have in common, starting
     * from the provided start index.
     *
     *
     * @param xTokens
     *        Tokens of the first string
     * @param yTokens
     *        Tokens of the second string
     * @param startIndex
     *        Index to start counting from
     * @return Number of common tokens from startIndex
     */
    private int getCommonTokenCount(final String[] xTokens, final String[] yTokens, final int startIndex) {
        // int length = Math.Min(xTokens.Length, yTokens.Length);
        final int length = xTokens.length > yTokens.length ? yTokens.length : xTokens.length;

        if (startIndex > length - 1) {
            // The start index was beyond one or more of the arrays, so
            // there can be no common tokens past startIndex.
            return 0;
        }

        int i;

        for (i = startIndex; i < length; i++) {
            if (tokenComparison.compare(xTokens[i], yTokens[i]) != 0) {
                break;
            }
        }

        return i - startIndex;
    }

    /**
     * Given a (token, tokenElements) pair and a tokenCount, return a
     * StringBuilder containing tokenCount nodes from the (token, tokenElements)
     * pair.
     *
     *
     * @param toUse
     *        If you have an existing StringBuilder you want to use
     * @param token
     *        Token part of (token, tokenElements) pair
     * @param tokenElements
     *        tokenElements part of (token, tokenElements) pair
     * @param tokenCount
     *        Number of tokens from node to put in the StringBuilder
     * @return StringBuilder containing a substring of node.m_token
     */
    private StringBuilder getPartialTokenStringBuilder(
        StringBuilder toUse,
        final String token,
        final String[] tokenElements,
        final int tokenCount) {
        if (null == toUse) {
            toUse = new StringBuilder(token.length());
        } else {
            toUse.ensureCapacity(token.length());
            toUse.setLength(0);
        }

        if (tokenCount == tokenElements.length) {
            // Caller wants the entire token.
            toUse.append(token);
        } else if (tokenSeparator != '\0') {
            int length = 0;

            // Add the lengths of the tokens.
            for (int i = 0; i < tokenCount; i++) {
                length += tokenElements[i].length();
            }

            // Add the length for the separator characters.
            if (tokenCount > 1) {
                length += tokenCount - 1;
            }

            toUse.append(token.substring(0, length));
        } else if (fixedElementLength > 0) {
            // Fixed element length is just a simple substring.
            toUse.append(token.substring(0, tokenCount * fixedElementLength));
        }

        return toUse;
    }

    /**
     * Returns the number of key/value pairs in this SparseTree.
     */
    public int getCount() {
        return count;
    }

    /**
     * The optional separator character. If this is the null character, there is
     * no separator and the token assumed to be a fixed length meaning the
     * FixedElementLength will be used to split the tokens.
     */
    public char getTokenSeparator() {
        return tokenSeparator;
    }

    /**
     * The optional element length. If this is -1, a variable length token is
     * assumed and the TokenSeparator will be used to split the tokens.
     */
    public int getFixedElementLength() {
        return fixedElementLength;
    }

    /**
     * A thread-local cache to speed up access patterns that exhibit token
     * locality (FindClosestNode)
     */

    private SparseTreeNode<T> getFindClosestNodeCache() {
        if (null == ts_findClosestNodeCache) {
            return null;
        }

        return ts_findClosestNodeCache.get();
    }

    private void setFindClosestNodeCache(final SparseTreeNode<T> value) {
        ts_findClosestNodeCache = new WeakReference<SparseTreeNode<T>>(value);
    }

    private enum State {
        InitialNode, SparseNodes, Normal
    }

    private class EnumParentsEnumerable implements Iterable<EnumeratedSparseTreeNode<T>> {
        public EnumParentsEnumerable(
            final SparseTree<T> sparseTree,
            final String token,
            final EnumParentsOptions options) {
            this.sparseTree = sparseTree;
            this.token = token;
            this.options = options;
        }

        @Override
        public Iterator<EnumeratedSparseTreeNode<T>> iterator() {
            return new EnumParentsEnumerator(sparseTree, token, options);
        }

        private final SparseTree<T> sparseTree;
        private final String token;
        private final EnumParentsOptions options;
    }

    private class EnumParentsEnumerator implements Iterator<EnumeratedSparseTreeNode<T>> {
        // Immutable members (initialization state)
        private final SparseTree<T> sparseTree;
        private String token;
        private final EnumParentsOptions options;

        // Enumerator state
        private EnumeratedSparseTreeNode<T> current;
        private String[] tokenElements;
        private SparseTreeNode<T> currentParent;
        private boolean isSpecifiedNode;
        private boolean hasChildren;
        private int enumTokenLength;
        private StringBuilder sparseNodeBuffer;

        public EnumParentsEnumerator(
            final SparseTree<T> sparseTree,
            final String token,
            final EnumParentsOptions options) {
            this.sparseTree = sparseTree;
            this.token = token;
            this.options = options;

            reset();
        }

        private boolean moveNext() {
            if (null == currentParent) {
                current = null;
                return false;
            }

            if (options.contains(EnumParentsOptions.ENUMERATE_SPARSE_NODES)) {
                if (currentParent.tokenElements.length < enumTokenLength) {
                    sparseNodeBuffer = sparseTree.getPartialTokenStringBuilder(
                        sparseNodeBuffer,
                        token,
                        tokenElements,
                        enumTokenLength);

                    final String sparseToken = sparseNodeBuffer.toString();

                    current = new EnumeratedSparseTreeNode<T>(sparseToken, null, false, null);

                    if (options.contains(EnumParentsOptions.INCLUDE_ADDITIONAL_DATA)) {
                        // If we haven't gotten to a node with children yet, see
                        // if the node we're on has
                        // children, and if it does, if any of those children
                        // are below the sparse node that
                        // we're enumerating.
                        if (!hasChildren && currentParent.getChildCount() > 0) {
                            hasChildren =
                                sparseTree.findRange(tokenElements, enumTokenLength, 0, currentParent).getStart() >= 0;
                        }

                        current.hasChildren = hasChildren;

                        // TODO: Here when the sparse node has no
                        // children, we return the token of the sparse node.
                        // When enumerating sparse nodes this path can actually
                        // get shorter as we enumerate nodes. So the
                        // consumer of EnumParents (when specifying the
                        // EnumerateSparseNodes option) may not get the 'best'
                        // (i.e. shortest in # of path parts) answer for
                        // NoChildrenBelow (over the entire enumeration) on the
                        // first item enumerated. We should improve this
                        // behavior, and return the best answer on each sparse
                        // node, until it is no longer valid and NoChildrenBelow
                        // goes to null permanently.
                        current.noChildrenBelow = hasChildren ? null : sparseToken;
                    }

                    // A node was enumerated, decrement the counter.
                    enumTokenLength--;

                    return true;
                }
            }

            // Don't enumerate the root node.
            if (null != currentParent.parent) {
                current =
                    new EnumeratedSparseTreeNode<T>(currentParent.token, currentParent.referencedObject, false, null);

                if (options.contains(EnumParentsOptions.INCLUDE_ADDITIONAL_DATA)) {
                    // If we haven't already gotten to a node with children, see
                    // if the node we're on has children.
                    if (!hasChildren) {
                        hasChildren = currentParent.getChildCount() > 0;

                        // We are checking to see if we have just made the false
                        // -> true transition for hasChildren (which
                        // can only occur once during the enumeration).
                        if (hasChildren && !isSpecifiedNode) {
                            // Here's the special logic for NoChildrenBelow --
                            // the only time the value is something other
                            // than null or the token enumerated (i.e. the only
                            // time it carries more data than the HasChildren
                            // boolean itself).
                            int noChildrenBelowTokenCount = currentParent.tokenElements.length;
                            SparseTreeRange range = new SparseTreeRange(0, currentParent.children.size());

                            while (range.getStart() >= 0) {
                                if (noChildrenBelowTokenCount >= tokenElements.length) {
                                    // We've narrowed the range to cover just
                                    // the children of the token being
                                    // requested,
                                    // but we still have a valid range. This
                                    // means that there are children which fall
                                    // below the token being requested. We
                                    // should return null for NoChildrenBelow.
                                    noChildrenBelowTokenCount = 0;
                                    break;
                                }

                                // TODO: We should enhance FindRange
                                // to take an endIndex as well, so we can
                                // bound this binary search on both ends as we
                                // move forward through the token elements.
                                range = sparseTree.findRange(
                                    tokenElements,
                                    ++noChildrenBelowTokenCount,
                                    range.getStart(),
                                    currentParent);
                            }

                            if (noChildrenBelowTokenCount > 0) {
                                current.noChildrenBelow = sparseTree.getPartialTokenStringBuilder(
                                    null,
                                    token,
                                    tokenElements,
                                    noChildrenBelowTokenCount).toString();
                            }
                        }
                    }

                    current.hasChildren = hasChildren;

                    // If we didn't come up with a special value for
                    // NoChildrenBelow, give it the standard treatment where
                    // it carries no more data than the HasChildren bool.
                    if (null == current.noChildrenBelow) {
                        current.noChildrenBelow = hasChildren ? null : currentParent.token;
                    }
                }

                // A node was enumerated, decrement the counter.
                enumTokenLength--;

                // Walk up the tree.
                currentParent = currentParent.parent;

                return true;
            }

            current = null;
            return false;
        }

        public void reset() {
            current = null;

            // Canonicalize and split the token
            this.token = sparseTree.canonicalizeToken(token);
            this.tokenElements = sparseTree.splitToken(token);
            this.enumTokenLength = tokenElements.length;

            final AtomicReference<SparseTreeNode<T>> closestNode = new AtomicReference<SparseTreeNode<T>>();
            this.isSpecifiedNode = sparseTree.findClosestNode(token, tokenElements, sparseTree.rootNode, closestNode);
            this.currentParent = closestNode.get();

            this.hasChildren = false;
            this.sparseNodeBuffer = null;
        }

        @Override
        public boolean hasNext() {
            if (current != null) {
                return true;
            }

            return moveNext();
        }

        @Override
        public EnumeratedSparseTreeNode<T> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            final EnumeratedSparseTreeNode<T> toReturn = current;
            moveNext();
            return toReturn;
        }

        @Override
        public void remove() {
            throw new NotYetImplementedException();
        }
    }

    private class EnumSubTreeEnumerable implements Iterable<EnumeratedSparseTreeNode<T>> {
        public EnumSubTreeEnumerable(
            final SparseTree<T> sparseTree,
            final String token,
            final EnumSubTreeOptions options,
            final int depth) {
            this.sparseTree = sparseTree;
            this.token = token;
            this.options = options;
            this.depth = depth;
        }

        @Override
        public Iterator<EnumeratedSparseTreeNode<T>> iterator() {
            return new EnumSubTreeEnumerator(sparseTree, token, options, depth);
        }

        private final SparseTree<T> sparseTree;
        private final String token;
        private final EnumSubTreeOptions options;
        private final int depth;
    }

    public class EnumSubTreeEnumerator implements Iterator<EnumeratedSparseTreeNode<T>> {
        // Immutable members (initialization state)
        private final SparseTree<T> sparseTree;
        private String token;
        private final EnumSubTreeOptions options;
        private final int depth;

        // Enumerator state
        private EnumeratedSparseTreeNode<T> current;
        private Stack<SparseTreeNode<T>> nodeStack;
        private String[] tokenElements;
        private SparseTreeNode<T> currentNode;
        private int nextSparseTokenCount;
        private SparseTreeNode<T> prevChildNode;
        private StringBuilder sparseNodeBuffer;
        private State state;

        public EnumSubTreeEnumerator(
            final SparseTree<T> sparseTree,
            final String token,
            final EnumSubTreeOptions options,
            final int depth) {
            this.sparseTree = sparseTree;
            this.token = token;
            this.options = options;
            this.depth = depth;

            reset();
        }

        public boolean moveNext() {
            Start: while (true) {
                if (State.InitialNode == state) {
                    // Reset() provided an initial node because of the
                    // EnumSubTreeRoot
                    // flag. Enumerate that node first.
                    state = State.Normal;

                    return (null != current);
                } else if (State.SparseNodes == state) {
                    // The EnumerateSparseNodes flag is set, so we need to check
                    // for
                    // and enumerate any sparse nodes on the way to
                    // m_currentNode
                    // before
                    // enumerating it.

                    // The two conditions are: Is there another sparse node
                    // before
                    // m_currentNode.m_token,
                    // and does our query depth permit enumerating this sparse
                    // node?
                    if (nextSparseTokenCount < currentNode.tokenElements.length - 1 &&
                        // WATCH OUT for overflow when working with m_depth,
                        // which
                        // is
                        // often Int32.MaxValue
                    nextSparseTokenCount - tokenElements.length + 1 <= depth) {
                        // m_nextSparseTokenCount + 1 is the number of token
                        // elements in the buffer.
                        sparseTree.addTokenPart(
                            sparseNodeBuffer,
                            nextSparseTokenCount,
                            currentNode.tokenElements[nextSparseTokenCount]);

                        nextSparseTokenCount++;

                        current = new EnumeratedSparseTreeNode<T>(sparseNodeBuffer.toString(), null, false, null);

                        if (options.contains(EnumSubTreeOptions.INCLUDE_ADDITIONAL_DATA)) {
                            // This is a sparse node on the way to an extant
                            // child.
                            current.hasChildren = true;
                            current.noChildrenBelow = null;
                        }

                        return true;
                    }

                    // Set prevChildNode for the next node to be enumerated, so
                    // that
                    // sparse nodes are not duplicated.
                    prevChildNode = currentNode;

                    // Before we switch back to the Normal state, make sure that
                    // we're actually supposed to enumerate
                    // m_currentNode. It might be beyond our depth, in which
                    // case we
                    // should null it out before jumping back.
                    if (depth < currentNode.tokenElements.length - tokenElements.length) {
                        currentNode = null;
                    }

                    state = State.Normal;
                    continue Start;
                } else
                /* if (State.Normal == m_state) */
                {
                    // We are proceeding through m_nodeStack and consuming
                    // nodes.
                    while (null == currentNode && nodeStack.size() > 0) {
                        final SparseTreeNode<T> poppedNode = nodeStack.pop();

                        if (options.contains(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES)) {
                            // The interesting case here where Math.Max matters
                            // is
                            // when m_token does not exist
                            // in the tree, so poppedNode.m_parent has fewer
                            // token
                            // elements than m_token.
                            int commonTokenCount =
                                Math.max(poppedNode.parent.tokenElements.length, tokenElements.length);

                            if (null != prevChildNode) {
                                // Add the number of nodes that poppedNode has
                                // in
                                // common with m_prevChildNode.
                                commonTokenCount += sparseTree.getCommonTokenCount(
                                    prevChildNode.tokenElements,
                                    poppedNode.tokenElements,
                                    commonTokenCount);
                            }

                            // Do we have sparse nodes to enumerate?
                            if (commonTokenCount < poppedNode.tokenElements.length - 1) {
                                sparseNodeBuffer = sparseTree.getPartialTokenStringBuilder(
                                    sparseNodeBuffer,
                                    poppedNode.token,
                                    poppedNode.tokenElements,
                                    commonTokenCount);

                                // We will switch to the SparseNodes state to
                                // generate these nodes. When it is done,
                                // then it will switch back to the Normal state.

                                nextSparseTokenCount = commonTokenCount;
                                currentNode = poppedNode;
                                state = State.SparseNodes;

                                continue Start;
                            }
                        }

                        final int poppedNodeDepth = depth - (poppedNode.tokenElements.length - tokenElements.length);

                        if (poppedNodeDepth >= 0) {
                            currentNode = poppedNode;
                        }
                    }

                    if (null != currentNode) {
                        current = new EnumeratedSparseTreeNode<T>(
                            currentNode.token,
                            currentNode.referencedObject,
                            false,
                            null);

                        if (options.contains(EnumSubTreeOptions.INCLUDE_ADDITIONAL_DATA)) {
                            current.hasChildren = currentNode.getChildCount() > 0;
                            current.noChildrenBelow = current.hasChildren ? null : currentNode.token;
                        }

                        final int currentNodeDepth = depth - (currentNode.tokenElements.length - tokenElements.length);

                        if (currentNodeDepth > 0 && currentNode.getChildCount() > 0) {
                            pushChildren(currentNode, 0, currentNode.children.size());
                        }

                        // We've completed the enumeration of this node.
                        currentNode = null;

                        return true;
                    }
                }
                break;
            }

            current = null;
            return false;
        }

        public void reset() {
            current = null;
            tokenElements = new String[0];
            nodeStack = new Stack<SparseTreeNode<T>>();
            state = State.Normal;

            // Canonicalize the token.
            token = sparseTree.canonicalizeToken(token);

            boolean isSpecifiedNode = true;
            SparseTreeNode<T> currentNode = sparseTree.rootNode;

            if (null != token) {
                tokenElements = sparseTree.splitToken(token);

                final AtomicReference<SparseTreeNode<T>> closestNode =
                    new AtomicReference<SparseTreeNode<T>>(currentNode);
                isSpecifiedNode = sparseTree.findClosestNode(token, tokenElements, sparseTree.rootNode, closestNode);
                currentNode = closestNode.get();
            }

            // There is no entry in the SparseTree for this token or any of
            // its children.
            if (null == currentNode) {
                return;
            }

            if (isSpecifiedNode) {
                if (options.contains(EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT) && null != token) {
                    current =
                        new EnumeratedSparseTreeNode<T>(currentNode.token, currentNode.referencedObject, false, null);

                    if (options.contains(EnumSubTreeOptions.INCLUDE_ADDITIONAL_DATA)) {
                        current.hasChildren = currentNode.getChildCount() > 0;
                        current.noChildrenBelow = current.hasChildren ? null : currentNode.token;
                    }

                    state = State.InitialNode;
                }

                if (depth > 0 && currentNode.getChildCount() > 0) {
                    pushChildren(currentNode, 0, currentNode.children.size());
                }
            } else if (currentNode.getChildCount() > 0) {
                final EnumSubTreeOptions enumSparseRoot =
                    EnumSubTreeOptions.ENUMERATE_SUB_TREE_ROOT.combine(EnumSubTreeOptions.ENUMERATE_SPARSE_NODES);

                final SparseTreeRange range = sparseTree.findRange(tokenElements, tokenElements.length, 0, currentNode);

                if (range.getStart() >= 0) {
                    if (options.contains(enumSparseRoot)) {
                        current = new EnumeratedSparseTreeNode<T>(token, null, false, null);

                        if (options.contains(EnumSubTreeOptions.INCLUDE_ADDITIONAL_DATA)) {
                            current.hasChildren = true;
                            current.noChildrenBelow = null;
                        }

                        state = State.InitialNode;
                    }

                    if (depth > 0) {
                        pushChildren(currentNode, range.getStart(), range.getEnd() - range.getStart() + 1);
                    }
                }
            }
        }

        private void pushChildren(final SparseTreeNode<T> node, final int startIndex, final int length) {
            for (int i = startIndex + length - 1; i >= startIndex; i--) {
                nodeStack.push(node.children.get(i));
            }
        }

        @Override
        public boolean hasNext() {
            if (current != null && state != State.InitialNode) {
                return true;
            }
            return moveNext();
        }

        @Override
        public EnumeratedSparseTreeNode<T> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            final EnumeratedSparseTreeNode<T> toReturn = current;
            moveNext();
            return toReturn;
        }

        @Override
        public void remove() {
            throw new NotYetImplementedException();
        }
    }

    /**
     * For callers who want to enumerate just the referenced objects, return a
     * ReferencedObjectEnumerable instance instead.
     *
     * @threadsafety unknown
     */
    private class ReferencedObjectEnumerable<X> implements Iterable<X>, Iterator<X> {
        private final Iterator<EnumeratedSparseTreeNode<X>> iterator;

        public ReferencedObjectEnumerable(final Iterable<EnumeratedSparseTreeNode<X>> iterable) {
            iterator = iterable.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public X next() {
            return iterator.next().referencedObject;
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        @Override
        public Iterator<X> iterator() {
            return this;
        }
    }
}
