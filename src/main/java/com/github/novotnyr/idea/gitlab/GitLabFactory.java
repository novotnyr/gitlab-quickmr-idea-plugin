package com.github.novotnyr.idea.gitlab;

import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;

public class GitLabFactory {
    private final Settings settings;

    public GitLabFactory(Settings settings) {
        this.settings = settings;
    }

    public GitLab getGitLab() {
        String uri = this.settings.getGitLabUri();
        String accessToken = this.settings.getAccessToken();

        return new GitLab(uri, accessToken, this.settings.isInsecureTls());
    }

    public static GitLabFactory getInstance(Project project) {
        return ServiceManager.getService(project, GitLabFactory.class);
    }
}
