// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;

public class BuildDefinitionComparer implements Comparator<IBuildDefinition> {
    private final Locale locale;

    public BuildDefinitionComparer(final IBuildServer buildServer) {
        locale = buildServer.getConnection().getLocale();
    }

    @Override
    public int compare(final IBuildDefinition x, final IBuildDefinition y) {
        return Collator.getInstance(locale).compare(x.getName(), y.getName());
    }
}
