package com.github.novotnyr.idea.gitlab;

public class GitLabProjectNotFoundException extends RuntimeException {
    public GitLabProjectNotFoundException(String message) {
        super(message);
    }

    public static GitLabProjectNotFoundException of(ProjectId projectId) {
        return new GitLabProjectNotFoundException("GitLab project '" + projectId + "' not found. Do GitLab URL and Git remote match?");
    }
}