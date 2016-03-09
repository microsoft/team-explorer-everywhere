// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.util.Check;

/**
 * A class containing metadata for all Team Explorer extensions. All out of box
 * components are implemented as extensions so metadata for all Team Explorer
 * components are managed by this class.
 *
 *
 * @threadsafety unknown
 */
public class TeamExplorerConfig {
    // Home page item ID.
    final static String HOME_ITEM_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.items.TeamExplorerHomeNavigationItem"; //$NON-NLS-1$

    // Team Explorer extension point IDs.
    private final static String PAGE_EXTENSION_POINT_ID = "com.microsoft.tfs.client.common.ui.teamExplorerPage"; //$NON-NLS-1$
    private final static String SECTION_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.ui.teamExplorerPageSection"; //$NON-NLS-1$
    private final static String NAVITEM_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.ui.teamExplorerNavigationItem"; //$NON-NLS-1$
    private final static String NAVLINK_EXTENSION_POINT_ID =
        "com.microsoft.tfs.client.common.ui.teamExplorerNavigationLink"; //$NON-NLS-1$

    // The list of Team Explorer Navigation Items sorted in display priority
    // order.
    private final TeamExplorerNavigationItemConfig[] navItems;

    // A map containing all Team Explorer Navigation Links (the second level
    // items on the Team Explroer HOME page). Keys in the map are IDs of the
    // target Navigation Item for a link. Values in the map are arrays of
    // Navigation Links sorted in display priority order.
    private final Map<String, TeamExplorerNavigationLinkConfig[]> navLinkMap;

    // A map containing all Team Explorer Page items. A page is the target of a
    // Navigation Item. Keys in the map are page IDs. Values in the map are
    // objects containing the full metadata definition for a page.
    private final Map<String, TeamExplorerPageConfig> pageMap;

    // A map containing all Team Explorer Sections (the collapsible compent
    // within a page). Keys in the map are IDs of the target Page. Values in the
    // map are arrays of Sections which target that Page sorted in display
    // priority order.
    private final Map<String, TeamExplorerSectionConfig[]> sectionsMap;

    // Metadata for the HOME page.
    private TeamExplorerNavigationItemConfig homeItem;

    public TeamExplorerConfig() {
        navItems = discoverNavigationItems();
        navLinkMap = discoverNavigationLinks();
        pageMap = discoverPages();
        sectionsMap = discoverSections();
    }

    public TeamExplorerNavigationItemConfig getHomeNavigationItem() {
        return homeItem;
    }

    public TeamExplorerNavigationItemConfig[] getNavigationItems() {
        return navItems;
    }

    public TeamExplorerNavigationLinkConfig[] getNavigationLinks(final String itemID) {
        return navLinkMap.get(itemID);
    }

    public TeamExplorerPageConfig getPage(final String targetPageID) {
        return pageMap.get(targetPageID);
    }

    public TeamExplorerSectionConfig[] getPageSections(final String pageID) {
        return sectionsMap.get(pageID);
    }

    /**
     * Query Eclipse for TeamExplorerPage extensions and return a map of page
     * IDs to page metadata.
     */
    private Map<String, TeamExplorerPageConfig> discoverPages() {
        final Map<String, TeamExplorerPageConfig> map = new HashMap<String, TeamExplorerPageConfig>();

        for (final IConfigurationElement element : getExtensions(PAGE_EXTENSION_POINT_ID)) {
            final TeamExplorerPageConfig page = TeamExplorerPageConfig.fromConfigurationElement(element);
            map.put(page.getID(), page);
        }

        return map;
    }

    /**
     * Query Eclipse for TeamExplorerNavigationItem extensions and return an
     * array of extesions sorted by display priority.
     */
    private TeamExplorerNavigationItemConfig[] discoverNavigationItems() {
        final List<TeamExplorerNavigationItemConfig> list = new ArrayList<TeamExplorerNavigationItemConfig>();

        for (final IConfigurationElement element : getExtensions(NAVITEM_EXTENSION_POINT_ID)) {
            final TeamExplorerNavigationItemConfig navItem =
                TeamExplorerNavigationItemConfig.fromConfigurationElement(element);

            if (navItem.getID().equals(HOME_ITEM_ID)) {
                homeItem = navItem;
            } else {
                list.add(navItem);
            }
        }

        Check.notNull(homeItem, "homeItem"); //$NON-NLS-1$

        final TeamExplorerNavigationItemConfig[] navItems =
            list.toArray(new TeamExplorerNavigationItemConfig[list.size()]);
        Arrays.sort(navItems, new TeamExplorerOrderedComponentComparator());

        return navItems;
    }

    /**
     * Query eclipse for TeamExplorerNavigationLink extensions and build the
     * navigation link map.
     *
     * The navigation link map contains a navigation item ID as a key and an
     * array of navigation links (sorted by priority) that target the item as a
     * value.
     */
    private Map<String, TeamExplorerNavigationLinkConfig[]> discoverNavigationLinks() {
        // Allocate a temporary map to build a list of links for each navigation
        // item. The individual lists will be converted to arrays and sorted by
        // display order later in this method.
        final Map<String, List<TeamExplorerNavigationLinkConfig>> map;
        map = new HashMap<String, List<TeamExplorerNavigationLinkConfig>>();

        // Iterate navigation link extensions and add each link to the list of
        // links for the respective item.
        for (final IConfigurationElement element : getExtensions(NAVLINK_EXTENSION_POINT_ID)) {
            // Get the navigation link configuration data.
            final TeamExplorerNavigationLinkConfig navLink =
                TeamExplorerNavigationLinkConfig.fromConfigurationElement(element);

            // Find or create the list of links for the target item.
            List<TeamExplorerNavigationLinkConfig> navLinks = map.get(navLink.getParentID());
            if (navLinks == null) {
                navLinks = new ArrayList<TeamExplorerNavigationLinkConfig>();
                map.put(navLink.getParentID(), navLinks);
            }

            // Add the link to the list for the target item.
            navLinks.add(navLink);
        }

        // Allocate the map to return.
        final Map<String, TeamExplorerNavigationLinkConfig[]> toReturn;
        toReturn = new HashMap<String, TeamExplorerNavigationLinkConfig[]>();

        // Iterate the set of target items and convert the associated links
        // to an array sorted by priority.
        for (final String itemID : map.keySet()) {
            final List<TeamExplorerNavigationLinkConfig> list = map.get(itemID);
            final TeamExplorerNavigationLinkConfig[] links =
                list.toArray(new TeamExplorerNavigationLinkConfig[list.size()]);

            // Sort the section by priority and place the sorted array into map.
            Arrays.sort(links, new TeamExplorerOrderedComponentComparator());
            toReturn.put(itemID, links);
        }

        return toReturn;
    }

    /**
     * Query eclipse for TeamExplorerPageSection extensions and build the page
     * section map.
     *
     * The page section map contains a pageID as a key and an array of sections
     * (sorted by priority) that target the page as a value.
     */
    private Map<String, TeamExplorerSectionConfig[]> discoverSections() {
        // Allocate a temporary map to build a list of sections for each page.
        // The individual lists will be converted to arrays and sorted by
        // display order later in this method.
        final Map<String, List<TeamExplorerSectionConfig>> map;
        map = new HashMap<String, List<TeamExplorerSectionConfig>>();

        // Iterate section extensions and add each section to the list of
        // sections for the respective page.
        for (final IConfigurationElement element : getExtensions(SECTION_EXTENSION_POINT_ID)) {
            // Get the section configuration data.
            final TeamExplorerSectionConfig section = TeamExplorerSectionConfig.fromConfigurationElement(element);

            // Find or create the list of sections for the target page.
            List<TeamExplorerSectionConfig> sections = map.get(section.getPageID());
            if (sections == null) {
                sections = new ArrayList<TeamExplorerSectionConfig>();
                map.put(section.getPageID(), sections);
            }

            // Add the section to the list for the target page.
            sections.add(section);
        }

        // Allocate the map to return.
        final Map<String, TeamExplorerSectionConfig[]> toReturn;
        toReturn = new HashMap<String, TeamExplorerSectionConfig[]>();

        // Iterate the set of target pages and convert the associated sections
        // to an array sorted by priority.
        for (final String pageID : map.keySet()) {
            final List<TeamExplorerSectionConfig> list = map.get(pageID);
            final TeamExplorerSectionConfig[] sections = list.toArray(new TeamExplorerSectionConfig[list.size()]);

            // Sort the section by priority and place the sorted array into map.
            Arrays.sort(sections, new TeamExplorerOrderedComponentComparator());
            toReturn.put(pageID, sections);
        }

        return toReturn;
    }

    /**
     * Helper method to retrieve Eclipse extensions for the specified extension
     * point.
     *
     *
     * @param extensionPointID
     *        The extension point ID.
     * @return Array of Eclipse extension elements.
     */
    private IConfigurationElement[] getExtensions(final String extensionPointID) {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint extensionPoint = registry.getExtensionPoint(extensionPointID);
        return extensionPoint.getConfigurationElements();
    }

    /**
     * A comparator for any extension item class that must be sorted by display
     * priority.
     *
     * @threadsafety unknown
     */
    private class TeamExplorerOrderedComponentComparator implements Comparator<TeamExplorerOrderedComponent> {
        @Override
        public int compare(final TeamExplorerOrderedComponent x, final TeamExplorerOrderedComponent y) {
            if (x.getDisplayPriority() < y.getDisplayPriority()) {
                return -1;
            } else if (x.getDisplayPriority() > y.getDisplayPriority()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
