// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.IBuildDefinitionSourceProvider;
import com.microsoft.tfs.core.clients.build.soapextensions.DefinitionTriggerType;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.build.buildservice._04._BuildDefinitionSourceProvider;
import ms.tfs.build.buildservice._04._NameValueField;

public class BuildDefinitionSourceProvider extends WebServiceObjectWrapper implements IBuildDefinitionSourceProvider {

    private final Map<String, String> fields = new HashMap<String, String>();

    public BuildDefinitionSourceProvider() {
        this(new _BuildDefinitionSourceProvider());
    }

    public BuildDefinitionSourceProvider(final String name, final DefinitionTriggerType triggerType) {
        this(new _BuildDefinitionSourceProvider(name, triggerType.getWebServiceObject(), null));
    }

    public BuildDefinitionSourceProvider(final _BuildDefinitionSourceProvider webServiceObject) {
        super(webServiceObject);
        loadFields();
    }

    public _BuildDefinitionSourceProvider getWebServiceObject() {
        return (_BuildDefinitionSourceProvider) this.webServiceObject;
    }

    @Override
    public String getName() {
        return getWebServiceObject().getName();
    }

    @Override
    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    @Override
    public DefinitionTriggerType getSupportedTriggerTypes() {
        return new DefinitionTriggerType(getWebServiceObject().getSupportedTriggerTypes());
    }

    @Override
    public void setSupportedTriggerTypes(final DefinitionTriggerType value) {
        getWebServiceObject().setSupportedTriggerTypes(value.getWebServiceObject());
    }

    @Override
    public String getValueByName(final String name) {
        return fields.get(name);
    }

    @Override
    public void setNameValueField(final String name, final String value) {
        fields.put(name, value);
    }

    @Override
    public Map<String, String> getAllFields() {
        return fields;
    }

    private void loadFields() {
        final _NameValueField[] valueFields = getWebServiceObject().getFields();
        if (valueFields != null) {
            for (final _NameValueField f : valueFields) {
                fields.put(f.getName(), f.getValue());
            }
        }
    }

    @Override
    public void prepareToSave() {
        final List<_NameValueField> nameValueFields = new ArrayList<_NameValueField>();
        for (final String key : fields.keySet()) {
            nameValueFields.add(new _NameValueField(key, fields.get(key)));
        }
        getWebServiceObject().setFields(nameValueFields.toArray(new _NameValueField[nameValueFields.size()]));
    }
}
