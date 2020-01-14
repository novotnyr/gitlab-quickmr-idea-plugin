package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.intellij.openapi.project.Project;

import java.util.Optional;

public class PlaceholderResolver {
    private final GitService gitService;

    private final Project project;

    public PlaceholderResolver(GitService gitService, Project project) {
        this.gitService = gitService;
        this.project = project;
    }

    public String resolvePlaceholder(final String template, String placeholder) {
        String buffer = template;
        if ("lastCommitMessage".equalsIgnoreCase(placeholder.trim())) {
            Optional<String> maybeLastCommitMessage = gitService.getLastCommitMessage(this.project);
            if(maybeLastCommitMessage.isPresent()) {
                String lastCommitMessage = maybeLastCommitMessage.get();
                buffer = buffer.replaceAll("\\{\\{lastCommitMessage}}", lastCommitMessage);
            }
        }
        return buffer;
    }
}
