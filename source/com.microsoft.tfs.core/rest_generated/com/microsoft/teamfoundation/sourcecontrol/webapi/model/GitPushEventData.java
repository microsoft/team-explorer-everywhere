// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.sourcecontrol.webapi.model;


/** 
 */
public class GitPushEventData {

    private String afterId;
    private String beforeId;
    private String branch;
    private GitCommit[] commits;
    private GitRepository repository;

    public String getAfterId() {
        return afterId;
    }

    public void setAfterId(final String afterId) {
        this.afterId = afterId;
    }

    public String getBeforeId() {
        return beforeId;
    }

    public void setBeforeId(final String beforeId) {
        this.beforeId = beforeId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(final String branch) {
        this.branch = branch;
    }

    public GitCommit[] getCommits() {
        return commits;
    }

    public void setCommits(final GitCommit[] commits) {
        this.commits = commits;
    }

    public GitRepository getRepository() {
        return repository;
    }

    public void setRepository(final GitRepository repository) {
        this.repository = repository;
    }
}
