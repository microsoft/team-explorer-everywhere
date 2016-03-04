// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.visualstudio.services.webapi.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.tfs.core.Messages;

public class VssJsonCollectionWrapper<T> {
    private int count;
    private T value;

    public VssJsonCollectionWrapper() {
    }

    public VssJsonCollectionWrapper(final T value) {
        this.value = value;
        if (value == null) {
            this.count = 0;
        } else if (value instanceof List<?>) {
            this.count = ((List<?>) value).size();
        } else if (value instanceof Collection<?>) {
            this.count = ((Collection<?>) value).size();
        } else {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    Messages.getString("VssJsonCollectionWrapper.CannotWrapFormat"), //$NON-NLS-1$
                    value.getClass().getName()));
        }
    }

    @JsonProperty("value")
    public T getValue() {
        return value;
    }

    @JsonProperty("value")
    public void setValue(final T value) {
        this.value = value;
    }

    @JsonProperty("count")
    public int getCount() {
        return count;
    }

    @JsonProperty("count")
    public void setCount(final int count) {
        this.count = count;
    }

    public static <T extends Object> VssJsonCollectionWrapper<List<T>> newInstance(final List<T> value) {
        final List<T> newValue = new ArrayList<T>();
        newValue.addAll(value);
        return new VssJsonCollectionWrapper<List<T>>(newValue);
    }
}
