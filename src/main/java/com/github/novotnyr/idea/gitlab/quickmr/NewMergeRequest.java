package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.User;

public class NewMergeRequest {
    private String gitLabProjectId;

    private String sourceBranch;

    private User assignee;

    public String getGitLabProjectId() {
        return gitLabProjectId;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setGitLabProjectId(String gitLabProjectId) {
        this.gitLabProjectId = gitLabProjectId;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }
}
