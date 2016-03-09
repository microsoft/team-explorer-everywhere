// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wittype;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.form.WIFormDescription;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.form.WIFormParseHandler;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.WorkItemTypeMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.pguidance.ProcessGuidanceConstants;
import com.microsoft.tfs.core.pguidance.ProcessGuidanceURLInfo;

public class WorkItemTypeImpl implements WorkItemType {
    private final WorkItemTypeMetadata workItemTypeMetadata;
    private final WITContext witContext;
    private final ProjectImpl project;

    /*
     * cached, lazily loaded form description for this work item type
     */
    private WIFormDescription formDescription;

    /*
     * cached, lazily loaded field definitions for this work item type
     */
    private FieldDefinitionCollectionImpl fieldDefinitionCollection;

    /*
     * action cache
     */
    private final NextStateCache nextStateCache;

    public WorkItemTypeImpl(
        final WorkItemTypeMetadata workItemTypeMetadata,
        final ProjectImpl project,
        final WITContext witContext) {
        this.workItemTypeMetadata = workItemTypeMetadata;
        this.project = project;
        this.witContext = witContext;

        nextStateCache = new NextStateCache(workItemTypeMetadata.getID(), witContext.getMetadata());
    }

    @Override
    public int compareTo(final WorkItemType other) {
        return workItemTypeMetadata.getName().compareToIgnoreCase(
            ((WorkItemTypeImpl) other).workItemTypeMetadata.getName());
    }

    /*
     * ************************************************************************
     * START of implementation of WorkItemType interface
     * ***********************************************************************
     */

    @Override
    public String getName() {
        return workItemTypeMetadata.getName();
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public int getID() {
        return workItemTypeMetadata.getID();
    }

    @Override
    public WIFormDescription getFormDescription() {
        if (formDescription == null) {
            calculateForm();
        }
        return formDescription;
    }

    @Override
    public FieldDefinitionCollection getFieldDefinitions() {
        synchronized (this) {
            if (fieldDefinitionCollection == null) {
                fieldDefinitionCollection = new FieldDefinitionCollectionImpl(false, witContext, this);
            }
            return fieldDefinitionCollection;
        }
    }

    @Override
    public ProcessGuidanceURLInfo getProcessGuidanceURL() {
        final String typeName = workItemTypeMetadata.getName();
        final String supportingFilesFolder = ProcessGuidanceConstants.SUPPORTING_FILES_FOLDER;

        final MessageFormat documentPathFormat = new MessageFormat("{0}/{1}"); //$NON-NLS-1$

        final String primaryDocumentPath = documentPathFormat.format(new Object[] {
            supportingFilesFolder,
            typeName
        });
        final String[] alternateDocumentPaths = new String[] {
            documentPathFormat.format(new Object[] {
                supportingFilesFolder,
                typeName.replaceAll(" ", "") //$NON-NLS-1$ //$NON-NLS-2$
            }), documentPathFormat.format(new Object[] {
                supportingFilesFolder,
                ProcessGuidanceConstants.ABOUT_WORK_ITEMS_FILENAME
            }), documentPathFormat.format(new Object[] {
                supportingFilesFolder,
                ProcessGuidanceConstants.FILENAME
            })
        };

        return witContext.getClient().getProcessGuidance().getProcessGuidanceURL(
            new ProjectInfo(project.getName(), project.getURI()),
            primaryDocumentPath,
            alternateDocumentPaths);
    }

    /*
     * ************************************************************************
     * END of implementation of WorkItemType interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of implementation of internal (WorkItemTypeImpl) methods
     * ***********************************************************************
     */

    public ProjectImpl getProjectInternal() {
        return project;
    }

    private void calculateForm() {
        final int formId = witContext.getMetadata().getRulesTable().getWorkItemFormID(
            getProject().getID(),
            workItemTypeMetadata.getName());
        final String formXML = witContext.getMetadata().getHierarchyPropertiesTable().getValue(formId);
        formDescription = (WIFormDescription) WIFormParseHandler.parse(formXML);
    }

    public String getNextState(final String currentState, final String action) {
        return nextStateCache.getNextState(currentState, action);
    }

    /*
     * ************************************************************************
     * END of implementation of internal (WorkItemTypeImpl) methods
     * ***********************************************************************
     */
}
