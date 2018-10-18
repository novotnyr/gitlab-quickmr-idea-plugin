package com.github.novotnyr.idea.gitlab;

import com.google.gson.annotations.SerializedName;

public class MergeRequestRequest {
    @SerializedName("source_branch")
    private String sourceBranch;

    @SerializedName("target_branch")
    private String targetBranch;

    private String title;

    @SerializedName("assignee_id")
    private long assigneeId;

    @SerializedName("remove_source_branch")
    private boolean removeSourceBranch;

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public boolean isRemoveSourceBranch() {
        return removeSourceBranch;
    }

    public void setRemoveSourceBranch(boolean removeSourceBranch) {
        this.removeSourceBranch = removeSourceBranch;
    }
}
