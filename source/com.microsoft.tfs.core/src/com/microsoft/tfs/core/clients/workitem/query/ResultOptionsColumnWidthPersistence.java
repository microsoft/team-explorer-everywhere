// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.util.StringUtil;

/**
 * <p>
 * Persists column widths for result options so they can be used later. All data
 * is stored in one large memento (the top-level memento holds child mementos
 * identified by keys specific to the results options), which is persisted via
 * {@link MementoRepository}.
 * </p>
 * <p>
 * This class supports internal UI code and is not public API.
 * </p>
 */
public class ResultOptionsColumnWidthPersistence {
    /*
     * TODO This class should really move into UI space. There is no UI type
     * leakage here, but it's really UI's concern which column widths are
     * persisted, and how (if they're shared between UI apps, etc.).
     */

    private static final String PERSISTENCE_KEY = "com.microsoft.tfs.client.common.wit.resultcolumnwidths"; //$NON-NLS-1$

    private static final String ROOT_MEMENTO_NAME = "ColumnWidths"; //$NON-NLS-1$

    private static final String KEY_PREFIX = "doc-"; //$NON-NLS-1$

    public static void restore(final MementoRepository mementos, final QueryDocument queryDocument) {
        final String documentKey = getKeyForQueryDocument(queryDocument);
        if (documentKey == null) {
            return;
        }

        /*
         * Load the root memento.
         */
        final Memento rootMemento = mementos.load(PERSISTENCE_KEY);
        final String rootMementoName = rootMemento != null ? rootMemento.getName() : null;
        if (rootMemento == null
            || StringUtil.isNullOrEmpty(rootMementoName)
            || !rootMementoName.equals(ROOT_MEMENTO_NAME)) {
            return;
        }

        /*
         * The top-level memento holds a child for each document key.
         */
        queryDocument.getResultOptions().loadFromMemento(rootMemento.getChild(documentKey));
    }

    public static void persist(final MementoRepository mementos, final QueryDocument queryDocument) {
        final String documentKey = getKeyForQueryDocument(queryDocument);
        if (documentKey == null) {
            return;
        }

        /*
         * Load the root memento, or create if it didn't exist or corrupted
         */
        Memento rootMemento = mementos.load(PERSISTENCE_KEY);
        final String rootMementoName = rootMemento != null ? rootMemento.getName() : null;
        if (rootMemento == null
            || StringUtil.isNullOrEmpty(rootMementoName)
            || !rootMementoName.equals(ROOT_MEMENTO_NAME)) {
            rootMemento = new XMLMemento(ROOT_MEMENTO_NAME);
        } else {
            /*
             * Remove all existing children with the same document key. There
             * should only be 0 or 1.
             */
            rootMemento.removeChildren(documentKey);
        }

        queryDocument.getResultOptions().saveToMemento(rootMemento.createChild(documentKey));

        mementos.save(PERSISTENCE_KEY, rootMemento);

    }

    private static String getKeyForQueryDocument(final QueryDocument queryDocument) {
        if (queryDocument.getFile() != null) {
            return KEY_PREFIX + queryDocument.getFile().getAbsolutePath().hashCode();
        } else if (queryDocument.getGUID() != null) {
            return KEY_PREFIX + queryDocument.getGUID();
        }

        return null;
    }
}
