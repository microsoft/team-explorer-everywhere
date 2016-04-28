// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.importwizard.support;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.git.json.TfsGitBranchJson;
import com.microsoft.tfs.client.common.git.json.TfsGitBranchesJson;
import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;

public class ImportGitRepository extends ImportItemBase implements Comparable<ImportGitRepository> {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String SUMMARY_SEPARATOR = ";"; //$NON-NLS-1$
    private final static String REFS_HEADS = "refs/heads/"; //$NON-NLS-1$
    private static final Log log = LogFactory.getLog(ImportGitRepository.class);

    public static final String CLONE_PENDING = Messages.getString("ImportGitRepository.ClonePendingState"); //$NON-NLS-1$
    public static final String CLONE_EMPTY_REPOSITORY = Messages.getString("ImportGitRepository.CloneEmptyState"); //$NON-NLS-1$
    public static final String CLONE_IN_PROGRESS = Messages.getString("ImportGitRepository.CloneInProgressState"); //$NON-NLS-1$
    public static final String CLONE_FINISHED = Messages.getString("ImportGitRepository.CloneFinishedState"); //$NON-NLS-1$
    public static final String CLONE_CANCELLED = Messages.getString("ImportGitRepository.CloneCancelledState"); //$NON-NLS-1$
    public static final String CLONE_ERROR = Messages.getString("ImportGitRepository.CloneErrorState"); //$NON-NLS-1$

    private TfsGitRepositoryJson gitRepositoryJson;
    private TfsGitBranchesJson gitBranchesJson;
    private GUID id;
    private URL remoteUrl;
    private String[] branches;
    private String[] refs;
    private String defaultBranch;
    private String defaultRef;
    private String workingDirectory;
    private boolean cloneSubmodules;
    private String remoteName;
    private String cloneStatus;
    private List<ImportEclipseProject> gitProjects = new ArrayList<ImportEclipseProject>();

    public ImportGitRepository(final String serverPath) {
        super(serverPath);
    }

    public void setBranchesJson(final TfsGitBranchesJson gitBranchesJson) {
        this.gitBranchesJson = gitBranchesJson;
    }

    public void setRepositroyJson(final TfsGitRepositoryJson gitRepositoryJson) {
        this.gitRepositoryJson = gitRepositoryJson;
    }

    public GUID getId() {
        if (gitRepositoryJson != null && id == null) {
            id = new GUID(gitRepositoryJson.getId());
        }

        return id;
    }

    public URL getRemoteUrl() {
        if (gitRepositoryJson != null && remoteUrl == null) {
            try {
                remoteUrl = new URL(gitRepositoryJson.getRemoteUrl());
            } catch (final MalformedURLException e) {
                log.error("Incorrect Remote URL", e); //$NON-NLS-1$
            }
        }

        return remoteUrl;
    }

    private void readBranches() {
        if (gitBranchesJson != null && branches == null) {
            final int nBranches = gitBranchesJson.getBranches().size();
            branches = new String[nBranches];
            refs = new String[nBranches];

            int idx = 0;
            for (final TfsGitBranchJson branch : gitBranchesJson.getBranches()) {
                final String name = branch.getName();
                final String fullName = branch.getFullName();

                branches[idx] = name;
                refs[idx] = fullName;

                if (fullName.equals(gitRepositoryJson.getDefaultBranch())) {
                    defaultBranch = name;
                    defaultRef = fullName;
                }

                idx++;
            }

            if (StringUtil.isNullOrEmpty(defaultBranch) && branches.length > 0) {
                defaultBranch = branches[0];
                defaultRef = refs[0];
            }

            setCloneStatus(CLONE_PENDING);
        }
    }

    public String[] getBranches() {
        readBranches();
        return branches;
    }

    public String[] getRefs() {
        readBranches();
        return refs;
    }

    public String getDefaultBranch() {
        readBranches();
        return defaultBranch;
    }

    public String getDefaultRef() {
        readBranches();
        return defaultRef;
    }

    public void setDefaultBranch(final String defaultBranch) {
        this.defaultBranch = defaultBranch;
        this.defaultRef = REFS_HEADS + defaultBranch;
        setCloneStatus(CLONE_PENDING);
    }

    public String getProjectName() {
        return ServerPath.getFileName(ServerPath.getParent(getFullPath()));
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(final String workingDirectory) {
        this.workingDirectory = workingDirectory;
        setCloneStatus(CLONE_PENDING);
    }

    public boolean getCloneSubmodules() {
        return cloneSubmodules;
    }

    public void setCloneSubmodules(final boolean cloneSubmodules) {
        this.cloneSubmodules = cloneSubmodules;
        setCloneStatus(CLONE_PENDING);
    }

    public String getRemoteName() {
        return remoteName;
    }

    public void setRemoteName(final String remoteName) {
        this.remoteName = remoteName;
        setCloneStatus(CLONE_PENDING);
    }

    public ImportEclipseProject[] getGitProjects() {
        return gitProjects.toArray(new ImportEclipseProject[gitProjects.size()]);
    }

    public void setGitProjects(final List<ImportEclipseProject> gitProjects) {
        this.gitProjects = gitProjects;
    }

    public int getGitProjectsCount() {
        return gitProjects.size();
    }

    public String getSummary() {
        final StringBuilder sb = new StringBuilder();

        sb.append(getProjectName());
        sb.append(SUMMARY_SEPARATOR);

        sb.append(getDefaultBranch());
        sb.append(SUMMARY_SEPARATOR);

        sb.append(getRemoteName());
        sb.append(SUMMARY_SEPARATOR);

        sb.append(getWorkingDirectory());

        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("URL:"); //$NON-NLS-1$
        appendNewline(sb);

        appendIndent(sb);
        sb.append(getRemoteUrl().toString());
        appendNewline(sb);

        sb.append("Branches:"); //$NON-NLS-1$
        appendNewline(sb);

        for (final String b : getBranches()) {
            appendIndent(sb);
            sb.append(b);
            appendNewline(sb);
        }

        sb.append("Default branch:"); //$NON-NLS-1$
        appendNewline(sb);

        appendIndent(sb);
        sb.append(getDefaultBranch());
        appendNewline(sb);

        return sb.toString();
    }

    private void appendNewline(final StringBuilder sb) {
        sb.append(NEWLINE);
    }

    private void appendIndent(final StringBuilder sb) {
        sb.append("   "); //$NON-NLS-1$
    }

    @Override
    public int compareTo(final ImportGitRepository o) {
        return getName().compareToIgnoreCase(o.getName());
    }

    public String getCloneStatus() {
        return cloneStatus;
    }

    public void setCloneStatus(final String status) {
        cloneStatus = status;
    }
}
