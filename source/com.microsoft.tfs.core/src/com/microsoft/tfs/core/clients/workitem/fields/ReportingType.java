// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

/**
 * Describes the reporting type of the field.
 *
 * @since TEE-SDK-10.1
 */
public class ReportingType {
    public static final ReportingType NONE = new ReportingType("None", 0); //$NON-NLS-1$
    public static final ReportingType MEASURE = new ReportingType("Measure", 1); //$NON-NLS-1$
    public static final ReportingType DIMENSION = new ReportingType("Dimension", 2); //$NON-NLS-1$
    public static final ReportingType DRILL_DOWN_FIELD = new ReportingType("DrillDownField", 3); //$NON-NLS-1$

    private final String name;
    private final int value;

    private ReportingType(final String name, final int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public static ReportingType fromValue(final int value) {
        switch (value) {
            case 1:
                return ReportingType.MEASURE;
            case 2:
                return ReportingType.DIMENSION;
            case 3:
                return ReportingType.DRILL_DOWN_FIELD;
            default:
                break;
        }
        return ReportingType.NONE;
    }
}
