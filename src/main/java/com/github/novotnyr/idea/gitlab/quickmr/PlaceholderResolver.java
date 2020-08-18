package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.project.Project;

import java.util.Optional;

public class PlaceholderResolver {
    public static final String LAST_COMMIT_MESSAGE_PLACEHOLDER = "lastCommitMessage";
    public static final String SOURCE_BRANCH_PLACEHOLDER = "sourceBranch";
    public static final String TARGET_BRANCH_PLACEHOLDER = "targetBranch";

    private final GitService gitService;
    private final Project project;
    private final Settings settings;

    public PlaceholderResolver(GitService gitService, Project project, Settings settings) {
        this.gitService = gitService;
        this.project = project;
        this.settings = settings;
    }

    public String resolvePlaceholder(final String template, String placeholder, NewMergeRequest newMergeRequest) {
        String buffer = template;
        if (LAST_COMMIT_MESSAGE_PLACEHOLDER.equalsIgnoreCase(placeholder.trim())) {
            Optional<String> maybeLastCommitMessage = gitService.getLastCommitMessage(this.project);
            if(maybeLastCommitMessage.isPresent()) {
                String lastCommitMessage = maybeLastCommitMessage.get();
                buffer = buffer.replaceAll("\\{\\{lastCommitMessage}}", lastCommitMessage);
            }
        }
        if (SOURCE_BRANCH_PLACEHOLDER.equalsIgnoreCase(placeholder.trim())) {
            buffer = buffer.replaceAll("\\{\\{sourceBranch}}", newMergeRequest.getSourceBranch());
        }
        if (TARGET_BRANCH_PLACEHOLDER.equalsIgnoreCase(placeholder.trim())) {
            buffer = buffer.replaceAll("\\{\\{targetBranch}}", settings.getDefaultTargetBranch());
        }

        return buffer;
    }
}
