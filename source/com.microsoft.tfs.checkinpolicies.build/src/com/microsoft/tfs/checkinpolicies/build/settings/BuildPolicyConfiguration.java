// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.settings;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

public class BuildPolicyConfiguration {
    // General Tab.
    private Area area = Area.FILE;

    // Marker Tab.
    private MarkerMatch[] markers = new MarkerMatch[0];

    private static final String AREA_ATTRIBUTE = "area"; //$NON-NLS-1$
    private static final String MARKER_MATCH_MEMENTO = "markerMatch"; //$NON-NLS-1$

    public BuildPolicyConfiguration() {
        /*
         * Fill some good defaults.
         */
        markers = new MarkerMatch[] {
            new MarkerMatch(
                "org.eclipse.jdt.core.problem", //$NON-NLS-1$
                true,
                Messages.getString("BuildPolicyConfiguration.MatchProblemsJava"), //$NON-NLS-1$
                true,
                false,
                false,
                false,
                false,
                false),
            new MarkerMatch(
                "org.eclipse.cdt.core.problem", //$NON-NLS-1$
                true,
                Messages.getString("BuildPolicyConfiguration.MacthProblemsC"), //$NON-NLS-1$
                true,
                false,
                false,
                false,
                false,
                false),
            new MarkerMatch(
                "edu.umd.cs.findbugs.plugin.eclipse.findbugsMarker", //$NON-NLS-1$
                true,
                Messages.getString("BuildPolicyConfiguration.MatchProblemsFindBugs"), //$NON-NLS-1$
                true,
                true,
                false,
                true,
                true,
                false),
        };
    }

    public BuildPolicyConfiguration(final Area area, final MarkerMatch[] markers) {
        Check.notNull(area, "area"); //$NON-NLS-1$
        Check.notNull(markers, "markers"); //$NON-NLS-1$

        this.area = area;
        this.markers = markers;
    }

    public BuildPolicyConfiguration(final BuildPolicyConfiguration configuration) {
        Check.notNull(configuration, "configuration"); //$NON-NLS-1$

        area = configuration.area;

        markers = new MarkerMatch[configuration.markers.length];
        for (int i = 0; i < markers.length; i++) {
            markers[i] = new MarkerMatch(configuration.markers[i]);
        }
    }

    public Area getArea() {
        return area;
    }

    public MarkerMatch[] getMarkers() {
        return markers;
    }

    public void setArea(final Area area) {
        Check.notNull(area, "area"); //$NON-NLS-1$
        this.area = area;
    }

    public void setMarkers(final MarkerMatch[] markers) {
        Check.notNull(markers, "markers"); //$NON-NLS-1$
        this.markers = markers;
    }

    public void save(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        memento.putInteger(AREA_ATTRIBUTE, area.getValue());

        for (int i = 0; i < markers.length; i++) {
            final Memento child = memento.createChild(MARKER_MATCH_MEMENTO);
            markers[i].save(child);
        }
    }

    public void load(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        try {
            area = Area.fromValue(memento.getInteger(AREA_ATTRIBUTE).intValue());
        } catch (final Exception e) {
            // Ignore.
        }

        final List newMarkers = new ArrayList();
        final Memento[] children = memento.getChildren(MARKER_MATCH_MEMENTO);
        for (int i = 0; i < children.length; i++) {
            final MarkerMatch marker = new MarkerMatch();

            try {
                marker.load(children[i]);
                newMarkers.add(marker);
            } catch (final Exception e) {
                /*
                 * Incompatible serializable format. Ignore.
                 */
            }
        }

        markers = (MarkerMatch[]) newMarkers.toArray(new MarkerMatch[newMarkers.size()]);
    }
}
