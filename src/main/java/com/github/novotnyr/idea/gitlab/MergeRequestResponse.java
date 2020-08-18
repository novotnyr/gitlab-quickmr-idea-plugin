package com.github.novotnyr.idea.gitlab;

import com.google.gson.annotations.SerializedName;

public class MergeRequestResponse {
    @SerializedName("iid")
    private String number;

    @SerializedName("web_url")
    private String webUrl;

    @SerializedName("target_branch")
    private String targetBranch;

    @SerializedName("source_branch")
    private String sourceBranch;

    private User assignee;

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public User getAssignee() {
        return assignee;
    }

    public String getAssigneeName() {
        if (this.assignee == null) {
            return null;
        }
        return this.assignee.getName();
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }
}
