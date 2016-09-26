// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.protocolhandler;

import org.junit.Test;

import com.microsoft.alm.teamfoundation.sourcecontrol.webapi.VstsInfo;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerGitRepository;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

import junit.framework.TestCase;

public class ProtocolHandlerHelpersTest extends TestCase {

    @Test
    public void testGetSelectedRepositoryThrows() {
        final VstsInfo repoInfo = ProtocolHandlerHelpersTestData.getDefaultRepoInfo();

        Exception exceptionExpected = null;
        try {
            Check.notNull(null, "branch"); //$NON-NLS-1$
        } catch (final Exception e) {
            exceptionExpected = e;
        }

        Throwable exceptionThrown = null;
        try {
            ProtocolHandlerHelpers.getSelectedRepository(repoInfo, null);
        } catch (final Throwable e) {
            exceptionThrown = e;
        }

        assertNotNull("getSelectedRepository should throw if branch is null", exceptionThrown); //$NON-NLS-1$
        assertEquals(true, exceptionThrown instanceof NullPointerException);
        assertEquals(exceptionExpected.getMessage(), exceptionThrown.getMessage());
    }

    @Test
    public void testGetSelectedDefaultRepository() {
        final VstsInfo repoInfo = ProtocolHandlerHelpersTestData.getDefaultRepoInfo();

        final String expectedProjectName = ProtocolHandlerHelpersTestData.getDefaultRepoProjectName();
        final String expectedProjectId = ProtocolHandlerHelpersTestData.getDefaultRepoProjectId();
        final String expectedRepoId = ProtocolHandlerHelpersTestData.getDefaultRepoRepoId();
        final String expectedRepoName = ProtocolHandlerHelpersTestData.getDefaultRepoRepoName();
        final String expectedRepoRemoteUrl = ProtocolHandlerHelpersTestData.getDefaultRepoRemoteUrl();

        final TypedServerGitRepository typedRepo =
            ProtocolHandlerHelpers.getSelectedRepository(repoInfo, StringUtil.EMPTY);

        final String projectId = typedRepo.getJson().getTeamProject().getId();
        assertEquals(expectedProjectId, projectId);

        final String projectName = typedRepo.getJson().getTeamProject().getName();
        assertEquals(expectedProjectName, projectName);

        final String repoId = typedRepo.getJson().getId();
        assertEquals(expectedRepoId, repoId);

        final String repoName = typedRepo.getJson().getName();
        assertEquals(expectedRepoName, repoName);

        final String repoRemoteUrl = typedRepo.getJson().getRemoteUrl();
        assertEquals(expectedRepoRemoteUrl, repoRemoteUrl);
    }

    @Test
    public void testGetSelectedNonDefaultRepository() {
        final VstsInfo repoInfo = ProtocolHandlerHelpersTestData.getNonDefaultRepoInfo();

        final String expectedProjectName = ProtocolHandlerHelpersTestData.getNonDefaultRepoProjectName();
        final String expectedProjectId = ProtocolHandlerHelpersTestData.getNonDefaultRepoProjectId();
        final String expectedRepoId = ProtocolHandlerHelpersTestData.getNonDefaultRepoRepoId();
        final String expectedRepoName = ProtocolHandlerHelpersTestData.getNonDefaultRepoRepoName();
        final String expectedRepoRemoteUrl = ProtocolHandlerHelpersTestData.getNonDefaultRepoRemoteUrl();

        final TypedServerGitRepository typedRepo =
            ProtocolHandlerHelpers.getSelectedRepository(repoInfo, StringUtil.EMPTY);

        final String projectId = typedRepo.getJson().getTeamProject().getId();
        assertEquals(expectedProjectId, projectId);

        final String projectName = typedRepo.getJson().getTeamProject().getName();
        assertEquals(expectedProjectName, projectName);

        final String repoId = typedRepo.getJson().getId();
        assertEquals(expectedRepoId, repoId);

        final String repoName = typedRepo.getJson().getName();
        assertEquals(expectedRepoName, repoName);

        final String repoRemoteUrl = typedRepo.getJson().getRemoteUrl();
        assertEquals(expectedRepoRemoteUrl, repoRemoteUrl);
    }

    @Test
    public void testGetSelectedNonLatinRepository() {
        final VstsInfo repoInfo = ProtocolHandlerHelpersTestData.getNonLatinRepoInfo();

        final String expectedProjectName = ProtocolHandlerHelpersTestData.getNonLatinRepoProjectName();
        final String expectedProjectId = ProtocolHandlerHelpersTestData.getNonLatinRepoProjectId();
        final String expectedRepoId = ProtocolHandlerHelpersTestData.getNonLatinRepoRepoId();
        final String expectedRepoName = ProtocolHandlerHelpersTestData.getNonLatinRepoRepoName();
        final String expectedRepoRemoteUrl = ProtocolHandlerHelpersTestData.getNonLatinRepoRemoteUrl();

        final TypedServerGitRepository typedRepo =
            ProtocolHandlerHelpers.getSelectedRepository(repoInfo, StringUtil.EMPTY);

        final String projectId = typedRepo.getJson().getTeamProject().getId();
        assertEquals(expectedProjectId, projectId);

        final String projectName = typedRepo.getJson().getTeamProject().getName();
        assertEquals(expectedProjectName, projectName);

        final String repoId = typedRepo.getJson().getId();
        assertEquals(expectedRepoId, repoId);

        final String repoName = typedRepo.getJson().getName();
        assertEquals(expectedRepoName, repoName);

        final String repoRemoteUrl = typedRepo.getJson().getRemoteUrl();
        assertEquals(expectedRepoRemoteUrl, repoRemoteUrl);
    }

    @Test
    public void testGetSelectedVstsRepository() {
        final VstsInfo repoInfo = ProtocolHandlerHelpersTestData.getVstsRepoInfo();

        final String expectedProjectName = ProtocolHandlerHelpersTestData.getVstsRepoProjectName();
        final String expectedProjectId = ProtocolHandlerHelpersTestData.getVstsRepoProjectId();
        final String expectedRepoId = ProtocolHandlerHelpersTestData.getVstsRepoRepoId();
        final String expectedRepoName = ProtocolHandlerHelpersTestData.getVstsRepoRepoName();
        final String expectedRepoRemoteUrl = ProtocolHandlerHelpersTestData.getVstsRepoRemoteUrl();

        final TypedServerGitRepository typedRepo =
            ProtocolHandlerHelpers.getSelectedRepository(repoInfo, StringUtil.EMPTY);

        final String projectId = typedRepo.getJson().getTeamProject().getId();
        assertEquals(expectedProjectId, projectId);

        final String projectName = typedRepo.getJson().getTeamProject().getName();
        assertEquals(expectedProjectName, projectName);

        final String repoId = typedRepo.getJson().getId();
        assertEquals(expectedRepoId, repoId);

        final String repoName = typedRepo.getJson().getName();
        assertEquals(expectedRepoName, repoName);

        final String repoRemoteUrl = typedRepo.getJson().getRemoteUrl();
        assertEquals(expectedRepoRemoteUrl, repoRemoteUrl);
    }
}
