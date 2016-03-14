// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.URLEncode;

public class GitProperties {

    public static final char PathSeparator = '/';
    public static final char WinPathSeparator = '\\';
    public static final String GIT = "git"; //$NON-NLS-1$
    public static final String RepositoryUrl = "RepositoryUrl"; //$NON-NLS-1$
    public static final String RepositoryName = "RepositoryName"; //$NON-NLS-1$
    public static final String DefaultBranch = "DefaultBranch"; //$NON-NLS-1$
    public static final String CIBranches = "CIBranches"; //$NON-NLS-1$
    public static final String LocalRepoPath = "LocalRepoPath"; //$NON-NLS-1$
    public static final String GitPathBeginning = "vstfs:///Git/VersionedItem/"; //$NON-NLS-1$
    public static final String BranchPrefix = "refs/heads/"; //$NON-NLS-1$

    private static final String BranchSeparator = ":"; //$NON-NLS-1$
    private static final String BranchInclusionOperator = "+"; //$NON-NLS-1$
    private static final String BranchExclusionOperator = "-"; //$NON-NLS-1$
    private static final String BranchPatternOperator = "*"; //$NON-NLS-1$

    /**
     * VS saves this unique repo name (teamProjectName/repoName) in source
     * provider
     *
     */
    public static String createUniqueRepoName(final String teamProjectName, final String repoName) {
        return teamProjectName + PathSeparator + repoName;
    }

    public static String getRepoNameFromUniqueRepoName(final String uniqRepoName) {
        if (StringUtil.isNullOrEmpty(uniqRepoName)) {
            return null;
        }

        final String[] names = uniqRepoName.split(LocalPath.GENERAL_LOCAL_PATH_SEPARATOR);
        if (names.length == 1) {
            return uniqRepoName;
        } else {
            return names[names.length - 1];
        }
    }

    public static void parseBranchSpec(
        final String branchSpec,
        boolean excludeBranch,
        String branch,
        boolean isPattern,
        final boolean removePattern) {
        excludeBranch = false;
        isPattern = false;
        branch = branchSpec;

        if (!StringUtil.isNullOrEmpty(branchSpec)) {
            if (branchSpec.startsWith(BranchExclusionOperator)) {
                excludeBranch = true;
                branch = branch.substring(1);
            } else if (branchSpec.startsWith(BranchInclusionOperator)) {
                excludeBranch = false;
                branch = branch.substring(1);
            }

            if (branchSpec.endsWith(BranchPatternOperator)) {
                isPattern = true;
                if (removePattern) {
                    branch = branch.substring(0, branch.length() - 1);
                }
            }
        }
    }

    public static String createBranchSpec(final boolean excludeBranch, final String branch) {
        final StringBuilder sb = new StringBuilder();
        if (excludeBranch) {
            sb.append(BranchExclusionOperator);
        }

        sb.append(branch);

        return sb.toString();
    }

    public static String joinBranches(final String[] branches) {
        if (branches != null && branches.length > 0) {
            return StringUtil.join(branches, BranchSeparator);
        }
        return null;
    }

    public static List<String> splitBranches(final String branchString) {
        if (StringUtil.isNullOrEmpty(branchString)) {
            return null;
        }

        final String[] branches = branchString.split(BranchSeparator);
        return Arrays.asList(branches);
    }

    public static String createGitRepositoryUrl(
        final String collectionUrl,
        final String teamProjectName,
        final String repoName) {
        if (StringUtil.isNullOrEmpty(collectionUrl) || StringUtil.isNullOrEmpty(repoName)) {
            return null;
        }

        String cUrl = collectionUrl;
        if (!cUrl.endsWith("/")) //$NON-NLS-1$
        {
            cUrl = collectionUrl + "/"; //$NON-NLS-1$
        }

        String projName = teamProjectName;
        if (StringUtil.isNullOrEmpty(teamProjectName)) {
            // If no team project was provided then assume that the team project
            // name is the same as the repoName
            projName = repoName;
        }

        // TODO: should really be using the REST api here
        // The format for the Git repo associated with a Team Project is
        // <collectionUrl>/_git/<teamProjectName> OR
        // <collectionUrl>/<teamProjectName>/_git/<repoName>
        return collectionUrl + URLEncode.encode(projName) + "/_git/" + URLEncode.encode(repoName); //$NON-NLS-1$
    }

    public static String gitUriToLocalRelativePath(final String gitUri) {
        if (StringUtil.isNullOrEmpty(gitUri) || !gitUri.startsWith(GitPathBeginning)) {
            return null;
        }
        final String text = gitUri.substring(GitPathBeginning.length());
        final String[] parts = StringUtil.split("/", text); //$NON-NLS-1$

        if (parts.length >= 4) {
            // git folder uri is like this
            // vstfs:///Git/VersionedItem/ProjName/RepoName/BranchName/relativePathInGitRepo
            return text.substring(parts[0].length() + parts[1].length() + parts[2].length() + 3);
        } else {
            return LocalPath.GENERAL_LOCAL_PATH_SEPARATOR;
        }
    }

    public static String createGitItemUrl(
        final String project,
        final String repo,
        final String branch,
        final String relativePathInRepo) {
        if (StringUtil.isNullOrEmpty(project) && StringUtil.isNullOrEmpty(repo)) {
            return null;
        }

        final StringBuilder sb = new StringBuilder(GitPathBeginning);

        // if project name is null, try use repo name
        if (StringUtil.isNullOrEmpty(project)) {
            sb.append(repo);
        } else {
            sb.append(project);

        }
        sb.append(PathSeparator);

        if (StringUtil.isNullOrEmpty(repo)) {
            sb.append(project);
        } else {
            sb.append(repo);

        }

        sb.append(PathSeparator);

        // default to master branch if branch is null
        if (StringUtil.isNullOrEmpty(branch)) {
            sb.append("master"); //$NON-NLS-1$
        } else {
            sb.append(branch);
        }

        if (!StringUtil.isNullOrEmpty(relativePathInRepo)) {
            String normalizedPath = normalizePathSeparator(relativePathInRepo);
            while (normalizedPath.length() > 0 && normalizedPath.charAt(0) == PathSeparator) {
                normalizedPath = normalizedPath.substring(1);
            }

            sb.append(PathSeparator);
            sb.append(normalizedPath);
        }

        return sb.toString();
    }

    /**
     * Get short branch name (e.g. master) from full branch name (e.g.
     * ref/heads/master)
     *
     */
    public static String getShortBranchName(final String name) {
        if (StringUtil.isNullOrEmpty(name) || !name.startsWith(BranchPrefix)) {
            return name;
        } else {
            String shortName = name.substring(BranchPrefix.length());
            if (shortName.indexOf(PathSeparator) != -1) {
                shortName = shortName.replace(PathSeparator, '\0');
            }
            return shortName;
        }
    }

    public static String normalizePathSeparator(final String string) {
        return string.replace(WinPathSeparator, PathSeparator);
    }

    /**
     * Git repository item URL has to have the following format:
     *
     * <prefix>/<project-name>/<repository-name>/<branch>[/<path>] where
     * <prefix> = vstfs:///Git/VersionedItem
     *
     * The method parses the URL supplied and assigns corresponding parts to
     * provided variables (if the corresponding variable is not null
     *
     */
    public static boolean parseGitItemUrl(
        final String url,
        final AtomicReference<String> projectName,
        final AtomicReference<String> repositoryName,
        final AtomicReference<String> branchName,
        final AtomicReference<String> path) {
        if (StringUtil.isNullOrEmpty(url) || !url.startsWith(GitPathBeginning)) {
            return false;
        }

        /*
         * <project-name>/<repository-name>/<branch>[/<path>]
         */
        final String s = url.substring(GitPathBeginning.length());
        final String[] parts = s.split(String.valueOf(new char[] {
            PathSeparator
        }));

        if (parts.length < 3) {
            return false;
        }

        for (int i = 0; i < parts.length - 1; i++) {
            if (StringUtil.isNullOrEmpty(parts[i])) {
                return false;
            }
        }

        assignTo(parts[0], projectName);
        assignTo(parts[1], repositoryName);
        assignTo(parts[2], branchName);

        if (parts.length < 4) {
            assignTo(StringUtil.EMPTY, path);
        } else {
            /*
             * Length of: <project-name>/<repository-name>/<branch>
             */
            final int pathOffset = parts[0].length() + 1 + parts[1].length() + 1 + parts[2].length() + 1;

            assignTo(s.substring(pathOffset), path);
        }

        return true;
    }

    private static void assignTo(final String value, final AtomicReference<String> target) {
        if (target != null) {
            target.set(value);
        }
    }
}
