// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.util.StringUtil;

public class FindInSCEQueryOptionsPersistence {
    private static final String PERSISTENCE_KEY = "com.microsoft.tfs.client.common.ui.findinscequeryoptions"; //$NON-NLS-1$

    private static final String ROOT_MEMENTO_NAME = "FindInSCEQueryOptions"; //$NON-NLS-1$

    public static final String OPTION_MEMENTO_NAME = "option"; //$NON-NLS-1$

    public static final String OPTION_NAME = "name"; //$NON-NLS-1$

    public static final String VALUE_NAME = "value"; //$NON-NLS-1$

    final static Log log = LogFactory.getLog(FindInSCEQueryOptionsPersistence.class);

    public static void restore(final MementoRepository mementos, final FindInSourceControlQuery query) {
        /*
         * Load the root memento.
         */
        final Memento rootMemento = mementos.load(PERSISTENCE_KEY);
        final String rootMementoName = rootMemento != null ? rootMemento.getName() : null;

        if (rootMemento != null) {
            if (StringUtil.isNullOrEmpty(rootMementoName) || !rootMementoName.equals(ROOT_MEMENTO_NAME)) {
                log.warn("Find in SCE options cache is corrupted"); //$NON-NLS-1$
            } else {
                /*
                 * The top-level memento holds a child for each document key.
                 */
                query.loadFromMemento(rootMemento);
            }
        }
    }

    public static void persist(final MementoRepository mementos, final FindInSourceControlQuery query) {
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
             * Remove all existing children.
             */
            rootMemento.removeChildren(OPTION_MEMENTO_NAME);
        }

        query.saveToMemento(rootMemento);

        mementos.save(PERSISTENCE_KEY, rootMemento);

    }

}
