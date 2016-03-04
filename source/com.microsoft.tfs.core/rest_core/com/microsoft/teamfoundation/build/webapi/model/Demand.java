// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.build.webapi.model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.microsoft.vss.client.core.utils.ArgumentUtility;
import com.microsoft.vss.client.core.utils.StringUtil;

@JsonDeserialize(using = DemandDeserializer.class)
@JsonSerialize(using = DemandSerializer.class)
public abstract class Demand {

    final private String name;
    final private String value;

    protected Demand(final String name, final String value) {
        ArgumentUtility.checkStringForNullOrEmpty(name, "name"); //$NON-NLS-1$
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public abstract Demand clone();

    protected abstract String getExpression();

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Demand) {
            final Demand demand = (Demand) obj;
            return demand != null && demand.toString().equalsIgnoreCase(this.toString());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return this.toString().toUpperCase(Locale.ROOT).hashCode();
    }

    @Override
    public final String toString() {
        return getExpression();
    }

    public static Demand tryParse(final String input) {
        Demand demand = null;

        final Matcher match = DEMAND_PATTERN.matcher(input);
        if (!match.matches()) {
            return null;
        }

        final String name = match.group(1);
        final String opcode = match.group(3);
        final String value = match.group(4);

        if (StringUtil.isNullOrEmpty(opcode)) {
            demand = new DemandExists(name);
        } else if (opcode.equals("equals")) { //$NON-NLS-1$
            demand = new DemandEquals(name, value);
        }

        return demand;
    }

    private static final Pattern DEMAND_PATTERN = Pattern.compile("^([^ ]+)([ ]+\\-([^ ]+)[ ]+(.*))?$"); //$NON-NLS-1$

}
