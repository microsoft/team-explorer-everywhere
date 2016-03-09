// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.links;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.update.ElementHandler;

/**
 * The base class for a collections of work item components: either a links
 * collection or a file attachments collection.
 */
public abstract class WITComponentCollection<T> {
    private final Set<T> components = new HashSet<T>();
    private final WorkItemImpl workItem;

    protected WITComponentCollection(final WorkItemImpl workItem) {
        this.workItem = workItem;
    }

    public void preSave() throws UnableToSaveException {

    }

    public final WorkItem getWorkItem() {
        return workItem;
    }

    public final Set<T> getComponentSet() {
        return components;
    }

    public final WorkItemImpl getWorkItemInternal() {
        return workItem;
    }

    protected final boolean containsEquivalent(final WITComponent component) {
        for (final Iterator<T> it = components.iterator(); it.hasNext();) {
            final WITComponent collectionComponent = (WITComponent) it.next();
            if (!collectionComponent.isPendingDelete() && collectionComponent.isEquivalentTo(component)) {
                return true;
            }
        }

        return false;
    }

    protected final boolean addComponent(final WITComponent component) {
        if (component.isNewlyCreated()) {
            if (components.contains(component)) {
                return false;
            }
            if (containsEquivalent(component)) {
                return false;
            }
        }

        component.associate(this);
        possiblyChangedDirtyState();
        return true;
    }

    protected final Object[] getPublicComponents(final Object[] arrayType) {
        final Set<T> returnedComponents = new HashSet<T>();
        for (final T component : components) {
            final WITComponent witComponent = (WITComponent) component;
            if (witComponent.shouldIncludeAsPartOfPublicCollection()) {
                returnedComponents.add(component);
            }
        }
        return returnedComponents.toArray(arrayType);
    }

    public final int getCount(final Class<WITComponent> type, final boolean oldCount) {
        int count = 0;
        for (final T component : components) {
            if (type == null || type.isAssignableFrom(component.getClass())) {
                final WITComponent witComponent = (WITComponent) component;
                if ((oldCount && witComponent.shouldIncludeInOldCount())
                    || (!oldCount && witComponent.shouldIncludeInNewCount())) {
                    ++count;
                }
            }
        }
        return count;
    }

    public final ElementHandler[] addUpdateXML(final Element parentElement) {
        final List<ElementHandler> elementHandlers = new ArrayList<ElementHandler>();

        for (final T component : components) {
            final WITComponent witComponent = (WITComponent) component;
            final ElementHandler handler = witComponent.createXMLForUpdatePackage(parentElement);
            if (handler != null) {
                elementHandlers.add(handler);
            }
        }
        return elementHandlers.toArray(new ElementHandler[] {});
    }

    /**
     * This method is called after a work item has been updated (through a call
     * to the Update web service method). The .update() method of each component
     * in this component collection is called.
     */
    public final void update() {
        /*
         * We must iterate over a copy of the component set instead of the
         * actual set, since WITComponent.update() may modify the internal
         * component set.
         */
        final Collection<T> copy = new HashSet<T>(components);

        for (final T component : copy) {
            final WITComponent witComponent = (WITComponent) component;
            witComponent.update();
        }
    }

    /**
     * This method is called to reset this component collection. The .reset()
     * method of each component is called.
     */
    public void reset() {
        /*
         * We must iterate over a copy of the component set instead of the
         * actual set, since WITComponent.reset() may modify the internal
         * component set.
         */
        final Collection<T> copy = new HashSet<T>(components);

        for (final T component : copy) {
            final WITComponent witComponent = (WITComponent) component;
            witComponent.reset();
        }
    }

    protected final void removeComponent(final WITComponent component) {
        if (!components.contains(component)) {
            throw new IllegalArgumentException("this collection does not contain the specified component"); //$NON-NLS-1$
        }

        component.delete();
        possiblyChangedDirtyState();
    }

    /**
     * @return true if any of the components in this collection are dirty
     */
    public final boolean isDirty() {
        for (final T component : components) {
            final WITComponent witComponent = (WITComponent) component;
            if (witComponent.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public final void possiblyChangedDirtyState() {
        workItem.fireStateListenersIfNeeded();
    }

    /**
     * Removes all components in this component collection, no matter what their
     * state is.
     */
    public void internalClear() {
        components.clear();
    }
}
