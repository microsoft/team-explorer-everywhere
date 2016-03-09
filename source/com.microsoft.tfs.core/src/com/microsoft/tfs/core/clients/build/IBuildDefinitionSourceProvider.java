// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Map;

import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;

public interface IBuildDefinitionSourceProvider {
    public String getName();

    public void setName(final String name);

    public DefinitionTriggerType getSupportedTriggerTypes();

    public void setSupportedTriggerTypes(final DefinitionTriggerType value);

    public String getValueByName(final String name);

    public void setNameValueField(final String name, final String value);

    public Map<String, String> getAllFields();

    public void prepareToSave();
}
