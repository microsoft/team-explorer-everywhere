// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LocalItemExclusionSet;
import com.microsoft.tfs.core.ws.runtime.xml.XMLConvert;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * Contains data describing exclusions which apply when scanning the file system
 * for candidates to pend for add.
 *
 * @threadsafety unknown
 * @since TEE-SDK 11.0
 */
public class ExclusionSet {
    private Set<String> defaultExclusions = new HashSet<String>();

    // State about the changes that haven't been committed to the file cache.
    private boolean defaultExclusionsOverwritten;
    private final Set<String> removedExclusions = new HashSet<String>();

    private GUID defaultExclusionWatermark;
    private Calendar lastDefaultExclusionUpdate;

    private static final String DEFAULT_EXCLUSIONS = "DefaultExclusions"; //$NON-NLS-1$
    private static final String EXCLUSION = "Exclusion"; //$NON-NLS-1$
    private static final String WATERMARK_ATTRIBUTE = "watermark"; //$NON-NLS-1$
    private static final String LAST_DEFAULT_EXCLUSION_UPDATE_ATTRIBUTE = "lastDefaultExclusionUpdate"; //$NON-NLS-1$

    public ExclusionSet() {
        setLastDefaultExclusionUpdate(null);
        setDefaultExclusionWatermark(GUID.EMPTY);

        // Load the default exclusions from the resource file since we weren't
        // created with any.
        final String exclusionsList =
            "bin;bld;ClientBin;Debug;obj;Package;Release;TestResults;*.*~;*.appx;*.appxrecipe;*.cache;*.cer;*.class;*.dbmdl;*.dll;*.docstates;*.docstates.suo;*.err;*.exe;*.ilk;*.ipch;*.lastbuildstate;*.lce;*.ldf;*.lib;*.log;*.mdf;*.msscci;*.ncb;*.obj;*.opensdf;*.pch;*.pdb;*.pri;*.res;*.resources;*.sdf;*.suo;*.swp;*.temp;*.tfOrig*;*.tlog;*.tmp;*.trx;*.user;*.unsuccessfulbuild;*.v11.suo;*.vcxproj.user;*.vsix;*.vsmdi;*.vspscc;*.vssettings;*.vssscc;*.wrn;*.xap"; //$NON-NLS-1$
        final String[] exclusions = exclusionsList.split(";"); //$NON-NLS-1$

        for (final String exclusion : exclusions) {
            defaultExclusions.add(exclusion);
        }
    }

    /**
     * Construct an exclusion set from data contained under the specified XML
     * DOM element (a team project collection element from the local items
     * exclusions XML configuration file).
     *
     *
     * @param collectionElement
     *        The team project collection element from the DOM containing this
     *        exclusion set.
     */
    public ExclusionSet(final Element collectionElement) {
        setLastDefaultExclusionUpdate(null);
        setDefaultExclusionWatermark(GUID.EMPTY);

        // Get the first DefaultExclusions element (there should only be one).
        final Element defaultExclusionElement = DOMUtils.getFirstChildElement(collectionElement, DEFAULT_EXCLUSIONS);
        if (defaultExclusionElement != null) {
            // Load the default exclusions attributes.
            final String watermarkValue = defaultExclusionElement.getAttribute(WATERMARK_ATTRIBUTE);
            if (watermarkValue != null && watermarkValue.length() > 0) {
                setDefaultExclusionWatermark(new GUID(watermarkValue));
            }

            final String updateValue = defaultExclusionElement.getAttribute(LAST_DEFAULT_EXCLUSION_UPDATE_ATTRIBUTE);
            if (updateValue != null && updateValue.length() > 0) {
                setLastDefaultExclusionUpdate(XMLConvert.toCalendar(updateValue, true));
            }

            // Load the default exclusions.
            final Element[] exclusionElements = DOMUtils.getChildElements(defaultExclusionElement, EXCLUSION);
            for (final Element exclusionElement : exclusionElements) {
                defaultExclusions.add(DOMUtils.getText(exclusionElement));
            }
        }
    }

    public GUID getDefaultExclusionWatermark() {
        return defaultExclusionWatermark;
    }

    private void setDefaultExclusionWatermark(final GUID value) {
        defaultExclusionWatermark = value;
    }

    public Calendar getLastDefaultExclusionUpdate() {
        return lastDefaultExclusionUpdate;
    }

    private void setLastDefaultExclusionUpdate(final Calendar value) {
        lastDefaultExclusionUpdate = value;
    }

    public boolean removeExclusion(final String exclusion) {
        final boolean somethingRemoved = defaultExclusions.remove(exclusion);

        if (somethingRemoved) {
            removedExclusions.add(exclusion);
        }

        return somethingRemoved;
    }

    public String[] getExclusions() {
        // Use a HashSet to build the joint exclusions list so that we don't get
        // duplicates. We do this as opposed to maintaining non-duplicates at
        // add/set time for the reason mentioned in the SetDefaultExlucions
        // method as well as because these can be modified directly by accessing
        // the file.
        final Set<String> allExclusions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        for (final String exclusion : defaultExclusions) {
            allExclusions.add(exclusion);
        }

        return allExclusions.toArray(new String[allExclusions.size()]);
    }

    public void setDefaultExclusions(final LocalItemExclusionSet exclusionsSet) {
        if (!exclusionsSet.getWatermark().equals(getDefaultExclusionWatermark())) {
            defaultExclusions.clear();

            // TODO: Remove when ".class" appears in the server exclusion list.
            removedExclusions.remove("*.class"); //$NON-NLS-1$
            defaultExclusions.add("*.class"); //$NON-NLS-1$

            for (final String exclusion : exclusionsSet.getExclusions()) {
                removedExclusions.remove(exclusion);
                defaultExclusions.add(exclusion);

                // We could remove this exclusion from the user exclusions at
                // this point to avoid duplication.
                // However, I don't want the list coming from the server to muck
                // with user exclusions
                // because those should only be affected by the user. This helps
                // prevent the following case:
                //
                // 1. User adds the "*.bar" exclusion.
                // 2. *.bar is added to the default exclusions and is sunk
                // (removing it from the user exclusions)
                // 3. *.bar is removed from the default exclusions because not
                // everyone wanted it and is sunk.
                // 4. Even though this user told us to ignore *.bar that
                // exclusion is now missing.
            }

            defaultExclusionWatermark = exclusionsSet.getWatermark();
            defaultExclusionsOverwritten = true;
        }

        // Always increment the time stamp to record the last time we checked
        // for updates.
        setLastDefaultExclusionUpdate(Calendar.getInstance());
    }

    /**
     * Save the exclusion set data in the DOM under the specified collection
     * node (a team project collection element from the local items exclusions
     * XML configuration file).
     *
     * @param collectionElement
     *        The team project collection to which this exclusion set applies.
     */
    public void save(final Element collectionElement) {
        final Element defaultsElement = DOMUtils.appendChild(collectionElement, DEFAULT_EXCLUSIONS);

        final String watermark = getDefaultExclusionWatermark().getGUIDString();
        final String date = XMLConvert.toString(getLastDefaultExclusionUpdate(), true, true);

        defaultsElement.setAttribute(WATERMARK_ATTRIBUTE, watermark);
        defaultsElement.setAttribute(LAST_DEFAULT_EXCLUSION_UPDATE_ATTRIBUTE, date);

        // TODO: Remove this when *.class is added to the global exclusion list.
        defaultExclusions.add("*.class"); //$NON-NLS-1$

        for (final String exclusion : defaultExclusions) {
            DOMUtils.appendChildWithText(defaultsElement, EXCLUSION, exclusion);
        }
    }

    public void merge(final ExclusionSet exclusionSetToMergeIn) {
        // If we have overwritten the default exclusions then take our list,
        // otherwise take the list from disk.
        final Set<String> defaultExclusions =
            (defaultExclusionsOverwritten) ? this.defaultExclusions : exclusionSetToMergeIn.defaultExclusions;

        // Now apply all of our removals
        for (final String removal : removedExclusions) {
            defaultExclusions.remove(removal);
        }

        // Update our member variables and then we are done.
        this.defaultExclusions = defaultExclusions;
    }

    /**
     * Should be called when the cache is written out to disk.
     */
    public void markClean() {
        // Clear the removed items.
        removedExclusions.clear();
        defaultExclusionsOverwritten = false;
    }
}
