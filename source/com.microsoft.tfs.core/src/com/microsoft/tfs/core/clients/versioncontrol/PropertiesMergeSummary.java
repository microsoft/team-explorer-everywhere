// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.util.Check;

public class PropertiesMergeSummary {
    private int totalConflicts = 0;
    private int totalTheirChanges = 0;
    private int totalYourChanges = 0;
    private boolean isRedundant = true;

    private PropertyValue[] mergedProperties;

    private PropertiesMergeSummary() {
    }

    public int getTotalConflicts() {
        return totalConflicts;
    }

    public int getTotalTheirChanges() {
        return totalTheirChanges;
    }

    public int getTotalYourChanges() {
        return totalYourChanges;
    }

    public boolean isRedundant() {
        return isRedundant;
    }

    public PropertyValue[] getMergedProperties() {
        return mergedProperties;
    }

    public void setMergedProperties(final PropertyValue[] mergedProperties) {
        this.mergedProperties = mergedProperties;
    }

    /**
     * Calculates the merge summary based on the provided properties.
     *
     * @param baseProperties
     * @param yourProperties
     * @param theirProperties
     * @return
     */
    public static PropertiesMergeSummary calculateSummary(
        final PropertyValue[] baseProperties,
        final PropertyValue[] yourProperties,
        final PropertyValue[] theirProperties) {
        Check.notNull(yourProperties, "yourProperties"); //$NON-NLS-1$
        Check.notNull(theirProperties, "theirProperties"); //$NON-NLS-1$

        final Map<String, MergedProperty> mergedProperties =
            new TreeMap<String, MergedProperty>(String.CASE_INSENSITIVE_ORDER);

        if (baseProperties != null) {
            for (final PropertyValue property : baseProperties) {
                final MergedProperty mergedProperty = new MergedProperty();
                mergedProperty.setBaseProperty(property);

                mergedProperties.put(property.getPropertyName(), mergedProperty);
            }
        }

        /* Add in your properties. */
        for (final PropertyValue property : yourProperties) {
            MergedProperty mergedProperty = mergedProperties.get(property.getPropertyName());

            if (mergedProperty == null) {
                mergedProperty = new MergedProperty();
                mergedProperties.put(property.getPropertyName(), mergedProperty);
            }

            mergedProperty.setYourProperty(property);
        }

        /* Add in their properties. */
        for (final PropertyValue property : theirProperties) {
            MergedProperty mergedProperty = mergedProperties.get(property.getPropertyName());

            if (mergedProperty == null) {
                mergedProperty = new MergedProperty();
                mergedProperties.put(property.getPropertyName(), mergedProperty);
            }

            mergedProperty.setTheirProperty(property);
        }

        /*
         * Now iterate through our merged properties and calculate the merge
         * summary.
         */
        final PropertiesMergeSummary mergeSummary = new PropertiesMergeSummary();
        List<PropertyValue> mergeResult = new ArrayList<PropertyValue>();

        for (final MergedProperty mergedProperty : mergedProperties.values()) {
            mergeSummary.totalYourChanges += mergedProperty.isYoursChanged() ? 1 : 0;
            mergeSummary.totalTheirChanges += mergedProperty.isTheirsChanged() ? 1 : 0;

            if (mergedProperty.isRedundant()) {
                mergeSummary.isRedundant = true;
            }

            if (mergedProperty.isConflicting()) {
                mergeSummary.totalConflicts++;

                // null out our mergeResult
                mergeResult = null;
            } else if (mergeResult != null && mergedProperty.getMergeResult() != null) {
                mergeResult.add(mergedProperty.getMergeResult());
            }
        }

        mergeSummary.setMergedProperties(
            (mergeResult == null) ? null : mergeResult.toArray(new PropertyValue[mergeResult.size()]));

        return mergeSummary;
    }

    /**
     * Helper class to represent a merged property.
     *
     * @threadsafety unknown
     */
    private static class MergedProperty {
        private PropertyValue baseProperty;
        private PropertyValue yourProperty;
        private PropertyValue theirProperty;

        private Boolean isYoursChanged;
        private Boolean isTheirsChanged;
        private Boolean isConflicting;
        private Boolean isRedundant;

        public void setBaseProperty(final PropertyValue baseProperty) {
            this.baseProperty = baseProperty;
        }

        public void setYourProperty(final PropertyValue yourProperty) {
            this.yourProperty = yourProperty;
        }

        public void setTheirProperty(final PropertyValue theirProperty) {
            this.theirProperty = theirProperty;
        }

        /**
         * Returns true if your property has changed when compared to the base.
         */
        public boolean isYoursChanged() {
            if (isYoursChanged == null) {
                isYoursChanged = !propertiesAreEqual(baseProperty, yourProperty);
            }

            return isYoursChanged;
        }

        /**
         * Returns true if their property has changed when compared to the base.
         */
        public boolean isTheirsChanged() {
            if (isTheirsChanged == null) {
                isTheirsChanged = !propertiesAreEqual(baseProperty, theirProperty);
            }

            return isTheirsChanged;
        }

        /**
         * Returns true if the three-way merge has resulted in a conflict.
         */
        public boolean isConflicting() {
            if (isConflicting == null) {
                if (!isYoursChanged() || !isTheirsChanged()) {
                    isConflicting = false;
                } else {
                    /* They both have changed, make sure they are equal. */
                    isConflicting = !propertiesAreEqual(yourProperty, theirProperty);
                }
            }

            return isConflicting;
        }

        /**
         * Returns true if the properties are redundant.
         */
        public boolean isRedundant() {
            if (isRedundant == null) {
                isRedundant = propertiesAreEqual(yourProperty, theirProperty);
            }

            return isRedundant;
        }

        /** The merged result if there is no conflict, null otherwise. */
        public PropertyValue getMergeResult() {
            if (isConflicting()) {
                return null;
            }

            return (isYoursChanged()) ? yourProperty : theirProperty;
        }

        /**
         * @return true if both properties are both null or if they are
         *         otherwise equal.
         */
        private static boolean propertiesAreEqual(final PropertyValue property1, final PropertyValue property2) {
            if (property1 == null && property2 == null) {
                return true;
            }

            if (property1 == null || property2 == null) {
                return false;
            }

            if (property1.getPropertyType() != property2.getPropertyType()) {
                return false;
            }

            if (byte[].class.equals(property1.getPropertyType())) {
                return Arrays.equals((byte[]) property1.getPropertyValue(), (byte[]) property2.getPropertyValue());
            } else if (property1.getPropertyValue() == null) {
                return (property2.getPropertyValue() == null);
            } else {
                return property1.getPropertyValue().equals(property2.getPropertyValue());
            }
        }
    }
}
