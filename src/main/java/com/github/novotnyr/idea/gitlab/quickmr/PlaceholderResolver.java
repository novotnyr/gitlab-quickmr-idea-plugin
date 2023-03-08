package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.project.Project;

import java.util.Optional;

public class PlaceholderResolver {
    public static final String LAST_COMMIT_MESSAGE_PLACEHOLDER = "lastCommitMessage";
    public static final String LAST_COMMIT_MESSAGE_SUBJECT_PLACEHOLDER = "lastCommitMessageSubject";
    public static final String LAST_COMMIT_MESSAGE_BODY_PLACEHOLDER = "lastCommitMessageBody";
    public static final String SOURCE_BRANCH_PLACEHOLDER = "sourceBranch";
    public static final String TARGET_BRANCH_PLACEHOLDER = "targetBranch";

    public static final String[] PLACEHOLDERS = {
        LAST_COMMIT_MESSAGE_PLACEHOLDER,
        LAST_COMMIT_MESSAGE_SUBJECT_PLACEHOLDER,
        LAST_COMMIT_MESSAGE_BODY_PLACEHOLDER,
        SOURCE_BRANCH_PLACEHOLDER,
        TARGET_BRANCH_PLACEHOLDER
    };

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
        if (LAST_COMMIT_MESSAGE_SUBJECT_PLACEHOLDER.equalsIgnoreCase(placeholder.trim())) {
            Optional<String> subject = gitService.getLastCommitMessageSubject(this.project);
            if(subject.isPresent()) {
                buffer = buffer.replaceAll("\\{\\{lastCommitMessageSubject}}", subject.get());
            }
        }
        if (LAST_COMMIT_MESSAGE_BODY_PLACEHOLDER.equalsIgnoreCase(placeholder.trim())) {
            Optional<String> body = gitService.getLastCommitMessageBody(this.project);
            if(body.isPresent()) {
                buffer = buffer.replaceAll("\\{\\{lastCommitMessageBody}}", body.get());
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
