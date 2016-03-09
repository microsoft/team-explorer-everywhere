// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.links.WITComponentCollection;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateErrorCallback;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateFinishedCallback;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEnd;
import com.microsoft.tfs.core.clients.workitem.link.WorkItemLinkTypeEndCollection;

public class LinkCollectionImpl extends WITComponentCollection<Link> implements LinkCollection {
    LinkCollectionChangedListenerSupport listeners;
    Set<String> hashFieldReferenceNames = new HashSet<String>();

    public LinkCollectionImpl(final WorkItemImpl workItem) {
        super(workItem);

        listeners = new LinkCollectionChangedListenerSupport(this);

        // The title and id of all related links are required for the link
        // description field.
        hashFieldReferenceNames.add("system.id"); //$NON-NLS-1$
        hashFieldReferenceNames.add("system.title"); //$NON-NLS-1$
    }

    /*
     * ************************************************************************
     * START of implementation of LinkCollection interface
     * ***********************************************************************
     */

    @Override
    public Iterator<Link> iterator() {
        return getComponentSet().iterator();
    }

    @Override
    public int size() {
        return getComponentSet().size();
    }

    @Override
    public boolean contains(final Link link) {
        return containsEquivalent((LinkImpl) link);
    }

    @Override
    public boolean add(final Link link) {
        if (link instanceof RelatedLink) {
            if (((RelatedLink) link).getTargetWorkItemID() == getWorkItemInternal().getID()) {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        Messages.getString("LinkCollectionImpl.LinkingAWorkItemToItselfNotAllowedFormat"), //$NON-NLS-1$
                        Integer.toString(getWorkItemInternal().getID())));
            }
        }

        // Add the link to the collection and fire the event if successful.
        final boolean added = addComponent((LinkImpl) link);
        if (added) {
            listeners.fireLinkAdded(link);
        }
        return added;
    }

    @Override
    public void remove(final Link link) {
        removeComponent((LinkImpl) link);
        listeners.fireLinkRemoved(link);
    }

    public void linkTargetsUpdated() {
        listeners.fireLinkTargetsUpdated();
    }

    public Link[] getLinks() {
        return (Link[]) getPublicComponents(new Link[] {});
    }

    public void mergeColumnFieldReferences(final String[] fieldReferences) {
        for (int i = 0; i < fieldReferences.length; i++) {
            final String fieldReference = fieldReferences[i].toLowerCase();
            if (!hashFieldReferenceNames.contains(fieldReference)) {
                hashFieldReferenceNames.add(fieldReference);
            }
        }
    }

    public boolean allDescriptionsComputed() {
        final Link[] displayLinks = getLinks();
        for (int i = 0; i < displayLinks.length; i++) {
            if (!((LinkImpl) displayLinks[i]).isDescriptionComputed()) {
                return false;
            }
        }

        return true;
    }

    public Runnable getDescriptionUpdateRunnable(
        final DescriptionUpdateErrorCallback errorCallback,
        final DescriptionUpdateFinishedCallback finishedCallback) {
        final Map linkTypeToUpdater = new HashMap();
        final String[] fieldReferenceNames =
            hashFieldReferenceNames.toArray(new String[hashFieldReferenceNames.size()]);

        final Link[] displayLinks = getLinks();
        for (int i = 0; i < displayLinks.length; i++) {
            if (!((LinkImpl) displayLinks[i]).isDescriptionComputed()) {
                final Class type = displayLinks[i].getClass();
                LinkDescriptionUpdater updater = (LinkDescriptionUpdater) linkTypeToUpdater.get(type);
                if (updater == null) {
                    updater = LinkDescriptionUpdaterFactory.getDescriptionUpdater(
                        type,
                        fieldReferenceNames,
                        errorCallback,
                        getWorkItemInternal().getContext());
                    linkTypeToUpdater.put(type, updater);
                }
                updater.addLinkToBeUpdated((LinkImpl) displayLinks[i]);
            }
        }

        return new DescriptionUpdateRunnable(errorCallback, finishedCallback, linkTypeToUpdater.values());
    }

    public void addLinkCollectionChangedListener(final LinkCollectionChangedListener listener) {
        listeners.addListener(listener);
    }

    public void removeLinkCollectionChangedListener(final LinkCollectionChangedListener listener) {
        listeners.removeListener(listener);
    }

    /*
     * ************************************************************************
     * END of implementation of LinkCollection interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of internal-only LinkCollectionImpl methods
     * ***********************************************************************
     */

    public void copy(final LinkCollectionImpl targetCollection) {
        /*
         * The MS implementation seems to copy all links (including links
         * pending deletion) except for newly added links.
         */

        final WorkItemClient client = targetCollection.getWorkItem().getClient();

        for (final Link link : getComponentSet()) {
            if (link.isNewlyCreated()) {
                continue;
            }

            LinkImpl newLink;
            if (link instanceof RelatedLinkImpl) {
                final RelatedLinkImpl relatedLink = (RelatedLinkImpl) link;

                if (client.supportsWorkItemLinkTypes()) {
                    final WorkItemLinkTypeEndCollection ends = client.getLinkTypes().getLinkTypeEnds();
                    final WorkItemLinkTypeEnd end = ends.getByID(relatedLink.getWorkItemLinkTypeID());

                    if (end.getLinkType().isOneToMany() && end.isForwardLink()) {
                        continue;
                    }
                }

                newLink = relatedLink.cloneLink();
                ((RelatedLink) newLink).setSourceWorkItem(targetCollection.getWorkItem());
            } else {
                newLink = ((LinkImpl) link).cloneLink();
            }

            targetCollection.add(newLink);
        }
    }
    /*
     * ************************************************************************
     * END of internal-only LinkCollectionImpl methods
     * ***********************************************************************
     */
}
