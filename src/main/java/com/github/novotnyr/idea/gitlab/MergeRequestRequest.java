package com.github.novotnyr.idea.gitlab;

import com.google.gson.annotations.SerializedName;

public class MergeRequestRequest {
    @SerializedName("source_branch")
    private String sourceBranch;

    @SerializedName("target_branch")
    private String targetBranch;

    private String title;

    private String description;

    @SerializedName("assignee_id")
    private Long assigneeId;

    @SerializedName("remove_source_branch")
    private boolean removeSourceBranch;

    private String labels;

    private boolean squash;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public boolean isRemoveSourceBranch() {
        return removeSourceBranch;
    }

    public void setRemoveSourceBranch(boolean removeSourceBranch) {
        this.removeSourceBranch = removeSourceBranch;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public boolean isSquash() {
        return squash;
    }

    public void setSquash(boolean squash) {
        this.squash = squash;
    }
}
