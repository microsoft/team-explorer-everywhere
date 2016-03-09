// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.settings;

import org.eclipse.core.resources.IMarker;

import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

public class MarkerMatch {
    private String markerType;
    private boolean includeSubtypes = true;

    private String comment;

    private boolean severityError = true;
    private boolean severityWarning;
    private boolean severityInfo;

    private boolean priorityHigh;
    private boolean priorityNormal;
    private boolean priorityLow;

    private static final String TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
    private static final String INCLUDE_SUBTYPES_ATTRIBUTE = "includeSubtypes"; //$NON-NLS-1$

    private static final String COMMENT_ATTRIBUTE = "comment"; //$NON-NLS-1$

    private static final String SEVERITY_ERROR_ATTRIBUTE = "severityError"; //$NON-NLS-1$
    private static final String SEVERITY_WARNING_ATTRIBUTE = "severityWarning"; //$NON-NLS-1$
    private static final String SEVERITY_INFO_ATTRIBUTE = "severityInfo"; //$NON-NLS-1$

    private static final String PRIORITY_HIGH_ATTRIBUTE = "priorityHigh"; //$NON-NLS-1$
    private static final String PRIORITY_NORMAL_ATTRIBUTE = "priorityNormal"; //$NON-NLS-1$
    private static final String PRIORITY_LOW_ATTRIBUTE = "priorityLow"; //$NON-NLS-1$

    /*
     * For matching.
     */
    private static final int SEVERITY_INVALID = -999;
    private static final int PRIORITY_INVALID = -999;

    public MarkerMatch() {
        markerType = ""; //$NON-NLS-1$
    }

    public MarkerMatch(
        final String markerType,
        final boolean includeSubtypes,
        final String comment,
        final boolean severityError,
        final boolean severityWarning,
        final boolean severityInfo,
        final boolean priorityHigh,
        final boolean priorityNormal,
        final boolean priorityLow) {
        super();

        Check.notNull(markerType, "markerType"); //$NON-NLS-1$
        Check.notNull(comment, "comment"); //$NON-NLS-1$

        this.markerType = markerType;
        this.includeSubtypes = includeSubtypes;

        this.comment = comment;

        this.severityError = severityError;
        this.severityWarning = severityWarning;
        this.severityInfo = severityInfo;

        this.priorityHigh = priorityHigh;
        this.priorityNormal = priorityNormal;
        this.priorityLow = priorityLow;
    }

    public MarkerMatch(final MarkerMatch marker) {
        super();

        Check.notNull(marker, "marker"); //$NON-NLS-1$

        markerType = marker.markerType;
        includeSubtypes = marker.includeSubtypes;

        comment = marker.comment;

        severityError = marker.severityError;
        severityWarning = marker.severityWarning;
        severityInfo = marker.severityInfo;

        priorityHigh = marker.priorityHigh;
        priorityNormal = marker.priorityNormal;
        priorityLow = marker.priorityLow;
    }

    public String getMarkerType() {
        return markerType;
    }

    public boolean isIncludeSubtypes() {
        return includeSubtypes;
    }

    public String getComment() {
        return comment;
    }

    public boolean isSeverityError() {
        return severityError;
    }

    public boolean isSeverityWarning() {
        return severityWarning;
    }

    public boolean isSeverityInfo() {
        return severityInfo;
    }

    public boolean isPriorityHigh() {
        return priorityHigh;
    }

    public boolean isPriorityNormal() {
        return priorityNormal;
    }

    public boolean isPriorityLow() {
        return priorityLow;
    }

    public void setMarkerType(final String markerType) {
        Check.notNull(markerType, "markerType"); //$NON-NLS-1$
        this.markerType = markerType;
    }

    public void setIncludeSubtypes(final boolean includeSubtypes) {
        this.includeSubtypes = includeSubtypes;
    }

    public void setComment(final String comment) {
        Check.notNull(comment, "comment"); //$NON-NLS-1$
        this.comment = comment;
    }

    public void setSeverityError(final boolean severityError) {
        this.severityError = severityError;
    }

    public void setSeverityWarning(final boolean severityWarning) {
        this.severityWarning = severityWarning;
    }

    public void setSeverityInfo(final boolean severityInfo) {
        this.severityInfo = severityInfo;
    }

    public void setPriorityHigh(final boolean priorityHigh) {
        this.priorityHigh = priorityHigh;
    }

    public void setPriorityNormal(final boolean priorityNormal) {
        this.priorityNormal = priorityNormal;
    }

    public void setPriorityLow(final boolean priorityLow) {
        this.priorityLow = priorityLow;
    }

    public void save(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        memento.putString(TYPE_ATTRIBUTE, markerType);
        memento.putBoolean(INCLUDE_SUBTYPES_ATTRIBUTE, includeSubtypes);

        memento.putString(COMMENT_ATTRIBUTE, comment);

        memento.putBoolean(SEVERITY_ERROR_ATTRIBUTE, severityError);
        memento.putBoolean(SEVERITY_WARNING_ATTRIBUTE, severityWarning);
        memento.putBoolean(SEVERITY_INFO_ATTRIBUTE, severityInfo);

        memento.putBoolean(PRIORITY_HIGH_ATTRIBUTE, priorityHigh);
        memento.putBoolean(PRIORITY_NORMAL_ATTRIBUTE, priorityNormal);
        memento.putBoolean(PRIORITY_LOW_ATTRIBUTE, priorityLow);
    }

    public void load(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        final String readType = memento.getString(TYPE_ATTRIBUTE);
        markerType = (readType != null) ? readType : ""; //$NON-NLS-1$
        includeSubtypes = memento.getBoolean(INCLUDE_SUBTYPES_ATTRIBUTE).booleanValue();

        final String readComment = memento.getString(COMMENT_ATTRIBUTE);
        comment = (readComment != null) ? readComment : ""; //$NON-NLS-1$

        severityError = memento.getBoolean(SEVERITY_ERROR_ATTRIBUTE).booleanValue();
        severityWarning = memento.getBoolean(SEVERITY_WARNING_ATTRIBUTE).booleanValue();
        severityInfo = memento.getBoolean(SEVERITY_INFO_ATTRIBUTE).booleanValue();

        priorityHigh = memento.getBoolean(PRIORITY_HIGH_ATTRIBUTE).booleanValue();
        priorityNormal = memento.getBoolean(PRIORITY_NORMAL_ATTRIBUTE).booleanValue();
        priorityLow = memento.getBoolean(PRIORITY_LOW_ATTRIBUTE).booleanValue();
    }

    public boolean matchesSeverityAndPriority(final IMarker marker) {
        Check.notNull(marker, "marker"); //$NON-NLS-1$

        final int severity = marker.getAttribute(IMarker.SEVERITY, SEVERITY_INVALID);

        /*
         * If it matches a configured severity (or none are enabled)...
         */
        final boolean severitySkip = (!severityError && !severityWarning && !severityInfo);
        if (severitySkip
            || (severityError && severity == IMarker.SEVERITY_ERROR)
            || (severityWarning && severity == IMarker.SEVERITY_WARNING)
            || (severityInfo && severity == IMarker.SEVERITY_INFO)) {
            final int priority = marker.getAttribute(IMarker.PRIORITY, PRIORITY_INVALID);

            /*
             * And matches a configured priority (or none are enabled)...
             */
            final boolean prioritySkip = (!priorityHigh && !priorityNormal && !priorityLow);
            if (prioritySkip
                || (priorityHigh && priority == IMarker.PRIORITY_HIGH)
                || (priorityNormal && priority == IMarker.PRIORITY_NORMAL)
                || (priorityLow && priority == IMarker.PRIORITY_LOW)) {
                return true;
            }
        }

        return false;
    }
}
