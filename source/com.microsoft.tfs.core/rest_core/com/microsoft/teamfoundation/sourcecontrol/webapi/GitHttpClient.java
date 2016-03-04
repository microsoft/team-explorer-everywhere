// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.sourcecontrol.webapi;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitItem;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRef;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitVersionDescriptor;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.VersionControlRecursionType;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.vss.client.core.utils.ArgumentUtility;

public class GitHttpClient extends GitHttpClientBase {

    public GitHttpClient(final TFSTeamProjectCollection connection) {
        super(connection);
    }

    /**
     * Get item
     *
     * @param project
     * @param repositoryId
     * @param path
     * @param includeContentMetadata
     * @return GitItem
     */
    public GitItem getItem(
        final String project,
        final String repositoryId,
        final String path,
        final boolean includeContentMetadata) {
        ArgumentUtility.checkStringForNullOrEmpty(project, "project", true); //$NON-NLS-1$
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId", true); //$NON-NLS-1$
        return super.getItem(
            project,
            repositoryId,
            path,
            null,
            VersionControlRecursionType.NONE,
            includeContentMetadata,
            false,
            false,
            null);
    }

    /**
     * Get item
     *
     * @param project
     * @param repositoryId
     * @param path
     * @param includeContentMetadata
     * @param latestProcessedChange
     * @param versionDescriptor
     * @return GitItem
     */
    public GitItem getItem(
        final String project,
        final String repositoryId,
        final String path,
        final boolean includeContentMetadata,
        final boolean latestProcessedChange,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkStringForNullOrEmpty(project, "project", true); //$NON-NLS-1$
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId", true); //$NON-NLS-1$
        return super.getItem(
            project,
            repositoryId,
            path,
            null,
            VersionControlRecursionType.NONE,
            includeContentMetadata,
            latestProcessedChange,
            false,
            versionDescriptor);
    }

    /**
     * Get item
     *
     * @param repositoryId
     * @param path
     * @param includeContentMetadata
     * @return GitItem
     */
    public GitItem getItem(final UUID repositoryId, final String path, final boolean includeContentMetadata) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getItem(
            repositoryId,
            path,
            null,
            VersionControlRecursionType.NONE,
            includeContentMetadata,
            false,
            false,
            null);
    }

    /**
     * Get item
     *
     * @param repositoryId
     * @param path
     * @param includeContentMetadata
     * @param latestProcessedChange
     * @param versionDescriptor
     * @return GitItem
     */
    public GitItem getItem(
        final UUID repositoryId,
        final String path,
        final boolean includeContentMetadata,
        final boolean latestProcessedChange,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getItem(
            repositoryId,
            path,
            null,
            VersionControlRecursionType.NONE,
            includeContentMetadata,
            latestProcessedChange,
            false,
            versionDescriptor);
    }

    /**
     * Get items
     *
     * @param repositoryId
     * @param scopePath
     * @param recursionLevel
     * @param includeContentMetadata
     * @param latestProcessedChange
     * @param includeLinks
     * @param versionDescriptor
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final UUID repositoryId,
        final String scopePath,
        final VersionControlRecursionType recursionLevel,
        final boolean includeContentMetadata,
        final boolean latestProcessedChange,
        final boolean includeLinks,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getItems(
            repositoryId,
            scopePath,
            recursionLevel,
            includeContentMetadata,
            latestProcessedChange,
            false,
            includeLinks,
            versionDescriptor);
    }

    /**
     * Get items
     *
     * @param project
     * @param repositoryId
     * @param scopePath
     * @param recursionLevel
     * @param includeContentMetadata
     * @param latestProcessedChange
     * @param includeLinks
     * @param versionDescriptor
     * @return List<GitItem>
     */
    public List<GitItem> getItems(
        final String project,
        final String repositoryId,
        final String scopePath,
        final VersionControlRecursionType recursionLevel,
        final boolean includeContentMetadata,
        final boolean latestProcessedChange,
        final boolean includeLinks,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkStringForNullOrEmpty(project, "project", true); //$NON-NLS-1$
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId", true); //$NON-NLS-1$
        return super.getItems(
            project,
            repositoryId,
            scopePath,
            recursionLevel,
            includeContentMetadata,
            latestProcessedChange,
            false,
            includeLinks,
            versionDescriptor);
    }

    /**
     * Get contents of item
     *
     * @param repositoryId
     * @param path
     * @param versionDescriptor
     * @return InputStream
     */
    public InputStream getItemContent(
        final UUID repositoryId,
        final String path,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getItemContent(
            repositoryId,
            path,
            null,
            VersionControlRecursionType.NONE,
            false,
            false,
            true,
            versionDescriptor);
    }

    /**
     * Get contents of item
     *
     * @param project
     * @param repositoryId
     * @param path
     * @param versionDescriptor
     * @return InputStream
     */
    public InputStream getItemContent(
        final String project,
        final String repositoryId,
        final String path,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkStringForNullOrEmpty(project, "project", true); //$NON-NLS-1$
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId", true); //$NON-NLS-1$
        return super.getItemContent(
            repositoryId,
            path,
            null,
            VersionControlRecursionType.NONE,
            false,
            false,
            true,
            versionDescriptor);
    }

    /**
     * Get item as zip
     *
     * @param project
     * @param repositoryId
     * @param scopePath
     * @param versionDescriptor
     * @return InputStream
     */
    public InputStream getItemZip(
        final String project,
        final String repositoryId,
        final String scopePath,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkStringForNullOrEmpty(project, "project", true); //$NON-NLS-1$
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId", true); //$NON-NLS-1$
        return super.getItemZip(
            project,
            repositoryId,
            null,
            scopePath,
            VersionControlRecursionType.FULL,
            false,
            false,
            true,
            versionDescriptor);
    }

    /**
     * Get item as zip
     *
     * @param repositoryId
     * @param scopePath
     * @param versionDescriptor
     * @return InputStream
     */
    public InputStream getItemZip(
        final UUID repositoryId,
        final String scopePath,
        final GitVersionDescriptor versionDescriptor) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getItemZip(
            repositoryId,
            null,
            scopePath,
            VersionControlRecursionType.FULL,
            false,
            false,
            true,
            versionDescriptor);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final String repositoryId) {
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId", true); //$NON-NLS-1$
        return super.getRefs(repositoryId, null, null);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @param includeLinks
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final String repositoryId, final boolean includeLinks) {
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getRefs(repositoryId, null, includeLinks);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @param refType
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final String repositoryId, final String refType) {
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getRefs(repositoryId, refType, null);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @param refType
     * @param includeLinks
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final String repositoryId, final String refType, final boolean includeLinks) {
        ArgumentUtility.checkStringForNullOrEmpty(repositoryId, "repositoryId"); //$NON-NLS-1$
        ArgumentUtility.checkStringForNullOrEmpty(refType, "refType", true); //$NON-NLS-1$
        return super.getRefs(repositoryId, refType, includeLinks);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final UUID repositoryId) {
        return super.getRefs(repositoryId, null, null);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @param includeLinks
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final UUID repositoryId, final boolean includeLinks) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getRefs(repositoryId, null, includeLinks);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @param refType
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final UUID repositoryId, final String refType) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        return super.getRefs(repositoryId, refType, null);
    }

    /**
     * Get refs
     *
     * @param repositoryId
     * @param refType
     * @param includeLinks
     * @return List<GitRef>
     */
    public List<GitRef> getRefs(final UUID repositoryId, final String refType, final boolean includeLinks) {
        ArgumentUtility.checkForEmptyGuid(repositoryId, "repositoryId"); //$NON-NLS-1$
        ArgumentUtility.checkStringForNullOrEmpty(refType, "refType", true); //$NON-NLS-1$
        return super.getRefs(repositoryId, refType, includeLinks);
    }

    /**
     * Get repositories
     *
     * @return List<GitRepository>
     */
    public List<GitRepository> getRepositories() {
        return super.getRepositories(null);
    }

    /**
     * Get repositories
     *
     * @param includeLinks
     * @return List<GitRepository>
     */
    public List<GitRepository> getRepositories(final boolean includeLinks) {
        return super.getRepositories(includeLinks);
    }

    /**
     * Get repositories
     *
     * @param project
     * @return List<GitRepository>
     */
    public List<GitRepository> getRepositories(final String project) {
        ArgumentUtility.checkStringForNullOrEmpty(project, "project", true); //$NON-NLS-1$
        return super.getRepositories(project, null);
    }

    /**
     * Get repositories
     *
     * @param project
     * @param includeLinks
     * @return List<GitRepository>
     */
    public List<GitRepository> getRepositories(final String project, final boolean includeLinks) {
        ArgumentUtility.checkStringForNullOrEmpty(project, "project", true); //$NON-NLS-1$
        return super.getRepositories(project, includeLinks);
    }
}
