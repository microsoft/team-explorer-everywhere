// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.repository.cache.annotation;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Annotation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

public class AnnotationCache {
    private final Workspace workspace;

    private final Object lock = new Object();
    private final Map<AnnotationsKey, Annotation[]> annotationsCache = new HashMap<AnnotationsKey, Annotation[]>();

    private final static Log log = LogFactory.getLog(AnnotationCache.class);

    public AnnotationCache(final Workspace workspace) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        this.workspace = workspace;
    }

    public Annotation[] getAnnotations(
        final String annotationName,
        final String annotatedServerItem,
        final int version) {
        Check.isTrue(
            annotationName != null || annotatedServerItem != null,
            "one of annotationName or annotatedServerItem must not be null"); //$NON-NLS-1$

        Annotation[] annotations;
        final AnnotationsKey key = new AnnotationsKey(annotationName, annotatedServerItem);

        synchronized (lock) {
            annotations = annotationsCache.get(key);
        }

        if (annotations == null) {
            log.info(MessageFormat.format(
                "Querying server annotation {0} for {1} version {2}", //$NON-NLS-1$
                annotationName,
                annotatedServerItem,
                Integer.toString(version)));

            annotations = workspace.getClient().queryAnnotation(annotationName, annotatedServerItem, version);

            synchronized (lock) {
                annotationsCache.put(key, annotations);
            }
        }

        return annotations;
    }

    public String getAnnotationValue(final String annotationName, final String annotatedServerItem, final int version) {
        Check.notNull(annotationName, "annotationName"); //$NON-NLS-1$
        Check.notNull(annotatedServerItem, "annotatedServerItem"); //$NON-NLS-1$

        Annotation[] annotations = null;

        try {
            annotations = getAnnotations(annotationName, annotatedServerItem, version);
        } catch (final Exception e) {
            log.warn("Could not query annotation " + annotationName + " for " + annotatedServerItem, e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (annotations == null || annotations.length != 1) {
            return null;
        }

        return annotations[0].getValue();
    }

    public void refresh() {
        synchronized (lock) {
            annotationsCache.clear();
        }
    }

    private final class AnnotationsKey {
        private final String name;
        private final String serverItem;

        public AnnotationsKey(final String name, final String serverItem) {
            this.name = name;
            this.serverItem = serverItem;
        }

        @Override
        public int hashCode() {
            int result = 17;

            result = result * 37 + ((name == null) ? 0 : name.hashCode());
            result = result * 37 + ((serverItem == null) ? 0 : serverItem.hashCode());

            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof AnnotationsKey)) {
                return false;
            }
            if (obj == this) {
                return true;
            }

            final AnnotationsKey other = (AnnotationsKey) obj;

            return (name == null) ? other.name == null : name.equals(other.name) && (serverItem == null)
                ? other.serverItem == null : serverItem.equals(other.serverItem);
        }
    }
}
