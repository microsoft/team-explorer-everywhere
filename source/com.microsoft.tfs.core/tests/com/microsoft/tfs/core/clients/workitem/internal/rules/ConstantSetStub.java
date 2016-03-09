// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

import java.util.HashSet;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.internal.metadata.IConstantSet;

public class ConstantSetStub implements IConstantSet {
    private final Set<String> patternMatches = new HashSet<String>();
    private final Set<Integer> constantIds = new HashSet<Integer>();
    private final Set<String> containsValues = new HashSet<String>();

    public ConstantSetStub addContainedValue(final String value) {
        containsValues.add(value);
        return this;
    }

    public ConstantSetStub addPatternMatch(final String value) {
        patternMatches.add(value);
        return this;
    }

    public ConstantSetStub addConstantID(final Integer constantId) {
        constantIds.add(constantId);
        return this;
    }

    @Override
    public boolean contains(final String valueToTest) {
        return containsValues.contains(valueToTest);
    }

    @Override
    public boolean containsConstID(final int constId) {
        return constantIds.contains(new Integer(constId));
    }

    @Override
    public String[] toArray() {
        return containsValues.toArray(new String[containsValues.size()]);
    }

    @Override
    public int getQueryCount() {
        return -1;
    }

    @Override
    public int getSize() {
        return -1;
    }

    @Override
    public Set<String> getValues() {
        return null;
    }

    @Override
    public boolean patternMatch(final Object input, final String debuggingInfo) {
        return patternMatches.contains(input);
    }
}
