// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.soapextensions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.microsoft.tfs.core.clients.build.IWorkspaceMapping;
import com.microsoft.tfs.core.clients.build.IWorkspaceTemplate;
import com.microsoft.tfs.core.clients.build.flags.WorkspaceMappingDepth;
import com.microsoft.tfs.core.clients.build.soapextensions.WorkspaceMappingType;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.datetime.DotNETDate;

import ms.tfs.build.buildservice._04._WorkspaceMapping;
import ms.tfs.build.buildservice._04._WorkspaceTemplate;

public class WorkspaceTemplate extends WebServiceObjectWrapper implements IWorkspaceTemplate {
    public WorkspaceTemplate() {
        this(new _WorkspaceTemplate());

        final _WorkspaceTemplate _o = getWebServiceObject();
        _o.setMappings(new _WorkspaceMapping[0]);
        _o.setLastModifiedDate(DotNETDate.MIN_CALENDAR);
    }

    public WorkspaceTemplate(final _WorkspaceTemplate webServiceObject) {
        super(webServiceObject);
    }

    public WorkspaceTemplate(final WorkspaceTemplate2010 template) {
        this();

        final _WorkspaceTemplate _o = getWebServiceObject();
        _o.setDefinitionUri(template.getDefinitionURI());
        _o.setLastModifiedBy(template.getLastModifiedBy());
        _o.setLastModifiedDate(template.getLastModifiedDate());

        final WorkspaceMapping[] mappings = TFS2010Helper.convert(template.getInternalMappings());
        _o.setMappings((_WorkspaceMapping[]) WrapperUtils.unwrap(_WorkspaceMapping.class, mappings));
    }

    public _WorkspaceTemplate getWebServiceObject() {
        return (_WorkspaceTemplate) this.webServiceObject;
    }

    /**
     * Gets the URI of the definition to which this template belongs.
     *
     *
     * @return
     */
    public String getDefinitionURI() {
        return getWebServiceObject().getDefinitionUri();
    }

    public void setDefinitionURI(final String value) {
        getWebServiceObject().setDefinitionUri(value);
    }

    /**
     * Gets the mappings for the template.
     *
     *
     * @return
     */
    public WorkspaceMapping[] getInternalMappings() {
        final _WorkspaceMapping[] mappings = getWebServiceObject().getMappings();
        if (mappings == null) {
            return new WorkspaceMapping[0];
        }

        return (WorkspaceMapping[]) WrapperUtils.wrap(WorkspaceMapping.class, mappings);
    }

    public void setInternalMappings(final WorkspaceMapping[] value) {
        getWebServiceObject().setMappings((_WorkspaceMapping[]) WrapperUtils.unwrap(_WorkspaceMapping.class, value));
    }

    /**
     * Gets the domain user name of the user that last modified the template.
     * This field is read-only. {@inheritDoc}
     */
    @Override
    public String getLastModifiedBy() {
        return getWebServiceObject().getLastModifiedBy();
    }

    /**
     * Gets the date and time the template was last modified. This field is
     * read-only. {@inheritDoc}
     */
    @Override
    public Calendar getLastModifiedDate() {
        return getWebServiceObject().getLastModifiedDate();
    }

    /**
     * The workspace mappings for the workspace. {@inheritDoc}
     */
    @Override
    public IWorkspaceMapping[] getMappings() {
        return getInternalMappings();
    }

    /**
     * Adds a workspace mapping to the workspace. {@inheritDoc}
     */
    @Override
    public IWorkspaceMapping addMapping() {
        final WorkspaceMapping mapping = new WorkspaceMapping();
        final _WorkspaceMapping[] _mappings = getWebServiceObject().getMappings();
        final _WorkspaceMapping[] _newMappings = new _WorkspaceMapping[_mappings.length + 1];

        for (int i = 0; i < _mappings.length; i++) {
            _newMappings[i] = _mappings[i];
        }
        _newMappings[_newMappings.length - 1] = mapping.getWebServiceObject();
        getWebServiceObject().setMappings(_newMappings);

        return mapping;
    }

    /**
     * Adds a workspace mapping with the provided server item, local item, and
     * mapping type with a depth of 120. If WorkspaceMappingType.Cloak is
     * specified then the localItem is forced to null. {@inheritDoc}
     */
    @Override
    public IWorkspaceMapping addMapping(
        final String serverItem,
        final String localItem,
        final WorkspaceMappingType type) {
        return addMapping(serverItem, localItem, type, WorkspaceMappingDepth.FULL);
    }

    /**
     * Adds a workspace mapping with the provided server item, local item,
     * mapping type, and depth. If WorkspaceMappingType.Cloak is specified then
     * the localItem parameter is forced to null. {@inheritDoc}
     */
    @Override
    public IWorkspaceMapping addMapping(
        final String serverItem,
        final String localItem,
        final WorkspaceMappingType type,
        final WorkspaceMappingDepth depth) {
        final IWorkspaceMapping mapping = addMapping();
        mapping.setServerItem(serverItem);
        mapping.setLocalItem(type.equals(WorkspaceMappingType.CLOAK) ? null : localItem);
        mapping.setMappingType(type);
        mapping.setDepth(depth);

        return mapping;
    }

    /**
     * Adds a cloak entry to the workspace for the specified server item.
     * {@inheritDoc}
     */
    @Override
    public IWorkspaceMapping cloak(final String serverItem) {
        return addMapping(serverItem, null, WorkspaceMappingType.CLOAK);
    }

    /**
     * Adds a workspace mapping with the provided server item and local item
     * with WorkspaceMappingType.Map. {@inheritDoc}
     */
    @Override
    public IWorkspaceMapping map(final String serverItem, final String localItem) {
        return addMapping(serverItem, localItem, WorkspaceMappingType.MAP);
    }

    /**
     * Removes the workspace mapping from the workspace template. {@inheritDoc}
     */
    @Override
    public boolean removeMapping(final IWorkspaceMapping mapping) {
        final _WorkspaceMapping _mappingToRemove = ((WorkspaceMapping) mapping).getWebServiceObject();
        final List<_WorkspaceMapping> list = new ArrayList<_WorkspaceMapping>();
        boolean removed = false;

        for (final _WorkspaceMapping _mapping : getWebServiceObject().getMappings()) {
            if (!_mapping.equals(_mappingToRemove)) {
                list.add(_mapping);
            } else {
                removed = true;
            }
        }

        if (removed) {
            getWebServiceObject().setMappings(list.toArray(new _WorkspaceMapping[list.size()]));
        }

        return removed;
    }

    /**
     * Clear the workspace mappings from the workspace template. (@inheritDoc)
     */
    @Override
    public void clearMappings() {
        getWebServiceObject().setMappings(new _WorkspaceMapping[0]);
    }

    /**
     * Removes the workspace mapping for the given server item from the
     * workspace template. {@inheritDoc}
     */
    @Override
    public boolean removeMapping(final String serverItem) {
        final List<_WorkspaceMapping> list = new ArrayList<_WorkspaceMapping>();
        boolean removed = false;

        for (final _WorkspaceMapping _mapping : getWebServiceObject().getMappings()) {
            if (!ServerPath.equals(_mapping.getServerItem(), serverItem)) {
                list.add(_mapping);
                removed = true;
            }
        }

        if (removed) {
            getWebServiceObject().setMappings(list.toArray(new _WorkspaceMapping[list.size()]));
        }

        return removed;
    }

    @Override
    public void copyFrom(final IWorkspaceTemplate workspaceTemplate) {
        Check.notNull(workspaceTemplate, "workspaceTemplate"); //$NON-NLS-1$

        final WorkspaceTemplate source = (WorkspaceTemplate) workspaceTemplate;

        getWebServiceObject().setDefinitionUri(source.getDefinitionURI());
        if (source.getInternalMappings() != null) {
            getWebServiceObject().setMappings(
                (_WorkspaceMapping[]) WrapperUtils.unwrap(_WorkspaceMapping.class, source.getInternalMappings()));
        } else {
            getWebServiceObject().setMappings(new _WorkspaceMapping[0]);
        }

        getWebServiceObject().setLastModifiedBy(source.getLastModifiedBy());
        getWebServiceObject().setLastModifiedDate(source.getLastModifiedDate());
    }
}
