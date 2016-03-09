// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata.impl;

import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.ConstantHandler;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.ConstantSet;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IConstantSet;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.IMetadataChangeListener;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.Metadata;

public class ConstantHandlerImpl implements ConstantHandler {
    private final Metadata metadata;
    private final Hashtable<String, IConstantSet> cache = new Hashtable<String, IConstantSet>();
    private final boolean disableCache = System.getProperty("com.microsoft.tfs.disable-constant-cache") != null; //$NON-NLS-1$
    private final static Log log = LogFactory.getLog(ConstantHandlerImpl.class);

    public ConstantHandlerImpl(final Metadata metadata) {
        this.metadata = metadata;
        metadata.addMetadataChangeListener(new IMetadataChangeListener() {
            @Override
            public void metadataChanged(final Set<String> tableNames) {
                cache.clear();
            }
        });
    }

    @Override
    public IConstantSet getConstantSet(
        final int rootConstantID,
        final boolean oneLevel,
        final boolean twoPlusLevels,
        final boolean leaf,
        final boolean interior,
        final boolean useCache) {
        final int[] rootConstantIDs = new int[1];
        rootConstantIDs[0] = rootConstantID;
        return getConstantSet(rootConstantIDs, oneLevel, twoPlusLevels, leaf, interior, useCache);
    }

    @Override
    public IConstantSet getConstantSet(
        final int[] rootConstantIDs,
        final boolean oneLevel,
        final boolean twoPlusLevels,
        final boolean leaf,
        final boolean interior,
        final boolean useCache) {
        if (!useCache || disableCache) {
            return new ConstantSet(metadata, rootConstantIDs, oneLevel, twoPlusLevels, leaf, interior);
        } else {
            final String key = createCacheKey(rootConstantIDs, oneLevel, twoPlusLevels, leaf, interior);
            if (!cache.containsKey(key)) {
                final IConstantSet set =
                    new ConstantSet(metadata, rootConstantIDs, oneLevel, twoPlusLevels, leaf, interior);
                cache.put(key, set);

                log.debug(MessageFormat.format(
                    "Cache constant set {0} count={1}, cache size={2}", //$NON-NLS-1$
                    key,
                    set.getSize(),
                    cache.size()));
            }
            return cache.get(key);
        }
    }

    private String createCacheKey(
        final int ids[],
        final boolean oneLevel,
        final boolean twoPlusLevels,
        final boolean leaf,
        final boolean interior) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sb.append("."); //$NON-NLS-1$
            }
            sb.append(ids[i]);
        }

        sb.append("."); //$NON-NLS-1$
        sb.append(oneLevel);
        sb.append("."); //$NON-NLS-1$
        sb.append(twoPlusLevels);
        sb.append("."); //$NON-NLS-1$
        sb.append(leaf);
        sb.append("."); //$NON-NLS-1$
        sb.append(interior);
        return sb.toString();
    }
}
