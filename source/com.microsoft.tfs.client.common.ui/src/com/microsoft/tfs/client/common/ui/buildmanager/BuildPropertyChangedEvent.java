// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.buildmanager;

import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.tfs.util.Check;

public class BuildPropertyChangedEvent extends EventObject {
    private static final long serialVersionUID = 1133356968500877298L;

    private final String buildUri;
    private final String buildProperty;

    public static final String BUILD_PROP_RETAIN_BUILD = "retain-build"; //$NON-NLS-1$
    public static final String BUILD_PROP_BUILD_QUALITY = "build-quality"; //$NON-NLS-1$

    private static final Set<String> mapPropertyNames;

    static {
        mapPropertyNames = new HashSet<String>();
        mapPropertyNames.add(BUILD_PROP_RETAIN_BUILD);
        mapPropertyNames.add(BUILD_PROP_BUILD_QUALITY);
    }

    public BuildPropertyChangedEvent(final Object source, final String buildUri, final String buildProperty) {
        super(source);

        Check.notNull(buildUri, "buildUri"); //$NON-NLS-1$
        Check.notNull(buildProperty, "buildProperty"); //$NON-NLS-1$
        Check.isTrue(mapPropertyNames.contains(buildProperty), "mapPropertyNames"); //$NON-NLS-1$

        this.buildUri = buildUri;
        this.buildProperty = buildProperty;
    }

    public String getBuildUri() {
        return buildUri;
    }

    public String getBuildProperty() {
        return buildProperty;
    }
}
